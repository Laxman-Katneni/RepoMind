package com.reviewassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewassistant.controller.dto.AuditNotification;
import com.reviewassistant.model.AuditFinding;
import com.reviewassistant.model.CodeAudit;
import com.reviewassistant.model.Repository;
import com.reviewassistant.repository.AuditFindingRepository;
import com.reviewassistant.repository.CodeAuditRepository;
import com.reviewassistant.repository.RepositoryRepository;
import com.reviewassistant.service.dto.AuditResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class CodeAuditService {

    private static final Logger logger = LoggerFactory.getLogger(CodeAuditService.class);

    // Directories and files to skip (blocklist)
    private static final Set<String> SKIP_DIRECTORIES = Set.of(
        "node_modules", "target", ".git", "build", "dist", ".idea",
        "coverage", ".vscode", "__pycache__", "vendor", ".gradle"
    );

    // File extensions to audit (expanded to include config files)
    private static final Set<String> AUDITABLE_EXTENSIONS = Set.of(
        ".java", ".py", ".js", ".ts", ".tsx", ".jsx",
        ".html", ".css", ".xml", ".properties", 
        ".yml", ".yaml", ".sql", ".json", ".md"
    );

    private final CodeAuditRepository codeAuditRepository;
    private final AuditFindingRepository auditFindingRepository;
    private final RepositoryRepository repositoryRepository;
    private final HuggingFaceAuditService huggingFaceAuditService;
    private final GithubService githubService;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public CodeAuditService(
            CodeAuditRepository codeAuditRepository,
            AuditFindingRepository auditFindingRepository,
            RepositoryRepository repositoryRepository,
            HuggingFaceAuditService huggingFaceAuditService,
            GithubService githubService,
            ObjectMapper objectMapper,
            SimpMessagingTemplate messagingTemplate) {
        this.codeAuditRepository = codeAuditRepository;
        this.auditFindingRepository = auditFindingRepository;
        this.repositoryRepository = repositoryRepository;
        this.huggingFaceAuditService = huggingFaceAuditService;
        this.githubService = githubService;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Initiates an audit job for a repository.
     * Returns the audit ID immediately for polling.
     */
    @Transactional
    public Long startAudit(Long repositoryId, String accessToken) {
        Repository repository = repositoryRepository.findById(repositoryId)
            .orElseThrow(() -> new IllegalArgumentException("Repository not found"));

        // Create audit job
        CodeAudit audit = new CodeAudit(repository);
        audit = codeAuditRepository.save(audit);

        logger.info("Started audit job {} for repository {}/{}", 
            audit.getId(), repository.getOwner(), repository.getName());

        // Start async processing
        Long auditId = audit.getId();
        processAuditAsync(auditId, repository, accessToken);

        return auditId;
    }

    /**
     * Asynchronously processes the audit.
     * SEQUENTIAL processing to avoid overwhelming free-tier HF API.
     */
    @Async
    @Transactional
    public void processAuditAsync(Long auditId, Repository repository, String accessToken) {
        CodeAudit audit = codeAuditRepository.findById(auditId)
            .orElseThrow(() -> new IllegalStateException("Audit not found"));

        try {
            audit.setStatus(CodeAudit.AuditStatus.IN_PROGRESS);
            codeAuditRepository.save(audit);

            logger.info("Processing audit {} for {}/{}", 
                auditId, repository.getOwner(), repository.getName());

            // Fetch files from GitHub
            List<String> filePaths = githubService.getRepositoryFilePaths(
                repository.getOwner(),
                repository.getName(),
                accessToken
            );

            // Filter files: only src directory, skip test files
            List<String> auditableFiles = filterAuditableFiles(filePaths);
            
            logger.info("Found {} auditable files in repository", auditableFiles.size());
            audit.setTotalFilesScanned(0);
            codeAuditRepository.save(audit);

            int processed = 0;
            int failedCount = 0;

            // SEQUENTIAL processing (one file at a time)
            for (String filePath : auditableFiles) {
                try {
                    processed++;
                    
                    // Update progress
                    audit.updateProgress(processed, auditableFiles.size(), filePath);
                    codeAuditRepository.save(audit);

                    logger.debug("Auditing file {}/{}: {}", processed, auditableFiles.size(), filePath);

                    // Fetch file content
                    String content = githubService.getFileContent(
                        repository.getOwner(),
                        repository.getName(),
                        filePath,
                        accessToken
                    );

                    // Detect language
                    String language = detectLanguage(filePath);

                    // Analyze with HF model
                    AuditResult result = huggingFaceAuditService.analyzeCode(content, language, filePath);

                    if (result != null) {
                        // Save finding
                        saveFinding(audit, filePath, content, result);
                        audit.incrementCounts(result.severity());
                        audit.setTotalFilesScanned(audit.getTotalFilesScanned() + 1);
                        
                        if (audit.getFilesWithIssues() == null) {
                            audit.setFilesWithIssues(0);
                        }
                        audit.setFilesWithIssues(audit.getFilesWithIssues() + 1);
                    } else {
                        logger.warn("No audit result for file: {}", filePath);
                        failedCount++;
                    }

                    // Save progress
                    codeAuditRepository.save(audit);

                } catch (Exception e) {
                    logger.error("Failed to audit file {}: {}", filePath, e.getMessage());
                    failedCount++;
                }
            }

            // Mark as completed
            if (failedCount == 0) {
                audit.markCompleted();
            } else if (failedCount < auditableFiles.size()) {
                audit.setStatus(CodeAudit.AuditStatus.PARTIALLY_COMPLETED);
                audit.setCompletedAt(LocalDateTime.now());
                audit.setErrorMessage(failedCount + " files failed to analyze");
            } else {
                audit.markFailed("All files failed to analyze");
            }

            codeAuditRepository.save(audit);
            logger.info("Audit {} completed: {} files scanned, {} files with issues, {} failed",
                auditId, audit.getTotalFilesScanned(), audit.getFilesWithIssues(), failedCount);

            // Send WebSocket notification on success
            sendAuditCompleteNotification(audit);

        } catch (Exception e) {
            logger.error("Audit {} failed with exception: {}", auditId, e.getMessage(), e);
            audit.markFailed(e.getMessage());
            codeAuditRepository.save(audit);
            
            // Send WebSocket notification on failure
            sendAuditFailedNotification(audit);
        }
    }

    /**
     * Sends WebSocket notification when audit completes successfully.
     */
    private void sendAuditCompleteNotification(CodeAudit audit) {
        try {
            AuditNotification notification = AuditNotification.success(
                audit.getId(),
                audit.getCriticalCount(),
                audit.getWarningCount(),
                audit.getInfoCount()
            );
            messagingTemplate.convertAndSend("/topic/audit-updates", notification);
            logger.info("Sent WebSocket notification for completed audit {}", audit.getId());
        } catch (Exception e) {
            logger.error("Failed to send WebSocket notification: {}", e.getMessage());
        }
    }

    /**
     * Sends WebSocket notification when audit fails.
     */
    private void sendAuditFailedNotification(CodeAudit audit) {
        try {
            AuditNotification notification = AuditNotification.failure(
                audit.getId(),
                audit.getErrorMessage()
            );
            messagingTemplate.convertAndSend("/topic/audit-updates", notification);
            logger.info("Sent WebSocket notification for failed audit {}", audit.getId());
        } catch (Exception e) {
            logger.error("Failed to send WebSocket notification: {}", e.getMessage());
        }
    }

    /**
     * Filters files using blocklist strategy - excludes bad directories, accepts valid extensions.
     * No longer requires files to be in src/ directory.
     */
    private List<String> filterAuditableFiles(List<String> allFiles) {
        List<String> filtered = new ArrayList<>();

        for (String filePath : allFiles) {
            boolean isAuditable = isAuditableFile(filePath);
            
            // Debug logging as requested
            logger.debug("File assessment: {} -> {}", filePath, isAuditable);
            
            if (isAuditable) {
                filtered.add(filePath);
            }
        }

        logger.info("Filtered {} auditable files from {} total files", filtered.size(), allFiles.size());
        return filtered;
    }

    /**
     * Determines if a file should be audited using blocklist + extension strategy.
     * Step A: Check blocklist
     * Step B: Check valid extension
     * Step C: No src/ requirement (removed)
     */
    private boolean isAuditableFile(String filePath) {
        // Step A: Blocklist - skip bad directories
        for (String dir : SKIP_DIRECTORIES) {
            if (filePath.contains("/" + dir + "/") || filePath.contains("\\" + dir + "\\") ||
                filePath.startsWith(dir + "/") || filePath.startsWith(dir + "\\")) {
                return false;
            }
        }

        // Step B: Extension check - must end with supported extension
        for (String ext : AUDITABLE_EXTENSIONS) {
            if (filePath.endsWith(ext)) {
                return true;
            }
        }

        // Step C: If no valid extension, reject
        return false;
    }

    /**
     * Saves an audit finding to the database.
     */
    private void saveFinding(CodeAudit audit, String filePath, String fileContent, AuditResult result) {
        try {
            AuditFinding finding = new AuditFinding();
            finding.setAudit(audit);
            finding.setFilePath(filePath);
            finding.setSeverity(result.severity());
            finding.setCategory(result.category());
            finding.setLanguage(result.language());
            finding.setTitle(result.title());
            finding.setMessage(result.message());
            finding.setSuggestion(result.suggestion());

            // Extract line number from evidence
            Integer lineNumber = huggingFaceAuditService.extractLineNumber(result);
            finding.setLineNumber(lineNumber);

            // Extract code snippet
            String snippet = huggingFaceAuditService.extractCodeSnippet(result);
            finding.setCodeSnippet(snippet);

            // Store full metadata as JSON
            if (result.extra() != null) {
                finding.setMetadata(objectMapper.writeValueAsString(result.extra()));
            }

            auditFindingRepository.save(finding);
            
        } catch (Exception e) {
            logger.error("Failed to save finding for {}: {}", filePath, e.getMessage());
        }
    }

    /**
     * Detects programming language from file extension.
     */
    private String detectLanguage(String filePath) {
        if (filePath.endsWith(".java")) return "Java";
        if (filePath.endsWith(".py")) return "Python";
        if (filePath.endsWith(".ts") || filePath.endsWith(".tsx")) return "TypeScript";
        if (filePath.endsWith(".js") || filePath.endsWith(".jsx")) return "JavaScript";
        return "Unknown";
    }

    /**
     * Gets audit status for polling.
     */
    public CodeAudit getAuditStatus(Long auditId) {
        return codeAuditRepository.findById(auditId)
            .orElseThrow(() -> new IllegalArgumentException("Audit not found"));
    }

    /**
     * Gets the latest audit for a repository.
     */
    public CodeAudit getLatestAuditForRepository(Long repositoryId) {
        return codeAuditRepository.findTopByRepositoryIdOrderByStartedAtDesc(repositoryId)
            .orElse(null);
    }
}
