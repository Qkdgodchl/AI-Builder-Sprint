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
    <header className="magazine-header">
      {/* Top Header Bar */}
      <div className="magazine-header-top">
        <div style={{ fontSize: '13px', fontWeight: '800', letterSpacing: '1px' }}>
          PIXEL CARE STUDIO
        </div>
        <div style={{ display: 'flex', gap: '20px', fontSize: '11px', color: '#555' }}>
          <span>HOME</span>
          <span>VOLUNTEER</span>
          <span>DONATION</span>
          <span>COMMUNITY</span>
          <span>AI MATE</span>
        </div>
        <div>
          <button className="pixel-btn" style={{ fontSize: '10px', padding: '4px 10px', borderRadius: '14px' }}>
            GET STARTED NOW
          </button>
        </div>
      </div>

      {/* Main Magazine Title */}
      <h1 className="magazine-title">
        PIXEL CARE MAGAZINE
      </h1>

      {/* Thermometer & Stats Banner */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', margin: '20px 0 28px', padding: '14px 20px', borderTop: '1px solid #111', borderBottom: '1px solid #111', flexWrap: 'wrap', gap: '12px' }}>
        <div style={{ fontSize: '12px', fontWeight: '700' }}>
          🌡️ 온기 온도계: <span style={{ color: 'var(--magazine-accent)', fontSize: '15px' }}>{temperature.toFixed(1)}°C</span>
        </div>
        <div style={{ display: 'flex', gap: '24px', fontSize: '12px', color: '#444' }}>
          <span>❤️ 기부금: <b>₩{totalDonation.toLocaleString()}</b></span>
          <span>⚡ 봉사시간: <b>{totalHours} 시간</b></span>
          <span>🏆 참여인원: <b>{totalMembers} 명</b></span>
        </div>
      </div>

      {/* Pill Filter Navigation (Matching Reference Image) */}
      <div className="magazine-pills-nav">
        <button
          className={`magazine-pill ${activeTab === 'volunteer' ? 'active' : ''}`}
          onClick={() => setActiveTab('volunteer')}
        >
          🤝 봉사 & 기부 (1365 연동)
        </button>
        <button
          className={`magazine-pill ${activeTab === 'diary' ? 'active' : ''}`}
          onClick={() => setActiveTab('diary')}
        >
          💬 픽셀 커뮤니티
        </button>
        <button
          className={`magazine-pill ${activeTab === 'ai' ? 'active' : ''}`}
          onClick={() => setActiveTab('ai')}
        >
          🤖 AI 픽셀 메이트 (Upstage)
        </button>
        <button
          className={`magazine-pill ${activeTab === 'roadmap' ? 'active' : ''}`}
          onClick={() => setActiveTab('roadmap')}
        >
          🗺️ 성장의 길 (뱃지 도감)
        </button>
      </div>
    </header>
  );
};
