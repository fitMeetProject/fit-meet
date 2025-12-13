package com.ssafy.fitmeet.domain.community.dao;

import com.ssafy.fitmeet.domain.community.dto.CommunityDto.PostSearchCondition;
import com.ssafy.fitmeet.domain.community.entity.CommunityPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommunityPostDao {

    void insert(CommunityPost post);

    int update(CommunityPost post);

    CommunityPost findById(@Param("id") Long id);

    int softDelete(@Param("postId") Long postId, @Param("userId") Long userId);

    int restore(@Param("postId") Long postId, @Param("userId") Long userId);

    void increaseViewCount(@Param("postId") Long postId);

    void updateLikeCount(@Param("postId") Long postId, @Param("delta") int delta);

    void updateCommentCount(@Param("postId") Long postId, @Param("delta") int delta);

    List<CommunityPost> search(PostSearchCondition condition);

    List<CommunityPost> findLikedPostsByUser(@Param("userId") Long userId);
}
