package com.example.SummerBuild.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.assertj.core.api.Assertions;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    classes = {MigrationIntegrationTest.TestConfig.class},
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
      "supabase.auth.enabled=false",
      "spring.main.allow-bean-definition-overriding=true"
    })
@ActiveProfiles("flyway-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
public class MigrationIntegrationTest {

  private static final boolean IS_CI = System.getenv("CI") != null;

  @Container
  private static final PostgreSQLContainer<?> postgres =
      IS_CI
          ? null
          : new PostgreSQLContainer<>("postgres:15")
              .withDatabaseName("testdb")
              .withUsername("test")
              .withPassword("test")
              .withReuse(true);

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    if (!IS_CI) {
      registry.add("spring.datasource.url", postgres::getJdbcUrl);
      registry.add("spring.datasource.username", postgres::getUsername);
      registry.add("spring.datasource.password", postgres::getPassword);
    }
  }

  @Autowired private DataSource dataSource;

  @BeforeEach
  void setUp() {
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement()) {
      stmt.execute("DROP SCHEMA public CASCADE");
      stmt.execute("CREATE SCHEMA public");
    } catch (Exception e) {
      fail("Failed to clean database: " + e.getMessage(), e);
    }
  }

  @Test
  void testAllMigrationsApplySuccessfully() {
    Flyway flyway = Flyway.configure().dataSource(dataSource).cleanDisabled(false).load();
    try {
      flyway.migrate();
      assertTrue(true, "Migrations applied successfully");
    } catch (Exception e) {
      fail("Migration failed: " + e.getMessage(), e);
    }
  }

  @Test
  void testMigrationFailureAndRecovery() {
    Flyway flyway = Flyway.configure().dataSource(dataSource).cleanDisabled(false).load();
    try {
      // Migration should succeed
      flyway.migrate();

      // Clean the database
      flyway.clean();

      // Try to migrate again - should succeed
      flyway.migrate();

      assertTrue(true, "Migration recovery successful");
    } catch (Exception e) {
      fail("Migration recovery failed: " + e.getMessage(), e);
    }
  }

  @Test
  void testMigrationPerformance() {
    Flyway flyway = Flyway.configure().dataSource(dataSource).cleanDisabled(false).load();
    try {
      // Measure migration execution time
      long startTime = System.currentTimeMillis();
      flyway.migrate();
      long endTime = System.currentTimeMillis();
      long executionTime = endTime - startTime;

      System.out.println("Migration Performance Results:");
      System.out.println("Total execution time: " + executionTime + "ms");
      System.out.println("Number of migrations: " + flyway.info().applied().length);
      System.out.println(
          "Average time per migration: " + (executionTime / flyway.info().applied().length) + "ms");

      // Migrations complete within a reasonable time (maybe within 30 seconds)
      Assertions.assertThat(executionTime).isLessThan(30000);
    } catch (Exception e) {
      fail("Migration performance test failed: " + e.getMessage(), e);
    }
  }

  @Configuration
  @ComponentScan(
      excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.example\\.SummerBuild\\.controller\\..*"),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.example\\.SummerBuild\\.service\\..*")
      })
  static class TestConfig {
    // Test configuration
  }
}
