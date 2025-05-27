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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private final UserService userService;

  // GET /api/users
  @GetMapping
  public ResponseEntity<String> getAllUsers() {
    return userService.getAllUsers();
  }

  // GET /api/users/{id}
  @GetMapping("/{id}")
  public ResponseEntity<String> getUserById(@PathVariable UUID id) {
    return userService.getUserById(id);
  }

  // DELETE /api/users/{id}
  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteUser(@PathVariable UUID id) {
    return userService.deleteUserById(id);
  }

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

  @GetMapping("/role/{role}")
  public ResponseEntity<List<UserDto>> getUsersByRole(@PathVariable UserRole role) {
    return ResponseEntity.ok(userService.findByRole(role));
  }
}
