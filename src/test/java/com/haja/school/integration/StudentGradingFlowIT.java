package com.haja.school.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.haja.school.conf.FacadeIT;
import com.haja.school.endpoint.rest.model.CourseCreateRequest;
import com.haja.school.endpoint.rest.model.CreateCohortRequest;
import com.haja.school.endpoint.rest.model.ExamCreateRequest;
import com.haja.school.endpoint.rest.model.GradeUpsertRequest;
import com.haja.school.endpoint.rest.model.LoginRequest;
import com.haja.school.endpoint.rest.model.LoginResponse;
import com.haja.school.endpoint.rest.model.StudentCreateRequest;
import com.haja.school.endpoint.rest.model.TeacherAssignmentRequest;
import com.haja.school.endpoint.rest.model.TeacherCreateRequest;
import com.haja.school.model.Cohort;
import com.haja.school.model.Course;
import com.haja.school.model.Exam;
import com.haja.school.model.Grade;
import com.haja.school.model.GradeHistory;
import com.haja.school.model.Group;
import com.haja.school.model.ReportStatus;
import com.haja.school.model.Role;
import com.haja.school.model.Student;
import com.haja.school.model.Teacher;
import com.haja.school.model.TeacherCourseBoard;
import com.haja.school.model.TeacherGradeBoard;
import com.haja.school.model.YearSummary;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JUser;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;

class StudentGradingFlowIT extends FacadeIT {

  @Value("${local.server.port}")
  private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private JUserRepository userRepository;

  private String adminToken;
  private String adminEmail;
  private final int entryYear = LocalDate.now().getYear();

  @BeforeEach
  void seedAdminAndLogin() {
    restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());

    String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    adminEmail = "hei.root" + uniqueSuffix + "@admin.com";

    userRepository.save(
        JUser.builder()
            .ref("ADM" + uniqueSuffix)
            .name("Admin")
            .firstname("Root" + uniqueSuffix)
            .email(adminEmail)
            .password("Password123!")
            .role(Role.ADMIN)
            .build());

    LoginResponse login =
        post("/auth/login", new LoginRequest(adminEmail, "Password123!"), null, LoginResponse.class)
            .getBody();

    assertThat(login).isNotNull();
    assertThat(login.getRole()).isEqualTo(Role.ADMIN);
    adminToken = login.getToken();
  }

  @Test
  void fullGradingWorkflow_producesCompleteYearSummaryAndHistory() {
    Cohort cohort =
        post(
                "/promotions",
                CreateCohortRequest.builder().ref("Z").entryYear(entryYear).build(),
                adminToken,
                Cohort.class)
            .getBody();
    assertThat(cohort).isNotNull();

    Student student =
        post(
                "/students",
                StudentCreateRequest.builder()
                    .name("Rakoto")
                    .firstname("Fara")
                    .cohortId(cohort.getId())
                    .build(),
                adminToken,
                Student.class)
            .getBody();
    assertThat(student).isNotNull();
    assertThat(student.getRef()).startsWith("STD");
    assertThat(student.getEmail()).isEqualTo("hei.fara@student.com");

    ResponseEntity<List<Group>> groupsResponse =
        postForList(
            "/promotions/" + cohort.getId() + "/groups/generate",
            adminToken,
            new ParameterizedTypeReference<>() {});
    assertThat(groupsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Group> groups = groupsResponse.getBody();
    assertThat(groups).hasSize(1);
    assertThat(groups.get(0).getRef()).isEqualTo("Z1");
    assertThat(groups.get(0).getStudentCount()).isEqualTo(1);

    Teacher teacher =
        post(
                "/teachers",
                TeacherCreateRequest.builder().name("Rabe").firstname("Jean").build(),
                adminToken,
                Teacher.class)
            .getBody();
    assertThat(teacher).isNotNull();
    assertThat(teacher.getEmail()).isEqualTo("hei.jean@teacher.com");

    Course course =
        post(
                "/courses",
                CourseCreateRequest.builder()
                    .ref("ALG1")
                    .title("Algorithmique")
                    .credit(30)
                    .semesterNumber(1)
                    .build(),
                adminToken,
                Course.class)
            .getBody();
    assertThat(course).isNotNull();

    ResponseEntity<Void> assignResponse =
        post(
            "/courses/" + course.getId() + "/teachers",
            TeacherAssignmentRequest.builder()
                .teacherId(teacher.getId())
                .academicYear(entryYear)
                .build(),
            adminToken,
            Void.class);
    assertThat(assignResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    ResponseEntity<List<Course>> teacherCourses =
        getForList(
            "/teachers/" + teacher.getId() + "/courses",
            adminToken,
            new ParameterizedTypeReference<>() {});
    assertThat(teacherCourses.getBody()).extracting(Course::getId).containsExactly(course.getId());

    Exam exam =
        post(
                "/courses/" + course.getId() + "/exams",
                ExamCreateRequest.builder()
                    .label("Examen final")
                    .dateExam(Instant.now())
                    .coefficient(1.0)
                    .academicYear(entryYear)
                    .build(),
                adminToken,
                Exam.class)
            .getBody();
    assertThat(exam).isNotNull();

    YearSummary provisional =
        get("/students/" + student.getId() + "/years/1/summary", adminToken, YearSummary.class)
            .getBody();
    assertThat(provisional).isNotNull();
    assertThat(provisional.getStatus()).isEqualTo(ReportStatus.PROVISIONAL);
    assertThat(provisional.getOverallAverage()).isNull();

    Grade grade =
        put(
                "/exams/" + exam.getId() + "/grades/" + student.getId(),
                GradeUpsertRequest.builder().value(15.0).reason("Saisie initiale").build(),
                adminToken,
                Grade.class)
            .getBody();
    assertThat(grade).isNotNull();
    assertThat(grade.getValue()).isEqualTo(15.0);

    YearSummary complete =
        get("/students/" + student.getId() + "/years/1/summary", adminToken, YearSummary.class)
            .getBody();
    assertThat(complete).isNotNull();
    assertThat(complete.getStatus()).isEqualTo(ReportStatus.COMPLETE);
    assertThat(complete.getOverallAverage()).isEqualTo(15.0);
    assertThat(complete.getTotalCredits()).isEqualTo(30);

    ResponseEntity<List<GradeHistory>> historyResponse =
        getForList(
            "/students/" + student.getId() + "/grades/history",
            adminToken,
            new ParameterizedTypeReference<>() {});
    assertThat(historyResponse.getBody()).hasSize(1);
    assertThat(historyResponse.getBody().get(0).getNewValue()).isEqualTo(15.0);
    assertThat(historyResponse.getBody().get(0).getOldValue()).isNull();

    ResponseEntity<List<com.haja.school.model.Graduate>> graduatesResponse =
        getForList(
            "/promotions/" + cohort.getId() + "/graduates",
            adminToken,
            new ParameterizedTypeReference<>() {});
    assertThat(graduatesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(graduatesResponse.getBody()).isEmpty();
  }

  @Test
  void login_rejectsMismatchedEmailDomainForRole() {
    String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    String fakeEmail = "hei.fake" + uniqueSuffix + "@student.com";
    userRepository.save(
        JUser.builder()
            .ref("STD" + uniqueSuffix)
            .name("Nobody")
            .firstname("Fake" + uniqueSuffix)
            .email(fakeEmail)
            .password("Whatever1!")
            .role(Role.ADMIN)
            .build());

    ResponseEntity<String> response =
        restTemplate.exchange(
            url("/auth/login"),
            HttpMethod.POST,
            new HttpEntity<>(new LoginRequest(fakeEmail, "Whatever1!")),
            String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void teacherGradeUpdate_recalculatesWeightedAverage() {
    String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

    Cohort cohort =
        post(
                "/promotions",
                CreateCohortRequest.builder().ref("Y").entryYear(entryYear).build(),
                adminToken,
                Cohort.class)
            .getBody();
    assertThat(cohort).isNotNull();

    Student student =
        post(
                "/students",
                StudentCreateRequest.builder()
                    .name("Garnier")
                    .firstname("Louis" + uniqueSuffix)
                    .cohortId(cohort.getId())
                    .build(),
                adminToken,
                Student.class)
            .getBody();
    assertThat(student).isNotNull();

    Teacher teacher =
        post(
                "/teachers",
                TeacherCreateRequest.builder()
                    .name("Teacher")
                    .firstname("Jean" + uniqueSuffix)
                    .build(),
                adminToken,
                Teacher.class)
            .getBody();
    assertThat(teacher).isNotNull();

    Course course =
        post(
                "/courses",
                CourseCreateRequest.builder()
                    .ref("AVG" + uniqueSuffix.substring(0, 5))
                    .title("Weighted average test")
                    .credit(5)
                    .semesterNumber(1)
                    .build(),
                adminToken,
                Course.class)
            .getBody();
    assertThat(course).isNotNull();

    ResponseEntity<Void> assignment =
        post(
            "/courses/" + course.getId() + "/teachers",
            TeacherAssignmentRequest.builder()
                .teacherId(teacher.getId())
                .academicYear(entryYear)
                .build(),
            adminToken,
            Void.class);
    assertThat(assignment.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    Exam continuousAssessment =
        post(
                "/courses/" + course.getId() + "/exams",
                ExamCreateRequest.builder()
                    .label("Controle continu")
                    .dateExam(Instant.now())
                    .coefficient(0.4)
                    .academicYear(entryYear)
                    .build(),
                adminToken,
                Exam.class)
            .getBody();
    Exam finalExam =
        post(
                "/courses/" + course.getId() + "/exams",
                ExamCreateRequest.builder()
                    .label("Examen final")
                    .dateExam(Instant.now())
                    .coefficient(0.6)
                    .academicYear(entryYear)
                    .build(),
                adminToken,
                Exam.class)
            .getBody();
    assertThat(continuousAssessment).isNotNull();
    assertThat(finalExam).isNotNull();

    put(
        "/exams/" + continuousAssessment.getId() + "/grades/" + student.getId(),
        GradeUpsertRequest.builder().value(14.0).reason("Initial entry").build(),
        adminToken,
        Grade.class);
    put(
        "/exams/" + finalExam.getId() + "/grades/" + student.getId(),
        GradeUpsertRequest.builder().value(14.0).reason("Initial entry").build(),
        adminToken,
        Grade.class);

    TeacherGradeBoard initialBoard =
        get("/teachers/" + teacher.getId() + "/grades", adminToken, TeacherGradeBoard.class)
            .getBody();
    assertThat(initialBoard).isNotNull();
    TeacherCourseBoard initialCourse = initialBoard.getCourses().get(0);
    assertThat(initialCourse.getStudentAverages().get(student.getId())).isEqualTo(14.0);

    put(
        "/exams/" + finalExam.getId() + "/grades/" + student.getId(),
        GradeUpsertRequest.builder().value(15.0).reason("Correction").build(),
        adminToken,
        Grade.class);

    TeacherGradeBoard updatedBoard =
        get("/teachers/" + teacher.getId() + "/grades", adminToken, TeacherGradeBoard.class)
            .getBody();
    assertThat(updatedBoard).isNotNull();
    TeacherCourseBoard updatedCourse = updatedBoard.getCourses().get(0);
    assertThat(updatedCourse.getStudentAverages().get(student.getId())).isEqualTo(14.6);
  }

  @Test
  void protectedEndpoint_rejectsRequestWithoutToken() {
    ResponseEntity<String> response =
        restTemplate.exchange(url("/promotions"), HttpMethod.GET, HttpEntity.EMPTY, String.class);

    assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }

  private HttpHeaders headers(String token) {
    HttpHeaders headers = new HttpHeaders();
    if (token != null) {
      headers.setBearerAuth(token);
    }
    return headers;
  }

  private <T> ResponseEntity<T> post(
      String path, Object body, String token, Class<T> responseType) {
    return restTemplate.exchange(
        url(path), HttpMethod.POST, new HttpEntity<>(body, headers(token)), responseType);
  }

  private <T> ResponseEntity<T> put(String path, Object body, String token, Class<T> responseType) {
    return restTemplate.exchange(
        url(path), HttpMethod.PUT, new HttpEntity<>(body, headers(token)), responseType);
  }

  private <T> ResponseEntity<T> get(String path, String token, Class<T> responseType) {
    return restTemplate.exchange(
        url(path), HttpMethod.GET, new HttpEntity<>(headers(token)), responseType);
  }

  private <T> ResponseEntity<T> getForList(
      String path, String token, ParameterizedTypeReference<T> responseType) {
    return restTemplate.exchange(
        url(path), HttpMethod.GET, new HttpEntity<>(headers(token)), responseType);
  }

  private <T> ResponseEntity<T> postForList(
      String path, String token, ParameterizedTypeReference<T> responseType) {
    return restTemplate.exchange(
        url(path), HttpMethod.POST, new HttpEntity<>(headers(token)), responseType);
  }
}
