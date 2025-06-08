package com.example.SummerBuild.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.User;
import com.example.SummerBuild.model.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import com.example.SummerBuild.SummerBuildApplication;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = SummerBuildApplication.class)
public class UserRepositoryTest {

  @Autowired private UserRepository userRepository;

  private User user1, user2, user3;

  @BeforeEach
  void setUp() {
    user1 = User.builder().id(UUID.randomUUID()).role(UserRole.ADMIN).gender(Gender.MALE).build();

    user2 = User.builder().id(UUID.randomUUID()).role(UserRole.USER).gender(Gender.FEMALE).build();

    user3 = User.builder().id(UUID.randomUUID()).role(UserRole.USER).gender(Gender.MALE).build();

    userRepository.saveAll(List.of(user1, user2, user3));
  }

  @Test
  void testFindByRole() {
    List<User> users = userRepository.findByRole(UserRole.USER);
    assertThat(users).hasSize(2).extracting("role").containsOnly(UserRole.USER);
  }

  @Test
  void testFindByGender() {
    List<User> users = userRepository.findByGender(Gender.MALE);
    assertThat(users).hasSize(2).extracting("gender").containsOnly(Gender.MALE);
  }

  @Test
  void testFindByCreatedBetween() {
    LocalDateTime now = LocalDateTime.now();
    List<User> users = userRepository.findByCreatedBetween(now.minusDays(1), now.plusDays(1));
    assertThat(users).hasSize(3);
  }

  @Test
  void testCountByRole() {
    long count = userRepository.countByRole(UserRole.USER);
    assertThat(count).isEqualTo(2);
  }
}
