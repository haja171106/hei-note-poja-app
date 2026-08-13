package com.haja.school.repository.model;

import com.haja.school.model.Track;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "course")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JCourse {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 50)
  private String ref;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false)
  private Integer credit;

  @Column(nullable = false)
  private Integer semesterNumber;

  @Enumerated(EnumType.STRING)
  @Column(length = 10)
  private Track track;
}
