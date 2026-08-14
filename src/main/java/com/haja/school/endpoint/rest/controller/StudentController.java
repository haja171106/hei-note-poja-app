package com.haja.school.endpoint.rest.controller;

import com.haja.school.endpoint.rest.model.GroupChangeRequest;
import com.haja.school.endpoint.rest.model.StudentCreateRequest;
import com.haja.school.model.Student;
import com.haja.school.service.StudentService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/students")
@AllArgsConstructor
public class StudentController {

  private final StudentService studentService;

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
}
