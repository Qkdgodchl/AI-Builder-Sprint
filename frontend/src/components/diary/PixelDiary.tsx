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

  const loadPosts = async () => {
    setLoading(true);
    try {
      const data = await fetchPosts(filterCategory);
      setPosts(data);
    } catch (err) {
      console.error('Failed to load community posts:', err);
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
        author: author.trim() || '부산 픽셀용사',
        category,
      });

      setPosts((prev) => [created, ...prev]);
      onAddDiary(0.5);
      showToast(`📝 픽셀 커뮤니티 글이 등록되었습니다! 온기 +0.5°C 상승!`);
      playBeep(587, 0.15);

      setTitle('');
      setContent('');
      setAuthor('');
    } catch (err) {
      console.error(err);
      alert('게시글 등록 중 오류가 발생했습니다.');
    }
  };

  const handleLike = async (id: number) => {
    try {
      const updated = await likePost(id);
      setPosts((prev) => prev.map((p) => (p.id === id ? updated : p)));
      onAddDiary(0.1);
      playBeep(784, 0.1);
      showToast('❤️ 게시글에 응원 하트를 보냈습니다!');
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      {/* Create Community Post Form */}
      <div className="pixel-box" style={{ marginBottom: '20px', background: '#faf0ca' }}>
        <div style={{ fontSize: '16px', fontWeight: 'bold', marginBottom: '12px', color: '#1a1a24', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span>💬</span> 픽셀 커뮤니티 새 글 작성하기
        </div>

        <form onSubmit={handleCreate}>
          <div style={{ display: 'flex', gap: '8px', marginBottom: '10px', flexWrap: 'wrap' }}>
            <input
              type="text"
              className="pixel-input"
              style={{ flex: 2, minWidth: '200px' }}
              placeholder="📌 게시글 제목 (예: 해운대 플로깅 봉사 함께해요!)"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              required
            />
            <input
              type="text"
              className="pixel-input"
              style={{ flex: 1, minWidth: '130px' }}
              placeholder="작성자 닉네임"
              value={author}
              onChange={(e) => setAuthor(e.target.value)}
            />
            <select
              className="pixel-input"
              style={{ padding: '6px' }}
              value={category}
              onChange={(e) => setCategory(e.target.value)}
            >
              <option value="REVIEW">📝 봉사 후기</option>
              <option value="RECRUIT">🤝 동행 모집</option>
              <option value="GENERAL">💬 자율 수다</option>
            </select>
          </div>

          <textarea
            className="pixel-input"
            style={{ width: '100%', height: '90px', marginBottom: '12px', resize: 'none' }}
            placeholder="봉사 참여 후기, 함께할 동행 모집, 따뜻한 선행 이야기를 나눠보세요!"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            required
          />

          <div style={{ textAlign: 'right' }}>
            <button type="submit" className="pixel-btn pixel-btn-red">
              🚀 게시글 등록 (온기 +0.5°C)
            </button>
          </div>
        </form>
      </div>

      {/* Filter Tabs */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
        <div style={{ fontSize: '15px', fontWeight: 'bold', color: '#1a1a24' }}>
          📋 커뮤니티 이야기 피드
        </div>
        <div style={{ display: 'flex', gap: '4px' }}>
          {['ALL', 'REVIEW', 'RECRUIT', 'GENERAL'].map((cat) => (
            <button
              key={cat}
              className="pixel-btn"
              style={{
                fontSize: '11px',
                padding: '4px 8px',
                background: filterCategory === cat ? '#2ec4b6' : '#eee',
                color: filterCategory === cat ? '#fff' : '#333',
              }}
              onClick={() => setFilterCategory(cat)}
            >
              {cat === 'ALL' ? '전체' : cat === 'REVIEW' ? '후기' : cat === 'RECRUIT' ? '모집' : '수다'}
            </button>
          ))}
        </div>
      </div>

      {/* Post Feed List */}
      {loading ? (
        <div className="pixel-box" style={{ textAlign: 'center', padding: '36px', color: '#666' }}>
          ⌛ 커뮤니티 피드 로딩 중...
        </div>
      ) : posts.length === 0 ? (
        <div className="pixel-box" style={{ textAlign: 'center', padding: '36px', color: '#666' }}>
          📭 아직 등록된 글이 없습니다. 첫 번째 이야기를 남겨보세요!
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          {posts.map((p) => (
            <div key={p.id} className="pixel-box" style={{ background: '#fff' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                <span className="pixel-tag" style={{ background: p.category === 'REVIEW' ? '#ff9f1c' : p.category === 'RECRUIT' ? '#2ec4b6' : '#e76f51', color: '#fff' }}>
                  {p.category === 'REVIEW' ? '📝 후기' : p.category === 'RECRUIT' ? '🤝 모집' : '💬 수다'}
                </span>
                <span style={{ fontSize: '11px', color: '#888' }}>
                  👁️ {p.views} · 📅 {new Date(p.createdAt).toLocaleDateString('ko-KR')}
                </span>
              </div>

              <div style={{ fontSize: '15px', fontWeight: 'bold', color: '#1a1a24', marginBottom: '6px' }}>
                {p.title}
              </div>

              <div style={{ fontSize: '13px', color: '#444', lineHeight: 1.5, marginBottom: '12px', whiteSpace: 'pre-line' }}>
                {p.content}
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: '8px', borderTop: '1px dashed #eee' }}>
                <span style={{ fontSize: '12px', color: '#666' }}>
                  ✍️ 작성자: <b>{p.author}</b>
                </span>
                <button
                  className="pixel-btn"
                  style={{ fontSize: '11px', background: '#ffe5ec', borderColor: '#ff4d6d', color: '#c9184a' }}
                  onClick={() => handleLike(p.id)}
                >
                  ❤️ 응원 하트 {p.likes}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
