const API_ORIGIN = 'http://localhost:8080';

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
