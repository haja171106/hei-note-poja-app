package com.haja.school.endpoint.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.haja.school.model.ReportStatus;
import java.time.Duration;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class TranscriptRequested extends PojaEvent {

  @JsonProperty("student_id")
  private UUID studentId;

  @JsonProperty("student_ref")
  private String studentRef;

  @JsonProperty("student_firstname")
  private String studentFirstname;

  @JsonProperty("student_name")
  private String studentName;

  @JsonProperty("student_email")
  private String studentEmail;

  @JsonProperty("recipient_email")
  private String recipientEmail;

  @JsonProperty("academic_year")
  private Integer academicYear;

  @JsonProperty("status")
  private ReportStatus status;

  @JsonProperty("request_id")
  private UUID requestId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(45);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
