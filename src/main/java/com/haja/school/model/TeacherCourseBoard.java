package com.haja.school.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherCourseBoard {

  private Course course;
  private List<Student> students;
  private List<Exam> exams;
  private List<Grade> grades;
  private Map<UUID, Double> studentAverages;
}
