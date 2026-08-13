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
@Table(name = "exam")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JExam {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private JCourse course;

  @Column(nullable = false)
  private Integer academicYear;

  @Column(nullable = false, length = 100)
  private String label;

  @Column(nullable = false)
  private Instant dateExam;

  @Column(nullable = false)
  private Double coefficient;
}
