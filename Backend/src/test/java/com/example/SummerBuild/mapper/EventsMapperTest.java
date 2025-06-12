package com.example.SummerBuild.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.example.SummerBuild.dto.EventsDto;
import com.example.SummerBuild.model.Events;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventsMapperTest {

  private EventsMapper eventsMapper;

  @BeforeEach
  void setUp() {
    eventsMapper = new EventsMapper();
  }

  @Test
  @DisplayName("toDto - converts entity to dto")
  void testToDto() {
    UUID eventId = UUID.randomUUID();
    UUID hostId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startTime = now.plusDays(1);
    LocalDateTime endTime = now.plusDays(1).plusHours(2);

    Events event =
        Events.builder()
            .id(eventId)
            .title("Test Event")
            .hostId(hostId)
            .capacity(100)
            .startTime(startTime)
            .endTime(endTime)
            .description("Test description")
            .tags(new String[] {"test", "event"})
            .picPath("/images/test.jpg")
            .build();
    event.setCreatedAt(now);
    event.setUpdatedAt(now);

    EventsDto dto = eventsMapper.toDto(event);

    assertNotNull(dto);
    assertEquals(event.getId(), dto.getId());
    assertEquals(event.getTitle(), dto.getTitle());
    assertEquals(event.getHostId(), dto.getHostUuid());
    assertEquals(event.getCapacity(), dto.getCapacity());
    assertEquals(event.getStartTime(), dto.getStartTime());
    assertEquals(event.getEndTime(), dto.getEndTime());
    assertEquals(event.getDescription(), dto.getDescription());
    assertArrayEquals(event.getTags(), dto.getTags().toArray(new String[0]));
    assertEquals(event.getPicPath(), dto.getPicPath());
    assertEquals(event.getCreatedAt(), dto.getCreatedAt());
    assertEquals(event.getUpdatedAt(), dto.getUpdatedAt());
  }

  @Test
  @DisplayName("toDto - handles null entity")
  void testToDto_nullEntity() {
    EventsDto dto = eventsMapper.toDto(null);
    assertNull(dto);
  }

  @Test
  @DisplayName("toEntity - converts dto to entity")
  void testToEntity() {
    UUID eventId = UUID.randomUUID();
    UUID hostId = UUID.randomUUID();
    LocalDateTime startTime = LocalDateTime.now().plusDays(1);
    LocalDateTime endTime = LocalDateTime.now().plusDays(1).plusHours(2);

    EventsDto dto = new EventsDto();
    dto.setId(eventId);
    dto.setTitle("Test Event");
    dto.setHostUuid(hostId);
    dto.setCapacity(100);
    dto.setStartTime(startTime);
    dto.setEndTime(endTime);
    dto.setDescription("Test description");
    dto.setTags(Arrays.asList("test", "event"));
    dto.setPicPath("/images/test.jpg");

    Events entity = eventsMapper.toEntity(dto);

    assertNotNull(entity);
    assertEquals(dto.getId(), entity.getId());
    assertEquals(dto.getTitle(), entity.getTitle());
    assertEquals(dto.getHostUuid(), entity.getHostId());
    assertEquals(dto.getCapacity(), entity.getCapacity());
    assertEquals(dto.getStartTime(), entity.getStartTime());
    assertEquals(dto.getEndTime(), entity.getEndTime());
    assertEquals(dto.getDescription(), entity.getDescription());
    assertArrayEquals(dto.getTags().toArray(new String[0]), entity.getTags());
    assertEquals(dto.getPicPath(), entity.getPicPath());
  }

  @Test
  @DisplayName("toEntity - handles null dto")
  void testToEntity_nullDto() {
    Events entity = eventsMapper.toEntity(null);
    assertNull(entity);
  }

  @Test
  @DisplayName("toEntity - generates UUID when dto id is null")
  void testToEntity_nullId_generatesUuid() {
    EventsDto dto = new EventsDto();
    dto.setTitle("Test Event");
    dto.setHostUuid(UUID.randomUUID());

    Events entity = eventsMapper.toEntity(dto);

    assertNotNull(entity);
    assertNotNull(entity.getId());
    assertEquals(dto.getTitle(), entity.getTitle());
  }

  @Test
  @DisplayName("updateEntityFromDto - updates entity fields")
  void testUpdateEntityFromDto() {
    UUID eventId = UUID.randomUUID();
    UUID hostId = UUID.randomUUID();
    LocalDateTime originalTime = LocalDateTime.now();

    Events entity =
        Events.builder()
            .id(eventId)
            .title("Original Title")
            .hostId(hostId)
            .capacity(50)
            .startTime(originalTime)
            .endTime(originalTime.plusHours(1))
            .description("Original description")
            .tags(new String[] {"original"})
            .picPath("/images/original.jpg")
            .build();

    EventsDto dto = new EventsDto();
    dto.setTitle("Updated Title");
    dto.setCapacity(100);
    dto.setStartTime(originalTime.plusDays(1));
    dto.setEndTime(originalTime.plusDays(1).plusHours(2));
    dto.setDescription("Updated description");
    dto.setTags(Arrays.asList("updated", "test"));
    dto.setPicPath("/images/updated.jpg");

    eventsMapper.updateEntityFromDto(dto, entity);

    assertEquals("Updated Title", entity.getTitle());
    assertEquals(Integer.valueOf(100), entity.getCapacity());
    assertEquals(dto.getStartTime(), entity.getStartTime());
    assertEquals(dto.getEndTime(), entity.getEndTime());
    assertEquals("Updated description", entity.getDescription());
    assertArrayEquals(new String[] {"updated", "test"}, entity.getTags());
    assertEquals("/images/updated.jpg", entity.getPicPath());
    // ID and host should remain unchanged
    assertEquals(eventId, entity.getId());
    assertEquals(hostId, entity.getHostId());
  }

  @Test
  @DisplayName("updateEntityFromDto - handles null dto")
  void testUpdateEntityFromDto_nullDto() {
    Events entity = Events.builder().id(UUID.randomUUID()).title("Original Title").build();
    String originalTitle = entity.getTitle();

    eventsMapper.updateEntityFromDto(null, entity);

    // Entity should remain unchanged
    assertEquals(originalTitle, entity.getTitle());
  }

  @Test
  @DisplayName("updateEntityFromDto - handles null entity")
  void testUpdateEntityFromDto_nullEntity() {
    EventsDto dto = new EventsDto();
    dto.setTitle("Test Title");

    // Should not throw exception
    assertDoesNotThrow(() -> eventsMapper.updateEntityFromDto(dto, null));
  }

  @Test
  @DisplayName("updateEntityFromDto - handles null fields in dto")
  void testUpdateEntityFromDto_nullFields() {
    Events entity =
        Events.builder()
            .id(UUID.randomUUID())
            .title("Original Title")
            .capacity(50)
            .description("Original description")
            .build();

    EventsDto dto = new EventsDto();
    // All fields are null

    eventsMapper.updateEntityFromDto(dto, entity);

    // Entity should remain unchanged when dto fields are null
    assertEquals("Original Title", entity.getTitle());
    assertEquals(Integer.valueOf(50), entity.getCapacity());
    assertEquals("Original description", entity.getDescription());
  }

  @Test
  @DisplayName("updateEntityFromDto - updates only non-null fields")
  void testUpdateEntityFromDto_partialUpdate() {
    Events entity =
        Events.builder()
            .id(UUID.randomUUID())
            .title("Original Title")
            .capacity(50)
            .description("Original description")
            .tags(new String[] {"original"})
            .build();

    EventsDto dto = new EventsDto();
    dto.setTitle("Updated Title");
    dto.setCapacity(100);
    // description, tags, etc. are null

    eventsMapper.updateEntityFromDto(dto, entity);

    assertEquals("Updated Title", entity.getTitle());
    assertEquals(Integer.valueOf(100), entity.getCapacity());
    // These should remain unchanged
    assertEquals("Original description", entity.getDescription());
    assertArrayEquals(new String[] {"original"}, entity.getTags());
  }

  @Test
  @DisplayName("toDto and toEntity - round trip conversion")
  void testRoundTripConversion() {
    UUID eventId = UUID.randomUUID();
    UUID hostId = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    Events originalEntity =
        Events.builder()
            .id(eventId)
            .title("Test Event")
            .hostId(hostId)
            .capacity(100)
            .startTime(now.plusDays(1))
            .endTime(now.plusDays(1).plusHours(2))
            .description("Test description")
            .tags(new String[] {"test", "event"})
            .picPath("/images/test.jpg")
            .build();
    originalEntity.setCreatedAt(now);
    originalEntity.setUpdatedAt(now);

    // Convert to DTO and back to entity
    EventsDto dto = eventsMapper.toDto(originalEntity);
    Events convertedEntity = eventsMapper.toEntity(dto);

    assertEquals(originalEntity.getId(), convertedEntity.getId());
    assertEquals(originalEntity.getTitle(), convertedEntity.getTitle());
    assertEquals(originalEntity.getHostId(), convertedEntity.getHostId());
    assertEquals(originalEntity.getCapacity(), convertedEntity.getCapacity());
    assertEquals(originalEntity.getStartTime(), convertedEntity.getStartTime());
    assertEquals(originalEntity.getEndTime(), convertedEntity.getEndTime());
    assertEquals(originalEntity.getDescription(), convertedEntity.getDescription());
    assertArrayEquals(originalEntity.getTags(), convertedEntity.getTags());
    assertEquals(originalEntity.getPicPath(), convertedEntity.getPicPath());
  }
}
