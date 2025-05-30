package com.example.SummerBuild.controller;

import com.example.SummerBuild.dto.EventsDto;
import com.example.SummerBuild.service.EventsService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class EventsController {

  private final EventsService eventsService;
  private static final Logger logger = LoggerFactory.getLogger(EventsController.class);

  @GetMapping
  public ResponseEntity<List<EventsDto>> getAllEvents() {
    logger.info("GET /api/events - Fetching all events");
    List<EventsDto> events = eventsService.findAll();
    return ResponseEntity.ok(events);
  }

  @GetMapping("/{id}")
  public ResponseEntity<EventsDto> getEventById(@PathVariable UUID id) {
    logger.info("GET /api/events/{} - Fetching event by ID", id);
    EventsDto event = eventsService.findById(id);
    return ResponseEntity.ok(event);
  }

  @PostMapping
  public ResponseEntity<EventsDto> createEvent(
      @Valid @RequestBody EventsDto eventsDto, Authentication authentication) {
    logger.info("POST /api/events - Creating new event with title: {}", eventsDto.getTitle());

    // Get the authenticated user's UUID
    UUID hostUuid = UUID.fromString(authentication.getName());

    EventsDto createdEvent = eventsService.create(eventsDto, hostUuid);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
  }

  @PutMapping("/{id}")
  public ResponseEntity<EventsDto> updateEvent(
      @PathVariable UUID id, @Valid @RequestBody EventsDto eventsDto) {
    logger.info("PUT /api/events/{} - Updating event", id);
    EventsDto updatedEvent = eventsService.update(id, eventsDto);
    return ResponseEntity.ok(updatedEvent);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteEvent(@PathVariable UUID id) {
    logger.info("DELETE /api/events/{} - Deleting event", id);
    eventsService.delete(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/host/{hostUuid}")
  public ResponseEntity<List<EventsDto>> getEventsByHost(@PathVariable UUID hostUuid) {
    logger.info("GET /api/events/host/{} - Fetching events by host", hostUuid);
    List<EventsDto> events = eventsService.findByHostUuid(hostUuid);
    return ResponseEntity.ok(events);
  }
}
