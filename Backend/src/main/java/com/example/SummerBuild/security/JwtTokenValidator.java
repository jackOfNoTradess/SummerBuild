package com.example.SummerBuild.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenValidator {

  private final Key key;
  private static final Logger logger = LoggerFactory.getLogger(JwtTokenValidator.class);

  public JwtTokenValidator(@Value("${supabase.jwt.secret}") String secret) {
    logger.info("Initializing JWT validator...");
    logger.info("JWT secret length: {}", secret != null ? secret.length() : "null");
    logger.info("JWT secret: {}", secret);
    try {
      byte[] decoded = Base64.getDecoder().decode(secret);

      // Use HMAC-SHA256 for Supabase compatibility (not SHA512)
      // Supabase uses HS256 algorithm, so we need to ensure minimum key length
      if (decoded.length < 32) {
        // If key is too short for HS256, pad it or throw error
        logger.warn("JWT secret may be too short for HS256. Length: {} bytes", decoded.length);
      }

      this.key = Keys.hmacShaKeyFor(java.util.Arrays.copyOf(decoded, 32));
      logger.info(
          "JWT validator initialized successfully with key algorithm: {}", key.getAlgorithm());
      logger.info("Key suitable for HS256: {}", decoded.length >= 32);

    } catch (Exception e) {
      logger.error("Failed to initialize JWT validator", e);
      throw new RuntimeException("JWT configuration error", e);
    }
  }

  public Claims validateToken(String token) {
    try {
      // Log token info for debugging
      if (token != null && token.length() > 50) {
        logger.debug("Validating token header: {}", token.substring(0, 50) + "...");
      }

      // Add this debugging section to decode more token info
      if (token != null && token.contains(".")) {
        try {
          String[] parts = token.split("\\.");
          if (parts.length >= 2) {
            String header = new String(Base64.getDecoder().decode(parts[0]));
            String payload = new String(Base64.getDecoder().decode(parts[1]));
            logger.debug("Token header: {}", header);
            logger.debug("Token payload (first 100 chars): {}",
                payload.length() > 100 ? payload.substring(0, 100) + "..." : payload);
          }
        } catch (Exception ex) {
          logger.debug("Could not decode token for debugging", ex);
        }
      }

      Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

      logger.debug("Token validation successful for user: {}", claims.getSubject());
      return claims;

    } catch (io.jsonwebtoken.security.SignatureException e) {
      logger.error("JWT signature validation failed!");
      logger.error("This indicates the token was signed with a different secret key");
      logger.error("Expected key algorithm: {}", key.getAlgorithm());

      // Decode token header to see what algorithm was used
      if (token != null && token.contains(".")) {
        try {
          String[] parts = token.split("\\.");
          if (parts.length >= 2) {
            String header = new String(Base64.getDecoder().decode(parts[0]));
            logger.error("Token header: {}", header);
            logger.error(
                "SOLUTION: Verify your Supabase JWT secret matches the one used to sign this token");
          }
        } catch (Exception ex) {
          logger.debug("Could not decode token header", ex);
        }
      }
      return null;

    } catch (Exception e) {
      logger.error("Token validation error: {}", e.getMessage());
      logger.debug("Full validation error", e);
      return null;
    }
  }
}
