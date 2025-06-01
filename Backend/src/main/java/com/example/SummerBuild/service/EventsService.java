package com.example.SummerBuild.service;

import com.example.SummerBuild.dto.EventsDto;
import com.example.SummerBuild.mapper.EventsMapper;
import com.example.SummerBuild.model.Events;
import com.example.SummerBuild.repository.EventsRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;

@Service
@RequiredArgsConstructor
public class EventsService {
  private final EventsRepository eventsRepository;
  private final EventsMapper eventsMapper;
  private static final Logger logger = LoggerFactory.getLogger(EventsService.class);

  @ResponseStatus(HttpStatus.NOT_FOUND)
  public static class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
      super(message);
    }
  }

  @ResponseStatus(HttpStatus.CONFLICT)
  public static class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
      super(message);
    }
  }

  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public static class InvalidDataException extends RuntimeException {
    public InvalidDataException(String message) {
      super(message);
    }
  }

  @Transactional(readOnly = true)
  public List<EventsDto> findAll() {
    logger.info("Fetching all events");
    return eventsRepository.findAll().stream().map(eventsMapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public EventsDto findById(UUID id) {
    logger.info("Fetching event with id: {}", id);
    return eventsRepository
        .findById(id)
        .map(eventsMapper::toDto)
        .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));
  }

  @Transactional
  public EventsDto create(EventsDto eventsDto, UUID hostUuid) {
    logger.info("Creating new event with title: {}", eventsDto.getTitle());

    // Validate input
    validateEventData(eventsDto);

    // Set the host UUID (from authenticated user)
    eventsDto.setHostUuid(hostUuid);

    // Convert to entity and save
    Events event = eventsMapper.toEntity(eventsDto);
    Events savedEvent = eventsRepository.save(event);

    logger.info("Successfully created event with id: {}", savedEvent.getId());
    return eventsMapper.toDto(savedEvent);
  }

  @Transactional
  public EventsDto update(UUID id, EventsDto eventsDto) {
    logger.info("Updating event with id: {}", id);

    // Validate input
    validateEventData(eventsDto);

    Events existingEvent =
        eventsRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));

    // Update the existing entity with new data
    eventsMapper.updateEntityFromDto(eventsDto, existingEvent);

    Events updatedEvent = eventsRepository.save(existingEvent);

    logger.info("Successfully updated event with id: {}", id);
    return eventsMapper.toDto(updatedEvent);
  }

  @Transactional
  public void delete(UUID id) {
    logger.info("Deleting event with id: {}", id);

    if (!eventsRepository.existsById(id)) {
      throw new ResourceNotFoundException("Event not found with id: " + id);
    }

    eventsRepository.deleteById(id);
    logger.info("Successfully deleted event with id: {}", id);
  }

  @Transactional(readOnly = true)
  public List<EventsDto> findByHostUuid(UUID hostUuid) {
    logger.info("Fetching events for host: {}", hostUuid);
    return eventsRepository.findAll().stream()
        .filter(event -> event.getHost_uuid().equals(hostUuid))
        .map(eventsMapper::toDto)
        .toList();
  }

  private void validateEventData(EventsDto eventsDto) {
    if (eventsDto.getStartTime() != null && eventsDto.getEndTime() != null) {
      if (eventsDto.getStartTime().isAfter(eventsDto.getEndTime())) {
        throw new InvalidDataException("Start time cannot be after end time");
      }
    }

    if (eventsDto.getCapacity() != null && eventsDto.getCapacity() <= 0) {
      throw new InvalidDataException("Capacity must be a positive number");
    }
  }
}
