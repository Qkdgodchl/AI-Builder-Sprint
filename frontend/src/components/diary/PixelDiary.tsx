import React, { useState } from 'react';
import type { PixelDiaryItem } from '../../types';
import { playBeep } from '../../services/soundFx';

interface PixelDiaryProps {
  onAddDiary: (tempIncrease: number) => void;
  showToast: (message: string) => void;
}

const INITIAL_DIARIES: PixelDiaryItem[] = [
  {
    id: 1,
    author: '해운대 픽셀용사',
    date: '2026.07.28',
    emotion: '😊',
    content: '오늘 해운대 플로깅 봉사에 다녀왔습니다! 쓰레기 3kg이나 주워서 동네가 더 깨끗해졌어요! 🌊',
    hearts: 12,
  },
  {
    id: 2,
    author: '금정 온기 요정',
    date: '2026.07.27',
    emotion: '🥰',
    content: '독거 어르신께 도시락 배달해드리고 손잡아드렸더니 너무 따뜻해졌습니다. 또 갈 거예요! 🍲',
    hearts: 18,
  },
];

export const PixelDiary: React.FC<PixelDiaryProps> = ({ onAddDiary, showToast }) => {
  const [diaries, setDiaries] = useState<PixelDiaryItem[]>(INITIAL_DIARIES);
  const [author, setAuthor] = useState('');
  const [content, setContent] = useState('');
  const [emotion, setEmotion] = useState('😊');

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim()) return;

    const newEntry: PixelDiaryItem = {
      id: Date.now(),
      author: author.trim() || '익명 픽셀용사',
      date: new Date().toLocaleDateString('ko-KR'),
      emotion,
      content: content.trim(),
      hearts: 1,
    };

    setDiaries((prev) => [newEntry, ...prev]);
    onAddDiary(0.5);
    showToast('📖 따뜻한 픽셀 일기가 등록되었습니다! 온기 +0.5°C 상승!');
    playBeep(587, 0.15);

    setAuthor('');
    setContent('');
  };

  const handleLike = (id: number) => {
    setDiaries((prev) =>
      prev.map((d) => (d.id === id ? { ...d, hearts: d.hearts + 1 } : d))
    );
    onAddDiary(0.1);
    playBeep(784, 0.1);
  };

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div className="pixel-box" style={{ marginBottom: '20px', background: '#faf0ca' }}>
        <div style={{ fontSize: '16px', fontWeight: 'bold', marginBottom: '12px', color: '#1a1a24' }}>
          ✏️ 오늘의 픽셀 봉사 일기 작성하기
        </div>

        <form onSubmit={handleCreate}>
          <div style={{ display: 'flex', gap: '8px', marginBottom: '12px', flexWrap: 'wrap' }}>
            <input
              type="text"
              className="pixel-input"
              style={{ flex: 1, minWidth: '160px' }}
              placeholder="작성자 닉네임 (예: 센텀 온기용사)"
              value={author}
              onChange={(e) => setAuthor(e.target.value)}
            />
            <div style={{ display: 'flex', gap: '4px', alignItems: 'center' }}>
              <span style={{ fontSize: '12px' }}>오늘의 감정:</span>
              {['😊', '🥰', '🔥', '🌱'].map((emo) => (
                <button
                  type="button"
                  key={emo}
                  className="pixel-btn"
                  style={{
                    padding: '4px 8px',
                    fontSize: '14px',
                    background: emotion === emo ? '#ff9f1c' : '#fff',
                  }}
                  onClick={() => setEmotion(emo)}
                >
                  {emo}
                </button>
              ))}
            </div>
          </div>

          <textarea
            className="pixel-input"
            style={{ width: '100%', height: '80px', marginBottom: '12px', resize: 'none' }}
            placeholder="오늘 행한 작은 선행이나 봉사 소감을 일기로 남겨주세요!"
            value={content}
            onChange={(e) => setContent(e.target.value)}
          />

          <div style={{ textAlign: 'right' }}>
            <button type="submit" className="pixel-btn pixel-btn-red">
              📖 일기 저장하기 (온기 +0.5°C)
            </button>
          </div>
        </form>
      </div>

      {/* Feed List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
        {diaries.map((d) => (
          <div key={d.id} className="pixel-box" style={{ background: '#fff' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '12px', color: '#666' }}>
              <span>
                {d.emotion} <b>{d.author}</b>
              </span>
              <span>📅 {d.date}</span>
            </div>

            <div style={{ fontSize: '14px', color: '#222', lineHeight: 1.5, marginBottom: '12px' }}>
              {d.content}
            </div>

            <div style={{ textAlign: 'right' }}>
              <button
                className="pixel-btn"
                style={{ fontSize: '11px', background: '#ffe5ec', borderColor: '#ff4d6d', color: '#c9184a' }}
                onClick={() => handleLike(d.id)}
              >
                ❤️ 응원 하트 {d.hearts}
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
