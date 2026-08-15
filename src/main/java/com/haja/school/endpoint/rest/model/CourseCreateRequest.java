package com.haja.school.endpoint.rest.model;

import com.haja.school.model.Track;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class CourseCreateRequest {

  @NotBlank(message = "Ref is required")
  private String ref;

  @NotBlank(message = "Title is required")
  private String title;

  @NotNull(message = "Credit is required")
  @Min(value = 1, message = "Credit must be at least 1")
  private Integer credit;

  @NotNull(message = "Semester number is required")
  @Min(value = 1, message = "Semester number must be between 1 and 6")
  @Max(value = 6, message = "Semester number must be between 1 and 6")
  private Integer semesterNumber;

  private Track track;
}
