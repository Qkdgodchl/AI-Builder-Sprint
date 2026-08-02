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
    commitmentType?: string;
    pledgeAmount?: number;
    pledgeFrequency?: string;
    renewalDueAt?: string;
    renewalStatus?: 'NOT_APPLICABLE' | 'SCHEDULED' | 'DUE';
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
    consultationId?: number;
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

export type CommitmentSummary = NonNullable<ApplicationResponse['commitment']>;

/** 월간·연간 정기 약정의 다음 갱신 주기를 연장한다. */
/**
 * 정기 약정 갱신.
 * 금액·주기를 비워 보내면 같은 조건으로 기간만 연장하고,
 * 값을 담아 보내면 조건이 바뀐 갱신이라 서버가 재서명 대기(SIGNING)로 돌린다.
 */
export const renewCommitment = (
  publicId: string,
  changes?: { effectiveTo?: string; pledgeAmount?: number; pledgeFrequency?: string },
) =>
  apiRequest<CommitmentSummary>(`/api/v1/commitments/${publicId}/renew`, {
    method: 'POST',
    body: JSON.stringify({
      effectiveTo: changes?.effectiveTo ?? null,
      pledgeAmount: changes?.pledgeAmount ?? null,
      pledgeFrequency: changes?.pledgeFrequency ?? null,
    }),
  });

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
