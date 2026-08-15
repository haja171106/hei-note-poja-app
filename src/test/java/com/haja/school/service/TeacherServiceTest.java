package com.haja.school.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.haja.school.endpoint.rest.model.TeacherCreateRequest;
import com.haja.school.model.Course;
import com.haja.school.model.Role;
import com.haja.school.model.Teacher;
import com.haja.school.repository.JCourseRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCourse;
import com.haja.school.repository.model.JUser;
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
}
