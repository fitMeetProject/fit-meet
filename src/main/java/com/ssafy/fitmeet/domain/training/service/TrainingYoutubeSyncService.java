package com.ssafy.fitmeet.domain.training.service;

import com.ssafy.fitmeet.domain.training.dao.TrainingVideoDao;
import com.ssafy.fitmeet.domain.training.dao.TrainingYoutubeSourceDao;
import com.ssafy.fitmeet.domain.training.entity.TrainingVideo;
import com.ssafy.fitmeet.domain.training.entity.TrainingYoutubeSource;
import com.ssafy.fitmeet.global.util.YoutubeApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class TrainingYoutubeSyncService {

    private final TrainingYoutubeSourceDao sourceDao;
    private final TrainingVideoDao trainingVideoDao;
    private final YoutubeApiClient youtubeApiClient;

    /**
     * 활성화된 모든 YouTube source 기준으로 영상 동기화
     */
    @Transactional
    public void syncAllSources() {
        List<TrainingYoutubeSource> sources = sourceDao.findActiveSources();
        log.info("YouTube Sync 시작 - 활성 소스 {}개", sources.size());

        for (TrainingYoutubeSource source : sources) {
            try {
                syncSource(source);
            } catch (Exception e) {
                log.error("YouTube Sync 실패 - sourceId={}, type={}, value={}",
                        source.getId(), source.getSourceType(), source.getSourceValue(), e);
            }
        }
    }

    /**
     * 개별 source 기준으로 영상 동기화
     */
    @Transactional
    public void syncSource(TrainingYoutubeSource source) {
        log.info("YouTube Sync - sourceId={}, type={}, value={}",
                source.getId(), source.getSourceType(), source.getSourceValue());

        // 1) videoId 목록 수집
        List<String> videoIds = fetchVideoIdsFromSource(source);

        if (videoIds.isEmpty()) {
            log.warn("YouTube Sync - sourceId={} 에 대해 수집된 videoId 없음", source.getId());
            return;
        }

        // 2) videoIds 기반 상세 정보 조회 (50개씩 chunk)
        List<TrainingVideo> videos = fetchVideoDetails(videoIds, source);

        // 3) DB upsert
        for (TrainingVideo tv : videos) {
            TrainingVideo exists = trainingVideoDao.findByYoutubeVideoId(tv.getYoutubeVideoId());
            if (exists == null) {
                trainingVideoDao.insertVideo(tv);
            } else {
                trainingVideoDao.updateVideoFromYoutube(tv);
            }
        }

        log.info("YouTube Sync 완료 - sourceId={}, 수집/업데이트 영상 수={}",
                source.getId(), videos.size());
    }

    /**
     * source 기준으로 videoId 리스트 가져오기
     */
    @SuppressWarnings("unchecked")
    private List<String> fetchVideoIdsFromSource(TrainingYoutubeSource source) {
        int maxResults = Optional.ofNullable(source.getMaxResults()).orElse(20);
        String type = source.getSourceType();

        List<String> videoIds = new ArrayList<>();

        if ("KEYWORD".equalsIgnoreCase(type)) {
            Map<String, Object> resp = youtubeApiClient.search(source.getSourceValue(), maxResults);
            List<Map<String, Object>> items = (List<Map<String, Object>>) resp.get("items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    Map<String, Object> idObj = (Map<String, Object>) item.get("id");
                    if (idObj != null) {
                        Object vid = idObj.get("videoId");
                        if (vid != null) {
                            videoIds.add(vid.toString());
                        }
                    }
                }
            }
        } else if ("PLAYLIST".equalsIgnoreCase(type)) {
            Map<String, Object> resp = youtubeApiClient.getPlaylistItems(source.getSourceValue(), maxResults);
            List<Map<String, Object>> items = (List<Map<String, Object>>) resp.get("items");
            if (items != null) {
                for (Map<String, Object> item : items) {
                    Map<String, Object> contentDetails = (Map<String, Object>) item.get("contentDetails");
                    if (contentDetails != null) {
                        Object vid = contentDetails.get("videoId");
                        if (vid != null) {
                            videoIds.add(vid.toString());
                        }
                    }
                }
            }
        }

        // 중복 제거
        return videoIds.stream().distinct().collect(Collectors.toList());
    }

    /**
     * videoId 목록에 대해 YouTube videos.list 호출 → TrainingVideo 리스트로 변환
     */
    @SuppressWarnings("unchecked")
    private List<TrainingVideo> fetchVideoDetails(List<String> videoIds, TrainingYoutubeSource source) {
        List<TrainingVideo> result = new ArrayList<>();

        int batchSize = 50;
        for (int i = 0; i < videoIds.size(); i += batchSize) {
            List<String> batch = videoIds.subList(i, Math.min(i + batchSize, videoIds.size()));
            Map<String, Object> resp = youtubeApiClient.getVideoDetails(batch);
            log.info("YT videos resp = {}", resp);

            List<Map<String, Object>> items = (List<Map<String, Object>>) resp.get("items");
            if (items == null) {
                log.warn("videos.list 응답에 items 없음: {}", resp);
                continue;
            }

            for (Map<String, Object> item : items) {
                String videoId = Objects.toString(item.get("id"), null);
                if (videoId == null) continue;

                Map<String, Object> snippet = (Map<String, Object>) item.get("snippet");
                Map<String, Object> contentDetails = (Map<String, Object>) item.get("contentDetails");
                Map<String, Object> statistics = (Map<String, Object>) item.get("statistics");

                String title = snippet != null ? Objects.toString(snippet.get("title"), "") : "";
                String description = snippet != null ? Objects.toString(snippet.get("description"), "") : "";
                String channelTitle = snippet != null ? Objects.toString(snippet.get("channelTitle"), "") : "";
                LocalDateTime publishedAt = parsePublishedAt(snippet != null ? Objects.toString(snippet.get("publishedAt"), null) : null);

                // 썸네일
                String thumbnailUrl = null;
                if (snippet != null) {
                    Map<String, Object> thumbnails = (Map<String, Object>) snippet.get("thumbnails");
                    if (thumbnails != null) {
                        Map<String, Object> high = (Map<String, Object>) thumbnails.get("high");
                        if (high == null) {
                            high = (Map<String, Object>) thumbnails.get("medium");
                        }
                        if (high == null) {
                            high = (Map<String, Object>) thumbnails.get("default");
                        }
                        if (high != null) {
                            thumbnailUrl = Objects.toString(high.get("url"), null);
                        }
                    }
                }

                // 재생시간
                int durationSec = 0;
                if (contentDetails != null) {
                    String isoDuration = Objects.toString(contentDetails.get("duration"), null);
                    durationSec = parseDurationToSeconds(isoDuration);
                }

                // 통계
                long ytViewCnt = 0L;
                long ytLikeCnt = 0L;
                if (statistics != null) {
                    Object v = statistics.get("viewCount");
                    Object l = statistics.get("likeCount");
                    if (v != null) ytViewCnt = Long.parseLong(v.toString());
                    if (l != null) ytLikeCnt = Long.parseLong(l.toString());
                }

                TrainingVideo video = TrainingVideo.builder()
                        .youtubeVideoId(videoId)
                        .title(title)
                        .description(description)
                        .thumbnailUrl(thumbnailUrl)
                        .durationSec(durationSec)
                        .mainCategory(source.getMainCategory())
                        .level(source.getLevel())
                        .tags(null)
                        .channelTitle(channelTitle)
                        // YouTube 통계
                        .youtubeViewCnt(ytViewCnt)
                        .youtubeLikeCnt(ytLikeCnt)
                        .publishedAt(publishedAt)
                        // 앱 내부 값
                        .viewCount(0L)
                        .likeCount(0L)
                        .isRecommended(0)
                        .isActive(1)
                        .build();


                result.add(video);
            }
        }

        return result;
    }

    /**
     * ISO8601 Duration을 초 단위로 변환
     */
    private int parseDurationToSeconds(String isoDuration) {
        if (isoDuration == null || isoDuration.isEmpty()) return 0;
        try {
            Duration d = Duration.parse(isoDuration);
            return (int) d.getSeconds();
        } catch (Exception e) {
            log.warn("재생시간 파싱 실패: {}", isoDuration, e);
            return 0;
        }
    }

    /**
     * publishedAt 문자열을 LocalDateTime 으로 변환
     */
    private LocalDateTime parsePublishedAt(String publishedAt) {
        if (publishedAt == null) return null;
        try {
            return OffsetDateTime.parse(publishedAt).toLocalDateTime();
        } catch (Exception e) {
            log.warn("publishedAt 파싱 실패: {}", publishedAt, e);
            return null;
        }
    }
}
