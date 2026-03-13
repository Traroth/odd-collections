## Working on this project with Claude

This project is developed with Claude as a coding assistant. The `.dev/`
directory contains everything Claude needs to work consistently on the
codebase — and everything a human contributor needs to understand the
project's conventions and history.

### Project-specific choices

A few decisions in this setup are deliberate choices for this project, not
universal standards or conventions:

- **`Optional<T>` as return type** — used more broadly than the Java
  community consensus, which often restricts it to stream operations.
  Here it is the default for any method whose result may be absent,
  except where an interface contract forbids it.
- **Two test classes per tested class** (`BlackBoxTest` / `WhiteBoxTest`) —
  a stricter separation than most projects use, chosen to keep contract
  tests independent from implementation knowledge.
- **`inverse()` returns a copy, not a live view** — unlike Guava's `BiMap`,
  by design. See `.dev/design/ARCHITECTURE.md` for the rationale.
- **Skills as Markdown files in `.dev/skills/`** — Claude reads these files
  on demand to follow consistent workflows. This is not a standard Claude
  convention; it is a lightweight adaptation of the Claude Code skill system
  to work within a Claude.ai Project.

### The `.dev/` directory

| File | Purpose |
|------|---------|
| `.dev/CONTEXT.md` | Entry point — read this first at the start of every session |
| `.dev/standards/JAVA_STANDARDS.md` | Coding conventions for all Java in this project |
| `.dev/backlog/BACKLOG.md` | Pending tasks and known issues, updated each session |
| `.dev/design/ARCHITECTURE.md` | Key design decisions and rationale for each class |

### Skills

Skills are Markdown files that encode recurring workflows. Claude reads the
relevant skill before starting the corresponding task.

| Skill | File | When it runs |
|-------|------|-------------|
| Static analysis | `.dev/skills/static-analysis/SKILL.md` | After every class generation or significant modification |
| New class | `.dev/skills/new-class/SKILL.md` | When scaffolding a new data structure |
| Update backlog | `.dev/skills/update-backlog/SKILL.md` | At the end of every session |
| Update README | `.dev/skills/update-readme/SKILL.md` | When the public API or roadmap changes, and at end of session |

### Starting a session

At the beginning of each working session, ask Claude to read
`.dev/CONTEXT.md`. This gives Claude the full picture: coding standards,
pending tasks, and design decisions. The skills are then available for the
rest of the session.

### Triggering a skill explicitly

Skills trigger automatically when Claude recognises the context (e.g. end of
session, new class being created). You can also trigger any skill explicitly:

```
Run the static-analysis skill on SymmetricMap.
```

```
We're done for today — run update-backlog and update-readme.
```