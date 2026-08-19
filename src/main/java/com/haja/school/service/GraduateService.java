package com.haja.school.service;

import com.haja.school.model.CursusStatus;
import com.haja.school.model.Graduate;
import com.haja.school.model.YearSummary;
import com.haja.school.repository.JCohortRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCohort;
import com.haja.school.repository.model.JUser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class GraduateService {

  private final JCohortRepository cohortRepository;
  private final JUserRepository userRepository;
  private final GradeCalculationService gradeCalculationService;

  public List<Graduate> getGraduatesByCohort(UUID cohortId) {
    JCohort cohort =
        cohortRepository
            .findById(cohortId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cohort not found"));

    boolean cohortCompleted3Years = hasCompletedThreeYears(cohort.getEntryYear());
    List<JUser> students = userRepository.findByCohortId(cohort.getId());

    return rankGraduates(collectGraduates(students, cohortCompleted3Years));
  }

  public List<Graduate> getAllGraduates() {
    List<Graduate> graduates = new ArrayList<>();

    for (JCohort cohort : cohortRepository.findAll()) {
      boolean cohortCompleted3Years = hasCompletedThreeYears(cohort.getEntryYear());
      List<JUser> students = userRepository.findByCohortId(cohort.getId());
      graduates.addAll(collectGraduates(students, cohortCompleted3Years));
    }

    return rankGraduates(graduates);
  }

  public byte[] exportGraduatesExcel(UUID cohortId) {
    JCohort cohort =
        cohortRepository
            .findById(cohortId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cohort not found"));

    List<Graduate> graduates = getGraduatesByCohort(cohortId);
    return buildExcel(graduates, "Graduates - " + cohort.getRef());
  }

  public byte[] exportAllGraduatesExcel() {
    List<Graduate> graduates = getAllGraduates();
    return buildExcel(graduates, "All Graduates");
  }

  private List<Graduate> collectGraduates(List<JUser> students, boolean cohortCompleted3Years) {
    List<Graduate> graduates = new ArrayList<>();

    for (JUser student : students) {
      if (student.getCursusStatus() == CursusStatus.DROPPED_OUT) {
        continue;
      }

      boolean isGraduatedStatus = student.getCursusStatus() == CursusStatus.GRADUATED;
      if (!cohortCompleted3Years && !isGraduatedStatus) {
        continue;
      }

      Double average = computeGraduationAverage(student);
      if (average != null && average >= 10.0 && allSubjectsPassedFor(student)) {
        graduates.add(
            Graduate.builder()
                .studentRef(student.getRef())
                .name(student.getName())
                .firstname(student.getFirstname())
                .overallAverage(average)
                .build());
      }
    }

    return graduates;
  }

  private boolean allSubjectsPassedFor(JUser student) {
    for (int year = 1; year <= 3; year++) {
      YearSummary summary = gradeCalculationService.computeYearSummary(student.getId(), year);
      if (summary.getCourses() == null) {
        return false;
      }
      for (var course : summary.getCourses()) {
        if (course.getFinalGrade() == null || course.getFinalGrade() < 10.0) {
          return false;
        }
      }
    }
    return true;
  }

  private Double computeGraduationAverage(JUser student) {
    if (student.getCohort() == null || student.getCohort().getEntryYear() == null) {
      return null;
    }

    double weightedSum = 0;
    int totalCredits = 0;
    for (int year = 1; year <= 3; year++) {
      YearSummary summary = gradeCalculationService.computeYearSummary(student.getId(), year);
      if (summary.getOverallAverage() == null) {
        return null;
      }
      weightedSum += summary.getOverallAverage() * summary.getTotalCredits();
      totalCredits += summary.getTotalCredits();
    }

    return totalCredits > 0 ? round2(weightedSum / totalCredits) : null;
  }

  private List<Graduate> rankGraduates(List<Graduate> graduates) {
    graduates.sort(Comparator.comparingDouble(Graduate::getOverallAverage).reversed());

    int rank = 1;
    for (Graduate g : graduates) {
      g.setRank(rank++);
    }

    return graduates;
  }

  private void setCellValue(Cell cell, Object value) {
    if (value == null) {
      cell.setCellValue("");
      return;
    }

    if (value instanceof Number number) {
      cell.setCellValue(number.doubleValue());
      return;
    }

    cell.setCellValue(String.valueOf(value));
  }

  private byte[] buildExcel(List<Graduate> graduates, String sheetTitle) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet(sheetTitle);

      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerFont.setColor(IndexedColors.WHITE.getIndex());
      headerStyle.setFont(headerFont);
      headerStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      Row headerRow = sheet.createRow(0);
      String[] columns = {"Rank", "Student Ref", "Name", "Firstname", "Overall Average"};
      for (int i = 0; i < columns.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(columns[i]);
        cell.setCellStyle(headerStyle);
      }

      int rowIdx = 1;
      for (Graduate graduate : graduates) {
        Row row = sheet.createRow(rowIdx++);
        setCellValue(row.createCell(0), graduate.getRank());
        setCellValue(row.createCell(1), graduate.getStudentRef());
        setCellValue(row.createCell(2), graduate.getName());
        setCellValue(row.createCell(3), graduate.getFirstname());
        setCellValue(row.createCell(4), graduate.getOverallAverage());
      }

      for (int i = 0; i < columns.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error generating Excel report", e);
    }
  }

  private boolean hasCompletedThreeYears(Integer entryYear) {
    if (entryYear == null) {
      return false;
    }
    int currentYear = LocalDate.now().getYear();
    return (currentYear - entryYear) >= 3;
  }

  private double round2(double value) {
    return Math.round(value * 100.0) / 100.0;
  }
}
