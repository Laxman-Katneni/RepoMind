package com.reviewassistant.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * OpenAI-compatible request format for Hugging Face llama-cpp-python server.
 * Note: Model field is omitted as the endpoint doesn't require/accept it.
 */
public record HuggingFaceAuditRequest(
    List<Message> messages,
    Double temperature,
    @JsonProperty("max_tokens") Integer maxTokens
) {
    public record Message(String role, String content) {}

    public static HuggingFaceAuditRequest forCode(String code, String language) {
        String systemMessage = String.format(
            "Analyze this %s code for architectural quality. Return strict JSON.",
            language
        );
        
        List<Message> messages = List.of(
            new Message("system", systemMessage),
            new Message("user", code)
        );
        
        return new HuggingFaceAuditRequest(messages, 0.2, 2000);
    }
}
