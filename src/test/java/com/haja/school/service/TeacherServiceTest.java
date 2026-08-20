package com.haja.school.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.haja.school.endpoint.rest.model.TeacherCreateRequest;
import com.haja.school.model.Course;
import com.haja.school.model.Grade;
import com.haja.school.model.Role;
import com.haja.school.model.Student;
import com.haja.school.model.Teacher;
import com.haja.school.model.TeacherCourseBoard;
import com.haja.school.model.TeacherGradeBoard;
import com.haja.school.model.YearSummary;
import com.haja.school.repository.JCourseRepository;
import com.haja.school.repository.JExamRepository;
import com.haja.school.repository.JGradeRepository;
import com.haja.school.repository.JStudentGroupHistoryRepository;
import com.haja.school.repository.JTeacherCourseAssignmentRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCourse;
import com.haja.school.repository.model.JExam;
import com.haja.school.repository.model.JGrade;
import com.haja.school.repository.model.JTeacherCourseAssignment;
import com.haja.school.repository.model.JUser;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

  @Mock private JUserRepository userRepository;
  @Mock private JCourseRepository courseRepository;
  @Mock private JExamRepository examRepository;
  @Mock private JGradeRepository gradeRepository;
  @Mock private JTeacherCourseAssignmentRepository teacherCourseAssignmentRepository;
  @Mock private JStudentGroupHistoryRepository studentGroupHistoryRepository;
  @Mock private CourseService courseService;
  @Mock private GradeCalculationService gradeCalculationService;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private TeacherService teacherService;

  @Test
  void createTeacher_success() {
    TeacherCreateRequest request =
        TeacherCreateRequest.builder()
            .name("Smith")
            .firstname("John")
            .address("456 Avenue")
            .birthdate(LocalDate.of(1985, 5, 20))
            .build();

    when(userRepository.findByRefStartingWith("TCR")).thenReturn(Collections.emptyList());
    when(userRepository.existsByEmail("hei.john@teacher.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

    UUID savedUserId = UUID.randomUUID();
    when(userRepository.save(any(JUser.class)))
        .thenAnswer(
            invocation -> {
              JUser user = invocation.getArgument(0);
              user.setId(savedUserId);
              return user;
            });

    Teacher createdTeacher = teacherService.createTeacher(request);

    assertNotNull(createdTeacher);
    assertEquals(savedUserId, createdTeacher.getId());
    assertEquals("TCR00001", createdTeacher.getRef());
    assertEquals("Smith", createdTeacher.getName());
    assertEquals("John", createdTeacher.getFirstname());
    assertEquals("hei.john@teacher.com", createdTeacher.getEmail());

    verify(userRepository).save(any(JUser.class));
  }

  @Test
  void createTeacher_incrementsExistingTeacherRef() {
    TeacherCreateRequest request =
        TeacherCreateRequest.builder().name("Taylor").firstname("Sarah").build();

    JUser existingTeacher = JUser.builder().ref("TCR00012").role(Role.TEACHER).build();

    when(userRepository.findByRefStartingWith("TCR")).thenReturn(List.of(existingTeacher));
    when(userRepository.existsByEmail("hei.sarah@teacher.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(userRepository.save(any(JUser.class)))
        .thenAnswer(
            invocation -> {
              JUser u = invocation.getArgument(0);
              u.setId(UUID.randomUUID());
              return u;
            });

    Teacher createdTeacher = teacherService.createTeacher(request);

    assertEquals("TCR00013", createdTeacher.getRef());
    assertEquals("hei.sarah@teacher.com", createdTeacher.getEmail());
  }

  @Test
  void createTeacher_handlesEmailCollision() {
    TeacherCreateRequest request =
        TeacherCreateRequest.builder().name("Dupont").firstname("Jean").build();

    when(userRepository.findByRefStartingWith("TCR")).thenReturn(Collections.emptyList());
    when(userRepository.existsByEmail("hei.jean@teacher.com")).thenReturn(true);
    when(userRepository.existsByEmail("hei.jean1@teacher.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(userRepository.save(any(JUser.class)))
        .thenAnswer(
            invocation -> {
              JUser u = invocation.getArgument(0);
              u.setId(UUID.randomUUID());
              return u;
            });

    Teacher createdTeacher = teacherService.createTeacher(request);

    assertEquals("hei.jean1@teacher.com", createdTeacher.getEmail());
  }

  @Test
  void createTeacher_teacherRefWithInvalidFormat_handledGracefully() {
    TeacherCreateRequest request =
        TeacherCreateRequest.builder().name("Brown").firstname("Charlie").build();

    JUser invalidRefTeacher = JUser.builder().ref("TCRXYZ").role(Role.TEACHER).build();

    when(userRepository.findByRefStartingWith("TCR")).thenReturn(List.of(invalidRefTeacher));
    when(userRepository.existsByEmail("hei.charlie@teacher.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(userRepository.save(any(JUser.class)))
        .thenAnswer(
            invocation -> {
              JUser u = invocation.getArgument(0);
              u.setId(UUID.randomUUID());
              return u;
            });

    Teacher createdTeacher = teacherService.createTeacher(request);

    assertEquals("TCR00001", createdTeacher.getRef());
    assertEquals("hei.charlie@teacher.com", createdTeacher.getEmail());
  }

  @Test
  void createTeacher_emptyFirstname_generatesDefaultEmail() {
    TeacherCreateRequest request =
        TeacherCreateRequest.builder().name("Ghost").firstname("").build();

    when(userRepository.findByRefStartingWith("TCR")).thenReturn(Collections.emptyList());
    when(userRepository.existsByEmail("hei.teacher@teacher.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(userRepository.save(any(JUser.class)))
        .thenAnswer(
            invocation -> {
              JUser u = invocation.getArgument(0);
              u.setId(UUID.randomUUID());
              return u;
            });

    Teacher createdTeacher = teacherService.createTeacher(request);

    assertEquals("hei.teacher@teacher.com", createdTeacher.getEmail());
  }

  @Test
  void createTeacher_specialCharactersInName_generatesValidEmail() {
    TeacherCreateRequest request =
        TeacherCreateRequest.builder().name("O'Brien").firstname("Jean-Pierre").build();

    when(userRepository.findByRefStartingWith("TCR")).thenReturn(Collections.emptyList());
    when(userRepository.existsByEmail("hei.jeanpierre@teacher.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(userRepository.save(any(JUser.class)))
        .thenAnswer(
            invocation -> {
              JUser u = invocation.getArgument(0);
              u.setId(UUID.randomUUID());
              return u;
            });

    Teacher createdTeacher = teacherService.createTeacher(request);

    assertEquals("hei.jeanpierre@teacher.com", createdTeacher.getEmail());
  }

  @Test
  void getCoursesByTeacher_admin_success() {
    UUID teacherId = UUID.randomUUID();
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("admin@admin.com").role(Role.ADMIN).build();
    JUser teacher = JUser.builder().id(teacherId).role(Role.TEACHER).build();
    JCourse course =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("PROG1")
            .title("Programming 1")
            .credit(5)
            .semesterNumber(1)
            .build();

    when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin));
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
    when(courseRepository.findByTeacherId(teacherId)).thenReturn(List.of(course));

    List<Course> courses = teacherService.getCoursesByTeacher(teacherId, "admin@admin.com");

    assertEquals(1, courses.size());
    assertEquals("PROG1", courses.get(0).getRef());
  }

  @Test
  void getCoursesByTeacher_teacherSelf_success() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).email("teacher@teacher.com").role(Role.TEACHER).build();
    JCourse course =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("PROG1")
            .title("Programming 1")
            .credit(5)
            .semesterNumber(1)
            .build();

    when(userRepository.findByEmail("teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
    when(courseRepository.findByTeacherId(teacherId)).thenReturn(List.of(course));

    List<Course> courses = teacherService.getCoursesByTeacher(teacherId, "teacher@teacher.com");

    assertEquals(1, courses.size());
    assertEquals("PROG1", courses.get(0).getRef());
  }

  @Test
  void getCoursesByTeacher_teacherOther_forbidden() {
    UUID teacherId1 = UUID.randomUUID();
    UUID teacherId2 = UUID.randomUUID();
    JUser teacher1 =
        JUser.builder().id(teacherId1).email("teacher1@teacher.com").role(Role.TEACHER).build();

    when(userRepository.findByEmail("teacher1@teacher.com")).thenReturn(Optional.of(teacher1));

    assertThrows(
        ResponseStatusException.class,
        () -> teacherService.getCoursesByTeacher(teacherId2, "teacher1@teacher.com"));
  }

  @Test
  void getCoursesByTeacher_teacherNotFound_throwsException() {
    UUID teacherId = UUID.randomUUID();
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("admin@admin.com").role(Role.ADMIN).build();

    when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin));
    when(userRepository.findById(teacherId)).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class,
        () -> teacherService.getCoursesByTeacher(teacherId, "admin@admin.com"));
  }

  @Test
  void getTeacherGradeBoard_admin_success() {
    UUID teacherId = UUID.randomUUID();
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("admin@admin.com").role(Role.ADMIN).build();
    JUser teacher = JUser.builder().id(teacherId).role(Role.TEACHER).build();
    JCourse course =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("PROG1")
            .title("Programming 1")
            .credit(5)
            .semesterNumber(1)
            .build();
    JUser student =
        JUser.builder()
            .id(UUID.randomUUID())
            .ref("STD00001")
            .name("Doe")
            .firstname("Jane")
            .role(Role.STUDENT)
            .build();
    JExam exam =
        JExam.builder()
            .id(UUID.randomUUID())
            .course(course)
            .academicYear(2026)
            .label("CC")
            .dateExam(Instant.now())
            .coefficient(1.0)
            .build();
    JGrade grade =
        JGrade.builder()
            .id(UUID.randomUUID())
            .exam(exam)
            .student(student)
            .value(14.0)
            .enteredAt(Instant.now())
            .build();
    Student studentModel =
        Student.builder()
            .id(student.getId())
            .ref(student.getRef())
            .name(student.getName())
            .firstname(student.getFirstname())
            .build();

    when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin));
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
    when(courseRepository.findByTeacherId(teacherId)).thenReturn(List.of(course));
    when(userRepository.findByRole(Role.STUDENT)).thenReturn(List.of(student));
    when(studentGroupHistoryRepository.findByStudentIdInAndEndDateIsNull(any()))
        .thenReturn(Collections.emptyList());
    when(courseRepository.findAll()).thenReturn(List.of(course));
    when(examRepository.findAll()).thenReturn(List.of(exam));
    when(gradeRepository.findByStudentIdIn(any())).thenReturn(List.of(grade));
    when(teacherCourseAssignmentRepository.findByTeacherId(teacherId))
        .thenReturn(
            List.of(
                JTeacherCourseAssignment.builder()
                    .id(UUID.randomUUID())
                    .teacher(teacher)
                    .course(course)
                    .academicYear(2026)
                    .build()));
    when(courseService.filterStudentsForCourse(any(), any(), any()))
        .thenReturn(List.of(studentModel));
    when(gradeCalculationService.computeTeacherPartialSummaryInMemory(
            any(), anyInt(), any(), any(), any(), any(), any()))
        .thenReturn(YearSummary.builder().overallAverage(14.0).build());

    TeacherGradeBoard board = teacherService.getTeacherGradeBoard(teacherId, "admin@admin.com");

    assertNotNull(board);
    assertEquals(1, board.getCourses().size());
    TeacherCourseBoard courseBoard = board.getCourses().get(0);
    assertEquals("PROG1", courseBoard.getCourse().getRef());
    assertEquals(1, courseBoard.getStudents().size());
    assertEquals("STD00001", courseBoard.getStudents().get(0).getRef());
    assertEquals(1, courseBoard.getExams().size());
    assertEquals("CC", courseBoard.getExams().get(0).getLabel());
    assertEquals(1, courseBoard.getGrades().size());
    Grade gradeModel = courseBoard.getGrades().get(0);
    assertEquals(exam.getId(), gradeModel.getExamId());
    assertEquals(student.getId(), gradeModel.getStudentId());
    assertEquals(14.0, gradeModel.getValue());
    assertEquals(14.0, courseBoard.getStudentAverages().get(student.getId()));
  }

  @Test
  void getTeacherGradeBoard_nullAverage_success() {
    UUID teacherId = UUID.randomUUID();
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("admin@admin.com").role(Role.ADMIN).build();
    JUser teacher = JUser.builder().id(teacherId).role(Role.TEACHER).build();
    JCourse course =
        JCourse.builder()
            .id(UUID.randomUUID())
            .ref("PROG1")
            .title("Algorithms")
            .credit(4)
            .semesterNumber(1)
            .build();
    JUser student =
        JUser.builder()
            .id(UUID.randomUUID())
            .ref("STD00001")
            .name("Doe")
            .firstname("John")
            .role(Role.STUDENT)
            .build();
    Student studentModel =
        Student.builder()
            .id(student.getId())
            .ref(student.getRef())
            .name(student.getName())
            .firstname(student.getFirstname())
            .build();

    when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin));
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
    when(courseRepository.findByTeacherId(teacherId)).thenReturn(List.of(course));
    when(userRepository.findByRole(Role.STUDENT)).thenReturn(List.of(student));
    when(studentGroupHistoryRepository.findByStudentIdInAndEndDateIsNull(any()))
        .thenReturn(Collections.emptyList());
    when(courseRepository.findAll()).thenReturn(List.of(course));
    when(examRepository.findAll()).thenReturn(Collections.emptyList());
    when(gradeRepository.findByStudentIdIn(any())).thenReturn(Collections.emptyList());
    when(teacherCourseAssignmentRepository.findByTeacherId(teacherId))
        .thenReturn(Collections.emptyList());
    when(courseService.filterStudentsForCourse(any(), any(), any()))
        .thenReturn(List.of(studentModel));
    when(gradeCalculationService.computeTeacherPartialSummaryInMemory(
            any(), anyInt(), any(), any(), any(), any(), any()))
        .thenReturn(YearSummary.builder().overallAverage(null).build());

    TeacherGradeBoard board = teacherService.getTeacherGradeBoard(teacherId, "admin@admin.com");

    assertNotNull(board);
    assertEquals(1, board.getCourses().size());
    TeacherCourseBoard courseBoard = board.getCourses().get(0);
    assertNull(courseBoard.getStudentAverages().get(student.getId()));
  }

  @Test
  void getTeacherGradeBoard_noCourses_returnsEmptyBoard() {
    UUID teacherId = UUID.randomUUID();
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("admin@admin.com").role(Role.ADMIN).build();
    JUser teacher = JUser.builder().id(teacherId).role(Role.TEACHER).build();

    when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin));
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
    when(courseRepository.findByTeacherId(teacherId)).thenReturn(Collections.emptyList());

    TeacherGradeBoard board = teacherService.getTeacherGradeBoard(teacherId, "admin@admin.com");

    assertNotNull(board);
    assertTrue(board.getCourses().isEmpty());
  }

  @Test
  void getTeacherGradeBoard_teacherSelf_success() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).email("teacher@teacher.com").role(Role.TEACHER).build();

    when(userRepository.findByEmail("teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));
    when(courseRepository.findByTeacherId(teacherId)).thenReturn(Collections.emptyList());

    TeacherGradeBoard board = teacherService.getTeacherGradeBoard(teacherId, "teacher@teacher.com");

    assertNotNull(board);
    assertTrue(board.getCourses().isEmpty());
  }

  @Test
  void getTeacherGradeBoard_teacherOther_forbidden() {
    UUID teacherId1 = UUID.randomUUID();
    UUID teacherId2 = UUID.randomUUID();
    JUser teacher1 =
        JUser.builder().id(teacherId1).email("teacher1@teacher.com").role(Role.TEACHER).build();

    when(userRepository.findByEmail("teacher1@teacher.com")).thenReturn(Optional.of(teacher1));

    assertThrows(
        ResponseStatusException.class,
        () -> teacherService.getTeacherGradeBoard(teacherId2, "teacher1@teacher.com"));
  }

  @Test
  void getTeacherGradeBoard_teacherNotFound_throwsException() {
    UUID teacherId = UUID.randomUUID();
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("admin@admin.com").role(Role.ADMIN).build();

    when(userRepository.findByEmail("admin@admin.com")).thenReturn(Optional.of(admin));
    when(userRepository.findById(teacherId)).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class,
        () -> teacherService.getTeacherGradeBoard(teacherId, "admin@admin.com"));
  }
}
