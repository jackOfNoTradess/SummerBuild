package com.example.SummerBuild.config;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.UserRole;
import com.example.SummerBuild.service.SupabaseAuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@TestConfiguration
@Profile("test")
public class TestAuthConfig {

  @Value("${supabase.jwt.secret}")
  private String jwtSecret;

  @Bean
  @Primary
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  @Primary
  public SupabaseAuthService supabaseAuthService() {
    SupabaseAuthService mockService = mock(SupabaseAuthService.class);

    // Mock successful signup
    when(mockService.signup(
            anyString(), anyString(), anyString(), any(UserRole.class), any(Gender.class)))
        .thenAnswer(
            invocation -> {
              String email = invocation.getArgument(0);
              String uuid = UUID.nameUUIDFromBytes(email.getBytes()).toString();
              String token = generateJwtToken(uuid, email, "USER");
              String response =
                  String.format(
                      """
                      {
                          "access_token": "%s",
                          "user": {
                              "id": "%s",
                              "email": "%s",
                              "created_at": "2025-01-01T00:00:00.000000Z",
                              "email_confirmed_at": "2025-01-01T00:00:00.000000Z",
                              "user_metadata": {
                                  "display_name": "Test User"
                              }
                          }
                      }""",
                      token, uuid, email);
              return ResponseEntity.ok(response);
            });

    // Mock successful login
    when(mockService.login(anyString(), anyString()))
        .thenAnswer(
            invocation -> {
              String email = invocation.getArgument(0);
              String uuid = UUID.nameUUIDFromBytes(email.getBytes()).toString();
              String token = generateJwtToken(uuid, email, "USER");
              String response =
                  String.format(
                      """
                      {
                          "access_token": "%s",
                          "user": {
                              "id": "%s",
                              "email": "%s"
                          }
                      }""",
                      token, uuid, email);
              return ResponseEntity.ok(response);
            });

    // Mock failed login attempts
    when(mockService.login(eq("invalid@example.com"), anyString()))
        .thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized"));
    when(mockService.login(anyString(), eq("wrongpassword")))
        .thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized"));

    return mockService;
  }

  private String generateJwtToken(String userId, String email, String role) {
    try {
      byte[] decoded = Base64.getDecoder().decode(jwtSecret);
      Key key = Keys.hmacShaKeyFor(decoded);

      Map<String, Object> claims = new HashMap<>();
      claims.put("sub", userId);
      claims.put("email", email);
      claims.put("role", role);

      return Jwts.builder()
          .setClaims(claims)
          .setIssuedAt(new Date())
          .setExpiration(new Date(System.currentTimeMillis() + 3600000))
          .signWith(key)
          .compact();
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate JWT token", e);
    }
  }
}
