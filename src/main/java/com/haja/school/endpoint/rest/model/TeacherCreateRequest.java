package com.haja.school.endpoint.rest.model;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
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
public class TeacherCreateRequest {

  @NotBlank(message = "Name is required")
  private String name;

  @NotBlank(message = "Firstname is required")
  private String firstname;

  private String address;

  private LocalDate birthdate;
  private String password;
}
