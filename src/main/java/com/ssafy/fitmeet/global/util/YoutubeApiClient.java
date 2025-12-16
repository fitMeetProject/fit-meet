package com.ssafy.fitmeet.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class YoutubeApiClient {

    @Value("${open-api.youtube.key}")
    private String apiKey;

    @Value("${open-api.youtube.end-point}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    private String normalizeBaseUrl() {
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private String enc(String v) {
        return UriUtils.encode(v, StandardCharsets.UTF_8);
    }

    /**
     * 검색어 기반 영상 검색 (search.list)
     */
    public Map<String, Object> search(String query, int maxResults) {

        String url = normalizeBaseUrl() + "search"
                        + "?part=snippet"
                        + "&type=video"
                        + "&regionCode=KR"
                        + "&relevanceLanguage=ko"
                        + "&maxResults=" + maxResults
                        + "&q=" + query
                        + "&key=" + apiKey;

        log.info("search url: {}", url);
        return restTemplate.getForObject(url, Map.class);
    }

    /**
     * playlistItems.list - 플레이리스트의 모든 영상 목록 조회
     */
    public Map<String, Object> getPlaylistItems(String playlistId, int maxResults) {

        String url = normalizeBaseUrl() + "playlistItems"
                        + "?part=contentDetails"
                        + "&playlistId=" + playlistId
                        + "&maxResults=" + maxResults
                        + "&key=" + apiKey;

        log.info("search url: {}", url);
        return restTemplate.getForObject(url, Map.class);
    }

    /**
     * videos.list - 여러 영상의 상세 정보 조회
     */
    public Map<String, Object> getVideoDetails(List<String> videoIds) {

        if (videoIds == null || videoIds.isEmpty()) {
            return Map.of();
        }

        String joinedIds = String.join(",", videoIds);
        String url = normalizeBaseUrl() + "videos"
                        + "?part=snippet,contentDetails,statistics"
                        + "&id=" + joinedIds
                        + "&key=" + apiKey;

        log.info("video url: {}", url);

        return restTemplate.getForObject(url, Map.class);
    }
}
