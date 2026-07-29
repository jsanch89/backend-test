---
name: implement-task
description: Given a task ID (e.g. T05, T12), look up its spec in plan/TASKS.md, mark it in-progress in plan/STATUS.md, implement it, then mark it done. Use when the user says "implement task T05", "work on T04", "start task T09", "do the next task", or names a task ID from the plan.
---

Paths below are relative to the repo root (`backend-test/`).

## Inputs

- A task ID like `T05`. If the user didn't give one, ask which task,
  or offer the next `⬜ Pendiente` row from `plan/STATUS.md` (top to
  bottom, respecting the `Fase` order already encoded in the table).

## Steps

1. **Read the task spec.**
   Open `plan/TASKS.md` and find the section headed `### T{id} — ...`.
   Read that whole section: **Descripción**, **Pasos**, **Criterios de
   aceptación**, **Entregable** (and any table like T08's error-case
   table or T13's scenario table). This is the actual spec — don't
   guess from the title alone.

2. **Check dependencies.**
   `plan/TASKS.md` groups tasks into phases (Setup → Arquitectura →
   Adaptadores → Rendimiento/Resiliencia → Tests → Entrega) and later
   tasks assume earlier ones exist (e.g. T05 needs the ports from T04;
   T06 needs the generated interfaces from T03). If a prerequisite
   task is still `⬜ Pendiente` in `plan/STATUS.md`, tell the user and
   confirm whether to proceed anyway, implement the prerequisite
   first, or stop.

3. **Mark it in-progress.**
   In `plan/STATUS.md`, change that task's row: status cell
   `⬜ Pendiente` → `🔵 En progreso`. Do this with Edit before writing
   any code, so the tracker reflects reality even if the session gets
   interrupted mid-task.

4. **Implement.**
   Follow the **Pasos** from `plan/TASKS.md` for that task. Respect
   the module boundaries already defined in T04 (`domain` /
   `application` / `infrastructure` as separate Gradle modules —
   `domain` never depends on Spring, `application` depends only on
   `domain`, `infrastructure` depends on `application`). Use the
   **Criterios de aceptación** as your definition of done, and verify
   them concretely (e.g. run `./gradlew :module:test`, hit the
   endpoint, check the generated sources) rather than assuming.

5. **Mark it done.**
   Once the acceptance criteria are verifiably met, in
   `plan/STATUS.md`:
   - Set that row's status to `✅ Completada`.
   - Fill the `Comentario` column with a short note if anything
     deviated from the plan (e.g. a version bump, a skipped optional
     step) — leave it blank if it went exactly as planned.
   - Update the `**Progreso global:** X / 18 tareas completadas`
     line at the bottom of the file.

   If you get partway through and can't finish (blocked, needs user
   input), leave the status at `🔵 En progreso` (or set `⛔ Bloqueada`
   with a comment explaining the blocker) rather than reverting to
   `⬜ Pendiente` — that preserves the record of what was attempted.

## Notes

- Don't batch multiple tasks' status changes together — update
  `plan/STATUS.md` for one task at a time, in step with actual
  progress, so it stays a reliable source of truth if work is
  interrupted.
- `plan/TASKS.md` is the spec; `plan/STATUS.md` is only ever a status
  index. Never write task detail into `STATUS.md` beyond the terse
  `Comentario` cell.
