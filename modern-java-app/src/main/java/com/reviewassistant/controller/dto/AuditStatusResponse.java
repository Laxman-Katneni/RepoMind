package com.reviewassistant.controller.dto;

public record AuditStatusResponse(
    Long auditId,
    String status,
    Integer progressPercentage,
    String currentFile,
    Integer totalFilesScanned,
    Integer filesWithIssues,
    Integer criticalCount,
    Integer warningCount,
    Integer infoCount,
    String errorMessage
) {
    public static AuditStatusResponse fromEntity(com.reviewassistant.model.CodeAudit audit) {
        return new AuditStatusResponse(
            audit.getId(),
            audit.getStatus().name(),
            audit.getProgressPercentage(),
            audit.getCurrentFile(),
            audit.getTotalFilesScanned(),
            audit.getFilesWithIssues(),
            audit.getCriticalCount(),
            audit.getWarningCount(),
            audit.getInfoCount(),
            audit.getErrorMessage()
        );
    }
}
