package com.haja.school.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haja.school.endpoint.rest.model.GradeUpsertRequest;
import com.haja.school.model.Grade;
import com.haja.school.security.JwtAuthFilter;
import com.haja.school.security.JwtProvider;
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

@WebMvcTest(controllers = GradeController.class)
@AutoConfigureMockMvc(addFilters = false)
class GradeControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

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
  void getExamGrades_asAdmin_returnsOk() throws Exception {
    UUID examId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    Grade grade =
        Grade.builder()
            .id(UUID.randomUUID())
            .examId(examId)
            .studentId(studentId)
            .value(15.0)
            .enteredAt(Instant.parse("2025-10-15T12:00:00Z"))
            .build();
    when(examService.getGradesByExam(eq(examId), anyString())).thenReturn(List.of(grade));

    mockMvc
        .perform(get("/exams/" + examId + "/grades").with(mockUser("admin@admin.com", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].value").value(15.0))
        .andExpect(jsonPath("$[0].examId").value(examId.toString()));
  }

  @Test
  void upsertExamGrade_asAdmin_returnsOk() throws Exception {
    UUID examId = UUID.randomUUID();
    UUID studentId = UUID.randomUUID();
    GradeUpsertRequest request =
        GradeUpsertRequest.builder().value(16.5).reason("Correction exam").build();

    Grade grade =
        Grade.builder()
            .id(UUID.randomUUID())
            .examId(examId)
            .studentId(studentId)
            .value(16.5)
            .enteredBy(UUID.randomUUID())
            .enteredAt(Instant.parse("2025-10-15T12:00:00Z"))
            .build();
    when(examService.upsertGrade(
            eq(examId), eq(studentId), any(GradeUpsertRequest.class), anyString()))
        .thenReturn(grade);

    mockMvc
        .perform(
            put("/exams/" + examId + "/grades/" + studentId)
                .with(mockUser("admin@admin.com", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value(16.5));
  }
}
