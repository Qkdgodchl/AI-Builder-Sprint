export interface ClmDocumentDto {
  id: number;
  volunteerId: number;
  volunteerTitle: string;
  applicantName: string;
  applicantEmail: string;
  applicantPhone?: string;
  modusignDocumentId: string;
  signingUrl: string;
  status: 'PENDING_SIGNATURE' | 'SIGNED' | 'REJECTED';
  createdAt: string;
  signedAt?: string;
}

export interface ClmSignRequestPayload {
  volunteerId: number;
  applicantName: string;
  applicantEmail: string;
  applicantPhone?: string;
}

const API_BASE = 'http://localhost:8080/api/v1/clm/documents';

/**
 * 모두싸인 전자서명 서류 요청 생성
 */
export const requestClmSign = async (payload: ClmSignRequestPayload): Promise<ClmDocumentDto | null> => {
  try {
    const response = await fetch(`${API_BASE}/request-sign`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      throw new Error(`모두싸인 서명 요청 실패: ${response.status}`);
    }

    const result = await response.json();
    return result.data || null;
  } catch (error) {
    console.error('CLM 서명 요청 오류:', error);
    return null;
  }
};

/**
 * 서명 완료 콜백 처리
 */
export const completeClmSign = async (documentId: number): Promise<ClmDocumentDto | null> => {
  try {
    const response = await fetch(`${API_BASE}/${documentId}/complete`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error(`서명 완료 처리 실패: ${response.status}`);
    }

    const result = await response.json();
    return result.data || null;
  } catch (error) {
    console.error('CLM 서명 완료 처리 오류:', error);
    return null;
  }
};

/**
 * 내 서명 완료 서류 목록 조회
 */
export const fetchMyClmDocuments = async (email: string): Promise<ClmDocumentDto[]> => {
  try {
    const response = await fetch(`${API_BASE}/my?email=${encodeURIComponent(email)}`);
    if (!response.ok) {
      throw new Error(`내 서류 조회 실패: ${response.status}`);
    }

    const result = await response.json();
    return result.success && Array.isArray(result.data) ? result.data : [];
  } catch (error) {
    console.error('내 CLM 서류 조회 오류:', error);
    return [];
  }
};
