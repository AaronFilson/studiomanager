-- Invites sent by email, so a studio can paste its member list instead of sharing links one by one.
--
-- The plain code is never stored, not even in the outbox. An emailed invite is created with the
-- hash of a code nobody knows. When the email goes out, the sender makes a new code, stores its
-- hash and emails the code in one transaction. A resend does the same, so an older email's code
-- stops working.

ALTER TABLE studio_invite
    ADD COLUMN email        text CHECK (char_length(email) BETWEEN 3 AND 254),
    ADD COLUMN last_sent_at timestamptz;

-- Finding an open invite for an address when a list is pasted again
CREATE INDEX studio_invite_studio_email_idx ON studio_invite (studio_id, lower(email))
    WHERE email IS NOT NULL AND accepted_at IS NULL;
