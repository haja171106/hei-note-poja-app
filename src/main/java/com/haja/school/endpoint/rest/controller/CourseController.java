package com.haja.school.endpoint.rest.controller;

import com.haja.school.endpoint.rest.model.CourseCreateRequest;
import com.haja.school.endpoint.rest.model.ExamCreateRequest;
import com.haja.school.endpoint.rest.model.TeacherAssignmentRequest;
import com.haja.school.model.Course;
import com.haja.school.model.Exam;
import com.haja.school.model.Student;
import com.haja.school.model.TeacherCourseAssignment;
import com.haja.school.model.Track;
import com.haja.school.service.CourseService;
import com.haja.school.service.ExamService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/courses")
@AllArgsConstructor
public class CourseController {

  private final CourseService courseService;
  private final ExamService examService;

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
  public ResponseEntity<List<Course>> getCourses(
      @RequestParam(required = false) Integer semester,
      @RequestParam(required = false) Track track,
      Authentication authentication) {
    return ResponseEntity.ok(courseService.getCourses(authentication.getName(), semester, track));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Course> createCourse(@Valid @RequestBody CourseCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(courseService.createCourse(request));
  }

  @GetMapping("/{id}/students")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<List<Student>> getCourseStudents(
      @PathVariable UUID id, Authentication authentication) {
    return ResponseEntity.ok(courseService.getStudentsByCourse(id, authentication.getName()));
  }

  @PostMapping("/{id}/teachers")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<TeacherCourseAssignment> assignTeacherToCourse(
      @PathVariable UUID id, @Valid @RequestBody TeacherAssignmentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(courseService.assignTeacherToCourse(id, request));
  }

  @GetMapping("/{id}/exams")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
  public ResponseEntity<List<Exam>> getCourseExams(
      @PathVariable UUID id,
      @RequestParam(required = false) Integer academicYear,
      Authentication authentication) {
    return ResponseEntity.ok(
        examService.getExamsByCourse(id, academicYear, authentication.getName()));
  }

  @PostMapping("/{id}/exams")
  @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
  public ResponseEntity<Exam> createExam(
      @PathVariable UUID id,
      @Valid @RequestBody ExamCreateRequest request,
      Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(examService.createExam(id, request, authentication.getName()));
  }
}
