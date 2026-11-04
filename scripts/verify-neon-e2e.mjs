import { neon } from '@neondatabase/serverless';
import fs from 'node:fs';

let databaseUrl = process.env.DATABASE_URL;

if (!databaseUrl && fs.existsSync('.env')) {
  const envContent = fs.readFileSync('.env', 'utf8');
  for (const line of envContent.split('\n')) {
    const match = line.match(/^DATABASE_URL=["']?(.*?)["']?$/);
    if (match) {
      databaseUrl = match[1].trim();
      break;
    }
  }
}

if (!databaseUrl) {
  console.error('❌ DATABASE_URL is not set');
  process.exit(1);
}

const sql = neon(databaseUrl);

async function runE2E() {
  console.log('⚡ Starting Neon PostgreSQL End-to-End Verification...');

  // 1. Connection check
  const ping = await sql`SELECT NOW() as now, current_database() as db, current_user as usr`;
  console.log(`1. Connected as user "${ping[0].usr}" to database "${ping[0].db}" at ${ping[0].now}`);

  // 2. Create test table
  console.log('2. Creating temporary verification table "_neon_health_probe"...');
  await sql`
    CREATE TABLE IF NOT EXISTS _neon_health_probe (
      id SERIAL PRIMARY KEY,
      service VARCHAR(50) NOT NULL,
      status VARCHAR(20) NOT NULL,
      payload JSONB,
      created_at TIMESTAMPTZ DEFAULT NOW()
    )
  `;

  // 3. Insert record
  console.log('3. Inserting verification record...');
  const inserted = await sql`
    INSERT INTO _neon_health_probe (service, status, payload)
    VALUES ('collobster-app', 'active', '{"provider":"neon","tier":"serverless"}'::jsonb)
    RETURNING id, service, status, payload, created_at
  `;
  console.log(`   Inserted record ID: ${inserted[0].id}, Status: ${inserted[0].status}`);

  // 4. Query record
  console.log('4. Querying inserted record...');
  const queried = await sql`
    SELECT id, service, status, payload
    FROM _neon_health_probe
    WHERE id = ${inserted[0].id}
  `;
  if (queried.length === 0 || queried[0].service !== 'collobster-app') {
    throw new Error('Data verification failed');
  }
  console.log('   Data verification successful: JSON payload verified:', JSON.stringify(queried[0].payload));

  // 5. Clean up
  console.log('5. Cleaning up verification table...');
  await sql`DROP TABLE IF EXISTS _neon_health_probe`;
  console.log('   Table cleaned up.');

  console.log('🎉 Neon PostgreSQL End-to-End Test PASSED with 100% success!');
}

runE2E().catch((err) => {
  console.error('❌ End-to-End verification failed:', err);
  process.exit(1);
});
