package com.example.SummerBuild.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EventsDto extends BaseDto {

  // Server-managed field, ignored when client sends data but included in responses
  private UUID id;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public UUID getId() {
    return this.id;
  }

  @NotBlank(message = "Title is required")
  @Size(max = 255, message = "Title must not exceed 255 characters")
  private String title;

  private UUID hostUuid;

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  public UUID getHostUuid() {
    return this.hostUuid;
  }

  @Positive(message = "Capacity must be a positive number")
  private Integer capacity;

  @NotNull(message = "Start time is required")
  private LocalDateTime startTime;

  @NotNull(message = "End time is required")
  private LocalDateTime endTime;

  @Size(max = 255, message = "Description must not exceed 255 characters")
  private String description;

  private List<String> tags;

  @Size(max = 255, message = "Picture path must not exceed 255 characters")
  private String picPath;
}
