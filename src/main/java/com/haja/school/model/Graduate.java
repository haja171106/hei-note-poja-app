package com.haja.school.model;

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
public class Graduate {

  private Integer rank;
  private String studentRef;
  private String name;
  private String firstname;
  private Double overallAverage;
}
