package com.reviewassistant.service;

import com.reviewassistant.exception.AiProcessingException;
import com.reviewassistant.model.CodeChunk;
import com.reviewassistant.model.Repository;
import com.reviewassistant.repository.CodeChunkRepository;
import com.reviewassistant.repository.RepositoryRepository;
import com.reviewassistant.service.dto.GitHubFileDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for RAG (Retrieval Augmented Generation) operations.
 * Handles code indexing with embeddings and semantic search retrieval.
 */
@Service
public class RagService {
    
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    private static final int CHUNK_SIZE = 1000;  // characters per chunk
    private static final int CHUNK_OVERLAP = 200;  // overlap between chunks
    private static final int RETRIEVAL_LIMIT = 5;  // number of chunks to retrieve
    
    private final GithubService githubService;
    private final CodeChunkRepository codeChunkRepository;
    private final RepositoryRepository repositoryRepository;
    private final EmbeddingModel embeddingModel;
    private final TokenTextSplitter textSplitter;
    
    public RagService(
            GithubService githubService,
            CodeChunkRepository codeChunkRepository,
            RepositoryRepository repositoryRepository,
            EmbeddingModel embeddingModel) {
        this.githubService = githubService;
        this.codeChunkRepository = codeChunkRepository;
        this.repositoryRepository = repositoryRepository;
        this.embeddingModel = embeddingModel;
        this.textSplitter = new TokenTextSplitter(CHUNK_SIZE, CHUNK_OVERLAP, 5, 10000, true);
    }
    
    /**
     * Index a repository by fetching all files, chunking them, and creating embeddings.
     * Retries on transient AI processing failures.
     * 
     * @param repoId Database ID of the repository
     * @param token GitHub OAuth access token
     * @throws GithubException if GitHub API fails
     * @throws AiProcessingException if embedding generation fails
     */
    @Transactional
    @Retryable(
            retryFor = {AiProcessingException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    public void indexRepository(Long repoId, String token) {
        logger.info("Starting indexing for repository ID: {}", repoId);
        
        // Fetch repository from database
        Repository repo = repositoryRepository.findById(repoId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repoId));
        
        // Fetch all files from GitHub
        List<GitHubFileDto> files = githubService.fetchRepositoryFiles(
                token, repo.getOwner(), repo.getName());
        
        logger.info("Found {} files to index in repository {}/{}", 
                files.size(), repo.getOwner(), repo.getName());
        
        // Filter code files (exclude binaries, images, etc.)
        List<GitHubFileDto> codeFiles = files.stream()
                .filter(this::isCodeFile)
                .collect(Collectors.toList());
        
        logger.info("Filtering to {} code files", codeFiles.size());
        
        int processedFiles = 0;
        int totalChunks = 0;
        List<CodeChunk> chunkBatch = new ArrayList<>();
        final int BATCH_SIZE = 50;
        
        // Process each file
        for (GitHubFileDto file : codeFiles) {
            try {
                // Fetch file content
                String content = githubService.fetchFileContent(
                        token, repo.getOwner(), repo.getName(), file.path);
                
                if (content == null || content.trim().isEmpty()) {
                    continue;
                }
                
                // Detect language from file extension
                String language = detectLanguage(file.path);
                
                // Split content into chunks
                List<Document> documents = textSplitter.split(
                        new Document(content)
                );
                
                // Process each chunk
                for (int i = 0; i < documents.size(); i++) {
                    Document doc = documents.get(i);
                    String chunkText = doc.getText();
                    
                    try {
                        // Generate embedding (returns float[])
                        float[] embedding = embeddingModel.embed(chunkText);
                        
                        // Create code chunk
                        CodeChunk chunk = new CodeChunk();
                        chunk.setRepository(repo);
                        chunk.setFilePath(file.path);
                        chunk.setLanguage(language);
                        chunk.setStartLine(i * CHUNK_SIZE);  // Approximate
                        chunk.setEndLine((i + 1) * CHUNK_SIZE);
                        chunk.setContent(chunkText);
                        chunk.setEmbedding(embedding);
                        
                        // Add to batch instead of immediate save
                        chunkBatch.add(chunk);
                        totalChunks++;
                        
                        // Save batch when it reaches BATCH_SIZE
                        if (chunkBatch.size() >= BATCH_SIZE) {
                            codeChunkRepository.saveAll(chunkBatch);
                            logger.debug("Saved batch of {} chunks", chunkBatch.size());
                            chunkBatch.clear();
                        }
                        
                    } catch (Exception e) {
                        logger.error("Error generating embedding for chunk in file {}: {}", file.path, e.getMessage());
                        throw new AiProcessingException("Failed to generate embedding: " + e.getMessage(), e);
                    }
                }
                
                processedFiles++;
                
                if (processedFiles % 10 == 0) {
                    logger.info("Processed {}/{} files, created {} chunks", 
                            processedFiles, codeFiles.size(), totalChunks);
                }
                
            } catch (Exception e) {
                logger.error("Error processing file {}: {}", file.path, e.getMessage());
            }
        }
        
        // Save remaining chunks in batch
        if (!chunkBatch.isEmpty()) {
            codeChunkRepository.saveAll(chunkBatch);
            logger.info("Saved final batch of {} chunks", chunkBatch.size());
        }
        
        logger.info("Indexing complete for repository {}. Processed {} files, created {} chunks",
                repoId, processedFiles, totalChunks);
    }
    
    /**
     * Retrieve similar code chunks for a query using semantic search.
     * Returns lightweight snippet objects without embedding data.
     * 
     * @param query Natural language query
     * @param repoId Repository ID to search within
     * @return List of most similar code chunks (without embeddings)
     */
    public List<CodeChunk> retrieve(String query, Long repoId) {
        logger.info("Retrieving chunks for query: '{}' in repository {}", query, repoId);
        
        // Generate embedding for the query (returns float[])
        float[] queryEmbedding = embeddingModel.embed(query);
        
        // Convert embedding to Postgres vector string format: '[0.1, 0.2, 0.3]'
        StringBuilder vectorString = new StringBuilder("[");
        for (int i = 0; i < queryEmbedding.length; i++) {
            if (i > 0) vectorString.append(",");
            vectorString.append(queryEmbedding[i]);
        }
        vectorString.append("]");
        
        // Query database for similar chunks (using projection to avoid fetching vector)
        List<CodeChunkRepository.ChunkSnippet> snippets = codeChunkRepository.findSimilarChunks(
                repoId, vectorString.toString(), RETRIEVAL_LIMIT);
        
        // Convert snippets to CodeChunk objects (without embedding)
        List<CodeChunk> results = snippets.stream()
                .map(snippet -> {
                    CodeChunk chunk = new CodeChunk();
                    chunk.setContent(snippet.getContent());
                    chunk.setFilePath(snippet.getFilePath());
                    chunk.setStartLine(snippet.getStartLine());
                    chunk.setEndLine(snippet.getEndLine());
                    chunk.setLanguage(snippet.getLanguage());
                    return chunk;
                })
                .collect(java.util.stream.Collectors.toList());
        
        logger.info("Retrieved {} similar chunks", results.size());
        return results;
    }
    
    /**
     * Check if a file is a code file based on extension.
     */
    private boolean isCodeFile(GitHubFileDto file) {
        String path = file.path.toLowerCase();
        
        // Exclude certain directories
        if (path.contains("/node_modules/") || 
            path.contains("/vendor/") ||
            path.contains("/.git/") ||
            path.contains("/dist/") ||
            path.contains("/build/")) {
            return false;
        }
        
        // Include common code file extensions
        return path.endsWith(".java") || path.endsWith(".py") || path.endsWith(".js") ||
               path.endsWith(".ts") || path.endsWith(".jsx") || path.endsWith(".tsx") ||
               path.endsWith(".go") || path.endsWith(".rs") || path.endsWith(".cpp") ||
               path.endsWith(".c") || path.endsWith(".h") || path.endsWith(".cs") ||
               path.endsWith(".rb") || path.endsWith(".php") || path.endsWith(".swift") ||
               path.endsWith(".kt") || path.endsWith(".scala") || path.endsWith(".sql");
    }
    
    /**
     * Detect programming language from file extension.
     */
    private String detectLanguage(String filePath) {
        String lower = filePath.toLowerCase();
        
        if (lower.endsWith(".java")) return "java";
        if (lower.endsWith(".py")) return "python";
        if (lower.endsWith(".js")) return "javascript";
        if (lower.endsWith(".ts")) return "typescript";
        if (lower.endsWith(".jsx") || lower.endsWith(".tsx")) return "react";
        if (lower.endsWith(".go")) return "go";
        if (lower.endsWith(".rs")) return "rust";
        if (lower.endsWith(".cpp") || lower.endsWith(".cc")) return "cpp";
        if (lower.endsWith(".c")) return "c";
        if (lower.endsWith(".h") || lower.endsWith(".hpp")) return "c-header";
        if (lower.endsWith(".cs")) return "csharp";
        if (lower.endsWith(".rb")) return "ruby";
        if (lower.endsWith(".php")) return "php";
        if (lower.endsWith(".swift")) return "swift";
        if (lower.endsWith(".kt")) return "kotlin";
        if (lower.endsWith(".scala")) return "scala";
        if (lower.endsWith(".sql")) return "sql";
        
        return "unknown";
    }
}
