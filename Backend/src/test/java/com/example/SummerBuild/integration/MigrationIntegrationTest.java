package com.example.SummerBuild.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = {MigrationIntegrationTest.TestConfig.class})
@ActiveProfiles("flyway-test")
@Testcontainers
public class MigrationIntegrationTest {

  @Container
  private static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test")
          .withReuse(true);

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
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
    Flyway flyway = Flyway.configure().dataSource(dataSource).load();

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
    Flyway flyway = Flyway.configure().dataSource(dataSource).load();

    try {
      Instant start = Instant.now();
      flyway.migrate();
      Instant end = Instant.now();

      Duration duration = Duration.between(start, end);
      assertTrue(
          duration.toSeconds() < 30,
          "Migration took too long: " + duration.toSeconds() + " seconds");
    } catch (Exception e) {
      fail("Migration performance test failed: " + e.getMessage(), e);
    }
  }

  @org.springframework.boot.test.context.TestConfiguration
  static class TestConfig {
    // Empty configuration - we only need the database
  }
}
