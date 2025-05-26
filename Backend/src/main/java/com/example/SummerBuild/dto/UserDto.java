package com.example.SummerBuild.dto;

import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto extends BaseDto {
  private String name;
  private String email;
  private UserRole role;
  private Gender gender;
}
