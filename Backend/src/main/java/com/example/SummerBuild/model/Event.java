package com.example.SummerBuild.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(nullable = false)
  private OffsetDateTime date;

  // Default constructor
  public Event() {}

  // Constructor with fields
  public Event(String name, OffsetDateTime date) {
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
