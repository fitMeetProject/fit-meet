package com.ssafy.fitmeet.domain.community.service;

import com.ssafy.fitmeet.domain.community.dto.CommunityDto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CommunityService {

    Long createPost(Long userId, PostCreateRequest req, MultipartFile image);

    PostDetailResponse getPostDetail(Long currentUserId, Long postId, boolean increaseView);

    void updatePost(Long userId, Long postId, PostUpdateRequest req, MultipartFile image);

    void softDeletePost(Long userId, Long postId);

    void restorePost(Long userId, Long postId);

    void likePost(Long userId, Long postId);

    void unlikePost(Long userId, Long postId);

    Long addComment(Long userId, Long postId, CommentCreateRequest req);

    void deleteComment(Long userId, Long commentId);

    List<CommentResponse> getComments(Long currentUserId, Long postId);

    PostPageResponse searchPosts(Long currentUserId, PostSearchCondition cond);

    PostPageResponse getMyLikedPosts(Long userId, Integer page, Integer size);

    PostPageResponse getMyPosts(Long userId, Integer page, Integer size);

    List<TagSummaryResponse> getTopTags(int limit);
}
