package com.haja.school.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.haja.school.model.CourseGradeSummary;
import com.haja.school.model.CursusStatus;
import com.haja.school.model.Graduate;
import com.haja.school.model.YearSummary;
import com.haja.school.repository.JCohortRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCohort;
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
  @Mock private GradeCalculationService gradeCalculationService;

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

    JUser student1 = student(student1Id, "STD00001", "Doe", "John", CursusStatus.ACTIVE);
    JUser student2 = student(student2Id, "STD00002", "Smith", "Jane", CursusStatus.ACTIVE);
    JUser studentLowGrade =
        student(studentLowGradeId, "STD00003", "Low", "Bob", CursusStatus.ACTIVE);
    JUser studentDropped =
        student(studentDroppedId, "STD00004", "Dropped", "Alice", CursusStatus.DROPPED_OUT);

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.findByCohortId(cohortId))
        .thenReturn(List.of(student1, student2, studentLowGrade, studentDropped));

    stubYearAveragesAllPassing(student1Id, 10.5, 12.0, 12.0);
    stubYearAveragesAllPassing(student2Id, 16.0, 16.0, 16.0);
    stubYearAveragesWithFailing(studentLowGradeId, 9.0, 9.0, 9.0);

    List<Graduate> graduates = graduateService.getGraduatesByCohort(cohortId);

    assertNotNull(graduates);
    assertEquals(2, graduates.size());

    assertEquals("STD00002", graduates.get(0).getStudentRef());
    assertEquals(1, graduates.get(0).getRank());
    assertEquals(16.0, graduates.get(0).getOverallAverage());

    assertEquals("STD00001", graduates.get(1).getStudentRef());
    assertEquals(2, graduates.get(1).getRank());
    assertEquals(11.5, graduates.get(1).getOverallAverage());
  }

  @Test
  void getGraduatesByCohort_weightedAverage_differsFromFlatAverage() {
    UUID studentId = UUID.randomUUID();
    JUser student = student(studentId, "STD00001", "Doe", "John", CursusStatus.ACTIVE);

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.findByCohortId(cohortId)).thenReturn(List.of(student));

    List<CourseGradeSummary> passingCourses =
        List.of(CourseGradeSummary.builder().finalGrade(12.0).credit(10).build());

    YearSummary year1 =
        YearSummary.builder()
            .year(1)
            .overallAverage(10.4)
            .totalCredits(10)
            .courses(passingCourses)
            .build();
    YearSummary year2 =
        YearSummary.builder()
            .year(2)
            .overallAverage(12.0)
            .totalCredits(60)
            .courses(passingCourses)
            .build();
    YearSummary year3 =
        YearSummary.builder()
            .year(3)
            .overallAverage(12.0)
            .totalCredits(60)
            .courses(passingCourses)
            .build();
    when(gradeCalculationService.computeYearSummary(studentId, 1)).thenReturn(year1);
    when(gradeCalculationService.computeYearSummary(studentId, 2)).thenReturn(year2);
    when(gradeCalculationService.computeYearSummary(studentId, 3)).thenReturn(year3);

    List<Graduate> graduates = graduateService.getGraduatesByCohort(cohortId);

    assertNotNull(graduates);
    assertEquals(1, graduates.size());

    double weightedAverage = (10.4 * 10 + 12.0 * 60 + 12.0 * 60) / (10 + 60 + 60);
    double roundedWeightedAverage = Math.round(weightedAverage * 100.0) / 100.0;
    double flatAverage = (16.0 + 8.0 + 8.0) / 3;

    assertEquals(11.88, graduates.get(0).getOverallAverage(), 0.001);
    assertEquals(roundedWeightedAverage, graduates.get(0).getOverallAverage(), 0.001);
    assertNotEquals(flatAverage, graduates.get(0).getOverallAverage(), 0.001);
  }

  @Test
  void getGraduatesByCohort_cohortNotCompleted3Years_returnsEmptyUnlessGraduated() {
    JCohort recentCohort = JCohort.builder().id(cohortId).ref("B").entryYear(2025).build();
    UUID activeStudentId = UUID.randomUUID();
    UUID graduatedStudentId = UUID.randomUUID();

    JUser activeStudent =
        student(activeStudentId, "STD00001", "Active", "Student", CursusStatus.ACTIVE);
    JUser graduatedStudent =
        student(graduatedStudentId, "STD00002", "Graduated", "Student", CursusStatus.GRADUATED);

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(recentCohort));
    when(userRepository.findByCohortId(cohortId))
        .thenReturn(List.of(activeStudent, graduatedStudent));

    stubYearAveragesAllPassing(graduatedStudentId, 16.0, 16.0, 16.0);

    List<Graduate> graduates = graduateService.getGraduatesByCohort(cohortId);

    assertNotNull(graduates);
    assertEquals(1, graduates.size());
    assertEquals("STD00002", graduates.get(0).getStudentRef());
  }

  @Test
  void exportGraduatesExcel_success() {
    UUID studentId = UUID.randomUUID();
    JUser student = student(studentId, "STD00001", "Doe", "John", CursusStatus.ACTIVE);

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.findByCohortId(cohortId)).thenReturn(List.of(student));

    stubYearAveragesAllPassing(studentId, 16.0, 16.0, 16.0);

    byte[] excelBytes = graduateService.exportGraduatesExcel(cohortId);

    assertNotNull(excelBytes);
    assertTrue(excelBytes.length > 0);
  }

  @Test
  void exportAllGraduatesExcel_success() {
    UUID studentId = UUID.randomUUID();
    JUser student = student(studentId, "STD00001", "Doe", "John", CursusStatus.ACTIVE);

    when(cohortRepository.findAll()).thenReturn(List.of(cohort));
    when(userRepository.findByCohortId(cohortId)).thenReturn(List.of(student));

    stubYearAveragesAllPassing(studentId, 16.0, 16.0, 16.0);

    byte[] excelBytes = graduateService.exportAllGraduatesExcel();

    assertNotNull(excelBytes);
    assertTrue(excelBytes.length > 0);
  }

  @Test
  void buildExcel_handlesNullValues_withoutThrowing() throws Exception {
    Graduate graduate =
        Graduate.builder()
            .rank(1)
            .studentRef("STD00001")
            .name(null)
            .firstname("John")
            .overallAverage(null)
            .build();

    var method = GraduateService.class.getDeclaredMethod("buildExcel", List.class, String.class);
    method.setAccessible(true);

    byte[] excelBytes = (byte[]) method.invoke(graduateService, List.of(graduate), "Graduates");

    assertNotNull(excelBytes);
    assertTrue(excelBytes.length > 0);
  }

  @Test
  void getGraduatesByCohort_studentWithoutCohort_excluded() {
    UUID studentId = UUID.randomUUID();
    JUser studentWithoutCohort =
        JUser.builder()
            .id(studentId)
            .ref("STD00001")
            .name("No")
            .firstname("Cohort")
            .cursusStatus(CursusStatus.ACTIVE)
            .cohort(null)
            .build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.findByCohortId(cohortId)).thenReturn(List.of(studentWithoutCohort));

    List<Graduate> graduates = graduateService.getGraduatesByCohort(cohortId);

    assertNotNull(graduates);
    assertTrue(graduates.isEmpty());
  }

  @Test
  void getGraduatesByCohort_cohortWithNullEntryYear_returnsEmpty() {
    JCohort nullEntryCohort = JCohort.builder().id(cohortId).ref("C").entryYear(null).build();
    UUID studentId = UUID.randomUUID();
    JUser student = student(studentId, "STD00001", "Doe", "John", CursusStatus.ACTIVE);

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(nullEntryCohort));
    when(userRepository.findByCohortId(cohortId)).thenReturn(List.of(student));

    List<Graduate> graduates = graduateService.getGraduatesByCohort(cohortId);

    assertNotNull(graduates);
    assertTrue(graduates.isEmpty());
  }

  @Test
  void getGraduatesByCohort_studentWithOneFailingSubject_excluded() {
    UUID studentId = UUID.randomUUID();
    JUser student = student(studentId, "STD00005", "Fail", "One", CursusStatus.ACTIVE);

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.findByCohortId(cohortId)).thenReturn(List.of(student));

    List<CourseGradeSummary> yearWithFail =
        List.of(
            CourseGradeSummary.builder().finalGrade(15.0).credit(10).build(),
            CourseGradeSummary.builder().finalGrade(8.0).credit(10).build());
    List<CourseGradeSummary> passingYear =
        List.of(CourseGradeSummary.builder().finalGrade(12.0).credit(10).build());

    when(gradeCalculationService.computeYearSummary(studentId, 1))
        .thenReturn(
            YearSummary.builder()
                .year(1)
                .overallAverage(14.0)
                .totalCredits(60)
                .courses(yearWithFail)
                .build());
    when(gradeCalculationService.computeYearSummary(studentId, 2))
        .thenReturn(
            YearSummary.builder()
                .year(2)
                .overallAverage(12.0)
                .totalCredits(60)
                .courses(passingYear)
                .build());
    when(gradeCalculationService.computeYearSummary(studentId, 3))
        .thenReturn(
            YearSummary.builder()
                .year(3)
                .overallAverage(12.0)
                .totalCredits(60)
                .courses(passingYear)
                .build());

    List<Graduate> graduates = graduateService.getGraduatesByCohort(cohortId);

    assertNotNull(graduates);
    assertTrue(graduates.isEmpty(), "Student with one subject < 10 must not graduate");
  }

  private JUser student(
      UUID id, String ref, String name, String firstname, CursusStatus cursusStatus) {
    return JUser.builder()
        .id(id)
        .ref(ref)
        .name(name)
        .firstname(firstname)
        .cursusStatus(cursusStatus)
        .cohort(cohort)
        .build();
  }

  private void stubYearAveragesAllPassing(UUID studentId, double avg1, double avg2, double avg3) {
    when(gradeCalculationService.computeYearSummary(studentId, 1))
        .thenReturn(
            YearSummary.builder()
                .year(1)
                .overallAverage(avg1)
                .totalCredits(60)
                .courses(
                    List.of(
                        CourseGradeSummary.builder()
                            .finalGrade(Math.max(avg1, 10.0))
                            .credit(10)
                            .build()))
                .build());
    when(gradeCalculationService.computeYearSummary(studentId, 2))
        .thenReturn(
            YearSummary.builder()
                .year(2)
                .overallAverage(avg2)
                .totalCredits(60)
                .courses(
                    List.of(
                        CourseGradeSummary.builder()
                            .finalGrade(Math.max(avg2, 10.0))
                            .credit(10)
                            .build()))
                .build());
    when(gradeCalculationService.computeYearSummary(studentId, 3))
        .thenReturn(
            YearSummary.builder()
                .year(3)
                .overallAverage(avg3)
                .totalCredits(60)
                .courses(
                    List.of(
                        CourseGradeSummary.builder()
                            .finalGrade(Math.max(avg3, 10.0))
                            .credit(10)
                            .build()))
                .build());
  }

  private void stubYearAveragesWithFailing(UUID studentId, double avg1, double avg2, double avg3) {
    List<CourseGradeSummary> failingCourses =
        List.of(CourseGradeSummary.builder().finalGrade(avg1).credit(10).build());
    when(gradeCalculationService.computeYearSummary(studentId, 1))
        .thenReturn(
            YearSummary.builder()
                .year(1)
                .overallAverage(avg1)
                .totalCredits(60)
                .courses(failingCourses)
                .build());
    when(gradeCalculationService.computeYearSummary(studentId, 2))
        .thenReturn(
            YearSummary.builder()
                .year(2)
                .overallAverage(avg2)
                .totalCredits(60)
                .courses(failingCourses)
                .build());
    when(gradeCalculationService.computeYearSummary(studentId, 3))
        .thenReturn(
            YearSummary.builder()
                .year(3)
                .overallAverage(avg3)
                .totalCredits(60)
                .courses(failingCourses)
                .build());
  }
}
