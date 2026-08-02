import { apiRequest } from './apiClient';

export interface GoodNewsItem {
  id: string;
  region: string;
  title: string;
  summary: string;
  source: string;
  url: string;
  publishedAt: string;
}

export interface GoodNewsResponse {
  region: string;
  items: GoodNewsItem[];
  updatedAt: string;
  provider: string;
  stale: boolean;
  message?: string;
}

/** 백엔드가 뉴스를 모아둔 지역 목록. 화면 표기도 이 값을 그대로 쓴다. */
export const NEWS_REGIONS = [
  '전국', '서울', '부산', '대구', '광주', '인천', '대전', '울산', '세종',
  '경기', '강원', '충북', '충남', '전북', '전남', '경북', '경남', '제주',
] as const;

const REGION_ALIASES: Record<string, string> = {
  서울특별시: '서울', 부산광역시: '부산', 대구광역시: '대구', 광주광역시: '광주',
  인천광역시: '인천', 대전광역시: '대전', 울산광역시: '울산', 세종특별자치시: '세종',
  경기도: '경기', 강원특별자치도: '강원', 강원도: '강원',
  충청북도: '충북', 충청남도: '충남',
  전북특별자치도: '전북', 전라북도: '전북', 전라남도: '전남',
  경상북도: '경북', 경상남도: '경남',
  제주특별자치도: '제주', 제주도: '제주',
};

// 긴 이름을 먼저 본다. "서울특별시 종로구 세종로"가 세종으로 빠지면 안 된다.
const REGION_LOOKUP_ORDER = [...Object.keys(REGION_ALIASES), ...NEWS_REGIONS]
  .sort((a, b) => b.length - a.length);

/**
 * 프로필 활동 지역은 자유 입력이라 "부산광역시 해운대구" 같은 값이 들어온다.
 * 뉴스가 준비된 지역으로 맞추고, 못 찾으면 전국으로 둔다. 백엔드와 같은 규칙이다.
 */
export const normalizeNewsRegion = (value?: string | null): string => {
  const text = value?.trim();
  if (!text) return '전국';
  const matched = REGION_LOOKUP_ORDER.find((name) => text.includes(name));
  return matched ? REGION_ALIASES[matched] ?? matched : '전국';
};

export const fetchGoodNews = (region = '전국', limit = 8) =>
  apiRequest<GoodNewsResponse>(
    `/api/v1/good-news?region=${encodeURIComponent(region)}&limit=${limit}`,
  );
