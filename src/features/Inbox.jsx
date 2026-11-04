import React, { useMemo, useRef, useState, useEffect } from 'react';
import {
  Archive,
  AtSign,
  Bell,
  BellOff,
  Bookmark,
  Check,
  CheckCheck,
  ChevronLeft,
  Copy,
  Edit3,
  FileText,
  Forward,
  GripHorizontal,
  Image,
  Info,
  Link2,
  Maximize2,
  MessageCircle,
  Mic,
  MicOff,
  Minimize2,
  MoreHorizontal,
  Paperclip,
  Pause,
  Phone,
  PhoneOff,
  Pin,
  Play,
  Plus,
  Reply,
  Search,
  Send,
  Settings,
  ShieldAlert,
  Smile,
  Star,
  User,
  UserMinus,
  UserPlus,
  Users,
  Video,
  VideoOff,
  Volume2,
  Trash2,
  X,
} from 'lucide-react';
import { useApp } from '../context';
import {
  Avatar,
  Button,
  Dialog,
  Empty,
  IconButton,
  PageHeader,
  SearchField,
  Tabs,
  Tag,
} from '../components';
import { people, photos } from '../data';
import { api } from '../api';

// ─── Constants & Seed Data ───────────────────────────────────────────────────

export const initialCallLog = [
  ['Design Systems Team', 'Group video · 24 min', 'Today, 2:15 PM', 'video', ['Riya Sharma', 'Aman Verma', 'You']],
  ['Riya Sharma', 'Video · 12 min', 'Today, 9:40 AM', 'video'],
  ['Aman Verma', 'Incoming audio · 5 min', 'Yesterday', 'audio'],
  ['Neha Patel', 'Missed audio call', 'Yesterday', 'audio'],
];

const assets = [
  ['Project overview.pdf', 'PDF · 2.4 MB'],
  ['Design-system.fig', 'FIG · 18.6 MB'],
  ['Roadmap.xlsx', 'XLSX · 240 KB'],
];

const latest = (thread) =>
  thread.messages.at(-1) || {
    text: 'Start a conversation',
    time: thread.context,
  };

// ─── 1. Utilities ────────────────────────────────────────────────────────────

export function formatDuration(seconds = 0) {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
}

function formatBytes(bytes = 0) {
  return bytes >= 1024 * 1024
    ? `${(bytes / (1024 * 1024)).toFixed(1)} MB`
    : `${Math.max(1, Math.round(bytes / 1024))} KB`;
}

function formatTimeSinceSent(message) {
  if (!message) return 'Just now';
  let timeMs = message.timestamp || (message.createdAt ? new Date(message.createdAt).getTime() : null);
  if (!timeMs) {
    if (message.time === 'Now' || message.time === 'Just now') return 'Just now';
    if (typeof message.time === 'string' && message.time.includes('ago')) return message.time;
    return message.time || 'Just now';
  }
  const diffMs = Math.max(0, Date.now() - timeMs);
  const diffSec = Math.floor(diffMs / 1000);
  if (diffSec < 60) return 'Just now';
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHr = Math.floor(diffMin / 60);
  if (diffHr < 24) return `${diffHr}h ago`;
  const diffDays = Math.floor(diffHr / 24);
  return `${diffDays}d ago`;
}

function UtilityRow({ name, detail, time, action, icon: IconComponent = Phone, onContextMenu, members }) {
  const memberList = Array.isArray(members) && members.length > 0
    ? members.map((m) => (typeof m === 'string' ? m : m.name || m)).join(', ')
    : null;

  return (
    <div className="utility-row" onContextMenu={onContextMenu}>
      <Avatar
        person={people.find((person) => person.name === name)}
        name={name}
      />
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <strong>{name}</strong>
          {IconComponent && (
            <IconComponent size={13} style={{ color: 'var(--muted)', opacity: 0.8 }} />
          )}
        </div>
        <small>{detail}</small>
        {memberList && (
          <small style={{ display: 'block', color: 'var(--muted)', fontSize: 11, marginTop: 2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            👥 {memberList}
          </small>
        )}
      </div>
      {time && <em>{time}</em>}
      {action}
    </div>
  );
}

function Header({ view, setView }) {
  return (
    <>
      <PageHeader
        eyebrow="CONVERSATIONS"
        title="Messages with momentum."
        description="Move naturally from a shared idea to a practical next step."
      />
      <Tabs
        items={['Messages', 'Calls']}
        value={view}
        onChange={setView}
      />
    </>
  );
}

function VoiceMessagePlayer({ item }) {
  const [isPlaying, setIsPlaying] = useState(false);
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    let interval;
    if (isPlaying) {
      interval = setInterval(() => {
        setProgress((prev) => {
          if (prev >= 100) {
            setIsPlaying(false);
            return 0;
          }
          return prev + 4;
        });
      }, 120);
    }
    return () => clearInterval(interval);
  }, [isPlaying]);

  const togglePlay = (e) => {
    e.stopPropagation();
    if (isPlaying) {
      setIsPlaying(false);
    } else {
      if (progress >= 100) setProgress(0);
      setIsPlaying(true);
    }
  };

  return (
    <div className="voice-message-player" onClick={(e) => e.stopPropagation()}>
      <button
        type="button"
        className="voice-play-btn"
        onClick={togglePlay}
        aria-label={isPlaying ? 'Pause voice message' : 'Play voice message'}
        title={isPlaying ? 'Pause' : 'Play'}
      >
        {isPlaying ? <Pause size={15} /> : <Play size={15} style={{ marginLeft: 2 }} />}
      </button>
      <div className="voice-info">
        <div className="voice-title-row">
          <span className="voice-filename">{item.filename || 'Voice note.m4a'}</span>
          <small className="voice-duration">
            {formatBytes(item.expectedBytes)}
          </small>
        </div>
        <div
          className="voice-wave-container"
          onClick={(e) => {
            const rect = e.currentTarget.getBoundingClientRect();
            const clickX = e.clientX - rect.left;
            const newPct = Math.max(0, Math.min(100, (clickX / rect.width) * 100));
            setProgress(newPct);
          }}
        >
          <div className="voice-wave-track">
            <div className="voice-wave-progress" style={{ width: `${progress}%` }} />
          </div>
        </div>
      </div>
    </div>
  );
}

function NewCallModal({ onClose, onStartCall }) {
  const [search, setSearch] = useState('');

  const candidates = people.filter((p) =>
    `${p.name} ${p.role || ''}`.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <Dialog title="Start a new call" onClose={onClose}>
      <div className="new-call-modal">
        <p style={{ color: 'var(--muted)', fontSize: 13, margin: '0 0 12px' }}>
          Select a team member or collaborator to start an instant audio or video call.
        </p>

        <SearchField
          value={search}
          onChange={setSearch}
          placeholder="Search people to call..."
          label="Search people"
        />

        <div
          className="candidate-list"
          style={{
            marginTop: 14,
            maxHeight: 320,
            overflowY: 'auto',
            display: 'flex',
            flexDirection: 'column',
            gap: 8,
          }}
        >
          {candidates.length === 0 ? (
            <div style={{ textAlign: 'center', padding: 24, color: 'var(--muted)' }}>
              No contacts found matching "{search}"
            </div>
          ) : (
            candidates.map((person) => (
              <div
                key={person.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 12,
                  padding: '10px 12px',
                  background: 'var(--canvas)',
                  border: '1px solid var(--line)',
                  borderRadius: 7,
                }}
              >
                <Avatar person={person} name={person.name} />
                <div style={{ flex: 1, minWidth: 0 }}>
                  <strong style={{ display: 'block', fontSize: 14 }}>{person.name}</strong>
                  <small style={{ color: 'var(--muted)', fontSize: 12 }}>{person.role}</small>
                </div>
                <div style={{ display: 'flex', gap: 6, flexShrink: 0 }}>
                  <Button
                    variant="quiet"
                    icon={Phone}
                    className="call-audio-btn"
                    onClick={() => {
                      onClose();
                      onStartCall(person.name, 'audio', false);
                    }}
                    title={`Audio call ${person.name}`}
                  >
                    <span className="call-action-label">Audio</span>
                  </Button>
                  <Button
                    variant="primary"
                    icon={Video}
                    className="call-video-btn"
                    onClick={() => {
                      onClose();
                      onStartCall(person.name, 'video', false);
                    }}
                    title={`Video call ${person.name}`}
                  >
                    <span className="call-action-label">Video</span>
                  </Button>
                </div>
              </div>
            ))
          )}
        </div>

        <div className="dialog-actions" style={{ marginTop: 14 }}>
          <Button variant="quiet" onClick={onClose}>
            Cancel
          </Button>
        </div>
      </div>
    </Dialog>
  );
}

// ─── 2. Messages Main Component (Exported) ───────────────────────────────────

export function Messages() {
  const a = useApp();
  const [view, setView] = useState('Messages');
  const [filter, setFilter] = useState('All'); // Requirement 1: 'All' is first before chats
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(a.activeThreadId || null);
  const [draft, setDraft] = useState('');
  const [info, setInfo] = useState(false);
  const [starred, setStarred] = useState(['m1']);
  const [disappearing, setDisappearing] = useState('Off');
  const [localCallLog, setLocalCallLog] = useState(initialCallLog);
  const callLog = a?.callLog !== undefined ? a.callLog : localCallLog;
  const setCallLog = a?.setCallLog || setLocalCallLog;
  const [callContextMenu, setCallContextMenu] = useState(null);
  const [pinnedThreads, setPinnedThreads] = useState({ riya: Date.now() }); // threadId -> timestamp
  const [blockedUsers, setBlockedUsers] = useState([]); // Array of blocked user names
  const [mutedThreads, setMutedThreads] = useState([]); // Array of muted threadIds
  const [archivedThreads, setArchivedThreads] = useState([]); // Array of archived threadIds
  const [threadContextMenu, setThreadContextMenu] = useState(null); // { thread, x, y }
  const [localActiveCall, setLocalActiveCall] = useState(null);
  const activeCall = a?.activeCall !== undefined ? a.activeCall : localActiveCall;
  const setActiveCall = a?.setActiveCall || setLocalActiveCall;
  const [lightbox, setLightbox] = useState(null);
  const [newCallModal, setNewCallModal] = useState(false);
  const [groupModal, setGroupModal] = useState(false);
  const [groupTitle, setGroupTitle] = useState('');
  const [groupMembers, setGroupMembers] = useState([]);

  useEffect(() => {
    if (a.activeThreadId) {
      setSelected(a.activeThreadId);
    }
  }, [a.activeThreadId]);

  useEffect(() => {
    const handleCloseMenus = () => {
      setCallContextMenu(null);
      setThreadContextMenu(null);
    };
    window.addEventListener('click', handleCloseMenus);
    return () => window.removeEventListener('click', handleCloseMenus);
  }, []);

  const togglePinThread = (threadId) => {
    setPinnedThreads((old) => {
      const next = { ...old };
      if (next[threadId]) {
        delete next[threadId];
        a.toast('Unpinned conversation');
      } else {
        next[threadId] = Date.now();
        a.toast('Conversation pinned to top');
      }
      return next;
    });
  };

  const clearChatMessages = (threadId) => {
    a.setThreads((old) =>
      old.map((t) => (t.id === threadId ? { ...t, messages: [] } : t))
    );
    a.toast('Chat cleared');
  };

  const clearChat = (threadId) => {
    const target = a.threads.find((t) => t.id === threadId);
    const name = target?.name || 'chat';
    if (window.confirm(`Clear all messages in chat with ${name}?`)) {
      clearChatMessages(threadId);
    }
  };

  const toggleMuteThread = (threadId) => {
    const target = a.threads.find((t) => t.id === threadId);
    const name = target?.name || 'conversation';
    setMutedThreads((old) => {
      const isMuted = old.includes(threadId);
      const next = isMuted ? old.filter((id) => id !== threadId) : [...old, threadId];
      a.toast(isMuted ? `Unmuted notifications for ${name}` : `Muted notifications for ${name}`);
      return next;
    });
  };

  const deleteChat = (threadId) => {
    const target = a.threads.find((t) => t.id === threadId);
    const name = target?.name || 'chat';
    if (window.confirm(`Delete chat with ${name}? All messages and media in this chat will be removed.`)) {
      a.setThreads((old) => old.filter((t) => t.id !== threadId));
      if (selected === threadId) {
        setSelected(null);
        if (a.setActiveThreadId) a.setActiveThreadId(null);
      }
      setArchivedThreads((old) => old.filter((id) => id !== threadId));
      setMutedThreads((old) => old.filter((id) => id !== threadId));
      a.toast(`Deleted chat with ${name}`);
    }
  };

  const toggleArchiveThread = (threadId) => {
    const target = a.threads.find((t) => t.id === threadId);
    const name = target?.name || 'chat';
    setArchivedThreads((old) => {
      const isArchived = old.includes(threadId);
      const next = isArchived ? old.filter((id) => id !== threadId) : [...old, threadId];
      a.toast(isArchived ? `Unarchived chat with ${name}` : `Archived chat with ${name}`);
      return next;
    });
  };

  const toggleBlockUser = (userName) => {
    const isBlocked = blockedUsers.includes(userName);
    if (isBlocked) {
      setBlockedUsers((prev) => prev.filter((u) => u !== userName));
      a.toast(`Unblocked ${userName}`);
    } else {
      if (window.confirm(`Block ${userName}? You will no longer receive calls or messages from them.`)) {
        setBlockedUsers((prev) => [...prev, userName]);
        a.toast(`Blocked ${userName}. You will stop getting messages.`);
      }
    }
  };

  const threads = useMemo(() => {
    const filtered = a.threads.filter((thread) => {
      const text =
        `${thread.name} ${thread.context} ${latest(thread).text}`.toLowerCase();
      const isGroup = thread.context === 'Group' || thread.isGroup;
      const isArchived = archivedThreads.includes(thread.id);

      // If user is not specifically looking at 'Archived' tab, hide archived threads
      if (filter !== 'Archived' && isArchived) {
        return false;
      }

      const group =
        filter === 'All'
          ? true
          : filter === 'Chats'
          ? !isGroup
          : filter === 'Groups'
          ? isGroup
          : filter === 'Unread'
          ? thread.unread
          : filter === 'Requests'
          ? thread.status === 'request'
          : filter === 'Starred'
          ? thread.messages.some((message) => starred.includes(message.id))
          : filter === 'Archived'
          ? isArchived
          : filter === 'Blocked'
          ? blockedUsers.includes(thread.name)
          : true;
      return group && text.includes(query.toLowerCase());
    });

    // Requirement 6: Pinned chats show on top; if multiple are pinned, sort by recent pinned timestamp
    return [...filtered].sort((t1, t2) => {
      const p1 = pinnedThreads[t1.id];
      const p2 = pinnedThreads[t2.id];
      if (p1 && p2) return p2 - p1;
      if (p1) return -1;
      if (p2) return 1;
      return 0;
    });
  }, [a.threads, filter, query, starred, pinnedThreads, archivedThreads, blockedUsers]);

  const active = selected
    ? a.threads.find((thread) => thread.id === selected)
    : threads[0] || a.threads[0];

  const open = (id) => {
    setSelected(id);
    if (a.setActiveThreadId) a.setActiveThreadId(id);
    setInfo(false);
    a.setThreads((old) =>
      old.map((thread) =>
        thread.id === id ? { ...thread, unread: false } : thread
      )
    );
    a.markConversationRead(id);
  };

  const newThread = () => a.setModal({ type: 'composer', format: 'Message' });

  const startCall = (name, type = 'audio', members = null) => {
    if (a?.startCall) {
      a.startCall(name, type, members);
      return;
    }
    const matchedThread = a.threads.find((t) => t.name === name);
    // members===false means explicit 1:1 call (from New Call modal) — never treat as group
    const callMembers = members === false
      ? null
      : members || (matchedThread?.isGroup || matchedThread?.context === 'Group' ? matchedThread.members : null);
    if (a.serviceState === 'connected') {
      a.toast?.('Audio/video calling is disabled in this release.');
      return;
    }
    const person = people.find((p) => p.name === name);
    setActiveCall({
      open: true,
      minimized: false,
      name,
      person,
      type,
      duration: 0,
      muted: false,
      videoOff: false,
      callId: crypto.randomUUID(),
      members: callMembers,
      isGroup: Boolean(callMembers && callMembers.length > 0),
    });
  };

  const endCall = () => {
    if (a?.endCall) {
      a.endCall();
      return;
    }
    if (!activeCall) return;
    const duration = activeCall.duration || 0;
    const isVideo = activeCall.type === 'video';
    const isGroupCall = Boolean(activeCall.isGroup || (activeCall.members && activeCall.members.length > 0));
    const record = [
      activeCall.name,
      `${isGroupCall ? 'Group ' : ''}${isVideo ? 'Video' : 'Outgoing audio'} · ${duration > 0 ? formatDuration(duration) : 'Cancelled'}`,
      'Just now',
      activeCall.type || 'audio',
      activeCall.members || null,
    ];
    setCallLog((old) => [record, ...old]);

    if (a.serviceState === 'connected' && a.auth?.accessToken && activeCall.callId) {
      api.messages
        .endCall(activeCall.callId, duration, 'ended', a.auth.accessToken)
        .catch(() => {});
    }

    a.toast(`Call ended · ${formatDuration(duration)}`);
    setActiveCall(null);
  };

  const createGroupSubmit = async (event) => {
    event?.preventDefault();
    const title = groupTitle.trim();
    if (!title) {
      a.toast('Please provide a group title.');
      return;
    }
    if (groupMembers.length === 0) {
      a.toast('Please select at least one teammate.');
      return;
    }

    let newId = `group-${crypto.randomUUID()}`;
    if (a.serviceState === 'connected' && a.auth?.accessToken) {
      try {
        const remote = await api.messages.createGroup(
          title,
          groupMembers,
          a.auth.accessToken
        );
        if (remote?.id) newId = remote.id;
      } catch {
        a.toast('Created group locally in preview mode.');
      }
    }

    const newGroup = {
      id: newId,
      name: title,
      context: 'Group',
      isGroup: true,
      status: 'connected',
      unread: false,
      members: ['You', ...groupMembers.map(m => (typeof m === 'string' ? m : m.name || m))],
      messages: [
        {
          id: crypto.randomUUID(),
          mine: true,
          text: `Group created: ${title}`,
          timestamp: Date.now(),
          time: 'Just now',
        },
      ],
    };

    a.setThreads((old) => [newGroup, ...old]);
    setGroupModal(false);
    setGroupTitle('');
    setGroupMembers([]);
    open(newId);
    a.toast(`Group "${title}" created.`);
  };

  const toggleStar = async (messageId) => {
    const isStarred = starred.includes(messageId);
    const next = isStarred
      ? starred.filter((id) => id !== messageId)
      : [...starred, messageId];
    setStarred(next);

    if (a.serviceState === 'connected' && a.auth?.accessToken && active?.id) {
      try {
        if (isStarred) {
          await api.messages.unstar(active.id, messageId, a.auth.accessToken);
        } else {
          await api.messages.star(active.id, messageId, a.auth.accessToken);
        }
      } catch {
        /* fallback to local */
      }
    }
  };

  const send = async (event, replyTo, attachments) => {
    event?.preventDefault();
    const text = draft.trim();
    if ((!text && !attachments?.length) || !active) return;

    const localId = crypto.randomUUID();
    const local = {
      id: localId,
      mine: true,
      text,
      timestamp: Date.now(),
      createdAt: new Date().toISOString(),
      time: 'Just now',
      attachments: attachments || [],
      replyTo: replyTo?.text?.slice(0, 96) || null,
      status: 'sent',
    };

    a.setThreads((old) => {
      const target = old.find((thread) => thread.id === active.id);
      if (!target) return old;
      const updated = { ...target, messages: [...target.messages, local] };
      return [updated, ...old.filter((thread) => thread.id !== active.id)];
    });
    setDraft('');

    // Transition from sent (single tick) to delivered (double tick)
    setTimeout(() => {
      a.setThreads((old) =>
        old.map((thread) =>
          thread.id === active.id
            ? {
                ...thread,
                messages: thread.messages.map((m) =>
                  m.id === localId ? { ...m, status: 'delivered' } : m
                ),
              }
            : thread
        )
      );
    }, 1200);

    try {
      const result = await a.sendMessage(active.id, text, attachments || []);
      if (result?.message) {
        a.setThreads((old) => {
          const target = old.find((thread) => thread.id === active.id);
          if (!target) return old;
          const updated = {
            ...target,
            messages: target.messages.map((message) =>
              message.id === localId
                ? { ...result.message, replyTo: local.replyTo, timestamp: local.timestamp, status: 'delivered' }
                : message
            ),
          };
          return [updated, ...old.filter((thread) => thread.id !== active.id)];
        });
      }
      a.notify('Message sent', `Your note is in ${active.name}.`, 'Messages');
      return true;
    } catch {
      return true;
    }
  };

  const header = (
    <Header
      view={view}
      setView={setView}
      onNew={newThread}
      onNewGroup={() => setGroupModal(true)}
    />
  );

  if (view === 'Calls') {
    return (
      <div className="inbox-page" onClick={() => setCallContextMenu(null)}>
        {header}
        <section className="message-utility">
          <div
            className="utility-head"
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: 16,
            }}
          >
            <h2 style={{ margin: 0 }}>Calls</h2>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              {callLog.length > 0 && (
                <Button
                  variant="quiet"
                  icon={Trash2}
                  className="calls-clear-btn"
                  onClick={() => {
                    setCallLog([]);
                    a.toast('Call history cleared.');
                  }}
                  title="Clear all calls"
                >
                  <span className="calls-btn-label">Clear call history</span>
                </Button>
              )}
              <Button
                icon={Phone}
                className="calls-new-btn"
                onClick={() => setNewCallModal(true)}
              >
                <span className="calls-btn-label">New call</span>
              </Button>
            </div>
          </div>

          {callLog.length === 0 ? (
            <Empty
              title="No calls yet"
              text="Audio and video call records will appear here."
            />
          ) : (
            callLog.map(([name, detail, time, type, members], idx) => {
              // Requirement 4 & 6: Match icon by recent type of call made, and show group participant names
              const isVideo = type === 'video' || (typeof detail === 'string' && detail.toLowerCase().includes('video'));
              const CallIcon = isVideo ? Video : Phone;
              const callMembers = members || a.threads.find((t) => t.name === name && (t.isGroup || t.context === 'Group'))?.members;

              return (
                <div
                  key={`${name}-${idx}`}
                  className="call-item-wrapper"
                  onContextMenu={(e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    setCallContextMenu({
                      index: idx,
                      name,
                      isVideo,
                      members: callMembers,
                      x: Math.min(e.clientX, window.innerWidth - 220),
                      y: Math.min(e.clientY, window.innerHeight - 170),
                    });
                  }}
                >
                  <UtilityRow
                    name={name}
                    detail={detail}
                    time={time}
                    icon={CallIcon}
                    members={callMembers}
                    action={
                      <IconButton
                        icon={CallIcon}
                        label={isVideo ? `Video call ${name}` : `Call ${name}`}
                        onClick={() => startCall(name, isVideo ? 'video' : 'audio', callMembers)}
                      />
                    }
                  />
                </div>
              );
            })
          )}
        </section>

        {callContextMenu && (
          <div
            className="context-menu-floating"
            style={{ top: callContextMenu.y, left: callContextMenu.x }}
            onClick={(e) => e.stopPropagation()}
          >
            <button
              type="button"
              className="context-menu-item"
              onClick={() => {
                const { name, members } = callContextMenu;
                setCallContextMenu(null);
                startCall(name, 'audio', members);
              }}
            >
              <Phone size={15} />
              <span>Audio call</span>
            </button>
            <button
              type="button"
              className="context-menu-item"
              onClick={() => {
                const { name, members } = callContextMenu;
                setCallContextMenu(null);
                startCall(name, 'video', members);
              }}
            >
              <Video size={15} />
              <span>Video call</span>
            </button>
            <button
              type="button"
              className="context-menu-item danger"
              onClick={() => {
                const targetIdx = callContextMenu.index;
                setCallLog((old) => old.filter((_, i) => i !== targetIdx));
                setCallContextMenu(null);
                a.toast('Call removed from history');
              }}
            >
              <Trash2 size={15} />
              <span>Clear this call</span>
            </button>
          </div>
        )}

        {newCallModal && (
          <NewCallModal
            onClose={() => setNewCallModal(false)}
            onStartCall={startCall}
          />
        )}
        {activeCall && (
          <CallModal
            call={activeCall}
            setCall={setActiveCall}
            onEnd={endCall}
          />
        )}
      </div>
    );
  }

  return (
    <div className={`inbox-page ${selected ? 'mobile-chat-open' : ''}`}>
      {header}
      <div className={`inbox-layout ${selected ? 'mobile-chat-open' : ''}`}>
        <section className="thread-list">
          <SearchField
            value={query}
            onChange={setQuery}
            placeholder="Search conversations"
          />
          <div className="thread-shortcuts">
            <button className="quick-thread" onClick={newThread}>
              <UserPlus size={16} />
              <span>New message</span>
            </button>
            <button className="quick-thread" onClick={() => setGroupModal(true)}>
              <Users size={16} />
              <span>New group</span>
            </button>
          </div>
          {/* Tabs: All, Chats, Groups, Unread, Starred, Archived, Blocked */}
          <Tabs
            items={['All', 'Chats', 'Groups', 'Unread', 'Starred', 'Archived', 'Blocked']}
            value={filter}
            onChange={setFilter}
          />
          {threads.length ? (
            threads.map((thread) => (
              <ThreadRow
                key={thread.id}
                thread={thread}
                active={active?.id === thread.id}
                isPinned={Boolean(pinnedThreads[thread.id])}
                isMuted={mutedThreads.includes(thread.id)}
                isArchived={archivedThreads.includes(thread.id)}
                isBlocked={blockedUsers.includes(thread.name)}
                onClick={() => open(thread.id)}
                onLongPress={(pos, t) => {
                  setThreadContextMenu({
                    thread: t,
                    x: Math.min(pos.clientX, window.innerWidth - 240),
                    y: Math.min(pos.clientY, window.innerHeight - 340),
                  });
                }}
              />
            ))
          ) : (
            <Empty
              title={
                filter === 'Blocked'
                  ? 'No blocked users'
                  : filter === 'Archived'
                  ? 'No archived chats'
                  : 'No conversations found'
              }
              text={
                filter === 'Blocked'
                  ? 'Blocked users will appear here.'
                  : filter === 'Archived'
                  ? 'Chats you archive will be stored here.'
                  : 'Try another name or start a conversation.'
              }
            />
          )}
        </section>

        {threadContextMenu && (
          <div
            className="thread-context-backdrop"
            style={{
              position: 'fixed',
              inset: 0,
              zIndex: 999,
              background: 'rgba(0, 0, 0, 0.25)',
            }}
            onClick={() => setThreadContextMenu(null)}
          >
            <div
              className="context-menu-floating"
              style={{
                position: 'fixed',
                top: Math.max(16, Math.min(threadContextMenu.y, window.innerHeight - 340)),
                left: Math.max(16, Math.min(threadContextMenu.x, window.innerWidth - 240)),
                zIndex: 1000,
                minWidth: 220,
                padding: '6px',
              }}
              onClick={(e) => e.stopPropagation()}
            >
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  padding: '8px 10px 10px',
                  borderBottom: '1px solid var(--line)',
                  marginBottom: 4,
                }}
              >
                <Avatar
                  person={threadContextMenu.thread.isGroup ? null : people.find((p) => p.name === threadContextMenu.thread.name)}
                  name={threadContextMenu.thread.name}
                  size="sm"
                />
                <div style={{ minWidth: 0, flex: 1 }}>
                  <strong
                    style={{
                      fontSize: 13,
                      display: 'block',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {threadContextMenu.thread.name}
                  </strong>
                  <small style={{ color: 'var(--muted)', fontSize: 11, display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {threadContextMenu.thread.context || 'Conversation'}
                  </small>
                </div>
              </div>

              <button
                type="button"
                className="context-menu-item"
                onClick={() => {
                  const id = threadContextMenu.thread.id;
                  setThreadContextMenu(null);
                  toggleMuteThread(id);
                }}
              >
                {mutedThreads.includes(threadContextMenu.thread.id) ? (
                  <>
                    <Bell size={16} />
                    <span>Unmute notifications</span>
                  </>
                ) : (
                  <>
                    <BellOff size={16} />
                    <span>Mute notifications</span>
                  </>
                )}
              </button>

              <button
                type="button"
                className="context-menu-item"
                onClick={() => {
                  const id = threadContextMenu.thread.id;
                  setThreadContextMenu(null);
                  clearChat(id);
                }}
              >
                <Trash2 size={16} />
                <span>Clear chat</span>
              </button>

              <button
                type="button"
                className="context-menu-item"
                onClick={() => {
                  const id = threadContextMenu.thread.id;
                  setThreadContextMenu(null);
                  toggleArchiveThread(id);
                }}
              >
                <Archive size={16} />
                <span>
                  {archivedThreads.includes(threadContextMenu.thread.id)
                    ? 'Unarchive chat'
                    : 'Archive chat'}
                </span>
              </button>

              <button
                type="button"
                className="context-menu-item danger"
                onClick={() => {
                  const id = threadContextMenu.thread.id;
                  setThreadContextMenu(null);
                  deleteChat(id);
                }}
              >
                <Trash2 size={16} />
                <span>Delete chat</span>
              </button>

              {!threadContextMenu.thread.isGroup && threadContextMenu.thread.context !== 'Group' && (
                <button
                  type="button"
                  className="context-menu-item danger"
                  onClick={() => {
                    const name = threadContextMenu.thread.name;
                    setThreadContextMenu(null);
                    toggleBlockUser(name);
                  }}
                >
                  <ShieldAlert size={16} />
                  <span>
                    {blockedUsers.includes(threadContextMenu.thread.name)
                      ? `Unblock ${threadContextMenu.thread.name}`
                      : `Block ${threadContextMenu.thread.name}`}
                  </span>
                </button>
              )}
            </div>
          </div>
        )}

        <Conversation
          active={active}
          draft={draft}
          setDraft={setDraft}
          send={send}
          info={info}
          setInfo={setInfo}
          starred={starred}
          toggleStar={toggleStar}
          onBack={() => setSelected(null)}
          onStartCall={startCall}
          disappearing={disappearing}
          setDisappearing={setDisappearing}
          isPinned={Boolean(pinnedThreads[active?.id])}
          onTogglePin={() => active?.id && togglePinThread(active.id)}
          isMuted={Boolean(active?.id && mutedThreads.includes(active.id))}
          onToggleMute={toggleMuteThread}
          isArchived={Boolean(active?.id && archivedThreads.includes(active.id))}
          onToggleArchive={toggleArchiveThread}
          onClearChat={clearChat}
          onDeleteChat={deleteChat}
          blockedUsers={blockedUsers}
          setBlockedUsers={setBlockedUsers}
          isBlocked={Boolean(active?.name && blockedUsers.includes(active.name))}
          onToggleBlock={toggleBlockUser}
          onPreviewImage={(src) =>
            setLightbox({ src, title: 'Message attachment' })
          }
        />
      </div>

      {lightbox && (
        <LightboxModal item={lightbox} onClose={() => setLightbox(null)} />
      )}

      {groupModal && (
        <div className="call-modal-overlay" onClick={() => setGroupModal(false)}>
          <div
            className="call-modal"
            style={{
              textAlign: 'left',
              alignItems: 'stretch',
              background: 'var(--surface)',
              color: 'var(--ink)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <h3 style={{ margin: 0, fontSize: 18 }}>Create new group</h3>
              <button
                style={{
                  background: 'none',
                  border: 0,
                  fontSize: 20,
                  cursor: 'pointer',
                }}
                onClick={() => setGroupModal(false)}
              >
                ×
              </button>
            </div>
            <form
              onSubmit={createGroupSubmit}
              style={{ display: 'grid', gap: 12, marginTop: 8 }}
            >
              <label style={{ fontSize: 12, fontWeight: 700 }}>
                Group name
                <input
                  style={{
                    width: '100%',
                    padding: 8,
                    borderRadius: 6,
                    border: '1px solid var(--line)',
                    background: 'var(--canvas)',
                    marginTop: 4,
                  }}
                  value={groupTitle}
                  onChange={(e) => setGroupTitle(e.target.value)}
                  placeholder="e.g. Design Systems Working Group"
                />
              </label>
              <label style={{ fontSize: 12, fontWeight: 700 }}>
                Select members
                <div
                  style={{
                    display: 'grid',
                    gap: 6,
                    maxHeight: 180,
                    overflowY: 'auto',
                    marginTop: 6,
                  }}
                >
                  {people.map((person) => {
                    const checked = groupMembers.includes(person.id);
                    return (
                      <div
                        key={person.id}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: 8,
                          padding: 6,
                          borderRadius: 6,
                          background: checked ? 'var(--soft)' : 'transparent',
                          cursor: 'pointer',
                        }}
                        onClick={() => {
                          setGroupMembers((old) =>
                            old.includes(person.id)
                              ? old.filter((id) => id !== person.id)
                              : [...old, person.id]
                          );
                        }}
                      >
                        <input type="checkbox" checked={checked} readOnly />
                        <Avatar person={person} name={person.name} />
                        <div>
                          <strong style={{ fontSize: 13, display: 'block' }}>
                            {person.name}
                          </strong>
                          <small style={{ color: 'var(--muted)' }}>
                            {person.role}
                          </small>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </label>
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'flex-end',
                  gap: 8,
                  marginTop: 8,
                }}
              >
                <Button
                  type="button"
                  variant="quiet"
                  onClick={() => setGroupModal(false)}
                >
                  Cancel
                </Button>
                <Button type="submit" variant="primary">
                  Create group
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

// ─── 3. Thread List Item ─────────────────────────────────────────────────────

function ThreadRow({
  thread,
  active,
  onClick,
  onLongPress,
  isPinned,
  isMuted,
  isArchived,
  isBlocked,
}) {
  const item = latest(thread);
  const a = useApp();
  const isGroup = thread.context === 'Group' || thread.isGroup;

  const timerRef = useRef(null);
  const longPressedRef = useRef(false);
  const startPosRef = useRef({ x: 0, y: 0 });

  const startPress = (e) => {
    longPressedRef.current = false;
    const clientX = e.clientX ?? e.touches?.[0]?.clientX ?? 0;
    const clientY = e.clientY ?? e.touches?.[0]?.clientY ?? 0;
    startPosRef.current = { x: clientX, y: clientY };

    timerRef.current = setTimeout(() => {
      longPressedRef.current = true;
      if (typeof navigator !== 'undefined' && navigator.vibrate) {
        try { navigator.vibrate(40); } catch (_) {}
      }
      onLongPress?.({ clientX, clientY }, thread);
    }, 480);
  };

  const cancelPress = () => {
    if (timerRef.current) {
      clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  };

  const handlePointerMove = (e) => {
    const clientX = e.clientX ?? e.touches?.[0]?.clientX ?? 0;
    const clientY = e.clientY ?? e.touches?.[0]?.clientY ?? 0;
    const diffX = Math.abs(clientX - startPosRef.current.x);
    const diffY = Math.abs(clientY - startPosRef.current.y);
    if (diffX > 10 || diffY > 10) {
      cancelPress();
    }
  };

  const handleClick = (e) => {
    if (longPressedRef.current) {
      e.preventDefault();
      e.stopPropagation();
      longPressedRef.current = false;
      return;
    }
    onClick?.(e);
  };

  const handleContextMenu = (e) => {
    e.preventDefault();
    e.stopPropagation();
    cancelPress();
    onLongPress?.({ clientX: e.clientX, clientY: e.clientY }, thread);
  };

  return (
    <button
      className={`thread-item ${active ? 'selected' : ''}`}
      onPointerDown={startPress}
      onPointerUp={cancelPress}
      onPointerCancel={cancelPress}
      onPointerMove={handlePointerMove}
      onContextMenu={handleContextMenu}
      onClick={handleClick}
      style={{ touchAction: 'pan-y', WebkitTouchCallout: 'none', userSelect: 'none' }}
    >
      <span
        className="thread-avatar-wrap"
        onClick={(e) => {
          // Requirement 2: Open user profile on click; do NOT open anything if group profile
          if (!isGroup && a?.openUserProfile) {
            e.stopPropagation();
            a.openUserProfile(thread.name, 'Posts');
          }
        }}
        style={{ cursor: isGroup ? 'default' : 'pointer', display: 'inline-flex', flex: '0 0 auto' }}
        title={!isGroup ? `Open ${thread.name}'s profile` : undefined}
      >
        <Avatar
          person={isGroup ? null : people.find((person) => person.name === thread.name)}
          name={thread.name}
        />
      </span>
      <span style={{ minWidth: 0, flex: 1 }}>
        <strong style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {thread.name}
          </span>
          {isPinned && (
            <Pin
              size={13}
              style={{
                color: 'var(--coral, #e34d42)',
                fill: 'currentColor',
                transform: 'rotate(45deg)',
                flexShrink: 0,
              }}
              title="Pinned on top"
            />
          )}
          {isMuted && (
            <BellOff
              size={13}
              style={{
                color: 'var(--muted)',
                flexShrink: 0,
              }}
              title="Notifications muted"
            />
          )}
          {isArchived && (
            <Archive
              size={12}
              style={{
                color: 'var(--muted)',
                flexShrink: 0,
              }}
              title="Archived chat"
            />
          )}
          {isBlocked && (
            <ShieldAlert
              size={12}
              style={{
                color: '#dc2626',
                flexShrink: 0,
              }}
              title="Blocked user"
            />
          )}
        </strong>
        <small className={thread.typingName ? 'typing-copy' : ''}>
          {thread.typingName ? `${thread.typingName} is typing…` : item.text}
        </small>
      </span>
      <em>{thread.typingName ? 'Now' : item.time}</em>
      {thread.unread && <i>1</i>}
    </button>
  );
}

// ─── 4. Conversation Pane ────────────────────────────────────────────────────

function Conversation({
  active,
  draft,
  setDraft,
  send,
  info,
  setInfo,
  starred,
  toggleStar,
  onBack,
  onStartCall,
  onPreviewImage,
  disappearing,
  setDisappearing,
  isPinned,
  onTogglePin,
  isMuted,
  onToggleMute,
  isArchived,
  onToggleArchive,
  onClearChat,
  onDeleteChat,
  blockedUsers = [],
  setBlockedUsers,
  isBlocked,
  onToggleBlock,
}) {
  const a = useApp();
  const [replyTo, setReplyTo] = useState(null);
  const [editingMessage, setEditingMessage] = useState(null);
  const [showFavouritesModal, setShowFavouritesModal] = useState(false);
  const [showManageMembersModal, setShowManageMembersModal] = useState(false);
  const [reactions, setReactions] = useState({});
  const [reactionMenu, setReactionMenu] = useState(null);
  const [attachment, setAttachment] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [recording, setRecording] = useState(false);
  const [recordTime, setRecordTime] = useState(0);
  const [chatMenuOpen, setChatMenuOpen] = useState(false);
  const [inChatSearchOpen, setInChatSearchOpen] = useState(false);
  const [inChatSearchQuery, setInChatSearchQuery] = useState('');
  const [favouriteChats, setFavouriteChats] = useState([]);
  const [contextMenu, setContextMenu] = useState(null); // { message, x, y }
  const [showCustomReactionPicker, setShowCustomReactionPicker] = useState(false);
  const [forwardModal, setForwardModal] = useState(null); // message
  const fileInput = useRef();
  const holdTimer = useRef();
  const messagesEndRef = useRef(null);

  useEffect(() => {
    if (active?.id) {
      messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }
  }, [active?.id, active?.messages?.length]);

  useEffect(() => {
    if (!recording) {
      setRecordTime(0);
      return;
    }
    const timer = setInterval(() => setRecordTime((t) => t + 1), 1000);
    return () => clearInterval(timer);
  }, [recording]);

  useEffect(() => {
    const closeAll = () => {
      setContextMenu(null);
      setShowCustomReactionPicker(false);
      setChatMenuOpen(false);
    };
    window.addEventListener('click', closeAll);
    return () => window.removeEventListener('click', closeAll);
  }, []);

  if (!active) {
    return (
      <section className="conversation">
        <Empty
          title="Choose a conversation"
          text="Select a thread to keep collaborating."
        />
      </section>
    );
  }

  const attach = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    setUploading(true);
    try {
      const result = await a.uploadFile(file);
      setAttachment(result.attachment);
      a.toast(
        result.mode === 'synced'
          ? 'File verified and ready to send'
          : `${file.name} attached to this preview`
      );
    } catch (error) {
      a.toast(error.message || 'Could not prepare that file.');
    } finally {
      setUploading(false);
    }
  };

  const clearHold = () => {
    if (holdTimer.current) {
      clearTimeout(holdTimer.current);
      holdTimer.current = null;
    }
  };

  const startHold = (id) => {
    clearHold();
    holdTimer.current = setTimeout(() => {
      setReactionMenu(id);
      holdTimer.current = null;
    }, 420);
  };

  useEffect(() => {
    if (!reactionMenu) return;
    const handleOutsideClick = (e) => {
      if (!e.target.closest('.reaction-menu') && !e.target.closest('.reaction-trigger-btn')) {
        setReactionMenu(null);
      }
    };
    document.addEventListener('pointerdown', handleOutsideClick);
    return () => document.removeEventListener('pointerdown', handleOutsideClick);
  }, [reactionMenu]);

  const react = async (messageId, emoji) => {
    const isDeselecting = reactions[messageId] === emoji;
    setReactions((old) => {
      const next = { ...old };
      if (isDeselecting) {
        delete next[messageId];
      } else {
        next[messageId] = emoji;
      }
      return next;
    });
    setReactionMenu(null);
    if (a.serviceState === 'connected' && a.auth?.accessToken) {
      try {
        if (isDeselecting) {
          await api.messages.removeReaction(active.id, messageId, a.auth.accessToken);
        } else {
          await api.messages.react(active.id, messageId, emoji, a.auth.accessToken);
        }
      } catch {
        /* fallback locally */
      }
    }
  };

  const deleteMessage = async (messageId) => {
    a.setThreads((old) =>
      old.map((t) =>
        t.id === active.id
          ? { ...t, messages: t.messages.filter((m) => m.id !== messageId) }
          : t
      )
    );
    if (a.serviceState === 'connected' && a.auth?.accessToken) {
      api.messages.deleteMessage(active.id, messageId, a.auth.accessToken).catch(() => {});
    }
    a.toast('Message deleted');
  };

  const togglePin = (messageId) => {
    a.setThreads((old) =>
      old.map((t) => {
        if (t.id !== active.id) return t;
        const currentPins = t.pinnedMessageIds || [];
        const isPinned = currentPins.includes(messageId);
        const updatedPins = isPinned
          ? currentPins.filter((id) => id !== messageId)
          : [...currentPins, messageId];
        return { ...t, pinnedMessageIds: updatedPins };
      })
    );
    a.toast('Updated pinned messages');
  };

  const submit = async (event) => {
    event?.preventDefault();
    if (editingMessage) {
      const newText = draft.trim();
      if (newText) {
        a.setThreads((old) =>
          old.map((t) =>
            t.id === active.id
              ? {
                  ...t,
                  messages: t.messages.map((m) =>
                    m.id === editingMessage.id
                      ? { ...m, text: newText, edited: true, editedAt: Date.now() }
                      : m
                  ),
                }
              : t
          )
        );
        a.toast('Message edited');
      }
      setEditingMessage(null);
      setDraft('');
      return;
    }
    const sent = await send(event, replyTo, attachment ? [attachment] : []);
    if (sent) {
      setAttachment(null);
      setReplyTo(null);
    }
  };

  const sendVoiceNote = async () => {
    if (recordTime < 1) {
      setRecording(false);
      return;
    }
    const voiceNote = {
      objectId: `audio-${crypto.randomUUID()}`,
      filename: `Voice note (${formatDuration(recordTime)}).m4a`,
      expectedBytes: recordTime * 16000,
      contentType: 'audio/m4a',
    };
    setRecording(false);
    await send({ preventDefault: () => {} }, replyTo, [voiceNote]);
  };

  const displayedMessages = useMemo(() => {
    if (!active?.messages) return [];
    if (!inChatSearchQuery.trim()) return active.messages;
    const q = inChatSearchQuery.toLowerCase();
    return active.messages.filter((m) => (m.text || '').toLowerCase().includes(q));
  }, [active?.messages, inChatSearchQuery]);

  const isGroup = active.context === 'Group' || active.isGroup;

  return (
    <section className="conversation">
      <header className="conversation-head">
        <button
          className="mobile-conversation-back"
          aria-label="Back to conversations"
          onClick={onBack}
        >
          <ChevronLeft size={20} />
        </button>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          {/* Requirement 2: Open profile of user; do NOT open anything if it was a group profile */}
          <div
            onClick={(e) => {
              e.stopPropagation();
              if (!isGroup && a.openUserProfile) {
                a.openUserProfile(active.name, 'Posts');
              }
            }}
            style={{ cursor: isGroup ? 'default' : 'pointer', flex: '0 0 auto', display: 'inline-flex' }}
            title={!isGroup ? `Click to open ${active.name}'s profile` : active.name}
          >
            <Avatar
              person={isGroup ? null : people.find((person) => person.name === active.name)}
              name={active.name}
            />
          </div>
          <div
            onClick={(e) => {
              if (!isGroup && a.openUserProfile) {
                a.openUserProfile(active.name, 'Posts');
              }
            }}
            style={{ cursor: isGroup ? 'default' : 'pointer', display: 'flex', flexDirection: 'column' }}
            title={!isGroup ? `Click to open ${active.name}'s profile` : active.name}
          >
            <strong>{active.name}</strong>
            <small>
              {isGroup
                ? `Group · ${(active.members || []).length || 3} members`
                : active.context}
            </small>
          </div>
        </div>

        {/* Requirement 3: 3 dots on right top for chat options & Info button */}
        <div className="conversation-actions" style={{ position: 'relative', display: 'flex', alignItems: 'center', gap: 4 }}>
          <IconButton
            icon={Phone}
            label="Start audio call"
            className="chat-head-call-btn"
            onClick={() => onStartCall(active.name, 'audio', isGroup ? active.members : null)}
          />
          <IconButton
            icon={Video}
            label="Start video call"
            className="chat-head-video-btn"
            onClick={() => onStartCall(active.name, 'video', isGroup ? active.members : null)}
          />
          <IconButton
            icon={MoreHorizontal}
            label="Chat options"
            className="chat-head-menu-btn"
            onClick={(e) => {
              e.stopPropagation();
              setChatMenuOpen((prev) => !prev);
            }}
          />

          {chatMenuOpen && (
            <div className="dropdown-menu-popover" onClick={(e) => e.stopPropagation()}>
              <button
                type="button"
                className="dropdown-menu-item"
                onClick={() => {
                  setChatMenuOpen(false);
                  setInChatSearchOpen(true);
                }}
              >
                <Search size={16} />
                <span>Search in chat</span>
              </button>
              <button
                type="button"
                className="dropdown-menu-item"
                onClick={() => {
                  setChatMenuOpen(false);
                  onTogglePin?.();
                }}
              >
                <Pin size={16} style={{ color: isPinned ? 'var(--coral)' : 'inherit' }} />
                <span>{isPinned ? 'Unpin chat' : 'Pin on top'}</span>
              </button>
              <button
                type="button"
                className="dropdown-menu-item"
                onClick={() => {
                  setChatMenuOpen(false);
                  const isFav = favouriteChats.includes(active.id);
                  setFavouriteChats(isFav ? favouriteChats.filter(id => id !== active.id) : [...favouriteChats, active.id]);
                  a.toast(isFav ? 'Removed from favourites' : 'Added to favourites');
                }}
              >
                <Star
                  size={16}
                  fill={favouriteChats.includes(active.id) ? 'currentColor' : 'none'}
                  style={{ color: favouriteChats.includes(active.id) ? '#eab308' : 'inherit' }}
                />
                <span>{favouriteChats.includes(active.id) ? 'Remove from favourites' : 'Add to favourites'}</span>
              </button>
              <button
                type="button"
                className="dropdown-menu-item"
                onClick={() => {
                  setChatMenuOpen(false);
                  setShowFavouritesModal(true);
                }}
              >
                <Bookmark size={16} />
                <span>Favourite messages</span>
              </button>
              {isGroup && (
                <button
                  type="button"
                  className="dropdown-menu-item"
                  onClick={() => {
                    setChatMenuOpen(false);
                    setShowManageMembersModal(true);
                  }}
                >
                  <Users size={16} />
                  <span>Manage members</span>
                </button>
              )}
              <button
                type="button"
                className="dropdown-menu-item"
                onClick={() => {
                  setChatMenuOpen(false);
                  setInfo(true);
                }}
              >
                <User size={16} />
                <span>Chat profile & info</span>
              </button>
              <button
                type="button"
                className="dropdown-menu-item"
                onClick={() => {
                  setChatMenuOpen(false);
                  onToggleMute?.(active.id);
                }}
              >
                {isMuted ? <Bell size={16} /> : <BellOff size={16} />}
                <span>{isMuted ? 'Unmute notifications' : 'Mute notifications'}</span>
              </button>
              <button
                type="button"
                className="dropdown-menu-item"
                onClick={() => {
                  setChatMenuOpen(false);
                  onToggleArchive?.(active.id);
                }}
              >
                <Archive size={16} />
                <span>{isArchived ? 'Unarchive chat' : 'Archive chat'}</span>
              </button>
              <button
                type="button"
                className="dropdown-menu-item"
                onClick={() => {
                  setChatMenuOpen(false);
                  onClearChat?.(active.id);
                }}
              >
                <Trash2 size={16} />
                <span>Clear chat</span>
              </button>
              <button
                type="button"
                className="dropdown-menu-item danger"
                onClick={() => {
                  setChatMenuOpen(false);
                  onDeleteChat?.(active.id);
                }}
              >
                <Trash2 size={16} />
                <span>Delete chat</span>
              </button>
              {!isGroup && (
                <button
                  type="button"
                  className="dropdown-menu-item danger"
                  onClick={() => {
                    setChatMenuOpen(false);
                    if (onToggleBlock) {
                      onToggleBlock(active.name);
                    } else if (blockedUsers.includes(active.name)) {
                      setBlockedUsers?.((prev) => prev.filter((u) => u !== active.name));
                      a.toast(`Unblocked ${active.name}`);
                    } else {
                      setBlockedUsers?.((prev) => [...prev, active.name]);
                      a.toast(`Blocked ${active.name}. You will stop getting messages.`);
                    }
                  }}
                >
                  <ShieldAlert size={16} />
                  <span>{isBlocked || blockedUsers.includes(active.name) ? `Unblock ${active.name}` : `Block ${active.name}`}</span>
                </button>
              )}
            </div>
          )}
        </div>
      </header>

      {inChatSearchOpen && (
        <div className="in-chat-search-bar">
          <Search size={16} style={{ color: 'var(--muted)', flexShrink: 0 }} />
          <input
            autoFocus
            value={inChatSearchQuery}
            onChange={(e) => setInChatSearchQuery(e.target.value)}
            placeholder="Search messages in this chat..."
          />
          <button
            type="button"
            onClick={() => {
              setInChatSearchOpen(false);
              setInChatSearchQuery('');
            }}
            title="Close search"
          >
            <X size={16} />
          </button>
        </div>
      )}

      {active && ((active.pinnedMessageIds && active.pinnedMessageIds.length > 0) || (active.context === 'Group' && !active.pinnedMessageIds)) && (
        <div className="pinned-message" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 14px', background: 'var(--soft)', borderBottom: '1px solid var(--line)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, overflow: 'hidden' }}>
            <Pin size={15} style={{ color: 'var(--accent, #e34d42)', flexShrink: 0 }} />
            <span style={{ fontSize: 12, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              <strong>Pinned</strong> ·{' '}
              {active.pinnedMessageIds?.length
                ? (active.messages.find(m => m.id === active.pinnedMessageIds.at(-1))?.text || 'Pinned item')
                : 'Team sprint sync notes are attached below.'}
            </span>
          </div>
          {active.pinnedMessageIds?.length > 0 && (
            <button
              type="button"
              style={{ background: 'none', border: 0, cursor: 'pointer', padding: 4, color: 'var(--muted)' }}
              onClick={() => togglePin(active.pinnedMessageIds.at(-1))}
              title="Unpin message"
            >
              <X size={14} />
            </button>
          )}
        </div>
      )}

      <div className="messages" aria-live="polite">
        {displayedMessages.map((message) => {
          const isForwarded = Boolean(
            message.forwarded ||
            (typeof message.text === 'string' && message.text.startsWith('[Forwarded]: '))
          );
          const cleanText = isForwarded && typeof message.text === 'string' && message.text.startsWith('[Forwarded]: ')
            ? message.text.replace(/^\[Forwarded\]:\s*/i, '')
            : message.text;

          // Requirement 7: Single tick for sent, double tick for delivered
          const isSent = message.status === 'sent';

          return (
          <div
            className={`bubble ${message.mine ? 'outgoing' : ''}`}
            key={message.id}
            onContextMenu={(e) => {
              // Right click on a message shows options (reaction, reply, forward, star, pin, delete)
              // Requirement 1: On sent messages, shift popup left so reaction + icon isn't clipped
              e.preventDefault();
              e.stopPropagation();
              const isMine = message.mine;
              const menuWidth = 270;
              const targetX = isMine
                ? Math.max(16, Math.min(e.clientX - 100, window.innerWidth - menuWidth - 20))
                : Math.min(e.clientX, window.innerWidth - menuWidth - 20);
              const targetY = Math.min(e.clientY, window.innerHeight - 340);
              setContextMenu({
                message: { ...message, text: cleanText },
                x: targetX,
                y: targetY,
              });
            }}
          >
            {/* Requirement 3: Forwarded header at the top of message with low font size as same as just now (10px) */}
            {isForwarded && (
              <div
                className="forwarded-header"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 4,
                  fontSize: 10,
                  fontStyle: 'italic',
                  opacity: 0.8,
                  marginBottom: 3,
                  color: message.mine ? 'rgba(255, 255, 255, 0.85)' : 'var(--muted)',
                }}
              >
                <Forward size={11} />
                <span>Forwarded</span>
              </div>
            )}
            {message.replyTo && (
              <div className="reply-preview">
                <Reply size={12} />
                {message.replyTo}
              </div>
            )}
            {cleanText && <p>{cleanText}</p>}
            {message.attachments?.map((item) => {
              const isAudio =
                item.contentType?.startsWith('audio/') ||
                item.filename?.match(/\.(m4a|mp3|wav|ogg|aac|webm)$/i) ||
                item.filename?.toLowerCase().includes('voice note');

              if (isAudio) {
                return <VoiceMessagePlayer key={item.objectId || item.filename} item={item} />;
              }

              return (
                <div
                  className="message-attachment"
                  key={item.objectId}
                  onClick={() => {
                    if (
                      item.contentType?.startsWith('image/') ||
                      item.filename?.match(/\.(jpg|jpeg|png|webp|gif)$/i)
                    ) {
                      onPreviewImage(item.downloadUrl || photos.team);
                    }
                  }}
                >
                  <Paperclip size={14} />
                  <span>{item.filename}</span>
                  <small>{formatBytes(item.expectedBytes)}</small>
                </div>
              );
            })}
            {reactions[message.id] && (
              <button
                type="button"
                className="reaction-badge"
                title="Click to remove reaction"
                onClick={() => react(message.id, reactions[message.id])}
              >
                {reactions[message.id]}
              </button>
            )}
            {/* Requirement 1: Removed option buttons under message as they were added to right click */}
            {/* Requirement 7: Show difference between message sent (single tick) and delivered (double tick) */}
            <div className="bubble-meta">
              <small title={message.time || 'Sent recently'}>
                {formatTimeSinceSent(message)}
              </small>
              {message.edited && (
                <span style={{ fontSize: 10, fontStyle: 'italic', opacity: 0.7, marginLeft: 4 }}>
                  (edited)
                </span>
              )}
              {message.mine && (
                <span
                  className="message-status-ticks"
                  title={isSent ? 'Sent' : 'Delivered'}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    marginLeft: 4,
                    opacity: 0.9,
                  }}
                >
                  {isSent ? (
                    <Check size={12} strokeWidth={2.5} />
                  ) : (
                    <CheckCheck size={13} strokeWidth={2.5} />
                  )}
                </span>
              )}
            </div>
          </div>
          );
        })}
        <div ref={messagesEndRef} style={{ height: 1 }} />
      </div>

      {/* Requirement 4: Message right-click floating context menu */}
      {contextMenu && (
        <div
          className="context-menu-floating"
          style={{ top: contextMenu.y, left: contextMenu.x }}
          onClick={(e) => e.stopPropagation()}
        >
          <div className="context-menu-reactions" style={{ position: 'relative' }}>
            {['❤️', '👍', '😂', '😮', '😢', '🙏'].map((emoji) => (
              <button
                type="button"
                key={emoji}
                onClick={() => {
                  react(contextMenu.message.id, emoji);
                  setShowCustomReactionPicker(false);
                  setContextMenu(null);
                }}
              >
                {emoji}
              </button>
            ))}
            <button
              type="button"
              className="custom-reaction-plus-btn"
              title="Choose custom reaction"
              onClick={(e) => {
                e.stopPropagation();
                setShowCustomReactionPicker((prev) => !prev);
              }}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: 26,
                height: 26,
                borderRadius: '50%',
                background: 'var(--soft, #eef5f2)',
                border: '1px solid var(--line, #dde6e5)',
                color: 'var(--ink)',
                cursor: 'pointer',
                padding: 0,
              }}
            >
              <Plus size={14} />
            </button>
          </div>

          {showCustomReactionPicker && (
            <div
              className="custom-reaction-picker-popover"
              onClick={(e) => e.stopPropagation()}
              style={{
                background: 'var(--surface, #fff)',
                border: '1px solid var(--line, #dde6e5)',
                borderRadius: 8,
                padding: '8px 10px',
                margin: '6px 0',
                boxShadow: 'var(--shadow, 0 8px 24px rgba(0,0,0,0.15))',
                display: 'grid',
                gridTemplateColumns: 'repeat(6, 1fr)',
                gap: 6,
                maxWidth: 220,
              }}
            >
              {['🔥', '🎉', '👏', '🚀', '💯', '✨', '👀', '💡', '🤝', '🎯', '🙌', '😍', '🤩', '🥳', '💪', '⚡', '🧠', '🎈'].map((em) => (
                <button
                  key={em}
                  type="button"
                  style={{
                    background: 'none',
                    border: 0,
                    fontSize: 18,
                    cursor: 'pointer',
                    padding: 3,
                    borderRadius: 4,
                  }}
                  onClick={() => {
                    react(contextMenu.message.id, em);
                    setShowCustomReactionPicker(false);
                    setContextMenu(null);
                  }}
                >
                  {em}
                </button>
              ))}
            </div>
          )}

          {contextMenu.message.mine && (
            <button
              type="button"
              className="context-menu-item"
              onClick={() => {
                setEditingMessage(contextMenu.message);
                setDraft(contextMenu.message.text || '');
                setShowCustomReactionPicker(false);
                setContextMenu(null);
              }}
            >
              <Edit3 size={15} />
              <span>Edit</span>
            </button>
          )}
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              const textToCopy = contextMenu.message.text || '';
              if (textToCopy) {
                navigator.clipboard?.writeText?.(textToCopy);
                a.toast('Message copied to clipboard');
              } else {
                a.toast('No message text to copy');
              }
              setShowCustomReactionPicker(false);
              setContextMenu(null);
            }}
          >
            <Copy size={15} />
            <span>Copy</span>
          </button>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              setReplyTo(contextMenu.message);
              setShowCustomReactionPicker(false);
              setContextMenu(null);
            }}
          >
            <Reply size={15} />
            <span>Reply</span>
          </button>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              setForwardModal(contextMenu.message);
              setShowCustomReactionPicker(false);
              setContextMenu(null);
            }}
          >
            <Forward size={15} />
            <span>Forward</span>
          </button>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              toggleStar(contextMenu.message.id);
              setContextMenu(null);
            }}
          >
            <Star
              size={15}
              fill={starred.includes(contextMenu.message.id) ? 'currentColor' : 'none'}
              style={{ color: starred.includes(contextMenu.message.id) ? '#eab308' : 'inherit' }}
            />
            <span>{starred.includes(contextMenu.message.id) ? 'Unstar' : 'Star'}</span>
          </button>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              togglePin(contextMenu.message.id);
              setContextMenu(null);
            }}
          >
            <Pin size={15} />
            <span>{(active.pinnedMessageIds || []).includes(contextMenu.message.id) ? 'Unpin message' : 'Pin message'}</span>
          </button>
          <button
            type="button"
            className="context-menu-item danger"
            onClick={() => {
              deleteMessage(contextMenu.message.id);
              setContextMenu(null);
            }}
          >
            <Trash2 size={15} />
            <span>Delete</span>
          </button>
        </div>
      )}

      {/* Forward message dialog */}
      {forwardModal && (
        <Dialog
          title="Forward message"
          onClose={() => setForwardModal(null)}
        >
          <div style={{ padding: '4px 0' }}>
            <div style={{ background: 'var(--soft)', padding: 10, borderRadius: 6, marginBottom: 14, fontSize: 13, borderLeft: '3px solid var(--coral)' }}>
              <em>"{forwardModal.text || 'Attachment'}"</em>
            </div>
            <p style={{ margin: '0 0 8px', fontSize: 12, fontWeight: 700, color: 'var(--muted)' }}>
              Choose a conversation to forward to:
            </p>
            <div style={{ display: 'grid', gap: 6, maxHeight: 220, overflowY: 'auto' }}>
              {a.threads.filter(t => t.id !== active.id).map((t) => (
                <button
                  key={t.id}
                  type="button"
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                    padding: 8,
                    borderRadius: 6,
                    border: '1px solid var(--line)',
                    background: 'var(--surface)',
                    cursor: 'pointer',
                    textAlign: 'left',
                  }}
                  onClick={() => {
                    const cleanText = forwardModal.text
                      ? forwardModal.text.replace(/^\[Forwarded\]:\s*/i, '')
                      : '';
                    const newMsg = {
                      id: crypto.randomUUID(),
                      mine: true,
                      forwarded: true,
                      text: cleanText,
                      timestamp: Date.now(),
                      time: 'Just now',
                      status: 'sent',
                      attachments: forwardModal.attachments || [],
                    };
                    a.setThreads((old) =>
                      old.map((thread) =>
                        thread.id === t.id
                          ? { ...thread, messages: [...thread.messages, newMsg] }
                          : thread
                      )
                    );
                    setTimeout(() => {
                      a.setThreads((old) =>
                        old.map((thread) =>
                          thread.id === t.id
                            ? {
                                ...thread,
                                messages: thread.messages.map((m) =>
                                  m.id === newMsg.id ? { ...m, status: 'delivered' } : m
                                ),
                              }
                            : thread
                        )
                      );
                    }, 1200);
                    a.toast(`Forwarded to ${t.name}`);
                    setForwardModal(null);
                  }}
                >
                  <Avatar person={t.isGroup || t.context === 'Group' ? null : people.find(p => p.name === t.name)} name={t.name} />
                  <div>
                    <strong style={{ fontSize: 13, display: 'block' }}>{t.name}</strong>
                    <small style={{ color: 'var(--muted)' }}>{t.context}</small>
                  </div>
                </button>
              ))}
            </div>
          </div>
        </Dialog>
      )}

      {/* Favourite Messages Modal */}
      {showFavouritesModal && (
        <FavouriteMessagesModal
          active={active}
          starred={starred}
          toggleStar={toggleStar}
          onClose={() => setShowFavouritesModal(false)}
        />
      )}

      {/* Manage Group Members Modal */}
      {showManageMembersModal && isGroup && (
        <ManageGroupMembersModal
          active={active}
          app={a}
          onClose={() => setShowManageMembersModal(false)}
        />
      )}

      {replyTo && (
        <div className="replying-to">
          <Reply size={14} />
          <span>
            Replying to {active.name}: {replyTo.text.slice(0, 54)}
          </span>
          <button
            type="button"
            onClick={() => setReplyTo(null)}
            aria-label="Cancel reply"
          >
            ×
          </button>
        </div>
      )}

      {attachment && (
        <div className="pending-attachment">
          <Paperclip size={14} />
          <span>{attachment.filename}</span>
          <small>{formatBytes(attachment.expectedBytes)}</small>
          <button
            type="button"
            aria-label="Remove attachment"
            onClick={() => setAttachment(null)}
          >
            ×
          </button>
        </div>
      )}

      {editingMessage && (
        <div
          className="editing-message-banner"
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '7px 14px',
            background: 'var(--soft)',
            borderTop: '1px solid var(--line)',
            fontSize: 12,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--coral)' }}>
            <Edit3 size={14} />
            <span>Editing message: <strong>"{editingMessage.text?.slice(0, 48)}"</strong></span>
          </div>
          <button
            type="button"
            onClick={() => {
              setEditingMessage(null);
              setDraft('');
            }}
            style={{ background: 'none', border: 0, cursor: 'pointer', color: 'var(--muted)' }}
            title="Cancel edit"
          >
            <X size={14} />
          </button>
        </div>
      )}

      {blockedUsers.includes(active.name) ? (
        <div style={{ padding: 14, background: '#fee2e2', borderTop: '1px solid #fca5a5', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#991b1b', fontSize: 13 }}>
            <ShieldAlert size={16} />
            <span>You have blocked {active.name}. You cannot send or receive messages from this user.</span>
          </div>
          <Button
            size="sm"
            variant="quiet"
            style={{ color: '#991b1b', borderColor: '#fca5a5' }}
            onClick={() => {
              setBlockedUsers?.((prev) => prev.filter((u) => u !== active.name));
              a.toast(`Unblocked ${active.name}`);
            }}
          >
            Unblock
          </Button>
        </div>
      ) : recording ? (
        <div className="message-compose" style={{ alignItems: 'center' }}>
          <div className="voice-recorder">
            <span className="voice-rec-dot" />
            <span className="voice-timer">{formatDuration(recordTime)}</span>
            <div className="voice-waves">
              <span />
              <span />
              <span />
              <span />
              <span />
            </div>
            <IconButton
              icon={X}
              label="Cancel recording"
              className="voice-cancel-btn"
              onClick={() => setRecording(false)}
            />
            <IconButton
              icon={Send}
              label="Send voice note"
              className="voice-send-btn"
              onClick={sendVoiceNote}
            />
          </div>
        </div>
      ) : (
        <form className="message-compose" onSubmit={submit}>
          <input
            ref={fileInput}
            className="attachment-input"
            type="file"
            accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.zip"
            onChange={attach}
          />
          <IconButton
            icon={Paperclip}
            label="Attach file"
            className="message-attach-btn"
            onClick={() => fileInput.current?.click()}
            disabled={uploading}
          />
          <input
            value={draft}
            className="message-input"
            onChange={(event) => setDraft(event.target.value)}
            placeholder={
              uploading ? 'Preparing file…' : editingMessage ? 'Edit your message…' : `Message ${active.name}`
            }
            aria-label={`Message ${active.name}`}
            disabled={uploading}
          />
          <IconButton
            icon={Mic}
            label="Record voice note"
            className="message-mic-btn"
            onClick={() => setRecording(true)}
          />
          <IconButton
            icon={Send}
            label="Send message"
            type="submit"
            className="message-send-btn"
            disabled={uploading}
          />
        </form>
      )}

      {info && (
        <ChatUserProfileDrawer
          active={active}
          onClose={() => setInfo(false)}
          onStartCall={onStartCall}
          onPreviewImage={onPreviewImage}
          starred={starred}
          toggleStar={toggleStar}
          disappearing={disappearing}
          setDisappearing={setDisappearing}
          blockedUsers={blockedUsers}
          setBlockedUsers={setBlockedUsers}
          onOpenManageMembers={() => setShowManageMembersModal(true)}
        />
      )}
    </section>
  );
}

function FavouriteMessagesModal({ active, starred, toggleStar, onClose }) {
  const favMessages = (active.messages || []).filter((m) => starred.includes(m.id));
  return (
    <Dialog title="Favourite messages" onClose={onClose}>
      <div style={{ maxHeight: 360, overflowY: 'auto', padding: '4px 0', display: 'flex', flexDirection: 'column', gap: 10 }}>
        {favMessages.length > 0 ? (
          favMessages.map((m) => (
            <div
              key={m.id}
              style={{
                padding: 12,
                borderRadius: 8,
                background: 'var(--soft)',
                border: '1px solid var(--line)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'flex-start',
                gap: 12,
              }}
            >
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 4 }}>
                  <b style={{ fontSize: 13 }}>{m.mine ? 'You' : active.name}</b>
                  <small style={{ color: 'var(--muted)', fontSize: 11 }}>{m.time || 'Recently'}</small>
                </div>
                <p style={{ margin: 0, fontSize: 13, color: 'var(--ink)' }}>{m.text}</p>
              </div>
              <button
                type="button"
                onClick={() => toggleStar(m.id)}
                style={{ background: 'none', border: 0, cursor: 'pointer', color: '#eab308', padding: 4 }}
                title="Remove from favourites"
              >
                <Star size={16} fill="currentColor" />
              </button>
            </div>
          ))
        ) : (
          <Empty title="No favourite messages" text="Star any message in this conversation to find it here." />
        )}
      </div>
    </Dialog>
  );
}

function ManageGroupMembersModal({ active, app, onClose }) {
  const currentThread = app.threads?.find((t) => t.id === active.id) || active;
  const rawMembers = currentThread.members || ['You', 'Riya Sharma', 'Aman Verma'];
  const currentMembers = rawMembers.map((m) => (typeof m === 'string' ? m : m.name || 'Member'));
  const [selectedUserToAdd, setSelectedUserToAdd] = useState('');

  const availableToAdd = people.filter(
    (p) => !currentMembers.includes(p.name) && p.name !== app.profile.name
  );

  const handleAddMember = () => {
    if (!selectedUserToAdd) return;
    app.setThreads((old) =>
      old.map((t) =>
        t.id === active.id
          ? { ...t, members: [...(t.members || rawMembers), selectedUserToAdd] }
          : t
      )
    );
    app.toast(`Added ${selectedUserToAdd} to ${active.name}`);
    setSelectedUserToAdd('');
  };

  const handleRemoveMember = (name) => {
    if (window.confirm(`Are you sure you want to remove ${name} from this group?`)) {
      app.setThreads((old) =>
        old.map((t) => {
          if (t.id !== active.id) return t;
          const prev = t.members || rawMembers;
          const filtered = prev.filter((m) => {
            const mName = typeof m === 'string' ? m : m.name || 'Member';
            return mName !== name;
          });
          return { ...t, members: filtered };
        })
      );
      app.toast(`Removed ${name} from ${active.name}`);
    }
  };

  return (
    <Dialog title={`Manage members · ${active.name}`} onClose={onClose}>
      <div style={{ padding: '6px 0', display: 'flex', flexDirection: 'column', gap: 14 }}>
        {availableToAdd.length > 0 && (
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <select
              style={{
                flex: 1,
                padding: '8px 10px',
                borderRadius: 6,
                border: '1px solid var(--line)',
                background: 'var(--surface)',
                color: 'var(--ink)',
              }}
              value={selectedUserToAdd}
              onChange={(e) => setSelectedUserToAdd(e.target.value)}
            >
              <option value="">Select a user to add...</option>
              {availableToAdd.map((p) => (
                <option key={p.name} value={p.name}>
                  {p.name} · {p.title || 'Teammate'}
                </option>
              ))}
            </select>
            <Button size="sm" icon={UserPlus} onClick={handleAddMember} disabled={!selectedUserToAdd}>
              Add to group
            </Button>
          </div>
        )}

        <div>
          <b style={{ fontSize: 13, display: 'block', marginBottom: 8, color: 'var(--muted)' }}>
            GROUP MEMBERS ({currentMembers.length})
          </b>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxHeight: 240, overflowY: 'auto' }}>
            {currentMembers.map((name) => (
              <div
                key={name}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '8px 12px',
                  borderRadius: 6,
                  border: '1px solid var(--line)',
                  background: 'var(--surface)',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <Avatar name={name} size="sm" />
                  <div>
                    <b style={{ fontSize: 13 }}>{name}</b>
                    {name === 'You' && <Tag style={{ marginLeft: 6 }}>You</Tag>}
                  </div>
                </div>
                {name !== 'You' && (
                  <Button
                    size="sm"
                    variant="ghost"
                    style={{ color: 'var(--coral)' }}
                    icon={UserMinus}
                    onClick={() => handleRemoveMember(name)}
                  >
                    Remove
                  </Button>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>
    </Dialog>
  );
}

function ChatUserProfileDrawer({
  active,
  onClose,
  onStartCall,
  onPreviewImage,
  starred,
  toggleStar,
  disappearing,
  setDisappearing,
  blockedUsers = [],
  setBlockedUsers,
  onOpenManageMembers,
}) {
  const a = useApp();
  const [muted, setMuted] = useState(false);
  const [theme, setTheme] = useState('Default');
  const isGroup = active.context === 'Group' || active.isGroup;
  const person = isGroup
    ? {
        name: active.name,
        role: `Group · ${(active.members || []).length || 3} members`,
        location: 'Group Chat',
        about: active.about || 'Group chat for team discussion and collaboration.',
      }
    : (people.find((p) => p.name === active.name) || {
        name: active.name,
        role: active.context === 'Group' ? 'Group chat' : 'Active collaborator',
        location: 'Bengaluru, India',
        about: 'Collaborating on Collobster product workspaces, design systems, and features.',
      });

  const [tab, setTab] = useState(isGroup ? 'Members' : 'Chat settings');

  return (
    <aside className="conversation-info chat-profile-drawer" style={{ width: 330, overflowY: 'auto' }}>
      <div className="conversation-info-head">
        <h3>{isGroup ? 'Group info' : 'Chat Profile & Info'}</h3>
        <button type="button" onClick={onClose} aria-label="Close contact info">
          ×
        </button>
      </div>

      <div className="chat-profile-hero" style={{ textAlign: 'center', padding: '16px 8px 12px' }}>
        <div
          onClick={() => {
            if (!isGroup && a.openUserProfile) {
              a.openUserProfile(active.name, 'Posts');
            }
          }}
          style={{ cursor: isGroup ? 'default' : 'pointer', display: 'inline-block' }}
          title={isGroup ? active.name : `Click to open ${active.name}'s profile and posts`}
        >
          <Avatar person={isGroup ? null : person} name={active.name} size="lg" />
          <h3 style={{ margin: '10px 0 2px' }}>{active.name}</h3>
        </div>
        <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--color-muted, #71767b)' }}>
          {isGroup ? `Group · ${(active.members || []).length || 3} members` : (person.role || active.context)}
        </p>
        <small style={{ display: 'block', marginTop: 4, color: '#10b981', fontWeight: 600 }}>
          ● Active now
        </small>
        <div style={{ display: 'flex', justifyContent: 'center', gap: 10, marginTop: 12 }}>
          <button
            type="button"
            className="action-pill-btn"
            onClick={() => onStartCall(active.name, 'audio')}
            title="Audio call"
            style={{ padding: '6px 14px', borderRadius: 20, border: '1px solid var(--color-border, #ddd)', background: 'var(--color-surface, #fff)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.82rem' }}
          >
            <Phone size={14} /> Audio
          </button>
          <button
            type="button"
            className="action-pill-btn"
            onClick={() => onStartCall(active.name, 'video')}
            title="Video call"
            style={{ padding: '6px 14px', borderRadius: 20, border: '1px solid var(--color-border, #ddd)', background: 'var(--color-surface, #fff)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 6, fontSize: '0.82rem' }}
          >
            <Video size={14} /> Video
          </button>
        </div>
      </div>

      {!isGroup && (
        <div style={{ padding: '0 8px', margin: '6px 0 10px' }}>
          <div
            className="chat-profile-selection"
            style={{
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              gap: 6,
              background: 'var(--canvas)',
              padding: 4,
              borderRadius: 8,
              border: '1px solid var(--line)',
            }}
          >
            <button
              type="button"
              onClick={() => {
                if (a.openUserProfile) {
                  a.openUserProfile(active.name, 'Posts');
                }
              }}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 6,
                padding: '8px 10px',
                border: '1px solid transparent',
                borderRadius: 6,
                background: 'var(--surface)',
                color: 'var(--ink)',
                fontSize: '0.82rem',
                fontWeight: 700,
                cursor: 'pointer',
                boxShadow: '0 1px 3px rgba(0,0,0,0.06)',
              }}
              title={`View ${active.name}'s full profile`}
            >
              <User size={15} /> User profile
            </button>
            <button
              type="button"
              onClick={() => {
                setTab('Chat settings');
              }}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: 6,
                padding: '8px 10px',
                border: tab === 'Chat settings' ? '1px solid var(--accent, #e34d42)' : '1px solid transparent',
                borderRadius: 6,
                background: tab === 'Chat settings' ? 'var(--accent-tint, rgba(227, 77, 66, 0.1))' : 'var(--surface)',
                color: tab === 'Chat settings' ? 'var(--accent, #e34d42)' : 'var(--ink)',
                fontSize: '0.82rem',
                fontWeight: 700,
                cursor: 'pointer',
              }}
              title="Open chat settings & preferences"
            >
              <Settings size={15} /> Chat profile
            </button>
          </div>
        </div>
      )}

      <div style={{ padding: '0 8px', marginBottom: 12 }}>
        <Tabs
          items={
            isGroup
              ? ['Members', 'Media & files', 'Chat settings']
              : ['Chat settings', 'User profile', 'Media & files']
          }
          value={tab}
          onChange={(nextTab) => {
            if (nextTab === 'User profile') {
              if (!isGroup && a.openUserProfile) {
                a.openUserProfile(active.name, 'Posts');
              }
              return;
            }
            if (nextTab === 'Chat profile') {
              setTab('Chat settings');
              return;
            }
            setTab(nextTab);
          }}
        />
      </div>

      {isGroup && tab === 'Members' && (
        <div className="chat-profile-details" style={{ display: 'flex', flexDirection: 'column', gap: 10, padding: '0 8px' }}>
          <div className="detail-card" style={{ background: 'var(--color-surface-hover, #f8f9fa)', padding: 10, borderRadius: 8 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
              <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: 0.5, color: 'var(--color-muted, #888)', fontWeight: 600 }}>Group Members</span>
              {onOpenManageMembers && (
                <Button size="sm" variant="ghost" icon={Users} onClick={onOpenManageMembers}>
                  Manage members
                </Button>
              )}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 8 }}>
              {(active.members || ['You', 'Riya Sharma', 'Aman Verma']).map((m, idx) => {
                const mName = typeof m === 'string' ? m : (m.name || 'Member');
                return (
                  <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <Avatar name={mName} />
                    <span style={{ flex: 1, fontSize: '0.85rem', fontWeight: 600 }}>{mName}</span>
                    <small style={{ color: 'var(--muted)', fontSize: '0.75rem' }}>{idx === 0 ? 'Admin' : 'Member'}</small>
                  </div>
                );
              })}
            </div>
          </div>
          <div className="detail-card" style={{ background: 'var(--color-surface-hover, #f8f9fa)', padding: 10, borderRadius: 8 }}>
            <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: 0.5, color: 'var(--color-muted, #888)', fontWeight: 600 }}>Group Description</span>
            <p style={{ margin: '4px 0 0', fontSize: '0.85rem' }}>{person.about}</p>
          </div>
        </div>
      )}

      {!isGroup && tab === 'Profile' && (
        <div className="chat-profile-details" style={{ display: 'flex', flexDirection: 'column', gap: 10, padding: '0 8px' }}>
          <Button
            size="sm"
            variant="secondary"
            onClick={() => a.openUserProfile && a.openUserProfile(active.name, 'Posts')}
            style={{ width: '100%', marginBottom: 4 }}
          >
            View profile &amp; posts
          </Button>
          <div className="detail-card" style={{ background: 'var(--color-surface-hover, #f8f9fa)', padding: 10, borderRadius: 8 }}>
            <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: 0.5, color: 'var(--color-muted, #888)', fontWeight: 600 }}>About & Bio</span>
            <p style={{ margin: '4px 0 0', fontSize: '0.85rem' }}>{person.about || 'Available for design, product strategy, and code review.'}</p>
          </div>
          <div className="detail-card" style={{ background: 'var(--color-surface-hover, #f8f9fa)', padding: 10, borderRadius: 8 }}>
            <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: 0.5, color: 'var(--color-muted, #888)', fontWeight: 600 }}>Location & Timezone</span>
            <p style={{ margin: '4px 0 0', fontSize: '0.85rem' }}>{person.location || 'India · Asia/Kolkata (IST)'}</p>
          </div>
          <div className="detail-card" style={{ background: 'var(--color-surface-hover, #f8f9fa)', padding: 10, borderRadius: 8 }}>
            <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', letterSpacing: 0.5, color: 'var(--color-muted, #888)', fontWeight: 600 }}>Mutual spaces</span>
            <p style={{ margin: '4px 0 0', fontSize: '0.85rem' }}>Design Collective, Flutter Developers, 2 Shared Projects</p>
          </div>
        </div>
      )}

      {tab === 'Media & files' && (
        <div className="chat-profile-media" style={{ display: 'flex', flexDirection: 'column', gap: 12, padding: '0 8px' }}>
          <div>
            <span style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--color-muted, #777)' }}>Photos & Media in this chat</span>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 6, marginTop: 6 }}>
              {[photos.team, photos.desk, photos.event, photos.code, photos.team, photos.desk].map((src, idx) => (
                <img
                  key={idx}
                  src={src}
                  alt="Shared media"
                  onClick={() => onPreviewImage(src)}
                  style={{ width: '100%', height: 68, objectFit: 'cover', borderRadius: 6, cursor: 'pointer' }}
                />
              ))}
            </div>
          </div>
          <div>
            <span style={{ fontSize: '0.78rem', fontWeight: 600, color: 'var(--color-muted, #777)' }}>Documents & Files in this chat</span>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginTop: 6 }}>
              {assets.map(([name, detail]) => (
                <div key={name} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 10px', background: 'var(--color-surface-hover, #f8f9fa)', borderRadius: 6, fontSize: '0.82rem' }}>
                  <div>
                    <strong style={{ display: 'block', fontSize: '0.82rem' }}>{name}</strong>
                    <small style={{ color: 'var(--color-muted, #888)' }}>{detail}</small>
                  </div>
                  <IconButton icon={MoreHorizontal} label="Options" onClick={() => a.toast(`Opening ${name}`)} />
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {(tab === 'Chat settings' || tab === 'Chat profile') && (
        <div className="chat-profile-settings" style={{ display: 'flex', flexDirection: 'column', gap: 10, padding: '0 8px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: 10, background: 'var(--color-surface-hover, #f8f9fa)', borderRadius: 8 }}>
            <div>
              <strong style={{ display: 'block', fontSize: '0.84rem' }}>Mute notifications</strong>
              <small style={{ color: 'var(--color-muted, #888)' }}>Silence alerts for this chat</small>
            </div>
            <button
              type="button"
              onClick={() => {
                setMuted(!muted);
                a.toast(muted ? 'Notifications unmuted' : 'Notifications muted for this chat');
              }}
              style={{ padding: '4px 10px', borderRadius: 12, border: '1px solid var(--color-border, #ddd)', background: muted ? '#fef3c7' : '#fff', cursor: 'pointer', fontSize: '0.78rem', fontWeight: 600 }}
            >
              {muted ? 'Muted 🔕' : 'Mute'}
            </button>
          </div>

          <div style={{ padding: 10, background: 'var(--color-surface-hover, #f8f9fa)', borderRadius: 8 }}>
            <strong style={{ display: 'block', fontSize: '0.84rem' }}>Disappearing messages</strong>
            <small style={{ color: 'var(--color-muted, #888)', display: 'block', marginBottom: 8 }}>Auto-delete messages in this chat</small>
            <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
              {['Off', '24 hours', '7 days', '30 days'].map((dur) => (
                <button
                  key={dur}
                  type="button"
                  onClick={() => {
                    if (setDisappearing) setDisappearing(dur);
                    a.toast(`Disappearing messages set to ${dur}`);
                  }}
                  style={{
                    fontSize: '0.75rem',
                    padding: '3px 8px',
                    borderRadius: 6,
                    border: (disappearing || 'Off') === dur ? '1px solid var(--accent, #e34d42)' : '1px solid var(--color-border, #ddd)',
                    background: (disappearing || 'Off') === dur ? 'var(--accent-tint, rgba(227, 77, 66, 0.1))' : '#fff',
                    cursor: 'pointer',
                  }}
                >
                  {dur}
                </button>
              ))}
            </div>
          </div>

          <div style={{ padding: 10, background: 'var(--color-surface-hover, #f8f9fa)', borderRadius: 8 }}>
            <strong style={{ display: 'block', fontSize: '0.84rem' }}>Chat theme & wallpaper</strong>
            <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
              {[
                { name: 'Default', bg: '#f4f6f8' },
                { name: 'Warm', bg: '#fff7ed' },
                { name: 'Rose', bg: '#fff1f2' },
                { name: 'Dark', bg: '#1e293b' },
              ].map((th) => (
                <button
                  key={th.name}
                  type="button"
                  onClick={() => {
                    setTheme(th.name);
                    a.toast(`Applied ${th.name} theme to this chat`);
                  }}
                  style={{
                    width: 28,
                    height: 28,
                    borderRadius: '50%',
                    background: th.bg,
                    border: theme === th.name ? '2px solid var(--accent, #e34d42)' : '1px solid #ccc',
                    cursor: 'pointer',
                  }}
                  title={th.name}
                />
              ))}
            </div>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginTop: 4 }}>
            <Button variant="quiet" icon={Archive} onClick={() => a.toast('Conversation archived')}>
              Archive conversation
            </Button>
            <Button
              variant="quiet"
              icon={Trash2}
              style={{ color: '#ef4444' }}
              onClick={() => {
                if (window.confirm(`Clear all chat history for ${active.name}?`)) {
                  a.setThreads((old) =>
                    old.map((t) =>
                      t.id === active.id
                        ? { ...t, messages: [], pinnedMessageIds: [] }
                        : t
                    )
                  );
                  a.toast('Chat history cleared');
                }
              }}
            >
              Clear chat history
            </Button>
            {!isGroup && (
              <Button
                variant="quiet"
                icon={ShieldAlert}
                style={{
                  color: blockedUsers.includes(active.name) ? '#16a34a' : '#dc2626',
                  borderColor: blockedUsers.includes(active.name) ? '#bbf7d0' : '#fca5a5',
                  marginTop: 6,
                }}
                onClick={() => {
                  if (blockedUsers.includes(active.name)) {
                    setBlockedUsers?.((prev) => prev.filter((u) => u !== active.name));
                    a.toast(`Unblocked ${active.name}`);
                  } else {
                    setBlockedUsers?.((prev) => [...prev, active.name]);
                    a.toast(`Blocked ${active.name}. You will no longer receive messages.`);
                  }
                }}
              >
                {blockedUsers.includes(active.name) ? `Unblock ${active.name}` : `Block ${active.name}`}
              </Button>
            )}
          </div>
        </div>
      )}
    </aside>
  );
}

// ─── 5. Call & Media Modals ──────────────────────────────────────────────────

export function CallModal({ call, setCall, onEnd, onMinimize }) {
  const a = useApp();

  const groupThread = a?.threads?.find((t) => t.name === call.name && (t.isGroup || t.context === 'Group'));
  const isGroup = Boolean(call.isGroup || (call.members && call.members.length > 0) || groupThread);
  const memberList = (call.members && call.members.length > 0)
    ? call.members.map((m) => (typeof m === 'string' ? m : m.name || m))
    : (groupThread?.members?.map((m) => (typeof m === 'string' ? m : m.name || m)) || ['You', 'Riya Sharma', 'Aman Verma']);

  const defaultWidth = isGroup ? 460 : 380;
  const defaultHeight = isGroup ? 520 : 440;

  const [size, setSize] = useState(() => {
    const screenW = typeof window !== 'undefined' ? window.innerWidth : 1024;
    return {
      width: Math.min(defaultWidth, screenW - 24),
      height: defaultHeight,
    };
  });

  const [position, setPosition] = useState(() => {
    const screenW = typeof window !== 'undefined' ? window.innerWidth : 1024;
    const screenH = typeof window !== 'undefined' ? window.innerHeight : 768;
    const w = Math.min(defaultWidth, screenW - 24);
    const h = defaultHeight;
    if (screenW > 768) {
      return {
        x: Math.max(16, screenW - w - 24),
        y: 72,
      };
    }
    return {
      x: Math.max(8, Math.round((screenW - w) / 2)),
      y: Math.max(16, Math.round((screenH - h) / 2)),
    };
  });

  const [isMaximized, setIsMaximized] = useState(true);
  const [isDragging, setIsDragging] = useState(false);
  const [isResizing, setIsResizing] = useState(false);

  const prevRectRef = useRef(null);

  // Dragging logic
  const handleDragStart = (e) => {
    if (e.target.closest('button, input, [role="button"]')) return;

    if (isMaximized) {
      setIsMaximized(false);
    }

    const clientX = e.clientX ?? e.touches?.[0]?.clientX;
    const clientY = e.clientY ?? e.touches?.[0]?.clientY;
    if (clientX === undefined || clientY === undefined) return;

    setIsDragging(true);
    const startX = clientX;
    const startY = clientY;
    const initialX = position.x;
    const initialY = position.y;

    const onMove = (moveEvent) => {
      const curX = moveEvent.clientX ?? moveEvent.touches?.[0]?.clientX;
      const curY = moveEvent.clientY ?? moveEvent.touches?.[0]?.clientY;
      if (curX === undefined || curY === undefined) return;

      const deltaX = curX - startX;
      const deltaY = curY - startY;

      const maxX = Math.max(0, window.innerWidth - size.width);
      const maxY = Math.max(0, window.innerHeight - 56);

      const nextX = Math.min(maxX, Math.max(0, initialX + deltaX));
      const nextY = Math.min(maxY, Math.max(0, initialY + deltaY));

      setPosition({ x: nextX, y: nextY });
    };

    const onEndDrag = () => {
      setIsDragging(false);
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onEndDrag);
      window.removeEventListener('touchmove', onMove);
      window.removeEventListener('touchend', onEndDrag);
    };

    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onEndDrag);
    window.addEventListener('touchmove', onMove, { passive: true });
    window.addEventListener('touchend', onEndDrag);
  };

  // Resizing logic
  const handleResizeStart = (e) => {
    e.stopPropagation();
    e.preventDefault();

    if (isMaximized) setIsMaximized(false);
    setIsResizing(true);

    const clientX = e.clientX ?? e.touches?.[0]?.clientX;
    const clientY = e.clientY ?? e.touches?.[0]?.clientY;
    if (clientX === undefined || clientY === undefined) return;

    const startX = clientX;
    const startY = clientY;
    const startW = size.width;
    const startH = size.height;

    const onMove = (moveEvent) => {
      const curX = moveEvent.clientX ?? moveEvent.touches?.[0]?.clientX;
      const curY = moveEvent.clientY ?? moveEvent.touches?.[0]?.clientY;
      if (curX === undefined || curY === undefined) return;

      const deltaX = curX - startX;
      const deltaY = curY - startY;

      const minW = 280;
      const minH = 260;
      const maxW = Math.max(minW, window.innerWidth - position.x - 8);
      const maxH = Math.max(minH, window.innerHeight - position.y - 8);

      const nextW = Math.min(maxW, Math.max(minW, startW + deltaX));
      const nextH = Math.min(maxH, Math.max(minH, startH + deltaY));

      setSize({ width: nextW, height: nextH });
    };

    const onEndResize = () => {
      setIsResizing(false);
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onEndResize);
      window.removeEventListener('touchmove', onMove);
      window.removeEventListener('touchend', onEndResize);
    };

    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onEndResize);
    window.addEventListener('touchmove', onMove, { passive: true });
    window.addEventListener('touchend', onEndResize);
  };

  const toggleMaximize = () => {
    if (isMaximized) {
      if (prevRectRef.current) {
        setPosition(prevRectRef.current.position);
        setSize(prevRectRef.current.size);
      }
      setIsMaximized(false);
    } else {
      prevRectRef.current = { position, size };
      setIsMaximized(true);
    }
  };

  useEffect(() => {
    const handleWindowResize = () => {
      if (isMaximized) return;
      setPosition((prevPos) => ({
        x: Math.min(Math.max(0, prevPos.x), Math.max(0, window.innerWidth - 100)),
        y: Math.min(Math.max(0, prevPos.y), Math.max(0, window.innerHeight - 60)),
      }));
      setSize((prevSize) => ({
        width: Math.min(prevSize.width, window.innerWidth - 16),
        height: Math.min(prevSize.height, window.innerHeight - 16),
      }));
    };
    window.addEventListener('resize', handleWindowResize);
    return () => window.removeEventListener('resize', handleWindowResize);
  }, [isMaximized]);

  const modalStyle = isMaximized
    ? {
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100vw',
        height: '100vh',
        maxWidth: '100vw',
        maxHeight: '100vh',
        borderRadius: 0,
        zIndex: 1501,
      }
    : {
        position: 'fixed',
        left: `${position.x}px`,
        top: `${position.y}px`,
        width: `${size.width}px`,
        height: `${size.height}px`,
        maxWidth: 'calc(100vw - 16px)',
        maxHeight: 'calc(100vh - 16px)',
        zIndex: 1001,
      };

  return (
    <div className={`call-modal-overlay call-movable-overlay ${isMaximized ? 'has-maximized' : ''}`}>
      <div
        className={`call-modal call-modal-floating ${isDragging ? 'dragging' : ''} ${isResizing ? 'resizing' : ''} ${isMaximized ? 'maximized' : ''}`}
        style={modalStyle}
      >
        <div
          className="call-modal-drag-bar"
          onMouseDown={handleDragStart}
          onTouchStart={handleDragStart}
          title={isMaximized ? 'Collobster Live Call' : 'Drag to move call popup anywhere on screen'}
        >
          <div className="call-drag-info">
            {!isMaximized && (
              <span className="call-drag-handle-icon" aria-hidden="true">
                <GripHorizontal size={15} />
              </span>
            )}
            <span className="call-live-indicator" />
            <span className="call-drag-title">
              {call.name} · {isGroup ? 'Group' : call.type === 'video' ? 'Video' : 'Audio'}
            </span>
          </div>

          <div className="call-drag-actions">
            {onMinimize && (
              <button
                type="button"
                className="call-window-btn minimize-pill-btn"
                onClick={onMinimize}
                aria-label="Minimize to small circle"
                title="Minimize to small circle"
              >
                <Minimize2 size={13} />
                <span className="minimize-pill-text">Minimize</span>
              </button>
            )}
            <button
              type="button"
              className="call-window-btn"
              onClick={toggleMaximize}
              aria-label={isMaximized ? 'Switch to floating window' : 'Maximize to full screen'}
              title={isMaximized ? 'Floating window' : 'Full screen'}
            >
              <Maximize2 size={13} />
            </button>
            <button
              type="button"
              className="call-window-btn close"
              onClick={onEnd}
              aria-label="End call"
              title="End call"
            >
              <PhoneOff size={13} />
            </button>
          </div>
        </div>

        <div className="call-modal-body">
          {isGroup ? (
            <div className="group-call-view" style={{ width: '100%', margin: '4px 0 10px', flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
              <div
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  alignSelf: 'center',
                  gap: 6,
                  padding: '3px 10px',
                  borderRadius: 20,
                  background: 'rgba(230, 81, 69, 0.12)',
                  color: 'var(--coral, #e65145)',
                  fontSize: 11,
                  fontWeight: 700,
                  marginBottom: 10,
                }}
              >
                <Users size={13} />
                <span>Group Call ({memberList.length} participants)</span>
              </div>

              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: memberList.length <= 2 ? 'repeat(2, 1fr)' : 'repeat(auto-fit, minmax(90px, 1fr))',
                  gap: 8,
                  width: '100%',
                  flex: 1,
                  maxHeight: '100%',
                  overflowY: 'auto',
                  padding: '2px',
                }}
              >
                {memberList.map((mName, idx) => {
                  const person = people.find((p) => p.name === mName);
                  const isUser = mName === 'You' || mName.toLowerCase().includes('you');
                  return (
                    <div
                      key={`${mName}-${idx}`}
                      style={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        background: 'rgba(255, 255, 255, 0.08)',
                        border: '1px solid rgba(255, 255, 255, 0.15)',
                        borderRadius: 10,
                        padding: '10px 6px',
                      }}
                    >
                      <Avatar person={person} name={mName} />
                      <span
                        style={{
                          fontSize: 11,
                          fontWeight: 600,
                          marginTop: 6,
                          textAlign: 'center',
                          maxWidth: 80,
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                        }}
                      >
                        {isUser ? 'You (Host)' : mName}
                      </span>
                      <span
                        style={{
                          fontSize: 10,
                          color: '#22c55e',
                          marginTop: 3,
                          display: 'flex',
                          alignItems: 'center',
                          gap: 4,
                          fontWeight: 600,
                        }}
                      >
                        <span
                          style={{
                            width: 5,
                            height: 5,
                            borderRadius: '50%',
                            background: '#22c55e',
                            display: 'inline-block',
                            boxShadow: '0 0 5px #22c55e',
                          }}
                        />
                        Connected
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          ) : call.type === 'video' && !call.videoOff ? (
            <div className={`call-video-preview ${isMaximized ? 'fullscreen-preview' : ''}`}>
              <span className="video-status-dot">
                <Video size={12} /> Live camera
              </span>
              <span>HD Video Connected</span>
            </div>
          ) : (
            <Avatar
              className={`call-avatar ${isMaximized ? 'call-avatar-lg' : ''}`}
              person={call.person}
              name={call.name}
            />
          )}

          <h3 className="call-participant-heading">{call.name}</h3>
          <p className="call-type-subtext">
            {isGroup
              ? (call.type === 'video' ? 'Group Video Call' : 'Group Audio Call')
              : (call.type === 'video' ? 'Collobster Video Call' : 'Collobster Audio Call')}
          </p>
          <span className="call-timer">{formatDuration(call.duration || 0)}</span>
        </div>

        <div className="call-modal-bottom-bar">
          <div className="call-controls">
            <button
              type="button"
              className={`call-btn ${call.muted ? 'active' : ''}`}
              onClick={() => setCall((old) => (old ? { ...old, muted: !old.muted } : null))}
              aria-label={call.muted ? 'Unmute mic' : 'Mute mic'}
              title={call.muted ? 'Unmute mic' : 'Mute mic'}
            >
              {call.muted ? <MicOff size={20} /> : <Mic size={20} />}
            </button>
            {call.type === 'video' && (
              <button
                type="button"
                className={`call-btn ${call.videoOff ? 'active' : ''}`}
                onClick={() =>
                  setCall((old) => (old ? { ...old, videoOff: !old.videoOff } : null))
                }
                aria-label={call.videoOff ? 'Turn video on' : 'Turn video off'}
                title={call.videoOff ? 'Turn camera on' : 'Turn camera off'}
              >
                {call.videoOff ? <VideoOff size={20} /> : <Video size={20} />}
              </button>
            )}
            {onMinimize && (
              <button
                type="button"
                className="call-btn minimize"
                onClick={onMinimize}
                aria-label="Minimize to small circle"
                title="Minimize to small circle"
              >
                <Minimize2 size={18} />
              </button>
            )}
            <button
              type="button"
              className="call-btn hangup"
              onClick={onEnd}
              aria-label="End call"
              title="End call"
            >
              <PhoneOff size={22} />
            </button>
          </div>
        </div>

        {!isMaximized && (
          <div
            className="call-modal-resize-handle"
            onMouseDown={handleResizeStart}
            onTouchStart={handleResizeStart}
            title="Drag to resize call window"
            aria-label="Resize call window"
          >
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" aria-hidden="true">
              <path d="M10 2L2 10M10 6L6 10M10 10L10 10.01" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
            </svg>
          </div>
        )}
      </div>
    </div>
  );
}

export function MinimizedCallBar({ call, setCall, onEnd, onExpand }) {
  if (!call) return null;
  const isVideo = call.type === 'video';
  const isGroup = Boolean(call.isGroup || (call.members && call.members.length > 0));
  const CallIcon = isVideo ? Video : Phone;

  // Floating position state (movable anywhere on the screen)
  const [pos, setPos] = useState(() => {
    const w = typeof window !== 'undefined' ? window.innerWidth : 1024;
    const h = typeof window !== 'undefined' ? window.innerHeight : 768;
    return {
      x: Math.max(16, w - (w <= 640 ? 64 : 76)),
      y: Math.max(90, h - (w <= 640 ? 190 : 140)),
    };
  });

  const [isDragging, setIsDragging] = useState(false);
  const dragRef = useRef({ didDrag: false, startX: 0, startY: 0, initX: 0, initY: 0 });

  const handlePointerDown = (e) => {
    if (e.target.closest('.minimized-circle-end-btn')) return;

    const clientX = e.clientX ?? e.touches?.[0]?.clientX;
    const clientY = e.clientY ?? e.touches?.[0]?.clientY;
    if (clientX === undefined || clientY === undefined) return;

    dragRef.current = {
      didDrag: false,
      startX: clientX,
      startY: clientY,
      initX: pos.x,
      initY: pos.y,
    };

    const onMove = (moveEvt) => {
      const curX = moveEvt.clientX ?? moveEvt.touches?.[0]?.clientX;
      const curY = moveEvt.clientY ?? moveEvt.touches?.[0]?.clientY;
      if (curX === undefined || curY === undefined) return;

      const dx = curX - dragRef.current.startX;
      const dy = curY - dragRef.current.startY;

      if (Math.abs(dx) > 4 || Math.abs(dy) > 4) {
        dragRef.current.didDrag = true;
        setIsDragging(true);
      }

      const circleSize = 46;
      const maxX = Math.max(0, window.innerWidth - circleSize - 8);
      const maxY = Math.max(0, window.innerHeight - circleSize - 8);

      const nextX = Math.min(maxX, Math.max(8, dragRef.current.initX + dx));
      const nextY = Math.min(maxY, Math.max(8, dragRef.current.initY + dy));

      setPos({ x: nextX, y: nextY });
    };

    const onUp = () => {
      setIsDragging(false);
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
      window.removeEventListener('touchmove', onMove);
      window.removeEventListener('touchend', onUp);
    };

    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
    window.addEventListener('touchmove', onMove, { passive: true });
    window.addEventListener('touchend', onUp);
  };

  const handleClick = () => {
    if (dragRef.current.didDrag) {
      dragRef.current.didDrag = false;
      return;
    }
    onExpand?.();
  };

  return (
    <div
      className={`minimized-call-circle-wrapper ${isDragging ? 'dragging' : ''} ${isVideo ? 'video-call' : 'audio-call'}`}
      style={{ left: `${pos.x}px`, top: `${pos.y}px` }}
      onMouseDown={handlePointerDown}
      onTouchStart={handlePointerDown}
      onClick={handleClick}
      role="button"
      tabIndex={0}
      aria-label={`${isVideo ? 'Video' : 'Audio'} call with ${call.name} in progress. Click to expand full screen.`}
      title={`${call.name} · Click to expand full screen, drag to move`}
    >
      <div className="minimized-call-circle">
        {/* Pulsing ring waves around circle */}
        <span className="call-circle-pulse-ring" />
        <span className="call-circle-pulse-ring-2" />

        {/* Inner circle with Call Icon */}
        <div className="call-circle-icon-core">
          <CallIcon size={20} className="call-circle-main-icon" />
          <span className="call-circle-live-dot" />
        </div>
      </div>

      {/* Floating duration pill attached under circle */}
      <span className="minimized-circle-timer">
        {formatDuration(call.duration || 0)}
      </span>

      {/* Quick hangup icon on hover / touch */}
      <button
        type="button"
        className="minimized-circle-end-btn"
        onClick={(e) => {
          e.stopPropagation();
          onEnd?.();
        }}
        aria-label="End call"
        title="End call"
      >
        <PhoneOff size={9} />
      </button>
    </div>
  );
}

export { MinimizedCallBar as MinimizedCallCircle };

function LightboxModal({ item, onClose }) {
  return (
    <div className="lightbox-overlay" onClick={onClose}>
      <div className="lightbox-content" onClick={(e) => e.stopPropagation()}>
        <button
          className="lightbox-close"
          onClick={onClose}
          aria-label="Close image preview"
        >
          ×
        </button>
        <img
          className="lightbox-image"
          src={item.src}
          alt={item.title || 'Attachment preview'}
        />
        <span style={{ color: '#fff', marginTop: 8, fontSize: 12 }}>
          {item.title}
        </span>
      </div>
    </div>
  );
}

// ─── 6. Notifications Component (Exported) ───────────────────────────────────

export function Notifications() {
  const a = useApp();
  const [tab, setTab] = useState('All');

  const list =
    tab === 'Unread'
      ? a.notifications.filter((notification) => notification.unread)
      : a.notifications;

  const markAll = () => {
    a.setNotifications((old) =>
      old.map((notification) => ({ ...notification, unread: false }))
    );
    a.toast('All caught up');
  };

  return (
    <div className="notifications-page">
      <PageHeader
        eyebrow="UPDATES"
        title="A calm place for what needs you."
        description="Notifications are grouped around the people and spaces you care about."
      >
        <Button icon={CheckCheck} onClick={markAll}>
          Mark all read
        </Button>
      </PageHeader>
      <div className="notification-layout">
        <section className="notification-list">
          <Tabs
            items={['All', 'Unread', 'Mentions']}
            value={tab}
            onChange={setTab}
          />
          {list.length ? (
            list.map((notification) => (
              <button
                key={notification.id}
                className={`notification-item ${
                  notification.unread ? 'unread' : ''
                }`}
                onClick={() => {
                  a.markNotificationRead(notification);
                  if (notification.module) a.go(notification.module);
                }}
              >
                <span className="notification-icon">
                  <Bell size={18} />
                </span>
                <span>
                  <strong>{notification.title}</strong>
                  <p>{notification.text}</p>
                  <small>{notification.time}</small>
                </span>
                {notification.unread && <i />}
              </button>
            ))
          ) : (
            <Empty
              title="You are all caught up"
              text="New activity from your spaces will land here."
            />
          )}
        </section>
        <aside className="space-rail">
          <span className="module-icon coral">
            <AtSign />
          </span>
          <h2>In your control</h2>
          <p>
            Choose the kinds of updates that deserve your attention, then adjust
            the rest in Settings.
          </p>
          <Button onClick={() => a.go('Settings')}>
            Manage preferences
          </Button>
        </aside>
      </div>
    </div>
  );
}
