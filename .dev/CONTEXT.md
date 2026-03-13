# Context for odd-collections

This is the entry point for Claude (or any other AI coding agent) working on
this project. At the beginning of each session, read all files listed below
before generating or modifying any code.

## Files to read

- `.dev/standards/JAVA_STANDARDS.md` — coding standards and conventions for
  all Java code in this project
- `.dev/backlog/BACKLOG.md` — pending tasks and known issues for all classes

## Project overview

`odd-collections` is a Java library of unconventional data structures. It is
licensed under the GNU Lesser General Public License v3.

### Current classes

- `fr.dufrenoy.util.SymmetricMap<K, V>` — a bijective map backed by a single
  array of buckets with two independent collision chains per bucket, one indexed
  by key hash and one by value hash. Implements `java.util.Map` via
  `AbstractMap`.
- `fr.dufrenoy.util.ChunkyList<E>` — (description to be added)

### Repository structure

```
src/
  main/java/fr/dufrenoy/util/
  test/java/fr/dufrenoy/util/
.dev/
  CONTEXT.md
  standards/
    JAVA_STANDARDS.md
  backlog/
    BACKLOG.md
```
