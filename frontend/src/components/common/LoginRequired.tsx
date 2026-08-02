import React from 'react';
import { useNavigate } from 'react-router-dom';

interface LoginRequiredProps {
  title: string;
  description: string;
  onLogin: () => void;
}

/** 계정에 묶인 화면을 로그아웃 상태로 열었을 때 보여주는 안내. */
export const LoginRequired: React.FC<LoginRequiredProps> = ({ title, description, onLogin }) => {
  const navigate = useNavigate();

  return (
    <section className="login-required">
      <span>MEMBERS ONLY</span>
      <h2>{title}</h2>
      <p>{description}</p>
      <div className="login-required-actions">
        <button type="button" onClick={onLogin}>로그인하기</button>
        <button type="button" className="secondary" onClick={() => navigate('/volunteer')}>
          프로그램 먼저 둘러보기
        </button>
      </div>
    </section>
  );
};
