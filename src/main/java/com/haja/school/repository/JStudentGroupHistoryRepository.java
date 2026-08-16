package com.haja.school.repository;

import com.haja.school.repository.model.JStudentGroupHistory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JStudentGroupHistoryRepository extends JpaRepository<JStudentGroupHistory, UUID> {

  List<JStudentGroupHistory> findByGroupCohortIdAndEndDateIsNull(UUID cohortId);

  Optional<JStudentGroupHistory> findByStudentIdAndEndDateIsNull(UUID studentId);

  List<JStudentGroupHistory> findByStudentIdInAndEndDateIsNull(Collection<UUID> studentIds);
}
