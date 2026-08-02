export interface BadgeProgress {
  level: number;
  name: string;
  icon: string;
  description: string;
  /** 달성 수치와 목표. 진행률을 그대로 보여주기 위해 함께 둔다. */
  current: number;
  goal: number;
}

/** 승인 이후로 볼 수 있는 신청 상태. 참여를 마친 것으로 계산한다. */
export const DONE_APPLICATION_STATUSES = ['APPROVED', 'COMPLETED', 'VERIFIED'];

/** 뱃지 정의. 로드맵과 마이페이지가 같은 기준을 쓰도록 한곳에 둔다. */
export const BADGE_DEFINITIONS = [
  { level: 1, name: '씨앗 용사', icon: '🌱', description: '첫 선행을 신청하면 얻는 뱃지', goal: 1 },
  { level: 2, name: '온기 수호자', icon: '🔥', description: '나의 온기를 50°C까지 올린 이웃', goal: 50 },
  { level: 3, name: '나눔이', icon: '⭐', description: '승인·완료된 참여 3건을 채운 이웃', goal: 3 },
  { level: 4, name: '동네 영웅', icon: '🛡️', description: '전자서명으로 약정을 남긴 이웃', goal: 1 },
  { level: 5, name: '잇다 마스터', icon: '👑', description: '전자서명 약정 3건을 이어온 이웃', goal: 3 },
] as const;

export interface BadgeInputs {
  temperature: number;
  applicationCount: number;
  completedCount: number;
  signedCount: number;
}

export const computeBadges = (inputs: BadgeInputs): BadgeProgress[] => {
  const achieved: Record<number, number> = {
    1: inputs.applicationCount,
    2: Math.round(inputs.temperature),
    3: inputs.completedCount,
    4: inputs.signedCount,
    5: inputs.signedCount,
  };
  return BADGE_DEFINITIONS.map((badge) => ({ ...badge, current: achieved[badge.level] ?? 0 }));
};

/** 로그아웃 상태에서는 달성치 0으로 두어 전부 잠긴 모습으로 보여준다. */
export const lockedBadges = (): BadgeProgress[] =>
  BADGE_DEFINITIONS.map((badge) => ({ ...badge, current: 0 }));
