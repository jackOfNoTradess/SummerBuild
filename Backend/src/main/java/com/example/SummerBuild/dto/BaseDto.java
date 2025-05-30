package com.example.SummerBuild.dto;

// lombok automatically generates getters and setters for all fields
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseDto {

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}
