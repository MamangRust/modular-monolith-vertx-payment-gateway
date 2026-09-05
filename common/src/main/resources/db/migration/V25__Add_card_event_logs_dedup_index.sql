-- Dedup card_event_logs untuk redelivery at-least-once.
-- card.limit.changed dikecualikan karena reference_id-nya = card_number
-- (banyak event limit per kartu, tidak boleh bentrok).
-- Pembersihan duplikat dilakukan dulu agar pembuatan unique index tidak gagal.

DELETE FROM "card_event_logs" a
USING "card_event_logs" b
WHERE a."event_id" > b."event_id"
  AND a."topic" = b."topic"
  AND a."topic" <> 'card.limit.changed'
  AND a."reference_id" IS NOT NULL
  AND a."reference_id" = b."reference_id";

CREATE UNIQUE INDEX idx_cel_dedup
    ON "card_event_logs" ("topic", "reference_id")
    WHERE "topic" <> 'card.limit.changed'
      AND "reference_id" IS NOT NULL;
