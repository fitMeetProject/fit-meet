package com.ssafy.fitmeet.domain.training.service;

import com.ssafy.fitmeet.domain.training.dto.TrainingVideoDto.*;

import java.util.List;

public interface TrainingVideoService {

    /**
     * 추천 운동영상 조회
     */
    List<VideoCardResponse> getRecommended(Long userId, String category, int limit);

    /**
     * 전체/필터 운동영상 조회
     */
    VideoPageResponse searchVideos(Long userId, VideoSearchRequest request);

    /**
     * 좋아요 토글
     */
    boolean toggleLike(Long userId, Long videoId);

    /**
     * 운동 완료 표시
     */
    void completeVideo(Long userId, Long videoId, Integer durationSec, String memo);

    /**
     * 운동 완료/취소 토글
     */
    boolean toggleComplete(Long userId, Long videoId, Integer durationSec, String memo);

    /**
     * 오늘 운동한 영상 목록 가져오기
     */
    List<TodayVideoResponse> getTodayCompletedVideos(Long userId);

    /**
     * 운동 영상 상세
     */
    VideoDetailResponse getVideoDetail(Long userId, Long videoId);

    /**
     * 댓글 등록
     */
    VideoCommentResponse addComment(Long userId, Long videoId, String content);

    /**
     * 댓글 삭제
     */
    void deleteComment(Long userId, Long commentId);
}
