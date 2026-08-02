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

export const fetchGoodNews = (region = '전국', limit = 8) =>
  apiRequest<GoodNewsResponse>(
    `/api/v1/good-news?region=${encodeURIComponent(region)}&limit=${limit}`,
  );
