import React, { useState } from 'react';
import { createVolunteer } from '../../services/volunteerApi';
import { playBeep } from '../../services/soundFx';

interface RegisterVolunteerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (message: string) => void;
}

export const RegisterVolunteerModal: React.FC<RegisterVolunteerModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [title, setTitle] = useState('');
  const [location, setLocation] = useState('');
  const [organizer, setOrganizer] = useState('');
  const [tagsInput, setTagsInput] = useState('1365 연동, 4시간 인정');
  const [link1365, setLink1365] = useState('https://www.1365.go.kr');
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !location.trim() || !organizer.trim()) {
      alert('제목, 위치, 주관 기관을 모두 입력해주세요!');
      return;
    }

    setIsSubmitting(true);
    try {
      const parsedTags = tagsInput
        .split(',')
        .map((t) => t.trim())
        .filter((t) => t.length > 0);

      await createVolunteer({
        title: title.trim(),
        location: location.trim(),
        organizer: organizer.trim(),
        category: 'VOLUNTEER',
        tags: parsedTags.length > 0 ? parsedTags : ['1365 연동', '봉사'],
        link1365: link1365.trim() || 'https://www.1365.go.kr',
      });

      playBeep(600, 0.2);
      onSuccess(`✨ 새로운 봉사 미션 "${title.trim()}"이 등록되었습니다!`);
      setTitle('');
      setLocation('');
      setOrganizer('');
      setTagsInput('1365 연동, 4시간 인정');
      setLink1365('https://www.1365.go.kr');
      onClose();
    } catch (err) {
      console.error(err);
      alert('봉사 등록 중 오류가 발생했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay active" onClick={onClose}>
      <div className="pixel-box modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '480px', width: '90%' }}>
        <div style={{ fontSize: '16px', fontWeight: 'bold', marginBottom: '12px', color: '#1a1a24', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span>🤝</span> 픽셀 봉사 미션 API 신규 등록
        </div>
        <div style={{ fontSize: '12px', color: '#666', marginBottom: '16px' }}>
          API 백엔드로 신규 봉사 데이터를 직접 등록합니다. (1365 공공데이터 연동 포맷)
        </div>

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '12px' }}>
            <label style={{ display: 'block', fontSize: '11px', fontWeight: 'bold', marginBottom: '4px' }}>
              📌 봉사 미션 제목 *
            </label>
            <input
              type="text"
              className="pixel-input"
              style={{ width: '100%' }}
              placeholder="예: 🐕 유기견 보육원 주말 돌봄 봉사"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
            />
          </div>

          <div style={{ marginBottom: '12px' }}>
            <label style={{ display: 'block', fontSize: '11px', fontWeight: 'bold', marginBottom: '4px' }}>
              📍 봉사 활동 장소 *
            </label>
            <input
              type="text"
              className="pixel-input"
              style={{ width: '100%' }}
              placeholder="예: 부산 북구 동물보호센터"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              required
            />
          </div>

          <div style={{ marginBottom: '12px' }}>
            <label style={{ display: 'block', fontSize: '11px', fontWeight: 'bold', marginBottom: '4px' }}>
              🏢 주관 기관 / 단체명 *
            </label>
            <input
              type="text"
              className="pixel-input"
              style={{ width: '100%' }}
              placeholder="예: 부산 동네 온기 봉사단"
              value={organizer}
              onChange={(e) => setOrganizer(e.target.value)}
              required
            />
          </div>

          <div style={{ marginBottom: '12px' }}>
            <label style={{ display: 'block', fontSize: '11px', fontWeight: 'bold', marginBottom: '4px' }}>
              🏷️ 태그 (쉼표로 구분)
            </label>
            <input
              type="text"
              className="pixel-input"
              style={{ width: '100%' }}
              placeholder="예: 1365 연동, 4시간 인정, 주말"
              value={tagsInput}
              onChange={(e) => setTagsInput(e.target.value)}
            />
          </div>

          <div style={{ marginBottom: '16px' }}>
            <label style={{ display: 'block', fontSize: '11px', fontWeight: 'bold', marginBottom: '4px' }}>
              🔗 1365 원문 URL
            </label>
            <input
              type="text"
              className="pixel-input"
              style={{ width: '100%' }}
              placeholder="https://www.1365.go.kr"
              value={link1365}
              onChange={(e) => setLink1365(e.target.value)}
            />
          </div>

          <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end' }}>
            <button
              type="button"
              className="pixel-btn"
              style={{ background: '#888', borderColor: '#aaa' }}
              onClick={onClose}
              disabled={isSubmitting}
            >
              취소
            </button>
            <button
              type="submit"
              className="pixel-btn"
              style={{ background: '#2ec4b6', borderColor: '#005f73', color: '#fff' }}
              disabled={isSubmitting}
            >
              {isSubmitting ? '등록 중...' : '➕ API 봉사 등록'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
