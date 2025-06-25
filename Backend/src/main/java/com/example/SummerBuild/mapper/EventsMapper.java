package com.example.SummerBuild.mapper;

import com.example.SummerBuild.dto.EventsDto;
import com.example.SummerBuild.model.Events;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EventsMapper implements GenericMapper<Events, EventsDto> {

  @Override
  public EventsDto toDto(Events entity) {
    if (entity == null) {
      return null;
    }

    EventsDto dto = new EventsDto();
    dto.setId(entity.getId());
    dto.setTitle(entity.getTitle());
    dto.setHostUuid(entity.getHostId());
    dto.setCapacity(entity.getCapacity());
    dto.setStartTime(entity.getStartTime());
    dto.setEndTime(entity.getEndTime());
    dto.setDescription(entity.getDescription());
    dto.setTags(
        java.util.Arrays.asList(entity.getTags() != null ? entity.getTags() : new String[0]));
    dto.setCreatedAt(entity.getCreatedAt());
    dto.setUpdatedAt(entity.getUpdatedAt());

    return dto;
  }

  @Override
  public Events toEntity(EventsDto dto) {
    if (dto == null) {
      return null;
    }

    Events entity = new Events();
    // Only set ID if it exists (for updates), let JPA generate it for new entities
    if (dto.getId() != null) {
      entity.setId(dto.getId());
    }
    entity.setTitle(dto.getTitle());
    entity.setHostId(dto.getHostUuid());
    entity.setCapacity(dto.getCapacity());
    entity.setStartTime(dto.getStartTime());
    entity.setEndTime(dto.getEndTime());
    entity.setDescription(dto.getDescription());
    entity.setTags(dto.getTags() != null ? dto.getTags().toArray(new String[0]) : null);

    return entity;
  }

  @Override
  public void updateEntityFromDto(EventsDto dto, Events entity) {
    if (dto == null || entity == null) {
      return;
    }

    if (dto.getTitle() != null) {
      entity.setTitle(dto.getTitle());
    }
    if (dto.getCapacity() != null) {
      entity.setCapacity(dto.getCapacity());
    }
    if (dto.getStartTime() != null) {
      entity.setStartTime(dto.getStartTime());
    }
    if (dto.getEndTime() != null) {
      entity.setEndTime(dto.getEndTime());
    }
    if (dto.getDescription() != null) {
      entity.setDescription(dto.getDescription());
    }
    if (dto.getTags() != null) {
      entity.setTags(dto.getTags().toArray(new String[0]));
    }
  }
}
