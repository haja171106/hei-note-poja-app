package com.haja.school.endpoint.web.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCohort;
import com.haja.school.repository.model.JUser;
import com.haja.school.security.JwtAuthFilter;
import com.haja.school.security.JwtProvider;
import com.haja.school.service.CohortService;
import com.haja.school.service.GraduateService;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = LoginViewController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoginViewControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private JUserRepository userRepository;
  @MockBean private JwtProvider jwtProvider;
  @MockBean private JwtAuthFilter jwtAuthFilter;
  @MockBean private CohortService cohortService;
  @MockBean private GraduateService graduateService;

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
  void loginPage_unauthenticated_returnsLoginView() throws Exception {
    mockMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("login"));
  }

  @Test
  @WithMockUser(
      username = "hei.admin@admin.com",
      roles = {"ADMIN"})
  void adminDashboard_withAdminRole_returnsAdminView() throws Exception {
    mockMvc
        .perform(get("/ui/admin"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin-dashboard"));
  }

  @Test
  @WithMockUser(
      username = "hei.prof@teacher.com",
      roles = {"TEACHER"})
  void teacherDashboard_withTeacherRole_returnsTeacherView() throws Exception {
    mockMvc
        .perform(get("/ui/teacher"))
        .andExpect(status().isOk())
        .andExpect(view().name("teacher-dashboard"));
  }

  @Test
  @WithMockUser(
      username = "hei.student@student.com",
      roles = {"STUDENT"})
  void studentDashboard_withStudentRole_returnsStudentView() throws Exception {
    mockMvc
        .perform(get("/ui/student"))
        .andExpect(status().isOk())
        .andExpect(view().name("student-dashboard"));
  }

  @Test
  @WithMockUser(
      username = "hei.admin@admin.com",
      roles = {"ADMIN"})
  void exportAllGraduatesExcel_returnsExcel() throws Exception {
    when(graduateService.exportAllGraduatesExcel()).thenReturn(new byte[] {1, 2, 3});

    mockMvc
        .perform(get("/ui/admin/graduates/export"))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"graduates-all.xlsx\""));
  }

  @Test
  void loginPage_root_unauthenticated_returnsLoginView() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("login"));
  }

  @Test
  void loginPage_uiLogin_unauthenticated_returnsLoginView() throws Exception {
    mockMvc.perform(get("/ui/login")).andExpect(status().isOk()).andExpect(view().name("login"));
  }

  @Test
  void loginPage_authenticatedAdmin_redirectsToAdmin() throws Exception {
    mockMvc
        .perform(get("/login").with(mockUser("hei.admin@admin.com", "ADMIN")))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/ui/admin"));
  }

  @Test
  void loginPage_authenticatedTeacher_redirectsToTeacher() throws Exception {
    mockMvc
        .perform(get("/login").with(mockUser("hei.prof@teacher.com", "TEACHER")))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/ui/teacher"));
  }

  @Test
  void loginPage_authenticatedStudent_redirectsToStudent() throws Exception {
    mockMvc
        .perform(get("/login").with(mockUser("hei.student@student.com", "STUDENT")))
        .andExpect(status().isFound())
        .andExpect(header().string("Location", "/ui/student"));
  }

  @Test
  void adminDashboard_populatesUserModel() throws Exception {
    JUser adminUser =
        JUser.builder()
            .id(UUID.randomUUID())
            .ref("REF001")
            .name("Admin")
            .firstname("Super")
            .email("hei.admin@admin.com")
            .build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(adminUser));
    when(cohortService.getAllCohorts()).thenReturn(Collections.emptyList());

    mockMvc
        .perform(get("/ui/admin").with(mockUser("hei.admin@admin.com", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("admin-dashboard"))
        .andExpect(model().attributeExists("user"))
        .andExpect(model().attribute("user", adminUser));
  }

  @Test
  @WithMockUser(
      username = "hei.admin@admin.com",
      roles = {"ADMIN"})
  void exportGraduatesExcel_byCohortId_returnsExcel() throws Exception {
    UUID cohortId = UUID.randomUUID();
    when(graduateService.exportGraduatesExcel(cohortId)).thenReturn(new byte[] {4, 5, 6});

    mockMvc
        .perform(get("/ui/admin/promotions/" + cohortId + "/graduates/export"))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    "attachment; filename=\"graduates-cohort-" + cohortId + ".xlsx\""));
  }

  @Test
  void studentDashboard_populatesStudyYearAndAcademicYear() throws Exception {
    int entryYear = 2024;
    JCohort cohort = JCohort.builder().id(UUID.randomUUID()).ref("C1").entryYear(entryYear).build();

    JUser student =
        JUser.builder()
            .id(UUID.randomUUID())
            .ref("REF001")
            .name("Student")
            .firstname("Test")
            .email("hei.student@student.com")
            .cohort(cohort)
            .build();

    when(userRepository.findByEmail("hei.student@student.com")).thenReturn(Optional.of(student));

    int currentYear = LocalDate.now().getYear();
    int currentMonth = LocalDate.now().getMonthValue();
    int yearsPassed = currentYear - entryYear;
    int semester = Math.max(1, Math.min(6, yearsPassed * 2 + (currentMonth >= 9 ? 1 : 0)));
    int expectedStudyYear = (semester + 1) / 2;
    int expectedAcademicYear = entryYear + expectedStudyYear - 1;

    mockMvc
        .perform(get("/ui/student").with(mockUser("hei.student@student.com", "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(view().name("student-dashboard"))
        .andExpect(model().attributeExists("user"))
        .andExpect(model().attribute("studyYear", expectedStudyYear))
        .andExpect(model().attribute("academicYear", expectedAcademicYear));
  }

  @Test
  void studentDashboard_studentWithoutCohort_noStudyYear() throws Exception {
    JUser studentWithoutCohort =
        JUser.builder()
            .id(UUID.randomUUID())
            .ref("REF002")
            .name("Student")
            .firstname("NoCohort")
            .email("hei.student@student.com")
            .cohort(null)
            .build();

    when(userRepository.findByEmail("hei.student@student.com"))
        .thenReturn(Optional.of(studentWithoutCohort));

    mockMvc
        .perform(get("/ui/student").with(mockUser("hei.student@student.com", "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(view().name("student-dashboard"))
        .andExpect(model().attributeExists("user"))
        .andExpect(model().attributeDoesNotExist("studyYear"))
        .andExpect(model().attributeDoesNotExist("academicYear"));
  }
}
