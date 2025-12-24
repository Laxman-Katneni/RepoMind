package com.reviewassistant.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing a code chunk for RAG/embedding.
 * Mapped from Python CodeChunk model in ingestion/models.py.
 */
@Entity
@Table(name = "code_chunks")
public class CodeChunk implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String filePath;
    
    @Column(nullable = false)
    private String language;
    
    @Column(nullable = false)
    private Integer startLine;
    
    @Column(nullable = false)
    private Integer endLine;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    /**
     * Additional metadata stored as JSON
     * Made non-insertable/updatable to avoid type casting issues with JSONB
     */
    @Column(columnDefinition = "jsonb", insertable = false, updatable = false)
    private String metadata;
    
    /**
     * Vector embedding for semantic search (AI-powered code retrieval)
     * Using native float[] for PostgreSQL vector compatibility
     */
    @Column(columnDefinition = "vector")
    private float[] embedding;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
}
