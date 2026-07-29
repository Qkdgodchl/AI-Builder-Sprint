import React from 'react';

interface HeaderProps {
  temperature: number;
  totalDonation: number;
  totalHours: number;
  totalMembers: number;
  activeTab: 'ai' | 'volunteer' | 'diary' | 'roadmap';
  setActiveTab: (tab: 'ai' | 'volunteer' | 'diary' | 'roadmap') => void;
  currentUser: string | null;
  onLogin: () => void;
  onLogout: () => void;
  onAdminApply: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  temperature,
  totalDonation,
  totalHours,
  totalMembers,
  activeTab,
  setActiveTab,
  currentUser,
  onLogin,
  onLogout,
  onAdminApply,
}) => {
  const navigation = [
    { label: 'HOME', tab: 'ai' },
    { label: 'VOLUNTEER / DONATION', tab: 'volunteer' },
    { label: 'COMMUNITY', tab: 'diary' },
    { label: 'MY PAGE', tab: 'roadmap' },
  ] as const;

  return (
    <header className={`magazine-header ${activeTab === 'volunteer' ? 'flush-content' : ''}`}>
      {/* Top Header Bar */}
      <div className="magazine-header-top">
        <button className="brand-button" type="button" onClick={() => setActiveTab('ai')}>
          PIXEL CARE STUDIO
        </button>

        <nav className="primary-navigation" aria-label="주요 메뉴">
          {navigation.map((item) => (
            <button
              key={item.tab}
              type="button"
              className={`primary-nav-item ${activeTab === item.tab ? 'active' : ''}`}
              onClick={() => setActiveTab(item.tab)}
            >
              {item.label}
            </button>
          ))}
        </nav>

        <div className="header-account-actions">
          {currentUser ? (
            <>
              <button className="admin-apply-button" type="button" onClick={onAdminApply}>
                ADMIN APPLY
              </button>
              <button className="header-login-button" type="button" onClick={onLogout}>
                LOGOUT
              </button>
            </>
          ) : (
            <button className="header-login-button" type="button" onClick={onLogin}>
              LOGIN
            </button>
          )}
        </div>
      </div>

      {/* Main Magazine Title */}
      <h1 className="magazine-title">
        PIXEL CARE MAGAZINE
      </h1>

      {/* Editorial Stats Banner */}
      <div className="magazine-stats" aria-label="픽셀 케어 누적 현황">
        <div className="magazine-stat">
          <span className="magazine-stat-label">WARMTH</span>
          <strong className="magazine-stat-value accent">{temperature.toFixed(1)}°C</strong>
        </div>
        <div className="magazine-stat">
          <span className="magazine-stat-label">DONATION</span>
          <strong className="magazine-stat-value">₩{totalDonation.toLocaleString()}</strong>
        </div>
        <div className="magazine-stat">
          <span className="magazine-stat-label">VOLUNTEER</span>
          <strong className="magazine-stat-value">{totalHours} HOURS</strong>
        </div>
        <div className="magazine-stat">
          <span className="magazine-stat-label">MEMBERS</span>
          <strong className="magazine-stat-value">{totalMembers}</strong>
        </div>
      </div>

    </header>
  );
};
