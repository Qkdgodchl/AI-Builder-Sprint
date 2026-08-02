export interface BadgeProgress {
  level: number;
  name: string;
  icon: string;
  description: string;
  /** 달성 수치와 목표. 진행률을 그대로 보여주기 위해 함께 둔다. */
  current: number;
  goal: number;
  /** 진행률의 시작점. 온기 뱃지는 0이 아니라 직전 단계에서 출발한다. */
  base: number;
  unit: string;
}

/** 승인 이후로 볼 수 있는 신청 상태. 참여를 마친 것으로 계산한다. */
export const DONE_APPLICATION_STATUSES = ['APPROVED', 'COMPLETED', 'VERIFIED'];

/**
 * 뱃지 정의. 로드맵과 마이페이지가 같은 기준을 쓰도록 한곳에 둔다.
 *
 * 첫 뱃지만 "첫 선행 신청"으로 열고, 그다음부터는 온기 하나로 줄을 세운다.
 * 기준이 제각각이면 5단계가 순서 없이 열려 성장하는 느낌이 나지 않는다.
 * 마지막 단계 50°C는 온기가 오를 수 있는 최고치와 같다.
 */
export const BADGE_DEFINITIONS = [
  { level: 1, name: '씨앗 용사', icon: '🌱', description: '첫 선행을 신청하면 얻는 뱃지', goal: 1, base: 0, unit: '' },
  { level: 2, name: '온기 수호자', icon: '🔥', description: '온기 38°C를 넘긴 이웃', goal: 38, base: 36.5, unit: '°C' },
  { level: 3, name: '나눔이', icon: '⭐', description: '온기 41°C를 넘긴 이웃', goal: 41, base: 38, unit: '°C' },
  { level: 4, name: '동네 영웅', icon: '🛡️', description: '온기 45°C를 넘긴 이웃', goal: 45, base: 41, unit: '°C' },
  { level: 5, name: '잇다 마스터', icon: '👑', description: '온기를 50°C까지 채운 이웃', goal: 50, base: 45, unit: '°C' },
] as const;

export interface BadgeInputs {
  temperature: number;
  applicationCount: number;
  completedCount: number;
  signedCount: number;
}

export const computeBadges = (inputs: BadgeInputs): BadgeProgress[] =>
  BADGE_DEFINITIONS.map((badge) => ({
    ...badge,
    current: badge.level === 1 ? inputs.applicationCount : inputs.temperature,
  }));

/** 로그아웃 상태에서는 시작점에 머문 것으로 두어 전부 잠긴 모습으로 보여준다. */
export const lockedBadges = (): BadgeProgress[] =>
  BADGE_DEFINITIONS.map((badge) => ({ ...badge, current: badge.base }));
