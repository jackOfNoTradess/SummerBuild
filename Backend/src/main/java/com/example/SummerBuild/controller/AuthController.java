package com.example.SummerBuild.controller;

import com.example.SummerBuild.model.*;
import com.example.SummerBuild.service.SupabaseAuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling authentication-related operations.
 *
 * <p>Provides endpoints for user signup and login using Supabase authentication. All routes are
 * protected by bearer token security, except for public-facing authentication.
 */
@RestController
@RequestMapping("/api/auth")
@SecurityRequirement(name = "bearerAuth")
public class AuthController {

  private final SupabaseAuthService authService;
  private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

  /**
   * Constructs the controller with required {@link SupabaseAuthService}.
   *
   * @param authService Service used to interact with Supabase authentication APIs
   */
  @Autowired
  public AuthController(SupabaseAuthService authService) {
    this.authService = authService;
  }

  /**
   * Endpoint to sign up a new user in Supabase.
   *
   * @param email Email address of the new user
   * @param password Password for the new user
   * @param displayName Display name to be stored in user metadata
   * @param userRole Role of the user (e.g., ADMIN, USER)
   * @param gender Gender of the user
   * @param request The HTTP request (used to log the client IP)
   * @return HTTP 200 with Supabase response if successful, or HTTP 500 if signup fails
   */
  // POST /api/auth/signup
  @PostMapping("/signup")
  public ResponseEntity<String> signup(
      @RequestParam String email,
      @RequestParam String password,
      @RequestParam String displayName,
      @RequestParam UserRole userRole,
      @RequestParam Gender gender,
      HttpServletRequest request) {

    String clientIp = getClientIp(request);
    try {
      ResponseEntity<String> response =
          authService.signup(email, password, displayName, userRole, gender);
      logger.info("Signup successful for email: {}, IP: {}", email, clientIp);
      return response;
    } catch (Exception e) {
      logger.warn(
          "Signup failed for email: {}, IP: {}, Reason: {}", email, clientIp, e.getMessage());
      return ResponseEntity.status(500).body("Signup failed");
    }
  }

  /**
   * Endpoint to log in a user.
   *
   * @param email Email address of the user
   * @param password Password for the user
   * @param request The HTTP request (used to log the client IP)
   * @return HTTP 200 with Supabase session or token if successful, or HTTP 401 if login fails
   */
  // POST /api/auth/login
  @PostMapping("/login")
  public ResponseEntity<String> login(
      @RequestParam String email, @RequestParam String password, HttpServletRequest request) {

    String clientIp = getClientIp(request);
    try {
      ResponseEntity<String> response = authService.login(email, password);
      logger.info("Login successful for email: {}, IP: {}", email, clientIp);
      return response;
    } catch (Exception e) {
      logger.warn(
          "Login failed for email: {}, IP: {}, Reason: {}", email, clientIp, e.getMessage());
      return ResponseEntity.status(401).body("Login failed");
    }
  }

  /**
   * Extracts the client IP address from the request headers or remote address.
   *
   * @param request HTTP request object
   * @return Client IP address
   */
  private String getClientIp(HttpServletRequest request) {
    String xfHeader = request.getHeader("X-Forwarded-For");
    return xfHeader == null ? request.getRemoteAddr() : xfHeader.split(",")[0];
  }
}
