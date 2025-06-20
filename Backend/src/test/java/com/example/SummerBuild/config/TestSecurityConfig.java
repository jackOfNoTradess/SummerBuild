package com.example.SummerBuild.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.SummerBuild.security.JwtAuthFilter;
import com.example.SummerBuild.security.JwtTokenValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@TestConfiguration
@EnableWebSecurity
@Profile("test")
public class TestSecurityConfig {

  @Value("${supabase.jwt.secret}")
  private String jwtSecret;

  @Bean
  @Primary
  public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/auth/**")
                    .permitAll()
                    .requestMatchers("/api/users/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  @Primary
  public JwtTokenValidator jwtTokenValidator() {
    JwtTokenValidator validator = mock(JwtTokenValidator.class);

    when(validator.validateToken(anyString()))
        .thenAnswer(
            invocation -> {
              String token = invocation.getArgument(0);
              if (token == null || token.isEmpty()) {
                return null;
              }

              try {
                byte[] decoded = Base64.getDecoder().decode(jwtSecret);
                Key key = Keys.hmacShaKeyFor(decoded);

                Claims claims =
                    Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

                // Check if token is expired
                if (claims.getExpiration().before(new Date())) {
                  throw new ExpiredJwtException(null, claims, "Token has expired");
                }

                // For test environment, always return valid claims
                Map<String, Object> testClaims = new HashMap<>();
                testClaims.put("sub", claims.get("sub", String.class));
                testClaims.put("email", claims.get("email", String.class));
                testClaims.put("role", claims.get("role", String.class));

                return Jwts.claims(testClaims);
              } catch (ExpiredJwtException e) {
                throw e;
              } catch (Exception e) {
                // In test environment, if token format is valid, return test claims
                if (token.startsWith("eyJ")) {
                  Map<String, Object> testClaims = new HashMap<>();
                  testClaims.put("sub", "test-user-id");
                  testClaims.put("email", "test@example.com");
                  testClaims.put("role", "USER");
                  return Jwts.claims(testClaims);
                }
                return null;
              }
            });

    return validator;
  }

  @Bean
  @Primary
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
      throws Exception {
    return authConfig.getAuthenticationManager();
  }
}
