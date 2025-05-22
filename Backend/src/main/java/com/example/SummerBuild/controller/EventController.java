package com.example.SummerBuild.controller;

import com.example.SummerBuild.dto.EventDTO;
import com.example.SummerBuild.services.EventService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

  private final EventService eventService;

  @Autowired
  public EventController(EventService eventService) {
    this.eventService = eventService;
  }

  /**
   * Create a new event with error handling POST /api/events
   *
   * @param eventDTO The event data
   * @return The created event or error response
   */
  @PostMapping
  public ResponseEntity<?> createEvent(@RequestBody @Validated EventDTO eventDTO) {
    try {
      // Validate required fields
      Map<String, String> errors = validateEventDTO(eventDTO);
      if (!errors.isEmpty()) {
        return new ResponseEntity<>(
            createErrorResponse("Validation failed", errors), HttpStatus.BAD_REQUEST);
      }

      // Create the event
      EventDTO createdEvent = eventService.createEvent(eventDTO);
      return new ResponseEntity<>(createdEvent, HttpStatus.CREATED);
    } catch (Exception e) {
      // Handle unexpected errors
      return new ResponseEntity<>(
          createErrorResponse("Failed to create event", e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Get all events GET /api/events
   *
   * @return List of all events
   */
  @GetMapping
  public ResponseEntity<?> getAllEvents() {
    try {
      List<EventDTO> events = eventService.getAllEvents();
      return new ResponseEntity<>(events, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(
          createErrorResponse("Failed to retrieve events", e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Get an event by ID GET /api/events/{id}
   *
   * @param id The event ID
   * @return The event if found
   */
  @GetMapping("/{id}")
  public ResponseEntity<?> getEventById(@PathVariable UUID id) {
    try {
      var eventOptional = eventService.getEventById(id);
      if (eventOptional.isPresent()) {
        return new ResponseEntity<>(eventOptional.get(), HttpStatus.OK);
      } else {
        return new ResponseEntity<>(
            createErrorResponse("Event not found", "No event found with ID: " + id),
            HttpStatus.NOT_FOUND);
      }
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>(
          createErrorResponse("Invalid ID format", e.getMessage()), HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
      return new ResponseEntity<>(
          createErrorResponse("Failed to retrieve event", e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Update an existing event PUT /api/events/{id}
   *
   * @param id The event ID
   * @param eventDTO The updated event data
   * @return The updated event if found
   */
  @PutMapping("/{id}")
  public ResponseEntity<?> updateEvent(
      @PathVariable UUID id, @RequestBody @Validated EventDTO eventDTO) {
    try {
      // Validate required fields
      Map<String, String> errors = validateEventDTO(eventDTO);
      if (!errors.isEmpty()) {
        return new ResponseEntity<>(
            createErrorResponse("Validation failed", errors), HttpStatus.BAD_REQUEST);
      }

      var updatedEventOptional = eventService.updateEvent(id, eventDTO);
      if (updatedEventOptional.isPresent()) {
        return new ResponseEntity<>(updatedEventOptional.get(), HttpStatus.OK);
      } else {
        return new ResponseEntity<>(
            createErrorResponse("Event not found", "No event found with ID: " + id),
            HttpStatus.NOT_FOUND);
      }
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>(
          createErrorResponse("Invalid ID format", e.getMessage()), HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
      return new ResponseEntity<>(
          createErrorResponse("Failed to update event", e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Delete an event by ID DELETE /api/events/{id}
   *
   * @param id The event ID
   * @return No content if deleted, not found if the event doesn't exist
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteEvent(@PathVariable UUID id) {
    try {
      boolean deleted = eventService.deleteEvent(id);
      if (deleted) {
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
      } else {
        return new ResponseEntity<>(
            createErrorResponse("Event not found", "No event found with ID: " + id),
            HttpStatus.NOT_FOUND);
      }
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>(
          createErrorResponse("Invalid ID format", e.getMessage()), HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
      return new ResponseEntity<>(
          createErrorResponse("Failed to delete event", e.getMessage()),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  // Internal Tool to validate input

  /**
   * Validate the EventDTO
   *
   * @param eventDTO The event DTO to validate
   * @return Map of validation errors (empty if valid)
   */
  private Map<String, String> validateEventDTO(EventDTO eventDTO) {
    Map<String, String> errors = new HashMap<>();

    if (eventDTO.getName() == null || eventDTO.getName().trim().isEmpty()) {
      errors.put("name", "Event name is required");
    }

    if (eventDTO.getDate() == null) {
      errors.put("date", "Event date is required");
    }

    return errors;
  }

  /**
   * Create a standardized error response
   *
   * @param message The main error message
   * @param details The error details (can be a string or a map of field errors)
   * @return Map representing the error response
   */
  private Map<String, Object> createErrorResponse(String message, Object details) {
    Map<String, Object> errorResponse = new HashMap<>();
    errorResponse.put("error", message);
    errorResponse.put("details", details);
    errorResponse.put("timestamp", java.time.OffsetDateTime.now().toString());
    return errorResponse;
  }
}
