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
  fileType: 'SIGNED_DOCUMENT' | 'AUDIT_TRAIL' | 'PLEDGE_DRAFT_PDF';
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

/** 센터 대시보드의 약정 현황 카드에서 조직 전체 전자서명 문서를 조회한다. */
export const fetchOrganizationClmDocuments = (
  organizationId: number,
): Promise<ClmDocumentDto[]> =>
  apiRequest<ClmDocumentDto[]>(
    `/api/v1/manager/organizations/${organizationId}/clm-documents`,
  );

/**
 * LLM 대화 기반 약정서 PDF 자동 생성 + 모두싸인 전자서명 요청
 * - consultationId: AI 상담 세션 ID
 * - commitmentPublicId: 약정 레코드 공개 식별자
 */
export const requestSignFromConversation = (payload: {
  consultationId: number;
  commitmentPublicId: string;
  applicantPhone?: string;
}): Promise<ClmDocumentDto> =>
  apiRequest<ClmDocumentDto>(`${API_BASE}/request-sign-from-conversation`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });

/** 약정서에서 되읽은 항목 하나와 우리가 보관한 값의 대조 결과. */
export interface ClmVerificationCheck {
  label: string;
  expected: string | null;
  found: string | null;
  matched: boolean;
}

export interface ClmVerificationDto {
  id: number;
  documentId: number;
  /** MATCHED | MISMATCHED | UNREADABLE */
  status: string;
  checkedCount: number;
  matchedCount: number;
  checks: ClmVerificationCheck[];
  parsedExcerpt: string | null;
  sourceFileType: string | null;
  provider: string | null;
  verifiedAt: string;
}

/** Upstage 문서 AI로 체결본을 되읽어 약정 원본과 대조한다. */
export const verifyClmDocument = (documentId: number): Promise<ClmVerificationDto> =>
  apiRequest<ClmVerificationDto>(`${API_BASE}/${documentId}/verification`, { method: 'POST' });

/** 마지막 검증 결과. 아직 검증한 적이 없으면 null이다. */
export const fetchClmVerification = (documentId: number): Promise<ClmVerificationDto | null> =>
  apiRequest<ClmVerificationDto | null>(`${API_BASE}/${documentId}/verification`);
