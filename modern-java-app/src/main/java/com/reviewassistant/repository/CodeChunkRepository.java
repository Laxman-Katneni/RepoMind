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
@Repository
public interface CodeChunkRepository extends JpaRepository<CodeChunk, Long> {
    
    /**
     * Find code chunks by repository ID.
     * 
     * @param repoId Repository ID
     * @return List of code chunks for the repository
     */
    List<CodeChunk> findByRepositoryId(Long repoId);
    
    /**
     * Find similar code chunks using vector similarity search (cosine distance).
     * Uses pgvector's <-> operator for cosine distance.
     * 
     * @param repoId Repository ID to scope the search
     * @param queryVector The query embedding vector as a Postgres-compatible string
     * @param limit Maximum number of results
     * @return List of most similar code chunks
     */
    @Query(value = "SELECT * FROM code_chunks WHERE repository_id = :repoId " +
                   "ORDER BY embedding <-> CAST(:queryVector AS vector) " +
                   "LIMIT :limit", 
           nativeQuery = true)
    List<CodeChunk> findSimilarChunks(
            @Param("repoId") Long repoId,
            @Param("queryVector") String queryVector,
            @Param("limit") int limit
    );
}
