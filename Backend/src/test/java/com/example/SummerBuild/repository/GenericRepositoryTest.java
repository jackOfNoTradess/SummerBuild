package com.example.SummerBuild.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.mapper.UserMapper;
import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.User;
import com.example.SummerBuild.model.UserRole;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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

  /** Test: Saving entity to repository */
  @Test
  void testSaveEntity() {
    // Given
    User user = new User();
    user.setName("testUser");
    user.setEmail("test@example.com");
    user.setRole(UserRole.USER);
    user.setGender(Gender.MALE);

    // When
    User savedUser = userRepository.save(user);

    // Then
    assertThat(savedUser).isNotNull();
    assertThat(savedUser.getId()).isNotNull();
    assertThat(savedUser.getCreatedAt()).isNotNull();
    assertThat(savedUser.getUpdatedAt()).isNotNull();
  }

  /** Test: Getting all saved entities */
  @Test
  void testFindAll() {
    // Given
    User user1 = new User();
    user1.setName("user1");
    user1.setEmail("user1@example.com");
    user1.setRole(UserRole.USER);
    user1.setGender(Gender.MALE);

    User user2 = new User();
    user2.setName("user2");
    user2.setEmail("user2@example.com");
    user2.setRole(UserRole.USER);
    user2.setGender(Gender.FEMALE);

    entityManager.persist(user1);
    entityManager.persist(user2);
    entityManager.flush();

    // When
    List<User> users = userRepository.findAll();

    // Then
    assertThat(users).hasSize(2);
    assertThat(users).extracting("name").containsExactlyInAnyOrder("user1", "user2");
  }

  /** Test: Deleting entity from repository */
  @Test
  void testDeleteEntity() {
    // Given
    User user = new User();
    user.setName("testUser");
    user.setEmail("test@example.com");
    user.setRole(UserRole.USER);
    user.setGender(Gender.MALE);
    entityManager.persist(user);
    entityManager.flush();

    // When
    userRepository.delete(user);
    entityManager.flush();

    // Then
    assertThat(userRepository.findById(user.getId())).isEmpty();
  }

  /** Test: Finding entity by ID if exists else should be empty */
  @Test
  void testFindById() {
    // Given
    User user = new User();
    user.setName("testUser");
    user.setEmail("test@example.com");
    user.setRole(UserRole.USER);
    user.setGender(Gender.MALE);
    entityManager.persist(user);
    entityManager.flush();

    // When
    var foundUser = userRepository.findById(user.getId());

    // Then
    assertThat(foundUser).isPresent();
    assertThat(foundUser.get().getName()).isEqualTo("testUser");
  }

  /** Test: Finding non-existent entity Expected: Returns empty optional */
  @Test
  void testFindNonExistentEntity() {
    // When
    var foundUser = userRepository.findById(UUID.randomUUID());

    // Then
    assertThat(foundUser).isEmpty();
  }

  @Test
  void testEntityListToDtoList() {
    // Given
    List<User> users = Arrays.asList(createUser("user1"), createUser("user2"));

    // When
    List<UserDto> dtos = userMapper.toDtoList(users);

    // Then
    assertThat(dtos).hasSize(2);
    assertThat(dtos.get(0).getName()).isEqualTo("user1");
    assertThat(dtos.get(1).getName()).isEqualTo("user2");
  }

  @Test
  void testDtoListToEntityList() {
    // Given
    List<UserDto> dtos = Arrays.asList(createUserDto("user1"), createUserDto("user2"));

    // When
    List<User> users = userMapper.toEntityList(dtos);

    // Then
    assertThat(users).hasSize(2);
    assertThat(users.get(0).getName()).isEqualTo("user1");
    assertThat(users.get(1).getName()).isEqualTo("user2");
  }

  private User createUser(String name) {
    User user = new User();
    user.setName(name);
    user.setEmail(name + "@example.com");
    user.setRole(UserRole.USER);
    user.setGender(Gender.MALE);
    return user;
  }

  private UserDto createUserDto(String name) {
    UserDto dto = new UserDto();
    dto.setName(name);
    dto.setEmail(name + "@example.com");
    dto.setRole(UserRole.USER);
    dto.setGender(Gender.MALE);
    return dto;
  }
}
