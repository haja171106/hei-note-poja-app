package com.haja.school.service;

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
