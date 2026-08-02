import { apiRequest } from './apiClient';

export interface Organization {
  id: number;
  name: string;
  organizationType: string;
  registrationNumber?: string;
  representativeName?: string;
  phone?: string;
  email?: string;
  address?: string;
  description?: string;
  verificationStatus: string;
}

export interface ManagerApplicationPayload {
  organizationName: string;
  position: string;
  contact: string;
  organizationType: string;
  registrationNumber?: string;
  evidenceFileId?: number;
  evidenceFileIds?: number[];
  reason: string;
  plannedCenterName?: string;
}

export const uploadEvidence = async (file: File): Promise<number> => {
  const form = new FormData();
  form.append('file', file);
  form.append('purpose', 'MANAGER_EVIDENCE');
  const response = await apiRequest<{ id: number }>('/api/v1/files', {
    method: 'POST',
    body: form,
  });
  return response.id;
};

export const submitManagerApplication = (payload: ManagerApplicationPayload) =>
  apiRequest('/api/v1/manager-applications', {
    method: 'POST',
    body: JSON.stringify(payload),
  });

export const fetchManagedOrganizations = () =>
  apiRequest<Organization[]>('/api/v1/manager/organizations');

export interface CenterDashboard {
  organizationId: number;
  publicOpportunities: number;
  closedOpportunities: number;
  pendingApplications: number;
  monthlyParticipants: number;
  totalCommitments: number;
  signedCommitments: number;
  awaitingSignature: number;
  signedPledgeAmount: number;
  renewalDueSoon: number;
}

export const fetchOrganizationDashboard = (organizationId: number) =>
  apiRequest<CenterDashboard>(
    `/api/v1/manager/organizations/${organizationId}/dashboard`,
  );

export interface ManagedOpportunity {
  id: number;
  organizationId: number;
  organizationName: string;
  type: string;
  category?: string;
  title: string;
  summary?: string;
  description: string;
  region?: string;
  location?: string;
  participationMode: string;
  recruitmentCapacity?: number;
  recruitmentStartDateTime?: string;
  recruitmentEndDateTime?: string;
  activityStartDateTime?: string;
  activityEndDateTime?: string;
  eligibility?: string;
  targetAmount?: number;
  cancellationPolicy?: string;
  status: string;
  applicantCount: number;
  requiredDocuments: Array<{
    code: string;
    name: string;
    description?: string;
    required: boolean;
  }>;
  createdByUserId?: number;
}

export const fetchManagedOpportunities = (organizationId: number) =>
  apiRequest<ManagedOpportunity[]>(
    `/api/v1/manager/organizations/${organizationId}/opportunities`,
  );

export const createManagedOpportunity = (
  organizationId: number,
  payload: Record<string, unknown>,
) =>
  apiRequest<ManagedOpportunity>(
    `/api/v1/manager/organizations/${organizationId}/opportunities`,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
  );

export const updateManagedOpportunity = (
  opportunityId: number,
  payload: Record<string, unknown>,
) =>
  apiRequest<ManagedOpportunity>(`/api/v1/manager/opportunities/${opportunityId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });

export const closeManagedOpportunity = (opportunityId: number) =>
  apiRequest<ManagedOpportunity>(
    `/api/v1/manager/opportunities/${opportunityId}/close`,
    { method: 'POST' },
  );

export const publishManagedOpportunity = (opportunityId: number) =>
  apiRequest<ManagedOpportunity>(
    `/api/v1/manager/opportunities/${opportunityId}/publish`,
    { method: 'POST' },
  );
