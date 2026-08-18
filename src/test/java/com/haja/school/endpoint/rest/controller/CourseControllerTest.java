package com.haja.school.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haja.school.endpoint.rest.model.CourseCreateRequest;
import com.haja.school.endpoint.rest.model.ExamCreateRequest;
import com.haja.school.endpoint.rest.model.TeacherAssignmentRequest;
import com.haja.school.model.Course;
import com.haja.school.model.Exam;
import com.haja.school.model.Student;
import com.haja.school.model.TeacherCourseAssignment;
import com.haja.school.security.JwtAuthFilter;
import com.haja.school.security.JwtProvider;
import com.haja.school.service.CourseService;
import com.haja.school.service.ExamService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private CourseService courseService;
  @MockBean private ExamService examService;
  @MockBean private JwtAuthFilter jwtAuthFilter;
  @MockBean private JwtProvider jwtProvider;

  private static RequestPostProcessor mockUser(String username, String... roles) {
    return (MockHttpServletRequest request) -> {
      List<SimpleGrantedAuthority> authorities =
          java.util.Arrays.stream(roles)
              .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
              .toList();
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(username, null, authorities);
      SecurityContextHolder.getContext().setAuthentication(auth);
      request.setUserPrincipal(auth);
      return request;
    };
  }

  @Test
  void getCourses_asAdmin_returnsOk() throws Exception {
    Course course =
        Course.builder()
            .id(UUID.randomUUID())
            .ref("MATH101")
            .title("Mathematics")
            .credit(6)
            .semesterNumber(1)
            .build();
    when(courseService.getCourses(anyString(), any(), any())).thenReturn(List.of(course));

    mockMvc
        .perform(get("/courses").with(mockUser("admin@admin.com", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].ref").value("MATH101"))
        .andExpect(jsonPath("$[0].title").value("Mathematics"));
  }

  @Test
  void createCourse_asAdmin_returns201() throws Exception {
    CourseCreateRequest request =
        CourseCreateRequest.builder()
            .ref("PHY101")
            .title("Physics")
            .credit(6)
            .semesterNumber(1)
            .build();

    Course created =
        Course.builder()
            .id(UUID.randomUUID())
            .ref("PHY101")
            .title("Physics")
            .credit(6)
            .semesterNumber(1)
            .build();
    when(courseService.createCourse(any(CourseCreateRequest.class))).thenReturn(created);

    mockMvc
        .perform(
            post("/courses")
                .with(mockUser("admin@admin.com", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.ref").value("PHY101"))
        .andExpect(jsonPath("$.title").value("Physics"));
  }

  @Test
  void getCourseStudents_asAdmin_returnsOk() throws Exception {
    UUID courseId = UUID.randomUUID();
    Student student =
        Student.builder()
            .id(UUID.randomUUID())
            .ref("STD00001")
            .name("Doe")
            .firstname("John")
            .email("hei.john@student.com")
            .build();
    when(courseService.getStudentsByCourse(eq(courseId), anyString())).thenReturn(List.of(student));

    mockMvc
        .perform(
            get("/courses/" + courseId + "/students").with(mockUser("admin@admin.com", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ref").value("STD00001"));
  }

  @Test
  void assignTeacherToCourse_asAdmin_returns201() throws Exception {
    UUID courseId = UUID.randomUUID();
    TeacherAssignmentRequest request =
        TeacherAssignmentRequest.builder().teacherId(UUID.randomUUID()).academicYear(2025).build();

    TeacherCourseAssignment assignment =
        TeacherCourseAssignment.builder()
            .id(UUID.randomUUID())
            .teacherId(request.getTeacherId())
            .courseId(courseId)
            .academicYear(2025)
            .build();
    when(courseService.assignTeacherToCourse(eq(courseId), any(TeacherAssignmentRequest.class)))
        .thenReturn(assignment);

    mockMvc
        .perform(
            post("/courses/" + courseId + "/teachers")
                .with(mockUser("admin@admin.com", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.academicYear").value(2025));
  }

  @Test
  void getCourseExams_asAdmin_returnsOk() throws Exception {
    UUID courseId = UUID.randomUUID();
    Exam exam =
        Exam.builder()
            .id(UUID.randomUUID())
            .courseId(courseId)
            .label("Midterm")
            .coefficient(0.3)
            .academicYear(2025)
            .dateExam(Instant.parse("2025-10-15T10:00:00Z"))
            .build();
    when(examService.getExamsByCourse(eq(courseId), anyInt(), anyString()))
        .thenReturn(List.of(exam));

    mockMvc
        .perform(
            get("/courses/" + courseId + "/exams")
                .with(mockUser("admin@admin.com", "ADMIN"))
                .param("academicYear", "2025"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].label").value("Midterm"))
        .andExpect(jsonPath("$[0].coefficient").value(0.3));
  }

  @Test
  void createExam_asAdmin_returns201() throws Exception {
    UUID courseId = UUID.randomUUID();
    ExamCreateRequest request =
        ExamCreateRequest.builder()
            .label("Final")
            .dateExam(Instant.parse("2026-01-20T10:00:00Z"))
            .coefficient(0.5)
            .academicYear(2025)
            .build();

    Exam created =
        Exam.builder()
            .id(UUID.randomUUID())
            .courseId(courseId)
            .label("Final")
            .coefficient(0.5)
            .academicYear(2025)
            .dateExam(Instant.parse("2026-01-20T10:00:00Z"))
            .build();
    when(examService.createExam(eq(courseId), any(ExamCreateRequest.class), anyString()))
        .thenReturn(created);

    mockMvc
        .perform(
            post("/courses/" + courseId + "/exams")
                .with(mockUser("admin@admin.com", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.label").value("Final"))
        .andExpect(jsonPath("$.coefficient").value(0.5));
  }
}
