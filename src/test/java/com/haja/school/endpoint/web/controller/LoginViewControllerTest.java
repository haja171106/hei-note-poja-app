package com.haja.school.endpoint.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.haja.school.repository.JUserRepository;
import com.haja.school.security.JwtAuthFilter;
import com.haja.school.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LoginViewController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoginViewControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private JUserRepository userRepository;
  @MockBean private JwtProvider jwtProvider;
  @MockBean private JwtAuthFilter jwtAuthFilter;

  @Test
  void loginPage_unauthenticated_returnsLoginView() throws Exception {
    mockMvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andExpect(view().name("login"));
  }

  @Test
  @WithMockUser(username = "hei.admin@admin.com", roles = {"ADMIN"})
  void adminDashboard_withAdminRole_returnsAdminView() throws Exception {
    mockMvc.perform(get("/ui/admin"))
        .andExpect(status().isOk())
        .andExpect(view().name("admin-dashboard"));
  }

  @Test
  @WithMockUser(username = "hei.prof@teacher.com", roles = {"TEACHER"})
  void teacherDashboard_withTeacherRole_returnsTeacherView() throws Exception {
    mockMvc.perform(get("/ui/teacher"))
        .andExpect(status().isOk())
        .andExpect(view().name("teacher-dashboard"));
  }

  @Test
  @WithMockUser(username = "hei.student@student.com", roles = {"STUDENT"})
  void studentDashboard_withStudentRole_returnsStudentView() throws Exception {
    mockMvc.perform(get("/ui/student"))
        .andExpect(status().isOk())
        .andExpect(view().name("student-dashboard"));
  }
}
