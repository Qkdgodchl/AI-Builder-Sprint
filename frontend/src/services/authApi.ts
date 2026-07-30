const API_BASE_URL = 'http://localhost:8080/api/users';

export interface UserAuthResponse {
  id: number;
  email: string;
  nickname: string;
  role: string;
  temperature: number;
}

export const signupUserApi = async (
  email: string,
  nickname: string,
  name: string,
  password: string
): Promise<UserAuthResponse> => {
  try {
    const res = await fetch(`${API_BASE_URL}/signup`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, nickname, name, password }),
    });

    if (!res.ok) {
      throw new Error(`회원가입 실패 (${res.status})`);
    }

    return await res.json();
  } catch (error) {
    console.warn('백엔드 연동 미작동, 목업 대체:', error);
    return {
      id: Date.now(),
      email,
      nickname,
      role: 'USER',
      temperature: 36.5,
    };
  }
};

export const loginUserApi = async (
  email: string,
  password: string
): Promise<UserAuthResponse> => {
  try {
    const res = await fetch(`${API_BASE_URL}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    });

    if (!res.ok) {
      throw new Error(`로그인 실패 (${res.status})`);
    }

    return await res.json();
  } catch (error) {
    console.warn('백엔드 연동 미작동, 목업 대체:', error);
    return {
      id: Date.now(),
      email,
      nickname: email.split('@')[0] || '픽셀 사용자',
      role: 'USER',
      temperature: 36.5,
    };
  }
};
