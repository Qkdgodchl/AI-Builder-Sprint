import React from 'react';
import { playBeep } from '../services/soundFx';

interface HeaderProps {
  temperature: number;
  totalDonation: number;
  totalHours: number;
  totalMembers: number;
  activeTab: 'ai' | 'volunteer' | 'diary' | 'roadmap';
  setActiveTab: (tab: 'ai' | 'volunteer' | 'diary' | 'roadmap') => void;
}

export const Header: React.FC<HeaderProps> = ({
  temperature,
  totalDonation,
  totalHours,
  totalMembers,
  activeTab,
  setActiveTab,
}) => {
  const handleTabChange = (tab: 'ai' | 'volunteer' | 'diary' | 'roadmap') => {
    setActiveTab(tab);
    playBeep(480, 0.1);
  };

  return (
    <header>
      <div className="main-title">
        <span>🎮</span> 픽셀 케어 (Pixel Care)
      </div>
      <div className="subtitle">
        *"하나하나의 픽셀이 모여 만드는 세상에서 가장 따뜻한 공간"*
      </div>

      <div style={{ marginTop: '16px' }}>
        {/* Thermometer */}
        <div className="thermometer-card">
          <div className="thermo-label">
            <span>🔥</span> 우리 동네 픽셀 온기:
          </div>
          <div className="thermo-bar-outer">
            <div
              className="thermo-bar-inner"
              style={{ width: `${Math.min(100, temperature)}%` }}
            ></div>
          </div>
          <div className="thermo-value">{temperature.toFixed(1)} °C</div>
        </div>

        {/* Stats */}
        <div className="stats-bar">
          <div className="pixel-box stat-card">
            <div className="label">총 누적 픽셀 기부금</div>
            <div className="value">₩{totalDonation.toLocaleString()}</div>
          </div>
          <div className="pixel-box stat-card">
            <div className="label">누적 봉사 시간</div>
            <div className="value">{totalHours} 시간</div>
          </div>
          <div className="pixel-box stat-card">
            <div className="label">참여 온기 영웅 수</div>
            <div className="value">{totalMembers} 명</div>
          </div>
        </div>

        {/* Navigation Tabs */}
        <nav className="nav-tabs">
          <button
            className={`tab-btn ${activeTab === 'ai' ? 'active' : ''}`}
            onClick={() => handleTabChange('ai')}
          >
            🤖 픽셀 AI 대화창
          </button>
          <button
            className={`tab-btn ${activeTab === 'volunteer' ? 'active' : ''}`}
            onClick={() => handleTabChange('volunteer')}
          >
            🤝 봉사 & 기부
          </button>
          <button
            className={`tab-btn ${activeTab === 'diary' ? 'active' : ''}`}
            onClick={() => handleTabChange('diary')}
          >
            📖 픽셀 일기장
          </button>
          <button
            className={`tab-btn ${activeTab === 'roadmap' ? 'active' : ''}`}
            onClick={() => handleTabChange('roadmap')}
          >
            🗺️ 성장의 길
          </button>
        </nav>
      </div>
    </header>
  );
};
