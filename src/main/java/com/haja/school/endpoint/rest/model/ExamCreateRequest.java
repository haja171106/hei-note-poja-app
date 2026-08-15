package com.haja.school.endpoint.rest.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
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
public class ExamCreateRequest {

  @NotBlank(message = "Label is required")
  private String label;

  @NotNull(message = "Exam date is required")
  private Instant dateExam;

  @NotNull(message = "Coefficient is required")
  @DecimalMin(value = "0.0", inclusive = false, message = "Coefficient must be greater than 0")
  @DecimalMax(value = "1.0", message = "Coefficient must be at most 1.0")
  private Double coefficient;

  @NotNull(message = "Academic year is required")
  private Integer academicYear;
}
