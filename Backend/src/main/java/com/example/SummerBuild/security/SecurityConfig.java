package com.example.SummerBuild.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@Profile("!test")
public class SecurityConfig {

  private final JwtAuthFilter jwtAuthFilter;
  private final CorsConfigurationSource corsConfigurationSource;

  public SecurityConfig(
      JwtAuthFilter jwtAuthFilter, CorsConfigurationSource corsConfigurationSource) {
    this.jwtAuthFilter = jwtAuthFilter;
    this.corsConfigurationSource = corsConfigurationSource;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable()) // disabled cause we building restapi, stateless application
        .cors(cors -> cors.configurationSource(corsConfigurationSource)) // Enable CORS
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
