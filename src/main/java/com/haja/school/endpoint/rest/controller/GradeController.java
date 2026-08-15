package com.haja.school.endpoint.rest.controller;

import com.haja.school.endpoint.rest.model.GradeUpsertRequest;
import com.haja.school.model.Grade;
import com.haja.school.service.ExamService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exams")
@AllArgsConstructor
public class GradeController {

  private final ExamService examService;

  @GetMapping("/{id}/grades")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<List<Grade>> getExamGrades(
      @PathVariable UUID id, Authentication authentication) {
    return ResponseEntity.ok(examService.getGradesByExam(id, authentication.getName()));
  }

  @PutMapping("/{id}/grades/{studentId}")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<Grade> upsertExamGrade(
      @PathVariable UUID id,
      @PathVariable UUID studentId,
      @Valid @RequestBody GradeUpsertRequest request,
      Authentication authentication) {
    return ResponseEntity.ok(
        examService.upsertGrade(id, studentId, request, authentication.getName()));
  }
}
