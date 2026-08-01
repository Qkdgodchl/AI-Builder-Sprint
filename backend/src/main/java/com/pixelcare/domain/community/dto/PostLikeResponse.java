package com.pixelcare.domain.community.dto;

public record PostLikeResponse(Long postId, boolean isLiked, int likeCount) {}
