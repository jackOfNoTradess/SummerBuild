package com.example.SummerBuild.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.SummerBuild.dto.UserDto;
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

  @InjectMocks private UserService userService;

  private User user;
  private UserDto userDto;
  private UUID userId;

  // Runs a method automatically BEFORE each individual test method
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

  // Checks that findAll returns a list of all users
  @Test
  void findAllReturnsAllUsers() {
    when(userRepository.findAll()).thenReturn(Arrays.asList(user));

    List<UserDto> result = userService.findAll();

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(user.getName(), result.get(0).getName());
  }

  // Checks that findById returns the correct user if user exists
  @Test
  void findByIdReturnsUser() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    UserDto result = userService.findById(userId);

    assertNotNull(result);
    assertEquals(user.getName(), result.getName());
    assertEquals(user.getEmail(), result.getEmail());
  }

  // Checks that findById throws ResourceNotFoundException when user doesn't exist
  @Test
  void findByIdThrowsWhenNotFound() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThrows(UserService.ResourceNotFoundException.class, () -> userService.findById(userId));
  }

  // Checks that findByEmail returns the correct user if user exists
  @Test
  void findByEmailReturnsUser() {
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

    UserDto result = userService.findByEmail(user.getEmail());

    assertNotNull(result);
    assertEquals(user.getName(), result.getName());
    assertEquals(user.getEmail(), result.getEmail());
  }

  // Checks that findByEmail throws ResourceNotFoundException if user doesn't exist
  @Test
  void findByEmailThrowsWhenNotFound() {
    when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

    assertThrows(
        UserService.ResourceNotFoundException.class,
        () -> userService.findByEmail(user.getEmail()));
  }

  // Verifies that user creation succeeds when email is unique:
  // 1. Mocks email check to return empty (email doesn't exist)
  // 2. Mocks save operation to return saved user
  // 3. Verifies the returned user has correct name and email
  @Test
  void createReturnsNewUser() {
    when(userRepository.findByEmail(userDto.getEmail())).thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenReturn(user);

    UserDto result = userService.create(userDto);

    assertNotNull(result);
    assertEquals(user.getName(), result.getName());
    assertEquals(user.getEmail(), result.getEmail());
  }

  // Checks that create throws DuplicateResourceException when email already exists
  @Test
  void createThrowsWhenEmailExists() {
    when(userRepository.findByEmail(userDto.getEmail())).thenReturn(Optional.of(user));

    assertThrows(UserService.DuplicateResourceException.class, () -> userService.create(userDto));
  }

  // Checks that update returns the updated user when user exists
  @Test
  void updateReturnsUpdatedUser() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenReturn(user);

    UserDto result = userService.update(userId, userDto);

    assertNotNull(result);
    assertEquals(user.getName(), result.getName());
    assertEquals(user.getEmail(), result.getEmail());
  }

  // Checks that update throws ResourceNotFoundException when user doesn't exist
  @Test
  void updateThrowsWhenNotFound() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThrows(
        UserService.ResourceNotFoundException.class, () -> userService.update(userId, userDto));
  }

  // Checks that delete removes the user when user exists
  @Test
  void deleteRemovesUser() {
    when(userRepository.existsById(userId)).thenReturn(true);
    doNothing().when(userRepository).deleteById(userId);

    assertDoesNotThrow(() -> userService.delete(userId));
    verify(userRepository).deleteById(userId);
  }

  // Checks that delete throws ResourceNotFoundException when user doesn't exist
  @Test
  void deleteThrowsWhenNotFound() {
    when(userRepository.existsById(userId)).thenReturn(false);

    assertThrows(UserService.ResourceNotFoundException.class, () -> userService.delete(userId));
  }

  // Checks that findByName returns users whose names contain the search term
  @Test
  void findByNameReturnsMatchingUsers() {
    when(userRepository.findByNameContainingIgnoreCase("John")).thenReturn(Arrays.asList(user));

    List<UserDto> result = userService.findByNameContaining("John");

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(user.getName(), result.get(0).getName());
  }

  // Checks that findByRole returns users with the specified role
  @Test
  void findByRoleReturnsUsers() {
    when(userRepository.findByRole(UserRole.USER)).thenReturn(Arrays.asList(user));

    List<UserDto> result = userService.findByRole(UserRole.USER);

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(UserRole.USER, result.get(0).getRole());
  }

  // Checks that countByRole returns the number of users with the specified role
  @Test
  void countByRoleReturnsCount() {
    when(userRepository.countByRole(UserRole.USER)).thenReturn(1L);

    Long result = userService.countByRole(UserRole.USER);

    assertEquals(1L, result);
  }
}
