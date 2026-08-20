package com.haja.school.repository;

import com.haja.school.model.Role;
import com.haja.school.repository.model.JUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JUserRepository extends JpaRepository<JUser, UUID> {
  Optional<JUser> findByEmail(String email);

  @EntityGraph(attributePaths = "cohort")
  Optional<JUser> findWithCohortById(UUID id);

  boolean existsByEmail(String email);

  boolean existsByRef(String ref);

  List<JUser> findByCohortId(UUID cohortId);

  @EntityGraph(attributePaths = "cohort")
  List<JUser> findByRole(Role role);

  List<JUser> findByRefStartingWith(String prefix);
}
