package com.reviewassistant.repository;

import com.reviewassistant.model.AuditFinding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditFindingRepository extends JpaRepository<AuditFinding, Long> {
    
    Page<AuditFinding> findByAuditId(Long auditId, Pageable pageable);
    
    Page<AuditFinding> findByAuditIdAndSeverity(Long auditId, String severity, Pageable pageable);
    
    Page<AuditFinding> findByAuditIdAndCategory(Long auditId, String category, Pageable pageable);
    
    @Query("SELECT f FROM AuditFinding f WHERE f.audit.id = :auditId " +
           "AND (:severity IS NULL OR f.severity = :severity) " +
           "AND (:category IS NULL OR f.category = :category)")
    Page<AuditFinding> findByFilters(
        @Param("auditId") Long auditId,
        @Param("severity") String severity,
        @Param("category") String category,
        Pageable pageable
    );
    
    long countByAuditId(Long auditId);
    
    long countByAuditIdAndSeverity(Long auditId, String severity);
}
