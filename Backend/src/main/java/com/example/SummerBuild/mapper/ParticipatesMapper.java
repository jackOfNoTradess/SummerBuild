package com.example.SummerBuild.mapper;

import com.example.SummerBuild.dto.ParticipatesDto;
import com.example.SummerBuild.model.Participates;
import org.springframework.stereotype.Component;

@Component
public class ParticipatesMapper implements GenericMapper<Participates, ParticipatesDto> {

  @Override
  public ParticipatesDto toDto(Participates entity) {
    if (entity == null) {
      return null;
    }

    ParticipatesDto dto =
        ParticipatesDto.builder()
            .id(entity.getId())
            .userId(entity.getUserId())
            .eventId(entity.getEventId())
            .build();

    dto.setCreatedAt(entity.getCreatedAt());
    dto.setUpdatedAt(entity.getUpdatedAt());

    return dto;
  }

  @Override
  public Participates toEntity(ParticipatesDto dto) {
    if (dto == null) {
      return null;
    }

    Participates entity =
        Participates.builder()
            .id(dto.getId())
            .userId(dto.getUserId())
            .eventId(dto.getEventId())
            .build();

    entity.setCreatedAt(dto.getCreatedAt());
    entity.setUpdatedAt(dto.getUpdatedAt());

    return entity;
  }

  @Override
  public void updateEntityFromDto(ParticipatesDto dto, Participates entity) {
    if (dto == null || entity == null) {
      return;
    }

    entity.setUserId(dto.getUserId());
    entity.setEventId(dto.getEventId());
    entity.setUpdatedAt(dto.getUpdatedAt());
  }
}
