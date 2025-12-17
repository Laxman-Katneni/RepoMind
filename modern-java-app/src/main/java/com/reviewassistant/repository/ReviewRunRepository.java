package com.reviewassistant.repository;

import com.reviewassistant.model.ReviewRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for ReviewRun entity.
 * Provides database access for review runs.
 */
@Repository
public interface ReviewRunRepository extends JpaRepository<ReviewRun, Long> {
    
    /**
     * Find all review runs for a given pull request.
     * Used for smart caching - check if commit was already reviewed.
     * 
     * @param pullRequestId Pull request ID
     * @return List of review runs for the PR
     */
    List<ReviewRun> findByPullRequestId(Long pullRequestId);
    
    /**
     * Count total review runs for a repository.
     * 
     * @param repoId Repository ID
     * @return Count of review runs
     */
    Long countByPullRequest_Repository_Id(Long repoId);
    
    /**
     * Count review runs for a repository created after a specific date.
     * 
     * @param repoId Repository ID
     * @param date Cutoff date
     * @return Count of recent review runs
     */
    Long countByPullRequest_Repository_IdAndCreatedAtAfter(Long repoId, LocalDateTime date);
    // Standard CRUD operations from JpaRepository
}
