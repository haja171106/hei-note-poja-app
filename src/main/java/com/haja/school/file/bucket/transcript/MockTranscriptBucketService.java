package com.haja.school.file.bucket.transcript;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Profile("local")
public class MockTranscriptBucketService implements TranscriptBucketService {

  private final Path mockS3BaseDir;

  public MockTranscriptBucketService(@Value("${mock.s3.base-dir:tmp/s3-mock}") String baseDir) {
    this.mockS3BaseDir = Path.of(baseDir);
    try {
      Files.createDirectories(mockS3BaseDir);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create mock S3 directory: " + mockS3BaseDir, e);
    }
  }

  @Override
  public String upload(byte[] pdfBytes, String bucketKey) {
    try {
      Path filePath = mockS3BaseDir.resolve(bucketKey);
      Files.createDirectories(filePath.getParent());
      Files.write(filePath, pdfBytes);
      log.info("Mock S3 upload: {}", filePath);
      return bucketKey;
    } catch (IOException e) {
      throw new RuntimeException("Failed to write mock S3 file: " + bucketKey, e);
    }
  }

  @Override
  public String generatePresignedUrl(String bucketKey) {
    return "http://localhost:8080/mock-s3/" + bucketKey;
  }
}
