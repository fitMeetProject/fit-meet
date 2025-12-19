package com.ssafy.fitmeet.global.config.aws;

public class AwsS3Dto {

    public record PresignUploadResponse(
            String key,
            String uploadUrl,
            String publicUrl
    ) {}


}
