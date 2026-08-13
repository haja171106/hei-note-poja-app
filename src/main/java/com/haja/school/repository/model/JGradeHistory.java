package com.haja.school.repository.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "grade_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JGradeHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "grade_id", nullable = false)
  private JGrade grade;

  @Column(name = "old_value")
  private Double oldValue;

  @Column(name = "new_value", nullable = false)
  private Double newValue;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "changed_by")
  private JUser changedBy;

  @Column(name = "changed_at", nullable = false)
  private Instant changedAt;

  @Column(nullable = false, length = 500)
  private String reason;
}
