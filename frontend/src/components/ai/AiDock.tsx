import React, { useEffect } from 'react';
import { PixelAiMate } from './PixelAiMate';

interface AiDockProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onOpenModal: (title: string, type: 'volunteer' | 'donate') => void;
  /** AI 대화는 계정에 저장되므로 로그인한 사용자만 열 수 있다. */
  canUseAi: boolean;
  onRequireLogin: () => void;
}

/**
 * 모든 화면 오른쪽 아래에 떠 있는 AI 메이트 진입점.
 * 페이지를 떠나지 않고 그 자리에서 대화창을 열어준다.
 */
export const AiDock: React.FC<AiDockProps> = ({
  open,
  onOpenChange,
  onOpenModal,
  canUseAi,
  onRequireLogin,
}) => {
  // 열려 있는 동안에는 뒤 화면이 같이 스크롤되지 않도록 잠근다.
  useEffect(() => {
    if (!open) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onOpenChange(false);
    };
    window.addEventListener('keydown', closeOnEscape);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', closeOnEscape);
    };
  }, [open, onOpenChange]);

  return (
    <>
      <button
        type="button"
        className={`ai-dock-launcher${open ? ' is-open' : ''}`}
        onClick={() => {
          if (!canUseAi) {
            onRequireLogin();
            return;
          }
          onOpenChange(!open);
        }}
        aria-expanded={open}
        aria-label={open ? 'AI 메이트 닫기' : 'AI 메이트에게 추천받기'}
      >
        <span className="ai-dock-launcher-face" aria-hidden="true">
          {open ? '✕' : '🤖'}
        </span>
        {!open && <span className="ai-dock-launcher-label">AI 추천</span>}
      </button>

      {open && canUseAi && (
        <div className="ai-dock-backdrop" onClick={() => onOpenChange(false)} role="presentation">
          <section
            className="ai-dock-panel"
            role="dialog"
            aria-modal="true"
            aria-label="잇다 AI 메이트"
            onClick={(event) => event.stopPropagation()}
          >
            <header className="ai-dock-panel-bar">
              <span>ITDA AI MATE</span>
              <button type="button" onClick={() => onOpenChange(false)} aria-label="닫기">
                ✕
              </button>
            </header>
            <div className="ai-dock-panel-body">
              <PixelAiMate onOpenModal={onOpenModal} />
            </div>
          </section>
        </div>
      )}
    </>
  );
};
