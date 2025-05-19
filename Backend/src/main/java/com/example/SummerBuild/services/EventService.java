package com.example.SummerBuild.services;

import com.example.SummerBuild.dto.EventDTO;
import com.example.SummerBuild.model.Event;
import com.example.SummerBuild.repository.EventRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventService {

  private final EventRepository eventRepository;

  @Autowired
  public EventService(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  /**
   * Create a new event
   *
   * @param eventDTO The event data transfer object
   * @return The created event as a DTO
   */
  public EventDTO createEvent(EventDTO eventDTO) {
    // Convert DTO to entity
    Event event = new Event(eventDTO.getName(), eventDTO.getDate());

    // Save to database (Supabase)
    Event savedEvent = eventRepository.save(event);

    // Convert back to DTO and return
    return convertToDTO(savedEvent);
  }

  /**
   * Get all events
   *
   * @return List of all events as DTOs
   */
  public List<EventDTO> getAllEvents() {
    return eventRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
  }

  /**
   * Get an event by ID
   *
   * @param id The event ID
   * @return The event as a DTO if found, otherwise empty
   */
  public Optional<EventDTO> getEventById(UUID id) {
    return eventRepository.findById(id).map(this::convertToDTO);
  }

  /**
   * Update an existing event
   *
   * @param id The event ID
   * @param eventDTO The updated event data
   * @return The updated event as a DTO if found, otherwise empty
   */
  public Optional<EventDTO> updateEvent(UUID id, EventDTO eventDTO) {
    return eventRepository
        .findById(id)
        .map(
            event -> {
              event.setName(eventDTO.getName());
              event.setDate(eventDTO.getDate());
              Event updatedEvent = eventRepository.save(event);
              return convertToDTO(updatedEvent);
            });
  }

  /**
   * Delete an event by ID
   *
   * @param id The event ID
   * @return true if deleted, false if not found
   */
  public boolean deleteEvent(UUID id) {
    if (eventRepository.existsById(id)) {
      eventRepository.deleteById(id);
      return true;
    }
    return false;
  }

  /**
   * Convert an Events entity to an EventDTO
   *
   * @param event The Events entity
   * @return The EventDTO
   */
  private EventDTO convertToDTO(Event event) {
    return new EventDTO(event.getId(), event.getName(), event.getDate());
  }
}
