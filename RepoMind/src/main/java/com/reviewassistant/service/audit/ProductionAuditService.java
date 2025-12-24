package com.reviewassistant.service.audit;

import com.reviewassistant.service.dto.AuditResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Production audit service with intelligent multi-tier fallback strategy.
 * 
 * Fallback order:
 * 1. Google Gemini 2.5 Flash (free, primary)
 * 2. OpenAI GPT-4o-mini (fast and reliable fallback)
 */
@Service("productionAuditService")
public class ProductionAuditService implements AuditService {

    private static final Logger logger = LoggerFactory.getLogger(ProductionAuditService.class);

    private final AuditService gemini;
    private final AuditService openAI;

    @Autowired
    public ProductionAuditService(
            @Qualifier("geminiAuditService") AuditService gemini,
            @Qualifier("openAIAuditService") AuditService openAI) {
        this.gemini = gemini;
        this.openAI = openAI;

        logger.info("ProductionAuditService initialized with Gemini → OpenAI fallback strategy");
    }

    @Override
    public String getServiceName() {
        return "Production Multi-Tier (Gemini → OpenAI)";
    }

    @Override
    public AuditResult analyzeCode(String code, String language, String filePath, String ragContext) {
        // Try Gemini first (primary - free)
        try {
            logger.debug("Attempting analysis with: {}", gemini.getServiceName());
            AuditResult result = gemini.analyzeCode(code, language, filePath, ragContext);
            if (result != null) {
                logger.info("✅ Successfully analyzed {} with {}", 
                    filePath, gemini.getServiceName());
                return result;
            }
        } catch (Exception e) {
            logger.warn("⚠️ {} failed for {}: {}",
                gemini.getServiceName(), filePath, e.getMessage());
        }

        // Fallback to OpenAI
        try {
            logger.info("🔄 Falling back to {} for {}", 
                openAI.getServiceName(), filePath);
            AuditResult result = openAI.analyzeCode(code, language, filePath, ragContext);
            if (result != null) {
                logger.info("✅ Successfully analyzed {} with {} (fallback)", 
                    filePath, openAI.getServiceName());
                return result;
            }
        } catch (Exception e) {
            logger.error("❌ {} also failed for {}: {}", 
                openAI.getServiceName(), filePath, e.getMessage());
        }

        // All services failed
        logger.error("❌ CRITICAL: Both Gemini and OpenAI failed for {}", filePath);
        return null;
    }
}
