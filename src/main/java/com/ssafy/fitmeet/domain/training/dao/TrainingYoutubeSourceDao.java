package com.ssafy.fitmeet.domain.training.dao;

import com.ssafy.fitmeet.domain.training.entity.TrainingYoutubeSource;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TrainingYoutubeSourceDao {

    List<TrainingYoutubeSource> findActiveSources();
}
