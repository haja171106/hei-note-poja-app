package com.haja.school.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.haja.school.file.zip.FileTyper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FileTyperTest {

  @Test
  void detectsPlainTextFile() throws Exception {
    Path file = Files.createTempFile("file-typer-", ".txt");
    try {
      Files.writeString(file, "hello");

      assertThat(new FileTyper().apply(file.toFile()).toString()).isEqualTo("text/plain");
    } finally {
      Files.deleteIfExists(file);
    }
  }
}
