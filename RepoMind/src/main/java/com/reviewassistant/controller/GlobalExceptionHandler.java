package com.reviewassistant.controller;

import com.reviewassistant.exception.AiProcessingException;
import com.reviewassistant.exception.GithubException;
import com.reviewassistant.exception.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application.
 * Converts exceptions into proper HTTP responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle GitHub rate limit exceptions (429 Too Many Requests).
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitException(RateLimitException ex) {
        logger.warn("Rate limit exceeded: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Rate Limit Exceeded");
        body.put("message", ex.getMessage());
        body.put("retryAfterSeconds", ex.getRetryAfterSeconds());
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(body);
    }
    
    /**
     * Handle general GitHub API exceptions (502 Bad Gateway).
     */
    @ExceptionHandler(GithubException.class)
    public ResponseEntity<Map<String, Object>> handleGithubException(GithubException ex) {
        logger.error("GitHub API error: {}", ex.getMessage(), ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("error", "GitHub API Error");
        body.put("message", ex.getMessage());
        body.put("status", HttpStatus.BAD_GATEWAY.value());
        
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(body);
    }
    
    /**
     * Handle AI processing exceptions (503 Service Unavailable).
     */
    @ExceptionHandler(AiProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleAiProcessingException(AiProcessingException ex) {
        logger.error("AI processing error: {}", ex.getMessage(), ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("error", "AI Processing Error");
        body.put("message", ex.getMessage());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(body);
    }
    
    /**
     * Handle all other uncaught exceptions (500 Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred");
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }
}
