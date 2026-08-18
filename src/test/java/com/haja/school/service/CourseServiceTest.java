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
import com.haja.school.repository.model.JGroup;
import com.haja.school.repository.model.JStudentGroupHistory;
import com.haja.school.repository.model.JTeacherCourseAssignment;
import com.haja.school.repository.model.JUser;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  void getCourses_admin_withSemester_filterBySemester() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("admin@admin.com").role(Role.ADMIN).build();
    JCourse sem2Course =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("MATH2")
            .title("Linear Algebra")
            .credit(4)
            .semesterNumber(2)
            .track(null)
            .build();

    when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findBySemesterNumber(2)).thenReturn(List.of(sem2Course));

    List<Course> result = courseService.getCourses("admin@admin.com", 2, null);

    assertEquals(1, result.size());
    assertEquals("MATH2", result.get(0).getRef());
    verify(courseRepository).findBySemesterNumber(2);
    verify(courseRepository, never()).findAll();
  }

  @Test
  void getCourses_admin_withTrack_filterByTrack() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("admin@admin.com").role(Role.ADMIN).build();
    JCourse elCourse =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("ML-EL")
            .title("Machine Learning EL")
            .credit(5)
            .semesterNumber(4)
            .track(Track.EL)
            .build();

    when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findByTrack(Track.EL)).thenReturn(List.of(elCourse));

    List<Course> result = courseService.getCourses("admin@admin.com", null, Track.EL);

    assertEquals(1, result.size());
    assertEquals("ML-EL", result.get(0).getRef());
    verify(courseRepository).findByTrack(Track.EL);
    verify(courseRepository, never()).findAll();
  }

  @Test
  void getCourses_admin_withSemesterAndTrack_filterBoth() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("admin@admin.com").role(Role.ADMIN).build();
    JCourse specificCourse =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("ML-TN-4")
            .title("Machine Learning TN Sem4")
            .credit(5)
            .semesterNumber(4)
            .track(Track.TN)
            .build();

    when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findBySemesterNumberAndTrack(4, Track.TN))
        .thenReturn(List.of(specificCourse));

    List<Course> result = courseService.getCourses("admin@admin.com", 4, Track.TN);

    assertEquals(1, result.size());
    assertEquals("ML-TN-4", result.get(0).getRef());
    verify(courseRepository).findBySemesterNumberAndTrack(4, Track.TN);
    verify(courseRepository, never()).findAll();
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
  void getCourses_teacher_withSemester_filter() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).email("teacher@teacher.com").role(Role.TEACHER).build();
    JCourse sem2Course =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("STAT2")
            .title("Statistics")
            .credit(4)
            .semesterNumber(2)
            .build();

    when(userRepository.findByEmail("teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(courseRepository.findByTeacherIdAndSemester(teacherId, 2)).thenReturn(List.of(sem2Course));

    List<Course> result = courseService.getCourses("teacher@teacher.com", 2, null);

    assertEquals(1, result.size());
    assertEquals("STAT2", result.get(0).getRef());
    verify(courseRepository).findByTeacherIdAndSemester(teacherId, 2);
  }

  @Test
  void getCourses_teacher_withTrack_filter() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).email("teacher@teacher.com").role(Role.TEACHER).build();
    JCourse tnCourse =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("SEC-TN")
            .title("Security TN")
            .credit(5)
            .semesterNumber(4)
            .track(Track.TN)
            .build();

    when(userRepository.findByEmail("teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(courseRepository.findByTeacherIdAndTrack(teacherId, Track.TN))
        .thenReturn(List.of(tnCourse));

    List<Course> result = courseService.getCourses("teacher@teacher.com", null, Track.TN);

    assertEquals(1, result.size());
    assertEquals("SEC-TN", result.get(0).getRef());
    verify(courseRepository).findByTeacherIdAndTrack(teacherId, Track.TN);
  }

  @Test
  void getCourses_teacher_withSemesterAndTrack_filter() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).email("teacher@teacher.com").role(Role.TEACHER).build();
    JCourse specificCourse =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("SEC-TN-4")
            .title("Security TN Sem4")
            .credit(5)
            .semesterNumber(4)
            .track(Track.TN)
            .build();

    when(userRepository.findByEmail("teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(courseRepository.findByTeacherIdAndSemesterAndTrack(teacherId, 4, Track.TN))
        .thenReturn(List.of(specificCourse));

    List<Course> result = courseService.getCourses("teacher@teacher.com", 4, Track.TN);

    assertEquals(1, result.size());
    assertEquals("SEC-TN-4", result.get(0).getRef());
    verify(courseRepository).findByTeacherIdAndSemesterAndTrack(teacherId, 4, Track.TN);
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
  void getCourses_student_withSemesterOverride() {
    JCohort cohort = JCohort.builder().entryYear(2024).build();
    JUser student =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("student@student.com")
            .role(Role.STUDENT)
            .cohort(cohort)
            .track(null)
            .build();

    JCourse sem2Course =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("MATH2")
            .title("Linear Algebra")
            .credit(4)
            .semesterNumber(2)
            .build();

    when(userRepository.findByEmail("student@student.com")).thenReturn(Optional.of(student));
    when(courseRepository.findBySemesterNumber(2)).thenReturn(List.of(sem2Course));

    List<Course> result = courseService.getCourses("student@student.com", 2, null);

    assertEquals(1, result.size());
    assertEquals("MATH2", result.get(0).getRef());
    verify(courseRepository).findBySemesterNumber(2);
  }

  @Test
  void getCourses_student_withTrackOverride() {
    JUser student =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("student@student.com")
            .role(Role.STUDENT)
            .cohort(null)
            .track(null)
            .build();

    JCourse tnCourse =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("ML-TN")
            .title("Machine Learning TN")
            .credit(5)
            .semesterNumber(4)
            .track(Track.TN)
            .build();

    when(userRepository.findByEmail("student@student.com")).thenReturn(Optional.of(student));
    when(courseRepository.findByTrack(Track.TN)).thenReturn(List.of(tnCourse));

    List<Course> result = courseService.getCourses("student@student.com", null, Track.TN);

    assertEquals(1, result.size());
    assertEquals("ML-TN", result.get(0).getRef());
    verify(courseRepository).findByTrack(Track.TN);
  }

  @Test
  void getCourses_callerNotFound_throwsException() {
    when(userRepository.findByEmail("unknown@unknown.com")).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class,
        () -> courseService.getCourses("unknown@unknown.com", null, null));
  }

  @Test
  void getCourses_invalidSemester_throwsBadRequest() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("admin@admin.com").role(Role.ADMIN).build();
    when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin));

    assertThrows(
        ResponseStatusException.class, () -> courseService.getCourses("admin@admin.com", 0, null));

    assertThrows(
        ResponseStatusException.class, () -> courseService.getCourses("admin@admin.com", 7, null));
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
  void createCourse_nullCredit_throwsBadRequest() {
    CourseCreateRequest request =
        CourseCreateRequest.builder()
            .ref("NULL1")
            .title("Null Credit")
            .credit(null)
            .semesterNumber(1)
            .build();

    assertThrows(ResponseStatusException.class, () -> courseService.createCourse(request));
  }

  @Test
  void createCourse_zeroCredit_throwsBadRequest() {
    CourseCreateRequest request =
        CourseCreateRequest.builder()
            .ref("ZERO1")
            .title("Zero Credit")
            .credit(0)
            .semesterNumber(1)
            .build();

    assertThrows(ResponseStatusException.class, () -> courseService.createCourse(request));
  }

  @Test
  void createCourse_negativeCredit_throwsBadRequest() {
    CourseCreateRequest request =
        CourseCreateRequest.builder()
            .ref("NEG1")
            .title("Negative Credit")
            .credit(-5)
            .semesterNumber(1)
            .build();

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

  @Test
  void getStudentsByCourse_withActiveGroupHistory_returnsGroupId() {
    UUID studentId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("admin@admin.com").role(Role.ADMIN).build();
    JUser student =
        JUser.builder()
            .id(studentId)
            .ref("STD00001")
            .name("Doe")
            .firstname("Jane")
            .role(Role.STUDENT)
            .cohort(
                JCohort.builder().id(UUID.randomUUID()).entryYear(entryYearForStudyYear(1)).build())
            .build();
    JGroup group = JGroup.builder().id(groupId).ref("A1").cohort(cohort).build();
    JStudentGroupHistory history =
        JStudentGroupHistory.builder()
            .id(UUID.randomUUID())
            .student(student)
            .group(group)
            .startDate(LocalDate.of(2025, 9, 1))
            .build();

    when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findById(courseId)).thenReturn(Optional.of(sampleCourse));
    when(userRepository.findByRole(Role.STUDENT)).thenReturn(List.of(student));
    when(studentGroupHistoryRepository.findByStudentIdAndEndDateIsNull(studentId))
        .thenReturn(Optional.of(history));

    List<Student> result = courseService.getStudentsByCourse(courseId, "admin@admin.com");

    assertEquals(1, result.size());
    assertEquals(groupId, result.get(0).getGroupId());
    assertEquals("STD00001", result.get(0).getRef());
  }

  @Test
  void getStudentsByCourse_courseWithNullTrack_semester4_returnsAllStudents() {
    UUID sem4CourseId = UUID.randomUUID();
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("admin@admin.com").role(Role.ADMIN).build();
    JCourse sem4Course =
        JCourse.builder()
            .id(sem4CourseId)
            .ref("ML-ALL")
            .title("Machine Learning All")
            .credit(5)
            .semesterNumber(4)
            .track(null)
            .build();
    JUser elStudent =
        JUser.builder()
            .id(UUID.randomUUID())
            .ref("STD00001")
            .name("Doe")
            .firstname("Jane")
            .role(Role.STUDENT)
            .track(Track.EL)
            .cohort(
                JCohort.builder().id(UUID.randomUUID()).entryYear(entryYearForStudyYear(2)).build())
            .build();
    JUser tnStudent =
        JUser.builder()
            .id(UUID.randomUUID())
            .ref("STD00002")
            .name("Smith")
            .firstname("John")
            .role(Role.STUDENT)
            .track(Track.TN)
            .cohort(
                JCohort.builder().id(UUID.randomUUID()).entryYear(entryYearForStudyYear(2)).build())
            .build();
    JUser noTrackStudent =
        JUser.builder()
            .id(UUID.randomUUID())
            .ref("STD00003")
            .name("Brown")
            .firstname("Alex")
            .role(Role.STUDENT)
            .track(null)
            .cohort(
                JCohort.builder().id(UUID.randomUUID()).entryYear(entryYearForStudyYear(2)).build())
            .build();

    when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin));
    when(courseRepository.findById(sem4CourseId)).thenReturn(Optional.of(sem4Course));
    when(userRepository.findByRole(Role.STUDENT))
        .thenReturn(List.of(elStudent, tnStudent, noTrackStudent));
    when(studentGroupHistoryRepository.findByStudentIdAndEndDateIsNull(any()))
        .thenReturn(Optional.empty());

    List<Student> result = courseService.getStudentsByCourse(sem4CourseId, "admin@admin.com");

    assertEquals(3, result.size());
  }

  @Test
  void filterStudentsForCourse_filtersCorrectly() {
    UUID groupId1 = UUID.randomUUID();
    UUID groupId2 = UUID.randomUUID();

    JCourse course =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("PROG1")
            .title("Programming")
            .credit(5)
            .semesterNumber(1)
            .track(null)
            .build();

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

    Map<UUID, UUID> groupIdByStudent = new HashMap<>();
    groupIdByStudent.put(studentYear1.getId(), groupId1);
    groupIdByStudent.put(studentYear2.getId(), groupId2);

    List<Student> result =
        courseService.filterStudentsForCourse(
            course, List.of(studentYear1, studentYear2), groupIdByStudent);

    assertEquals(1, result.size());
    assertEquals("STD00001", result.get(0).getRef());
    assertEquals(groupId1, result.get(0).getGroupId());
  }

  @Test
  void filterStudentsForCourse_withTrackFilter_includesMatchingStudents() {
    UUID sem4CourseId = UUID.randomUUID();
    JCourse course =
        JCourse.builder()
            .id(sem4CourseId)
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

    List<Student> result =
        courseService.filterStudentsForCourse(course, List.of(tnStudent, elStudent), Map.of());

    assertEquals(1, result.size());
    assertEquals("STD00001", result.get(0).getRef());
  }

  private JCohort cohort = JCohort.builder().id(UUID.randomUUID()).ref("A").entryYear(2024).build();

  private int entryYearForStudyYear(int studyYear) {
    int currentYear = LocalDate.now().getYear();
    int increment = LocalDate.now().getMonthValue() >= 9 ? 1 : 0;
    return currentYear - studyYear + increment;
  }
}
