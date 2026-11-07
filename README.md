# SmartResolve

SmartResolve is a prototype IT support ticket tool for the FR Channel Journey capstone. Employees can submit support requests; Spring AI suggests a summary, category, priority, troubleshooting steps, an initial response, and a support team. Support engineers remain responsible for reviewing recommendations and managing tickets.

## Current capabilities

- Create, assign, comment on, resolve, and update tickets
- Ask a local Ollama model for structured triage recommendations through Spring AI
- Retrieve up to three similar resolved tickets from pgvector and include their recorded fixes as AI context
- Store tickets, users, teams, comments, and status history in PostgreSQL
- Apply versioned schema migrations with Flyway
- Use the React dashboard to submit, triage, and track tickets

AI recommendations are suggestions only. If Ollama is unavailable or returns an invalid recommendation, ticket creation returns an explicit service error and does not save a partially triaged ticket.

## Technology

- Java 17 and Spring Boot 3.4
- Spring AI 1.0 with Ollama
- PostgreSQL 16 with pgvector, Spring Data JPA, and Flyway
- React and Vite
- API request and response records use the project's `RO` naming convention

The application currently seeds demo users and teams. Authentication is not implemented; do not expose this prototype publicly.

## Prerequisites

- Java 17
- Docker Desktop with Docker Compose
- Node.js and npm
- Ollama installed locally

No hosted LLM API key is needed. Install Ollama from [ollama.com/download](https://ollama.com/download); it runs the selected model on your machine. If we later switch to a hosted model, I’ll recommend provider options and show you how to obtain and store its key securely at that point.

## Start the application

1. Start PostgreSQL from the repository root:

   ```powershell
   docker compose up -d postgres
   ```

2. Install and start the local model once:

   ```powershell
   ollama pull llama3.2
   ollama pull nomic-embed-text
   ```

   Ollama normally stays available as a local service at `http://localhost:11434`.

3. Start the backend in the repository root:

   ```powershell
   .\gradlew bootRun
   ```

4. In a separate terminal, start the frontend:

   ```powershell
   cd frontend
   npm install
   npm run dev
   ```

Open http://localhost:5173. The web app proxies `/api` requests to Spring Boot on port 8080.

The default PostgreSQL connection is `smartresolve` / `smartresolve` on `localhost:5432` and is for local development only. Override it with `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`. Configure the Ollama endpoint and model with `OLLAMA_BASE_URL` and `SMARTRESOLVE_AI_MODEL`.
The embedding model can be changed with `SMARTRESOLVE_EMBEDDING_MODEL`. Similar resolved tickets must meet
the configured cosine-similarity cutoff (`SMARTRESOLVE_KNOWLEDGE_SIMILARITY_THRESHOLD`, default `0.75`);
lower-scoring results are omitted, so no unrelated-history section is shown when none qualify. The indexed
search text focuses on the original issue, while the resolution note is supplied only after a match qualifies.

When a support engineer resolves a ticket, they must add resolution notes. The notes are saved as a
resolution-type ticket comment and indexed in pgvector with the original title, category, and description.
Later ticket submissions search these records before AI triage; the ticket stores IDs for the related
examples so the dashboard can show which prior resolutions informed the recommendation. Ordinary comments
are not included in retrieval. Existing tickets resolved before this feature was added are not automatically
indexed; they need a backfill that indexes their recorded resolution notes before they can appear in AI context.
Existing vector entries created before this focused-search change should be reindexed so resolution wording
does not affect their similarity score.

## Main API routes

- `GET /api/tickets`
- `POST /api/tickets`
- `GET /api/tickets/{id}`
- `POST /api/tickets/{id}/assign`
- `PATCH /api/tickets/{id}/status`
- `POST /api/tickets/{id}/comments`
- `POST /api/tickets/{id}/resolve` (requires a `message` containing resolution notes)
- `GET /api/dashboard/stats`
- `GET /api/users`
- `GET /api/teams`

## Checks

Run backend tests:

```powershell
.\gradlew test
```

The test profile uses an in-memory H2 database and Flyway migrations, so tests do not require Docker or Ollama.

Run frontend checks:

```powershell
cd frontend
npm run lint
npm run build
```

## Session notes and working preferences

- Keep communication cool, friendly, and fun.
- Before changing project files, explain the analysis, options, and design plan, then wait for approval.
- Keep code straightforward to follow and debug; use newer Java features only when they improve clarity.
- 2026-11-04: The user requested code that reads like a maintainable, human-built application, with clear naming and high engineering standards.
- 2026-11-04: The user prefers request/response classes to use the `RO` suffix instead of `DTO`.
- 2026-11-06: The user approved integrating Spring AI with local Ollama (no hosted API key) and PostgreSQL before adding authentication.
- 2026-11-06: Docker and PostgreSQL/Ollama are not installed in the current development environment; backend tests use H2, and local Docker/Ollama setup steps are documented.
- 2026-11-06: Added pgvector retrieval of prior resolved tickets, traceable source references, and a resolution workflow that saves notes as ticket comments and indexes them for future triage. Authentication remains out of scope.
- Keep this file and `.github/copilot-instructions.md` as persistent context for future sessions.
