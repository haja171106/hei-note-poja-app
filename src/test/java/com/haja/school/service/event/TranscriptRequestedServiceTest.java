package com.haja.school.service.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.haja.school.endpoint.event.model.TranscriptRequested;
import com.haja.school.file.bucket.transcript.TranscriptBucketService;
import com.haja.school.mail.Email;
import com.haja.school.mail.Mailer;
import com.haja.school.model.YearSummary;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCohort;
import com.haja.school.repository.model.JUser;
import com.haja.school.service.GradeCalculationService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TranscriptRequestedServiceTest {

  @Mock private TranscriptBucketService transcriptBucketService;
  @Mock private TranscriptPdfGenerator transcriptPdfGenerator;
  @Mock private Mailer mailer;
  @Mock private GradeCalculationService gradeCalculationService;
  @Mock private JUserRepository userRepository;

  @InjectMocks private TranscriptRequestedService transcriptRequestedService;

  private UUID studentId;
  private UUID requestId;
  private JUser student;
  private TranscriptRequested event;
  private YearSummary yearSummary;

  private static final String DOWNLOAD_URL = "http://s3.example.com/transcript.pdf";

  @BeforeEach
  void setUp() {
    studentId = UUID.randomUUID();
    requestId = UUID.randomUUID();

    JCohort cohort =
        JCohort.builder().id(UUID.randomUUID()).ref("COHORT-2023").entryYear(2023).build();

    student =
        JUser.builder()
            .id(studentId)
            .ref("STU-001")
            .name("Doe")
            .firstname("John")
            .email("hajaravahatra@gmail.com")
            .cohort(cohort)
            .build();

    event =
        TranscriptRequested.builder()
            .studentId(studentId)
            .studentRef("STU-001")
            .studentFirstname("John")
            .studentName("Doe")
            .studentEmail("hajaravahatra@gmail.com")
            .academicYear(2024)
            .status(com.haja.school.model.ReportStatus.COMPLETE)
            .requestId(requestId)
            .build();

    yearSummary =
        YearSummary.builder()
            .studentId(studentId)
            .year(2)
            .overallAverage(15.0)
            .totalCredits(60)
            .status(com.haja.school.model.ReportStatus.COMPLETE)
            .courses(List.of())
            .build();
  }

  private void stubHappyPath(JUser s, YearSummary summary, TranscriptRequested e) throws Exception {
    when(userRepository.findById(studentId)).thenReturn(Optional.of(s));
    when(gradeCalculationService.computeYearSummary(
            studentId, e.getAcademicYear() - s.getCohort().getEntryYear() + 1))
        .thenReturn(summary);
    when(transcriptPdfGenerator.generate(eq(s), eq(e.getAcademicYear()), eq(summary)))
        .thenReturn(new byte[] {1, 2, 3});
    when(transcriptBucketService.upload(any(byte[].class), anyString()))
        .thenReturn("transcript-key");
    when(transcriptBucketService.generatePresignedUrl(anyString())).thenReturn(DOWNLOAD_URL);
  }

  @Test
  void accept_studentNotFound_logsErrorAndDoesNotProceed() throws Exception {
    when(userRepository.findById(studentId)).thenReturn(Optional.empty());

    transcriptRequestedService.accept(event);

    verify(userRepository).findById(studentId);
    verify(transcriptPdfGenerator, never()).generate(any(), anyInt(), any());
    verify(transcriptBucketService, never()).upload(any(), anyString());
    verify(mailer, never()).accept(any());
  }

  @Test
  void accept_studentFound_generatesPdfUploadsAndSendsEmail() throws Exception {
    stubHappyPath(student, yearSummary, event);

    transcriptRequestedService.accept(event);

    verify(userRepository).findById(studentId);
    verify(gradeCalculationService).computeYearSummary(studentId, 2);
    verify(transcriptPdfGenerator).generate(student, 2024, yearSummary);
    verify(transcriptBucketService).upload(any(byte[].class), anyString());
    verify(transcriptBucketService).generatePresignedUrl(anyString());
    verify(mailer).accept(any(Email.class));
  }

  @Test
  void accept_sendsEmailWithCorrectRecipient() throws Exception {
    stubHappyPath(student, yearSummary, event);

    transcriptRequestedService.accept(event);

    ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(emailCaptor.capture());

    Email sentEmail = emailCaptor.getValue();
    assert sentEmail.to().toString().contains("hajaravahatra@gmail.com");
  }

  @Test
  void accept_sendsEmailToRequestedRecipient() throws Exception {
    stubHappyPath(student, yearSummary, event);
    event.setRecipientEmail("recipient@example.com");

    transcriptRequestedService.accept(event);

    ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(emailCaptor.capture());
    assert emailCaptor.getValue().to().toString().contains("recipient@example.com");
  }

  @Test
  void accept_sendsEmailWithDownloadLink() throws Exception {
    stubHappyPath(student, yearSummary, event);

    transcriptRequestedService.accept(event);

    ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(emailCaptor.capture());

    Email sentEmail = emailCaptor.getValue();
    assert sentEmail.htmlBody().contains(DOWNLOAD_URL);
    assert sentEmail.htmlBody().contains("Télécharger votre relevé de notes");
  }

  @Test
  void accept_sendsEmailWithCorrectSubject() throws Exception {
    stubHappyPath(student, yearSummary, event);

    transcriptRequestedService.accept(event);

    ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(emailCaptor.capture());

    Email sentEmail = emailCaptor.getValue();
    assert sentEmail.subject().contains("Relevé de notes");
    assert sentEmail.subject().contains("2024");
    assert sentEmail.subject().contains("John Doe");
  }

  @Test
  void accept_cohortEntryYearNull_logsErrorAndDoesNotProceed() throws Exception {
    JCohort cohortNoEntryYear =
        JCohort.builder().id(UUID.randomUUID()).ref("COHORT-NULL").entryYear(null).build();
    JUser studentWithNullEntryYear =
        JUser.builder()
            .id(studentId)
            .ref("STU-001")
            .name("Doe")
            .firstname("John")
            .email("hajaravahatra@gmail.com")
            .cohort(cohortNoEntryYear)
            .build();
    when(userRepository.findById(studentId)).thenReturn(Optional.of(studentWithNullEntryYear));

    transcriptRequestedService.accept(event);

    verify(userRepository).findById(studentId);
    verify(gradeCalculationService, never()).computeYearSummary(any(), anyInt());
    verify(transcriptBucketService, never()).upload(any(), anyString());
  }

  @Test
  void accept_nullCohort_logsErrorAndDoesNotProceed() throws Exception {
    JUser studentWithNullCohort =
        JUser.builder()
            .id(studentId)
            .ref("STU-001")
            .name("Doe")
            .firstname("John")
            .email("hajaravahatra@gmail.com")
            .cohort(null)
            .build();
    when(userRepository.findById(studentId)).thenReturn(Optional.of(studentWithNullCohort));

    transcriptRequestedService.accept(event);

    verify(userRepository).findById(studentId);
    verify(gradeCalculationService, never()).computeYearSummary(any(), anyInt());
  }

  @Test
  void accept_provisionalStatus_sendsEmailWithProvisionalLabel() throws Exception {
    YearSummary provisionalSummary =
        YearSummary.builder()
            .studentId(studentId)
            .year(1)
            .overallAverage(null)
            .totalCredits(60)
            .status(com.haja.school.model.ReportStatus.PROVISIONAL)
            .courses(List.of())
            .build();
    TranscriptRequested provisionalEvent =
        TranscriptRequested.builder()
            .studentId(studentId)
            .studentRef("STU-001")
            .studentFirstname("John")
            .studentName("Doe")
            .studentEmail("hajaravahatra@gmail.com")
            .academicYear(2023)
            .status(com.haja.school.model.ReportStatus.PROVISIONAL)
            .requestId(requestId)
            .build();

    stubHappyPath(student, provisionalSummary, provisionalEvent);

    transcriptRequestedService.accept(provisionalEvent);

    ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
    verify(mailer).accept(emailCaptor.capture());

    Email sentEmail = emailCaptor.getValue();
    assert sentEmail.subject().contains("PROVISOIRE");
    assert sentEmail.htmlBody().contains("PROVISOIRE");
  }

  @Test
  void accept_academicYearMismatch_computesCorrectStudyYear() throws Exception {
    JCohort cohort2020 = JCohort.builder().id(UUID.randomUUID()).ref("OLD").entryYear(2020).build();
    JUser olderStudent =
        JUser.builder()
            .id(studentId)
            .ref("STU-002")
            .name("Old")
            .firstname("Student")
            .email("hajaravahatra@gmail.com")
            .cohort(cohort2020)
            .build();
    TranscriptRequested oldEvent =
        TranscriptRequested.builder()
            .studentId(studentId)
            .studentRef("STU-002")
            .studentFirstname("Student")
            .studentName("Old")
            .studentEmail("hajaravahatra@gmail.com")
            .academicYear(2023)
            .status(com.haja.school.model.ReportStatus.COMPLETE)
            .requestId(requestId)
            .build();

    stubHappyPath(olderStudent, yearSummary, oldEvent);

    transcriptRequestedService.accept(oldEvent);

    verify(gradeCalculationService).computeYearSummary(studentId, 4);
  }
}
