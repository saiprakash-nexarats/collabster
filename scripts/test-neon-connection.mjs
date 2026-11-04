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
  console.error('❌ DATABASE_URL is not set in process.env or .env');
  process.exit(1);
}

console.log('Connecting to Neon PostgreSQL using DATABASE_URL...');
const sql = neon(databaseUrl);

try {
  const result = await sql`SELECT NOW() as current_time, version() as pg_version, current_database() as db_name`;
  console.log('✅ Successfully connected to Neon PostgreSQL!');
  console.log('Database:', result[0].db_name);
  console.log('Postgres Version:', result[0].pg_version);
  console.log('Server Time:', result[0].current_time);
  process.exit(0);
} catch (err) {
  console.error('❌ Failed to connect to Neon:', err.message);
  process.exit(1);
}
