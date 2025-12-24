package com.reviewassistant.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for GitHub file content API response.
 * Maps JSON from GET /repos/{owner}/{repo}/contents/{path}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubFileContentDto {
    
    public String name;
    
    public String path;
    
    public String content;  // Base64 encoded
    
    public String encoding;  // "base64"
    
    public Long size;
}
