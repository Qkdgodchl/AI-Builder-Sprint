import React, { useEffect, useState } from 'react';

interface AuthModalProps {
  mode: 'login' | 'admin' | null;
  currentUser: string | null;
  onClose: () => void;
  onLogin: (email: string) => void;
  onAdminApply: (organizationName: string) => void;
}

export const AuthModal: React.FC<AuthModalProps> = ({
  mode,
  currentUser,
  onClose,
  onLogin,
  onAdminApply,
}) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [organizationName, setOrganizationName] = useState('');
  const [reason, setReason] = useState('');

  useEffect(() => {
    if (!mode) {
      setPassword('');
      setOrganizationName('');
      setReason('');
    }
  }, [mode]);

  if (!mode) return null;

  const handleLoginSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    onLogin(email.trim());
  };

  const handleAdminSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    onAdminApply(organizationName.trim());
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

        {mode === 'login' ? (
          <>
            <p className="auth-modal-eyebrow">PIXEL CARE ACCOUNT</p>
            <h2 id="auth-modal-title">LOGIN</h2>
            <p className="auth-modal-description">
              로그인하고 봉사·기부 신청 내역과 나의 온기 기록을 관리하세요.
            </p>

            <form className="auth-form" onSubmit={handleLoginSubmit}>
              <label>
                EMAIL
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
                PASSWORD
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
                LOGIN
              </button>
            </form>
            <p className="auth-modal-note">
              현재는 프론트 화면 확인용 로그인입니다. 실제 계정 인증은 백엔드 API 연동 시 적용됩니다.
            </p>
          </>
        ) : (
          <>
            <p className="auth-modal-eyebrow">CENTER MANAGER</p>
            <h2 id="auth-modal-title">ADMIN APPLICATION</h2>
            <p className="auth-modal-description">
              {currentUser} 계정으로 센터 관리자 권한을 신청합니다.
            </p>

            <form className="auth-form" onSubmit={handleAdminSubmit}>
              <label>
                ORGANIZATION
                <input
                  type="text"
                  value={organizationName}
                  onChange={(event) => setOrganizationName(event.target.value)}
                  placeholder="소속 기관 또는 센터명"
                  required
                  autoFocus
                />
              </label>
              <label>
                APPLICATION REASON
                <textarea
                  value={reason}
                  onChange={(event) => setReason(event.target.value)}
                  placeholder="관리자 권한이 필요한 이유를 입력해 주세요."
                  required
                  rows={4}
                />
              </label>
              <button className="auth-submit-button" type="submit">
                SUBMIT APPLICATION
              </button>
            </form>
          </>
        )}
      </section>
    </div>
  );
};
