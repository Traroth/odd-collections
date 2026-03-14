# Context for odd-collections

This is the entry point for Claude (or any other AI coding agent) working on
this project. At the beginning of each session, read all files listed below
before generating or modifying any code.

## Files to read

- `.dev/standards/JAVA_STANDARDS.md` — coding standards and conventions for
  all Java code in this project
- `.dev/backlog/BACKLOG.md` — pending tasks and known issues for all classes
- `.dev/design/ARCHITECTURE.md` — key design decisions and rationale for each class
- `.dev/WORKFLOW.md` — required order of operations for any class creation or
  modification

## Skills

Skills encode recurring workflows. Read the relevant skill file before starting
the corresponding task.

| Task                                        | Skill                                      |
|---------------------------------------------|--------------------------------------------|
| Generate or significantly modify a class    | `.dev/skills/static-analysis/SKILL.md`     |
| Create a new class                          | `.dev/skills/new-class/SKILL.md`           |
| End of session                              | `.dev/skills/update-backlog/SKILL.md`      |
| End of session (if API or roadmap changed)  | `.dev/skills/update-readme/SKILL.md`       |

## Project overview

`odd-collections` is a Java library of unconventional data structures. It is
licensed under the GNU Lesser General Public License v3.

### Current classes

- `fr.dufrenoy.util.SymmetricMap<K, V>` — a bijective map backed by a single
  array of buckets with two independent collision chains per bucket, one indexed
  by key hash and one by value hash. Implements `java.util.Map` via
  `AbstractMap`.
- `fr.dufrenoy.util.ChunkyList<E>` — a Java implementation of an unrolled linked
  list, with an unsynchronized and a thread-safe variant backed by a
  `ReentrantReadWriteLock`. Features configurable growing and shrinking
  strategies, a native `Spliterator` for efficient parallel streams, and a
  `reorganize()` operation to compact sparsely filled chunks.

### Repository structure

```
src/
  main/java/fr/dufrenoy/util/
  test/java/fr/dufrenoy/util/
.dev/
  CONTEXT.md
  WORKFLOW.md
  standards/
    JAVA_STANDARDS.md
  backlog/
    BACKLOG.md
  design/
    ARCHITECTURE.md
  skills/
    static-analysis/
      SKILL.md
    new-class/
      SKILL.md
    update-backlog/
      SKILL.md
    update-readme/
      SKILL.md
```