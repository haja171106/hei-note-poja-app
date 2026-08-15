package com.haja.school.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.haja.school.model.CursusStatus;
import com.haja.school.model.Graduate;
import com.haja.school.repository.JCohortRepository;
import com.haja.school.repository.JGradeRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCohort;
import com.haja.school.repository.model.JGrade;
import com.haja.school.repository.model.JUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GraduateServiceTest {

  @Mock private JCohortRepository cohortRepository;
  @Mock private JUserRepository userRepository;
  @Mock private JGradeRepository gradeRepository;

  @InjectMocks private GraduateService graduateService;

  private UUID cohortId;
  private JCohort cohort;

  @BeforeEach
  void setUp() {
    cohortId = UUID.randomUUID();
    cohort = JCohort.builder().id(cohortId).ref("A").entryYear(2023).build();
  }

  @Test
  void getGraduatesByCohort_success_onlyAverageAbove10AndCompleted3Years() {
    UUID student1Id = UUID.randomUUID();
    UUID student2Id = UUID.randomUUID();
    UUID studentLowGradeId = UUID.randomUUID();
    UUID studentDroppedId = UUID.randomUUID();

    JUser student1 =
        JUser.builder()
            .id(student1Id)
            .ref("STD00001")
            .name("Doe")
            .firstname("John")
            .cursusStatus(CursusStatus.ACTIVE)
            .build();
    JUser student2 =
        JUser.builder()
            .id(student2Id)
            .ref("STD00002")
            .name("Smith")
            .firstname("Jane")
            .cursusStatus(CursusStatus.ACTIVE)
            .build();
    JUser studentLowGrade =
        JUser.builder()
            .id(studentLowGradeId)
            .ref("STD00003")
            .name("Low")
            .firstname("Bob")
            .cursusStatus(CursusStatus.ACTIVE)
            .build();
    JUser studentDropped =
        JUser.builder()
            .id(studentDroppedId)
            .ref("STD00004")
            .name("Dropped")
            .firstname("Alice")
            .cursusStatus(CursusStatus.DROPPED_OUT)
            .build();

    JGrade grade1 = JGrade.builder().id(UUID.randomUUID()).value(15.0).build();
    JGrade grade2 = JGrade.builder().id(UUID.randomUUID()).value(18.0).build();
    JGrade gradeLow = JGrade.builder().id(UUID.randomUUID()).value(9.5).build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.findByCohortId(cohortId))
        .thenReturn(List.of(student1, student2, studentLowGrade, studentDropped));
    when(gradeRepository.findByStudentId(student1Id)).thenReturn(List.of(grade1));
    when(gradeRepository.findByStudentId(student2Id)).thenReturn(List.of(grade2));
    when(gradeRepository.findByStudentId(studentLowGradeId)).thenReturn(List.of(gradeLow));

    List<Graduate> graduates = graduateService.getGraduatesByCohort(cohortId);

    assertNotNull(graduates);
    assertEquals(2, graduates.size());

    assertEquals("STD00002", graduates.get(0).getStudentRef());
    assertEquals(1, graduates.get(0).getRank());
    assertEquals(18.0, graduates.get(0).getOverallAverage());

    assertEquals("STD00001", graduates.get(1).getStudentRef());
    assertEquals(2, graduates.get(1).getRank());
    assertEquals(15.0, graduates.get(1).getOverallAverage());
  }

  @Test
  void getGraduatesByCohort_cohortNotCompleted3Years_returnsEmptyUnlessGraduated() {
    JCohort recentCohort = JCohort.builder().id(cohortId).ref("B").entryYear(2025).build();
    UUID studentId = UUID.randomUUID();
    JUser student =
        JUser.builder()
            .id(studentId)
            .ref("STD00001")
            .name("Active")
            .firstname("Student")
            .cursusStatus(CursusStatus.ACTIVE)
            .build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(recentCohort));
    when(userRepository.findByCohortId(cohortId)).thenReturn(List.of(student));

    List<Graduate> graduates = graduateService.getGraduatesByCohort(cohortId);

    assertNotNull(graduates);
    assertTrue(graduates.isEmpty());
  }

  @Test
  void exportGraduatesExcel_success() {
    UUID studentId = UUID.randomUUID();
    JUser student =
        JUser.builder()
            .id(studentId)
            .ref("STD00001")
            .name("Doe")
            .firstname("John")
            .cursusStatus(CursusStatus.ACTIVE)
            .build();
    JGrade grade = JGrade.builder().id(UUID.randomUUID()).value(16.0).build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.findByCohortId(cohortId)).thenReturn(List.of(student));
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of(grade));

    byte[] excelBytes = graduateService.exportGraduatesExcel(cohortId);

    assertNotNull(excelBytes);
    assertTrue(excelBytes.length > 0);
  }

  @Test
  void exportAllGraduatesExcel_success() {
    UUID studentId = UUID.randomUUID();
    JUser student =
        JUser.builder()
            .id(studentId)
            .ref("STD00001")
            .name("Doe")
            .firstname("John")
            .cursusStatus(CursusStatus.ACTIVE)
            .build();
    JGrade grade = JGrade.builder().id(UUID.randomUUID()).value(16.0).build();

    when(cohortRepository.findAll()).thenReturn(List.of(cohort));
    when(userRepository.findByCohortId(cohortId)).thenReturn(List.of(student));
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of(grade));

    byte[] excelBytes = graduateService.exportAllGraduatesExcel();

    assertNotNull(excelBytes);
    assertTrue(excelBytes.length > 0);
  }
}
