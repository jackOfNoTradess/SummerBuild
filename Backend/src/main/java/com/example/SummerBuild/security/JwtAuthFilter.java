package com.example.SummerBuild.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtTokenValidator tokenValidator;
  private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

  public JwtAuthFilter(JwtTokenValidator tokenValidator) {
    this.tokenValidator = tokenValidator;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      Claims claims = tokenValidator.validateToken(token);

      if (claims != null) {
        // Extract user identifier from claims
        // In tests: "sub" contains UUID directly
        // In production: "sub" might contain Supabase user ID
        String userIdentifier = extractUserIdentifier(claims);

        logger.debug("Extracted user identifier: {}", userIdentifier);

        if (userIdentifier != null) {
          var authentication =
              new UsernamePasswordAuthenticationToken(
                  userIdentifier, null, Collections.emptyList());
          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authentication);
          logger.debug("Authentication set for user: {}", userIdentifier);
        } else {
          logger.warn("Could not extract user identifier from JWT claims");
        }
      } else {
        logger.warn("JWT token validation failed");
      }
    }

    filterChain.doFilter(request, response);
  }

  /**
   * Extracts user identifier from JWT claims. Handles both test environment (mock JWT) and
   * production (Supabase JWT).
   */
  private String extractUserIdentifier(Claims claims) {
    // First try to get from "sub" claim (standard JWT claim for subject)
    String sub = claims.get("sub", String.class);

    if (sub != null && !sub.isEmpty()) {
      logger.debug("Found user identifier in 'sub' claim: {}", sub);
      return sub;
    }

    // Fallback: try email if sub is not available (shouldn't happen in your case)
    String email = claims.get("email", String.class);
    if (email != null && !email.isEmpty()) {
      logger.debug("Fallback to email as user identifier: {}", email);
      return email;
    }

    logger.warn("No valid user identifier found in JWT claims: {}", claims);
    return null;
  }
}
