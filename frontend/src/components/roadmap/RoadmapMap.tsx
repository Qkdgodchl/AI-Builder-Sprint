import React, { useEffect, useState } from 'react';
import type { SessionUser } from '../../types';
import { playBeep } from '../../services/soundFx';
import { fetchMyProfile } from '../../services/authApi';
import { fetchMyApplications } from '../../services/applicationApi';
import { fetchMyClmDocuments } from '../../services/clmApi';
import { BadgeGrid } from './BadgeGrid';
import {
  computeBadges,
  lockedBadges,
  DONE_APPLICATION_STATUSES,
  type BadgeProgress,
} from './badgeProgress';

interface RoadmapMapProps {
  showToast: (message: string) => void;
  currentUser: SessionUser | null;
  onLogin: () => void;
}

export const RoadmapMap: React.FC<RoadmapMapProps> = ({ showToast, currentUser, onLogin }) => {
  const [badges, setBadges] = useState<BadgeProgress[] | null>(null);

  useEffect(() => {
    if (!currentUser) {
      setBadges(null);
      return;
    }
    Promise.all([fetchMyProfile(), fetchMyApplications(), fetchMyClmDocuments()])
      .then(([profile, applications, documents]) => {
        setBadges(
          computeBadges({
            temperature: profile.temperature ?? 0,
            applicationCount: applications.length,
            completedCount: applications.filter((item) =>
              DONE_APPLICATION_STATUSES.includes(item.status),
            ).length,
            signedCount: documents.filter((item) => item.status === 'SIGNED').length,
          }),
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
        <span>ITDA ROADMAP</span>
        <h2>성장의 길</h2>
        <p>
          {currentUser
            ? `${currentUser.nickname}님의 선행 기록으로 계산한 단계입니다.`
            : '선행을 쌓을수록 단계가 열립니다. 뱃지는 계정마다 따로 기록됩니다.'}
        </p>
      </header>

      {visibleBadges === null ? (
        <p className="roadmap-loading">뱃지 진행 상황을 불러오는 중입니다…</p>
      ) : (
        <BadgeGrid
          badges={visibleBadges}
          preview={!currentUser}
          onSelect={currentUser ? handleBadgeClick : undefined}
        />
      )}

      {/* 2단계부터는 온기로 열리므로 온기를 어떻게 올리는지 여기서 알려준다. */}
      <section className="roadmap-warmth-tip">
        <strong>Tip!</strong>
        <p>온기는 이렇게 오릅니다.</p>
        <ul>
          <li><b>+1.0°C</b> 전자서명으로 약정 체결</li>
          <li><b>+0.5°C</b> 봉사·기부 신청 제출</li>
          <li><b>+0.5°C</b> 정기 약정 갱신</li>
          <li><b>+0.3°C</b> 커뮤니티 글 작성</li>
          <li><b>+0.2°C</b> 활동 다이어리 기록</li>
          <li><b>+0.1°C</b> 댓글 작성 · 응원 보내기</li>
        </ul>
        <small>하루에 최대 +2.0°C까지 오르고, 50°C가 마지막 단계입니다.</small>
      </section>

      {!currentUser && (
        <section className="roadmap-login-note">
          <p>로그인하고 나만의 뱃지를 모아보세요.</p>
          <button type="button" onClick={onLogin}>로그인하기</button>
        </section>
      )}
    </article>
  );
};
