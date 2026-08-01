import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { playBeep } from '../../services/soundFx';
import { sendAiMessage, fetchAiHistory, clearAiHistory } from '../../services/aiApi';

interface PixelAiMateProps {
  onOpenModal: (title: string, type: 'volunteer' | 'donate') => void;
}

interface RecommendedCard {
  id: number;
  title: string;
  category: 'VOLUNTEER' | 'DONATION';
  location: string;
  organizer: string;
  tags: string[];
}

export interface AiChatMessage {
  id: string;
  sender: 'USER' | 'AI';
  text: string;
  /** DB에서 실제 매칭된 카드 목록. 없으면 빈 배열 */
  recommendedCards: RecommendedCard[];
  createdAt: string;
}

const INITIAL_MESSAGES: AiChatMessage[] = [
  {
    id: 'init-1',
    sender: 'AI',
    text: '안녕! 나는 픽셀 케어 AI 메이트야 🤖✨\n부산 지역 봉사활동이나 기부처를 물어봐줘!\n우리 DB에 등록된 실제 활동만 정확하게 추천해드려요.\n\n예: "금정구 봉사 추천해줘" / "유기견 봉사 알려줘" / "부산대 근처 봉사"',
    recommendedCards: [],
    createdAt: new Date().toLocaleTimeString(),
  },
];

/** 백엔드 API 카드 → 프론트 카드 타입 변환 */
function parseCard(raw: any): RecommendedCard | null {
  const id = raw?.opportunityId ?? raw?.id;
  if (!id || !raw?.title) return null;
  const isDonation = (raw.category || '').includes('GENERAL') ||
    (raw.category || '').includes('LEGACY') ||
    (raw.category || '').includes('UNESCO') ||
    (raw.category || '').includes('HERITAGE') ||
    (raw.category || '').includes('HOMETOWN') ||
    raw.title.includes('기부') || raw.title.includes('후원') || raw.title.includes('펀딩');
  return {
    id: Number(id),
    title: raw.title,
    category: isDonation ? 'DONATION' : 'VOLUNTEER',
    location: raw.region || '부산 지역',
    organizer: '픽셀 케어',
    tags: ['AI 추천', isDonation ? '기부' : '봉사'],
  };
}

export const PixelAiMate: React.FC<PixelAiMateProps> = ({ onOpenModal }) => {
  const navigate = useNavigate();
  const [messages, setMessages] = useState<AiChatMessage[]>(INITIAL_MESSAGES);
  const [inputText, setInputText] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [thinkingStep, setThinkingStep] = useState<number>(0);

  // 대화 히스토리 불러오기
  useEffect(() => {
    const loadHistory = async () => {
      try {
        const history = await fetchAiHistory();
        if (history && history.length > 0) {
          const formattedHistory: AiChatMessage[] = history.map((item: any) => {
            let cards: RecommendedCard[] = [];
            if (item.recommendedMissionsJson) {
              try {
                const parsed = JSON.parse(item.recommendedMissionsJson);
                if (Array.isArray(parsed)) {
                  cards = parsed.map(parseCard).filter(Boolean) as RecommendedCard[];
                }
              } catch (_) {}
            }
            return {
              id: item.id.toString(),
              sender: item.sender === 'USER' ? 'USER' : 'AI',
              text: item.message,
              recommendedCards: cards,
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

  /** 카드 ID로 상세 페이지 이동 (봉사 or 커뮤니티) */
  const handleCardNavigate = (cardId: number) => {
    playBeep(520, 0.1);
    if (cardId >= 1000) {
      navigate(`/community/posts/${cardId - 1000}`);
    } else {
      navigate(`/volunteer/${cardId}`);
    }
  };

  const handleSend = async (userQuery?: string) => {
    const textToSend = userQuery || inputText;
    if (!textToSend.trim()) return;

    const userMsg: AiChatMessage = {
      id: Date.now().toString(),
      sender: 'USER',
      text: textToSend,
      recommendedCards: [],
      createdAt: new Date().toLocaleTimeString(),
    };

    setMessages((prev) => [...prev, userMsg]);
    if (!userQuery) setInputText('');
    setIsTyping(true);
    setThinkingStep(1);
    playBeep(440, 0.1);

    // AI 사고(Reasoning) 3단계 visual delay 효과
    const stepTimer1 = setTimeout(() => {
      setThinkingStep(2);
      playBeep(580, 0.08);
    }, 600);

    const stepTimer2 = setTimeout(() => {
      setThinkingStep(3);
      playBeep(720, 0.08);
    }, 1300);

    try {
      const startTime = Date.now();
      const aiResult = await sendAiMessage(textToSend);
      const elapsedTime = Date.now() - startTime;

      // 최소 1.8초 동안은 사고 과정 UI를 시각적으로 보여줌
      if (elapsedTime < 1800) {
        await new Promise((resolve) => setTimeout(resolve, 1800 - elapsedTime));
      }

      // 백엔드에서 내려온 실제 DB 카드 파싱
      const cards: RecommendedCard[] = [];
      if (aiResult.recommendedCards && aiResult.recommendedCards.length > 0) {
        for (const raw of aiResult.recommendedCards) {
          const card = parseCard(raw);
          if (card) cards.push(card);
        }
      }

      const aiMsg: AiChatMessage = {
        id: (Date.now() + 1).toString(),
        sender: 'AI',
        text: aiResult.reply,
        recommendedCards: cards,
        createdAt: new Date().toLocaleTimeString(),
      };

      setMessages((prev) => [...prev, aiMsg]);
      playBeep(880, 0.15);
    } catch (error) {
      console.error('AI Solar LLM 통신 실패:', error);
      const errorMsg: AiChatMessage = {
        id: (Date.now() + 1).toString(),
        sender: 'AI',
        text: '⚠️ AI 서버 통신 중 일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.',
        recommendedCards: [],
        createdAt: new Date().toLocaleTimeString(),
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      clearTimeout(stepTimer1);
      clearTimeout(stepTimer2);
      setIsTyping(false);
      setThinkingStep(0);
    }
  };

  const handleClearHistory = async () => {
    if (!window.confirm('AI 메이트와의 모든 대화 내역을 초기화하시겠습니까?')) return;
    const success = await clearAiHistory();
    if (success) {
      setMessages(INITIAL_MESSAGES);
      playBeep(330, 0.15);
    } else {
      alert('대화 내역 초기화 중 오류가 발생했습니다.');
    }
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
              Upstage Solar LLM · DB 기반 정확 추천
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

      {/* Chat Messages Stream */}
      <div className="ai-chat-body">
        {messages.map((msg) => (
          <div key={msg.id} className={`chat-bubble-row ${msg.sender === 'USER' ? 'user-row' : 'ai-row'}`}>
            {msg.sender === 'AI' && <div className="chat-avatar">🤖</div>}

            <div className="chat-content">
              <div className={`chat-bubble ${msg.sender === 'USER' ? 'user-bubble' : 'ai-bubble'}`}>
                {msg.text.split('\n').map((line, i) => (
                  <React.Fragment key={i}>{line}{i < msg.text.split('\n').length - 1 && <br />}</React.Fragment>
                ))}
              </div>

              {/* DB 매칭 카드 목록 (있을 때만 표시) */}
              {msg.recommendedCards && msg.recommendedCards.length > 0 && (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginTop: '8px' }}>
                  {msg.recommendedCards.map((card) => (
                    <div
                      key={card.id}
                      className="ai-recommended-card"
                      style={{ cursor: 'pointer' }}
                      onClick={() => handleCardNavigate(card.id)}
                    >
                      <div className="card-badge">
                        {card.category === 'DONATION' ? '💝 기부/후원' : '✅ 봉사 활동'} · DB 매칭
                      </div>
                      <h4 style={{ margin: '6px 0 4px', fontSize: '14px', fontWeight: '700' }}>
                        {card.title}
                      </h4>
                      <p style={{ margin: '0 0 8px', fontSize: '12px', color: '#555' }}>
                        📍 {card.location}
                      </p>
                      <div className="card-tags">
                        {card.tags.map((tag) => (
                          <span key={tag} className="tag">{tag}</span>
                        ))}
                      </div>
                      <div style={{ display: 'flex', gap: '8px', marginTop: '10px' }}>
                        <button
                          type="button"
                          className="pixel-button primary"
                          style={{ fontSize: '12px', padding: '6px 14px' }}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleCardNavigate(card.id);
                          }}
                        >
                          👀 상세 보기
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              <span className="chat-timestamp">{msg.createdAt}</span>
            </div>
          </div>
        ))}

        {isTyping && (
          <div className="chat-bubble-row ai-row">
            <div className="chat-avatar">🤖</div>
            <div className="chat-bubble ai-bubble typing-box" style={{ background: 'rgba(255, 255, 255, 0.95)', border: '2px solid #5d4037', borderRadius: '12px', padding: '14px 18px', boxShadow: '0 4px 12px rgba(0,0,0,0.1)' }}>
              <div style={{ fontSize: '12px', fontWeight: 'bold', color: '#ff3b30', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <span className="spinning-pixel">⚙️</span>
                <span>Upstage Solar LLM 사고 과정 (Reasoning...):</span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', fontSize: '13px', color: '#333' }}>
                <div style={{ opacity: thinkingStep >= 1 ? 1 : 0.4, transition: 'all 0.3s ease', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span>{thinkingStep > 1 ? '✅' : '🧠'}</span>
                  <span style={{ fontWeight: thinkingStep === 1 ? 'bold' : 'normal' }}>1단계: 사용자 질의 의도 및 위치/카테고리 키워드 분석</span>
                </div>
                <div style={{ opacity: thinkingStep >= 2 ? 1 : 0.4, transition: 'all 0.3s ease', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span>{thinkingStep > 2 ? '✅' : '🔍'}</span>
                  <span style={{ fontWeight: thinkingStep === 2 ? 'bold' : 'normal' }}>2단계: 픽셀 케어 DB 내 맞춤 봉사·기부 카드 정밀 탐색</span>
                </div>
                <div style={{ opacity: thinkingStep >= 3 ? 1 : 0.4, transition: 'all 0.3s ease', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span>{thinkingStep >= 3 ? '⚡' : '⏳'}</span>
                  <span style={{ fontWeight: thinkingStep === 3 ? 'bold' : 'normal' }}>3단계: 레트로 스타일 최종 추천 답변 및 카드 조합 생성</span>
                </div>
              </div>
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
          placeholder="예: '부산대 근처 봉사' / '금정구 유기견 봉사' / '기부 후원 추천'"
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
