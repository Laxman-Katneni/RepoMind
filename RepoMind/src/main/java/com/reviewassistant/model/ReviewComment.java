package com.reviewassistant.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing an AI-generated review comment.
 * Mapped from Python ReviewComment model in pr/models.py.
 */
@Entity
@Table(name = "review_comments")
public class ReviewComment implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String filePath;
    
    /**
     * Line number where the comment applies
     */
    @Column(nullable = false)
    private Integer lineNumber;
    
    /**
     * Severity level: "info" | "warning" | "critical"
     */
    @Column(nullable = false)
    private String severity;
    
    /**
     * Category: "architecture" | "security" | "bug-risk" | "performance" | "readability" | etc.
     */
    @Column(nullable = false)
    private String category;
    
    /**
     * The actual comment text
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;
    
    /**
     * Why this matters
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String rationale;
    
    /**
     * Optional suggestion for improvement
     */
    @Column(columnDefinition = "TEXT")
    private String suggestion;
    
    /**
     * Extra metadata stored as JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String extra;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_run_id", nullable = false)
    private ReviewRun reviewRun;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
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

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ReviewRun getReviewRun() {
        return reviewRun;
    }

    public void setReviewRun(ReviewRun reviewRun) {
        this.reviewRun = reviewRun;
    }
}
