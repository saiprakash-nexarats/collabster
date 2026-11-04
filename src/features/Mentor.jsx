import React, { useMemo, useState } from 'react';
import {
  Award,
  BadgeCheck,
  BookOpen,
  CalendarDays,
  CheckCircle2,
  ChevronRight,
  CircleDollarSign,
  Clock3,
  FileText,
  GraduationCap,
  LineChart,
  MessageCircle,
  Play,
  Plus,
  Search,
  Send,
  Star,
  Users,
  Video,
} from 'lucide-react';
import { useApp } from '../context';
import { useStoredState } from '../store';
import {
  Avatar,
  Button,
  Empty,
  Field,
  PageHeader,
  Progress,
  SearchField,
  SectionTitle,
  Tag,
  Tabs,
} from '../components';
import { courses, people } from '../data';

// ─── Constants & Seed Data ───────────────────────────────────────────────────

const mentors = [
  {
    id: 'aman',
    name: 'Aman Verma',
    role: 'Senior Flutter developer · Orbit',
    person: people[1],
    rating: '4.9',
    reviews: '342',
    rate: 1200,
    experience: '6 years',
    learners: '1.2k',
    tags: ['Flutter', 'Dart', 'Firebase'],
    about:
      'I help builders turn product ideas into reliable Flutter apps, one practical decision at a time.',
  },
  {
    id: 'arjun',
    name: 'Arjun Mehta',
    role: 'Product mentor · Independent',
    person: people[3],
    rating: '4.8',
    reviews: '218',
    rate: 1500,
    experience: '8 years',
    learners: '860',
    tags: ['Product', 'Career', 'Strategy'],
    about:
      'I work with early-career product people on sharper portfolios, interviews, and their next meaningful role.',
  },
  {
    id: 'riya',
    name: 'Riya Sharma',
    role: 'Product designer · Layers',
    person: people[0],
    rating: '4.9',
    reviews: '196',
    rate: 1000,
    experience: '5 years',
    learners: '940',
    tags: ['Design', 'Figma', 'Research'],
    about:
      'I make design careers feel less mysterious through concrete critique and collaborative practice.',
  },
  {
    id: 'neha',
    name: 'Neha Patel',
    role: 'Founder · GreenCart & Backend Architect',
    person: people[2],
    rating: '4.8',
    reviews: '174',
    rate: 1400,
    experience: '8 years',
    learners: '820',
    tags: ['Startups', 'Java', 'Spring Boot', 'Backend', 'Leadership'],
    about:
      'I support founders and backend engineers working through microservice architecture, Spring Boot, and team scale.',
  },
  {
    id: 'vikram',
    name: 'Vikram Rao',
    role: 'Staff Java Architect · Ex-Google & FinTech',
    person: {
      id: 'vikram',
      name: 'Vikram Rao',
      role: 'Staff Java Architect',
      initials: 'VR',
      color: 'blue',
      image: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80',
    },
    rating: '4.95',
    reviews: '286',
    rate: 1800,
    experience: '12 years',
    learners: '1.4k',
    tags: ['Java', 'Spring Boot', 'Backend', 'Architecture', 'Career'],
    about:
      'Staff Engineer specializing in modern Java 21, virtual threads, high-throughput financial architectures, and senior/staff career growth.',
  },
];

const sessionTypes = [
  ['1:1 mentoring', 'Personal guidance and career advice', 60],
  ['Code review', 'Bring a focused problem or pull request', 45],
  ['Portfolio review', 'Get candid, actionable feedback', 45],
  ['Project discussion', 'Unblock an idea or build plan', 60],
];

const initialRequests = [
  {
    id: 'rq1',
    learner: 'Riya Singh',
    topic: '1:1 Mentorship Session',
    time: '28 May · 7:00 PM',
    status: 'Pending',
  },
  {
    id: 'rq2',
    learner: 'Aman Kumar',
    topic: 'Code Review Session',
    time: '29 May · 6:00 PM',
    status: 'Pending',
  },
  {
    id: 'rq3',
    learner: 'Neha Patel',
    topic: 'Career Guidance',
    time: '30 May · 5:00 PM',
    status: 'Accepted',
  },
];

const initialContent = [
  {
    id: 'ct1',
    type: 'Video lesson',
    title: 'Widgets in Flutter',
    course: 'Flutter Development Bootcamp',
    status: 'Published',
    views: '1.2k',
    date: '20 May',
  },
  {
    id: 'ct2',
    type: 'Live class',
    title: 'State management clinic',
    course: 'Flutter Development Bootcamp',
    status: 'Scheduled',
    views: '150 registered',
    date: '24 May',
  },
  {
    id: 'ct3',
    type: 'PDF / document',
    title: 'Flutter cheat sheet',
    course: 'Flutter Development Bootcamp',
    status: 'Published',
    views: '1.1k downloads',
    date: '18 May',
  },
  {
    id: 'ct4',
    type: 'Quiz',
    title: 'Dart basics quiz',
    course: 'Flutter Development Bootcamp',
    status: 'Draft',
    views: '0 attempts',
    date: '16 May',
  },
];

const contentTypes = [
  ['Video lesson', 'Upload a recorded lesson', Video],
  ['Live class', 'Schedule a real-time class', CalendarDays],
  ['PDF / document', 'Share notes, slides, or guides', FileText],
  ['Quiz', 'Create an assessment', Award],
  ['Assignment', 'Set practice work', CheckCircle2],
  ['Article', 'Publish a text lesson', BookOpen],
];

// ─── 1. Shared Presentation Helpers ──────────────────────────────────────────

function Metric({ label, value }) {
  return (
    <div>
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  );
}

function StatusPill({ status }) {
  return (
    <span className={`status-pill ${status.toLowerCase().replaceAll(' ', '-')}`}>
      {status}
    </span>
  );
}

function SessionList({ sessions, onModify }) {
  const a = useApp();
  return (
    <section className="session-list">
      {sessions.length ? (
        sessions.map((s) => (
          <article key={s.id} className="session-row" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <span className="date-chip">
                <CalendarDays size={17} />
              </span>
              <div>
                <b>{s.title}</b>
                <small style={{ display: 'block', color: 'var(--color-muted, #71767b)' }}>
                  {s.date || '28 May · 7:00 PM'} · {s.duration || '60 min'} · Online
                </small>
                {s.notes && (
                  <small style={{ display: 'block', marginTop: 2, color: 'var(--color-muted, #888)', fontStyle: 'italic' }}>
                    Note: {s.notes}
                  </small>
                )}
              </div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <StatusPill status={s.status || 'Confirmed'} />
              {s.meetingLink && (
                <button
                  type="button"
                  className="action-pill-btn"
                  onClick={() => a.toast(`Joining room for: ${s.title}`)}
                  style={{ padding: '4px 10px', borderRadius: 14, border: '1px solid var(--color-border, #ddd)', background: 'var(--color-surface, #fff)', cursor: 'pointer', fontSize: '0.78rem' }}
                >
                  Join room
                </button>
              )}
              {onModify && (
                <button
                  type="button"
                  className="action-pill-btn"
                  onClick={() => onModify(s)}
                  style={{ padding: '4px 10px', borderRadius: 14, border: '1px solid var(--color-border, #ddd)', background: 'var(--color-surface, #fff)', cursor: 'pointer', fontSize: '0.78rem', fontWeight: 600 }}
                >
                  Modify session
                </button>
              )}
            </div>
          </article>
        ))
      ) : (
        <Empty
          title="No sessions yet"
          text="A booked mentor conversation will appear here."
        />
      )}
    </section>
  );
}

function RequestList({ requests }) {
  const [storedRequests] = useStoredState('collobster_mentor_requests', initialRequests);
  const displayRequests = requests || storedRequests;

  return (
    <section className="session-list">
      {displayRequests.length ? (
        displayRequests.map((s) => (
          <article key={s.id} className="session-row">
            <span className="date-chip">
              <Clock3 size={17} />
            </span>
            <div>
              <b>{s.topic || s.title}</b>
              <small>{s.time || s.date} · Online</small>
            </div>
            <StatusPill status={s.status} />
          </article>
        ))
      ) : (
        <Empty title="No requests yet" text="Requests submitted to mentors will appear here." />
      )}
    </section>
  );
}

function Reviews() {
  return (
    <section className="reviews">
      <div className="review-summary">
        <strong>4.8</strong>
        <span className="rating">
          <Star size={17} fill="currentColor" />
          <Star size={17} fill="currentColor" />
          <Star size={17} fill="currentColor" />
          <Star size={17} fill="currentColor" />
          <Star size={17} fill="currentColor" />
        </span>
        <small>1.2k reviews across mentors</small>
      </div>
      {[
        'Angela made a complicated next step feel manageable and specific.',
        'A clear, generous conversation with notes I am still using.',
        'The feedback was honest and immediately useful.',
      ].map((x, i) => (
        <article className="review" key={x}>
          <Avatar person={people[i]} />
          <div>
            <b>{people[i].name}</b>
            <p>{x}</p>
            <span className="rating">
              <Star size={13} fill="currentColor" /> 5.0
            </span>
          </div>
        </article>
      ))}
    </section>
  );
}

// ─── 2. Root Mentor View (Exported) ──────────────────────────────────────────

export function Mentor() {
  const a = useApp();
  const [mode, setMode] = useState('Learner');

  return (
    <main className="mentor-shell">
      <PageHeader
        eyebrow="Mentor space"
        title={
          mode === 'Learner'
            ? 'Find guidance that compounds.'
            : mode === 'Mentor'
            ? 'Help learners move forward.'
            : 'Teach what you know.'
        }
        description={
          mode === 'Learner'
            ? 'Find a person, book a focused session, and keep the learning moving.'
            : mode === 'Mentor'
            ? 'Run sessions, respond to requests, and keep your availability clear.'
            : 'Create courses and learning material from one practical studio.'
        }
      >
        <Tabs
          items={['Learner', 'Mentor', 'Tutor studio']}
          value={mode}
          onChange={setMode}
        />
      </PageHeader>
      {mode === 'Learner' ? (
        <LearnerSpace app={a} />
      ) : mode === 'Mentor' ? (
        <MentorWorkspace app={a} />
      ) : (
        <TutorStudio app={a} />
      )}
    </main>
  );
}

// ─── 3. Learner View ─────────────────────────────────────────────────────────

function LearnerSpace({ app }) {
  const [view, setView] = useState('Find mentors');
  const [query, setQuery] = useState('');
  const [tag, setTag] = useState('All');
  const [selected, setSelected] = useState(null);
  const [saved, setSaved] = useState(['aman']);

  const catalog = [
    ...(app.mentorCatalog || []),
    ...mentors.filter(
      (mentor) => !(app.mentorCatalog || []).some((remote) => remote.id === mentor.id)
    ),
  ];

  const visible = useMemo(
    () =>
      catalog.filter(
        (m) =>
          (tag === 'All' || m.tags.includes(tag)) &&
          `${m.name} ${m.role} ${m.tags.join(' ')}`
            .toLowerCase()
            .includes(query.toLowerCase())
      ),
    [tag, query, app.mentorCatalog]
  );

  const save = (id) =>
    setSaved((x) => (x.includes(id) ? x.filter((i) => i !== id) : [...x, id]));

  if (selected) {
    return (
      <MentorProfile
        app={app}
        mentor={selected}
        onBack={() => setSelected(null)}
        saved={saved.includes(selected.id)}
        onSave={() => save(selected.id)}
      />
    );
  }

  const sessions = app.bookings.filter(
    (b) => b.title?.includes('Mentorship') || b.title?.includes('Session')
  );

  return (
    <div className="mentor-layout">
      <section className="mentor-main">
        <Tabs
          items={['Find mentors', 'My sessions', 'Requests', 'Saved', 'Reviews']}
          value={view}
          onChange={setView}
        />

        {view === 'Find mentors' && (
          <>
            <section className="mentor-hero">
              <div>
                <Tag tone="purple">A clearer next step</Tag>
                <h2>Learn from someone who has been there.</h2>
                <p>
                  Book a focused conversation around the work you are doing right now.
                </p>
              </div>
              <div className="mentor-hero-stat">
                <strong>4.8</strong>
                <span>
                  <Star size={15} fill="currentColor" /> average mentor rating
                </span>
              </div>
            </section>

            <div className="mentor-toolbar">
              <SearchField
                value={query}
                onChange={setQuery}
                placeholder="Search mentors, skills, or goals"
              />
              <div className="chips">
                {['All', 'Java', 'Spring Boot', 'Backend', 'Flutter', 'Design', 'AI', 'Product', 'Career', 'Startups'].map(
                  (x) => (
                    <button
                      key={x}
                      className={tag === x ? 'selected' : ''}
                      onClick={() => setTag(x)}
                    >
                      {x}
                    </button>
                  )
                )}
              </div>
            </div>

            <section className="mentor-grid">
              {visible.map((m) => (
                <MentorCard
                  key={m.id}
                  mentor={m}
                  saved={saved.includes(m.id)}
                  onSave={() => save(m.id)}
                  onOpen={() => setSelected(m)}
                />
              ))}
            </section>
          </>
        )}

        {view === 'My sessions' && <SessionList sessions={sessions} />}
        {view === 'Requests' && <RequestList />}

        {view === 'Saved' && (
          <section className="mentor-grid">
            {catalog
              .filter((m) => saved.includes(m.id))
              .map((m) => (
                <MentorCard
                  key={m.id}
                  mentor={m}
                  saved
                  onSave={() => save(m.id)}
                  onOpen={() => setSelected(m)}
                />
              ))}
          </section>
        )}

        {view === 'Reviews' && <Reviews />}
      </section>

      <aside className="mentor-rail">
        <SectionTitle title="How 1:1 mentorship works" />
        <div className="rail-card">
          <b>Prepare your focus</b>
          <p style={{ margin: '4px 0 10px', fontSize: '0.85rem', color: 'var(--color-muted, #71767b)' }}>
            Bring a specific pull request, Figma file, or career dilemma to make every minute count.
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <span style={{ fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: 6 }}>
              ✓ Verified senior mentors
            </span>
            <span style={{ fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: 6 }}>
              ✓ Screen-sharing & notes included
            </span>
            <span style={{ fontSize: '0.82rem', display: 'flex', alignItems: 'center', gap: 6 }}>
              ✓ Flexible rescheduling
            </span>
          </div>
        </div>

        <SectionTitle title="Quick actions" />
        <div className="quick-stack">
          <Button
            variant="ghost"
            icon={MessageCircle}
            onClick={() => {
              const q = window.prompt('Ask a question to mentors in chat:', 'Hi, I need guidance with Flutter and system architecture.');
              if (!q) return;
              const threadId = 'mentor-community-qa';
              app.setThreads?.((prev) => {
                const exists = prev.find((t) => t.id === threadId);
                if (exists) {
                  return prev.map((t) =>
                    t.id === threadId
                      ? { ...t, messages: [...t.messages, { id: `m-${Date.now()}`, mine: true, text: q, time: 'Just now' }] }
                      : t
                  );
                }
                return [
                  {
                    id: threadId,
                    name: 'Mentor Community Q&A',
                    context: 'Mentor Space',
                    status: 'connected',
                    unread: false,
                    messages: [
                      { id: 'm0', mine: false, text: 'Welcome to the mentor chat! Mentors and community leads are here to help.', time: 'Today' },
                      { id: `m-${Date.now()}`, mine: true, text: q, time: 'Just now' }
                    ]
                  },
                  ...prev
                ];
              });
              app.toast('Question posted to mentor chat!');
              app.go('Messages');
            }}
          >
            Ask a question in chat
          </Button>
        </div>
      </aside>
    </div>
  );
}

function MentorCard({ mentor, saved, onSave, onOpen }) {
  return (
    <article className="mentor-card">
      <div className="mentor-card-top">
        <Avatar person={mentor.person} size="large" />
        <button
          className={saved ? 'save-toggle saved' : 'save-toggle'}
          aria-label="Save mentor"
          onClick={onSave}
        >
          ⌑
        </button>
      </div>
      <h3>
        {mentor.name} <BadgeCheck size={16} />
      </h3>
      <p>{mentor.role}</p>
      <div className="rating">
        <Star size={15} fill="currentColor" /> {mentor.rating}{' '}
        <span>({mentor.reviews})</span>
      </div>
      <div className="tag-row">
        {mentor.tags.map((t) => (
          <Tag key={t}>{t}</Tag>
        ))}
      </div>
      <div className="mentor-card-foot">
        <span>INR {mentor.rate}/hr</span>
        <Button onClick={onOpen}>View profile</Button>
      </div>
    </article>
  );
}

function MentorProfile({ app, mentor, onBack, saved, onSave }) {
  const [tab, setTab] = useState('Overview');
  const [booking, setBooking] = useState(false);

  const sendMessage = () => {
    if (!app.threads.some((t) => t.id === mentor.id)) {
      app.setThreads((x) => [
        ...x,
        {
          id: mentor.id,
          name: mentor.name,
          context: 'Mentor',
          status: 'connected',
          unread: false,
          messages: [
            {
              id: 'hello',
              mine: false,
              text: `Hi, I am ${mentor.name}. What would you like to work through?`,
            },
          ],
        },
      ]);
    }
    app.go('Messages');
  };

  return (
    <section className="mentor-profile">
      <button className="back-link" onClick={onBack}>
        ← All mentors
      </button>

      <header className="mentor-profile-head">
        <Avatar person={mentor.person} size="xl" />
        <div>
          <span className="eyebrow">Available this week</span>
          <h2>
            {mentor.name} <BadgeCheck size={18} />
          </h2>
          <p>{mentor.role}</p>
          <div className="rating">
            <Star size={15} fill="currentColor" /> {mentor.rating} ({mentor.reviews}{' '}
            reviews) · {mentor.learners} learners
          </div>
        </div>
        <div className="profile-actions">
          <button
            className={saved ? 'save-toggle saved' : 'save-toggle'}
            onClick={onSave}
          >
            {saved ? 'Saved' : 'Save'}
          </button>
          <Button variant="ghost" icon={MessageCircle} onClick={sendMessage}>
            Message
          </Button>
          <Button icon={CalendarDays} onClick={() => setBooking(true)}>
            Book session
          </Button>
        </div>
      </header>

      {booking ? (
        <BookingFlow app={app} mentor={mentor} onClose={() => setBooking(false)} />
      ) : (
        <>
          <div className="metric-grid">
            <Metric label="Experience" value={mentor.experience} />
            <Metric label="Learners" value={mentor.learners} />
            <Metric label="Response rate" value="98%" />
            <Metric label="Typical rate" value={`INR ${mentor.rate}/hr`} />
          </div>

          <Tabs
            items={['Overview', 'Sessions', 'Courses', 'Reviews', 'About']}
            value={tab}
            onChange={setTab}
          />

          {tab === 'Overview' && (
            <div className="profile-columns">
              <section>
                <h3>About</h3>
                <p>{mentor.about}</p>
                <h3>Expertise</h3>
                <div className="tag-row">
                  {mentor.tags.concat(['Mobile dev']).map((t) => (
                    <Tag key={t}>{t}</Tag>
                  ))}
                </div>
                <h3>What a session can cover</h3>
                <ul className="check-list">
                  <li>Clarify the next best step</li>
                  <li>Review a concrete piece of work</li>
                  <li>Leave with notes and a plan</li>
                </ul>
              </section>
              <section className="inset-card">
                <h3>Upcoming sessions</h3>
                <p>Flutter debugging clinic</p>
                <small>28 May · 7:00 PM · Online</small>
                <Button variant="ghost" onClick={() => setBooking(true)}>
                  Register
                </Button>
              </section>
            </div>
          )}

          {tab === 'Sessions' && (
            <SessionList
              sessions={[
                {
                  id: 's1',
                  title: 'Build real-world Flutter apps',
                  date: '28 May · 7:00 PM',
                  status: 'Open',
                },
                {
                  id: 's2',
                  title: 'State management clinic',
                  date: '02 Jun · 6:00 PM',
                  status: 'Open',
                },
              ]}
            />
          )}

          {tab === 'Courses' && (
            <section className="course-strip">
              {courses.map((c) => (
                <button
                  key={c.id}
                  onClick={() => app.setModal({ kind: 'course', item: c })}
                >
                  <span className={`mini-mark ${c.color}`}>{c.symbol}</span>
                  <span>
                    <b>{c.title}</b>
                    <small>
                      {c.level} · {c.duration}
                    </small>
                  </span>
                  <Play size={17} />
                </button>
              ))}
            </section>
          )}

          {tab === 'Reviews' && <Reviews />}

          {tab === 'About' && (
            <div className="profile-columns">
              <section>
                <h3>Experience</h3>
                <p>
                  {mentor.experience} of product work, teaching, and mentoring.
                  Sessions are online and notes are shared afterwards.
                </p>
              </section>
              <section>
                <h3>Languages</h3>
                <Tag>English</Tag> <Tag>Hindi</Tag>
              </section>
            </div>
          )}
        </>
      )}
    </section>
  );
}

function BookingFlow({ app, mentor, onClose }) {
  const dates = Array.from({ length: 3 }, (_, index) => {
    const value = new Date(Date.now() + (index + 1) * 86400000);
    return {
      value: value.toISOString().slice(0, 10),
      label: value.toLocaleDateString([], { day: '2-digit', month: 'short' }),
    };
  });

  const [step, setStep] = useState(1);
  const [type, setType] = useState(sessionTypes[0]);
  const [date, setDate] = useState(dates[0].value);
  const [time, setTime] = useState('7:00 PM');
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const slots = app.mentorSlots?.[mentor.id] || [];

  React.useEffect(() => {
    if (mentor.id && app.fetchMentorSlots) {
      app.fetchMentorSlots(mentor.id).catch(() => {});
    }
  }, [mentor.id]);

  const finish = async () => {
    setSaving(true);
    setError('');
    try {
      let result;
      if (selectedSlot) {
        result = await app.bookMentorSlot(
          mentor.id,
          selectedSlot.id,
          type[0],
          `Requested from ${mentor.name}'s profile`
        );
      } else {
        const [clock, period] = time.split(' ');
        const [hours, minutes] = clock.split(':').map(Number);
        const start = new Date(`${date}T00:00:00`);
        start.setHours((hours % 12) + (period === 'PM' ? 12 : 0), minutes, 0, 0);
        const end = new Date(start.getTime() + type[2] * 60000);
        result = await app.requestMentorSession(mentor, {
          sessionType: type[0],
          startsAt: start.toISOString(),
          endsAt: end.toISOString(),
          note: `Requested from ${mentor.name}'s profile`,
        });
      }
      app.notify(
        'Mentorship session scheduled',
        `${type[0]} with ${mentor.name} is confirmed on your learning schedule.`
      );
      app.toast(
        result.mode === 'synced'
          ? 'Session booked & confirmed.'
          : 'Session booked in Preview.'
      );
      onClose();
    } catch (issue) {
      setError(issue.message || 'Could not book this session. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="booking-flow">
      <div className="stepper">
        {['Session', 'Date & time', 'Confirm'].map((x, i) => (
          <span
            key={x}
            className={step === i + 1 ? 'active' : step > i + 1 ? 'done' : ''}
          >
            <i>{i + 1}</i>
            {x}
          </span>
        ))}
      </div>

      {step === 1 && (
        <>
          <h3>What would help most?</h3>
          <div className="booking-options">
            {sessionTypes.map((s) => (
              <button
                className={type[0] === s[0] ? 'chosen' : ''}
                key={s[0]}
                onClick={() => setType(s)}
              >
                <b>{s[0]}</b>
                <small>
                  {s[1]} · {s[2]} min
                </small>
              </button>
            ))}
          </div>
          <Button onClick={() => setStep(2)}>Continue</Button>
        </>
      )}

      {step === 2 && (
        <>
          <h3>Choose a time</h3>
          {slots.length > 0 && (
            <div className="mentor-live-slots">
              <h4>Available live calendar slots:</h4>
              <div className="slots-grid">
                {slots
                  .filter((s) => !s.booked)
                  .map((s) => (
                    <button
                      key={s.id}
                      type="button"
                      className={`slot-chip ${
                        selectedSlot?.id === s.id ? 'chosen' : ''
                      }`}
                      onClick={() => setSelectedSlot(s)}
                    >
                      <CalendarDays size={14} />
                      <span>
                        {new Date(s.slotTime).toLocaleDateString([], {
                          month: 'short',
                          day: 'numeric',
                        })}{' '}
                        at{' '}
                        {new Date(s.slotTime).toLocaleTimeString([], {
                          hour: 'numeric',
                          minute: '2-digit',
                        })}{' '}
                        ({s.durationMinutes}m)
                      </span>
                    </button>
                  ))}
              </div>
              {selectedSlot && (
                <p className="slot-selected-hint">
                  Selected: {new Date(selectedSlot.slotTime).toLocaleString()}
                </p>
              )}
            </div>
          )}

          <div className="booking-divider">
            <span>or select standard preference</span>
          </div>

          <div className="booking-options compact">
            {dates.map((d) => (
              <button
                key={d.value}
                className={!selectedSlot && date === d.value ? 'chosen' : ''}
                onClick={() => {
                  setDate(d.value);
                  setSelectedSlot(null);
                }}
              >
                {d.label}
              </button>
            ))}
          </div>

          <div className="booking-options compact">
            {['10:00 AM', '2:00 PM', '7:00 PM', '8:00 PM'].map((t) => (
              <button
                key={t}
                className={!selectedSlot && time === t ? 'chosen' : ''}
                onClick={() => {
                  setTime(t);
                  setSelectedSlot(null);
                }}
              >
                {t}
              </button>
            ))}
          </div>

          <div className="flow-actions">
            <Button variant="ghost" onClick={() => setStep(1)}>
              Back
            </Button>
            <Button onClick={() => setStep(3)}>Review</Button>
          </div>
        </>
      )}

      {step === 3 && (
        <>
          <h3>Ready to request?</h3>
          <div className="confirm-box">
            <b>{type[0]}</b>
            <span>{mentor.name}</span>
            <span>
              {selectedSlot
                ? `${new Date(selectedSlot.slotTime).toLocaleString([], {
                    dateStyle: 'medium',
                    timeStyle: 'short',
                  })} · Reserved calendar slot`
                : `${
                    dates.find((item) => item.value === date)?.label
                  } · ${time} · Online`}
            </span>
            <strong>INR {mentor.rate}</strong>
          </div>
          {error && (
            <p className="auth-error" role="alert">
              {error}
            </p>
          )}
          <div className="flow-actions">
            <Button
              variant="ghost"
              onClick={() => setStep(2)}
              disabled={saving}
            >
              Back
            </Button>
            <Button onClick={finish} disabled={saving}>
              {saving ? 'Booking…' : 'Confirm booking'}
            </Button>
          </div>
        </>
      )}
    </section>
  );
}

// ─── 4. Mentor Mode (Workspace) ──────────────────────────────────────────────

function MentorWorkspace({ app }) {
  const [view, setView] = useState('Dashboard');
  const defaultSessions = [
    {
      id: 'ms1',
      title: '1:1 mentorship with Riya Singh',
      learner: 'Riya Singh',
      date: '28 May · 7:00 PM',
      duration: '60 min',
      meetingLink: 'https://collobster.app/meet/riya-singh',
      status: 'Confirmed',
      notes: 'Focus on Flutter responsive layouts and Riverpod providers.',
    },
    {
      id: 'ms2',
      title: 'Code review with Aman Kumar',
      learner: 'Aman Kumar',
      date: '05 Jun · 6:00 PM',
      duration: '45 min',
      meetingLink: 'https://collobster.app/meet/aman-kumar',
      status: 'Confirmed',
      notes: 'Reviewing pull request for offline data synchronization.',
    },
    {
      id: 'ms3',
      title: 'Career guidance call with Neha Patel',
      learner: 'Neha Patel',
      date: '15 Jun · 5:00 PM',
      duration: '45 min',
      meetingLink: 'https://collobster.app/meet/neha-patel',
      status: 'Completed',
      notes: 'Discussed transitioning to product design & startup hiring.',
    },
  ];
  const [requests, setRequests] = useStoredState('collobster_mentor_requests', initialRequests);
  const [sessions, setSessions] = useStoredState('collobster_mentor_sessions', defaultSessions);
  const [editingSession, setEditingSession] = useState(null);
  const [discussingRequest, setDiscussingRequest] = useState(null);

  const changeRequest = (id, status, modifiedTime = null) => {
    const req = requests.find((r) => r.id === id);
    setRequests((x) =>
      x.map((r) => (r.id === id ? { ...r, status, ...(modifiedTime ? { time: modifiedTime } : {}) } : r))
    );
    if (status === 'Accepted' && req) {
      const newSession = {
        id: `ms-${Date.now()}`,
        title: `${req.topic} with ${req.learner}`,
        learner: req.learner,
        date: modifiedTime || req.time,
        duration: '60 min',
        meetingLink: `https://collobster.app/meet/${req.learner.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`,
        status: 'Confirmed',
        notes: modifiedTime ? `Time modified by discussion: ${modifiedTime}` : 'Accepted from session request.',
      };
      setSessions((prev) => [newSession, ...prev]);
      app.toast(`Request from ${req.learner} accepted and pushed to My sessions!`);
    } else if (status === 'Declined') {
      app.toast('Request declined');
    }
  };

  const handleSaveModifiedSession = (updated) => {
    setSessions((old) => old.map((s) => (s.id === updated.id ? updated : s)));
    setEditingSession(null);
    app.toast('Session details updated');
  };

  return (
    <div className="mentor-layout">
      <section className="mentor-main">
        <Tabs
          items={[
            'Dashboard',
            'My sessions',
            'Calendar',
            'Requests',
            'Learners',
            'Earnings',
            'Profile',
            'Availability',
          ]}
          value={view}
          onChange={setView}
        />

        {view === 'Dashboard' && (
          <>
            <section className="tutor-hero">
              <div>
                <Tag tone="purple">Mentor account</Tag>
                <h2>Good afternoon, Aman.</h2>
                <p>One session is coming up today. Your availability is live.</p>
                <Button icon={CalendarDays} onClick={() => setView('My sessions')}>
                  View my sessions
                </Button>
              </div>
              <div className="earnings-total">
                <span>This month</span>
                <strong>INR 48,750</strong>
                <small>+12.5% from last month</small>
              </div>
            </section>

            <div className="metric-grid mentor-metrics">
              <Metric label="Sessions" value={String(sessions.length)} />
              <Metric label="Learners" value="156" />
              <Metric label="Rating" value="4.8" />
              <Metric label="Response rate" value="98%" />
            </div>

            <SectionTitle
              title="Session requests"
              action="View all"
              onAction={() => setView('Requests')}
            />
            <Requests
              requests={requests.slice(0, 2)}
              onChange={changeRequest}
              onDiscuss={(r) => setDiscussingRequest(r)}
            />
          </>
        )}

        {view === 'My sessions' && (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
              <div>
                <h3 style={{ margin: 0 }}>My Confirmed Sessions</h3>
                <small style={{ color: 'var(--color-muted, #71767b)' }}>
                  Approved requests appear here automatically. You can modify session times and links below.
                </small>
              </div>
              <Button icon={CalendarDays} variant="ghost" onClick={() => setView('Calendar')}>
                Open calendar view
              </Button>
            </div>
            <SessionList
              sessions={sessions}
              onModify={(s) => setEditingSession(s)}
            />
          </div>
        )}

        {view === 'Calendar' && (
          <MentorCalendar
            sessions={sessions}
            onModify={(s) => setEditingSession(s)}
          />
        )}

        {view === 'Requests' && (
          <div>
            <p style={{ margin: '0 0 12px', fontSize: '0.88rem', color: 'var(--color-muted, #71767b)' }}>
              Learner requests can be accepted, declined, or modified in discussion before confirming.
            </p>
            <Requests
              requests={requests}
              onChange={changeRequest}
              onDiscuss={(r) => setDiscussingRequest(r)}
            />
          </div>
        )}

        {view === 'Learners' && <Learners />}
        {view === 'Earnings' && <Earnings />}
        {view === 'Profile' && <MentorEdit app={app} />}
        {view === 'Availability' && <Availability app={app} />}
      </section>

      <aside className="mentor-rail">
        <SectionTitle title="Quick actions" />
        <div className="quick-stack">
          <Button icon={CalendarDays} onClick={() => setView('Calendar')}>
            Check session calendar
          </Button>
          <Button
            variant="ghost"
            icon={MessageCircle}
            onClick={() => app.go('Messages')}
          >
            Open messages
          </Button>
          <Button
            variant="ghost"
            icon={LineChart}
            onClick={() => setView('Earnings')}
          >
            View earnings
          </Button>
        </div>

        <SectionTitle title="Your next session" />
        <div className="rail-card">
          <Tag tone="green">Confirmed</Tag>
          <b>{sessions[0]?.title || '1:1 mentoring session'}</b>
          <span>{sessions[0]?.date || 'Today · 7:00 PM'}</span>
          <Button onClick={() => app.toast('Joining live video room...')}>Join room</Button>
        </div>
      </aside>

      {/* Modify Session Modal */}
      {editingSession && (
        <ModifySessionModal
          session={editingSession}
          onSave={handleSaveModifiedSession}
          onClose={() => setEditingSession(null)}
        />
      )}

      {/* Discuss & Modify Request Modal */}
      {discussingRequest && (
        <DiscussRequestModal
          request={discussingRequest}
          onClose={() => setDiscussingRequest(null)}
          onConfirm={(reqId, newTime, note) => {
            changeRequest(reqId, 'Accepted', newTime);
            setDiscussingRequest(null);
            if (note) {
              app.notify('Session discussion sent', `Message sent to ${discussingRequest.learner}: "${note}"`, 'Mentor');
            }
          }}
        />
      )}
    </div>
  );
}

function MentorCalendar({ sessions, onModify }) {
  const [selectedDay, setSelectedDay] = useState(28);
  const daysInMonth = Array.from({ length: 31 }, (_, i) => i + 1);

  const getSessionDay = (dateStr) => {
    if (!dateStr) return null;
    const match = dateStr.match(/(\d+)/);
    return match ? parseInt(match[1], 10) : null;
  };

  const sessionsForDay = sessions.filter((s) => getSessionDay(s.date) === selectedDay);

  return (
    <section className="mentor-calendar-panel" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h3 style={{ margin: 0 }}>Mentor Schedule Calendar</h3>
          <p style={{ margin: '2px 0 0', fontSize: '0.85rem', color: 'var(--color-muted, #71767b)' }}>
            Select a day to review and modify scheduled sessions.
          </p>
        </div>
        <Tag tone="purple">Month View · May / Jun 2026</Tag>
      </div>

      <div
        className="calendar-month-grid"
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(7, 1fr)',
          gap: 6,
          background: 'var(--color-surface-hover, #f8f9fa)',
          padding: 12,
          borderRadius: 10,
        }}
      >
        {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((d) => (
          <div key={d} style={{ textAlign: 'center', fontSize: '0.75rem', fontWeight: 600, color: 'var(--color-muted, #888)', padding: '4px 0' }}>
            {d}
          </div>
        ))}
        {daysInMonth.map((day) => {
          const hasSessions = sessions.some((s) => getSessionDay(s.date) === day);
          const isSelected = selectedDay === day;
          return (
            <button
              key={day}
              type="button"
              onClick={() => setSelectedDay(day)}
              style={{
                aspectRatio: '1',
                borderRadius: 8,
                border: isSelected ? '2px solid var(--accent, #e34d42)' : '1px solid var(--color-border, #e5e7eb)',
                background: isSelected
                  ? 'var(--accent-tint, rgba(227, 77, 66, 0.12))'
                  : hasSessions
                  ? '#ecfdf5'
                  : 'var(--color-surface, #fff)',
                cursor: 'pointer',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                position: 'relative',
                fontWeight: isSelected || hasSessions ? 600 : 400,
                color: isSelected ? 'var(--accent, #e34d42)' : hasSessions ? '#059669' : 'inherit',
              }}
            >
              <span>{day}</span>
              {hasSessions && (
                <span
                  style={{
                    width: 5,
                    height: 5,
                    borderRadius: '50%',
                    background: '#10b981',
                    marginTop: 2,
                  }}
                />
              )}
            </button>
          );
        })}
      </div>

      <div style={{ marginTop: 8 }}>
        <SectionTitle title={`Sessions for Day ${selectedDay}`} />
        {sessionsForDay.length ? (
          <SessionList sessions={sessionsForDay} onModify={onModify} />
        ) : (
          <div style={{ padding: '24px 16px', textAlign: 'center', background: 'var(--color-surface-hover, #f8f9fa)', borderRadius: 8 }}>
            <p style={{ margin: 0, fontSize: '0.88rem', color: 'var(--color-muted, #71767b)' }}>
              No sessions scheduled for Day {selectedDay}. Click on another date with an indicator dot.
            </p>
          </div>
        )}
      </div>
    </section>
  );
}

function ModifySessionModal({ session, onSave, onClose }) {
  const [title, setTitle] = useState(session.title);
  const [date, setDate] = useState(session.date);
  const [duration, setDuration] = useState(session.duration || '60 min');
  const [meetingLink, setMeetingLink] = useState(session.meetingLink || '');
  const [status, setStatus] = useState(session.status || 'Confirmed');
  const [notes, setNotes] = useState(session.notes || '');

  return (
    <div className="call-modal-overlay" style={{ background: 'rgba(0,0,0,0.5)', zIndex: 1200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ background: 'var(--color-surface, #fff)', borderRadius: 12, padding: 24, width: '100%', maxWidth: 480, boxShadow: '0 12px 30px rgba(0,0,0,0.15)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h3 style={{ margin: 0 }}>Modify Mentorship Session</h3>
          <button type="button" onClick={onClose} style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer' }}>×</button>
        </div>
        <form onSubmit={(e) => {
          e.preventDefault();
          onSave({ ...session, title, date, duration, meetingLink, status, notes });
        }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.85rem' }}>
              <span>Session Topic / Title</span>
              <input value={title} onChange={(e) => setTitle(e.target.value)} style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }} />
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
              <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.85rem' }}>
                <span>Date & Time</span>
                <input value={date} onChange={(e) => setDate(e.target.value)} style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }} />
              </label>
              <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.85rem' }}>
                <span>Duration</span>
                <select value={duration} onChange={(e) => setDuration(e.target.value)} style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }}>
                  <option>30 min</option>
                  <option>45 min</option>
                  <option>60 min</option>
                  <option>90 min</option>
                </select>
              </label>
            </div>
            <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.85rem' }}>
              <span>Meeting Room Link</span>
              <input value={meetingLink} onChange={(e) => setMeetingLink(e.target.value)} placeholder="https://..." style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }} />
            </label>
            <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.85rem' }}>
              <span>Status</span>
              <select value={status} onChange={(e) => setStatus(e.target.value)} style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }}>
                <option>Confirmed</option>
                <option>Rescheduled</option>
                <option>Completed</option>
                <option>Cancelled</option>
              </select>
            </label>
            <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.85rem' }}>
              <span>Session Agenda / Notes</span>
              <textarea rows="2" value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Agreed discussion points..." style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }} />
            </label>
          </div>
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 18 }}>
            <Button variant="quiet" type="button" onClick={onClose}>Cancel</Button>
            <Button type="submit">Save changes</Button>
          </div>
        </form>
      </div>
    </div>
  );
}

function DiscussRequestModal({ request, onConfirm, onClose }) {
  const [proposedTime, setProposedTime] = useState(request.time);
  const [message, setMessage] = useState('');

  return (
    <div className="call-modal-overlay" style={{ background: 'rgba(0,0,0,0.5)', zIndex: 1200, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ background: 'var(--color-surface, #fff)', borderRadius: 12, padding: 24, width: '100%', maxWidth: 480, boxShadow: '0 12px 30px rgba(0,0,0,0.15)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <h3 style={{ margin: 0 }}>Discuss & Modify Session with {request.learner}</h3>
          <button type="button" onClick={onClose} style={{ background: 'none', border: 'none', fontSize: '1.2rem', cursor: 'pointer' }}>×</button>
        </div>
        <p style={{ margin: '0 0 14px', fontSize: '0.88rem', color: 'var(--color-muted, #71767b)' }}>
          Requested topic: <b>{request.topic}</b> · Original slot: <b>{request.time}</b>
        </p>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.85rem' }}>
            <span>Propose New Date / Time</span>
            <input
              value={proposedTime}
              onChange={(e) => setProposedTime(e.target.value)}
              placeholder="e.g. 29 May · 6:00 PM"
              style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }}
            />
          </label>
          <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.85rem' }}>
            <span>Discussion Note for {request.learner}</span>
            <textarea
              rows="3"
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="e.g. Hi! 7 PM is tight for me, could we do 6 PM instead? Let me know if that works!"
              style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }}
            />
          </label>
        </div>
        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 18 }}>
          <Button variant="quiet" type="button" onClick={onClose}>Cancel</Button>
          <Button onClick={() => onConfirm(request.id, proposedTime, message)}>
            Confirm with Modified Time
          </Button>
        </div>
      </div>
    </div>
  );
}

function Requests({ requests, onChange, onDiscuss }) {
  return (
    <section className="session-list">
      {requests.map((r) => (
        <article key={r.id} className="request-card" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Avatar
              person={
                people.find((p) => r.learner.includes(p.name.split(' ')[0])) ||
                people[0]
              }
            />
            <div>
              <b>{r.learner}</b>
              <small style={{ display: 'block', color: 'var(--color-muted, #71767b)' }}>
                {r.topic} · {r.time}
              </small>
            </div>
          </div>
          {r.status === 'Pending' ? (
            <div className="inline-actions" style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              {onDiscuss && (
                <Button
                  variant="ghost"
                  onClick={() => onDiscuss(r)}
                  style={{ fontSize: '0.82rem' }}
                >
                  Discuss & Modify
                </Button>
              )}
              <Button
                variant="ghost"
                onClick={() => onChange(r.id, 'Declined')}
              >
                Decline
              </Button>
              <Button onClick={() => onChange(r.id, 'Accepted')}>Accept</Button>
            </div>
          ) : (
            <StatusPill status={r.status} />
          )}
        </article>
      ))}
    </section>
  );
}

function Learners() {
  return (
    <section className="session-list">
      {people.map((p, i) => (
        <article className="session-row" key={p.id}>
          <Avatar person={p} />
          <div>
            <b>{p.name}</b>
            <small>
              {i + 2} sessions ·{' '}
              {[
                'Flutter developer',
                'Product designer',
                'Founder',
                'Career switcher',
              ][i]}
            </small>
          </div>
          <Button variant="ghost" icon={MessageCircle}>
            Message
          </Button>
        </article>
      ))}
    </section>
  );
}

function Earnings() {
  return (
    <>
      <section className="metric-grid mentor-metrics">
        <Metric label="Total earnings" value="INR 2,48,750" />
        <Metric label="This month" value="INR 48,750" />
        <Metric label="Payouts" value="INR 75,000" />
        <Metric label="Pending" value="INR 12,400" />
      </section>
      <section className="chart-card">
        <SectionTitle title="Earnings overview" />
        <div className="bar-chart">
          {[35, 48, 41, 63, 54, 72, 86].map((h, i) => (
            <i key={i} style={{ height: `${h}%` }} />
          ))}
        </div>
        <p>
          Figures are example workspace data. Payouts remain local in this prototype.
        </p>
      </section>
    </>
  );
}

function MentorEdit({ app }) {
  const [name, setName] = useState(app?.profile?.name || 'Aman Verma');
  const [headline, setHeadline] = useState(app?.profile?.headline || 'Senior Flutter developer · Orbit');
  const [about, setAbout] = useState(app?.profile?.bio || 'Practical mentorship for people building with Flutter and Dart.');
  const [rate, setRate] = useState('1200');
  const [skills, setSkills] = useState(['Flutter', 'Dart', 'Firebase']);
  const [newSkill, setNewSkill] = useState('');
  const [showAddSkill, setShowAddSkill] = useState(false);

  const handleAddSkill = (e) => {
    e?.preventDefault();
    const trimmed = newSkill.trim();
    if (trimmed && !skills.includes(trimmed)) {
      setSkills((prev) => [...prev, trimmed]);
      setNewSkill('');
      setShowAddSkill(false);
      app?.toast(`Added "${trimmed}" to expertise`);
    }
  };

  const handleSaveProfile = () => {
    if (app?.saveProfile) {
      app.saveProfile({
        name,
        headline,
        bio: about,
        skills,
      });
    } else if (app?.setProfile) {
      app.setProfile((prev) => ({
        ...prev,
        name,
        headline,
        bio: about,
      }));
    }
    app?.toast('Mentor profile saved successfully!');
  };

  return (
    <section className="content-editor">
      <Field label="Public name">
        <input value={name} onChange={(e) => setName(e.target.value)} />
      </Field>
      <Field label="Headline">
        <input value={headline} onChange={(e) => setHeadline(e.target.value)} />
      </Field>
      <Field label="About">
        <textarea value={about} onChange={(e) => setAbout(e.target.value)} />
      </Field>
      <Field label="Hourly rate (INR)">
        <input value={rate} onChange={(e) => setRate(e.target.value)} type="number" />
      </Field>
      <div className="field-label" style={{ fontWeight: 600, fontSize: '0.88rem', marginBottom: 4 }}>
        Expertise & Skills
      </div>
      <div className="tag-row" style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center' }}>
        {skills.map((s) => (
          <Tag key={s}>
            {s}{' '}
            <button
              type="button"
              onClick={() => setSkills((x) => x.filter((k) => k !== s))}
              style={{ background: 'none', border: 'none', cursor: 'pointer', marginLeft: 4, fontWeight: 700 }}
              aria-label={`Remove ${s}`}
            >
              ×
            </button>
          </Tag>
        ))}
        {showAddSkill ? (
          <form onSubmit={handleAddSkill} style={{ display: 'inline-flex', gap: 4, alignItems: 'center' }}>
            <input
              type="text"
              autoFocus
              value={newSkill}
              onChange={(e) => setNewSkill(e.target.value)}
              placeholder="e.g. Next.js"
              style={{ padding: '4px 8px', borderRadius: 4, border: '1px solid var(--color-border, #ccc)', fontSize: '0.85rem' }}
            />
            <Button size="small" type="submit">Add</Button>
            <Button size="small" variant="quiet" type="button" onClick={() => setShowAddSkill(false)}>Cancel</Button>
          </form>
        ) : (
          <button type="button" className="add-chip" onClick={() => setShowAddSkill(true)}>
            + Add expertise
          </button>
        )}
      </div>
      <div style={{ marginTop: 20 }}>
        <Button variant="primary" onClick={handleSaveProfile}>Save profile</Button>
      </div>
    </section>
  );
}

function Availability({ app }) {
  const [weeklyStatus, setWeeklyStatus] = useState({
    Mon: 'Available',
    Tue: 'Available',
    Wed: 'Available',
    Thu: 'Available',
    Fri: 'Available',
    Sat: 'Not available',
    Sun: 'Not available',
  });
  const [slotTime, setSlotTime] = useState('');
  const [duration, setDuration] = useState(30);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const myMentorId = app?.auth?.profileId || 'me';
  const mySlots = app?.mentorSlots?.[myMentorId] || app?.mentorSlots?.me || [];

  const addSlot = async (e) => {
    e.preventDefault();
    if (!slotTime) {
      setError('Please select date and time for the slot.');
      return;
    }
    const iso = new Date(slotTime).toISOString();
    // Allow saving any valid date selected by user (e.g. 27-09-2026 or current/past records)
    setSaving(true);
    setError('');
    try {
      if (app.createMentorSlot) {
        await app.createMentorSlot(iso, Number(duration));
      } else if (app.setMentorSlots) {
        const newSlot = {
          id: `slot-${Date.now()}`,
          slotTime: iso,
          durationMinutes: Number(duration),
          booked: false,
        };
        app.setMentorSlots((prev) => ({
          ...prev,
          [myMentorId]: [newSlot, ...(prev[myMentorId] || [])],
        }));
      }
      app.toast('Availability slot added to your calendar');
      setSlotTime('');
    } catch (err) {
      setError(err.message || 'Could not create availability slot');
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="availability-section">
      <SectionTitle title="Weekly schedule template" />
      <p style={{ margin: '0 0 14px', fontSize: '0.85rem', color: 'var(--color-muted, #71767b)' }}>
        Configure your standard recurring availability for each day of the week:
      </p>
      <div className="availability-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))', gap: 10 }}>
        {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((d) => {
          const isAvail = weeklyStatus[d] === 'Available';
          return (
            <div
              key={d}
              style={{
                display: 'flex',
                flexDirection: 'column',
                gap: 8,
                padding: '12px 14px',
                borderRadius: 10,
                border: isAvail ? '1.5px solid var(--accent, #e34d42)' : '1px solid var(--color-border, #ddd)',
                background: isAvail ? 'var(--color-surface, #fff)' : 'var(--color-surface-hover, #f8f9fa)',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <b style={{ fontSize: '0.95rem' }}>{d}</b>
                <span
                  style={{
                    width: 8,
                    height: 8,
                    borderRadius: '50%',
                    background: isAvail ? '#10b981' : '#9ca3af',
                  }}
                />
              </div>
              <select
                value={weeklyStatus[d]}
                onChange={(e) => {
                  const val = e.target.value;
                  setWeeklyStatus((prev) => ({ ...prev, [d]: val }));
                  app.toast(`${d} set to: ${val}`);
                }}
                style={{
                  padding: '6px 8px',
                  borderRadius: 6,
                  border: '1px solid var(--color-border, #ccc)',
                  background: 'var(--color-surface, #fff)',
                  color: isAvail ? '#059669' : 'var(--color-muted, #71767b)',
                  fontSize: '0.82rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                <option value="Available">Available</option>
                <option value="Not available">Not available</option>
              </select>
              <small style={{ fontSize: '0.74rem', color: 'var(--color-muted, #71767b)' }}>
                {isAvail ? '10:00 AM – 7:00 PM' : 'Off'}
              </small>
            </div>
          );
        })}
      </div>

      <div className="slot-creator-card">
        <SectionTitle title="Add specific calendar slot" />
        <p className="muted">
          Create bookable 1:1 consultation slots that learners can reserve directly.
        </p>
        <form onSubmit={addSlot} className="slot-creator-form">
          <div className="field-group">
            <label>Date and start time</label>
            <input
              type="datetime-local"
              value={slotTime}
              onChange={(e) => setSlotTime(e.target.value)}
              className="input-datetime"
              required
            />
          </div>
          <div className="field-group">
            <label>Duration</label>
            <select
              value={duration}
              onChange={(e) => setDuration(Number(e.target.value))}
            >
              <option value={30}>30 minutes</option>
              <option value={45}>45 minutes</option>
              <option value={60}>60 minutes</option>
            </select>
          </div>
          {error && (
            <p className="auth-error" role="alert">
              {error}
            </p>
          )}
          <Button variant="primary" type="submit" disabled={saving}>
            {saving ? 'Adding slot…' : 'Publish availability slot'}
          </Button>
        </form>
      </div>

      <SectionTitle title="Your scheduled slots" />
      <div className="slots-list">
        {mySlots.length ? (
          mySlots.map((s) => (
            <article key={s.id} className="scheduled-slot-row">
              <CalendarDays size={18} />
              <div>
                <strong>
                  {new Date(s.slotTime).toLocaleDateString([], {
                    weekday: 'short',
                    month: 'short',
                    day: 'numeric',
                    year: 'numeric',
                  })}{' '}
                  at{' '}
                  {new Date(s.slotTime).toLocaleTimeString([], {
                    hour: 'numeric',
                    minute: '2-digit',
                  })}
                </strong>
                <small>{s.durationMinutes} minutes duration</small>
              </div>
              <span
                className={`status-pill status-${s.booked ? 'booked' : 'open'}`}
              >
                {s.booked ? 'Booked' : 'Available'}
              </span>
            </article>
          ))
        ) : (
          <p className="muted">
            No specific slots published yet. Add a slot above so learners can book
            with you.
          </p>
        )}
      </div>
    </section>
  );
}

// ─── 5. Tutor Studio Mode ────────────────────────────────────────────────────

function TutorStudio({ app }) {
  const [view, setView] = useState('Dashboard');
  const [content, setContent] = useState(initialContent);

  return (
    <div className="mentor-layout">
      <section className="mentor-main">
        <Tabs
          items={['Dashboard', 'Create content', 'My content', 'Analytics']}
          value={view}
          onChange={setView}
        />

        {view === 'Dashboard' && (
          <>
            <section className="tutor-hero">
              <div>
                <Tag tone="purple">Tutor studio</Tag>
                <h2>Build a course your learners can use.</h2>
                <p>
                  Keep your lessons, live sessions, resources, and assessment work
                  together.
                </p>
                <Button icon={Plus} onClick={() => setView('Create content')}>
                  Create content
                </Button>
              </div>
              <div className="earnings-total">
                <span>Total course learners</span>
                <strong>1.2k</strong>
                <small>96 published pieces</small>
              </div>
            </section>

            <div className="metric-grid mentor-metrics">
              <Metric label="Courses" value="12" />
              <Metric label="Learners" value="1.2k" />
              <Metric label="Lessons" value="96" />
              <Metric label="Rating" value="4.8" />
            </div>

            <SectionTitle
              title="Recent content"
              action="Manage"
              onAction={() => setView('My content')}
            />
            <ContentList content={content.slice(0, 3)} />
          </>
        )}

        {view === 'Create content' && (
          <ContentCreator
            app={app}
            onPublish={(item) => {
              setContent((x) => [item, ...x]);
              setView('My content');
            }}
          />
        )}

        {view === 'My content' && <ContentList content={content} />}

        {view === 'Analytics' && <TutorAnalytics content={content} />}
      </section>

      <aside className="mentor-rail">
        <SectionTitle title="Your courses" />
        <div className="rail-list">
          {courses.map((c) => (
            <button
              key={c.id}
              onClick={() => app.setModal({ kind: 'course', item: c })}
            >
              <span className={`mini-mark ${c.color}`}>{c.symbol}</span>
              <span>
                <b>{c.title}</b>
                <small>
                  {c.lessons.length} lessons · {c.level}
                </small>
              </span>
              <ChevronRight size={16} />
            </button>
          ))}
        </div>

        <SectionTitle title="Next live class" />
        <div className="rail-card">
          <Tag tone="red">Live</Tag>
          <b>State management clinic</b>
          <span>24 May · 7:00 PM</span>
          <Button variant="ghost">Open class room</Button>
        </div>
      </aside>
    </div>
  );
}

function ContentCreator({ app, onPublish }) {
  const [type, setType] = useState(null);
  const [step, setStep] = useState(1);
  const [title, setTitle] = useState('');
  const [desc, setDesc] = useState('');
  const [course, setCourse] = useState('Flutter Development Bootcamp');
  const [selectedFile, setSelectedFile] = useState(null);
  const [errorMsg, setErrorMsg] = useState('');
  const fileInputRef = React.useRef(null);

  if (!type) {
    return (
      <section className="content-types">
        {contentTypes.map(([name, desc, Icon]) => (
          <button key={name} onClick={() => setType([name, desc, Icon])}>
            <span>
              <Icon size={21} />
            </span>
            <div>
              <b>{name}</b>
              <small>{desc}</small>
            </div>
            <ChevronRight size={17} />
          </button>
        ))}
      </section>
    );
  }

  const Icon = type[2];

  const publish = () => {
    if (!title.trim()) {
      setErrorMsg('Please provide a title for this content.');
      setStep(1);
      return;
    }
    if (!selectedFile) {
      setErrorMsg('Please choose a file to upload learning material before publishing.');
      setStep(2);
      return;
    }
    const item = {
      id: `content-${Date.now()}`,
      type: type[0],
      title: title.trim(),
      fileName: selectedFile.name,
      course,
      status: 'Published',
      views: '0 views',
      date: 'Today',
    };
    onPublish(item);
    app.notify(
      'Content published',
      `"${item.title}" (${selectedFile.name}) is now live in your tutor studio.`
    );
    app.toast('Content published successfully!');
  };

  return (
    <section className="content-editor">
      <div className="stepper">
        {['Basic info', 'Content', 'Settings'].map((x, i) => (
          <span
            key={x}
            className={step === i + 1 ? 'active' : step > i + 1 ? 'done' : ''}
          >
            <i>{i + 1}</i>
            {x}
          </span>
        ))}
      </div>

      <div className="content-kind">
        <Icon size={20} />
        {type[0]}
        <button
          onClick={() => {
            setType(null);
            setStep(1);
            setSelectedFile(null);
            setErrorMsg('');
          }}
        >
          Change
        </button>
      </div>

      {errorMsg && (
        <div style={{ padding: '8px 12px', background: 'rgba(239, 68, 68, 0.1)', color: '#dc2626', borderRadius: 8, fontSize: '0.85rem', marginBottom: 12 }}>
          {errorMsg}
        </div>
      )}

      {step === 1 && (
        <>
          <Field label="Title">
            <input
              value={title}
              onChange={(e) => {
                setTitle(e.target.value);
                setErrorMsg('');
              }}
              placeholder={`e.g. Introduction to ${type[0]}`}
              required
            />
          </Field>
          <Field label="Description">
            <textarea
              value={desc}
              onChange={(e) => setDesc(e.target.value)}
              placeholder="What will learners take away?"
            />
          </Field>
          <Field label="Course">
            <select value={course} onChange={(e) => setCourse(e.target.value)}>
              <option value="Flutter Development Bootcamp">Flutter Development Bootcamp</option>
              <option value="Design systems that scale">Design systems that scale</option>
            </select>
          </Field>
          <Button
            onClick={() => {
              if (!title.trim()) {
                setErrorMsg('Please enter a content title to continue.');
                return;
              }
              setErrorMsg('');
              setStep(2);
            }}
          >
            Next: content
          </Button>
        </>
      )}

      {step === 2 && (
        <>
          <div className="upload-well">
            <input
              type="file"
              ref={fileInputRef}
              style={{ display: 'none' }}
              onChange={(e) => {
                if (e.target.files && e.target.files[0]) {
                  setSelectedFile(e.target.files[0]);
                  setErrorMsg('');
                }
              }}
            />
            <Plus size={28} />
            <b>
              {type[0] === 'Video lesson'
                ? 'Upload a video'
                : 'Add your learning material'}
            </b>
            <small>
              {selectedFile ? (
                <span style={{ color: '#059669', fontWeight: 600 }}>
                  Selected: {selectedFile.name} ({(selectedFile.size / 1024).toFixed(1)} KB)
                </span>
              ) : (
                'Choose a file to attach to this learning item.'
              )}
            </small>
            <Button
              variant="ghost"
              type="button"
              onClick={() => fileInputRef.current?.click()}
            >
              {selectedFile ? 'Change file' : 'Choose file'}
            </Button>
          </div>
          <Field label="Notes for learners">
            <textarea placeholder="Add context, links, or instructions." />
          </Field>
          <div className="flow-actions">
            <Button variant="ghost" onClick={() => setStep(1)}>
              Back
            </Button>
            <Button
              onClick={() => {
                if (!selectedFile) {
                  setErrorMsg('Please choose a file for your content before proceeding.');
                  return;
                }
                setErrorMsg('');
                setStep(3);
              }}
            >
              Next: settings
            </Button>
          </div>
        </>
      )}

      {step === 3 && (
        <>
          <Field label="Visibility">
            <select>
              <option>Public</option>
              <option>Private course</option>
              <option>Unlisted</option>
            </select>
          </Field>
          <Field label="Tags">
            <input defaultValue="Flutter, Dart, beginner" />
          </Field>
          <div className="flow-actions">
            <Button variant="ghost" onClick={() => setStep(2)}>
              Back
            </Button>
            <Button onClick={publish}>Publish {type[0]}</Button>
          </div>
        </>
      )}
    </section>
  );
}

function ContentList({ content }) {
  return (
    <section className="content-list">
      {content.map((c) => (
        <article key={c.id}>
          <span className="date-chip">
            <FileText size={17} />
          </span>
          <div>
            <b>{c.title}</b>
            <small>
              {c.type} · {c.course} · {c.fileName ? `File: ${c.fileName} · ` : ''}{c.views}
            </small>
          </div>
          <StatusPill status={c.status} />
          <button className="more-button" aria-label="More options">
            ···
          </button>
        </article>
      ))}
    </section>
  );
}

function TutorAnalytics({ content }) {
  const publishedCount = content.filter((c) => c.status === 'Published').length;
  const scheduledCount = content.filter((c) => c.status === 'Scheduled').length;
  const draftCount = content.filter((c) => c.status === 'Draft').length;
  const totalCount = content.length;

  return (
    <>
      <section className="metric-grid mentor-metrics">
        <Metric label="Total lessons" value={String(totalCount)} />
        <Metric label="Published" value={String(publishedCount)} />
        <Metric label="Scheduled" value={String(scheduledCount)} />
        <Metric label="Drafts" value={String(draftCount)} />
      </section>
      <section className="chart-card">
        <SectionTitle title="Learning engagement & activity" />
        <div className="bar-chart purple">
          {content.map((c, i) => {
            const h = c.status === 'Published' ? 85 : c.status === 'Scheduled' ? 50 : 25;
            return <i key={c.id || i} style={{ height: `${h}%` }} title={`${c.title}: ${c.status}`} />;
          })}
        </div>
        <p style={{ marginTop: 8, fontSize: '0.82rem', color: 'var(--color-muted, #71767b)' }}>
          Real-time metrics computed dynamically across {totalCount} tutor pieces ({publishedCount} published, {scheduledCount} scheduled, {draftCount} in drafts).
        </p>
      </section>
      <section className="content-list">
        <h3>Live content breakdown</h3>
        {content.map((c) => (
          <article key={c.id}>
            <span className="date-chip">
              <Play size={16} />
            </span>
            <div>
              <b>{c.title}</b>
              <small>{c.type} · {c.course} · {c.fileName ? `Attached: ${c.fileName}` : c.views}</small>
            </div>
            <StatusPill status={c.status} />
          </article>
        ))}
      </section>
    </>
  );
}
