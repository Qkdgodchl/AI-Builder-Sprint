import React, { useState } from 'react';

interface ModalProps {
  isOpen: boolean;
  title: string;
  type: 'volunteer' | 'donate';
  onClose: () => void;
  onSubmit: (name: string, amount: number) => void;
}

export const Modal: React.FC<ModalProps> = ({ isOpen, title, type, onClose, onSubmit }) => {
  const [name, setName] = useState('');
  const [amount, setAmount] = useState(10000);

  if (!isOpen) return null;

  const handleFormSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit(name.trim() || '익명용사', amount);
    setName('');
  };

  return (
    <div className="modal-overlay active" onClick={onClose}>
      <div
        className="pixel-box modal-content"
        role="dialog"
        aria-modal="true"
        aria-label={type === 'donate' ? '후원 신청' : '봉사 신청'}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ fontSize: '16px', fontWeight: 'bold', marginBottom: '10px', color: '#1a1a24' }}>
          {type === 'donate' ? '후원 신청' : '봉사 신청'}
        </div>
        <div style={{ fontSize: '13px', color: '#555', marginBottom: '14px' }}>
          선택한 미션: <b>{title}</b>
        </div>

        <form onSubmit={handleFormSubmit}>
          <div style={{ marginBottom: '12px' }}>
            <label style={{ display: 'block', fontSize: '11px', marginBottom: '4px' }}>참여용사 닉네임</label>
            <input
              type="text"
              className="pixel-input"
              style={{ width: '100%' }}
              placeholder="예: 부산 픽셀 영웅"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>

          {type === 'donate' && (
            <div style={{ marginBottom: '14px' }}>
              <label style={{ display: 'block', fontSize: '11px', marginBottom: '4px' }}>기부 마음 금액 (원)</label>
              <input
                type="number"
                className="pixel-input"
                style={{ width: '100%' }}
                step={1000}
                value={amount}
                onChange={(e) => setAmount(parseInt(e.target.value) || 0)}
              />
            </div>
          )}

          <div style={{ display: 'flex', gap: '8px', justifyContent: 'flex-end', marginTop: '16px' }}>
            <button type="button" className="pixel-btn" style={{ background: '#888', borderColor: '#aaa' }} onClick={onClose}>
              취소
            </button>
            <button type="submit" className={`pixel-btn ${type === 'donate' ? 'pixel-btn-red' : ''}`}>
              {type === 'donate' ? '후원 신청 완료' : '봉사 신청 완료'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
