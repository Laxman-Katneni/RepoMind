package com.reviewassistant.repository;

import com.reviewassistant.model.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for PullRequest entity.
 * Provides database access for pull requests.
 */
@Repository
public interface PullRequestRepository extends JpaRepository<PullRequest, Long> {
    
    /**
     * Find all pull requests for a given repository.
     *
     * @param repositoryId the repository ID
     * @return list of pull requests
     */
    List<PullRequest> findByRepositoryId(Long repositoryId);
    
    /**
     * Find a specific pull request by repository ID and PR number.
     *
     * @param repositoryId the repository ID
     * @param number the pull request number
     * @return Optional containing the pull request if found
     */
    Optional<PullRequest> findByRepositoryIdAndNumber(Long repositoryId, Integer number);
}
