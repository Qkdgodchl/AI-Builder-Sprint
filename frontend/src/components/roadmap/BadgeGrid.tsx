import React from 'react';
import type { BadgeProgress } from './badgeProgress';

interface BadgeGridProps {
  badges: BadgeProgress[];
  /** 로그인 전 미리보기. 수치 대신 안내 문구를 보여준다. */
  preview?: boolean;
  onSelect?: (badge: BadgeProgress, unlocked: boolean) => void;
}

export const BadgeGrid: React.FC<BadgeGridProps> = ({ badges, preview = false, onSelect }) => (
  <div className={`roadmap-badge-grid${preview ? ' is-preview' : ''}`}>
    {badges.map((badge) => {
      const unlocked = badge.current >= badge.goal;
      const percent = Math.min(100, Math.round((badge.current / badge.goal) * 100));
      return (
        <button
          type="button"
          key={badge.level}
          className={`roadmap-badge${unlocked ? ' unlocked' : ''}`}
          onClick={() => onSelect?.(badge, unlocked)}
        >
          <span className="roadmap-badge-icon" aria-hidden="true">
            {unlocked ? badge.icon : '🔒'}
          </span>
          <strong>LV{badge.level} {badge.name}</strong>
          <small>{badge.description}</small>
          <div className="roadmap-badge-track">
            <span style={{ width: `${percent}%` }} />
          </div>
          <em>
            {preview ? '로그인 후 확인' : unlocked ? '획득 완료' : `${badge.current} / ${badge.goal}`}
          </em>
        </button>
      );
    })}
  </div>
);
