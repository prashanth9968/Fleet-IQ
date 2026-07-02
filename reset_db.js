const { Client } = require('pg');

const client = new Client({
  connectionString: 'postgres://neondb_owner:npg_Z9asKje6dyAJ@ep-red-brook-aoeu2wgh-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require'
});

async function reset() {
  try {
    await client.connect();
    console.log('Connected to Neon!');
    await client.query('DROP SCHEMA public CASCADE;');
    await client.query('CREATE SCHEMA public;');
    await client.query('CREATE EXTENSION IF NOT EXISTS "uuid-ossp";');
    await client.query('CREATE EXTENSION IF NOT EXISTS pgcrypto;');
    await client.query('CREATE EXTENSION IF NOT EXISTS postgis;');
    await client.query('CREATE EXTENSION IF NOT EXISTS pg_trgm;');
    console.log('Reset complete!');
  } catch (err) {
    console.error('Error:', err);
  } finally {
    await client.end();
  }
}

reset();
