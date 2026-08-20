package com.haja.school.service.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.haja.school.model.CursusStatus;
import com.haja.school.model.ReportStatus;
import com.haja.school.model.Role;
import com.haja.school.model.YearSummary;
import com.haja.school.repository.model.JUser;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TranscriptPdfGeneratorTest {

  @Test
  void generatesReadablePdfForYearSummary() throws Exception {
    JUser student =
        JUser.builder()
            .id(UUID.randomUUID())
            .ref("STD24001")
            .name("Garnier")
            .firstname("Louis")
            .email("louis@student.com")
            .role(Role.STUDENT)
            .cursusStatus(CursusStatus.ACTIVE)
            .build();
    YearSummary summary =
        YearSummary.builder()
            .studentId(student.getId())
            .year(1)
            .courses(List.of())
            .overallAverage(14.5)
            .totalCredits(30)
            .status(ReportStatus.COMPLETE)
            .build();

    byte[] pdf = new TranscriptPdfGenerator().generate(student, 2026, summary);

    assertThat(pdf).isNotEmpty();
    assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
  }
}
