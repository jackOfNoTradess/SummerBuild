package com.example.SummerBuild.mapper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generic mapper interface for converting between entities and DTOs
 *
 * @param <E> Entity type
 * @param <D> DTO type
 */
public interface GenericMapper<E, D> {

  /**
   * Convert entity to DTO
   *
   * @param entity the entity to convert
   * @return the DTO
   */
  D toDto(E entity);

  /**
   * Convert DTO to entity
   *
   * @param dto the DTO to convert
   * @return the entity
   */
  E toEntity(D dto);

  /**
   * Update entity from DTO
   *
   * @param dto the DTO containing updated values
   * @param entity the entity to update
   */
  void updateEntityFromDto(D dto, E entity);

  /**
   * Convert list of entities to list of DTOs
   *
   * @param entities the list of entities to convert
   * @return the list of DTOs
   */
  default List<D> toDtoList(List<E> entities) {
    return entities.stream().map(this::toDto).collect(Collectors.toList());
  }

  /**
   * Convert list of DTOs to list of entities
   *
   * @param dtos the list of DTOs to convert
   * @return the list of entities
   */
  default List<E> toEntityList(List<D> dtos) {
    return dtos.stream().map(this::toEntity).collect(Collectors.toList());
  }
}
