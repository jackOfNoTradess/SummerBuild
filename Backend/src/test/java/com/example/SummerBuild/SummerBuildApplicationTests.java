package com.example.SummerBuild;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
      "SUPABASE_JWT_SECRET=c3VwZXItc2VjcmV0LWp3dC10b2tlbi13aXRoLWF0LWxlYXN0LTMyLWNoYXJhY3RlcnMtbG9uZw==",
      "SUPABASE_URL=http://localhost:54321",
      "SUPABASE_ANON_KEY=test-anon-key",
      "SUPABASE_SERVICE_KEY=test-service-key"
    })
public class SummerBuildApplicationTests {
  @Test
  public void contextLoads() {
    // Test that the application context loads successfully
  }
}
