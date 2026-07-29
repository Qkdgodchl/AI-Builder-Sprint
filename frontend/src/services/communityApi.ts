export interface Author {
  id: number;
  nickname: string;
  badge: string;
}

export interface PostItem {
  id: number;
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

const API_BASE_URL = 'http://localhost:8080/api/posts';

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
    
    // 백엔드 ApiResponse { success: true, data: { content: [...] } } 언패킹
    if (result.success && result.data) {
      if (Array.isArray(result.data.content)) {
        return result.data.content;
      }
      if (Array.isArray(result.data)) {
        return result.data;
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
      },
      body: JSON.stringify({
        title: payload.title,
        content: payload.content,
        category: payload.category || 'FREE',
        imageUrl: payload.imageUrl || '',
      }),
    });

    if (!response.ok) {
      throw new Error(`게시글 작성 실패: ${response.status}`);
    }

    const result = await response.json();
    return result.data || null;
  } catch (error) {
    console.error('게시글 작성 오류:', error);
    return null;
  }
};

/**
 * 게시글 좋아요 토글 API 호출
 */
export const likePost = async (id: number): Promise<{ postId: number; isLiked: boolean; likeCount: number }> => {
  const response = await fetch(`${API_BASE_URL}/${id}/like`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(`좋아요 처리 실패: ${response.status}`);
  }

  const result = await response.json();
  return result.data;
};
