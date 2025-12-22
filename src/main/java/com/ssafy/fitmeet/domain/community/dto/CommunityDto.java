package com.ssafy.fitmeet.domain.community.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class CommunityDto {

    // multipart data 파트로 받는 요청 DTO
    public record PostCreateRequest(
            String title,
            String content,
            String category,
            List<String> tags,
            LocalDate activityDate,
            Boolean shareDiet,
            Boolean shareStreak,
            Boolean shareWorkout,
            String dietNote,
            List<Long> dietIds,
            String workoutNote,
            List<Long> workoutVideoIds
    ) {}

    public record PostUpdateRequest(
            String title,
            String content,
            String category,
            List<String> tags,
            LocalDate activityDate,
            Boolean shareDiet,
            Boolean shareStreak,
            Boolean shareWorkout,
            String dietNote,
            List<Long> dietIds,
            String workoutNote,
            List<Long> workoutVideoIds
    ) {}

    // 운동영상 카드(조회 시 조인 결과)
    public record WorkoutVideoCard(
            Long id,
            String youtubeVideoId,
            String title,
            String thumbnailUrl,
            int durationSec,
            String youtubeUrl
    ) {
        public static WorkoutVideoCard of(Long id, String youtubeVideoId, String title, String thumbnailUrl, int durationSec) {
            String url = (youtubeVideoId == null) ? null : "https://www.youtube.com/watch?v=" + youtubeVideoId;
            return new WorkoutVideoCard(id, youtubeVideoId, title, thumbnailUrl, durationSec, url);
        }
    }

    // 검색 조건
    public record PostSearchCondition(
            String keyword,
            String tag,
            String category,
            String sort, // POPULAR / LATEST / OLDEST
            Integer page,
            Integer size,
            Integer offset
    ) {}

    public record PostPageResponse(
            List<PostSummaryResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
        public static PostPageResponse of(List<PostSummaryResponse> content, int page, int size, long totalElements) {
            int totalPages = (int) Math.ceil((double) totalElements / size);
            boolean hasNext = page + 1 < totalPages;
            return new PostPageResponse(content, page, size, totalElements, totalPages, hasNext);
        }
    }


    // 게시글 목록용 요약 응답
    public record PostSummaryResponse(
            Long id,
            String title,
            String contentPreview,
            String category,
            LocalDate activityDate,
            boolean shareDiet,
            boolean shareStreak,
            boolean shareWorkout,
            int likeCount,
            int viewCount,
            int commentCount,
            boolean likedByMe,
            List<String> tags,
            String writerNickname,
            String writerProfileImageUrl,
            LocalDateTime createdAt

    ) {}

    // 활동 요약 (식단/스트릭/운동)
    public record ActivitySummaryResponse(
            LocalDate activityDate,
            boolean dietLogged,
            List<Long> dietIds,
            String dietNote,
            Double todayKcal,
            Double todayCarbo,
            Double todayProtein,
            Double todayFat,
            Double todaySugar,
            Double todaySodium,
            Integer streakCount,
            String workoutNote,
            List<Long> workoutVideoIds
    ) {}

    public record DietCard(
            Long dietId,
            LocalDateTime date,
            String mealType,
            String description,
            String imageUrl,
            Double totalKcal,
            Double totalCarbo,
            Double totalProtein,
            Double totalFat
    ) {}

    // 상세
    public record PostDetailResponse(
            Long id,
            String title,
            String content,
            String category,
            String postImageUrl,
            LocalDate activityDate,
            boolean shareDiet,
            boolean shareStreak,
            boolean shareWorkout,
            String dietNote,
            String workoutNote,
            int likeCount,
            int viewCount,
            int commentCount,
            boolean likedByMe,
            List<String> tags,
            String writerNickname,
            String writerProfileImageUrl,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<DietCard> dietCards,
            List<WorkoutVideoCard> workoutVideos,
            ActivitySummaryResponse activitySummary
    ) {}

    // 댓글 등록 요청
    public record CommentCreateRequest(
            String content,
            Long parentId
    ) {}

    // 댓글 응답
    public record CommentResponse(
            Long id,
            Long postId,
            String content,
            boolean deleted,
            Long parentId,
            String writerNickname,
            String writerProfileImageUrl,
            LocalDateTime createdAt
    ) {}

    // 인기 태그 응답
    public record TagSummaryResponse(
            Long id,
            String name,
            int usageCount
    ) {}
}
