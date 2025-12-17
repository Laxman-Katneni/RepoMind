package com.reviewassistant.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for GitHub file from Git Tree API.
 * Maps JSON from GET /repos/{owner}/{repo}/git/trees/{sha}?recursive=1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubFileDto {
    
    public String path;
    
    public String type;  // "blob" for files, "tree" for directories
    
    public String sha;
    
    public Long size;
    
    public String url;  // API URL to fetch file content
}
