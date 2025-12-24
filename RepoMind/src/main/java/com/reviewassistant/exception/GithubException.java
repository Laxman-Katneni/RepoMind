package com.reviewassistant.exception;

/**
 * Exception thrown when GitHub API calls fail.
 * This includes network errors, invalid responses, and other API failures.
 */
public class GithubException extends RuntimeException {
    
    private final int statusCode;
    
    public GithubException(String message) {
        super(message);
        this.statusCode = 500;
    }
    
    public GithubException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public GithubException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
    }
    
    public GithubException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}
