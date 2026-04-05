# Backlog — odd-collections

Pending tasks and known issues, grouped by class. Update this file at the end
of each session.

---

## Project-wide

- [ ] Copy constructors between `UnsynchronizedChunkyList` and
  `SynchronizedChunkyList` — on hold
- [ ] `SortedChunkyList`
- [ ] `Tuple` — a type-safe immutable tuple system without one class per arity
- [ ] `CircularBuffer` — a circular doubly-linked list backed by a ring of nodes
- [ ] Implement `Serializable` on all data structures
- [ ] Set up multi-release JAR with Java 21 variant — chosen approach:
  full duplication of interfaces and classes in the Java 21 layer
  (no base class extraction, no cross-layer inheritance).
  Java 21 classes are independent copies, free to fully leverage Java 21
  APIs (`SequencedCollection`, etc.).
  Accepted trade-off: bug fixes and evolutions must be applied in both
  layers. To be done once all classes are complete.
- [ ] Update project description for GitHub (`pom.xml` and repository)
- [ ] Port odd-collections to TypeScript
- [ ] Port odd-collections to Python

## TreeList

- [ ] Implement `subList(int, int)` — live view over a tree-backed structure

---

## JML / Formal verification

- [ ] Fix 2 remaining OpenJML warnings: `Optional.isEmpty()` not recognized
  as `pure` by OpenJML 21-0.23 (limitation: no JDK spec for `isEmpty()`,
  added in Java 11)
- [ ] OpenJML 21-0.23 crashes with internal error (exit code 4) due to
  `module-info.java` — upstream bug (issue filed), exit code 4 accepted
  in Maven profiles as workaround
- [ ] Add OpenJML RAC profile — blocked by the module-info.java crash
  (RAC produces no output, unlike ESC which completes analysis before
  crashing). Revisit when upstream fix is available

---

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

### JML / Formal verification

- [x] Fix OpenJML `strictly_pure` / `spec_pure` error — annotated
  `UnsynchronizedSymmetricMap.size()` as `strictly_pure`
- [x] Move JML `/*@ invariant @*/` blocks inside class bodies (all 6 classes)
  — OpenJML 21 does not support invariant blocks between Javadoc and class
  declaration
- [x] Complete `INVARIANTS.md` with concrete invariants derived from JML
  annotations (SymmetricMap, ChunkyList, TreeList)
- [x] Add `also` keyword to all JML specs on overriding methods in
  `UnsynchronizedSymmetricMap`
- [x] Annotate `getKey()` as `pure` in `SymmetricMap` interface and
  `UnsynchronizedSymmetricMap`
- [x] Make OpenJML profile portable: `openjml-unix` (Linux, macOS) and
  `openjml-windows` (Windows via WSL), configured via `openjml.home` in
  `~/.m2/settings.xml`
- [x] Add JML `@invariant` annotations and `@requires` / `@ensures` contracts
  to `UnsynchronizedChunkyList` and `SynchronizedChunkyList`
- [x] Add JML `@invariant` annotations and `@requires` / `@ensures` contracts
  to `UnsynchronizedSymmetricMap` and `SynchronizedSymmetricMap`
- [x] Add JML `@requires` / `@ensures` contracts to key mutation methods
  (`put`, `remove`, `removeByValue`, `safePut`)
- [x] Create `jml-design` skill — write JML contracts during design phase
- [x] Create `jml-test-generation` skill — generate tests from JML contracts
  (Claude-driven, no JMLUnit dependency — JMLUnit is dormant since ~2014)
- [x] Extend OpenJML ESC profiles to all 3 classes (`UnsynchronizedSymmetricMap`,
  `UnsynchronizedChunkyList`, `UnsynchronizedTreeList`)
- [x] Fix JML annotation placement in `UnsynchronizedTreeList` — specs must
  precede Java annotations (`@Override`), not follow them
- [x] Add `also` keyword to all overriding methods in `UnsynchronizedChunkyList`
  and `UnsynchronizedTreeList`
- [x] Add `pure` annotations to getters in `ChunkyList` and `TreeList`
  interfaces, and `UnsynchronizedChunkyList` implementation
- [x] Fix erasure clash in `UnsynchronizedTreeList` — renamed private
  `compare(Object, Object)` to `compareElements` to avoid conflict with
  JML model method `compare(E, E)`
- [x] Accept OpenJML exit code 4 in Maven profiles (upstream module-info bug)
- [x] Add JML annotation placement rules to `JAVA_STANDARDS.md` (section 9)

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

### TreeList

- [x] Implement `UnsynchronizedTreeList` — red-black tree augmented with subtree sizes
- [x] Implement `SynchronizedTreeList` — delegates to `UnsynchronizedTreeList` with `ReentrantReadWriteLock`
- [x] Create `MockTreeList`
- [x] Create `TreeListTest` (interface contract)
- [x] Create `UnsynchronizedTreeListBlackBoxTest`
- [x] Create `UnsynchronizedTreeListWhiteBoxTest`
- [x] Create `SynchronizedTreeListBlackBoxTest`
- [x] Create `SynchronizedTreeListWhiteBoxTest`

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
- [x] `TreeList`
- [x] `MultiMap` — recursive multi-dimensional map with `UnsynchronizedMultiMap`,
  `SynchronizedMultiMap`, interface contract tests, black-box tests, white-box
  tests, JML contracts, ARCHITECTURE.md, INVARIANTS.md, README.md

### SymmetricMap

- [x] Change `getKey()` and `removeByValue()` return types to `Optional`
  (per null handling rules in `JAVA_STANDARDS.md`)

---

## Rejected

Items that were explored and deliberately discarded.

- **JMLUnit for test generation** — JMLUnit and JMLUnit-NG are dormant
  since ~2014, do not support Java 8+. No actively maintained alternative
  exists. Replaced by the `jml-test-generation` skill (Claude-driven test
  derivation from JML specs, optionally complemented by OpenJML RAC).
- **JJBMC for bounded model checking** — JJBMC bundles an old OpenJML and
  requires Java 8 exclusively. Not compatible with our Java 11 baseline.
  Academic single-author project with sporadic maintenance (~5 commits/year).
  Revisit if Java 11+ support is added.
- **KeY for interactive theorem proving** — powerful but fundamentally
  interactive (no CI integration). Limited Java subset (no lambdas).
  Steep learning curve for limited project scope. Revisit if a specific
  invariant proves impossible to verify with OpenJML ESC.