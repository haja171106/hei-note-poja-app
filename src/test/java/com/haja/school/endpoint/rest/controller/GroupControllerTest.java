package com.haja.school.endpoint.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.haja.school.model.Group;
import com.haja.school.security.JwtAuthFilter;
import com.haja.school.security.JwtProvider;
import com.haja.school.service.GroupService;
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

@WebMvcTest(controllers = GroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class GroupControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private GroupService groupService;
  @MockBean private JwtAuthFilter jwtAuthFilter;
  @MockBean private JwtProvider jwtProvider;

  @Test
  @WithMockUser(
      username = "admin@admin.com",
      roles = {"ADMIN"})
  void getGroups_asAdmin_returnsOk() throws Exception {
    UUID cohortId = UUID.randomUUID();
    Group group =
        Group.builder()
            .id(UUID.randomUUID())
            .ref("A1")
            .cohortId(cohortId)
            .academicYear(2024)
            .studentCount(25L)
            .build();
    when(groupService.getGroupsByCohort(cohortId)).thenReturn(List.of(group));

    mockMvc
        .perform(get("/promotions/" + cohortId + "/groups"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].ref").value("A1"))
        .andExpect(jsonPath("$[0].studentCount").value(25));
  }

  @Test
  @WithMockUser(
      username = "admin@admin.com",
      roles = {"ADMIN"})
  void generateGroups_asAdmin_returnsOk() throws Exception {
    UUID cohortId = UUID.randomUUID();
    Group group1 =
        Group.builder()
            .id(UUID.randomUUID())
            .ref("A1")
            .cohortId(cohortId)
            .academicYear(2024)
            .studentCount(15L)
            .build();
    Group group2 =
        Group.builder()
            .id(UUID.randomUUID())
            .ref("A2")
            .cohortId(cohortId)
            .academicYear(2024)
            .studentCount(15L)
            .build();
    when(groupService.generateGroups(cohortId)).thenReturn(List.of(group1, group2));

    mockMvc
        .perform(post("/promotions/" + cohortId + "/groups/generate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].ref").value("A1"))
        .andExpect(jsonPath("$[1].ref").value("A2"));
  }
}
