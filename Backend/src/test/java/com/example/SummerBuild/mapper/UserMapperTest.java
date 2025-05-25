package com.example.SummerBuild.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UserMapperTest {

  @Autowired private UserMapper userMapper;

  @Test
  void whenConvertUserToDto_thenReturnUserDto() {
    // Given
    User user = new User();
    user.setId(1L);
    user.setUsername("testuser");
    user.setEmail("test@example.com");
    user.setPassword("password123");

    // When
    UserDto userDto = userMapper.toDto(user);

    // Then
    assertThat(userDto).isNotNull();
    assertThat(userDto.getId()).isEqualTo(1L);
    assertThat(userDto.getUsername()).isEqualTo("testuser");
    assertThat(userDto.getEmail()).isEqualTo("test@example.com");
    // Password is intentionally not included in DTO
  }

  @Test
  void whenConvertDtoToUser_thenReturnUser() {
    // Given
    UserDto userDto = new UserDto();
    userDto.setId(1L);
    userDto.setUsername("testuser");
    userDto.setEmail("test@example.com");

    // When
    User user = userMapper.toEntity(userDto);

    // Then
    assertThat(user).isNotNull();
    assertThat(user.getId()).isEqualTo(1L);
    assertThat(user.getUsername()).isEqualTo("testuser");
    assertThat(user.getEmail()).isEqualTo("test@example.com");
    // Password should be null as it's not in DTO
    assertThat(user.getPassword()).isNull();
  }

  @Test
  void whenUpdateUserFromDto_thenUserIsUpdated() {
    // Given
    User user = new User();
    user.setId(1L);
    user.setUsername("olduser");
    user.setEmail("old@example.com");
    user.setPassword("oldpassword");

    UserDto userDto = new UserDto();
    userDto.setId(1L);
    userDto.setUsername("newuser");
    userDto.setEmail("new@example.com");

    // When
    userMapper.updateEntityFromDto(userDto, user);

    // Then
    assertThat(user.getUsername()).isEqualTo("newuser");
    assertThat(user.getEmail()).isEqualTo("new@example.com");
    // Password should remain unchanged
    assertThat(user.getPassword()).isEqualTo("oldpassword");
  }
}
