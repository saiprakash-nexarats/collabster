import test from 'node:test';
import assert from 'node:assert/strict';

process.env.VITE_API_URL = 'http://localhost:8080';
const { api, ApiError } = await import('../src/api.js');

test('services API endpoints are fully mapped with correct functions', () => {
  assert.equal(typeof api.services.list, 'function');
  assert.equal(typeof api.services.bookings, 'function');
  assert.equal(typeof api.services.createListing, 'function');
  assert.equal(typeof api.services.book, 'function');
  assert.equal(typeof api.services.confirm, 'function');
  assert.equal(typeof api.services.cancel, 'function');
  assert.equal(typeof api.services.reviews, 'function');
  assert.equal(typeof api.services.createReview, 'function');
});

test('payments API endpoints are fully mapped with correct functions', () => {
  assert.equal(typeof api.payments.createOrder, 'function');
  assert.equal(typeof api.payments.confirmOrder, 'function');
  assert.equal(typeof api.payments.webhook, 'function');
  assert.equal(typeof api.payments.orders, 'function');
  assert.equal(typeof api.payments.reconciliation, 'function');
  assert.equal(typeof api.payments.requestPayout, 'function');
  assert.equal(typeof api.payments.payouts, 'function');
});

test('services requests construct correct URL paths, methods and authorization headers', async () => {
  const originalFetch = globalThis.fetch;
  const calls = [];
  try {
    globalThis.fetch = async (url, options = {}) => {
      calls.push({ url, options });
      return {
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ success: true, data: { ok: true, id: 'test-123' } }),
      };
    };

    const token = 'jwt-test-services-token';
    const serviceId = '00000000-0000-0000-0000-000000000001';
    const bookingId = '00000000-0000-0000-0000-000000000002';

    await api.services.list(token);
    await api.services.bookings(token);
    await api.services.createListing({ title: 'Full Stack Service' }, token);
    await api.services.book(serviceId, { notes: 'Need setup' }, token);
    await api.services.confirm(bookingId, token);
    await api.services.cancel(bookingId, token);
    await api.services.reviews(serviceId, token);
    await api.services.createReview(serviceId, { rating: 5, reviewText: 'Superb work' }, token);

    assert.equal(calls.length, 8);

    assert.equal(calls[0].url, 'http://localhost:8080/v1/services');
    assert.equal(calls[0].options.headers.Authorization, `Bearer ${token}`);

    assert.equal(calls[1].url, 'http://localhost:8080/v1/services/bookings');
    assert.equal(calls[1].options.headers.Authorization, `Bearer ${token}`);

    assert.equal(calls[2].url, 'http://localhost:8080/v1/services');
    assert.equal(calls[2].options.method, 'POST');
    assert.ok(calls[2].options.headers['Idempotency-Key']);

    assert.equal(calls[3].url, `http://localhost:8080/v1/services/${serviceId}/bookings`);
    assert.equal(calls[3].options.method, 'POST');

    assert.equal(calls[4].url, `http://localhost:8080/v1/services/bookings/${bookingId}/confirm`);
    assert.equal(calls[4].options.method, 'POST');

    assert.equal(calls[5].url, `http://localhost:8080/v1/services/bookings/${bookingId}/cancel`);
    assert.equal(calls[5].options.method, 'POST');

    assert.equal(calls[6].url, `http://localhost:8080/v1/services/${serviceId}/reviews`);

    assert.equal(calls[7].url, `http://localhost:8080/v1/services/${serviceId}/reviews`);
    assert.equal(calls[7].options.method, 'POST');
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('payments requests construct correct URL paths, methods and headers', async () => {
  const originalFetch = globalThis.fetch;
  const calls = [];
  try {
    globalThis.fetch = async (url, options = {}) => {
      calls.push({ url, options });
      return {
        ok: true,
        status: 200,
        headers: new Headers({ 'content-type': 'application/json' }),
        json: async () => ({ success: true, data: { ok: true, id: 'pay-ord-123' } }),
      };
    };

    const token = 'jwt-test-payments-token';
    const orderId = '00000000-0000-0000-0000-000000000010';

    await api.payments.orders(token);
    await api.payments.createOrder({ amountCents: 500000, currency: 'INR' }, token);
    await api.payments.confirmOrder(orderId, { gatewayPaymentId: 'pay_sim_1' }, token);
    await api.payments.reconciliation(orderId, token);
    await api.payments.requestPayout({ amountCents: 100000, destinationReference: 'BANK123' }, token);
    await api.payments.payouts(token);
    await api.payments.webhook({ event: 'payment.captured' }, 'sig_hmac_sha256');

    assert.equal(calls.length, 7);

    assert.equal(calls[0].url, 'http://localhost:8080/v1/payments/orders');
    assert.equal(calls[0].options.headers.Authorization, `Bearer ${token}`);

    assert.equal(calls[1].url, 'http://localhost:8080/v1/payments/orders');
    assert.equal(calls[1].options.method, 'POST');
    assert.ok(calls[1].options.headers['Idempotency-Key']);

    assert.equal(calls[2].url, `http://localhost:8080/v1/payments/orders/${orderId}/confirm`);
    assert.equal(calls[2].options.method, 'POST');

    assert.equal(calls[3].url, `http://localhost:8080/v1/payments/orders/${orderId}/reconciliation`);

    assert.equal(calls[4].url, 'http://localhost:8080/v1/payments/payouts');
    assert.equal(calls[4].options.method, 'POST');

    assert.equal(calls[5].url, 'http://localhost:8080/v1/payments/payouts');

    assert.equal(calls[6].url, 'http://localhost:8080/v1/payments/webhook');
    assert.equal(calls[6].options.method, 'POST');
    assert.equal(calls[6].options.headers['X-Webhook-Signature'], 'sig_hmac_sha256');
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('api rejects services and payments operations with ApiError when backend returns error status', async () => {
  const originalFetch = globalThis.fetch;
  try {
    globalThis.fetch = async () => ({
      ok: false,
      status: 409,
      headers: new Headers({ 'content-type': 'application/json' }),
      json: async () => ({ code: 'conflict', detail: 'Service slot already booked' }),
    });

    await assert.rejects(
      () => api.services.book('srv-1', { startsAt: '2026-12-01T10:00:00Z' }, 'tok'),
      (err) => err instanceof ApiError && err.status === 409
    );
  } finally {
    globalThis.fetch = originalFetch;
  }
});
