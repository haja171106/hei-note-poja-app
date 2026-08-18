package com.haja.school.service.event;

import com.haja.school.endpoint.event.model.TranscriptRequested;
import com.haja.school.file.bucket.transcript.TranscriptBucketService;
import com.haja.school.mail.Email;
import com.haja.school.mail.Mailer;
import com.haja.school.model.YearSummary;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JUser;
import com.haja.school.service.GradeCalculationService;
import jakarta.mail.internet.InternetAddress;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class TranscriptRequestedService implements Consumer<TranscriptRequested> {

  private final TranscriptBucketService transcriptBucketService;
  private final TranscriptPdfGenerator transcriptPdfGenerator;
  private final Mailer mailer;
  private final GradeCalculationService gradeCalculationService;
  private final JUserRepository userRepository;

  @Override
  public void accept(TranscriptRequested event) {
    try {
      JUser student =
          userRepository
              .findWithCohortById(event.getStudentId())
              .orElseThrow(
                  () -> new IllegalStateException("Student not found: " + event.getStudentId()));

      int studyYear = event.getAcademicYear() - student.getCohort().getEntryYear() + 1;
      YearSummary summary = gradeCalculationService.computeYearSummary(student.getId(), studyYear);

      byte[] pdfBytes = transcriptPdfGenerator.generate(student, event.getAcademicYear(), summary);

      String bucketKey =
          "transcripts/"
              + student.getRef()
              + "/"
              + event.getAcademicYear()
              + "_"
              + Instant.now().toEpochMilli()
              + ".pdf";
      transcriptBucketService.upload(pdfBytes, bucketKey);

      String downloadUrl = transcriptBucketService.generatePresignedUrl(bucketKey);

      sendTranscriptEmail(
          student,
          event.getRecipientEmail() == null || event.getRecipientEmail().isBlank()
              ? student.getEmail()
              : event.getRecipientEmail(),
          event.getAcademicYear(),
          summary.getStatus(),
          downloadUrl);

      log.info(
          "Transcript {} processed and emailed to {}",
          event.getRequestId(),
          event.getRecipientEmail() == null || event.getRecipientEmail().isBlank()
              ? student.getEmail()
              : event.getRecipientEmail());
    } catch (Exception e) {
      log.error(
          "Failed to process transcript {} for student {}",
          event.getRequestId(),
          event.getStudentId(),
          e);
      throw new IllegalStateException(
          "Transcript processing failed for request " + event.getRequestId(), e);
    }
  }

  private void sendTranscriptEmail(
      JUser student,
      String recipientEmail,
      Integer academicYear,
      com.haja.school.model.ReportStatus status,
      String downloadUrl)
      throws Exception {
    String statusLabel =
        status == com.haja.school.model.ReportStatus.PROVISIONAL ? "PROVISOIRE" : "COMPLET";
    String subject =
        "Relevé de notes - Ann\u00e9e "
            + academicYear
            + " (Statut: "
            + statusLabel
            + ") - "
            + student.getFirstname()
            + " "
            + student.getName();
    String htmlBody =
        "<html><body>"
            + "<p>Bonjour "
            + student.getFirstname()
            + " "
            + student.getName()
            + ",</p>"
            + "<p>Votre relevé de notes pour l'année académique "
            + academicYear
            + " est prêt. Vous pouvez le télécharger en cliquant sur le lien ci-dessous :</p>"
            + "<p><a href=\""
            + downloadUrl
            + "\">Télécharger votre relevé de notes</a></p>"
            + "<p><strong>Statut du relevé : "
            + statusLabel
            + "</strong></p>"
            + "<p>Ce lien expirera dans 48 heures.</p>"
            + "<p>Cordialement,<br/>HEI Administration</p>"
            + "</body></html>";

    Email email =
        new Email(
            new InternetAddress(recipientEmail),
            List.of(),
            List.of(),
            subject,
            htmlBody,
            List.of());

    mailer.accept(email);
  }
}
