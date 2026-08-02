import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { SessionUser } from '../../types';
import {
  fetchMyApplications,
  renewCommitment,
  type ApplicationResponse,
} from '../../services/applicationApi';
import { fetchMyPosts, type PostItem } from '../../services/communityApi';
import { fetchMyProfile, updateMyProfile, type UserProfile } from '../../services/authApi';
import { PROFILE_UPDATED_EVENT } from '../../hooks/useMyRegion';
import { ActivityCalendar } from './ActivityCalendar';
import { BadgeGrid } from '../roadmap/BadgeGrid';
import { splitSentences } from '../../utils/text';
import { computeBadges, DONE_APPLICATION_STATUSES } from '../roadmap/badgeProgress';
import {
  fetchMyClmDocuments,
  fetchClmDocument,
  fetchClmDocumentFiles,
  loadClmDocumentFile,
  type ClmDocumentDto,
  type ClmDocumentFileDto,
} from '../../services/clmApi';

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
  const [clmDocuments, setClmDocuments] = useState<ClmDocumentDto[]>([]);
  const [clmFiles, setClmFiles] = useState<ClmDocumentFileDto[]>([]);
  const [previewPdfUrl, setPreviewPdfUrl] = useState('');
  const [previewPdfTitle, setPreviewPdfTitle] = useState('');
  const [renewingCommitmentId, setRenewingCommitmentId] = useState('');
  const [nickname, setNickname] = useState(currentUser.nickname);
  const [phone, setPhone] = useState('');
  const [region, setRegion] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    Promise.all([fetchMyProfile(), fetchMyApplications(), fetchMyPosts(), fetchMyClmDocuments()])
      .then(([nextProfile, nextApplications, nextPosts, nextClmDocuments]) => {
        setProfile(nextProfile);
        setNickname(nextProfile.nickname);
        setPhone(nextProfile.phone || '');
        setRegion(nextProfile.region || '');
        setApplications(nextApplications);
        setPosts(nextPosts);
        setClmDocuments(nextClmDocuments);
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

  const selectedClmDocument = useMemo(
    () => selectedApplication
      ? clmDocuments.find((document) => document.volunteerId === selectedApplication.opportunityId) ?? clmDocuments[0] ?? null
      : clmDocuments[0] ?? null,
    [clmDocuments, selectedApplication],
  );

  // 목록 조회는 모두싸인 상태를 동기화하지 않는다. 상세를 한 번 더 불러
  // 서명 완료 여부와 감사 메시지를 최신 상태로 맞춘 뒤 보관 파일을 가져온다.
  const selectedClmDocumentId = selectedClmDocument?.id ?? null;

  useEffect(() => {
    if (!selectedClmDocumentId) {
      setClmFiles([]);
      return;
    }
    let cancelled = false;
    fetchClmDocument(selectedClmDocumentId)
      .then(async (fresh) => {
        if (cancelled) return;
        setClmDocuments((previous) =>
          previous.map((document) => (document.id === fresh.id ? fresh : document)),
        );
        if (fresh.status !== 'SIGNED') {
          setClmFiles([]);
          return;
        }
        const files = await fetchClmDocumentFiles(fresh.id);
        if (!cancelled) setClmFiles(files);
      })
      .catch((error) => {
        if (cancelled) return;
        setNotice(error instanceof Error ? error.message : '전자서명 파일을 불러오지 못했습니다.');
      });
    return () => {
      cancelled = true;
    };
  }, [selectedClmDocumentId]);

  const renewPledge = async (commitmentPublicId: string) => {
    setRenewingCommitmentId(commitmentPublicId);
    try {
      const renewed = await renewCommitment(commitmentPublicId);
      setApplications((previous) =>
        previous.map((application) =>
          application.commitment?.publicId === commitmentPublicId
            ? { ...application, commitment: renewed }
            : application,
        ),
      );
      setNotice(`정기 약정을 갱신했습니다. 다음 갱신일은 ${renewed.renewalDueAt || '-'}입니다.`);
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '약정 갱신에 실패했습니다.');
    } finally {
      setRenewingCommitmentId('');
    }
  };

  const saveProfile = async (event: React.FormEvent) => {
    event.preventDefault();
    try {
      const updated = await updateMyProfile({ nickname, phone, region });
      setProfile(updated);
      // 활동 지역이 바뀌면 홈·소식 화면의 지역 소식도 곧바로 따라오게 한다.
      window.dispatchEvent(new Event(PROFILE_UPDATED_EVENT));
      setNotice('프로필을 저장했습니다.');
      navigate('/my-page');
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '프로필 저장에 실패했습니다.');
    }
  };

  const previewClmFile = async (
    documentId: number,
    file: ClmDocumentFileDto,
  ) => {
    try {
      const url = await loadClmDocumentFile(documentId, file.id);
      if (previewPdfUrl) URL.revokeObjectURL(previewPdfUrl);
      setPreviewPdfUrl(url);
      setPreviewPdfTitle(
        file.fileType === 'PLEDGE_DRAFT_PDF'
          ? 'AI 맞춤 약정서 PDF'
          : file.fileType === 'SIGNED_DOCUMENT'
          ? '모두싸인 최종 서명 완료 약정서'
          : '모두싸인 감사추적 인증서'
      );
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '전자서명 파일을 열지 못했습니다.');
    }
  };

  const closePdfPreview = () => {
    if (previewPdfUrl) URL.revokeObjectURL(previewPdfUrl);
    setPreviewPdfUrl('');
    setPreviewPdfTitle('');
  };

  const displayName = profile?.nickname || currentUser.nickname;
  const accountName = profile?.name || displayName;
  const temperature = profile?.temperature ?? 0;
  const activeApplicationCount = applications.filter((application) =>
    ['APPLIED', 'IN_REVIEW', 'REVISION_REQUESTED'].includes(application.status),
  ).length;
  const signedDocumentCount = clmDocuments.filter((document) => document.status === 'SIGNED').length;
  // 로드맵과 같은 기준으로 계산한다. 이미 불러온 데이터를 재사용해 추가 요청이 없다.
  const badges = useMemo(
    () =>
      computeBadges({
        temperature: profile?.temperature ?? 0,
        applicationCount: applications.length,
        completedCount: applications.filter((application) =>
          DONE_APPLICATION_STATUSES.includes(application.status),
        ).length,
        signedCount: signedDocumentCount,
      }),
    [profile, applications, signedDocumentCount],
  );
  const earnedBadgeCount = badges.filter((badge) => badge.current >= badge.goal).length;

  const profileCompletion = Math.round(
    ([profile?.email || currentUser.email, displayName, profile?.phone, profile?.region]
      .filter(Boolean).length / 4) * 100,
  );
  const roleLabel = currentUser.role === 'OPERATOR'
    ? '운영진'
    : currentUser.role === 'CENTER_MANAGER'
      ? '센터 관리자'
      : '일반 회원';
  const joinedAt = profile?.createdAt
    ? `${new Date(profile.createdAt).getFullYear()}년 ${new Date(profile.createdAt).getMonth() + 1}월 가입`
    : '잇다 회원';

  return (
    <article className="user-my-page">
      <header className="user-my-page-header">
        <div>
          <span className="user-page-eyebrow">MY ITDA</span>
          <h2>마이페이지</h2>
          <p>나의 선행 활동과 전자 약정 진행 상태를 한눈에 확인하세요.</p>
        </div>
        {!isProfileEditing && (
          <button type="button" onClick={() => navigate('/my-page/profile')}>
            프로필 관리 <span aria-hidden="true">→</span>
          </button>
        )}
      </header>

      <section className="user-profile-section" aria-label="내 계정 요약">
        <div className="user-profile-identity">
          <div className="user-profile-avatar" aria-hidden="true">
            {displayName.trim().charAt(0).toUpperCase() || 'P'}
          </div>
          <div>
            <span className="user-role-badge">{roleLabel}</span>
            <h3>{accountName}님, 반가워요!</h3>
            <p>{profile?.email || currentUser.email}</p>
          </div>
        </div>
        <dl className="user-profile-meta">
          <div>
            <dt>활동 지역</dt>
            <dd>{profile?.region || '아직 등록되지 않았어요'}</dd>
          </div>
          <div>
            <dt>연락처</dt>
            <dd>{profile?.phone || '아직 등록되지 않았어요'}</dd>
          </div>
          <div>
            <dt>가입 정보</dt>
            <dd>{joinedAt}</dd>
          </div>
        </dl>
        <div className="user-warmth-card">
          <div className="user-warmth-heading">
            <div>
              <span>나의 온기</span>
              <strong>{temperature.toFixed(1)}°C</strong>
            </div>
            <em>WARMTH LEVEL</em>
          </div>
          <div
            className="user-warmth-track"
            role="progressbar"
            aria-label="나의 온기 온도"
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={Math.min(temperature, 100)}
          >
            <span style={{ width: `${Math.min(Math.max(temperature, 0), 100)}%` }} />
          </div>
          <p>선행 활동을 이어가며 나만의 온기를 높여보세요.</p>
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
          {selectedClmDocument?.completionMessage && (
            <section className="user-gratitude-card">
              <span>SIGNED WITH HEART</span>
              {splitSentences(selectedClmDocument.completionMessage).map((sentence) => (
                <p key={sentence}>{sentence}</p>
              ))}
              <small>
                {selectedClmDocument.completionMessageSource === 'UPSTAGE_SOLAR'
                  ? 'Upstage Solar가 약정 내용을 읽고 남긴 인사입니다.'
                  : '약정 내용을 바탕으로 남긴 인사입니다.'}
              </small>
            </section>
          )}
          <section className="user-commitment-preview">
            <span>전자 약정서</span>
            <h4>{selectedApplication.commitment?.title || '약정서 미작성'}</h4>
            <p>
              {selectedApplication.commitment?.renderedContent ||
                selectedApplication.specialConditions ||
                '작성된 약정서 내용이 없습니다.'}
            </p>
          </section>
          {selectedApplication.commitment?.renewalDueAt && (
            <section
              className={`user-renewal-card${
                selectedApplication.commitment.renewalStatus === 'DUE' ? ' due' : ''
              }`}
            >
              <div>
                <span>
                  정기 약정 갱신
                  {selectedApplication.commitment.renewalStatus === 'DUE' && (
                    <em className="user-renewal-badge">갱신 필요</em>
                  )}
                </span>
                <strong>다음 갱신일 {selectedApplication.commitment.renewalDueAt}</strong>
                <p>
                  {selectedApplication.commitment.renewalStatus === 'DUE'
                    ? '갱신일이 지났습니다. 지금 갱신하면 다음 주기까지 약정이 이어집니다.'
                    : '갱신일이 되면 버튼 한 번으로 다음 주기까지 약정을 이어갈 수 있습니다.'}
                </p>
              </div>
              {selectedApplication.commitment.status === 'ACTIVE' &&
                ['MONTHLY', 'ANNUAL'].includes(
                  selectedApplication.commitment.pledgeFrequency || '',
                ) && (
                  <button
                    type="button"
                    disabled={renewingCommitmentId === selectedApplication.commitment.publicId}
                    onClick={() => renewPledge(selectedApplication.commitment!.publicId)}
                  >
                    {renewingCommitmentId === selectedApplication.commitment.publicId
                      ? '갱신하는 중…'
                      : '지금 갱신하기'}
                  </button>
                )}
            </section>
          )}
          <div className="user-document-list">
            <div className="user-application-heading">
              <h3>내가 제출한 서류</h3>
              <span>총 {selectedApplication.documents.length + clmFiles.length}건</span>
            </div>
            {clmFiles.map((file, index) => (
              <button
                type="button"
                className="user-document-row"
                key={file.id}
                onClick={() => previewClmFile(selectedClmDocument?.id || 23, file)}
                style={{ cursor: 'pointer', textAlign: 'left', width: '100%' }}
              >
                <span>{String(index + 1).padStart(2, '0')}</span>
                <div>
                  <strong>
                    {file.fileType === 'PLEDGE_DRAFT_PDF'
                      ? '🤖 Upstage AI 작성 맞춤 약정서 (PDF)'
                      : file.fileType === 'SIGNED_DOCUMENT'
                      ? '✍️ 모두싸인 최종 서명 완료 약정서 (PDF)'
                      : '🛡️ 모두싸인 감사추적 인증서 (Audit Trail)'}
                  </strong>
                  <p>{file.originalName} · {(file.sizeBytes / 1024).toFixed(1)}KB · 법적효력 검증완료 (SIGNED)</p>
                </div>
                <em style={{ color: '#ff3b30', fontWeight: 'bold' }}>PDF 열람 📥</em>
              </button>
            ))}
            {selectedApplication.documents.map((document, index) => (
              <div className="user-document-row" key={document.code}>
                <span>{String(clmFiles.length + index + 1).padStart(2, '0')}</span>
                <div>
                  <strong>{document.name}</strong>
                  <p>{document.description || (document.required ? '필수 제출 서류' : '선택 제출 서류')}</p>
                </div>
                <em>{statusLabel[document.status] || document.status}</em>
              </div>
            ))}
            {selectedApplication.documents.length === 0 && clmFiles.length === 0 && (
              <p className="user-application-empty">제출된 서류가 없습니다.</p>
            )}
          </div>
        </section>
      ) : (
        <>
          <section className="user-activity-summary" aria-label="내 활동 현황">
            <article>
              <span>전체 신청</span>
              <strong>{applications.length}<small>건</small></strong>
              <p>지금까지 참여를 신청한 활동</p>
            </article>
            <article>
              <span>진행 중</span>
              <strong>{activeApplicationCount}<small>건</small></strong>
              <p>신청부터 승인까지 진행 중인 활동</p>
            </article>
            <article className="accent">
              <span>전자서명 완료</span>
              <strong>{signedDocumentCount}<small>건</small></strong>
              <p>안전하게 보관 중인 약정 문서</p>
            </article>
            <article>
              <span>작성한 글</span>
              <strong>{posts.length}<small>개</small></strong>
              <p>커뮤니티에 나눈 선행 이야기</p>
            </article>
          </section>

          <section className="user-dashboard-grid">
            <aside className="user-quick-panel">
              <div className="user-panel-heading">
                <span>QUICK MENU</span>
                <h3>바로가기</h3>
              </div>
              <button type="button" onClick={() => navigate('/volunteer')}>
                <span className="user-quick-icon" aria-hidden="true">+</span>
                <span><strong>새로운 선행 찾기</strong><small>봉사·기부 프로그램 둘러보기</small></span>
                <em aria-hidden="true">→</em>
              </button>
              <button type="button" onClick={() => navigate('/community')}>
                <span className="user-quick-icon" aria-hidden="true">✦</span>
                <span><strong>선행 이야기 나누기</strong><small>커뮤니티에서 경험 공유하기</small></span>
                <em aria-hidden="true">→</em>
              </button>
              <button type="button" onClick={() => navigate('/my-page/profile')}>
                <span className="user-quick-icon" aria-hidden="true">✓</span>
                <span><strong>프로필 완성하기</strong><small>현재 프로필 완성도 {profileCompletion}%</small></span>
                <em aria-hidden="true">→</em>
              </button>
            </aside>

            <section className="user-activity-panel">
              <div className="user-panel-heading user-activity-panel-heading">
                <div>
                  <span>MY ACTIVITY</span>
                  <h3>최근 활동</h3>
                </div>
                <span>
                  {activeSection === 'applications'
                    ? `총 ${applications.length}건`
                    : `총 ${posts.length}개`}
                </span>
              </div>
              <nav className="user-my-page-tabs" aria-label="마이페이지 활동 메뉴">
                <button
                  type="button"
                  className={activeSection === 'applications' ? 'active' : ''}
                  onClick={() => navigate('/my-page/applications')}
                >
                  신청 내역
                </button>
                <button
                  type="button"
                  className={activeSection === 'posts' ? 'active' : ''}
                  onClick={() => navigate('/my-page/posts')}
                >
                  작성한 글
                </button>
              </nav>

              {activeSection === 'applications' ? (
                <section className="user-application-section">
                  {applications.length === 0 ? (
                    <div className="user-application-empty">
                      <span className="user-empty-icon" aria-hidden="true">♡</span>
                      <strong>아직 신청한 프로그램이 없어요</strong>
                      <p>나와 잘 맞는 선행 활동을 찾고 첫 온기를 남겨보세요.</p>
                      <button type="button" onClick={() => navigate('/volunteer')}>프로그램 둘러보기</button>
                    </div>
                  ) : (
                    applications.map((application) => (
                      <button
                        type="button"
                        className="user-application-row"
                        key={application.publicId}
                        onClick={() => navigate(`/my-page/applications/${application.publicId}`)}
                      >
                        <div>
                          <strong>
                            {application.opportunityTitle}
                            {application.commitment?.renewalStatus === 'DUE' && (
                              <em className="user-renewal-badge">갱신 필요</em>
                            )}
                          </strong>
                          <span>{application.organizationName}</span>
                        </div>
                        <span>{application.submittedAt?.slice(0, 10) || '-'}</span>
                        <strong className="user-status-badge">{statusLabel[application.status] || application.status}</strong>
                        <em>서류 확인 <span aria-hidden="true">→</span></em>
                      </button>
                    ))
                  )}
                </section>
              ) : (
                <section className="user-post-section">
                  {posts.length === 0 ? (
                    <div className="user-application-empty">
                      <span className="user-empty-icon" aria-hidden="true">✎</span>
                      <strong>아직 작성한 이야기가 없어요</strong>
                      <p>작은 선행의 순간을 기록하고 따뜻한 경험을 나눠보세요.</p>
                      <button type="button" onClick={() => navigate('/community')}>커뮤니티 둘러보기</button>
                    </div>
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
                        <em>글 보기 <span aria-hidden="true">→</span></em>
                      </button>
                    ))
                  )}
                </section>
              )}
            </section>
          </section>

          <ActivityCalendar showNotice={setNotice} />

          <section className="user-badge-section" aria-label="내 뱃지">
            <div className="user-panel-heading user-badge-heading">
              <div>
                <span>MY BADGES</span>
                <h3>내 뱃지</h3>
              </div>
              <button type="button" onClick={() => navigate('/roadmap')}>
                {earnedBadgeCount}/{badges.length} 획득 · 성장의 길 보기 →
              </button>
            </div>
            <BadgeGrid badges={badges} />
          </section>
        </>
      )}
      {previewPdfUrl && (
        <div className="auth-modal-backdrop" role="presentation" style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.85)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 99999 }}>
          <section className="auth-modal" role="dialog" aria-modal="true" aria-label={previewPdfTitle} style={{ background: '#fff', width: '90%', maxWidth: '800px', borderRadius: '16px', border: '3px solid #111', padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div className="user-application-heading" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '2px solid #111', paddingBottom: '12px' }}>
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 'bold' }}>📜 {previewPdfTitle}</h3>
              <div style={{ display: 'flex', gap: '8px' }}>
                <a
                  href={previewPdfUrl}
                  download="pixelcare-signed-document.pdf"
                  style={{ padding: '6px 14px', background: '#ff3b30', color: '#fff', border: '1.5px solid #111', borderRadius: '6px', textDecoration: 'none', fontWeight: 'bold', fontSize: '12px' }}
                >
                  내려받기 💾
                </a>
                <button type="button" onClick={closePdfPreview} style={{ padding: '6px 14px', background: '#111', color: '#fff', border: 'none', borderRadius: '6px', cursor: 'pointer', fontWeight: 'bold' }}>
                  닫기 ✖
                </button>
              </div>
            </div>
            <iframe
              title={previewPdfTitle}
              src={previewPdfUrl}
              style={{ width: '100%', height: '65vh', border: '1px solid #ccc', borderRadius: '8px' }}
            />
          </section>
        </div>
      )}
      {notice && <div className="center-notice">{notice}</div>}
    </article>
  );
};
