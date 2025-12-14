package com.ssafy.fitmeet.domain.training.web;

import com.ssafy.fitmeet.domain.training.service.TrainingYoutubeSyncService;
import com.ssafy.fitmeet.global.response.Response;
import com.ssafy.fitmeet.global.response.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/training/youtube")
@RequiredArgsConstructor
@Tag(name = "TrainingYouTubeAdmin", description = "운동영상 YouTube 동기화 관리 API")
public class TrainingYoutubeAdminRestController {

    private final TrainingYoutubeSyncService syncService;

    @PostMapping("/sync")
    @Operation(summary = "YouTube 전체 소스 동기화", description = "활성화된 모든 YouTube 소스를 기준으로 training_video 테이블을 동기화합니다.")
    public ResponseEntity<Response<?>> syncAll() {
        syncService.syncAllSources();
        return ResponseEntity.ok(ResponseUtil.ok("YouTube Sync 완료"));
    }
}
