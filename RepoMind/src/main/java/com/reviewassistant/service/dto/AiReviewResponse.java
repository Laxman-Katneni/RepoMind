package com.reviewassistant.service.dto;

import java.util.List;

/**
 * DTO for capturing LLM output from code review analysis.
 * Used with BeanOutputParser to ensure structured JSON response.
 */
public class AiReviewResponse {
    
    private String summary;
    private List<CommentDto> comments;
    
    public AiReviewResponse() {
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public List<CommentDto> getComments() {
        return comments;
    }
    
    public void setComments(List<CommentDto> comments) {
        this.comments = comments;
    }
    
    /**
     * Inner DTO for individual review comments.
     */
    public static class CommentDto {
        
        private String filePath;
        private Integer lineNumber;
        private String severity;
        private String category;
        private String rationale;
        private String suggestion;
        
        public CommentDto() {
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
        
        public Integer getLineNumber() {
            return lineNumber;
        }
        
        public void setLineNumber(Integer lineNumber) {
            this.lineNumber = lineNumber;
        }
        
        public String getSeverity() {
            return severity;
        }
        
        public void setSeverity(String severity) {
            this.severity = severity;
        }
        
        public String getCategory() {
            return category;
        }
        
        public void setCategory(String category) {
            this.category = category;
        }
        
        public String getRationale() {
            return rationale;
        }
        
        public void setRationale(String rationale) {
            this.rationale = rationale;
        }
        
        public String getSuggestion() {
            return suggestion;
        }
        
        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }
    }
}
