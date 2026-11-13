-- The highest question number a Santa's thread has ever used. Questions can now be deleted, so the number of the next one
-- cannot be read from the questions left: it continues after this one, and a deleted number is never given out again.
-- Threads that exist continue after their highest number.
ALTER TABLE assignments ADD COLUMN last_question_number INTEGER NOT NULL DEFAULT 0;

UPDATE assignments SET last_question_number = COALESCE(
    (SELECT MAX(number) FROM questions q WHERE q.group_id = assignments.group_id AND q.santa = assignments.santa), 0);
