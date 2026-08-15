package com.haja.school.repository;

import com.haja.school.repository.model.JExam;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JExamRepository extends JpaRepository<JExam, UUID> {

  List<JExam> findByCourseId(UUID courseId);

  List<JExam> findByCourseIdAndAcademicYear(UUID courseId, Integer academicYear);

  @Query(
      "SELECT COALESCE(SUM(e.coefficient), 0) FROM JExam e WHERE e.course.id = :courseId AND"
          + " e.academicYear = :academicYear")
  Double sumCoefficientByCourseIdAndAcademicYear(
      @Param("courseId") UUID courseId, @Param("academicYear") Integer academicYear);
}
