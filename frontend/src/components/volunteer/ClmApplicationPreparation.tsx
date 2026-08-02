import React, { useEffect, useRef, useState } from 'react';
import type { SessionUser, VolunteerItem } from '../../types';
import {
  createApplication,
  submitCommitment,
} from '../../services/applicationApi';
import { requestClmSign, requestSignFromConversation, fetchClmDocument, fetchClmDocumentFiles } from '../../services/clmApi';
import type { ClmDocumentDto, ClmDocumentFileDto } from '../../services/clmApi';
import {
  confirmConsultation,
  startConsultation,
  sendConsultationMessage,
  updateConsultationIntent,
} from '../../services/consultationApi';
import type { ConsultationResponse, PledgeIntent } from '../../services/consultationApi';
import { fetchMyProfile } from '../../services/authApi';

interface ChatMessage {
  role: 'ai' | 'user';
  content: string;
  timestamp?: Date;
}

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
  const documentName = isVolunteer ? '봉사 참여 약정서 (제2026-PC-01호)' : '후원 및 기부 약정서 (제2026-PC-02호)';

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
      ? `안녕하세요! 저는 픽셀케어 AI 상담 매니저예요 🙌\n\n**[${item.title}]** (${item.location} / 주관: ${item.organizer}) 봉사 프로그램 신청을 도와드릴게요.\n\n어떤 주기로 봉사에 참여하고 싶으신가요? (예: 주말 매주, 격주, 하루 일시 참여 등) 😊`
      : isHometown
      ? `안녕하세요! 저는 픽셀케어 AI 기부 상담사예요 🏡\n\n**[${item.title}]** (${item.location} / ${item.organizer}) 고향사랑기부에 관심 가져주셔서 감사해요!\n\n10만원 이하 기부 시 100% 전액 세액공제 환급과 30% 답례품(지역 화폐/특산품) 혜택이 지원돼요. 기부 납부 주기는 어떻게 생각하고 계신가요? (예: 일시 기부, 매월 정기 후원 등) 🌟`
      : isLegacy
      ? `안녕하세요! 저는 픽셀케어 AI 유산기부 상담사예요 📜\n\n**[${item.title}]** (${item.location} / ${item.organizer}) 유산기부 약정 안내를 도와드릴게요.\n\n약정 주기나 절차 중 궁금하신 점이나 희망하시는 방식이 있으신가요? 🤝`
      : `안녕하세요! 저는 픽셀케어 AI 기부 상담사예요 💝\n\n**[${item.title}]** (${item.location} / ${item.organizer}) 기부 신청을 선택해 주셨네요!\n\n기부 납부 주기는 어떻게 생각하고 계신가요? (예: 일시 기부, 매월 정기 후원, 분기별 후원 등)`;
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
            ? '픽셀케어 봉사 활동 참여 및 약정 시스템'
            : isLegacy
            ? '픽셀케어 유산 및 지정 기부 약정 시스템'
            : '픽셀케어 후원 및 기부 약정 시스템'}
        </p>
        <h2>신청 서류 작성 & 전자서명</h2>
        <span>
          신청 정보를 바탕으로 약정서를 작성하고 모두싸인 전자서명을 완료하면 최종 신청 및 서류가 안전하게 보존됩니다.
        </span>
      </header>

      {/* 프로그램 유형별 동적 스테퍼 헤더 */}
      <ol className="clm-steps" aria-label="신청 진행 단계" style={{
        display: 'grid',
        gridTemplateColumns: `repeat(${isHometown ? 4 : 3}, 1fr)`,
        gap: '8px',
        marginBottom: '24px',
        listStyle: 'none',
        padding: 0
      }}>
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
              onClick={() => setActiveStep(s.step as 1 | 2 | 3 | 4)}
              style={{
                background: isActive ? 'linear-gradient(135deg, #0077b6, #2ec4b6)' : isDone ? 'rgba(46,196,182,0.15)' : '#f8fafc',
                border: isActive ? '2px solid #0077b6' : isDone ? '1px solid #2ec4b6' : '1px solid #e2e8f0',
                color: isActive ? '#fff' : isDone ? '#0077b6' : '#64748b',
                borderRadius: '12px',
                padding: '12px',
                textAlign: 'center',
                cursor: 'pointer',
                transition: 'all 0.2s',
                boxShadow: isActive ? '0 4px 14px rgba(0,119,182,0.3)' : 'none',
              }}
            >
              <strong style={{ display: 'block', fontSize: '13px', fontWeight: 800 }}>{s.title}</strong>
              <span style={{ fontSize: '11px', opacity: isActive ? 0.9 : 0.7 }}>{s.sub}</span>
            </li>
          );
        })}
      </ol>

      <div className="clm-layout">
        <div className="clm-main">
          {/* 프로그램 요약 정보 */}
          <section className="clm-program-summary" style={{ marginBottom: '16px', background: '#f8fafc', border: '1px solid #cbd5e1', borderRadius: '12px', padding: '16px' }}>
            <div>
              <span style={{ fontSize: '11px', color: '#64748b', fontWeight: 700 }}>신청 프로그램</span>
              <strong style={{ display: 'block', fontSize: '15px', color: '#0f172a', fontWeight: 800 }}>{item.title}</strong>
            </div>
            <dl style={{ display: 'flex', gap: '20px', marginTop: '8px', fontSize: '12px', color: '#475569' }}>
              <div><dt style={{ display: 'inline', fontWeight: 700 }}>구분: </dt><dd style={{ display: 'inline' }}>{typeLabel}</dd></div>
              <div><dt style={{ display: 'inline', fontWeight: 700 }}>주관기관: </dt><dd style={{ display: 'inline' }}>{item.organizer}</dd></div>
              <div><dt style={{ display: 'inline', fontWeight: 700 }}>지역: </dt><dd style={{ display: 'inline' }}>{item.location}</dd></div>
            </dl>
          </section>

          {/* ================= STEP 01: 기부 약정 (AI 상담 & 의사 정리) ================= */}
          {activeStep === 1 && (
            <section className="clm-document-section" style={{ background: '#fff', border: '2px solid #2ec4b6', borderRadius: '16px', padding: '24px' }}>
              <div className="clm-section-heading" style={{ marginBottom: '16px' }}>
                <span style={{ background: '#0077b6', color: '#fff', fontSize: '11px', fontWeight: 800, padding: '3px 8px', borderRadius: '6px' }}>STEP 01</span>
                <h3 style={{ fontSize: '18px', fontWeight: 800, margin: '6px 0 2px', color: '#0f172a' }}>💬 기부 약정 정리 (Upstage Solar AI 대화)</h3>
                <p style={{ fontSize: '12px', color: '#64748b', margin: 0 }}>AI 마스코트와 대화를 나눠 기부금액, 약정 주기 및 기부 목적을 정리해 주세요.</p>
              </div>

              {/* 채팅창 컨테이너 */}
              <div style={{
                background: 'linear-gradient(135deg, #0f0f1a 0%, #1a1a2e 100%)',
                border: '2px solid #2ec4b6',
                borderRadius: '16px',
                overflow: 'hidden',
                boxShadow: '0 8px 32px rgba(46,196,182,0.15)',
                marginBottom: '16px',
              }}>
                <div style={{
                  background: 'linear-gradient(90deg, #2ec4b6, #0077b6)',
                  padding: '12px 16px', display: 'flex', alignItems: 'center', gap: '10px',
                }}>
                  <div style={{
                    width: '36px', height: '36px', borderRadius: '50%', background: 'rgba(255,255,255,0.15)',
                    border: '2px solid rgba(255,255,255,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '18px',
                  }}>🤖</div>
                  <div>
                    <div style={{ color: '#fff', fontWeight: 800, fontSize: '13px' }}>Pixel AI 약정 매니저</div>
                    <div style={{ color: 'rgba(255,255,255,0.75)', fontSize: '11px' }}>
                      {chatSending ? '생각 중...' : 'Upstage Solar LLM · 실시간 의사 분석'}
                    </div>
                  </div>
                </div>

                <div style={{
                  height: '300px', overflowY: 'auto', padding: '16px',
                  display: 'flex', flexDirection: 'column', gap: '12px',
                }}>
                  {chatMessages.map((msg, idx) => (
                    <div key={idx} style={{
                      display: 'flex', flexDirection: msg.role === 'user' ? 'row-reverse' : 'row', alignItems: 'flex-end', gap: '8px',
                    }}>
                      <div style={{
                        maxWidth: '80%', padding: '10px 14px', borderRadius: '14px', fontSize: '13px', lineHeight: 1.5,
                        background: msg.role === 'user' ? 'linear-gradient(135deg, #2ec4b6, #0077b6)' : 'rgba(255,255,255,0.08)',
                        color: '#fff', border: msg.role === 'user' ? 'none' : '1px solid rgba(255,255,255,0.12)',
                        whiteSpace: 'pre-wrap',
                      }}>
                        {msg.content}
                      </div>
                    </div>
                  ))}
                  <div ref={chatEndRef} />
                </div>

                {/* 퀵 칩 입력 혜택 */}
                {!aiConfirmed && (
                  <div style={{ padding: '8px 16px', display: 'flex', flexWrap: 'wrap', gap: '6px', borderTop: '1px solid rgba(46,196,182,0.2)', background: 'rgba(0,0,0,0.2)' }}>
                    {(isVolunteer
                      ? ['주말 매주 참여해요', '격주 봉사 원해요', '하루 일시 참여']
                      : ['매월 3만원 기부할게요', '일시 10만원 기부할게요', '매년 100만원 기부', '답례품 미수령']
                    ).map((chip) => (
                      <button key={chip} type="button" disabled={chatSending} onClick={() => handleChatSend(chip)} style={{ background: 'rgba(46,196,182,0.15)', border: '1px solid rgba(46,196,182,0.4)', borderRadius: '20px', color: '#2ec4b6', fontSize: '11px', fontWeight: 600, padding: '4px 10px', cursor: 'pointer' }}>{chip}</button>
                    ))}
                  </div>
                )}

                {!aiConfirmed && (
                  <div style={{ padding: '12px 16px', borderTop: '1px solid rgba(46,196,182,0.2)', display: 'flex', gap: '10px' }}>
                    <textarea
                      ref={chatInputRef}
                      value={chatInput}
                      onChange={(e) => setChatInput(e.target.value)}
                      onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleChatSend(); } }}
                      placeholder="약정 관련 답변을 입력하세요... (예: 매월 3만원 부산 영도구 기부할게요)"
                      rows={1}
                      disabled={chatSending}
                      style={{ flex: 1, background: 'rgba(255,255,255,0.08)', border: '1px solid rgba(46,196,182,0.4)', borderRadius: '12px', padding: '10px 14px', color: '#fff', fontSize: '13px', outline: 'none', resize: 'none' }}
                    />
                    <button
                      type="button"
                      onClick={() => handleChatSend()}
                      disabled={!chatInput.trim() || chatSending}
                      style={{ width: '44px', height: '44px', borderRadius: '50%', background: 'linear-gradient(135deg, #2ec4b6, #0077b6)', border: 'none', color: '#fff', cursor: 'pointer' }}
                    >➤</button>
                  </div>
                )}
              </div>

              {/* AI 정리 의향 카드 */}
              {intent && (
                <div style={{ background: '#f8fafc', border: '1.5px solid #2ec4b6', borderRadius: '12px', padding: '16px', marginBottom: '16px' }}>
                  <div style={{ fontWeight: 800, fontSize: '13px', color: '#0077b6', marginBottom: '12px' }}>
                    📋 AI 정리 약정 내역 (수정 가능)
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                    <label style={{ fontSize: '12px', fontWeight: 700 }}>
                      약정 유형
                      <select value={intent.pledgeType || ''} onChange={(e) => setIntent({ ...intent, pledgeType: e.target.value })} style={{ width: '100%', padding: '6px', borderRadius: '6px', border: '1px solid #cbd5e1' }}>
                        <option value="DONATION">일반 기부</option>
                        <option value="HOMETOWN_DONATION">고향사랑기부</option>
                        <option value="VOLUNTEER">봉사 참여</option>
                        <option value="LEGACY_DONATION">유산 기부</option>
                      </select>
                    </label>
                    <label style={{ fontSize: '12px', fontWeight: 700 }}>
                      주기 (납부/참여)
                      <select value={intent.frequency || ''} onChange={(e) => setIntent({ ...intent, frequency: e.target.value })} style={{ width: '100%', padding: '6px', borderRadius: '6px', border: '1px solid #cbd5e1' }}>
                        <option value="ONE_TIME">일시 (1회성)</option>
                        <option value="WEEKLY">매주 (주간)</option>
                        <option value="MONTHLY">매월 (월간)</option>
                        <option value="ANNUAL">매년 (연간)</option>
                      </select>
                    </label>
                    <label style={{ fontSize: '12px', fontWeight: 700 }}>
                      수혜 대상·기관
                      <input value={intent.beneficiary || ''} onChange={(e) => setIntent({ ...intent, beneficiary: e.target.value })} style={{ width: '100%', padding: '6px', borderRadius: '6px', border: '1px solid #cbd5e1' }} />
                    </label>
                    <label style={{ fontSize: '12px', fontWeight: 700 }}>
                      지역
                      <input value={intent.region || ''} onChange={(e) => setIntent({ ...intent, region: e.target.value })} style={{ width: '100%', padding: '6px', borderRadius: '6px', border: '1px solid #cbd5e1' }} />
                    </label>
                  </div>
                  <button
                    type="button"
                    onClick={async () => {
                      if (!intent) return;
                      await handleConfirmIntent();
                      setActiveStep(isHometown ? 2 : 3);
                    }}
                    style={{
                      width: '100%', marginTop: '16px', padding: '14px', background: 'linear-gradient(135deg, #0077b6, #2ec4b6)',
                      color: '#fff', border: 'none', borderRadius: '12px', fontWeight: 800, fontSize: '14px', cursor: 'pointer',
                    }}
                  >
                    {isHometown
                      ? '약정 의사 확정 완료 → STEP 02. 답례품 선택하기 ➔'
                      : '약정 의사 확정 완료 → STEP 02. 필수 동의 진행하기 ➔'}
                  </button>
                </div>
              )}
              {aiFeedback && <div style={{ color: '#ef4444', fontSize: '12px', marginTop: '8px' }}>{aiFeedback}</div>}
            </section>
          )}

          {/* ================= STEP 02: 답례품 선택 (isHometown 전용) ================= */}
          {activeStep === 2 && isHometown && (
            <section className="clm-document-section" style={{ background: '#fff', border: '2px solid #2ec4b6', borderRadius: '16px', padding: '24px' }}>
              <div className="clm-section-heading" style={{ marginBottom: '16px' }}>
                <span style={{ background: '#0077b6', color: '#fff', fontSize: '11px', fontWeight: 800, padding: '3px 8px', borderRadius: '6px' }}>STEP 02</span>
                <h3 style={{ fontSize: '18px', fontWeight: 800, margin: '6px 0 2px', color: '#0f172a' }}>🎁 답례품 선택 (부산 고향사랑e음 busanlove.kr)</h3>
                <p style={{ fontSize: '12px', color: '#64748b', margin: 0 }}>기부금의 30% 한도 내에서 제공되는 부산 명품 답례품을 선택하세요.</p>
              </div>

              <div style={{
                background: 'linear-gradient(135deg, #e6f4f1, #f0f9ff)',
                border: '1.5px solid #2ec4b6', borderRadius: '12px', padding: '16px', marginBottom: '20px',
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              }}>
                <div>
                  <div style={{ fontWeight: 800, fontSize: '14px', color: '#0077b6' }}>고향사랑기부 산정 포인트</div>
                  <div style={{ fontSize: '12px', color: '#555' }}>소득세법 10만원 100% 세액공제 + 30% 답례품 포인트 환급</div>
                </div>
                <div style={{ background: '#0077b6', color: '#fff', fontSize: '16px', fontWeight: 800, padding: '6px 16px', borderRadius: '20px' }}>
                  보유 {intent?.amount ? Math.floor(intent.amount * 0.3).toLocaleString() : '30,000'} P
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px', marginBottom: '24px' }}>
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
                      style={{
                        border: isSelected ? '2.5px solid #0077b6' : '1px solid #cbd5e1',
                        background: isSelected ? '#f0f9ff' : '#fff',
                        borderRadius: '12px', padding: '14px', textAlign: 'left', cursor: 'pointer', transition: 'all 0.2s',
                        boxShadow: isSelected ? '0 4px 14px rgba(0,119,182,0.2)' : 'none',
                      }}
                    >
                      <div style={{ fontWeight: 800, fontSize: '13px', color: isSelected ? '#0077b6' : '#1e293b', marginBottom: '4px' }}>{gift.name}</div>
                      <div style={{ fontSize: '11px', color: '#64748b', marginBottom: '8px' }}>{gift.desc}</div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span style={{ fontSize: '11px', fontWeight: 800, color: '#2ec4b6' }}>{gift.pts}</span>
                        {isSelected && <span style={{ fontSize: '11px', fontWeight: 800, color: '#0077b6' }}>✓ 선택됨</span>}
                      </div>
                    </button>
                  );
                })}
              </div>

              <div style={{ display: 'flex', gap: '10px' }}>
                <button type="button" onClick={() => setActiveStep(1)} style={{ padding: '12px 20px', background: '#e2e8f0', border: 'none', borderRadius: '10px', fontWeight: 700, cursor: 'pointer' }}>← 1단계 이전</button>
                <button type="button" onClick={() => setActiveStep(3)} style={{ flex: 1, padding: '12px 20px', background: 'linear-gradient(135deg, #0077b6, #2ec4b6)', color: '#fff', border: 'none', borderRadius: '10px', fontWeight: 800, cursor: 'pointer' }}>답례품 선택 완료 → STEP 03. 세액공제 및 동의 ➔</button>
              </div>
            </section>
          )}

          {/* ================= STEP 03: 세액공제 & 개인정보 동의 ================= */}
          {activeStep === 3 && (
            <section className="clm-document-section" style={{ background: '#fff', border: '2px solid #2ec4b6', borderRadius: '16px', padding: '24px' }}>
              <div className="clm-section-heading" style={{ marginBottom: '16px' }}>
                <span style={{ background: '#0077b6', color: '#fff', fontSize: '11px', fontWeight: 800, padding: '3px 8px', borderRadius: '6px' }}>
                  {isHometown ? 'STEP 03' : 'STEP 02'}
                </span>
                <h3 style={{ fontSize: '18px', fontWeight: 800, margin: '6px 0 2px', color: '#0f172a' }}>
                  {isHometown
                    ? '⚖️ 세액공제 신청 및 약정 동의'
                    : isVolunteer
                    ? '🛡️ 봉사 참여 서약 및 필수 동의'
                    : '🛡️ 후원 약정 필수 동의'}
                </h3>
                <p style={{ fontSize: '12px', color: '#64748b', margin: 0 }}>
                  {isHometown
                    ? '국세청 홈택스 100% 세액공제 영수증 연동 및 필수 개인정보 동의를 진행하세요.'
                    : isVolunteer
                    ? '안전한 봉사활동 참여를 위한 필수 개인정보 및 서약 항목을 동의하세요.'
                    : '후원 약정 체결 및 본인 확인을 위한 필수 약정 동의를 진행하세요.'}
                </p>
              </div>

              <div style={{ background: '#f8fafc', borderRadius: '12px', padding: '16px', marginBottom: '20px' }}>
                {isHometown && (
                  <label style={{ display: 'flex', flexDirection: 'column', gap: '6px', fontSize: '13px', fontWeight: 700, color: '#1e293b', marginBottom: '16px' }}>
                    🏛️ 국세청 연말정산 100% 세액공제 영수증 발급
                    <select
                      value={intent?.taxDeductionConsent !== false ? 'YES' : 'NO'}
                      onChange={(e) => intent && setIntent({ ...intent, taxDeductionConsent: e.target.value === 'YES' })}
                      style={{ padding: '10px', borderRadius: '8px', border: '1.5px solid #2ec4b6', fontSize: '13px', background: '#fff' }}
                    >
                      <option value="YES">국세청 홈택스 자동 발급 신청함 (소득세법 제59조의4)</option>
                      <option value="NO">발급 신청하지 않음</option>
                    </select>
                  </label>
                )}

                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '13px', cursor: 'pointer' }}>
                    <input type="checkbox" checked={privacyConsent} onChange={(e) => setPrivacyConsent(e.target.checked)} style={{ width: '18px', height: '18px' }} />
                    <span>[필수] 개인정보 수집 및 이용 동의 (약정 체결 및 본인 확인)</span>
                  </label>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '13px', cursor: 'pointer' }}>
                    <input type="checkbox" checked={thirdPartyConsent} onChange={(e) => setThirdPartyConsent(e.target.checked)} style={{ width: '18px', height: '18px' }} />
                    <span>[필수] 주관기관 및 행정안전부 고향사랑e음 정보 제공 동의</span>
                  </label>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '13px', cursor: 'pointer' }}>
                    <input type="checkbox" checked={portraitConsent} onChange={(e) => setPortraitConsent(e.target.checked)} style={{ width: '18px', height: '18px' }} />
                    <span>[선택] 활동 기록 및 픽셀 온기 뱃지 수집 활용 동의</span>
                  </label>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '10px' }}>
                <button type="button" onClick={() => setActiveStep(isHometown ? 2 : 1)} style={{ padding: '12px 20px', background: '#e2e8f0', border: 'none', borderRadius: '10px', fontWeight: 700, cursor: 'pointer' }}>← 이전 단계</button>
                <button
                  type="button"
                  disabled={!privacyConsent || !thirdPartyConsent}
                  onClick={() => setActiveStep(4)}
                  style={{
                    flex: 1, padding: '12px 20px',
                    background: privacyConsent && thirdPartyConsent ? 'linear-gradient(135deg, #0077b6, #2ec4b6)' : '#cbd5e1',
                    color: '#fff', border: 'none', borderRadius: '10px', fontWeight: 800, cursor: privacyConsent && thirdPartyConsent ? 'pointer' : 'not-allowed',
                  }}
                >
                  동의 완료 → {isHometown ? 'STEP 04' : 'STEP 03'}. 모두싸인 서명하기 ➔
                </button>
              </div>
            </section>
          )}

          {/* ================= STEP 04: 전자서명 & 증빙 ================= */}
          {activeStep === 4 && (
            <section className="clm-document-section" style={{ background: '#fff', border: '2px solid #2ec4b6', borderRadius: '16px', padding: '24px' }}>
              <div className="clm-section-heading" style={{ marginBottom: '16px' }}>
                <span style={{ background: '#0077b6', color: '#fff', fontSize: '11px', fontWeight: 800, padding: '3px 8px', borderRadius: '6px' }}>
                  {isHometown ? 'STEP 04' : 'STEP 03'}
                </span>
                <h3 style={{ fontSize: '18px', fontWeight: 800, margin: '6px 0 2px', color: '#0f172a' }}>✍️ 모두싸인 API 전자서명 및 최종 증빙</h3>
                <p style={{ fontSize: '12px', color: '#64748b', margin: 0 }}>생성된 약정서를 확인하고 모두싸인 보안 전자서명을 완료하세요.</p>
              </div>

              <div style={{ background: '#f8fafc', borderRadius: '12px', padding: '16px', marginBottom: '20px' }}>
                <div style={{ fontWeight: 800, fontSize: '14px', color: '#0077b6', marginBottom: '8px' }}>
                  📄 {documentName}
                </div>
                <div style={{ fontSize: '12px', color: '#64748b', marginBottom: '16px' }}>
                  서명자: {applicantName} ({applicantEmail}) · 체결 방식: 모두싸인 전자서명 보안 인증
                </div>

                {!clmDoc ? (
                  <button
                    type="button"
                    disabled={requestingSign}
                    onClick={handleStartModusign}
                    style={{
                      width: '100%', padding: '14px', background: 'linear-gradient(135deg, #0077b6, #2ec4b6)',
                      color: '#fff', border: 'none', borderRadius: '12px', fontWeight: 800, fontSize: '14px', cursor: 'pointer',
                    }}
                  >
                    {requestingSign ? '약정서 및 서명창 준비 중...' : '🖋️ 약정서 생성 및 모두싸인 전자서명 시작'}
                  </button>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    <div style={{ padding: '12px', background: '#e6f4f1', borderRadius: '8px', fontSize: '13px', color: '#0077b6', fontWeight: 700 }}>
                      {isSigned ? '✅ 전자서명이 성공적으로 완료되었습니다!' : '⏳ 모두싸인 서명이 진행 중입니다.'}
                    </div>
                    {signatureStatusMessage && <div style={{ fontSize: '12px', color: '#0284c7' }}>{signatureStatusMessage}</div>}
                    <div style={{ display: 'flex', gap: '10px' }}>
                      {!isSigned && (
                        <>
                          <button type="button" onClick={() => setIsSigningModalOpen(true)} style={{ flex: 1, padding: '10px', background: '#0077b6', color: '#fff', border: 'none', borderRadius: '8px', fontWeight: 700, cursor: 'pointer' }}>서명창 바로 열기</button>
                          <button type="button" onClick={handleCheckSignature} disabled={checkingSignature} style={{ padding: '10px 14px', background: '#e2e8f0', border: 'none', borderRadius: '8px', fontWeight: 700, cursor: 'pointer' }}>{checkingSignature ? '확인중...' : '🔄 서명 상태 확인'}</button>
                        </>
                      )}
                      <button type="button" onClick={handleOpenDocView} style={{ flex: 1, padding: '10px', background: '#2ec4b6', color: '#fff', border: 'none', borderRadius: '8px', fontWeight: 700, cursor: 'pointer' }}>약정서 PDF 확인</button>
                    </div>
                  </div>
                )}
              </div>

              <div style={{ display: 'flex', gap: '10px' }}>
                <button type="button" onClick={() => setActiveStep(3)} style={{ padding: '12px 20px', background: '#e2e8f0', border: 'none', borderRadius: '10px', fontWeight: 700, cursor: 'pointer' }}>← 3단계 이전</button>
                {completed && (
                  <button type="button" onClick={onBack} style={{ flex: 1, padding: '12px 20px', background: '#10b981', color: '#fff', border: 'none', borderRadius: '10px', fontWeight: 800, cursor: 'pointer' }}>🎉 최종 신청 완료 및 목록으로 돌아가기</button>
                )}
              </div>
            </section>
          )}
        </div>
      </div>

      {/* 모두싸인 전자서명 진행 모달 */}
      {isSigningModalOpen && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.8)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 9999, padding: '20px' }}>
          <div style={{ background: '#fff', width: '100%', maxWidth: '800px', height: '85vh', borderRadius: '16px', display: 'flex', flexDirection: 'column', overflow: 'hidden', border: '3px solid #2ec4b6' }}>
            <div style={{ background: '#2ec4b6', padding: '12px 20px', color: '#fff', display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontWeight: 800 }}>
              <span>✍️ 모두싸인 전자서명</span>
              <button type="button" onClick={() => setIsSigningModalOpen(false)} style={{ background: 'none', border: 'none', color: '#fff', fontSize: '20px', cursor: 'pointer' }}>✕</button>
            </div>
            {clmDoc?.signingUrl ? (
              <iframe src={clmDoc.signingUrl} title="Modusign Signing" style={{ width: '100%', flex: 1, border: 'none' }} />
            ) : (
              <div style={{ padding: '40px', textAlign: 'center' }}>서명 URL을 불러오는 중...</div>
            )}
          </div>
        </div>
      )}

      {/* 완료 약정 증서 열람 모달 */}
      {isDocViewModalOpen && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.85)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 9999, padding: '20px' }}>
          <div style={{ background: '#fff', width: '100%', maxWidth: '720px', maxHeight: '90vh', borderRadius: '16px', border: '3px solid #2ec4b6', padding: '28px', overflowY: 'auto', textAlign: 'left', position: 'relative' }}>
            <h2 style={{ textAlign: 'center', fontSize: '20px', fontWeight: 800, color: '#111', marginBottom: '16px' }}>📜 픽셀케어 전자서명 완료 약정 증서</h2>
            <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: '20px', fontSize: '13px' }}>
              <tbody>
                <tr><th style={{ background: '#f8f9fa', padding: '10px', border: '1px solid #e2e8f0', width: '30%' }}>약정서 명칭</th><td style={{ padding: '10px', border: '1px solid #e2e8f0', fontWeight: 'bold' }}>{documentName}</td></tr>
                <tr><th style={{ background: '#f8f9fa', padding: '10px', border: '1px solid #e2e8f0' }}>문서 관리번호</th><td style={{ padding: '10px', border: '1px solid #e2e8f0', color: '#0077b6', fontWeight: 'bold' }}>{clmDoc?.modusignDocumentId || 'MODU_SIGNED_PENDING'}</td></tr>
                <tr><th style={{ background: '#f8f9fa', padding: '10px', border: '1px solid #e2e8f0' }}>신청 프로그램</th><td style={{ padding: '10px', border: '1px solid #e2e8f0' }}>{item.title} ({item.organizer})</td></tr>
                <tr><th style={{ background: '#f8f9fa', padding: '10px', border: '1px solid #e2e8f0' }}>서명자 정보</th><td style={{ padding: '10px', border: '1px solid #e2e8f0' }}>{applicantName} ({applicantEmail})</td></tr>
                <tr><th style={{ background: '#f8f9fa', padding: '10px', border: '1px solid #e2e8f0' }}>서명 인증 상태</th><td style={{ padding: '10px', border: '1px solid #e2e8f0', color: '#10b981', fontWeight: 'bold' }}>전자서명법 제3조 규정 법적 증빙 완료 (SIGNED)</td></tr>
              </tbody>
            </table>

            {docFiles && docFiles.length > 0 ? (
              <div style={{ marginBottom: '20px', background: '#f0f9ff', border: '1px solid #bae6fd', borderRadius: '10px', padding: '14px' }}>
                <div style={{ fontWeight: 800, fontSize: '13px', color: '#0369a1', marginBottom: '8px' }}>📁 원본 약정서 PDF 다운로드</div>
                {docFiles.map((f) => (
                  <a key={f.id} href={f.downloadUrl} target="_blank" rel="noreferrer" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 12px', background: '#fff', border: '1px solid #cbd5e1', borderRadius: '8px', fontSize: '12px', color: '#0077b6', textDecoration: 'none', marginBottom: '6px', fontWeight: 700 }}>
                    <span>📥 {f.originalName} ({Math.round(f.sizeBytes / 1024)} KB)</span>
                    <span style={{ fontSize: '11px', background: '#0284c7', color: '#fff', padding: '2px 8px', borderRadius: '4px' }}>다운로드</span>
                  </a>
                ))}
              </div>
            ) : (
              <div style={{ marginBottom: '20px', background: '#f8fafc', border: '1px solid #cbd5e1', borderRadius: '10px', padding: '14px', textAlign: 'center', fontSize: '12px', color: '#64748b' }}>
                💡 약정서 생성이 완료되었으며, 보관함(마이페이지)에서도 언제든지 증명서를 조회할 수 있습니다.
              </div>
            )}

            <div style={{ textAlign: 'center' }}>
              <button type="button" style={{ padding: '12px 32px', background: 'linear-gradient(135deg, #0077b6, #2ec4b6)', color: '#fff', border: 'none', borderRadius: '10px', fontWeight: 800, fontSize: '14px', cursor: 'pointer' }} onClick={() => setIsDocViewModalOpen(false)}>확인 및 닫기</button>
            </div>
          </div>
        </div>
      )}
    </article>
  );
};
