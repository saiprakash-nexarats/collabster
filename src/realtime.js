/**
 * Collobster Realtime WebSocket Client - STOMP 1.2
 * 
 * Features:
 * - Exponential backoff with jitter on reconnection
 * - Duplicate event deduplication
 * - Token refresh callback support
 * - Clean teardown on logout
 * - Multi-destination subscription (/user/queue/updates, /user/queue/notifications, /user/queue/messages)
 */

let activeSocket;
const seenEventIds = new Set();
const MAX_SEEN = 500;

export function frame(command, headers = {}, body = '') {
  return `${command}\n${Object.entries(headers).map(([key, value]) => `${key}:${value}`).join('\n')}\n\n${body}\0`;
}

export function parseRealtimeFrames(raw) {
  return String(raw)
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .split('\0')
    .filter(Boolean)
    .map(chunk => {
      const [head, ...parts] = chunk.split('\n\n');
      const [command, ...lines] = head.split('\n');
      const headers = Object.fromEntries(
        lines.filter(Boolean).map(line => {
          const index = line.indexOf(':');
          return index < 0 ? [line, ''] : [line.slice(0, index), line.slice(index + 1)];
        })
      );
      const body = parts.join('\n\n');
      try {
        return { command, headers, body: body ? JSON.parse(body) : null };
      } catch {
        return { command, headers, body: null };
      }
    });
}

export function sendRealtime(destination, body) {
  if (activeSocket?.readyState !== WebSocket.OPEN) return false;
  activeSocket.send(frame('SEND', { destination, 'content-type': 'application/json' }, JSON.stringify(body)));
  return true;
}

export function getWebSocketUrl() {
  if (typeof window === 'undefined') return '';
  const configured = import.meta.env?.VITE_WS_URL;
  if (configured) return configured;
  const apiUrl = import.meta.env?.VITE_API_URL;
  if (apiUrl && /^https?:\/\//i.test(apiUrl)) {
    try {
      const origin = new URL(apiUrl).origin;
      return origin.replace(/^http/i, 'ws') + '/ws/community';
    } catch (_) {}
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws/community`;
}

export function isDuplicateEvent(event) {
  if (!event || typeof event !== 'object') return false;
  const id = event.id || event.eventId || event.messageId || event.clientMessageId;
  if (!id) return false;
  if (seenEventIds.has(id)) return true;
  if (seenEventIds.size >= MAX_SEEN) {
    const first = seenEventIds.values().next().value;
    seenEventIds.delete(first);
  }
  seenEventIds.add(id);
  return false;
}

export function connectRealtime(tokenOrGetter, { onEvent, onState } = {}) {
  if (!tokenOrGetter || typeof WebSocket === 'undefined') return () => {};
  const url = getWebSocketUrl();
  if (!url) return () => {};

  let socket;
  let closed = false;
  let retryTimer;
  let attempt = 0;

  const getBackoffMs = (att) => {
    const base = 1000 * Math.pow(1.5, Math.min(att, 7));
    const jitter = Math.random() * 500;
    return Math.min(30000, Math.floor(base + jitter));
  };

  const getToken = () => {
    return typeof tokenOrGetter === 'function' ? tokenOrGetter() : tokenOrGetter;
  };

  const open = () => {
    if (closed) return;
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      // Defer connection until network returns
      const onOnline = () => {
        window.removeEventListener('online', onOnline);
        if (!closed) open();
      };
      window.addEventListener('online', onOnline);
      return;
    }

    const currentToken = getToken();
    if (!currentToken) {
      onState?.('unauthenticated');
      return;
    }

    try {
      socket = new WebSocket(url);
    } catch (err) {
      scheduleRetry();
      return;
    }

    socket.onopen = () => {
      activeSocket = socket;
      attempt = 0;
      onState?.('connected');
      socket.send(
        frame('CONNECT', {
          Authorization: `Bearer ${currentToken}`,
          'accept-version': '1.2',
          'heart-beat': '10000,10000'
        })
      );
    };

    socket.onmessage = (event) => {
      const packets = parseRealtimeFrames(event.data);
      for (const packet of packets) {
        if (packet.command === 'CONNECTED') {
          // Subscribe to user queue updates, notifications, and direct messages
          socket.send(frame('SUBSCRIBE', { id: 'sub-updates', destination: '/user/queue/updates', ack: 'auto' }));
          socket.send(frame('SUBSCRIBE', { id: 'sub-notifications', destination: '/user/queue/notifications', ack: 'auto' }));
          socket.send(frame('SUBSCRIBE', { id: 'sub-messages', destination: '/user/queue/messages', ack: 'auto' }));
        } else if (packet.command === 'MESSAGE' && packet.body) {
          if (!isDuplicateEvent(packet.body)) {
            onEvent?.(packet.body);
          }
        }
      }
    };

    socket.onclose = () => {
      if (activeSocket === socket) activeSocket = undefined;
      if (!closed) {
        onState?.('disconnected');
        scheduleRetry();
      }
    };

    socket.onerror = () => {
      socket.close();
    };
  };

  const scheduleRetry = () => {
    if (closed) return;
    clearTimeout(retryTimer);
    const delay = getBackoffMs(attempt++);
    retryTimer = setTimeout(open, delay);
  };

  open();

  return () => {
    closed = true;
    clearTimeout(retryTimer);
    if (activeSocket === socket) activeSocket = undefined;
    if (socket?.readyState === WebSocket.OPEN) {
      socket.send(frame('DISCONNECT'));
    }
    socket?.close();
  };
}
