# Findings

## Project Context

- Workspace: `D:\My\Java\project\contentPublish`
- Initial git status shows untracked directories: `agentsPrompt/`, `hooks/`, and `接口文档/`.
- Local shell rule: use `rtk` as a prefix for shell commands.

## Documents

- Root documents:
  - `Pulse-Distro-完整架构方案.md`: full architecture accepts Java core plus optional Python/FastAPI/Chrome extension direction, but still positions Mock publishing as stable MVP and real publishing as advanced extension.
  - `Pulse-Distro-Java轻量实践方案.md`: Java 21 + Spring Boot 3.3 single-Jar MVP, H2 file DB, local media storage, Vue 3 static frontend, async adaptation, WebSocket status pipeline, Mock publishing as the main path, Chrome extension as optional real-publish extension.
  - `接口文档/java接口文档.md`: API base `/api`, media and mock routes outside `/api`, unified JSON envelope, session init, userToken/traceId isolation, async adapt flow, media lifecycle, publish flow, plugin register/heartbeat/status, WebSocket event formats.
- Encoding note: files are UTF-8; PowerShell output needs explicit UTF-8 console encoding to avoid mojibake.

## Agents

- `OverviewAgent` prompt aligns with session initialization, WebSocket routing, event-driven boundaries, and avoiding business logic in global controllers.
- `AdaptAgent`: aligns with async `/api/tasks/{taskId}/adapt`, placeholder `platform_publish_record`, LangChain4j plus template fallback, and WebSocket completion/degraded events.
- `ConfigAgent`: aligns with platform config JSON loading, H2 persistence, and `/api/configs/platforms`.
- `TaskAgent`: aligns with content task CRUD and normalized content JSON model.
- `MediaAgent`: aligns with local media storage, `/media/{mediaId}`, metadata, SHA-256, and reference-safe deletion.
- `PublishAgent`: aligns with `Publisher`, MockPublisher, status transitions, `/mock/{platform}/{recordId}`, and `/api/mock/...`.
- `PluginAgent`: aligns with optional real-publish extension support, plugin register/heartbeat/status, and suspended status callback.

## Hooks

- `hooks/base_hook.py` computes workspace from its own path, loads the matching agent `requirements.md` and `api_spec.md`, increments `state.json`, and blocks after 5 cycles if `summary.md` is missing.
- Thin hook wrappers call `base_hook.run_hook("<AgentName>")`.

## Initial Risks

- Some docs/prompts use lowercase API values (`mode=mock/real`) while SQL comments/interface comments mention uppercase (`MOCK/REAL`). This can confuse agents and generated enums.
- `platform_publish_record.publish_mode` is declared `NOT NULL`, but adaptation placeholder records are created before publish mode is known and API examples return `publishMode: null`.
- The 5-cycle hook blocks forever once `summary.md` exists because it resets the counter in memory but does not require a fresh summary for the next 5 cycles. It also treats any stale summary as sufficient.
- The hook prints full docs every run. This is useful for forcing context, but can be noisy and token-heavy.
- Hook behavior depends on a Python runtime and mutable `state.json`; syntax is portable Python, but there is no explicit Python version guard or atomic state write.

## Optimizations Applied

- Added `agentsPrompt/CODEX_AGENT_CONTRACT.md` as the shared Codex contract for project mainline, enum casing, publish mode nullability, WebSocket/session rules, and summary cadence.
- Rewrote all module `prompt.md` files into a consistent Codex format: goal, startup context, responsibility boundary, implementation protocol, and acceptance checklist.
- Updated `hooks/base_hook.py` to:
  - Load the shared contract before module docs.
  - Use `cycle`, `max_cycles`, `last_summary_mtime`, and `last_run_at`.
  - Require a fresh `summary.md` after five cycles instead of accepting stale summaries forever.
  - Return explicit exit codes and write state atomically.
  - Support `CODEX_HOOK_OUTPUT=full` and `CODEX_HOOK_MAX_CHARS`.
- Added `tests/test_base_hook.py` for hook cycle state and summary freshness behavior.

## Agent.md Navigation Applied

- Added root `agent.md` with project description, module navigation, ignored files, overview-agent injection responsibility, global conventions, and 3-cycle update rule.
- Added module `agent.md` files for Overview, Task, Media, Config, Adapt, Publish, and Plugin agents.
- Updated `hooks/base_hook.py` to load only:
  - root `agent.md`
  - current module `agentsPrompt/<ModuleAgent>/agent.md`
- Updated hook cycle rule from 5-cycle `summary.md` freshness to 3-cycle root/module `agent.md` freshness.
- Ordinary module agents no longer need to read requirement docs, API docs, prompt files, hook files, or planning logs by default.
