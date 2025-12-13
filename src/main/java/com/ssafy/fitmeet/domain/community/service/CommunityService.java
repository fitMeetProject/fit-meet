package com.ssafy.fitmeet.domain.community.service;

import com.ssafy.fitmeet.domain.community.dto.CommunityDto.*;

import java.util.List;

public interface CommunityService {

    Long createPost(Long userId, PostCreateRequest request);

    void updatePost(Long userId, Long postId, PostUpdateRequest request);

    void softDeletePost(Long userId, Long postId);

    void restorePost(Long userId, Long postId);

    PostDetailResponse getPostDetail(Long currentUserId, Long postId);

    List<PostSummaryResponse> searchPosts(Long currentUserId, PostSearchCondition condition);

    void likePost(Long userId, Long postId);

    void unlikePost(Long userId, Long postId);

    List<PostSummaryResponse> getMyLikedPosts(Long userId);

    Long addComment(Long userId, Long postId, CommentCreateRequest request);

    List<CommentResponse> getComments(Long currentUserId, Long postId);

    void deleteComment(Long userId, Long commentId);

    List<TagSummaryResponse> getTopTags(int limit);
}
