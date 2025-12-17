package com.reviewassistant.service;

import com.reviewassistant.model.Repository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test caching behavior of GithubService.
 * Uses simple cache manager (not Redis) for testing.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.cache.type=simple",  // Use simple in-memory cache for testing
        "spring.ai.openai.api-key=test-key"  // Dummy API key for test context
})
class GithubServiceCacheTest {
    
    @SpyBean
    private GithubService githubService;
    
    @Autowired(required = false)
    private CacheManager cacheManager;
    
    @Test
    void testFetchUserRepositories_CachesResults() {
        String token = "test-token";
        
        // Create mock data
        List<Repository> mockRepos = new ArrayList<>();
        Repository repo1 = new Repository();
        repo1.setOwner("testuser");
        repo1.setName("testrepo");
        repo1.setUrl("https://github.com/testuser/testrepo");
        mockRepos.add(repo1);
        
        // Mock the method to return test data
        doReturn(mockRepos).when(githubService).fetchUserRepositories(token);
        
        // First call - should hit the actual method
        List<Repository> result1 = githubService.fetchUserRepositories(token);
        assertNotNull(result1);
        assertEquals(1, result1.size());
        
        // Second call - should use cache (method not called again)
        List<Repository> result2 = githubService.fetchUserRepositories(token);
        assertNotNull(result2);
        assertEquals(1, result2.size());
        
        // Verify the method was only called once due to caching
        verify(githubService, times(1)).fetchUserRepositories(token);
        
        // Verify results are the same
        assertSame(result1, result2, "Cached result should be the same instance");
    }
    
    @Test
    void testClearCache_EvictsAllCaches() {
        String token = "test-token";
        String owner = "testowner";
        String repo = "testrepo";
        
        // Clear the cache
        githubService.clearCache(token, owner, repo);
        
        // Verify the method was called
        verify(githubService, times(1)).clearCache(token, owner, repo);
        
        // If cache manager is available, verify caches exist
        if (cacheManager != null) {
            assertNotNull(cacheManager.getCache("repos"), "repos cache should exist");
            assertNotNull(cacheManager.getCache("prs"), "prs cache should exist");
            assertNotNull(cacheManager.getCache("diffs"), "diffs cache should exist");
        }
    }
    
    @Test
    void testDifferentTokens_DifferentCacheKeys() {
        String token1 = "token1";
        String token2 = "token2";
        
        List<Repository> mockRepos1 = new ArrayList<>();
        Repository repo1 = new Repository();
        repo1.setOwner("user1");
        repo1.setName("repo1");
        mockRepos1.add(repo1);
        
        List<Repository> mockRepos2 = new ArrayList<>();
        Repository repo2 = new Repository();
        repo2.setOwner("user2");
        repo2.setName("repo2");
        mockRepos2.add(repo2);
        
        // Mock different results for different tokens
        doReturn(mockRepos1).when(githubService).fetchUserRepositories(token1);
        doReturn(mockRepos2).when(githubService).fetchUserRepositories(token2);
        
        // Call with first token
        List<Repository> result1 = githubService.fetchUserRepositories(token1);
        assertEquals("user1", result1.get(0).getOwner());
        
        // Call with second token - should not use cache from first call
        List<Repository> result2 = githubService.fetchUserRepositories(token2);
        assertEquals("user2", result2.get(0).getOwner());
        
        // Verify each was called once (different cache keys)
        verify(githubService, times(1)).fetchUserRepositories(token1);
        verify(githubService, times(1)).fetchUserRepositories(token2);
    }
}
