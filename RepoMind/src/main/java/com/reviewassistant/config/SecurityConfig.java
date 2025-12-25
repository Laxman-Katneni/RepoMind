package com.reviewassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;

import java.util.List;

/**
 * Security configuration for GitHub OAuth2 authentication.
 * Configures public and protected routes, enables OAuth2 login flow.
 * Uses cookie-based session authentication instead of tokens.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(authorize -> authorize
                // Public routes - no authentication required
                .requestMatchers("/", "/index.html", "/assets/**").permitAll()
                .requestMatchers("/api/webhooks/**").permitAll()
                .requestMatchers("/login", "/error").permitAll()
                
                // OAuth2 endpoints - must be public to initiate login
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                
                // Protected routes - authentication required
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                // Use cookie-based storage for authorization requests instead of session
                // This fixes issues with multiple backend instances on Render
                .authorizationEndpoint(authorization -> authorization
                    .authorizationRequestRepository(cookieAuthorizationRequestRepository())
                )
                .defaultSuccessUrl("/auth/success", true)
                .failureHandler((request, response, exception) -> {
                    // Log the OAuth failure for debugging
                    System.err.println("OAuth Login Failed: " + exception.getMessage());
                    exception.printStackTrace();
                    
                    // Redirect to frontend with error
                    String frontendUrl = System.getenv().getOrDefault("FRONTEND_URL", "http://localhost:5173");
                    response.sendRedirect(frontendUrl + "/login?error=oauth");
                })
            )
            .csrf(csrf -> csrf
                // Disable CSRF for API endpoints (frontend uses cookie-based auth)
                .ignoringRequestMatchers("/api/**")
            )
            .exceptionHandling(e -> e
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            );

        return http.build();
    }

    /**
     * CORS configuration to allow frontend origin with credentials.
     * Critical for cookie-based authentication to work across origins.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow frontend origin (localhost for dev, Render URL for production)
        String frontendUrl = System.getenv().getOrDefault("FRONTEND_URL", "http://localhost:5173");
        configuration.setAllowedOrigins(List.of("http://localhost:5173", frontendUrl));
        
        // Allow common HTTP methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Allow all headers
        configuration.setAllowedHeaders(List.of("*"));
        
        // CRITICAL: Allow credentials (cookies) to be sent
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Configure session cookie for cross-subdomain sharing.
     * Sets cookie domain to .onrender.com so cookies work across repomind-app and repomind-api.
     */
    @Bean
    public CookieSameSiteSupplier cookieSameSiteSupplier() {
        return CookieSameSiteSupplier.ofNone();
    }

    /**
     * Use cookie-based storage for OAuth2 authorization requests.
     * This prevents "authorization_request_not_found" errors when running multiple instances
     * since the authorization request is stored in a cookie, not server-side session.
     */
    @Bean
    public org.springframework.security.oauth2.client.web.AuthorizationRequestRepository<org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest> cookieAuthorizationRequestRepository() {
        // Using cookie-based repository instead of HttpSession-based
        // This stores the OAuth state in a cookie, making it available across all backend instances
        return new org.springframework.security.oauth2.client.web.HttpCookieOAuth2AuthorizationRequestRepository();
    }
}
