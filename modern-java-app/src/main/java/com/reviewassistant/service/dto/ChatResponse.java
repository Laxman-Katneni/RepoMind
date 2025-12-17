package com.reviewassistant.service.dto;

/**
 * Response DTO for chat messages.
 */
public class ChatResponse {
    
    private String answer;
    
    public ChatResponse() {
    }
    
    public ChatResponse(String answer) {
        this.answer = answer;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
