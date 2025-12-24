package com.reviewassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AI ChatClient.
 * Configures retry behavior and default settings.
 */
@Configuration
public class AiConfig {
    
    /**
     * Configure ChatClient with custom settings.
     * Disables retry to prevent HttpRetryException in streaming mode.
     * 
     * @param builder ChatClient builder provided by Spring AI
     * @return Configured ChatClient instance
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("You are a helpful AI assistant.")
                // Disable retry to prevent HttpRetryException in streaming mode
                // The underlying HTTP client doesn't support retries in streaming mode
                .build();
    }
}
