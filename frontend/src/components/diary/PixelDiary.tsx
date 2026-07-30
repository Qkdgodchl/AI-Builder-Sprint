import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { PostItem, CommentItem } from '../../services/communityApi';
import { fetchPosts, createPost, likePost, deletePost, fetchComments, createComment, deleteComment } from '../../services/communityApi';
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

  // 댓글 관련 상태
  const [comments, setComments] = useState<CommentItem[]>([]);
  const [commentContent, setCommentContent] = useState('');
  const [commentAuthor, setCommentAuthor] = useState('');
  const [loadingComments, setLoadingComments] = useState(false);

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

  // 선택된 게시글의 댓글 목록 로드
  const loadComments = async (postId: number) => {
    setLoadingComments(true);
    try {
      const data = await fetchComments(postId);
      setComments(data);
    } catch (err) {
      console.error('Failed to load comments:', err);
      setComments([]);
    } finally {
      setLoadingComments(false);
    }
  };

  useEffect(() => {
    if (selectedPost) {
      loadComments(selectedPost.id);
    } else {
      setComments([]);
    }
  }, [selectedPost]);

  const handleCreateComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedPost) return;
    if (!commentContent.trim()) {
      alert('댓글 내용을 입력해주세요!');
      return;
    }

    try {
      const newComment = await createComment(
        selectedPost.id,
        commentContent.trim(),
        commentAuthor.trim() || undefined
      );

      if (newComment) {
        setComments((prev) => [...prev, newComment]);
        setCommentContent('');
        setSelectedPost((prev) =>
          prev ? { ...prev, commentCount: (prev.commentCount ?? 0) + 1 } : null
        );
        onAddDiary(0.1);
        playBeep(659, 0.12);
        showToast('💬 따뜻한 댓글이 작성되었습니다! (온기 +0.1°C)');
      }
    } catch (err) {
      console.error('댓글 작성 오류:', err);
      alert('댓글 작성 중 오류가 발생했습니다.');
    }
  };

  const handleDeleteCommentItem = async (commentId: number) => {
    if (!window.confirm('정말로 이 댓글을 삭제하시겠습니까?')) return;

    try {
      const success = await deleteComment(commentId);
      if (success) {
        setComments((prev) => prev.filter((c) => c.id !== commentId));
        setSelectedPost((prev) =>
          prev ? { ...prev, commentCount: Math.max(0, (prev.commentCount ?? 1) - 1) } : null
        );
        playBeep(349, 0.1);
        showToast('🗑️ 댓글이 삭제되었습니다.');
      } else {
        alert('댓글 삭제에 실패했습니다.');
      }
    } catch (err) {
      console.error('댓글 삭제 오류:', err);
    }
  };

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

        <section className="detail-section">
          <p className="detail-section-number">03</p>
          <div style={{ width: '100%' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ margin: 0 }}>💬 픽셀 용사들의 온기 댓글 ({comments.length})</h3>
            </div>

            {/* 댓글 작성 폼 */}
            <form onSubmit={handleCreateComment} style={{ marginBottom: '24px', padding: '16px', background: '#fcf8eb', borderRadius: '12px', border: '2px solid var(--pc-dark, #111)' }}>
              <div style={{ display: 'flex', gap: '10px', marginBottom: '10px' }}>
                <input
                  type="text"
                  className="pixel-input"
                  style={{ flex: 1, padding: '8px 12px', fontSize: '13px', background: '#fff' }}
                  placeholder="작성자 닉네임 (기본: 익명 용사)"
                  value={commentAuthor}
                  onChange={(e) => setCommentAuthor(e.target.value)}
                />
              </div>
              <textarea
                className="pixel-input"
                style={{ width: '100%', height: '70px', padding: '10px', fontSize: '14px', resize: 'none', marginBottom: '10px', background: '#fff' }}
                placeholder="따뜻한 응원이나 소감을 댓글로 자유롭게 나눠주세요!"
                value={commentContent}
                onChange={(e) => setCommentContent(e.target.value)}
                required
              />
              <div style={{ textAlign: 'right' }}>
                <button type="submit" className="opportunity-action" style={{ background: '#ff70a6', fontSize: '12px', padding: '8px 16px' }}>
                  💬 댓글 남기기 (온기 +0.1°C)
                </button>
              </div>
            </form>

            {/* 댓글 목록 */}
            {loadingComments ? (
              <p style={{ color: '#666', fontSize: '14px' }}>댓글을 불러오는 중입니다...</p>
            ) : comments.length === 0 ? (
              <div style={{ padding: '24px', textAlign: 'center', background: '#fafafa', borderRadius: '8px', border: '1px dashed #ccc', color: '#777', fontSize: '14px' }}>
                👾 첫 번째 온기 댓글의 주인공이 되어보세요!
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {comments.map((comment) => {
                  const badgeInfo = getBadgeColor(comment.authorBadge || 'LV1_SEED');
                  const commentDate = comment.createdAt ? new Date(comment.createdAt).toLocaleString('ko-KR') : '방금 전';

                  return (
                    <div
                      key={comment.id}
                      style={{
                        padding: '14px 16px',
                        background: '#ffffff',
                        borderRadius: '10px',
                        border: '1.5px solid #e0e0e0',
                        boxShadow: '0 2px 4px rgba(0,0,0,0.03)'
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                          <span style={{ fontWeight: '700', fontSize: '14px', color: '#1a1a24' }}>
                            ✍️ {comment.authorNickname}
                          </span>
                          <span style={{ fontSize: '10px', padding: '2px 8px', borderRadius: '12px', color: '#fff', background: badgeInfo.bg, fontWeight: 'bold' }}>
                            {badgeInfo.name}
                          </span>
                          <span style={{ fontSize: '12px', color: '#888' }}>
                            · {commentDate}
                          </span>
                        </div>
                        <button
                          type="button"
                          onClick={() => handleDeleteCommentItem(comment.id)}
                          style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '12px', color: '#ff3b30', opacity: 0.8 }}
                          title="댓글 삭제"
                        >
                          🗑️ 삭제
                        </button>
                      </div>
                      <p style={{ margin: 0, fontSize: '14px', lineHeight: 1.6, color: '#333', whiteSpace: 'pre-wrap' }}>
                        {comment.content}
                      </p>
                    </div>
                  );
                })}
              </div>
            )}
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
    <section className="opportunity-catalog">
      {/* Primary Category Nav (VolunteerCatalog와 동일한 네비게이션) */}
      <nav className="opportunity-primary-nav" aria-label="커뮤니티 이야기 분류">
        {['ALL', 'REVIEW', 'RECRUIT', 'FREE'].map((cat) => (
          <button
            key={cat}
            type="button"
            className={filterCategory === cat ? 'active' : ''}
            onClick={() => setFilterCategory(cat)}
          >
            {cat === 'ALL' ? '전체' : cat === 'REVIEW' ? '봉사후기' : cat === 'RECRUIT' ? '동행모집' : '자율수다'}
          </button>
        ))}

        <button
          type="button"
          className="opportunity-action"
          style={{ background: isWriteOpen ? '#444' : 'var(--magazine-accent, #ff3b30)', padding: '6px 14px', fontSize: '12px', borderRadius: '20px' }}
          onClick={() => setIsWriteOpen(!isWriteOpen)}
        >
          {isWriteOpen ? '✖️ 작성 닫기' : '📝 이야기 작성'}
        </button>

        <span className="opportunity-count" aria-live="polite">
          총 {safePosts.length}개 이야기
        </span>
      </nav>

      {/* 글쓰기 폼 */}
      {isWriteOpen && (
        <div style={{ marginBottom: '24px', padding: '20px', background: '#faf0ca', borderRadius: '12px', border: '2px solid var(--pc-dark, #111)' }}>
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

      {loading ? (
        <div className="opportunity-state">이야기를 불러오는 중입니다.</div>
      ) : safePosts.length === 0 ? (
        <div className="opportunity-state">등록된 이야기 피드가 없습니다. 첫 번째 이야기를 나눠보세요!</div>
      ) : (
        <div className="community-line-feed-container">
          {safePosts.map((post, idx) => {
            const likesCount = post.likeCount ?? (post as any).likes ?? 0;
            const viewsCount = post.viewCount ?? (post as any).views ?? 0;
            const snippetText = post.contentSnippet || post.content || '';
            const createdDate = post.createdAt ? new Date(post.createdAt).toLocaleDateString('ko-KR') : '방금 전';
            const badgeInfo = getBadgeColor(getAuthorBadge(post.author));

            return (
              <article
                key={`${post.id}-${idx}`}
                className="community-line-feed-item"
                onClick={() => navigate(`/community/posts/${post.id}`)}
              >
                {/* 좌측: 카테고리/뱃지 태그 + 굵은 제목 + 본문 미리보기 + 메타정보 */}
                <div className="feed-content-main">
                  <div className="feed-tags-row">
                    <span className="feed-cat-badge">
                      {getCategoryLabel(post.category)}
                    </span>
                    <span className="feed-user-badge" style={{ background: badgeInfo.bg }}>
                      {badgeInfo.name}
                    </span>
                  </div>

                  <h3 className="feed-title">{post.title}</h3>
                  <p className="feed-snippet">{snippetText}</p>

                  <div className="feed-meta-row">
                    <span className="stat-item" style={{ color: '#ff3b30', fontWeight: 'bold' }}>
                      ❤️ {likesCount}
                    </span>
                    <span className="stat-item" style={{ color: '#2ec4b6', fontWeight: 'bold' }}>
                      💬 {post.commentCount ?? 0}
                    </span>
                    <span className="stat-item">
                      👁️ {viewsCount}
                    </span>
                    <span>·</span>
                    <span>✍️ {getAuthorName(post.author)}</span>
                    <span>·</span>
                    <span>📅 {createdDate}</span>
                  </div>
                </div>

                {/* 우측: 80x80 정사각형 썸네일/아이콘 박스 */}
                <div className="feed-thumbnail-box">
                  {post.imageUrl ? (
                    <img src={post.imageUrl} alt={post.title} />
                  ) : (
                    <div className="placeholder-icon">
                      {post.category === 'REVIEW' ? '📝' : post.category === 'RECRUIT' ? '🤝' : '💬'}
                    </div>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
};
