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

const AI_API_BASE_URL = 'http://localhost:8080/api/ai';

/**
 * Upstage Solar LLM 챗봇 추천 대화 API 호출
 */
export const sendAiMessage = async (userInput: string): Promise<AiRecommendResponseData> => {
  const response = await fetch(`${AI_API_BASE_URL}/recommend`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ userInput }),
  });

  if (!response.ok) {
    throw new Error(`AI 추천 요청 실패: ${response.status}`);
  }

  const result = await response.json();
  return result.data;
};

/**
 * 대화 히스토리 목록 조회 API 호출
 */
export const fetchAiHistory = async (): Promise<ChatMessageItem[]> => {
  try {
    const response = await fetch(`${AI_API_BASE_URL}/messages`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`대화 히스토리 조회 실패: ${response.status}`);
    }

    const result = await response.json();
    return result.data || [];
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
    const response = await fetch(`${AI_API_BASE_URL}/messages`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`AI 히스토리 삭제 실패: ${response.status}`);
    }

    const result = await response.json();
    return result.success ?? true;
  } catch (error) {
    console.error('AI 히스토리 삭제 오류:', error);
    return false;
  }
};
