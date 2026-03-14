---
name: new-class
description: >
  Scaffold a new class in odd-collections: generate the source file, the two
  test classes, update BACKLOG.md and ARCHITECTURE.md, and run a static analysis.
  Use this skill whenever the user asks to create, implement, or scaffold a new
  data structure or class in the library — even if they just say "let's add X"
  or "I want to start working on Y".
---

# New Class — odd-collections

## Before writing any code

Read the following files in full:

1. `.dev/WORKFLOW.md` — the required order of operations; follow it strictly
2. `.dev/standards/JAVA_STANDARDS.md` — coding standards (file header, imports,
   declaration order, Javadoc, null handling, versioning)
3. `.dev/design/ARCHITECTURE.md` — existing design decisions and rationale
4. `.dev/backlog/BACKLOG.md` — pending tasks; check if this class is already listed

---

## Procedure

Follow the order defined in `.dev/WORKFLOW.md`:
1. Update `ARCHITECTURE.md`
2. Update `BACKLOG.md`
3. Write production code (then run `static-analysis`)
4. Write tests (`BlackBoxTest` first, then `WhiteBoxTest`)
5. Update `README.md` (run `update-readme`)

### 1. Clarify the design (before coding)

Ask the user — or infer from context — the following:

- What is the core data structure and its invariant?
- Does it implement an existing Java interface (`List`, `Map`, `Set`, `Queue`…)?
  If so, which `AbstractXxx` base class should it extend?
- Is a thread-safe variant needed alongside an unsynchronized one?
- What are the key operations and their expected complexity?
- Are there configurable strategies or behaviours (like `ChunkyList`)?
- What design alternatives were considered and rejected? (for `ARCHITECTURE.md`)

Do not proceed to code until the design is clear.

### 2. Generate the source file

Location: `src/main/java/fr/dufrenoy/util/ClassName.java`

Required structure (in order):

1. **File header** — C-style comment with class name, version (match `pom.xml`),
   and full LGPL v3 notice
2. **`package` statement**
3. **`import` statements** — explicit, sorted alphabetically, no wildcards
4. **Class Javadoc** — describe the data structure, its invariant, complexity,
   thread-safety, and any non-obvious contracts
5. **Class declaration** — `extends AbstractXxx` if applicable
6. **Body** — in order: static fields, instance fields, constructors, methods
   (grouped by functionality, separated by `// ─── Section ───...` decorators)

Null handling: return `Optional<T>` for methods whose result may be absent,
except where an interface contract forbids it (document with `@return ... or
{@code null}`). Never use `Optional` as parameter type or field type.

### 3. Generate the two test classes

#### `ClassNameBlackBoxTest`

Location: `src/test/java/fr/dufrenoy/util/ClassNameBlackBoxTest.java`

- Tests based solely on the public contract and Javadoc
- No knowledge of internal implementation
- Cover: nominal case, boundary cases (null, empty, single element),
  error cases (expected exceptions)
- Must be writable without reading the source code

#### `ClassNameWhiteBoxTest`

Location: `src/test/java/fr/dufrenoy/util/ClassNameWhiteBoxTest.java`

- Tests targeting technically risky internal areas
- Each test must have a comment explaining *why* it targets that specific point
  (e.g. hash collision, resize boundary, lock ordering)
- Cover: internal structure consistency, algorithmic edge cases,
  concurrent modification if applicable

### 4. Run static analysis

After generating the source file, apply the `static-analysis` skill to the
produced code. Report all findings before proceeding.

### 5. Update ARCHITECTURE.md

Add a new section for the class with:

- The key design decisions made
- Alternatives considered and reasons for rejection
- Any invariants that future contributors must preserve

### 6. Update BACKLOG.md

Add a new `## ClassName` section with:

- `[ ] Create ClassNameBlackBoxTest` (if not yet written)
- `[ ] Create ClassNameWhiteBoxTest` (if not yet written)
- Any known limitations or future tasks discovered during design
- Move completed items to `### Done` under the class name

---

## Checklist before handing off

- [ ] File header present and version matches `pom.xml`
- [ ] All imports explicit and sorted
- [ ] All public methods have Javadoc with `@param`, `@return`, `@throws`
- [ ] `Optional` used correctly (returns only, not params or fields)
- [ ] `AbstractXxx` extended where available
- [ ] Overrides of inherited methods documented if semantics differ
- [ ] Static analysis run and findings reported
- [ ] `ARCHITECTURE.md` updated
- [ ] `BACKLOG.md` updated