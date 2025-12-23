package com.reviewassistant.service.dto;

import java.util.List;

/**
 * OpenAI-compatible request format for Hugging Face llama-cpp-python server.
 */
public record HuggingFaceAuditRequest(
    List<Message> messages,
    Double temperature,
    Integer maxTokens
) {
    public record Message(String role, String content) {}

    public static HuggingFaceAuditRequest forCode(String code, String language) {
        String systemMessage = String.format(
            "Analyze this %s code for architectural quality and security issues. Return strict JSON with fields: severity, category, language, message, suggestion, and evidence.",
            language
        );
        
        List<Message> messages = List.of(
            new Message("system", systemMessage),
            new Message("user", code)
        );
        
        return new HuggingFaceAuditRequest(messages, 0.2, 2000);
    }
}
