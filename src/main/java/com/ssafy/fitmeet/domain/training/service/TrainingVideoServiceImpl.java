package com.ssafy.fitmeet.domain.training.service;

import com.ssafy.fitmeet.domain.training.dao.TrainingVideoCommentDao;
import com.ssafy.fitmeet.domain.training.dao.TrainingVideoDao;
import com.ssafy.fitmeet.domain.training.dao.TrainingVideoHistoryDao;
import com.ssafy.fitmeet.domain.training.dto.TrainingVideoDto.*;
import com.ssafy.fitmeet.domain.training.entity.TrainingVideo;
import com.ssafy.fitmeet.domain.training.entity.TrainingVideoComment;
import com.ssafy.fitmeet.domain.training.entity.TrainingVideoHistory;
import com.ssafy.fitmeet.domain.user.dao.UserDao;
import com.ssafy.fitmeet.domain.user.entity.User;
import com.ssafy.fitmeet.global.error.CustomException;
import com.ssafy.fitmeet.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TrainingVideoServiceImpl implements TrainingVideoService {

    private final TrainingVideoDao trainingVideoDao;
    private final TrainingVideoHistoryDao trainingVideoHistoryDao;
    private final TrainingVideoCommentDao trainingVideoCommentDao;
    private final UserDao userDao;

    @Override
    public List<VideoCardResponse> getRecommended(Long userId, String category, int limit) {
        List<TrainingVideo> list = trainingVideoDao.findRecommended(category, limit);

        return list.stream()
                .map(v -> VideoCardResponse.of(v, trainingVideoDao.isLiked(userId, v.getId()) != null))
                .toList();
    }

    @Override
    public VideoPageResponse searchVideos(Long userId, VideoSearchRequest req) {

        int page = (req.page() != null && req.page() >= 0) ? req.page() : 0;
        int size = (req.size() != null && req.size() > 0) ? req.size() : 20;
        int offset = page * size;

        // 목록
        List<TrainingVideo> list = trainingVideoDao.findVideos(
                req.category(),
                req.level(),
                req.shortVideo(),
                req.sort(),
                offset,
                size
        );

        List<VideoCardResponse> content = list.stream()
                .map(v -> VideoCardResponse.of(v, trainingVideoDao.isLiked(userId, v.getId()) != null))
                .toList();

        // 총 개수
        long total = trainingVideoDao.countVideos(
                req.category(),
                req.level(),
                req.shortVideo(),
                req.sort()
        );

        return VideoPageResponse.of(content, page, size, total);
    }


    @Override
    @Transactional
    public boolean toggleLike(Long userId, Long videoId) {
        TrainingVideo video = trainingVideoDao.findById(videoId);
        if (video == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        Integer liked = trainingVideoDao.isLiked(userId, videoId);

        if (liked == null) {
            trainingVideoDao.insertLike(userId, videoId);
            trainingVideoDao.increaseLikeCount(videoId);
            return true;
        } else {
            trainingVideoDao.deleteLike(userId, videoId);
            trainingVideoDao.decreaseLikeCount(videoId);
            return false;
        }
    }

    @Override
    @Transactional
    public void completeVideo(Long userId, Long videoId, Integer durationSec, String memo) {
        TrainingVideo video = trainingVideoDao.findById(videoId);
        if (video == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        LocalDate today = LocalDate.now();
        TrainingVideoHistory history = trainingVideoHistoryDao.findByUserAndVideoAndDate(userId, videoId, today);

        if (history == null) {
            history = TrainingVideoHistory.builder()
                    .userId(userId)
                    .videoId(videoId)
                    .activityDate(today)
                    .completed(1)
                    .durationSec(durationSec)
                    .memo(memo)
                    .build();
            trainingVideoHistoryDao.insertHistory(history);
        } else {
            history.setCompleted(1);
            history.setDurationSec(durationSec);
            history.setMemo(memo);
            trainingVideoHistoryDao.updateHistory(history);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodayVideoResponse> getTodayCompletedVideos(Long userId) {
        LocalDate today = LocalDate.now();
        List<TrainingVideoHistory> histories = trainingVideoHistoryDao.findByUserAndDate(userId, today);

        if (histories.isEmpty()) return List.of();

        return histories.stream()
                .map(h -> {
                    TrainingVideo v = trainingVideoDao.findById(h.getVideoId());
                    return TodayVideoResponse.of(h, v);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VideoDetailResponse getVideoDetail(Long userId, Long videoId) {
        TrainingVideo video = trainingVideoDao.findById(videoId);
        if (video == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        boolean liked = trainingVideoDao.isLiked(userId, videoId) != null;

        // 오늘 완료 여부
        LocalDate today = LocalDate.now();
        TrainingVideoHistory history = trainingVideoHistoryDao.findByUserAndVideoAndDate(userId, videoId, today);
        boolean completedToday = (history != null && history.getCompleted() == 1);

        // 댓글 목록
        List<TrainingVideoComment> comments = trainingVideoCommentDao.findByVideoId(videoId);
        List<VideoCommentResponse> commentResponses = comments.stream()
                .map(c -> {
                    User u = userDao.findById(c.getUserId());
                    String nickname = (u != null) ? u.getNickname() : "탈퇴회원";
                    return new VideoCommentResponse(
                            c.getId(),
                            c.getUserId(),
                            nickname,
                            c.getContent(),
                            c.getCreatedAt()
                    );
                })
                .toList();

        List<String> tags = video.getTags() == null || video.getTags().isEmpty()
                ? List.of()
                : Arrays.asList(video.getTags().split(","));

        return new VideoDetailResponse(
                video.getId(),
                video.getYoutubeVideoId(),
                video.getTitle(),
                video.getDescription(),
                video.getThumbnailUrl(),
                video.getDurationSec(),
                video.getMainCategory(),
                video.getLevel(),
                tags,
                video.getViewCount(),
                video.getLikeCount(),
                Optional.ofNullable(video.getYoutubeViewCnt()).orElse(0L),
                Optional.ofNullable(video.getYoutubeLikeCnt()).orElse(0L),
                liked,
                completedToday,
                commentResponses
        );
    }

    @Override
    @Transactional
    public VideoCommentResponse addComment(Long userId, Long videoId, String content) {
        TrainingVideo video = trainingVideoDao.findById(videoId);
        if (video == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        TrainingVideoComment comment = TrainingVideoComment.builder()
                .videoId(videoId)
                .userId(userId)
                .content(content)
                .build();

        trainingVideoCommentDao.insertComment(comment);

        User u = userDao.findById(userId);
        String nickname = (u != null) ? u.getNickname() : "탈퇴회원";

        return new VideoCommentResponse(
                comment.getId(),
                userId,
                nickname,
                content,
                comment.getCreatedAt()
        );
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        TrainingVideoComment comment = trainingVideoCommentDao.findById(commentId);
        if (comment == null || comment.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        if (!Objects.equals(comment.getUserId(), userId)) {
            throw new CustomException(ErrorCode.AUTH_ACCESS_DENIED);
        }
        trainingVideoCommentDao.softDelete(commentId);
    }
}
