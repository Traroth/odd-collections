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

| Task                                                        | Skill                                              |
|-------------------------------------------------------------|----------------------------------------------------|
| Generate or significantly modify a class                    | `.dev/skills/static-analysis/SKILL.md`             |
| Create a new class                                          | `.dev/skills/new-class/SKILL.md`                   |
| Before implementing a new class or data structure           | `.dev/skills/design-review/SKILL.md`               |
| After writing tests or when coverage seems insufficient     | `.dev/skills/test-coverage-review/SKILL.md`        |
| After adding public methods or before a release             | `.dev/skills/api-consistency-check/SKILL.md`       |
| After significant refactoring or before a release           | `.dev/skills/architecture-drift-check/SKILL.md`    |
| End of session                                              | `.dev/skills/update-backlog/SKILL.md`              |
| End of session (if API or roadmap changed)                  | `.dev/skills/update-readme/SKILL.md`               |

## Project overview

`odd-collections` is a Java library of unconventional data structures. It is
licensed under the GNU Lesser General Public License v3.

### Current classes

- `fr.dufrenoy.util.SymmetricMap<K, V>` — a bijective map interface extending
  `java.util.Map`. Declares symmetric operations: `getKey()`, `removeByValue()`,
  `safePut()`, and `inverse()`. Values are unique by definition, so `values()`
  returns `Set<V>`. Two implementations are provided:
  - `UnsynchronizedSymmetricMap` — backed by a single array of buckets with two
    independent collision chains per bucket (one by key hash, one by value hash),
    extending `AbstractMap`. Not thread-safe.
  - `SynchronizedSymmetricMap` — delegates to `UnsynchronizedSymmetricMap`,
    protected by a `ReentrantReadWriteLock`. Iterators are snapshot-based.

- `fr.dufrenoy.util.ChunkyList<E>` — a Java implementation of an unrolled linked
  list, with an unsynchronized and a thread-safe variant backed by a
  `ReentrantReadWriteLock`. Features configurable growing and shrinking
  strategies, a native `Iterator` and `Spliterator` for efficient sequential
  and parallel traversal, an optimized `addAll` using `toArray` and bulk
  `System.arraycopy`, and a `reorganize()` operation to compact sparsely
  filled chunks.

### Repository structure
```
src/
  main/java/fr/dufrenoy/util/
  test/java/fr/dufrenoy/util/
  benchmark/java/fr/dufrenoy/util/
.dev/
  CONTEXT.md
  WORKFLOW.md
  standards/
    JAVA_STANDARDS.md
  backlog/
    BACKLOG.md
  design/
    ARCHITECTURE.md
    INVARIANTS.md
  skills/
    static-analysis/
      SKILL.md
    new-class/
      SKILL.md
    update-backlog/
      SKILL.md
    update-readme/
      SKILL.md
    api-consistency-check/
      SKILL.md
    architecture-drift-check/
      SKILL.md
    design-review/
      SKILL.md
    test-coverage-review/
      SKILL.md
```