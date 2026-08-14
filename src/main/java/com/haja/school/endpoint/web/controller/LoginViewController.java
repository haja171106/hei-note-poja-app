package com.haja.school.endpoint.web.controller;

import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JUser;
import com.haja.school.service.CohortService;
import com.haja.school.service.GraduateService;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@AllArgsConstructor
public class LoginViewController {

  private final JUserRepository userRepository;
  private final CohortService cohortService;
  private final GraduateService graduateService;

  @GetMapping({"/", "/login", "/ui/login"})
  public String loginPage(Authentication authentication) {
    if (authentication != null && authentication.isAuthenticated()) {
      return redirectBasedOnRole(authentication);
    }
    return "login";
  }

  @GetMapping("/ui/admin")
  @PreAuthorize("hasRole('ADMIN')")
  public String adminDashboard(Principal principal, Model model) {
    populateUserModel(principal, model);
    model.addAttribute("cohorts", cohortService.getAllCohorts());
    return "admin-dashboard";
  }

  @GetMapping({"/ui/admin/promotions/{id}/graduates/export", "/promotions/{id}/graduates/export"})
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<byte[]> exportGraduatesExcel(@PathVariable UUID id) {
    byte[] excelBytes = graduateService.exportGraduatesExcel(id);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"graduates-cohort-" + id + ".xlsx\"")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(excelBytes);
  }

  @GetMapping("/ui/teacher")
  @PreAuthorize("hasRole('TEACHER')")
  public String teacherDashboard(Principal principal, Model model) {
    populateUserModel(principal, model);
    return "teacher-dashboard";
  }

  @GetMapping("/ui/student")
  @PreAuthorize("hasRole('STUDENT')")
  public String studentDashboard(Principal principal, Model model) {
    populateUserModel(principal, model);
    return "student-dashboard";
  }

  private String redirectBasedOnRole(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(a -> a.getAuthority())
        .filter(role -> role.startsWith("ROLE_"))
        .findFirst()
        .map(
            role ->
                switch (role) {
                  case "ROLE_ADMIN" -> "redirect:/ui/admin";
                  case "ROLE_TEACHER" -> "redirect:/ui/teacher";
                  case "ROLE_STUDENT" -> "redirect:/ui/student";
                  default -> "login";
                })
        .orElse("login");
  }

  private void populateUserModel(Principal principal, Model model) {
    if (principal != null) {
      Optional<JUser> userOpt = userRepository.findByEmail(principal.getName());
      userOpt.ifPresent(user -> model.addAttribute("user", user));
    }
  }
}
