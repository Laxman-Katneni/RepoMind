package com.reviewassistant.config;

import com.reviewassistant.service.audit.AuditService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for selecting the active audit service based on application.properties
 */
@Configuration
public class AuditServiceConfig {

    @Value("${audit.service.mode:production}")
    private String auditServiceMode;

    @Bean(name = "activeAuditService")
    public AuditService activeAuditService(
            @Qualifier("productionAuditService") AuditService productionService,
            @Qualifier("finetunedModelAuditService") AuditService finetunedService,
            @Qualifier("geminiAuditService") AuditService geminiService,
            @Qualifier("openAIAuditService") AuditService openAIService) {

        return switch (auditServiceMode.toLowerCase()) {
            case "finetuned" -> finetunedService;
            case "gemini" -> geminiService;
            case "openai" -> openAIService;
            default -> productionService; // Default to production (Gemini → OpenAI fallback)
        };
    }
}
