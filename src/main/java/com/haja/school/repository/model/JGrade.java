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
@Table(
    name = "grade",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_grade_exam_student",
            columnNames = {"exam_id", "student_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JGrade {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "exam_id", nullable = false)
  private JExam exam;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "student_id", nullable = false)
  private JUser student;

  @Column(nullable = false)
  private Double value;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "entered_by")
  private JUser enteredBy;

  @Column(name = "entered_at", nullable = false)
  private Instant enteredAt;
}
