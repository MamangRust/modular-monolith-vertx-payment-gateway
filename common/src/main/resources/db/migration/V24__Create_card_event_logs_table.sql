CREATE TABLE "card_event_logs" (
    "event_id"     BIGSERIAL PRIMARY KEY,
    "topic"        VARCHAR(80) NOT NULL,
    "event_type"   VARCHAR(40) NOT NULL,
    "card_number"  VARCHAR(16),
    "reference_id" VARCHAR(64),
    "payload"      JSONB NOT NULL,
    "received_at"  TIMESTAMP NOT NULL DEFAULT current_timestamp
);

CREATE INDEX idx_cel_card_time ON "card_event_logs" ("card_number", "received_at");
CREATE INDEX idx_cel_type_time ON "card_event_logs" ("event_type", "received_at");
