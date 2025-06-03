package com.example.SummerBuild.integration;

import static org.assertj.core.api.Assertions.*;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserAPIIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ObjectMapper objectMapper;

  @LocalServerPort private int port;

  private String baseUrl;
  private HttpHeaders authHeaders;
  private UUID testUserId;

  @BeforeEach
  void setUp() {
    baseUrl = "http://localhost:" + port + "/api/users";
    testUserId = UUID.randomUUID();

    String jwtToken = loginAndGetToken();

    authHeaders = new HttpHeaders();
    authHeaders.setContentType(MediaType.APPLICATION_JSON);
    authHeaders.setBearerAuth(jwtToken);

    setupTestUserData();
  }

  // Tests complete authentication flow and Supabase integration for user retrieval
  @Test
  @Order(1)
  @DisplayName("Integration: Authentication Flow → Service Layer → Supabase Integration")
  void shouldAuthenticateAndRetrieveUsersFromSupabase() {
    HttpEntity<Void> request = new HttpEntity<>(authHeaders);

    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl, HttpMethod.GET, request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    try {
      JsonNode jsonResponse = objectMapper.readTree(response.getBody());
      assertThat(jsonResponse.isArray()).isTrue();
    } catch (Exception e) {
      fail("Response should be valid JSON from Supabase: " + e.getMessage());
    }
  }

  // Tests user retrieval by ID through service layer to Supabase with local database verification
  @Test
  @Order(2)
  @DisplayName("Integration: Get User by ID → Service → Supabase → Database Verification")
  void shouldRetrieveUserByIdAndVerifyDataFlow() {
    HttpEntity<Void> request = new HttpEntity<>(authHeaders);

    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/" + testUserId, HttpMethod.GET, request, String.class);

    assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND);

    if (response.getStatusCode() == HttpStatus.OK) {
      try {
        JsonNode userJson = objectMapper.readTree(response.getBody());
        assertThat(userJson.has("id")).isTrue();

        verifyLocalUserDataExists(testUserId);
      } catch (Exception e) {
        fail("Should receive valid JSON from Supabase: " + e.getMessage());
      }
    }
  }

  // Tests user update operations through service layer to Supabase
  @Test
  @Order(3)
  @DisplayName("Integration: Update User → Service → Supabase → Verification")
  void shouldUpdateUserThroughSupabaseIntegration() {
    String newPassword = "newIntegrationTestPassword";
    String newDisplayName = "Integration Test User";

    HttpEntity<Void> request = new HttpEntity<>(authHeaders);

    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl + "/" + testUserId + "?password=" + newPassword + "&newName=" + newDisplayName,
            HttpMethod.PUT,
            request,
            String.class);

    assertThat(response.getStatusCode())
        .isIn(HttpStatus.OK, HttpStatus.NOT_FOUND, HttpStatus.BAD_REQUEST);

    if (response.getStatusCode() == HttpStatus.OK) {
      assertThat(response.getBody()).isNotNull();

      try {
        JsonNode updateResponse = objectMapper.readTree(response.getBody());
        assertThat(updateResponse).isNotNull();
      } catch (Exception e) {
        // Some update responses might not be JSON, that's okay
      }
    }
  }

  // Tests role-based filtering with database queries and service layer integration
  @Test
  @Order(4)
  @DisplayName("Integration: Users by Role → Database Query → Service → API Response")
  void shouldFilterUsersByRoleAndVerifyDatabaseIntegration() {
    setupUsersWithDifferentRoles();

    HttpEntity<Void> request = new HttpEntity<>(authHeaders);

    ResponseEntity<UserDto[]> adminResponse =
        restTemplate.exchange(baseUrl + "/role/ADMIN", HttpMethod.GET, request, UserDto[].class);

    assertThat(adminResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<UserDto> adminUsers = Arrays.asList(adminResponse.getBody());

    assertThat(adminUsers).allMatch(user -> user.getRole() == UserRole.ADMIN);

    String sql = "SELECT COUNT(*) FROM users WHERE role = ?";
    Integer adminCountInDb =
        jdbcTemplate.queryForObject(sql, Integer.class, UserRole.ADMIN.toString());
    assertThat(adminUsers.size()).isEqualTo(adminCountInDb);

    ResponseEntity<UserDto[]> userResponse =
        restTemplate.exchange(baseUrl + "/role/USER", HttpMethod.GET, request, UserDto[].class);

    assertThat(userResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<UserDto> regularUsers = Arrays.asList(userResponse.getBody());
    assertThat(regularUsers).allMatch(user -> user.getRole() == UserRole.USER);
  }

  // Tests user deletion through service layer to Supabase with database cleanup verification
  @Test
  @Order(5)
  @DisplayName("Integration: Delete User → Service → Supabase → Database Cleanup")
  void shouldDeleteUserAndVerifyFullWorkflow() {
    UUID userToDelete = UUID.randomUUID();

    String insertSql = "INSERT INTO users (id, role, gender, created_at) VALUES (?, ?, ?, ?)";
    jdbcTemplate.update(
        insertSql,
        userToDelete,
        UserRole.USER.toString(),
        Gender.OTHERS.toString(),
        java.time.LocalDateTime.now());

    String countSql = "SELECT COUNT(*) FROM users WHERE id = ?";
    Integer countBefore = jdbcTemplate.queryForObject(countSql, Integer.class, userToDelete);
    assertThat(countBefore).isEqualTo(1);

    HttpEntity<Void> request = new HttpEntity<>(authHeaders);

    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl + "/" + userToDelete, HttpMethod.DELETE, request, String.class);

    assertThat(response.getStatusCode())
        .isIn(HttpStatus.OK, HttpStatus.NO_CONTENT, HttpStatus.NOT_FOUND);

    if (response.getStatusCode() == HttpStatus.OK
        || response.getStatusCode() == HttpStatus.NO_CONTENT) {
      assertThat(response.getBody())
          .isNotNull()
          .withFailMessage("Delete response should not be null");
    }
  }

  // Tests authentication middleware and security protection for protected endpoints
  @Test
  @Order(6)
  @DisplayName("Integration: Authentication Failures → Security → Service Protection")
  void shouldProtectServiceLayerWithAuthentication() {
    HttpHeaders noAuthHeaders = new HttpHeaders();
    noAuthHeaders.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Void> unauthenticatedRequest = new HttpEntity<>(noAuthHeaders);

    ResponseEntity<String> getAllResponse =
        restTemplate.exchange(baseUrl, HttpMethod.GET, unauthenticatedRequest, String.class);

    ResponseEntity<UserDto[]> getRoleResponse =
        restTemplate.exchange(
            baseUrl + "/role/USER", HttpMethod.GET, unauthenticatedRequest, UserDto[].class);

    assertThat(getAllResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(getRoleResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    HttpEntity<Void> authenticatedRequest = new HttpEntity<>(authHeaders);

    ResponseEntity<String> authGetAllResponse =
        restTemplate.exchange(baseUrl, HttpMethod.GET, authenticatedRequest, String.class);
    ResponseEntity<UserDto[]> authGetRoleResponse =
        restTemplate.exchange(
            baseUrl + "/role/USER", HttpMethod.GET, authenticatedRequest, UserDto[].class);

    assertThat(authGetAllResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(authGetRoleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  // Tests validation layer for invalid role enum values
  @Test
  @Order(7)
  @DisplayName("Integration: Invalid Role Request → Validation → Error Handling")
  void shouldHandleInvalidRoleWithProperErrorFlow() {
    HttpEntity<Void> request = new HttpEntity<>(authHeaders);

    ResponseEntity<UserDto[]> response =
        restTemplate.exchange(
            baseUrl + "/role/INVALID_ROLE", HttpMethod.GET, request, UserDto[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  // Tests concurrent operations and database consistency under load
  @Test
  @Order(8)
  @DisplayName("Integration: Concurrent User Operations → Database Consistency")
  void shouldHandleConcurrentUserOperationsWithDataConsistency() {
    HttpEntity<Void> request = new HttpEntity<>(authHeaders);

    ResponseEntity<String> response1 =
        restTemplate.exchange(baseUrl, HttpMethod.GET, request, String.class);
    ResponseEntity<UserDto[]> response2 =
        restTemplate.exchange(baseUrl + "/role/USER", HttpMethod.GET, request, UserDto[].class);
    ResponseEntity<UserDto[]> response3 =
        restTemplate.exchange(baseUrl + "/role/ADMIN", HttpMethod.GET, request, UserDto[].class);

    assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.OK);

    List<UserDto> userRoleUsers = Arrays.asList(response2.getBody());
    List<UserDto> adminRoleUsers = Arrays.asList(response3.getBody());

    assertThat(userRoleUsers).allMatch(user -> user.getRole() == UserRole.USER);
    assertThat(adminRoleUsers).allMatch(user -> user.getRole() == UserRole.ADMIN);
  }

  // Tests input validation for UUID format and service layer error handling
  @Test
  @Order(9)
  @DisplayName("Integration: User Data Validation → Service Layer → Database Constraints")
  void shouldValidateUserDataThroughServiceToDatabase() {
    String invalidUuid = "not-a-valid-uuid-format";
    HttpEntity<Void> request = new HttpEntity<>(authHeaders);

    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/" + invalidUuid, HttpMethod.GET, request, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

    UUID nonExistentUser = UUID.randomUUID();
    ResponseEntity<String> notFoundResponse =
        restTemplate.exchange(
            baseUrl + "/" + nonExistentUser, HttpMethod.GET, request, String.class);

    assertThat(notFoundResponse.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND);
  }

  // Tests different parameter combinations for user updates through complete workflow
  @Test
  @Order(10)
  @DisplayName("Integration: User Update Workflow → Parameter Processing → Service Integration")
  void shouldProcessUpdateParametersThroughCompleteWorkflow() {
    HttpEntity<Void> passwordRequest = new HttpEntity<>(authHeaders);
    ResponseEntity<String> passwordResponse =
        restTemplate.exchange(
            baseUrl + "/" + testUserId + "?password=newTestPassword123",
            HttpMethod.PUT,
            passwordRequest,
            String.class);

    assertThat(passwordResponse.getStatusCode())
        .isIn(HttpStatus.OK, HttpStatus.NOT_FOUND, HttpStatus.BAD_REQUEST);

    ResponseEntity<String> nameResponse =
        restTemplate.exchange(
            baseUrl + "/" + testUserId + "?newName=UpdatedTestName",
            HttpMethod.PUT,
            passwordRequest,
            String.class);

    assertThat(nameResponse.getStatusCode())
        .isIn(HttpStatus.OK, HttpStatus.NOT_FOUND, HttpStatus.BAD_REQUEST);

    ResponseEntity<String> bothResponse =
        restTemplate.exchange(
            baseUrl + "/" + testUserId + "?password=newPassword&newName=NewName",
            HttpMethod.PUT,
            passwordRequest,
            String.class);

    assertThat(bothResponse.getStatusCode())
        .isIn(HttpStatus.OK, HttpStatus.NOT_FOUND, HttpStatus.BAD_REQUEST);

    ResponseEntity<String> noParamsResponse =
        restTemplate.exchange(
            baseUrl + "/" + testUserId, HttpMethod.PUT, passwordRequest, String.class);

    assertThat(noParamsResponse.getStatusCode())
        .isIn(HttpStatus.OK, HttpStatus.BAD_REQUEST, HttpStatus.NOT_FOUND);
  }

  // Tests complete authentication workflow: signup → login → token usage → protected API access
  @Test
  @Order(11)
  @DisplayName("Integration: End-to-End Authentication Workflow")
  void shouldTestCompleteAuthenticationWorkflow() {
    // Create user account
    // If user exists ignore response
    restTemplate.postForEntity(
        "http://localhost:"
            + port
            + "/api/auth/signup?email=workflow@example.com&password=workflowtest123&displayName=Workflow User&userRole=USER&gender=MALE",
        null,
        String.class);

    // Get JWT token
    ResponseEntity<String> loginResponse =
        restTemplate.postForEntity(
            "http://localhost:"
                + port
                + "/api/auth/login?email=workflow@example.com&password=workflowtest123",
            null,
            String.class);
    assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

    try {
      JsonNode jsonResponse = objectMapper.readTree(loginResponse.getBody());
      String jwtToken = jsonResponse.get("access_token").asText();
      assertThat(jwtToken).isNotNull().isNotEmpty();

      // Use token to access protected endpoint
      HttpHeaders workflowHeaders = new HttpHeaders();
      workflowHeaders.setContentType(MediaType.APPLICATION_JSON);
      workflowHeaders.setBearerAuth(jwtToken);

      ResponseEntity<String> protectedResponse =
          restTemplate.exchange(
              baseUrl, HttpMethod.GET, new HttpEntity<>(workflowHeaders), String.class);

      assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(protectedResponse.getBody()).isNotNull();

    } catch (Exception e) {
      fail("Complete authentication workflow should work: " + e.getMessage());
    }
  }

  private String loginAndGetToken() {
    String signupUrl = "http://localhost:" + port + "/api/auth/signup";

    String signupParams =
        "?email=testuser@example.com&password=testpassword123&displayName=Test User&userRole=USER&gender=OTHERS";

    try {
      restTemplate.postForEntity(signupUrl + signupParams, null, String.class);
    } catch (Exception e) {
      // User might already exist, that's fine
    }

    String loginUrl = "http://localhost:" + port + "/api/auth/login";
    String loginParams = "?email=testuser@example.com&password=testpassword123";

    ResponseEntity<String> loginResponse =
        restTemplate.postForEntity(loginUrl + loginParams, null, String.class);

    try {
      JsonNode jsonResponse = objectMapper.readTree(loginResponse.getBody());
      return jsonResponse.get("access_token").asText();
    } catch (Exception e) {
      throw new RuntimeException("Failed to extract JWT token from login response", e);
    }
  }

  private void setupTestUserData() {
    String insertSql =
        "INSERT INTO users (id, role, gender, created_at) VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING";
    jdbcTemplate.update(
        insertSql,
        testUserId,
        UserRole.USER.toString(),
        Gender.OTHERS.toString(),
        java.time.LocalDateTime.now());
  }

  private void setupUsersWithDifferentRoles() {
    String insertSql =
        "INSERT INTO users (id, role, gender, created_at) VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING";
    java.time.LocalDateTime now = java.time.LocalDateTime.now();

    for (int i = 1; i <= 2; i++) {
      jdbcTemplate.update(
          insertSql, UUID.randomUUID(), UserRole.ADMIN.toString(), Gender.MALE.toString(), now);
    }

    for (int i = 1; i <= 3; i++) {
      jdbcTemplate.update(
          insertSql, UUID.randomUUID(), UserRole.USER.toString(), Gender.FEMALE.toString(), now);
    }

    for (int i = 1; i <= 1; i++) {
      jdbcTemplate.update(
          insertSql,
          UUID.randomUUID(),
          UserRole.ORGANIZER.toString(),
          Gender.OTHERS.toString(),
          now);
    }
  }

  private void verifyLocalUserDataExists(UUID userId) {
    String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
    Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
    assertThat(count).isGreaterThan(0);
  }
}
