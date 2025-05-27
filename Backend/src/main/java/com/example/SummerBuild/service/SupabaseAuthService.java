package com.example.SummerBuild.service;

import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.User;
import com.example.SummerBuild.model.UserRole;
import com.example.SummerBuild.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SupabaseAuthService {

  @Autowired private UserRepository userRepository;
  private final RestTemplate restTemplate;
  private static final Logger logger = LoggerFactory.getLogger(SupabaseAuthService.class);

  @Value("${supabase.auth.url}")
  private String supabaseUrl;

  @Value("${supabase.anonKey}")
  private String supabaseApiKey;

  public SupabaseAuthService() {
    this.restTemplate = new RestTemplate();
  }

  public ResponseEntity<String> signup(
      String email, String password, String displayName, UserRole role, Gender gender) {

    // inserting into authentication table
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("apikey", supabaseApiKey);

    String signupUrl = supabaseUrl + "/auth/v1/signup";

    Map<String, Object> body = new HashMap<>();
    body.put("email", email);
    body.put("password", password);

    Map<String, Object> userMetadata = new HashMap<>();
    userMetadata.put("display_name", displayName);
    body.put("data", userMetadata);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(signupUrl, HttpMethod.POST, request, String.class);

    // inserting into database table for unique role and gender
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonNode;
    try {
      jsonNode = objectMapper.readTree(response.getBody());
    } catch (JsonProcessingException e) {
      logger.error("Error processing user for unique role and gender: ", e.getMessage());
      throw new RuntimeException("Failed to parse Supabase signup response", e);
    }
    UUID uid = UUID.fromString(jsonNode.get("user").get("id").asText());

    User user = User.builder().userUuid(uid).role(role).gender(gender).build();
    userRepository.save(user);

    return response;
  }

  public ResponseEntity<String> login(String email, String password) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("apikey", supabaseApiKey);
    headers.set("Authorization", "Bearer " + supabaseApiKey);

    String loginUrl = supabaseUrl + "/auth/v1/token?grant_type=password";

    Map<String, String> body = new HashMap<>();
    body.put("email", email);
    body.put("password", password);

    HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

    return restTemplate.exchange(loginUrl, HttpMethod.POST, request, String.class);
  }
}
