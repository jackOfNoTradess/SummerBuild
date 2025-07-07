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
  private final DistributedLockService distributedLockService;
  private final AtomicCapacityService atomicCapacityService;
  private static final Logger logger = LoggerFactory.getLogger(EventsService.class);

  private static final String EVENT_LOCK_PREFIX = "event:lock:";

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
    List<Events> events = eventsRepository.findAll();
    return events.stream()
        .map(
            event -> {
              EventsDto dto = eventsMapper.toDto(event);
              // Get real-time capacity from Redis
              long currentCapacity = atomicCapacityService.getCurrentCapacity(event.getId());
              dto.setCapacity((int) currentCapacity);
              return dto;
            })
        .toList();
  }

  @Transactional(readOnly = true)
  public EventsDto findById(UUID id) {
    logger.info("Fetching event with id: {}", id);
    Events event =
        eventsRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));

    EventsDto dto = eventsMapper.toDto(event);
    // Get real-time capacity from Redis
    long currentCapacity = atomicCapacityService.getCurrentCapacity(id);
    dto.setCapacity((int) currentCapacity);
    return dto;
  }

  @Transactional
  public EventsDto create(EventsDto eventsDto, UUID hostUuid) {
    logger.info("Creating new event with title: {}", eventsDto.getTitle());

    return distributedLockService.executeWithLock(
        EVENT_LOCK_PREFIX + "create:" + hostUuid,
        () -> {
          // Validate input
          validateEventData(eventsDto);

          // Set the host UUID (from authenticated user)
          eventsDto.setHostUuid(hostUuid);

          // Convert to entity and save
          Events event = eventsMapper.toEntity(eventsDto);
          Events savedEvent = eventsRepository.save(event);

          // Initialize capacity in Redis
          atomicCapacityService.initializeCapacity(savedEvent.getId(), savedEvent.getCapacity());

          EventsDto resultDto = eventsMapper.toDto(savedEvent);
          logger.info("Successfully created event with id: {}", savedEvent.getId());
          return resultDto;
        });
  }

  @Transactional
  public EventsDto update(UUID id, EventsDto eventsDto) {
    logger.info("Updating event with id: {}", id);

    return distributedLockService.executeWithLock(
        EVENT_LOCK_PREFIX + id,
        () -> {
          // Validate input
          validateEventData(eventsDto);

          Events existingEvent =
              eventsRepository
                  .findById(id)
                  .orElseThrow(
                      () -> new ResourceNotFoundException("Event not found with id: " + id));

          // Update the existing entity with new data
          eventsMapper.updateEntityFromDto(eventsDto, existingEvent);
          Events updatedEvent = eventsRepository.save(existingEvent);

          // Update capacity in Redis if it changed
          if (eventsDto.getCapacity() != null) {
            atomicCapacityService.updateCapacity(id, eventsDto.getCapacity());
          }

          EventsDto resultDto = eventsMapper.toDto(updatedEvent);
          logger.info("Successfully updated event with id: {}", id);
          return resultDto;
        });
  }

  @Transactional
  public void delete(UUID id) {
    logger.info("Deleting event with id: {}", id);

    distributedLockService.executeWithLock(
        EVENT_LOCK_PREFIX + id,
        () -> {
          if (!eventsRepository.existsById(id)) {
            throw new ResourceNotFoundException("Event not found with id: " + id);
          }

          eventsRepository.deleteById(id);
          // Clean up capacity from Redis
          atomicCapacityService.deleteCapacity(id);

          logger.info("Successfully deleted event with id: {}", id);
          return null;
        });
  }

  @Transactional(readOnly = true)
  public List<EventsDto> findByHostUuid(UUID hostUuid) {
    logger.info("Fetching events for host: {}", hostUuid);
    List<Events> events =
        eventsRepository.findAll().stream()
            .filter(event -> event.getHostId().equals(hostUuid))
            .toList();

    return events.stream()
        .map(
            event -> {
              EventsDto dto = eventsMapper.toDto(event);
              // Get real-time capacity from Redis
              long currentCapacity = atomicCapacityService.getCurrentCapacity(event.getId());
              dto.setCapacity((int) currentCapacity);
              return dto;
            })
        .toList();
  }

  /** handle event registration (decrement capacity) */
  public boolean registerForEvent(UUID eventId) {
    logger.info("Attempting to register for event: {}", eventId);

    return distributedLockService.executeWithLock(
        EVENT_LOCK_PREFIX + eventId,
        () -> {
          // Verify event exists
          if (!eventsRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
          }

          // Atomically decrement capacity
          boolean success = atomicCapacityService.decrementCapacity(eventId);
          if (success) {
            logger.info("Successfully registered for event: {}", eventId);
          } else {
            logger.warn("Failed to register for event: {} - no capacity available", eventId);
          }
          return success;
        });
  }

  /** handle event unregistration (increment capacity) */
  public void unregisterFromEvent(UUID eventId) {
    logger.info("Unregistering from event: {}", eventId);

    distributedLockService.executeWithLock(
        EVENT_LOCK_PREFIX + eventId,
        () -> {
          // Verify event exists
          if (!eventsRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
          }

          // Atomically increment capacity
          atomicCapacityService.incrementCapacity(eventId);
          logger.info("Successfully unregistered from event: {}", eventId);
          return null;
        });
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
