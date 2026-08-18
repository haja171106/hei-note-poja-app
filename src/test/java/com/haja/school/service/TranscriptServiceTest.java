package com.haja.school.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.haja.school.endpoint.event.EventProducer;
import com.haja.school.endpoint.event.model.TranscriptRequested;
import com.haja.school.endpoint.rest.model.TranscriptRequestAck;
import com.haja.school.model.ReportStatus;
import com.haja.school.model.Role;
import com.haja.school.model.YearSummary;
import com.haja.school.repository.JTranscriptRequestRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCohort;
import com.haja.school.repository.model.JTranscriptRequest;
import com.haja.school.repository.model.JUser;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TranscriptServiceTest {

  @Mock private JUserRepository userRepository;
  @Mock private JTranscriptRequestRepository transcriptRequestRepository;
  @Mock private GradeCalculationService gradeCalculationService;
  @Mock private EventProducer<TranscriptRequested> eventProducer;

  @InjectMocks private TranscriptService transcriptService;

  private UUID studentId;
  private JUser student;
  private JCohort cohort;

  @BeforeEach
  void setUp() {
    studentId = UUID.randomUUID();
    cohort = JCohort.builder().id(UUID.randomUUID()).ref("A").entryYear(2023).build();
    student =
        JUser.builder()
            .id(studentId)
            .ref("STD00001")
            .name("Dupont")
            .firstname("Jean")
            .email("hei.jean@student.com")
            .role(Role.STUDENT)
            .cohort(cohort)
            .build();
  }

  @Test
  void requestTranscript_asStudent_ownTranscript_success() {
    YearSummary summary =
        YearSummary.builder()
            .studentId(studentId)
            .year(1)
            .courses(List.of())
            .overallAverage(14.0)
            .totalCredits(30)
            .status(ReportStatus.COMPLETE)
            .build();

    when(userRepository.findByEmail("hei.jean@student.com")).thenReturn(Optional.of(student));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(gradeCalculationService.computeYearSummary(studentId, 1)).thenReturn(summary);
    when(transcriptRequestRepository.save(any(JTranscriptRequest.class)))
        .thenAnswer(
            invocation -> {
              JTranscriptRequest req = invocation.getArgument(0);
              req.setId(UUID.randomUUID());
              return req;
            });

    TranscriptRequestAck ack =
        transcriptService.requestTranscript(studentId, 2023, "hei.jean@student.com");

    assertNotNull(ack);
    assertNotNull(ack.getRequestId());
    assertNotNull(ack.getMessage());
    assertTrue(ack.getMessage().contains("accepted"));
    verify(transcriptRequestRepository).save(any(JTranscriptRequest.class));
    verify(eventProducer).accept(any());

    ArgumentCaptor<List<TranscriptRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    TranscriptRequested event = captor.getValue().get(0);
    assertEquals(studentId, event.getStudentId());
    assertEquals(2023, event.getAcademicYear());
    assertEquals(ReportStatus.COMPLETE, event.getStatus());
    assertEquals("STD00001", event.getStudentRef());
    assertEquals("hei.jean@student.com", event.getRecipientEmail());
  }

  @Test
  void requestTranscript_withCustomRecipient_addsRecipientToEvent() {
    YearSummary summary =
        YearSummary.builder()
            .studentId(studentId)
            .year(1)
            .courses(List.of())
            .status(ReportStatus.COMPLETE)
            .build();
    when(userRepository.findByEmail("hei.jean@student.com")).thenReturn(Optional.of(student));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(gradeCalculationService.computeYearSummary(studentId, 1)).thenReturn(summary);
    when(transcriptRequestRepository.save(any(JTranscriptRequest.class)))
        .thenAnswer(
            invocation -> {
              JTranscriptRequest request = invocation.getArgument(0);
              request.setId(UUID.randomUUID());
              return request;
            });

    transcriptService.requestTranscript(
        studentId, 2023, "recipient@example.com", "hei.jean@student.com");

    ArgumentCaptor<List<TranscriptRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    assertEquals("recipient@example.com", captor.getValue().get(0).getRecipientEmail());
  }

  @Test
  void requestTranscript_asAdmin_anyStudent_success() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    YearSummary summary =
        YearSummary.builder()
            .studentId(studentId)
            .year(1)
            .courses(List.of())
            .overallAverage(12.0)
            .totalCredits(30)
            .status(ReportStatus.PROVISIONAL)
            .build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(gradeCalculationService.computeYearSummary(studentId, 1)).thenReturn(summary);
    when(transcriptRequestRepository.save(any(JTranscriptRequest.class)))
        .thenAnswer(
            invocation -> {
              JTranscriptRequest req = invocation.getArgument(0);
              req.setId(UUID.randomUUID());
              return req;
            });

    TranscriptRequestAck ack =
        transcriptService.requestTranscript(studentId, 2023, "hei.admin@admin.com");

    assertNotNull(ack);
    assertNotNull(ack.getRequestId());
    verify(transcriptRequestRepository).save(any(JTranscriptRequest.class));
    verify(eventProducer).accept(any());
  }

  @Test
  void requestTranscript_asStudent_otherStudent_throwsForbidden() {
    JUser otherStudent =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.other@student.com")
            .role(Role.STUDENT)
            .cohort(cohort)
            .build();

    when(userRepository.findByEmail("hei.other@student.com")).thenReturn(Optional.of(otherStudent));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> transcriptService.requestTranscript(studentId, 2023, "hei.other@student.com"));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
  }

  @Test
  void requestTranscript_unknownCaller_throwsUnauthorized() {
    when(userRepository.findByEmail("unknown@unknown.com")).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> transcriptService.requestTranscript(studentId, 2023, "unknown@unknown.com"));

    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
  }

  @Test
  void requestTranscript_studentNotFound_throwsNotFound() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(userRepository.findById(studentId)).thenReturn(Optional.empty());

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> transcriptService.requestTranscript(studentId, 2023, "hei.admin@admin.com"));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
  }

  @Test
  void requestTranscript_targetIsNotStudent_throwsNotFound() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();
    JUser teacher =
        JUser.builder().id(studentId).email("hei.teacher@teacher.com").role(Role.TEACHER).build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(teacher));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> transcriptService.requestTranscript(studentId, 2023, "hei.admin@admin.com"));

    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
  }

  @Test
  void requestTranscript_invalidYear_throwsBadRequest() {
    JUser admin =
        JUser.builder().id(UUID.randomUUID()).email("hei.admin@admin.com").role(Role.ADMIN).build();

    when(userRepository.findByEmail("hei.admin@admin.com")).thenReturn(Optional.of(admin));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> transcriptService.requestTranscript(studentId, 2020, "hei.admin@admin.com"));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }

  @Test
  void requestTranscript_asTeacher_throwsForbidden() {
    JUser teacher =
        JUser.builder()
            .id(UUID.randomUUID())
            .email("hei.teacher@teacher.com")
            .role(Role.TEACHER)
            .build();

    when(userRepository.findByEmail("hei.teacher@teacher.com")).thenReturn(Optional.of(teacher));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> transcriptService.requestTranscript(studentId, 2023, "hei.teacher@teacher.com"));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
  }

  @Test
  void requestTranscript_provisionalStatus_savedAndEventCorrectly() {
    YearSummary summary =
        YearSummary.builder()
            .studentId(studentId)
            .year(1)
            .courses(List.of())
            .overallAverage(null)
            .totalCredits(30)
            .status(ReportStatus.PROVISIONAL)
            .build();

    when(userRepository.findByEmail("hei.jean@student.com")).thenReturn(Optional.of(student));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(gradeCalculationService.computeYearSummary(studentId, 1)).thenReturn(summary);
    when(transcriptRequestRepository.save(any(JTranscriptRequest.class)))
        .thenAnswer(
            invocation -> {
              JTranscriptRequest req = invocation.getArgument(0);
              req.setId(UUID.randomUUID());
              return req;
            });

    TranscriptRequestAck ack =
        transcriptService.requestTranscript(studentId, 2023, "hei.jean@student.com");

    assertNotNull(ack);
    verify(transcriptRequestRepository)
        .save(argThat(req -> req.getStatus() == ReportStatus.PROVISIONAL));

    ArgumentCaptor<List<TranscriptRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    assertEquals(ReportStatus.PROVISIONAL, captor.getValue().get(0).getStatus());
  }

  @Test
  void requestTranscript_cohortRequired_throwsBadRequest() {
    JUser studentWithoutCohort =
        JUser.builder()
            .id(studentId)
            .ref("STD00002")
            .name("Test")
            .firstname("NoCohort")
            .email("hei.nocohort@student.com")
            .role(Role.STUDENT)
            .cohort(null)
            .build();

    when(userRepository.findByEmail("hei.nocohort@student.com"))
        .thenReturn(Optional.of(studentWithoutCohort));
    when(userRepository.findById(studentId)).thenReturn(Optional.of(studentWithoutCohort));

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> transcriptService.requestTranscript(studentId, 2023, "hei.nocohort@student.com"));

    assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
  }
}
