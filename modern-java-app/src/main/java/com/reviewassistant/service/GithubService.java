package com.reviewassistant.service;

import com.reviewassistant.exception.GithubException;
import com.reviewassistant.exception.RateLimitException;
import com.reviewassistant.model.PullRequest;
import com.reviewassistant.model.Repository;
import com.reviewassistant.service.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for interacting with GitHub API.
 * Replicates functionality from Python github_auth.py and github_pr_client.py.
 */
@Service
public class GithubService {
    
    private static final Logger logger = LoggerFactory.getLogger(GithubService.class);
    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final int PER_PAGE = 100;
    
    private final RestClient restClient;
    
    public GithubService() {
        this.restClient = RestClient.builder()
                .baseUrl(GITHUB_API_BASE)
                .build();
    }
    
    /**
     * Fetch all repositories accessible to the authenticated user.
     * Replicates Python: get_user_repos(access_token)
     * Cached for 10 minutes to reduce GitHub API calls.
     * 
     * @param token GitHub OAuth access token
     * @return List of Repository entities (not persisted)
     * @throws RateLimitException if rate limit is exceeded
     * @throws GithubException if GitHub API fails
     */
    @Cacheable(value = "repos", key = "#token")
    @Retryable(
            retryFor = {RateLimitException.class, GithubException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public List<Repository> fetchUserRepositories(String token) {
        logger.info("Fetching user repositories from GitHub");
        
        List<GitHubRepoDto> allRepos = new ArrayList<>();
        int page = 1;
        boolean hasMore = true;
        
        while (hasMore) {
            try {
                int currentPage = page;  // Make effectively final for lambda
                List<GitHubRepoDto> batch = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/user/repos")
                                .queryParam("per_page", PER_PAGE)
                                .queryParam("page", currentPage)
                                .queryParam("sort", "updated")
                                .build())
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/vnd.github+json")
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<GitHubRepoDto>>() {});
                
                if (batch == null || batch.isEmpty()) {
                    hasMore = false;
                } else {
                    allRepos.addAll(batch);
                    page++;
                }
            } catch (HttpClientErrorException e) {
                handleHttpError(e, "fetch user repositories");
            } catch (HttpServerErrorException e) {
                throw new GithubException("GitHub server error: " + e.getMessage(), e.getStatusCode().value(), e);
            } catch (RestClientException e) {
                throw new GithubException("Failed to fetch user repositories: " + e.getMessage(), e);
            }
        }
        
        logger.info("Fetched {} repositories from GitHub", allRepos.size());
        
        // Convert DTOs to entities
        return allRepos.stream()
                .map(this::mapToRepositoryEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * Fetch open pull requests for a repository.
     * Replicates Python: list_pull_requests(owner, repo, access_token)
     * Cached for 10 minutes per repository to reduce GitHub API calls.
     * 
     * @param token GitHub OAuth access token
     * @param owner Repository owner
     * @param repo Repository name
     * @return List of PullRequest entities (not persisted)
     */
    @Cacheable(value = "prs", key = "#owner + '-' + #repo")
    public List<PullRequest> fetchOpenPullRequests(String token, String owner, String repo) {
        logger.info("Fetching open pull requests for {}/{}", owner, repo);
        
        List<GitHubPullRequestDto> prs = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/pulls")
                        .queryParam("state", "open")
                        .queryParam("sort", "updated")
                        .queryParam("direction", "desc")
                        .build(owner, repo))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(new ParameterizedTypeReference<List<GitHubPullRequestDto>>() {});
        
        if (prs == null) {
            prs = new ArrayList<>();
        }
        
        logger.info("Fetched {} open pull requests", prs.size());
        
        // Convert DTOs to entities
        return prs.stream()
                .map(dto -> mapToPullRequestEntity(dto, owner, repo))
                .collect(Collectors.toList());
    }
    
    /**
     * Fetch the raw diff/patch text for a pull request.
     * Replicates Python: get_pull_request_files(owner, repo, pr_number, access_token)
     * Cached for 10 minutes per PR to reduce GitHub API calls.
     * 
     * @param token GitHub OAuth access token
     * @param owner Repository owner
     * @param repo Repository name
     * @param prNumber Pull request number
     * @return Combined diff text from all files
     */
    @Cacheable(value = "diffs", key = "#owner + '-' + #repo + '-' + #prNumber")
    public String fetchPrDiff(String token, String owner, String repo, int prNumber) {
        logger.info("Fetching diff for PR #{} in {}/{}", prNumber, owner, repo);
        
        List<GitHubPullRequestFileDto> allFiles = new ArrayList<>();
        int page = 1;
        boolean hasMore = true;
        
        while (hasMore) {
            int currentPage = page;  // Make effectively final for lambda
            List<GitHubPullRequestFileDto> batch = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/repos/{owner}/{repo}/pulls/{number}/files")
                            .queryParam("per_page", PER_PAGE)
                            .queryParam("page", currentPage)
                            .build(owner, repo, prNumber))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GitHubPullRequestFileDto>>() {});
            
            if (batch == null || batch.isEmpty()) {
                hasMore = false;
            } else {
                allFiles.addAll(batch);
                page++;
            }
        }
        
        logger.info("Fetched {} files for PR #{}", allFiles.size(), prNumber);
        
        // Combine all patches into a single diff string
        StringBuilder combinedDiff = new StringBuilder();
        for (GitHubPullRequestFileDto file : allFiles) {
            if (file.patch != null && !file.patch.isEmpty()) {
                combinedDiff.append("--- a/").append(file.filename).append("\n");
                combinedDiff.append("+++ b/").append(file.filename).append("\n");
                combinedDiff.append(file.patch).append("\n\n");
            }
        }
        
        return combinedDiff.toString();
    }
    
    
    /**
     * Fetch all files in a repository using Git Tree API.
     * Uses recursive=1 to get the entire file tree.
     * 
     * @param token GitHub OAuth access token
     * @param owner Repository owner
     * @param repo Repository name
     * @return List of files with their paths and metadata
     */
    public List<GitHubFileDto> fetchRepositoryFiles(String token, String owner, String repo) {
        logger.info("Fetching file tree for repository {}/{}", owner, repo);
        
        // First, get the default branch
        GitHubRepoDto repoInfo = restClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(GitHubRepoDto.class);
        
        if (repoInfo == null || repoInfo.fullName == null) {
            logger.warn("Could not fetch repository info for {}/{}", owner, repo);
            return new ArrayList<>();
        }
        
        // Get the tree using Git Tree API with recursive=1
        // Use HEAD or master as the tree SHA
        GitHubTreeDto tree = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/git/trees/HEAD")
                        .queryParam("recursive", "1")
                        .build(owner, repo))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(GitHubTreeDto.class);
        
        if (tree == null || tree.tree == null) {
            logger.warn("Could not fetch file tree for {}/{}", owner, repo);
            return new ArrayList<>();
        }
        
        // Filter out only files (type == "blob"), not directories
        List<GitHubFileDto> files = tree.tree.stream()
                .filter(item -> "blob".equals(item.type))
                .collect(Collectors.toList());
        
        logger.info("Fetched {} files from {}/{}", files.size(), owner, repo);
        return files;
    }
    
    /**
     * Fetch the content of a specific file from GitHub.
     * 
     * @param token GitHub OAuth access token
     * @param owner Repository owner
     * @param repo Repository name
     * @param path File path within the repository
     * @return Decoded file content as string, or null if not found
     */
    public String fetchFileContent(String token, String owner, String repo, String path) {
        try {
            GitHubFileContentDto content = restClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, repo, path)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .retrieve()
                    .body(GitHubFileContentDto.class);
            
            if (content != null && "base64".equals(content.encoding) && content.content != null) {
                // Decode base64 content
                byte[] decodedBytes = Base64.getDecoder().decode(content.content.replaceAll("\\s", ""));
                return new String(decodedBytes);
            }
        } catch (Exception e) {
            logger.error("Error fetching content for file {} in {}/{}: {}", path, owner, repo, e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Map GitHub repository DTO to Repository entity.
     */
    private Repository mapToRepositoryEntity(GitHubRepoDto dto) {
        Repository repo = new Repository();
        repo.setOwner(dto.owner.login);
        repo.setName(dto.name);
        repo.setUrl(dto.htmlUrl);
        return repo;
    }
    
    /**
     * Map GitHub pull request DTO to PullRequest entity.
     */
    private PullRequest mapToPullRequestEntity(GitHubPullRequestDto dto, String owner, String repo) {
        PullRequest pr = new PullRequest();
        pr.setNumber(dto.number);
        pr.setTitle(dto.title);
        pr.setAuthor(dto.user.login);
        pr.setHtmlUrl(dto.htmlUrl);
        pr.setBaseBranch(dto.base.ref);
        pr.setHeadBranch(dto.head.ref);
        pr.setHeadSha(dto.head.sha);
        pr.setBody(dto.body);
        // Note: repository relationship must be set by caller
        return pr;
    }
    
    /**
     * Post a pull request review with comments back to GitHub.
     * Maps our ReviewComments to GitHub's review comment format.
     * 
     * @param token GitHub OAuth access token
     * @param owner Repository owner
     * @param repo Repository name
     * @param prNumber Pull request number
     * @param commitId Commit SHA to attach the review to
     * @param comments List of review comments
     * @param summary Overall review summary
     */
    public void postPullRequestReview(
            String token,
            String owner,
            String repo,
            Integer prNumber,
            String commitId,
            List<com.reviewassistant.model.ReviewComment> comments,
            String summary) {
        
        logger.info("Posting review for PR #{} in {}/{}", prNumber, owner, repo);
        
        // Build GitHub review request body
        Map<String, Object> reviewRequest = new HashMap<>();
        reviewRequest.put("commit_id", commitId);
        reviewRequest.put("body", summary);
        reviewRequest.put("event", "COMMENT"); // APPROVE, REQUEST_CHANGES, or COMMENT
        
        // Map our comments to GitHub's format
        List<Map<String, Object>> githubComments = comments.stream()
                .filter(c -> c.getFilePath() != null && c.getLineNumber() != null)
                .map(comment -> {
                    Map<String, Object> ghComment = new HashMap<>();
                    ghComment.put("path", comment.getFilePath());
                    ghComment.put("line", comment.getLineNumber());
                    ghComment.put("body", formatCommentBody(comment));
                    return ghComment;
                })
                .collect(Collectors.toList());
        
        reviewRequest.put("comments", githubComments);
        
        try {
            // POST to GitHub API
            restClient.post()
                    .uri("/repos/{owner}/{repo}/pulls/{pr_number}/reviews", owner, repo, prNumber)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .body(reviewRequest)
                    .retrieve()
                    .toBodilessEntity();
            
            logger.info("Successfully posted review with {} comments", githubComments.size());
            
        } catch (HttpClientErrorException e) {
            handleHttpError(e, "post pull request review");
        } catch (RestClientException e) {
            throw new GithubException("Failed to post review: " + e.getMessage(), e);
        }
    }
    
    /**
     * Format a ReviewComment into GitHub-friendly markdown.
     */
    private String formatCommentBody(com.reviewassistant.model.ReviewComment comment) {
        StringBuilder body = new StringBuilder();
        
        // Add severity as emoji
        switch (comment.getSeverity().toLowerCase()) {
            case "critical":
                body.append("🔴 **Critical**\n\n");
                break;
            case "warning":
                body.append("🟡 **Warning**\n\n");
                break;
            default:
                body.append("ℹ️ **Info**\n\n");
                break;
        }
        
        // Add category
        body.append("**").append(comment.getCategory()).append("**\n\n");
        
        // Add rationale
        body.append(comment.getRationale());
        
        // Add suggestion if available
        if (comment.getSuggestion() != null && !comment.getSuggestion().isEmpty()) {
            body.append("\n\n**Suggestion:**\n").append(comment.getSuggestion());
        }
        
        return body.toString();
    }
    
    /**
     * Clear all GitHub API caches.
     * Useful for forcing a refresh of data from GitHub.
     * Can be called when user clicks a 'Refresh' button.
     * 
     * @param token GitHub OAuth access token (not used but kept for API consistency)
     * @param owner Repository owner (not used but kept for API consistency)
     * @param repo Repository name (not used but kept for API consistency)
     */
    @CacheEvict(value = {"repos", "prs", "diffs"}, allEntries = true)
    public void clearCache(String token, String owner, String repo) {
        logger.info("Clearing all GitHub API caches for {}/{}", owner, repo);
    }
    
    /**
     * Handle HTTP client errors from GitHub API.
     * Specifically detects rate limiting (403/429) and throws appropriate exceptions.
     */
    private void handleHttpError(HttpClientErrorException e, String operation) {
        int statusCode = e.getStatusCode().value();
        
        // Rate limiting
        if (statusCode == 403 || statusCode == 429) {
            String retryAfter = e.getResponseHeaders() != null && e.getResponseHeaders().getFirst("Retry-After") != null
                    ? e.getResponseHeaders().getFirst("Retry-After")
                    : "60";
            
            long retrySeconds = 60;
            try {
                retrySeconds = Long.parseLong(retryAfter);
            } catch (NumberFormatException ignored) {
                // Use default
            }
            
            logger.warn("GitHub rate limit exceeded while trying to {}", operation);
            throw new RateLimitException(
                    "GitHub rate limit exceeded. Please retry after " + retrySeconds + " seconds.",
                    retrySeconds
            );
        }
        
        // Other client errors
        throw new GithubException(
                "GitHub API error while trying to " + operation + ": " + e.getMessage(),
                statusCode,
                e
        );
    }
}
