package com.reviewassistant.repository;

import com.reviewassistant.model.CodeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for CodeChunk entity.
 * Provides database access for code chunks with vector similarity search.
 */
public interface CodeChunkRepository extends JpaRepository<CodeChunk, Long> {
    
    /**
     * Projection interface to fetch only necessary fields (excludes vector column).
     */
    interface ChunkSnippet {
        String getContent();
        String getFilePath();
        Integer getStartLine();
        Integer getEndLine();
        String getLanguage();
    }
    
    /**
     * Find similar code chunks using vector similarity search.
     * Uses projection to avoid fetching the vector column which can crash JDBC driver.
     * 
     * @param repositoryId Repository ID to search within
     * @param queryEmbedding Query embedding as vector string '[0.1, 0.2, ...]'
     * @param limit Maximum number of results
     * @return List of chunk snippets (without embedding data)
     */
    @Query(value = "SELECT content, file_path as filePath, start_line as startLine, " +
                   "end_line as endLine, language " +
                   "FROM code_chunks " +
                   "WHERE repository_id = :repositoryId " +
                   "ORDER BY embedding <-> cast(:queryEmbedding as vector) " +
                   "LIMIT :limit", 
           nativeQuery = true)
    List<ChunkSnippet> findSimilarChunks(
            @Param("repositoryId") Long repositoryId,
            @Param("queryEmbedding") String queryEmbedding,
            @Param("limit") int limit);
}
