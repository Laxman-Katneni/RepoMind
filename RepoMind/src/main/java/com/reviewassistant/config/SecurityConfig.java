package com.reviewassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
                
                // Protected routes - authentication required
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("http://localhost:5173/app/select-repo", true)
                .failureUrl("/login?error=true")
            )
            .csrf(csrf -> csrf
                // Disable CSRF for API endpoints (frontend uses cookie-based auth)
                .ignoringRequestMatchers("/api/**")
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
        
        // Allow frontend origin
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        
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
}
