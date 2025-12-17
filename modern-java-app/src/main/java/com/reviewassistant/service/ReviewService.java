package com.reviewassistant.service;

import com.reviewassistant.model.CodeChunk;
import com.reviewassistant.model.PullRequest;
import com.reviewassistant.model.Repository;
import com.reviewassistant.model.ReviewComment;
import com.reviewassistant.model.ReviewRun;
import com.reviewassistant.repository.PullRequestRepository;
import com.reviewassistant.repository.ReviewRunRepository;
import com.reviewassistant.repository.RepositoryRepository;
import com.reviewassistant.service.dto.AiReviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for AI-powered pull request code review.
 * Uses Spring AI ChatClient with structured output parsing.
 */
@Service
public class ReviewService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);
    private static final int CONTEXT_CHUNKS_LIMIT = 3;
    
    private final GithubService githubService;
    private final RagService ragService;
    private final ChatClient chatClient;
    private final ReviewRunRepository reviewRunRepository;
    private final PullRequestRepository pullRequestRepository;
    private final RepositoryRepository repositoryRepository;
    
    public ReviewService(
            GithubService githubService,
            RagService ragService,
            ChatClient.Builder chatClientBuilder,
            ReviewRunRepository reviewRunRepository,
            PullRequestRepository pullRequestRepository,
            RepositoryRepository repositoryRepository) {
        this.githubService = githubService;
        this.ragService = ragService;
        this.chatClient = chatClientBuilder.build();
        this.reviewRunRepository = reviewRunRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.repositoryRepository = repositoryRepository;
    }
    
    /**
     * Analyze a pull request using AI and save the review results.
     * 
     * @param repoId Database ID of the repository
     * @param prNumber Pull request number
     * @param token GitHub OAuth access token
     * @return Saved ReviewRun entity with comments
     */
    @Transactional
    public ReviewRun analyzePr(Long repoId, Integer prNumber, String token) {
        logger.info("Starting AI review for PR #{} in repository ID {}", prNumber, repoId);
        
        // Fetch repository
        Repository repository = repositoryRepository.findById(repoId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found: " + repoId));
        
        // Fetch PR details from GitHub
        List<PullRequest> prs = githubService.fetchOpenPullRequests(
                token, repository.getOwner(), repository.getName());
        
        PullRequest pr = prs.stream()
                .filter(p -> p.getNumber().equals(prNumber))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "PR #" + prNumber + " not found in " + repository.getOwner() + "/" + repository.getName()));
        
        // Set repository relationship
        pr.setRepository(repository);
        
        // Save or update PR
        PullRequest savedPr = pullRequestRepository.findByRepositoryIdAndNumber(repoId, prNumber)
                .map(existing -> {
                    existing.setTitle(pr.getTitle());
                    existing.setAuthor(pr.getAuthor());
                    existing.setHtmlUrl(pr.getHtmlUrl());
                    existing.setBaseBranch(pr.getBaseBranch());
                    existing.setHeadBranch(pr.getHeadBranch());
                    existing.setHeadSha(pr.getHeadSha());
                    existing.setBody(pr.getBody());
                    return pullRequestRepository.save(existing);
                })
                .orElseGet(() -> pullRequestRepository.save(pr));
        
        // SMART CACHING: Check if we already reviewed this exact commit
        // If headSha matches an existing review, return cached result (saves OpenAI $$$)
        List<ReviewRun> existingReviews = reviewRunRepository.findByPullRequestId(savedPr.getId());
        for (ReviewRun existingRun : existingReviews) {
            if (pr.getHeadSha().equals(existingRun.getPullRequest().getHeadSha())) {
                logger.info("Found existing review for commit {} - returning cached result",
                        pr.getHeadSha());
                return existingRun;
            }
        }
        
        logger.info("No cached review found for commit {} - proceeding with AI analysis",
                pr.getHeadSha());
        
        // Fetch PR diff
        String diff = githubService.fetchPrDiff(
                token, repository.getOwner(), repository.getName(), prNumber);
        
        // Retrieve context from RAG (optional, use PR title as query)
        String context = "";
        try {
            List<CodeChunk> relevantChunks = ragService.retrieve(pr.getTitle(), repoId);
            if (!relevantChunks.isEmpty()) {
                context = "\n\n### Relevant Code Context:\n" +
                        relevantChunks.stream()
                                .limit(CONTEXT_CHUNKS_LIMIT)
                                .map(chunk -> String.format("File: %s\n```%s\n%s\n```",
                                        chunk.getFilePath(),
                                        chunk.getLanguage(),
                                        chunk.getContent()))
                                .collect(Collectors.joining("\n\n"));
            }
        } catch (Exception e) {
            logger.warn("Failed to retrieve RAG context: {}", e.getMessage());
            // Continue without context
        }
        
        // Create AI prompt
        String systemMessage = "You are a senior code reviewer with expertise in software engineering best practices. " +
                "Analyze the provided pull request diff and provide constructive feedback. " +
                "Focus on code quality, potential bugs, security issues, and maintainability. " +
                "Be specific and provide actionable suggestions.";
        
        String userMessage = String.format(
                "# Pull Request Review\n\n" +
                "**Title:** %s\n\n" +
                "**Description:**\n%s\n\n" +
                "**Diff:**\n```diff\n%s\n```%s",
                pr.getTitle(),
                pr.getBody() != null ? pr.getBody() : "No description provided",
                diff,
                context
        );
        
        // Use BeanOutputConverter for structured output
        BeanOutputConverter<AiReviewResponse> outputConverter = 
                new BeanOutputConverter<>(AiReviewResponse.class);
        
        String format = outputConverter.getFormat();
        
        logger.info("Calling AI with structured output format");
        
        // Call AI
        AiReviewResponse aiResponse = chatClient.prompt()
                .system(systemMessage)
                .user(userMessage + "\n\n" + format)
                .call()
                .entity(AiReviewResponse.class);
        
        logger.info("AI review completed with {} comments", 
                aiResponse.getComments() != null ? aiResponse.getComments().size() : 0);
        
        // Create ReviewRun entity
        ReviewRun reviewRun = new ReviewRun();
        reviewRun.setPullRequest(savedPr);
        reviewRun.setSummary(aiResponse.getSummary());
        reviewRun.setCommentCount(aiResponse.getComments() != null ? aiResponse.getComments().size() : 0);
        
        // Save ReviewRun first to get ID
        ReviewRun savedReviewRun = reviewRunRepository.save(reviewRun);
        
        // Map and save ReviewComments
        if (aiResponse.getComments() != null) {
            List<ReviewComment> comments = aiResponse.getComments().stream()
                    .map(commentDto -> mapToReviewComment(commentDto, savedReviewRun))
                    .collect(Collectors.toList());
            
            savedReviewRun.setComments(comments);
        }
        
        logger.info("Review run saved with ID: {}", savedReviewRun.getId());
        
        return savedReviewRun;
    }
    
    /**
     * Publish a review back to GitHub as PR comments.
     * 
     * @param runId ReviewRun database ID
     * @param token GitHub OAuth access token
     */
    public void publishReview(Long runId, String token) {
        logger.info("Publishing review {} to GitHub", runId);
        
        ReviewRun reviewRun = reviewRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("ReviewRun not found: " + runId));
        
        PullRequest pr = reviewRun.getPullRequest();
        Repository repo = pr.getRepository();
        
        githubService.postPullRequestReview(
                token,
                repo.getOwner(),
                repo.getName(),
                pr.getNumber(),
                pr.getHeadSha(),
                reviewRun.getComments(),
                reviewRun.getSummary()
        );
        
        logger.info("Review {} successfully published to GitHub", runId);
    }
    
    /**
     * Map AI comment DTO to ReviewComment entity.
     */
    private ReviewComment mapToReviewComment(AiReviewResponse.CommentDto dto, ReviewRun reviewRun) {
        ReviewComment comment = new ReviewComment();
        comment.setReviewRun(reviewRun);
        comment.setFilePath(dto.getFilePath());
        comment.setLineNumber(dto.getLineNumber());
        comment.setSeverity(dto.getSeverity());
        comment.setCategory(dto.getCategory());
        comment.setRationale(dto.getRationale());
        comment.setSuggestion(dto.getSuggestion());
        comment.setBody(String.format("%s: %s", dto.getCategory(), dto.getRationale()));
        return comment;
    }
}
