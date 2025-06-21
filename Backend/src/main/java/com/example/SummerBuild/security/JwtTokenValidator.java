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
    byte[] decoded = Base64.getDecoder().decode(secret);
    this.key = Keys.hmacShaKeyFor(decoded);
  }

  public Claims validateToken(String token) {
    try {
      return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    } catch (Exception e) {
      logger.error("Error in validating token: {}", token);
      logger.error("Error message in validating token: {}", e.getMessage());
      return null;
    }
  }
}
