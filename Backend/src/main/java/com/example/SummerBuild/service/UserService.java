package com.example.SummerBuild.service;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.mapper.UserMapper;
import com.example.SummerBuild.model.User;
import com.example.SummerBuild.model.UserRole;
import com.example.SummerBuild.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ResponseStatus;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @ResponseStatus(HttpStatus.NOT_FOUND)
  public static class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
      super(message);
    }
  }

  @ResponseStatus(HttpStatus.CONFLICT)
  public static class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
      super(message);
    }
  }

  @Transactional(readOnly = true)
  public List<UserDto> findAll() {
    return userRepository.findAll().stream().map(userMapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public UserDto findById(UUID id) {
    return userRepository
        .findById(id)
        .map(userMapper::toDto)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
  }

  @Transactional(readOnly = true)
  public UserDto findByName(String name) {
    return userRepository
        .findByName(name)
        .map(userMapper::toDto)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with name: " + name));
  }

  @Transactional(readOnly = true)
  public UserDto findByEmail(String email) {
    return userRepository
        .findByEmail(email)
        .map(userMapper::toDto)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
  }

  @Transactional
  public UserDto create(UserDto userDto) {
    validateNewUser(userDto);
    User user = userMapper.toEntity(userDto);
    User savedUser = userRepository.save(user);
    return userMapper.toDto(savedUser);
  }

  @Transactional
  public UserDto update(UUID id, UserDto userDto) {
    User existingUser =
        userRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

    validateUpdateUser(userDto, existingUser);
    userMapper.updateEntityFromDto(userDto, existingUser);
    User updatedUser = userRepository.save(existingUser);
    return userMapper.toDto(updatedUser);
  }

  @Transactional
  public void delete(UUID id) {
    if (!userRepository.existsById(id)) {
      throw new ResourceNotFoundException("User not found with id: " + id);
    }
    userRepository.deleteById(id);
  }

  @Transactional(readOnly = true)
  public List<UserDto> findByRole(UserRole role) {
    return userRepository.findByRole(role).stream().map(userMapper::toDto).toList();
  }

  @Transactional(readOnly = true)
  public List<UserDto> findByNameContaining(String name) {
    return userRepository.findByNameContainingIgnoreCase(name).stream()
        .map(userMapper::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<UserDto> findByCreatedBetween(LocalDateTime startDate, LocalDateTime endDate) {
    return userRepository.findByCreatedBetween(startDate, endDate).stream()
        .map(userMapper::toDto)
        .toList();
  }

  @Transactional(readOnly = true)
  public long countByRole(UserRole role) {
    return userRepository.countByRole(role);
  }

  private void validateNewUser(UserDto userDto) {
    if (userRepository.existsByEmail(userDto.getEmail())) {
      throw new DuplicateResourceException("Email already exists: " + userDto.getEmail());
    }
    if (userRepository.findByName(userDto.getName()).isPresent()) {
      throw new DuplicateResourceException("Username already exists: " + userDto.getName());
    }
  }

  private void validateUpdateUser(UserDto userDto, User existingUser) {
    if (!existingUser.getEmail().equals(userDto.getEmail())
        && userRepository.existsByEmail(userDto.getEmail())) {
      throw new DuplicateResourceException("Email already exists: " + userDto.getEmail());
    }
    if (!existingUser.getName().equals(userDto.getName())
        && userRepository.findByName(userDto.getName()).isPresent()) {
      throw new DuplicateResourceException("Username already exists: " + userDto.getName());
    }
  }
}
