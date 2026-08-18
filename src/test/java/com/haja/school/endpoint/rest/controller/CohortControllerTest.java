package com.haja.school.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haja.school.endpoint.rest.model.CreateCohortRequest;
import com.haja.school.model.Cohort;
import com.haja.school.model.Graduate;
import com.haja.school.model.Student;
import com.haja.school.security.JwtAuthFilter;
import com.haja.school.security.JwtProvider;
import com.haja.school.service.CohortService;
import com.haja.school.service.GraduateService;
import com.haja.school.service.StudentService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CohortController.class)
@AutoConfigureMockMvc(addFilters = false)
class CohortControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private CohortService cohortService;
  @MockBean private StudentService studentService;
  @MockBean private GraduateService graduateService;
  @MockBean private JwtAuthFilter jwtAuthFilter;
  @MockBean private JwtProvider jwtProvider;

  @Test
  @WithMockUser(
      username = "admin@admin.com",
      roles = {"ADMIN"})
  void getAllPromotions_returnsList() throws Exception {
    Cohort cohort =
        Cohort.builder()
            .id(UUID.randomUUID())
            .ref("A")
            .entryYear(2024)
            .studentCount(30L)
            .groupCount(2L)
            .build();
    when(cohortService.getAllCohorts()).thenReturn(List.of(cohort));

    mockMvc
        .perform(get("/promotions"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].ref").value("A"))
        .andExpect(jsonPath("$[0].entryYear").value(2024));
  }

  @Test
  @WithMockUser(
      username = "admin@admin.com",
      roles = {"ADMIN"})
  void createPromotion_returns201() throws Exception {
    CreateCohortRequest request = CreateCohortRequest.builder().ref("B").entryYear(2025).build();
    Cohort created =
        Cohort.builder()
            .id(UUID.randomUUID())
            .ref("B")
            .entryYear(2025)
            .studentCount(0L)
            .groupCount(0L)
            .build();
    when(cohortService.createCohort(any(CreateCohortRequest.class))).thenReturn(created);

    mockMvc
        .perform(
            post("/promotions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.ref").value("B"))
        .andExpect(jsonPath("$.entryYear").value(2025));

    verify(cohortService).createCohort(any(CreateCohortRequest.class));
  }

  @Test
  @WithMockUser(
      username = "admin@admin.com",
      roles = {"ADMIN"})
  void getStudentsByCohort_returnsList() throws Exception {
    UUID cohortId = UUID.randomUUID();
    Student student =
        Student.builder()
            .id(UUID.randomUUID())
            .ref("STD00001")
            .name("Doe")
            .firstname("John")
            .email("hei.john@student.com")
            .cohortId(cohortId)
            .build();
    when(studentService.getStudentsByCohort(cohortId)).thenReturn(List.of(student));

    mockMvc
        .perform(get("/promotions/" + cohortId + "/students"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].ref").value("STD00001"));
  }

  @Test
  @WithMockUser(
      username = "admin@admin.com",
      roles = {"ADMIN"})
  void getGraduatesByCohort_returnsList() throws Exception {
    UUID cohortId = UUID.randomUUID();
    Graduate graduate =
        Graduate.builder()
            .rank(1)
            .studentRef("STD00001")
            .name("Doe")
            .firstname("John")
            .overallAverage(15.5)
            .build();
    when(graduateService.getGraduatesByCohort(cohortId)).thenReturn(List.of(graduate));

    mockMvc
        .perform(get("/promotions/" + cohortId + "/graduates"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].rank").value(1))
        .andExpect(jsonPath("$[0].overallAverage").value(15.5));
  }

  @Test
  @WithMockUser(
      username = "admin@admin.com",
      roles = {"ADMIN"})
  void exportGraduatesExcel_returnsExcelFile() throws Exception {
    UUID cohortId = UUID.randomUUID();
    byte[] excelBytes = new byte[] {1, 2, 3, 4, 5};
    when(graduateService.exportGraduatesExcel(cohortId)).thenReturn(excelBytes);

    mockMvc
        .perform(get("/promotions/" + cohortId + "/graduates/export"))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    "attachment; filename=\"graduates-cohort-" + cohortId + ".xlsx\""))
        .andExpect(
            header()
                .string(
                    "Content-Type",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
  }
}
