package com.haja.school.endpoint.rest.controller;

import com.haja.school.endpoint.rest.model.CreateCohortRequest;
import com.haja.school.model.Cohort;
import com.haja.school.model.Graduate;
import com.haja.school.model.Student;
import com.haja.school.service.CohortService;
import com.haja.school.service.GraduateService;
import com.haja.school.service.StudentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/promotions")
@AllArgsConstructor
public class CohortController {

  private final CohortService cohortService;
  private final StudentService studentService;
  private final GraduateService graduateService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<Cohort>> getAllPromotions() {
    return ResponseEntity.ok(cohortService.getAllCohorts());
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Cohort> createPromotion(@Valid @RequestBody CreateCohortRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(cohortService.createCohort(request));
  }

  @GetMapping("/{id}/students")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<Student>> getStudentsByCohort(@PathVariable UUID id) {
    return ResponseEntity.ok(studentService.getStudentsByCohort(id));
  }

  @GetMapping("/{id}/graduates")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<Graduate>> getGraduatesByCohort(@PathVariable UUID id) {
    return ResponseEntity.ok(graduateService.getGraduatesByCohort(id));
  }

  @GetMapping("/{id}/graduates/export")
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
}
