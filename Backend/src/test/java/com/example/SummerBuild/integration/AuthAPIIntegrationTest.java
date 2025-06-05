package com.example.SummerBuild.integration;

import static org.assertj.core.api.Assertions.*;

import com.example.SummerBuild.config.TestAuthConfig;
import com.example.SummerBuild.config.TestSecurityConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
@ActiveProfiles("test")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import({TestAuthConfig.class, TestSecurityConfig.class})
class AuthAPIIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private ObjectMapper objectMapper;

  private String baseUrl;

  @BeforeEach
  void setUp() {
    baseUrl = "http://localhost:" + port;
  }

  @Test
  @Order(1)
  // Test to discover what signup actually returns
  // Returns: {"user": {"id": "string", "email": "string"}}
  // Fields:
  //      user.id: String (UUID format)
  //      user.email: String
  void testSignupResponseFormat() throws Exception {
    String signupUrl = baseUrl + "/api/auth/signup";
    String signupParams =
        "?email=debug@example.com&password=password123&displayName=Debug User&userRole=USER&gender=MALE";

    ResponseEntity<String> response =
        restTemplate.postForEntity(signupUrl + signupParams, null, String.class);

    System.out.println("=== SIGNUP RESPONSE DISCOVERY ===");
    System.out.println("Status: " + response.getStatusCode());
    System.out.println("Headers: " + response.getHeaders());
    System.out.println("Raw Body: " + response.getBody());

    // Try to parse as JSON to see the structure
    try {
      JsonNode responseBody = objectMapper.readTree(response.getBody());
      System.out.println("Parsed JSON: " + responseBody.toPrettyString());

      // List all fields
      StringBuilder fields = new StringBuilder("Available fields: ");
      responseBody.fieldNames().forEachRemaining(field -> fields.append(field).append(", "));
      System.out.println(fields.toString());

      // Check for token fields
      System.out.println("Has 'access_token': " + responseBody.has("access_token"));
      System.out.println("Has 'token': " + responseBody.has("token"));
      System.out.println("Has 'accessToken': " + responseBody.has("accessToken"));
      System.out.println("Has 'jwt': " + responseBody.has("jwt"));

    } catch (Exception e) {
      System.out.println("Response is not JSON: " + e.getMessage());
    }

    // Just assert success for now
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @Order(2)
  // Returns: {"access_token": "string"}
  // Fields:
  //      access_token: String (JWT token)
  void testLoginResponseFormat() throws Exception {
    // First do signup
    String email = "login-debug@example.com";
    String password = "password123";

    String signupUrl = baseUrl + "/api/auth/signup";
    String signupParams =
        String.format(
            "?email=%s&password=%s&displayName=Login Debug&userRole=USER&gender=FEMALE",
            email, password);
    restTemplate.postForEntity(signupUrl + signupParams, null, String.class);

    // Now test login
    String loginUrl = baseUrl + "/api/auth/login";
    String loginParams = String.format("?email=%s&password=%s", email, password);

    ResponseEntity<String> response =
        restTemplate.postForEntity(loginUrl + loginParams, null, String.class);

    System.out.println("=== LOGIN RESPONSE DISCOVERY ===");
    System.out.println("Status: " + response.getStatusCode());
    System.out.println("Headers: " + response.getHeaders());
    System.out.println("Raw Body: " + response.getBody());

    // Try to parse as JSON
    try {
      JsonNode responseBody = objectMapper.readTree(response.getBody());
      System.out.println("Parsed JSON: " + responseBody.toPrettyString());

      // List all fields
      StringBuilder fields = new StringBuilder("Available fields: ");
      responseBody.fieldNames().forEachRemaining(field -> fields.append(field).append(", "));
      System.out.println(fields.toString());

      // Check for token fields
      System.out.println("Has 'access_token': " + responseBody.has("access_token"));
      System.out.println("Has 'token': " + responseBody.has("token"));
      System.out.println("Has 'accessToken': " + responseBody.has("accessToken"));
      System.out.println("Has 'jwt': " + responseBody.has("jwt"));

    } catch (Exception e) {
      System.out.println("Response is not JSON: " + e.getMessage());
    }

    // Just assert success for now
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @Order(3)
  // Test accessing protected endpoint without authorization token fails
  void testAccessWithoutToken() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(baseUrl + "/api/users", String.class);

    assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
  }

  @Test
  @Order(4)
  // Test accessing protected endpoint with malformed JWT token fails
  void testAccessWithInvalidToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth("invalid.jwt.token");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/api/users", HttpMethod.GET, entity, String.class);

    assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
  }

  @Test
  @Order(5)
  void testSignupReturnsToken() throws Exception {
    String signupUrl = baseUrl + "/api/auth/signup";
    String signupParams =
        "?email=test@example.com&password=password123&displayName=Test User&userRole=USER&gender=MALE";

    ResponseEntity<String> response =
        restTemplate.postForEntity(signupUrl + signupParams, null, String.class);

    assertThat(response.getStatusCode())
        .satisfiesAnyOf(
            status -> assertThat(status).isEqualTo(HttpStatus.OK),
            status -> assertThat(status).isEqualTo(HttpStatus.CREATED));

    JsonNode responseBody = objectMapper.readTree(response.getBody());
    assertThat(responseBody).isNotNull();
    assertThat(responseBody.has("access_token")).isTrue();

    String token = responseBody.get("access_token").asText();
    assertThat(token).isNotNull().isNotEmpty();
  }

  @Test
  @Order(6)
  // Test user login after successful signup returns JWT token
  void testLoginWithValidCredentials() throws Exception {
    String email = "login-test@example.com";
    String password = "password123";
    performSignup(email, password, "Login Test User", "USER", "FEMALE");

    String loginUrl = baseUrl + "/api/auth/login";
    String loginParams = String.format("?email=%s&password=%s", email, password);

    ResponseEntity<String> response =
        restTemplate.postForEntity(loginUrl + loginParams, null, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode responseBody = objectMapper.readTree(response.getBody());
    assertThat(responseBody).isNotNull();
    assertThat(responseBody.has("access_token")).isTrue();

    String token = responseBody.get("access_token").asText();
    assertThat(token).isNotNull().isNotEmpty();
  }

  private void performSignup(
      String email, String password, String displayName, String userRole, String gender)
      throws Exception {
    String signupUrl = baseUrl + "/api/auth/signup";
    String signupParams =
        String.format(
            "?email=%s&password=%s&displayName=%s&userRole=%s&gender=%s",
            email, password, displayName, userRole, gender);

    ResponseEntity<String> response =
        restTemplate.postForEntity(signupUrl + signupParams, null, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    JsonNode responseBody = objectMapper.readTree(response.getBody());
    assertThat(responseBody.has("user")).isTrue();
    assertThat(responseBody.get("user").has("id")).isTrue();
  }
}
