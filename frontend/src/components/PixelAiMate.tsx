import React, { useState } from 'react';
import type { AiChatMessage } from '../types';
import { playBeep } from '../services/soundFx';

interface PixelAiMateProps {
  onOpenModal: (title: string, type: 'volunteer' | 'donate') => void;
}

export const PixelAiMate: React.FC<PixelAiMateProps> = ({ onOpenModal }) => {
  const [messages, setMessages] = useState<AiChatMessage[]>([
    {
      id: '1',
      sender: 'AI',
      text: '안녕! 나는 너의 픽셀 AI 메이트야 🤖<br>어떤 봉사활동이나 기부 프로젝트를 찾고 있니? 나에게 가볍게 말을 걸어봐!',
      createdAt: new Date().toLocaleTimeString(),
    },
  ]);
  const [inputVal, setInputVal] = useState('');

  const handleSend = (textToSend?: string) => {
    const query = textToSend || inputVal.trim();
    if (!query) return;

    const userMsg: AiChatMessage = {
      id: Date.now().toString(),
      sender: 'USER',
      text: query,
      createdAt: new Date().toLocaleTimeString(),
    };

    setMessages((prev) => [...prev, userMsg]);
    if (!textToSend) setInputVal('');
    playBeep(540, 0.1);

    // Simulate AI response (Upstage Solar LLM)
    setTimeout(() => {
      let replyHtml = '';
      let actionTitle = '';
      let actionType: 'volunteer' | 'donate' = 'volunteer';

      if (query.includes('유기동물') || query.includes('동물')) {
        actionTitle = '유기견 보육원 돌봄 봉사';
        actionType = 'volunteer';
        replyHtml = `
          🐾 <b>추천 봉사 미션: 유기견 보육원 주말 돌봄 봉사</b><br>
          요청하신 동물 관련 따뜻한 미션을 찾았습니다!<br>
          📍 장소: 부산 동물보호 센터 (4시간 인정)<br>
          🏷️ 1365 연동 데이터
        `;
      } else if (query.includes('플로깅') || query.includes('해변') || query.includes('환경')) {
        actionTitle = '부산 해운대 해변 플로깅';
        actionType = 'volunteer';
        replyHtml = `
          🌊 <b>추천 봉사 미션: 부산 해운대 해변 플로깅 정화</b><br>
          바다를 깨끗하게 만드는 주말 환경 미션입니다!<br>
          📍 장소: 해운대 해수욕장 광장 (3시간 인정)<br>
          🏷️ 1365 연동 데이터
        `;
      } else if (query.includes('어르신') || query.includes('도시락') || query.includes('급식')) {
        actionTitle = '독거어르신 온기 도시락 전달';
        actionType = 'volunteer';
        replyHtml = `
          🍲 <b>추천 봉사 미션: 독거어르신 온기 도시락 전달</b><br>
          어르신들께 따뜻한 한 끼와 안부를 전하는 감동 미션입니다.<br>
          📍 장소: 부산 자원봉사센터 (4시간 인정)
        `;
      } else {
        actionTitle = '유기견 난방비 기부';
        actionType = 'donate';
        replyHtml = `
          ❤️ <b>추천 기부 프로젝트: 유기견 겨울 난방비 모금</b><br>
          작은 마음 하나로 픽셀 온도를 올려주세요!<br>
          📍 기부 목표: 1,000,000원 (85% 달성 중)
        `;
      }

      const aiMsg: AiChatMessage = {
        id: (Date.now() + 1).toString(),
        sender: 'AI',
        text: replyHtml,
        createdAt: new Date().toLocaleTimeString(),
        recommendedCard: {
          id: Date.now(),
          title: actionTitle,
          category: actionType === 'volunteer' ? 'VOLUNTEER' : 'DONATION',
          location: '부산 관내',
          organizer: '픽셀 케어 연동 기관',
          tags: ['1365 연동', '추천 미션'],
        },
      };

      setMessages((prev) => [...prev, aiMsg]);
      playBeep(640, 0.1);
    }, 600);
  };

  return (
    <div className="ai-chat-section">
      <div className="ai-chat-header">
        <div className="ai-avatar-box">
          <div className="ai-avatar">🤖</div>
          <div>
            <div className="ai-title">픽셀 AI 메이트 (Pixel AI Mate)</div>
            <div className="ai-subtitle">Upstage Solar LLM 기반 개인 맞춤형 봉사/기부 큐레이터</div>
          </div>
        </div>
        <span className="pixel-tag pixel-tag-gov">ONLINE</span>
      </div>

      <div className="chat-box">
        {messages.map((msg) => (
          <div key={msg.id} className={`chat-bubble ${msg.sender.toLowerCase()}`}>
            <div dangerouslySetInnerHTML={{ __html: msg.text }} />
            {msg.recommendedCard && (
              <div style={{ marginTop: '10px' }}>
                <button
                  className={`pixel-btn ${msg.recommendedCard.category === 'DONATION' ? 'pixel-btn-red' : ''}`}
                  style={{ fontSize: '11px', padding: '6px 12px' }}
                  onClick={() =>
                    onOpenModal(
                      msg.recommendedCard!.title,
                      msg.recommendedCard!.category === 'DONATION' ? 'donate' : 'volunteer'
                    )
                  }
                >
                  {msg.recommendedCard.category === 'DONATION'
                    ? '❤️ 픽셀 마음 기부하기'
                    : '⚡ 바로 참가 신청하기'}
                </button>
              </div>
            )}
          </div>
        ))}
      </div>

      <div className="prompt-chips">
        <button className="chip-btn" onClick={() => handleSend('🐶 주말 유기동물 봉사 추천해줘')}>
          🐶 주말 유기동물 봉사
        </button>
        <button className="chip-btn" onClick={() => handleSend('🌊 해변 플로깅 미션 알려줘')}>
          🌊 해변 플로깅 미션
        </button>
        <button className="chip-btn" onClick={() => handleSend('🍲 어르신 도시락 전달 봉사')}>
          🍲 어르신 도시락 봉사
        </button>
        <button className="chip-btn" onClick={() => handleSend('❤️ 따뜻한 기부 프로젝트 추천')}>
          ❤️ 기부 프로젝트
        </button>
      </div>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          handleSend();
        }}
        className="chat-input-row"
      >
        <input
          type="text"
          className="pixel-input"
          placeholder="AI에게 무엇이든 물어보세요... (예: 부산 해운대 근처 봉사추천)"
          value={inputVal}
          onChange={(e) => setInputVal(e.target.value)}
        />
        <button type="submit" className="pixel-btn pixel-btn-purple">
          전송 🚀
        </button>
      </form>
    </div>
  );
};
