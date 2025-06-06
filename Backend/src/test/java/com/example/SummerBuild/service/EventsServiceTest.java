package com.example.SummerBuild.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.SummerBuild.dto.EventsDto;
import com.example.SummerBuild.mapper.EventsMapper;
import com.example.SummerBuild.model.Events;
import com.example.SummerBuild.repository.EventsRepository;
import com.example.SummerBuild.service.EventsService.InvalidDataException;
import com.example.SummerBuild.service.EventsService.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventsServiceTest {

  @Mock private EventsRepository eventsRepository;
  @Mock private EventsMapper eventsMapper;

  @InjectMocks private EventsService eventsService;

  private UUID testEventId;
  private UUID testHostId;
  private Events testEvent;
  private EventsDto testEventDto;

  @BeforeEach
  void setUp() {
    testEventId = UUID.randomUUID();
    testHostId = UUID.randomUUID();
    testEvent = createTestEvent();
    testEventDto = createTestEventDto();
  }

  private Events createTestEvent() {
    return Events.builder()
        .id(testEventId)
        .title("Test Event")
        .host_uuid(testHostId)
        .capacity(100)
        .startTime(LocalDateTime.now().plusDays(1))
        .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
        .description("Test event description")
        .tags(Arrays.asList("test", "event"))
        .build();
  }

  private EventsDto createTestEventDto() {
    EventsDto dto = new EventsDto();
    dto.setId(testEventId);
    dto.setTitle("Test Event");
    dto.setHostUuid(testHostId);
    dto.setCapacity(100);
    dto.setStartTime(LocalDateTime.now().plusDays(1));
    dto.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
    dto.setDescription("Test event description");
    dto.setTags(Arrays.asList("test", "event"));
    return dto;
  }

  @Test
  @DisplayName("findAll - returns mapped events")
  void findAll_returnsMappedEvents() {
    when(eventsRepository.findAll()).thenReturn(Arrays.asList(testEvent));
    when(eventsMapper.toDto(testEvent)).thenReturn(testEventDto);

    List<EventsDto> result = eventsService.findAll();

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testEventDto);
    verify(eventsRepository).findAll();
    verify(eventsMapper).toDto(testEvent);
  }

  @Test
  @DisplayName("findById - found - returns dto")
  void findById_found_returnsDto() {
    when(eventsRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
    when(eventsMapper.toDto(testEvent)).thenReturn(testEventDto);

    EventsDto result = eventsService.findById(testEventId);

    assertThat(result).isEqualTo(testEventDto);
    verify(eventsRepository).findById(testEventId);
    verify(eventsMapper).toDto(testEvent);
  }

  @Test
  @DisplayName("findById - not found - throws exception")
  void findById_notFound_throwsException() {
    when(eventsRepository.findById(testEventId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventsService.findById(testEventId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Event not found with id: " + testEventId);

    verify(eventsRepository).findById(testEventId);
    verifyNoInteractions(eventsMapper);
  }

  @Test
  @DisplayName("create - valid data - creates event")
  void create_validData_createsEvent() {
    when(eventsMapper.toEntity(testEventDto)).thenReturn(testEvent);
    when(eventsRepository.save(testEvent)).thenReturn(testEvent);
    when(eventsMapper.toDto(testEvent)).thenReturn(testEventDto);

    EventsDto result = eventsService.create(testEventDto, testHostId);

    assertThat(result).isEqualTo(testEventDto);
    verify(eventsMapper).toEntity(testEventDto);
    verify(eventsRepository).save(testEvent);
    verify(eventsMapper).toDto(testEvent);
  }

  @Test
  @DisplayName("create - invalid start/end time - throws exception")
  void create_invalidStartEndTime_throwsException() {
    EventsDto invalidDto = createTestEventDto();
    invalidDto.setStartTime(LocalDateTime.now().plusDays(2));
    invalidDto.setEndTime(LocalDateTime.now().plusDays(1)); // End before start

    assertThatThrownBy(() -> eventsService.create(invalidDto, testHostId))
        .isInstanceOf(InvalidDataException.class)
        .hasMessageContaining("Start time cannot be after end time");

    verifyNoInteractions(eventsRepository);
  }

  @Test
  @DisplayName("create - invalid capacity - throws exception")
  void create_invalidCapacity_throwsException() {
    EventsDto invalidDto = createTestEventDto();
    invalidDto.setCapacity(-10);

    assertThatThrownBy(() -> eventsService.create(invalidDto, testHostId))
        .isInstanceOf(InvalidDataException.class)
        .hasMessageContaining("Capacity must be a positive number");

    verifyNoInteractions(eventsRepository);
  }

  @Test
  @DisplayName("update - valid data - updates event")
  void update_validData_updatesEvent() {
    when(eventsRepository.findById(testEventId)).thenReturn(Optional.of(testEvent));
    when(eventsRepository.save(testEvent)).thenReturn(testEvent);
    when(eventsMapper.toDto(testEvent)).thenReturn(testEventDto);

    EventsDto result = eventsService.update(testEventId, testEventDto);

    assertThat(result).isEqualTo(testEventDto);
    verify(eventsRepository).findById(testEventId);
    verify(eventsMapper).updateEntityFromDto(testEventDto, testEvent);
    verify(eventsRepository).save(testEvent);
    verify(eventsMapper).toDto(testEvent);
  }

  @Test
  @DisplayName("update - not found - throws exception")
  void update_notFound_throwsException() {
    when(eventsRepository.findById(testEventId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventsService.update(testEventId, testEventDto))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Event not found with id: " + testEventId);

    verify(eventsRepository).findById(testEventId);
    verifyNoMoreInteractions(eventsRepository);
    verifyNoInteractions(eventsMapper);
  }

  @Test
  @DisplayName("update - invalid data - throws exception")
  void update_invalidData_throwsException() {
    EventsDto invalidDto = createTestEventDto();
    invalidDto.setCapacity(0); // Invalid capacity

    assertThatThrownBy(() -> eventsService.update(testEventId, invalidDto))
        .isInstanceOf(InvalidDataException.class)
        .hasMessageContaining("Capacity must be a positive number");

    verifyNoInteractions(eventsRepository);
  }

  @Test
  @DisplayName("delete - found - deletes event")
  void delete_found_deletesEvent() {
    when(eventsRepository.existsById(testEventId)).thenReturn(true);

    eventsService.delete(testEventId);

    verify(eventsRepository).existsById(testEventId);
    verify(eventsRepository).deleteById(testEventId);
  }

  @Test
  @DisplayName("delete - not found - throws exception")
  void delete_notFound_throwsException() {
    when(eventsRepository.existsById(testEventId)).thenReturn(false);

    assertThatThrownBy(() -> eventsService.delete(testEventId))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Event not found with id: " + testEventId);

    verify(eventsRepository).existsById(testEventId);
    verify(eventsRepository, never()).deleteById(any());
  }

  @Test
  @DisplayName("findByHostUuid - returns filtered events")
  void findByHostUuid_returnsFilteredEvents() {
    Events anotherEvent = createTestEvent();
    anotherEvent.setId(UUID.randomUUID());
    anotherEvent.setHost_uuid(UUID.randomUUID()); // Different host

    when(eventsRepository.findAll()).thenReturn(Arrays.asList(testEvent, anotherEvent));
    when(eventsMapper.toDto(testEvent)).thenReturn(testEventDto);

    List<EventsDto> result = eventsService.findByHostUuid(testHostId);

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testEventDto);
    verify(eventsRepository).findAll();
    verify(eventsMapper).toDto(testEvent);
    verify(eventsMapper, never()).toDto(anotherEvent);
  }

  @Test
  @DisplayName("findByHostUuid - no events for host - returns empty list")
  void findByHostUuid_noEventsForHost_returnsEmptyList() {
    Events anotherEvent = createTestEvent();
    anotherEvent.setHost_uuid(UUID.randomUUID()); // Different host

    when(eventsRepository.findAll()).thenReturn(Arrays.asList(anotherEvent));

    List<EventsDto> result = eventsService.findByHostUuid(testHostId);

    assertThat(result).isEmpty();
    verify(eventsRepository).findAll();
    verifyNoInteractions(eventsMapper);
  }

  @Test
  @DisplayName("validateEventData - null times - no exception")
  void validateEventData_nullTimes_noException() {
    EventsDto dto = createTestEventDto();
    dto.setStartTime(null);
    dto.setEndTime(null);

    when(eventsMapper.toEntity(dto)).thenReturn(testEvent);
    when(eventsRepository.save(any())).thenReturn(testEvent);
    when(eventsMapper.toDto(any())).thenReturn(dto);

    // Should not throw exception
    assertThatCode(() -> eventsService.create(dto, testHostId)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("validateEventData - null capacity - no exception")
  void validateEventData_nullCapacity_noException() {
    EventsDto dto = createTestEventDto();
    dto.setCapacity(null);

    when(eventsMapper.toEntity(dto)).thenReturn(testEvent);
    when(eventsRepository.save(any())).thenReturn(testEvent);
    when(eventsMapper.toDto(any())).thenReturn(dto);

    // Should not throw exception
    assertThatCode(() -> eventsService.create(dto, testHostId)).doesNotThrowAnyException();
  }
}
