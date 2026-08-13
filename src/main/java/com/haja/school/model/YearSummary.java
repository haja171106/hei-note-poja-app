package com.haja.school.model;

import java.util.List;
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
public class YearSummary {

  private UUID studentId;
  private Integer year;
  private List<CourseGradeSummary> courses;
  private Double overallAverage;
  private Integer totalCredits;
  private ReportStatus status;
}
