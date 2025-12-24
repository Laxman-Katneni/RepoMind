package com.reviewassistant.model;

import java.util.List;

/**
 * Structured response from AI code review.
 * Used with BeanOutputConverter to enforce JSON schema.
 */
public record AiReviewResponse(List<Issue> issues) {
    
    /**
     * Individual code review issue identified by AI.
     */
    public record Issue(
            String filePath,
            int lineNumber,
            String severity,      // "critical", "warning", "info"
            String category,      // e.g., "Security", "Performance", "Code Quality"
            String description,   // What the issue is
            String suggestion     // How to fix it
    ) {}
}
