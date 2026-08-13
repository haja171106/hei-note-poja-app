package com.haja.school.model;

import java.time.Instant;
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
public class Exam {

  private UUID id;
  private UUID courseId;
  private Integer academicYear;
  private String label;
  private Instant dateExam;
  private Double coefficient;
}
