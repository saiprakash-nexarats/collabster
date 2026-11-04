import React, { useState, useEffect } from 'react';
import {
  Lightbulb,
  FileCheck,
  Building2,
  CheckCircle2,
  ArrowRight,
  TrendingUp,
  Users,
  Briefcase,
  DollarSign,
  Compass,
  Scale,
  Landmark,
  Palette,
  Globe,
  Rocket,
  Plus,
  Clock,
  ExternalLink,
  ChevronRight,
  Check,
  Search,
  FolderOpen,
  MessageSquare,
  ShieldCheck,
  Award,
  Sparkles,
  HelpCircle,
  FileText,
  BarChart3,
  X,
  UserPlus,
  Settings,
  Mail,
  Send,
  Zap,
} from 'lucide-react';
import { useApp } from '../context';
import { companies, people } from '../data';
import {
  Avatar,
  Button,
  IconButton,
  Tag,
  Tabs,
  PageHeader,
  SectionTitle,
  Progress,
  Dialog,
} from '../components';
import './company-team.css';
import './company-flow.css';
import {
  useStage1Data,
  OverviewHeaderCard,
  OverviewActionsCard,
  OverviewHealthCard,
  OverviewMilestonesCard,
  IdeaManagementPanelEnhanced,
  MarketResearchPanelEnhanced,
  ValidationPanelEnhanced,
  MVPPlanningPanelEnhanced,
  BusinessModelPanelEnhanced,
  PeopleTeamPanelEnhanced,
  FundingPrepPanelEnhanced,
  CompanyTasksPanelEnhanced,
  CompanyDocumentsPanelEnhanced,
  CompanyDiscussionsPanelEnhanced,
} from './CompanyStage1Panels';

export function CompanyRedesign({
  CompanyTeamPanel,
  TaskBoard,
  CompanyDocuments,
  Hiring,
  FundingCreator,
  Products,
  Roadmap,
  Updates,
  OperatorAnalytics,
}) {
  const app = useApp();

  // Top-level mode: 'Company workspace' | 'Discover'
  const [mode, setMode] = useState('Company workspace');
  // Active stage within Company workspace: 'stage1' | 'stage2' | 'stage3'
  const [activeStage, setActiveStage] = useState('stage1');
  const [activeNav, setActiveNav] = useState('Overview');
  const [activeModal, setActiveModal] = useState(null); // 'stage1-onboarding' | 'stage2-onboarding' | 'stage3-onboarding' | null
  const [modalStep, setModalStep] = useState(1);

  // Stage 1 Company State (Clean Guided Workspace by Default)
  const [ideaData, setIdeaData] = useState({
    name: 'Untitled Idea Workspace',
    description: 'A new guided venture workspace ready for problem framing, validation, and MVP planning.',
    industry: 'Technology',
    goals: ['Frame core problem hypothesis', 'Conduct customer interviews', 'Validate market demand', 'Plan MVP specification'],
    health: 0,
    progress: 0,
    status: 'draft', // 'draft' | 'ready_for_review' | 'submitted' | 'approved' | 'registered'
  });

  // Stage 2 Company State
  const [formationData, setFormationData] = useState({
    name: '',
    industry: 'Technology',
    structure: 'Private Limited',
    progress: 0,
    registrationStep: 1,
    status: 'draft',
    founders: [],
  });

  // Stage 3 Active Company State
  const [activeCompanyData, setActiveCompanyData] = useState({
    name: '',
    status: 'draft',
    teamMembers: 1,
    openJobs: 0,
    activeProjects: 0,
    revenueYTD: '$0',
    growthRate: '0%',
  });

  // Interactive Checklist States
  const [stage1Actions, setStage1Actions] = useState([
    { id: 'a1', label: 'Complete customer interviews', done: false },
    { id: 'a2', label: 'Analyze competitor research', done: false },
    { id: 'a3', label: 'Build MVP prototype', done: false },
    { id: 'a4', label: 'Draft product validation survey', done: false },
  ]);

  const [stage1Milestones, setStage1Milestones] = useState([
    { id: 'm1', label: 'Idea defined', done: false },
    { id: 'm2', label: 'Market research', done: false },
    { id: 'm3', label: 'Validation', done: false },
    { id: 'm4', label: 'MVP planning', done: false },
  ]);

  const [stage1Criteria, setStage1Criteria] = useState({
    ideaDefined: false,
    marketValidated: false,
    mvpPlanned: false,
    initialTeamIdentified: false,
    fundingPrepDone: false,
  });

  const [stage2Actions, setStage2Actions] = useState([
    { id: 'sa1', label: 'Submit incorporation documents', done: false },
    { id: 'sa2', label: 'Set up business bank account', done: false },
    { id: 'sa3', label: 'Apply for tax registration', done: false },
    { id: 'sa4', label: 'Execute founder vesting agreement', done: false },
  ]);

  const [stage2Criteria, setStage2Criteria] = useState({
    companyRegistered: false,
    legalComplianceComplete: false,
    bankingFinanceSetup: false,
    brandDigitalReady: false,
    launchPrepared: false,
  });

  const loadSampleEcoCartData = () => {
    setIdeaData({
      name: 'EcoCart',
      description: 'Sustainable e-commerce for local businesses',
      industry: 'E-commerce & Climate Tech',
      goals: ['Validate customer willingness to pay', 'Recruit technical co-founder', 'Build low-code MVP prototype', 'Secure 10 pilot vendor commitments'],
      health: 72,
      progress: 60,
      status: 'ready_for_review',
    });
    setFormationData({
      name: 'EcoCart Technologies',
      industry: 'E-commerce & Climate Tech',
      structure: 'Private Limited',
      progress: 70,
      registrationStep: 3,
      status: 'submitted',
      founders: [
        { id: 'f1', name: 'Mohit Koppera', role: 'Founder' },
        { id: 'f2', name: 'Priya Sharma', role: 'Co-founder' },
      ],
    });
    setActiveCompanyData({
      name: 'EcoCart Technologies',
      status: 'Active Company',
      teamMembers: 24,
      openJobs: 5,
      activeProjects: 8,
      revenueYTD: '$120K',
      growthRate: '+32%',
    });
    setStage1Actions([
      { id: 'a1', label: 'Complete customer interviews', done: true },
      { id: 'a2', label: 'Analyze competitor research', done: true },
      { id: 'a3', label: 'Build MVP prototype', done: false },
      { id: 'a4', label: 'Draft product validation survey', done: false },
    ]);
    setStage1Milestones([
      { id: 'm1', label: 'Idea defined', done: true },
      { id: 'm2', label: 'Market research', done: true },
      { id: 'm3', label: 'Validation', done: true },
      { id: 'm4', label: 'MVP planning', done: true },
    ]);
    setStage1Criteria({
      ideaDefined: true,
      marketValidated: true,
      mvpPlanned: true,
      initialTeamIdentified: true,
      fundingPrepDone: false,
    });
    setStage2Actions([
      { id: 'sa1', label: 'Submit incorporation documents', done: true },
      { id: 'sa2', label: 'Set up business bank account', done: true },
      { id: 'sa3', label: 'Apply for tax registration', done: true },
      { id: 'sa4', label: 'Execute founder vesting agreement', done: true },
    ]);
    setStage2Criteria({
      companyRegistered: true,
      legalComplianceComplete: true,
      bankingFinanceSetup: true,
      brandDigitalReady: true,
      launchPrepared: false,
    });
    app.toast('Loaded EcoCart sample company workspace!');
  };

  const stage1RequirementsMet = Boolean(
    stage1Criteria.ideaDefined &&
    stage1Criteria.marketValidated &&
    stage1Criteria.mvpPlanned
  );

  const stage2RequirementsMet = Boolean(
    stage2Criteria.companyRegistered &&
    stage2Criteria.legalComplianceComplete &&
    stage2Criteria.bankingFinanceSetup
  );

  const [gateBlockedModal, setGateBlockedModal] = useState(null);

  // Move between stages with strict gate validation
  const moveToStage = (stage) => {
    if (stage === 'stage2' && !stage1RequirementsMet) {
      setGateBlockedModal({
        targetStage: 'Stage 2: Formation / Launch',
        reason: 'Stage 1 validation criteria must be completed before entering Stage 2 (Company Formation).',
        requirements: [
          { name: 'Idea Defined & Problem Hypothesis Framed', met: stage1Criteria.ideaDefined, nav: 'Idea' },
          { name: 'Customer & Market Demand Validated', met: stage1Criteria.marketValidated, nav: 'Validation' },
          { name: 'MVP Specification & Architecture Planned', met: stage1Criteria.mvpPlanned, nav: 'MVP' },
        ],
      });
      return;
    }
    if (stage === 'stage3' && !stage2RequirementsMet) {
      setGateBlockedModal({
        targetStage: 'Stage 3: Active Company',
        reason: 'Stage 2 statutory and legal prerequisites must be completed before active company operations.',
        requirements: [
          { name: 'Company Registration / Incorporation', met: stage2Criteria.companyRegistered, nav: 'Registration' },
          { name: 'Legal Compliance & Founder Agreements Executed', met: stage2Criteria.legalComplianceComplete, nav: 'Legal' },
          { name: 'Business Banking & Capital Account Verified', met: stage2Criteria.bankingFinanceSetup, nav: 'Finance' },
        ],
      });
      return;
    }
    setActiveStage(stage);
    setActiveNav('Overview');
    app.toast(
      stage === 'stage2'
        ? 'Advanced to Stage 2: Formation / Launch workspace'
        : stage === 'stage3'
        ? 'Advanced to Stage 3: Active Company workspace'
        : 'Navigated to Stage 1: Idea / Build'
    );
  };

  const openOnboarding = (stageModal) => {
    setActiveModal(stageModal);
    setModalStep(1);
  };

  const closeOnboarding = () => {
    setActiveModal(null);
  };

  // Roles & updates for shared components
  const [roles, setRoles] = useState([
    { title: 'Frontend developer', applicants: 12 },
    { title: 'Product designer', applicants: 8 },
    { title: 'Growth lead', applicants: 5 },
  ]);
  const [updates, setUpdates] = useState([
    'EcoCart platform milestone achieved: 12 vendor partner agreements verified.',
  ]);

  return (
    <main className="operator-space" id="company-flow-container">
      {/* Top Header */}
      <PageHeader
        eyebrow="COMPANY LIFECYCLE"
        title={
          mode === 'Discover'
            ? 'Discover companies, follow their progress, or create a place for your own team.'
            : activeStage === 'stage1'
            ? 'Idea / Build — Turn an idea into a validated business opportunity'
            : activeStage === 'stage2'
            ? 'Formation / Launch — Turn the validated idea into a legally and operationally launch-ready company'
            : 'Active Company — Operate, manage and grow the company'
        }
        description={
          mode === 'Discover'
            ? 'Discover community companies, follow builder progress, or start your own company journey.'
            : activeStage === 'stage1'
            ? 'Define your problem, validate user demand, build your MVP, and prepare for formal company formation.'
            : activeStage === 'stage2'
            ? 'Execute company registration, legal compliance, banking, brand identity, and prepare for your market launch.'
            : 'Manage hiring, run projects, communicate with your team, track revenue, and scale operations.'
        }
      >
        <Tabs
          items={['Company workspace', 'Discover']}
          value={mode}
          onChange={setMode}
        />
      </PageHeader>

      {/* Statutory Disclosure Banner & Process Tracker Bar */}
      {mode === 'Company workspace' && (
        <>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '12px',
            background: 'var(--surface, #ffffff)',
            border: '1px solid var(--line, #e2e8f0)',
            borderLeft: '4px solid #6366f1',
            borderRadius: '8px',
            padding: '10px 16px',
            marginBottom: '16px',
            fontSize: '13px'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '16px' }}>⚖️</span>
              <div>
                <strong style={{ color: 'var(--ink)' }}>Internal Workspace Record</strong>
                <span style={{ color: 'var(--muted)', marginLeft: '6px' }}>
                  Collobster workspaces are operational collaboration tools and do not constitute an official statutory filing until formally registered with government authorities.
                </span>
              </div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexShrink: 0 }}>
              {ideaData.progress === 0 && (
                <button
                  type="button"
                  className="edit-pill-btn"
                  onClick={loadSampleEcoCartData}
                  style={{ fontSize: '11px', padding: '3px 8px' }}
                >
                  Load EcoCart Sample
                </button>
              )}
              <span style={{ fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--muted)' }}>Status:</span>
              <span style={{
                padding: '3px 8px',
                borderRadius: '6px',
                fontWeight: 600,
                fontSize: '12px',
                background: (activeStage === 'stage3' ? activeCompanyData.status : activeStage === 'stage2' ? formationData.status : ideaData.status) === 'registered' ? '#dcfce7' : '#fef3c7',
                color: (activeStage === 'stage3' ? activeCompanyData.status : activeStage === 'stage2' ? formationData.status : ideaData.status) === 'registered' ? '#15803d' : '#b45309'
              }}>
                {activeStage === 'stage3' ? (activeCompanyData.status || 'Active') : activeStage === 'stage2' ? (formationData.status || 'Draft') : (ideaData.status || 'Draft')}
              </span>
            </div>
          </div>

          <div className="stage-navigator-wrapper">
            <div className="stage-tabs-row">
              {/* Stage 1 Pill */}
              <div
                className={`stage-tab-btn ${activeStage === 'stage1' ? 'active-stage1' : ''}`}
                onClick={() => { setActiveStage('stage1'); setActiveNav('Overview'); }}
                role="button"
                tabIndex={0}
                aria-label="Switch to Stage 1 Idea / Build"
              >
                <div className="stage-tab-icon stage1">💡</div>
                <div className="stage-tab-text">
                  <span className="stage-tab-tag stage1">Stage 1</span>
                  <span className="stage-tab-title">Idea / Build</span>
                  <span className="stage-tab-sub">Turn idea into validated opportunity</span>
                </div>
              </div>

              {/* Stage 2 Pill (Gated) */}
              <div
                className={`stage-tab-btn ${activeStage === 'stage2' ? 'active-stage2' : ''}`}
                onClick={() => moveToStage('stage2')}
                role="button"
                tabIndex={0}
                aria-label="Switch to Stage 2 Formation / Launch"
              >
                <div className="stage-tab-icon stage2">📄</div>
                <div className="stage-tab-text">
                  <span className="stage-tab-tag stage2">Stage 2 {!stage1RequirementsMet ? '🔒 Gated' : '✓ Unlocked'}</span>
                  <span className="stage-tab-title">Formation / Launch</span>
                  <span className="stage-tab-sub">Legally and launch-ready company</span>
                </div>
              </div>

              {/* Stage 3 Pill (Gated) */}
              <div
                className={`stage-tab-btn ${activeStage === 'stage3' ? 'active-stage3' : ''}`}
                onClick={() => moveToStage('stage3')}
                role="button"
                tabIndex={0}
                aria-label="Switch to Stage 3 Active Company"
              >
                <div className="stage-tab-icon stage3">🏢</div>
                <div className="stage-tab-text">
                  <span className="stage-tab-tag stage3">Stage 3 {!stage2RequirementsMet ? '🔒 Gated' : '✓ Unlocked'}</span>
                  <span className="stage-tab-title">Active Company</span>
                  <span className="stage-tab-sub">Operate, manage and grow</span>
                </div>
              </div>
            </div>

          {/* Process Flow Indicator */}
          <div className="stage-process-tracker">
            {activeStage === 'stage1' && (
              <>
                <span className="stage-process-step completed"><CheckCircle2 size={14} /> 1. Create Idea</span>
                <span className="stage-process-arrow">→</span>
                <span className="stage-process-step completed"><CheckCircle2 size={14} /> 2. Research</span>
                <span className="stage-process-arrow">→</span>
                <span className="stage-process-step active">3. Validate</span>
                <span className="stage-process-arrow">→</span>
                <span className="stage-process-step">4. Plan</span>
                <span className="stage-process-arrow">→</span>
                <span className="stage-process-step">5. Prepare</span>
                <button
                  type="button"
                  className="stage-process-next-stage"
                  onClick={() => moveToStage('stage2')}
                >
                  Move to Stage 2 →
                </button>
              </>
            )}

            {activeStage === 'stage2' && (
              <>
                <span className="stage-process-step completed"><CheckCircle2 size={14} /> 1. Set Company Details</span>
                <span className="stage-process-arrow">→</span>
                <span className="stage-process-step completed"><CheckCircle2 size={14} /> 2. Register</span>
                <span className="stage-process-arrow">→</span>
                <span className="stage-process-step active">3. Legal & Finance</span>
                <span className="stage-process-arrow">→</span>
                <span className="stage-process-step">4. Brand & Launch</span>
                <span className="stage-process-arrow">→</span>
                <span className="stage-process-step">5. Go Live</span>
                <button
                  type="button"
                  className="stage-process-next-stage"
                  onClick={() => moveToStage('stage3')}
                >
                  Move to Stage 3 →
                </button>
              </>
            )}

            {activeStage === 'stage3' && (
              <>
                <span className="stage-process-step completed"><CheckCircle2 size={14} /> 1. Set Up Organization</span>
                <span className="stage-process-arrow">→</span>
                <span className="stage-process-step completed"><CheckCircle2 size={14} /> 2. Build Team</span>
                <span className="stage-process-arrow">→</span>
                <span className="stage-process-step active">3. Hire & Operate</span>
                <span className="stage-process-arrow">→</span>
                <span className="stage-process-step active">4. Grow Business</span>
                <span className="stage-process-next-stage" style={{ color: '#059669', background: '#ecfdf5' }}>
                  ✓ Continuous Growth
                </span>
              </>
            )}
          </div>
        </div>
        </>
      )}

      {/* Main Stage Views */}
      {mode === 'Discover' ? (
        <DiscoverCompaniesView app={app} onStart={() => openOnboarding('stage1-onboarding')} />
      ) : activeStage === 'stage1' ? (
        <Stage1Workspace
          app={app}
          ideaData={ideaData}
          setIdeaData={setIdeaData}
          activeNav={activeNav}
          setActiveNav={setActiveNav}
          stage1Actions={stage1Actions}
          setStage1Actions={setStage1Actions}
          stage1Milestones={stage1Milestones}
          setStage1Milestones={setStage1Milestones}
          stage1Criteria={stage1Criteria}
          setStage1Criteria={setStage1Criteria}
          onMoveToStage2={() => moveToStage('stage2')}
          onOpenOnboarding={() => openOnboarding('stage1-onboarding')}
          CompanyTeamPanel={CompanyTeamPanel}
          TaskBoard={TaskBoard}
          CompanyDocuments={CompanyDocuments}
        />
      ) : activeStage === 'stage2' ? (
        <Stage2Workspace
          app={app}
          formationData={formationData}
          setFormationData={setFormationData}
          activeNav={activeNav}
          setActiveNav={setActiveNav}
          stage2Actions={stage2Actions}
          setStage2Actions={setStage2Actions}
          stage2Criteria={stage2Criteria}
          setStage2Criteria={setStage2Criteria}
          onMoveToStage3={() => moveToStage('stage3')}
          onOpenOnboarding={() => openOnboarding('stage2-onboarding')}
          CompanyTeamPanel={CompanyTeamPanel}
          TaskBoard={TaskBoard}
          CompanyDocuments={CompanyDocuments}
        />
      ) : (
        <Stage3Workspace
          app={app}
          companyData={activeCompanyData}
          setCompanyData={setActiveCompanyData}
          activeNav={activeNav}
          setActiveNav={setActiveNav}
          onOpenOnboarding={() => openOnboarding('stage3-onboarding')}
          CompanyTeamPanel={CompanyTeamPanel}
          TaskBoard={TaskBoard}
          CompanyDocuments={CompanyDocuments}
          Hiring={Hiring}
          roles={roles}
          setRoles={setRoles}
          FundingCreator={FundingCreator}
          Products={Products}
          Roadmap={Roadmap}
          Updates={Updates}
          updates={updates}
          setUpdates={setUpdates}
          OperatorAnalytics={OperatorAnalytics}
        />
      )}

      {/* Onboarding Modals */}
      {activeModal === 'stage1-onboarding' && (
        <Stage1OnboardingModal
          step={modalStep}
          setStep={setModalStep}
          onClose={closeOnboarding}
          ideaData={ideaData}
          setIdeaData={setIdeaData}
          onComplete={() => {
            closeOnboarding();
            setActiveStage('stage1');
            setActiveNav('Overview');
            app.toast('Idea workspace created! Welcome to Stage 1.');
          }}
        />
      )}

      {activeModal === 'stage2-onboarding' && (
        <Stage2OnboardingModal
          step={modalStep}
          setStep={setModalStep}
          onClose={closeOnboarding}
          formationData={formationData}
          setFormationData={setFormationData}
          onComplete={() => {
            closeOnboarding();
            setActiveStage('stage2');
            setActiveNav('Overview');
            app.toast('Formation workspace created! Welcome to Stage 2.');
          }}
        />
      )}

      {activeModal === 'stage3-onboarding' && (
        <Stage3OnboardingModal
          step={modalStep}
          setStep={setModalStep}
          onClose={closeOnboarding}
          companyData={activeCompanyData}
          setCompanyData={setActiveCompanyData}
          onComplete={() => {
            closeOnboarding();
            setActiveStage('stage3');
            setActiveNav('Overview');
            app.toast('Active company workspace ready! Welcome to Stage 3.');
          }}
        />
      )}

      {gateBlockedModal && (
        <Dialog
          title={`🔒 ${gateBlockedModal.targetStage} Locked`}
          onClose={() => setGateBlockedModal(null)}
        >
          <div style={{ padding: '8px 0' }}>
            <p style={{ margin: '0 0 14px', fontSize: '14px', color: 'var(--ink)' }}>
              {gateBlockedModal.reason}
            </p>
            <div style={{ background: 'var(--soft, #f8fafc)', borderRadius: '8px', padding: '12px', marginBottom: '16px' }}>
              <div style={{ fontSize: '12px', fontWeight: 600, color: 'var(--muted)', marginBottom: '8px', textTransform: 'uppercase' }}>
                Prerequisite Checklist:
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {gateBlockedModal.requirements.map((req) => (
                  <div key={req.name} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', fontSize: '13px' }}>
                    <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span>{req.met ? '✅' : '❌'}</span>
                      <span style={{ color: req.met ? 'var(--ink)' : '#dc2626', fontWeight: req.met ? 400 : 600 }}>
                        {req.name}
                      </span>
                    </span>
                    {!req.met && req.nav && (
                      <button
                        type="button"
                        className="edit-pill-btn"
                        style={{ fontSize: '11px', padding: '2px 8px' }}
                        onClick={() => {
                          setActiveNav(req.nav);
                          setGateBlockedModal(null);
                        }}
                      >
                        Go to {req.nav} →
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '10px' }}>
              <Button
                variant="ghost"
                onClick={() => {
                  loadSampleEcoCartData();
                  setGateBlockedModal(null);
                }}
              >
                Load EcoCart Sample (Unlock)
              </Button>
              <Button
                variant="primary"
                onClick={() => setGateBlockedModal(null)}
              >
                Understood
              </Button>
            </div>
          </div>
        </Dialog>
      )}
    </main>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// STAGE 1 WORKSPACE: IDEA / BUILD
// ─────────────────────────────────────────────────────────────────────────────
function Stage1Workspace({
  app,
  ideaData,
  setIdeaData,
  activeNav,
  setActiveNav,
  stage1Actions,
  setStage1Actions,
  stage1Milestones,
  setStage1Milestones,
  stage1Criteria,
  setStage1Criteria,
  onMoveToStage2,
  onOpenOnboarding,
  CompanyTeamPanel,
  TaskBoard,
  CompanyDocuments,
}) {
  const stage1 = useStage1Data(app);
  const [editingOverview, setEditingOverview] = useState(false);
  const [adjustingHealth, setAdjustingHealth] = useState(false);
  const [overviewForm, setOverviewForm] = useState(stage1.data.overview);
  const [healthForm, setHealthForm] = useState(stage1.data.overview.health);

  useEffect(() => {
    setOverviewForm(stage1.data.overview);
    setHealthForm(stage1.data.overview.health);
  }, [stage1.data.overview]);

  useEffect(() => {
    if (ideaData?.name && ideaData.name !== stage1.data.overview.name) {
      stage1.save({
        overview: {
          ...stage1.data.overview,
          name: ideaData.name,
          description: ideaData.description || stage1.data.overview.description,
          industry: ideaData.industry || stage1.data.overview.industry,
        },
      });
    }
  }, [ideaData?.name, ideaData?.description, ideaData?.industry]);

  const navItems = [
    { id: 'Overview', label: 'Overview', icon: BarChart3 },
    { id: 'Idea', label: 'Idea', icon: Lightbulb },
    { id: 'Market Research', label: 'Market Research', icon: Compass },
    { id: 'Validation', label: 'Validation', icon: CheckCircle2 },
    { id: 'MVP', label: 'MVP', icon: Rocket },
    { id: 'Business Model', label: 'Business Model', icon: Briefcase },
    { id: 'Team', label: 'People & Team', icon: Users },
    { id: 'Funding', label: 'Funding', icon: DollarSign },
    { id: 'Tasks', label: 'Tasks', icon: FileCheck },
    { id: 'Documents', label: 'Documents', icon: FileText },
    { id: 'Discussions', label: 'Discussions', icon: MessageSquare },
  ];

  const keyFeatureFlows = [
    {
      num: 1,
      title: 'Idea Management',
      items: ['Create & edit idea', 'Problem statement', 'Solution definition', 'Target audience', 'Value proposition', 'Industry & category', 'Tags and keywords'],
      nav: 'Idea',
    },
    {
      num: 2,
      title: 'Market Research',
      items: ['Market size and trends', 'Competitor analysis', 'Customer segmentation', 'Save research insights', 'Industry reports', 'AI powered insights', 'SWOT analysis'],
      nav: 'Market Research',
    },
    {
      num: 3,
      title: 'Validation',
      items: ['Create surveys', 'Conduct interviews', 'Collect feedback', 'Validate assumptions', 'Track validation metrics', 'User personas', 'Validation summary'],
      nav: 'Validation',
    },
    {
      num: 4,
      title: 'MVP Planning',
      items: ['Define MVP features', 'Prioritize roadmap', 'Create wireframes', 'Track development tasks', 'Set milestones', 'Prototype links', 'Progress tracking'],
      nav: 'MVP',
    },
    {
      num: 5,
      title: 'Business Model',
      items: ['Revenue model', 'Pricing strategy', 'Cost structure', 'Business model canvas', 'Go-to-market strategy', 'Financial assumptions', 'Unit economics'],
      nav: 'Business Model',
    },
    {
      num: 6,
      title: 'People & Team',
      items: ['Find co-founders', 'Invite collaborators', 'Find mentors/advisors', 'Build initial team', 'Manage requests', 'Roles and interests', 'Team discussion space'],
      nav: 'Team',
    },
    {
      num: 7,
      title: 'Funding Preparation',
      items: ['Create pitch deck', 'Define funding needs', 'Find investors', 'Track investor interactions', 'Financial projections', 'Funding readiness score', 'Investor feedback'],
      nav: 'Funding',
    },
    {
      num: 8,
      title: 'Tasks & Milestones',
      items: ['Create tasks', 'Set milestones', 'Track progress', 'Assign team members', 'Set deadlines', 'Progress visualization', 'Reminders & notifications'],
      nav: 'Tasks',
    },
    {
      num: 9,
      title: 'Documents & Notes',
      items: ['Idea notes', 'Research documents', 'MVP documents', 'Pitch deck', 'Save links and resources', 'Organize with folders', 'Version history'],
      nav: 'Documents',
    },
  ];

  const handleSaveOverview = (e) => {
    e.preventDefault();
    const updated = {
      ...stage1.data.overview,
      name: overviewForm.name,
      description: overviewForm.description,
      industry: overviewForm.industry,
      progress: Number(overviewForm.progress),
      health: Number(overviewForm.health),
      stage: overviewForm.stage,
    };
    stage1.save({ overview: updated });
    setIdeaData((prev) => ({
      ...prev,
      name: overviewForm.name,
      description: overviewForm.description,
      industry: overviewForm.industry,
      progress: Number(overviewForm.progress),
      health: Number(overviewForm.health),
    }));
    setEditingOverview(false);
    app.toast('Company overview updated successfully!');
  };

  const handleSaveHealth = (e) => {
    e.preventDefault();
    const h = Number(healthForm);
    const updated = { ...stage1.data.overview, health: h };
    stage1.save({ overview: updated });
    setIdeaData((prev) => ({ ...prev, health: h }));
    setAdjustingHealth(false);
    app.toast('Idea health score updated!');
  };

  const toggleAction = (id) => {
    const nextList = (stage1.data.overview.actions || []).map((item) =>
      item.id === id ? { ...item, done: !item.done } : item
    );
    stage1.save({ overview: { ...stage1.data.overview, actions: nextList } });
  };

  const addAction = (label) => {
    const newAct = {
      id: 'a_' + Date.now(),
      label,
      done: false,
      priority: 'Medium',
    };
    stage1.save({
      overview: {
        ...stage1.data.overview,
        actions: [...(stage1.data.overview.actions || []), newAct],
      },
    });
    app.toast('Action added.');
  };

  const editAction = (action) => {
    const nextList = (stage1.data.overview.actions || []).map((item) =>
      item.id === action.id ? action : item
    );
    stage1.save({ overview: { ...stage1.data.overview, actions: nextList } });
    app.toast('Action updated.');
  };

  const deleteAction = (id) => {
    const nextList = (stage1.data.overview.actions || []).filter((item) => item.id !== id);
    stage1.save({ overview: { ...stage1.data.overview, actions: nextList } });
    app.toast('Action removed.');
  };

  const toggleMilestone = (id) => {
    const nextList = (stage1.data.overview.milestones || []).map((item) =>
      item.id === id ? { ...item, done: !item.done } : item
    );
    stage1.save({ overview: { ...stage1.data.overview, milestones: nextList } });
  };

  const addMilestone = (label) => {
    const newMilestone = {
      id: 'm_' + Date.now(),
      label,
      done: false,
      targetDate: '',
    };
    stage1.save({
      overview: {
        ...stage1.data.overview,
        milestones: [...(stage1.data.overview.milestones || []), newMilestone],
      },
    });
    app.toast('Milestone added.');
  };

  const editMilestone = (m) => {
    const nextList = (stage1.data.overview.milestones || []).map((item) =>
      item.id === m.id ? m : item
    );
    stage1.save({ overview: { ...stage1.data.overview, milestones: nextList } });
    app.toast('Milestone updated.');
  };

  const deleteMilestone = (id) => {
    const nextList = (stage1.data.overview.milestones || []).filter((item) => item.id !== id);
    stage1.save({ overview: { ...stage1.data.overview, milestones: nextList } });
    app.toast('Milestone removed.');
  };

  const toggleCriterion = (key) => {
    const current = stage1.data.overview.criteria || {};
    stage1.save({
      overview: {
        ...stage1.data.overview,
        criteria: { ...current, [key]: !current[key] },
      },
    });
  };

  const criteria = stage1.data.overview.criteria || {};

  return (
    <div className="company-stage-layout">
      {/* Sidebar Navigation */}
      <nav className="company-sidebar-nav" aria-label="Stage 1 navigation">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <button
              key={item.id}
              type="button"
              className={`company-nav-item ${activeNav === item.id ? 'active' : ''}`}
              onClick={() => setActiveNav(item.id)}
            >
              <Icon size={16} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Main Workspace Body */}
      <div className="stage-content-body">
        <div style={{ marginBottom: '14px', overflowX: 'auto' }}>
          <Tabs
            items={navItems.map((n) => n.id)}
            value={activeNav}
            onChange={setActiveNav}
          />
        </div>

        {/* View Switcher */}
        {activeNav === 'Overview' && (
          <>
            {/* Stage 1 Header Card with Edit & Reset Defaults */}
            <OverviewHeaderCard
              overview={{
                ...stage1.data.overview,
                name: ideaData?.name || stage1.data.overview.name,
                description: ideaData?.description || stage1.data.overview.description,
                industry: ideaData?.industry || stage1.data.overview.industry,
              }}
              onEdit={() => setEditingOverview(true)}
              onOpenOnboarding={onOpenOnboarding}
              onResetDefaults={stage1.resetToDefaults}
            />

            {/* 3 Overview Widgets */}
            <div className="dashboard-grid-3">
              <OverviewActionsCard
                actions={stage1.data.overview.actions || []}
                onToggle={toggleAction}
                onAdd={addAction}
                onEdit={editAction}
                onDelete={deleteAction}
              />

              <OverviewHealthCard
                health={stage1.data.overview.health}
                onEditHealth={() => setAdjustingHealth(true)}
              />

              <OverviewMilestonesCard
                milestones={stage1.data.overview.milestones || []}
                onToggle={toggleMilestone}
                onAdd={addMilestone}
                onEdit={editMilestone}
                onDelete={deleteMilestone}
              />
            </div>

            {/* 3. Key Feature Flows in Stage 1 */}
            <section className="key-features-section">
              <h3 className="key-features-heading">
                <span>3. Key Feature Flows in Stage 1</span>
              </h3>
              <div className="key-features-grid">
                {keyFeatureFlows.map((flow) => (
                  <div
                    key={flow.num}
                    className="feature-flow-card stage1"
                    onClick={() => setActiveNav(flow.nav)}
                    role="button"
                    tabIndex={0}
                  >
                    <div className="feature-card-header">
                      <span className="feature-card-num">{flow.num}</span>
                      <h4 className="feature-card-title">{flow.title}</h4>
                    </div>
                    <ul className="feature-card-items">
                      {flow.items.map((item, idx) => (
                        <li key={idx}>{item}</li>
                      ))}
                    </ul>
                    <div className="feature-card-action">
                      <span>Open {flow.title}</span>
                      <ChevronRight size={14} />
                    </div>
                  </div>
                ))}
              </div>
            </section>

            {/* 4. Stage 1 Completion Criteria */}
            <div className="completion-criteria-card stage1">
              <div>
                <h4 style={{ margin: '0 0 10px', fontSize: '15px', color: '#78350f' }}>
                  4. Stage 1 Completion Criteria
                </h4>
                <div className="completion-checklist-items">
                  <label className={`completion-pill ${criteria.ideaDefined ? 'checked' : ''}`}>
                    <input
                      type="checkbox"
                      checked={!!criteria.ideaDefined}
                      onChange={() => toggleCriterion('ideaDefined')}
                    />
                    <span>Idea well defined</span>
                  </label>
                  <label className={`completion-pill ${criteria.marketValidated ? 'checked' : ''}`}>
                    <input
                      type="checkbox"
                      checked={!!criteria.marketValidated}
                      onChange={() => toggleCriterion('marketValidated')}
                    />
                    <span>Market validated</span>
                  </label>
                  <label className={`completion-pill ${criteria.mvpPlanned ? 'checked' : ''}`}>
                    <input
                      type="checkbox"
                      checked={!!criteria.mvpPlanned}
                      onChange={() => toggleCriterion('mvpPlanned')}
                    />
                    <span>MVP planned</span>
                  </label>
                  <label className={`completion-pill ${criteria.initialTeamIdentified ? 'checked' : ''}`}>
                    <input
                      type="checkbox"
                      checked={!!criteria.initialTeamIdentified}
                      onChange={() => toggleCriterion('initialTeamIdentified')}
                    />
                    <span>Initial team identified</span>
                  </label>
                  <label className={`completion-pill ${criteria.fundingPrepDone ? 'checked' : ''}`}>
                    <input
                      type="checkbox"
                      checked={!!criteria.fundingPrepDone}
                      onChange={() => toggleCriterion('fundingPrepDone')}
                    />
                    <span>Funding preparation done</span>
                  </label>
                </div>
              </div>

              <button
                type="button"
                className="stage-transition-btn stage1-btn"
                onClick={onMoveToStage2}
              >
                <span>Move to Stage 2 (Formation / Launch)</span>
                <ArrowRight size={16} />
              </button>
            </div>
          </>
        )}

        {/* Dedicated Tab Views (All with Full Interactive CRUD) */}
        {activeNav === 'Idea' && (
          <IdeaManagementPanelEnhanced
            idea={stage1.data.idea}
            onSave={(next) => {
              stage1.save({ idea: next });
              setIdeaData((prev) => ({ ...prev, name: next.name, industry: next.industry }));
            }}
            app={app}
          />
        )}
        {activeNav === 'Market Research' && (
          <MarketResearchPanelEnhanced
            marketResearch={stage1.data.marketResearch}
            onSave={(next) => stage1.save({ marketResearch: next })}
            app={app}
          />
        )}
        {activeNav === 'Validation' && (
          <ValidationPanelEnhanced
            validation={stage1.data.validation}
            onSave={(next) => stage1.save({ validation: next })}
            app={app}
          />
        )}
        {activeNav === 'MVP' && (
          <MVPPlanningPanelEnhanced
            mvp={stage1.data.mvp}
            onSave={(next) => stage1.save({ mvp: next })}
            app={app}
          />
        )}
        {activeNav === 'Business Model' && (
          <BusinessModelPanelEnhanced
            businessModel={stage1.data.businessModel}
            onSave={(next) => stage1.save({ businessModel: next })}
            app={app}
          />
        )}
        {(activeNav === 'Team' || activeNav === 'People') && (
          <PeopleTeamPanelEnhanced
            team={stage1.data.team}
            onSave={(next) => stage1.save({ team: next })}
            app={app}
          />
        )}
        {activeNav === 'Funding' && (
          <FundingPrepPanelEnhanced
            funding={stage1.data.funding}
            onSave={(next) => stage1.save({ funding: next })}
            app={app}
          />
        )}
        {activeNav === 'Tasks' && (
          <CompanyTasksPanelEnhanced
            tasks={stage1.data.tasks}
            onSave={(next) => stage1.save({ tasks: next })}
            app={app}
          />
        )}
        {activeNav === 'Documents' && (
          <CompanyDocumentsPanelEnhanced
            documents={stage1.data.documents}
            onSave={(next) => stage1.save({ documents: next })}
            app={app}
          />
        )}
        {activeNav === 'Discussions' && (
          <CompanyDiscussionsPanelEnhanced
            discussions={stage1.data.discussions}
            onSave={(next) => stage1.save({ discussions: next })}
            app={app}
          />
        )}
      </div>

      {/* Edit Overview Basics Modal */}
      {editingOverview && (
        <Dialog title="Edit Company Overview Details" onClose={() => setEditingOverview(false)}>
          <form onSubmit={handleSaveOverview} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Company / Idea Name">
                <input
                  value={overviewForm.name}
                  onChange={(e) => setOverviewForm({ ...overviewForm, name: e.target.value })}
                  required
                />
              </Field>
              <Field label="Stage Pill Label">
                <input
                  value={overviewForm.stage}
                  onChange={(e) => setOverviewForm({ ...overviewForm, stage: e.target.value })}
                  placeholder="e.g. Idea Stage"
                />
              </Field>
            </div>
            <Field label="One-Liner / Description">
              <input
                value={overviewForm.description}
                onChange={(e) => setOverviewForm({ ...overviewForm, description: e.target.value })}
                required
              />
            </Field>
            <Field label="Industry & Vertical">
              <input
                value={overviewForm.industry}
                onChange={(e) => setOverviewForm({ ...overviewForm, industry: e.target.value })}
                required
              />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label={`Overall Progress: ${overviewForm.progress}%`}>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={overviewForm.progress}
                  onChange={(e) => setOverviewForm({ ...overviewForm, progress: e.target.value })}
                />
              </Field>
              <Field label={`Idea Health: ${overviewForm.health}%`}>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={overviewForm.health}
                  onChange={(e) => setOverviewForm({ ...overviewForm, health: e.target.value })}
                />
              </Field>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '6px' }}>
              <Button variant="ghost" onClick={() => setEditingOverview(false)}>Cancel</Button>
              <Button type="submit" variant="primary">Save Overview</Button>
            </div>
          </form>
        </Dialog>
      )}

      {/* Adjust Idea Health Modal */}
      {adjustingHealth && (
        <Dialog title="Fine-Tune Idea Health Score" onClose={() => setAdjustingHealth(false)}>
          <form onSubmit={handleSaveHealth} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div>
              <label style={{ fontSize: '13px', fontWeight: 600, display: 'block', marginBottom: '8px' }}>
                Health Score: {healthForm}%
              </label>
              <input
                type="range"
                min="0"
                max="100"
                value={healthForm}
                onChange={(e) => setHealthForm(e.target.value)}
                style={{ width: '100%' }}
              />
            </div>
            <div style={{ background: 'var(--soft)', padding: '12px', borderRadius: '8px', fontSize: '12px', lineHeight: 1.6 }}>
              <b>Health Score Guide:</b>
              <ul style={{ margin: '4px 0 0', paddingLeft: '18px' }}>
                <li><strong>75% - 100%: Strong Validation</strong> · Problem confirmed by 20+ interviews, TAM &gt; $1B, high willingness to pay.</li>
                <li><strong>50% - 74%: Good Progress</strong> · Problem articulated, initial customer feedback positive, MVP scope in progress.</li>
                <li><strong>0% - 49%: Needs Attention</strong> · Weak proof points, unresolved competitive threats, or unvalidated pricing.</li>
              </ul>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setAdjustingHealth(false)}>Cancel</Button>
              <Button type="submit" variant="primary">Update Score</Button>
            </div>
          </form>
        </Dialog>
      )}
    </div>
  );
}


// ─────────────────────────────────────────────────────────────────────────────
// STAGE 2 WORKSPACE: FORMATION / LAUNCH
// ─────────────────────────────────────────────────────────────────────────────
function Stage2Workspace({
  app,
  formationData,
  setFormationData,
  activeNav,
  setActiveNav,
  stage2Actions,
  setStage2Actions,
  stage2Criteria,
  setStage2Criteria,
  onMoveToStage3,
  onOpenOnboarding,
  CompanyTeamPanel,
  TaskBoard,
  CompanyDocuments,
}) {
  const navItems = [
    { id: 'Overview', label: 'Overview', icon: BarChart3 },
    { id: 'Registration', label: 'Registration', icon: FileCheck },
    { id: 'Legal', label: 'Legal', icon: Scale },
    { id: 'Finance', label: 'Finance', icon: Landmark },
    { id: 'Brand', label: 'Brand', icon: Palette },
    { id: 'Digital Presence', label: 'Digital Presence', icon: Globe },
    { id: 'Launch Plan', label: 'Launch Plan', icon: Rocket },
    { id: 'Service Providers', label: 'Service Providers', icon: Users },
    { id: 'Team', label: 'Team', icon: UserPlus },
    { id: 'Tasks', label: 'Tasks', icon: CheckCircle2 },
    { id: 'Documents', label: 'Documents', icon: FileText },
    { id: 'Discussions', label: 'Discussions', icon: MessageSquare },
  ];

  const keyFeatureFlows = [
    {
      num: 1,
      title: 'Company Registration',
      items: ['Choose company structure', 'Name selection & availability', 'Founder information', 'Prepare documents', 'Apply for registration', 'Track registration status', 'Get official certificates'],
      nav: 'Registration',
    },
    {
      num: 2,
      title: 'Legal & Compliance',
      items: ['Founder agreements', 'Shareholding structure', 'Legal documents templates', 'Terms and policies', 'Compliance checklist', 'Government requirements', 'KYC and verification'],
      nav: 'Legal',
    },
    {
      num: 3,
      title: 'Finance Setup',
      items: ['Open business bank account', 'Tax registration (GST, PAN etc.)', 'Accounting setup', 'Initial budget planning', 'Financial projections', 'Expense tracking', 'Connect with financial advisors'],
      nav: 'Finance',
    },
    {
      num: 4,
      title: 'Brand Setup',
      items: ['Company name and tagline', 'Logo design', 'Brand identity', 'Brand guidelines', 'Marketing assets', 'Brand story', 'Visual templates'],
      nav: 'Brand',
    },
    {
      num: 5,
      title: 'Digital Presence',
      items: ['Website setup', 'Domain purchase', 'Email setup', 'Social media profiles', 'Company landing page', 'Content and SEO', 'Integrate with tools'],
      nav: 'Digital Presence',
    },
    {
      num: 6,
      title: 'Launch Preparation',
      items: ['Go-to-market strategy', 'Marketing plan', 'Sales preparation', 'Product launch checklist', 'Partnerships setup', 'Initial customer acquisition', 'Launch events'],
      nav: 'Launch Plan',
    },
    {
      num: 7,
      title: 'Service Providers',
      items: ['Find lawyers', 'Find accountants', 'Find company secretaries', 'Find designers/developers', 'Compare and hire', 'Manage service engagements'],
      nav: 'Service Providers',
    },
    {
      num: 8,
      title: 'Team Building',
      items: ['Invite team members', 'Set roles (founder/early team)', 'Manage invitations', 'Collaborate on formation tasks', 'Discussion and updates'],
      nav: 'Team',
    },
    {
      num: 9,
      title: 'Documents & Files',
      items: ['Registration documents', 'Legal agreements', 'Financial documents', 'Brand assets', 'Business plans', 'Organize with folders', 'Version history'],
      nav: 'Documents',
    },
  ];

  const serviceProviders = [
    { title: 'Legal Advisor', email: 'karthi@lawfirm.com', avatar: 'K' },
    { title: 'Accountant', email: 'priya@taxpro.com', avatar: 'P' },
    { title: 'Brand Designer', email: 'neha@designhub.com', avatar: 'N' },
  ];

  const toggleAction = (id) => {
    setStage2Actions((prev) =>
      prev.map((item) => (item.id === id ? { ...item, done: !item.done } : item))
    );
  };

  const toggleCriterion = (key) => {
    setStage2Criteria((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  return (
    <div className="company-stage-layout">
      {/* Sidebar Navigation */}
      <nav className="company-sidebar-nav" aria-label="Stage 2 navigation">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <button
              key={item.id}
              type="button"
              className={`company-nav-item ${activeNav === item.id ? 'active' : ''}`}
              onClick={() => setActiveNav(item.id)}
            >
              <Icon size={16} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Main Workspace Body */}
      <div className="stage-content-body">
        <div style={{ marginBottom: '14px', overflowX: 'auto' }}>
          <Tabs
            items={navItems.map((n) => n.id)}
            value={activeNav}
            onChange={setActiveNav}
          />
        </div>

        {/* Stage 2 Header Card */}
        <div className="stage-workspace-header">
          <div className="stage-workspace-title-box">
            <div className="stage-logo-badge stage2">
              <FileCheck size={24} />
            </div>
            <div>
              <h2>
                {formationData.name}
                <span className="stage-badge-pill stage2">Formation Stage</span>
              </h2>
              <p style={{ margin: 0, fontSize: '13px', color: 'var(--muted)' }}>
                {formationData.industry} · {formationData.structure}
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div className="stage-progress-widget">
              <span className="stage-progress-label">{formationData.progress}%</span>
              <div className="stage-progress-bar-container">
                <div
                  className="stage-progress-fill stage2"
                  style={{ width: `${formationData.progress}%` }}
                />
              </div>
            </div>
            <Button variant="ghost" onClick={onOpenOnboarding}>
              Onboarding Flow
            </Button>
          </div>
        </div>

        {/* View Switcher */}
        {activeNav === 'Overview' && (
          <>
            {/* Registration Stepper Bar */}
            <div className="dash-card" style={{ marginBottom: '20px' }}>
              <div className="dash-card-header">
                <span className="dash-card-title">
                  <Scale size={16} color="#2563eb" /> Registration Progress
                </span>
                <span style={{ fontSize: '12px', fontWeight: 700, color: '#2563eb' }}>
                  {formationData.progress}%
                </span>
              </div>
              <div className="stepper-container">
                <div className="stepper-line" />
                <div className="stepper-line-active" style={{ width: '70%' }} />

                <div className="step-node completed">
                  <div className="step-dot">✓</div>
                  <span className="step-label">Name</span>
                </div>
                <div className="step-node completed">
                  <div className="step-dot">✓</div>
                  <span className="step-label">Documents</span>
                </div>
                <div className="step-node completed">
                  <div className="step-dot">✓</div>
                  <span className="step-label">Registration</span>
                </div>
                <div className="step-node active">
                  <div className="step-dot">4</div>
                  <span className="step-label">Tax Setup</span>
                </div>
                <div className="step-node">
                  <div className="step-dot">5</div>
                  <span className="step-label">Complete</span>
                </div>
              </div>
            </div>

            {/* 4 Status Cards */}
            <div className="dashboard-grid-4">
              <div className="dash-card">
                <span className="kpi-header">Legal & Compliance</span>
                <span className="kpi-value" style={{ fontSize: '22px' }}>3/5</span>
                <span className="kpi-trend">Completed</span>
              </div>
              <div className="dash-card">
                <span className="kpi-header">Finance Setup</span>
                <span className="kpi-value" style={{ fontSize: '22px' }}>2/4</span>
                <span className="kpi-trend">Completed</span>
              </div>
              <div className="dash-card">
                <span className="kpi-header">Brand Setup</span>
                <span className="kpi-value" style={{ fontSize: '22px' }}>80%</span>
                <span className="kpi-trend">Completed</span>
              </div>
              <div className="dash-card">
                <span className="kpi-header">Launch Plan</span>
                <span className="kpi-value" style={{ fontSize: '22px' }}>4/8</span>
                <span className="kpi-trend">Completed</span>
              </div>
            </div>

            {/* Next Actions & Recent Activity & Service Providers */}
            <div className="dashboard-grid-3">
              {/* Next Actions */}
              <div className="dash-card">
                <div className="dash-card-header">
                  <span className="dash-card-title">
                    <CheckCircle2 size={16} color="#2563eb" /> Next Actions
                  </span>
                  <button
                    type="button"
                    className="dash-card-link"
                    onClick={() => setActiveNav('Tasks')}
                  >
                    View all →
                  </button>
                </div>
                <div className="actions-checklist">
                  {stage2Actions.map((action) => (
                    <label
                      key={action.id}
                      className={`action-check-item ${action.done ? 'done' : ''}`}
                    >
                      <input
                        type="checkbox"
                        checked={action.done}
                        onChange={() => toggleAction(action.id)}
                      />
                      <span>{action.label}</span>
                    </label>
                  ))}
                </div>
              </div>

              {/* Recent Activity */}
              <div className="dash-card">
                <div className="dash-card-header">
                  <span className="dash-card-title">
                    <Clock size={16} color="#6b7280" /> Recent Activity
                  </span>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  <div style={{ fontSize: '12px' }}>
                    <b>Company name approved</b>
                    <small style={{ display: 'block', color: 'var(--muted)' }}>2 days ago</small>
                  </div>
                  <div style={{ fontSize: '12px' }}>
                    <b>Logo uploaded</b>
                    <small style={{ display: 'block', color: 'var(--muted)' }}>3 days ago</small>
                  </div>
                  <div style={{ fontSize: '12px' }}>
                    <b>Meeting with lawyer completed</b>
                    <small style={{ display: 'block', color: 'var(--muted)' }}>4 days ago</small>
                  </div>
                </div>
              </div>

              {/* Service Providers */}
              <div className="dash-card">
                <div className="dash-card-header">
                  <span className="dash-card-title">
                    <Users size={16} color="#2563eb" /> Service Providers
                  </span>
                </div>
                <div>
                  {serviceProviders.map((sp) => (
                    <div key={sp.email} className="provider-row">
                      <div className="provider-info">
                        <div className="provider-avatar">{sp.avatar}</div>
                        <div className="provider-details">
                          <b>{sp.title}</b>
                          <small>{sp.email}</small>
                        </div>
                      </div>
                      <Button
                        variant="ghost"
                        onClick={() => app.toast(`Contacted ${sp.title} (${sp.email})`)}
                      >
                        Contact
                      </Button>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* 3. Key Feature Flows in Stage 2 */}
            <section className="key-features-section">
              <h3 className="key-features-heading">
                <span>3. Key Feature Flows in Stage 2</span>
              </h3>
              <div className="key-features-grid">
                {keyFeatureFlows.map((flow) => (
                  <div
                    key={flow.num}
                    className="feature-flow-card stage2"
                    onClick={() => setActiveNav(flow.nav)}
                    role="button"
                    tabIndex={0}
                  >
                    <div className="feature-card-header">
                      <span className="feature-card-num">{flow.num}</span>
                      <h4 className="feature-card-title">{flow.title}</h4>
                    </div>
                    <ul className="feature-card-items">
                      {flow.items.map((item, idx) => (
                        <li key={idx}>{item}</li>
                      ))}
                    </ul>
                    <div className="feature-card-action">
                      <span>Open {flow.title}</span>
                      <ChevronRight size={14} />
                    </div>
                  </div>
                ))}
              </div>
            </section>

            {/* 4. Stage 2 Completion Criteria */}
            <div className="completion-criteria-card stage2">
              <div>
                <h4 style={{ margin: '0 0 10px', fontSize: '15px', color: '#1e3a8a' }}>
                  4. Stage 2 Completion Criteria
                </h4>
                <div className="completion-checklist-items">
                  <label className={`completion-pill ${stage2Criteria.companyRegistered ? 'checked' : ''}`}>
                    <input
                      type="checkbox"
                      checked={stage2Criteria.companyRegistered}
                      onChange={() => toggleCriterion('companyRegistered')}
                    />
                    <span>Company registered</span>
                  </label>
                  <label className={`completion-pill ${stage2Criteria.legalComplianceComplete ? 'checked' : ''}`}>
                    <input
                      type="checkbox"
                      checked={stage2Criteria.legalComplianceComplete}
                      onChange={() => toggleCriterion('legalComplianceComplete')}
                    />
                    <span>Legal & compliance complete</span>
                  </label>
                  <label className={`completion-pill ${stage2Criteria.bankingFinanceSetup ? 'checked' : ''}`}>
                    <input
                      type="checkbox"
                      checked={stage2Criteria.bankingFinanceSetup}
                      onChange={() => toggleCriterion('bankingFinanceSetup')}
                    />
                    <span>Banking and finance setup</span>
                  </label>
                  <label className={`completion-pill ${stage2Criteria.brandDigitalReady ? 'checked' : ''}`}>
                    <input
                      type="checkbox"
                      checked={stage2Criteria.brandDigitalReady}
                      onChange={() => toggleCriterion('brandDigitalReady')}
                    />
                    <span>Brand and digital presence ready</span>
                  </label>
                  <label className={`completion-pill ${stage2Criteria.launchPrepared ? 'checked' : ''}`}>
                    <input
                      type="checkbox"
                      checked={stage2Criteria.launchPrepared}
                      onChange={() => toggleCriterion('launchPrepared')}
                    />
                    <span>Launch run prepared</span>
                  </label>
                </div>
              </div>

              <button
                type="button"
                className="stage-transition-btn stage2-btn"
                onClick={onMoveToStage3}
              >
                <span>Move to Stage 3 (Active Company)</span>
                <ArrowRight size={16} />
              </button>
            </div>
          </>
        )}

        {/* Dedicated Tab Views */}
        {activeNav === 'Tasks' && <TaskBoard />}
        {activeNav === 'Documents' && <CompanyDocuments />}
        {activeNav === 'Team' && (
          <CompanyTeamPanel
            app={app}
            company={app.companyDrafts[0] || { id: 'preview-company', name: formationData.name, remote: false }}
          />
        )}
        {activeNav === 'Registration' && <CompanyRegistrationPanel formationData={formationData} setFormationData={setFormationData} app={app} />}
        {activeNav === 'Legal' && <LegalCompliancePanel />}
        {activeNav === 'Finance' && <FinanceSetupPanel />}
        {activeNav === 'Brand' && <BrandSetupPanel />}
        {activeNav === 'Digital Presence' && <DigitalPresencePanel />}
        {activeNav === 'Launch Plan' && <LaunchPrepPanel />}
        {activeNav === 'Service Providers' && <ServiceProvidersPanel />}
        {activeNav === 'Discussions' && <DiscussionsPanel />}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// STAGE 3 WORKSPACE: ACTIVE COMPANY
// ─────────────────────────────────────────────────────────────────────────────
function Stage3Workspace({
  app,
  companyData,
  setCompanyData,
  activeNav,
  setActiveNav,
  onOpenOnboarding,
  CompanyTeamPanel,
  TaskBoard,
  CompanyDocuments,
  Hiring,
  roles,
  setRoles,
  FundingCreator,
  Products,
  Roadmap,
  Updates,
  updates,
  setUpdates,
  OperatorAnalytics,
}) {
  const navItems = [
    { id: 'Overview', label: 'Overview', icon: BarChart3 },
    { id: 'Team', label: 'Team', icon: Users },
    { id: 'Jobs & Hiring', label: 'Jobs & Hiring', icon: Briefcase },
    { id: 'Projects', label: 'Projects', icon: Rocket },
    { id: 'Operations', label: 'Operations', icon: Settings },
    { id: 'Company Feed', label: 'Company Feed', icon: MessageSquare },
    { id: 'Funding', label: 'Funding', icon: DollarSign },
    { id: 'Investors', label: 'Investors', icon: TrendingUp },
    { id: 'Analytics', label: 'Analytics', icon: BarChart3 },
    { id: 'Documents', label: 'Documents', icon: FileText },
    { id: 'Tasks', label: 'Tasks', icon: CheckCircle2 },
    { id: 'Settings', label: 'Settings', icon: Settings },
  ];

  const keyFeatureFlows = [
    {
      num: 1,
      title: 'Organization Management',
      items: ['Company profile and settings', 'Multiple locations', 'Departments and structure', 'Business units', 'Organization policies', 'Manage branding and presence'],
      nav: 'Settings',
    },
    {
      num: 2,
      title: 'Team Management',
      items: ['Add team members', 'Departments and teams', 'Roles and permissions', 'Invitations and onboarding', 'Employee profiles', 'Performance and engagement', 'Team collaboration'],
      nav: 'Team',
    },
    {
      num: 3,
      title: 'Hiring & Recruitment',
      items: ['Create job postings', 'Manage applications', 'Hiring pipeline (screening, interview)', 'Assign hiring team', 'Schedule interviews', 'Make offers', 'Recruitment analytics'],
      nav: 'Jobs & Hiring',
    },
    {
      num: 4,
      title: 'Projects & Operations',
      items: ['Create projects', 'Task management', 'Milestones and tracking', 'Workflows and approvals', 'Internal documents', 'Team collaboration', 'Calendar and deadlines'],
      nav: 'Projects',
    },
    {
      num: 5,
      title: 'Company Communication',
      items: ['Company feed', 'Announcements', 'Internal discussions', 'Team chat', 'Knowledge sharing', 'Notifications', 'Mentions and updates'],
      nav: 'Company Feed',
    },
    {
      num: 6,
      title: 'Funding & Investor Relations',
      items: ['Manage fundraising rounds', 'Track investor interactions', 'Cap table management', 'Share pitch materials', 'Investor updates', 'Schedule meetings', 'Funding analytics'],
      nav: 'Funding',
    },
    {
      num: 7,
      title: 'Business Growth & Analytics',
      items: ['Revenue tracking', 'Business metrics', 'Customer acquisition', 'Market expansion', 'Partnerships', 'New opportunities', 'Growth analytics and reports'],
      nav: 'Analytics',
    },
    {
      num: 8,
      title: 'Documents & Knowledge',
      items: ['Company documents', 'Contracts and agreements', 'HR documents', 'Policies and processes', 'Organize with folders', 'Version control', 'Access management'],
      nav: 'Documents',
    },
    {
      num: 9,
      title: 'Integrations & Tools',
      items: ['Connect external tools', 'Integrate with accounting', 'Integrate with HR tools', 'API access', 'Data import/export', 'Automations and workflows'],
      nav: 'Operations',
    },
  ];

  const continuousGrowthPillars = [
    'Hire great people',
    'Execute projects',
    'Increase revenue',
    'Expand to new markets',
    'Get investments',
    'Build partnerships',
    'Track performance',
  ];

  return (
    <div className="company-stage-layout">
      {/* Sidebar Navigation */}
      <nav className="company-sidebar-nav" aria-label="Stage 3 navigation">
        {navItems.map((item) => {
          const Icon = item.icon;
          return (
            <button
              key={item.id}
              type="button"
              className={`company-nav-item ${activeNav === item.id ? 'active' : ''}`}
              onClick={() => setActiveNav(item.id)}
            >
              <Icon size={16} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      {/* Main Workspace Body */}
      <div className="stage-content-body">
        <div style={{ marginBottom: '14px', overflowX: 'auto' }}>
          <Tabs
            items={navItems.map((n) => n.id)}
            value={activeNav}
            onChange={setActiveNav}
          />
        </div>

        {/* Stage 3 Header Card */}
        <div className="stage-workspace-header">
          <div className="stage-workspace-title-box">
            <div className="stage-logo-badge stage3">
              <Building2 size={24} />
            </div>
            <div>
              <h2>
                {companyData.name}
                <span className="stage-badge-pill stage3">Active Company</span>
              </h2>
              <p style={{ margin: 0, fontSize: '13px', color: 'var(--muted)' }}>
                Production Workspace · Operations Active · 24 Members
              </p>
            </div>
          </div>

          <div style={{ display: 'flex', gap: '10px' }}>
            <Button
              variant="ghost"
              onClick={() => app.toast('Editing company profile details')}
            >
              Edit Profile
            </Button>
            <Button variant="ghost" onClick={onOpenOnboarding}>
              Setup Flow
            </Button>
          </div>
        </div>

        {/* View Switcher */}
        {activeNav === 'Overview' && (
          <>
            {/* 4 KPI Metric Cards */}
            <div className="dashboard-grid-4">
              <div className="kpi-card">
                <span className="kpi-header">Team Members</span>
                <span className="kpi-value">{companyData.teamMembers}</span>
                <span className="kpi-trend">+3 this month</span>
              </div>
              <div className="kpi-card">
                <span className="kpi-header">Open Jobs</span>
                <span className="kpi-value">{companyData.openJobs}</span>
                <span className="kpi-trend">12 applicants</span>
              </div>
              <div className="kpi-card">
                <span className="kpi-header">Active Projects</span>
                <span className="kpi-value">{companyData.activeProjects}</span>
                <span className="kpi-trend">5 on track</span>
              </div>
              <div className="kpi-card">
                <span className="kpi-header">Revenue (YTD)</span>
                <span className="kpi-value">{companyData.revenueYTD}</span>
                <span className="kpi-trend">+20%</span>
              </div>
            </div>

            {/* Hiring Pipeline & Team Distribution & Recent Activity & Growth */}
            <div className="dashboard-grid-3">
              {/* Hiring Pipeline */}
              <div className="dash-card">
                <div className="dash-card-header">
                  <span className="dash-card-title">
                    <Briefcase size={16} color="#059669" /> Hiring Pipeline
                  </span>
                  <button
                    type="button"
                    className="dash-card-link"
                    onClick={() => setActiveNav('Jobs & Hiring')}
                  >
                    View pipeline →
                  </button>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px' }}>
                    <span>Applied</span>
                    <b>48</b>
                  </div>
                  <Progress value={80} />
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px' }}>
                    <span>Screening</span>
                    <b>20</b>
                  </div>
                  <Progress value={45} />
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px' }}>
                    <span>Interview</span>
                    <b>8</b>
                  </div>
                  <Progress value={25} />
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '12px' }}>
                    <span>Offer</span>
                    <b>3</b>
                  </div>
                  <Progress value={12} />
                </div>
              </div>

              {/* Team Distribution Donut */}
              <div className="dash-card">
                <div className="dash-card-header">
                  <span className="dash-card-title">
                    <Users size={16} color="#2563eb" /> Team Distribution
                  </span>
                </div>
                <div className="donut-chart-container">
                  <svg width="90" height="90" viewBox="0 0 42 42">
                    <circle cx="21" cy="21" r="15.915" fill="none" stroke="#e2e8f0" strokeWidth="6" />
                    {/* Product (33%) */}
                    <circle
                      cx="21" cy="21" r="15.915" fill="none" stroke="#3b82f6" strokeWidth="6"
                      strokeDasharray="33 67" strokeDashoffset="25"
                    />
                    {/* Engineering (25%) */}
                    <circle
                      cx="21" cy="21" r="15.915" fill="none" stroke="#10b981" strokeWidth="6"
                      strokeDasharray="25 75" strokeDashoffset="92"
                    />
                    {/* Marketing (17%) */}
                    <circle
                      cx="21" cy="21" r="15.915" fill="none" stroke="#f59e0b" strokeWidth="6"
                      strokeDasharray="17 83" strokeDashoffset="67"
                    />
                    {/* Operations (17%) */}
                    <circle
                      cx="21" cy="21" r="15.915" fill="none" stroke="#8b5cf6" strokeWidth="6"
                      strokeDasharray="17 83" strokeDashoffset="50"
                    />
                    <text x="21" y="23" textAnchor="middle" fontSize="8" fontWeight="bold" fill="currentColor">
                      24
                    </text>
                  </svg>

                  <div className="donut-legend">
                    <div className="donut-legend-item">
                      <span><span className="legend-color-dot" style={{ background: '#3b82f6' }} /> Product</span>
                      <b>8</b>
                    </div>
                    <div className="donut-legend-item">
                      <span><span className="legend-color-dot" style={{ background: '#10b981' }} /> Engineering</span>
                      <b>6</b>
                    </div>
                    <div className="donut-legend-item">
                      <span><span className="legend-color-dot" style={{ background: '#f59e0b' }} /> Marketing</span>
                      <b>4</b>
                    </div>
                    <div className="donut-legend-item">
                      <span><span className="legend-color-dot" style={{ background: '#8b5cf6' }} /> Operations</span>
                      <b>4</b>
                    </div>
                    <div className="donut-legend-item">
                      <span><span className="legend-color-dot" style={{ background: '#94a3b8' }} /> Others</span>
                      <b>2</b>
                    </div>
                  </div>
                </div>
              </div>

              {/* Recent Activity & Growth */}
              <div className="dash-card">
                <div className="dash-card-header">
                  <span className="dash-card-title">
                    <TrendingUp size={16} color="#059669" /> Business Growth
                  </span>
                  <span style={{ fontSize: '12px', fontWeight: 700, color: '#059669' }}>
                    +32%
                  </span>
                </div>
                <div style={{ height: '70px', display: 'flex', alignItems: 'flex-end', gap: '6px', padding: '6px 0' }}>
                  {[28, 35, 42, 38, 55, 68, 85].map((h, i) => (
                    <div
                      key={i}
                      style={{
                        flex: 1,
                        height: `${h}%`,
                        background: 'linear-gradient(to top, #10b981, #6ee7b7)',
                        borderRadius: '3px',
                      }}
                    />
                  ))}
                </div>
                <div style={{ borderTop: '1px solid var(--line)', paddingTop: '8px', fontSize: '11px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <div><b>New job posted:</b> Frontend Developer <small style={{ color: 'var(--muted)' }}>(2h ago)</small></div>
                  <div><b>Team completed:</b> Website Redesign <small style={{ color: 'var(--muted)' }}>(1d ago)</small></div>
                </div>
              </div>
            </div>

            {/* 3. Key Feature Flows in Stage 3 */}
            <section className="key-features-section">
              <h3 className="key-features-heading">
                <span>3. Key Feature Flows in Stage 3</span>
              </h3>
              <div className="key-features-grid">
                {keyFeatureFlows.map((flow) => (
                  <div
                    key={flow.num}
                    className="feature-flow-card stage3"
                    onClick={() => setActiveNav(flow.nav)}
                    role="button"
                    tabIndex={0}
                  >
                    <div className="feature-card-header">
                      <span className="feature-card-num">{flow.num}</span>
                      <h4 className="feature-card-title">{flow.title}</h4>
                    </div>
                    <ul className="feature-card-items">
                      {flow.items.map((item, idx) => (
                        <li key={idx}>{item}</li>
                      ))}
                    </ul>
                    <div className="feature-card-action">
                      <span>Open {flow.title}</span>
                      <ChevronRight size={14} />
                    </div>
                  </div>
                ))}
              </div>
            </section>

            {/* 4. Continuous Growth (Stage 3 Ongoing) */}
            <div className="completion-criteria-card stage3">
              <div style={{ width: '100%' }}>
                <h4 style={{ margin: '0 0 6px', fontSize: '15px', color: '#064e3b' }}>
                  4. Continuous Growth (Stage 3 Ongoing)
                </h4>
                <p style={{ margin: '0 0 10px', fontSize: '12px', color: 'var(--muted)' }}>
                  Active operations flywheel for ongoing scale, team development, and sustainable business revenue:
                </p>
                <div className="continuous-growth-row">
                  {continuousGrowthPillars.map((pillar) => (
                    <div
                      key={pillar}
                      className="growth-pill-item"
                      onClick={() => app.toast(`Opened ${pillar} workflow`)}
                    >
                      <Zap size={14} color="#059669" />
                      <span>{pillar}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </>
        )}

        {/* Dedicated Tab Views */}
        {activeNav === 'Tasks' && <TaskBoard />}
        {activeNav === 'Documents' && <CompanyDocuments />}
        {activeNav === 'Team' && (
          <CompanyTeamPanel
            app={app}
            company={app.companyDrafts[0] || { id: 'preview-company', name: companyData.name, remote: false }}
          />
        )}
        {activeNav === 'Jobs & Hiring' && <Hiring roles={roles} setRoles={setRoles} />}
        {activeNav === 'Projects' && <Products />}
        {activeNav === 'Operations' && <Roadmap />}
        {activeNav === 'Company Feed' && (
          <Updates updates={updates} setUpdates={setUpdates} />
        )}
        {activeNav === 'Funding' && <FundingCreator />}
        {activeNav === 'Investors' && <FundingCreator />}
        {activeNav === 'Analytics' && <OperatorAnalytics />}
        {activeNav === 'Settings' && <OrgSettingsPanel companyData={companyData} />}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// DISCOVER / DIRECTORY VIEW (BACKWARD COMPATIBLE)
// ─────────────────────────────────────────────────────────────────────────────
function DiscoverCompaniesView({ app, onStart }) {
  return (
    <div className="operator-layout">
      <section>
        <div className="operator-hero company-operator">
          <div>
            <Tag tone="purple">Startup and company</Tag>
            <h2>Build the useful thing, then make the work visible.</h2>
            <p>
              Create a company when there is a real problem, a small proof,
              and someone ready to own the next step.
            </p>
            <Button icon={Plus} onClick={onStart}>
              Create company
            </Button>
          </div>
          <div className="hero-score">
            <strong>82%</strong>
            <span>company readiness</span>
            <Progress value={82} />
          </div>
        </div>

        <SectionTitle
          title="Explore companies"
          action="Create yours"
          onAction={onStart}
        />
        <div className="card-grid">
          {companies.map((c) => (
            <article className="company-card" key={c.id}>
              <span className={`module-icon ${c.color}`}>
                <Building2 size={21} />
              </span>
              <h3>{c.title}</h3>
              <p>{c.description}</p>
              <div className="tag-cloud">
                {c.tags.map((t) => (
                  <Tag key={t}>{t}</Tag>
                ))}
              </div>
              <small>
                {c.stage} · {c.location} · {c.team} people
              </small>
              <Button
                onClick={() =>
                  app.setModal({ type: 'details', kind: 'company', item: c })
                }
              >
                View company
              </Button>
            </article>
          ))}
        </div>
      </section>

      <aside className="space-rail">
        <SectionTitle title="Company Lifecycle Stages" />
        <div className="journey">
          <span>1</span>
          <div>
            <strong>Stage 1: Idea / Build</strong>
            <p>Turn an idea into a validated business opportunity.</p>
          </div>
          <span>2</span>
          <div>
            <strong>Stage 2: Formation / Launch</strong>
            <p>Turn the validated idea into a legally launch-ready company.</p>
          </div>
          <span>3</span>
          <div>
            <strong>Stage 3: Active Company</strong>
            <p>Operate, manage and grow the business with your team.</p>
          </div>
        </div>
      </aside>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// STAGE 1 ONBOARDING MODAL FLOW (4 STEPS)
// ─────────────────────────────────────────────────────────────────────────────
function Stage1OnboardingModal({ step, setStep, onClose, ideaData, setIdeaData, onComplete }) {
  const [selectedGoal, setSelectedGoal] = useState({
    'Validate idea': true,
    'Find co-founders': true,
    'Build MVP': true,
    'Get market insights': true,
    'Prepare for funding': false,
  });

  const [intent, setIntent] = useState("I'm working on an idea");

  return (
    <div className="onboarding-modal-overlay">
      <div className="onboarding-modal-window">
        <div className="onboarding-modal-header">
          <div className="modal-brand-badge">
            <span style={{ fontSize: '18px' }}>💡</span>
            <span>Stage 1: Idea Onboarding ({step}/4)</span>
          </div>
          <button type="button" className="modal-close-btn" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <div className="onboarding-modal-body">
          {/* Step 1: Choose What You Want to Do? */}
          {step === 1 && (
            <>
              <div>
                <h3 className="modal-flow-title">Choose What You Want to Do?</h3>
                <p className="modal-flow-desc">Select where you are in your startup journey today.</p>
              </div>
              <div className="radio-cards-group">
                {[
                  { id: "I'm working on an idea", desc: 'Turn your early concept into a validated opportunity.' },
                  { id: 'I already have an idea', desc: 'Jump right into market research, validation, and MVP planning.' },
                  { id: 'I already have a company', desc: 'Set up company formation, legal docs, or ongoing operations.' },
                ].map((opt) => (
                  <label
                    key={opt.id}
                    className={`radio-choice-card ${intent === opt.id ? 'selected' : ''}`}
                  >
                    <input
                      type="radio"
                      name="stage1-intent"
                      checked={intent === opt.id}
                      onChange={() => setIntent(opt.id)}
                    />
                    <div className="radio-choice-content">
                      <b>{opt.id}</b>
                      <small>{opt.desc}</small>
                    </div>
                  </label>
                ))}
              </div>
            </>
          )}

          {/* Step 2: Describe Your Idea */}
          {step === 2 && (
            <>
              <div>
                <h3 className="modal-flow-title">Describe Your Idea</h3>
                <p className="modal-flow-desc">Give your project an identity and initial focus.</p>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div className="flow-input-field">
                  <label htmlFor="idea-name">Idea Name</label>
                  <input
                    id="idea-name"
                    value={ideaData.name}
                    onChange={(e) => setIdeaData({ ...ideaData, name: e.target.value })}
                    placeholder="e.g. EcoCart"
                  />
                </div>
                <div className="flow-input-field">
                  <label htmlFor="idea-desc">Short Description</label>
                  <textarea
                    id="idea-desc"
                    rows={2}
                    value={ideaData.description}
                    onChange={(e) => setIdeaData({ ...ideaData, description: e.target.value })}
                    placeholder="Sustainable e-commerce for local businesses"
                  />
                </div>
                <div className="flow-input-field">
                  <label htmlFor="idea-industry">Industry</label>
                  <select
                    id="idea-industry"
                    value={ideaData.industry}
                    onChange={(e) => setIdeaData({ ...ideaData, industry: e.target.value })}
                  >
                    <option value="E-commerce">E-commerce</option>
                    <option value="Fintech">Fintech</option>
                    <option value="Healthtech">Healthtech</option>
                    <option value="Clean Energy">Clean Energy</option>
                    <option value="Edtech">Edtech</option>
                    <option value="SaaS & Developer Tools">SaaS & Developer Tools</option>
                    <option value="AI & Robotics">AI & Robotics</option>
                  </select>
                </div>
              </div>
            </>
          )}

          {/* Step 3: Set Your Goals */}
          {step === 3 && (
            <>
              <div>
                <h3 className="modal-flow-title">Set Your Goals</h3>
                <p className="modal-flow-desc">What do you want to accomplish in Stage 1?</p>
              </div>
              <div className="actions-checklist">
                {Object.keys(selectedGoal).map((goal) => (
                  <label key={goal} className="action-check-item">
                    <input
                      type="checkbox"
                      checked={selectedGoal[goal]}
                      onChange={() => setSelectedGoal({ ...selectedGoal, [goal]: !selectedGoal[goal] })}
                    />
                    <span>{goal}</span>
                  </label>
                ))}
              </div>
            </>
          )}

          {/* Step 4: Idea Workspace Created! */}
          {step === 4 && (
            <div className="modal-celebration-step">
              <div className="celebration-icon-box stage1">
                <Sparkles size={36} />
              </div>
              <h3 className="modal-flow-title">Idea Workspace Created!</h3>
              <p className="modal-flow-desc">
                Your idea workspace for <b>{ideaData.name}</b> is ready. Let's start building!
              </p>
            </div>
          )}
        </div>

        <div className="onboarding-modal-footer">
          {step < 4 ? (
            <button
              type="button"
              className="modal-primary-btn"
              onClick={() => setStep(step + 1)}
            >
              {step === 1 ? 'Continue' : 'Next'}
            </button>
          ) : (
            <button
              type="button"
              className="modal-primary-btn"
              onClick={onComplete}
            >
              Go to Dashboard
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// STAGE 2 ONBOARDING MODAL FLOW (4 STEPS)
// ─────────────────────────────────────────────────────────────────────────────
function Stage2OnboardingModal({ step, setStep, onClose, formationData, setFormationData, onComplete }) {
  const [startChoice, setStartChoice] = useState('Continue from my idea (EcoCart)');
  const [newFounderName, setNewFounderName] = useState('');
  const [newFounderRole, setNewFounderRole] = useState('Co-founder');

  const addFounder = () => {
    if (!newFounderName.trim()) return;
    setFormationData({
      ...formationData,
      founders: [
        ...formationData.founders,
        { id: `f-${Date.now()}`, name: newFounderName.trim(), role: newFounderRole },
      ],
    });
    setNewFounderName('');
  };

  return (
    <div className="onboarding-modal-overlay">
      <div className="onboarding-modal-window">
        <div className="onboarding-modal-header">
          <div className="modal-brand-badge">
            <span style={{ fontSize: '18px' }}>📄</span>
            <span>Stage 2: Formation Onboarding ({step}/4)</span>
          </div>
          <button type="button" className="modal-close-btn" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <div className="onboarding-modal-body">
          {/* Step 1: How do you want to start? */}
          {step === 1 && (
            <>
              <div>
                <h3 className="modal-flow-title">How do you want to start?</h3>
                <p className="modal-flow-desc">Choose whether to transition an existing idea or start fresh.</p>
              </div>
              <div className="radio-cards-group">
                {[
                  { id: 'Continue from my idea (EcoCart)', desc: 'Carry over your validated problem, market insights, and MVP artifacts.' },
                  { id: 'Start new company directly', desc: 'Begin formation for a brand new corporate entity.' },
                ].map((opt) => (
                  <label
                    key={opt.id}
                    className={`radio-choice-card ${startChoice === opt.id ? 'selected' : ''}`}
                  >
                    <input
                      type="radio"
                      name="stage2-start"
                      checked={startChoice === opt.id}
                      onChange={() => setStartChoice(opt.id)}
                    />
                    <div className="radio-choice-content">
                      <b>{opt.id}</b>
                      <small>{opt.desc}</small>
                    </div>
                  </label>
                ))}
              </div>
            </>
          )}

          {/* Step 2: Company Information */}
          {step === 2 && (
            <>
              <div>
                <h3 className="modal-flow-title">Company Information</h3>
                <p className="modal-flow-desc">Enter legal registration details and corporate structure.</p>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div className="flow-input-field">
                  <label htmlFor="company-name">Company Name</label>
                  <input
                    id="company-name"
                    value={formationData.name}
                    onChange={(e) => setFormationData({ ...formationData, name: e.target.value })}
                    placeholder="e.g. EcoCart Technologies"
                  />
                </div>
                <div className="flow-input-field">
                  <label htmlFor="company-industry">Industry</label>
                  <select
                    id="company-industry"
                    value={formationData.industry}
                    onChange={(e) => setFormationData({ ...formationData, industry: e.target.value })}
                  >
                    <option value="E-commerce">E-commerce</option>
                    <option value="Fintech">Fintech</option>
                    <option value="Healthtech">Healthtech</option>
                    <option value="Clean Energy">Clean Energy</option>
                    <option value="Technology & SaaS">Technology & SaaS</option>
                  </select>
                </div>
                <div className="flow-input-field">
                  <label htmlFor="company-structure">Company Structure</label>
                  <select
                    id="company-structure"
                    value={formationData.structure}
                    onChange={(e) => setFormationData({ ...formationData, structure: e.target.value })}
                  >
                    <option value="Private Limited">Private Limited</option>
                    <option value="Limited Liability Company (LLC)">Limited Liability Company (LLC)</option>
                    <option value="C-Corporation">C-Corporation</option>
                    <option value="Sole Proprietorship">Sole Proprietorship</option>
                    <option value="Partnership">Partnership</option>
                  </select>
                </div>
              </div>
            </>
          )}

          {/* Step 3: Founders Information */}
          {step === 3 && (
            <>
              <div>
                <h3 className="modal-flow-title">Founders Information</h3>
                <p className="modal-flow-desc">Add primary founders and equity stakeholders.</p>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {formationData.founders.map((f) => (
                  <div
                    key={f.id}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '10px 14px',
                      background: 'var(--soft)',
                      borderRadius: '8px',
                      border: '1px solid var(--line)',
                    }}
                  >
                    <div>
                      <b style={{ display: 'block', fontSize: '13px' }}>{f.name}</b>
                      <small style={{ color: 'var(--muted)' }}>{f.role}</small>
                    </div>
                    <Tag tone="blue">{f.role}</Tag>
                  </div>
                ))}

                <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                  <input
                    style={{ flex: 1, padding: '8px 10px', fontSize: '13px', borderRadius: '6px', border: '1px solid var(--line)' }}
                    placeholder="Founder full name"
                    value={newFounderName}
                    onChange={(e) => setNewFounderName(e.target.value)}
                  />
                  <select
                    style={{ padding: '8px 10px', fontSize: '13px', borderRadius: '6px', border: '1px solid var(--line)' }}
                    value={newFounderRole}
                    onChange={(e) => setNewFounderRole(e.target.value)}
                  >
                    <option value="Co-founder">Co-founder</option>
                    <option value="Advisor">Advisor</option>
                    <option value="CTO">CTO</option>
                    <option value="COO">COO</option>
                  </select>
                  <Button variant="ghost" onClick={addFounder}>
                    + Add Founder
                  </Button>
                </div>
              </div>
            </>
          )}

          {/* Step 4: Formation Workspace Created! */}
          {step === 4 && (
            <div className="modal-celebration-step">
              <div className="celebration-icon-box stage2">
                <FileCheck size={36} />
              </div>
              <h3 className="modal-flow-title">Formation Workspace Created!</h3>
              <p className="modal-flow-desc">
                Your company formation workspace for <b>{formationData.name}</b> is ready.
                Let's get your company registered and launch-ready.
              </p>
            </div>
          )}
        </div>

        <div className="onboarding-modal-footer">
          {step < 4 ? (
            <button
              type="button"
              className="modal-primary-btn"
              onClick={() => setStep(step + 1)}
            >
              {step === 1 ? 'Continue' : 'Next'}
            </button>
          ) : (
            <button
              type="button"
              className="modal-primary-btn"
              onClick={onComplete}
            >
              Go to Dashboard
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// STAGE 3 ACTIVE COMPANY SETUP FLOW (4 STEPS)
// ─────────────────────────────────────────────────────────────────────────────
function Stage3OnboardingModal({ step, setStep, onClose, companyData, setCompanyData, onComplete }) {
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState('Employee');

  return (
    <div className="onboarding-modal-overlay">
      <div className="onboarding-modal-window">
        <div className="onboarding-modal-header">
          <div className="modal-brand-badge">
            <span style={{ fontSize: '18px' }}>🏢</span>
            <span>Stage 3: Active Setup ({step}/4)</span>
          </div>
          <button type="button" className="modal-close-btn" onClick={onClose}>
            <X size={18} />
          </button>
        </div>

        <div className="onboarding-modal-body">
          {/* Step 1: Company Activation */}
          {step === 1 && (
            <>
              <div>
                <h3 className="modal-flow-title">Company Activation</h3>
                <p className="modal-flow-desc">Your company is now active on Collobster!</p>
              </div>
              <div className="actions-checklist">
                <div className="action-check-item">
                  <CheckCircle2 size={18} color="#059669" />
                  <span>Registration complete</span>
                </div>
                <div className="action-check-item">
                  <CheckCircle2 size={18} color="#059669" />
                  <span>Company profile created</span>
                </div>
                <div className="action-check-item">
                  <CheckCircle2 size={18} color="#059669" />
                  <span>Team setup initiated</span>
                </div>
              </div>
            </>
          )}

          {/* Step 2: Set Up Organization */}
          {step === 2 && (
            <>
              <div>
                <h3 className="modal-flow-title">Set Up Organization</h3>
                <p className="modal-flow-desc">Configure headquarters, regional offices, and core departments.</p>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                <div className="flow-input-field">
                  <label htmlFor="company-locations">Locations</label>
                  <input
                    id="company-locations"
                    defaultValue="Bengaluru, India (HQ) · San Francisco · Remote"
                  />
                </div>
                <div className="flow-input-field">
                  <label htmlFor="company-depts">Departments</label>
                  <input
                    id="company-depts"
                    defaultValue="Product, Engineering, Marketing, Operations, Sales"
                  />
                </div>
              </div>
            </>
          )}

          {/* Step 3: Invite Team Members */}
          {step === 3 && (
            <>
              <div>
                <h3 className="modal-flow-title">Invite Team Members</h3>
                <p className="modal-flow-desc">Bring your builders, engineers, and operators on board.</p>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                <div className="flow-input-field">
                  <label htmlFor="invite-email">Enter email address</label>
                  <input
                    id="invite-email"
                    type="email"
                    placeholder="teammate@company.com"
                    value={inviteEmail}
                    onChange={(e) => setInviteEmail(e.target.value)}
                  />
                </div>
                <div className="flow-input-field">
                  <label htmlFor="invite-role">Select role</label>
                  <select
                    id="invite-role"
                    value={inviteRole}
                    onChange={(e) => setInviteRole(e.target.value)}
                  >
                    <option value="Employee">Employee</option>
                    <option value="Manager">Manager</option>
                    <option value="Executive">Executive</option>
                    <option value="Advisor">Advisor</option>
                    <option value="Contractor">Contractor</option>
                  </select>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <Button
                    onClick={() => {
                      if (inviteEmail) {
                        alert(`Invite sent to ${inviteEmail} as ${inviteRole}`);
                        setInviteEmail('');
                      }
                      setStep(4);
                    }}
                  >
                    Send Invite
                  </Button>
                  <button
                    type="button"
                    className="modal-skip-btn"
                    onClick={() => setStep(4)}
                  >
                    Skip for now
                  </button>
                </div>
              </div>
            </>
          )}

          {/* Step 4: Start Operations */}
          {step === 4 && (
            <div className="modal-celebration-step">
              <div className="celebration-icon-box stage3">
                <Rocket size={36} />
              </div>
              <h3 className="modal-flow-title">Start Operations</h3>
              <p className="modal-flow-desc">
                Your company workspace is ready. Manage your team, hire, run operations and grow your business.
              </p>
            </div>
          )}
        </div>

        <div className="onboarding-modal-footer">
          {step === 1 && (
            <button
              type="button"
              className="modal-primary-btn"
              onClick={() => setStep(2)}
            >
              Get Started
            </button>
          )}
          {step === 2 && (
            <button
              type="button"
              className="modal-primary-btn"
              onClick={() => setStep(3)}
            >
              Next
            </button>
          )}
          {step === 4 && (
            <button
              type="button"
              className="modal-primary-btn"
              onClick={onComplete}
            >
              Go to Dashboard
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// FEATURE SUBPANELS (STAGE 1 & STAGE 2)
// ─────────────────────────────────────────────────────────────────────────────

function IdeaManagementPanel({ ideaData, setIdeaData }) {
  return (
    <section className="dash-card">
      <SectionTitle title="Idea Management" />
      <p style={{ fontSize: '13px', color: 'var(--muted)' }}>
        Formulate your problem hypothesis, value proposition, and customer segment.
      </p>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Problem Statement</b>
          <p style={{ fontSize: '12px', color: 'var(--muted)' }}>
            Independent sustainable producers lack an accessible, transparent distribution network for conscious consumers.
          </p>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Solution Definition</b>
          <p style={{ fontSize: '12px', color: 'var(--muted)' }}>
            A verified hyper-local e-commerce marketplace connecting verified eco-friendly producers with buyers.
          </p>
        </div>
      </div>
    </section>
  );
}

function MarketResearchPanel() {
  return (
    <section className="dash-card">
      <SectionTitle title="Market Research & Analysis" />
      <div className="dashboard-grid-3">
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Market Size & Trends</b>
          <p style={{ fontSize: '12px', color: 'var(--muted)' }}>TAM: $14B sustainable retail market growing at 22% CAGR.</p>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Competitor Analysis</b>
          <p style={{ fontSize: '12px', color: 'var(--muted)' }}>Benchmarked 5 regional platforms; clear gap in vendor verification.</p>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>AI Powered Insights</b>
          <p style={{ fontSize: '12px', color: 'var(--muted)' }}>Top consumer requirement: verifiable carbon footprint offsets.</p>
        </div>
      </div>
    </section>
  );
}

function ValidationPanel() {
  return (
    <section className="dash-card">
      <SectionTitle title="Validation & Customer Feedback" />
      <div className="dashboard-grid-3">
        <div className="dash-card">
          <span className="kpi-header">Interviews Conducted</span>
          <span className="kpi-value">32</span>
          <span className="kpi-trend">88% positive intent</span>
        </div>
        <div className="dash-card">
          <span className="kpi-header">Surveys Completed</span>
          <span className="kpi-value">145</span>
          <span className="kpi-trend">High willingness to pay</span>
        </div>
        <div className="dash-card">
          <span className="kpi-header">Assumptions Validated</span>
          <span className="kpi-value">4 / 5</span>
          <span className="kpi-trend">Core hypothesis confirmed</span>
        </div>
      </div>
    </section>
  );
}

function MVPPlanningPanel() {
  return (
    <section className="dash-card">
      <SectionTitle title="MVP Planning & Features" />
      <div className="actions-checklist">
        <div className="action-check-item done">
          <CheckCircle2 size={16} color="#059669" />
          <span>Vendor onboarding and catalog builder</span>
        </div>
        <div className="action-check-item done">
          <CheckCircle2 size={16} color="#059669" />
          <span>Customer cart & checkout flow</span>
        </div>
        <div className="action-check-item">
          <CheckCircle2 size={16} color="#6b7280" />
          <span>Automated carbon savings metric badge</span>
        </div>
        <div className="action-check-item">
          <CheckCircle2 size={16} color="#6b7280" />
          <span>Courier delivery integration</span>
        </div>
      </div>
    </section>
  );
}

function BusinessModelPanel() {
  return (
    <section className="dash-card">
      <SectionTitle title="Business Model Canvas" />
      <div className="dashboard-grid-4">
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Revenue Model</b>
          <p style={{ fontSize: '11px', color: 'var(--muted)' }}>8% marketplace take-rate + premium seller tier.</p>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Pricing Strategy</b>
          <p style={{ fontSize: '11px', color: 'var(--muted)' }}>Dynamic commission based on verified carbon metrics.</p>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Cost Structure</b>
          <p style={{ fontSize: '11px', color: 'var(--muted)' }}>Cloud hosting, payment gateway, verification logistics.</p>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Unit Economics</b>
          <p style={{ fontSize: '11px', color: 'var(--muted)' }}>Target CAC: $12, LTV: $84, Payback: 2.5 months.</p>
        </div>
      </div>
    </section>
  );
}

function FundingPrepPanel() {
  return (
    <section className="dash-card">
      <SectionTitle title="Funding Preparation & Pitch Deck" />
      <p style={{ fontSize: '13px', color: 'var(--muted)' }}>
        Build investor-ready pitch materials, financial models, and valuation metrics.
      </p>
      <div className="dashboard-grid-3">
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Seed Pitch Deck</b>
          <p style={{ fontSize: '11px', color: 'var(--muted)' }}>12 slides completed with market validation data.</p>
          <Button variant="ghost">Download PDF</Button>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Funding Needs</b>
          <p style={{ fontSize: '11px', color: 'var(--muted)' }}>Raising $250K for 18-month engineering & launch runway.</p>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Readiness Score</b>
          <span style={{ fontSize: '24px', fontWeight: 'bold', color: '#10b981' }}>84 / 100</span>
          <p style={{ fontSize: '11px', color: 'var(--muted)' }}>Strong validation proof points.</p>
        </div>
      </div>
    </section>
  );
}

function DiscussionsPanel() {
  return (
    <section className="dash-card">
      <SectionTitle title="Workspace Discussions & Updates" />
      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Mohit Koppera:</b> "We have finalized our preliminary supplier interviews. Onboarding looks solid!"
          <small style={{ display: 'block', color: 'var(--muted)', marginTop: '4px' }}>Yesterday at 4:30 PM</small>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Priya Sharma:</b> "Draft founder agreements have been sent to our legal advisor for final review."
          <small style={{ display: 'block', color: 'var(--muted)', marginTop: '4px' }}>2 days ago</small>
        </div>
      </div>
    </section>
  );
}

function CompanyRegistrationPanel({ formationData, setFormationData, app }) {
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState(() => ({
    name: formationData.name || '', structure: formationData.structure || 'Private Limited',
    registrationNumber: formationData.registrationNumber || '', registeredAddress: formationData.registeredAddress || '',
  }));
  const save = (event) => {
    event.preventDefault();
    if (!form.name.trim() || !form.registeredAddress.trim()) return app.toast('Add the company name and registered address to continue.');
    setFormationData(old => ({ ...old, ...form, registrationStep: Math.max(old.registrationStep || 1, 4), progress: Math.max(old.progress || 0, 80) }));
    setEditing(false);
    app.toast('Registration details saved in this workspace.');
  };
  return (
    <section className="dash-card">
      <div className="dash-card-header"><SectionTitle title="Company Registration & Certificates" /><Button variant="primary" onClick={() => setEditing(!editing)}>{editing ? 'Cancel' : 'Fill registration details'}</Button></div>
      {editing && <form onSubmit={save} style={{display:'grid',gap:12,marginBottom:20}}>
        <label className="auth-field"><span>Legal company name</span><input value={form.name} onChange={e=>setForm({...form,name:e.target.value})} required /></label>
        <label className="auth-field"><span>Company structure</span><select value={form.structure} onChange={e=>setForm({...form,structure:e.target.value})}><option>Private Limited</option><option>LLP</option><option>Partnership</option><option>Sole Proprietorship</option></select></label>
        <label className="auth-field"><span>Registered address</span><textarea value={form.registeredAddress} onChange={e=>setForm({...form,registeredAddress:e.target.value})} required /></label>
        <label className="auth-field"><span>Registration / CIN number (optional until issued)</span><input value={form.registrationNumber} onChange={e=>setForm({...form,registrationNumber:e.target.value})} /></label>
        <Button type="submit" variant="primary">Save registration progress</Button>
      </form>}
      <div className="dashboard-grid-3">
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Entity Name</b>
          <p style={{ fontSize: '13px' }}>{formationData.name}</p>
          <Tag tone="green">Name Approved</Tag>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Corporate Structure</b>
          <p style={{ fontSize: '13px' }}>{formationData.structure}</p>
          <Tag tone="blue">Application Filed</Tag>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Official Certificate</b>
          <p style={{ fontSize: '13px' }}>{formationData.registrationNumber || 'CIN / Registration Number'}</p>
          <Tag tone={formationData.registrationNumber ? 'green' : 'gold'}>{formationData.registrationNumber ? 'Recorded' : 'Pending Issuance'}</Tag>
        </div>
      </div>
    </section>
  );
}

function LegalCompliancePanel() {
  return (
    <section className="dash-card">
      <SectionTitle title="Legal & Compliance Center" />
      <div className="actions-checklist">
        <div className="action-check-item done">
          <CheckCircle2 size={16} color="#059669" />
          <span>Founder shareholding structure and equity split</span>
        </div>
        <div className="action-check-item done">
          <CheckCircle2 size={16} color="#059669" />
          <span>Articles of Association & Memorandum</span>
        </div>
        <div className="action-check-item done">
          <CheckCircle2 size={16} color="#059669" />
          <span>Non-disclosure & IP assignment agreements</span>
        </div>
        <div className="action-check-item">
          <CheckCircle2 size={16} color="#6b7280" />
          <span>Terms of Service and Privacy Policy for website</span>
        </div>
      </div>
    </section>
  );
}

function FinanceSetupPanel() {
  return (
    <section className="dash-card">
      <SectionTitle title="Finance & Banking Setup" />
      <div className="dashboard-grid-3">
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Commercial Bank Account</b>
          <p style={{ fontSize: '12px', color: 'var(--muted)' }}>Application under review with HDFC / Silicon Valley Bank.</p>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Tax ID & Registrations</b>
          <p style={{ fontSize: '12px', color: 'var(--muted)' }}>PAN, TAN, and GST application documents ready.</p>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Accounting System</b>
          <p style={{ fontSize: '12px', color: 'var(--muted)' }}>Configured chart of accounts in QuickBooks / Zoho.</p>
        </div>
      </div>
    </section>
  );
}

function BrandSetupPanel() {
  return (
    <section className="dash-card">
      <SectionTitle title="Brand & Visual Identity" />
      <div className="dashboard-grid-3">
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Logomark & Typo</b>
          <p style={{ fontSize: '12px', color: 'var(--muted)' }}>Vector SVGs and PNG icon kits created.</p>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Brand Guidelines</b>
          <p style={{ fontSize: '12px', color: 'var(--muted)' }}>Colors, typography tokens, and voice guide documented.</p>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Social Assets</b>
          <p style={{ fontSize: '12px', color: 'var(--muted)' }}>Header banners for LinkedIn, X/Twitter, and Instagram.</p>
        </div>
      </div>
    </section>
  );
}

function DigitalPresencePanel() {
  return (
    <section className="dash-card">
      <SectionTitle title="Digital Presence & Domain" />
      <div className="dashboard-grid-3">
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Domain Name</b>
          <p style={{ fontSize: '12px' }}>ecocart.app · Active DNS</p>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Company Landing Page</b>
          <p style={{ fontSize: '12px' }}>Published & SSL Secured</p>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <b>Work Email Suite</b>
          <p style={{ fontSize: '12px' }}>Google Workspace active</p>
        </div>
      </div>
    </section>
  );
}

function LaunchPrepPanel() {
  return (
    <section className="dash-card">
      <SectionTitle title="Launch Preparation Checklist" />
      <div className="actions-checklist">
        <div className="action-check-item done">
          <CheckCircle2 size={16} color="#059669" />
          <span>Product launch testing on staging environment</span>
        </div>
        <div className="action-check-item done">
          <CheckCircle2 size={16} color="#059669" />
          <span>Launch press release & media kit drafted</span>
        </div>
        <div className="action-check-item">
          <CheckCircle2 size={16} color="#6b7280" />
          <span>10 founding vendor partners onboarded</span>
        </div>
        <div className="action-check-item">
          <CheckCircle2 size={16} color="#6b7280" />
          <span>Product Hunt and community demo day scheduling</span>
        </div>
      </div>
    </section>
  );
}

function ServiceProvidersPanel() {
  return (
    <section className="dash-card">
      <SectionTitle title="Service Providers & Partners" />
      <div className="dashboard-grid-3">
        <div className="dash-card">
          <b>Karthi V.</b>
          <small style={{ color: 'var(--muted)' }}>Corporate Lawyer at LawFirm Associates</small>
          <p style={{ fontSize: '12px', margin: '8px 0' }}>Specializes in startup incorporation and tech contracts.</p>
          <Button variant="ghost">Message</Button>
        </div>
        <div className="dash-card">
          <b>Priya N.</b>
          <small style={{ color: 'var(--muted)' }}>Chartered Accountant at TaxPro</small>
          <p style={{ fontSize: '12px', margin: '8px 0' }}>Manages GST filings, payroll, and statutory audits.</p>
          <Button variant="ghost">Message</Button>
        </div>
        <div className="dash-card">
          <b>Neha S.</b>
          <small style={{ color: 'var(--muted)' }}>Design Director at DesignHub</small>
          <p style={{ fontSize: '12px', margin: '8px 0' }}>Brand identity and marketing web design.</p>
          <Button variant="ghost">Message</Button>
        </div>
      </div>
    </section>
  );
}

function OrgSettingsPanel({ companyData }) {
  return (
    <section className="dash-card">
      <SectionTitle title="Organization Settings & Policy" />
      <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
        <div className="flow-input-field">
          <label>Organization Name</label>
          <input defaultValue={companyData.name} />
        </div>
        <div className="flow-input-field">
          <label>Primary Office</label>
          <input defaultValue="Bengaluru, Karnataka, India" />
        </div>
        <div className="flow-input-field">
          <label>Operating Hours & Timezone</label>
          <input defaultValue="IST (UTC+5:30) · 9:00 AM – 6:00 PM" />
        </div>
      </div>
    </section>
  );
}
