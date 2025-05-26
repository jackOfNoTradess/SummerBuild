package com.example.SummerBuild.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SupabaseAuthService {

  private final RestTemplate restTemplate;

  @Value("${supabase.auth.url}")
  private String supabaseUrl;

  @Value("${supabase.anonKey}")
  private String supabaseApiKey;

  public SupabaseAuthService() {
    this.restTemplate = new RestTemplate();
  }

  public ResponseEntity<String> signup(String email, String password) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("apikey", supabaseApiKey);

    String signupUrl = supabaseUrl + "/auth/v1/signup";

    Map<String, Object> body = new HashMap<>();
    body.put("email", email);
    body.put("password", password);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    return restTemplate.exchange(signupUrl, HttpMethod.POST, request, String.class);
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
