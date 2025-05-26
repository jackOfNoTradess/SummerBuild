package com.example.SummerBuild.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.User;
import com.example.SummerBuild.model.UserRole;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class GenericMapperTest {

  @Autowired private UserMapper userMapper;

  /** Test: Converting entity to DTO */
  @Test
  void testEntityToDtoBaseFields() {
    // Given
    User user = new User();
    UUID id = UUID.randomUUID();
    user.setId(id);
    LocalDateTime now = LocalDateTime.now();
    user.setCreatedAt(now);
    user.setUpdatedAt(now);

    // When
    UserDto dto = userMapper.toDto(user);

    // Then
    assertThat(dto).isNotNull();
    assertThat(dto.getId()).isEqualTo(id);
    assertThat(dto.getCreatedAt()).isEqualTo(now);
    assertThat(dto.getUpdatedAt()).isEqualTo(now);
  }

  /** Test: Converting DTO to entity, mapped back to entity */
  @Test
  void testDtoToEntityBaseFields() {
    // Given
    UserDto dto = new UserDto();
    UUID id = UUID.randomUUID();
    dto.setId(id);
    LocalDateTime now = LocalDateTime.now();
    dto.setCreatedAt(now);
    dto.setUpdatedAt(now);

    // When
    User user = userMapper.toEntity(dto);

    // Then
    assertThat(user).isNotNull();
    assertThat(user.getId()).isEqualTo(id);
    assertThat(user.getCreatedAt()).isEqualTo(now);
    assertThat(user.getUpdatedAt()).isEqualTo(now);
  }

  /** Test: Converting entity to DTO with specific fields */
  @Test
  void testEntityToDtoSpecificFields() {
    // Given
    User user = new User();
    UUID id = UUID.randomUUID();
    user.setId(id);
    user.setName("testUser");
    user.setEmail("test@example.com");
    user.setRole(UserRole.USER);
    user.setGender(Gender.MALE);

    // When
    UserDto dto = userMapper.toDto(user);

    // Then
    assertThat(dto.getName()).isEqualTo("testUser");
    assertThat(dto.getEmail()).isEqualTo("test@example.com");
    assertThat(dto.getRole()).isEqualTo(UserRole.USER);
    assertThat(dto.getGender()).isEqualTo(Gender.MALE);
  }

  /** Test: Converting DTO to entity with specific fields */
  @Test
  void testDtoToEntitySpecificFields() {
    // Given
    UserDto dto = new UserDto();
    UUID id = UUID.randomUUID();
    dto.setId(id);
    dto.setName("testUser");
    dto.setEmail("test@example.com");
    dto.setRole(UserRole.USER);
    dto.setGender(Gender.MALE);

    // When
    User user = userMapper.toEntity(dto);

    // Then
    assertThat(user.getName()).isEqualTo("testUser");
    assertThat(user.getEmail()).isEqualTo("test@example.com");
    assertThat(user.getRole()).isEqualTo(UserRole.USER);
    assertThat(user.getGender()).isEqualTo(Gender.MALE);
  }

  /** Test: Updating entity with partial DTO data Only specified fields are updated */
  @Test
  void testPartialUpdate() {
    // Given
    User user = new User();
    UUID id = UUID.randomUUID();
    user.setId(id);
    user.setName("oldUser");
    user.setEmail("old@example.com");
    user.setRole(UserRole.USER);
    user.setGender(Gender.MALE);
    LocalDateTime originalCreatedAt = LocalDateTime.now().minusDays(1);
    user.setCreatedAt(originalCreatedAt);
    user.setUpdatedAt(originalCreatedAt);

    UserDto dto = new UserDto();
    dto.setId(id);
    dto.setName("newuser");

    // When
    userMapper.updateEntityFromDto(dto, user);

    // Then
    assertThat(user.getName()).isEqualTo("newuser");
    assertThat(user.getEmail()).isEqualTo("old@example.com");
    assertThat(user.getCreatedAt()).isEqualTo(originalCreatedAt);
  }

  /** Test: Converting null entity to DTO Expected: Returns null */
  @Test
  void testNullEntityToDto() {
    // When
    UserDto dto = userMapper.toDto(null);

    // Then
    assertThat(dto).isNull();
  }

  /** Test: Converting null DTO to entity Expected: Returns null */
  @Test
  void testNullDtoToEntity() {
    // When
    User user = userMapper.toEntity(null);

    // Then
    assertThat(user).isNull();
  }

  /** Test: Updating entity with null DTO Expected: Entity remains unchanged */
  @Test
  void testUpdateWithNullDto() {
    // Given
    User user = new User();
    UUID id = UUID.randomUUID();
    user.setId(id);
    user.setName("testUser");
    user.setEmail("test@example.com");
    user.setRole(UserRole.USER);
    user.setGender(Gender.MALE);
    LocalDateTime originalCreatedAt = LocalDateTime.now();
    user.setCreatedAt(originalCreatedAt);
    user.setUpdatedAt(originalCreatedAt);

    // When
    userMapper.updateEntityFromDto(null, user);

    // Then
    assertThat(user.getName()).isEqualTo("testUser");
    assertThat(user.getEmail()).isEqualTo("test@example.com");
    assertThat(user.getCreatedAt()).isEqualTo(originalCreatedAt);
  }
}
