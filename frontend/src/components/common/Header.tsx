import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';

interface HeaderProps {
  temperature: number;
  totalDonation: number;
  totalHours: number;
  totalMembers: number;
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
  currentUser,
  onLogin,
  onLogout,
  onAdminApply,
}) => {
  const navigate = useNavigate();
  const location = useLocation();

  const getActiveTab = () => {
    const path = location.pathname;
    if (path.startsWith('/community')) return 'community';
    if (path.startsWith('/ai')) return 'ai';
    if (path.startsWith('/roadmap')) return 'roadmap';
    return 'volunteer';
  };

  const activeTab = getActiveTab();

  const navigation = [
    { label: 'VOLUNTEER / DONATION', path: '/volunteer' },
    { label: 'COMMUNITY', path: '/community' },
    { label: 'AI MATE', path: '/ai' },
    { label: 'MY PAGE / ROADMAP', path: '/roadmap' },
  ];

  return (
    <header className={`magazine-header ${activeTab === 'volunteer' ? 'flush-content' : ''}`}>
      {/* Top Header Bar */}
      <div className="magazine-header-top">
        <button className="brand-button" type="button" onClick={() => navigate('/volunteer')}>
          PIXEL CARE STUDIO
        </button>

        <nav className="primary-navigation" aria-label="주요 메뉴">
          {navigation.map((item) => {
            const isActive = location.pathname.startsWith(item.path) || (item.path === '/volunteer' && location.pathname === '/');
            return (
              <button
                key={item.path}
                type="button"
                className={`primary-nav-item ${isActive ? 'active' : ''}`}
                onClick={() => navigate(item.path)}
              >
                {item.label}
              </button>
            );
          })}
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
      <h1 className="magazine-title" style={{ cursor: 'pointer' }} onClick={() => navigate('/volunteer')}>
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
