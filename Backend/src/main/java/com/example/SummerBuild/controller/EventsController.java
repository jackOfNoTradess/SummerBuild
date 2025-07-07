package com.example.SummerBuild.controller;

import com.example.SummerBuild.dto.EventsDto;
import com.example.SummerBuild.service.EventsService;
import com.example.SummerBuild.util.FileLoaderService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class EventsController {

  private final EventsService eventsService;
  private final FileLoaderService fileLoaderService;
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

  @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @RequestBody(
      content =
          @Content(
              mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
              encoding = {
                @Encoding(name = "event", contentType = MediaType.APPLICATION_JSON_VALUE),
                @Encoding(name = "files", contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
              }))
  public ResponseEntity<EventsDto> createEvent(
      @RequestPart(value = "files") List<MultipartFile> files,
      @Valid @RequestPart(value = "event") EventsDto eventsDto,
      Authentication authentication) {

    logger.info("=== CONTROLLER REACHED ===");
    logger.info("POST /api/events - Creating new event with title: {}", eventsDto.getTitle());
    logger.info("Authentication object: {}", authentication);
    logger.info(
        "Authentication name: {}", authentication != null ? authentication.getName() : "NULL");

    // Get the authenticated user's UUID
    UUID hostUuid = UUID.fromString(authentication.getName());

    EventsDto createdEvent = eventsService.create(eventsDto, hostUuid);

    UUID eventUuid = createdEvent.getId();

    logger.info("inserting images for event into bucket: {}", eventUuid);
    String serverReply = fileLoaderService.uploadFile(files, eventUuid, hostUuid);
    if (serverReply == null || !serverReply.equals("Files uploaded successfully")) {
      logger.error("File upload failed for event: {}. Server response: {}", eventUuid, serverReply);
      logger.error("File upload failed: {}", serverReply != null ? serverReply : "Unknown error");

      // look at logs for more details cause i cant return a string here
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }

    return ResponseEntity.status(HttpStatus.CREATED).body(createdEvent);
  }

  @PutMapping(
      value = "/{id}",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @RequestBody(
      content =
          @Content(
              mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
              encoding = {
                @Encoding(name = "event", contentType = MediaType.APPLICATION_JSON_VALUE),
                @Encoding(name = "files", contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
              }))
  public ResponseEntity<EventsDto> updateEvent(
      @RequestPart(value = "files", required = false) List<MultipartFile> files,
      @PathVariable UUID id,
      @Valid @RequestPart(value = "event") EventsDto eventsDto,
      Authentication authentication) {
    logger.info("PUT /api/events/{} - Updating event", id);

    // Verify the authenticated user is the event host
    UUID hostUuid = UUID.fromString(authentication.getName());
    EventsDto event = eventsService.findById(id);

    if (!event.getHostUuid().equals(hostUuid)) {
      logger.warn("Unauthorized attempt to delete file by user: {}", hostUuid);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    if (files != null && !files.isEmpty()) {
      logger.info("Updating event with ID: {} and uploading new files", id);
      String serverReply = fileLoaderService.uploadFile(files, id, hostUuid);
      if (serverReply == null || !serverReply.equals("Files uploaded successfully")) {
        logger.error("File upload failed for event: {}. Server response: {}", id, serverReply);

        // same thing here, look at logs for more details cause i cant return a string
        // here
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
      }
    } else {
      logger.info("No files provided for update of event with ID: {}", id);
    }

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

  /** Register for an event (decrements available capacity) */
  @PostMapping("/{eventId}/register")
  public ResponseEntity<String> registerForEvent(
      @PathVariable UUID eventId, Authentication authentication) {
    logger.info(
        "POST /api/events/{}/register - User {} registering for event",
        eventId,
        authentication.getName());

    try {
      boolean success = eventsService.registerForEvent(eventId);
      if (success) {
        return ResponseEntity.ok("Successfully registered for event");
      } else {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body("Event is full - no available capacity");
      }
    } catch (EventsService.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Event not found");
    } catch (Exception e) {
      logger.error("Error registering for event {}: {}", eventId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to register for event");
    }
  }

  /** Unregister from an event (increments available capacity) */
  @DeleteMapping("/{eventId}/register")
  public ResponseEntity<String> unregisterFromEvent(
      @PathVariable UUID eventId, Authentication authentication) {
    logger.info(
        "DELETE /api/events/{}/register - User {} unregistering from event",
        eventId,
        authentication.getName());

    try {
      eventsService.unregisterFromEvent(eventId);
      return ResponseEntity.ok("Successfully unregistered from event");
    } catch (EventsService.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Event not found");
    } catch (Exception e) {
      logger.error("Error unregistering from event {}: {}", eventId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Failed to unregister from event");
    }
  }

  /** Get current available capacity for an event */
  @GetMapping("/{eventId}/capacity")
  public ResponseEntity<Integer> getEventCapacity(@PathVariable UUID eventId) {
    logger.info("GET /api/events/{}/capacity - Fetching current capacity", eventId);

    try {
      EventsDto event = eventsService.findById(eventId);
      return ResponseEntity.ok(event.getCapacity());
    } catch (EventsService.ResourceNotFoundException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    } catch (Exception e) {
      logger.error("Error fetching capacity for event {}: {}", eventId, e.getMessage(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GetMapping("/{eventId}/files/{fileName}")
  public ResponseEntity<String> getEventFile(
      @PathVariable UUID eventId, @PathVariable String fileName) {
    logger.info("GET /api/events/{}/files/{} - Fetching file", eventId, fileName);

    String filePath = fileLoaderService.getFilePath(eventId.toString(), fileName);
    if (filePath != null) {
      return ResponseEntity.ok(filePath);
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{eventId}/files/{fileName}")
  public ResponseEntity<Void> deleteEventFile(
      @PathVariable UUID eventId, @PathVariable String fileName, Authentication authentication) {
    logger.info("DELETE /api/events/{}/files/{} - Deleting file", eventId, fileName);

    // Verify the authenticated user is the event host
    UUID hostUuid = UUID.fromString(authentication.getName());
    EventsDto event = eventsService.findById(eventId);

    if (!event.getHostUuid().equals(hostUuid)) {
      logger.warn("Unauthorized attempt to delete file by user: {}", hostUuid);
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    boolean deleted = fileLoaderService.deleteFile(eventId.toString(), fileName);
    if (deleted) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.notFound().build();
    }
  }
}
