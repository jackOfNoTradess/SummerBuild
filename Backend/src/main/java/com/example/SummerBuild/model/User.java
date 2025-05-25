package com.example.SummerBuild.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseEntity {
  private String username;
  private String email;
  private String password;
  // Add more user-specific fields here
}
