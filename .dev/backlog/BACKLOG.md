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
- [ ] Set up multi-release JAR with Java 21 variant
- [ ] Update project description for GitHub (`pom.xml` and repository)
- [ ] Port odd-collections to TypeScript
- [ ] Port odd-collections to Python

## JML / Formal verification

- [ ] Fix 4 OpenJML errors: `strictly_pure` methods may not call `spec_pure`
  methods — `size()` in `KeySetView`, `ValueSetView`, `EntrySetView`
- [ ] Add JML `@invariant` annotations to `UnsynchronizedSymmetricMap`
- [ ] Add JML `@invariant` annotations to `UnsynchronizedChunkyList`
- [ ] Add JML `@requires` / `@ensures` contracts to key mutation methods
  (`put`, `remove`, `removeByValue`, `safePut`, `insertEntry`, `removeEntry`)
- [ ] Complete `INVARIANTS.md` with concrete invariants derived from JML
  annotations
- [ ] Explore JMLUnit for test generation from JML contracts
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
- [ ] Implement `addAll(int index, Collection<? extends E> c)` — optimized
  insertion at arbitrary index using `toArray` + `System.arraycopy` by chunks
- [ ] Implement `removeAll(Collection<?> c)` — optimized single-pass with
  compaction
- [ ] Write tests and benchmarks for new `removeAll` method

---

## Optimization

Potential performance improvements identified through benchmarking.
Each item should be benchmarked before and after to confirm impact.

- [ ] **Chunk index for `get(int)`** — maintain a cumulative offset array to
  enable binary search instead of linear chain traversal. O(log(n/chunkSize))
  instead of O(n/chunkSize). High complexity — requires maintaining the index
  on every structural modification.
- [ ] **`SPLIT_STRATEGY` / `MERGE_STRATEGY`** — new symmetric strategy pair
  where insertion splits the target chunk in two instead of propagating
  overflow. Would reduce `addAtMiddle` cost significantly at the expense of
  increased chunk count.

---

## Done

### Project-wide

- [x] Migrate baseline from Java 9 to Java 11
- [x] Update README requirements section (Java 11 baseline, Java 21 variant)
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
- [x] Integrate OpenJML 21-0.23 in Maven via `exec-maven-plugin` — called
  via WSL, in an optional `openjml` profile (activate with `-P openjml` or
  via IntelliJ Maven panel)
- [x] Fix `{@link Collection}` Javadoc reference in `UnsynchronizedSymmetricMap`
  (replaced with `{@code Collection<V>}`)

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
- [x] Integrate JMH benchmarks — `ChunkyListBenchmark`, `ChunkyListRemoveBenchmark`,
  `ChunkyListAddAllBenchmark` in `src/benchmark/java/` via Maven `benchmark` profile
- [x] Fix O(n²) iteration bug — implement native `ChunkIterator` overriding
  `AbstractList`'s default iterator (which delegated to `get(int)`)
- [x] Implement bidirectional chunk traversal in `findChunk(int)` — traverse
  from nearest end when `index >= size/2`, used by `get`, `set`, `add(int,E)`,
  `remove(int)`
- [x] Optimize `removeFromChunk` — skip `arraycopy` and remove chunk directly
  when `nbElements == 1`
- [x] Declare `chunkSize` as `final`
- [x] Implement optimized `addAll(Collection<? extends E> c)` using `toArray`
  + bulk `System.arraycopy` by chunks
- [x] Optimize constructor `(int chunkSize, Collection<? extends E> c)` to
  delegate to `addAll`
- [x] Add `growingStrategy` and `shrinkingStrategy` `@Param` to all benchmark
  classes
- [x] Run full benchmark suite with all strategy combinations — results
  recorded in `.dev/benchmarks/`
- [x] `SynchronizedChunkyList.getChunkSize()` — remove unnecessary read lock
  (`chunkSize` is `final`, visible without synchronisation)
- [x] Update `CONTEXT.md`, `BACKLOG.md`, `ARCHITECTURE.md`, `README.md` with
  benchmark conclusions and performance profile
- [x] Transition to Claude Code (JetBrains plugin) as primary coding agent