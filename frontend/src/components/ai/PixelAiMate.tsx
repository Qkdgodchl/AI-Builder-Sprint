import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { playBeep } from '../../services/soundFx';
import { sendAiMessage, fetchAiHistory, clearAiHistory } from '../../services/aiApi';
import { startConsultation, confirmConsultation, type ConsultationResponse } from '../../services/consultationApi';
import { requestSignFromConversation, refreshClmSecureLink, type ClmDocumentDto } from '../../services/clmApi';
import { createApplication } from '../../services/applicationApi';
import { Logo } from '../common/Logo';

interface PixelAiMateProps {
  onOpenModal: (title: string, type: 'volunteer' | 'donate') => void;
  /** 도크 안에서 열렸을 때, 다른 화면으로 넘어가면 도크를 닫아 준다. */
  onNavigateAway?: () => void;
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
  /** 등록된 프로그램을 못 찾은 답변. CONNECT로 넘길 수 있게 원문을 들고 있는다. */
  unmatchedQuery?: string;
  /** Solar LLM 파싱 결과 (있을 경우 약정서 생성 카드 표시) */
  consultation?: ConsultationResponse;
  signedDoc?: ClmDocumentDto;
}

const INITIAL_MESSAGES: AiChatMessage[] = [
  {
    id: 'init-1',
    sender: 'AI',
    text: '안녕하세요! 저는 잇다 AI 메이트예요\n부산 지역 봉사활동이나 기부처를 물어봐 주세요.\n\n예: "금정구 봉사 추천해줘" / "매월 3만원 기부 약정하고 싶어" / "유기견 봉사 알려줘"',
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
    organizer: '잇다',
    tags: ['AI 추천', isDonation ? '기부' : '봉사'],
  };
}

/**
 * LLM 답변에 섞여 오는 마크다운을 화면용으로 정리한다.
 * `**굵게**`는 실제 굵은 글씨로, 줄머리 `- `는 가운뎃점으로 바꿔
 * 별표가 그대로 노출되지 않게 한다.
 */
function renderMessageText(text: string) {
  const lines = text
    .replace(/<br\s*\/?>/gi, '\n')
    .split('\n')
    // 표 구분선(|---|)은 화면에서 의미가 없으므로 버린다.
    .filter((line) => !/^\s*\|?[\s|:-]{3,}\|?\s*$/.test(line))
    .map((line) =>
      line
        .replace(/^\s*\|\s?/, '')
        .replace(/\s?\|\s*$/, '')
        .replace(/\s*\|\s*/g, ' · '),
    );

  // 줄바꿈만으로 이어 붙이면 문장이 빽빽하게 붙어 읽히지 않는다.
  // 한 줄을 한 문단으로 세워 사이를 띄우고, 목록 항목끼리는 더 가깝게 둔다.
  return lines
    .map((line) => line.trim())
    .filter(Boolean)
    .map((rawLine, lineIndex) => {
      const isBullet = /^\s*[-*·]\s+/.test(rawLine);
      const line = rawLine.replace(/^\s*[-*]\s+/, '· ');
      const segments = line.split(/(\*\*[^*]+\*\*)/g).filter(Boolean);
      return (
        <p key={lineIndex} className={`chat-line${isBullet ? ' is-bullet' : ''}`}>
          {segments.map((segment, index) =>
            segment.startsWith('**') && segment.endsWith('**') ? (
              <strong key={index}>{segment.slice(2, -2)}</strong>
            ) : (
              <React.Fragment key={index}>{segment}</React.Fragment>
            ),
          )}
        </p>
      );
    });
}

export const PixelAiMate: React.FC<PixelAiMateProps> = ({
  onOpenModal: _onOpenModal,
  onNavigateAway,
}) => {
  const navigate = useNavigate();
  const [messages, setMessages] = useState<AiChatMessage[]>(INITIAL_MESSAGES);
  const [inputText, setInputText] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [thinkingStep, setThinkingStep] = useState<number>(0);

  const chatEndRef = React.useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isTyping, thinkingStep]);

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

  /**
   * 못 찾은 요청을 CONNECT로 옮긴다.
   * 사용자가 한 말을 그대로 초안으로 넘겨 다시 쓰지 않게 한다.
   */
  const goConnect = (query: string) => {
    playBeep(520, 0.1);
    // 도크가 화면을 덮고 있으면 이동해도 아무 일 없는 것처럼 보인다. 먼저 닫는다.
    onNavigateAway?.();
    navigate('/connect', {
      state: {
        connectDraft: {
          title: query.length > 110 ? `${query.slice(0, 110)}…` : query,
          content: query,
          category: /기부|후원|모금|성금/.test(query) ? 'DONATION' : 'VOLUNTEER',
        },
      },
    });
  };

  /** 카드 ID로 상세 페이지 이동 (봉사 or 커뮤니티) */
  const handleCardNavigate = (cardId: number) => {
    playBeep(520, 0.1);
    onNavigateAway?.();
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
      // 약정 파싱은 신청 화면에서 다시 하므로 여기서 부르지 않는다.
      // 화면에 쓰지도 않으면서 응답을 기다리고 상담 기록만 쌓였다.
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
        // 맞는 프로그램이 없으면 그 말을 그대로 들고 있다가 CONNECT로 넘긴다.
        unmatchedQuery: cards.length === 0 ? textToSend : undefined,
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
        <div className="ai-header-identity">
          <h2>ITDA AI MATE</h2>
          <p>Upstage Solar LLM · 등록된 프로그램만 추천</p>
        </div>

        <div className="ai-header-actions">
          <span className="online-badge">응답 가능</span>
          <button type="button" className="ai-reset-button" onClick={handleClearHistory}>
            대화 초기화
          </button>
        </div>
      </div>

      {/* Chat Messages Stream */}
      <div className="ai-chat-body">
        {messages.map((msg) => (
          <div key={msg.id} className={`chat-bubble-row ${msg.sender === 'USER' ? 'user-row' : 'ai-row'}`}>
            {msg.sender === 'AI' && <Logo variant="mark" className="chat-avatar" />}

            <div className="chat-content">
              <div className={`chat-bubble ${msg.sender === 'USER' ? 'user-bubble' : 'ai-bubble'}`}>
                {renderMessageText(msg.text)}
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

              {/*
                약정 파싱 결과와 서명 버튼은 여기에 두지 않는다.
                서명은 프로그램을 고른 뒤 신청 화면에서 단계를 밟아 진행하는 흐름이고,
                도크에서 곧바로 체결 버튼이 뜨면 무엇에 서명하는지 알 수 없다.
              */}

              {/*
                찾는 활동이 등록되어 있지 않을 때, 그냥 "없다"로 끝내지 않고
                그 말을 CONNECT로 옮겨 센터가 새 프로그램을 열도록 잇는다.
              */}
              {msg.unmatchedQuery && (
                <div className="ai-connect-suggest">
                  <strong>아직 등록된 프로그램이 없어요</strong>
                  <p>
                    말씀하신 활동을 CONNECT에 남기면 관련 센터가 보고
                    프로그램 개설을 검토합니다.
                  </p>
                  <button
                    type="button"
                    onClick={() => goConnect(msg.unmatchedQuery!)}
                  >
                    CONNECT에 요청 남기기 →
                  </button>
                </div>
              )}

              <span className="chat-timestamp">{msg.createdAt}</span>
            </div>
          </div>
        ))}

        {isTyping && (
          <div className="chat-bubble-row ai-row">
            <Logo variant="mark" className="chat-avatar" />
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
                  <span style={{ fontWeight: thinkingStep === 2 ? 'bold' : 'normal' }}>2단계: 잇다 DB 내 맞춤 봉사·기부 카드 정밀 탐색</span>
                </div>
                <div style={{ opacity: thinkingStep >= 3 ? 1 : 0.4, transition: 'all 0.3s ease', display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span>{thinkingStep >= 3 ? '⚡' : '⏳'}</span>
                  <span style={{ fontWeight: thinkingStep === 3 ? 'bold' : 'normal' }}>3단계: 레트로 스타일 최종 추천 답변 및 카드 조합 생성</span>
                </div>
              </div>
            </div>
          </div>
        )}
        <div ref={chatEndRef} />
      </div>

      {/* Quick Prompt Suggestion Chips */}
      <div style={{
        padding: '8px 16px', display: 'flex', gap: '8px', overflowX: 'auto',
        background: '#faf8f5', borderTop: '1px dashed #d8c3b0', whiteSpace: 'nowrap'
      }}>
        <button
          type="button"
          className="pixel-button"
          style={{ fontSize: '11px', padding: '4px 10px', background: '#e6f4ff', color: '#0958d9', borderColor: '#91caff', cursor: 'pointer', flexShrink: 0 }}
          onClick={() => handleSend('부산 고향사랑기부 매월 3만원 약정하고 싶어. 동백전 답례품으로 원해.')}
        >
          🏠 부산 고향사랑기부 (동백전 답례품)
        </button>
        <button
          type="button"
          className="pixel-button"
          style={{ fontSize: '11px', padding: '4px 10px', background: '#f6ffed', color: '#389e0d', borderColor: '#b7eb8f', cursor: 'pointer', flexShrink: 0 }}
          onClick={() => handleSend('범어사 삼층석탑 보존을 위해 유산기부를 상담하고 싶어.')}
        >
          🏛️ 범어사 삼층석탑 유산기부 약정
        </button>
        <button
          type="button"
          className="pixel-button"
          style={{ fontSize: '11px', padding: '4px 10px', background: '#fff7e6', color: '#d46b08', borderColor: '#ffd591', cursor: 'pointer', flexShrink: 0 }}
          onClick={() => handleSend('금정구 독거어르신 도시락 봉사 활동에 참여하고 싶어.')}
        >
          🍱 금정구 도시락 봉사 약정
        </button>
        <button
          type="button"
          className="pixel-button"
          style={{ fontSize: '11px', padding: '4px 10px', background: '#fff0f6', color: '#c41d7f', borderColor: '#ffadd2', cursor: 'pointer', flexShrink: 0 }}
          onClick={() => handleSend('매월 3만원 정기후원 신청 시 세액공제 혜택과 절차가 어떻게 되나요?')}
        >
          💝 정기후원 세액공제 문의
        </button>
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
          placeholder="예: '부산대 근처 봉사' / '매월 3만원 기부 약정하고 싶어' / '고향사랑기부 답례품'"
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
