import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { CenterActivityNotePanel } from './CenterActivityNotePanel';
import { useNavigate, useParams } from 'react-router-dom';
import type { SessionUser } from '../../types';
import {
  closeManagedOpportunity,
  createManagedOpportunity,
  fetchManagedOpportunities,
  fetchManagedOrganizations,
  fetchOrganizationDashboard,
  publishManagedOpportunity,
  updateManagedOpportunity,
  type ManagedOpportunity,
} from '../../services/managementApi';
import {
  approveApplication,
  fetchOpportunityApplications,
  type ApplicationResponse,
} from '../../services/applicationApi';
import {
  fetchClmDocumentFiles,
  fetchManagerApplicationClmDocuments,
  loadClmDocumentFile,
  type ClmDocumentDto,
  type ClmDocumentFileDto,
} from '../../services/clmApi';
import {
  fetchConnectRequests,
  handleConnectRequest,
  type ConnectRequestItem,
} from '../../services/connectApi';

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
  totalCommitments: number;
  signedCommitments: number;
  awaitingSignature: number;
  signedPledgeAmount: number;
  renewalDueSoon: number;
}

type PostStatus = '공개 중' | '마감';
type ApplicantStatus = '검토 대기' | '승인 완료';
type CenterTab = 'posts' | 'applicants' | 'connect';
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
  raw: ManagedOpportunity;
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

/* 이전 화면 확인용 하드코딩 데이터. 실제 화면은 아래 API 매핑만 사용한다.
const initialManagedCenters: ManagedCenter[] = [
  {
    id: 1,
    name: '잇다 데모 센터',
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
*/

const formatPeriod = (start?: string, end?: string) => {
  if (!start && !end) return '상시 모집';
  const format = (value?: string) => value ? value.slice(0, 10).replaceAll('-', '.') : '-';
  return `${format(start)} – ${format(end)}`;
};

/** datetime-local 입력은 'YYYY-MM-DDTHH:mm'만 받는다. 서버 ISO 문자열을 잘라 맞춘다. */
const toDateTimeLocal = (value?: string | null) => (value ? value.slice(0, 16) : '');

/**
 * 모집 마감 → 활동 시작 → 활동 종료 순서를 확인한다.
 * 입력 순서가 자유로워 브라우저 min 속성만으로는 뒤늦게 뒤집힌 값을 잡지 못한다.
 */
const validateSchedule = (
  recruitmentEnd?: string | null,
  activityStart?: string | null,
  activityEnd?: string | null,
) => {
  if (recruitmentEnd && activityStart && activityStart < recruitmentEnd) {
    return '활동 시작은 모집 마감 이후여야 합니다.';
  }
  if (activityStart && activityEnd && activityEnd < activityStart) {
    return '활동 종료는 활동 시작 이후여야 합니다.';
  }
  return '';
};

const mapOpportunity = (opportunity: ManagedOpportunity): CenterPost => ({
  id: opportunity.id,
  centerId: opportunity.organizationId,
  title: opportunity.title,
  category: opportunity.type === 'VOLUNTEER' ? '봉사' : '기부',
  period: formatPeriod(
    opportunity.recruitmentStartDateTime,
    opportunity.recruitmentEndDateTime,
  ),
  status: opportunity.status === 'PUBLISHED' ? '공개 중' : '마감',
  applicantCount: opportunity.applicantCount,
  description: opportunity.description,
  raw: opportunity,
});

const mapApplication = (
  application: ApplicationResponse,
  index: number,
): CenterApplicant => ({
  id: index + 1,
  publicId: application.publicId,
  centerId: application.organizationId,
  postId: application.opportunityId,
  name: application.applicantName || '이름 미등록',
  email: application.applicantEmail,
  phone: '-',
  postTitle: application.opportunityTitle,
  appliedAt: application.submittedAt?.slice(0, 10) || '-',
  status: application.status === 'APPROVED' ? '승인 완료' : '검토 대기',
  motivation: application.specialConditions || '별도 전달사항이 없습니다.',
  documents: [
    ...(application.commitment ? [application.commitment.title] : []),
    ...application.documents.map((document) => document.name),
  ],
});

function CenterApplicationLevel({
  step,
  title,
  empty,
  items,
  selectedId,
  onSelect,
}: {
  step: string;
  title: string;
  empty: string;
  items: Array<{ id: string; label: string; meta?: string }>;
  selectedId: string | null;
  onSelect: (id: string) => void;
}) {
  return (
    <section className="operator-document-level">
      <div><span>{step}</span><h3>{title}</h3></div>
      {items.length === 0 ? <p>{empty}</p> : items.map((item) => (
        <button
          type="button"
          key={item.id}
          className={item.id === selectedId ? 'active' : ''}
          onClick={() => onSelect(item.id)}
        >
          <strong>{item.label}</strong>
          {item.meta && <span>{item.meta}</span>}
        </button>
      ))}
    </section>
  );
}

function CenterSignedDocumentPanel({ applicationPublicId }: { applicationPublicId: string }) {
  const [documents, setDocuments] = useState<ClmDocumentDto[]>([]);
  const [files, setFiles] = useState<Array<ClmDocumentFileDto & { documentId: number }>>([]);
  const [loading, setLoading] = useState(true);
  const [openingFileId, setOpeningFileId] = useState<number | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    fetchManagerApplicationClmDocuments(applicationPublicId)
      .then(async (items) => {
        const fileGroups = await Promise.all(items.map(async (document) => {
          // 상태 동기화가 어긋나 있어도 보관된 증빙은 존재할 수 있으므로 항상 조회하고,
          // 한 건의 실패가 목록 전체를 지우지 않게 문서 단위로 방어한다.
          try {
            const archived = await fetchClmDocumentFiles(document.id);
            return archived
              .filter((file) => file.fileType === 'SIGNED_DOCUMENT' || file.fileType === 'AUDIT_TRAIL')
              .map((file) => ({ ...file, documentId: document.id }));
          } catch {
            return [];
          }
        }));
        if (!active) return;
        setDocuments(items);
        setFiles(fileGroups.flat());
      })
      .catch((reason) => active && setError(
        reason instanceof Error ? reason.message : '전자서명 문서를 불러오지 못했습니다.',
      ))
      .finally(() => active && setLoading(false));
    return () => { active = false; };
  }, [applicationPublicId]);

  const openFile = async (file: ClmDocumentFileDto & { documentId: number }) => {
    const popup = window.open('', '_blank');
    setOpeningFileId(file.id);
    try {
      const url = await loadClmDocumentFile(file.documentId, file.id);
      if (popup) popup.location.href = url;
      else window.open(url, '_blank', 'noopener,noreferrer');
      window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (reason) {
      popup?.close();
      setError(reason instanceof Error ? reason.message : '파일을 열지 못했습니다.');
    } finally {
      setOpeningFileId(null);
    }
  };

  const statusLabel = (status: ClmDocumentDto['status']) => ({
    PENDING_SIGNATURE: '서명 대기', SIGNING: '서명 진행 중', PARTIALLY_SIGNED: '일부 서명',
    SIGNED: '서명 완료', REJECTED: '서명 거절', CANCELED: '요청 취소',
    SIGNING_CANCELED: '서명 취소',
  })[status];

  return (
    <section className="center-signed-documents">
      <header>
        <div><span>MODUSIGN ARCHIVE</span><h3>전자서명 원본 보관</h3></div>
        <strong>{files.length}개 원본</strong>
      </header>
      {loading ? (
        <p className="center-document-state">서명 상태와 보관 파일을 확인하는 중입니다…</p>
      ) : error ? (
        <p className="center-document-state error">{error}</p>
      ) : documents.length === 0 ? (
        <p className="center-document-state">아직 모두싸인 전자서명 요청이 생성되지 않았습니다.</p>
      ) : (
        <>
          <div className="center-signature-statuses">
            {documents.map((document) => (
              <div key={document.id}>
                <span>문서 #{document.id}</span>
                <strong className={document.status === 'SIGNED' ? 'signed' : ''}>
                  {statusLabel(document.status)}
                </strong>
                <small>{document.signedAt ? `서명일 ${document.signedAt.slice(0, 10)}` : document.volunteerTitle}</small>
              </div>
            ))}
          </div>
          {files.length === 0 ? (
            <p className="center-document-state">서명이 완료되면 서명 PDF와 감사추적증명서가 이곳에 보관됩니다.</p>
          ) : (
            <div className="center-real-file-list">
              {files.map((file) => (
                <div key={`${file.documentId}-${file.id}`}>
                  <span>{file.fileType === 'SIGNED_DOCUMENT' ? 'PDF' : 'AUDIT'}</span>
                  <div>
                    <strong>{file.fileType === 'SIGNED_DOCUMENT' ? '서명 완료 문서' : '감사추적증명서'}</strong>
                    <small>{(file.sizeBytes / 1024).toFixed(1)} KB · SHA-256 {file.sha256.slice(0, 12)}…</small>
                  </div>
                  <button type="button" onClick={() => openFile(file)} disabled={openingFileId === file.id}>
                    {openingFileId === file.id ? '여는 중…' : '실제 PDF 열기'}
                  </button>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </section>
  );
}

const CONNECT_STATUS_LABEL: Record<ConnectRequestItem['status'], string> = {
  OPEN: '수락 대기',
  REVIEWING: '진행 중',
  FULFILLED: '프로그램 개설 완료',
};

/** 이웃이 역제안한 선행(CONNECT) 요청을 센터 업무 화면 안에서 바로 수락·연결한다. */
function CenterConnectPanel({ organizationId }: { organizationId: number }) {
  const [requests, setRequests] = useState<ConnectRequestItem[]>([]);
  const [opportunities, setOpportunities] = useState<ManagedOpportunity[]>([]);
  const [linkingId, setLinkingId] = useState('');
  const [selectedOpportunity, setSelectedOpportunity] = useState('');
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setError('');
    fetchConnectRequests()
      .then((items) =>
        // 수락 대기 → 진행 중 → 완료 순으로, 같은 상태면 응원 많은 요청을 먼저 보여준다.
        setRequests([...items].sort((a, b) => {
          const rank = { OPEN: 0, REVIEWING: 1, FULFILLED: 2 } as const;
          return rank[a.status] - rank[b.status] || b.supportCount - a.supportCount;
        })),
      )
      .catch((reason) => {
        setRequests([]);
        setError(reason instanceof Error ? reason.message : '요청을 불러오지 못했습니다.');
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    fetchManagedOpportunities(organizationId)
      .then((items) => setOpportunities(items.filter((item) => item.status === 'PUBLISHED')))
      .catch(() => setOpportunities([]));
  }, [organizationId]);

  const act = async (publicId: string, opportunityId?: number) => {
    setBusyId(publicId);
    setError('');
    try {
      await handleConnectRequest(publicId, opportunityId ? { opportunityId } : {});
      setLinkingId('');
      setSelectedOpportunity('');
      load();
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : '요청 처리에 실패했습니다.');
    } finally {
      setBusyId('');
    }
  };

  const openCount = requests.filter((item) => item.status === 'OPEN').length;

  return (
    <section className="center-signed-documents center-connect-panel">
      <header>
        <div><span>CONNECT</span><h3>온기 잇다 · 이웃이 요청한 선행</h3></div>
        <strong>수락 대기 {openCount}건</strong>
      </header>
      {loading ? (
        <p className="center-document-state">이웃들의 요청을 불러오는 중입니다…</p>
      ) : error ? (
        <p className="center-document-state error">{error}</p>
      ) : requests.length === 0 ? (
        <p className="center-document-state">아직 접수된 선행 요청이 없습니다.</p>
      ) : (
        <div className="connect-list">
          {requests.map((item) => (
            <article
              key={item.publicId}
              className={`connect-row${item.status === 'FULFILLED' ? ' status-fulfilled' : ''}`}
            >
              <div className="connect-row-main">
                <div className="connect-row-labels">
                  <span className="connect-badge">{item.category === 'VOLUNTEER' ? '봉사' : '기부'}</span>
                  {item.origin === 'AI' && <span className="connect-badge is-ai">AI 대화에서</span>}
                  <span className="connect-status">{CONNECT_STATUS_LABEL[item.status]}</span>
                </div>
                <h4>{item.title}</h4>
                <p>{item.content}</p>
                <div className="connect-row-meta">
                  <strong>{item.requesterNickname}</strong>
                  <span>·</span>
                  <span>응원 {item.supportCount}</span>
                  {item.region && (
                    <>
                      <span>·</span>
                      <span>{item.region}</span>
                    </>
                  )}
                  {item.handledOrganizationName && (
                    <>
                      <span>·</span>
                      <span>{item.handledOrganizationName} 담당</span>
                    </>
                  )}
                </div>
                {linkingId === item.publicId && (
                  <div className="connect-link-panel">
                    <strong>어떤 프로그램으로 열었나요?</strong>
                    {opportunities.length === 0 ? (
                      <p>공개(PUBLISHED)된 프로그램이 없습니다. 모집글을 먼저 등록·공개해 주세요.</p>
                    ) : (
                      <div className="connect-link-row">
                        <select
                          value={selectedOpportunity}
                          onChange={(event) => setSelectedOpportunity(event.target.value)}
                        >
                          <option value="">프로그램 선택</option>
                          {opportunities.map((opportunity) => (
                            <option key={opportunity.id} value={opportunity.id}>
                              {opportunity.title}
                            </option>
                          ))}
                        </select>
                        <button
                          type="button"
                          disabled={!selectedOpportunity || busyId === item.publicId}
                          onClick={() => act(item.publicId, Number(selectedOpportunity))}
                        >
                          연결하고 완료로 표시
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </div>
              <div className="connect-row-reaction">
                {item.canHandle && item.status === 'OPEN' && (
                  <button
                    type="button"
                    className="connect-claim"
                    disabled={busyId === item.publicId}
                    onClick={() => act(item.publicId)}
                  >
                    {busyId === item.publicId ? '처리 중…' : '이 요청 맡기'}
                  </button>
                )}
                {item.canHandle && item.status === 'REVIEWING' && (
                  <button
                    type="button"
                    className="connect-claim"
                    onClick={() => {
                      setLinkingId(linkingId === item.publicId ? '' : item.publicId);
                      setSelectedOpportunity('');
                    }}
                  >
                    {linkingId === item.publicId ? '취소' : '프로그램 연결'}
                  </button>
                )}
                {!item.canHandle && item.status === 'REVIEWING' && (
                  <small className="connect-other-center">다른 센터가 맡음</small>
                )}
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

export const MyCenterPage: React.FC<MyCenterPageProps> = ({ currentUser }) => {
  const navigate = useNavigate();
  const route = useParams()['*'] ?? '';
  const [managedCenters, setManagedCenters] = useState<ManagedCenter[]>([]);
  const [centersLoading, setCentersLoading] = useState(true);
  const [selectedCenter, setSelectedCenter] = useState<ManagedCenter | null>(null);
  const [activeTab, setActiveTab] = useState<CenterTab>('posts');
  const [postManagerTab, setPostManagerTab] = useState<PostManagerTab>('edit');
  const [posts, setPosts] = useState<CenterPost[]>([]);
  const [applicants, setApplicants] = useState<CenterApplicant[]>([]);
  const [editingPost, setEditingPost] = useState<CenterPost | null>(null);
  const [viewingApplicant, setViewingApplicant] = useState<CenterApplicant | null>(null);
  const [applicationPostId, setApplicationPostId] = useState<number | null>(null);
  const [applicationPublicId, setApplicationPublicId] = useState<string | null>(null);
  const [notice, setNotice] = useState('');
  const [creatingPost, setCreatingPost] = useState(false);
  const [newPost, setNewPost] = useState({
    type: 'VOLUNTEER',
    title: '',
    description: '',
    region: '',
    location: '',
    recruitmentCapacity: '10',
    recruitmentEndDateTime: '',
    activityStartDateTime: '',
    activityEndDateTime: '',
  });

  useEffect(() => {
    const loadCenters = async () => {
      try {
        const organizations = await fetchManagedOrganizations();
        const centers = await Promise.all(
          organizations.map(async (organization) => {
            const dashboard = await fetchOrganizationDashboard(organization.id);
            return {
              id: organization.id,
              name: organization.name,
              type: organization.organizationType,
              region: organization.address || '지역 미등록',
              status: organization.verificationStatus,
              publishedPosts: dashboard.publicOpportunities,
              completedPosts: dashboard.closedOpportunities,
              pendingApplicants: dashboard.pendingApplications,
              monthlyParticipants: dashboard.monthlyParticipants,
              totalCommitments: dashboard.totalCommitments,
              signedCommitments: dashboard.signedCommitments,
              awaitingSignature: dashboard.awaitingSignature,
              signedPledgeAmount: dashboard.signedPledgeAmount,
              renewalDueSoon: dashboard.renewalDueSoon,
            } satisfies ManagedCenter;
          }),
        );
        setManagedCenters(centers);
      } catch (error) {
        setNotice(error instanceof Error ? error.message : '센터 정보를 불러오지 못했습니다.');
        setManagedCenters([]);
      } finally {
        setCentersLoading(false);
      }
    };
    loadCenters();
  }, []);

  useEffect(() => {
    if (!selectedCenter) return;
    const loadCenterData = async () => {
      try {
        const opportunities = await fetchManagedOpportunities(selectedCenter.id);
        const mappedPosts = opportunities.map(mapOpportunity);
        setPosts(mappedPosts);
        const applicationGroups = await Promise.all(
          opportunities.map((opportunity) => fetchOpportunityApplications(opportunity.id)),
        );
        setApplicants(applicationGroups.flat().map((application, index) =>
          mapApplication(application, index),
        ));
      } catch (error) {
        setNotice(error instanceof Error ? error.message : '센터 운영 데이터를 불러오지 못했습니다.');
      }
    };
    loadCenterData();
  }, [selectedCenter?.id]);

  const centerPosts = useMemo(
    () => posts.filter((post) => post.centerId === selectedCenter?.id),
    [posts, selectedCenter],
  );
  const centerApplicants = useMemo(
    () => applicants.filter((applicant) => applicant.centerId === selectedCenter?.id),
    [applicants, selectedCenter],
  );
  const applicationPostApplicants = useMemo(
    () => centerApplicants.filter((applicant) => applicant.postId === applicationPostId),
    [applicationPostId, centerApplicants],
  );
  const selectedApplication = useMemo(
    () =>
      applicationPostApplicants.find(
        (applicant) => applicant.publicId === applicationPublicId,
      ) ?? null,
    [applicationPostApplicants, applicationPublicId],
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
      if (centersLoading) return;
      navigate('/my-centers', { replace: true });
      return;
    }

    setSelectedCenter(center);

    if (section === 'posts' && entityId === 'new') {
      setCreatingPost(true);
      setEditingPost(null);
      setViewingApplicant(null);
      setActiveTab('posts');
      return;
    }
    setCreatingPost(false);

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
      const numericPostId = entityId ? Number(entityId) : null;
      const legacyApplicant =
        entityId && Number.isNaN(numericPostId)
          ? applicants.find(
              (item) => item.centerId === center.id && item.publicId === entityId,
            )
          : null;
      setApplicationPostId(
        legacyApplicant?.postId ?? (numericPostId && !Number.isNaN(numericPostId) ? numericPostId : null),
      );
      setApplicationPublicId(legacyApplicant?.publicId ?? subSection ?? null);
      setViewingApplicant(null);
      return;
    }

    if (section === 'connect') {
      setActiveTab('connect');
      setApplicationPostId(null);
      setApplicationPublicId(null);
      setViewingApplicant(null);
      return;
    }

    setActiveTab('posts');
    setApplicationPostId(null);
    setApplicationPublicId(null);
    setViewingApplicant(null);
  }, [applicants, centersLoading, managedCenters, navigate, posts, route]);

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

  const savePost = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!editingPost) return;
    try {
      const raw = editingPost.raw;
      await updateManagedOpportunity(editingPost.id, {
        type: editingPost.category === '봉사' ? 'VOLUNTEER' : raw.type,
        category: raw.category,
        title: editingPost.title,
        // 목록 요약문은 작성 화면과 동일하게 본문 앞부분에서 다시 만든다.
        summary: editingPost.description.slice(0, 120),
        description: editingPost.description,
        region: raw.region,
        location: raw.location,
        participationMode: raw.participationMode,
        recruitmentCapacity: raw.recruitmentCapacity,
        recruitmentStartDateTime: raw.recruitmentStartDateTime,
        recruitmentEndDateTime: raw.recruitmentEndDateTime,
        activityStartDateTime: raw.activityStartDateTime,
        activityEndDateTime: raw.activityEndDateTime,
        eligibility: raw.eligibility,
        targetAmount: raw.targetAmount,
        cancellationPolicy: raw.cancellationPolicy,
        requiredDocuments: raw.requiredDocuments,
      });
      if (editingPost.status === '마감' && raw.status === 'PUBLISHED') {
        await closeManagedOpportunity(editingPost.id);
      }
      // 일정을 고쳤으면 목록에 보이는 모집 기간 문구도 새 값으로 다시 만든다.
      const savedPost: CenterPost = {
        ...editingPost,
        period: formatPeriod(
          editingPost.raw.recruitmentStartDateTime,
          editingPost.raw.recruitmentEndDateTime,
        ),
      };
      setPosts((current) =>
        current.map((post) => (post.id === editingPost.id ? savedPost : post)),
      );
      const center = managedCenters.find((item) => item.id === editingPost.centerId);
      navigate(center ? `/my-centers/${center.id}` : '/my-centers');
      setNotice('모집글 수정 내용을 DB에 저장했습니다.');
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '모집글 수정에 실패했습니다.');
    }
  };

  const createPost = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedCenter) return;
    try {
      const created = await createManagedOpportunity(selectedCenter.id, {
        type: newPost.type,
        category: newPost.type === 'VOLUNTEER' ? '봉사' : '기부',
        title: newPost.title,
        summary: newPost.description.slice(0, 120),
        description: newPost.description,
        region: newPost.region,
        location: newPost.location,
        participationMode: 'OFFLINE',
        recruitmentCapacity:
          newPost.type === 'VOLUNTEER' ? Number(newPost.recruitmentCapacity) : null,
        recruitmentEndDateTime: newPost.recruitmentEndDateTime || null,
        activityStartDateTime: newPost.activityStartDateTime || null,
        activityEndDateTime: newPost.activityEndDateTime || null,
        requiredDocuments: [
          {
            code: 'CLM_COMMITMENT',
            name: '선행 약정서',
            description: '신청 과정에서 작성하는 CLM 약정서',
            required: true,
          },
        ],
      });
      const published = await publishManagedOpportunity(created.id);
      setPosts((current) => [mapOpportunity(published), ...current]);
      navigate(`/my-centers/${selectedCenter.id}`);
      setNotice('모집글을 작성하고 공개했습니다.');
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '모집글 작성에 실패했습니다.');
    }
  };

  const openPostManagement = (post: CenterPost) => {
    const center = managedCenters.find((item) => item.id === post.centerId);
    if (center) navigate(`/my-centers/${center.id}/posts/${post.id}`);
  };

  const approveApplicant = async (publicId: string) => {
    try {
      await approveApplication(publicId);
      setApplicants((current) =>
        current.map((applicant) =>
          applicant.publicId === publicId ? { ...applicant, status: '승인 완료' } : applicant,
        ),
      );
      setViewingApplicant((current) =>
        current?.publicId === publicId ? { ...current, status: '승인 완료' } : current,
      );
      setNotice('신청자를 승인하고 DB에 기록했습니다.');
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '신청 승인에 실패했습니다.');
    }
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

  if (creatingPost) {
    const isVolunteerPost = newPost.type === 'VOLUNTEER';
    const scheduleWarning = validateSchedule(
      newPost.recruitmentEndDateTime,
      newPost.activityStartDateTime,
      newPost.activityEndDateTime,
    );

    return (
      <article className="center-editor-page">
        <div className="my-center-detail-nav">
          <button type="button" onClick={() => navigate(`/my-centers/${selectedCenter.id}`)}>
            모집글 목록으로 돌아가기
          </button>
          <span>모집글 작성</span>
        </div>
        <header className="center-editor-header">
          <p>새 프로그램</p>
          <h2>모집글 작성</h2>
          <span>{selectedCenter.name}</span>
        </header>
        <form className="center-editor-form" onSubmit={createPost}>
          <div className="center-editor-form-row">
            <label>
              <span>구분 <em className="field-required">필수</em></span>
              <select
                value={newPost.type}
                onChange={(event) => setNewPost({ ...newPost, type: event.target.value })}
              >
                <option value="VOLUNTEER">봉사</option>
                <option value="DONATION">기부</option>
                <option value="LEGACY_DONATION">유산기부</option>
                <option value="HERITAGE_SPONSORSHIP">문화유산 후원</option>
              </select>
              <small>구분에 따라 신청자에게 보이는 약정서 항목이 달라집니다.</small>
            </label>
            {isVolunteerPost && (
              <label>
                <span>모집 인원 <em className="field-required">필수</em></span>
                <input
                  type="number"
                  min="1"
                  value={newPost.recruitmentCapacity}
                  onChange={(event) =>
                    setNewPost({ ...newPost, recruitmentCapacity: event.target.value })
                  }
                  required
                />
                <small>모집할 봉사자 수를 명 단위로 입력하세요.</small>
              </label>
            )}
          </div>

          <label>
            <span>모집글 제목 <em className="field-required">필수</em></span>
            <input
              value={newPost.title}
              onChange={(event) => setNewPost({ ...newPost, title: event.target.value })}
              placeholder="예) 금정구 독거어르신 온기 도시락 배달"
              maxLength={100}
              required
            />
            <small>{newPost.title.length}/100자 · 목록에 그대로 노출됩니다.</small>
          </label>

          <label>
            <span>상세 설명 <em className="field-required">필수</em></span>
            <textarea
              rows={8}
              value={newPost.description}
              onChange={(event) => setNewPost({ ...newPost, description: event.target.value })}
              placeholder={'활동 내용, 준비물, 유의사항을 적어주세요.\n앞부분 120자가 목록 요약문으로 사용됩니다.'}
              required
            />
            <small>앞 120자가 목록 요약문이 되니 핵심을 먼저 적어주세요.</small>
          </label>

          <div className="center-editor-form-row">
            <label>
              <span>지역</span>
              <input
                value={newPost.region}
                onChange={(event) => setNewPost({ ...newPost, region: event.target.value })}
                placeholder="예) 부산광역시 금정구"
              />
            </label>
            <label>
              <span>활동 장소</span>
              <input
                value={newPost.location}
                onChange={(event) => setNewPost({ ...newPost, location: event.target.value })}
                placeholder="예) 금정구 종합사회복지관 2층"
              />
            </label>
          </div>

          <fieldset className="center-editor-fieldset">
            <legend>일정</legend>
            <div className="center-editor-form-row center-editor-form-row-3">
              <label>
                <span>모집 마감</span>
                <input
                  type="datetime-local"
                  value={newPost.recruitmentEndDateTime}
                  onChange={(event) =>
                    setNewPost({ ...newPost, recruitmentEndDateTime: event.target.value })
                  }
                />
              </label>
              <label>
                <span>활동 시작</span>
                <input
                  type="datetime-local"
                  value={newPost.activityStartDateTime}
                  min={newPost.recruitmentEndDateTime || undefined}
                  onChange={(event) =>
                    setNewPost({ ...newPost, activityStartDateTime: event.target.value })
                  }
                />
              </label>
              <label>
                <span>활동 종료</span>
                <input
                  type="datetime-local"
                  value={newPost.activityEndDateTime}
                  min={newPost.activityStartDateTime || undefined}
                  onChange={(event) =>
                    setNewPost({ ...newPost, activityEndDateTime: event.target.value })
                  }
                />
              </label>
            </div>
            <small className="center-editor-fieldset-hint">
              비워두면 상시 모집으로 표시됩니다.
            </small>
          </fieldset>

          {scheduleWarning && <p className="center-editor-warning">{scheduleWarning}</p>}

          <div className="center-editor-actions">
            <span className="center-editor-actions-hint">
              작성을 완료하면 모집글이 <strong>바로 공개</strong>됩니다.
            </span>
            <button type="button" onClick={() => navigate(`/my-centers/${selectedCenter.id}`)}>
              취소
            </button>
            <button type="submit" disabled={Boolean(scheduleWarning)}>
              작성 완료 및 공개
            </button>
          </div>
        </form>
        {notice && <div className="center-notice">{notice}</div>}
      </article>
    );
  }

  if (editingPost && !viewingApplicant) {
    // 일정·지역 같은 값은 CenterPost가 아니라 원본 응답(raw)에 있고, 저장 시에도 raw가 그대로 전송된다.
    const updateEditingRaw = (patch: Partial<ManagedOpportunity>) =>
      setEditingPost((current) =>
        current ? { ...current, raw: { ...current.raw, ...patch } } : current,
      );
    const editScheduleWarning = validateSchedule(
      editingPost.raw.recruitmentEndDateTime,
      editingPost.raw.activityStartDateTime,
      editingPost.raw.activityEndDateTime,
    );

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
              <span>모집글 제목 <em className="field-required">필수</em></span>
              <input
                value={editingPost.title}
                onChange={(event) => setEditingPost({ ...editingPost, title: event.target.value })}
                maxLength={100}
                required
              />
              <small>{editingPost.title.length}/100자 · 목록에 그대로 노출됩니다.</small>
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
                <small>
                  {editingPost.status === '마감'
                    ? '저장하면 신청을 더 이상 받지 않습니다.'
                    : '신청자에게 공개되어 신청을 받는 상태입니다.'}
                </small>
              </label>
            </div>

            <div className="center-editor-form-row">
              <label>
                <span>지역</span>
                <input
                  value={editingPost.raw.region || ''}
                  onChange={(event) => updateEditingRaw({ region: event.target.value })}
                  placeholder="예) 부산광역시 금정구"
                />
              </label>
              <label>
                <span>활동 장소</span>
                <input
                  value={editingPost.raw.location || ''}
                  onChange={(event) => updateEditingRaw({ location: event.target.value })}
                  placeholder="예) 금정구 종합사회복지관 2층"
                />
              </label>
            </div>

            <fieldset className="center-editor-fieldset">
              <legend>일정</legend>
              <div className="center-editor-form-row center-editor-form-row-3">
                <label>
                  <span>모집 마감</span>
                  <input
                    type="datetime-local"
                    value={toDateTimeLocal(editingPost.raw.recruitmentEndDateTime)}
                    onChange={(event) =>
                      updateEditingRaw({ recruitmentEndDateTime: event.target.value })
                    }
                  />
                </label>
                <label>
                  <span>활동 시작</span>
                  <input
                    type="datetime-local"
                    value={toDateTimeLocal(editingPost.raw.activityStartDateTime)}
                    onChange={(event) =>
                      updateEditingRaw({ activityStartDateTime: event.target.value })
                    }
                  />
                </label>
                <label>
                  <span>활동 종료</span>
                  <input
                    type="datetime-local"
                    value={toDateTimeLocal(editingPost.raw.activityEndDateTime)}
                    onChange={(event) =>
                      updateEditingRaw({ activityEndDateTime: event.target.value })
                    }
                  />
                </label>
              </div>
              <small className="center-editor-fieldset-hint">
                비워두면 상시 모집으로 표시됩니다. 현재 모집 기간: {editingPost.period}
              </small>
            </fieldset>

            {editingPost.category === '봉사' && (
              <div className="center-editor-form-row">
                <label>
                  <span>모집 인원</span>
                  <input
                    type="number"
                    min="1"
                    value={editingPost.raw.recruitmentCapacity ?? ''}
                    onChange={(event) =>
                      updateEditingRaw({
                        recruitmentCapacity: event.target.value
                          ? Number(event.target.value)
                          : undefined,
                      })
                    }
                  />
                  <small>현재 {editingPost.applicantCount}명이 신청했습니다.</small>
                </label>
              </div>
            )}

            <label>
              <span>상세 설명 <em className="field-required">필수</em></span>
              <textarea
                rows={8}
                value={editingPost.description}
                onChange={(event) =>
                  setEditingPost({ ...editingPost, description: event.target.value })
                }
                required
              />
              <small>앞 120자가 목록 요약문이 되니 핵심을 먼저 적어주세요.</small>
            </label>

            {editScheduleWarning && (
              <p className="center-editor-warning">{editScheduleWarning}</p>
            )}

            <div className="center-editor-actions">
              <button type="button" onClick={() => navigate(`/my-centers/${selectedCenter.id}`)}>
                취소
              </button>
              <button type="submit" disabled={Boolean(editScheduleWarning)}>
                수정 내용 저장
              </button>
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
                        approveApplicant(applicant.publicId);
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
              <span className="document-metadata-label">신청 항목</span>
            </div>
          ))}
        </section>

        <CenterSignedDocumentPanel applicationPublicId={viewingApplicant.publicId} />

        <CenterActivityNotePanel applicationPublicId={viewingApplicant.publicId} />

        {viewingApplicant.status === '검토 대기' && (
          <button
            type="button"
            className="applicant-detail-approve"
            onClick={() => approveApplicant(viewingApplicant.publicId)}
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
        <button
          type="button"
          onClick={() => navigate(`/my-centers/${selectedCenter.id}/posts/new`)}
        >
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

      <section className="center-commitment-board" aria-label="약정 현황 및 증빙">
        <div className="center-commitment-heading">
          <span>COMMITMENT STATUS</span>
          <h3>약정 현황 · 전자서명 증빙</h3>
        </div>
        <div className="center-commitment-metrics">
          <div>
            <span>전체 약정</span>
            <strong>{selectedCenter.totalCommitments}<small>건</small></strong>
            <p>취소를 제외한 누적 약정</p>
          </div>
          <div className="accent">
            <span>서명 완료</span>
            <strong>{selectedCenter.signedCommitments}<small>건</small></strong>
            <p>모두싸인 증빙이 보관된 약정</p>
          </div>
          <div>
            <span>서명 대기</span>
            <strong>{selectedCenter.awaitingSignature}<small>건</small></strong>
            <p>요청했으나 아직 미완료</p>
          </div>
          <div>
            <span>증빙된 약정액</span>
            <strong>{selectedCenter.signedPledgeAmount.toLocaleString('ko-KR')}<small>원</small></strong>
            <p>서명으로 확정된 후원 금액</p>
          </div>
          <div className={selectedCenter.renewalDueSoon > 0 ? 'warn' : ''}>
            <span>갱신 임박</span>
            <strong>{selectedCenter.renewalDueSoon}<small>건</small></strong>
            <p>30일 내 갱신이 필요한 정기 약정</p>
          </div>
        </div>
        <p className="center-commitment-rate">
          서명 완료율{' '}
          <strong>
            {selectedCenter.totalCommitments === 0
              ? 0
              : Math.round(
                  (selectedCenter.signedCommitments / selectedCenter.totalCommitments) * 100,
                )}
            %
          </strong>
        </p>
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
          신청 관리
        </button>
        <button
          type="button"
          className={activeTab === 'connect' ? 'active' : ''}
          onClick={() => navigate(`/my-centers/${selectedCenter.id}/connect`)}
        >
          온기 잇다 요청
        </button>
        <span>
          {activeTab === 'posts'
            ? `총 ${centerPosts.length}개 모집글`
            : activeTab === 'connect'
              ? '이웃이 역제안한 선행 요청'
              : `총 ${centerApplicants.length}건 신청`}
        </span>
      </nav>

      {activeTab === 'connect' ? (
        <CenterConnectPanel organizationId={selectedCenter.id} />
      ) : activeTab === 'posts' ? (
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
        <section className="operator-document-section center-application-management">
          <div className="operator-document-flow center-application-flow">
            <CenterApplicationLevel
              step="01"
              title="모집글"
              empty="등록된 모집글이 없습니다."
              items={centerPosts.map((post) => ({
                id: String(post.id),
                label: post.title,
                meta: `${post.status} · 신청 ${centerApplicants.filter((item) => item.postId === post.id).length}건`,
              }))}
              selectedId={applicationPostId ? String(applicationPostId) : null}
              onSelect={(id) =>
                navigate(`/my-centers/${selectedCenter.id}/applications/${id}`)
              }
            />
            <CenterApplicationLevel
              step="02"
              title="신청자"
              empty={
                applicationPostId
                  ? '이 모집글에 접수된 신청자가 없습니다.'
                  : '모집글을 먼저 선택해주세요.'
              }
              items={applicationPostApplicants.map((applicant) => ({
                id: applicant.publicId,
                label: applicant.name,
                meta: `${applicant.status} · ${applicant.appliedAt}`,
              }))}
              selectedId={applicationPublicId}
              onSelect={(publicId) =>
                navigate(
                  `/my-centers/${selectedCenter.id}/applications/${applicationPostId}/${publicId}`,
                )
              }
            />
          </div>

          <div className="operator-document-preview">
            <div className="operator-document-preview-heading">
              <div>
                <span>03</span>
                <h3>신청·제출 서류 확인</h3>
              </div>
              {selectedApplication && (
                <strong>{selectedApplication.status}</strong>
              )}
            </div>
            {!selectedApplication ? (
              <div className="operator-document-empty">
                모집글과 신청자를 순서대로 선택하면 신청 정보와 제출 서류가 표시됩니다.
              </div>
            ) : (
              <>
                <div className="operator-applicant-summary">
                  <div><span>신청자</span><strong>{selectedApplication.name}</strong></div>
                  <div><span>이메일</span><strong>{selectedApplication.email}</strong></div>
                  <div><span>신청 모집글</span><strong>{selectedApplication.postTitle}</strong></div>
                </div>
                <section className="operator-commitment">
                  <span>신청 동기·전달사항</span>
                  <h4>{selectedApplication.name}님의 신청</h4>
                  <p>{selectedApplication.motivation}</p>
                </section>
                <div className="operator-document-list">
                  {selectedApplication.documents.length === 0 ? (
                    <div className="center-application-document-empty">
                      제출된 서류가 없습니다.
                    </div>
                  ) : (
                    selectedApplication.documents.map((document, index) => (
                      <div key={`${document}-${index}`}>
                        <span>{String(index + 1).padStart(2, '0')}</span>
                        <strong>{document}</strong>
                        <span className="document-metadata-label">신청 항목</span>
                      </div>
                    ))
                  )}
                </div>
                <CenterSignedDocumentPanel applicationPublicId={selectedApplication.publicId} />
                <div className="center-application-actions">
                  <span>
                    신청일 {selectedApplication.appliedAt} · 연락처 {selectedApplication.phone}
                  </span>
                  {selectedApplication.status === '검토 대기' ? (
                    <button
                      type="button"
                      onClick={() => approveApplicant(selectedApplication.publicId)}
                    >
                      신청 승인
                    </button>
                  ) : (
                    <strong>승인 완료</strong>
                  )}
                </div>
              </>
            )}
          </div>
        </section>
      )}
      {notice && <div className="center-notice">{notice}</div>}


    </article>
  );
};
