package com.haja.school.repository;

import com.haja.school.repository.model.JGrade;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JGradeRepository extends JpaRepository<JGrade, UUID> {

  List<JGrade> findByStudentId(UUID studentId);

  List<JGrade> findByStudentIdIn(Collection<UUID> studentIds);

  List<JGrade> findByExamId(UUID examId);

  Optional<JGrade> findByExamIdAndStudentId(UUID examId, UUID studentId);
}
