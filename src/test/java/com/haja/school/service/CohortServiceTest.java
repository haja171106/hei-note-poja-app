package com.haja.school.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.haja.school.endpoint.rest.model.CreateCohortRequest;
import com.haja.school.model.Cohort;
import com.haja.school.repository.JCohortRepository;
import com.haja.school.repository.model.JCohort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CohortServiceTest {

  @Mock private JCohortRepository cohortRepository;

  @InjectMocks private CohortService cohortService;

  private UUID cohortId;
  private JCohort cohort;

  @BeforeEach
  void setUp() {
    cohortId = UUID.randomUUID();
    cohort = JCohort.builder().id(cohortId).ref("A").entryYear(2024).build();
  }

  @Test
  void getAllCohorts_mapsStudentAndGroupCounts() {
    when(cohortRepository.findAll()).thenReturn(List.of(cohort));
    when(cohortRepository.countStudentsByCohortId(cohortId)).thenReturn(42L);
    when(cohortRepository.countGroupsByCohortId(cohortId)).thenReturn(2L);

    List<Cohort> result = cohortService.getAllCohorts();

    assertEquals(1, result.size());
    assertEquals("A", result.get(0).getRef());
    assertEquals(42L, result.get(0).getStudentCount());
    assertEquals(2L, result.get(0).getGroupCount());
  }

  @Test
  void createCohort_success() {
    CreateCohortRequest request = CreateCohortRequest.builder().ref("b").entryYear(2026).build();
    when(cohortRepository.existsByRef("B")).thenReturn(false);
    when(cohortRepository.save(any(JCohort.class)))
        .thenAnswer(
            invocation -> {
              JCohort toSave = invocation.getArgument(0);
              toSave.setId(UUID.randomUUID());
              return toSave;
            });
    when(cohortRepository.countStudentsByCohortId(any())).thenReturn(0L);
    when(cohortRepository.countGroupsByCohortId(any())).thenReturn(0L);

    Cohort result = cohortService.createCohort(request);

    assertEquals("B", result.getRef());
    assertEquals(2026, result.getEntryYear());
    verify(cohortRepository).save(any(JCohort.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"L", "M", "D", "l", "m", "d"})
  void createCohort_rejectsReservedLetters(String ref) {
    CreateCohortRequest request = CreateCohortRequest.builder().ref(ref).entryYear(2026).build();

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> cohortService.createCohort(request));
    assertEquals(400, ex.getStatusCode().value());
    verify(cohortRepository, never()).save(any());
  }

  @Test
  void createCohort_rejectsMultiCharacterRef() {
    CreateCohortRequest request = CreateCohortRequest.builder().ref("AB").entryYear(2026).build();

    assertThrows(ResponseStatusException.class, () -> cohortService.createCohort(request));
    verify(cohortRepository, never()).save(any());
  }

  @Test
  void createCohort_rejectsNonLetterRef() {
    CreateCohortRequest request = CreateCohortRequest.builder().ref("1").entryYear(2026).build();

    assertThrows(ResponseStatusException.class, () -> cohortService.createCohort(request));
    verify(cohortRepository, never()).save(any());
  }

  @Test
  void createCohort_rejectsDuplicateRef() {
    CreateCohortRequest request = CreateCohortRequest.builder().ref("C").entryYear(2026).build();
    when(cohortRepository.existsByRef("C")).thenReturn(true);

    ResponseStatusException ex =
        assertThrows(ResponseStatusException.class, () -> cohortService.createCohort(request));
    assertEquals(400, ex.getStatusCode().value());
    verify(cohortRepository, never()).save(any());
  }
}
