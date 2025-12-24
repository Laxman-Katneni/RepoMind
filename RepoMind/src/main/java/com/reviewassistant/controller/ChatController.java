package com.reviewassistant.controller;

import com.reviewassistant.model.CodeChunk;
import com.reviewassistant.service.RagService;
import com.reviewassistant.service.dto.ChatRequest;
import com.reviewassistant.service.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * REST controller for AI chat functionality with conversation memory.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private static final int MAX_CONTEXT_CHUNKS = 5;
    
    private final RagService ragService;
    private final MessageChatMemoryAdvisor chatMemoryAdvisor;
    private final ChatClient.Builder chatClientBuilder;
    
    public ChatController(RagService ragService, MessageChatMemoryAdvisor chatMemoryAdvisor, ChatClient.Builder chatClientBuilder) {
        this.ragService = ragService;
        this.chatMemoryAdvisor = chatMemoryAdvisor;
        this.chatClientBuilder = chatClientBuilder;
    }
    
    /**
     * Chat with the codebase using RAG-powered AI with conversation memory.
     * 
     * @param request Chat request with message, repository ID, and conversation ID
     * @return AI-generated response
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        
        // Generate conversation ID if not provided
        final String conversationId;
        if (request.getConversationId() == null || request.getConversationId().isEmpty()) {
            conversationId = UUID.randomUUID().toString();
            logger.info("Generated new conversation ID: {}", conversationId);
        } else {
            conversationId = request.getConversationId();
        }
        
        logger.info("Received chat request for repo {} with message: {} (conversation: {})", 
                request.getRepoId(), request.getMessage(), conversationId);
        
        // Retrieve relevant code chunks using RAG
        List<CodeChunk> relevantChunks = ragService.retrieve(
                request.getMessage(), 
                request.getRepoId()
        );
        
        // Build context from retrieved chunks
        String context = relevantChunks.stream()
                .limit(MAX_CONTEXT_CHUNKS)
                .map(chunk -> String.format(
                        "File: %s (lines %d-%d)\n```%s\n%s\n```",
                        chunk.getFilePath(),
                        chunk.getStartLine(),
                        chunk.getEndLine(),
                        chunk.getLanguage(),
                        chunk.getContent()
                ))
                .collect(Collectors.joining("\n\n"));
        
        // Create prompt with context
        String systemMessage = "You are a helpful coding assistant with deep knowledge of this codebase. " +
                "Answer questions based on the provided code context and previous conversation. " +
                "Remember information from earlier in our conversation. " +
                "Be specific and reference actual code when possible.";
        
        String userMessage = String.format(
                "Question: %s\n\nRelevant Code Context:\n%s",
                request.getMessage(),
                context.isEmpty() ? "No relevant code found." : context
        );
        
        logger.info("Calling AI with {} chunks of context for conversation {}", 
                relevantChunks.size(), conversationId);
        
        try {
            // Build ChatClient with chat memory advisor
            ChatClient chatClient = chatClientBuilder
                    .defaultSystem(systemMessage)
                    .defaultAdvisors(chatMemoryAdvisor)
                    .build();
            
            // Call AI with conversation memory using proper parameter keys
            String answer = chatClient.prompt()
                    .user(userMessage)
                    .advisors(a -> a
                            .param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId)
                            .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                    .call()
                    .content();
            
            logger.info("AI response generated successfully for conversation {}", conversationId);
            
            return ResponseEntity.ok(new ChatResponse(answer));
            
        } catch (Exception e) {
            logger.error("Error processing chat request: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(new ChatResponse("Error: " + e.getMessage()));
        }
    }
}
