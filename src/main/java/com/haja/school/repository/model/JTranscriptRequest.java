package com.haja.school.repository.model;

import com.haja.school.model.ReportStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transcript_request")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JTranscriptRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id", nullable = false)
  private JUser student;

  @Column(name = "academic_year", nullable = false)
  private Integer academicYear;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReportStatus status;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requested_by")
  private JUser requestedBy;
}
