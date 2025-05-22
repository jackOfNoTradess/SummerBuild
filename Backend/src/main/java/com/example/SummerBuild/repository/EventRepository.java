package com.example.SummerBuild.repository;

import com.example.SummerBuild.model.Event;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
  // You can add custom query methods here if needed
}
