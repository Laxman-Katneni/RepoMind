package com.reviewassistant.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO for GitHub pull request file API response.
 * Maps JSON from GET /repos/{owner}/{repo}/pulls/{number}/files
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubPullRequestFileDto {
    
    public String filename;
    
    public String status;
    
    public String patch;
}
