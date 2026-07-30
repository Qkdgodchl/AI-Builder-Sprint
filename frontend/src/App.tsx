import { useState } from 'react';
import { Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import { Header } from './components/common/Header';
import { Modal } from './components/common/Modal';
import { Toast } from './components/common/Toast';
import { PixelAiMate } from './components/ai/PixelAiMate';
import { VolunteerCatalog } from './components/volunteer/VolunteerCatalog';
import { PixelDiary } from './components/diary/PixelDiary';
import { RoadmapMap } from './components/roadmap/RoadmapMap';
import { AuthModal } from './components/auth/AuthModal';
import { ManagerApplicationPage } from './components/user/ManagerApplicationPage';
import { MyPage } from './components/user/MyPage';
import { MyCenterPage } from './components/center/MyCenterPage';
import { playBeep } from './services/soundFx';
import { logout as logoutApi } from './services/authApi';
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
  const [temperature, setTemperature] = useState<number>(78.4);
  const [totalDonation, setTotalDonation] = useState<number>(1250000);
  const [totalHours, setTotalHours] = useState<number>(342);
  const [totalMembers, setTotalMembers] = useState<number>(128);
  const [currentUser, setCurrentUser] = useState<SessionUser | null>(loadStoredUser);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);

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

  const triggerToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => {
      setToastMessage(null);
    }, 3000);
  };

  const handleOpenModal = (title: string, type: 'volunteer' | 'donate') => {
    setModalState({ isOpen: true, title, type });
    playBeep(520, 0.1);
  };

  const handleCloseModal = () => {
    setModalState((prev) => ({ ...prev, isOpen: false }));
  };

  const handleModalSubmit = (name: string, amount: number) => {
    if (modalState.type === 'donate') {
      setTotalDonation((prev) => prev + amount);
      triggerToast(`❤️ ${name}님, ${amount.toLocaleString()}원 기부 완료! 뱃지를 획득하셨습니다!`);
    } else {
      setTotalMembers((prev) => prev + 1);
      setTotalHours((prev) => prev + 4);
      triggerToast(`⚡ ${name}님, 간편 봉사 신청이 완료되었습니다! 뱃지 발급!`);
    }

    setTemperature((prev) => Math.min(99.9, prev + 0.8));
    handleCloseModal();
    playBeep(660, 0.2);
  };

  const handleIncreaseTemp = (val: number) => {
    setTemperature((prev) => Math.min(99.9, prev + val));
  };

  const handleAuthenticate = (user: SessionUser) => {
    localStorage.setItem('pixel-care-user', JSON.stringify(user));
    setCurrentUser(user);
    setIsAuthModalOpen(false);
    triggerToast(`${user.nickname}님, 로그인했습니다.`);
  };

  const handleLogout = async () => {
    await logoutApi();
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
        temperature={temperature}
        totalDonation={totalDonation}
        totalHours={totalHours}
        totalMembers={totalMembers}
        currentUser={currentUser}
        onLogin={() => setIsAuthModalOpen(true)}
        onLogout={handleLogout}
        onAdminApply={() => navigate('/manager-application')}
      />

      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/volunteer" replace />} />
          <Route path="/volunteer/*" element={<VolunteerCatalog />} />
          <Route path="/community" element={<PixelDiary onAddDiary={handleIncreaseTemp} showToast={triggerToast} />} />
          <Route path="/community/posts/:id" element={<PixelDiary onAddDiary={handleIncreaseTemp} showToast={triggerToast} />} />
          <Route path="/ai" element={<PixelAiMate onOpenModal={handleOpenModal} />} />
          <Route path="/roadmap" element={<RoadmapMap showToast={triggerToast} />} />
          <Route
            path="/my-page"
            element={
              currentUser ? (
                <MyPage currentUser={currentUser} />
              ) : (
                <Navigate to="/volunteer" replace />
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
