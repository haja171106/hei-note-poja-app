package com.haja.school.repository;

import com.haja.school.repository.model.JGroup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface JGroupRepository extends JpaRepository<JGroup, UUID> {

  List<JGroup> findByCohortId(UUID cohortId);

  boolean existsByCohortId(UUID cohortId);

  @Query(
      "SELECT COUNT(sgh) FROM JStudentGroupHistory sgh WHERE sgh.group.id = :groupId AND"
          + " sgh.endDate IS NULL")
  long countActiveStudentsByGroupId(UUID groupId);
}
