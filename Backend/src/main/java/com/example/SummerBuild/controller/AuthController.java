package com.example.SummerBuild.controller;

import com.example.SummerBuild.service.SupabaseAuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
  public ResponseEntity<String> signup(@RequestParam String email, @RequestParam String password) {
    return authService.signup(email, password);
  }
}
