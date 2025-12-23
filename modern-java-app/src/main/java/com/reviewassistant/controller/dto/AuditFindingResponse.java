package com.reviewassistant.controller.dto;

import java.time.LocalDateTime;

public record AuditFindingResponse(
    Long id,
    String filePath,
    Integer lineNumber,
    String severity,
    String category,
    String language,
    String title,
    String message,
    String suggestion,
    String codeSnippet,
    LocalDateTime createdAt
) {
    public static AuditFindingResponse fromEntity(com.reviewassistant.model.AuditFinding finding) {
        return new AuditFindingResponse(
            finding.getId(),
            finding.getFilePath(),
            finding.getLineNumber(),
            finding.getSeverity(),
            finding.getCategory(),
            finding.getLanguage(),
            finding.getTitle(),
            finding.getMessage(),
            finding.getSuggestion(),
            finding.getCodeSnippet(),
            finding.getCreatedAt()
        );
    }
}
