import React, { useEffect, useRef, useState } from 'react';
import type { SessionUser, VolunteerItem } from '../../types';
import {
  createApplication,
  submitCommitment,
} from '../../services/applicationApi';
import { requestClmSign, requestSignFromConversation, fetchClmDocument, fetchClmDocumentFiles, refreshClmSecureLink } from '../../services/clmApi';
import type { ClmDocumentDto, ClmDocumentFileDto } from '../../services/clmApi';
import {
  confirmConsultation,
  startConsultation,
  sendConsultationMessage,
  updateConsultationIntent,
} from '../../services/consultationApi';
import type { ConsultationResponse, PledgeIntent } from '../../services/consultationApi';
import { fetchMyProfile } from '../../services/authApi';
import { Logo } from '../common/Logo';

interface ChatMessage {
  role: 'ai' | 'user';
  content: string;
  timestamp?: Date;
}

/** 서명 링크 발급이 막히면 서버가 로컬 대체 주소를 내려주므로, 진짜 서명창인지 가린다. */
const isModusignUrl = (url: string) => /(^|\.)modusign\.co\.kr/.test(new URL(url, window.location.origin).hostname);

/**
 * Solar 응답과 안내 문구에 **강조** 표기가 섞여 온다.
 * 말풍선에 별표가 그대로 보이지 않도록 굵은 글씨로 바꿔 준다.
 */
const renderChatText = (text: string) =>
  text.split(/\*\*(.+?)\*\*/g).map((part, index) =>
    index % 2 === 1 ? <strong key={index}>{part}</strong> : part,
  );

interface ApplicationItem extends VolunteerItem {
  programType: string;
  availability: string;
}

interface ClmApplicationPreparationProps {
  item: ApplicationItem;
  typeLabel: string;
  currentUser: SessionUser | null;
  onBack: () => void;
}

export const ClmApplicationPreparation: React.FC<ClmApplicationPreparationProps> = ({
  item,
  typeLabel,
  currentUser,
  onBack,
}) => {
  const isVolunteer = item.category === 'VOLUNTEER';
  const isHometown = item.programType === 'HOMETOWN' || item.category === 'HOMETOWN';
  const isLegacy = item.programType === 'LEGACY' || item.category === 'LEGACY';
  const documentName = isVolunteer ? '봉사 참여 약정서 (제2026-ITDA-01호)' : '후원 및 기부 약정서 (제2026-ITDA-02호)';

  const [specialConditions, setSpecialConditions] = useState('');
  const [privacyConsent, setPrivacyConsent] = useState(false);
  const [thirdPartyConsent, setThirdPartyConsent] = useState(false);
  const [portraitConsent, setPortraitConsent] = useState(false);
  const [completed, setCompleted] = useState(false);
  const [aiFeedback, setAiFeedback] = useState('');
  const [consultation, setConsultation] = useState<ConsultationResponse | null>(null);
  const [intent, setIntent] = useState<PledgeIntent | null>(null);
  const [aiConfirmed, setAiConfirmed] = useState(false);
  const [externalAiConsent] = useState(true);
  const [commitmentPublicId, setCommitmentPublicId] = useState<string | null>(null);

  // 대화형 채팅 상태
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [chatInput, setChatInput] = useState('');
  const [chatSending, setChatSending] = useState(false);
  const chatEndRef = useRef<HTMLDivElement>(null);
  const chatInputRef = useRef<HTMLTextAreaElement>(null);

  const [activeStep, setActiveStep] = useState<1 | 2 | 3 | 4>(1);
  const [applicantName, setApplicantName] = useState(currentUser?.nickname || '');
  const [applicantEmail, setApplicantEmail] = useState(currentUser?.email || '');
  const [clmDoc, setClmDoc] = useState<ClmDocumentDto | null>(null);
  const [docFiles, setDocFiles] = useState<ClmDocumentFileDto[]>([]);
  const [isSigningModalOpen, setIsSigningModalOpen] = useState(false);
  const [isDocViewModalOpen, setIsDocViewModalOpen] = useState(false);
  const [isSigned, setIsSigned] = useState(false);
  const [requestingSign, setRequestingSign] = useState(false);
  const [checkingSignature, setCheckingSignature] = useState(false);
  const [signatureStatusMessage, setSignatureStatusMessage] = useState('');

  useEffect(() => {
    if (!currentUser) {
      setApplicantName('');
      setApplicantEmail('');
      return;
    }

    setApplicantName(currentUser.nickname);
    setApplicantEmail(currentUser.email);

    let active = true;
    fetchMyProfile()
      .then((profile) => {
        if (!active) return;
        setApplicantName(profile.name?.trim() || profile.nickname || currentUser.nickname);
        setApplicantEmail(profile.email || currentUser.email);
      })
      .catch((error) => {
        console.error('서명자 프로필 조회 실패:', error);
      });

    return () => {
      active = false;
    };
  }, [currentUser]);

  // 컴포넌트 마운트 시 AI 첫 인사 메시지
  useEffect(() => {
    const greeting = isVolunteer
      ? `안녕하세요! 저는 잇다 AI 상담 매니저예요 🙌\n\n**[${item.title}]** (${item.location} / 주관: ${item.organizer}) 봉사 프로그램 신청을 도와드릴게요.\n\n어떤 주기로 봉사에 참여하고 싶으신가요? (예: 주말 매주, 격주, 하루 일시 참여 등) 😊`
      : isHometown
      ? `안녕하세요! 저는 잇다 AI 기부 상담사예요 🏡\n\n**[${item.title}]** (${item.location} / ${item.organizer}) 고향사랑기부에 관심 가져주셔서 감사해요!\n\n10만원 이하 기부 시 100% 전액 세액공제 환급과 30% 답례품(지역 화폐/특산품) 혜택이 지원돼요. 기부 납부 주기는 어떻게 생각하고 계신가요? (예: 일시 기부, 매월 정기 후원 등) 🌟`
      : isLegacy
      ? `안녕하세요! 저는 잇다 AI 유산기부 상담사예요 📜\n\n**[${item.title}]** (${item.location} / ${item.organizer}) 유산기부 약정 안내를 도와드릴게요.\n\n약정 주기나 절차 중 궁금하신 점이나 희망하시는 방식이 있으신가요? 🤝`
      : `안녕하세요! 저는 잇다 AI 기부 상담사예요 💝\n\n**[${item.title}]** (${item.location} / ${item.organizer}) 기부 신청을 선택해 주셨네요!\n\n기부 납부 주기는 어떻게 생각하고 계신가요? (예: 일시 기부, 매월 정기 후원, 분기별 후원 등)`;
    setChatMessages([{ role: 'ai', content: greeting, timestamp: new Date() }]);
  }, []);

  // 모두싸인 iframe 서명 완료 postMessage 감지
  useEffect(() => {
    const handleMessage = (event: MessageEvent) => {
      const dataStr = typeof event.data === 'string' ? event.data : JSON.stringify(event.data || {});
      if (dataStr.includes('signed') || dataStr.includes('MODUSIGN_SUCCESS') || event.data?.event === 'document_signed') {
        setIsSigned(true);
        setCompleted(true);
        setIsSigningModalOpen(false);
      }
    };
    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, []);

  // 채팅 스크롤 자동 하단
  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessages, chatSending]);

  // 채팅 메시지 전송 핸들러
  const handleChatSend = async (overrideText?: string) => {
    const text = (overrideText ?? chatInput).trim();
    if (!text || chatSending) return;
    setChatInput('');
    setChatSending(true);
    setAiFeedback('');

    const userMsg: ChatMessage = { role: 'user', content: text, timestamp: new Date() };
    setChatMessages((prev) => [...prev, userMsg]);

    try {
      let result: ConsultationResponse;
      if (!consultation) {
        const contextPrompt = `[선택 프로그램: ${item.title}, 지역: ${item.location}, 주관기관: ${item.organizer}] ${text}`;
        result = await startConsultation(contextPrompt, externalAiConsent);
      } else {
        result = await sendConsultationMessage(consultation.id, text, externalAiConsent);
      }

      const enriched: PledgeIntent = {
        ...result.intent,
        pledgeType: result.intent.pledgeType || (isHometown ? 'HOMETOWN_DONATION' : isVolunteer ? 'VOLUNTEER' : 'DONATION'),
        beneficiary: result.intent.beneficiary || item.organizer,
        region: result.intent.region || item.location,
        frequency: result.intent.frequency || (isVolunteer ? 'NOT_APPLICABLE' : isHometown ? 'MONTHLY' : 'ONE_TIME'),
      };
      setConsultation(result);
      setIntent(enriched);

      if (result.assistantMessage) {
        setChatMessages((prev) => [...prev, { role: 'ai', content: result.assistantMessage, timestamp: new Date() }]);
      }
    } catch (error) {
      setAiFeedback(error instanceof Error ? error.message : 'AI 응답을 받지 못했습니다.');
      setChatMessages((prev) => [
        ...prev,
        { role: 'ai', content: '⚠️ 잠시 연결이 원활하지 않아요. 다시 시도해 주세요!', timestamp: new Date() },
      ]);
    } finally {
      setChatSending(false);
      setTimeout(() => chatInputRef.current?.focus(), 100);
    }
  };

  const handleOpenDocView = async () => {
    setIsDocViewModalOpen(true);
    if (clmDoc) {
      try {
        const files = await fetchClmDocumentFiles(clmDoc.id);
        setDocFiles(files);
      } catch (e) {
        console.error('서명 완료 PDF 문서 파일 조회 실패:', e);
      }
    }
  };

  const canStartSigning = aiConfirmed && privacyConsent && thirdPartyConsent;

  const handleConfirmIntent = async () => {
    if (!consultation || !intent) return;
    setAiFeedback('');
    try {
      const updated = await updateConsultationIntent(consultation.id, intent);
      const confirmedIntent = await confirmConsultation(updated.id);
      setConsultation(confirmedIntent);
      setIntent(confirmedIntent.intent);
      setSpecialConditions(confirmedIntent.intent.specialConditions || specialConditions);
      setAiConfirmed(true);
    } catch (error) {
      setAiFeedback(error instanceof Error ? error.message : '약정 의사를 확정하지 못했습니다.');
    }
  };

  const handleStartModusign = async () => {
    if (!aiConfirmed || !consultation) {
      alert('AI가 정리한 약정 의사를 먼저 확인·확정해 주세요.');
      return;
    }
    if (!canStartSigning) {
      alert('필수 동의 항목을 먼저 동의해 주세요.');
      return;
    }

    setRequestingSign(true);
    try {
      let activeCommitmentId = commitmentPublicId;
      if (!activeCommitmentId) {
        const application = await createApplication(item.id, {
          consultationId: consultation.id,
          specialConditions,
          privacyConsent,
          thirdPartyConsent,
          portraitConsent,
        });
        activeCommitmentId = application.commitment?.publicId || null;
        if (!activeCommitmentId) throw new Error('생성된 약정서 식별자를 확인할 수 없습니다.');
        try {
          await submitCommitment(activeCommitmentId);
        } catch (_) {}
        setCommitmentPublicId(activeCommitmentId);
      }

      const doc = consultation
        ? await requestSignFromConversation({
            consultationId: consultation.id,
            commitmentPublicId: activeCommitmentId,
          })
        : await requestClmSign({
            commitmentPublicId: activeCommitmentId,
          });

      // 방금 발급된 링크이므로 그대로 연다.
      // 여기서 또 재발급하면 서명 한 번에 발급 요청이 두 번 나가 레이트 리밋에 걸린다.
      setClmDoc(doc);
      setSignatureStatusMessage('');
      setIsSigningModalOpen(true);
    } catch (err) {
      console.error(err);
      alert(err instanceof Error ? err.message : '서명 요청 처리 중 오류가 발생했습니다.');
    } finally {
      setRequestingSign(false);
    }
  };

  /**
   * 서명이 끝나도 모두싸인 쪽에서 우리 화면으로 되돌려 보내주지 못한다.
   * 되돌아오려면 공개 https 주소가 필요한데 개발 환경은 localhost라서다.
   * 그래서 우리가 연 창을 직접 들고 있다가 서명이 확인되면 닫아 준다.
   */
  const signingWindowRef = useRef<Window | null>(null);

  const launchSigningWindow = () => {
    if (!clmDoc?.signingUrl) return;
    signingWindowRef.current = window.open(clmDoc.signingUrl, 'modusign-signing');
  };

  const closeSigningWindow = () => {
    try {
      signingWindowRef.current?.close();
    } catch (_) {
      // 사용자가 이미 닫았거나 브라우저가 막으면 그대로 둔다.
    }
    signingWindowRef.current = null;
  };

  /** 서명 링크는 10분이면 만료되므로 다시 열 때는 새로 발급받는다. */
  const openSigningWindow = async () => {
    if (!clmDoc) return;
    setSignatureStatusMessage('');
    try {
      const fresh = await refreshClmSecureLink(clmDoc.id);
      setClmDoc(fresh);
    } catch (err) {
      console.error(err);
      setSignatureStatusMessage('서명 링크를 새로 발급하지 못했습니다. 잠시 후 다시 시도해 주세요.');
    }
    setIsSigningModalOpen(true);
  };

  // 서명창이 열려 있는 동안만 완료 여부를 확인한다.
  const signingDocId = isSigningModalOpen && !isSigned ? clmDoc?.id ?? null : null;
  const lastCheckedAtRef = useRef(0);

  useEffect(() => {
    if (!signingDocId) return;
    const startedAt = Date.now();
    let stopped = false;

    // 모두싸인 API에 호출 제한이 있어 최소 간격을 둔다.
    const checkSigned = async () => {
      if (stopped || Date.now() - lastCheckedAtRef.current < 2500) return;
      lastCheckedAtRef.current = Date.now();
      try {
        const fresh = await fetchClmDocument(signingDocId);
        if (stopped || fresh.status !== 'SIGNED') return;
        stopped = true;
        setClmDoc(fresh);
        setIsSigned(true);
        setCompleted(true);
        setIsSigningModalOpen(false);
        closeSigningWindow();
        setSignatureStatusMessage('전자서명이 완료되어 약정서가 안전하게 보관되었습니다.');
      } catch (_) {
        // 한 번 실패해도 다음 신호에 다시 확인한다.
      }
    };

    /*
     * 주기적 확인만 두면 서명을 마쳐도 다음 차례까지 기다려야 한다.
     * 서명을 끝낸 사람은 창을 닫거나 이 화면으로 돌아오므로,
     * 그 두 순간을 신호로 삼아 바로 확인한다. 창이 닫혔는지 보는 건 통신이 아니라 공짜다.
     */
    const watchWindow = window.setInterval(() => {
      if (signingWindowRef.current?.closed) {
        signingWindowRef.current = null;
        void checkSigned();
      }
    }, 700);

    const onFocus = () => void checkSigned();
    window.addEventListener('focus', onFocus);
    document.addEventListener('visibilitychange', onFocus);

    // 아무 신호가 없어도 놓치지 않도록 받쳐 주는 주기 확인. 3분이면 멈춘다.
    const poll = window.setInterval(() => {
      if (Date.now() - startedAt > 3 * 60 * 1000) {
        window.clearInterval(poll);
        return;
      }
      void checkSigned();
    }, 6000);

    return () => {
      stopped = true;
      window.clearInterval(watchWindow);
      window.clearInterval(poll);
      window.removeEventListener('focus', onFocus);
      document.removeEventListener('visibilitychange', onFocus);
    };
  }, [signingDocId]);

  const handleCheckSignature = async () => {
    if (!clmDoc) return;
    setCheckingSignature(true);
    setSignatureStatusMessage('');
    try {
      const updated = await fetchClmDocument(clmDoc.id);
      setClmDoc(updated);
      if (updated.status === 'SIGNED') {
        setIsSigned(true);
        setCompleted(true);
        setIsSigningModalOpen(false);
        closeSigningWindow();
        alert('모두싸인 전자서명 완료가 확인되었습니다.');
      } else {
        setSignatureStatusMessage('아직 서명이 완료되지 않았습니다. 서명을 마친 뒤 다시 확인해 주세요.');
      }
    } catch (err) {
      console.error(err);
      setSignatureStatusMessage('서명 상태 확인 실패');
    } finally {
      setCheckingSignature(false);
    }
  };

  return (
    <article className="clm-application">
      <div className="clm-back-nav">
        <button type="button" onClick={onBack}>
          프로그램 상세로 돌아가기
        </button>
        <span>CLM APPLICATION (MODUSIGN VERIFIED)</span>
      </div>

      <header className="clm-header">
        <p>
          {isHometown
            ? '부산 고향사랑기부(busanlove.kr) 연계 기부 약정 시스템'
            : isVolunteer
            ? '잇다 봉사 활동 참여 및 약정 시스템'
            : isLegacy
            ? '잇다 유산 및 지정 기부 약정 시스템'
            : '잇다 후원 및 기부 약정 시스템'}
        </p>
        <h2>신청 서류 작성 & 전자서명</h2>
        <span>
          신청 정보를 바탕으로 약정서를 작성하고 모두싸인 전자서명을 완료하면 최종 신청 및 서류가 안전하게 보존됩니다.
        </span>
      </header>

      {/* 프로그램 유형별 동적 스테퍼 헤더 */}
      <ol
        className={`clm-steps${isHometown ? '' : ' clm-steps--three'}`}
        aria-label="신청 진행 단계"
      >
        {(isHometown
          ? [
              { step: 1, title: '01. 기부 약정', sub: 'AI 대화 정리' },
              { step: 2, title: '02. 답례품 선택', sub: '부산 특산품/동백전' },
              { step: 3, title: '03. 세액공제·동의', sub: '영수증 및 개인정보' },
              { step: 4, title: '04. 전자서명 증빙', sub: '모두싸인 API' },
            ]
          : isVolunteer
          ? [
              { step: 1, title: '01. 봉사 신청', sub: 'AI 일시/의사 정리' },
              { step: 3, title: '02. 참여 서약·동의', sub: '개인정보 및 서약' },
              { step: 4, title: '03. 전자서명 증빙', sub: '모두싸인 API' },
            ]
          : [
              { step: 1, title: '01. 기부 약정', sub: 'AI 대화 정리' },
              { step: 3, title: '02. 약정 동의', sub: '필수 개인정보 동의' },
              { step: 4, title: '03. 전자서명 증빙', sub: '모두싸인 API' },
            ]
        ).map((s) => {
          const isActive = activeStep === s.step;
          const isDone = activeStep > s.step || (s.step === 1 && aiConfirmed) || (s.step === 3 && privacyConsent && thirdPartyConsent) || (s.step === 4 && isSigned);
          return (
            <li
              key={s.step}
              className={isActive ? 'current' : isDone ? 'complete' : ''}
              onClick={() => setActiveStep(s.step as 1 | 2 | 3 | 4)}
            >
              <strong>{s.title}</strong>
              <span>{s.sub}</span>
            </li>
          );
        })}
      </ol>

      <div className="clm-layout">
        <div className="clm-main">
          {/* 프로그램 요약 정보 */}
          <section className="clm-program-summary">
            <div>
              <span>신청 프로그램</span>
              <strong>{item.title}</strong>
            </div>
            <dl className="clm-program-meta">
              <div><dt>구분</dt><dd>{typeLabel}</dd></div>
              <div><dt>주관기관</dt><dd>{item.organizer}</dd></div>
              <div><dt>지역</dt><dd>{item.location}</dd></div>
            </dl>
          </section>

          {/* ================= STEP 01: 기부 약정 (AI 상담 & 의사 정리) ================= */}
          {activeStep === 1 && (
            <section className="clm-document-section">
              <div className="clm-section-heading">
                <span className="clm-step-badge">STEP 01</span>
                <h3>기부 약정 정리</h3>
                <p>AI와 대화를 나눠 기부금액, 약정 주기 및 기부 목적을 정리해 주세요.</p>
              </div>

              {/* 채팅창 컨테이너 */}
              <div className="clm-chat">
                <div className="clm-chat-bar">
                  <Logo variant="mark" className="clm-chat-avatar" />
                  <div>
                    <strong>잇다 AI 약정 매니저</strong>
                    <span>{chatSending ? '생각하는 중…' : 'UPSTAGE SOLAR LLM · 실시간 의사 분석'}</span>
                  </div>
                </div>

                <div className="clm-chat-log">
                  {chatMessages.map((msg, idx) => (
                    <div
                      key={idx}
                      className={`clm-chat-row${msg.role === 'user' ? ' is-user' : ''}`}
                    >
                      <div className={`chat-bubble ${msg.role === 'user' ? 'user-bubble' : 'ai-bubble'}`}>
                        {renderChatText(msg.content)}
                      </div>
                    </div>
                  ))}
                  <div ref={chatEndRef} />
                </div>

                {/* 퀵 칩 입력 혜택 */}
                {!aiConfirmed && (
                  <div className="clm-chat-chips">
                    {(isVolunteer
                      ? ['주말 매주 참여해요', '격주 봉사 원해요', '하루 일시 참여']
                      : ['매월 3만원 기부할게요', '일시 10만원 기부할게요', '매년 100만원 기부', '답례품 미수령']
                    ).map((chip) => (
                      <button key={chip} type="button" disabled={chatSending} onClick={() => handleChatSend(chip)}>{chip}</button>
                    ))}
                  </div>
                )}

                {!aiConfirmed && (
                  <div className="clm-chat-input">
                    <textarea
                      ref={chatInputRef}
                      value={chatInput}
                      onChange={(e) => setChatInput(e.target.value)}
                      onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleChatSend(); } }}
                      placeholder="약정 관련 답변을 입력하세요 (예: 매월 3만원 부산 영도구 기부할게요)"
                      rows={1}
                      disabled={chatSending}
                    />
                    <button
                      type="button"
                      className="clm-chat-send"
                      onClick={() => handleChatSend()}
                      disabled={!chatInput.trim() || chatSending}
                      aria-label="보내기"
                    >→</button>
                  </div>
                )}
              </div>

              {/* AI 정리 의향 카드 */}
              {intent && (
                <div className="clm-intent-card">
                  <div className="clm-intent-title">
                    <span>AI SUMMARY</span>
                    AI가 정리한 약정 내역 · 직접 고칠 수 있어요
                  </div>
                  <div className="clm-intent-grid">
                    <label>
                      약정 유형
                      <select value={intent.pledgeType || ''} onChange={(e) => setIntent({ ...intent, pledgeType: e.target.value })}>
                        <option value="DONATION">일반 기부</option>
                        <option value="HOMETOWN_DONATION">고향사랑기부</option>
                        <option value="VOLUNTEER">봉사 참여</option>
                        <option value="LEGACY_DONATION">유산 기부</option>
                      </select>
                    </label>
                    <label>
                      주기 (납부/참여)
                      <select value={intent.frequency || ''} onChange={(e) => setIntent({ ...intent, frequency: e.target.value })}>
                        <option value="ONE_TIME">일시 (1회성)</option>
                        <option value="WEEKLY">매주 (주간)</option>
                        <option value="MONTHLY">매월 (월간)</option>
                        <option value="ANNUAL">매년 (연간)</option>
                      </select>
                    </label>
                    <label>
                      수혜 대상·기관
                      <input value={intent.beneficiary || ''} onChange={(e) => setIntent({ ...intent, beneficiary: e.target.value })} />
                    </label>
                    <label>
                      지역
                      <input value={intent.region || ''} onChange={(e) => setIntent({ ...intent, region: e.target.value })} />
                    </label>
                  </div>
                  <button
                    type="button"
                    onClick={async () => {
                      if (!intent) return;
                      await handleConfirmIntent();
                      setActiveStep(isHometown ? 2 : 3);
                    }}
                    className="clm-primary-action"
                  >
                    {isHometown
                      ? '약정 의사 확정 → STEP 02. 답례품 선택하기'
                      : '약정 의사 확정 → STEP 02. 필수 동의 진행하기'}
                  </button>
                </div>
              )}
              {aiFeedback && <div className="clm-inline-error">{aiFeedback}</div>}
            </section>
          )}

          {/* ================= STEP 02: 답례품 선택 (isHometown 전용) ================= */}
          {activeStep === 2 && isHometown && (
            <section className="clm-document-section">
              <div className="clm-section-heading">
                <span className="clm-step-badge">STEP 02</span>
                <h3>답례품 선택</h3>
                <p>부산 고향사랑e음(busanlove.kr) 연계 · 기부금의 30% 한도 내에서 답례품을 고를 수 있습니다.</p>
              </div>

              <div className="clm-point-bar">
                <div>
                  <strong>고향사랑기부 산정 포인트</strong>
                  <span>소득세법 10만원 100% 세액공제 + 30% 답례품 포인트 환급</span>
                </div>
                <em>
                  보유 {intent?.amount ? Math.floor(intent.amount * 0.3).toLocaleString() : '30,000'} P
                </em>
              </div>

              <div className="clm-gift-grid">
                {[
                  { id: 'dongbaek', name: '💳 부산 동백전 지역화폐 30% 포인트', pts: '기부액 30%', desc: '부산 전역 가맹점 현금처럼 즉시 사용' },
                  { id: 'gijang', name: '🐟 부산 기장 명품 미역·다시마 세트', pts: '30,000 P', desc: '임금님 수라상 청정 기장 해풍 미역' },
                  { id: 'hanwoo', name: '🥩 부산 명품 한우 수제 육포 세트', pts: '30,000 P', desc: '100% 국산 한우 프리미엄 영양 간식' },
                  { id: 'eomuk', name: '🥮 부산 영도 고구마 앙금빵 & 어묵', pts: '30,000 P', desc: '조내기 고구마 빵 & 수제 어묵 명품 세트' },
                  { id: 'coffee', name: '☕ 부산 영도 모모스 스페셜티 원두', pts: '30,000 P', desc: '월드 바리스타 챔피언십 우승 영도 커피' },
                  { id: 'none', name: '💚 답례품 수령 안 함 (전액 기탁)', pts: '0 P', desc: '답례품 없이 영도구 발전 기금으로 전액 기탁' },
                ].map((gift) => {
                  const isSelected = (intent?.giftItem || '').includes(gift.name.replace(/^[^\s]+\s/, '')) ||
                    (gift.id === 'none' && intent?.rewardPreference === 'NONE') ||
                    (gift.id === 'dongbaek' && (!intent?.giftItem || intent?.giftItem.includes('동백전')));
                  return (
                    <button
                      key={gift.id}
                      type="button"
                      onClick={() => {
                        if (!intent) return;
                        if (gift.id === 'none') {
                          setIntent({ ...intent, rewardPreference: 'NONE', giftItem: '답례품 미수령 (전액 기탁)' });
                        } else {
                          const cleanName = gift.name.replace(/^[^\s]+\s/, '');
                          setIntent({ ...intent, rewardPreference: 'UNSPECIFIED', giftItem: cleanName });
                        }
                      }}
                      className={`clm-gift-card${isSelected ? ' is-selected' : ''}`}
                      aria-pressed={isSelected}
                    >
                      <strong>{gift.name}</strong>
                      <span className="clm-gift-desc">{gift.desc}</span>
                      <span className="clm-gift-foot">
                        <em>{gift.pts}</em>
                        {isSelected && <b>선택됨</b>}
                      </span>
                    </button>
                  );
                })}
              </div>

              <div className="clm-step-nav">
                <button type="button" className="clm-back-action" onClick={() => setActiveStep(1)}>이전 단계</button>
                <button type="button" className="clm-primary-action" onClick={() => setActiveStep(3)}>답례품 선택 완료 → STEP 03. 세액공제 및 동의</button>
              </div>
            </section>
          )}

          {/* ================= STEP 03: 세액공제 & 개인정보 동의 ================= */}
          {activeStep === 3 && (
            <section className="clm-document-section">
              <div className="clm-section-heading">
                <span className="clm-step-badge">
                  {isHometown ? 'STEP 03' : 'STEP 02'}
                </span>
                <h3>
                  {isHometown
                    ? '세액공제 신청 및 약정 동의'
                    : isVolunteer
                    ? '봉사 참여 서약 및 필수 동의'
                    : '후원 약정 필수 동의'}
                </h3>
                <p>
                  {isHometown
                    ? '국세청 홈택스 100% 세액공제 영수증 연동 및 필수 개인정보 동의를 진행하세요.'
                    : isVolunteer
                    ? '안전한 봉사활동 참여를 위한 필수 개인정보 및 서약 항목을 동의하세요.'
                    : '후원 약정 체결 및 본인 확인을 위한 필수 약정 동의를 진행하세요.'}
                </p>
              </div>

              <div className="clm-consent-panel">
                {isHometown && (
                  <label className="clm-tax-field">
                    국세청 연말정산 100% 세액공제 영수증 발급
                    <select
                      value={intent?.taxDeductionConsent !== false ? 'YES' : 'NO'}
                      onChange={(e) => intent && setIntent({ ...intent, taxDeductionConsent: e.target.value === 'YES' })}
                    >
                      <option value="YES">국세청 홈택스 자동 발급 신청함 (소득세법 제59조의4)</option>
                      <option value="NO">발급 신청하지 않음</option>
                    </select>
                  </label>
                )}

                <div className="clm-consent-list">
                  <label>
                    <input type="checkbox" checked={privacyConsent} onChange={(e) => setPrivacyConsent(e.target.checked)} />
                    <span><b>필수</b> 개인정보 수집 및 이용 동의 (약정 체결 및 본인 확인)</span>
                  </label>
                  <label>
                    <input type="checkbox" checked={thirdPartyConsent} onChange={(e) => setThirdPartyConsent(e.target.checked)} />
                    <span><b>필수</b> 주관기관 및 행정안전부 고향사랑e음 정보 제공 동의</span>
                  </label>
                  <label>
                    <input type="checkbox" checked={portraitConsent} onChange={(e) => setPortraitConsent(e.target.checked)} />
                    <span><b className="is-optional">선택</b> 활동 기록 및 잇다 온기 뱃지 수집 활용 동의</span>
                  </label>
                </div>
              </div>

              <div className="clm-step-nav">
                <button type="button" className="clm-back-action" onClick={() => setActiveStep(isHometown ? 2 : 1)}>이전 단계</button>
                <button
                  type="button"
                  className="clm-primary-action"
                  disabled={!privacyConsent || !thirdPartyConsent}
                  onClick={() => setActiveStep(4)}
                >
                  동의 완료 → {isHometown ? 'STEP 04' : 'STEP 03'}. 모두싸인 서명하기
                </button>
              </div>
            </section>
          )}

          {/* ================= STEP 04: 전자서명 & 증빙 ================= */}
          {activeStep === 4 && (
            <section className="clm-document-section">
              <div className="clm-section-heading">
                <span className="clm-step-badge">
                  {isHometown ? 'STEP 04' : 'STEP 03'}
                </span>
                <h3>모두싸인 전자서명 및 최종 증빙</h3>
                <p>생성된 약정서를 확인하고 모두싸인 보안 전자서명을 완료하세요.</p>
              </div>

              <div className="clm-sign-panel">
                <strong className="clm-sign-title">{documentName}</strong>
                <span className="clm-sign-meta">
                  서명자 {applicantName} ({applicantEmail}) · 체결 방식 모두싸인 전자서명 보안 인증
                </span>

                {!clmDoc ? (
                  <button
                    type="button"
                    disabled={requestingSign}
                    onClick={handleStartModusign}
                    className="clm-primary-action"
                  >
                    {requestingSign ? '약정서 및 서명창 준비 중…' : '약정서 생성 및 모두싸인 전자서명 시작'}
                  </button>
                ) : (
                  <div className="clm-sign-state">
                    <div className={`clm-sign-status${isSigned ? ' is-done' : ''}`}>
                      {isSigned ? '전자서명이 완료되었습니다.' : '모두싸인 서명이 진행 중입니다.'}
                    </div>
                    {signatureStatusMessage && <p className="clm-sign-note">{signatureStatusMessage}</p>}
                    <div className="clm-sign-actions">
                      {!isSigned && (
                        <>
                          <button type="button" className="clm-primary-action" onClick={openSigningWindow}>서명창 바로 열기</button>
                          <button type="button" className="clm-back-action" onClick={handleCheckSignature} disabled={checkingSignature}>{checkingSignature ? '확인 중…' : '서명 상태 확인'}</button>
                        </>
                      )}
                      <button type="button" className="clm-ghost-action" onClick={handleOpenDocView}>약정서 PDF 확인</button>
                    </div>
                  </div>
                )}
              </div>

              <div className="clm-step-nav">
                <button type="button" className="clm-back-action" onClick={() => setActiveStep(isHometown ? 3 : 1)}>이전 단계</button>
                {completed && (
                  <button type="button" className="clm-primary-action is-complete" onClick={onBack}>최종 신청 완료 · 목록으로 돌아가기</button>
                )}
              </div>
            </section>
          )}
        </div>
      </div>

      {/* 모두싸인 전자서명 진행 모달 */}
      {isSigningModalOpen && (
        <div className="clm-modal-backdrop">
          <div className="clm-modal clm-modal--sign">
            <div className="clm-modal-bar">
              <span>모두싸인 전자서명</span>
              <button type="button" onClick={() => setIsSigningModalOpen(false)} aria-label="닫기">✕</button>
            </div>
            {!clmDoc?.signingUrl ? (
              <div className="clm-modal-empty">서명 URL을 불러오는 중…</div>
            ) : isModusignUrl(clmDoc.signingUrl) ? (
              /*
               * 모두싸인 서명창은 iframe에 넣지 않고 새 탭으로 연다.
               * 다른 출처를 iframe에 담으면 브라우저가 서드파티 쿠키를 막아
               * 모두싸인이 서명 세션을 못 만들고 "문서에 접근할 수 없습니다"로 끝난다.
               * (사파리는 기본 차단이라 시연 환경에서 반드시 걸린다.)
               */
              <div className="clm-sign-launch">
                <strong>{documentName}</strong>
                <p>
                  아래 버튼을 누르면 모두싸인 서명창이 새 탭에서 열립니다.
                  서명을 마치면 이 화면이 자동으로 확인하고 서명창을 닫아 드립니다.
                </p>
                <button type="button" className="clm-primary-action" onClick={launchSigningWindow}>
                  모두싸인 서명창 열기 ↗
                </button>
                <button
                  type="button"
                  className="clm-back-action"
                  onClick={handleCheckSignature}
                  disabled={checkingSignature}
                >
                  {checkingSignature ? '확인 중…' : '서명 상태 확인'}
                </button>
                <small>링크는 발급 후 10분간 유효합니다. 만료되면 &lsquo;서명창 바로 열기&rsquo;를 다시 눌러 주세요.</small>
              </div>
            ) : (
              // 링크 발급이 막히면 서버가 로컬 대체 주소를 내려준다.
              // 그대로 띄우면 서명창 자리에 우리 서비스 화면이 들어와 더 혼란스럽다.
              <div className="clm-modal-empty">
                <strong>서명창을 지금 열 수 없습니다.</strong>
                <p>
                  모두싸인 서명 링크 발급이 일시적으로 제한되었습니다.
                  잠시 후 &lsquo;서명창 바로 열기&rsquo;를 다시 눌러 주세요.
                </p>
              </div>
            )}
          </div>
        </div>
      )}

      {/* 완료 약정 증서 열람 모달 */}
      {isDocViewModalOpen && (
        <div className="clm-modal-backdrop">
          <div className="clm-modal clm-modal--cert">
            <p className="clm-cert-eyebrow">SIGNED CERTIFICATE</p>
            <h2 className="clm-cert-title">잇다 전자서명 완료 약정 증서</h2>
            <table className="clm-cert-table">
              <tbody>
                <tr><th>약정서 명칭</th><td>{documentName}</td></tr>
                <tr><th>문서 관리번호</th><td className="is-mono">{clmDoc?.modusignDocumentId || 'MODU_SIGNED_PENDING'}</td></tr>
                <tr><th>신청 프로그램</th><td>{item.title} ({item.organizer})</td></tr>
                <tr><th>서명자 정보</th><td>{applicantName} ({applicantEmail})</td></tr>
                <tr><th>서명 인증 상태</th><td className="is-signed">전자서명법 제3조 규정 법적 증빙 완료 (SIGNED)</td></tr>
              </tbody>
            </table>

            {docFiles && docFiles.length > 0 ? (
              <div className="clm-cert-files">
                <strong>원본 약정서 PDF</strong>
                {docFiles.map((f) => (
                  <a key={f.id} href={f.downloadUrl} target="_blank" rel="noreferrer">
                    <span>{f.originalName} ({Math.round(f.sizeBytes / 1024)} KB)</span>
                    <em>다운로드</em>
                  </a>
                ))}
              </div>
            ) : (
              <div className="clm-cert-empty">
                약정서 생성이 완료되었습니다. 마이페이지 보관함에서도 언제든지 증명서를 조회할 수 있습니다.
              </div>
            )}

            <div className="clm-cert-close">
              <button type="button" className="clm-primary-action" onClick={() => setIsDocViewModalOpen(false)}>확인 및 닫기</button>
            </div>
          </div>
        </div>
      )}
    </article>
  );
};
