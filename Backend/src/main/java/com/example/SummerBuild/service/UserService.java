package com.example.SummerBuild.service;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.mapper.UserMapper;
import com.example.SummerBuild.model.UserRole;
import com.example.SummerBuild.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private static final Logger logger = LoggerFactory.getLogger(UserService.class);

  @Value("${supabase.service.key}")
  private String service.key;

  @Value("${supabase.auth.url}")
  private String supabaseUrl;

  private final RestTemplate restTemplate = new RestTemplate();

  @ResponseStatus(HttpStatus.NOT_FOUND)
  public static class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
      super(message);
    }
  }

  @ResponseStatus(HttpStatus.CONFLICT)
  public static class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
      super(message);
    }
  }

  @Transactional(readOnly = true)
  public List<UserDto> findAll() {
    return userRepository.findAll().stream().map(userMapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public UserDto findById(UUID id) {
    return userRepository
        .findById(id)
        .map(userMapper::toDto)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
  }

  @Transactional
  public void delete(UUID id) {
    if (!userRepository.existsById(id)) {
      throw new ResourceNotFoundException("User not found with id: " + id);
    }
    userRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<UserDto> findByRole(UserRole role) {
    return userRepository.findByRole(role).stream().map(userMapper::toDto).toList();
  }

  // transactional decorator not needed cause its a http request to supabase, not
  // a direct database action
  public ResponseEntity<String> getAllUsers() {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set("apikey", service.key);
      headers.set("Authorization", "Bearer " + service.key);
      headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

      HttpEntity<String> entity = new HttpEntity<>(headers);
      String url = supabaseUrl + "/auth/v1/admin/users";

      return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      logger.error("Errors in Http Client: ", e);
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    } catch (RestClientException e) {
      logger.error("Errors in Rest Client: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error retrieving users: " + e.getMessage());
    }
  }

  // transactional decorator not needed cause its a http request to supabase, not
  // a direct database action
  public ResponseEntity<String> getUserById(UUID userId) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set("apikey", service.key);
      headers.set("Authorization", "Bearer " + service.key);
      headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

      HttpEntity<String> entity = new HttpEntity<>(headers);
      String url = supabaseUrl + "/auth/v1/admin/users/" + userId;

      return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      logger.error("Errors in Http Client: ", e);
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    } catch (RestClientException e) {
      logger.error("Errors in Rest Client: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error retrieving users: " + e.getMessage());
    }
  }

  // transactional decorator not needed cause its a http request to supabase, not
  // a direct database action
  public ResponseEntity<String> deleteUserById(UUID userId) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set("apikey", service.key);
      headers.set("Authorization", "Bearer " + service.key);
      headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

      HttpEntity<String> entity = new HttpEntity<>(headers);
      String url = supabaseUrl + "/auth/v1/admin/users/" + userId;

      return restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      logger.error("Errors in Http Client: ", e);
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    } catch (RestClientException e) {
      logger.error("Errors in Rest Client: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error retrieving users: " + e.getMessage());
    }
  }

  // transactional decorator not needed cause its a http request to supabase, not
  // a direct database action
  public ResponseEntity<String> updateUserById(UUID userId, Map<String, Object> updates) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set("apikey", service.key);
      headers.set("Authorization", "Bearer " + service.key);
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updates, headers);
      String url = supabaseUrl + "/auth/v1/admin/users/" + userId;

      return restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
    } catch (HttpClientErrorException | HttpServerErrorException e) {
      logger.error("Errors in Http Client: ", e);
      return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
    } catch (RestClientException e) {
      logger.error("Errors in Rest Client: ", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error retrieving users: " + e.getMessage());
    }
  }
}
