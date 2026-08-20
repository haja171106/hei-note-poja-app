package com.haja.school.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.haja.school.conf.FacadeIT;
import com.haja.school.endpoint.event.EventProducer;
import com.haja.school.endpoint.event.model.TranscriptRequested;
import com.haja.school.endpoint.rest.model.CreateCohortRequest;
import com.haja.school.endpoint.rest.model.GroupChangeRequest;
import com.haja.school.endpoint.rest.model.LoginRequest;
import com.haja.school.endpoint.rest.model.LoginResponse;
import com.haja.school.endpoint.rest.model.StudentCreateRequest;
import com.haja.school.endpoint.rest.model.TeacherCreateRequest;
import com.haja.school.mail.Mailer;
import com.haja.school.model.Cohort;
import com.haja.school.model.Group;
import com.haja.school.model.Role;
import com.haja.school.model.Student;
import com.haja.school.model.Teacher;
import com.haja.school.repository.JTranscriptRequestRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JUser;
import com.haja.school.service.event.TranscriptRequestedService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("local")
class CriticalWorkflowsIT extends FacadeIT {

  @Value("${local.server.port}")
  private int port;

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JUserRepository userRepository;
  @Autowired private JTranscriptRequestRepository transcriptRequestRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TranscriptRequestedService transcriptRequestedService;

  @MockBean private EventProducer<TranscriptRequested> transcriptEventProducer;
  @MockBean private Mailer mailer;

  private String adminToken;
  private String adminEmail;

  @BeforeEach
  void setUpAdmin() {
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    adminEmail = "integration.admin." + suffix + "@admin.com";
    userRepository.save(
        JUser.builder()
            .ref("ADM" + suffix)
            .name("Integration")
            .firstname("Admin")
            .email(adminEmail)
            .password(passwordEncoder.encode("Password123!"))
            .role(Role.ADMIN)
            .build());

    LoginResponse login =
        post("/auth/login", new LoginRequest(adminEmail, "Password123!"), null, LoginResponse.class)
            .getBody();
    assertThat(login).isNotNull();
    adminToken = login.getToken();
  }

  @Test
  void adminCanCreateStudentTeacherAndManageGroups() {
    Cohort cohort = createCohort("X");
    Student student =
        post(
                "/students",
                StudentCreateRequest.builder()
                    .name("Student")
                    .firstname("Group")
                    .cohortId(cohort.getId())
                    .build(),
                adminToken,
                Student.class)
            .getBody();
    Teacher teacher =
        post(
                "/teachers",
                TeacherCreateRequest.builder().name("Teacher").firstname("Group").build(),
                adminToken,
                Teacher.class)
            .getBody();
    assertThat(student).isNotNull();
    assertThat(teacher).isNotNull();

    List<Group> groups =
        postForList(
                "/promotions/" + cohort.getId() + "/groups/generate",
                adminToken,
                new ParameterizedTypeReference<List<Group>>() {})
            .getBody();
    assertThat(groups).hasSize(1);

    Student movedStudent =
        patch(
                "/students/" + student.getId() + "/group",
                new GroupChangeRequest(groups.get(0).getId(), java.time.LocalDate.now()),
                adminToken,
                Student.class)
            .getBody();
    assertThat(movedStudent).isNotNull();
    assertThat(movedStudent.getGroupId()).isEqualTo(groups.get(0).getId());
  }

  @Test
  void graduateExportReturnsAnExcelDocument() {
    Cohort cohort = createCohort("W");

    ResponseEntity<byte[]> response =
        exchange(
            "/promotions/" + cohort.getId() + "/graduates/export",
            HttpMethod.GET,
            null,
            adminToken,
            byte[].class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType().toString()).contains("spreadsheetml.sheet");
    assertThat(response.getBody()).startsWith(new byte[] {'P', 'K'});
  }

  @Test
  void transcriptRequestPersistsAndPublishesEmailWorkflowEvent() throws Exception {
    Cohort cohort = createCohort("V");
    Student student =
        post(
                "/students",
                StudentCreateRequest.builder()
                    .name("Transcript")
                    .firstname("Student")
                    .cohortId(cohort.getId())
                    .build(),
                adminToken,
                Student.class)
            .getBody();

    ResponseEntity<String> response =
        exchange(
            "/students/" + student.getId() + "/transcript?academicYear=" + cohort.getEntryYear(),
            HttpMethod.POST,
            null,
            adminToken,
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(transcriptRequestRepository.findAll())
        .anyMatch(r -> r.getStudent().getId().equals(student.getId()));
    ArgumentCaptor<List<TranscriptRequested>> events = ArgumentCaptor.forClass(List.class);
    verify(transcriptEventProducer).accept(events.capture());
    assertThat(events.getValue()).hasSize(1);
    assertThat(events.getValue().get(0).getRecipientEmail()).isEqualTo(student.getEmail());

    transcriptRequestedService.accept(events.getValue().get(0));
    verify(mailer).accept(any());

    Path transcriptDirectory = Path.of("tmp", "s3-mock", "transcripts", student.getRef());
    Path generatedPdf;
    try (var files = Files.list(transcriptDirectory)) {
      generatedPdf =
          files.filter(path -> path.toString().endsWith(".pdf")).findFirst().orElseThrow();
    }
    assertThat(Files.readAllBytes(generatedPdf)).startsWith(new byte[] {'%', 'P', 'D', 'F'});

    String mockS3Path =
        Path.of("transcripts", student.getRef(), generatedPdf.getFileName().toString())
            .toString()
            .replace('\\', '/');
    ResponseEntity<byte[]> downloaded =
        exchange("/mock-s3/" + mockS3Path, HttpMethod.GET, null, null, byte[].class);
    assertThat(downloaded.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(downloaded.getBody()).startsWith(new byte[] {'%', 'P', 'D', 'F'});
  }

  @Test
  void healthAndMockS3EndpointsWorkWithoutExternalServices() throws Exception {
    ResponseEntity<String> ping = exchange("/ping", HttpMethod.GET, null, null, String.class);
    assertThat(ping.getStatusCode()).isEqualTo(HttpStatus.OK);

    ResponseEntity<String> database =
        exchange("/health/db", HttpMethod.GET, null, null, String.class);
    assertThat(database.getStatusCode()).isEqualTo(HttpStatus.OK);

    Path mockFile = Path.of("tmp", "s3-mock", "integration", "health.pdf");
    Files.createDirectories(mockFile.getParent());
    Files.write(mockFile, new byte[] {'P', 'D', 'F'});
    try {
      ResponseEntity<byte[]> downloaded =
          exchange("/mock-s3/integration/health.pdf", HttpMethod.GET, null, null, byte[].class);
      assertThat(downloaded.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(downloaded.getBody()).containsExactly('P', 'D', 'F');
    } finally {
      Files.deleteIfExists(mockFile);
    }

    ResponseEntity<String> email =
        exchange("/health/email?to=health@example.com", HttpMethod.GET, null, null, String.class);
    assertThat(email.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(mailer, times(5)).accept(any());
  }

  @Test
  void studentCannotUseAdminOnlyStudentCreationEndpoint() {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    Cohort cohort = createCohort("P");
    String studentEmail = "integration.student." + suffix + "@student.com";
    JUser student =
        userRepository.save(
            JUser.builder()
                .ref("STD" + suffix)
                .name("Student")
                .firstname("Permission")
                .email(studentEmail)
                .password(passwordEncoder.encode("Password123!"))
                .role(Role.STUDENT)
                .build());
    LoginResponse login =
        post(
                "/auth/login",
                new LoginRequest(studentEmail, "Password123!"),
                null,
                LoginResponse.class)
            .getBody();
    assertThat(login).isNotNull();

    ResponseEntity<String> response =
        exchange(
            "/students",
            HttpMethod.POST,
            StudentCreateRequest.builder()
                .name("Forbidden")
                .firstname("Student")
                .cohortId(cohort.getId())
                .build(),
            login.getToken(),
            String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(userRepository.findById(student.getId())).isPresent();
  }

  private Cohort createCohort(String ref) {
    Cohort cohort =
        post(
                "/promotions",
                CreateCohortRequest.builder()
                    .ref(ref)
                    .entryYear(java.time.LocalDate.now().getYear())
                    .build(),
                adminToken,
                Cohort.class)
            .getBody();
    assertThat(cohort).isNotNull();
    return cohort;
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }

  private HttpHeaders headers(String token) {
    HttpHeaders headers = new HttpHeaders();
    if (token != null) headers.setBearerAuth(token);
    return headers;
  }

  private <T> ResponseEntity<T> post(String path, Object body, String token, Class<T> type) {
    return exchange(path, HttpMethod.POST, body, token, type);
  }

  private <T> ResponseEntity<T> patch(String path, Object body, String token, Class<T> type) {
    return exchange(path, HttpMethod.PATCH, body, token, type);
  }

  private <T> ResponseEntity<T> exchange(
      String path, HttpMethod method, Object body, String token, Class<T> type) {
    return restTemplate.exchange(url(path), method, new HttpEntity<>(body, headers(token)), type);
  }

  private <T> ResponseEntity<T> postForList(
      String path, String token, ParameterizedTypeReference<T> type) {
    return restTemplate.exchange(
        url(path), HttpMethod.POST, new HttpEntity<>(headers(token)), type);
  }
}
