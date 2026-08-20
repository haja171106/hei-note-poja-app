package com.haja.school.file.bucket;

import static org.assertj.core.api.Assertions.assertThat;

import com.haja.school.file.bucket.transcript.MockTranscriptBucketService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MockTranscriptBucketServiceTest {

  @Test
  void uploadsBytesAndGeneratesPresignedUrl() throws Exception {
    Path baseDir = Files.createTempDirectory("mock-s3-");
    try {
      MockTranscriptBucketService service = new MockTranscriptBucketService(baseDir.toString());
      byte[] content = {1, 2, 3};

      assertThat(service.upload(content, "transcripts/student.pdf"))
          .isEqualTo("transcripts/student.pdf");
      assertThat(Files.readAllBytes(baseDir.resolve("transcripts/student.pdf")))
          .containsExactly(content);
      assertThat(service.generatePresignedUrl("transcripts/student.pdf"))
          .isEqualTo("http://localhost:8080/mock-s3/transcripts/student.pdf");
    } finally {
      Files.walk(baseDir)
          .sorted(java.util.Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
              });
    }
  }
}
