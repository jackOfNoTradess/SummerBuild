package com.example.SummerBuild.repository;

import com.example.SummerBuild.model.Events;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface EventsRepository extends GenericRepository<Events, UUID> {

  // Future custom query ideas (implementations can be added when needed):

  // Find events by host UUID
  // List<Events> findByHostUuid(UUID hostUuid);

  // Find events within a date range
  // List<Events> findByStartTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

  // Find currently active events (started but not ended)
  // List<Events> findActiveEvents(LocalDateTime currentTime);

  // Find upcoming events (not started yet)
  // List<Events> findUpcomingEvents(LocalDateTime currentTime);

  // Search events by title (case-insensitive)
  // List<Events> findByTitleContainingIgnoreCase(String title);

  // Find events with available capacity
  // List<Events> findEventsWithAvailableCapacity();

  // Count events by host
  // long countByHostUuid(UUID hostUuid);

  // Find events by tags
  // List<Events> findByTagsIn(List<String> tags);
}
