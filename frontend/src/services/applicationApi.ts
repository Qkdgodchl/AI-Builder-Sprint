import { apiRequest } from './apiClient';

export interface ApplicationResponse {
  publicId: string;
  opportunityId: number;
  opportunityTitle: string;
  opportunityType: string;
  organizationId: number;
  organizationName: string;
  applicantUserId: number;
  applicantName: string;
  applicantEmail: string;
  participationDate?: string;
  specialConditions?: string;
  status: string;
  submittedAt?: string;
  reviewReason?: string;
  commitment?: {
    publicId: string;
    status: string;
    title: string;
    renderedContent: string;
  };
  documents: Array<{
    code: string;
    name: string;
    description?: string;
    required: boolean;
    status: string;
  }>;
}

export const createApplication = (
  opportunityId: number,
  payload: {
    participationDate?: string;
    specialConditions?: string;
    privacyConsent: boolean;
    thirdPartyConsent: boolean;
    portraitConsent: boolean;
  },
) =>
  apiRequest<ApplicationResponse>(`/api/v1/opportunities/${opportunityId}/applications`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });

export const updateCommitment = (
  publicId: string,
  payload: {
    specialConditions?: string;
    privacyConsent: boolean;
    thirdPartyConsent: boolean;
    portraitConsent: boolean;
  },
) =>
  apiRequest(`/api/v1/commitments/${publicId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });

export const submitCommitment = (publicId: string) =>
  apiRequest(`/api/v1/commitments/${publicId}/submit-review`, { method: 'POST' });

export const fetchOpportunityApplications = (opportunityId: number) =>
  apiRequest<ApplicationResponse[]>(
    `/api/v1/manager/opportunities/${opportunityId}/applications`,
  );

export const fetchMyApplications = () =>
  apiRequest<ApplicationResponse[]>('/api/v1/applications/me');

export const approveApplication = (publicId: string) =>
  apiRequest<ApplicationResponse>(`/api/v1/manager/applications/${publicId}/approve`, {
    method: 'POST',
    body: JSON.stringify({}),
  });
