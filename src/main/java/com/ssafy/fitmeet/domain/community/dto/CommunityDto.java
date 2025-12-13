package com.ssafy.fitmeet.domain.community.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class CommunityDto {

    // 게시글 등록 요청
    public record PostCreateRequest(
            String title,
            String content,
            String category,
            List<String> tags,
            LocalDate activityDate,
            Boolean shareDiet,
            Boolean shareStreak,
            Boolean shareWorkout,
            String workoutNote,
            String workoutVideoUrl
    ) {}

    // 게시글 수정 요청
    public record PostUpdateRequest(
            String title,
            String content,
            String category,
            List<String> tags,
            LocalDate activityDate,
            Boolean shareDiet,
            Boolean shareStreak,
            Boolean shareWorkout,
            String workoutNote,
            String workoutVideoUrl
    ) {}

    // 검색 조건
    public record PostSearchCondition(
            String keyword,
            String tag,
            String category,
            String sort, // POPULAR / LATEST / OLDEST
            Integer page,
            Integer size
    ) {}

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
            boolean workoutLogged,
            boolean challengeDone,
            Double todayKcal,
            Double todayCarbo,
            Double todayProtein,
            Double todayFat,
            Double todaySugar,
            Double todaySodium,
            Integer streakCount,
            String workoutNote,
            String workoutVideoUrl
    ) {}

    // 게시글 상세 응답
    public record PostDetailResponse(
            Long id,
            String title,
            String content,
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
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
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
