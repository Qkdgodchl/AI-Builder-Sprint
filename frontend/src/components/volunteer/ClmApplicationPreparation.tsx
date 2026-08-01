import React, { useState } from 'react';
import type { VolunteerItem } from '../../types';
import {
  createApplication,
  submitCommitment,
} from '../../services/applicationApi';
import { requestClmSign, fetchClmDocument, refreshClmSecureLink, fetchClmDocumentFiles } from '../../services/clmApi';
import type { ClmDocumentDto, ClmDocumentFileDto } from '../../services/clmApi';
import { API_ORIGIN } from '../../services/apiClient';
import {
  confirmConsultation,
  startConsultation,
  updateConsultationIntent,
} from '../../services/consultationApi';
import type { ConsultationResponse, PledgeIntent } from '../../services/consultationApi';

interface ApplicationItem extends VolunteerItem {
  programType: string;
  availability: string;
}

interface ClmApplicationPreparationProps {
  item: ApplicationItem;
  typeLabel: string;
  onBack: () => void;
}

export const ClmApplicationPreparation: React.FC<ClmApplicationPreparationProps> = ({
  item,
  typeLabel,
  onBack,
}) => {
  const isVolunteer = item.category === 'VOLUNTEER';
  const isHometown = item.programType === 'HOMETOWN' || item.category === 'HOMETOWN';
  const documentName = isVolunteer ? '봉사 참여 약정서 (제2026-PC-01호)' : '후원 및 기부 약정서 (제2026-PC-02호)';

  const [specialConditions, setSpecialConditions] = useState('');
  const [privacyConsent, setPrivacyConsent] = useState(false);
  const [thirdPartyConsent, setThirdPartyConsent] = useState(false);
  const [portraitConsent, setPortraitConsent] = useState(false);
  const [confirmed, setConfirmed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [completed, setCompleted] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [aiPrompt, setAiPrompt] = useState(
    isHometown
      ? '부산 지역 아동을 위해 매월 3만원씩 고향사랑기부를 하고 싶고 답례품은 필요 없어요.'
      : isVolunteer
        ? `${item.title} 봉사에 참여해서 ${item.organizer}를 돕고 싶어요.`
        : `${item.title}에 일시 3만원을 기부하고 싶어요.`,
  );
  const [consultation, setConsultation] = useState<ConsultationResponse | null>(null);
  const [intent, setIntent] = useState<PledgeIntent | null>(null);
  const [aiConfirmed, setAiConfirmed] = useState(false);
  const [structuringIntent, setStructuringIntent] = useState(false);
  const [commitmentPublicId, setCommitmentPublicId] = useState<string | null>(null);

  // 모두싸인 (Modusign) 전자서명 상태
  const [applicantName] = useState('로그인 사용자');
  const [applicantEmail] = useState('로그인 계정 이메일');
  const [clmDoc, setClmDoc] = useState<ClmDocumentDto | null>(null);
  const [docFiles, setDocFiles] = useState<ClmDocumentFileDto[]>([]);
  const [isSigningModalOpen, setIsSigningModalOpen] = useState(false);
  const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false);
  const [isDocViewModalOpen, setIsDocViewModalOpen] = useState(false);
  const [isSigned, setIsSigned] = useState(false);
  const [requestingSign, setRequestingSign] = useState(false);
  const [checkingSignature, setCheckingSignature] = useState(false);
  const [signatureStatusMessage, setSignatureStatusMessage] = useState('');

  const handleOpenDocView = async () => {
    setIsDocViewModalOpen(true);
    if (clmDoc) {
      try {
        const files = await fetchClmDocumentFiles(clmDoc.id);
        setDocFiles(files);
      } catch (e) {
        console.error('서명 완료 PDF 문서 파일 조회 실패:', e);
      }
    }
  };

  const canStartSigning = aiConfirmed && privacyConsent && thirdPartyConsent;

  const handleStructureIntent = async () => {
    if (!aiPrompt.trim()) return;
    setStructuringIntent(true);
    setErrorMessage('');
    try {
      const result = await startConsultation(aiPrompt.trim());
      const enriched: PledgeIntent = {
        ...result.intent,
        pledgeType: result.intent.pledgeType || (isHometown ? 'HOMETOWN_DONATION' : isVolunteer ? 'VOLUNTEER' : 'DONATION'),
        beneficiary: result.intent.beneficiary || item.organizer,
        region: result.intent.region || item.location,
        frequency: result.intent.frequency || (isVolunteer ? 'NOT_APPLICABLE' : isHometown ? 'MONTHLY' : 'ONE_TIME'),
      };
      setConsultation(result);
      setIntent(enriched);
      setAiConfirmed(false);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : 'AI 약정 정리에 실패했습니다.');
    } finally {
      setStructuringIntent(false);
    }
  };

  const handleConfirmIntent = async () => {
    if (!consultation || !intent) return;
    setStructuringIntent(true);
    setErrorMessage('');
    try {
      const updated = await updateConsultationIntent(consultation.id, intent);
      const confirmedIntent = await confirmConsultation(updated.id);
      setConsultation(confirmedIntent);
      setIntent(confirmedIntent.intent);
      setSpecialConditions(confirmedIntent.intent.specialConditions || specialConditions);
      setAiConfirmed(true);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '약정 의사를 확정하지 못했습니다.');
    } finally {
      setStructuringIntent(false);
    }
  };

  // 모두싸인 서명 요청 시작
  const handleStartModusign = async () => {
    if (!aiConfirmed || !consultation) {
      alert('AI가 정리한 약정 의사를 먼저 확인·확정해 주세요.');
      return;
    }
    if (!canStartSigning) {
      alert('필수 동의 항목을 먼저 동의해 주세요.');
      return;
    }

    setRequestingSign(true);
    try {
      let activeCommitmentId = commitmentPublicId;
      if (!activeCommitmentId) {
        const application = await createApplication(item.id, {
          consultationId: consultation.id,
          specialConditions,
          privacyConsent,
          thirdPartyConsent,
          portraitConsent,
        });
        activeCommitmentId = application.commitment?.publicId || null;
        if (!activeCommitmentId) throw new Error('생성된 약정서 식별자를 확인할 수 없습니다.');
        await submitCommitment(activeCommitmentId);
        setCommitmentPublicId(activeCommitmentId);
      }
      const doc = await requestClmSign({
        commitmentPublicId: activeCommitmentId,
      });

      setClmDoc(doc);
      setSignatureStatusMessage('');
      setIsSigningModalOpen(true);
    } catch (err) {
      console.error(err);
      alert(err instanceof Error ? err.message : '서명 요청 처리 중 오류가 발생했습니다.');
    } finally {
      setRequestingSign(false);
    }
  };

  const handleCheckSignature = async () => {
    if (!clmDoc) return;
    setCheckingSignature(true);
    setSignatureStatusMessage('');
    try {
      const updated = await fetchClmDocument(clmDoc.id);
      setClmDoc(updated);
      if (updated.status === 'SIGNED') {
        setIsSigned(true);
        setIsSigningModalOpen(false);
        alert('모두싸인 전자서명 완료가 확인되었습니다.');
      } else {
        setSignatureStatusMessage('아직 서명이 완료되지 않았습니다. 모두싸인에서 서명을 끝낸 뒤 다시 확인해 주세요.');
      }
    } catch (err) {
      console.error(err);
      setSignatureStatusMessage(
        err instanceof Error && err.message.includes('잠시')
          ? err.message
          : '상태 확인 요청이 잠시 겹쳤습니다. 서명을 마친 뒤 잠시 후 다시 눌러주세요.',
      );
    } finally {
      setCheckingSignature(false);
    }
  };

  const handleReopenSigning = async () => {
    if (!clmDoc) return;
    const updated = await refreshClmSecureLink(clmDoc.id);
    setClmDoc(updated);
    return updated;
  };

  const handleSubmit = async () => {
    if (!isSigned) {
      alert('모두싸인 전자서명을 먼저 완료해 주세요!');
      return;
    }

    setSubmitting(true);
    setErrorMessage('');
    try {
      setCompleted(true);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : '신청에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <article className="clm-application">
      <div className="clm-back-nav">
        <button type="button" onClick={onBack}>
          프로그램 상세로 돌아가기
        </button>
        <span>CLM APPLICATION (MODUSIGN VERIFIED)</span>
      </div>

      <header className="clm-header">
        <p>모두싸인 API 기반 약정서 전자서명 및 CLM</p>
        <h2>신청 서류 작성 & 전자서명</h2>
        <span>
          신청 정보를 바탕으로 약정서를 작성하고 모두싸인 전자서명을 완료하면 최종 신청 및 서류가 안전하게 보존됩니다.
        </span>
      </header>

      <ol className="clm-steps" aria-label="신청 진행 단계">
        <li className={aiConfirmed ? 'complete' : 'current'}>
          <span>01</span>
          <strong>AI 의사 정리</strong>
        </li>
        <li className={!aiConfirmed ? 'upcoming' : privacyConsent && thirdPartyConsent ? 'complete' : 'current'}>
          <span>02</span>
          <strong>서류 작성</strong>
        </li>
        <li className={isSigned ? 'complete' : canStartSigning ? 'current' : 'upcoming'}>
          <span>03</span>
          <strong>전자서명</strong>
        </li>
        <li className={completed ? 'complete' : 'upcoming'}>
          <span>04</span>
          <strong>신청 완료</strong>
        </li>
      </ol>

      <div className="clm-layout">
        <div className="clm-main">
          <section className="clm-program-summary">
            <div>
              <span>신청 프로그램</span>
              <strong>{item.title}</strong>
            </div>
            <dl>
              <div>
                <dt>구분</dt>
                <dd>{typeLabel}</dd>
              </div>
              <div>
                <dt>주관기관</dt>
                <dd>{item.organizer}</dd>
              </div>
              <div>
                <dt>지역</dt>
                <dd>{item.location}</dd>
              </div>
            </dl>
          </section>

          <section className="clm-document-section clm-ai-intent-section">
            <div className="clm-section-heading">
              <div>
                <span>STEP 01</span>
                <h3>AI로 약정 의사 정리</h3>
              </div>
              <span className={`clm-status ${aiConfirmed ? 'complete' : 'pending'}`}>
                {aiConfirmed ? '확정 완료' : '확인 필요'}
              </span>
            </div>
            <p className="clm-ai-description">
              하고 싶은 기부·봉사 내용을 편하게 적으면 약정 항목으로 정리합니다. 결과는 직접 수정한 뒤 확정할 수 있습니다.
            </p>
            {isHometown && (
              <div className="clm-hometown-guide">
                <strong>부산 고향사랑 정기기부 데모</strong>
                <span>부산 지역 · 매월 3만원 · 지역 아동 지원 · 답례품 미선택</span>
              </div>
            )}
            <label>
              나의 약정 의사
              <textarea
                value={aiPrompt}
                onChange={(event) => setAiPrompt(event.target.value)}
                rows={3}
                disabled={aiConfirmed}
              />
            </label>
            {!intent && (
              <button type="button" className="clm-ai-action" onClick={handleStructureIntent} disabled={structuringIntent}>
                {structuringIntent ? '정리 중...' : 'AI로 약정 항목 정리하기'}
              </button>
            )}
            {intent && (
              <div className="clm-intent-editor">
                <label>
                  약정 유형
                  <select
                    value={intent.pledgeType || ''}
                    onChange={(event) => setIntent({ ...intent, pledgeType: event.target.value })}
                    disabled={aiConfirmed}
                  >
                    <option value="DONATION">일반 기부</option>
                    <option value="HOMETOWN_DONATION">고향사랑기부</option>
                    <option value="VOLUNTEER">봉사</option>
                    <option value="LEGACY_DONATION">유산기부</option>
                    <option value="CULTURAL_HERITAGE_DONATION">문화유산기부</option>
                  </select>
                </label>
                <label>
                  수혜 대상·기관
                  <input
                    value={intent.beneficiary || ''}
                    onChange={(event) => setIntent({ ...intent, beneficiary: event.target.value })}
                    disabled={aiConfirmed}
                  />
                </label>
                {intent.pledgeType !== 'VOLUNTEER' && (
                  <label>
                    금액(원)
                    <input
                      type="number"
                      min="0"
                      value={intent.amount ?? ''}
                      onChange={(event) => setIntent({ ...intent, amount: event.target.value ? Number(event.target.value) : null })}
                      disabled={aiConfirmed}
                    />
                  </label>
                )}
                <label>
                  주기
                  <select
                    value={intent.frequency || ''}
                    onChange={(event) => setIntent({ ...intent, frequency: event.target.value })}
                    disabled={aiConfirmed}
                  >
                    <option value="ONE_TIME">일시</option>
                    <option value="MONTHLY">매월</option>
                    <option value="ANNUAL">매년</option>
                    <option value="NOT_APPLICABLE">해당 없음</option>
                  </select>
                </label>
                <label>
                  지역
                  <input
                    value={intent.region || ''}
                    onChange={(event) => setIntent({ ...intent, region: event.target.value })}
                    disabled={aiConfirmed}
                  />
                </label>
                {intent.pledgeType === 'HOMETOWN_DONATION' && (
                  <label>
                    답례품
                    <select
                      value={intent.rewardPreference || 'UNSPECIFIED'}
                      onChange={(event) => setIntent({ ...intent, rewardPreference: event.target.value })}
                      disabled={aiConfirmed}
                    >
                      <option value="UNSPECIFIED">나중에 선택</option>
                      <option value="NONE">받지 않음</option>
                    </select>
                  </label>
                )}
                <div className="clm-intent-summary">
                  <span>정리 결과</span>
                  <strong>{consultation?.summary}</strong>
                  <small>구조화 방식: {consultation?.source === 'UPSTAGE_SOLAR' ? 'Upstage Solar' : '안전한 로컬 폴백'}</small>
                </div>
                {!aiConfirmed && (
                  <button type="button" className="clm-ai-action" onClick={handleConfirmIntent} disabled={structuringIntent}>
                    {structuringIntent ? '확정 중...' : '수정한 약정 의사 확정하기'}
                  </button>
                )}
              </div>
            )}
          </section>

          <section className="clm-document-section">
            <div className="clm-section-heading">
              <div>
                <span>STEP 02</span>
                <h3>서류 작성 & 서식 열람</h3>
              </div>
              <span className={`clm-status ${canStartSigning ? 'complete' : 'pending'}`}>
                {canStartSigning ? '작성 완료' : '작성 중'}
              </span>
            </div>

            <div className="clm-document-card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <span>공식 서식 양식</span>
                <strong>{documentName}</strong>
                <p>
                  참여 조건, 안전 수칙 준수, 픽셀 온기 보상 및 보안 동의 항목 포함
                </p>
              </div>
              <button
                type="button"
                style={{
                  padding: '8px 14px',
                  background: '#faf0ca',
                  border: '1.5px solid #111',
                  borderRadius: '6px',
                  fontWeight: 'bold',
                  cursor: 'pointer',
                  fontSize: '13px'
                }}
                onClick={() => setIsPreviewModalOpen(true)}
              >
                📄 약정서 전문 미리보기
              </button>
            </div>

            <div style={{ display: 'flex', gap: '12px', marginBottom: '16px' }}>
              <label style={{ flex: 1 }}>
                신청자 성명
                <input
                  type="text"
                  className="pixel-input"
                  style={{ width: '100%', padding: '8px 12px', marginTop: '6px' }}
                  value={applicantName}
                  readOnly
                />
              </label>
              <label style={{ flex: 1 }}>
                신청자 이메일
                <input
                  type="email"
                  className="pixel-input"
                  style={{ width: '100%', padding: '8px 12px', marginTop: '6px' }}
                  value={applicantEmail}
                  readOnly
                />
              </label>
            </div>
            <small className="clm-identity-note">서명자 정보는 로그인한 계정의 프로필을 기준으로 서버에서 검증합니다.</small>

            <label>
              특별 조건 및 전달사항
              <textarea
                value={specialConditions}
                onChange={(event) => setSpecialConditions(event.target.value)}
                placeholder="참여 가능한 시간이나 기관에 전달할 내용을 입력해주세요."
                rows={3}
              />
            </label>

            <label className="clm-final-consent">
              <input
                type="checkbox"
                checked={privacyConsent}
                onChange={(event) => setPrivacyConsent(event.target.checked)}
              />
              <span>개인정보 수집·이용에 동의합니다. (필수)</span>
            </label>
            <label className="clm-final-consent">
              <input
                type="checkbox"
                checked={thirdPartyConsent}
                onChange={(event) => setThirdPartyConsent(event.target.checked)}
              />
              <span>주관기관에 신청 정보를 제공하는 데 동의합니다. (필수)</span>
            </label>
            <label className="clm-final-consent">
              <input
                type="checkbox"
                checked={portraitConsent}
                onChange={(event) => setPortraitConsent(event.target.checked)}
              />
              <span>활동 사진의 초상권 활용 및 온기 뱃지 기록에 동의합니다. (선택)</span>
            </label>
          </section>

          <section className="clm-signature-section">
            <div className="clm-section-heading">
              <div>
                <span>STEP 03</span>
                <h3>모두싸인 (Modusign) 전자서명</h3>
              </div>
              <span className={`clm-status ${isSigned ? 'complete' : canStartSigning ? 'pending' : 'locked'}`}>
                {isSigned ? '서명 완료' : canStartSigning ? '서명 대기' : '서류 작성 후 가능'}
              </span>
            </div>

            <div className="clm-signature-placeholder" style={{ borderColor: isSigned ? '#2ec4b6' : 'var(--pc-dark)' }}>
              <span>MODUSIGN E-SIGNATURE</span>
              <strong>{isSigned ? '전자서명 완료' : '모두싸인 전자서명'}</strong>
              <p>
                {isSigned
                  ? `문서 ID: ${clmDoc?.modusignDocumentId || 'MODU_SIGNED'} · 완료된 전자서명 문서가 보존되었습니다.`
                  : '약정서를 확인한 뒤 모두싸인 보안 서명창에서 전자서명을 진행합니다.'}
              </p>

              {!isSigned ? (
                <button
                  type="button"
                  style={{
                    background: canStartSigning ? '#ff70a6' : '#aaa',
                    cursor: canStartSigning ? 'pointer' : 'not-allowed',
                    color: '#fff',
                    padding: '12px 24px',
                    fontSize: '14px',
                    borderRadius: '8px',
                    border: '2px solid #111',
                    fontWeight: 'bold'
                  }}
                  disabled={!canStartSigning || requestingSign}
                  onClick={handleStartModusign}
                >
                  {requestingSign ? '서명 창 로딩 중...' : '모두싸인 전자서명 시작'}
                </button>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '10px' }}>
                  <div style={{ color: '#2ec4b6', fontWeight: 'bold', fontSize: '15px' }}>
                    ✍️ 서명 완료일시: {new Date().toLocaleString('ko-KR')}
                  </div>
                  <button
                    type="button"
                    style={{
                      padding: '8px 18px',
                      background: '#2ec4b6',
                      color: '#fff',
                      border: '2px solid #111',
                      borderRadius: '6px',
                      fontWeight: 'bold',
                      cursor: 'pointer',
                      fontSize: '13px'
                    }}
                    onClick={handleOpenDocView}
                  >
                    🔍 서명 완료된 약정 증서 열람하기
                  </button>
                </div>
              )}
            </div>
            <p className="clm-placeholder-note">
                모두싸인 보안 서명창에서 작성한 전자서명 문서가 픽셀케어 CLM에 안전하게 보관됩니다.
            </p>
          </section>
        </div>

        <aside className="clm-submit-panel">
          <p>신청 완료 조건</p>
          <ul>
            <li className="complete">신청 프로그램 확인</li>
            <li className={privacyConsent && thirdPartyConsent ? 'complete' : ''}>필수 서류 & 약정 작성</li>
            <li className={privacyConsent && thirdPartyConsent ? 'complete' : ''}>개인정보 동의</li>
            <li className={isSigned ? 'complete' : ''}>모두싸인 전자서명 완료</li>
          </ul>
          <div className="clm-submit-divider" />
          <label className="clm-final-consent">
            <input
              type="checkbox"
              checked={confirmed}
              onChange={(event) => setConfirmed(event.target.checked)}
            />
            <span>작성 내용과 약정 조건을 모두 확인했습니다.</span>
          </label>
          <button
            type="button"
            className="clm-final-submit"
            disabled={!privacyConsent || !thirdPartyConsent || !isSigned || !confirmed || submitting || completed}
            onClick={handleSubmit}
          >
            {completed ? '🎉 최종 신청 접수 완료!' : submitting ? '신청 처리 중...' : '🚀 작성 완료 및 최종 신청'}
          </button>
          {errorMessage && <small className="auth-form-error">{errorMessage}</small>}
          <small>
            {completed
              ? '신청서와 모두싸인 전자서명이 픽셀케어 CLM에 보관되었습니다.'
              : '모두싸인 서명 완료 후 최종 신청할 수 있습니다.'}
          </small>
        </aside>
      </div>

      {/* 1. 모두싸인 실시간 템플릿 약정서 작성/서명 뷰어 모달 */}
      {isPreviewModalOpen && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.85)', display: 'flex', justifyContent: 'center', alignItems: 'center',
          zIndex: 9999, padding: '20px'
        }}>
          <div style={{
            background: '#fff', width: '100%', maxWidth: '640px',
            borderRadius: '16px', border: '3px solid #111', padding: '28px', display: 'flex', flexDirection: 'column',
            boxShadow: '0 10px 30px rgba(0,0,0,0.6)', textAlign: 'center'
          }}>
            <div style={{ borderBottom: '2px solid #111', paddingBottom: '16px', marginBottom: '20px' }}>
              <span style={{ fontSize: '11px', fontWeight: 'bold', background: '#ff3b30', color: '#fff', padding: '3px 8px', borderRadius: '4px' }}>
                모두싸인 템플릿 연동 (ID: 47f3a310...)
              </span>
              <h3 style={{ fontSize: '20px', fontWeight: 'bold', margin: '8px 0 4px', color: '#111' }}>
                📜 {documentName}
              </h3>
              <p style={{ fontSize: '12px', color: '#666', margin: 0 }}>
                모두싸인 보안 서명창에서 실제 템플릿 서식의 빈칸을 직접 입력하고 서명합니다.
              </p>
            </div>

            {/* 뷰어 안내 및 팝업 열기 버튼 */}
            <div style={{ background: '#f8f9fa', border: '2px dashed #ff3b30', borderRadius: '12px', padding: '24px', marginBottom: '20px' }}>
              <span style={{ fontSize: '36px', display: 'block', marginBottom: '8px' }}>✒️</span>
              <h4 style={{ margin: '0 0 6px', fontSize: '16px', fontWeight: 'bold', color: '#1a1a24' }}>
                모두싸인 템플릿 전자약정서 서명창
              </h4>
              <p style={{ fontSize: '13px', color: '#555', margin: '0 0 16px', lineHeight: 1.5 }}>
                보안 정책(X-Frame-Options) 차단 없이 안전하고 쾌적하게 작성하기 위해<br/>
                <b>모두싸인 공식 서약창 팝업</b>으로 즉시 연결됩니다.
              </p>

              <button
                type="button"
                style={{
                  padding: '12px 28px', background: '#ff3b30', color: '#fff',
                  border: '2px solid #111', borderRadius: '8px', fontWeight: 'bold', fontSize: '14px',
                  cursor: 'pointer', boxShadow: '0 4px 10px rgba(255,59,48,0.3)',
                  display: 'inline-flex', alignItems: 'center', gap: '8px'
                }}
                onClick={async () => {
                  if (!clmDoc?.signingUrl) {
                    await handleStartModusign();
                  } else {
                    window.open(clmDoc.signingUrl, 'ModusignWindow', 'width=1000,height=800,scrollbars=yes,resizable=yes');
                  }
                }}
              >
                🚀 모두싸인 템플릿 서약창 열기 (팝업)
              </button>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px', paddingTop: '12px', borderTop: '1px solid #eee' }}>
              <button
                type="button"
                style={{
                  padding: '10px 18px', background: '#e9ecef', color: '#495057',
                  border: 'none', borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer'
                }}
                onClick={() => setIsPreviewModalOpen(false)}
              >
                닫기
              </button>
              <button
                type="button"
                style={{
                  padding: '10px 22px', background: '#2ec4b6', color: '#fff',
                  border: '2px solid #111', borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer',
                  fontSize: '13px'
                }}
                onClick={() => {
                  setIsPreviewModalOpen(false);
                  handleCheckSignature();
                }}
              >
                ✅ 양식 작성 & 서명 완료 확인
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 2. 모두싸인 SECURE_LINK 서명 진행 안내 모달 */}
      {isSigningModalOpen && clmDoc && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0, 0, 0, 0.8)', display: 'flex', justifyContent: 'center', alignItems: 'center',
          zIndex: 9999, padding: '20px'
        }}>
          <div style={{
            background: '#fff', width: '100%', maxWidth: '640px',
            borderRadius: '16px', border: '3px solid #111', padding: '28px',
            boxShadow: '0 10px 30px rgba(0,0,0,0.6)', textAlign: 'center'
          }}>
            <h3 style={{ fontSize: '20px', fontWeight: 'bold', margin: '0 0 8px 0', color: '#111' }}>
              📜 모두싸인 전자서명 진행
            </h3>
            <p style={{ fontSize: '13px', color: '#555', marginBottom: '20px', lineHeight: 1.5 }}>
              <b>[{clmDoc.volunteerTitle}]</b> 서약서 생성이 완료되었습니다.<br/>
              아래 버튼을 눌러 <b>모두싸인 서약창 팝업</b>에서 서약을 완료해 주세요.
            </p>

            <div style={{ background: '#f8f9fa', border: '2px dashed #2ec4b6', borderRadius: '12px', padding: '24px', marginBottom: '20px' }}>
              <span style={{ fontSize: '36px', display: 'block', marginBottom: '8px' }}>✒️</span>
              <button
                type="button"
                style={{
                  padding: '12px 28px', background: '#ff3b30', color: '#fff',
                  border: '2px solid #111', borderRadius: '8px', fontWeight: 'bold', fontSize: '14px',
                  cursor: 'pointer', boxShadow: '0 4px 10px rgba(255,59,48,0.3)',
                  display: 'inline-flex', alignItems: 'center', gap: '8px'
                }}
                onClick={async () => {
                  try {
                    const updated = await handleReopenSigning();
                    if (updated?.signingUrl) {
                      window.open(updated.signingUrl, 'ModusignWindow', 'width=1000,height=800,scrollbars=yes,resizable=yes');
                    }
                  } catch (error) {
                    alert(error instanceof Error ? error.message : '서명 링크를 다시 발급하지 못했습니다.');
                  }
                }}
              >
                🚀 모두싸인 서약창 열기 (팝업)
              </button>
              <div style={{ marginTop: '12px', fontSize: '11px', color: '#888' }}>
                문서 코드: {clmDoc.modusignDocumentId}
              </div>
            </div>

            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
              <button
                type="button"
                style={{
                  padding: '12px 20px', background: '#ccc', border: '2px solid #111',
                  borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer'
                }}
                onClick={() => setIsSigningModalOpen(false)}
              >
                취소
              </button>
              <button
                type="button"
                style={{
                  padding: '12px 24px', background: '#2ec4b6',
                  color: '#fff', border: '2px solid #111', borderRadius: '8px',
                  fontWeight: 'bold', cursor: 'pointer'
                }}
                onClick={handleCheckSignature}
                disabled={checkingSignature}
              >
                {checkingSignature ? '확인 중...' : '✅ 서명 완료 확인'}
              </button>
            </div>
            {signatureStatusMessage && (
              <p role="status" style={{ margin: '12px 0 0', color: '#b54708', fontSize: '13px', fontWeight: 700 }}>
                {signatureStatusMessage}
              </p>
            )}
          </div>
        </div>
      )}

      {/* 3. 서명 완료된 최종 계약/약정 증서 열람 모달 */}
      {isDocViewModalOpen && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.85)', display: 'flex', justifyContent: 'center', alignItems: 'center',
          zIndex: 9999, padding: '20px'
        }}>
          <div style={{
            background: '#fff', width: '100%', maxWidth: '680px', maxHeight: '90vh',
            borderRadius: '16px', border: '3px solid #2ec4b6', padding: '32px', overflowY: 'auto',
            boxShadow: '0 10px 30px rgba(0,0,0,0.6)', textAlign: 'left', position: 'relative'
          }}>
            {/* 공식 직인 뱃지 */}
            <div style={{
              position: 'absolute', top: '24px', right: '24px',
              width: '85px', height: '85px', borderRadius: '50%',
              border: '3px double #ff3b30', color: '#ff3b30', display: 'flex',
              flexDirection: 'column', justifyContent: 'center', alignItems: 'center',
              transform: 'rotate(-12deg)', fontWeight: 'bold', fontSize: '11px', textAlign: 'center',
              background: 'rgba(255,255,255,0.95)', boxShadow: '0 4px 10px rgba(0,0,0,0.1)'
            }}>
              <span>픽셀케어</span>
              <span>전자서명</span>
              <span>[검증완료]</span>
            </div>

            <h2 style={{ textAlign: 'center', fontSize: '22px', fontWeight: '900', color: '#111', marginBottom: '6px' }}>
              📜 픽셀케어 전자서명 완료 약정 증서
            </h2>
            <p style={{ textAlign: 'center', fontSize: '12px', color: '#666', marginBottom: '24px' }}>
              모두싸인에서 체결된 완료 문서와 감사추적 자료를 픽셀케어 CLM에서 함께 보관합니다.
            </p>

            <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: '20px', fontSize: '13px' }}>
              <tbody>
                <tr>
                  <th style={{ background: '#f8f9fa', padding: '10px 14px', border: '1px solid #dee2e6', width: '130px' }}>약정서 명칭</th>
                  <td style={{ padding: '10px 14px', border: '1px solid #dee2e6', fontWeight: 'bold' }}>{documentName}</td>
                </tr>
                <tr>
                  <th style={{ background: '#f8f9fa', padding: '10px 14px', border: '1px solid #dee2e6' }}>문서 식별자 (ID)</th>
                  <td style={{ padding: '10px 14px', border: '1px solid #dee2e6', fontFamily: 'monospace', color: '#ff3b30', fontWeight: 'bold' }}>
                    {clmDoc?.modusignDocumentId || '1e0ab220-8d7a-11f1-826b-bb259b33cd21'}
                  </td>
                </tr>
                <tr>
                  <th style={{ background: '#f8f9fa', padding: '10px 14px', border: '1px solid #dee2e6' }}>신청 프로그램</th>
                  <td style={{ padding: '10px 14px', border: '1px solid #dee2e6', fontWeight: 'bold' }}>{item.title} ({item.organizer})</td>
                </tr>
                <tr>
                  <th style={{ background: '#f8f9fa', padding: '10px 14px', border: '1px solid #dee2e6' }}>서명자 정보</th>
                  <td style={{ padding: '10px 14px', border: '1px solid #dee2e6' }}>{applicantName} ({applicantEmail})</td>
                </tr>
                {specialConditions && (
                  <tr>
                    <th style={{ background: '#f8f9fa', padding: '10px 14px', border: '1px solid #dee2e6' }}>서약 특약 사항</th>
                    <td style={{ padding: '10px 14px', border: '1px solid #dee2e6', color: '#555' }}>{specialConditions}</td>
                  </tr>
                )}
                <tr>
                  <th style={{ background: '#f8f9fa', padding: '10px 14px', border: '1px solid #dee2e6' }}>서명 체결일시</th>
                  <td style={{ padding: '10px 14px', border: '1px solid #dee2e6', color: '#2ec4b6', fontWeight: 'bold' }}>
                    {clmDoc?.signedAt ? new Date(clmDoc.signedAt).toLocaleString('ko-KR') : new Date().toLocaleString('ko-KR')}
                  </td>
                </tr>
              </tbody>
            </table>

            {/* 서명 완료 원본 PDF 문서 다운로드 섹션 */}
            <div style={{ background: '#f8f9fa', border: '1.5px solid #dee2e6', borderRadius: '12px', padding: '18px', marginBottom: '20px' }}>
              <h4 style={{ margin: '0 0 10px', fontSize: '14px', fontWeight: 'bold', color: '#111', display: 'flex', alignItems: 'center', gap: '6px' }}>
                📥 체결 완료 원본 약정 서류 및 증서 다운로드
              </h4>
              {docFiles && docFiles.length > 0 ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {docFiles.map((file) => (
                    <a
                      key={file.id}
                      href={`${API_ORIGIN}${file.downloadUrl}`}
                      target="_blank"
                      rel="noreferrer"
                      style={{
                        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                        background: '#fff', padding: '10px 14px', border: '1.5px solid #ced4da',
                        borderRadius: '8px', textDecoration: 'none', color: '#1a1a24', fontSize: '13px', fontWeight: 'bold'
                      }}
                    >
                      <span>📄 {file.originalName} ({file.fileType === 'SIGNED_DOCUMENT' ? '서명 완료 PDF' : '감사추적 인증서'})</span>
                      <span style={{ background: '#ff3b30', color: '#fff', padding: '4px 12px', borderRadius: '6px', fontSize: '12px' }}>
                        내려받기 💾
                      </span>
                    </a>
                  ))}
                </div>
              ) : (
                <div style={{ background: '#fff', padding: '12px', borderRadius: '8px', border: '1px solid #e9ecef', fontSize: '12px', color: '#555', lineHeight: 1.5 }}>
                  💡 모두싸인에서 완결된 서명 문서가 보존되었습니다.<br/>
                  문서 식별 코드: <b style={{ color: '#ff3b30' }}>{clmDoc?.modusignDocumentId || '1e0ab220-8d7a-11f1-826b-bb259b33cd21'}</b>
                </div>
              )}
            </div>

            {/* 검증 안심 상자 */}
            <div style={{
              background: '#e6fffa', border: '2px dashed #2ec4b6', borderRadius: '12px',
              padding: '16px', textAlign: 'center', marginBottom: '24px'
            }}>
              <span style={{ fontSize: '12px', color: '#0077b6', display: 'block', marginBottom: '4px', fontWeight: 'bold' }}>
                ✅ 모두싸인 전자서명 완료 상태 확인 (SIGNED)
              </span>
              <p style={{ margin: 0, fontSize: '12px', color: '#333' }}>
                서명 완료 문서와 감사추적 자료의 SHA-256 체크섬을 기록해 보관 파일의 동일성을 확인할 수 있습니다.
              </p>
            </div>

            <div style={{ textAlign: 'center', display: 'flex', justifyContent: 'center', gap: '12px' }}>
              <button
                type="button"
                style={{
                  padding: '10px 24px', background: '#2ec4b6', color: '#fff',
                  border: 'none', borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer', fontSize: '14px'
                }}
                onClick={() => setIsDocViewModalOpen(false)}
              >
                확인 및 닫기
              </button>
            </div>
          </div>
        </div>
      )}
    </article>
  );
};
