import React, { useState } from 'react';
import type { UserBadge } from '../types';
import { playBeep } from '../services/soundFx';

interface RoadmapMapProps {
  showToast: (msg: string) => void;
}

const BADGES: UserBadge[] = [
  { id: 1, level: 1, name: '시작의 픽셀', description: '첫 1초 간편 봉사/기부 참가 완료', iconUrl: '🌱', isUnlocked: true },
  { id: 2, level: 2, name: '동네 온기 용사', description: '누적 봉사 5시간 달성', iconUrl: '🛡️', isUnlocked: true },
  { id: 3, level: 3, name: '빛나는 선행가', description: '픽셀 일기장 3회 작성', iconUrl: '⭐', isUnlocked: true },
  { id: 4, level: 4, name: '마을 수호자', description: '누적 기부금 50,000원 달성', iconUrl: '👑', isUnlocked: false },
  { id: 5, level: 5, name: '전설의 픽셀 영웅', description: '온기 온도계 99.9°C 도달', iconUrl: '🏆', isUnlocked: false },
];

export const RoadmapMap: React.FC<RoadmapMapProps> = ({ showToast }) => {
  const [spritePos, setSpritePos] = useState({ left: 15, top: 75 });
  const [activeLevel, setActiveLevel] = useState(1);

  const handleNodeClick = (level: number, left: number, top: number, name: string) => {
    setSpritePos({ left, top });
    setActiveLevel(level);
    showToast(`🏃 LV${level} [${name}] 위치로 이동했습니다!`);
    playBeep(440, 0.15);
  };

  return (
    <div>
      <div style={{ fontSize: '16px', fontWeight: 'bold', marginBottom: '14px', color: '#1a1a24' }}>
        🗺️ 성장의 길 (S자 픽셀 로드맵 & 뱃지 도감)
      </div>

      {/* S-Curve Path Roadmap */}
      <div className="roadmap-container">
        <svg className="svg-path-bg" viewBox="0 0 100 100" preserveAspectRatio="none">
          <path
            d="M 15 75 C 40 75, 40 45, 50 45 C 60 45, 60 15, 85 15"
            fill="none"
            stroke="#383944"
            strokeWidth="10"
            strokeLinecap="round"
          />
          <path
            d="M 15 75 C 40 75, 40 45, 50 45 C 60 45, 60 15, 85 15"
            fill="none"
            stroke="#e4e2d4"
            strokeWidth="2"
            strokeDasharray="3 3"
          />
        </svg>

        {/* Player Sprite */}
        <div
          className="player-sprite"
          style={{ left: `${spritePos.left}%`, top: `${spritePos.top}%` }}
        >
          🧙
        </div>

        {/* Nodes */}
        <div className="roadmap-nodes">
          <div
            className="node-pin unlocked"
            style={{ left: '15%', top: '75%' }}
            onClick={() => handleNodeClick(1, 15, 75, 'LV1 시작의 픽셀')}
          >
            🌱
          </div>
          <div
            className="node-pin unlocked"
            style={{ left: '35%', top: '60%' }}
            onClick={() => handleNodeClick(2, 35, 60, 'LV2 동네 온기 용사')}
          >
            🛡️
          </div>
          <div
            className="node-pin unlocked"
            style={{ left: '50%', top: '45%' }}
            onClick={() => handleNodeClick(3, 50, 45, 'LV3 빛나는 선행가')}
          >
            ⭐
          </div>
          <div
            className="node-pin locked"
            style={{ left: '68%', top: '30%' }}
            onClick={() => handleNodeClick(4, 68, 30, 'LV4 마을 수호자')}
          >
            🔒
          </div>
          <div
            className="node-pin locked"
            style={{ left: '85%', top: '15%' }}
            onClick={() => handleNodeClick(5, 85, 15, 'LV5 전설의 픽셀 영웅')}
          >
            🏆
          </div>
        </div>
      </div>

      {/* Badge List */}
      <div style={{ marginTop: '20px' }}>
        <div style={{ fontWeight: 'bold', marginBottom: '10px' }}>🏆 픽셀 뱃지 도감</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '10px' }}>
          {BADGES.map((b) => (
            <div
              key={b.id}
              className="pixel-box"
              style={{
                padding: '10px',
                textAlign: 'center',
                opacity: b.isUnlocked ? 1 : 0.5,
                background: b.id === activeLevel ? '#fff9d6' : '#ffffff',
              }}
            >
              <div style={{ fontSize: '24px', marginBottom: '4px' }}>{b.iconUrl}</div>
              <div style={{ fontSize: '12px', fontWeight: 'bold' }}>
                LV{b.level}. {b.name}
              </div>
              <div style={{ fontSize: '10px', color: '#666', marginTop: '2px' }}>{b.description}</div>
              <div style={{ marginTop: '6px' }}>
                <span
                  className="pixel-tag"
                  style={{
                    background: b.isUnlocked ? '#3da047' : '#999',
                    fontSize: '9px',
                  }}
                >
                  {b.isUnlocked ? '획득 완료' : '잠김'}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
