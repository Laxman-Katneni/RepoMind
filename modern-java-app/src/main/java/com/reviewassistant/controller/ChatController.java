package com.reviewassistant.controller;

import com.reviewassistant.model.CodeChunk;
import com.reviewassistant.service.RagService;
import com.reviewassistant.service.dto.ChatRequest;
import com.reviewassistant.service.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for AI chat functionality.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private static final int MAX_CONTEXT_CHUNKS = 5;
    
    private final RagService ragService;
    private final ChatClient chatClient;
    
    public ChatController(RagService ragService, ChatClient.Builder chatClientBuilder) {
        this.ragService = ragService;
        this.chatClient = chatClientBuilder.build();
    }
    
    /**
     * Chat with the codebase using RAG-powered AI.
     * 
     * @param request Chat request with message and repository ID
     * @return AI-generated response
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        
        logger.info("Received chat request for repo {} with message: {}", 
                request.getRepoId(), request.getMessage());
        
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
                "Answer questions based on the provided code context. " +
                "Be specific and reference actual code when possible.";
        
        String userMessage = String.format(
                "Question: %s\n\nRelevant Code Context:\n%s",
                request.getMessage(),
                context.isEmpty() ? "No relevant code found." : context
        );
        
        logger.info("Calling AI with {} chunks of context", relevantChunks.size());
        
        // Call AI
        String answer = chatClient.prompt()
                .system(systemMessage)
                .user(userMessage)
                .call()
                .content();
        
        logger.info("AI response generated successfully");
        
        return ResponseEntity.ok(new ChatResponse(answer));
    }
}
