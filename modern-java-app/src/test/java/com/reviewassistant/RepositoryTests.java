package com.reviewassistant;

import com.reviewassistant.model.Repository;
import com.reviewassistant.repository.RepositoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Repository entity and RepositoryRepository.
 * Tests basic database operations to verify JPA and DB connection.
 */
@SpringBootTest
@Transactional
public class RepositoryTests {
    
    @Autowired
    private RepositoryRepository repositoryRepository;
    
    @Test
    public void testSaveAndFindRepository() {
        // Create a new repository
        Repository repo = new Repository();
        repo.setOwner("testuser");
        repo.setName("test-repo");
        repo.setUrl("https://github.com/testuser/test-repo");
        
        // Save it
        Repository saved = repositoryRepository.save(repo);
        
        // Verify it was saved with an ID
        assertNotNull(saved.getId());
        assertEquals("testuser", saved.getOwner());
        assertEquals("test-repo", saved.getName());
        assertEquals("https://github.com/testuser/test-repo", saved.getUrl());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        
        // Find by ID
        Optional<Repository> found = repositoryRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getOwner());
        assertEquals("test-repo", found.get().getName());
    }
    
    @Test
    public void testFindByOwnerAndName() {
        // Create and save a repository
        Repository repo = new Repository();
        repo.setOwner("github");
        repo.setName("spring-boot");
        repo.setUrl("https://github.com/github/spring-boot");
        
        repositoryRepository.save(repo);
        
        // Find by owner and name
        Optional<Repository> found = repositoryRepository.findByOwnerAndName("github", "spring-boot");
        
        assertTrue(found.isPresent());
        assertEquals("github", found.get().getOwner());
        assertEquals("spring-boot", found.get().getName());
        assertEquals("https://github.com/github/spring-boot", found.get().getUrl());
    }
    
    @Test
    public void testFindByOwnerAndName_NotFound() {
        // Try to find a repository that doesn't exist
        Optional<Repository> found = repositoryRepository.findByOwnerAndName("nonexistent", "repo");
        
        assertFalse(found.isPresent());
    }
}
