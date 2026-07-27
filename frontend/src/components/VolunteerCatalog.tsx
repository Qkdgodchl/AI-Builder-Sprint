import React from 'react';
import type { VolunteerItem } from '../types';

interface VolunteerCatalogProps {
  onOpenModal: (title: string, type: 'volunteer' | 'donate') => void;
}

const SAMPLE_ITEMS: VolunteerItem[] = [
  {
    id: 1,
    title: '🐕 유기견 보육원 주말 돌봄 봉사',
    category: 'VOLUNTEER',
    location: '부산 북구 동물보호센터',
    organizer: '부산 동네 온기 봉사단',
    tags: ['1365 연동', '4시간 인정', '주말'],
    link1365: 'https://www.1365.go.kr',
  },
  {
    id: 2,
    title: '🌊 해운대 해변 픽셀 플로깅 정화',
    category: 'VOLUNTEER',
    location: '부산 해운대 구남로 광장',
    organizer: '그린 픽셀 에코 클럽',
    tags: ['1365 연동', '3시간 인정', '환경'],
    link1365: 'https://www.1365.go.kr',
  },
  {
    id: 3,
    title: '🍲 독거어르신 온기 도시락 배달',
    category: 'VOLUNTEER',
    location: '부산 금정구 종합복지관',
    organizer: '사랑의 픽셀 이웃',
    tags: ['1365 연동', '4시간 인정', '복지'],
    link1365: 'https://www.1365.go.kr',
  },
  {
    id: 4,
    title: '❤️ 유기동물 겨울 난방비 픽셀 기부',
    category: 'DONATION',
    location: '부산 동물사랑 기금',
    organizer: '픽셀 케어 공익 펀딩',
    targetAmount: 1000000,
    currentAmount: 850000,
    tags: ['기부 펀딩', '목표 85% 달성'],
  },
  {
    id: 5,
    title: '📚 동네 꿈나무 작은도서관 도서 기부',
    category: 'DONATION',
    location: '부산 사상구 아동센터',
    organizer: '꿈나무 픽셀 희망',
    targetAmount: 500000,
    currentAmount: 320000,
    tags: ['도서 펀딩', '목표 64% 달성'],
  },
  {
    id: 6,
    title: '🌳 우리 동네 픽셀 나무 심기 캠페인',
    category: 'VOLUNTEER',
    location: '부산 시민공원 그린존',
    organizer: '부산광역시 자원봉사센터',
    tags: ['1365 연동', '5시간 인정'],
    link1365: 'https://www.1365.go.kr',
  },
];

export const VolunteerCatalog: React.FC<VolunteerCatalogProps> = ({ onOpenModal }) => {
  return (
    <div>
      <div style={{ marginBottom: '16px', fontSize: '16px', fontWeight: 'bold', color: '#1a1a24' }}>
        🤝 추천 봉사 미션 & 픽셀 기부 펀딩 (1365 공공데이터 연동)
      </div>

      <div className="cards-grid">
        {SAMPLE_ITEMS.map((item) => (
          <div key={item.id} className="pixel-box item-card">
            <div>
              <div className="card-header">
                <span className={`pixel-tag ${item.category === 'VOLUNTEER' ? 'pixel-tag-gov' : ''}`}>
                  {item.category === 'VOLUNTEER' ? '🤝 1365 봉사' : '❤️ 픽셀 기부'}
                </span>
                {item.link1365 && (
                  <a
                    href={item.link1365}
                    target="_blank"
                    rel="noreferrer"
                    style={{ fontSize: '11px', color: '#005f73', textDecoration: 'none' }}
                  >
                    원문보러가기 ↗
                  </a>
                )}
              </div>

              <div className="title">{item.title}</div>
              <div className="meta">
                📍 {item.location}<br />
                🏢 {item.organizer}
              </div>

              <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap', marginBottom: '12px' }}>
                {item.tags.map((t, idx) => (
                  <span key={idx} className="pixel-tag" style={{ background: '#eee', color: '#333' }}>
                    {t}
                  </span>
                ))}
              </div>
            </div>

            <button
              className={`pixel-btn ${item.category === 'DONATION' ? 'pixel-btn-red' : ''}`}
              style={{ width: '100%', fontSize: '12px' }}
              onClick={() =>
                onOpenModal(item.title, item.category === 'DONATION' ? 'donate' : 'volunteer')
              }
            >
              {item.category === 'DONATION' ? '❤️ 픽셀 기부 참여하기' : '⚡ 1초 간편 신청하기'}
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};
