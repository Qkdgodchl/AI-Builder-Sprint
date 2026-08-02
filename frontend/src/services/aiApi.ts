import { apiRequest } from './apiClient';

export interface RecommendedCard {
  opportunityId: number;
  title: string;
  region: string;
  category: string;
  badgeReward: string;
}

export interface AiRecommendResponseData {
  reply: string;
  recommendedCards: RecommendedCard[];
}

export interface ChatMessageItem {
  id: number;
  sender: 'USER' | 'ASSISTANT';
  message: string;
  recommendedMissionsJson?: string;
  createdAt: string;
}

const AI_API_BASE_URL = '/api/ai';

/**
 * Upstage Solar LLM 챗봇 추천 대화 API 호출
 */
export const sendAiMessage = async (userInput: string): Promise<AiRecommendResponseData> => {
  return apiRequest<AiRecommendResponseData>(`${AI_API_BASE_URL}/recommend`, {
    method: 'POST',
    body: JSON.stringify({ userInput }),
  });
};

/**
 * 대화 히스토리 목록 조회 API 호출
 */
export const fetchAiHistory = async (): Promise<ChatMessageItem[]> => {
  try {
    return await apiRequest<ChatMessageItem[]>(`${AI_API_BASE_URL}/messages`);
  } catch (error) {
    console.error('AI 히스토리 조회 오류:', error);
    return [];
  }
};

/**
 * AI 대화 히스토리 삭제 (초기화) API 호출
 */
export const clearAiHistory = async (): Promise<boolean> => {
  try {
    await apiRequest<string>(`${AI_API_BASE_URL}/messages`, {
      method: 'DELETE',
    });
    return true;
  } catch (error) {
    console.error('AI 히스토리 삭제 오류:', error);
    return false;
  }
};
