package com.haja.school.endpoint.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haja.school.endpoint.rest.model.GroupChangeRequest;
import com.haja.school.endpoint.rest.model.StudentCreateRequest;
import com.haja.school.endpoint.rest.model.TrackAssignRequest;
import com.haja.school.endpoint.rest.model.TranscriptRequestAck;
import com.haja.school.model.GradeHistory;
import com.haja.school.model.Student;
import com.haja.school.model.StudentGrades;
import com.haja.school.model.Track;
import com.haja.school.model.YearSummary;
import com.haja.school.security.JwtAuthFilter;
import com.haja.school.security.JwtProvider;
import com.haja.school.service.GradeCalculationService;
import com.haja.school.service.StudentService;
import com.haja.school.service.TranscriptService;
import java.time.LocalDate;
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

@WebMvcTest(controllers = StudentController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudentControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private StudentService studentService;
  @MockBean private GradeCalculationService gradeCalculationService;
  @MockBean private TranscriptService transcriptService;
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
  void createStudent_asAdmin_returns201() throws Exception {
    UUID cohortId = UUID.randomUUID();
    StudentCreateRequest request =
        StudentCreateRequest.builder()
            .name("Doe")
            .firstname("John")
            .birthdate(LocalDate.of(2000, 1, 15))
            .cohortId(cohortId)
            .build();

    Student student =
        Student.builder()
            .id(UUID.randomUUID())
            .ref("STD00001")
            .name("Doe")
            .firstname("John")
            .email("hei.john@student.com")
            .cohortId(cohortId)
            .build();
    when(studentService.createStudent(any(StudentCreateRequest.class))).thenReturn(student);

    mockMvc
        .perform(
            post("/students")
                .with(mockUser("admin@admin.com", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.ref").value("STD00001"))
        .andExpect(jsonPath("$.name").value("Doe"));

    verify(studentService).createStudent(any(StudentCreateRequest.class));
  }

  @Test
  void deleteStudent_asAdmin_returns204() throws Exception {
    UUID studentId = UUID.randomUUID();

    mockMvc
        .perform(delete("/students/" + studentId).with(mockUser("admin@admin.com", "ADMIN")))
        .andExpect(status().isNoContent());

    verify(studentService).deleteStudent(studentId);
  }

  @Test
  void changeGroup_asAdmin_returnsOk() throws Exception {
    UUID studentId = UUID.randomUUID();
    UUID newGroupId = UUID.randomUUID();
    GroupChangeRequest request =
        GroupChangeRequest.builder()
            .newGroupId(newGroupId)
            .effectiveDate(LocalDate.of(2025, 9, 1))
            .build();

    Student student =
        Student.builder()
            .id(studentId)
            .ref("STD00001")
            .name("Doe")
            .firstname("John")
            .email("hei.john@student.com")
            .groupId(newGroupId)
            .build();
    when(studentService.changeStudentGroup(eq(studentId), any(GroupChangeRequest.class)))
        .thenReturn(student);

    mockMvc
        .perform(
            patch("/students/" + studentId + "/group")
                .with(mockUser("admin@admin.com", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groupId").value(newGroupId.toString()));
  }

  @Test
  void assignTrack_asAdmin_returnsOk() throws Exception {
    UUID studentId = UUID.randomUUID();
    TrackAssignRequest request = TrackAssignRequest.builder().track(Track.EL).build();

    Student student =
        Student.builder()
            .id(studentId)
            .ref("STD00001")
            .name("Doe")
            .firstname("John")
            .email("hei.john@student.com")
            .track(Track.EL)
            .build();
    when(studentService.assignStudentTrack(eq(studentId), any(TrackAssignRequest.class)))
        .thenReturn(student);

    mockMvc
        .perform(
            patch("/students/" + studentId + "/parcours")
                .with(mockUser("admin@admin.com", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.track").value("EL"));
  }

  @Test
  void getStudentGrades_asAdmin_returnsOk() throws Exception {
    UUID studentId = UUID.randomUUID();
    StudentGrades studentGrades =
        StudentGrades.builder().studentId(studentId).academicYear(2025).courses(List.of()).build();
    when(gradeCalculationService.getStudentGrades(eq(studentId), anyInt(), anyString()))
        .thenReturn(studentGrades);

    mockMvc
        .perform(
            get("/students/" + studentId + "/grades")
                .with(mockUser("admin@admin.com", "ADMIN"))
                .param("academicYear", "2025"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.studentId").value(studentId.toString()));
  }

  @Test
  void getStudentGrades_asStudent_returnsOk() throws Exception {
    UUID studentId = UUID.randomUUID();
    StudentGrades studentGrades =
        StudentGrades.builder().studentId(studentId).academicYear(2025).courses(List.of()).build();
    when(gradeCalculationService.getStudentGrades(eq(studentId), anyInt(), anyString()))
        .thenReturn(studentGrades);

    mockMvc
        .perform(
            get("/students/" + studentId + "/grades")
                .with(mockUser("student@student.com", "STUDENT"))
                .param("academicYear", "2025"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.studentId").value(studentId.toString()));
  }

  @Test
  void getStudentGradeHistory_asAdmin_returnsOk() throws Exception {
    UUID studentId = UUID.randomUUID();
    GradeHistory history =
        GradeHistory.builder()
            .id(UUID.randomUUID())
            .gradeId(UUID.randomUUID())
            .oldValue(12.0)
            .newValue(14.0)
            .reason("Correction")
            .build();
    when(gradeCalculationService.getStudentGradeHistory(eq(studentId), anyString()))
        .thenReturn(List.of(history));

    mockMvc
        .perform(
            get("/students/" + studentId + "/grades/history")
                .with(mockUser("admin@admin.com", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].oldValue").value(12.0))
        .andExpect(jsonPath("$[0].newValue").value(14.0));
  }

  @Test
  void getYearSummary_asAdmin_returnsOk() throws Exception {
    UUID studentId = UUID.randomUUID();
    YearSummary summary =
        YearSummary.builder()
            .studentId(studentId)
            .year(1)
            .overallAverage(14.5)
            .totalCredits(60)
            .courses(List.of())
            .build();
    when(gradeCalculationService.getYearSummary(eq(studentId), anyInt(), anyString()))
        .thenReturn(summary);

    mockMvc
        .perform(
            get("/students/" + studentId + "/years/1/summary")
                .with(mockUser("admin@admin.com", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.year").value(1))
        .andExpect(jsonPath("$.overallAverage").value(14.5));
  }

  @Test
  void requestTranscript_asAdmin_returns202() throws Exception {
    UUID studentId = UUID.randomUUID();
    TranscriptRequestAck ack =
        TranscriptRequestAck.builder()
            .requestId(UUID.randomUUID())
            .message(
                "Transcript request accepted. The document will be sent to your email shortly.")
            .build();
    when(transcriptService.requestTranscript(
            eq(studentId), anyInt(), eq("recipient@example.com"), anyString()))
        .thenReturn(ack);

    mockMvc
        .perform(
            post("/students/" + studentId + "/transcript")
                .with(mockUser("admin@admin.com", "ADMIN"))
                .param("academicYear", "2025")
                .param("recipientEmail", "recipient@example.com"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.message").value(ack.getMessage()));
  }
}
