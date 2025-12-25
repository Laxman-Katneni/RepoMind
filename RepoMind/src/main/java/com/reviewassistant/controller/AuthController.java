package com.reviewassistant.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {
    
    @GetMapping("/auth/success")
    public void authSuccess(HttpServletResponse response) throws Exception {
        // Session cookie is already set by Spring Security
        // Just redirect to frontend
        String frontendUrl = System.getenv().getOrDefault("FRONTEND_URL", "http://localhost:5173");
        response.sendRedirect(frontendUrl + "/app/select-repo");
    }
}
