package com.example.SummerBuild.controller;

import com.example.SummerBuild.dto.ParticipatesDto;
import com.example.SummerBuild.service.ParticipatesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/participates")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ParticipatesController {

  private final ParticipatesService participatesService;
  private static final Logger logger = LoggerFactory.getLogger(ParticipatesController.class);

  @PostMapping("/register")
  public ResponseEntity<ParticipatesDto> registerEvent(@RequestBody ParticipatesDto request) {
    try {
      logger.info(
          "POST /api/participates/register - Registering user : {} to event : {}",
          request.getUserId(),
          request.getEventId());
      ParticipatesDto participation =
          participatesService.addParticipation(request.getUserId(), request.getEventId());
      return ResponseEntity.status(HttpStatus.CREATED).body(participation);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @DeleteMapping("/unregister")
  public ResponseEntity<Void> unregisterEvent(@RequestBody ParticipatesDto request) {
    try {
      participatesService.removeParticipation(request.getUserId(), request.getEventId());
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/user/{userId}")
  @Operation(
      summary = "Get user participations",
      description = "Get all events a user is participating in")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved user participations")
  public ResponseEntity<List<ParticipatesDto>> getUserParticipations(
      @Parameter(description = "User ID") @PathVariable UUID userId) {
    List<ParticipatesDto> participations = participatesService.getUserParticipations(userId);
    return ResponseEntity.ok(participations);
  }

  @GetMapping("/event/{eventId}")
  @Operation(summary = "Get event participants", description = "Get all participants for an event")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved event participants")
  public ResponseEntity<List<ParticipatesDto>> getEventParticipants(
      @Parameter(description = "Event ID") @PathVariable UUID eventId) {
    List<ParticipatesDto> participants = participatesService.getEventParticipants(eventId);
    return ResponseEntity.ok(participants);
  }

  @GetMapping("/check")
  @Operation(
      summary = "Check participation",
      description = "Check if a user is participating in an event")
  @ApiResponse(responseCode = "200", description = "Successfully checked participation status")
  public ResponseEntity<Boolean> checkParticipation(
      @Parameter(description = "User ID") @RequestParam UUID userId,
      @Parameter(description = "Event ID") @RequestParam UUID eventId) {
    boolean isParticipating = participatesService.isUserParticipating(userId, eventId);
    return ResponseEntity.ok(isParticipating);
  }

  @GetMapping("/count/event/{eventId}")
  @Operation(
      summary = "Get participant count",
      description = "Get the number of participants for an event")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved participant count")
  public ResponseEntity<Long> getParticipantCount(
      @Parameter(description = "Event ID") @PathVariable UUID eventId) {
    long count = participatesService.getParticipantCount(eventId);
    return ResponseEntity.ok(count);
  }

  @GetMapping("/count/user/{userId}")
  @Operation(
      summary = "Get user event count",
      description = "Get the number of events a user is participating in")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved user event count")
  public ResponseEntity<Long> getUserEventCount(
      @Parameter(description = "User ID") @PathVariable UUID userId) {
    long count = participatesService.getUserEventCount(userId);
    return ResponseEntity.ok(count);
  }

  @GetMapping
  @Operation(
      summary = "Get all participations",
      description = "Get all participations in the system")
  @ApiResponse(responseCode = "200", description = "Successfully retrieved all participations")
  public ResponseEntity<List<ParticipatesDto>> getAllParticipations() {
    List<ParticipatesDto> participations = participatesService.getAllParticipations();
    return ResponseEntity.ok(participations);
  }

  // @GetMapping("/{id}")
  // @Operation(summary = "Get participation by ID", description = "Get a specific participation by
  // its ID")
  // @ApiResponses(value = {
  //     @ApiResponse(responseCode = "200", description = "Successfully retrieved participation"),
  //     @ApiResponse(responseCode = "404", description = "Participation not found")
  // })
  // public ResponseEntity<ParticipatesDto> getParticipationById(
  //         @Parameter(description = "Participation ID") @PathVariable UUID id) {
  //     Optional<ParticipatesDto> participation = participatesService.getParticipationById(id);
  //     return participation.map(ResponseEntity::ok)
  //             .orElse(ResponseEntity.notFound().build());
  // }
}
