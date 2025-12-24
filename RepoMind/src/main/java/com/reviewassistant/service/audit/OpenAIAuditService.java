package com.reviewassistant.service.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewassistant.service.dto.AuditResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Audit service using OpenAI GPT-4o-mini.
 * Fallback 1 in production multi-tier strategy.
 */
@Service("openAIAuditService")
public class OpenAIAuditService implements AuditService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIAuditService.class);

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    private static final String AUDIT_PROMPT_TEMPLATE = """
        Analyze this {language} code for quality, security, and architectural issues.
        {ragContext}
        
        File: {filePath}
        
        Code:
        ```{language}
        {code}
        ```
        
        Return ONLY valid JSON with this exact structure (no markdown, no extra text):
        {{
          "severity": "CRITICAL|WARNING|INFO",
          "category": "SECURITY|PERFORMANCE|CODE_QUALITY|ARCHITECTURE|BEST_PRACTICES",
          "language": "{language}",
          "title": "Brief title",
          "message": "Detailed description of issues found",
          "suggestion": "Concrete steps to fix the issues",
          "extra": {{}}
        }}
        """;

    @Autowired
    public OpenAIAuditService(ChatModel chatModel, ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        logger.info("OpenAIAuditService initialized");
    }

    @Override
    public String getServiceName() {
        return "OpenAI GPT-4o-mini";
    }

    @Override
    public AuditResult analyzeCode(String code, String language, String filePath, String ragContext) {
        try {
            logger.debug("Analyzing file: {} with OpenAI (language: {}, with context: {})",
                filePath, language, ragContext != null);

            // Build RAG context section
            String contextSection = ragContext != null && !ragContext.isEmpty()
                ? "Consider this related code from the repository:\n" + ragContext
                : "";

            // Create prompt with template
            PromptTemplate promptTemplate = new PromptTemplate(AUDIT_PROMPT_TEMPLATE);
            Prompt prompt = promptTemplate.create(Map.of(
                "language", language,
                "code", code,
                "filePath", filePath,
                "ragContext", contextSection
            ));

            // Call OpenAI
            String response = chatModel.call(prompt).getResult().getOutput().getText();

            if (response == null || response.isEmpty()) {
                logger.warn("Received empty response from OpenAI for file: {}", filePath);
                return null;
            }

            logger.debug("Received response from OpenAI: {}",
                response.length() > 200 ? response.substring(0, 200) + "..." : response);

            // Clean response (remove markdown if present)
            String cleanedResponse = response.trim();
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            }
            if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            cleanedResponse = cleanedResponse.trim();

            // Parse JSON to AuditResult
            AuditResult result = parseAuditResult(cleanedResponse, filePath);
            logger.info("🎯 analyzeCode returning for {}: {}", filePath, result != null ? "SUCCESS" : "NULL");
            return result;

        } catch (Exception e) {
            logger.error("OpenAI API call failed for file {}: {}", filePath, e.getMessage());
            throw new RuntimeException("OpenAI audit failed", e);
        }
    }

    private AuditResult parseAuditResult(String content, String filePath) {
        try {
            logger.debug("Parsing JSON for {}: {}", filePath, content);
            
            // Parse as JsonNode first to handle missing fields gracefully
            JsonNode jsonNode = objectMapper.readTree(content);
            
            // Extract required fields
            String severity = jsonNode.get("severity").asText();
            String category = jsonNode.get("category").asText();
            String language = jsonNode.get("language").asText();
            String title = jsonNode.get("title").asText();
            String message = jsonNode.get("message").asText();
            String suggestion = jsonNode.get("suggestion").asText();
            
            // Handle optional extra field - default to empty map if missing
            Map<String, Object> extra = new HashMap<>();
            if (jsonNode.has("extra") && !jsonNode.get("extra").isNull()) {
                extra = objectMapper.convertValue(
                    jsonNode.get("extra"), 
                    new TypeReference<Map<String, Object>>() {}
                );
            }
            
            AuditResult result = new AuditResult(severity, category, language, title, message, suggestion, extra);
            logger.info("✅ Successfully parsed OpenAI result for {}: {}/{}", 
                filePath, severity, category);
            return result;

        } catch (Exception e) {
            logger.error("❌ Failed to parse OpenAI JSON for file {}: {}", filePath, e.getMessage(), e);
            logger.error("Full JSON content: {}", content);
            return null;
        }
    }
}
