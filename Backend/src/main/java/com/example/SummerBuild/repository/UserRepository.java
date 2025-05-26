package com.example.SummerBuild.repository;

import com.example.SummerBuild.model.Gender;
import com.example.SummerBuild.model.User;
import com.example.SummerBuild.model.UserRole;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends GenericRepository<User, UUID> {
  Optional<User> findByName(String name);

  Optional<User> findByEmail(String email);

  List<User> findByRole(UserRole role);

  List<User> findByGender(Gender gender);

  boolean existsByEmail(String email);

  /**
   * Performs a case-insensitive search for users. EG: Searching for "john" will match "John",
   * "JOHN", "johnny", etc.
   *
   * @param name
   * @return
   */
  List<User> findByNameContainingIgnoreCase(String name);

  /**
   * Finds users created within a specific date range. Both start and end dates are inclusive.
   *
   * @param startDate
   * @param endDate
   * @return
   */
  @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
  List<User> findByCreatedBetween(
      @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  /**
   * Counts the total number of users with a specific role.
   *
   * @param role
   * @return
   */
  @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
  long countByRole(@Param("role") UserRole role);
}
