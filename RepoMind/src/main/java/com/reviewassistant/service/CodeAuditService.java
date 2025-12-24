package com.reviewassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewassistant.controller.dto.AuditNotification;
import com.reviewassistant.model.AuditFinding;
import com.reviewassistant.model.CodeAudit;
import com.reviewassistant.model.CodeChunk;
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

    // Performance optimization constants (Level 4: Ultra-Aggressive for free tier)
    private static final int MAX_FILE_SIZE_KB = 10; // Skip files larger than 10KB (smaller files = faster)
    private static final int MAX_FILES_PER_AUDIT = 10; // Limit to 10 most critical files
    private static final int PARALLEL_BATCH_SIZE = 2; // Process 2 files in parallel (smaller batches)

    // Directories and files to skip (blocklist)
    private static final Set<String> SKIP_DIRECTORIES = Set.of(
        "node_modules", "target", ".git", "build", "dist", ".idea",
        "coverage", ".vscode", "__pycache__", "vendor", ".gradle",
        "test", "tests", "__tests__", "spec", "specs", // Skip test directories
        "docs", "documentation", "examples" // Skip documentation
    );

    // High priority folders (analyze first) - language-agnostic
    private static final Set<String> HIGH_PRIORITY_FOLDERS = Set.of(
        "src", "lib", "app", "services", "service", 
        "controllers", "controller", "models", "model",
        "api", "routes", "handlers", "core"
    );

    // Level 3: ONLY code files (skip styling, docs, configs)
    private static final Set<String> AUDITABLE_EXTENSIONS = Set.of(
        ".java", ".py", ".js", ".ts", ".tsx", ".jsx",
        ".go", ".rs", ".rb", ".php", ".c", ".cpp", ".h"
        // Excluded: .css, .html, .json, .md, .xml, .yml
    );

    // Level 4: Ultra-strict - skip everything except critical patterns
    private static final Set<String> SKIP_UI_PATTERNS = Set.of(
        "loading", "not-found", "error", "404", "500",
        "layout", "_app", "_document",
        "-card", "-badge", "-avatar", "-icon", "-button",
        "-chart", "-graph", "-skeleton", "-spinner",
        "component", "util", "helper", "constant", "config",
        "type", "interface", "enum", "dto"
    );

    // Level 4: ONLY security-critical files (ultra-strict)
    private static final Set<String> CRITICAL_PATTERNS = Set.of(
        // API & Routes
        "api/", "route", "endpoint", "view", "handler",
        // Actions & Server Logic  
        "action", "server", "service",
        // Authentication & Security
        "auth", "login", "signin", "signup", "password",
        "middleware", "guard", "protect", "security",
        // Data & Validation
        "schema", "validation", "validator", "form",
        "model", "database", "prisma", "query",
        // Critical Business Logic
        "payment", "checkout", "billing", "stripe",
        "upload", "file", "storage",
        // Python-specific
        "views.py", "urls.py", "models.py", "serializers.py", "settings.py"
    );

    private final CodeAuditRepository codeAuditRepository;
    private final AuditFindingRepository auditFindingRepository;
    private final RepositoryRepository repositoryRepository;
    private final com.reviewassistant.service.audit.AuditService auditService;
    private final GithubService githubService;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final RagService ragService;

    public CodeAuditService(
            CodeAuditRepository codeAuditRepository,
            AuditFindingRepository auditFindingRepository,
            RepositoryRepository repositoryRepository,
            @org.springframework.beans.factory.annotation.Qualifier("activeAuditService")  // Configured via audit.service.mode property
            com.reviewassistant.service.audit.AuditService auditService,
            GithubService githubService,
            ObjectMapper objectMapper,
            SimpMessagingTemplate messagingTemplate,
            RagService ragService) {
        this.codeAuditRepository = codeAuditRepository;
        this.auditFindingRepository = auditFindingRepository;
        this.repositoryRepository = repositoryRepository;
        this.auditService = auditService;
        this.githubService = githubService;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
        this.ragService = ragService;
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
     * Asynchronously processes the audit with PARALLEL batch processing.
     * Includes repo structure context for better architectural insights.
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

            // Fetch all files from GitHub
            List<String> allFilePaths = githubService.getRepositoryFilePaths(
                repository.getOwner(),
                repository.getName(),
                accessToken
            );

            logger.info("Fetched {} total files from repository", allFilePaths.size());

            // Filter, prioritize, and limit files
            List<String> auditableFiles = filterAndPrioritizeFiles(allFilePaths);
            
            logger.info("Selected {} files for audit (from {} total)", 
                auditableFiles.size(), allFilePaths.size());

            audit.setTotalFilesScanned(0);
            codeAuditRepository.save(audit);

            int totalFiles = auditableFiles.size();
            int processed = 0;
            int failedCount = 0;

            // PARALLEL BATCH PROCESSING with RAG context
            for (int i = 0; i < totalFiles; i += PARALLEL_BATCH_SIZE) {
                int endIndex = Math.min(i + PARALLEL_BATCH_SIZE, totalFiles);
                List<String> batch = auditableFiles.subList(i, endIndex);
                
                logger.info("Processing batch {}-{} of {}", i+1, endIndex, totalFiles);

                // Process batch in parallel
                List<FileScanResult> batchResults = batch.parallelStream()
                    .map(filePath -> {
                        try {
                            logger.debug("Scanning file: {}", filePath);

                            // Fetch file content
                            String content = githubService.getFileContent(
                                repository.getOwner(),
                                repository.getName(),
                                filePath,
                                accessToken
                            );

                            // Check file size
                            if (content.length() > MAX_FILE_SIZE_KB * 1024) {
                                logger.warn("Skipping large file: {} ({} KB)", 
                                    filePath, content.length() / 1024);
                                return new FileScanResult(filePath, null, "File too large", true);
                            }

                            // Detect language
                            String language = detectLanguage(filePath);

                            // Build RAG query from file content + path
                            String ragQuery = buildRagQuery(content, filePath, language);
                            
                            // Retrieve relevant code chunks using RAG
                            String relevantContext = retrieveRelevantContext(
                                ragQuery, 
                                repository.getId()
                            );

                            // Analyze with multi-tier audit service (with intelligent RAG context)
                            AuditResult result = auditService.analyzeCode(
                                content, language, filePath, relevantContext
                            );

                            return new FileScanResult(filePath, result, null, true);

                        } catch (Exception e) {
                            logger.error("Failed to scan {}: {}", filePath, e.getMessage());
                            return new FileScanResult(filePath, null, e.getMessage(), true);
                        }
                    })
                    .toList();

                // Save results from batch
                for (FileScanResult scanResult : batchResults) {
                    processed++;
                    
                    // Update progress
                    audit.updateProgress(processed, totalFiles, scanResult.filePath);
                    
                    if (scanResult.success) {
                        if (scanResult.result != null) {
                            // Save finding
                            saveFinding(audit, scanResult.filePath, "", scanResult.result);
                            audit.incrementCounts(scanResult.result.severity());
                            audit.setTotalFilesScanned(audit.getTotalFilesScanned() + 1);
                            
                            if (audit.getFilesWithIssues() == null) {
                                audit.setFilesWithIssues(0);
                            }
                            audit.setFilesWithIssues(audit.getFilesWithIssues() + 1);
                        }
                    } else {
                        logger.warn("Scan failed for {}: {}", scanResult.filePath, scanResult.error);
                        failedCount++;
                    }
                }

                // Save progress after each batch
                codeAuditRepository.save(audit);
            }

            // Mark as completed
            if (failedCount == 0) {
                audit.markCompleted();
            } else if (failedCount < totalFiles) {
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
     * Helper record for file scan results
     */
    private record FileScanResult(
        String filePath,
        AuditResult result,
        String error,
        boolean success
    ) {}

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
     * Builds an intelligent RAG query from file content.
     * Extracts imports, class names, and key patterns to find relevant context.
     */
    private String buildRagQuery(String fileContent, String filePath, String language) {
        StringBuilder query = new StringBuilder();
        
        // Add file name and path context
        String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        query.append(fileName).append(" ");
        
        // Extract imports and dependencies
        List<String> imports = extractImports(fileContent, language);
        if (!imports.isEmpty()) {
            query.append("imports: ").append(String.join(" ", imports)).append(" ");
        }
        
        // Extract class/interface names
        String className = extractClassName(fileContent, language);
        if (className != null) {
            query.append("class: ").append(className).append(" ");
        }
        
        // Add language-specific keywords
        query.append(language).append(" ");
        
        // Add architectural keywords based on file location  
        if (filePath.contains("controller") || filePath.contains("Controller")) {
            query.append("API endpoint HTTP request response ");
        } else if (filePath.contains("service") || filePath.contains("Service")) {
            query.append("business logic service layer ");
        } else if (filePath.contains("repository") || filePath.contains("Repository")) {
            query.append("database query persistence ");
        } else if (filePath.contains("model") || filePath.contains("entity")) {
            query.append("data model entity schema ");
        }
        
        return query.toString().trim();
    }

    /**
     * Extracts import statements from code to understand dependencies.
     */
    private List<String> extractImports(String content, String language) {
        List<String> imports = new ArrayList<>();
        
        try {
            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                
                // Java/TypeScript imports
                if (language.equals("Java") || language.equals("TypeScript")) {
                    if (line.startsWith("import ") && !line.contains("*")) {
                        String imported = line.replace("import ", "")
                                             .replace(";", "")
                                             .trim();
                        if (imported.contains(".")) {
                            String className = imported.substring(imported.lastIndexOf('.') + 1);
                            imports.add(className);
                        }
                    }
                }
                // Python imports
                else if (language.equals("Python")) {
                    if (line.startsWith("from ") || line.startsWith("import ")) {
                        String[] parts = line.split(" ");
                        for (String part : parts) {
                            if (!part.equals("from") && !part.equals("import") && 
                                !part.equals("as") && !part.contains(".")) {
                                imports.add(part.replace(",", ""));
                            }
                        }
                    }
                }
                
                // Limit to first 20 imports to keep query concise
                if (imports.size() >= 20) break;
            }
        } catch (Exception e) {
            logger.debug("Error extracting imports: {}", e.getMessage());
        }
        
        return imports;
    }

    /**
     * Extracts class name from code content.
     */
    private String extractClassName(String content, String language) {
        try {
            String[] lines = content.split("\n");
            for (String line : lines) {
                line = line.trim();
                
                // Java/TypeScript class
                if (language.equals("Java") || language.equals("TypeScript")) {
                    if (line.contains("class ") || line.contains("interface ")) {
                        String[] parts = line.split("\\s+");
                        for (int i = 0; i < parts.length - 1; i++) {
                            if (parts[i].equals("class") || parts[i].equals("interface")) {
                                return parts[i + 1].replace("{", "").trim();
                            }
                        }
                    }
                }
                // Python class
                else if (language.equals("Python")) {
                    if (line.startsWith("class ")) {
                        return line.replace("class ", "")
                                  .split("[:\\(]")[0]
                                  .trim();
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Error extracting class name: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Retrieves relevant code context using RAG semantic search.
     * Returns formatted context string with related code chunks.
     */
    private String retrieveRelevantContext(String query, Long repositoryId) {
        try {
            // Retrieve similar code chunks
            List<CodeChunk> chunks = ragService.retrieve(query, repositoryId);
            
            if (chunks.isEmpty()) {
                return "No related code found in repository.";
            }
            
            StringBuilder context = new StringBuilder();
            context.append("Related code in repository:\n\n");
            
            for (int i = 0; i < chunks.size(); i++) {
                CodeChunk chunk = chunks.get(i);
                context.append(String.format("// %s (lines %d-%d)\n",
                    chunk.getFilePath(),
                    chunk.getStartLine(),
                    chunk.getEndLine()));
                
                // Truncate very long chunks
                String chunkContent = chunk.getContent();
                if (chunkContent.length() > 500) {
                    chunkContent = chunkContent.substring(0, 500) + "\n// ... (truncated)";
                }
                
                context.append(chunkContent);
                context.append("\n\n");
                
                // Limit context to avoid token overflow
                if (context.length() > 2000) {
                    context.append("// ... (more related code available)");
                    break;
                }
            }
            
            return context.toString();
            
        } catch (Exception e) {
            logger.error("Error retrieving RAG context: {}", e.getMessage());
            return "Error retrieving related code context.";
        }
    }

    /**
     * Filters, prioritizes, and limits files for audit.
     * Uses language-agnostic folder patterns for prioritization.
     */
    private List<String> filterAndPrioritizeFiles(List<String> allFiles) {
        // Step 1: Filter auditable files
        List<String> filtered = allFiles.stream()
            .filter(this::isAuditableFile)
            .toList();

        logger.info("Filtered {} auditable files from {} total", filtered.size(), allFiles.size());

        // Step 2: Prioritize by folder patterns
        List<String> highPriority = new ArrayList<>();
        List<String> normalPriority = new ArrayList<>();

        for (String file : filtered) {
            if (isHighPriorityFile(file)) {
                highPriority.add(file);
            } else {
                normalPriority.add(file);
            }
        }

        logger.info("Prioritized: {} high priority, {} normal priority", 
            highPriority.size(), normalPriority.size());

        // Step 3: Combine and limit total files
        List<String> result = new ArrayList<>();
        result.addAll(highPriority);
       
        int remaining = MAX_FILES_PER_AUDIT - result.size();
        if (remaining > 0 && !normalPriority.isEmpty()) {
            result.addAll(normalPriority.subList(0, Math.min(remaining, normalPriority.size())));
        }

        // Limit to MAX_FILES_PER_AUDIT
        if (result.size() > MAX_FILES_PER_AUDIT) {
            result = result.subList(0, MAX_FILES_PER_AUDIT);
        }

        logger.info("Final selection: {} files (max: {})", result.size(), MAX_FILES_PER_AUDIT);
        return result;
    }

    /**
     * Checks if file is in high-priority folder (language-agnostic).
     */
    private boolean isHighPriorityFile(String filePath) {
        String lowerPath = filePath.toLowerCase();
        return HIGH_PRIORITY_FOLDERS.stream()
            .anyMatch(folder -> lowerPath.contains("/" + folder + "/") || 
                               lowerPath.startsWith(folder + "/"));
    }

    /**
     * Level 3 filtering: Aggressive filtering for critical files only.
     * Focuses on architecture & security audit needs.
     */
    private boolean isAuditableFile(String filePath) {
        String lowerPath = filePath.toLowerCase();
        
        // Step 1: Blocklist - skip bad directories
        for (String dir : SKIP_DIRECTORIES) {
            if (lowerPath.contains("/" + dir + "/") || lowerPath.contains("\\" + dir + "\\") ||
                lowerPath.startsWith(dir + "/") || lowerPath.startsWith(dir + "\\")) {
                return false;
            }
        }

        // Step 2: Extension check - must end with code extension
        boolean hasValidExtension = false;
        for (String ext : AUDITABLE_EXTENSIONS) {
            if (lowerPath.endsWith(ext)) {
                hasValidExtension = true;
                break;
            }
        }
        if (!hasValidExtension) {
            return false;
        }

        // Step 3: Level 3 - Skip UI-only patterns
        for (String pattern : SKIP_UI_PATTERNS) {
            if (lowerPath.contains(pattern)) {
                // Exception: Keep if it's a critical file
                boolean isCritical = CRITICAL_PATTERNS.stream()
                    .anyMatch(lowerPath::contains);
                if (!isCritical) {
                    return false; // Skip UI component
                }
            }
        }

        // Step 4: Prioritize critical files
        // If file doesn't match critical patterns, it's lower priority
        // Still include it if we have room, but it won't be prioritized
        
        return true;
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
            Integer lineNumber = extractLineNumber(result);
            finding.setLineNumber(lineNumber);

            // Extract code snippet
            String snippet = extractCodeSnippet(result);
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

    /**
     * Extracts line number from evidence array.
     * Looks for patterns like "@L123:" in the evidence.
     */
    private Integer extractLineNumber(AuditResult result) {
        if (result == null || result.getEvidence().isEmpty()) {
            return null;
        }
        
        for (String evidence : result.getEvidence()) {
            if (evidence.startsWith("@L")) {
                try {
                    int colonIndex = evidence.indexOf(":");
                    if (colonIndex > 2) {
                        String lineNum = evidence.substring(2, colonIndex);
                        return Integer.parseInt(lineNum);
                    }
                } catch (NumberFormatException e) {
                    // Continue to next evidence
                }
            }
        }
        
        return null;
    }

    /**
     * Extracts code snippet from evidence array.
     * Returns the first evidence that looks like code.
     */
    private String extractCodeSnippet(AuditResult result) {
        if (result == null || result.getEvidence().isEmpty()) {
            return null;
        }
        
        for (String evidence : result.getEvidence()) {
            // Remove line number prefix if present
            if (evidence.startsWith("@L")) {
                int colonIndex = evidence.indexOf(":");
                if (colonIndex > 0 && colonIndex < evidence.length() - 1) {
                    return evidence.substring(colonIndex + 1).trim();
                }
            } else if (evidence.length() > 10) {
                // Return first substantial evidence
                return evidence;
            }
        }
        
        return null;
    }
}
