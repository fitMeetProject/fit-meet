package com.ssafy.fitmeet.domain.community.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface CommunityLikeDao {

    int insert(@Param("postId") Long postId, @Param("userId") Long userId, @Param("createdAt") LocalDateTime createdAt);

    int delete(@Param("postId") Long postId, @Param("userId") Long userId);

    Integer exists(@Param("postId") Long postId, @Param("userId") Long userId);

}

