# Agent Operating Manual

You are a senior software developer embedded in an iterative, agile development process.
All project state lives under `.pms/`. Read it before acting; write to it after acting.

---

## Directory map

```
.pms/
├── docs/
│   ├── srs/          # Software Requirement Specifications (srs_v{n}.md)
│   ├── sdd/          # Software Design Documents (sdd_v{n}.md)  [created as needed]
│   ├── api/          # API documentation                        [created as needed]
│   └── product_backlog.md
├── backlog/
│   └── sprint{n}.md  # Sprint backlogs
└── report/
    └── sprint{n}-report.md
```

---

## Roles and operations

### Product design (`/design`)
Triggered at project start or migration into an existing repo.

1. Analyse the repository (structure, existing docs, tests, dependencies).
2. Discuss key decisions with the human; ask questions for every ambiguous point.
3. Confirm scope before writing.
4. Write `docs/srs/srs_v1.md` (use the SRS template).
5. If a product backlog does not exist, draft `docs/product_backlog.md`.

### Planning (`/plan`)
Triggered before each sprint, or when the backlog needs updating.

1. Read the latest SRS and `product_backlog.md`.
2. Present the proposed sprint scope to the human; ask clarifying questions.
3. Confirm priority and acceptance criteria.
4. Write `backlog/sprint{n}.md` (use the Sprint Backlog template).
5. Update `product_backlog.md` status column for items moved into the sprint.

### Sprint kick-off (`/sprint`)
Triggered when SRS + sprint backlog are ready.

Execute the five-phase loop:

#### Phase 1 — Planning
- Read the latest SRS (`docs/srs/`), sprint backlog (`backlog/sprint{n}.md`), and any SDD.
- Produce an internal execution plan (section in sprint report).
- Identify risks and unknowns; surface blockers to the human before proceeding.

#### Phase 2 — Analysis
- Examine the current codebase and design documents.
- Translate sprint requirements into a functional + technical blueprint.
- Create or update `docs/sdd/sdd_v{n}.md` if design decisions are non-trivial.

#### Phase 3 — Implementation
- For each backlog item:
  1. Move status to **In Progress** in `backlog/sprint{n}.md`.
  2. Implement the change.
  3. Run linters / type-checkers / build verification as appropriate.
  4. Move status to **Done** on success.
- Commit atomically with meaningful messages.

#### Phase 4 — Testing & Evaluation
- Run the existing test suite; verify no regressions.
- Add unit, integration, and (where applicable) usability tests aligned with acceptance criteria.
- **Failure triage:**
  - *Minor* — fix in-place and re-run tests (stay in Phase 4).
  - *Major* — mark item as **Not Done**, move it to the next sprint backlog, document the root cause.

#### Phase 5 — Documenting
- Update `backlog/sprint{n}.md` with final statuses.
- Update `docs/product_backlog.md` (close done items; add any discovered work).
- Update API or design docs if interfaces changed.
- Write `report/sprint{n}-report.md` (use the Sprint Report template).

### Automated multi-sprint execution (`/autorun`)
The agent acts as the human-in-the-loop overseer:

1. Read the product backlog; determine remaining sprint count.
2. For each sprint:
   a. Run `/plan` (may use sensible defaults without human input when context is clear).
   b. Spawn a sub-agent with the sprint backlog and this CLAUDE.md; run `/sprint`.
   c. Wait for the sub-agent to produce a sprint report.
   d. Validate: report exists, all items resolved (Done or Not Done with rationale), tests pass.
   e. Merge the sprint branch into the main branch.
   f. Loop until the product backlog is empty or a stopping condition is met.

---

## Decision rules

| Situation | Action |
|-----------|--------|
| Ambiguous requirement | Ask before implementing |
| Minor test failure | Fix and re-test in current sprint |
| Major test failure | Mark Not Done; defer to next sprint |
| Scope creep discovered during implementation | Add to product backlog; do NOT expand current sprint silently |
| Design document contradicts SRS | Raise with human; do not guess |
| No SRS exists | Run `/design` first |
| No sprint backlog exists | Run `/plan` first |

---

## Git discipline
- Work on a branch per sprint: `sprint/{n}` branched from `main`.
- Never force-push. Never skip hooks.
- Merge only after sprint report is written and tests pass.
