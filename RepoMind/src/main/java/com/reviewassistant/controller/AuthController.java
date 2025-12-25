package com.reviewassistant.controller;

import com.reviewassistant.model.UserGithubToken;
import com.reviewassistant.repository.UserGithubTokenRepository;
import com.reviewassistant.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {
    
    private final UserGithubTokenRepository tokenRepository;
    private final JwtUtil jwtUtil;
    private final OAuth2AuthorizedClientService authorizedClientService;
    
    @Value("${FRONTEND_URL:http://localhost:5173}")
    private String frontendUrl;
    
    public AuthController(
            UserGithubTokenRepository tokenRepository, 
            JwtUtil jwtUtil,
            OAuth2AuthorizedClientService authorizedClientService) {
        this.tokenRepository = tokenRepository;
        this.jwtUtil = jwtUtil;
        this.authorizedClientService = authorizedClientService;
    }
    
    @GetMapping("/auth/callback")
    public void authCallback(
            OAuth2AuthenticationToken authentication,
            HttpServletResponse response) throws Exception {
        
        // Get OAuth2User from authentication
        OAuth2User oauth2User = authentication.getPrincipal();
        
        // Get the authorized client to extract access token
        OAuth2AuthorizedClient authorizedClient = authorizedClientService
                .loadAuthorizedClient(
                        authentication.getAuthorizedClientRegistrationId(),
                        authentication.getName()
                );
        
        if (authorizedClient == null) {
            System.err.println("ERROR: No authorized client found!");
            response.sendRedirect(frontendUrl + "/login?error=oauth");
            return;
        }
        
        // Extract user info from GitHub OAuth
        Long githubId = oauth2User.getAttribute("id");
        String username = oauth2User.getAttribute("login");
        String accessToken = authorizedClient.getAccessToken().getTokenValue();
        
        System.out.println("=== OAuth Success ===");
        System.out.println("GitHub ID: " + githubId);
        System.out.println("Username: " + username);
        
        // Store or update GitHub access token in database
        UserGithubToken userToken = tokenRepository.findByGithubId(githubId)
                .orElse(new UserGithubToken());
        
        userToken.setGithubId(githubId);
        userToken.setUsername(username);
        userToken.setAccessToken(accessToken);
        tokenRepository.save(userToken);
        
        // Generate JWT for frontend
        String jwt = jwtUtil.generateToken(username, githubId);
        
        // Redirect to frontend with JWT in URL fragment
        response.sendRedirect(frontendUrl + "/auth/callback#token=" + jwt);
    }
}
