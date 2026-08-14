package com.haja.school.service;

import com.haja.school.endpoint.rest.model.GroupChangeRequest;
import com.haja.school.endpoint.rest.model.StudentCreateRequest;
import com.haja.school.endpoint.rest.model.TrackAssignRequest;
import com.haja.school.model.CursusStatus;
import com.haja.school.model.Role;
import com.haja.school.model.Student;
import com.haja.school.repository.JCohortRepository;
import com.haja.school.repository.JGroupRepository;
import com.haja.school.repository.JStudentGroupHistoryRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCohort;
import com.haja.school.repository.model.JGroup;
import com.haja.school.repository.model.JStudentGroupHistory;
import com.haja.school.repository.model.JUser;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class StudentService {

  private final JUserRepository userRepository;
  private final JCohortRepository cohortRepository;
  private final JGroupRepository groupRepository;
  private final JStudentGroupHistoryRepository studentGroupHistoryRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public Student createStudent(StudentCreateRequest request) {
    JCohort cohort =
        cohortRepository
            .findById(request.getCohortId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cohort not found"));

    String ref = generateStudentRef();
    String email = generateInstitutionalEmail(request.getFirstname());
    String rawPassword = UUID.randomUUID().toString().substring(0, 8);
    String encodedPassword = passwordEncoder.encode(rawPassword);

    JUser jUser =
        JUser.builder()
            .ref(ref)
            .name(request.getName())
            .firstname(request.getFirstname())
            .email(email)
            .password(encodedPassword)
            .role(Role.STUDENT)
            .cursusStatus(CursusStatus.ACTIVE)
            .cohort(cohort)
            .birthdate(request.getBirthdate())
            .address(request.getAddress())
            .build();

    JUser savedUser = userRepository.save(jUser);

    JGroup assignedGroup = null;
    if (request.getGroupId() != null) {
      assignedGroup =
          groupRepository
              .findById(request.getGroupId())
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    } else {
      List<JGroup> cohortGroups = groupRepository.findByCohortId(cohort.getId());
      if (!cohortGroups.isEmpty()) {
        assignedGroup =
            cohortGroups.stream()
                .min(
                    Comparator.comparingLong(
                        g -> groupRepository.countActiveStudentsByGroupId(g.getId())))
                .orElse(cohortGroups.get(0));
      }
    }

    if (assignedGroup != null) {
      studentGroupHistoryRepository.save(
          JStudentGroupHistory.builder()
              .student(savedUser)
              .group(assignedGroup)
              .startDate(LocalDate.now())
              .build());
    }

    return Student.builder()
        .id(savedUser.getId())
        .ref(savedUser.getRef())
        .name(savedUser.getName())
        .firstname(savedUser.getFirstname())
        .email(savedUser.getEmail())
        .cohortId(cohort.getId())
        .groupId(assignedGroup != null ? assignedGroup.getId() : null)
        .track(savedUser.getTrack())
        .status(savedUser.getCursusStatus())
        .build();
  }

  @Transactional
  public void deleteStudent(UUID studentId) {
    JUser student =
        userRepository
            .findById(studentId)
            .filter(u -> u.getRole() == Role.STUDENT)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

    student.setCursusStatus(CursusStatus.DROPPED_OUT);
    userRepository.save(student);

    studentGroupHistoryRepository
        .findByStudentIdAndEndDateIsNull(studentId)
        .ifPresent(
            history -> {
              history.setEndDate(LocalDate.now());
              studentGroupHistoryRepository.save(history);
            });
  }

  @Transactional
  public Student changeStudentGroup(UUID studentId, GroupChangeRequest request) {
    JUser student =
        userRepository
            .findById(studentId)
            .filter(u -> u.getRole() == Role.STUDENT)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

    JGroup newGroup =
        groupRepository
            .findById(request.getNewGroupId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

    studentGroupHistoryRepository
        .findByStudentIdAndEndDateIsNull(studentId)
        .ifPresent(
            history -> {
              history.setEndDate(request.getEffectiveDate());
              studentGroupHistoryRepository.save(history);
            });

    studentGroupHistoryRepository.save(
        JStudentGroupHistory.builder()
            .student(student)
            .group(newGroup)
            .startDate(request.getEffectiveDate())
            .build());

    return Student.builder()
        .id(student.getId())
        .ref(student.getRef())
        .name(student.getName())
        .firstname(student.getFirstname())
        .email(student.getEmail())
        .cohortId(student.getCohort() != null ? student.getCohort().getId() : null)
        .groupId(newGroup.getId())
        .track(student.getTrack())
        .status(student.getCursusStatus())
        .build();
  }

  @Transactional
  public Student assignStudentTrack(UUID studentId, TrackAssignRequest request) {
    JUser student =
        userRepository
            .findById(studentId)
            .filter(u -> u.getRole() == Role.STUDENT)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

    if (student.getCohort() == null || student.getCohort().getEntryYear() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Student is not assigned to a valid cohort with entry year");
    }

    int currentSemester = computeCurrentSemester(student.getCohort().getEntryYear());
    if (currentSemester < 4) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Track assignment is only allowed from the 4th semester onwards");
    }

    student.setTrack(request.getTrack());
    JUser savedStudent = userRepository.save(student);

    UUID activeGroupId =
        studentGroupHistoryRepository
            .findByStudentIdAndEndDateIsNull(studentId)
            .map(history -> history.getGroup().getId())
            .orElse(null);

    return Student.builder()
        .id(savedStudent.getId())
        .ref(savedStudent.getRef())
        .name(savedStudent.getName())
        .firstname(savedStudent.getFirstname())
        .email(savedStudent.getEmail())
        .cohortId(savedStudent.getCohort() != null ? savedStudent.getCohort().getId() : null)
        .groupId(activeGroupId)
        .track(savedStudent.getTrack())
        .status(savedStudent.getCursusStatus())
        .build();
  }

  private int computeCurrentSemester(int entryYear) {
    int currentYear = LocalDate.now().getYear();
    int currentMonth = LocalDate.now().getMonthValue();
    int yearsPassed = currentYear - entryYear;
    return yearsPassed * 2 + (currentMonth >= 9 ? 1 : 0);
  }

  private String generateStudentRef() {
    List<JUser> students = userRepository.findByRefStartingWith("STD");
    int maxNumber = 0;
    for (JUser user : students) {
      String ref = user.getRef();
      if (ref != null && ref.startsWith("STD")) {
        try {
          int num = Integer.parseInt(ref.substring(3));
          if (num > maxNumber) {
            maxNumber = num;
          }
        } catch (NumberFormatException ignored) {
        }
      }
    }
    return String.format("STD%05d", maxNumber + 1);
  }

  private String generateInstitutionalEmail(String firstname) {
    String normalizedName = firstname.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    if (normalizedName.isEmpty()) {
      normalizedName = "student";
    }
    String baseEmail = "hei." + normalizedName + "@student.com";
    if (!userRepository.existsByEmail(baseEmail)) {
      return baseEmail;
    }

    int counter = 1;
    while (userRepository.existsByEmail("hei." + normalizedName + counter + "@student.com")) {
      counter++;
    }
    return "hei." + normalizedName + counter + "@student.com";
  }
}
