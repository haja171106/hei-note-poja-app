package com.haja.school.model;

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
public class User {

  private UUID id;
  private String ref;
  private String name;
  private String firstname;
  private String email;
  private String password;
  private Role role;
  private Track track;
  private CursusStatus cursusStatus;
  private UUID cohortId;
  private LocalDate birthdate;
  private String address;
}
