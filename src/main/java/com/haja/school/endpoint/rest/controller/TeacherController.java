package com.haja.school.endpoint.rest.controller;

import com.haja.school.endpoint.rest.model.TeacherCreateRequest;
import com.haja.school.model.Teacher;
import com.haja.school.service.TeacherService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
}
