package com.example.SummerBuild.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.UserRole;
import com.example.SummerBuild.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = UserController.class,
    excludeAutoConfiguration = {
      SecurityAutoConfiguration.class,
      SecurityFilterAutoConfiguration.class,
      UserDetailsServiceAutoConfiguration.class,
      OAuth2ClientAutoConfiguration.class,
      OAuth2ResourceServerAutoConfiguration.class
    })
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "net.bytebuddy.experimental=true",
      "spring.main.allow-bean-definition-overriding=true"
    })
class UserControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private UserService userService;
  @Autowired private ObjectMapper objectMapper;

  private UserDto userDto;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    userDto = new UserDto();
    userDto.setId(userId);
    userDto.setName("John Doe");
    userDto.setEmail("john@example.com");
    userDto.setRole(UserRole.USER);
    userDto.setGender(Gender.MALE);
    userDto.setCreatedAt(LocalDateTime.now());
    userDto.setUpdatedAt(LocalDateTime.now());
  }

  @Test
  void getAll() throws Exception {
    when(userService.findAll()).thenReturn(Arrays.asList(userDto));

    mockMvc
        .perform(get("/api/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("John Doe"));
  }

  @Test
  void getById() throws Exception {
    when(userService.findById(userId)).thenReturn(userDto);

    mockMvc
        .perform(get("/api/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("John Doe"));
  }

  @Test
  void getByEmail() throws Exception {
    when(userService.findByEmail("john@example.com")).thenReturn(userDto);

    mockMvc
        .perform(get("/api/users/email/{email}", "john@example.com"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("John Doe"));
  }

  @Test
  void create() throws Exception {
    when(userService.create(any(UserDto.class))).thenReturn(userDto);

    mockMvc
        .perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("John Doe"));
  }

  @Test
  void update() throws Exception {
    when(userService.update(eq(userId), any(UserDto.class))).thenReturn(userDto);

    mockMvc
        .perform(
            put("/api/users/{id}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userDto)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("John Doe"));
  }

  @Test
  void deleteUser() throws Exception {
    doNothing().when(userService).delete(userId);

    mockMvc.perform(delete("/api/users/{id}", userId)).andExpect(status().isNoContent());
  }

  @Test
  void search() throws Exception {
    when(userService.findByNameContaining("John")).thenReturn(Arrays.asList(userDto));

    mockMvc
        .perform(get("/api/users/search").param("name", "John"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("John Doe"));
  }

  @Test
  void getByRole() throws Exception {
    when(userService.findByRole(UserRole.USER)).thenReturn(Arrays.asList(userDto));

    mockMvc
        .perform(get("/api/users/role/{role}", UserRole.USER))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("John Doe"));
  }

  @Test
  void countByRole() throws Exception {
    when(userService.countByRole(UserRole.USER)).thenReturn(1L);

    mockMvc
        .perform(get("/api/users/count").param("role", UserRole.USER.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").value(1));
  }
}
