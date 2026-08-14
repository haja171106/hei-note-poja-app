package com.haja.school.service;

import com.haja.school.model.Graduate;
import com.haja.school.repository.JCohortRepository;
import com.haja.school.repository.JGradeRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCohort;
import com.haja.school.repository.model.JGrade;
import com.haja.school.repository.model.JUser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
  private final JGradeRepository gradeRepository;

  public List<Graduate> getGraduatesByCohort(UUID cohortId) {
    JCohort cohort =
        cohortRepository
            .findById(cohortId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cohort not found"));

    List<JUser> students = userRepository.findByCohortId(cohort.getId());
    List<Graduate> list = new ArrayList<>();

    for (JUser student : students) {
      List<JGrade> grades = gradeRepository.findByStudentId(student.getId());
      double average = 0.0;
      if (!grades.isEmpty()) {
        double sum = grades.stream().mapToDouble(JGrade::getValue).sum();
        average = Math.round((sum / grades.size()) * 100.0) / 100.0;
      }

      list.add(
          Graduate.builder()
              .studentRef(student.getRef())
              .name(student.getName())
              .firstname(student.getFirstname())
              .overallAverage(average)
              .build());
    }

    list.sort(Comparator.comparingDouble(Graduate::getOverallAverage).reversed());

    int rank = 1;
    for (Graduate g : list) {
      g.setRank(rank++);
    }

    return list;
  }

  public byte[] exportGraduatesExcel(UUID cohortId) {
    JCohort cohort =
        cohortRepository
            .findById(cohortId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cohort not found"));

    List<Graduate> graduates = getGraduatesByCohort(cohortId);

    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Graduates - " + cohort.getRef());

      // Header Style
      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerFont.setColor(IndexedColors.WHITE.getIndex());
      headerStyle.setFont(headerFont);
      headerStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

      // Header Row
      Row headerRow = sheet.createRow(0);
      String[] columns = {"Rank", "Student Ref", "Name", "Firstname", "Overall Average"};
      for (int i = 0; i < columns.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(columns[i]);
        cell.setCellStyle(headerStyle);
      }

      // Data Rows
      int rowIdx = 1;
      for (Graduate graduate : graduates) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(graduate.getRank());
        row.createCell(1).setCellValue(graduate.getStudentRef());
        row.createCell(2).setCellValue(graduate.getName());
        row.createCell(3).setCellValue(graduate.getFirstname());
        row.createCell(4).setCellValue(graduate.getOverallAverage());
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
}
