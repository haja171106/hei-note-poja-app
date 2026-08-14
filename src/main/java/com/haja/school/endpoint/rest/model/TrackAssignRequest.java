package com.haja.school.endpoint.rest.model;

import com.haja.school.model.Track;
import jakarta.validation.constraints.NotNull;
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
public class TrackAssignRequest {

  @NotNull(message = "Track is required")
  private Track track;
}
