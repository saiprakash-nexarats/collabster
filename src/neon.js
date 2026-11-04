import { neon } from '@neondatabase/serverless';

/**
 * Neon PostgreSQL Serverless Client
 *
 * Configured according to Neon best practices for browser and serverless runtimes.
 * Connects over secure HTTPS/WebSocket endpoints.
 */
const databaseUrl = import.meta.env?.VITE_NEON_DATABASE_URL;

export const neonSql = databaseUrl ? neon(databaseUrl) : null;

/**
 * Health check probe for Neon PostgreSQL
 */
export async function pingNeon() {
  if (!neonSql) {
    return { ok: false, error: 'VITE_NEON_DATABASE_URL not configured' };
  }
  try {
    const [row] = await neonSql`SELECT NOW() as now, current_database() as db, version() as version`;
    return {
      ok: true,
      database: row.db,
      timestamp: row.now,
      version: row.version
    };
  } catch (error) {
    return {
      ok: false,
      error: error.message || String(error)
    };
  }
}
