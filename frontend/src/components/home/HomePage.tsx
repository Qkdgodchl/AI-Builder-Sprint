import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { SessionUser, VolunteerItem } from '../../types';
import { fetchVolunteers } from '../../services/volunteerApi';
import { fetchGoodNews, type GoodNewsItem } from '../../services/newsApi';

interface HomePageProps {
  currentUser: SessionUser | null;
  onLogin: () => void;
}

const categoryLabel = (category: VolunteerItem['category']) =>
  category === 'VOLUNTEER' ? '봉사' : category === 'LEGACY' ? '유산기부' : '기부';

export function HomePage({ currentUser, onLogin }: HomePageProps) {
  const navigate = useNavigate();
  const [opportunities, setOpportunities] = useState<VolunteerItem[]>([]);
  const [news, setNews] = useState<GoodNewsItem[]>([]);

  useEffect(() => {
    Promise.all([fetchVolunteers(), fetchGoodNews('부산', 3)])
      .then(([items, feed]) => {
        setOpportunities(items.slice(0, 3));
        setNews(feed.items);
      })
      .catch(() => {
        setOpportunities([]);
        setNews([]);
      });
  }, []);

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
          <div className="home-hero-actions">
            <button type="button" onClick={() => navigate('/volunteer')}>선행 프로그램 찾기</button>
            <button type="button" className="secondary" onClick={() => navigate('/ai')}>AI에게 추천받기</button>
          </div>
          {!currentUser && (
            <button className="home-login-link" type="button" onClick={onLogin}>
              이미 참여 중이신가요? 로그인 →
            </button>
          )}
        </div>
        <aside className="home-hero-panel">
          <span>HOW IT WORKS</span>
          {[
            ['01', '마음 말하기', '원하는 봉사·기부 내용을 자연어로 적습니다.'],
            ['02', 'AI 약정 정리', '조건을 확인 가능한 항목으로 구조화합니다.'],
            ['03', '서명하고 관리', '전자서명 후 나와 담당 센터가 상태를 관리합니다.'],
          ].map(([number, title, description]) => (
            <div key={number}>
              <strong>{number}</strong><h3>{title}</h3><p>{description}</p>
            </div>
          ))}
        </aside>
      </section>

      <section className="home-section">
        <div className="home-section-title">
          <div><span>OPEN NOW</span><h2>지금 참여할 수 있는 선행</h2></div>
          <button type="button" onClick={() => navigate('/volunteer')}>전체 프로그램 보기 →</button>
        </div>
        <div className="home-opportunity-grid">
          {opportunities.length > 0 ? opportunities.map((item, index) => (
            <button type="button" key={item.id} onClick={() => navigate(`/volunteer/${item.id}`)}>
              <span className="home-card-number">0{index + 1}</span>
              <div className="home-card-meta"><span>{categoryLabel(item.category)}</span><span>{item.location}</span></div>
              <h3>{item.title}</h3>
              <p>{item.organizer}</p>
              <strong>상세 보기 ↗</strong>
            </button>
          )) : (
            <div className="home-content-empty">공개 중인 프로그램을 불러오는 중입니다.</div>
          )}
        </div>
      </section>

      <section className="home-trust-strip">
        <div><span>AI</span><strong>자연어 약정 구조화</strong><p>Upstage Solar 기반</p></div>
        <div><span>SIGN</span><strong>전자서명 생애주기</strong><p>요청·서명·완료 추적</p></div>
        <div><span>CLM</span><strong>기관별 안전한 관리</strong><p>역할·소속 기반 문서 접근</p></div>
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
        <div><span>PIXEL AI MATE</span><h2>무엇부터 해야 할지 모르겠다면?</h2><p>지역과 관심사를 말하면 등록된 프로그램 중 맞는 활동을 찾아드립니다.</p></div>
        <button type="button" onClick={() => navigate('/ai')}>AI 추천 시작하기 →</button>
      </section>
    </article>
  );
}
