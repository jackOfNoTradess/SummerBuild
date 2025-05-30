package com.example.SummerBuild.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.model.UserRole;
import com.example.SummerBuild.service.UserService;
import com.example.SummerBuild.service.UserService.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  private MockMvc mockMvc;

  @Mock private UserService userService;

  @InjectMocks private UserController userController;

  private UUID testUserId;
  private String sampleJson;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(userController)
            .setMessageConverters(
                new org.springframework.http.converter.StringHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter())
            .build();

    testUserId = UUID.randomUUID();
    sampleJson = "{\"message\":\"ok\"}";
  }

  @Test
  @DisplayName("GET /api/users - happy flow")
  void whenGetAllUsers_happyFlow_returns200() throws Exception {
    given(userService.getAllUsers()).willReturn(ResponseEntity.ok(sampleJson));

    mockMvc
        .perform(get("/api/users"))
        .andExpect(status().isOk())
        .andExpect(content().string(sampleJson));
    verify(userService).getAllUsers();
  }

  @Test
  @DisplayName("GET /api/users - sad flow (service error)")
  void whenGetAllUsers_sadFlow_returns500() throws Exception {
    given(userService.getAllUsers())
        .willReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Service down"));

    mockMvc
        .perform(get("/api/users"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string("Service down"));
    verify(userService).getAllUsers();
  }

  @Test
  @DisplayName("GET /api/users/{id} - happy flow")
  void whenGetUserById_happyFlow_returns200() throws Exception {
    given(userService.getUserById(testUserId)).willReturn(ResponseEntity.ok(sampleJson));

    mockMvc
        .perform(get("/api/users/{id}", testUserId))
        .andExpect(status().isOk())
        .andExpect(content().string(sampleJson));
    verify(userService).getUserById(testUserId);
  }

  @Test
  @DisplayName("GET /api/users/{id} - sad flow (not found)")
  void whenGetUserById_sadFlow_returns404() throws Exception {
    given(userService.getUserById(testUserId))
        .willReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found"));

    mockMvc
        .perform(get("/api/users/{id}", testUserId))
        .andExpect(status().isNotFound())
        .andExpect(content().string("Not found"));
    verify(userService).getUserById(testUserId);
  }

  @Test
  @DisplayName("DELETE /api/users/{id} - happy flow")
  void whenDeleteUser_happyFlow_returns204() throws Exception {
    given(userService.deleteUserById(testUserId)).willReturn(ResponseEntity.noContent().build());

    mockMvc.perform(delete("/api/users/{id}", testUserId)).andExpect(status().isNoContent());
    verify(userService).deleteUserById(testUserId);
  }

  @Test
  @DisplayName("DELETE /api/users/{id} - sad flow (not found)")
  void whenDeleteUser_sadFlow_returns404() throws Exception {
    given(userService.deleteUserById(testUserId))
        .willReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found"));

    mockMvc
        .perform(delete("/api/users/{id}", testUserId))
        .andExpect(status().isNotFound())
        .andExpect(content().string("Not found"));
    verify(userService).deleteUserById(testUserId);
  }

  @Test
  @DisplayName("PUT /api/users/{id} - happy flow")
  void whenUpdateUser_happyFlow_returns200() throws Exception {
    given(userService.updateUserById(eq(testUserId), any(Map.class)))
        .willReturn(ResponseEntity.ok(sampleJson));

    mockMvc
        .perform(
            put("/api/users/{id}", testUserId).param("password", "pwd").param("newName", "Name"))
        .andExpect(status().isOk())
        .andExpect(content().string(sampleJson));
    verify(userService)
        .updateUserById(
            eq(testUserId),
            argThat(map -> map.containsKey("password") && map.containsKey("user_metadata")));
  }

  @Test
  @DisplayName("PUT /api/users/{id} - sad flow (conflict)")
  void whenUpdateUser_sadFlow_returns409() throws Exception {
    given(userService.updateUserById(eq(testUserId), any(Map.class)))
        .willReturn(ResponseEntity.status(HttpStatus.CONFLICT).body("Conflict"));

    mockMvc
        .perform(put("/api/users/{id}", testUserId).param("password", "pwd"))
        .andExpect(status().isConflict())
        .andExpect(content().string("Conflict"));
    verify(userService).updateUserById(eq(testUserId), any(Map.class));
  }

  @Test
  @DisplayName("GET /api/users/role/{role} - happy flow")
  void whenGetUsersByRole_happyFlow_returns200() throws Exception {
    UserDto dto = new UserDto();
    dto.setId(UUID.randomUUID());
    dto.setRole(UserRole.ADMIN);
    given(userService.findByRole(UserRole.ADMIN)).willReturn(List.of(dto));

    mockMvc
        .perform(get("/api/users/role/{role}", UserRole.ADMIN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].role").value("ADMIN"));
    verify(userService).findByRole(UserRole.ADMIN);
  }

  @Test
  @DisplayName("GET /api/users/role/{role} - sad flow (not found)")
  void whenGetUsersByRole_sadFlow_returns404() throws Exception {
    given(userService.findByRole(UserRole.ADMIN))
        .willThrow(new ResourceNotFoundException("Role not found"));

    mockMvc.perform(get("/api/users/role/{role}", UserRole.ADMIN)).andExpect(status().isNotFound());
    verify(userService).findByRole(UserRole.ADMIN);
  }
}
