import React, { useState, useEffect } from 'react';
import type { AiChatMessage } from '../../types';
import { playBeep } from '../../services/soundFx';
import { sendAiMessage, fetchAiHistory } from '../../services/aiApi';

interface PixelAiMateProps {
  onOpenModal: (title: string, type: 'volunteer' | 'donate') => void;
}

const INITIAL_MESSAGES: AiChatMessage[] = [
  {
    id: '1',
    sender: 'AI',
    text: '반가워요 픽셀용사님! 🤖 저는 당신의 맞춤 선행 큐레이터 Pixel AI Mate입니다. 희망하는 봉사/기부 조건(지역, 관심분야, 가능한 시간)을 말씀해 주세요!',
    createdAt: new Date().toLocaleTimeString(),
  },
];

export const PixelAiMate: React.FC<PixelAiMateProps> = ({ onOpenModal }) => {
  const [messages, setMessages] = useState<AiChatMessage[]>(INITIAL_MESSAGES);
  const [inputText, setInputText] = useState('');
  const [isTyping, setIsTyping] = useState(false);

  // 대화 히스토리 불러오기
  useEffect(() => {
    const loadHistory = async () => {
      try {
        const history = await fetchAiHistory();
        if (history && history.length > 0) {
          const formattedHistory: AiChatMessage[] = history.map((item) => {
            let recCard = undefined;
            if (item.recommendedMissionsJson) {
              try {
                const parsedCards = JSON.parse(item.recommendedMissionsJson);
                if (parsedCards && parsedCards.length > 0) {
                  const firstCard = parsedCards[0];
                  recCard = {
                    id: firstCard.opportunityId || 101,
                    title: firstCard.title,
                    category: 'VOLUNTEER' as const,
                    location: firstCard.region || '부산 지역',
                    organizer: '픽셀 케어 연동 봉사단',
                    tags: ['1365 연동', firstCard.badgeReward || 'LV1_SEED'],
                    link1365: 'https://www.1365.go.kr',
                  };
                }
              } catch (e) {}
            }

            return {
              id: item.id.toString(),
              sender: item.sender === 'USER' ? 'USER' : 'AI',
              text: item.message,
              recommendedCard: recCard,
              createdAt: new Date(item.createdAt).toLocaleTimeString(),
            };
          });

          setMessages([INITIAL_MESSAGES[0], ...formattedHistory]);
        }
      } catch (e) {
        console.error('AI 대화 히스토리 로드 실패:', e);
      }
    };

    loadHistory();
  }, []);

  const handleSend = async (userQuery?: string) => {
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

    try {
      // 실시간 백엔드 Upstage Solar LLM API 호출
      const aiResult = await sendAiMessage(textToSend);

      let recCard = undefined;
      if (aiResult.recommendedCards && aiResult.recommendedCards.length > 0) {
        const firstCard = aiResult.recommendedCards[0];
        recCard = {
          id: firstCard.opportunityId || Date.now(),
          title: firstCard.title,
          category: 'VOLUNTEER' as const,
          location: firstCard.region || '부산 지역',
          organizer: '픽셀 케어 온기 센터',
          tags: ['Upstage AI 큐레이션', firstCard.badgeReward || 'LV2_WARMTH'],
          link1365: 'https://www.1365.go.kr',
        };
      }

      const aiMsg: AiChatMessage = {
        id: (Date.now() + 1).toString(),
        sender: 'AI',
        text: aiResult.reply,
        recommendedCard: recCard,
        createdAt: new Date().toLocaleTimeString(),
      };

      setMessages((prev) => [...prev, aiMsg]);
      playBeep(880, 0.15);
    } catch (error) {
      console.error('AI Solar LLM 통신 실패:', error);
      const errorMsg: AiChatMessage = {
        id: (Date.now() + 1).toString(),
        sender: 'AI',
        text: '⚠️ 죄송합니다! AI 서버 통신 중 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.',
        createdAt: new Date().toLocaleTimeString(),
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setIsTyping(false);
    }
  };

  return (
    <div className="ai-dark-container" style={{ maxWidth: '840px', margin: '0 auto' }}>
      <div style={{ fontSize: '18px', fontWeight: '800', marginBottom: '14px', color: '#ffffff', display: 'flex', alignItems: 'center', gap: '8px', letterSpacing: '0.5px' }}>
        <span>🤖</span> Upstage Solar LLM AI 픽셀 큐레이터 (ArteDante Glow)
      </div>

      {/* Recommended Chips */}
      <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '20px' }}>
        <button className="pixel-btn" style={{ fontSize: '11px', background: 'rgba(255,255,255,0.1)', color: '#fff', borderColor: 'rgba(255,255,255,0.2)' }} onClick={() => handleSend('주말에 유기견 돌봄 봉사하고 싶어')}>
          🐕 유기견 돌봄 봉사
        </button>
        <button className="pixel-btn" style={{ fontSize: '11px', background: 'rgba(255,255,255,0.1)', color: '#fff', borderColor: 'rgba(255,255,255,0.2)' }} onClick={() => handleSend('해변 쓰레기 줍는 플로깅 추천해줘')}>
          🌊 해변 플로깅 봉사
        </button>
        <button className="pixel-btn" style={{ fontSize: '11px', background: 'rgba(255,255,255,0.1)', color: '#fff', borderColor: 'rgba(255,255,255,0.2)' }} onClick={() => handleSend('어르신 도시락 배달 봉사 추천해줘')}>
          🍲 독거어르신 도시락 배달
        </button>
      </div>

      {/* Messages Feed */}
      <div style={{ height: '380px', overflowY: 'auto', paddingRight: '8px', marginBottom: '20px' }}>
        {messages.map((m) => (
          <div
            key={m.id}
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: m.sender === 'USER' ? 'flex-end' : 'flex-start',
              marginBottom: '14px',
            }}
          >
            <div
              className="ai-glass-card"
              style={{
                background: m.sender === 'USER' ? '#ff3b30' : 'rgba(255, 255, 255, 0.08)',
                color: '#ffffff',
                maxWidth: '75%',
                fontSize: '13px',
                lineHeight: 1.5,
              }}
            >
              {m.text}
            </div>

            {m.recommendedCard && (
              <div className="ai-glass-card" style={{ marginTop: '6px', maxWidth: '340px', background: 'rgba(255,255,255,0.12)', border: '1px solid rgba(255,255,255,0.25)' }}>
                <div style={{ fontSize: '14px', fontWeight: 'bold', marginBottom: '6px', color: '#ffb703' }}>{m.recommendedCard.title}</div>
                <div style={{ fontSize: '11px', color: '#ccc', marginBottom: '10px', lineHeight: 1.4 }}>
                  📍 {m.recommendedCard.location}<br />
                  🏢 {m.recommendedCard.organizer}
                </div>
                <button
                  className="pixel-btn pixel-btn-red"
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
          <div style={{ fontSize: '12px', color: '#aaa', fontStyle: 'italic' }}>
            👾 Solar LLM AI가 답변을 생성 중입니다...
          </div>
        )}
      </div>

      {/* Input */}
      <div style={{ display: 'flex', gap: '10px' }}>
        <input
          type="text"
          className="pixel-input"
          style={{ flex: 1, background: 'rgba(255,255,255,0.08)', color: '#ffffff', borderColor: 'rgba(255,255,255,0.2)' }}
          placeholder="예: 해운대 근처에서 주말 오전 환경 봉사하고 싶어!"
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSend()}
        />
        <button className="pixel-btn pixel-btn-red" onClick={() => handleSend()}>
          전송 🚀
        </button>
      </div>
    </div>
  );
};
