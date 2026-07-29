import React, { useEffect, useState } from 'react';
import type { UserRole } from '../../types';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAuthenticate: (email: string, nickname: string, role: UserRole) => void;
}

type AuthView = 'login' | 'signup';

export const AuthModal: React.FC<AuthModalProps> = ({
  isOpen,
  onClose,
  onAuthenticate,
}) => {
  const [view, setView] = useState<AuthView>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [nickname, setNickname] = useState('');
  const [name, setName] = useState('');
  const [privacyAgreed, setPrivacyAgreed] = useState(false);
  const [formError, setFormError] = useState('');

  useEffect(() => {
    if (!isOpen) {
      setView('login');
      setPassword('');
      setPasswordConfirm('');
      setFormError('');
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleLoginSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    const normalizedEmail = email.trim();
    onAuthenticate(normalizedEmail, normalizedEmail.split('@')[0] || '픽셀 사용자', 'USER');
  };

  const handleSignupSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    setFormError('');

    if (password !== passwordConfirm) {
      setFormError('비밀번호가 일치하지 않습니다.');
      return;
    }

    onAuthenticate(email.trim(), nickname.trim(), 'USER');
  };

  const switchView = (nextView: AuthView) => {
    setView(nextView);
    setFormError('');
  };

  return (
    <div className="auth-modal-backdrop" role="presentation" onMouseDown={onClose}>
      <section
        className="auth-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="auth-modal-title"
        onMouseDown={(event) => event.stopPropagation()}
      >
        <button className="auth-modal-close" type="button" aria-label="닫기" onClick={onClose}>
          ×
        </button>

        <p className="auth-modal-eyebrow">PIXEL CARE ACCOUNT</p>
        <div className="auth-view-tabs" role="tablist" aria-label="계정 메뉴">
          <button
            type="button"
            role="tab"
            aria-selected={view === 'login'}
            className={view === 'login' ? 'active' : ''}
            onClick={() => switchView('login')}
          >
            로그인
          </button>
          <button
            type="button"
            role="tab"
            aria-selected={view === 'signup'}
            className={view === 'signup' ? 'active' : ''}
            onClick={() => switchView('signup')}
          >
            회원가입
          </button>
        </div>

        <h2 id="auth-modal-title">{view === 'login' ? 'LOGIN' : 'SIGN UP'}</h2>
        <p className="auth-modal-description">
          {view === 'login'
            ? '로그인하고 봉사·기부 신청 내역과 약정 진행 상태를 관리하세요.'
            : '픽셀케어 계정을 만들고 나에게 맞는 선행 활동을 시작하세요.'}
        </p>

        {view === 'login' ? (
          <form className="auth-form" onSubmit={handleLoginSubmit}>
            <label>
              이메일
              <input
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="you@example.com"
                autoComplete="email"
                required
                autoFocus
              />
            </label>
            <label>
              비밀번호
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="비밀번호"
                autoComplete="current-password"
                required
              />
            </label>
            <button className="auth-submit-button" type="submit">
              로그인
            </button>
          </form>
        ) : (
          <form className="auth-form signup-form" onSubmit={handleSignupSubmit}>
            <div className="auth-form-row">
              <label>
                이름
                <input
                  type="text"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  placeholder="실명"
                  autoComplete="name"
                  required
                />
              </label>
              <label>
                닉네임
                <input
                  type="text"
                  value={nickname}
                  onChange={(event) => setNickname(event.target.value)}
                  placeholder="픽셀 닉네임"
                  required
                />
              </label>
            </div>
            <label>
              이메일
              <input
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="you@example.com"
                autoComplete="email"
                required
              />
            </label>
            <div className="auth-form-row">
              <label>
                비밀번호
                <input
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="8자 이상"
                  minLength={8}
                  autoComplete="new-password"
                  required
                />
              </label>
              <label>
                비밀번호 확인
                <input
                  type="password"
                  value={passwordConfirm}
                  onChange={(event) => setPasswordConfirm(event.target.value)}
                  placeholder="다시 입력"
                  minLength={8}
                  autoComplete="new-password"
                  required
                />
              </label>
            </div>
            <label className="auth-consent">
              <input
                type="checkbox"
                checked={privacyAgreed}
                onChange={(event) => setPrivacyAgreed(event.target.checked)}
                required
              />
              <span>서비스 이용약관과 개인정보 수집·이용에 동의합니다.</span>
            </label>
            {formError && <p className="auth-form-error">{formError}</p>}
            <button className="auth-submit-button" type="submit">
              회원가입
            </button>
          </form>
        )}

        <div className="social-login-divider">
          <span>소셜 로그인</span>
        </div>
        <div className="social-login-buttons" aria-label="소셜 로그인 준비 중">
          <button type="button" disabled aria-label="카카오 로그인 준비 중">
            <span className="social-mark kakao">K</span>
            카카오
          </button>
          <button type="button" disabled aria-label="네이버 로그인 준비 중">
            <span className="social-mark naver">N</span>
            네이버
          </button>
          <button type="button" disabled aria-label="구글 로그인 준비 중">
            <span className="social-mark google">G</span>
            구글
          </button>
        </div>

        {view === 'login' && (
          <button
            type="button"
            className="manager-demo-login"
            onClick={() =>
              onAuthenticate('manager@pixelcare.demo', '데모 센터 관리자', 'CENTER_MANAGER')
            }
          >
            센터 관리자 데모로 로그인
          </button>
        )}

        <p className="auth-modal-note">
          현재는 화면 확인용 계정 흐름입니다. 실제 인증과 소셜 로그인은 백엔드 연동 시 적용됩니다.
        </p>
      </section>
    </div>
  );
};
