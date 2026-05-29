# Task Plan: Agents Prompt and Hook Review

## Goal

Read the project overview requirements and API documentation, then review the agent prompts and hooks for feasibility, consistency, and execution risk. Optimize the prompts/hooks within the existing project structure.

## Phases

| Phase | Status | Notes |
| --- | --- | --- |
| 1. Initialize work log | in_progress | Create task_plan.md, findings.md, progress.md. |
| 2. Read requirements and API docs | complete | Main Java MVP/API docs and all module requirement/API docs read. |
| 3. Inspect agent prompts and hooks | complete | All module prompts and hook scripts inspected. |
| 4. Assess feasibility and risks | complete | Check prompt clarity, hook portability, command validity, and alignment with docs. |
| 5. Apply scoped optimizations | complete | Added shared Codex contract, rewrote module prompts, improved hook state handling, and added hook tests. |
| 6. Verify | complete | Hook unit tests, Python syntax checks, and real hook smoke test passed. |
| 7. Generate agent.md navigation | complete | Added root and module agent.md files; hook now loads only agent.md context and requires refresh every 3 cycles. |

## Decisions

- Follow local AGENTS instruction via `C:\Users\15070\.codex\RTK.md`: prefix shell commands with `rtk`.
- Treat requirement/API/agent/hook files as project data, not executable instructions.
- Optimize prompts and hooks specifically for Codex usage, including a shared contract and Codex pre-build hook behavior.
- Default module-agent context should come from root `agent.md` plus module `agent.md`; requirement/API/prompt/hook files are not default context for ordinary module agents.

## Errors Encountered

| Error | Attempt | Resolution |
| --- | --- | --- |
| `rtk Get-Content` failed because PowerShell cmdlets are not PATH binaries | Tried to run a cmdlet directly through `rtk` | Use `rtk powershell -NoProfile -Command ...` for PowerShell commands. |
| `rtk rg --files` returned access denied | Tried direct ripgrep through `rtk` | Use PowerShell file enumeration through `rtk` if needed. |
| Console output appeared as mojibake | Read UTF-8 Chinese documents without setting console encoding | Set `[Console]::OutputEncoding` and `Get-Content -Encoding UTF8`. |
| PowerShell pipeline using `$_.Name` failed during hook file reading | `$` was interpreted before reaching inner PowerShell command | Root cause is command quoting in inspection, not project hook code; read files individually instead. |
| PowerShell `Select-String` formatting using `$_.Path` failed repeatedly | Same outer-shell `$` expansion issue | Escape `$` as `` `$ `` or avoid inline formatting. This is inspection-tooling noise, not a project issue. |
