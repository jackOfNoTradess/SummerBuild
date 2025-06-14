package com.example.SummerBuild.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipatesDto extends BaseDto {

  @JsonProperty(access = JsonProperty.Access.READ_ONLY)
  private UUID id;

  private UUID userId;
  private UUID eventId;

  // // Optional nested DTOs for related entities
  // private UserDto user;
  // private EventsDto event;
}
