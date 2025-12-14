package com.ssafy.fitmeet.domain.training.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * YouTube → training_video 자동 동기화 스케줄러
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class TrainingYoutubeScheduler {

    private final TrainingYoutubeSyncService syncService;

    /**
     * 매일 새벽 3시에 전체 소스 동기화 ( 서버가 켜져 있다면 )
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void syncDaily() {
        log.info("[Scheduler] 매일 새벽 YouTube Sync 실행 시작");
        syncService.syncAllSources();
        log.info("[Scheduler] 매일 새벽 YouTube Sync 실행 종료");
    }
}
