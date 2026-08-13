package com.haja.school.model;

import java.time.Instant;
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
public class Grade {

  private UUID id;
  private UUID examId;
  private UUID studentId;
  private Double value;
  private UUID enteredBy;
  private Instant enteredAt;
}
