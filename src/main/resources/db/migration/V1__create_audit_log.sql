CREATE TABLE audit_log (
                           id          BIGSERIAL PRIMARY KEY,
                           type        VARCHAR(4) NOT NULL,
                           wallet_id   TEXT NOT NULL,
                           stock_name  TEXT NOT NULL,
                           inserted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_log_inserted_at ON audit_log (inserted_at);
