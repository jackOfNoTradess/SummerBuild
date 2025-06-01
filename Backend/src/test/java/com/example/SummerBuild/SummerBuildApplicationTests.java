package com.example.SummerBuild;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
public class SummerBuildApplicationTests {
  @Test
  public void contextLoads() {}
}
