import React, { useEffect, useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Apple,
  CheckCircle2,
  Eye,
  EyeOff,
  GitBranch,
  MapPin,
  ShieldCheck,
  Sparkles,
} from 'lucide-react';
import {
  beginOidcLogin,
  finishOidcLogin,
  hasOidcCallback,
  oidcConfigured,
} from '../oidc';
import { PolicyDialog } from '../components';
import { api } from '../api';

// ─── Constants & Seed Data ───────────────────────────────────────────────────

const initial = {
  firstName: '',
  lastName: '',
  email: '',
  phone: '',
  password: '',
  confirmPassword: '',
  username: '',
  dob: '',
  gender: '',
  country: 'India',
  city: 'Bengaluru',
  location: 'Bengaluru, India',
  role: '',
  skills: '',
  interests: '',
};

export { genderOptions, countryCityMap } from '../geo.mjs';
import { genderOptions, countryCityMap } from '../geo.mjs';

const oauth = [
  ['Google', 'G'],
  ['Apple', Apple],
  ['GitHub', GitBranch],
];

// ─── 1. Form UI Components ───────────────────────────────────────────────────

function Value({ icon: Icon, title, text }) {
  return (
    <div>
      <Icon size={20} />
      <b>{title}</b>
      <span>{text}</span>
    </div>
  );
}

function MailIcon() {
  return <span className="mail-glyph">@</span>;
}

function Primary({ children, disabled = false }) {
  return (
    <button className="auth-primary" type="submit" disabled={disabled}>
      {children}
    </button>
  );
}

function Error({ text }) {
  return (
    <p className="auth-error" role="alert">
      {text}
    </p>
  );
}

function TextField({ label, type = 'text', value, onChange, placeholder, icon: Icon }) {
  return (
    <label className="auth-field">
      <span>{label}</span>
      <div className="auth-input">
        {Icon && <Icon size={17} />}
        <input
          type={type}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
        />
      </div>
    </label>
  );
}

function PasswordField({ value, show, onToggle, onChange }) {
  return (
    <label className="auth-field">
      <span>Password</span>
      <div className="auth-input">
        <input
          type={show ? 'text' : 'password'}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder="Create a password"
        />
        <button
          type="button"
          onClick={onToggle}
          aria-label={show ? 'Hide password' : 'Show password'}
        >
          {show ? <EyeOff size={17} /> : <Eye size={17} />}
        </button>
      </div>
    </label>
  );
}

function SelectField({ label, value, onChange, options }) {
  return (
    <label className="auth-field">
      <span>{label}</span>
      <select value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">Select an option</option>
        {options.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </select>
    </label>
  );
}

// ─── 2. Landing View ─────────────────────────────────────────────────────────

function Landing({ onSignIn, onSignUp, onExploreDemo }) {
  const [policyModal, setPolicyModal] = useState(null);

  return (
    <main className="landing">
      <nav className="landing-nav">
        <button
          className="auth-brand"
          type="button"
          onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
          title="Collobster home"
        >
          <img src="/favicon.svg" alt="Collobster logo" />
          collobster<span>.</span>
        </button>
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <button
            className="landing-cta"
            type="button"
            onClick={onExploreDemo}
            style={{ background: 'var(--coral)', color: '#fff', border: 'none', padding: '8px 16px', borderRadius: '8px', fontWeight: 600, cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '6px' }}
          >
            <Sparkles size={16} /> Explore Demo
          </button>
          <button className="landing-link" onClick={onSignIn}>
            Sign in
          </button>
          <button className="landing-cta" onClick={onSignUp}>
            Create your space <ArrowRight size={16} />
          </button>
        </div>
      </nav>

      <section className="landing-hero">
        <div>
          <span className="auth-kicker">ONE PLACE. MANY NEXT STEPS.</span>
          <h1>Bring your ideas closer to the people who make them move.</h1>
          <p>
            Learn with intention, meet good collaborators, discover opportunities,
            and give your work somewhere to grow.
          </p>
          <div className="landing-actions" style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', alignItems: 'center' }}>
            <button
              className="landing-cta"
              type="button"
              onClick={onExploreDemo}
              style={{ background: 'var(--coral)', color: '#fff', border: 'none', padding: '12px 22px', borderRadius: '10px', fontWeight: 600, cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '8px', fontSize: '15px' }}
            >
              <Sparkles size={18} /> Explore Live Demo <ArrowRight size={17} />
            </button>
            <button className="landing-cta" onClick={onSignUp} style={{ background: 'var(--surface)', color: 'var(--ink)', border: '1px solid var(--line)' }}>
              Get started
            </button>
            <button className="landing-link" onClick={onSignIn}>
              I already have an account
            </button>
          </div>
        </div>

        <div className="landing-scene">
          <div className="scene-card scene-main">
            <Sparkles size={24} />
            <span>Today, make room for</span>
            <strong>A better next step.</strong>
            <div className="scene-progress">
              <i />
              <i />
              <i />
              <i />
            </div>
          </div>
          <div className="scene-card scene-note">
            <MailIcon />
            <b>A thoughtful message</b>
            <small>from someone who gets it</small>
          </div>
          <div className="scene-card scene-map">
            <MapPin size={18} />
            <b>A local collaboration</b>
            <small>worth showing up for</small>
          </div>
        </div>
      </section>

      <section className="landing-values">
        <Value
          icon={ShieldCheck}
          title="Intentional by default"
          text="Space for work that matters."
        />
        <Value
          icon={GitBranch}
          title="Built for momentum"
          text="Learning, collaboration, and progress."
        />
        <Value
          icon={CheckCircle2}
          title="Your space, your pace"
          text="Start small and make it yours."
        />
      </section>

      <footer className="landing-footer">
        <div className="landing-footer-grid">
          <div className="landing-footer-brand">
            <div className="auth-brand" style={{ cursor: 'pointer' }} onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
              <img src="/favicon.svg" alt="" />
              collobster<span>.</span>
            </div>
            <p>A collaborative workspace for builders, learners, and operators worldwide.</p>
            <div className="landing-footer-contact">
              <a href="mailto:support@collobster.app" className="landing-contact-link">
                support@collobster.app
              </a>
              <span className="contact-divider">·</span>
              <a href="tel:+918045678900" className="landing-contact-link">
                +91 80 4567 8900
              </a>
            </div>
          </div>

          <div className="landing-footer-col">
            <h4>Platform</h4>
            <button type="button" onClick={onSignUp}>Get Started</button>
            <button type="button" onClick={onSignIn}>Sign In</button>
            <button type="button" onClick={() => setPolicyModal({ title: 'Help & Support', kind: 'help' })}>Support Center</button>
          </div>

          <div className="landing-footer-col">
            <h4>Trust & Legal</h4>
            <button type="button" onClick={() => setPolicyModal({ title: 'Terms of Service', kind: 'terms' })}>Terms of Service</button>
            <button type="button" onClick={() => setPolicyModal({ title: 'Privacy Policy', kind: 'privacy' })}>Privacy Policy</button>
            <button type="button" onClick={() => setPolicyModal({ title: 'Community Guidelines', kind: 'guidelines' })}>Guidelines</button>
          </div>
        </div>

        <div className="landing-footer-bottom">
          <p>© {new Date().getFullYear()} Collobster, Inc. All rights reserved.</p>
          <p>Designed for calm, focused, and momentum-driven progress.</p>
        </div>
      </footer>

      {policyModal && (
        <PolicyDialog
          title={policyModal.title}
          kind={policyModal.kind}
          onClose={() => setPolicyModal(null)}
        />
      )}
    </main>
  );
}

// ─── 3. Auth Main Component (Exported) ───────────────────────────────────────

export function Auth({ onComplete }) {
  const [mode, setMode] = useState('landing');
  const [step, setStep] = useState(0);
  const [form, setForm] = useState(initial);
  const [show, setShow] = useState(false);
  const [error, setError] = useState('');
  const [oidcSession, setOidcSession] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [verifyToken, setVerifyToken] = useState('');
  const [verifySuccess, setVerifySuccess] = useState('');
  const [pendingSession, setPendingSession] = useState(null);
  const [customCity, setCustomCity] = useState(false);

  useEffect(() => {
    try {
      const urlParams = new URLSearchParams(window.location.search);
      const token = urlParams.get('verify') || urlParams.get('token');
      if (token) {
        setVerifyToken(token);
        setMode('verify');
      }
    } catch (e) {}
  }, []);

  useEffect(() => {
    if (!hasOidcCallback()) return;
    let active = true;

    finishOidcLogin()
      .then((result) => {
        if (!active) return;
        const claims = result.claims || {};
        const name = (claims.name || '').split(' ');
        setForm((old) => ({
          ...old,
          firstName: claims.given_name || name[0] || old.firstName,
          lastName: claims.family_name || name.slice(1).join(' ') || old.lastName,
          email: claims.email || old.email,
        }));
        setOidcSession({
          accessToken: result.access_token,
          provider: result.provider,
        });
        setMode('signup');
        setStep(1);
      })
      .catch((exception) => {
        if (active) {
          setError(
            exception.message === 'OIDC_CALLBACK_INVALID'
              ? 'Your sign-in session expired. Please try again.'
              : 'We could not finish sign in with your provider.'
          );
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const update = (key, value) => {
    setForm((x) => ({ ...x, [key]: value }));
    setError('');
  };

  const signIn = async (e) => {
    e.preventDefault();
    if (!form.email || !form.password) {
      return setError('Enter your email and password to continue.');
    }
    if (!api.configured) {
      onComplete({ ...form, firstName: form.firstName || 'Ananya', lastName: form.lastName || 'Sharma' });
      return;
    }
    setSubmitting(true);
    try {
      const session = await api.auth.login({ email: form.email, password: form.password });
      const names = (session.displayName || '').trim().split(/\s+/);
      onComplete({
        ...form,
        firstName: names[0] || 'Member',
        lastName: names.slice(1).join(' '),
        email: session.email || form.email,
        accessToken: session.accessToken,
        refreshToken: session.refreshToken,
        profileId: session.profileId,
        provider: 'email',
      });
    } catch (exception) {
      setError(exception.detail || 'We could not sign you in. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const signUp = (e) => {
    e.preventDefault();
    if (!form.firstName || !form.lastName || !form.email || !form.phone || !form.password) {
      return setError('Complete each field to continue.');
    }
    if (form.password !== form.confirmPassword) {
      return setError('Your passwords do not match.');
    }
    setStep(1);
  };

  const setup = async (e) => {
    e.preventDefault();
    if (
      !form.username ||
      !form.dob ||
      !form.gender ||
      !form.location ||
      !form.role ||
      !form.skills ||
      !form.interests
    ) {
      return setError('Complete your profile details to continue.');
    }
    if (!api.configured) {
      onComplete({ ...form, accessToken: oidcSession?.accessToken, provider: oidcSession?.provider || form.provider });
      return;
    }
    if (oidcSession?.accessToken) {
      setError('OIDC account provisioning is not configured for this API. Use email sign-up or complete the server-side OIDC integration before release.');
      return;
    }
    setSubmitting(true);
    try {
      const session = await api.auth.register({
        email: form.email,
        password: form.password,
        displayName: `${form.firstName} ${form.lastName}`.trim(),
        username: form.username,
        termsVersion: '2026.1',
        privacyPolicyVersion: '2026.1',
      });
      const pending = {
        ...form,
        email: session.email || form.email,
        accessToken: session.accessToken,
        refreshToken: session.refreshToken,
        profileId: session.profileId,
        provider: 'email',
      };
      setPendingSession(pending);
      setMode('verify');
      setError('');
    } catch (exception) {
      setError(exception.detail || 'We could not create your account. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleVerifyEmail = async (e) => {
    e.preventDefault();
    if (!verifyToken.trim()) {
      return setError('Enter the verification token or code sent to your email.');
    }
    setSubmitting(true);
    setError('');
    try {
      if (api.configured) {
        await api.auth.verifyEmail(verifyToken.trim());
      }
      setVerifySuccess('Email has been successfully verified!');
      setTimeout(() => {
        if (pendingSession) {
          onComplete(pendingSession);
        } else {
          setMode('signin');
          setVerifySuccess('');
        }
      }, 1200);
    } catch (exception) {
      setError(exception.detail || 'Invalid or expired email verification token.');
    } finally {
      setSubmitting(false);
    }
  };

  const oauthStart = async (provider) => {
    setError('');
    if (provider.toLowerCase() === 'google') {
      const backendBase = (import.meta.env?.VITE_API_URL || '').replace(/\/api\/?$/, '').replace(/\/$/, '') || 'https://collobster-for-web-launch-v1-test.onrender.com';
      window.location.href = `${backendBase}/oauth2/authorization/google`;
      return;
    }
    if (api.configured) {
      setError(`${provider} sign-in is not configured yet. Please sign in with Google or use email sign-in.`);
      return;
    }
    if (!oidcConfigured) {
      setForm((x) => ({
        ...x,
        firstName: x.firstName || 'Ananya',
        lastName: x.lastName || 'Sharma',
        email: x.email || `ananya@${provider.toLowerCase()}.example`,
        provider,
      }));
      setMode('signup');
      setStep(1);
      setError('Connect an OIDC provider to use real social sign-in. Continuing in Preview mode.');
      return;
    }
    try {
      await beginOidcLogin(provider);
    } catch {
      setError('We could not start secure sign in. Please try again.');
    }
  };

  if (mode === 'landing') {
    return (
      <Landing
        onSignIn={() => setMode('signin')}
        onSignUp={() => {
          setMode('signup');
          setStep(0);
        }}
        onExploreDemo={() => onComplete({ firstName: 'Ananya', lastName: 'Sharma', provider: 'preview' })}
      />
    );
  }

  const signup = mode === 'signup';

  return (
    <main className="auth-shell">
      <section className="auth-aside">
        <button className="auth-brand" onClick={() => setMode('landing')}>
          <img src="/favicon.svg" alt="" />
          collobster<span>.</span>
        </button>

        <div className="auth-aside-copy">
          <span className="auth-kicker">YOUR EVERYDAY, CONNECTED</span>
          <h1>Make the next good thing easier to find.</h1>
          <p>
            A calm place to learn, meet people, build work, and keep the right
            opportunities close.
          </p>
        </div>

        <div className="auth-proof">
          <CheckCircle2 size={18} />
          <span>Profile details stay on this device in the interactive preview.</span>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-card">
          <button className="auth-back" onClick={() => setMode('landing')}>
            <ArrowLeft size={17} /> Back
          </button>

          <div className="auth-heading">
            <span className="auth-kicker">
              {mode === 'verify'
                ? 'EMAIL VERIFICATION'
                : signup
                ? 'CREATE YOUR SPACE'
                : 'WELCOME BACK'}
            </span>
            <h2>
              {mode === 'verify'
                ? 'Verify your email address'
                : signup
                ? step
                  ? 'A little about you.'
                  : 'Start with the essentials.'
                : 'Sign in to continue.'}
            </h2>
            <p>
              {mode === 'verify'
                ? 'Confirm your email to complete registration and secure your account.'
                : signup
                ? step
                  ? 'This helps shape your first Collobster experience.'
                  : 'Build your personal space in a minute.'
                : 'Your conversations, learning, and work are waiting.'}
            </p>
          </div>

          {signup && mode !== 'verify' && (
            <div className="auth-steps">
              <span className={step === 0 ? 'active' : ''}>1 Account</span>
              <span className={step === 1 ? 'active' : ''}>2 Profile setup</span>
            </div>
          )}

          {mode === 'verify' ? (
            /* Email Verification Form */
            <form className="auth-form" onSubmit={handleVerifyEmail}>
              <div style={{ textAlign: 'center', marginBottom: '14px' }}>
                <div style={{
                  width: '44px',
                  height: '44px',
                  borderRadius: '50%',
                  background: 'var(--accent-tint, rgba(227, 77, 66, 0.1))',
                  color: 'var(--accent, #e34d42)',
                  display: 'inline-flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  marginBottom: '10px'
                }}>
                  <MailIcon />
                </div>
                <h3 style={{ margin: '0 0 6px', fontSize: '15px' }}>Check your inbox</h3>
                <p style={{ margin: 0, fontSize: '13px', color: 'var(--color-muted, #71767b)' }}>
                  We sent a verification code to <strong>{form.email || 'your email'}</strong>. Enter it below to activate your account.
                </p>
              </div>

              <TextField
                label="Verification token or code"
                value={verifyToken}
                onChange={(v) => { setVerifyToken(v); setError(''); }}
                placeholder="Enter verification code / token"
              />

              {verifySuccess && (
                <div style={{
                  padding: '10px 14px',
                  borderRadius: '8px',
                  background: '#ecfdf5',
                  color: '#065f46',
                  fontSize: '13px',
                  fontWeight: 500,
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px'
                }}>
                  <CheckCircle2 size={16} />
                  {verifySuccess}
                </div>
              )}

              {error && <Error text={error} />}

              <Primary disabled={submitting || Boolean(verifySuccess)}>
                {submitting ? 'Verifying…' : <>Confirm email <ArrowRight size={17} /></>}
              </Primary>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '10px' }}>
                <button
                  type="button"
                  className="auth-link"
                  onClick={() => {
                    if (pendingSession) {
                      onComplete(pendingSession);
                    } else {
                      setMode('signin');
                      setError('');
                    }
                  }}
                >
                  {pendingSession ? 'Continue to App (Verify later)' : 'Back to Sign In'}
                </button>

                <button
                  type="button"
                  className="auth-link"
                  onClick={() => {
                    setError('If an account exists, a new verification email will be dispatched.');
                  }}
                >
                  Resend code
                </button>
              </div>
            </form>
          ) : !signup ? (
            /* Sign In Form */
            <form className="auth-form" onSubmit={signIn}>
              <TextField
                label="Email address"
                type="email"
                value={form.email}
                onChange={(v) => update('email', v)}
                placeholder="you@example.com"
              />
              <PasswordField
                value={form.password}
                show={show}
                onToggle={() => setShow(!show)}
                onChange={(v) => update('password', v)}
              />
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
                <button
                  className="auth-link"
                  type="button"
                  onClick={() =>
                    setError('Password recovery instructions are dispatched to your verified email.')
                  }
                >
                  Forgot password?
                </button>
                <button
                  className="auth-link"
                  type="button"
                  onClick={() => {
                    setMode('verify');
                    setError('');
                  }}
                >
                  Have a verification code?
                </button>
              </div>
              {error && <Error text={error} />}
              <Primary disabled={submitting}>
                {submitting ? 'Signing in…' : <>Sign in <ArrowRight size={17} /></>}
              </Primary>
              <button
                type="button"
                className="auth-link"
                style={{ textAlign: 'center', marginTop: '12px', color: 'var(--coral)', fontWeight: 600, display: 'block', width: '100%', cursor: 'pointer', background: 'none', border: 'none' }}
                onClick={() => onComplete({ firstName: 'Ananya', lastName: 'Sharma', provider: 'preview' })}
              >
                Or enter interactive demo workspace directly →
              </button>
            </form>
          ) : step === 0 ? (
            /* Sign Up Step 1: Account Essentials */
            <form className="auth-form" onSubmit={signUp}>
              <div className="auth-two">
                <TextField
                  label="First name"
                  value={form.firstName}
                  onChange={(v) => update('firstName', v)}
                  placeholder="First name"
                />
                <TextField
                  label="Last name"
                  value={form.lastName}
                  onChange={(v) => update('lastName', v)}
                  placeholder="Last name"
                />
              </div>
              <TextField
                label="Email address"
                type="email"
                value={form.email}
                onChange={(v) => update('email', v)}
                placeholder="you@example.com"
              />
              <TextField
                label="Phone number"
                type="tel"
                value={form.phone}
                onChange={(v) => update('phone', v)}
                placeholder="+91 98765 43210"
              />
              <PasswordField
                value={form.password}
                show={show}
                onToggle={() => setShow(!show)}
                onChange={(v) => update('password', v)}
              />
              <TextField
                label="Confirm password"
                type={show ? 'text' : 'password'}
                value={form.confirmPassword}
                onChange={(v) => update('confirmPassword', v)}
                placeholder="Confirm your password"
              />
              {error && <Error text={error} />}
              <Primary disabled={submitting}>
                Continue <ArrowRight size={17} />
              </Primary>
            </form>
          ) : (
            /* Sign Up Step 2: Profile Details */
            <form className="auth-form" onSubmit={setup}>
              <TextField
                label="Username"
                value={form.username}
                onChange={(v) => update('username', v)}
                placeholder="e.g. ananya.builds"
              />
              <TextField
                label="Date of birth"
                type="date"
                value={form.dob}
                onChange={(v) => update('dob', v)}
              />
              <SelectField
                label="Gender"
                value={form.gender}
                onChange={(v) => update('gender', v)}
                options={genderOptions}
              />
              <SelectField
                label="Country"
                value={form.country || 'India'}
                onChange={(v) => {
                  update('country', v);
                  const cities = countryCityMap[v] || [];
                  const firstRec = cities[0] || '';
                  setCustomCity(false);
                  update('city', firstRec);
                  update('location', firstRec ? `${firstRec}, ${v}` : v);
                }}
                options={Object.keys(countryCityMap)}
              />
              <label className="auth-field">
                <span>City</span>
                <select
                  value={
                    customCity
                      ? '__custom__'
                      : (countryCityMap[form.country || 'India']?.includes(form.city)
                        ? form.city
                        : (form.city ? '__custom__' : ''))
                  }
                  onChange={(e) => {
                    const selectedVal = e.target.value;
                    if (selectedVal === '__custom__') {
                      setCustomCity(true);
                      update('city', '');
                      update('location', form.country || 'India');
                    } else {
                      setCustomCity(false);
                      update('city', selectedVal);
                      update('location', selectedVal ? `${selectedVal}, ${form.country || 'India'}` : (form.country || 'India'));
                    }
                  }}
                >
                  <option value="">Select your city in {form.country || 'India'}</option>
                  {(countryCityMap[form.country || 'India'] || []).map((city) => (
                    <option key={city} value={city}>
                      {city}
                    </option>
                  ))}
                  <option value="__custom__">Other / Enter custom city…</option>
                </select>
              </label>
              {(customCity || (form.city && !countryCityMap[form.country || 'India']?.includes(form.city))) && (
                <div className="auth-input" style={{ marginTop: '-4px' }}>
                  <MapPin size={17} />
                  <input
                    type="text"
                    value={form.city || ''}
                    onChange={(e) => {
                      const c = e.target.value;
                      update('city', c);
                      update('location', c ? `${c}, ${form.country || 'India'}` : form.country || '');
                    }}
                    placeholder={`Type your city name in ${form.country || 'India'}`}
                    autoFocus
                  />
                </div>
              )}
              {countryCityMap[form.country || 'India']?.length > 0 && (
                <div className="city-chips-container" style={{ margin: '-4px 0 12px', display: 'flex', flexDirection: 'column', gap: 6 }}>
                  <small style={{ fontSize: '0.78rem', color: 'var(--color-muted, #71767b)', fontWeight: 500 }}>
                    Recommended cities in {form.country || 'India'}:
                  </small>
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, maxHeight: '120px', overflowY: 'auto' }}>
                    {countryCityMap[form.country || 'India'].map((city) => (
                      <button
                        key={city}
                        type="button"
                        onClick={() => {
                          setCustomCity(false);
                          update('city', city);
                          update('location', `${city}, ${form.country || 'India'}`);
                        }}
                        style={{
                          fontSize: '0.78rem',
                          padding: '3px 10px',
                          borderRadius: '12px',
                          border: form.city === city && !customCity ? '1px solid var(--accent, #e34d42)' : '1px solid var(--color-border, #e1e4e8)',
                          background: form.city === city && !customCity ? 'var(--accent-tint, rgba(227, 77, 66, 0.1))' : 'var(--color-surface, #fff)',
                          color: form.city === city && !customCity ? 'var(--accent, #e34d42)' : 'inherit',
                          cursor: 'pointer',
                          fontWeight: form.city === city && !customCity ? 600 : 400,
                        }}
                      >
                        {city}
                      </button>
                    ))}
                  </div>
                </div>
              )}
              <SelectField
                label="Primary role"
                value={form.role}
                onChange={(v) => update('role', v)}
                options={[
                  'Student',
                  'Professional',
                  'Founder',
                  'Mentor',
                  'Creator',
                  'Investor',
                ]}
              />
              <TextField
                label="Skills"
                value={form.skills}
                onChange={(v) => update('skills', v)}
                placeholder="e.g. Product design, React"
              />
              <TextField
                label="Interests"
                value={form.interests}
                onChange={(v) => update('interests', v)}
                placeholder="e.g. Learning, startups, community"
              />
              {error && <Error text={error} />}
              <div className="auth-actions">
                <button
                  className="auth-secondary"
                  type="button"
                  onClick={() => setStep(0)}
                >
                  Back
                </button>
                <Primary disabled={submitting}>
                  {submitting ? 'Creating account…' : <>Enter Collobster <ArrowRight size={17} /></>}
                </Primary>
              </div>
            </form>
          )}

          {mode !== 'verify' && step === 0 && (
            <>
              <div className="auth-divider">
                <span>or continue with</span>
              </div>
              <div className="oauth-row">
                {oauth.map(([name, Icon]) => (
                  <button
                    key={name}
                    type="button"
                    onClick={() => oauthStart(name)}
                    aria-label={`Continue with ${name}`}
                  >
                    {typeof Icon === 'string' ? <b>{Icon}</b> : <Icon size={18} />}
                    <span>{name}</span>
                  </button>
                ))}
              </div>
            </>
          )}

          <p className="auth-switch">
            {mode === 'verify' ? (
              <button
                type="button"
                onClick={() => {
                  setMode('signin');
                  setError('');
                }}
              >
                Back to Sign in
              </button>
            ) : (
              <>
                {signup ? 'Already have an account?' : 'New to Collobster?'}{' '}
                <button
                  type="button"
                  onClick={() => {
                    setMode(signup ? 'signin' : 'signup');
                    setStep(0);
                    setError('');
                  }}
                >
                  {signup ? 'Sign in' : 'Create an account'}
                </button>
              </>
            )}
          </p>
        </div>
      </section>
    </main>
  );
}
