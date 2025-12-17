package com.reviewassistant.controller;

import com.reviewassistant.model.ReviewRun;
import com.reviewassistant.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for pull request code reviews.
 * Uses OAuth2 session-based authentication to access GitHub API.
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);
    
    private final ReviewService reviewService;
    
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }
    
    /**
     * Analyze a pull request and create an AI-powered code review.
     * Token is extracted from OAuth2 session instead of Authorization header.
     * 
     * @param repoId Database ID of the repository
     * @param prNumber Pull request number
     * @param authorizedClient OAuth2 authorized client from session
     * @return Saved ReviewRun with comments
     */
    @PostMapping("/{repoId}/{prNumber}")
    public ResponseEntity<ReviewRun> analyzePullRequest(
            @PathVariable Long repoId,
            @PathVariable Integer prNumber,
            @RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient authorizedClient) {
        
        logger.info("Received review request for PR #{} in repository {}", prNumber, repoId);
        
        // Extract token from OAuth2 session
        String token = authorizedClient.getAccessToken().getTokenValue();
        
        ReviewRun reviewRun = reviewService.analyzePr(repoId, prNumber, token);
        
        logger.info("Review completed successfully with {} comments", reviewRun.getCommentCount());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewRun);
    }
}
