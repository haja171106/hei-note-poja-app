package com.haja.school.endpoint.rest.controller;

import com.haja.school.model.Group;
import com.haja.school.service.GroupService;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/promotions/{id}/groups")
@AllArgsConstructor
public class GroupController {

  private final GroupService groupService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<Group>> getGroups(@PathVariable UUID id) {
    return ResponseEntity.ok(groupService.getGroupsByCohort(id));
  }

  @PostMapping("/generate")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<Group>> generateGroups(@PathVariable UUID id) {
    return ResponseEntity.ok(groupService.generateGroups(id));
  }
}
