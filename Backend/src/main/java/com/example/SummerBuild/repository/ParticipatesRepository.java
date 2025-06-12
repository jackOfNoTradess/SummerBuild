package com.example.SummerBuild.repository;

import com.example.SummerBuild.model.Participates;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipatesRepository extends JpaRepository<Participates, UUID> {

  /** Find all participations for a specific user */
  List<Participates> findByUserId(UUID userId);

  /** Find all participations for a specific event */
  List<Participates> findByEventId(UUID eventId);

  /** Find a specific participation by user and event */
  Optional<Participates> findByUserIdAndEventId(UUID userId, UUID eventId);

  /** Check if a user is already participating in an event */
  boolean existsByUserIdAndEventId(UUID userId, UUID eventId);

  /** Count the number of participants for an event */
  @Query("SELECT COUNT(p) FROM Participates p WHERE p.eventId = :eventId")
  long countParticipantsByEventId(@Param("eventId") UUID eventId);

  /** Count the number of events a user is participating in */
  @Query("SELECT COUNT(p) FROM Participates p WHERE p.userId = :userId")
  long countEventsByUserId(@Param("userId") UUID userId);
}
