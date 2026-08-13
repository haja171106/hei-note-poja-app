package com.haja.school.repository;

import com.haja.school.repository.model.JUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JUserRepository extends JpaRepository<JUser, UUID> {
  Optional<JUser> findByEmail(String email);

  boolean existsByEmail(String email);

  List<JUser> findByCohortId(UUID cohortId);
}
