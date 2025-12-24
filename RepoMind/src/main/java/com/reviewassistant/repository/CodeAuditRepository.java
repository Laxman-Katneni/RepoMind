package com.reviewassistant.repository;

import com.reviewassistant.model.CodeAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeAuditRepository extends JpaRepository<CodeAudit, Long> {
    
    List<CodeAudit> findByRepositoryIdOrderByStartedAtDesc(Long repositoryId);
    
    List<CodeAudit> findByRepositoryIdAndStatusOrderByStartedAtDesc(
        Long repositoryId, 
        CodeAudit.AuditStatus status
    );
    
    Optional<CodeAudit> findTopByRepositoryIdOrderByStartedAtDesc(Long repositoryId);
}
