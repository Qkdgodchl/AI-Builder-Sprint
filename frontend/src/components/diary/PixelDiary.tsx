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
      showToast('❤️ 게시글에 응원 하트를 보냈습니다!');
    } catch (err) {
      console.error(err);
    }
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

  // 상세 보기 화면
  if (selectedPost) {
    const likesCount = selectedPost.likeCount ?? (selectedPost as any).likes ?? 0;
    const viewsCount = selectedPost.viewCount ?? (selectedPost as any).views ?? 0;
    const textContent = selectedPost.content || selectedPost.contentSnippet || '';
    const createdDate = selectedPost.createdAt ? new Date(selectedPost.createdAt).toLocaleString('ko-KR') : '방금 전';

    return (
      <section className="opportunity-catalog" style={{ maxWidth: '840px', margin: '0 auto' }}>
        <button
          type="button"
          className="opportunity-action"
          style={{ marginBottom: '16px', background: '#444', padding: '6px 16px' }}
          onClick={() => setSelectedPost(null)}
        >
          ← 목록으로 돌아가기
        </button>

        <article className="opportunity-row" style={{ flexDirection: 'column', alignItems: 'stretch', gap: '16px', padding: '24px', background: '#fff' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span className="opportunity-type">{getCategoryLabel(selectedPost.category)}</span>
            <span style={{ fontSize: '12px', color: '#888' }}>
              👁️ {viewsCount} 회 · 📅 {createdDate}
            </span>
          </div>

          <h2 style={{ fontSize: '20px', fontWeight: '800', margin: '8px 0', color: '#111' }}>
            {selectedPost.title}
          </h2>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '8px 12px', background: '#f8f9fa', borderRadius: '8px' }}>
            <span style={{ fontSize: '13px', fontWeight: '700' }}>✍️ {getAuthorName(selectedPost.author)}</span>
            <span className="opportunity-type" style={{ background: '#2ec4b6', color: '#fff', fontSize: '10px' }}>
              {getAuthorBadge(selectedPost.author)}
            </span>
          </div>

          <div style={{ fontSize: '15px', color: '#333', lineHeight: 1.7, minHeight: '120px', whiteSpace: 'pre-line' }}>
            {textContent}
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '16px', borderTop: '1px dashed #ddd' }}>
            <button
              type="button"
              className="opportunity-action"
              style={{ background: '#ff4d6d', color: '#fff', fontSize: '13px', padding: '10px 20px' }}
              onClick={() => handleLike(selectedPost.id)}
            >
              ❤️ 응원 하트 보내기 ({likesCount})
            </button>
          </div>
        </article>
      </section>
    );
  }

  return (
    <section className="opportunity-catalog" style={{ maxWidth: '960px', margin: '0 auto' }}>
      {/* Primary Category Nav (Volunteer Catalog와 동일 구조) */}
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

      {/* 커뮤니티 잡지형 매거진 테이블 목록 (VolunteerCatalog와 identical 구조) */}
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
            <span role="columnheader">조회/하트</span>
            <span role="columnheader">작성일</span>
            <span role="columnheader" aria-label="상세 보기" />
          </div>

          {safePosts.map((post, idx) => {
            const likesCount = post.likeCount ?? (post as any).likes ?? 0;
            const viewsCount = post.viewCount ?? (post as any).views ?? 0;
            const snippetText = post.contentSnippet || post.content || '';
            const createdDate = post.createdAt ? new Date(post.createdAt).toLocaleDateString('ko-KR') : '방금 전';

            return (
              <article
                className="opportunity-row"
                role="row"
                key={`${post.id}-${idx}`}
                style={{ cursor: 'pointer' }}
                onClick={() => setSelectedPost(post)}
              >
                <span className="opportunity-type" role="cell">
                  {getCategoryLabel(post.category)}
                </span>

                <div className="opportunity-program" role="cell">
                  <strong>{post.title}</strong>
                  <span style={{ fontSize: '12px', color: '#666', marginTop: '2px' }}>
                    {snippetText.length > 50 ? `${snippetText.substring(0, 50)}...` : snippetText}
                  </span>
                </div>

                <div className="opportunity-keywords" role="cell">
                  <span style={{ fontWeight: 'bold' }}>{getAuthorName(post.author)}</span>
                  <span style={{ background: '#2ec4b6', color: '#fff', fontSize: '9px', padding: '2px 6px', borderRadius: '4px' }}>
                    {getAuthorBadge(post.author)}
                  </span>
                </div>

                <span className="opportunity-area" role="cell">
                  👁️ {viewsCount} · ❤️ {likesCount}
                </span>

                <span className="opportunity-status" role="cell">
                  {createdDate}
                </span>

                <div style={{ display: 'flex', gap: '6px' }} role="cell">
                  <button
                    type="button"
                    className="opportunity-action"
                    style={{ background: '#ffe5ec', borderColor: '#ff4d6d', color: '#c9184a', fontSize: '11px', padding: '4px 8px' }}
                    onClick={(e) => handleLike(post.id, e)}
                  >
                    ❤️ {likesCount}
                  </button>
                  <button
                    type="button"
                    className="opportunity-action"
                    style={{ fontSize: '11px', padding: '4px 8px' }}
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
