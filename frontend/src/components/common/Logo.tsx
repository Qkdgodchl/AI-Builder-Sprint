import React from 'react';

interface LogoProps {
  /** mark: 심볼만 (파비콘·좁은 자리) / full: 심볼 + ITDA 워드마크 */
  variant?: 'mark' | 'full';
  className?: string;
  onClick?: () => void;
}

/**
 * 잇다(ITDA) 로고.
 * 맞물린 두 고리는 마음과 행동, 기부자와 기관이 하나로 이어지는 약정을 뜻한다.
 * 왼쪽 고리는 서비스의 기본색, 오른쪽 고리는 강조색으로 두어 '잇는 지점'이 드러난다.
 */
export const Logo: React.FC<LogoProps> = ({ variant = 'full', className = '', onClick }) => {
  const Tag = onClick ? 'button' : 'div';

  return (
    <Tag
      {...(onClick ? { type: 'button' as const, onClick } : {})}
      className={`itda-logo itda-logo-${variant} ${className}`.trim()}
      aria-label="잇다 ITDA"
    >
      <svg className="itda-logo-mark" viewBox="0 0 72 40" role="img" aria-hidden="true">
        <defs>
          {/* 위쪽 교차 지점만 남겨, 왼쪽 고리를 오른쪽 고리 위로 다시 덮는다.
              아래쪽은 오른쪽 고리가 위에 남아 두 고리가 실제로 맞물려 보인다. */}
          <clipPath id="itda-logo-weave">
            <rect x="26" y="0" width="20" height="18" />
          </clipPath>
        </defs>
        <rect
          x="4" y="6" width="38" height="28" rx="14"
          fill="none" stroke="currentColor" strokeWidth="7"
        />
        <rect
          x="30" y="6" width="38" height="28" rx="14"
          fill="none" stroke="var(--magazine-accent, #ff3b30)" strokeWidth="7"
        />
        <rect
          x="4" y="6" width="38" height="28" rx="14"
          fill="none" stroke="currentColor" strokeWidth="7"
          clipPath="url(#itda-logo-weave)"
        />
      </svg>
      {variant === 'full' && <span className="itda-logo-word">ITDA</span>}
    </Tag>
  );
};
