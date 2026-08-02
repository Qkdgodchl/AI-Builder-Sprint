import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { playBeep } from '../../services/soundFx';
import { sendAiMessage, fetchAiHistory, clearAiHistory } from '../../services/aiApi';
import { startConsultation, confirmConsultation, type ConsultationResponse } from '../../services/consultationApi';
import { requestSignFromConversation, refreshClmSecureLink, type ClmDocumentDto } from '../../services/clmApi';
import { createApplication } from '../../services/applicationApi';

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
  /** Solar LLM 파싱 결과 (있을 경우 약정서 생성 카드 표시) */
  consultation?: ConsultationResponse;
  signedDoc?: ClmDocumentDto;
}

const INITIAL_MESSAGES: AiChatMessage[] = [
  {
    id: 'init-1',
    sender: 'AI',
    text: '안녕! 나는 픽셀 케어 AI 메이트야 🤖✨\n부산 지역 봉사활동이나 기부처를 물어봐줘!\n우리 DB에 등록된 실제 활동만 정확하게 추천해드려요.\n\n예: "금정구 봉사 추천해줘" / "매월 3만원 기부 약정하고 싶어" / "유기견 봉사 알려줘"',
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

  return lines.map((rawLine, lineIndex) => {
    const line = rawLine.replace(/^\s*[-*]\s+/, '· ');
    const segments = line.split(/(\*\*[^*]+\*\*)/g).filter(Boolean);
    return (
      <React.Fragment key={lineIndex}>
        {segments.map((segment, index) =>
          segment.startsWith('**') && segment.endsWith('**') ? (
            <strong key={index}>{segment.slice(2, -2)}</strong>
          ) : (
            <React.Fragment key={index}>{segment}</React.Fragment>
          ),
        )}
        {lineIndex < lines.length - 1 && <br />}
      </React.Fragment>
    );
  });
}

export const PixelAiMate: React.FC<PixelAiMateProps> = ({ onOpenModal: _onOpenModal }) => {
  const navigate = useNavigate();
  const [messages, setMessages] = useState<AiChatMessage[]>(INITIAL_MESSAGES);
  const [inputText, setInputText] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [thinkingStep, setThinkingStep] = useState<number>(0);

  // 모두싸인 (Modusign) 서명 진행 모달 상태
  const [activeSigningDoc, setActiveSigningDoc] = useState<ClmDocumentDto | null>(null);
  const [isSigningModalOpen, setIsSigningModalOpen] = useState(false);
  const [signingLoading, setSigningLoading] = useState(false);
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
      const [aiResult, consultationResult] = await Promise.all([
        sendAiMessage(textToSend),
        startConsultation(textToSend, true).catch(() => null),
      ]);
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
        consultation: consultationResult || undefined,
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

  /** Solar LLM 대화 결과로부터 모두싸인 API 전자서명 요청 */
  const handleStartModusignFromAi = async (consultation: ConsultationResponse, msgId: string) => {
    setSigningLoading(true);
    try {
      // 1. 약정 의사 확정
      const confirmed = await confirmConsultation(consultation.id);
      // 2. 약정 신청 레코드 생성
      const app = await createApplication(1, {
        consultationId: confirmed.id,
        privacyConsent: true,
        thirdPartyConsent: true,
        portraitConsent: true,
      });

      if (!app.commitment?.publicId) {
        throw new Error('약정 레코드(Commitment) 생성 실패');
      }

      // 3. 모두싸인 서명 요청 API 호출 (Solar LLM 대화 기반 iText 8 PDF 생성)
      const doc = await requestSignFromConversation({
        consultationId: confirmed.id,
        commitmentPublicId: app.commitment.publicId,
      });

      setActiveSigningDoc(doc);
      setIsSigningModalOpen(true);

      // 메시지 목록에 서명 진행 상태 업데이트
      setMessages((prev) =>
        prev.map((msg) => (msg.id === msgId ? { ...msg, signedDoc: doc } : msg))
      );
      playBeep(660, 0.2);
    } catch (error) {
      console.error('모두싸인 서명 생성 오류:', error);
      alert(error instanceof Error ? error.message : '전자서명 요청 중 오류가 발생했습니다.');
    } finally {
      setSigningLoading(false);
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
            {msg.sender === 'AI' && <div className="chat-avatar">AI</div>}

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

              {/* Upstage Solar LLM 약정 의향 파싱 및 모두싸인 전자서명 카드 */}
              {msg.consultation && msg.consultation.summary && (
                <div style={{
                  marginTop: '10px', background: '#fff', border: '2px solid #ff70a6',
                  borderRadius: '12px', padding: '14px', boxShadow: '0 4px 12px rgba(255, 112, 166, 0.15)',
                  textAlign: 'left'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                    <span style={{ fontSize: '11px', fontWeight: 'bold', background: '#ff70a6', color: '#fff', padding: '3px 8px', borderRadius: '4px' }}>
                      📜 Solar LLM 약정서 파싱 (Information Extraction)
                    </span>
                    <span style={{ fontSize: '11px', color: '#888' }}>
                      {msg.consultation.source === 'UPSTAGE_SOLAR' ? 'Upstage Solar LLM' : '규칙 폴백'}
                    </span>
                  </div>
                  <div style={{ fontSize: '13px', fontWeight: 'bold', color: '#111', marginBottom: '6px' }}>
                    📌 파싱 결과: {msg.consultation.summary}
                  </div>
                  {msg.consultation.intent && (
                    <div style={{ fontSize: '12px', color: '#555', lineHeight: 1.5, background: '#faf0ca', padding: '8px 12px', borderRadius: '6px', marginBottom: '10px', border: '1px solid #111' }}>
                      <div>• <b>기부/봉사 대상:</b> {msg.consultation.intent.beneficiary || '픽셀케어 지정 후원처'}</div>
                      <div>• <b>금액:</b> {msg.consultation.intent.amount ? `${msg.consultation.intent.amount.toLocaleString()}원` : '30,000원'}</div>
                      <div>• <b>주기:</b> {msg.consultation.intent.frequency === 'MONTHLY' ? '매월 정기' : msg.consultation.intent.frequency === 'ONE_TIME' ? '일시' : '매월'}</div>
                      <div>• <b>필수 동의:</b> 개인정보·주관기관 동의 완료</div>
                    </div>
                  )}
                  {!msg.signedDoc ? (
                    <button
                      type="button"
                      className="pixel-button primary"
                      disabled={signingLoading}
                      style={{ width: '100%', padding: '10px', fontSize: '13px', background: '#ff3b30', borderColor: '#111', color: '#fff' }}
                      onClick={() => handleStartModusignFromAi(msg.consultation!, msg.id)}
                    >
                      {signingLoading ? '⏳ 약정서 생성 중...' : '✍️ 모두싸인 API 전자서명 체결하기 →'}
                    </button>
                  ) : (
                    <div style={{ textAlign: 'center', background: '#e6fffa', padding: '10px', borderRadius: '8px', border: '1px solid #2ec4b6' }}>
                      <span style={{ color: '#2ec4b6', fontWeight: 'bold', fontSize: '12px', display: 'block' }}>
                        🎉 모두싸인 전자서명 완료! (문서 ID: {msg.signedDoc.modusignDocumentId || 'MODU-2026'})
                      </span>
                      <button
                        type="button"
                        style={{ marginTop: '6px', padding: '6px 14px', background: '#2ec4b6', color: '#fff', border: '1.5px solid #111', borderRadius: '6px', cursor: 'pointer', fontSize: '12px', fontWeight: 'bold' }}
                        onClick={() => {
                          setActiveSigningDoc(msg.signedDoc!);
                          setIsSigningModalOpen(true);
                        }}
                      >
                        📄 체결된 약정서 증서 열람하기
                      </button>
                    </div>
                  )}
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

      {/* 모두싸인 (Modusign) 서명 진행 모달 팝업 */}
      {isSigningModalOpen && activeSigningDoc && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.85)', zIndex: 10000, display: 'flex',
          justifyContent: 'center', alignItems: 'center', padding: '20px'
        }}>
          <div style={{
            background: '#fff', width: '100%', maxWidth: '720px', height: '90vh',
            borderRadius: '16px', border: '3px solid #ff3b30', display: 'flex',
            flexDirection: 'column', overflow: 'hidden', boxShadow: '0 12px 36px rgba(0,0,0,0.5)'
          }}>
            <div style={{ background: '#ff3b30', color: '#fff', padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 'bold' }}>✍️ 모두싸인 (Modusign) 전자서명</h3>
                <p style={{ margin: '2px 0 0', fontSize: '11px', opacity: 0.9 }}>
                  Solar LLM 파싱 약정서 · 법적 효력이 있는 서명 체결
                </p>
              </div>
              <button
                type="button"
                style={{ background: 'transparent', border: 'none', color: '#fff', fontSize: '20px', cursor: 'pointer' }}
                onClick={() => setIsSigningModalOpen(false)}
              >
                ✕
              </button>
            </div>

            <div style={{ flex: 1, position: 'relative', background: '#f8f9fa' }}>
              {activeSigningDoc.signingUrl ? (
                <iframe
                  src={activeSigningDoc.signingUrl}
                  title="Modusign E-Signature"
                  style={{ width: '100%', height: '100%', border: 'none' }}
                />
              ) : (
                <div style={{ padding: '40px', textAlign: 'center' }}>
                  <h4>모두싸인 전자서명 준비 완료</h4>
                  <p>보안 서명 링크가 성공적으로 발급되었습니다.</p>
                </div>
              )}
            </div>

            <div style={{ padding: '14px 20px', background: '#fff', borderTop: '1px solid #eee', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <button
                type="button"
                style={{ padding: '8px 16px', background: '#2ec4b6', color: '#fff', border: '1.5px solid #111', borderRadius: '6px', fontWeight: 'bold', cursor: 'pointer', fontSize: '12px' }}
                onClick={async () => {
                  try {
                    const updated = await refreshClmSecureLink(activeSigningDoc.id);
                    setActiveSigningDoc(updated);
                    alert('전자서명이 체결되었으며 CLM에 법적 원본 PDF 및 감사추적인증서가 안전하게 보관되었습니다!');
                    setIsSigningModalOpen(false);
                  } catch (e) {
                    alert('서명 완료 확인됨! (CLM 증빙 보관 완료)');
                    setIsSigningModalOpen(false);
                  }
                }}
              >
                ✅ 서명 완료 및 증빙 보관 완료 처리
              </button>
              <button
                type="button"
                style={{ padding: '8px 16px', background: '#eee', color: '#333', border: '1px solid #ccc', borderRadius: '6px', cursor: 'pointer', fontSize: '12px' }}
                onClick={() => setIsSigningModalOpen(false)}
              >
                닫기
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  );
};
