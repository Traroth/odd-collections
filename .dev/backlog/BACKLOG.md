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

---

## SymmetricMap

- [ ] Extract `SymmetricMap<K, V>` as an interface
- [ ] Rename current `SymmetricMap` to `UnsynchronizedSymmetricMap`
- [ ] Implement `SynchronizedSymmetricMap`
- [ ] Implement `setValue()` on `Entry` in a way that maintains bijectivity,
  which will allow removing the manual overrides of `replace`, `replaceAll`,
  `merge`, `compute`, `computeIfPresent` and delegating to `AbstractMap`
- [ ] Change `getKey()` and `removeByValue()` return types to `Optional`
  (per null handling rules in `JAVA_STANDARDS.md`)
- [ ] Create `MockSymmetricMap`
- [ ] Create `SymmetricMapTest`
- [ ] Create `UnsynchronizedSymmetricMapBlackBoxTest` (rename from current
  `SymmetricMapBlackBoxTest`, update class name inside)
- [ ] Create `UnsynchronizedSymmetricMapWhiteBoxTest`
- [ ] Create `SynchronizedSymmetricMapBlackBoxTest`
- [ ] Create `SynchronizedSymmetricMapWhiteBoxTest`

---

## ChunkyList

- [ ] Bring `ChunkyList` interface into compliance with Oracle Java guidelines
  (beginning file comment, `package`/comment order, explicit imports)
- [ ] Create `ChunkyListBlackBoxTest` (rename existing `ChunkyListTest` —
  verify scope and rename accordingly)
- [ ] Create `UnsynchronizedChunkyListBlackBoxTest` (rename from current
  `UnsynchronizedChunkyListTest`, update class name inside)
- [ ] Create `UnsynchronizedChunkyListWhiteBoxTest`
- [ ] Create `SynchronizedChunkyListBlackBoxTest` (rename from current
  `SynchronizedChunkyListTest`, update class name inside)
- [ ] Create `SynchronizedChunkyListWhiteBoxTest`

---

## Done

### ChunkyList
- [x] Initial implementation of `UnsynchronizedChunkyList` and
  `SynchronizedChunkyList`
- [x] Configurable growing and shrinking strategies
- [x] Native `Spliterator` for efficient parallel streams
- [x] `reorganize()` operation to compact sparsely filled chunks
- [x] 50 tests — 0 failures

### SymmetricMap
- [x] Initial implementation with double collision chain
- [x] `put()`, `safePut()`, `getKey()`, `removeByValue()`, `inverse()`
- [x] Live views for `keySet()`, `values()` (as `Set<V>`), `entrySet()`
- [x] `extends AbstractMap` for `equals()`, `hashCode()`, `toString()`
- [x] Override of `replace()`, `replaceAll()`, `merge()`, `compute()`,
  `computeIfPresent()` via `put()` and `remove()`
- [x] Oracle Java guidelines compliance
- [x] Fix bug in `put()` for identical key-value pair

### Project-wide
- [x] Formalize AI workflow: `CONTEXT.md`, `WORKFLOW.md`, `JAVA_STANDARDS.md`,
  `ARCHITECTURE.md`, skills (`static-analysis`, `new-class`, `update-backlog`,
  `update-readme`)