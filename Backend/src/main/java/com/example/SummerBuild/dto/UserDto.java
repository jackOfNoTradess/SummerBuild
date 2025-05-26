package com.example.SummerBuild.dto;

import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.UserRole;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserDto extends BaseDto {
  // Server-managed field, ignored when client sends data
  @JsonIgnore private UUID id;

  @NotBlank(message = "Name is required")
  private String name;

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;

  @NotNull(message = "Role is required")
  private UserRole role;

  @NotNull(message = "Gender is required")
  private Gender gender;

  // Server-managed field, ignored when client sends data
  @JsonIgnore private LocalDateTime createdAt;

  // Server-managed field, ignored when client sends data
  @JsonIgnore private LocalDateTime updatedAt;
}
