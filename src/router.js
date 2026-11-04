/**
 * Collobster SPA Router - Phase 3 Implementation
 * 
 * Provides canonical hash routing, entity deep-linking, legacy bookmark redirects,
 * browser back/forward history synchronization, and not-found handling.
 */

export const ROUTE_DEFS = [
  { page: 'Home', canonical: '#/feed', aliases: ['#Home', '#/Home', '#home', '#/home', '#/', '#'] },
  { page: 'Explore', canonical: '#/explore', aliases: ['#Explore', '#/Explore'] },
  { page: 'Learn', canonical: '#/learning', aliases: ['#Learn', '#/Learn', '#learn', '#/learn', '#/courses'] },
  { page: 'Career', canonical: '#/career', aliases: ['#Career', '#/Career', '#jobs', '#/jobs'] },
  { page: 'Mentor', canonical: '#/mentorship', aliases: ['#Mentor', '#/Mentor', '#mentor', '#/mentor'] },
  { page: 'Projects', canonical: '#/projects', aliases: ['#Projects', '#/Projects'] },
  { page: 'Company', canonical: '#/companies', aliases: ['#Company', '#/Company', '#company', '#/company'] },
  { page: 'Investment', canonical: '#/investments', aliases: ['#Investment', '#/Investment', '#investment', '#/investment'] },
  { page: 'Local services', canonical: '#/services', aliases: ['#Local services', '#Local%20services', '#/local-services', '#/services'] },
  { page: 'Events', canonical: '#/events', aliases: ['#Events', '#/Events'] },
  { page: 'Community', canonical: '#/communities', aliases: ['#Community', '#/Community', '#community', '#/community'] },
  { page: 'Messages', canonical: '#/messages', aliases: ['#Messages', '#/Messages'] },
  { page: 'Notifications', canonical: '#/notifications', aliases: ['#Notifications', '#/Notifications'] },
  { page: 'Profile', canonical: '#/profile', aliases: ['#Profile', '#/Profile'] },
  { page: 'Saved', canonical: '#/saved', aliases: ['#Saved', '#/Saved'] },
  { page: 'Settings', canonical: '#/settings', aliases: ['#Settings', '#/Settings'] }
];

export const ENTITY_ROUTES = [
  { type: 'conversation', page: 'Messages', prefix: '#/messages/' },
  { type: 'community', page: 'Community', prefix: '#/communities/' },
  { type: 'community_alt', page: 'Community', prefix: '#/community/' },
  { type: 'event', page: 'Events', prefix: '#/events/' },
  { type: 'job', page: 'Career', prefix: '#/career/' },
  { type: 'job_alt', page: 'Career', prefix: '#/jobs/' },
  { type: 'project', page: 'Projects', prefix: '#/projects/' },
  { type: 'course', page: 'Learn', prefix: '#/learning/' },
  { type: 'course_alt', page: 'Learn', prefix: '#/learn/' },
  { type: 'company', page: 'Company', prefix: '#/companies/' },
  { type: 'company_alt', page: 'Company', prefix: '#/company/' },
  { type: 'profile', page: 'Profile', prefix: '#/profile/' }
];

export const VALID_PAGES = ROUTE_DEFS.map(r => r.page);

/**
 * Parses any incoming window.location.hash into a structured Route object.
 * Handles canonical hashes, entity deep links, legacy hash bookmarks, and unknown paths.
 */
export function parseRoute(rawHash) {
  const hash = String(rawHash || '').trim();

  // Root or empty hash defaults to Feed (Home)
  if (!hash || hash === '#' || hash === '#/' || hash === '#/feed') {
    return {
      page: 'Home',
      canonicalHash: '#/feed',
      entityType: null,
      entityId: null,
      isNotFound: false,
      isLegacy: hash !== '#/feed'
    };
  }

  // 1. Check for Entity Deep Links (#/prefix/:entityId)
  for (const entityDef of ENTITY_ROUTES) {
    if (hash.toLowerCase().startsWith(entityDef.prefix.toLowerCase())) {
      const rawId = hash.slice(entityDef.prefix.length).split('?')[0].split('/')[0].trim();
      const entityId = decodeURIComponent(rawId);
      if (entityId) {
        const canonicalPrefix = entityDef.type.includes('_alt')
          ? ENTITY_ROUTES.find(e => e.type === entityDef.type.replace('_alt', ''))?.prefix || entityDef.prefix
          : entityDef.prefix;

        return {
          page: entityDef.page,
          canonicalHash: `${canonicalPrefix}${encodeURIComponent(entityId)}`,
          entityType: entityDef.type.replace('_alt', ''),
          entityId,
          isNotFound: false,
          isLegacy: entityDef.type.includes('_alt')
        };
      }
    }
  }

  // 2. Exact match against Canonical Route definitions
  const exactDef = ROUTE_DEFS.find(r => r.canonical.toLowerCase() === hash.toLowerCase());
  if (exactDef) {
    return {
      page: exactDef.page,
      canonicalHash: exactDef.canonical,
      entityType: null,
      entityId: null,
      isNotFound: false,
      isLegacy: false
    };
  }

  // 3. Match against Legacy Route Aliases (e.g. #Explore, #Community, #/local-services)
  const legacyDef = ROUTE_DEFS.find(r =>
    r.aliases.some(alias => alias.toLowerCase() === hash.toLowerCase())
  );
  if (legacyDef) {
    return {
      page: legacyDef.page,
      canonicalHash: legacyDef.canonical,
      entityType: null,
      entityId: null,
      isNotFound: false,
      isLegacy: true
    };
  }

  // 4. Fallback for raw single-word hashes (e.g. #Home, #explore)
  const cleaned = hash.replace(/^#\/?/, '').trim();
  const matchedPage = ROUTE_DEFS.find(r =>
    r.page.toLowerCase() === decodeURIComponent(cleaned).toLowerCase()
  );
  if (matchedPage) {
    return {
      page: matchedPage.page,
      canonicalHash: matchedPage.canonical,
      entityType: null,
      entityId: null,
      isNotFound: false,
      isLegacy: true
    };
  }

  // 5. Unknown route -> Not Found
  return {
    page: 'NotFound',
    canonicalHash: hash,
    entityType: null,
    entityId: null,
    isNotFound: true,
    isLegacy: false
  };
}

/**
 * Backward compatible page resolver for legacy callers
 */
export function resolvePage(hash) {
  const parsed = parseRoute(hash);
  return parsed.page;
}

/**
 * Formats a canonical route string given a page name or entity reference
 */
export function formatRoute(target, entityId = null) {
  if (!target) return '#/feed';

  if (entityId) {
    const entityDef = ENTITY_ROUTES.find(e => !e.type.includes('_alt') && (e.page === target || e.type === target));
    if (entityDef) {
      return `${entityDef.prefix}${encodeURIComponent(entityId)}`;
    }
  }

  const def = ROUTE_DEFS.find(r => r.page.toLowerCase() === target.toLowerCase() || r.canonical.toLowerCase() === target.toLowerCase());
  return def ? def.canonical : `#/feed`;
}

