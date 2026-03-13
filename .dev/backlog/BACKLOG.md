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

---

## SymmetricMap

- [ ] Implement `setValue()` on `Entry` in a way that maintains bijectivity,
  which will allow removing the manual overrides of `replace`, `replaceAll`,
  `merge`, `compute`, `computeIfPresent` and delegating to `AbstractMap`
- [ ] Create `SymmetricMapBlackBoxTest`
- [ ] Create `SymmetricMapWhiteBoxTest`

---

## ChunkyList

- [ ] Bring into compliance with Oracle Java guidelines (beginning file comment,
  `package`/comment order, explicit imports)
- [ ] Create `ChunkyListBlackBoxTest`
- [ ] Create `ChunkyListWhiteBoxTest`