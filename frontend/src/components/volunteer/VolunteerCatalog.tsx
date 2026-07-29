import React, { useEffect, useMemo, useState } from 'react';
import type { VolunteerItem } from '../../types';
import { fetchVolunteers } from '../../services/volunteerApi';
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
}

const donationFilters: Array<{ value: DonationFilter; label: string }> = [
  { value: 'ALL', label: '전체 기부' },
  { value: 'GENERAL', label: '일반기부' },
  { value: 'LEGACY', label: '유산기부' },
  { value: 'UNESCO', label: '유네스코 후원' },
  { value: 'HERITAGE', label: '문화유산 후원' },
];

const featuredPrograms: CatalogItem[] = [
  {
    id: -1,
    title: '지역 아동 교육 정기 후원',
    category: 'DONATION',
    programType: 'GENERAL',
    location: '전국',
    organizer: '픽셀케어 파트너 재단',
    tags: ['정기후원', '아동·청소년'],
    availability: '상시 모집',
  },
  {
    id: -2,
    title: '나눔을 이어가는 유산기부 상담',
    category: 'DONATION',
    programType: 'LEGACY',
    location: '전국',
    organizer: '유산기부 전문 상담센터',
    tags: ['전문상담', '약정'],
    availability: '상담 가능',
  },
  {
    id: -3,
    title: '세계유산 보존 정기후원',
    category: 'DONATION',
    programType: 'UNESCO',
    location: '전 세계',
    organizer: '세계유산 보존 파트너',
    tags: ['세계유산', '보존사업'],
    availability: '상시 모집',
  },
  {
    id: -4,
    title: '우리 문화유산 지킴이 후원',
    category: 'DONATION',
    programType: 'HERITAGE',
    location: '전국',
    organizer: '지역 문화유산 센터',
    tags: ['문화유산', '복원'],
    availability: '상시 모집',
  },
  {
    id: -5,
    title: '부산 고향사랑기부제',
    category: 'DONATION',
    programType: 'HOMETOWN',
    location: '부산광역시',
    organizer: '지역 상생 기부 안내센터',
    tags: ['고향사랑기부', '지역상생'],
    availability: '신청 가능',
  },
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

export const VolunteerCatalog: React.FC = () => {
  const [items, setItems] = useState<VolunteerItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [primaryFilter, setPrimaryFilter] = useState<PrimaryFilter>(null);
  const [donationFilter, setDonationFilter] = useState<DonationFilter>('ALL');
  const [selectedItem, setSelectedItem] = useState<CatalogItem | null>(null);
  const [applicationItem, setApplicationItem] = useState<CatalogItem | null>(null);

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

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'auto' });
  }, [applicationItem, selectedItem]);

  const catalogItems = useMemo<CatalogItem[]>(() => {
    const apiPrograms = items.map<CatalogItem>((item) => ({
      ...item,
      title: removeLeadingSymbol(item.title),
      programType: item.category === 'VOLUNTEER' ? 'VOLUNTEER' : 'GENERAL',
      availability: '모집 중',
    }));

    return [...apiPrograms, ...featuredPrograms];
  }, [items]);

  const filteredItems = useMemo(() => {
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

  const handlePrimaryFilter = (filter: Exclude<PrimaryFilter, null>) => {
    setPrimaryFilter((current) => (current === filter ? null : filter));
    if (filter !== 'DONATION') setDonationFilter('ALL');
  };

  if (applicationItem) {
    return (
      <ClmApplicationPreparation
        item={applicationItem}
        typeLabel={getProgramLabel(applicationItem.programType)}
        onBack={() => setApplicationItem(null)}
      />
    );
  }

  if (selectedItem) {
    return (
      <OpportunityDetail
        item={selectedItem}
        onBack={() => setSelectedItem(null)}
        onApply={() => setApplicationItem(selectedItem)}
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

      <div className="opportunity-sub-nav-slot">
        <nav
          className={`opportunity-sub-nav ${primaryFilter === 'DONATION' ? 'visible' : ''}`}
          aria-label="기부 세부 분류"
          aria-hidden={primaryFilter !== 'DONATION'}
        >
          {donationFilters.map((filter) => (
            <button
              key={filter.value}
              type="button"
              className={donationFilter === filter.value ? 'active' : ''}
              onClick={() => setDonationFilter(filter.value)}
              tabIndex={primaryFilter === 'DONATION' ? 0 : -1}
            >
              {filter.label}
            </button>
          ))}
        </nav>
      </div>

      {loading && items.length === 0 ? (
        <div className="opportunity-state">프로그램을 불러오는 중입니다.</div>
      ) : filteredItems.length === 0 ? (
        <div className="opportunity-state">이 분류에 등록된 프로그램이 없습니다.</div>
      ) : (
        <div className="opportunity-table" role="table" aria-label="봉사 및 기부 프로그램">
          <div className="opportunity-table-head" role="row">
            <span role="columnheader">구분</span>
            <span role="columnheader">프로그램</span>
            <span role="columnheader">지역</span>
            <span role="columnheader">키워드</span>
            <span role="columnheader">상태</span>
            <span role="columnheader" aria-label="상세 보기" />
          </div>

          {filteredItems.map((item) => (
            <article className="opportunity-row" role="row" key={item.id}>
              <span className="opportunity-type" role="cell">
                {getProgramLabel(item.programType)}
              </span>
              <div className="opportunity-program" role="cell">
                <strong>{item.title}</strong>
                <span>{item.organizer}</span>
              </div>
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
              <button
                type="button"
                className="opportunity-action"
                onClick={() => setSelectedItem(item)}
                aria-label={`${item.title} 상세 보기 및 신청`}
              >
                상세보기
              </button>
            </article>
          ))}
        </div>
      )}
    </section>
  );
};
