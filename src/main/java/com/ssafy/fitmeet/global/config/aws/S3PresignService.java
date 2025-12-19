package com.ssafy.fitmeet.global.config.aws;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3PresignService {

    private final AmazonS3 amazonS3;

    @Value("${app.aws.s3.bucket}")
    private String bucket;

    @Value("${app.aws.cloudfront.domain}")
    private String cloudFrontDomain;

    public AwsS3Dto.PresignUploadResponse createPresignedUrl(String originalFilename) {

        String safeName = originalFilename.replaceAll("\\s+", "_"); // 공백 제거(권장)

        String key = "diet-images/" + LocalDate.now() + "/" + UUID.randomUUID() + "_" + safeName;

        Date expiration = new Date(System.currentTimeMillis() + 1000L * 60 * 5);

        URL uploadUrl = amazonS3.generatePresignedUrl(
                new GeneratePresignedUrlRequest(bucket, key)
                        .withMethod(HttpMethod.PUT)
                        .withExpiration(expiration)
        );

        String publicUrl = "https://" + cloudFrontDomain + "/" + key;

        return new AwsS3Dto.PresignUploadResponse(
                key,
                uploadUrl.toString(),
                publicUrl
        );
    }
}


