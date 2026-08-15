package com.haja.school.endpoint.rest.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class GradeUpsertRequest {

  @NotNull(message = "Value is required")
  @DecimalMin(value = "0.0", message = "Value must be between 0 and 20")
  @DecimalMax(value = "20.0", message = "Value must be between 0 and 20")
  private Double value;

  @NotBlank(message = "Reason is required")
  private String reason;
}
