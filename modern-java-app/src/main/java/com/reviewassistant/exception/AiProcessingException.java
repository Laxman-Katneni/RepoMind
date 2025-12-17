package com.reviewassistant.exception;

/**
 * Exception thrown when AI processing (OpenAI/Spring AI) fails.
 * This includes embedding generation failures, model errors, etc.
 */
public class AiProcessingException extends RuntimeException {
    
    public AiProcessingException(String message) {
        super(message);
    }
    
    public AiProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
