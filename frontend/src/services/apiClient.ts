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

/**
 * 액세스 토큰이 만료되면 리프레시 토큰으로 한 번 재발급받는다.
 * 목록 화면은 요청 수십 개를 동시에 보내므로, 동시에 401이 떨어져도
 * 재발급은 한 번만 타도록 진행 중인 약속을 공유한다.
 * (리프레시 토큰은 재사용이 막혀 있어 두 번 타면 그 자체로 세션이 끊긴다.)
 */
let refreshInFlight: Promise<boolean> | null = null;

const tryRefreshTokens = async (): Promise<boolean> => {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return false;
  try {
    const response = await fetch(`${API_ORIGIN}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });
    if (!response.ok) return false;
    const envelope = await response.json();
    const data = envelope?.data ?? envelope;
    if (!data?.accessToken) return false;
    saveTokens(data.accessToken, data.refreshToken ?? refreshToken);
    return true;
  } catch {
    return false;
  }
};

export const apiRequest = async <T>(
  path: string,
  init: RequestInit = {},
): Promise<T> => {
  const doFetch = () => {
    const headers = new Headers(init.headers);
    const token = getAccessToken();
    if (token) headers.set('Authorization', `Bearer ${token}`);
    if (init.body && !(init.body instanceof FormData)) {
      headers.set('Content-Type', 'application/json');
    }
    return fetch(`${API_ORIGIN}${path}`, { ...init, headers });
  };

  let response = await doFetch();

  // 액세스 토큰 만료(401)면 바로 로그아웃시키지 않고 재발급을 한 번 시도한 뒤 재요청한다.
  // 로그인·회원가입 요청의 401은 자격 증명 오류이므로 그대로 흘려보낸다.
  if (response.status === 401 && !path.startsWith('/api/v1/auth/')) {
    if (!refreshInFlight) {
      refreshInFlight = tryRefreshTokens().finally(() => {
        refreshInFlight = null;
      });
    }
    const refreshed = await refreshInFlight;
    if (refreshed) {
      response = await doFetch();
    }
  }

  // 재발급까지 실패했을 때만 세션을 정리하고 앱 전역에 알린다.
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
