---
name: update-readme
description: >
  Update README.md in odd-collections after a structural change to the library.
  Trigger this skill when a new class is added, a new public method is introduced,
  or the roadmap changes. Also trigger automatically at the end of every working
  session, alongside update-backlog. Also trigger when the user says "update the
  README", "document this", or "add this to the README". Do not trigger for
  internal refactoring, bug fixes, or test-only changes.
---

# Update README — odd-collections

## When to run

- A new class was added to the library
- A new public method was added to an existing class (if significant enough to
  appear in the README)
- The roadmap changed (item completed, new item added, item rephrased)
- At the end of every working session — run alongside `update-backlog`, even if
  no explicit change was requested (skip only if the session contained no public
  API change and no roadmap change)

Do **not** update the README for:
- Internal refactoring with no public API change
- Bug fixes that don't affect documented behaviour
- Test-only changes

---

## Before writing anything

Read the current `README.md` in full to understand the existing structure and
tone before making any modifications. The README is written in English, in a
clear and concise technical style. Preserve that tone.

---

## README structure

The README has the following top-level sections. Only touch the sections
relevant to the change.

```
# odd-collections
## Structures
  ### ClassName          ← one per class
    #### How it works
    #### Architecture    ← table: Type / Role
    #### Features        ← bullet list
    #### Strategies      ← if applicable (tables + descriptions)
    #### Usage           ← code examples
    #### Thread safety   ← if applicable
## Roadmap
## Requirements
## License
```

---

## Procedure by trigger

### Trigger: new class added

Add a new `### ClassName` section under `## Structures`, after the existing
classes. Include all subsections below.

#### `#### How it works`

- Explain the core data structure and its invariant in plain English
- Include an ASCII diagram if the structure is spatial
  (see `ChunkyList` for style reference)
- One short paragraph on the performance trade-offs vs. standard library
  alternatives

#### `#### Architecture`

A markdown table with columns `Type` and `Role`:

| Type | Role |
|---|---|
| `Interface<E>` | ... |
| `UnsynchronizedImpl<E>` | ... |
| `SynchronizedImpl<E>` | ... |

Omit rows that don't exist for this class.

#### `#### Features`

A bullet list of the main public capabilities. Use the same style as the
existing entries (verb phrase, backtick for type names). Include:
- Full interface implementation inherited
- Configurable behaviours (strategies, comparators, etc.)
- Notable operations specific to this class

#### `#### Strategies` (only if the class has pluggable strategies)

For each strategy enum:
- A short intro sentence
- A markdown table with columns `Strategy` and `Behaviour`
- A paragraph on symmetric pairs if applicable
- A code example showing how to change strategies at runtime

#### `#### Usage`

Always present. Claude generates these examples from scratch based on the
class's public API and Javadoc. Examples must:
- Compile against the actual public API (no invented methods)
- Cover: basic construction, the most common operation, any class-specific
  feature (e.g. `safePut`, `getKey`, `inverse` for `SymmetricMap`)
- Use the style of the existing `ChunkyList` usage block (fenced ```java blocks,
  inline comments)
- Be self-contained (no imports shown, no boilerplate)

#### `#### Thread safety` (only if a synchronized variant exists)

Summarise:
- Which implementation is thread-safe
- The locking strategy used
- Iterator / stream behaviour (fail-fast vs. snapshot)
- Any memory note about snapshot-based operations

---

### Trigger: new public method on an existing class

Locate the existing `### ClassName` section. Update only what has changed:

- Add to `#### Features` if the method is significant and not already implied
- Update `#### Usage` if a new example would be helpful
- Do **not** rewrite the whole section — surgical edits only

---

### Trigger: roadmap changed

Locate `## Roadmap`. Apply one of:

- **Item completed** → remove it from the roadmap entirely (it is now
  documented under `## Structures`)
- **New item added** → append a bullet with the same style as existing entries:
  bold name, em dash, one-sentence description
- **Item rephrased** → edit in place

---

## Style rules

- All text in English
- Backticks for all type names, method names, and code references inline
- Fenced ```java blocks for all code examples
- Tables use the compact `|---|---|` separator style
- No trailing whitespace
- Preserve all existing section order and heading levels exactly

---

## Before presenting the result

Show the user a summary of what was changed:
- Which sections were added
- Which sections were modified
- Which sections were left untouched

Then present the updated content for review. Do not instruct the user to
merge blindly — flag anything uncertain for their decision.