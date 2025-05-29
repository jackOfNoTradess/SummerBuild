package com.example.SummerBuild.dto;

import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserDto extends BaseDto {
  // Server-managed field, ignored when client sends data but included in responses
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID id;

  @NotNull(message = "Role is required")
  private UserRole role;

  @NotNull(message = "Gender is required")
  private Gender gender;

  // Server-managed field, ignored when client sends data but included in responses
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private LocalDateTime createdAt;

  // Server-managed field, ignored when client sends data but included in responses
  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private LocalDateTime updatedAt;
}
