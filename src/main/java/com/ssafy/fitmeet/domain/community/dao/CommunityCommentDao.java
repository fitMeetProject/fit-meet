package com.ssafy.fitmeet.domain.community.dao;

import com.ssafy.fitmeet.domain.community.entity.CommunityComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommunityCommentDao {
    void insert(CommunityComment comment);

    int softDelete(@Param("commentId") Long commentId, @Param("userId") Long userId);

    List<CommunityComment> findByPostId(@Param("postId") Long postId);

    // comment_count 감소 위해 commentId → postId 알아야 하므로 추가
    CommunityComment findById(@Param("commentId") Long commentId);
}

