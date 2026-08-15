package com.haja.school.endpoint.rest.model;

import jakarta.validation.constraints.NotNull;
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
public class TeacherAssignmentRequest {

  @NotNull(message = "Teacher ID is required")
  private UUID teacherId;

  @NotNull(message = "Academic year is required")
  private Integer academicYear;
}
