package com.example.SummerBuild.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.User;
import com.example.SummerBuild.model.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserMapperTest {

  private UserMapper userMapper;

  @BeforeEach
  void setUp() {
    userMapper = new UserMapper();
  }

  @Test
  void testToDto() {
    UUID uuid = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    User user = User.builder().id(uuid).gender(Gender.MALE).role(UserRole.ADMIN).build();
    user.setId(uuid);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    UserDto dto = userMapper.toDto(user);

    assertNotNull(dto);
    assertEquals(user.getId(), dto.getId());
    assertEquals(user.getGender(), dto.getGender());
    assertEquals(user.getRole(), dto.getRole());
    assertEquals(user.getCreatedAt(), dto.getCreatedAt());
    assertEquals(user.getUpdatedAt(), dto.getUpdatedAt());
  }

  @Test
  void testToEntity() {
    UUID uuid = UUID.randomUUID();
    LocalDateTime now = LocalDateTime.now();
    UserDto dto = new UserDto();
    dto.setId(UUID.randomUUID());
    dto.setGender(Gender.FEMALE);
    dto.setRole(UserRole.USER);
    dto.setCreatedAt(now);
    dto.setUpdatedAt(now);

    User user = userMapper.toEntity(dto);

    assertNotNull(user);
    assertEquals(dto.getId(), user.getId());
    assertEquals(dto.getGender(), user.getGender());
    assertEquals(dto.getRole(), user.getRole());
    assertEquals(dto.getCreatedAt(), user.getCreatedAt());
    assertEquals(dto.getUpdatedAt(), user.getUpdatedAt());
  }

  @Test
  void testUpdateEntityFromDto() {
    User user =
        User.builder().id(UUID.randomUUID()).gender(Gender.MALE).role(UserRole.ADMIN).build();
    user.setId(UUID.randomUUID());
    user.setUpdatedAt(LocalDateTime.now());
    UserDto dto = new UserDto();
    dto.setGender(Gender.FEMALE);
    dto.setRole(UserRole.USER);
    dto.setUpdatedAt(LocalDateTime.now().plusDays(1));

    userMapper.updateEntityFromDto(dto, user);

    assertEquals(Gender.FEMALE, user.getGender());
    assertEquals(UserRole.USER, user.getRole());
    assertEquals(dto.getUpdatedAt(), user.getUpdatedAt());
  }

  @Test
  void testToDtoList() {
    List<User> users =
        List.of(
            User.builder().id(UUID.randomUUID()).gender(Gender.MALE).role(UserRole.USER).build(),
            User.builder()
                .id(UUID.randomUUID())
                .gender(Gender.FEMALE)
                .role(UserRole.ADMIN)
                .build());

    List<UserDto> dtos = userMapper.toDtoList(users);

    assertEquals(2, dtos.size());
    assertEquals(users.get(0).getId(), dtos.get(0).getId());
    assertEquals(users.get(1).getId(), dtos.get(1).getId());
  }

  @Test
  void testToEntityList() {
    List<UserDto> dtos =
        List.of(
            createUserDto(UUID.randomUUID(), UUID.randomUUID(), Gender.MALE, UserRole.ADMIN),
            createUserDto(UUID.randomUUID(), UUID.randomUUID(), Gender.FEMALE, UserRole.USER));

    List<User> users = userMapper.toEntityList(dtos);

    assertEquals(2, users.size());
    assertEquals(dtos.get(0).getId(), users.get(0).getId());
    assertEquals(dtos.get(1).getId(), users.get(1).getId());
  }

  private UserDto createUserDto(UUID id, UUID uuid, Gender gender, UserRole role) {
    UserDto dto = new UserDto();
    dto.setId(id);
    dto.setGender(gender);
    dto.setRole(role);
    return dto;
  }
}
