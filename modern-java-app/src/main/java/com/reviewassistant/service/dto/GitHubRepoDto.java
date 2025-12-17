package com.reviewassistant.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for GitHub repository API response.
 * Maps JSON from GET /user/repos
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubRepoDto {
    
    public String name;
    
    @JsonProperty("full_name")
    public String fullName;
    
    @JsonProperty("html_url")
    public String htmlUrl;
    
    public OwnerDto owner;
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OwnerDto {
        public String login;
    }
}
