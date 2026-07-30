import React, { useState, useRef } from 'react';
import type { VolunteerItem } from '../../types';
import {
  createApplication,
  submitCommitment,
} from '../../services/applicationApi';
import { requestClmSign, completeClmSign } from '../../services/clmApi';
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
  const documentName = isVolunteer ? '봉사 참여 약정서' : '후원 및 기부 약정서';
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
  const [isSigned, setIsSigned] = useState(false);
  const [requestingSign, setRequestingSign] = useState(false);

  // 캔버스 마우스/터치 서명 관련 상태
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [hasDrawn, setHasDrawn] = useState(false);

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

      if (doc) {
        setClmDoc(doc);
        setIsSigningModalOpen(true);
        setHasDrawn(false);
      } else {
        alert('모두싸인 서명 요청 문서 생성에 실패했습니다.');
      }
    } catch (err) {
      console.error(err);
      alert('서명 요청 처리 중 오류가 발생했습니다.');
    } finally {
      setRequestingSign(false);
    }
  };

  // 캔버스 드로잉 로직 (마우스 / 터치)
  const startDrawing = (e: React.MouseEvent<HTMLCanvasElement> | React.TouchEvent<HTMLCanvasElement>) => {
    setIsDrawing(true);
    draw(e);
  };

  const stopDrawing = () => {
    setIsDrawing(false);
    const canvas = canvasRef.current;
    if (canvas) {
      const ctx = canvas.getContext('2d');
      if (ctx) ctx.beginPath();
    }
  };

  const draw = (e: React.MouseEvent<HTMLCanvasElement> | React.TouchEvent<HTMLCanvasElement>) => {
    if (!isDrawing && e.type !== 'mousedown' && e.type !== 'touchstart') return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const rect = canvas.getBoundingClientRect();
    let clientX = 0;
    let clientY = 0;

    if ('touches' in e) {
      clientX = e.touches[0].clientX;
      clientY = e.touches[0].clientY;
    } else {
      const mouseEvent = e as React.MouseEvent<HTMLCanvasElement>;
      clientX = mouseEvent.clientX;
      clientY = mouseEvent.clientY;
    }

    const x = clientX - rect.left;
    const y = clientY - rect.top;

    ctx.lineWidth = 3;
    ctx.lineCap = 'round';
    ctx.strokeStyle = '#111';

    ctx.lineTo(x, y);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(x, y);
    setHasDrawn(true);
  };

  const clearCanvas = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    setHasDrawn(false);
  };

  // 전자서명 최종 제출 처리
  const handleConfirmSignature = async () => {
    if (!clmDoc) return;
    if (!hasDrawn) {
      alert('마우스 또는 터치로 캔버스에 직접 자필 서명을 남겨주세요!');
      return;
    }

    try {
      const updated = await completeClmSign(clmDoc.id);
      if (updated && updated.status === 'SIGNED') {
        setIsSigned(true);
        setIsSigningModalOpen(false);
        alert('✍️ 마우스 자필 전자서명이 작성되었으며 모두싸인 CLM DB에 보존 처리되었습니다!');
      }
    } catch (err) {
      console.error(err);
      alert('서명 완료 처리 중 오류가 발생했습니다.');
    }
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
                <h3>서류 작성 & 신청자 정보</h3>
              </div>
              <span className={`clm-status ${canStartSigning ? 'complete' : 'pending'}`}>
                {canStartSigning ? '작성 완료' : '작성 중'}
              </span>
            </div>

            <div className="clm-document-card">
              <div>
                <span>필수 서류</span>
                <strong>{documentName} (모두싸인 연동)</strong>
                <p>
                  참여 조건, 활동 또는 후원 내용, 개인정보 동의 항목이 포함됩니다.
                </p>
              </div>
              <span className="clm-status complete">온라인 작성</span>
            </div>

            <div style={{ display: 'flex', gap: '12px', marginBottom: '16px' }}>
              <label style={{ flex: 1 }}>
                신청자 이름
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
                rows={4}
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
              <span>활동 사진의 초상권 활용에 동의합니다. (선택)</span>
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
              <strong>{isSigned ? '✅ 자필 전자서명 완료됨' : '모두싸인 자필 전자서명'}</strong>
              <p>
                {isSigned
                  ? `문서 ID: ${clmDoc?.modusignDocumentId || 'MODU_SIGNED'} · 법적 효력이 있는 서명이 보존되었습니다.`
                  : '약정서를 확인한 뒤 마우스 또는 손가락으로 자필 전자서명을 작성합니다.'}
              </p>

              {!isSigned && (
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
                  {requestingSign ? '서명 창 로딩 중...' : '✍️ 모두싸인 자필 서명하기 (마우스 드로잉)'}
                </button>
              )}

              {isSigned && (
                <div style={{ color: '#2ec4b6', fontWeight: 'bold', fontSize: '15px', marginTop: '10px' }}>
                  ✍️ 서명 완료일시: {new Date().toLocaleString('ko-KR')}
                </div>
              )}
            </div>
            <p className="clm-placeholder-note">
              모두싸인(Modusign API v2) 공식 규격과 연동하여 마우스 자필 서명이 CLM DB에 안전하게 보존됩니다.
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

      {/* 모두싸인 자필 서명 마우스 캔버스 모달 */}
      {isSigningModalOpen && clmDoc && (
        <div style={{
          position: 'fixed',
          top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0, 0, 0, 0.75)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 9999,
          padding: '20px'
        }}>
          <div style={{
            background: '#fff',
            width: '100%',
            maxWidth: '560px',
            borderRadius: '16px',
            border: '3px solid #111',
            padding: '24px',
            boxShadow: '0 10px 30px rgba(0,0,0,0.5)',
            textAlign: 'center'
          }}>
            <h3 style={{ fontSize: '18px', margin: '0 0 8px 0', color: '#111' }}>
              ✍️ 모두싸인 (Modusign) 자필 전자서명 작성
            </h3>
            <p style={{ fontSize: '13px', color: '#555', marginBottom: '16px', lineHeight: 1.4 }}>
              <b>{clmDoc.volunteerTitle}</b> 약정서 서명 패드입니다.<br/>
              <span style={{ color: '#ff70a6', fontWeight: 'bold' }}>아래 하얀 창에 마우스나 손가락으로 직접 서명을 그려주세요!</span>
            </p>

            {/* 마우스/터치 자필 서명 HTML5 Canvas */}
            <div style={{ position: 'relative', marginBottom: '16px' }}>
              <canvas
                ref={canvasRef}
                width={500}
                height={180}
                onMouseDown={startDrawing}
                onMouseUp={stopDrawing}
                onMouseLeave={stopDrawing}
                onMouseMove={draw}
                onTouchStart={startDrawing}
                onTouchEnd={stopDrawing}
                onTouchMove={draw}
                style={{
                  border: '2px dashed #ff70a6',
                  background: '#ffffff',
                  borderRadius: '12px',
                  cursor: 'crosshair',
                  touchAction: 'none',
                  display: 'block',
                  margin: '0 auto'
                }}
              />
              {!hasDrawn && (
                <div style={{
                  position: 'absolute',
                  top: '50%',
                  left: '50%',
                  transform: 'translate(-50%, -50%)',
                  pointerEvents: 'none',
                  color: '#aaa',
                  fontSize: '14px',
                  fontWeight: 'bold'
                }}>
                  🖊️ 마우스로 이곳에 서명하세요
                </div>
              )}
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <button
                type="button"
                onClick={clearCanvas}
                style={{
                  padding: '6px 14px',
                  fontSize: '12px',
                  background: '#fff',
                  border: '1px solid #777',
                  borderRadius: '6px',
                  cursor: 'pointer'
                }}
              >
                🔄 서명 다시 그리기 (지우기)
              </button>
              <span style={{ fontSize: '11px', color: '#888' }}>
                문서 코드: {clmDoc.modusignDocumentId}
              </span>
            </div>

            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
              <button
                type="button"
                style={{
                  padding: '12px 20px',
                  background: '#ccc',
                  border: '2px solid #111',
                  borderRadius: '8px',
                  fontWeight: 'bold',
                  cursor: 'pointer'
                }}
                onClick={() => setIsSigningModalOpen(false)}
              >
                취소
              </button>
              <button
                type="button"
                style={{
                  padding: '12px 24px',
                  background: hasDrawn ? '#2ec4b6' : '#aaa',
                  color: '#fff',
                  border: '2px solid #111',
                  borderRadius: '8px',
                  fontWeight: 'bold',
                  cursor: hasDrawn ? 'pointer' : 'not-allowed'
                }}
                disabled={!hasDrawn}
                onClick={handleConfirmSignature}
              >
                ✍️ 서명 제출 및 완성
              </button>
            </div>
          </div>
        </div>
      )}
    </article>
  );
};
