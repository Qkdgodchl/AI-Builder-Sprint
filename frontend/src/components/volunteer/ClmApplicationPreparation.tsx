import React, { useState } from 'react';
import type { VolunteerItem } from '../../types';
import {
  createApplication,
  submitCommitment,
} from '../../services/applicationApi';

interface ApplicationItem extends VolunteerItem {
  programType: string;
  availability: string;
}

interface ClmApplicationPreparationProps {
  item: ApplicationItem;
  typeLabel: string;
  onBack: () => void;
}

const steps = [
  { number: '01', label: '신청 정보', state: 'complete' },
  { number: '02', label: '서류 작성', state: 'current' },
  { number: '03', label: '전자서명', state: 'upcoming' },
  { number: '04', label: '신청 완료', state: 'upcoming' },
] as const;

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

  const handleSubmit = async () => {
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
        <span>CLM APPLICATION</span>
      </div>

      <header className="clm-header">
        <p>약정서 작성 및 전자서명</p>
        <h2>신청 서류 작성</h2>
        <span>
          신청 정보를 바탕으로 약정서를 작성하고 전자서명을 완료하면 최종 신청이 접수됩니다.
        </span>
      </header>

      <ol className="clm-steps" aria-label="신청 진행 단계">
        {steps.map((step) => (
          <li key={step.number} className={step.state} aria-current={step.state === 'current' ? 'step' : undefined}>
            <span>{step.number}</span>
            <strong>{step.label}</strong>
          </li>
        ))}
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
                <h3>서류 작성</h3>
              </div>
              <span className="clm-status pending">작성 전</span>
            </div>

            <div className="clm-document-card">
              <div>
                <span>필수 서류</span>
                <strong>{documentName}</strong>
                <p>
                  참여 조건, 활동 또는 후원 내용, 개인정보 동의 항목이 포함됩니다.
                </p>
              </div>
              <span className="clm-status pending">온라인 작성</span>
            </div>
            <label>
              특별 조건 및 전달사항
              <textarea
                value={specialConditions}
                onChange={(event) => setSpecialConditions(event.target.value)}
                placeholder="참여 가능한 시간이나 기관에 전달할 내용을 입력해주세요."
                rows={5}
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
                <h3>전자서명</h3>
              </div>
              <span className="clm-status locked">서류 작성 후 가능</span>
            </div>

            <div className="clm-signature-placeholder">
              <span>E-SIGNATURE AREA</span>
              <strong>전자서명 영역</strong>
              <p>작성된 약정서를 확인한 뒤 본인 인증과 전자서명을 진행합니다.</p>
              <button type="button" disabled>
                전자서명 시작
              </button>
            </div>
            <p className="clm-placeholder-note">
              전자서명 제공업체 연동 전까지는 온라인 약정 제출 상태로 저장됩니다.
            </p>
          </section>
        </div>

        <aside className="clm-submit-panel">
          <p>신청 완료 조건</p>
          <ul>
            <li className="complete">신청 프로그램 확인</li>
            <li className={specialConditions || confirmed ? 'complete' : ''}>필수 약정서 작성</li>
            <li className={privacyConsent && thirdPartyConsent ? 'complete' : ''}>개인정보 동의</li>
            <li>전자서명 완료</li>
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
            disabled={!privacyConsent || !thirdPartyConsent || !confirmed || submitting || completed}
            onClick={handleSubmit}
          >
            {completed ? '신청 접수 완료' : submitting ? '신청 중...' : '작성 완료 및 신청'}
          </button>
          {errorMessage && <small className="auth-form-error">{errorMessage}</small>}
          <small>
            {completed
              ? '신청과 약정 초안이 MySQL에 저장됐습니다.'
              : '필수 동의와 작성 내용 확인 후 신청할 수 있습니다.'}
          </small>
        </aside>
      </div>
    </article>
  );
};
