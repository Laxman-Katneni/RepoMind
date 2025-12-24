package com.reviewassistant.exception;

/**
 * Exception thrown when GitHub API rate limit is exceeded.
 * This is a specific case of GithubException for 403/429 status codes.
 * Triggers retry logic with backoff.
 */
public class RateLimitException extends GithubException {
    
    private final long retryAfterSeconds;
    
    public RateLimitException(String message) {
        super(message, 429);
        this.retryAfterSeconds = 60; // Default 1 minute
    }
    
    public RateLimitException(String message, long retryAfterSeconds) {
        super(message, 429);
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public RateLimitException(String message, int statusCode) {
        super(message, statusCode);
        this.retryAfterSeconds = 60;
    }
    
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
