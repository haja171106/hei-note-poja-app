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
public class TranscriptRequest {

  private UUID id;
  private UUID studentId;
  private Integer academicYear;
  private ReportStatus status;
  private Instant requestedAt;
  private UUID requestedBy;
}
