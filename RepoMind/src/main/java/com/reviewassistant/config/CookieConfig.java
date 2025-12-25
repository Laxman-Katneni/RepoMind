package com.reviewassistant.config;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.SessionCookieConfig;

/**
 * Custom cookie configuration for cross-subdomain session sharing.
 * Sets cookie domain to allow session cookies to work across different subdomains
 * (e.g., repomind-app.onrender.com and repomind-api.onrender.com).
 */
@Configuration
public class CookieConfig implements ServletContextInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();
        
        // Get environment variable for cookie domain
        String cookieDomain = System.getenv("COOKIE_DOMAIN");
        
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            // In production on Render: .onrender.com
            sessionCookieConfig.setDomain(cookieDomain);
            System.out.println("Session cookie domain set to: " + cookieDomain);
        } else {
            // In development: no domain (defaults to localhost)
            System.out.println("Session cookie domain not set (using default)");
        }
        
        // Note: Secure, HttpOnly, and SameSite are configured in application.properties
        // to avoid conflicts and ensure proper ordering
    }
}
