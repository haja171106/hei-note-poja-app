package com.haja.school.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.haja.school.model.Group;
import com.haja.school.repository.JCohortRepository;
import com.haja.school.repository.JGroupRepository;
import com.haja.school.repository.JStudentGroupHistoryRepository;
import com.haja.school.repository.JUserRepository;
import com.haja.school.repository.model.JCohort;
import com.haja.school.repository.model.JGroup;
import com.haja.school.repository.model.JUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

  @Mock private JCohortRepository cohortRepository;
  @Mock private JGroupRepository groupRepository;
  @Mock private JUserRepository userRepository;
  @Mock private JStudentGroupHistoryRepository studentGroupHistoryRepository;

  @InjectMocks private GroupService groupService;

  private UUID cohortId;
  private JCohort cohort;

  @BeforeEach
  void setUp() {
    cohortId = UUID.randomUUID();
    cohort = JCohort.builder().id(cohortId).ref("A").entryYear(2024).build();
  }

  private List<JUser> students(int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> JUser.builder().id(UUID.randomUUID()).build())
        .collect(Collectors.toList());
  }

  @Test
  void getGroupsByCohort_notFound() {
    when(cohortRepository.findById(cohortId)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> groupService.getGroupsByCohort(cohortId));
  }

  @Test
  void getGroupsByCohort_returnsGroups() {
    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    JGroup group = JGroup.builder().id(UUID.randomUUID()).ref("A1").cohort(cohort).build();
    when(groupRepository.findByCohortId(cohortId)).thenReturn(List.of(group));
    when(groupRepository.countActiveStudentsByGroupId(group.getId())).thenReturn(10L);

    List<Group> result = groupService.getGroupsByCohort(cohortId);

    assertEquals(1, result.size());
    assertEquals("A1", result.get(0).getRef());
    assertEquals(10L, result.get(0).getStudentCount());
  }

  @Test
  void generateGroups_cohortNotFound() {
    when(cohortRepository.findById(cohortId)).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class, () -> groupService.generateGroups(cohortId));
  }

  @Test
  void generateGroups_noStudents() {
    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.findByCohortId(cohortId)).thenReturn(List.of());

    assertThrows(ResponseStatusException.class, () -> groupService.generateGroups(cohortId));
  }

  @Test
  void generateGroups_singleGroupWhenAtOrBelowThreshold() {
    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.findByCohortId(cohortId)).thenReturn(new ArrayList<>(students(50)));
    when(groupRepository.save(any(JGroup.class)))
        .thenAnswer(
            invocation -> {
              JGroup g = invocation.getArgument(0);
              g.setId(UUID.randomUUID());
              return g;
            });
    when(groupRepository.countActiveStudentsByGroupId(any())).thenReturn(50L);

    List<Group> result = groupService.generateGroups(cohortId);

    assertEquals(1, result.size());
    assertEquals("A1", result.get(0).getRef());
    verify(studentGroupHistoryRepository, times(50)).save(any());
  }

  @Test
  void generateGroups_splitsIntoMultipleGroupsAbove50() {
    when(cohortRepository.findById(cohortId)).thenReturn(Optional.of(cohort));
    when(userRepository.findByCohortId(cohortId)).thenReturn(new ArrayList<>(students(90)));
    when(groupRepository.save(any(JGroup.class)))
        .thenAnswer(
            invocation -> {
              JGroup g = invocation.getArgument(0);
              g.setId(UUID.randomUUID());
              return g;
            });
    when(groupRepository.countActiveStudentsByGroupId(any())).thenReturn(45L);

    List<Group> result = groupService.generateGroups(cohortId);

    assertEquals(3, result.size());
    assertEquals("A1", result.get(0).getRef());
    assertEquals("A2", result.get(1).getRef());
    assertEquals("A3", result.get(2).getRef());
    verify(studentGroupHistoryRepository, times(90)).save(any());
  }
}
