import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { SessionUser } from '../../types';
import { fetchMyApplications, type ApplicationResponse } from '../../services/applicationApi';
import { fetchMyPosts, type PostItem } from '../../services/communityApi';
import { fetchMyProfile, updateMyProfile, type UserProfile } from '../../services/authApi';

interface MyPageProps {
  currentUser: SessionUser;
}

const statusLabel: Record<string, string> = {
  APPLIED: '신청 완료',
  IN_REVIEW: '검토 중',
  REVISION_REQUESTED: '수정 요청',
  APPROVED: '승인 완료',
  REJECTED: '신청 거절',
  CANCELLED: '취소',
  REQUIRED: '제출 필요',
  SUBMITTED: '제출 완료',
  DRAFT: '작성 중',
};

const postCategoryLabel: Record<string, string> = {
  REVIEW: '봉사 후기',
  RECRUIT: '동행 모집',
  FREE: '자율 수다',
  GENERAL: '자율 수다',
};

export const MyPage: React.FC<MyPageProps> = ({ currentUser }) => {
  const navigate = useNavigate();
  const route = useParams()['*'] ?? '';
  const [section, entityId] = route.split('/').filter(Boolean);
  const activeSection = section === 'posts' ? 'posts' : 'applications';
  const isProfileEditing = section === 'profile';

  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [applications, setApplications] = useState<ApplicationResponse[]>([]);
  const [posts, setPosts] = useState<PostItem[]>([]);
  const [nickname, setNickname] = useState(currentUser.nickname);
  const [phone, setPhone] = useState('');
  const [region, setRegion] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    Promise.all([fetchMyProfile(), fetchMyApplications(), fetchMyPosts()])
      .then(([nextProfile, nextApplications, nextPosts]) => {
        setProfile(nextProfile);
        setNickname(nextProfile.nickname);
        setPhone(nextProfile.phone || '');
        setRegion(nextProfile.region || '');
        setApplications(nextApplications);
        setPosts(nextPosts);
      })
      .catch((error) => {
        setNotice(error instanceof Error ? error.message : '마이페이지를 불러오지 못했습니다.');
      });
  }, []);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'auto' });
  }, [route]);

  const selectedApplication = useMemo(
    () => applications.find((application) => application.publicId === entityId) ?? null,
    [applications, entityId],
  );

  const saveProfile = async (event: React.FormEvent) => {
    event.preventDefault();
    try {
      const updated = await updateMyProfile({ nickname, phone, region });
      setProfile(updated);
      setNotice('프로필을 저장했습니다.');
      navigate('/my-page');
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '프로필 저장에 실패했습니다.');
    }
  };

  return (
    <article className="user-my-page">
      <header className="user-my-page-header">
        <h2>마이페이지</h2>
        {!isProfileEditing && (
          <button type="button" onClick={() => navigate('/my-page/profile')}>
            프로필 수정
          </button>
        )}
      </header>

      <section className="user-profile-section">
        <div>
          <span>계정</span>
          <strong>{profile?.email || currentUser.email}</strong>
        </div>
        <div>
          <span>닉네임</span>
          <strong>{profile?.nickname || currentUser.nickname}</strong>
        </div>
        <div>
          <span>온기 온도</span>
          <strong>{profile?.temperature ?? 0}°C</strong>
        </div>
      </section>

      {isProfileEditing ? (
        <section className="user-profile-editor">
          <div className="user-section-back">
            <button type="button" onClick={() => navigate('/my-page')}>마이페이지로 돌아가기</button>
            <span>PROFILE EDIT</span>
          </div>
          <form className="user-profile-form" onSubmit={saveProfile}>
            <label>
              닉네임
              <input value={nickname} onChange={(event) => setNickname(event.target.value)} required />
            </label>
            <label>
              연락처
              <input value={phone} onChange={(event) => setPhone(event.target.value)} />
            </label>
            <label>
              지역
              <input value={region} onChange={(event) => setRegion(event.target.value)} />
            </label>
            <button type="submit">수정 내용 저장</button>
          </form>
        </section>
      ) : selectedApplication ? (
        <section className="user-application-detail">
          <div className="user-section-back">
            <button type="button" onClick={() => navigate('/my-page/applications')}>
              나의 신청내역으로 돌아가기
            </button>
            <span>APPLICATION DOCUMENTS</span>
          </div>
          <header>
            <div>
              <span>신청 프로그램</span>
              <h3>{selectedApplication.opportunityTitle}</h3>
              <p>{selectedApplication.organizationName}</p>
            </div>
            <strong>{statusLabel[selectedApplication.status] || selectedApplication.status}</strong>
          </header>
          <div className="user-application-facts">
            <div><span>신청자</span><strong>{selectedApplication.applicantName}</strong></div>
            <div><span>신청일</span><strong>{selectedApplication.submittedAt?.slice(0, 10) || '-'}</strong></div>
            <div><span>참여 희망일</span><strong>{selectedApplication.participationDate || '-'}</strong></div>
          </div>
          <section className="user-commitment-preview">
            <span>전자 약정서</span>
            <h4>{selectedApplication.commitment?.title || '약정서 미작성'}</h4>
            <p>
              {selectedApplication.commitment?.renderedContent ||
                selectedApplication.specialConditions ||
                '작성된 약정서 내용이 없습니다.'}
            </p>
          </section>
          <div className="user-document-list">
            <div className="user-application-heading">
              <h3>내가 제출한 서류</h3>
              <span>총 {selectedApplication.documents.length}건</span>
            </div>
            {selectedApplication.documents.length === 0 ? (
              <p className="user-application-empty">제출된 서류가 없습니다.</p>
            ) : (
              selectedApplication.documents.map((document, index) => (
                <div className="user-document-row" key={document.code}>
                  <span>{String(index + 1).padStart(2, '0')}</span>
                  <div>
                    <strong>{document.name}</strong>
                    <p>{document.description || (document.required ? '필수 제출 서류' : '선택 제출 서류')}</p>
                  </div>
                  <em>{statusLabel[document.status] || document.status}</em>
                </div>
              ))
            )}
          </div>
        </section>
      ) : (
        <>
          <nav className="user-my-page-tabs" aria-label="마이페이지 활동 메뉴">
            <button
              type="button"
              className={activeSection === 'applications' ? 'active' : ''}
              onClick={() => navigate('/my-page/applications')}
            >
              나의 신청내역
            </button>
            <button
              type="button"
              className={activeSection === 'posts' ? 'active' : ''}
              onClick={() => navigate('/my-page/posts')}
            >
              나의 글
            </button>
            <span>
              {activeSection === 'applications'
                ? `총 ${applications.length}건 신청`
                : `총 ${posts.length}개 글`}
            </span>
          </nav>

          {activeSection === 'applications' ? (
            <section className="user-application-section">
              {applications.length === 0 ? (
                <p className="user-application-empty">아직 신청한 프로그램이 없습니다.</p>
              ) : (
                applications.map((application) => (
                  <button
                    type="button"
                    className="user-application-row"
                    key={application.publicId}
                    onClick={() => navigate(`/my-page/applications/${application.publicId}`)}
                  >
                    <div>
                      <strong>{application.opportunityTitle}</strong>
                      <span>{application.organizationName}</span>
                    </div>
                    <span>{application.submittedAt?.slice(0, 10) || '-'}</span>
                    <strong>{statusLabel[application.status] || application.status}</strong>
                    <em>서류 확인</em>
                  </button>
                ))
              )}
            </section>
          ) : (
            <section className="user-post-section">
              {posts.length === 0 ? (
                <p className="user-application-empty">아직 커뮤니티에 작성한 글이 없습니다.</p>
              ) : (
                posts.map((post) => (
                  <button
                    type="button"
                    className="user-post-row"
                    key={post.id}
                    onClick={() => navigate(`/community/posts/${post.id}`)}
                  >
                    <div>
                      <strong>{post.title}</strong>
                      <span>{post.contentSnippet || post.content}</span>
                    </div>
                    <span>{postCategoryLabel[post.category] || post.category}</span>
                    <span>{post.createdAt ? new Date(post.createdAt).toLocaleDateString('ko-KR') : '-'}</span>
                    <em>글 보기</em>
                  </button>
                ))
              )}
            </section>
          )}
        </>
      )}
      {notice && <div className="center-notice">{notice}</div>}
    </article>
  );
};
