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
  private final String supabaseUrl;

  public JwtTokenValidator(
      @Value("${supabase.jwt.secret}") String secret,
      @Value("${supabase.url:}") String supabaseUrl) {
    this.supabaseUrl = supabaseUrl;
    logger.info("Initializing JWT validator...");
    logger.info("Supabase URL: {}", supabaseUrl);

    try {
      // Supabase JWT secrets are Base64 encoded
      byte[] decodedSecret = Base64.getDecoder().decode(secret);
      this.key = Keys.hmacShaKeyFor(decodedSecret);

      logger.info("JWT validator initialized successfully");
      logger.info("Decoded secret length: {} bytes", decodedSecret.length);
      logger.info("Key algorithm: {}", key.getAlgorithm());

    } catch (Exception e) {
      logger.error("Failed to initialize JWT validator with Base64 secret", e);
      throw new RuntimeException("JWT configuration error: " + e.getMessage(), e);
    }
  }

  public Claims validateToken(String token) {
    try {
      // Parse and extract header for debugging
      String[] tokenParts = token.split("\\.");
      if (tokenParts.length >= 1) {
        String headerJson = new String(Base64.getUrlDecoder().decode(tokenParts[0]));
        logger.debug("Token header: {}", headerJson);
      }

      Claims claims =
          Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

      // Verify issuer matches expected Supabase URL
      String issuer = claims.getIssuer();
      logger.debug("Token issuer: {}", issuer);
      logger.debug("Expected Supabase URL: {}", supabaseUrl);

      if (issuer != null && supabaseUrl != null && !issuer.startsWith(supabaseUrl)) {
        logger.warn("Token issuer {} does not match expected Supabase URL {}", issuer, supabaseUrl);
      }

      logger.debug("Token validation successful for user: {}", claims.getSubject());
      return claims;

    } catch (io.jsonwebtoken.security.SignatureException e) {
      logger.error("JWT signature validation failed!");
      logger.error("This indicates the token was signed with a different secret key");
      logger.error("Expected key algorithm: {}", key.getAlgorithm());

      // Extract and log token details for debugging
      try {
        String[] tokenParts = token.split("\\.");
        if (tokenParts.length >= 1) {
          String headerJson = new String(Base64.getUrlDecoder().decode(tokenParts[0]));
          logger.error("Token header: {}", headerJson);
        }
        if (tokenParts.length >= 2) {
          String payloadJson = new String(Base64.getUrlDecoder().decode(tokenParts[1]));
          // Log first 200 chars to avoid logging sensitive data
          String payloadPreview =
              payloadJson.length() > 200 ? payloadJson.substring(0, 200) + "..." : payloadJson;
          logger.error("Token payload (first 200 chars): {}", payloadPreview);

          // Extract issuer from payload for troubleshooting
          if (payloadJson.contains("\"iss\":")) {
            String issuerMatch = payloadJson.replaceAll(".*\"iss\":\"([^\"]+)\".*", "$1");
            logger.error("Token was issued by: {}", issuerMatch);
          }
        }
      } catch (Exception parseEx) {
        logger.error("Could not parse token for debugging", parseEx);
      }

      logger.error(
          "SOLUTION 1: Verify your Supabase JWT secret matches the one used to sign this token");
      logger.error("SOLUTION 2: Check if the token issuer matches your Supabase project URL");
      logger.error("SOLUTION 3: Verify you're using the JWT Secret (not anon/service_role key)");
      return null;

    } catch (io.jsonwebtoken.ExpiredJwtException e) {
      logger.warn("Token has expired: {}", e.getMessage());
      return null;
    } catch (Exception e) {
      logger.error("Token validation error: {}", e.getMessage());
      return null;
    }
  }

  public String extractUserIdFromToken(String token) {
    Claims claims = validateToken(token);
    return claims != null ? claims.getSubject() : null;
  }
}
