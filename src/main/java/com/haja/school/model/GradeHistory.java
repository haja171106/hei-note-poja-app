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
public class GradeHistory {

  private UUID id;
  private UUID gradeId;
  private Double oldValue;
  private Double newValue;
  private UUID changedBy;
  private Instant changedAt;
  private String reason;
}
