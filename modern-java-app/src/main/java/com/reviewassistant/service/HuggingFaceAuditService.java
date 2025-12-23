package com.reviewassistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewassistant.service.dto.AuditResult;
import com.reviewassistant.service.dto.HuggingFaceAuditRequest;
import com.reviewassistant.service.dto.HuggingFaceAuditResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class HuggingFaceAuditService {

    private static final Logger logger = LoggerFactory.getLogger(HuggingFaceAuditService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${huggingface.audit.url}")
    private String auditUrl;

    @Value("${huggingface.api.token}")
    private String apiToken;

    public HuggingFaceAuditService(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        
        // Configure RestClient with timeout and connection settings
        this.restClient = restClientBuilder
            .baseUrl("") // Will use full URL in request
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();
        
        logger.info("HuggingFaceAuditService initialized");
    }

    /**
     * Analyzes code using the fine-tuned Hugging Face model via OpenAI-compatible API.
     * Handles the response parsing from llama-cpp-python server.
     * 
     * @param code The source code to analyze
     * @param language The programming language (Java, Python, TypeScript, etc.)
     * @param filePath The file path for context
     * @return AuditResult or null if analysis fails
     */
    @Retryable(
        retryFor = {RestClientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public AuditResult analyzeCode(String code, String language, String filePath) {
        try {
            logger.debug("Analyzing file: {} (language: {})", filePath, language);
            logger.debug("Calling endpoint: {}", auditUrl);
            
            // Create OpenAI-compatible request
            HuggingFaceAuditRequest request = HuggingFaceAuditRequest.forCode(code, language);
            
            // Call Hugging Face API and get raw string response for better error handling
            String rawResponse = restClient.post()
                .uri(auditUrl)
                .header("Authorization", "Bearer " + apiToken)
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(String.class);
            
            if (rawResponse == null || rawResponse.isEmpty()) {
                logger.error("Received null or empty response from Hugging Face for file: {}", filePath);
                return null;
            }
            
            // Log first 500 chars of response for debugging
            logger.debug("Raw response (first 500 chars): {}", 
                rawResponse.length() > 500 ? rawResponse.substring(0, 500) : rawResponse);
            
            // Check if response is HTML (error page)
            if (rawResponse.trim().startsWith("<")) {
                logger.error("Received HTML instead of JSON from endpoint: {}", auditUrl);
                logger.error("HTML response: {}", rawResponse.substring(0, Math.min(1000, rawResponse.length())));
                return null;
            }
            
            // Parse the string as HuggingFaceAuditResponse
            HuggingFaceAuditResponse response;
            try {
                response = objectMapper.readValue(rawResponse, HuggingFaceAuditResponse.class);
            } catch (JsonProcessingException e) {
                logger.error("Failed to parse response as JSON for file {}: {}", filePath, e.getMessage());
                logger.error("Response was: {}", rawResponse);
                return null;
            }
            
            // Extract content from OpenAI response structure
            String content = response.getContent();
            if (content == null || content.isEmpty()) {
                logger.warn("Received empty content from Hugging Face for file: {}", filePath);
                logger.warn("Full response: {}", rawResponse);
                return null;
            }
            
            logger.debug("Extracted content: {}", content);
            
            // Parse the JSON content to AuditResult
            return parseAuditResult(content, filePath);
            
        } catch (RestClientException e) {
            logger.error("API call failed for file {}: {}", filePath, e.getMessage());
            throw e; // Will trigger retry
        } catch (Exception e) {
            logger.error("Unexpected error analyzing file {}: {}", filePath, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parses the stringified JSON into AuditResult.
     * Handles malformed JSON gracefully.
     */
    private AuditResult parseAuditResult(String jsonString, String filePath) {
        try {
            // Parse stringified JSON to AuditResult
            AuditResult result = objectMapper.readValue(jsonString, AuditResult.class);
            
            logger.debug("Successfully parsed audit result for {}: severity={}, category={}",
                filePath, result.severity(), result.category());
            
            return result;
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse inner JSON for file {}: {}", filePath, e.getMessage());
            logger.error("Malformed JSON: {}", jsonString);
            
            // Return null to mark this file as "Analysis Failed"
            return null;
        }
    }

    /**
     * Extracts line number from evidence array if available.
     * Evidence format: ["@L45: var i = 0;", "global state"]
     */
    public Integer extractLineNumber(AuditResult result) {
        if (result == null) {
            return null;
        }
        
        for (String evidence : result.getEvidence()) {
            if (evidence.startsWith("@L")) {
                try {
                    // Extract number from "@L45: ..." format
                    String lineStr = evidence.substring(2, evidence.indexOf(":"));
                    return Integer.parseInt(lineStr);
                } catch (Exception e) {
                    // If parsing fails, continue to next evidence
                    continue;
                }
            }
        }
        
        return null;
    }

    /**
     * Extracts code snippet from evidence array.
     * Returns the first evidence that looks like code.
     */
    public String extractCodeSnippet(AuditResult result) {
        if (result == null || result.getEvidence().isEmpty()) {
            return null;
        }
        
        for (String evidence : result.getEvidence()) {
            // Remove line number prefix if present
            if (evidence.startsWith("@L")) {
                int colonIndex = evidence.indexOf(":");
                if (colonIndex > 0 && colonIndex < evidence.length() - 1) {
                    return evidence.substring(colonIndex + 1).trim();
                }
            } else if (evidence.length() > 10) {
                // Return first substantial evidence
                return evidence;
            }
        }
        
        return null;
    }
}
