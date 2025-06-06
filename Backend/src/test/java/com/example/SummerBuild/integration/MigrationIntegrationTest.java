package com.example.SummerBuild.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@JdbcTest
@ExtendWith(SpringExtension.class)
@ActiveProfiles("flyway-test")
@Testcontainers
public class MigrationIntegrationTest {

  @Container
  private static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test");

  @Autowired private JdbcTemplate jdbcTemplate;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.clean-disabled", () -> false);
  }

  @Test
  void testAllMigrationsApplySuccessfully() {
    // Configure and run Flyway
    Flyway flyway =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .cleanDisabled(false)
            .locations("classpath:db/migration")
            .load();
    flyway.migrate();

    // Verify that all required tables exist
    assertTrue(tableExists("users"), "users table should exist");
    assertTrue(tableExists("events"), "events table should exist");
  }

  @Test
  void testMigrationFailureAndRecovery() {
    Flyway flyway =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .cleanDisabled(false)
            .locations("classpath:db/migration")
            .load();
    flyway.clean();
    flyway.migrate();
    assertTrue(tableExists("users"), "users table should exist after recovery");
    assertTrue(tableExists("events"), "events table should exist after recovery");
  }

  @Test
  void testMigrationPerformance() {
    long startTime = System.currentTimeMillis();
    Flyway flyway =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .cleanDisabled(false)
            .locations("classpath:db/migration")
            .load();
    flyway.clean();
    flyway.migrate();
    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;
    assertTrue(duration < 5000, "Migrations should complete in under 5 seconds");
  }

  private boolean tableExists(String tableName) {
    try {
      jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
