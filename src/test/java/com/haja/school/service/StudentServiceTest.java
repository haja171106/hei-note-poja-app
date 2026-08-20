package com.haja.school.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.haja.school.endpoint.rest.model.GroupChangeRequest;
import com.haja.school.endpoint.rest.model.StudentCreateRequest;
import com.haja.school.endpoint.rest.model.TrackAssignRequest;
import com.haja.school.model.CursusStatus;
import com.haja.school.model.Role;
import com.haja.school.model.Student;
import com.haja.school.model.Track;
import com.haja.school.repository.JCohortRepository;
import com.haja.school.repository.JGroupRepository;
import com.haja.school.repository.JStudentGroupHistoryRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCohort;
import com.haja.school.repository.model.JGroup;
import com.haja.school.repository.model.JStudentGroupHistory;
import com.haja.school.repository.model.JUser;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

  @Mock private JUserRepository userRepository;
  @Mock private JCohortRepository cohortRepository;
  @Mock private JGroupRepository groupRepository;
  @Mock private JStudentGroupHistoryRepository studentGroupHistoryRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private StudentService studentService;

  private UUID cohortId;
  private JCohort cohort;

  @BeforeEach
  void setUp() {
    cohortId = UUID.randomUUID();
    cohort = JCohort.builder().id(cohortId).ref("A").entryYear(2024).build();
  }

  @Test
  void createStudent_success() {
    StudentCreateRequest request =
        StudentCreateRequest.builder()
            .name("Doe")
            .firstname("John")
            .address("123 Street")
            .birthdate(LocalDate.of(2000, 1, 1))
            .cohortId(cohortId)
            .build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.existsByRef(anyString())).thenReturn(false);
    when(userRepository.existsByEmail("hei.john@student.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

    UUID savedUserId = UUID.randomUUID();
    when(userRepository.save(any(JUser.class)))
        .thenAnswer(
            invocation -> {
              JUser user = invocation.getArgument(0);
              user.setId(savedUserId);
              return user;
            });

    Student createdStudent = studentService.createStudent(request);

    assertNotNull(createdStudent);
    assertEquals(savedUserId, createdStudent.getId());
    assertTrue(createdStudent.getRef().matches("STD24\\d{3}"));
    assertEquals("Doe", createdStudent.getName());
    assertEquals("John", createdStudent.getFirstname());
    assertEquals("hei.john@student.com", createdStudent.getEmail());
    assertEquals(cohortId, createdStudent.getCohortId());
    assertEquals(CursusStatus.ACTIVE, createdStudent.getStatus());

    verify(userRepository).save(any(JUser.class));
  }

  @Test
  void createStudent_generatesUniqueReferenceWhenCandidateExists() {
    StudentCreateRequest request =
        StudentCreateRequest.builder().name("Smith").firstname("Jane").cohortId(cohortId).build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.existsByRef(anyString())).thenReturn(false);
    when(userRepository.existsByEmail("hei.jane@student.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(userRepository.save(any(JUser.class)))
        .thenAnswer(
            invocation -> {
              JUser u = invocation.getArgument(0);
              u.setId(UUID.randomUUID());
              return u;
            });

    Student createdStudent = studentService.createStudent(request);

    assertTrue(createdStudent.getRef().matches("STD24\\d{3}"));
    assertEquals("hei.jane@student.com", createdStudent.getEmail());
  }

  @Test
  void createStudent_handlesEmailCollision() {
    StudentCreateRequest request =
        StudentCreateRequest.builder().name("Dupont").firstname("Jean").cohortId(cohortId).build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.existsByRef(anyString())).thenReturn(false);
    when(userRepository.existsByEmail("hei.jean@student.com")).thenReturn(true);
    when(userRepository.existsByEmail("hei.jean1@student.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(userRepository.save(any(JUser.class)))
        .thenAnswer(
            invocation -> {
              JUser u = invocation.getArgument(0);
              u.setId(UUID.randomUUID());
              return u;
            });

    Student createdStudent = studentService.createStudent(request);

    assertEquals("hei.jean1@student.com", createdStudent.getEmail());
  }

  @Test
  void createStudent_withExplicitGroup() {
    UUID groupId = UUID.randomUUID();
    JGroup group = JGroup.builder().id(groupId).cohort(cohort).ref("A1").build();

    StudentCreateRequest request =
        StudentCreateRequest.builder()
            .name("Martin")
            .firstname("Paul")
            .cohortId(cohortId)
            .groupId(groupId)
            .build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
    when(userRepository.existsByRef(anyString())).thenReturn(false);
    when(userRepository.existsByEmail("hei.paul@student.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(userRepository.save(any(JUser.class)))
        .thenAnswer(
            invocation -> {
              JUser u = invocation.getArgument(0);
              u.setId(UUID.randomUUID());
              return u;
            });

    Student createdStudent = studentService.createStudent(request);

    assertEquals(groupId, createdStudent.getGroupId());
    verify(studentGroupHistoryRepository).save(any());
  }

  @Test
  void createStudent_cohortNotFound_throwsException() {
    StudentCreateRequest request =
        StudentCreateRequest.builder().name("Unknown").firstname("User").cohortId(cohortId).build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> studentService.createStudent(request));
  }

  @Test
  void createStudent_noGroupsInCohort_noGroupAssigned() {
    StudentCreateRequest request =
        StudentCreateRequest.builder().name("Solo").firstname("Alone").cohortId(cohortId).build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.existsByRef(anyString())).thenReturn(false);
    when(userRepository.existsByEmail("hei.alone@student.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(groupRepository.findByCohortId(cohortId)).thenReturn(Collections.emptyList());
    when(userRepository.save(any(JUser.class)))
        .thenAnswer(
            invocation -> {
              JUser u = invocation.getArgument(0);
              u.setId(UUID.randomUUID());
              return u;
            });

    Student createdStudent = studentService.createStudent(request);

    assertNotNull(createdStudent);
    assertNull(createdStudent.getGroupId());
    verify(studentGroupHistoryRepository, never()).save(any());
  }

  @Test
  void createStudent_specialCharactersInName_generatesValidEmail() {
    StudentCreateRequest request =
        StudentCreateRequest.builder()
            .name("O'Connor")
            .firstname("Jean-Pierre")
            .cohortId(cohortId)
            .build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.existsByRef(anyString())).thenReturn(false);
    when(userRepository.existsByEmail("hei.jeanpierre@student.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(userRepository.save(any(JUser.class)))
        .thenAnswer(
            invocation -> {
              JUser u = invocation.getArgument(0);
              u.setId(UUID.randomUUID());
              return u;
            });

    Student createdStudent = studentService.createStudent(request);

    assertEquals("hei.jeanpierre@student.com", createdStudent.getEmail());
  }

  @Test
  void createStudent_emptyFirstname_usesDefaultEmail() {
    StudentCreateRequest request =
        StudentCreateRequest.builder().name("Ghost").firstname("").cohortId(cohortId).build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.existsByRef(anyString())).thenReturn(false);
    when(userRepository.existsByEmail("hei.student@student.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(userRepository.save(any(JUser.class)))
        .thenAnswer(
            invocation -> {
              JUser u = invocation.getArgument(0);
              u.setId(UUID.randomUUID());
              return u;
            });

    Student createdStudent = studentService.createStudent(request);

    assertEquals("hei.student@student.com", createdStudent.getEmail());
  }

  @Test
  void deleteStudent_success() {
    UUID studentId = UUID.randomUUID();
    JUser student =
        JUser.builder().id(studentId).role(Role.STUDENT).cursusStatus(CursusStatus.ACTIVE).build();

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

    studentService.deleteStudent(studentId);

    assertEquals(CursusStatus.DROPPED_OUT, student.getCursusStatus());
    verify(userRepository).save(student);
  }

  @Test
  void deleteStudent_withActiveGroupHistory_closesEndDate() {
    UUID studentId = UUID.randomUUID();
    UUID groupId = UUID.randomUUID();
    JUser student =
        JUser.builder().id(studentId).role(Role.STUDENT).cursusStatus(CursusStatus.ACTIVE).build();
    JGroup group = JGroup.builder().id(groupId).ref("A1").cohort(cohort).build();
    JStudentGroupHistory history =
        JStudentGroupHistory.builder()
            .id(UUID.randomUUID())
            .student(student)
            .group(group)
            .startDate(LocalDate.of(2025, 9, 1))
            .build();

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(studentGroupHistoryRepository.findByStudentIdAndEndDateIsNull(studentId))
        .thenReturn(Optional.of(history));

    studentService.deleteStudent(studentId);

    assertEquals(CursusStatus.DROPPED_OUT, student.getCursusStatus());
    verify(userRepository).save(student);
    assertNotNull(history.getEndDate());
    verify(studentGroupHistoryRepository).save(history);
  }

  @Test
  void deleteStudent_studentRoleOnly_throwsNotFound() {
    UUID teacherId = UUID.randomUUID();
    JUser teacher =
        JUser.builder().id(teacherId).role(Role.TEACHER).cursusStatus(CursusStatus.ACTIVE).build();

    when(userRepository.findById(teacherId)).thenReturn(Optional.of(teacher));

    assertThrows(ResponseStatusException.class, () -> studentService.deleteStudent(teacherId));
  }

  @Test
  void deleteStudent_notFound_throwsException() {
    UUID studentId = UUID.randomUUID();
    when(userRepository.findById(studentId)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> studentService.deleteStudent(studentId));
  }

  @Test
  void changeStudentGroup_success() {
    UUID studentId = UUID.randomUUID();
    UUID newGroupId = UUID.randomUUID();
    LocalDate effectiveDate = LocalDate.of(2026, 9, 1);

    JUser student =
        JUser.builder()
            .id(studentId)
            .role(Role.STUDENT)
            .cursusStatus(CursusStatus.ACTIVE)
            .cohort(cohort)
            .build();

    JGroup newGroup = JGroup.builder().id(newGroupId).cohort(cohort).ref("A2").build();

    GroupChangeRequest request =
        GroupChangeRequest.builder().newGroupId(newGroupId).effectiveDate(effectiveDate).build();

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(groupRepository.findById(newGroupId)).thenReturn(Optional.of(newGroup));

    Student updatedStudent = studentService.changeStudentGroup(studentId, request);

    assertNotNull(updatedStudent);
    assertEquals(newGroupId, updatedStudent.getGroupId());
    verify(studentGroupHistoryRepository, times(1)).save(any());
  }

  @Test
  void changeStudentGroup_withExistingGroupHistory_closesOldAndCreatesNew() {
    UUID studentId = UUID.randomUUID();
    UUID oldGroupId = UUID.randomUUID();
    UUID newGroupId = UUID.randomUUID();
    LocalDate effectiveDate = LocalDate.of(2026, 9, 1);

    JUser student =
        JUser.builder()
            .id(studentId)
            .role(Role.STUDENT)
            .cursusStatus(CursusStatus.ACTIVE)
            .cohort(cohort)
            .build();

    JGroup oldGroup = JGroup.builder().id(oldGroupId).cohort(cohort).ref("A1").build();
    JGroup newGroup = JGroup.builder().id(newGroupId).cohort(cohort).ref("A2").build();

    JStudentGroupHistory existingHistory =
        JStudentGroupHistory.builder()
            .id(UUID.randomUUID())
            .student(student)
            .group(oldGroup)
            .startDate(LocalDate.of(2025, 9, 1))
            .build();

    GroupChangeRequest request =
        GroupChangeRequest.builder().newGroupId(newGroupId).effectiveDate(effectiveDate).build();

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(groupRepository.findById(newGroupId)).thenReturn(Optional.of(newGroup));
    when(studentGroupHistoryRepository.findByStudentIdAndEndDateIsNull(studentId))
        .thenReturn(Optional.of(existingHistory));

    Student updatedStudent = studentService.changeStudentGroup(studentId, request);

    assertNotNull(updatedStudent);
    assertEquals(newGroupId, updatedStudent.getGroupId());
    assertNotNull(existingHistory.getEndDate());
    assertEquals(effectiveDate, existingHistory.getEndDate());
    verify(studentGroupHistoryRepository).save(existingHistory);
    verify(studentGroupHistoryRepository, times(2)).save(any());
  }

  @Test
  void assignStudentTrack_success() {
    UUID studentId = UUID.randomUUID();
    JCohort cohort2024 = JCohort.builder().id(UUID.randomUUID()).entryYear(2024).build();

    JUser student =
        JUser.builder()
            .id(studentId)
            .role(Role.STUDENT)
            .cursusStatus(CursusStatus.ACTIVE)
            .cohort(cohort2024)
            .build();

    TrackAssignRequest request = TrackAssignRequest.builder().track(Track.EL).build();

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));
    when(userRepository.save(any(JUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Student updatedStudent = studentService.assignStudentTrack(studentId, request);

    assertNotNull(updatedStudent);
    assertEquals(Track.EL, updatedStudent.getTrack());
    verify(userRepository).save(student);
  }

  @Test
  void assignStudentTrack_semesterLessThan4_throwsException() {
    UUID studentId = UUID.randomUUID();
    JCohort cohort2026 = JCohort.builder().id(UUID.randomUUID()).entryYear(2026).build();

    JUser student =
        JUser.builder()
            .id(studentId)
            .role(Role.STUDENT)
            .cursusStatus(CursusStatus.ACTIVE)
            .cohort(cohort2026)
            .build();

    TrackAssignRequest request = TrackAssignRequest.builder().track(Track.TN).build();

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

    assertThrows(
        ResponseStatusException.class, () -> studentService.assignStudentTrack(studentId, request));
  }

  @Test
  void assignStudentTrack_noCohort_throwsBadRequest() {
    UUID studentId = UUID.randomUUID();

    JUser student =
        JUser.builder()
            .id(studentId)
            .role(Role.STUDENT)
            .cursusStatus(CursusStatus.ACTIVE)
            .cohort(null)
            .build();

    TrackAssignRequest request = TrackAssignRequest.builder().track(Track.EL).build();

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

    assertThrows(
        ResponseStatusException.class, () -> studentService.assignStudentTrack(studentId, request));
  }

  @Test
  void assignStudentTrack_noEntryYear_throwsBadRequest() {
    UUID studentId = UUID.randomUUID();
    JCohort cohortNoEntry = JCohort.builder().id(UUID.randomUUID()).entryYear(null).build();

    JUser student =
        JUser.builder()
            .id(studentId)
            .role(Role.STUDENT)
            .cursusStatus(CursusStatus.ACTIVE)
            .cohort(cohortNoEntry)
            .build();

    TrackAssignRequest request = TrackAssignRequest.builder().track(Track.TN).build();

    when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

    assertThrows(
        ResponseStatusException.class, () -> studentService.assignStudentTrack(studentId, request));
  }

  @Test
  void getStudentsByCohort_success() {
    UUID student1Id = UUID.randomUUID();
    UUID student2Id = UUID.randomUUID();

    JUser student1 =
        JUser.builder()
            .id(student1Id)
            .ref("STD00001")
            .name("Doe")
            .firstname("John")
            .email("hei.john@student.com")
            .cohort(cohort)
            .cursusStatus(CursusStatus.ACTIVE)
            .build();
    JUser student2 =
        JUser.builder()
            .id(student2Id)
            .ref("STD00002")
            .name("Smith")
            .firstname("Jane")
            .email("hei.jane@student.com")
            .cohort(cohort)
            .cursusStatus(CursusStatus.ACTIVE)
            .build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.findByCohortId(cohortId)).thenReturn(List.of(student1, student2));
    when(studentGroupHistoryRepository.findByStudentIdAndEndDateIsNull(any()))
        .thenReturn(Optional.empty());

    List<Student> students = studentService.getStudentsByCohort(cohortId);

    assertNotNull(students);
    assertEquals(2, students.size());
    assertEquals("STD00001", students.get(0).getRef());
    assertEquals("STD00002", students.get(1).getRef());
  }

  @Test
  void getStudentsByCohort_cohortNotFound_throwsException() {
    UUID nonexistentCohortId = UUID.randomUUID();
    when(cohortRepository.findById(nonexistentCohortId)).thenReturn(Optional.empty());

    assertThrows(
        ResponseStatusException.class,
        () -> studentService.getStudentsByCohort(nonexistentCohortId));
  }
}
