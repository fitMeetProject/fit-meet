package com.ssafy.fitmeet.domain.community.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommunityPostVideoDao {
    void insert(@Param("postId") Long postId, @Param("videoId") Long videoId);

    void deleteByPostId(@Param("postId") Long postId);

    List<Long> findVideoIdsByPostId(@Param("postId") Long postId);

    List<WorkoutVideoJoinRow> findVideoCardsByPostId(@Param("postId") Long postId);

    record WorkoutVideoJoinRow(
            Long id,
            String youtubeVideoId,
            String title,
            String thumbnailUrl,
            Integer durationSec
    ) {}
}
