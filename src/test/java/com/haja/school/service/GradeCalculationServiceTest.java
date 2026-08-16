package com.haja.school.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.haja.school.model.CourseGradeSummary;
import com.haja.school.model.GradeHistory;
import com.haja.school.model.ReportStatus;
import com.haja.school.model.Role;
import com.haja.school.model.StudentGrades;
import com.haja.school.model.Track;
import com.haja.school.model.YearSummary;
import com.haja.school.repository.JCourseRepository;
import com.haja.school.repository.JExamRepository;
import com.haja.school.repository.JGradeHistoryRepository;
import com.haja.school.repository.JGradeRepository;
import com.haja.school.repository.JTeacherCourseAssignmentRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCohort;
import com.haja.school.repository.model.JCourse;
import com.haja.school.repository.model.JExam;
import com.haja.school.repository.model.JGrade;
import com.haja.school.repository.model.JGradeHistory;
import com.haja.school.repository.model.JUser;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class GradeCalculationServiceTest {

  @Mock private JUserRepository userRepository;
  @Mock private JCourseRepository courseRepository;
  @Mock private JExamRepository examRepository;
  @Mock private JGradeRepository gradeRepository;
  @Mock private JTeacherCourseAssignmentRepository teacherCourseAssignmentRepository;
  @Mock private JGradeHistoryRepository gradeHistoryRepository;

  @InjectMocks private GradeCalculationService gradeCalculationService;

  private UUID studentId;
  private JUser student;
  private JCourse courseA;
  private JCourse courseB;
  private JExam examA1;
  private JExam examA2;
  private JExam examB1;

  @BeforeEach
  void setUp() {
    studentId = UUID.randomUUID();
    JCohort cohort = JCohort.builder().id(UUID.randomUUID()).ref("A").entryYear(2023).build();
    student =
        JUser.builder()
            .id(studentId)
            .email("hei.student@student.com")
            .role(Role.STUDENT)
            .track(null)
            .cohort(cohort)
            .build();

    courseA =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("ALGO")
            .title("Algorithmics")
            .credit(6)
            .semesterNumber(1)
            .track(null)
            .build();
    courseB =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("PROG")
            .title("Programming")
            .credit(4)
            .semesterNumber(2)
            .track(null)
            .build();

    examA1 = exam(courseA, 0.4);
    examA2 = exam(courseA, 0.6);
    examB1 = exam(courseB, 1.0);
  }

  @Test
  void computeCourseFinalGrade_weightedSum_notSimpleAverage() {
    when(courseRepository.findById(courseA.getId())).thenReturn(Optional.of(courseA));
    when(examRepository.findByCourseIdAndAcademicYear(courseA.getId(), 2023))
        .thenReturn(List.of(examA1, examA2));
    when(gradeRepository.findByStudentId(studentId))
        .thenReturn(List.of(grade(examA1, 12.0), grade(examA2, 18.0)));

    CourseGradeSummary summary =
        gradeCalculationService.computeCourseFinalGrade(studentId, courseA.getId(), 2023);

    assertNotNull(summary);
    assertEquals(15.6, summary.getFinalGrade());
    assertNotEquals(15.0, summary.getFinalGrade());
    assertEquals(6, summary.getCredit());
  }

  @Test
  void computeCourseFinalGrade_noGrade_returnsNullFinalGrade() {
    when(courseRepository.findById(courseA.getId())).thenReturn(Optional.of(courseA));
    when(examRepository.findByCourseIdAndAcademicYear(courseA.getId(), 2023))
        .thenReturn(List.of(examA1, examA2));
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of());

    CourseGradeSummary summary =
        gradeCalculationService.computeCourseFinalGrade(studentId, courseA.getId(), 2023);

    assertNotNull(summary);
    assertNull(summary.getFinalGrade());
  }

  @Test
  void computeCourseFinalGrade_courseNotFound_throwsNotFound() {
    when(courseRepository.findById(courseA.getId())).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                gradeCalculationService.computeCourseFinalGrade(studentId, courseA.getId(), 2023));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
  }

  @Test
  void computeYearSummary_creditWeightedAverage() {
    JExam examAOnly = exam(courseA, 1.0);
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(courseRepository.findBySemesterNumber(1)).thenReturn(List.of(courseA));
    when(courseRepository.findBySemesterNumber(2)).thenReturn(List.of(courseB));
    when(examRepository.findByCourseIdAndAcademicYear(courseA.getId(), 2023))
        .thenReturn(List.of(examAOnly));
    when(examRepository.findByCourseIdAndAcademicYear(courseB.getId(), 2023))
        .thenReturn(List.of(examB1));
    when(gradeRepository.findByStudentId(studentId))
        .thenReturn(List.of(grade(examAOnly, 10.0), grade(examB1, 20.0)));

    YearSummary summary = gradeCalculationService.computeYearSummary(studentId, 1);

    assertNotNull(summary);
    assertEquals(studentId, summary.getStudentId());
    assertEquals(1, summary.getYear());
    assertEquals(2, summary.getCourses().size());
    assertEquals(14.0, summary.getOverallAverage());
    assertEquals(10, summary.getTotalCredits());
    assertEquals(ReportStatus.COMPLETE, summary.getStatus());

    CourseGradeSummary courseASummary = summary.getCourses().get(0);
    assertEquals(10.0, courseASummary.getFinalGrade());
    CourseGradeSummary courseBSummary = summary.getCourses().get(1);
    assertEquals(20.0, courseBSummary.getFinalGrade());
  }

  @Test
  void computeYearSummary_statusProvisional_whenGradeMissing() {
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(courseRepository.findBySemesterNumber(1)).thenReturn(List.of(courseA));
    when(courseRepository.findBySemesterNumber(2)).thenReturn(List.of());
    when(examRepository.findByCourseIdAndAcademicYear(courseA.getId(), 2023))
        .thenReturn(List.of(examA1, examA2));
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of(grade(examA1, 12.0)));

    YearSummary summary = gradeCalculationService.computeYearSummary(studentId, 1);

    assertNotNull(summary);
    assertEquals(ReportStatus.PROVISIONAL, summary.getStatus());
    assertEquals(4.8, summary.getCourses().get(0).getFinalGrade());
  }

  @Test
  void computeYearSummary_trackSpecificCoursesExcludedFromSemester4() {
    JUser tnStudent =
        JUser.builder()
            .id(studentId)
            .role(Role.STUDENT)
            .track(Track.TN)
            .cohort(student.getCohort())
            .build();
    JCourse semester4El =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("ML-EL")
            .title("Machine Learning EL")
            .credit(5)
            .semesterNumber(4)
            .track(Track.EL)
            .build();
    JCourse semester4Common =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("SECU")
            .title("Security")
            .credit(3)
            .semesterNumber(4)
            .track(null)
            .build();

    when(userRepository.findById(studentId)).thenReturn(Optional.of(tnStudent));
    when(courseRepository.findBySemesterNumber(3)).thenReturn(List.of());
    when(courseRepository.findBySemesterNumber(4))
        .thenReturn(List.of(semester4El, semester4Common));
    when(examRepository.findByCourseIdAndAcademicYear(semester4Common.getId(), 2024))
        .thenReturn(List.of());
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of());

    YearSummary summary = gradeCalculationService.computeYearSummary(studentId, 2);

    assertNotNull(summary);
    assertEquals(1, summary.getCourses().size());
    assertEquals("SECU", summary.getCourses().get(0).getCourseRef());
    assertEquals(3, summary.getTotalCredits());
  }

  @Test
  void getYearSummary_asStudent_ownSummary_success() {
    JExam examAOnly = exam(courseA, 1.0);
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findByEmail("hei.student@student.com")).thenReturn(Optional.of(student));
    when(courseRepository.findBySemesterNumber(1)).thenReturn(List.of(courseA));
    when(courseRepository.findBySemesterNumber(2)).thenReturn(List.of(courseB));
    when(examRepository.findByCourseIdAndAcademicYear(courseA.getId(), 2023))
        .thenReturn(List.of(examAOnly));
    when(examRepository.findByCourseIdAndAcademicYear(courseB.getId(), 2023))
        .thenReturn(List.of(examB1));
    when(gradeRepository.findByStudentId(studentId))
        .thenReturn(List.of(grade(examAOnly, 10.0), grade(examB1, 20.0)));

    YearSummary summary =
        gradeCalculationService.getYearSummary(studentId, 1, "hei.student@student.com");

    assertNotNull(summary);
    assertEquals(14.0, summary.getOverallAverage());
  }

  @Test
  void getYearSummary_asStudent_otherStudent_throwsForbidden() {
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    JUser otherStudent =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.other@student.com")
            .role(Role.STUDENT)
            .build();
    when(userRepository.findByEmail("hei.other@student.com")).thenReturn(Optional.of(otherStudent));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> gradeCalculationService.getYearSummary(studentId, 1, "hei.other@student.com"));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
  }

  @Test
  void getYearSummary_asAdmin_success() {
    JExam examAOnly = exam(courseA, 1.0);
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findBySemesterNumber(1)).thenReturn(List.of(courseA));
    when(courseRepository.findBySemesterNumber(2)).thenReturn(List.of(courseB));
    when(examRepository.findByCourseIdAndAcademicYear(courseA.getId(), 2023))
        .thenReturn(List.of(examAOnly));
    when(examRepository.findByCourseIdAndAcademicYear(courseB.getId(), 2023))
        .thenReturn(List.of(examB1));
    when(gradeRepository.findByStudentId(studentId))
        .thenReturn(List.of(grade(examAOnly, 10.0), grade(examB1, 20.0)));

    YearSummary summary =
        gradeCalculationService.getYearSummary(studentId, 1, "hei.admin@admin.com");

    assertNotNull(summary);
    assertEquals(14.0, summary.getOverallAverage());
  }

  @Test
  void getYearSummary_asTeacher_limitedToAssignedCourses() {
    JUser teacher =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.teacher@teacher.com")
            .role(Role.TEACHER)
            .build();
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findByEmail("hei.teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(courseRepository.findBySemesterNumber(1)).thenReturn(List.of(courseA));
    when(courseRepository.findBySemesterNumber(2)).thenReturn(List.of(courseB));
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseIdAndAcademicYear(
            teacher.getId(), courseA.getId(), 2023))
        .thenReturn(true);
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseIdAndAcademicYear(
            teacher.getId(), courseB.getId(), 2023))
        .thenReturn(false);
    when(examRepository.findByCourseIdAndAcademicYear(courseA.getId(), 2023))
        .thenReturn(List.of(examA1));
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of());

    YearSummary summary =
        gradeCalculationService.getYearSummary(studentId, 1, "hei.teacher@teacher.com");

    assertNotNull(summary);
    assertEquals(1, summary.getCourses().size());
    assertEquals("ALGO", summary.getCourses().get(0).getCourseRef());
    assertEquals(6, summary.getTotalCredits());
  }

  @Test
  void getYearSummary_asTeacher_studentWithoutCohort_fallsBackToCourseOnlyExams() {
    JUser studentWithoutCohort =
        JUser.builder()
            .id(studentId)
            .email("hei.student@student.com")
            .role(Role.STUDENT)
            .track(null)
            .cohort(null)
            .build();
    JUser teacher =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.teacher@teacher.com")
            .role(Role.TEACHER)
            .build();

    when(userRepository.findById(studentId)).thenReturn(Optional.of(studentWithoutCohort));
    when(userRepository.findByEmail("hei.teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(courseRepository.findBySemesterNumber(1)).thenReturn(List.of(courseA));
    when(courseRepository.findBySemesterNumber(2)).thenReturn(List.of());
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseId(
            teacher.getId(), courseA.getId()))
        .thenReturn(true);
    when(examRepository.findByCourseId(courseA.getId())).thenReturn(List.of());
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of());

    YearSummary summary =
        gradeCalculationService.getYearSummary(studentId, 1, "hei.teacher@teacher.com");

    assertNotNull(summary);
    assertEquals(1, summary.getCourses().size());
    assertEquals(6, summary.getTotalCredits());
    assertNull(summary.getOverallAverage());
  }

  @Test
  void computeYearSummary_nonStudentUser_throwsNotFound() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> gradeCalculationService.computeYearSummary(admin.getId(), 1));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
  }

  @Test
  void getYearSummary_studentNotFound_throwsNotFound() {
    when(userRepository.findById(studentId)).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> gradeCalculationService.getYearSummary(studentId, 1, "hei.admin@admin.com"));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
  }

  @Test
  void getYearSummary_invalidYear_throwsBadRequest() {
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> gradeCalculationService.getYearSummary(studentId, 4, "hei.admin@admin.com"));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void getYearSummary_unknownCaller_throwsUnauthorized() {
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findByEmail("unknown@unknown.com")).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> gradeCalculationService.getYearSummary(studentId, 1, "unknown@unknown.com"));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
  }

  @Test
  void getStudentGrades_asAdmin_success() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findBySemesterNumber(1)).thenReturn(List.of(courseA));
    when(courseRepository.findBySemesterNumber(2)).thenReturn(List.of(courseB));
    when(examRepository.findByCourseIdAndAcademicYear(courseA.getId(), 2023))
        .thenReturn(List.of(examA1, examA2));
    when(examRepository.findByCourseIdAndAcademicYear(courseB.getId(), 2023))
        .thenReturn(List.of(examB1));
    when(gradeRepository.findByStudentId(studentId))
        .thenReturn(List.of(grade(examA1, 12.0), grade(examA2, 18.0), grade(examB1, 15.0)));

    StudentGrades grades =
        gradeCalculationService.getStudentGrades(studentId, 2023, "hei.admin@admin.com");

    assertNotNull(grades);
    assertEquals(studentId, grades.getStudentId());
    assertEquals(2023, grades.getAcademicYear());
    assertEquals(2, grades.getCourses().size());
    assertEquals(15.6, grades.getCourses().get(0).getFinalGrade());
    assertEquals(6, grades.getCourses().get(0).getCredit());
    assertEquals("ALGO", grades.getCourses().get(0).getCourseRef());
    assertEquals(15.0, grades.getCourses().get(1).getFinalGrade());
  }

  @Test
  void getStudentGrades_asAdmin_withoutAcademicYear_returnsAllCourses() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findBySemesterNumber(1)).thenReturn(List.of(courseA));
    when(courseRepository.findBySemesterNumber(2)).thenReturn(List.of(courseB));
    when(courseRepository.findBySemesterNumber(3)).thenReturn(List.of());
    when(courseRepository.findBySemesterNumber(4)).thenReturn(List.of());
    when(courseRepository.findBySemesterNumber(5)).thenReturn(List.of());
    when(courseRepository.findBySemesterNumber(6)).thenReturn(List.of());
    when(examRepository.findByCourseId(courseA.getId())).thenReturn(List.of(examA1, examA2));
    when(examRepository.findByCourseId(courseB.getId())).thenReturn(List.of(examB1));
    when(gradeRepository.findByStudentId(studentId))
        .thenReturn(List.of(grade(examA1, 12.0), grade(examA2, 18.0), grade(examB1, 15.0)));

    StudentGrades grades =
        gradeCalculationService.getStudentGrades(studentId, null, "hei.admin@admin.com");

    assertNotNull(grades);
    assertEquals(2, grades.getCourses().size());
    assertNull(grades.getAcademicYear());
    assertEquals(15.6, grades.getCourses().get(0).getFinalGrade());
  }

  @Test
  void getStudentGrades_asStudent_ownGrades_success() {
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findByEmail("hei.student@student.com")).thenReturn(Optional.of(student));
    when(courseRepository.findBySemesterNumber(1)).thenReturn(List.of(courseA));
    when(courseRepository.findBySemesterNumber(2)).thenReturn(List.of(courseB));
    when(examRepository.findByCourseIdAndAcademicYear(courseA.getId(), 2023))
        .thenReturn(List.of(examA1, examA2));
    when(examRepository.findByCourseIdAndAcademicYear(courseB.getId(), 2023))
        .thenReturn(List.of(examB1));
    when(gradeRepository.findByStudentId(studentId))
        .thenReturn(List.of(grade(examA1, 12.0), grade(examA2, 18.0), grade(examB1, 15.0)));

    StudentGrades grades =
        gradeCalculationService.getStudentGrades(studentId, 2023, "hei.student@student.com");

    assertNotNull(grades);
    assertEquals(2, grades.getCourses().size());
    assertEquals(15.6, grades.getCourses().get(0).getFinalGrade());
  }

  @Test
  void getStudentGrades_asStudent_otherStudent_throwsForbidden() {
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    JUser otherStudent =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.other@student.com")
            .role(Role.STUDENT)
            .build();
    when(userRepository.findByEmail("hei.other@student.com")).thenReturn(Optional.of(otherStudent));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                gradeCalculationService.getStudentGrades(studentId, 2023, "hei.other@student.com"));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
  }

  @Test
  void getStudentGrades_asTeacher_limitedToAssignedCourses() {
    JUser teacher =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.teacher@teacher.com")
            .role(Role.TEACHER)
            .build();
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findByEmail("hei.teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(courseRepository.findBySemesterNumber(1)).thenReturn(List.of(courseA));
    when(courseRepository.findBySemesterNumber(2)).thenReturn(List.of(courseB));
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseIdAndAcademicYear(
            teacher.getId(), courseA.getId(), 2023))
        .thenReturn(true);
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseIdAndAcademicYear(
            teacher.getId(), courseB.getId(), 2023))
        .thenReturn(false);
    when(examRepository.findByCourseIdAndAcademicYear(courseA.getId(), 2023))
        .thenReturn(List.of(examA1));
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of(grade(examA1, 10.0)));

    StudentGrades grades =
        gradeCalculationService.getStudentGrades(studentId, 2023, "hei.teacher@teacher.com");

    assertNotNull(grades);
    assertEquals(1, grades.getCourses().size());
    assertEquals("ALGO", grades.getCourses().get(0).getCourseRef());
    assertEquals(4.0, grades.getCourses().get(0).getFinalGrade());
  }

  @Test
  void getStudentGrades_academicYearOutOfRange_returnsEmptyCourses() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));

    StudentGrades grades =
        gradeCalculationService.getStudentGrades(studentId, 2020, "hei.admin@admin.com");

    assertNotNull(grades);
    assertEquals(0, grades.getCourses().size());
    assertEquals(2020, grades.getAcademicYear());
  }

  @Test
  void getStudentGrades_studentNotFound_throwsNotFound() {
    when(userRepository.findById(studentId)).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> gradeCalculationService.getStudentGrades(studentId, 2023, "hei.admin@admin.com"));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
  }

  @Test
  void getStudentGrades_unknownCaller_throwsUnauthorized() {
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findByEmail("unknown@unknown.com")).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> gradeCalculationService.getStudentGrades(studentId, 2023, "unknown@unknown.com"));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
  }

  @Test
  void getStudentGradeHistory_asAdmin_success() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    JUser grader =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.teacher@teacher.com")
            .role(Role.TEACHER)
            .build();
    JGrade grade = grade(examA1, 12.0);
    JGradeHistory history =
        JGradeHistory.builder()
            .id(UUID.randomUUID())
            .grade(grade)
            .oldValue(8.0)
            .newValue(12.0)
            .changedBy(grader)
            .changedAt(Instant.now())
            .reason("student appeal")
            .build();

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(gradeHistoryRepository.findByGrade_Student_IdOrderByChangedAtDesc(studentId))
        .thenReturn(List.of(history));

    List<GradeHistory> historyList =
        gradeCalculationService.getStudentGradeHistory(studentId, "hei.admin@admin.com");

    assertNotNull(historyList);
    assertEquals(1, historyList.size());
    assertEquals(grade.getId(), historyList.get(0).getGradeId());
    assertEquals(8.0, historyList.get(0).getOldValue());
    assertEquals(12.0, historyList.get(0).getNewValue());
    assertEquals(grader.getId(), historyList.get(0).getChangedBy());
    assertEquals("student appeal", historyList.get(0).getReason());
  }

  @Test
  void getStudentGradeHistory_asStudent_ownHistory_success() {
    JGrade grade = grade(examA1, 12.0);
    JGradeHistory history =
        JGradeHistory.builder()
            .id(UUID.randomUUID())
            .grade(grade)
            .oldValue(null)
            .newValue(12.0)
            .changedBy(null)
            .changedAt(Instant.now())
            .reason("initial entry")
            .build();

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findByEmail("hei.student@student.com")).thenReturn(Optional.of(student));
    when(gradeHistoryRepository.findByGrade_Student_IdOrderByChangedAtDesc(studentId))
        .thenReturn(List.of(history));

    List<GradeHistory> historyList =
        gradeCalculationService.getStudentGradeHistory(studentId, "hei.student@student.com");

    assertNotNull(historyList);
    assertEquals(1, historyList.size());
    assertNull(historyList.get(0).getOldValue());
    assertNull(historyList.get(0).getChangedBy());
  }

  @Test
  void getStudentGradeHistory_asStudent_otherStudent_throwsForbidden() {
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    JUser otherStudent =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.other@student.com")
            .role(Role.STUDENT)
            .build();
    when(userRepository.findByEmail("hei.other@student.com")).thenReturn(Optional.of(otherStudent));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                gradeCalculationService.getStudentGradeHistory(studentId, "hei.other@student.com"));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
  }

  @Test
  void getStudentGradeHistory_asTeacher_throwsForbidden() {
    JUser teacher =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.teacher@teacher.com")
            .role(Role.TEACHER)
            .build();
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findByEmail("hei.teacher@teacher.com")).thenReturn(Optional.of(teacher));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                gradeCalculationService.getStudentGradeHistory(
                    studentId, "hei.teacher@teacher.com"));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
  }

  @Test
  void getStudentGradeHistory_studentNotFound_throwsNotFound() {
    when(userRepository.findById(studentId)).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> gradeCalculationService.getStudentGradeHistory(studentId, "hei.admin@admin.com"));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
  }

  @Test
  void getStudentGradeHistory_unknownCaller_throwsUnauthorized() {
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.findByEmail("unknown@unknown.com")).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> gradeCalculationService.getStudentGradeHistory(studentId, "unknown@unknown.com"));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
  }

  @Test
  void computeTeacherPartialSummaryInMemory_limitedToAssignedCourses() {
    JUser teacher =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.teacher@teacher.com")
            .role(Role.TEACHER)
            .build();
    List<JCourse> allCourses = List.of(courseA, courseB);
    Map<UUID, List<JExam>> examsByCourse =
        Map.of(courseA.getId(), List.of(examA1), courseB.getId(), List.of(examB1));
    Map<UUID, Map<UUID, Double>> gradesByStudent = Map.of();
    Map<UUID, Set<Integer>> teacherAcademicYearsByCourse = Map.of(courseA.getId(), Set.of(2023));

    YearSummary summary =
        gradeCalculationService.computeTeacherPartialSummaryInMemory(
            student,
            1,
            teacher,
            allCourses,
            examsByCourse,
            gradesByStudent,
            teacherAcademicYearsByCourse);

    assertNotNull(summary);
    assertEquals(1, summary.getCourses().size());
    assertEquals("ALGO", summary.getCourses().get(0).getCourseRef());
    assertEquals(6, summary.getTotalCredits());
    assertNull(summary.getOverallAverage());
    assertEquals(ReportStatus.PROVISIONAL, summary.getStatus());
  }

  @Test
  void computeTeacherPartialSummaryInMemory_creditWeightedAverage() {
    JUser teacher =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.teacher@teacher.com")
            .role(Role.TEACHER)
            .build();
    List<JCourse> allCourses = List.of(courseA, courseB);
    Map<UUID, List<JExam>> examsByCourse =
        Map.of(courseA.getId(), List.of(examA1), courseB.getId(), List.of(examB1));
    Map<UUID, Map<UUID, Double>> gradesByStudent = Map.of(studentId, Map.of(examA1.getId(), 10.0));
    Map<UUID, Set<Integer>> teacherAcademicYearsByCourse = Map.of(courseA.getId(), Set.of(2023));

    YearSummary summary =
        gradeCalculationService.computeTeacherPartialSummaryInMemory(
            student,
            1,
            teacher,
            allCourses,
            examsByCourse,
            gradesByStudent,
            teacherAcademicYearsByCourse);

    assertNotNull(summary);
    assertEquals(1, summary.getCourses().size());
    assertEquals(4.0, summary.getCourses().get(0).getFinalGrade());
    assertEquals(4.0, summary.getOverallAverage());
    assertEquals(6, summary.getTotalCredits());
  }

  @Test
  void computeTeacherPartialSummaryInMemory_studentWithoutCohort_usesCourseOnlyExams() {
    JUser studentWithoutCohort =
        JUser.builder()
            .id(studentId)
            .email("hei.student@student.com")
            .role(Role.STUDENT)
            .track(null)
            .cohort(null)
            .build();
    JUser teacher =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.teacher@teacher.com")
            .role(Role.TEACHER)
            .build();
    List<JCourse> allCourses = List.of(courseA);
    Map<UUID, List<JExam>> examsByCourse = Map.of(courseA.getId(), List.of(examA1));
    Map<UUID, Map<UUID, Double>> gradesByStudent = Map.of(studentId, Map.of(examA1.getId(), 10.0));
    Map<UUID, Set<Integer>> teacherAcademicYearsByCourse = Map.of(courseA.getId(), Set.of(2023));

    YearSummary summary =
        gradeCalculationService.computeTeacherPartialSummaryInMemory(
            studentWithoutCohort,
            1,
            teacher,
            allCourses,
            examsByCourse,
            gradesByStudent,
            teacherAcademicYearsByCourse);

    assertNotNull(summary);
    assertEquals(1, summary.getCourses().size());
    assertEquals(4.0, summary.getOverallAverage());
    assertEquals(6, summary.getTotalCredits());
  }

  private JExam exam(JCourse course, double coefficient) {
    return JExam.builder()
        .id(UUID.randomUUID())
        .course(course)
        .academicYear(2023)
        .label("CC")
        .dateExam(Instant.now())
        .coefficient(coefficient)
        .build();
  }

  private JGrade grade(JExam exam, double value) {
    return JGrade.builder()
        .id(UUID.randomUUID())
        .exam(exam)
        .student(student)
        .value(value)
        .enteredAt(Instant.now())
        .build();
  }
}
