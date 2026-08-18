package com.haja.school.service.event;

import com.haja.school.endpoint.event.model.TranscriptRequested;
import com.haja.school.file.bucket.BucketComponent;
import com.haja.school.mail.EmailConf;
import com.haja.school.model.YearSummary;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JUser;
import com.haja.school.service.GradeCalculationService;
import com.openhtmltopdf.pdfboxout.PDFontSupplier;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

@Service
@AllArgsConstructor
@Slf4j
public class TranscriptRequestedService implements Consumer<TranscriptRequested> {

  private final BucketComponent bucketComponent;
  private final EmailConf emailConf;
  private final GradeCalculationService gradeCalculationService;
  private final JUserRepository userRepository;

  @Override
  public void accept(TranscriptRequested event) {
    try {
      JUser student =
          userRepository
              .findById(event.getStudentId())
              .orElseThrow(
                  () -> new IllegalStateException("Student not found: " + event.getStudentId()));

      int studyYear = event.getAcademicYear() - student.getCohort().getEntryYear() + 1;
      YearSummary summary = gradeCalculationService.computeYearSummary(student.getId(), studyYear);

      byte[] pdfBytes = generateTranscriptPdf(student, event.getAcademicYear(), summary);

      File tempFile = Files.createTempFile("transcript", ".pdf").toFile();
      Files.write(tempFile.toPath(), pdfBytes);

      String bucketKey =
          "transcripts/"
              + student.getRef()
              + "/"
              + event.getAcademicYear()
              + "_"
              + Instant.now().toEpochMilli()
              + ".pdf";
      bucketComponent.upload(tempFile, bucketKey);

      sendTranscriptEmail(student, event.getAcademicYear(), summary.getStatus(), pdfBytes);

      tempFile.delete();

      log.info(
          "Transcript {} processed and emailed to {}", event.getRequestId(), student.getEmail());
    } catch (Exception e) {
      log.error(
          "Failed to process transcript {} for student {}",
          event.getRequestId(),
          event.getStudentId(),
          e);
    }
  }

  private byte[] generateTranscriptPdf(JUser student, Integer academicYear, YearSummary summary)
      throws IOException {
    TemplateEngine templateEngine = createTemplateEngine();
    Context context = new Context();
    context.setVariable("student", student);
    context.setVariable("academicYear", academicYear);
    context.setVariable("summary", summary);
    context.setVariable(
        "generatedAt",
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("Europe/Paris"))
            .format(Instant.now()));

    StringWriter stringWriter = new StringWriter();
    templateEngine.process("transcript", context, stringWriter);
    String html = stringWriter.toString();

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    PdfRendererBuilder builder =
        new PdfRendererBuilder().withHtmlContent(html, "/").toStream(outputStream);

    File regularFont = new File("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf");
    File boldFont = new File("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf");
    if (regularFont.exists()) {
      builder.useFont(
          new PDFontSupplier(PDType0Font.load(new PDDocument(), regularFont)), "DejaVu Sans");
    }
    if (boldFont.exists()) {
      builder.useFont(
          new PDFontSupplier(PDType0Font.load(new PDDocument(), boldFont)), "DejaVu Sans");
    }
    builder.run();
    return outputStream.toByteArray();
  }

  private TemplateEngine createTemplateEngine() {
    TemplateEngine templateEngine = new TemplateEngine();
    StringTemplateResolver resolver = new StringTemplateResolver();
    resolver.setTemplateMode("XHTML");
    resolver.setCacheable(false);
    templateEngine.setTemplateResolver(resolver);
    return templateEngine;
  }

  private void sendTranscriptEmail(
      JUser student,
      Integer academicYear,
      com.haja.school.model.ReportStatus status,
      byte[] pdfBytes)
      throws MessagingException, IOException {
    String statusLabel =
        status == com.haja.school.model.ReportStatus.PROVISIONAL ? "PROVISOIRE" : "COMPLET";
    String subject =
        "Relev\u00e9 de notes - Ann\u00e9e "
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
            + "<p>Votre relev\u00e9 de notes pour l\u2019ann\u00e9e acad\u00e9mique "
            + academicYear
            + " est joint \u00e0 cet email.</p>"
            + "<p><strong>Statut du relev\u00e9 : "
            + statusLabel
            + "</strong></p>"
            + "<p>Cordialement,<br/>HEI Administration</p>"
            + "</body></html>";

    File pdfFile = Files.createTempFile("transcript-email", ".pdf").toFile();
    Files.write(pdfFile.toPath(), pdfBytes);

    Session session = Session.getDefaultInstance(new Properties());
    MimeMessage mimeMessage = new MimeMessage(session);
    mimeMessage.setFrom(new InternetAddress(emailConf.getSesSource()));
    mimeMessage.setRecipients(jakarta.mail.Message.RecipientType.TO, student.getEmail());
    mimeMessage.setSubject(subject, "UTF-8");

    var htmlPart = new MimeBodyPart();
    htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");

    var attachmentPart = new MimeBodyPart();
    DataSource dataSource =
        new ByteArrayDataSource(Files.readAllBytes(pdfFile.toPath()), "application/pdf");
    attachmentPart.setDataHandler(new DataHandler(dataSource));
    attachmentPart.setFileName("releve_notes_" + academicYear + "_" + student.getRef() + ".pdf");

    MimeMultipart mimeMultipart = new MimeMultipart("mixed");
    mimeMultipart.addBodyPart(htmlPart);
    mimeMultipart.addBodyPart(attachmentPart);

    mimeMessage.setContent(mimeMultipart);

    var outputStream = new ByteArrayOutputStream();
    mimeMessage.writeTo(outputStream);
    ByteBuffer byteBuffer = ByteBuffer.wrap(outputStream.toByteArray());
    var bytes = new byte[byteBuffer.remaining()];
    byteBuffer.get(bytes);

    var rawEmailRequest =
        SendRawEmailRequest.builder()
            .rawMessage(RawMessage.builder().data(SdkBytes.fromByteArray(bytes)).build())
            .build();

    emailConf.getSesClient().sendRawEmail(rawEmailRequest);

    pdfFile.delete();
  }
}
