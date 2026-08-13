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
public class Group {

  private UUID id;
  private String ref;
  private UUID cohortId;
  private Integer academicYear;
  private Long studentCount;
}
