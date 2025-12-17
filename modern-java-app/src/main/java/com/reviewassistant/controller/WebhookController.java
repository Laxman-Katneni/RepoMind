package com.reviewassistant.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewassistant.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Controller for handling GitHub webhooks.
 * Listens for pull_request events and triggers automated code reviews.
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    
    private final ReviewService reviewService;
    private final ObjectMapper objectMapper;
    
    @Value("${github.webhook.secret}")
    private String webhookSecret;
    
    public WebhookController(ReviewService reviewService, ObjectMapper objectMapper) {
        this.reviewService = reviewService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * GitHub webhook endpoint for pull request events.
     * 
     * @param signature GitHub signature header (X-Hub-Signature-256)
     * @param event GitHub event type header (X-GitHub-Event)
     * @param payload Raw JSON payload from GitHub
     * @return HTTP 200 on success, 401 on signature validation failure
     */
    @PostMapping("/github")
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestBody String payload) {
        
        logger.info("Received GitHub webhook event: {}", event);
        
        // Validate signature
        if (!validateSignature(payload, signature)) {
            logger.warn("Invalid webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }
        
        // Only process pull_request events
        if (!"pull_request".equals(event)) {
            logger.info("Ignoring non-pull_request event: {}", event);
            return ResponseEntity.ok("Event ignored");
        }
        
        try {
            JsonNode json = objectMapper.readTree(payload);
            String action = json.get("action").asText();
            
            // Only process opened and synchronize events
            if (!"opened".equals(action) && !"synchronize".equals(action)) {
                logger.info("Ignoring PR action: {}", action);
                return ResponseEntity.ok("Action ignored");
            }
            
            // Extract PR details
            JsonNode repo = json.get("repository");
            JsonNode pr = json.get("pull_request");
            
            String owner = repo.get("owner").get("login").asText();
            String repoName = repo.get("name").asText();
            Integer prNumber = pr.get("number").asInt();
            String headSha = pr.get("head").get("sha").asText();
            Long repoId = repo.get("id").asLong(); // Note: This is GitHub's ID, not our DB ID
            
            logger.info("Processing PR #{} in {}/{} (commit: {})", prNumber, owner, repoName, headSha);
            
            // Trigger async review (GitHub webhook times out after 10 seconds)
            processWebhookAsync(repoId, prNumber, headSha);
            
            return ResponseEntity.ok("Review triggered");
            
        } catch (Exception e) {
            logger.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook");
        }
    }
    
    /**
     * Process webhook asynchronously to avoid GitHub timeout.
     * Note: This is a placeholder - actual implementation would need to:
     * 1. Map GitHub repo ID to our database repo ID
     * 2. Get GitHub token from secure storage
     * 3. Call reviewService.analyzePr()
     */
    @Async
    public void processWebhookAsync(Long githubRepoId, Integer prNumber, String headSha) {
        logger.info("Async processing webhook for PR #{} with commit {}", prNumber, headSha);
        
        // TODO: Implement actual logic
        // 1. Look up repository in database by GitHub ID or owner/name
        // 2. Retrieve stored GitHub token for this repository
        // 3. Call reviewService.analyzePr(repoId, prNumber, token)
        // 4. Optionally call reviewService.publishReview(runId) to post back to GitHub
        
        logger.warn("Webhook processing not fully implemented yet - requires repo/token mapping");
    }
    
    /**
     * Validate GitHub webhook signature using HMAC SHA256.
     * 
     * @param payload Raw request body
     * @param signature GitHub signature (sha256=...)
     * @return true if signature is valid
     */
    private boolean validateSignature(String payload, String signature) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }
        
        try {
            String expectedSignature = signature.substring(7); // Remove "sha256=" prefix
            
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), 
                    HMAC_ALGORITHM
            );
            mac.init(secretKey);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String actualSignature = HexFormat.of().formatHex(hash);
            
            return actualSignature.equals(expectedSignature);
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error validating signature: {}", e.getMessage());
            return false;
        }
    }
}
