DELETE FROM volunteer_tags
WHERE tag = '1365 연동';

ALTER TABLE volunteers
    DROP COLUMN link1365;
