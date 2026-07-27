import React, { useState, useEffect } from 'react';
import type { PixelDiaryItem } from '../types';
import { playBeep } from '../services/soundFx';

interface PixelDiaryProps {
  onAddDiary: (increaseVal: number) => void;
  showToast: (msg: string) => void;
}

const INITIAL_DIARIES: PixelDiaryItem[] = [
  {
    id: 1,
    author: '부산픽셀용사',
    date: '오늘 14:20',
    emotion: '😊',
    content: '주말에 해운대 해변 플로깅에 참여했습니다! 쓰레기 3kg 모으고 온기 뱃지도 획득했어요. 뿌듯합니다 🔥',
    hearts: 12,
  },
  {
    id: 2,
    author: '따뜻한마음',
    date: '어제 18:45',
    emotion: '🥰',
    content: '유기견 보육원에서 아기 강아지 산책 봉사를 다녀왔습니다. 다들 따뜻한 온기 꼭 나눠주세요!',
    hearts: 19,
  },
];

export const PixelDiary: React.FC<PixelDiaryProps> = ({ onAddDiary, showToast }) => {
  const [diaries, setDiaries] = useState<PixelDiaryItem[]>(() => {
    const saved = localStorage.getItem('pixel_diaries');
    return saved ? JSON.parse(saved) : INITIAL_DIARIES;
  });

  const [author, setAuthor] = useState('');
  const [emotion, setEmotion] = useState('😊 기쁨');
  const [content, setContent] = useState('');

  useEffect(() => {
    localStorage.setItem('pixel_diaries', JSON.stringify(diaries));
  }, [diaries]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim()) {
      showToast('⚠️ 오늘 봉사 일기 내용을 적어주세요!');
      return;
    }

    const newDiary: PixelDiaryItem = {
      id: Date.now(),
      author: author.trim() || '익명용사',
      date: '방금 전',
      emotion: emotion.split(' ')[0],
      content: content.trim(),
      hearts: 1,
    };

    setDiaries([newDiary, ...diaries]);
    setContent('');
    showToast('📖 오늘의 봉사 일기가 일기장에 남겨졌습니다!');
    onAddDiary(0.4);
    playBeep(580, 0.15);
  };

  const handleHeart = (id: number) => {
    setDiaries((prev) =>
      prev.map((d) => (d.id === id ? { ...d, hearts: d.hearts + 1 } : d))
    );
    showToast('❤️ 응원 픽셀 온기를 보냈습니다!');
    onAddDiary(0.1);
    playBeep(700, 0.1);
  };

  return (
    <div className="diary-section">
      <div style={{ fontSize: '16px', fontWeight: 'bold', marginBottom: '14px', color: '#1a1a24' }}>
        📖 픽셀 일기장 & 커뮤니티 (봉사 일기 나누기)
      </div>

      {/* Diary Form */}
      <form onSubmit={handleSubmit} className="pixel-box" style={{ padding: '16px', marginBottom: '20px' }}>
        <div style={{ display: 'flex', gap: '10px', marginBottom: '10px', flexWrap: 'wrap' }}>
          <input
            type="text"
            className="pixel-input"
            placeholder="닉네임 (예: 부산용사)"
            value={author}
            onChange={(e) => setAuthor(e.target.value)}
            style={{ maxWidth: '180px' }}
          />
          <select
            className="pixel-input"
            value={emotion}
            onChange={(e) => setEmotion(e.target.value)}
            style={{ maxWidth: '160px' }}
          >
            <option value="😊 기쁨">😊 기쁨</option>
            <option value="🥰 감동">🥰 감동</option>
            <option value="🔥 열정">🔥 열정</option>
            <option value="🌱 성장">🌱 성장</option>
          </select>
        </div>

        <textarea
          className="pixel-input"
          placeholder="오늘 어떤 따뜻한 봉사나 기부를 경험하셨나요? 픽셀 일기를 기록해 보세요!"
          rows={3}
          style={{ width: '100%', marginBottom: '10px', resize: 'vertical' }}
          value={content}
          onChange={(e) => setContent(e.target.value)}
        />

        <button type="submit" className="pixel-btn" style={{ width: '100%' }}>
          📖 픽셀 일기 남기기 (+온도계 0.4°C 상승)
        </button>
      </form>

      {/* Diary Feed */}
      <div>
        {diaries.map((diary) => (
          <div key={diary.id} className="diary-card">
            <div className="diary-header">
              <span className="diary-title">🎮 {diary.author}의 봉사 일기</span>
              <span>{diary.date} • {diary.emotion}</span>
            </div>
            <div className="diary-body">{diary.content}</div>
            <div className="diary-footer">
              <span className="pixel-tag" style={{ background: '#55c54f' }}>
                🤝 봉사 일기
              </span>
              <button
                className="pixel-btn pixel-btn-red"
                style={{ fontSize: '11px', padding: '4px 8px' }}
                onClick={() => handleHeart(diary.id)}
              >
                ❤️ 픽셀 온기 {diary.hearts}
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
