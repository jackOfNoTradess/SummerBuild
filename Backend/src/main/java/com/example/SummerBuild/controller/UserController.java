package com.example.SummerBuild.controller;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.model.UserRole;
import com.example.SummerBuild.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  // GET /api/users
  @GetMapping
  public ResponseEntity<List<UserDto>> getAllUsers() {
    return ResponseEntity.ok(userService.findAll());
  }

  // GET /api/users/{id}
  @GetMapping("/{id}")
  public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
    return ResponseEntity.ok(userService.findById(id));
  }

  // GET /api/users/email/{email}
  // Example: /api/users/email/john.doe@example.com
  @GetMapping("/email/{email}")
  public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
    return ResponseEntity.ok(userService.findByEmail(email));
  }

  // POST /api/users
  // Body: UserDto JSON
  @PostMapping
  public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
    UserDto createdUser = userService.create(userDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
  }

  // PUT /api/users/{id}
  // Body: UserDto JSON
  @PutMapping("/{id}")
  public ResponseEntity<UserDto> updateUser(
      @PathVariable UUID id, @Valid @RequestBody UserDto userDto) {
    return ResponseEntity.ok(userService.update(id, userDto));
  }

  // DELETE /api/users/{id}
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
    userService.delete(id);
    return ResponseEntity.noContent().build();
  }

  // GET /api/users/search?name=xxx
  // Example: /api/users/search?name=john
  @GetMapping("/search")
  public ResponseEntity<List<UserDto>> searchUsersByName(@RequestParam String name) {
    return ResponseEntity.ok(userService.findByNameContaining(name));
  }

  // Finding users by role
  // GET /api/users/role/{role}
  // Example: /api/users/role/ADMIN
  @GetMapping("/role/{role}")
  public ResponseEntity<List<UserDto>> getUsersByRole(@PathVariable UserRole role) {
    return ResponseEntity.ok(userService.findByRole(role));
  }

  // Count the number of users by role
  // GET /api/users/count?role=xxx
  // Example: /api/users/count?role=USER
  @GetMapping("/count")
  public ResponseEntity<Long> countUsersByRole(@RequestParam UserRole role) {
    return ResponseEntity.ok(userService.countByRole(role));
  }
}
