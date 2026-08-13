package com.haja.school.repository.model;

import com.haja.school.model.CursusStatus;
import com.haja.school.model.Role;
import com.haja.school.model.Track;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "\"user\"")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JUser {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 50)
  private String ref;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 100)
  private String firstname;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Enumerated(EnumType.STRING)
  @Column(length = 10)
  private Track track;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private CursusStatus cursusStatus;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cohort_id")
  private JCohort cohort;

  private LocalDate birthdate;

  @Column(length = 255)
  private String address;
}
