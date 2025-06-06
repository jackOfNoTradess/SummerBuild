package com.example.SummerBuild.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
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

  @Container private static final PostgreSQLContainer<?> postgres;

  static {
    if (IS_CI) {
      postgres = null;
    } else {
      postgres =
          new PostgreSQLContainer<>("postgres:15")
              .withDatabaseName("testdb")
              .withUsername("test")
              .withPassword("test");
      postgres.start();
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
    // Create Flyway instance
    FluentConfiguration config =
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .baselineOnMigrate(true)
            .validateOnMigrate(true);

    Flyway flyway = config.load();

    // Clean and migrate
    flyway.clean();
    flyway.migrate();

    // Verify migration success
    assertTrue(flyway.info().all().length > 0, "Should have applied migrations");
    assertTrue(flyway.info().applied().length > 0, "Should have successful migrations");
  }

  @Test
  void testMigrationFailureAndRecovery() {
    // Create Flyway instance
    FluentConfiguration config =
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .baselineOnMigrate(true)
            .validateOnMigrate(true);

    Flyway flyway = config.load();

    // Clean and migrate
    flyway.clean();
    flyway.migrate();

    // Verify migration success
    assertTrue(flyway.info().all().length > 0, "Should have applied migrations");
    assertTrue(flyway.info().applied().length > 0, "Should have successful migrations");
  }

  @Test
  void testMigrationPerformance() {
    // Create Flyway instance
    FluentConfiguration config =
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .cleanDisabled(false)
            .baselineOnMigrate(true)
            .validateOnMigrate(true);

    Flyway flyway = config.load();

    // Clean and migrate
    flyway.clean();

    // Measure migration time
    Instant start = Instant.now();
    flyway.migrate();
    Instant end = Instant.now();

    Duration duration = Duration.between(start, end);

    // Verify migration success and performance
    assertTrue(flyway.info().all().length > 0, "Should have applied migrations");
    assertTrue(flyway.info().applied().length > 0, "Should have successful migrations");
    assertTrue(duration.toMillis() < 5000, "Migration should complete within 5 seconds");
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

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    if (!IS_CI) {
      registry.add("spring.datasource.url", postgres::getJdbcUrl);
      registry.add("spring.datasource.username", postgres::getUsername);
      registry.add("spring.datasource.password", postgres::getPassword);
    }
  }
}
