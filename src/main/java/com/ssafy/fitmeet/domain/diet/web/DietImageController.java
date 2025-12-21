package com.ssafy.fitmeet.domain.diet.web;

import com.ssafy.fitmeet.domain.diet.dto.DietImageDto;
import com.ssafy.fitmeet.domain.diet.service.ai.DietImageAiService;
import com.ssafy.fitmeet.global.config.aws.AwsS3Dto;
import com.ssafy.fitmeet.global.config.aws.S3PresignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/diet/images")
@Tag(name = "DietImageAI", description = "식단 이미지 분석 AI")
public class DietImageController {

    private final S3PresignService s3PresignService;
    private final DietImageAiService dietImageAiService;

    @GetMapping("/presign")
    @Operation(summary = "S3 서버 URL 요청",
            description = "S3서버에 사진 올릴 URL을 요청 ( UploadUrl, PublicUrl ) -> uploadUrl PUT 요청으로 서버에 실제로 사진을 저장 / publicUrl로 분석 요청")
    public AwsS3Dto.PresignUploadResponse presign(@RequestParam String filename) {
        return s3PresignService.createPresignedUrl(filename);
    }

    @PostMapping("/analyze")
    @Operation(summary = "GPT Vision 분석", description = "/presign 으로 받아온 publicUrl로 분석 요청 후 우리 서비스 DB 혹은 공공데이터와 결과 매핑")
    public DietImageDto.FoodAnalyzeMappedResponse analyze(@RequestParam String publicUrl) {
        return dietImageAiService.analyzeAndMap(publicUrl.trim());
    }
}
