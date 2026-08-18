package com.haja.school.file.bucket.transcript;

import com.haja.school.PojaGenerated;
import com.haja.school.file.bucket.BucketConf;
import java.net.URL;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@PojaGenerated
@Service
@AllArgsConstructor
@Slf4j
@Profile("!local")
public class AwsTranscriptBucketService implements TranscriptBucketService {

  private final BucketConf bucketConf;

  @Override
  public String upload(byte[] pdfBytes, String bucketKey) {
    bucketConf
        .getS3Client()
        .putObject(
            PutObjectRequest.builder().bucket(bucketConf.getBucketName()).key(bucketKey).build(),
            RequestBody.fromBytes(pdfBytes));
    log.info("Uploaded transcript to S3: s3://{}/{}", bucketConf.getBucketName(), bucketKey);
    return bucketKey;
  }

  @Override
  public String generatePresignedUrl(String bucketKey) {
    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofHours(48))
            .getObjectRequest(req -> req.bucket(bucketConf.getBucketName()).key(bucketKey).build())
            .build();
    URL url = bucketConf.getS3Presigner().presignGetObject(presignRequest).url();
    return url.toString();
  }
}
