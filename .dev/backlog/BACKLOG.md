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
- [ ] Experiment with JML
- [ ] Update project description for GitHub (`pom.xml` and repository)
- [ ] Complete INVARIANTS.md with concrete invariants for SymmetricMap and
  ChunkyList — articulate with JML annotations

---

## SymmetricMap

- [ ] Change `getKey()` and `removeByValue()` return types to `Optional`
  (per null handling rules in `JAVA_STANDARDS.md`)

---

## ChunkyList

- [ ] Bring `ChunkyList` interface into compliance with Oracle Java guidelines
  (beginning file comment, `package`/comment order, explicit imports)
- [ ] Fix Checkstyle warnings (indentation, wildcard imports, header/package
  order, NeedBraces, variable names)

---

## Done

### Project-wide

- [x] Formalize AI workflow: `CONTEXT.md`, `WORKFLOW.md`, `JAVA_STANDARDS.md`,
  `ARCHITECTURE.md`, skills (`static-analysis`, `new-class`, `update-backlog`,
  `update-readme`)
- [x] Add `spotbugs-exclude.xml` and configure in `pom.xml`

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