package com.example.SummerBuild.config;

import com.example.SummerBuild.security.JwtAuthFilter;
import com.example.SummerBuild.security.JwtTokenValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@TestConfiguration
@EnableWebSecurity
@Profile("test")
public class TestSecurityConfig {

  // Mock the real JWT components to prevent them from interfering
  @Bean
  @Primary
  public JwtTokenValidator mockJwtTokenValidator() {
    return Mockito.mock(JwtTokenValidator.class);
  }

  @Bean
  @Primary
  public JwtAuthFilter mockJwtAuthFilter() {
    return Mockito.mock(JwtAuthFilter.class);
  }

  @Bean
  @Primary
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/auth/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated() // Keep authentication requirement
            )
        .addFilterBefore(testJwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  @Primary
  public OncePerRequestFilter testJwtAuthFilter() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String requestURI = request.getRequestURI();

        System.out.println("=== TEST JWT FILTER DEBUG ===");
        System.out.println("Request URI: " + requestURI);
        System.out.println("Auth Header: " + authHeader);

        // Validate bearer token is from our test auth service
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
          String token = authHeader.substring(7);
          System.out.println("Token: " + token.substring(0, Math.min(20, token.length())) + "...");
          System.out.println("Token starts with eyJ: " + token.startsWith("eyJ"));

          // Validation: Check if it's a JWT format (starts with eyJ)
          if (token.startsWith("eyJ")) {
            // Create authenticated user for tests
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    "test-user",
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            System.out.println("✓ Authentication set successfully");
          } else {
            System.out.println("✗ Token doesn't start with eyJ");
          }
        } else {
          System.out.println("✗ No valid Authorization header");
        }

        System.out.println(
            "Current authentication: " + SecurityContextHolder.getContext().getAuthentication());
        System.out.println("==============================");

        filterChain.doFilter(request, response);
      }
    };
  }
}
