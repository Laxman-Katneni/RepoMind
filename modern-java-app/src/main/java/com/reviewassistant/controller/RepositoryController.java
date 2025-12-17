package com.reviewassistant.controller;

import com.reviewassistant.model.Repository;
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
    
    public RepositoryController(
            GithubService githubService,
            ReviewRunRepository reviewRunRepository,
            ReviewCommentRepository reviewCommentRepository) {
        this.githubService = githubService;
        this.reviewRunRepository = reviewRunRepository;
        this.reviewCommentRepository = reviewCommentRepository;
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
}
