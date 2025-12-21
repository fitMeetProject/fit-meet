package com.ssafy.fitmeet.domain.community.web;

import com.ssafy.fitmeet.domain.community.dto.CommunityDto.*;
import com.ssafy.fitmeet.domain.community.service.CommunityService;
import com.ssafy.fitmeet.global.response.Response;
import com.ssafy.fitmeet.global.response.ResponseUtil;
import com.ssafy.fitmeet.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
@Tag(name = "Community", description = "커뮤니티 게시글/댓글/태그 API")
public class CommunityRestController {

    private final CommunityService communityService;

    /**
     * 게시글 작성
     * - 제목, 내용, 카테고리, 태그
     * - 고급 설정: activityDate, shareDiet, shareStreak, shareWorkout, workoutNote, workoutVideoUrl
     */
    @PostMapping
    @Operation(summary = "게시글 작성", description = "커뮤니티 게시글 등록 (식단/스트릭/운동 공유 옵션 포함)")
    public ResponseEntity<Response<PostDetailResponse>> createPost(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody PostCreateRequest request) {
        Long userId = userDetails.getId();

        Long postId = communityService.createPost(userId, request);
        PostDetailResponse body = communityService.getPostDetail(userId, postId, true);

        return ResponseEntity.ok(ResponseUtil.ok("게시글 등록 완료", body));
    }

    /**
     * 게시글 상세 조회
     * - 조회수 +1
     * - activityDate 기반 식단/스트릭/운동 요약 포함
     */
    @GetMapping("/{postId}")
    @Operation(summary = "게시글 상세 조회", description = "게시글 상세, 태그, 활동 요약(식단/스트릭/운동), 좋아요 여부 등을 조회")
    public ResponseEntity<Response<PostDetailResponse>> getPostDetail(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long postId) {
        Long currentUserId = (userDetails != null) ? userDetails.getId() : null; // 비로그인 허용 시 대비
        PostDetailResponse body = communityService.getPostDetail(currentUserId, postId, false);

        return ResponseEntity.ok(ResponseUtil.ok(body));
    }

    /**
     * 게시글 수정
     */
    @PutMapping("/{postId}")
    @Operation(summary = "게시글 수정", description = "본인이 작성한 커뮤니티 게시글 수정")
    public ResponseEntity<Response<Void>> updatePost(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long postId, @RequestBody PostUpdateRequest request) {
        Long userId = userDetails.getId();
        communityService.updatePost(userId, postId, request);

        return ResponseEntity.ok(ResponseUtil.ok("게시글 수정 완료", null));
    }

    /**
     * 게시글 소프트 삭제 (휴지통)
     */
    @DeleteMapping("/{postId}")
    @Operation(summary = "게시글 삭제", description = "게시글 소프트 삭제(휴지통 이동)")
    public ResponseEntity<Response<Void>> deletePost(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long postId) {
        Long userId = userDetails.getId();
        communityService.softDeletePost(userId, postId);

        return ResponseEntity.ok(ResponseUtil.ok("게시글 삭제 완료", null));
    }

    /**
     * 게시글 복원 (휴지통 → 복원)
     */
    @PutMapping("/{postId}/restore")
    @Operation(summary = "게시글 복원", description = "휴지통에 있는 게시글 복원")
    public ResponseEntity<Response<Void>> restorePost(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long postId) {
        Long userId = userDetails.getId();
        communityService.restorePost(userId, postId);

        return ResponseEntity.ok(ResponseUtil.ok("게시글 복원 완료", null));
    }

    /**
     * 게시글 검색 / 목록 조회
     * - keyword, tag, category, sort(POPULAR/LATEST/OLDEST), page, size
     * - 기본 sort = POPULAR
     */
    @GetMapping
    @Operation(summary = "게시글 목록/검색", description = "키워드/태그/카테고리/정렬 기준으로 게시글 목록 조회")
    public ResponseEntity<Response<PostPageResponse>> searchPosts(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "category", required = false) String category,
            @Schema(description = "목록 정렬 기준", example = "POPULAR / OLDEST / LATEST")
            @RequestParam(value = "sort", required = false, defaultValue = "POPULAR") String sort,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        Long currentUserId = (userDetails != null) ? userDetails.getId() : null;

        PostSearchCondition cond = new PostSearchCondition(keyword, tag, category, sort, page, size, 0);
        PostPageResponse body = communityService.searchPosts(currentUserId, cond);
        return ResponseEntity.ok(ResponseUtil.ok(body));
    }

    /**
     * 좋아요 등록
     */
    @PostMapping("/{postId}/like")
    @Operation(summary = "게시글 좋아요", description = "게시글에 좋아요 추가")
    public ResponseEntity<Response<Void>> likePost(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long postId) {
        Long userId = userDetails.getId();
        communityService.likePost(userId, postId);

        return ResponseEntity.ok(ResponseUtil.ok("좋아요 추가 완료", null));
    }

    /**
     * 좋아요 취소
     */
    @DeleteMapping("/{postId}/like")
    @Operation(summary = "게시글 좋아요 취소", description = "게시글에 추가했던 좋아요 취소")
    public ResponseEntity<Response<Void>> unlikePost(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long postId) {
        Long userId = userDetails.getId();
        communityService.unlikePost(userId, postId);

        return ResponseEntity.ok(ResponseUtil.ok("좋아요 취소 완료", null));
    }

    /**
     * 내가 좋아요한 게시글 목록
     */
    @GetMapping("/likes/me")
    @Operation(summary = "내가 좋아요한 게시글 목록", description = "현재 로그인한 사용자가 좋아요한 게시글 목록 조회")
    public ResponseEntity<Response<PostPageResponse>> getMyLikedPosts(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        Long userId = userDetails.getId();
        PostPageResponse body = communityService.getMyLikedPosts(userId, page, size);

        return ResponseEntity.ok(ResponseUtil.ok(body));
    }

    /**
     * 내가 등록한 게시글 목록
     */
    @GetMapping("/posts/me")
    @Operation(summary = "내가 등록한 게시글 목록", description = "현재 로그인한 사용자가 등록한 게시글 목록 조회")
    public ResponseEntity<Response<PostPageResponse>> getMyPosts(@AuthenticationPrincipal CustomUserDetails userDetails,
              @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
              @RequestParam(value = "size", required = false, defaultValue = "20") Integer size) {
        Long userId = userDetails.getId();
        PostPageResponse body = communityService.getMyPosts(userId, page, size);

        return ResponseEntity.ok(ResponseUtil.ok(body));
    }

    /**
     * 댓글 작성
     * - parentId가 있으면 대댓글
     */
    @PostMapping("/{postId}/comments")
    @Operation(summary = "댓글 등록", description = "게시글에 댓글 또는 대댓글 등록")
    public ResponseEntity<Response<CommentResponse>> addComment(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long postId, @RequestBody CommentCreateRequest request) {
        Long userId = userDetails.getId();
        Long commentId = communityService.addComment(userId, postId, request);

        List<CommentResponse> comments = communityService.getComments(userId, postId);
        CommentResponse created = comments.stream()
                .filter(c -> c.id().equals(commentId))
                .findFirst()
                .orElse(null);

        return ResponseEntity.ok(ResponseUtil.ok("댓글 등록 완료", created));
    }

    /**
     * 댓글 목록 조회
     */
    @GetMapping("/{postId}/comments")
    @Operation(summary = "댓글 목록 조회", description = "게시글에 등록된 댓글/대댓글 목록 조회")
    public ResponseEntity<Response<List<CommentResponse>>> getComments(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long postId) {
        Long currentUserId = (userDetails != null) ? userDetails.getId() : null;
        List<CommentResponse> body = communityService.getComments(currentUserId, postId);

        return ResponseEntity.ok(ResponseUtil.ok(body));
    }

    /**
     * 댓글 삭제 (소프트 삭제)
     */
    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "댓글 삭제", description = "본인이 작성한 댓글 소프트 삭제")
    public ResponseEntity<Response<Void>> deleteComment(@AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable Long commentId) {
        Long userId = userDetails.getId();
        communityService.deleteComment(userId, commentId);

        return ResponseEntity.ok(ResponseUtil.ok("댓글 삭제 완료", null));
    }

    /**
     * 인기 태그 Top N
     */
    @GetMapping("/tags/top")
    @Operation(summary = "인기 태그 Top N", description = "usage_count 기준으로 인기 태그 상위 N개 조회")
    public ResponseEntity<Response<List<TagSummaryResponse>>> getTopTags(@RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit) {
        List<TagSummaryResponse> body = communityService.getTopTags(limit);
        return ResponseEntity.ok(ResponseUtil.ok(body));
    }
}
