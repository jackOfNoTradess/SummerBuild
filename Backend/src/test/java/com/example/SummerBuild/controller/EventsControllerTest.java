package com.example.SummerBuild.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.SummerBuild.dto.EventsDto;
import com.example.SummerBuild.service.EventsService;
import com.example.SummerBuild.service.EventsService.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class EventsControllerTest {

  private MockMvc mockMvc;

  @Mock private EventsService eventsService;
  @Mock private Authentication authentication;

  @InjectMocks private EventsController eventsController;

  private ObjectMapper objectMapper;
  private UUID testEventId;
  private UUID testHostId;
  private EventsDto testEventDto;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(eventsController)
            .setMessageConverters(
                new org.springframework.http.converter.StringHttpMessageConverter(),
                new MappingJackson2HttpMessageConverter())
            .build();

    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();

    testEventId = UUID.randomUUID();
    testHostId = UUID.randomUUID();
    testEventDto = createTestEventDto();
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
    dto.setPicPath("/images/test.jpg");
    dto.setCreatedAt(LocalDateTime.now());
    dto.setUpdatedAt(LocalDateTime.now());
    return dto;
  }

  @Test
  @DisplayName("GET /api/events - happy flow")
  void whenGetAllEvents_happyFlow_returns200() throws Exception {
    List<EventsDto> events = Arrays.asList(testEventDto);
    given(eventsService.findAll()).willReturn(events);

    mockMvc
        .perform(get("/api/events"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("Test Event"))
        .andExpect(jsonPath("$[0].capacity").value(100));

    verify(eventsService).findAll();
  }

  @Test
  @DisplayName("GET /api/events/{id} - happy flow")
  void whenGetEventById_happyFlow_returns200() throws Exception {
    given(eventsService.findById(testEventId)).willReturn(testEventDto);

    mockMvc
        .perform(get("/api/events/{id}", testEventId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Test Event"))
        .andExpect(jsonPath("$.hostUuid").value(testHostId.toString()));

    verify(eventsService).findById(testEventId);
  }

  @Test
  @DisplayName("GET /api/events/{id} - sad flow (not found)")
  void whenGetEventById_sadFlow_returns404() throws Exception {
    given(eventsService.findById(testEventId))
        .willThrow(new ResourceNotFoundException("Event not found with id: " + testEventId));

    mockMvc.perform(get("/api/events/{id}", testEventId)).andExpect(status().isNotFound());

    verify(eventsService).findById(testEventId);
  }

  @Test
  @DisplayName("POST /api/events - happy flow")
  void whenCreateEvent_happyFlow_returns201() throws Exception {
    given(authentication.getName()).willReturn(testHostId.toString());
    given(eventsService.create(any(EventsDto.class), eq(testHostId))).willReturn(testEventDto);

    String eventJson = objectMapper.writeValueAsString(testEventDto);

    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson)
                .principal(authentication))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Test Event"));

    verify(eventsService).create(any(EventsDto.class), eq(testHostId));
  }

  @Test
  @DisplayName("POST /api/events - sad flow (invalid data)")
  void whenCreateEvent_sadFlow_returns400() throws Exception {
    EventsDto invalidDto = new EventsDto();
    // Missing required fields

    String eventJson = objectMapper.writeValueAsString(invalidDto);

    mockMvc
        .perform(
            post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson)
                .principal(authentication))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT /api/events/{id} - happy flow")
  void whenUpdateEvent_happyFlow_returns200() throws Exception {
    EventsDto updatedDto = createTestEventDto();
    updatedDto.setTitle("Updated Event");

    given(eventsService.update(eq(testEventId), any(EventsDto.class))).willReturn(updatedDto);

    String eventJson = objectMapper.writeValueAsString(updatedDto);

    mockMvc
        .perform(
            put("/api/events/{id}", testEventId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated Event"));

    verify(eventsService).update(eq(testEventId), any(EventsDto.class));
  }

  @Test
  @DisplayName("PUT /api/events/{id} - sad flow (not found)")
  void whenUpdateEvent_sadFlow_returns404() throws Exception {
    given(eventsService.update(eq(testEventId), any(EventsDto.class)))
        .willThrow(new ResourceNotFoundException("Event not found with id: " + testEventId));

    String eventJson = objectMapper.writeValueAsString(testEventDto);

    mockMvc
        .perform(
            put("/api/events/{id}", testEventId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(eventJson))
        .andExpect(status().isNotFound());

    verify(eventsService).update(eq(testEventId), any(EventsDto.class));
  }

  @Test
  @DisplayName("DELETE /api/events/{id} - happy flow")
  void whenDeleteEvent_happyFlow_returns204() throws Exception {
    doNothing().when(eventsService).delete(testEventId);

    mockMvc.perform(delete("/api/events/{id}", testEventId)).andExpect(status().isNoContent());

    verify(eventsService).delete(testEventId);
  }

  @Test
  @DisplayName("DELETE /api/events/{id} - sad flow (not found)")
  void whenDeleteEvent_sadFlow_returns404() throws Exception {
    doThrow(new ResourceNotFoundException("Event not found with id: " + testEventId))
        .when(eventsService)
        .delete(testEventId);

    mockMvc.perform(delete("/api/events/{id}", testEventId)).andExpect(status().isNotFound());

    verify(eventsService).delete(testEventId);
  }

  @Test
  @DisplayName("GET /api/events/host/{hostUuid} - happy flow")
  void whenGetEventsByHost_happyFlow_returns200() throws Exception {
    List<EventsDto> events = Arrays.asList(testEventDto);
    given(eventsService.findByHostUuid(testHostId)).willReturn(events);

    mockMvc
        .perform(get("/api/events/host/{hostUuid}", testHostId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].hostUuid").value(testHostId.toString()));

    verify(eventsService).findByHostUuid(testHostId);
  }

  @Test
  @DisplayName("GET /api/events/host/{hostUuid} - sad flow (empty list)")
  void whenGetEventsByHost_sadFlow_returnsEmptyList() throws Exception {
    given(eventsService.findByHostUuid(testHostId)).willReturn(Arrays.asList());

    mockMvc
        .perform(get("/api/events/host/{hostUuid}", testHostId))
        .andExpect(status().isOk())
        .andExpect(content().string("[]"));

    verify(eventsService).findByHostUuid(testHostId);
  }
}
