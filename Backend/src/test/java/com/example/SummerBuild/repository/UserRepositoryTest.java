package com.example.SummerBuild.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.SummerBuild.model.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
public class UserRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private UserRepository userRepository;

  @Test
  void whenSaveUser_thenReturnSavedUser() {
    // Given
    User user = new User();
    user.setUsername("testuser");
    user.setEmail("test@example.com");
    user.setPassword("password123");

    // When
    User savedUser = userRepository.save(user);

    // Then
    assertThat(savedUser).isNotNull();
    assertThat(savedUser.getId()).isNotNull();
    assertThat(savedUser.getUsername()).isEqualTo("testuser");
  }

  @Test
  void whenFindByUsername_thenReturnUser() {
    // Given
    User user = new User();
    user.setUsername("testuser");
    user.setEmail("test@example.com");
    user.setPassword("password123");
    entityManager.persist(user);
    entityManager.flush();

    // When
    Optional<User> found = userRepository.findByUsername("testuser");

    // Then
    assertThat(found).isPresent();
    assertThat(found.get().getUsername()).isEqualTo("testuser");
  }

  @Test
  void whenFindByEmail_thenReturnUser() {
    // Given
    User user = new User();
    user.setUsername("testuser");
    user.setEmail("test@example.com");
    user.setPassword("password123");
    entityManager.persist(user);
    entityManager.flush();

    // When
    Optional<User> found = userRepository.findByEmail("test@example.com");

    // Then
    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("test@example.com");
  }
}
