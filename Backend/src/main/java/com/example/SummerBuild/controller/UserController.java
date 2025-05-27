package com.example.SummerBuild.controller;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.model.UserRole;
import com.example.SummerBuild.service.UserService;
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
    return ResponseEntity.ok(userService.findByRole(role));
  }
}
