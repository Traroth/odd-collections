# Backlog — odd-collections

Pending tasks and known issues, grouped by class. Update this file at the end
of each session.

---

## Project-wide

- [ ] Copy constructors between `UnsynchronizedChunkyList` and
  `SynchronizedChunkyList` — on hold
- [ ] `SortedChunkyList`
- [ ] `TreeList`
- [ ] `MultiMap`
- [ ] `Tuple` — a type-safe immutable tuple system without one class per arity
- [ ] `CircularBuffer` — a circular doubly-linked list backed by a ring of nodes
- [ ] Migrate baseline from Java 9 to Java 11
- [ ] Set up multi-release JAR with Java 21 variant
- [ ] Update README requirements section (Java 11 baseline, Java 21 variant)
- [ ] Update project description for GitHub (`pom.xml` and repository)
- [ ] Port odd-collections to TypeScript
- [ ] Port odd-collections to Python

## JML / Formal verification

- [ ] Add JML `@invariant` annotations to `UnsynchronizedSymmetricMap`
- [ ] Add JML `@invariant` annotations to `UnsynchronizedChunkyList`
- [ ] Add JML `@requires` / `@ensures` contracts to key mutation methods
  (`put`, `remove`, `removeByValue`, `safePut`, `insertEntry`, `removeEntry`)
- [ ] Complete `INVARIANTS.md` with concrete invariants derived from JML
  annotations
- [ ] Explore OpenJML or JMLUnit for test generation from JML contracts
- [ ] Create `jml-design` skill — write JML contracts during design phase
- [ ] Create `jml-test-generation` skill — generate tests from JML contracts
- [ ] Explore KeY integration for formal program verification

---

## SymmetricMap

- [ ] Change `getKey()` and `removeByValue()` return types to `Optional`
  (per null handling rules in `JAVA_STANDARDS.md`)

---

## ChunkyList

- [ ] Bring `ChunkyList` interface into compliance with Oracle Java guidelines
  (beginning file comment, `package`/comment order, explicit imports)
- [ ] Fix remaining Checkstyle warnings in `UnsynchronizedChunkyList` and
  `SynchronizedChunkyList` (`NeedBraces`, `MissingSwitchDefault`, wildcard
  imports, `EmptyLineSeparator`, unused imports)

---

## Done

### Project-wide

- [x] Formalize AI workflow: `CONTEXT.md`, `WORKFLOW.md`, `JAVA_STANDARDS.md`,
  `ARCHITECTURE.md`, skills (`static-analysis`, `new-class`, `update-backlog`,
  `update-readme`)
- [x] Add `spotbugs-exclude.xml` and configure in `pom.xml`
- [x] Replace `google_checks.xml` with custom `checkstyle.xml` (4-space
  indentation, 120-char line length, `module-info.java` excluded)
- [x] Upgrade `maven-checkstyle-plugin` from `3.3.1` to `3.6.0`
- [x] Add naming conventions section to `JAVA_STANDARDS.md` (exception
  parameter names: `ioe`, `npe`, `iae`...)
- [x] Translate all French comments in
  `SynchronizedSymmetricMapWhiteBoxTest.java` to English
- [x] Remove unused imports from `UnsynchronizedSymmetricMap` (`ArrayList`,
  `List`, `BiFunction`, `Collection`) and `SynchronizedSymmetricMap`
  (`Collection`)

### SymmetricMap

- [x] Extract `SymmetricMap<K, V>` as an interface
- [x] Rename current `SymmetricMap` to `UnsynchronizedSymmetricMap`
- [x] Implement `SynchronizedSymmetricMap`
- [x] Implement `setValue()` on `Entry` to maintain bijectivity, removing
  manual overrides of `replace`, `replaceAll`, `merge`, `compute`,
  `computeIfPresent`
- [x] Refactor `Entry` — remove `map` reference, wrap in
  `EntrySetView.iterator()`
- [x] Fix `equals()` in the anonymous `Entry` wrapper in
  `EntrySetView.iterator()`
- [x] Create `MockSymmetricMap`
- [x] Create `SymmetricMapTest`
- [x] Create `UnsynchronizedSymmetricMapBlackBoxTest`
- [x] Create `UnsynchronizedSymmetricMapWhiteBoxTest`
- [x] Create `SynchronizedSymmetricMapBlackBoxTest`
- [x] Create `SynchronizedSymmetricMapWhiteBoxTest`

### ChunkyList

- [x] Initial implementation of `UnsynchronizedChunkyList` and
  `SynchronizedChunkyList`
- [x] Configurable growing and shrinking strategies
- [x] Native `Spliterator` for efficient parallel streams
- [x] `reorganize()` operation to compact sparsely filled chunks
- [x] 56 tests — 0 failures (restructured into Black Box / White Box)
- [x] Null elements not allowed (compliance with Optional preference)
- [x] Create `UnsynchronizedChunkyListBlackBoxTest`
- [x] Create `UnsynchronizedChunkyListWhiteBoxTest`
- [x] Create `SynchronizedChunkyListBlackBoxTest`
- [x] Create SynchronizedChunkyListWhiteBoxTest
- [x] Add final to countChunks() in UnsynchronizedChunkyList
- [x] Add countChunks() to SynchronizedChunkyList