import { API_ORIGIN } from './apiClient';

export interface Author {
  id: number;
  nickname: string;
  badge: string;
}

export interface PostItem {
  id: number;
  authorUserId?: number;
  author: Author | string;
  category: string;
  title: string;
  contentSnippet?: string;
  content?: string;
  imageUrl?: string;
  likeCount?: number;
  commentCount?: number;
  viewCount?: number;
  likes?: number;
  views?: number;
  createdAt: string;
}

const API_BASE_URL = `${API_ORIGIN}/api/posts`;

const normalizePost = (post: any): PostItem => ({
  ...post,
  author: post.author ?? post.authorNickname ?? '작성자 미등록',
});

/**
 * 백엔드 REST API에서 커뮤니티 게시글 목록 조회
 */
export const fetchPosts = async (category?: string, sort: string = 'latest'): Promise<PostItem[]> => {
  try {
    let url = `${API_BASE_URL}?sort=${sort}`;
    if (category && category !== 'ALL') {
      url += `&category=${encodeURIComponent(category)}`;
    }
    
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`API 응답 오류: ${response.status}`);
    }

    const result = await response.json();
    
    if (result.success && result.data) {
      if (Array.isArray(result.data.content)) {
        return result.data.content.map(normalizePost);
      }
      if (Array.isArray(result.data)) {
        return result.data.map(normalizePost);
      }
    }
    
    if (Array.isArray(result)) {
      return result;
    }

    return [];
  } catch (error) {
    console.error('커뮤니티 게시글 목록 조회 실패:', error);
    return [];
  }
};

/**
 * 게시글 상세 조회. 백엔드에서 조회수가 한 번 증가한다.
 */
export const fetchPost = async (id: number): Promise<PostItem> => {
  const response = await fetch(`${API_BASE_URL}/${id}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(`게시글 상세 조회 실패: ${response.status}`);
  }

  const result = await response.json();
  if (!result.success || !result.data) {
    throw new Error('게시글 상세 조회 응답이 올바르지 않습니다.');
  }

  return normalizePost(result.data);
};

export const fetchMyPosts = async (): Promise<PostItem[]> => {
  const token = localStorage.getItem('pixel-care-access-token');
  const response = await fetch(`${API_BASE_URL}/me?size=100`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  if (!response.ok) throw new Error(`나의 글 조회 실패: ${response.status}`);
  const result = await response.json();
  const content = result?.data?.content;
  return Array.isArray(content) ? content.map(normalizePost) : [];
};

export interface CreatePostPayload {
  title: string;
  content: string;
  category?: string;
  author?: string;
  imageUrl?: string;
}

/**
 * 게시글 작성 API 호출
 */
export const createPost = async (payload: CreatePostPayload): Promise<PostItem | null> => {
  try {
    const response = await fetch(API_BASE_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(localStorage.getItem('pixel-care-access-token')
          ? { Authorization: `Bearer ${localStorage.getItem('pixel-care-access-token')}` }
          : {}),
      },
      body: JSON.stringify({
        title: payload.title,
        content: payload.content,
        category: payload.category || 'FREE',
        authorNickname: payload.author || '부산 잇다 이웃',
        imageUrl: payload.imageUrl || '',
      }),
    });

    if (!response.ok) {
      throw new Error(`게시글 작성 실패: ${response.status}`);
    }

    const result = await response.json();
    return result.data ? normalizePost(result.data) : null;
  } catch (error) {
    console.error('게시글 작성 오류:', error);
    return null;
  }
};

/**
 * 게시글 소프트 삭제 API 호출
 */
export const deletePost = async (id: number): Promise<boolean> => {
  try {
    const response = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        ...(localStorage.getItem('pixel-care-access-token')
          ? { Authorization: `Bearer ${localStorage.getItem('pixel-care-access-token')}` }
          : {}),
      },
    });

    if (!response.ok) {
      throw new Error(`게시글 삭제 실패: ${response.status}`);
    }

    const result = await response.json();
    return result.success ?? true;
  } catch (error) {
    console.error('게시글 삭제 오류:', error);
    return false;
  }
};

/**
 * 게시글 좋아요 토글 API 호출
 */
export const likePost = async (id: number): Promise<{ postId: number; isLiked: boolean; likeCount: number }> => {
  const token = localStorage.getItem('pixel-care-access-token');
  const response = await fetch(`${API_BASE_URL}/${id}/like`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  if (!response.ok) {
    throw new Error(`좋아요 처리 실패: ${response.status}`);
  }

  const result = await response.json();
  return result.data;
};

export interface CommentItem {
  id: number;
  postId: number;
  content: string;
  authorNickname: string;
  authorBadge: string;
  createdAt: string;
}

/**
 * 특정 게시글의 댓글 목록 조회
 */
export const fetchComments = async (postId: number): Promise<CommentItem[]> => {
  try {
    const response = await fetch(`${API_BASE_URL}/${postId}/comments`);
    if (!response.ok) throw new Error(`댓글 조회 실패: ${response.status}`);
    const result = await response.json();
    return result.success && Array.isArray(result.data) ? result.data : [];
  } catch (error) {
    console.error('댓글 목록 조회 오류:', error);
    return [];
  }
};

/**
 * 댓글 작성
 */
export const createComment = async (
  postId: number,
  content: string,
  authorNickname?: string,
  authorBadge?: string
): Promise<CommentItem | null> => {
  try {
    const token = localStorage.getItem('pixel-care-access-token');
    const response = await fetch(`${API_BASE_URL}/${postId}/comments`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: JSON.stringify({ content, authorNickname, authorBadge }),
    });
    if (!response.ok) throw new Error(`댓글 작성 실패: ${response.status}`);
    const result = await response.json();
    return result.data || null;
  } catch (error) {
    console.error('댓글 작성 오류:', error);
    return null;
  }
};

/**
 * 댓글 삭제
 */
export const deleteComment = async (commentId: number): Promise<boolean> => {
  try {
    const token = localStorage.getItem('pixel-care-access-token');
    const response = await fetch(`${API_ORIGIN}/api/comments/${commentId}`, {
      method: 'DELETE',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });
    if (!response.ok) throw new Error(`댓글 삭제 실패: ${response.status}`);
    const result = await response.json();
    return result.success ?? true;
  } catch (error) {
    console.error('댓글 삭제 오류:', error);
    return false;
  }
};
