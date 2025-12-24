package com.reviewassistant.controller;

import com.reviewassistant.model.Repository;
import com.reviewassistant.repository.PullRequestRepository;
import com.reviewassistant.repository.ReviewCommentRepository;
import com.reviewassistant.repository.ReviewRunRepository;
import com.reviewassistant.service.GithubService;
import com.reviewassistant.service.dto.DashboardMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.reviewassistant.model.PullRequest;

/**
 * REST controller for repository operations.
 * Uses OAuth2 session-based authentication to access GitHub API.
 */
@RestController
@RequestMapping("/api/repos")
public class RepositoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(RepositoryController.class);
    
    private final GithubService githubService;
    private final ReviewRunRepository reviewRunRepository;
    private final ReviewCommentRepository reviewCommentRepository;
    private final PullRequestRepository pullRequestRepository;
    private final com.reviewassistant.service.RagService ragService;
    
    public RepositoryController(
            GithubService githubService,
            ReviewRunRepository reviewRunRepository,
            ReviewCommentRepository reviewCommentRepository,
            PullRequestRepository pullRequestRepository,
            com.reviewassistant.service.RagService ragService) {
        this.githubService = githubService;
        this.reviewRunRepository = reviewRunRepository;
        this.reviewCommentRepository = reviewCommentRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.ragService = ragService;
    }
    
    /**
     * Get all repositories for the authenticated user.
     * Token is extracted from OAuth2 session instead of Authorization header.
     * 
     * @param authorizedClient OAuth2 authorized client from session
     * @return List of repositories
     */
    @GetMapping
    public ResponseEntity<List<Repository>> getUserRepositories(
            @RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient authorizedClient) {
        
        logger.info("Fetching repositories for authenticated user");
        
        // Extract token from OAuth2 session
        String token = authorizedClient.getAccessToken().getTokenValue();
        
        List<Repository> repositories = githubService.fetchUserRepositories(token);
        
        logger.info("Found {} repositories", repositories.size());
        
        return ResponseEntity.ok(repositories);
    }
    
    /**
     * Get dashboard metrics for a specific repository.
     * 
     * @param repoId Repository database ID
     * @return Dashboard metrics including review stats
     */
    @GetMapping("/{repoId}/metrics")
    public ResponseEntity<DashboardMetrics> getRepositoryMetrics(@PathVariable Long repoId) {
        
        logger.info("Fetching metrics for repository {}", repoId);
        
        // Total reviews
        Long totalReviews = reviewRunRepository.countByPullRequest_Repository_Id(repoId);
        
        // Critical issues count (severity = "critical")
        Integer criticalIssuesCount = reviewCommentRepository
                .countByReviewRun_PullRequest_Repository_IdAndSeverity(repoId, "critical");
        
        // Average review time (placeholder - would need timestamps)
        Double averageReviewTime = 2.3; // seconds (dummy for now)
        
        // Reviews per day (last 7 days)
        Map<String, Integer> reviewsPerDay = new HashMap<>();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        
        Long recentReviews = reviewRunRepository
                .countByPullRequest_Repository_IdAndCreatedAtAfter(repoId, sevenDaysAgo);
        
        // Simple distribution (would be more sophisticated in production)
        reviewsPerDay.put("Mon", (int) (recentReviews / 7));
        reviewsPerDay.put("Tue", (int) (recentReviews / 7));
        reviewsPerDay.put("Wed", (int) (recentReviews / 7));
        reviewsPerDay.put("Thu", (int) (recentReviews / 7));
        reviewsPerDay.put("Fri", (int) (recentReviews / 7));
        reviewsPerDay.put("Sat", (int) (recentReviews / 7));
        reviewsPerDay.put("Sun", (int) (recentReviews / 7));
        
        DashboardMetrics metrics = new DashboardMetrics(
                totalReviews,
                criticalIssuesCount,
                averageReviewTime,
                reviewsPerDay
        );
        
        logger.info("Metrics: {} total reviews, {} critical issues", totalReviews, criticalIssuesCount);
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Manually sync pull requests from GitHub for a repository.
     * Useful for populating data without webhooks during local development.
     * 
     * @param id Repository database ID
     * @param authorizedClient OAuth2 authorized client from session
     * @return Success message with count of synced PRs
     */
    @PostMapping("/{id}/sync")
    public ResponseEntity<Map<String, Object>> syncRepository(
            @PathVariable Long id,
            @RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient authorizedClient) {
        
        logger.info("Syncing pull requests for repository ID: {}", id);
        
        // Extract token from OAuth2 session
        String token = authorizedClient.getAccessToken().getTokenValue();
        
        List<PullRequest> syncedPrs = githubService.syncPullRequests(id, token);
        
        logger.info("Synced {} pull requests", syncedPrs.size());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Successfully synced pull requests");
        response.put("count", syncedPrs.size());
        response.put("repositoryId", id);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all pull requests for a repository.
     * 
     * @param id Repository database ID
     * @return List of pull requests
     */
    @GetMapping("/{id}/pull-requests")
    public ResponseEntity<List<PullRequest>> getPullRequests(@PathVariable Long id) {
        logger.info("Fetching pull requests for repository ID: {}", id);
        
        List<PullRequest> pullRequests = pullRequestRepository.findByRepositoryId(id);
        
        logger.info("Found {} pull requests", pullRequests.size());
        
        return ResponseEntity.ok(pullRequests);
    }
    
    /**
     * Index repository for RAG (Retrieval Augmented Generation).
     * Fetches all code files, chunks them, and generates embeddings.
     * This enables the chat feature to answer questions about the codebase.
     * 
     * @param repoId Repository database ID
     * @param authorizedClient OAuth2 authorized client from session
     * @return Success message
     */
    @PostMapping("/{repoId}/index")
    public ResponseEntity<Map<String, String>> indexRepository(
            @PathVariable Long repoId,
            @RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient authorizedClient) {
        
        logger.info("Starting indexing for repository {}", repoId);
        
        // Extract token from OAuth2 session
        String token = authorizedClient.getAccessToken().getTokenValue();
        
        try {
            // Trigger repository indexing
            ragService.indexRepository(repoId, token);
            
            logger.info("Successfully indexed repository {}", repoId);
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Repository indexed successfully. You can now chat about the code!"
            ));
            
        } catch (Exception e) {
            logger.error("Failed to index repository {}: {}", repoId, e.getMessage(), e);
            
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "Failed to index repository: " + e.getMessage()
            ));
        }
    }
}
