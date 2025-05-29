package com.example.SummerBuild.model;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    setCreatedAt(now);
    setUpdatedAt(now);
  }

  @PreUpdate
  protected void onUpdate() {
    setUpdatedAt(LocalDateTime.now());
  }
}
