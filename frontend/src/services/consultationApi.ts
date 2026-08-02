import { apiRequest } from './apiClient';

export interface PledgeIntent {
  pledgeType: string | null;
  beneficiary: string | null;
  amount: number | null;
  frequency: string | null;
  startDate: string | null;
  region: string | null;
  rewardPreference: string | null;
  taxDeductionConsent: boolean | null;
  privacyConsent: boolean | null;
  specialConditions: string | null;
  giftItem?: string | null;
  localGovCode?: string | null;
  taxCreditAmount?: number | null;
  heritageTarget?: string | null;
  bequestType?: string | null;
  missingFields: string[];
}

export interface ConsultationResponse {
  id: number;
  status: 'IN_PROGRESS' | 'CONFIRMED';
  summary: string;
  intent: PledgeIntent;
  assistantMessage: string;
  source: 'RULE_FALLBACK' | 'UPSTAGE_SOLAR' | 'STORED';
}

const BASE = '/api/v1/ai/consultations';

export const startConsultation = (message: string, externalAiConsent: boolean) =>
  apiRequest<ConsultationResponse>(BASE, {
    method: 'POST',
    body: JSON.stringify({ message, externalAiConsent }),
  });

export const updateConsultationIntent = (id: number, intent: PledgeIntent) =>
  apiRequest<ConsultationResponse>(`${BASE}/${id}/intent`, {
    method: 'PATCH',
    body: JSON.stringify(intent),
  });

export const confirmConsultation = (id: number) =>
  apiRequest<ConsultationResponse>(`${BASE}/${id}/confirm`, { method: 'POST' });

/** 이미 시작된 상담 세션에 멀티턴 메시지 추가 */
export const sendConsultationMessage = (id: number, message: string, externalAiConsent: boolean) =>
  apiRequest<ConsultationResponse>(`${BASE}/${id}/messages`, {
    method: 'POST',
    body: JSON.stringify({ message, externalAiConsent }),
  });
