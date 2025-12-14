package com.ssafy.fitmeet.domain.training.dao;

import com.ssafy.fitmeet.domain.training.entity.TrainingVideoComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TrainingVideoCommentDao {

    List<TrainingVideoComment> findByVideoId(@Param("videoId") Long videoId);

    int insertComment(TrainingVideoComment comment);

    TrainingVideoComment findById(@Param("id") Long id);

    int softDelete(@Param("id") Long id);
}

