package com.reviewassistant.service.audit;

import com.reviewassistant.service.dto.AuditResult;

/**
 * Core interface for code audit services.
 * Supports multiple implementations (finetuned model, OpenAI, Gemini, etc.)
 * with intelligent fallback strategies.
 */
public interface AuditService {
    
    /**
     * Analyzes code with optional RAG context.
     * 
     * @param code The code content to analyze
     * @param language Programming language (e.g., "JavaScript", "Python")
     * @param filePath File path for context
     * @param ragContext Optional RAG-retrieved context (can be null)
     * @return AuditResult containing findings and suggestions
     */
    AuditResult analyzeCode(String code, String language, String filePath, String ragContext);
    
    /**
     * Returns the name of this audit service implementation.
     * Used for logging and metrics.
     */
    String getServiceName();
    
    /**
     * Checks if this service is currently available/healthy.
     * Used for fallback decision making.
     */
    default boolean isAvailable() {
        return true;
    }
}
