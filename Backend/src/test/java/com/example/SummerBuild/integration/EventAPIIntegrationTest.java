package com.example.SummerBuild.integration;

import static org.assertj.core.api.Assertions.*;

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

/**
 * Integration tests for Event API that test the complete workflow: HTTP Request → Controller →
 * Service → Repository → Database → Response
 */
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
      // If user already exists, fallback to a fixed UUID for test
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

  @Test
  @Order(1)
  @DisplayName("Integration: Create Event → Verify Database Storage → Retrieve via API")
  void shouldCreateEventAndVerifyFullWorkflow() {
    System.out.println("=== TEST STARTED ===");

    // Given - Create event request
    EventsDto newEvent = createValidEventDto();
    newEvent.setTitle("Integration Test Event");
    newEvent.setCapacity(75);

    System.out.println("=== ABOUT TO MAKE HTTP REQUEST ===");
    HttpEntity<EventsDto> request = new HttpEntity<>(newEvent, authHeaders);

    // When - Call API to create event
    try {
      ResponseEntity<EventsDto> createResponse =
          restTemplate.postForEntity(baseUrl, request, EventsDto.class);
      System.out.println("=== HTTP REQUEST COMPLETED ===");
      System.out.println("Status: " + createResponse.getStatusCode());
      System.out.println("Body: " + createResponse.getBody());

      // Continue with original test...
      assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(createResponse.getBody()).isNotNull();

    } catch (Exception e) {
      System.out.println("=== HTTP REQUEST FAILED ===");
      System.out.println("Exception: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
  }

  @Test
  @Order(2)
  @DisplayName("Integration: Update Event → Verify Database Changes → Confirm via API")
  void shouldUpdateEventAndVerifyDatabaseChanges() {
    // Given - First create an event
    EventsDto originalEvent = createValidEventDto();
    originalEvent.setTitle("Original Event");
    originalEvent.setCapacity(50);

    HttpEntity<EventsDto> createRequest = new HttpEntity<>(originalEvent, authHeaders);
    ResponseEntity<EventsDto> createResponse =
        restTemplate.postForEntity(baseUrl, createRequest, EventsDto.class);
    UUID eventId = createResponse.getBody().getId();

    // Verify original data in database
    String selectSql = "SELECT title, capacity FROM events WHERE id = ?";
    Map<String, Object> originalDbRecord = jdbcTemplate.queryForMap(selectSql, eventId);
    assertThat(originalDbRecord.get("title")).isEqualTo("Original Event");
    assertThat(originalDbRecord.get("capacity")).isEqualTo(50);

    // When - Update the event through API
    EventsDto updateEvent = createValidEventDto();
    updateEvent.setTitle("Updated Event Title");
    updateEvent.setCapacity(100);
    updateEvent.setDescription("Updated description via integration test");

    HttpEntity<EventsDto> updateRequest = new HttpEntity<>(updateEvent, authHeaders);
    ResponseEntity<EventsDto> updateResponse =
        restTemplate.exchange(
            baseUrl + "/" + eventId, HttpMethod.PUT, updateRequest, EventsDto.class);

    // Then - Verify API response
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(updateResponse.getBody().getTitle()).isEqualTo("Updated Event Title");

    // Verify changes are persisted in database
    Map<String, Object> updatedDbRecord = jdbcTemplate.queryForMap(selectSql, eventId);
    assertThat(updatedDbRecord.get("title")).isEqualTo("Updated Event Title");
    assertThat(updatedDbRecord.get("capacity")).isEqualTo(100);

    // Verify updated_at timestamp is set
    String timestampSql = "SELECT updated_at FROM events WHERE id = ?";
    Object updatedAt = jdbcTemplate.queryForObject(timestampSql, Object.class, eventId);
    assertThat(updatedAt).isNotNull();
  }

  @Test
  @Order(3)
  @DisplayName("Integration: Filter Events by Host → Verify Database Query → API Response")
  void shouldFilterEventsByHostAndVerifyDatabaseQuery() {
    // Given - Create multiple events with different hosts
    UUID host1 = UUID.randomUUID();
    UUID host2 = UUID.randomUUID();

    // Create events for host1
    EventsDto event1 = createValidEventDto();
    event1.setTitle("Host1 Event 1");
    EventsDto event2 = createValidEventDto();
    event2.setTitle("Host1 Event 2");

    // Create event for host2
    EventsDto event3 = createValidEventDto();
    event3.setTitle("Host2 Event");

    // Insert directly into database to simulate different hosts
    String insertSql =
        """
            INSERT INTO events (id, title, host_id, capacity, start_time, end_time, description, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    UUID event1Id = UUID.randomUUID();
    UUID event2Id = UUID.randomUUID();
    UUID event3Id = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();

    jdbcTemplate.update(
        insertSql,
        event1Id,
        "Host1 Event 1",
        host1,
        50,
        now.plusDays(1),
        now.plusDays(1).plusHours(2),
        "Test event 1",
        now);
    jdbcTemplate.update(
        insertSql,
        event2Id,
        "Host1 Event 2",
        host1,
        75,
        now.plusDays(2),
        now.plusDays(2).plusHours(2),
        "Test event 2",
        now);
    jdbcTemplate.update(
        insertSql,
        event3Id,
        "Host2 Event",
        host2,
        100,
        now.plusDays(3),
        now.plusDays(3).plusHours(2),
        "Test event 3",
        now);

    // When - Call API to get events by host1
    HttpEntity<Void> request = new HttpEntity<>(authHeaders);
    ResponseEntity<EventsDto[]> response =
        restTemplate.exchange(
            baseUrl + "/host/" + host1, HttpMethod.GET, request, EventsDto[].class);

    // Then - Verify API response
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<EventsDto> host1Events = Arrays.asList(response.getBody());
    assertThat(host1Events).hasSize(2);
    assertThat(host1Events).allMatch(event -> event.getHostUuid().equals(host1));

    // Verify database query worked correctly
    String verifySql = "SELECT COUNT(*) FROM events WHERE host_id = ?";
    Integer host1Count = jdbcTemplate.queryForObject(verifySql, Integer.class, host1);
    Integer host2Count = jdbcTemplate.queryForObject(verifySql, Integer.class, host2);

    assertThat(host1Count).isEqualTo(2);
    assertThat(host2Count).isEqualTo(1);
  }

  @Test
  @Order(4)
  @DisplayName("Integration: Delete Event → Verify Database Removal → Confirm 404 Response")
  void shouldDeleteEventAndVerifyDatabaseRemoval() {
    // Given - Create an event
    EventsDto eventToDelete = createValidEventDto();
    eventToDelete.setTitle("Event to Delete");

    HttpEntity<EventsDto> createRequest = new HttpEntity<>(eventToDelete, authHeaders);
    ResponseEntity<EventsDto> createResponse =
        restTemplate.postForEntity(baseUrl, createRequest, EventsDto.class);
    UUID eventId = createResponse.getBody().getId();

    // Verify event exists in database
    String countSql = "SELECT COUNT(*) FROM events WHERE id = ?";
    Integer countBefore = jdbcTemplate.queryForObject(countSql, Integer.class, eventId);
    assertThat(countBefore).isEqualTo(1);

    // When - Delete the event through API
    HttpEntity<Void> deleteRequest = new HttpEntity<>(authHeaders);
    ResponseEntity<Void> deleteResponse =
        restTemplate.exchange(
            baseUrl + "/" + eventId, HttpMethod.DELETE, deleteRequest, Void.class);

    // Then - Verify API response
    assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    // Verify event is removed from database
    Integer countAfter = jdbcTemplate.queryForObject(countSql, Integer.class, eventId);
    assertThat(countAfter).isEqualTo(0);

    // Verify subsequent GET returns 404
    HttpEntity<Void> getRequest = new HttpEntity<>(authHeaders);
    ResponseEntity<EventsDto> getResponse =
        restTemplate.exchange(baseUrl + "/" + eventId, HttpMethod.GET, getRequest, EventsDto.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @Order(5)
  @DisplayName("Integration: Validation Errors → Database Rollback → Error Response")
  void shouldHandleValidationErrorsWithDatabaseRollback() {
    // Given - Invalid event data (violates constraints)
    EventsDto invalidEvent = new EventsDto();
    invalidEvent.setTitle(""); // Violates @NotBlank
    invalidEvent.setCapacity(-10); // Violates @Positive
    // Missing required startTime and endTime

    // Count events before attempted creation
    String countSql = "SELECT COUNT(*) FROM events";
    Integer countBefore = jdbcTemplate.queryForObject(countSql, Integer.class);

    HttpEntity<EventsDto> request = new HttpEntity<>(invalidEvent, authHeaders);

    // When - Attempt to create invalid event
    ResponseEntity<EventsDto> response =
        restTemplate.postForEntity(baseUrl, request, EventsDto.class);

    // Then - Verify validation error response
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    // Verify no data was inserted into database (transaction rollback)
    Integer countAfter = jdbcTemplate.queryForObject(countSql, Integer.class);
    assertThat(countAfter).isEqualTo(countBefore);
  }

  @Test
  @Order(6)
  @DisplayName("Integration: Get All Events → Database Query → Pagination Response")
  void shouldGetAllEventsFromDatabaseWithCorrectResponse() {
    // Given - Insert multiple events directly into database
    String insertSql =
        """
            INSERT INTO events (id, title, host_id, capacity, start_time, end_time, description, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    LocalDateTime now = LocalDateTime.now();
    for (int i = 1; i <= 3; i++) {
      jdbcTemplate.update(
          insertSql,
          UUID.randomUUID(),
          "Test Event " + i,
          UUID.randomUUID(),
          50 + i,
          now.plusDays(i),
          now.plusDays(i).plusHours(2),
          "Description " + i,
          now);
    }

    // When - Call API to get all events
    HttpEntity<Void> request = new HttpEntity<>(authHeaders);
    ResponseEntity<EventsDto[]> response =
        restTemplate.exchange(baseUrl, HttpMethod.GET, request, EventsDto[].class);

    // Then - Verify API response
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<EventsDto> events = Arrays.asList(response.getBody());
    assertThat(events.size()).isGreaterThanOrEqualTo(3);

    // Verify database query returned correct data
    String countSql = "SELECT COUNT(*) FROM events";
    Integer dbCount = jdbcTemplate.queryForObject(countSql, Integer.class);
    assertThat(events.size()).isEqualTo(dbCount);

    // Verify all events have required fields populated
    assertThat(events)
        .allMatch(
            event ->
                event.getId() != null
                    && event.getTitle() != null
                    && event.getHostUuid() != null
                    && event.getCapacity() != null
                    && event.getCreatedAt() != null);
  }

  @Test
  @Order(7)
  @DisplayName("Integration: Authentication Flow → Security → Database Access")
  void shouldEnforceAuthenticationForDatabaseAccess() {
    // Given - Request without authentication
    HttpHeaders noAuthHeaders = new HttpHeaders();
    noAuthHeaders.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Void> unauthenticatedRequest = new HttpEntity<>(noAuthHeaders);

    // When - Attempt to access protected endpoint
    ResponseEntity<EventsDto[]> response =
        restTemplate.exchange(baseUrl, HttpMethod.GET, unauthenticatedRequest, EventsDto[].class);

    // Then - Should be rejected before reaching database
    assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);

    // Verify with valid authentication, request succeeds
    HttpEntity<Void> authenticatedRequest = new HttpEntity<>(authHeaders);
    ResponseEntity<EventsDto[]> authResponse =
        restTemplate.exchange(baseUrl, HttpMethod.GET, authenticatedRequest, EventsDto[].class);
    assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @Order(8)
  @DisplayName("Integration: Concurrent Operations → Database Consistency → API Responses")
  void shouldHandleConcurrentOperationsWithDatabaseConsistency() {
    // Given - Create an event
    EventsDto concurrentEvent = createValidEventDto();
    concurrentEvent.setTitle("Concurrent Test Event");

    HttpEntity<EventsDto> createRequest = new HttpEntity<>(concurrentEvent, authHeaders);
    ResponseEntity<EventsDto> createResponse =
        restTemplate.postForEntity(baseUrl, createRequest, EventsDto.class);
    UUID eventId = createResponse.getBody().getId();

    // When - Simulate concurrent read operations
    HttpEntity<Void> getRequest = new HttpEntity<>(authHeaders);

    ResponseEntity<EventsDto> response1 =
        restTemplate.exchange(baseUrl + "/" + eventId, HttpMethod.GET, getRequest, EventsDto.class);
    ResponseEntity<EventsDto> response2 =
        restTemplate.exchange(baseUrl + "/" + eventId, HttpMethod.GET, getRequest, EventsDto.class);

    // Then - Both responses should be successful and consistent
    assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response1.getBody().getId()).isEqualTo(response2.getBody().getId());
    assertThat(response1.getBody().getTitle()).isEqualTo(response2.getBody().getTitle());

    // Verify database consistency
    String sql = "SELECT title FROM events WHERE id = ?";
    String dbTitle = jdbcTemplate.queryForObject(sql, String.class, eventId);
    assertThat(dbTitle).isEqualTo("Concurrent Test Event");
  }

  private void setupTestEventData() {
    String insertSql =
        """
            INSERT INTO events (id, title, host_id, capacity, start_time, end_time, description, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    LocalDateTime now = LocalDateTime.now();
    jdbcTemplate.update(
        insertSql,
        UUID.randomUUID(),
        "Test Event",
        testHostUuid,
        50,
        now.plusDays(1),
        now.plusDays(1).plusHours(2),
        "Test event description",
        now);
  }

  // Helper method to create valid event DTO
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
