package com.example.SummerBuild.config;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.SummerBuild.service.SupabaseAuthService;
import com.example.SummerBuild.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.*;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;

@TestConfiguration
@Profile("test")
public class TestAuthConfig {

  @Value("${supabase.jwtSecret}")
  private String jwtSecret;

  private SupabaseAuthService supabaseAuthService;
  private UserService userService;

  @Bean
  @Primary // Add this
  public SupabaseAuthService supabaseAuthService() {
    if (this.supabaseAuthService == null) {
      this.supabaseAuthService = mock(SupabaseAuthService.class);
      setupMocks(); // Setup immediately
    }
    return this.supabaseAuthService;
  }

  @Bean
  @Primary // Add this
  public UserService userService() {
    if (this.userService == null) {
      this.userService = mock(UserService.class);
    }
    return this.userService;
  }

  public void setupMocks() {
    if (supabaseAuthService == null || userService == null) {
      return; // Not ready yet
    }

    // Mock signup
    when(supabaseAuthService.signup(anyString(), anyString(), anyString(), any(), any()))
        .thenAnswer(
            invocation -> {
              String email = invocation.getArgument(0);
              String uuid = UUID.nameUUIDFromBytes(email.getBytes()).toString();
              return ResponseEntity.ok(
                  "{\"user\":{\"id\":\"" + uuid + "\",\"email\":\"" + email + "\"}}");
            });

    // Mock login with real JWT token generation
    when(supabaseAuthService.login(anyString(), anyString()))
        .thenAnswer(
            invocation -> {
              try {
                String email = invocation.getArgument(0);
                byte[] decoded = Base64.getDecoder().decode(jwtSecret);
                Key key = Keys.hmacShaKeyFor(decoded);

                String uuid = UUID.nameUUIDFromBytes(email.getBytes()).toString();

                Map<String, Object> claims = new HashMap<>();
                claims.put("sub", uuid);
                claims.put("email", email);
                claims.put("role", "USER");

                String token =
                    Jwts.builder()
                        .setClaims(claims)
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                        .signWith(key)
                        .compact();

                return ResponseEntity.ok("{\"access_token\":\"" + token + "\"}");
              } catch (Exception e) {
                throw new RuntimeException("Failed to generate JWT token", e);
              }
            });

    // Mock getAllUsers
    when(userService.getAllUsers())
        .thenReturn(
            ResponseEntity.ok(
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
            """));

    // Mock getUserById
    when(userService.getUserById(any(UUID.class)))
        .thenAnswer(
            invocation -> {
              UUID userId = invocation.getArgument(0);
              String mockResponse =
                  String.format(
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
              """,
                      userId.toString());
              return ResponseEntity.ok(mockResponse);
            });

    // Mock deleteUserById
    when(userService.deleteUserById(any(UUID.class)))
        .thenReturn(ResponseEntity.ok("{\"message\": \"User deleted successfully\"}"));

    // Mock updateUserById
    ResponseEntity<String> updateResponse =
        ResponseEntity.ok(
            String.format(
                """
        {
            "id": "updated-id",
            "email": "updated@example.com",
            "created_at": "2025-01-01T00:00:00.000000Z",
            "email_confirmed_at": "2025-01-01T00:00:00.000000Z",
            "user_metadata": {
                "display_name": "Updated User"
            }
        }
        """));
    when(userService.updateUserById(any(UUID.class), ArgumentMatchers.<Map<String, Object>>any()))
        .thenReturn(updateResponse);
  }
}
