# Task Plan: Pulse Distro Initialization and Java MVP Build

## Goal

Initialize the Pulse Distro project as the Orchestrator, synchronize minimal module context into each module `agent.md`, enforce Hook and `.env` contracts, then build the Java 21 + Spring Boot 3.3 MVP in dependency order.

## Active Rules

- Every local shell command must start with `rtk`.
- Treat the Orchestrator role as this coordination role; do not confuse it with Java `OverviewAgent`.
- Ordinary module work should read only root `agent.md`, the module `agentsPrompt/<ModuleAgent>/agent.md`, source, and tests.
- Before starting a module, run its Hook script with `rtk python hooks/<module>_hook.py`.
- If a Hook reports the 3-cycle limit, refresh root `agent.md` and the current module `agent.md` before continuing.
- Real `.env` stays local and ignored; `.env.example` is the commit-safe template.
- Behavior code changes follow TDD where practical: failing test, implementation, passing test.

## Phases

| Phase | Status | Notes |
| --- | --- | --- |
| 1. Load workflow and recover context | complete | Loaded Superpowers, planning files, RTK rule, and current git status. |
| 2. Read source project docs | complete | Read root `agent.md`, shared contract, Java practice plan, API doc, and existing module `agent.md` files. |
| 3. Extract module boundaries | complete | Built minimal API/data/dependency context for each module. |
| 4. Present initialization design | complete | Design was presented; continuation now proceeds under the persistent goal. |
| 5. Synchronize agent.md files | complete | Updated root and module agent guides with extracted context, Hook commands, source/test paths, and `.env` rules. |
| 6. Run Hook initialization | complete | Ran overview/task/media/config/adapt/publish/plugin hooks successfully; TaskAgent is now at cycle 3/3. |
| 7. Add `.env` isolation | complete | Added `.gitignore`, `.env.example`, local ignored `.env`, Hook `.env` loader, and tests. |
| 8. Stage 1 data foundation | complete | Implemented TaskAgent and MediaAgent model/storage/Markdown normalization with integration coverage. |
| 9. Stage 2 business logic | complete | Implemented ConfigAgent platform rules and AdaptAgent async template fallback with record APIs. |
| 10. Stage 3 publish and push | complete | Implemented PublishAgent mock publishing and OverviewAgent session/WebSocket event scaffolding. |
| 11. Stage 4 optional plugin | complete | Implemented PluginAgent register/heartbeat/status/publish callback without blocking mock MVP. |
| 12. Frontend integration verification | complete | Did not edit user-owned `index.html`; Mock HTML uses responsive glass styling and HTTP smoke verifies API flow. |
| 13. Final verification | complete | `rtk mvn test`, Hook Python tests, and Hook py_compile all passed. |

## Current Decisions

- User-provided requirements are the target scope; Java MVP implementation now covers stages 1-4.
- Existing `index.html` is modified in the worktree and must be treated as user-owned unless this task later explicitly edits it.
- API envelopes use JSON `code` values with HTTP 200 for expected business errors.
- `platform_publish_record.publish_mode` must allow null until publish begins.
- Hook `.env` support should be verified with tests before changing `hooks/base_hook.py`.
- `real` publish mode is a plugin-mediated extension: offline plugin returns business `code=400`; online plugin moves records to `PUBLISHING` and waits for callback.

## Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| Existing active goal prevented creating a new goal | Tried `create_goal` | Continue under the existing thread goal and update only when truly complete/blocked. |
| `rtk rg --files` returned access denied | Tried direct ripgrep through `rtk` | Use `rtk powershell -NoProfile -Command "Get-ChildItem ..."` for file enumeration. |
| Chinese Markdown displayed as mojibake | Read UTF-8 docs without `-Encoding UTF8` | Re-read with `Get-Content -Raw -Encoding UTF8`. |
| PowerShell `Select-String` formatting failed | Used inline `ForEach-Object { "$($_.LineNumber):$($_.Line)" }` inside nested quotes | Avoid inline `$_` formatting; use `Select-Object LineNumber,Line`. |
| PowerShell cleanup command lost `$p` variable | Used nested double quotes around a command containing `$p` | Re-ran with single-quoted `-Command` so PowerShell received `$p` literally. |
| `GET /api/tasks/{taskId}` returned 404 during API smoke | Added route to smoke test before implementation | Added `TaskService.getTaskSummary` and `TaskController#getTask`. |
| MockMvc JSON body showed mojibake in logs | Java text blocks were sent through default request encoding | Sent JSON request bodies as UTF-8 bytes in `PulseDistroApiSmokeTest`. |
