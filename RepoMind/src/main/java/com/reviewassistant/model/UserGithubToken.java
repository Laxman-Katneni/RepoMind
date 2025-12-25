package com.reviewassistant.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_github_tokens")
@Data
public class UserGithubToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private Long githubId;
    
    @Column(nullable = false)
    private String username;
    
    @Column(nullable = false, length = 1000)
    private String accessToken;
    
    @Column
    private String refreshToken;
    
    public UserGithubToken() {}
    
    public UserGithubToken(Long githubId, String username, String accessToken) {
        this.githubId = githubId;
        this.username = username;
        this.accessToken = accessToken;
    }
}
