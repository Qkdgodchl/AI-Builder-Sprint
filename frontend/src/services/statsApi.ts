import { apiRequest } from './apiClient';

export interface PlatformStats {
  averageWarmth: number;
  pledgedDonation: number;
  volunteerHours: number;
  signedCommitments: number;
  activeMembers: number;
}

/** 홈 상단 누적 현황. 로그인 없이 조회한다. */
export const fetchPlatformStats = () =>
  apiRequest<PlatformStats>('/api/v1/stats/summary');
