package com.reviewassistant.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "code_audits")
public class CodeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "repository_id", nullable = false)
    private Repository repository;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime completedAt;

    @Column
    private Integer totalFilesScanned;

    @Column
    private Integer filesWithIssues;

    @Column
    private Integer criticalCount;

    @Column
    private Integer warningCount;

    @Column
    private Integer infoCount;

    @Column(name = "current_file")
    private String currentFile;

    @Column
    private Integer progressPercentage;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "audit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuditFinding> findings = new ArrayList<>();

    public enum AuditStatus {
        QUEUED,
        IN_PROGRESS,
        COMPLETED,
        PARTIALLY_COMPLETED,
        FAILED
    }

    // Constructors
    public CodeAudit() {}

    public CodeAudit(Repository repository) {
        this.repository = repository;
        this.status = AuditStatus.QUEUED;
        this.totalFilesScanned = 0;
        this.filesWithIssues = 0;
        this.criticalCount = 0;
        this.warningCount = 0;
        this.infoCount = 0;
        this.progressPercentage = 0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public AuditStatus getStatus() {
        return status;
    }

    public void setStatus(AuditStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getTotalFilesScanned() {
        return totalFilesScanned;
    }

    public void setTotalFilesScanned(Integer totalFilesScanned) {
        this.totalFilesScanned = totalFilesScanned;
    }

    public Integer getFilesWithIssues() {
        return filesWithIssues;
    }

    public void setFilesWithIssues(Integer filesWithIssues) {
        this.filesWithIssues = filesWithIssues;
    }

    public Integer getCriticalCount() {
        return criticalCount;
    }

    public void setCriticalCount(Integer criticalCount) {
        this.criticalCount = criticalCount;
    }

    public Integer getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(Integer warningCount) {
        this.warningCount = warningCount;
    }

    public Integer getInfoCount() {
        return infoCount;
    }

    public void setInfoCount(Integer infoCount) {
        this.infoCount = infoCount;
    }

    public String getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(String currentFile) {
        this.currentFile = currentFile;
    }

    public Integer getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Integer progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<AuditFinding> getFindings() {
        return findings;
    }

    public void setFindings(List<AuditFinding> findings) {
        this.findings = findings;
    }

    // Helper methods
    public void incrementCounts(String severity) {
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            this.criticalCount++;
        } else if ("WARNING".equalsIgnoreCase(severity)) {
            this.warningCount++;
        } else if ("INFO".equalsIgnoreCase(severity)) {
            this.infoCount++;
        }
    }

    public void updateProgress(int current, int total, String currentFile) {
        this.progressPercentage = (int) ((current / (double) total) * 100);
        this.currentFile = currentFile;
    }

    public void markCompleted() {
        this.status = AuditStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.progressPercentage = 100;
        this.currentFile = null;
    }

    public void markFailed(String errorMessage) {
        this.status = AuditStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }
}
