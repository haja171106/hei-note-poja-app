package com.haja.school.endpoint.rest.controller;

import com.haja.school.endpoint.rest.model.CreateCohortRequest;
import com.haja.school.model.Cohort;
import com.haja.school.service.CohortService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/promotions")
@AllArgsConstructor
public class CohortController {

  private final CohortService cohortService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<Cohort>> getAllPromotions() {
    return ResponseEntity.ok(cohortService.getAllCohorts());
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Cohort> createPromotion(@Valid @RequestBody CreateCohortRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(cohortService.createCohort(request));
  }
}
