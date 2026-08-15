package com.haja.school.service;

import com.haja.school.endpoint.rest.model.ExamCreateRequest;
import com.haja.school.model.Exam;
import com.haja.school.model.Grade;
import com.haja.school.model.Role;
import com.haja.school.repository.JCourseRepository;
import com.haja.school.repository.JExamRepository;
import com.haja.school.repository.JGradeRepository;
import com.haja.school.repository.JTeacherCourseAssignmentRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCourse;
import com.haja.school.repository.model.JExam;
import com.haja.school.repository.model.JGrade;
import com.haja.school.repository.model.JUser;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class ExamService {

  private final JExamRepository examRepository;
  private final JCourseRepository courseRepository;
  private final JUserRepository userRepository;
  private final JTeacherCourseAssignmentRepository teacherCourseAssignmentRepository;
  private final JGradeRepository gradeRepository;

  public List<Exam> getExamsByCourse(UUID courseId, Integer academicYear, String callerEmail) {
    JUser caller =
        userRepository
            .findByEmail(callerEmail)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

    JCourse course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

    validateCourseAccess(caller, course);

    List<JExam> exams =
        academicYear != null
            ? examRepository.findByCourseIdAndAcademicYear(courseId, academicYear)
            : examRepository.findByCourseId(courseId);

    return exams.stream().map(this::toModel).toList();
  }

  public List<Grade> getGradesByExam(UUID examId, String callerEmail) {
    JUser caller =
        userRepository
            .findByEmail(callerEmail)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

    JExam exam =
        examRepository
            .findById(examId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));

    validateGradeAccess(caller, exam);

    return gradeRepository.findByExamId(examId).stream().map(this::toGradeModel).toList();
  }

  private void validateGradeAccess(JUser caller, JExam exam) {
    if (caller.getRole() == Role.ADMIN) {
      return;
    }

    if (caller.getRole() == Role.TEACHER) {
      boolean isAssigned =
          teacherCourseAssignmentRepository.existsByTeacherIdAndCourseId(
              caller.getId(), exam.getCourse().getId());
      if (!isAssigned) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Access denied: teacher is not assigned to the exam's course");
      }
      return;
    }

    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN, "Access denied: insufficient permissions");
  }

  @Transactional
  public Exam createExam(UUID courseId, ExamCreateRequest request, String callerEmail) {
    JUser caller =
        userRepository
            .findByEmail(callerEmail)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

    JCourse course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

    validateWriteAccess(caller, course);

    Double existingSum =
        examRepository.sumCoefficientByCourseIdAndAcademicYear(courseId, request.getAcademicYear());
    double newTotal = existingSum + request.getCoefficient();

    if (newTotal > 1.0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Total coefficient for this course and academic year would exceed 1.0 (current: "
              + existingSum
              + ")");
    }

    JExam jExam =
        JExam.builder()
            .course(course)
            .academicYear(request.getAcademicYear())
            .label(request.getLabel())
            .dateExam(request.getDateExam())
            .coefficient(request.getCoefficient())
            .build();

    return toModel(examRepository.save(jExam));
  }

  private void validateWriteAccess(JUser caller, JCourse course) {
    if (caller.getRole() == Role.ADMIN) {
      return;
    }

    if (caller.getRole() == Role.TEACHER) {
      boolean isAssigned =
          teacherCourseAssignmentRepository.existsByTeacherIdAndCourseId(
              caller.getId(), course.getId());
      if (!isAssigned) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Access denied: teacher is not assigned to this course");
      }
      return;
    }

    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN, "Access denied: insufficient permissions");
  }

  private void validateCourseAccess(JUser caller, JCourse course) {
    if (caller.getRole() == Role.ADMIN) {
      return;
    }

    if (caller.getRole() == Role.TEACHER) {
      boolean isAssigned =
          teacherCourseAssignmentRepository.existsByTeacherIdAndCourseId(
              caller.getId(), course.getId());
      if (!isAssigned) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Access denied: teacher is not assigned to this course");
      }
      return;
    }

    if (caller.getRole() == Role.STUDENT) {
      if (course.getTrack() != null
          && caller.getTrack() != null
          && course.getTrack() != caller.getTrack()) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Access denied: course track does not match student track");
      }

      Integer studentSemester = resolveStudentSemester(caller);
      if (studentSemester != null && course.getSemesterNumber() > studentSemester) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Access denied: course is not in student's current curriculum");
      }
    }
  }

  private Integer resolveStudentSemester(JUser student) {
    if (student.getCohort() == null || student.getCohort().getEntryYear() == null) {
      return null;
    }
    int entryYear = student.getCohort().getEntryYear();
    int currentYear = LocalDate.now().getYear();
    int currentMonth = LocalDate.now().getMonthValue();
    int yearsPassed = currentYear - entryYear;
    int computed = yearsPassed * 2 + (currentMonth >= 9 ? 1 : 0);
    return Math.max(1, Math.min(6, computed));
  }

  private Exam toModel(JExam jExam) {
    return Exam.builder()
        .id(jExam.getId())
        .courseId(jExam.getCourse().getId())
        .academicYear(jExam.getAcademicYear())
        .label(jExam.getLabel())
        .dateExam(jExam.getDateExam())
        .coefficient(jExam.getCoefficient())
        .build();
  }

  private Grade toGradeModel(JGrade grade) {
    return Grade.builder()
        .id(grade.getId())
        .examId(grade.getExam().getId())
        .studentId(grade.getStudent().getId())
        .value(grade.getValue())
        .enteredBy(grade.getEnteredBy() != null ? grade.getEnteredBy().getId() : null)
        .enteredAt(grade.getEnteredAt())
        .build();
  }
}
