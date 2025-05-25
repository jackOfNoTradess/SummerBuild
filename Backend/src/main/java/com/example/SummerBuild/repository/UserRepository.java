package com.example.SummerBuild.repository;

import com.example.SummerBuild.model.User;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends GenericRepository<User, Long> {
  // Add custom query methods here if needed
  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);
}
