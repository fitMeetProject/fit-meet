package com.ssafy.fitmeet.domain.community.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommunityLikeDao {

    int insert(@Param("postId") Long postId, @Param("userId") Long userId);

    int delete(@Param("postId") Long postId, @Param("userId") Long userId);

    Boolean exists(@Param("postId") Long postId, @Param("userId") Long userId);
}

