package com.example.SummerBuild.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.SummerBuild.config.TestAuthConfig;
import com.example.SummerBuild.config.TestSecurityConfig;
import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.mapper.UserMapper;
import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.User;
import com.example.SummerBuild.model.UserRole;
import com.example.SummerBuild.repository.UserRepository;
import com.example.SummerBuild.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({ TestAuthConfig.class, TestSecurityConfig.class })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(locations = "classpath:application-test.properties")
public class UserAPIIntegrationTest {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;
  @Autowired
  private UserRepository userRepository;
  @MockBean
  private UserService userService;
  @Autowired
  private UserMapper userMapper;

  @Value("${supabase.jwt.secret}")
  private String jwt.secret;

  private String baseUrl;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private String jwtToken;

  @BeforeAll
  void setup() {
    baseUrl = "http://localhost:" + port + "/api/users";
    jwtToken = generateJwtToken();
  }

  @BeforeEach
  void seedTestData() {
    // Clear existing data
    userRepository.deleteAll();

    // Create test users
    userRepository.saveAll(
        List.of(
            // User Role
            User.builder().id(UUID.randomUUID()).role(UserRole.USER).gender(Gender.MALE).build(),

            // Admin Role
            User.builder()
                .id(UUID.randomUUID())
                .role(UserRole.ADMIN)
                .gender(Gender.FEMALE)
                .build()));

    // Set up UserService mock
    when(userService.getUserById(any(UUID.class)))
        .thenAnswer(
            invocation -> {
              UUID id = invocation.getArgument(0);
              return userRepository
                  .findById(id)
                  .map(
                      user -> {
                        String response = String.format(
                            "{\"id\":\"%s\",\"role\":\"%s\",\"gender\":\"%s\"}",
                            user.getId(), user.getRole(), user.getGender());
                        return ResponseEntity.ok(response);
                      })
                  .orElse(ResponseEntity.notFound().build());
            });
    // Mock findByRole to return users with the given role as DTOs
    when(userService.findByRole(any(UserRole.class)))
        .thenAnswer(
            invocation -> {
              UserRole role = invocation.getArgument(0);
              return userRepository.findAll().stream()
                  .filter(user -> user.getRole() == role)
                  .map(userMapper::toDto)
                  .toList();
            });
  }

  @Test
  @Order(1)
  void returnUsersByRole() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwtToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    ResponseEntity<UserDto[]> response = restTemplate.exchange(baseUrl + "/role/USER", HttpMethod.GET, entity,
        UserDto[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()[0].getRole()).isEqualTo(UserRole.USER);
    assertThat(response.getBody()).hasSize(1);
  }

  @Test
  @Order(2)
  void whenRoleNotFound() {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwtToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/role/MODERATOR", HttpMethod.GET, entity,
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  @Order(3)
  void getUserById() {
    UUID userId = UUID.randomUUID();
    User testUser = User.builder().id(userId).role(UserRole.USER).gender(Gender.MALE).build();
    userRepository.save(testUser);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(jwtToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);
    ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/" + userId, HttpMethod.GET, entity,
        String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).contains(userId.toString());
  }

  private String generateJwtToken() {
    try {
      byte[] decoded = java.util.Base64.getDecoder().decode(jwt.secret);
      java.security.Key key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(decoded);
      java.util.Map<String, Object> claims = new java.util.HashMap<>();
      claims.put("sub", java.util.UUID.randomUUID().toString());
      claims.put("email", "test@example.com");
      claims.put("role", "ADMIN");
      return io.jsonwebtoken.Jwts.builder()
          .setClaims(claims)
          .setIssuedAt(new java.util.Date())
          .setExpiration(new java.util.Date(System.currentTimeMillis() + 3600000))
          .signWith(key)
          .compact();
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate JWT token", e);
    }
  }
}
