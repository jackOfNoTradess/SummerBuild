package com.example.SummerBuild.repository;

import com.example.SummerBuild.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends GenericRepository<User, UUID> {
  // Add custom query methods here if needed
  Optional<User> findByName(String name);

  Optional<User> findByEmail(String email);
}
