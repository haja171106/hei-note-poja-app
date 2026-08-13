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
public class StudentGroupHistory {

  private UUID id;
  private UUID studentId;
  private UUID groupId;
  private LocalDate startDate;
  private LocalDate endDate;
}
