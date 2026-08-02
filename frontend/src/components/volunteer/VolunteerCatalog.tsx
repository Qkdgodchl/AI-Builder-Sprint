import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import type { SessionUser, VolunteerItem } from '../../types';
import { deleteOpportunity, fetchVolunteers } from '../../services/volunteerApi';
import { OpportunityDetail } from './OpportunityDetail';
import { ClmApplicationPreparation } from './ClmApplicationPreparation';

type PrimaryFilter = 'VOLUNTEER' | 'DONATION' | 'HOMETOWN' | null;
type DonationFilter = 'ALL' | 'GENERAL' | 'LEGACY' | 'UNESCO' | 'HERITAGE';
type ProgramType =
  | 'VOLUNTEER'
  | 'GENERAL'
  | 'LEGACY'
  | 'UNESCO'
  | 'HERITAGE'
  | 'HOMETOWN';

interface CatalogItem extends VolunteerItem {
  programType: ProgramType;
  availability: string;
  broadRegion: string;
}

const regionOrder = [
  '서울',
  '부산',
  '대구',
  '인천',
  '광주',
  '대전',
  '울산',
  '세종',
  '경기',
  '강원',
  '충북',
  '충남',
  '전북',
  '전남',
  '경북',
  '경남',
  '제주',
  '전국',
  '해외',
  '기타',
];

const broadRegionPatterns: Array<{ label: string; patterns: string[] }> = [
  { label: '서울', patterns: ['서울특별시', '서울'] },
  { label: '부산', patterns: ['부산광역시', '부산'] },
  { label: '대구', patterns: ['대구광역시', '대구'] },
  { label: '인천', patterns: ['인천광역시', '인천'] },
  { label: '광주', patterns: ['광주광역시', '광주'] },
  { label: '대전', patterns: ['대전광역시', '대전'] },
  { label: '울산', patterns: ['울산광역시', '울산'] },
  { label: '세종', patterns: ['세종특별자치시', '세종'] },
  { label: '경기', patterns: ['경기도', '경기'] },
  { label: '강원', patterns: ['강원특별자치도', '강원도', '강원'] },
  { label: '충북', patterns: ['충청북도', '충북'] },
  { label: '충남', patterns: ['충청남도', '충남'] },
  { label: '전북', patterns: ['전북특별자치도', '전라북도', '전북'] },
  { label: '전남', patterns: ['전라남도', '전남'] },
  { label: '경북', patterns: ['경상북도', '경북'] },
  { label: '경남', patterns: ['경상남도', '경남'] },
  { label: '제주', patterns: ['제주특별자치도', '제주도', '제주'] },
];

const getBroadRegion = (location: string) => {
  const normalized = location.trim();
  if (/전국|온라인|비대면/.test(normalized)) return '전국';
  if (/전 세계|해외|국외/.test(normalized)) return '해외';

  return broadRegionPatterns.find(({ patterns }) =>
    patterns.some((pattern) => normalized.startsWith(pattern)),
  )?.label || '기타';
};

const donationFilters: Array<{ value: DonationFilter; label: string }> = [
  { value: 'ALL', label: '전체 기부' },
  { value: 'GENERAL', label: '일반기부' },
  { value: 'LEGACY', label: '유산기부' },
  { value: 'UNESCO', label: '유네스코 후원' },
  { value: 'HERITAGE', label: '문화유산 후원' },
];

const getProgramLabel = (programType: ProgramType) => {
  const labels: Record<ProgramType, string> = {
    VOLUNTEER: '봉사',
    GENERAL: '일반기부',
    LEGACY: '유산기부',
    UNESCO: '유네스코',
    HERITAGE: '문화유산',
    HOMETOWN: '고향사랑기부',
  };

  return labels[programType];
};

const removeLeadingSymbol = (title: string) => title.replace(/^[^가-힣A-Za-z0-9]+/, '');

interface VolunteerCatalogProps {
  currentUser: SessionUser | null;
  showToast: (message: string) => void;
  onRequireLogin: (message: string) => void;
}

export const VolunteerCatalog: React.FC<VolunteerCatalogProps> = ({
  currentUser,
  showToast,
  onRequireLogin,
}) => {
  const [items, setItems] = useState<VolunteerItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [primaryFilter, setPrimaryFilter] = useState<PrimaryFilter>(null);
  const [donationFilter, setDonationFilter] = useState<DonationFilter>('ALL');
  const [regionFilter, setRegionFilter] = useState('ALL');
  const navigate = useNavigate();
  const route = useParams()['*'] ?? '';
  const [searchParams] = useSearchParams();

  // 홈의 구분 타일에서 넘어오면 상위 탭과 기부 하위 탭까지 맞춰 연다.
  const requestedType = searchParams.get('type');
  const requestedSub = searchParams.get('sub');
  useEffect(() => {
    if (requestedType === 'VOLUNTEER' || requestedType === 'DONATION' || requestedType === 'HOMETOWN') {
      setPrimaryFilter(requestedType);
    }
    if (
      requestedType === 'DONATION' &&
      (requestedSub === 'GENERAL' ||
        requestedSub === 'LEGACY' ||
        requestedSub === 'UNESCO' ||
        requestedSub === 'HERITAGE')
    ) {
      setDonationFilter(requestedSub);
    }
  }, [requestedType, requestedSub]);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      try {
        setItems(await fetchVolunteers());
      } catch (error) {
        console.error('Failed to load volunteer data', error);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, []);

  const catalogItems = useMemo<CatalogItem[]>(() => {
    const apiPrograms = items.map<CatalogItem>((item) => {
      const rawCategory = item.category || 'VOLUNTEER';
      const validTypes: ProgramType[] = ['VOLUNTEER', 'GENERAL', 'LEGACY', 'UNESCO', 'HERITAGE', 'HOMETOWN'];
      const programType: ProgramType = validTypes.includes(rawCategory as ProgramType)
        ? (rawCategory as ProgramType)
        : rawCategory === 'DONATION'
          ? 'GENERAL'
          : 'VOLUNTEER';

      return {
        ...item,
        title: removeLeadingSymbol(item.title),
        programType,
        broadRegion: getBroadRegion(item.location),
        availability:
          programType === 'LEGACY'
            ? '상담 가능'
            : programType === 'HOMETOWN'
              ? '신청 가능'
              : programType === 'VOLUNTEER'
                ? '모집 중'
                : '상시 모집',
      };
    });

    return apiPrograms;
  }, [items]);

  const categoryFilteredItems = useMemo(() => {
    if (!primaryFilter) return catalogItems;

    if (primaryFilter === 'VOLUNTEER') {
      return catalogItems.filter((item) => item.programType === 'VOLUNTEER');
    }

    if (primaryFilter === 'HOMETOWN') {
      return catalogItems.filter((item) => item.programType === 'HOMETOWN');
    }

    if (donationFilter === 'ALL') {
      return catalogItems.filter(
        (item) => item.programType !== 'VOLUNTEER' && item.programType !== 'HOMETOWN',
      );
    }

    return catalogItems.filter((item) => item.programType === donationFilter);
  }, [catalogItems, donationFilter, primaryFilter]);

  const availableRegions = useMemo(() => {
    const regions = new Set(categoryFilteredItems.map((item) => item.broadRegion));
    return regionOrder.filter((region) => regions.has(region));
  }, [categoryFilteredItems]);

  const regionCounts = useMemo(() => {
    return categoryFilteredItems.reduce<Record<string, number>>((counts, item) => {
      counts[item.broadRegion] = (counts[item.broadRegion] || 0) + 1;
      return counts;
    }, {});
  }, [categoryFilteredItems]);

  const filteredItems = useMemo(() => {
    if (regionFilter === 'ALL') return categoryFilteredItems;
    return categoryFilteredItems.filter((item) => item.broadRegion === regionFilter);
  }, [categoryFilteredItems, regionFilter]);

  const [routeId, routeAction] = route.split('/');
  const selectedItem = routeId
    ? catalogItems.find((item) => String(item.id) === routeId) ?? null
    : null;
  const isApplicationRoute = routeAction === 'apply';

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'auto' });
  }, [route]);

  const handlePrimaryFilter = (filter: Exclude<PrimaryFilter, null>) => {
    setPrimaryFilter((current) => (current === filter ? null : filter));
    if (filter !== 'DONATION') setDonationFilter('ALL');
    setRegionFilter('ALL');
  };

  const canDeleteItem = (item: CatalogItem) =>
    Boolean(
      currentUser &&
        (currentUser.role === 'OPERATOR' || currentUser.id === item.createdByUserId),
    );

  const handleDeleteItem = async (item: CatalogItem) => {
    if (!window.confirm(`"${item.title}" 프로그램을 삭제하시겠습니까?`)) return;
    try {
      await deleteOpportunity(item.id);
      setItems((current) => current.filter((candidate) => candidate.id !== item.id));
      navigate('/volunteer');
      showToast('프로그램을 삭제했습니다.');
    } catch (error) {
      showToast(error instanceof Error ? error.message : '프로그램 삭제에 실패했습니다.');
    }
  };

  if (selectedItem && isApplicationRoute) {
    return (
      <ClmApplicationPreparation
        item={selectedItem}
        typeLabel={getProgramLabel(selectedItem.programType)}
        currentUser={currentUser}
        onBack={() => navigate(`/volunteer/${selectedItem.id}`)}
      />
    );
  }

  if (selectedItem) {
    const canDelete = canDeleteItem(selectedItem);
    return (
      <OpportunityDetail
        item={selectedItem}
        onBack={() => navigate('/volunteer')}
        onApply={() => {
          // 신청은 계정에 남는 약정 절차라 로그인 없이는 진행할 수 없다.
          if (!currentUser) {
            onRequireLogin('로그인이 필요합니다. 신청은 로그인 후 이용할 수 있어요.');
            return;
          }
          navigate(`/volunteer/${selectedItem.id}/apply`);
        }}
        canDelete={canDelete}
        onDelete={() => void handleDeleteItem(selectedItem)}
        related={catalogItems
          .filter(
            (candidate) =>
              candidate.programType === selectedItem.programType &&
              candidate.id !== selectedItem.id,
          )
          .slice(0, 3)}
        onSelectRelated={(id) => navigate(`/volunteer/${id}`)}
      />
    );
  }

  return (
    <section className="opportunity-catalog">
      <nav className="opportunity-primary-nav" aria-label="선행 프로그램 분류">
        <button
          type="button"
          className={!primaryFilter ? 'active' : ''}
          onClick={() => {
            setPrimaryFilter(null);
            setDonationFilter('ALL');
            setRegionFilter('ALL');
          }}
        >
          전체
        </button>
        <button
          type="button"
          className={primaryFilter === 'VOLUNTEER' ? 'active' : ''}
          onClick={() => handlePrimaryFilter('VOLUNTEER')}
        >
          봉사
        </button>
        <button
          type="button"
          className={primaryFilter === 'DONATION' ? 'active' : ''}
          onClick={() => handlePrimaryFilter('DONATION')}
        >
          기부
        </button>
        <button
          type="button"
          className={primaryFilter === 'HOMETOWN' ? 'active' : ''}
          onClick={() => handlePrimaryFilter('HOMETOWN')}
        >
          고향사랑기부
        </button>
        <span className="opportunity-count" aria-live="polite">
          총 {filteredItems.length}개 프로그램
        </span>
      </nav>

      {primaryFilter === 'DONATION' && (
        <div className="opportunity-sub-nav-slot">
          <nav className="opportunity-sub-nav visible" aria-label="기부 세부 분류">
            {donationFilters.map((filter) => (
              <button
                key={filter.value}
                type="button"
                className={donationFilter === filter.value ? 'active' : ''}
                onClick={() => {
                  setDonationFilter(filter.value);
                  setRegionFilter('ALL');
                }}
              >
                {filter.label}
              </button>
            ))}
          </nav>
        </div>
      )}

      <div className="opportunity-region-filter">
        <span className="opportunity-region-label">지역</span>
        <nav className="opportunity-region-nav" aria-label="광역 지역별 프로그램">
          <button
            type="button"
            className={regionFilter === 'ALL' ? 'active' : ''}
            onClick={() => setRegionFilter('ALL')}
          >
            전체 지역 <small>{categoryFilteredItems.length}</small>
          </button>
          {availableRegions.map((region) => (
            <button
              key={region}
              type="button"
              className={regionFilter === region ? 'active' : ''}
              onClick={() => setRegionFilter(region)}
            >
              {region} <small>{regionCounts[region]}</small>
            </button>
          ))}
        </nav>
      </div>

      {loading && items.length === 0 ? (
        <div className="opportunity-state">프로그램을 불러오는 중입니다.</div>
      ) : filteredItems.length === 0 ? (
        <div className="opportunity-state">선택한 지역에 등록된 프로그램이 없습니다.</div>
      ) : (
        <div className="opportunity-table" role="table" aria-label="봉사 및 기부 프로그램">
          <div className="opportunity-table-head" role="row">
            <span role="columnheader">프로그램</span>
            <span role="columnheader">구분</span>
            <span role="columnheader">지역</span>
            <span role="columnheader">키워드</span>
            <span role="columnheader">상태</span>
            <span role="columnheader" aria-label="상세 보기" />
          </div>

          {filteredItems.map((item) => (
            <article
              className="opportunity-row"
              role="row"
              key={item.id}
              tabIndex={0}
              onClick={() => navigate(`/volunteer/${item.id}`)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' || event.key === ' ') {
                  event.preventDefault();
                  navigate(`/volunteer/${item.id}`);
                }
              }}
            >
              <div className="opportunity-program" role="cell">
                <strong>{item.title}</strong>
                <span>{item.organizer}</span>
              </div>
              <span className="opportunity-type" role="cell">
                {getProgramLabel(item.programType)}
              </span>
              <span className="opportunity-area" role="cell">
                {item.location}
              </span>
              <div className="opportunity-keywords" role="cell">
                {item.tags.slice(0, 2).map((tag) => (
                  <span key={tag}>{tag}</span>
                ))}
              </div>
              <span className="opportunity-status" role="cell">
                {item.availability}
              </span>
              <div className="opportunity-row-actions" role="cell">
                <button
                  type="button"
                  className="opportunity-action"
                  onClick={(event) => {
                    event.stopPropagation();
                    navigate(`/volunteer/${item.id}`);
                  }}
                  aria-label={`${item.title} 상세 보기 및 신청`}
                >
                  상세보기
                </button>
                {canDeleteItem(item) && (
                  <button
                    type="button"
                    className="content-list-delete-button"
                    onClick={(event) => {
                      event.stopPropagation();
                      void handleDeleteItem(item);
                    }}
                    aria-label={`${item.title} 삭제`}
                  >
                    삭제
                  </button>
                )}
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
};
