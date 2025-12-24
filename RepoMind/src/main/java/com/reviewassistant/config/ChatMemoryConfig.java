package com.reviewassistant.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AI Chat Memory.
 * Enables conversation history across multiple chat messages.
 */
@Configuration
public class ChatMemoryConfig {
    
    /**
     * Create in-memory chat memory bean.
     * Stores conversation history in memory for fast access.
     * 
     * @return ChatMemory instance
     */
    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }
    
    /**
     * Create MessageChatMemoryAdvisor bean.
     * Integrates chat memory with Spring AI ChatClient.
     * 
     * @param chatMemory The chat memory to use
     * @return MessageChatMemoryAdvisor instance
     */
    @Bean
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return new MessageChatMemoryAdvisor(chatMemory);
    }
}
