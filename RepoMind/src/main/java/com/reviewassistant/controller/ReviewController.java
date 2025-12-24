package com.reviewassistant.controller;

import com.reviewassistant.model.ReviewRun;
import com.reviewassistant.repository.ReviewRunRepository;
import com.reviewassistant.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for pull request code reviews.
 * Uses OAuth2 session-based authentication to access GitHub API.
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);
    
    private final ReviewService reviewService;
    private final ReviewRunRepository reviewRunRepository;
    
    public ReviewController(ReviewService reviewService, ReviewRunRepository reviewRunRepository) {
        this.reviewService = reviewService;
        this.reviewRunRepository = reviewRunRepository;
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
   /* @PostMapping("/{repoId}/{prNumber}")
    public ResponseEntity<ReviewRun> analyzePullRequest(
            @PathVariable Long repoId,
            @PathVariable Integer prNumber,
            @RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient authorizedClient) {
        
        logger.info("Received review request for PR #{} in repository {}", prNumber, repoId);
        
        // Extract token from OAuth2 session
        String token = authorizedClient.getAccessToken().getTokenValue();
        
        // Note: This endpoint is currently disabled - use /run/{prId} instead
        // ReviewRun reviewRun = reviewService.analyzePr(repoId, prNumber, token);
        
        logger.info("Review completed successfully");
        
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }*/
    
    /**
     * Run AI review for a pull request by its database ID.
     * This endpoint is called from the frontend PR list.
     * 
     * @param prId Database ID of the pull request
     * @param authorizedClient OAuth2 authorized client from session
     * @return Saved ReviewRun with comments
     */
    @PostMapping("/run/{prId}")
    public ResponseEntity<ReviewRun> runReview(
            @PathVariable Long prId,
            @RegisteredOAuth2AuthorizedClient("github") OAuth2AuthorizedClient authorizedClient) {
        logger.info("Running AI review for PR ID: {}", prId);
        
        // Extract token from OAuth2 session
        String token = authorizedClient.getAccessToken().getTokenValue();
        
        ReviewRun reviewRun = reviewService.analyzePr(prId, token);
        
        logger.info("Review completed with {} comments", reviewRun.getCommentCount());
        
        return ResponseEntity.ok(reviewRun);
    }
    
    /**
     * Get all reviews for a pull request by PR database ID.
     * Returns list of reviews ordered by creation time.
     * 
     * @param prId Database ID of the pull request
     * @return List of ReviewRun entities
     */
    @GetMapping("/pr/{prId}")
    public ResponseEntity<List<ReviewRun>> getReviews(@PathVariable Long prId) {
        logger.info("Fetching reviews for PR ID: {}", prId);
        
        List<ReviewRun> reviews = reviewRunRepository.findByPullRequestId(prId);
        
        logger.info("Found {} reviews", reviews.size());
        
        return ResponseEntity.ok(reviews);
    }
}
