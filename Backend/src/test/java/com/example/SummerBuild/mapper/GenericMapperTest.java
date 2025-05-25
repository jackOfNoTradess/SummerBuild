package com.example.SummerBuild.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.model.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class GenericMapperTest {

  @Autowired private UserMapper userMapper;

  /**
   * Test: Converting entity to DTO Expected: Base fields (id, createdAt, updatedAt) are correctly
   * mapped
   */
  @Test
  void testEntityToDtoBaseFields() {
    // Given
    User user = new User();
    user.setId(1L);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());

    // When
    UserDto dto = userMapper.toDto(user);

    // Then
    assertThat(dto).isNotNull();
    assertThat(dto.getId()).isEqualTo(1L);
    assertThat(dto.getCreatedAt()).isEqualTo(user.getCreatedAt());
    assertThat(dto.getUpdatedAt()).isEqualTo(user.getUpdatedAt());
  }

  /** Test: Converting DTO to entity Expected: Base fields are correctly mapped back to entity */
  @Test
  void testDtoToEntityBaseFields() {
    // Given
    UserDto dto = new UserDto();
    dto.setId(1L);
    dto.setCreatedAt(LocalDateTime.now());
    dto.setUpdatedAt(LocalDateTime.now());

    // When
    User user = userMapper.toEntity(dto);

    // Then
    assertThat(user).isNotNull();
    assertThat(user.getId()).isEqualTo(1L);
    assertThat(user.getCreatedAt()).isEqualTo(dto.getCreatedAt());
    assertThat(user.getUpdatedAt()).isEqualTo(dto.getUpdatedAt());
  }

  /**
   * Test: Converting entity to DTO with specific fields Expected: Entity-specific fields are
   * correctly mapped
   */
  @Test
  void testEntityToDtoSpecificFields() {
    // Given
    User user = new User();
    user.setId(1L);
    user.setUsername("testuser");
    user.setEmail("test@example.com");

    // When
    UserDto dto = userMapper.toDto(user);

    // Then
    assertThat(dto.getUsername()).isEqualTo("testuser");
    assertThat(dto.getEmail()).isEqualTo("test@example.com");
  }

  /**
   * Test: Converting DTO to entity with specific fields Expected: DTO fields are correctly mapped
   * to entity
   */
  @Test
  void testDtoToEntitySpecificFields() {
    // Given
    UserDto dto = new UserDto();
    dto.setId(1L);
    dto.setUsername("testuser");
    dto.setEmail("test@example.com");

    // When
    User user = userMapper.toEntity(dto);

    // Then
    assertThat(user.getUsername()).isEqualTo("testuser");
    assertThat(user.getEmail()).isEqualTo("test@example.com");
  }

  /**
   * Test: Updating entity with partial DTO data Expected: Only specified fields are updated, others
   * remain unchanged
   */
  @Test
  void testPartialUpdate() {
    // Given
    User user = new User();
    user.setId(1L);
    user.setUsername("olduser");
    user.setEmail("old@example.com");
    LocalDateTime originalCreatedAt = LocalDateTime.now().minusDays(1);
    user.setCreatedAt(originalCreatedAt);
    user.setUpdatedAt(originalCreatedAt);

    UserDto dto = new UserDto();
    dto.setId(1L);
    dto.setUsername("newuser");
    // Email not set in DTO

    // When
    userMapper.updateEntityFromDto(dto, user);

    // Then
    assertThat(user.getUsername()).isEqualTo("newuser");
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
    user.setId(1L);
    user.setUsername("testuser");
    user.setEmail("test@example.com");
    LocalDateTime originalCreatedAt = LocalDateTime.now();
    user.setCreatedAt(originalCreatedAt);
    user.setUpdatedAt(originalCreatedAt);

    // When
    userMapper.updateEntityFromDto(null, user);

    // Then
    assertThat(user.getUsername()).isEqualTo("testuser");
    assertThat(user.getEmail()).isEqualTo("test@example.com");
    assertThat(user.getCreatedAt()).isEqualTo(originalCreatedAt);
  }
}
