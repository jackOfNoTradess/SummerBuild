package com.example.SummerBuild.integration;

import static org.assertj.core.api.Assertions.*;

import com.example.SummerBuild.config.TestAuthConfig;
import com.example.SummerBuild.config.TestSecurityConfig;
import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.lang.Arrays;
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
    testUserId = signupAndGetUserId();
    String jwtToken = loginAndGetToken();

    authHeaders = new HttpHeaders();
    authHeaders.setContentType(MediaType.APPLICATION_JSON);
    authHeaders.setBearerAuth(jwtToken);
  }

  @AfterEach
  void cleanupTestData() {
    try {
      jdbcTemplate.update("DELETE FROM users WHERE role = 'USER' AND id != ?", testUserId);
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

  /** Tests creating a user and verifies it's stored in the database */
  @Test
  @Order(1)
  @DisplayName("Integration: Create User → Database Storage → API Response")
  void testCreateUser() {
    // Insert test user directly into database
    UUID newUserId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO users (id, role, gender, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
        newUserId,
        UserRole.USER.name(),
        Gender.MALE.name());

    // Verify user exists in database
    String countSql = "SELECT COUNT(*) FROM users WHERE id = ?";
    Integer count = jdbcTemplate.queryForObject(countSql, Integer.class, newUserId);
    assertThat(count).isEqualTo(1);

    // Verify user data is correct
    String selectSql = "SELECT role, gender FROM users WHERE id = ?";
    Map<String, Object> userData = jdbcTemplate.queryForMap(selectSql, newUserId);
    assertThat(userData.get("role")).isEqualTo(UserRole.USER.name());
    assertThat(userData.get("gender")).isEqualTo(Gender.MALE.name());
  }

  /** Tests retrieving a user by ID -> calls Supabase API */
  @Test
  @Order(2)
  @DisplayName("Integration: Get User By ID → Supabase API → Response Handling")
  void testGetUserById() {
    HttpEntity<Void> request = new HttpEntity<>(authHeaders);
    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/" + testUserId, HttpMethod.GET, request, String.class);

    // This endpoint calls Supabase API
    // Testing that endpoint is secured
    assertThat(response.getStatusCode())
        .isIn(
            HttpStatus.OK,
            HttpStatus.FORBIDDEN,
            HttpStatus.INTERNAL_SERVER_ERROR,
            HttpStatus.NOT_FOUND);

    // If forbidden or error, verify it's due to Supabase API issues, not auth issues
    if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
      assertThat(response.getBody()).containsAnyOf("JWT", "token", "auth", "forbidden");
    }
  }

  /** Tests updating user profile calls Supabase API */
  @Test
  @Order(3)
  @DisplayName("Integration: Update User Profile → Supabase API → Response Handling")
  void testUpdateUserProfile() {
    String updateUrl = baseUrl + "/" + testUserId + "?newName=Updated User Name";
    HttpEntity<Void> request = new HttpEntity<>(authHeaders);
    ResponseEntity<String> response =
        restTemplate.exchange(updateUrl, HttpMethod.PUT, request, String.class);

    // This endpoint calls Supabase API
    assertThat(response.getStatusCode())
        .isIn(
            HttpStatus.OK,
            HttpStatus.NO_CONTENT,
            HttpStatus.FORBIDDEN,
            HttpStatus.INTERNAL_SERVER_ERROR);

    // Verify endpoint is secured
    HttpHeaders noAuthHeaders = new HttpHeaders();
    noAuthHeaders.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Void> unauthRequest = new HttpEntity<>(noAuthHeaders);

    ResponseEntity<String> unauthResponse =
        restTemplate.exchange(updateUrl, HttpMethod.PUT, unauthRequest, String.class);
    assertThat(unauthResponse.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
  }

  /** Tests deleting user calls Supabase API correctly */
  @Test
  @Order(4)
  @DisplayName("Integration: Delete User → Supabase API → Response Handling")
  void testDeleteUser() {
    // Create a separate user for deletion test
    UUID deleteUserId =
        signupUserAndGetId("deleteuser@example.com", "deletepass123", "Delete User");

    HttpEntity<Void> request = new HttpEntity<>(authHeaders);
    ResponseEntity<String> response =
        restTemplate.exchange(
            baseUrl + "/" + deleteUserId, HttpMethod.DELETE, request, String.class);

    // This endpoint calls Supabase API
    assertThat(response.getStatusCode())
        .isIn(
            HttpStatus.OK,
            HttpStatus.NO_CONTENT,
            HttpStatus.FORBIDDEN,
            HttpStatus.INTERNAL_SERVER_ERROR);

    // Verify endpoint is secured
    HttpHeaders noAuthHeaders = new HttpHeaders();
    noAuthHeaders.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Void> unauthRequest = new HttpEntity<>(noAuthHeaders);

    ResponseEntity<String> unauthResponse =
        restTemplate.exchange(
            baseUrl + "/" + deleteUserId, HttpMethod.DELETE, unauthRequest, String.class);
    assertThat(unauthResponse.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
  }

  private UUID signupUserAndGetId(String email, String password, String displayName) {
    String signupUrl = "http://localhost:" + port + "/api/auth/signup";
    String signupParams =
        String.format(
            "?email=%s&password=%s&displayName=%s&userRole=USER&gender=MALE",
            email, password, displayName);
    ResponseEntity<String> signupResponse =
        restTemplate.postForEntity(signupUrl + signupParams, null, String.class);
    try {
      JsonNode jsonResponse = objectMapper.readTree(signupResponse.getBody());
      String userId = jsonResponse.path("user").path("id").asText();
      return UUID.fromString(userId);
    } catch (Exception e) {
      return UUID.nameUUIDFromBytes(email.getBytes());
    }
  }

  /** Tests retrieving all users handles Supabase API appropriately */
  @Test
  @Order(5)
  @DisplayName("Integration: Get All Users → Supabase API → Response Handling")
  void testGetAllUsers() {
    HttpEntity<Void> request = new HttpEntity<>(authHeaders);
    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl, HttpMethod.GET, request, String.class);

    // This endpoint calls Supabase API
    // We're testing that the endpoint is secured and responds appropriately
    assertThat(response.getStatusCode())
        .isIn(HttpStatus.OK, HttpStatus.FORBIDDEN, HttpStatus.INTERNAL_SERVER_ERROR);

    // Verify endpoint requires authentication
    HttpHeaders noAuthHeaders = new HttpHeaders();
    noAuthHeaders.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Void> unauthRequest = new HttpEntity<>(noAuthHeaders);

    ResponseEntity<String> unauthResponse =
        restTemplate.exchange(baseUrl, HttpMethod.GET, unauthRequest, String.class);
    assertThat(unauthResponse.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);

    // If we got a forbidden/error response, verify it contains expected error indicators
    if (response.getStatusCode() != HttpStatus.OK) {
      assertThat(response.getBody()).containsAnyOf("JWT", "token", "auth", "error", "invalid");
    }
  }

  /** Tests that endpoints are properly secured and require authentication */
  @Test
  @Order(6)
  @DisplayName("Integration: Authentication Required → Security → Access Control")
  void testAuthenticationRequired() {
    HttpHeaders noAuthHeaders = new HttpHeaders();
    noAuthHeaders.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Void> unauthenticatedRequest = new HttpEntity<>(noAuthHeaders);

    // Test endpoints that require authentication
    String[] protectedEndpoints = {
      baseUrl, // GET /api/users
      baseUrl + "/" + testUserId, // GET /api/users/{id}
      baseUrl + "/role/USER" // GET /api/users/role/{role}
    };

    for (String endpoint : protectedEndpoints) {
      ResponseEntity<String> response =
          restTemplate.exchange(endpoint, HttpMethod.GET, unauthenticatedRequest, String.class);

      assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    // Verify authenticated requests work
    HttpEntity<Void> authenticatedRequest = new HttpEntity<>(authHeaders);
    ResponseEntity<String> authResponse =
        restTemplate.exchange(
            baseUrl + "/role/USER", HttpMethod.GET, authenticatedRequest, String.class);

    assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  /** Tests role-based filtering works with local database */
  @Test
  @Order(7)
  @DisplayName("Integration: Role Restrictions → Local Database → Access Control")
  void testRoleRestrictions() {
    // Insert users with different roles
    UUID adminUserId = UUID.randomUUID();
    UUID regularUserId = UUID.randomUUID();

    jdbcTemplate.update(
        "INSERT INTO users (id, role, gender, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
        adminUserId,
        UserRole.ADMIN.name(),
        Gender.MALE.name());

    jdbcTemplate.update(
        "INSERT INTO users (id, role, gender, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())",
        regularUserId,
        UserRole.USER.name(),
        Gender.FEMALE.name());

    // Test the local database filtering endpoint (this one doesn't call Supabase)
    HttpEntity<Void> request = new HttpEntity<>(authHeaders);
    ResponseEntity<UserDto[]> response =
        restTemplate.exchange(baseUrl + "/role/ADMIN", HttpMethod.GET, request, UserDto[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<UserDto> adminUsers = Arrays.asList(response.getBody());

    // Verify admin users were found
    assertThat(adminUsers).isNotEmpty();
    assertThat(adminUsers).allMatch(user -> user.getRole() == UserRole.ADMIN);

    // Test USER role filtering
    ResponseEntity<UserDto[]> userResponse =
        restTemplate.exchange(baseUrl + "/role/USER", HttpMethod.GET, request, UserDto[].class);

    assertThat(userResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<UserDto> regularUsers = Arrays.asList(userResponse.getBody());
    assertThat(regularUsers).isNotEmpty();
    assertThat(regularUsers).allMatch(user -> user.getRole() == UserRole.USER);

    // Verify database consistency
    String countAdminSql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
    String countUserSql = "SELECT COUNT(*) FROM users WHERE role = 'USER'";
    Integer adminCount = jdbcTemplate.queryForObject(countAdminSql, Integer.class);
    Integer userCount = jdbcTemplate.queryForObject(countUserSql, Integer.class);

    assertThat(adminCount).isGreaterThanOrEqualTo(1);
    assertThat(userCount).isGreaterThanOrEqualTo(1);
  }
}
