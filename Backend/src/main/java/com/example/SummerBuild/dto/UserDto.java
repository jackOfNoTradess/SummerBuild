package com.example.SummerBuild.dto;

import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
// Ignores server-managed fields during JSON processing
// Example: Client sends {"id": "123", "name": "John"} -> id is ignored, only name is used
@JsonIgnoreProperties(
    value = {"id", "createdAt", "updatedAt"},
    allowSetters = false)
public class UserDto extends BaseDto {
  private UUID id;

  @NotBlank(message = "Name is required")
  private String name;

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;

  @NotNull(message = "Role is required")
  private UserRole role;

  @NotNull(message = "Gender is required")
  private Gender gender;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
