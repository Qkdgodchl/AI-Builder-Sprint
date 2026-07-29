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

export const OpportunityDetail: React.FC<OpportunityDetailProps> = ({
  item,
  onBack,
  onApply,
}) => {
  const programNotices = notices[item.programType] ?? notices.DEFAULT;
  const isVolunteer = item.category === 'VOLUNTEER';

  return (
    <article className="opportunity-detail">
      <div className="detail-back-nav">
        <button type="button" className="detail-back-button" onClick={onBack}>
          목록으로 돌아가기
        </button>
      </div>

      <header className="detail-hero">
        <h2>{item.title}</h2>
        <p>{descriptions[item.programType] ?? descriptions.GENERAL}</p>
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

      <section className="detail-section">
        <p className="detail-section-number">01</p>
        <div>
          <h3>프로그램 안내</h3>
          <p>
            {isVolunteer
              ? '신청이 접수되면 주관기관에서 참여 가능 여부를 확인합니다. 확정된 참여자에게 활동 시간, 장소, 준비사항을 별도로 안내합니다.'
              : '신청서에 입력한 내용을 바탕으로 후원 또는 상담 절차가 시작됩니다. 금액과 약정 조건은 최종 확인 전까지 자유롭게 검토할 수 있습니다.'}
          </p>
        </div>
      </section>

      <section className="detail-section">
        <p className="detail-section-number">02</p>
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
    </article>
  );
};
