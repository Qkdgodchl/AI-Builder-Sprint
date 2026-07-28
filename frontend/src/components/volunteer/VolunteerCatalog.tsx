import React, { useEffect, useState } from 'react';
import type { VolunteerItem } from '../../types';
import { fetchVolunteers } from '../../services/volunteerApi';
import { RegisterVolunteerModal } from './RegisterVolunteerModal';

interface VolunteerCatalogProps {
  onOpenModal: (title: string, type: 'volunteer' | 'donate') => void;
  showToast?: (message: string) => void;
}

export const VolunteerCatalog: React.FC<VolunteerCatalogProps> = ({ onOpenModal, showToast }) => {
  const [items, setItems] = useState<VolunteerItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [categoryFilter, setCategoryFilter] = useState<'ALL' | 'VOLUNTEER' | 'DONATION'>('VOLUNTEER');
  const [isRegisterModalOpen, setIsRegisterModalOpen] = useState<boolean>(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const data = await fetchVolunteers();
      setItems(data);
    } catch (err) {
      console.error('Failed to load volunteer data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleRegisterSuccess = (msg: string) => {
    if (showToast) showToast(msg);
    loadData();
  };

  const filteredItems = items.filter((item) => {
    if (categoryFilter === 'ALL') return true;
    return item.category === categoryFilter;
  });

  return (
    <div>
      {/* Header bar with controls */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '12px',
          marginBottom: '16px',
        }}
      >
        <div style={{ fontSize: '16px', fontWeight: 'bold', color: '#1a1a24' }}>
          🤝 추천 봉사 미션 & 픽셀 기부 펀딩 (1365 공공데이터 연동 API)
        </div>

        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          {/* Category Filter Tabs */}
          <div style={{ display: 'flex', gap: '4px', background: '#eee', padding: '3px', borderRadius: '6px' }}>
            <button
              className="pixel-btn"
              style={{
                fontSize: '11px',
                padding: '4px 8px',
                background: categoryFilter === 'VOLUNTEER' ? '#ff9f1c' : 'transparent',
                borderColor: categoryFilter === 'VOLUNTEER' ? '#e07a5f' : 'transparent',
                color: categoryFilter === 'VOLUNTEER' ? '#fff' : '#444',
                boxShadow: categoryFilter === 'VOLUNTEER' ? undefined : 'none',
              }}
              onClick={() => setCategoryFilter('VOLUNTEER')}
            >
              🤝 봉사 미션만
            </button>
            <button
              className="pixel-btn"
              style={{
                fontSize: '11px',
                padding: '4px 8px',
                background: categoryFilter === 'DONATION' ? '#e76f51' : 'transparent',
                borderColor: categoryFilter === 'DONATION' ? '#d62828' : 'transparent',
                color: categoryFilter === 'DONATION' ? '#fff' : '#444',
                boxShadow: categoryFilter === 'DONATION' ? undefined : 'none',
              }}
              onClick={() => setCategoryFilter('DONATION')}
            >
              ❤️ 기부 펀딩만
            </button>
            <button
              className="pixel-btn"
              style={{
                fontSize: '11px',
                padding: '4px 8px',
                background: categoryFilter === 'ALL' ? '#2ec4b6' : 'transparent',
                borderColor: categoryFilter === 'ALL' ? '#005f73' : 'transparent',
                color: categoryFilter === 'ALL' ? '#fff' : '#444',
                boxShadow: categoryFilter === 'ALL' ? undefined : 'none',
              }}
              onClick={() => setCategoryFilter('ALL')}
            >
              전체 보기
            </button>
          </div>

          {/* Add New Volunteer Mission via API Button */}
          <button
            className="pixel-btn"
            style={{
              fontSize: '11px',
              padding: '6px 12px',
              background: '#2a9d8f',
              borderColor: '#1d3557',
              color: '#fff',
            }}
            onClick={() => setIsRegisterModalOpen(true)}
          >
            ➕ 봉사 API 신규 등록
          </button>
        </div>
      </div>

      {/* Loading state */}
      {loading ? (
        <div
          className="pixel-box"
          style={{ textAlign: 'center', padding: '36px', color: '#555', fontSize: '14px' }}
        >
          ⌛ API 봉사 데이터 로딩 중...
        </div>
      ) : filteredItems.length === 0 ? (
        /* Empty state */
        <div
          className="pixel-box"
          style={{ textAlign: 'center', padding: '36px', color: '#666', fontSize: '14px' }}
        >
          📭 등록된 {categoryFilter === 'VOLUNTEER' ? '봉사 미션이' : categoryFilter === 'DONATION' ? '기부 펀딩이' : '데이터가'} 없습니다.
          <br />
          <button
            className="pixel-btn"
            style={{ marginTop: '14px', fontSize: '12px', background: '#2a9d8f', color: '#fff' }}
            onClick={() => setIsRegisterModalOpen(true)}
          >
            ➕ 첫 봉사 미션 직접 등록하기
          </button>
        </div>
      ) : (
        /* Dynamic item cards grid */
        <div className="cards-grid">
          {filteredItems.map((item) => (
            <div key={item.id} className="pixel-box item-card">
              <div>
                <div className="card-header">
                  <span className={`pixel-tag ${item.category === 'VOLUNTEER' ? 'pixel-tag-gov' : ''}`}>
                    {item.category === 'VOLUNTEER' ? '🤝 1365 봉사' : '❤️ 픽셀 기부'}
                  </span>
                  {item.link1365 && (
                    <a
                      href={
                        item.link1365 === 'https://www.1365.go.kr' || item.link1365 === 'https://www.1365.go.kr/'
                          ? `https://www.1365.go.kr/vols/1365/act/volsList.do?searchKeyword=${encodeURIComponent(
                              item.title.replace(/^[\u{1F300}-\u{1F9FF}|🐕|🌊|🍲|🌳|❤️|📚]\s*/u, '')
                            )}`
                          : item.link1365
                      }
                      target="_blank"
                      rel="noreferrer"
                      style={{ fontSize: '11px', color: '#005f73', textDecoration: 'none' }}
                    >
                      1365 원문보기 ↗
                    </a>
                  )}
                </div>

                <div className="title">{item.title}</div>
                <div className="meta">
                  📍 {item.location}
                  <br />
                  🏢 {item.organizer}
                </div>

                <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap', marginBottom: '12px' }}>
                  {item.tags.map((t, idx) => (
                    <span key={idx} className="pixel-tag" style={{ background: '#eee', color: '#333' }}>
                      {t}
                    </span>
                  ))}
                </div>
              </div>

              <button
                className={`pixel-btn ${item.category === 'DONATION' ? 'pixel-btn-red' : ''}`}
                style={{ width: '100%', fontSize: '12px' }}
                onClick={() =>
                  onOpenModal(item.title, item.category === 'DONATION' ? 'donate' : 'volunteer')
                }
              >
                {item.category === 'DONATION' ? '❤️ 픽셀 기부 참여하기' : '⚡ 1초 간편 신청하기'}
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Volunteer Registration Modal */}
      <RegisterVolunteerModal
        isOpen={isRegisterModalOpen}
        onClose={() => setIsRegisterModalOpen(false)}
        onSuccess={handleRegisterSuccess}
      />
    </div>
  );
};
