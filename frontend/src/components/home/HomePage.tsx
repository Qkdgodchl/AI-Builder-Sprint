import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { SessionUser, VolunteerItem } from '../../types';
import { fetchVolunteers } from '../../services/volunteerApi';
import { fetchGoodNews, type GoodNewsItem } from '../../services/newsApi';
import { fetchPlatformStats, type PlatformStats } from '../../services/statsApi';

interface HomePageProps {
  currentUser: SessionUser | null;
  onLogin: () => void;
  onOpenAi: () => void;
}

const categoryLabel = (category: VolunteerItem['category']) =>
  category === 'VOLUNTEER' ? '봉사' : category === 'LEGACY' ? '유산기부' : '기부';

/**
 * 카탈로그 상단 탭과 같은 구분. 홈에서 바로 해당 목록으로 넘긴다.
 * categories는 API가 내려주는 실제 분류값이라 건수 집계에 그대로 쓴다.
 */
const CATEGORY_TILES = [
  { name: '봉사', icon: '🤝', hint: '현장에서 함께합니다', filter: 'VOLUNTEER', sub: '', categories: ['VOLUNTEER'] },
  { name: '기부', icon: '💛', hint: '금액으로 잇습니다', filter: 'DONATION', sub: 'GENERAL', categories: ['GENERAL'] },
  { name: '고향사랑기부', icon: '🌾', hint: '세액공제·답례품', filter: 'HOMETOWN', sub: '', categories: ['HOMETOWN'] },
  { name: '유산기부', icon: '🕊️', hint: '오래 남는 약속', filter: 'DONATION', sub: 'LEGACY', categories: ['LEGACY'] },
  { name: '문화유산', icon: '⛩️', hint: '함께 지키는 유산', filter: 'DONATION', sub: 'HERITAGE', categories: ['HERITAGE'] },
  { name: '유네스코', icon: '🌏', hint: '국경 넘는 후원', filter: 'DONATION', sub: 'UNESCO', categories: ['UNESCO'] },
] as const;

export function HomePage({ currentUser, onLogin, onOpenAi }: HomePageProps) {
  const navigate = useNavigate();
  const [opportunities, setOpportunities] = useState<VolunteerItem[]>([]);
  const [news, setNews] = useState<GoodNewsItem[]>([]);
  const [stats, setStats] = useState<PlatformStats | null>(null);

  useEffect(() => {
    Promise.all([fetchVolunteers(), fetchGoodNews('부산', 4), fetchPlatformStats()])
      .then(([items, feed, summary]) => {
        setOpportunities(items);
        setNews(feed.items);
        setStats(summary);
      })
      .catch(() => {
        setOpportunities([]);
        setNews([]);
      });
  }, []);

  // 모금 목표가 있는 기부를 먼저 보여주면 진행률이 드러나 참여를 유도하기 좋다.
  const featured = useMemo(() => {
    const funded = opportunities.filter((item) => (item.targetAmount ?? 0) > 0);
    const rest = opportunities.filter((item) => !((item.targetAmount ?? 0) > 0));
    return [...funded, ...rest].slice(0, 6);
  }, [opportunities]);

  const categoryCounts = useMemo(() => {
    const counts: Record<string, number> = {};
    opportunities.forEach((item) => {
      const key = item.category || 'VOLUNTEER';
      counts[key] = (counts[key] ?? 0) + 1;
    });
    return counts;
  }, [opportunities]);

  // 홈 타일과 카탈로그 탭을 1:1로 맞춰, 누른 구분이 그대로 열리게 한다.
  const goCatalog = (filter?: string, sub?: string) => {
    if (!filter) return navigate('/volunteer');
    const params = new URLSearchParams({ type: filter });
    if (sub) params.set('sub', sub);
    return navigate(`/volunteer?${params.toString()}`);
  };

  return (
    <article className="service-home">
      <section className="home-hero">
        <div className="home-hero-copy">
          <span className="home-kicker">AI-POWERED GOOD ACTION PLATFORM</span>
          <h2>선한 마음을<br />약정으로 이어갑니다.</h2>
          <p>
            하고 싶은 일을 말하면 AI가 봉사·기부 약정을 정리하고,
            전자서명부터 기관의 사후 관리까지 한 흐름으로 연결합니다.
          </p>
          {/* AI 진입은 화면 오른쪽 아래 도크와 하단 배너로 모았다. 히어로는 목록으로만 보낸다. */}
          <div className="home-hero-actions">
            <button type="button" onClick={() => goCatalog()}>선행 프로그램 찾기</button>
          </div>
          {!currentUser && (
            <button className="home-login-link" type="button" onClick={onLogin}>
              이미 참여 중이신가요? 로그인 →
            </button>
          )}

          {stats && (
            <dl className="home-hero-proof">
              <div>
                <dt>참여 회원</dt>
                <dd>{stats.activeMembers.toLocaleString()}명</dd>
              </div>
              <div>
                <dt>누적 봉사</dt>
                <dd>{stats.volunteerHours.toLocaleString()}시간</dd>
              </div>
              <div>
                <dt>전자서명 약정</dt>
                <dd>{stats.signedCommitments.toLocaleString()}건</dd>
              </div>
            </dl>
          )}
        </div>

        <aside className="home-hero-panel">
          <span>HOW IT WORKS</span>
          {[
            ['01', 'AI와 이야기하기', 'AI 메이트와 대화하듯 하고 싶은 일을 말합니다.'],
            ['02', 'AI가 약정 정리', '대화 내용을 확인 가능한 약정 항목으로 정리합니다.'],
            ['03', '서명하고 관리', '전자서명 후 나와 담당 센터가 상태를 관리합니다.'],
          ].map(([number, title, description]) => (
            <div key={number}>
              <strong>{number}</strong><h3>{title}</h3><p>{description}</p>
            </div>
          ))}
        </aside>
      </section>

      <section className="home-category-nav" aria-label="선행 구분 바로가기">
        {CATEGORY_TILES.map((tile) => {
          const count = tile.categories.reduce(
            (total, category) => total + (categoryCounts[category] ?? 0),
            0,
          );
          return (
            <button type="button" key={tile.name} onClick={() => goCatalog(tile.filter, tile.sub)}>
              <span className="home-category-icon" aria-hidden="true">{tile.icon}</span>
              <strong>{tile.name}</strong>
              <small>{tile.hint}</small>
              <em>{count}개 프로그램</em>
            </button>
          );
        })}
      </section>

      <section className="home-section">
        <div className="home-section-title">
          <div><span>OPEN NOW</span><h2>지금 참여할 수 있는 선행</h2></div>
          <button type="button" onClick={() => goCatalog()}>전체 프로그램 보기 →</button>
        </div>
        <div className="home-opportunity-grid">
          {featured.length > 0 ? featured.map((item) => {
            const target = item.targetAmount ?? 0;
            const current = item.currentAmount ?? 0;
            const percent = target > 0 ? Math.min(100, Math.round((current / target) * 100)) : null;
            return (
              <button type="button" key={item.id} onClick={() => navigate(`/volunteer/${item.id}`)}>
                <div className="home-card-meta">
                  <span className={`home-card-tag tag-${item.category?.toLowerCase() ?? 'volunteer'}`}>
                    {categoryLabel(item.category)}
                  </span>
                  <span>{item.location}</span>
                </div>
                <h3>{item.title}</h3>
                <p>{item.organizer}</p>
                {percent !== null ? (
                  <div className="home-card-progress">
                    <div className="home-card-progress-track">
                      <span style={{ width: `${percent}%` }} />
                    </div>
                    <div className="home-card-progress-meta">
                      <strong>{percent}%</strong>
                      <span>{current.toLocaleString()} / {target.toLocaleString()}원</span>
                    </div>
                  </div>
                ) : (
                  <div className="home-card-progress home-card-progress-empty">
                    <span>모집 중 · 상시 참여 가능</span>
                  </div>
                )}
                <strong className="home-card-cta">상세 보기 ↗</strong>
              </button>
            );
          }) : (
            <div className="home-content-empty">공개 중인 프로그램을 불러오는 중입니다.</div>
          )}
        </div>
      </section>

      <section className="home-section home-trust-section">
        <div className="home-section-title">
          <div><span>HOW WE KEEP IT SAFE</span><h2>약속이 기록으로 남는 방식</h2></div>
        </div>
        <div className="home-trust-strip">
          <div>
            <span>AI</span>
            <strong>대화로 정리하는 약정</strong>
            <p>AI 메이트와 나눈 대화를 금액·주기·대상 항목으로 정리합니다.</p>
          </div>
          <div>
            <span>SIGN</span>
            <strong>전자서명 생애주기</strong>
            <p>서명 요청부터 완료·보관까지 상태를 그대로 추적합니다.</p>
          </div>
          <div>
            <span>CLM</span>
            <strong>기관별 안전한 관리</strong>
            <p>역할과 소속을 확인한 담당자만 약정 문서를 열람합니다.</p>
          </div>
        </div>
      </section>

      <section className="home-section home-news-preview">
        <div className="home-section-title">
          <div><span>LOCAL GOOD NEWS</span><h2>부산에서 이어지는 좋은 소식</h2></div>
          <button type="button" onClick={() => navigate('/news')}>지역별 소식 보기 →</button>
        </div>
        <div className="home-news-list">
          {news.length > 0 ? news.map((item, index) => (
            <a href={item.url} target="_blank" rel="noreferrer" key={item.id}>
              <span>{String(index + 1).padStart(2, '0')}</span>
              <div><small>{item.source}</small><h3>{item.title}</h3></div>
              <strong>↗</strong>
            </a>
          )) : <div className="home-content-empty">지역 선행 소식을 연결하고 있습니다.</div>}
        </div>
      </section>

      <section className="home-ai-cta">
        <div>
          <span>ITDA AI MATE</span>
          <h2>무엇부터 해야 할지 모르겠다면?</h2>
          <p>지역과 관심사를 말하면 등록된 프로그램 중 맞는 활동을 찾아드립니다.</p>
        </div>
        <button type="button" onClick={onOpenAi}>AI 추천 시작하기 →</button>
      </section>
    </article>
  );
}
