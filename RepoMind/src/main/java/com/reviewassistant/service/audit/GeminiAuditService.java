package com.reviewassistant.service.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewassistant.service.dto.AuditResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Audit service using Google Gemini API.
 * Fallback 2 in production multi-tier strategy (free tier).
 */
@Service("geminiAuditService")
public class GeminiAuditService implements AuditService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiAuditService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.model:gemini-1.5-flash}")
    private String model;

    public GeminiAuditService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
            .baseUrl("https://generativelanguage.googleapis.com/v1")
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();

        logger.info("GeminiAuditService initialized");
    }

    @Override
    public String getServiceName() {
        return "Google Gemini";
    }

    @Override
    public AuditResult analyzeCode(String code, String language, String filePath, String ragContext) {
        try {
            logger.debug("Analyzing file: {} with Gemini (language: {}, with context: {})",
                filePath, language, ragContext != null);

            // Build prompt
            String prompt = buildPrompt(code, language, filePath, ragContext);

            // Create Gemini request
            Map<String, Object> request = Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(Map.of("text", prompt))
                ))
            );

            // Call Gemini API
            String endpoint = String.format("/models/%s:generateContent?key=%s", model, apiKey);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                .uri(endpoint)
                .body(request)
                .retrieve()
                .body(Map.class);

            if (response == null) {
                logger.warn("Received null response from Gemini for file: {}", filePath);
                return null;
            }

            // Extract text from nested structure
            String responseText = extractResponseText(response);
            if (responseText == null || responseText.isEmpty()) {
                logger.warn("Failed to extract text from Gemini response for file: {}", filePath);
                return null;
            }

            logger.debug("Received response from Gemini: {}",
                responseText.length() > 200 ? responseText.substring(0, 200) + "..." : responseText);

            // Clean and parse response
            String cleanedResponse = cleanResponse(responseText);
            return parseAuditResult(cleanedResponse, filePath);

        } catch (Exception e) {
            logger.error("Gemini API call failed for file {}: {}", filePath, e.getMessage());
            throw new RuntimeException("Gemini audit failed", e);
        }
    }

    private String buildPrompt(String code, String language, String filePath, String ragContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this ").append(language).append(" code for quality, security, and architectural issues.\n\n");

        if (ragContext != null && !ragContext.isEmpty()) {
            prompt.append("Consider this related code from the repository:\n");
            prompt.append(ragContext).append("\n\n");
        }

        prompt.append("File: ").append(filePath).append("\n\n");
        prompt.append("Code:\n```").append(language.toLowerCase()).append("\n");
        prompt.append(code).append("\n```\n\n");
        prompt.append("Return ONLY valid JSON with this exact structure (no markdown, no extra text):\n");
        prompt.append("{\n");
        prompt.append("  \"severity\": \"CRITICAL|WARNING|INFO\",\n");
        prompt.append("  \"category\": \"SECURITY|PERFORMANCE|CODE_QUALITY|ARCHITECTURE|BEST_PRACTICES\",\n");
        prompt.append("  \"language\": \"").append(language).append("\",\n");
        prompt.append("  \"title\": \"Brief title\",\n");
        prompt.append("  \"message\": \"Detailed description of issues found\",\n");
        prompt.append("  \"suggestion\": \"Concrete steps to fix the issues\",\n");
        prompt.append("  \"extra\": {}\n");
        prompt.append("}");

        return prompt.toString();
    }

    @SuppressWarnings("unchecked")
    private String extractResponseText(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

            if (parts == null || parts.isEmpty()) {
                return null;
            }

            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            logger.error("Error extracting text from Gemini response", e);
            return null;
        }
    }

    private String cleanResponse(String response) {
        String cleaned = response.trim();
        // Remove markdown code blocks
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }

    private AuditResult parseAuditResult(String content, String filePath) {
        try {
            // Manual parsing to handle missing extra field gracefully
            com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(content);
            
            String severity = jsonNode.get("severity").asText();
            String category = jsonNode.get("category").asText();
            String language = jsonNode.get("language").asText();
            String title = jsonNode.get("title").asText();
            String message = jsonNode.get("message").asText();
            String suggestion = jsonNode.get("suggestion").asText();
            
            // Handle extra field - default to empty map if missing
            Map<String, Object> extra = new HashMap<>();
            if (jsonNode.has("extra") && !jsonNode.get("extra").isNull()) {
                extra = objectMapper.convertValue(
                    jsonNode.get("extra"),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}
                );
            }
            
            AuditResult result = new AuditResult(severity, category, language, title, message, suggestion, extra);
            logger.info("✅ Successfully parsed Gemini result for {}: {}/{}", 
                filePath, severity, category);
            return result;

        } catch (Exception e) {
            logger.error("Failed to parse Gemini JSON for file {}: {}", filePath, e.getMessage(), e);
            logger.error("Malformed JSON: {}", content);
            return null;
        }
    }
}
