package com.reviewassistant.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity representing code changes within a Pull Request.
 * Mapped from Python DiffChunk model in pr/models.py.
 */
@Entity
@Table(name = "diff_chunks")
public class DiffChunk implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String filePath;
    
    /**
     * File status: "added" | "modified" | "removed" | "renamed"
     */
    @Column(nullable = false)
    private String status;
    
    /**
     * Hunk header, e.g., "@@ -10,5 +12,8 @@"
     */
    @Column(nullable = false)
    private String hunkHeader;
    
    /**
     * Starting line in new file
     */
    @Column(nullable = false)
    private Integer newStart;
    
    /**
     * End line in new file (approximate)
     */
    @Column(nullable = false)
    private Integer newEnd;
    
    /**
     * The diff hunk text
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String patchText;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_request_id", nullable = false)
    private PullRequest pullRequest;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHunkHeader() {
        return hunkHeader;
    }

    public void setHunkHeader(String hunkHeader) {
        this.hunkHeader = hunkHeader;
    }

    public Integer getNewStart() {
        return newStart;
    }

    public void setNewStart(Integer newStart) {
        this.newStart = newStart;
    }

    public Integer getNewEnd() {
        return newEnd;
    }

    public void setNewEnd(Integer newEnd) {
        this.newEnd = newEnd;
    }

    public String getPatchText() {
        return patchText;
    }

    public void setPatchText(String patchText) {
        this.patchText = patchText;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public PullRequest getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }
}
