export interface PostItem {
  id: number;
  title: string;
  content: string;
  author: string;
  category: string;
  likes: number;
  views: number;
  createdAt: string;
}

const API_BASE_URL = 'http://localhost:8080/api/posts';

/**
 * Fetch list of community posts from API
 */
export const fetchPosts = async (category?: string): Promise<PostItem[]> => {
  try {
    const url = category && category !== 'ALL' ? `${API_BASE_URL}?category=${encodeURIComponent(category)}` : API_BASE_URL;
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`API response status: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Failed to fetch community posts:', error);
    return [];
  }
};

export interface CreatePostPayload {
  title: string;
  content: string;
  author: string;
  category?: string;
}

/**
 * Create a new community post via API
 */
export const createPost = async (payload: CreatePostPayload): Promise<PostItem> => {
  const response = await fetch(API_BASE_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      title: payload.title,
      content: payload.content,
      author: payload.author || '익명 픽셀용사',
      category: payload.category || 'GENERAL',
    }),
  });

  if (!response.ok) {
    throw new Error(`Failed to create post: ${response.status}`);
  }

  return await response.json();
};

/**
 * Like a community post via API
 */
export const likePost = async (id: number): Promise<PostItem> => {
  const response = await fetch(`${API_BASE_URL}/${id}/like`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
  });

  if (!response.ok) {
    throw new Error(`Failed to like post: ${response.status}`);
  }

  return await response.json();
};
