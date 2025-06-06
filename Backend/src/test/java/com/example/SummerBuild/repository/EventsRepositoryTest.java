package com.example.SummerBuild.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.SummerBuild.model.Events;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class EventsRepositoryTest {

  @Autowired private EventsRepository eventsRepository;

  private Events event1, event2, event3;
  private UUID hostId1, hostId2;

  @BeforeEach
  void setUp() {
    hostId1 = UUID.randomUUID();
    hostId2 = UUID.randomUUID();

    event1 =
        Events.builder()
            .id(UUID.randomUUID())
            .title("Event 1")
            .host_uuid(hostId1)
            .capacity(100)
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
            .description("First test event")
            .tags(Arrays.asList("test", "event1"))
            .build();

    event2 =
        Events.builder()
            .id(UUID.randomUUID())
            .title("Event 2")
            .host_uuid(hostId1)
            .capacity(50)
            .startTime(LocalDateTime.now().plusDays(2))
            .endTime(LocalDateTime.now().plusDays(2).plusHours(3))
            .description("Second test event")
            .tags(Arrays.asList("test", "event2"))
            .build();

    event3 =
        Events.builder()
            .id(UUID.randomUUID())
            .title("Event 3")
            .host_uuid(hostId2)
            .capacity(200)
            .startTime(LocalDateTime.now().plusDays(3))
            .endTime(LocalDateTime.now().plusDays(3).plusHours(4))
            .description("Third test event")
            .tags(Arrays.asList("test", "event3"))
            .build();

    eventsRepository.saveAll(Arrays.asList(event1, event2, event3));
  }

  @Test
  @DisplayName("save - persists event successfully")
  void testSave() {
    Events newEvent =
        Events.builder()
            .id(UUID.randomUUID())
            .title("New Event")
            .host_uuid(UUID.randomUUID())
            .capacity(75)
            .startTime(LocalDateTime.now().plusDays(5))
            .endTime(LocalDateTime.now().plusDays(5).plusHours(2))
            .description("New test event")
            .tags(Arrays.asList("new", "test"))
            .build();

    Events savedEvent = eventsRepository.save(newEvent);

    assertThat(savedEvent).isNotNull();
    assertThat(savedEvent.getId()).isEqualTo(newEvent.getId());
    assertThat(savedEvent.getTitle()).isEqualTo("New Event");
    assertThat(savedEvent.getCreatedAt()).isNotNull();
    assertThat(savedEvent.getUpdatedAt()).isNotNull();
  }

  @Test
  @DisplayName("findById - returns event when found")
  void testFindById_found() {
    Optional<Events> foundEvent = eventsRepository.findById(event1.getId());

    assertThat(foundEvent).isPresent();
    assertThat(foundEvent.get().getTitle()).isEqualTo("Event 1");
    assertThat(foundEvent.get().getHost_uuid()).isEqualTo(hostId1);
  }

  @Test
  @DisplayName("findById - returns empty when not found")
  void testFindById_notFound() {
    UUID nonExistentId = UUID.randomUUID();

    Optional<Events> foundEvent = eventsRepository.findById(nonExistentId);

    assertThat(foundEvent).isEmpty();
  }

  @Test
  @DisplayName("findAll - returns all events")
  void testFindAll() {
    List<Events> allEvents = eventsRepository.findAll();

    assertThat(allEvents).hasSize(3);
    assertThat(allEvents)
        .extracting("title")
        .containsExactlyInAnyOrder("Event 1", "Event 2", "Event 3");
  }

  @Test
  @DisplayName("existsById - returns true when event exists")
  void testExistsById_exists() {
    boolean exists = eventsRepository.existsById(event1.getId());

    assertThat(exists).isTrue();
  }

  @Test
  @DisplayName("existsById - returns false when event does not exist")
  void testExistsById_notExists() {
    UUID nonExistentId = UUID.randomUUID();

    boolean exists = eventsRepository.existsById(nonExistentId);

    assertThat(exists).isFalse();
  }

  @Test
  @DisplayName("deleteById - removes event")
  void testDeleteById() {
    UUID eventIdToDelete = event1.getId();

    eventsRepository.deleteById(eventIdToDelete);

    Optional<Events> deletedEvent = eventsRepository.findById(eventIdToDelete);
    assertThat(deletedEvent).isEmpty();

    List<Events> remainingEvents = eventsRepository.findAll();
    assertThat(remainingEvents).hasSize(2);
    assertThat(remainingEvents).extracting("title").containsExactlyInAnyOrder("Event 2", "Event 3");
  }

  @Test
  @DisplayName("count - returns correct count")
  void testCount() {
    long count = eventsRepository.count();

    assertThat(count).isEqualTo(3);
  }

  @Test
  @DisplayName("update - modifies existing event")
  void testUpdate() {
    Events eventToUpdate = eventsRepository.findById(event1.getId()).orElseThrow();
    LocalDateTime originalCreatedAt = eventToUpdate.getCreatedAt();
    LocalDateTime originalUpdatedAt = eventToUpdate.getUpdatedAt();

    // Add a small delay to ensure timestamp difference
    try {
      Thread.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    eventToUpdate.setTitle("Updated Event 1");
    eventToUpdate.setCapacity(150);

    Events updatedEvent = eventsRepository.saveAndFlush(eventToUpdate);

    assertThat(updatedEvent.getTitle()).isEqualTo("Updated Event 1");
    assertThat(updatedEvent.getCapacity()).isEqualTo(150);
    assertThat(updatedEvent.getCreatedAt()).isEqualTo(originalCreatedAt);
    assertThat(updatedEvent.getUpdatedAt()).isAfter(originalUpdatedAt);
  }

  @Test
  @DisplayName("save - handles events with tags")
  void testSaveWithTags() {
    Events eventWithTags =
        Events.builder()
            .id(UUID.randomUUID())
            .title("Tagged Event")
            .host_uuid(UUID.randomUUID())
            .capacity(100)
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
            .tags(Arrays.asList("music", "outdoor", "festival"))
            .build();

    Events savedEvent = eventsRepository.save(eventWithTags);

    assertThat(savedEvent.getTags()).hasSize(3);
    assertThat(savedEvent.getTags()).containsExactly("music", "outdoor", "festival");
  }

  @Test
  @DisplayName("save - handles events with null optional fields")
  void testSaveWithNullFields() {
    Events minimalEvent =
        Events.builder()
            .id(UUID.randomUUID())
            .title("Minimal Event")
            .host_uuid(UUID.randomUUID())
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
            // capacity, description, tags, picPath are null
            .build();

    Events savedEvent = eventsRepository.save(minimalEvent);

    assertThat(savedEvent).isNotNull();
    assertThat(savedEvent.getTitle()).isEqualTo("Minimal Event");
    assertThat(savedEvent.getCapacity()).isNull();
    assertThat(savedEvent.getDescription()).isNull();
    assertThat(savedEvent.getTags()).isNull();
  }

  @Test
  @DisplayName("BaseEntity timestamps - are set automatically")
  void testBaseEntityTimestamps() {
    Events newEvent =
        Events.builder()
            .id(UUID.randomUUID())
            .title("Timestamp Test Event")
            .host_uuid(UUID.randomUUID())
            .startTime(LocalDateTime.now().plusDays(1))
            .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
            .build();

    Events savedEvent = eventsRepository.save(newEvent);

    assertThat(savedEvent.getCreatedAt()).isNotNull();
    assertThat(savedEvent.getUpdatedAt()).isNotNull();
    assertThat(savedEvent.getCreatedAt()).isEqualTo(savedEvent.getUpdatedAt());
  }
}
