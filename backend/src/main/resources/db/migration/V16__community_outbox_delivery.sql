CREATE TABLE community_outbox_delivery (
                                           id UUID PRIMARY KEY,
                                           event_id UUID NOT NULL REFERENCES community_outbox(id),
                                           consumer_name VARCHAR(150) NOT NULL,
                                           attempts INTEGER NOT NULL DEFAULT 0,
                                           available_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           delivered_at TIMESTAMPTZ,
                                           failed_at TIMESTAMPTZ,
                                           last_error TEXT,
                                           UNIQUE (event_id, consumer_name)
);

CREATE INDEX idx_outbox_delivery_pending
    ON community_outbox_delivery(delivered_at, failed_at, available_at);
