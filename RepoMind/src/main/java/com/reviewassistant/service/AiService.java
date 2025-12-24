package com.reviewassistant.service;

import com.reviewassistant.model.AiReviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for AI-powered code review analysis.
 * Uses Spring AI ChatClient to generate structured reviews with a senior engineer persona.
 */
@Service
public class AiService {
    
    private static final Logger logger = LoggerFactory.getLogger(AiService.class);
    
    private final ChatClient chatClient;
    
    public AiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    /**
     * Get structured AI code review for a given diff.
     * Returns structured JSON with issues broken down by file, severity, etc.
     * 
     * @param diff Git diff string to analyze
     * @return Structured review response with list of issues
     */
    public AiReviewResponse getReview(String diff) {
        logger.info("Requesting AI review for diff ({} characters)", diff != null ? diff.length() : 0);
        
        try {
            // 1. Define output converter for structured JSON
            var converter = new BeanOutputConverter<>(AiReviewResponse.class);
            String format = converter.getFormat();
            
            // 2. System prompt with JSON schema enforcement
            String systemPrompt = "You are a Senior Software Engineer expert in clean code, security, and architecture. " +
                    "Review the following git diff and identify bugs, security vulnerabilities, and architectural issues. " +
                    "For each issue, specify the file path, line number, severity (critical/warning/info), " +
                    "category (Security/Performance/Code Quality/Architecture), description, and suggestion. " +
                    "Strictly follow this JSON format: " + format;
            
            // 3. Call AI and get structured response
            logger.debug("Sending diff to AI with structured output format");
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(diff)
                    .call()
                    .content();
            
            logger.debug("Raw AI response: {}", response);
            
            // 4. Convert to structured object
            AiReviewResponse aiResponse = converter.convert(response);
            
            int issueCount = aiResponse != null && aiResponse.issues() != null ? aiResponse.issues().size() : 0;
            logger.info("AI review completed successfully with {} issues", issueCount);
            
            return aiResponse;
            
        } catch (Exception e) {
            logger.error("Failed to get AI review: {}", e.getMessage(), e);
            // Return empty response instead of null
            return new AiReviewResponse(List.of());
        }
    }
}
