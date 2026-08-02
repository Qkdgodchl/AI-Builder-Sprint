import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { PostItem, CommentItem } from '../../services/communityApi';
import { fetchPost, fetchPosts, createPost, likePost, deletePost, fetchComments, createComment, deleteComment } from '../../services/communityApi';
import { playBeep } from '../../services/soundFx';
import type { SessionUser } from '../../types';

interface PixelDiaryProps {
  onAddDiary: (tempIncrease: number) => void;
  showToast: (message: string) => void;
  currentUser: SessionUser | null;
  onRequireLogin: (message: string) => void;
}

export const PixelDiary: React.FC<PixelDiaryProps> = ({
  onAddDiary,
  showToast,
  currentUser,
  onRequireLogin,
}) => {
  // 글쓰기는 계정에 남는 기록이라 로그인한 사용자에게만 연다.
  const openComposer = () => {
    if (!currentUser) {
      onRequireLogin('로그인이 필요합니다. 이야기 작성은 로그인 후 이용할 수 있어요.');
      return;
    }
    setIsWriteOpen((open) => !open);
  };
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
  const detailRequestRef = useRef<{ id: number; promise: Promise<PostItem> } | null>(null);

  // 댓글 관련 상태
  const [comments, setComments] = useState<CommentItem[]>([]);
  const [commentContent, setCommentContent] = useState('');
  const [commentAuthor, setCommentAuthor] = useState('');
  const [loadingComments, setLoadingComments] = useState(false);

  const loadPosts = useCallback(async () => {
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
  }, [filterCategory]);

  useEffect(() => {
    void loadPosts();
  }, [loadPosts]);

  useEffect(() => {
    if (!urlPostId) {
      detailRequestRef.current = null;
      setSelectedPost(null);
      return;
    }

    const targetId = Number(urlPostId);
    if (!Number.isInteger(targetId) || targetId <= 0) {
      setSelectedPost(null);
      return;
    }

    const request = detailRequestRef.current?.id === targetId
      ? detailRequestRef.current
      : { id: targetId, promise: fetchPost(targetId) };
    detailRequestRef.current = request;

    let active = true;
    request.promise
      .then((post) => {
        if (active) setSelectedPost(post);
      })
      .catch((err) => {
        console.error('Failed to load community post detail:', err);
        if (detailRequestRef.current?.id === targetId) {
          detailRequestRef.current = null;
        }
        if (active) {
          showToast('게시글을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.');
          navigate('/community');
        }
      });

    return () => {
      active = false;
    };
  }, [navigate, showToast, urlPostId]);

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
        currentUser ? undefined : commentAuthor.trim() || undefined
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
        author: author.trim() || currentUser?.nickname || '부산 잇다 이웃',
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
    if (!currentUser) {
      showToast('로그인 후 게시글에 응원을 보낼 수 있습니다.');
      return;
    }
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
      if (updatedLike.isLiked) {
        onAddDiary(0.1);
        playBeep(784, 0.1);
        showToast('❤️ 게시글에 응원 하트를 보냈습니다! (온기 +0.1°C)');
      } else {
        showToast('게시글 응원을 취소했습니다.');
      }
    } catch (err) {
      console.error(err);
      showToast('응원 처리에 실패했습니다. 잠시 후 다시 시도해주세요.');
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

  const canDeletePost = (post: PostItem) =>
    Boolean(
      currentUser &&
        (currentUser.role === 'OPERATOR' || currentUser.id === post.authorUserId),
    );

  const handleCopyLink = () => {
    navigator.clipboard.writeText(window.location.href);
    playBeep(520, 0.1);
    showToast('🔗 게시글 링크가 클립보드에 복사되었습니다!');
  };

  const getAuthorName = (authorInfo: any) => {
    if (!authorInfo) return '부산 잇다 이웃';
    if (typeof authorInfo === 'string') return authorInfo;
    return authorInfo.nickname || '부산 잇다 이웃';
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
    const canDelete = canDeletePost(selectedPost);
    const likesCount = selectedPost.likeCount ?? (selectedPost as any).likes ?? 0;
    const viewsCount = selectedPost.viewCount ?? (selectedPost as any).views ?? 0;
    const textContent = selectedPost.content || selectedPost.contentSnippet || '';
    const createdDate = selectedPost.createdAt ? new Date(selectedPost.createdAt).toLocaleDateString('ko-KR') : '방금 전';
    const badgeInfo = getBadgeColor(getAuthorBadge(selectedPost.author));
    const authorName = getAuthorName(selectedPost.author);
    const authorInitial = authorName.trim().charAt(0) || '픽';
    const categoryLabel = getCategoryLabel(selectedPost.category);

    return (
      <article className="community-post-page">
        <nav className="community-post-nav" aria-label="게시글 상세 탐색">
          <button type="button" onClick={() => navigate('/community')}>
            <span aria-hidden="true">←</span> 커뮤니티 목록
          </button>
          {canDelete && (
            <button
              type="button"
              className="community-post-delete"
              onClick={() => handleDeletePost(selectedPost.id)}
            >
              삭제
            </button>
          )}
        </nav>

        <main className="community-post-main">
          <header className="community-post-header">
            <span className={`community-post-category ${selectedPost.category.toLowerCase()}`}>
              {categoryLabel}
            </span>
            <h2>{selectedPost.title}</h2>

            <div className="community-post-author">
              <span className="community-post-avatar" aria-hidden="true">{authorInitial}</span>
              <div>
                <div className="community-post-author-name">
                  <strong>{authorName}</strong>
                  <span style={{ '--badge-color': badgeInfo.bg } as React.CSSProperties}>{badgeInfo.name}</span>
                </div>
                <p>{createdDate} · 조회 {viewsCount} · 댓글 {comments.length}</p>
              </div>
            </div>
          </header>

          <section className="community-post-content" aria-label="게시글 본문">
            {selectedPost.imageUrl && (
              <figure>
                <img src={selectedPost.imageUrl} alt={selectedPost.title} />
              </figure>
            )}
            <p>{textContent}</p>
          </section>

          <div className="community-post-actions" aria-label="게시글 반응">
            <button type="button" className="community-post-like" onClick={() => handleLike(selectedPost.id)}>
              <span aria-hidden="true">♡</span> 응원 {likesCount}
            </button>
            <button type="button" onClick={handleCopyLink}>
              <span aria-hidden="true">↗</span> 링크 복사
            </button>
          </div>

          <aside className="community-post-note">
            <strong>함께 지키는 커뮤니티</strong>
            <span>따뜻한 응원과 구체적인 경험을 나누고, 개인정보가 포함되지 않도록 확인해주세요.</span>
          </aside>

          <section className="community-comments" aria-labelledby="community-comments-title">
            <header>
              <div>
                <h3 id="community-comments-title">댓글 <span>{comments.length}</span></h3>
                <p>이야기에 공감하거나 도움이 되는 경험을 이어서 나눠보세요.</p>
              </div>
              <span>등록순</span>
            </header>

            <form className="community-comment-form" onSubmit={handleCreateComment}>
              <div className="community-comment-form-author">
                <span className="community-comment-avatar" aria-hidden="true">
                  {(currentUser?.nickname || commentAuthor || '익').trim().charAt(0)}
                </span>
                <div>
                  <strong>{currentUser?.nickname || '익명 용사'}</strong>
                  <span>서로를 존중하는 댓글을 남겨주세요.</span>
                </div>
              </div>
              {!currentUser && (
                <label>
                  <span>작성자 닉네임</span>
                <input
                  type="text"
                  placeholder="닉네임을 입력해주세요. (미입력 시 익명 용사)"
                  value={commentAuthor}
                  onChange={(e) => setCommentAuthor(e.target.value)}
                />
                </label>
              )}
              <label className="community-comment-message">
                <span>댓글 내용</span>
                <textarea
                  placeholder="따뜻한 응원이나 도움이 되는 정보를 남겨주세요."
                  value={commentContent}
                  onChange={(e) => setCommentContent(e.target.value)}
                  required
                />
              </label>
              <footer>
                <span>{commentContent.length}자</span>
                <button type="submit">댓글 등록</button>
              </footer>
            </form>

            {loadingComments ? (
              <p className="community-comments-status">댓글을 불러오는 중입니다.</p>
            ) : comments.length === 0 ? (
              <div className="community-comments-empty">
                <strong>아직 댓글이 없습니다.</strong>
                <span>첫 번째 응원과 경험을 남겨보세요.</span>
              </div>
            ) : (
              <div className="community-comment-list">
                {comments.map((comment) => {
                  const commentBadge = getBadgeColor(comment.authorBadge || 'LV1_SEED');
                  const commentDate = comment.createdAt ? new Date(comment.createdAt).toLocaleString('ko-KR') : '방금 전';

                  return (
                    <article key={comment.id} className="community-comment-item">
                      <span className="community-comment-avatar" aria-hidden="true">
                        {(comment.authorNickname || '익').trim().charAt(0)}
                      </span>
                      <div className="community-comment-body">
                        <header>
                          <div>
                            <strong>{comment.authorNickname}</strong>
                            <span className="community-comment-badge" style={{ '--badge-color': commentBadge.bg } as React.CSSProperties}>
                              {commentBadge.name}
                            </span>
                            <time>{commentDate}</time>
                          </div>
                          <button
                            type="button"
                            className="community-comment-delete"
                            onClick={() => handleDeleteCommentItem(comment.id)}
                            title="댓글 삭제"
                          >
                            삭제
                          </button>
                        </header>
                        <p>{comment.content}</p>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </section>
        </main>
      </article>
    );
  }

  return (
    <section className="community-board-page">
      <header className="community-board-header">
        <div>
          <p>ITDA COMMUNITY</p>
          <h2>선행을 나누는 사람들의 이야기</h2>
          <span>
            봉사 경험과 유용한 팁을 기록하고, 같은 마음을 가진 이웃을 만나보세요.
          </span>
        </div>
        <button type="button" onClick={openComposer}>
          {isWriteOpen ? '작성 닫기' : '이야기 작성'} <span aria-hidden="true">＋</span>
        </button>
      </header>

      <div className="community-board-toolbar">
        <nav className="community-board-tabs" aria-label="커뮤니티 이야기 분류">
          {[
            { value: 'ALL', label: '전체' },
            { value: 'REVIEW', label: '봉사 후기' },
            { value: 'RECRUIT', label: '동행 모집' },
            { value: 'FREE', label: '자유 이야기' },
          ].map((filter) => (
            <button
              key={filter.value}
              type="button"
              className={filterCategory === filter.value ? 'active' : ''}
              onClick={() => setFilterCategory(filter.value)}
            >
              {filter.label}
            </button>
          ))}
        </nav>
        <span aria-live="polite">총 {safePosts.length}개</span>
      </div>

      {isWriteOpen && (
        <section className="community-composer" aria-labelledby="community-composer-title">
          <div className="community-composer-heading">
            <div>
              <p>새 글</p>
              <h3 id="community-composer-title">이야기 작성</h3>
            </div>
            <button type="button" onClick={() => setIsWriteOpen(false)} aria-label="작성 화면 닫기">×</button>
          </div>
          <form onSubmit={handleCreate}>
            <div className="community-composer-grid">
              <label>
                <span>분류</span>
                <select value={category} onChange={(e) => setCategory(e.target.value)}>
                  <option value="REVIEW">봉사 후기</option>
                  <option value="RECRUIT">동행 모집</option>
                  <option value="FREE">자유 이야기</option>
                </select>
              </label>
              <label>
                <span>작성자</span>
                <input
                  type="text"
                  placeholder={currentUser?.nickname || '작성자 닉네임'}
                  value={author}
                  onChange={(e) => setAuthor(e.target.value)}
                />
              </label>
              <label className="community-composer-wide">
                <span>제목</span>
                <input
                  type="text"
                  placeholder="경험과 핵심 내용이 잘 드러나는 제목을 적어주세요"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  required
                />
              </label>
              <label className="community-composer-wide">
                <span>내용</span>
                <textarea
                  placeholder="참여 과정, 준비물, 함께하고 싶은 일정 등 다른 이웃에게 도움이 될 내용을 나눠주세요."
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  required
                />
              </label>
            </div>
            <div className="community-composer-footer">
              <p>서로를 배려하는 표현과 정확한 정보를 사용해주세요.</p>
              <button type="submit">게시하기</button>
            </div>
          </form>
        </section>
      )}

      <main className="community-topic-board" aria-label="커뮤니티 이야기 목록">
        <div className="community-topic-heading">
          <div>
            <h3>최신 이야기</h3>
            <p>새로 올라온 경험과 동행 소식을 확인하세요.</p>
          </div>
          <span>최신순</span>
        </div>

        <div className="community-topic-columns" aria-hidden="true">
          <span>이야기</span>
          <span>반응</span>
          <span>조회</span>
          <span>작성일</span>
        </div>

        {loading ? (
          <div className="community-empty-state">이야기를 불러오는 중입니다.</div>
        ) : safePosts.length === 0 ? (
          <div className="community-empty-state">
            <strong>아직 등록된 이야기가 없습니다.</strong>
            <span>첫 번째 경험을 나누고 새로운 연결을 만들어보세요.</span>
            <button type="button" onClick={openComposer}>첫 이야기 작성하기</button>
          </div>
        ) : (
          <div className="community-topic-list">
            {safePosts.map((post) => {
              const likesCount = post.likeCount ?? post.likes ?? 0;
              const viewsCount = post.viewCount ?? post.views ?? 0;
              const snippetText = post.contentSnippet || post.content || '';
              const createdDate = post.createdAt
                ? new Date(post.createdAt).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
                : '방금 전';
              const authorName = getAuthorName(post.author);
              const badgeInfo = getBadgeColor(getAuthorBadge(post.author));

              return (
                <article
                  key={post.id}
                  className="community-topic-row"
                  tabIndex={0}
                  onClick={() => navigate(`/community/posts/${post.id}`)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault();
                      navigate(`/community/posts/${post.id}`);
                    }
                  }}
                >
                  <div className="community-topic-main">
                    <div className="community-topic-labels">
                      <span className={`community-category-label ${post.category.toLowerCase()}`}>
                        {getCategoryLabel(post.category).replace(/^[^\s]+\s/, '')}
                      </span>
                      <span className="community-author-level" style={{ '--badge-color': badgeInfo.bg } as React.CSSProperties}>
                        {badgeInfo.name}
                      </span>
                    </div>
                    <h4>{post.title}</h4>
                    <p>{snippetText}</p>
                    <div className="community-topic-meta">
                      <strong>{authorName}</strong>
                      <span>·</span>
                      <span>{createdDate}</span>
                      <span>·</span>
                      <span>댓글 {post.commentCount ?? 0}</span>
                    </div>
                  </div>

                  <div className="community-topic-reaction">
                    <button type="button" onClick={(event) => void handleLike(post.id, event)}>
                      ♡ {likesCount}
                    </button>
                    <span>응원</span>
                  </div>
                  <div className="community-topic-view">
                    <strong>{viewsCount}</strong>
                    <span>회</span>
                  </div>
                  <div className="community-topic-activity">
                    <span>{createdDate}</span>
                    <button
                      type="button"
                      aria-label={`${post.title} 자세히 보기`}
                      onClick={(event) => {
                        event.stopPropagation();
                        navigate(`/community/posts/${post.id}`);
                      }}
                    >
                      →
                    </button>
                    {canDeletePost(post) && (
                      <button
                        type="button"
                        className="community-topic-delete"
                        onClick={(event) => {
                          event.stopPropagation();
                          void handleDeletePost(post.id);
                        }}
                      >
                        삭제
                      </button>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </main>

      <footer className="community-board-note">
        <strong>함께 만드는 커뮤니티</strong>
        <span>구체적인 경험과 정확한 동행 정보를 나누고, 개인정보와 존중의 언어를 지켜주세요.</span>
      </footer>
    </section>
  );
};
