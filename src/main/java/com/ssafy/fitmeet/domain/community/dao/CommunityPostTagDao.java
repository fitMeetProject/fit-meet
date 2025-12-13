package com.ssafy.fitmeet.domain.community.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommunityPostTagDao {

    void insert(@Param("postId") Long postId, @Param("tagId") Long tagId);

    void deleteByPostId(@Param("postId") Long postId);

    // postId 기준 태그 이름 목록 조회
    List<String> findTagsByPostId(@Param("postId") Long postId);
}
