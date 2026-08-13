package com.haja.school.service;

import com.haja.school.endpoint.rest.model.CreateCohortRequest;
import com.haja.school.model.Cohort;
import com.haja.school.repository.JCohortRepository;
import com.haja.school.repository.model.JCohort;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class CohortService {

  private static final Set<String> RESERVED_LETTERS = Set.of("L", "M", "D");

  private final JCohortRepository cohortRepository;

  public List<Cohort> getAllCohorts() {
    return cohortRepository.findAll().stream().map(this::toDomain).toList();
  }

  public Cohort createCohort(CreateCohortRequest request) {
    String ref = request.getRef().trim().toUpperCase();

    if (ref.length() != 1 || !Character.isLetter(ref.charAt(0))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "ref must be a single letter (A-Z)");
    }

    if (RESERVED_LETTERS.contains(ref)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "ref cannot be L, M or D (reserved for LMD system)");
    }

    if (cohortRepository.existsByRef(ref)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A cohort with ref '" + ref + "' already exists");
    }

    JCohort saved =
        cohortRepository.save(JCohort.builder().ref(ref).entryYear(request.getEntryYear()).build());

    return toDomain(saved);
  }

  private Cohort toDomain(JCohort jCohort) {
    return Cohort.builder()
        .id(jCohort.getId())
        .ref(jCohort.getRef())
        .entryYear(jCohort.getEntryYear())
        .studentCount(cohortRepository.countStudentsByCohortId(jCohort.getId()))
        .groupCount(cohortRepository.countGroupsByCohortId(jCohort.getId()))
        .build();
  }
}
