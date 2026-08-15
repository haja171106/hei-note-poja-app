package com.haja.school.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.haja.school.endpoint.rest.model.ExamCreateRequest;
import com.haja.school.endpoint.rest.model.GradeUpsertRequest;
import com.haja.school.model.Exam;
import com.haja.school.model.Grade;
import com.haja.school.model.Role;
import com.haja.school.model.Track;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

  @Mock private JExamRepository examRepository;
  @Mock private JCourseRepository courseRepository;
  @Mock private JUserRepository userRepository;
  @Mock private JTeacherCourseAssignmentRepository teacherCourseAssignmentRepository;
  @Mock private JGradeRepository gradeRepository;
  @Mock private JGradeHistoryRepository gradeHistoryRepository;

  @InjectMocks private ExamService examService;

  private UUID courseId;
  private JCourse course;
  private UUID examId;
  private JExam exam;

  @BeforeEach
  void setUp() {
    courseId = UUID.randomUUID();
    course =
        JCourse.builder()
            .id(courseId)
            .ref("PROG1")
            .title("Algorithms")
            .credit(5)
            .semesterNumber(1)
            .track(null)
            .build();

    examId = UUID.randomUUID();
    exam =
        JExam.builder()
            .id(examId)
            .course(course)
            .academicYear(2026)
            .label("CC1")
            .dateExam(Instant.now())
            .coefficient(0.4)
            .build();
  }

  @Test
  void getExamsByCourse_asAdmin_success() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(examRepository.findByCourseId(courseId)).thenReturn(List.of(exam));

    List<Exam> exams = examService.getExamsByCourse(courseId, null, "hei.admin@admin.com");

    assertNotNull(exams);
    assertEquals(1, exams.size());
    assertEquals(examId, exams.get(0).getId());
    assertEquals("CC1", exams.get(0).getLabel());
  }

  @Test
  void getExamsByCourse_withAcademicYear_success() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(examRepository.findByCourseIdAndAcademicYear(courseId, 2026)).thenReturn(List.of(exam));

    List<Exam> exams = examService.getExamsByCourse(courseId, 2026, "hei.admin@admin.com");

    assertNotNull(exams);
    assertEquals(1, exams.size());
    assertEquals(2026, exams.get(0).getAcademicYear());
  }

  @Test
  void getExamsByCourse_asTeacher_assigned_success() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).email("hei.teacher@teacher.com").role(Role.TEACHER).build();

    when(userRepository.findByEmail("hei.teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseId(teacherId, courseId))
        .thenReturn(true);
    when(examRepository.findByCourseId(courseId)).thenReturn(List.of(exam));

    List<Exam> exams = examService.getExamsByCourse(courseId, null, "hei.teacher@teacher.com");

    assertNotNull(exams);
    assertEquals(1, exams.size());
  }

  @Test
  void getExamsByCourse_asTeacher_notAssigned_throwsForbidden() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).email("hei.teacher@teacher.com").role(Role.TEACHER).build();

    when(userRepository.findByEmail("hei.teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseId(teacherId, courseId))
        .thenReturn(false);

    assertThrows(
        ResponseStatusException.class,
        () -> examService.getExamsByCourse(courseId, null, "hei.teacher@teacher.com"));
  }

  @Test
  void getExamsByCourse_asStudent_matchingCurriculum_success() {
    JCohort cohort = JCohort.builder().id(UUID.randomUUID()).ref("A").entryYear(2023).build();
    JUser student =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.student@student.com")
            .role(Role.STUDENT)
            .cohort(cohort)
            .track(Track.EL)
            .build();

    when(userRepository.findByEmail("hei.student@student.com")).thenReturn(Optional.of(student));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(examRepository.findByCourseId(courseId)).thenReturn(List.of(exam));

    List<Exam> exams = examService.getExamsByCourse(courseId, null, "hei.student@student.com");

    assertNotNull(exams);
    assertEquals(1, exams.size());
  }

  @Test
  void getExamsByCourse_asStudent_trackMismatch_throwsForbidden() {
    JCourse elCourse =
        JCourse.builder()
            .id(courseId)
            .ref("EL101")
            .title("Ecosystem")
            .semesterNumber(4)
            .track(Track.EL)
            .build();

    JCohort cohort = JCohort.builder().id(UUID.randomUUID()).ref("A").entryYear(2023).build();
    JUser student =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.student@student.com")
            .role(Role.STUDENT)
            .cohort(cohort)
            .track(Track.TN)
            .build();

    when(userRepository.findByEmail("hei.student@student.com")).thenReturn(Optional.of(student));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(elCourse));

    assertThrows(
        ResponseStatusException.class,
        () -> examService.getExamsByCourse(courseId, null, "hei.student@student.com"));
  }

  @Test
  void getExamsByCourse_courseNotFound_throwsNotFound() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class,
        () -> examService.getExamsByCourse(courseId, null, "hei.admin@admin.com"));
  }

  @Test
  void getExamsByCourse_userNotFound_throwsUnauthorized() {
    when(userRepository.findByEmail("unknown@admin.com")).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class,
        () -> examService.getExamsByCourse(courseId, null, "unknown@admin.com"));
  }

  @Test
  void getGradesByExam_asAdmin_success() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    JUser student =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.student@student.com")
            .role(Role.STUDENT)
            .build();
    JGrade grade =
        JGrade.builder()
            .id(UUID.randomUUID())
            .exam(exam)
            .student(student)
            .value(14.5)
            .enteredBy(admin)
            .enteredAt(Instant.now())
            .build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
    when(gradeRepository.findByExamId(examId)).thenReturn(List.of(grade));

    List<Grade> grades = examService.getGradesByExam(examId, "hei.admin@admin.com");

    assertNotNull(grades);
    assertEquals(1, grades.size());
    assertEquals(14.5, grades.get(0).getValue());
    assertEquals(examId, grades.get(0).getExamId());
    assertEquals(student.getId(), grades.get(0).getStudentId());
    assertEquals(admin.getId(), grades.get(0).getEnteredBy());
  }

  @Test
  void getGradesByExam_asTeacher_assigned_success() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).email("hei.teacher@teacher.com").role(Role.TEACHER).build();
    JUser student =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.student@student.com")
            .role(Role.STUDENT)
            .build();
    JGrade grade =
        JGrade.builder()
            .id(UUID.randomUUID())
            .exam(exam)
            .student(student)
            .value(12.0)
            .enteredAt(Instant.now())
            .build();

    when(userRepository.findByEmail("hei.teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseId(teacherId, courseId))
        .thenReturn(true);
    when(gradeRepository.findByExamId(examId)).thenReturn(List.of(grade));

    List<Grade> grades = examService.getGradesByExam(examId, "hei.teacher@teacher.com");

    assertNotNull(grades);
    assertEquals(1, grades.size());
    assertEquals(12.0, grades.get(0).getValue());
    assertNull(grades.get(0).getEnteredBy());
  }

  @Test
  void getGradesByExam_asTeacher_notAssigned_throwsForbidden() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).email("hei.teacher@teacher.com").role(Role.TEACHER).build();

    when(userRepository.findByEmail("hei.teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseId(teacherId, courseId))
        .thenReturn(false);

    assertThrows(
        ResponseStatusException.class,
        () -> examService.getGradesByExam(examId, "hei.teacher@teacher.com"));
  }

  @Test
  void getGradesByExam_asStudent_throwsForbidden() {
    JUser student =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.student@student.com")
            .role(Role.STUDENT)
            .build();

    when(userRepository.findByEmail("hei.student@student.com")).thenReturn(Optional.of(student));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

    assertThrows(
        ResponseStatusException.class,
        () -> examService.getGradesByExam(examId, "hei.student@student.com"));
  }

  @Test
  void getGradesByExam_examNotFound_throwsNotFound() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(examRepository.findById(examId)).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class,
        () -> examService.getGradesByExam(examId, "hei.admin@admin.com"));
  }

  @Test
  void getGradesByExam_userNotFound_throwsUnauthorized() {
    when(userRepository.findByEmail("unknown@admin.com")).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class,
        () -> examService.getGradesByExam(examId, "unknown@admin.com"));
  }

  @Test
  void createExam_asAdmin_success() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    ExamCreateRequest request =
        ExamCreateRequest.builder()
            .label("CC1")
            .dateExam(Instant.now())
            .coefficient(0.4)
            .academicYear(2026)
            .build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(examRepository.sumCoefficientByCourseIdAndAcademicYear(courseId, 2026)).thenReturn(0.0);
    when(examRepository.save(any())).thenReturn(exam);

    Exam result = examService.createExam(courseId, request, "hei.admin@admin.com");

    assertNotNull(result);
    assertEquals("CC1", result.getLabel());
  }

  @Test
  void createExam_coefficientExceeds1_throwsBadRequest() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    ExamCreateRequest request =
        ExamCreateRequest.builder()
            .label("Final")
            .dateExam(Instant.now())
            .coefficient(0.5)
            .academicYear(2026)
            .build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(examRepository.sumCoefficientByCourseIdAndAcademicYear(courseId, 2026)).thenReturn(0.7);

    assertThrows(
        ResponseStatusException.class,
        () -> examService.createExam(courseId, request, "hei.admin@admin.com"));
  }

  @Test
  void createExam_asTeacher_assigned_success() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).email("hei.teacher@teacher.com").role(Role.TEACHER).build();
    ExamCreateRequest request =
        ExamCreateRequest.builder()
            .label("CC2")
            .dateExam(Instant.now())
            .coefficient(0.3)
            .academicYear(2026)
            .build();

    when(userRepository.findByEmail("hei.teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseId(teacherId, courseId))
        .thenReturn(true);
    when(examRepository.sumCoefficientByCourseIdAndAcademicYear(courseId, 2026)).thenReturn(0.0);
    when(examRepository.save(any())).thenReturn(exam);

    Exam result = examService.createExam(courseId, request, "hei.teacher@teacher.com");

    assertNotNull(result);
  }

  @Test
  void createExam_asTeacher_notAssigned_throwsForbidden() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).email("hei.teacher@teacher.com").role(Role.TEACHER).build();
    ExamCreateRequest request =
        ExamCreateRequest.builder()
            .label("CC1")
            .dateExam(Instant.now())
            .coefficient(0.5)
            .academicYear(2026)
            .build();

    when(userRepository.findByEmail("hei.teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseId(teacherId, courseId))
        .thenReturn(false);

    assertThrows(
        ResponseStatusException.class,
        () -> examService.createExam(courseId, request, "hei.teacher@teacher.com"));
  }

  @Test
  void createExam_asStudent_throwsForbidden() {
    JUser student =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.student@student.com")
            .role(Role.STUDENT)
            .build();
    ExamCreateRequest request =
        ExamCreateRequest.builder()
            .label("CC1")
            .dateExam(Instant.now())
            .coefficient(0.5)
            .academicYear(2026)
            .build();

    when(userRepository.findByEmail("hei.student@student.com")).thenReturn(Optional.of(student));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

    assertThrows(
        ResponseStatusException.class,
        () -> examService.createExam(courseId, request, "hei.student@student.com"));
  }

  @Test
  void upsertGrade_asAdmin_newGrade_success() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    UUID studentId = UUID.randomUUID();
    JUser student =
        JUser.builder().id(studentId).email("hei.student@student.com").role(Role.STUDENT).build();
    GradeUpsertRequest request =
        GradeUpsertRequest.builder().value(14.5).reason("initial entry").build();
    JGrade grade = JGrade.builder().id(UUID.randomUUID()).exam(exam).student(student).build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(gradeRepository.findByExamIdAndStudentId(examId, studentId)).thenReturn(Optional.empty());
    when(gradeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Grade result = examService.upsertGrade(examId, studentId, request, "hei.admin@admin.com");

    assertNotNull(result);
    assertEquals(14.5, result.getValue());
    assertEquals(examId, result.getExamId());
    assertEquals(studentId, result.getStudentId());
    assertEquals(admin.getId(), result.getEnteredBy());

    ArgumentCaptor<JGradeHistory> historyCaptor = ArgumentCaptor.forClass(JGradeHistory.class);
    verify(gradeHistoryRepository).save(historyCaptor.capture());
    JGradeHistory history = historyCaptor.getValue();
    assertNull(history.getOldValue());
    assertEquals(14.5, history.getNewValue());
    assertEquals("initial entry", history.getReason());
    assertEquals(admin.getId(), history.getChangedBy().getId());
  }

  @Test
  void upsertGrade_asAdmin_updateExistingGrade_success() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    UUID studentId = UUID.randomUUID();
    JUser student =
        JUser.builder().id(studentId).email("hei.student@student.com").role(Role.STUDENT).build();
    GradeUpsertRequest request =
        GradeUpsertRequest.builder().value(16.0).reason("student appeal").build();
    JGrade existingGrade =
        JGrade.builder()
            .id(UUID.randomUUID())
            .exam(exam)
            .student(student)
            .value(12.0)
            .enteredBy(admin)
            .enteredAt(Instant.now())
            .build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(gradeRepository.findByExamIdAndStudentId(examId, studentId))
        .thenReturn(Optional.of(existingGrade));
    when(gradeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Grade result = examService.upsertGrade(examId, studentId, request, "hei.admin@admin.com");

    assertNotNull(result);
    assertEquals(16.0, result.getValue());

    ArgumentCaptor<JGradeHistory> historyCaptor = ArgumentCaptor.forClass(JGradeHistory.class);
    verify(gradeHistoryRepository).save(historyCaptor.capture());
    JGradeHistory history = historyCaptor.getValue();
    assertEquals(12.0, history.getOldValue());
    assertEquals(16.0, history.getNewValue());
    assertEquals("student appeal", history.getReason());
  }

  @Test
  void upsertGrade_asTeacher_assigned_success() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).email("hei.teacher@teacher.com").role(Role.TEACHER).build();
    UUID studentId = UUID.randomUUID();
    JUser student =
        JUser.builder().id(studentId).email("hei.student@student.com").role(Role.STUDENT).build();
    GradeUpsertRequest request =
        GradeUpsertRequest.builder().value(10.0).reason("initial entry").build();

    when(userRepository.findByEmail("hei.teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseId(teacherId, courseId))
        .thenReturn(true);
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(gradeRepository.findByExamIdAndStudentId(examId, studentId)).thenReturn(Optional.empty());
    when(gradeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Grade result = examService.upsertGrade(examId, studentId, request, "hei.teacher@teacher.com");

    assertNotNull(result);
    assertEquals(10.0, result.getValue());
    assertEquals(teacher.getId(), result.getEnteredBy());
    verify(gradeHistoryRepository).save(any());
  }

  @Test
  void upsertGrade_asTeacher_notAssigned_throwsForbidden() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).email("hei.teacher@teacher.com").role(Role.TEACHER).build();
    UUID studentId = UUID.randomUUID();
    GradeUpsertRequest request =
        GradeUpsertRequest.builder().value(14.0).reason("initial entry").build();

    when(userRepository.findByEmail("hei.teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseId(teacherId, courseId))
        .thenReturn(false);

    assertThrows(
        ResponseStatusException.class,
        () -> examService.upsertGrade(examId, studentId, request, "hei.teacher@teacher.com"));
  }

  @Test
  void upsertGrade_asStudent_throwsForbidden() {
    JUser student =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.student@student.com")
            .role(Role.STUDENT)
            .build();
    GradeUpsertRequest request =
        GradeUpsertRequest.builder().value(14.0).reason("initial entry").build();

    when(userRepository.findByEmail("hei.student@student.com")).thenReturn(Optional.of(student));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

    assertThrows(
        ResponseStatusException.class,
        () -> examService.upsertGrade(examId, student.getId(), request, "hei.student@student.com"));
  }

  @Test
  void upsertGrade_examNotFound_throwsNotFound() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    GradeUpsertRequest request =
        GradeUpsertRequest.builder().value(14.0).reason("initial entry").build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(examRepository.findById(examId)).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class,
        () -> examService.upsertGrade(examId, UUID.randomUUID(), request, "hei.admin@admin.com"));
  }

  @Test
  void upsertGrade_studentNotFound_throwsNotFound() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    UUID studentId = UUID.randomUUID();
    GradeUpsertRequest request =
        GradeUpsertRequest.builder().value(14.0).reason("initial entry").build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
    when(userRepository.findById(studentId)).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class,
        () -> examService.upsertGrade(examId, studentId, request, "hei.admin@admin.com"));
  }

  @Test
  void upsertGrade_callerNotFound_throwsUnauthorized() {
    GradeUpsertRequest request =
        GradeUpsertRequest.builder().value(14.0).reason("initial entry").build();

    when(userRepository.findByEmail("unknown@admin.com")).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class,
        () -> examService.upsertGrade(examId, UUID.randomUUID(), request, "unknown@admin.com"));
  }

  @Test
  void upsertGrade_invalidValue_throwsBadRequest() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    UUID studentId = UUID.randomUUID();
    JUser student =
        JUser.builder().id(studentId).email("hei.student@student.com").role(Role.STUDENT).build();
    GradeUpsertRequest request =
        GradeUpsertRequest.builder().value(25.0).reason("initial entry").build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

    assertThrows(
        ResponseStatusException.class,
        () -> examService.upsertGrade(examId, studentId, request, "hei.admin@admin.com"));
    verify(gradeRepository, never()).save(any());
    verify(gradeHistoryRepository, never()).save(any());
  }

  @Test
  void upsertGrade_missingReason_throwsBadRequest() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    UUID studentId = UUID.randomUUID();
    JUser student =
        JUser.builder().id(studentId).email("hei.student@student.com").role(Role.STUDENT).build();
    GradeUpsertRequest request = GradeUpsertRequest.builder().value(14.0).reason("   ").build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

    assertThrows(
        ResponseStatusException.class,
        () -> examService.upsertGrade(examId, studentId, request, "hei.admin@admin.com"));
    verify(gradeRepository, never()).save(any());
    verify(gradeHistoryRepository, never()).save(any());
  }
}
