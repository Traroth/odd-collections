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
| Writing JML contracts (step 2 or retrofitting)              | `.dev/skills/jml-design/SKILL.md`                  |
| Generating or reviewing tests from JML specs                | `.dev/skills/jml-test-generation/SKILL.md`         |
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

- `fr.dufrenoy.util.TreeList<E>` — a sorted list backed by a red-black tree,
  containing no duplicate elements. Elements are ordered by a `Comparator`
  or their natural ordering. Position-dependent write operations (`add(int, E)`,
  `set`, `subList`, etc.) are unsupported since insertion order is determined
  by sort order. Null elements are forbidden; duplicates are silently rejected.
  Two implementations are provided:
  - `UnsynchronizedTreeList` — not thread-safe.
  - `SynchronizedTreeList` — thread-safe, protected by a
    `ReentrantReadWriteLock`.

- `fr.dufrenoy.util.MultiMap<K, V>` — a recursive multi-dimensional map
  interface. Each level associates keys of type `K` to values of type `V`,
  where `V` may itself be another `MultiMap`, enabling multi-level key
  hierarchies with heterogeneous key types per dimension. Supports chained
  lookups, `getOrCreate` for nested writes, and `getOpt` for safe reads via
  `Optional`. Does not extend `java.util.Map` (recursive semantics are
  incompatible with the full `Map` contract). Null keys and values are
  forbidden. Two implementations are provided:
  - `UnsynchronizedMultiMap` — not thread-safe, fail-fast iterators.
  - `SynchronizedMultiMap` — thread-safe, snapshot-based iterators.

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