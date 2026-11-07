-- V39: Enforce Financial Security, Booking Concurrency, and Payout Constraints for Stage P3 Closure

-- 1. Prevent duplicate payment references from paying multiple orders
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_orders_gateway_payment
    ON payment_orders(gateway_payment_id)
    WHERE gateway_payment_id IS NOT NULL AND status = 'paid';

-- 2. Payout idempotency and unique tracking
ALTER TABLE payout_requests ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);
CREATE UNIQUE INDEX IF NOT EXISTS uq_payout_requests_idempotency
    ON payout_requests(idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- 3. Prevent double-booking same service slot under concurrent requests
CREATE UNIQUE INDEX IF NOT EXISTS uq_service_bookings_slot
    ON service_bookings(service_id, starts_at)
    WHERE starts_at IS NOT NULL AND status NOT IN ('cancelled', 'rejected', 'expired');

-- 4. Ledger integrity: Prevent negative balance snapshots
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_financial_ledger_balance'
    ) THEN
        ALTER TABLE financial_ledger_entries ADD CONSTRAINT chk_financial_ledger_balance CHECK (balance_after_cents >= 0);
    END IF;
END $$;
