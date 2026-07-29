package com.pixelcare.domain.community.dto;

public class LikeToggleResponse {

    private Long postId;
    private boolean isLiked;
    private Integer likeCount;

    public LikeToggleResponse() {}

    public LikeToggleResponse(Long postId, boolean isLiked, Integer likeCount) {
        this.postId = postId;
        this.isLiked = isLiked;
        this.likeCount = likeCount;
    }

    public Long getPostId() { return postId; }
    public boolean isLiked() { return isLiked; }
    public Integer getLikeCount() { return likeCount; }
}
