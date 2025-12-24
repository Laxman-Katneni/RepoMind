package com.reviewassistant.controller.dto;

public record AuditNotification(
    Long auditId,
    String status,
    String message,
    Integer criticalCount,
    Integer warningCount,
    Integer infoCount,
    Integer totalIssues
) {
    public static AuditNotification success(Long auditId, Integer critical, Integer warning, Integer info) {
        int total = (critical != null ? critical : 0) + 
                   (warning != null ? warning : 0) + 
                   (info != null ? info : 0);
        
        return new AuditNotification(
            auditId,
            "COMPLETED",
            String.format("Audit completed! Found %d issue%s (%d critical, %d warnings)",
                total, total != 1 ? "s" : "", critical, warning),
            critical,
            warning,
            info,
            total
        );
    }

    public static AuditNotification failure(Long auditId, String errorMessage) {
        return new AuditNotification(
            auditId,
            "FAILED",
            "Audit failed: " + errorMessage,
            0,
            0,
            0,
            0
        );
    }
}
