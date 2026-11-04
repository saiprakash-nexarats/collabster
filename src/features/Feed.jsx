import React, { useState, useEffect, useMemo, useRef } from 'react';
import {
  Image,
  FileText,
  ChartNoAxesColumn,
  Plus,
  Heart,
  MessageCircle,
  Share2,
  Bookmark,
  ArrowUpRight,
  ArrowRight,
  MoreHorizontal,
  Check,
  Globe,
  SlidersHorizontal,
  Link2,
  Send,
  CornerDownRight,
  EyeOff,
  ThumbsDown,
  ShieldAlert,
  CalendarDays,
  BriefcaseBusiness,
  FolderKanban,
  Award,
  MapPin,
} from 'lucide-react';
import { useApp } from '../context';
import { rankFeed } from '../domain.mjs';
import { people, events, jobs, projects, initialComments } from '../data';
import {
  Avatar,
  Button,
  IconButton,
  Tag,
  Tabs,
  PageHeader,
  Media,
  Empty,
  Dialog,
} from '../components';
import { RightRail } from '../App';

// ─── Saved Categories Definition ──────────────────────────────────────────
const SAVED_CATEGORIES = [
  { id: 'All', label: 'All', desc: 'All saved items across your network' },
  { id: 'Posts', label: 'Posts', desc: 'General updates' },
  { id: 'Photos', label: 'Photos', desc: 'Posts with image attachments' },
  { id: 'Articles', label: 'Articles', desc: 'In-depth articles' },
  { id: 'Polls', label: 'Polls', desc: 'Interactive community polls' },
  { id: 'Projects', label: 'Projects', desc: 'Collaboration requests & project embeds' },
  { id: 'Milestones', label: 'Milestones', desc: 'Milestones & achievements' },
  { id: 'Events', label: 'Events', desc: 'Events & meetups' },
  { id: 'Jobs', label: 'Jobs', desc: 'Opportunities & roles' },
];

// ─── 1. Main Feed View (Default Export) ──────────────────────────────────────

export default function Feed({ savedOnly = false }) {
  const a = useApp();
  const [tab, setTab] = useState('For you');
  const [savedFilter, setSavedFilter] = useState('All');
  const [discussion, setDiscussion] = useState(null);
  const [comment, setComment] = useState('');
  const [filters, setFilters] = useState(false);
  const [sharePost, setSharePost] = useState(null);
  const [commentError, setCommentError] = useState('');

  const feed = rankFeed(
    savedOnly
      ? a.posts.filter((post) => a.saved.includes(post.id))
      : a.posts,
    {
      mode: tab,
      interests: a.profile.interests,
      following: a.following,
    }
  );

  // Saved data aggregation across all module entities
  const savedPostItems = useMemo(() => {
    return (a.posts || []).filter((post) => a.saved?.includes(post.id) && !post.hidden);
  }, [a.posts, a.saved]);

  const savedEventItems = useMemo(() => {
    return (events || []).filter(
      (e) => a.savedEvents?.includes(e.id) || a.saved?.includes(e.id)
    );
  }, [a.savedEvents, a.saved]);

  const savedJobItems = useMemo(() => {
    return (jobs || []).filter((j) => a.saved?.includes(j.id));
  }, [a.saved]);

  const savedProjectItems = useMemo(() => {
    return (projects || []).filter(
      (pr) => a.saved?.includes(pr.id) || a.projectState?.[pr.id]?.saved
    );
  }, [a.saved, a.projectState]);

  const savedCategoryCounts = useMemo(() => {
    const counts = {
      All: 0,
      Posts: 0,
      Photos: 0,
      Articles: 0,
      Polls: 0,
      Projects: 0,
      Milestones: 0,
      Events: 0,
      Jobs: 0,
    };

    savedPostItems.forEach((p) => {
      const isPhoto = Boolean(p.image);
      const isArticle =
        p.type === 'card' ||
        p.format === 'Article' ||
        p.type === 'article' ||
        (Array.isArray(p.body) && p.body.length > 0);
      const isPoll =
        p.type === 'poll' ||
        p.format === 'Poll' ||
        (p.options && p.options.length > 0);
      const isProject =
        p.type === 'project' ||
        p.format === 'Project' ||
        p.tags?.some((t) => t.toLowerCase().includes('project'));
      const isMilestone =
        p.type === 'achievement' ||
        p.format === 'Achievement' ||
        p.type === 'milestone' ||
        p.tags?.some(
          (t) =>
            t.toLowerCase().includes('milestone') ||
            t.toLowerCase().includes('achievement')
        );
      const isEvent =
        p.type === 'event' ||
        p.format === 'Event' ||
        p.tags?.some((t) => t.toLowerCase().includes('event'));
      const isJob =
        p.type === 'job' ||
        p.format === 'Job' ||
        p.tags?.some(
          (t) =>
            t.toLowerCase().includes('job') ||
            t.toLowerCase().includes('career')
        );

      if (isPhoto) counts.Photos++;
      if (isArticle) counts.Articles++;
      if (isPoll) counts.Polls++;
      if (isProject) counts.Projects++;
      if (isMilestone) counts.Milestones++;
      if (isEvent) counts.Events++;
      if (isJob) counts.Jobs++;
      if (
        !isPhoto &&
        !isArticle &&
        !isPoll &&
        !isProject &&
        !isMilestone &&
        !isEvent &&
        !isJob
      ) {
        counts.Posts++;
      } else if (p.type === 'post' || p.format === 'Post') {
        counts.Posts++;
      }
    });

    counts.Events += savedEventItems.length;
    counts.Jobs += savedJobItems.length;
    counts.Projects += savedProjectItems.length;

    counts.All =
      savedPostItems.length +
      savedEventItems.length +
      savedJobItems.length +
      savedProjectItems.length;

    return counts;
  }, [savedPostItems, savedEventItems, savedJobItems, savedProjectItems]);

  const filteredSavedItems = useMemo(() => {
    if (!savedOnly) return [];

    let postMatches = [];
    let eventMatches = [];
    let jobMatches = [];
    let projectMatches = [];

    if (savedFilter === 'All') {
      postMatches = savedPostItems;
      eventMatches = savedEventItems;
      jobMatches = savedJobItems;
      projectMatches = savedProjectItems;
    } else if (savedFilter === 'Posts') {
      postMatches = savedPostItems.filter((p) => {
        const isPhoto = Boolean(p.image);
        const isSpecial =
          p.type === 'card' ||
          p.type === 'poll' ||
          p.type === 'achievement' ||
          p.type === 'project' ||
          p.type === 'event' ||
          p.type === 'job';
        return (p.type === 'post' || p.format === 'Post' || !isSpecial) && !isPhoto;
      });
    } else if (savedFilter === 'Photos') {
      postMatches = savedPostItems.filter((p) => Boolean(p.image));
    } else if (savedFilter === 'Articles') {
      postMatches = savedPostItems.filter(
        (p) =>
          p.type === 'card' ||
          p.format === 'Article' ||
          p.type === 'article' ||
          (Array.isArray(p.body) && p.body.length > 0)
      );
    } else if (savedFilter === 'Polls') {
      postMatches = savedPostItems.filter(
        (p) =>
          p.type === 'poll' ||
          p.format === 'Poll' ||
          (p.options && p.options.length > 0)
      );
    } else if (savedFilter === 'Projects') {
      postMatches = savedPostItems.filter(
        (p) =>
          p.type === 'project' ||
          p.format === 'Project' ||
          p.tags?.some((t) => t.toLowerCase().includes('project'))
      );
      projectMatches = savedProjectItems;
    } else if (savedFilter === 'Milestones') {
      postMatches = savedPostItems.filter(
        (p) =>
          p.type === 'achievement' ||
          p.format === 'Achievement' ||
          p.type === 'milestone' ||
          p.tags?.some(
            (t) =>
              t.toLowerCase().includes('milestone') ||
              t.toLowerCase().includes('achievement')
          )
      );
    } else if (savedFilter === 'Events') {
      postMatches = savedPostItems.filter(
        (p) =>
          p.type === 'event' ||
          p.format === 'Event' ||
          p.tags?.some((t) => t.toLowerCase().includes('event'))
      );
      eventMatches = savedEventItems;
    } else if (savedFilter === 'Jobs') {
      postMatches = savedPostItems.filter(
        (p) =>
          p.type === 'job' ||
          p.format === 'Job' ||
          p.tags?.some(
            (t) =>
              t.toLowerCase().includes('job') ||
              t.toLowerCase().includes('career')
          )
      );
      jobMatches = savedJobItems;
    }

    return [
      ...postMatches.map((p) => ({ kind: 'post', data: p, key: `p-${p.id}` })),
      ...eventMatches.map((e) => ({ kind: 'event', data: e, key: `e-${e.id}` })),
      ...jobMatches.map((j) => ({ kind: 'job', data: j, key: `j-${j.id}` })),
      ...projectMatches.map((pr) => ({ kind: 'project', data: pr, key: `pr-${pr.id}` })),
    ];
  }, [
    savedOnly,
    savedFilter,
    savedPostItems,
    savedEventItems,
    savedJobItems,
    savedProjectItems,
  ]);

  const submitComment = async (event) => {
    event.preventDefault();
    if (!comment.trim() || !discussion) return;
    setCommentError('');
    try {
      await a.addComment(discussion.id, comment.trim());
      setComment('');
    } catch {
      setCommentError(
        'Your comment could not be sent. It is still in the box for you to retry.'
      );
    }
  };

  const firstName = a.profile.name.split(' ')[0];

  return (
    <div className={`feed-page-wrapper ${savedOnly ? 'saved-feed-view' : ''}`}>
      <PageHeader
        title={
          savedOnly
            ? 'The good stuff, kept close.'
            : `Hey ${firstName}, find your next spark.`
        }
        description={
          savedOnly
            ? 'Ideas and conversations worth coming back to.'
            : 'A little inspiration. A new connection. Something worth building.'
        }
      />

      <div className="home-layout">
        <section className="feed-column">
          {savedOnly && (
            <div style={{ marginBottom: 14 }}>
              <div
                className="saved-filter-bar"
                role="tablist"
                aria-label="Filter saved items"
              >
                {SAVED_CATEGORIES.map((cat) => {
                  const count = savedCategoryCounts[cat.id] || 0;
                  const isActive = savedFilter === cat.id;
                  return (
                    <button
                      key={cat.id}
                      type="button"
                      role="tab"
                      aria-selected={isActive}
                      className={`saved-filter-pill ${isActive ? 'active' : ''}`}
                      onClick={() => setSavedFilter(cat.id)}
                    >
                      <span>{cat.label}</span>
                      <span className="saved-filter-pill-badge">{count}</span>
                    </button>
                  );
                })}
              </div>

              <div className="saved-category-banner">
                <div className="saved-category-text">
                  <strong>{SAVED_CATEGORIES.find((c) => c.id === savedFilter)?.label}</strong>
                  <span>·</span>
                  <small>{SAVED_CATEGORIES.find((c) => c.id === savedFilter)?.desc}</small>
                </div>
                <Tag tone="coral">
                  {savedCategoryCounts[savedFilter] || 0}{' '}
                  {(savedCategoryCounts[savedFilter] || 0) === 1 ? 'item' : 'items'}
                </Tag>
              </div>
            </div>
          )}

          {!savedOnly && (
            <>
              <div className="feed-tabs">
                <Tabs
                  items={['For you', 'Following', 'Latest']}
                  value={tab}
                  onChange={setTab}
                />
                <Button
                  variant="primary"
                  icon={Plus}
                  className="feed-create-btn"
                  onClick={() => a.setModal({ type: 'composer' })}
                  style={{ height: '34px', fontSize: '13px', padding: '0 13px', borderRadius: '7px' }}
                >
                  Create post
                </Button>
                <IconButton
                  icon={SlidersHorizontal}
                  label="Feed interests"
                  onClick={() => setFilters(true)}
                />
              </div>

              <div className="feed-label">
                <span>
                  {tab === 'Following'
                    ? 'FROM YOUR PEOPLE'
                    : 'A FEW THINGS YOU MIGHT LOVE'}
                </span>
                <span className="line" />
              </div>
            </>
          )}

          {a.feedState.error && !savedOnly && (
            <p className="system-notice" role="status">
              {a.feedState.error}
            </p>
          )}

          {savedOnly ? (
            filteredSavedItems.length ? (
              filteredSavedItems.map((item) => {
                if (item.kind === 'event') {
                  return <SavedEventCard key={item.key} event={item.data} app={a} />;
                }
                if (item.kind === 'job') {
                  return <SavedJobCard key={item.key} job={item.data} app={a} />;
                }
                if (item.kind === 'project') {
                  return <SavedProjectCard key={item.key} project={item.data} app={a} />;
                }
                return (
                  <PostCard
                    key={item.key}
                    post={item.data}
                    app={a}
                    onDiscuss={setDiscussion}
                    onShare={setSharePost}
                  />
                );
              })
            ) : (
              <Empty
                title={
                  savedFilter === 'All'
                    ? 'Your collection starts here'
                    : `No saved ${savedFilter.toLowerCase()} yet`
                }
                text={
                  savedFilter === 'All'
                    ? 'Save posts, photos, articles, polls, projects, milestones, events, or jobs to find them here.'
                    : `Save any ${savedFilter.toLowerCase()} from your feed or spaces to keep them organized here.`
                }
                action="Explore feed"
                onAction={() => a.go('Home')}
              />
            )
          ) : feed.length ? (
            feed.map((post) => (
              <PostCard
                key={post.id}
                post={post}
                app={a}
                onDiscuss={setDiscussion}
                onShare={setSharePost}
              />
            ))
          ) : (
            <Empty
              title="Your feed is quiet"
              text="Follow a few people to see what they are making."
              action="Explore your network"
              onAction={() => a.go('Explore')}
            />
          )}

          <div className="feed-end">
            {a.feedState.nextCursor ? (
              <Button
                variant="quiet"
                onClick={a.loadMoreFeed}
                disabled={a.feedState.loading}
              >
                {a.feedState.loading ? 'Loading updates…' : 'Load more updates'}
              </Button>
            ) : (
              "You're in good company."
            )}
          </div>
        </section>

        <RightRail />
      </div>

      {discussion && (
        <Discussion
          post={discussion}
          app={a}
          comment={comment}
          setComment={setComment}
          error={commentError}
          onSubmit={submitComment}
          onClose={() => setDiscussion(null)}
        />
      )}

      {filters && <InterestFilter app={a} onClose={() => setFilters(false)} />}

      {sharePost && (
        <ShareDialog
          post={sharePost}
          app={a}
          onClose={() => setSharePost(null)}
        />
      )}
    </div>
  );
}

// ─── Saved Item Cards for Events, Jobs, Projects ────────────────────────────

function SavedEventCard({ event, app }) {
  return (
    <article className="post-card saved-card saved-event-card">
      <header className="saved-card-header">
        <div className="saved-card-main">
          <span className="date-tile" style={{ flexShrink: 0 }}>
            <small>{event.month || 'EVENT'}</small>
            <b>{event.day || '📅'}</b>
          </span>
          <div className="saved-card-info">
            <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' }}>
              <Tag tone="coral">{event.category || 'Event'}</Tag>
              <span style={{ fontSize: '11px', color: 'var(--muted)' }}>{event.time}</span>
            </div>
            <h3 style={{ margin: '4px 0 2px', fontSize: '15px', wordBreak: 'break-word' }}>{event.title}</h3>
            <p style={{ margin: 0, fontSize: '12px', color: 'var(--muted)' }}>
              <MapPin size={12} style={{ verticalAlign: '-1px', marginRight: 3 }} />
              {event.location} · {event.mode || 'In person'}
            </p>
          </div>
        </div>
        <IconButton
          icon={Bookmark}
          label="Remove from saved"
          style={{ color: 'var(--coral)', flexShrink: 0 }}
          onClick={() => (app.toggleSaveEvent ? app.toggleSaveEvent(event.id) : app.toggleSave(event.id))}
        />
      </header>

      <p style={{ margin: '8px 0 12px', fontSize: '13.5px', color: 'var(--ink)' }}>{event.description}</p>

      <div className="saved-card-footer">
        <span style={{ fontSize: '12px', color: 'var(--muted)' }}>
          {event.attendees ? `${event.attendees} attending` : 'Open registration'}
        </span>
        <Button
          variant="quiet"
          icon={ArrowRight}
          onClick={() => app.setModal({ type: 'details', kind: 'event', item: event })}
        >
          View event details
        </Button>
      </div>
    </article>
  );
}

function SavedJobCard({ job, app }) {
  return (
    <article className="post-card saved-card saved-job-card">
      <header className="saved-card-header">
        <div className="saved-card-main">
          <div className="course-mark blue" style={{ flexShrink: 0 }}>
            <BriefcaseBusiness size={20} />
          </div>
          <div className="saved-card-info">
            <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' }}>
              <Tag tone="blue">{job.type || 'Role'}</Tag>
              <span style={{ fontSize: '11px', color: 'var(--muted)' }}>{job.salary}</span>
            </div>
            <h3 style={{ margin: '4px 0 2px', fontSize: '15px', wordBreak: 'break-word' }}>{job.title}</h3>
            <p style={{ margin: 0, fontSize: '12px', color: 'var(--muted)' }}>
              {job.company} · {job.location} ({job.mode})
            </p>
          </div>
        </div>
        <IconButton
          icon={Bookmark}
          label="Remove from saved"
          style={{ color: 'var(--coral)', flexShrink: 0 }}
          onClick={() => app.toggleSave(job.id)}
        />
      </header>

      <p style={{ margin: '8px 0 12px', fontSize: '13.5px', color: 'var(--ink)' }}>{job.description}</p>

      <div className="saved-card-footer">
        <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
          {(job.tags || []).map((t) => (
            <Tag key={t} tone="quiet">#{t}</Tag>
          ))}
        </div>
        <Button
          variant="quiet"
          icon={ArrowRight}
          onClick={() => app.setModal({ type: 'details', kind: 'job', item: job })}
        >
          View opportunity
        </Button>
      </div>
    </article>
  );
}

function SavedProjectCard({ project, app }) {
  return (
    <article className="post-card saved-card saved-project-card">
      <header className="saved-card-header">
        <div className="saved-card-main">
          <div className="course-mark green" style={{ flexShrink: 0 }}>
            <FolderKanban size={20} />
          </div>
          <div className="saved-card-info">
            <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' }}>
              <Tag tone="green">{project.category || 'Project'}</Tag>
              <span style={{ fontSize: '11px', color: 'var(--muted)' }}>{project.members || 1} contributors</span>
            </div>
            <h3 style={{ margin: '4px 0 2px', fontSize: '15px', wordBreak: 'break-word' }}>{project.title}</h3>
            <p style={{ margin: 0, fontSize: '12px', color: 'var(--muted)' }}>
              {project.roles || 'Open collaboration'}
            </p>
          </div>
        </div>
        <IconButton
          icon={Bookmark}
          label="Remove from saved"
          style={{ color: 'var(--coral)', flexShrink: 0 }}
          onClick={() => app.toggleSave(project.id)}
        />
      </header>

      <p style={{ margin: '8px 0 12px', fontSize: '13.5px', color: 'var(--ink)' }}>{project.description}</p>

      <div className="saved-card-footer">
        <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
          {(project.tags || []).map((t) => (
            <Tag key={t} tone="quiet">#{t}</Tag>
          ))}
        </div>
        <Button
          variant="quiet"
          icon={ArrowRight}
          onClick={() => app.setModal({ type: 'details', kind: 'project', item: project })}
        >
          View project
        </Button>
      </div>
    </article>
  );
}

// ─── 2. Post Card & Sub-Components ──────────────────────────────────────────

function LinkedInCommentNode({
  comment,
  postId,
  app,
  commentLikes,
  toggleCommentLike,
  depth = 0,
  replyingTo,
  setReplyingTo,
  expandedThreads,
  toggleThreadExpanded,
  onReplySubmit,
}) {
  const isChild = depth > 0;
  const cId = comment.id || `c-${postId}-${(comment.body || '').slice(0, 10)}`;
  const authorName = typeof comment === 'object' && (comment.author || comment.name)
    ? (comment.author || comment.name)
    : app.profile.name;
  const role = typeof comment === 'object' && comment.role ? comment.role : 'Member';
  const time = typeof comment === 'object' && comment.time ? comment.time : 'Just now';
  const body = typeof comment === 'string' ? comment : comment.body;
  const baseLikes = typeof comment === 'object' && comment.likes ? comment.likes : 0;
  const isLiked = Boolean(commentLikes[cId]);
  const currentLikes = baseLikes + (isLiked ? 1 : 0);
  const replies = typeof comment === 'object' && Array.isArray(comment.replies) ? comment.replies : [];
  const replyCount = replies.length;
  const isExpanded = Boolean(expandedThreads[cId]);
  const isReplyingToThis = replyingTo?.commentId === cId;
  const [replyInputText, setReplyInputText] = useState('');
  const replyInputRef = useRef(null);

  useEffect(() => {
    if (isReplyingToThis) {
      setTimeout(() => replyInputRef.current?.focus(), 80);
    }
  }, [isReplyingToThis]);

  const handleInlineReply = async (e) => {
    e?.preventDefault();
    if (!replyInputText.trim()) return;
    const textToSend = replyInputText.trim();
    setReplyInputText('');
    setReplyingTo(null);
    await onReplySubmit(comment.id, textToSend);
  };

  return (
    <div className={`linkedin-comment-item ${isChild ? 'is-reply' : ''}`} key={cId} id={`comment-${cId}`}>
      <div
        className={`comment-avatar-tile ${isChild ? 'reply-avatar' : ''}`}
        onClick={() => app.openUserProfile(authorName, 'Posts')}
        style={{ cursor: 'pointer' }}
        title={`View ${authorName}'s profile`}
      >
        <Avatar name={authorName} person={comment.avatar ? { image: comment.avatar } : undefined} />
      </div>

      <div className="comment-bubble-container">
        <div className="comment-speech-bubble">
          <div className="comment-bubble-header">
            <div className="comment-author-meta">
              <button
                type="button"
                className="comment-author-name-btn"
                onClick={() => app.openUserProfile(authorName, 'Posts')}
              >
                <strong>{authorName}</strong>
              </button>
              <span className="comment-author-headline">{role}</span>
            </div>
            <span className="comment-timestamp">{time}</span>
          </div>
          <p className="comment-bubble-text">{body}</p>
        </div>

        <div className="comment-bubble-actions">
          <button
            type="button"
            className={`comment-inline-like-btn ${isLiked ? 'liked' : ''}`}
            onClick={() => toggleCommentLike(cId)}
          >
            <Heart size={12} fill={isLiked ? 'currentColor' : 'none'} />
            <span>Like</span>
            {currentLikes > 0 && <span className="comment-like-badge">· {currentLikes}</span>}
          </button>
          <span className="comment-action-divider">·</span>
          <button
            type="button"
            className="comment-inline-reply-btn"
            onClick={() => {
              if (!isExpanded && replyCount > 0) {
                toggleThreadExpanded(cId);
              }
              setReplyingTo({ commentId: cId, targetName: authorName });
              setReplyInputText(`@${authorName} `);
            }}
          >
            Reply
          </button>

          {/* Tree-type toggle button if comment has any replies */}
          {replyCount > 0 && (
            <>
              <span className="comment-action-divider">·</span>
              <button
                type="button"
                className={`comment-toggle-replies-btn ${isExpanded ? 'expanded' : ''}`}
                onClick={() => toggleThreadExpanded(cId)}
                aria-label={isExpanded ? 'Hide replies' : `View ${replyCount} ${replyCount === 1 ? 'reply' : 'replies'}`}
              >
                <CornerDownRight size={12} className="tree-arrow-icon" />
                <span>
                  {isExpanded
                    ? 'Hide replies'
                    : `${replyCount} ${replyCount === 1 ? 'reply' : 'replies'}`}
                </span>
              </button>
            </>
          )}
        </div>

        {/* Inline Reply Input Box under this comment */}
        {isReplyingToThis && (
          <div className="comment-inline-reply-box">
            <div className="reply-box-avatar">
              <Avatar person={app.profile} name={app.profile.name} />
            </div>
            <form className="comment-inline-reply-form" onSubmit={handleInlineReply}>
              <div className="comment-input-field-wrapper reply-field-wrapper">
                <input
                  ref={replyInputRef}
                  type="text"
                  className="comment-text-input reply-text-input"
                  placeholder={`Reply to ${authorName}...`}
                  value={replyInputText}
                  onChange={(e) => setReplyInputText(e.target.value)}
                  maxLength={1000}
                  aria-label={`Reply to ${authorName}`}
                />
                <button
                  type="submit"
                  className={`comment-circle-send-btn ${replyInputText.trim() ? 'active' : ''}`}
                  disabled={!replyInputText.trim()}
                  title="Send reply"
                  aria-label="Send reply"
                >
                  <Send size={13} />
                </button>
              </div>
              <button
                type="button"
                className="comment-reply-cancel-btn"
                onClick={() => {
                  setReplyingTo(null);
                  setReplyInputText('');
                }}
              >
                Cancel
              </button>
            </form>
          </div>
        )}

        {/* Tree-type nested replies container at any depth */}
        {replyCount > 0 && isExpanded && (
          <div className="comment-replies-tree" aria-label="Replies thread">
            <div className="replies-tree-list">
              {replies.map((reply, rIdx) => (
                <div className="reply-tree-node" key={reply.id || `r-${cId}-${rIdx}`}>
                  <div className="tree-node-connector" aria-hidden="true" />
                  <LinkedInCommentNode
                    comment={reply}
                    postId={postId}
                    app={app}
                    commentLikes={commentLikes}
                    toggleCommentLike={toggleCommentLike}
                    depth={depth + 1}
                    replyingTo={replyingTo}
                    setReplyingTo={setReplyingTo}
                    expandedThreads={expandedThreads}
                    toggleThreadExpanded={toggleThreadExpanded}
                    onReplySubmit={onReplySubmit}
                  />
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function PostCard({ post, app, onDiscuss, onShare }) {
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    if (!menuOpen) return;
    const handleOutside = () => setMenuOpen(false);
    window.addEventListener('click', handleOutside);
    return () => window.removeEventListener('click', handleOutside);
  }, [menuOpen]);

  const person =
    people.find((item) => item.id === post.authorId) || {
      name: post.authorName || app.profile.name,
      role: post.authorHeadline || app.profile.headline,
      id: post.authorId,
    };

  const liked = app.likes.includes(post.id);
  const isSaved = app.saved.includes(post.id);
  const isFollowing = post.authorId && app.following.includes(post.authorId);
  const canFollow =
    post.authorId &&
    post.authorId !== 'me' &&
    post.authorId !== app.profile.profileId;

  const react = async () => {
    try {
      await app.toggleReaction(post.id);
    } catch {
      app.toast('Could not update your reaction. Please try again.');
    }
  };

  // LinkedIn-style expandable comments state
  const postComments = useMemo(() => {
    const userComments = app.comments?.[post.id] || [];
    const seeded = initialComments[post.id] || [];
    const map = new Map();
    for (const item of seeded) {
      const key = item.id || (typeof item === 'string' ? item : item.body);
      map.set(key, item);
    }
    for (const item of userComments) {
      const key = item.id || (typeof item === 'string' ? item : item.body);
      const existing = map.get(key);
      if (existing && existing.replies?.length && (!item.replies || item.replies.length === 0)) {
        map.set(key, { ...item, replies: existing.replies });
      } else {
        map.set(key, item);
      }
    }
    return Array.from(map.values());
  }, [app.comments, post.id]);

  const totalCommentsCount = useMemo(() => {
    function countItem(item) {
      if (!item) return 0;
      let count = 1;
      if (item.replies && Array.isArray(item.replies)) {
        for (const r of item.replies) {
          count += countItem(r);
        }
      }
      return count;
    }
    let total = 0;
    for (const c of postComments) {
      total += countItem(c);
    }
    return total || post.comments || 0;
  }, [postComments, post.comments]);

  const [showComments, setShowComments] = useState(post.id === 'p-me-1');
  const [commentText, setCommentText] = useState('');
  const [commentLikes, setCommentLikes] = useState({});
  const [replyingTo, setReplyingTo] = useState(null); // { commentId, targetName }
  const [expandedThreads, setExpandedThreads] = useState({
    'c-me-1-1': true,
    'c-me-1-1-r1': true,
  });
  const commentInputRef = useRef(null);

  const toggleThreadExpanded = (commentId) => {
    setExpandedThreads((prev) => ({
      ...prev,
      [commentId]: !prev[commentId],
    }));
  };

  const handleSendComment = async (e) => {
    e?.preventDefault();
    if (!commentText.trim()) return;
    const textToSend = commentText.trim();
    setCommentText('');
    try {
      await app.addComment(post.id, textToSend);
    } catch {
      app.toast('Could not post comment. Please try again.');
    }
  };

  const handleReplySubmit = async (parentCommentId, text) => {
    try {
      await app.addComment(post.id, text, parentCommentId);
      setExpandedThreads((prev) => ({
        ...prev,
        [parentCommentId]: true,
      }));
      app.toast('Reply posted');
    } catch {
      app.toast('Could not post reply. Please try again.');
    }
  };

  const toggleCommentLike = (commentId) => {
    setCommentLikes((prev) => ({
      ...prev,
      [commentId]: !prev[commentId],
    }));
  };

  return (
    <article className="post-card">
      <header className="post-person">
        <div
          onClick={() => app.openUserProfile(person, 'Posts')}
          style={{ cursor: 'pointer' }}
          title={`View ${person.name}'s profile`}
        >
          <Avatar person={person} />
        </div>
        <div>
          <div className="person-row-compact">
            <button
              onClick={() =>
                app.openUserProfile(person, 'Posts')
              }
            >
              <strong>{person.name}</strong>
              {post.authorId === 'riya' && (
                <span className="verified">
                  <Check size={10} />
                </span>
              )}
            </button>
            {canFollow && (
              <button
                className={`follow-tag-btn ${isFollowing ? 'following' : ''}`}
                onClick={() => app.toggleFollow(post.authorId)}
              >
                {isFollowing ? 'Following' : 'Follow'}
              </button>
            )}
          </div>
          <p>{person.role}</p>
          <small>
            {post.authorId === 'me' ? 'Just now' : '2h'} · <Globe size={11} />
          </small>
        </div>

        <div className="post-context" style={{ position: 'relative' }}>
          <IconButton
            icon={MoreHorizontal}
            label="Post options"
            onClick={(e) => {
              e.stopPropagation();
              setMenuOpen((prev) => !prev);
            }}
          />

          {menuOpen && (
            <div
              className="dropdown-menu-popover"
              style={{ minWidth: '220px', zIndex: 120, top: 'calc(100% + 4px)', right: 0 }}
              onClick={(e) => e.stopPropagation()}
            >
              <button
                type="button"
                className="dropdown-menu-item"
                onClick={() => {
                  setMenuOpen(false);
                  app.toggleSave(post.id);
                }}
              >
                <Bookmark size={15} />
                <span>{isSaved ? 'Remove from saved' : 'Save post'}</span>
              </button>

              <button
                type="button"
                className="dropdown-menu-item"
                onClick={() => {
                  setMenuOpen(false);
                  app.setPosts((old) =>
                    old.map((p) => (p.id === post.id ? { ...p, hidden: true } : p))
                  );
                  app.toast('Post hidden from your feed');
                }}
              >
                <EyeOff size={15} />
                <span>Hide this post</span>
              </button>

              <button
                type="button"
                className="dropdown-menu-item"
                onClick={() => {
                  setMenuOpen(false);
                  app.setPosts((old) =>
                    old.map((p) => (p.id === post.id ? { ...p, hidden: true } : p))
                  );
                  app.toast('Marked as not interested. We will tune your feed.');
                }}
              >
                <ThumbsDown size={15} />
                <span>Not interested</span>
              </button>

              <button
                type="button"
                className="dropdown-menu-item danger"
                onClick={() => {
                  setMenuOpen(false);
                  app.setModal({
                    type: 'details',
                    kind: 'report-content',
                    item: {
                      contentType: 'social_post',
                      contentId: post.id,
                      title: post.title || post.text,
                    },
                  });
                }}
              >
                <ShieldAlert size={15} />
                <span>Report post</span>
              </button>
            </div>
          )}
        </div>
      </header>

      <div className="post-body">
        <p>{post.text}</p>
        {post.type !== 'card' && post.type !== 'poll' && (
          <div className="post-tags">
            {(post.tags || []).map((tag) => (
              <button
                key={tag}
                onClick={() => {
                  app.go('Explore');
                  app.setSearch(tag);
                }}
              >
                #{tag.replaceAll(' ', '')}
              </button>
            ))}
          </div>
        )}
      </div>

      {post.image && (
        <button
          className="post-media-button"
          aria-label={`Read ${post.title || 'post'}`}
          onClick={() => app.setModal({ type: 'reader', id: post.id })}
        >
          <Media
            src={post.image}
            alt={post.imageAlt || 'Shared post image'}
            className="post-media"
          />
        </button>
      )}

      {post.type === 'card' && (
        <div className="card-story">
          <div>
            <span className="eyebrow">
              {post.category || 'Ideas & insights'} <span>· 4 MIN READ</span>
            </span>
            <h2>{post.title}</h2>
          </div>
          <IconButton
            icon={ArrowUpRight}
            label={`Read ${post.title}`}
            onClick={() => app.setModal({ type: 'reader', id: post.id })}
          />
        </div>
      )}

      {post.type === 'project' && (
        <button className="project-embed" onClick={() => app.go('Projects')}>
          <span className="module-icon green">G</span>
          <span>
            <strong>{post.title}</strong>
            <small>2 open roles · Join the project</small>
          </span>
          <ArrowRight size={20} />
        </button>
      )}

      {post.type === 'poll' && <Poll post={post} app={app} />}

      <div className="post-stats">
        <span>
          <span className="like-dot">
            <Heart size={10} fill="currentColor" />
          </span>
          {(post.likes || 0) + (liked ? 1 : 0)} people found this inspiring
        </span>
        <button
          onClick={() => {
            setShowComments((prev) => !prev);
            setTimeout(() => commentInputRef.current?.focus(), 80);
          }}
          title={showComments ? 'Hide comments' : 'Show comments'}
        >
          {totalCommentsCount} comments
        </button>
      </div>

      <footer className="post-actions">
        <button
          className={liked ? 'selected' : ''}
          aria-pressed={liked}
          onClick={react}
        >
          <Heart size={19} fill={liked ? 'currentColor' : 'none'} />
          Like
        </button>
        <button
          className={showComments ? 'selected' : ''}
          aria-pressed={showComments}
          onClick={() => {
            setShowComments((prev) => !prev);
            setTimeout(() => commentInputRef.current?.focus(), 80);
          }}
        >
          <MessageCircle size={19} />
          Comment
        </button>
        <button onClick={() => onShare(post)}>
          <Share2 size={19} />
          Share
        </button>
        <button
          className={isSaved ? 'selected' : ''}
          aria-label={isSaved ? 'Unsave post' : 'Save post'}
          aria-pressed={isSaved}
          onClick={() => app.toggleSave(post.id)}
        >
          <Bookmark size={19} fill={isSaved ? 'currentColor' : 'none'} />
          <span>Save</span>
        </button>
      </footer>

      {showComments && (
        <div className="post-comments-section" aria-label="Comments section">
          {/* Add Comment Input Row with Circular Send Button beside Text Box */}
          <form className="comment-input-form" onSubmit={handleSendComment}>
            <div className="comment-input-avatar" title={app.profile.name}>
              <Avatar person={app.profile} name={app.profile.name} />
            </div>
            <div className="comment-input-field-wrapper">
              <input
                ref={commentInputRef}
                type="text"
                className="comment-text-input"
                placeholder="Add a comment..."
                value={commentText}
                onChange={(e) => setCommentText(e.target.value)}
                maxLength={1000}
                aria-label="Add a comment"
              />
              <button
                type="submit"
                className={`comment-circle-send-btn ${commentText.trim() ? 'active' : ''}`}
                disabled={!commentText.trim()}
                title="Send comment"
                aria-label="Send comment"
              >
                <Send size={15} />
              </button>
            </div>
          </form>

          {/* LinkedIn-style Comments List with Tree Replies */}
          <div className="post-comments-list">
            {postComments.map((item, idx) => (
              <LinkedInCommentNode
                key={item.id || `c-${post.id}-${idx}`}
                comment={item}
                postId={post.id}
                app={app}
                commentLikes={commentLikes}
                toggleCommentLike={toggleCommentLike}
                isChild={false}
                replyingTo={replyingTo}
                setReplyingTo={setReplyingTo}
                expandedThreads={expandedThreads}
                toggleThreadExpanded={toggleThreadExpanded}
                onReplySubmit={handleReplySubmit}
              />
            ))}
          </div>
        </div>
      )}
    </article>
  );
}

function Poll({ post, app }) {
  const votedIndex = app.votes[post.id];
  const voted = votedIndex !== undefined;
  const total = (post.votes || []).reduce((sum, value) => sum + value, 0) + (voted ? 1 : 0);

  const handleVote = (index) => {
    const optionId = post.pollOptionIds?.[index];
    app.votePoll(post.id, post.pollId, optionId, index);
  };

  return (
    <div className="poll">
      <h3>{post.title}</h3>
      {(post.options || []).map((option, index) => {
        const isSelected = votedIndex === index;
        const optVotes =
          (post.votes?.[index] || 0) + (isSelected ? 1 : 0);
        const percent = total > 0 ? Math.round((optVotes / total) * 100) : 0;

        return (
          <button
            type="button"
            key={option}
            className={isSelected ? 'chosen' : ''}
            onClick={() => handleVote(index)}
            aria-pressed={isSelected}
            title={isSelected ? 'Click to deselect your vote' : voted ? 'Click to switch your vote' : 'Click to vote'}
          >
            <span style={{ width: voted ? `${percent}%` : 0 }} />
            <b>{isSelected ? '✓ ' : ''}{option}</b>
            {voted && (
              <small>
                {percent}% {isSelected ? '· Your vote (click to deselect)' : ''}
              </small>
            )}
          </button>
        );
      })}
      {post.text && post.title && post.text !== post.title && (
        <p className="poll-explanation">{post.text}</p>
      )}
      <small>
        {voted
          ? 'Click another option to change your choice, or click your vote to deselect.'
          : 'Choose one answer.'}
      </small>
    </div>
  );
}

// ─── 3. Modals & Dialogs ────────────────────────────────────────────────────

function Discussion({ post, app, comment, setComment, error, onSubmit, onClose }) {
  const comments = useMemo(() => {
    const userComments = app.comments?.[post.id] || [];
    const seeded = initialComments[post.id] || [];
    const map = new Map();
    for (const item of seeded) {
      const key = item.id || (typeof item === 'string' ? item : item.body);
      map.set(key, item);
    }
    for (const item of userComments) {
      const key = item.id || (typeof item === 'string' ? item : item.body);
      const existing = map.get(key);
      if (existing && existing.replies?.length && (!item.replies || item.replies.length === 0)) {
        map.set(key, { ...item, replies: existing.replies });
      } else {
        map.set(key, item);
      }
    }
    return Array.from(map.values());
  }, [app.comments, post.id]);

  const [commentLikes, setCommentLikes] = useState({});
  const [replyingTo, setReplyingTo] = useState(null);
  const [expandedThreads, setExpandedThreads] = useState({
    'c-me-1-1': true,
    'c-me-1-1-r1': true,
  });

  const toggleThreadExpanded = (commentId) => {
    setExpandedThreads((prev) => ({
      ...prev,
      [commentId]: !prev[commentId],
    }));
  };

  const handleReplySubmit = async (parentCommentId, text) => {
    try {
      await app.addComment(post.id, text, parentCommentId);
      setExpandedThreads((prev) => ({
        ...prev,
        [parentCommentId]: true,
      }));
      app.toast('Reply posted');
    } catch {
      app.toast('Could not post reply. Please try again.');
    }
  };

  const toggleCommentLike = (commentId) => {
    setCommentLikes((prev) => ({
      ...prev,
      [commentId]: !prev[commentId],
    }));
  };

  return (
    <Dialog title="The conversation" onClose={onClose}>
      <p style={{ fontWeight: 600, color: 'var(--ink)' }}>{post.title || post.text}</p>
      <div className="comment-list post-comments-list" style={{ maxHeight: '380px', overflowY: 'auto' }}>
        {comments.map((item, index) => (
          <LinkedInCommentNode
            key={item.id || index}
            comment={item}
            postId={post.id}
            app={app}
            commentLikes={commentLikes}
            toggleCommentLike={toggleCommentLike}
            isChild={false}
            replyingTo={replyingTo}
            setReplyingTo={setReplyingTo}
            expandedThreads={expandedThreads}
            toggleThreadExpanded={toggleThreadExpanded}
            onReplySubmit={handleReplySubmit}
          />
        ))}
        {!comments.length && (
          <p className="muted">Be the first to add a thoughtful comment.</p>
        )}
      </div>

      <form onSubmit={onSubmit} className="comment-input-form" style={{ marginTop: 14 }}>
        <div className="comment-input-avatar" title={app.profile.name}>
          <Avatar person={app.profile} name={app.profile.name} />
        </div>
        <div className="comment-input-field-wrapper">
          <input
            type="text"
            className="comment-text-input"
            placeholder="Add a comment..."
            value={comment}
            onChange={(event) => setComment(event.target.value)}
            required
            maxLength={2000}
            aria-label="Your comment"
          />
          <button
            type="submit"
            className={`comment-circle-send-btn ${comment.trim() ? 'active' : ''}`}
            disabled={!comment.trim()}
            title="Send comment"
            aria-label="Send comment"
          >
            <Send size={15} />
          </button>
        </div>
      </form>
      {error && (
        <p className="auth-error" role="alert" style={{ marginTop: 8 }}>
          {error}
        </p>
      )}
    </Dialog>
  );
}

function ShareDialog({ post, app, onClose }) {
  const [note, setNote] = useState('Thought you might find this useful.');

  const suggested = [
    ...people.filter((person) => app.following.includes(person.id)),
    ...people.filter((person) => !app.following.includes(person.id)),
  ].slice(0, 5);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(`${location.origin}/#Home`);
      app.toast('Post link copied.');
    } catch {
      app.toast('Copy the address from your browser to share this post.');
    }
  };

  const [sentPeople, setSentPeople] = useState([]);

  const shareTo = (person) => {
    const text = `${note.trim() ? `${note.trim()}\n\n` : ''}Shared: ${
      post.title || post.text.slice(0, 88)
    }`;

    app.setThreads((old) => {
      const existing = old.find((thread) => thread.name === person.name);
      if (existing) {
        return old.map((thread) =>
          thread.id === existing.id
            ? {
                ...thread,
                messages: [
                  ...thread.messages,
                  { id: crypto.randomUUID(), mine: true, text, time: 'Now' },
                ],
              }
            : thread
        );
      }
      return [
        ...old,
        {
          id: crypto.randomUUID(),
          name: person.name,
          context: 'Direct',
          unread: false,
          messages: [
            { id: crypto.randomUUID(), mine: true, text, time: 'Now' },
          ],
        },
      ];
    });

    setSentPeople((prev) => [...prev, person.id]);
    app.notify('Post shared', `Sent to ${person.name}.`, 'Messages');
    app.toast(`Shared with ${person.name}`);
  };

  return (
    <Dialog title="Share post" onClose={onClose} wide>
      <div className="share-dialog">
        <div className="share-preview">
          <span className="eyebrow">FROM YOUR FEED</span>
          <strong>{post.title || post.text.slice(0, 110)}</strong>
        </div>

        <label className="field">
          <span>Add a note</span>
          <textarea
            rows="2"
            value={note}
            onChange={(event) => setNote(event.target.value)}
            maxLength={280}
            placeholder="Add your thoughts or why you are sharing this..."
          />
        </label>

        <div className="share-actions">
          <Button icon={Link2} onClick={copy}>
            Copy link
          </Button>
          <Button
            variant="quiet"
            icon={MessageCircle}
            onClick={() => {
              app.go('Messages');
              onClose();
            }}
          >
            Open messages
          </Button>
        </div>

        <h3 style={{ margin: '14px 0 6px', fontSize: '0.88rem', fontWeight: 600, color: 'var(--muted)' }}>
          Send to connections
        </h3>
        <div className="share-connections-list">
          {suggested.map((person) => {
            const isSent = sentPeople.includes(person.id);
            return (
              <div key={person.id} className="share-connection-row">
                <Avatar person={person} size="sm" />
                <div className="share-connection-details">
                  <strong>{person.name}</strong>
                  <p>{person.role}</p>
                </div>
                <button
                  type="button"
                  className={`share-send-btn ${isSent ? 'sent' : ''}`}
                  onClick={() => !isSent && shareTo(person)}
                  disabled={isSent}
                >
                  {isSent ? (
                    <>
                      <Check size={13} />
                      <span>Sent</span>
                    </>
                  ) : (
                    <>
                      <Send size={13} />
                      <span>Send</span>
                    </>
                  )}
                </button>
              </div>
            );
          })}
        </div>
      </div>
    </Dialog>
  );
}

function InterestFilter({ app, onClose }) {
  const interests = [
    'Design',
    'React',
    'Flutter',
    'Projects',
    'Learning',
    'Career',
    'Climate',
    'Open source',
  ];

  const toggleInterest = (interest) => {
    const current = app.profile.interests;
    const updated = current.includes(interest)
      ? current.filter((value) => value !== interest)
      : [...current, interest];
    app.setProfile({ ...app.profile, interests: updated });
  };

  return (
    <Dialog title="What interests you?" onClose={onClose}>
      <p className="muted">
        Your interests help put relevant conversations first.
      </p>
      <div className="choice-grid">
        {interests.map((interest) => (
          <label className="check-choice" key={interest}>
            <input
              type="checkbox"
              checked={app.profile.interests.includes(interest)}
              onChange={() => toggleInterest(interest)}
            />
            {interest}
          </label>
        ))}
      </div>
      <Button variant="primary" onClick={onClose}>
        Done
      </Button>
    </Dialog>
  );
}
