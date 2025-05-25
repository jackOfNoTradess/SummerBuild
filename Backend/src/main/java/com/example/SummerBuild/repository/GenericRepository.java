package com.example.SummerBuild.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface GenericRepository<T, ID> extends JpaRepository<T, ID> {
  // No custom methods for now
  // Can add if there are common methods for all repositories
}
