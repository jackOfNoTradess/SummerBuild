package com.example.SummerBuild.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class EventDTO {
  private UUID id;
  private String name;
  private OffsetDateTime date;

  // Default constructor
  public EventDTO() {}

  // Constructor with fields
  public EventDTO(UUID id, String name, OffsetDateTime date) {
    this.id = id;
    this.name = name;
    this.date = date;
  }

  // Getters and setters
  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public OffsetDateTime getDate() {
    return date;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDate(OffsetDateTime date) {
    this.date = date;
  }
}
