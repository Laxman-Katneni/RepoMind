package com.reviewassistant.repository;

import com.reviewassistant.model.DiffChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for DiffChunk entity.
 * Provides database access for diff chunks.
 */
@Repository
public interface DiffChunkRepository extends JpaRepository<DiffChunk, Long> {
    // Standard CRUD operations from JpaRepository
}
