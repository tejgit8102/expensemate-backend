package com.expensemate.expensemate_backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest; // ✅ Import for HttpServletRequest

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final String secret;
    private final long expirationTime;
    private final Key key;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") long expirationTime) {
        this.secret = secret;
        this.expirationTime = expirationTime;
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // ✅ Generate token with extra claims (userId + role + username)
    public String generateToken(Long userId, String email, String role, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put("username", username);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email) // email as subject
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ Backward-compatible: generate token with just username
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ✅ Extract email/username (subject)
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // ✅ Extract userId
    public Long extractUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    // ✅ Extract role
    public String extractRole(String token) {
        Object role = extractAllClaims(token).get("role");
        return role != null ? role.toString() : null;
    }

    // ✅ Extract username
    public String extractUsernameFromToken(String token) {
        Object username = extractAllClaims(token).get("username");
        return username != null ? username.toString() : null;
    }

    // ✅ Validate token
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return !isTokenExpired(token);
        } catch (JwtException e) {
            return false;
        }
    }

    // ✅ Validate against UserDetails (Spring Security)
    public boolean validateToken(String token, org.springframework.security.core.userdetails.UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ✅ New method: Extract userId directly from HttpServletRequest
    public Long extractUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        System.out.println("JwtUtil: Authorization header: " + authHeader);
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            System.out.println("JwtUtil: Extracted token: " + token.substring(0, Math.min(20, token.length())) + "...");
            
            try {
                Long userId = extractUserId(token);
                System.out.println("JwtUtil: Extracted userId: " + userId);
                return userId;
            } catch (Exception e) {
                System.out.println("JwtUtil: Error extracting userId from token: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
        
        System.out.println("JwtUtil: No valid Authorization header found");
        return null;
    }
}
