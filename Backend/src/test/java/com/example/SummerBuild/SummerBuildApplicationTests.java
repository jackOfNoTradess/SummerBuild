package com.example.SummerBuild;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
      "supabase.jwt.secret=c3VwZXItc2VjcmV0LWp3dC10b2tlbi13aXRoLWF0LWxlYXN0LTMyLWNoYXJhY3RlcnMtbG9uZw==",
      "supabase.auth.url=http://localhost:54321",
      "supabase.anonKey=test-anon-key",
      "supabase.serviceKey=test-service-key"
    })
public class SummerBuildApplicationTests {
  @Test
  public void contextLoads() {}
}
