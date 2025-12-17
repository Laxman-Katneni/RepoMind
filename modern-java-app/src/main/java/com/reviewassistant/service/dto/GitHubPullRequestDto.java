package com.reviewassistant.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for GitHub pull request API response.
 * Maps JSON from GET /repos/{owner}/{repo}/pulls
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubPullRequestDto {
    
    public Integer number;
    
    public String title;
    
    public String body;
    
    @JsonProperty("html_url")
    public String htmlUrl;
    
    public UserDto user;
    
    public BranchDto base;
    
    public BranchDto head;
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserDto {
        public String login;
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BranchDto {
        public String ref;
        public String sha;
    }
}
