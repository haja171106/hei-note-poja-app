package com.haja.school.service;

import com.haja.school.model.CourseGradeSummary;
import com.haja.school.model.GradeHistory;
import com.haja.school.model.ReportStatus;
import com.haja.school.model.Role;
import com.haja.school.model.StudentGrades;
import com.haja.school.model.YearSummary;
import com.haja.school.repository.JCourseRepository;
import com.haja.school.repository.JExamRepository;
import com.haja.school.repository.JGradeHistoryRepository;
import com.haja.school.repository.JGradeRepository;
import com.haja.school.repository.JTeacherCourseAssignmentRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCourse;
import com.haja.school.repository.model.JExam;
import com.haja.school.repository.model.JGrade;
import com.haja.school.repository.model.JGradeHistory;
import com.haja.school.repository.model.JUser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class GradeCalculationService {

  private final JUserRepository userRepository;
  private final JCourseRepository courseRepository;
  private final JExamRepository examRepository;
  private final JGradeRepository gradeRepository;
  private final JTeacherCourseAssignmentRepository teacherCourseAssignmentRepository;
  private final JGradeHistoryRepository gradeHistoryRepository;

  public CourseGradeSummary computeCourseFinalGrade(
      UUID studentId, UUID courseId, int academicYear) {
    JCourse course =
        courseRepository
            .findById(courseId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
    return computeCourseGrade(studentId, course, academicYear);
  }

  public YearSummary computeYearSummary(UUID studentId, int year) {
    validateYear(year);
    JUser student =
        userRepository
            .findById(studentId)
            .filter(user -> user.getRole() == Role.STUDENT)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    Integer calendarYear = calendarAcademicYear(student, year);
    return buildSummary(student, year, findYearCourses(student, year), calendarYear);
  }

  public YearSummary getYearSummary(UUID studentId, int year, String callerEmail) {
    validateYear(year);
    JUser student =
        userRepository
            .findById(studentId)
            .filter(user -> user.getRole() == Role.STUDENT)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

    JUser caller =
        userRepository
            .findByEmail(callerEmail)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

    if (caller.getRole() == Role.ADMIN) {
      return computeYearSummary(studentId, year);
    }

    if (caller.getRole() == Role.STUDENT) {
      if (!caller.getId().equals(studentId)) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Access denied: student can only view their own summary");
      }
      return computeYearSummary(studentId, year);
    }

    if (caller.getRole() == Role.TEACHER) {
      return computeTeacherPartialSummary(student, year, caller);
    }

    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN, "Access denied: insufficient permissions");
  }

  private YearSummary computeTeacherPartialSummary(JUser student, int year, JUser teacher) {
    Integer calendarYear = calendarAcademicYear(student, year);
    List<JCourse> assignedCourses =
        findYearCourses(student, year).stream()
            .filter(course -> isAssignedToCourse(teacher, course, calendarYear))
            .toList();
    return buildSummary(student, year, assignedCourses, calendarYear);
  }

  private boolean isAssignedToCourse(JUser teacher, JCourse course, Integer calendarYear) {
    if (calendarYear != null) {
      return teacherCourseAssignmentRepository.existsByTeacherIdAndCourseIdAndAcademicYear(
          teacher.getId(), course.getId(), calendarYear);
    }
    return teacherCourseAssignmentRepository.existsByTeacherIdAndCourseId(
        teacher.getId(), course.getId());
  }

  public StudentGrades getStudentGrades(UUID studentId, Integer academicYear, String callerEmail) {
    JUser student =
        userRepository
            .findById(studentId)
            .filter(user -> user.getRole() == Role.STUDENT)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

    JUser caller =
        userRepository
            .findByEmail(callerEmail)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

    validateGradesAccess(caller, student);

    List<JCourse> courses = resolveStudentCourses(student, academicYear);

    if (caller.getRole() == Role.TEACHER) {
      courses =
          courses.stream()
              .filter(course -> isAssignedToCourse(caller, course, academicYear))
              .toList();
    }

    List<CourseGradeSummary> courseSummaries =
        courses.stream()
            .map(course -> computeCourseGrade(student.getId(), course, academicYear))
            .toList();

    return StudentGrades.builder()
        .studentId(student.getId())
        .academicYear(academicYear)
        .courses(courseSummaries)
        .build();
  }

  public List<GradeHistory> getStudentGradeHistory(UUID studentId, String callerEmail) {
    JUser student =
        userRepository
            .findById(studentId)
            .filter(user -> user.getRole() == Role.STUDENT)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

    JUser caller =
        userRepository
            .findByEmail(callerEmail)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

    validateHistoryAccess(caller, student);

    return gradeHistoryRepository.findByGrade_Student_IdOrderByChangedAtDesc(studentId).stream()
        .map(this::toGradeHistoryModel)
        .toList();
  }

  private void validateHistoryAccess(JUser caller, JUser student) {
    if (caller.getRole() == Role.ADMIN) {
      return;
    }

    if (caller.getRole() == Role.STUDENT) {
      if (!caller.getId().equals(student.getId())) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Access denied: student can only view their own grade history");
      }
      return;
    }

    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN, "Access denied: insufficient permissions");
  }

  private GradeHistory toGradeHistoryModel(JGradeHistory history) {
    return GradeHistory.builder()
        .id(history.getId())
        .gradeId(history.getGrade().getId())
        .oldValue(history.getOldValue())
        .newValue(history.getNewValue())
        .changedBy(history.getChangedBy() != null ? history.getChangedBy().getId() : null)
        .changedAt(history.getChangedAt())
        .reason(history.getReason())
        .build();
  }

  private void validateGradesAccess(JUser caller, JUser student) {
    if (caller.getRole() == Role.ADMIN) {
      return;
    }

    if (caller.getRole() == Role.STUDENT) {
      if (!caller.getId().equals(student.getId())) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Access denied: student can only view their own grades");
      }
      return;
    }

    if (caller.getRole() == Role.TEACHER) {
      return;
    }

    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN, "Access denied: insufficient permissions");
  }

  private List<JCourse> resolveStudentCourses(JUser student, Integer academicYear) {
    if (academicYear == null) {
      return findAllStudentCourses(student);
    }

    Integer entryYear = student.getCohort() != null ? student.getCohort().getEntryYear() : null;
    if (entryYear == null) {
      return findAllStudentCourses(student);
    }

    int studyYear = academicYear - entryYear + 1;
    if (studyYear < 1 || studyYear > 3) {
      return List.of();
    }
    return findYearCourses(student, studyYear);
  }

  private List<JCourse> findAllStudentCourses(JUser student) {
    List<JCourse> courses = new ArrayList<>();
    Set<UUID> seen = new HashSet<>();
    for (int year = 1; year <= 3; year++) {
      for (JCourse course : findYearCourses(student, year)) {
        if (seen.add(course.getId())) {
          courses.add(course);
        }
      }
    }
    return courses;
  }

  private YearSummary buildSummary(
      JUser student, int year, List<JCourse> courses, Integer calendarYear) {
    Map<UUID, Double> gradeByExam = loadGradeByExam(student.getId());
    List<CourseGradeSummary> courseSummaries = new ArrayList<>();
    int totalCredits = 0;
    double weightedSum = 0;
    int gradedCredits = 0;
    ReportStatus status = ReportStatus.COMPLETE;

    for (JCourse course : courses) {
      totalCredits += course.getCredit();

      double courseWeightedSum = 0;
      boolean hasGrade = false;
      for (JExam exam : findExams(course, calendarYear)) {
        Double value = gradeByExam.get(exam.getId());
        if (value == null) {
          status = ReportStatus.PROVISIONAL;
        } else {
          courseWeightedSum += value * exam.getCoefficient();
          hasGrade = true;
        }
      }

      Double finalGrade = hasGrade ? round2(courseWeightedSum) : null;
      courseSummaries.add(
          CourseGradeSummary.builder()
              .courseId(course.getId())
              .courseRef(course.getRef())
              .courseTitle(course.getTitle())
              .credit(course.getCredit())
              .finalGrade(finalGrade)
              .build());

      if (finalGrade != null) {
        weightedSum += finalGrade * course.getCredit();
        gradedCredits += course.getCredit();
      }
    }

    Double overallAverage = gradedCredits > 0 ? round2(weightedSum / gradedCredits) : null;

    return YearSummary.builder()
        .studentId(student.getId())
        .year(year)
        .courses(courseSummaries)
        .overallAverage(overallAverage)
        .totalCredits(totalCredits)
        .status(status)
        .build();
  }

  private CourseGradeSummary computeCourseGrade(
      UUID studentId, JCourse course, Integer academicYear) {
    Map<UUID, Double> gradeByExam = loadGradeByExam(studentId);
    double courseWeightedSum = 0;
    boolean hasGrade = false;
    for (JExam exam : findExams(course, academicYear)) {
      Double value = gradeByExam.get(exam.getId());
      if (value != null) {
        courseWeightedSum += value * exam.getCoefficient();
        hasGrade = true;
      }
    }

    return CourseGradeSummary.builder()
        .courseId(course.getId())
        .courseRef(course.getRef())
        .courseTitle(course.getTitle())
        .credit(course.getCredit())
        .finalGrade(hasGrade ? round2(courseWeightedSum) : null)
        .build();
  }

  private List<JCourse> findYearCourses(JUser student, int year) {
    List<JCourse> courses = new ArrayList<>();
    for (int semester : List.of(year * 2 - 1, year * 2)) {
      for (JCourse course : courseRepository.findBySemesterNumber(semester)) {
        if (isCourseExpectedForStudent(course, student)) {
          courses.add(course);
        }
      }
    }
    return courses;
  }

  private boolean isCourseExpectedForStudent(JCourse course, JUser student) {
    if (course.getSemesterNumber() < 4) {
      return true;
    }
    return course.getTrack() == null || course.getTrack() == student.getTrack();
  }

  private Integer calendarAcademicYear(JUser student, int year) {
    if (student.getCohort() == null || student.getCohort().getEntryYear() == null) {
      return null;
    }
    return student.getCohort().getEntryYear() + year - 1;
  }

  private List<JExam> findExams(JCourse course, Integer calendarYear) {
    if (calendarYear != null) {
      return examRepository.findByCourseIdAndAcademicYear(course.getId(), calendarYear);
    }
    return examRepository.findByCourseId(course.getId());
  }

  private Map<UUID, Double> loadGradeByExam(UUID studentId) {
    return gradeRepository.findByStudentId(studentId).stream()
        .filter(grade -> grade.getExam() != null)
        .collect(
            Collectors.toMap(
                grade -> grade.getExam().getId(),
                JGrade::getValue,
                (existing, replacement) -> replacement));
  }

  private void validateYear(int year) {
    if (year < 1 || year > 3) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "The 'year' parameter must be between 1 and 3");
    }
  }

  private double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
