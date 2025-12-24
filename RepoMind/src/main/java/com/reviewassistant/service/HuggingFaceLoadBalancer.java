package com.reviewassistant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Load balancer for distributing audit requests across multiple HuggingFace endpoints.
 * Uses round-robin distribution for fair load balancing.
 */
@Service
public class HuggingFaceLoadBalancer {
    
    private static final

 Logger logger = LoggerFactory.getLogger(HuggingFaceLoadBalancer.class);
    
    private final List<String> endpoints;
    private final AtomicInteger counter = new AtomicInteger(0);
    
    public HuggingFaceLoadBalancer(@Value("${finetuned.model.urls}") String endpointUrls) {
        // Parse comma-separated URLs from config
        this.endpoints = new ArrayList<>();
        
        if (endpointUrls != null && !endpointUrls.isEmpty()) {
            String[] urls = endpointUrls.split(",");
            for (String url : urls) {
                String trimmed = url.trim();
                if (!trimmed.isEmpty()) {
                    endpoints.add(trimmed);
                    logger.info("Added HuggingFace endpoint: {}", trimmed);
                }
            }
        }
        
        if (endpoints.isEmpty()) {
            logger.warn("No HuggingFace endpoints configured!");
        } else {
            logger.info("Initialized load balancer with {} endpoints", endpoints.size());
        }
    }
    
    /**
     * Gets the next endpoint using round-robin distribution.
     * Thread-safe and distributes load evenly across all endpoints.
     * 
     * @return Next endpoint URL
     */
    public String getNextEndpoint() {
        if (endpoints.isEmpty()) {
            throw new IllegalStateException("No HuggingFace endpoints configured");
        }
        
        if (endpoints.size() == 1) {
            return endpoints.get(0);
        }
        
        int index = counter.getAndIncrement() % endpoints.size();
        String endpoint = endpoints.get(index);
        
        logger.debug("Load balancer selected endpoint {} (index {})", endpoint, index);
        return endpoint;
    }
    
    /**
     * Returns the total number of configured endpoints.
     */
    public int getEndpointCount() {
        return endpoints.size();
    }
    
    /**
     * Returns all configured endpoints.
     */
    public List<String> getAllEndpoints() {
        return new ArrayList<>(endpoints);
    }
}
