import React, { useEffect, useState } from 'react';
import type { SessionUser } from '../../types';
import { playBeep } from '../../services/soundFx';
import { fetchMyProfile } from '../../services/authApi';
import { fetchMyApplications } from '../../services/applicationApi';
import { fetchMyClmDocuments } from '../../services/clmApi';

interface RoadmapMapProps {
  showToast: (message: string) => void;
  currentUser: SessionUser | null;
  onLogin: () => void;
}

interface BadgeProgress {
  level: number;
  name: string;
  icon: string;
  description: string;
  /** 달성 수치와 목표. 진행률을 그대로 보여주기 위해 함께 둔다. */
  current: number;
  goal: number;
}

const DONE_STATUSES = ['APPROVED', 'COMPLETED', 'VERIFIED'];

/** 뱃지 정의. 로그아웃 상태에서는 달성치 0으로 두어 전부 잠긴 모습으로 보여준다. */
const BADGE_DEFINITIONS = [
  { level: 1, name: '씨앗 용사', icon: '🌱', description: '첫 선행을 신청하면 얻는 뱃지', goal: 1 },
  { level: 2, name: '온기 수호자', icon: '🔥', description: '나의 온기를 50°C까지 올린 이웃', goal: 50 },
  { level: 3, name: '픽셀 나눔이', icon: '⭐', description: '승인·완료된 참여 3건을 채운 이웃', goal: 3 },
  { level: 4, name: '동네 영웅', icon: '🛡️', description: '전자서명으로 약정을 남긴 이웃', goal: 1 },
  { level: 5, name: '픽셀 마스터', icon: '👑', description: '전자서명 약정 3건을 이어온 이웃', goal: 3 },
] as const;

const lockedBadges = (): BadgeProgress[] =>
  BADGE_DEFINITIONS.map((badge) => ({ ...badge, current: 0 }));

export const RoadmapMap: React.FC<RoadmapMapProps> = ({ showToast, currentUser, onLogin }) => {
  const [badges, setBadges] = useState<BadgeProgress[] | null>(null);

  useEffect(() => {
    if (!currentUser) {
      setBadges(null);
      return;
    }
    Promise.all([fetchMyProfile(), fetchMyApplications(), fetchMyClmDocuments()])
      .then(([profile, applications, documents]) => {
        const completed = applications.filter((item) => DONE_STATUSES.includes(item.status)).length;
        const signed = documents.filter((item) => item.status === 'SIGNED').length;
        const achieved: Record<number, number> = {
          1: applications.length,
          2: Math.round(profile.temperature ?? 0),
          3: completed,
          4: signed,
          5: signed,
        };
        setBadges(
          BADGE_DEFINITIONS.map((badge) => ({ ...badge, current: achieved[badge.level] ?? 0 })),
        );
      })
      .catch(() => setBadges(null));
  }, [currentUser]);

  const handleBadgeClick = (badge: BadgeProgress, unlocked: boolean) => {
    playBeep(unlocked ? 659 : 220, 0.1);
    showToast(
      unlocked
        ? `🏆 LV${badge.level} ${badge.name} 뱃지를 보유 중입니다.`
        : `🔒 LV${badge.level} ${badge.name} · ${badge.current}/${badge.goal} 달성하면 열립니다.`,
    );
  };

  // 로그아웃 상태에서도 어떤 단계가 있는지 보이도록 잠긴 뱃지를 그대로 노출한다.
  const visibleBadges = currentUser ? badges : lockedBadges();

  return (
    <article className="roadmap-page">
      <header className="roadmap-header">
        <span>PIXEL ROADMAP</span>
        <h2>픽셀 성장의 길</h2>
        <p>
          {currentUser
            ? `${currentUser.nickname}님의 선행 기록으로 계산한 단계입니다.`
            : '선행을 쌓을수록 단계가 열립니다. 뱃지는 계정마다 따로 기록됩니다.'}
        </p>
      </header>

      {visibleBadges === null ? (
        <p className="roadmap-loading">뱃지 진행 상황을 불러오는 중입니다…</p>
      ) : (
        <div className={`roadmap-badge-grid${currentUser ? '' : ' is-preview'}`}>
          {visibleBadges.map((badge) => {
            const unlocked = badge.current >= badge.goal;
            const percent = Math.min(100, Math.round((badge.current / badge.goal) * 100));
            return (
              <button
                type="button"
                key={badge.level}
                className={`roadmap-badge${unlocked ? ' unlocked' : ''}`}
                onClick={() => handleBadgeClick(badge, unlocked)}
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
                  {!currentUser
                    ? '로그인 후 확인'
                    : unlocked
                      ? '획득 완료'
                      : `${badge.current} / ${badge.goal}`}
                </em>
              </button>
            );
          })}
        </div>
      )}

      {!currentUser && (
        <section className="roadmap-login-note">
          <p>로그인하고 나만의 뱃지를 모아보세요.</p>
          <button type="button" onClick={onLogin}>로그인하기</button>
        </section>
      )}
    </article>
  );
};
