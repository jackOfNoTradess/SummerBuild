package com.example.SummerBuild.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.mapper.UserMapper;
import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.User;
import com.example.SummerBuild.model.UserRole;
import com.example.SummerBuild.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private UserMapper userMapper;

  @InjectMocks private UserService userService;

  private User user;
  private UserDto userDto;
  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    user = new User();
    user.setId(userId);
    user.setName("John Doe");
    user.setEmail("john@example.com");
    user.setRole(UserRole.USER);
    user.setGender(Gender.MALE);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());

    userDto = new UserDto();
    userDto.setId(userId);
    userDto.setName("John Doe");
    userDto.setEmail("john@example.com");
    userDto.setRole(UserRole.USER);
    userDto.setGender(Gender.MALE);
  }

  @Test
  void findAllReturnsAllUsers() {
    when(userRepository.findAll()).thenReturn(Arrays.asList(user));
    when(userMapper.toDto(user)).thenReturn(userDto);

    List<UserDto> result = userService.findAll();

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(userDto.getName(), result.get(0).getName());
  }

  @Test
  void findByIdReturnsUser() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userMapper.toDto(user)).thenReturn(userDto);

    UserDto result = userService.findById(userId);

    assertNotNull(result);
    assertEquals(userDto.getName(), result.getName());
    assertEquals(userDto.getEmail(), result.getEmail());
  }

  @Test
  void findByIdThrowsWhenNotFound() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThrows(UserService.ResourceNotFoundException.class, () -> userService.findById(userId));
  }

  @Test
  void findByEmailReturnsUser() {
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    when(userMapper.toDto(user)).thenReturn(userDto);

    UserDto result = userService.findByEmail(user.getEmail());

    assertNotNull(result);
    assertEquals(userDto.getName(), result.getName());
    assertEquals(userDto.getEmail(), result.getEmail());
  }

  @Test
  void findByEmailThrowsWhenNotFound() {
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

    assertThrows(
        UserService.ResourceNotFoundException.class,
        () -> userService.findByEmail(user.getEmail()));
  }

  @Test
  void createReturnsNewUser() {
    when(userRepository.existsByEmail(userDto.getEmail())).thenReturn(false);
    when(userRepository.findByName(userDto.getName())).thenReturn(Optional.empty());
    when(userMapper.toEntity(userDto)).thenReturn(user);
    when(userRepository.save(any(User.class))).thenReturn(user);
    when(userMapper.toDto(user)).thenReturn(userDto);

    UserDto result = userService.create(userDto);

    assertNotNull(result);
    assertEquals(userDto.getName(), result.getName());
    assertEquals(userDto.getEmail(), result.getEmail());
  }

  @Test
  void createThrowsWhenEmailExists() {
    when(userRepository.existsByEmail(userDto.getEmail())).thenReturn(true);

    assertThrows(UserService.DuplicateResourceException.class, () -> userService.create(userDto));
  }

  @Test
  void updateReturnsUpdatedUser() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    doAnswer(
            invocation -> {
              UserDto dto = invocation.getArgument(0);
              User entity = invocation.getArgument(1);
              entity.setName(dto.getName());
              entity.setEmail(dto.getEmail());
              entity.setRole(dto.getRole());
              entity.setGender(dto.getGender());
              return null;
            })
        .when(userMapper)
        .updateEntityFromDto(any(UserDto.class), any(User.class));
    when(userRepository.save(any(User.class))).thenReturn(user);
    when(userMapper.toDto(user)).thenReturn(userDto);

    UserDto result = userService.update(userId, userDto);

    assertNotNull(result);
    assertEquals(userDto.getName(), result.getName());
    assertEquals(userDto.getEmail(), result.getEmail());
  }

  @Test
  void updateThrowsWhenNotFound() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThrows(
        UserService.ResourceNotFoundException.class, () -> userService.update(userId, userDto));
  }

  @Test
  void deleteRemovesUser() {
    when(userRepository.existsById(userId)).thenReturn(true);
    doNothing().when(userRepository).deleteById(userId);

    assertDoesNotThrow(() -> userService.delete(userId));
    verify(userRepository).deleteById(userId);
  }

  @Test
  void deleteThrowsWhenNotFound() {
    when(userRepository.existsById(userId)).thenReturn(false);

    assertThrows(UserService.ResourceNotFoundException.class, () -> userService.delete(userId));
  }

  @Test
  void findByNameReturnsMatchingUsers() {
    when(userRepository.findByNameContainingIgnoreCase("John")).thenReturn(Arrays.asList(user));
    when(userMapper.toDto(user)).thenReturn(userDto);

    List<UserDto> result = userService.findByNameContaining("John");

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(userDto.getName(), result.get(0).getName());
  }

  @Test
  void findByRoleReturnsUsers() {
    when(userRepository.findByRole(UserRole.USER)).thenReturn(Arrays.asList(user));
    when(userMapper.toDto(user)).thenReturn(userDto);

    List<UserDto> result = userService.findByRole(UserRole.USER);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(userDto.getRole(), result.get(0).getRole());
  }

  @Test
  void countByRoleReturnsCount() {
    when(userRepository.countByRole(UserRole.USER)).thenReturn(1L);

    Long result = userService.countByRole(UserRole.USER);

    assertEquals(1L, result);
  }
}
