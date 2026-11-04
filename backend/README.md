# 🚀 CollabSphere Backend

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue)
![Maven](https://img.shields.io/badge/Build-Maven-red)
![Status](https://img.shields.io/badge/Status-Active-success)
![Version](https://img.shields.io/badge/Version-v1.0-blue)

---

# 📖 Overview

CollabSphere is an enterprise collaboration platform that brings together startups, investors, brands, influencers, job seekers, mentors, and communities into a single ecosystem.

The backend is built using **Java 21** and **Spring Boot**, following a modular architecture that is designed to scale from a modular monolith to microservices as the platform grows.

---

# 🎯 Project Goal

The goal of CollabSphere is to build a unified collaboration platform where:

- 🚀 Startups can build and manage their companies.
- 💰 Investors can discover promising startups.
- 🏢 Brands can collaborate with creators and startups.
- 📱 Influencers can manage partnerships.
- 💼 Job Seekers can discover startup opportunities.
- 🎓 Mentors can guide startups.
- 🤝 Communities can connect and collaborate.

---

# ✨ Current Features

## 🔐 Authentication

- User Registration
- User Login
- JWT Authentication
- Refresh Token
- Role-Based Authorization
- Spring Security Integration

---

## 🚀 Startup Module

### Dashboard

- Startup Overview
- Completion Percentage
- Startup Statistics
- Recent Activities
- Recent Products
- Recent Documents
- Recent Jobs
- Quick Actions

### Startup Profile

- Company Information
- Headquarters
- Country & City
- Mission
- Vision
- Problem Statement
- Solution
- Business Model
- Revenue Model
- Market Size
- Competitive Advantage

### Products

- Create Product
- Update Product
- Delete Product
- View Products

### Documents

- Upload Documents
- Download Documents
- Rename Documents
- Delete Documents

### Hiring

- Create Jobs
- Update Jobs
- Delete Jobs
- View Jobs

### Funding

- Funding Management
- Funding Dashboard

### Membership

- Invite Members
- Join Startup
- Approve Requests
- Reject Requests
- Update Member Roles
- Remove Members
- Leave Startup
- Transfer Ownership

### Analytics

- Startup Overview Analytics
- Growth Analytics
- Product Analytics
- Hiring Analytics
- Funding Analytics
- Team Analytics

### Search

- Keyword Search
- Industry Filter
- Startup Stage Filter
- Startup Type Filter
- Verification Filter
- Pagination

### Settings

- Startup Settings
- Startup Configuration

### Public Startup View

- Public Startup Profile

---

# 📊 Module Status

| Module | Status |
|---------|:------:|
| Authentication | ✅ Complete |
| User Management | ✅ Complete |
| Startup Module | ✅ Complete |
| Investor Module | 🚧 In Progress |
| Career Module | 📅 Planned |
| Events Module | 📅 Planned |
| Feed Module | 📅 Planned |
| Messaging Module | 📅 Planned |
| Notification Service | 📅 Planned |
| Recommendation Engine | 📅 Planned |

---

# 🔒 Security

- JWT Authentication
- Spring Security
- BCrypt Password Encryption
- Role-Based Access Control (RBAC)
- Jakarta Bean Validation

---

# 🛠 Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 21 |
| Framework | Spring Boot |
| Security | Spring Security + JWT |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Build Tool | Maven |
| Utilities | Lombok |

---

# 📂 Project Structure

```text
src/main/java
│
├── auth
├── common
├── security
├── user
├── startup
│   ├── activity
│   ├── analytics
│   ├── completion
│   ├── dashboard
│   ├── documents
│   ├── funding
│   ├── hiring
│   ├── idea
│   ├── membership
│   ├── products
│   ├── profile
│   ├── search
│   ├── settings
│   ├── statistics
│   └── view
```

---

# 🏗 Architecture

Current Architecture

```text
Controller
      │
      ▼
Service
      │
      ▼
Repository
      │
      ▼
PostgreSQL Database
```

Future Architecture

```text
                   API Gateway
                        │
      ┌─────────────────┼─────────────────┐
      │                 │                 │
 Authentication     Startup Service   Investor Service
      │                 │                 │
      ├────────────── Future Services ──────────────┤
      │                                             │
 Career   Events   Feed   Messaging   Notifications
```

---

# 🚀 Getting Started

## Prerequisites

- Java 21
- Maven
- PostgreSQL

## Clone Repository

```bash
git clone https://github.com/MadhuriReddy-Nexarats/collabster.git
```

## Build Project

```bash
mvn clean install
```

## Run Application

```bash
mvn spring-boot:run
```

---

# 🗄 Database

- PostgreSQL
- Hibernate ORM
- Spring Data JPA

---

# 📌 Roadmap

## ✅ Completed

- Authentication
- User Management
- Startup Module

## 🚧 Currently Under Development

- Investor Module

## 📅 Planned

- Career Module
- Events Module
- Feed Module
- Messaging Module
- Notification Service
- Recommendation Engine
- AI Matching
- Shared Media Service

---

# 👩‍💻 Contributor

**Madhuri Reddy**

---

# 📄 License

This project is intended for educational, learning, and portfolio purposes unless otherwise specified.