import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { SessionUser } from '../../types';
import { fetchMyJournal, addMyNote, type JournalEntry, type JournalNote } from '../../services/journalApi';
import { uploadPhoto, photoUrl, photoDownloadUrl } from '../../services/photoApi';

interface DiaryDayPageProps {
  currentUser: SessionUser;
}

interface DayNote extends JournalNote {
  opportunityTitle: string;
  organizationName: string;
}

/** 하루치 활동만 따로 펼쳐 보는 페이지. 캘린더에서 날짜를 누르면 여기로 온다. */
export const DiaryDayPage: React.FC<DiaryDayPageProps> = ({ currentUser }) => {
  const navigate = useNavigate();
  const { date = '' } = useParams();
  const [entries, setEntries] = useState<JournalEntry[]>([]);
  const [draft, setDraft] = useState('');
  const [photo, setPhoto] = useState<{ id: number; name: string } | null>(null);
  const [shareWithCenter, setShareWithCenter] = useState(false);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState('');

  const load = () => {
    fetchMyJournal()
      .then(setEntries)
      .catch((error) =>
        setNotice(error instanceof Error ? error.message : '활동 기록을 불러오지 못했습니다.'),
      );
  };

  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    window.scrollTo({ top: 0 });
  }, [date]);

  /** 이 날 참여한 활동. 기록 작성은 여기에 붙는다. */
  const dayEntries = useMemo(
    () => entries.filter((entry) => entry.activityDate === date),
    [entries, date],
  );

  /** 같은 활동의 기록은 흩어지지 않도록 그 활동의 기준일에 함께 모아 본다. */
  const dayNotes = useMemo<DayNote[]>(
    () =>
      dayEntries
        .flatMap((entry) =>
          entry.notes.map((note) => ({
            ...note,
            opportunityTitle: entry.opportunityTitle,
            organizationName: entry.organizationName,
          })),
        )
        .sort((a, b) => a.createdAt.localeCompare(b.createdAt)),
    [dayEntries],
  );

  const attachPhoto = async (file: File | undefined) => {
    if (!file) return;
    try {
      const uploaded = await uploadPhoto(file, 'ACTIVITY_PHOTO');
      setPhoto({ id: uploaded.id, name: uploaded.originalName });
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '사진 업로드에 실패했습니다.');
    }
  };

  const saveNote = async (applicationPublicId: string) => {
    if (!draft.trim() && !photo) return;
    setSaving(true);
    try {
      await addMyNote(applicationPublicId, {
        content: draft.trim(),
        fileIds: photo ? [photo.id] : [],
        shared: shareWithCenter,
      });
      setDraft('');
      setPhoto(null);
      load();
      setNotice('기록을 남겼습니다.');
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '기록 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const writableEntry = dayEntries[0] ?? null;

  return (
    <article className="diary-day-page">
      <div className="my-center-detail-nav">
        <button type="button" onClick={() => navigate('/my-page')}>
          잇다 다이어리로 돌아가기
        </button>
        <span>MY DIARY</span>
      </div>

      <header className="diary-day-header">
        <span>{currentUser.nickname}님의 기록</span>
        <h2>{date}</h2>
        <p>
          {dayEntries.length > 0
            ? `이 날 참여한 활동 ${dayEntries.length}건과 남겨진 기록 ${dayNotes.length}개입니다.`
            : `이 날 남겨진 기록 ${dayNotes.length}개입니다.`}
        </p>
      </header>

      {dayEntries.length > 0 && (
        <section className="diary-day-section">
          <h3>이 날의 활동</h3>
          {dayEntries.map((entry) => (
            <div className="diary-day-activity" key={entry.applicationPublicId}>
              <strong>{entry.opportunityTitle}</strong>
              <span>{entry.organizationName}</span>
            </div>
          ))}
        </section>
      )}

      <section className="diary-day-section">
        <h3>남겨진 기록</h3>
        {dayNotes.length === 0 ? (
          <p className="activity-day-empty">아직 이 날의 기록이 없습니다.</p>
        ) : (
          dayNotes.map((note) => (
            <div
              className={`activity-note activity-note-${note.authorType.toLowerCase()}`}
              key={note.publicId}
            >
              <div className="activity-note-meta">
                <em>{note.authorType === 'CENTER' ? '센터 기록' : '내 기록'}</em>
                {note.authorType === 'USER' && (
                  <b className={`activity-note-visibility ${note.visibility.toLowerCase()}`}>
                    {note.visibility === 'PRIVATE' ? '나만 보기' : '센터 공유'}
                  </b>
                )}
                <span>{note.authorName} · {note.opportunityTitle}</span>
              </div>
              {note.content && <p>{note.content}</p>}
              {note.photos.length > 0 && (
                <div className="activity-note-photos">
                  {note.photos.map((item) => (
                    <figure key={item.fileId}>
                      <img src={photoUrl(item.fileId)} alt={item.originalName} />
                      <a href={photoDownloadUrl(item.fileId)}>사진 저장</a>
                    </figure>
                  ))}
                </div>
              )}
            </div>
          ))
        )}
      </section>

      {writableEntry && (
        <section className="diary-day-section">
          <h3>이 날의 기록 남기기</h3>
          <div className="activity-note-composer">
            <textarea
              rows={4}
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              placeholder={
                shareWithCenter
                  ? '센터에도 전달될 내용입니다.'
                  : '나만 보는 메모입니다. 센터에는 전달되지 않아요.'
              }
            />
            <div className="activity-note-composer-actions">
              <input
                type="file"
                accept="image/png,image/jpeg"
                onChange={(event) => void attachPhoto(event.target.files?.[0])}
              />
              {photo && <small>{photo.name}</small>}
              <label className="activity-note-share">
                <input
                  type="checkbox"
                  checked={shareWithCenter}
                  onChange={(event) => setShareWithCenter(event.target.checked)}
                />
                센터에도 공유
              </label>
              <button
                type="button"
                disabled={saving || (!draft.trim() && !photo)}
                onClick={() => void saveNote(writableEntry.applicationPublicId)}
              >
                {saving ? '저장 중…' : '기록 남기기'}
              </button>
            </div>
          </div>
        </section>
      )}

      {notice && <div className="center-notice">{notice}</div>}
    </article>
  );
};
