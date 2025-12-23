package com.reviewassistant.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * OpenAI-compatible response format from llama-cpp-python server.
 */
public record HuggingFaceAuditResponse(
    String id,
    String object,
    Long created,
    String model,
    List<Choice> choices,
    Usage usage
) {
    public record Choice(
        Integer index,
        Message message,
        @JsonProperty("finish_reason") String finishReason
    ) {}
    
    public record Message(
        String role,
        String content
    ) {}
    
    public record Usage(
        @JsonProperty("prompt_tokens") Integer promptTokens,
        @JsonProperty("completion_tokens") Integer completionTokens,
        @JsonProperty("total_tokens") Integer totalTokens
    ) {}
    
    /**
     * Extract the assistant's message content (which contains our JSON).
     */
    public String getContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        Choice firstChoice = choices.get(0);
        if (firstChoice == null || firstChoice.message() == null) {
            return null;
        }
        return firstChoice.message().content();
    }
}
