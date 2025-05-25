package com.example.SummerBuild.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto extends BaseDto {
  private String username;
  private String email;
  // Note: password is not included in DTO for security
  // Add more user-specific fields here
}
