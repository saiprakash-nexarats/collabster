import React, { useState, useEffect } from 'react';
import {
  Lightbulb,
  CheckCircle2,
  ArrowRight,
  Users,
  Briefcase,
  DollarSign,
  Compass,
  Rocket,
  Plus,
  Clock,
  ExternalLink,
  ChevronRight,
  Check,
  Search,
  MessageSquare,
  Award,
  Sparkles,
  FileText,
  BarChart3,
  X,
  UserPlus,
  Settings,
  Mail,
  Send,
  Zap,
  Edit2,
  Trash2,
  Filter,
  ThumbsUp,
  MessageCircle,
  Upload,
  Download,
  RefreshCw,
  Target,
  Layers,
  HelpCircle,
  AlertCircle,
  CheckSquare,
  MoreVertical,
} from 'lucide-react';
import {
  Avatar,
  Button,
  IconButton,
  Tag,
  Tabs,
  SectionTitle,
  Progress,
  Dialog,
  Field,
} from '../components';

// ─────────────────────────────────────────────────────────────────────────────
// EMPTY GUIDED STAGE 1 TEMPLATE DATA (FOR NEW WORKSPACES)
// ─────────────────────────────────────────────────────────────────────────────
export const EMPTY_STAGE1_DATA = {
  overview: {
    name: 'Untitled Idea Workspace',
    description: 'A new guided venture workspace ready for problem framing, validation, and MVP planning.',
    industry: 'Technology',
    stage: 'Idea Stage',
    progress: 0,
    health: 0,
    status: 'draft',
    goals: [
      'Define core customer problem and solution hypothesis',
      'Conduct 10+ target user interviews',
      'Benchmark existing market alternatives',
      'Scope Minimum Viable Product (MVP)',
    ],
    actions: [
      { id: 'a1', label: 'Frame problem statement and target persona', done: false, priority: 'High' },
      { id: 'a2', label: 'Conduct primary customer discovery interviews', done: false, priority: 'High' },
      { id: 'a3', label: 'Document competitive alternatives & moat', done: false, priority: 'Medium' },
      { id: 'a4', label: 'Draft MVP functional specification', done: false, priority: 'Medium' },
    ],
    milestones: [
      { id: 'm1', label: 'Idea defined & problem hypothesis framed', done: false, targetDate: 'Sprint 1' },
      { id: 'm2', label: 'Market research & competitive benchmarking', done: false, targetDate: 'Sprint 2' },
      { id: 'm3', label: 'Validation through customer interviews & surveys', done: false, targetDate: 'Sprint 3' },
      { id: 'm4', label: 'MVP architecture and roadmap planned', done: false, targetDate: 'Sprint 4' },
      { id: 'm5', label: 'Company formation gates satisfied', done: false, targetDate: 'Sprint 5' },
    ],
    criteria: {
      ideaDefined: false,
      marketValidated: false,
      mvpPlanned: false,
      initialTeamIdentified: false,
      fundingPrepDone: false,
    },
  },
  idea: {
    name: 'Untitled Idea',
    problem: '',
    problemTarget: '',
    alternatives: '',
    solution: '',
    uvp: '',
    moat: '',
    targetAudience: '',
    industry: 'Technology',
    category: 'Startup',
    tags: ['New Idea', 'Early Stage'],
  },
  marketResearch: {
    tam: '',
    sam: '',
    som: '',
    cagr: '',
    competitors: [],
    insights: [],
  },
  validation: {
    interviewsCount: 0,
    surveysCount: 0,
    positiveRatio: 0,
    personas: [],
    hypotheses: [],
  },
  mvp: {
    features: [],
    roadmap: [],
    prototypeUrl: '',
  },
  businessModel: {
    revenueStreams: [],
    pricingStrategy: '',
    costDrivers: [],
    unitEconomics: '',
  },
  team: {
    members: [],
    advisors: [],
  },
  funding: {
    targetAmount: '',
    currentRound: 'Pre-Seed',
    investorLeads: [],
  },
  tasks: [],
  documents: [],
  discussions: [],
};

// ─────────────────────────────────────────────────────────────────────────────
// DEFAULT STAGE 1 TEMPLATE DATA
// ─────────────────────────────────────────────────────────────────────────────
export const DEFAULT_STAGE1_DATA = {
  overview: {
    name: 'EcoCart',
    description: 'Sustainable e-commerce for local businesses',
    industry: 'E-commerce & Climate Tech',
    stage: 'Idea Stage',
    progress: 60,
    health: 72,
    goals: [
      'Validate customer willingness to pay',
      'Recruit technical co-founder',
      'Build low-code MVP prototype',
      'Secure 10 pilot vendor commitments',
    ],
    actions: [
      { id: 'a1', label: 'Complete customer interviews', done: true, priority: 'High' },
      { id: 'a2', label: 'Analyze competitor research', done: true, priority: 'Medium' },
      { id: 'a3', label: 'Build MVP prototype', done: false, priority: 'High' },
      { id: 'a4', label: 'Draft product validation survey', done: false, priority: 'Medium' },
    ],
    milestones: [
      { id: 'm1', label: 'Idea defined & problem hypothesis framed', done: true, targetDate: 'Aug 2026' },
      { id: 'm2', label: 'Market research & competitive benchmarking', done: true, targetDate: 'Sep 2026' },
      { id: 'm3', label: 'Validation through customer interviews & surveys', done: false, targetDate: 'Oct 2026' },
      { id: 'm4', label: 'MVP architecture and supplier catalog onboarded', done: false, targetDate: 'Nov 2026' },
      { id: 'm5', label: 'Formal company registration & launch prep', done: false, targetDate: 'Dec 2026' },
    ],
    criteria: {
      ideaDefined: true,
      marketValidated: true,
      mvpPlanned: true,
      initialTeamIdentified: true,
      fundingPrepDone: false,
    },
  },
  idea: {
    name: 'EcoCart',
    problem: 'Independent sustainable producers lack an accessible, transparent distribution network for conscious consumers, resulting in 40% unsold inventory and fragmented customer discovery.',
    problemTarget: 'Local artisanal eco-producers and conscious urban consumers aged 22-45.',
    alternatives: 'Generic marketplaces (high fees, zero eco-verification), farmers markets (low frequency, weather dependent), Instagram DMs (no unified checkout or trust).',
    solution: 'A verified hyper-local e-commerce marketplace connecting verified eco-friendly producers with buyers, featuring carbon-neutral delivery and transparent sustainability proof.',
    uvp: 'Instant 1-click eco-verification + 2-hour zero-emission delivery network for local conscious commerce.',
    moat: 'Proprietary supplier sustainability audit score & localized micro-fulfillment hubs.',
    targetAudience: 'Conscious urban consumers and boutique local sustainable producers.',
    industry: 'E-commerce & Climate Tech',
    category: 'Hyperlocal Sustainable Marketplace',
    tags: ['Sustainability', 'Hyperlocal', 'Marketplace', 'Zero-Waste', 'D2C', 'ClimateTech'],
  },
  marketResearch: {
    tam: '$14.2B',
    sam: '$3.8B',
    som: '$420M',
    cagr: '22.4%',
    trends: 'Rising demand for verifiable carbon footprint tracking; 73% of Gen-Z and Millennials willing to pay a 10-15% premium for verified sustainable products.',
    competitors: [
      {
        id: 'c1',
        name: 'EcoRoot Market',
        marketShare: '18%',
        strengths: 'Established brand and national shipping network',
        weaknesses: 'High commission (22%), slow 4-day delivery',
        pricing: '18-22% take rate',
        advantage: 'Our hyperlocal 2-hour delivery & zero-emission fleet',
      },
      {
        id: 'c2',
        name: 'GreenBasket',
        marketShare: '12%',
        strengths: 'Strong grocery & organic produce catalog',
        weaknesses: 'No non-food eco-products, clunky mobile web experience',
        pricing: '15% take rate',
        advantage: 'Full lifestyle catalog + verified carbon offset badge',
      },
      {
        id: 'c3',
        name: 'LocalCraft Collective',
        marketShare: '8%',
        strengths: 'Good community relationship with artisanal creators',
        weaknesses: 'Manual order dispatching, no live inventory tracking',
        pricing: 'Subscription $29/mo',
        advantage: 'Automated real-time inventory and delivery logistics',
      },
    ],
    segments: [
      {
        id: 's1',
        name: 'Conscious Urban Millennials',
        size: '4.2M target consumers in Tier 1 metros',
        willingnessToPay: 'High (willing to pay up to 15% green premium)',
        painPoint: 'Hard to verify genuine green claims and slow delivery from niche brands.',
      },
      {
        id: 's2',
        name: 'Independent Sustainable Producers',
        size: '18,000+ local boutique brands',
        willingnessToPay: 'Moderate (prefer take-rate over high upfront SaaS)',
        painPoint: 'High logistics friction and customer acquisition cost on big tech platforms.',
      },
      {
        id: 's3',
        name: 'Corporate Sustainable Gifting',
        size: '650+ tech and venture firms',
        willingnessToPay: 'Very High (pre-allocated quarterly CSR budgets)',
        painPoint: 'Need bulk branded packaging with audited environmental impact reports.',
      },
    ],
    swot: {
      strengths: [
        'Proprietary vendor sustainability audit score',
        'Direct relationship with 30+ regional artisan cooperatives',
        'Lightweight serverless tech architecture with low overhead',
      ],
      weaknesses: [
        'Early brand awareness requires strong initial grassroots push',
        'Hyperlocal logistics unit economics need tight cluster density',
        'Requires continuous audit to prevent greenwashing',
      ],
      opportunities: [
        'Corporate sustainable procurement and employee gifting contracts',
        'Carbon-credit micro-rebates for repeat buyer transactions',
        'Expansion to Tier 2 educational & eco-tourism hubs',
      ],
      threats: [
        'Big tech marketplaces introducing pseudo-green filter badges',
        'Supply chain volatility for organic raw materials',
        'Fluctuating localized courier fuel and delivery surcharges',
      ],
    },
    insights: [
      {
        id: 'i1',
        title: 'Carbon Transparency Drives Conversion',
        summary: '84% of surveyed shoppers say seeing certified grams of CO2 offset directly boosts checkout completion by over 30%.',
        source: 'Primary Survey (N=148)',
        date: 'Sep 2026',
      },
      {
        id: 'i2',
        title: 'Vendor Onboarding Bottleneck',
        summary: 'Producers abandon self-onboarding if catalog upload takes > 10 minutes. AI-powered bulk photo scanner reduces friction to 2 minutes.',
        source: 'Supplier Interviews',
        date: 'Sep 2026',
      },
    ],
  },
  validation: {
    metrics: {
      interviewsCount: 36,
      surveysCompleted: 148,
      positiveIntentPct: 89,
      assumptionsRatio: '4 / 5',
      willingnessScore: '84%',
    },
    hypotheses: [
      {
        id: 'h1',
        statement: 'Consumers are willing to wait 2 hours for verified local eco-delivery instead of instant 10-minute generic delivery.',
        risk: 'High',
        status: 'Validated',
        evidence: '91% of interviewees confirmed intentionality over impulse speed for home and lifestyle items.',
      },
      {
        id: 'h2',
        statement: 'Local producers will accept an 8.5% platform commission if logistics and packaging are handled.',
        risk: 'Medium',
        status: 'Validated',
        evidence: '12 out of 14 interviewed vendors signed preliminary Letters of Intent (LOI).',
      },
      {
        id: 'h3',
        statement: 'Customers value verified carbon audit certifications over self-declared eco-labels.',
        risk: 'High',
        status: 'Validated',
        evidence: 'A/B test on mock landing page yielded 2.4x higher clickthrough for third-party audited badge.',
      },
      {
        id: 'h4',
        statement: 'Average basket size will exceed $28 per order to maintain positive unit economics.',
        risk: 'High',
        status: 'Testing',
        evidence: 'Current pilot mock orders average $32.50 across 40 simulated orders.',
      },
      {
        id: 'h5',
        statement: 'Suppliers can maintain same-day order readiness within 45 minutes of customer purchase.',
        risk: 'Medium',
        status: 'In Progress',
        evidence: 'Pilot workflow testing currently in progress with 4 partner merchants.',
      },
    ],
    interviews: [
      {
        id: 'in1',
        name: 'Anita Roy',
        roleSegment: 'Urban Conscious Consumer (Bengaluru)',
        quote: 'I love buying local bamboo products, but having to message Instagram sellers and Pay via UPI is frustrating. A unified cart with quick delivery would make me buy weekly.',
        outcome: 'Strong Validation',
        date: 'Sep 22, 2026',
      },
      {
        id: 'in2',
        name: 'Vikram Joshi',
        roleSegment: 'Founder, EarthWear Apparel',
        quote: 'Mainstream platforms eat 25% of my margin and mix my handcrafted organic linen with synthetic knockoffs. A curated eco-platform is what we have been praying for.',
        outcome: 'Strong Validation',
        date: 'Sep 20, 2026',
      },
      {
        id: 'in3',
        name: 'Rachel Adams',
        roleSegment: 'Corporate ESG Officer',
        quote: 'We spend $15k every festive quarter on employee hampers. If you can provide a downloadable CO2 offset certificate with each order, we will sign an annual vendor contract.',
        outcome: 'Strong Validation',
        date: 'Sep 18, 2026',
      },
    ],
    personas: [
      {
        id: 'p1',
        name: 'Conscious Clara',
        archetype: 'Eco-conscious working professional, 28',
        goals: 'Reduce household plastic waste, support independent makers, transparent sourcing.',
        frustrations: 'Greenwashing, fragmented niche websites, slow cross-country shipping.',
      },
      {
        id: 'p2',
        name: 'Artisan Aarav',
        archetype: 'Small-batch sustainable producer, 34',
        goals: 'Scale orders beyond weekend bazaars, retain healthy gross margins, reliable couriers.',
        frustrations: 'Exorbitant marketplace ad fees, returns fraud, complicated tax paperwork.',
      },
    ],
  },
  mvp: {
    targetDate: '2026-11-15',
    scopeProgress: 70,
    statement: 'Launch pilot web marketplace in Koramangala & Indiranagar with 15 verified eco-producers, zero-emission courier integration, and live carbon metrics.',
    features: [
      {
        id: 'f1',
        title: 'Vendor Onboarding & Audit Score',
        description: 'Self-serve supplier onboarding portal with document upload and automated audit check.',
        priority: 'Must Have',
        status: 'Done',
        effort: '2 weeks',
      },
      {
        id: 'f2',
        title: 'Customer Cart & Unified Multi-Vendor Checkout',
        description: 'Single payment gateway transaction supporting split payouts to multiple vendors.',
        priority: 'Must Have',
        status: 'Done',
        effort: '3 weeks',
      },
      {
        id: 'f3',
        title: 'Verified Carbon Savings Metric Badge',
        description: 'Calculates exact grams of greenhouse gas emissions prevented per product in cart.',
        priority: 'Must Have',
        status: 'Building',
        effort: '1 week',
      },
      {
        id: 'f4',
        title: 'Local Electric Courier Dispatch API',
        description: 'Integration with Porter / Shadowfax EV fleet for automated delivery dispatch.',
        priority: 'Must Have',
        status: 'Building',
        effort: '2 weeks',
      },
      {
        id: 'f5',
        title: 'Customer Review & Authenticity Badges',
        description: 'Verified buyer reviews with photo verification of compostable packaging.',
        priority: 'Should Have',
        status: 'Planned',
        effort: '1 week',
      },
      {
        id: 'f6',
        title: 'Corporate Bulk Order Gifting Portal',
        description: 'Custom portal for corporate gifting orders with automated GST invoices.',
        priority: 'Could Have',
        status: 'Planned',
        effort: '3 weeks',
      },
    ],
    links: [
      {
        id: 'l1',
        title: 'Figma Design System & Mobile Wireframes',
        url: 'https://figma.com/@collobster/ecocart-mvp-design',
        type: 'Figma',
        notes: 'Covers user checkout, supplier dashboard, and carbon metric badges.',
      },
      {
        id: 'l2',
        title: 'Interactive Low-Code Prototype Demo',
        url: 'https://ecocart-preview.collobster.app',
        type: 'Prototype',
        notes: 'Clickable prototype used for customer validation interviews.',
      },
      {
        id: 'l3',
        title: 'MVP Engineering Roadmap & Linear Tracker',
        url: 'https://linear.app/ecocart/mvp-sprint-1',
        type: 'Task Tracker',
        notes: 'Sprint 1 & Sprint 2 task backlog for full-stack engineering.',
      },
    ],
  },
  businessModel: {
    revenueModel: 'Marketplace Commission + Seller SaaS Tier',
    takeRate: '8.5% standard take-rate on buyer checkout',
    pricingTiers: [
      {
        id: 'pt1',
        name: 'Starter Maker',
        price: 'Free',
        interval: 'forever',
        features: ['Up to 25 catalog products', 'Standard 8.5% commission', 'Basic sales analytics', 'Standard EV courier support'],
        popular: false,
      },
      {
        id: 'pt2',
        name: 'Verified Artisan Pro',
        price: '$29',
        interval: '/ month',
        features: ['Unlimited products', 'Reduced 6.0% commission', 'Featured home placement', 'Automated GST invoice generation', 'Direct customer chat'],
        popular: true,
      },
      {
        id: 'pt3',
        name: 'Enterprise Brand',
        price: '$149',
        interval: '/ month',
        features: ['Custom branded storefront', 'Dedicated account manager', 'Bulk corporate sales inclusion', 'Custom carbon audit certificate API'],
        popular: false,
      },
    ],
    costs: [
      { id: 'cs1', title: 'Cloud Infrastructure & Database (Vercel / Supabase)', monthlyCost: '$120', type: 'Fixed', category: 'Tech' },
      { id: 'cs2', title: 'Payment Processing Fees (2% + $0.20 per txn)', monthlyCost: '$350', type: 'Variable', category: 'Operations' },
      { id: 'cs3', title: 'Hyperlocal EV Courier Delivery Subsidies', monthlyCost: '$650', type: 'Variable', category: 'Logistics' },
      { id: 'cs4', title: 'Vendor Sustainability Verification & Audits', monthlyCost: '$400', type: 'Fixed', category: 'Compliance' },
      { id: 'cs5', title: 'Targeted Community & Meta Ads (CAC)', monthlyCost: '$800', type: 'Variable', category: 'Marketing' },
    ],
    unitEconomics: {
      cac: 12.5,
      ltv: 87.5,
      paybackMonths: 2.1,
      grossMarginPct: 74,
      avgOrderValue: 31.0,
    },
    gtmChannels: [
      {
        id: 'gt1',
        channel: 'Hyperlocal Farmers Markets & Popups',
        strategy: 'Set up interactive scan-to-order booths at weekly Bengaluru organic markets.',
        status: 'Active',
        efficiency: 'High (CAC ~$6)',
      },
      {
        id: 'gt2',
        channel: 'Micro-Influencer Eco-Affiliates',
        strategy: 'Partner with 20 zero-waste creators offering 10% discount codes to followers.',
        status: 'Active',
        efficiency: 'Very High (Organic reach)',
      },
      {
        id: 'gt3',
        channel: 'Corporate Wellness & CSR Partnerships',
        strategy: 'Direct B2B pitches to HR teams for plastic-free office supply kits.',
        status: 'In Pipeline',
        efficiency: 'High Ticket LTV',
      },
    ],
  },
  team: {
    members: [
      {
        id: 'tm1',
        name: 'Mohit Koppera',
        role: 'Founder & Product Lead',
        equity: '52%',
        skills: ['Product Architecture', 'Full-stack Dev', 'Growth'],
        bio: 'Tech founder passionate about sustainable supply chains and scalable developer architecture.',
        status: 'Active Co-founder',
      },
      {
        id: 'tm2',
        name: 'Priya Sharma',
        role: 'Co-founder & Head of Supply',
        equity: '28%',
        skills: ['Vendor Relations', 'Retail Operations', 'ESG Auditing'],
        bio: 'Ex-FabIndia supply chain lead with direct network across 100+ Indian artisanal craft guilds.',
        status: 'Active Co-founder',
      },
      {
        id: 'tm3',
        name: 'Arjun Nambiar',
        role: 'Lead Mobile & Frontend Engineer',
        equity: '6%',
        skills: ['React Native', 'TypeScript', 'UI/UX'],
        bio: 'Senior frontend developer who built rapid checkout flows for leading D2C brands.',
        status: 'Early Team',
      },
    ],
    advisors: [
      {
        id: 'adv1',
        name: 'Dr. Ramesh Kulkarni',
        expertise: 'ESG Audits & Sustainability Compliance',
        firm: 'GreenCert Global',
        equity: '1.0%',
        cadence: 'Bi-weekly advisory call',
      },
      {
        id: 'adv2',
        name: 'Shalini Verma',
        expertise: 'B2C E-commerce Scaling & Marketplace Economics',
        firm: 'Ex-VP Flipkart',
        equity: '1.5%',
        cadence: 'Monthly strategy review',
      },
    ],
    openRoles: [
      {
        id: 'or1',
        title: 'Senior Backend & Logistics API Engineer',
        department: 'Engineering',
        type: 'Full-time / Equity',
        requirements: 'Node.js, PostgreSQL, experience with real-time routing or delivery dispatch.',
        status: 'Interviewing',
      },
      {
        id: 'or2',
        title: 'Community & Creator Growth Lead',
        department: 'Marketing',
        type: 'Full-time',
        requirements: 'Experience managing campus ambassador networks and zero-waste creator collaborations.',
        status: 'Open',
      },
    ],
  },
  funding: {
    targetRaise: '$250,000',
    valuationCap: '$2,500,000',
    stage: 'Pre-Seed',
    instrument: 'Post-Money SAFE',
    minCheck: '$10,000',
    committed: '$85,000',
    useOfFunds: [
      { category: 'Product & Engineering', pct: 40, color: '#3b82f6' },
      { category: 'Hyperlocal Logistics & Warehouse', pct: 25, color: '#10b981' },
      { category: 'Vendor Onboarding & Growth', pct: 20, color: '#f59e0b' },
      { category: 'Legal, Compliance & Working Capital', pct: 15, color: '#8b5cf6' },
    ],
    materials: [
      {
        id: 'mat1',
        title: 'EcoCart Pre-Seed Pitch Deck (v2.4)',
        type: 'Presentation',
        url: 'https://docsend.com/view/ecocart-seed-deck',
        status: 'Investor Ready',
        updated: 'Sep 24, 2026',
      },
      {
        id: 'mat2',
        title: '3-Year Financial Model & Unit Economics',
        type: 'Spreadsheet',
        url: 'https://docs.google.com/spreadsheets/d/ecocart-model',
        status: 'Investor Ready',
        updated: 'Sep 21, 2026',
      },
      {
        id: 'mat3',
        title: 'One-Pager Executive Summary',
        type: 'Document',
        url: 'https://notion.so/ecocart-one-pager',
        status: 'Final Review',
        updated: 'Sep 25, 2026',
      },
    ],
    pipeline: [
      {
        id: 'inv1',
        name: 'Ananya Deshmukh',
        firm: 'SeedBloom Climate Ventures',
        stage: 'Term Sheet Discussion',
        amount: '$50,000',
        notes: 'Very excited about our vendor audit score; term sheet expected next Tuesday.',
      },
      {
        id: 'inv2',
        name: 'Rajesh Singhal',
        firm: 'Angel Syndicate Partner',
        stage: 'Committed',
        amount: '$35,000',
        notes: 'SAFE agreement executed; capital wired to escrow account.',
      },
      {
        id: 'inv3',
        name: 'Karan Mehra',
        firm: 'Urban Tech Angels',
        stage: 'Partner Meeting',
        amount: '$25,000',
        notes: 'Follow-up meeting scheduled to review cohort retention metrics.',
      },
      {
        id: 'inv4',
        name: 'Sunita Rao',
        firm: 'GreenWave Capital',
        stage: 'Initial Call',
        amount: '$50,000',
        notes: 'Intro deck sent; awaiting response from associate.',
      },
    ],
    readinessChecklist: [
      { id: 'rc1', label: '12-slide investor pitch deck with verified data', done: true, weight: 20 },
      { id: 'rc2', label: 'Detailed 3-year bottom-up financial projections', done: true, weight: 20 },
      { id: 'rc3', label: 'Customer LOIs and signed vendor pilot agreements', done: true, weight: 20 },
      { id: 'rc4', label: 'Clean cap table and founder IP assignment agreements', done: true, weight: 20 },
      { id: 'rc5', label: 'Virtual data room set up with due diligence docs', done: false, weight: 20 },
    ],
  },
  tasks: [
    {
      id: 't1',
      title: 'Complete 15 merchant catalog photo shoots',
      description: 'Collect high-resolution product photos and ingredient lists from Koramangala makers.',
      priority: 'High',
      column: 'In progress',
      assignee: 'Priya Sharma',
      dueDate: 'Oct 02, 2026',
    },
    {
      id: 't2',
      title: 'Integrate Shadowfax EV webhook API',
      description: 'Set up real-time delivery status push notifications for rider dispatch.',
      priority: 'Urgent',
      column: 'In progress',
      assignee: 'Mohit Koppera',
      dueDate: 'Oct 04, 2026',
    },
    {
      id: 't3',
      title: 'Finalize terms of service and vendor agreement',
      description: 'Review standard vendor marketplace contracts with corporate legal advisor.',
      priority: 'Medium',
      column: 'Review',
      assignee: 'Dr. Ramesh Kulkarni',
      dueDate: 'Oct 05, 2026',
    },
    {
      id: 't4',
      title: 'Deploy landing page with waitlist capture',
      description: 'Publish responsive Next.js landing page with zero-emissions badge.',
      priority: 'High',
      column: 'Done',
      assignee: 'Arjun Nambiar',
      dueDate: 'Sep 26, 2026',
    },
    {
      id: 't5',
      title: 'Draft 5 case studies of local artisans',
      description: 'Interview our first 5 partner makers for Instagram spotlight stories.',
      priority: 'Low',
      column: 'To do',
      assignee: 'Open Role (Growth)',
      dueDate: 'Oct 12, 2026',
    },
  ],
  documents: [
    {
      id: 'doc1',
      title: 'EcoCart Pitch Deck Presentation v2.4',
      category: 'Pitch',
      accessScope: 'Investors & Team',
      url: 'https://docsend.com/view/ecocart-seed-deck',
      size: '8.4 MB · PDF',
      date: 'Sep 24, 2026',
      notes: 'Contains market opportunity, customer interview insights, and financial asks.',
    },
    {
      id: 'doc2',
      title: 'Supplier Sustainability Audit Standards',
      category: 'Research',
      accessScope: 'Team Only',
      url: 'https://notion.so/ecocart/audit-framework',
      size: '2.1 MB · PDF',
      date: 'Sep 18, 2026',
      notes: '15-point criteria checklist used to evaluate prospective vendors.',
    },
    {
      id: 'doc3',
      title: 'Standard Vendor Marketplace Agreement (Draft)',
      category: 'Legal',
      accessScope: 'Team & Advisors',
      url: 'https://docs.google.com/document/d/vendor-contract',
      size: '420 KB · DOCX',
      date: 'Sep 20, 2026',
      notes: 'Reviewed by LawFirm Associates; includes dispute resolution and commission terms.',
    },
    {
      id: 'doc4',
      title: 'Product Architecture & Database Schema Diagram',
      category: 'Product',
      accessScope: 'Team Only',
      url: 'https://figma.com/@collobster/system-arch',
      size: '1.2 MB · PNG',
      date: 'Sep 15, 2026',
      notes: 'Entity relationship diagram covering users, vendors, orders, and delivery tracking.',
    },
  ],
  discussions: [
    {
      id: 'disc1',
      author: 'Mohit Koppera',
      avatar: 'MK',
      title: 'Preliminary Supplier Interviews Complete: 12 LOIs Signed!',
      content: 'We reached our milestone of 15 partner conversations yesterday! 12 artisan brands have signed preliminary LOIs to list their top 5 items on day one. Our next sprint is focused on finalizing the automated product catalog builder.',
      date: 'Yesterday at 4:30 PM',
      tag: 'Milestone',
      pinned: true,
      likes: 5,
      comments: [
        {
          id: 'c1',
          author: 'Priya Sharma',
          content: 'The feedback on the 2-hour delivery was overwhelmingly positive! Most makers said standard shipping was their #1 customer complaint.',
          date: 'Yesterday at 5:15 PM',
        },
      ],
    },
    {
      id: 'disc2',
      author: 'Priya Sharma',
      avatar: 'PS',
      title: 'Draft Founder Agreements Ready for Review',
      content: 'Draft founder agreements and initial cap table calculations have been forwarded to our legal advisor for final sign-off. Please check the Documents tab for the draft file and leave any comments by Friday.',
      date: '2 days ago',
      tag: 'Legal',
      pinned: false,
      likes: 3,
      comments: [],
    },
    {
      id: 'disc3',
      author: 'Arjun Nambiar',
      avatar: 'AN',
      title: 'Checkout Flow Redesign & Carbon Badge Preview',
      content: 'I have uploaded the interactive Figma prototype for the checkout screen with the green badge calculation. Take a look and let me know if the font hierarchy is readable on mobile devices.',
      date: '3 days ago',
      tag: 'Design',
      pinned: false,
      likes: 4,
      comments: [],
    },
  ],
};

// ─────────────────────────────────────────────────────────────────────────────
// CUSTOM HOOK FOR PERSISTENT STAGE 1 DATA
// ─────────────────────────────────────────────────────────────────────────────
export function useStage1Data(app) {
  const STORAGE_KEY = 'collobster_company_stage1_v2';

  const [data, setData] = useState(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved);
        // Merge with empty template to guarantee all properties exist
        return {
          ...EMPTY_STAGE1_DATA,
          ...parsed,
          overview: { ...EMPTY_STAGE1_DATA.overview, ...(parsed.overview || {}) },
          idea: { ...EMPTY_STAGE1_DATA.idea, ...(parsed.idea || {}) },
          marketResearch: { ...EMPTY_STAGE1_DATA.marketResearch, ...(parsed.marketResearch || {}) },
          validation: { ...EMPTY_STAGE1_DATA.validation, ...(parsed.validation || {}) },
          mvp: { ...EMPTY_STAGE1_DATA.mvp, ...(parsed.mvp || {}) },
          businessModel: { ...EMPTY_STAGE1_DATA.businessModel, ...(parsed.businessModel || {}) },
          team: { ...EMPTY_STAGE1_DATA.team, ...(parsed.team || {}) },
          funding: { ...EMPTY_STAGE1_DATA.funding, ...(parsed.funding || {}) },
          tasks: parsed.tasks || EMPTY_STAGE1_DATA.tasks,
          documents: parsed.documents || EMPTY_STAGE1_DATA.documents,
          discussions: parsed.discussions || EMPTY_STAGE1_DATA.discussions,
        };
      }
    } catch (e) {
      console.warn('Failed to load Stage 1 saved data, using empty guided template', e);
    }
    return EMPTY_STAGE1_DATA;
  });

  const save = (updater) => {
    setData((prev) => {
      const next = typeof updater === 'function' ? updater(prev) : { ...prev, ...updater };
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      } catch (e) {
        console.error('Failed to save Stage 1 data to localStorage', e);
      }
      return next;
    });
  };

  const loadSampleTemplate = () => {
    setData(DEFAULT_STAGE1_DATA);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(DEFAULT_STAGE1_DATA));
      app?.toast?.('Loaded EcoCart sample company workspace!');
    } catch (e) {}
  };

  const resetToEmpty = () => {
    setData(EMPTY_STAGE1_DATA);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(EMPTY_STAGE1_DATA));
      app?.toast?.('Reset workspace to clean guided template.');
    } catch (e) {}
  };

  const resetToDefaults = () => {
    loadSampleTemplate();
  };

  return { data, save, resetToDefaults, loadSampleTemplate, resetToEmpty };
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. OVERVIEW PANEL ENHANCEMENT
// ─────────────────────────────────────────────────────────────────────────────
export function OverviewHeaderCard({ overview, onEdit, onOpenOnboarding, onResetDefaults }) {
  return (
    <div className="stage-workspace-header">
      <div className="stage-workspace-title-box">
        <div className="stage-logo-badge stage1">
          <Lightbulb size={24} />
        </div>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
            <h2 style={{ margin: 0 }}>{overview.name}</h2>
            <span className="stage-badge-pill stage1">{overview.stage || 'Idea Stage'}</span>
            <span style={{
              fontSize: '11px',
              fontWeight: 600,
              padding: '2px 8px',
              borderRadius: '10px',
              background: overview.status === 'registered' ? '#dcfce7' : overview.status === 'approved' ? '#e0e7ff' : overview.status === 'submitted' ? '#fef3c7' : '#f1f5f9',
              color: overview.status === 'registered' ? '#166534' : overview.status === 'approved' ? '#3730a3' : overview.status === 'submitted' ? '#92400e' : '#475569',
              textTransform: 'uppercase',
              letterSpacing: '0.04em'
            }}>
              {overview.status || 'Draft'}
            </span>
            <button
              type="button"
              className="edit-pill-btn"
              onClick={onEdit}
              title="Edit Company Basics"
              aria-label="Edit Company Basics"
            >
              <Edit2 size={13} />
              <span>Edit Details</span>
            </button>
          </div>
          <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--muted)' }}>
            {overview.description} · <span style={{ color: 'var(--ink)' }}>{overview.industry}</span>
          </p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '14px', flexWrap: 'wrap' }}>
        <div className="stage-progress-widget" title="Calculated stage progress">
          <span className="stage-progress-label">{overview.progress}%</span>
          <div className="stage-progress-bar-container">
            <div
              className="stage-progress-fill stage1"
              style={{ width: `${overview.progress}%` }}
            />
          </div>
        </div>
        <Button variant="ghost" onClick={onOpenOnboarding}>
          Onboarding Flow
        </Button>
        <button
          type="button"
          className="edit-pill-btn"
          onClick={onResetDefaults}
          title={overview.progress > 0 ? "Reload Sample Template (EcoCart)" : "Load Sample Template (EcoCart)"}
        >
          <RefreshCw size={13} />
          <span>{overview.progress > 0 ? 'Load EcoCart Sample' : 'Load Sample (EcoCart)'}</span>
        </button>
      </div>
    </div>
  );
}

export function OverviewActionsCard({ actions, onToggle, onAdd, onEdit, onDelete }) {
  const [newLabel, setNewLabel] = useState('');
  const [editingItem, setEditingItem] = useState(null);

  const handleAdd = (e) => {
    e.preventDefault();
    if (!newLabel.trim()) return;
    onAdd(newLabel.trim());
    setNewLabel('');
  };

  return (
    <div className="dash-card">
      <div className="dash-card-header">
        <span className="dash-card-title">
          <CheckCircle2 size={16} color="#d97706" /> Next Actions
        </span>
        <span style={{ fontSize: '12px', color: 'var(--muted)' }}>
          {actions.filter((a) => a.done).length} / {actions.length} Done
        </span>
      </div>

      <form onSubmit={handleAdd} style={{ display: 'flex', gap: '6px', margin: '10px 0 12px' }}>
        <input
          type="text"
          value={newLabel}
          onChange={(e) => setNewLabel(e.target.value)}
          placeholder="Add new next action..."
          style={{
            flex: 1,
            padding: '6px 10px',
            fontSize: '12px',
            borderRadius: '6px',
            border: '1px solid var(--line)',
            background: 'var(--surface)',
            color: 'var(--ink)',
          }}
        />
        <Button type="submit" variant="primary" style={{ padding: '6px 12px', fontSize: '12px' }}>
          <Plus size={14} /> Add
        </Button>
      </form>

      <div className="actions-checklist" style={{ maxHeight: '240px', overflowY: 'auto' }}>
        {actions.map((action) => (
          <div
            key={action.id}
            className={`action-check-item ${action.done ? 'done' : ''}`}
            style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '6px 8px' }}
          >
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', flex: 1, minWidth: 0 }}>
              <input
                type="checkbox"
                checked={action.done}
                onChange={() => onToggle(action.id)}
              />
              <span style={{ fontSize: '12px', wordBreak: 'break-word' }}>{action.label}</span>
            </label>
            <div style={{ display: 'flex', alignItems: 'center', gap: '4px', flexShrink: 0 }}>
              <button
                type="button"
                className="icon-subtle-btn"
                onClick={() => setEditingItem(action)}
                title="Edit action"
              >
                <Edit2 size={12} />
              </button>
              <button
                type="button"
                className="icon-subtle-btn danger"
                onClick={() => onDelete(action.id)}
                title="Delete action"
              >
                <Trash2 size={12} />
              </button>
            </div>
          </div>
        ))}
      </div>

      {editingItem && (
        <Dialog title="Edit Next Action" onClose={() => setEditingItem(null)}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <Field label="Action Description">
              <input
                value={editingItem.label}
                onChange={(e) => setEditingItem({ ...editingItem, label: e.target.value })}
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setEditingItem(null)}>Cancel</Button>
              <Button
                variant="primary"
                onClick={() => {
                  onEdit(editingItem);
                  setEditingItem(null);
                }}
              >
                Save Action
              </Button>
            </div>
          </div>
        </Dialog>
      )}
    </div>
  );
}

export function OverviewHealthCard({ health, onEditHealth }) {
  const getStatusText = (h) => {
    if (h >= 75) return 'Strong Validation';
    if (h >= 50) return 'Good Progress';
    return 'Needs Attention';
  };

  return (
    <div className="dash-card">
      <div className="dash-card-header">
        <span className="dash-card-title">
          <Sparkles size={16} color="#059669" /> Idea Health
        </span>
        <button
          type="button"
          className="edit-pill-btn"
          onClick={onEditHealth}
          title="Adjust health score"
        >
          <Edit2 size={12} />
          <span>Adjust</span>
        </button>
      </div>

      <div className="idea-health-gauge">
        <svg className="gauge-svg" viewBox="0 0 100 100">
          <circle className="gauge-bg" cx="50" cy="50" r="40" />
          <circle
            className="gauge-meter"
            cx="50"
            cy="50"
            r="40"
            strokeDasharray="251.2"
            strokeDashoffset={251.2 * (1 - health / 100)}
          />
          <text className="gauge-text" x="50" y="52">
            {health}%
          </text>
        </svg>
        <div className="gauge-status">{getStatusText(health)}</div>
      </div>
      <p style={{ margin: '8px 0 0', fontSize: '11px', textAlign: 'center', color: 'var(--muted)' }}>
        Based on user interviews, market TAM size, and assumptions verified.
      </p>
    </div>
  );
}

export function OverviewMilestonesCard({ milestones, onToggle, onAdd, onEdit, onDelete }) {
  const [newLabel, setNewLabel] = useState('');
  const [editingItem, setEditingItem] = useState(null);

  const handleAdd = (e) => {
    e.preventDefault();
    if (!newLabel.trim()) return;
    onAdd(newLabel.trim());
    setNewLabel('');
  };

  return (
    <div className="dash-card">
      <div className="dash-card-header">
        <span className="dash-card-title">
          <Award size={16} color="#7c3aed" /> Milestones
        </span>
        <span style={{ fontSize: '12px', color: 'var(--muted)' }}>
          {milestones.filter((m) => m.done).length} / {milestones.length} Reached
        </span>
      </div>

      <form onSubmit={handleAdd} style={{ display: 'flex', gap: '6px', margin: '10px 0 12px' }}>
        <input
          type="text"
          value={newLabel}
          onChange={(e) => setNewLabel(e.target.value)}
          placeholder="Add new milestone..."
          style={{
            flex: 1,
            padding: '6px 10px',
            fontSize: '12px',
            borderRadius: '6px',
            border: '1px solid var(--line)',
            background: 'var(--surface)',
            color: 'var(--ink)',
          }}
        />
        <Button type="submit" variant="primary" style={{ padding: '6px 12px', fontSize: '12px' }}>
          <Plus size={14} /> Add
        </Button>
      </form>

      <div className="actions-checklist" style={{ maxHeight: '240px', overflowY: 'auto' }}>
        {milestones.map((m) => (
          <div
            key={m.id}
            className={`action-check-item ${m.done ? 'done' : ''}`}
            style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '6px 8px' }}
          >
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', flex: 1, minWidth: 0 }}>
              <input
                type="checkbox"
                checked={m.done}
                onChange={() => onToggle(m.id)}
              />
              <span style={{ fontSize: '12px', wordBreak: 'break-word' }}>
                {m.label} {m.targetDate ? <small style={{ color: 'var(--muted)' }}>({m.targetDate})</small> : ''}
              </span>
            </label>
            <div style={{ display: 'flex', alignItems: 'center', gap: '4px', flexShrink: 0 }}>
              <button
                type="button"
                className="icon-subtle-btn"
                onClick={() => setEditingItem(m)}
                title="Edit milestone"
              >
                <Edit2 size={12} />
              </button>
              <button
                type="button"
                className="icon-subtle-btn danger"
                onClick={() => onDelete(m.id)}
                title="Delete milestone"
              >
                <Trash2 size={12} />
              </button>
            </div>
          </div>
        ))}
      </div>

      {editingItem && (
        <Dialog title="Edit Milestone" onClose={() => setEditingItem(null)}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <Field label="Milestone Title">
              <input
                value={editingItem.label}
                onChange={(e) => setEditingItem({ ...editingItem, label: e.target.value })}
              />
            </Field>
            <Field label="Target Date / Quarter">
              <input
                value={editingItem.targetDate || ''}
                onChange={(e) => setEditingItem({ ...editingItem, targetDate: e.target.value })}
                placeholder="e.g. Q4 2026 or Nov 2026"
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setEditingItem(null)}>Cancel</Button>
              <Button
                variant="primary"
                onClick={() => {
                  onEdit(editingItem);
                  setEditingItem(null);
                }}
              >
                Save Milestone
              </Button>
            </div>
          </div>
        </Dialog>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. IDEA MANAGEMENT PANEL (FULL CRUD)
// ─────────────────────────────────────────────────────────────────────────────
export function IdeaManagementPanelEnhanced({ idea, onSave, app }) {
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState(idea);
  const [newTag, setNewTag] = useState('');

  useEffect(() => {
    setForm(idea);
  }, [idea]);

  const handleSave = (e) => {
    e.preventDefault();
    onSave(form);
    setEditing(false);
    app?.toast?.('Idea details updated successfully!');
  };

  const handleAddTag = (e) => {
    e.preventDefault();
    if (!newTag.trim()) return;
    if (!form.tags.includes(newTag.trim())) {
      setForm({ ...form, tags: [...form.tags, newTag.trim()] });
    }
    setNewTag('');
  };

  const handleRemoveTag = (tagToRemove) => {
    setForm({ ...form, tags: form.tags.filter((t) => t !== tagToRemove) });
  };

  return (
    <section className="dash-card">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
        <div>
          <SectionTitle title="Idea Management & Formulation" />
          <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--muted)' }}>
            Define and refine the core problem hypothesis, solution architecture, target audience, and unique value proposition.
          </p>
        </div>
        <Button
          variant={editing ? 'secondary' : 'primary'}
          icon={editing ? X : Edit2}
          onClick={() => {
            if (editing) setForm(idea);
            setEditing(!editing);
          }}
        >
          {editing ? 'Cancel' : 'Edit Idea'}
        </Button>
      </div>

      {editing ? (
        <form onSubmit={handleSave} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '14px' }}>
            <Field label="Idea / Company Name">
              <input
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
              />
            </Field>
            <Field label="Industry & Vertical">
              <input
                value={form.industry}
                onChange={(e) => setForm({ ...form, industry: e.target.value })}
                required
              />
            </Field>
          </div>

          <Field label="Problem Statement">
            <textarea
              rows={3}
              value={form.problem}
              onChange={(e) => setForm({ ...form, problem: e.target.value })}
              required
            />
          </Field>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '14px' }}>
            <Field label="Who Experiences This Problem? (Target Audience)">
              <textarea
                rows={2}
                value={form.problemTarget}
                onChange={(e) => setForm({ ...form, problemTarget: e.target.value })}
              />
            </Field>
            <Field label="Current Ineffective Alternatives">
              <textarea
                rows={2}
                value={form.alternatives}
                onChange={(e) => setForm({ ...form, alternatives: e.target.value })}
              />
            </Field>
          </div>

          <Field label="Proposed Solution Definition">
            <textarea
              rows={3}
              value={form.solution}
              onChange={(e) => setForm({ ...form, solution: e.target.value })}
              required
            />
          </Field>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '14px' }}>
            <Field label="Unique Value Proposition (UVP)">
              <textarea
                rows={2}
                value={form.uvp}
                onChange={(e) => setForm({ ...form, uvp: e.target.value })}
              />
            </Field>
            <Field label="Defensible Moat / Unfair Advantage">
              <textarea
                rows={2}
                value={form.moat}
                onChange={(e) => setForm({ ...form, moat: e.target.value })}
              />
            </Field>
          </div>

          {/* Tags management */}
          <div>
            <label style={{ fontSize: '12px', fontWeight: 600, display: 'block', marginBottom: '6px' }}>
              Keywords & Focus Tags
            </label>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', marginBottom: '8px' }}>
              {form.tags.map((tag) => (
                <span
                  key={tag}
                  style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '4px',
                    padding: '4px 10px',
                    borderRadius: '16px',
                    background: 'var(--soft)',
                    border: '1px solid var(--line)',
                    fontSize: '12px',
                  }}
                >
                  #{tag}
                  <button
                    type="button"
                    onClick={() => handleRemoveTag(tag)}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0, display: 'flex' }}
                  >
                    <X size={12} />
                  </button>
                </span>
              ))}
            </div>
            <div style={{ display: 'flex', gap: '6px', maxWidth: '360px' }}>
              <input
                type="text"
                placeholder="Add new tag..."
                value={newTag}
                onChange={(e) => setNewTag(e.target.value)}
                style={{
                  flex: 1,
                  padding: '6px 10px',
                  fontSize: '12px',
                  borderRadius: '6px',
                  border: '1px solid var(--line)',
                }}
              />
              <Button type="button" variant="secondary" onClick={handleAddTag} style={{ fontSize: '12px' }}>
                <Plus size={14} /> Add Tag
              </Button>
            </div>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '12px' }}>
            <Button variant="ghost" onClick={() => setEditing(false)}>Cancel</Button>
            <Button type="submit" variant="primary" icon={Check}>
              Save Idea Changes
            </Button>
          </div>
        </form>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {/* Main 2-column cards */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '16px' }}>
            <div className="dash-card" style={{ background: 'var(--soft)', borderLeft: '4px solid #ef4444' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                <AlertCircle size={16} color="#ef4444" />
                <b style={{ fontSize: '14px' }}>Problem Statement</b>
              </div>
              <p style={{ fontSize: '13px', lineHeight: 1.5, margin: 0 }}>
                {idea.problem}
              </p>
              <div style={{ marginTop: '12px', borderTop: '1px solid var(--line)', paddingTop: '8px' }}>
                <small style={{ color: 'var(--muted)', display: 'block' }}>Who is affected:</small>
                <span style={{ fontSize: '12px', color: 'var(--ink)' }}>{idea.problemTarget}</span>
              </div>
              <div style={{ marginTop: '8px' }}>
                <small style={{ color: 'var(--muted)', display: 'block' }}>Why alternatives fail:</small>
                <span style={{ fontSize: '12px', color: 'var(--ink)' }}>{idea.alternatives}</span>
              </div>
            </div>

            <div className="dash-card" style={{ background: 'var(--soft)', borderLeft: '4px solid #10b981' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                <CheckCircle2 size={16} color="#10b981" />
                <b style={{ fontSize: '14px' }}>Solution Architecture</b>
              </div>
              <p style={{ fontSize: '13px', lineHeight: 1.5, margin: 0 }}>
                {idea.solution}
              </p>
              <div style={{ marginTop: '12px', borderTop: '1px solid var(--line)', paddingTop: '8px' }}>
                <small style={{ color: 'var(--muted)', display: 'block' }}>Unique Value Proposition:</small>
                <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--ink)' }}>{idea.uvp}</span>
              </div>
              <div style={{ marginTop: '8px' }}>
                <small style={{ color: 'var(--muted)', display: 'block' }}>Defensible Moat:</small>
                <span style={{ fontSize: '12px', color: 'var(--ink)' }}>{idea.moat}</span>
              </div>
            </div>
          </div>

          {/* Metadata pill bar */}
          <div
            style={{
              padding: '12px 16px',
              borderRadius: '8px',
              background: 'var(--surface)',
              border: '1px solid var(--line)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              flexWrap: 'wrap',
              gap: '12px',
            }}
          >
            <div>
              <small style={{ color: 'var(--muted)', display: 'block' }}>Industry & Category</small>
              <span style={{ fontSize: '13px', fontWeight: 600 }}>{idea.industry} · {idea.category || 'General'}</span>
            </div>
            <div>
              <small style={{ color: 'var(--muted)', display: 'block', marginBottom: '4px' }}>Keywords & Tags</small>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                {idea.tags.map((t) => (
                  <Tag key={t} tone="purple">#{t}</Tag>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. MARKET RESEARCH PANEL (FULL CRUD)
// ─────────────────────────────────────────────────────────────────────────────
export function MarketResearchPanelEnhanced({ marketResearch, onSave, app }) {
  const [data, setData] = useState(marketResearch);
  const [editingSizes, setEditingSizes] = useState(false);
  const [competitorModal, setCompetitorModal] = useState({ open: false, item: null });
  const [segmentModal, setSegmentModal] = useState({ open: false, item: null });
  const [insightModal, setInsightModal] = useState({ open: false, item: null });
  const [swotModal, setSwotModal] = useState({ open: false, type: 'strengths', text: '' });

  useEffect(() => {
    setData(marketResearch);
  }, [marketResearch]);

  const updateAndSave = (next) => {
    setData(next);
    onSave(next);
  };

  // Competitor operations
  const saveCompetitor = (competitor) => {
    const nextList = competitor.id
      ? data.competitors.map((c) => (c.id === competitor.id ? competitor : c))
      : [...data.competitors, { ...competitor, id: 'c_' + Date.now() }];
    updateAndSave({ ...data, competitors: nextList });
    setCompetitorModal({ open: false, item: null });
    app?.toast?.('Competitor saved.');
  };

  const deleteCompetitor = (id) => {
    updateAndSave({ ...data, competitors: data.competitors.filter((c) => c.id !== id) });
    app?.toast?.('Competitor removed.');
  };

  // Customer Segment operations
  const saveSegment = (segment) => {
    const nextList = segment.id
      ? data.segments.map((s) => (s.id === segment.id ? segment : s))
      : [...data.segments, { ...segment, id: 's_' + Date.now() }];
    updateAndSave({ ...data, segments: nextList });
    setSegmentModal({ open: false, item: null });
    app?.toast?.('Customer segment saved.');
  };

  const deleteSegment = (id) => {
    updateAndSave({ ...data, segments: data.segments.filter((s) => s.id !== id) });
    app?.toast?.('Segment removed.');
  };

  // SWOT operations
  const addSwotItem = (type, text) => {
    if (!text.trim()) return;
    const currentList = data.swot[type] || [];
    updateAndSave({
      ...data,
      swot: { ...data.swot, [type]: [...currentList, text.trim()] },
    });
    setSwotModal({ open: false, type: 'strengths', text: '' });
  };

  const removeSwotItem = (type, index) => {
    const nextList = data.swot[type].filter((_, idx) => idx !== index);
    updateAndSave({
      ...data,
      swot: { ...data.swot, [type]: nextList },
    });
  };

  return (
    <section className="dash-card">
      <SectionTitle title="Market Research & Competitive Intelligence" />
      <p style={{ margin: '4px 0 16px', fontSize: '13px', color: 'var(--muted)' }}>
        Analyze addressable market size (TAM/SAM/SOM), benchmark direct competitors, map customer segments, and maintain SWOT analysis.
      </p>

      {/* 1. Market Size Metric Cards with Edit Modal */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
        <h4 style={{ margin: 0, fontSize: '14px' }}>Market Opportunity & Sizing</h4>
        <button
          type="button"
          className="edit-pill-btn"
          onClick={() => setEditingSizes(true)}
        >
          <Edit2 size={13} />
          <span>Edit Sizing & Trends</span>
        </button>
      </div>

      <div className="dashboard-grid-4" style={{ marginBottom: '20px' }}>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <span className="kpi-header">TAM (Total Addressable)</span>
          <span className="kpi-value" style={{ color: '#2563eb' }}>{data.tam}</span>
          <span className="kpi-trend">Global / National potential</span>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <span className="kpi-header">SAM (Serviceable)</span>
          <span className="kpi-value" style={{ color: '#7c3aed' }}>{data.sam}</span>
          <span className="kpi-trend">Regional target segment</span>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <span className="kpi-header">SOM (Obtainable in 3Y)</span>
          <span className="kpi-value" style={{ color: '#059669' }}>{data.som}</span>
          <span className="kpi-trend">Initial market capture</span>
        </div>
        <div className="dash-card" style={{ background: 'var(--soft)' }}>
          <span className="kpi-header">Annual Growth Rate</span>
          <span className="kpi-value" style={{ color: '#d97706' }}>{data.cagr}</span>
          <span className="kpi-trend">CAGR across eco-retail</span>
        </div>
      </div>

      <div style={{ padding: '12px 14px', borderRadius: '8px', background: 'var(--surface)', border: '1px solid var(--line)', marginBottom: '24px' }}>
        <b style={{ fontSize: '12px', display: 'block', color: 'var(--muted)', marginBottom: '4px' }}>Market Trends & Drivers:</b>
        <p style={{ margin: 0, fontSize: '13px', lineHeight: 1.5 }}>{data.trends}</p>
      </div>

      {/* 2. Competitor Benchmarking */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
        <div>
          <h4 style={{ margin: 0, fontSize: '14px' }}>Competitor Analysis ({data.competitors?.length || 0})</h4>
          <small style={{ color: 'var(--muted)' }}>Direct, indirect, and emerging market competitors</small>
        </div>
        <Button
          variant="secondary"
          icon={Plus}
          onClick={() => setCompetitorModal({ open: true, item: { name: '', marketShare: '', strengths: '', weaknesses: '', pricing: '', advantage: '' } })}
        >
          Add Competitor
        </Button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(290px, 1fr))', gap: '14px', marginBottom: '24px' }}>
        {data.competitors?.map((comp) => (
          <div key={comp.id} className="dash-card" style={{ position: 'relative', border: '1px solid var(--line)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
              <div>
                <b style={{ fontSize: '15px' }}>{comp.name}</b>
                <span style={{ display: 'block', fontSize: '11px', color: 'var(--muted)' }}>Market Share: {comp.marketShare || 'N/A'} · Pricing: {comp.pricing || 'N/A'}</span>
              </div>
              <div style={{ display: 'flex', gap: '4px' }}>
                <button
                  type="button"
                  className="icon-subtle-btn"
                  onClick={() => setCompetitorModal({ open: true, item: comp })}
                  title="Edit competitor"
                >
                  <Edit2 size={13} />
                </button>
                <button
                  type="button"
                  className="icon-subtle-btn danger"
                  onClick={() => deleteCompetitor(comp.id)}
                  title="Delete competitor"
                >
                  <Trash2 size={13} />
                </button>
              </div>
            </div>

            <div style={{ fontSize: '12px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
              <div>
                <span style={{ color: '#059669', fontWeight: 600 }}>Strengths: </span>
                <span>{comp.strengths}</span>
              </div>
              <div>
                <span style={{ color: '#dc2626', fontWeight: 600 }}>Weaknesses: </span>
                <span>{comp.weaknesses}</span>
              </div>
              <div style={{ marginTop: '4px', paddingTop: '6px', borderTop: '1px dashed var(--line)' }}>
                <span style={{ color: '#7c3aed', fontWeight: 600 }}>Our Advantage: </span>
                <span>{comp.advantage}</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* 3. Customer Segments */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
        <div>
          <h4 style={{ margin: 0, fontSize: '14px' }}>Target Customer Segments</h4>
          <small style={{ color: 'var(--muted)' }}>Who has the highest urgency and willingness to pay?</small>
        </div>
        <Button
          variant="secondary"
          icon={Plus}
          onClick={() => setSegmentModal({ open: true, item: { name: '', size: '', willingnessToPay: '', painPoint: '' } })}
        >
          Add Segment
        </Button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '14px', marginBottom: '24px' }}>
        {data.segments?.map((seg) => (
          <div key={seg.id} className="dash-card" style={{ background: 'var(--soft)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '6px' }}>
              <b style={{ fontSize: '14px' }}>{seg.name}</b>
              <div style={{ display: 'flex', gap: '4px' }}>
                <button
                  type="button"
                  className="icon-subtle-btn"
                  onClick={() => setSegmentModal({ open: true, item: seg })}
                >
                  <Edit2 size={12} />
                </button>
                <button
                  type="button"
                  className="icon-subtle-btn danger"
                  onClick={() => deleteSegment(seg.id)}
                >
                  <Trash2 size={12} />
                </button>
              </div>
            </div>
            <div style={{ fontSize: '12px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
              <span style={{ color: 'var(--muted)' }}>Est. Size: {seg.size}</span>
              <span><strong>Willingness to Pay:</strong> {seg.willingnessToPay}</span>
              <span><strong>Core Pain Point:</strong> {seg.painPoint}</span>
            </div>
          </div>
        ))}
      </div>

      {/* 4. SWOT Analysis Matrix (4 Quadrants with Add/Remove) */}
      <h4 style={{ margin: '0 0 10px', fontSize: '14px' }}>Interactive SWOT Analysis</h4>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '14px' }}>
        {/* Strengths */}
        <div className="dash-card" style={{ borderTop: '4px solid #10b981' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <b style={{ color: '#047857' }}>Strengths (Internal)</b>
            <button
              type="button"
              className="icon-subtle-btn"
              onClick={() => setSwotModal({ open: true, type: 'strengths', text: '' })}
              title="Add strength"
            >
              <Plus size={14} />
            </button>
          </div>
          <ul style={{ margin: 0, paddingLeft: '18px', fontSize: '12px', lineHeight: 1.6 }}>
            {data.swot?.strengths?.map((item, idx) => (
              <li key={idx} style={{ marginBottom: '4px' }}>
                <span>{item}</span>{' '}
                <button
                  type="button"
                  onClick={() => removeSwotItem('strengths', idx)}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--muted)', padding: 0 }}
                >
                  ×
                </button>
              </li>
            ))}
          </ul>
        </div>

        {/* Weaknesses */}
        <div className="dash-card" style={{ borderTop: '4px solid #ef4444' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <b style={{ color: '#b91c1c' }}>Weaknesses (Internal)</b>
            <button
              type="button"
              className="icon-subtle-btn"
              onClick={() => setSwotModal({ open: true, type: 'weaknesses', text: '' })}
              title="Add weakness"
            >
              <Plus size={14} />
            </button>
          </div>
          <ul style={{ margin: 0, paddingLeft: '18px', fontSize: '12px', lineHeight: 1.6 }}>
            {data.swot?.weaknesses?.map((item, idx) => (
              <li key={idx} style={{ marginBottom: '4px' }}>
                <span>{item}</span>{' '}
                <button
                  type="button"
                  onClick={() => removeSwotItem('weaknesses', idx)}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--muted)', padding: 0 }}
                >
                  ×
                </button>
              </li>
            ))}
          </ul>
        </div>

        {/* Opportunities */}
        <div className="dash-card" style={{ borderTop: '4px solid #3b82f6' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <b style={{ color: '#1d4ed8' }}>Opportunities (External)</b>
            <button
              type="button"
              className="icon-subtle-btn"
              onClick={() => setSwotModal({ open: true, type: 'opportunities', text: '' })}
              title="Add opportunity"
            >
              <Plus size={14} />
            </button>
          </div>
          <ul style={{ margin: 0, paddingLeft: '18px', fontSize: '12px', lineHeight: 1.6 }}>
            {data.swot?.opportunities?.map((item, idx) => (
              <li key={idx} style={{ marginBottom: '4px' }}>
                <span>{item}</span>{' '}
                <button
                  type="button"
                  onClick={() => removeSwotItem('opportunities', idx)}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--muted)', padding: 0 }}
                >
                  ×
                </button>
              </li>
            ))}
          </ul>
        </div>

        {/* Threats */}
        <div className="dash-card" style={{ borderTop: '4px solid #f59e0b' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
            <b style={{ color: '#b45309' }}>Threats (External)</b>
            <button
              type="button"
              className="icon-subtle-btn"
              onClick={() => setSwotModal({ open: true, type: 'threats', text: '' })}
              title="Add threat"
            >
              <Plus size={14} />
            </button>
          </div>
          <ul style={{ margin: 0, paddingLeft: '18px', fontSize: '12px', lineHeight: 1.6 }}>
            {data.swot?.threats?.map((item, idx) => (
              <li key={idx} style={{ marginBottom: '4px' }}>
                <span>{item}</span>{' '}
                <button
                  type="button"
                  onClick={() => removeSwotItem('threats', idx)}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--muted)', padding: 0 }}
                >
                  ×
                </button>
              </li>
            ))}
          </ul>
        </div>
      </div>

      {/* MODALS */}
      {/* Edit Market Sizing Modal */}
      {editingSizes && (
        <Dialog title="Edit Market Sizing & Trends" onClose={() => setEditingSizes(false)}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <Field label="TAM (Total Addressable Market)">
                <input
                  value={data.tam}
                  onChange={(e) => setData({ ...data, tam: e.target.value })}
                />
              </Field>
              <Field label="SAM (Serviceable Market)">
                <input
                  value={data.sam}
                  onChange={(e) => setData({ ...data, sam: e.target.value })}
                />
              </Field>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <Field label="SOM (Obtainable Market)">
                <input
                  value={data.som}
                  onChange={(e) => setData({ ...data, som: e.target.value })}
                />
              </Field>
              <Field label="Annual CAGR Growth %">
                <input
                  value={data.cagr}
                  onChange={(e) => setData({ ...data, cagr: e.target.value })}
                />
              </Field>
            </div>
            <Field label="Market Trends & Key Drivers">
              <textarea
                rows={3}
                value={data.trends}
                onChange={(e) => setData({ ...data, trends: e.target.value })}
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setEditingSizes(false)}>Cancel</Button>
              <Button
                variant="primary"
                onClick={() => {
                  updateAndSave(data);
                  setEditingSizes(false);
                  app?.toast?.('Market sizing updated!');
                }}
              >
                Save Sizing
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Add/Edit Competitor Modal */}
      {competitorModal.open && (
        <Dialog
          title={competitorModal.item?.id ? 'Edit Competitor' : 'Add New Competitor'}
          onClose={() => setCompetitorModal({ open: false, item: null })}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '10px' }}>
              <Field label="Competitor Name">
                <input
                  value={competitorModal.item?.name || ''}
                  onChange={(e) => setCompetitorModal({ ...competitorModal, item: { ...competitorModal.item, name: e.target.value } })}
                  placeholder="e.g. EcoRoot Market"
                  required
                />
              </Field>
              <Field label="Est. Market Share">
                <input
                  value={competitorModal.item?.marketShare || ''}
                  onChange={(e) => setCompetitorModal({ ...competitorModal, item: { ...competitorModal.item, marketShare: e.target.value } })}
                  placeholder="e.g. 15%"
                />
              </Field>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Pricing / Fee Model">
                <input
                  value={competitorModal.item?.pricing || ''}
                  onChange={(e) => setCompetitorModal({ ...competitorModal, item: { ...competitorModal.item, pricing: e.target.value } })}
                  placeholder="e.g. 18% commission"
                />
              </Field>
              <Field label="Our Direct Advantage">
                <input
                  value={competitorModal.item?.advantage || ''}
                  onChange={(e) => setCompetitorModal({ ...competitorModal, item: { ...competitorModal.item, advantage: e.target.value } })}
                  placeholder="e.g. 2-hr delivery & verified proof"
                />
              </Field>
            </div>
            <Field label="Key Strengths">
              <textarea
                rows={2}
                value={competitorModal.item?.strengths || ''}
                onChange={(e) => setCompetitorModal({ ...competitorModal, item: { ...competitorModal.item, strengths: e.target.value } })}
              />
            </Field>
            <Field label="Key Weaknesses">
              <textarea
                rows={2}
                value={competitorModal.item?.weaknesses || ''}
                onChange={(e) => setCompetitorModal({ ...competitorModal, item: { ...competitorModal.item, weaknesses: e.target.value } })}
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '8px' }}>
              <Button variant="ghost" onClick={() => setCompetitorModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveCompetitor(competitorModal.item)}>
                Save Competitor
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Add/Edit Segment Modal */}
      {segmentModal.open && (
        <Dialog
          title={segmentModal.item?.id ? 'Edit Customer Segment' : 'Add Customer Segment'}
          onClose={() => setSegmentModal({ open: false, item: null })}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Segment Name">
              <input
                value={segmentModal.item?.name || ''}
                onChange={(e) => setSegmentModal({ ...segmentModal, item: { ...segmentModal.item, name: e.target.value } })}
                placeholder="e.g. Conscious Urban Millennials"
                required
              />
            </Field>
            <Field label="Estimated Segment Size">
              <input
                value={segmentModal.item?.size || ''}
                onChange={(e) => setSegmentModal({ ...segmentModal, item: { ...segmentModal.item, size: e.target.value } })}
                placeholder="e.g. 4.2M in metro areas"
              />
            </Field>
            <Field label="Willingness to Pay">
              <input
                value={segmentModal.item?.willingnessToPay || ''}
                onChange={(e) => setSegmentModal({ ...segmentModal, item: { ...segmentModal.item, willingnessToPay: e.target.value } })}
                placeholder="e.g. High (willing to pay 10-15% premium)"
              />
            </Field>
            <Field label="Core Pain Point">
              <textarea
                rows={2}
                value={segmentModal.item?.painPoint || ''}
                onChange={(e) => setSegmentModal({ ...segmentModal, item: { ...segmentModal.item, painPoint: e.target.value } })}
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setSegmentModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveSegment(segmentModal.item)}>Save Segment</Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Add SWOT Item Modal */}
      {swotModal.open && (
        <Dialog title={`Add ${swotModal.type.slice(0, -1)}`} onClose={() => setSwotModal({ open: false, type: 'strengths', text: '' })}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Point Description">
              <input
                value={swotModal.text}
                onChange={(e) => setSwotModal({ ...swotModal, text: e.target.value })}
                placeholder="Enter concise bullet point..."
                autoFocus
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setSwotModal({ open: false, type: 'strengths', text: '' })}>Cancel</Button>
              <Button variant="primary" onClick={() => addSwotItem(swotModal.type, swotModal.text)}>Add Point</Button>
            </div>
          </div>
        </Dialog>
      )}
    </section>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. VALIDATION PANEL (FULL CRUD)
// ─────────────────────────────────────────────────────────────────────────────
export function ValidationPanelEnhanced({ validation, onSave, app }) {
  const [data, setData] = useState(validation);
  const [editingMetrics, setEditingMetrics] = useState(false);
  const [hypothesisModal, setHypothesisModal] = useState({ open: false, item: null });
  const [interviewModal, setInterviewModal] = useState({ open: false, item: null });
  const [personaModal, setPersonaModal] = useState({ open: false, item: null });

  useEffect(() => {
    setData(validation);
  }, [validation]);

  const updateAndSave = (next) => {
    setData(next);
    onSave(next);
  };

  const saveHypothesis = (hypo) => {
    const nextList = hypo.id
      ? data.hypotheses.map((h) => (h.id === hypo.id ? hypo : h))
      : [...data.hypotheses, { ...hypo, id: 'h_' + Date.now() }];
    updateAndSave({ ...data, hypotheses: nextList });
    setHypothesisModal({ open: false, item: null });
    app?.toast?.('Hypothesis saved.');
  };

  const deleteHypothesis = (id) => {
    updateAndSave({ ...data, hypotheses: data.hypotheses.filter((h) => h.id !== id) });
    app?.toast?.('Hypothesis removed.');
  };

  const saveInterview = (item) => {
    const nextList = item.id
      ? data.interviews.map((i) => (i.id === item.id ? item : i))
      : [...data.interviews, { ...item, id: 'in_' + Date.now(), date: item.date || 'Today' }];
    updateAndSave({ ...data, interviews: nextList });
    setInterviewModal({ open: false, item: null });
    app?.toast?.('Interview feedback recorded.');
  };

  const deleteInterview = (id) => {
    updateAndSave({ ...data, interviews: data.interviews.filter((i) => i.id !== id) });
    app?.toast?.('Interview record deleted.');
  };

  const savePersona = (persona) => {
    const nextList = persona.id
      ? data.personas.map((p) => (p.id === persona.id ? persona : p))
      : [...data.personas, { ...persona, id: 'p_' + Date.now() }];
    updateAndSave({ ...data, personas: nextList });
    setPersonaModal({ open: false, item: null });
    app?.toast?.('User persona saved.');
  };

  const deletePersona = (id) => {
    updateAndSave({ ...data, personas: data.personas.filter((p) => p.id !== id) });
    app?.toast?.('Persona removed.');
  };

  return (
    <section className="dash-card">
      <SectionTitle title="Validation & Customer Discovery Feedback" />
      <p style={{ margin: '4px 0 16px', fontSize: '13px', color: 'var(--muted)' }}>
        Track user interviews, survey data, key hypotheses testing status, and actual feedback records.
      </p>

      {/* Top Validation KPIs with Edit Trigger */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
        <h4 style={{ margin: 0, fontSize: '14px' }}>Validation Metrics Summary</h4>
        <button
          type="button"
          className="edit-pill-btn"
          onClick={() => setEditingMetrics(true)}
        >
          <Edit2 size={13} />
          <span>Edit Metrics</span>
        </button>
      </div>

      <div className="dashboard-grid-4" style={{ marginBottom: '24px' }}>
        <div className="dash-card">
          <span className="kpi-header">Interviews Conducted</span>
          <span className="kpi-value">{data.metrics.interviewsCount}</span>
          <span className="kpi-trend" style={{ color: '#059669' }}>
            {data.metrics.positiveIntentPct}% positive intent
          </span>
        </div>
        <div className="dash-card">
          <span className="kpi-header">Surveys Completed</span>
          <span className="kpi-value">{data.metrics.surveysCompleted}</span>
          <span className="kpi-trend" style={{ color: '#2563eb' }}>
            High willingness to pay
          </span>
        </div>
        <div className="dash-card">
          <span className="kpi-header">Assumptions Validated</span>
          <span className="kpi-value">{data.metrics.assumptionsRatio}</span>
          <span className="kpi-trend" style={{ color: '#7c3aed' }}>
            Core problem verified
          </span>
        </div>
        <div className="dash-card">
          <span className="kpi-header">Willingness Score</span>
          <span className="kpi-value">{data.metrics.willingnessScore}</span>
          <span className="kpi-trend" style={{ color: '#d97706' }}>
            Green premium acceptance
          </span>
        </div>
      </div>

      {/* Hypotheses & Assumptions Tracker */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
        <div>
          <h4 style={{ margin: 0, fontSize: '14px' }}>Core Hypotheses & Assumptions ({data.hypotheses?.length || 0})</h4>
          <small style={{ color: 'var(--muted)' }}>Testable beliefs about user demand, pricing, and distribution</small>
        </div>
        <Button
          variant="secondary"
          icon={Plus}
          onClick={() => setHypothesisModal({ open: true, item: { statement: '', risk: 'Medium', status: 'In Progress', evidence: '' } })}
        >
          Add Hypothesis
        </Button>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '24px' }}>
        {data.hypotheses?.map((h) => {
          const statusTone =
            h.status === 'Validated' ? 'green' : h.status === 'Invalidated' ? 'red' : 'gold';
          return (
            <div
              key={h.id}
              className="dash-card"
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                justifyContent: 'space-between',
                padding: '12px 16px',
                background: 'var(--soft)',
                gap: '12px',
              }}
            >
              <div style={{ flex: 1 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                  <Tag tone={statusTone}>{h.status}</Tag>
                  <Tag tone={h.risk === 'High' ? 'red' : 'purple'}>Risk: {h.risk}</Tag>
                </div>
                <p style={{ margin: '4px 0', fontSize: '13px', fontWeight: 600 }}>{h.statement}</p>
                {h.evidence && (
                  <p style={{ margin: '2px 0 0', fontSize: '12px', color: 'var(--muted)' }}>
                    <strong>Evidence:</strong> {h.evidence}
                  </p>
                )}
              </div>
              <div style={{ display: 'flex', gap: '4px', flexShrink: 0 }}>
                <button
                  type="button"
                  className="icon-subtle-btn"
                  onClick={() => setHypothesisModal({ open: true, item: h })}
                >
                  <Edit2 size={13} />
                </button>
                <button
                  type="button"
                  className="icon-subtle-btn danger"
                  onClick={() => deleteHypothesis(h.id)}
                >
                  <Trash2 size={13} />
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Customer Interviews Feedback Log */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
        <div>
          <h4 style={{ margin: 0, fontSize: '14px' }}>Customer Interview Log & Direct Quotes</h4>
          <small style={{ color: 'var(--muted)' }}>Qualitative quotes directly from prospect conversations</small>
        </div>
        <Button
          variant="secondary"
          icon={Plus}
          onClick={() => setInterviewModal({ open: true, item: { name: '', roleSegment: '', quote: '', outcome: 'Strong Validation', date: '' } })}
        >
          Add Interview
        </Button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '14px', marginBottom: '24px' }}>
        {data.interviews?.map((inItem) => (
          <div key={inItem.id} className="dash-card" style={{ border: '1px solid var(--line)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
              <div>
                <b style={{ fontSize: '14px' }}>{inItem.name}</b>
                <span style={{ display: 'block', fontSize: '11px', color: 'var(--muted)' }}>{inItem.roleSegment} · {inItem.date}</span>
              </div>
              <div style={{ display: 'flex', gap: '4px' }}>
                <button
                  type="button"
                  className="icon-subtle-btn"
                  onClick={() => setInterviewModal({ open: true, item: inItem })}
                >
                  <Edit2 size={12} />
                </button>
                <button
                  type="button"
                  className="icon-subtle-btn danger"
                  onClick={() => deleteInterview(inItem.id)}
                >
                  <Trash2 size={12} />
                </button>
              </div>
            </div>
            <blockquote style={{ margin: '6px 0', fontSize: '12px', fontStyle: 'italic', color: 'var(--ink)' }}>
              "{inItem.quote}"
            </blockquote>
            <div style={{ marginTop: '8px' }}>
              <Tag tone={inItem.outcome.includes('Strong') ? 'green' : 'blue'}>{inItem.outcome}</Tag>
            </div>
          </div>
        ))}
      </div>

      {/* User Personas */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
        <div>
          <h4 style={{ margin: 0, fontSize: '14px' }}>User Personas ({data.personas?.length || 0})</h4>
          <small style={{ color: 'var(--muted)' }}>Key user archetypes guiding product decisions</small>
        </div>
        <Button
          variant="secondary"
          icon={Plus}
          onClick={() => setPersonaModal({ open: true, item: { name: '', archetype: '', goals: '', frustrations: '' } })}
        >
          Add Persona
        </Button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '14px' }}>
        {data.personas?.map((persona) => (
          <div key={persona.id} className="dash-card" style={{ background: 'var(--soft)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '6px' }}>
              <div>
                <b style={{ fontSize: '14px' }}>{persona.name}</b>
                <span style={{ display: 'block', fontSize: '11px', color: 'var(--muted)' }}>{persona.archetype}</span>
              </div>
              <div style={{ display: 'flex', gap: '4px' }}>
                <button
                  type="button"
                  className="icon-subtle-btn"
                  onClick={() => setPersonaModal({ open: true, item: persona })}
                >
                  <Edit2 size={12} />
                </button>
                <button
                  type="button"
                  className="icon-subtle-btn danger"
                  onClick={() => deletePersona(persona.id)}
                >
                  <Trash2 size={12} />
                </button>
              </div>
            </div>
            <div style={{ fontSize: '12px', display: 'flex', flexDirection: 'column', gap: '4px', marginTop: '6px' }}>
              <div>
                <strong style={{ color: '#059669' }}>Goals: </strong>
                <span>{persona.goals}</span>
              </div>
              <div>
                <strong style={{ color: '#ef4444' }}>Frustrations: </strong>
                <span>{persona.frustrations}</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* MODALS */}
      {/* Edit Validation Metrics Modal */}
      {editingMetrics && (
        <Dialog title="Edit Validation Metrics" onClose={() => setEditingMetrics(false)}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Interviews Conducted">
                <input
                  type="number"
                  value={data.metrics.interviewsCount}
                  onChange={(e) => setData({ ...data, metrics: { ...data.metrics, interviewsCount: Number(e.target.value) } })}
                />
              </Field>
              <Field label="Positive Intent %">
                <input
                  type="number"
                  value={data.metrics.positiveIntentPct}
                  onChange={(e) => setData({ ...data, metrics: { ...data.metrics, positiveIntentPct: Number(e.target.value) } })}
                />
              </Field>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Surveys Completed">
                <input
                  type="number"
                  value={data.metrics.surveysCompleted}
                  onChange={(e) => setData({ ...data, metrics: { ...data.metrics, surveysCompleted: Number(e.target.value) } })}
                />
              </Field>
              <Field label="Assumptions Validated (e.g. 4 / 5)">
                <input
                  value={data.metrics.assumptionsRatio}
                  onChange={(e) => setData({ ...data, metrics: { ...data.metrics, assumptionsRatio: e.target.value } })}
                />
              </Field>
            </div>
            <Field label="Willingness to Pay Score">
              <input
                value={data.metrics.willingnessScore}
                onChange={(e) => setData({ ...data, metrics: { ...data.metrics, willingnessScore: e.target.value } })}
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setEditingMetrics(false)}>Cancel</Button>
              <Button
                variant="primary"
                onClick={() => {
                  updateAndSave(data);
                  setEditingMetrics(false);
                  app?.toast?.('Validation metrics updated!');
                }}
              >
                Save Metrics
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Add/Edit Hypothesis Modal */}
      {hypothesisModal.open && (
        <Dialog
          title={hypothesisModal.item?.id ? 'Edit Hypothesis' : 'Add Testable Hypothesis'}
          onClose={() => setHypothesisModal({ open: false, item: null })}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Hypothesis Statement">
              <textarea
                rows={3}
                value={hypothesisModal.item?.statement || ''}
                onChange={(e) => setHypothesisModal({ ...hypothesisModal, item: { ...hypothesisModal.item, statement: e.target.value } })}
                placeholder="We believe that [target persona] will [expected action] because..."
                required
              />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Status">
                <select
                  value={hypothesisModal.item?.status || 'In Progress'}
                  onChange={(e) => setHypothesisModal({ ...hypothesisModal, item: { ...hypothesisModal.item, status: e.target.value } })}
                >
                  <option value="In Progress">In Progress</option>
                  <option value="Validated">Validated</option>
                  <option value="Invalidated">Invalidated</option>
                </select>
              </Field>
              <Field label="Risk Level">
                <select
                  value={hypothesisModal.item?.risk || 'Medium'}
                  onChange={(e) => setHypothesisModal({ ...hypothesisModal, item: { ...hypothesisModal.item, risk: e.target.value } })}
                >
                  <option value="Low">Low</option>
                  <option value="Medium">Medium</option>
                  <option value="High">High</option>
                </select>
              </Field>
            </div>
            <Field label="Evidence & Proof Notes">
              <textarea
                rows={2}
                value={hypothesisModal.item?.evidence || ''}
                onChange={(e) => setHypothesisModal({ ...hypothesisModal, item: { ...hypothesisModal.item, evidence: e.target.value } })}
                placeholder="Key data points, quotes, or metrics proving/disproving this..."
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setHypothesisModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveHypothesis(hypothesisModal.item)}>Save Hypothesis</Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Add/Edit Interview Modal */}
      {interviewModal.open && (
        <Dialog
          title={interviewModal.item?.id ? 'Edit Interview Entry' : 'Log Customer Interview'}
          onClose={() => setInterviewModal({ open: false, item: null })}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Interviewee Name">
                <input
                  value={interviewModal.item?.name || ''}
                  onChange={(e) => setInterviewModal({ ...interviewModal, item: { ...interviewModal.item, name: e.target.value } })}
                  placeholder="e.g. Anita Roy"
                  required
                />
              </Field>
              <Field label="Role / Segment">
                <input
                  value={interviewModal.item?.roleSegment || ''}
                  onChange={(e) => setInterviewModal({ ...interviewModal, item: { ...interviewModal.item, roleSegment: e.target.value } })}
                  placeholder="e.g. Urban Eco Consumer"
                />
              </Field>
            </div>
            <Field label="Key Feedback / Direct Quote">
              <textarea
                rows={3}
                value={interviewModal.item?.quote || ''}
                onChange={(e) => setInterviewModal({ ...interviewModal, item: { ...interviewModal.item, quote: e.target.value } })}
                placeholder="What did they explicitly say?"
                required
              />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Validation Verdict">
                <select
                  value={interviewModal.item?.outcome || 'Strong Validation'}
                  onChange={(e) => setInterviewModal({ ...interviewModal, item: { ...interviewModal.item, outcome: e.target.value } })}
                >
                  <option value="Strong Validation">Strong Validation</option>
                  <option value="Moderate Validation">Moderate Validation</option>
                  <option value="Neutral">Neutral</option>
                  <option value="Critical / Needs Pivot">Critical / Needs Pivot</option>
                </select>
              </Field>
              <Field label="Date Conducted">
                <input
                  value={interviewModal.item?.date || ''}
                  onChange={(e) => setInterviewModal({ ...interviewModal, item: { ...interviewModal.item, date: e.target.value } })}
                  placeholder="e.g. Sep 28, 2026"
                />
              </Field>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setInterviewModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveInterview(interviewModal.item)}>Save Interview</Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Add/Edit Persona Modal */}
      {personaModal.open && (
        <Dialog
          title={personaModal.item?.id ? 'Edit User Persona' : 'Create User Persona'}
          onClose={() => setPersonaModal({ open: false, item: null })}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Persona Name">
              <input
                value={personaModal.item?.name || ''}
                onChange={(e) => setPersonaModal({ ...personaModal, item: { ...personaModal.item, name: e.target.value } })}
                placeholder="e.g. Conscious Clara"
                required
              />
            </Field>
            <Field label="Demographic Archetype">
              <input
                value={personaModal.item?.archetype || ''}
                onChange={(e) => setPersonaModal({ ...personaModal, item: { ...personaModal.item, archetype: e.target.value } })}
                placeholder="e.g. Eco-conscious working professional, 28"
              />
            </Field>
            <Field label="Core Goals & Motivations">
              <textarea
                rows={2}
                value={personaModal.item?.goals || ''}
                onChange={(e) => setPersonaModal({ ...personaModal, item: { ...personaModal.item, goals: e.target.value } })}
              />
            </Field>
            <Field label="Main Frustrations">
              <textarea
                rows={2}
                value={personaModal.item?.frustrations || ''}
                onChange={(e) => setPersonaModal({ ...personaModal, item: { ...personaModal.item, frustrations: e.target.value } })}
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setPersonaModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => savePersona(personaModal.item)}>Save Persona</Button>
            </div>
          </div>
        </Dialog>
      )}
    </section>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. MVP PLANNING PANEL (FULL CRUD)
// ─────────────────────────────────────────────────────────────────────────────
export function MVPPlanningPanelEnhanced({ mvp, onSave, app }) {
  const [data, setData] = useState(mvp);
  const [editingTarget, setEditingTarget] = useState(false);
  const [featureModal, setFeatureModal] = useState({ open: false, item: null });
  const [linkModal, setLinkModal] = useState({ open: false, item: null });

  useEffect(() => {
    setData(mvp);
  }, [mvp]);

  const updateAndSave = (next) => {
    setData(next);
    onSave(next);
  };

  const saveFeature = (feat) => {
    const nextList = feat.id
      ? data.features.map((f) => (f.id === feat.id ? feat : f))
      : [...data.features, { ...feat, id: 'f_' + Date.now() }];
    updateAndSave({ ...data, features: nextList });
    setFeatureModal({ open: false, item: null });
    app?.toast?.('MVP feature saved.');
  };

  const deleteFeature = (id) => {
    updateAndSave({ ...data, features: data.features.filter((f) => f.id !== id) });
    app?.toast?.('Feature removed.');
  };

  const toggleFeatureStatus = (id) => {
    const order = ['Planned', 'Building', 'Testing', 'Done'];
    const nextList = data.features.map((f) => {
      if (f.id === id) {
        const nextIdx = (order.indexOf(f.status) + 1) % order.length;
        return { ...f, status: order[nextIdx] };
      }
      return f;
    });
    updateAndSave({ ...data, features: nextList });
  };

  const saveLink = (link) => {
    const nextList = link.id
      ? data.links.map((l) => (l.id === link.id ? link : l))
      : [...data.links, { ...link, id: 'l_' + Date.now() }];
    updateAndSave({ ...data, links: nextList });
    setLinkModal({ open: false, item: null });
    app?.toast?.('Prototype link saved.');
  };

  const deleteLink = (id) => {
    updateAndSave({ ...data, links: data.links.filter((l) => l.id !== id) });
    app?.toast?.('Link deleted.');
  };

  return (
    <section className="dash-card">
      <SectionTitle title="MVP Planning & Feature Scope Backlog" />
      <p style={{ margin: '4px 0 16px', fontSize: '13px', color: 'var(--muted)' }}>
        Define your minimum viable product scope using MoSCoW prioritization, track engineering feature progress, and attach wireframe/prototype links.
      </p>

      {/* Target Release & Scope Progress Bar */}
      <div
        style={{
          padding: '16px 20px',
          borderRadius: '10px',
          background: 'var(--soft)',
          border: '1px solid var(--line)',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '16px',
        }}
      >
        <div>
          <span style={{ fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--muted)', fontWeight: 600 }}>
            TARGET MVP LAUNCH
          </span>
          <h3 style={{ margin: '2px 0 4px', fontSize: '18px' }}>
            {data.targetDate} · <span style={{ color: '#059669' }}>{data.scopeProgress}% Scope Ready</span>
          </h3>
          <p style={{ margin: 0, fontSize: '13px', color: 'var(--ink)' }}>{data.statement}</p>
        </div>
        <button
          type="button"
          className="edit-pill-btn"
          onClick={() => setEditingTarget(true)}
        >
          <Edit2 size={13} />
          <span>Edit Target & Scope</span>
        </button>
      </div>

      {/* Feature Backlog Header & Add Button */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
        <div>
          <h4 style={{ margin: 0, fontSize: '14px' }}>Feature Backlog ({data.features?.length || 0})</h4>
          <small style={{ color: 'var(--muted)' }}>Prioritized via Must Have / Should Have / Could Have tiers</small>
        </div>
        <Button
          variant="secondary"
          icon={Plus}
          onClick={() => setFeatureModal({ open: true, item: { title: '', description: '', priority: 'Must Have', status: 'Planned', effort: '1 week' } })}
        >
          Add Feature
        </Button>
      </div>

      {/* Feature List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '24px' }}>
        {data.features?.map((feat) => {
          const priorityTone =
            feat.priority === 'Must Have'
              ? 'red'
              : feat.priority === 'Should Have'
              ? 'blue'
              : 'purple';
          const isDone = feat.status === 'Done';

          return (
            <div
              key={feat.id}
              className="dash-card"
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '12px 16px',
                border: '1px solid var(--line)',
                gap: '12px',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: 1, minWidth: 0 }}>
                <button
                  type="button"
                  onClick={() => toggleFeatureStatus(feat.id)}
                  title={`Status: ${feat.status}. Click to cycle.`}
                  style={{
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    padding: 0,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <CheckCircle2 size={20} color={isDone ? '#059669' : '#9ca3af'} />
                </button>
                <div style={{ minWidth: 0 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap', marginBottom: '2px' }}>
                    <b style={{ fontSize: '14px', textDecoration: isDone ? 'line-through' : 'none' }}>
                      {feat.title}
                    </b>
                    <Tag tone={priorityTone}>{feat.priority}</Tag>
                    <Tag tone={isDone ? 'green' : 'gold'}>{feat.status}</Tag>
                    {feat.effort && <small style={{ color: 'var(--muted)' }}>Effort: {feat.effort}</small>}
                  </div>
                  <p style={{ margin: 0, fontSize: '12px', color: 'var(--muted)', wordBreak: 'break-word' }}>
                    {feat.description}
                  </p>
                </div>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexShrink: 0 }}>
                <button
                  type="button"
                  className="icon-subtle-btn"
                  onClick={() => setFeatureModal({ open: true, item: feat })}
                >
                  <Edit2 size={13} />
                </button>
                <button
                  type="button"
                  className="icon-subtle-btn danger"
                  onClick={() => deleteFeature(feat.id)}
                >
                  <Trash2 size={13} />
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Prototype & Wireframe Links */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
        <div>
          <h4 style={{ margin: 0, fontSize: '14px' }}>Prototypes & Wireframe Links</h4>
          <small style={{ color: 'var(--muted)' }}>Figma, Loom walkthroughs, and interactive prototype demos</small>
        </div>
        <Button
          variant="secondary"
          icon={Plus}
          onClick={() => setLinkModal({ open: true, item: { title: '', url: '', type: 'Figma', notes: '' } })}
        >
          Add Link
        </Button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '14px' }}>
        {data.links?.map((link) => (
          <div key={link.id} className="dash-card" style={{ background: 'var(--soft)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '6px' }}>
              <div style={{ minWidth: 0 }}>
                <b style={{ fontSize: '14px', display: 'block', wordBreak: 'break-word' }}>{link.title}</b>
                <Tag tone="purple">{link.type}</Tag>
              </div>
              <div style={{ display: 'flex', gap: '4px', flexShrink: 0 }}>
                <button
                  type="button"
                  className="icon-subtle-btn"
                  onClick={() => setLinkModal({ open: true, item: link })}
                >
                  <Edit2 size={12} />
                </button>
                <button
                  type="button"
                  className="icon-subtle-btn danger"
                  onClick={() => deleteLink(link.id)}
                >
                  <Trash2 size={12} />
                </button>
              </div>
            </div>
            {link.notes && <p style={{ margin: '6px 0', fontSize: '12px', color: 'var(--muted)' }}>{link.notes}</p>}
            <a
              href={link.url}
              target="_blank"
              rel="noreferrer"
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '4px',
                fontSize: '12px',
                color: 'var(--purple, #7c3aed)',
                fontWeight: 600,
                marginTop: '6px',
                textDecoration: 'none',
              }}
            >
              Open Resource <ExternalLink size={12} />
            </a>
          </div>
        ))}
      </div>

      {/* MODALS */}
      {/* Edit Target & Scope Modal */}
      {editingTarget && (
        <Dialog title="Edit MVP Launch Target & Scope" onClose={() => setEditingTarget(false)}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Target Launch Date">
                <input
                  value={data.targetDate}
                  onChange={(e) => setData({ ...data, targetDate: e.target.value })}
                  placeholder="e.g. 2026-11-15"
                />
              </Field>
              <Field label="Scope Ready %">
                <input
                  type="number"
                  min={0}
                  max={100}
                  value={data.scopeProgress}
                  onChange={(e) => setData({ ...data, scopeProgress: Number(e.target.value) })}
                />
              </Field>
            </div>
            <Field label="MVP Release Statement">
              <textarea
                rows={3}
                value={data.statement}
                onChange={(e) => setData({ ...data, statement: e.target.value })}
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setEditingTarget(false)}>Cancel</Button>
              <Button
                variant="primary"
                onClick={() => {
                  updateAndSave(data);
                  setEditingTarget(false);
                  app?.toast?.('MVP launch target updated!');
                }}
              >
                Save Target
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Add/Edit Feature Modal */}
      {featureModal.open && (
        <Dialog
          title={featureModal.item?.id ? 'Edit MVP Feature' : 'Add MVP Feature'}
          onClose={() => setFeatureModal({ open: false, item: null })}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Feature Title">
              <input
                value={featureModal.item?.title || ''}
                onChange={(e) => setFeatureModal({ ...featureModal, item: { ...featureModal.item, title: e.target.value } })}
                placeholder="e.g. Zero-emission courier checkout"
                required
              />
            </Field>
            <Field label="Feature Description">
              <textarea
                rows={2}
                value={featureModal.item?.description || ''}
                onChange={(e) => setFeatureModal({ ...featureModal, item: { ...featureModal.item, description: e.target.value } })}
                placeholder="What does this feature accomplish?"
              />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '10px' }}>
              <Field label="MoSCoW Priority">
                <select
                  value={featureModal.item?.priority || 'Must Have'}
                  onChange={(e) => setFeatureModal({ ...featureModal, item: { ...featureModal.item, priority: e.target.value } })}
                >
                  <option value="Must Have">Must Have</option>
                  <option value="Should Have">Should Have</option>
                  <option value="Could Have">Could Have</option>
                  <option value="Won't Have">Won't Have</option>
                </select>
              </Field>
              <Field label="Status">
                <select
                  value={featureModal.item?.status || 'Planned'}
                  onChange={(e) => setFeatureModal({ ...featureModal, item: { ...featureModal.item, status: e.target.value } })}
                >
                  <option value="Planned">Planned</option>
                  <option value="Building">Building</option>
                  <option value="Testing">Testing</option>
                  <option value="Done">Done</option>
                </select>
              </Field>
              <Field label="Effort / Weeks">
                <input
                  value={featureModal.item?.effort || ''}
                  onChange={(e) => setFeatureModal({ ...featureModal, item: { ...featureModal.item, effort: e.target.value } })}
                  placeholder="e.g. 2 weeks"
                />
              </Field>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setFeatureModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveFeature(featureModal.item)}>Save Feature</Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Add/Edit Link Modal */}
      {linkModal.open && (
        <Dialog
          title={linkModal.item?.id ? 'Edit Link' : 'Add Prototype Link'}
          onClose={() => setLinkModal({ open: false, item: null })}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Resource Title">
              <input
                value={linkModal.item?.title || ''}
                onChange={(e) => setLinkModal({ ...linkModal, item: { ...linkModal.item, title: e.target.value } })}
                placeholder="e.g. Figma Mobile Design System"
                required
              />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '10px' }}>
              <Field label="URL">
                <input
                  value={linkModal.item?.url || ''}
                  onChange={(e) => setLinkModal({ ...linkModal, item: { ...linkModal.item, url: e.target.value } })}
                  placeholder="https://..."
                  required
                />
              </Field>
              <Field label="Type">
                <select
                  value={linkModal.item?.type || 'Figma'}
                  onChange={(e) => setLinkModal({ ...linkModal, item: { ...linkModal.item, type: e.target.value } })}
                >
                  <option value="Figma">Figma</option>
                  <option value="Prototype">Prototype</option>
                  <option value="Task Tracker">Task Tracker</option>
                  <option value="GitHub">GitHub</option>
                  <option value="Loom Video">Loom Video</option>
                  <option value="Other">Other</option>
                </select>
              </Field>
            </div>
            <Field label="Notes / Instructions">
              <input
                value={linkModal.item?.notes || ''}
                onChange={(e) => setLinkModal({ ...linkModal, item: { ...linkModal.item, notes: e.target.value } })}
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setLinkModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveLink(linkModal.item)}>Save Link</Button>
            </div>
          </div>
        </Dialog>
      )}
    </section>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. BUSINESS MODEL PANEL (FULL CRUD)
// ─────────────────────────────────────────────────────────────────────────────
export function BusinessModelPanelEnhanced({ businessModel, onSave, app }) {
  const [data, setData] = useState(businessModel);
  const [editingRevenue, setEditingRevenue] = useState(false);
  const [editingUnitEcon, setEditingUnitEcon] = useState(false);
  const [tierModal, setTierModal] = useState({ open: false, item: null });
  const [costModal, setCostModal] = useState({ open: false, item: null });
  const [gtmModal, setGtmModal] = useState({ open: false, item: null });

  useEffect(() => {
    setData(businessModel);
  }, [businessModel]);

  const updateAndSave = (next) => {
    setData(next);
    onSave(next);
  };

  const saveTier = (tier) => {
    const nextList = tier.id
      ? data.pricingTiers.map((t) => (t.id === tier.id ? tier : t))
      : [...data.pricingTiers, { ...tier, id: 'pt_' + Date.now() }];
    updateAndSave({ ...data, pricingTiers: nextList });
    setTierModal({ open: false, item: null });
    app?.toast?.('Pricing tier saved.');
  };

  const deleteTier = (id) => {
    updateAndSave({ ...data, pricingTiers: data.pricingTiers.filter((t) => t.id !== id) });
    app?.toast?.('Pricing tier removed.');
  };

  const saveCost = (cost) => {
    const nextList = cost.id
      ? data.costs.map((c) => (c.id === cost.id ? cost : c))
      : [...data.costs, { ...cost, id: 'cs_' + Date.now() }];
    updateAndSave({ ...data, costs: nextList });
    setCostModal({ open: false, item: null });
    app?.toast?.('Cost item saved.');
  };

  const deleteCost = (id) => {
    updateAndSave({ ...data, costs: data.costs.filter((c) => c.id !== id) });
    app?.toast?.('Cost item deleted.');
  };

  const saveGtm = (gtm) => {
    const nextList = gtm.id
      ? data.gtmChannels.map((g) => (g.id === gtm.id ? g : g))
      : [...data.gtmChannels, { ...gtm, id: 'gt_' + Date.now() }];
    updateAndSave({ ...data, gtmChannels: nextList });
    setGtmModal({ open: false, item: null });
    app?.toast?.('GTM channel saved.');
  };

  const deleteGtm = (id) => {
    updateAndSave({ ...data, gtmChannels: data.gtmChannels.filter((g) => g.id !== id) });
    app?.toast?.('GTM channel removed.');
  };

  const ltvCacRatio = data.unitEconomics?.cac
    ? (data.unitEconomics.ltv / data.unitEconomics.cac).toFixed(1)
    : '0';

  return (
    <section className="dash-card">
      <SectionTitle title="Business Model Canvas & Unit Economics" />
      <p style={{ margin: '4px 0 16px', fontSize: '13px', color: 'var(--muted)' }}>
        Define revenue streams, pricing tiers, operational cost structure, unit economics (LTV/CAC), and go-to-market channels.
      </p>

      {/* Revenue Model Overview */}
      <div
        style={{
          padding: '16px 20px',
          borderRadius: '10px',
          background: 'var(--soft)',
          border: '1px solid var(--line)',
          marginBottom: '24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '12px',
        }}
      >
        <div>
          <span style={{ fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--muted)', fontWeight: 600 }}>
            PRIMARY REVENUE ENGINE
          </span>
          <h3 style={{ margin: '2px 0', fontSize: '17px' }}>{data.revenueModel}</h3>
          <p style={{ margin: 0, fontSize: '13px', color: 'var(--muted)' }}>{data.takeRate}</p>
        </div>
        <button
          type="button"
          className="edit-pill-btn"
          onClick={() => setEditingRevenue(true)}
        >
          <Edit2 size={13} />
          <span>Edit Engine</span>
        </button>
      </div>

      {/* Unit Economics 4 KPIs */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
        <h4 style={{ margin: 0, fontSize: '14px' }}>Unit Economics & Financial Metrics</h4>
        <button
          type="button"
          className="edit-pill-btn"
          onClick={() => setEditingUnitEcon(true)}
        >
          <Edit2 size={13} />
          <span>Edit Economics</span>
        </button>
      </div>

      <div className="dashboard-grid-4" style={{ marginBottom: '24px' }}>
        <div className="dash-card">
          <span className="kpi-header">Customer Acquisition (CAC)</span>
          <span className="kpi-value">${data.unitEconomics?.cac || 0}</span>
          <span className="kpi-trend">Blended paid & organic</span>
        </div>
        <div className="dash-card">
          <span className="kpi-header">Customer Lifetime Value (LTV)</span>
          <span className="kpi-value">${data.unitEconomics?.ltv || 0}</span>
          <span className="kpi-trend">Estimated 18-month spend</span>
        </div>
        <div className="dash-card">
          <span className="kpi-header">LTV : CAC Ratio</span>
          <span className="kpi-value" style={{ color: '#059669' }}>{ltvCacRatio}x</span>
          <span className="kpi-trend" style={{ color: Number(ltvCacRatio) >= 3 ? '#059669' : '#d97706' }}>
            {Number(ltvCacRatio) >= 3 ? 'Healthy startup benchmark' : 'Needs margin tuning'}
          </span>
        </div>
        <div className="dash-card">
          <span className="kpi-header">Gross Margin %</span>
          <span className="kpi-value">{data.unitEconomics?.grossMarginPct || 0}%</span>
          <span className="kpi-trend">Payback: {data.unitEconomics?.paybackMonths || 0} months</span>
        </div>
      </div>

      {/* Pricing Tiers */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
        <div>
          <h4 style={{ margin: 0, fontSize: '14px' }}>Pricing Strategy & Seller Tiers ({data.pricingTiers?.length || 0})</h4>
          <small style={{ color: 'var(--muted)' }}>Tiers designed to maximize adoption while scaling take-rate</small>
        </div>
        <Button
          variant="secondary"
          icon={Plus}
          onClick={() => setTierModal({ open: true, item: { name: '', price: '', interval: '/ month', features: [''], popular: false } })}
        >
          Add Tier
        </Button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '14px', marginBottom: '24px' }}>
        {data.pricingTiers?.map((tier) => (
          <div
            key={tier.id}
            className="dash-card"
            style={{
              border: tier.popular ? '2px solid #7c3aed' : '1px solid var(--line)',
              position: 'relative',
            }}
          >
            {tier.popular && (
              <span
                style={{
                  position: 'absolute',
                  top: '-10px',
                  right: '16px',
                  background: '#7c3aed',
                  color: '#fff',
                  padding: '2px 8px',
                  borderRadius: '10px',
                  fontSize: '11px',
                  fontWeight: 600,
                }}
              >
                MOST POPULAR
              </span>
            )}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '6px' }}>
              <div>
                <b style={{ fontSize: '15px' }}>{tier.name}</b>
                <h3 style={{ margin: '4px 0', fontSize: '20px', color: 'var(--ink)' }}>
                  {tier.price} <small style={{ fontSize: '12px', color: 'var(--muted)' }}>{tier.interval}</small>
                </h3>
              </div>
              <div style={{ display: 'flex', gap: '4px' }}>
                <button
                  type="button"
                  className="icon-subtle-btn"
                  onClick={() => setTierModal({ open: true, item: tier })}
                >
                  <Edit2 size={12} />
                </button>
                <button
                  type="button"
                  className="icon-subtle-btn danger"
                  onClick={() => deleteTier(tier.id)}
                >
                  <Trash2 size={12} />
                </button>
              </div>
            </div>

            <ul style={{ margin: '8px 0 0', paddingLeft: '16px', fontSize: '12px', lineHeight: 1.6 }}>
              {tier.features?.map((f, idx) => (
                <li key={idx}>{f}</li>
              ))}
            </ul>
          </div>
        ))}
      </div>

      {/* Cost Structure Breakdown */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
        <div>
          <h4 style={{ margin: 0, fontSize: '14px' }}>Cost Structure & Operating Expenses</h4>
          <small style={{ color: 'var(--muted)' }}>Fixed vs variable monthly operational expenditure</small>
        </div>
        <Button
          variant="secondary"
          icon={Plus}
          onClick={() => setCostModal({ open: true, item: { title: '', monthlyCost: '', type: 'Fixed', category: 'Tech' } })}
        >
          Add Cost Item
        </Button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '12px', marginBottom: '24px' }}>
        {data.costs?.map((cost) => (
          <div
            key={cost.id}
            className="dash-card"
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '10px 14px',
              background: 'var(--soft)',
            }}
          >
            <div>
              <b style={{ fontSize: '13px' }}>{cost.title}</b>
              <div style={{ display: 'flex', gap: '6px', marginTop: '2px' }}>
                <Tag tone={cost.type === 'Fixed' ? 'blue' : 'purple'}>{cost.type}</Tag>
                <span style={{ fontSize: '11px', color: 'var(--muted)' }}>{cost.category}</span>
              </div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '14px', fontWeight: 700 }}>{cost.monthlyCost}</span>
              <button
                type="button"
                className="icon-subtle-btn danger"
                onClick={() => deleteCost(cost.id)}
              >
                <Trash2 size={12} />
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Go-to-Market Channels */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
        <div>
          <h4 style={{ margin: 0, fontSize: '14px' }}>Go-To-Market (GTM) Strategy & Distribution</h4>
          <small style={{ color: 'var(--muted)' }}>Channels for customer and seller acquisition</small>
        </div>
        <Button
          variant="secondary"
          icon={Plus}
          onClick={() => setGtmModal({ open: true, item: { channel: '', strategy: '', status: 'Active', efficiency: 'High' } })}
        >
          Add Channel
        </Button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '12px' }}>
        {data.gtmChannels?.map((gtm) => (
          <div key={gtm.id} className="dash-card" style={{ border: '1px solid var(--line)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '4px' }}>
              <div>
                <b style={{ fontSize: '13px' }}>{gtm.channel}</b>
                <div style={{ display: 'flex', gap: '6px', marginTop: '2px' }}>
                  <Tag tone="green">{gtm.status}</Tag>
                  <Tag tone="purple">{gtm.efficiency}</Tag>
                </div>
              </div>
              <div style={{ display: 'flex', gap: '4px' }}>
                <button
                  type="button"
                  className="icon-subtle-btn"
                  onClick={() => setGtmModal({ open: true, item: gtm })}
                >
                  <Edit2 size={12} />
                </button>
                <button
                  type="button"
                  className="icon-subtle-btn danger"
                  onClick={() => deleteGtm(gtm.id)}
                >
                  <Trash2 size={12} />
                </button>
              </div>
            </div>
            <p style={{ margin: '6px 0 0', fontSize: '12px', color: 'var(--muted)' }}>{gtm.strategy}</p>
          </div>
        ))}
      </div>

      {/* MODALS */}
      {/* Edit Revenue Engine Modal */}
      {editingRevenue && (
        <Dialog title="Edit Primary Revenue Engine" onClose={() => setEditingRevenue(false)}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Revenue Model Description">
              <input
                value={data.revenueModel}
                onChange={(e) => setData({ ...data, revenueModel: e.target.value })}
                required
              />
            </Field>
            <Field label="Commission / Take-rate Details">
              <input
                value={data.takeRate}
                onChange={(e) => setData({ ...data, takeRate: e.target.value })}
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setEditingRevenue(false)}>Cancel</Button>
              <Button
                variant="primary"
                onClick={() => {
                  updateAndSave(data);
                  setEditingRevenue(false);
                  app?.toast?.('Revenue engine updated!');
                }}
              >
                Save Engine
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Edit Unit Economics Modal */}
      {editingUnitEcon && (
        <Dialog title="Edit Unit Economics" onClose={() => setEditingUnitEcon(false)}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Target CAC ($)">
                <input
                  type="number"
                  step="0.1"
                  value={data.unitEconomics?.cac || 0}
                  onChange={(e) => setData({ ...data, unitEconomics: { ...data.unitEconomics, cac: Number(e.target.value) } })}
                />
              </Field>
              <Field label="Target LTV ($)">
                <input
                  type="number"
                  step="0.1"
                  value={data.unitEconomics?.ltv || 0}
                  onChange={(e) => setData({ ...data, unitEconomics: { ...data.unitEconomics, ltv: Number(e.target.value) } })}
                />
              </Field>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Gross Margin %">
                <input
                  type="number"
                  value={data.unitEconomics?.grossMarginPct || 0}
                  onChange={(e) => setData({ ...data, unitEconomics: { ...data.unitEconomics, grossMarginPct: Number(e.target.value) } })}
                />
              </Field>
              <Field label="Payback Period (Months)">
                <input
                  type="number"
                  step="0.1"
                  value={data.unitEconomics?.paybackMonths || 0}
                  onChange={(e) => setData({ ...data, unitEconomics: { ...data.unitEconomics, paybackMonths: Number(e.target.value) } })}
                />
              </Field>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setEditingUnitEcon(false)}>Cancel</Button>
              <Button
                variant="primary"
                onClick={() => {
                  updateAndSave(data);
                  setEditingUnitEcon(false);
                  app?.toast?.('Unit economics updated!');
                }}
              >
                Save Metrics
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Add/Edit Tier Modal */}
      {tierModal.open && (
        <Dialog
          title={tierModal.item?.id ? 'Edit Pricing Tier' : 'Add Pricing Tier'}
          onClose={() => setTierModal({ open: false, item: null })}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Tier Name">
              <input
                value={tierModal.item?.name || ''}
                onChange={(e) => setTierModal({ ...tierModal, item: { ...tierModal.item, name: e.target.value } })}
                placeholder="e.g. Verified Artisan Pro"
                required
              />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Price">
                <input
                  value={tierModal.item?.price || ''}
                  onChange={(e) => setTierModal({ ...tierModal, item: { ...tierModal.item, price: e.target.value } })}
                  placeholder="e.g. $29 or Free"
                />
              </Field>
              <Field label="Billing Interval">
                <input
                  value={tierModal.item?.interval || ''}
                  onChange={(e) => setTierModal({ ...tierModal, item: { ...tierModal.item, interval: e.target.value } })}
                  placeholder="e.g. / month"
                />
              </Field>
            </div>
            <Field label="Features (Comma separated)">
              <textarea
                rows={3}
                value={tierModal.item?.features?.join(', ') || ''}
                onChange={(e) => setTierModal({ ...tierModal, item: { ...tierModal.item, features: e.target.value.split(',').map((s) => s.trim()).filter(Boolean) } })}
                placeholder="Feature 1, Feature 2, Feature 3"
              />
            </Field>
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={tierModal.item?.popular || false}
                onChange={(e) => setTierModal({ ...tierModal, item: { ...tierModal.item, popular: e.target.checked } })}
              />
              <span>Mark as 'Most Popular' plan</span>
            </label>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setTierModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveTier(tierModal.item)}>Save Tier</Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Add Cost Item Modal */}
      {costModal.open && (
        <Dialog title="Add Operational Cost Item" onClose={() => setCostModal({ open: false, item: null })}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Cost Item Title">
              <input
                value={costModal.item?.title || ''}
                onChange={(e) => setCostModal({ ...costModal, item: { ...costModal.item, title: e.target.value } })}
                placeholder="e.g. Cloud Hosting & Serverless"
                required
              />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Monthly Cost">
                <input
                  value={costModal.item?.monthlyCost || ''}
                  onChange={(e) => setCostModal({ ...costModal, item: { ...costModal.item, monthlyCost: e.target.value } })}
                  placeholder="e.g. $150"
                  required
                />
              </Field>
              <Field label="Type">
                <select
                  value={costModal.item?.type || 'Fixed'}
                  onChange={(e) => setCostModal({ ...costModal, item: { ...costModal.item, type: e.target.value } })}
                >
                  <option value="Fixed">Fixed</option>
                  <option value="Variable">Variable</option>
                </select>
              </Field>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setCostModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveCost(costModal.item)}>Add Cost</Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Add/Edit GTM Modal */}
      {gtmModal.open && (
        <Dialog title="Add/Edit GTM Channel" onClose={() => setGtmModal({ open: false, item: null })}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Channel Name">
              <input
                value={gtmModal.item?.channel || ''}
                onChange={(e) => setGtmModal({ ...gtmModal, item: { ...gtmModal.item, channel: e.target.value } })}
                placeholder="e.g. Micro-Influencer Affiliates"
                required
              />
            </Field>
            <Field label="Strategy & Tactical Execution">
              <textarea
                rows={2}
                value={gtmModal.item?.strategy || ''}
                onChange={(e) => setGtmModal({ ...gtmModal, item: { ...gtmModal.item, strategy: e.target.value } })}
              />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Status">
                <select
                  value={gtmModal.item?.status || 'Active'}
                  onChange={(e) => setGtmModal({ ...gtmModal, item: { ...gtmModal.item, status: e.target.value } })}
                >
                  <option value="Active">Active</option>
                  <option value="In Pipeline">In Pipeline</option>
                  <option value="Planned">Planned</option>
                </select>
              </Field>
              <Field label="Efficiency">
                <input
                  value={gtmModal.item?.efficiency || ''}
                  onChange={(e) => setGtmModal({ ...gtmModal, item: { ...gtmModal.item, efficiency: e.target.value } })}
                  placeholder="e.g. High (CAC < $10)"
                />
              </Field>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setGtmModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveGtm(gtmModal.item)}>Save Channel</Button>
            </div>
          </div>
        </Dialog>
      )}
    </section>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. PEOPLE & TEAM PANEL (FULL CRUD)
// ─────────────────────────────────────────────────────────────────────────────
export function PeopleTeamPanelEnhanced({ team, onSave, app }) {
  const [data, setData] = useState(team);
  const [memberModal, setMemberModal] = useState({ open: false, item: null });
  const [advisorModal, setAdvisorModal] = useState({ open: false, item: null });
  const [roleModal, setRoleModal] = useState({ open: false, item: null });
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState('Co-founder');

  useEffect(() => {
    setData(team);
  }, [team]);

  const updateAndSave = (next) => {
    setData(next);
    onSave(next);
  };

  const saveMember = (mem) => {
    const nextList = mem.id
      ? data.members.map((m) => (m.id === mem.id ? mem : m))
      : [...data.members, { ...mem, id: 'tm_' + Date.now() }];
    updateAndSave({ ...data, members: nextList });
    setMemberModal({ open: false, item: null });
    app?.toast?.('Team member saved.');
  };

  const deleteMember = (id) => {
    updateAndSave({ ...data, members: data.members.filter((m) => m.id !== id) });
    app?.toast?.('Team member removed.');
  };

  const saveAdvisor = (adv) => {
    const nextList = adv.id
      ? data.advisors.map((a) => (a.id === adv.id ? adv : a))
      : [...data.advisors, { ...adv, id: 'adv_' + Date.now() }];
    updateAndSave({ ...data, advisors: nextList });
    setAdvisorModal({ open: false, item: null });
    app?.toast?.('Advisor saved.');
  };

  const deleteAdvisor = (id) => {
    updateAndSave({ ...data, advisors: data.advisors.filter((a) => a.id !== id) });
    app?.toast?.('Advisor removed.');
  };

  const saveOpenRole = (role) => {
    const nextList = role.id
      ? data.openRoles.map((r) => (r.id === role.id ? role : r))
      : [...data.openRoles, { ...role, id: 'or_' + Date.now() }];
    updateAndSave({ ...data, openRoles: nextList });
    setRoleModal({ open: false, item: null });
    app?.toast?.('Open role updated.');
  };

  const deleteOpenRole = (id) => {
    updateAndSave({ ...data, openRoles: data.openRoles.filter((r) => r.id !== id) });
    app?.toast?.('Role removed.');
  };

  const handleSendInvite = (e) => {
    e.preventDefault();
    if (!inviteEmail.trim()) return;
    app?.toast?.(`Invitation sent to ${inviteEmail} for role: ${inviteRole}!`);
    setInviteEmail('');
  };

  return (
    <section className="dash-card">
      <SectionTitle title="People, Co-Founders & Team Building" />
      <p style={{ margin: '4px 0 16px', fontSize: '13px', color: 'var(--muted)' }}>
        Manage founding team roster, equity allocations, advisor network, and open candidate recruitment positions.
      </p>

      {/* Quick Invite Box */}
      <form
        onSubmit={handleSendInvite}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
          padding: '12px 16px',
          borderRadius: '8px',
          background: 'var(--soft)',
          border: '1px solid var(--line)',
          marginBottom: '20px',
          flexWrap: 'wrap',
        }}
      >
        <Mail size={18} color="var(--purple, #7c3aed)" />
        <span style={{ fontSize: '13px', fontWeight: 600 }}>Invite Collaborator:</span>
        <input
          type="email"
          placeholder="colleague@domain.com"
          value={inviteEmail}
          onChange={(e) => setInviteEmail(e.target.value)}
          style={{
            flex: 1,
            minWidth: '180px',
            padding: '6px 10px',
            borderRadius: '6px',
            border: '1px solid var(--line)',
            fontSize: '12px',
          }}
          required
        />
        <select
          value={inviteRole}
          onChange={(e) => setInviteRole(e.target.value)}
          style={{ padding: '6px 10px', borderRadius: '6px', border: '1px solid var(--line)', fontSize: '12px' }}
        >
          <option value="Co-founder">Co-founder</option>
          <option value="Lead Engineer">Lead Engineer</option>
          <option value="Advisor">Advisor</option>
          <option value="Contributor">Contributor</option>
        </select>
        <Button type="submit" variant="primary" icon={Send} style={{ fontSize: '12px', padding: '6px 14px' }}>
          Send Invite
        </Button>
      </form>

      {/* Core Team & Co-Founders */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
        <div>
          <h4 style={{ margin: 0, fontSize: '14px' }}>Co-Founders & Core Team ({data.members?.length || 0})</h4>
          <small style={{ color: 'var(--muted)' }}>Founding equity holders and operating leaders</small>
        </div>
        <Button
          variant="secondary"
          icon={Plus}
          onClick={() => setMemberModal({ open: true, item: { name: '', role: '', equity: '', skills: ['Full-stack'], bio: '', status: 'Active Co-founder' } })}
        >
          Add Member
        </Button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(290px, 1fr))', gap: '14px', marginBottom: '24px' }}>
        {data.members?.map((m) => (
          <div key={m.id} className="dash-card" style={{ border: '1px solid var(--line)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Avatar name={m.name} size="medium" />
                <div>
                  <b style={{ fontSize: '14px' }}>{m.name}</b>
                  <span style={{ display: 'block', fontSize: '12px', color: 'var(--muted)' }}>{m.role}</span>
                </div>
              </div>
              <div style={{ display: 'flex', gap: '4px' }}>
                <button
                  type="button"
                  className="icon-subtle-btn"
                  onClick={() => setMemberModal({ open: true, item: m })}
                >
                  <Edit2 size={12} />
                </button>
                <button
                  type="button"
                  className="icon-subtle-btn danger"
                  onClick={() => deleteMember(m.id)}
                >
                  <Trash2 size={12} />
                </button>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', margin: '6px 0' }}>
              <Tag tone="purple">Equity: {m.equity}</Tag>
              <Tag tone="green">{m.status}</Tag>
            </div>
            {m.bio && <p style={{ margin: '6px 0', fontSize: '12px', color: 'var(--muted)' }}>{m.bio}</p>}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', marginTop: '6px' }}>
              {m.skills?.map((s) => (
                <span key={s} style={{ fontSize: '11px', background: 'var(--soft)', padding: '2px 6px', borderRadius: '4px' }}>
                  {s}
                </span>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* Advisory Board */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
        <div>
          <h4 style={{ margin: 0, fontSize: '14px' }}>Advisory Board & Mentors ({data.advisors?.length || 0})</h4>
          <small style={{ color: 'var(--muted)' }}>Domain experts assisting with regulatory, GTM, and technical architecture</small>
        </div>
        <Button
          variant="secondary"
          icon={Plus}
          onClick={() => setAdvisorModal({ open: true, item: { name: '', expertise: '', firm: '', equity: '1.0%', cadence: 'Monthly' } })}
        >
          Add Advisor
        </Button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '14px', marginBottom: '24px' }}>
        {data.advisors?.map((adv) => (
          <div key={adv.id} className="dash-card" style={{ background: 'var(--soft)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '4px' }}>
              <div>
                <b style={{ fontSize: '14px' }}>{adv.name}</b>
                <span style={{ display: 'block', fontSize: '11px', color: 'var(--muted)' }}>{adv.firm} · {adv.expertise}</span>
              </div>
              <div style={{ display: 'flex', gap: '4px' }}>
                <button
                  type="button"
                  className="icon-subtle-btn"
                  onClick={() => setAdvisorModal({ open: true, item: adv })}
                >
                  <Edit2 size={12} />
                </button>
                <button
                  type="button"
                  className="icon-subtle-btn danger"
                  onClick={() => deleteAdvisor(adv.id)}
                >
                  <Trash2 size={12} />
                </button>
              </div>
            </div>
            <div style={{ display: 'flex', gap: '8px', fontSize: '11px', marginTop: '6px' }}>
              <Tag tone="gold">Advisory: {adv.equity}</Tag>
              <span style={{ color: 'var(--muted)' }}>Cadence: {adv.cadence}</span>
            </div>
          </div>
        ))}
      </div>

      {/* Open Roles & Recruitment */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
        <div>
          <h4 style={{ margin: 0, fontSize: '14px' }}>Open Roles / Seeking Co-Founders ({data.openRoles?.length || 0})</h4>
          <small style={{ color: 'var(--muted)' }}>Positions currently being recruited</small>
        </div>
        <Button
          variant="secondary"
          icon={Plus}
          onClick={() => setRoleModal({ open: true, item: { title: '', department: 'Engineering', type: 'Full-time / Equity', requirements: '', status: 'Open' } })}
        >
          Post Role
        </Button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '12px' }}>
        {data.openRoles?.map((r) => (
          <div key={r.id} className="dash-card" style={{ border: '1px solid var(--line)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '4px' }}>
              <div>
                <b style={{ fontSize: '13px' }}>{r.title}</b>
                <div style={{ display: 'flex', gap: '6px', marginTop: '2px' }}>
                  <Tag tone={r.status === 'Open' ? 'green' : 'gold'}>{r.status}</Tag>
                  <span style={{ fontSize: '11px', color: 'var(--muted)' }}>{r.department} · {r.type}</span>
                </div>
              </div>
              <div style={{ display: 'flex', gap: '4px' }}>
                <button
                  type="button"
                  className="icon-subtle-btn"
                  onClick={() => setRoleModal({ open: true, item: r })}
                >
                  <Edit2 size={12} />
                </button>
                <button
                  type="button"
                  className="icon-subtle-btn danger"
                  onClick={() => deleteOpenRole(r.id)}
                >
                  <Trash2 size={12} />
                </button>
              </div>
            </div>
            <p style={{ margin: '6px 0 0', fontSize: '12px', color: 'var(--muted)' }}>{r.requirements}</p>
          </div>
        ))}
      </div>

      {/* MODALS */}
      {/* Add/Edit Member Modal */}
      {memberModal.open && (
        <Dialog
          title={memberModal.item?.id ? 'Edit Team Member' : 'Add Team Member'}
          onClose={() => setMemberModal({ open: false, item: null })}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Full Name">
              <input
                value={memberModal.item?.name || ''}
                onChange={(e) => setMemberModal({ ...memberModal, item: { ...memberModal.item, name: e.target.value } })}
                placeholder="e.g. Mohit Koppera"
                required
              />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Title / Role">
                <input
                  value={memberModal.item?.role || ''}
                  onChange={(e) => setMemberModal({ ...memberModal, item: { ...memberModal.item, role: e.target.value } })}
                  placeholder="e.g. Co-founder & CTO"
                  required
                />
              </Field>
              <Field label="Equity Allocation">
                <input
                  value={memberModal.item?.equity || ''}
                  onChange={(e) => setMemberModal({ ...memberModal, item: { ...memberModal.item, equity: e.target.value } })}
                  placeholder="e.g. 25%"
                />
              </Field>
            </div>
            <Field label="Skills (comma separated)">
              <input
                value={memberModal.item?.skills?.join(', ') || ''}
                onChange={(e) => setMemberModal({ ...memberModal, item: { ...memberModal.item, skills: e.target.value.split(',').map((s) => s.trim()).filter(Boolean) } })}
                placeholder="React, Logistics, Finance"
              />
            </Field>
            <Field label="Short Bio & Experience">
              <textarea
                rows={2}
                value={memberModal.item?.bio || ''}
                onChange={(e) => setMemberModal({ ...memberModal, item: { ...memberModal.item, bio: e.target.value } })}
              />
            </Field>
            <Field label="Membership Status">
              <select
                value={memberModal.item?.status || 'Active Co-founder'}
                onChange={(e) => setMemberModal({ ...memberModal, item: { ...memberModal.item, status: e.target.value } })}
              >
                <option value="Active Co-founder">Active Co-founder</option>
                <option value="Early Team">Early Team</option>
                <option value="Part-time">Part-time</option>
                <option value="Contractor">Contractor</option>
              </select>
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setMemberModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveMember(memberModal.item)}>Save Member</Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Add/Edit Advisor Modal */}
      {advisorModal.open && (
        <Dialog title="Add/Edit Advisor" onClose={() => setAdvisorModal({ open: false, item: null })}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Advisor Name">
              <input
                value={advisorModal.item?.name || ''}
                onChange={(e) => setAdvisorModal({ ...advisorModal, item: { ...advisorModal.item, name: e.target.value } })}
                required
              />
            </Field>
            <Field label="Domain Expertise">
              <input
                value={advisorModal.item?.expertise || ''}
                onChange={(e) => setAdvisorModal({ ...advisorModal, item: { ...advisorModal.item, expertise: e.target.value } })}
              />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Firm / Background">
                <input
                  value={advisorModal.item?.firm || ''}
                  onChange={(e) => setAdvisorModal({ ...advisorModal, item: { ...advisorModal.item, firm: e.target.value } })}
                />
              </Field>
              <Field label="Advisory Equity">
                <input
                  value={advisorModal.item?.equity || ''}
                  onChange={(e) => setAdvisorModal({ ...advisorModal, item: { ...advisorModal.item, equity: e.target.value } })}
                />
              </Field>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setAdvisorModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveAdvisor(advisorModal.item)}>Save Advisor</Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Add/Edit Open Role Modal */}
      {roleModal.open && (
        <Dialog title="Post New Role" onClose={() => setRoleModal({ open: false, item: null })}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Role Title">
              <input
                value={roleModal.item?.title || ''}
                onChange={(e) => setRoleModal({ ...roleModal, item: { ...roleModal.item, title: e.target.value } })}
                placeholder="e.g. Lead Logistics Engineer"
                required
              />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Department">
                <input
                  value={roleModal.item?.department || ''}
                  onChange={(e) => setRoleModal({ ...roleModal, item: { ...roleModal.item, department: e.target.value } })}
                />
              </Field>
              <Field label="Employment Type">
                <input
                  value={roleModal.item?.type || ''}
                  onChange={(e) => setRoleModal({ ...roleModal, item: { ...roleModal.item, type: e.target.value } })}
                  placeholder="e.g. Full-time / Equity"
                />
              </Field>
            </div>
            <Field label="Key Requirements">
              <textarea
                rows={2}
                value={roleModal.item?.requirements || ''}
                onChange={(e) => setRoleModal({ ...roleModal, item: { ...roleModal.item, requirements: e.target.value } })}
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setRoleModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveOpenRole(roleModal.item)}>Post Role</Button>
            </div>
          </div>
        </Dialog>
      )}
    </section>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. FUNDING PREP PANEL (FULL CRUD)
// ─────────────────────────────────────────────────────────────────────────────
export function FundingPrepPanelEnhanced({ funding, onSave, app }) {
  const [data, setData] = useState(funding);
  const [editingRound, setEditingRound] = useState(false);
  const [materialModal, setMaterialModal] = useState({ open: false, item: null });
  const [pipelineModal, setPipelineModal] = useState({ open: false, item: null });

  useEffect(() => {
    setData(funding);
  }, [funding]);

  const updateAndSave = (next) => {
    setData(next);
    onSave(next);
  };

  const saveMaterial = (mat) => {
    const nextList = mat.id
      ? data.materials.map((m) => (m.id === mat.id ? mat : m))
      : [...data.materials, { ...mat, id: 'mat_' + Date.now(), updated: 'Today' }];
    updateAndSave({ ...data, materials: nextList });
    setMaterialModal({ open: false, item: null });
    app?.toast?.('Pitch material saved.');
  };

  const deleteMaterial = (id) => {
    updateAndSave({ ...data, materials: data.materials.filter((m) => m.id !== id) });
    app?.toast?.('Material deleted.');
  };

  const saveInvestor = (inv) => {
    const nextList = inv.id
      ? data.pipeline.map((i) => (i.id === inv.id ? inv : i))
      : [...data.pipeline, { ...inv, id: 'inv_' + Date.now() }];
    updateAndSave({ ...data, pipeline: nextList });
    setPipelineModal({ open: false, item: null });
    app?.toast?.('Investor pipeline contact saved.');
  };

  const deleteInvestor = (id) => {
    updateAndSave({ ...data, pipeline: data.pipeline.filter((i) => i.id !== id) });
    app?.toast?.('Investor removed.');
  };

  const toggleChecklistItem = (id) => {
    const nextList = data.readinessChecklist.map((item) =>
      item.id === id ? { ...item, done: !item.done } : item
    );
    updateAndSave({ ...data, readinessChecklist: nextList });
  };

  // Calculate live readiness score
  const readinessScore = data.readinessChecklist?.length
    ? Math.round(
        (data.readinessChecklist.filter((c) => c.done).length /
          data.readinessChecklist.length) *
          100
      )
    : 80;

  return (
    <section className="dash-card">
      <SectionTitle title="Funding Preparation, Pitch Deck & Investor CRM" />
      <p style={{ margin: '4px 0 16px', fontSize: '13px', color: 'var(--muted)' }}>
        Structure your fundraising round terms, organize pitch materials, track investor outreach pipeline, and measure readiness score.
      </p>

      {/* Target Raise & Terms Banner */}
      <div
        style={{
          padding: '16px 20px',
          borderRadius: '10px',
          background: 'var(--soft)',
          border: '1px solid var(--line)',
          marginBottom: '20px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          flexWrap: 'wrap',
          gap: '16px',
        }}
      >
        <div>
          <span style={{ fontSize: '11px', textTransform: 'uppercase', letterSpacing: '0.05em', color: 'var(--muted)', fontWeight: 600 }}>
            {data.stage} ROUND · {data.instrument}
          </span>
          <h3 style={{ margin: '2px 0 4px', fontSize: '20px' }}>
            Raising {data.targetRaise} <small style={{ fontSize: '13px', color: 'var(--muted)' }}>at {data.valuationCap} Valuation Cap</small>
          </h3>
          <p style={{ margin: 0, fontSize: '13px', color: '#059669', fontWeight: 600 }}>
            Committed so far: {data.committed} · Min Ticket: {data.minCheck}
          </p>
        </div>
        <button
          type="button"
          className="edit-pill-btn"
          onClick={() => setEditingRound(true)}
        >
          <Edit2 size={13} />
          <span>Edit Round Terms</span>
        </button>
      </div>

      {/* Readiness Score Card */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        <div className="dash-card" style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
          <div style={{ textAlign: 'center' }}>
            <span style={{ fontSize: '32px', fontWeight: 800, color: readinessScore >= 80 ? '#10b981' : '#f59e0b', display: 'block' }}>
              {readinessScore}
            </span>
            <small style={{ color: 'var(--muted)' }}>out of 100</small>
          </div>
          <div>
            <b style={{ fontSize: '14px', display: 'block' }}>Funding Readiness Score</b>
            <p style={{ margin: '2px 0', fontSize: '12px', color: 'var(--muted)' }}>
              {readinessScore >= 80 ? 'Investor ready! Solid validation and cap table.' : 'Complete pending items below to boost readiness.'}
            </p>
          </div>
        </div>

        {/* Readiness Checklist */}
        <div className="dash-card">
          <b style={{ fontSize: '13px', display: 'block', marginBottom: '8px' }}>Readiness Checklist:</b>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            {data.readinessChecklist?.map((item) => (
              <label
                key={item.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  fontSize: '12px',
                  cursor: 'pointer',
                  color: item.done ? 'var(--ink)' : 'var(--muted)',
                }}
              >
                <input
                  type="checkbox"
                  checked={item.done}
                  onChange={() => toggleChecklistItem(item.id)}
                />
                <span style={{ textDecoration: item.done ? 'line-through' : 'none' }}>{item.label}</span>
              </label>
            ))}
          </div>
        </div>
      </div>

      {/* Pitch Materials & Links */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
        <div>
          <h4 style={{ margin: 0, fontSize: '14px' }}>Pitch Materials & Data Room ({data.materials?.length || 0})</h4>
          <small style={{ color: 'var(--muted)' }}>Decks, models, and one-pagers shared with angel investors</small>
        </div>
        <Button
          variant="secondary"
          icon={Plus}
          onClick={() => setMaterialModal({ open: true, item: { title: '', type: 'Presentation', url: '', status: 'Investor Ready' } })}
        >
          Add Material
        </Button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '14px', marginBottom: '24px' }}>
        {data.materials?.map((mat) => (
          <div key={mat.id} className="dash-card" style={{ background: 'var(--soft)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '6px' }}>
              <div>
                <b style={{ fontSize: '14px' }}>{mat.title}</b>
                <span style={{ display: 'block', fontSize: '11px', color: 'var(--muted)' }}>{mat.type} · Updated {mat.updated}</span>
              </div>
              <div style={{ display: 'flex', gap: '4px' }}>
                <button
                  type="button"
                  className="icon-subtle-btn"
                  onClick={() => setMaterialModal({ open: true, item: mat })}
                >
                  <Edit2 size={12} />
                </button>
                <button
                  type="button"
                  className="icon-subtle-btn danger"
                  onClick={() => deleteMaterial(mat.id)}
                >
                  <Trash2 size={12} />
                </button>
              </div>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '10px' }}>
              <Tag tone={mat.status === 'Investor Ready' ? 'green' : 'blue'}>{mat.status}</Tag>
              <a
                href={mat.url}
                target="_blank"
                rel="noreferrer"
                style={{ fontSize: '12px', fontWeight: 600, color: 'var(--purple, #7c3aed)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '4px' }}
              >
                View Deck <ExternalLink size={12} />
              </a>
            </div>
          </div>
        ))}
      </div>

      {/* Investor CRM Pipeline */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '10px' }}>
        <div>
          <h4 style={{ margin: 0, fontSize: '14px' }}>Investor Pipeline CRM ({data.pipeline?.length || 0})</h4>
          <small style={{ color: 'var(--muted)' }}>Track venture discussions, meeting status, and commitments</small>
        </div>
        <Button
          variant="secondary"
          icon={Plus}
          onClick={() => setPipelineModal({ open: true, item: { name: '', firm: '', stage: 'Initial Call', amount: '$25,000', notes: '' } })}
        >
          Add Investor
        </Button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(290px, 1fr))', gap: '12px' }}>
        {data.pipeline?.map((inv) => (
          <div key={inv.id} className="dash-card" style={{ border: '1px solid var(--line)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '6px' }}>
              <div>
                <b style={{ fontSize: '14px' }}>{inv.name}</b>
                <span style={{ display: 'block', fontSize: '12px', color: 'var(--muted)' }}>{inv.firm}</span>
              </div>
              <div style={{ display: 'flex', gap: '4px' }}>
                <button
                  type="button"
                  className="icon-subtle-btn"
                  onClick={() => setPipelineModal({ open: true, item: inv })}
                >
                  <Edit2 size={12} />
                </button>
                <button
                  type="button"
                  className="icon-subtle-btn danger"
                  onClick={() => deleteInvestor(inv.id)}
                >
                  <Trash2 size={12} />
                </button>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', margin: '4px 0' }}>
              <Tag tone={inv.stage.includes('Term Sheet') || inv.stage.includes('Committed') ? 'green' : 'gold'}>
                {inv.stage}
              </Tag>
              <Tag tone="purple">Target: {inv.amount}</Tag>
            </div>
            {inv.notes && <p style={{ margin: '6px 0 0', fontSize: '12px', color: 'var(--muted)' }}>{inv.notes}</p>}
          </div>
        ))}
      </div>

      {/* MODALS */}
      {/* Edit Round Terms Modal */}
      {editingRound && (
        <Dialog title="Edit Round Terms & Target" onClose={() => setEditingRound(false)}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Target Raise ($)">
                <input
                  value={data.targetRaise}
                  onChange={(e) => setData({ ...data, targetRaise: e.target.value })}
                  placeholder="e.g. $250,000"
                />
              </Field>
              <Field label="Valuation Cap ($)">
                <input
                  value={data.valuationCap}
                  onChange={(e) => setData({ ...data, valuationCap: e.target.value })}
                  placeholder="e.g. $2,500,000"
                />
              </Field>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Stage">
                <select
                  value={data.stage}
                  onChange={(e) => setData({ ...data, stage: e.target.value })}
                >
                  <option value="Pre-Seed">Pre-Seed</option>
                  <option value="Seed">Seed</option>
                  <option value="Angel">Angel</option>
                  <option value="Series A">Series A</option>
                </select>
              </Field>
              <Field label="Instrument">
                <select
                  value={data.instrument}
                  onChange={(e) => setData({ ...data, instrument: e.target.value })}
                >
                  <option value="Post-Money SAFE">Post-Money SAFE</option>
                  <option value="Pre-Money SAFE">Pre-Money SAFE</option>
                  <option value="Convertible Note">Convertible Note</option>
                  <option value="Priced Equity Round">Priced Equity Round</option>
                </select>
              </Field>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Committed Capital So Far">
                <input
                  value={data.committed}
                  onChange={(e) => setData({ ...data, committed: e.target.value })}
                  placeholder="e.g. $85,000"
                />
              </Field>
              <Field label="Minimum Check Size">
                <input
                  value={data.minCheck}
                  onChange={(e) => setData({ ...data, minCheck: e.target.value })}
                  placeholder="e.g. $10,000"
                />
              </Field>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setEditingRound(false)}>Cancel</Button>
              <Button
                variant="primary"
                onClick={() => {
                  updateAndSave(data);
                  setEditingRound(false);
                  app?.toast?.('Funding round terms updated!');
                }}
              >
                Save Round Terms
              </Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Add/Edit Material Modal */}
      {materialModal.open && (
        <Dialog title="Add/Edit Pitch Material" onClose={() => setMaterialModal({ open: false, item: null })}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Document / Material Title">
              <input
                value={materialModal.item?.title || ''}
                onChange={(e) => setMaterialModal({ ...materialModal, item: { ...materialModal.item, title: e.target.value } })}
                placeholder="e.g. EcoCart Pre-Seed Pitch Deck"
                required
              />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Type">
                <select
                  value={materialModal.item?.type || 'Presentation'}
                  onChange={(e) => setMaterialModal({ ...materialModal, item: { ...materialModal.item, type: e.target.value } })}
                >
                  <option value="Presentation">Presentation / Deck</option>
                  <option value="Spreadsheet">Spreadsheet / Financial Model</option>
                  <option value="Document">Document / One-Pager</option>
                  <option value="Data Room">Virtual Data Room</option>
                </select>
              </Field>
              <Field label="Status">
                <select
                  value={materialModal.item?.status || 'Investor Ready'}
                  onChange={(e) => setMaterialModal({ ...materialModal, item: { ...materialModal.item, status: e.target.value } })}
                >
                  <option value="Investor Ready">Investor Ready</option>
                  <option value="Final Review">Final Review</option>
                  <option value="Draft">Draft</option>
                </select>
              </Field>
            </div>
            <Field label="DocSend / Drive URL">
              <input
                value={materialModal.item?.url || ''}
                onChange={(e) => setMaterialModal({ ...materialModal, item: { ...materialModal.item, url: e.target.value } })}
                placeholder="https://..."
                required
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setMaterialModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveMaterial(materialModal.item)}>Save Material</Button>
            </div>
          </div>
        </Dialog>
      )}

      {/* Add/Edit Investor Modal */}
      {pipelineModal.open && (
        <Dialog title="Add/Edit Investor Contact" onClose={() => setPipelineModal({ open: false, item: null })}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Investor / Partner Name">
                <input
                  value={pipelineModal.item?.name || ''}
                  onChange={(e) => setPipelineModal({ ...pipelineModal, item: { ...pipelineModal.item, name: e.target.value } })}
                  placeholder="e.g. Ananya Deshmukh"
                  required
                />
              </Field>
              <Field label="Venture Firm / Syndicate">
                <input
                  value={pipelineModal.item?.firm || ''}
                  onChange={(e) => setPipelineModal({ ...pipelineModal, item: { ...pipelineModal.item, firm: e.target.value } })}
                  placeholder="e.g. SeedBloom Climate"
                />
              </Field>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Pipeline Stage">
                <select
                  value={pipelineModal.item?.stage || 'Initial Call'}
                  onChange={(e) => setPipelineModal({ ...pipelineModal, item: { ...pipelineModal.item, stage: e.target.value } })}
                >
                  <option value="Initial Call">Initial Call</option>
                  <option value="Partner Meeting">Partner Meeting</option>
                  <option value="Due Diligence">Due Diligence</option>
                  <option value="Term Sheet Discussion">Term Sheet Discussion</option>
                  <option value="Committed">Committed</option>
                  <option value="Passed">Passed</option>
                </select>
              </Field>
              <Field label="Check Amount Target">
                <input
                  value={pipelineModal.item?.amount || ''}
                  onChange={(e) => setPipelineModal({ ...pipelineModal, item: { ...pipelineModal.item, amount: e.target.value } })}
                  placeholder="e.g. $50,000"
                />
              </Field>
            </div>
            <Field label="Notes & Next Steps">
              <textarea
                rows={2}
                value={pipelineModal.item?.notes || ''}
                onChange={(e) => setPipelineModal({ ...pipelineModal, item: { ...pipelineModal.item, notes: e.target.value } })}
                placeholder="Meeting notes, objections, timeline..."
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setPipelineModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveInvestor(pipelineModal.item)}>Save Investor</Button>
            </div>
          </div>
        </Dialog>
      )}
    </section>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// 9. TASKS PANEL (FULL CRUD KANBAN)
// ─────────────────────────────────────────────────────────────────────────────
export function CompanyTasksPanelEnhanced({ tasks, onSave, app }) {
  const [taskList, setTaskList] = useState(tasks || []);
  const [taskModal, setTaskModal] = useState({ open: false, item: null });
  const [search, setSearch] = useState('');

  useEffect(() => {
    setTaskList(tasks || []);
  }, [tasks]);

  const updateAndSave = (next) => {
    setTaskList(next);
    onSave(next);
  };

  const columns = ['To do', 'In progress', 'Review', 'Done'];

  const saveTask = (task) => {
    const nextList = task.id
      ? taskList.map((t) => (t.id === task.id ? task : t))
      : [...taskList, { ...task, id: 'task_' + Date.now() }];
    updateAndSave(nextList);
    setTaskModal({ open: false, item: null });
    app?.toast?.('Task updated.');
  };

  const deleteTask = (id) => {
    updateAndSave(taskList.filter((t) => t.id !== id));
    app?.toast?.('Task removed.');
  };

  const advanceTask = (task) => {
    const nextCol = {
      'To do': 'In progress',
      'In progress': 'Review',
      Review: 'Done',
      Done: 'To do',
    }[task.column];
    const nextList = taskList.map((t) => (t.id === task.id ? { ...t, column: nextCol } : t));
    updateAndSave(nextList);
  };

  const filteredTasks = taskList.filter((t) =>
    t.title.toLowerCase().includes(search.toLowerCase()) ||
    (t.description && t.description.toLowerCase().includes(search.toLowerCase())) ||
    (t.assignee && t.assignee.toLowerCase().includes(search.toLowerCase()))
  );

  return (
    <section className="dash-card">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <SectionTitle title="Company Tasks & Milestones Board" />
          <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--muted)' }}>
            Coordinate startup tasks, set assignees, track deadlines, and move work across Kanban stages.
          </p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <input
            type="text"
            placeholder="Search tasks..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{
              padding: '6px 12px',
              borderRadius: '8px',
              border: '1px solid var(--line)',
              fontSize: '12px',
              background: 'var(--surface)',
            }}
          />
          <Button
            variant="primary"
            icon={Plus}
            onClick={() => setTaskModal({ open: true, item: { title: '', description: '', priority: 'Medium', column: 'To do', assignee: 'Mohit Koppera', dueDate: '' } })}
          >
            New Task
          </Button>
        </div>
      </div>

      {/* Kanban Board Columns */}
      <div className="task-columns" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '14px' }}>
        {columns.map((column) => {
          const colTasks = filteredTasks.filter((t) => t.column === column);
          return (
            <div
              key={column}
              className="task-column"
              style={{
                background: 'var(--soft)',
                borderRadius: '10px',
                padding: '14px',
                display: 'flex',
                flexDirection: 'column',
                gap: '10px',
                border: '1px solid var(--line)',
                minHeight: '280px',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                <b style={{ fontSize: '13px', textTransform: 'uppercase', letterSpacing: '0.04em' }}>{column}</b>
                <Tag tone="purple">{colTasks.length}</Tag>
              </div>

              {colTasks.length === 0 && (
                <div style={{ textAlign: 'center', padding: '30px 10px', color: 'var(--muted)', fontSize: '12px' }}>
                  No tasks in {column}
                </div>
              )}

              {colTasks.map((task) => (
                <div
                  key={task.id}
                  style={{
                    background: 'var(--surface)',
                    border: '1px solid var(--line)',
                    borderRadius: '8px',
                    padding: '12px',
                    boxShadow: '0 1px 3px rgba(0,0,0,0.03)',
                    display: 'flex',
                    flexDirection: 'column',
                    gap: '6px',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                    <b style={{ fontSize: '13px', lineHeight: 1.3 }}>{task.title}</b>
                    <div style={{ display: 'flex', gap: '2px', flexShrink: 0 }}>
                      <button
                        type="button"
                        className="icon-subtle-btn"
                        onClick={() => setTaskModal({ open: true, item: task })}
                        title="Edit task"
                      >
                        <Edit2 size={12} />
                      </button>
                      <button
                        type="button"
                        className="icon-subtle-btn danger"
                        onClick={() => deleteTask(task.id)}
                        title="Delete task"
                      >
                        <Trash2 size={12} />
                      </button>
                    </div>
                  </div>

                  {task.description && (
                    <p style={{ margin: 0, fontSize: '12px', color: 'var(--muted)', wordBreak: 'break-word' }}>
                      {task.description}
                    </p>
                  )}

                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '6px', paddingTop: '6px', borderTop: '1px solid var(--line)', fontSize: '11px' }}>
                    <span style={{ color: 'var(--muted)' }}>{task.assignee || 'Unassigned'}</span>
                    <button
                      type="button"
                      onClick={() => advanceTask(task)}
                      title="Advance to next column"
                      style={{
                        background: 'var(--soft)',
                        border: '1px solid var(--line)',
                        borderRadius: '4px',
                        padding: '2px 6px',
                        fontSize: '11px',
                        cursor: 'pointer',
                        color: 'var(--purple, #7c3aed)',
                        fontWeight: 600,
                      }}
                    >
                      Move →
                    </button>
                  </div>
                </div>
              ))}
            </div>
          );
        })}
      </div>

      {/* Add/Edit Task Modal */}
      {taskModal.open && (
        <Dialog
          title={taskModal.item?.id ? 'Edit Task' : 'Create New Task'}
          onClose={() => setTaskModal({ open: false, item: null })}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Task Title">
              <input
                value={taskModal.item?.title || ''}
                onChange={(e) => setTaskModal({ ...taskModal, item: { ...taskModal.item, title: e.target.value } })}
                placeholder="e.g. Prepare seed round financial model"
                required
              />
            </Field>
            <Field label="Task Description">
              <textarea
                rows={3}
                value={taskModal.item?.description || ''}
                onChange={(e) => setTaskModal({ ...taskModal, item: { ...taskModal.item, description: e.target.value } })}
                placeholder="Additional context, deliverables, links..."
              />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Board Column">
                <select
                  value={taskModal.item?.column || 'To do'}
                  onChange={(e) => setTaskModal({ ...taskModal, item: { ...taskModal.item, column: e.target.value } })}
                >
                  <option value="To do">To do</option>
                  <option value="In progress">In progress</option>
                  <option value="Review">Review</option>
                  <option value="Done">Done</option>
                </select>
              </Field>
              <Field label="Priority">
                <select
                  value={taskModal.item?.priority || 'Medium'}
                  onChange={(e) => setTaskModal({ ...taskModal, item: { ...taskModal.item, priority: e.target.value } })}
                >
                  <option value="Low">Low</option>
                  <option value="Medium">Medium</option>
                  <option value="High">High</option>
                  <option value="Urgent">Urgent</option>
                </select>
              </Field>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Assignee">
                <input
                  value={taskModal.item?.assignee || ''}
                  onChange={(e) => setTaskModal({ ...taskModal, item: { ...taskModal.item, assignee: e.target.value } })}
                  placeholder="e.g. Mohit Koppera"
                />
              </Field>
              <Field label="Due Date">
                <input
                  value={taskModal.item?.dueDate || ''}
                  onChange={(e) => setTaskModal({ ...taskModal, item: { ...taskModal.item, dueDate: e.target.value } })}
                  placeholder="e.g. Oct 15, 2026"
                />
              </Field>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setTaskModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveTask(taskModal.item)}>Save Task</Button>
            </div>
          </div>
        </Dialog>
      )}
    </section>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// 10. DOCUMENTS PANEL (FULL CRUD)
// ─────────────────────────────────────────────────────────────────────────────
export function CompanyDocumentsPanelEnhanced({ documents, onSave, app }) {
  const [docList, setDocList] = useState(documents || []);
  const [filter, setFilter] = useState('All');
  const [search, setSearch] = useState('');
  const [docModal, setDocModal] = useState({ open: false, item: null });

  useEffect(() => {
    setDocList(documents || []);
  }, [documents]);

  const updateAndSave = (next) => {
    setDocList(next);
    onSave(next);
  };

  const categories = ['All', 'Pitch', 'Research', 'Product', 'Legal', 'General'];

  const saveDocument = (doc) => {
    const nextList = doc.id
      ? docList.map((d) => (d.id === doc.id ? doc : d))
      : [...docList, { ...doc, id: 'doc_' + Date.now(), date: 'Today' }];
    updateAndSave(nextList);
    setDocModal({ open: false, item: null });
    app?.toast?.('Document saved.');
  };

  const deleteDocument = (id) => {
    updateAndSave(docList.filter((d) => d.id !== id));
    app?.toast?.('Document deleted.');
  };

  const filteredDocs = docList.filter((d) => {
    const matchesFilter = filter === 'All' || d.category === filter;
    const matchesSearch =
      d.title.toLowerCase().includes(search.toLowerCase()) ||
      (d.notes && d.notes.toLowerCase().includes(search.toLowerCase()));
    return matchesFilter && matchesSearch;
  });

  return (
    <section className="dash-card">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <SectionTitle title="Company Documents & Cloud Resources" />
          <p style={{ margin: '4px 0 0', fontSize: '13px', color: 'var(--muted)' }}>
            Central repository for pitch decks, market research papers, legal contracts, and architecture diagrams.
          </p>
        </div>
        <Button
          variant="primary"
          icon={Plus}
          onClick={() => setDocModal({ open: true, item: { title: '', category: 'General', accessScope: 'Team Only', url: '', size: 'Cloud Doc', notes: '' } })}
        >
          Add Document
        </Button>
      </div>

      {/* Filter and Search Bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px', flexWrap: 'wrap', gap: '10px' }}>
        <Tabs items={categories} value={filter} onChange={setFilter} />
        <input
          type="text"
          placeholder="Filter documents..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{
            padding: '6px 12px',
            borderRadius: '8px',
            border: '1px solid var(--line)',
            fontSize: '12px',
            background: 'var(--surface)',
          }}
        />
      </div>

      {/* Document Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(290px, 1fr))', gap: '14px' }}>
        {filteredDocs.map((doc) => (
          <div key={doc.id} className="dash-card" style={{ border: '1px solid var(--line)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', minWidth: 0 }}>
                <div style={{ padding: '8px', borderRadius: '8px', background: 'var(--soft)', color: 'var(--purple, #7c3aed)' }}>
                  <FileText size={20} />
                </div>
                <div style={{ minWidth: 0 }}>
                  <b style={{ fontSize: '13px', display: 'block', wordBreak: 'break-word' }}>{doc.title}</b>
                  <span style={{ fontSize: '11px', color: 'var(--muted)' }}>{doc.size || 'Web Link'} · {doc.date}</span>
                </div>
              </div>
              <div style={{ display: 'flex', gap: '4px', flexShrink: 0 }}>
                <button
                  type="button"
                  className="icon-subtle-btn"
                  onClick={() => setDocModal({ open: true, item: doc })}
                >
                  <Edit2 size={12} />
                </button>
                <button
                  type="button"
                  className="icon-subtle-btn danger"
                  onClick={() => deleteDocument(doc.id)}
                >
                  <Trash2 size={12} />
                </button>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '6px', margin: '6px 0' }}>
              <Tag tone="purple">{doc.category}</Tag>
              <Tag tone="green">{doc.accessScope || 'Team'}</Tag>
            </div>
            {doc.notes && <p style={{ margin: '6px 0', fontSize: '12px', color: 'var(--muted)' }}>{doc.notes}</p>}

            <div style={{ marginTop: '10px', paddingTop: '8px', borderTop: '1px solid var(--line)', display: 'flex', justifyContent: 'flex-end' }}>
              <a
                href={doc.url}
                target="_blank"
                rel="noreferrer"
                style={{
                  display: 'inline-flex',
                  alignItems: 'center',
                  gap: '4px',
                  fontSize: '12px',
                  fontWeight: 600,
                  color: 'var(--purple, #7c3aed)',
                  textDecoration: 'none',
                }}
              >
                Open File <ExternalLink size={12} />
              </a>
            </div>
          </div>
        ))}
      </div>

      {/* Add/Edit Document Modal */}
      {docModal.open && (
        <Dialog
          title={docModal.item?.id ? 'Edit Document Details' : 'Add Document / Cloud Link'}
          onClose={() => setDocModal({ open: false, item: null })}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Document Title">
              <input
                value={docModal.item?.title || ''}
                onChange={(e) => setDocModal({ ...docModal, item: { ...docModal.item, title: e.target.value } })}
                placeholder="e.g. EcoCart Investor Deck v2.4"
                required
              />
            </Field>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
              <Field label="Category">
                <select
                  value={docModal.item?.category || 'General'}
                  onChange={(e) => setDocModal({ ...docModal, item: { ...docModal.item, category: e.target.value } })}
                >
                  <option value="General">General</option>
                  <option value="Pitch">Pitch & Funding</option>
                  <option value="Research">Research & Validation</option>
                  <option value="Product">Product & MVP</option>
                  <option value="Legal">Legal & Compliance</option>
                </select>
              </Field>
              <Field label="Access Scope">
                <select
                  value={docModal.item?.accessScope || 'Team Only'}
                  onChange={(e) => setDocModal({ ...docModal, item: { ...docModal.item, accessScope: e.target.value } })}
                >
                  <option value="Team Only">Team Only</option>
                  <option value="Investors & Team">Investors & Team</option>
                  <option value="Public">Public</option>
                </select>
              </Field>
            </div>
            <Field label="Resource Link / URL">
              <input
                value={docModal.item?.url || ''}
                onChange={(e) => setDocModal({ ...docModal, item: { ...docModal.item, url: e.target.value } })}
                placeholder="https://drive.google.com/... or DocSend link"
                required
              />
            </Field>
            <Field label="Notes / Description">
              <textarea
                rows={2}
                value={docModal.item?.notes || ''}
                onChange={(e) => setDocModal({ ...docModal, item: { ...docModal.item, notes: e.target.value } })}
                placeholder="Summary of contents or instructions..."
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setDocModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveDocument(docModal.item)}>Save Document</Button>
            </div>
          </div>
        </Dialog>
      )}
    </section>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// 11. DISCUSSIONS PANEL (FULL CRUD)
// ─────────────────────────────────────────────────────────────────────────────
export function CompanyDiscussionsPanelEnhanced({ discussions, onSave, app }) {
  const [discList, setDiscList] = useState(discussions || []);
  const [editModal, setEditModal] = useState({ open: false, item: null });
  const [newTitle, setNewTitle] = useState('');
  const [newContent, setNewContent] = useState('');
  const [newTag, setNewTag] = useState('Update');
  const [commentInputs, setCommentInputs] = useState({});

  useEffect(() => {
    setDiscList(discussions || []);
  }, [discussions]);

  const updateAndSave = (next) => {
    setDiscList(next);
    onSave(next);
  };

  const handlePost = (e) => {
    e.preventDefault();
    if (!newContent.trim()) return;
    const newPost = {
      id: 'disc_' + Date.now(),
      author: app?.profile?.name || 'Mohit Koppera',
      avatar: (app?.profile?.name || 'Mohit Koppera').split(' ').map((n) => n[0]).join(''),
      title: newTitle.trim() || 'Team Update',
      content: newContent.trim(),
      date: 'Just now',
      tag: newTag,
      pinned: false,
      likes: 0,
      comments: [],
    };
    updateAndSave([newPost, ...discList]);
    setNewTitle('');
    setNewContent('');
    app?.toast?.('Discussion posted!');
  };

  const saveEdit = (item) => {
    const nextList = discList.map((d) => (d.id === item.id ? item : d));
    updateAndSave(nextList);
    setEditModal({ open: false, item: null });
    app?.toast?.('Discussion updated.');
  };

  const deletePost = (id) => {
    updateAndSave(discList.filter((d) => d.id !== id));
    app?.toast?.('Discussion removed.');
  };

  const likePost = (id) => {
    const nextList = discList.map((d) =>
      d.id === id ? { ...d, likes: (d.likes || 0) + 1 } : d
    );
    updateAndSave(nextList);
  };

  const addComment = (discId) => {
    const commentText = commentInputs[discId];
    if (!commentText || !commentText.trim()) return;
    const newComm = {
      id: 'c_' + Date.now(),
      author: app?.profile?.name || 'Mohit Koppera',
      content: commentText.trim(),
      date: 'Just now',
    };
    const nextList = discList.map((d) =>
      d.id === discId ? { ...d, comments: [...(d.comments || []), newComm] } : d
    );
    updateAndSave(nextList);
    setCommentInputs({ ...commentInputs, [discId]: '' });
  };

  return (
    <section className="dash-card">
      <SectionTitle title="Workspace Discussions & Team Updates" />
      <p style={{ margin: '4px 0 16px', fontSize: '13px', color: 'var(--muted)' }}>
        Share updates, ask questions, debate key trade-offs, and announce sprint milestones.
      </p>

      {/* New Discussion Composer */}
      <form
        onSubmit={handlePost}
        style={{
          padding: '16px',
          borderRadius: '10px',
          background: 'var(--soft)',
          border: '1px solid var(--line)',
          marginBottom: '24px',
          display: 'flex',
          flexDirection: 'column',
          gap: '12px',
        }}
      >
        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '10px' }}>
          <input
            type="text"
            placeholder="Topic title (e.g. Pricing test results)..."
            value={newTitle}
            onChange={(e) => setNewTitle(e.target.value)}
            style={{
              padding: '8px 12px',
              borderRadius: '6px',
              border: '1px solid var(--line)',
              fontSize: '13px',
              background: 'var(--surface)',
            }}
          />
          <select
            value={newTag}
            onChange={(e) => setNewTag(e.target.value)}
            style={{
              padding: '8px 12px',
              borderRadius: '6px',
              border: '1px solid var(--line)',
              fontSize: '13px',
              background: 'var(--surface)',
            }}
          >
            <option value="Update">Update</option>
            <option value="Milestone">Milestone</option>
            <option value="Question">Question</option>
            <option value="Design">Design</option>
            <option value="Legal">Legal</option>
          </select>
        </div>

        <textarea
          rows={3}
          placeholder="Share your thoughts or discussion points with the workspace..."
          value={newContent}
          onChange={(e) => setNewContent(e.target.value)}
          required
          style={{
            padding: '10px 12px',
            borderRadius: '6px',
            border: '1px solid var(--line)',
            fontSize: '13px',
            background: 'var(--surface)',
          }}
        />

        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Button type="submit" variant="primary" icon={Send}>
            Post Discussion
          </Button>
        </div>
      </form>

      {/* Discussion Stream */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
        {discList.map((disc) => (
          <div
            key={disc.id}
            className="dash-card"
            style={{
              border: disc.pinned ? '1px solid #7c3aed' : '1px solid var(--line)',
              padding: '16px',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Avatar name={disc.author} size="small" />
                <div>
                  <b style={{ fontSize: '13px' }}>{disc.author}</b>
                  <span style={{ display: 'block', fontSize: '11px', color: 'var(--muted)' }}>{disc.date}</span>
                </div>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Tag tone="purple">{disc.tag}</Tag>
                {disc.pinned && <Tag tone="gold">Pinned</Tag>}
                <button
                  type="button"
                  className="icon-subtle-btn"
                  onClick={() => setEditModal({ open: true, item: disc })}
                  title="Edit post"
                >
                  <Edit2 size={12} />
                </button>
                <button
                  type="button"
                  className="icon-subtle-btn danger"
                  onClick={() => deletePost(disc.id)}
                  title="Delete post"
                >
                  <Trash2 size={12} />
                </button>
              </div>
            </div>

            <h4 style={{ margin: '4px 0 6px', fontSize: '15px' }}>{disc.title}</h4>
            <p style={{ margin: 0, fontSize: '13px', lineHeight: 1.5, color: 'var(--ink)' }}>
              {disc.content}
            </p>

            {/* Reactions & Reply Toggle */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginTop: '12px', paddingTop: '8px', borderTop: '1px solid var(--line)' }}>
              <button
                type="button"
                onClick={() => likePost(disc.id)}
                style={{
                  background: 'none',
                  border: 'none',
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                  fontSize: '12px',
                  color: 'var(--muted)',
                }}
              >
                <ThumbsUp size={14} color="#7c3aed" />
                <span>{disc.likes || 0} Likes</span>
              </button>
              <span style={{ fontSize: '12px', color: 'var(--muted)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                <MessageCircle size={14} />
                <span>{disc.comments?.length || 0} Comments</span>
              </span>
            </div>

            {/* Existing Comments */}
            {disc.comments && disc.comments.length > 0 && (
              <div style={{ marginTop: '12px', display: 'flex', flexDirection: 'column', gap: '8px', paddingLeft: '16px', borderLeft: '2px solid var(--line)' }}>
                {disc.comments.map((comm) => (
                  <div key={comm.id} style={{ background: 'var(--soft)', padding: '8px 12px', borderRadius: '6px' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '2px' }}>
                      <b style={{ fontSize: '12px' }}>{comm.author}</b>
                      <small style={{ color: 'var(--muted)', fontSize: '10px' }}>{comm.date}</small>
                    </div>
                    <p style={{ margin: 0, fontSize: '12px' }}>{comm.content}</p>
                  </div>
                ))}
              </div>
            )}

            {/* Add Comment Input */}
            <div style={{ display: 'flex', gap: '6px', marginTop: '10px' }}>
              <input
                type="text"
                placeholder="Write a reply..."
                value={commentInputs[disc.id] || ''}
                onChange={(e) => setCommentInputs({ ...commentInputs, [disc.id]: e.target.value })}
                onKeyDown={(e) => { if (e.key === 'Enter') addComment(disc.id); }}
                style={{
                  flex: 1,
                  padding: '6px 10px',
                  fontSize: '12px',
                  borderRadius: '6px',
                  border: '1px solid var(--line)',
                  background: 'var(--surface)',
                }}
              />
              <Button
                variant="secondary"
                onClick={() => addComment(disc.id)}
                style={{ fontSize: '12px', padding: '6px 12px' }}
              >
                Reply
              </Button>
            </div>
          </div>
        ))}
      </div>

      {/* Edit Discussion Modal */}
      {editModal.open && (
        <Dialog title="Edit Discussion Post" onClose={() => setEditModal({ open: false, item: null })}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Field label="Discussion Title">
              <input
                value={editModal.item?.title || ''}
                onChange={(e) => setEditModal({ ...editModal, item: { ...editModal.item, title: e.target.value } })}
              />
            </Field>
            <Field label="Category Tag">
              <select
                value={editModal.item?.tag || 'Update'}
                onChange={(e) => setEditModal({ ...editModal, item: { ...editModal.item, tag: e.target.value } })}
              >
                <option value="Update">Update</option>
                <option value="Milestone">Milestone</option>
                <option value="Question">Question</option>
                <option value="Design">Design</option>
                <option value="Legal">Legal</option>
              </select>
            </Field>
            <Field label="Content">
              <textarea
                rows={4}
                value={editModal.item?.content || ''}
                onChange={(e) => setEditModal({ ...editModal, item: { ...editModal.item, content: e.target.value } })}
              />
            </Field>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <Button variant="ghost" onClick={() => setEditModal({ open: false, item: null })}>Cancel</Button>
              <Button variant="primary" onClick={() => saveEdit(editModal.item)}>Save Post</Button>
            </div>
          </div>
        </Dialog>
      )}
    </section>
  );
}
