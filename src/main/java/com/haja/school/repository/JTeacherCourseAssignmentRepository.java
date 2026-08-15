package com.haja.school.repository;

import com.haja.school.repository.model.JTeacherCourseAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JTeacherCourseAssignmentRepository
    extends JpaRepository<JTeacherCourseAssignment, UUID> {

  boolean existsByTeacherIdAndCourseIdAndAcademicYear(
      UUID teacherId, UUID courseId, Integer academicYear);

  boolean existsByTeacherIdAndCourseId(UUID teacherId, UUID courseId);

  Optional<JTeacherCourseAssignment> findByTeacherIdAndCourseIdAndAcademicYear(
      UUID teacherId, UUID courseId, Integer academicYear);

  List<JTeacherCourseAssignment> findByCourseId(UUID courseId);

  List<JTeacherCourseAssignment> findByTeacherId(UUID teacherId);
}
