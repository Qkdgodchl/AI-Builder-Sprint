import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import type { ApplicationResponse } from '../../services/applicationApi';
import {
  decideManagerApplication,
  decideOrganizationApplication,
  fetchManagerApplications,
  fetchOperatorDocuments,
  fetchOrganizationApplications,
  type ManagerApplication,
  type OrganizationApplication,
} from '../../services/operatorApi';

type Section = 'approvals' | 'documents';

const sectionLabels: Record<Section, string> = {
  approvals: '승인 관리',
  documents: '전체 서류',
};
const visibleSections: Section[] = ['approvals', 'documents'];

export function ManagementPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const route = location.pathname.split('/').filter(Boolean);
  const section: Section = route[1] === 'documents' ? 'documents' : 'approvals';

  const [managers, setManagers] = useState<ManagerApplication[]>([]);
  const [organizations, setOrganizations] = useState<OrganizationApplication[]>([]);
  const [documents, setDocuments] = useState<ApplicationResponse[]>([]);
  const [selectedOrganizationId, setSelectedOrganizationId] = useState<number | null>(null);
  const [selectedOpportunityId, setSelectedOpportunityId] = useState<number | null>(null);
  const [selectedApplicationId, setSelectedApplicationId] = useState<string | null>(null);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');

  const loadAll = async () => {
    setError('');
    try {
      const [managerData, organizationData, documentData] =
        await Promise.all([
          fetchManagerApplications(),
          fetchOrganizationApplications(),
          fetchOperatorDocuments(),
        ]);
      setManagers(managerData);
      setOrganizations(organizationData);
      setDocuments(documentData);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : '운영 정보를 불러오지 못했습니다.');
    }
  };

  useEffect(() => {
    void loadAll();
  }, []);

  useEffect(() => {
    if (location.pathname.startsWith('/management/content')) {
      navigate('/management', { replace: true });
    }
  }, [location.pathname, navigate]);

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'auto' });
  }, [location.pathname]);

  useEffect(() => {
    if (!notice) return;
    const timer = window.setTimeout(() => setNotice(''), 2600);
    return () => window.clearTimeout(timer);
  }, [notice]);

  const runAction = async (action: () => Promise<unknown>, success: string) => {
    setError('');
    try {
      await action();
      setNotice(success);
      await loadAll();
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : '처리에 실패했습니다.');
    }
  };

  const centers = useMemo(
    () =>
      Array.from(
        new Map(
          documents.map((document) => [
            document.organizationId,
            { id: document.organizationId, name: document.organizationName },
          ]),
        ).values(),
      ),
    [documents],
  );
  const centerDocuments = documents.filter(
    (document) => document.organizationId === selectedOrganizationId,
  );
  const centerOpportunities = Array.from(
    new Map(
      centerDocuments.map((document) => [
        document.opportunityId,
        { id: document.opportunityId, title: document.opportunityTitle },
      ]),
    ).values(),
  );
  const opportunityDocuments = centerDocuments.filter(
    (document) => document.opportunityId === selectedOpportunityId,
  );
  const selectedDocument = documents.find(
    (document) => document.publicId === selectedApplicationId,
  );

  return (
    <article className="operator-management-page">
      <header className="operator-management-header">
        <div>
          <p>플랫폼 운영진</p>
          <h2>운영 관리</h2>
          <span>관리자·센터 승인과 전체 제출 서류를 관리합니다.</span>
        </div>
        <strong>운영진 전용</strong>
      </header>

      <div className="operator-management-metrics">
        <div><span>대기 중 관리자 신청</span><strong>{managers.length}</strong></div>
        <div><span>대기 중 센터 신청</span><strong>{organizations.length}</strong></div>
        <div><span>서류 제출 센터</span><strong>{centers.length}</strong></div>
        <div><span>전체 제출 서류</span><strong>{documents.length}</strong></div>
      </div>

      <nav className="center-management-tabs operator-primary-tabs" aria-label="운영 관리 메뉴">
        {visibleSections.map((item) => (
          <button
            key={item}
            type="button"
            className={section === item ? 'active' : ''}
            onClick={() =>
              navigate(item === 'approvals' ? '/management' : `/management/${item}`)
            }
          >
            {sectionLabels[item]}
          </button>
        ))}
        <span>{sectionSummary(section, managers.length, organizations.length, documents.length)}</span>
      </nav>

      {error && <div className="center-notice operator-error">{error}</div>}
      {notice && <div className="center-notice">{notice}</div>}

      {section === 'approvals' && (
        <ApprovalSection
          managers={managers}
          organizations={organizations}
          onManagerDecision={(id, decision) =>
            void runAction(
              () => decideManagerApplication(
                id,
                decision,
                decision === 'approve' ? '운영진 승인' : '운영진 검토 결과 반려',
              ),
              decision === 'approve' ? '관리자 권한을 승인했습니다.' : '관리자 신청을 반려했습니다.',
            )
          }
          onOrganizationDecision={(id, decision) =>
            void runAction(
              () => decideOrganizationApplication(
                id,
                decision,
                decision === 'approve' ? '운영진 승인' : '운영진 검토 결과 반려',
              ),
              decision === 'approve' ? '센터 등록을 승인했습니다.' : '센터 신청을 반려했습니다.',
            )
          }
        />
      )}

      {section === 'documents' && (
        <section className="operator-document-section">
          <div className="operator-document-flow" aria-label="전체 서류 탐색">
            <DocumentLevel
              step="01"
              title="센터"
              empty="제출 서류가 있는 센터가 없습니다."
              items={centers.map((center) => ({ id: String(center.id), label: center.name }))}
              selectedId={selectedOrganizationId ? String(selectedOrganizationId) : null}
              onSelect={(id) => {
                setSelectedOrganizationId(Number(id));
                setSelectedOpportunityId(null);
                setSelectedApplicationId(null);
              }}
            />
            <DocumentLevel
              step="02"
              title="모집글"
              empty="센터를 먼저 선택해주세요."
              items={centerOpportunities.map((opportunity) => ({
                id: String(opportunity.id),
                label: opportunity.title,
              }))}
              selectedId={selectedOpportunityId ? String(selectedOpportunityId) : null}
              onSelect={(id) => {
                setSelectedOpportunityId(Number(id));
                setSelectedApplicationId(null);
              }}
            />
            <DocumentLevel
              step="03"
              title="신청자"
              empty="모집글을 먼저 선택해주세요."
              items={opportunityDocuments.map((application) => ({
                id: application.publicId,
                label: application.applicantName || application.applicantEmail,
                meta: statusLabel(application.status),
              }))}
              selectedId={selectedApplicationId}
              onSelect={setSelectedApplicationId}
            />
          </div>
          <div className="operator-document-preview">
            <div className="operator-document-preview-heading">
              <div>
                <span>04</span>
                <h3>제출 서류 확인</h3>
              </div>
              {selectedDocument && <strong>{statusLabel(selectedDocument.status)}</strong>}
            </div>
            {!selectedDocument ? (
              <div className="operator-document-empty">
                센터, 모집글, 신청자를 순서대로 선택하면 제출 서류가 표시됩니다.
              </div>
            ) : (
              <>
                <div className="operator-applicant-summary">
                  <div><span>신청자</span><strong>{selectedDocument.applicantName}</strong></div>
                  <div><span>이메일</span><strong>{selectedDocument.applicantEmail}</strong></div>
                  <div><span>모집글</span><strong>{selectedDocument.opportunityTitle}</strong></div>
                </div>
                <section className="operator-commitment">
                  <span>전자 약정서</span>
                  <h4>{selectedDocument.commitment?.title || '약정서 미작성'}</h4>
                  <p>{selectedDocument.commitment?.renderedContent || '작성된 약정서 내용이 없습니다.'}</p>
                </section>
                <div className="operator-document-list">
                    {selectedDocument.documents.map((document) => (
                      <div key={document.code} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <div>
                          <span>{document.required ? '필수' : '선택'}</span>
                          <strong>{document.name}</strong>
                          <em>{statusLabel(document.status)}</em>
                        </div>
                        <a
                          href="http://localhost:8080/api/v1/clm/documents/23/files/11/download"
                          target="_blank"
                          rel="noreferrer"
                          style={{
                            display: 'inline-flex', alignItems: 'center', gap: '4px',
                            padding: '6px 12px', background: '#ff3b30', color: '#fff',
                            border: '1px solid #111', borderRadius: '6px', textDecoration: 'none',
                            fontWeight: 'bold', fontSize: '11px'
                          }}
                        >
                          내려받기 💾
                        </a>
                      </div>
                    ))}
                </div>
              </>
            )}
          </div>
        </section>
      )}
    </article>
  );
}

function ApprovalSection({
  managers,
  organizations,
  onManagerDecision,
  onOrganizationDecision,
}: {
  managers: ManagerApplication[];
  organizations: OrganizationApplication[];
  onManagerDecision: (id: string, decision: 'approve' | 'reject') => void;
  onOrganizationDecision: (id: string, decision: 'approve' | 'reject') => void;
}) {
  const rows = [
    ...managers.map((item) => ({
      id: item.publicId,
      kind: '관리자 권한',
      applicant: item.applicantEmail,
      target: item.plannedCenterName || item.organizationName,
      detail: `${item.position} · ${item.contact}`,
      decide: (decision: 'approve' | 'reject') => onManagerDecision(item.publicId, decision),
    })),
    ...organizations.map((item) => ({
      id: item.publicId,
      kind: '센터 등록',
      applicant: item.applicantEmail,
      target: item.name,
      detail: `${item.representativeName} · ${item.organizationType}`,
      decide: (decision: 'approve' | 'reject') => onOrganizationDecision(item.publicId, decision),
    })),
  ];
  return (
    <div className="operator-approval-table" role="table" aria-label="운영 승인 대기 목록">
      <div className="operator-approval-head" role="row">
        <span role="columnheader">신청 구분</span>
        <span role="columnheader">신청자</span>
        <span role="columnheader">신청 대상</span>
        <span role="columnheader">신청 정보</span>
        <span role="columnheader">상태</span>
        <span role="columnheader" aria-label="승인 처리" />
      </div>
      {rows.length === 0 ? (
        <div className="operator-empty-row">현재 검토 대기 중인 신청이 없습니다.</div>
      ) : rows.map((row) => (
        <section className="operator-approval-row" role="row" key={`${row.kind}-${row.id}`}>
          <strong className="operator-kind" role="cell">{row.kind}</strong>
          <span role="cell">{row.applicant}</span>
          <div role="cell"><strong>{row.target}</strong></div>
          <span role="cell">{row.detail}</span>
          <strong className="operator-pending" role="cell">검토 대기</strong>
          <div className="operator-row-actions" role="cell">
            <button type="button" onClick={() => row.decide('approve')}>승인</button>
            <button type="button" onClick={() => row.decide('reject')}>반려</button>
          </div>
        </section>
      ))}
    </div>
  );
}

function DocumentLevel({
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

const statusLabel = (status: string) => {
  const labels: Record<string, string> = {
    PENDING: '검토 대기',
    APPLIED: '신청 완료',
    IN_REVIEW: '검토 중',
    APPROVED: '승인 완료',
    REJECTED: '반려',
    PUBLISHED: '공개 중',
    DRAFT: '작성 중',
    RECRUITMENT_CLOSED: '모집 마감',
    CANCELLED: '취소',
    REQUIRED: '제출 필요',
    SUBMITTED: '제출 완료',
  };
  return labels[status] || status;
};

const sectionSummary = (
  section: Section,
  managerCount: number,
  organizationCount: number,
  documentCount: number,
) => {
  if (section === 'approvals') return `총 ${managerCount + organizationCount}건 검토 대기`;
  return `총 ${documentCount}건 제출`;
};
