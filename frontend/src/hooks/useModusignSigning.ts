import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchClmDocument, type ClmDocumentDto } from '../services/clmApi';

/**
 * 모두싸인 서명창을 열고 완료를 감지한다.
 *
 * 서명창은 iframe이 아니라 새 탭으로 연다. 다른 출처를 iframe에 담으면
 * 브라우저가 서드파티 쿠키를 막아 모두싸인이 서명 세션을 만들지 못한다.
 *
 * 서명을 마쳐도 모두싸인이 우리 화면으로 되돌려 보내주지는 못한다.
 * 되돌아오려면 공개 https 주소가 필요한데 개발 환경은 localhost라서다.
 * 그래서 창을 직접 들고 있다가 완료가 확인되면 닫아 준다.
 */
export function useModusignSigning(onSigned?: () => void) {
  const windowRef = useRef<Window | null>(null);
  const lastCheckedAtRef = useRef(0);
  const [watchingDocumentId, setWatchingDocumentId] = useState<number | null>(null);
  const [message, setMessage] = useState('');

  const closeWindow = useCallback(() => {
    try {
      windowRef.current?.close();
    } catch (_) {
      // 이미 닫혔거나 브라우저가 막으면 그대로 둔다.
    }
    windowRef.current = null;
  }, []);

  /** 서명창을 새 탭으로 열고 완료를 지켜보기 시작한다. */
  const open = useCallback((document: ClmDocumentDto) => {
    if (!document.signingUrl) return;
    windowRef.current = window.open(document.signingUrl, 'modusign-signing');
    setWatchingDocumentId(document.id);
    setMessage('서명을 마치면 이 화면이 자동으로 확인합니다.');
  }, []);

  const stop = useCallback(() => {
    setWatchingDocumentId(null);
    closeWindow();
  }, [closeWindow]);

  useEffect(() => {
    if (!watchingDocumentId) return;
    const startedAt = Date.now();
    let stopped = false;

    // 모두싸인 API에 호출 제한이 있어 최소 간격을 둔다.
    const check = async () => {
      if (stopped || Date.now() - lastCheckedAtRef.current < 2500) return;
      lastCheckedAtRef.current = Date.now();
      try {
        const fresh = await fetchClmDocument(watchingDocumentId);
        if (stopped || fresh.status !== 'SIGNED') return;
        stopped = true;
        setWatchingDocumentId(null);
        closeWindow();
        setMessage('전자서명이 완료되었습니다.');
        onSigned?.();
      } catch (_) {
        // 한 번 실패해도 다음 신호에 다시 확인한다.
      }
    };

    // 서명을 끝낸 사람은 창을 닫거나 이 화면으로 돌아온다.
    // 그 두 순간을 신호로 삼아 바로 확인한다. 창이 닫혔는지 보는 건 통신이 아니라 공짜다.
    const watchWindow = window.setInterval(() => {
      if (windowRef.current?.closed) {
        windowRef.current = null;
        void check();
      }
    }, 700);

    const onFocus = () => void check();
    window.addEventListener('focus', onFocus);
    document.addEventListener('visibilitychange', onFocus);

    // 아무 신호가 없어도 놓치지 않도록 받쳐 주는 주기 확인. 3분이면 멈춘다.
    const poll = window.setInterval(() => {
      if (Date.now() - startedAt > 3 * 60 * 1000) {
        window.clearInterval(poll);
        setWatchingDocumentId(null);
        return;
      }
      void check();
    }, 6000);

    return () => {
      stopped = true;
      window.clearInterval(watchWindow);
      window.clearInterval(poll);
      window.removeEventListener('focus', onFocus);
      document.removeEventListener('visibilitychange', onFocus);
    };
  }, [watchingDocumentId, closeWindow, onSigned]);

  return { open, stop, message, setMessage, isWatching: watchingDocumentId !== null };
}
