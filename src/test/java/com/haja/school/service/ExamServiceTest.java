package com.haja.school.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.haja.school.model.Exam;
import com.haja.school.model.Role;
import com.haja.school.model.Track;
import com.haja.school.repository.JCourseRepository;
import com.haja.school.repository.JExamRepository;
import com.haja.school.repository.JTeacherCourseAssignmentRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCohort;
import com.haja.school.repository.model.JCourse;
import com.haja.school.repository.model.JExam;
import com.haja.school.repository.model.JUser;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
}
