import { useState } from 'react';
import { Header } from './components/common/Header';
import { Modal } from './components/common/Modal';
import { Toast } from './components/common/Toast';
import { PixelAiMate } from './components/ai/PixelAiMate';
import { VolunteerCatalog } from './components/volunteer/VolunteerCatalog';
import { PixelDiary } from './components/diary/PixelDiary';
import { RoadmapMap } from './components/roadmap/RoadmapMap';
import { playBeep } from './services/soundFx';

export function App() {
  const [activeTab, setActiveTab] = useState<'ai' | 'volunteer' | 'diary' | 'roadmap'>('ai');
  const [temperature, setTemperature] = useState<number>(78.4);
  const [totalDonation, setTotalDonation] = useState<number>(1250000);
  const [totalHours, setTotalHours] = useState<number>(342);
  const [totalMembers, setTotalMembers] = useState<number>(128);

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

  return (
    <div className="app-container">
      <Header
        temperature={temperature}
        totalDonation={totalDonation}
        totalHours={totalHours}
        totalMembers={totalMembers}
        activeTab={activeTab}
        setActiveTab={setActiveTab}
      />

      <main>
        {activeTab === 'ai' && <PixelAiMate onOpenModal={handleOpenModal} />}
        {activeTab === 'volunteer' && (
          <VolunteerCatalog onOpenModal={handleOpenModal} showToast={triggerToast} />
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
    </div>
  );
}

export default App;
