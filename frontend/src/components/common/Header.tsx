import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import type { SessionUser } from '../../types';

interface HeaderProps {
  temperature: number;
  totalDonation: number;
  totalHours: number;
  totalMembers: number;
  currentUser: SessionUser | null;
  onLogin: () => void;
  onLogout: () => void;
  onAdminApply: () => void;
}

interface NavigationItem {
  label: string;
  path: string;
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

  const navigation: NavigationItem[] = [
    { label: 'HOME', path: '/ai' },
    { label: 'VOLUNTEER / DONATION', path: '/volunteer' },
    { label: 'COMMUNITY', path: '/community' },
    { label: 'MY PAGE', path: currentUser ? '/my-page' : '/roadmap' },
  ];

  if (currentUser?.role === 'CENTER_MANAGER') {
    navigation.push({ label: 'MY CENTER', path: '/my-centers' });
  }

  const usesContentDivider =
    location.pathname.startsWith('/volunteer') ||
    location.pathname.startsWith('/manager-application') ||
    location.pathname.startsWith('/my-centers') ||
    location.pathname.startsWith('/my-page');

  const isPathActive = (path: string) => {
    if (path === '/community') return location.pathname.startsWith('/community');
    if (path === '/volunteer') return location.pathname.startsWith('/volunteer');
    if (path === '/my-centers') return location.pathname.startsWith('/my-centers');
    return location.pathname === path;
  };

  return (
    <header className={`magazine-header ${usesContentDivider ? 'flush-content' : ''}`}>
      <div className="magazine-header-top">
        <button className="brand-button" type="button" onClick={() => navigate('/volunteer')}>
          PIXEL CARE STUDIO
        </button>

        <nav className="primary-navigation" aria-label="주요 메뉴">
          {navigation.map((item) => (
            <button
              key={item.path}
              type="button"
              className={`primary-nav-item ${isPathActive(item.path) ? 'active' : ''}`}
              onClick={() => navigate(item.path)}
            >
              {item.label}
            </button>
          ))}
        </nav>

        <div className="header-account-actions">
          {currentUser ? (
            <>
              {currentUser.role === 'USER' && (
                <button className="admin-apply-button" type="button" onClick={onAdminApply}>
                  관리자 신청
                </button>
              )}
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

      <h1 className="magazine-title" onClick={() => navigate('/volunteer')}>
        PIXEL CARE MAGAZINE
      </h1>

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
