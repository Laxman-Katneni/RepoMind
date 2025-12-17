package com.reviewassistant.repository;

import com.reviewassistant.model.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for Repository entity.
 * Provides database access for Git repositories.
 */
@org.springframework.stereotype.Repository
public interface RepositoryRepository extends JpaRepository<Repository, Long> {
    
    /**
     * Find a repository by owner and name.
     * This combination should be unique.
     *
     * @param owner the repository owner
     * @param name the repository name
     * @return Optional containing the repository if found
     */
    Optional<Repository> findByOwnerAndName(String owner, String name);
}
