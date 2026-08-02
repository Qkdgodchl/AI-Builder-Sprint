import { apiRequest } from './apiClient';

export interface JournalPhoto {
  fileId: number;
  originalName: string;
}

export interface JournalNote {
  publicId: string;
  authorType: 'CENTER' | 'USER';
  visibility: 'SHARED' | 'PRIVATE';
  authorName: string;
  activityDate: string;
  content?: string;
  photos: JournalPhoto[];
  createdAt: string;
}

export interface JournalEntry {
  applicationPublicId: string;
  opportunityId: number;
  opportunityTitle: string;
  organizationName: string;
  status: string;
  activityDate: string;
  notes: JournalNote[];
}

export const fetchMyJournal = () => apiRequest<JournalEntry[]>('/api/v1/me/journal');

export const addMyNote = (
  applicationPublicId: string,
  payload: { activityDate: string; content: string; fileIds?: number[]; shared: boolean },
) =>
  apiRequest<JournalNote[]>(`/api/v1/applications/${applicationPublicId}/notes`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });

/** 담당 센터가 참여자에게 남기는 사진·코멘트. */
export const addCenterNote = (
  applicationPublicId: string,
  payload: { activityDate: string; content: string; fileIds?: number[] },
) =>
  apiRequest<JournalNote[]>(`/api/v1/manager/applications/${applicationPublicId}/notes`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
