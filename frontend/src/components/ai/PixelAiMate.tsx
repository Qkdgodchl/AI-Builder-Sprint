import React, { useState } from 'react';
import type { AiChatMessage } from '../../types';
import { playBeep } from '../../services/soundFx';

interface PixelAiMateProps {
  onOpenModal: (title: string, type: 'volunteer' | 'donate') => void;
}

const INITIAL_MESSAGES: AiChatMessage[] = [
  {
    id: '1',
    sender: 'AI',
    text: '반가워요 용사님! 👾 저는 당신의 선행 메이트 Pixel AI Mate입니다. 어떤 봉사나 기부를 찾고 계신가요?',
    createdAt: new Date().toLocaleTimeString(),
  },
];

export const PixelAiMate: React.FC<PixelAiMateProps> = ({ onOpenModal }) => {
  const [messages, setMessages] = useState<AiChatMessage[]>(INITIAL_MESSAGES);
  const [inputText, setInputText] = useState('');
  const [isTyping, setIsTyping] = useState(false);

  const handleSend = (userQuery?: string) => {
    const textToSend = userQuery || inputText;
    if (!textToSend.trim()) return;

    const userMsg: AiChatMessage = {
      id: Date.now().toString(),
      sender: 'USER',
      text: textToSend,
      createdAt: new Date().toLocaleTimeString(),
    };

    setMessages((prev) => [...prev, userMsg]);
    if (!userQuery) setInputText('');
    setIsTyping(true);
    playBeep(440, 0.1);

    setTimeout(() => {
      let aiResponseText = '용사님의 마음에 딱 맞는 맞춤 퀘스트를 큐레이션했습니다!';
      let recCard = undefined;

      if (textToSend.includes('동물') || textToSend.includes('유기견')) {
        aiResponseText = '🐕 동물들을 사랑하는 따뜻한 마음을 가지셨군요! 주말 유기견 보육원 봉사 미션을 추천합니다.';
        recCard = {
          id: 101,
          title: '🐕 유기견 보육원 주말 돌봄 봉사',
          category: 'VOLUNTEER' as const,
          location: '부산 북구 동물보호센터',
          organizer: '부산 동네 온기 봉사단',
          tags: ['1365 연동', '4시간 인정', '주말'],
          link1365: 'https://www.1365.go.kr',
        };
      } else if (textToSend.includes('환경') || textToSend.includes('바다') || textToSend.includes('해변')) {
        aiResponseText = '🌊 깨끗한 지구를 만드는 픽셀 그린 영웅! 해운대 플로깅 봉사를 추천해 드려요.';
        recCard = {
          id: 102,
          title: '🌊 해운대 해변 픽셀 플로깅 정화',
          category: 'VOLUNTEER' as const,
          location: '부산 해운대 구남로 광장',
          organizer: '그린 픽셀 에코 클럽',
          tags: ['1365 연동', '3시간 인정', '환경'],
          link1365: 'https://www.1365.go.kr',
        };
      } else {
        aiResponseText = '🍲 이웃에게 전하는 온기 한 그릇! 독거어르신 도시락 배달 봉사 미션을 추천합니다.';
        recCard = {
          id: 103,
          title: '🍲 독거어르신 온기 도시락 배달',
          category: 'VOLUNTEER' as const,
          location: '부산 금정구 종합복지관',
          organizer: '사랑의 픽셀 이웃',
          tags: ['1365 연동', '4시간 인정', '복지'],
          link1365: 'https://www.1365.go.kr',
        };
      }

      const aiMsg: AiChatMessage = {
        id: (Date.now() + 1).toString(),
        sender: 'AI',
        text: aiResponseText,
        recommendedCard: recCard,
        createdAt: new Date().toLocaleTimeString(),
      };

      setMessages((prev) => [...prev, aiMsg]);
      setIsTyping(false);
      playBeep(880, 0.15);
    }, 1000);
  };

  return (
    <div className="pixel-box" style={{ maxWidth: '800px', margin: '0 auto' }}>
      <div style={{ fontSize: '16px', fontWeight: 'bold', marginBottom: '12px', color: '#1a1a24', display: 'flex', alignItems: 'center', gap: '8px' }}>
        <span>🤖</span> Upstage Solar LLM 픽셀 AI 큐레이터
      </div>

      {/* Recommended Chips */}
      <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginBottom: '16px' }}>
        <button className="pixel-btn" style={{ fontSize: '11px', background: '#eee', color: '#333' }} onClick={() => handleSend('주말에 유기견 돌봄 봉사하고 싶어')}>
          🐕 유기견 돌봄 봉사
        </button>
        <button className="pixel-btn" style={{ fontSize: '11px', background: '#eee', color: '#333' }} onClick={() => handleSend('해변 쓰레기 줍는 플로깅 추천해줘')}>
          🌊 해변 플로깅 봉사
        </button>
        <button className="pixel-btn" style={{ fontSize: '11px', background: '#eee', color: '#333' }} onClick={() => handleSend('어르신 도시락 배달 봉사 추천해줘')}>
          🍲 독거어르신 도시락 배달
        </button>
      </div>

      {/* Messages */}
      <div style={{ height: '360px', overflowY: 'auto', paddingRight: '6px', marginBottom: '16px' }}>
        {messages.map((m) => (
          <div
            key={m.id}
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: m.sender === 'USER' ? 'flex-end' : 'flex-start',
              marginBottom: '12px',
            }}
          >
            <div
              className="pixel-box"
              style={{
                background: m.sender === 'USER' ? '#2ec4b6' : '#fff',
                color: m.sender === 'USER' ? '#fff' : '#1a1a24',
                maxWidth: '75%',
                fontSize: '13px',
                padding: '10px 14px',
              }}
            >
              {m.text}
            </div>

            {m.recommendedCard && (
              <div className="pixel-box item-card" style={{ marginTop: '8px', maxWidth: '320px', background: '#fff' }}>
                <div className="title" style={{ fontSize: '13px' }}>{m.recommendedCard.title}</div>
                <div className="meta" style={{ fontSize: '11px', marginBottom: '8px' }}>
                  📍 {m.recommendedCard.location}<br />
                  🏢 {m.recommendedCard.organizer}
                </div>
                <button
                  className="pixel-btn"
                  style={{ width: '100%', fontSize: '11px' }}
                  onClick={() => onOpenModal(m.recommendedCard!.title, 'volunteer')}
                >
                  ⚡ 1초 간편 신청하기
                </button>
              </div>
            )}
          </div>
        ))}

        {isTyping && (
          <div style={{ fontSize: '12px', color: '#888', fontStyle: 'italic' }}>
            👾 AI Mate가 생각 중입니다...
          </div>
        )}
      </div>

      {/* Input */}
      <div style={{ display: 'flex', gap: '8px' }}>
        <input
          type="text"
          className="pixel-input"
          style={{ flex: 1 }}
          placeholder="예: 센텀시티 근처에서 할 수 있는 주말 봉사 추천해줘!"
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSend()}
        />
        <button className="pixel-btn" onClick={() => handleSend()}>
          전송 🚀
        </button>
      </div>
    </div>
  );
};
