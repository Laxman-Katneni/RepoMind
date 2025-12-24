package com.reviewassistant.repository;

import com.reviewassistant.model.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for ReviewComment entity.
 * Provides database access for review comments.
 */
@Repository
public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {
    
    /**
     * Count comments by repository and severity level.
     * 
     * @param repoId Repository ID
     * @param severity Severity level (e.g., "critical", "warning", "info")
     * @return Count of comments matching criteria
     */
    Integer countByReviewRun_PullRequest_Repository_IdAndSeverity(Long repoId, String severity);
    // Standard CRUD operations from JpaRepository
}
