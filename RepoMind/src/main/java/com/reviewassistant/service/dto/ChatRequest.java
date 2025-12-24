package com.reviewassistant.service.dto;

/**
 * Request DTO for chat messages.
 */
public class ChatRequest {
    
    private String message;
    private Long repoId;
    private String conversationId;
    
    public ChatRequest() {
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Long getRepoId() {
        return repoId;
    }
    
    public void setRepoId(Long repoId) {
        this.repoId = repoId;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
