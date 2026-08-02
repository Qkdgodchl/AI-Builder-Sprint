import React, { useEffect, useMemo, useState } from 'react';
import {
  fetchMyJournal,
  addMyNote,
  type JournalEntry,
} from '../../services/journalApi';
import { uploadPhoto, photoUrl, photoDownloadUrl } from '../../services/photoApi';

interface ActivityCalendarProps {
  showNotice: (message: string) => void;
}

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];

const toKey = (date: Date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(
    date.getDate(),
  ).padStart(2, '0')}`;

/**
 * 내가 참여한 날짜를 달력에 표시하고, 그 날의 센터 기록과 개인 기록을 함께 보여준다.
 * 데이터는 참여 건 단위로 오므로 날짜별로 다시 묶어 쓴다.
 */
export const ActivityCalendar: React.FC<ActivityCalendarProps> = ({ showNotice }) => {
  const [entries, setEntries] = useState<JournalEntry[]>([]);
  const [cursor, setCursor] = useState(() => new Date());
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [draft, setDraft] = useState('');
  const [photo, setPhoto] = useState<{ id: number; name: string } | null>(null);
  const [saving, setSaving] = useState(false);
  // 기본은 나만 보는 메모. 필요할 때만 센터에 공유한다.
  const [shareWithCenter, setShareWithCenter] = useState(false);

  const load = () => {
    fetchMyJournal()
      .then(setEntries)
      .catch((error) =>
        showNotice(error instanceof Error ? error.message : '활동 기록을 불러오지 못했습니다.'),
      );
  };

  useEffect(load, []);

  const byDate = useMemo(() => {
    const map = new Map<string, JournalEntry[]>();
    entries.forEach((entry) => {
      const list = map.get(entry.activityDate) ?? [];
      list.push(entry);
      map.set(entry.activityDate, list);
    });
    return map;
  }, [entries]);

  const days = useMemo(() => {
    const first = new Date(cursor.getFullYear(), cursor.getMonth(), 1);
    const cells: Array<Date | null> = Array(first.getDay()).fill(null);
    const lastDay = new Date(cursor.getFullYear(), cursor.getMonth() + 1, 0).getDate();
    for (let day = 1; day <= lastDay; day += 1) {
      cells.push(new Date(cursor.getFullYear(), cursor.getMonth(), day));
    }
    return cells;
  }, [cursor]);

  const selectedEntries = selectedDate ? byDate.get(selectedDate) ?? [] : [];

  const attachPhoto = async (file: File | undefined) => {
    if (!file) return;
    try {
      const uploaded = await uploadPhoto(file, 'ACTIVITY_PHOTO');
      setPhoto({ id: uploaded.id, name: uploaded.originalName });
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '사진 업로드에 실패했습니다.');
    }
  };

  const saveNote = async (applicationPublicId: string) => {
    if (!selectedDate || (!draft.trim() && !photo)) return;
    setSaving(true);
    try {
      await addMyNote(applicationPublicId, {
        activityDate: selectedDate,
        content: draft.trim(),
        fileIds: photo ? [photo.id] : [],
        shared: shareWithCenter,
      });
      setDraft('');
      setPhoto(null);
      load();
      showNotice('기록을 남겼습니다.');
    } catch (error) {
      showNotice(error instanceof Error ? error.message : '기록 저장에 실패했습니다.');
    } finally {
      setSaving(false);
    }
  };

  const monthLabel = `${cursor.getFullYear()}년 ${cursor.getMonth() + 1}월`;
  const moveMonth = (step: number) =>
    setCursor(new Date(cursor.getFullYear(), cursor.getMonth() + step, 1));

  return (
    <section className="activity-calendar" aria-label="잇다 다이어리">
      <div className="user-panel-heading activity-calendar-heading">
        <div>
          <span>MY DIARY</span>
          <h3>잇다 다이어리</h3>
        </div>
        <div className="activity-calendar-nav">
          <button type="button" onClick={() => moveMonth(-1)} aria-label="이전 달">←</button>
          <strong>{monthLabel}</strong>
          <button type="button" onClick={() => moveMonth(1)} aria-label="다음 달">→</button>
        </div>
      </div>

      <div className="activity-calendar-grid" role="grid">
        {WEEKDAYS.map((weekday) => (
          <span className="activity-calendar-weekday" key={weekday}>{weekday}</span>
        ))}
        {days.map((day, index) => {
          if (!day) return <span className="activity-calendar-cell empty" key={`pad-${index}`} />;
          const key = toKey(day);
          const dayEntries = byDate.get(key) ?? [];
          const noteCount = dayEntries.reduce((total, entry) => total + entry.notes.length, 0);
          return (
            <button
              type="button"
              key={key}
              className={`activity-calendar-cell${dayEntries.length ? ' has-activity' : ''}${
                selectedDate === key ? ' selected' : ''
              }`}
              onClick={() => setSelectedDate(dayEntries.length ? key : null)}
              disabled={!dayEntries.length}
            >
              <em>{day.getDate()}</em>
              {dayEntries.length > 0 && (
                <span className="activity-calendar-dot" aria-hidden="true">
                  {noteCount > 0 ? `기록 ${noteCount}` : '참여'}
                </span>
              )}
            </button>
          );
        })}
      </div>

      {selectedDate && selectedEntries.length > 0 && (
        <div className="activity-day-detail">
          <h4>{selectedDate} 활동</h4>
          {selectedEntries.map((entry) => (
            <article className="activity-day-card" key={entry.applicationPublicId}>
              <header>
                <strong>{entry.opportunityTitle}</strong>
                <span>{entry.organizationName}</span>
              </header>

              {entry.notes.length === 0 ? (
                <p className="activity-day-empty">아직 남겨진 기록이 없습니다.</p>
              ) : (
                entry.notes.map((note) => (
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
                      <span>{note.authorName}</span>
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

              <div className="activity-note-composer">
                <textarea
                  rows={3}
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
                    onClick={() => void saveNote(entry.applicationPublicId)}
                  >
                    {saving ? '저장 중…' : '기록 남기기'}
                  </button>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
};
