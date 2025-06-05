package com.example.SummerBuild.controller;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.model.UserRole;
import com.example.SummerBuild.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing users.
 *
 * <p>Provides endpoints to retrieve, update, and delete users, as well as fetch users by role. All
 * endpoints require bearer token authentication.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private final UserService userService;

  /**
   * Retrieves all users from Supabase.
   *
   * @return ResponseEntity containing the JSON response of all users
   */
  // GET /api/users
  @GetMapping
  public ResponseEntity<String> getAllUsers() {
    return userService.getAllUsers();
  }

  /**
   * Retrieves a specific user by UUID from Supabase.
   *
   * @param id UUID of the user
   * @return ResponseEntity containing the user's information or error message
   */
  // GET /api/users/{id}
  @GetMapping("/{id}")
  public ResponseEntity<String> getUserById(@PathVariable UUID id) {
    return userService.getUserById(id);
  }

  /**
   * Deletes a user by UUID from Supabase.
   *
   * @param id UUID of the user to delete
   * @return ResponseEntity indicating the result of the deletion
   */
  // DELETE /api/users/{id}
  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteUser(@PathVariable UUID id) {
    return userService.deleteUserById(id);
  }

  /**
   * Updates a user's password or display name.
   *
   * <p>Accepts optional request parameters: "password" and "newName". Only non-null fields will be
   * updated.
   *
   * @param id UUID of the user to update
   * @param password New password for the user (optional)
   * @param newName New display name for the user (optional)
   * @return ResponseEntity indicating the result of the update operation
   */
  // PUT /api/users/{id}
  @PutMapping("/{id}")
  public ResponseEntity<String> updateUser(
      @PathVariable UUID id,
      @RequestParam(required = false) String password,
      @RequestParam(required = false) String newName) {
    Map<String, Object> updates = new HashMap<>();
    if (password != null) updates.put("password", password);
    if (newName != null) {
      Map<String, Object> metadata = new HashMap<>();
      metadata.put("display_name", newName);
      updates.put("user_metadata", metadata);
    }
    return userService.updateUserById(id, updates);
  }

  /**
   * Retrieves users by their role.
   *
   * @param role The role to filter users by (e.g., ADMIN, USER)
   * @return ResponseEntity with a list of matching UserDto objects
   */
  // GET /api/users/role/{role}
  @GetMapping("/role/{role}")
  public ResponseEntity<List<UserDto>> getUsersByRole(@PathVariable UserRole role) {
    List<UserDto> users = userService.findByRole(role);

    // ========== CONTROLLER DEBUG ==========
    System.out.println("========== CONTROLLER DEBUG ==========");
    System.out.println("Service returned " + users.size() + " users");

    if (!users.isEmpty()) {
      UserDto firstUser = users.get(0);
      System.out.println("First user object: " + firstUser);
      System.out.println("First user ID: " + firstUser.getId());
      System.out.println("First user role: " + firstUser.getRole());
      System.out.println("First user gender: " + firstUser.getGender());
      System.out.println("First user createdAt: " + firstUser.getCreatedAt());
      System.out.println("First user updatedAt: " + firstUser.getUpdatedAt());

      // Test Jackson serialization manually
      try {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String json = mapper.writeValueAsString(firstUser);
        System.out.println("Manual JSON serialization: " + json);

        // Test list serialization
        String listJson = mapper.writeValueAsString(users);
        System.out.println("Manual LIST JSON serialization: " + listJson);
      } catch (Exception e) {
        System.out.println("JSON serialization error: " + e.getMessage());
        e.printStackTrace();
      }
    } else {
      System.out.println("Users list is EMPTY!");
    }

    System.out.println("About to return ResponseEntity.ok() with " + users.size() + " users");
    System.out.println("=======================================");

    return ResponseEntity.ok(users);
  }
}
