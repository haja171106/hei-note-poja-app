package com.haja.school.repository.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "teacher_course_assignment",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_teacher_course_year",
            columnNames = {"teacher_id", "course_id", "academic_year"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JTeacherCourseAssignment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "teacher_id", nullable = false)
  private JUser teacher;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "course_id", nullable = false)
  private JCourse course;

  @Column(name = "academic_year", nullable = false)
  private Integer academicYear;
}
