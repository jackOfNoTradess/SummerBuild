package com.example.SummerBuild.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", nullable = false, columnDefinition = "UUID")
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @Column(name = "gender", nullable = false)
  private Gender gender;

  @OneToMany(mappedBy = "host", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Events> hostedEvents;
}
