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
  void testAccessWithoutToken() {
    // Test accessing protected endpoint without authorization token fails
    ResponseEntity<String> response =
        restTemplate.getForEntity(baseUrl + "/api/users", String.class);

    assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
  }

  @Test
  @Order(2)
  void testAccessWithInvalidToken() {
    // Test accessing protected endpoint with malformed JWT token fails
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth("invalid.jwt.token");
    HttpEntity<String> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/api/users", HttpMethod.GET, entity, String.class);

    assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.UNAUTHORIZED);
  }

  private String performSignup(
      String email, String password, String displayName, String userRole, String gender)
      throws Exception {
    String signupUrl = baseUrl + "/api/auth/signup";
    String signupParams =
        String.format(
            "?email=%s&password=%s&displayName=%s&userRole=%s&gender=%s",
            email, password, displayName, userRole, gender);

    ResponseEntity<String> response =
        restTemplate.postForEntity(signupUrl + signupParams, null, String.class);

    assertThat(response.getStatusCode())
        .satisfiesAnyOf(
            status -> assertThat(status).isEqualTo(HttpStatus.OK),
            status -> assertThat(status).isEqualTo(HttpStatus.CREATED));

    JsonNode responseBody = objectMapper.readTree(response.getBody());
    return responseBody.get("access_token").asText();
  }
}
