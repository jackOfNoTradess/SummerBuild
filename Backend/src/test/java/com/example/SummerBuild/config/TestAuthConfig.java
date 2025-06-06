package com.example.SummerBuild.config;

import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.UserRole;
import com.example.SummerBuild.service.SupabaseAuthService;
import com.example.SummerBuild.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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

  @Bean
  @Primary
  public UserService userService() {
    return new UserService(null, null) {

      @Override
      public ResponseEntity<String> getAllUsers() {
        // Mocking data for this method
        String mockResponse =
            """
            [
                {
                    "id": "12345678-1234-1234-1234-123456789012",
                    "email": "user1@example.com",
                    "created_at": "2025-01-01T00:00:00.000000Z",
                    "email_confirmed_at": "2025-01-01T00:00:00.000000Z",
                    "user_metadata": {
                        "display_name": "Test User 1"
                    }
                },
                {
                    "id": "87654321-4321-4321-4321-210987654321",
                    "email": "user2@example.com",
                    "created_at": "2025-01-01T00:00:00.000000Z",
                    "email_confirmed_at": "2025-01-01T00:00:00.000000Z",
                    "user_metadata": {
                        "display_name": "Test User 2"
                    }
                }
            ]
            """;

        return ResponseEntity.ok(mockResponse);
      }

      @Override
      public ResponseEntity<String> getUserById(UUID userId) {
        String mockResponse =
            """
            {
                "id": "%s",
                "email": "user@example.com",
                "created_at": "2025-01-01T00:00:00.000000Z",
                "email_confirmed_at": "2025-01-01T00:00:00.000000Z",
                "user_metadata": {
                    "display_name": "Test User"
                }
            }
            """
                .formatted(userId.toString());

        return ResponseEntity.ok(mockResponse);
      }

      @Override
      public ResponseEntity<String> deleteUserById(UUID userId) {
        return ResponseEntity.ok("{\"message\": \"User deleted successfully\"}");
      }

      @Override
      public ResponseEntity<String> updateUserById(UUID userId, Map<String, Object> updates) {
        String mockResponse =
            """
            {
                "id": "%s",
                "email": "updated@example.com",
                "created_at": "2025-01-01T00:00:00.000000Z",
                "email_confirmed_at": "2025-01-01T00:00:00.000000Z",
                "user_metadata": {
                    "display_name": "Updated User"
                }
            }
            """
                .formatted(userId.toString());

        return ResponseEntity.ok(mockResponse);
      }
    };
  }
}
