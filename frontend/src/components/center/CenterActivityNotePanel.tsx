import React, { useEffect, useState } from 'react';
import { addCenterNote, type JournalNote } from '../../services/journalApi';
import { uploadPhoto, photoUrl } from '../../services/photoApi';
import { apiRequest } from '../../services/apiClient';

interface CenterActivityNotePanelProps {
  applicationPublicId: string;
}

/**
 * 담당 센터가 참여자에게 남기는 사진과 코멘트.
 * 참여자의 봉사 다이어리 해당 날짜에 그대로 표시된다.
 */
export const CenterActivityNotePanel: React.FC<CenterActivityNotePanelProps> = ({
  applicationPublicId,
}) => {
  const [notes, setNotes] = useState<JournalNote[]>([]);
  const [activityDate, setActivityDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [content, setContent] = useState('');
  const [photo, setPhoto] = useState<{ id: number; name: string } | null>(null);
  const [status, setStatus] = useState('');
  const [saving, setSaving] = useState(false);

  const load = () => {
    apiRequest<JournalNote[]>(`/api/v1/applications/${applicationPublicId}/notes`)
      .then(setNotes)
      .catch(() => setNotes([]));
  };

  useEffect(load, [applicationPublicId]);

  const attachPhoto = async (file: File | undefined) => {
    if (!file) return;
    try {
      const uploaded = await uploadPhoto(file, 'ACTIVITY_PHOTO');
      setPhoto({ id: uploaded.id, name: uploaded.originalName });
      setStatus('');
    } catch (error) {
      setStatus(error instanceof Error ? error.message : '사진 업로드에 실패했습니다.');
    }
  };

  const submit = async () => {
    if (!content.trim() && !photo) return;
    setSaving(true);
    try {
      const updated = await addCenterNote(applicationPublicId, {
        activityDate,
        content: content.trim(),
        fileIds: photo ? [photo.id] : [],
      });
      setNotes(updated);
      setContent('');
      setPhoto(null);
      setStatus('참여자의 다이어리에 기록을 남겼습니다.');
    } catch (error) {
      setStatus(error instanceof Error ? error.message : '기록 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="center-note-panel">
      <div className="center-note-heading">
        <span>ACTIVITY RECORD</span>
        <h3>참여자에게 남기는 기록</h3>
        <p>여기 남긴 사진과 코멘트는 참여자의 봉사 다이어리에 그대로 표시됩니다.</p>
      </div>

      {notes.length > 0 && (
        <div className="center-note-list">
          {notes.map((note) => (
            <div
              className={`activity-note activity-note-${note.authorType.toLowerCase()}`}
              key={note.publicId}
            >
              <div className="activity-note-meta">
                <em>{note.authorType === 'CENTER' ? '센터 기록' : '참여자 기록'}</em>
                <span>{note.authorName} · {note.activityDate}</span>
              </div>
              {note.content && <p>{note.content}</p>}
              {note.photos.length > 0 && (
                <div className="activity-note-photos">
                  {note.photos.map((item) => (
                    <img key={item.fileId} src={photoUrl(item.fileId)} alt={item.originalName} />
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <div className="center-note-form">
        <label>
          <span>활동일</span>
          <input
            type="date"
            value={activityDate}
            onChange={(event) => setActivityDate(event.target.value)}
          />
        </label>
        <label>
          <span>코멘트</span>
          <textarea
            rows={3}
            value={content}
            onChange={(event) => setContent(event.target.value)}
            placeholder="참여해 주신 분께 전하고 싶은 말을 남겨주세요."
          />
        </label>
        <div className="center-note-form-actions">
          <input
            type="file"
            accept="image/png,image/jpeg"
            onChange={(event) => void attachPhoto(event.target.files?.[0])}
          />
          {photo && <small>{photo.name}</small>}
          <button
            type="button"
            disabled={saving || (!content.trim() && !photo)}
            onClick={() => void submit()}
          >
            {saving ? '남기는 중…' : '기록 남기기'}
          </button>
        </div>
        {status && <p className="center-note-status">{status}</p>}
      </div>
    </section>
  );
};
