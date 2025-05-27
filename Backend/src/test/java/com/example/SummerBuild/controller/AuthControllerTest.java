package com.example.SummerBuild.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.UserRole;
import com.example.SummerBuild.service.SupabaseAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SupabaseAuthService authService;

  @MockitoBean private com.example.SummerBuild.security.JwtTokenValidator jwtTokenValidator;

  @Test
  @DisplayName("POST /api/auth/signup successful path")
  void whenSignupSucceeds_thenReturns200() throws Exception {
    // Arrange
    Mockito.when(
            authService.signup(
                eq("alice@example.com"),
                eq("superSecret"),
                eq("Alice"),
                eq(UserRole.USER),
                eq(Gender.FEMALE)))
        .thenReturn(ResponseEntity.ok("OK"));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/auth/signup")
                .param("email", "alice@example.com")
                .param("password", "superSecret")
                .param("displayName", "Alice")
                .param("userRole", "USER")
                .param("gender", "FEMALE")
                .with(
                    request -> {
                      request.setRemoteAddr("127.0.0.1");
                      return request;
                    })
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().isOk())
        .andExpect(content().string("OK"));
  }

  @Test
  @DisplayName("POST /api/auth/signup unsuccesful path (service throws)")
  void whenSignupFails_thenReturns500() throws Exception {
    // Arrange
    Mockito.when(authService.signup(anyString(), anyString(), anyString(), any(), any()))
        .thenThrow(new RuntimeException("Something went wrong"));

    // Act & Assert
    mockMvc
        .perform(
            post("/api/auth/signup")
                .param("email", "bob@example.com")
                .param("password", "1234")
                .param("displayName", "Bob")
                .param("userRole", "ADMIN")
                .param("gender", "MALE")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string("Signup failed"));
  }

  @Test
  @DisplayName("POST /api/auth/login Succhessful path")
  void whenLoginSucceeds_thenReturns200() throws Exception {
    Mockito.when(authService.login(eq("test@gmail.com"), eq("testest")))
        .thenReturn(ResponseEntity.ok("OK"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .param("email", "test@gmail.com")
                .param("password", "testest")
                .with(
                    request -> {
                      request.setRemoteAddr("127.0.0.1");
                      return request;
                    })
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().isOk())
        .andExpect(content().string("OK"));
  }

  @Test
  @DisplayName("POST /api/auth/login failed path")
  void whenLoginFails_thenReturns500() throws Exception {
    Mockito.when(authService.login(anyString(), anyString()))
        .thenThrow(new RuntimeException("Invalid credentials"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .param("email", "bob@example.com")
                .param("password", "wrongPassword")
                .with(
                    request -> {
                      request.setRemoteAddr("192.168.0.1");
                      return request;
                    })
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(status().isUnauthorized())
        .andExpect(content().string("Login failed"));
  }
}
