package com.example.SummerBuild.mapper;

import com.example.SummerBuild.dto.UserDto;
import com.example.SummerBuild.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper implements GenericMapper<User, UserDto> {

  @Override
  public UserDto toDto(User entity) {
    if (entity == null) {
      return null;
    }

    UserDto dto = new UserDto();
    dto.setId(entity.getId());
    dto.setUsername(entity.getUsername());
    dto.setEmail(entity.getEmail());
    dto.setCreatedAt(entity.getCreatedAt());
    dto.setUpdatedAt(entity.getUpdatedAt());
    return dto;
  }

  @Override
  public User toEntity(UserDto dto) {
    if (dto == null) {
      return null;
    }

    User entity = new User();
    entity.setId(dto.getId());
    entity.setUsername(dto.getUsername());
    entity.setEmail(dto.getEmail());
    entity.setCreatedAt(dto.getCreatedAt());
    entity.setUpdatedAt(dto.getUpdatedAt());
    return entity;
  }

  @Override
  public void updateEntityFromDto(UserDto dto, User entity) {
    if (dto == null || entity == null) {
      return;
    }

    entity.setUsername(dto.getUsername());
    entity.setEmail(dto.getEmail());
    entity.setUpdatedAt(dto.getUpdatedAt());
  }
}
