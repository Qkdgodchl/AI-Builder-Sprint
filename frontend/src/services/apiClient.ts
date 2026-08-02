export const API_ORIGIN = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  message?: string;
}

export const getAccessToken = () => localStorage.getItem('pixel-care-access-token');
export const getRefreshToken = () => localStorage.getItem('pixel-care-refresh-token');

export const saveTokens = (accessToken: string, refreshToken: string) => {
  localStorage.setItem('pixel-care-access-token', accessToken);
  localStorage.setItem('pixel-care-refresh-token', refreshToken);
};

export const clearTokens = () => {
  localStorage.removeItem('pixel-care-access-token');
  localStorage.removeItem('pixel-care-refresh-token');
};

/** 토큰이 만료돼 세션이 끊겼음을 앱 전역에 알리는 이벤트. */
export const SESSION_EXPIRED_EVENT = 'pixel-care:session-expired';

export const apiRequest = async <T>(
  path: string,
  init: RequestInit = {},
): Promise<T> => {
  const headers = new Headers(init.headers);
  const token = getAccessToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);
  if (init.body && !(init.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(`${API_ORIGIN}${path}`, { ...init, headers });

  // 토큰이 만료되면 화면마다 빈 목록이 뜨는 대신 로그아웃 상태로 되돌리고 알린다.
  // 로그인·회원가입 요청의 401은 자격 증명 오류이므로 그대로 흘려보낸다.
  if (response.status === 401 && !path.startsWith('/api/v1/auth/')) {
    clearTokens();
    localStorage.removeItem('pixel-care-user');
    window.dispatchEvent(new CustomEvent(SESSION_EXPIRED_EVENT));
    throw new Error('로그인이 만료되었습니다. 다시 로그인해 주세요.');
  }

  if (!response.ok) {
    let message = `요청에 실패했습니다. (${response.status})`;
    try {
      const body = await response.json();
      message = body.message || body.error?.message || message;
    } catch {
      // JSON이 아닌 오류 응답은 기본 메시지를 사용한다.
    }
    throw new Error(message);
  }
  if (response.status === 204) return undefined as T;
  const envelope: ApiEnvelope<T> = await response.json();
  return envelope.data;
};
