import { useState } from 'react';
import { Header } from './components/common/Header';
import { Modal } from './components/common/Modal';
import { Toast } from './components/common/Toast';
import { PixelAiMate } from './components/ai/PixelAiMate';
import { VolunteerCatalog } from './components/volunteer/VolunteerCatalog';
import { PixelDiary } from './components/diary/PixelDiary';
import { RoadmapMap } from './components/roadmap/RoadmapMap';
import { AuthModal } from './components/auth/AuthModal';
import { playBeep } from './services/soundFx';

type ActiveTab = 'ai' | 'volunteer' | 'diary' | 'roadmap';
type AuthModalMode = 'login' | 'admin';

export function App() {
  const [activeTab, setActiveTab] = useState<ActiveTab>('ai');
  const [temperature, setTemperature] = useState<number>(78.4);
  const [totalDonation, setTotalDonation] = useState<number>(1250000);
  const [totalHours, setTotalHours] = useState<number>(342);
  const [totalMembers, setTotalMembers] = useState<number>(128);
  const [currentUser, setCurrentUser] = useState<string | null>(
    () => localStorage.getItem('pixel-care-user'),
  );
  const [authModalMode, setAuthModalMode] = useState<AuthModalMode | null>(null);

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

  const handleLogin = (email: string) => {
    localStorage.setItem('pixel-care-user', email);
    setCurrentUser(email);
    setAuthModalMode(null);
    triggerToast(`반가워요! ${email} 계정으로 로그인했습니다.`);
  };

  const handleLogout = () => {
    localStorage.removeItem('pixel-care-user');
    setCurrentUser(null);
    setActiveTab('ai');
    triggerToast('로그아웃되었습니다.');
  };

  const handleAdminApplication = (organizationName: string) => {
    setAuthModalMode(null);
    triggerToast(`${organizationName} 관리자 계정 신청이 접수되었습니다.`);
  };

  return (
    <div className="app-container">
      <Header
        temperature={temperature}
        totalDonation={totalDonation}
        totalHours={totalHours}
        totalMembers={totalMembers}
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        currentUser={currentUser}
        onLogin={() => setAuthModalMode('login')}
        onLogout={handleLogout}
        onAdminApply={() => setAuthModalMode('admin')}
      />

      <main>
        {activeTab === 'ai' && <PixelAiMate onOpenModal={handleOpenModal} />}
        {activeTab === 'volunteer' && (
          <VolunteerCatalog />
        )}
        {activeTab === 'diary' && (
          <PixelDiary onAddDiary={handleIncreaseTemp} showToast={triggerToast} />
        )}
        {activeTab === 'roadmap' && <RoadmapMap showToast={triggerToast} />}
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
        mode={authModalMode}
        currentUser={currentUser}
        onClose={() => setAuthModalMode(null)}
        onLogin={handleLogin}
        onAdminApply={handleAdminApplication}
      />
    </div>
  );
}

export default App;
