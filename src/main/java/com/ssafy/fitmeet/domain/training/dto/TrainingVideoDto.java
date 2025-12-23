package com.ssafy.fitmeet.domain.training.dto;

import com.ssafy.fitmeet.domain.training.entity.TrainingVideo;
import com.ssafy.fitmeet.domain.training.entity.TrainingVideoHistory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class TrainingVideoDto {

    @Schema(description = "운동 영상 카드 정보")
    public record VideoCardResponse(
            Long id,
            String youtubeVideoId,
            String title,
            String description,
            String thumbnailUrl,
            int durationSec,
            String mainCategory,
            String level,
            List<String> tags,
            long viewCount,
            long likeCount,
            boolean liked,
            boolean completed,
            boolean doneBefore
    ) {
        public static VideoCardResponse of(TrainingVideo v, boolean liked, boolean completed, boolean doneBefore) {
            List<String> tagList = (v.getTags() == null || v.getTags().isEmpty())
                    ? List.of()
                    : Arrays.asList(v.getTags().split(","));

            return new VideoCardResponse(
                    v.getId(),
                    v.getYoutubeVideoId(),
                    v.getTitle(),
                    v.getDescription(),
                    v.getThumbnailUrl(),
                    v.getDurationSec(),
                    v.getMainCategory(),
                    v.getLevel(),
                    tagList,
                    v.getViewCount(),
                    v.getLikeCount(),
                    liked,
                    completed,
                    doneBefore
            );
        }
    }

    @Schema(description = "운동 영상 검색 요청")
    public record VideoSearchRequest(
            String category, // YOGA, PILATES, HOME
            String level, // BEGINNER, INTERMEDIATE
            Boolean shortVideo, // true이면 15분 이하
            String sort, // LATEST, POPULAR
            Integer page,
            Integer size
    ) {}

    @Schema(description = "운동 영상 목록")
    public record VideoPageResponse(
            List<VideoCardResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
        public static VideoPageResponse of(List<VideoCardResponse> content, int page, int size, long totalElements) {
            int totalPages = (int) Math.ceil((double) totalElements / size);
            boolean hasNext = page + 1 < totalPages;
            return new VideoPageResponse(content, page, size, totalElements, totalPages, hasNext);
        }
    }

    @Schema(description = "오늘 완료한 운동 영상 카드")
    public record TodayVideoResponse(
            Long historyId,
            Long videoId,
            String title,
            String thumbnailUrl,
            int durationSec,
            String mainCategory,
            String level
    ) {
        public static TodayVideoResponse of(TrainingVideoHistory h, TrainingVideo v) {
            return new TodayVideoResponse(
                    h.getId(),
                    v.getId(),
                    v.getTitle(),
                    v.getThumbnailUrl(),
                    v.getDurationSec(),
                    v.getMainCategory(),
                    v.getLevel()
            );
        }
    }

    @Schema(description = "운동 영상 완료 요청")
    public record CompleteVideoRequest(
            Integer durationSec,
            String memo
    ) {}

    @Schema(description = "운동 영상 댓글 응답")
    public record VideoCommentResponse(
            Long id,
            Long userId,
            String nickname,
            String content,
            LocalDateTime createdAt
    ) {}

    @Schema(description = "운동 영상 상세 응답")
    public record VideoDetailResponse(
            Long id,
            String youtubeVideoId,
            String title,
            String description,
            String thumbnailUrl,
            int durationSec,
            String mainCategory,
            String level,
            List<String> tags,
            long viewCount,
            long likeCount,
            long youtubeViewCnt,
            long youtubeLikeCnt,
            boolean liked,
            boolean completed,
            boolean doneBefore,
            List<VideoCommentResponse> comments
    ) {}
    @Schema(description = "운동 영상 댓글 작성 요청")
    public record CreateCommentRequest(
            String content
    ) {}

}
