import React, { useEffect, useState } from 'react';
import type { SessionUser } from '../../types';
import { fetchMyApplications, type ApplicationResponse } from '../../services/applicationApi';
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
};

export const MyPage: React.FC<MyPageProps> = ({ currentUser }) => {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [applications, setApplications] = useState<ApplicationResponse[]>([]);
  const [nickname, setNickname] = useState(currentUser.nickname);
  const [phone, setPhone] = useState('');
  const [region, setRegion] = useState('');
  const [notice, setNotice] = useState('');

  useEffect(() => {
    Promise.all([fetchMyProfile(), fetchMyApplications()])
      .then(([nextProfile, nextApplications]) => {
        setProfile(nextProfile);
        setNickname(nextProfile.nickname);
        setPhone(nextProfile.phone || '');
        setRegion(nextProfile.region || '');
        setApplications(nextApplications);
      })
      .catch((error) => {
        setNotice(error instanceof Error ? error.message : '마이페이지를 불러오지 못했습니다.');
      });
  }, []);

  const saveProfile = async (event: React.FormEvent) => {
    event.preventDefault();
    try {
      const updated = await updateMyProfile({ nickname, phone, region });
      setProfile(updated);
      setNotice('프로필을 저장했습니다.');
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '프로필 저장에 실패했습니다.');
    }
  };

  return (
    <article className="user-my-page">
      <header className="user-my-page-header">
        <p>나의 픽셀케어</p>
        <h2>마이페이지</h2>
        <span>프로필과 봉사·기부 신청 현황을 확인합니다.</span>
      </header>

      <section className="user-profile-section">
        <div>
          <span>계정</span>
          <strong>{profile?.email || currentUser.email}</strong>
        </div>
        <div>
          <span>권한</span>
          <strong>{profile?.roles.join(', ') || currentUser.roles.join(', ')}</strong>
        </div>
        <div>
          <span>온기 온도</span>
          <strong>{profile?.temperature ?? 0}°C</strong>
        </div>
      </section>

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
        <button type="submit">프로필 저장</button>
      </form>

      <section className="user-application-section">
        <div className="user-application-heading">
          <h3>나의 신청 내역</h3>
          <span>총 {applications.length}건</span>
        </div>
        {applications.length === 0 ? (
          <p className="user-application-empty">아직 신청한 프로그램이 없습니다.</p>
        ) : (
          applications.map((application) => (
            <div className="user-application-row" key={application.publicId}>
              <div>
                <strong>{application.opportunityTitle}</strong>
                <span>{application.organizationName}</span>
              </div>
              <span>{application.submittedAt?.slice(0, 10) || '-'}</span>
              <strong>{statusLabel[application.status] || application.status}</strong>
            </div>
          ))
        )}
      </section>
      {notice && <div className="center-notice">{notice}</div>}
    </article>
  );
};
