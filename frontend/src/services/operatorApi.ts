import { apiRequest } from './apiClient';
import type { ApplicationResponse } from './applicationApi';
import type { ManagedOpportunity } from './managementApi';
import type { PostItem } from './communityApi';

export interface ManagerApplication {
  publicId: string;
  applicantEmail: string;
  organizationName: string;
  position: string;
  contact: string;
  reason: string;
  plannedCenterName?: string;
  status: string;
  createdAt: string;
}

export interface OrganizationApplication {
  publicId: string;
  applicantEmail: string;
  name: string;
  organizationType: string;
  registrationNumber?: string;
  representativeName: string;
  phone?: string;
  email?: string;
  address?: string;
  description?: string;
  status: string;
  reviewReason?: string;
  createdOrganizationId?: number;
  createdAt: string;
}

export const fetchManagerApplications = (status = 'PENDING') =>
  apiRequest<ManagerApplication[]>(
    `/api/v1/operator/manager-applications?status=${encodeURIComponent(status)}`,
  );

export const decideManagerApplication = (
  publicId: string,
  decision: 'approve' | 'reject',
  reason = '운영진 승인',
) =>
  apiRequest<ManagerApplication>(
    `/api/v1/operator/manager-applications/${publicId}/${decision}`,
    { method: 'POST', body: JSON.stringify({ reason }) },
  );

export const fetchOrganizationApplications = (status = 'PENDING') =>
  apiRequest<OrganizationApplication[]>(
    `/api/v1/operator/organization-applications?status=${encodeURIComponent(status)}`,
  );

export const decideOrganizationApplication = (
  publicId: string,
  decision: 'approve' | 'reject',
  reason = '운영진 승인',
) =>
  apiRequest<OrganizationApplication>(
    `/api/v1/operator/organization-applications/${publicId}/${decision}`,
    { method: 'POST', body: JSON.stringify({ reason }) },
  );

export const fetchOperatorOpportunities = () =>
  apiRequest<ManagedOpportunity[]>('/api/v1/operator/opportunities');

export const updateOperatorOpportunity = (
  id: number,
  payload: Record<string, unknown>,
) =>
  apiRequest<ManagedOpportunity>(`/api/v1/operator/opportunities/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });

export const deleteOperatorOpportunity = (id: number) =>
  apiRequest<void>(`/api/v1/operator/opportunities/${id}`, { method: 'DELETE' });

export const fetchOperatorPosts = () =>
  apiRequest<{ content: PostItem[] }>('/api/v1/operator/community/posts?size=100');

export const updateOperatorPost = (
  id: number,
  payload: { category: string; title: string; content: string; imageUrl?: string },
) =>
  apiRequest<PostItem>(`/api/v1/operator/community/posts/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });

export const deleteOperatorPost = (id: number) =>
  apiRequest<void>(`/api/v1/operator/community/posts/${id}`, { method: 'DELETE' });

export const fetchOperatorDocuments = () =>
  apiRequest<ApplicationResponse[]>('/api/v1/operator/documents');
