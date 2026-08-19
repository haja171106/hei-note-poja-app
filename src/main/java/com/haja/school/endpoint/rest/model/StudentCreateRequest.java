package com.haja.school.endpoint.rest.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
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
public class StudentCreateRequest {

  @NotBlank(message = "Name is required")
  private String name;

  @NotBlank(message = "Firstname is required")
  private String firstname;

  private String address;

  private LocalDate birthdate;

  @NotNull(message = "Cohort ID is required")
  private UUID cohortId;

  private UUID groupId;
  private String password;
}
