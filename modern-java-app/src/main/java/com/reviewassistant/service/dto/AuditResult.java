package com.reviewassistant.service.dto;

import java.util.Map;

public record AuditResult(
    String severity,
    String category,
    String language,
    String title,
    String message,
    String suggestion,
    Map<String, Object> extra
) {
    // Helper methods to extract common metadata
    public String getConfidence() {
        if (extra != null && extra.containsKey("confidence")) {
            return extra.get("confidence").toString();
        }
        return "unknown";
    }
    
    public String getCwe() {
        if (extra != null && extra.containsKey("cwe")) {
            return extra.get("cwe").toString();
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    public java.util.List<String> getEvidence() {
        if (extra != null && extra.containsKey("evidence")) {
            Object evidence = extra.get("evidence");
            if (evidence instanceof java.util.List) {
                return (java.util.List<String>) evidence;
            }
        }
        return java.util.Collections.emptyList();
    }
}
