package com.haja.school.service;

import com.haja.school.endpoint.event.EventProducer;
import com.haja.school.endpoint.event.model.TranscriptRequested;
import com.haja.school.endpoint.rest.model.TranscriptRequestAck;
import com.haja.school.model.Role;
import com.haja.school.model.YearSummary;
import com.haja.school.repository.JTranscriptRequestRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JTranscriptRequest;
import com.haja.school.repository.model.JUser;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
@Slf4j
public class TranscriptService {

  private final JUserRepository userRepository;
  private final JTranscriptRequestRepository transcriptRequestRepository;
  private final GradeCalculationService gradeCalculationService;
  private final EventProducer<TranscriptRequested> eventProducer;

  @Transactional
  public TranscriptRequestAck requestTranscript(
      UUID studentId, Integer academicYear, String callerEmail) {
    return requestTranscript(studentId, academicYear, null, callerEmail);
  }

  @Transactional
  public TranscriptRequestAck requestTranscript(
      UUID studentId, Integer academicYear, String recipientEmail, String callerEmail) {
    JUser caller =
        userRepository
            .findByEmail(callerEmail)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Authenticated user not found"));

    JUser student =
        userRepository
            .findById(studentId)
            .filter(u -> u.getRole() == Role.STUDENT)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

    validateAccess(caller, student);

    String resolvedRecipientEmail = resolveRecipientEmail(recipientEmail, student.getEmail());

    int year = resolveStudyYear(student, academicYear);

    YearSummary summary = gradeCalculationService.computeYearSummary(studentId, year);

    JTranscriptRequest request =
        JTranscriptRequest.builder()
            .student(student)
            .academicYear(academicYear)
            .status(summary.getStatus())
            .requestedAt(Instant.now())
            .requestedBy(caller)
            .build();

    JTranscriptRequest saved = transcriptRequestRepository.save(request);

    var event =
        TranscriptRequested.builder()
            .studentId(student.getId())
            .studentRef(student.getRef())
            .studentFirstname(student.getFirstname())
            .studentName(student.getName())
            .studentEmail(student.getEmail())
            .recipientEmail(resolvedRecipientEmail)
            .academicYear(academicYear)
            .status(summary.getStatus())
            .requestId(saved.getId())
            .build();
    eventProducer.accept(List.of(event));

    log.info("Transcript requested {} for student {}", saved.getId(), studentId);

    return TranscriptRequestAck.builder()
        .requestId(saved.getId())
        .message("Transcript request accepted. The document will be sent to your email shortly.")
        .build();
  }

  private void validateAccess(JUser caller, JUser student) {
    if (caller.getRole() == Role.ADMIN) {
      return;
    }
    if (caller.getRole() == Role.STUDENT) {
      if (!caller.getId().equals(student.getId())) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Access denied: student can only request their own transcript");
      }
      return;
    }
    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN, "Access denied: insufficient permissions");
  }

  private String resolveRecipientEmail(String recipientEmail, String defaultRecipientEmail) {
    if (recipientEmail == null || recipientEmail.isBlank()) {
      return defaultRecipientEmail;
    }

    try {
      InternetAddress address = new InternetAddress(recipientEmail.trim());
      address.validate();
      return address.getAddress();
    } catch (AddressException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid recipient email");
    }
  }

  private int resolveStudyYear(JUser student, Integer academicYear) {
    if (academicYear == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "academicYear is required for transcript generation");
    }
    if (student.getCohort() == null || student.getCohort().getEntryYear() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Student is not assigned to a valid cohort");
    }
    int studyYear = academicYear - student.getCohort().getEntryYear() + 1;
    if (studyYear < 1 || studyYear > 3) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Invalid academic year: computed study year " + studyYear + " is out of range (1-3)");
    }
    return studyYear;
  }
}
