import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { SessionUser } from '../../types';

interface MyCenterPageProps {
  currentUser: SessionUser;
}

interface ManagedCenter {
  id: number;
  name: string;
  type: string;
  region: string;
  status: string;
  publishedPosts: number;
  completedPosts: number;
  pendingApplicants: number;
  monthlyParticipants: number;
}

type PostStatus = '공개 중' | '마감';
type ApplicantStatus = '검토 대기' | '승인 완료';
type CenterTab = 'posts' | 'applicants';
type PostManagerTab = 'edit' | 'applicants';

interface CenterPost {
  id: number;
  centerId: number;
  title: string;
  category: string;
  period: string;
  status: PostStatus;
  applicantCount: number;
  description: string;
}

interface CenterApplicant {
  id: number;
  publicId: string;
  centerId: number;
  postId: number;
  name: string;
  email: string;
  phone: string;
  postTitle: string;
  appliedAt: string;
  status: ApplicantStatus;
  motivation: string;
  documents: string[];
}

const managedCenters: ManagedCenter[] = [
  {
    id: 1,
    name: '픽셀케어 데모 센터',
    type: '사회복지기관',
    region: '부산광역시 금정구',
    status: '승인 완료',
    publishedPosts: 3,
    completedPosts: 2,
    pendingApplicants: 12,
    monthlyParticipants: 24,
  },
  {
    id: 2,
    name: '부산 온기 봉사센터',
    type: '비영리단체',
    region: '부산광역시 해운대구',
    status: '승인 완료',
    publishedPosts: 2,
    completedPosts: 4,
    pendingApplicants: 5,
    monthlyParticipants: 18,
  },
  {
    id: 3,
    name: '문화유산 지킴이 센터',
    type: '문화·유산기관',
    region: '경상남도 경주시',
    status: '승인 완료',
    publishedPosts: 4,
    completedPosts: 1,
    pendingApplicants: 7,
    monthlyParticipants: 31,
  },
];

const initialPosts: CenterPost[] = [
  {
    id: 101,
    centerId: 1,
    title: '금정구 독거어르신 온기 도시락 배달',
    category: '봉사',
    period: '2026.08.01 – 2026.08.31',
    status: '공개 중',
    applicantCount: 12,
    description: '지역 내 독거어르신께 도시락을 포장하고 전달하는 정기 봉사 프로그램입니다.',
  },
  {
    id: 102,
    centerId: 1,
    title: '지역 아동 여름방학 학습 멘토링',
    category: '봉사',
    period: '2026.08.05 – 2026.08.28',
    status: '공개 중',
    applicantCount: 8,
    description: '지역 아동의 기초 학습과 진로 탐색을 돕는 멘토링 프로그램입니다.',
  },
  {
    id: 103,
    centerId: 1,
    title: '취약계층 냉방비 지원 모금',
    category: '기부',
    period: '2026.07.15 – 2026.08.20',
    status: '공개 중',
    applicantCount: 19,
    description: '폭염 취약계층을 위한 냉방비와 여름 물품을 지원합니다.',
  },
  {
    id: 104,
    centerId: 1,
    title: '봄맞이 주거환경 개선 봉사',
    category: '봉사',
    period: '2026.04.01 – 2026.04.30',
    status: '마감',
    applicantCount: 21,
    description: '지역 내 노후 주거공간을 정리하고 보수하는 활동입니다.',
  },
  {
    id: 201,
    centerId: 2,
    title: '해운대 해변 플로깅 활동',
    category: '봉사',
    period: '2026.08.03 – 2026.08.24',
    status: '공개 중',
    applicantCount: 14,
    description: '해운대 해변 일대를 걸으며 해양 쓰레기를 수거합니다.',
  },
  {
    id: 202,
    centerId: 2,
    title: '여름철 유기동물 보호소 지원',
    category: '기부',
    period: '2026.07.20 – 2026.08.31',
    status: '공개 중',
    applicantCount: 9,
    description: '보호소의 사료와 여름철 냉방 물품을 지원합니다.',
  },
  {
    id: 301,
    centerId: 3,
    title: '경주 문화유산 환경 정비',
    category: '봉사',
    period: '2026.08.10 – 2026.09.15',
    status: '공개 중',
    applicantCount: 16,
    description: '문화유산 주변 환경을 정비하고 관람객 안내를 돕습니다.',
  },
  {
    id: 302,
    centerId: 3,
    title: '문화유산 보존 정기 후원',
    category: '기부',
    period: '상시 모집',
    status: '공개 중',
    applicantCount: 27,
    description: '지역 문화유산의 보수와 기록 사업을 정기 후원합니다.',
  },
];

const initialApplicants: CenterApplicant[] = [
  {
    id: 1001,
    publicId: '6f3c2a91-7d48-4c0a-9f52-12b6a3e8d401',
    centerId: 1,
    postId: 101,
    name: '김서연',
    email: 'seoyeon@example.com',
    phone: '010-1234-5678',
    postTitle: '금정구 독거어르신 온기 도시락 배달',
    appliedAt: '2026.07.29',
    status: '검토 대기',
    motivation: '지역 어르신께 직접 도움을 드리는 활동에 꾸준히 참여하고 싶습니다.',
    documents: ['봉사활동 신청서.pdf', '개인정보 수집·이용 동의서.pdf', '전자서명 약정서.pdf'],
  },
  {
    id: 1002,
    publicId: 'ab846d20-52a7-4ee9-a81a-723b5e1c4902',
    centerId: 1,
    postId: 102,
    name: '이도윤',
    email: 'doyun@example.com',
    phone: '010-2874-1182',
    postTitle: '지역 아동 여름방학 학습 멘토링',
    appliedAt: '2026.07.28',
    status: '검토 대기',
    motivation: '교육 봉사 경험을 살려 아이들의 학습과 진로 탐색을 돕고 싶습니다.',
    documents: ['멘토링 신청서.pdf', '재학증명서.pdf', '전자서명 약정서.pdf'],
  },
  {
    id: 1003,
    publicId: 'c1940e75-830d-4ad3-a683-74b1f2986e03',
    centerId: 1,
    postId: 101,
    name: '박지우',
    email: 'jiwoo@example.com',
    phone: '010-9321-4451',
    postTitle: '금정구 독거어르신 온기 도시락 배달',
    appliedAt: '2026.07.27',
    status: '승인 완료',
    motivation: '정기적으로 참여할 수 있는 생활 밀착형 봉사를 찾고 있었습니다.',
    documents: ['봉사활동 신청서.pdf', '개인정보 수집·이용 동의서.pdf', '전자서명 약정서.pdf'],
  },
  {
    id: 2001,
    publicId: 'd82a9634-1f75-46d9-8853-9b1a4c702104',
    centerId: 2,
    postId: 201,
    name: '최민준',
    email: 'minjun@example.com',
    phone: '010-7781-2304',
    postTitle: '해운대 해변 플로깅 활동',
    appliedAt: '2026.07.29',
    status: '검토 대기',
    motivation: '해양 환경 보호 활동에 직접 참여하고 싶습니다.',
    documents: ['참가 신청서.pdf', '활동 안전 동의서.pdf'],
  },
  {
    id: 3001,
    publicId: 'e7314b68-9a20-41cf-b572-06d8f32a9505',
    centerId: 3,
    postId: 301,
    name: '정하린',
    email: 'harin@example.com',
    phone: '010-6092-3374',
    postTitle: '경주 문화유산 환경 정비',
    appliedAt: '2026.07.26',
    status: '검토 대기',
    motivation: '문화유산 보존과 지역사회 활동에 관심이 많습니다.',
    documents: ['참여 신청서.pdf', '활동 서약서.pdf'],
  },
];

export const MyCenterPage: React.FC<MyCenterPageProps> = ({ currentUser }) => {
  const navigate = useNavigate();
  const route = useParams()['*'] ?? '';
  const [selectedCenter, setSelectedCenter] = useState<ManagedCenter | null>(null);
  const [activeTab, setActiveTab] = useState<CenterTab>('posts');
  const [postManagerTab, setPostManagerTab] = useState<PostManagerTab>('edit');
  const [posts, setPosts] = useState<CenterPost[]>(initialPosts);
  const [applicants, setApplicants] = useState<CenterApplicant[]>(initialApplicants);
  const [editingPost, setEditingPost] = useState<CenterPost | null>(null);
  const [viewingApplicant, setViewingApplicant] = useState<CenterApplicant | null>(null);
  const [notice, setNotice] = useState('');

  const centerPosts = useMemo(
    () => posts.filter((post) => post.centerId === selectedCenter?.id),
    [posts, selectedCenter],
  );
  const centerApplicants = useMemo(
    () => applicants.filter((applicant) => applicant.centerId === selectedCenter?.id),
    [applicants, selectedCenter],
  );
  const postApplicants = useMemo(
    () => applicants.filter((applicant) => applicant.postId === editingPost?.id),
    [applicants, editingPost],
  );

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'auto' });
  }, [route]);

  useEffect(() => {
    const [centerId, section, entityId, subSection, applicantId] = route
      .split('/')
      .filter(Boolean);

    if (!centerId) {
      setSelectedCenter(null);
      setEditingPost(null);
      setViewingApplicant(null);
      return;
    }

    const center = managedCenters.find((item) => String(item.id) === centerId);
    if (!center) {
      navigate('/my-centers', { replace: true });
      return;
    }

    setSelectedCenter(center);

    if (section === 'posts' && entityId) {
      const post = posts.find(
        (item) => item.centerId === center.id && String(item.id) === entityId,
      );
      if (!post) {
        navigate(`/my-centers/${center.id}`, { replace: true });
        return;
      }

      setEditingPost((current) => (current?.id === post.id ? current : { ...post }));
      setActiveTab('posts');

      if (subSection === 'applications') {
        setPostManagerTab('applicants');
        const applicant = applicantId
          ? applicants.find(
              (item) => item.postId === post.id && item.publicId === applicantId,
            )
          : null;
        setViewingApplicant(applicant ?? null);
      } else {
        setPostManagerTab('edit');
        setViewingApplicant(null);
      }
      return;
    }

    setEditingPost(null);

    if (section === 'applications') {
      setActiveTab('applicants');
      const applicant = entityId
        ? applicants.find(
            (item) => item.centerId === center.id && item.publicId === entityId,
          )
        : null;
      setViewingApplicant(applicant ?? null);
      return;
    }

    setActiveTab('posts');
    setViewingApplicant(null);
  }, [applicants, navigate, posts, route]);

  useEffect(() => {
    if (!notice) return;
    const timer = window.setTimeout(() => setNotice(''), 2400);
    return () => window.clearTimeout(timer);
  }, [notice]);

  const selectCenter = (center: ManagedCenter) => {
    navigate(`/my-centers/${center.id}`);
  };

  const returnToCenterList = () => {
    navigate('/my-centers');
  };

  const savePost = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!editingPost) return;
    setPosts((current) =>
      current.map((post) => (post.id === editingPost.id ? editingPost : post)),
    );
    const center = managedCenters.find((item) => item.id === editingPost.centerId);
    navigate(center ? `/my-centers/${center.id}` : '/my-centers');
    setNotice('모집글 수정 내용을 저장했습니다.');
  };

  const openPostManagement = (post: CenterPost) => {
    const center = managedCenters.find((item) => item.id === post.centerId);
    if (center) navigate(`/my-centers/${center.id}/posts/${post.id}`);
  };

  const approveApplicant = (applicantId: number) => {
    setApplicants((current) =>
      current.map((applicant) =>
        applicant.id === applicantId ? { ...applicant, status: '승인 완료' } : applicant,
      ),
    );
    setViewingApplicant((current) =>
      current?.id === applicantId ? { ...current, status: '승인 완료' } : current,
    );
    setNotice('신청자를 승인했습니다.');
  };

  if (!selectedCenter) {
    return (
      <article className="my-centers-list-page">
        <header className="my-centers-list-header">
          <div>
            <p>센터 관리자</p>
            <h2>내 센터 목록</h2>
            <span>{currentUser.nickname}님이 관리 권한을 가진 센터입니다.</span>
          </div>
          <strong>총 {managedCenters.length}개 센터</strong>
        </header>

        <div className="my-centers-table" role="table" aria-label="내 센터 목록">
          <div className="my-centers-table-head" role="row">
            <span role="columnheader">센터</span>
            <span role="columnheader" aria-label="센터 관리" />
          </div>

          {managedCenters.map((center) => (
            <section
              className="my-center-list-row"
              role="row"
              key={center.id}
              tabIndex={0}
              onClick={() => selectCenter(center)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault();
                  selectCenter(center);
                }
              }}
            >
              <div className="my-center-list-name" role="cell">
                <strong>{center.name}</strong>
              </div>
              <button
                type="button"
                className="my-center-list-action"
                onClick={(event) => {
                  event.stopPropagation();
                  selectCenter(center);
                }}
                aria-label={`${center.name} 관리하기`}
              >
                관리하기
              </button>
            </section>
          ))}
        </div>
      </article>
    );
  }

  if (editingPost && !viewingApplicant) {
    return (
      <article className="center-editor-page">
        <div className="my-center-detail-nav">
          <button type="button" onClick={() => navigate(`/my-centers/${selectedCenter.id}`)}>
            모집글 목록으로 돌아가기
          </button>
          <span>POST MANAGEMENT</span>
        </div>

        <header className="center-editor-header">
          <p>모집글 관리</p>
          <h2>{editingPost.title}</h2>
          <span>{selectedCenter.name}</span>
        </header>

        <nav className="post-management-tabs" aria-label="모집글 관리 메뉴">
          <button
            type="button"
            className={postManagerTab === 'edit' ? 'active' : ''}
            onClick={() =>
              navigate(`/my-centers/${selectedCenter.id}/posts/${editingPost.id}`)
            }
          >
            글 수정
          </button>
          <button
            type="button"
            className={postManagerTab === 'applicants' ? 'active' : ''}
            onClick={() =>
              navigate(
                `/my-centers/${selectedCenter.id}/posts/${editingPost.id}/applications`,
              )
            }
          >
            이 글의 신청자
          </button>
          <span>총 {postApplicants.length}명 신청</span>
        </nav>

        {postManagerTab === 'edit' ? (
          <form className="center-editor-form" onSubmit={savePost}>
            <label>
              <span>모집글 제목</span>
              <input
                value={editingPost.title}
                onChange={(event) => setEditingPost({ ...editingPost, title: event.target.value })}
                required
              />
            </label>
            <div className="center-editor-form-row">
              <label>
                <span>구분</span>
                <select
                  value={editingPost.category}
                  onChange={(event) =>
                    setEditingPost({ ...editingPost, category: event.target.value })
                  }
                >
                  <option>봉사</option>
                  <option>기부</option>
                </select>
              </label>
              <label>
                <span>공개 상태</span>
                <select
                  value={editingPost.status}
                  onChange={(event) =>
                    setEditingPost({ ...editingPost, status: event.target.value as PostStatus })
                  }
                >
                  <option>공개 중</option>
                  <option>마감</option>
                </select>
              </label>
            </div>
            <label>
              <span>모집 기간</span>
              <input
                value={editingPost.period}
                onChange={(event) => setEditingPost({ ...editingPost, period: event.target.value })}
                required
              />
            </label>
            <label>
              <span>상세 설명</span>
              <textarea
                rows={8}
                value={editingPost.description}
                onChange={(event) =>
                  setEditingPost({ ...editingPost, description: event.target.value })
                }
                required
              />
            </label>
            <div className="center-editor-actions">
              <button type="button" onClick={() => navigate(`/my-centers/${selectedCenter.id}`)}>
                취소
              </button>
              <button type="submit">수정 내용 저장</button>
            </div>
          </form>
        ) : (
          <div className="center-applicant-table" role="table" aria-label="모집글 신청자 목록">
            <div className="center-applicant-table-head" role="row">
              <span role="columnheader">신청자</span>
              <span role="columnheader">연락처</span>
              <span role="columnheader">신청일</span>
              <span role="columnheader">상태</span>
              <span role="columnheader">제출서류</span>
              <span role="columnheader" aria-label="승인 관리" />
            </div>
            {postApplicants.length === 0 ? (
              <div className="post-applicant-empty">이 모집글에 접수된 신청자가 없습니다.</div>
            ) : (
              postApplicants.map((applicant) => (
                <section
                  className="center-applicant-row"
                  role="row"
                  key={applicant.id}
                  tabIndex={0}
                  onClick={() =>
                    navigate(
                      `/my-centers/${selectedCenter.id}/posts/${editingPost.id}/applications/${applicant.publicId}`,
                    )
                  }
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' || event.key === ' ') {
                      event.preventDefault();
                      navigate(
                        `/my-centers/${selectedCenter.id}/posts/${editingPost.id}/applications/${applicant.publicId}`,
                      );
                    }
                  }}
                >
                  <div className="center-applicant-name" role="cell">
                    <strong>{applicant.name}</strong>
                    <span>{applicant.email}</span>
                  </div>
                  <span role="cell">{applicant.phone}</span>
                  <span role="cell">{applicant.appliedAt}</span>
                  <strong
                    className={`center-applicant-status ${
                      applicant.status === '승인 완료' ? 'approved' : ''
                    }`}
                    role="cell"
                  >
                    {applicant.status}
                  </strong>
                  <button
                    type="button"
                    className="document-view-button"
                    onClick={(event) => {
                      event.stopPropagation();
                      navigate(
                        `/my-centers/${selectedCenter.id}/posts/${editingPost.id}/applications/${applicant.publicId}`,
                      );
                    }}
                    aria-label={`${applicant.name} 제출서류 보기`}
                  >
                    서류보기
                  </button>
                  {applicant.status === '검토 대기' ? (
                    <button
                      type="button"
                      className="applicant-approve-button"
                      onClick={(event) => {
                        event.stopPropagation();
                        approveApplicant(applicant.id);
                      }}
                      aria-label={`${applicant.name} 신청 승인`}
                    >
                      승인
                    </button>
                  ) : (
                    <span className="approval-complete">승인됨</span>
                  )}
                </section>
              ))
            )}
          </div>
        )}
        {notice && <div className="center-notice">{notice}</div>}
      </article>
    );
  }

  if (viewingApplicant) {
    return (
      <article className="applicant-document-page">
        <div className="my-center-detail-nav">
          <button
            type="button"
            onClick={() =>
              navigate(
                editingPost
                  ? `/my-centers/${selectedCenter.id}/posts/${editingPost.id}/applications`
                  : `/my-centers/${selectedCenter.id}/applications`,
              )
            }
          >
            {editingPost ? '이 글의 신청자 목록으로 돌아가기' : '신청자 목록으로 돌아가기'}
          </button>
          <span>APPLICATION DOCUMENTS</span>
        </div>

        <header className="applicant-document-header">
          <div>
            <p>신청자 제출서류</p>
            <h2>{viewingApplicant.name}</h2>
            <span>{viewingApplicant.postTitle}</span>
          </div>
          <strong className={viewingApplicant.status === '승인 완료' ? 'approved' : ''}>
            {viewingApplicant.status}
          </strong>
        </header>

        <section className="applicant-contact-grid">
          <div>
            <span>이메일</span>
            <strong>{viewingApplicant.email}</strong>
          </div>
          <div>
            <span>연락처</span>
            <strong>{viewingApplicant.phone}</strong>
          </div>
          <div>
            <span>신청일</span>
            <strong>{viewingApplicant.appliedAt}</strong>
          </div>
        </section>

        <section className="applicant-motivation">
          <span>신청 동기</span>
          <p>{viewingApplicant.motivation}</p>
        </section>

        <section className="applicant-documents">
          <div className="applicant-section-title">
            <h3>제출서류</h3>
            <span>총 {viewingApplicant.documents.length}개</span>
          </div>
          {viewingApplicant.documents.map((document, index) => (
            <div className="applicant-document-row" key={document}>
              <span>{String(index + 1).padStart(2, '0')}</span>
              <strong>{document}</strong>
              <button type="button" onClick={() => setNotice(`${document} 열람 화면을 확인했습니다.`)}>
                서류 열람
              </button>
            </div>
          ))}
        </section>

        {viewingApplicant.status === '검토 대기' && (
          <button
            type="button"
            className="applicant-detail-approve"
            onClick={() => approveApplicant(viewingApplicant.id)}
          >
            신청 승인하기
          </button>
        )}
        {notice && <div className="center-notice">{notice}</div>}
      </article>
    );
  }

  return (
    <article className="my-center-page">
      <div className="my-center-detail-nav">
        <button type="button" onClick={returnToCenterList}>
          내 센터 목록으로 돌아가기
        </button>
        <span>CENTER MANAGEMENT</span>
      </div>

      <header className="my-center-header">
        <div>
          <p>센터 관리</p>
          <h2>{selectedCenter.name}</h2>
          <span>{selectedCenter.region} · {selectedCenter.type}</span>
        </div>
        <button type="button" disabled>
          모집글 작성
        </button>
      </header>

      <section className="my-center-metrics">
        <div>
          <span>공개 모집글</span>
          <strong>{centerPosts.filter((post) => post.status === '공개 중').length}</strong>
        </div>
        <div>
          <span>끝난 모집글</span>
          <strong>{centerPosts.filter((post) => post.status === '마감').length}</strong>
        </div>
        <div>
          <span>대기 신청자</span>
          <strong>
            {centerApplicants.filter((applicant) => applicant.status === '검토 대기').length}
          </strong>
        </div>
        <div>
          <span>이번 달 참여자</span>
          <strong>{selectedCenter.monthlyParticipants}</strong>
        </div>
      </section>

      <nav className="center-management-tabs" aria-label="센터 관리 메뉴">
        <button
          type="button"
          className={activeTab === 'posts' ? 'active' : ''}
          onClick={() => navigate(`/my-centers/${selectedCenter.id}`)}
        >
          모집글 목록
        </button>
        <button
          type="button"
          className={activeTab === 'applicants' ? 'active' : ''}
          onClick={() => navigate(`/my-centers/${selectedCenter.id}/applications`)}
        >
          신청자 목록
        </button>
        <span>
          {activeTab === 'posts'
            ? `총 ${centerPosts.length}개 모집글`
            : `총 ${centerApplicants.length}명 신청자`}
        </span>
      </nav>

      {activeTab === 'posts' ? (
        <div className="center-post-table" role="table" aria-label="센터 모집글 목록">
          <div className="center-post-table-head" role="row">
            <span role="columnheader">프로그램</span>
            <span role="columnheader">구분</span>
            <span role="columnheader">모집 기간</span>
            <span role="columnheader">상태</span>
            <span role="columnheader">신청자</span>
            <span role="columnheader" aria-label="모집글 관리" />
          </div>
          {centerPosts.map((post) => (
            <section
              className="center-post-row"
              role="row"
              key={post.id}
              tabIndex={0}
              onClick={() => openPostManagement(post)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault();
                  openPostManagement(post);
                }
              }}
            >
              <div className="center-post-title" role="cell">
                <strong>{post.title}</strong>
                <span>{post.description}</span>
              </div>
              <span className="center-post-category" role="cell">{post.category}</span>
              <span role="cell">{post.period}</span>
              <strong
                className={`center-post-status ${post.status === '마감' ? 'closed' : ''}`}
                role="cell"
              >
                {post.status}
              </strong>
              <span role="cell">{post.applicantCount}명</span>
              <button
                type="button"
                onClick={(event) => {
                  event.stopPropagation();
                  openPostManagement(post);
                }}
                aria-label={`${post.title} 관리하기`}
              >
                관리하기
              </button>
            </section>
          ))}
        </div>
      ) : (
        <div className="center-applicant-table" role="table" aria-label="센터 신청자 목록">
          <div className="center-applicant-table-head" role="row">
            <span role="columnheader">신청자</span>
            <span role="columnheader">신청 모집글</span>
            <span role="columnheader">신청일</span>
            <span role="columnheader">상태</span>
            <span role="columnheader">제출서류</span>
            <span role="columnheader" aria-label="승인 관리" />
          </div>
          {centerApplicants.map((applicant) => (
            <section
              className="center-applicant-row"
              role="row"
              key={applicant.id}
              tabIndex={0}
              onClick={() =>
                navigate(`/my-centers/${selectedCenter.id}/applications/${applicant.publicId}`)
              }
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault();
                  navigate(`/my-centers/${selectedCenter.id}/applications/${applicant.publicId}`);
                }
              }}
            >
              <div className="center-applicant-name" role="cell">
                <strong>{applicant.name}</strong>
                <span>{applicant.email}</span>
              </div>
              <span role="cell">{applicant.postTitle}</span>
              <span role="cell">{applicant.appliedAt}</span>
              <strong
                className={`center-applicant-status ${
                  applicant.status === '승인 완료' ? 'approved' : ''
                }`}
                role="cell"
              >
                {applicant.status}
              </strong>
              <button
                type="button"
                className="document-view-button"
                onClick={(event) => {
                  event.stopPropagation();
                  navigate(`/my-centers/${selectedCenter.id}/applications/${applicant.publicId}`);
                }}
                aria-label={`${applicant.name} 제출서류 보기`}
              >
                서류보기
              </button>
              {applicant.status === '검토 대기' ? (
                <button
                  type="button"
                  className="applicant-approve-button"
                  onClick={(event) => {
                    event.stopPropagation();
                    approveApplicant(applicant.id);
                  }}
                  aria-label={`${applicant.name} 신청 승인`}
                >
                  승인
                </button>
              ) : (
                <span className="approval-complete">승인됨</span>
              )}
            </section>
          ))}
        </div>
      )}
      {notice && <div className="center-notice">{notice}</div>}
    </article>
  );
};
