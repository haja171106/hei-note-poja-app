package com.haja.school.endpoint.rest.model;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupChangeRequest {

  @NotNull(message = "New group ID is required")
  private UUID newGroupId;

  @NotNull(message = "Effective date is required")
  private LocalDate effectiveDate;
}
