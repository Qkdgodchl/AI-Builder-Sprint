import React from 'react';
import type { VolunteerItem } from '../../types';

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
              <button type="button" disabled>
                서류 작성 시작
              </button>
            </div>
            <p className="clm-placeholder-note">
              약정서 입력 화면은 다음 구현 단계에서 연결됩니다.
            </p>
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
              전자서명 제공업체 연동은 다음 구현 단계에서 연결됩니다.
            </p>
          </section>
        </div>

        <aside className="clm-submit-panel">
          <p>신청 완료 조건</p>
          <ul>
            <li className="complete">신청 프로그램 확인</li>
            <li>필수 약정서 작성</li>
            <li>개인정보 동의</li>
            <li>전자서명 완료</li>
          </ul>
          <div className="clm-submit-divider" />
          <label className="clm-final-consent">
            <input type="checkbox" disabled />
            <span>작성 내용과 약정 조건을 모두 확인했습니다.</span>
          </label>
          <button type="button" className="clm-final-submit" disabled>
            작성 완료 및 신청
          </button>
          <small>
            서류 작성과 전자서명을 완료하면 최종 신청 버튼이 활성화됩니다.
          </small>
        </aside>
      </div>
    </article>
  );
};
