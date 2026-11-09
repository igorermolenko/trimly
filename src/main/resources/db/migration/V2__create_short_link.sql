-- url-shortener: the short-link table (ShortLink aggregate, owned by the `link` module).
-- Rows are never deleted (spec §3, ADR-0002), so a short code is never reissued (AC-20).
-- Time columns are written by the app from the injected Clock; there is no DB DEFAULT now() (CLAUDE.md).

CREATE TABLE IF NOT EXISTS short_link (
    -- UUIDv7 from shared.IdGenerator, monotonic within a millisecond (feature ADR-0004).
    -- The PK index also serves the list: WHERE id < :cursor ORDER BY id DESC LIMIT :n.
    id          UUID        NOT NULL,
    -- 7 characters from the 56-character alphabet. The alphabet itself is defined only in link.domain,
    -- so SQL checks the length only. COLLATE "C" keeps comparison byte-wise and case-sensitive
    -- whatever the database locale is.
    short_code  TEXT        COLLATE "C" NOT NULL,
    -- Stored exactly as submitted, never normalised (AC-01, AC-05); at most 2048 characters (AC-04).
    target_url  TEXT        NOT NULL,
    state       TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    -- Time of the last real state change; equals created_at until the first disable.
    -- A repeated disable/re-enable leaves it unchanged (AC-14).
    updated_at  TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_short_link PRIMARY KEY (id),
    -- The create use case detects a code collision by this constraint name and retries (ADR-0002, AC-20b).
    CONSTRAINT uq_short_link_short_code UNIQUE (short_code),
    CONSTRAINT ck_short_link_short_code_length CHECK (char_length(short_code) = 7),
    CONSTRAINT ck_short_link_target_url_length CHECK (char_length(target_url) BETWEEN 1 AND 2048),
    CONSTRAINT ck_short_link_state CHECK (state IN ('ACTIVE', 'DISABLED'))
);
