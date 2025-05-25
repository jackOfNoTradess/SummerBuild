package com.example.SummerBuild.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.mapper.UserMapper;
import com.example.SummerBuild.model.User;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(UserMapper.class)
public class GenericRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private UserRepository userRepository;

  @Autowired private UserMapper userMapper;

  /**
   * Test: Saving entity to repository Expected: Entity is saved with generated ID and timestamps
   */
  @Test
  void testSaveEntity() {
    // Given
    User user = new User();
    user.setUsername("testuser");
    user.setEmail("test@example.com");

    // When
    User savedUser = userRepository.save(user);

    // Then
    assertThat(savedUser).isNotNull();
    assertThat(savedUser.getId()).isNotNull();
    assertThat(savedUser.getCreatedAt()).isNotNull();
    assertThat(savedUser.getUpdatedAt()).isNotNull();
  }

  /** Test: Finding all entities Expected: Returns all saved entities */
  @Test
  void testFindAll() {
    // Given
    User user1 = new User();
    user1.setUsername("user1");
    user1.setEmail("user1@example.com");

    User user2 = new User();
    user2.setUsername("user2");
    user2.setEmail("user2@example.com");

    entityManager.persist(user1);
    entityManager.persist(user2);
    entityManager.flush();

    // When
    List<User> users = userRepository.findAll();

    // Then
    assertThat(users).hasSize(2);
    assertThat(users).extracting("username").containsExactlyInAnyOrder("user1", "user2");
  }

  /** Test: Deleting entity Expected: Entity is removed from repository */
  @Test
  void testDeleteEntity() {
    // Given
    User user = new User();
    user.setUsername("testuser");
    user.setEmail("test@example.com");
    entityManager.persist(user);
    entityManager.flush();

    // When
    userRepository.delete(user);
    entityManager.flush();

    // Then
    assertThat(userRepository.findById(user.getId())).isEmpty();
  }

  /** Test: Finding entity by ID Expected: Returns entity if exists, empty if not */
  @Test
  void testFindById() {
    // Given
    User user = new User();
    user.setUsername("testuser");
    user.setEmail("test@example.com");
    entityManager.persist(user);
    entityManager.flush();

    // When
    var foundUser = userRepository.findById(user.getId());

    // Then
    assertThat(foundUser).isPresent();
    assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
  }

  /** Test: Finding non-existent entity Expected: Returns empty optional */
  @Test
  void testFindNonExistentEntity() {
    // When
    var foundUser = userRepository.findById(999L);

    // Then
    assertThat(foundUser).isEmpty();
  }

  @Test
  void testEntityListToDtoList() {
    // Given
    List<User> users = Arrays.asList(createUser(1L, "user1"), createUser(2L, "user2"));

    // When
    List<UserDto> dtos = userMapper.toDtoList(users);

    // Then
    assertThat(dtos).hasSize(2);
    assertThat(dtos.get(0).getUsername()).isEqualTo("user1");
    assertThat(dtos.get(1).getUsername()).isEqualTo("user2");
  }

  @Test
  void testDtoListToEntityList() {
    // Given
    List<UserDto> dtos = Arrays.asList(createUserDto(1L, "user1"), createUserDto(2L, "user2"));

    // When
    List<User> users = userMapper.toEntityList(dtos);

    // Then
    assertThat(users).hasSize(2);
    assertThat(users.get(0).getUsername()).isEqualTo("user1");
    assertThat(users.get(1).getUsername()).isEqualTo("user2");
  }

  private User createUser(Long id, String username) {
    User user = new User();
    user.setId(id);
    user.setUsername(username);
    return user;
  }

  private UserDto createUserDto(Long id, String username) {
    UserDto dto = new UserDto();
    dto.setId(id);
    dto.setUsername(username);
    return dto;
  }
}
