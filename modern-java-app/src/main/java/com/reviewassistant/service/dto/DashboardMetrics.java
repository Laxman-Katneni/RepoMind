package com.reviewassistant.service.dto;

import java.util.Map;

/**
 * DTO for dashboard metrics and statistics.
 */
public class DashboardMetrics {
    
    private Long totalReviews;
    private Integer criticalIssuesCount;
    private Double averageReviewTime;
    private Map<String, Integer> reviewsPerDay;
    
    public DashboardMetrics() {
    }
    
    public DashboardMetrics(Long totalReviews, Integer criticalIssuesCount, 
                           Double averageReviewTime, Map<String, Integer> reviewsPerDay) {
        this.totalReviews = totalReviews;
        this.criticalIssuesCount = criticalIssuesCount;
        this.averageReviewTime = averageReviewTime;
        this.reviewsPerDay = reviewsPerDay;
    }
    
    public Long getTotalReviews() {
        return totalReviews;
    }
    
    public void setTotalReviews(Long totalReviews) {
        this.totalReviews = totalReviews;
    }
    
    public Integer getCriticalIssuesCount() {
        return criticalIssuesCount;
    }
    
    public void setCriticalIssuesCount(Integer criticalIssuesCount) {
        this.criticalIssuesCount = criticalIssuesCount;
    }
    
    public Double getAverageReviewTime() {
        return averageReviewTime;
    }
    
    public void setAverageReviewTime(Double averageReviewTime) {
        this.averageReviewTime = averageReviewTime;
    }
    
    public Map<String, Integer> getReviewsPerDay() {
        return reviewsPerDay;
    }
    
    public void setReviewsPerDay(Map<String, Integer> reviewsPerDay) {
        this.reviewsPerDay = reviewsPerDay;
    }
}
