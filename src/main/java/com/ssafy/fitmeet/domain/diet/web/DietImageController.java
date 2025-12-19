package com.ssafy.fitmeet.domain.diet.web;

import com.ssafy.fitmeet.domain.diet.service.ai.DietImageAiService;
import com.ssafy.fitmeet.global.config.ai.OpenAiDto;
import com.ssafy.fitmeet.global.config.aws.AwsS3Dto;
import com.ssafy.fitmeet.global.config.aws.S3PresignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/diet/images")
public class DietImageController {

    private final S3PresignService s3PresignService;
    private final DietImageAiService dietImageAiService;

    @GetMapping("/presign")
    public AwsS3Dto.PresignUploadResponse presign(@RequestParam String filename) {
        return s3PresignService.createPresignedUrl(filename);
    }

    @PostMapping("/analyze")
    public OpenAiDto.FoodAnalyzeResponse analyze(@RequestParam String publicUrl) {
        return dietImageAiService.analyze(publicUrl.trim());
    }
}
