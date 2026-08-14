package com.haja.school.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.haja.school.endpoint.rest.model.GroupChangeRequest;
import com.haja.school.endpoint.rest.model.StudentCreateRequest;
import com.haja.school.model.CursusStatus;
import com.haja.school.model.Role;
import com.haja.school.model.Student;
import com.haja.school.repository.JCohortRepository;
import com.haja.school.repository.JGroupRepository;
import com.haja.school.repository.JStudentGroupHistoryRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCohort;
import com.haja.school.repository.model.JGroup;
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
    when(userRepository.findByRefStartingWith("STD")).thenReturn(Collections.emptyList());
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
    assertEquals("STD00001", createdStudent.getRef());
    assertEquals("Doe", createdStudent.getName());
    assertEquals("John", createdStudent.getFirstname());
    assertEquals("hei.john@student.com", createdStudent.getEmail());
    assertEquals(cohortId, createdStudent.getCohortId());
    assertEquals(CursusStatus.ACTIVE, createdStudent.getStatus());

    verify(userRepository).save(any(JUser.class));
  }

  @Test
  void createStudent_incrementsExistingStudentRef() {
    StudentCreateRequest request =
        StudentCreateRequest.builder().name("Smith").firstname("Jane").cohortId(cohortId).build();

    JUser existingStudent = JUser.builder().ref("STD00005").role(Role.STUDENT).build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.findByRefStartingWith("STD")).thenReturn(List.of(existingStudent));
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

    assertEquals("STD00006", createdStudent.getRef());
    assertEquals("hei.jane@student.com", createdStudent.getEmail());
  }

  @Test
  void createStudent_handlesEmailCollision() {
    StudentCreateRequest request =
        StudentCreateRequest.builder().name("Dupont").firstname("Jean").cohortId(cohortId).build();

    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.findByRefStartingWith("STD")).thenReturn(Collections.emptyList());
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
    when(userRepository.findByRefStartingWith("STD")).thenReturn(Collections.emptyList());
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
}
