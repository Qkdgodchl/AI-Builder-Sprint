import { useCallback, useEffect, useState } from 'react';
import { Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import './App.css';
import { Header } from './components/common/Header';
import { Modal } from './components/common/Modal';
import { Toast } from './components/common/Toast';
import { PixelAiMate } from './components/ai/PixelAiMate';
import { AiDock } from './components/ai/AiDock';
import { LoginRequired } from './components/common/LoginRequired';
import { VolunteerCatalog } from './components/volunteer/VolunteerCatalog';
import { PixelDiary } from './components/diary/PixelDiary';
import { RoadmapMap } from './components/roadmap/RoadmapMap';
import { AuthModal } from './components/auth/AuthModal';
import { ManagerApplicationPage } from './components/user/ManagerApplicationPage';
import { MyPage } from './components/user/MyPage';
import { DiaryDayPage } from './components/user/DiaryDayPage';
import { MyCenterPage } from './components/center/MyCenterPage';
import { ManagementPage } from './components/operator/ManagementPage';
import { HomePage } from './components/home/HomePage';
import { GoodNewsPage } from './components/news/GoodNewsPage';
import { playBeep } from './services/soundFx';
import { logout as logoutApi } from './services/authApi';
import { fetchPlatformStats, type PlatformStats } from './services/statsApi';
import { SESSION_EXPIRED_EVENT } from './services/apiClient';
import type { SessionUser } from './types';

const loadStoredUser = (): SessionUser | null => {
  const storedUser = localStorage.getItem('pixel-care-user');
  if (!storedUser) return null;

  try {
    const parsed = JSON.parse(storedUser) as SessionUser;
    if (parsed.id && parsed.email && parsed.role) return parsed;
  } catch {
    localStorage.removeItem('pixel-care-user');
  }

  return null;
};

export function App() {
  const navigate = useNavigate();
  // 홈 상단 누적 현황은 서버 집계를 그대로 보여준다. 조회 전에는 빈 값으로 둔다.
  const [stats, setStats] = useState<PlatformStats | null>(null);
  const [currentUser, setCurrentUser] = useState<SessionUser | null>(loadStoredUser);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [isAiDockOpen, setIsAiDockOpen] = useState(false);

  const [modalState, setModalState] = useState<{
    isOpen: boolean;
    title: string;
    type: 'volunteer' | 'donate';
  }>({
    isOpen: false,
    title: '',
    type: 'volunteer',
  });

  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const reloadStats = useCallback(() => {
    fetchPlatformStats()
      .then(setStats)
      .catch(() => {
        // 통계는 보조 정보이므로 실패해도 화면 전체를 막지 않는다.
      });
  }, []);

  useEffect(() => {
    reloadStats();
  }, [reloadStats, currentUser]);

  const triggerToast = useCallback((msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage(null);
    }, 3000);
  }, []);

  // 세션이 끊기면 헤더를 로그아웃 상태로 되돌려 왜 데이터가 비었는지 알 수 있게 한다.
  useEffect(() => {
    const handleSessionExpired = () => {
      setCurrentUser((previous) => {
        if (previous) triggerToast('로그인이 만료되었습니다. 다시 로그인해 주세요.');
        return null;
      });
    };
    window.addEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired);
  }, [triggerToast]);

  // 로그인이 필요한 동작을 눌렀을 때 이유를 알려주고 곧바로 로그인 창을 띄운다.
  const requireLogin = useCallback(
    (message: string) => {
      triggerToast(message);
      setIsAuthModalOpen(true);
    },
    [triggerToast],
  );

  const openAiDock = useCallback(() => {
    if (!currentUser) {
      requireLogin('로그인이 필요합니다. AI 추천은 로그인 후 이용할 수 있어요.');
      return;
    }
    setIsAiDockOpen(true);
  }, [currentUser, requireLogin]);

  const handleOpenModal = (title: string, type: 'volunteer' | 'donate') => {
    setModalState({ isOpen: true, title, type });
    playBeep(520, 0.1);
  };

  const handleCloseModal = () => {
    setModalState((prev) => ({ ...prev, isOpen: false }));
  };

  // 커뮤니티 활동으로 온기가 오르면 서버 값을 다시 읽어 반영한다.
  const handleDiaryActivity = useCallback(() => {
    reloadStats();
  }, [reloadStats]);

  const handleModalSubmit = (name: string, amount: number) => {
    if (modalState.type === 'donate') {
      triggerToast(`❤️ ${name}님, ${amount.toLocaleString()}원 기부 완료! 뱃지를 획득하셨습니다!`);
    } else {
      triggerToast(`⚡ ${name}님, 간편 봉사 신청이 완료되었습니다! 뱃지 발급!`);
    }

    // 누적 현황은 로컬에서 더하지 않고 서버 집계를 다시 읽는다.
    reloadStats();
    handleCloseModal();
    playBeep(660, 0.2);
  };

  const handleAuthenticate = (user: SessionUser) => {
    localStorage.setItem('pixel-care-user', JSON.stringify(user));
    setCurrentUser(user);
    setIsAuthModalOpen(false);
    triggerToast(`${user.nickname}님, 로그인했습니다.`);
  };

  const handleLogout = async () => {
    try {
      await logoutApi();
    } catch {
      // 토큰이 이미 만료돼 서버 호출이 실패해도 로컬 로그인 상태는 반드시 정리한다.
      // 그러지 않으면 만료된 세션에서 로그아웃 자체가 불가능해진다.
    }
    localStorage.removeItem('pixel-care-user');
    setCurrentUser(null);
    triggerToast('로그아웃되었습니다.');
  };

  const handleManagerApplicationSubmit = (centerName: string) => {
    navigate('/roadmap');
    triggerToast(`${centerName} 센터 관리자 신청이 접수되었습니다.`);
  };

  return (
    <div className="app-container">
      <Header
        stats={stats}
        currentUser={currentUser}
        onLogin={() => setIsAuthModalOpen(true)}
        onLogout={handleLogout}
        onAdminApply={() => navigate('/manager-application')}
      />

      <main>
        <Routes>
          <Route
            path="/"
            element={
              <HomePage
                currentUser={currentUser}
                onLogin={() => setIsAuthModalOpen(true)}
                onOpenAi={openAiDock}
              />
            }
          />
          <Route
            path="/volunteer/*"
            element={
              <VolunteerCatalog
                currentUser={currentUser}
                showToast={triggerToast}
                onRequireLogin={requireLogin}
              />
            }
          />
          <Route
            path="/community"
            element={
              <PixelDiary
                currentUser={currentUser}
                onAddDiary={handleDiaryActivity}
                showToast={triggerToast}
                onRequireLogin={requireLogin}
              />
            }
          />
          <Route
            path="/community/posts/:id"
            element={
              <PixelDiary
                currentUser={currentUser}
                onAddDiary={handleDiaryActivity}
                showToast={triggerToast}
                onRequireLogin={requireLogin}
              />
            }
          />
          <Route path="/ai" element={<PixelAiMate onOpenModal={handleOpenModal} />} />
          <Route path="/news" element={<GoodNewsPage currentUser={currentUser} />} />
          <Route
            path="/roadmap"
            element={
              <RoadmapMap
                showToast={triggerToast}
                currentUser={currentUser}
                onLogin={() => setIsAuthModalOpen(true)}
              />
            }
          />
          <Route
            path="/diary/:date"
            element={
              currentUser ? (
                <DiaryDayPage currentUser={currentUser} />
              ) : (
                <LoginRequired
                  title="다이어리는 로그인 후 볼 수 있어요"
                  description="활동 기록은 계정에 저장되는 개인 기록이라 로그인이 필요합니다."
                  onLogin={() => setIsAuthModalOpen(true)}
                />
              )
            }
          />
          <Route
            path="/my-page/*"
            element={
              currentUser ? (
                <MyPage currentUser={currentUser} />
              ) : (
                <LoginRequired
                  title="마이페이지는 로그인 후 볼 수 있어요"
                  description="신청 내역과 전자서명 약정은 계정에 저장되는 기록이라 로그인이 필요합니다."
                  onLogin={() => setIsAuthModalOpen(true)}
                />
              )
            }
          />
          <Route
            path="/manager-application"
            element={
              currentUser ? (
                <ManagerApplicationPage
                  currentUser={currentUser}
                  onBack={() => navigate('/roadmap')}
                  onSubmit={handleManagerApplicationSubmit}
                />
              ) : (
                <Navigate to="/roadmap" replace />
              )
            }
          />
          <Route
            path="/my-centers/*"
            element={
              currentUser?.role === 'CENTER_MANAGER' ? (
                <MyCenterPage currentUser={currentUser} />
              ) : (
                <Navigate to="/roadmap" replace />
              )
            }
          />
          <Route
            path="/management/*"
            element={
              currentUser?.role === 'OPERATOR' ? (
                <ManagementPage />
              ) : (
                <Navigate to="/volunteer" replace />
              )
            }
          />
          <Route path="*" element={<Navigate to="/volunteer" replace />} />
        </Routes>
      </main>

      <Modal
        isOpen={modalState.isOpen}
        title={modalState.title}
        type={modalState.type}
        onClose={handleCloseModal}
        onSubmit={handleModalSubmit}
      />

      <AiDock
        open={isAiDockOpen}
        onOpenChange={setIsAiDockOpen}
        onOpenModal={handleOpenModal}
        canUseAi={Boolean(currentUser)}
        onRequireLogin={() =>
          requireLogin('로그인이 필요합니다. AI 추천은 로그인 후 이용할 수 있어요.')
        }
      />

      <Toast message={toastMessage} />

      <AuthModal
        isOpen={isAuthModalOpen}
        onClose={() => setIsAuthModalOpen(false)}
        onAuthenticate={handleAuthenticate}
      />
    </div>
  );
}

export default App;
