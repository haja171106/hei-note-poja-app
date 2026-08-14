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
public class Student {

  private UUID id;
  private String ref;
  private String name;
  private String firstname;
  private String email;
  private UUID cohortId;
  private UUID groupId;
  private Track track;
  private CursusStatus status;
}
