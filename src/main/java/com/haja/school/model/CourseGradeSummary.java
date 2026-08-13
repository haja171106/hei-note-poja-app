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
public class CourseGradeSummary {

  private UUID courseId;
  private String courseRef;
  private String courseTitle;
  private Integer credit;
  private Double finalGrade;
}
