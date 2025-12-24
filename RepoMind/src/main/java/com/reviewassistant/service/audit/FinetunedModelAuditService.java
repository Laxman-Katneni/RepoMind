package com.reviewassistant.service.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewassistant.service.HuggingFaceLoadBalancer;
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

/**
 * Audit service using custom finetuned Qwen model.
 * Primary service for production with specialized code review capabilities.
 */
@Service("finetunedModelAuditService")
public class FinetunedModelAuditService implements AuditService {

    private static final Logger logger = LoggerFactory.getLogger(FinetunedModelAuditService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final HuggingFaceLoadBalancer loadBalancer;

    @Value("${finetuned.model.token}")
    private String apiToken;

    public FinetunedModelAuditService(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            HuggingFaceLoadBalancer loadBalancer) {
        this.objectMapper = objectMapper;
        this.loadBalancer = loadBalancer;

        // Configure RestClient with timeout and connection settings
        this.restClient = restClientBuilder
            .baseUrl("") // Will use full URL from load balancer
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .build();

        logger.info("FinetunedModelAuditService initialized with {} endpoints",
            loadBalancer.getEndpointCount());
    }

    @Override
    public String getServiceName() {
        return "Finetuned Model (Qwen)";
    }

    @Override
    @Retryable(
        retryFor = RestClientException.class,
        maxAttempts = 2,
        backoff = @Backoff(delay = 1000)
    )
    public AuditResult analyzeCode(String code, String language, String filePath, String ragContext) {
        try {
            logger.debug("Analyzing file: {} (language: {}, with context: {})",
                filePath, language, ragContext != null);

            // Get endpoint from load balancer
            String endpoint = loadBalancer.getNextEndpoint();
            logger.debug("Using endpoint: {}", endpoint);

            // Create OpenAI-compatible request with optional RAG context
            HuggingFaceAuditRequest request = ragContext != null
                ? HuggingFaceAuditRequest.forCodeWithContext(code, language, filePath, ragContext)
                : HuggingFaceAuditRequest.forCode(code, language);

            // Call OpenAI-compatible endpoint
            HuggingFaceAuditResponse response = restClient.post()
                .uri(endpoint)
                .header("Authorization", "Bearer " + apiToken)
                .header("Content-Type", "application/json")
                .body(request)
                .retrieve()
                .body(HuggingFaceAuditResponse.class);

            if (response == null) {
                logger.error("Received null response from finetuned model for file: {}", filePath);
                return null;
            }

            // Extract content from choices[0].message.content
            String content = response.getContent();
            if (content == null || content.isEmpty()) {
                logger.warn("Received empty content from finetuned model for file: {}", filePath);
                return null;
            }

            logger.debug("Extracted content from finetuned model response: {}",
                content.length() > 200 ? content.substring(0, 200) + "..." : content);

            // Double-deserialize: parse the content string as JSON to get AuditResult
            return parseAuditResult(content, filePath);

        } catch (RestClientException e) {
            logger.error("Finetuned model API call failed for file {}: {}", filePath, e.getMessage());
            throw e; // Will trigger retry or fallback
        } catch (Exception e) {
            logger.error("Unexpected error analyzing file {} with finetuned model: {}", 
                filePath, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parses the inner JSON content to extract AuditResult.
     */
    private AuditResult parseAuditResult(String content, String filePath) {
        try {
            // Parse the JSON string to AuditResult
            AuditResult result = objectMapper.readValue(content, AuditResult.class);
            logger.debug("Successfully parsed audit result for {}: severity={}, category={}",
                filePath, result.severity(), result.category());
            return result;

        } catch (JsonProcessingException e) {
            logger.error("Failed to parse inner JSON for file {}: {}", filePath, e.getMessage());
            logger.error("Malformed JSON: {}", content.substring(0, Math.min(200, content.length())));
            return null;
        }
    }
}
