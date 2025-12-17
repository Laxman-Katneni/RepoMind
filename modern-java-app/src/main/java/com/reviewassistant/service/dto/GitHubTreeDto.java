package com.reviewassistant.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO for GitHub Git Tree API response.
 * Maps JSON from GET /repos/{owner}/{repo}/git/trees/{sha}?recursive=1
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubTreeDto {
    
    public String sha;
    
    public String url;
    
    public List<GitHubFileDto> tree;
    
    public Boolean truncated;
}
