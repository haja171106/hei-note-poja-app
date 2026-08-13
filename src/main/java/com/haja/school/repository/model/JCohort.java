package com.haja.school.repository.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cohort")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JCohort {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 10)
  private String ref;

  @Column(name = "entry_year", nullable = false)
  private Integer entryYear;
}
