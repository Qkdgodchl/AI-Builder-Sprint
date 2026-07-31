import React, { useState } from 'react';
import type { VolunteerItem } from '../../types';
import {
  createApplication,
  submitCommitment,
} from '../../services/applicationApi';
import { requestClmSign, fetchClmDocument, refreshClmSecureLink } from '../../services/clmApi';
import type { ClmDocumentDto } from '../../services/clmApi';

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
  const documentName = isVolunteer ? '봉사 참여 약정서 (제2026-PC-01호)' : '후원 및 기부 약정서 (제2026-PC-02호)';

  const [specialConditions, setSpecialConditions] = useState('');
  const [privacyConsent, setPrivacyConsent] = useState(false);
  const [thirdPartyConsent, setThirdPartyConsent] = useState(false);
  const [portraitConsent, setPortraitConsent] = useState(false);
  const [confirmed, setConfirmed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [completed, setCompleted] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  // 모두싸인 (Modusign) 전자서명 상태
  const [applicantName, setApplicantName] = useState('부산 픽셀용사');
  const [applicantEmail, setApplicantEmail] = useState('user@pixelcare.com');
  const [clmDoc, setClmDoc] = useState<ClmDocumentDto | null>(null);
  const [isSigningModalOpen, setIsSigningModalOpen] = useState(false);
  const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false);
  const [isDocViewModalOpen, setIsDocViewModalOpen] = useState(false);
  const [isSigned, setIsSigned] = useState(false);
  const [requestingSign, setRequestingSign] = useState(false);
  const [checkingSignature, setCheckingSignature] = useState(false);
  const [signatureStatusMessage, setSignatureStatusMessage] = useState('');

  const canStartSigning = privacyConsent && thirdPartyConsent;

  // 모두싸인 서명 요청 시작
  const handleStartModusign = async () => {
    if (!canStartSigning) {
      alert('필수 동의 항목을 먼저 동의해 주세요.');
      return;
    }

    setRequestingSign(true);
    try {
      const doc = await requestClmSign({
        volunteerId: item.id,
        applicantName: applicantName.trim() || '부산 픽셀용사',
        applicantEmail: applicantEmail.trim() || 'user@pixelcare.com',
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
  };

  const handleSubmit = async () => {
    if (!isSigned) {
      alert('모두싸인 전자서명을 먼저 완료해 주세요!');
      return;
    }

    setSubmitting(true);
    setErrorMessage('');
    try {
      const application = await createApplication(item.id, {
        specialConditions,
        privacyConsent,
        thirdPartyConsent,
        portraitConsent,
      });
      if (application.commitment?.publicId) {
        await submitCommitment(application.commitment.publicId);
      }
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
        <li className="complete">
          <span>01</span>
          <strong>신청 정보</strong>
        </li>
        <li className={privacyConsent && thirdPartyConsent ? 'complete' : 'current'}>
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
                  onChange={(e) => setApplicantName(e.target.value)}
                />
              </label>
              <label style={{ flex: 1 }}>
                신청자 이메일
                <input
                  type="email"
                  className="pixel-input"
                  style={{ width: '100%', padding: '8px 12px', marginTop: '6px' }}
                  value={applicantEmail}
                  onChange={(e) => setApplicantEmail(e.target.value)}
                />
              </label>
            </div>

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
                    onClick={() => setIsDocViewModalOpen(true)}
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

      {/* 1. 약정서 전문 미리보기 모달 */}
      {isPreviewModalOpen && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.7)', display: 'flex', justifyContent: 'center', alignItems: 'center',
          zIndex: 9999, padding: '20px'
        }}>
          <div style={{
            background: '#fffef9', width: '100%', maxWidth: '640px', maxHeight: '85vh',
            borderRadius: '16px', border: '3px solid #111', padding: '28px', overflowY: 'auto',
            boxShadow: '0 10px 30px rgba(0,0,0,0.5)', textAlign: 'left'
          }}>
            <h2 style={{ textAlign: 'center', fontSize: '20px', borderBottom: '2px solid #111', paddingBottom: '12px', marginBottom: '20px' }}>
              📜 {documentName}
            </h2>

            <div style={{ fontSize: '14px', lineHeight: 1.7, color: '#222' }}>
              <p><b>[문서 번호]</b> PC-CLM-2026-0814</p>
              <p><b>[신청 프로그램]</b> {item.title} ({item.organizer})</p>
              <p><b>[신청자]</b> {applicantName} ({applicantEmail})</p>

              <hr style={{ margin: '16px 0', borderColor: '#eee' }} />

              <h4 style={{ color: '#ff70a6' }}>제 1 조 (목적)</h4>
              <p>본 약정은 픽셀 케어(Pixel Care) 플랫폼을 통하여 <b>{item.organizer}</b>이 주관하는 <b>[{item.title}]</b> 활동에 참여 및 후원함에 있어, 신청자와 주관기관 간의 권리와 의무 사항을 규정함을 목적으로 합니다.</p>

              <h4 style={{ color: '#ff70a6' }}>제 2 조 (신청자의 성실 의무 및 안전 수칙)</h4>
              <p>1. 신청자는 동행 및 후원 수칙을 성실히 이행하며, 주관기관의 현장 안내 및 안전 지침을 준수합니다.<br/>
              2. 무단 불참이나 타인에게 피해를 주는 행위를 하지 않으며, 일정 변경 시 사전 통보합니다.</p>

              <h4 style={{ color: '#ff70a6' }}>제 3 조 (픽셀 온기 보상 및 뱃지 자격)</h4>
              <p>본 약정을 완료하고 활동을 성실히 수행한 용사는 픽셀 온기 온도계 +0.5°C 상승 및 픽셀 뱃지 자격을 획득합니다.</p>

              <h4 style={{ color: '#ff70a6' }}>제 4 조 (전자서명의 법적 효력)</h4>
              <p>본 약정서는 모두싸인(Modusign API v2) 규격에 따라 작성되었으며, 전자서명법 제3조에 의하여 서명 날인된 종이 문서와 동일한 법적 효력을 갖습니다.</p>
            </div>

            <div style={{ textAlign: 'center', marginTop: '24px' }}>
              <button
                type="button"
                style={{
                  padding: '10px 24px', background: '#111', color: '#fff',
                  border: 'none', borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer'
                }}
                onClick={() => setIsPreviewModalOpen(false)}
              >
                닫기
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 2. 모두싸인 SECURE_LINK 진행 안내 */}
      {isSigningModalOpen && clmDoc && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0, 0, 0, 0.75)', display: 'flex', justifyContent: 'center', alignItems: 'center',
          zIndex: 9999, padding: '20px'
        }}>
          <div style={{
            background: '#fff', width: '100%', maxWidth: '900px',
            borderRadius: '16px', border: '3px solid #111', padding: '24px',
            boxShadow: '0 10px 30px rgba(0,0,0,0.5)', textAlign: 'center'
          }}>
            <h3 style={{ fontSize: '18px', margin: '0 0 8px 0', color: '#111' }}>
              모두싸인 전자서명
            </h3>
            <p style={{ fontSize: '13px', color: '#555', marginBottom: '16px', lineHeight: 1.4 }}>
              <b>{clmDoc.volunteerTitle}</b> 약정서가 모두싸인 보안 서명창에서 열렸습니다.<br/>
              서명을 완료한 뒤 아래의 ‘서명 완료 확인’을 눌러주세요.
            </p>

            <iframe
              title="모두싸인 보안 전자서명"
              src={clmDoc.signingUrl}
              style={{
                width: '100%',
                height: '520px',
                border: '2px solid #111',
                borderRadius: '8px',
                marginBottom: '16px',
                background: '#fff',
              }}
            />

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <button
                type="button"
                onClick={handleReopenSigning}
                style={{
                  padding: '6px 14px', fontSize: '12px', background: '#fff',
                  border: '1px solid #777', borderRadius: '6px', cursor: 'pointer'
                }}
              >
                서명창 다시 열기
              </button>
              <span style={{ fontSize: '11px', color: '#888' }}>
                문서 코드: {clmDoc.modusignDocumentId}
              </span>
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
                {checkingSignature ? '확인 중...' : '서명 완료 확인'}
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
          background: 'rgba(0,0,0,0.8)', display: 'flex', justifyContent: 'center', alignItems: 'center',
          zIndex: 9999, padding: '20px'
        }}>
          <div style={{
            background: '#fff', width: '100%', maxWidth: '620px', maxHeight: '90vh',
            borderRadius: '16px', border: '3px solid #2ec4b6', padding: '28px', overflowY: 'auto',
            boxShadow: '0 10px 30px rgba(0,0,0,0.6)', textAlign: 'left', position: 'relative'
          }}>
            {/* 공식 직인 뱃지 */}
            <div style={{
              position: 'absolute', top: '24px', right: '24px',
              width: '80px', height: '80px', borderRadius: '50%',
              border: '3px double #ff3b30', color: '#ff3b30', display: 'flex',
              flexDirection: 'column', justifyContent: 'center', alignItems: 'center',
              transform: 'rotate(-12deg)', fontWeight: 'bold', fontSize: '11px', textAlign: 'center',
              background: 'rgba(255,255,255,0.9)'
            }}>
              <span>픽셀케어</span>
              <span>전자서명</span>
              <span>[검증완료]</span>
            </div>

            <h2 style={{ textAlign: 'center', fontSize: '20px', color: '#111', marginBottom: '8px' }}>
              🎖️ 픽셀케어 전자서명 완료 증서
            </h2>
            <p style={{ textAlign: 'center', fontSize: '12px', color: '#666', marginBottom: '24px' }}>
              Modusign API v2 전자서명법 제3조에 의거 보존된 전자약정서입니다.
            </p>

            <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: '20px', fontSize: '13px' }}>
              <tbody>
                <tr>
                  <th style={{ background: '#f5f5f5', padding: '8px 12px', border: '1px solid #ddd', width: '110px' }}>문서번호</th>
                  <td style={{ padding: '8px 12px', border: '1px solid #ddd' }}>{clmDoc?.modusignDocumentId || 'MODU-2026-0814'}</td>
                </tr>
                <tr>
                  <th style={{ background: '#f5f5f5', padding: '8px 12px', border: '1px solid #ddd' }}>신청 프로그램</th>
                  <td style={{ padding: '8px 12px', border: '1px solid #ddd', fontWeight: 'bold' }}>{item.title}</td>
                </tr>
                <tr>
                  <th style={{ background: '#f5f5f5', padding: '8px 12px', border: '1px solid #ddd' }}>주관 기관</th>
                  <td style={{ padding: '8px 12px', border: '1px solid #ddd' }}>{item.organizer}</td>
                </tr>
                <tr>
                  <th style={{ background: '#f5f5f5', padding: '8px 12px', border: '1px solid #ddd' }}>서명인 성명</th>
                  <td style={{ padding: '8px 12px', border: '1px solid #ddd' }}>{applicantName} ({applicantEmail})</td>
                </tr>
                <tr>
                  <th style={{ background: '#f5f5f5', padding: '8px 12px', border: '1px solid #ddd' }}>서명 완료일시</th>
                  <td style={{ padding: '8px 12px', border: '1px solid #ddd', color: '#2ec4b6', fontWeight: 'bold' }}>
                    {new Date().toLocaleString('ko-KR')}
                  </td>
                </tr>
              </tbody>
            </table>

            {/* 서명 원본은 모두싸인에 보존되며 픽셀케어에는 상태와 문서 식별자를 보존한다. */}
            <div style={{
              background: '#fafafa', border: '2px dashed #2ec4b6', borderRadius: '12px',
              padding: '16px', textAlign: 'center', marginBottom: '24px'
            }}>
              <span style={{ fontSize: '12px', color: '#888', display: 'block', marginBottom: '8px' }}>
                [모두싸인 전자서명 검증 상태]
              </span>
              <span style={{ fontSize: '18px', fontWeight: 'bold', color: '#111' }}>서명 완료</span>
            </div>

            <div style={{ textAlign: 'center' }}>
              <button
                type="button"
                style={{
                  padding: '10px 24px', background: '#2ec4b6', color: '#fff',
                  border: 'none', borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer'
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
