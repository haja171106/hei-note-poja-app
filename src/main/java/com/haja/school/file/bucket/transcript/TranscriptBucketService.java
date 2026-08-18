package com.haja.school.file.bucket.transcript;

public interface TranscriptBucketService {

  String upload(byte[] pdfBytes, String bucketKey);

  String generatePresignedUrl(String bucketKey);
}
