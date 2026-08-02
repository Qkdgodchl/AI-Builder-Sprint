import React, { useCallback, useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import type { SessionUser } from '../../types';
import {
  createConnectRequest,
  fetchConnectRequests,
  handleConnectRequest,
  toggleConnectSupport,
  type ConnectRequestItem,
} from '../../services/connectApi';
import {
  fetchManagedOpportunities,
  fetchManagedOrganizations,
  type ManagedOpportunity,
} from '../../services/managementApi';

interface ConnectPageProps {
  currentUser: SessionUser | null;
  onRequireLogin: (message: string) => void;
  showToast: (message: string) => void;
}

/** AI 도크에서 넘어올 때 채워 오는 초안. */
export interface ConnectDraft {
  title: string;
  content: string;
  category?: string;
  region?: string;
}

const CATEGORY_TABS = [
  { value: '', label: '전체' },
  { value: 'VOLUNTEER', label: '봉사' },
  { value: 'DONATION', label: '기부' },
] as const;

const STATUS_LABEL: Record<string, string> = {
  OPEN: '센터 응답 대기',
  REVIEWING: '센터 검토 중',
  FULFILLED: '프로그램 개설 완료',
};

const formatDate = (value: string) => {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric' }).format(date);
};

export const ConnectPage: React.FC<ConnectPageProps> = ({
  currentUser,
  onRequireLogin,
  showToast,
}) => {
  const navigate = useNavigate();
  const location = useLocation();
  const draft = (location.state as { connectDraft?: ConnectDraft } | null)?.connectDraft;

  const [category, setCategory] = useState('');
  const [requests, setRequests] = useState<ConnectRequestItem[] | null>(null);
  const [error, setError] = useState('');
  const [isWriting, setIsWriting] = useState(false);
  const [fromAi, setFromAi] = useState(false);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [formCategory, setFormCategory] = useState('VOLUNTEER');
  const [region, setRegion] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const isManager = currentUser?.role === 'CENTER_MANAGER';
  // 요청을 맡은 뒤 실제로 연 프로그램을 고르게 하려고 내 센터의 프로그램을 미리 받아 둔다.
  const [myOpportunities, setMyOpportunities] = useState<ManagedOpportunity[]>([]);
  const [linkingId, setLinkingId] = useState('');
  const [selectedOpportunity, setSelectedOpportunity] = useState('');

  useEffect(() => {
    if (!isManager) return;
    fetchManagedOrganizations()
      .then((organizations) =>
        organizations.length ? fetchManagedOpportunities(organizations[0].id) : [],
      )
      .then(setMyOpportunities)
      .catch(() => setMyOpportunities([]));
  }, [isManager]);

  const load = useCallback(() => {
    setError('');
    fetchConnectRequests(category || undefined)
      .then(setRequests)
      .catch((reason) => {
        setRequests([]);
        setError(reason instanceof Error ? reason.message : '요청을 불러오지 못했습니다.');
      });
  }, [category]);

  useEffect(() => {
    load();
  }, [load]);

  /*
   * 초안은 화면이 처음 그려질 때만이 아니라 넘어올 때마다 반영해야 한다.
   * 이미 이 화면에 있는 상태에서 AI가 초안을 넘기면 useState 초깃값은 다시 읽히지
   * 않아, 새로고침해야만 폼이 채워지는 문제가 있었다.
   * 한 번 쓰고 나면 state를 비워 새로고침 시 되살아나지 않게 한다.
   */
  useEffect(() => {
    if (!draft) return;
    setTitle(draft.title ?? '');
    setContent(draft.content ?? '');
    setFormCategory(draft.category ?? 'VOLUNTEER');
    setRegion(draft.region ?? '');
    setFromAi(true);
    setIsWriting(true);
    navigate(location.pathname, { replace: true, state: null });
  }, [draft, location.pathname, navigate]);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!currentUser) {
      onRequireLogin('로그인이 필요합니다. 로그인 후 요청을 남길 수 있어요.');
      return;
    }
    setSubmitting(true);
    try {
      await createConnectRequest({
        title: title.trim(),
        content: content.trim(),
        category: formCategory,
        region: region.trim() || undefined,
        origin: fromAi ? 'AI' : 'DIRECT',
      });
      setTitle('');
      setContent('');
      setRegion('');
      setFromAi(false);
      setIsWriting(false);
      showToast('센터에 요청을 전달했습니다.');
      load();
    } catch (reason) {
      showToast(reason instanceof Error ? reason.message : '요청을 남기지 못했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const support = async (item: ConnectRequestItem) => {
    if (!currentUser) {
      onRequireLogin('로그인이 필요합니다. 로그인 후 참여할 수 있어요.');
      return;
    }
    try {
      const updated = await toggleConnectSupport(item.publicId);
      setRequests((previous) =>
        (previous ?? []).map((row) => (row.publicId === updated.publicId ? updated : row)),
      );
    } catch (reason) {
      showToast(reason instanceof Error ? reason.message : '처리하지 못했습니다.');
    }
  };

  const claim = async (item: ConnectRequestItem) => {
    const message = window.prompt('요청을 남긴 분에게 전할 말을 적어주세요.', '검토 후 프로그램 개설을 준비하겠습니다.');
    if (message === null) return;
    try {
      const updated = await handleConnectRequest(item.publicId, { message });
      setRequests((previous) =>
        (previous ?? []).map((row) => (row.publicId === updated.publicId ? updated : row)),
      );
      showToast('요청을 맡았습니다. 프로그램을 열면 연결해 주세요.');
    } catch (reason) {
      // 다른 센터가 먼저 맡았을 수 있으니 목록을 다시 읽어 최신 상태로 맞춘다.
      showToast(reason instanceof Error ? reason.message : '처리하지 못했습니다.');
      load();
    }
  };

  /** 맡은 요청을 실제로 연 프로그램과 이어 붙여 완결한다. */
  const linkOpportunity = async (item: ConnectRequestItem) => {
    if (!selectedOpportunity) {
      showToast('연결할 프로그램을 골라주세요.');
      return;
    }
    try {
      const updated = await handleConnectRequest(item.publicId, {
        opportunityId: Number(selectedOpportunity),
        message: '요청해 주신 활동으로 프로그램을 열었습니다. 신청해 주세요!',
      });
      setRequests((previous) =>
        (previous ?? []).map((row) => (row.publicId === updated.publicId ? updated : row)),
      );
      setLinkingId('');
      setSelectedOpportunity('');
      showToast('프로그램을 연결했습니다.');
    } catch (reason) {
      showToast(reason instanceof Error ? reason.message : '연결하지 못했습니다.');
      load();
    }
  };

  return (
    <article className="connect-page">
      <header className="connect-heading">
        <div>
          <span>CONNECT</span>
          <h2>하고 싶은 선행, 잇다가 잇습니다</h2>
          <p>
            찾는 활동이 아직 등록되어 있지 않다면 여기에 남겨주세요.
            같은 마음이 모이면 센터가 프로그램을 새로 엽니다.
          </p>
        </div>
        <button type="button" className="connect-write-open" onClick={() => setIsWriting((v) => !v)}>
          {isWriting ? '닫기' : '원하는 활동 남기기'}
        </button>
      </header>

      {isWriting && (
        <form className="connect-form" onSubmit={submit}>
          <div className="connect-form-inner">
            {fromAi && (
              <p className="connect-form-origin">
                AI와 나눈 대화를 그대로 옮겨 왔습니다. 내용을 다듬어 보내주세요.
              </p>
            )}
          <label>
            어떤 활동을 원하시나요
            <small>한 줄로 적으면 센터가 알아보기 쉬워요.</small>
            <input
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              placeholder="예: 주말에 유기견 목욕 봉사를 하고 싶어요"
              maxLength={120}
              required
            />
          </label>
          <label>
            자세한 내용
            <small>언제, 어디서, 얼마나 자주 참여할 수 있는지 적어주세요.</small>
            <textarea
              value={content}
              onChange={(event) => setContent(event.target.value)}
              placeholder="어떤 활동인지, 얼마나 자주 참여할 수 있는지 적어주시면 센터가 준비하기 쉬워요."
              rows={4}
              required
            />
          </label>
          <div className="connect-form-row">
            <label>
              구분
              <select value={formCategory} onChange={(event) => setFormCategory(event.target.value)}>
                <option value="VOLUNTEER">봉사</option>
                <option value="DONATION">기부</option>
              </select>
            </label>
            <label>
              희망 지역
              <input
                value={region}
                onChange={(event) => setRegion(event.target.value)}
                placeholder="예: 부산 사하구"
              />
            </label>
          </div>
          <div className="connect-form-actions">
            <button type="submit" disabled={submitting}>
              {submitting ? '전달하는 중…' : '센터에 요청 전달하기'}
            </button>
            <button type="button" onClick={() => setIsWriting(false)}>취소</button>
          </div>
          </div>
        </form>
      )}

      <nav className="connect-filter" aria-label="요청 구분 선택">
        {CATEGORY_TABS.map((tab) => (
          <button
            key={tab.value || 'all'}
            type="button"
            className={category === tab.value ? 'active' : ''}
            onClick={() => setCategory(tab.value)}
          >
            {tab.label}
          </button>
        ))}
      </nav>

      {error && <div className="connect-empty">{error}</div>}

      {requests === null ? (
        <div className="connect-empty">요청을 불러오는 중입니다…</div>
      ) : requests.length === 0 ? (
        <div className="connect-empty">
          <strong>아직 남겨진 요청이 없습니다.</strong>
          <p>원하는 활동을 처음으로 남겨보세요.</p>
        </div>
      ) : (
        <section className="connect-list">
          {requests.map((item) => (
            <article key={item.publicId} className={`connect-row status-${item.status.toLowerCase()}`}>
              <div className="connect-row-main">
                <div className="connect-row-labels">
                  <span className="connect-badge">{item.category === 'VOLUNTEER' ? '봉사' : '기부'}</span>
                  {item.origin === 'AI' && <span className="connect-badge is-ai">AI 대화에서</span>}
                  <span className="connect-status">{STATUS_LABEL[item.status] ?? item.status}</span>
                </div>

                <h4>{item.title}</h4>
                <p>{item.content}</p>

                <div className="connect-row-meta">
                  <strong>{item.requesterNickname}</strong>
                  <span>·</span>
                  <span>{formatDate(item.createdAt)}</span>
                  {item.region && (
                    <>
                      <span>·</span>
                      <span>{item.region}</span>
                    </>
                  )}
                </div>

                {linkingId === item.publicId && (
                  <div className="connect-link-panel">
                    <strong>어떤 프로그램으로 열었나요?</strong>
                    {myOpportunities.length === 0 ? (
                      <p>
                        아직 등록한 프로그램이 없습니다. 내 센터에서 프로그램을 먼저 등록해 주세요.
                      </p>
                    ) : (
                      <div className="connect-link-row">
                        <select
                          value={selectedOpportunity}
                          onChange={(event) => setSelectedOpportunity(event.target.value)}
                        >
                          <option value="">프로그램 선택</option>
                          {myOpportunities.map((opportunity) => (
                            <option key={opportunity.id} value={opportunity.id}>
                              {opportunity.title}
                            </option>
                          ))}
                        </select>
                        <button type="button" onClick={() => linkOpportunity(item)}>
                          연결하고 완료로 표시
                        </button>
                      </div>
                    )}
                  </div>
                )}

                {item.handledMessage && (
                  <div className="connect-reply">
                    <strong>{item.handledOrganizationName ?? '센터'}</strong>
                    <p>{item.handledMessage}</p>
                    {item.handledOpportunityId && (
                      <button
                        type="button"
                        onClick={() => navigate(`/volunteer/${item.handledOpportunityId}`)}
                      >
                        개설된 프로그램 보기 →
                      </button>
                    )}
                  </div>
                )}
              </div>

              <div className="connect-row-reaction">
                <button
                  type="button"
                  className={`connect-support${item.supportedByMe ? ' is-on' : ''}`}
                  onClick={() => support(item)}
                >
                  <span aria-hidden="true">🙌</span>
                  {item.supportCount}
                </button>
                <small>나도 원해요</small>
                {item.canHandle && item.status === 'OPEN' && (
                  <button type="button" className="connect-claim" onClick={() => claim(item)}>
                    이 요청 맡기
                  </button>
                )}
                {item.canHandle && item.status === 'REVIEWING' && (
                  <button
                    type="button"
                    className="connect-claim"
                    onClick={() => {
                      setLinkingId(linkingId === item.publicId ? '' : item.publicId);
                      setSelectedOpportunity('');
                    }}
                  >
                    {linkingId === item.publicId ? '취소' : '프로그램 연결'}
                  </button>
                )}
                {isManager && !item.canHandle && item.status === 'REVIEWING' && (
                  <small className="connect-other-center">다른 센터가 맡음</small>
                )}
              </div>
            </article>
          ))}
        </section>
      )}
    </article>
  );
};
