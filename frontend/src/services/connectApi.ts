import { apiRequest } from './apiClient';

export interface ConnectRequestItem {
  publicId: string;
  title: string;
  content: string;
  category: 'VOLUNTEER' | 'DONATION';
  region?: string;
  /** AI 추천이 비어 넘어온 요청인지, 직접 쓴 요청인지 */
  origin: 'AI' | 'DIRECT';
  status: 'OPEN' | 'REVIEWING' | 'FULFILLED';
  supportCount: number;
  supportedByMe: boolean;
  requesterNickname: string;
  mine: boolean;
  /** 이 요청을 맡은 센터가 내 센터인지 */
  handledByMe: boolean;
  /** 지금 내가 맡거나 프로그램을 연결할 수 있는지 (서버가 판단) */
  canHandle: boolean;
  handledOrganizationName?: string;
  handledOpportunityId?: number;
  handledMessage?: string;
  createdAt: string;
}

export const fetchConnectRequests = (category?: string) =>
  apiRequest<ConnectRequestItem[]>(
    `/api/v1/connect/requests${category ? `?category=${encodeURIComponent(category)}` : ''}`,
  );

export const createConnectRequest = (payload: {
  title: string;
  content: string;
  category: string;
  region?: string;
  origin?: 'AI' | 'DIRECT';
}) =>
  apiRequest<ConnectRequestItem>('/api/v1/connect/requests', {
    method: 'POST',
    body: JSON.stringify(payload),
  });

export const toggleConnectSupport = (publicId: string) =>
  apiRequest<ConnectRequestItem>(`/api/v1/connect/requests/${publicId}/support`, {
    method: 'POST',
  });

export const handleConnectRequest = (
  publicId: string,
  payload: { message?: string; opportunityId?: number },
) =>
  apiRequest<ConnectRequestItem>(`/api/v1/connect/requests/${publicId}/handle`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
