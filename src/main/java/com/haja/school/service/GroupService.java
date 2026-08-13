package com.haja.school.service;

import com.haja.school.model.Group;
import com.haja.school.repository.JCohortRepository;
import com.haja.school.repository.JGroupRepository;
import com.haja.school.repository.JStudentGroupHistoryRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCohort;
import com.haja.school.repository.model.JGroup;
import com.haja.school.repository.model.JStudentGroupHistory;
import com.haja.school.repository.model.JUser;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class GroupService {

  private static final int MAX_GROUP_SIZE = 40;
  private static final int SINGLE_GROUP_THRESHOLD = 50;

  private final JCohortRepository cohortRepository;
  private final JGroupRepository groupRepository;
  private final JUserRepository userRepository;
  private final JStudentGroupHistoryRepository studentGroupHistoryRepository;

  public List<Group> getGroupsByCohort(UUID cohortId) {
    cohortRepository
        .findById(cohortId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cohort not found"));

    return groupRepository.findByCohortId(cohortId).stream().map(this::toDomain).toList();
  }

  @Transactional
  public List<Group> generateGroups(UUID cohortId) {
    JCohort cohort =
        cohortRepository
            .findById(cohortId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cohort not found"));

    List<JUser> students = userRepository.findByCohortId(cohortId);
    int total = students.size();

    if (total == 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No students found in this cohort");
    }

    int groupCount = computeGroupCount(total);

    List<JGroup> groups = new ArrayList<>();
    for (int i = 1; i <= groupCount; i++) {
      JGroup group =
          groupRepository.save(
              JGroup.builder()
                  .ref(cohort.getRef() + i)
                  .cohort(cohort)
                  .academicYear(cohort.getEntryYear())
                  .build());
      groups.add(group);
    }

    Collections.shuffle(students);
    LocalDate today = LocalDate.now();

    for (int i = 0; i < total; i++) {
      JGroup assignedGroup = groups.get(i % groupCount);
      studentGroupHistoryRepository.save(
          JStudentGroupHistory.builder()
              .student(students.get(i))
              .group(assignedGroup)
              .startDate(today)
              .build());
    }

    return groups.stream().map(this::toDomain).toList();
  }

  private int computeGroupCount(int total) {
    if (total <= SINGLE_GROUP_THRESHOLD) {
      return 1;
    }
    return (int) Math.ceil((double) total / MAX_GROUP_SIZE);
  }

  private Group toDomain(JGroup jGroup) {
    return Group.builder()
        .id(jGroup.getId())
        .ref(jGroup.getRef())
        .cohortId(jGroup.getCohort().getId())
        .academicYear(jGroup.getAcademicYear())
        .studentCount(groupRepository.countActiveStudentsByGroupId(jGroup.getId()))
        .build();
  }
}
