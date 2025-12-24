package com.reviewassistant.service;

import com.reviewassistant.model.AiReviewResponse;
import com.reviewassistant.model.CodeChunk;
import com.reviewassistant.model.PullRequest;
import com.reviewassistant.model.Repository;
import com.reviewassistant.model.ReviewComment;
import com.reviewassistant.model.ReviewRun;
import com.reviewassistant.repository.PullRequestRepository;
import com.reviewassistant.repository.ReviewRunRepository;
import com.reviewassistant.repository.RepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AiService aiService;
    
    public ReviewService(
            GithubService githubService,
            RagService ragService,
            ChatClient.Builder chatClientBuilder,
            ReviewRunRepository reviewRunRepository,
            PullRequestRepository pullRequestRepository,
            RepositoryRepository repositoryRepository,
            AiService aiService) {
        this.githubService = githubService;
        this.ragService = ragService;
        this.chatClient = chatClientBuilder.build();
        this.reviewRunRepository = reviewRunRepository;
        this.pullRequestRepository = pullRequestRepository;
        this.repositoryRepository = repositoryRepository;
        this.aiService = aiService;
    }
    
    
    /**
     * Analyze a pull request by database ID with real AI integration.
     * Fetches real PR diff from GitHub and sends to AI for analysis.
     * 
     * @param prId Database ID of the pull request
     * @param token GitHub OAuth access token
     * @return Saved ReviewRun entity with AI-generated review
     */
    @Transactional
    public ReviewRun analyzePr(Long prId, String token) {
        logger.info("Starting real AI review for PR database ID: {}", prId);
        
        // 1. Fetch PR from database
        PullRequest pr = pullRequestRepository.findById(prId)
                .orElseThrow(() -> new IllegalArgumentException("PR not found: " + prId));
        
        logger.info("Found PR #{} - {}", pr.getNumber(), pr.getTitle());
        
        // Get repository
        Repository repository = pr.getRepository();
        if (repository == null) {
            throw new IllegalStateException("PR has no associated repository");
        }
        
        // 2. Fetch Real Diff from GitHub
        logger.info("Fetching diff from GitHub for PR #{}", pr.getNumber());
        String diff = githubService.fetchPrDiff(repository.getId(), pr.getNumber(), token);
        
        if (diff == null || diff.isEmpty()) {
            logger.warn("Empty diff received for PR #{}", pr.getNumber());
            diff = "No changes detected in this pull request.";
        }
        
        System.out.println("DIFF SENT TO AI: " + diff.substring(0, Math.min(diff.length(), 100)) + "...");
        logger.info("Diff length: {} characters", diff.length());
        
        // 3. Call Real AI with structured output
        logger.info("Calling AI service for structured code review");
        AiReviewResponse aiResponse = aiService.getReview(diff);
        
        if (aiResponse == null || aiResponse.issues() == null || aiResponse.issues().isEmpty()) {
            logger.warn("AI returned no issues for PR #{}", pr.getNumber());
        } else {
            logger.info("AI response received with {} issues", aiResponse.issues().size());
        }
        
        // 4. Save ReviewRun with AI summary
        ReviewRun reviewRun = new ReviewRun();
        reviewRun.setPullRequest(pr);
        
        int issueCount = aiResponse != null && aiResponse.issues() != null ? aiResponse.issues().size() : 0;
        reviewRun.setSummary("AI Analysis Completed: " + issueCount + " issues found");
        reviewRun.setCommentCount(issueCount);
        reviewRun = reviewRunRepository.save(reviewRun);
        
        logger.info("Saved ReviewRun with ID: {}", reviewRun.getId());
        
        // 5. Save each issue as a separate ReviewComment
        if (aiResponse != null && aiResponse.issues() != null && !aiResponse.issues().isEmpty()) {
            List<ReviewComment> comments = new ArrayList<>();
            
            for (AiReviewResponse.Issue issue : aiResponse.issues()) {
                ReviewComment comment = new ReviewComment();
                comment.setReviewRun(reviewRun);
                comment.setFilePath(issue.filePath() != null ? issue.filePath() : "unknown");
                comment.setLineNumber(issue.lineNumber() > 0 ? issue.lineNumber() : 1);
                
                // Normalize severity to lowercase for database
                String severity = issue.severity() != null ? issue.severity().toLowerCase() : "info";
                comment.setSeverity(severity);
                
                comment.setCategory(issue.category() != null ? issue.category() : "General");
                comment.setRationale(issue.description() != null ? issue.description() : "");
                comment.setSuggestion(issue.suggestion() != null ? issue.suggestion() : "");
                
                // Build body from description and suggestion
                StringBuilder body = new StringBuilder();
                if (issue.description() != null) {
                    body.append(issue.description());
                }
                if (issue.suggestion() != null && !issue.suggestion().isEmpty()) {
                    if (body.length() > 0) {
                        body.append("\n\nSuggestion: ");
                    }
                    body.append(issue.suggestion());
                }
                comment.setBody(body.length() > 0 ? body.toString() : "No description provided");
                
                comments.add(comment);
            }
            
            reviewRun.setComments(comments);
            logger.info("Created {} ReviewComment entities", comments.size());
        }
        
        logger.info("AI review completed successfully for PR #{}", pr.getNumber());
        
        return reviewRun;
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
}
