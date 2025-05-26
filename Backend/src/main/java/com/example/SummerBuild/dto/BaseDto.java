package com.example.SummerBuild.dto;

// lombok automatically generates getters and setters for all fields
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseDto {

  private UUID id;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  // You can add more common fields here
}
