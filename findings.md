# Findings

## Project Context

- Workspace: `D:\My\Java\project\contentPublish`
- Shell rule: every command must be prefixed with `rtk`.
- Current git status includes a pre-existing modification to `index.html`; treat it as user work unless explicitly editing frontend.
- Existing project now includes a Java 21 + Spring Boot 3.3.13 MVP under `src/`, plus documentation, prompts, hooks, tests, and one pre-existing modified prototype `index.html`.

## Orchestrator vs OverviewAgent

- Orchestrator is the coordination role in this session. It reads broad docs, extracts minimal context, updates module `agent.md` files, runs Hooks, and sequences module work.
- `OverviewAgent` is a Java business module. It owns `/api/session/init`, WebSocket connection handling, event wrapping, and user-token-targeted incremental pushes.
- The Orchestrator may update `agentsPrompt/OverviewAgent/agent.md`; Java `OverviewAgent` must not absorb Task/Media/Adapt/Publish business logic.

## Source Documents Read

- Root `agent.md`: Java 21 + Spring Boot 3.3, H2, local media, WebSocket, LangChain4j with template fallback, mock publish as MVP main path.
- `agentsPrompt/CODEX_AGENT_CONTRACT.md`: priority rules, lowercase platform/mode values, uppercase statuses, `publishMode` nullable during adaptation, WebSocket/session shape.
- `Pulse-Distro-Java轻量实践方案.md`: recommended package layout, data model tables, `NormalizedContent`, `MediaRef`, `AdaptedContent`, async adaptation, MockPublisher, optional plugin extension.
- `接口文档/java接口文档.md`: concrete HTTP routes, request/response examples, unified envelope, session token headers, WebSocket events, fallback query flow.
- Existing module `agent.md` files: already contain first-pass module responsibilities and route boundaries; they need richer minimal context for data models, source paths, `.env`, Hook rhythm, and cross-module contracts.

## API Boundary Summary

- Session: `POST /api/session/init`; WebSocket `/ws/pipeline?userToken={userToken}&traceId={traceId}`.
- Task: `POST /api/tasks`, `GET /api/tasks/{taskId}`, `GET/PUT /api/tasks/{taskId}/normalized`.
- Media: `POST /api/tasks/{taskId}/media`, `GET /api/tasks/{taskId}/media`, `GET /media/{mediaId}`, `DELETE /api/tasks/{taskId}/media/{mediaId}`.
- Adapt/records: `POST /api/tasks/{taskId}/adapt`, `GET /api/tasks/{taskId}/records`, `GET/PUT /api/records/{recordId}`, `POST /api/records/{recordId}/skip`.
- Publish/mock: `POST /api/tasks/{taskId}/publish`, `GET /mock/{platform}/{recordId}`, `GET /api/mock/{platform}/{recordId}`.
- Config: `GET /api/configs/platforms`, `GET /api/configs/platforms/{platform}`.
- Plugin optional: `POST /api/plugin/register`, `POST /api/plugin/heartbeat`, `GET /api/plugin/status`, `POST /api/plugin/publish-status`.

## Data Model Summary

- `ContentTask`: task metadata, source type, raw content, normalized JSON, cover media, status, timestamps.
- `MediaResource`: task-owned media metadata, local storage key, public URL, SHA-256, dimensions, status.
- `PlatformPublishRecord`: per-platform adapted content, tags/media JSON, nullable publish mode until publishing, status, publish URL, error, timestamps.
- `PlatformConfig`: platform ID, display name, full JSON config, enabled flag.
- Plugin online state: optional real-publish extension currently uses an in-memory `PluginManager` keyed by user token/session ID.
- Standard records: `MediaRef`, `ContentBlock`, `NormalizedContent`, `AdaptedContent`.

## Implementation Summary

- Maven project: `pom.xml`, `src/main/resources/application.yml`, and `src/test/resources/application-test.yml`.
- Stage 1: Task and Media services/controllers, JPA entities/repositories, normalized content models, local image storage, `/media/{mediaId}` binary access.
- Stage 2: platform config JSON loading/persistence, async adaptation placeholders, template fallback generation, record edit/skip/detail/list.
- Stage 3: session init, WebSocket registration and userToken-scoped event publishing, mock publishing, mock JSON and responsive mock HTML.
- Stage 4 optional: plugin register/heartbeat/status/publish callback and `real` publish online-state handoff.
- Tests: stage integration tests plus API smoke test cover 9 Java scenarios; Hook tests cover `.env` loading and cycle enforcement.

## Frontend Contract Notes

- Reference design is `index.html`; do not casually overwrite current user modifications.
- Use responsive flex/grid layout and avoid fixed widths or hard-coded percentage widths in new frontend work.
- Preserve Tailwind v4, Outfit headings, Inter body, `.glass-panel`, and `.glass-header` language.
- WebSocket must auto-reconnect and HTTP-fetch latest task/records on initialization or reconnect.
- Edited adapted text must be saved through `PUT /api/records/{recordId}` before publish.
- In `real` mode, publish controls must reflect plugin offline state gracefully.
