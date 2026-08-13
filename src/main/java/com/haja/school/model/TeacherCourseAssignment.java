package com.haja.school.model;

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
public class TeacherCourseAssignment {

  private UUID id;
  private UUID teacherId;
  private UUID courseId;
  private Integer academicYear;
}
