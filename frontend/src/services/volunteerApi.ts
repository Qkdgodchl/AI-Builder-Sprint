import type { VolunteerItem } from '../types';
import { apiRequest } from './apiClient';

const API_BASE_URL = '/api/v1/opportunities';

interface OpportunityItem {
  id: number;
  organizationName: string;
  type: string;
  category?: string;
  title: string;
  summary?: string;
  description?: string;
  region?: string;
  location?: string;
  targetAmount?: number;
  currentAmount?: number;
  recruitmentCapacity?: number;
  applicantCount?: number;
  recruitmentEndDateTime?: string;
  activityStartDateTime?: string;
  activityEndDateTime?: string;
  requiredDocuments?: Array<{ code: string; name: string; description?: string; required: boolean }>;
  status: string;
  createdByUserId?: number;
}

const PAGE_SIZE = 100;

/**
 * Fetch list of volunteers directly from Spring Boot REST API.
 * 목록 화면이 지역·구분별 건수를 클라이언트에서 세므로 모든 페이지를 받아온다.
 * 한 페이지만 받으면 서버 기본값(20건)을 넘는 공고가 목록과 필터 숫자에서 빠진다.
 */
export const fetchVolunteers = async (category?: string): Promise<VolunteerItem[]> => {
  try {
    const items: OpportunityItem[] = [];
    let pageNumber = 0;
    let totalPages = 1;

    do {
      const params = new URLSearchParams({
        page: String(pageNumber),
        size: String(PAGE_SIZE),
      });
      if (category) params.set('type', category);
      const page = await apiRequest<{ items: OpportunityItem[]; totalPages: number }>(
        `${API_BASE_URL}?${params.toString()}`,
      );
      items.push(...page.items);
      totalPages = page.totalPages || 1;
      pageNumber += 1;
    } while (pageNumber < totalPages);

    return items.map((item) => ({
      id: item.id,
      title: item.title,
      category: mapCategory(item),
      location: item.location || item.region || '전국',
      organizer: item.organizationName,
      targetAmount: item.targetAmount,
      currentAmount: item.currentAmount,
      tags: [categoryLabel(item), item.region || '전국'],
      createdByUserId: item.createdByUserId,
      summary: item.summary,
      description: item.description,
      region: item.region,
      recruitmentCapacity: item.recruitmentCapacity,
      applicantCount: item.applicantCount,
      recruitmentEndDateTime: item.recruitmentEndDateTime,
      activityStartDateTime: item.activityStartDateTime,
      activityEndDateTime: item.activityEndDateTime,
      requiredDocuments: item.requiredDocuments,
    }));
  } catch (error) {
    console.error('Failed to fetch from Volunteer API:', error);
    return [];
  }
};

export const deleteOpportunity = (id: number) =>
  apiRequest<void>(`/api/v1/opportunities/${id}`, { method: 'DELETE' });

const mapCategory = (item: OpportunityItem): VolunteerItem['category'] => {
  if (item.type === 'VOLUNTEER') return 'VOLUNTEER';
  if (item.type === 'HOMETOWN_DONATION') return 'HOMETOWN';
  if (item.type === 'LEGACY_DONATION') return 'LEGACY';
  if (item.type === 'CULTURAL_HERITAGE_DONATION') {
    return item.category === 'UNESCO' ? 'UNESCO' : 'HERITAGE';
  }
  return 'GENERAL';
};

const categoryLabel = (item: OpportunityItem) => {
  const mapped = mapCategory(item);
  const labels: Record<VolunteerItem['category'], string> = {
    VOLUNTEER: '봉사',
    DONATION: '기부',
    GENERAL: '일반기부',
    LEGACY: '유산기부',
    UNESCO: '유네스코 후원',
    HERITAGE: '문화유산 후원',
    HOMETOWN: '고향사랑기부',
  };
  return labels[mapped];
};

export interface CreateVolunteerPayload {
  title: string;
  location: string;
  organizer: string;
  category?: 'VOLUNTEER' | 'DONATION';
  tags?: string[];
  targetAmount?: number;
  currentAmount?: number;
}

/**
 * Register a new volunteer item via Spring Boot REST API
 */
export const createVolunteer = async (payload: CreateVolunteerPayload): Promise<VolunteerItem> => {
  const newItemData = {
    title: payload.title,
    location: payload.location,
    organizer: payload.organizer,
    category: payload.category || 'VOLUNTEER',
    tags: payload.tags || ['신규 등록', '봉사'],
    targetAmount: payload.targetAmount,
    currentAmount: payload.currentAmount || 0,
  };

  const response = await fetch(API_BASE_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(newItemData),
  });

  if (!response.ok) {
    throw new Error(`Failed to create volunteer via API: ${response.status}`);
  }

  const createdItem: VolunteerItem = await response.json();
  return createdItem;
};
