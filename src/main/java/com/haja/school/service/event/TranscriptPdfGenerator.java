package com.haja.school.service.event;

import com.haja.school.model.YearSummary;
import com.haja.school.repository.model.JUser;
import com.openhtmltopdf.pdfboxout.PDFontSupplier;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Component
@Slf4j
public class TranscriptPdfGenerator {

  public byte[] generate(JUser student, Integer academicYear, YearSummary summary)
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
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode("HTML");
    resolver.setCacheable(false);
    templateEngine.setTemplateResolver(resolver);
    return templateEngine;
  }
}
