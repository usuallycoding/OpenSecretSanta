-- A group may say how much to spend: a whole number and the currency it is in. Both stay NULL for the groups that
-- exist now and for a group without a budget; the range and the length are checked by the domain, like the other fields.
ALTER TABLE groups ADD COLUMN budget INTEGER;
ALTER TABLE groups ADD COLUMN currency TEXT;
