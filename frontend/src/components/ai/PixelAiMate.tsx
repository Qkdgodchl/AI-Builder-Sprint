import React, { useState, useEffect } from 'react';
import { playBeep } from '../../services/soundFx';
import { sendAiMessage, fetchAiHistory, clearAiHistory } from '../../services/aiApi';

interface PixelAiMateProps {
  onOpenModal: (title: string, type: 'volunteer' | 'donate') => void;
}

export interface AiChatMessage {
  id: string;
  sender: 'USER' | 'AI';
  text: string;
  recommendedCard?: {
    id: number;
    title: string;
    category: 'VOLUNTEER' | 'DONATION';
    location: string;
    organizer: string;
    tags: string[];
  };
  createdAt: string;
}

const INITIAL_MESSAGES: AiChatMessage[] = [
  {
    id: 'init-1',
    sender: 'AI',
    text: '안녕! 나는 너의 픽셀 케어 AI 메이트야 🤖✨ 부산 지역 봉사활동이나 기부처, 혹은 오늘 기분에 맞는 선행 활동을 물어봐줘! 예: "해운대 근처에서 할 수 있는 주말 봉사 추천해줘"',
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
                    tags: ['봉사 추천', firstCard.badgeReward || 'LV1_SEED'],
                  };
                }
              } catch (e) {}
            }

            return {
              id: item.id.toString(),
              sender: item.sender === 'USER' ? 'USER' : 'AI',
              text: item.message,
              recommendedCard: recCard,
              createdAt: item.createdAt ? new Date(item.createdAt).toLocaleTimeString() : '과거 대화',
            };
          });
          setMessages(formattedHistory);
        }
      } catch (e) {
        console.error('Failed to load AI history:', e);
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

  const handleClearHistory = async () => {
    if (!window.confirm('AI 메이트와의 모든 대화 내역을 초기화하시겠습니까?')) {
      return;
    }
    const success = await clearAiHistory();
    if (success) {
      setMessages(INITIAL_MESSAGES);
      playBeep(330, 0.15);
    } else {
      alert('대화 내역 초기화 중 오류가 발생했습니다.');
    }
  };

  const handleQuickPrompt = (promptText: string) => {
    handleSend(promptText);
  };

  return (
    <section className="pixel-ai-container">
      {/* Header */}
      <div className="ai-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div className="ai-avatar">🤖</div>
          <div>
            <h2 style={{ fontSize: '18px', fontWeight: '800', margin: 0, color: '#1a1a24' }}>
              PIXEL AI MATE
            </h2>
            <p style={{ fontSize: '12px', color: '#666', margin: '2px 0 0' }}>
              Upstage Solar LLM 파워드 · 부산 선행 큐레이터
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <button
            type="button"
            className="pixel-button"
            style={{ fontSize: '11px', padding: '5px 10px', background: '#ffe5ec', color: '#ff3b30', borderColor: '#ff3b30' }}
            onClick={handleClearHistory}
          >
            🗑️ 대화 초기화
          </button>
          <span className="online-badge">● ONLINE</span>
        </div>
      </div>

      {/* Quick Prompts */}
      <div className="ai-quick-prompts">
        <button type="button" onClick={() => handleQuickPrompt('해운대 근처 주말 봉사 추천해줘')}>
          🐕 유기견 봉사
        </button>
        <button type="button" onClick={() => handleQuickPrompt('오늘 1시간 정도 할 수 있는 소규모 기부 활동')}>
          🍲 도시락 배달
        </button>
        <button type="button" onClick={() => handleQuickPrompt('어린이 학습 지도 및 동행 봉사')}>
          📚 학습 지도
        </button>
      </div>

      {/* Chat Messages Stream */}
      <div className="ai-chat-body">
        {messages.map((msg) => (
          <div key={msg.id} className={`chat-bubble-row ${msg.sender === 'USER' ? 'user-row' : 'ai-row'}`}>
            {msg.sender === 'AI' && <div className="chat-avatar">🤖</div>}
            
            <div className="chat-content">
              <div className={`chat-bubble ${msg.sender === 'USER' ? 'user-bubble' : 'ai-bubble'}`}>
                {msg.text}
              </div>

              {/* Recommended Mission Card (Bento Overlay) */}
              {msg.recommendedCard && (
                <div className="ai-recommended-card">
                  <div className="card-badge">✨ UPSTAGE AI MATCH</div>
                  <h4>{msg.recommendedCard.title}</h4>
                  <p>📍 위치: {msg.recommendedCard.location} | 주관: {msg.recommendedCard.organizer}</p>
                  <div className="card-tags">
                    {msg.recommendedCard.tags.map((tag) => (
                      <span key={tag} className="tag">{tag}</span>
                    ))}
                  </div>
                  <div style={{ display: 'flex', gap: '8px', marginTop: '12px' }}>
                    <button
                      type="button"
                      className="pixel-button primary"
                      style={{ fontSize: '12px', padding: '6px 12px' }}
                      onClick={() => onOpenModal(msg.recommendedCard!.title, 'volunteer')}
                    >
                      ⚡ 간편 신청하기
                    </button>
                  </div>
                </div>
              )}

              <span className="chat-timestamp">{msg.createdAt}</span>
            </div>
          </div>
        ))}

        {isTyping && (
          <div className="chat-bubble-row ai-row">
            <div className="chat-avatar">🤖</div>
            <div className="chat-bubble ai-bubble typing">
              <span>.</span><span>.</span><span>.</span> Upstage Solar LLM이 최적의 선행 활동을 탐색 중입니다
            </div>
          </div>
        )}
      </div>

      {/* Input Form */}
      <form
        className="ai-input-form"
        onSubmit={(e) => {
          e.preventDefault();
          handleSend();
        }}
      >
        <input
          type="text"
          className="pixel-input"
          placeholder="AI 메이트에게 부산 선행 활동이나 기부처를 물어보세요! (예: 금정구 도시락 봉사)"
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
        />
        <button type="submit" className="pixel-button primary" style={{ background: '#ff3b30' }}>
          🚀 전송
        </button>
      </form>
    </section>
  );
};
