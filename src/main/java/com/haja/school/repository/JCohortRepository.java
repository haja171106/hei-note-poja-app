package com.haja.school.repository;

import com.haja.school.repository.model.JCohort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface JCohortRepository extends JpaRepository<JCohort, UUID> {

  boolean existsByRef(String ref);

  Optional<JCohort> findByRef(String ref);

  @Query("SELECT COUNT(u) FROM JUser u WHERE u.cohort.id = :cohortId")
  long countStudentsByCohortId(UUID cohortId);

  @Query("SELECT COUNT(g) FROM JGroup g WHERE g.cohort.id = :cohortId")
  long countGroupsByCohortId(UUID cohortId);
}
