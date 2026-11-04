# Collobster Web Application

A modern, full-featured web platform for builders, learners, and creators to connect, collaborate, learn, and grow.

## Architecture status

The checked-in web client is **React 18 + Vite**. The platform backend is a **Spring Boot modular monolith** using PostgreSQL, Flyway, Redis, JWT security, WebSockets, and an object-storage abstraction. It defines protected HTTP contracts for a separate FastAPI recommendation service.

Flutter, Next.js, and FastAPI are target deployables, not checked-in applications yet. Their required boundaries and migration rules are documented in [docs/TARGET_ARCHITECTURE.md](docs/TARGET_ARCHITECTURE.md).

## Features Included in This Web App

- **Feed & Social**: Post updates, share achievements, interactive polls, rich media embeds, likes, bookmarks, and threaded discussions.
- **Learning & Courses**: Comprehensive courses (Java & Spring Boot, Flutter & Dart, React & Next.js, Product Design, AI Agents, Go Microservices), live interactive classes, practice labs, study circles, certificates, and bookmarks.
- **Projects & Code Workspace**: Multi-file code workspace supporting Java 21, React, TypeScript, CSS, Markdown with live compilation checks and execution runner.
- **Professional & On-Demand Services**: Marketplace with 7 categories (Software & Cloud, Legal Opinion, Hardware & Repairs, CA & Tax, UI/UX Design, Facility Operations) with milestone-backed escrow protection and custom RFPs.
- **Careers & Opportunities**: Job board with filterable roles (Full-time, Internships, Remote, Hybrid), salary ranges, and application management.
- **Mentorship & 1:1 Sessions**: Expert directory (Java Architects, Flutter Leads, Design Directors, Founders), interactive booking flow, availability calendar, and ratings.
- **Companies & Startups**: Team formation, equity stages, task management, company document vault, and investor pitches.
- **Communities & Circles**: Public & private circles, permission-based membership approvals, discussion threads, and resource sharing.
- **Events & Ticketing**: Hackathons, meetups, round-based stages, QR pass generation, and calendar sync.
- **Real-Time Messaging**: Direct messaging, community chats, attachments, read receipts, and reactions.
- **Dual Mode (Offline Preview + Live Cloud)**: Operates 100% standalone out of the box with offline-first browser persistence, and automatically synchronizes with the backend API when connected.

## Quick Start

### 1. Install Dependencies
```bash
npm install
```

### 2. Run Locally in Development Mode
```bash
npm run dev
```
Open your browser at `http://localhost:5173`.

### 3. Build for Production
```bash
npm run build
```
The optimized static bundle is generated in the `dist/` directory.

### 4. Run Unit Tests
```bash
npm test
```

### 5. (Optional) Connect to Live Backend API
To synchronize with your backend instead of running in Preview Mode:
Create a `.env` file in the root directory:
```env
VITE_API_URL=https://collobster-api.onrender.com
```
Then run `npm run dev` or `npm run build`.

### Backend runtime

The backend is in `backend/` and requires Java 17. For the local H2 development profile:

```bash
cd backend
./mvnw spring-boot:run
```

For PostgreSQL and Redis through Docker Compose, copy `.env.example` to `.env`, replace every secret placeholder with unique values, then run:

```bash
docker compose up --build
```

Compose intentionally refuses to start when `POSTGRES_PASSWORD` or `JWT_SECRET` are missing.

### Test database note

The backend contains H2-compatible tests and PostgreSQL integration tests. The PostgreSQL tests currently target a local PostgreSQL instance (mostly `localhost:5433/collabster`), so they require that database before the full Maven suite can pass.

### 6. (Optional) Deploy to Cloudflare Workers / Pages
```bash
npx wrangler deploy
```
