package com.haja.school.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    cohort = JCohort.builder().id(cohortId).ref("A").entryYear(2024).build();
  }

  @Test
  void getGraduatesByCohort_success() {
    UUID student1Id = UUID.randomUUID();
    UUID student2Id = UUID.randomUUID();

    JUser student1 =
        JUser.builder().id(student1Id).ref("STD00001").name("Doe").firstname("John").build();
    JUser student2 =
        JUser.builder().id(student2Id).ref("STD00002").name("Smith").firstname("Jane").build();

    JGrade grade1 = JGrade.builder().id(UUID.randomUUID()).value(15.0).build();
    JGrade grade2 = JGrade.builder().id(UUID.randomUUID()).value(18.0).build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.findByCohortId(cohortId)).thenReturn(List.of(student1, student2));
    when(gradeRepository.findByStudentId(student1Id)).thenReturn(List.of(grade1));
    when(gradeRepository.findByStudentId(student2Id)).thenReturn(List.of(grade2));

    List<Graduate> graduates = graduateService.getGraduatesByCohort(cohortId);

    assertNotNull(graduates);
    assertEquals(2, graduates.size());

    // Student 2 has higher average (18.0) so rank 1
    assertEquals("STD00002", graduates.get(0).getStudentRef());
    assertEquals(1, graduates.get(0).getRank());
    assertEquals(18.0, graduates.get(0).getOverallAverage());

    // Student 1 has average 15.0 so rank 2
    assertEquals("STD00001", graduates.get(1).getStudentRef());
    assertEquals(2, graduates.get(1).getRank());
    assertEquals(15.0, graduates.get(1).getOverallAverage());
  }

  @Test
  void exportGraduatesExcel_success() {
    UUID studentId = UUID.randomUUID();
    JUser student =
        JUser.builder().id(studentId).ref("STD00001").name("Doe").firstname("John").build();
    JGrade grade = JGrade.builder().id(UUID.randomUUID()).value(16.0).build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.findByCohortId(cohortId)).thenReturn(List.of(student));
    when(gradeRepository.findByStudentId(studentId)).thenReturn(List.of(grade));

    byte[] excelBytes = graduateService.exportGraduatesExcel(cohortId);

    assertNotNull(excelBytes);
    assertTrue(excelBytes.length > 0);
  }
}
