package com.haja.school.endpoint.rest.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mock-s3")
@Profile("local")
public class MockS3Controller {

  private final Path mockS3BaseDir;

  public MockS3Controller(@Value("${mock.s3.base-dir:tmp/s3-mock}") String baseDir) {
    this.mockS3BaseDir = Path.of(baseDir);
  }

  @GetMapping("/{path:**}")
  public ResponseEntity<byte[]> download(@PathVariable String path) throws IOException {
    Path filePath = mockS3BaseDir.resolve(path);
    if (!Files.exists(filePath)) {
      return ResponseEntity.notFound().build();
    }
    byte[] content = Files.readAllBytes(filePath);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path + "\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(content);
  }
}
