package com.example.SummerBuild.controller;

import com.example.SummerBuild.model.*;
import com.example.SummerBuild.service.SupabaseAuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@SecurityRequirement(name = "bearerAuth")
public class AuthController {
  private final SupabaseAuthService authService;
  private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

  @Autowired
  public AuthController(SupabaseAuthService authService) {
    this.authService = authService;
  }

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

  private String getClientIp(HttpServletRequest request) {
    String xfHeader = request.getHeader("X-Forwarded-For");
    return xfHeader == null ? request.getRemoteAddr() : xfHeader.split(",")[0];
  }
}
