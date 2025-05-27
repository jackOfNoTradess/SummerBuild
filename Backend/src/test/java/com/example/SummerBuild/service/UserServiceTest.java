package com.example.SummerBuild.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.mapper.UserMapper;
import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.User;
import com.example.SummerBuild.model.UserRole;
import com.example.SummerBuild.repository.UserRepository;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private UserMapper userMapper;
  @InjectMocks private UserService userService;

  @Captor private ArgumentCaptor<UUID> uuidCaptor;

  private final UUID userId = UUID.randomUUID();
  private final User user =
      User.builder().userUuid(userId).gender(Gender.MALE).role(UserRole.USER).build();

  private final UserDto userDto = new UserDto();

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    userService = new UserService(userRepository, userMapper);
    ReflectionTestUtils.setField(userService, "serviceKey", "dummyServiceKey");
    ReflectionTestUtils.setField(userService, "supabaseUrl", "http://dummy.supabase.io");
  }

  @Test
  void findAll_returnsMappedUsers() {
    when(userRepository.findAll()).thenReturn(List.of(user));
    when(userMapper.toDto(user)).thenReturn(userDto);

    List<UserDto> result = userService.findAll();

    assertThat(result).containsExactly(userDto);
  }

  @Test
  void findById_found_returnsDto() {
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userMapper.toDto(user)).thenReturn(userDto);

    UserDto result = userService.findById(userId);

    assertThat(result).isEqualTo(userDto);
  }

  @Test
  void findById_notFound_throwsException() {
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.findById(userId))
        .isInstanceOf(UserService.ResourceNotFoundException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  void delete_found_callsDelete() {
    when(userRepository.existsById(userId)).thenReturn(true);

    userService.delete(userId);

    verify(userRepository).deleteById(userId);
  }

  @Test
  void delete_notFound_throwsException() {
    when(userRepository.existsById(userId)).thenReturn(false);

    assertThatThrownBy(() -> userService.delete(userId))
        .isInstanceOf(UserService.ResourceNotFoundException.class);
  }

  @Test
  void findByRole_returnsMappedUsers() {
    when(userRepository.findByRole(UserRole.USER)).thenReturn(List.of(user));
    when(userMapper.toDto(user)).thenReturn(userDto);

    List<UserDto> result = userService.findByRole(UserRole.USER);

    assertThat(result).containsExactly(userDto);
  }

  @Test
  void getAllUsers_returnsResponseEntity() {
    RestTemplate restTemplateMock = mock(RestTemplate.class);
    ReflectionTestUtils.setField(userService, "restTemplate", restTemplateMock);

    String response = "{\"message\": \"ok\"}";
    when(restTemplateMock.exchange(
            anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(new ResponseEntity<>(response, HttpStatus.OK));

    ResponseEntity<String> result = userService.getAllUsers();

    assertThat(result.getBody()).isEqualTo(response);
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void getUserById_returnsResponseEntity() {
    RestTemplate restTemplateMock = mock(RestTemplate.class);
    ReflectionTestUtils.setField(userService, "restTemplate", restTemplateMock);

    when(restTemplateMock.exchange(
            contains(userId.toString()),
            eq(HttpMethod.GET),
            any(HttpEntity.class),
            eq(String.class)))
        .thenReturn(ResponseEntity.ok("{\"email\":\"test@example.com\"}"));

    ResponseEntity<String> result = userService.getUserById(userId);
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void deleteUserById_handlesHttpErrors() {
    RestTemplate restTemplateMock = mock(RestTemplate.class);
    ReflectionTestUtils.setField(userService, "restTemplate", restTemplateMock);

    String errorJson =
        "{\"code\":404,\"error_code\":\"user_not_found\",\"msg\":\"User not found\"}";

    HttpClientErrorException exception =
        HttpClientErrorException.create(
            HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, errorJson.getBytes(), null);

    when(restTemplateMock.exchange(
            contains(userId.toString()),
            eq(HttpMethod.DELETE),
            any(HttpEntity.class),
            eq(String.class)))
        .thenThrow(exception);

    ResponseEntity<String> result = userService.deleteUserById(userId);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(result.getBody()).contains("\"msg\":\"User not found\"");
  }
}
