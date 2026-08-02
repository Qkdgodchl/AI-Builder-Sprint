import React from 'react';
import type { VolunteerItem } from '../../types';

interface DetailItem extends VolunteerItem {
  programType: string;
  availability: string;
}

interface OpportunityDetailProps {
  item: DetailItem;
  onBack: () => void;
  onApply: () => void;
  canDelete: boolean;
  onDelete: () => void;
  /** 같은 구분의 다른 프로그램. 상세에서 곧바로 이어 볼 수 있게 한다. */
  related?: DetailItem[];
  onSelectRelated?: (id: number) => void;
}

const descriptions: Record<string, string> = {
  VOLUNTEER:
    '지역사회에 필요한 활동에 직접 참여하는 봉사 프로그램입니다. 신청 후 주관기관의 안내에 따라 활동 일정과 준비사항을 확인할 수 있습니다.',
  GENERAL:
    '도움이 필요한 이웃과 공익사업을 지속적으로 지원하는 기부 프로그램입니다. 원하는 금액으로 참여하고 후원 내역을 관리할 수 있습니다.',
  LEGACY:
    '삶의 가치와 나눔의 뜻을 다음 세대에 전하는 유산기부 상담 프로그램입니다. 전문 상담을 통해 기부 의사와 약정 방식을 충분히 검토할 수 있습니다.',
  UNESCO:
    '세계가 함께 보호해야 할 유산의 보존과 교육 사업을 지원하는 후원 프로그램입니다. 후원금은 지속 가능한 보존 활동에 사용됩니다.',
  HERITAGE:
    '우리 지역의 문화유산을 보존하고 다음 세대에 전하기 위한 후원 프로그램입니다. 복원, 기록, 시민 참여 사업을 함께 지원합니다.',
  HOMETOWN:
    '현재 거주지를 제외한 관심 지역에 기부하고 지역 공동체 사업을 지원하는 고향사랑기부 프로그램입니다.',
};

const notices: Record<string, string[]> = {
  VOLUNTEER: [
    '활동 일정과 집결 장소를 신청 전에 확인해 주세요.',
    '봉사시간 인정 여부는 주관기관의 최종 승인 기준을 따릅니다.',
    '신청 후 기관에서 준비물과 세부 일정을 안내합니다.',
  ],
  LEGACY: [
    '신청 단계에서는 상담 의사만 접수되며 약정이 즉시 체결되지 않습니다.',
    '전문 상담 이후 본인의 의사에 따라 약정 절차를 진행합니다.',
    '가족과 법률·세무 전문가의 충분한 검토를 권장합니다.',
  ],
  HOMETOWN: [
    '현재 주민등록상 거주 지역에는 기부할 수 없습니다.',
    '기부 가능 금액과 세액공제 기준은 공식 안내를 확인해 주세요.',
    '최종 기부는 연계되는 공식 절차에서 완료됩니다.',
  ],
  DEFAULT: [
    '후원 목적과 사용 계획을 확인한 뒤 신청해 주세요.',
    '결제 또는 약정 전 최종 내용을 다시 확인할 수 있습니다.',
    '신청 내역은 로그인 후 마이페이지에서 확인할 수 있습니다.',
  ],
};

/**
 * 괄호 안 설명이 '(감천마을 재 / 생)'처럼 갈라지지 않도록
 * 괄호 내부 공백만 줄바꿈 없는 공백으로 바꾼다.
 */
const keepParentheticalTogether = (title: string) =>
  title.replace(/\(([^)]*)\)/g, (_match, inner: string) => `(${inner.replace(/ /g, '\u00A0')})`);

const formatDateTime = (value?: string) => {
  if (!value) return null;
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return null;
  return parsed.toLocaleString('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export const OpportunityDetail: React.FC<OpportunityDetailProps> = ({
  item,
  onBack,
  onApply,
  canDelete,
  onDelete,
  related = [],
  onSelectRelated,
}) => {
  const programNotices = notices[item.programType] ?? notices.DEFAULT;
  const isVolunteer = item.category === 'VOLUNTEER';

  const target = item.targetAmount ?? 0;
  const raised = item.currentAmount ?? 0;
  const fundingPercent = target > 0 ? Math.min(100, Math.round((raised / target) * 100)) : null;

  const capacity = item.recruitmentCapacity ?? 0;
  const applicants = item.applicantCount ?? 0;
  const seatsLeft = capacity > 0 ? Math.max(0, capacity - applicants) : null;

  const schedule = [
    { label: '모집 마감', value: formatDateTime(item.recruitmentEndDateTime) },
    { label: '활동 시작', value: formatDateTime(item.activityStartDateTime) },
    { label: '활동 종료', value: formatDateTime(item.activityEndDateTime) },
  ].filter((entry) => entry.value);

  const documents = item.requiredDocuments ?? [];
  let sectionNumber = 0;
  const nextNumber = () => String(++sectionNumber).padStart(2, '0');

  return (
    <article className="opportunity-detail">
      <div className="detail-back-nav">
        <button type="button" className="detail-back-button" onClick={onBack}>
          목록으로 돌아가기
        </button>
        {canDelete && (
          <button type="button" className="content-delete-button" onClick={onDelete}>
            프로그램 삭제
          </button>
        )}
      </div>

      <header className="detail-hero">
        <h2>{keepParentheticalTogether(item.title)}</h2>
        <p>{item.summary?.trim() || descriptions[item.programType] || descriptions.GENERAL}</p>
        <div className="detail-inline-keywords" aria-label="관련 키워드">
          {item.tags.map((tag) => (
            <span key={tag}>#{tag.replace(/\s+/g, '')}</span>
          ))}
        </div>
      </header>

      <dl className="detail-facts">
        <div>
          <dt>주관기관</dt>
          <dd>{item.organizer}</dd>
        </div>
        <div>
          <dt>활동·후원 지역</dt>
          <dd>{item.location}</dd>
        </div>
        <div>
          <dt>진행 상태</dt>
          <dd>{item.availability}</dd>
        </div>
        <div>
          <dt>참여 방식</dt>
          <dd>{isVolunteer ? '온라인 신청 후 기관 승인' : '온라인 신청 또는 상담'}</dd>
        </div>
      </dl>

      {(fundingPercent !== null || capacity > 0) && (
        <section className="detail-progress">
          {fundingPercent !== null && (
            <div className="detail-progress-main">
              <span>모금 현황</span>
              <strong>{fundingPercent}%</strong>
              <div className="detail-progress-track">
                <span style={{ width: `${fundingPercent}%` }} />
              </div>
              <p>
                {raised.toLocaleString()}원 모금 · 목표 {target.toLocaleString()}원
              </p>
            </div>
          )}
          {capacity > 0 && (
            <div className="detail-progress-side">
              <div>
                <span>모집 인원</span>
                <strong>{capacity}명</strong>
              </div>
              <div>
                <span>현재 신청</span>
                <strong>{applicants}명</strong>
              </div>
              <div className={seatsLeft === 0 ? 'warn' : ''}>
                <span>남은 자리</span>
                <strong>{seatsLeft}명</strong>
              </div>
            </div>
          )}
        </section>
      )}

      <section className="detail-section">
        <p className="detail-section-number">{nextNumber()}</p>
        <div>
          <h3>이 프로그램은</h3>
          {/* 기관이 등록한 실제 소개글. 없을 때만 구분별 기본 설명으로 대체한다. */}
          <p>{item.description?.trim() || descriptions[item.programType] || descriptions.GENERAL}</p>
        </div>
      </section>

      <section className="detail-section">
        <p className="detail-section-number">{nextNumber()}</p>
        <div>
          <h3>참여 절차</h3>
          <ol className="detail-steps">
            {(isVolunteer
              ? [
                  '신청서를 작성해 참여 의사를 전달합니다.',
                  '주관기관이 참여 가능 여부를 확인하고 승인합니다.',
                  '확정되면 활동 시간·장소·준비물을 안내받습니다.',
                  '활동을 마치면 참여 기록이 내 기록에 남습니다.',
                ]
              : [
                  'AI 상담으로 후원 금액과 주기를 정리합니다.',
                  '정리된 내용으로 약정서를 확인하고 수정합니다.',
                  '모두싸인 전자서명으로 약정을 체결합니다.',
                  '서명 완료본과 감사추적증명서가 보관됩니다.',
                ]
            ).map((step, index) => (
              <li key={step}>
                <span>{String(index + 1).padStart(2, '0')}</span>
                <p>{step}</p>
              </li>
            ))}
          </ol>
        </div>
      </section>

      {schedule.length > 0 && (
        <section className="detail-section">
          <p className="detail-section-number">{nextNumber()}</p>
          <div>
            <h3>일정</h3>
            <ol className="detail-timeline">
              {schedule.map((entry) => (
                <li key={entry.label}>
                  <span>{entry.label}</span>
                  <strong>{entry.value}</strong>
                </li>
              ))}
            </ol>
          </div>
        </section>
      )}

      {documents.length > 0 && (
        <section className="detail-section">
          <p className="detail-section-number">{nextNumber()}</p>
          <div>
            <h3>제출 서류</h3>
            <ul className="detail-document-list">
              {documents.map((document) => (
                <li key={document.code}>
                  <strong>{document.name}</strong>
                  <em>{document.required ? '필수' : '선택'}</em>
                  {document.description && <p>{document.description}</p>}
                </li>
              ))}
            </ul>
          </div>
        </section>
      )}

      <section className="detail-section">
        <p className="detail-section-number">{nextNumber()}</p>
        <div>
          <h3>신청 전 확인사항</h3>
          <ul>
            {programNotices.map((notice) => (
              <li key={notice}>{notice}</li>
            ))}
          </ul>
        </div>
      </section>

      <footer className="detail-apply-bar">
        <div>
          <span>READY TO JOIN</span>
          <strong>{isVolunteer ? '이 봉사에 참여하시겠어요?' : '이 프로그램을 후원하시겠어요?'}</strong>
        </div>
        <button type="button" onClick={onApply}>
          {isVolunteer ? '봉사 신청하기' : '후원 신청하기'}
        </button>
      </footer>

      {related.length > 0 && onSelectRelated && (
        <section className="detail-related">
          <div className="detail-related-heading">
            <span>RELATED</span>
            <h3>비슷한 프로그램</h3>
          </div>
          <div className="detail-related-grid">
            {related.map((program) => (
              <button type="button" key={program.id} onClick={() => onSelectRelated(program.id)}>
                <span>{program.availability}</span>
                <strong>{program.title}</strong>
                <small>{program.location}</small>
              </button>
            ))}
          </div>
        </section>
      )}
    </article>
  );
};
