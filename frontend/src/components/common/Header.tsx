import React from 'react';

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
  return (
    <header style={{ marginBottom: '20px' }}>
      <div className="pixel-box header-banner" style={{ background: '#2ec4b6', color: '#1a1a24' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <span style={{ fontSize: '32px' }}>👾</span>
          <div>
            <h1 style={{ fontSize: '20px', margin: 0, fontWeight: 'bold' }}>픽셀 케어 (Pixel Care)</h1>
            <div style={{ fontSize: '11px', color: '#005f73' }}>
              하나하나의 픽셀이 모여 만드는 세상에서 가장 따뜻한 공간
            </div>
          </div>
        </div>

        {/* Thermometer UI */}
        <div className="thermometer-box">
          <div style={{ fontSize: '11px', fontWeight: 'bold', marginBottom: '4px', textAlign: 'right' }}>
            🌡️ 우리 동네 온기 온도계: <span style={{ color: '#e76f51', fontSize: '14px' }}>{temperature.toFixed(1)}°C</span>
          </div>
          <div className="pixel-progress-bg">
            <div
              className="pixel-progress-fill"
              style={{ width: `${Math.min(100, Math.max(0, (temperature - 36.5) * 1.5))}%` }}
            />
          </div>
        </div>
      </div>

      {/* Stats Bar */}
      <div className="pixel-box stats-bar">
        <div className="stat-item">
          <span className="label">❤️ 누적 기부금</span>
          <span className="value">₩{totalDonation.toLocaleString()}</span>
        </div>
        <div className="stat-item">
          <span className="label">⚡ 누적 봉사시간</span>
          <span className="value">{totalHours} 시간</span>
        </div>
        <div className="stat-item">
          <span className="label">🏆 참여 픽셀 영웅</span>
          <span className="value">{totalMembers} 명</span>
        </div>
      </div>

      {/* Nav Tabs */}
      <nav style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
        <button
          className={`pixel-btn ${activeTab === 'ai' ? 'pixel-btn-red' : ''}`}
          onClick={() => setActiveTab('ai')}
        >
          🤖 Pixel AI Mate (Upstage)
        </button>
        <button
          className={`pixel-btn ${activeTab === 'volunteer' ? 'pixel-btn-red' : ''}`}
          onClick={() => setActiveTab('volunteer')}
        >
          🤝 봉사 & 기부 (1365 연동)
        </button>
        <button
          className={`pixel-btn ${activeTab === 'diary' ? 'pixel-btn-red' : ''}`}
          onClick={() => setActiveTab('diary')}
        >
          📖 픽셀 일기장 & 커뮤니티
        </button>
        <button
          className={`pixel-btn ${activeTab === 'roadmap' ? 'pixel-btn-red' : ''}`}
          onClick={() => setActiveTab('roadmap')}
        >
          🗺️ 성장의 길 (뱃지 도감)
        </button>
      </nav>
    </header>
  );
};
