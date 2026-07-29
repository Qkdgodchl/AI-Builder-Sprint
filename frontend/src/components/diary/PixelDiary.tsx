import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { PostItem } from '../../services/communityApi';
import { fetchPosts, createPost, likePost, deletePost } from '../../services/communityApi';
import { playBeep } from '../../services/soundFx';

interface PixelDiaryProps {
  onAddDiary: (tempIncrease: number) => void;
  showToast: (message: string) => void;
}

export const PixelDiary: React.FC<PixelDiaryProps> = ({ onAddDiary, showToast }) => {
  const navigate = useNavigate();
  const { id: urlPostId } = useParams<{ id?: string }>();

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

  useEffect(() => {
    if (urlPostId && posts.length > 0) {
      const targetId = Number(urlPostId);
      const found = posts.find((p) => p.id === targetId);
      if (found) {
        setSelectedPost(found);
      }
    } else if (!urlPostId) {
      setSelectedPost(null);
    }
  }, [urlPostId, posts]);

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
        author: author.trim() || '부산 픽셀용사',
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

  const handleDeletePost = async (id: number) => {
    if (!window.confirm('정말로 이 커뮤니티 이야기를 삭제하시겠습니까? (삭제 후 복구할 수 없습니다)')) {
      return;
    }

    try {
      const success = await deletePost(id);
      if (success) {
        showToast('🗑️ 커뮤니티 게시글이 성공적으로 삭제되었습니다.');
        playBeep(330, 0.15);
        navigate('/community');
        loadPosts();
      } else {
        alert('게시글 삭제에 실패했습니다.');
      }
    } catch (err) {
      console.error(err);
      alert('게시글 삭제 처리 중 오류가 발생했습니다.');
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
    const createdDate = selectedPost.createdAt ? new Date(selectedPost.createdAt).toLocaleDateString('ko-KR') : '방금 전';
    const badgeInfo = getBadgeColor(getAuthorBadge(selectedPost.author));

    return (
      <article className="opportunity-detail">
        <div className="detail-back-nav">
          <button type="button" className="detail-back-button" onClick={() => navigate('/community')}>
            목록으로 돌아가기
          </button>
          <button
            type="button"
            className="detail-back-button"
            style={{ color: '#ff3b30', fontWeight: 'bold' }}
            onClick={() => handleDeletePost(selectedPost.id)}
          >
            🗑️ 게시글 삭제
          </button>
        </div>

        <header className="detail-hero">
          <h2>{selectedPost.title}</h2>
          <p>{getCategoryLabel(selectedPost.category)} · 따뜻한 픽셀 케어 커뮤니티 선행 소통 이야기입니다.</p>
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
            <dd>{badgeInfo.name}</dd>
          </div>
          <div>
            <dt>작성일</dt>
            <dd>📅 {createdDate}</dd>
          </div>
          <div>
            <dt>조회수 / 하트</dt>
            <dd>👁️ {viewsCount} 회 · ❤️ {likesCount} 개</dd>
          </div>
        </dl>

        <section className="detail-section">
          <p className="detail-section-number">01</p>
          <div>
            <h3>이야기 본문</h3>
            
            {selectedPost.imageUrl ? (
              <div style={{ margin: '16px 0', borderRadius: '8px', overflow: 'hidden', border: '1px solid var(--pc-dark)' }}>
                <img src={selectedPost.imageUrl} alt={selectedPost.title} style={{ width: '100%', maxHeight: '420px', objectFit: 'cover' }} />
              </div>
            ) : null}

            <p style={{ whiteSpace: 'pre-line', fontSize: '15px', lineHeight: 1.7, color: '#333' }}>
              {textContent}
            </p>
          </div>
        </section>

        <section className="detail-section">
          <p className="detail-section-number">02</p>
          <div>
            <h3>선행 응원 및 안내사항</h3>
            <ul>
              <li>따뜻한 봉사 후기와 소중한 선행 이야기를 나눠주셔서 감사합니다.</li>
              <li>하단 응원 버튼을 누르면 작성자에게 픽셀 온기 +0.1°C가 전달됩니다.</li>
              <li>게시글 링크를 복사하여 카카오톡이나 SNS로 이웃들과 공유해 보세요.</li>
            </ul>
          </div>
        </section>

        <footer className="detail-apply-bar">
          <div>
            <span>COMMUNITY ACTION</span>
            <strong>{getAuthorName(selectedPost.author)} 님의 따뜻한 이야기를 응원하시겠어요?</strong>
          </div>
          <div style={{ display: 'flex', gap: '10px' }}>
            <button type="button" style={{ background: '#2ec4b6' }} onClick={handleCopyLink}>
              🔗 공유 / 링크 복사
            </button>
            <button type="button" onClick={() => handleLike(selectedPost.id)}>
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

      {/* 헤더 텍스트 수직 가이드라인과 1대1 자석 일치 테이블 */}
      {loading ? (
        <div className="opportunity-state">커뮤니티 이야기를 불러오는 중입니다...</div>
      ) : safePosts.length === 0 ? (
        <div className="opportunity-state">등록된 커뮤니티 이야기 피드가 없습니다. 첫 번째 글을 작성해보세요!</div>
      ) : (
        <div className="opportunity-table" role="table" aria-label="커뮤니티 피드">
          {/* 헤더 (6개 컬럼 정밀 좌측 수직 기준선) */}
          <div className="opportunity-table-head" role="row" style={{ padding: '14px 20px' }}>
            <span role="columnheader" style={{ padding: 0, margin: 0 }}>분류</span>
            <span role="columnheader" style={{ padding: 0, margin: 0 }}>이야기 제목 및 미리보기</span>
            <span role="columnheader" style={{ padding: 0, margin: 0 }}>작성자</span>
            <span role="columnheader" style={{ padding: 0, margin: 0 }}>뱃지 / 반응</span>
            <span role="columnheader" style={{ padding: 0, margin: 0 }}>작성일</span>
            <span role="columnheader" style={{ padding: 0, margin: 0, textAlign: 'center' }}>상세 보기</span>
          </div>

          {/* 목록 데이터 (헤더 텍스트 바로 밑 수직선 1px 오차 없이 칼정렬) */}
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
                style={{ cursor: 'pointer', padding: '16px 20px', minHeight: '96px', alignItems: 'center' }}
                onClick={() => navigate(`/community/posts/${post.id}`)}
              >
                {/* Col 1: 분류 (헤더 '분류' 텍스트 바로 밑 수직선 100% 일치) */}
                <span
                  className="opportunity-type"
                  role="cell"
                  style={{ display: 'flex', alignItems: 'center', height: '64px', padding: 0, margin: 0, fontSize: '13px', fontWeight: '800', color: '#111' }}
                >
                  {getCategoryLabel(post.category)}
                </span>

                {/* Col 2: 썸네일 + 제목/미리보기 (헤더 '이야기 제목...' 바로 밑 수직선 100% 일치) */}
                <div
                  className="opportunity-program"
                  role="cell"
                  style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', gap: '16px', height: '64px', padding: 0, margin: 0 }}
                >
                  {post.imageUrl ? (
                    <img
                      src={post.imageUrl}
                      alt={post.title}
                      style={{ width: '64px', height: '64px', borderRadius: '4px', objectFit: 'cover', border: '2px solid #111', flexShrink: 0, boxShadow: '2px 2px 0 #111' }}
                    />
                  ) : (
                    <div style={{ width: '64px', height: '64px', borderRadius: '4px', background: '#faf0ca', border: '2px solid #111', boxShadow: '2px 2px 0 #111', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '24px', flexShrink: 0 }}>
                      {post.category === 'REVIEW' ? '📝' : post.category === 'RECRUIT' ? '🤝' : '💬'}
                    </div>
                  )}

                  <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', height: '64px', minWidth: 0, gap: '4px' }}>
                    <strong style={{ fontSize: '15px', fontWeight: '800', color: '#111', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', lineHeight: 1.2 }}>
                      {post.title}
                    </strong>
                    <span style={{ fontSize: '12px', color: '#666', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', lineHeight: 1.2 }}>
                      {snippetText}
                    </span>
                  </div>
                </div>

                {/* Col 3: 작성자 (헤더 '작성자' 바로 밑 수직선 100% 일치) */}
                <span
                  className="opportunity-area"
                  role="cell"
                  style={{ display: 'flex', alignItems: 'center', height: '64px', padding: 0, margin: 0, fontSize: '13px', fontWeight: '700', color: '#222' }}
                >
                  ✍️ {getAuthorName(post.author)}
                </span>

                {/* Col 4: 뱃지 및 반응 (헤더 '뱃지 / 반응' 바로 밑 수직선 100% 일치) */}
                <div
                  className="opportunity-keywords"
                  role="cell"
                  style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', height: '64px', gap: '4px', alignItems: 'flex-start', padding: 0, margin: 0 }}
                >
                  <span style={{ background: badgeInfo.bg, color: '#fff', fontSize: '9px', fontWeight: 'bold', padding: '3px 8px', borderRadius: '3px', border: '1px solid rgba(0,0,0,0.15)' }}>
                    {badgeInfo.name}
                  </span>
                  <span style={{ fontSize: '11px', color: '#555', fontWeight: '600' }}>
                    👁️ {viewsCount} · ❤️ {likesCount}
                  </span>
                </div>

                {/* Col 5: 작성일 (헤더 '작성일' 바로 밑 수직선 100% 일치) */}
                <span
                  className="opportunity-status"
                  role="cell"
                  style={{ display: 'flex', alignItems: 'center', height: '64px', padding: 0, margin: 0, fontSize: '12px', fontWeight: '700', color: '#2b9348' }}
                >
                  {createdDate}
                </span>

                {/* Col 6: [상세보기] 버튼 (헤더 '상세 보기' 중앙 수직선 100% 일치) */}
                <div
                  style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '64px', padding: 0, margin: 0 }}
                  role="cell"
                >
                  <button
                    type="button"
                    className="opportunity-action"
                    style={{ width: '92px', height: '36px', fontSize: '12px', fontWeight: 'bold', padding: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: '18px' }}
                    onClick={() => navigate(`/community/posts/${post.id}`)}
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
