package com.haja.school.endpoint.rest.controller;

import com.haja.school.endpoint.rest.model.TeacherCreateRequest;
import com.haja.school.model.Course;
import com.haja.school.model.Teacher;
import com.haja.school.model.TeacherGradeBoard;
import com.haja.school.service.TeacherService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teachers")
@AllArgsConstructor
public class TeacherController {

  private final TeacherService teacherService;

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Teacher> createTeacher(@Valid @RequestBody TeacherCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(teacherService.createTeacher(request));
  }

  @GetMapping("/{id}/courses")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<List<Course>> getCoursesByTeacher(
      @PathVariable UUID id, Authentication authentication) {
    return ResponseEntity.ok(teacherService.getCoursesByTeacher(id, authentication.getName()));
  }

  @GetMapping("/{id}/grades")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<TeacherGradeBoard> getTeacherGradeBoard(
      @PathVariable UUID id, Authentication authentication) {
    return ResponseEntity.ok(teacherService.getTeacherGradeBoard(id, authentication.getName()));
  }
}
