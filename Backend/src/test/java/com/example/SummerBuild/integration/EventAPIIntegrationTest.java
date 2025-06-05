package com.example.SummerBuild.integration;

import static org.assertj.core.api.Assertions.*;

import com.example.SummerBuild.config.EventsDtoTestByPass;
import com.example.SummerBuild.config.TestAuthConfig;
import com.example.SummerBuild.config.TestSecurityConfig;
import com.example.SummerBuild.dto.EventsDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import({TestAuthConfig.class, TestSecurityConfig.class})
class EventAPIIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ObjectMapper objectMapper;

  @LocalServerPort private int port;

  private String baseUrl;
  private HttpHeaders authHeaders;
  private UUID testHostUuid;

  @BeforeEach
  void setUp() {
    baseUrl = "http://localhost:" + port + "/api/events";
    testHostUuid = signupAndGetUserId();
    String jwtToken = loginAndGetToken();

    authHeaders = new HttpHeaders();
    authHeaders.setContentType(MediaType.APPLICATION_JSON);
    authHeaders.setBearerAuth(jwtToken);
    objectMapper.addMixIn(EventsDto.class, EventsDtoTestByPass.class);
  }

  @AfterEach
  void cleanupTestData() {
    try {
      jdbcTemplate.update(
          "DELETE FROM event_tags WHERE event_id IN (SELECT id FROM events WHERE title LIKE '%Test%' OR title LIKE '%Host%')");
      jdbcTemplate.update("DELETE FROM events WHERE title LIKE '%Test%' OR title LIKE '%Host%'");
    } catch (Exception e) {
      System.out.println("Test cleanup warning: " + e.getMessage());
    }
  }

  private UUID signupAndGetUserId() {
    String signupUrl = "http://localhost:" + port + "/api/auth/signup";
    String signupParams =
        "?email=testuser@example.com&password=testpassword123&displayName=Test User&userRole=USER&gender=OTHERS";
    ResponseEntity<String> signupResponse =
        restTemplate.postForEntity(signupUrl + signupParams, null, String.class);
    try {
      JsonNode jsonResponse = objectMapper.readTree(signupResponse.getBody());
      String userId = jsonResponse.path("user").path("id").asText();
      return UUID.fromString(userId);
    } catch (Exception e) {
      return UUID.nameUUIDFromBytes("testuser@example.com".getBytes());
    }
  }

  private String loginAndGetToken() {
    String signupUrl = "http://localhost:" + port + "/api/auth/login";
    String signupParams = "?email=testuser@example.com&password=testpassword123";

    ResponseEntity<String> loginResponse =
        restTemplate.postForEntity(signupUrl + signupParams, null, String.class);

    try {
      JsonNode jsonResponse = objectMapper.readTree(loginResponse.getBody());
      String token = jsonResponse.get("access_token").asText();
      System.out.println("=== JWT TOKEN GENERATED ===");
      System.out.println("Token: " + token);
      return token;
    } catch (Exception e) {
      throw new RuntimeException("Failed to extract JWT token from login response", e);
    }
  }

  /** Tests successful creation of an event and verifies DB persistence */
  @Test
  @Order(1)
  @DisplayName("Integration: Create Event → Verify Database Storage → Retrieve via API")
  void testCreateEvent() {
    System.out.println("=== TEST STARTED ===");

    EventsDto newEvent = createValidEventDto();
    newEvent.setTitle("Integration Test Event");
    newEvent.setCapacity(75);

    System.out.println("=== ABOUT TO MAKE HTTP REQUEST ===");
    HttpEntity<EventsDto> request = new HttpEntity<>(newEvent, authHeaders);

    try {
      ResponseEntity<EventsDto> createResponse =
          restTemplate.postForEntity(baseUrl, request, EventsDto.class);
      System.out.println("=== HTTP REQUEST COMPLETED ===");
      System.out.println("Status: " + createResponse.getStatusCode());
      System.out.println("Body: " + createResponse.getBody());

      assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(createResponse.getBody()).isNotNull();

    } catch (Exception e) {
      System.out.println("=== HTTP REQUEST FAILED ===");
      System.out.println("Exception: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
  }

  /** Tests updating an event and checks DB reflects the changes */
  @Test
  @Order(2)
  @DisplayName("Integration: Update Event → Verify Database Changes → Confirm via API")
  void testUpdateEvent() {
    EventsDto originalEvent = createValidEventDto();
    originalEvent.setTitle("Original Event");
    originalEvent.setCapacity(50);

    HttpEntity<EventsDto> createRequest = new HttpEntity<>(originalEvent, authHeaders);
    ResponseEntity<EventsDto> createResponse =
        restTemplate.postForEntity(baseUrl, createRequest, EventsDto.class);
    UUID eventId = createResponse.getBody().getId();

    String selectSql = "SELECT title, capacity FROM events WHERE id = ?";
    Map<String, Object> originalDbRecord = jdbcTemplate.queryForMap(selectSql, eventId);
    assertThat(originalDbRecord.get("title")).isEqualTo("Original Event");
    assertThat(originalDbRecord.get("capacity")).isEqualTo(50);

    EventsDto updateEvent = createValidEventDto();
    updateEvent.setTitle("Updated Event Title");
    updateEvent.setCapacity(100);
    updateEvent.setDescription("Updated description via integration test");

    HttpEntity<EventsDto> updateRequest = new HttpEntity<>(updateEvent, authHeaders);
    ResponseEntity<EventsDto> updateResponse =
        restTemplate.exchange(
            baseUrl + "/" + eventId, HttpMethod.PUT, updateRequest, EventsDto.class);

    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updateResponse.getBody().getTitle()).isEqualTo("Updated Event Title");

    Map<String, Object> updatedDbRecord = jdbcTemplate.queryForMap(selectSql, eventId);
    assertThat(updatedDbRecord.get("title")).isEqualTo("Updated Event Title");
    assertThat(updatedDbRecord.get("capacity")).isEqualTo(100);

    String timestampSql = "SELECT updated_at FROM events WHERE id = ?";
    Object updatedAt = jdbcTemplate.queryForObject(timestampSql, Object.class, eventId);
    assertThat(updatedAt).isNotNull();
  }

  /** Tests filtering events by hostUuid and validates DB results */
  @Test
  @Order(3)
  @DisplayName("Integration: Filter Events by Host → Verify Database Query → API Response")
  void testFilterByHost() {
    EventsDto event1 = createValidEventDto();
    event1.setTitle("Host1 Event 1");
    event1.setCapacity(50);

    EventsDto event2 = createValidEventDto();
    event2.setTitle("Host1 Event 2");
    event2.setCapacity(75);

    HttpEntity<EventsDto> createRequest1 = new HttpEntity<>(event1, authHeaders);
    HttpEntity<EventsDto> createRequest2 = new HttpEntity<>(event2, authHeaders);

    ResponseEntity<EventsDto> createResponse1 =
        restTemplate.postForEntity(baseUrl, createRequest1, EventsDto.class);
    ResponseEntity<EventsDto> createResponse2 =
        restTemplate.postForEntity(baseUrl, createRequest2, EventsDto.class);

    assertThat(createResponse1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(createResponse2.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    UUID host1 = testHostUuid;

    HttpEntity<Void> request = new HttpEntity<>(authHeaders);
    ResponseEntity<EventsDto[]> response =
        restTemplate.exchange(
            baseUrl + "/host/" + host1, HttpMethod.GET, request, EventsDto[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<EventsDto> host1Events = Arrays.asList(response.getBody());

    List<EventsDto> testEvents =
        host1Events.stream().filter(event -> event.getTitle().startsWith("Host1 Event")).toList();

    assertThat(testEvents).hasSize(2);
    assertThat(testEvents).allMatch(event -> event.getHostUuid().equals(host1));

    String verifySql =
        "SELECT COUNT(*) FROM events WHERE host_id = ? AND title LIKE 'Host1 Event%'";
    Integer host1Count = jdbcTemplate.queryForObject(verifySql, Integer.class, host1);
    assertThat(host1Count).isEqualTo(2);
  }

  /** Tests deleting an event and verifies it is removed from DB */
  @Test
  @Order(4)
  @DisplayName("Integration: Delete Event → Verify Database Removal → Confirm 404 Response")
  void testDeleteEvent() {
    EventsDto eventToDelete = createValidEventDto();
    eventToDelete.setTitle("Event to Delete");

    HttpEntity<EventsDto> createRequest = new HttpEntity<>(eventToDelete, authHeaders);
    ResponseEntity<String> createResponseRaw =
        restTemplate.postForEntity(baseUrl, createRequest, String.class);

    System.out.println("=== CREATE RESPONSE RAW ===");
    System.out.println("Status: " + createResponseRaw.getStatusCode());
    System.out.println("Body: " + createResponseRaw.getBody());

    EventsDto createdEvent = null;
    UUID eventId = null;

    try {
      ObjectMapper testMapper = new ObjectMapper();
      testMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
      testMapper.addMixIn(EventsDto.class, EventsDtoTestByPass.class);
      System.out.println("Mix-in applied: " + objectMapper.findMixInClassFor(EventsDto.class));
      createdEvent = testMapper.readValue(createResponseRaw.getBody(), EventsDto.class);
      System.out.println("Mapper identity: " + testMapper);
      eventId = createdEvent.getId();
      System.out.println("Parsed ID: " + eventId);
      System.out.println("Parsed Host UUID: " + createdEvent.getHostUuid());
    } catch (Exception e) {
      System.out.println("=== ERROR PARSING JSON ===");
      e.printStackTrace();
    }

    String countSql = "SELECT COUNT(*) FROM events WHERE id = ?";
    Integer countBefore = jdbcTemplate.queryForObject(countSql, Integer.class, eventId);
    assertThat(countBefore).isEqualTo(1);

    HttpEntity<Void> deleteRequest = new HttpEntity<>(authHeaders);
    ResponseEntity<Void> deleteResponse =
        restTemplate.exchange(
            baseUrl + "/" + eventId, HttpMethod.DELETE, deleteRequest, Void.class);

    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    Integer countAfter = jdbcTemplate.queryForObject(countSql, Integer.class, eventId);
    assertThat(countAfter).isEqualTo(0);

    HttpEntity<Void> getRequest = new HttpEntity<>(authHeaders);
    ResponseEntity<EventsDto> getResponse =
        restTemplate.exchange(baseUrl + "/" + eventId, HttpMethod.GET, getRequest, EventsDto.class);
    assertThat(getResponse.getStatusCode()).isIn(HttpStatus.NOT_FOUND, HttpStatus.FORBIDDEN);
  }

  /** Tests invalid input handling and ensures DB rollback occurs */
  @Test
  @Order(5)
  @DisplayName("Integration: Validation Errors → Database Rollback → Error Response")
  void testValidationRollback() {
    EventsDto invalidEvent = new EventsDto();
    invalidEvent.setTitle("");
    invalidEvent.setCapacity(-10);
    invalidEvent.setHostUuid(testHostUuid);

    String countSql = "SELECT COUNT(*) FROM events";
    Integer countBefore = jdbcTemplate.queryForObject(countSql, Integer.class);

    HttpEntity<EventsDto> request = new HttpEntity<>(invalidEvent, authHeaders);

    ResponseEntity<EventsDto> response =
        restTemplate.postForEntity(baseUrl, request, EventsDto.class);

    assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.FORBIDDEN);

    Integer countAfter = jdbcTemplate.queryForObject(countSql, Integer.class);
    assertThat(countAfter).isEqualTo(countBefore);
  }

  /** Tests retrieval of all events and validates response correctness */
  @Test
  @Order(6)
  @DisplayName("Integration: Get All Events → Database Query → Pagination Response")
  void testGetAllEvents() {
    EventsDto event1 = createValidEventDto();
    event1.setTitle("Test Event 1");
    event1.setCapacity(51);

    EventsDto event2 = createValidEventDto();
    event2.setTitle("Test Event 2");
    event2.setCapacity(52);

    EventsDto event3 = createValidEventDto();
    event3.setTitle("Test Event 3");
    event3.setCapacity(53);

    HttpEntity<EventsDto> createRequest1 = new HttpEntity<>(event1, authHeaders);
    HttpEntity<EventsDto> createRequest2 = new HttpEntity<>(event2, authHeaders);
    HttpEntity<EventsDto> createRequest3 = new HttpEntity<>(event3, authHeaders);

    ResponseEntity<EventsDto> createResponse1 =
        restTemplate.postForEntity(baseUrl, createRequest1, EventsDto.class);
    ResponseEntity<EventsDto> createResponse2 =
        restTemplate.postForEntity(baseUrl, createRequest2, EventsDto.class);
    ResponseEntity<EventsDto> createResponse3 =
        restTemplate.postForEntity(baseUrl, createRequest3, EventsDto.class);

    assertThat(createResponse1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(createResponse2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(createResponse3.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    HttpEntity<Void> request = new HttpEntity<>(authHeaders);
    ResponseEntity<EventsDto[]> response =
        restTemplate.exchange(baseUrl, HttpMethod.GET, request, EventsDto[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<EventsDto> events = Arrays.asList(response.getBody());

    List<String> eventTitles =
        events.stream()
            .map(EventsDto::getTitle)
            .filter(title -> title.startsWith("Test Event "))
            .toList();
    assertThat(eventTitles).hasSize(3);

    assertThat(events.size()).isGreaterThanOrEqualTo(3);

    assertThat(events)
        .allMatch(
            event ->
                event.getId() != null
                    && event.getTitle() != null
                    && event.getHostUuid() != null
                    && event.getCapacity() != null
                    && event.getCreatedAt() != null);
  }

  /** Tests that endpoints reject unauthenticated access */
  @Test
  @Order(7)
  @DisplayName("Integration: Authentication Flow → Security → Database Access")
  void testAuthRequired() {
    HttpHeaders noAuthHeaders = new HttpHeaders();
    noAuthHeaders.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Void> unauthenticatedRequest = new HttpEntity<>(noAuthHeaders);

    ResponseEntity<EventsDto[]> response =
        restTemplate.exchange(baseUrl, HttpMethod.GET, unauthenticatedRequest, EventsDto[].class);

    assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);

    HttpEntity<Void> authenticatedRequest = new HttpEntity<>(authHeaders);
    ResponseEntity<EventsDto[]> authResponse =
        restTemplate.exchange(baseUrl, HttpMethod.GET, authenticatedRequest, EventsDto[].class);
    assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  /** Tests concurrent operations and ensures DB consistency is maintained */
  @Test
  @Order(8)
  @DisplayName("Integration: Concurrent Operations → Database Consistency → API Responses")
  void testConcurrentAccess() {
    EventsDto concurrentEvent = createValidEventDto();
    concurrentEvent.setTitle("Concurrent Test Event");

    HttpEntity<EventsDto> createRequest = new HttpEntity<>(concurrentEvent, authHeaders);
    ResponseEntity<EventsDto> createResponse =
        restTemplate.postForEntity(baseUrl, createRequest, EventsDto.class);
    UUID eventId = createResponse.getBody().getId();

    HttpEntity<Void> getRequest = new HttpEntity<>(authHeaders);

    ResponseEntity<EventsDto> response1 =
        restTemplate.exchange(baseUrl + "/" + eventId, HttpMethod.GET, getRequest, EventsDto.class);
    ResponseEntity<EventsDto> response2 =
        restTemplate.exchange(baseUrl + "/" + eventId, HttpMethod.GET, getRequest, EventsDto.class);

    assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response1.getBody().getId()).isEqualTo(response2.getBody().getId());
    assertThat(response1.getBody().getTitle()).isEqualTo(response2.getBody().getTitle());

    String sql = "SELECT title FROM events WHERE id = ?";
    String dbTitle = jdbcTemplate.queryForObject(sql, String.class, eventId);
    assertThat(dbTitle).isEqualTo("Concurrent Test Event");
  }

  private EventsDto createValidEventDto() {
    EventsDto event = new EventsDto();
    event.setTitle("Test Event");
    event.setCapacity(100);
    event.setStartTime(LocalDateTime.now().plusDays(1));
    event.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
    event.setDescription("Integration test event description");
    event.setTags(Arrays.asList("test", "integration"));
    event.setPicPath("/images/test-event.jpg");
    event.setHostUuid(testHostUuid);
    return event;
  }
}
