import React, { useState } from 'react';
import type { SessionUser } from '../../types';

interface ManagerApplicationPageProps {
  currentUser: SessionUser;
  onBack: () => void;
  onSubmit: (centerName: string) => void;
}

export const ManagerApplicationPage: React.FC<ManagerApplicationPageProps> = ({
  currentUser,
  onBack,
  onSubmit,
}) => {
  const [centerName, setCenterName] = useState('');
  const [centerType, setCenterType] = useState('');
  const [registrationNumber, setRegistrationNumber] = useState('');
  const [representativeName, setRepresentativeName] = useState('');
  const [applicantPosition, setApplicantPosition] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [reason, setReason] = useState('');
  const [employmentProof, setEmploymentProof] = useState<File | null>(null);
  const [registrationProof, setRegistrationProof] = useState<File | null>(null);
  const [agreed, setAgreed] = useState(false);

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    onSubmit(centerName.trim());
  };

  return (
    <article className="manager-application-page">
      <div className="manager-application-nav">
        <button type="button" onClick={onBack}>
          마이페이지로 돌아가기
        </button>
        <span>MANAGER APPLICATION</span>
      </div>

      <header className="manager-application-header">
        <p>센터 관리자 권한 신청</p>
        <h2>관리자 신청 서류</h2>
        <span>
          소속 센터와 담당자 관계를 확인할 수 있는 정보를 제출하면 운영진 검토 후 권한이
          부여됩니다.
        </span>
      </header>

      <ol className="manager-application-steps" aria-label="관리자 신청 단계">
        <li className="current">
          <span>01</span>
          <strong>센터 정보</strong>
        </li>
        <li>
          <span>02</span>
          <strong>담당자 정보</strong>
        </li>
        <li>
          <span>03</span>
          <strong>입증자료</strong>
        </li>
        <li>
          <span>04</span>
          <strong>검토 요청</strong>
        </li>
      </ol>

      <form className="manager-application-form" onSubmit={handleSubmit}>
        <section className="manager-form-section">
          <div className="manager-form-section-heading">
            <span>01</span>
            <div>
              <h3>센터 정보</h3>
              <p>관리 권한을 신청할 기관 또는 센터의 기본 정보를 입력해 주세요.</p>
            </div>
          </div>

          <div className="manager-form-grid">
            <label className="wide">
              센터명
              <input
                type="text"
                value={centerName}
                onChange={(event) => setCenterName(event.target.value)}
                placeholder="예: 부산 픽셀 복지센터"
                required
              />
            </label>
            <label>
              기관 유형
              <select
                value={centerType}
                onChange={(event) => setCenterType(event.target.value)}
                required
              >
                <option value="">선택해 주세요</option>
                <option value="WELFARE">사회복지기관</option>
                <option value="NONPROFIT">비영리단체</option>
                <option value="PUBLIC">공공기관</option>
                <option value="HERITAGE">문화·유산기관</option>
                <option value="OTHER">기타</option>
              </select>
            </label>
            <label>
              사업자등록번호 또는 고유번호
              <input
                type="text"
                value={registrationNumber}
                onChange={(event) => setRegistrationNumber(event.target.value)}
                placeholder="000-00-00000"
                required
              />
            </label>
            <label>
              대표자명
              <input
                type="text"
                value={representativeName}
                onChange={(event) => setRepresentativeName(event.target.value)}
                placeholder="대표자 실명"
                required
              />
            </label>
            <label>
              센터 연락처
              <input
                type="tel"
                value={phone}
                onChange={(event) => setPhone(event.target.value)}
                placeholder="051-000-0000"
                required
              />
            </label>
            <label className="wide">
              센터 주소
              <input
                type="text"
                value={address}
                onChange={(event) => setAddress(event.target.value)}
                placeholder="도로명 주소"
                required
              />
            </label>
          </div>
        </section>

        <section className="manager-form-section">
          <div className="manager-form-section-heading">
            <span>02</span>
            <div>
              <h3>신청자 정보</h3>
              <p>로그인 계정과 센터에서의 담당 업무를 확인합니다.</p>
            </div>
          </div>

          <div className="manager-form-grid">
            <label>
              신청 계정
              <input type="email" value={currentUser.email} readOnly />
            </label>
            <label>
              신청자명
              <input type="text" value={currentUser.nickname} readOnly />
            </label>
            <label className="wide">
              센터 내 직책 또는 담당 업무
              <input
                type="text"
                value={applicantPosition}
                onChange={(event) => setApplicantPosition(event.target.value)}
                placeholder="예: 자원봉사 담당 사회복지사"
                required
              />
            </label>
            <label className="wide">
              관리자 권한 신청 사유
              <textarea
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                placeholder="센터 모집글 작성과 신청자 관리 권한이 필요한 이유를 입력해 주세요."
                rows={5}
                required
              />
            </label>
          </div>
        </section>

        <section className="manager-form-section">
          <div className="manager-form-section-heading">
            <span>03</span>
            <div>
              <h3>입증자료 제출</h3>
              <p>센터 소속과 기관 실재 여부를 확인할 수 있는 자료를 제출해 주세요.</p>
            </div>
          </div>

          <div className="manager-proof-grid">
            <label className="manager-proof-upload">
              <span>필수 자료 01</span>
              <strong>재직증명서 또는 담당자 확인서</strong>
              <p>신청자가 해당 센터 소속임을 확인할 수 있는 자료</p>
              <input
                type="file"
                accept=".pdf,.jpg,.jpeg,.png"
                onChange={(event) => setEmploymentProof(event.target.files?.[0] ?? null)}
                required
              />
              <em>{employmentProof?.name ?? 'PDF, JPG, PNG / 최대 10MB'}</em>
            </label>

            <label className="manager-proof-upload">
              <span>필수 자료 02</span>
              <strong>사업자등록증 또는 고유번호증</strong>
              <p>센터 또는 기관의 등록 정보를 확인할 수 있는 자료</p>
              <input
                type="file"
                accept=".pdf,.jpg,.jpeg,.png"
                onChange={(event) => setRegistrationProof(event.target.files?.[0] ?? null)}
                required
              />
              <em>{registrationProof?.name ?? 'PDF, JPG, PNG / 최대 10MB'}</em>
            </label>
          </div>
        </section>

        <footer className="manager-application-submit">
          <label>
            <input
              type="checkbox"
              checked={agreed}
              onChange={(event) => setAgreed(event.target.checked)}
              required
            />
            <span>
              제출한 정보가 사실이며 관리자 권한 검토를 위한 개인정보 및 입증자료 처리에
              동의합니다.
            </span>
          </label>
          <div>
            <p>제출 후 운영진 검토 전까지 신청 내용을 수정하거나 취소할 수 있습니다.</p>
            <button type="submit">관리자 권한 검토 요청</button>
          </div>
        </footer>
      </form>
    </article>
  );
};
