package com.reviewassistant.controller.dto;

public record StartAuditResponse(
    Long auditId,
    String message
) {
    public static StartAuditResponse success(Long auditId) {
        return new StartAuditResponse(
            auditId,
            "Audit started successfully. Use the audit ID to poll for status."
        );
    }
}
