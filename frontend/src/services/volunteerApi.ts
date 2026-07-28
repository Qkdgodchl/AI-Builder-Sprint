import type { VolunteerItem } from '../types';

const API_BASE_URL = 'http://localhost:8080/api/volunteers';

/**
 * Fetch list of volunteers directly from Spring Boot REST API
 */
export const fetchVolunteers = async (category?: string): Promise<VolunteerItem[]> => {
  try {
    const url = category ? `${API_BASE_URL}?category=${encodeURIComponent(category)}` : API_BASE_URL;
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`API response status: ${response.status}`);
    }

    const data: VolunteerItem[] = await response.json();
    return data;
  } catch (error) {
    console.error('Failed to fetch from Volunteer API:', error);
    return [];
  }
};

export interface CreateVolunteerPayload {
  title: string;
  location: string;
  organizer: string;
  category?: 'VOLUNTEER' | 'DONATION';
  tags?: string[];
  link1365?: string;
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
    tags: payload.tags || ['신규 등록', '1365 연동'],
    link1365: payload.link1365 || 'https://www.1365.go.kr',
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
