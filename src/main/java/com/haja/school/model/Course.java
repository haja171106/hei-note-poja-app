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
public class Course {

  private UUID id;
  private String ref;
  private String title;
  private Integer credit;
  private Integer semesterNumber;
  private Track track;
}
