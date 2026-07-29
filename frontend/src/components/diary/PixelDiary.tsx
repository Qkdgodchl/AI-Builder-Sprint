import React, { useEffect, useState } from 'react';
import type { PostItem } from '../../services/communityApi';
import { fetchPosts, createPost, likePost } from '../../services/communityApi';
import { playBeep } from '../../services/soundFx';

interface PixelDiaryProps {
  onAddDiary: (tempIncrease: number) => void;
  showToast: (message: string) => void;
}

export const PixelDiary: React.FC<PixelDiaryProps> = ({ onAddDiary, showToast }) => {
  const [posts, setPosts] = useState<PostItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [title, setTitle] = useState('');
  const [author, setAuthor] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('REVIEW');
  const [filterCategory, setFilterCategory] = useState('ALL');
  const [isWriteOpen, setIsWriteOpen] = useState(false);
  const [selectedPost, setSelectedPost] = useState<PostItem | null>(null);

  const loadPosts = async () => {
    setLoading(true);
    try {
      const data = await fetchPosts(filterCategory);
      setPosts(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to load community posts:', err);
      setPosts([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPosts();
  }, [filterCategory]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) {
      alert('게시글 제목과 내용을 모두 입력해 주세요!');
      return;
    }

    try {
      const created = await createPost({
        title: title.trim(),
        content: content.trim(),
        category,
      });

      if (created) {
        setPosts((prev) => [created, ...prev]);
      }
      onAddDiary(0.5);
      showToast(`📝 픽셀 커뮤니티 글이 등록되었습니다! 온기 +0.5°C 상승!`);
      playBeep(587, 0.15);

      setTitle('');
      setContent('');
      setAuthor('');
      setIsWriteOpen(false);
      loadPosts();
    } catch (err) {
      console.error(err);
      alert('게시글 등록 중 오류가 발생했습니다.');
    }
  };

  const handleLike = async (id: number, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    try {
      const updatedLike = await likePost(id);
      setPosts((prev) =>
        prev.map((p) =>
          p.id === id ? { ...p, likeCount: updatedLike.likeCount } : p
        )
      );
      if (selectedPost && selectedPost.id === id) {
        setSelectedPost((prev) => prev ? { ...prev, likeCount: updatedLike.likeCount } : null);
      }
      onAddDiary(0.1);
      playBeep(784, 0.1);
      showToast('❤️ 게시글에 응원 하트를 보냈습니다! (온기 +0.1°C)');
    } catch (err) {
      console.error(err);
    }
  };

  const handleCopyLink = () => {
    navigator.clipboard.writeText(window.location.href);
    playBeep(520, 0.1);
    showToast('🔗 게시글 링크가 클립보드에 복사되었습니다!');
  };

  const getAuthorName = (authorInfo: any) => {
    if (!authorInfo) return '부산 픽셀용사';
    if (typeof authorInfo === 'string') return authorInfo;
    return authorInfo.nickname || '부산 픽셀용사';
  };

  const getAuthorBadge = (authorInfo: any) => {
    if (typeof authorInfo === 'object' && authorInfo?.badge) {
      return authorInfo.badge;
    }
    return 'LV1_SEED';
  };

  const getBadgeColor = (badge: string) => {
    switch (badge) {
      case 'LV5_GUARDIAN': return { bg: '#9d4edd', name: '👑 LV5 온기 수호자' };
      case 'LV4_HERO': return { bg: '#ffb703', name: '⭐ LV4 픽셀 영웅' };
      case 'LV3_PIONEER': return { bg: '#2ec4b6', name: '⚡ LV3 선행 개척자' };
      case 'LV2_WARMTH': return { bg: '#ff70a6', name: '💖 LV2 온기 전파자' };
      default: return { bg: '#70e000', name: '🌱 LV1 싹틔움 용사' };
    }
  };

  const getCategoryLabel = (cat: string) => {
    switch (cat) {
      case 'REVIEW': return '📝 봉사 후기';
      case 'RECRUIT': return '🤝 동행 모집';
      case 'FREE':
      case 'GENERAL': return '💬 자율 수다';
      default: return '💬 자유 소통';
    }
  };

  const safePosts = Array.isArray(posts) ? posts : [];

  // [상세 보기 UI]
  if (selectedPost) {
    const likesCount = selectedPost.likeCount ?? (selectedPost as any).likes ?? 0;
    const viewsCount = selectedPost.viewCount ?? (selectedPost as any).views ?? 0;
    const textContent = selectedPost.content || selectedPost.contentSnippet || '';
    const createdDate = selectedPost.createdAt ? new Date(selectedPost.createdAt).toLocaleString('ko-KR') : '방금 전';
    const badgeInfo = getBadgeColor(getAuthorBadge(selectedPost.author));

    return (
      <article className="opportunity-detail" style={{ maxWidth: '880px', margin: '0 auto' }}>
        <div className="detail-back-nav">
          <button type="button" className="detail-back-button" onClick={() => setSelectedPost(null)}>
            ← 목록으로 돌아가기
          </button>
        </div>

        <header className="detail-hero">
          <div style={{ marginBottom: '10px' }}>
            <span className="opportunity-type" style={{ fontSize: '12px', padding: '4px 10px', borderRadius: '4px' }}>
              {getCategoryLabel(selectedPost.category)}
            </span>
          </div>
          <h2 style={{ fontSize: '24px', fontWeight: '900', color: '#111', lineHeight: 1.3, marginBottom: '12px' }}>
            {selectedPost.title}
          </h2>
          <div className="detail-inline-keywords" aria-label="관련 키워드">
            <span>#{getCategoryLabel(selectedPost.category).replace(/\s+/g, '')}</span>
            <span>#픽셀온기</span>
            <span>#{getAuthorName(selectedPost.author)}</span>
          </div>
        </header>

        <dl className="detail-facts">
          <div>
            <dt>작성자</dt>
            <dd>✍️ {getAuthorName(selectedPost.author)}</dd>
          </div>
          <div>
            <dt>픽셀 레벨 뱃지</dt>
            <dd>
              <span style={{ background: badgeInfo.bg, color: '#fff', fontSize: '10px', fontWeight: 'bold', padding: '3px 10px', borderRadius: '12px' }}>
                {badgeInfo.name}
              </span>
            </dd>
          </div>
          <div>
            <dt>작성 시각</dt>
            <dd>📅 {createdDate}</dd>
          </div>
          <div>
            <dt>조회수 및 하트</dt>
            <dd>👁️ {viewsCount} 회 · ❤️ {likesCount} 개</dd>
          </div>
        </dl>

        <section className="detail-section">
          <p className="detail-section-number">01</p>
          <div style={{ width: '100%' }}>
            <h3>이야기 본문</h3>
            
            {selectedPost.imageUrl ? (
              <div style={{ margin: '16px 0', borderRadius: '8px', overflow: 'hidden', border: '2px solid #111' }}>
                <img src={selectedPost.imageUrl} alt={selectedPost.title} style={{ width: '100%', maxHeight: '420px', objectFit: 'cover' }} />
              </div>
            ) : null}

            <div style={{ fontSize: '15px', color: '#222', lineHeight: 1.8, minHeight: '120px', whiteSpace: 'pre-line', marginTop: '12px', padding: '16px', background: '#faf0ca', borderRadius: '8px', border: '1px solid #e9ecef' }}>
              {textContent}
            </div>
          </div>
        </section>

        <section className="detail-section">
          <p className="detail-section-number">02</p>
          <div>
            <h3>선행 응원 및 소통 안내</h3>
            <p style={{ fontSize: '13px', color: '#555', lineHeight: 1.6 }}>
              따뜻한 봉사 후기와 이야기를 남겨주셔서 감사합니다. 응원 하트를 눌러 작성자에게 따뜻한 픽셀 온기를 전달해 보세요! (하트 전달 시 온기 +0.1°C)
            </p>
          </div>
        </section>

        <footer className="detail-apply-bar">
          <div>
            <span>COMMUNITY ACTION</span>
            <strong>{getAuthorName(selectedPost.author)} 님의 이야기를 응원하시겠어요?</strong>
          </div>
          <div style={{ display: 'flex', gap: '10px' }}>
            <button type="button" style={{ background: '#2ec4b6', padding: '10px 16px', fontSize: '12px' }} onClick={handleCopyLink}>
              🔗 공유 / 링크 복사
            </button>
            <button type="button" style={{ background: '#ff4d6d', padding: '10px 20px', fontSize: '13px', fontWeight: 'bold' }} onClick={() => handleLike(selectedPost.id)}>
              ❤️ 응원 하트 보내기 ({likesCount})
            </button>
          </div>
        </footer>
      </article>
    );
  }

  return (
    <section className="opportunity-catalog" style={{ maxWidth: '960px', margin: '0 auto' }}>
      {/* Primary Category Nav */}
      <nav className="opportunity-primary-nav" aria-label="커뮤니티 분류">
        {['ALL', 'REVIEW', 'RECRUIT', 'FREE'].map((cat) => (
          <button
            key={cat}
            type="button"
            className={filterCategory === cat ? 'active' : ''}
            onClick={() => setFilterCategory(cat)}
          >
            {cat === 'ALL' ? '전체 소통' : cat === 'REVIEW' ? '봉사 후기' : cat === 'RECRUIT' ? '동행 모집' : '자율 수다'}
          </button>
        ))}

        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button
            type="button"
            className="opportunity-action"
            style={{ background: isWriteOpen ? '#444' : 'var(--pixel-primary, #ff3b30)', padding: '6px 14px', fontSize: '12px' }}
            onClick={() => setIsWriteOpen(!isWriteOpen)}
          >
            {isWriteOpen ? '✖️ 작성 닫기' : '📝 새 글 작성하기'}
          </button>
          <span className="opportunity-count" aria-live="polite">
            총 {safePosts.length}개 이야기
          </span>
        </div>
      </nav>

      {/* 글쓰기 Bento Box Form */}
      {isWriteOpen && (
        <div style={{ marginBottom: '24px', padding: '20px', background: '#faf0ca', borderRadius: '12px', border: '2px solid #111' }}>
          <div style={{ fontSize: '16px', fontWeight: '800', marginBottom: '14px', color: '#1a1a24', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span>💬</span> 픽셀 커뮤니티 새 이야기 작성
          </div>

          <form onSubmit={handleCreate}>
            <div style={{ display: 'flex', gap: '10px', marginBottom: '12px', flexWrap: 'wrap' }}>
              <input
                type="text"
                className="pixel-input"
                style={{ flex: 3, minWidth: '220px', padding: '10px' }}
                placeholder="📌 게시글 제목 (예: 해운대 플로깅 봉사 후기 올립니다!)"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
              />
              <input
                type="text"
                className="pixel-input"
                style={{ flex: 1, minWidth: '130px', padding: '10px' }}
                placeholder="작성자 닉네임"
                value={author}
                onChange={(e) => setAuthor(e.target.value)}
              />
              <select
                className="pixel-input"
                style={{ padding: '10px', fontWeight: 'bold' }}
                value={category}
                onChange={(e) => setCategory(e.target.value)}
              >
                <option value="REVIEW">📝 봉사 후기</option>
                <option value="RECRUIT">🤝 동행 모집</option>
                <option value="FREE">💬 자율 수다</option>
              </select>
            </div>

            <textarea
              className="pixel-input"
              style={{ width: '100%', height: '110px', marginBottom: '14px', resize: 'none', padding: '12px', lineHeight: 1.5 }}
              placeholder="따뜻한 봉사 후기, 함께할 동행 모집, 선행에 관한 이야기를 나눠보세요!"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              required
            />

            <div style={{ textAlign: 'right' }}>
              <button type="submit" className="opportunity-action" style={{ background: '#ff3b30', fontSize: '13px', padding: '10px 20px' }}>
                🚀 게시글 게시 (온기 +0.5°C)
              </button>
            </div>
          </form>
        </div>
      )}

      {/* [정밀 교정] 커뮤니티 매거진 테이블 목록 */}
      {loading ? (
        <div className="opportunity-state">커뮤니티 이야기를 불러오는 중입니다...</div>
      ) : safePosts.length === 0 ? (
        <div className="opportunity-state">등록된 커뮤니티 이야기 피드가 없습니다. 첫 번째 글을 작성해보세요!</div>
      ) : (
        <div className="opportunity-table" role="table" aria-label="커뮤니티 피드">
          <div className="opportunity-table-head" role="row">
            <span role="columnheader">분류</span>
            <span role="columnheader">이야기 제목 및 내용 미리보기</span>
            <span role="columnheader">작성자 뱃지</span>
            <span role="columnheader">조회 / 하트</span>
            <span role="columnheader">작성일</span>
            <span role="columnheader" aria-label="상세 보기 및 액션" />
          </div>

          {safePosts.map((post, idx) => {
            const likesCount = post.likeCount ?? (post as any).likes ?? 0;
            const viewsCount = post.viewCount ?? (post as any).views ?? 0;
            const snippetText = post.contentSnippet || post.content || '';
            const createdDate = post.createdAt ? new Date(post.createdAt).toLocaleDateString('ko-KR') : '방금 전';
            const badgeInfo = getBadgeColor(getAuthorBadge(post.author));

            return (
              <article
                className="opportunity-row"
                role="row"
                key={`${post.id}-${idx}`}
                style={{ cursor: 'pointer' }}
                onClick={() => setSelectedPost(post)}
              >
                {/* Col 1: 분류 */}
                <span className="opportunity-type" role="cell" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                  {getCategoryLabel(post.category)}
                </span>

                {/* Col 2: 제목 & 내용 미리보기 */}
                <div className="opportunity-program" role="cell">
                  <strong style={{ fontSize: '15px', color: '#111' }}>{post.title}</strong>
                  <span style={{ fontSize: '12px', color: '#666', marginTop: '2px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {snippetText}
                  </span>
                </div>

                {/* Col 3: 작성자 닉네임 & 레벨 뱃지 (수직 2줄 정돈) */}
                <div className="opportunity-keywords" role="cell" style={{ display: 'flex', flexDirection: 'column', gap: '4px', alignItems: 'flex-start' }}>
                  <span style={{ fontSize: '12px', fontWeight: 'bold', color: '#333', background: '#f0f0f0', padding: '2px 8px', borderRadius: '12px', border: '1px solid #ddd' }}>
                    {getAuthorName(post.author)}
                  </span>
                  <span style={{ background: badgeInfo.bg, color: '#fff', fontSize: '9px', fontWeight: 'bold', padding: '2px 6px', borderRadius: '4px' }}>
                    {badgeInfo.name}
                  </span>
                </div>

                {/* Col 4: 조회수 / 하트 */}
                <span className="opportunity-area" role="cell" style={{ fontSize: '12px', color: '#444' }}>
                  👁️ {viewsCount} · ❤️ {likesCount}
                </span>

                {/* Col 5: 작성일 */}
                <span className="opportunity-status" role="cell" style={{ fontSize: '12px', color: '#2b9348', fontWeight: 'bold' }}>
                  {createdDate}
                </span>

                {/* Col 6: 상세보기 & 응원 액션 버튼 */}
                <div style={{ display: 'flex', gap: '6px', alignItems: 'center' }} role="cell">
                  <button
                    type="button"
                    className="opportunity-action"
                    style={{ background: '#ffe5ec', borderColor: '#ff4d6d', color: '#c9184a', fontSize: '11px', padding: '6px 12px', borderRadius: '16px' }}
                    onClick={(e) => handleLike(post.id, e)}
                  >
                    ❤️ {likesCount}
                  </button>
                  <button
                    type="button"
                    className="opportunity-action"
                    style={{ fontSize: '11px', padding: '6px 14px', borderRadius: '16px' }}
                    onClick={() => setSelectedPost(post)}
                  >
                    상세보기
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
};
