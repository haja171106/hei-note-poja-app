package com.haja.school.repository;

import com.haja.school.repository.model.JGrade;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JGradeRepository extends JpaRepository<JGrade, UUID> {

  List<JGrade> findByStudentId(UUID studentId);
}
