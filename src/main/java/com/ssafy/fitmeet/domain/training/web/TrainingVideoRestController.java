package com.ssafy.fitmeet.domain.training.web;

import com.ssafy.fitmeet.domain.training.dto.TrainingVideoDto.*;
import com.ssafy.fitmeet.domain.training.service.TrainingVideoService;
import com.ssafy.fitmeet.global.response.Response;
import com.ssafy.fitmeet.global.response.ResponseUtil;
import com.ssafy.fitmeet.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/training/videos")
@RequiredArgsConstructor
@Tag(name = "TrainingVideo", description = "운동 영상 관련 API")
public class TrainingVideoRestController {

    private final TrainingVideoService trainingVideoService;

    @GetMapping("/recommended")
    @Operation(summary = "추천 운동영상 조회", description = "카테고리별/전체 추천 운동영상 조회")
    public ResponseEntity<Response<?>> recommended(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam(required = false) String category, @RequestParam(defaultValue = "10") int limit) {
        Long userId = userDetails.getId();

        List<VideoCardResponse> res = trainingVideoService.getRecommended(userId, category, limit);
        return ResponseEntity.ok(ResponseUtil.ok(res));
    }

    @GetMapping
    @Operation(summary = "운동영상 목록 조회", description = "카테고리/레벨/영상길이/정렬 조건으로 운동영상 목록 조회")
    public ResponseEntity<Response<?>> search(@AuthenticationPrincipal CustomUserDetails userDetails, VideoSearchRequest request) {
        Long userId = userDetails.getId();

        VideoPageResponse res = trainingVideoService.searchVideos(userId, request);
        return ResponseEntity.ok(ResponseUtil.ok(res));
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "운동영상 좋아요 토글", description = "운동영상 좋아요 추가/취소")
    public ResponseEntity<Response<?>> toggleLike(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("id") Long id) {
        Long userId = userDetails.getId();

        boolean liked = trainingVideoService.toggleLike(userId, id);
        return ResponseEntity.ok(ResponseUtil.ok(liked));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "운동 영상 완료 처리", description = "유저가 특정 운동 영상을 완료했을 때 기록")
    public ResponseEntity<Response<?>> completeVideo(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("id") Long videoId, @RequestBody(required = false) CompleteVideoRequest request) {
        Long userId = userDetails.getId();
        Integer durationSec = (request != null) ? request.durationSec() : null;
        String memo = (request != null) ? request.memo() : null;

        trainingVideoService.completeVideo(userId, videoId, durationSec, memo);
        return ResponseEntity.ok(ResponseUtil.ok(null));
    }

    @GetMapping("/history/today")
    @Operation(summary = "오늘 완료한 운동영상 목록", description = "현재 로그인 유저가 오늘 완료한 운동영상 목록 조회 (커뮤니티)")
    public ResponseEntity<Response<?>> getTodayCompleted(@AuthenticationPrincipal CustomUserDetails userDetails) {

        Long userId = userDetails.getId();
        List<TodayVideoResponse> res = trainingVideoService.getTodayCompletedVideos(userId);
        return ResponseEntity.ok(ResponseUtil.ok(res));
    }

    @GetMapping("/{id}")
    @Operation(summary = "운동영상 상세 조회", description = "영상 정보 + 좋아요 여부 + 오늘 완료 여부 + 댓글 목록")
    public ResponseEntity<Response<?>> getDetail(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("id") Long id) {
        Long userId = userDetails.getId();
        VideoDetailResponse res = trainingVideoService.getVideoDetail(userId, id);
        return ResponseEntity.ok(ResponseUtil.ok(res));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "운동영상 댓글 작성", description = "해당 운동영상에 댓글 작성")
    public ResponseEntity<Response<?>> addComment(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("id") Long id, @RequestBody CreateCommentRequest request) {
        Long userId = userDetails.getId();
        VideoCommentResponse res = trainingVideoService.addComment(userId, id, request.content());
        return ResponseEntity.ok(ResponseUtil.ok(res));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "운동영상 댓글 삭제", description = "본인이 작성한 댓글 삭제")
    public ResponseEntity<Response<?>> deleteComment(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable("commentId") Long commentId) {
        Long userId = userDetails.getId();
        trainingVideoService.deleteComment(userId, commentId);
        return ResponseEntity.ok(ResponseUtil.ok(null));
    }
}
