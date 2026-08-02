import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchMyJournal, type JournalEntry } from '../../services/journalApi';

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
  const navigate = useNavigate();
  const [entries, setEntries] = useState<JournalEntry[]>([]);
  const [cursor, setCursor] = useState(() => new Date());

  useEffect(() => {
    fetchMyJournal()
      .then(setEntries)
      .catch((error) =>
        showNotice(error instanceof Error ? error.message : '활동 기록을 불러오지 못했습니다.'),
      );
  }, []);

  /** 참여한 날과 기록이 남은 날을 각각 센다. 기록은 자기 활동일에 놓인다. */
  const summary = useMemo(() => {
    const map = new Map<string, { activities: number; notes: number }>();
    const bump = (key: string, field: 'activities' | 'notes') => {
      const current = map.get(key) ?? { activities: 0, notes: 0 };
      current[field] += 1;
      map.set(key, current);
    };
    // 같은 활동의 기록은 모두 그 활동의 기준일 한 칸에 모은다.
    entries.forEach((entry) => {
      bump(entry.activityDate, 'activities');
      entry.notes.forEach(() => bump(entry.activityDate, 'notes'));
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
          const info = summary.get(key);
          const hasSomething = Boolean(info && (info.activities || info.notes));
          return (
            <button
              type="button"
              key={key}
              className={`activity-calendar-cell${hasSomething ? ' has-activity' : ''}`}
              onClick={() => hasSomething && navigate(`/diary/${key}`)}
              disabled={!hasSomething}
            >
              <em>{day.getDate()}</em>
              {hasSomething && (
                <span className="activity-calendar-dot">
                  {info!.notes > 0 ? `기록 ${info!.notes}` : '참여'}
                </span>
              )}
            </button>
          );
        })}
      </div>

      <p className="activity-calendar-hint">날짜를 누르면 그날의 기록을 자세히 볼 수 있습니다.</p>
    </section>
  );
};
