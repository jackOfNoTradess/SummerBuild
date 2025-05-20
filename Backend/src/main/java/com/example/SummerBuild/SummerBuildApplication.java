package com.example.SummerBuild;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class SummerBuildApplication {
  public static void main(String[] args) {
    initializeApplication(args);
  }

  static ConfigurableApplicationContext initializeApplication(String[] args) {
    return SpringApplication.run(SummerBuildApplication.class, args);
  }
}
