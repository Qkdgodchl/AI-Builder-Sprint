import { useEffect, useState } from 'react';
import { fetchGoodNews, type GoodNewsResponse } from '../../services/newsApi';

const regions = ['전국', '서울', '부산', '대구', '광주', '인천', '대전', '울산', '경기', '강원', '제주'];

const formatDate = (value: string) => {
  if (!value) return '최근';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric' }).format(date);
};

export function GoodNewsPage() {
  const [region, setRegion] = useState('전국');
  const [feed, setFeed] = useState<GoodNewsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    fetchGoodNews(region, 10)
      .then((response) => active && setFeed(response))
      .catch((reason) => active && setError(
        reason instanceof Error ? reason.message : '소식을 불러오지 못했습니다.',
      ))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [region, reloadKey]);

  return (
    <article className="good-news-page">
      <header className="good-news-heading">
        <div>
          <span>LOCAL GOOD NEWS</span>
          <h2>우리 동네의 선한 움직임</h2>
          <p>지역에서 이어지는 봉사·기부·나눔 소식을 출처와 함께 모았습니다.</p>
        </div>
        <div className="news-source-note">
          <strong>기사 원문 연결</strong>
          <span>제목을 누르면 해당 언론사의 원문으로 이동합니다.</span>
        </div>
      </header>

      <nav className="news-region-filter" aria-label="뉴스 지역 선택">
        {regions.map((item) => (
          <button
            type="button"
            key={item}
            className={region === item ? 'active' : ''}
            onClick={() => setRegion(item)}
          >
            {item}
          </button>
        ))}
      </nav>

      {feed?.message && <div className="news-feed-notice">{feed.message}</div>}
      {error && (
        <div className="news-empty-state">
          <strong>소식을 연결하지 못했습니다.</strong>
          <p>{error}</p>
          <button type="button" onClick={() => setReloadKey((key) => key + 1)}>다시 불러오기</button>
        </div>
      )}
      {loading ? (
        <div className="news-loading" aria-live="polite">{region} 선행 소식을 모으는 중입니다…</div>
      ) : !error && feed?.items.length === 0 ? (
        <div className="news-empty-state">
          <strong>최근 등록된 소식이 없습니다.</strong>
          <p>다른 지역을 선택하거나 잠시 후 다시 확인해주세요.</p>
        </div>
      ) : (
        <section className="news-editorial-list">
          {feed?.items.map((item, index) => (
            <a key={item.id} href={item.url} target="_blank" rel="noreferrer">
              <span className="news-index">{String(index + 1).padStart(2, '0')}</span>
              <div className="news-copy">
                <div><span>{item.region}</span><span>{item.source}</span></div>
                <h3>{item.title}</h3>
                {item.summary && <p>{item.summary}</p>}
              </div>
              <div className="news-date"><span>{formatDate(item.publishedAt)}</span><strong>↗</strong></div>
            </a>
          ))}
        </section>
      )}

      {feed && (
        <footer className="news-feed-footer">
          <span>{feed.provider}에서 최근 30일 기사를 수집합니다.</span>
          {feed.stale && <strong>마지막 수집 데이터</strong>}
        </footer>
      )}
    </article>
  );
}
