import { API_ORIGIN, apiRequest, getAccessToken } from './apiClient';

export interface ClmDocumentDto {
  id: number;
  volunteerId: number;
  volunteerTitle: string;
  applicantName: string;
  applicantEmail: string;
  applicantPhone?: string;
  modusignDocumentId: string;
  signingMethod: 'SECURE_LINK';
  signingUrl: string;
  signingUrlExpiresAt: string;
  status: 'PENDING_SIGNATURE' | 'SIGNING' | 'PARTIALLY_SIGNED' | 'SIGNED' | 'REJECTED' | 'CANCELED' | 'SIGNING_CANCELED';
  createdAt: string;
  signedAt?: string;
  completionMessage?: string;
  completionMessageSource?: 'UPSTAGE_SOLAR' | 'LOCAL_FALLBACK';
}

export interface ClmSignRequestPayload {
  commitmentPublicId: string;
  applicantPhone?: string;
}

export interface ClmDocumentFileDto {
  id: number;
  fileType: 'SIGNED_DOCUMENT' | 'AUDIT_TRAIL';
  originalName: string;
  contentType: string;
  sizeBytes: number;
  sha256: string;
  createdAt: string;
  downloadUrl: string;
}

const API_BASE = '/api/v1/clm/documents';

/**
 * 모두싸인 전자서명 서류 요청 생성
 */
export const requestClmSign = (payload: ClmSignRequestPayload): Promise<ClmDocumentDto> =>
  apiRequest<ClmDocumentDto>(`${API_BASE}/request-sign`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });

/**
 * 서명 완료 콜백 처리
 */
export const refreshClmSecureLink = (documentId: number): Promise<ClmDocumentDto> => {
  return apiRequest<ClmDocumentDto>(`${API_BASE}/${documentId}/secure-link`, { method: 'POST' });
};

export const fetchClmDocument = (documentId: number): Promise<ClmDocumentDto> => {
  return apiRequest<ClmDocumentDto>(`${API_BASE}/${documentId}`);
};

export const fetchClmDocumentFiles = (documentId: number): Promise<ClmDocumentFileDto[]> =>
  apiRequest<ClmDocumentFileDto[]>(`${API_BASE}/${documentId}/files`);

export const loadClmDocumentFile = async (documentId: number, fileId: number) => {
  const token = getAccessToken();
  const response = await fetch(
    `${API_ORIGIN}${API_BASE}/${documentId}/files/${fileId}/download`,
    { headers: token ? { Authorization: `Bearer ${token}` } : {} },
  );
  if (!response.ok) throw new Error('전자서명 파일을 열지 못했습니다.');
  return URL.createObjectURL(await response.blob());
};

/**
 * 내 서명 완료 서류 목록 조회
 */
export const fetchMyClmDocuments = async (_email?: string): Promise<ClmDocumentDto[]> => {
  try {
    return await apiRequest<ClmDocumentDto[]>(`${API_BASE}/my`);
  } catch (error) {
    console.error('내 CLM 서류 조회 오류:', error);
    return [];
  }
};

export const fetchManagerApplicationClmDocuments = (
  applicationPublicId: string,
): Promise<ClmDocumentDto[]> =>
  apiRequest<ClmDocumentDto[]>(
    `/api/v1/manager/applications/${applicationPublicId}/clm-documents`,
  );
