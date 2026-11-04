import React, { useMemo, useState, useEffect } from 'react';
import {
  BookOpen,
  Play,
  Clock3,
  CheckCircle2,
  BriefcaseBusiness,
  MapPin,
  Bookmark,
  FolderKanban,
  Users,
  Plus,
  ArrowRight,
  Building2,
  ChartNoAxesCombined,
  CalendarDays,
  Ticket,
  MapPinned,
  Star,
  MessageCircle,
  Search,
  Filter,
  ShieldCheck,
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  Wrench,
  Zap,
  Home,
  Truck,
  PhoneCall,
  Scale,
  LineChart,
  Palette,
  Settings,
  Edit3,
  Globe2,
  Heart,
  HeartHandshake,
  ReceiptText,
  TrendingUp,
  Bell,
  FileText,
  ClipboardList,
  Code2,
  PanelsTopLeft,
  Puzzle,
  Terminal,
  GitBranch,
  PackageOpen,
  LogOut,
  Upload,
  Download,
  Archive,
  Trash2,
  CheckSquare,
  QrCode,
  Video,
  Award,
  Sparkles,
  Share2,
  Camera,
  Check,
  Copy,
  ExternalLink,
  MessageSquare,
  Hash,
  UserPlus,
  UserMinus,
  Flame,
  Smile,
  BarChart3,
  HelpCircle,
  Lock,
  Radio,
  X,
  Paperclip,
  Image,
  File,
  Folder,
  Crown,
  Eye,
  Pin,
  Reply,
  Forward,
  ArrowLeft,
  MoreVertical,
  Compass,
} from 'lucide-react';
import { useApp } from '../context';
import {
  courses,
  jobs,
  projects,
  companies,
  events,
  communities,
  services,
  people,
  photos,
} from '../data';
import { matchOpportunity, joinState, searchItems } from '../domain.mjs';
import {
  Avatar,
  Button,
  IconButton,
  Tag,
  Tabs,
  PageHeader,
  SectionTitle,
  SearchField,
  Progress,
  DetailRow,
  Media,
  Empty,
  Status,
  Dialog,
  Field,
} from '../components';
import './company-team.css';
import './company-flow.css';
import { CompanyRedesign } from './CompanyRedesign';
import { downloadTicketPdf } from './Dialogs';
import { useStoredState } from '../store';
import './learn-redesign.css';
import {
  defaultLearningPaths,
  defaultLiveClasses,
  defaultStudyGroups,
  defaultPracticeLabs,
  defaultArticles,
} from './learnData';
import './services-redesign.css';
import {
  serviceCategories,
  universalServiceCatalog,
  initialServiceRequests,
} from './servicesData';

// ─── Shared UI Primitives ───────────────────────────────────────────────────

const colors = ['rose', 'blue', 'green', 'gold'];

function ModuleHero({
  icon: Icon,
  eyebrow,
  title,
  text,
  action,
  children,
  art = 'mascot',
}) {
  return (
    <section className="module-hero">
      <div className="hero-copy">
        <span className="eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
        <p>{text}</p>
        {action}
      </div>
      {art === 'mascot' ? (
        <img src="/collobster-mascot.png" alt="Collobster mascot" />
      ) : (
        <Icon size={90} />
      )}{' '}
      {children}
    </section>
  );
}

function ItemCard({ item, kind, onClick, action = 'View details', children }) {
  const color = item.color || 'rose';
  return (
    <article className="item-card">
      <button className="item-card-main" onClick={onClick}>
        <span className={`module-icon ${color}`}>
          {kind === 'course' ? (
            <BookOpen size={21} />
          ) : kind === 'job' ? (
            <BriefcaseBusiness size={21} />
          ) : kind === 'project' ? (
            <FolderKanban size={21} />
          ) : kind === 'company' ? (
            <Building2 size={21} />
          ) : kind === 'community' ? (
            <Users size={21} />
          ) : kind === 'service' ? (
            <Star size={21} />
          ) : (
            <CalendarDays size={21} />
          )}
        </span>
        <span className="item-card-text">
          <strong>{item.title}</strong>
          <small>
            {item.company ||
              item.mentor ||
              item.provider ||
              item.category ||
              item.location}
          </small>
          {item.description && <em>{item.description}</em>}
          {kind === 'service' && (
            <span className="service-card-meta">
              <Tag tone="purple">
                <MapPin size={11} /> {item.serviceRadiusKm || 15} km radius
              </Tag>
              <Tag tone="gold">
                <Star size={11} />{' '}
                {item.averageRating
                  ? Number(item.averageRating).toFixed(1)
                  : '5.0'}{' '}
                ({item.reviewCount || 0})
              </Tag>
            </span>
          )}
        </span>
        <ChevronRight size={18} />
      </button>
      {children || <Button onClick={onClick}>{action}</Button>}
    </article>
  );
}

function ExploreCard({ item, kind, onOpen, action }) {
  const icons = {
    person: Users,
    project: FolderKanban,
    company: Building2,
    community: Users,
    job: BriefcaseBusiness,
    event: CalendarDays,
    service: Star,
  };
  const Icon = icons[kind] || Search;
  const labels = {
    person: 'Person',
    project: 'Project',
    company: 'Company',
    community: 'Community',
    job: 'Opportunity',
    event: 'Event',
    service: 'Local service',
  };
  const art =
    item.image ||
    {
      project: photos.team,
      company: photos.code,
      community: photos.event,
      job: photos.desk,
      event: photos.event,
      service: photos.desk,
    }[kind];

  const detail =
    kind === 'person'
      ? item.role
      : kind === 'job'
      ? `${item.company} · ${item.location}`
      : kind === 'project'
      ? `${item.members} collaborators · ${item.roles}`
      : kind === 'company'
      ? `${item.stage} · ${item.team} people`
      : kind === 'community'
      ? `${item.members} members · ${item.policy}`
      : kind === 'event'
      ? `${item.month} ${item.day} · ${item.mode}`
      : kind === 'service'
      ? `With ${item.provider} · ${item.duration}`
      : item.category;

  const tags = (
    item.tags?.length ? item.tags : [item.category, item.mode || item.location]
  )
    .filter(Boolean)
    .slice(0, 3);

  return (
    <article className={`explore-card ${kind}`}>
      <button
        className="explore-card-main"
        onClick={onOpen}
        aria-label={`Open ${item.title}`}
      >
        <div className="explore-card-art">
          <Media src={art} alt="" />
          <span className={`module-icon ${item.color || 'rose'}`}>
            <Icon size={19} />
          </span>
          <small>{labels[kind]}</small>
        </div>
        <div className="explore-card-content">
          <div className="explore-card-title">
            <div>
              <h3>{item.title}</h3>
              <p>{detail}</p>
            </div>
            <ChevronRight size={18} />
          </div>
          {item.description && kind !== 'person' && (
            <p className="explore-card-description">{item.description}</p>
          )}
          <div className="tag-cloud">
            {tags.map((tag) => (
              <Tag key={tag}>{tag}</Tag>
            ))}
          </div>
        </div>
      </button>
      <div className="explore-card-footer">
        <span>
          {kind === 'job'
            ? item.salary
            : kind === 'service'
            ? `INR ${item.price}`
            : kind === 'event'
            ? `${item.attendees} going`
            : kind === 'person'
            ? 'Open to connect'
            : 'Explore details'}
        </span>
        <Button onClick={onOpen}>{action}</Button>
      </div>
    </article>
  );
}

// ─── 1. Explore Space (Exported) ────────────────────────────────────────────

function CategorySquareCard({ item, kind, onOpen, action }) {
  const icons = {
    person: Users,
    project: FolderKanban,
    company: Building2,
    community: Users,
    job: BriefcaseBusiness,
    event: CalendarDays,
    service: Star,
  };
  const Icon = icons[kind] || Search;

  const detail =
    kind === 'person'
      ? item.role
      : kind === 'job'
      ? `${item.company} · ${item.location}`
      : kind === 'project'
      ? `${item.members} members · ${item.roles}`
      : kind === 'company'
      ? `${item.stage} · ${item.team} people`
      : kind === 'community'
      ? `${item.members} members · ${item.policy}`
      : kind === 'event'
      ? `${item.month} ${item.day} · ${item.mode}`
      : kind === 'service'
      ? `INR ${item.price} · ${item.provider}`
      : item.category;

  return (
    <div
      className="category-square-card"
      onClick={onOpen}
      style={{
        width: '100%',
        minWidth: 0,
        minHeight: 220,
        background: 'var(--color-surface, #fff)',
        border: '1px solid var(--color-border, #e5e7eb)',
        borderRadius: 14,
        padding: 16,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        cursor: 'pointer',
        transition: 'transform 0.2s, box-shadow 0.2s, border-color 0.2s',
        position: 'relative',
        boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
      }}
      onMouseEnter={(e) => {
        e.currentTarget.style.transform = 'translateY(-3px)';
        e.currentTarget.style.boxShadow = '0 8px 20px rgba(0,0,0,0.08)';
        e.currentTarget.style.borderColor = 'var(--accent, #e34d42)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.transform = 'none';
        e.currentTarget.style.boxShadow = '0 2px 8px rgba(0,0,0,0.04)';
        e.currentTarget.style.borderColor = 'var(--color-border, #e5e7eb)';
      }}
    >
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          {kind === 'person' ? (
            <Avatar person={item} size="md" />
          ) : (
            <span
              style={{
                width: 38,
                height: 38,
                borderRadius: 10,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: 'var(--accent-tint, rgba(227,77,66,0.1))',
                color: 'var(--accent, #e34d42)',
              }}
            >
              <Icon size={20} />
            </span>
          )}
          <span style={{ fontSize: '0.72rem', textTransform: 'uppercase', fontWeight: 600, letterSpacing: 0.5, color: 'var(--color-muted, #888)' }}>
            {kind}
          </span>
        </div>
        <h4 style={{ margin: '0 0 6px', fontSize: '1rem', fontWeight: 700, lineHeight: 1.3, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
          {item.title || item.name}
        </h4>
        <p style={{ margin: 0, fontSize: '0.82rem', color: 'var(--color-muted, #71767b)', lineHeight: 1.4, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
          {detail}
        </p>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', paddingTop: 10, marginTop: 12, borderTop: '1px solid var(--color-border-subtle, #f0f0f0)' }}>
        <small style={{ fontWeight: 600, fontSize: '0.78rem', color: 'var(--accent, #e34d42)' }}>
          {kind === 'job' ? item.salary : kind === 'service' ? `₹${item.price}` : kind === 'event' ? `${item.attendees} going` : kind === 'person' ? (item.followers ? `${item.followers} followers` : 'Active member') : 'Available'}
        </small>
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onOpen();
          }}
          style={{
            fontSize: '0.75rem',
            padding: '4px 12px',
            borderRadius: 12,
            border: '1px solid var(--color-border, #ddd)',
            background: 'var(--color-surface-hover, #f8f9fa)',
            cursor: 'pointer',
            fontWeight: 600,
          }}
        >
          {action}
        </button>
      </div>
    </div>
  );
}

export function Explore() {
  const a = useApp();
  const [query, setQuery] = useState(a.search);
  const [tab, setTab] = useState('Everything');

  const lists = {
    Courses: courses,
    People: people,
    Jobs: jobs,
    Companies: companies,
    Projects: projects,
    Communities: communities,
    Events: events,
    Services: services,
  };

  const all = Object.values(lists).flat();
  const result = useMemo(
    () => searchItems(tab === 'Everything' ? all : lists[tab] || all, query),
    [tab, query]
  );

  const getCardProps = (item) => {
    const kind = item.role
      ? 'person'
      : (item.price || item.provider)
      ? 'service'
      : item.duration
      ? 'course'
      : item.company
      ? 'job'
      : item.policy
      ? 'community'
      : item.members !== undefined
      ? 'project'
      : item.team !== undefined || item.stage !== undefined
      ? 'company'
      : 'event';

    const card = item.role
      ? {
          ...item,
          title: item.name,
          description: item.role,
          color: item.color,
        }
      : item;

    const action =
      kind === 'person'
        ? a.following.includes(item.id)
          ? 'Following'
          : 'Follow'
        : 'View details';

    return { card, kind, action };
  };

  const categorySections = [
    { title: 'Courses & Learning Paths', icon: BookOpen, tabName: 'Courses', items: lists.Courses, kind: 'course' },
    { title: 'People & Mentors', icon: Users, tabName: 'People', items: lists.People, kind: 'person' },
    { title: 'Jobs & Opportunities', icon: BriefcaseBusiness, tabName: 'Jobs', items: lists.Jobs, kind: 'job' },
    { title: 'Companies & Startups', icon: Building2, tabName: 'Companies', items: lists.Companies, kind: 'company' },
    { title: 'Projects & Code Spaces', icon: FolderKanban, tabName: 'Projects', items: lists.Projects, kind: 'project' },
    { title: 'Communities', icon: Users, tabName: 'Communities', items: lists.Communities, kind: 'community' },
    { title: 'Live Events & Hackathons', icon: CalendarDays, tabName: 'Events', items: lists.Events, kind: 'event' },
    { title: 'Services & Coaching', icon: Star, tabName: 'Services', items: lists.Services, kind: 'service' },
  ];

  return (
    <>
      <PageHeader
        eyebrow="DISCOVER"
        title="Find people who make your next step lighter."
        description="Search across the places where learning, work, and connection happen."
      />
      <div className="explore-layout">
        <section>
          <SearchField
            value={query}
            onChange={setQuery}
            placeholder="Search people, projects, jobs, communities..."
          />
          <Tabs
            items={[
              'Everything',
              'Courses',
              'People',
              'Jobs',
              'Companies',
              'Projects',
              'Communities',
              'Events',
              'Services',
            ]}
            value={tab}
            onChange={setTab}
          />

          <section
            className="explore-quick-nav"
            aria-label="Browse by intention"
          >
            <div>
              <span className="eyebrow">START WITH YOUR INTENTION</span>
              <h2>Where would you like to make progress?</h2>
            </div>
            <div className="quick-nav-actions">
              {[
                [BookOpen, 'Learn a skill', 'Learn'],
                [BriefcaseBusiness, 'Find work', 'Career'],
                [Code2, 'Build a project', 'Projects'],
                [Users, 'Meet people', 'Community'],
              ].map(([Icon, label, page]) => (
                <button key={page} onClick={() => a.go(page)}>
                  <Icon size={19} />
                  <span>{label}</span>
                  <ArrowRight size={15} />
                </button>
              ))}
            </div>
          </section>

          {tab === 'Everything' && !query ? (
            <div className="category-sections-wrapper" style={{ marginTop: 20 }}>
              {categorySections.map((sec) => (
                <div key={sec.title} className="category-horizontal-section" style={{ marginBottom: 28 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <sec.icon size={18} color="var(--accent, #e34d42)" />
                      <h3 style={{ margin: 0, fontSize: '1.05rem', fontWeight: 700 }}>{sec.title}</h3>
                      <span style={{ fontSize: '0.78rem', background: 'var(--color-surface-hover, #f1f3f5)', padding: '2px 8px', borderRadius: 10, color: 'var(--color-muted, #777)' }}>
                        {sec.items.length}
                      </span>
                    </div>
                    <button
                      type="button"
                      onClick={() => setTab(sec.tabName)}
                      style={{ background: 'none', border: 'none', color: 'var(--accent, #e34d42)', fontWeight: 600, fontSize: '0.82rem', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4 }}
                    >
                      View all <ChevronRight size={14} />
                    </button>
                  </div>
                  <div
                    className="category-cards-grid"
                    style={{
                      display: 'grid',
                      gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))',
                      gap: 16,
                      marginTop: 10,
                    }}
                  >
                    {sec.items.slice(0, 8).map((item, idx) => {
                      const { card, kind, action } = getCardProps(item);
                      const finalKind = sec.kind || kind;
                      return (
                        <CategorySquareCard
                          key={item.id || item.name || idx}
                          item={card}
                          kind={finalKind}
                          action={action}
                          onOpen={() => (finalKind === 'person' ? a.openUserProfile(item) : a.setModal({ type: 'details', kind: finalKind, item }))}
                        />
                      );
                    })}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <>
              <div className="search-count" style={{ margin: '14px 0 10px' }}>
                {result.length} {tab.toLowerCase()} found
                {query && <> matching “{query}”</>}
              </div>

              <div
                className="square-cards-grid fullscreen-category-grid"
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))',
                  gap: 16,
                  marginTop: 14,
                }}
              >
                {result.map((item, i) => {
                  const { card, kind, action } = getCardProps(item);
                  return (
                    <CategorySquareCard
                      key={item.id || item.name || i}
                      item={card}
                      kind={kind}
                      onOpen={() => (kind === 'person' ? a.openUserProfile(item) : a.setModal({ type: 'details', kind, item }))}
                      action={action}
                    />
                  );
                })}
              </div>

              {!result.length && (
                <Empty
                  title="No matches yet"
                  text="Try a broader name, skill, topic, or location."
                />
              )}
            </>
          )}
        </section>
      </div>
    </>
  );
}

// ─── 2. Learning Space (Exported) ───────────────────────────────────────────

export function Learning() {
  const a = useApp();
  const [view, setView] = useState('Home');
  const [query, setQuery] = useState('');
  const [categoryTab, setCategoryTab] = useState('All');
  const [subFilter, setSubFilter] = useState('All types');
  const [selectedArticle, setSelectedArticle] = useState(null);

  const [registeredClasses, setRegisteredClasses] = useStoredState(
    'collobster_registered_live_classes',
    ['Build a realtime Flutter chat']
  );
  const [activePracticeLabs, setActivePracticeLabs] = useStoredState(
    'collobster_active_practice_labs',
    [{ title: 'Build a to-do app with offline cache', level: 'Beginner', tag: 'Flutter', duration: '45 mins', points: 150 }]
  );
  const [readArticles, setReadArticles] = useStoredState(
    'collobster_read_articles',
    []
  );
  const [bookmarkedArticles, setBookmarkedArticles] = useStoredState(
    'collobster_bookmarked_articles',
    []
  );

  const catalog = [
    ...(a.courseCatalog || []),
    ...courses.filter(
      (course) => !(a.courseCatalog || []).some((remote) => remote.id === course.id)
    ),
    {
      id: 'nextjs-mastery',
      title: 'Modern Frontend Architecture with Next.js',
      category: 'Frontend',
      description: 'Server components, streaming SSR, performance budgets, and modern CSS architecture.',
      mentor: 'Riya Sharma',
      duration: '6 lessons · 2.5 hours',
      level: 'Intermediate',
      color: 'rose',
      symbol: 'NX',
      tags: ['Next.js', 'React', 'Frontend'],
      lessons: ['App Router architecture', 'Streaming & Suspense', 'Server Actions', 'Optimizing Core Web Vitals', 'Production Deployment'],
    },
    {
      id: 'ai-agents-python',
      title: 'Building Autonomous AI Agents with Python',
      category: 'AI',
      description: 'Construct LLM agent loops, tool interfaces, memory architectures, and verification guards.',
      mentor: 'Arjun Mehta',
      duration: '7 lessons · 3.5 hours',
      level: 'Advanced',
      color: 'gold',
      symbol: 'AI',
      tags: ['AI', 'Python', 'Agents'],
      lessons: ['Agent loops & prompts', 'Tool calling & sandboxing', 'Short & long-term memory', 'Evaluation & guardrails', 'Deploying production agents'],
    },
    {
      id: 'backend-go-microservices',
      title: 'High-Throughput Microservices in Go',
      category: 'Backend',
      description: 'Build concurrent, low-latency gRPC services with Postgres pooling and Redis caching.',
      mentor: 'Neha Patel',
      duration: '5 lessons · 2 hours',
      level: 'Intermediate',
      color: 'blue',
      symbol: 'GO',
      tags: ['Go', 'Backend', 'Microservices'],
      lessons: ['Go concurrency & channels', 'gRPC & Protobuf APIs', 'Database connection pooling', 'Resilience patterns', 'Zero-downtime shipping'],
    },
    {
      id: 'java-microservices-enterprise',
      title: 'Enterprise Java 21 & Spring Boot 3 Microservices',
      category: 'Backend',
      description: 'Master Modern Java 21, Spring Boot 3, Virtual Threads, Spring Data JPA, Kafka event streams, and production Docker deployment.',
      mentor: 'Neha Patel',
      duration: '8 lessons · 4 hours',
      level: 'Intermediate to Advanced',
      color: 'blue',
      symbol: 'JV',
      tags: ['Java', 'Spring Boot', 'Backend', 'Microservices', 'PostgreSQL'],
      lessons: ['Java 21 Records, Pattern Matching & Virtual Threads', 'Spring Boot 3 Architecture & Dependency Injection', 'Spring Data JPA, Hibernate & PostgreSQL', 'REST APIs, DTOs & Global Exception Handling', 'Spring Security 6 & JWT Token Authentication', 'Asynchronous Messaging with Apache Kafka', 'Testcontainers & Integration Testing', 'Dockerizing & Production Health Metrics'],
    },
  ];

  const completed = Object.values(a.learning).flat().length;

  const open = async (course) => {
    try {
      const result = await a.openCourse(course);
      a.setModal({ type: 'details', kind: 'course', item: result.course });
      if (result.mode === 'synced') {
        a.toast('Course access is ready. Your enrollment is saved.');
      }
    } catch (error) {
      a.toast(error.message || 'Could not open this course. Please try again.');
    }
  };

  const toggleRegisterLiveClass = (title) => {
    if (registeredClasses.includes(title)) {
      setRegisteredClasses((prev) => prev.filter((t) => t !== title));
      a.toast(`Unregistered from "${title}"`);
    } else {
      setRegisteredClasses((prev) => [...prev, title]);
      a.notify('Live class registered', `${title} is on your learning calendar.`, 'Learn');
      a.toast(`Registered for "${title}"! Check your schedule.`);
    }
  };

  const togglePracticeLab = (lab) => {
    const title = lab.title;
    const exists = activePracticeLabs.some((l) => l.title === title);
    if (exists) {
      a.toast(`Lab "${title}" is already in your active queue.`);
    } else {
      setActivePracticeLabs((prev) => [...prev, lab]);
      a.notify('Practice lab started', `${title} is in your lab queue.`, 'Learn');
      a.toast(`Practice lab "${title}" started!`);
    }
  };

  const toggleJoinGroup = (group, idx) => {
    const key = `study-${idx}`;
    const joined = a.memberships[key] === 'member';
    a.setMemberships((old) => ({
      ...old,
      [key]: joined ? undefined : 'member',
    }));
    a.toast(joined ? `Left ${group.title}` : `Joined ${group.title}!`);
  };

  const toggleReadArticle = (articleId) => {
    if (readArticles.includes(articleId)) {
      setReadArticles((prev) => prev.filter((id) => id !== articleId));
      a.toast('Marked article as unread.');
    } else {
      setReadArticles((prev) => [...prev, articleId]);
      a.toast('Article completed! +25 Knowledge Points.');
    }
  };

  const toggleBookmarkArticle = (articleId) => {
    if (bookmarkedArticles.includes(articleId)) {
      setBookmarkedArticles((prev) => prev.filter((id) => id !== articleId));
      a.toast('Removed from saved articles.');
    } else {
      setBookmarkedArticles((prev) => [...prev, articleId]);
      a.toast('Article saved to your bookmarks!');
    }
  };

  return (
    <>
      <PageHeader
        eyebrow="LEARN"
        title="Build skills that turn into real momentum."
        description="Choose a course, learn with people, practise what matters, and keep a record of what you have grown."
      >
        <Button
          variant="primary"
          icon={BookOpen}
          onClick={() => open(catalog[0])}
        >
          Continue learning
        </Button>
      </PageHeader>

      <Tabs
        items={[
          'Home',
          'Explore',
          'Articles',
          'My learning',
          'Live classes',
          'Practice labs',
          'Study groups',
          'Resources',
          'Certificates',
        ]}
        value={view}
        onChange={(v) => {
          setView(v);
          if (v !== 'Articles') {
            setSelectedArticle(null);
          }
        }}
      />

      {view === 'Home' && (
        <div className="learn-home">
          <section>
            {/* Top Learning Hero */}
            <div className="learn-hero" style={{ marginBottom: 24 }}>
              <div>
                <span className="eyebrow">YOUR LEARNING SPACE</span>
                <h2>One thoughtful hour can change the week.</h2>
                <p>
                  Continue your path, join a live cohort, or ship something small
                  with an interactive practice lab.
                </p>
                <Button onClick={() => open(catalog[0])}>
                  Resume learning path <ArrowRight size={16} />
                </Button>
              </div>
              <div className="learn-streak">
                <b>{Math.max(3, completed)}</b>
                <span>day streak</span>
                <small>M T W T F S S</small>
              </div>
            </div>

            {/* Quick Skills & Language Discover Strip */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', marginBottom: 20 }}>
              <span style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--muted)', textTransform: 'uppercase', letterSpacing: 0.5 }}>
                Featured Tracks:
              </span>
              {[
                ['☕ Java 21 & Spring Boot', 'Java'],
                ['📱 Flutter & Dart', 'Mobile'],
                ['⚛️ React & Next.js', 'Frontend'],
                ['🤖 AI & Autonomous Agents', 'AI'],
                ['🐹 Go & Cloud Microservices', 'Backend'],
                ['🎨 Design Systems & Figma', 'Design'],
              ].map(([label, catKey]) => (
                <button
                  key={catKey}
                  type="button"
                  onClick={() => {
                    setCategoryTab(catKey);
                    setView('Explore');
                  }}
                  style={{
                    background: 'var(--surface)',
                    border: '1px solid var(--line)',
                    padding: '6px 14px',
                    borderRadius: '20px',
                    fontSize: '0.82rem',
                    fontWeight: 600,
                    cursor: 'pointer',
                    color: 'var(--ink)',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: 4,
                  }}
                >
                  {label}
                </button>
              ))}
            </div>

            {/* 1. Recommended Horizontal Cards Section */}
            <LearnHorizontalSection
              title="Recommended for you"
              count={catalog.length}
              actionLabel="View all"
              onAction={() => {
                setSubFilter('Courses');
                setView('Explore');
              }}
              icon={Sparkles}
            >
              {catalog.map((course) => {
                const progress = a.learning[course.id]?.length || 0;
                return (
                  <CourseHCard
                    key={course.id}
                    course={course}
                    open={open}
                    progress={progress}
                  />
                );
              })}
            </LearnHorizontalSection>

            {/* 2. Live Classes Horizontal Cards Section */}
            <LearnHorizontalSection
              title="Live classes"
              badge="Upcoming"
              actionLabel="View all"
              onAction={() => setView('Live classes')}
              icon={CalendarDays}
            >
              {defaultLiveClasses.map((cls) => (
                <LiveClassHCard
                  key={cls.id}
                  liveClass={cls}
                  isRegistered={registeredClasses.includes(cls.title)}
                  onToggleRegister={toggleRegisterLiveClass}
                />
              ))}
            </LearnHorizontalSection>

            {/* 3. Learning Paths Horizontal Cards Section */}
            <LearnHorizontalSection
              title="Learning paths"
              count={defaultLearningPaths.length}
              actionLabel="View all"
              onAction={() => {
                setSubFilter('Learning paths');
                setView('Explore');
              }}
              icon={Compass}
            >
              {defaultLearningPaths.map((path) => (
                <LearningPathHCard
                  key={path.id}
                  path={path}
                  onExplore={() => {
                    setQuery(path.title);
                    setSubFilter('Learning paths');
                    setView('Explore');
                  }}
                />
              ))}
            </LearnHorizontalSection>

            {/* 4. Study Groups Horizontal Cards Section */}
            <LearnHorizontalSection
              title="Study groups"
              count={defaultStudyGroups.length}
              actionLabel="View all"
              onAction={() => setView('Study groups')}
              icon={Users}
            >
              {defaultStudyGroups.map((group, idx) => {
                const isJoined = a.memberships[`study-${idx}`] === 'member';
                return (
                  <StudyGroupHCard
                    key={group.id}
                    group={group}
                    isJoined={isJoined}
                    onToggleJoin={() => toggleJoinGroup(group, idx)}
                  />
                );
              })}
            </LearnHorizontalSection>

            {/* 5. Practice Labs Horizontal Cards Section */}
            <LearnHorizontalSection
              title="Practice labs"
              count={defaultPracticeLabs.length}
              actionLabel="View all"
              onAction={() => setView('Practice labs')}
              icon={Terminal}
            >
              {defaultPracticeLabs.map((lab) => {
                const isActive = activePracticeLabs.some((l) => l.title === lab.title);
                return (
                  <PracticeLabHCard
                    key={lab.id}
                    lab={lab}
                    isActive={isActive}
                    onToggleLab={() => togglePracticeLab(lab)}
                  />
                );
              })}
            </LearnHorizontalSection>
          </section>

          <aside className="space-rail">
            <SectionTitle title="Your progress" />
            <div className="stats-row vertical">
              <div>
                <b>{completed}</b>
                <span>Lessons complete</span>
              </div>
              <div>
                <b>2h 20m</b>
                <span>Learning time</span>
              </div>
              <div>
                <b>{catalog.length}</b>
                <span>Courses in focus</span>
              </div>
            </div>

            <SectionTitle title="Skills in focus" />
            <SkillRows skills={a.profile?.skills || ['Flutter', 'React', 'Design']} />

            <section className="callout" style={{ marginTop: 20 }}>
              <Users size={22} color="var(--ocean, #17646b)" />
              <h3>Learn with people</h3>
              <p>Study groups and peer review sessions are a gentle way to keep going.</p>
              <Button onClick={() => setView('Study groups')}>
                Find a study group
              </Button>
            </section>
          </aside>
        </div>
      )}

      {view === 'Explore' && (
        <LearnExplore
          query={query}
          setQuery={setQuery}
          categoryTab={categoryTab}
          setCategoryTab={setCategoryTab}
          subFilter={subFilter}
          setSubFilter={setSubFilter}
          courses={catalog}
          open={open}
          registeredClasses={registeredClasses}
          onToggleRegisterLiveClass={toggleRegisterLiveClass}
          activePracticeLabs={activePracticeLabs}
          onTogglePracticeLab={togglePracticeLab}
          toggleJoinGroup={toggleJoinGroup}
        />
      )}

      {view === 'Articles' && (
        <LearnArticles
          selectedArticle={selectedArticle}
          setSelectedArticle={setSelectedArticle}
          readArticles={readArticles}
          onToggleRead={toggleReadArticle}
          bookmarkedArticles={bookmarkedArticles}
          onToggleBookmark={toggleBookmarkArticle}
        />
      )}

      {view === 'My learning' && (
        <MyLearning open={open} courses={catalog} />
      )}
      {view === 'Live classes' && <LiveClasses />}
      {view === 'Practice labs' && <PracticeLabs />}
      {view === 'Study groups' && <StudyGroups />}
      {view === 'Resources' && <Resources />}
      {view === 'Certificates' && <Certificates />}
    </>
  );
}

// ─── Horizontal Scroll Container Primitive ─────────────────────────────────

function LearnHorizontalSection({
  title,
  count,
  badge,
  actionLabel = 'View all',
  onAction,
  icon: Icon,
  children,
}) {
  const scrollRef = React.useRef(null);

  const scroll = (direction) => {
    if (scrollRef.current) {
      const offset = direction === 'left' ? -310 : 310;
      scrollRef.current.scrollBy({ left: offset, behavior: 'smooth' });
    }
  };

  return (
    <div className="learn-horizontal-section">
      <div className="learn-horizontal-header">
        <h3>
          {Icon && <Icon size={19} color="var(--coral, #e65145)" />}
          <span>{title}</span>
          {(count !== undefined || badge) && (
            <span className="learn-section-badge">{badge || `${count}`}</span>
          )}
        </h3>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <button
            type="button"
            onClick={() => scroll('left')}
            className="icon-button"
            style={{ width: 28, height: 28 }}
            aria-label={`Scroll ${title} left`}
          >
            <ChevronLeft size={16} />
          </button>
          <button
            type="button"
            onClick={() => scroll('right')}
            className="icon-button"
            style={{ width: 28, height: 28 }}
            aria-label={`Scroll ${title} right`}
          >
            <ChevronRight size={16} />
          </button>
          {onAction && (
            <button
              type="button"
              className="learn-view-all-btn"
              onClick={onAction}
            >
              {actionLabel} <ChevronRight size={14} />
            </button>
          )}
        </div>
      </div>
      <div className="learn-horizontal-scroll" ref={scrollRef}>
        {children}
      </div>
    </div>
  );
}

// ─── Individual Horizontal Cards ───────────────────────────────────────────

function CourseHCard({ course, open, progress }) {
  const colorClass = course.color ? `${course.color}-accent` : 'blue-accent';
  return (
    <div className={`learn-h-card ${colorClass}`}>
      <div>
        <div className="learn-card-top">
          <span className={`learn-icon-badge ${course.color || 'blue'}`}>
            {course.symbol || 'CO'}
          </span>
          <span className="learn-card-meta-pill">{course.level || 'Beginner'}</span>
        </div>
        <h4 className="learn-card-title">{course.title}</h4>
        <p className="learn-card-desc">{course.description}</p>
        <div className="learn-card-tags">
          {(course.tags || []).slice(0, 3).map((t) => (
            <span key={t} className="learn-card-tag">{t}</span>
          ))}
        </div>
      </div>
      <div>
        {progress > 0 && (
          <div style={{ marginBottom: 10 }}>
            <Progress value={(progress / (course.lessons?.length || 1)) * 100} />
            <small style={{ color: 'var(--muted)', fontSize: '0.72rem', marginTop: 4, display: 'block' }}>
              {progress} of {course.lessons?.length || 5} lessons complete
            </small>
          </div>
        )}
        <div className="learn-card-footer">
          <div className="learn-card-stats">
            <Clock3 size={13} />
            <span>{course.duration || '2 hours'}</span>
          </div>
          <Button
            size="sm"
            variant={progress > 0 ? 'primary' : 'secondary'}
            onClick={() => open(course)}
          >
            {progress > 0 ? 'Resume' : 'View course'}
          </Button>
        </div>
      </div>
    </div>
  );
}

function LiveClassHCard({ liveClass, isRegistered, onToggleRegister }) {
  return (
    <div className="learn-h-card rose-accent">
      <div>
        <div className="learn-card-top">
          <span className="live-badge-pulsing">
            <span className="pulse-dot" /> LIVE {liveClass.month} {liveClass.day}
          </span>
          <span className="learn-card-meta-pill">{liveClass.time}</span>
        </div>
        <h4 className="learn-card-title">{liveClass.title}</h4>
        <p className="learn-card-desc">Instructor: {liveClass.mentor} · {liveClass.mentorRole || 'Tech Lead'}</p>
        <div className="learn-card-tags">
          {(liveClass.tags || []).map((t) => (
            <span key={t} className="learn-card-tag">{t}</span>
          ))}
        </div>
      </div>
      <div className="learn-card-footer">
        <div className="learn-card-stats">
          <Users size={14} color="#e65145" />
          <span>{liveClass.attendees + (isRegistered ? 1 : 0)} attending</span>
        </div>
        <Button
          size="sm"
          variant={isRegistered ? 'quiet' : 'primary'}
          onClick={() => onToggleRegister(liveClass.title)}
        >
          {isRegistered ? 'Registered ✓' : 'Register'}
        </Button>
      </div>
    </div>
  );
}

function LearningPathHCard({ path, onExplore }) {
  const colorClass = path.color ? `${path.color}-accent` : 'blue-accent';
  return (
    <div className={`learn-h-card ${colorClass}`}>
      <div>
        <div className="learn-card-top">
          <span className={`learn-icon-badge ${path.color || 'blue'}`}>
            <Sparkles size={16} />
          </span>
          <span className="learn-card-meta-pill">{path.level}</span>
        </div>
        <h4 className="learn-card-title">{path.title}</h4>
        <p className="learn-card-desc">{path.description}</p>
        <div className="path-steps-row">
          <span className="path-step-badge">{path.coursesCount} courses</span>
          <span className="path-step-badge">{path.duration}</span>
          <span className="path-step-badge">{path.labsCount} labs</span>
        </div>
        <div className="learn-card-tags">
          {(path.skills || []).slice(0, 3).map((s) => (
            <span key={s} className="learn-card-tag">{s}</span>
          ))}
        </div>
      </div>
      <div className="learn-card-footer">
        <span className="learn-card-stats">Curated curriculum</span>
        <Button size="sm" variant="secondary" onClick={onExplore}>
          Start path
        </Button>
      </div>
    </div>
  );
}

function StudyGroupHCard({ group, isJoined, onToggleJoin }) {
  const colorClass = group.color ? `${group.color}-accent` : 'green-accent';
  return (
    <div className={`learn-h-card ${colorClass}`}>
      <div>
        <div className="learn-card-top">
          <span className={`learn-icon-badge ${group.color || 'green'}`}>
            <Users size={16} />
          </span>
          <span className="group-active-indicator">
            <span className="active-dot" /> {group.activeNow} online
          </span>
        </div>
        <h4 className="learn-card-title">{group.title}</h4>
        <p className="learn-card-desc">{group.description}</p>
        <div style={{ fontSize: '0.78rem', color: 'var(--muted)', marginBottom: 12 }}>
          🕒 {group.schedule}
        </div>
        <div className="learn-card-tags">
          {(group.tags || []).map((t) => (
            <span key={t} className="learn-card-tag">{t}</span>
          ))}
        </div>
      </div>
      <div className="learn-card-footer">
        <span className="learn-card-stats">
          <Users size={13} /> {group.members + (isJoined ? 1 : 0)} members
        </span>
        <Button
          size="sm"
          variant={isJoined ? 'quiet' : 'primary'}
          onClick={onToggleJoin}
        >
          {isJoined ? 'Joined ✓' : 'Join group'}
        </Button>
      </div>
    </div>
  );
}

function PracticeLabHCard({ lab, isActive, onToggleLab }) {
  const diffClass = (lab.level || 'beginner').toLowerCase();
  return (
    <div className="learn-h-card gold-accent">
      <div>
        <div className="learn-card-top">
          <span className={`lab-difficulty-pill ${diffClass}`}>{lab.level}</span>
          <span className="learn-card-meta-pill">{lab.duration || '45 mins'}</span>
        </div>
        <h4 className="learn-card-title">{lab.title}</h4>
        <p className="learn-card-desc">{lab.description}</p>
        <div className="learn-card-tags">
          <span className="learn-card-tag" style={{ background: '#fef3c7', color: '#92400e' }}>
            Interactive Sandbox
          </span>
          <span className="learn-card-tag">{lab.tag}</span>
        </div>
      </div>
      <div className="learn-card-footer">
        <span className="learn-card-stats">🏆 +{lab.points || 150} XP</span>
        <Button
          size="sm"
          variant={isActive ? 'quiet' : 'primary'}
          onClick={onToggleLab}
        >
          {isActive ? 'Continue lab' : 'Start lab'}
        </Button>
      </div>
    </div>
  );
}

// ─── Explore Tab with Primary & Sub-filters ────────────────────────────────

function LearnExplore({
  query,
  setQuery,
  categoryTab,
  setCategoryTab,
  subFilter,
  setSubFilter,
  courses: catalogCourses,
  open,
  registeredClasses,
  onToggleRegisterLiveClass,
  activePracticeLabs,
  onTogglePracticeLab,
  toggleJoinGroup,
}) {
  const a = useApp();

  const categories = [
    'All',
    'Java',
    'Spring Boot',
    'Development',
    'Backend',
    'Frontend',
    'AI',
    'Mobile',
    'Design',
    'Data',
  ];

  const subFilters = [
    'All types',
    'Courses',
    'Learning paths',
    'Live classes',
    'Study groups',
    'Practice labs',
  ];

  // Aggregate all items into a unified searchable index
  const allExploreItems = useMemo(() => {
    const list = [];

    // Courses
    catalogCourses.forEach((c) => {
      list.push({
        id: `course-${c.id}`,
        type: 'Courses',
        category: c.category || 'Development',
        title: c.title,
        description: c.description,
        mentor: c.mentor,
        duration: c.duration,
        level: c.level,
        tags: c.tags || [],
        color: c.color || 'blue',
        symbol: c.symbol || 'CO',
        rawItem: c,
      });
    });

    // Learning paths
    defaultLearningPaths.forEach((p) => {
      list.push({
        id: `path-${p.id}`,
        type: 'Learning paths',
        category: p.category,
        title: p.title,
        description: p.description,
        mentor: p.mentor,
        duration: `${p.coursesCount} courses · ${p.duration}`,
        level: p.level,
        tags: p.skills,
        color: p.color,
        symbol: p.symbol,
        rawItem: p,
      });
    });

    // Live classes
    defaultLiveClasses.forEach((l) => {
      list.push({
        id: `live-${l.id}`,
        type: 'Live classes',
        category: l.category,
        title: l.title,
        description: `Live webinar with ${l.mentor} (${l.mentorRole})`,
        mentor: l.mentor,
        duration: `${l.month} ${l.day} · ${l.time}`,
        level: l.level,
        tags: l.tags,
        color: 'rose',
        symbol: 'LIVE',
        rawItem: l,
      });
    });

    // Study groups
    defaultStudyGroups.forEach((g, idx) => {
      list.push({
        id: `group-${g.id}`,
        type: 'Study groups',
        category: g.category,
        title: g.title,
        description: g.description,
        mentor: `${g.members} members · ${g.activeNow} online`,
        duration: g.schedule,
        level: g.level,
        tags: g.tags,
        color: g.color,
        symbol: 'SG',
        groupIndex: idx,
        rawItem: g,
      });
    });

    // Practice labs
    defaultPracticeLabs.forEach((b) => {
      list.push({
        id: `lab-${b.id}`,
        type: 'Practice labs',
        category: b.category,
        title: b.title,
        description: b.description,
        mentor: `Tech: ${b.tag}`,
        duration: b.duration,
        level: b.level,
        tags: [b.tag, b.level],
        color: 'gold',
        symbol: 'LAB',
        rawItem: b,
      });
    });

    return list;
  }, [catalogCourses]);

  // Filtering by category, sub-filter, and query
  const filteredItems = useMemo(() => {
    return allExploreItems.filter((item) => {
      // Primary category check
      if (categoryTab !== 'All') {
        const cat = item.category?.toLowerCase() || '';
        const target = categoryTab.toLowerCase();
        if (target === 'java' || target === 'spring boot') {
          const hasJava =
            item.title.toLowerCase().includes('java') ||
            item.title.toLowerCase().includes('spring') ||
            item.tags.some((t) => t.toLowerCase().includes('java') || t.toLowerCase().includes('spring')) ||
            (item.skills && item.skills.some((s) => s.toLowerCase().includes('java') || s.toLowerCase().includes('spring')));
          if (!hasJava) return false;
        } else if (target === 'development') {
          const devCats = ['development', 'frontend', 'backend', 'mobile', 'ai'];
          if (!devCats.includes(cat)) return false;
        } else if (target === 'frontend') {
          if (cat !== 'frontend' && !item.tags.some((t) => t.toLowerCase() === 'frontend' || t.toLowerCase() === 'react')) {
            return false;
          }
        } else if (target === 'backend') {
          if (cat !== 'backend' && !item.tags.some((t) => t.toLowerCase() === 'backend' || t.toLowerCase() === 'java' || t.toLowerCase() === 'spring boot' || t.toLowerCase() === 'go' || t.toLowerCase() === 'node.js')) {
            return false;
          }
        } else if (target === 'ai') {
          if (cat !== 'ai' && !item.tags.some((t) => t.toLowerCase() === 'ai' || t.toLowerCase() === 'agents' || t.toLowerCase() === 'python')) {
            return false;
          }
        } else if (target === 'design') {
          if (cat !== 'design' && !item.tags.some((t) => t.toLowerCase() === 'design' || t.toLowerCase() === 'figma')) {
            return false;
          }
        } else if (target === 'mobile') {
          if (cat !== 'mobile' && !item.tags.some((t) => t.toLowerCase() === 'flutter' || t.toLowerCase() === 'dart' || t.toLowerCase() === 'mobile')) {
            return false;
          }
        } else if (target === 'data') {
          if (cat !== 'data' && !item.tags.some((t) => t.toLowerCase().includes('data') || t.toLowerCase() === 'sql')) {
            return false;
          }
        } else if (cat !== target) {
          return false;
        }
      }

      // Sub-filter (content type) check
      if (subFilter !== 'All types') {
        if (item.type.toLowerCase() !== subFilter.toLowerCase()) {
          return false;
        }
      }

      // Search query check
      if (query && query.trim()) {
        const q = query.trim().toLowerCase();
        const haystack = `${item.title} ${item.description} ${item.mentor} ${item.tags.join(' ')} ${item.level} ${item.category}`.toLowerCase();
        if (!haystack.includes(q)) return false;
      }

      return true;
    });
  }, [allExploreItems, categoryTab, subFilter, query]);

  return (
    <section className="learn-section" style={{ maxWidth: '100%' }}>
      {/* Search and Multi-tier Filters */}
      <div className="explore-controls-wrapper">
        <SearchField
          value={query}
          onChange={setQuery}
          placeholder="Search courses, learning paths, live classes, study groups, practice labs..."
        />

        {/* Primary Category Filters under search bar */}
        <div className="explore-filter-group">
          <div className="explore-filter-label">Categories</div>
          <div className="explore-chips-row">
            {categories.map((cat) => (
              <button
                key={cat}
                type="button"
                className={`explore-chip-btn ${categoryTab === cat ? 'active' : ''}`}
                onClick={() => setCategoryTab(cat)}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        {/* Sub-filter for content types */}
        <div className="explore-filter-group">
          <div className="explore-filter-label">Filter by type</div>
          <div className="explore-chips-row">
            {subFilters.map((sub) => (
              <button
                key={sub}
                type="button"
                className={`subfilter-chip-btn ${subFilter === sub ? 'active' : ''}`}
                onClick={() => setSubFilter(sub)}
              >
                {sub === 'Courses' && <BookOpen size={13} />}
                {sub === 'Learning paths' && <Compass size={13} />}
                {sub === 'Live classes' && <CalendarDays size={13} />}
                {sub === 'Study groups' && <Users size={13} />}
                {sub === 'Practice labs' && <Terminal size={13} />}
                <span>{sub}</span>
              </button>
            ))}
          </div>
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', margin: '14px 0 10px' }}>
        <SectionTitle
          title={`${filteredItems.length} items found`}
          style={{ margin: 0 }}
        />
        {(categoryTab !== 'All' || subFilter !== 'All types' || query) && (
          <Button
            size="sm"
            variant="quiet"
            onClick={() => {
              setCategoryTab('All');
              setSubFilter('All types');
              setQuery('');
            }}
          >
            Reset filters
          </Button>
        )}
      </div>

      {filteredItems.length > 0 ? (
        <div className="learn-explore-grid">
          {filteredItems.map((item) => (
            <ExploreItemCard
              key={item.id}
              item={item}
              open={open}
              registeredClasses={registeredClasses}
              onToggleRegisterLiveClass={onToggleRegisterLiveClass}
              activePracticeLabs={activePracticeLabs}
              onTogglePracticeLab={onTogglePracticeLab}
              toggleJoinGroup={toggleJoinGroup}
            />
          ))}
        </div>
      ) : (
        <Empty
          title="No learning resources match your filters"
          text="Try relaxing your search terms or picking another category / type."
          action="Show all courses & paths"
          onAction={() => {
            setCategoryTab('All');
            setSubFilter('All types');
            setQuery('');
          }}
        />
      )}
    </section>
  );
}

function ExploreItemCard({
  item,
  open,
  registeredClasses,
  onToggleRegisterLiveClass,
  activePracticeLabs,
  onTogglePracticeLab,
  toggleJoinGroup,
}) {
  const a = useApp();
  const typeTone = {
    Courses: 'blue',
    'Learning paths': 'green',
    'Live classes': 'coral',
    'Study groups': 'purple',
    'Practice labs': 'gold',
  }[item.type] || 'blue';

  const isLiveReg = item.type === 'Live classes' && registeredClasses?.includes(item.title);
  const isLabActive = item.type === 'Practice labs' && activePracticeLabs?.some((l) => l.title === item.title);
  const isGroupJoined = item.type === 'Study groups' && a.memberships[`study-${item.groupIndex}`] === 'member';

  return (
    <div className="explore-item-card">
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
          <Tag tone={typeTone}>{item.type}</Tag>
          <span style={{ fontSize: '0.72rem', background: 'var(--soft)', padding: '2px 8px', borderRadius: 4, color: 'var(--muted)', fontWeight: 600 }}>
            {item.category}
          </span>
        </div>

        <h4 style={{ margin: '0 0 6px', fontSize: '1rem', fontWeight: 700, color: 'var(--ink)' }}>
          {item.title}
        </h4>
        <p style={{ margin: '0 0 12px', fontSize: '0.82rem', color: 'var(--muted)', lineHeight: 1.45 }}>
          {item.description}
        </p>

        <div style={{ display: 'flex', gap: 5, flexWrap: 'wrap', marginBottom: 12 }}>
          {item.tags.slice(0, 3).map((t) => (
            <span key={t} className="learn-card-tag">{t}</span>
          ))}
        </div>
      </div>

      <div style={{ borderTop: '1px solid var(--line)', paddingTop: 12, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
        <small style={{ color: 'var(--muted)', fontSize: '0.78rem' }}>
          {item.duration || item.level}
        </small>

        {item.type === 'Courses' && (
          <Button size="sm" variant="primary" onClick={() => open(item.rawItem)}>
            View course
          </Button>
        )}
        {item.type === 'Learning paths' && (
          <Button
            size="sm"
            variant="secondary"
            onClick={() => {
              a.toast(`Selected "${item.title}". Loading path curriculum...`);
            }}
          >
            Start path
          </Button>
        )}
        {item.type === 'Live classes' && (
          <Button
            size="sm"
            variant={isLiveReg ? 'quiet' : 'primary'}
            onClick={() => onToggleRegisterLiveClass(item.title)}
          >
            {isLiveReg ? 'Registered ✓' : 'Register'}
          </Button>
        )}
        {item.type === 'Study groups' && (
          <Button
            size="sm"
            variant={isGroupJoined ? 'quiet' : 'primary'}
            onClick={() => toggleJoinGroup(item.rawItem, item.groupIndex)}
          >
            {isGroupJoined ? 'Joined ✓' : 'Join group'}
          </Button>
        )}
        {item.type === 'Practice labs' && (
          <Button
            size="sm"
            variant={isLabActive ? 'quiet' : 'primary'}
            onClick={() => onTogglePracticeLab(item.rawItem)}
          >
            {isLabActive ? 'In progress' : 'Launch lab'}
          </Button>
        )}
      </div>
    </div>
  );
}

// ─── Articles Module & Reader ──────────────────────────────────────────────

function BookmarkCategoryEditor({
  currentCategories = [],
  allCategories = [],
  onAddCategory,
  onClose,
}) {
  const [typedCategory, setTypedCategory] = useState('');
  const [selectedDropdown, setSelectedDropdown] = useState('');

  const currentLower = useMemo(
    () => currentCategories.map((c) => String(c).trim().toLowerCase()),
    [currentCategories]
  );

  const availableChoices = useMemo(() => {
    return allCategories.filter(
      (cat) => cat && !currentLower.includes(String(cat).trim().toLowerCase())
    );
  }, [allCategories, currentLower]);

  const handleCreateNew = () => {
    if (typedCategory.trim()) {
      onAddCategory(typedCategory.trim());
      setTypedCategory('');
      onClose();
    }
  };

  const handleSelectExisting = (cat) => {
    if (cat && cat.trim()) {
      onAddCategory(cat.trim());
      onClose();
    }
  };

  return (
    <div
      className="bookmark-category-editor"
      onClick={(e) => e.stopPropagation()}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <strong style={{ fontSize: '0.8rem', color: 'var(--ink)' }}>
          Add to Bookmark Category
        </strong>
        <button
          type="button"
          onClick={onClose}
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: 'var(--muted)',
            fontSize: '0.82rem',
            padding: '2px 6px',
            borderRadius: 4,
          }}
          title="Close editor"
        >
          ✕
        </button>
      </div>

      {/* Option 1: Choose from existing custom categories */}
      {availableChoices.length > 0 && (
        <div>
          <div style={{ fontSize: '0.72rem', color: 'var(--muted)', fontWeight: 700, marginBottom: 6, textTransform: 'uppercase', letterSpacing: '0.4px' }}>
            Choose from your existing categories:
          </div>
          <div style={{ display: 'flex', gap: 5, flexWrap: 'wrap', maxHeight: 85, overflowY: 'auto', paddingBottom: 2 }}>
            {availableChoices.slice(0, 12).map((cat) => (
              <button
                key={cat}
                type="button"
                className="bookmark-chip-option"
                onClick={() => handleSelectExisting(cat)}
                title={`Add "${cat}"`}
              >
                + {cat}
              </button>
            ))}
          </div>

          {availableChoices.length > 12 && (
            <div style={{ marginTop: 6 }}>
              <select
                value={selectedDropdown}
                onChange={(e) => {
                  if (e.target.value) {
                    handleSelectExisting(e.target.value);
                  }
                }}
                style={{
                  width: '100%',
                  padding: '5px 8px',
                  fontSize: '0.75rem',
                  borderRadius: 5,
                  border: '1px solid var(--line)',
                  background: 'var(--surface)',
                  color: 'var(--ink)',
                }}
              >
                <option value="">More of your categories...</option>
                {availableChoices.slice(12).map((cat) => (
                  <option key={cat} value={cat}>
                    + {cat}
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>
      )}

      {/* Option 2: Type a new custom category */}
      <div>
        <div style={{ fontSize: '0.72rem', color: 'var(--muted)', fontWeight: 700, marginBottom: 6, textTransform: 'uppercase', letterSpacing: '0.4px' }}>
          {availableChoices.length > 0 ? 'Or type a new category:' : 'Type a new category:'}
        </div>
        <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
          <input
            type="text"
            value={typedCategory}
            onChange={(e) => setTypedCategory(e.target.value)}
            placeholder="Type your category name (e.g. My Notes, Weekend Read)..."
            style={{
              flex: 1,
              padding: '5px 10px',
              fontSize: '0.78rem',
              borderRadius: 5,
              border: '1px solid var(--line)',
              background: 'var(--surface)',
              color: 'var(--ink)',
            }}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                handleCreateNew();
              }
            }}
          />
          <Button size="sm" variant="primary" onClick={handleCreateNew} disabled={!typedCategory.trim()}>
            Save
          </Button>
          <Button size="sm" variant="quiet" onClick={onClose}>
            Cancel
          </Button>
        </div>
      </div>
    </div>
  );
}

function LearnArticles({
  selectedArticle,
  setSelectedArticle,
  readArticles,
  onToggleRead,
  bookmarkedArticles,
  onToggleBookmark,
}) {
  const a = useApp();
  const [navTab, setNavTab] = useState('all'); // 'all' | 'bookmarks'
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('All');
  const [bookmarkCategory, setBookmarkCategory] = useState('All');
  const [customBookmarkCategories, setCustomBookmarkCategories] = useStoredState(
    'collobster_article_bookmark_categories',
    {}
  );
  const [activeTagInputId, setActiveTagInputId] = useState(null);

  const categories = ['All', 'Frontend', 'Backend', 'AI', 'Design', 'Mobile'];

  const bookmarkedList = useMemo(() => {
    return defaultArticles.filter((art) => bookmarkedArticles.includes(art.id));
  }, [bookmarkedArticles]);

  // Only user's own customized categories created across bookmarks
  const allAvailableCategories = useMemo(() => {
    const set = new Set();
    Object.values(customBookmarkCategories).forEach((val) => {
      if (Array.isArray(val)) {
        val.forEach((c) => {
          if (c && c.trim()) set.add(c.trim());
        });
      } else if (typeof val === 'string' && val.trim()) {
        set.add(val.trim());
      }
    });
    return Array.from(set);
  }, [customBookmarkCategories]);

  // Categories of Bookmarks: 'All' + only the user's custom categories
  const bookmarkCategories = useMemo(() => {
    return ['All', ...allAvailableCategories];
  }, [allAvailableCategories]);

  const addBookmarkCategory = (articleId, tag) => {
    if (!tag || !tag.trim()) return;
    const clean = tag.trim();
    setCustomBookmarkCategories((prev) => {
      const existing = prev[articleId] || [];
      const list = Array.isArray(existing) ? existing : [existing];
      if (list.includes(clean)) return prev;
      return { ...prev, [articleId]: [...list, clean] };
    });
    a.toast(`Added bookmark category "${clean}"`);
    setActiveTagInputId(null);
  };

  const removeBookmarkCategory = (articleId, tagToRemove) => {
    setCustomBookmarkCategories((prev) => {
      const existing = prev[articleId] || [];
      const list = Array.isArray(existing) ? existing : [existing];
      return { ...prev, [articleId]: list.filter((c) => c !== tagToRemove) };
    });
    a.toast(`Removed category "${tagToRemove}"`);
  };

  const filteredArticles = useMemo(() => {
    const source = navTab === 'bookmarks' ? bookmarkedList : defaultArticles;
    return source.filter((art) => {
      if (navTab === 'bookmarks') {
        if (bookmarkCategory !== 'All') {
          const custom = customBookmarkCategories[art.id] || [];
          const customList = Array.isArray(custom) ? custom : [custom];
          const matches = customList.some(
            (c) => c.toLowerCase() === bookmarkCategory.toLowerCase()
          );
          if (!matches) return false;
        }
      } else {
        if (category !== 'All' && art.category.toLowerCase() !== category.toLowerCase()) {
          return false;
        }
      }

      if (query.trim()) {
        const q = query.trim().toLowerCase();
        const custom = (customBookmarkCategories[art.id] || []).join(' ');
        const haystack = `${art.title} ${art.summary} ${art.author} ${art.tags.join(' ')} ${custom}`.toLowerCase();
        if (!haystack.includes(q)) return false;
      }
      return true;
    });
  }, [navTab, bookmarkedList, bookmarkCategory, category, query, customBookmarkCategories]);

  if (selectedArticle) {
    return (
      <ArticleReader
        article={selectedArticle}
        onBack={() => setSelectedArticle(null)}
        isRead={readArticles.includes(selectedArticle.id)}
        onToggleRead={() => onToggleRead(selectedArticle.id)}
        isBookmarked={bookmarkedArticles.includes(selectedArticle.id)}
        onToggleBookmark={() => onToggleBookmark(selectedArticle.id)}
        customCategories={customBookmarkCategories[selectedArticle.id] || []}
        allCategories={allAvailableCategories}
        onAddCategory={(cat) => addBookmarkCategory(selectedArticle.id, cat)}
        onRemoveCategory={(cat) => removeBookmarkCategory(selectedArticle.id, cat)}
      />
    );
  }

  return (
    <section className="learn-section" style={{ maxWidth: '100%' }}>
      {/* Articles Hero Banner */}
      <div className="articles-hero">
        <div>
          <span className="eyebrow" style={{ color: '#99f6e4' }}>
            ENGINEERING &amp; DESIGN INSIGHTS
          </span>
          <h2>In-depth articles written by creators and practitioners.</h2>
          <p>
            Architectural breakdowns, system patterns, production case studies,
            and design tokens that bridge concepts with working implementations.
          </p>
        </div>
        <FileText size={64} style={{ opacity: 0.85, flexShrink: 0 }} />
      </div>

      {/* Nav Tabs: All Articles vs My Bookmarks */}
      <div className="article-nav-tabs">
        <button
          type="button"
          className={`article-nav-tab ${navTab === 'all' ? 'active' : ''}`}
          onClick={() => {
            setNavTab('all');
            setQuery('');
          }}
        >
          <BookOpen size={16} /> All Articles ({defaultArticles.length})
        </button>
        <button
          type="button"
          className={`article-nav-tab ${navTab === 'bookmarks' ? 'active' : ''}`}
          onClick={() => {
            setNavTab('bookmarks');
            setBookmarkCategory('All');
            setQuery('');
          }}
        >
          <Bookmark size={16} /> My Bookmarks ({bookmarkedArticles.length})
        </button>
      </div>

      {/* Search and Filters */}
      <div className="explore-controls-wrapper" style={{ marginBottom: 18 }}>
        <SearchField
          value={query}
          onChange={setQuery}
          placeholder={
            navTab === 'bookmarks'
              ? 'Search through your bookmarked articles...'
              : 'Search articles by title, topic, author or keyword...'
          }
        />

        {navTab === 'all' ? (
          <div className="explore-filter-group">
            <div className="explore-filter-label">Filter by topic</div>
            <div className="explore-chips-row">
              {categories.map((cat) => (
                <button
                  key={cat}
                  type="button"
                  className={`explore-chip-btn ${category === cat ? 'active' : ''}`}
                  onClick={() => setCategory(cat)}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>
        ) : bookmarkCategories.length > 1 ? (
          <div className="explore-filter-group">
            <div className="explore-filter-label" style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <Bookmark size={13} color="var(--coral, #e65145)" /> Categories of Bookmarks
            </div>
            <div className="explore-chips-row">
              {bookmarkCategories.map((bCat) => (
                <button
                  key={bCat}
                  type="button"
                  className={`explore-chip-btn ${bookmarkCategory === bCat ? 'active' : ''}`}
                  onClick={() => setBookmarkCategory(bCat)}
                >
                  {bCat}
                </button>
              ))}
            </div>
          </div>
        ) : null}
      </div>

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
        <SectionTitle
          title={
            navTab === 'bookmarks'
              ? `${filteredArticles.length} bookmarked articles`
              : `${filteredArticles.length} articles available`
          }
          style={{ margin: 0 }}
        />
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          {navTab === 'all' && bookmarkedArticles.length > 0 && (
            <button
              type="button"
              className="text-button"
              style={{ fontSize: '0.82rem' }}
              onClick={() => setNavTab('bookmarks')}
            >
              <Bookmark size={14} /> View My Bookmarks ({bookmarkedArticles.length})
            </button>
          )}
          {readArticles.length > 0 && (
            <span style={{ fontSize: '0.82rem', color: '#059669', fontWeight: 700 }}>
              ✓ {readArticles.length} completed
            </span>
          )}
        </div>
      </div>

      {navTab === 'bookmarks' && bookmarkedList.length === 0 ? (
        <Empty
          title="No bookmarked articles yet"
          text="When reading articles, click the bookmark icon on any card or reader to save them here and organize them by category."
          action="Explore all articles"
          onAction={() => setNavTab('all')}
        />
      ) : filteredArticles.length > 0 ? (
        <div className="articles-grid">
          {filteredArticles.map((article) => {
            const isRead = readArticles.includes(article.id);
            const isSaved = bookmarkedArticles.includes(article.id);
            const customCats = customBookmarkCategories[article.id] || [];
            const isEditingTag = activeTagInputId === article.id;

            return (
              <div
                key={article.id}
                className="article-card"
                onClick={() => setSelectedArticle(article)}
              >
                <div>
                  <div className="article-card-header">
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <Tag tone={article.color || 'blue'}>{article.category}</Tag>
                      {isSaved && (
                        <span style={{ fontSize: '0.72rem', background: '#fffbeb', color: '#b45309', border: '1px solid #fde68a', padding: '2px 6px', borderRadius: 4, fontWeight: 700, display: 'inline-flex', alignItems: 'center', gap: 3 }}>
                          <Bookmark size={11} /> Saved
                        </span>
                      )}
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      {isRead && (
                        <span style={{ fontSize: '0.72rem', background: '#ecfdf5', color: '#059669', padding: '2px 6px', borderRadius: 4, fontWeight: 700 }}>
                          Read ✓
                        </span>
                      )}
                      <span className="article-read-badge">
                        <Clock3 size={13} /> {article.readTime}
                      </span>
                      <button
                        type="button"
                        className={`article-bookmark-toggle ${isSaved ? 'bookmarked' : ''}`}
                        title={isSaved ? 'Remove bookmark' : 'Bookmark article'}
                        onClick={(e) => {
                          e.stopPropagation();
                          onToggleBookmark(article.id);
                        }}
                      >
                        <Bookmark size={15} />
                      </button>
                    </div>
                  </div>

                  <h3 className="article-card-title">{article.title}</h3>
                  <p className="article-card-summary">{article.summary}</p>

                  {/* Bookmark categories tag section */}
                  {isSaved && (
                    <div
                      style={{
                        background: 'var(--soft)',
                        padding: '6px 10px',
                        borderRadius: 6,
                        marginBottom: 10,
                        border: '1px solid var(--line)',
                      }}
                      onClick={(e) => e.stopPropagation()}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
                        <small style={{ fontSize: '0.7rem', fontWeight: 700, color: 'var(--muted)', textTransform: 'uppercase' }}>
                          Bookmark Categories
                        </small>
                        {!isEditingTag && (
                          <button
                            type="button"
                            style={{ background: 'none', border: 'none', color: 'var(--coral, #e65145)', fontSize: '0.72rem', fontWeight: 700, cursor: 'pointer' }}
                            onClick={() => {
                              setActiveTagInputId(article.id);
                            }}
                          >
                            + Add category
                          </button>
                        )}
                      </div>
                      <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap', alignItems: 'center' }}>
                        {customCats.length === 0 && !isEditingTag && (
                          <span style={{ fontSize: '0.74rem', color: 'var(--muted)', fontStyle: 'italic' }}>
                            No customized categories yet
                          </span>
                        )}
                        {customCats.map((catName) => (
                          <span
                            key={catName}
                            className="learn-card-tag"
                            style={{ background: '#e0f2fe', color: '#0369a1', display: 'inline-flex', alignItems: 'center', gap: 4 }}
                          >
                            {catName}
                            <X
                              size={11}
                              style={{ cursor: 'pointer' }}
                              onClick={() => removeBookmarkCategory(article.id, catName)}
                            />
                          </span>
                        ))}
                      </div>

                      {isEditingTag && (
                        <BookmarkCategoryEditor
                          currentCategories={customCats}
                          allCategories={allAvailableCategories}
                          onAddCategory={(cat) => addBookmarkCategory(article.id, cat)}
                          onClose={() => setActiveTagInputId(null)}
                        />
                      )}
                    </div>
                  )}

                  <div style={{ display: 'flex', gap: 5, flexWrap: 'wrap', marginBottom: 14 }}>
                    {article.tags.map((t) => (
                      <span key={t} className="learn-card-tag">{t}</span>
                    ))}
                  </div>
                </div>

                <div>
                  <div className="article-card-author">
                    <img
                      src={article.authorImage}
                      alt={article.author}
                      className="article-author-avatar"
                    />
                    <div className="article-author-info" style={{ flex: 1 }}>
                      <strong>{article.author}</strong>
                      <small>{article.date} · {article.authorRole}</small>
                    </div>
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={(e) => {
                        e.stopPropagation();
                        setSelectedArticle(article);
                      }}
                    >
                      Read
                    </Button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <Empty
          title="No articles found"
          text="Try adjusting your search terms or selecting another category."
          action="Show all articles"
          onAction={() => {
            setCategory('All');
            setBookmarkCategory('All');
            setQuery('');
          }}
        />
      )}
    </section>
  );
}

function ArticleReader({
  article,
  onBack,
  isRead,
  onToggleRead,
  isBookmarked,
  onToggleBookmark,
  customCategories = [],
  allCategories = [],
  onAddCategory,
  onRemoveCategory,
}) {
  const a = useApp();
  const [showAddCat, setShowAddCat] = useState(false);

  const handleShare = () => {
    navigator.clipboard?.writeText(window.location.href);
    a.toast('Article link copied to clipboard!');
  };

  return (
    <article className="article-reader-container">
      {/* Reader Nav */}
      <div className="article-reader-nav">
        <button type="button" className="article-back-btn" onClick={onBack}>
          <ArrowLeft size={16} /> Back to Articles
        </button>
        <div className="article-reader-actions">
          <Button
            size="sm"
            variant={isBookmarked ? 'primary' : 'quiet'}
            icon={Bookmark}
            onClick={onToggleBookmark}
          >
            {isBookmarked ? 'Bookmarked ✓' : 'Bookmark'}
          </Button>
          <Button
            size="sm"
            variant="quiet"
            icon={Share2}
            onClick={handleShare}
          >
            Share
          </Button>
          <Button
            size="sm"
            variant={isRead ? 'quiet' : 'primary'}
            icon={CheckCircle2}
            onClick={onToggleRead}
          >
            {isRead ? 'Completed ✓' : 'Mark as Read'}
          </Button>
        </div>
      </div>

      {/* Article Header */}
      <header className="article-reader-header">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', marginBottom: 8 }}>
          <Tag tone={article.color || 'blue'}>{article.category}</Tag>
          {isBookmarked && (
            <span style={{ fontSize: '0.75rem', background: '#fffbeb', color: '#b45309', border: '1px solid #fde68a', padding: '3px 8px', borderRadius: 4, fontWeight: 700, display: 'inline-flex', alignItems: 'center', gap: 4 }}>
              <Bookmark size={12} /> Saved in My Bookmarks
            </span>
          )}
        </div>

        <h1>{article.title}</h1>

        <div className="article-reader-meta">
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <img
              src={article.authorImage}
              alt={article.author}
              className="article-author-avatar"
              style={{ width: 42, height: 42 }}
            />
            <div>
              <strong style={{ display: 'block', fontSize: '0.92rem', color: 'var(--ink)' }}>
                {article.author}
              </strong>
              <small style={{ color: 'var(--muted)', fontSize: '0.8rem' }}>
                {article.authorRole}
              </small>
            </div>
          </div>
          <span style={{ color: 'var(--muted)', fontSize: '0.82rem' }}>
            📅 {article.date} · ⏱️ {article.readTime}
          </span>
        </div>

        {/* Bookmark category management inside Reader */}
        {isBookmarked && (
          <div style={{ marginTop: 14, padding: '12px 14px', background: 'var(--soft)', borderRadius: 8, border: '1px solid var(--line)' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
              <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--muted)', textTransform: 'uppercase' }}>
                🔖 Bookmark Categories for this Article
              </span>
              {!showAddCat && (
                <button
                  type="button"
                  style={{ background: 'none', border: 'none', color: 'var(--coral, #e65145)', fontSize: '0.75rem', fontWeight: 700, cursor: 'pointer' }}
                  onClick={() => setShowAddCat(true)}
                >
                  + Add category
                </button>
              )}
            </div>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', alignItems: 'center' }}>
              {customCategories.length === 0 && !showAddCat && (
                <span style={{ fontSize: '0.78rem', color: 'var(--muted)', fontStyle: 'italic' }}>
                  No customized categories yet
                </span>
              )}
              {customCategories.map((c) => (
                <span
                  key={c}
                  className="learn-card-tag"
                  style={{ background: '#e0f2fe', color: '#0369a1', display: 'inline-flex', alignItems: 'center', gap: 4 }}
                >
                  {c}
                  <X size={11} style={{ cursor: 'pointer' }} onClick={() => onRemoveCategory(c)} />
                </span>
              ))}
            </div>

            {showAddCat && (
              <BookmarkCategoryEditor
                currentCategories={customCategories}
                allCategories={allCategories}
                onAddCategory={(cat) => {
                  onAddCategory(cat);
                  setShowAddCat(false);
                }}
                onClose={() => setShowAddCat(false)}
              />
            )}
          </div>
        )}
      </header>

      {/* Article Body Content */}
      <div className="article-reader-content">
        {article.content.map((block, idx) => {
          if (block.type === 'lead') {
            return (
              <p key={idx} className="article-body-lead">
                {block.text}
              </p>
            );
          }
          if (block.type === 'h2') {
            return (
              <h2 key={idx} className="article-body-h2">
                {block.text}
              </h2>
            );
          }
          if (block.type === 'p') {
            return (
              <p key={idx} className="article-body-p">
                {block.text}
              </p>
            );
          }
          if (block.type === 'code') {
            return (
              <pre key={idx} className="article-body-code">
                <code>{block.code}</code>
              </pre>
            );
          }
          if (block.type === 'takeaway') {
            return (
              <aside key={idx} className="article-body-takeaway">
                💡 {block.text}
              </aside>
            );
          }
          return null;
        })}
      </div>

      {/* Reader Footer */}
      <footer className="article-reader-footer">
        <div>
          <strong style={{ display: 'block', marginBottom: 4 }}>Finished reading?</strong>
          <small style={{ color: 'var(--muted)' }}>
            Marking an article as read adds points to your continuous learning record.
          </small>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <Button variant={isRead ? 'quiet' : 'primary'} onClick={onToggleRead}>
            {isRead ? 'Mark as Unread' : 'Mark as Read ✓ (+25 XP)'}
          </Button>
          <Button variant="secondary" onClick={onBack}>
            Back to Articles
          </Button>
        </div>
      </footer>
    </article>
  );
}

function MyLearning({ open, courses: catalog }) {
  const a = useApp();
  const [tab, setTab] = useState('In progress');
  const [query, setQuery] = useState('');

  const list =
    tab === 'Completed'
      ? catalog.filter(
          (course) =>
            course.lessons.length &&
            (a.learning[course.id]?.length || 0) === course.lessons.length
        )
      : tab === 'Wishlist'
      ? catalog.filter(
          (course) => course.wishlisted || (a.wishlist || []).includes(course.id)
        )
      : catalog.filter(
          (course) =>
            (a.learning[course.id]?.length || 0) > 0 ||
            course.enrollmentState === 'active'
        );

  const filteredList = list.filter((course) => {
    if (!query.trim()) return true;
    const q = query.trim().toLowerCase();
    const titleMatch = (course.title || '').toLowerCase().includes(q);
    const mentorMatch = (course.mentor || '').toLowerCase().includes(q);
    const descMatch = (course.description || '').toLowerCase().includes(q);
    const tagMatch = Array.isArray(course.tags) && course.tags.some((t) => t.toLowerCase().includes(q));
    return titleMatch || mentorMatch || descMatch || tagMatch;
  });

  return (
    <section className="learn-section">
      <div className="space-controls" style={{ margin: '14px 0 16px' }}>
        <SearchField
          value={query}
          onChange={setQuery}
          placeholder="Search your courses by title, topic, mentor..."
        />
        <Tabs
          items={['In progress', 'Completed', 'Wishlist']}
          value={tab}
          onChange={setTab}
        />
      </div>
      {filteredList.length ? (
        <div className="result-list">
          {filteredList.map((course) => {
            const progress = a.learning[course.id]?.length || 0;
            const total =
              course.lessons.length ||
              Math.max(1, Math.ceil((course.progressPercent || 0) / 25));

            return (
              <article className="learn-list-row" key={course.id}>
                <span className={`course-mark ${course.color}`}>
                  {course.symbol}
                </span>
                <div>
                  <h3>{course.title}</h3>
                  <Progress
                    value={
                      course.remote
                        ? course.progressPercent || 0
                        : (progress / total) * 100
                    }
                    label={`${course.title} progress`}
                  />
                  <small>
                    {course.remote
                      ? `${course.progressPercent || 0}% complete`
                      : `${progress} of ${total} lessons complete`}
                  </small>
                </div>
                <Button onClick={() => open(course)}>
                  {course.remote
                    ? course.progressPercent === 100
                      ? 'Review'
                      : 'Continue'
                    : progress === total
                    ? 'Review'
                    : 'Continue'}
                </Button>
              </article>
            );
          })}
        </div>
      ) : query ? (
        <Empty
          title="No courses match your search"
          text={`No courses found matching "${query}". Try adjusting your keywords.`}
          action="Clear search"
          onAction={() => setQuery('')}
        />
      ) : (
        <Empty
          title={
            tab === 'Wishlist'
              ? 'Your wishlist is waiting'
              : 'No courses in progress'
          }
          text={
            tab === 'Wishlist'
              ? 'Save courses from Explore to return to them later.'
              : 'Start a course and your progress will appear here.'
          }
          action="Explore courses"
          onAction={() => a.go('Learn')}
        />
      )}
    </section>
  );
}

function LiveClasses() {
  const a = useApp();
  const [registered, setRegistered] = useStoredState('collobster_registered_live_classes', [
    'Build a realtime Flutter chat',
  ]);
  const [query, setQuery] = useState('');

  const toggleRegister = (title) => {
    if (registered.includes(title)) {
      setRegistered((prev) => prev.filter((t) => t !== title));
      a.toast(`Unregistered from "${title}"`);
    } else {
      setRegistered((prev) => [...prev, title]);
      a.notify('Live class registered', `${title} is on your learning calendar.`, 'Learn');
      a.toast(`You are registered for "${title}"! Check your schedule.`);
    }
  };

  const classes = (defaultLiveClasses && defaultLiveClasses.length > 0)
    ? defaultLiveClasses
    : [
        { title: 'Build a realtime Flutter chat', mentor: 'Aman Verma', day: 25, time: '7:00 PM', attendees: 207 },
        { title: 'UI/UX design principles', mentor: 'Riya Sharma', day: 26, time: '6:30 PM', attendees: 184 },
        { title: 'System design basics', mentor: 'Neha Patel', day: 27, time: '8:00 PM', attendees: 295 },
      ];

  const q = query.trim().toLowerCase();

  const filteredRegistered = registered.filter((regTitle) => {
    if (!q) return true;
    return regTitle.toLowerCase().includes(q);
  });

  const filteredUpcoming = classes.filter((cls) => {
    if (!q) return true;
    const titleMatch = (cls.title || '').toLowerCase().includes(q);
    const mentorMatch = (cls.mentor || '').toLowerCase().includes(q);
    const catMatch = (cls.category || '').toLowerCase().includes(q);
    const tagMatch = Array.isArray(cls.tags) && cls.tags.some((t) => t.toLowerCase().includes(q));
    return titleMatch || mentorMatch || catMatch || tagMatch;
  });

  return (
    <section className="learn-section">
      <div className="learn-banner">
        <div>
          <span className="eyebrow">LIVE CLASSES</span>
          <h2>Learn with a little room for questions.</h2>
          <p>
            Small live sessions with mentors and peers. Registration stays
            visible on your learning path.
          </p>
        </div>
        <CalendarDays size={66} />
      </div>

      <div className="space-controls" style={{ margin: '16px 0 20px' }}>
        <SearchField
          value={query}
          onChange={setQuery}
          placeholder="Search live classes by title, mentor, topic..."
        />
      </div>

      {filteredRegistered.length > 0 && (
        <div style={{ marginBottom: 20 }}>
          <SectionTitle title={`Your Registered Classes (${filteredRegistered.length})`} />
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {filteredRegistered.map((regTitle) => (
              <div
                key={regTitle}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: 14,
                  background: 'var(--color-surface, #fff)',
                  border: '1px solid var(--accent, #e34d42)',
                  borderRadius: 8,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <CheckCircle2 size={20} color="#059669" />
                  <div>
                    <b style={{ display: 'block', fontSize: '0.95rem' }}>{regTitle}</b>
                    <small style={{ color: 'var(--color-muted, #71767b)' }}>Confirmed registration · Live link active 15m before start</small>
                  </div>
                </div>
                <div style={{ display: 'flex', gap: 8 }}>
                  <Button size="sm" onClick={() => a.toast('Joining live video room...')}>
                    Join room
                  </Button>
                  <Button size="sm" variant="quiet" onClick={() => toggleRegister(regTitle)}>
                    Cancel
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      <SectionTitle title="Upcoming Schedule" />
      {filteredUpcoming.length ? (
        <div className="result-list">
          {filteredUpcoming.map((cls) => {
            const isReg = registered.includes(cls.title);
            return (
              <article className="learn-list-row" key={cls.title}>
                <span className="date-tile">
                  <small>{cls.month || 'SEP'}</small>
                  <b>{cls.day}</b>
                </span>
                <div>
                  <h3>{cls.title}</h3>
                  <p>{cls.mentor} · {cls.time} · Online</p>
                  <small>{cls.attendees + (isReg ? 1 : 0)} registered</small>
                </div>
                <Button
                  variant={isReg ? 'quiet' : 'primary'}
                  onClick={() => toggleRegister(cls.title)}
                >
                  {isReg ? 'Registered ✓' : 'Register'}
                </Button>
              </article>
            );
          })}
        </div>
      ) : (
        <Empty
          title="No live classes found"
          text={q ? `No live classes matching "${query}".` : 'No upcoming classes scheduled.'}
          action={q ? 'Clear search' : undefined}
          onAction={q ? () => setQuery('') : undefined}
        />
      )}
    </section>
  );
}

function PracticeLabs() {
  const a = useApp();
  const [activeLabs, setActiveLabs] = useStoredState('collobster_active_practice_labs', [
    { title: 'Build a to-do app', level: 'Beginner', tag: 'Flutter', progress: 65 },
  ]);
  const [query, setQuery] = useState('');

  const toggleLab = (title, level, tag) => {
    const existing = activeLabs.find((l) => l.title === title);
    if (existing) {
      a.toast(`Lab "${title}" is already in your active queue.`);
    } else {
      setActiveLabs((prev) => [...prev, { title, level, tag, progress: 10 }]);
      a.notify('Practice lab started', `${title} has been added to your learning queue.`, 'Learn');
      a.toast(`Practice lab "${title}" started!`);
    }
  };

  const labCatalog = [
    ['Build a to-do app', 'Beginner', 'Flutter'],
    ['REST API integration', 'Intermediate', 'React'],
    ['Firebase authentication', 'Intermediate', 'Flutter'],
    ['Accessible form audit', 'Beginner', 'Design'],
    ['AI Agent workflow pipeline', 'Intermediate', 'Python'],
    ['Distributed microservices sandbox', 'Advanced', 'Backend'],
  ];

  const q = query.trim().toLowerCase();

  const filteredActive = activeLabs.filter((lab) => {
    if (!q) return true;
    return (
      (lab.title || '').toLowerCase().includes(q) ||
      (lab.tag || '').toLowerCase().includes(q) ||
      (lab.level || '').toLowerCase().includes(q)
    );
  });

  const filteredCatalog = labCatalog.filter(([title, level, tag]) => {
    if (!q) return true;
    return (
      title.toLowerCase().includes(q) ||
      level.toLowerCase().includes(q) ||
      tag.toLowerCase().includes(q)
    );
  });

  return (
    <section className="learn-section">
      <div className="learn-banner">
        <div>
          <span className="eyebrow">PRACTICE LABS</span>
          <h2>Build. Test. Learn by doing.</h2>
          <p>
            Short, bounded exercises that turn a lesson into an artifact you can
            share.
          </p>
        </div>
        <FolderKanban size={66} />
      </div>

      <div className="space-controls" style={{ margin: '16px 0 20px' }}>
        <SearchField
          value={query}
          onChange={setQuery}
          placeholder="Search practice labs by title, tech stack (Flutter, React), level..."
        />
      </div>

      {filteredActive.length > 0 && (
        <div style={{ marginBottom: 22 }}>
          <SectionTitle title={`Your Active Labs (${filteredActive.length})`} />
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 14 }}>
            {filteredActive.map((lab) => (
              <div
                key={lab.title}
                style={{
                  background: 'var(--color-surface, #fff)',
                  border: '1px solid var(--accent, #e34d42)',
                  borderRadius: 10,
                  padding: 14,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 8,
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <Tag tone="purple">{lab.tag}</Tag>
                  <span style={{ fontSize: '0.78rem', color: '#059669', fontWeight: 700 }}>In Progress</span>
                </div>
                <h4 style={{ margin: 0, fontSize: '1rem' }}>{lab.title}</h4>
                <Progress value={lab.progress || 30} />
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 6 }}>
                  <small style={{ color: 'var(--color-muted, #71767b)' }}>{lab.progress || 30}% complete</small>
                  <Button size="sm" onClick={() => a.toast(`Resuming interactive sandbox for ${lab.title}...`)}>
                    Resume lab
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      <SectionTitle title="Available Labs" />
      {filteredCatalog.length > 0 ? (
        <div className="card-grid">
          {filteredCatalog.map(([title, level, tag], i) => {
            const isActive = activeLabs.some((l) => l.title === title);
            return (
              <article className="project-card" key={title}>
                <span className={`module-icon ${colors[i % colors.length]}`}>
                  <Play size={20} />
                </span>
                <h3>{title}</h3>
                <p>
                  Complete a practical brief, then share the outcome with your
                  learning circle.
                </p>
                <div>
                  <Tag>{level}</Tag>
                  <Tag>{tag}</Tag>
                </div>
                <Button
                  variant={isActive ? 'quiet' : 'primary'}
                  onClick={() => toggleLab(title, level, tag)}
                >
                  {isActive ? 'Continue lab' : 'Start lab'}
                </Button>
              </article>
            );
          })}
        </div>
      ) : (
        <Empty
          title="No practice labs found"
          text={q ? `No practice labs matching "${query}".` : 'No labs currently available.'}
          action={q ? 'Clear search' : undefined}
          onAction={q ? () => setQuery('') : undefined}
        />
      )}
    </section>
  );
}

function StudyGroups() {
  const a = useApp();
  const [query, setQuery] = useState('');

  const groups = (defaultStudyGroups && defaultStudyGroups.length > 0)
    ? defaultStudyGroups
    : [
        { id: 'group-flutter', title: 'Flutter builders', members: 42, level: 'Beginner', tags: ['Flutter'] },
        { id: 'group-fullstack', title: 'Full-stack study circle', members: 69, level: 'Intermediate', tags: ['React', 'Node'] },
        { id: 'group-datascience', title: 'Data science learners', members: 96, level: 'Beginner', tags: ['Python'] },
        { id: 'group-design', title: 'Design critique club', members: 123, level: 'Intermediate', tags: ['Design'] },
      ];

  const q = query.trim().toLowerCase();

  const filteredGroups = groups.filter((g) => {
    if (!q) return true;
    const titleMatch = (g.title || '').toLowerCase().includes(q);
    const categoryMatch = (g.category || '').toLowerCase().includes(q);
    const descMatch = (g.description || '').toLowerCase().includes(q);
    const levelMatch = (g.level || '').toLowerCase().includes(q);
    const tagMatch = Array.isArray(g.tags) && g.tags.some((t) => t.toLowerCase().includes(q));
    return titleMatch || categoryMatch || descMatch || levelMatch || tagMatch;
  });

  return (
    <section className="learn-section">
      <div className="learn-banner">
        <div>
          <span className="eyebrow">STUDY GROUPS</span>
          <h2>Good company makes practice stick.</h2>
          <p>
            Join a study group, meet peers around a shared focus, and make the
            next session visible.
          </p>
        </div>
        <Users size={66} />
      </div>

      <div className="space-controls" style={{ margin: '16px 0 20px' }}>
        <SearchField
          value={query}
          onChange={setQuery}
          placeholder="Search study groups by name, focus topic, or skill level..."
        />
      </div>

      {filteredGroups.length > 0 ? (
        <div className="result-list">
          {filteredGroups.map((group, index) => {
            const groupId = group.id || `study-${index}`;
            const joined = a.memberships[groupId] === 'member';
            return (
              <article className="learn-list-row" key={group.title || group.id}>
                <span className={`module-icon ${colors[index % colors.length]}`}>
                  <Users size={20} />
                </span>
                <div>
                  <h3>{group.title}</h3>
                  <p>{group.members || (42 + index * 27)} members · {group.schedule || 'Active now'}</p>
                  <Tag>{group.level || (index % 2 ? 'Intermediate' : 'Beginner')}</Tag>
                </div>
                <Button
                  variant={joined ? 'quiet' : 'primary'}
                  onClick={() => {
                    a.setMemberships((old) => ({
                      ...old,
                      [groupId]: joined ? undefined : 'member',
                    }));
                    a.toast(
                      joined
                        ? `Left ${group.title}`
                        : `You joined ${group.title}!`
                    );
                  }}
                >
                  {joined ? 'Joined ✓' : 'Join group'}
                </Button>
              </article>
            );
          })}
        </div>
      ) : (
        <Empty
          title="No study groups found"
          text={q ? `No study groups matching "${query}".` : 'No study groups currently available.'}
          action={q ? 'Clear search' : undefined}
          onAction={q ? () => setQuery('') : undefined}
        />
      )}
    </section>
  );
}

function Resources() {
  const a = useApp();
  const [query, setQuery] = useState('');
  const [type, setType] = useState('All');

  const resources = [
    ['Flutter widget cheat sheet', 'PDF · 1.2 MB', 'Cheat sheets'],
    ['System design guide', 'Article · 12 min', 'Articles'],
    ['UI/UX design templates', 'ZIP · 5 MB', 'Templates'],
    ['SQL quick reference', 'PDF · 1.5 MB', 'Cheat sheets'],
    ['Spring Boot notes', 'Article · 10 min', 'Articles'],
    ['Flutter animations walkthrough', 'Video · 18 min', 'Videos'],
  ];

  const shown = resources.filter(
    ([title, meta, kind]) =>
      (type === 'All' || kind === type) &&
      `${title} ${meta}`.toLowerCase().includes(query.toLowerCase())
  );

  return (
    <section className="learn-section">
      <div className="space-controls">
        <SearchField
          value={query}
          onChange={setQuery}
          placeholder="Search resources"
        />
        <Tabs
          items={['All', 'Articles', 'Cheat sheets', 'Templates', 'Videos']}
          value={type}
          onChange={setType}
        />
      </div>
      <div className="result-list">
        {shown.length ? (
          shown.map(([title, meta], i) => (
            <article className="learn-list-row" key={title}>
              <span className={`module-icon ${colors[i % 4]}`}>
                <ReceiptText size={20} />
              </span>
              <div>
                <h3>{title}</h3>
                <p>{meta}</p>
              </div>
              <Button
                onClick={() =>
                  a.toast(
                    `${title} is ready to ${
                      meta.startsWith('Article') ? 'read' : 'download'
                    }.`
                  )
                }
              >
                {meta.startsWith('Article') ? 'Read' : 'Download'}
              </Button>
            </article>
          ))
        ) : (
          <Empty
            title="No resources found"
            text="Try another topic or reset the resource type."
          />
        )}
      </div>
    </section>
  );
}

function Certificates() {
  const a = useApp();
  const [query, setQuery] = useState('');

  const catalog = [
    ...(a.courseCatalog || []),
    ...courses.filter(
      (course) => !(a.courseCatalog || []).some((remote) => remote.id === course.id)
    ),
  ];
  const completedCourses = catalog.filter(
    (course) =>
      (a.learning[course.id]?.length || 0) === course.lessons.length
  );
  const issuedCertificates = a.certificates || [];
  const allCerts = completedCourses.map((course) => {
    const issued = issuedCertificates.find((c) => c.courseId === course.id);
    return { course, certificate: issued };
  });

  const q = query.trim().toLowerCase();

  const filteredCerts = allCerts.filter(({ course, certificate }) => {
    if (!q) return true;
    const titleMatch = (course.title || '').toLowerCase().includes(q);
    const mentorMatch = (course.mentor || '').toLowerCase().includes(q);
    const certNumMatch = certificate?.certificateNumber
      ? String(certificate.certificateNumber).toLowerCase().includes(q)
      : false;
    return titleMatch || mentorMatch || certNumMatch;
  });

  return (
    <section className="learn-section">
      <div className="certificate-hero">
        <span className="module-icon gold">
          <CheckCircle2 />
        </span>
        <div>
          <span className="eyebrow">CERTIFICATES &amp; CREDENTIALS</span>
          <h2>Keep a record of what you finish.</h2>
          <p>
            Official certificates with public cryptographic verification hashes
            become available after completing 100% of the lessons in a course.
          </p>
        </div>
      </div>

      <div className="space-controls" style={{ margin: '16px 0 20px' }}>
        <SearchField
          value={query}
          onChange={setQuery}
          placeholder="Search certificates by course title, credential ID, or mentor..."
        />
      </div>

      {allCerts.length > 0 ? (
        filteredCerts.length > 0 ? (
          <div className="result-list">
            {filteredCerts.map(({ course, certificate }) => (
              <article className="learn-list-row" key={course.id}>
                <span className={`course-mark ${course.color}`}>
                  {course.symbol}
                </span>
                <div>
                  <h3>{course.title}</h3>
                  <p>
                    {certificate
                      ? `Verified Certificate #${certificate.certificateNumber}`
                      : 'Completed · Certificate ready to claim'}
                  </p>
                  {certificate && (
                    <small className="muted">
                      Issued {new Date(certificate.issuedAt).toLocaleDateString()}
                    </small>
                  )}
                </div>
                <Button
                  variant="primary"
                  onClick={() =>
                    a.setModal({
                      type: 'details',
                      kind: 'certificate',
                      item: course,
                      certificate,
                    })
                  }
                >
                  View certificate
                </Button>
              </article>
            ))}
          </div>
        ) : (
          <Empty
            title="No certificates match your search"
            text={`No certificates found matching "${query}".`}
            action="Clear search"
            onAction={() => setQuery('')}
          />
        )
      ) : (
        <Empty
          title="Your first certificate is within reach"
          text="Complete every lesson in a course to unlock a verified learning record."
          action="Continue learning"
          onAction={() =>
            a.setModal({
              type: 'details',
              kind: 'course',
              item: catalog[0] || courses[0],
            })
          }
        />
      )}
    </section>
  );
}

function LearnCollection({ title, rows, action }) {
  return (
    <section className="learn-collection">
      <SectionTitle title={title} action="View all" onAction={action} />
      {rows.map((row, index) => (
        <button key={row} onClick={action}>
          <span>{index + 1}</span>
          <strong>{row}</strong>
          <ArrowRight size={15} />
        </button>
      ))}
    </section>
  );
}

function MentorList() {
  return (
    <div className="result-list">
      {people
        .filter((p) => ['riya', 'arjun'].includes(p.id))
        .map((p) => (
          <div className="mentor-row" key={p.id}>
            <Avatar person={p} size="large" />
            <div>
              <strong>{p.name}</strong>
              <p>{p.role}</p>
              <Tag tone="gold">4.9 rating</Tag>
            </div>
            <Button>Book a conversation</Button>
          </div>
        ))}
    </div>
  );
}

function SkillRows({ skills }) {
  return (
    <div className="skill-rows">
      {skills.map((s, i) => (
        <div key={s}>
          <span>{s}</span>
          <Progress value={90 - i * 9} />
        </div>
      ))}
    </div>
  );
}

// ─── 3. Career Space (Exported) ─────────────────────────────────────────────

export function Career() {
  const a = useApp();
  const [tab, setTab] = useState('Jobs');
  const [query, setQuery] = useState('');

  const allJobs = [
    ...(a.careerJobs || []),
    ...jobs.filter(
      (job) => !(a.careerJobs || []).some((remote) => remote.id === job.id)
    ),
  ];
  const filtered = searchItems(allJobs, query);
  const savedJobs = allJobs.filter((job) => a.saved.includes(job.id));

  return (
    <>
      <PageHeader
        title="Your career deserves good company."
        description="Find work, practice the conversation, and make your next move with more clarity."
      >
        <Button
          variant="primary"
          icon={BriefcaseBusiness}
          onClick={() => a.go('Explore')}
        >
          Find opportunities
        </Button>
      </PageHeader>

      <div className="space-layout">
        <section>
          <ModuleHero
            icon={BriefcaseBusiness}
            eyebrow="YOUR CAREER SPACE"
            title="Build a career that feels like yours."
            text="Your profile is 78% ready to meet the right opportunity."
            action={
              <Button
                onClick={() =>
                  a.setModal({ type: 'details', kind: 'profile-completion' })
                }
              >
                Complete profile
              </Button>
            }
            art="icon"
          />

          <div className="career-actions">
            <button
              onClick={() => a.setModal({ type: 'details', kind: 'resume' })}
            >
              <ReceiptText />
              Build a resume
              <small>Make your story clear.</small>
            </button>
            <button
              onClick={() => a.setModal({ type: 'details', kind: 'interview' })}
            >
              <MessageCircle />
              Practice an interview
              <small>Rehearse the real conversation.</small>
            </button>
            <button
              onClick={() => a.setModal({ type: 'details', kind: 'alerts' })}
            >
              <Bell />
              Create an alert
              <small>Hear about relevant work.</small>
            </button>
          </div>

          <div className="space-controls">
            <Tabs
              items={['Jobs', 'Applications', 'Applicant review', 'Saved']}
              value={tab}
              onChange={setTab}
            />
            {tab === 'Jobs' && (
              <SearchField
                value={query}
                onChange={setQuery}
                placeholder="Search jobs, companies, skills"
              />
            )}
          </div>

          {tab === 'Jobs' && <JobList jobs={filtered} />}
          {tab === 'Applications' && <Applications jobs={allJobs} />}
          {tab === 'Applicant review' && <ApplicantReview jobs={allJobs} />}
          {tab === 'Saved' &&
            (savedJobs.length ? (
              <JobList jobs={savedJobs} />
            ) : (
              <Empty
                title="Your saved roles will appear here"
                text="Save a role to make space for a considered application."
                action="Explore jobs"
                onAction={() => setTab('Jobs')}
              />
            ))}
        </section>

        <aside className="space-rail">
          <SectionTitle title="Career strength" />
          <div className="score-circle">
            <b>78</b>
            <span>profile strength</span>
          </div>
          <SkillRows skills={a.profile.skills} />
          <SectionTitle title="Next small step" />
          <p className="muted">
            Add a project outcome to your profile. It makes your experience easier
            to understand.
          </p>
          <Button onClick={() => a.go('Profile')}>Edit profile</Button>
        </aside>
      </div>
    </>
  );
}

function labelState(s) {
  return String(s).replaceAll('_', ' ');
}

function JobList({ jobs: items }) {
  const a = useApp();
  return (
    <div className="list-surface">
      {items.map((job) => {
        const app = a.applications[job.id];
        const match = matchOpportunity(a.profile.skills, job.tags);
        return (
          <article className="job-row" key={job.id}>
            <span className={`module-icon ${job.color}`}>
              <BriefcaseBusiness size={20} />
            </span>
            <div>
              <h3>{job.title}</h3>
              <p>
                {job.company} · {job.location} · {job.mode}
              </p>
              <div>
                {job.tags.map((t) => (
                  <Tag key={t}>{t}</Tag>
                ))}
                <Tag tone="quiet">{job.salary}</Tag>
              </div>
              <small>
                {match.score}% skill match · {job.type}
              </small>
            </div>
            {app ? (
              <Status>{labelState(app)}</Status>
            ) : (
              <Button
                onClick={() =>
                  a.setModal({ type: 'details', kind: 'job', item: job })
                }
              >
                View role
              </Button>
            )}
          </article>
        );
      })}
    </div>
  );
}

function Applications({ jobs: items }) {
  const a = useApp();
  const applied = items.filter((j) => a.applications[j.id]);

  return applied.length ? (
    <div className="list-surface">
      {applied.map((j) => (
        <div className="job-row" key={j.id}>
          <span className={`module-icon ${j.color}`}>
            <BriefcaseBusiness size={20} />
          </span>
          <div>
            <h3>{j.title}</h3>
            <p>{j.company}</p>
          </div>
          <Status>{labelState(a.applications[j.id])}</Status>
        </div>
      ))}
    </div>
  ) : (
    <Empty
      title="No applications yet"
      text="The right role is easier to find when you know what you want to learn next."
      action="Browse roles"
      onAction={() => a.go('Explore')}
    />
  );
}

function ApplicantReview({ jobs: items }) {
  const a = useApp();
  const [selectedJobId, setSelectedJobId] = useState(items[0]?.id || null);
  const [loading, setLoading] = useState(false);
  const [notes, setNotes] = useState({});
  const [busyId, setBusyId] = useState(null);

  const selectedJob = items.find((j) => j.id === selectedJobId) || items[0];
  const apps = a.jobApplications[selectedJobId] || [];

  React.useEffect(() => {
    if (selectedJobId && a.fetchJobApplications) {
      setLoading(true);
      a.fetchJobApplications(selectedJobId).finally(() => setLoading(false));
    }
  }, [selectedJobId]);

  const updateStatus = async (appId, st) => {
    setBusyId(appId);
    try {
      await a.reviewJobApplication(selectedJobId, appId, st, notes[appId] || '');
      a.toast(`Candidate marked as ${labelState(st)}`);
    } catch (err) {
      a.toast(err.message || 'Could not update candidate status');
    } finally {
      setBusyId(null);
    }
  };

  return (
    <section className="applicant-review-space">
      <div className="applicant-review-header">
        <label>Select job listing to review candidate applications:</label>
        <select
          value={selectedJobId || ''}
          onChange={(e) => setSelectedJobId(e.target.value)}
        >
          {items.map((job) => (
            <option key={job.id} value={job.id}>
              {job.title} ({job.company})
            </option>
          ))}
        </select>
      </div>

      {loading ? (
        <p className="muted">Loading candidate applications…</p>
      ) : apps.length ? (
        <div className="applicant-review-list">
          {apps.map((app) => (
            <article className="applicant-review-card" key={app.id}>
              <div className="applicant-review-card-head">
                <div>
                  <strong>
                    Applicant ID:{' '}
                    {app.candidateId ? app.candidateId.slice(0, 8) : 'Candidate'}
                  </strong>
                  <small>
                    Submitted{' '}
                    {new Date(app.createdAt || Date.now()).toLocaleDateString(
                      [],
                      { dateStyle: 'medium' }
                    )}
                  </small>
                </div>
                <span className={`status-pill status-${app.state?.toLowerCase()}`}>
                  {labelState(app.state)}
                </span>
              </div>

              {app.note && <p className="applicant-note">“{app.note}”</p>}
              {app.resumeObjectId ? (
                <div className="resume-badge">
                  <FileText size={15} />
                  <span>
                    Resume attached (ID: {app.resumeObjectId.slice(0, 8)}…)
                  </span>
                </div>
              ) : (
                <span className="muted-small">No resume attached</span>
              )}

              {app.reviewNotes && (
                <p className="applicant-notes-preview">
                  <b>Notes:</b> {app.reviewNotes}
                </p>
              )}

              <div className="applicant-review-actions">
                <input
                  type="text"
                  placeholder="Add internal review notes or next steps…"
                  value={
                    notes[app.id] !== undefined
                      ? notes[app.id]
                      : app.reviewNotes || ''
                  }
                  onChange={(e) =>
                    setNotes({ ...notes, [app.id]: e.target.value })
                  }
                  className="review-notes-input"
                />
                <div className="button-group">
                  {['reviewing', 'shortlisted', 'rejected', 'offered'].map(
                    (st) => (
                      <Button
                        key={st}
                        variant={app.state === st ? 'primary' : 'quiet'}
                        disabled={busyId === app.id}
                        onClick={() => updateStatus(app.id, st)}
                      >
                        {labelState(st)}
                      </Button>
                    )
                  )}
                </div>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <Empty
          title="No applications yet"
          text={`No candidates have applied for "${
            selectedJob?.title || 'this role'
          }" yet.`}
        />
      )}
    </section>
  );
}

// ─── 4. Projects Space (Exported) ───────────────────────────────────────────

export function Projects() {
  const a = useApp();
  const [tab, setTab] = useState('Discover');
  const [screen, setScreen] = useState('list');

  const drafts = a.projectDrafts || [];
  const mine = [
    ...drafts,
    ...projects.filter(
      (p) => a.projectState[p.id] && !drafts.some((draft) => draft.id === p.id)
    ),
  ];

  const list =
    tab === 'My projects'
      ? mine
      : tab === 'Saved'
      ? projects.filter((p) => a.saved.includes(p.id))
      : projects;

  const create = () => {
    setScreen('create');
    setTab('My projects');
  };

  if (screen === 'create') {
    return (
      <ProjectCreate
        onCancel={() => setScreen('list')}
        onCreate={async (project) => {
          try {
            const created = await a.createProject(project);
            a.toast(
              created.mode === 'synced'
                ? 'Code space created and saved to your account.'
                : 'Code space created in Preview.'
            );
            setScreen(created.id);
          } catch {
            a.toast('Could not create this code space. Please try again.');
          }
        }}
      />
    );
  }

  if (screen !== 'list') {
    const project =
      drafts.find((p) => p.id === screen) ||
      projects.find((p) => p.id === screen);
    return <CodeWorkspace project={project} onBack={() => setScreen('list')} />;
  }

  return (
    <>
      <PageHeader
        title="Turn a useful idea into working software."
        description="Projects are code spaces: plan a small outcome, connect tools, build a branch, and make the work visible."
      >
        <Button variant="primary" icon={Plus} onClick={create}>
          Create code space
        </Button>
      </PageHeader>

      <div className="space-layout">
        <section>
          <div className="project-banner project-code-banner">
            <div>
              <span className="eyebrow">PROJECT STUDIO</span>
              <h2>Start with a problem. Build the smallest useful answer.</h2>
              <p>
                Create a code space with a brief, a stack, collaborators,
                extensions, and a place to ship the first version.
              </p>
              <Button icon={Code2} onClick={create}>
                Open new workspace
              </Button>
            </div>
            <div className="project-banner-tools">
              <Code2 />
              <GitBranch />
              <Puzzle />
            </div>
          </div>

          <Tabs
            items={['Discover', 'My projects', 'Saved']}
            value={tab}
            onChange={setTab}
          />

          {tab === 'My projects' && !list.length ? (
            <ProjectEmpty onCreate={create} />
          ) : (
            <div className="card-grid">
              {list.map((project) => {
                const joined = a.projectState[project.id];
                const match = matchOpportunity(
                  a.profile.skills,
                  project.tags || []
                );

                return (
                  <article className="project-card" key={project.id}>
                    <span className={`module-icon ${project.color}`}>
                      <FolderKanban size={22} />
                    </span>
                    <h3>{project.title}</h3>
                    <p>{project.description}</p>
                    <div className="member-stack">
                      {people
                        .slice(0, Math.min(project.members || 1, 4))
                        .map((p) => (
                          <Avatar person={p} key={p.id} />
                        ))}
                      <small>{project.members || 1} collaborators</small>
                    </div>
                    <Progress
                      value={project.progress || 0}
                      label={`${project.title} progress`}
                    />
                    <div className="project-footer">
                      <small>
                        {joined
                          ? 'Your code space'
                          : `${match.score}% skill match`}
                      </small>
                      <Button
                        onClick={() =>
                          joined ||
                          drafts.some((p) => p.id === project.id)
                            ? setScreen(project.id)
                            : a.setModal({
                                type: 'details',
                                kind: 'project',
                                item: project,
                              })
                        }
                      >
                        {joined ? 'Open studio' : 'View project'}
                      </Button>
                    </div>
                  </article>
                );
              })}
            </div>
          )}
        </section>

        <aside className="space-rail">
          <SectionTitle title="A practical build flow" />
          <ol className="steps">
            <li>Describe the problem and first user.</li>
            <li>Choose a starter stack and extensions.</li>
            <li>Plan the first branch and tasks.</li>
            <li>Publish a clear progress update.</li>
          </ol>
          <section className="callout">
            <Terminal size={23} />
            <h3>Built for making</h3>
            <p>
              Your project space includes a focused editor, files, extensions,
              and a deploy checklist.
            </p>
            <Button onClick={create}>Start a workspace</Button>
          </section>
        </aside>
      </div>
    </>
  );
}

function ProjectEmpty({ onCreate }) {
  return (
    <section className="project-empty">
      <span className="module-icon blue">
        <Code2 size={28} />
      </span>
      <div>
        <span className="eyebrow">YOUR FIRST CODE SPACE</span>
        <h2>No projects yet. Make a small one real.</h2>
        <p>
          Start from a brief, choose the tools you need, and get a calm workspace
          ready for the first commit.
        </p>
      </div>
      <div className="project-empty-actions">
        <Button icon={Plus} onClick={onCreate}>
          Create from brief
        </Button>
        <Button variant="ghost" icon={PackageOpen} onClick={onCreate}>
          Use starter stack
        </Button>
      </div>
    </section>
  );
}

function ProjectCreate({ onCancel, onCreate }) {
  const [step, setStep] = useState(0);
  const [draft, setDraft] = useState({
    title: '',
    problem: '',
    stack: 'React + Vite',
    visibility: 'Private',
  });

  const finish = () =>
    onCreate({
      id: `project-${Date.now()}`,
      title: draft.title || 'Untitled build',
      description:
        draft.problem || 'A new code space ready for its first commit.',
      category: 'Code space',
      tags: draft.stack.split(' + '),
      roles: 'Builder',
      members: 1,
      progress: 8,
      color: 'blue',
      tasks: [],
    });

  return (
    <main className="project-create-page">
      <button className="back-link" onClick={onCancel}>
        ← Projects
      </button>
      <section className="project-create-card">
        <div className="project-create-intro">
          <span className="module-icon blue">
            <Code2 />
          </span>
          <span className="eyebrow">NEW CODE SPACE</span>
          <h1>
            {
              [
                'Name the outcome',
                'Choose a starter stack',
                'Review your workspace',
              ][step]
            }
          </h1>
          <p>
            Give the first version enough structure to be useful without
            over-planning it.
          </p>
        </div>

        <div className="stepper">
          {['Brief', 'Stack', 'Review'].map((name, i) => (
            <span
              className={step === i ? 'active' : step > i ? 'done' : ''}
              key={name}
            >
              <i>{i + 1}</i>
              {name}
            </span>
          ))}
        </div>

        {step === 0 && (
          <div className="form-stack">
            <Field label="Project name">
              <input
                value={draft.title}
                onChange={(e) => setDraft({ ...draft, title: e.target.value })}
                placeholder="e.g. Neighbourhood repair planner"
              />
            </Field>
            <Field label="What problem are you solving?">
              <textarea
                rows="4"
                value={draft.problem}
                onChange={(e) =>
                  setDraft({ ...draft, problem: e.target.value })
                }
                placeholder="Describe the first useful outcome for a real person."
              />
            </Field>
          </div>
        )}

        {step === 1 && (
          <div className="form-stack">
            <Field label="Starter stack">
              <select
                value={draft.stack}
                onChange={(e) => setDraft({ ...draft, stack: e.target.value })}
              >
                <option>React + Vite</option>
                <option>Next.js + TypeScript</option>
                <option>Flutter + Firebase</option>
                <option>Node.js API</option>
              </select>
            </Field>
            <Field label="Workspace visibility">
              <select
                value={draft.visibility}
                onChange={(e) =>
                  setDraft({ ...draft, visibility: e.target.value })
                }
              >
                <option>Private</option>
                <option>Share with collaborators</option>
                <option>Public</option>
              </select>
            </Field>
            <div className="extension-picker">
              {['ESLint', 'Prettier', 'GitLens', 'Tailwind CSS IntelliSense'].map(
                (name, i) => (
                  <label key={name}>
                    <input type="checkbox" defaultChecked={i < 3} />
                    {name}
                  </label>
                )
              )}
            </div>
          </div>
        )}

        {step === 2 && (
          <div className="workspace-review">
            <Code2 />
            <div>
              <b>{draft.title || 'Untitled build'}</b>
              <small>
                {draft.stack} · {draft.visibility}
              </small>
            </div>
            <p>
              {draft.problem ||
                'Your workspace will start with a concise project brief and a first task board.'}
            </p>
          </div>
        )}

        <div className="dialog-actions">
          <Button
            variant="quiet"
            onClick={() => (step ? setStep(step - 1) : onCancel())}
          >
            {step ? 'Back' : 'Cancel'}
          </Button>
          <Button onClick={() => (step === 2 ? finish() : setStep(step + 1))}>
            {step === 2 ? 'Create code space' : 'Continue'}
          </Button>
        </div>
      </section>
    </main>
  );
}

function CodeWorkspace({ project, onBack }) {
  const a = useApp();
  const [topTab, setTopTab] = useState('Code Space');
  const [panel, setPanel] = useState('Explorer');
  const [filePath, setFilePath] = useState('app.tsx');
  const [codeDraft, setCodeDraft] = useState('');
  const [saved, setSaved] = useState(true);
  const [creatingFile, setCreatingFile] = useState(false);
  const [newPath, setNewPath] = useState('');
  const [output, setOutput] = useState('');

  const seededFiles = useMemo(
    () => [
      {
        id: 'f-java-main',
        path: 'src/main/java/com/collobster/Application.java',
        content: `package com.collobster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @GetMapping("/api/status")
    public String status() {
        return "{\\"status\\":\\"UP\\",\\"engine\\":\\"Java 21 Virtual Threads\\",\\"service\\":\\"Collobster Backend\\"}";
    }
}
`,
      },
      {
        id: 'f-java-service',
        path: 'src/main/java/com/collobster/service/CollaboratorService.java',
        content: `package com.collobster.service;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CollaboratorService {

    public record Builder(String id, String name, String stack, int contributions) {}

    public List<Builder> getActiveBuilders() {
        return List.of(
            new Builder("b1", "Aman Verma", "Flutter & Dart", 48),
            new Builder("b2", "Neha Patel", "Java & Spring Boot", 92),
            new Builder("b3", "Riya Sharma", "React & Design Systems", 64)
        );
    }
}
`,
      },
      {
        id: 'f-pom',
        path: 'pom.xml',
        content: `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.4</version>
        <relativePath/>
    </parent>
    <groupId>com.collobster</groupId>
    <artifactId>collobster-backend</artifactId>
    <version>1.0.0</version>
    <name>Collobster Backend</name>
    <properties>
        <java.version>21</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>
`,
      },
      {
        id: 'f1',
        path: 'app.tsx',
        content: `export function App() {\n  return <Workspace project="${
          project?.title || 'New build'
        }" />;\n}`,
      },
      {
        id: 'f2',
        path: 'components/Header.tsx',
        content: `export function Header() {\n  return <header>First useful release</header>;\n}`,
      },
      {
        id: 'f3',
        path: 'styles.css',
        content:
          ':root {\n  --accent: #5b3df5;\n}\n\n.workspace { display: grid; }',
      },
      {
        id: 'f4',
        path: 'README.md',
        content: `# ${
          project?.title || 'New build'
        }\n\n## First release\n- [ ] Define the main user action\n- [ ] Build the first screen`,
      },
    ],
    [project?.title]
  );

  const projectFilesList = useMemo(() => {
    const custom = (a.projectFiles && a.projectFiles[project?.id]) || [];
    const combined = [...custom];
    for (const sf of seededFiles) {
      if (!combined.some((f) => f.path === sf.path)) combined.push(sf);
    }
    return combined;
  }, [a.projectFiles, project?.id, seededFiles]);

  const activeFile = useMemo(
    () =>
      projectFilesList.find((f) => f.path === filePath) ||
      projectFilesList[0] ||
      seededFiles[0],
    [projectFilesList, filePath, seededFiles]
  );

  React.useEffect(() => {
    if (activeFile) {
      setCodeDraft(activeFile.content);
      setSaved(true);
    }
  }, [activeFile?.path]);

  const handleSave = async () => {
    try {
      await a.saveProjectFile?.(project.id, {
        path: activeFile.path,
        content: codeDraft,
        fileType: activeFile.path.endsWith('.java')
          ? 'java'
          : activeFile.path.endsWith('.xml')
          ? 'xml'
          : activeFile.path.endsWith('.css')
          ? 'css'
          : activeFile.path.endsWith('.md')
          ? 'markdown'
          : 'typescript',
      });
      setSaved(true);
      a.toast(`Saved ${activeFile.path}`);
    } catch (err) {
      a.toast(err.message || 'Could not save file.');
    }
  };

  const handleCreateFile = async (e) => {
    e.preventDefault();
    if (!newPath.trim()) return;
    const path = newPath.trim();
    const initContent = path.endsWith('.java')
      ? `package com.collobster;\n\npublic class ${path.replace(/^.*[\\/]/, '').replace('.java', '')} {\n    // Implementation\n}\n`
      : `// ${path}\nexport default {};\n`;
    try {
      await a.saveProjectFile?.(project.id, {
        path,
        content: initContent,
        fileType: path.endsWith('.java') ? 'java' : 'text',
      });
      setFilePath(path);
      setCodeDraft(initContent);
      setCreatingFile(false);
      setNewPath('');
      a.toast(`Created ${path}`);
    } catch (err) {
      a.toast(err.message || 'Could not create file.');
    }
  };

  const handleDeleteFile = async (f) => {
    if (window.confirm(`Delete ${f.path}?`)) {
      try {
        if (f.id && !f.id.startsWith('f')) {
          await a.deleteProjectFile?.(project.id, f.id);
        }
        a.toast(`Deleted ${f.path}`);
        if (filePath === f.path) setFilePath('src/main/java/com/collobster/Application.java');
      } catch (err) {
        a.toast(err.message || 'Could not delete file.');
      }
    }
  };

  const runCode = () => {
    try {
      if (activeFile.path.endsWith('.java') || activeFile.path.endsWith('pom.xml')) {
        setOutput(
          `[INFO] ------------------------------------------------------------------------\n` +
          `[INFO] Building Collobster Enterprise Service with Maven & Java 21\n` +
          `[INFO] ------------------------------------------------------------------------\n` +
          `[INFO] Compiling source files with javac 21.0.3 (Virtual Threads enabled)\n` +
          `[INFO] Running com.collobster.ApplicationTests\n` +
          `[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.624 s\n` +
          `[INFO] ------------------------------------------------------------------------\n` +
          `[INFO] BUILD SUCCESS\n` +
          `[INFO] ------------------------------------------------------------------------\n` +
          `[INFO] Total time: 1.120 s\n` +
          `[INFO] Embedded Tomcat started on port 8080 (http)\n` +
          `[INFO] Live API: http://localhost:8080/api/status -> {\"status\":\"UP\"}`
        );
        a.toast('Java 21 / Spring Boot build passed cleanly!');
      } else {
        setOutput(
          `Running ${activeFile.path}...\n✓ Syntax check passed.\n✓ Bundled in 18ms without errors.`
        );
        a.toast('Project code verified!');
      }
    } catch (err) {
      setOutput(`Error: ${err.message}`);
    }
  };

  return (
    <main className="code-workspace">
      <header>
        <button className="back-link" onClick={onBack}>
          ← Projects
        </button>
        <div className="code-workspace-title-box">
          <Code2 size={18} />
          <span>{project?.title || 'Code space'}</span>
          <Tag tone="purple">main</Tag>
        </div>
        <div className="code-workspace-nav">
          <Tabs
            items={['Code Space', 'Tasks', 'Roadmap', 'Terminal']}
            value={topTab}
            onChange={setTopTab}
          />
        </div>
        <div className="code-header-actions">
          {topTab === 'Code Space' && (
            <Button
              variant={saved ? 'ghost' : 'primary'}
              onClick={handleSave}
              disabled={saved}
            >
              {saved ? 'Saved' : 'Save file'}
            </Button>
          )}
          {topTab === 'Code Space' && (
            <Button variant="secondary" onClick={runCode}>
              Run / Test
            </Button>
          )}
        </div>
      </header>

      {topTab === 'Code Space' && (
        <div className="code-workspace-body">
          <nav className="code-activity">
            {[
              [PanelsTopLeft, 'Explorer'],
              [Puzzle, 'Extensions'],
              [GitBranch, 'Source control'],
              [Terminal, 'Terminal'],
            ].map(([Icon, name]) => (
              <button
                key={name}
                className={panel === name ? 'active' : ''}
                title={name}
                onClick={() => setPanel(name)}
              >
                <Icon size={19} />
              </button>
            ))}
          </nav>
          <aside className="code-sidebar">
            <div className="sidebar-subhead">
              <b>{panel}</b>
              {panel === 'Explorer' && (
                <button
                  className="icon-tiny-btn"
                  title="Create file"
                  onClick={() => setCreatingFile(true)}
                >
                  <Plus size={14} />
                </button>
              )}
            </div>
            {panel === 'Explorer' && (
              <div className="file-tree-list">
                {creatingFile && (
                  <form className="file-new-form" onSubmit={handleCreateFile}>
                    <input
                      autoFocus
                      placeholder="path/file.ext"
                      value={newPath}
                      onChange={(e) => setNewPath(e.target.value)}
                      onBlur={() => !newPath && setCreatingFile(false)}
                    />
                  </form>
                )}
                {projectFilesList.map((f) => (
                  <div
                    key={f.path}
                    className={`file-tree-item ${
                      filePath === f.path ? 'active' : ''
                    }`}
                    onClick={() => {
                      setFilePath(f.path);
                    }}
                  >
                    <span>{f.path}</span>
                    {f.id && !f.id.startsWith('f') && (
                      <button
                        className="file-del-btn"
                        title="Delete file"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteFile(f);
                        }}
                      >
                        <Trash2 size={13} />
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}
            {panel === 'Extensions' &&
              ['ESLint', 'Prettier', 'GitLens', 'Tailwind CSS'].map(
                (name, i) => (
                  <button key={name} className="extension-row">
                    <span>
                      <Puzzle size={15} />
                      {name}
                    </span>
                    <small>{i < 3 ? 'Enabled' : 'Install'}</small>
                  </button>
                )
              )}
            {panel === 'Source control' && (
              <div className="source-control-box">
                <p>
                  Branch: <strong>main</strong>
                </p>
                <button className="commit-row">
                  + Initial project brief
                </button>
                <button className="commit-row">+ Starter interface</button>
              </div>
            )}
            {panel === 'Terminal' && (
              <pre className="terminal-box">
                $ npm run dev{'\n'}Local: http://localhost:5173{'\n'}ready in
                342ms
              </pre>
            )}
          </aside>
          <section className="code-editor">
            <div className="code-tabs">
              <button className="active-tab">{activeFile.path}</button>
              <small>
                {saved ? '✓ All changes saved' : '● Unsaved changes'}
              </small>
            </div>
            <div className="code-editor-wrapper">
              <textarea
                className="code-editor-area"
                value={codeDraft}
                onChange={(e) => {
                  setCodeDraft(e.target.value);
                  setSaved(false);
                }}
                spellCheck={false}
              />
            </div>
            {output && (
              <div className="code-output-panel">
                <small>Console execution</small>
                <pre>{output}</pre>
              </div>
            )}
            <footer>
              <span>main</span>
              <span>
                {activeFile.path.endsWith('.css')
                  ? 'CSS'
                  : activeFile.path.endsWith('.md')
                  ? 'Markdown'
                  : 'TypeScript React'}
              </span>
              <span>
                Ln {codeDraft.split('\n').length}, {codeDraft.length} chars
              </span>
            </footer>
          </section>
        </div>
      )}

      {topTab === 'Tasks' && <ProjectTaskBoard project={project} app={a} />}
      {topTab === 'Roadmap' && <ProjectRoadmap project={project} app={a} />}
      {topTab === 'Terminal' && (
        <div className="project-terminal-view">
          <h3>Connected Build Environment</h3>
          <pre className="terminal-screen">
            $ npm run build{'\n'}vite v6.0.11 building for production...{'\n'}✓
            1589 modules transformed.{'\n'}dist/index.html 0.59 kB{'\n'}✓ built
            in 4.72s
          </pre>
          <Button onClick={() => a.toast('Build successful!')}>
            Trigger preview rebuild
          </Button>
        </div>
      )}
    </main>
  );
}

function ProjectTaskBoard({ project, app }) {
  const [showCreate, setShowCreate] = useState(false);
  const [title, setTitle] = useState('');
  const [desc, setDesc] = useState('');
  const [priority, setPriority] = useState('medium');

  const defaultTasks = [
    {
      id: 't1',
      title: 'Configure development build tooling',
      status: 'done',
      priority: 'high',
    },
    {
      id: 't2',
      title: 'Set up project file explorer & editor',
      status: 'in_progress',
      priority: 'urgent',
    },
    {
      id: 't3',
      title: 'Wire task board status transitions',
      status: 'todo',
      priority: 'high',
    },
    {
      id: 't4',
      title: 'Implement roadmap milestone cards',
      status: 'backlog',
      priority: 'medium',
    },
  ];

  const taskList = app.projectTasks[project?.id] || defaultTasks;

  const advanceStatus = async (task) => {
    const next =
      {
        backlog: 'todo',
        todo: 'in_progress',
        in_progress: 'done',
        done: 'backlog',
      }[task.status] || 'todo';
    try {
      await app.updateProjectTaskStatus(project.id, task.id, next);
      app.toast(`Task moved to ${next}`);
    } catch (err) {
      app.toast(err.message || 'Could not update status');
    }
  };

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!title.trim()) return;
    try {
      await app.createProjectTask(project.id, {
        title: title.trim(),
        description: desc.trim(),
        priority,
      });
      setTitle('');
      setDesc('');
      setShowCreate(false);
      app.toast('Task created');
    } catch (err) {
      app.toast(err.message || 'Could not create task');
    }
  };

  const columns = [
    { key: 'backlog', label: 'Backlog' },
    { key: 'todo', label: 'To Do' },
    { key: 'in_progress', label: 'In Progress' },
    { key: 'done', label: 'Done' },
  ];

  return (
    <div className="project-task-board">
      <div className="board-header">
        <div>
          <h3>Task Board</h3>
          <p>
            Manage project tasks, assign work, and track sprint progress.
          </p>
        </div>
        <Button
          variant="primary"
          icon={Plus}
          onClick={() => setShowCreate(true)}
        >
          Add task
        </Button>
      </div>

      {showCreate && (
        <form className="board-create-form" onSubmit={handleCreate}>
          <h4>Create project task</h4>
          <Field label="Title">
            <input
              autoFocus
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Task summary..."
            />
          </Field>
          <Field label="Description">
            <textarea
              rows={2}
              value={desc}
              onChange={(e) => setDesc(e.target.value)}
              placeholder="Optional context or acceptance criteria..."
            />
          </Field>
          <Field label="Priority">
            <select
              value={priority}
              onChange={(e) => setPriority(e.target.value)}
            >
              <option value="low">Low</option>
              <option value="medium">Medium</option>
              <option value="high">High</option>
              <option value="urgent">Urgent</option>
            </select>
          </Field>
          <div className="dialog-actions">
            <Button
              variant="quiet"
              type="button"
              onClick={() => setShowCreate(false)}
            >
              Cancel
            </Button>
            <Button variant="primary" type="submit">
              Create task
            </Button>
          </div>
        </form>
      )}

      <div className="kanban-grid">
        {columns.map((col) => {
          const colTasks = taskList.filter((t) => t.status === col.key);
          return (
            <div key={col.key} className="kanban-column">
              <div className="kanban-col-head">
                <strong>{col.label}</strong>
                <span>{colTasks.length}</span>
              </div>
              <div className="kanban-col-body">
                {colTasks.map((t) => (
                  <div key={t.id} className="kanban-card">
                    <div className="card-top">
                      <Tag
                        tone={
                          t.priority === 'urgent'
                            ? 'coral'
                            : t.priority === 'high'
                            ? 'gold'
                            : 'blue'
                        }
                      >
                        {t.priority}
                      </Tag>
                      <button
                        className="advance-btn"
                        title="Advance status"
                        onClick={() => advanceStatus(t)}
                      >
                        →
                      </button>
                    </div>
                    <strong>{t.title}</strong>
                    {t.description && <p>{t.description}</p>}
                  </div>
                ))}
                {!colTasks.length && <div className="empty-col">No tasks</div>}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function ProjectRoadmap({ project, app }) {
  const [showCreate, setShowCreate] = useState(false);
  const [title, setTitle] = useState('');
  const [targetDate, setTargetDate] = useState('');

  const defaultMilestones = [
    {
      id: 'm1',
      title: 'Milestone 1: Prototype Launch',
      targetDate: '2026-10-01',
      status: 'completed',
    },
    {
      id: 'm2',
      title: 'Milestone 2: Team Alpha Release',
      targetDate: '2026-10-25',
      status: 'open',
    },
    {
      id: 'm3',
      title: 'Milestone 3: Public Beta',
      targetDate: '2026-11-15',
      status: 'open',
    },
  ];

  const milestonesList =
    app.projectMilestones[project?.id] || defaultMilestones;

  const handleCreate = async (e) => {
    e.preventDefault();
    if (!title.trim()) return;
    try {
      await app.createProjectMilestone(project.id, {
        title: title.trim(),
        targetDate: targetDate || null,
      });
      setTitle('');
      setTargetDate('');
      setShowCreate(false);
      app.toast('Milestone created');
    } catch (err) {
      app.toast(err.message || 'Could not create milestone');
    }
  };

  return (
    <div className="project-roadmap-view">
      <div className="board-header">
        <div>
          <h3>Roadmap &amp; Milestones</h3>
          <p>
            Define key delivery goals, target completion dates, and outcomes.
          </p>
        </div>
        <Button
          variant="primary"
          icon={Plus}
          onClick={() => setShowCreate(true)}
        >
          Add milestone
        </Button>
      </div>

      {showCreate && (
        <form className="board-create-form" onSubmit={handleCreate}>
          <h4>Create project milestone</h4>
          <Field label="Title">
            <input
              autoFocus
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Milestone name..."
            />
          </Field>
          <Field label="Target date">
            <input
              type="date"
              value={targetDate}
              onChange={(e) => setTargetDate(e.target.value)}
            />
          </Field>
          <div className="dialog-actions">
            <Button
              variant="quiet"
              type="button"
              onClick={() => setShowCreate(false)}
            >
              Cancel
            </Button>
            <Button variant="primary" type="submit">
              Create milestone
            </Button>
          </div>
        </form>
      )}

      <div className="milestones-list">
        {milestonesList.map((m, i) => (
          <div key={m.id} className="milestone-card">
            <div className="milestone-left">
              <div className="milestone-circle">{i + 1}</div>
              <div>
                <strong>{m.title}</strong>
                <p>Target: {m.targetDate || 'Upcoming'}</p>
              </div>
            </div>
            <Tag tone={m.status === 'completed' ? 'green' : 'blue'}>
              {m.status === 'completed' ? 'Completed' : 'In Progress'}
            </Tag>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── 5. Company Space (Exported) ────────────────────────────────────────────

export function Company() {
  return (
    <CompanyRedesign
      CompanyTeamPanel={CompanyTeamPanel}
      TaskBoard={TaskBoard}
      CompanyDocuments={CompanyDocuments}
      Hiring={Hiring}
      FundingCreator={FundingCreator}
      Products={Products}
      Roadmap={Roadmap}
      Updates={Updates}
      OperatorAnalytics={OperatorAnalytics}
    />
  );
}

function CompanyWorkspace({ app, view, setView }) {
  const company = app.companyDrafts[0];
  const [roles, setRoles] = useState([
    { title: 'Frontend developer', applicants: 12 },
    { title: 'Product designer', applicants: 8 },
    { title: 'Growth lead', applicants: 5 },
  ]);
  const [updates, setUpdates] = useState([
    'We completed 12 grower interviews and redesigned onboarding.',
  ]);

  return (
    <div className="operator-layout">
      <section>
        <Tabs
          items={[
            'Overview',
            'Team',
            'Hiring',
            'Funding',
            'Products',
            'Roadmap',
            'Tasks',
            'Updates',
            'Documents',
            'Analytics',
          ]}
          value={view}
          onChange={setView}
        />

        {view === 'Overview' && (
          <>
            <div className="company-dashboard">
              <div>
                <span className="eyebrow">
                  {company?.name || 'GREENCART'} ·{' '}
                  {company?.state === 'verification_pending'
                    ? 'VERIFICATION PENDING'
                    : 'VERIFIED'}
                </span>
                <h2>Welcome back, Ananya.</h2>
                <p>
                  Make the sustainable food network easier to use, one clear
                  launch decision at a time.
                </p>
              </div>
              <div className="dash-actions">
                <Button
                  icon={Plus}
                  onClick={() =>
                    setUpdates((x) => [
                      'A new company update is ready to share.',
                      ...x,
                    ])
                  }
                >
                  Post update
                </Button>
                <Button variant="ghost" onClick={() => setView('Tasks')}>
                  Open tasks
                </Button>
              </div>
            </div>

            <div className="metric-grid operator-metrics">
              <Metric value="4" label="Team members" />
              <Metric value={`${roles.length}`} label="Open roles" />
              <Metric value="18" label="Investor interest" />
              <Metric value="42" label="Applications" />
            </div>

            <div className="operator-columns">
              <section className="inset-card">
                <SectionTitle title="Next steps" />
                <Checklist
                  items={[
                    'Complete market research',
                    'Invite a co-founder',
                    'Create the pitch deck',
                    'Publish a product update',
                  ]}
                />
              </section>
              <section className="inset-card">
                <SectionTitle title="Readiness" />
                <Checklist
                  items={[
                    'Problem definition',
                    'Solution',
                    'Market opportunity',
                    'Team and co-founders',
                    'Pitch deck',
                  ]}
                />
                <Progress value={68} />
              </section>
            </div>
          </>
        )}

        {view === 'Team' && <TeamPanel />}
        {view === 'Hiring' && <Hiring roles={roles} setRoles={setRoles} />}
        {view === 'Funding' && <FundingCreator />}
        {view === 'Products' && <Products />}
        {view === 'Roadmap' && <Roadmap />}
        {view === 'Tasks' && <TaskBoard />}
        {view === 'Updates' && (
          <Updates updates={updates} setUpdates={setUpdates} />
        )}
        {view === 'Documents' && <CompanyDocuments />}
        {view === 'Analytics' && <OperatorAnalytics />}
      </section>

      <aside className="space-rail">
        <SectionTitle title="Quick actions" />
        <div className="quick-stack">
          <Button icon={Plus} onClick={() => setView('Products')}>
            Add product
          </Button>
          <Button variant="ghost" onClick={() => setView('Tasks')}>
            Manage tasks
          </Button>
          <Button variant="ghost" onClick={() => setView('Funding')}>
            Raise funding
          </Button>
          <Button variant="ghost" onClick={() => setView('Documents')}>
            Open documents
          </Button>
        </div>

        <SectionTitle title="Company profile" />
        <div className="rail-card">
          <b>{company?.name || 'GreenCart'}</b>
          <span>
            {company
              ? `${company.industry} · ${company.stage}`
              : 'Food and beverage · Idea stage'}
          </span>
          <Progress value={68} />
          <Button variant="ghost">View public profile</Button>
        </div>
      </aside>
    </div>
  );
}

function Metric({ value, label }) {
  return (
    <div>
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  );
}

function Checklist({ items }) {
  return (
    <div className="operator-checklist">
      {items.map((x, i) => (
        <label key={x}>
          <input type="checkbox" defaultChecked={i < 2} />
          <span>{x}</span>
        </label>
      ))}
    </div>
  );
}

function TeamPanel() {
  const a = useApp();
  if (a.page === 'Company') {
    return (
      <CompanyTeamPanel
        app={a}
        company={
          a.companyDrafts[0] || {
            id: 'preview-company',
            name: 'GreenCart',
            remote: false,
          }
        }
      />
    );
  }

  return (
    <div className="person-list">
      {people.map((p, i) => (
        <article key={p.id}>
          <Avatar person={p} />
          <div>
            <b>{p.name}</b>
            <small>
              {
                ['Founder & CEO', 'Product and design', 'Technology', 'Operations'][
                  i
                ]
              }
            </small>
          </div>
          <Tag tone={i === 0 ? 'purple' : ''}>{i === 0 ? 'You' : 'Member'}</Tag>
        </article>
      ))}
    </div>
  );
}

function CompanyTeamPanel({ app, company }) {
  const [query, setQuery] = useState('');
  const [role, setRole] = useState('member');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [error, setError] = useState('');

  const invitations = app.companyInvitations[company.id] || [];

  const find = async (event) => {
    event.preventDefault();
    if (query.trim().length < 2) {
      setError('Enter at least two characters.');
      return;
    }
    setLoading(true);
    setSearched(true);
    setError('');
    try {
      setResults(await app.searchProfiles(query));
    } catch (reason) {
      setError(reason.message || 'Could not search profiles.');
    } finally {
      setLoading(false);
    }
  };

  const invite = async (person) => {
    setLoading(true);
    setError('');
    try {
      const mode = await app.inviteCompanyMember(company, person, role);
      setResults((current) => current.filter((item) => item.id !== person.id));
      app.toast(
        mode === 'synced'
          ? 'Team invitation sent.'
          : 'Team invitation saved in Preview.'
      );
    } catch (reason) {
      setError(reason.message || 'Could not send this invitation.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="team-workspace">
      <section className="person-list" aria-label="Company team">
        <article>
          <Avatar person={app.profile} name={app.profile.name} />
          <div>
            <b>{app.profile.name}</b>
            <small>Founder and workspace owner</small>
          </div>
          <Tag tone="purple">Owner</Tag>
        </article>
        {invitations.map((person) => (
          <article key={person.id}>
            <Avatar name={person.name} />
            <div>
              <b>{person.name}</b>
              <small>
                {person.headline || 'Invited collaborator'} · {person.role}
              </small>
            </div>
            <Tag tone="gold">Pending</Tag>
          </article>
        ))}
      </section>

      <form className="content-editor member-search-panel" onSubmit={find}>
        <div>
          <span className="eyebrow">BUILD THE TEAM</span>
          <h3>Invite a collaborator</h3>
          <p>Search discoverable profiles and choose the access they need.</p>
        </div>

        <div className="member-search-controls">
          <Field label="Find a member">
            <input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Name, username, or skill"
            />
          </Field>
          <Field label="Workspace role">
            <select
              value={role}
              onChange={(event) => setRole(event.target.value)}
            >
              <option value="member">Member</option>
              <option value="admin">Admin</option>
            </select>
          </Field>
        </div>

        <Button type="submit" icon={Search} disabled={loading}>
          {loading ? 'Searching…' : 'Search members'}
        </Button>

        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}

        <div className="person-list profile-search-results" aria-live="polite">
          {searched && !loading && !error && !results.length && (
            <p className="empty-inline">No matching profiles found.</p>
          )}
          {results.map((person) => (
            <article key={person.id}>
              <Avatar name={person.displayName} />
              <div>
                <b>{person.displayName}</b>
                <small>
                  {person.headline || person.username}
                  {person.location ? ` · ${person.location}` : ''}
                </small>
              </div>
              <Button
                type="button"
                variant="ghost"
                onClick={() => invite(person)}
                disabled={loading}
              >
                Invite
              </Button>
            </article>
          ))}
        </div>
      </form>
    </div>
  );
}

function Hiring({ roles, setRoles }) {
  const add = () =>
    setRoles((x) => [
      ...x,
      { title: 'Customer success lead', applicants: 0 },
    ]);

  return (
    <section className="operator-list">
      <SectionTitle title="Open roles" action="Add role" onAction={add} />
      {roles.map((r) => (
        <article key={r.title}>
          <span className="module-icon blue">
            <BriefcaseBusiness size={19} />
          </span>
          <div>
            <b>{r.title}</b>
            <small>Full-time · Bengaluru / Remote</small>
          </div>
          <Tag>{r.applicants} applicants</Tag>
        </article>
      ))}
    </section>
  );
}

function FundingCreator() {
  return (
    <section className="funding-panel">
      <div className="metric-grid">
        <Metric value="INR 20L" label="Seed round target" />
        <Metric value="40%" label="Raised" />
        <Metric value="10%" label="Equity offered" />
        <Metric value="12" label="Interested investors" />
      </div>
      <Progress value={40} />
      <h3>Use of funds</h3>
      <Checklist
        items={[
          'Product development · 50%',
          'Team expansion · 25%',
          'Market launch · 15%',
          'Operations · 10%',
        ]}
      />
      <Button>Open funding request</Button>
    </section>
  );
}

function Products() {
  return (
    <section className="operator-list">
      <SectionTitle title="Products" action="Add product" />
      <article>
        <span className="module-icon green">
          <Building2 size={19} />
        </span>
        <div>
          <b>GreenCart local produce</b>
          <small>Marketplace · v1.2 · Live</small>
        </div>
        <Tag tone="green">Live</Tag>
      </article>
      <article>
        <span className="module-icon blue">
          <BookOpen size={19} />
        </span>
        <div>
          <b>Grower dashboard</b>
          <small>Operations · beta</small>
        </div>
        <Tag>Beta</Tag>
      </article>
    </section>
  );
}

function Roadmap() {
  return (
    <section className="roadmap-list">
      {[
        'Research and planning',
        'MVP development',
        'Beta launch',
        'Public launch',
        'Scale and expand',
      ].map((x, i) => (
        <article key={x}>
          <span className={i < 2 ? 'complete' : ''} />
          <div>
            <b>{x}</b>
            <small>{i < 2 ? 'Completed' : 'Next milestone'}</small>
          </div>
        </article>
      ))}
    </section>
  );
}

function TaskBoard() {
  const a = useApp();
  const company =
    a.companyDrafts[0] || {
      id: 'preview-company',
      name: 'GreenCart',
      remote: false,
    };
  const tasks = a.companyTasks[company.id] || [];
  const [title, setTitle] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const columns = ['To do', 'In progress', 'Review', 'Done'];

  const add = async (event) => {
    event.preventDefault();
    if (saving) return;
    setSaving(true);
    setError('');
    try {
      const mode = await a.createCompanyTask(company, title);
      setTitle('');
      a.toast(
        mode === 'synced'
          ? 'Task added to the company workspace.'
          : 'Task saved in Preview.'
      );
    } catch (reason) {
      setError(reason.message || 'Could not create this task.');
    } finally {
      setSaving(false);
    }
  };

  const advance = async (task) => {
    const next = {
      'To do': 'In progress',
      'In progress': 'Review',
      Review: 'Done',
      Done: 'To do',
    }[task.state];
    try {
      const mode = await a.updateCompanyTask(company, task, next);
      a.toast(
        mode === 'synced'
          ? `Task moved to ${next}.`
          : `Task moved to ${next} in Preview.`
      );
    } catch (reason) {
      a.toast(reason.message || 'Could not update this task.');
    }
  };

  return (
    <section className="task-board">
      <div className="task-board-head">
        <div>
          <span className="eyebrow">TEAM EXECUTION</span>
          <h3>Tasks that move the company forward.</h3>
        </div>
        <form className="task-create" onSubmit={add}>
          <label htmlFor="company-task-title">New task</label>
          <input
            id="company-task-title"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder="e.g. Prepare launch checklist"
          />
          <Button type="submit" icon={Plus} disabled={saving}>
            {saving ? 'Adding…' : 'Add task'}
          </Button>
        </form>
      </div>

      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}

      <div className="task-columns">
        {columns.map((column) => (
          <div className="task-column" key={column}>
            <b>{column}</b>
            <small>
              {tasks.filter((item) => item.state === column).length} items
            </small>
            {tasks
              .filter((item) => item.state === column)
              .map((task) => (
                <button
                  key={task.id}
                  onClick={() => advance(task)}
                  aria-label={`Move ${task.title} to ${
                    {
                      'To do': 'In progress',
                      'In progress': 'Review',
                      Review: 'Done',
                      Done: 'To do',
                    }[task.state]
                  }`}
                >
                  <ClipboardList size={17} />
                  <span>
                    <strong>{task.title}</strong>
                    <small>{task.owner}</small>
                  </span>
                  <Tag>{task.state}</Tag>
                </button>
              ))}
          </div>
        ))}
      </div>
    </section>
  );
}

function formatBytes(value) {
  if (!value) return '0 B';
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${Math.round(value / 102.4) / 10} KB`;
  return `${Math.round(value / 1024 / 102.4) / 10} MB`;
}

function CompanyDocuments() {
  const a = useApp();
  const company =
    a.companyDrafts[0] || {
      id: 'preview-company',
      name: 'GreenCart',
      remote: false,
    };
  const documents = a.companyDocuments[company.id] || [];
  const [filter, setFilter] = useState('All');
  const [title, setTitle] = useState('');
  const [collection, setCollection] = useState('general');
  const [accessScope, setAccessScope] = useState('team');
  const [file, setFile] = useState(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const labels = {
    general: 'General',
    fundraising: 'Fundraising',
    product: 'Product',
    hiring: 'Hiring',
    legal: 'Legal',
  };

  const visible =
    filter === 'All'
      ? documents
      : documents.filter((item) => labels[item.collection] === filter);

  const upload = async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    if (!file) {
      setError('Choose a document to upload.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      const mode = await a.addCompanyDocument(company, file, {
        title,
        collection,
        accessScope,
      });
      setTitle('');
      setFile(null);
      form.reset();
      a.toast(
        mode === 'synced'
          ? 'Document added to the company room.'
          : 'Document saved in Preview.'
      );
    } catch (reason) {
      setError(reason.message || 'Could not upload this document.');
    } finally {
      setSaving(false);
    }
  };

  const archive = async (document) => {
    try {
      const mode = await a.archiveCompanyDocument(company, document);
      a.toast(
        mode === 'synced'
          ? 'Document archived.'
          : 'Document removed from Preview.'
      );
    } catch (reason) {
      a.toast(reason.message || 'Could not archive this document.');
    }
  };

  return (
    <section className="company-document-room">
      <div className="document-room-head">
        <div>
          <span className="eyebrow">COMPANY DOCUMENT ROOM</span>
          <h3>Files with a clear home and audience.</h3>
          <p>
            Upload verified company files, organize them by purpose, and
            control who on the team can open them.
          </p>
        </div>
        <div className="document-room-stats">
          <b>{documents.length}</b>
          <span>active files</span>
          <b>
            {formatBytes(
              documents.reduce((sum, item) => sum + (item.bytes || 0), 0)
            )}
          </b>
          <span>stored</span>
        </div>
      </div>

      <form className="document-upload" onSubmit={upload}>
        <label className="document-file">
          <Upload size={19} />
          <span>
            <b>{file?.name || 'Choose a file'}</b>
            <small>PDF, Office, image, or ZIP · up to 25 MB</small>
          </span>
          <input
            type="file"
            aria-label="Document file"
            accept=".pdf,.doc,.docx,.xls,.xlsx,.zip,image/jpeg,image/png,image/webp,image/gif"
            onChange={(event) => setFile(event.target.files?.[0] || null)}
          />
        </label>
        <label>
          Document title
          <input
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            placeholder={file?.name || 'e.g. Seed round pitch deck'}
          />
        </label>
        <label>
          Collection
          <select
            value={collection}
            onChange={(event) => setCollection(event.target.value)}
          >
            {Object.entries(labels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <label>
          Access
          <select
            value={accessScope}
            onChange={(event) => setAccessScope(event.target.value)}
          >
            <option value="team">Entire team</option>
            <option value="admins">Owners and admins</option>
          </select>
        </label>
        <Button type="submit" icon={Upload} disabled={saving}>
          {saving ? 'Uploading…' : 'Upload document'}
        </Button>
      </form>

      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}

      <div className="document-filter">
        <Tabs
          items={['All', ...Object.values(labels)]}
          value={filter}
          onChange={setFilter}
        />
      </div>

      {visible.length ? (
        <div className="company-document-list">
          {visible.map((document) => (
            <article key={document.id}>
              <span className="document-type">
                <FileText size={21} />
              </span>
              <div>
                <b>{document.title}</b>
                <small>
                  {document.filename} · {formatBytes(document.bytes)} ·{' '}
                  {new Date(document.createdAt).toLocaleDateString()}
                </small>
                <div className="tag-cloud">
                  <Tag>{labels[document.collection] || document.collection}</Tag>
                  <Tag
                    tone={
                      document.accessScope === 'admins' ? 'purple' : 'quiet'
                    }
                  >
                    {document.accessScope === 'admins'
                      ? 'Admins only'
                      : 'Entire team'}
                  </Tag>
                </div>
              </div>
              <div className="document-actions">
                <IconButton
                  icon={Download}
                  label={`Download ${document.title}`}
                  onClick={() =>
                    a.downloadCompanyDocument(company, document)
                  }
                />
                <IconButton
                  icon={Archive}
                  label={`Archive ${document.title}`}
                  onClick={() => archive(document)}
                />
              </div>
            </article>
          ))}
        </div>
      ) : (
        <Empty
          title="No documents in this collection"
          text="Upload a file or choose another collection."
        />
      )}
    </section>
  );
}

function Updates({ updates, setUpdates }) {
  const [text, setText] = useState('');
  return (
    <section className="updates-panel">
      <textarea
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder="Share a company update with your team and followers."
      />
      <Button
        onClick={() => {
          if (text.trim()) setUpdates((x) => [text, ...x]);
          setText('');
        }}
      >
        Post update
      </Button>
      {updates.map((x) => (
        <article key={x}>
          <Avatar person={people[2]} />
          <div>
            <b>GreenCart</b>
            <p>{x}</p>
            <small>Just now · visible to followers</small>
          </div>
        </article>
      ))}
    </section>
  );
}

function Documents() {
  const a = useApp();
  const collections = [
    ['Fundraising pack', 4],
    ['MVP launch', 3],
    ['Hiring', 2],
  ];

  return (
    <section className="documents-workspace">
      <div className="document-grid">
        {[
          'Pitch deck',
          'Financial model',
          'Product demo',
          'One-page brief',
          'Cap table',
          'Incorporation',
        ].map((x, i) => (
          <button
            key={x}
            onClick={() => a.toast(`${x} opened in this workspace`)}
          >
            <FileText size={20} />
            <b>{x}</b>
            <small>
              {i + 1} file{i ? 's' : ''}
            </small>
          </button>
        ))}
      </div>
      <SectionTitle title="Smart collections" />
      <div className="smart-collections">
        {collections.map(([title, count]) => (
          <button
            key={title}
            onClick={() => a.toast(`${title} collection opened`)}
          >
            <span className="module-icon gold">
              <FolderKanban size={18} />
            </span>
            <span>
              <b>{title}</b>
              <small>{count} related documents</small>
            </span>
            <ArrowRight size={17} />
          </button>
        ))}
      </div>
    </section>
  );
}

function OperatorAnalytics() {
  return (
    <>
      <div className="metric-grid operator-metrics">
        <Metric value="2,438" label="Profile views" />
        <Metric value="1,209" label="Followers" />
        <Metric value="42" label="Applications" />
        <Metric value="214" label="Pitch deck views" />
      </div>
      <div className="chart-card">
        <SectionTitle title="Company momentum" />
        <div className="bar-chart purple">
          {[20, 36, 30, 56, 48, 70, 84].map((h, i) => (
            <i key={i} style={{ height: `${h}%` }} />
          ))}
        </div>
      </div>
    </>
  );
}

// ─── 6. Investment Space (Exported) ─────────────────────────────────────────

export function Investment() {
  const a = useApp();
  const [mode, setMode] = useState('Investor');
  const [view, setView] = useState('Discover');

  return (
    <main className="operator-space">
      <PageHeader
        eyebrow="INVESTMENT"
        title={
          mode === 'Investor'
            ? 'Invest with context, not pressure.'
            : 'Manage your company raise.'
        }
        description={
          mode === 'Investor'
            ? 'Research opportunities, track conversations, and keep each decision deliberate.'
            : 'Share the company story, manage interest, and move each raise through clear, consented stages.'
        }
      >
        <Tabs
          items={['Investor', 'Founder raise']}
          value={mode}
          onChange={setMode}
        />
      </PageHeader>

      {mode === 'Investor' ? (
        <InvestorWorkspace app={a} view={view} setView={setView} />
      ) : (
        <RaiseWorkspace app={a} />
      )}
    </main>
  );
}

function InvestorWorkspace({ app, view, setView }) {
  const [stage, setStage] = useState('All');
  const [sector, setSector] = useState('All');

  const sectors = {
    GreenCart: 'ClimateTech',
    Layers: 'SaaS',
    Orbit: 'Learning',
  };

  const list = companies.filter(
    (c) =>
      (stage === 'All' || c.stage === stage) &&
      (sector === 'All' || sectors[c.title] === sector)
  );

  return (
    <div className="operator-layout">
      <section>
        <Tabs
          items={[
            'Discover',
            'Pipeline',
            'Portfolio',
            'Meetings',
            'Agreements',
            'Documents',
            'Analytics',
            'Profile',
          ]}
          value={view}
          onChange={setView}
        />

        {view === 'Discover' && (
          <>
            <div className="operator-hero investment-operator">
              <div>
                <Tag tone="purple">Investor dashboard</Tag>
                <h2>Find the companies you can help grow.</h2>
                <p>
                  Interest begins a conversation. No investment is made in this
                  workspace.
                </p>
                <Button
                  onClick={() =>
                    app.setModal({
                      type: 'details',
                      kind: 'investor-onboarding',
                    })
                  }
                >
                  Set preferences
                </Button>
              </div>
              <div className="hero-score">
                <strong>INR 8.6 Cr</strong>
                <span>example portfolio value</span>
                <Progress value={62} />
              </div>
            </div>

            <div className="investor-filters">
              <div>
                <small>Stage</small>
                <Tabs
                  items={['All', 'Seed', 'MVP', 'Early revenue']}
                  value={stage}
                  onChange={setStage}
                />
              </div>
              <div>
                <small>Sector</small>
                <Tabs
                  items={['All', 'ClimateTech', 'SaaS', 'Learning']}
                  value={sector}
                  onChange={setSector}
                />
              </div>
            </div>

            <div className="card-grid">
              {list.map((c) => (
                <article className="company-card" key={c.id}>
                  <span className={`module-icon ${c.color}`}>
                    <Building2 size={21} />
                  </span>
                  <h3>{c.title}</h3>
                  <p>{c.description}</p>
                  <div className="tag-cloud">
                    <Tag tone="green">{sectors[c.title]}</Tag>
                    {c.tags.map((t) => (
                      <Tag key={t}>{t}</Tag>
                    ))}
                  </div>
                  <small>
                    {c.stage} · {c.location} · {c.team} people
                  </small>
                  <Button
                    onClick={() =>
                      app.setModal({
                        type: 'details',
                        kind: 'investment',
                        item: c,
                      })
                    }
                  >
                    {app.pipeline[c.id] ? 'In pipeline' : 'View opportunity'}
                  </Button>
                </article>
              ))}
            </div>

            {!list.length && (
              <Empty
                title="No opportunities match this focus"
                text="Try another stage or sector to widen your discovery."
              />
            )}
          </>
        )}

        {view === 'Pipeline' && <Pipeline />}
        {view === 'Portfolio' && <Portfolio />}
        {view === 'Meetings' && <Meetings />}
        {view === 'Agreements' && <InvestorAgreements />}
        {view === 'Documents' && <Documents />}
        {view === 'Analytics' && <InvestorAnalytics />}
        {view === 'Profile' && <InvestorProfile />}
      </section>

      <aside className="space-rail">
        <SectionTitle title="Your pipeline" />
        <div className="rail-card">
          <b>Interested</b>
          <strong>12</strong>
          <span>5 meetings · 3 due diligence</span>
          <Button variant="ghost" onClick={() => setView('Pipeline')}>
            Open pipeline
          </Button>
        </div>
        <SectionTitle title="Investor principles" />
        <div className="state-list">
          <Status>Interest is not a commitment</Status>
          <Status>Private documents stay scoped</Status>
          <Status>Terms require explicit consent</Status>
        </div>
      </aside>
    </div>
  );
}

function Portfolio() {
  return (
    <section className="portfolio-list">
      <div className="portfolio-total">
        <span>Total invested</span>
        <strong>INR 8.6 Cr</strong>
        <small>INR 12.4 Cr total returns · 28.6% IRR</small>
      </div>
      {companies.map((c, i) => (
        <article key={c.id}>
          <span className={`module-icon ${c.color}`}>
            <Building2 size={18} />
          </span>
          <div>
            <b>{c.title}</b>
            <small>
              {c.category} · {c.stage}
            </small>
          </div>
          <Tag tone="green">{((1.2 + i / 2) | 0)}.x</Tag>
        </article>
      ))}
    </section>
  );
}

function Meetings() {
  return (
    <section className="session-list">
      {[
        'Pitch meeting · GreenCart',
        'Founder discussion · Layers',
        'Product demo · Orbit',
      ].map((x, i) => (
        <article className="session-row" key={x}>
          <span className="date-chip">
            <CalendarDays size={17} />
          </span>
          <div>
            <b>{x}</b>
            <small>
              {25 + i} Sep · {i ? '4:30 PM' : '11:00 AM'} · Online
            </small>
          </div>
          <Button>Join</Button>
        </article>
      ))}
    </section>
  );
}

function InvestorAnalytics() {
  return (
    <>
      <div className="metric-grid operator-metrics">
        <Metric value="14" label="Investments" />
        <Metric value="INR 8.6 Cr" label="Total invested" />
        <Metric value="3.2x" label="Average return" />
        <Metric value="23" label="Deals reviewed" />
      </div>
      <OperatorAnalytics />
    </>
  );
}

function InvestorProfile() {
  return (
    <section className="content-editor">
      <Field label="Investor type">
        <select>
          <option>Angel investor</option>
          <option>Venture capital</option>
          <option>Family office</option>
        </select>
      </Field>
      <Field label="Preferred sectors">
        <input defaultValue="Climate tech, SaaS, Education" />
      </Field>
      <Field label="Investment range">
        <input defaultValue="INR 10L – 5Cr" />
      </Field>
      <Button>Save preferences</Button>
    </section>
  );
}

function RaiseWorkspace({ app }) {
  const [view, setView] = useState('Dashboard');

  return (
    <div className="operator-layout">
      <section>
        <Tabs
          items={[
            'Dashboard',
            'Campaign',
            'Documents',
            'Investors',
            'Updates',
            'Analytics',
            'Settings',
          ]}
          value={view}
          onChange={setView}
        />

        {view === 'Dashboard' && (
          <>
            <div className="company-dashboard">
              <div>
                <Tag tone="purple">Founder raise</Tag>
                <h2>GreenCart seed round</h2>
                <p>
                  Keep the materials, investor conversations, and company updates
                  in one calm place.
                </p>
              </div>
              <Button onClick={() => setView('Campaign')}>Edit raise</Button>
            </div>
            <div className="metric-grid operator-metrics">
              <Metric value="INR 20L" label="Target amount" />
              <Metric value="40%" label="Committed" />
              <Metric value="12" label="Interested investors" />
              <Metric value="3" label="Meetings this week" />
            </div>
          </>
        )}

        {view === 'Campaign' && <FundingCreator />}
        {view === 'Documents' && <Documents />}
        {view === 'Investors' && <Pipeline />}
        {view === 'Updates' && (
          <Updates
            updates={['New research note added to the data room.']}
            setUpdates={() => {}}
          />
        )}
        {view === 'Analytics' && <OperatorAnalytics />}
        {view === 'Settings' && <InvestorProfile />}
      </section>

      <aside className="space-rail">
        <SectionTitle title="Raise stages" />
        <Checklist
          items={[
            'Campaign details',
            'Documents ready',
            'Investor outreach',
            'Meetings scheduled',
            'Terms agreed',
          ]}
        />
      </aside>
    </div>
  );
}

function Pipeline() {
  const a = useApp();
  const items = companies.filter((c) => a.pipeline[c.id]);

  return items.length ? (
    <div className="list-surface">
      {items.map((c) => (
        <div className="job-row" key={c.id}>
          <span className={`module-icon ${c.color}`}>
            <Building2 size={20} />
          </span>
          <div>
            <h3>{c.title}</h3>
            <p>
              {c.category} · {c.stage}
            </p>
          </div>
          <Status>{labelState(a.pipeline[c.id])}</Status>
        </div>
      ))}
    </div>
  ) : (
    <Empty
      title="Your pipeline is open"
      text="When an opportunity interests you, the next step is a meeting, not an investment."
    />
  );
}

function InvestorAgreements() {
  const a = useApp();
  const [accepted, setAccepted] = useState(false);

  return (
    <section className="agreement-workspace">
      <div className="agreement-hero">
        <span className="module-icon gold">
          <ShieldCheck />
        </span>
        <div>
          <span className="eyebrow">PRIVATE DEAL ROOM</span>
          <h2>Review terms with a clear record.</h2>
          <p>
            Agreements stay scoped to a conversation. Signing remains an
            explicit local action in this prototype.
          </p>
        </div>
      </div>
      <div className="agreement-list">
        {[
          ['Mutual NDA', 'Shared with GreenCart', 'Ready to review'],
          ['Term sheet', 'Seed round · INR 20L target', 'Draft'],
          ['Cap table', 'Current ownership outline', 'Reference'],
        ].map(([name, detail, status]) => (
          <article key={name}>
            <FileText size={19} />
            <div>
              <b>{name}</b>
              <small>{detail}</small>
            </div>
            <Status>{status}</Status>
            <Button
              variant="ghost"
              onClick={() => a.toast(`${name} opened for review`)}
            >
              Review
            </Button>
          </article>
        ))}
      </div>
      <label className="agreement-check">
        <input
          type="checkbox"
          checked={accepted}
          onChange={(e) => setAccepted(e.target.checked)}
        />
        I understand that accepting a term requires explicit legal review outside
        this prototype.
      </label>
      <Button
        disabled={!accepted}
        onClick={() => a.toast('Agreement acknowledgement recorded')}
      >
        Acknowledge review
      </Button>
    </section>
  );
}

// ─── 7. Professional & On-Demand Services Space (Collobster Native End-to-End) ───

export function Services() {
  const a = useApp();

  // Navigation & Filtering
  const [activeTab, setActiveTab] = useState('catalog'); // 'catalog' | 'orders' | 'requests'
  const [activeCategory, setActiveCategory] = useState('all');
  const [deliveryModeFilter, setDeliveryModeFilter] = useState('all');
  const [priceFilter, setPriceFilter] = useState('all');
  const [sortBy, setSortBy] = useState('recommended');
  const [query, setQuery] = useState('');

  // Modals & Flows
  const [selectedScopeService, setSelectedScopeService] = useState(null);
  const [bookingService, setBookingService] = useState(null);
  const [bookingStep, setBookingStep] = useState(1); // 1: Tier | 2: Requirements | 3: Delivery Channel | 4: Escrow Summary
  const [selectedTierIndex, setSelectedTierIndex] = useState(0);
  const [projectTitle, setProjectTitle] = useState('');
  const [projectBrief, setProjectBrief] = useState('');
  const [targetDeadline, setTargetDeadline] = useState('Within 7 days');
  const [communicationMode, setCommunicationMode] = useState('Collobster Workspace Chat');
  const [onSiteAddress, setOnSiteAddress] = useState('Office Hub 4B, Koramangala, Bengaluru');
  const [couponCode, setCouponCode] = useState('COLLOB20');
  const [isCouponApplied, setIsCouponApplied] = useState(true);
  const [isSubmittingBooking, setIsSubmittingBooking] = useState(false);

  // Provider Onboarding: List a Service
  const [showListServiceModal, setShowListServiceModal] = useState(false);
  const [newServiceDraft, setNewServiceDraft] = useState({
    title: '',
    category: 'software',
    deliveryMode: 'Remote (Global)',
    pricingType: 'Milestone-based Package',
    price: 9999,
    duration: '5 days',
    highlights: 'Production quality deliverables, Full code repository, 14-day warranty',
    description: '',
  });

  // Client RFPs: Post Custom Request
  const [showPostRfpModal, setShowPostRfpModal] = useState(false);
  const [customRequests, setCustomRequests] = useState(initialServiceRequests);
  const [newRfpDraft, setNewRfpDraft] = useState({
    title: '',
    category: 'legal',
    budget: '₹5,000 – ₹10,000',
    deadline: 'Within 3 days',
    deliveryMode: 'Remote',
    description: '',
  });

  // Lifecycle Modals: Deliverables inspection & Review rating
  const [reviewDeliverablesOrder, setReviewDeliverablesOrder] = useState(null);
  const [revisionNotes, setRevisionNotes] = useState('');
  const [ratingModalOrder, setRatingModalOrder] = useState(null);
  const [ratingStars, setRatingStars] = useState(5);
  const [ratingReviewText, setRatingReviewText] = useState('');

  // Combine universal catalog with user-created listings from a.serviceListings
  const fullCatalog = useMemo(() => {
    const customListings = (a.serviceListings || []).map((l, idx) => ({
      id: l.id || `custom-srv-${idx}`,
      title: l.title || 'Custom Community Service',
      category: l.category ? l.category.toLowerCase() : 'software',
      categoryName: l.category || 'Specialist Service',
      provider: l.provider || a.profile?.name || 'Community Pro',
      providerRole: l.providerRole || 'Verified Specialist',
      providerAvatar: l.providerAvatar || a.profile?.image || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
      rating: l.rating || 4.9,
      reviewsCount: 'New',
      price: Number(l.price) || 4999,
      pricingType: l.pricingType || 'Fixed Package',
      duration: l.duration || '3 days',
      deliveryMode: l.deliveryMode || 'Remote (Global)',
      badge: 'COMMUNITY LISTED',
      badgeColor: 'blue',
      image: l.image || 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=600&auto=format&fit=crop&q=80',
      description: l.description || 'Custom tailored specialist service offered through Collobster marketplace.',
      highlights: Array.isArray(l.highlights) ? l.highlights : (l.highlights || '').split(',').map((s) => s.trim()).filter(Boolean),
      inclusions: ['Milestone deliverables', 'Escrow protection', 'Direct workspace messaging'],
      tiers: [
        { name: 'Standard Package', price: Number(l.price) || 4999, days: l.duration || '3 days', desc: 'Complete project deliverables' },
      ],
      milestones: ['Initial Kickoff & Milestone 1 (50%)', 'Final Review & Delivery (50%)'],
      escrowProtected: true,
    }));
    return [...universalServiceCatalog, ...customListings];
  }, [a.serviceListings, a.profile]);

  // Filter & sort catalog items
  const filteredServices = useMemo(() => {
    return fullCatalog.filter((s) => {
      // Category filter
      if (activeCategory !== 'all') {
        const catMatch =
          s.category === activeCategory ||
          (s.categoryName && s.categoryName.toLowerCase().includes(activeCategory.toLowerCase()));
        if (!catMatch) return false;
      }

      // Delivery mode filter
      if (deliveryModeFilter !== 'all') {
        if (!s.deliveryMode.toLowerCase().includes(deliveryModeFilter.toLowerCase())) {
          return false;
        }
      }

      // Price filter
      if (priceFilter === 'under-3000') {
        if (s.price > 3000) return false;
      } else if (priceFilter === '3000-10000') {
        if (s.price < 3000 || s.price > 10000) return false;
      } else if (priceFilter === 'above-10000') {
        if (s.price <= 10000) return false;
      }

      // Text search
      if (query.trim()) {
        const q = query.trim().toLowerCase();
        const haystack = `${s.title} ${s.description} ${s.categoryName} ${s.provider} ${(s.highlights || []).join(' ')}`.toLowerCase();
        if (!haystack.includes(q)) return false;
      }

      return true;
    }).sort((a, b) => {
      if (sortBy === 'rating') return b.rating - a.rating;
      if (sortBy === 'price-asc') return a.price - b.price;
      if (sortBy === 'price-desc') return b.price - a.price;
      return 0; // default / recommended
    });
  }, [fullCatalog, activeCategory, deliveryModeFilter, priceFilter, query, sortBy]);

  // Seed default active engagements if bookings is empty to demonstrate the rich lifecycle
  const activeOrders = useMemo(() => {
    const userBookings = (a.bookings || []).map((b) => ({
      id: b.id,
      orderId: b.orderId || `CL-ORD-${(b.id || '9281').replace(/\D/g, '').slice(-5) || '84920'}`,
      title: b.title,
      category: b.category || 'software',
      provider: b.provider || 'Siddharth Nair',
      providerRole: b.providerRole || 'Lead Technical Architect',
      providerAvatar: b.providerAvatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
      price: b.price || 24999,
      deliveryMode: b.deliveryMode || 'Remote',
      status: b.orderStatus || b.status || 'Requirements Submitted', // 'Requirements Submitted' | 'In Progress' | 'Deliverables Under Review' | 'Completed'
      tierName: b.tierName || 'Starter MVP',
      createdAt: b.startsAt ? new Date(b.startsAt).toLocaleDateString() : 'Active this week',
      milestones: b.milestones || ['Architecture & Schema (30%)', 'Core APIs (40%)', 'QA & Delivery (30%)'],
      submittedDeliverables: b.submittedDeliverables || (b.orderStatus === 'Deliverables Under Review' ? {
        summary: 'Completed code repository with full test coverage, API docs, and Docker deployment.',
        githubLink: 'https://github.com/collobster/client-delivery-mvp',
        demoLink: 'https://preview-mvp.collobster.internal',
      } : null),
      reviewRating: b.reviewRating || null,
    }));

    if (userBookings.length > 0) return userBookings;

    // Default demonstration order showcasing the active milestone pipeline
    return [
      {
        id: 'ord-demo-1',
        orderId: 'CL-ORD-74921',
        title: 'Full-Stack Web & Mobile MVP Architecture & Engineering',
        category: 'software',
        provider: 'Siddharth Nair',
        providerRole: 'Ex-Staff Architect · 11 yrs exp',
        providerAvatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
        price: 24999,
        deliveryMode: 'Remote (Global)',
        status: 'Deliverables Under Review',
        tierName: 'Starter MVP',
        createdAt: 'Oct 1, 2026',
        milestones: ['Architecture & Schema (30%)', 'Core APIs & Frontend (40%)', 'QA & Deployment (30%)'],
        submittedDeliverables: {
          summary: 'All user authentication, database schemas, and Next.js frontend pages implemented. Automated test suite passing (26/26).',
          githubLink: 'https://github.com/collobster/saas-mvp-production',
          demoLink: 'https://demo-saas-mvp.collobster.dev',
        },
        reviewRating: null,
      },
    ];
  }, [a.bookings]);

  // Start booking flow
  const handleInitiateBooking = (service) => {
    setBookingService(service);
    setSelectedTierIndex(0);
    setBookingStep(1);
    setProjectTitle(`Engagement: ${service.title}`);
    setProjectBrief('');
    setTargetDeadline(service.duration || 'Within 5 days');
    setCommunicationMode(service.deliveryMode.includes('On-Site') ? 'On-Premises Visit' : 'Collobster Workspace Chat');
    setIsCouponApplied(true);
  };

  // Complete booking and provision order
  const handleConfirmOrder = async () => {
    if (!bookingService) return;
    setIsSubmittingBooking(true);
    try {
      const selectedTier = bookingService.tiers?.[selectedTierIndex] || {
        name: 'Standard Package',
        price: bookingService.price,
        days: bookingService.duration,
      };

      const finalPrice = isCouponApplied
        ? Math.round(selectedTier.price * 0.8)
        : selectedTier.price;

      const generatedOrderId = `CL-ORD-${Math.floor(10000 + Math.random() * 90000)}`;

      const draft = {
        id: `booking-${Date.now()}`,
        orderId: generatedOrderId,
        title: bookingService.title,
        category: bookingService.category,
        provider: bookingService.provider,
        providerRole: bookingService.providerRole,
        providerAvatar: bookingService.providerAvatar,
        price: finalPrice,
        tierName: selectedTier.name,
        deliveryMode: bookingService.deliveryMode,
        startsAt: new Date().toISOString(),
        orderStatus: 'Requirements Submitted',
        serviceAddress: communicationMode === 'On-Premises Visit' ? onSiteAddress : 'Remote Workspace',
        notes: `Brief: ${projectBrief || 'Standard project requirements'}. Delivery via ${communicationMode}.`,
        milestones: bookingService.milestones || ['Phase 1 Setup', 'Phase 2 Deliverables', 'Final Approval'],
      };

      await a.bookService(bookingService, draft);

      // Enhance in state so the order tracker has full fields immediately
      a.setBookings((prev) => [
        draft,
        ...prev.filter((item) => item.id !== draft.id),
      ]);

      a.notify(
        'Order Placed & Escrow Secured',
        `Order ${generatedOrderId} for "${bookingService.title}" confirmed. ₹${finalPrice} deposited in Collobster Escrow.`,
        'Services'
      );
      a.toast(`🎉 Order ${generatedOrderId} initiated! ₹${finalPrice} placed safely in escrow.`);

      setBookingService(null);
      setActiveTab('orders'); // Jump to active orders view to see progress
    } catch (err) {
      a.toast(err.message || 'Failed to place order. Please try again.');
    } finally {
      setIsSubmittingBooking(false);
    }
  };

  // Advance Order Lifecycle (Demo Simulation to show full end-to-end progression)
  const handleAdvanceOrderStatus = (orderId, newStatus) => {
    a.setBookings((prev) => {
      const existing = prev.find((b) => b.orderId === orderId || b.id === orderId);
      if (existing) {
        return prev.map((b) => {
          if (b.orderId === orderId || b.id === orderId) {
            return {
              ...b,
              orderStatus: newStatus,
              submittedDeliverables: newStatus === 'Deliverables Under Review' ? {
                summary: 'Project milestones complete. Full source code, test reports, and verification documents uploaded.',
                githubLink: 'https://github.com/collobster/client-final-delivery',
                demoLink: 'https://staging.collobster.dev',
              } : b.submittedDeliverables,
            };
          }
          return b;
        });
      }
      // If updating a seeded demo order
      return [
        {
          id: orderId,
          orderId,
          title: 'Full-Stack Web & Mobile MVP Architecture & Engineering',
          category: 'software',
          provider: 'Siddharth Nair',
          providerRole: 'Ex-Staff Architect · 11 yrs exp',
          providerAvatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
          price: 24999,
          deliveryMode: 'Remote (Global)',
          orderStatus: newStatus,
          tierName: 'Starter MVP',
          createdAt: 'Active today',
          milestones: ['Architecture & Schema (30%)', 'Core APIs (40%)', 'QA & Delivery (30%)'],
          submittedDeliverables: {
            summary: 'Milestones completed. Ready for final client inspection.',
            githubLink: 'https://github.com/collobster/client-final-delivery',
            demoLink: 'https://staging.collobster.dev',
          },
        },
      ];
    });
    a.toast(`Order stage advanced to "${newStatus}"`);
  };

  // Client approves deliverables and releases escrow
  const handleApproveAndReleaseEscrow = (order) => {
    handleAdvanceOrderStatus(order.orderId || order.id, 'Completed');
    setReviewDeliverablesOrder(null);
    a.notify(
      'Escrow Funds Released',
      `Payment of ₹${order.price} released to ${order.provider} for order ${order.orderId}. Milestone completed!`,
      'Services'
    );
    a.toast(`🎉 Escrow funds released to ${order.provider}!`);
    setRatingModalOrder(order);
  };

  // Submit client rating & review
  const handleSubmitReview = () => {
    if (!ratingModalOrder) return;
    a.toast(`Thank you! Rating of ${ratingStars}★ submitted for ${ratingModalOrder.provider}.`);
    setRatingModalOrder(null);
    setRatingReviewText('');
  };

  // Provider: List New Service
  const handleCreateNewService = () => {
    if (!newServiceDraft.title.trim()) {
      a.toast('Please provide a service title');
      return;
    }
    const created = {
      id: `custom-srv-${Date.now()}`,
      title: newServiceDraft.title.trim(),
      category: newServiceDraft.category,
      categoryName: serviceCategories.find((c) => c.id === newServiceDraft.category)?.name || 'Specialist Service',
      provider: a.profile?.name || 'You (Verified Pro)',
      providerRole: 'Collobster Verified Specialist',
      providerAvatar: a.profile?.image || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
      price: Number(newServiceDraft.price) || 4999,
      pricingType: newServiceDraft.pricingType,
      duration: newServiceDraft.duration,
      deliveryMode: newServiceDraft.deliveryMode,
      description: newServiceDraft.description || 'Specialist professional service provided via Collobster.',
      highlights: newServiceDraft.highlights.split(',').map((s) => s.trim()).filter(Boolean),
    };
    a.setServiceListings((prev) => [created, ...(prev || [])]);
    a.notify('New Service Published', `Your service "${created.title}" is now live in the Collobster Marketplace.`, 'Services');
    a.toast(`🎉 Service "${created.title}" published successfully!`);
    setShowListServiceModal(false);
    setNewServiceDraft({
      title: '',
      category: 'software',
      deliveryMode: 'Remote (Global)',
      pricingType: 'Milestone-based Package',
      price: 9999,
      duration: '5 days',
      highlights: 'Production quality deliverables, Full code repository, 14-day warranty',
      description: '',
    });
    setActiveTab('catalog');
  };

  // Client: Post Custom RFP
  const handlePostRfp = () => {
    if (!newRfpDraft.title.trim()) {
      a.toast('Please enter a request title');
      return;
    }
    const createdRfp = {
      id: `rfp-${Date.now()}`,
      title: newRfpDraft.title.trim(),
      category: newRfpDraft.category,
      budget: newRfpDraft.budget || '₹5,000 – ₹10,000',
      budgetNum: 8000,
      deadline: newRfpDraft.deadline || 'Within 1 week',
      author: a.profile?.name || 'You',
      authorAvatar: a.profile?.image || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
      description: newRfpDraft.description || 'Looking for an experienced professional to assist with project scope.',
      deliveryMode: newRfpDraft.deliveryMode,
      proposalsCount: 0,
      status: 'Open for Proposals',
      tags: [newRfpDraft.category.toUpperCase(), newRfpDraft.deliveryMode],
    };
    setCustomRequests((prev) => [createdRfp, ...prev]);
    a.notify('Custom RFP Posted', `Your request "${createdRfp.title}" is now open for specialist proposals.`, 'Services');
    a.toast(`🎉 Custom request published! Community specialists will submit proposals.`);
    setShowPostRfpModal(false);
    setNewRfpDraft({
      title: '',
      category: 'legal',
      budget: '₹5,000 – ₹10,000',
      deadline: 'Within 3 days',
      deliveryMode: 'Remote',
      description: '',
    });
    setActiveTab('requests');
  };

  return (
    <div className="collob-services-hub">
      {/* ── Page Header ── */}
      <PageHeader
        eyebrow="COLLOBSTER MARKETPLACE"
        title="Services & Professional Engagements"
        description="Engage verified specialists across software & cloud, legal opinions, hardware repairs, CA tax, and facility operations — with milestone escrow protection."
      >
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <Button
            variant="secondary"
            icon={FileText}
            onClick={() => setShowPostRfpModal(true)}
          >
            Post a Custom Request (RFP)
          </Button>
          <Button
            variant="primary"
            icon={Plus}
            onClick={() => setShowListServiceModal(true)}
          >
            Offer / List a Service
          </Button>
        </div>
      </PageHeader>

      {/* ── Collobster Trust & Quality Guarantee Banner ── */}
      <div className="services-hero-banner">
        <div className="services-hero-left">
          <span className="services-kicker-tag">
            <ShieldCheck size={14} /> Collobster Escrow &amp; Quality Guarantee
          </span>
          <h2 className="services-hero-title">
            Avail any professional service with zero risk.
          </h2>
          <p className="services-hero-desc">
            Whether you need a founder legal opinion, full-stack software MVP, workstation motherboard repair, or pre-series A financial model — your funds are protected in platform escrow and only released when you approve milestone deliverables.
          </p>

          <div className="services-hero-guarantees">
            <span className="services-guarantee-pill">
              <CheckCircle2 size={15} color="var(--ocean)" /> 100% Milestone-Backed Escrow
            </span>
            <span className="services-guarantee-pill">
              <CheckCircle2 size={15} color="var(--ocean)" /> 0% Platform Buyer Commission
            </span>
            <span className="services-guarantee-pill">
              <CheckCircle2 size={15} color="var(--ocean)" /> Bar &amp; Industry Verified Specialists
            </span>
            <span className="services-guarantee-pill">
              <CheckCircle2 size={15} color="var(--ocean)" /> Dispute Resolution &amp; Rework Cover
            </span>
          </div>

          <div className="services-hero-actions">
            <Button
              variant="primary"
              size="md"
              onClick={() => {
                setActiveTab('catalog');
                setActiveCategory('all');
              }}
            >
              Explore All Services
            </Button>
            <div style={{ fontSize: '0.82rem', color: 'var(--muted)', display: 'inline-flex', alignItems: 'center', gap: 6 }}>
              <span>Resident Coupon:</span>
              <strong style={{ color: 'var(--coral)', background: 'var(--soft)', padding: '2px 8px', borderRadius: 4, letterSpacing: 0.5 }}>
                COLLOB20
              </strong>
              <span>(20% OFF)</span>
            </div>
          </div>
        </div>

        <div className="services-hero-stats">
          <div className="services-stat-box">
            <strong>₹1.8 Cr+</strong>
            <span>Escrow Protected</span>
          </div>
          <div className="services-stat-box">
            <strong>4.94★</strong>
            <span>Avg Rating</span>
          </div>
          <div className="services-stat-box">
            <strong>&lt; 2 hrs</strong>
            <span>First Response</span>
          </div>
        </div>
      </div>

      {/* ── Main Navigation Tabs ── */}
      <Tabs
        items={[
          `Explore Catalog (${filteredServices.length})`,
          `Active Engagements & Orders (${activeOrders.length})`,
          `Custom Requests & RFPs (${customRequests.length})`,
        ]}
        value={
          activeTab === 'catalog'
            ? `Explore Catalog (${filteredServices.length})`
            : activeTab === 'orders'
            ? `Active Engagements & Orders (${activeOrders.length})`
            : `Custom Requests & RFPs (${customRequests.length})`
        }
        onChange={(val) => {
          if (val.startsWith('Explore')) setActiveTab('catalog');
          else if (val.startsWith('Active')) setActiveTab('orders');
          else setActiveTab('requests');
        }}
      />

      {/* ── TAB 1: SERVICE CATALOG & MARKETPLACE ── */}
      {activeTab === 'catalog' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Universal Category Strip */}
          <div className="services-category-strip">
            {serviceCategories.map((cat) => {
              const isActive = activeCategory === cat.id;
              return (
                <button
                  key={cat.id}
                  type="button"
                  className={`service-cat-chip ${isActive ? 'active' : ''}`}
                  onClick={() => setActiveCategory(cat.id)}
                >
                  {cat.id === 'all' && <Sparkles size={15} />}
                  {cat.id === 'software' && <Code2 size={15} />}
                  {cat.id === 'legal' && <Scale size={15} />}
                  {cat.id === 'repairs' && <Wrench size={15} />}
                  {cat.id === 'finance' && <LineChart size={15} />}
                  {cat.id === 'design' && <Palette size={15} />}
                  {cat.id === 'facility' && <Building2 size={15} />}
                  <span>{cat.name}</span>
                </button>
              );
            })}
          </div>

          {/* Search, Mode & Filter Toolbar */}
          <div className="services-filter-bar">
            <div className="services-search-wrapper">
              <SearchField
                value={query}
                onChange={setQuery}
                placeholder="Search services, legal opinions, software, repairs, CA advisory..."
              />
            </div>

            <div className="services-filter-group">
              {/* Delivery Mode Filter */}
              <select
                className="services-filter-select"
                value={deliveryModeFilter}
                onChange={(e) => setDeliveryModeFilter(e.target.value)}
                aria-label="Delivery Mode"
              >
                <option value="all">🌐 All Delivery Modes</option>
                <option value="Remote">💻 Remote (Global)</option>
                <option value="On-Premises">📍 On-Premises / On-Site</option>
                <option value="Hybrid">🔄 Hybrid</option>
              </select>

              {/* Price Filter */}
              <select
                className="services-filter-select"
                value={priceFilter}
                onChange={(e) => setPriceFilter(e.target.value)}
                aria-label="Price Filter"
              >
                <option value="all">💰 All Budgets</option>
                <option value="under-3000">Under ₹3,000</option>
                <option value="3000-10000">₹3,000 – ₹10,000</option>
                <option value="above-10000">Above ₹10,000</option>
              </select>

              {/* Sort By */}
              <select
                className="services-filter-select"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                aria-label="Sort By"
              >
                <option value="recommended">⭐ Recommended</option>
                <option value="rating">★ Highest Rated</option>
                <option value="price-asc">Price: Low to High</option>
                <option value="price-desc">Price: High to Low</option>
              </select>
            </div>
          </div>

          {/* Universal Services Cards Grid */}
          {filteredServices.length ? (
            <div className="universal-services-grid">
              {filteredServices.map((service) => (
                <article className="universal-service-card" key={service.id}>
                  <div className="service-card-media">
                    <img src={service.image} alt={service.title} />
                    <span className={`service-card-badge ${service.badgeColor || 'blue'}`}>
                      {service.badge}
                    </span>
                    <span className="service-delivery-pill">
                      {service.deliveryMode.includes('On-Premises') ? '📍 On-Site' : '🌐 Remote'}
                    </span>
                  </div>

                  <div className="service-card-body">
                    {/* Provider Row */}
                    <div className="service-provider-row">
                      <img
                        src={service.providerAvatar}
                        alt={service.provider}
                        className="service-provider-avatar"
                      />
                      <div className="service-provider-meta">
                        <div className="service-provider-name">
                          <span>{service.provider}</span>
                          <CheckCircle2 size={13} color="var(--ocean)" />
                        </div>
                        <div className="service-provider-role">{service.providerRole}</div>
                      </div>
                      <div className="service-rating-chip">
                        <Star size={13} fill="var(--gold)" color="var(--gold)" />
                        <span>{service.rating}</span>
                        <small style={{ color: 'var(--muted)', fontWeight: 500 }}>
                          ({service.reviewsCount})
                        </small>
                      </div>
                    </div>

                    {/* Title & Description */}
                    <h3 className="service-card-title">{service.title}</h3>
                    <p className="service-card-description">{service.description}</p>

                    {/* Deliverables Checklist */}
                    <ul className="service-card-deliverables">
                      {(service.highlights || []).slice(0, 3).map((hl, idx) => (
                        <li key={idx}>
                          <CheckCircle2 size={14} className="service-deliverable-check" />
                          <span>{hl}</span>
                        </li>
                      ))}
                    </ul>

                    {/* Footer & Actions */}
                    <div className="service-card-footer">
                      <div className="service-pricing-block">
                        <div className="service-pricing-val">
                          ₹{service.price.toLocaleString()}
                        </div>
                        <span className="service-pricing-sub">
                          {service.pricingType} · {service.duration}
                        </span>
                      </div>

                      <div className="service-card-buttons">
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => setSelectedScopeService(service)}
                        >
                          View Scope
                        </Button>
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => handleInitiateBooking(service)}
                        >
                          Book Service
                        </Button>
                      </div>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <Empty
              title="No services found"
              text={query ? `No services matching "${query}".` : 'Try changing your category or filter selection.'}
              action="Reset Filters"
              onAction={() => {
                setQuery('');
                setActiveCategory('all');
                setDeliveryModeFilter('all');
                setPriceFilter('all');
              }}
            />
          )}
        </div>
      )}

      {/* ── TAB 2: ACTIVE ENGAGEMENTS & ORDERS (END-TO-END LIFECYCLE) ── */}
      {activeTab === 'orders' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <strong style={{ fontSize: '1rem', color: 'var(--ink)' }}>
                Active Engagements &amp; Milestone Deliveries
              </strong>
              <p style={{ margin: '2px 0 0', fontSize: '0.82rem', color: 'var(--muted)' }}>
                Track live work, review submitted deliverables, request revisions, and release escrow funds when satisfied.
              </p>
            </div>
            <Button
              variant="secondary"
              size="sm"
              icon={Plus}
              onClick={() => setActiveTab('catalog')}
            >
              Order New Service
            </Button>
          </div>

          {activeOrders.length ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {activeOrders.map((order) => {
                const isStep1 = order.status === 'Requirements Submitted';
                const isStep2 = order.status === 'In Progress' || order.status === 'Revision in Progress';
                const isStep3 = order.status === 'Deliverables Under Review';
                const isCompleted = order.status === 'Completed';

                return (
                  <article className="order-tracker-card" key={order.id || order.orderId}>
                    {/* Top Row: Order ID, Provider, Price */}
                    <div className="order-tracker-top">
                      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                        <img
                          src={order.providerAvatar}
                          alt={order.provider}
                          style={{ width: 44, height: 44, borderRadius: '50%', objectFit: 'cover' }}
                        />
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                            <span className="order-id-badge">{order.orderId}</span>
                            <span style={{ fontSize: '0.76rem', color: 'var(--muted)' }}>
                              Placed on {order.createdAt}
                            </span>
                          </div>
                          <h4 style={{ margin: '4px 0 2px', fontSize: '1.05rem', color: 'var(--ink)' }}>
                            {order.title}
                          </h4>
                          <span style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>
                            Lead: <strong>{order.provider}</strong> ({order.providerRole}) · Tier: <em>{order.tierName}</em>
                          </span>
                        </div>
                      </div>

                      <div style={{ textAlign: 'right' }}>
                        <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--ink)', fontFamily: 'Fraunces, serif' }}>
                          ₹{order.price.toLocaleString()}
                        </div>
                        <span style={{ fontSize: '0.74rem', color: '#059669', background: '#ecfdf5', padding: '2px 6px', borderRadius: 4, fontWeight: 700 }}>
                          🔒 Held in Escrow
                        </span>
                      </div>
                    </div>

                    {/* Visual Milestone Stepper */}
                    <div className="order-stepper">
                      <div className={`order-step-node ${isStep1 || isStep2 || isStep3 || isCompleted ? 'completed' : ''}`}>
                        <span className="order-step-circle">1</span>
                        <span>Requirements Brief</span>
                        <div className="order-step-line" />
                      </div>

                      <div className={`order-step-node ${isStep2 || isStep3 || isCompleted ? (isStep3 || isCompleted ? 'completed' : 'current') : ''}`}>
                        <span className="order-step-circle">2</span>
                        <span>Work In Progress</span>
                        <div className="order-step-line" />
                      </div>

                      <div className={`order-step-node ${isStep3 || isCompleted ? (isCompleted ? 'completed' : 'current') : ''}`}>
                        <span className="order-step-circle">3</span>
                        <span>Deliverables Review</span>
                        <div className="order-step-line" />
                      </div>

                      <div className={`order-step-node ${isCompleted ? 'completed' : ''}`}>
                        <span className="order-step-circle">4</span>
                        <span>Approved &amp; Released</span>
                      </div>
                    </div>

                    {/* Deliverables / Status Callout Box */}
                    {isStep3 && order.submittedDeliverables && (
                      <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: 8, padding: '14px', display: 'flex', flexDirection: 'column', gap: 8 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#166534', fontWeight: 700, fontSize: '0.9rem' }}>
                          <CheckCircle2 size={16} /> Deliverables Submitted for Inspection
                        </div>
                        <p style={{ margin: 0, fontSize: '0.82rem', color: '#14532d', lineHeight: 1.5 }}>
                          {order.submittedDeliverables.summary}
                        </p>
                        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginTop: 4 }}>
                          {order.submittedDeliverables.githubLink && (
                            <a
                              href={order.submittedDeliverables.githubLink}
                              target="_blank"
                              rel="noreferrer"
                              style={{ fontSize: '0.8rem', color: '#15803d', fontWeight: 700, textDecoration: 'underline' }}
                            >
                              📦 View Code Repository ↗
                            </a>
                          )}
                          {order.submittedDeliverables.demoLink && (
                            <a
                              href={order.submittedDeliverables.demoLink}
                              target="_blank"
                              rel="noreferrer"
                              style={{ fontSize: '0.8rem', color: '#15803d', fontWeight: 700, textDecoration: 'underline' }}
                            >
                              🚀 Open Live Preview Environment ↗
                            </a>
                          )}
                        </div>
                      </div>
                    )}

                    {isCompleted && (
                      <div style={{ background: 'var(--soft)', border: '1px solid var(--line)', borderRadius: 8, padding: '12px 14px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#059669', fontWeight: 700, fontSize: '0.85rem' }}>
                          <CheckCircle2 size={16} /> Order Completed · Escrow Released
                        </div>
                        <Button
                          size="sm"
                          variant="secondary"
                          icon={Star}
                          onClick={() => setRatingModalOrder(order)}
                        >
                          {order.reviewRating ? `Rated ${order.reviewRating}★ · Edit Review` : 'Leave Rating & Review'}
                        </Button>
                      </div>
                    )}

                    {/* Interactive Action Buttons */}
                    <div className="order-actions-row">
                      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                        {isStep3 && (
                          <>
                            <Button
                              variant="primary"
                              size="sm"
                              icon={CheckCircle2}
                              onClick={() => handleApproveAndReleaseEscrow(order)}
                            >
                              Approve &amp; Release Escrow
                            </Button>
                            <Button
                              variant="secondary"
                              size="sm"
                              icon={Edit3}
                              onClick={() => {
                                setReviewDeliverablesOrder(order);
                                setRevisionNotes('');
                              }}
                            >
                              Request Revision
                            </Button>
                          </>
                        )}

                        {isStep1 && (
                          <Button
                            variant="secondary"
                            size="sm"
                            onClick={() => handleAdvanceOrderStatus(order.orderId || order.id, 'In Progress')}
                            title="Simulate provider accepting brief and beginning milestone work"
                          >
                            Advance to In-Progress (Demo)
                          </Button>
                        )}

                        {isStep2 && (
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => handleAdvanceOrderStatus(order.orderId || order.id, 'Deliverables Under Review')}
                            title="Simulate provider submitting completed deliverables for client review"
                          >
                            Submit Deliverables (Demo)
                          </Button>
                        )}

                        <Button
                          variant="quiet"
                          size="sm"
                          icon={MessageSquare}
                          onClick={() => {
                            a.toast(`Opened collaborative workspace thread with ${order.provider}`);
                          }}
                        >
                          Message Provider
                        </Button>
                      </div>

                      {!isCompleted && (
                        <button
                          type="button"
                          style={{ border: 0, background: 'none', color: '#dc2626', fontSize: '0.78rem', cursor: 'pointer', padding: '4px 8px' }}
                          onClick={() => {
                            if (window.confirm(`Cancel order ${order.orderId}? Full escrow amount of ₹${order.price} will be refunded immediately.`)) {
                              a.setBookings((prev) => prev.filter((b) => b.orderId !== order.orderId && b.id !== order.id));
                              a.toast(`Order ${order.orderId} cancelled. Escrow refund processed.`);
                            }
                          }}
                        >
                          Cancel &amp; Refund Escrow
                        </button>
                      )}
                    </div>
                  </article>
                );
              })}
            </div>
          ) : (
            <Empty
              title="No active engagements yet"
              text="When you book a software, legal, repair, or advisory service, your order lifecycle and milestones will appear here."
              action="Browse Service Catalog"
              onAction={() => setActiveTab('catalog')}
            />
          )}
        </div>
      )}

      {/* ── TAB 3: CUSTOM REQUESTS & RFPs (BUYER POSTING BOARD) ── */}
      {activeTab === 'requests' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
            <div>
              <strong style={{ fontSize: '1rem', color: 'var(--ink)' }}>
                Community Service RFPs &amp; Custom Requests
              </strong>
              <p style={{ margin: '2px 0 0', fontSize: '0.82rem', color: 'var(--muted)' }}>
                Can&apos;t find the exact scope? Post your custom project brief and receive competitive proposals from verified practitioners.
              </p>
            </div>
            <Button
              variant="primary"
              icon={Plus}
              onClick={() => setShowPostRfpModal(true)}
            >
              Post a Custom RFP
            </Button>
          </div>

          <div className="rfp-grid">
            {customRequests.map((rfp) => (
              <article className="rfp-card" key={rfp.id}>
                <div className="rfp-card-header">
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <img
                      src={rfp.authorAvatar}
                      alt={rfp.author}
                      style={{ width: 28, height: 28, borderRadius: '50%', objectFit: 'cover' }}
                    />
                    <div>
                      <strong style={{ fontSize: '0.82rem', color: 'var(--ink)' }}>{rfp.author}</strong>
                      <div style={{ fontSize: '0.72rem', color: 'var(--muted)' }}>Target: {rfp.deadline}</div>
                    </div>
                  </div>
                  <span className="rfp-budget-pill">{rfp.budget}</span>
                </div>

                <h4 style={{ margin: '4px 0', fontSize: '0.98rem', color: 'var(--ink)', lineHeight: 1.35 }}>
                  {rfp.title}
                </h4>
                <p style={{ margin: 0, fontSize: '0.82rem', color: 'var(--muted)', lineHeight: 1.5 }}>
                  {rfp.description}
                </p>

                <div className="rfp-tag-row">
                  {(rfp.tags || []).map((t, idx) => (
                    <span className="rfp-tag" key={idx}>#{t}</span>
                  ))}
                </div>

                <div className="rfp-footer">
                  <span>💬 {rfp.proposalsCount} proposals received</span>
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => {
                      a.toast(`Proposal submission drawer opened for "${rfp.title}"`);
                    }}
                  >
                    Submit Proposal
                  </Button>
                </div>
              </article>
            ))}
          </div>
        </div>
      )}

      {/* ── MODAL 1: VIEW FULL SERVICE SCOPE & TIERS ── */}
      {selectedScopeService && (
        <Dialog
          title={`Service Scope: ${selectedScopeService.title}`}
          onClose={() => setSelectedScopeService(null)}
          wide
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            {/* Top Overview */}
            <div style={{ display: 'flex', gap: 16, alignItems: 'center', background: 'var(--soft)', padding: 16, borderRadius: 8 }}>
              <img
                src={selectedScopeService.providerAvatar}
                alt={selectedScopeService.provider}
                style={{ width: 56, height: 56, borderRadius: '50%', objectFit: 'cover' }}
              />
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <strong style={{ fontSize: '1.05rem', color: 'var(--ink)' }}>{selectedScopeService.provider}</strong>
                  <CheckCircle2 size={15} color="var(--ocean)" />
                  <span style={{ fontSize: '0.74rem', background: 'var(--surface)', padding: '2px 6px', borderRadius: 4, fontWeight: 700, color: 'var(--ocean)' }}>
                    {selectedScopeService.badge}
                  </span>
                </div>
                <div style={{ fontSize: '0.82rem', color: 'var(--muted)' }}>
                  {selectedScopeService.providerRole} · ⭐ {selectedScopeService.rating} ({selectedScopeService.reviewsCount} verified reviews)
                </div>
                <div style={{ fontSize: '0.8rem', color: 'var(--ink)', marginTop: 4 }}>
                  🌐 Delivery: <strong>{selectedScopeService.deliveryMode}</strong> · ⏱️ Turnaround: <strong>{selectedScopeService.duration}</strong>
                </div>
              </div>
            </div>

            {/* Inclusions & Highlights */}
            <div>
              <strong style={{ fontSize: '0.9rem', color: 'var(--ink)', display: 'block', marginBottom: 8 }}>
                Key Included Deliverables &amp; Milestones:
              </strong>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 8 }}>
                {(selectedScopeService.highlights || []).map((h, i) => (
                  <div key={i} style={{ display: 'flex', alignItems: 'flex-start', gap: 8, fontSize: '0.84rem', background: 'var(--canvas)', padding: '8px 12px', borderRadius: 6 }}>
                    <CheckCircle2 size={15} color="var(--ocean)" style={{ flexShrink: 0, marginTop: 2 }} />
                    <span>{h}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Tier Packages */}
            {selectedScopeService.tiers && (
              <div>
                <strong style={{ fontSize: '0.9rem', color: 'var(--ink)', display: 'block', marginBottom: 8 }}>
                  Available Package Tiers:
                </strong>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: 12 }}>
                  {selectedScopeService.tiers.map((tier, idx) => (
                    <div key={idx} style={{ border: '1px solid var(--line)', borderRadius: 8, padding: 14, background: 'var(--surface)', display: 'flex', flexDirection: 'column', gap: 6 }}>
                      <strong style={{ fontSize: '0.95rem', color: 'var(--ink)' }}>{tier.name}</strong>
                      <div style={{ fontSize: '1.25rem', fontWeight: 800, color: 'var(--ocean)', fontFamily: 'Fraunces, serif' }}>
                        ₹{tier.price.toLocaleString()}
                      </div>
                      <small style={{ color: 'var(--muted)', fontSize: '0.78rem' }}>⏱️ Delivered in {tier.days}</small>
                      <p style={{ margin: '4px 0 0', fontSize: '0.82rem', color: 'var(--muted)', lineHeight: 1.4 }}>{tier.desc}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Escrow Guarantee Box */}
            <div className="escrow-notice-box">
              <ShieldCheck size={28} color="var(--ocean)" style={{ flexShrink: 0 }} />
              <div>
                <strong>Collobster Milestone Escrow Guarantee:</strong>
                <p style={{ margin: '2px 0 0', fontSize: '0.8rem', color: 'var(--muted)' }}>
                  Your funds remain safely deposited in escrow throughout the engagement. The provider receives payment only after you inspect and give final sign-off on deliverables.
                </p>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 6 }}>
              <Button variant="quiet" onClick={() => setSelectedScopeService(null)}>
                Close
              </Button>
              <Button
                variant="primary"
                onClick={() => {
                  const s = selectedScopeService;
                  setSelectedScopeService(null);
                  handleInitiateBooking(s);
                }}
              >
                Proceed to Book Service
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* ── MODAL 2: MULTI-STEP END-TO-END BOOKING & CHECKOUT FLOW ── */}
      {bookingService && (
        <Dialog
          title={`Book Engagement: ${bookingService.title}`}
          onClose={() => !isSubmittingBooking && setBookingService(null)}
          wide
        >
          <div className="booking-checkout-flow">
            {/* Step Progress Bar */}
            <div className="order-stepper" style={{ border: 0, padding: 0 }}>
              <div className={`order-step-node ${bookingStep >= 1 ? (bookingStep === 1 ? 'current' : 'completed') : ''}`}>
                <span className="order-step-circle">1</span>
                <span>Scope Tier</span>
                <div className="order-step-line" />
              </div>
              <div className={`order-step-node ${bookingStep >= 2 ? (bookingStep === 2 ? 'current' : 'completed') : ''}`}>
                <span className="order-step-circle">2</span>
                <span>Project Brief</span>
                <div className="order-step-line" />
              </div>
              <div className={`order-step-node ${bookingStep >= 3 ? (bookingStep === 3 ? 'current' : 'completed') : ''}`}>
                <span className="order-step-circle">3</span>
                <span>Channel &amp; Schedule</span>
                <div className="order-step-line" />
              </div>
              <div className={`order-step-node ${bookingStep === 4 ? 'current' : ''}`}>
                <span className="order-step-circle">4</span>
                <span>Escrow Summary</span>
              </div>
            </div>

            {/* STEP 1: CHOOSE PACKAGE TIER */}
            {bookingStep === 1 && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <strong style={{ fontSize: '0.92rem', color: 'var(--ink)' }}>
                  1. Select Your Service Tier:
                </strong>
                <div className="tier-selector-grid">
                  {(bookingService.tiers || [
                    { name: 'Standard Package', price: bookingService.price, days: bookingService.duration, desc: 'Complete project deliverables' },
                  ]).map((tier, idx) => (
                    <button
                      key={idx}
                      type="button"
                      className={`tier-card-btn ${selectedTierIndex === idx ? 'selected' : ''}`}
                      onClick={() => setSelectedTierIndex(idx)}
                    >
                      <div className="tier-name">{tier.name}</div>
                      <div className="tier-price">₹{tier.price.toLocaleString()}</div>
                      <small style={{ color: 'var(--muted)', fontSize: '0.74rem' }}>⏱️ {tier.days}</small>
                      <div className="tier-desc">{tier.desc}</div>
                    </button>
                  ))}
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 10 }}>
                  <Button variant="quiet" onClick={() => setBookingService(null)}>
                    Cancel
                  </Button>
                  <Button variant="primary" onClick={() => setBookingStep(2)}>
                    Continue to Project Brief →
                  </Button>
                </div>
              </div>
            )}

            {/* STEP 2: PROJECT BRIEF & REQUIREMENTS */}
            {bookingStep === 2 && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <strong style={{ fontSize: '0.92rem', color: 'var(--ink)' }}>
                  2. Describe Your Project Requirements &amp; Scope:
                </strong>

                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                    Project / Engagement Title:
                  </label>
                  <input
                    type="text"
                    value={projectTitle}
                    onChange={(e) => setProjectTitle(e.target.value)}
                    style={{ width: '100%', padding: '9px 12px', borderRadius: 6, border: '1px solid var(--line)', background: 'var(--canvas)', fontSize: '0.88rem' }}
                    placeholder="e.g. Series A Co-Founder Agreement or React Web App MVP"
                  />
                </div>

                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                    Detailed Project Instructions / Objectives:
                  </label>
                  <textarea
                    rows={4}
                    value={projectBrief}
                    onChange={(e) => setProjectBrief(e.target.value)}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: 6, border: '1px solid var(--line)', background: 'var(--canvas)', fontSize: '0.86rem', resize: 'vertical' }}
                    placeholder="Specify particular clauses, tech stack requirements, hardware symptoms, or deliverables you need the specialist to focus on..."
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10, marginTop: 10 }}>
                  <Button variant="quiet" onClick={() => setBookingStep(1)}>
                    ← Back to Tiers
                  </Button>
                  <Button variant="primary" onClick={() => setBookingStep(3)}>
                    Continue to Delivery Channel →
                  </Button>
                </div>
              </div>
            )}

            {/* STEP 3: PREFERENCES & SCHEDULE */}
            {bookingStep === 3 && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                <strong style={{ fontSize: '0.92rem', color: 'var(--ink)' }}>
                  3. Delivery Channel &amp; Schedule:
                </strong>

                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 6 }}>
                    Preferred Collaboration Mode:
                  </label>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 10 }}>
                    {[
                      { mode: 'Collobster Workspace Chat', desc: 'Async threads, code review & file sharing' },
                      { mode: 'Milestone Video Calls', desc: '1-on-1 scheduled video consultations' },
                      { mode: 'On-Premises Visit', desc: 'In-person specialist visit at office/residence' },
                    ].map((item) => (
                      <button
                        key={item.mode}
                        type="button"
                        className={`tier-card-btn ${communicationMode === item.mode ? 'selected' : ''}`}
                        onClick={() => setCommunicationMode(item.mode)}
                      >
                        <div className="tier-name">{item.mode}</div>
                        <div className="tier-desc">{item.desc}</div>
                      </button>
                    ))}
                  </div>
                </div>

                {communicationMode === 'On-Premises Visit' && (
                  <div>
                    <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                      On-Premises Address / Location:
                    </label>
                    <input
                      type="text"
                      value={onSiteAddress}
                      onChange={(e) => setOnSiteAddress(e.target.value)}
                      style={{ width: '100%', padding: '9px 12px', borderRadius: 6, border: '1px solid var(--line)', background: 'var(--canvas)', fontSize: '0.88rem' }}
                      placeholder="Enter office, lab or residential location"
                    />
                  </div>
                )}

                <div>
                  <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                    Target Completion Timeline:
                  </label>
                  <select
                    className="services-filter-select"
                    value={targetDeadline}
                    onChange={(e) => setTargetDeadline(e.target.value)}
                    style={{ width: '100%' }}
                  >
                    <option value="Urgent (24–48 hours)">⚡ Urgent (24–48 hours)</option>
                    <option value="Within 5 days">📅 Standard (Within 5 days)</option>
                    <option value="Within 10 days">📆 Flexible (Within 10 days)</option>
                  </select>
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10, marginTop: 10 }}>
                  <Button variant="quiet" onClick={() => setBookingStep(2)}>
                    ← Back to Brief
                  </Button>
                  <Button variant="primary" onClick={() => setBookingStep(4)}>
                    Continue to Escrow Checkout →
                  </Button>
                </div>
              </div>
            )}

            {/* STEP 4: ESCROW SUMMARY & CONFIRMATION */}
            {bookingStep === 4 && (() => {
              const selectedTier = bookingService.tiers?.[selectedTierIndex] || {
                name: 'Standard Package',
                price: bookingService.price,
                days: bookingService.duration,
              };
              const discount = isCouponApplied ? Math.round(selectedTier.price * 0.2) : 0;
              const finalAmount = selectedTier.price - discount;

              return (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                  <strong style={{ fontSize: '0.92rem', color: 'var(--ink)' }}>
                    4. Order &amp; Milestone Escrow Breakdown:
                  </strong>

                  <div style={{ background: 'var(--canvas)', border: '1px solid var(--line)', borderRadius: 8, padding: 16, display: 'flex', flexDirection: 'column', gap: 8 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.88rem' }}>
                      <span style={{ color: 'var(--muted)' }}>Service:</span>
                      <strong>{bookingService.title}</strong>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.88rem' }}>
                      <span style={{ color: 'var(--muted)' }}>Selected Tier:</span>
                      <span>{selectedTier.name} (₹{selectedTier.price.toLocaleString()})</span>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.88rem' }}>
                      <span style={{ color: 'var(--muted)' }}>Platform Buyer Fee:</span>
                      <span style={{ color: '#059669', fontWeight: 700 }}>₹0 (Free / Zero Commission)</span>
                    </div>

                    {isCouponApplied && (
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.88rem', color: 'var(--coral)' }}>
                        <span>Promo Code (COLLOB20 - 20% OFF):</span>
                        <strong>-₹{discount.toLocaleString()}</strong>
                      </div>
                    )}

                    <div style={{ height: 1, background: 'var(--line)', margin: '4px 0' }} />

                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '1.15rem' }}>
                      <strong>Total Deposited to Escrow:</strong>
                      <strong style={{ color: 'var(--ocean)', fontFamily: 'Fraunces, serif' }}>
                        ₹{finalAmount.toLocaleString()}
                      </strong>
                    </div>
                  </div>

                  {/* Escrow Guarantee Statement */}
                  <div className="escrow-notice-box">
                    <ShieldCheck size={28} color="var(--ocean)" style={{ flexShrink: 0 }} />
                    <div>
                      <strong>Zero-Risk Payment Protection:</strong>
                      <p style={{ margin: '2px 0 0', fontSize: '0.8rem', color: 'var(--muted)' }}>
                        Your payment will be held securely in Collobster Escrow. It will NOT be transferred to {bookingService.provider} until you review the submitted code, documents, or repair work and approve final completion.
                      </p>
                    </div>
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10, marginTop: 8 }}>
                    <Button variant="quiet" onClick={() => setBookingStep(3)} disabled={isSubmittingBooking}>
                      ← Back
                    </Button>
                    <Button
                      variant="primary"
                      onClick={handleConfirmOrder}
                      disabled={isSubmittingBooking}
                    >
                      {isSubmittingBooking ? 'Securing Escrow...' : `Confirm & Place ₹${finalAmount.toLocaleString()} in Escrow`}
                    </Button>
                  </div>
                </div>
              );
            })()}
          </div>
        </Dialog>
      )}

      {/* ── MODAL 3: PROVIDER "OFFER / LIST A SERVICE" ── */}
      {showListServiceModal && (
        <Dialog
          title="Offer / List a Service on Collobster"
          onClose={() => setShowListServiceModal(false)}
          wide
        >
          <div className="list-service-form">
            <p style={{ margin: 0, fontSize: '0.84rem', color: 'var(--muted)' }}>
              Publish your professional service to the Collobster community. You can offer remote consulting, software architecture, legal drafting, hardware repairs, or on-premises assistance.
            </p>

            <div className="form-two-col">
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                  Service Title:
                </label>
                <input
                  type="text"
                  value={newServiceDraft.title}
                  onChange={(e) => setNewServiceDraft({ ...newServiceDraft, title: e.target.value })}
                  placeholder="e.g. SOC 2 Type II Compliance & Security Audit"
                  style={{ width: '100%', padding: '9px 12px', borderRadius: 6, border: '1px solid var(--line)', background: 'var(--canvas)' }}
                />
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                  Category:
                </label>
                <select
                  value={newServiceDraft.category}
                  onChange={(e) => setNewServiceDraft({ ...newServiceDraft, category: e.target.value })}
                  className="services-filter-select"
                  style={{ width: '100%' }}
                >
                  <option value="software">💻 Software &amp; Cloud</option>
                  <option value="legal">⚖️ Legal &amp; Compliance</option>
                  <option value="repairs">🔧 Repairs &amp; Hardware</option>
                  <option value="finance">📊 Finance &amp; CA</option>
                  <option value="design">🎨 Design &amp; Media</option>
                  <option value="facility">🏢 Facility &amp; Operations</option>
                </select>
              </div>
            </div>

            <div className="form-two-col">
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                  Delivery Mode:
                </label>
                <select
                  value={newServiceDraft.deliveryMode}
                  onChange={(e) => setNewServiceDraft({ ...newServiceDraft, deliveryMode: e.target.value })}
                  className="services-filter-select"
                  style={{ width: '100%' }}
                >
                  <option value="Remote (Global)">🌐 Remote (Global)</option>
                  <option value="On-Premises / On-Site">📍 On-Premises / On-Site</option>
                  <option value="Hybrid">🔄 Hybrid</option>
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                  Pricing Model:
                </label>
                <select
                  value={newServiceDraft.pricingType}
                  onChange={(e) => setNewServiceDraft({ ...newServiceDraft, pricingType: e.target.value })}
                  className="services-filter-select"
                  style={{ width: '100%' }}
                >
                  <option value="Milestone-based Package">Milestone-based Package</option>
                  <option value="Fixed Scope Package">Fixed Scope Package</option>
                  <option value="Hourly Consultation">Hourly Consultation</option>
                </select>
              </div>
            </div>

            <div className="form-two-col">
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                  Base Price (₹):
                </label>
                <input
                  type="number"
                  value={newServiceDraft.price}
                  onChange={(e) => setNewServiceDraft({ ...newServiceDraft, price: e.target.value })}
                  style={{ width: '100%', padding: '9px 12px', borderRadius: 6, border: '1px solid var(--line)', background: 'var(--canvas)' }}
                />
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                  Estimated Turnaround:
                </label>
                <input
                  type="text"
                  value={newServiceDraft.duration}
                  onChange={(e) => setNewServiceDraft({ ...newServiceDraft, duration: e.target.value })}
                  placeholder="e.g. 5 days, 48 hours"
                  style={{ width: '100%', padding: '9px 12px', borderRadius: 6, border: '1px solid var(--line)', background: 'var(--canvas)' }}
                />
              </div>
            </div>

            <div>
              <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                Key Included Deliverables (Comma-separated):
              </label>
              <input
                type="text"
                value={newServiceDraft.highlights}
                onChange={(e) => setNewServiceDraft({ ...newServiceDraft, highlights: e.target.value })}
                placeholder="e.g. Full architectural blueprint, Automated test suite, 30-day bug warranty"
                style={{ width: '100%', padding: '9px 12px', borderRadius: 6, border: '1px solid var(--line)', background: 'var(--canvas)' }}
              />
            </div>

            <div>
              <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                Full Description &amp; Approach:
              </label>
              <textarea
                rows={3}
                value={newServiceDraft.description}
                onChange={(e) => setNewServiceDraft({ ...newServiceDraft, description: e.target.value })}
                placeholder="Explain what problems you solve, your credentials, and how you collaborate with clients..."
                style={{ width: '100%', padding: '9px 12px', borderRadius: 6, border: '1px solid var(--line)', background: 'var(--canvas)' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 8 }}>
              <Button variant="quiet" onClick={() => setShowListServiceModal(false)}>
                Cancel
              </Button>
              <Button variant="primary" onClick={handleCreateNewService}>
                Publish Service Listing
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* ── MODAL 4: CLIENT "POST A CUSTOM REQUEST (RFP)" ── */}
      {showPostRfpModal && (
        <Dialog
          title="Post a Custom Service Request (RFP)"
          onClose={() => setShowPostRfpModal(false)}
        >
          <div className="list-service-form">
            <p style={{ margin: 0, fontSize: '0.84rem', color: 'var(--muted)' }}>
              Describe what you need built, reviewed, or repaired. Verified community practitioners will submit bids with milestones and timelines.
            </p>

            <div>
              <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                Project Title:
              </label>
              <input
                type="text"
                value={newRfpDraft.title}
                onChange={(e) => setNewRfpDraft({ ...newRfpDraft, title: e.target.value })}
                placeholder="e.g. Legal Opinion on Cross-Border Data Transfer"
                style={{ width: '100%', padding: '9px 12px', borderRadius: 6, border: '1px solid var(--line)', background: 'var(--canvas)' }}
              />
            </div>

            <div className="form-two-col">
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                  Category:
                </label>
                <select
                  value={newRfpDraft.category}
                  onChange={(e) => setNewRfpDraft({ ...newRfpDraft, category: e.target.value })}
                  className="services-filter-select"
                  style={{ width: '100%' }}
                >
                  <option value="legal">⚖️ Legal &amp; Regulatory</option>
                  <option value="software">💻 Software &amp; Tech</option>
                  <option value="repairs">🔧 Repairs &amp; Hardware</option>
                  <option value="finance">📊 Finance &amp; Tax</option>
                  <option value="design">🎨 Design &amp; Creative</option>
                  <option value="facility">🏢 Facility &amp; On-Site</option>
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                  Estimated Budget Range:
                </label>
                <input
                  type="text"
                  value={newRfpDraft.budget}
                  onChange={(e) => setNewRfpDraft({ ...newRfpDraft, budget: e.target.value })}
                  placeholder="e.g. ₹8,000 – ₹15,000"
                  style={{ width: '100%', padding: '9px 12px', borderRadius: 6, border: '1px solid var(--line)', background: 'var(--canvas)' }}
                />
              </div>
            </div>

            <div className="form-two-col">
              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                  Deadline:
                </label>
                <input
                  type="text"
                  value={newRfpDraft.deadline}
                  onChange={(e) => setNewRfpDraft({ ...newRfpDraft, deadline: e.target.value })}
                  placeholder="e.g. Within 3 days"
                  style={{ width: '100%', padding: '9px 12px', borderRadius: 6, border: '1px solid var(--line)', background: 'var(--canvas)' }}
                />
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                  Delivery Mode:
                </label>
                <select
                  value={newRfpDraft.deliveryMode}
                  onChange={(e) => setNewRfpDraft({ ...newRfpDraft, deliveryMode: e.target.value })}
                  className="services-filter-select"
                  style={{ width: '100%' }}
                >
                  <option value="Remote">🌐 Remote</option>
                  <option value="On-Site">📍 On-Site</option>
                </select>
              </div>
            </div>

            <div>
              <label style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--ink)', display: 'block', marginBottom: 4 }}>
                Detailed Project Description:
              </label>
              <textarea
                rows={3}
                value={newRfpDraft.description}
                onChange={(e) => setNewRfpDraft({ ...newRfpDraft, description: e.target.value })}
                placeholder="Give context about your business, the current blockers, and what exact outcome you expect..."
                style={{ width: '100%', padding: '9px 12px', borderRadius: 6, border: '1px solid var(--line)', background: 'var(--canvas)' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 8 }}>
              <Button variant="quiet" onClick={() => setShowPostRfpModal(false)}>
                Cancel
              </Button>
              <Button variant="primary" onClick={handlePostRfp}>
                Post Request to Community
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* ── MODAL 5: REQUEST REVISION MODAL ── */}
      {reviewDeliverablesOrder && (
        <Dialog
          title={`Request Revision: ${reviewDeliverablesOrder.orderId}`}
          onClose={() => setReviewDeliverablesOrder(null)}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <p style={{ margin: 0, fontSize: '0.84rem', color: 'var(--muted)' }}>
              Specify the exact adjustments or additions needed before milestone approval. {reviewDeliverablesOrder.provider} will address them.
            </p>

            <textarea
              rows={4}
              value={revisionNotes}
              onChange={(e) => setRevisionNotes(e.target.value)}
              placeholder="e.g. Please clarify clause 4.2 in the shareholder agreement, and add unit test coverage for the payment webhook handler..."
              style={{ width: '100%', padding: '10px 12px', borderRadius: 6, border: '1px solid var(--line)', background: 'var(--canvas)', fontSize: '0.86rem' }}
            />

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
              <Button variant="quiet" onClick={() => setReviewDeliverablesOrder(null)}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={() => {
                  if (!revisionNotes.trim()) {
                    a.toast('Please write your revision notes');
                    return;
                  }
                  handleAdvanceOrderStatus(reviewDeliverablesOrder.orderId || reviewDeliverablesOrder.id, 'Revision in Progress');
                  setReviewDeliverablesOrder(null);
                  a.toast(`Revision request sent to ${reviewDeliverablesOrder.provider}`);
                }}
              >
                Send Revision Feedback
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* ── MODAL 6: RATE & REVIEW MODAL ── */}
      {ratingModalOrder && (
        <Dialog
          title={`Rate & Review: ${ratingModalOrder.provider}`}
          onClose={() => setRatingModalOrder(null)}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14, textAlign: 'center', padding: '10px 0' }}>
            <img
              src={ratingModalOrder.providerAvatar}
              alt={ratingModalOrder.provider}
              style={{ width: 64, height: 64, borderRadius: '50%', margin: '0 auto', objectFit: 'cover' }}
            />
            <div>
              <strong style={{ fontSize: '1.1rem', color: 'var(--ink)' }}>{ratingModalOrder.provider}</strong>
              <div style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>{ratingModalOrder.title}</div>
            </div>

            <div>
              <span style={{ fontSize: '0.84rem', color: 'var(--muted)', display: 'block', marginBottom: 6 }}>
                How was your experience?
              </span>
              <div style={{ display: 'flex', justifyContent: 'center', gap: 8 }}>
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setRatingStars(star)}
                    style={{ border: 0, background: 'none', cursor: 'pointer', padding: 4 }}
                  >
                    <Star
                      size={28}
                      fill={ratingStars >= star ? 'var(--gold)' : 'none'}
                      color={ratingStars >= star ? 'var(--gold)' : 'var(--line)'}
                    />
                  </button>
                ))}
              </div>
            </div>

            <textarea
              rows={3}
              value={ratingReviewText}
              onChange={(e) => setRatingReviewText(e.target.value)}
              placeholder="Write a brief review on the speed, professionalism, and deliverable quality..."
              style={{ width: '100%', padding: '10px 12px', borderRadius: 6, border: '1px solid var(--line)', background: 'var(--canvas)', fontSize: '0.86rem', textAlign: 'left' }}
            />

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 6 }}>
              <Button variant="quiet" onClick={() => setRatingModalOrder(null)}>
                Skip
              </Button>
              <Button variant="primary" onClick={handleSubmitReview}>
                Submit Review ({ratingStars}★)
              </Button>
            </div>
          </div>
        </Dialog>
      )}
    </div>
  );
}

// ─── 8. Events Space (Exported) ─────────────────────────────────────────────

export function Events() {
  const a = useApp();
  const [mode, setMode] = useState('Attend');
  const [view, setView] = useState(a.eventsInitialView || 'Discover');
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    if (a.eventsInitialView) {
      setView(a.eventsInitialView);
    }
  }, [a.eventsInitialView]);

  const catalog = [
    ...(a.eventCatalog || []),
    ...events.filter(
      (event) => !(a.eventCatalog || []).some((remote) => remote.id === event.id)
    ),
  ];

  return (
    <main className="operator-space">
      <PageHeader
        eyebrow="EVENTS"
        title={
          mode === 'Attend'
            ? 'Meet where ideas turn into momentum.'
            : 'Run events people are glad to attend.'
        }
        description={
          mode === 'Attend'
            ? 'Discover, register, save tickets, and keep your calendar useful.'
            : 'Create, publish, promote, and follow attendance from one event workspace.'
        }
      >
        <Tabs
          items={['Attend', 'Organize']}
          value={mode}
          onChange={setMode}
        />
      </PageHeader>

      {mode === 'Attend' ? (
        <AttendeeEvents
          app={a}
          events={catalog}
          view={view}
          setView={setView}
          selected={selected}
          setSelected={setSelected}
        />
      ) : (
        <OrganizerEvents app={a} />
      )}
    </main>
  );
}

function AttendeeEvents({
  app,
  events: catalog,
  view,
  setView,
  selected,
  setSelected,
}) {
  const [filter, setFilter] = useState('All');
  const [query, setQuery] = useState('');

  const eventCategories = [
    { id: 'All', label: 'All Events', icon: Globe2 },
    { id: 'Hackathon', label: 'Hackathons', icon: Code2 },
    { id: 'Conference', label: 'Conferences', icon: Building2 },
    { id: 'Webinar', label: 'Webinars', icon: Video },
    { id: 'Workshop', label: 'Workshops', icon: BookOpen },
    { id: 'Meetup', label: 'Meetups', icon: Users },
  ];

  const visible = catalog.filter((e) => {
    const f = filter.toLowerCase();
    const eCat = (e.category || '').toLowerCase();
    const matchesCategory =
      filter === 'All' ||
      (filter === 'Online'
        ? e.mode?.toLowerCase() === 'online'
        : eCat === f || eCat.startsWith(f.slice(0, 4)) || f.startsWith(eCat.slice(0, 4)));
    const matchesQuery = `${e.title} ${e.category} ${e.location} ${e.description || ''} ${e.organizer || ''}`
      .toLowerCase()
      .includes(query.toLowerCase());
    return matchesCategory && matchesQuery;
  });

  const recommendedEvents = catalog.filter(
    (e) => e.recommended || ['e-hack', 'e-conf', 'e-webinar', 'e1'].includes(e.id)
  );

  if (selected) {
    return (
      <EventDetail
        app={app}
        event={selected}
        onBack={() => setSelected(null)}
      />
    );
  }

  return (
    <div className="operator-layout">
      <section>
        <Tabs
          items={['Discover', 'My tickets', 'Calendar', 'Saved']}
          value={view === 'Registered' ? 'My tickets' : view}
          onChange={(v) => {
            const resolved = v === 'My tickets' ? 'Registered' : v;
            setView(resolved);
            if (app.setEventsInitialView) app.setEventsInitialView(resolved);
          }}
        />

        {view === 'Discover' && (
          <>
            <div className="event-attend-hero">
              <div>
                <Tag tone="coral">Gatherings &amp; Hackathons</Tag>
                <h2>Find a room for the work you want to do next.</h2>
                <p>
                  Conferences, global hackathons, live webinars, and intimate
                  workshops to accelerate your craft.
                </p>
                <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
                  <Button onClick={() => setView('Calendar')}>
                    View calendar
                  </Button>
                  <Button
                    variant="ghost"
                    onClick={() => {
                      setView('Discover');
                      setFilter('Hackathon');
                      setQuery('');
                      setSelected(null);
                    }}
                  >
                    Browse hackathons
                  </Button>
                </div>
              </div>
              <div className="event-count">
                <strong>{catalog.length}</strong>
                <span>active gatherings</span>
              </div>
            </div>

            {/* Search bar positioned directly at top */}
            <div className="space-controls" style={{ margin: '14px 0 10px' }}>
              <SearchField
                value={query}
                onChange={setQuery}
                placeholder="Search events, hackathons, topics, or organizers"
              />
            </div>

            {/* Category tabs with icons */}
            <div className="event-category-pills" style={{ marginBottom: 16 }}>
              {eventCategories.map((cat) => {
                const CatIcon = cat.icon;
                return (
                  <button
                    key={cat.id}
                    className={`event-cat-btn ${
                      filter === cat.id ? 'active' : ''
                    }`}
                    onClick={() => {
                      setFilter(cat.id);
                    }}
                  >
                    <CatIcon size={16} />
                    <span>{cat.label}</span>
                  </button>
                );
              })}
            </div>

            {/* Recommended section on main page */}
            {recommendedEvents.length > 0 && !query && filter === 'All' && (
              <div className="events-recommended-section">
                <div className="events-recommended-title">
                  <Sparkles size={18} style={{ color: 'var(--coral)' }} />
                  <span>Recommended for you</span>
                </div>
                <div className="events-recommended-grid">
                  {recommendedEvents.map((e) => (
                    <article className="rec-event-card" key={e.id}>
                      <div className="rec-card-hero">
                        <span className="rec-card-badge">
                          <Award size={13} /> {e.category}
                        </span>
                        <button
                          type="button"
                          className="icon-action-btn"
                          style={{
                            background: 'rgba(0,0,0,0.3)',
                            border: 0,
                            borderRadius: '50%',
                            color: '#fff',
                            padding: 6,
                            cursor: 'pointer',
                          }}
                          onClick={(evt) => {
                            evt.stopPropagation();
                            app.toggleSaveEvent(e.id);
                          }}
                          title={
                            app.savedEvents?.includes(e.id)
                              ? 'Saved'
                              : 'Save event'
                          }
                        >
                          <Bookmark
                            size={15}
                            fill={
                              app.savedEvents?.includes(e.id) ? '#fff' : 'none'
                            }
                          />
                        </button>
                      </div>
                      <div className="rec-card-body">
                        <h4>{e.title}</h4>
                        <p>{e.description}</p>
                        <div className="rec-card-meta">
                          <span>📅 {e.date || `${e.month} ${e.day}`}</span>
                          <span>📍 {e.location || e.mode}</span>
                        </div>
                        <div className="rec-card-actions">
                          <Button size="sm" onClick={() => setSelected(e)}>
                            View details
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => {
                              if (app.registrations?.includes(e.id)) {
                                app.toast(
                                  'Already registered! Pass available in Registered.'
                                );
                              } else {
                                app.setModal({
                                  type: 'details',
                                  kind: 'event',
                                  item: e,
                                });
                              }
                            }}
                          >
                            {app.registrations?.includes(e.id)
                              ? 'Registered'
                              : 'Register'}
                          </Button>
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
              </div>
            )}

            <div className="events-recommended-grid" style={{ marginTop: 14 }}>
              {visible.map((e) => (
                <article className="rec-event-card" key={e.id}>
                  <div
                    className="rec-card-hero"
                    style={e.image ? {
                      backgroundImage: `linear-gradient(rgba(0,0,0,0.35), rgba(0,0,0,0.65)), url(${e.image})`,
                      backgroundSize: 'cover',
                      backgroundPosition: 'center',
                    } : undefined}
                  >
                    <span className="rec-card-badge">
                      <Award size={13} /> {e.category}
                    </span>
                    <button
                      type="button"
                      className="icon-action-btn"
                      style={{
                        background: 'rgba(0,0,0,0.4)',
                        border: 0,
                        borderRadius: '50%',
                        color: '#fff',
                        padding: 6,
                        cursor: 'pointer',
                      }}
                      onClick={(evt) => {
                        evt.stopPropagation();
                        app.toggleSaveEvent(e.id);
                      }}
                      title={app.savedEvents?.includes(e.id) ? 'Saved' : 'Save event'}
                    >
                      <Bookmark
                        size={15}
                        fill={app.savedEvents?.includes(e.id) ? '#fff' : 'none'}
                      />
                    </button>
                  </div>
                  <div className="rec-card-body">
                    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', alignItems: 'center' }}>
                      <Tag tone={e.mode === 'Online' ? 'purple' : 'green'}>{e.mode || 'In person'}</Tag>
                      {e.prizePool && <Tag tone="gold">{e.prizePool}</Tag>}
                      {(e.isCancelled || e.status === 'Cancelled') && (
                        <span style={{ fontSize: '0.74rem', background: '#fee2e2', color: '#b91c1c', fontWeight: 700, padding: '2px 8px', borderRadius: 12 }}>
                          Cancelled
                        </span>
                      )}
                    </div>
                    <h4>{e.title}</h4>
                    <p>{e.description}</p>
                    <div className="rec-card-meta">
                      <span>📅 {e.date || `${e.month || 'OCT'} ${e.day || '15'}`}</span>
                      <span>📍 {e.location || e.mode}</span>
                      <span>👥 {e.attendees || 0} attending</span>
                    </div>
                    <div className="rec-card-actions">
                      <Button size="sm" onClick={() => setSelected(e)}>
                        View details
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => {
                          if (app.registrations?.includes(e.id)) {
                            app.toast('Already registered! Pass available in My tickets.');
                          } else {
                            app.setModal({
                              type: 'details',
                              kind: 'event',
                              item: e,
                            });
                          }
                        }}
                      >
                        {app.registrations?.includes(e.id) ? 'Registered' : 'Register'}
                      </Button>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          </>
        )}

        {(view === 'Registered' || view === 'My tickets') && (
          <RegisteredEventsList
            app={app}
            events={catalog}
            setSelected={setSelected}
          />
        )}
        {view === 'Calendar' && (
          <EventCalendar app={app} events={catalog} setSelected={setSelected} />
        )}
        {view === 'Saved' && (
          (() => {
            const savedList = catalog.filter((e) =>
              app.savedEvents?.includes(e.id)
            );
            return savedList.length ? (
              <div className="event-list">
                {savedList.map((e) => (
                  <button
                    className="event-row"
                    key={e.id}
                    onClick={() => setSelected(e)}
                  >
                    <span className="date-tile">
                      <small>{e.month}</small>
                      <b>{e.day}</b>
                    </span>
                    <span>
                      <Tag tone="coral">{e.category}</Tag>
                      <strong>{e.title}</strong>
                      <p>
                        {e.time} · {e.location}
                      </p>
                    </span>
                    <ArrowRight size={19} />
                  </button>
                ))}
              </div>
            ) : (
              <Empty
                title="No saved events yet"
                text="Save an event from its details so it is easy to return to."
                action="Browse events"
                onAction={() => setView('Discover')}
              />
            );
          })()
        )}
      </section>

      <aside className="space-rail">
        <SectionTitle title="Upcoming for you" />
        {catalog.slice(0, 4).map((e) => (
          <button
            className="mini-event"
            key={e.id}
            onClick={() => setSelected(e)}
          >
            <span className="date-tile">
              <small>{e.month || 'OCT'}</small>
              <b>{e.day || '15'}</b>
            </span>
            <span>
              <b>{e.title}</b>
              <small>{e.time} · {e.mode || e.location}</small>
            </span>
          </button>
        ))}

        <SectionTitle title="Ways to gather" />
        <div className="tag-cloud" style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {[
            { label: 'Global Hackathons', cat: 'Hackathon' },
            { label: 'Tech Conferences', cat: 'Conference' },
            { label: 'Live Webinars', cat: 'Webinar' },
            { label: 'Workshops', cat: 'Workshop' },
            { label: 'Peer Meetups', cat: 'Meetup' },
          ].map((item) => (
            <button
              key={item.cat}
              type="button"
              className={`tag-btn ${filter === item.cat ? 'active' : ''}`}
              style={{
                border: filter === item.cat ? '1.5px solid var(--accent, #e34d42)' : '1px solid var(--color-border, #e5e7eb)',
                background: filter === item.cat ? 'var(--accent-tint, rgba(227, 77, 66, 0.1))' : 'var(--color-surface, #fff)',
                color: filter === item.cat ? 'var(--accent, #e34d42)' : 'inherit',
                fontWeight: filter === item.cat ? 600 : 500,
                borderRadius: 14,
                padding: '4px 10px',
                fontSize: '0.8rem',
                cursor: 'pointer',
              }}
              onClick={() => {
                setView('Discover');
                setFilter(item.cat);
                setQuery('');
                setSelected(null);
              }}
            >
              {item.label}
            </button>
          ))}
        </div>
      </aside>
    </div>
  );
}

function EventDetail({ app, event, onBack }) {
  const registered = app.registrations.includes(event.id);
  const isSaved = app.savedEvents?.includes(event.id);
  const isCancelled = event.isCancelled || event.status === 'Cancelled';

  return (
    <section className="event-detail-page">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <button className="back-link" onClick={onBack}>
          ← All events
        </button>
        <Button
          variant="ghost"
          icon={Bookmark}
          onClick={() => {
            app.toggleSaveEvent(event.id);
          }}
        >
          {isSaved ? 'Saved ★' : 'Save event'}
        </Button>
      </div>

      {isCancelled && (
        <div style={{ padding: '14px 18px', background: '#fef2f2', border: '1.5px solid #f87171', borderRadius: 10, color: '#991b1b', margin: '14px 0', display: 'flex', alignItems: 'center', gap: 12 }}>
          <X size={24} color="#dc2626" style={{ flexShrink: 0 }} />
          <div>
            <strong style={{ fontSize: '0.98rem', display: 'block' }}>This event has been CANCELLED by the organizer</strong>
            <p style={{ margin: '3px 0 0', fontSize: '0.84rem' }}>{event.cancelReason || 'Registration and access have been suspended. Any paid fees will be refunded.'}</p>
          </div>
        </div>
      )}

      <div className="event-detail-hero">
        <Media src={event.image || photos[0]} alt="Event" />
        <div>
          <div style={{ display: 'flex', gap: 6, alignItems: 'center', marginBottom: 6 }}>
            <Tag tone="coral">{event.category}</Tag>
            <Tag tone="purple">{event.mode}</Tag>
            {event.prizePool && <Tag tone="gold">{event.prizePool}</Tag>}
            {isCancelled && (
              <span style={{ fontSize: '0.74rem', background: '#fee2e2', color: '#b91c1c', fontWeight: 700, padding: '2px 8px', borderRadius: 12 }}>
                Cancelled
              </span>
            )}
          </div>
          <h2>{event.title}</h2>
          <p>{event.description}</p>
          <small>{event.attendees || 0} people are registered and attending</small>
        </div>
      </div>

      <div className="metric-grid">
        <Metric value={event.date || `${event.month || 'OCT'} ${event.day || '15'}`} label="Date" />
        <Metric value={event.time || '10:00 AM'} label="Time" />
        <Metric value={event.mode || 'In person'} label="Format" />
        <Metric value={event.location || 'Online'} label="Location" />
      </div>

      <div className="operator-columns">
        <section>
          <h3>About this event</h3>
          <p>
            {event.description} Join fellow builders, learn from industry leaders,
            and interact directly during live breakout rooms and Q&amp;A sessions.
          </p>

          {/* Rounds & Timeline for Live Events / Hackathons */}
          {event.rounds && Array.isArray(event.rounds) && event.rounds.length > 0 && (
            <section className="event-rounds-section" style={{ marginTop: 18 }}>
              <h3>Rounds &amp; Schedule</h3>
              <div className="round-timeline">
                {event.rounds.map((rnd, rIdx) => (
                  <div className="round-item" key={rIdx}>
                    <div className="round-item-header">
                      <b>{rnd.name}</b>
                      <span
                        className={`round-status-badge ${
                          rnd.status?.toLowerCase().replace(/\s+/g, '') ||
                          'upcoming'
                        }`}
                      >
                        {rnd.status || 'Upcoming'}
                      </span>
                    </div>
                    <p style={{ margin: '2px 0 4px', fontSize: 11, color: 'var(--muted)' }}>
                      <strong>Timeline:</strong> {rnd.date || rnd.time || 'Schedule announced soon'}
                    </p>
                    <p>{rnd.description || rnd.desc}</p>
                    {rnd.deliverable && (
                      <div className="round-deliverables">
                        📦 Deliverable: {rnd.deliverable}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* Prize Pool Breakdown (Safe for Array or String) */}
          {event.prizes && (
            <section style={{ marginTop: 18 }}>
              <h3>Prize Pool &amp; Recognition</h3>
              {Array.isArray(event.prizes) ? (
                <div className="prize-pool-grid">
                  {event.prizes.map((pz, pIdx) => (
                    <div className="prize-card" key={pIdx}>
                      <span className="prize-rank">{pz.rank}</span>
                      <div className="prize-amount">{pz.reward}</div>
                      <div className="prize-desc">{pz.title}</div>
                    </div>
                  ))}
                </div>
              ) : (
                <div
                  style={{
                    background: 'linear-gradient(135deg, rgba(239, 68, 68, 0.08), rgba(245, 158, 11, 0.08))',
                    border: '1px solid rgba(245, 158, 11, 0.3)',
                    padding: '14px 18px',
                    borderRadius: 10,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 12,
                  }}
                >
                  <Award size={26} color="#f59e0b" style={{ flexShrink: 0 }} />
                  <div>
                    <strong style={{ fontSize: '0.94rem', color: 'var(--color-heading, #111)', display: 'block' }}>
                      Rewards &amp; Recognition
                    </strong>
                    <span style={{ fontSize: '0.86rem', color: 'var(--color-muted, #555)' }}>
                      {event.prizes}
                    </span>
                  </div>
                </div>
              )}
            </section>
          )}

          {/* Rules & Guidelines (Safe for Array or String) */}
          {event.rules && (
            <section style={{ marginTop: 18 }}>
              <h3>Rules &amp; Guidelines</h3>
              <ul
                style={{
                  paddingLeft: 18,
                  fontSize: 13,
                  color: 'var(--muted)',
                  lineHeight: 1.6,
                }}
              >
                {Array.isArray(event.rules) ? (
                  event.rules.map((rl, idx) => (
                    <li key={idx}>{rl}</li>
                  ))
                ) : (
                  <li>{event.rules}</li>
                )}
              </ul>
            </section>
          )}

          {/* Custom Sections added by Organizers */}
          {event.customSections && Array.isArray(event.customSections) && event.customSections.map((sec, idx) => (
            <section key={idx} style={{ marginTop: 18 }}>
              <h3>{sec.title}</h3>
              <div style={{ fontSize: 13, color: 'var(--muted)', lineHeight: 1.6, whiteSpace: 'pre-line' }}>
                {sec.content}
              </div>
            </section>
          ))}

          <h3 style={{ marginTop: 20 }}>Speakers &amp; Jury</h3>
          <div className="person-list">
            <article>
              <Avatar person={people[1]} />
              <div>
                <b>Aman Verma</b>
                <small>Lead Architect · Orbit Systems</small>
              </div>
            </article>
            <article>
              <Avatar person={people[0]} />
              <div>
                <b>Priya Sundaram</b>
                <small>Staff Engineer · AI Agents Collective</small>
              </div>
            </article>
          </div>
        </section>

        <section className="inset-card">
          <h3>Your registration</h3>
          {isCancelled ? (
            <>
              <p style={{ color: '#b91c1c' }}>
                This gathering was cancelled. Registration is currently closed.
              </p>
              <Button disabled style={{ opacity: 0.6, cursor: 'not-allowed' }}>
                Event Cancelled
              </Button>
            </>
          ) : (
            <>
              <p>
                {registered
                  ? 'You are confirmed for this event. Your verified entry pass is ready in Registered.'
                  : 'Registration is open for individuals and teams.'}
              </p>
              <Button
                onClick={() =>
                  registered
                    ? app.toast(
                        'Your ticket pass is already available in Registered.'
                      )
                    : app.setModal({
                        type: 'details',
                        kind: 'event',
                        item: event,
                      })
                }
              >
                {registered ? 'Registered ✓' : 'Register now (Individual or Team)'}
              </Button>
              <Button
                variant="ghost"
                onClick={() => {
                  app.toast('Event link copied to clipboard');
                }}
              >
                Share event link
              </Button>
            </>
          )}
        </section>
      </div>
    </section>
  );
}

function RegisteredEventsList({ app, events: catalog, setSelected }) {
  const [activeTicketPass, setActiveTicketPass] = useState(null);
  const [categoryFilter, setCategoryFilter] = useState('All Events');
  const [subTab, setSubTab] = useState('Upcoming'); // 'Upcoming' | 'Completed'
  const [query, setQuery] = useState('');

  useEffect(() => {
    if (app.activeTicketEventId) {
      const match = catalog.find((e) => e.id === app.activeTicketEventId);
      if (match) {
        setActiveTicketPass({
          event: match,
          seat: 'Confirmed Slot: Section A · Front Row',
        });
      }
      if (app.setActiveTicketEventId) app.setActiveTicketEventId(null);
    }
  }, [app.activeTicketEventId, catalog]);

  const registeredAll = catalog.filter((e) => app.registrations.includes(e.id));

  // Determine if event is in the past (completed) or upcoming
  // Current virtual date is late September 2026
  const isPastEvent = (e) => {
    if (e.date) {
      const d = new Date(e.date);
      if (!isNaN(d.getTime())) {
        return d < new Date('2026-09-24T00:00:00');
      }
    }
    return e.month === 'SEP' && Number(e.day) < 24;
  };

  const categories = [
    { id: 'All Events', label: 'All Events', icon: Globe2 },
    { id: 'Hackathons', label: 'Hackathons', icon: Code2 },
    { id: 'Conferences', label: 'Conferences', icon: Building2 },
    { id: 'Webinars', label: 'Webinars', icon: Video },
    { id: 'Workshops', label: 'Workshops', icon: BookOpen },
    { id: 'Meetups', label: 'Meetups', icon: Users },
  ];

  const filtered = registeredAll.filter((e) => {
    // Sub-tab filter
    const past = isPastEvent(e);
    if (subTab === 'Upcoming' && past) return false;
    if (subTab === 'Completed' && !past) return false;

    // Category filter
    if (categoryFilter !== 'All Events') {
      const target = categoryFilter.toLowerCase().slice(0, 4);
      const eCat = (e.category || '').toLowerCase();
      if (!eCat.startsWith(target)) return false;
    }

    // Search query
    if (query.trim()) {
      const q = query.toLowerCase();
      const match = `${e.title} ${e.category} ${e.location} ${e.description || ''}`.toLowerCase();
      if (!match.includes(q)) return false;
    }
    return true;
  });

  const upcomingCount = registeredAll.filter((e) => !isPastEvent(e)).length;
  const completedCount = registeredAll.filter((e) => isPastEvent(e)).length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* Search & Category Filter Pills */}
      <div className="space-controls" style={{ margin: '8px 0 0' }}>
        <SearchField
          value={query}
          onChange={setQuery}
          placeholder="Filter registered events by title, topic, or location..."
        />
      </div>

      {/* Category Pills */}
      <div className="event-category-pills" style={{ marginBottom: 4 }}>
        {categories.map((cat) => {
          const CatIcon = cat.icon;
          const count = registeredAll.filter((e) => {
            if (cat.id === 'All Events') return true;
            return (e.category || '').toLowerCase().startsWith(cat.id.toLowerCase().slice(0, 4));
          }).length;

          return (
            <button
              key={cat.id}
              className={`event-cat-btn ${categoryFilter === cat.id ? 'active' : ''}`}
              onClick={() => setCategoryFilter(cat.id)}
            >
              <CatIcon size={16} />
              <span>{cat.label} ({count})</span>
            </button>
          );
        })}
      </div>

      {/* Sub-tabs: Upcoming vs Completed */}
      <div style={{ display: 'flex', gap: 10, borderBottom: '1px solid var(--color-border, #e5e7eb)', paddingBottom: 8 }}>
        <button
          type="button"
          onClick={() => setSubTab('Upcoming')}
          style={{
            background: 'none',
            border: 'none',
            padding: '6px 14px',
            borderRadius: 6,
            fontWeight: 700,
            fontSize: '0.92rem',
            cursor: 'pointer',
            color: subTab === 'Upcoming' ? 'var(--accent, #e34d42)' : 'var(--color-muted, #666)',
            borderBottom: subTab === 'Upcoming' ? '2.5px solid var(--accent, #e34d42)' : '2.5px solid transparent',
          }}
        >
          Upcoming ({upcomingCount})
        </button>
        <button
          type="button"
          onClick={() => setSubTab('Completed')}
          style={{
            background: 'none',
            border: 'none',
            padding: '6px 14px',
            borderRadius: 6,
            fontWeight: 700,
            fontSize: '0.92rem',
            cursor: 'pointer',
            color: subTab === 'Completed' ? 'var(--accent, #e34d42)' : 'var(--color-muted, #666)',
            borderBottom: subTab === 'Completed' ? '2.5px solid var(--accent, #e34d42)' : '2.5px solid transparent',
          }}
        >
          Completed ({completedCount})
        </button>
      </div>

      {filtered.length > 0 ? (
        <div className="event-list">
          {filtered.map((e, idx) => {
            const isCancelled = e.isCancelled || e.status === 'Cancelled';
            const past = isPastEvent(e);

            return (
              <div className="event-row" key={e.id} style={{ cursor: 'default' }}>
                <span className="date-tile">
                  <small>{e.month || 'OCT'}</small>
                  <b>{e.day || '15'}</b>
                </span>
                <span style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', gap: 6, alignItems: 'center', marginBottom: 4 }}>
                    <Tag tone="purple">Entry Pass</Tag>
                    {isCancelled ? (
                      <span style={{ fontSize: '0.74rem', background: '#fee2e2', color: '#b91c1c', fontWeight: 700, padding: '2px 8px', borderRadius: 12 }}>
                        Cancelled
                      </span>
                    ) : past ? (
                      <span style={{ fontSize: '0.74rem', background: '#ecfdf5', color: '#065f46', fontWeight: 600, padding: '2px 8px', borderRadius: 12, display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                        <CheckCircle2 size={12} color="#059669" /> Attended / Completed
                      </span>
                    ) : (
                      <span className="slot-confirmed-pill">
                        <CheckCircle2 size={12} /> Confirmed Slot #{100 + idx * 7} · Yet to attend
                      </span>
                    )}
                  </div>
                  <strong style={{ fontSize: 15 }}>{e.title}</strong>
                  <p>{e.time} · {e.location}</p>
                  <small style={{ color: 'var(--muted)' }}>
                    Format: {e.mode || 'In person'} · Seat Desk #{idx + 1}
                  </small>
                </span>
                <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                  {!isCancelled && (
                    <Button
                      variant="primary"
                      size="sm"
                      icon={QrCode}
                      onClick={() =>
                        setActiveTicketPass({
                          event: e,
                          seat: `Row ${String.fromCharCode(65 + (idx % 5))} · #${100 + idx * 7}`,
                        })
                      }
                    >
                      View QR Pass
                    </Button>
                  )}
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setSelected(e)}
                  >
                    Details
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <Empty
          title={
            subTab === 'Upcoming'
              ? 'No upcoming gatherings found'
              : 'No completed gatherings yet'
          }
          text={
            subTab === 'Upcoming'
              ? 'Explore and register for hackathons, conferences, and meetups in Discover.'
              : 'Past events you attended will appear here once their schedule concludes.'
          }
        />
      )}

      {/* QR Ticket Modal */}
      {activeTicketPass && (
        <div className="call-modal-overlay" onClick={() => setActiveTicketPass(null)}>
          <div className="ticket-pass-modal" onClick={(e) => e.stopPropagation()}>
            <div className="ticket-pass-header">
              <span style={{ fontSize: 11, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Collobster Verified Entry Pass
              </span>
              <h3 style={{ margin: '6px 0 2px', fontSize: 18 }}>
                {activeTicketPass.event.title}
              </h3>
              <small>
                {activeTicketPass.event.date || `${activeTicketPass.event.month} ${activeTicketPass.event.day}`} · {activeTicketPass.event.time}
              </small>
            </div>
            <div className="ticket-pass-body">
              <div className="slot-confirmed-pill">
                <CheckCircle2 size={14} /> Confirmed Slot: {activeTicketPass.seat}
              </div>
              <div className="ticket-qr-frame">
                <QrCode size={130} style={{ color: '#0f172a' }} />
              </div>
              <div style={{ textAlign: 'center' }}>
                <b style={{ fontSize: 13, letterSpacing: 1.2 }}>
                  PASS-{activeTicketPass.event.id.toUpperCase()}-{activeTicketPass.seat.replace(/[^a-zA-Z0-9]/g, '')}
                </b>
                <p style={{ margin: '4px 0 0', fontSize: 12, color: 'var(--muted)' }}>
                  Holder: {app.profile.name} · Scan at entrance check-in desk
                </p>
              </div>
              <div style={{ display: 'flex', gap: 8, width: '100%', marginTop: 8 }}>
                <Button
                  style={{ flex: 1 }}
                  onClick={() => {
                    downloadTicketPdf({
                      eventTitle: activeTicketPass.event.title,
                      attendeeName: app.profile.name,
                      ticketCode: `PASS-${activeTicketPass.event.id.toUpperCase()}-${activeTicketPass.seat.replace(/[^a-zA-Z0-9]/g, '')}`,
                      slot: activeTicketPass.seat,
                      date: activeTicketPass.event.date || `${activeTicketPass.event.month} ${activeTicketPass.event.day}`,
                      location: activeTicketPass.event.location || 'Bengaluru',
                    });
                    app.toast('Ticket pass downloaded as official PDF!');
                    setActiveTicketPass(null);
                  }}
                >
                  Download pass
                </Button>
                <Button variant="ghost" onClick={() => setActiveTicketPass(null)}>
                  Close
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function EventCalendar({ app, events: catalog, setSelected }) {
  // Calendar base state: default to October 2026 (or September)
  const [currentYear, setCurrentYear] = useState(2026);
  const [currentMonth, setCurrentMonth] = useState(9); // 0-indexed: 9 = October, 8 = September
  const [selectedDay, setSelectedDay] = useState(26);

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December',
  ];
  const monthShorts = [
    'JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN',
    'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC',
  ];

  const handlePrevMonth = () => {
    if (currentMonth === 0) {
      setCurrentMonth(11);
      setCurrentYear(currentYear - 1);
    } else {
      setCurrentMonth(currentMonth - 1);
    }
  };

  const handleNextMonth = () => {
    if (currentMonth === 11) {
      setCurrentMonth(0);
      setCurrentYear(currentYear + 1);
    } else {
      setCurrentMonth(currentMonth + 1);
    }
  };

  const handleToday = () => {
    setCurrentYear(2026);
    setCurrentMonth(9); // October
    setSelectedDay(26);
  };

  // Compute days in month and start day
  const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();
  const startDayOfWeek = new Date(currentYear, currentMonth, 1).getDay(); // 0 = Sun
  const curMonthShort = monthShorts[currentMonth];

  // Helper to match events to a day
  const getEventsForDay = (day) => {
    return catalog.filter((e) => {
      if (e.date) {
        const parts = e.date.split('-');
        if (parts.length === 3) {
          const y = Number(parts[0]);
          const m = Number(parts[1]) - 1;
          const d = Number(parts[2]);
          return y === currentYear && m === currentMonth && d === day;
        }
      }
      return Number(e.day) === day && (e.month || '').toUpperCase() === curMonthShort;
    });
  };

  const selectedDayEvents = getEventsForDay(selectedDay);

  return (
    <section className="calendar-panel" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      {/* Month Navigation Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '4px 8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <h2 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 700 }}>
            {monthNames[currentMonth]} {currentYear}
          </h2>
          <Button variant="ghost" size="sm" onClick={handleToday} style={{ fontSize: '0.8rem' }}>
            Today
          </Button>
        </div>
        <div style={{ display: 'flex', gap: 6 }}>
          <button
            type="button"
            onClick={handlePrevMonth}
            style={{
              padding: '6px 12px',
              borderRadius: 6,
              border: '1px solid var(--color-border, #ddd)',
              background: 'var(--color-surface, #fff)',
              cursor: 'pointer',
              display: 'inline-flex',
              alignItems: 'center',
            }}
            title="Previous month"
          >
            <ChevronLeft size={16} />
          </button>
          <button
            type="button"
            onClick={handleNextMonth}
            style={{
              padding: '6px 12px',
              borderRadius: 6,
              border: '1px solid var(--color-border, #ddd)',
              background: 'var(--color-surface, #fff)',
              cursor: 'pointer',
              display: 'inline-flex',
              alignItems: 'center',
            }}
            title="Next month"
          >
            <ChevronRight size={16} />
          </button>
        </div>
      </div>

      {/* Weekday headers */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 4, textAlign: 'center', fontWeight: 600, fontSize: '0.78rem', color: 'var(--color-muted, #71767b)' }}>
        {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((d) => (
          <div key={d} style={{ padding: '4px 0' }}>{d}</div>
        ))}
      </div>

      {/* Calendar Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 6 }}>
        {/* Empty cells before start of month */}
        {Array.from({ length: startDayOfWeek }).map((_, i) => (
          <div key={`empty-${i}`} style={{ minHeight: 64, opacity: 0.2 }} />
        ))}

        {/* Days of the month */}
        {Array.from({ length: daysInMonth }, (_, i) => {
          const day = i + 1;
          const dayEvts = getEventsForDay(day);
          const hasRegistered = dayEvts.some((ev) => app?.registrations?.includes(ev.id));
          const isSelected = day === selectedDay;
          const isToday = currentYear === 2026 && currentMonth === 9 && day === 26;

          return (
            <button
              key={day}
              type="button"
              onClick={() => setSelectedDay(day)}
              style={{
                minHeight: 64,
                padding: '6px 8px',
                borderRadius: 8,
                border: isSelected
                  ? '2px solid var(--accent, #e34d42)'
                  : hasRegistered
                  ? '1.5px solid #8b5cf6'
                  : '1px solid var(--color-border, #e5e7eb)',
                background: isSelected
                  ? 'var(--accent-tint, rgba(227, 77, 66, 0.08))'
                  : isToday
                  ? '#eff6ff'
                  : 'var(--color-surface, #fff)',
                cursor: 'pointer',
                textAlign: 'left',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                transition: 'all 0.15s ease',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
                <span
                  style={{
                    fontSize: '0.85rem',
                    fontWeight: isSelected || isToday ? 800 : 500,
                    color: isToday ? '#2563eb' : 'inherit',
                  }}
                >
                  {day}
                </span>
                {hasRegistered && (
                  <span
                    style={{
                      width: 8,
                      height: 8,
                      borderRadius: '50%',
                      background: 'var(--accent, #e34d42)',
                      boxShadow: '0 0 6px rgba(227, 77, 66, 0.6)',
                    }}
                    title="You have a registered gathering on this date"
                  />
                )}
              </div>

              {dayEvts.length > 0 && (
                <div style={{ marginTop: 4 }}>
                  <span
                    style={{
                      fontSize: '0.68rem',
                      fontWeight: 600,
                      padding: '2px 5px',
                      borderRadius: 4,
                      background: hasRegistered ? '#f3e8ff' : '#f1f5f9',
                      color: hasRegistered ? '#7c3aed' : '#475569',
                      display: 'block',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {hasRegistered ? '★ Registered' : `${dayEvts.length} event${dayEvts.length > 1 ? 's' : ''}`}
                  </span>
                </div>
              )}
            </button>
          );
        })}
      </div>

      {/* Selected Day Header & Events Filter */}
      <div style={{ marginTop: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h3 style={{ margin: 0, fontSize: '1.05rem', fontWeight: 700 }}>
            Gatherings on {monthNames[currentMonth]} {selectedDay}, {currentYear}
          </h3>
          <span style={{ fontSize: '0.82rem', color: 'var(--color-muted, #71767b)' }}>
            {selectedDayEvents.length} gathering{selectedDayEvents.length === 1 ? '' : 's'} scheduled
          </span>
        </div>

        {selectedDayEvents.length > 0 ? (
          <div className="event-list">
            {selectedDayEvents.map((e) => {
              const isRegistered = app?.registrations?.includes(e.id);
              const isCancelled = e.isCancelled || e.status === 'Cancelled';
              const eventDate = e.date ? new Date(e.date) : new Date(currentYear, currentMonth, selectedDay);
              const isPast = eventDate < new Date('2026-09-24T00:00:00');

              let statusLabel = 'Registration Open';
              let statusTone = 'blue';
              if (isCancelled) {
                statusLabel = 'Cancelled';
                statusTone = 'red';
              } else if (isRegistered && isPast) {
                statusLabel = 'Attended';
                statusTone = 'green';
              } else if (isRegistered) {
                statusLabel = 'Yet to attend';
                statusTone = 'purple';
              }

              return (
                <div className="event-row" key={e.id} style={{ cursor: 'default' }}>
                  <span className="date-tile">
                    <small>{e.month || curMonthShort}</small>
                    <b>{e.day || selectedDay}</b>
                  </span>
                  <span style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', gap: 6, alignItems: 'center', marginBottom: 4 }}>
                      <Tag tone={e.mode === 'Online' ? 'purple' : 'green'}>{e.mode}</Tag>
                      <Tag tone="coral">{e.category}</Tag>
                      <span
                        style={{
                          fontSize: '0.74rem',
                          fontWeight: 700,
                          padding: '2px 8px',
                          borderRadius: 12,
                          background:
                            statusTone === 'red' ? '#fee2e2' :
                            statusTone === 'green' ? '#ecfdf5' :
                            statusTone === 'purple' ? '#f3e8ff' : '#eff6ff',
                          color:
                            statusTone === 'red' ? '#b91c1c' :
                            statusTone === 'green' ? '#065f46' :
                            statusTone === 'purple' ? '#6d28d9' : '#1d4ed8',
                        }}
                      >
                        {statusLabel}
                      </span>
                    </div>
                    <strong style={{ fontSize: 15 }}>{e.title}</strong>
                    <p>{e.time} · {e.location}</p>
                    <small style={{ color: 'var(--muted)' }}>
                      {e.attendees || 0} registered attendees
                    </small>
                  </span>
                  <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                    <Button size="sm" onClick={() => setSelected(e)}>
                      View details
                    </Button>
                    {!isCancelled && !isRegistered && (
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() =>
                          app?.setModal({
                            type: 'details',
                            kind: 'event',
                            item: e,
                          })
                        }
                      >
                        Register
                      </Button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div
            style={{
              padding: 24,
              textAlign: 'center',
              background: 'var(--color-surface-hover, #f8f9fa)',
              borderRadius: 10,
              border: '1px dashed var(--color-border, #ddd)',
            }}
          >
            <CalendarDays size={28} color="var(--color-muted, #999)" style={{ margin: '0 auto 8px', display: 'block' }} />
            <b style={{ display: 'block', fontSize: '0.94rem' }}>
              No gatherings on {monthNames[currentMonth]} {selectedDay}
            </b>
            <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--color-muted, #71767b)' }}>
              Select highlighted dates in the calendar to view scheduled hackathons, meetups, and conferences.
            </p>
          </div>
        )}
      </div>
    </section>
  );
}

function OrganizerCalendar({ events: catalog, onManageEvent }) {
  const [currentYear, setCurrentYear] = useState(2026);
  const [currentMonth, setCurrentMonth] = useState(9); // October
  const [selectedDay, setSelectedDay] = useState(25);

  const monthNames = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December',
  ];
  const monthShorts = [
    'JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN',
    'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC',
  ];

  const handlePrev = () => {
    if (currentMonth === 0) {
      setCurrentMonth(11);
      setCurrentYear(currentYear - 1);
    } else {
      setCurrentMonth(currentMonth - 1);
    }
  };

  const handleNext = () => {
    if (currentMonth === 11) {
      setCurrentMonth(0);
      setCurrentYear(currentYear + 1);
    } else {
      setCurrentMonth(currentMonth + 1);
    }
  };

  const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();
  const startDayOfWeek = new Date(currentYear, currentMonth, 1).getDay();
  const curMonthShort = monthShorts[currentMonth];

  const getEventsForDay = (day) => {
    return catalog.filter((e) => {
      if (e.date) {
        const parts = e.date.split('-');
        if (parts.length === 3) {
          const y = Number(parts[0]);
          const m = Number(parts[1]) - 1;
          const d = Number(parts[2]);
          return y === currentYear && m === currentMonth && d === day;
        }
      }
      return Number(e.day) === day && (e.month || '').toUpperCase() === curMonthShort;
    });
  };

  const selectedDayEvents = getEventsForDay(selectedDay);

  return (
    <section className="calendar-panel" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h2 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 700 }}>
            {monthNames[currentMonth]} {currentYear}
          </h2>
          <small style={{ color: 'var(--color-muted, #71767b)' }}>Organizer Schedule &amp; Milestones</small>
        </div>
        <div style={{ display: 'flex', gap: 6 }}>
          <button
            type="button"
            onClick={handlePrev}
            style={{ padding: '6px 12px', borderRadius: 6, border: '1px solid #ccc', background: '#fff', cursor: 'pointer' }}
          >
            <ChevronLeft size={16} />
          </button>
          <button
            type="button"
            onClick={handleNext}
            style={{ padding: '6px 12px', borderRadius: 6, border: '1px solid #ccc', background: '#fff', cursor: 'pointer' }}
          >
            <ChevronRight size={16} />
          </button>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 4, textAlign: 'center', fontWeight: 600, fontSize: '0.78rem', color: 'var(--color-muted)' }}>
        {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((d) => (
          <div key={d} style={{ padding: '4px 0' }}>{d}</div>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 6 }}>
        {Array.from({ length: startDayOfWeek }).map((_, i) => (
          <div key={`empty-${i}`} style={{ minHeight: 60, opacity: 0.2 }} />
        ))}

        {Array.from({ length: daysInMonth }, (_, i) => {
          const day = i + 1;
          const dayEvts = getEventsForDay(day);
          const hasEvent = dayEvts.length > 0;
          const isSelected = day === selectedDay;

          return (
            <button
              key={day}
              type="button"
              onClick={() => setSelectedDay(day)}
              style={{
                minHeight: 60,
                padding: '6px 8px',
                borderRadius: 8,
                border: isSelected ? '2px solid var(--accent, #e34d42)' : hasEvent ? '1.5px solid #2563eb' : '1px solid #e5e7eb',
                background: isSelected ? 'var(--accent-tint, rgba(227, 77, 66, 0.08))' : '#fff',
                cursor: 'pointer',
                textAlign: 'left',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.85rem', fontWeight: isSelected ? 800 : 500 }}>{day}</span>
                {hasEvent && <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#2563eb' }} />}
              </div>
              {hasEvent && (
                <small style={{ fontSize: '0.68rem', color: '#2563eb', fontWeight: 700, display: 'block', marginTop: 4 }}>
                  {dayEvts[0].category}
                </small>
              )}
            </button>
          );
        })}
      </div>

      {/* Selected Day Events for Organizer */}
      <div style={{ marginTop: 12 }}>
        <h3 style={{ margin: '0 0 10px', fontSize: '1.05rem', fontWeight: 700 }}>
          Organizer Events on {monthNames[currentMonth]} {selectedDay}, {currentYear}
        </h3>
        {selectedDayEvents.length > 0 ? (
          <div className="event-list">
            {selectedDayEvents.map((e) => (
              <div className="event-row" key={e.id} style={{ cursor: 'default' }}>
                <span className="date-tile">
                  <small>{e.month || curMonthShort}</small>
                  <b>{e.day || selectedDay}</b>
                </span>
                <span style={{ flex: 1, minWidth: 0 }}>
                  <Tag tone="green">{e.status || 'Active'}</Tag>
                  <strong style={{ fontSize: 15 }}>{e.title}</strong>
                  <p>{e.time} · {e.location}</p>
                  <small>{e.attendees || 0} registered</small>
                </span>
                <Button variant="primary" size="sm" onClick={() => onManageEvent(e)}>
                  Manage event
                </Button>
              </div>
            ))}
          </div>
        ) : (
          <p style={{ color: 'var(--color-muted, #71767b)', fontSize: '0.85rem' }}>
            No organizer gatherings scheduled on this date.
          </p>
        )}
      </div>
    </section>
  );
}

function OrganizerEvents({ app }) {
  const [view, setView] = useState('Dashboard');
  const [createStep, setCreateStep] = useState(1);
  const [created, setCreated] = useState([]);
  const [manageEvent, setManageEvent] = useState(null);
  const [manageTab, setManageTab] = useState('Overview');

  const catalog = [
    ...created,
    ...(app.eventCatalog || []),
    ...events.filter(
      (ev) =>
        !created.some((c) => c.id === ev.id) &&
        !(app.eventCatalog || []).some((remote) => remote.id === ev.id)
    ),
  ];

  return (
    <div className="operator-layout">
      <section>
        <Tabs
          items={['Dashboard', 'My events', 'Calendar', 'Create event']}
          value={view}
          onChange={(v) => {
            setView(v);
            if (v !== 'My events') setManageEvent(null);
          }}
        />

        {view === 'Dashboard' && (
          <>
            <div className="company-dashboard">
              <div>
                <Tag tone="purple">Organizer Workspace</Tag>
                <h2>Welcome back, {app.profile.name}.</h2>
                <p>
                  Manage event rosters, live QR check-ins, attendee feedback,
                  and broadcast announcements from one command center.
                </p>
              </div>
              <Button icon={Plus} onClick={() => setView('Create event')}>
                Create event
              </Button>
            </div>

            <div className="metric-grid operator-metrics">
              <Metric value="12" label="Total events" />
              <Metric value="1.2k" label="Registrations" />
              <Metric value="856" label="Checked in" />
              <Metric value="85%" label="Check-in rate" />
            </div>

            <SectionTitle title="Quick actions" />
            <div className="quick-action-grid">
              <Button icon={Plus} onClick={() => setView('Create event')}>
                Create event
              </Button>
              <Button
                variant="ghost"
                icon={QrCode}
                onClick={() => {
                  setManageEvent(catalog[0]);
                  setManageTab('Check-in');
                  setView('My events');
                }}
              >
                Live QR Scanner
              </Button>
              <Button
                variant="ghost"
                icon={Users}
                onClick={() => {
                  setManageEvent(catalog[0]);
                  setManageTab('Attendees');
                  setView('My events');
                }}
              >
                Attendee Directory
              </Button>
              <Button
                variant="ghost"
                icon={Bell}
                onClick={() => {
                  setManageEvent(catalog[0]);
                  setManageTab('Broadcast');
                  setView('My events');
                }}
              >
                Broadcast Announcement
              </Button>
              <Button
                variant="ghost"
                icon={BarChart3}
                onClick={() => {
                  setManageEvent(catalog[0]);
                  setManageTab('Analytics');
                  setView('My events');
                }}
              >
                Event Analytics
              </Button>
              <Button
                variant="ghost"
                icon={Smile}
                onClick={() => {
                  setManageEvent(catalog[0]);
                  setManageTab('Feedback');
                  setView('My events');
                }}
              >
                Feedback &amp; Reviews
              </Button>
              <Button
                variant="ghost"
                icon={ReceiptText}
                onClick={() => {
                  setManageEvent(catalog[0]);
                  setManageTab('Revenue');
                  setView('My events');
                }}
              >
                Revenue &amp; Payouts
              </Button>
            </div>
          </>
        )}

        {view === 'My events' && (
          <>
            {!manageEvent ? (
              <div className="event-list">
                {catalog.map((e) => (
                  <div className="event-row" key={e.id}>
                    <span className="date-tile">
                      <small>{e.month}</small>
                      <b>{e.day}</b>
                    </span>
                    <span>
                      <Tag tone={e.isCancelled || e.status === 'Cancelled' ? 'red' : 'green'}>
                        {e.isCancelled || e.status === 'Cancelled' ? 'Cancelled' : 'Upcoming'}
                      </Tag>
                      <strong>{e.title}</strong>
                      <p>
                        {e.time} · {e.location}
                      </p>
                      <small>{e.attendees || 0} registered</small>
                    </span>
                    <Button
                      variant="primary"
                      onClick={() => {
                        setManageEvent(e);
                        setManageTab('Overview');
                      }}
                    >
                      Manage event
                    </Button>
                  </div>
                ))}
              </div>
            ) : (
              <div className="manage-event-console">
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    marginBottom: 16,
                  }}
                >
                  <button
                    className="back-link"
                    onClick={() => setManageEvent(null)}
                  >
                    ← All organizer events
                  </button>
                  <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <Tag tone="coral">{manageEvent.category}</Tag>
                    <b>{manageEvent.title}</b>
                  </div>
                </div>

                <Tabs
                  items={[
                    'Overview',
                    'Attendees',
                    'Check-in',
                    'Broadcast',
                    'Analytics',
                    'Feedback',
                    'Revenue',
                    'Reports',
                    'Settings',
                  ]}
                  value={manageTab}
                  onChange={setManageTab}
                />

                {manageTab === 'Overview' && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                    <div className="metric-grid operator-metrics">
                      <Metric value={manageEvent.attendees || 84} label="Registered" />
                      <Metric value="68" label="Checked in" />
                      <Metric value="81%" label="Check-in rate" />
                      <Metric value="4.8 ★" label="Attendee CSAT" />
                    </div>
                    <div className="operator-columns">
                      <section className="inset-card">
                        <h3>Event Details</h3>
                        <p>{manageEvent.description}</p>
                        <DetailRow label="Schedule" value={`${manageEvent.date} · ${manageEvent.time}`} />
                        <DetailRow label="Venue / URL" value={manageEvent.location} />
                        <DetailRow label="Format" value={manageEvent.mode} />
                      </section>
                      <section className="inset-card">
                        <h3>Quick Controls</h3>
                        <div style={{ display: 'grid', gap: 8 }}>
                          <Button icon={QrCode} onClick={() => setManageTab('Check-in')}>
                            Launch QR Check-in Desk
                          </Button>
                          <Button variant="ghost" icon={Bell} onClick={() => setManageTab('Broadcast')}>
                            Broadcast reminder
                          </Button>
                          <Button variant="ghost" icon={Smile} onClick={() => setManageTab('Feedback')}>
                            View attendee feedback
                          </Button>
                        </div>
                      </section>
                    </div>
                  </div>
                )}

                {manageTab === 'Attendees' && <Attendees app={app} eventId={manageEvent.id} />}
                {manageTab === 'Check-in' && <CheckIn app={app} eventId={manageEvent.id} />}
                {manageTab === 'Broadcast' && <BroadcastReminder app={app} eventId={manageEvent.id} />}
                {manageTab === 'Analytics' && <EventAnalytics />}
                {manageTab === 'Feedback' && <FeedbackAnalysis event={manageEvent} />}
                {manageTab === 'Revenue' && <OrganizerRevenue app={app} />}
                {manageTab === 'Reports' && <Reports />}
                {manageTab === 'Settings' && (
                  <EventSettings
                    app={app}
                    event={manageEvent}
                    onUpdateEvent={(updated) => {
                      setManageEvent(updated);
                      setCreated((prev) =>
                        prev.map((e) => (e.id === updated.id ? updated : e))
                      );
                      if (app.setEventCatalog) {
                        app.setEventCatalog((prev) =>
                          (prev || []).map((e) => (e.id === updated.id ? updated : e))
                        );
                      }
                    }}
                  />
                )}
              </div>
            )}
          </>
        )}

        {view === 'Calendar' && (
          <OrganizerCalendar
            events={catalog}
            onManageEvent={(e) => {
              setManageEvent(e);
              setManageTab('Overview');
              setView('My events');
            }}
          />
        )}

        {view === 'Create event' && (
          <CreateEventFlow
            step={createStep}
            setStep={setCreateStep}
            onPublish={(newEventData) => {
              const fullEvent = {
                id: `evt-${Date.now()}`,
                title: newEventData.title || 'New Community Gathering',
                tagline: newEventData.tagline || '',
                category: newEventData.category || 'Meetup',
                mode: newEventData.format || 'In person',
                date: newEventData.date || '2026-10-20',
                endDate: newEventData.endDate || newEventData.date || '2026-10-20',
                day: (newEventData.date ? newEventData.date.split('-')[2] : '20') || '20',
                month: (newEventData.date ? new Date(newEventData.date).toLocaleString('en-US', { month: 'short' }).toUpperCase() : 'OCT') || 'OCT',
                time: newEventData.time || '18:00 - 21:00',
                location: newEventData.location || 'Bengaluru, India',
                venueAddress: newEventData.venueAddress || '',
                virtualLink: newEventData.virtualLink || '',
                description:
                  newEventData.description ||
                  'An interactive gathering for creators and engineers.',
                ticketPrice: newEventData.ticketPrice || '0',
                capacity: newEventData.capacity || '150',
                prizes: newEventData.prizes || (newEventData.category === 'Hackathon' ? '₹2,50,000 Total Prize Pool + Cloud Credits' : 'Swag Kits & Certificates'),
                prizePool: newEventData.prizePool || '',
                rules: newEventData.rules || 'Open to all builders. Respect code of conduct and present original work.',
                attendees: 1,
                image: newEventData.bannerImage || newEventData.image || photos[0],
                bannerImage: newEventData.bannerImage || newEventData.image || photos[0],
                rounds: newEventData.rounds || null,
                customSections: newEventData.customSections || [],
                offerFreeTier: newEventData.offerFreeTier !== false,
                offerPaidTier: Boolean(newEventData.offerPaidTier),
                paidTierPrice: newEventData.paidTierPrice || '0',
                freeCapacity: newEventData.freeCapacity || '150',
                paidCapacity: newEventData.paidCapacity || '50',
                regDeadline: newEventData.regDeadline || newEventData.date || '2026-10-20',
                regStatus: 'Open',
                isCancelled: false,
              };
              setCreated((x) => [fullEvent, ...x]);
              if (app.setEventCatalog) {
                app.setEventCatalog((old) => [fullEvent, ...(old || [])]);
              }
              setView('My events');
              setManageEvent(fullEvent);
              setManageTab('Overview');
              app.toast('Event published successfully!');
              app.notify(
                'Event published',
                `${fullEvent.title} is now published and visible to attendees.`
              );
            }}
          />
        )}
      </section>

      <aside className="space-rail">
        <SectionTitle title="Your next event" />
        <div className="rail-card">
          <Tag tone="green">Upcoming</Tag>
          <b>{catalog[0]?.title || 'Flutter meetup'}</b>
          <span>
            {catalog[0]?.date || '2026-10-14'} · {catalog[0]?.time || '10:00 AM'}
          </span>
          <Button
            variant="ghost"
            onClick={() => {
              setManageEvent(catalog[0]);
              setManageTab('Check-in');
              setView('My events');
            }}
          >
            Open check-in
          </Button>
        </div>

        <SectionTitle title="Event checklist" />
        <Checklist
          items={[
            'Event details published',
            'Ticketing & registration live',
            'Email confirmation active',
            'QR Scanner configured',
            'Reminders scheduled',
          ]}
        />
      </aside>
    </div>
  );
}

function FeedbackAnalysis({ event }) {
  return (
    <section className="feedback-analysis-panel">
      <div className="feedback-summary-grid">
        <div className="feedback-stat-card">
          <b>4.8 / 5.0</b>
          <div style={{ color: '#f59e0b', margin: '4px 0' }}>★★★★★</div>
          <small>Average CSAT Score (124 reviews)</small>
        </div>
        <div className="feedback-stat-card">
          <b>+74</b>
          <div style={{ color: '#22c55e', margin: '4px 0', fontWeight: 700 }}>
            Excellent
          </div>
          <small>Net Promoter Score (NPS)</small>
        </div>
        <div className="feedback-stat-card">
          <b>91%</b>
          <div className="sentiment-bar-track">
            <span className="sentiment-pos" style={{ width: '91%' }} />
            <span className="sentiment-neu" style={{ width: '7%' }} />
            <span className="sentiment-neg" style={{ width: '2%' }} />
          </div>
          <small>Positive sentiment (91% Pos · 7% Neu · 2% Neg)</small>
        </div>
      </div>

      <SectionTitle title="Recent Attendee Reviews" />
      <div className="feedback-quote-list">
        {[
          {
            name: 'Rohit Sharma',
            role: 'Senior Frontend Dev',
            rating: 5,
            comment:
              'Outstanding organization! The hackathon rounds were clear, mentoring sessions were invaluable, and check-in via QR took less than 5 seconds.',
          },
          {
            name: 'Kavita Pillai',
            role: 'Product Designer',
            rating: 5,
            comment:
              'Great balance of technical depth and networking. Very smooth ticketing and entry experience.',
          },
          {
            name: 'Aditya Nair',
            role: 'AI Researcher',
            rating: 4,
            comment:
              'High quality speakers and engaging live discussion. Would love to have more breakout rooms next time!',
          },
        ].map((fb, idx) => (
          <div className="feedback-quote-item" key={idx}>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                marginBottom: 4,
              }}
            >
              <b>
                {fb.name} · <small style={{ color: 'var(--muted)' }}>{fb.role}</small>
              </b>
              <span style={{ color: '#f59e0b' }}>
                {'★'.repeat(fb.rating)}
                {'☆'.repeat(5 - fb.rating)}
              </span>
            </div>
            <p style={{ margin: 0, color: 'var(--ink)' }}>"{fb.comment}"</p>
          </div>
        ))}
      </div>
    </section>
  );
}

function OrganizerRevenue({ app }) {
  const orders = app.payoutRequests || [];
  const paid = (app.paymentOrders || []).filter(
    (o) => o.status === 'completed'
  );
  const grossCents = paid.reduce((sum, o) => sum + (o.amountCents || 0), 0);
  const platformFeePct = 0.05;
  const netCents = Math.round(grossCents * (1 - platformFeePct));

  const [method, setMethod] = useState('bank_account');
  const [dest, setDest] = useState('');
  const [payAmt, setPayAmt] = useState('');
  const [payoutSaving, setPayoutSaving] = useState(false);

  const handleRequestPayout = async (e) => {
    e.preventDefault();
    if (!dest.trim() || !payAmt) return;
    setPayoutSaving(true);
    try {
      const amountCents = Math.round(Number(payAmt) * 100);
      await app.requestPayout({
        amountCents,
        method,
        destinationReference: dest.trim(),
      });
      app.toast('Payout request submitted. Processing in 3-5 business days.');
    } catch (err) {
      app.toast(err.message || 'Could not submit payout request.');
    } finally {
      setPayoutSaving(false);
      setDest('');
      setPayAmt('');
    }
  };

  return (
    <section className="revenue-workspace">
      <div className="revenue-stats-grid">
        <div className="revenue-stat-card">
          <span className="revenue-stat-icon">🎟</span>
          <div>
            <b>{paid.length || 24}</b>
            <small>Paid tickets sold</small>
          </div>
        </div>
        <div className="revenue-stat-card">
          <span className="revenue-stat-icon">💰</span>
          <div>
            <b>₹{((grossCents || 1197600) / 100).toLocaleString('en-IN')}</b>
            <small>Gross revenue</small>
          </div>
        </div>
        <div className="revenue-stat-card">
          <span className="revenue-stat-icon">📤</span>
          <div>
            <b>₹{((netCents || 1137720) / 100).toLocaleString('en-IN')}</b>
            <small>Net payout balance (after 5% fee)</small>
          </div>
        </div>
        <div className="revenue-stat-card">
          <span className="revenue-stat-icon">⏳</span>
          <div>
            <b>{orders.filter((p) => p.status === 'pending').length}</b>
            <small>Pending payouts</small>
          </div>
        </div>
      </div>

      <div className="revenue-columns">
        <section className="revenue-payout-form">
          <h3>Request payout</h3>
          <p className="muted">
            Payouts are transferred directly to your bank account or UPI VPA.
          </p>
          <form onSubmit={handleRequestPayout} className="form-stack">
            <label className="field">
              <span>Method</span>
              <select
                value={method}
                onChange={(e) => setMethod(e.target.value)}
              >
                <option value="bank_account">Bank account (NEFT / IMPS)</option>
                <option value="upi">UPI ID</option>
              </select>
            </label>
            <label className="field">
              <span>
                {method === 'upi' ? 'UPI ID' : 'Account Number &amp; IFSC'}
              </span>
              <input
                value={dest}
                onChange={(e) => setDest(e.target.value)}
                placeholder={method === 'upi' ? 'you@bank' : 'HDFC0001234 · 50100234567890'}
                required
              />
            </label>
            <label className="field">
              <span>Amount (₹)</span>
              <input
                type="number"
                min="1"
                step="0.01"
                value={payAmt}
                onChange={(e) => setPayAmt(e.target.value)}
                placeholder="e.g. 5000"
                required
              />
            </label>
            <Button disabled={payoutSaving}>
              {payoutSaving ? 'Submitting…' : 'Request payout'}
            </Button>
          </form>
        </section>

        <section className="revenue-payout-history">
          <h3>Payout history</h3>
          {orders.length === 0 ? (
            <p className="muted">No payout requests yet.</p>
          ) : (
            <div className="payout-list">
              {orders.map((p) => (
                <div className="payout-row" key={p.id}>
                  <div>
                    <b>
                      ₹{((p.amountCents || 0) / 100).toLocaleString('en-IN')}
                    </b>
                    <small>
                      {p.method === 'upi' ? 'UPI' : 'Bank'} ·{' '}
                      {new Date(p.createdAt).toLocaleDateString('en-IN')}
                    </small>
                  </div>
                  <span className={`order-status-badge status-${p.status}`}>
                    {p.status}
                  </span>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </section>
  );
}

function CreateEventFlow({ step, setStep, onPublish }) {
  const [title, setTitle] = useState('');
  const [tagline, setTagline] = useState('');
  const [category, setCategory] = useState('Hackathon');
  const [format, setFormat] = useState('Hybrid');
  const [capacity, setCapacity] = useState('200');

  // Banner image state & presets
  const bannerPresets = [
    { label: 'Hackathon Sprint', url: photos[0] || 'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1200&q=80' },
    { label: 'Developer Summit', url: photos[1] || 'https://images.unsplash.com/photo-1531403009284-440f080d1e12?auto=format&fit=crop&w=1200&q=80' },
    { label: 'AI & Data Lab', url: photos[2] || 'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?auto=format&fit=crop&w=1200&q=80' },
    { label: 'Tech Conference', url: photos[3] || 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=1200&q=80' },
  ];
  const [bannerImage, setBannerImage] = useState(bannerPresets[0].url);
  const [customBannerUrl, setCustomBannerUrl] = useState('');
  
  // Date & Venue
  const [date, setDate] = useState('2026-10-25');
  const [endDate, setEndDate] = useState('2026-10-27');
  const [time, setTime] = useState('09:30 AM - 06:00 PM IST');
  const [location, setLocation] = useState('Bengaluru, Karnataka');
  const [venueAddress, setVenueAddress] = useState('Bengaluru Innovation Hub, Koramangala 4th Block');
  const [virtualLink, setVirtualLink] = useState('https://meet.google.com/clb-hackathon-2026');
  
  // Simultaneous Free & Paid Ticketing Tiers
  const [offerFreeTier, setOfferFreeTier] = useState(true);
  const [freeCapacity, setFreeCapacity] = useState('150');
  const [freeDescription, setFreeDescription] = useState('General admission pass with access to all keynote sessions and expo floor');
  
  const [offerPaidTier, setOfferPaidTier] = useState(true);
  const [paidTierPrice, setPaidTierPrice] = useState('499');
  const [paidCapacity, setPaidCapacity] = useState('50');
  const [paidPerks, setPaidPerks] = useState('VIP front-row access, 1:1 mentor speed-networking, curated swag box & networking mixer');

  // Squad / Team Registration
  const [allowTeamReg, setAllowTeamReg] = useState(true);
  const [teamSizeMin, setTeamSizeMin] = useState('2');
  const [teamSizeMax, setTeamSizeMax] = useState('4');

  // Content & Guidelines
  const [description, setDescription] = useState('');
  const [prizes, setPrizes] = useState('₹5,00,000 Total Prize Pool (1st: ₹2,50,000 · 2nd: ₹1,50,000 · 3rd: ₹1,00,000 + Cloud Credits)');
  const [rules, setRules] = useState('1. Open to all students and professionals.\n2. Projects must be built during the hackathon sprint.\n3. Plagiarism leads to disqualification.');

  // Custom Sections Builder
  const [customSections, setCustomSections] = useState([
    {
      id: 'cs-1',
      title: 'Prerequisites & Preparation',
      content: 'Bring your laptop with Node.js 20+, Git, and your preferred IDE installed. Cloud sandbox keys and API credits will be distributed at check-in.',
    },
    {
      id: 'cs-2',
      title: 'Judging Criteria & Scoring Rubric',
      content: 'Projects are evaluated on: Innovation & Impact (30%), Technical Execution & Architecture (30%), Practical Usability (20%), and Live Pitch Delivery (20%).',
    },
  ]);

  const handleAddCustomSection = (title = '', content = '') => {
    setCustomSections((prev) => [
      ...prev,
      {
        id: `cs-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
        title: title || 'New Section Title',
        content: content || 'Provide details, instructions or guidelines for attendees...',
      },
    ]);
  };

  const handleUpdateCustomSection = (idx, field, value) => {
    setCustomSections((prev) => {
      const copy = [...prev];
      copy[idx] = { ...copy[idx], [field]: value };
      return copy;
    });
  };

  const handleRemoveCustomSection = (idx) => {
    setCustomSections((prev) => prev.filter((_, i) => i !== idx));
  };

  // Customizable Rounds Builder
  const [rounds, setRounds] = useState([
    {
      name: 'Round 1: Idea Pitch & Problem Statement',
      date: 'Day 1 · Oct 25 (10:00 AM)',
      description: 'Submit your solution concept, target users, and architectural stack diagram.',
      deliverable: 'Pitch Deck PDF or Notion Link',
      status: 'Upcoming',
    },
    {
      name: 'Round 2: Mid-Sprint Prototype & Mentor Review',
      date: 'Day 2 · Oct 26 (02:00 PM)',
      description: 'Hands-on live codebase review with assigned industry mentors.',
      deliverable: 'Public GitHub Repo & Working Preview',
      status: 'Upcoming',
    },
    {
      name: 'Round 3: Grand Finale & Live Jury Showcase',
      date: 'Day 3 · Oct 27 (04:00 PM)',
      description: 'Top 10 finalists present a 4-minute demo pitch to the venture panel.',
      deliverable: 'Live Demo & Production Deployment',
      status: 'Upcoming',
    },
  ]);

  const handleUpdateRound = (idx, field, value) => {
    setRounds((prev) => {
      const copy = [...prev];
      copy[idx] = { ...copy[idx], [field]: value };
      return copy;
    });
  };

  const handleAddRound = () => {
    setRounds((prev) => [
      ...prev,
      {
        name: `Round ${prev.length + 1}: Checkpoint Stage`,
        date: 'Day 3 · Oct 27 (11:00 AM)',
        description: 'Describe stage guidelines and milestone goals.',
        deliverable: 'Code or Demo link',
        status: 'Upcoming',
      },
    ]);
  };

  const handleRemoveRound = (idx) => {
    setRounds((prev) => prev.filter((_, i) => i !== idx));
  };

  return (
    <section className="content-editor">
      <div className="stepper">
        {['Overview & Banner', 'Date & Venue', 'Ticketing Tiers', 'Custom Sections', 'Rounds & Schedule', 'Publish'].map(
          (x, i) => (
            <span
              key={x}
              className={step === i + 1 ? 'active' : step > i + 1 ? 'done' : ''}
              onClick={() => step > i + 1 && setStep(i + 1)}
              style={{ cursor: step > i + 1 ? 'pointer' : 'default' }}
            >
              <i>{i + 1}</i>
              {x}
            </span>
          )
        )}
      </div>

      {/* STEP 1: Overview, Identity & Banner Image */}
      {step === 1 && (
        <div className="form-stack">
          {/* Banner Image Selector & Preview */}
          <div style={{ marginBottom: 12 }}>
            <span style={{ fontSize: '0.84rem', fontWeight: 600, color: 'var(--color-heading, #222)', display: 'block', marginBottom: 6 }}>
              Event Banner Image *
            </span>
            <div
              style={{
                position: 'relative',
                height: 140,
                borderRadius: 10,
                overflow: 'hidden',
                border: '1px solid var(--color-border, #ddd)',
                marginBottom: 10,
                background: '#1a1a1a',
              }}
            >
              <img
                src={bannerImage}
                alt="Banner preview"
                style={{ width: '100%', height: '100%', objectFit: 'cover', opacity: 0.9 }}
              />
              <div
                style={{
                  position: 'absolute',
                  inset: 0,
                  background: 'linear-gradient(to top, rgba(0,0,0,0.7) 0%, transparent 60%)',
                  display: 'flex',
                  alignItems: 'flex-end',
                  padding: '12px 16px',
                }}
              >
                <div style={{ color: '#fff' }}>
                  <span style={{ fontSize: '0.74rem', background: 'rgba(255,255,255,0.25)', padding: '2px 8px', borderRadius: 4, textTransform: 'uppercase', fontWeight: 700, letterSpacing: '0.04em' }}>
                    {category}
                  </span>
                  <div style={{ fontSize: '1.05rem', fontWeight: 700, marginTop: 4 }}>
                    {title || 'Your Event Title Preview'}
                  </div>
                </div>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8, marginBottom: 10 }}>
              {bannerPresets.map((p, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => {
                    setBannerImage(p.url);
                    setCustomBannerUrl('');
                  }}
                  style={{
                    position: 'relative',
                    height: 52,
                    borderRadius: 6,
                    overflow: 'hidden',
                    border: bannerImage === p.url ? '2px solid var(--accent, #e34d42)' : '1px solid #ccc',
                    padding: 0,
                    cursor: 'pointer',
                  }}
                >
                  <img src={p.url} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                  <span
                    style={{
                      position: 'absolute',
                      inset: 0,
                      background: 'rgba(0,0,0,0.4)',
                      color: '#fff',
                      fontSize: '0.68rem',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: 600,
                      textAlign: 'center',
                      padding: 2,
                    }}
                  >
                    {p.label}
                  </span>
                </button>
              ))}
            </div>

            <div style={{ display: 'flex', gap: 8 }}>
              <input
                value={customBannerUrl}
                onChange={(e) => {
                  setCustomBannerUrl(e.target.value);
                  if (e.target.value.trim().startsWith('http')) {
                    setBannerImage(e.target.value.trim());
                  }
                }}
                placeholder="Or paste custom banner image URL (https://...)"
                style={{ flex: 1, padding: '7px 10px', borderRadius: 6, border: '1px solid #ccc', fontSize: '0.84rem' }}
              />
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  const randomPreset = bannerPresets[Math.floor(Math.random() * bannerPresets.length)].url;
                  setBannerImage(randomPreset);
                }}
              >
                Shuffle Banner
              </Button>
            </div>
          </div>

          <Field label="Event title *">
            <input
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Autonomous AI Hackathon 2026"
            />
          </Field>
          <Field label="Short Tagline / Pitch">
            <input
              value={tagline}
              onChange={(e) => setTagline(e.target.value)}
              placeholder="e.g. Build multi-agent systems and real-world tools with next-gen models"
            />
          </Field>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <Field label="Category">
              <select value={category} onChange={(e) => setCategory(e.target.value)}>
                <option value="Hackathon">Hackathon</option>
                <option value="Conference">Conference</option>
                <option value="Webinar">Webinar</option>
                <option value="Workshop">Workshop</option>
                <option value="Meetup">Meetup</option>
              </select>
            </Field>
            <Field label="Format">
              <select value={format} onChange={(e) => setFormat(e.target.value)}>
                <option value="Hybrid">Hybrid (In person + Online)</option>
                <option value="Online">Online / Virtual</option>
                <option value="In person">In person Only</option>
              </select>
            </Field>
          </div>
          <Field label="Expected Capacity / Max Attendees">
            <input
              type="number"
              min="10"
              value={capacity}
              onChange={(e) => setCapacity(e.target.value)}
              placeholder="e.g. 250"
            />
          </Field>
          <Field label="Detailed Description">
            <textarea
              rows="4"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Explain the background, tracks, who should attend, and what will happen during the gathering..."
            />
          </Field>
          <div className="flow-actions" style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 12 }}>
            <Button onClick={() => setStep(2)} disabled={!title.trim()}>
              Next: Schedule &amp; Venue
            </Button>
          </div>
        </div>
      )}

      {/* STEP 2: Schedule & Venue Customization */}
      {step === 2 && (
        <div className="form-stack">
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <Field label="Start Date *">
              <input
                type="date"
                required
                value={date}
                onChange={(e) => setDate(e.target.value)}
              />
            </Field>
            <Field label="End Date">
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </Field>
          </div>
          <Field label="Time Window">
            <input
              value={time}
              onChange={(e) => setTime(e.target.value)}
              placeholder="e.g. 09:30 AM - 06:00 PM IST"
            />
          </Field>
          <Field label="Primary City / Region">
            <input
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              placeholder="e.g. Bengaluru, Karnataka or Global Remote"
            />
          </Field>
          {format !== 'Online' && (
            <Field label="Physical Venue Address">
              <input
                value={venueAddress}
                onChange={(e) => setVenueAddress(e.target.value)}
                placeholder="e.g. Hall 4, PES University Tech Quad, 100 Feet Ring Road"
              />
            </Field>
          )}
          {format !== 'In person' && (
            <Field label="Virtual Livestream / Room URL">
              <input
                value={virtualLink}
                onChange={(e) => setVirtualLink(e.target.value)}
                placeholder="e.g. https://meet.google.com/xyz or Zoom URL"
              />
            </Field>
          )}
          <div className="flow-actions" style={{ display: 'flex', justifyContent: 'space-between', marginTop: 12 }}>
            <Button variant="ghost" onClick={() => setStep(1)}>
              Back
            </Button>
            <Button onClick={() => setStep(3)}>Next: Ticketing Tiers</Button>
          </div>
        </div>
      )}

      {/* STEP 3: Dual Ticketing (Both Free and Paid simultaneously) */}
      {step === 3 && (
        <div className="form-stack">
          <div>
            <h4 style={{ margin: 0, fontSize: '1rem' }}>Admission &amp; Ticketing Tiers</h4>
            <p style={{ margin: '4px 0 12px', fontSize: '0.84rem', color: 'var(--color-muted, #71767b)' }}>
              You can offer <b>both Free and Paid ticket tiers at the same time</b>, letting attendees choose between complimentary standard entry or a VIP / Supporter pass.
            </p>
          </div>

          {/* Tier 1: Free Pass */}
          <div
            style={{
              padding: 14,
              borderRadius: 10,
              border: offerFreeTier ? '2px solid #059669' : '1px solid #ddd',
              background: offerFreeTier ? '#ecfdf5' : '#fafafa',
              display: 'flex',
              flexDirection: 'column',
              gap: 8,
            }}
          >
            <label style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={offerFreeTier}
                onChange={(e) => {
                  if (!e.target.checked && !offerPaidTier) {
                    return; // Prevent disabling both
                  }
                  setOfferFreeTier(e.target.checked);
                }}
                style={{ width: 18, height: 18, accentColor: '#059669', cursor: 'pointer' }}
              />
              <span style={{ fontSize: '0.96rem', fontWeight: 700, color: offerFreeTier ? '#065f46' : '#555' }}>
                Option 1: Offer Free General Admission Pass (₹0)
              </span>
            </label>
            {offerFreeTier && (
              <div style={{ display: 'grid', gridTemplateColumns: '120px 1fr', gap: 10, marginTop: 4 }}>
                <Field label="Free Seats Limit">
                  <input
                    type="number"
                    min="1"
                    value={freeCapacity}
                    onChange={(e) => setFreeCapacity(e.target.value)}
                    placeholder="150"
                  />
                </Field>
                <Field label="Tier Description &amp; Perks">
                  <input
                    value={freeDescription}
                    onChange={(e) => setFreeDescription(e.target.value)}
                    placeholder="Full access to keynote sessions and expo floor"
                  />
                </Field>
              </div>
            )}
          </div>

          {/* Tier 2: Paid / VIP Pass */}
          <div
            style={{
              padding: 14,
              borderRadius: 10,
              border: offerPaidTier ? '2px solid var(--accent, #e34d42)' : '1px solid #ddd',
              background: offerPaidTier ? 'var(--accent-tint, rgba(227, 77, 66, 0.06))' : '#fafafa',
              display: 'flex',
              flexDirection: 'column',
              gap: 8,
            }}
          >
            <label style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={offerPaidTier}
                onChange={(e) => {
                  if (!e.target.checked && !offerFreeTier) {
                    return; // Prevent disabling both
                  }
                  setOfferPaidTier(e.target.checked);
                }}
                style={{ width: 18, height: 18, accentColor: 'var(--accent, #e34d42)', cursor: 'pointer' }}
              />
              <span style={{ fontSize: '0.96rem', fontWeight: 700, color: offerPaidTier ? 'var(--accent, #e34d42)' : '#555' }}>
                Option 2: Offer Paid / VIP Supporter Pass
              </span>
            </label>
            {offerPaidTier && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 4 }}>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                  <Field label="Ticket Price (₹) *">
                    <input
                      type="number"
                      min="1"
                      value={paidTierPrice}
                      onChange={(e) => setPaidTierPrice(e.target.value)}
                      placeholder="e.g. 499"
                    />
                  </Field>
                  <Field label="VIP Seats Limit">
                    <input
                      type="number"
                      min="1"
                      value={paidCapacity}
                      onChange={(e) => setPaidCapacity(e.target.value)}
                      placeholder="50"
                    />
                  </Field>
                </div>
                <Field label="VIP Perks &amp; Inclusions">
                  <input
                    value={paidPerks}
                    onChange={(e) => setPaidPerks(e.target.value)}
                    placeholder="e.g. VIP front-row seating, mentor lounge, lunch & swag kit"
                  />
                </Field>
              </div>
            )}
          </div>

          <div style={{ background: 'var(--color-surface-hover, #f8f9fa)', padding: 12, borderRadius: 8, border: '1px solid var(--color-border, #e5e7eb)' }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', fontSize: '0.86rem', fontWeight: 600 }}>
              <input
                type="checkbox"
                checked={allowTeamReg}
                onChange={(e) => setAllowTeamReg(e.target.checked)}
              />
              <span>Allow Team Registrations (Hackathon Squads)</span>
            </label>
            {allowTeamReg && (
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginTop: 10 }}>
                <label style={{ fontSize: '0.8rem', display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <span>Min Squad Size</span>
                  <select value={teamSizeMin} onChange={(e) => setTeamSizeMin(e.target.value)}>
                    <option value="1">1 member (Solo allowed)</option>
                    <option value="2">2 members</option>
                  </select>
                </label>
                <label style={{ fontSize: '0.8rem', display: 'flex', flexDirection: 'column', gap: 4 }}>
                  <span>Max Squad Size</span>
                  <select value={teamSizeMax} onChange={(e) => setTeamSizeMax(e.target.value)}>
                    <option value="3">3 members</option>
                    <option value="4">4 members</option>
                    <option value="5">5 members</option>
                  </select>
                </label>
              </div>
            )}
          </div>

          <Field label="Prize Pool &amp; Rewards (Details)">
            <input
              value={prizes}
              onChange={(e) => setPrizes(e.target.value)}
              placeholder="e.g. ₹5,00,000 Total Pool (1st: ₹2.5L, 2nd: ₹1.5L, 3rd: ₹1L + Swags)"
            />
          </Field>

          <Field label="Rules, Eligibility &amp; Submission Guidelines">
            <textarea
              rows="3"
              value={rules}
              onChange={(e) => setRules(e.target.value)}
              placeholder="Outline rules, eligibility, intellectual property policy..."
            />
          </Field>

          <div className="flow-actions" style={{ display: 'flex', justifyContent: 'space-between', marginTop: 12 }}>
            <Button variant="ghost" onClick={() => setStep(2)}>
              Back
            </Button>
            <Button onClick={() => setStep(4)}>Next: Custom Sections</Button>
          </div>
        </div>
      )}

      {/* STEP 4: Customized Sections Builder */}
      {step === 4 && (
        <div className="form-stack">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
            <div>
              <h4 style={{ margin: 0, fontSize: '1rem' }}>Customized Sections</h4>
              <p style={{ margin: '2px 0 0', fontSize: '0.84rem', color: 'var(--color-muted, #71767b)' }}>
                Add tailored sections like Prerequisites, Hardware, Mentorship, Travel, or FAQs to your event page.
              </p>
            </div>
            <Button variant="secondary" size="sm" onClick={() => handleAddCustomSection()}>
              + Add Section
            </Button>
          </div>

          {/* Quick template chips */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 8 }}>
            <span style={{ fontSize: '0.78rem', color: 'var(--color-muted, #71767b)', alignSelf: 'center', marginRight: 4 }}>
              Add quick template:
            </span>
            {[
              { t: 'Hardware & Lab Kit', c: 'Access to high-spec GPU workstations, IoT sensors, and microcontroller development kits will be provided in Hall B.' },
              { t: 'Mentorship Tracks', c: 'Industry specialists from Google, Microsoft, and AI startups will conduct 1:1 architectural reviews from 2 PM to 5 PM.' },
              { t: 'Travel & Accommodation', c: 'Outstation participants are eligible for travel reimbursement up to ₹2,500 and dorm accommodation on PES campus.' },
              { t: 'Code of Conduct & FAQs', c: 'Collobster values a harassment-free and inclusive hackathon experience for everyone. Bring your College/Govt photo ID.' },
            ].map((tpl) => (
              <button
                key={tpl.t}
                type="button"
                onClick={() => handleAddCustomSection(tpl.t, tpl.c)}
                style={{
                  padding: '4px 10px',
                  borderRadius: 16,
                  border: '1px solid #d1d5db',
                  background: '#f9fafb',
                  fontSize: '0.78rem',
                  cursor: 'pointer',
                }}
              >
                + {tpl.t}
              </button>
            ))}
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {customSections.map((sec, idx) => (
              <div
                key={sec.id || idx}
                style={{
                  background: '#fff',
                  border: '1px solid var(--color-border, #ddd)',
                  borderRadius: 8,
                  padding: 12,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 8,
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <b style={{ fontSize: '0.86rem', color: 'var(--accent, #e34d42)' }}>
                    Custom Section #{idx + 1}
                  </b>
                  <button
                    type="button"
                    onClick={() => handleRemoveCustomSection(idx)}
                    style={{ background: 'none', border: 'none', color: '#dc2626', cursor: 'pointer', fontSize: '0.78rem' }}
                  >
                    Remove
                  </button>
                </div>
                <input
                  value={sec.title}
                  onChange={(e) => handleUpdateCustomSection(idx, 'title', e.target.value)}
                  placeholder="Section Title (e.g. Prerequisites & Preparation)"
                  style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid #ccc', fontSize: '0.86rem', fontWeight: 600 }}
                />
                <textarea
                  rows="3"
                  value={sec.content}
                  onChange={(e) => handleUpdateCustomSection(idx, 'content', e.target.value)}
                  placeholder="Section details, instructions, links, or guidelines for attendees..."
                  style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid #ccc', fontSize: '0.84rem' }}
                />
              </div>
            ))}
          </div>

          <div className="flow-actions" style={{ display: 'flex', justifyContent: 'space-between', marginTop: 14 }}>
            <Button variant="ghost" onClick={() => setStep(3)}>
              Back
            </Button>
            <Button onClick={() => setStep(5)}>Next: Rounds &amp; Schedule</Button>
          </div>
        </div>
      )}

      {/* STEP 5: Interactive Rounds & Timeline Builder */}
      {step === 5 && (
        <div className="form-stack">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
            <div>
              <h4 style={{ margin: 0, fontSize: '0.96rem' }}>Custom Rounds &amp; Milestones</h4>
              <small style={{ color: 'var(--muted)' }}>
                Customize round timelines, guidelines, and deliverables for participants.
              </small>
            </div>
            <Button variant="secondary" size="sm" onClick={handleAddRound}>
              + Add Round
            </Button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {rounds.map((rnd, idx) => (
              <div
                key={idx}
                style={{
                  background: 'var(--color-surface, #fff)',
                  border: '1px solid var(--color-border, #ddd)',
                  borderRadius: 8,
                  padding: 12,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 8,
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <b style={{ fontSize: '0.86rem', color: 'var(--accent, #e34d42)' }}>
                    Stage {idx + 1}
                  </b>
                  {rounds.length > 1 && (
                    <button
                      type="button"
                      onClick={() => handleRemoveRound(idx)}
                      style={{ background: 'none', border: 'none', color: '#dc2626', cursor: 'pointer', fontSize: '0.78rem' }}
                    >
                      Remove
                    </button>
                  )}
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                  <input
                    value={rnd.name}
                    onChange={(e) => handleUpdateRound(idx, 'name', e.target.value)}
                    placeholder="Round Name"
                    style={{ padding: '6px 8px', borderRadius: 4, border: '1px solid #ccc', fontSize: '0.84rem' }}
                  />
                  <input
                    value={rnd.date}
                    onChange={(e) => handleUpdateRound(idx, 'date', e.target.value)}
                    placeholder="Timeline / Date"
                    style={{ padding: '6px 8px', borderRadius: 4, border: '1px solid #ccc', fontSize: '0.84rem' }}
                  />
                </div>
                <input
                  value={rnd.description}
                  onChange={(e) => handleUpdateRound(idx, 'description', e.target.value)}
                  placeholder="Stage Description"
                  style={{ padding: '6px 8px', borderRadius: 4, border: '1px solid #ccc', fontSize: '0.84rem' }}
                />
                <input
                  value={rnd.deliverable}
                  onChange={(e) => handleUpdateRound(idx, 'deliverable', e.target.value)}
                  placeholder="Required Deliverable (e.g. GitHub Repo / Pitch Deck)"
                  style={{ padding: '6px 8px', borderRadius: 4, border: '1px solid #ccc', fontSize: '0.84rem' }}
                />
              </div>
            ))}
          </div>

          <div className="flow-actions" style={{ display: 'flex', justifyContent: 'space-between', marginTop: 14 }}>
            <Button variant="ghost" onClick={() => setStep(4)}>
              Back
            </Button>
            <Button onClick={() => setStep(6)}>Next: Review &amp; Publish</Button>
          </div>
        </div>
      )}

      {/* STEP 6: Review & Confirm */}
      {step === 6 && (
        <>
          <div
            className="confirm-box"
            style={{
              padding: 16,
              borderRadius: 10,
              background: 'var(--color-surface-hover, #f8f9fa)',
              border: '1px solid var(--color-border, #e5e7eb)',
              display: 'flex',
              flexDirection: 'column',
              gap: 10,
            }}
          >
            {/* Banner preview */}
            <div style={{ height: 100, borderRadius: 8, overflow: 'hidden', position: 'relative' }}>
              <img src={bannerImage} alt="" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
              <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.35)', display: 'flex', alignItems: 'center', padding: '0 16px', color: '#fff' }}>
                <b style={{ fontSize: '1.15rem' }}>{title || 'Untitled Gathering'}</b>
              </div>
            </div>

            <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' }}>
              <Tag tone="coral">{category}</Tag>
              <Tag tone="purple">{format}</Tag>
              {offerFreeTier && <Tag tone="green">Free Tier (Cap: {freeCapacity})</Tag>}
              {offerPaidTier && <Tag tone="gold">VIP ₹{paidTierPrice} (Cap: {paidCapacity})</Tag>}
            </div>
            {tagline && <p style={{ margin: 0, fontStyle: 'italic', color: 'var(--muted)' }}>"{tagline}"</p>}
            
            <div style={{ fontSize: '0.86rem', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
              <div>📅 <b>Schedule:</b> {date} to {endDate} ({time})</div>
              <div>📍 <b>Location:</b> {location}</div>
              <div>👥 <b>Capacity:</b> Up to {capacity} attendees</div>
              <div>🤝 <b>Team rules:</b> {allowTeamReg ? `${teamSizeMin}–${teamSizeMax} members per squad` : 'Individual entries only'}</div>
            </div>

            {prizes && (
              <div style={{ fontSize: '0.84rem', padding: 8, background: '#fff', borderRadius: 6, border: '1px solid #eee' }}>
                🏆 <b>Prize Pool:</b> {prizes}
              </div>
            )}

            <div style={{ fontSize: '0.84rem' }}>
              📑 <b>Custom Sections:</b> {customSections.length} sections defined ({customSections.map(s => s.title).join(', ')})
            </div>

            <div style={{ fontSize: '0.84rem' }}>
              🎯 <b>Stages:</b> {rounds.length} customized rounds configured
            </div>
          </div>

          <div className="flow-actions" style={{ display: 'flex', justifyContent: 'space-between', marginTop: 14 }}>
            <Button variant="ghost" onClick={() => setStep(5)}>
              Back
            </Button>
            <Button
              onClick={() =>
                onPublish({
                  title: title.trim(),
                  tagline: tagline.trim(),
                  category,
                  format,
                  date,
                  endDate,
                  time,
                  location,
                  venueAddress,
                  virtualLink,
                  description: description.trim(),
                  bannerImage,
                  image: bannerImage,
                  offerFreeTier,
                  freeCapacity,
                  freeDescription,
                  offerPaidTier,
                  paidTierPrice: offerPaidTier ? paidTierPrice : '0',
                  paidCapacity,
                  paidPerks,
                  ticketPrice: offerPaidTier ? paidTierPrice : '0',
                  capacity,
                  prizes: prizes.trim(),
                  prizePool: prizes.includes('₹') ? prizes.split('(')[0].trim() : '',
                  rules: rules.trim(),
                  customSections: customSections.filter(s => s.title.trim()),
                  rounds,
                })
              }
            >
              Publish event
            </Button>
          </div>
        </>
      )}
    </section>
  );
}

function Attendees({ app, eventId }) {
  const catalog = [...(app.eventCatalog || []), ...events];
  const [selectedEventId, setSelectedEventId] = useState(
    eventId || catalog[0]?.id || ''
  );
  const [attendees, setAttendees] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (selectedEventId && app?.fetchEventAttendees) {
      setLoading(true);
      app
        .fetchEventAttendees(selectedEventId)
        .then((list) => setAttendees(list || []))
        .catch(() => setAttendees([]))
        .finally(() => setLoading(false));
    }
  }, [selectedEventId, app]);

  return (
    <section className="person-list">
      <div className="event-selector-header">
        <label>
          <b>Event:</b>
        </label>
        <select
          value={selectedEventId}
          onChange={(e) => setSelectedEventId(e.target.value)}
        >
          {catalog.map((ev) => (
            <option key={ev.id} value={ev.id}>
              {ev.title}
            </option>
          ))}
        </select>
      </div>

      {loading ? (
        <p className="muted">Loading attendee roster…</p>
      ) : attendees.length > 0 ? (
        attendees.map((att, i) => (
          <article key={att.ticketId || att.id || i}>
            <Avatar name={att.displayName || 'Attendee'} />
            <div>
              <b>{att.displayName || 'Attendee'}</b>
              <small>Ticket: {att.ticketId || att.id}</small>
            </div>
            <Status>{att.checkedIn ? 'Checked in' : 'Registered'}</Status>
          </article>
        ))
      ) : (
        people.concat(people.slice(0, 3)).map((p, i) => (
          <article key={`${p.id}${i}`}>
            <Avatar person={p} />
            <div>
              <b>{p.name}</b>
              <small>{p.role} · Ticket #CLB-2026-00{i + 1}</small>
            </div>
            <Status tone={i % 2 === 0 ? 'positive' : 'neutral'}>
              {i % 2 === 0 ? 'Checked in' : 'Registered'}
            </Status>
          </article>
        ))
      )}

      <Button
        variant="ghost"
        onClick={() => app?.toast('Attendee list exported to CSV')}
      >
        Export attendees to CSV
      </Button>
    </section>
  );
}

function CheckIn({ app, eventId }) {
  const catalog = [...(app.eventCatalog || []), ...events];
  const [selectedEventId, setSelectedEventId] = useState(
    eventId || catalog[0]?.id || ''
  );
  const [ticketInput, setTicketInput] = useState('');
  const [attendees, setAttendees] = useState([]);
  const [loading, setLoading] = useState(false);
  const [statusMessage, setStatusMessage] = useState('');
  const [isError, setIsError] = useState(false);
  const [isCameraActive, setIsCameraActive] = useState(true);

  useEffect(() => {
    if (selectedEventId && app?.fetchEventAttendees) {
      setLoading(true);
      app
        .fetchEventAttendees(selectedEventId)
        .then((list) => setAttendees(list || []))
        .catch(() => setAttendees([]))
        .finally(() => setLoading(false));
    }
  }, [selectedEventId, app]);

  const handleCheckIn = async (ticketId) => {
    const cleanId = (ticketId || ticketInput).trim();
    if (!cleanId) return;
    setIsError(false);
    setStatusMessage('');
    try {
      if (app?.checkInEventTicket) {
        await app.checkInEventTicket(selectedEventId, cleanId);
      }
      setStatusMessage(
        `✓ Ticket [${cleanId}] verified and checked in successfully at ${new Date().toLocaleTimeString()}!`
      );
      setTicketInput('');
      setAttendees((old) =>
        old.map((a) =>
          a.ticketId === cleanId || a.id === cleanId
            ? {
                ...a,
                checkedIn: true,
                checkedInAt: new Date().toISOString(),
              }
            : a
        )
      );
      app.toast('Ticket checked in successfully!');
    } catch (err) {
      setIsError(true);
      setStatusMessage(
        `✗ ${err.message || 'Check-in failed. Please verify the ticket ID.'}`
      );
    }
  };

  const handleSimulateScan = () => {
    const sampleIds = [
      'PASS-AI-HACK-001',
      'PASS-NEXTDEV-042',
      'PASS-FLUTTER-789',
      'PASS-VIP-992',
    ];
    const picked = sampleIds[Math.floor(Math.random() * sampleIds.length)];
    handleCheckIn(picked);
  };

  return (
    <section className="checkin-panel">
      <div className="event-selector-header">
        <label>
          <b>Active Event:</b>
        </label>
        <select
          value={selectedEventId}
          onChange={(e) => setSelectedEventId(e.target.value)}
        >
          {catalog.map((ev) => (
            <option key={ev.id} value={ev.id}>
              {ev.title}
            </option>
          ))}
        </select>
      </div>

      {/* QR Scanner Viewfinder Simulation */}
      <div className="qr-viewfinder-container">
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: '#94a3b8' }}>
          <Camera size={18} />
          <span>Live Entrance Camera Scanner</span>
          <span style={{ color: '#22c55e', fontWeight: 700 }}>● Active</span>
        </div>

        <div className={`qr-viewfinder-box ${isCameraActive ? 'scanning' : ''}`}>
          <div className="scanner-corner tl" />
          <div className="scanner-corner tr" />
          <div className="scanner-corner bl" />
          <div className="scanner-corner br" />
          <QrCode size={100} style={{ opacity: 0.25 }} />
        </div>

        <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
          <Button
            variant="primary"
            icon={QrCode}
            onClick={handleSimulateScan}
          >
            Simulate Camera QR Scan
          </Button>
          <Button
            variant="ghost"
            onClick={() => setIsCameraActive(!isCameraActive)}
          >
            {isCameraActive ? 'Pause Viewfinder' : 'Resume Viewfinder'}
          </Button>
        </div>

        <p style={{ margin: '12px 0 0', fontSize: 12, color: '#94a3b8' }}>
          Align visitor ticket QR pass within the viewfinder for auto-verification.
        </p>
      </div>

      <div className="scanner-box" style={{ marginTop: 16 }}>
        <Ticket size={32} />
        <h3>Manual Ticket ID Entry</h3>
        <p>Or enter ticket reference code if camera is unavailable.</p>
        <div className="ticket-input-row">
          <input
            value={ticketInput}
            onChange={(e) => setTicketInput(e.target.value)}
            placeholder="Enter Ticket ID (e.g. PASS-AI-HACK-001)"
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleCheckIn(ticketInput);
            }}
          />
          <Button
            variant="primary"
            onClick={() => handleCheckIn(ticketInput)}
            disabled={!ticketInput.trim()}
          >
            Validate &amp; Check In
          </Button>
        </div>
        {statusMessage && (
          <div
            className={`checkin-status-banner ${isError ? 'error' : 'success'}`}
          >
            {statusMessage}
          </div>
        )}
      </div>

      <SectionTitle title="Attendee Roster &amp; Live Verification Status" />
      <div className="attendee-checkin-table">
        {people.concat(people.slice(0, 3)).map((att, i) => {
          const isChecked = i % 2 === 0;
          const tId = `PASS-00${i + 1}`;
          return (
            <div key={`${att.id}-${i}`} className="attendee-checkin-row">
              <Avatar person={att} />
              <div className="attendee-info">
                <b>{att.name}</b>
                <small>Ticket: {tId}</small>
              </div>
              {isChecked ? (
                <div className="checked-badge">
                  <CheckCircle2 size={16} />
                  <span>Checked In</span>
                </div>
              ) : (
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => handleCheckIn(tId)}
                >
                  Check in
                </Button>
              )}
            </div>
          );
        })}
      </div>
    </section>
  );
}

function BroadcastReminder({ app, eventId }) {
  const catalog = [...(app.eventCatalog || []), ...events];
  const [selectedEventId, setSelectedEventId] = useState(
    eventId || catalog[0]?.id || ''
  );
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [sending, setSending] = useState(false);
  const [statusResult, setStatusResult] = useState(null);

  const handleBroadcast = async (e) => {
    e.preventDefault();
    if (!title.trim() || !message.trim()) return;
    setSending(true);
    setStatusResult(null);
    try {
      if (app.broadcastEventReminder) {
        await app.broadcastEventReminder(selectedEventId, {
          title: title.trim(),
          message: message.trim(),
        });
      }
      setStatusResult({ success: true, count: 124 });
      setTitle('');
      setMessage('');
      app.toast('Broadcast announcement sent to all attendees!');
    } catch (err) {
      setStatusResult({
        success: false,
        error: err.message || 'Failed to broadcast reminder.',
      });
    } finally {
      setSending(false);
    }
  };

  return (
    <section className="broadcast-panel content-editor">
      <div className="broadcast-hero">
        <Bell size={28} />
        <div>
          <h3>Broadcast Event Announcement</h3>
          <p>
            Send instant alerts, room links, schedule shifts, or parking updates
            to confirmed ticket holders.
          </p>
        </div>
      </div>

      <form onSubmit={handleBroadcast} className="form-stack">
        <Field label="Select Event">
          <select
            value={selectedEventId}
            onChange={(e) => setSelectedEventId(e.target.value)}
          >
            {catalog.map((ev) => (
              <option key={ev.id} value={ev.id}>
                {ev.title}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Announcement Title">
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Doors open in 30 minutes / Main Stage Link Updated"
            required
          />
        </Field>
        <Field label="Message">
          <textarea
            rows="4"
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            placeholder="Provide room access links, speaker notes, or schedule shifts..."
            required
          />
        </Field>
        {statusResult && statusResult.success && (
          <div className="checkin-status-banner success">
            ✓ Broadcast delivered successfully to {statusResult.count} registered
            attendees.
          </div>
        )}
        {statusResult && !statusResult.success && (
          <div className="checkin-status-banner error">
            ✗ {statusResult.error}
          </div>
        )}
        <Button
          type="submit"
          variant="primary"
          disabled={sending || !title.trim() || !message.trim()}
        >
          {sending ? 'Broadcasting Announcement…' : 'Send Broadcast Announcement'}
        </Button>
      </form>
    </section>
  );
}

function EventAnalytics() {
  return (
    <>
      <div className="metric-grid operator-metrics">
        <Metric value="1.8k" label="Page Views" />
        <Metric value="620" label="Registrations" />
        <Metric value="524" label="Checked in" />
        <Metric value="84.5%" label="Check-in rate" />
      </div>

      <div className="operator-columns" style={{ marginTop: 16 }}>
        <div className="chart-card">
          <SectionTitle title="Registration Funnel" />
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, padding: 8 }}>
            {[
              ['Impressions', 2400, '100%'],
              ['Page Views', 1840, '76%'],
              ['Ticket Clicks', 920, '38%'],
              ['Confirmed Registrations', 620, '25%'],
              ['Checked In At Entrance', 524, '21%'],
            ].map(([stage, count, pct]) => (
              <div key={stage}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 2 }}>
                  <b>{stage}</b>
                  <span>{count} ({pct})</span>
                </div>
                <Progress value={parseInt(pct)} />
              </div>
            ))}
          </div>
        </div>

        <div className="chart-card">
          <SectionTitle title="Attendee Role Breakdown" />
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, padding: 8 }}>
            {[
              ['Software Engineers & Devs', 48],
              ['Product Designers & UI/UX', 22],
              ['AI / ML Researchers', 16],
              ['Founders & Product Leads', 14],
            ].map(([role, share]) => (
              <div key={role}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginBottom: 2 }}>
                  <span>{role}</span>
                  <b>{share}%</b>
                </div>
                <Progress value={share} />
              </div>
            ))}
          </div>
        </div>
      </div>
    </>
  );
}

function Reports() {
  return (
    <section className="operator-list">
      {[
        'Attendance & Check-in Roster Report (CSV)',
        'Registration & Demographics Report (PDF)',
        'Attendee Feedback & CSAT Analysis Report (PDF)',
        'Financial Revenue & Ticket Sales Audit (CSV)',
        'Participation Certificate Export (Batch Zip)',
      ].map((x) => (
        <article key={x}>
          <span className="module-icon blue">
            <FileText size={18} />
          </span>
          <div>
            <b>{x}</b>
            <small>Download comprehensive report data</small>
          </div>
          <Button variant="ghost">Export</Button>
        </article>
      ))}
    </section>
  );
}

function EventSettings({ app, event, onUpdateEvent }) {
  const [visibility, setVisibility] = useState(event?.visibility || 'Public');
  const [approvalPolicy, setApprovalPolicy] = useState(event?.approvalPolicy || 'Open');
  const [maxCapacity, setMaxCapacity] = useState(event?.capacity || 250);
  const [supportEmail, setSupportEmail] = useState(
    event?.supportEmail || app?.profile?.email || 'events@collobster.app'
  );

  // Registration Deadline Extension
  const [regDeadline, setRegDeadline] = useState(event?.regDeadline || event?.date || '2026-10-25');
  const [regStatus, setRegStatus] = useState(event?.regStatus || (event?.isCancelled ? 'Closed' : 'Open'));

  // Rounds & Milestones Timeline Management
  const [rounds, setRounds] = useState(
    event?.rounds && Array.isArray(event.rounds) && event.rounds.length > 0
      ? event.rounds
      : [
          {
            name: 'Round 1: Idea Pitch & Problem Statement',
            date: 'Day 1 · Oct 25 (10:00 AM)',
            description: 'Submit your solution concept, target users, and architectural stack diagram.',
            deliverable: 'Pitch Deck PDF or Notion Link',
            status: 'Upcoming',
          },
          {
            name: 'Round 2: Mid-Sprint Prototype & Mentor Review',
            date: 'Day 2 · Oct 26 (02:00 PM)',
            description: 'Hands-on live codebase review with assigned industry mentors.',
            deliverable: 'Public GitHub Repo & Working Preview',
            status: 'Upcoming',
          },
          {
            name: 'Round 3: Grand Finale & Live Jury Showcase',
            date: 'Day 3 · Oct 27 (04:00 PM)',
            description: 'Top 10 finalists present a 4-minute demo pitch to the venture panel.',
            deliverable: 'Live Demo & Production Deployment',
            status: 'Upcoming',
          },
        ]
  );

  // Cancellation State & Controls
  const [isCancelled, setIsCancelled] = useState(Boolean(event?.isCancelled || event?.status === 'Cancelled'));
  const [cancellationReason, setCancellationReason] = useState(
    event?.cancellationReason || event?.cancelReason || 'Schedule alteration and venue maintenance.'
  );
  const [notifyAttendees, setNotifyAttendees] = useState(true);
  const [showCancelPrompt, setShowCancelPrompt] = useState(false);

  const [requireCheckIn, setRequireCheckIn] = useState(event?.requireCheckIn !== false);
  const [allowTeamReg, setAllowTeamReg] = useState(event?.allowTeamReg !== false);
  const [sendReminders, setSendReminders] = useState(event?.sendReminders !== false);
  const [enableFeedback, setEnableFeedback] = useState(event?.enableFeedback !== false);
  const [customWelcomeNote, setCustomWelcomeNote] = useState(
    event?.customWelcomeNote || 'Welcome to the event! Please bring a photo ID and have your Collobster QR pass ready at entrance.'
  );

  const [saving, setSaving] = useState(false);
  const [lastSaved, setLastSaved] = useState(null);

  // Helper to extend registration deadline by N days
  const handleExtendRegDeadline = (daysToAdd) => {
    try {
      const baseDate = new Date(regDeadline && !isNaN(Date.parse(regDeadline)) ? regDeadline : Date.now());
      baseDate.setDate(baseDate.getDate() + daysToAdd);
      const newDateStr = baseDate.toISOString().split('T')[0];
      setRegDeadline(newDateStr);
      setRegStatus('Open');
      if (app?.toast) {
        app.toast(`Registration deadline extended by ${daysToAdd} days to ${newDateStr}`);
      }
    } catch {
      // fallback
    }
  };

  // Helper to update a specific round
  const handleUpdateRound = (idx, field, value) => {
    setRounds((prev) => {
      const copy = [...prev];
      copy[idx] = { ...copy[idx], [field]: value };
      return copy;
    });
  };

  // Helper to quickly extend a round's timeline
  const handleExtendRoundDate = (idx, daysToAdd) => {
    const rnd = rounds[idx];
    const newDateText = `${rnd.date} (+${daysToAdd}d extension)`;
    handleUpdateRound(idx, 'date', newDateText);
    if (app?.toast) {
      app.toast(`Extended Stage ${idx + 1} deadline by ${daysToAdd} days.`);
    }
  };

  const handleAddRound = () => {
    setRounds((prev) => [
      ...prev,
      {
        name: `Round ${prev.length + 1}: Checkpoint Review`,
        date: 'Scheduled date / time',
        description: 'Provide guidelines for milestone review and deliverables.',
        deliverable: 'GitHub Repo or Presentation',
        status: 'Upcoming',
      },
    ]);
  };

  const handleRemoveRound = (idx) => {
    setRounds((prev) => prev.filter((_, i) => i !== idx));
  };

  // Handle Event Cancellation
  const handleConfirmCancelEvent = () => {
    setIsCancelled(true);
    setRegStatus('Closed');
    setShowCancelPrompt(false);

    const updatedEvent = {
      ...event,
      isCancelled: true,
      status: 'Cancelled',
      cancelReason: cancellationReason,
      cancellationReason,
      regStatus: 'Closed',
    };

    if (onUpdateEvent) {
      onUpdateEvent(updatedEvent);
    }

    if (app?.toast) {
      app.toast('Event cancelled. Registrations halted and attendees notified.');
    }
    if (app?.notify && notifyAttendees) {
      app.notify(
        'Event Cancelled',
        `Important: "${event.title}" has been cancelled. Reason: ${cancellationReason}`,
        'Events'
      );
    }
  };

  // Handle Event Restoration / Reactivation
  const handleRestoreEvent = () => {
    setIsCancelled(false);
    setRegStatus('Open');

    const updatedEvent = {
      ...event,
      isCancelled: false,
      status: 'Upcoming',
      cancelReason: '',
      cancellationReason: '',
      regStatus: 'Open',
    };

    if (onUpdateEvent) {
      onUpdateEvent(updatedEvent);
    }

    if (app?.toast) {
      app.toast('Event restored and reactivated successfully! Registrations re-opened.');
    }
    if (app?.notify) {
      app.notify(
        'Event Reactivated',
        `"${event.title}" is active again and registrations are open.`,
        'Events'
      );
    }
  };

  const handleSave = async (e) => {
    if (e) e.preventDefault();
    setSaving(true);
    try {
      await new Promise((resolve) => setTimeout(resolve, 350));
      const updatedEvent = {
        ...event,
        visibility,
        approvalPolicy,
        capacity: Number(maxCapacity),
        supportEmail,
        regDeadline,
        regStatus,
        rounds,
        isCancelled,
        status: isCancelled ? 'Cancelled' : 'Upcoming',
        cancellationReason,
        cancelReason: cancellationReason,
        requireCheckIn,
        allowTeamReg,
        sendReminders,
        enableFeedback,
        customWelcomeNote,
      };

      if (onUpdateEvent) {
        onUpdateEvent(updatedEvent);
      }
      const timeStr = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
      setLastSaved(timeStr);
      if (app?.toast) {
        app.toast('All event settings, deadlines and schedules updated successfully!');
      }
    } catch {
      if (app?.toast) {
        app.toast('Failed to save settings. Please try again.');
      }
    } finally {
      setSaving(false);
    }
  };

  return (
    <section className="content-editor" style={{ maxWidth: 680 }}>
      {lastSaved && (
        <div
          style={{
            padding: '10px 14px',
            marginBottom: 16,
            borderRadius: 8,
            background: '#ecfdf5',
            border: '1px solid #a7f3d0',
            color: '#065f46',
            fontSize: '0.85rem',
            display: 'flex',
            alignItems: 'center',
            gap: 8,
          }}
        >
          <CheckCircle2 size={16} color="#059669" />
          <span>All settings saved successfully at <b>{lastSaved}</b>.</span>
        </div>
      )}

      {/* Cancellation Warning Banner if currently Cancelled */}
      {isCancelled && (
        <div
          style={{
            padding: '14px 16px',
            marginBottom: 16,
            borderRadius: 10,
            background: '#fef2f2',
            border: '2px solid #ef4444',
            color: '#991b1b',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <X size={18} color="#dc2626" />
              <strong style={{ fontSize: '0.96rem' }}>THIS EVENT IS CURRENTLY CANCELLED</strong>
            </div>
            <p style={{ margin: '4px 0 0', fontSize: '0.82rem', color: '#b91c1c' }}>
              Reason: <i>"{cancellationReason}"</i>. Registrations are suspended across Collobster.
            </p>
          </div>
          <Button variant="primary" size="sm" onClick={handleRestoreEvent}>
            Restore &amp; Reopen Event
          </Button>
        </div>
      )}

      <form onSubmit={handleSave} className="form-stack">
        {/* SECTION 1: Registration Deadline & Access Controls */}
        <div
          style={{
            background: '#fff',
            padding: 14,
            borderRadius: 10,
            border: '1px solid #e2e8f0',
            display: 'flex',
            flexDirection: 'column',
            gap: 10,
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--color-heading, #111)' }}>
              📅 Registration Deadline &amp; Status
            </span>
            <Tag tone={regStatus === 'Open' ? 'green' : 'red'}>
              {regStatus === 'Open' ? 'Registrations Open' : 'Registrations Closed'}
            </Tag>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            <Field label="Registration Cut-off Date *">
              <input
                type="date"
                value={regDeadline}
                onChange={(e) => setRegDeadline(e.target.value)}
              />
            </Field>
            <Field label="Registration Access Status">
              <select value={regStatus} onChange={(e) => setRegStatus(e.target.value)}>
                <option value="Open">Open · Accepting new attendees</option>
                <option value="Closed">Closed · Pause all registrations</option>
              </select>
            </Field>
          </div>

          <div>
            <span style={{ fontSize: '0.78rem', color: 'var(--color-muted, #71767b)', display: 'block', marginBottom: 6 }}>
              Quick extend registration deadline:
            </span>
            <div style={{ display: 'flex', gap: 8 }}>
              <Button type="button" variant="secondary" size="sm" onClick={() => handleExtendRegDeadline(3)}>
                +3 Days
              </Button>
              <Button type="button" variant="secondary" size="sm" onClick={() => handleExtendRegDeadline(7)}>
                +7 Days (1 Week)
              </Button>
              <Button type="button" variant="secondary" size="sm" onClick={() => handleExtendRegDeadline(14)}>
                +14 Days (2 Weeks)
              </Button>
            </div>
          </div>
        </div>

        {/* SECTION 2: Extend Rounds & Milestone Timelines */}
        <div
          style={{
            background: '#fff',
            padding: 14,
            borderRadius: 10,
            border: '1px solid #e2e8f0',
            display: 'flex',
            flexDirection: 'column',
            gap: 12,
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <span style={{ fontSize: '0.9rem', fontWeight: 700, color: 'var(--color-heading, #111)', display: 'block' }}>
                🎯 Extend Rounds &amp; Milestones Schedule
              </span>
              <small style={{ color: 'var(--color-muted, #71767b)' }}>
                Adjust timelines, extend submission deadlines, or add sprint rounds.
              </small>
            </div>
            <Button type="button" variant="secondary" size="sm" onClick={handleAddRound}>
              + Add Round
            </Button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {rounds.map((rnd, idx) => (
              <div
                key={idx}
                style={{
                  background: 'var(--color-surface-hover, #f8f9fa)',
                  border: '1px solid #e2e8f0',
                  borderRadius: 8,
                  padding: 10,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 6,
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <b style={{ fontSize: '0.84rem', color: 'var(--accent, #e34d42)' }}>
                    Round {idx + 1}
                  </b>
                  <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
                    <button
                      type="button"
                      onClick={() => handleExtendRoundDate(idx, 2)}
                      style={{
                        padding: '2px 8px',
                        borderRadius: 4,
                        border: '1px solid #cbd5e1',
                        background: '#fff',
                        fontSize: '0.74rem',
                        cursor: 'pointer',
                        fontWeight: 600,
                      }}
                    >
                      +2 Days
                    </button>
                    <button
                      type="button"
                      onClick={() => handleExtendRoundDate(idx, 7)}
                      style={{
                        padding: '2px 8px',
                        borderRadius: 4,
                        border: '1px solid #cbd5e1',
                        background: '#fff',
                        fontSize: '0.74rem',
                        cursor: 'pointer',
                        fontWeight: 600,
                      }}
                    >
                      +1 Week
                    </button>
                    {rounds.length > 1 && (
                      <button
                        type="button"
                        onClick={() => handleRemoveRound(idx)}
                        style={{
                          background: 'none',
                          border: 'none',
                          color: '#dc2626',
                          cursor: 'pointer',
                          fontSize: '0.74rem',
                        }}
                      >
                        Remove
                      </button>
                    )}
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: 8 }}>
                  <input
                    value={rnd.name}
                    onChange={(e) => handleUpdateRound(idx, 'name', e.target.value)}
                    placeholder="Round Name"
                    style={{ padding: '6px 8px', borderRadius: 4, border: '1px solid #ccc', fontSize: '0.82rem' }}
                  />
                  <input
                    value={rnd.date}
                    onChange={(e) => handleUpdateRound(idx, 'date', e.target.value)}
                    placeholder="Timeline / Deadline"
                    style={{ padding: '6px 8px', borderRadius: 4, border: '1px solid #ccc', fontSize: '0.82rem', fontWeight: 600 }}
                  />
                </div>
                <input
                  value={rnd.deliverable || ''}
                  onChange={(e) => handleUpdateRound(idx, 'deliverable', e.target.value)}
                  placeholder="Required Deliverable (e.g. GitHub Repo / Demo Link)"
                  style={{ padding: '6px 8px', borderRadius: 4, border: '1px solid #ccc', fontSize: '0.8rem' }}
                />
              </div>
            ))}
          </div>
        </div>

        {/* SECTION 3: General Event Policies */}
        <Field label="Event visibility">
          <select value={visibility} onChange={(e) => setVisibility(e.target.value)}>
            <option value="Public">Public · Listed in Collobster Discover</option>
            <option value="Private">Private · Unlisted (Invite link only)</option>
          </select>
        </Field>

        <Field label="Registration Approval Policy">
          <select value={approvalPolicy} onChange={(e) => setApprovalPolicy(e.target.value)}>
            <option value="Open">Open registration (Instant ticket issue)</option>
            <option value="Manual">Manual approval required by organizers</option>
          </select>
        </Field>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
          <Field label="Maximum Capacity (Seats limit)">
            <input
              type="number"
              min="10"
              value={maxCapacity}
              onChange={(e) => setMaxCapacity(e.target.value)}
              placeholder="e.g. 250"
            />
          </Field>
          <Field label="Organizer Support Email">
            <input
              type="email"
              value={supportEmail}
              onChange={(e) => setSupportEmail(e.target.value)}
              placeholder="support@event.com"
            />
          </Field>
        </div>

        <Field label="Custom Welcome / Check-in Instruction (Sent on pass)">
          <textarea
            rows="2"
            value={customWelcomeNote}
            onChange={(e) => setCustomWelcomeNote(e.target.value)}
            placeholder="Instructions shown on attendee pass..."
          />
        </Field>

        <div style={{ background: 'var(--color-surface-hover, #f8f9fa)', padding: 14, borderRadius: 10, border: '1px solid var(--color-border, #e5e7eb)', display: 'flex', flexDirection: 'column', gap: 10, marginTop: 4 }}>
          <span style={{ fontSize: '0.84rem', fontWeight: 600, color: 'var(--color-heading, #222)' }}>
            Entry &amp; Automation Controls
          </span>

          <label style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: '0.84rem', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={requireCheckIn}
              onChange={(e) => setRequireCheckIn(e.target.checked)}
              style={{ width: 16, height: 16, cursor: 'pointer' }}
            />
            <span>Require QR check-in verification for venue entry</span>
          </label>

          <label style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: '0.84rem', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={allowTeamReg}
              onChange={(e) => setAllowTeamReg(e.target.checked)}
              style={{ width: 16, height: 16, cursor: 'pointer' }}
            />
            <span>Allow team registrations (2–4 members per squad)</span>
          </label>

          <label style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: '0.84rem', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={sendReminders}
              onChange={(e) => setSendReminders(e.target.checked)}
              style={{ width: 16, height: 16, cursor: 'pointer' }}
            />
            <span>Send automated email reminders 24h &amp; 1h before start</span>
          </label>

          <label style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: '0.84rem', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={enableFeedback}
              onChange={(e) => setEnableFeedback(e.target.checked)}
              style={{ width: 16, height: 16, cursor: 'pointer' }}
            />
            <span>Enable attendee post-event feedback survey</span>
          </label>
        </div>

        <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginTop: 12 }}>
          <Button type="submit" disabled={saving}>
            {saving ? 'Saving settings...' : 'Save event settings'}
          </Button>
          {lastSaved && (
            <span style={{ fontSize: '0.8rem', color: '#059669', display: 'inline-flex', alignItems: 'center', gap: 4 }}>
              <Check size={14} /> Saved
            </span>
          )}
        </div>

        {/* SECTION 4: Danger Zone - Cancellation Option for Event */}
        <div
          style={{
            marginTop: 24,
            padding: 16,
            borderRadius: 10,
            border: '1.5px solid #fca5a5',
            background: '#fff5f5',
            display: 'flex',
            flexDirection: 'column',
            gap: 10,
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <b style={{ color: '#dc2626', fontSize: '0.94rem', display: 'block' }}>
                Danger Zone: Event Cancellation
              </b>
              <small style={{ color: '#7f1d1d' }}>
                Cancel this event in case of emergencies, venue constraints, or rescheduled dates.
              </small>
            </div>
            {!isCancelled && !showCancelPrompt && (
              <button
                type="button"
                onClick={() => setShowCancelPrompt(true)}
                style={{
                  padding: '7px 14px',
                  borderRadius: 6,
                  border: '1px solid #dc2626',
                  background: '#dc2626',
                  color: '#fff',
                  fontSize: '0.82rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                }}
              >
                Cancel Event
              </button>
            )}
          </div>

          {showCancelPrompt && !isCancelled && (
            <div
              style={{
                marginTop: 8,
                padding: 12,
                borderRadius: 8,
                background: '#fff',
                border: '1px solid #f87171',
                display: 'flex',
                flexDirection: 'column',
                gap: 10,
              }}
            >
              <span style={{ fontSize: '0.86rem', fontWeight: 600, color: '#991b1b' }}>
                Reason for Cancellation (Displayed to attendees):
              </span>
              <input
                value={cancellationReason}
                onChange={(e) => setCancellationReason(e.target.value)}
                placeholder="e.g. Unforeseen venue maintenance and speaker scheduling conflict"
                style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid #ccc', fontSize: '0.84rem' }}
              />

              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                {[
                  'Venue emergency & maintenance',
                  'Speaker / Mentor rescheduling',
                  'Inclement weather advisory',
                  'Postponed to next season',
                ].map((preset) => (
                  <button
                    key={preset}
                    type="button"
                    onClick={() => setCancellationReason(preset)}
                    style={{
                      padding: '3px 8px',
                      borderRadius: 12,
                      border: '1px solid #fca5a5',
                      background: '#fef2f2',
                      fontSize: '0.74rem',
                      cursor: 'pointer',
                      color: '#991b1b',
                    }}
                  >
                    {preset}
                  </button>
                ))}
              </div>

              <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: '0.82rem', cursor: 'pointer', color: '#7f1d1d' }}>
                <input
                  type="checkbox"
                  checked={notifyAttendees}
                  onChange={(e) => setNotifyAttendees(e.target.checked)}
                />
                <span>Broadcast cancellation alert immediately to all registered attendees</span>
              </label>

              <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
                <button
                  type="button"
                  onClick={handleConfirmCancelEvent}
                  style={{
                    padding: '8px 16px',
                    borderRadius: 6,
                    border: 'none',
                    background: '#dc2626',
                    color: '#fff',
                    fontWeight: 700,
                    fontSize: '0.84rem',
                    cursor: 'pointer',
                  }}
                >
                  Yes, Confirm Cancellation
                </button>
                <button
                  type="button"
                  onClick={() => setShowCancelPrompt(false)}
                  style={{
                    padding: '8px 14px',
                    borderRadius: 6,
                    border: '1px solid #ccc',
                    background: '#fff',
                    fontSize: '0.84rem',
                    cursor: 'pointer',
                  }}
                >
                  Nevermind, Keep Event Active
                </button>
              </div>
            </div>
          )}
        </div>
      </form>
    </section>
  );
}

// ─── 9. Community Space (Exported) ──────────────────────────────────────────

export function Community() {
  const a = useApp();
  const [tab, setTab] = useState('Discover');
  const [selected, setSelected] = useState(null);
  const [query, setQuery] = useState('');
  const [catFilter, setCatFilter] = useState('All');

  const catalog = useMemo(() => {
    const deleted = a.deletedCommunityIds || [];
    return [
      ...(a.communityCatalog || []),
      ...communities.filter(
        (c) => !(a.communityCatalog || []).some((remote) => remote.id === c.id)
      ),
    ].filter((c) => !deleted.includes(c.id));
  }, [a.communityCatalog, a.deletedCommunityIds]);

  const list = catalog.filter((c) => {
    const matchesCat =
      catFilter === 'All' || c.category === catFilter;
    const matchesQuery = `${c.title} ${c.category} ${c.tags?.join(' ') || ''}`
      .toLowerCase()
      .includes(query.toLowerCase());
    return matchesCat && matchesQuery;
  });

  const myCommunities = catalog.filter((c) => a.memberships[c.id] === 'member');

  if (selected) {
    return (
      <CommunityWorkspace
        app={a}
        community={selected}
        onBack={() => setSelected(null)}
      />
    );
  }

  return (
    <main className="operator-space">
      <PageHeader
        eyebrow="COMMUNITY"
        title="Find a corner of the internet that feels like home."
        description="Communities for questions, work, channels, and the people you want more of."
      >
        <Button
          icon={Plus}
          onClick={() =>
            a.setModal({ type: 'details', kind: 'community-create' })
          }
        >
          Create community
        </Button>
      </PageHeader>

      <div className="operator-layout" style={{ gridTemplateColumns: '1fr' }}>
        <section>
          <Tabs
            items={['Discover', 'My communities']}
            value={tab}
            onChange={setTab}
          />

          {tab === 'Discover' && (
            <>
              <SearchField
                value={query}
                onChange={setQuery}
                placeholder="Search communities, topics, or craft..."
              />

              {/* Fast filter chips directly under Discover */}
              <div className="fast-filter-chips">
                {['All', 'Development', 'Design', 'Startup', 'AI / ML', 'Career', 'Local'].map(
                  (cat) => (
                    <button
                      key={cat}
                      className={`filter-chip ${
                        catFilter === cat ? 'active' : ''
                      }`}
                      onClick={() => setCatFilter(cat)}
                    >
                      {cat}
                    </button>
                  )
                )}
              </div>

              <div className="card-grid">
                {list.map((c) => {
                  const state =
                    a.memberships[c.id] ||
                    joinState(
                      c.policy,
                      a.profile.roles.includes('community_member'),
                      a.profile.roles.includes('verified')
                    );
                  return (
                    <article className="community-card" key={c.id}>
                      <span className={`module-icon ${c.color || 'blue'}`}>
                        <Users size={22} />
                      </span>
                      <h3>{c.title}</h3>
                      <p>{c.description}</p>
                      <div className="tag-cloud">
                        {(c.tags || []).map((t) => (
                          <Tag key={t}>{t}</Tag>
                        ))}
                      </div>
                      <small>
                        {c.members || 1} members · {c.policy || 'Open'} · {c.category}
                      </small>
                      <Button
                        onClick={() =>
                          state === 'member'
                            ? setSelected(c)
                            : a.setModal({
                                type: 'details',
                                kind: 'community',
                                item: c,
                              })
                        }
                      >
                        {state === 'member'
                          ? 'Open community'
                          : state === 'pending'
                          ? 'Request sent'
                          : 'View community'}
                      </Button>
                    </article>
                  );
                })}
              </div>
            </>
          )}

          {tab === 'My communities' && (
            <div className="card-grid">
              {myCommunities.length > 0 ? (
                myCommunities.map((c) => {
                  const role = c.role || c.userRole || 'Member';
                  return (
                    <article className="community-card" key={c.id}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
                        <span className={`module-icon ${c.color || 'blue'}`}>
                          <Users size={22} />
                        </span>
                        <span
                          className={`community-role-badge ${
                            role === 'Admin'
                              ? 'role-badge-admin'
                              : role === 'Sub-admin'
                              ? 'role-badge-subadmin'
                              : 'role-badge-member'
                          }`}
                        >
                          {role}
                        </span>
                      </div>
                      <h3>{c.title}</h3>
                      <p>{c.description}</p>
                      <div className="tag-cloud">
                        {(c.tags || [c.category]).map((t) => (
                          <Tag key={t}>{t}</Tag>
                        ))}
                      </div>
                      <small>
                        {c.members || 1} members · {c.policy || 'Open'} · {c.category}
                      </small>
                      <Button onClick={() => setSelected(c)}>
                        Open community
                      </Button>
                    </article>
                  );
                })
              ) : (
                <Empty
                  title="No joined communities yet"
                  text="Explore Discover to join communities matching your craft."
                  action="Explore communities"
                  onAction={() => setTab('Discover')}
                />
              )}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}

function CommunityWorkspace({ app, community, onBack }) {
  const [view, setView] = useState('Chat');
  const [chatSubView, setChatSubView] = useState('stream'); // 'stream' | 'resources'
  const [activeChannel, setActiveChannel] = useState('general');
  const [mobileChannelsOpen, setMobileChannelsOpen] = useState(false);

  // Community-wide settings state
  const [communitySettings, setCommunitySettings] = useState({
    title: community.title || 'Community',
    description: community.description || '',
    privacy: community.privacy || community.policy || 'Public',
    needsApproval:
      community.needsApproval !== undefined
        ? Boolean(community.needsApproval)
        : (community.policy === 'Approval required' ||
           community.policy === 'Application' ||
           community.privacy === 'Private' ||
           true),
    allowSubadminsApproveRequests:
      community.allowSubadminsApproveRequests !== undefined
        ? Boolean(community.allowSubadminsApproveRequests)
        : true,
    roomCreationPermission: community.roomCreationPermission || 'subadmins', // 'all' | 'subadmins' | 'admins'
    postingPermission: community.postingPermission || 'all',
    allowInvites: community.allowInvites !== false,
    allowMemberPolls: community.allowMemberPolls !== false,
    guidelines:
      community.guidelines ||
      'Be respectful, share constructive feedback, and collaborate honestly with your fellow members.',
  });

  // Pending membership join requests
  const [joinRequests, setJoinRequests] = useState(
    community.joinRequests || [
      {
        id: 'req-1',
        name: 'Rohan Joshi',
        title: 'Fullstack Engineer · React & Node.js',
        email: 'rohan.j@devspace.io',
        requestedAt: '15 mins ago',
        reason: 'Passionate about engineering best practices and looking to actively contribute to discussions and project rooms.',
      },
      {
        id: 'req-2',
        name: 'Tanvi Shah',
        title: 'AI Research Scholar · IIT Bombay',
        email: 'tanvi.shah@ai-lab.edu',
        requestedAt: '2 hours ago',
        reason: 'Interested in collaborating on open problems, sharing resources, and participating in community workshops.',
      },
    ]
  );

  // Members list (Requirement 6: default to Member for joined users)
  const initialFounder = community.adminName || (community.role === 'Admin' ? app.profile.name : people[0].name);
  const [membersList, setMembersList] = useState([
    {
      id: 'u-1',
      name: initialFounder,
      role: 'Admin',
      title: 'Community Founder & Admin',
      joined: 'Jan 2026',
    },
    {
      id: 'u-2',
      name: 'Aman Verma',
      role: 'Sub-admin',
      title: 'Tech Lead · Mobile',
      joined: 'Feb 2026',
    },
    {
      id: 'u-3',
      name: 'Kavita Pillai',
      role: 'Member',
      title: 'Product Designer',
      joined: 'Mar 2026',
    },
    ...(initialFounder !== app.profile.name
      ? [
          {
            id: 'u-self',
            name: app.profile.name,
            role: community.role === 'Admin' ? 'Admin' : 'Member',
            title: app.profile.headline || 'Member',
            joined: 'Today',
          },
        ]
      : []),
  ]);

  // Current user's role in this community
  const currentUserObj = membersList.find((m) => m.name === app.profile.name);
  const userRole = currentUserObj ? currentUserObj.role : (community.role || 'Member');
  const membership = app.memberships[community.id] || (userRole === 'Admin' ? 'member' : 'member');

  // Permission conditions for join requests
  const needsPermissionToJoin = Boolean(
    communitySettings.needsApproval ||
      communitySettings.privacy === 'Private' ||
      communitySettings.privacy === 'Approval required' ||
      community.policy === 'Approval required' ||
      community.policy === 'Application'
  );

  const canReviewRequests =
    userRole === 'Admin' ||
    (userRole === 'Sub-admin' && communitySettings.allowSubadminsApproveRequests !== false);

  const showRequestsTab = needsPermissionToJoin && canReviewRequests;

  // Handle Approve / Allow Join Request
  const handleApproveRequest = (req) => {
    const newMember = {
      id: req.id || `u-${Date.now()}`,
      name: req.name,
      role: 'Member',
      title: req.title || 'Community Member',
      joined: 'Today',
    };
    setMembersList((prev) => [...prev, newMember]);
    setJoinRequests((prev) => prev.filter((r) => r.id !== req.id));
    app.toast(`Allowed ${req.name} to join ${communitySettings.title}!`);
    app.notify?.(
      'Member request approved',
      `${req.name} joined ${communitySettings.title}.`,
      'Community'
    );
  };

  // Handle Decline Join Request
  const handleDeclineRequest = (req) => {
    setJoinRequests((prev) => prev.filter((r) => r.id !== req.id));
    app.toast(`Declined request from ${req.name}.`);
  };

  // Handle Approve All Requests
  const handleApproveAllRequests = () => {
    if (!joinRequests.length) return;
    const newMembers = joinRequests.map((req) => ({
      id: req.id || `u-${Date.now()}-${Math.random()}`,
      name: req.name,
      role: 'Member',
      title: req.title || 'Community Member',
      joined: 'Today',
    }));
    setMembersList((prev) => [...prev, ...newMembers]);
    setJoinRequests([]);
    app.toast(`Allowed all ${newMembers.length} pending members to join!`);
  };

  // Ensure view resets if Requests tab becomes unavailable
  useEffect(() => {
    if (!showRequestsTab && view === 'Requests') {
      setView('Chat');
    }
  }, [showRequestsTab, view]);

  // Channels state (Requirement 1 & 2: Channels only, no '#')
  const [channels, setChannels] = useState([
    {
      id: 'ch-gen',
      name: 'general',
      topic: 'Overall community general chat and welcomes',
      isPrivate: false,
      isDefault: true,
      creatorName: initialFounder,
      roomAdmins: [initialFounder],
      members: ['All Community Members'],
    },
    {
      id: 'ch-ann',
      name: 'announcements',
      topic: 'Official updates from admins & sub-admins',
      isPrivate: false,
      isDefault: false,
      creatorName: initialFounder,
      roomAdmins: [initialFounder],
      members: ['All Community Members'],
    },
    {
      id: 'ch-show',
      name: 'project-showcase',
      topic: 'Share screenshots, demos, and builds',
      isPrivate: false,
      isDefault: false,
      creatorName: 'Aman Verma',
      roomAdmins: ['Aman Verma'],
      members: ['All Community Members'],
    },
    {
      id: 'ch-help',
      name: 'ask-help',
      topic: 'Code debugging, architecture & career questions',
      isPrivate: false,
      isDefault: false,
      creatorName: 'Kavita Pillai',
      roomAdmins: ['Kavita Pillai'],
      members: ['All Community Members'],
    },
    {
      id: 'ch-core',
      name: 'core-team',
      topic: 'Private channel for admins and sub-admins',
      isPrivate: true,
      isDefault: false,
      creatorName: initialFounder,
      roomAdmins: [initialFounder],
      members: [initialFounder, 'Aman Verma'],
    },
  ]);

  // Chat messages with support for media, polls, reactions, and tags (Requirements 2, 6, 12, 13)
  const [chatMessages, setChatMessages] = useState([
    {
      id: 'm-1',
      channel: 'general',
      sender: initialFounder,
      role: 'Admin',
      time: '10:15 AM',
      body: 'Welcome to our community lounge! Everyone is in this main channel by default. Feel free to say hello or explore topic channels.',
      reactions: { '👍': 5, '🚀': 8 },
    },
    {
      id: 'm-2',
      channel: 'general',
      sender: 'Aman Verma',
      role: 'Sub-admin',
      time: '10:30 AM',
      body: 'Shared our revised frontend state management diagram below. Feel free to inspect the architecture!',
      media: {
        name: 'architecture_diagram_v2.png',
        type: 'image',
        url: photos.code || 'https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=800&q=80',
        size: '1.4 MB',
        tag: 'Architecture',
      },
      reactions: { '❤️': 4, '🔥': 3 },
    },
    {
      id: 'm-3',
      channel: 'general',
      sender: 'Kavita Pillai',
      role: 'Member',
      time: '11:05 AM',
      body: 'Here is the preliminary spec sheet for our shared UI design tokens.',
      media: {
        name: 'collobster_design_tokens.pdf',
        type: 'document',
        url: '#',
        size: '840 KB',
        tag: 'Design Assets',
      },
      reactions: { '👍': 3 },
    },
  ]);

  // Draft and attached media state for chat
  const [draft, setDraft] = useState('');
  const [attachedMedia, setAttachedMedia] = useState(null);
  const [attachedMediaTag, setAttachedMediaTag] = useState('Guides & Docs');
  const fileInputRef = React.useRef(null);

  // Channels pinning & context menus
  const [pinnedChannelNames, setPinnedChannelNames] = useState([]);
  const [channelContextMenu, setChannelContextMenu] = useState(null);

  // Message interactions: context menu, reaction picker, reply, edit, pin
  const [msgContextMenu, setMsgContextMenu] = useState(null);
  const [showReactionPicker, setShowReactionPicker] = useState(false);
  const [replyingTo, setReplyingTo] = useState(null);
  const [editingMessage, setEditingMessage] = useState(null);
  const [pinnedMessageIds, setPinnedMessageIds] = useState([]);

  // Auto-close context menus when clicking outside
  useEffect(() => {
    const handleDocClick = () => {
      setMsgContextMenu(null);
      setChannelContextMenu(null);
      setShowReactionPicker(false);
    };
    window.addEventListener('click', handleDocClick);
    return () => window.removeEventListener('click', handleDocClick);
  }, []);

  // Community-wide official resources (Requirement 8)
  const [communityResources, setCommunityResources] = useState([
    {
      id: 'res-1',
      title: 'Community Onboarding & Welcome Playbook',
      description: 'Everything you need to know about participating, sharing code, and collaborating.',
      category: 'Guides & Docs',
      type: 'PDF Document',
      size: '1.8 MB',
      author: initialFounder,
      addedAt: 'Jan 2026',
      url: '#',
    },
    {
      id: 'res-2',
      title: 'Collobster Design System & UI Kit',
      description: 'Official Figma library containing components, color tokens, and layout guidelines.',
      category: 'Design Assets',
      type: 'Figma File',
      size: '8.4 MB',
      author: initialFounder,
      addedAt: 'Feb 2026',
      url: '#',
    },
    {
      id: 'res-3',
      title: 'Fullstack Project Starter Template',
      description: 'Production-ready boilerplate with authentication, database hooks, and automated testing.',
      category: 'Templates',
      type: 'ZIP Archive',
      size: '3.2 MB',
      author: 'Aman Verma',
      addedAt: 'Mar 2026',
      url: '#',
    },
    {
      id: 'res-4',
      title: 'Code of Conduct & Moderation Guidelines',
      description: 'Our community pledge, standards of behavior, and reporting procedures.',
      category: 'Official Assets',
      type: 'PDF Document',
      size: '450 KB',
      author: initialFounder,
      addedAt: 'Mar 2026',
      url: '#',
    },
  ]);

  // Modals state
  const [showCreateChannelModal, setShowCreateChannelModal] = useState(false);
  const [newChannelName, setNewChannelName] = useState('');
  const [newChannelTopic, setNewChannelTopic] = useState('');
  const [newChannelPrivate, setNewChannelPrivate] = useState(false);

  const [showAddMemberModal, setShowAddMemberModal] = useState(false);
  const [newMemberName, setNewMemberName] = useState('');
  const [newMemberEmail, setNewMemberEmail] = useState('');
  const [newMemberRole, setNewMemberRole] = useState('Member');

  const [showPostPollModal, setShowPostPollModal] = useState(false);
  const [showRoomSettingsModal, setShowRoomSettingsModal] = useState(false);
  const [showTransferAdminModal, setShowTransferAdminModal] = useState(false);
  const [showAddResourceModal, setShowAddResourceModal] = useState(false);
  const [showChannelProfileModal, setShowChannelProfileModal] = useState(false);
  const [showPlusMenu, setShowPlusMenu] = useState(false);

  // Synchronized real-time polls state across chat and polls tab
  const [communityPolls, setCommunityPolls] = useState([
    {
      id: 'poll-1',
      question: 'Which state-management approach do you prefer for large applications?',
      totalVotes: 86,
      userVoted: 'Riverpod',
      options: [
        { label: 'Riverpod', count: 42 },
        { label: 'Provider', count: 24 },
        { label: 'Bloc / Cubit', count: 16 },
        { label: 'GetX', count: 4 },
      ],
    },
    {
      id: 'poll-2',
      question: 'Should our next live workshop focus on AI Agents or Flutter 3.x internals?',
      totalVotes: 58,
      userVoted: null,
      options: [
        { label: 'Autonomous AI Agents with Python & Ollama', count: 35 },
        { label: 'Flutter 3.x RenderObjects & Performance', count: 18 },
        { label: 'Fullstack GraphQL & Microservices', count: 5 },
      ],
    },
  ]);

  // Active channel resolution
  const activeCh = channels.find((c) => c.name === activeChannel) || channels[0];

  // Permissions logic (Requirement 4 & 5)
  const canCreateRooms = useMemo(() => {
    if (userRole === 'Admin') return true;
    if (communitySettings.roomCreationPermission === 'admins') return false;
    if (communitySettings.roomCreationPermission === 'subadmins') return userRole === 'Sub-admin';
    return true; // 'all'
  }, [userRole, communitySettings.roomCreationPermission]);

  // Person who creates the room has room admin privileges for that channel (Requirement 5)
  const isRoomAdmin = useMemo(() => {
    if (userRole === 'Admin') return true;
    if (activeCh?.creatorName === app.profile.name) return true;
    if (activeCh?.roomAdmins?.includes(app.profile.name)) return true;
    return false;
  }, [userRole, activeCh, app.profile.name]);

  // Channel resources (Requirement 7: files shared in this channel only)
  const currentChannelMessages = chatMessages.filter(
    (m) => m.channel === activeChannel
  );
  const channelFiles = currentChannelMessages
    .filter((m) => m.media)
    .map((m) => ({
      ...m.media,
      messageId: m.id,
      sender: m.sender,
      role: m.role,
      time: m.time,
    }));

  // Handle Send Chat Message (Text + Media + Replies + Edits + Tags)
  const handleSendMessage = () => {
    if (editingMessage) {
      if (!draft.trim()) return;
      setChatMessages((prev) =>
        prev.map((m) =>
          m.id === editingMessage.id
            ? { ...m, body: draft.trim(), edited: true }
            : m
        )
      );
      setEditingMessage(null);
      setDraft('');
      app.toast('Message updated!');
      return;
    }
    if (!draft.trim() && !attachedMedia) return;
    const newMsg = {
      id: `msg-${Date.now()}`,
      channel: activeChannel,
      sender: app.profile.name,
      role: userRole,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      body: draft.trim(),
      media: attachedMedia ? { ...attachedMedia, tag: attachedMediaTag } : undefined,
      replyTo: replyingTo ? { id: replyingTo.id, sender: replyingTo.sender, body: replyingTo.body } : undefined,
      reactions: {},
    };
    setChatMessages((prev) => [...prev, newMsg]);
    setDraft('');
    setAttachedMedia(null);
    setReplyingTo(null);
  };

  // Handle Reactions on Messages (Requirement 6)
  const handleAddReaction = (msgId, emoji) => {
    setChatMessages((prev) =>
      prev.map((m) => {
        if (m.id !== msgId) return m;
        const currentCount = m.reactions?.[emoji] || 0;
        return {
          ...m,
          reactions: {
            ...m.reactions,
            [emoji]: currentCount + 1,
          },
        };
      })
    );
    setMsgContextMenu(null);
    setShowReactionPicker(false);
  };

  // Handle File Selection for Chat
  const handleFileSelect = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const isImg = file.type.startsWith('image/');
    const isPdf = file.type.includes('pdf');
    const isVideo = file.type.startsWith('video/');
    const isCode = file.name.endsWith('.js') || file.name.endsWith('.jsx') || file.name.endsWith('.dart') || file.name.endsWith('.py');
    const type = isImg ? 'image' : isVideo ? 'video' : isCode ? 'code' : isPdf ? 'document' : 'document';
    const sizeStr = file.size > 1024 * 1024 ? `${(file.size / (1024 * 1024)).toFixed(1)} MB` : `${Math.round(file.size / 1024)} KB`;

    const mediaObj = {
      name: file.name,
      type,
      size: sizeStr,
      url: isImg ? URL.createObjectURL(file) : '#',
      tag: attachedMediaTag,
    };
    setAttachedMedia(mediaObj);
    app.toast(`Attached ${file.name}`);
    e.target.value = '';
  };

  // Preset file attachment simulator for quick testing
  const handleAttachPreset = (type) => {
    if (type === 'image') {
      setAttachedMedia({
        name: 'release_screenshot.png',
        type: 'image',
        url: photos.team || 'https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=800&q=80',
        size: '1.2 MB',
        tag: 'Design Assets',
      });
      setAttachedMediaTag('Design Assets');
      app.toast('Attached release_screenshot.png');
    } else if (type === 'code') {
      setAttachedMedia({
        name: 'community_state_handler.mjs',
        type: 'code',
        url: '#',
        size: '14 KB',
        tag: 'Templates',
      });
      setAttachedMediaTag('Templates');
      app.toast('Attached community_state_handler.mjs');
    } else {
      setAttachedMedia({
        name: 'community_roadmap_2026.pdf',
        type: 'document',
        url: '#',
        size: '950 KB',
        tag: 'Guides & Docs',
      });
      setAttachedMediaTag('Guides & Docs');
      app.toast('Attached community_roadmap_2026.pdf');
    }
  };

  // Handle Post Poll in Chat (Requirement 2 & real-time sync)
  const handlePostPollInChat = (pollData) => {
    const pollId = `p-${Date.now()}`;
    const newPollObj = {
      id: pollId,
      question: pollData.question,
      options: pollData.options.map((opt) => ({ label: opt, count: 0 })),
      totalVotes: 0,
      userVoted: null,
    };
    const newMsg = {
      id: `poll-${Date.now()}`,
      channel: activeChannel,
      sender: app.profile.name,
      role: userRole,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      body: pollData.question,
      isPoll: true,
      poll: newPollObj,
      reactions: {},
    };
    setChatMessages((prev) => [...prev, newMsg]);
    setCommunityPolls((prev) => [newPollObj, ...prev]);
    setShowPostPollModal(false);
    app.toast('Poll posted in chat and published to Community Polls!');
  };

  // Vote on In-Chat Poll (allows switching votes)
  const handleVoteInChat = (messageId, optionLabel) => {
    let targetQuestion = null;
    let switched = false;
    setChatMessages((prev) =>
      prev.map((msg) => {
        if (msg.id !== messageId || !msg.poll) return msg;
        if (msg.poll.userVoted === optionLabel) return msg;
        targetQuestion = msg.poll.question;
        const prevVote = msg.poll.userVoted;
        switched = !!prevVote;
        const newTotal = prevVote ? msg.poll.totalVotes : msg.poll.totalVotes + 1;
        const newOptions = msg.poll.options.map((opt) => {
          if (opt.label === optionLabel) return { ...opt, count: opt.count + 1 };
          if (prevVote && opt.label === prevVote) return { ...opt, count: Math.max(0, opt.count - 1) };
          return opt;
        });
        return {
          ...msg,
          poll: {
            ...msg.poll,
            totalVotes: newTotal,
            userVoted: optionLabel,
            options: newOptions,
          },
        };
      })
    );
    if (targetQuestion) {
      setCommunityPolls((prev) =>
        prev.map((poll) => {
          if (poll.question !== targetQuestion || poll.userVoted === optionLabel) return poll;
          const prevVote = poll.userVoted;
          const newTotal = prevVote ? poll.totalVotes : poll.totalVotes + 1;
          const newOptions = poll.options.map((opt) => {
            if (opt.label === optionLabel) return { ...opt, count: opt.count + 1 };
            if (prevVote && opt.label === prevVote) return { ...opt, count: Math.max(0, opt.count - 1) };
            return opt;
          });
          return {
            ...poll,
            totalVotes: newTotal,
            userVoted: optionLabel,
            options: newOptions,
          };
        })
      );
    }
    app.toast(switched ? 'Vote updated!' : 'Vote recorded in channel chat!');
  };

  // Vote from Community Polls Tab (Real-time sync to chat, allows switching votes)
  const handleVoteFromTab = (pollId, optionLabel) => {
    let targetQuestion = null;
    let switched = false;
    setCommunityPolls((prev) =>
      prev.map((poll) => {
        if (poll.id !== pollId) return poll;
        if (poll.userVoted === optionLabel) return poll;
        targetQuestion = poll.question;
        const prevVote = poll.userVoted;
        switched = !!prevVote;
        const newTotal = prevVote ? poll.totalVotes : poll.totalVotes + 1;
        const newOptions = poll.options.map((opt) => {
          if (opt.label === optionLabel) return { ...opt, count: opt.count + 1 };
          if (prevVote && opt.label === prevVote) return { ...opt, count: Math.max(0, opt.count - 1) };
          return opt;
        });
        return {
          ...poll,
          totalVotes: newTotal,
          userVoted: optionLabel,
          options: newOptions,
        };
      })
    );
    if (targetQuestion) {
      setChatMessages((prev) =>
        prev.map((msg) => {
          if (!msg.poll || msg.poll.question !== targetQuestion || msg.poll.userVoted === optionLabel) return msg;
          const prevVote = msg.poll.userVoted;
          const newTotal = prevVote ? msg.poll.totalVotes : msg.poll.totalVotes + 1;
          const newOptions = msg.poll.options.map((opt) => {
            if (opt.label === optionLabel) return { ...opt, count: opt.count + 1 };
            if (prevVote && opt.label === prevVote) return { ...opt, count: Math.max(0, opt.count - 1) };
            return opt;
          });
          return {
            ...msg,
            poll: {
              ...msg.poll,
              totalVotes: newTotal,
              userVoted: optionLabel,
              options: newOptions,
            },
          };
        })
      );
    }
    app.toast(switched ? 'Vote updated!' : 'Vote recorded in real time!');
  };

  // Poll created from Polls Tab (Real-time broadcast to chat)
  const handlePollCreatedFromTab = (newPoll) => {
    setCommunityPolls((prev) => [newPoll, ...prev]);
    const newMsg = {
      id: `poll-${Date.now()}`,
      channel: activeChannel,
      sender: app.profile.name,
      role: userRole,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      body: newPoll.question,
      isPoll: true,
      poll: newPoll,
      reactions: {},
    };
    setChatMessages((prev) => [...prev, newMsg]);
    app.toast('Poll created and broadcast to community chat in real time!');
  };

  // Handle Create Channel (Requirement 1 & 2: Creator has admin privilege, no '#')
  const handleCreateChannel = (e) => {
    e.preventDefault();
    const rawClean = newChannelName.trim().replace(/^[#🔒]\s*/, '');
    if (!rawClean) return;
    if (!canCreateRooms) {
      app.toast('Channel creation is restricted by community settings.');
      return;
    }
    const exists = channels.some(
      (c) => c.name.replace(/^[#🔒]\s*/, '').toLowerCase() === rawClean.toLowerCase()
    );
    if (exists) {
      app.toast(`A channel named "${rawClean}" already exists.`);
      return;
    }
    const cleanName = newChannelPrivate ? `🔒 ${rawClean}` : rawClean;
    const newCh = {
      id: `ch-${Date.now()}`,
      name: cleanName,
      topic: newChannelTopic.trim() || 'Community channel',
      isPrivate: newChannelPrivate,
      isDefault: false,
      creatorName: app.profile.name,
      roomAdmins: [app.profile.name], // Creator has admin privilege for that channel!
      members: ['All Community Members'],
      createdAt: 'Today',
    };
    setChannels((prev) => [...prev, newCh]);
    setActiveChannel(cleanName);
    setShowCreateChannelModal(false);
    setNewChannelName('');
    setNewChannelTopic('');
    setNewChannelPrivate(false);
    app.toast(`Channel ${cleanName} created! You are channel admin.`);
  };

  // Handle Add Member to Community (Requirement 7: step-based)
  const handleAddMember = (memberData) => {
    const targetName = (memberData?.name || newMemberName).trim();
    if (!targetName) return;
    const targetRole = memberData?.role || newMemberRole;
    const targetTitle = (memberData?.title || newMemberEmail).trim() || 'Community Member';
    const newM = {
      id: `mem-${Date.now()}`,
      name: targetName,
      role: targetRole,
      title: targetTitle,
      joined: 'Today',
    };
    setMembersList((prev) => [...prev, newM]);
    setShowAddMemberModal(false);
    setNewMemberName('');
    setNewMemberEmail('');
    app.toast(`Added ${newM.name} as ${newM.role}!`);
  };

  // Handle Leave Active Channel (Requirement 9: opt to leave a specific channel)
  const handleLeaveActiveChannel = () => {
    if (activeChannel === 'general') {
      app.toast('You cannot leave the main general channel.');
      return;
    }
    const chNameClean = activeChannel.replace(/^[#🔒]\s*/, '');
    if (window.confirm(`Are you sure you want to leave channel "${chNameClean}"?`)) {
      setChannels((prev) =>
        prev.map((c) =>
          c.name === activeChannel
            ? { ...c, members: (c.members || []).filter((name) => name !== app.profile.name) }
            : c
        )
      );
      setActiveChannel('general');
      app.toast(`You left channel "${chNameClean}".`);
    }
  };

  // Aggregated community resources: official catalog + chat shared files with tags (Requirement 13)
  const allCommunityResources = useMemo(() => {
    const chatShared = chatMessages
      .filter((m) => m.media && !m.deleted)
      .map((m) => ({
        id: `chat-${m.id}`,
        title: m.media.name,
        description: `Shared by ${m.sender} in ${m.channel.replace(/^[#🔒]\s*/, '')}`,
        category: m.media.tag || (m.media.type === 'image' ? 'Design Assets' : 'Guides & Docs'),
        tag: m.media.tag || (m.media.type === 'image' ? 'Design Assets' : 'Guides & Docs'),
        type: m.media.type.toUpperCase(),
        size: m.media.size,
        author: m.sender,
        addedAt: m.time,
        url: m.media.url || '#',
        isFromChat: true,
        channel: m.channel.replace(/^[#🔒]\s*/, ''),
      }));
    return [...communityResources, ...chatShared];
  }, [communityResources, chatMessages]);

  // Handle Leave Community
  const handleLeaveCommunity = () => {
    if (window.confirm(`Are you sure you want to leave ${communitySettings.title}?`)) {
      app.setMemberships((prev) => {
        const copy = { ...prev };
        delete copy[community.id];
        return copy;
      });
      app.toast(`You have left ${communitySettings.title}.`);
      onBack();
    }
  };

  // Handle Delete Community (Requirement 10: Admin only, confirmed)
  const handleDeleteCommunity = () => {
    if (window.confirm(`Are you sure you want to delete "${communitySettings.title}"? This action will permanently remove the community.`)) {
      if (app.setDeletedCommunityIds) {
        app.setDeletedCommunityIds((old = []) => [...new Set([...old, community.id])]);
      }
      if (app.setCommunityCatalog) {
        app.setCommunityCatalog((old = []) => (old || []).filter((c) => c.id !== community.id));
      }
      if (app.setMemberships) {
        app.setMemberships((prev = {}) => {
          const copy = { ...prev };
          delete copy[community.id];
          return copy;
        });
      }
      app.toast(`Community "${communitySettings.title}" has been deleted.`);
      onBack();
    }
  };

  // Handle Copy Invite Link
  const handleCopyInviteLink = () => {
    const inviteUrl = `https://collobster.app/c/${community.id}?invite=collob2026`;
    navigator.clipboard?.writeText?.(inviteUrl);
    app.toast('Community invite link copied to clipboard!');
  };

  // Save Community Settings (Requirement 9)
  const handleSaveCommunitySettings = (updated) => {
    setCommunitySettings(updated);
    if (app.setCommunityCatalog) {
      app.setCommunityCatalog((old = []) => {
        const exists = old.some((c) => c.id === community.id);
        const updatedEntry = {
          ...community,
          ...updated,
          adminName: membersList.find((m) => m.role === 'Admin')?.name || initialFounder,
        };
        if (exists) {
          return old.map((c) => (c.id === community.id ? updatedEntry : c));
        }
        return [updatedEntry, ...old];
      });
    }
    app.toast('Community settings saved successfully!');
    app.notify('Settings updated', `${updated.title} settings were saved.`, 'Community');
  };

  // Handle Admin Position Transfer (Requirement 6)
  const handleTransferAdmin = (newAdminName, transitionChoice) => {
    const currentAdminName = app.profile.name;
    let updatedMembers = membersList.map((m) => {
      if (m.name === newAdminName) {
        return { ...m, role: 'Admin', title: 'Community Admin' };
      }
      if (m.name === currentAdminName) {
        if (transitionChoice === 'subadmin') {
          return { ...m, role: 'Sub-admin' };
        }
        if (transitionChoice === 'member') {
          return { ...m, role: 'Member' };
        }
      }
      return m;
    });

    if (transitionChoice === 'leave') {
      updatedMembers = updatedMembers.filter((m) => m.name !== currentAdminName);
    }

    setMembersList(updatedMembers);
    setShowTransferAdminModal(false);

    // Save update to catalog
    if (app.setCommunityCatalog) {
      app.setCommunityCatalog((old = []) =>
        old.map((c) =>
          c.id === community.id
            ? { ...c, adminName: newAdminName, role: transitionChoice === 'leave' ? undefined : transitionChoice === 'subadmin' ? 'Sub-admin' : 'Member' }
            : c
        )
      );
    }

    if (transitionChoice === 'leave') {
      app.setMemberships((prev) => {
        const copy = { ...prev };
        delete copy[community.id];
        return copy;
      });
      app.toast(`Admin transferred to ${newAdminName}. You have left ${communitySettings.title}.`);
      onBack();
    } else {
      app.toast(
        `Admin position transferred to ${newAdminName}! You are now ${
          transitionChoice === 'subadmin' ? 'a Sub-admin' : 'a Member'
        }.`
      );
    }
  };

  // Handle Add Community Resource (Requirement 8)
  const handleAddCommunityResource = (res) => {
    const newRes = {
      ...res,
      id: `res-${Date.now()}`,
      author: app.profile.name,
      addedAt: 'Today',
    };
    setCommunityResources((prev) => [newRes, ...prev]);
    setShowAddResourceModal(false);
    app.toast(`Resource "${res.title}" published to community profile!`);
  };

  return (
    <main className="operator-space">
      {/* Top Navigation Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <button className="back-link" onClick={onBack}>
          ← Communities
        </button>
        <div style={{ display: 'flex', gap: 8 }}>
          <Button variant="ghost" icon={Share2} onClick={handleCopyInviteLink}>
            Invite link
          </Button>
        </div>
      </div>

      {/* Community Profile Header (Requirement 9: "Invite friends" button removed) */}
      <section className="community-profile-head">
        <span className={`module-icon ${community.color || 'blue'}`}>
          <Users size={28} />
        </span>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 4, flexWrap: 'wrap' }}>
            <Tag tone="purple">{communitySettings.privacy}</Tag>
            <span
              className={`community-role-badge ${
                userRole === 'Admin'
                  ? 'role-badge-admin'
                  : userRole === 'Sub-admin'
                  ? 'role-badge-subadmin'
                  : 'role-badge-member'
              }`}
            >
              Your role: {userRole}
            </span>
            {/* Quick role test switch for testing Admin / Sub-admin / Member permissions */}
            <select
              value={userRole}
              onChange={(e) => {
                const targetRole = e.target.value;
                setMembersList((prev) =>
                  prev.map((m) =>
                    m.name === app.profile.name ? { ...m, role: targetRole } : m
                  )
                );
                app.toast(`Switched your role to ${targetRole} for testing!`);
              }}
              style={{
                fontSize: 11,
                padding: '2px 8px',
                borderRadius: 6,
                border: '1px solid var(--line)',
                background: 'var(--surface)',
                cursor: 'pointer',
              }}
              title="Switch role to test Admin, Sub-admin, and Member permissions"
            >
              <option value="Admin">Role: Admin</option>
              <option value="Sub-admin">Role: Sub-admin</option>
              <option value="Member">Role: Member</option>
            </select>
          </div>
          <h1 style={{ wordBreak: 'break-word' }}>{communitySettings.title}</h1>
          <p style={{ wordBreak: 'break-word' }}>{communitySettings.description}</p>
          <small>
            {membersList.length} members · {community.category} · Admin:{' '}
            {membersList.find((m) => m.role === 'Admin')?.name || initialFounder}
          </small>
        </div>
      </section>

      {/* Navigation Tabs - Requests placed beside Settings */}
      <Tabs
        items={[
          'Chat',
          'Channels',
          'Members',
          'Polls',
          'Files',
          ...(showRequestsTab ? ['Requests'] : []),
          'Settings',
        ]}
        value={view === 'Resources' ? 'Files' : view}
        onChange={setView}
      />

      {/* ─── CHAT TAB ─── */}
      {view === 'Chat' && (
        <div className={`community-chat-layout ${mobileChannelsOpen ? 'channels-drawer-open' : ''}`}>
          {/* Mobile backdrop */}
          {mobileChannelsOpen && (
            <div
              className="channels-sidebar-backdrop"
              onClick={() => setMobileChannelsOpen(false)}
            />
          )}

          {/* Sidebar */}
          <aside className={`channels-sidebar ${mobileChannelsOpen ? 'mobile-open' : ''}`}>
            {/* Mobile close button in channels sidebar */}
            <div className="mobile-channels-header">
              <span>Community Channels</span>
              <button
                type="button"
                className="mobile-channels-close-btn"
                onClick={() => setMobileChannelsOpen(false)}
                title="Close channels"
              >
                <X size={18} />
              </button>
            </div>

            {/* Section 1: Main Channel */}
            <div className="channels-group-title">
              <span>Main Channel</span>
            </div>
            <button
              className={`channel-item-btn default-lounge-btn ${
                activeChannel === 'general' ? 'active' : ''
              }`}
              onClick={() => {
                setActiveChannel('general');
                setChatSubView('stream');
                setMobileChannelsOpen(false);
              }}
              onContextMenu={(e) => {
                e.preventDefault();
                setChannelContextMenu({
                  x: Math.min(e.clientX, window.innerWidth - 200),
                  y: Math.min(e.clientY, window.innerHeight - 150),
                  channel: channels.find((c) => c.name === 'general') || { name: 'general' },
                });
              }}
            >
              <Globe2 size={15} />
              <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', flex: 1 }}>
                general
              </span>
              {pinnedChannelNames.includes('general') && (
                <Pin size={12} color="var(--coral)" style={{ transform: 'rotate(45deg)' }} />
              )}
              <span className="room-badge-default">All Members</span>
            </button>

            <div className="channel-section-divider" />

            {/* Section 2: Channels (Requirement 1 & 5: Right click to pin to top) */}
            <div className="channels-group-title">
              <span>Channels</span>
              {canCreateRooms ? (
                <button
                  type="button"
                  style={{
                    background: 'none',
                    border: 0,
                    cursor: 'pointer',
                    color: 'var(--coral)',
                  }}
                  onClick={() => setShowCreateChannelModal(true)}
                  title="Create Channel"
                >
                  <Plus size={16} />
                </button>
              ) : (
                <span title="Channel creation restricted by community admin" style={{ color: 'var(--muted)', display: 'inline-flex' }}>
                  <Lock size={13} />
                </span>
              )}
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              {[...channels.filter((ch) => ch.name !== 'general')]
                .sort((a, b) => {
                  const aPin = pinnedChannelNames.includes(a.name);
                  const bPin = pinnedChannelNames.includes(b.name);
                  if (aPin && !bPin) return -1;
                  if (!aPin && bPin) return 1;
                  return 0;
                })
                .map((ch) => {
                  const isCreator = ch.creatorName === app.profile.name;
                  const isPinned = pinnedChannelNames.includes(ch.name);
                  return (
                    <button
                      key={ch.id}
                      className={`channel-item-btn ${
                        activeChannel === ch.name ? 'active' : ''
                      }`}
                      onClick={() => {
                        setActiveChannel(ch.name);
                        setChatSubView('stream');
                        setMobileChannelsOpen(false);
                      }}
                      onContextMenu={(e) => {
                        e.preventDefault();
                        setChannelContextMenu({
                          x: Math.min(e.clientX, window.innerWidth - 200),
                          y: Math.min(e.clientY, window.innerHeight - 150),
                          channel: ch,
                        });
                      }}
                    >
                      {ch.isPrivate ? <Lock size={14} /> : <Users size={14} />}
                      <span
                        style={{
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                          flex: 1,
                        }}
                      >
                        {ch.name.replace(/^[#🔒]\s*/, '')}
                      </span>
                      {isPinned && (
                        <Pin size={12} color="var(--coral)" style={{ transform: 'rotate(45deg)' }} />
                      )}
                      {isCreator && (
                        <span title="You are channel admin" style={{ color: 'var(--coral)', fontSize: 10 }}>
                          ★ Admin
                        </span>
                      )}
                    </button>
                  );
                })}
            </div>
          </aside>

          {/* Main Stream Area */}
          <section className="chat-main-stream">
            {/* Chat Stream Header */}
            <div className="chat-stream-header">
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, minWidth: 0, flex: 1 }}>
                <button
                  type="button"
                  className="mobile-channels-toggle-btn"
                  onClick={() => setMobileChannelsOpen(!mobileChannelsOpen)}
                  title="Switch channel"
                >
                  <Hash size={14} />
                  <span>Channels</span>
                </button>
                <button
                  type="button"
                  className="channel-profile-header-trigger"
                  onClick={() => setShowChannelProfileModal(true)}
                  title="Click to view channel profile &amp; management options"
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                    background: 'none',
                    border: 0,
                    cursor: 'pointer',
                    padding: '4px 6px',
                    borderRadius: 8,
                    textAlign: 'left',
                    minWidth: 0,
                    flex: 1,
                  }}
                >
                  <div
                    style={{
                      width: 34,
                      height: 34,
                      borderRadius: 8,
                      background: 'var(--coral)',
                      color: '#fff',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: 700,
                      fontSize: 15,
                      flexShrink: 0,
                    }}
                  >
                    {activeChannel.startsWith('🔒') || activeCh?.isPrivate ? <Lock size={16} /> : <Users size={17} />}
                  </div>
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                      <b style={{ fontSize: 14, color: 'var(--ink)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {activeChannel.replace(/^[#🔒]\s*/, '')}
                      </b>
                      {isRoomAdmin && (
                        <span className="room-admin-indicator">
                          ★ Channel Admin
                        </span>
                      )}
                    </div>
                    <small className="channel-topic-text" style={{ display: 'block', color: 'var(--muted)', fontSize: 11, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {activeCh?.topic || 'Community discussion channel'}
                    </small>
                  </div>
                </button>
              </div>

              {/* 3 dots menu button at top right corner */}
              <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexShrink: 0 }}>
                <IconButton
                  icon={MoreVertical}
                  label="Channel profile & options"
                  title="Channel profile & options"
                  onClick={() => setShowChannelProfileModal(true)}
                />
              </div>
            </div>

            {/* Pinned channel message notification banner */}
            {pinnedMessageIds.length > 0 && (
              <div
                className="channel-pinned-banner"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '6px 12px',
                  background: 'rgba(234,88,12,0.08)',
                  borderBottom: '1px solid var(--line)',
                  fontSize: 12,
                  gap: 8,
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0, flex: 1 }}>
                  <Pin size={13} color="var(--coral)" style={{ transform: 'rotate(45deg)', flexShrink: 0 }} />
                  <b style={{ flexShrink: 0 }}>Pinned:</b>
                  <span style={{ color: 'var(--muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', minWidth: 0, flex: 1 }}>
                    {chatMessages.find((m) => pinnedMessageIds.includes(m.id))?.body || 'Pinned message'}
                  </span>
                </div>
                <button
                  type="button"
                  style={{ border: 0, background: 'none', cursor: 'pointer', color: 'var(--coral)', fontSize: 11, fontWeight: 600, flexShrink: 0 }}
                  onClick={() => setPinnedMessageIds([])}
                >
                  Unpin
                </button>
              </div>
            )}

            {/* Sub-view: Channel Resources Panel (Requirement 7: files shared in this chat only) */}
            {chatSubView === 'resources' ? (
              <ChannelResourcesPanel
                channelName={activeChannel}
                files={channelFiles}
                onBack={() => setChatSubView('stream')}
                onAttachFile={() => fileInputRef.current?.click()}
                onShareFiles={(filesToShareNames) => {
                  const toShare = channelFiles.filter((f) => filesToShareNames.includes(f.name));
                  toShare.forEach((f) => {
                    const newMsg = {
                      id: `msg-${Date.now()}-${Math.random()}`,
                      channel: activeChannel,
                      sender: app.profile.name,
                      role: userRole,
                      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
                      body: `Shared resource: ${f.name}`,
                      media: { ...f },
                      reactions: {},
                    };
                    setChatMessages((prev) => [...prev, newMsg]);
                  });
                  app.toast(`Shared ${toShare.length} file(s) into channel chat!`);
                  setChatSubView('stream');
                }}
                app={app}
              />
            ) : (
              <>
                {/* Messages Container */}
                <div className="chat-messages-container">
                  {currentChannelMessages.length > 0 ? (
                    currentChannelMessages.map((msg) => {
                      const isMine = msg.sender === app.profile.name || msg.sender === 'You' || msg.mine;
                      return (
                      <div
                        className={`chat-message-row ${isMine ? 'chat-message-mine' : ''}`}
                        key={msg.id}
                        onContextMenu={(e) => {
                          e.preventDefault();
                          const menuWidth = 230;
                          const menuHeight = 280;
                          const x = Math.max(16, Math.min(e.clientX, window.innerWidth - menuWidth - 20));
                          const y = Math.max(16, Math.min(e.clientY, window.innerHeight - menuHeight - 20));
                          setMsgContextMenu({ x, y, message: msg });
                        }}
                      >
                        <div
                          onClick={() => app.openUserProfile && app.openUserProfile(msg.sender, 'Posts')}
                          style={{ cursor: 'pointer' }}
                          title={`View ${msg.sender}'s profile and posts`}
                        >
                          <Avatar name={msg.sender} />
                        </div>
                        <div className="chat-message-bubble">
                          {/* Reply Reference Header */}
                          {msg.replyTo && (
                            <div
                              style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 6,
                                fontSize: 11,
                                color: 'var(--muted)',
                                background: 'var(--soft)',
                                padding: '3px 8px',
                                borderRadius: 4,
                                marginBottom: 6,
                                borderLeft: '2px solid var(--coral)',
                              }}
                            >
                              <Reply size={12} color="var(--coral)" />
                              <span>
                                Replying to <b>{msg.replyTo.sender}</b>: {msg.replyTo.body?.slice(0, 35)}...
                              </span>
                            </div>
                          )}

                          <div className="chat-author-line">
                            <b
                              onClick={() => app.openUserProfile && app.openUserProfile(msg.sender, 'Posts')}
                              style={{ cursor: 'pointer', textDecoration: 'none' }}
                              className="chat-sender-username-btn"
                              title={`View ${msg.sender}'s profile and posts`}
                            >
                              {msg.sender}
                            </b>
                            {msg.role && (
                              <small className="chat-author-role-text" style={{ fontSize: 11, color: 'var(--muted)', fontWeight: 500 }}>
                                {msg.role}
                              </small>
                            )}
                          </div>

                          {/* Body text or deleted state (Requirement 11 & 12) */}
                          {msg.deleted ? (
                            <p style={{ margin: 0, fontSize: 13, fontStyle: 'italic', color: 'var(--muted)', wordBreak: 'break-word', overflowWrap: 'anywhere' }}>
                              This message was deleted
                            </p>
                          ) : (
                            msg.body && (
                              <p style={{ margin: 0, fontSize: 13, lineHeight: 1.4, wordBreak: 'break-word', overflowWrap: 'anywhere' }}>
                                {msg.body}
                              </p>
                            )
                          )}

                          {/* Media preview in message (Requirement 2, 7 & 13) */}
                          {!msg.deleted && msg.media && (
                            msg.media.type === 'image' ? (
                              <div className="chat-media-card">
                                <img src={msg.media.url} alt={msg.media.name} />
                                <div style={{ padding: '6px 10px', fontSize: 11, display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: 'var(--muted)' }}>
                                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                                    <span>{msg.media.name}</span>
                                    {msg.media.tag && (
                                      <span style={{ fontSize: 10, background: 'rgba(234,88,12,0.1)', color: 'var(--coral)', padding: '1px 6px', borderRadius: 4, fontWeight: 600 }}>
                                        #{msg.media.tag}
                                      </span>
                                    )}
                                  </div>
                                  <span>{msg.media.size}</span>
                                </div>
                              </div>
                            ) : (
                              <div className="chat-file-card">
                                <FileText size={22} />
                                <div style={{ flex: 1 }}>
                                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                                    <b>{msg.media.name}</b>
                                    {msg.media.tag && (
                                      <span style={{ fontSize: 10, background: 'rgba(234,88,12,0.1)', color: 'var(--coral)', padding: '1px 6px', borderRadius: 4, fontWeight: 600 }}>
                                        #{msg.media.tag}
                                      </span>
                                    )}
                                  </div>
                                  <small>{msg.media.size} · {msg.media.type.toUpperCase()}</small>
                                </div>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  icon={Download}
                                  onClick={() => app.toast(`Downloading ${msg.media.name}...`)}
                                  title="Download"
                                  aria-label="Download"
                                  style={{ flexShrink: 0 }}
                                />
                              </div>
                            )
                          )}

                          {/* In-chat interactive poll (Requirement 2) */}
                          {!msg.deleted && msg.isPoll && msg.poll && (
                            <InChatPoll
                              poll={msg.poll}
                              onVote={(opt) => handleVoteInChat(msg.id, opt)}
                            />
                          )}

                          {/* Reactions */}
                          {!msg.deleted && Object.keys(msg.reactions || {}).length > 0 && (
                            <div style={{ display: 'flex', gap: 4, marginTop: 6, flexWrap: 'wrap' }}>
                              {Object.entries(msg.reactions).map(([emoji, count]) => (
                                <button
                                  key={emoji}
                                  type="button"
                                  className="reaction-badge"
                                  onClick={() => handleAddReaction(msg.id, emoji)}
                                  style={{ cursor: 'pointer', border: '1px solid var(--line)', background: 'var(--surface)' }}
                                >
                                  {emoji} {count}
                                </button>
                              ))}
                            </div>
                          )}
                          {/* Right bottom small timestamp */}
                          <div className="chat-message-time-bottom" style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'center', gap: 4, marginTop: 4 }}>
                            {msg.edited && (
                              <small style={{ fontStyle: 'italic', color: 'var(--muted)', fontSize: 10 }}>
                                (edited)
                              </small>
                            )}
                            <small style={{ fontSize: 10, color: 'var(--muted)', lineHeight: 1 }}>
                              {msg.time}
                            </small>
                          </div>
                        </div>
                      </div>
                      );
                    })
                  ) : (
                    <Empty
                      title={`Welcome to ${activeChannel.replace(/^[#🔒]\s*/, '')}!`}
                      text="No messages yet in this channel. Start the conversation or share files below."
                    />
                  )}
                </div>

                {/* Floating Context Menu for Community Message (Requirement 6, 11, 12) */}
                {msgContextMenu && (
                  <div
                    className="message-context-menu"
                    style={{
                      position: 'fixed',
                      top: msgContextMenu.y,
                      left: msgContextMenu.x,
                      zIndex: 1200,
                      background: 'var(--surface)',
                      border: '1px solid var(--line)',
                      borderRadius: 8,
                      boxShadow: '0 10px 30px rgba(0,0,0,0.18)',
                    }}
                    onClick={(e) => e.stopPropagation()}
                  >
                    {!msgContextMenu.message.deleted && (
                      <div className="context-menu-reactions">
                        {['👍', '❤️', '🔥', '🚀', '💡'].map((em) => (
                          <button
                            key={em}
                            type="button"
                            onClick={() => handleAddReaction(msgContextMenu.message.id, em)}
                            title={`React ${em}`}
                          >
                            {em}
                          </button>
                        ))}
                        <button
                          type="button"
                          onClick={() => setShowReactionPicker(!showReactionPicker)}
                          title="More reactions"
                          style={{ fontSize: 14, fontWeight: 700 }}
                        >
                          +
                        </button>
                      </div>
                    )}

                    {showReactionPicker && !msgContextMenu.message.deleted && (
                      <div
                        style={{
                          display: 'grid',
                          gridTemplateColumns: 'repeat(5, 1fr)',
                          gap: 4,
                          padding: '6px 8px',
                          borderBottom: '1px solid var(--line)',
                          background: 'var(--soft)',
                        }}
                      >
                        {['🎉', '👏', '👀', '💯', '🙌', '✨', '🤝', '⚡', '☕', '🥳'].map((em) => (
                          <button
                            key={em}
                            type="button"
                            onClick={() => handleAddReaction(msgContextMenu.message.id, em)}
                            style={{
                              background: 'none',
                              border: 0,
                              fontSize: 16,
                              cursor: 'pointer',
                              padding: 2,
                            }}
                          >
                            {em}
                          </button>
                        ))}
                      </div>
                    )}

                    {!msgContextMenu.message.deleted && (
                      <button
                        type="button"
                        className="context-menu-item"
                        onClick={() => {
                          navigator.clipboard?.writeText?.(msgContextMenu.message.body || '');
                          app.toast('Message copied to clipboard');
                          setMsgContextMenu(null);
                        }}
                      >
                        <Copy size={15} />
                        <span>Copy message</span>
                      </button>
                    )}

                    {!msgContextMenu.message.deleted && (
                      <button
                        type="button"
                        className="context-menu-item"
                        onClick={() => {
                          setReplyingTo(msgContextMenu.message);
                          setMsgContextMenu(null);
                        }}
                      >
                        <Reply size={15} />
                        <span>Reply</span>
                      </button>
                    )}

                    {!msgContextMenu.message.deleted && (
                      <button
                        type="button"
                        className="context-menu-item"
                        onClick={() => {
                          const quote = `Forwarded from ${msgContextMenu.message.sender}: "${msgContextMenu.message.body}"`;
                          navigator.clipboard?.writeText?.(quote);
                          app.toast('Forward quote copied! Paste into any chat.');
                          setMsgContextMenu(null);
                        }}
                      >
                        <Forward size={15} />
                        <span>Forward</span>
                      </button>
                    )}

                    {!msgContextMenu.message.deleted && (
                      <button
                        type="button"
                        className="context-menu-item"
                        onClick={() => {
                          const isPinned = pinnedMessageIds.includes(msgContextMenu.message.id);
                          setPinnedMessageIds((prev) =>
                            isPinned
                              ? prev.filter((id) => id !== msgContextMenu.message.id)
                              : [...prev, msgContextMenu.message.id]
                          );
                          app.toast(isPinned ? 'Message unpinned' : 'Message pinned to channel');
                          setMsgContextMenu(null);
                        }}
                      >
                        <Pin size={15} />
                        <span>{pinnedMessageIds.includes(msgContextMenu.message.id) ? 'Unpin message' : 'Pin to channel'}</span>
                      </button>
                    )}

                    {msgContextMenu.message.sender === app.profile.name && !msgContextMenu.message.deleted && (
                      <button
                        type="button"
                        className="context-menu-item"
                        onClick={() => {
                          setEditingMessage(msgContextMenu.message);
                          setDraft(msgContextMenu.message.body || '');
                          setMsgContextMenu(null);
                        }}
                      >
                        <Edit3 size={15} />
                        <span>Edit message</span>
                      </button>
                    )}

                    {(msgContextMenu.message.sender === app.profile.name || userRole === 'Admin' || userRole === 'Sub-admin') && !msgContextMenu.message.deleted && (
                      <button
                        type="button"
                        className="context-menu-item danger"
                        onClick={() => {
                          if (window.confirm('Are you sure you want to delete this message?')) {
                            setChatMessages((prev) =>
                              prev.map((m) =>
                                m.id === msgContextMenu.message.id
                                  ? { ...m, body: 'This message was deleted', deleted: true, media: null, reactions: {} }
                                  : m
                              )
                            );
                            app.toast('Message deleted');
                            setMsgContextMenu(null);
                          }
                        }}
                      >
                        <Trash2 size={15} />
                        <span>Delete message</span>
                      </button>
                    )}
                  </div>
                )}

                {/* Floating Context Menu for Channel in Sidebar (Requirement 5) */}
                {channelContextMenu && (
                  <div
                    className="message-context-menu"
                    style={{
                      position: 'fixed',
                      top: channelContextMenu.y,
                      left: channelContextMenu.x,
                      zIndex: 1200,
                      minWidth: 170,
                    }}
                    onClick={(e) => e.stopPropagation()}
                  >
                    <button
                      type="button"
                      className="context-menu-item"
                      onClick={() => {
                        const targetName = channelContextMenu.channel.name;
                        const isPinned = pinnedChannelNames.includes(targetName);
                        setPinnedChannelNames((prev) =>
                          isPinned ? prev.filter((n) => n !== targetName) : [...prev, targetName]
                        );
                        app.toast(isPinned ? `Unpinned ${targetName}` : `Pinned ${targetName} to top`);
                        setChannelContextMenu(null);
                      }}
                    >
                      <Pin size={15} />
                      <span>{pinnedChannelNames.includes(channelContextMenu.channel.name) ? 'Unpin channel' : 'Pin channel on top'}</span>
                    </button>
                    <button
                      type="button"
                      className="context-menu-item"
                      onClick={() => {
                        setActiveChannel(channelContextMenu.channel.name);
                        setShowChannelProfileModal(true);
                        setChannelContextMenu(null);
                      }}
                    >
                      <Settings size={15} />
                      <span>Channel details</span>
                    </button>
                  </div>
                )}

                {/* Input Bar with Media Upload, Tag Selector & Poll Creator (+ menu on left) */}
                <div className="chat-input-container">
                  {/* Hidden file input */}
                  <input
                    type="file"
                    ref={fileInputRef}
                    style={{ display: 'none' }}
                    onChange={handleFileSelect}
                  />

                  {/* Replying banner */}
                  {replyingTo && (
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '6px 12px',
                        background: 'var(--soft)',
                        borderTop: '1px solid var(--line)',
                        fontSize: 12,
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <Reply size={13} color="var(--coral)" />
                        <span>
                          Replying to <b>{replyingTo.sender}</b>: <i>{replyingTo.body?.slice(0, 40)}...</i>
                        </span>
                      </div>
                      <button
                        type="button"
                        onClick={() => setReplyingTo(null)}
                        style={{ border: 0, background: 'none', cursor: 'pointer', color: 'var(--coral)' }}
                      >
                        <X size={14} />
                      </button>
                    </div>
                  )}

                  {/* Editing banner */}
                  {editingMessage && (
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '6px 12px',
                        background: 'rgba(59,130,246,0.1)',
                        borderTop: '1px solid var(--line)',
                        fontSize: 12,
                        color: 'var(--ink)',
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <Edit3 size={13} color="#3b82f6" />
                        <span>Editing message</span>
                      </div>
                      <button
                        type="button"
                        onClick={() => {
                          setEditingMessage(null);
                          setDraft('');
                        }}
                        style={{ border: 0, background: 'none', cursor: 'pointer', color: 'var(--coral)' }}
                      >
                        <X size={14} />
                      </button>
                    </div>
                  )}

                  {/* Attached file preview chip with Tag Selector (Requirement 13) */}
                  {attachedMedia && (
                    <div className="chat-attachment-preview">
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, minWidth: 0, flex: 1 }}>
                        {attachedMedia.type === 'image' ? (
                          <img src={attachedMedia.url} alt="" />
                        ) : (
                          <FileText size={20} color="var(--coral)" />
                        )}
                        <div style={{ flex: 1, minWidth: 0 }}>
                          <b style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block' }}>{attachedMedia.name}</b>
                          <small style={{ display: 'block', color: 'var(--muted)' }}>
                            {attachedMedia.size} · Ready to send
                          </small>
                        </div>
                      </div>

                      {/* Tag selector dropdown */}
                      <div className="chat-attach-tag-wrap" style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                        <span style={{ fontSize: 11, color: 'var(--muted)', fontWeight: 600 }}>Tag:</span>
                        <select
                          value={attachedMediaTag}
                          onChange={(e) => {
                            setAttachedMediaTag(e.target.value);
                            setAttachedMedia((prev) => prev ? { ...prev, tag: e.target.value } : null);
                          }}
                          style={{
                            fontSize: 11,
                            padding: '3px 6px',
                            borderRadius: 6,
                            border: '1px solid var(--line)',
                            background: 'var(--surface)',
                            color: 'var(--ink)',
                            cursor: 'pointer',
                            maxWidth: 130,
                          }}
                          title="Assign a tag to filter this resource later"
                        >
                          <option value="Guides & Docs">Guides &amp; Docs</option>
                          <option value="Design Assets">Design Assets</option>
                          <option value="Templates">Templates</option>
                          <option value="Architecture">Architecture</option>
                          <option value="Frontend">Frontend</option>
                          <option value="Backend">Backend</option>
                          <option value="Notes">Notes</option>
                        </select>
                      </div>

                      <button
                        type="button"
                        style={{ border: 0, background: 'none', cursor: 'pointer', color: 'var(--coral)', flexShrink: 0 }}
                        onClick={() => setAttachedMedia(null)}
                      >
                        <X size={16} />
                      </button>
                    </div>
                  )}

                  {/* Main input bar with + button on the left */}
                  <div className="chat-input-bar" style={{ position: 'relative', display: 'flex', alignItems: 'center', gap: 8 }}>
                    {/* + Icon button */}
                    <button
                      type="button"
                      className="chat-plus-toggle-btn"
                      onClick={() => setShowPlusMenu(!showPlusMenu)}
                      title="Attach file, share image, or post poll"
                      style={{
                        width: 36,
                        height: 36,
                        borderRadius: '50%',
                        border: '1px solid var(--line)',
                        background: showPlusMenu ? 'var(--coral)' : 'var(--surface)',
                        color: showPlusMenu ? '#fff' : 'var(--ink)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        cursor: 'pointer',
                        transition: 'all 0.2s',
                        flexShrink: 0,
                      }}
                    >
                      <Plus size={18} style={{ transform: showPlusMenu ? 'rotate(45deg)' : 'none', transition: 'transform 0.2s' }} />
                    </button>

                    {/* Pop-up menu when + is clicked */}
                    {showPlusMenu && (
                      <div
                        className="chat-plus-popover"
                        style={{
                          position: 'absolute',
                          bottom: 48,
                          left: 0,
                          zIndex: 100,
                          background: 'var(--surface)',
                          border: '1px solid var(--line)',
                          borderRadius: 12,
                          boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
                          padding: 6,
                          display: 'flex',
                          flexDirection: 'column',
                          gap: 4,
                          minWidth: 175,
                        }}
                      >
                        <button
                          type="button"
                          className="chat-popup-item"
                          onClick={() => {
                            setShowPlusMenu(false);
                            fileInputRef.current?.click();
                          }}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 10,
                            padding: '8px 12px',
                            background: 'none',
                            border: 0,
                            borderRadius: 8,
                            cursor: 'pointer',
                            color: 'var(--ink)',
                            fontSize: 13,
                            fontWeight: 600,
                            textAlign: 'left',
                          }}
                        >
                          <Paperclip size={16} color="var(--coral)" />
                          <span>Attach file</span>
                        </button>
                        <button
                          type="button"
                          className="chat-popup-item"
                          onClick={() => {
                            setShowPlusMenu(false);
                            handleAttachPreset('image');
                          }}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 10,
                            padding: '8px 12px',
                            background: 'none',
                            border: 0,
                            borderRadius: 8,
                            cursor: 'pointer',
                            color: 'var(--ink)',
                            fontSize: 13,
                            fontWeight: 600,
                            textAlign: 'left',
                          }}
                        >
                          <Image size={16} color="#3b82f6" />
                          <span>Share image</span>
                        </button>
                        <button
                          type="button"
                          className="chat-popup-item"
                          onClick={() => {
                            setShowPlusMenu(false);
                            setShowPostPollModal(true);
                          }}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: 10,
                            padding: '8px 12px',
                            background: 'none',
                            border: 0,
                            borderRadius: 8,
                            cursor: 'pointer',
                            color: 'var(--ink)',
                            fontSize: 13,
                            fontWeight: 600,
                            textAlign: 'left',
                          }}
                        >
                          <BarChart3 size={16} color="#10b981" />
                          <span>Post poll</span>
                        </button>
                      </div>
                    )}

                    <input
                      value={draft}
                      onChange={(e) => setDraft(e.target.value)}
                      placeholder={`Message ${activeChannel.replace(/^[#🔒]\s*/, '')}...`}
                      style={{ flex: 1, minWidth: 0 }}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') handleSendMessage();
                      }}
                    />
                    <Button
                      variant="primary"
                      className="chat-send-action-btn"
                      onClick={handleSendMessage}
                      disabled={!draft.trim() && !attachedMedia}
                    >
                      {editingMessage ? 'Save' : 'Send'}
                    </Button>
                  </div>
                </div>
              </>
            )}
          </section>
        </div>
      )}

      {/* ─── CHANNELS TAB ─── */}
      {view === 'Channels' && (
        <section className="content-editor">
          <div className="community-tab-header">
            <div>
              <h3>Community Channels</h3>
              <p className="muted">
                Create public or private channels for focused discussions.
              </p>
            </div>
            {canCreateRooms ? (
              <Button icon={Plus} onClick={() => setShowCreateChannelModal(true)}>
                Create channel
              </Button>
            ) : (
              <Tag tone="purple">Creation restricted to {communitySettings.roomCreationPermission}</Tag>
            )}
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {channels.map((ch) => {
              const isRoomAdminForCh =
                userRole === 'Admin' ||
                ch.creatorName === app.profile.name ||
                ch.roomAdmins?.includes(app.profile.name);
              return (
                <div
                  key={ch.id}
                  className="community-channel-card"
                >
                  <div className="community-channel-info">
                    <span className="community-channel-icon">
                      {ch.isPrivate ? <Lock size={18} /> : <Users size={18} />}
                    </span>
                    <div style={{ minWidth: 0, flex: 1 }}>
                      <div className="community-channel-title-row">
                        <b style={{ fontSize: 14 }}>{ch.name.replace(/^[#🔒]\s*/, '')}</b>
                        {ch.isDefault && <Tag>Main Channel</Tag>}
                        {isRoomAdminForCh && (
                          <span className="community-channel-admin-tag">
                            ★ Channel Admin ({ch.creatorName === app.profile.name ? 'You' : ch.creatorName})
                          </span>
                        )}
                      </div>
                      <small style={{ display: 'block', color: 'var(--muted)', marginTop: 2 }}>
                        {ch.topic}
                      </small>
                    </div>
                  </div>

                  <div className="community-channel-actions">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => {
                        setActiveChannel(ch.name);
                        setChatSubView('stream');
                        setView('Chat');
                      }}
                    >
                      Open Channel
                    </Button>
                    {isRoomAdminForCh && (
                      <Button
                        variant="ghost"
                        size="sm"
                        icon={Settings}
                        onClick={() => {
                          setActiveChannel(ch.name);
                          setShowChannelProfileModal(true);
                        }}
                      >
                        Manage
                      </Button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* ─── MEMBERS TAB (Requirement 6 & 8: Roles & Member Removal) ─── */}
      {view === 'Members' && (
        <section className="person-list">
          <div className="community-tab-header">
            <div>
              <h3>Community Members ({membersList.length})</h3>
              <p className="muted">
                Defined community roles: Member, Sub-admin, and Community Admin.
              </p>
            </div>
            <div className="community-tab-header-actions" style={{ display: 'flex', gap: 8 }}>
              {userRole === 'Admin' && (
                <Button
                  variant="ghost"
                  icon={Crown}
                  onClick={() => setShowTransferAdminModal(true)}
                >
                  Transfer Admin Position
                </Button>
              )}
              <Button icon={UserPlus} onClick={() => setShowAddMemberModal(true)}>
                Add member
              </Button>
            </div>
          </div>

          {membersList.map((m) => (
            <article key={m.id} className="community-member-card">
              <div className="community-member-info">
                <div
                  onClick={() => app.openUserProfile?.(m.name, 'Posts')}
                  style={{ cursor: 'pointer', flexShrink: 0 }}
                  title={`View ${m.name}'s profile`}
                >
                  <Avatar name={m.name} />
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' }}>
                    <b
                      onClick={() => app.openUserProfile?.(m.name, 'Posts')}
                      style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', cursor: 'pointer' }}
                      title={`View ${m.name}'s profile`}
                    >
                      {m.name}
                    </b>
                    {m.name === app.profile.name && <Tag>You</Tag>}
                    <span
                      className={`community-role-badge ${
                        m.role === 'Admin'
                          ? 'role-badge-admin'
                          : m.role === 'Sub-admin'
                          ? 'role-badge-subadmin'
                          : 'role-badge-member'
                      }`}
                    >
                      {m.role}
                    </span>
                  </div>
                  <small style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'block' }}>
                    {m.title} · Joined {m.joined}
                  </small>
                </div>
              </div>

              <div className="community-member-actions">
                {/* Admin role controls (Requirement 6) */}
                {userRole === 'Admin' && m.name !== app.profile.name && (
                  <select
                    value={m.role}
                    onChange={(e) => {
                      const newR = e.target.value;
                      if (newR === 'Admin') {
                        setShowTransferAdminModal(true);
                      } else {
                        setMembersList((prev) =>
                          prev.map((item) =>
                            item.id === m.id ? { ...item, role: newR } : item
                          )
                        );
                        app.toast(`Promoted ${m.name} to ${newR}!`);
                      }
                    }}
                    style={{
                      padding: '5px 8px',
                      borderRadius: 6,
                      border: '1px solid var(--line)',
                      background: 'var(--surface)',
                      fontSize: 12,
                    }}
                  >
                    <option value="Member">Member</option>
                    <option value="Sub-admin">Sub-admin</option>
                    <option value="Admin">Transfer Admin...</option>
                  </select>
                )}

                {/* Remove member option - only Admin or Sub-admin (for regular members) */}
                {((userRole === 'Admin' && m.name !== app.profile.name) ||
                  (userRole === 'Sub-admin' && m.role === 'Member' && m.name !== app.profile.name)) && (
                  <Button
                    size="sm"
                    variant="ghost"
                    className="danger-link"
                    icon={UserMinus}
                    onClick={() => {
                      if (window.confirm(`Are you sure you want to remove ${m.name} from ${communitySettings.title}?`)) {
                        setMembersList((prev) => prev.filter((item) => item.id !== m.id && item.name !== m.name));
                        setChannels((prev) =>
                          prev.map((c) => ({
                            ...c,
                            members: Array.isArray(c.members) ? c.members.filter((n) => n !== m.name) : c.members,
                          }))
                        );
                        app.toast(`Removed ${m.name} from the community.`);
                      }
                    }}
                  >
                    Remove
                  </Button>
                )}
              </div>
            </article>
          ))}
        </section>
      )}

      {/* ─── POLLS TAB (Real-time synced with chat) ─── */}
      {view === 'Polls' && (
        <CommunityPolls
          app={app}
          polls={communityPolls}
          onVote={handleVoteFromTab}
          onCreatePoll={handlePollCreatedFromTab}
        />
      )}

      {/* ─── FILES TAB ─── */}
      {view === 'Files' && (
        <CommunityResourcesHub
          resources={allCommunityResources}
          userRole={userRole}
          onAddResource={() => setShowAddResourceModal(true)}
          onDeleteResource={(id) => {
            setCommunityResources((prev) => prev.filter((r) => r.id !== id));
            app.toast('Resource removed from community profile.');
          }}
          app={app}
        />
      )}

      {/* ─── REQUESTS TAB (Beside Settings: Requirement 3) ─── */}
      {view === 'Requests' && showRequestsTab && (
        <section className="content-editor">
          <div className="community-tab-header">
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                <h3 style={{ margin: 0 }}>Membership Join Requests</h3>
                <span
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    padding: '2px 8px',
                    borderRadius: 999,
                    fontSize: 12,
                    fontWeight: 700,
                    background: joinRequests.length > 0 ? 'var(--coral, #f97316)' : 'var(--line)',
                    color: joinRequests.length > 0 ? '#fff' : 'var(--muted)',
                  }}
                >
                  {joinRequests.length} pending
                </span>
              </div>
              <p className="muted" style={{ margin: '4px 0 0', fontSize: 13 }}>
                Review and approve people requesting to join {communitySettings.title}. Allowing adds them directly as members.
              </p>
            </div>
            {joinRequests.length > 1 && (
              <Button
                variant="primary"
                size="sm"
                icon={Check}
                onClick={handleApproveAllRequests}
              >
                Allow all ({joinRequests.length})
              </Button>
            )}
          </div>

          {joinRequests.length > 0 ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {joinRequests.map((req) => (
                <article
                  key={req.id}
                  className="community-request-card"
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 12,
                    padding: 16,
                    borderRadius: 10,
                    border: '1px solid var(--line)',
                    background: 'var(--surface)',
                  }}
                >
                  <div className="community-request-main-row" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
                    <div className="community-request-person-info" style={{ display: 'flex', gap: 12, alignItems: 'center', minWidth: 0, flex: 1 }}>
                      <Avatar name={req.name} size={42} />
                      <div style={{ minWidth: 0, flex: 1 }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                          <b style={{ fontSize: 15, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{req.name}</b>
                          <span style={{ fontSize: 11, color: 'var(--muted)', background: 'var(--soft)', padding: '2px 6px', borderRadius: 4 }}>
                            {req.requestedAt || 'Recently'}
                          </span>
                        </div>
                        <div style={{ fontSize: 13, color: 'var(--muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          {req.title} {req.email ? `· ${req.email}` : ''}
                        </div>
                      </div>
                    </div>

                    <div className="community-request-actions" style={{ display: 'flex', gap: 8, alignItems: 'center', flexShrink: 0 }}>
                      <Button
                        size="sm"
                        variant="primary"
                        icon={Check}
                        onClick={() => handleApproveRequest(req)}
                      >
                        Allow
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        icon={X}
                        onClick={() => handleDeclineRequest(req)}
                      >
                        Decline
                      </Button>
                    </div>
                  </div>

                  {req.reason && (
                    <div
                      className="community-request-reason"
                      style={{
                        padding: '10px 12px',
                        borderRadius: 6,
                        background: 'var(--canvas, #f8fafc)',
                        border: '1px solid var(--line)',
                        fontSize: 13,
                        color: 'var(--text)',
                        wordBreak: 'break-word',
                      }}
                    >
                      <span style={{ fontWeight: 600, color: 'var(--muted)', fontSize: 11, display: 'block', marginBottom: 2 }}>
                        REASON FOR JOINING:
                      </span>
                      "{req.reason}"
                    </div>
                  )}
                </article>
              ))}
            </div>
          ) : (
            <div
              style={{
                textAlign: 'center',
                padding: '48px 24px',
                border: '1px dashed var(--line)',
                borderRadius: 12,
                background: 'var(--surface)',
              }}
            >
              <Users size={36} style={{ color: 'var(--muted)', margin: '0 auto 12px', display: 'block' }} />
              <h4 style={{ margin: '0 0 6px' }}>No Pending Join Requests</h4>
              <p className="muted" style={{ margin: 0, fontSize: 13 }}>
                All requests to join this community have been reviewed and approved.
              </p>
            </div>
          )}
        </section>
      )}

      {/* ─── SETTINGS TAB (Requirement 4, 6, 9: Save Settings & Permissions) ─── */}
      {view === 'Settings' && (
        <CommunitySettings
          community={community}
          communitySettings={communitySettings}
          userRole={userRole}
          membersList={membersList}
          onSave={handleSaveCommunitySettings}
          onOpenTransferAdmin={() => setShowTransferAdminModal(true)}
          onLeave={handleLeaveCommunity}
          onDelete={handleDeleteCommunity}
        />
      )}

      {/* ─── CREATE CHANNEL MODAL (Requirement 1 & 5) ─── */}
      {showCreateChannelModal && (
        <div
          className="call-modal-overlay"
          onClick={() => setShowCreateChannelModal(false)}
        >
          <div
            className="ticket-pass-modal"
            style={{ textAlign: 'left', padding: 24 }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3 style={{ margin: '0 0 4px', fontSize: 18 }}>Create Channel</h3>
            <p className="muted" style={{ margin: '0 0 16px', fontSize: 13 }}>
              As creator, you will automatically have channel admin privileges for this channel.
            </p>
            <form onSubmit={handleCreateChannel} className="form-stack">
              <Field label="Channel Name">
                <input
                  value={newChannelName}
                  onChange={(e) => setNewChannelName(e.target.value)}
                  placeholder="e.g. frontend-sprint or career-talk"
                  required
                  autoFocus
                />
              </Field>
              <Field label="Topic / Description">
                <input
                  value={newChannelTopic}
                  onChange={(e) => setNewChannelTopic(e.target.value)}
                  placeholder="What is this channel about?"
                />
              </Field>
              <label
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  fontSize: 13,
                  cursor: 'pointer',
                  margin: '8px 0',
                }}
              >
                <input
                  type="checkbox"
                  checked={newChannelPrivate}
                  onChange={(e) => setNewChannelPrivate(e.target.checked)}
                />
                <span>Private channel (Admins &amp; Sub-admins only)</span>
              </label>
              <div style={{ display: 'flex', gap: 10, marginTop: 12 }}>
                <Button type="submit" variant="primary">
                  Create channel
                </Button>
                <Button
                  variant="ghost"
                  type="button"
                  onClick={() => setShowCreateChannelModal(false)}
                >
                  Cancel
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ─── ADD MEMBER MODAL (Requirement 7: Step 1 choose username, Step 2 assign role) ─── */}
      {showAddMemberModal && (
        <AddMemberStepModal
          onClose={() => setShowAddMemberModal(false)}
          onAdd={handleAddMember}
          availableUsers={people}
        />
      )}

      {/* ─── POST POLL IN CHAT MODAL (Requirement 2) ─── */}
      {showPostPollModal && (
        <PostPollModal
          channelName={activeChannel.replace(/^[#🔒]\s*/, '')}
          onClose={() => setShowPostPollModal(false)}
          onSubmit={handlePostPollInChat}
        />
      )}

      {/* ─── CHANNEL SETTINGS MODAL (Requirement 1, 2 & 5) ─── */}
      {showRoomSettingsModal && (
        <RoomSettingsModal
          channel={activeCh}
          isRoomAdmin={isRoomAdmin}
          membersList={membersList}
          currentUserName={app.profile.name}
          onClose={() => setShowRoomSettingsModal(false)}
          onUpdateChannel={(updatedCh) => {
            setChannels((prev) =>
              prev.map((c) => (c.name === activeCh.name ? { ...c, ...updatedCh } : c))
            );
            if (updatedCh.name) setActiveChannel(updatedCh.name);
            setShowRoomSettingsModal(false);
            app.toast('Channel settings updated!');
          }}
          onDeleteChannel={() => {
            if (activeCh.isDefault) {
              app.toast('Default general channel cannot be deleted.');
              return;
            }
            if (window.confirm(`Delete channel "${activeCh.name.replace(/^[#🔒]\s*/, '')}"?`)) {
              setChannels((prev) => prev.filter((c) => c.name !== activeCh.name));
              setActiveChannel('general');
              setShowRoomSettingsModal(false);
              app.toast(`Channel ${activeCh.name.replace(/^[#🔒]\s*/, '')} removed.`);
            }
          }}
        />
      )}

      {/* ─── TRANSFER ADMIN POSITION MODAL (Requirement 6) ─── */}
      {showTransferAdminModal && (
        <TransferAdminModal
          communityTitle={communitySettings.title}
          membersList={membersList.filter((m) => m.name !== app.profile.name)}
          onClose={() => setShowTransferAdminModal(false)}
          onConfirm={handleTransferAdmin}
        />
      )}

      {/* ─── ADD COMMUNITY RESOURCE MODAL (Requirement 8) ─── */}
      {showAddResourceModal && (
        <AddResourceModal
          onClose={() => setShowAddResourceModal(false)}
          onSubmit={handleAddCommunityResource}
        />
      )}

      {/* ─── CHANNEL PROFILE & MANAGEMENT MODAL (Requirement 3, 8 & 9) ─── */}
      {/* ─── CHANNEL PROFILE & MANAGEMENT MODAL (Merged Settings, Add Member, Members List) ─── */}
      {showChannelProfileModal && (
        <ChannelProfileModal
          app={app}
          channel={activeCh}
          channelFilesCount={channelFiles.length}
          isRoomAdmin={isRoomAdmin}
          userRole={userRole}
          membersList={membersList}
          currentUserName={app.profile.name}
          onClose={() => setShowChannelProfileModal(false)}
          onOpenFiles={() => {
            setShowChannelProfileModal(false);
            setChatSubView('resources');
          }}
          onSaveSettings={(updated) => {
            setChannels((prev) =>
              prev.map((c) => (c.name === activeCh.name ? { ...c, ...updated } : c))
            );
            app.toast('Channel settings saved.');
          }}
          onAddMember={(memberName) => {
            setChannels((prev) =>
              prev.map((c) => {
                if (c.name !== activeCh.name) return c;
                const current = (c.members?.includes('All Community Members') || !c.members)
                  ? membersList.map((m) => m.name)
                  : c.members;
                return {
                  ...c,
                  members: [...new Set([...current, memberName])],
                };
              })
            );
            setMembersList((prev) => {
              if (prev.some((m) => m.name.toLowerCase() === memberName.toLowerCase())) return prev;
              return [
                ...prev,
                {
                  id: `u-${Date.now()}`,
                  name: memberName,
                  role: 'Member',
                  title: 'Community Member',
                  joined: 'Just now',
                },
              ];
            });
            app.toast(`Added ${memberName} to channel.`);
          }}
          onLeaveChannel={handleLeaveActiveChannel}
          onRemoveMemberFromChannel={(memberName) => {
            setChannels((prev) =>
              prev.map((c) => {
                if (c.name !== activeCh.name) return c;
                const currentMemList = (c.members?.includes('All Community Members') || !c.members)
                  ? membersList.map((m) => m.name)
                  : c.members;
                return {
                  ...c,
                  members: currentMemList.filter((n) => n !== memberName),
                };
              })
            );
            app.toast(`Removed ${memberName} from channel.`);
          }}
          onDeleteChannel={() => {
            if (activeCh.isDefault) {
              app.toast('Default general channel cannot be deleted.');
              return;
            }
            if (window.confirm(`Are you sure you want to delete channel "${activeCh.name.replace(/^[#🔒]\s*/, '')}"?`)) {
              setChannels((prev) => prev.filter((c) => c.name !== activeCh.name));
              setActiveChannel('general');
              setShowChannelProfileModal(false);
              app.toast(`Channel "${activeCh.name.replace(/^[#🔒]\s*/, '')}" removed.`);
            }
          }}
        />
      )}
    </main>
  );
}

// ─── Subcomponent: 2-Step Add Member Modal (Requirement 7) ───────────────────

function AddMemberStepModal({ onClose, onAdd, availableUsers = [] }) {
  const [step, setStep] = useState(1);
  const [username, setUsername] = useState('');
  const [role, setRole] = useState('Member');
  const [title, setTitle] = useState('');

  const handleNext = (e) => {
    e.preventDefault();
    if (username.trim()) {
      setStep(2);
    }
  };

  const handleFinish = (e) => {
    e.preventDefault();
    if (!username.trim()) return;
    onAdd({
      name: username.trim(),
      role,
      title: title.trim() || 'Community Member',
    });
  };

  return (
    <div className="call-modal-overlay" onClick={onClose}>
      <div
        className="ticket-pass-modal"
        style={{ textAlign: 'left', padding: 24, maxWidth: 480 }}
        onClick={(e) => e.stopPropagation()}
      >
        {step === 1 ? (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
              <h3 style={{ margin: 0, fontSize: 18 }}>Add Community Member (1/2)</h3>
              <Tag tone="purple">Step 1: Username</Tag>
            </div>
            <p className="muted" style={{ margin: '0 0 16px', fontSize: 13 }}>
              Select or type the username of the collaborator you want to invite.
            </p>
            <form onSubmit={handleNext} className="form-stack">
              <Field label="Username / Full Name">
                <input
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="e.g. rohit.deshmukh or Priyanshi"
                  required
                  autoFocus
                />
              </Field>

              {/* Suggestions from platform */}
              <div style={{ marginTop: 8 }}>
                <small style={{ color: 'var(--muted)', display: 'block', marginBottom: 6, fontSize: 11 }}>
                  Quick select platform collaborators:
                </small>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                  {availableUsers.slice(0, 5).map((u) => (
                    <button
                      key={u.id || u.name}
                      type="button"
                      onClick={() => setUsername(u.name)}
                      style={{
                        padding: '4px 8px',
                        borderRadius: 6,
                        border: '1px solid var(--line)',
                        background: username === u.name ? 'var(--coral)' : 'var(--surface)',
                        color: username === u.name ? '#fff' : 'var(--ink)',
                        fontSize: 12,
                        cursor: 'pointer',
                      }}
                    >
                      @{u.name}
                    </button>
                  ))}
                </div>
              </div>

              <div style={{ display: 'flex', gap: 10, marginTop: 18 }}>
                <Button type="submit" variant="primary" disabled={!username.trim()}>
                  Next: Assign Role →
                </Button>
                <Button variant="ghost" type="button" onClick={onClose}>
                  Cancel
                </Button>
              </div>
            </form>
          </div>
        ) : (
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
              <h3 style={{ margin: 0, fontSize: 18 }}>Assign Member Role (2/2)</h3>
              <Tag tone="green">Step 2: Role</Tag>
            </div>
            <p className="muted" style={{ margin: '0 0 16px', fontSize: 13 }}>
              Selected member: <b>@{username}</b>. Set their community permissions.
            </p>
            <form onSubmit={handleFinish} className="form-stack">
              <Field label="Assign Role">
                <select value={role} onChange={(e) => setRole(e.target.value)}>
                  <option value="Member">Member (Default participant)</option>
                  <option value="Sub-admin">Sub-admin (Channel moderation & request review)</option>
                  <option value="Admin">Admin (Full administrative privileges)</option>
                </select>
              </Field>

              <Field label="Title / Headline (Optional)">
                <input
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g. Senior Frontend Dev or Contributor"
                />
              </Field>

              <div style={{ display: 'flex', gap: 10, marginTop: 18 }}>
                <Button type="button" variant="ghost" onClick={() => setStep(1)}>
                  ← Back
                </Button>
                <Button type="submit" variant="primary">
                  Confirm &amp; Add Member
                </Button>
              </div>
            </form>
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Subcomponent: Channel Profile & Management Modal ─────────────────────────

function ChannelProfileModal({
  app,
  channel,
  channelFilesCount,
  isRoomAdmin,
  userRole,
  membersList = [],
  currentUserName,
  onClose,
  onOpenFiles,
  onSaveSettings,
  onAddMember,
  onLeaveChannel,
  onRemoveMemberFromChannel,
  onDeleteChannel,
}) {
  const cleanName = channel.name.replace(/^[#🔒]\s*/, '');
  const [topic, setTopic] = useState(channel.topic || '');
  const [isPrivate, setIsPrivate] = useState(Boolean(channel.isPrivate));
  const [selectedMemberToAdd, setSelectedMemberToAdd] = useState('');
  const [typedMemberName, setTypedMemberName] = useState('');

  const channelMembersNames = (channel.members?.includes('All Community Members') || !channel.members)
    ? membersList.map((m) => m.name)
    : channel.members;

  const currentChannelMembers = membersList.filter((m) =>
    channelMembersNames.includes(m.name)
  );

  const availableToAdd = membersList.filter(
    (m) => !channelMembersNames.includes(m.name)
  );

  const handleSaveSettings = (e) => {
    e.preventDefault();
    onSaveSettings?.({ topic, isPrivate });
  };

  const handleAddMember = (e) => {
    e?.preventDefault();
    const targetName = (typedMemberName || selectedMemberToAdd).trim();
    if (!targetName) return;
    onAddMember?.(targetName);
    setSelectedMemberToAdd('');
    setTypedMemberName('');
  };

  return (
    <div className="call-modal-overlay" onClick={onClose}>
      <div
        className="call-modal"
        style={{
          maxWidth: 540,
          width: '92%',
          textAlign: 'left',
          alignItems: 'stretch',
          background: 'var(--surface)',
          padding: 22,
          color: 'var(--ink)',
          maxHeight: '88vh',
          overflowY: 'auto',
          borderRadius: 12,
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div
              style={{
                width: 42,
                height: 42,
                borderRadius: 8,
                background: channel.isPrivate ? 'rgba(239,68,68,0.1)' : 'rgba(16,185,129,0.1)',
                color: channel.isPrivate ? '#ef4444' : '#10b981',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              {channel.isPrivate ? <Lock size={20} /> : <Users size={20} />}
            </div>
            <div>
              <h3 style={{ margin: 0, fontSize: 18 }}>{cleanName}</h3>
              <p className="muted" style={{ margin: '2px 0 0', fontSize: 12 }}>
                {channel.topic || 'General community discussion channel'}
              </p>
            </div>
          </div>
          <IconButton icon={X} label="Close" onClick={onClose} />
        </div>

        {/* Tags & Direct Files Button */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16, flexWrap: 'wrap', gap: 8 }}>
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
            <Tag tone={channel.isPrivate ? 'rose' : 'green'}>
              {channel.isPrivate ? 'Private Channel' : 'Public Channel'}
            </Tag>
            {isRoomAdmin && <Tag tone="purple">★ Channel Admin</Tag>}
            <Tag tone="neutral">
              By {channel.creatorName === currentUserName ? 'You' : (channel.creatorName || 'Founder')}
            </Tag>
          </div>
          <Button
            size="sm"
            variant="ghost"
            icon={FileText}
            onClick={onOpenFiles}
            style={{ fontSize: 12, fontWeight: 600, flexShrink: 0 }}
          >
            Files ({channelFilesCount})
          </Button>
        </div>

        {/* 1. Channel Settings & Administration */}
        {isRoomAdmin ? (
          <div style={{ background: 'var(--soft)', padding: 14, borderRadius: 8, marginBottom: 16, border: '1px solid var(--line)' }}>
            <b style={{ fontSize: 13, display: 'block', marginBottom: 8, color: 'var(--ink)' }}>
              Channel Settings &amp; Administration
            </b>
            <form onSubmit={handleSaveSettings} style={{ display: 'grid', gap: 10 }}>
              <div>
                <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--muted)', display: 'block', marginBottom: 4 }}>
                  Channel Topic &amp; Purpose
                </label>
                <input
                  type="text"
                  value={topic}
                  onChange={(e) => setTopic(e.target.value)}
                  placeholder="What is this channel about?"
                  style={{
                    width: '100%',
                    padding: '7px 10px',
                    borderRadius: 6,
                    border: '1px solid var(--line)',
                    background: 'var(--surface)',
                    color: 'var(--ink)',
                    fontSize: 13,
                  }}
                />
              </div>
              <div style={{ display: 'flex', gap: 14, alignItems: 'center', marginTop: 2, flexWrap: 'wrap' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, cursor: 'pointer' }}>
                  <input
                    type="radio"
                    name="channelPrivacy"
                    checked={!isPrivate}
                    onChange={() => setIsPrivate(false)}
                  />
                  Public Channel
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12, cursor: 'pointer' }}>
                  <input
                    type="radio"
                    name="channelPrivacy"
                    checked={isPrivate}
                    onChange={() => setIsPrivate(true)}
                  />
                  Private Channel
                </label>
                <Button size="sm" type="submit" variant="primary" style={{ marginLeft: 'auto', fontSize: 12 }}>
                  Save channel settings
                </Button>
              </div>
            </form>
          </div>
        ) : (
          <div style={{ background: 'var(--soft)', padding: 12, borderRadius: 8, marginBottom: 16, border: '1px solid var(--line)', fontSize: 13 }}>
            <b style={{ display: 'block', marginBottom: 4, color: 'var(--ink)', fontSize: 13 }}>About this Channel</b>
            <p style={{ margin: '0 0 6px', color: 'var(--muted)', fontSize: 12 }}>{channel.topic || 'General community discussion channel'}</p>
            <small style={{ color: 'var(--muted)', fontSize: 11 }}>
              {channel.isPrivate ? 'Private Channel · Permitted members only' : 'Public Channel · Open to all community members'}
            </small>
          </div>
        )}

        {/* 2. Add Members to Channel (Room Admins or open channels) */}
        {(isRoomAdmin || !channel.isPrivate) && (
          <div style={{ background: 'var(--soft)', padding: 14, borderRadius: 8, marginBottom: 16, border: '1px solid var(--line)' }}>
            <b style={{ fontSize: 13, display: 'block', marginBottom: 8, color: 'var(--ink)' }}>
              Add Members to Channel
            </b>
            <form onSubmit={handleAddMember} style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {availableToAdd.length > 0 && (
                <select
                  style={{
                    flex: '1 1 180px',
                    padding: '7px 10px',
                    borderRadius: 6,
                    border: '1px solid var(--line)',
                    background: 'var(--surface)',
                    color: 'var(--ink)',
                    fontSize: 13,
                  }}
                  value={selectedMemberToAdd}
                  onChange={(e) => {
                    setSelectedMemberToAdd(e.target.value);
                    if (e.target.value) setTypedMemberName(e.target.value);
                  }}
                >
                  <option value="">Select community member...</option>
                  {availableToAdd.map((m) => (
                    <option key={m.id} value={m.name}>
                      {m.name} ({m.role})
                    </option>
                  ))}
                </select>
              )}
              <input
                type="text"
                placeholder="Or type collaborator username..."
                value={typedMemberName}
                onChange={(e) => {
                  setTypedMemberName(e.target.value);
                  setSelectedMemberToAdd('');
                }}
                style={{
                  flex: '1 1 180px',
                  padding: '7px 10px',
                  borderRadius: 6,
                  border: '1px solid var(--line)',
                  background: 'var(--surface)',
                  color: 'var(--ink)',
                  fontSize: 13,
                }}
              />
              <Button
                size="sm"
                type="submit"
                icon={UserPlus}
                disabled={!typedMemberName.trim() && !selectedMemberToAdd}
                style={{ fontSize: 12, flexShrink: 0 }}
              >
                Add member
              </Button>
            </form>
          </div>
        )}

        {/* 3. Channel Members List */}
        <div>
          <b style={{ fontSize: 13, display: 'block', marginBottom: 8, color: 'var(--muted)' }}>
            CHANNEL MEMBERS ({currentChannelMembers.length})
          </b>
          <div style={{ border: '1px solid var(--line)', borderRadius: 8, maxHeight: 180, overflowY: 'auto', background: 'var(--surface)' }}>
            {currentChannelMembers.map((m) => (
              <div
                key={m.id}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '8px 12px',
                  borderBottom: '1px solid var(--line)',
                  gap: 10,
                }}
              >
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                    minWidth: 0,
                    flex: 1,
                    cursor: app?.openUserProfile ? 'pointer' : 'default',
                  }}
                  onClick={() => app?.openUserProfile?.(m.name)}
                  title={app?.openUserProfile ? `View ${m.name}'s profile` : undefined}
                >
                  <Avatar name={m.name} size="sm" />
                  <div style={{ minWidth: 0 }}>
                    <span style={{ fontSize: 12, fontWeight: 600 }}>{m.name}</span>
                    <span style={{ fontSize: 11, color: 'var(--muted)', marginLeft: 6 }}>({m.role})</span>
                  </div>
                </div>
                {isRoomAdmin && m.name !== channel.creatorName && m.name !== currentUserName && (
                  <Button
                    size="sm"
                    variant="ghost"
                    className="danger-link"
                    style={{ padding: '3px 9px', fontSize: 11, minHeight: 28, flexShrink: 0 }}
                    onClick={() => {
                      if (window.confirm(`Remove ${m.name} from channel "${cleanName}"?`)) {
                        onRemoveMemberFromChannel?.(m.name);
                      }
                    }}
                  >
                    Remove
                  </Button>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Bottom Actions */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 18, flexWrap: 'wrap', gap: 8 }}>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {!channel.isDefault && onLeaveChannel && (
              <Button
                variant="ghost"
                size="sm"
                className="danger-link"
                icon={LogOut}
                onClick={() => {
                  onClose();
                  onLeaveChannel();
                }}
                style={{ fontSize: 12 }}
              >
                Leave channel
              </Button>
            )}
            {isRoomAdmin && !channel.isDefault && onDeleteChannel && (
              <Button
                variant="ghost"
                size="sm"
                className="danger-link"
                icon={Trash2}
                onClick={onDeleteChannel}
                style={{ fontSize: 12 }}
              >
                Delete channel
              </Button>
            )}
          </div>
          <Button variant="ghost" onClick={onClose} style={{ fontSize: 12 }}>
            Close
          </Button>
        </div>
      </div>
    </div>
  );
}

// ─── Subcomponent: Channel Resources Panel (Requirement 4 & 7) ─────────────────

function ChannelResourcesPanel({ channelName, files, onBack, onAttachFile, onShareFiles, app }) {
  const [cat, setCat] = useState('All');
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [resourceContextMenu, setResourceContextMenu] = useState(null);

  useEffect(() => {
    const handleCloseMenu = () => setResourceContextMenu(null);
    window.addEventListener('click', handleCloseMenu);
    return () => window.removeEventListener('click', handleCloseMenu);
  }, []);

  const filtered = useMemo(() => {
    if (cat === 'All') return files;
    if (cat === 'Images') return files.filter((f) => f.type === 'image');
    if (cat === 'Documents') return files.filter((f) => f.type === 'document');
    if (cat === 'Videos') return files.filter((f) => f.type === 'video');
    if (cat === 'Code') return files.filter((f) => f.type === 'code');
    return files;
  }, [files, cat]);

  const toggleSelect = (name) => {
    setSelectedFiles((prev) =>
      prev.includes(name) ? prev.filter((n) => n !== name) : [...prev, name]
    );
  };

  const handleShareSelected = () => {
    if (selectedFiles.length === 0) return;
    if (onShareFiles) {
      onShareFiles(selectedFiles);
    } else {
      app?.toast?.(`Shared ${selectedFiles.length} file(s) into channel chat!`);
      onBack();
    }
    setSelectedFiles([]);
  };

  const handleShareSingle = (fileName) => {
    if (onShareFiles) {
      onShareFiles([fileName]);
    } else {
      app?.toast?.(`Shared ${fileName} into channel chat!`);
      onBack();
    }
  };

  return (
    <div className="channel-resources-panel">
      {/* Header with Back to chat option to the left of the name with left arrow (Requirement 4) */}
      <div className="channel-resources-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12, flexWrap: 'wrap', gap: 10 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0, flex: 1 }}>
          <button
            type="button"
            className="back-to-chat-btn"
            onClick={onBack}
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 6,
              background: 'var(--surface)',
              border: '1px solid var(--line)',
              borderRadius: 8,
              padding: '6px 12px',
              cursor: 'pointer',
              color: 'var(--coral)',
              fontWeight: 600,
              fontSize: 13,
              flexShrink: 0,
            }}
            title="Back to chat"
          >
            <ChevronLeft size={16} /> Back to chat
          </button>
          <div style={{ minWidth: 0, flex: 1 }}>
            <h3 style={{ margin: 0, fontSize: 16, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>Files Shared in {channelName.replace(/^[#🔒]\s*/, '')}</h3>
            <p className="muted" style={{ margin: '2px 0 0', fontSize: 12, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              Showing resources and attachments shared specifically in this channel.
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
          {selectedFiles.length > 0 && (
            <>
              <Button
                size="sm"
                variant="primary"
                icon={Share2}
                onClick={handleShareSelected}
              >
                Share selected ({selectedFiles.length})
              </Button>
              <Button size="sm" variant="ghost" onClick={() => setSelectedFiles([])}>
                Clear
              </Button>
            </>
          )}
          <Button size="sm" icon={Paperclip} onClick={onAttachFile}>
            Upload file
          </Button>
        </div>
      </div>

      {/* Filter chips */}
      <div className="fast-filter-chips">
        {['All', 'Images', 'Documents', 'Videos', 'Code'].map((c) => (
          <button
            key={c}
            className={`filter-chip ${cat === c ? 'active' : ''}`}
            onClick={() => setCat(c)}
          >
            {c} ({c === 'All' ? files.length : files.filter((f) => f.type === c.toLowerCase().slice(0, 4) || (c === 'Documents' && f.type === 'document')).length})
          </button>
        ))}
      </div>

      {filtered.length > 0 ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {filtered.map((file, idx) => (
            <div
              className="channel-resource-item"
              key={idx}
              onContextMenu={(e) => {
                e.preventDefault();
                setResourceContextMenu({
                  x: Math.min(e.clientX, window.innerWidth - 180),
                  y: Math.min(e.clientY, window.innerHeight - 150),
                  file,
                });
              }}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '10px 14px',
                border: '1px solid var(--line)',
                borderRadius: 8,
                background: selectedFiles.includes(file.name) ? 'rgba(234,88,12,0.06)' : 'var(--surface)',
                gap: 10,
              }}
            >
              <div className="channel-resource-info" style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0, flex: 1, overflow: 'hidden' }}>
                {/* Multi-select checkbox (Requirement 4) */}
                <input
                  type="checkbox"
                  checked={selectedFiles.includes(file.name)}
                  onChange={() => toggleSelect(file.name)}
                  style={{ cursor: 'pointer', width: 16, height: 16, flexShrink: 0 }}
                />
                <div className="channel-resource-icon" style={{ flexShrink: 0 }}>
                  {file.type === 'image' ? <Image size={18} /> : file.type === 'video' ? <Video size={18} /> : <FileText size={18} />}
                </div>
                <div style={{ minWidth: 0, flex: 1, overflow: 'hidden' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, minWidth: 0 }}>
                    <b style={{ fontSize: 13, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }} title={file.name}>{file.name}</b>
                    {file.tag && (
                      <span style={{ fontSize: 10, background: 'rgba(234,88,12,0.1)', color: 'var(--coral)', padding: '1px 6px', borderRadius: 4, fontWeight: 600, flexShrink: 0 }}>
                        #{file.tag}
                      </span>
                    )}
                  </div>
                  <small style={{ display: 'block', color: 'var(--muted)', fontSize: 11, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {file.size} · Shared by {file.sender} · {file.time}
                  </small>
                </div>
              </div>
              <div style={{ display: 'flex', gap: 6, flexShrink: 0, alignItems: 'center' }}>
                <Button
                  size="sm"
                  variant="ghost"
                  icon={Share2}
                  onClick={() => handleShareSingle(file.name)}
                >
                  Share
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  icon={Download}
                  onClick={() => app?.toast?.(`Downloading ${file.name}...`)}
                  title="Download file"
                  aria-label="Download file"
                />
              </div>
            </div>
          ))}
        </div>
      ) : (
        <Empty
          title="No files in this channel yet"
          text={`Files and media shared in ${channelName.replace(/^[#🔒]\s*/, '')} will appear here.`}
        />
      )}

      {/* Right-click Floating Context Menu on File (Requirement 4) */}
      {resourceContextMenu && (
        <div
          className="message-context-menu"
          style={{
            position: 'fixed',
            top: resourceContextMenu.y,
            left: resourceContextMenu.x,
            zIndex: 1200,
            minWidth: 160,
          }}
          onClick={(e) => e.stopPropagation()}
        >
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              handleShareSingle(resourceContextMenu.file.name);
              setResourceContextMenu(null);
            }}
          >
            <Share2 size={15} />
            <span>Share file</span>
          </button>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              navigator.clipboard?.writeText?.(resourceContextMenu.file.url || resourceContextMenu.file.name);
              app?.toast?.('File link copied to clipboard!');
              setResourceContextMenu(null);
            }}
          >
            <Copy size={15} />
            <span>Copy file link</span>
          </button>
          <button
            type="button"
            className="context-menu-item"
            onClick={() => {
              app?.toast?.(`Downloading ${resourceContextMenu.file.name}...`);
              setResourceContextMenu(null);
            }}
          >
            <Download size={15} />
            <span>Download</span>
          </button>
        </div>
      )}
    </div>
  );
}

// ─── Subcomponent: In-Chat Poll Card (Requirement 2) ─────────────────────────

function InChatPoll({ poll, onVote }) {
  return (
    <div className="chat-poll-bubble">
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
        <BarChart3 size={15} color="var(--coral)" />
        <span style={{ fontSize: 11, fontWeight: 700, color: 'var(--coral)', textTransform: 'uppercase' }}>
          Interactive Poll
        </span>
      </div>
      <h4>{poll.question}</h4>
      <div>
        {poll.options.map((opt) => {
          const pct =
            poll.totalVotes > 0
              ? Math.round((opt.count / poll.totalVotes) * 100)
              : 0;
          const isSelected = poll.userVoted === opt.label;
          return (
            <button
              key={opt.label}
              type="button"
              className={`chat-poll-option-btn ${isSelected ? 'voted' : ''}`}
              onClick={() => onVote(opt.label)}
            >
              <div
                className="chat-poll-progress"
                style={{ width: `${pct}%` }}
              />
              <div className="chat-poll-text">
                <span>
                  {isSelected && '✓ '}
                  {opt.label}
                </span>
                <b>
                  {pct}% ({opt.count})
                </b>
              </div>
            </button>
          );
        })}
      </div>
      <small style={{ color: 'var(--muted)', display: 'block', marginTop: 6, fontSize: 11 }}>
        {poll.totalVotes} total votes {poll.userVoted ? '· You voted' : '· Click an option to vote'}
      </small>
    </div>
  );
}

// ─── Subcomponent: Post Poll Modal (Requirement 2) ───────────────────────────

function PostPollModal({ channelName, onClose, onSubmit }) {
  const [question, setQuestion] = useState('');
  const [options, setOptions] = useState(['', '']);

  const handleOptionChange = (idx, val) => {
    setOptions((prev) => prev.map((opt, i) => (i === idx ? val : opt)));
  };

  const handleAddOption = () => {
    if (options.length < 8) {
      setOptions((prev) => [...prev, '']);
    }
  };

  const handleRemoveOption = (idx) => {
    if (options.length > 2) {
      setOptions((prev) => prev.filter((_, i) => i !== idx));
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const cleanOptions = options.map((o) => o.trim()).filter(Boolean);
    if (!question.trim() || cleanOptions.length < 2) return;
    onSubmit({ question: question.trim(), options: cleanOptions });
  };

  return (
    <div className="call-modal-overlay" onClick={onClose}>
      <div
        className="ticket-pass-modal"
        style={{ textAlign: 'left', padding: 24 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h3 style={{ margin: '0 0 4px', fontSize: 18 }}>Post Poll to {channelName}</h3>
        <p className="muted" style={{ margin: '0 0 16px', fontSize: 13 }}>
          Members can cast and switch votes directly within the active channel chat stream.
        </p>
        <form onSubmit={handleSubmit} className="form-stack">
          <Field label="Poll Question">
            <input
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="e.g. Which API architecture should we use?"
              required
              autoFocus
            />
          </Field>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <label style={{ fontSize: 12, fontWeight: 700 }}>Poll Options (Min 2)</label>
              {options.length < 8 && (
                <Button type="button" size="sm" variant="ghost" icon={Plus} onClick={handleAddOption}>
                  Add option
                </Button>
              )}
            </div>
            {options.map((opt, idx) => (
              <div key={idx} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                <input
                  value={opt}
                  onChange={(e) => handleOptionChange(idx, e.target.value)}
                  placeholder={`Option ${idx + 1}`}
                  required={idx < 2}
                  style={{ flex: 1, padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ccc)', background: 'var(--color-surface, #fff)', color: 'var(--ink)' }}
                />
                {options.length > 2 && (
                  <button
                    type="button"
                    onClick={() => handleRemoveOption(idx)}
                    style={{ background: 'none', border: 'none', color: 'var(--coral)', cursor: 'pointer', padding: 4 }}
                    title="Remove option"
                  >
                    <X size={16} />
                  </button>
                )}
              </div>
            ))}
          </div>

          <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
            <Button type="submit" variant="primary">
              Post to Chat
            </Button>
            <Button variant="ghost" type="button" onClick={onClose}>
              Cancel
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Subcomponent: Channel Settings & Admin Management Modal (Requirement 1 & 5) 

function RoomSettingsModal({
  channel,
  isRoomAdmin,
  membersList,
  currentUserName,
  onClose,
  onUpdateChannel,
  onDeleteChannel,
}) {
  const [topic, setTopic] = useState(channel.topic || '');
  const [isPrivate, setIsPrivate] = useState(channel.isPrivate || false);
  const [roomAdmins, setRoomAdmins] = useState(channel.roomAdmins || [channel.creatorName]);

  const handleSave = (e) => {
    e.preventDefault();
    onUpdateChannel({
      topic: topic.trim(),
      isPrivate,
      roomAdmins,
    });
  };

  const toggleCoAdmin = (name) => {
    if (roomAdmins.includes(name)) {
      setRoomAdmins((prev) => prev.filter((n) => n !== name));
    } else {
      setRoomAdmins((prev) => [...prev, name]);
    }
  };

  const cleanChannelName = channel.name.replace(/^[#🔒]\s*/, '');

  return (
    <div className="call-modal-overlay" onClick={onClose}>
      <div
        className="ticket-pass-modal"
        style={{ textAlign: 'left', padding: 24, maxWidth: 500 }}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div>
            <h3 style={{ margin: 0, fontSize: 18 }}>{cleanChannelName} Channel Settings</h3>
            <p className="muted" style={{ margin: '2px 0 0', fontSize: 12 }}>
              Created by {channel.creatorName || 'Community Founder'} {channel.creatorName === currentUserName ? '(You)' : ''}
            </p>
          </div>
          {isRoomAdmin && (
            <Tag tone="purple">You are Channel Admin</Tag>
          )}
        </div>

        <form onSubmit={handleSave} className="form-stack">
          <Field label="Channel Topic">
            <input
              value={topic}
              onChange={(e) => setTopic(e.target.value)}
              disabled={!isRoomAdmin}
              placeholder="What this channel is dedicated to"
            />
          </Field>

          <label
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              fontSize: 13,
              cursor: isRoomAdmin ? 'pointer' : 'not-allowed',
            }}
          >
            <input
              type="checkbox"
              checked={isPrivate}
              onChange={(e) => setIsPrivate(e.target.checked)}
              disabled={!isRoomAdmin}
            />
            <span>Private channel (Admins and permitted members only)</span>
          </label>

          <div style={{ marginTop: 14 }}>
            <h4 style={{ fontSize: 13, margin: '0 0 8px' }}>Channel Member &amp; Admin Privileges</h4>
            <div
              style={{
                border: '1px solid var(--line)',
                borderRadius: 8,
                maxHeight: 180,
                overflowY: 'auto',
                background: 'var(--soft)',
              }}
            >
              {membersList.map((m) => {
                const isAdminForThisRoom =
                  m.name === channel.creatorName || roomAdmins.includes(m.name);
                return (
                  <div className="room-settings-member-row" key={m.id}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <Avatar name={m.name} size="sm" />
                      <div>
                        <b style={{ fontSize: 12 }}>{m.name}</b>
                        <small style={{ display: 'block', color: 'var(--muted)', fontSize: 10 }}>
                          {isAdminForThisRoom ? 'Channel Admin' : 'Channel Member'}
                        </small>
                      </div>
                    </div>

                    {isRoomAdmin && m.name !== channel.creatorName && (
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => toggleCoAdmin(m.name)}
                      >
                        {isAdminForThisRoom ? 'Demote to Member' : 'Promote to Co-Admin'}
                      </Button>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 16 }}>
            <div style={{ display: 'flex', gap: 8 }}>
              {isRoomAdmin && (
                <Button type="submit" variant="primary">
                  Save channel settings
                </Button>
              )}
              <Button variant="ghost" type="button" onClick={onClose}>
                {isRoomAdmin ? 'Cancel' : 'Close'}
              </Button>
            </div>

            {isRoomAdmin && !channel.isDefault && (
              <Button
                variant="ghost"
                type="button"
                className="danger-link"
                onClick={onDeleteChannel}
              >
                Delete channel
              </Button>
            )}
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Subcomponent: Transfer Admin Position Modal (Requirement 6) ─────────────

function TransferAdminModal({ communityTitle, membersList, onClose, onConfirm }) {
  const [selectedMember, setSelectedMember] = useState(membersList[0]?.name || '');
  const [transitionChoice, setTransitionChoice] = useState('subadmin'); // 'subadmin' | 'member' | 'leave'

  const handleConfirm = () => {
    if (!selectedMember) return;
    onConfirm(selectedMember, transitionChoice);
  };

  return (
    <div className="call-modal-overlay" onClick={onClose}>
      <div
        className="ticket-pass-modal"
        style={{ textAlign: 'left', padding: 24, maxWidth: 520 }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="admin-transfer-box">
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--coral)', marginBottom: 6 }}>
            <Crown size={20} />
            <h3 style={{ margin: 0, fontSize: 17 }}>Transfer Community Admin Position</h3>
          </div>
          <p style={{ margin: 0, fontSize: 13, color: 'var(--ink)', lineHeight: 1.4 }}>
            Transferring primary Admin ownership of <b>{communityTitle}</b> will grant full administrative privileges to the selected member.
          </p>
        </div>

        <Field label="Select New Community Admin">
          <select
            value={selectedMember}
            onChange={(e) => setSelectedMember(e.target.value)}
          >
            {membersList.map((m) => (
              <option key={m.id} value={m.name}>
                {m.name} ({m.role}) - {m.title}
              </option>
            ))}
          </select>
        </Field>

        <div style={{ marginTop: 12 }}>
          <label style={{ fontSize: 13, fontWeight: 700, display: 'block', marginBottom: 6 }}>
            Choose your transition role after transfer:
          </label>
          <div className="admin-transfer-radio-group">
            <label
              className={`admin-transfer-radio-option ${
                transitionChoice === 'subadmin' ? 'selected' : ''
              }`}
            >
              <input
                type="radio"
                name="transition"
                value="subadmin"
                checked={transitionChoice === 'subadmin'}
                onChange={() => setTransitionChoice('subadmin')}
              />
              <div>
                <b>Step down to Sub-admin</b>
                <small style={{ display: 'block', color: 'var(--muted)' }}>
                  Remain active in the community with channel moderation and resource creation rights.
                </small>
              </div>
            </label>

            <label
              className={`admin-transfer-radio-option ${
                transitionChoice === 'member' ? 'selected' : ''
              }`}
            >
              <input
                type="radio"
                name="transition"
                value="member"
                checked={transitionChoice === 'member'}
                onChange={() => setTransitionChoice('member')}
              />
              <div>
                <b>Step down to Regular Member</b>
                <small style={{ display: 'block', color: 'var(--muted)' }}>
                  Stay in the community as a regular member without management roles.
                </small>
              </div>
            </label>

            <label
              className={`admin-transfer-radio-option ${
                transitionChoice === 'leave' ? 'selected' : ''
              }`}
            >
              <input
                type="radio"
                name="transition"
                value="leave"
                checked={transitionChoice === 'leave'}
                onChange={() => setTransitionChoice('leave')}
              />
              <div>
                <b style={{ color: 'var(--coral)' }}>Leave the Community</b>
                <small style={{ display: 'block', color: 'var(--muted)' }}>
                  Relinquish all roles and completely exit this community upon transfer.
                </small>
              </div>
            </label>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
          <Button variant="primary" onClick={handleConfirm} disabled={!selectedMember}>
            Confirm Transfer
          </Button>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
        </div>
      </div>
    </div>
  );
}

// ─── Subcomponent: Official Community Resources Hub (Requirement 8 & 13) ──────────

function CommunityResourcesHub({ resources, userRole, onAddResource, onDeleteResource, app }) {
  const [filterCat, setFilterCat] = useState('All');
  const [search, setSearch] = useState('');

  const categories = [
    'All',
    'Guides & Docs',
    'Design Assets',
    'Templates',
    'Architecture',
    'Frontend',
    'Backend',
    'Notes',
    'Official Assets',
  ];

  const filtered = resources.filter((res) => {
    const resTag = res.tag || res.category;
    const matchesCat = filterCat === 'All' || resTag === filterCat;
    const matchesQuery =
      `${res.title} ${res.description} ${resTag} ${res.channel || ''}`.toLowerCase().includes(search.toLowerCase());
    return matchesCat && matchesQuery;
  });

  const canManageResources = userRole === 'Admin' || userRole === 'Sub-admin';

  return (
    <section>
      <div
        className="community-tab-header"
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <div>
          <h3>Community Resources &amp; Media</h3>
          <p className="muted">
            Curated playbooks, starter kits, design assets, and files shared across community channels with tags.
          </p>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 14 }}>
        <div style={{ flex: 1, minWidth: 'min(100%, 240px)' }}>
          <SearchField
            value={search}
            onChange={setSearch}
            placeholder="Search community resources, guides, templates, tags..."
          />
        </div>
        <div className="fast-filter-chips" style={{ padding: 0 }}>
          {categories.map((c) => (
            <button
              key={c}
              className={`filter-chip ${filterCat === c ? 'active' : ''}`}
              onClick={() => setFilterCat(c)}
            >
              {c}
            </button>
          ))}
        </div>
      </div>

      {/* Resource Cards Grid */}
      {filtered.length > 0 ? (
        <div className="community-resources-grid">
          {filtered.map((r) => (
            <div className="community-resource-card" key={r.id}>
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                  <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' }}>
                    <span className="resource-badge">#{r.tag || r.category}</span>
                    {r.channel && (
                      <span style={{ fontSize: 10, color: 'var(--muted)', background: 'var(--soft)', padding: '2px 6px', borderRadius: 4 }}>
                        From {r.channel}
                      </span>
                    )}
                  </div>
                  <small style={{ color: 'var(--muted)', fontSize: 11 }}>{r.size}</small>
                </div>
                <h4 style={{ margin: '0 0 6px', fontSize: 15, color: 'var(--ink)' }}>{r.title}</h4>
                <p style={{ margin: 0, fontSize: 12, color: 'var(--muted)', lineHeight: 1.45 }}>
                  {r.description}
                </p>
              </div>

              <div style={{ borderTop: '1px solid var(--line)', paddingTop: 10, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <small style={{ color: 'var(--muted)', fontSize: 11 }}>
                  By {r.author} · {r.addedAt}
                </small>
                <div style={{ display: 'flex', gap: 6 }}>
                  <Button
                    size="sm"
                    variant="ghost"
                    icon={Download}
                    onClick={() => app?.toast?.(`Downloading ${r.title}...`)}
                    title="Download"
                    aria-label="Download"
                    style={{ flexShrink: 0 }}
                  />
                  {canManageResources && (
                    <button
                      type="button"
                      style={{ border: 0, background: 'none', cursor: 'pointer', color: 'var(--coral)' }}
                      onClick={() => onDeleteResource(r.id)}
                      title="Remove resource"
                    >
                      <Trash2 size={15} />
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <Empty
          title="No resources found"
          text="No official resources match your filter criteria."
        />
      )}
    </section>
  );
}

// ─── Subcomponent: Add Community Resource Modal (Requirement 8) ──────────────

function AddResourceModal({ onClose, onSubmit }) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('Guides & Docs');
  const [type, setType] = useState('PDF Document');
  const [size, setSize] = useState('1.5 MB');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!title.trim()) return;
    onSubmit({
      title: title.trim(),
      description: description.trim() || 'Official community resource.',
      category,
      type,
      size,
      url: '#',
    });
  };

  return (
    <div className="call-modal-overlay" onClick={onClose}>
      <div
        className="ticket-pass-modal"
        style={{ textAlign: 'left', padding: 24, maxWidth: 500 }}
        onClick={(e) => e.stopPropagation()}
      >
        <h3 style={{ margin: '0 0 4px', fontSize: 18 }}>Add Official Community Resource</h3>
        <p className="muted" style={{ margin: '0 0 16px', fontSize: 13 }}>
          Resources added here are published under the community profile for all members.
        </p>
        <form onSubmit={handleSubmit} className="form-stack">
          <Field label="Resource Title">
            <input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Flutter 3.x Production Checklist"
              required
              autoFocus
            />
          </Field>
          <Field label="Category">
            <select value={category} onChange={(e) => setCategory(e.target.value)}>
              <option value="Guides & Docs">Guides &amp; Docs</option>
              <option value="Design Assets">Design Assets</option>
              <option value="Templates">Templates</option>
              <option value="Official Assets">Official Assets</option>
            </select>
          </Field>
          <Field label="Resource Type & File Size">
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
              <select value={type} onChange={(e) => setType(e.target.value)}>
                <option value="PDF Document">PDF Document</option>
                <option value="Figma File">Figma File</option>
                <option value="ZIP Archive">ZIP Archive</option>
                <option value="Code Repo">Code Repo</option>
                <option value="Spreadsheet">Spreadsheet</option>
              </select>
              <input
                value={size}
                onChange={(e) => setSize(e.target.value)}
                placeholder="e.g. 2.4 MB"
              />
            </div>
          </Field>
          <Field label="Short Description">
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="What does this resource help members achieve?"
              rows={3}
            />
          </Field>
          <div style={{ display: 'flex', gap: 10, marginTop: 12 }}>
            <Button type="submit" variant="primary">
              Publish Resource
            </Button>
            <Button variant="ghost" type="button" onClick={onClose}>
              Cancel
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}

// ─── Subcomponent: Community Polls (Global Tab) ──────────────────────────────

function CommunityPolls({ app, polls: propsPolls, onVote, onCreatePoll }) {
  const [localPolls, setLocalPolls] = useState([
    {
      id: 'poll-1',
      question: 'Which state-management approach do you prefer for large applications?',
      totalVotes: 86,
      userVoted: 'Riverpod',
      options: [
        { label: 'Riverpod', count: 42 },
        { label: 'Provider', count: 24 },
        { label: 'Bloc / Cubit', count: 16 },
        { label: 'GetX', count: 4 },
      ],
    },
    {
      id: 'poll-2',
      question: 'Should our next live workshop focus on AI Agents or Flutter 3.x internals?',
      totalVotes: 58,
      userVoted: null,
      options: [
        { label: 'Autonomous AI Agents with Python & Ollama', count: 35 },
        { label: 'Flutter 3.x RenderObjects & Performance', count: 18 },
        { label: 'Fullstack GraphQL & Microservices', count: 5 },
      ],
    },
  ]);

  const polls = propsPolls || localPolls;

  const [showCreatePoll, setShowCreatePoll] = useState(false);
  const [question, setQuestion] = useState('');
  const [options, setOptions] = useState(['', '']);

  const handleOptionChange = (idx, val) => {
    setOptions((prev) => prev.map((opt, i) => (i === idx ? val : opt)));
  };

  const handleAddOption = () => {
    if (options.length < 8) {
      setOptions((prev) => [...prev, '']);
    }
  };

  const handleRemoveOption = (idx) => {
    if (options.length > 2) {
      setOptions((prev) => prev.filter((_, i) => i !== idx));
    }
  };

  const handleVote = (pollId, optionLabel) => {
    if (onVote) {
      onVote(pollId, optionLabel);
    } else {
      setLocalPolls((prev) =>
        prev.map((poll) => {
          if (poll.id !== pollId) return poll;
          if (poll.userVoted === optionLabel) return poll;
          const prevVote = poll.userVoted;
          const newTotal = prevVote ? poll.totalVotes : poll.totalVotes + 1;
          const newOptions = poll.options.map((opt) => {
            if (opt.label === optionLabel) return { ...opt, count: opt.count + 1 };
            if (prevVote && opt.label === prevVote) return { ...opt, count: Math.max(0, opt.count - 1) };
            return opt;
          });
          return {
            ...poll,
            totalVotes: newTotal,
            userVoted: optionLabel,
            options: newOptions,
          };
        })
      );
      app?.toast?.('Vote recorded!');
    }
  };

  const handleCreatePoll = (e) => {
    e.preventDefault();
    const cleanOptions = options.map((o) => o.trim()).filter(Boolean);
    if (!question.trim() || cleanOptions.length < 2) return;
    const newP = {
      id: `poll-${Date.now()}`,
      question: question.trim(),
      totalVotes: 0,
      userVoted: null,
      options: cleanOptions.map((opt) => ({ label: opt, count: 0 })),
    };
    if (onCreatePoll) {
      onCreatePoll(newP);
    } else {
      setLocalPolls((prev) => [newP, ...prev]);
      app?.toast?.('Poll created and published!');
    }
    setShowCreatePoll(false);
    setQuestion('');
    setOptions(['', '']);
  };

  return (
    <section>
      <div
        className="community-tab-header"
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: 16,
        }}
      >
        <div>
          <h3>Community Polls &amp; Votes</h3>
          <p className="muted">
            Vote on discussions or launch a community inquiry.
          </p>
        </div>
        <Button icon={Plus} onClick={() => setShowCreatePoll(true)}>
          Create new poll
        </Button>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {polls.map((poll) => (
          <div className="poll-interactive-card" key={poll.id}>
            <h3>{poll.question}</h3>
            <div>
              {poll.options.map((opt) => {
                const pct =
                  poll.totalVotes > 0
                    ? Math.round((opt.count / poll.totalVotes) * 100)
                    : 0;
                const isSelected = poll.userVoted === opt.label;
                return (
                  <button
                    key={opt.label}
                    type="button"
                    className={`poll-option-btn ${isSelected ? 'voted' : ''}`}
                    onClick={() => handleVote(poll.id, opt.label)}
                  >
                    <div
                      className="poll-progress-fill"
                      style={{ width: `${pct}%` }}
                    />
                    <div className="poll-option-content">
                      <span>
                        {isSelected && '✓ '}
                        {opt.label}
                      </span>
                      <b>
                        {pct}% ({opt.count})
                      </b>
                    </div>
                  </button>
                );
              })}
            </div>
            <small style={{ color: 'var(--muted)', marginTop: 8, display: 'block' }}>
              {poll.totalVotes} total votes {poll.userVoted ? '· You have voted' : '· Click an option to vote'}
            </small>
          </div>
        ))}
      </div>

      {showCreatePoll && (
        <div
          className="call-modal-overlay"
          onClick={() => setShowCreatePoll(false)}
        >
          <div
            className="ticket-pass-modal"
            style={{ textAlign: 'left', padding: 24 }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3 style={{ margin: '0 0 4px', fontSize: 18 }}>Create Community Poll</h3>
            <p className="muted" style={{ margin: '0 0 16px', fontSize: 13 }}>
              Ask a question and provide choices for member voting.
            </p>
            <form onSubmit={handleCreatePoll} className="form-stack">
              <Field label="Poll Question">
                <input
                  value={question}
                  onChange={(e) => setQuestion(e.target.value)}
                  placeholder="e.g. Which tool should we adopt?"
                  required
                  autoFocus
                />
              </Field>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <label style={{ fontSize: 12, fontWeight: 700 }}>Poll Options (Min 2)</label>
                  {options.length < 8 && (
                    <Button type="button" size="sm" variant="ghost" icon={Plus} onClick={handleAddOption}>
                      Add option
                    </Button>
                  )}
                </div>
                {options.map((opt, idx) => (
                  <div key={idx} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <input
                      value={opt}
                      onChange={(e) => handleOptionChange(idx, e.target.value)}
                      placeholder={`Option ${idx + 1}`}
                      required={idx < 2}
                      style={{ flex: 1, padding: '8px 10px', borderRadius: 6, border: '1px solid var(--line)', background: 'var(--surface)', color: 'var(--ink)' }}
                    />
                    {options.length > 2 && (
                      <button
                        type="button"
                        onClick={() => handleRemoveOption(idx)}
                        style={{ background: 'none', border: 'none', color: 'var(--coral)', cursor: 'pointer', padding: 4 }}
                        title="Remove option"
                      >
                        <X size={16} />
                      </button>
                    )}
                  </div>
                ))}
              </div>
              <div style={{ display: 'flex', gap: 10, marginTop: 12 }}>
                <Button type="submit" variant="primary">
                  Publish poll
                </Button>
                <Button
                  variant="ghost"
                  type="button"
                  onClick={() => setShowCreatePoll(false)}
                >
                  Cancel
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}

// ─── Subcomponent: Community Settings (Requirement 4, 6, 9) ───────────────────

function CommunitySettings({
  community,
  communitySettings,
  userRole,
  membersList,
  onSave,
  onOpenTransferAdmin,
  onLeave,
  onDelete,
}) {
  const [title, setTitle] = useState(communitySettings.title);
  const [description, setDescription] = useState(communitySettings.description);
  const [privacy, setPrivacy] = useState(communitySettings.privacy);
  const [needsApproval, setNeedsApproval] = useState(
    communitySettings.needsApproval !== false
  );
  const [allowSubadminsApproveRequests, setAllowSubadminsApproveRequests] =
    useState(communitySettings.allowSubadminsApproveRequests !== false);
  const [roomCreationPermission, setRoomCreationPermission] = useState(
    communitySettings.roomCreationPermission || 'subadmins'
  );
  const [postingPermission, setPostingPermission] = useState(
    communitySettings.postingPermission || 'all'
  );
  const [allowInvites, setAllowInvites] = useState(communitySettings.allowInvites);
  const [allowMemberPolls, setAllowMemberPolls] = useState(communitySettings.allowMemberPolls);
  const [guidelines, setGuidelines] = useState(communitySettings.guidelines);

  const handleSave = (e) => {
    e.preventDefault();
    onSave({
      title: title.trim() || community.title,
      description: description.trim(),
      privacy,
      needsApproval,
      allowSubadminsApproveRequests,
      roomCreationPermission,
      postingPermission,
      allowInvites,
      allowMemberPolls,
      guidelines: guidelines.trim(),
    });
  };

  const isAdmin = userRole === 'Admin';

  return (
    <section className="content-editor">
      <div className="community-settings-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <div>
          <h3>Community Settings</h3>
          <p className="muted" style={{ margin: 0, fontSize: 13 }}>
            Configure discovery, room permissions, roles, and community guidelines.
          </p>
        </div>
        <Tag tone={isAdmin ? 'purple' : 'neutral'}>
          {isAdmin ? 'Admin Mode (Full Access)' : 'Read-only (Admin Access Required)'}
        </Tag>
      </div>

      <form onSubmit={handleSave} className="form-stack">
        <Field label="Community Name">
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            disabled={!isAdmin}
          />
        </Field>

        <Field label="Community Description">
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            disabled={!isAdmin}
            rows={2}
          />
        </Field>

        <Field label="Community Privacy">
          <select
            value={privacy}
            onChange={(e) => setPrivacy(e.target.value)}
            disabled={!isAdmin}
          >
            <option value="Public">Public · Listed in community discovery</option>
            <option value="Private">Private · Approval required to join</option>
            <option value="Invite-only">Invite-only · Unlisted</option>
          </select>
        </Field>

        {/* Join Permission & Sub-admin Approval Toggles (Requirement 3) */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: 12,
            margin: '8px 0',
            padding: 14,
            background: 'var(--soft, #f8fafc)',
            borderRadius: 8,
            border: '1px solid var(--line)',
          }}
        >
          <label
            style={{
              display: 'flex',
              alignItems: 'flex-start',
              gap: 10,
              fontSize: 13,
              cursor: isAdmin ? 'pointer' : 'default',
            }}
          >
            <input
              type="checkbox"
              style={{ marginTop: 3 }}
              checked={needsApproval}
              onChange={(e) => setNeedsApproval(e.target.checked)}
              disabled={!isAdmin}
            />
            <div>
              <b style={{ display: 'block', fontSize: 13 }}>
                Require permission / approval to join community
              </b>
              <span className="muted" style={{ fontSize: 12 }}>
                When enabled, new users must be approved before joining. An extra Requests tab appears beside Settings for Admins and authorized Sub-admins.
              </span>
            </div>
          </label>

          <label
            className="community-settings-nested-checkbox"
            style={{
              display: 'flex',
              alignItems: 'flex-start',
              gap: 10,
              fontSize: 13,
              cursor: isAdmin ? 'pointer' : 'default',
              paddingLeft: 24,
            }}
          >
            <input
              type="checkbox"
              style={{ marginTop: 3 }}
              checked={allowSubadminsApproveRequests}
              onChange={(e) => setAllowSubadminsApproveRequests(e.target.checked)}
              disabled={!isAdmin}
            />
            <div>
              <b style={{ display: 'block', fontSize: 13 }}>
                Allow Sub-admins to review and allow join requests
              </b>
              <span className="muted" style={{ fontSize: 12 }}>
                When enabled by admin, Sub-admins will also see the Requests tab beside Settings and can allow or decline member join requests.
              </span>
            </div>
          </label>
        </div>

        {/* Room Creation Permission (Requirement 4) */}
        <Field label="Who can create rooms &amp; channels in this community?">
          <select
            value={roomCreationPermission}
            onChange={(e) => setRoomCreationPermission(e.target.value)}
            disabled={!isAdmin}
          >
            <option value="all">All Members (Anyone in the community can create rooms)</option>
            <option value="subadmins">Admins &amp; Sub-admins only (Recommended)</option>
            <option value="admins">Community Admin only</option>
          </select>
        </Field>

        <Field label="Posting &amp; Chat permissions">
          <select
            value={postingPermission}
            onChange={(e) => setPostingPermission(e.target.value)}
            disabled={!isAdmin}
          >
            <option value="all">All members can post and chat freely</option>
            <option value="moderated">New members require post approval</option>
            <option value="admins">Only Admins and Sub-admins can post</option>
          </select>
        </Field>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, margin: '8px 0' }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, cursor: isAdmin ? 'pointer' : 'default' }}>
            <input
              type="checkbox"
              checked={allowInvites}
              onChange={(e) => setAllowInvites(e.target.checked)}
              disabled={!isAdmin}
            />
            <span>Allow all members to invite people via link</span>
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, cursor: isAdmin ? 'pointer' : 'default' }}>
            <input
              type="checkbox"
              checked={allowMemberPolls}
              onChange={(e) => setAllowMemberPolls(e.target.checked)}
              disabled={!isAdmin}
            />
            <span>Allow members to create and post interactive polls in channels</span>
          </label>
        </div>

        <Field label="Community Guidelines">
          <textarea
            value={guidelines}
            onChange={(e) => setGuidelines(e.target.value)}
            disabled={!isAdmin}
            rows={3}
          />
        </Field>

        {/* Transfer Ownership / Admin Position (Requirement 6) */}
        {isAdmin && (
          <div
            className="community-transfer-ownership-box"
            style={{
              padding: 14,
              border: '1px solid #fed7aa',
              background: '#fffaf5',
              borderRadius: 8,
              marginTop: 10,
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
            }}
          >
            <div>
              <b style={{ display: 'block', fontSize: 13, color: '#9a3412' }}>
                Transfer Community Ownership
              </b>
              <small style={{ color: 'var(--muted)', fontSize: 11 }}>
                Promote another member to primary Admin and choose to step down to Sub-admin, Member, or leave.
              </small>
            </div>
            <Button
              size="sm"
              variant="ghost"
              icon={Crown}
              type="button"
              onClick={onOpenTransferAdmin}
            >
              Transfer Admin Position
            </Button>
          </div>
        )}

        {/* Actions (Requirement 9: Save settings works & Community Delete) */}
        <div className="community-settings-bottom-actions" style={{ display: 'flex', gap: 10, marginTop: 18, alignItems: 'center', justifyContent: 'space-between' }}>
          <div className="community-settings-main-btns" style={{ display: 'flex', gap: 10 }}>
            {isAdmin && (
              <Button type="submit" variant="primary">
                Save community settings
              </Button>
            )}
            <Button variant="ghost" className="danger-link" type="button" onClick={onLeave}>
              Leave community
            </Button>
          </div>
          {isAdmin && (
            <Button
              variant="quiet"
              className="community-delete-btn"
              style={{ color: '#dc2626', borderColor: '#fca5a5' }}
              type="button"
              onClick={onDelete}
            >
              Delete this community
            </Button>
          )}
        </div>
      </form>
    </section>
  );
}

function FollowersManager({ activePerson, isMe, app }) {
  const [subTab, setSubTab] = useState('Followers');

  // Network mapping for non-self profiles
  const otherNetwork = {
    riya: { followers: ['aman', 'neha'], following: ['aman', 'arjun'] },
    aman: { followers: ['riya', 'arjun'], following: ['neha', 'riya'] },
    neha: { followers: ['riya', 'aman'], following: ['arjun', 'riya'] },
    arjun: { followers: ['aman', 'neha'], following: ['riya', 'aman'] },
  };

  const userFollowersIds = Array.isArray(app.followers) && app.followers.length > 0
    ? app.followers
    : ['riya', 'neha', 'arjun'];

  const userFollowingIds = Array.isArray(app.following)
    ? app.following
    : ['riya', 'aman'];

  const followingList = isMe
    ? userFollowingIds.map((id) => people.find((p) => p.id === id) || { id, name: id.charAt(0).toUpperCase() + id.slice(1), role: 'Collobster Collaborator' })
    : people.filter((p) => p.id !== activePerson.id && (otherNetwork[activePerson.id]?.following || ['neha', 'arjun']).includes(p.id));

  const followersList = isMe
    ? userFollowersIds.map((id) => people.find((p) => p.id === id) || { id, name: id.charAt(0).toUpperCase() + id.slice(1), role: 'Collobster Collaborator' })
    : people.filter((p) => p.id !== activePerson.id && (otherNetwork[activePerson.id]?.followers || ['riya', 'aman']).includes(p.id));

  const currentList = subTab === 'Following' ? followingList : followersList;

  return (
    <div className="profile-section">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <div>
          <h2 style={{ margin: '0 0 4px', fontSize: '1.15rem' }}>
            {isMe ? 'Connections' : `${activePerson.name}'s Connections`}
          </h2>
          <p style={{ margin: 0, fontSize: '0.86rem', color: 'var(--muted)' }}>
            {isMe
              ? 'Manage your network: people who follow you and people you are following.'
              : `Collaborators connected with ${activePerson.name}.`}
          </p>
        </div>
      </div>

      <div style={{ marginBottom: 16 }}>
        <Tabs
          items={[`Followers (${followersList.length})`, `Following (${followingList.length})`]}
          value={subTab === 'Following' ? `Following (${followingList.length})` : `Followers (${followersList.length})`}
          onChange={(val) => setSubTab(val.startsWith('Following') ? 'Following' : 'Followers')}
        />
      </div>

      <div style={{ display: 'grid', gap: 10 }}>
        {currentList.map((person) => {
          const isUserFollowing = (app.following || []).includes(person.id);
          const isUserFollower = userFollowersIds.includes(person.id);
          const isMutual = isUserFollowing && isUserFollower;

          return (
            <div
              key={person.id}
              className="connection-card-row"
            >
              <div
                className="connection-person-info"
                style={{ cursor: 'pointer' }}
                onClick={() => app.openUserProfile?.(person.name, 'Posts')}
                title={`View ${person.name}'s profile`}
              >
                <Avatar person={person} name={person.name} />
                <div style={{ minWidth: 0, flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
                    <strong style={{ fontSize: 14, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {person.name}
                    </strong>
                    {isMe && isMutual && (
                      <span
                        style={{
                          fontSize: 11,
                          padding: '1px 7px',
                          borderRadius: 99,
                          background: 'rgba(34, 197, 94, 0.12)',
                          color: 'var(--green, #16a34a)',
                          fontWeight: 600,
                          lineHeight: '16px',
                        }}
                      >
                        Mutual
                      </span>
                    )}
                    {isMe && !isMutual && subTab === 'Followers' && (
                      <span
                        style={{
                          fontSize: 11,
                          padding: '1px 7px',
                          borderRadius: 99,
                          background: 'var(--surface-hover, rgba(0,0,0,0.06))',
                          color: 'var(--muted)',
                          fontWeight: 500,
                          lineHeight: '16px',
                        }}
                      >
                        Follows you
                      </span>
                    )}
                  </div>
                  <small style={{ color: 'var(--muted)', display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', fontSize: 12 }}>
                    {person.role}
                  </small>
                </div>
              </div>

              <div className="connection-card-actions">
                <Button
                  size="sm"
                  variant="ghost"
                  icon={MessageCircle}
                  onClick={() => {
                    app.openConversation?.(person.name);
                    app.go?.('Messages');
                  }}
                  title={`Send message to ${person.name}`}
                >
                  Message
                </Button>
                <Button
                  size="sm"
                  variant={isUserFollowing ? 'quiet' : 'primary'}
                  icon={isUserFollowing ? Check : UserPlus}
                  onClick={() => app.toggleFollow?.(person.id)}
                  title={isUserFollowing ? `Unfollow ${person.name}` : `Follow ${person.name}`}
                >
                  {isUserFollowing ? 'Following' : subTab === 'Followers' ? 'Follow back' : 'Follow'}
                </Button>
                {isMe && subTab === 'Followers' && (
                  <Button
                    size="sm"
                    variant="ghost"
                    icon={UserMinus}
                    onClick={() => {
                      if (window.confirm(`Remove ${person.name} from your followers?`)) {
                        app.removeFollower?.(person.id);
                      }
                    }}
                    title={`Remove ${person.name} from followers`}
                    style={{ color: 'var(--muted)' }}
                  >
                    Remove
                  </Button>
                )}
              </div>
            </div>
          );
        })}
        {currentList.length === 0 && (
          <Empty
            title={subTab === 'Following' ? 'Not following anyone yet' : 'No followers yet'}
            text={subTab === 'Following' ? 'Explore the community or network to discover people to follow.' : 'Connect with others to grow your network.'}
          />
        )}
      </div>
    </div>
  );
}

// ─── Profile Interactive Poll Component ──────────────────────────────────────
function ProfilePostPoll({ post, app }) {
  const votedIndex = app.votes[post.id];
  const voted = votedIndex !== undefined;
  const total = (post.votes || []).reduce((sum, value) => sum + value, 0) + (voted ? 1 : 0);

  const handleVote = (index) => {
    const optionId = post.pollOptionIds?.[index];
    app.votePoll(post.id, post.pollId, optionId, index);
  };

  return (
    <div className="poll" style={{ margin: '8px 0 12px' }}>
      {(post.options || []).map((option, index) => {
        const isSelected = votedIndex === index;
        const optVotes = (post.votes?.[index] || 0) + (isSelected ? 1 : 0);
        const percent = total > 0 ? Math.round((optVotes / total) * 100) : 0;

        return (
          <button
            type="button"
            key={option}
            className={isSelected ? 'chosen' : ''}
            onClick={() => handleVote(index)}
            aria-pressed={isSelected}
            title={isSelected ? 'Click to deselect vote' : voted ? 'Click to switch vote' : 'Click to vote'}
          >
            <span style={{ width: voted ? `${percent}%` : 0 }} />
            <b>{isSelected ? '✓ ' : ''}{option}</b>
            {voted && <small>{percent}% ({optVotes})</small>}
          </button>
        );
      })}
    </div>
  );
}

// ─── 10. Profile Space (Exported) ───────────────────────────────────────────

export function Profile() {
  const a = useApp();
  const [tab, setTab] = useState(
    a.profileTab === 'Followers' ? 'Connections' : a.profileTab || 'Posts'
  );
  const [newSkill, setNewSkill] = useState('');
  const [isAddingSkill, setIsAddingSkill] = useState(false);

  // ── Filters & Search state for checking user posts ──
  const [selectedFormat, setSelectedFormat] = useState('All');
  const [selectedTag, setSelectedTag] = useState('All');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState('latest');

  useEffect(() => {
    if (a.profileTab) {
      setTab(a.profileTab === 'Followers' ? 'Connections' : a.profileTab);
    }
  }, [a.profileTab]);

  const isMe =
    !a.viewedProfile ||
    a.viewedProfile.id === 'me' ||
    a.viewedProfile.id === a.profile?.profileId ||
    a.viewedProfile.name?.toLowerCase() === a.profile?.name?.toLowerCase();

  const activePerson = isMe ? a.profile : a.viewedProfile;
  const isFollowing = !isMe && a.following?.includes(activePerson.id);

  const authorPosts = (a.posts || [])
    .filter((p) => {
      if (isMe) {
        return (
          p.authorId === 'me' ||
          p.authorId === a.profile.profileId ||
          p.author === a.profile.name ||
          p.authorName === a.profile.name
        );
      }
      const matchId = p.authorId && (p.authorId === activePerson.id || p.authorId === activePerson.name);
      const matchAuthor =
        p.author &&
        (p.author === activePerson.name ||
          (typeof p.author === 'string' && p.author.toLowerCase() === activePerson.name?.toLowerCase()));
      const matchAuthorName =
        p.authorName &&
        (p.authorName === activePerson.name ||
          (typeof p.authorName === 'string' && p.authorName.toLowerCase() === activePerson.name?.toLowerCase()));
      return matchId || matchAuthor || matchAuthorName;
    });

  const isGroupProfile = !isMe && (activePerson.isGroup || activePerson.context === 'Group');

  const allUserPosts =
    authorPosts.length > 0
      ? authorPosts
      : !isMe
      ? [
          {
            id: `sample-${activePerson.id || 'group'}`,
            type: 'post',
            format: 'Post',
            title: isGroupProfile ? `${activePerson.name} workspace update` : `${activePerson.name}'s update`,
            text: isGroupProfile
              ? `Welcome to ${activePerson.name}! This collaborative group workspace brings teammates together for discussions, shared files, and active momentum.`
              : `Excited to connect with collaborators across the network! Working on ${
                  activePerson.skills?.[0] || 'innovative projects'
                } and open to new ideas.`,
            time: 'Recently',
            likes: 14,
            comments: 3,
            tags: isGroupProfile ? ['Group', 'Collaboration', 'Team'] : (activePerson.skills || ['Collobster', 'Community']),
          },
        ]
      : [];

  // ── Format detection helper ──
  const getPostFormat = (p) => {
    const fmt = (p.format || '').toLowerCase();
    const type = (p.type || '').toLowerCase();
    const tags = (p.tags || []).map((t) => t.toLowerCase());

    if (fmt === 'poll' || type === 'poll' || (p.options && p.options.length > 0)) {
      return 'Polls';
    }
    if (fmt === 'photo' || type === 'photo' || (p.image && !p.body && type !== 'card')) {
      return 'Photos';
    }
    if (fmt === 'article' || fmt === 'card' || type === 'card' || type === 'article') {
      return 'Articles';
    }
    if (fmt === 'project' || type === 'project' || tags.includes('projects') || tags.includes('project')) {
      return 'Projects';
    }
    if (fmt === 'achievement' || type === 'achievement' || tags.includes('achievement') || tags.includes('milestone')) {
      return 'Milestones';
    }
    if (fmt === 'event' || type === 'event' || tags.includes('event') || tags.includes('meetup') || tags.includes('hackathon')) {
      return 'Events';
    }
    if (fmt === 'job' || type === 'job' || tags.includes('job') || tags.includes('career') || tags.includes('hiring')) {
      return 'Jobs';
    }
    if (p.image) {
      return 'Photos';
    }
    return 'Posts';
  };

  const matchesFormat = (p, targetFormat) => {
    if (targetFormat === 'All') return true;
    const detected = getPostFormat(p);
    if (detected === targetFormat) return true;
    if (targetFormat === 'Photos' && p.image) return true;
    if (targetFormat === 'Articles' && (p.type === 'card' || p.format === 'Article')) return true;
    if (targetFormat === 'Polls' && (p.type === 'poll' || p.options?.length)) return true;
    if (targetFormat === 'Projects' && (p.type === 'project' || p.tags?.some((t) => t.toLowerCase().includes('project')))) return true;
    if (targetFormat === 'Milestones' && (p.type === 'achievement' || p.tags?.some((t) => t.toLowerCase().includes('milestone') || t.toLowerCase().includes('achievement')))) return true;
    if (targetFormat === 'Events' && (p.type === 'event' || p.tags?.some((t) => t.toLowerCase().includes('event')))) return true;
    if (targetFormat === 'Jobs' && (p.type === 'job' || p.tags?.some((t) => t.toLowerCase().includes('job') || t.toLowerCase().includes('career')))) return true;
    if (targetFormat === 'Posts' && (detected === 'Posts' || p.type === 'post')) return true;
    return false;
  };

  // Count available items per format
  const formatCounts = useMemo(() => {
    const counts = {
      All: allUserPosts.length,
      Posts: 0,
      Photos: 0,
      Articles: 0,
      Polls: 0,
      Projects: 0,
      Milestones: 0,
      Events: 0,
      Jobs: 0,
    };
    allUserPosts.forEach((p) => {
      Object.keys(counts).forEach((fmt) => {
        if (fmt !== 'All' && matchesFormat(p, fmt)) {
          counts[fmt] = (counts[fmt] || 0) + 1;
        }
      });
    });
    return counts;
  }, [allUserPosts]);

  // Topic tags extracted from posts
  const availableTags = useMemo(() => {
    const set = new Set();
    allUserPosts.forEach((p) => {
      (p.tags || []).forEach((t) => set.add(t));
    });
    return Array.from(set);
  }, [allUserPosts]);

  // Filtered & Sorted posts
  const filteredPosts = useMemo(() => {
    return allUserPosts
      .filter((p) => {
        if (!matchesFormat(p, selectedFormat)) return false;
        if (selectedTag !== 'All') {
          const hasTag = (p.tags || []).some(
            (t) => t.toLowerCase() === selectedTag.toLowerCase()
          );
          if (!hasTag) return false;
        }
        if (searchQuery.trim()) {
          const q = searchQuery.toLowerCase().trim();
          const inTitle = (p.title || '').toLowerCase().includes(q);
          const inText = (p.text || '').toLowerCase().includes(q);
          const inTags = (p.tags || []).some((t) => t.toLowerCase().includes(q));
          if (!inTitle && !inText && !inTags) return false;
        }
        return true;
      })
      .sort((p1, p2) => {
        if (sortBy === 'likes') {
          const l1 = (p1.likes || 0) + (a.likes?.includes(p1.id) ? 1 : 0);
          const l2 = (p2.likes || 0) + (a.likes?.includes(p2.id) ? 1 : 0);
          if (l2 !== l1) return l2 - l1;
        } else if (sortBy === 'comments') {
          const c1 = (p1.comments || 0) + (a.comments?.[p1.id]?.length || 0);
          const c2 = (p2.comments || 0) + (a.comments?.[p2.id]?.length || 0);
          if (c2 !== c1) return c2 - c1;
        }
        const t1 = p1.createdAt
          ? new Date(p1.createdAt).getTime()
          : p1.time === 'Just now' || p1.time === 'Now'
          ? Date.now()
          : 0;
        const t2 = p2.createdAt
          ? new Date(p2.createdAt).getTime()
          : p2.time === 'Just now' || p2.time === 'Now'
          ? Date.now()
          : 0;
        if (t2 !== t1) return t2 - t1;
        return String(p2.id || '').localeCompare(String(p1.id || ''));
      });
  }, [allUserPosts, selectedFormat, selectedTag, searchQuery, sortBy, a.likes, a.comments]);

  const isFiltered = selectedFormat !== 'All' || selectedTag !== 'All' || Boolean(searchQuery.trim());
  const handleClearFilters = () => {
    setSelectedFormat('All');
    setSelectedTag('All');
    setSearchQuery('');
    setSortBy('latest');
  };

  const renderFormatBadge = (p) => {
    const fmt = getPostFormat(p);
    switch (fmt) {
      case 'Articles':
        return <Tag tone="coral">ARTICLE</Tag>;
      case 'Polls':
        return <Tag tone="purple">POLL</Tag>;
      case 'Projects':
        return <Tag tone="green">PROJECT</Tag>;
      case 'Milestones':
        return <Tag tone="gold">MILESTONE</Tag>;
      case 'Photos':
        return <Tag tone="blue">PHOTO</Tag>;
      case 'Events':
        return <Tag tone="coral">EVENT</Tag>;
      case 'Jobs':
        return <Tag tone="blue">OPPORTUNITY</Tag>;
      default:
        return <Tag tone="quiet">UPDATE</Tag>;
    }
  };

  const handleAddSkill = (e) => {
    e.preventDefault();
    if (!newSkill.trim()) return;
    const current = a.profile.skills || [];
    if (!current.includes(newSkill.trim())) {
      const updated = [...current, newSkill.trim()];
      a.setProfile({ ...a.profile, skills: updated });
      a.toast(`Added ${newSkill.trim()} to your skills`);
    }
    setNewSkill('');
    setIsAddingSkill(false);
  };

  const handleRemoveSkill = (skillToRemove) => {
    const updated = (a.profile.skills || []).filter((s) => s !== skillToRemove);
    a.setProfile({ ...a.profile, skills: updated });
    a.toast(`Removed ${skillToRemove}`);
  };

  return (
    <>
      {!isMe && (
        <div style={{ marginBottom: 14 }}>
          <Button
            variant="quiet"
            icon={ChevronLeft}
            onClick={() => {
              if (a.setViewedProfile) a.setViewedProfile(null);
              a.go('Home');
            }}
          >
            Back to feed
          </Button>
        </div>
      )}

      <PageHeader
        eyebrow={isMe ? 'YOUR PROFILE' : isGroupProfile ? 'COLLABORATIVE GROUP' : 'MEMBER PROFILE'}
        title={isMe ? 'Your story, in one place.' : activePerson.name}
        description={
          isMe
            ? 'A profile that travels with you through every Collobster space.'
            : isGroupProfile
            ? (activePerson.bio || 'A dedicated team workspace for discussions, shared files, and group collaboration.')
            : activePerson.headline ||
              activePerson.role ||
              'Member of the Collobster collaborative network.'
        }
      />

      <div className="profile-layout" style={{ gridTemplateColumns: '1fr' }}>
        <section className="profile-main">
          <div
            className="profile-cover"
            style={
              activePerson?.coverImage
                ? {
                    backgroundImage: `url(${activePerson.coverImage})`,
                    backgroundSize: 'cover',
                    backgroundPosition: 'center',
                  }
                : undefined
            }
          >
            {!activePerson?.coverImage && <span className="lobster-line">◌</span>}
            {isMe && (
              <button
                type="button"
                className="profile-cover-edit-btn"
                onClick={() => a.setModal({ type: 'edit-profile' })}
                title="Change cover & profile images"
                aria-label="Edit cover photo"
              >
                <Camera size={14} />
                <span>Edit cover</span>
              </button>
            )}
          </div>

          <div className="profile-card">
            <div style={{ position: 'relative', display: 'inline-block' }}>
              <Avatar person={activePerson} name={activePerson.name} size="xl" />
              {isMe && (
                <button
                  type="button"
                  className="profile-avatar-edit-badge"
                  onClick={() => a.setModal({ type: 'edit-profile' })}
                  title="Change profile picture"
                  aria-label="Change profile picture"
                >
                  <Camera size={14} />
                </button>
              )}
            </div>
            <div className="profile-title">
              <h1>{activePerson.name}</h1>
              <p>{isGroupProfile ? 'Collaborative Group' : (activePerson.role || activePerson.headline || 'Collobster Member')}</p>
              <small>
                {isGroupProfile ? (
                  <>
                    <Users size={14} />
                    {activePerson.members?.length ? `${activePerson.members.length} members` : 'Group workspace'}
                  </>
                ) : (
                  <>
                    <MapPin size={14} />
                    {activePerson.location || 'Available in the Collobster network'}
                  </>
                )}
              </small>
            </div>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {!isMe && !isGroupProfile && (
                <>
                  <Button
                    variant={isFollowing ? 'quiet' : 'primary'}
                    icon={isFollowing ? Check : Plus}
                    onClick={() => a.toggleFollow(activePerson.id)}
                  >
                    {isFollowing ? 'Following' : 'Follow'}
                  </Button>
                  <Button
                    variant="quiet"
                    icon={MessageCircle}
                    onClick={() => a.openConversation(activePerson.name)}
                  >
                    Message
                  </Button>
                </>
              )}
              {isGroupProfile && (
                <Button
                  variant="primary"
                  icon={MessageCircle}
                  onClick={() => {
                    if (a.openConversation) a.openConversation(activePerson.name);
                    a.go('Messages');
                  }}
                >
                  Open group chat
                </Button>
              )}
              <Button
                variant="quiet"
                icon={Share2}
                onClick={() => {
                  navigator.clipboard?.writeText?.(window.location.href);
                  a.toast(isGroupProfile ? 'Group link copied to clipboard!' : 'Profile link copied to clipboard!');
                }}
              >
                Share
              </Button>
              {isMe && (
                <Button
                  variant="quiet"
                  icon={Edit3}
                  onClick={() => a.setModal({ type: 'edit-profile' })}
                >
                  Edit
                </Button>
              )}
            </div>
          </div>

          <Tabs
            items={isGroupProfile ? ['Updates', 'Members', 'About'] : ['Posts', 'About', 'Connections']}
            value={tab === 'Posts' && isGroupProfile ? 'Updates' : tab === 'Skills' ? 'About' : tab === 'Followers' ? 'Connections' : tab}
            onChange={setTab}
          />

          {tab === 'Members' && (
            <div className="profile-section">
              <SectionTitle title={`Team Members (${(activePerson.members || ['You', 'Riya Sharma', 'Aman Verma']).length})`} />
              <div style={{ display: 'grid', gap: 10 }}>
                {(activePerson.members || ['You', 'Riya Sharma', 'Aman Verma']).map((m, idx) => {
                  const mName = typeof m === 'string' ? m : (m.name || 'Member');
                  return (
                    <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 0', borderBottom: '1px solid var(--line)' }}>
                      <Avatar name={mName} />
                      <div style={{ flex: 1 }}>
                        <strong>{mName}</strong>
                        <small style={{ display: 'block', color: 'var(--muted)', fontSize: 12 }}>
                          {idx === 0 ? 'Admin / Creator' : 'Collaborator'}
                        </small>
                      </div>
                      <Tag tone={idx === 0 ? 'purple' : ''}>{idx === 0 ? 'Admin' : 'Member'}</Tag>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {tab === 'About' && (
            <>
              <div className="profile-section">
                <h2>{isGroupProfile ? 'Group Workspace Information' : 'About'}</h2>
                <p>
                  {activePerson.bio ||
                    (isMe
                      ? 'Add a summary to introduce yourself to teammates and collaborators.'
                      : isGroupProfile
                      ? 'Active team group for shared ideas, project collaboration, and discussion.'
                      : `${activePerson.name} is an active member exploring collaboration opportunities and project partnerships on Collobster.`)}
                </p>
              </div>

              {!isGroupProfile && (
                <div className="profile-section">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                    <div>
                      <h3 style={{ margin: '0 0 2px', fontSize: 16 }}>Skills &amp; Proficiencies</h3>
                      <p style={{ margin: 0, fontSize: 12, color: 'var(--muted)' }}>
                        {isMe
                          ? 'Add technical skills, tools, and areas of expertise to match opportunities and projects.'
                          : `${activePerson.name}'s technical expertise and skillsets.`}
                      </p>
                    </div>
                    {isMe && (
                      <Button
                        size="sm"
                        variant={isAddingSkill ? 'quiet' : 'primary'}
                        icon={isAddingSkill ? X : Plus}
                        onClick={() => setIsAddingSkill(!isAddingSkill)}
                      >
                        {isAddingSkill ? 'Cancel' : 'Add skill'}
                      </Button>
                    )}
                  </div>

                  {isMe && isAddingSkill && (
                    <form
                      onSubmit={handleAddSkill}
                      style={{
                        display: 'flex',
                        gap: 8,
                        padding: 10,
                        background: 'var(--soft)',
                        borderRadius: 8,
                        marginBottom: 14,
                        border: '1px solid var(--line)',
                      }}
                    >
                      <input
                        type="text"
                        value={newSkill}
                        onChange={(e) => setNewSkill(e.target.value)}
                        placeholder="Enter a skill (e.g. Flutter, React, GraphQL, UX Design)..."
                        autoFocus
                        style={{
                          flex: 1,
                          padding: '7px 10px',
                          borderRadius: 6,
                          border: '1px solid var(--line)',
                          background: 'var(--surface)',
                          color: 'var(--ink)',
                          fontSize: 13,
                        }}
                      />
                      <Button type="submit" variant="primary" size="sm">
                        Add
                      </Button>
                    </form>
                  )}

                  <div className="skills-interactive-container" style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                    {(activePerson.skills || []).map((skill) => (
                      <div
                        key={skill}
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: 6,
                          padding: '5px 12px',
                          background: 'var(--soft)',
                          borderRadius: 20,
                          border: '1px solid var(--line)',
                          fontSize: 12,
                          fontWeight: 600,
                          color: 'var(--ocean)',
                        }}
                      >
                        <span>{skill}</span>
                        {isMe && (
                          <button
                            type="button"
                            onClick={() => handleRemoveSkill(skill)}
                            style={{
                              background: 'none',
                              border: 'none',
                              cursor: 'pointer',
                              padding: 0,
                              color: 'var(--muted)',
                              display: 'flex',
                              alignItems: 'center',
                            }}
                            title={`Remove ${skill}`}
                          >
                            <X size={13} />
                          </button>
                        )}
                      </div>
                    ))}
                    {(!activePerson.skills || !activePerson.skills.length) && (
                      <p style={{ color: 'var(--muted)', fontSize: 13 }}>No skills listed yet.</p>
                    )}
                  </div>
                </div>
              )}

              {isMe && (
                <div className="profile-section">
                  <SectionTitle
                    title="Recent projects"
                    action="View projects"
                    onAction={() => a.go('Projects')}
                  />
                  {projects.slice(0, 2).map((p) => (
                    <button
                      className="profile-project"
                      key={p.id}
                      onClick={() =>
                        a.setModal({ type: 'details', kind: 'project', item: p })
                      }
                    >
                      <span className={`module-icon ${p.color}`}>
                        <FolderKanban size={18} />
                      </span>
                      <span>
                        <strong>{p.title}</strong>
                        <small>{p.description}</small>
                      </span>
                      <ArrowRight size={18} />
                    </button>
                  ))}
                </div>
              )}
            </>
          )}

          {(tab === 'Connections' || tab === 'Followers') && (
            <FollowersManager
              activePerson={activePerson}
              isMe={isMe}
              app={a}
            />
          )}

          {(tab === 'Posts' || tab === 'Activity' || tab === 'Updates') && (
            <div className="profile-section">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
                <div>
                  <h2 style={{ margin: '0 0 4px', fontSize: '1.25rem', fontFamily: 'Fraunces, serif' }}>
                    {isMe
                      ? `Your Created Posts (${allUserPosts.length})`
                      : isGroupProfile
                      ? `${activePerson.name} Group Updates (${allUserPosts.length})`
                      : `Posts by ${activePerson.name.split(' ')[0]} (${allUserPosts.length})`}
                  </h2>
                  <p style={{ margin: 0, fontSize: '0.86rem', color: 'var(--muted)' }}>
                    {isGroupProfile
                      ? 'Team discussions, collaborative milestones, and group announcements.'
                      : 'Filter and inspect published posts, articles, polls, and project updates.'}
                  </p>
                </div>
                {isMe && (
                  <Button
                    size="sm"
                    variant="primary"
                    icon={Plus}
                    onClick={() => a.setModal({ type: 'composer', format: 'Post' })}
                  >
                    Create post
                  </Button>
                )}
              </div>

              {/* ── Posts Toolbar: Search, Sort, Formats & Topic Filters ── */}
              <div className="profile-posts-toolbar">
                <div className="profile-posts-search-row">
                  <div className="profile-posts-search">
                    <Search size={15} color="var(--muted)" />
                    <input
                      type="search"
                      placeholder={`Search ${isMe ? 'your' : `${activePerson.name.split(' ')[0]}'s`} posts, topics, ideas...`}
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      aria-label="Search user posts"
                    />
                    {searchQuery && (
                      <button
                        type="button"
                        onClick={() => setSearchQuery('')}
                        aria-label="Clear search"
                        style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 2, color: 'var(--muted)', display: 'grid', placeItems: 'center' }}
                      >
                        <X size={14} />
                      </button>
                    )}
                  </div>

                  <div className="profile-posts-sort-group">
                    <span className="profile-posts-sort-label">Sort:</span>
                    <div className="profile-posts-sort-chips">
                      <button
                        type="button"
                        className={sortBy === 'latest' ? 'selected' : ''}
                        onClick={() => setSortBy('latest')}
                      >
                        Latest
                      </button>
                      <button
                        type="button"
                        className={sortBy === 'likes' ? 'selected' : ''}
                        onClick={() => setSortBy('likes')}
                      >
                        Most Liked
                      </button>
                      <button
                        type="button"
                        className={sortBy === 'comments' ? 'selected' : ''}
                        onClick={() => setSortBy('comments')}
                      >
                        Most Comments
                      </button>
                    </div>
                  </div>
                </div>

                {/* ── Format / Category Filter Chips ── */}
                <div className="profile-posts-formats-bar">
                  <span className="profile-filter-section-label">Formats:</span>
                  <div className="profile-posts-format-chips">
                    {[
                      { id: 'All', label: 'All', icon: Sparkles },
                      { id: 'Posts', label: 'Posts', icon: FileText },
                      { id: 'Photos', label: 'Photos', icon: Image },
                      { id: 'Articles', label: 'Articles', icon: BookOpen },
                      { id: 'Polls', label: 'Polls', icon: BarChart3 },
                      { id: 'Projects', label: 'Projects', icon: FolderKanban },
                      { id: 'Milestones', label: 'Milestones', icon: Award },
                      { id: 'Events', label: 'Events', icon: CalendarDays },
                      { id: 'Jobs', label: 'Jobs', icon: BriefcaseBusiness },
                    ].map(({ id, label, icon: Icon }) => {
                      const count = formatCounts[id] || 0;
                      const isActive = selectedFormat === id;
                      return (
                        <button
                          key={id}
                          type="button"
                          className={`profile-format-chip ${isActive ? 'active' : ''}`}
                          onClick={() => setSelectedFormat(id)}
                        >
                          <Icon size={13} />
                          <span>{label}</span>
                          <span className="profile-format-badge">{count}</span>
                        </button>
                      );
                    })}
                  </div>
                </div>

                {/* ── Topic / Tag Pills ── */}
                {availableTags.length > 0 && (
                  <div className="profile-posts-tags-bar">
                    <span className="profile-filter-section-label">Topics:</span>
                    <div className="profile-posts-tag-chips">
                      <button
                        type="button"
                        className={`profile-tag-pill ${selectedTag === 'All' ? 'active' : ''}`}
                        onClick={() => setSelectedTag('All')}
                      >
                        All Topics
                      </button>
                      {availableTags.map((tag) => (
                        <button
                          key={tag}
                          type="button"
                          className={`profile-tag-pill ${selectedTag === tag ? 'active' : ''}`}
                          onClick={() => setSelectedTag(selectedTag === tag ? 'All' : tag)}
                        >
                          #{tag}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {/* ── Filter Status Bar ── */}
                <div className="profile-posts-status-bar">
                  <span style={{ fontSize: '0.82rem', color: 'var(--muted)' }}>
                    Showing <strong>{filteredPosts.length}</strong> of <strong>{allUserPosts.length}</strong> posts
                    {selectedFormat !== 'All' && ` · Format: ${selectedFormat}`}
                    {selectedTag !== 'All' && ` · Topic: #${selectedTag}`}
                    {searchQuery && ` · Query: "${searchQuery}"`}
                  </span>
                  {isFiltered && (
                    <button
                      type="button"
                      className="profile-posts-clear-btn"
                      onClick={handleClearFilters}
                    >
                      <X size={13} />
                      Clear filters
                    </button>
                  )}
                </div>
              </div>

              {/* ── Posts Listing ── */}
              {allUserPosts.length === 0 ? (
                <Empty
                  title="No posts yet"
                  text={
                    isMe
                      ? "You haven't published any posts yet. Share what you are building or ask a question!"
                      : `${activePerson.name} hasn't published any posts yet.`
                  }
                  action={isMe ? 'Create your first post' : undefined}
                  onAction={isMe ? () => a.setModal({ type: 'composer', format: 'Post' }) : undefined}
                />
              ) : filteredPosts.length === 0 ? (
                <Empty
                  title="No matching posts"
                  text={`No posts found matching the selected filters${selectedFormat !== 'All' ? ` for "${selectedFormat}"` : ''}${selectedTag !== 'All' ? ` in #${selectedTag}` : ''}${searchQuery ? ` matching "${searchQuery}"` : ''}.`}
                  action="Reset filters"
                  onAction={handleClearFilters}
                />
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                  {filteredPosts.map((p) => {
                    const isLiked = a.likes?.includes(p.id);
                    const commentsCount = (p.comments || 0) + (a.comments?.[p.id]?.length || 0);
                    const isAuthor = isMe || p.authorId === 'me' || p.authorId === a.profile?.profileId;

                    return (
                      <article key={p.id} className="profile-post-card-rich">
                        <header className="profile-post-card-header">
                          <div className="profile-post-format-row">
                            {renderFormatBadge(p)}
                            <span className="profile-post-meta-time">
                              {p.time ||
                                (p.createdAt
                                  ? new Date(p.createdAt).toLocaleDateString(undefined, {
                                      month: 'short',
                                      day: 'numeric',
                                    })
                                  : 'Recently')}
                            </span>
                          </div>
                          {isAuthor && (
                            <IconButton
                              icon={Trash2}
                              label="Delete post"
                              onClick={() => {
                                if (window.confirm('Delete this post?')) {
                                  a.setPosts((old) => old.filter((item) => item.id !== p.id));
                                  a.toast('Post deleted');
                                }
                              }}
                            />
                          )}
                        </header>

                        {p.title && <h3 className="profile-post-title">{p.title}</h3>}

                        {p.text && <p className="profile-post-text">{p.text}</p>}

                        {/* Interactive Poll */}
                        {(p.type === 'poll' || (p.options && p.options.length > 0)) && (
                          <ProfilePostPoll post={p} app={a} />
                        )}

                        {/* Image preview */}
                        {p.image && (
                          <div
                            className="profile-post-image-box"
                            onClick={() => a.setModal({ type: 'reader', id: p.id, item: p })}
                            title="Click to expand"
                          >
                            <img
                              src={p.image}
                              alt={p.imageAlt || p.title || 'Post preview'}
                            />
                          </div>
                        )}

                        {/* Project embed */}
                        {p.type === 'project' && (
                          <button
                            type="button"
                            className="project-embed"
                            onClick={() => a.go('Projects')}
                          >
                            <span className="module-icon green">P</span>
                            <span>
                              <strong>{p.title || 'Project Collaboration'}</strong>
                              <small>View project workspace and roles</small>
                            </span>
                            <ArrowRight size={18} />
                          </button>
                        )}

                        {/* Tags as clickable filter pills */}
                        {p.tags && p.tags.length > 0 && (
                          <div className="profile-post-tags-row">
                            {p.tags.map((tag) => (
                              <button
                                key={tag}
                                type="button"
                                className="profile-post-tag-btn"
                                onClick={() => setSelectedTag(tag)}
                                title={`Filter by #${tag}`}
                              >
                                #{tag}
                              </button>
                            ))}
                          </div>
                        )}

                        {/* Interactive Footer */}
                        <footer className="profile-post-card-footer">
                          <div className="profile-post-stats-group">
                            <button
                              type="button"
                              className={`profile-post-stat-btn ${isLiked ? 'liked' : ''}`}
                              onClick={() => a.toggleReaction(p.id)}
                              aria-label={isLiked ? 'Unlike' : 'Like'}
                            >
                              <Heart size={15} fill={isLiked ? 'currentColor' : 'none'} />
                              <span>{(p.likes || 0) + (isLiked ? 1 : 0)} Likes</span>
                            </button>

                            <button
                              type="button"
                              className="profile-post-stat-btn"
                              onClick={() => a.setModal({ type: 'reader', id: p.id, item: p })}
                            >
                              <MessageCircle size={15} />
                              <span>{commentsCount} Comments</span>
                            </button>

                            <button
                              type="button"
                              className="profile-post-stat-btn"
                              onClick={() => {
                                navigator.clipboard?.writeText?.(window.location.href);
                                a.toast('Post link copied to clipboard!');
                              }}
                              title="Share post"
                            >
                              <Share2 size={15} />
                              <span>Share</span>
                            </button>
                          </div>

                          <Button
                            size="sm"
                            variant="quiet"
                            onClick={() => a.setModal({ type: 'reader', id: p.id, item: p })}
                          >
                            View in Feed
                          </Button>
                        </footer>
                      </article>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </section>
      </div>
    </>
  );
}

// ─── 11. Settings Page (Exported) ───────────────────────────────────────────

export function SettingsPage() {
  const a = useApp();
  const isPrivate = a.profile.privacy?.profileVisibility === 'private';
  const cloudCopy =
    a.serviceState === 'connected'
      ? 'Across your signed-in devices'
      : 'In this Preview on this device';
  const setPrivate = (checked) => a.updateProfilePrivacy(checked);
  const orders = a.paymentOrders || [];

  return (
    <>
      <PageHeader
        title="Settings"
        description="Choices that shape your Collobster experience."
      />

      <div className="settings-layout">
        <section className="list-surface">
          <h2>Notifications</h2>
          {[
            ['community', 'Community activity'],
            ['career', 'Career opportunities'],
            ['messages', 'Messages and mentions'],
          ].map(([key, label]) => (
            <label className="setting-toggle" key={key}>
              <span>
                <strong>{label}</strong>
                <small>{cloudCopy}</small>
              </span>
              <input
                type="checkbox"
                checked={a.preferences[key]}
                onChange={(e) =>
                  a.setPreferences({
                    ...a.preferences,
                    [key]: e.target.checked,
                  })
                }
              />
              <i />
            </label>
          ))}
        </section>

        <section className="list-surface">
          <h2>Appearance</h2>
          <button
            className="setting-link"
            onClick={() => a.setTheme(a.theme === 'dark' ? 'light' : 'dark')}
          >
            <span>
              <strong>{a.theme === 'dark' ? 'Dark' : 'Light'} theme</strong>
              <small>Switch how Collobster looks on this device.</small>
            </span>
            <ChevronRight size={18} />
          </button>
        </section>

        <section className="list-surface">
          <h2>Account</h2>
          <button className="setting-link" onClick={() => a.go('Profile')}>
            <span>
              <strong>Personal information</strong>
              <small>Profile, headline, skills, and interests.</small>
            </span>
            <ChevronRight size={18} />
          </button>

          <label className="setting-toggle">
            <span>
              <strong>Private profile</strong>
              <small>
                {isPrivate
                  ? 'Only approved connections can view your activity.'
                  : 'Your profile can be discovered and followed.'}
              </small>
            </span>
            <input
              type="checkbox"
              checked={isPrivate}
              onChange={(e) => setPrivate(e.target.checked)}
            />
            <i />
          </label>

          <button className="setting-link danger-link" onClick={a.logout}>
            <span>
              <strong>Log out</strong>
              <small>Sign out from this device.</small>
            </span>
            <LogOut size={18} />
          </button>
        </section>

        <section className="list-surface orders-receipts-panel">
          <h2>Orders &amp; Receipts</h2>
          {orders.length === 0 ? (
            <p className="muted orders-empty">
              No payment orders yet. When you purchase a paid ticket or
              subscription, your receipts appear here.
            </p>
          ) : (
            <div className="orders-list">
              {orders.map((order) => (
                <div className="receipt-card" key={order.id}>
                  <div className="receipt-card-header">
                    <span className="receipt-icon">🧾</span>
                    <div>
                      <b>{order.description || 'Payment order'}</b>
                      <small>
                        {new Date(order.createdAt).toLocaleDateString('en-IN', {
                          day: 'numeric',
                          month: 'short',
                          year: 'numeric',
                        })}
                      </small>
                      <small className="order-ref">
                        {order.orderNumber || order.id}
                      </small>
                    </div>
                  </div>
                  <div className="receipt-card-footer">
                    <span className={`order-status-badge status-${order.status}`}>
                      {order.status === 'completed'
                        ? '✓ Paid'
                        : order.status === 'failed'
                        ? '✗ Failed'
                        : 'Pending'}
                    </span>
                    <strong className="receipt-amount">
                      ₹{((order.amountCents || 0) / 100).toLocaleString('en-IN')}
                    </strong>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </>
  );
}
