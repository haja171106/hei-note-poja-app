package com.haja.school.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.haja.school.endpoint.rest.model.CourseCreateRequest;
import com.haja.school.endpoint.rest.model.TeacherAssignmentRequest;
import com.haja.school.model.Course;
import com.haja.school.model.Role;
import com.haja.school.model.Student;
import com.haja.school.model.TeacherCourseAssignment;
import com.haja.school.model.Track;
import com.haja.school.repository.JCourseRepository;
import com.haja.school.repository.JStudentGroupHistoryRepository;
import com.haja.school.repository.JTeacherCourseAssignmentRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCohort;
import com.haja.school.repository.model.JCourse;
import com.haja.school.repository.model.JTeacherCourseAssignment;
import com.haja.school.repository.model.JUser;
import java.time.LocalDate;
import java.util.Collections;
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
class CourseServiceTest {

  @Mock private JCourseRepository courseRepository;
  @Mock private JUserRepository userRepository;
  @Mock private JTeacherCourseAssignmentRepository teacherCourseAssignmentRepository;
  @Mock private JStudentGroupHistoryRepository studentGroupHistoryRepository;

  @InjectMocks private CourseService courseService;

  private UUID courseId;
  private JCourse sampleCourse;

  @BeforeEach
  void setUp() {
    courseId = UUID.randomUUID();
    sampleCourse =
        JCourse.builder()
            .id(courseId)
            .ref("PROG1")
            .title("Algorithms and Data Structures")
            .credit(5)
            .semesterNumber(1)
            .track(null)
            .build();
  }

  @Test
  void getCourses_admin_returnsAll() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("admin@admin.com").role(Role.ADMIN).build();
    when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findAll()).thenReturn(List.of(sampleCourse));

    List<Course> result = courseService.getCourses("admin@admin.com", null, null);

    assertEquals(1, result.size());
    assertEquals("PROG1", result.get(0).getRef());
  }

  @Test
  void getCourses_teacher_returnsAssigned() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).email("teacher@teacher.com").role(Role.TEACHER).build();
    when(userRepository.findByEmail("teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(courseRepository.findByTeacherId(teacherId)).thenReturn(List.of(sampleCourse));

    List<Course> result = courseService.getCourses("teacher@teacher.com", null, null);

    assertEquals(1, result.size());
    assertEquals("PROG1", result.get(0).getRef());
  }

  @Test
  void getCourses_student_restrictedToSemesterAndTrack() {
    JCohort cohort = JCohort.builder().entryYear(2025).build();
    JUser student =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("student@student.com")
            .role(Role.STUDENT)
            .cohort(cohort)
            .track(Track.EL)
            .build();
    when(userRepository.findByEmail("student@student.com")).thenReturn(Optional.of(student));
    when(courseRepository.findBySemesterNumberAndTrack(anyInt(), eq(Track.EL)))
        .thenReturn(List.of(sampleCourse));

    List<Course> result = courseService.getCourses("student@student.com", null, null);

    assertFalse(result.isEmpty());
  }

  @Test
  void createCourse_success() {
    CourseCreateRequest request =
        CourseCreateRequest.builder()
            .ref("MATH1")
            .title("Mathematics")
            .credit(6)
            .semesterNumber(1)
            .track(null)
            .build();

    when(courseRepository.existsByRef("MATH1")).thenReturn(false);
    when(courseRepository.findBySemesterNumber(1)).thenReturn(Collections.emptyList());
    when(courseRepository.findBySemesterNumber(2)).thenReturn(Collections.emptyList());
    when(courseRepository.save(any(JCourse.class)))
        .thenAnswer(
            invocation -> {
              JCourse c = invocation.getArgument(0);
              c.setId(UUID.randomUUID());
              return c;
            });

    Course created = courseService.createCourse(request);

    assertNotNull(created);
    assertEquals("MATH1", created.getRef());
    assertEquals(6, created.getCredit());
    assertEquals(1, created.getSemesterNumber());
  }

  @Test
  void createCourse_duplicateRef_throwsException() {
    CourseCreateRequest request =
        CourseCreateRequest.builder()
            .ref("PROG1")
            .title("Programming")
            .credit(5)
            .semesterNumber(1)
            .build();

    when(courseRepository.existsByRef("PROG1")).thenReturn(true);

    assertThrows(ResponseStatusException.class, () -> courseService.createCourse(request));
  }

  @Test
  void createCourse_exceeds30CreditsInSemester_throwsException() {
    CourseCreateRequest request =
        CourseCreateRequest.builder()
            .ref("PROG2")
            .title("Programming 2")
            .credit(10)
            .semesterNumber(1)
            .build();

    JCourse existingCourse = JCourse.builder().credit(25).semesterNumber(1).build();

    when(courseRepository.existsByRef("PROG2")).thenReturn(false);
    when(courseRepository.findBySemesterNumber(1)).thenReturn(List.of(existingCourse));

    assertThrows(ResponseStatusException.class, () -> courseService.createCourse(request));
  }

  @Test
  void createCourse_exceeds60CreditsInYear_throwsException() {
    CourseCreateRequest request =
        CourseCreateRequest.builder()
            .ref("PROG2")
            .title("Programming 2")
            .credit(5)
            .semesterNumber(1)
            .build();

    JCourse s1Course = JCourse.builder().credit(25).semesterNumber(1).build();
    JCourse s2Course = JCourse.builder().credit(31).semesterNumber(2).build();

    when(courseRepository.existsByRef("PROG2")).thenReturn(false);
    when(courseRepository.findBySemesterNumber(1)).thenReturn(List.of(s1Course));
    when(courseRepository.findBySemesterNumber(2)).thenReturn(List.of(s2Course));

    assertThrows(ResponseStatusException.class, () -> courseService.createCourse(request));
  }

  @Test
  void assignTeacherToCourse_success() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher = JUser.builder().id(teacherId).role(Role.TEACHER).build();
    TeacherAssignmentRequest request =
        TeacherAssignmentRequest.builder().teacherId(teacherId).academicYear(2026).build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(sampleCourse));
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseIdAndAcademicYear(
            teacherId, courseId, 2026))
        .thenReturn(false);
    when(teacherCourseAssignmentRepository.save(any(JTeacherCourseAssignment.class)))
        .thenAnswer(
            invocation -> {
              JTeacherCourseAssignment a = invocation.getArgument(0);
              a.setId(UUID.randomUUID());
              return a;
            });

    TeacherCourseAssignment assignment = courseService.assignTeacherToCourse(courseId, request);

    assertNotNull(assignment);
    assertEquals(teacherId, assignment.getTeacherId());
    assertEquals(courseId, assignment.getCourseId());
    assertEquals(2026, assignment.getAcademicYear());
  }

  @Test
  void assignTeacherToCourse_courseNotFound_throwsException() {
    UUID teacherId = UUID.randomUUID();
    TeacherAssignmentRequest request =
        TeacherAssignmentRequest.builder().teacherId(teacherId).academicYear(2026).build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class,
        () -> courseService.assignTeacherToCourse(courseId, request));
  }

  @Test
  void assignTeacherToCourse_teacherNotFound_throwsException() {
    UUID teacherId = UUID.randomUUID();
    TeacherAssignmentRequest request =
        TeacherAssignmentRequest.builder().teacherId(teacherId).academicYear(2026).build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(sampleCourse));
    when(userRepository.findById(teacherId)).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class,
        () -> courseService.assignTeacherToCourse(courseId, request));
  }

  @Test
  void assignTeacherToCourse_notATeacher_throwsException() {
    UUID studentId = UUID.randomUUID();
    JUser student = JUser.builder().id(studentId).role(Role.STUDENT).build();
    TeacherAssignmentRequest request =
        TeacherAssignmentRequest.builder().teacherId(studentId).academicYear(2026).build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(sampleCourse));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

    assertThrows(
        ResponseStatusException.class,
        () -> courseService.assignTeacherToCourse(courseId, request));
  }

  @Test
  void assignTeacherToCourse_alreadyAssigned_throwsException() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher = JUser.builder().id(teacherId).role(Role.TEACHER).build();
    TeacherAssignmentRequest request =
        TeacherAssignmentRequest.builder().teacherId(teacherId).academicYear(2026).build();

    when(courseRepository.findById(courseId)).thenReturn(Optional.of(sampleCourse));
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseIdAndAcademicYear(
            teacherId, courseId, 2026))
        .thenReturn(true);

    assertThrows(
        ResponseStatusException.class,
        () -> courseService.assignTeacherToCourse(courseId, request));
  }

  @Test
  void getStudentsByCourse_asAdmin_returnsMatchingStudents() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("admin@admin.com").role(Role.ADMIN).build();
    JUser studentYear1 =
        JUser.builder()
            .id(UUID.randomUUID())
            .ref("STD00001")
            .name("Doe")
            .firstname("Jane")
            .role(Role.STUDENT)
            .cohort(
                JCohort.builder().id(UUID.randomUUID()).entryYear(entryYearForStudyYear(1)).build())
            .build();
    JUser studentYear2 =
        JUser.builder()
            .id(UUID.randomUUID())
            .ref("STD00002")
            .name("Smith")
            .firstname("John")
            .role(Role.STUDENT)
            .cohort(
                JCohort.builder().id(UUID.randomUUID()).entryYear(entryYearForStudyYear(2)).build())
            .build();

    when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(sampleCourse));
    when(userRepository.findByRole(Role.STUDENT)).thenReturn(List.of(studentYear1, studentYear2));
    when(studentGroupHistoryRepository.findByStudentIdAndEndDateIsNull(any()))
        .thenReturn(Optional.empty());

    List<Student> result = courseService.getStudentsByCourse(courseId, "admin@admin.com");

    assertEquals(1, result.size());
    assertEquals(studentYear1.getId(), result.get(0).getId());
    assertEquals("STD00001", result.get(0).getRef());
  }

  @Test
  void getStudentsByCourse_asTeacher_assigned_success() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).email("teacher@teacher.com").role(Role.TEACHER).build();
    JUser student =
        JUser.builder()
            .id(UUID.randomUUID())
            .ref("STD00001")
            .role(Role.STUDENT)
            .cohort(
                JCohort.builder().id(UUID.randomUUID()).entryYear(entryYearForStudyYear(1)).build())
            .build();

    when(userRepository.findByEmail("teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(sampleCourse));
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseId(teacherId, courseId))
        .thenReturn(true);
    when(userRepository.findByRole(Role.STUDENT)).thenReturn(List.of(student));
    when(studentGroupHistoryRepository.findByStudentIdAndEndDateIsNull(any()))
        .thenReturn(Optional.empty());

    List<Student> result = courseService.getStudentsByCourse(courseId, "teacher@teacher.com");

    assertEquals(1, result.size());
    assertEquals(student.getId(), result.get(0).getId());
  }

  @Test
  void getStudentsByCourse_asTeacher_notAssigned_throwsForbidden() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).email("teacher@teacher.com").role(Role.TEACHER).build();

    when(userRepository.findByEmail("teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(sampleCourse));
    when(teacherCourseAssignmentRepository.existsByTeacherIdAndCourseId(teacherId, courseId))
        .thenReturn(false);

    assertThrows(
        ResponseStatusException.class,
        () -> courseService.getStudentsByCourse(courseId, "teacher@teacher.com"));
  }

  @Test
  void getStudentsByCourse_asStudent_throwsForbidden() {
    JUser student =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("student@student.com")
            .role(Role.STUDENT)
            .build();

    when(userRepository.findByEmail("student@student.com")).thenReturn(Optional.of(student));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(sampleCourse));

    assertThrows(
        ResponseStatusException.class,
        () -> courseService.getStudentsByCourse(courseId, "student@student.com"));
  }

  @Test
  void getStudentsByCourse_courseNotFound_throwsNotFound() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("admin@admin.com").role(Role.ADMIN).build();

    when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findById(courseId)).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class,
        () -> courseService.getStudentsByCourse(courseId, "admin@admin.com"));
  }

  @Test
  void getStudentsByCourse_callerNotFound_throwsUnauthorized() {
    when(userRepository.findByEmail("unknown@unknown.com")).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class,
        () -> courseService.getStudentsByCourse(courseId, "unknown@unknown.com"));
  }

  @Test
  void getStudentsByCourse_trackMismatch_excludesStudent() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("admin@admin.com").role(Role.ADMIN).build();
    JCourse trackCourse =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("ML-TN")
            .title("Machine Learning TN")
            .credit(5)
            .semesterNumber(4)
            .track(Track.TN)
            .build();
    JUser tnStudent =
        JUser.builder()
            .id(UUID.randomUUID())
            .ref("STD00001")
            .role(Role.STUDENT)
            .track(Track.TN)
            .cohort(
                JCohort.builder().id(UUID.randomUUID()).entryYear(entryYearForStudyYear(2)).build())
            .build();
    JUser elStudent =
        JUser.builder()
            .id(UUID.randomUUID())
            .ref("STD00002")
            .role(Role.STUDENT)
            .track(Track.EL)
            .cohort(
                JCohort.builder().id(UUID.randomUUID()).entryYear(entryYearForStudyYear(2)).build())
            .build();

    when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findById(trackCourse.getId())).thenReturn(Optional.of(trackCourse));
    when(userRepository.findByRole(Role.STUDENT)).thenReturn(List.of(tnStudent, elStudent));
    when(studentGroupHistoryRepository.findByStudentIdAndEndDateIsNull(any()))
        .thenReturn(Optional.empty());

    List<Student> result =
        courseService.getStudentsByCourse(trackCourse.getId(), "admin@admin.com");

    assertEquals(1, result.size());
    assertEquals("STD00001", result.get(0).getRef());
  }

  private int entryYearForStudyYear(int studyYear) {
    int currentYear = LocalDate.now().getYear();
    int increment = LocalDate.now().getMonthValue() >= 9 ? 1 : 0;
    return currentYear - studyYear + increment;
  }
}
