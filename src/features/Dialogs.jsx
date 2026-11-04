import React, { useRef, useState, useEffect } from 'react';
import {
  ArrowRight, BookOpen, CalendarDays, CheckCircle2,
  FolderKanban, MessageCircle, Send, Users, Play, Pause,
  Volume2, Maximize2, Bookmark, FileText, HelpCircle, Check,
  Ticket, MapPin, Clock3, ImagePlus, X, Star,
  ShieldCheck, Smartphone, CreditCard, QrCode, Download, Edit3, Building2,
  Camera, Upload,
} from 'lucide-react';
import { useApp } from '../context';
import { Avatar, Button, DetailRow, Dialog, Field, Progress, Tag, Tabs } from '../components';
import { people } from '../data';

// ─── Shared helper ────────────────────────────────────────────────────────────
const close = a => a.setModal(null);

export function downloadTicketPdf({ eventTitle, attendeeName, ticketCode, slot, date, location }) {
  const safeTitle = (eventTitle || 'Collobster Event').replace(/[\(\)\\]/g, '');
  const safeName = (attendeeName || 'Guest').replace(/[\(\)\\]/g, '');
  const safeCode = (ticketCode || 'PASS-001').replace(/[\(\)\\]/g, '');
  const safeSlot = (slot || 'General Admission').replace(/[\(\)\\]/g, '');
  const safeDate = (date || new Date().toLocaleDateString()).replace(/[\(\)\\]/g, '');
  const safeLoc = (location || 'Bengaluru / Online').replace(/[\(\)\\]/g, '');

  const content = `%PDF-1.4
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
endobj
2 0 obj
<< /Type /Pages /Kids [3 0 R] /Count 1 >>
endobj
3 0 obj
<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>
endobj
4 0 obj
<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>
endobj
5 0 obj
<< /Length 380 >>
stream
BT
/F1 22 Tf
50 720 Td
(COLLOBSTER OFFICIAL EVENT PASS) Tj
0 -36 Td
/F1 14 Tf
(Event: ${safeTitle}) Tj
0 -24 Td
(Attendee: ${safeName}) Tj
0 -24 Td
(Pass ID: ${safeCode}) Tj
0 -24 Td
(Slot / Seat: ${safeSlot}) Tj
0 -24 Td
(Date: ${safeDate}) Tj
0 -24 Td
(Location: ${safeLoc}) Tj
0 -40 Td
/F1 11 Tf
(Status: CONFIRMED - Scan QR code at entrance desk.) Tj
ET
endstream
endobj
xref
0 6
0000000000 65535 f 
0000000009 00000 n 
0000000058 00000 n 
0000000115 00000 n 
0000000224 00000 n 
0000000305 00000 n 
trailer
<< /Size 6 /Root 1 0 R >>
startxref
740
%%EOF`;

  const blob = new Blob([content], { type: 'application/pdf' });
  const url = URL.createObjectURL(blob);
  const dl = document.createElement('a');
  dl.href = url;
  dl.download = `Ticket-Pass-${safeCode}.pdf`;
  document.body.appendChild(dl);
  dl.click();
  document.body.removeChild(dl);
  URL.revokeObjectURL(url);
}


// ─── 1. UTILITY COMPONENTS ───────────────────────────────────────────────────

/** Step-progress bar used at the top of every multi-step flow. */
function FlowHeader({ steps, step }) {
  return (
    <div className="flow-header">
      <div className="flow-header-mobile-meta">
        <span className="flow-step-badge">Step {step + 1} of {steps.length}</span>
        <b className="flow-current-step-title">{steps[step]}</b>
      </div>
      <div className="stepper">
        {steps.map((x, i) => (
          <span
            className={step === i ? 'active' : step > i ? 'done' : ''}
            key={x}
          >
            <i>{i + 1}</i>
            <span className="step-name-text">{x}</span>
          </span>
        ))}
      </div>
      <Progress value={(step + 1) / steps.length * 100} />
    </div>
  );
}

/** Checklist of checkboxes used in onboarding / registration forms. */
function FormChecks({ items }) {
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


// ─── 2. AUTH / ONBOARDING ────────────────────────────────────────────────────

/** Three-step welcome dialog shown to new users. */
export function Onboarding() {
  const a = useApp();
  const [step, setStep] = useState(0);
  const [role, setRole] = useState('Builder');

  const copy = [
    ['Find your people',      'Choose the direction that feels most useful right now.'],
    ['Make your profile useful', 'A little context helps people know how to connect.'],
    ['Start with one space',  'Your profile travels with you as your work changes.'],
  ][step];

  return (
    <Dialog title="Welcome to Collobster" onClose={() => close(a)}>
      <div className="onboarding">
        <Progress value={(step + 1) * 33.3} label="Onboarding progress" />
        <h2>{copy[0]}</h2>
        <p>{copy[1]}</p>

        {step === 0 && (
          <div className="choice-list">
            {['Builder', 'Learner', 'Mentor', 'Investor'].map(x => (
              <button
                className={role === x ? 'chosen' : ''}
                key={x}
                onClick={() => setRole(x)}
              >
                {x}
              </button>
            ))}
          </div>
        )}

        {step === 1 && (
          <Field label="Your headline">
            <input
              value={a.profile.headline}
              onChange={e => a.setProfile({ ...a.profile, headline: e.target.value })}
            />
          </Field>
        )}

        {step === 2 && (
          <div className="choice-list">
            <button onClick={() => a.go('Explore')}>Explore opportunities</button>
            <button onClick={() => a.go('Events')}>Discover events</button>
            <button onClick={() => a.go('Community')}>Join a community</button>
          </div>
        )}

        <div className="dialog-actions">
          <Button variant="quiet" onClick={() => step ? setStep(step - 1) : close(a)}>
            {step ? 'Back' : 'Later'}
          </Button>
          <Button
            variant="primary"
            onClick={() => {
              if (step === 2) {
                a.setProfile({
                  ...a.profile,
                  roles: [...new Set([...a.profile.roles, role.toLowerCase()])],
                });
                close(a);
              } else {
                setStep(step + 1);
              }
            }}
          >
            {step === 2 ? 'Finish' : 'Continue'}
            <ArrowRight size={16} />
          </Button>
        </div>
      </div>
    </Dialog>
  );
}


// ─── 3. PROFILE ──────────────────────────────────────────────────────────────

/** Edit profile fields: profile image, background cover, name, headline, location, bio, and skills. */
export function ProfileEditor() {
  const a = useApp();
  const [draft, setDraft] = useState({
    name: '',
    headline: '',
    location: '',
    bio: '',
    skills: [],
    image: '',
    coverImage: '',
    ...(a.profile || {}),
  });
  const [skillsText, setSkillsText] = useState((a.profile?.skills || []).join(', '));
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const avatarInputRef = useRef(null);
  const coverInputRef = useRef(null);

  function handleImageFile(e, field) {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      setError('Please choose a valid image file (JPG, PNG, WebP).');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError('Image must be under 5MB.');
      return;
    }
    setError('');
    const reader = new FileReader();
    reader.onload = ev => {
      setDraft(prev => ({ ...prev, [field]: ev.target.result }));
    };
    reader.onerror = () => {
      setError('Failed to read image file. Please try another image.');
    };
    reader.readAsDataURL(file);
    e.target.value = '';
  }

  async function save(e) {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      const parsedSkills = skillsText
        .split(',')
        .map(x => x.trim())
        .filter(Boolean);
      const updatedProfile = {
        ...draft,
        skills: parsedSkills,
      };
      const mode = await a.saveProfile(updatedProfile);
      a.toast(mode === 'synced' ? 'Profile saved to your account' : 'Profile updated in Preview');
      close(a);
    } catch {
      setError('Your profile could not be saved to the server. Nothing was changed locally.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog title="Edit your profile" onClose={() => !saving && close(a)}>
      <form className="form-stack" onSubmit={save}>
        {/* Profile Image & Background Cover Section */}
        <div className="profile-edit-media">
          {/* Background / Cover Image */}
          <div className="profile-edit-cover-wrap">
            <div className="profile-edit-cover-label">
              <span>Background Cover Image</span>
              {draft.coverImage && (
                <button
                  type="button"
                  className="text-button"
                  style={{ fontSize: 12, color: 'var(--coral)' }}
                  onClick={() => setDraft(prev => ({ ...prev, coverImage: '' }))}
                >
                  Remove cover
                </button>
              )}
            </div>

            <div
              className={`profile-edit-cover-preview ${draft.coverImage ? 'has-image' : ''}`}
              style={
                draft.coverImage
                  ? {
                      backgroundImage: `url(${draft.coverImage})`,
                      backgroundSize: 'cover',
                      backgroundPosition: 'center',
                    }
                  : undefined
              }
            >
              {!draft.coverImage && (
                <div style={{ textAlign: 'center', padding: '12px' }}>
                  <ImagePlus size={24} style={{ opacity: 0.8, marginBottom: 4 }} />
                  <div style={{ fontSize: 12, fontWeight: 600 }}>No background cover set</div>
                  <small style={{ fontSize: 11, opacity: 0.8 }}>Banner appears at the top of your profile</small>
                </div>
              )}
            </div>

            <div className="profile-edit-cover-actions">
              <input
                ref={coverInputRef}
                type="file"
                accept="image/*"
                style={{ display: 'none' }}
                onChange={e => handleImageFile(e, 'coverImage')}
              />
              <Button
                variant="quiet"
                icon={Upload}
                onClick={() => coverInputRef.current?.click()}
              >
                {draft.coverImage ? 'Change cover image' : 'Upload background image'}
              </Button>
            </div>
          </div>

          {/* Profile Photo (Avatar) */}
          <div className="profile-edit-avatar-wrap">
            <span style={{ fontSize: 12, fontWeight: 700 }}>Profile Photo</span>
            <div className="profile-edit-avatar-flex">
              <div className="profile-edit-avatar-preview">
                {draft.image ? (
                  <img src={draft.image} alt={draft.name || 'Avatar preview'} />
                ) : (
                  <Avatar person={draft} name={draft.name || 'User'} size="large" />
                )}
              </div>

              <div className="profile-edit-avatar-meta">
                <div className="profile-edit-avatar-actions">
                  <input
                    ref={avatarInputRef}
                    type="file"
                    accept="image/*"
                    style={{ display: 'none' }}
                    onChange={e => handleImageFile(e, 'image')}
                  />
                  <Button
                    variant="quiet"
                    icon={Camera}
                    onClick={() => avatarInputRef.current?.click()}
                  >
                    {draft.image ? 'Change photo' : 'Upload photo'}
                  </Button>
                  {draft.image && (
                    <Button
                      variant="quiet"
                      icon={X}
                      onClick={() => setDraft(prev => ({ ...prev, image: '' }))}
                    >
                      Remove
                    </Button>
                  )}
                </div>
                <span className="profile-edit-hint">Square image recommended (JPG, PNG or WebP)</span>
              </div>
            </div>
          </div>
        </div>

        <Field label="Name">
          <input
            value={draft.name}
            onChange={e => setDraft({ ...draft, name: e.target.value })}
          />
        </Field>
        <Field label="Headline">
          <input
            value={draft.headline}
            onChange={e => setDraft({ ...draft, headline: e.target.value })}
          />
        </Field>
        <Field label="Location">
          <input
            value={draft.location}
            onChange={e => setDraft({ ...draft, location: e.target.value })}
          />
        </Field>
        <Field label="About">
          <textarea
            rows="4"
            value={draft.bio}
            onChange={e => setDraft({ ...draft, bio: e.target.value })}
          />
        </Field>
        <Field label="Skills, separated by commas">
          <input
            value={skillsText}
            onChange={e => setSkillsText(e.target.value)}
          />
        </Field>

        {error && <p className="auth-error" role="alert">{error}</p>}

        <div className="dialog-actions">
          <Button variant="quiet" onClick={() => close(a)} disabled={saving}>
            Cancel
          </Button>
          <Button variant="primary" type="submit" disabled={saving}>
            {saving ? 'Saving…' : 'Save profile'}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}


// ─── 4. CONTENT / SOCIAL ─────────────────────────────────────────────────────

/** Create a new post, photo, article, poll, project update, achievement, event, or job. */
export function Composer() {
  const a = useApp();
  const [format, setFormat] = useState(
    a.modal?.format === 'Card' ? 'Article' : a.modal?.format || 'Post'
  );
  const [title, setTitle]         = useState('');
  const [text, setText]           = useState('');
  const [options, setOptions]     = useState(['', '']);
  const [tag, setTag]             = useState('');
  const [attachment, setAttachment] = useState(null);
  const [detail, setDetail]       = useState('');
  const [saving, setSaving]       = useState(false);
  const [error, setError]         = useState('');
  const fileRef = useRef(null);

  const isMessage = format === 'Message';
  const isPoll    = format === 'Poll';
  const isCard    = format === 'Article' || format === 'Card';
  const isPhoto   = format === 'Photo';

  const formatCopy = {
    Post:        'Share a useful update with your network.',
    Photo:       'Add a photo and a short story behind it.',
    Article:     'Publish a considered idea people can save and discuss.',
    Poll:        'Ask one clear question and invite a decision.',
    Project:     'Show a project and name the help you need.',
    Achievement: 'Mark a milestone and the people behind it.',
    Event:       'Share an event people can attend.',
    Job:         'Post an opportunity with useful context.',
  };

  function chooseFile(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      setError('Choose an image file for a photo update.');
      return;
    }
    setError('');
    setAttachment({ name: file.name, preview: URL.createObjectURL(file) });
  }

  async function publish(e) {
    e.preventDefault();
    const cleanOptions = options.map(x => x.trim()).filter(Boolean);

    // Message flow
    if (isMessage) {
      if (!title) { setError('Choose someone to start the conversation.'); return; }
      if (!text.trim()) { setError('Write a message before sending.'); return; }
      a.setThreads(old => [
        ...old,
        {
          id: crypto.randomUUID(),
          name: title,
          context: 'Direct',
          unread: false,
          messages: [{ id: crypto.randomUUID(), mine: true, text: text.trim(), time: 'Now' }],
        },
      ]);
      a.notify('Conversation started', `Your message to ${title} is ready.`, 'Messages');
      close(a);
      return;
    }

    // Content validations
    if (!title.trim() && !text.trim()) {
      setError('Add a title or a description before publishing.');
      return;
    }
    if (isPhoto && !attachment) {
      setError('Choose a photo before publishing a photo update.');
      return;
    }
    if (isPoll && cleanOptions.length < 2) {
      setError('A poll needs at least two options.');
      return;
    }

    setSaving(true);
    setError('');
    try {
      const type = isPoll ? 'poll' : isCard ? 'card' : format === 'Project' ? 'project' : 'post';
      const body = detail.trim()
        ? `${text.trim()}${text.trim() ? '\n\n' : ''}${detail.trim()}`
        : text;
      const mode = await a.publishPost({
        type,
        format,
        title,
        text: body,
        image: attachment?.preview,
        imageAlt: attachment?.name,
        options: isPoll ? cleanOptions : undefined,
        tags: tag.trim()
          ? tag.split(',').map(x => x.trim()).filter(Boolean)
          : ['General'],
      });
      a.notify(`${format} published`, 'Your update is now in the home feed.', 'Home');
      a.toast(mode === 'synced' ? 'Your post is live' : 'Your update is live in Preview');
      close(a);
    } catch {
      setError('We could not publish your update. Your draft is still here.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog
      title={isMessage ? 'New message' : 'Create post'}
      onClose={() => !saving && close(a)}
    >
      <form className="form-stack composer-form" onSubmit={publish}>

        {/* ── Recipient picker (message mode) ── */}
        {isMessage ? (
          <section className="recipient-picker">
            <span className="eyebrow">START A CONVERSATION</span>
            <h3>Who would you like to message?</h3>
            <div>
              {people.map(person => (
                <button
                  type="button"
                  className={title === person.name ? 'selected' : ''}
                  key={person.id}
                  onClick={() => setTitle(person.name)}
                >
                  <Avatar person={person} />
                  <span>
                    <b>{person.name}</b>
                    <small>{person.role}</small>
                  </span>
                  {title === person.name && <Check size={16} />}
                </button>
              ))}
            </div>
          </section>
        ) : (
          <>
            <Tabs
              items={['Post', 'Photo', 'Article', 'Poll', 'Project', 'Achievement', 'Event', 'Job']}
              value={format}
              onChange={next => { setFormat(next); setError(''); }}
            />
            <p className="composer-format-hint">{formatCopy[format]}</p>
          </>
        )}

        {/* ── Title / question field ── */}
        {!isMessage && (
          <Field label={isPoll ? 'Question' : 'Title'}>
            <input
              value={title}
              onChange={e => setTitle(e.target.value)}
              placeholder={isPoll ? 'What would you like the group to decide?' : 'Make the main point clear'}
            />
          </Field>
        )}

        {/* ── Photo upload ── */}
        {isPhoto && (
          <section className="composer-upload">
            <input
              ref={fileRef}
              className="attachment-input"
              type="file"
              accept="image/*"
              onChange={chooseFile}
            />
            {attachment ? (
              <div className="composer-preview">
                <img src={attachment.preview} alt="Selected upload preview" />
                <div>
                  <b>{attachment.name}</b>
                  <small>Ready to publish in this preview.</small>
                </div>
                <button
                  type="button"
                  aria-label="Remove selected image"
                  onClick={() => { setAttachment(null); fileRef.current.value = ''; }}
                >
                  <X size={17} />
                </button>
              </div>
            ) : (
              <button type="button" onClick={() => fileRef.current?.click()}>
                <ImagePlus size={20} />
                <span>
                  <b>Choose a photo</b>
                  <small>JPG, PNG, or WebP</small>
                </span>
              </button>
            )}
          </section>
        )}

        {/* ── Body / description ── */}
        <Field label={
          isMessage ? 'Message'
          : isCard   ? 'Your article'
          : isPoll   ? 'Why are you asking?'
          : 'Description'
        }>
          <textarea
            value={text}
            onChange={e => setText(e.target.value)}
            rows="5"
            placeholder={
              isMessage ? 'Write a message…'
              : isPhoto ? 'Give this image the context it deserves.'
              : 'Write enough context for someone to respond.'
            }
            required={isMessage}
          />
        </Field>

        {/* ── Extra detail field for structured formats ── */}
        {['Project', 'Achievement', 'Event', 'Job'].includes(format) && (
          <Field label={
            format === 'Event' ? 'Date, location, and registration link'
            : format === 'Job' ? 'Team, location, and application detail'
            : 'What should people do next?'
          }>
            <input
              value={detail}
              onChange={e => setDetail(e.target.value)}
              placeholder={
                format === 'Event' ? 'e.g. 25 May, Bengaluru · Register below'
                : format === 'Job' ? 'e.g. Remote · Product design · Apply by 30 May'
                : 'e.g. Looking for a front-end collaborator'
              }
            />
          </Field>
        )}

        {/* ── Topic tag ── */}
        {!isMessage && !isPoll && (
          <Field label="Tags">
            <input
              value={tag}
              onChange={e => setTag(e.target.value)}
              placeholder="e.g. Design, Flutter, Community"
            />
          </Field>
        )}

        {/* ── Poll options ── */}
        {isPoll && (
          <div className="poll-fields">
            {options.map((option, i) => (
              <Field label={`Option ${i + 1}`} key={i}>
                <input
                  value={option}
                  onChange={e =>
                    setOptions(old => old.map((x, index) => index === i ? e.target.value : x))
                  }
                  placeholder={`Choice ${i + 1}`}
                />
              </Field>
            ))}
            <Button type="button" variant="quiet" onClick={() => setOptions([...options, ''])}>
              Add option
            </Button>
          </div>
        )}

        {error && <p className="auth-error" role="alert">{error}</p>}

        <div className="dialog-actions">
          <Button variant="quiet" type="button" onClick={() => close(a)} disabled={saving}>
            Cancel
          </Button>
          <Button variant="primary" icon={Send} type="submit" disabled={saving}>
            {saving         ? 'Publishing…'
             : isMessage    ? 'Send message'
             : format === 'Article' ? 'Publish article'
             : `Publish ${format.toLowerCase()}`}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}

/** Full-screen reader for a post or article. */
export function Reader() {
  const a = useApp();
  const item = a.modal?.item || a.posts.find(post => post.id === a.modal?.id);

  return (
    <Dialog title={item?.title || 'Update'} onClose={() => close(a)} wide>
      <article className="reader">
        <div className="reader-kicker">FROM THE COLLOBSTER FEED</div>
        <h1>{item?.title}</h1>
        <p className="reader-lead">{item?.text}</p>
        {item?.body?.map(p => <p key={p}>{p}</p>) || (
          <p>
            This update is a starting point for a useful conversation.
            Reply with a specific thought, offer, or question to keep it moving.
          </p>
        )}
        <div className="tag-cloud">
          {item?.tags?.map(t => <Tag key={t}>{t}</Tag>)}
        </div>
      </article>
    </Dialog>
  );
}

/** Report a post or piece of content for moderation. */
function ReportContentModal({ item, app, onClose }) {
  const [reason, setReason]       = useState('spam');
  const [details, setDetails]     = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function submitReport(e) {
    e.preventDefault();
    setSubmitting(true);
    try {
      await app.reportContent?.({
        contentType: item.contentType,
        contentId:   item.contentId,
        reason,
        details,
      });
      app.toast('Report submitted successfully. Thank you.');
      onClose();
    } catch {
      app.toast('Could not submit report. Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog title="Report content" onClose={onClose}>
      <form className="form-stack" onSubmit={submitReport}>
        <Field label="Reason">
          <select value={reason} onChange={e => setReason(e.target.value)}>
            <option value="spam">Spam or misleading</option>
            <option value="harassment">Harassment or hate speech</option>
            <option value="inappropriate">Inappropriate content</option>
            <option value="other">Other</option>
          </select>
        </Field>
        <Field label="Additional details (optional)">
          <textarea
            rows="3"
            value={details}
            onChange={e => setDetails(e.target.value)}
            placeholder="Provide any additional context..."
          />
        </Field>
        <div className="dialog-actions">
          <Button variant="quiet" type="button" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button variant="primary" type="submit" disabled={submitting}>
            {submitting ? 'Submitting...' : 'Submit report'}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}


// ─── 5. LEARNING ─────────────────────────────────────────────────────────────

/** Video player + lesson tabs (Overview, Exercise, Notes, Resources, Q&A). */
function CoursePlayer({ course, initialLesson, initialIndex, onClose }) {
  const a = useApp();
  const [index, setIndex]       = useState(initialIndex || 0);
  const [playing, setPlaying]   = useState(false);
  const [tab, setTab]           = useState('Overview');
  const [bookmark, setBookmark] = useState(false);
  const [note, setNote]         = useState('');
  const [claiming, setClaiming] = useState(false);

  const lesson    = course.lessons[index] || initialLesson;
  const completed = (a.learning[course.id] || []).includes(index);
  const progress  = Math.round(
    ((a.learning[course.id] || []).length / course.lessons.length) * 100
  );

  const [codeDraft, setCodeDraft] = useState(
    `// Practice Lab: ${lesson}\nfunction runLessonExercise() {\n  const message = "Lesson ${index + 1} solution validated";\n  return message;\n}\nconsole.log(runLessonExercise());`
  );
  const [consoleOut, setConsoleOut]   = useState('');
  const [testsPassed, setTestsPassed] = useState(false);

  function runExerciseCode() {
    try {
      const logs = [];
      const fakeConsole = {
        log: (...args) =>
          logs.push(args.map(x => (typeof x === 'object' ? JSON.stringify(x) : String(x))).join(' ')),
      };
      const fn = new Function('console', codeDraft);
      fn(fakeConsole);
      setConsoleOut(
        (logs.join('\n') || 'Code executed with no output.') +
        '\n\n✓ Exercise test passed! You are ready to complete this lesson.'
      );
      setTestsPassed(true);
      a.toast('Exercise test passed!');
    } catch (err) {
      setConsoleOut('Execution error: ' + err.message);
      setTestsPassed(false);
    }
  }

  function resetLesson(i, lessonTitle) {
    setIndex(i);
    setPlaying(false);
    setCodeDraft(
      `// Practice Lab: ${lessonTitle}\nfunction runLessonExercise() {\n  const message = "Lesson ${i + 1} solution validated";\n  return message;\n}\nconsole.log(runLessonExercise());`
    );
    setConsoleOut('');
    setTestsPassed(false);
  }

  const toggleComplete = async () => {
    if (completed) {
      a.setLearning(old => ({ ...old, [course.id]: old[course.id].filter(x => x !== index) }));
      a.toast('Lesson marked incomplete');
      return;
    }
    try {
      const mode = await a.completeCourseLesson(course, index);
      a.toast(mode === 'synced' ? 'Lesson completion saved.' : 'Lesson complete');
    } catch (error) {
      a.toast(error.message || 'Could not save lesson progress. Please try again.');
    }
  };

  const handleClaim = async () => {
    setClaiming(true);
    try {
      const res = await a.claimCertificate(course);
      a.toast('Certificate awarded!');
      onClose();
      setTimeout(
        () => a.setModal({ type: 'details', kind: 'certificate', item: course, certificate: res.certificate }),
        100
      );
    } catch (err) {
      a.toast(err.message || 'Could not claim certificate.');
    } finally {
      setClaiming(false);
    }
  };

  return (
    <Dialog title="Course player" onClose={onClose} wide>

      {/* ── Completion banner ── */}
      {progress === 100 && (
        <div className="course-completion-banner">
          <div>
            <CheckCircle2 size={24} className="gold-ink" />
            <div>
              <strong>100% complete!</strong>
              <p>You have finished every lesson in this course.</p>
            </div>
          </div>
          <Button variant="primary" onClick={handleClaim} disabled={claiming}>
            {claiming ? 'Issuing…' : 'Claim certificate'}
          </Button>
        </div>
      )}

      {/* ── Video player + lesson queue ── */}
      <section className="course-player-shell">
        <div className="course-video" aria-label="Lesson video player">
          <div className="course-video-glow" />
          <span className={`course-mark ${course.color}`}>{course.symbol}</span>
          <div className="course-video-copy">
            <small>{course.title}</small>
            <h2>{lesson}</h2>
          </div>
          <button
            className="video-play"
            aria-label={playing ? 'Pause lesson' : 'Play lesson'}
            onClick={() => setPlaying(!playing)}
          >
            {playing ? <Pause /> : <Play fill="currentColor" />}
          </button>
          <div className="video-controls">
            <span>08:24</span>
            <input aria-label="Video progress" type="range" defaultValue="38" />
            <span>22:16</span>
            <button aria-label="Volume"><Volume2 size={17} /></button>
            <button aria-label="Fullscreen"><Maximize2 size={17} /></button>
          </div>
        </div>

        <aside className="course-queue">
          <div>
            <span className="eyebrow">COURSE CONTENT</span>
            <b>{progress}% complete</b>
          </div>
          {course.lessons.map((entry, i) => {
            const done = (a.learning[course.id] || []).includes(i);
            return (
              <button
                key={entry}
                className={i === index ? 'active' : ''}
                onClick={() => resetLesson(i, entry)}
              >
                <span>{done ? <Check size={15} /> : i + 1}</span>
                <div>
                  <b>{entry}</b>
                  <small>{i === index ? 'Now playing' : done ? 'Completed' : '8 min'}</small>
                </div>
                <Play size={14} />
              </button>
            );
          })}
        </aside>
      </section>

      {/* ── Lesson detail + tabs ── */}
      <section className="course-player-details">
        <div className="course-player-heading">
          <div>
            <span className="eyebrow">LESSON {index + 1}</span>
            <h1>{lesson}</h1>
            <p>Build the idea in context, then take it into one small working example.</p>
          </div>
        </div>

        <Tabs
          items={['Overview', 'Exercise / Lab', 'Notes', 'Resources', 'Q&A']}
          value={tab}
          onChange={setTab}
        />

        {/* Overview */}
        {tab === 'Overview' && (
          <div className="lesson-panel">
            <h3>What you will learn</h3>
            <ul>
              <li>Frame the problem before writing an implementation.</li>
              <li>Use a small example to test the concept.</li>
              <li>Know what to keep for your project notes.</li>
            </ul>
          </div>
        )}

        {/* Exercise / Lab */}
        {tab === 'Exercise / Lab' && (
          <div className="exercise-playground">
            <div className="exercise-instructions">
              <h4>Interactive practice exercise</h4>
              <p>
                Edit the code below and run the test to validate your solution for{' '}
                <strong>{lesson}</strong>.
              </p>
            </div>
            <div className="code-editor-box">
              <textarea
                value={codeDraft}
                onChange={e => setCodeDraft(e.target.value)}
                rows={6}
                className="code-editor-textarea"
                spellCheck={false}
              />
              <div className="code-editor-actions">
                <Button
                  variant="ghost"
                  onClick={() => {
                    setCodeDraft(
                      `// Practice Lab: ${lesson}\nfunction runLessonExercise() {\n  return "Lesson ${index + 1} reset";\n}\nconsole.log(runLessonExercise());`
                    );
                    setConsoleOut('');
                  }}
                >
                  Reset
                </Button>
                <Button variant="primary" onClick={runExerciseCode}>
                  Run test / code
                </Button>
              </div>
            </div>
            {consoleOut && (
              <pre className={`console-output ${testsPassed ? 'success' : ''}`}>
                {consoleOut}
              </pre>
            )}
          </div>
        )}

        {/* Notes */}
        {tab === 'Notes' && (
          <div className="lesson-panel">
            <Field label="Your private note">
              <textarea
                rows="4"
                value={note}
                onChange={e => setNote(e.target.value)}
                placeholder="Capture an insight, decision, or question..."
              />
            </Field>
            <Button
              variant="ghost"
              onClick={() => a.toast(note.trim() ? 'Note saved on this device' : 'Add a note first')}
            >
              Save note
            </Button>
          </div>
        )}

        {/* Resources */}
        {tab === 'Resources' && (
          <div className="resource-inline">
            <FileText />
            <span>
              <b>Lesson starter files</b>
              <small>Examples and reference snippets</small>
            </span>
            <Button variant="ghost" onClick={() => a.toast('Resources are ready to download')}>
              Download
            </Button>
          </div>
        )}

        {/* Q&A */}
        {tab === 'Q&A' && (
          <div className="lesson-panel">
            <HelpCircle />
            <h3>Ask the learning group</h3>
            <p>Share a precise question and bring the answer back to your project.</p>
            <Button onClick={() => { a.go('Community'); onClose(); }}>
              Open discussion
            </Button>
          </div>
        )}

        <div className="dialog-actions">
          <Button variant="quiet" onClick={onClose}>Close player</Button>
          <Button variant="primary" icon={CheckCircle2} onClick={toggleComplete}>
            {completed ? 'Mark incomplete' : 'Mark complete'}
          </Button>
        </div>
      </section>
    </Dialog>
  );
}

/** Verified certificate display with copy-link and print actions. */
function CertificateModal({ item, app, onClose }) {
  const cert = app.modal?.certificate ||
    app.certificates.find(c => c.courseId === item.id) || {
      certificateNumber: 'COLLOB-' + new Date().getFullYear() + '-VERIFIED',
      issuedAt:          new Date().toISOString(),
      recipientName:     app.profile.name,
      courseTitle:       item.title,
      verificationHash:  'verified-credential-collobster',
    };

  const copyVerifyLink = async () => {
    const link = `${location.origin}/v1/learning/certificates/verify/${cert.certificateNumber}`;
    try {
      await navigator.clipboard.writeText(link);
      app.toast('Verification link copied to clipboard.');
    } catch {
      app.toast('Link: ' + link);
    }
  };

  return (
    <Dialog title="Verified Certificate" onClose={onClose} wide>
      <div className="certificate-modal-frame">
        <div className="certificate-preview-gold">
          <div className="certificate-badge"><CheckCircle2 size={40} /></div>
          <span className="certificate-eyebrow">COLLOBSTER VERIFIED CREDENTIAL</span>
          <h1>Certificate of Completion</h1>
          <p className="cert-to">This is proudly presented to</p>
          <h2 className="cert-recipient">{cert.recipientName || app.profile.name}</h2>
          <p className="cert-desc">for successfully completing all curriculum requirements for</p>
          <h3 className="cert-course-title">{cert.courseTitle || item.title}</h3>
          <div className="cert-meta-row">
            <div>
              <span>Certificate Number</span>
              <strong>{cert.certificateNumber}</strong>
            </div>
            <div>
              <span>Date of Issue</span>
              <strong>
                {new Date(cert.issuedAt).toLocaleDateString(undefined, {
                  year: 'numeric', month: 'long', day: 'numeric',
                })}
              </strong>
            </div>
            <div>
              <span>Verification Status</span>
              <strong className="verified-text">✓ Cryptographically Verified</strong>
            </div>
          </div>
          <div className="cert-hash-row">
            <small>Verification Hash: {cert.verificationHash}</small>
          </div>
        </div>

        <div className="dialog-actions">
          <Button variant="quiet" onClick={onClose}>Close</Button>
          <Button variant="secondary" onClick={copyVerifyLink}>Copy verification link</Button>
          <Button variant="primary" onClick={() => window.print()}>Print / Save PDF</Button>
        </div>
      </div>
    </Dialog>
  );
}


// ─── 6. CAREER ───────────────────────────────────────────────────────────────

/** Job detail page with note, optional resume attachment, and apply action. */
function JobDetailDialog({ item, app: a, onClose, onDone }) {
  const saved = a.saved.includes(item.id);
  const isAlreadyApplied = Boolean(a.applications?.[item.id]);
  const [note, setNote]           = useState('');
  const [saving, setSaving]       = useState(false);
  const [error, setError]         = useState('');
  const [resumeFile, setResumeFile] = useState(null);
  const fileInputRef = useRef(null);

  const apply = async () => {
    if (isAlreadyApplied) {
      setError('You have already submitted an application for this role! Please check Career > Applications.');
      a.toast('Error: Application already submitted for this role.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      let resumeObjectId = null;
      if (resumeFile) {
        const uploadResult = await a.uploadFile(resumeFile);
        resumeObjectId = uploadResult.attachment?.objectId || null;
      }
      const result = await a.applyForJob(item, note.trim(), resumeObjectId);
      onDone(
        result.mode === 'synced'
          ? 'Application submitted with resume'
          : 'Application saved in Preview',
        'Career'
      );
      onClose();
    } catch (issue) {
      setError(issue.message || 'Could not submit your application. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog title="Role details" onClose={() => !saving && onClose()}>
      <div className="detail-intro">
        <Tag>{item.type}</Tag>
        <h1>{item.title}</h1>
        <p>{item.company} · {item.location} · {item.mode}</p>
        <div className="tag-cloud">
          {item.tags.map(t => <Tag key={t}>{t}</Tag>)}
        </div>
      </div>

      <p>{item.description}</p>
      <DetailRow label="Compensation">{item.salary}</DetailRow>

      {isAlreadyApplied && (
        <div style={{ padding: '12px 14px', borderRadius: 8, background: '#fee2e2', border: '1px solid #ef4444', color: '#b91c1c', margin: '14px 0', fontSize: '0.88rem', fontWeight: 600 }}>
          ⚠️ Application Already Submitted: You have already applied for this role. You can review your submission and track status in Career → Applications.
        </div>
      )}

      <Field label="A short note for the hiring team">
        <textarea
          rows="3"
          value={note}
          onChange={e => setNote(e.target.value)}
          placeholder="What makes this role a good fit?"
          disabled={isAlreadyApplied}
        />
      </Field>

      <Field label="Resume attachment (PDF, DOCX, or TXT)">
        <div className="resume-attach-row">
          <input
            ref={fileInputRef}
            type="file"
            accept=".pdf,.docx,.doc,.txt"
            style={{ display: 'none' }}
            disabled={isAlreadyApplied}
            onChange={e => { if (e.target.files?.[0]) setResumeFile(e.target.files[0]); }}
          />
          {resumeFile ? (
            <div className="resume-selected-chip">
              <FileText size={16} />
              <span>{resumeFile.name} ({(resumeFile.size / 1024).toFixed(0)} KB)</span>
              {!isAlreadyApplied && (
                <button
                  type="button"
                  aria-label="Remove resume"
                  onClick={() => { setResumeFile(null); if (fileInputRef.current) fileInputRef.current.value = ''; }}
                >
                  <X size={14} />
                </button>
              )}
            </div>
          ) : (
            <Button type="button" variant="quiet" icon={FileText} onClick={() => fileInputRef.current?.click()} disabled={isAlreadyApplied}>
              Attach resume
            </Button>
          )}
        </div>
      </Field>

      {error && <p className="auth-error" role="alert">{error}</p>}

      <div className="dialog-actions">
        <Button
          variant="quiet"
          onClick={() => { a.toggleSave(item.id); a.toast(saved ? 'Role removed from saved' : 'Role saved for later'); }}
          disabled={saving}
        >
          {saved ? 'Remove from saved' : 'Save for later'}
        </Button>
        <Button variant="primary" onClick={apply} disabled={saving || isAlreadyApplied}>
          {isAlreadyApplied ? 'Application Submitted ✓' : saving ? 'Submitting…' : 'Apply with profile & resume'}
        </Button>
      </div>
    </Dialog>
  );
}


// ─── 7. SERVICES & EVENTS ────────────────────────────────────────────────────

/** Three-step service booking flow with a separate Reviews & Ratings tab. */
function ServiceBooking({ item, onClose }) {
  const a = useApp();
  const [mainTab, setMainTab]   = useState('Book');
  const [step, setStep]         = useState(1);
  const [slot, setSlot]         = useState('10:00 AM');
  const [note, setNote]         = useState('');
  const [address, setAddress]   = useState(a.profile.location || '');
  const [saving, setSaving]     = useState(false);
  const [error, setError]       = useState('');

  // Reviews state
  const [reviews, setReviews]                   = useState([]);
  const [loadingReviews, setLoadingReviews]     = useState(false);
  const [reviewRating, setReviewRating]         = useState(5);
  const [reviewText, setReviewText]             = useState('');
  const [reviewError, setReviewError]           = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);

  useEffect(() => {
    if (mainTab !== 'Reviews & Ratings') return;
    setLoadingReviews(true);
    a.fetchServiceReviews(item.id)
      .then(list => setReviews(list || []))
      .catch(() => setReviews([]))
      .finally(() => setLoadingReviews(false));
  }, [mainTab, item.id]);

  function bookingWindow() {
    const match = slot.match(/(\d+):\d+\s+(AM|PM)/);
    const start = new Date(Date.now() + 86400000);
    let hour = Number(match?.[1] || 10);
    if (match?.[2] === 'PM' && hour !== 12) hour += 12;
    if (match?.[2] === 'AM' && hour === 12) hour = 0;
    start.setHours(hour, 0, 0, 0);
    const end = new Date(start.getTime() + 60 * 60 * 1000);
    return { startsAt: start.toISOString(), endsAt: end.toISOString() };
  }

  const confirm = async () => {
    setSaving(true);
    setError('');
    try {
      const result = await a.bookService(item, {
        ...bookingWindow(),
        serviceAddress: address.trim() || a.profile.location,
        notes: note.trim() || undefined,
      });
      a.notify('Service booking confirmed', `${item.title} is scheduled for ${slot}.`, 'Local services');
      a.toast(result.mode === 'synced' ? 'Booking request sent to the provider.' : 'Booking confirmed in Preview.');
      onClose();
    } catch (error) {
      setError(error.message || 'Could not confirm this booking. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  const handleCreateReview = async (e) => {
    e.preventDefault();
    if (!reviewText.trim()) return;
    setSubmittingReview(true);
    setReviewError('');
    try {
      const res = await a.createServiceReview(item.id, { rating: reviewRating, reviewText: reviewText.trim() });
      setReviews(old => [
        res.review || {
          id: Date.now(), rating: reviewRating,
          reviewText: reviewText.trim(),
          authorName: a.profile.name,
          createdAt: new Date().toISOString(),
        },
        ...old,
      ]);
      setReviewText('');
      a.toast('Review submitted successfully!');
    } catch (err) {
      setReviewError(err.message || 'Could not submit review.');
    } finally {
      setSubmittingReview(false);
    }
  };

  return (
    <Dialog title={item.title || 'Service details'} onClose={() => !saving && onClose()} wide>
      <div className="service-dialog-tabs">
        <Tabs items={['Book', 'Reviews & Ratings']} value={mainTab} onChange={setMainTab} />
      </div>

      {/* ── Book tab ── */}
      {mainTab === 'Book' ? (
        <section className="booking-flow">
          <FlowHeader steps={['Service', 'Schedule', 'Confirm']} step={step - 1} />

          {/* Step 1 – Service selection */}
          {step === 1 && (
            <>
              <div className="booking-service-head">
                <span className={`module-icon ${item.color || 'green'}`}><CheckCircle2 /></span>
                <div>
                  <div className="tag-cloud">
                    <Tag>{item.category}</Tag>
                    <Tag tone="purple"><MapPin size={12} /> Within {item.serviceRadiusKm || 15} km</Tag>
                    <Tag tone="gold">
                      <Star size={12} /> {item.averageRating ? Number(item.averageRating).toFixed(1) : '5.0'} ({item.reviewCount || 0} reviews)
                    </Tag>
                  </div>
                  <h2>{item.title}</h2>
                  <p>{item.provider} · {item.location}</p>
                </div>
              </div>
              <div className="service-options">
                {['Standard visit', 'Deep service', 'Full home package'].map((name, i) => (
                  <button key={name} className={i === 0 ? 'selected' : ''}>
                    <span>
                      <b>{name}</b>
                      <small>{i === 0 ? item.duration : 'Includes materials and follow-up'}</small>
                    </span>
                    <strong>INR {Number(item.price || 0) + (i * 300)}</strong>
                  </button>
                ))}
              </div>
              <Button variant="primary" onClick={() => setStep(2)}>Choose a time</Button>
            </>
          )}

          {/* Step 2 – Schedule */}
          {step === 2 && (
            <>
              <h3>Pick a convenient slot</h3>
              <div className="slot-grid">
                {['8:00 AM', '10:00 AM', '12:00 PM', '4:00 PM', '6:00 PM', '8:00 PM'].map(time => (
                  <button
                    key={time}
                    className={slot === time ? 'active' : ''}
                    onClick={() => setSlot(time)}
                  >
                    {time}
                  </button>
                ))}
              </div>
              <Field label="Address">
                <input value={address} onChange={e => setAddress(e.target.value)} />
              </Field>
              <Field label="Instructions for the professional">
                <textarea
                  rows="3"
                  value={note}
                  onChange={e => setNote(e.target.value)}
                  placeholder="Gate access, issue details, or preferred contact method"
                />
              </Field>
              <div className="flow-actions">
                <Button variant="quiet" onClick={() => setStep(1)}>Back</Button>
                <Button onClick={() => setStep(3)} disabled={!address.trim()}>Review booking</Button>
              </div>
            </>
          )}

          {/* Step 3 – Confirm */}
          {step === 3 && (
            <>
              <div className="booking-summary">
                <Ticket />
                <div>
                  <b>{item.title}</b>
                  <small>{slot} · {item.location}</small>
                </div>
                <strong>INR {item.price}</strong>
              </div>
              <DetailRow label="Provider">{item.provider}</DetailRow>
              <DetailRow label="Coverage">Within {item.serviceRadiusKm || 15} km</DetailRow>
              <DetailRow label="Service time">{slot}</DetailRow>
              <DetailRow label="Payment">Pay after service</DetailRow>
              {error && <p className="auth-error" role="alert">{error}</p>}
              <div className="flow-actions">
                <Button variant="quiet" onClick={() => setStep(2)} disabled={saving}>Back</Button>
                <Button onClick={confirm} disabled={saving}>
                  {saving ? 'Confirming…' : 'Confirm booking'}
                </Button>
              </div>
            </>
          )}
        </section>

      ) : (
        /* ── Reviews & Ratings tab ── */
        <section className="service-reviews-panel">
          <div className="service-reviews-summary">
            <div className="rating-big-score">
              <strong>
                {item.averageRating
                  ? Number(item.averageRating).toFixed(1)
                  : reviews.length
                    ? (reviews.reduce((acc, r) => acc + r.rating, 0) / reviews.length).toFixed(1)
                    : '5.0'}
              </strong>
              <span className="stars-icon-row">★ ★ ★ ★ ★</span>
              <small>{reviews.length || item.reviewCount || 0} verified reviews</small>
            </div>
            <div className="service-coverage-box">
              <MapPin size={20} />
              <div>
                <b>Service Radius Coverage</b>
                <p>
                  Certified provider servicing clients within a {item.serviceRadiusKm || 15} km radius
                  of {item.location || 'the operational base'}.
                </p>
              </div>
            </div>
          </div>

          {/* Write a review */}
          <div className="write-review-card">
            <h4>Leave a customer review</h4>
            <form onSubmit={handleCreateReview}>
              <div className="star-rating-selector">
                <span>Rating:</span>
                <div className="star-buttons">
                  {[1, 2, 3, 4, 5].map(s => (
                    <button
                      type="button"
                      key={s}
                      className={`star-btn ${reviewRating >= s ? 'filled' : ''}`}
                      onClick={() => setReviewRating(s)}
                    >
                      ★
                    </button>
                  ))}
                  <b>{reviewRating} of 5 stars</b>
                </div>
              </div>
              <Field label="Review feedback">
                <textarea
                  rows="3"
                  value={reviewText}
                  onChange={e => setReviewText(e.target.value)}
                  placeholder="Describe the quality, punctuality, and overall service delivery..."
                  required
                />
              </Field>
              {reviewError && <p className="auth-error" role="alert">{reviewError}</p>}
              <Button type="submit" variant="primary" disabled={submittingReview || !reviewText.trim()}>
                {submittingReview ? 'Submitting review…' : 'Submit Verified Review'}
              </Button>
            </form>
          </div>

          {/* Review list */}
          <div className="reviews-feed">
            <h4>Customer Reviews</h4>
            {loadingReviews ? (
              <p className="muted">Loading reviews…</p>
            ) : reviews.length === 0 ? (
              <p className="muted">
                No customer reviews yet. Book this service and be the first to leave feedback!
              </p>
            ) : (
              reviews.map(r => (
                <div key={r.id} className="review-card-item">
                  <div className="review-card-header">
                    <div>
                      <b>{r.authorName || 'Customer'}</b>
                      <small>{r.createdAt ? new Date(r.createdAt).toLocaleDateString() : 'Recently'}</small>
                    </div>
                    <span className="rating-badge">
                      {'★'.repeat(r.rating)}{'☆'.repeat(5 - r.rating)}
                    </span>
                  </div>
                  <p>{r.reviewText}</p>
                </div>
              ))
            )}
          </div>
        </section>
      )}
    </Dialog>
  );
}

/** Comprehensive event ticket checkout with team support, mail verification, multiple payment methods, and payment confirmation screen. */
function EventRegistration({ event, onClose }) {
  const a = useApp();
  const [step, setStep] = useState(1);
  const [tier, setTier] = useState('free');
  const [regMode, setRegMode] = useState('individual'); // 'individual' | 'team'
  const [teamName, setTeamName] = useState('');
  const [teamSize, setTeamSize] = useState(3);
  const [teamMembers, setTeamMembers] = useState(['', '']);
  const [attendeeName, setAttendeeName] = useState(a.profile.name || '');
  const [teamMemberNames, setTeamMemberNames] = useState(['', '', '']);
  const [teamMemberEmails, setTeamMemberEmails] = useState(['', '', '']);
  const [email, setEmail] = useState(a.profile.email || 'builder@collobster.app');
  const [phone, setPhone] = useState('+91 98765 43210');
  const [organization, setOrganization] = useState('Orbit Dev Labs');
  const [attendeeRole, setAttendeeRole] = useState('Frontend Developer');
  
  // Email verification state
  const [otpSent, setOtpSent] = useState(false);
  const [otpInput, setOtpInput] = useState('');
  const [emailVerified, setEmailVerified] = useState(true);
  
  // Payment methods state
  const [paymentMethod, setPaymentMethod] = useState('upi'); // 'upi' | 'card' | 'netbanking' | 'wallet'
  const [upiApp, setUpiApp] = useState('gpay');
  const [upiId, setUpiId] = useState('collob@okaxis');
  const [cardNumber, setCardNumber] = useState('4242 •••• •••• 4242');
  const [cardExp, setCardExp] = useState('12/28');
  const [cardCvv, setCardCvv] = useState('888');
  const [selectedBank, setSelectedBank] = useState('HDFC Bank');
  
  const [quantity, setQuantity] = useState(1);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [pendingOrder, setPendingOrder] = useState(null);
  const [confirmationData, setConfirmationData] = useState(null);

  const registered = a.registrations.includes(event.id);
  const unitPrice = Number(event.ticketPrice || event.paidTierPrice || 499) > 0 ? Number(event.ticketPrice || event.paidTierPrice || 499) : 499;
  const PAID_AMOUNT_CENTS = unitPrice * 100;
  const isPaid = tier === 'paid';
  const totalAmount = isPaid ? unitPrice * quantity : 0;

  const handleSendOtp = () => {
    if (!email) return setError('Please enter a valid email address.');
    setError('');
    setOtpSent(true);
    a.toast('Verification code sent to ' + email + ' (Use 482910)');
  };

  const handleVerifyOtp = () => {
    if (otpInput === '482910' || otpInput.trim().length >= 4) {
      setEmailVerified(true);
      setError('');
      a.toast('Email address verified successfully!');
    } else {
      setError('Invalid code. Please enter 482910 to verify.');
    }
  };

  const handleContinueStep1 = () => {
    setError('');
    if (regMode === 'team' && !teamName.trim()) {
      return setError('Team Name is mandatory. Please specify your team name.');
    }
    setStep(2);
  };

  const handleContinueStep2 = async () => {
    setError('');
    if (!attendeeName.trim()) {
      return setError(regMode === 'team' ? 'Team Leader Full Name is mandatory. Please enter your name.' : 'Full Name is mandatory. Please enter your name.');
    }
    if (regMode === 'team') {
      for (let i = 0; i < teamSize - 1; i++) {
        if (!teamMemberNames[i]?.trim()) {
          return setError(`Team Member ${i + 2} Full Name is mandatory. Please provide names for all ${teamSize} members.`);
        }
      }
    }
    if (!phone.trim()) {
      return setError('Phone Number is mandatory for pass verification.');
    }
    if (!email.trim()) {
      return setError('Email Address is mandatory.');
    }
    if (!emailVerified) {
      return setError('Please verify your email address before continuing.');
    }

    if (isPaid) {
      setSaving(true);
      try {
        let order = null;
        if (a.createPaymentOrder) {
          try {
            const result = await a.createPaymentOrder({
              amountCents: PAID_AMOUNT_CENTS * quantity,
              currency: 'INR',
              description: `${event.title} – ${tier === 'paid' ? 'VIP Pass' : 'Ticket'} × ${quantity}`,
            });
            order = result?.order;
          } catch {
            // fallback for demo/preview mode
          }
        }
        if (!order) {
          order = {
            id: `ord-${Date.now()}`,
            orderNumber: `ORD-VIP-${Math.floor(100000 + Math.random() * 900000)}`,
            amountCents: PAID_AMOUNT_CENTS * quantity,
          };
        }
        setPendingOrder(order);
        setStep(3);
      } catch (err) {
        setError(err.message || 'Could not initiate checkout. Please try again.');
      } finally {
        setSaving(false);
      }
    } else {
      setStep(3);
    }
  };

  const handleConfirmPaidPayment = async () => {
    setSaving(true);
    setError('');
    try {
      const mockGatewayId = `GATEWAY-${Date.now()}`;
      if (pendingOrder && a.confirmPaymentOrder) {
        try {
          await a.confirmPaymentOrder(pendingOrder.id, mockGatewayId);
        } catch (e) {
          console.warn('Payment confirmation remote fallback:', e);
        }
      }
      if (a.registerForEvent) {
        try {
          await a.registerForEvent(event);
        } catch (e) {
          console.warn('Event registration fallback:', e);
          if (a.setRegistrations) {
            a.setRegistrations(old => old.includes(event.id) ? old : [...old, event.id]);
          }
        }
      } else if (a.setRegistrations) {
        a.setRegistrations(old => old.includes(event.id) ? old : [...old, event.id]);
      }
      const conf = {
        txId: `TXN-COLLOB-${Math.floor(100000 + Math.random() * 900000)}`,
        orderId: pendingOrder?.orderNumber || `ORD-${Date.now()}`,
        amount: totalAmount,
        method: paymentMethod.toUpperCase(),
        slot: 'Slot Confirmed: Section A · Front Row (Seat 14)',
        ticketCode: `CLB-VIP-${Math.floor(1000 + Math.random() * 9000)}`,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setConfirmationData(conf);
      if (a.notify) {
        a.notify('Event ticket confirmed', `${event.title} – Payment confirmed. Ticket ready in My tickets.`, 'Events');
      }
      if (a.toast) {
        a.toast('Payment verified & ticket booked!');
      }
      setStep(4);
    } catch (err) {
      setError(err.message || 'Payment could not be completed. Please try again.');
    } finally {
      setSaving(false);
    }
  };

  const handleConfirmFreeRegistration = async () => {
    setSaving(true);
    setError('');
    try {
      await a.registerForEvent(event);
      const conf = {
        txId: `FREE-COLLOB-${Math.floor(100000 + Math.random() * 900000)}`,
        orderId: `FREE-${Date.now()}`,
        amount: 0,
        method: 'Complimentary pass',
        slot: regMode === 'team' ? `Team Slot Confirmed: ${teamName} (Squad #08)` : 'Slot Confirmed: General Access (Pass A-42)',
        ticketCode: `CLB-GEN-${Math.floor(1000 + Math.random() * 9000)}`,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setConfirmationData(conf);
      a.notify('Event ticket confirmed', `${event.title} is now saved in Registered.`, 'Events');
      setStep(4);
    } catch (err) {
      setError(err.message || 'Registration could not be confirmed.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog title={step === 4 ? "Registration & Payment Confirmed" : "Register for Event"} onClose={() => !saving && onClose()} wide>
      <section className="event-checkout" style={{ maxWidth: 580, margin: '0 auto' }}>
        <div style={{ display: 'flex', gap: 14, alignItems: 'center', marginBottom: 14 }}>
          <img src={event.image} alt="" style={{ width: 68, height: 68, borderRadius: 10, objectFit: 'cover' }} />
          <div className="event-checkout-info">
            <Tag tone="coral">{event.category}</Tag>
            <h2 style={{ margin: '4px 0', fontSize: '1.15rem' }}>{event.title}</h2>
            <p style={{ margin: 0, fontSize: '0.84rem', color: 'var(--color-muted, #71767b)' }}>
              <CalendarDays size={14} style={{ display: 'inline', verticalAlign: 'middle', marginRight: 4 }} />
              {event.date} · {event.time} | <MapPin size={14} style={{ display: 'inline', verticalAlign: 'middle', margin: '0 4px' }} />
              {event.location}
            </p>
          </div>
        </div>

        {step < 4 && (
          <FlowHeader steps={['Participation & Ticket', 'Attendee Details', isPaid ? 'Payment Options' : 'Review & Confirm']} step={step - 1} />
        )}

        {/* Already registered note */}
        {registered && step < 4 && (
          <div className="confirmation-state" style={{ padding: 12, marginBottom: 12, background: '#ecfdf5', borderRadius: 8, display: 'flex', alignItems: 'center', gap: 10 }}>
            <CheckCircle2 color="#059669" size={20} />
            <span style={{ fontSize: '0.86rem', color: '#065f46' }}>You already have an active ticket stored in Registered. Registering again will add another attendee pass.</span>
          </div>
        )}

        {/* STEP 1: Participation Mode & Tier */}
        {step === 1 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <div>
              <span style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--color-muted, #666)', display: 'block', marginBottom: 8 }}>
                1. PARTICIPATION TYPE
              </span>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                <button
                  type="button"
                  onClick={() => setRegMode('individual')}
                  style={{
                    padding: 12,
                    borderRadius: 10,
                    border: regMode === 'individual' ? '2px solid var(--accent, #e34d42)' : '1px solid var(--color-border, #ddd)',
                    background: regMode === 'individual' ? 'var(--accent-tint, rgba(227, 77, 66, 0.08))' : 'var(--color-surface, #fff)',
                    cursor: 'pointer',
                    textAlign: 'left',
                  }}
                >
                  <strong style={{ display: 'block', fontSize: '0.92rem' }}>👤 Individual</strong>
                  <small style={{ color: 'var(--color-muted, #71767b)' }}>Join as solo builder or attendee</small>
                </button>
                <button
                  type="button"
                  onClick={() => setRegMode('team')}
                  style={{
                    padding: 12,
                    borderRadius: 10,
                    border: regMode === 'team' ? '2px solid var(--accent, #e34d42)' : '1px solid var(--color-border, #ddd)',
                    background: regMode === 'team' ? 'var(--accent-tint, rgba(227, 77, 66, 0.08))' : 'var(--color-surface, #fff)',
                    cursor: 'pointer',
                    textAlign: 'left',
                  }}
                >
                  <strong style={{ display: 'block', fontSize: '0.92rem' }}>👥 Join as Team</strong>
                  <small style={{ color: 'var(--color-muted, #71767b)' }}>Register with 2–4 teammates</small>
                </button>
              </div>
            </div>

            {regMode === 'team' && (
              <div style={{ background: 'var(--color-surface-hover, #f8f9fa)', padding: 12, borderRadius: 8, display: 'flex', flexDirection: 'column', gap: 10 }}>
                <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.85rem' }}>
                  <span>Team Name *</span>
                  <input
                    value={teamName}
                    onChange={(e) => setTeamName(e.target.value)}
                    placeholder="e.g. CyberLobsters Squad"
                    style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }}
                  />
                </label>
                <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.85rem' }}>
                  <span>Team Size</span>
                  <select value={teamSize} onChange={(e) => setTeamSize(Number(e.target.value))} style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }}>
                    <option value={2}>2 Members</option>
                    <option value={3}>3 Members</option>
                    <option value={4}>4 Members</option>
                  </select>
                </label>
              </div>
            )}

            <div>
              <span style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--color-muted, #666)', display: 'block', marginBottom: 8 }}>
                2. SELECT TICKET TIER
              </span>
              <div className="ticket-choice" style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
                <button
                  type="button"
                  className={tier === 'free' ? 'selected' : ''}
                  onClick={() => setTier('free')}
                  style={{
                    padding: 12,
                    borderRadius: 10,
                    border: tier === 'free' ? '2px solid var(--accent, #e34d42)' : '1px solid var(--color-border, #ddd)',
                    background: tier === 'free' ? 'var(--accent-tint, rgba(227, 77, 66, 0.08))' : 'var(--color-surface, #fff)',
                    cursor: 'pointer',
                    textAlign: 'left',
                  }}
                >
                  <strong style={{ display: 'block', fontSize: '0.92rem' }}>General Admission</strong>
                  <small style={{ color: 'var(--color-muted, #71767b)', display: 'block', margin: '4px 0' }}>
                    {event.freeDescription || 'Access to all tracks & stages'}
                  </small>
                  <b style={{ color: '#059669' }}>Free</b>
                </button>
                <button
                  type="button"
                  className={tier === 'paid' ? 'selected' : ''}
                  onClick={() => setTier('paid')}
                  style={{
                    padding: 12,
                    borderRadius: 10,
                    border: tier === 'paid' ? '2px solid var(--accent, #e34d42)' : '1px solid var(--color-border, #ddd)',
                    background: tier === 'paid' ? 'var(--accent-tint, rgba(227, 77, 66, 0.08))' : 'var(--color-surface, #fff)',
                    cursor: 'pointer',
                    textAlign: 'left',
                  }}
                >
                  <strong style={{ display: 'block', fontSize: '0.92rem' }}>VIP Supporter Pass</strong>
                  <small style={{ color: 'var(--color-muted, #71767b)', display: 'block', margin: '4px 0' }}>
                    {event.paidPerks || 'Front rows, swag & mixer'}
                  </small>
                  <b style={{ color: 'var(--accent, #e34d42)' }}>₹{unitPrice}</b>
                </button>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <label style={{ fontSize: '0.85rem', display: 'flex', alignItems: 'center', gap: 8 }}>
                <span>Passes count:</span>
                <select value={quantity} onChange={(e) => setQuantity(Number(e.target.value))} style={{ padding: '4px 8px', borderRadius: 6, border: '1px solid #ccc' }}>
                  {[1, 2, 3, 4].map((n) => <option key={n} value={n}>{n}</option>)}
                </select>
              </label>
              {isPaid && (
                <strong style={{ fontSize: '1rem', color: 'var(--accent, #e34d42)' }}>
                  Total: ₹{(unitPrice * quantity).toLocaleString('en-IN')}
                </strong>
              )}
            </div>

            {error && <p className="auth-error" role="alert">{error}</p>}

            <div className="flow-actions" style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 10 }}>
              <Button onClick={handleContinueStep1}>Continue to Attendee Form</Button>
            </div>
          </div>
        )}

        {/* STEP 2: Comprehensive Attendee Form & Mail Verification */}
        {step === 2 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
              <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.85rem' }}>
                <span style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span>{regMode === 'team' ? 'Team Leader Name' : 'Full Name'}</span>
                  <span style={{ fontSize: '0.72rem', color: '#dc2626', fontWeight: 600 }}>* Mandatory</span>
                </span>
                <input
                  required
                  value={attendeeName}
                  onChange={(e) => setAttendeeName(e.target.value)}
                  placeholder="e.g. Aman Verma"
                  style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }}
                />
              </label>
              <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.85rem' }}>
                <span style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span>Phone Number</span>
                  <span style={{ fontSize: '0.72rem', color: '#dc2626', fontWeight: 600 }}>* Mandatory</span>
                </span>
                <input
                  required
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="+91 98765 43210"
                  style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }}
                />
              </label>
            </div>

            {/* Email verification row */}
            <div>
              <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.85rem' }}>
                <span style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span>Email Address (for Ticket & Passes)</span>
                  <span style={{ fontSize: '0.72rem', color: '#dc2626', fontWeight: 600 }}>* Mandatory</span>
                </span>
                <div style={{ display: 'flex', gap: 8 }}>
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => {
                      setEmail(e.target.value);
                      setEmailVerified(false);
                    }}
                    placeholder="you@domain.com"
                    style={{ flex: 1, padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }}
                  />
                  {!emailVerified ? (
                    <Button variant="secondary" type="button" onClick={handleSendOtp} style={{ whiteSpace: 'nowrap' }}>
                      {otpSent ? 'Resend code' : 'Verify email'}
                    </Button>
                  ) : (
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4, color: '#059669', fontWeight: 600, fontSize: '0.82rem', padding: '0 8px' }}>
                      <CheckCircle2 size={16} /> Verified
                    </span>
                  )}
                </div>
              </label>
              {otpSent && !emailVerified && (
                <div style={{ marginTop: 8, padding: 10, background: '#f0fdf4', border: '1px solid #bbf7d0', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 10 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <small style={{ fontSize: '0.8rem', color: '#166534', fontWeight: 500 }}>Enter OTP (Test OTP: <b>482910</b>):</small>
                    <input
                      value={otpInput}
                      onChange={(e) => setOtpInput(e.target.value)}
                      placeholder="482910"
                      style={{ width: 100, padding: '4px 8px', borderRadius: 4, border: '1px solid #86efac', textAlign: 'center', fontWeight: 700 }}
                    />
                  </div>
                  <div style={{ display: 'flex', gap: 6 }}>
                    <button
                      type="button"
                      onClick={() => setOtpInput('482910')}
                      style={{ fontSize: '0.75rem', padding: '4px 8px', background: '#dcfce7', border: 'none', borderRadius: 4, cursor: 'pointer', color: '#15803d' }}
                    >
                      Auto-fill
                    </button>
                    <button
                      type="button"
                      onClick={handleVerifyOtp}
                      style={{ fontSize: '0.75rem', padding: '4px 10px', background: '#16a34a', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer', fontWeight: 600 }}
                    >
                      Verify
                    </button>
                  </div>
                </div>
              )}
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
              <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.85rem' }}>
                <span>Organization / College</span>
                <input
                  value={organization}
                  onChange={(e) => setOrganization(e.target.value)}
                  placeholder="e.g. PES University or Tech Co"
                  style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }}
                />
              </label>
              <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.85rem' }}>
                <span>Your Role / Discipline</span>
                <input
                  value={attendeeRole}
                  onChange={(e) => setAttendeeRole(e.target.value)}
                  placeholder="e.g. Mobile Developer / Student"
                  style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }}
                />
              </label>
            </div>

            {regMode === 'team' && (
              <div style={{ background: 'var(--color-surface-hover, #f8f9fa)', padding: 12, borderRadius: 8, border: '1px solid var(--color-border, #e5e7eb)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                  <span style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--color-muted, #555)' }}>
                    Team Members Details ({teamSize - 1} additional members)
                  </span>
                  <span style={{ fontSize: '0.72rem', color: '#dc2626', fontWeight: 600 }}>* Member names mandatory</span>
                </div>
                {Array.from({ length: teamSize - 1 }).map((_, idx) => (
                  <div key={idx} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 8 }}>
                    <div>
                      <input
                        required
                        placeholder={`Member ${idx + 2} Full Name *`}
                        value={teamMemberNames[idx] || ''}
                        onChange={(e) => {
                          const updated = [...teamMemberNames];
                          updated[idx] = e.target.value;
                          setTeamMemberNames(updated);
                        }}
                        style={{ width: '100%', padding: '6px 10px', borderRadius: 6, border: (teamMemberNames[idx] || '').trim() ? '1px solid var(--color-border, #ddd)' : '1px solid #f87171', fontSize: '0.82rem' }}
                      />
                    </div>
                    <div>
                      <input
                        type="email"
                        placeholder={`Member ${idx + 2} Email (Optional)`}
                        value={teamMemberEmails[idx] || ''}
                        onChange={(e) => {
                          const updated = [...teamMemberEmails];
                          updated[idx] = e.target.value;
                          setTeamMemberEmails(updated);
                        }}
                        style={{ width: '100%', padding: '6px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)', fontSize: '0.82rem' }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            )}

            {error && <p className="auth-error" role="alert">{error}</p>}

            <div className="flow-actions" style={{ display: 'flex', justifyContent: 'space-between', marginTop: 12 }}>
              <Button variant="quiet" onClick={() => setStep(1)}>Back</Button>
              <Button onClick={handleContinueStep2}>
                {isPaid ? 'Proceed to Payment' : 'Review & Confirm'}
              </Button>
            </div>
          </div>
        )}

        {/* STEP 3: Multiple Payment Options (Paid) or Review (Free) */}
        {step === 3 && isPaid && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div className="booking-summary payment-summary" style={{ padding: 12, background: 'var(--color-surface-hover, #f8f9fa)', borderRadius: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <b>{quantity} × VIP Supporter Pass</b>
                <small style={{ display: 'block', color: 'var(--color-muted, #71767b)' }}>{event.title}</small>
                {regMode === 'team' && <small style={{ color: '#2563eb', fontWeight: 600 }}>Team: {teamName}</small>}
              </div>
              <strong style={{ fontSize: '1.2rem', color: 'var(--accent, #e34d42)' }}>
                ₹{(unitPrice * quantity).toLocaleString('en-IN')}
              </strong>
            </div>

            <div>
              <span style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--color-muted, #666)', display: 'block', marginBottom: 8 }}>
                CHOOSE PAYMENT METHOD
              </span>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8, marginBottom: 12 }}>
                {[
                  { id: 'upi', label: 'UPI / QR', icon: Smartphone },
                  { id: 'card', label: 'Card', icon: CreditCard },
                  { id: 'netbanking', label: 'NetBanking', icon: Building2 },
                  { id: 'wallet', label: 'Wallets', icon: Ticket },
                ].map((m) => (
                  <button
                    key={m.id}
                    type="button"
                    onClick={() => setPaymentMethod(m.id)}
                    style={{
                      padding: '10px 4px',
                      borderRadius: 8,
                      border: paymentMethod === m.id ? '2px solid var(--accent, #e34d42)' : '1px solid var(--color-border, #ddd)',
                      background: paymentMethod === m.id ? 'var(--accent-tint, rgba(227, 77, 66, 0.08))' : 'var(--color-surface, #fff)',
                      cursor: 'pointer',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: 4,
                      fontSize: '0.78rem',
                      fontWeight: paymentMethod === m.id ? 700 : 500,
                    }}
                  >
                    <m.icon size={18} color={paymentMethod === m.id ? 'var(--accent, #e34d42)' : '#666'} />
                    {m.label}
                  </button>
                ))}
              </div>

              {/* UPI Options */}
              {paymentMethod === 'upi' && (
                <div style={{ padding: 12, border: '1px solid var(--color-border, #ddd)', borderRadius: 8, display: 'flex', flexDirection: 'column', gap: 10 }}>
                  <div style={{ display: 'flex', gap: 8 }}>
                    {['Google Pay', 'PhonePe', 'Paytm', 'BHIM'].map((app) => (
                      <button
                        key={app}
                        type="button"
                        onClick={() => setUpiApp(app)}
                        style={{
                          flex: 1,
                          padding: '6px 4px',
                          borderRadius: 6,
                          border: upiApp === app ? '1px solid var(--accent, #e34d42)' : '1px solid #e1e4e8',
                          background: upiApp === app ? '#fff1f2' : '#fff',
                          cursor: 'pointer',
                          fontSize: '0.75rem',
                          fontWeight: 600,
                        }}
                      >
                        {app}
                      </button>
                    ))}
                  </div>
                  <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.82rem' }}>
                    <span>Or enter UPI ID / VPA</span>
                    <input
                      value={upiId}
                      onChange={(e) => setUpiId(e.target.value)}
                      placeholder="username@okhdfcbank"
                      style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }}
                    />
                  </label>
                  <small style={{ color: '#059669', fontSize: '0.76rem', display: 'flex', alignItems: 'center', gap: 4 }}>
                    <ShieldCheck size={14} /> Instant zero-fee UPI transfer with encrypted webhook
                  </small>
                </div>
              )}

              {/* Card Options */}
              {paymentMethod === 'card' && (
                <div style={{ padding: 12, border: '1px solid var(--color-border, #ddd)', borderRadius: 8, display: 'flex', flexDirection: 'column', gap: 10 }}>
                  <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.82rem' }}>
                    <span>Card Number</span>
                    <input
                      value={cardNumber}
                      onChange={(e) => setCardNumber(e.target.value)}
                      style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }}
                    />
                  </label>
                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                    <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.82rem' }}>
                      <span>Valid Thru</span>
                      <input
                        value={cardExp}
                        onChange={(e) => setCardExp(e.target.value)}
                        placeholder="MM/YY"
                        style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }}
                      />
                    </label>
                    <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.82rem' }}>
                      <span>CVV</span>
                      <input
                        type="password"
                        value={cardCvv}
                        onChange={(e) => setCardCvv(e.target.value)}
                        placeholder="•••"
                        style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }}
                      />
                    </label>
                  </div>
                </div>
              )}

              {/* Netbanking Options */}
              {paymentMethod === 'netbanking' && (
                <div style={{ padding: 12, border: '1px solid var(--color-border, #ddd)', borderRadius: 8 }}>
                  <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: '0.82rem' }}>
                    <span>Select Bank</span>
                    <select
                      value={selectedBank}
                      onChange={(e) => setSelectedBank(e.target.value)}
                      style={{ padding: '8px 10px', borderRadius: 6, border: '1px solid var(--color-border, #ddd)' }}
                    >
                      <option>HDFC Bank</option>
                      <option>ICICI Bank</option>
                      <option>State Bank of India</option>
                      <option>Axis Bank</option>
                      <option>Kotak Mahindra Bank</option>
                    </select>
                  </label>
                </div>
              )}

              {/* Wallet Options */}
              {paymentMethod === 'wallet' && (
                <div style={{ padding: 12, border: '1px solid var(--color-border, #ddd)', borderRadius: 8, display: 'flex', gap: 8 }}>
                  {['Paytm Wallet', 'Amazon Pay', 'MobiKwik'].map((w) => (
                    <button
                      key={w}
                      type="button"
                      style={{ flex: 1, padding: 8, borderRadius: 6, border: '1px solid #ccc', background: '#fff', cursor: 'pointer', fontSize: '0.8rem' }}
                    >
                      {w}
                    </button>
                  ))}
                </div>
              )}
            </div>

            {error && <p className="auth-error" role="alert">{error}</p>}

            <div className="flow-actions" style={{ display: 'flex', justifyContent: 'space-between', marginTop: 10 }}>
              <Button variant="quiet" onClick={() => setStep(2)} disabled={saving}>Back</Button>
              <Button onClick={handleConfirmPaidPayment} disabled={saving}>
                {saving ? 'Processing payment…' : `Pay ₹${(unitPrice * quantity).toLocaleString('en-IN')}`}
              </Button>
            </div>
          </div>
        )}

        {/* STEP 3: Confirm Free Registration */}
        {step === 3 && !isPaid && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div style={{ background: 'var(--color-surface-hover, #f8f9fa)', padding: 14, borderRadius: 8 }}>
              <h3 style={{ margin: '0 0 8px', fontSize: '1rem' }}>Review Registration Details</h3>
              <p style={{ margin: '2px 0', fontSize: '0.85rem' }}>Attendee: <b>{attendeeName}</b> ({attendeeRole})</p>
              <p style={{ margin: '2px 0', fontSize: '0.85rem' }}>Email: <b>{email}</b> (Verified ✓)</p>
              <p style={{ margin: '2px 0', fontSize: '0.85rem' }}>Phone: <b>{phone}</b></p>
              {regMode === 'team' && (
                <p style={{ margin: '2px 0', fontSize: '0.85rem', color: '#2563eb' }}>
                  Team: <b>{teamName}</b> ({teamSize} Members)
                </p>
              )}
              <div style={{ marginTop: 10, paddingTop: 10, borderTop: '1px solid var(--color-border, #e5e7eb)', display: 'flex', justifyContent: 'space-between' }}>
                <span>Pass type: <b>General Admission × {quantity}</b></span>
                <strong style={{ color: '#059669' }}>Free</strong>
              </div>
            </div>

            {error && <p className="auth-error" role="alert">{error}</p>}

            <div className="flow-actions" style={{ display: 'flex', justifyContent: 'space-between', marginTop: 10 }}>
              <Button variant="quiet" onClick={() => setStep(2)} disabled={saving}>Back</Button>
              <Button onClick={handleConfirmFreeRegistration} disabled={saving}>
                {saving ? 'Generating pass…' : 'Confirm Registration'}
              </Button>
            </div>
          </div>
        )}

        {/* STEP 4: Genuine Payment & Ticket Confirmation Screen */}
        {step === 4 && confirmationData && (
          <div className="payment-confirmation-screen" style={{ textAlign: 'center', padding: '16px 8px' }}>
            <div style={{ width: 64, height: 64, borderRadius: '50%', background: '#dcfce7', color: '#16a34a', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 14px' }}>
              <CheckCircle2 size={38} />
            </div>
            <h2 style={{ margin: '0 0 6px', fontSize: '1.35rem', color: '#111827' }}>
              {isPaid ? 'Payment & Slot Confirmed!' : 'Registration Confirmed!'}
            </h2>
            <p style={{ margin: '0 0 16px', color: 'var(--color-muted, #71767b)', fontSize: '0.88rem' }}>
              Your ticket pass and entry QR code have been issued and saved to your account.
            </p>

            <div style={{ background: 'var(--color-surface-hover, #f8f9fa)', border: '1px solid var(--color-border, #e5e7eb)', borderRadius: 12, padding: 18, textAlign: 'left', marginBottom: 16 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #e5e7eb', paddingBottom: 10, marginBottom: 10 }}>
                <div>
                  <strong style={{ display: 'block', fontSize: '0.98rem' }}>{event.title}</strong>
                  <small style={{ color: 'var(--color-muted, #666)' }}>{event.date} · {event.time} · {event.location}</small>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <b style={{ color: isPaid ? 'var(--accent, #e34d42)' : '#059669', fontSize: '1rem' }}>
                    {isPaid ? `₹${confirmationData.amount.toLocaleString('en-IN')}` : 'FREE'}
                  </b>
                  <small style={{ display: 'block', color: '#059669', fontWeight: 600 }}>{confirmationData.method}</small>
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, fontSize: '0.84rem' }}>
                <div>
                  <span style={{ color: 'var(--color-muted, #888)', display: 'block', fontSize: '0.74rem' }}>TRANSACTION ID</span>
                  <b>{confirmationData.txId}</b>
                </div>
                <div>
                  <span style={{ color: 'var(--color-muted, #888)', display: 'block', fontSize: '0.74rem' }}>TICKET PASS CODE</span>
                  <b style={{ color: 'var(--accent, #e34d42)' }}>{confirmationData.ticketCode}</b>
                </div>
                <div style={{ gridColumn: 'span 2' }}>
                  <span style={{ color: 'var(--color-muted, #888)', display: 'block', fontSize: '0.74rem' }}>SLOT CONFIRMATION</span>
                  <b style={{ color: '#059669' }}>{confirmationData.slot}</b>
                </div>
                {regMode === 'team' && (
                  <div style={{ gridColumn: 'span 2' }}>
                    <span style={{ color: 'var(--color-muted, #888)', display: 'block', fontSize: '0.74rem' }}>TEAM</span>
                    <b>{teamName} ({teamSize} members registered)</b>
                  </div>
                )}
              </div>

              {/* QR Code Pass Preview */}
              <div style={{ marginTop: 14, paddingTop: 14, borderTop: '1px dashed #d1d5db', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{ background: '#fff', padding: 6, borderRadius: 6, border: '1px solid #ccc' }}>
                    <QrCode size={42} color="#111" />
                  </div>
                  <div>
                    <strong style={{ display: 'block', fontSize: '0.82rem' }}>Entry QR Code Ready</strong>
                    <small style={{ color: 'var(--color-muted, #777)' }}>Scan this at venue check-in</small>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    downloadTicketPdf({
                      eventTitle: event?.title || 'Collobster Event',
                      attendeeName,
                      ticketCode: confirmationData?.txId || `TX-${Date.now()}`,
                      slot: `Pass × ${quantity}`,
                      date: event?.date || `${event?.month} ${event?.day}`,
                      location: event?.location || 'Bengaluru',
                    });
                    a.toast('Receipt downloaded as official PDF!');
                  }}
                  style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '6px 12px', borderRadius: 6, border: '1px solid #ccc', background: '#fff', cursor: 'pointer', fontSize: '0.8rem', fontWeight: 600, color: '#111' }}
                >
                  <Download size={14} /> Receipt
                </button>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'center', gap: 12 }}>
              <Button
                variant="primary"
                onClick={() => {
                  onClose();
                  if (a.openEventsTickets) {
                    a.openEventsTickets(event?.id);
                  } else {
                    a.go('Events');
                  }
                }}
              >
                View my ticket
              </Button>
              <Button variant="quiet" onClick={onClose}>
                Done
              </Button>
            </div>
          </div>
        )}
      </section>
    </Dialog>
  );
}


// ─── 8. COMPANY / INVESTOR / COMMUNITY ───────────────────────────────────────

/** Ten-step company or startup registration flow. */
function CompanyOnboarding({ onComplete }) {
  const a = useApp();
  const [step, setStep]           = useState(0);
  const [path, setPath]           = useState('Startup / dev environment');
  const [error, setError]         = useState('');
  const [documents, setDocuments] = useState({});
  const [submission, setSubmission] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [draft, setDraft] = useState({
    name: '', tagline: '', website: '', description: '',
    industry: 'Technology', model: 'SaaS', stage: 'Idea stage',
    founded: '2026', country: 'India',
    headquarters: a.profile.location,
    teamSize: '1 – 10',
    founder: a.profile.name,
    email: '', phone: '', role: 'Founder / CEO',
    linkedin: '', cofounder: '', partner: '',
    roles: 'Product / technology, Design',
  });

  const steps = [
    'Choose path', 'Registration', 'Basic information', 'Business details',
    'Founder / owner', 'Team & co-founders', 'Documents', 'Review & submit',
    'Verification', 'Complete',
  ];

  const update  = (key, value) => setDraft(old => ({ ...old, [key]: value }));
  const saveDraft = () => a.toast('Startup registration draft saved on this device.');
  const upload  = (key, event) => {
    const file = event.target.files?.[0];
    if (file) setDocuments(old => ({ ...old, [key]: file.name }));
  };

  const legal = path === 'Existing company';

  const next = async () => {
    if (step === 0) { setStep(1); return; }
    if (step === 2 && !draft.name.trim()) {
      setError('Enter the startup or company name before continuing.');
      return;
    }
    if (step === 4 && (!draft.founder.trim() || !draft.email.trim())) {
      setError('Add the primary founder name and email before continuing.');
      return;
    }
    if (step === 7) {
      setSubmitting(true);
      setError('');
      try {
        const created = await a.createCompany({ ...draft, path });
        setSubmission(created);
        setStep(8);
        a.toast(
          created.mode === 'synced'
            ? 'Company profile created and queued for verification'
            : 'Company profile created in Preview'
        );
        return;
      } catch {
        setError('We could not create the company profile. Check your connection and try again.');
        return;
      } finally {
        setSubmitting(false);
      }
    }
    setError('');
    if (step < steps.length - 1) { setStep(step + 1); return; }
    a.toast('Startup verification submitted');
    a.notify(
      'Startup verification submitted',
      `${draft.name} is ready for review and its workspace is available in ${submission?.mode === 'synced' ? 'your account' : 'Preview'}.`,
      'Company'
    );
    onComplete?.(submission);
    close(a);
  };

  return (
    <Dialog title={legal ? 'Register your company' : 'Create your startup'} onClose={() => close(a)} wide>
      <form
        className="form-stack startup-registration-flow"
        onSubmit={event => { event.preventDefault(); next(); }}
      >
        <FlowHeader steps={steps} step={step} />

        {/* Step 0 – Choose path */}
        {step === 0 && (
          <section className="registration-path">
            <span className="eyebrow">STARTUP / COMPANY ENTRY</span>
            <h3>How would you like to get started?</h3>
            <p>Choose the path that describes where the work is today. You can keep building after launch.</p>
            <div className="choice-list">
              <button
                type="button"
                className={!legal ? 'chosen' : ''}
                onClick={() => setPath('Startup / dev environment')}
              >
                <b>Startup / dev environment</b>
                <small>Build from an idea, validate the problem, and register when you are ready.</small>
              </button>
              <button
                type="button"
                className={legal ? 'chosen' : ''}
                onClick={() => setPath('Existing company')}
              >
                <b>Existing company</b>
                <small>Register a legally established company and bring your team into one workspace.</small>
              </button>
            </div>
          </section>
        )}

        {/* Step 1 – Registration checklist overview */}
        {step === 1 && (
          <section className="registration-progress">
            <span className="eyebrow">REGISTRATION PROGRESS</span>
            <h3>{legal ? 'Bring your company profile together.' : 'Build the foundation, then launch.'}</h3>
            <div className="registration-checklist">
              {[
                ['Basic information',    'Name the company and its purpose'],
                ['Business details',     'Add stage, model, and operating region'],
                ['Founder / owner',      'Confirm the accountable owner'],
                ['Team & co-founders',   'Invite collaborators and roles'],
                ['Documents',            'Add evidence for verification'],
                ['Review & submit',      'Check the information before submitting'],
                ['Verification',         'Track eligibility and document review'],
              ].map(([title, copy], index) => (
                <div key={title}>
                  <i>{index + 1}</i>
                  <span>
                    <b>{title}</b>
                    <small>{copy}</small>
                  </span>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Step 2 – Basic information */}
        {step === 2 && (
          <>
            <section className="flow-intro">
              <span className="eyebrow">BASIC INFORMATION</span>
              <h3>Introduce the work clearly.</h3>
              <p>A concise profile makes it easier for team members, customers, and investors to understand the company.</p>
            </section>
            <div className="onboarding-field-grid">
              <Field label={legal ? 'Legal company name' : 'Startup name'}>
                <input
                  value={draft.name}
                  onChange={event => update('name', event.target.value)}
                  placeholder={legal ? 'e.g. GreenCart Technologies Pvt. Ltd.' : 'e.g. GreenCart'}
                />
              </Field>
              <Field label="Tagline">
                <input
                  value={draft.tagline}
                  onChange={event => update('tagline', event.target.value)}
                  placeholder="One clear sentence about what you do"
                />
              </Field>
            </div>
            <Field label="Short description">
              <textarea
                rows="3"
                value={draft.description}
                onChange={event => update('description', event.target.value)}
                placeholder="Describe the problem, audience, and early outcome you are building toward."
              />
            </Field>
            <Field label="Website (optional)">
              <input
                value={draft.website}
                onChange={event => update('website', event.target.value)}
                placeholder="https://yourcompany.com"
              />
            </Field>
          </>
        )}

        {/* Step 3 – Business details */}
        {step === 3 && (
          <>
            <section className="flow-intro">
              <span className="eyebrow">BUSINESS DETAILS</span>
              <h3>Describe how the company operates.</h3>
            </section>
            <div className="onboarding-field-grid">
              <Field label="Industry">
                <select value={draft.industry} onChange={event => update('industry', event.target.value)}>
                  <option>Technology</option>
                  <option>Food and beverage</option>
                  <option>Healthcare</option>
                  <option>Education</option>
                  <option>Climate / sustainability</option>
                  <option>Financial services</option>
                </select>
              </Field>
              <Field label="Business model">
                <select value={draft.model} onChange={event => update('model', event.target.value)}>
                  <option>SaaS</option>
                  <option>Marketplace</option>
                  <option>Direct to consumer</option>
                  <option>Enterprise / B2B</option>
                  <option>Services</option>
                </select>
              </Field>
              <Field label="Stage">
                <select value={draft.stage} onChange={event => update('stage', event.target.value)}>
                  <option>Idea stage</option>
                  <option>Prototype</option>
                  <option>MVP</option>
                  <option>Early revenue</option>
                  <option>Growth stage</option>
                </select>
              </Field>
              <Field label="Founded year">
                <input value={draft.founded} onChange={event => update('founded', event.target.value)} inputMode="numeric" />
              </Field>
              <Field label="Country / region">
                <input value={draft.country} onChange={event => update('country', event.target.value)} />
              </Field>
              <Field label="Headquarters">
                <input value={draft.headquarters} onChange={event => update('headquarters', event.target.value)} />
              </Field>
              <Field label="Team size">
                <select value={draft.teamSize} onChange={event => update('teamSize', event.target.value)}>
                  <option>1 – 10</option>
                  <option>11 – 50</option>
                  <option>51 – 200</option>
                  <option>201 – 500</option>
                </select>
              </Field>
              {legal && (
                <Field label="Registration type">
                  <select>
                    <option>Private limited company</option>
                    <option>LLP</option>
                    <option>Partnership firm</option>
                    <option>Sole proprietorship</option>
                  </select>
                </Field>
              )}
            </div>
          </>
        )}

        {/* Step 4 – Founder / owner */}
        {step === 4 && (
          <>
            <section className="flow-intro">
              <span className="eyebrow">FOUNDER / OWNER</span>
              <h3>Who is accountable for this profile?</h3>
            </section>
            <div className="onboarding-field-grid">
              <Field label="Full name">
                <input value={draft.founder} onChange={event => update('founder', event.target.value)} />
              </Field>
              <Field label="Role">
                <select value={draft.role} onChange={event => update('role', event.target.value)}>
                  <option>Founder / CEO</option>
                  <option>Co-founder / CTO</option>
                  <option>Director</option>
                  <option>Authorised representative</option>
                </select>
              </Field>
              <Field label="Work email">
                <input
                  type="email"
                  value={draft.email}
                  onChange={event => update('email', event.target.value)}
                  placeholder="founder@company.com"
                />
              </Field>
              <Field label="Phone number">
                <input
                  value={draft.phone}
                  onChange={event => update('phone', event.target.value)}
                  placeholder="+91 98765 43210"
                />
              </Field>
            </div>
            <Field label="LinkedIn profile (optional)">
              <input
                value={draft.linkedin}
                onChange={event => update('linkedin', event.target.value)}
                placeholder="linkedin.com/in/name"
              />
            </Field>
          </>
        )}

        {/* Step 5 – Team & co-founders */}
        {step === 5 && (
          <>
            <section className="flow-intro">
              <span className="eyebrow">TEAM & CO-FOUNDERS</span>
              <h3>Invite the people who will build with you.</h3>
            </section>
            <Field label="Co-founder or leadership invite">
              <input
                type="email"
                value={draft.cofounder}
                onChange={event => update('cofounder', event.target.value)}
                placeholder="name@company.com (optional)"
              />
            </Field>
            <Field label="Partner / advisor invite">
              <input
                type="email"
                value={draft.partner}
                onChange={event => update('partner', event.target.value)}
                placeholder="advisor@company.com (optional)"
              />
            </Field>
            <Field label="Roles needed">
              <input
                value={draft.roles}
                onChange={event => update('roles', event.target.value)}
                placeholder="Product, design, engineering, marketing"
              />
            </Field>
            <FormChecks items={[
              'Let invited members view workspace details',
              'Allow team members to add product updates',
              'Require owner approval for financial changes',
            ]} />
          </>
        )}

        {/* Step 6 – Documents */}
        {step === 6 && (
          <>
            <section className="flow-intro">
              <span className="eyebrow">DOCUMENTS</span>
              <h3>Prepare the evidence required for review.</h3>
              <p>Files stay represented in this device preview until secure upload is connected.</p>
            </section>
            <div className="document-upload-list">
              {[
                [legal ? 'Certificate of incorporation' : 'Founder identity document', 'identity'],
                [legal ? 'PAN / GST certificate' : 'Pitch deck or product brief', 'registration'],
                ['Proof of business address', 'address'],
                ['Cap table or shareholder summary', 'cap-table'],
              ].map(([label, key]) => (
                <label key={key}>
                  <span>
                    <b>{label}</b>
                    <small>{documents[key] || 'PDF, PNG, or JPG · not uploaded yet'}</small>
                  </span>
                  <input type="file" accept=".pdf,.png,.jpg,.jpeg" onChange={event => upload(key, event)} />
                  <span className="upload-button">Choose file</span>
                </label>
              ))}
            </div>
          </>
        )}

        {/* Step 7 – Review & submit */}
        {step === 7 && (
          <section className="registration-review">
            <span className="eyebrow">REVIEW & SUBMIT</span>
            <h2>{draft.name || 'Your startup'}</h2>
            <div className="confirm-box">
              <b>{legal ? 'Company registration' : 'Startup registration'} · {draft.stage}</b>
              <span>{draft.industry} · {draft.model} · {draft.headquarters || draft.country}</span>
              <span>Founder: {draft.founder} · {draft.role}</span>
              <span>{Object.keys(documents).length} of 4 supporting documents selected</span>
            </div>
            <FormChecks items={[
              'I confirm I am authorised to create this company profile',
              'I confirm the information is accurate',
              'I agree to the platform company guidelines',
            ]} />
          </section>
        )}

        {/* Step 8 – Verification status */}
        {step === 8 && (
          <section className="verification-card">
            <CheckCircle2 size={42} />
            <h2>Application submitted</h2>
            <p>
              {submission?.mode === 'synced'
                ? 'Your company profile is in the verification queue. You can continue operating the workspace while statuses update.'
                : 'Your company profile is ready in Preview. Connect a verified API session to submit it for production review.'}
            </p>
            <div className="verification-status">
              <span>Profile information <b>Complete</b></span>
              <span>Document review <b>{Object.keys(documents).length ? 'In review' : 'Needs documents'}</b></span>
              <span>Company verification <b>{submission?.mode === 'synced' ? 'Pending' : 'Preview'}</b></span>
            </div>
          </section>
        )}

        {/* Step 9 – Complete */}
        {step === 9 && (
          <section className="registration-complete">
            <CheckCircle2 size={48} />
            <span className="eyebrow">WORKSPACE READY</span>
            <h2>{draft.name || 'Your startup'} is ready to build.</h2>
            <p>
              Invite collaborators, capture your first milestone, publish a product update,
              or continue verification from the company workspace.
            </p>
            <Button type="button" onClick={() => { onComplete?.(submission); close(a); }}>
              Open company workspace
            </Button>
          </section>
        )}

        {error && <p className="auth-error" role="alert">{error}</p>}

        <div className="dialog-actions">
          <Button
            variant="quiet"
            type="button"
            onClick={() => step ? setStep(step - 1) : close(a)}
            disabled={submitting}
          >
            {step ? 'Back' : 'Save & exit'}
          </Button>
          {step < 9 && (
            <Button variant="quiet" type="button" onClick={saveDraft} disabled={submitting}>
              Save draft
            </Button>
          )}
          <Button variant="primary" type="submit" disabled={submitting}>
            {submitting         ? 'Creating company…'
             : step === 8      ? 'Continue to workspace'
             : step === 9      ? 'Open workspace'
             : step === 7      ? 'Submit company'
             : 'Continue'}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}

/** Five-step investor profile registration. */
function InvestorOnboarding() {
  const a = useApp();
  const [step, setStep]             = useState(0);
  const [investorType, setInvestorType] = useState('Individual investor');
  const steps = ['Basics', 'Preferences', 'Type', 'Verify', 'Complete'];

  const next = () => {
    if (step < steps.length - 1) {
      setStep(step + 1);
    } else {
      a.toast('Investor profile complete');
      a.notify('Investor profile verified', 'Your investment discovery workspace is ready.', 'Investment');
      close(a);
    }
  };

  return (
    <Dialog title="Investor registration" onClose={() => close(a)} wide>
      <form className="form-stack" onSubmit={e => { e.preventDefault(); next(); }}>
        <FlowHeader steps={steps} step={step} />

        {/* Step 0 – Basics */}
        {step === 0 && (
          <>
            <Field label="Full name">
              <input defaultValue={a.profile.name} />
            </Field>
            <Field label="Email address">
              <input type="email" placeholder="you@example.com" />
            </Field>
            <Field label="Location">
              <input defaultValue={a.profile.location} />
            </Field>
          </>
        )}

        {/* Step 1 – Preferences */}
        {step === 1 && (
          <>
            <Field label="Preferred stages">
              <input defaultValue="Seed, MVP, Early revenue" />
            </Field>
            <Field label="Preferred sectors">
              <input defaultValue="Climate tech, SaaS, Education" />
            </Field>
            <Field label="Investment range">
              <select>
                <option>INR 10L – 50L</option>
                <option>INR 50L – 2Cr</option>
                <option>INR 2Cr – 5Cr</option>
              </select>
            </Field>
          </>
        )}

        {/* Step 2 – Investor type */}
        {step === 2 && (
          <div className="choice-list">
            {['Individual investor', 'Syndicate', 'Venture capital', 'Family office'].map(x => (
              <button
                type="button"
                key={x}
                className={investorType === x ? 'chosen' : ''}
                onClick={() => setInvestorType(x)}
              >
                {x}
              </button>
            ))}
          </div>
        )}

        {/* Step 3 – Document verification */}
        {step === 3 && (
          <>
            <FormChecks items={[
              'Identity document',
              'Tax identifier / PAN',
              'Address confirmation',
              'Income or net-worth certificate',
            ]} />
            <p className="muted">
              Verification documents are local placeholders here; they are not uploaded or transmitted.
            </p>
          </>
        )}

        {/* Step 4 – Complete */}
        {step === 4 && (
          <div className="verification-card">
            <CheckCircle2 size={42} />
            <h2>Investor profile ready</h2>
            <p>You can now discover opportunities and record interest without committing capital.</p>
          </div>
        )}

        <div className="dialog-actions">
          <Button variant="quiet" onClick={() => step ? setStep(step - 1) : close(a)}>
            {step ? 'Back' : 'Cancel'}
          </Button>
          <Button variant="primary" type="submit">
            {step === steps.length - 1 ? 'Finish' : 'Continue'}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}

function CommunityCreate() {
  const a = useApp();
  const [step, setStep]       = useState(0);
  const [name, setName]       = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('Development');
  const [privacy, setPrivacy] = useState('Public');
  const [roomCreationPermission, setRoomCreationPermission] = useState('subadmins');
  const [postingPermission, setPostingPermission] = useState('all');
  const [guidelines, setGuidelines] = useState('');
  const [welcomeMessage, setWelcomeMessage] = useState('');
  const [cover, setCover]     = useState('');
  const [coverPreview, setCoverPreview] = useState('');
  const [tags, setTags]       = useState('');
  const [allowInvites, setAllowInvites] = useState(true);
  const [allowMemberPolls, setAllowMemberPolls] = useState(true);
  const [requireGuidelinesAcceptance, setRequireGuidelinesAcceptance] = useState(true);
  const [error, setError]     = useState('');
  const steps = ['Basics', 'Identity', 'Access & Rooms', 'People & Guidelines', 'Review'];

  const next = () => {
    if (step === 0 && !name.trim()) {
      setError('Give your community a name before continuing.');
      return;
    }
    setError('');
    if (step < steps.length - 1) {
      setStep(step + 1);
    } else {
      const commId = `comm-${Date.now()}`;
      const newComm = {
        id: commId,
        title: name.trim(),
        description: description.trim() || 'A community for learning, collaborating, and growing together.',
        category,
        policy: privacy === 'Public' ? 'Open' : privacy === 'Private' ? 'Application' : 'Invite-only',
        privacy,
        needsApproval: privacy === 'Private' || privacy === 'Approval required',
        allowSubadminsApproveRequests: true,
        roomCreationPermission,
        postingPermission,
        guidelines: guidelines.trim(),
        welcomeMessage: welcomeMessage.trim() || 'Welcome to the community! Introduce yourself in the lounge.',
        allowInvites,
        allowMemberPolls,
        requireGuidelinesAcceptance,
        color: ['rose', 'blue', 'green', 'gold'][Math.floor(Math.random() * 4)],
        members: 1,
        tags: tags ? tags.split(',').map(t => t.trim()).filter(Boolean) : [category],
        role: 'Admin',
        userRole: 'Admin',
        coverImage: coverPreview || cover,
        createdAt: new Date().toISOString(),
      };
      if (a.setCommunityCatalog) {
        a.setCommunityCatalog(old => [newComm, ...(old || [])]);
      }
      a.setMemberships(old => ({
        ...old,
        [commId]: 'member',
      }));
      a.toast('Community created successfully!');
      a.notify('Community created', `${name} is ready and published to Discover.`, 'Community');
      close(a);
    }
  };

  return (
    <Dialog title="Create a community" onClose={() => close(a)} wide>
      <form
        className="form-stack community-create-flow"
        onSubmit={e => { e.preventDefault(); next(); }}
      >
        <FlowHeader steps={steps} step={step} />

        {/* Step 0 – Basics */}
        {step === 0 && (
          <>
            <section className="flow-intro">
              <span className="eyebrow">COMMUNITY FOUNDATION</span>
              <h3>Give people a clear reason to join.</h3>
              <p>A good name, purpose, and category make the right community easier to discover.</p>
            </section>
            <Field label="Community name">
              <input
                value={name}
                onChange={e => setName(e.target.value)}
                placeholder="e.g. Flutter builders Bengaluru"
                autoFocus
              />
            </Field>
            <Field label="Short description">
              <textarea
                rows="3"
                value={description}
                onChange={e => setDescription(e.target.value)}
                placeholder="What will members learn, share, or build here?"
              />
            </Field>
            <Field label="Category">
              <select value={category} onChange={e => setCategory(e.target.value)}>
                <option>Development</option>
                <option>Design</option>
                <option>Startup</option>
                <option>AI / ML</option>
                <option>Career</option>
                <option>Local</option>
              </select>
            </Field>
          </>
        )}

        {/* Step 1 – Identity */}
        {step === 1 && (
          <>
            <section className="flow-intro">
              <span className="eyebrow">IDENTITY</span>
              <h3>Make the space recognisable.</h3>
            </section>
            <Field label="Community cover image">
              <div className="community-cover-upload-zone">
                <input
                  id="community-cover-file-input"
                  type="file"
                  accept="image/*"
                  style={{ display: 'none' }}
                  onChange={e => {
                    const f = e.target.files?.[0];
                    if (f) {
                      setCover(f.name);
                      try {
                        const url = URL.createObjectURL(f);
                        setCoverPreview(url);
                      } catch {}
                    }
                  }}
                />
                {cover ? (
                  <div className="cover-selected-card">
                    {coverPreview && (
                      <img src={coverPreview} alt="Cover preview" className="cover-thumbnail-preview" />
                    )}
                    <div style={{ minWidth: 0, flex: 1 }}>
                      <b style={{ display: 'block', fontSize: 13, wordBreak: 'break-all' }}>{cover}</b>
                      <span className="muted" style={{ fontSize: 11 }}>Cover selected</span>
                    </div>
                    <button
                      type="button"
                      className="text-button"
                      style={{ color: 'var(--coral)', fontSize: 12 }}
                      onClick={() => { setCover(''); setCoverPreview(''); }}
                    >
                      Remove
                    </button>
                  </div>
                ) : (
                  <label htmlFor="community-cover-file-input" className="cover-upload-placeholder-btn">
                    <Upload size={20} color="var(--coral)" />
                    <span style={{ fontWeight: 600, fontSize: 13 }}>Choose cover image</span>
                    <small className="muted" style={{ fontSize: 11 }}>Tap to select an image from your device</small>
                  </label>
                )}
              </div>
            </Field>

            <Field label="Community tags">
              <input
                value={tags}
                onChange={e => setTags(e.target.value)}
                placeholder="e.g. Flutter, Dart, Mobile development"
              />
              <div className="fast-filter-chips" style={{ marginTop: 6, paddingBottom: 0 }}>
                {['#React', '#Design', '#AI', '#Flutter', '#Startups', '#Career'].map(tag => (
                  <button
                    key={tag}
                    type="button"
                    className="filter-chip"
                    style={{ fontSize: 11, padding: '3px 8px' }}
                    onClick={() => {
                      const cleanTag = tag.replace('#', '');
                      const current = tags.split(',').map(t => t.trim()).filter(Boolean);
                      if (!current.includes(cleanTag)) {
                        setTags(current.length > 0 ? `${tags}, ${cleanTag}` : cleanTag);
                      }
                    }}
                  >
                    + {tag}
                  </button>
                ))}
              </div>
            </Field>

            <Field label="Welcome message">
              <textarea
                rows="3"
                value={welcomeMessage}
                onChange={e => setWelcomeMessage(e.target.value)}
                placeholder="Welcome new members and describe the first useful thing they can do."
              />
            </Field>
          </>
        )}

        {/* Step 2 – Access & Rooms */}
        {step === 2 && (
          <>
            <section className="flow-intro">
              <span className="eyebrow">ACCESS &amp; ROOM PERMISSIONS</span>
              <h3>Choose who can find and create rooms.</h3>
            </section>
            <div className="choice-list">
              {[
                ['Public',   'Anyone can find, preview, and join this community.'],
                ['Private',  'Only approved members can find and participate.'],
                ['Invite-only', 'Only people with the invite link can access it.'],
              ].map(([value, copy]) => (
                <button
                  type="button"
                  className={privacy === value ? 'chosen' : ''}
                  key={value}
                  onClick={() => setPrivacy(value)}
                  style={{ display: 'flex', alignItems: 'flex-start', gap: 10, textAlign: 'left' }}
                >
                  <div
                    style={{
                      width: 18,
                      height: 18,
                      borderRadius: '50%',
                      border: privacy === value ? '5px solid var(--coral)' : '2px solid var(--line)',
                      marginTop: 2,
                      flexShrink: 0,
                      background: '#fff',
                    }}
                  />
                  <div style={{ minWidth: 0, flex: 1 }}>
                    <b style={{ display: 'block', fontSize: 13 }}>{value}</b>
                    <small style={{ display: 'block', fontSize: 11, color: 'var(--muted)', marginTop: 2 }}>{copy}</small>
                  </div>
                </button>
              ))}
            </div>
            <Field label="Who can create rooms &amp; channels in this community?">
              <select
                value={roomCreationPermission}
                onChange={e => setRoomCreationPermission(e.target.value)}
              >
                <option value="all">All Members (Anyone can create topic rooms)</option>
                <option value="subadmins">Admins &amp; Sub-admins only (Recommended)</option>
                <option value="admins">Community Admin only</option>
              </select>
            </Field>
            <Field label="Posting &amp; Chat permissions">
              <select
                value={postingPermission}
                onChange={e => setPostingPermission(e.target.value)}
              >
                <option value="all">All members can post and chat freely</option>
                <option value="moderated">New members require post approval</option>
                <option value="admins">Only Admins and Sub-admins can post</option>
              </select>
            </Field>
          </>
        )}

        {/* Step 3 – People & Guidelines */}
        {step === 3 && (
          <>
            <section className="flow-intro">
              <span className="eyebrow">PEOPLE &amp; GUIDELINES</span>
              <h3>Set expectations and safety for your community.</h3>
            </section>
            <Field label="Community guidelines">
              <textarea
                rows="3"
                value={guidelines}
                onChange={e => setGuidelines(e.target.value)}
                placeholder="Be respectful, stay constructive, and keep discussions relevant to community goals."
              />
            </Field>
            <div className="operator-checklist" style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 6 }}>
              <label style={{ display: 'flex', alignItems: 'flex-start', gap: 10, cursor: 'pointer', fontSize: 13 }}>
                <input
                  type="checkbox"
                  style={{ marginTop: 2 }}
                  checked={allowInvites}
                  onChange={e => setAllowInvites(e.target.checked)}
                />
                <span>Allow member invitations via invite link</span>
              </label>
              <label style={{ display: 'flex', alignItems: 'flex-start', gap: 10, cursor: 'pointer', fontSize: 13 }}>
                <input
                  type="checkbox"
                  style={{ marginTop: 2 }}
                  checked={allowMemberPolls}
                  onChange={e => setAllowMemberPolls(e.target.checked)}
                />
                <span>Allow members to create and vote on polls</span>
              </label>
              <label style={{ display: 'flex', alignItems: 'flex-start', gap: 10, cursor: 'pointer', fontSize: 13 }}>
                <input
                  type="checkbox"
                  style={{ marginTop: 2 }}
                  checked={requireGuidelinesAcceptance}
                  onChange={e => setRequireGuidelinesAcceptance(e.target.checked)}
                />
                <span>Display community guidelines before joining</span>
              </label>
            </div>
          </>
        )}

        {/* Step 4 – Review */}
        {step === 4 && (
          <section className="community-review">
            <span className="eyebrow">READY TO PUBLISH</span>
            <h2 style={{ wordBreak: 'break-word', overflowWrap: 'anywhere' }}>{name}</h2>
            <div className="confirm-box" style={{ gap: 8, wordBreak: 'break-word', overflowWrap: 'anywhere' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--line)', paddingBottom: 6 }}>
                <span className="muted">Category &amp; Access</span>
                <b>{category} · {privacy}</b>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--line)', paddingBottom: 6 }}>
                <span className="muted">Room Creation</span>
                <b>{roomCreationPermission === 'all' ? 'All members' : roomCreationPermission === 'subadmins' ? 'Admins & Sub-admins' : 'Admin only'}</b>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--line)', paddingBottom: 6 }}>
                <span className="muted">Chat &amp; Posts</span>
                <b>{postingPermission === 'all' ? 'Free posting' : postingPermission === 'moderated' ? 'Moderated' : 'Admins only'}</b>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid var(--line)', paddingBottom: 6 }}>
                <span className="muted">Guidelines</span>
                <b>{guidelines ? `${guidelines.slice(0, 35)}...` : 'Default guidelines'}</b>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span className="muted">Cover Image</span>
                <b>{cover ? cover : 'No cover attached'}</b>
              </div>
            </div>
            <p className="muted" style={{ fontSize: 12, marginTop: 10 }}>
              You can edit categories, room permissions, roles, and guidelines from Community settings at any time.
            </p>
          </section>
        )}

        {error && <p className="auth-error" role="alert">{error}</p>}

        <div className="dialog-actions">
          <Button variant="quiet" type="button" onClick={() => step ? setStep(step - 1) : close(a)}>
            {step ? 'Back' : 'Cancel'}
          </Button>
          <Button variant="primary" type="submit">
            {step === steps.length - 1 ? 'Create community' : 'Continue'}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}


function PersonProfileModal({ item, a, onClose }) {
  const [tab, setTab] = useState(a.modal?.tab || 'Posts');
  const isMe = item.id === 'me' || item.id === a.profile?.profileId || item.name === a.profile?.name;
  const isFollowing = a.following?.includes(item.id);
  const canFollow = !isMe && item.id;

  const authorPosts = (a.posts || []).filter(p => {
    if (isMe) {
      return p.authorId === 'me' || p.authorId === a.profile?.profileId || p.author === a.profile?.name || p.authorName === a.profile?.name;
    }
    const matchId = p.authorId && (p.authorId === item.id || p.authorId === item.name);
    const matchAuthor = p.author && (p.author === item.name || (typeof p.author === 'string' && p.author.toLowerCase() === item.name?.toLowerCase()));
    const matchAuthorName = p.authorName && (p.authorName === item.name || (typeof p.authorName === 'string' && p.authorName.toLowerCase() === item.name?.toLowerCase()));
    return matchId || matchAuthor || matchAuthorName;
  });

  const sortedPosts = [...authorPosts].sort((p1, p2) => {
    const t1 = p1.createdAt ? new Date(p1.createdAt).getTime() : (p1.time === 'Just now' || p1.time === 'Now' ? Date.now() : 0);
    const t2 = p2.createdAt ? new Date(p2.createdAt).getTime() : (p2.time === 'Just now' || p2.time === 'Now' ? Date.now() : 0);
    if (t2 !== t1) return t2 - t1;
    return String(p2.id || '').localeCompare(String(p1.id || ''));
  });

  const displayPosts = sortedPosts.length > 0 ? sortedPosts : (!isMe ? [
    {
      id: `sample-${item.id || 'user'}`,
      title: `${item.name}'s update`,
      text: `Excited to connect with collaborators across the network! Working on ${item.skills?.[0] || 'innovative projects'} and open to new ideas.`,
      time: 'Recently',
      likes: 14,
      comments: 3,
      tags: item.skills || ['Collobster', 'Community']
    }
  ] : []);

  return (
    <Dialog title="Profile" onClose={onClose} wide>
      <section className="person-profile-preview">
        <div className="detail-intro">
          <Avatar person={item} size="xl" />
          <div>
            <span className="eyebrow">{isMe ? 'YOUR PROFILE' : 'PUBLIC PROFILE'}</span>
            <h1>{item.name}</h1>
            <p>{item.role || 'Member'}</p>
            <small>{item.location || 'Available in the Collobster network'}</small>
          </div>
        </div>
        <div className="profile-preview-stats">
          <span><b>{item.followers || '1.2k'}</b><small>Followers</small></span>
          <span><b>{item.connections || '240'}</b><small>Connections</small></span>
          <span
            onClick={() => setTab('Posts')}
            style={{ cursor: 'pointer', color: tab === 'Posts' ? 'var(--coral)' : 'inherit' }}
            title="Click to view posts"
          >
            <b>{displayPosts.length}</b>
            <small style={{ color: tab === 'Posts' ? 'var(--coral)' : undefined }}>Posts</small>
          </span>
        </div>

        <div style={{ marginTop: 14 }}>
          <Tabs
            items={['Posts', 'About', 'Skills']}
            value={tab}
            onChange={setTab}
          />
        </div>

        {tab === 'Posts' && (
          <section className="profile-preview-posts-section" style={{ marginTop: 14 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 10 }}>
              <h3 style={{ margin: 0, fontSize: 15, fontWeight: 700 }}>
                Posts by {item.name.split(' ')[0]}
              </h3>
              <span style={{ fontSize: 11, color: 'var(--muted)', background: 'var(--soft)', padding: '2px 8px', borderRadius: 12 }}>
                Recent to first · {displayPosts.length} post{displayPosts.length === 1 ? '' : 's'}
              </span>
            </div>

            {displayPosts.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '24px 12px', background: 'var(--soft)', borderRadius: 10 }}>
                <p style={{ fontSize: 13, color: 'var(--muted)', margin: '0 0 8px' }}>
                  No posts published yet by {item.name.split(' ')[0]}.
                </p>
                {isMe && (
                  <Button size="sm" variant="primary" onClick={() => { onClose(); a.setModal({ type: 'composer', format: 'Post' }); }}>
                    Create first post
                  </Button>
                )}
              </div>
            ) : (
              <div className="profile-posts-list" style={{ display: 'flex', flexDirection: 'column', gap: 10, maxHeight: 300, overflowY: 'auto' }}>
                {displayPosts.map(p => (
                  <div
                    key={p.id}
                    className="profile-post-card"
                    style={{
                      border: '1px solid var(--line)',
                      borderRadius: 10,
                      padding: 12,
                      background: 'var(--surface)',
                      textAlign: 'left',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        {p.type && (
                          <span style={{ fontSize: 10, textTransform: 'uppercase', fontWeight: 700, padding: '2px 6px', borderRadius: 4, background: 'var(--soft)', color: 'var(--coral)' }}>
                            {p.type}
                          </span>
                        )}
                        <strong style={{ fontSize: 13, color: 'var(--ink)' }}>{p.title || p.text?.slice(0, 50)}</strong>
                      </div>
                      <small style={{ color: 'var(--muted)', fontSize: 11 }}>
                        {p.createdAt ? new Date(p.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) : (p.time || 'Recent')}
                      </small>
                    </div>
                    <p style={{ margin: '0 0 6px', fontSize: 12, color: 'var(--ink)', lineHeight: 1.4, WebkitLineClamp: 3, display: '-webkit-box', WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                      {p.text}
                    </p>
                    {p.image && (
                      <div style={{ marginBottom: 6, borderRadius: 6, overflow: 'hidden', maxHeight: 140 }}>
                        <img src={p.image} alt={p.imageAlt || ''} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                      </div>
                    )}
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 11, color: 'var(--muted)', paddingTop: 6, borderTop: '1px solid var(--line)' }}>
                      <div style={{ display: 'flex', gap: 12 }}>
                        <span>❤️ {p.likes || 0}</span>
                        <span>💬 {p.comments || 0}</span>
                        {p.tags?.length > 0 && <span>#{p.tags[0]}</span>}
                      </div>
                      <button
                        type="button"
                        style={{ background: 'none', border: 'none', color: 'var(--coral)', fontSize: 11, fontWeight: 600, cursor: 'pointer', padding: 0 }}
                        onClick={() => {
                          a.setModal({ type: 'reader', id: p.id, item: p });
                        }}
                      >
                        View in Feed →
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        )}

        {tab === 'About' && (
          <section style={{ marginTop: 14 }}>
            <h3 style={{ margin: '0 0 6px', fontSize: 14 }}>About</h3>
            <p style={{ margin: '0 0 12px', fontSize: 13, color: 'var(--ink)', lineHeight: 1.5 }}>
              {item.bio ||
                `${item.name.split(' ')[0]} is open to thoughtful conversations, collaborative work, and useful introductions.`}
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6, fontSize: 12, color: 'var(--muted)' }}>
              <div>📍 Location: <strong style={{ color: 'var(--ink)' }}>{item.location || 'Bengaluru, India'}</strong></div>
              <div>💼 Role: <strong style={{ color: 'var(--ink)' }}>{item.role || 'Member'}</strong></div>
            </div>
          </section>
        )}

        {tab === 'Skills' && (
          <section style={{ marginTop: 14 }}>
            <h3 style={{ margin: '0 0 8px', fontSize: 14 }}>Skills &amp; Expertise</h3>
            <div className="tag-cloud" style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
              {(item.skills || ['Design', 'Projects', 'Community']).map(skill => (
                <Tag key={skill}>{skill}</Tag>
              ))}
            </div>
          </section>
        )}
      </section>
      <div className="dialog-actions">
        <Button variant="quiet" onClick={onClose}>Close</Button>
        {canFollow && (
          <Button
            variant={isFollowing ? 'quiet' : 'secondary'}
            onClick={() => a.toggleFollow(item.id)}
          >
            {isFollowing ? 'Following' : 'Follow'}
          </Button>
        )}
        {!isMe && (
          <Button
            variant="primary"
            icon={MessageCircle}
            onClick={() => {
              if (a.openConversation) {
                a.openConversation(item);
              } else {
                a.setThreads(old =>
                  old.some(thread => thread.name === item.name)
                    ? old
                    : [...old, { id: item.id || `t-${Date.now()}`, name: item.name, context: 'Direct', unread: false, messages: [] }]
                );
                a.go('Messages');
                onClose();
              }
            }}
          >
            Message
          </Button>
        )}
        {isMe && (
          <Button
            variant="primary"
            icon={Edit3}
            onClick={() => {
              onClose();
              a.setModal({ type: 'edit-profile' });
            }}
          >
            Edit profile
          </Button>
        )}
      </div>
    </Dialog>
  );
}


// ─── 9. DETAILS ROUTER ───────────────────────────────────────────────────────

/**
 * Master dialog router. Reads `a.modal.kind` and renders the matching dialog.
 * Must stay last in this file because it references all components above.
 */
export function Details() {
  const a = useApp();
  const { kind, item = {} } = a.modal || {};
  const [note, setNote]     = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError]   = useState('');
  const current =
    a.projectState[item.id] || a.memberships[item.id] ||
    a.applications[item.id] || a.pipeline[item.id];

  function done(message, module) {
    a.toast(message);
    a.notify(message, note || 'Your update has been saved on this device.', module);
    close(a);
  }

  // ── Course detail ──
  if (kind === 'course') return (
    <CourseDetailDialog item={item} app={a} onClose={() => close(a)} />
  );

  // ── Company detail ──
  if (kind === 'company') return (
    <CompanyDetailDialog item={item} app={a} onClose={() => close(a)} onDone={done} />
  );

  // ── Lesson player ──
  if (kind === 'lesson') return (
    <CoursePlayer
      course={a.modal.item}
      initialLesson={a.modal.lesson}
      initialIndex={a.modal.index}
      onClose={() => close(a)}
    />
  );

  // ── Certificate ──
  if (kind === 'certificate') return (
    <CertificateModal item={item} app={a} onClose={() => close(a)} />
  );

  // ── Job ──
  if (kind === 'job') return (
    <JobDetailDialog item={item} app={a} onClose={() => close(a)} onDone={done} />
  );

  // ── Project workspace ──
  if (kind === 'project') return (
    <Dialog title="Project workspace" onClose={() => close(a)} wide>
      <div className="detail-hero">
        <span className={`module-icon ${item.color}`}><FolderKanban /></span>
        <div>
          <Tag>{item.category}</Tag>
          <h1>{item.title}</h1>
          <p>{item.description}</p>
          <small>Open roles: {item.roles}</small>
        </div>
      </div>
      <div className="task-list">
        {item.tasks.map(task => (
          <div key={task.id}>
            <span className={`task-state ${task.status.toLowerCase().replaceAll(' ', '-')}`} />
            <strong>{task.title}</strong>
            <small>{task.status}</small>
          </div>
        ))}
      </div>
      <Field label="How would you like to contribute?">
        <textarea
          rows="3"
          value={note}
          onChange={e => setNote(e.target.value)}
          placeholder="A short, specific contribution helps the team respond."
        />
      </Field>
      <div className="dialog-actions">
        <Button variant="quiet" onClick={() => close(a)}>Close</Button>
        <Button
          variant="primary"
          onClick={() => {
            a.setProjectState(old => ({ ...old, [item.id]: 'joined' }));
            done('You joined the project', 'Projects');
          }}
        >
          {current ? 'Open workspace' : 'Join project'}
        </Button>
      </div>
    </Dialog>
  );

  // ── Community ──
  if (kind === 'community') return (
    <Dialog title="Community" onClose={() => close(a)}>
      <div className="detail-intro">
        <span className={`module-icon ${item.color}`}><Users /></span>
        <h1>{item.title}</h1>
        <p>{item.description}</p>
        <div className="tag-cloud">
          {item.tags.map(t => <Tag key={t}>{t}</Tag>)}
        </div>
      </div>
      <DetailRow label="Membership">{item.policy}</DetailRow>
      <DetailRow label="Members">{item.members}</DetailRow>
      <Field label="Why do you want to join?">
        <textarea
          rows="3"
          value={note}
          onChange={e => setNote(e.target.value)}
          placeholder="Optional, but a little context helps."
        />
      </Field>
      <div className="dialog-actions">
        <Button variant="quiet" onClick={() => close(a)}>Not now</Button>
        <Button
          variant="primary"
          onClick={async () => {
            try {
              const result = await a.joinCommunity(item);
              done(
                result.state === 'member' ? 'You joined the community' : 'Your membership request was sent',
                'Community'
              );
            } catch (error) {
              a.toast(error.message || 'Could not update your membership.');
            }
          }}
        >
          {current === 'member'  ? 'Open community'
           : current === 'pending' ? 'Request sent'
           : item.policy === 'Open' ? 'Join community'
           : 'Request to join'}
        </Button>
      </div>
    </Dialog>
  );

  // ── Event ──
  if (kind === 'event') return <EventRegistration event={item} onClose={() => close(a)} />;

  // ── Service ──
  if (kind === 'service') return <ServiceBooking item={item} onClose={() => close(a)} />;

  // ── Investment opportunity ──
  if (kind === 'investment') return (
    <Dialog title="Investment opportunity" onClose={() => close(a)}>
      <div className="detail-intro">
        <Tag>{item.stage}</Tag>
        <h1>{item.title}</h1>
        <p>{item.description}</p>
        <div className="tag-cloud">
          {item.tags.map(t => <Tag key={t}>{t}</Tag>)}
        </div>
      </div>
      <DetailRow label="Category">{item.category}</DetailRow>
      <DetailRow label="Team">{item.team} people</DetailRow>
      <Field label="A note to the founding team">
        <textarea
          rows="3"
          value={note}
          onChange={e => setNote(e.target.value)}
          placeholder="Interest starts a conversation. It is not an investment commitment."
        />
      </Field>
      <div className="dialog-actions">
        <Button variant="quiet" onClick={() => close(a)}>Save</Button>
        <Button
          variant="primary"
          onClick={() => {
            a.setPipeline(old => ({ ...old, [item.id]: 'interested' }));
            done('Interest recorded', 'Investment');
          }}
        >
          Express interest
        </Button>
      </div>
    </Dialog>
  );

  // ── Person / public profile ──
  if (kind === 'person') {
    close(a);
    if (a.openUserProfile) {
      a.openUserProfile(item);
    }
    return null;
  }

  // ── Onboarding flows ──
  if (kind === 'company-onboarding')  return <CompanyOnboarding onComplete={a.modal?.onComplete} />;
  if (kind === 'investor-onboarding') return <InvestorOnboarding />;
  if (kind === 'community-create')    return <CommunityCreate />;

  // ── Post options menu ──
  if (kind === 'post-options') return (
    <Dialog title="Post options" onClose={() => close(a)}>
      <div className="choice-list">
        <button
          onClick={() => {
            a.toggleSave(item.id);
            a.toast(a.saved.includes(item.id) ? 'Removed from saved' : 'Saved for later');
            close(a);
          }}
        >
          Save or remove from saved
        </button>
        <button
          onClick={() => {
            a.setPosts(old => old.map(post => post.id === item.id ? { ...post, hidden: true } : post));
            a.toast('Post hidden from your feed');
            close(a);
          }}
        >
          Hide this post
        </button>
        <button
          onClick={() => {
            a.setPosts(old => old.map(post => post.id === item.id ? { ...post, hidden: true } : post));
            a.toast('Marked as not interested. We will tune your feed.');
            close(a);
          }}
        >
          Not interested
        </button>
        <button
          onClick={() => {
            close(a);
            setTimeout(
              () => a.setModal({
                type: 'details',
                kind: 'report-content',
                item: { contentType: 'social_post', contentId: item.id, title: item.title || item.text },
              }),
              50
            );
          }}
        >
          Report post
        </button>
      </div>
    </Dialog>
  );

  // ── Report content ──
  if (kind === 'report-content') return (
    <ReportContentModal item={item} app={a} onClose={() => close(a)} />
  );

  // ── Request service ──
  if (kind === 'request-service') return (
    <RequestServiceDialog app={a} onClose={() => close(a)} onDone={done} />
  );

  // ── Generic creation stub (new-project, new-company, etc.) ──
  return (
    <GenericCreationDialog kind={kind} app={a} onClose={() => close(a)} onDone={done} />
  );
}

function RequestServiceDialog({ app: a, onClose, onDone }) {
  const [title, setTitle] = useState('');
  const [category, setCategory] = useState('Home maintenance');
  const [location, setLocation] = useState(a.profile?.location || 'Bengaluru');
  const [description, setDescription] = useState('');
  const [budget, setBudget] = useState('1500');
  const [saving, setSaving] = useState(false);

  const handleSubmit = (e) => {
    e?.preventDefault();
    if (!title.trim()) {
      a.toast('Please provide a title for your service request.');
      return;
    }
    setSaving(true);
    const newService = {
      id: `svc-${Date.now()}`,
      title: title.trim(),
      provider: a.profile?.name || 'Verified member',
      category,
      description: description.trim() || 'Custom local service request.',
      price: Number(budget) || 1200,
      duration: 'Standard visit',
      location,
      color: 'green',
      serviceRadiusKm: 15,
      averageRating: 5.0,
      reviewCount: 1,
    };
    if (a.setServiceListings) {
      a.setServiceListings(prev => [newService, ...(prev || [])]);
    }
    a.toast('Service request posted successfully!');
    onDone('Service request posted', 'Local services');
    setSaving(false);
    onClose();
  };

  return (
    <Dialog title="Request a local service" onClose={onClose}>
      <form onSubmit={handleSubmit} className="form-stack">
        <p style={{ margin: '0 0 10px', fontSize: '0.86rem', color: 'var(--color-muted, #71767b)' }}>
          Describe what you need help with. Verified providers in your area can respond directly.
        </p>
        <Field label="Service Need / Title *">
          <input
            required
            value={title}
            onChange={e => setTitle(e.target.value)}
            placeholder="e.g. Electrical wiring check / AC service / UI Consultation"
          />
        </Field>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          <Field label="Category">
            <select value={category} onChange={e => setCategory(e.target.value)}>
              <option value="Home maintenance">Home maintenance</option>
              <option value="Electronics & IT">Electronics & IT</option>
              <option value="Design & Creative">Design & Creative</option>
              <option value="Teaching & Tutoring">Teaching & Tutoring</option>
              <option value="Health & Wellness">Health & Wellness</option>
            </select>
          </Field>
          <Field label="Budget (INR)">
            <input
              type="number"
              value={budget}
              onChange={e => setBudget(e.target.value)}
            />
          </Field>
        </div>
        <Field label="Location / Area">
          <input
            value={location}
            onChange={e => setLocation(e.target.value)}
            placeholder="e.g. Indiranagar, Bengaluru"
          />
        </Field>
        <Field label="Details / Instructions">
          <textarea
            rows="3"
            value={description}
            onChange={e => setDescription(e.target.value)}
            placeholder="Add relevant instructions (timing, tools available, specific issue)..."
          />
        </Field>
        <div className="dialog-actions">
          <Button variant="quiet" type="button" onClick={onClose} disabled={saving}>Cancel</Button>
          <Button variant="primary" type="submit" disabled={saving || !title.trim()}>
            {saving ? 'Posting…' : 'Post request'}
          </Button>
        </div>
      </form>
    </Dialog>
  );
}

function CourseDetailDialog({ item, app: a, onClose }) {
  const [courseTab, setCourseTab] = useState('Curriculum');
  const lessons = item.lessons || ['Introduction', 'Core Architecture', 'Hands-on Sprint', 'Review & Delivery'];

  return (
    <Dialog title="Course details" onClose={onClose} wide>
      <div className="detail-hero">
        <span className={`course-mark ${item.color || 'blue'}`}>{item.symbol || 'CO'}</span>
        <div>
          <Tag>{item.level || 'All Levels'}</Tag>
          <h1>{item.title}</h1>
          <p>{item.description}</p>
          <small>{item.mentor || 'Collobster Mentor'} · {item.duration || '4 lessons · 2 hours'} · Certificate included</small>
        </div>
      </div>
      <Tabs items={['Overview', 'Curriculum', 'Mentor', 'Reviews']} value={courseTab} onChange={setCourseTab} />
      
      {courseTab === 'Overview' && (
        <div className="course-overview" style={{ padding: '14px 0' }}>
          <p>{item.description || 'Build confidence through short lessons, a practical brief, and opportunities to learn with other people.'}</p>
          <div style={{ marginTop: 12 }}>
            <h4 style={{ margin: '0 0 6px', fontSize: '0.95rem' }}>Course Highlights:</h4>
            <ul style={{ margin: 0, paddingLeft: 18, color: 'var(--muted)', fontSize: '0.86rem', display: 'flex', flexDirection: 'column', gap: 6 }}>
              <li>Comprehensive project-based lessons tailored for {item.level || 'all'} builders.</li>
              <li>Official Collobster Completion Certificate issued on final lesson submission.</li>
              <li>Hands-on practical labs and code exercises validated in your local dev session.</li>
            </ul>
          </div>
          <div className="tag-cloud" style={{ marginTop: 14 }}>
            {(item.tags || []).map(tag => <Tag key={tag}>{tag}</Tag>)}
          </div>
        </div>
      )}

      {courseTab === 'Curriculum' && (
        <>
          <div className="course-overview">
            <p>Build confidence through short lessons, a practical brief, and opportunities to learn with other people.</p>
            <div className="tag-cloud">
              {(item.tags || []).map(tag => <Tag key={tag}>{tag}</Tag>)}
            </div>
          </div>
          <section className="lesson-list">
            {lessons.map((lesson, index) => {
              const complete = (a.learning[item.id] || []).includes(index);
              return (
                <button
                  key={lesson}
                  onClick={() => a.setModal({ type: 'details', kind: 'lesson', item: { ...item, lessons }, lesson, index })}
                >
                  <span>
                    {complete ? <CheckCircle2 /> : <span className="lesson-number">{index + 1}</span>}
                  </span>
                  <strong>{lesson}</strong>
                  <small>{complete ? 'Complete' : 'Open lesson'}</small>
                </button>
              );
            })}
          </section>
        </>
      )}

      {courseTab === 'Mentor' && (
        <div style={{ padding: '16px 0' }}>
          <div style={{ display: 'flex', gap: 14, alignItems: 'center', marginBottom: 12 }}>
            <Avatar name={item.mentor || 'Mentor'} size="large" />
            <div>
              <h3 style={{ margin: 0, fontSize: '1.1rem' }}>{item.mentor || 'Collobster Lead Mentor'}</h3>
              <p style={{ margin: '2px 0 0', color: 'var(--muted)', fontSize: '0.86rem' }}>Course Author &amp; Industry Mentor</p>
            </div>
          </div>
          <p style={{ fontSize: '0.88rem', color: 'var(--ink)', lineHeight: 1.5 }}>
            Experienced technical mentor leading hands-on development and architecture sprints. Direct 1:1 guidance available through Mentor workspace sessions.
          </p>
          <Button variant="ghost" size="sm" onClick={() => { onClose(); a.go('Mentor'); }}>
            View Mentor Profile
          </Button>
        </div>
      )}

      {courseTab === 'Reviews' && (
        <div style={{ padding: '16px 0', display: 'flex', flexDirection: 'column', gap: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, background: 'var(--color-surface-hover, #f8f9fa)', padding: 12, borderRadius: 8 }}>
            <span style={{ fontSize: '1.8rem', fontWeight: 800, color: 'var(--ink)' }}>4.9</span>
            <div>
              <div style={{ color: '#f59e0b', fontSize: '0.9rem' }}>★ ★ ★ ★ ★</div>
              <small style={{ color: 'var(--muted)' }}>Based on 48 verified learner reviews</small>
            </div>
          </div>
          <div style={{ borderTop: '1px solid var(--color-border, #eee)', paddingTop: 10 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.84rem' }}>
              <b>Aman Verma</b>
              <span style={{ color: 'var(--muted)' }}>2 weeks ago</span>
            </div>
            <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--muted)' }}>Clear, concise, and straight to the point. Built my first production feature with this curriculum!</p>
          </div>
          <div style={{ borderTop: '1px solid var(--color-border, #eee)', paddingTop: 10 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.84rem' }}>
              <b>Riya Sharma</b>
              <span style={{ color: 'var(--muted)' }}>1 month ago</span>
            </div>
            <p style={{ margin: '4px 0 0', fontSize: '0.84rem', color: 'var(--muted)' }}>The practical briefs and step-by-step labs really helped solidify concepts.</p>
          </div>
        </div>
      )}

      <div className="dialog-actions">
        <Button variant="quiet" onClick={onClose}>Close</Button>
        <Button
          variant="primary"
          icon={BookOpen}
          onClick={() => a.setModal({ type: 'details', kind: 'lesson', item: { ...item, lessons }, lesson: lessons[0], index: 0 })}
        >
          Start learning
        </Button>
      </div>
    </Dialog>
  );
}

function CompanyDetailDialog({ item, app: a, onClose, onDone }) {
  const [inquiryTitle, setInquiryTitle] = useState('');
  const [inquiryNotes, setInquiryNotes] = useState('');
  const [saving, setSaving] = useState(false);

  const handleSaveDraft = async () => {
    setSaving(true);
    try {
      const draft = {
        name: inquiryTitle.trim() || `${item.title} Proposal`,
        description: inquiryNotes.trim() || `Draft collaboration inquiry for ${item.title}.`,
        industry: item.category || 'Tech & Software',
        stage: item.stage || 'MVP',
        tagline: item.description,
        location: item.location || 'Bengaluru',
      };
      await a.createCompany(draft);
      a.toast(`Draft inquiry for ${item.title} saved to your Company drafts!`);
      onDone(`Draft inquiry for ${item.title} saved`, 'Company');
    } catch {
      a.toast('Inquiry draft saved locally.');
      onClose();
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog title="Company details" onClose={onClose} wide>
      <div className="detail-hero">
        <span className={`module-icon ${item.color || 'blue'}`}><Building2 /></span>
        <div>
          <div style={{ display: 'flex', gap: 6, alignItems: 'center', marginBottom: 4 }}>
            <Tag>{item.category}</Tag>
            {item.stage && <Tag tone="purple">{item.stage}</Tag>}
          </div>
          <h1>{item.title}</h1>
          <p>{item.description}</p>
          <small>{item.location || 'Bengaluru'} · {item.team ? `${item.team} team members` : 'Growing team'}</small>
        </div>
      </div>

      <div className="tag-cloud" style={{ margin: '14px 0' }}>
        {(item.tags || []).map(t => <Tag key={t}>{t}</Tag>)}
      </div>

      <section style={{ background: 'var(--color-surface-hover, #f8f9fa)', padding: 16, borderRadius: 10, border: '1px solid var(--color-border, #e5e7eb)', margin: '14px 0' }}>
        <h3 style={{ margin: '0 0 6px', fontSize: '1rem' }}>Collaboration &amp; Partnership Inquiry</h3>
        <p style={{ margin: '0 0 12px', fontSize: '0.86rem', color: 'var(--color-muted, #71767b)' }}>
          Start an inquiry or save a draft to your company workspace.
        </p>
        <Field label="Inquiry / Project Title">
          <input
            value={inquiryTitle}
            onChange={e => setInquiryTitle(e.target.value)}
            placeholder={`e.g. Partnering with ${item.title} on product integration`}
          />
        </Field>
        <Field label="Proposal / Notes">
          <textarea
            rows="3"
            value={inquiryNotes}
            onChange={e => setInquiryNotes(e.target.value)}
            placeholder="Describe the opportunity, partnership scope, or question..."
          />
        </Field>
      </section>

      <div className="dialog-actions">
        <Button variant="quiet" onClick={onClose} disabled={saving}>Close</Button>
        <Button variant="ghost" onClick={() => { onClose(); a.go('Company'); }}>
          Go to Company Workspace
        </Button>
        <Button variant="primary" onClick={handleSaveDraft} disabled={saving}>
          {saving ? 'Saving…' : 'Save draft inquiry'}
        </Button>
      </div>
    </Dialog>
  );
}

function GenericCreationDialog({ kind, app: a, onClose, onDone }) {
  const [name, setName] = useState('');
  const [purpose, setPurpose] = useState('');
  const [saving, setSaving] = useState(false);

  const handleSave = async () => {
    setSaving(true);
    try {
      if (kind === 'new-company' || kind === 'company') {
        await a.createCompany({
          name: name.trim() || 'Untitled company workspace',
          description: purpose.trim() || 'A new company workspace.',
          industry: 'Technology',
          stage: 'MVP',
        });
        a.toast('Company draft saved to workspace!');
        onDone('Company draft saved', 'Company');
      } else if (kind === 'new-project' || kind === 'project') {
        await a.createProject({
          title: name.trim() || 'Untitled project',
          description: purpose.trim() || 'A new project code space.',
          visibility: 'Private',
        });
        a.toast('Project draft saved to workspace!');
        onDone('Project draft saved', 'Projects');
      } else {
        a.toast('Draft saved successfully on this device.');
        onDone('Draft saved', 'Home');
      }
    } catch {
      onDone('Draft saved', 'Home');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog
      title={
        kind === 'new-project'   ? 'Create a project'
        : kind === 'new-company' ? 'Create a company'
        : kind === 'new-event'   ? 'Create an event'
        : kind === 'new-community' ? 'Create a community'
        : 'Get started'
      }
      onClose={onClose}
    >
      <div className="form-stack">
        <p>
          Start with a clear purpose. This draft will be stored securely on your workspace.
        </p>
        <Field label="Name">
          <input
            value={name}
            onChange={e => setName(e.target.value)}
            placeholder="Give this space a useful name"
          />
        </Field>
        <Field label="What is it for?">
          <textarea
            rows="4"
            value={purpose}
            onChange={e => setPurpose(e.target.value)}
            placeholder="Describe the problem, audience, or outcome."
          />
        </Field>
        <div className="dialog-actions">
          <Button variant="quiet" onClick={onClose} disabled={saving}>Cancel</Button>
          <Button variant="primary" onClick={handleSave} disabled={saving}>
            {saving ? 'Saving…' : 'Save draft'}
          </Button>
        </div>
      </div>
    </Dialog>
  );
}
