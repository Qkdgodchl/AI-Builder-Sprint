import { useEffect, useState } from 'react';
import type { SessionUser } from '../types';
import { fetchMyProfile } from '../services/authApi';
import { normalizeNewsRegion } from '../services/newsApi';

/** 마이페이지에서 활동 지역을 저장하면 열려 있는 화면도 곧바로 따라오게 한다. */
export const PROFILE_UPDATED_EVENT = 'pixel-care-profile-updated';

const STORAGE_KEY = 'pixel-care-region';

interface CachedRegion {
  userId: number;
  region: string;
}

// 지역을 매번 서버에서 읽으면 홈이 열릴 때마다 전국 → 내 지역으로 한 번 깜빡인다.
const readCache = (userId?: number): string | null => {
  if (!userId) return null;
  try {
    const cached = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '') as CachedRegion;
    return cached.userId === userId ? cached.region : null;
  } catch {
    return null;
  }
};

const writeCache = (userId: number, region: string) => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ userId, region } satisfies CachedRegion));
  } catch {
    // 저장소를 못 써도 매번 서버에서 읽으면 되므로 화면에는 영향이 없다.
  }
};

/**
 * 로그인한 회원의 활동 지역을 뉴스 지역으로 돌려준다.
 * 로그인 전이거나 지역을 아직 등록하지 않았으면 전국이다.
 */
export function useMyRegion(currentUser: SessionUser | null): string {
  const [region, setRegion] = useState(() => readCache(currentUser?.id) ?? '전국');

  useEffect(() => {
    if (!currentUser) {
      setRegion('전국');
      return;
    }

    setRegion(readCache(currentUser.id) ?? '전국');

    let active = true;
    const load = () => {
      fetchMyProfile()
        .then((profile) => {
          if (!active) return;
          const next = normalizeNewsRegion(profile.region);
          setRegion(next);
          writeCache(currentUser.id, next);
        })
        .catch(() => {
          // 프로필을 못 읽으면 전국 소식을 보여주면 되므로 화면을 막지 않는다.
        });
    };

    load();
    window.addEventListener(PROFILE_UPDATED_EVENT, load);
    return () => {
      active = false;
      window.removeEventListener(PROFILE_UPDATED_EVENT, load);
    };
  }, [currentUser]);

  return region;
}
