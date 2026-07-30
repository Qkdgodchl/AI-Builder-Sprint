import type { SessionUser } from '../types';
import {
  apiRequest,
  clearTokens,
  getRefreshToken,
  saveTokens,
} from './apiClient';

interface AuthResponse {
  userId: number;
  email: string;
  nickname: string;
  roles: string[];
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

const toSession = (response: AuthResponse): SessionUser => ({
  id: response.userId,
  email: response.email,
  nickname: response.nickname,
  role: response.roles.includes('OPERATOR')
    ? 'OPERATOR'
    : response.roles.includes('CENTER_MANAGER')
      ? 'CENTER_MANAGER'
      : 'USER',
  roles: response.roles,
});

const acceptAuth = (response: AuthResponse) => {
  saveTokens(response.accessToken, response.refreshToken);
  return toSession(response);
};

export const signup = async (input: {
  email: string;
  password: string;
  name: string;
  nickname: string;
  privacyConsent: boolean;
}): Promise<SessionUser> => {
  const response = await apiRequest<AuthResponse>('/api/v1/auth/signup', {
    method: 'POST',
    body: JSON.stringify(input),
  });
  return acceptAuth(response);
};

export const login = async (email: string, password: string): Promise<SessionUser> => {
  const response = await apiRequest<AuthResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
  return acceptAuth(response);
};

export const logout = async (): Promise<void> => {
  try {
    await apiRequest<void>('/api/v1/auth/logout', {
      method: 'POST',
      body: JSON.stringify({ refreshToken: getRefreshToken() }),
    });
  } finally {
    clearTokens();
  }
};

export interface UserProfile {
  id: number;
  email: string;
  nickname: string;
  name: string;
  phone?: string;
  birthDate?: string;
  region?: string;
  roles: string[];
  interests: string[];
  accountStatus: string;
  temperature: number;
  createdAt: string;
}

export const fetchMyProfile = () =>
  apiRequest<UserProfile>('/api/v1/users/me');

export const updateMyProfile = (payload: {
  nickname?: string;
  phone?: string;
  region?: string;
  interests?: string[];
}) =>
  apiRequest<UserProfile>('/api/v1/users/me', {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
