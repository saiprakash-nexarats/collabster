-- V38: Create Services Marketplace and Payments Schema for Stage P3

-- ============================================================================
-- 1. SERVICES MARKETPLACE MODULE
-- ============================================================================

CREATE TABLE IF NOT EXISTS service_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(64) NOT NULL DEFAULT 'software',
    service_area VARCHAR(255) DEFAULT 'Remote (Global)',
    service_radius_km INT DEFAULT 15,
    starting_price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    price_cents BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    duration VARCHAR(64) DEFAULT '3 days',
    pricing_unit VARCHAR(64) DEFAULT 'Service visit',
    delivery_mode VARCHAR(64) DEFAULT 'Remote (Global)',
    pricing_type VARCHAR(64) DEFAULT 'Fixed Package',
    highlights TEXT,
    state VARCHAR(32) NOT NULL DEFAULT 'active',
    average_rating NUMERIC(3, 2) DEFAULT 0.00,
    review_count INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_service_listings_state CHECK (state IN ('draft', 'active', 'paused', 'archived')),
    CONSTRAINT chk_service_listings_price CHECK (price_cents >= 0)
);

CREATE INDEX IF NOT EXISTS idx_service_listings_provider ON service_listings(provider_id);
CREATE INDEX IF NOT EXISTS idx_service_listings_category ON service_listings(category);
CREATE INDEX IF NOT EXISTS idx_service_listings_state ON service_listings(state);
CREATE INDEX IF NOT EXISTS idx_service_listings_created ON service_listings(created_at DESC);

CREATE TABLE IF NOT EXISTS service_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES service_listings(id) ON DELETE CASCADE,
    customer_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    order_id VARCHAR(64),
    title VARCHAR(255) NOT NULL,
    category VARCHAR(64),
    tier_name VARCHAR(128) DEFAULT 'Standard Package',
    price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    amount_cents BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    delivery_mode VARCHAR(64) DEFAULT 'Remote',
    status VARCHAR(32) NOT NULL DEFAULT 'requested',
    starts_at TIMESTAMP WITH TIME ZONE,
    service_address VARCHAR(255),
    notes TEXT,
    milestones TEXT,
    payment_order_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_service_bookings_status CHECK (status IN ('requested', 'confirmed', 'in_progress', 'completed', 'cancelled', 'rejected', 'expired'))
);

CREATE INDEX IF NOT EXISTS idx_service_bookings_service ON service_bookings(service_id);
CREATE INDEX IF NOT EXISTS idx_service_bookings_customer ON service_bookings(customer_id);
CREATE INDEX IF NOT EXISTS idx_service_bookings_provider ON service_bookings(provider_id);
CREATE INDEX IF NOT EXISTS idx_service_bookings_status ON service_bookings(status);
CREATE INDEX IF NOT EXISTS idx_service_bookings_starts ON service_bookings(service_id, starts_at);

CREATE TABLE IF NOT EXISTS service_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES service_listings(id) ON DELETE CASCADE,
    booking_id UUID REFERENCES service_bookings(id) ON DELETE SET NULL,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    author_name VARCHAR(255),
    rating INT NOT NULL,
    review_text TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_service_reviews_rating CHECK (rating >= 1 AND rating <= 5)
);

CREATE INDEX IF NOT EXISTS idx_service_reviews_service ON service_reviews(service_id);
CREATE INDEX IF NOT EXISTS idx_service_reviews_author ON service_reviews(author_id);

-- ============================================================================
-- 2. PAYMENTS & PAYOUTS MODULE
-- ============================================================================

CREATE TABLE IF NOT EXISTS payment_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    order_number VARCHAR(64) NOT NULL UNIQUE,
    idempotency_key VARCHAR(128) UNIQUE,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(32) NOT NULL DEFAULT 'created',
    description VARCHAR(512),
    booking_id UUID REFERENCES service_bookings(id) ON DELETE SET NULL,
    gateway_provider VARCHAR(32) NOT NULL DEFAULT 'SANDBOX',
    gateway_order_id VARCHAR(128),
    gateway_payment_id VARCHAR(128),
    paid_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_orders_amount CHECK (amount_cents > 0),
    CONSTRAINT chk_payment_orders_status CHECK (status IN ('created', 'pending', 'authorized', 'paid', 'failed', 'cancelled', 'refunded'))
);

CREATE INDEX IF NOT EXISTS idx_payment_orders_user ON payment_orders(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_orders_status ON payment_orders(status);
CREATE INDEX IF NOT EXISTS idx_payment_orders_created ON payment_orders(created_at DESC);

CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES payment_orders(id) ON DELETE CASCADE,
    transaction_type VARCHAR(32) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    gateway_provider VARCHAR(32) NOT NULL DEFAULT 'SANDBOX',
    gateway_transaction_id VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    raw_response TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_order ON payment_transactions(order_id);

CREATE TABLE IF NOT EXISTS payment_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(128) NOT NULL UNIQUE,
    provider VARCHAR(32) NOT NULL DEFAULT 'SANDBOX',
    event_type VARCHAR(64) NOT NULL,
    payload TEXT NOT NULL,
    signature VARCHAR(256),
    processed BOOLEAN NOT NULL DEFAULT false,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_payment_webhook_events_id ON payment_webhook_events(event_id);

CREATE TABLE IF NOT EXISTS payout_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    method VARCHAR(32) NOT NULL DEFAULT 'bank_account',
    destination_reference VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    gateway_payout_id VARCHAR(128),
    notes TEXT,
    processed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payout_requests_amount CHECK (amount_cents > 0),
    CONSTRAINT chk_payout_requests_status CHECK (status IN ('pending', 'approved', 'processing', 'completed', 'rejected', 'failed'))
);

CREATE INDEX IF NOT EXISTS idx_payout_requests_provider ON payout_requests(provider_id);
CREATE INDEX IF NOT EXISTS idx_payout_requests_status ON payout_requests(status);

CREATE TABLE IF NOT EXISTS financial_ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entry_type VARCHAR(32) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    balance_after_cents BIGINT NOT NULL,
    reference_type VARCHAR(32),
    reference_id UUID,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_financial_ledger_user ON financial_ledger_entries(user_id, created_at DESC);
