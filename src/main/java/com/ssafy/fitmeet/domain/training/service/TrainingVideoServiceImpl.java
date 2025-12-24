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
import com.ssafy.fitmeet.domain.user.service.UserDailyActivityService;
import com.ssafy.fitmeet.global.error.CustomException;
import com.ssafy.fitmeet.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TrainingVideoServiceImpl implements TrainingVideoService {

    private final TrainingVideoDao trainingVideoDao;
    private final TrainingVideoHistoryDao trainingVideoHistoryDao;
    private final TrainingVideoCommentDao trainingVideoCommentDao;
    private final UserDailyActivityService userDailyActivityService;

    private final UserDao userDao;

    @Override
    @Transactional(readOnly = true)
    public List<VideoCardResponse> getRecommended(Long userId, String category, int limit) {
        List<TrainingVideo> list = trainingVideoDao.findRecommended(category, limit);
        if (list.isEmpty()) return List.of();

        LocalDate today = LocalDate.now();
        Set<Long> todayCompletedSet = new HashSet<>(trainingVideoHistoryDao.findCompletedVideoIdsByUserAndDate(userId, today));
        Set<Long> everCompletedSet = new HashSet<>(trainingVideoHistoryDao.findEverCompletedVideoIdsByUser(userId));

        return list.stream()
                .map(v -> {
                    boolean liked = trainingVideoDao.isLiked(userId, v.getId()) != null;
                    boolean completedToday = todayCompletedSet.contains(v.getId());
                    boolean doneBefore = everCompletedSet.contains(v.getId());
                    return VideoCardResponse.of(v, liked, completedToday, doneBefore);
                })
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public VideoPageResponse searchVideos(Long userId, VideoSearchRequest req) {
        int page = (req.page() != null && req.page() >= 0) ? req.page() : 0;
        int size = (req.size() != null && req.size() > 0) ? req.size() : 20;
        int offset = page * size;

        List<TrainingVideo> list = trainingVideoDao.findVideos(
                req.category(),
                req.level(),
                req.shortVideo(),
                req.sort(),
                offset,
                size
        );

        long total = trainingVideoDao.countVideos(
                req.category(),
                req.level(),
                req.shortVideo(),
                req.sort()
        );

        if (list.isEmpty()) return VideoPageResponse.of(List.of(), page, size, total);

        LocalDate today = LocalDate.now();
        Set<Long> todayCompletedSet = new HashSet<>(trainingVideoHistoryDao.findCompletedVideoIdsByUserAndDate(userId, today));
        Set<Long> everCompletedSet = new HashSet<>(trainingVideoHistoryDao.findEverCompletedVideoIdsByUser(userId));

        List<VideoCardResponse> content = list.stream()
                .map(v -> {
                    boolean liked = trainingVideoDao.isLiked(userId, v.getId()) != null;
                    boolean completedToday = todayCompletedSet.contains(v.getId());
                    boolean doneBefore = everCompletedSet.contains(v.getId());
                    return VideoCardResponse.of(v, liked, completedToday, doneBefore);
                })
                .toList();

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

        userDailyActivityService.markWorkoutLogged(userId, today);
    }

    @Override
    @Transactional
    public boolean toggleComplete(Long userId, Long videoId, Integer durationSec, String memo) {
        TrainingVideo video = trainingVideoDao.findById(videoId);
        if (video == null) throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);

        LocalDate today = LocalDate.now();
        TrainingVideoHistory history = trainingVideoHistoryDao.findByUserAndVideoAndDate(userId, videoId, today);

        boolean nowCompleted;

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
            nowCompleted = true;
        } else {
            int current = (history.getCompleted() != null) ? history.getCompleted() : 0;
            int next = (current == 1) ? 0 : 1;

            history.setCompleted(next);

            if (next == 1) {
                history.setDurationSec(durationSec);
                history.setMemo(memo);
            }
            trainingVideoHistoryDao.updateHistory(history);
            nowCompleted = (next == 1);
        }

        int completedCount = trainingVideoHistoryDao.countCompletedByUserAndDate(userId, today);
        if (completedCount > 0) userDailyActivityService.markWorkoutLogged(userId, today);
        else userDailyActivityService.unmarkWorkoutLogged(userId, today);

        return nowCompleted;
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
    @Transactional // readOnly 제거
    public VideoDetailResponse getVideoDetail(Long userId, Long videoId) {
        TrainingVideo video = trainingVideoDao.findById(videoId);
        if (video == null) throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);

        trainingVideoDao.increaseViewCount(videoId);
        video.setViewCount(video.getViewCount() + 1);

        boolean liked = trainingVideoDao.isLiked(userId, videoId) != null;

        LocalDate today = LocalDate.now();
        TrainingVideoHistory todayHistory = trainingVideoHistoryDao.findByUserAndVideoAndDate(userId, videoId, today);
        boolean completedToday = (todayHistory != null && Objects.equals(todayHistory.getCompleted(), 1));
        boolean doneBefore = trainingVideoHistoryDao.findEverCompletedVideoIdsByUser(userId).contains(videoId);

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

        List<String> tags = (video.getTags() == null || video.getTags().isEmpty()) ? List.of() : Arrays.asList(video.getTags().split(","));

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
                doneBefore,
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
