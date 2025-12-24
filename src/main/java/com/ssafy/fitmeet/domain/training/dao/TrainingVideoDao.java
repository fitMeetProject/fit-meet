package com.ssafy.fitmeet.domain.training.dao;

import com.ssafy.fitmeet.domain.training.entity.TrainingVideo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TrainingVideoDao {

    List<Long> findAllIds();

    List<TrainingVideo> findRecommended(@Param("category") String category, @Param("limit") int limit);

    List<TrainingVideo> findVideos(@Param("category") String category, @Param("level") String level, @Param("shortVideo") Boolean shortVideo, @Param("sort") String sort, @Param("offset") int offset, @Param("size") int size);

    long countVideos(@Param("category") String category, @Param("level") String level, @Param("shortVideo") Boolean shortVideo, @Param("sort") String sort);

    TrainingVideo findById(@Param("id") Long id);

    TrainingVideo findByYoutubeVideoId(@Param("youtubeVideoId") String youtubeVideoId);

    int increaseViewCount(@Param("id") Long id);

    int insertVideo(TrainingVideo video);

    int updateVideoFromYoutube(TrainingVideo video);

    int increaseLikeCount(@Param("id") Long id);
    int decreaseLikeCount(@Param("id") Long id);

    Integer isLiked(@Param("userId") Long userId, @Param("videoId") Long videoId);
    int insertLike(@Param("userId") Long userId, @Param("videoId") Long videoId);
    int deleteLike(@Param("userId") Long userId, @Param("videoId") Long videoId);

    // Seeding 용
    int increaseViewCountBy(@Param("id") Long id, @Param("delta") long delta);
    int setViewCount(@Param("id") Long id, @Param("value") long value);

}
