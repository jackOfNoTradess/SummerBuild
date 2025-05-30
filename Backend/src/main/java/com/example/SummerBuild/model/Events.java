package com.example.SummerBuild.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Events extends BaseEntity {
  @Column(name = "title", nullable = false, length = 255)
  private String title;

  @ManyToOne(fetch = FetchType.LAZY)
  @Column(name = "host_id", nullable = false)
  private UUID host_uuid;

  @Column(name = "capacity")
  private Integer capacity;

  @Column(name = "start_time", nullable = false)
  private LocalDateTime startTime;

  @Column(name = "end_time", nullable = false)
  private LocalDateTime endTime;

  @Column(name = "description", length = 255)
  private String description;

  @Column(name = "tag", columnDefinition = "TEXT[]")
  private List<String> tags;

  // Using List instead of Set for simplicity with Hibernate

  @Column(name = "pic_path", length = 255)
  private String picPath;
}
