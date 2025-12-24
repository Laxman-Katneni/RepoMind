package com.reviewassistant.service;

import com.reviewassistant.model.CodeChunk;
import com.reviewassistant.model.Repository;
import com.reviewassistant.repository.CodeChunkRepository;
import com.reviewassistant.repository.RepositoryRepository;
import com.reviewassistant.service.dto.GitHubFileDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for RagService.
 * Tests the indexing flow with mocked dependencies.
 */
@SpringBootTest
class RagServiceTest {
    
    @Autowired
    private RagService ragService;
    
    @MockBean
    private GithubService githubService;
    
    @MockBean
    private EmbeddingModel embeddingModel;
    
    @MockBean
    private CodeChunkRepository codeChunkRepository;
    
    @MockBean
    private RepositoryRepository repositoryRepository;
    
    private Repository testRepository;
    private GitHubFileDto testFile;
    private float[] testEmbedding;
    
    @BeforeEach
    void setUp() {
        // Create test repository
        testRepository = new Repository();
        testRepository.setId(1L);
        testRepository.setOwner("testuser");
        testRepository.setName("testrepo");
        testRepository.setUrl("https://github.com/testuser/testrepo");
        
        // Create test file
        testFile = new GitHubFileDto();
        testFile.path = "src/main/java/Example.java";
        testFile.type = "blob";
        testFile.size = 500L;
        
        // Create test embedding (1536 dimensions for OpenAI)
        testEmbedding = new float[1536];
        Arrays.fill(testEmbedding, 0.1f);
    }
    
    @Test
    void testIndexRepository_Success() {
        // Arrange
        String token = "test-github-token";
        Long repoId = 1L;
        
        String fileContent = "public class Example {\n" +
                "    public static void main(String[] args) {\n" +
                "        System.out.println(\"Hello, World!\");\n" +
                "    }\n" +
                "}";
        
        // Mock repository fetch
        when(repositoryRepository.findById(repoId))
                .thenReturn(Optional.of(testRepository));
        
        // Mock GitHub file list
        when(githubService.fetchRepositoryFiles(token, "testuser", "testrepo"))
                .thenReturn(Arrays.asList(testFile));
        
        // Mock file content fetch
        when(githubService.fetchFileContent(token, "testuser", "testrepo", "src/main/java/Example.java"))
                .thenReturn(fileContent);
        
        // Mock embedding generation
        when(embeddingModel.embed(anyString()))
                .thenReturn(testEmbedding);
        
        // Mock save operation
        when(codeChunkRepository.save(any(CodeChunk.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ragService.indexRepository(repoId, token);
        
        // Assert
        verify(repositoryRepository, times(1)).findById(repoId);
        verify(githubService, times(1)).fetchRepositoryFiles(token, "testuser", "testrepo");
        verify(githubService, times(1)).fetchFileContent(token, "testuser", "testrepo", "src/main/java/Example.java");
        verify(embeddingModel, atLeastOnce()).embed(anyString());
        verify(codeChunkRepository, atLeastOnce()).save(any(CodeChunk.class));
    }
    
    @Test
    void testIndexRepository_SkipsNonCodeFiles() {
        // Arrange
        String token = "test-github-token";
        Long repoId = 1L;
        
        GitHubFileDto imageFile = new GitHubFileDto();
        imageFile.path = "images/logo.png";
        imageFile.type = "blob";
        imageFile.size = 1024L;
        
        // Mock repository fetch
        when(repositoryRepository.findById(repoId))
                .thenReturn(Optional.of(testRepository));
        
        // Mock GitHub file list with non-code file
        when(githubService.fetchRepositoryFiles(token, "testuser", "testrepo"))
                .thenReturn(Arrays.asList(imageFile));
        
        // Act
        ragService.indexRepository(repoId, token);
        
        // Assert
        verify(githubService, times(1)).fetchRepositoryFiles(token, "testuser", "testrepo");
        verify(githubService, never()).fetchFileContent(anyString(), anyString(), anyString(), anyString());
        verify(embeddingModel, never()).embed(anyString());
        verify(codeChunkRepository, never()).save(any(CodeChunk.class));
    }
}
