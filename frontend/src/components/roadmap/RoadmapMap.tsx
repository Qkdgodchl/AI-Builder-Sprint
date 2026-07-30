import React from 'react';
import type { UserBadge } from '../../types';
import { playBeep } from '../../services/soundFx';

interface RoadmapMapProps {
  showToast: (message: string) => void;
}

const BADGES: UserBadge[] = [
  {
    id: 1,
    level: 1,
    name: 'LV1. 씨앗 용사',
    description: '첫 번째 봉사/기부에 참여한 루키 픽셀 용사',
    iconUrl: '🌱',
    isUnlocked: true,
  },
  {
    id: 2,
    level: 2,
    name: 'LV2. 온기 수호자',
    description: '온기 온도계를 50°C 이상 올린 따뜻한 이웃',
    iconUrl: '🔥',
    isUnlocked: true,
  },
  {
    id: 3,
    level: 3,
    name: 'LV3. 픽셀 나눔이',
    description: '3회 이상 봉사 미션을 완수한 성실한 영웅',
    iconUrl: '⭐',
    isUnlocked: true,
  },
  {
    id: 4,
    level: 4,
    name: 'LV4. 동네 영웅',
    description: '봉사 10시간 이상 달성한 기사',
    iconUrl: '🛡️',
    isUnlocked: false,
  },
  {
    id: 5,
    level: 5,
    name: 'LV5. 픽셀 마스터',
    description: '세상을 바꾼 최고의 픽셀 케어 전설 레전드',
    iconUrl: '👑',
    isUnlocked: false,
  },
];

export const RoadmapMap: React.FC<RoadmapMapProps> = ({ showToast }) => {
  const handleBadgeClick = (badge: UserBadge) => {
    playBeep(badge.isUnlocked ? 659 : 220, 0.1);
    if (badge.isUnlocked) {
      showToast(`🏆 [${badge.name}] 뱃지를 보유 중입니다: "${badge.description}"`);
    } else {
      showToast(`🔒 [${badge.name}] 미해금 뱃지: 봉사 퀘스트를 더 진행하여 해금하세요!`);
    }
  };

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div className="pixel-box" style={{ marginBottom: '20px', textAlign: 'center' }}>
        <div style={{ fontSize: '18px', fontWeight: 'bold', marginBottom: '6px', color: '#1a1a24' }}>
          🗺️ 픽셀 성장의 길 & 뱃지 도감
        </div>
        <div style={{ fontSize: '12px', color: '#555' }}>
          작은 선행(Pixel)을 쌓아 레벨업하고 픽셀 트로피 뱃지를 모아보세요!
        </div>
      </div>

      {/* Grid Badges */}
      <div className="cards-grid">
        {BADGES.map((b) => (
          <div
            key={b.id}
            className="pixel-box"
            style={{
              textAlign: 'center',
              padding: '20px 14px',
              background: b.isUnlocked ? '#fff' : '#e9ecef',
              opacity: b.isUnlocked ? 1 : 0.6,
              cursor: 'pointer',
            }}
            onClick={() => handleBadgeClick(b)}
          >
            <div style={{ fontSize: '40px', marginBottom: '8px' }}>
              {b.isUnlocked ? b.iconUrl : '🔒'}
            </div>
            <div style={{ fontSize: '14px', fontWeight: 'bold', color: '#1a1a24', marginBottom: '4px' }}>
              {b.name}
            </div>
            <div style={{ fontSize: '11px', color: '#666', lineHeight: 1.4 }}>
              {b.description}
            </div>
            <div style={{ marginTop: '10px' }}>
              <span className={`pixel-tag ${b.isUnlocked ? 'pixel-tag-gov' : ''}`}>
                {b.isUnlocked ? '✅ 획득 완료' : '🔒 미해금'}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
