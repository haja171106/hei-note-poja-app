package com.haja.school.endpoint.rest.controller;

import com.haja.school.endpoint.rest.model.GroupChangeRequest;
import com.haja.school.endpoint.rest.model.StudentCreateRequest;
import com.haja.school.endpoint.rest.model.TrackAssignRequest;
import com.haja.school.model.Student;
import com.haja.school.model.StudentGrades;
import com.haja.school.model.YearSummary;
import com.haja.school.service.GradeCalculationService;
import com.haja.school.service.StudentService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/students")
@AllArgsConstructor
public class StudentController {

  private final StudentService studentService;
  private final GradeCalculationService gradeCalculationService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Student> createStudent(@Valid @RequestBody StudentCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteStudent(@PathVariable UUID id) {
    studentService.deleteStudent(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/group")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Student> changeGroup(
      @PathVariable UUID id, @Valid @RequestBody GroupChangeRequest request) {
    return ResponseEntity.ok(studentService.changeStudentGroup(id, request));
  }

  @PatchMapping("/{id}/parcours")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Student> assignTrack(
      @PathVariable UUID id, @Valid @RequestBody TrackAssignRequest request) {
    return ResponseEntity.ok(studentService.assignStudentTrack(id, request));
  }

  @GetMapping("/{id}/grades")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
  public ResponseEntity<StudentGrades> getStudentGrades(
      @PathVariable UUID id,
      @RequestParam(required = false) Integer academicYear,
      Authentication authentication) {
    return ResponseEntity.ok(
        gradeCalculationService.getStudentGrades(id, academicYear, authentication.getName()));
  }

  @GetMapping("/{id}/years/{year}/summary")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
  public ResponseEntity<YearSummary> getYearSummary(
      @PathVariable UUID id, @PathVariable int year, Authentication authentication) {
    return ResponseEntity.ok(
        gradeCalculationService.getYearSummary(id, year, authentication.getName()));
  }
}
