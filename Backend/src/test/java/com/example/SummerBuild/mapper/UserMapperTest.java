package com.example.SummerBuild.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.User;
import com.example.SummerBuild.model.UserRole;
import java.util.UUID;
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
    UUID id = UUID.randomUUID();
    user.setId(id);
    user.setName("testuser");
    user.setEmail("test@example.com");
    user.setRole(UserRole.USER);
    user.setGender(Gender.MALE);

    // When
    UserDto userDto = userMapper.toDto(user);

    // Then
    assertThat(userDto).isNotNull();
    assertThat(userDto.getId()).isEqualTo(id);
    assertThat(userDto.getName()).isEqualTo("testuser");
    assertThat(userDto.getEmail()).isEqualTo("test@example.com");
    assertThat(userDto.getRole()).isEqualTo(UserRole.USER);
    assertThat(userDto.getGender()).isEqualTo(Gender.MALE);
  }

  @Test
  void whenConvertDtoToUser_thenReturnUser() {
    // Given
    UserDto userDto = new UserDto();
    UUID id = UUID.randomUUID();
    userDto.setId(id);
    userDto.setName("testuser");
    userDto.setEmail("test@example.com");
    userDto.setRole(UserRole.USER);
    userDto.setGender(Gender.MALE);

    // When
    User user = userMapper.toEntity(userDto);

    // Then
    assertThat(user).isNotNull();
    assertThat(user.getId()).isEqualTo(id);
    assertThat(user.getName()).isEqualTo("testuser");
    assertThat(user.getEmail()).isEqualTo("test@example.com");
    assertThat(user.getRole()).isEqualTo(UserRole.USER);
    assertThat(user.getGender()).isEqualTo(Gender.MALE);
  }

  @Test
  void whenUpdateUserFromDto_thenUserIsUpdated() {
    // Given
    User user = new User();
    UUID id1 = UUID.randomUUID();
    user.setId(id1);
    user.setName("olduser");
    user.setEmail("old@example.com");
    user.setRole(UserRole.USER);
    user.setGender(Gender.MALE);

    UserDto userDto = new UserDto();
    UUID id2 = UUID.randomUUID();
    userDto.setId(id2);
    userDto.setName("newuser");
    userDto.setEmail("new@example.com");
    userDto.setRole(UserRole.ADMIN);
    userDto.setGender(Gender.FEMALE);

    // When
    userMapper.updateEntityFromDto(userDto, user);

    // Then
    assertThat(user.getName()).isEqualTo("newuser");
    assertThat(user.getEmail()).isEqualTo("new@example.com");
    assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    assertThat(user.getGender()).isEqualTo(Gender.FEMALE);
  }
}
