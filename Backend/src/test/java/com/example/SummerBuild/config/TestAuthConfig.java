package com.example.SummerBuild.config;

import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.UserRole;
import com.example.SummerBuild.service.SupabaseAuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;

@Configuration
public class TestAuthConfig {

  @Value("${supabase.jwtSecret}")
  private String jwtSecret;

  @Bean
  @Primary
  public SupabaseAuthService supabaseAuthService() {
    return new SupabaseAuthService() {
      @Override
      public ResponseEntity<String> signup(
          String email, String password, String displayName, UserRole role, Gender gender) {
        String uuid = java.util.UUID.nameUUIDFromBytes(email.getBytes()).toString();
        return ResponseEntity.ok(
            "{\"user\":{\"id\":\"" + uuid + "\",\"email\":\"" + email + "\"}}");
      }

      @Override
      public ResponseEntity<String> login(String email, String password) {
        try {
          byte[] decoded = Base64.getDecoder().decode(jwtSecret);
          Key key = Keys.hmacShaKeyFor(decoded);

          String uuid = java.util.UUID.nameUUIDFromBytes(email.getBytes()).toString();

          Map<String, Object> claims = new HashMap<>();
          claims.put("sub", uuid);
          claims.put("email", email);
          claims.put("role", "USER");

          String token =
              Jwts.builder()
                  .setClaims(claims)
                  .setIssuedAt(new Date())
                  .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
                  .signWith(key)
                  .compact();

          return ResponseEntity.ok("{\"access_token\":\"" + token + "\"}");
        } catch (Exception e) {
          throw new RuntimeException("Failed to generate JWT token", e);
        }
      }
    };
  }
}
