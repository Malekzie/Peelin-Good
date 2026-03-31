# 🍊 Peelin' Good

An e-commerce bakery platform where customers can browse and order baked goods online, earn loyalty rewards, and leave product reviews. Built as a full-stack monorepo with a **SvelteKit** frontend and a **Spring Boot** backend, deployed on **DigitalOcean**.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Repository Structure](#repository-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [Backend](#backend)
  - [Frontend](#frontend)
- [Development Workflow](#development-workflow)
  - [Branching Strategy](#branching-strategy)
  - [Pull Requests](#pull-requests)
  - [CI/CD Pipelines](#cicd-pipelines)
  - [Code Quality](#code-quality)
- [Deployment](#deployment)
- [Environment Variables](#environment-variables)
- [Useful Commands](#useful-commands)

---

## Overview

Peelin' Good is a bakery e-commerce platform that supports:

- 🛒 **Online ordering** — customers browse products, place pickup or delivery orders from bakery locations
- 🏷️ **Product tagging & filtering** — products are organized with tags (breads, cakes, seasonal, dietary attributes, etc.)
- 👤 **Customer accounts** — registration, login, order history, and personalized preferences
- ⭐ **Reviews & ratings** — customers review purchased products, moderated by employees
- 🎁 **Loyalty rewards** — tiered reward system (Bronze, Silver, Gold) with points earned on orders
- 📦 **Inventory & batch tracking** — ingredient stock per bakery, supplier management, batch production records
- 👩‍🍳 **Employee management** — employees linked to bakeries, responsible for batch preparation and content moderation

The monorepo contains two independently deployable applications:

| App | Path | Description |
| --- | --- | --- |
| **Backend** | `apps/backend` | REST API built with Spring Boot 3.5, Java 21, PostgreSQL, and Flyway migrations |
| **Frontend** | `apps/frontend` | Web client built with SvelteKit 2, Svelte 5, TypeScript, and Tailwind CSS 4 |

Both apps are containerized with multi-stage Dockerfiles and deployed to DigitalOcean App Platform via GitHub Actions.

### Detailed Documentation

| Document | Description |
| --- | --- |
| [Backend Guide](docs/backend.md) | Setup, architecture, migrations, testing, and Docker for the Spring Boot API |
| [Frontend Guide](docs/frontend.md) | Setup, routing, styling, components, linting, and Docker for the SvelteKit client |
| [Contributing Guide](docs/contributing.md) | Branching strategy, PR workflow, CI/CD checks, CodeScene, and merge process |
| [Database Design](docs/database-design.md) | Full entity/relationship breakdown and problem statement |
| [ERD Diagram](docs/bakery_erd.pdf) | Visual entity-relationship diagram |

---

## Tech Stack

### Backend

- **Language:** Java 21
- **Framework:** Spring Boot 3.5.11
- **Database:** PostgreSQL
- **Migrations:** Flyway
- **Security:** Spring Security
- **Build Tool:** Maven (wrapper included — no global install needed)
- **Runtime Image:** Distroless Java 21

### Frontend

- **Language:** TypeScript
- **Framework:** SvelteKit 2 (Svelte 5) with `adapter-node`
- **Styling:** Tailwind CSS 4 with `tw-animate-css`, `tailwind-variants`, and `tailwind-merge`
- **UI Components:** shadcn-svelte conventions (`$lib/components/ui`, `$lib/utils.ts`)
- **Icons:** Lucide Svelte
- **Linting:** ESLint 9 + Prettier
- **Runtime:** Node.js 22

### Infrastructure

- **Hosting:** DigitalOcean App Platform
- **Container Registry:** GitHub Container Registry (`ghcr.io`)
- **CI/CD:** GitHub Actions
- **Code Analysis:** CodeScene Delta Analysis (on PRs)

---

## Repository Structure

```
.
├── .github/
│   └── workflows/
│       ├── backend-build.yml      # Backend CI: build, test, Docker, deploy
│       └── frontend-build.yml     # Frontend CI: lint, check, build, Docker, deploy
├── apps/
│   ├── backend/                   # Spring Boot API
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/sait/peelin/   # Application source code
│   │   │   │   └── resources/
│   │   │   │       ├── application.yaml     # Spring config
│   │   │   │       └── db/migration/        # Flyway SQL migrations
│   │   │   └── test/                        # Test source code
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   ├── mvnw / mvnw.cmd                 # Maven wrapper (no install needed)
│   │   └── HELP.md
│   └── frontend/                  # SvelteKit web client
│       ├── src/
│       │   ├── lib/
│       │   │   ├── assets/                  # Static assets (favicon, etc.)
│       │   │   ├── components/ui/           # Reusable UI components
│       │   │   ├── hooks/                   # Custom Svelte hooks
│       │   │   ├── utils.ts                 # Utility helpers (cn, types)
│       │   │   └── index.ts                 # Barrel exports
│       │   └── routes/                      # SvelteKit file-based routing
│       ├── Dockerfile
│       ├── package.json
│       ├── svelte.config.js
│       ├── vite.config.ts
│       ├── tsconfig.json
│       └── eslint.config.js
├── docs/
│   ├── backend.md                 # Backend setup, architecture, and development guide
│   ├── frontend.md                # Frontend setup, routing, styling, and development guide
│   ├── contributing.md            # Branching, PR workflow, CI/CD, and merge process
│   ├── database-design.md         # Full entity/relationship breakdown
│   └── bakery_erd.pdf             # Visual entity-relationship diagram
├── infra/                         # Infrastructure configuration
├── scripts/                       # Utility scripts
└── README.md                      # ← You are here
```

---

## Prerequisites

Make sure you have the following installed on your machine:

| Tool | Version | Notes |
| --- | --- | --- |
| **Java JDK** | 21+ | Required for the backend. [Temurin](https://adoptium.net/) recommended |
| **Node.js** | 22+ | Required for the frontend. Use [nvm](https://github.com/nvm-sh/nvm) or [fnm](https://github.com/Schniz/fnm) to manage versions |
| **npm** | Comes with Node | Used for frontend dependency management |
| **Docker** | Latest | Needed to build/test container images locally |
| **Git** | Latest | Obviously |
| **PostgreSQL** | 15+ | For local backend development. Alternatively, run it via Docker |

> **Note:** You do **not** need to install Maven globally. The project includes a Maven wrapper (`mvnw` / `mvnw.cmd`) that will automatically download the correct version.

---

## Getting Started

### Seeded role passwords (local/demo)

Use the following default passwords for seeded accounts by role:

- Customer: `Cust123!`
- Employee: `Emp123!`
- Admin: `Admin123!`

### Clone the Repository

```sh
git clone <repository-url>
cd Workshop7
```

### Backend

```sh
cd apps/backend

# Build and run tests
./mvnw verify

# Run the application in development mode
./mvnw spring-boot:run
```

> **Windows users:** Use `mvnw.cmd` instead of `./mvnw`.

The API will start on **http://localhost:8080**.

You will need a running PostgreSQL instance. Configure the connection in `apps/backend/src/main/resources/application.yaml` or via environment variables (see [Environment Variables](#environment-variables)).

Flyway will automatically run any pending migrations from `src/main/resources/db/migration/` on startup.

### Frontend

```sh
cd apps/frontend

# Install dependencies
npm install

# Start the dev server
npm run dev
```

The app will start on **http://localhost:5173** (Vite default).

---

## Development Workflow

### Branching Strategy

1. **`main`** is the production branch and is **protected**.
   - Direct pushes to `main` are not allowed.
   - Merges require a pull request.
   - PRs cannot be merged if there are **merge conflicts** — resolve them first.
2. Create a **feature branch** from `main` for your work:
   ```sh
   git checkout -b feature/your-feature-name
   ```
3. Push your branch and open a **Pull Request** against `main`.

### Pull Requests

When you open a PR targeting `main`, the following happens automatically:

1. **GitHub Actions CI** runs the relevant pipeline(s) based on which files changed:
   - Changes in `apps/backend/**` → Backend CI (build, test, Docker build)
   - Changes in `apps/frontend/**` → Frontend CI (lint, type check, build, Docker build)
2. **CodeScene Delta Analysis** analyzes your PR for code health, complexity trends, and potential risks.
3. A **PR preview environment** is deployed to DigitalOcean, and a comment is posted on the PR with the preview URL.
4. If any CI step fails, a comment is posted on the PR linking to the failed run's logs.

> ⚠️ **PRs that break the build cannot be merged.** The GitHub Actions workflows act as required status checks, so make sure your code passes all checks before requesting a review.

### CI/CD Pipelines

#### Backend CI (`backend-build.yml`)

| Trigger | Steps |
| --- | --- |
| **PR** to `main` | Checkout → JDK 21 setup → `mvnw verify` → Docker build → Deploy PR preview |
| **Push** to `main` | Checkout → JDK 21 setup → `mvnw verify` → Docker build & push to GHCR → Deploy to production |

#### Frontend CI (`frontend-build.yml`)

| Trigger | Steps |
| --- | --- |
| **PR** to `main` | Checkout → Node 22 setup → `npm ci` → Lint → Type check → Build → Docker build → Deploy PR preview |
| **Push** to `main` | Checkout → Node 22 setup → `npm ci` → Lint → Type check → Build → Docker build & push to GHCR → Deploy to production |

> Both pipelines are scoped by path — backend changes won't trigger the frontend pipeline and vice versa.

### Code Quality

- **CodeScene Delta Analysis** runs on every PR to catch code health regressions, complexity hotspots, and coupling issues.
- **Frontend linting** is enforced in CI via:
  - **ESLint 9** with TypeScript and Svelte plugins
  - **Prettier** with Svelte and Tailwind CSS plugins
- **Frontend type checking** runs `svelte-check` in CI.
- **Backend tests** run via `mvnw verify` (includes unit and integration tests).

Before pushing, run these locally to catch issues early:

```sh
# Frontend
cd apps/frontend
npm run lint          # Check linting + formatting
npm run format        # Auto-fix formatting
npm run check         # TypeScript / Svelte type checking

# Backend
cd apps/backend
./mvnw verify         # Compile + run all tests
```

---

## Deployment

The application is deployed on **DigitalOcean App Platform**.

- **Production deploys** happen automatically when code is merged into `main`.
- **Preview deploys** happen automatically on every PR.
- Docker images are pushed to **GitHub Container Registry** (`ghcr.io`) on production deploys.

The deployment flow:

```
PR Merge → GitHub Actions → Build & Test → Docker Image → Push to GHCR → Deploy to DigitalOcean
```

---

## Environment Variables

### Backend

The backend uses Spring Boot's configuration via `application.yaml` and environment variables. At minimum, you'll need to configure your PostgreSQL connection for local development.

Common Spring Boot environment variables:

| Variable | Description |
| --- | --- |
| `SPRING_DATASOURCE_URL` | JDBC connection string (e.g., `jdbc:postgresql://localhost:5432/peelin`) |
| `SPRING_DATASOURCE_USERNAME` | Database username |
| `SPRING_DATASOURCE_PASSWORD` | Database password |

### Frontend

The frontend uses SvelteKit's environment variable conventions. Create a `.env` file in `apps/frontend/`:

```sh
cp .env.example .env   # if an example file exists
```

> **Important:** Never commit `.env` files. They are git-ignored by default. Only `.env.example` and `.env.test` are allowed.

### CI/CD Secrets (GitHub)

These are configured in the repository's GitHub Settings → Secrets:

| Secret | Purpose |
| --- | --- |
| `DIGITALOCEAN_ACCESS_TOKEN_` | DigitalOcean API token for deployments |
| `GITHUB_TOKEN` | Automatically provided by GitHub for GHCR access |

---

## Useful Commands

### Backend (`apps/backend`)

| Command | Description |
| --- | --- |
| `./mvnw spring-boot:run` | Start the dev server |
| `./mvnw verify` | Build and run all tests |
| `./mvnw clean package -DskipTests` | Build JAR without tests |
| `docker build -t peelin-backend .` | Build Docker image locally |

### Frontend (`apps/frontend`)

| Command | Description |
| --- | --- |
| `npm run dev` | Start Vite dev server with HMR |
| `npm run build` | Production build |
| `npm run preview` | Preview the production build locally |
| `npm run lint` | Run ESLint + Prettier checks |
| `npm run format` | Auto-format code with Prettier |
| `npm run check` | Svelte/TypeScript type checking |
| `npm run check:watch` | Type checking in watch mode |
| `docker build -t peelin-frontend .` | Build Docker image locally |