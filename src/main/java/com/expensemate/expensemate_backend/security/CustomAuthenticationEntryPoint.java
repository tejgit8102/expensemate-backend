package com.expensemate.expensemate_backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // Set response type and status
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        // Build structured error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", 401);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("path", request.getRequestURI());

        // Handle different authentication failures
        if (authException instanceof DisabledException) {
            errorResponse.put("message", "Your account is deactivated. Please contact the administrator.");
        } else {
            errorResponse.put("message", authException.getMessage() != null
                    ? authException.getMessage()
                    : "Authentication failed. Please provide valid credentials.");
        }

        // Write JSON response
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
