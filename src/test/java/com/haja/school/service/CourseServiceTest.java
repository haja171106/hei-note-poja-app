package com.haja.school.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.haja.school.endpoint.rest.model.CourseCreateRequest;
import com.haja.school.model.Course;
import com.haja.school.model.Role;
import com.haja.school.model.Track;
import com.haja.school.repository.JCourseRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCohort;
import com.haja.school.repository.model.JCourse;
import com.haja.school.repository.model.JUser;
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
}
