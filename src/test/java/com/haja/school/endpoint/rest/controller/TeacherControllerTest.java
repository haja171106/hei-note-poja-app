package com.haja.school.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haja.school.endpoint.rest.model.TeacherCreateRequest;
import com.haja.school.model.Course;
import com.haja.school.model.Teacher;
import com.haja.school.model.TeacherGradeBoard;
import com.haja.school.security.JwtAuthFilter;
import com.haja.school.security.JwtProvider;
import com.haja.school.service.TeacherService;
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

@WebMvcTest(controllers = TeacherController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeacherControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private TeacherService teacherService;
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
  void createTeacher_asAdmin_returns201() throws Exception {
    TeacherCreateRequest request =
        TeacherCreateRequest.builder().name("Martin").firstname("Sophie").build();

    Teacher teacher =
        Teacher.builder()
            .id(UUID.randomUUID())
            .ref("TCR00001")
            .name("Martin")
            .firstname("Sophie")
            .email("hei.sophie@teacher.com")
            .build();
    when(teacherService.createTeacher(any(TeacherCreateRequest.class))).thenReturn(teacher);

    mockMvc
        .perform(
            post("/teachers")
                .with(mockUser("admin@admin.com", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.ref").value("TCR00001"))
        .andExpect(jsonPath("$.name").value("Martin"))
        .andExpect(jsonPath("$.firstname").value("Sophie"));
  }

  @Test
  void getCoursesByTeacher_asAdmin_returnsOk() throws Exception {
    UUID teacherId = UUID.randomUUID();
    Course course =
        Course.builder()
            .id(UUID.randomUUID())
            .ref("MATH101")
            .title("Mathematics")
            .credit(6)
            .semesterNumber(1)
            .build();
    when(teacherService.getCoursesByTeacher(eq(teacherId), anyString()))
        .thenReturn(List.of(course));

    mockMvc
        .perform(
            get("/teachers/" + teacherId + "/courses").with(mockUser("admin@admin.com", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].ref").value("MATH101"))
        .andExpect(jsonPath("$[0].title").value("Mathematics"));
  }

  @Test
  void getTeacherGradeBoard_asAdmin_returnsOk() throws Exception {
    UUID teacherId = UUID.randomUUID();
    TeacherGradeBoard board = TeacherGradeBoard.builder().courses(List.of()).build();
    when(teacherService.getTeacherGradeBoard(eq(teacherId), anyString())).thenReturn(board);

    mockMvc
        .perform(
            get("/teachers/" + teacherId + "/grades").with(mockUser("admin@admin.com", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.courses").isArray())
        .andExpect(jsonPath("$.courses.length()").value(0));
  }
}
