package com.example.SummerBuild.mapper;

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
}
