-- Questions from a Santa to their receiver, and the receiver's answers.
-- A question hangs off the Santa's assignment, so its receiver is always the one that assignment names
-- and the questions disappear with the group. number counts from 1 within one Santa's thread.
CREATE TABLE questions (
    group_id INTEGER NOT NULL,
    santa TEXT NOT NULL,
    number INTEGER NOT NULL CHECK (number >= 1),
    text TEXT NOT NULL,
    asked_at INTEGER NOT NULL,
    answer TEXT,
    answered_at INTEGER,
    answer_updated_at INTEGER,
    PRIMARY KEY (group_id, santa, number),
    FOREIGN KEY (group_id, santa) REFERENCES assignments (group_id, santa) ON DELETE CASCADE,
    CHECK ((answer IS NULL) = (answered_at IS NULL)),
    CHECK ((answer IS NULL) = (answer_updated_at IS NULL))
);
