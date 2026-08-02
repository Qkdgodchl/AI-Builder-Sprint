import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import type { SessionUser } from '../../types';
import type { PlatformStats } from '../../services/statsApi';
import { Logo } from './Logo';

interface HeaderProps {
  stats: PlatformStats | null;
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
  stats,
  currentUser,
  onLogin,
  onLogout,
  onAdminApply,
}) => {
  const navigate = useNavigate();
  const location = useLocation();

  // 집계를 아직 못 받았으면 자리만 지키고 값은 비워 둔다. 임의의 숫자를 보여주지 않는다.
  const placeholder = '—';
  const statItems = [
    {
      label: 'WARMTH',
      value: stats ? `${stats.averageWarmth.toFixed(1)}°C` : placeholder,
      accent: true,
    },
    {
      label: 'DONATION',
      value: stats ? `₩${stats.pledgedDonation.toLocaleString()}` : placeholder,
      accent: false,
    },
    {
      label: 'VOLUNTEER',
      value: stats ? `${stats.volunteerHours.toLocaleString()} HOURS` : placeholder,
      accent: false,
    },
    {
      label: 'SIGNED',
      value: stats ? `${stats.signedCommitments.toLocaleString()} DOCS` : placeholder,
      accent: false,
    },
    {
      label: 'MEMBERS',
      value: stats ? `${stats.activeMembers}` : placeholder,
      accent: false,
    },
  ];

  const navigation: NavigationItem[] = [
    { label: 'HOME', path: '/' },
    { label: 'VOLUNTEER / DONATION', path: '/volunteer' },
    { label: 'COMMUNITY', path: '/community' },
    { label: 'GOOD NEWS', path: '/news' },
    { label: 'MY PAGE', path: '/my-page' },
  ];

  if (currentUser?.role === 'CENTER_MANAGER') {
    navigation.push({ label: 'MY CENTER', path: '/my-centers' });
  }
  if (currentUser?.role === 'OPERATOR') {
    navigation.push({ label: 'MANAGEMENT', path: '/management' });
  }

  const usesContentDivider =
    location.pathname.startsWith('/volunteer') ||
    location.pathname.startsWith('/manager-application') ||
    location.pathname.startsWith('/my-centers') ||
    location.pathname.startsWith('/management') ||
    location.pathname.startsWith('/my-page') ||
    location.pathname.startsWith('/news');

  const isPathActive = (path: string) => {
    if (path === '/community') return location.pathname.startsWith('/community');
    if (path === '/') return location.pathname === '/';
    if (path === '/news') return location.pathname.startsWith('/news');
    if (path === '/volunteer') return location.pathname.startsWith('/volunteer');
    if (path === '/my-centers') return location.pathname.startsWith('/my-centers');
    if (path === '/management') return location.pathname.startsWith('/management');
    return location.pathname === path;
  };

  return (
    <header className={`magazine-header ${usesContentDivider ? 'flush-content' : ''}`}>
      <div className="magazine-header-top">
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

      <Logo className="magazine-logo" onClick={() => navigate('/')} />

      <div className="magazine-stats" aria-label="잇다 누적 현황">
        {/* 같은 항목을 세 벌 이어 붙이고 한 벌만큼 밀어 끊김 없이 순환시킨다.
            두 벌만 두면 넓은 화면에서 순환 지점에 빈 공간이 보인다.
            복제본은 화면에만 필요하므로 보조기기에서는 숨긴다. */}
        <div className="magazine-stats-track">
          {[0, 1, 2].map((copy) => (
            <div className="magazine-stats-set" key={copy} aria-hidden={copy !== 0}>
              {statItems.map((item) => (
                <div className="magazine-stat" key={item.label}>
                  <span className="magazine-stat-label">{item.label}</span>
                  <strong className={`magazine-stat-value${item.accent ? ' accent' : ''}`}>
                    {item.value}
                  </strong>
                </div>
              ))}
            </div>
          ))}
        </div>
      </div>
    </header>
  );
};
