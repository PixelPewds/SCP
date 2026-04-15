# PRD — The Architect's Breach: Anomaly Roster Expansion (v1.0)

> **For:** Ralph Loop (execution agent)
> **Source:** `PROJECT.md` + `REQUIREMENTS.md`
> **Milestone:** v1.0 — Anomaly Roster Expansion

---

## 1. What We're Building

A lo-fi survival horror game in **pure Java (Swing/AWT)**, no external dependencies.
The player navigates a collapsing SCP containment facility via a hallway hub, enters containment cells, and collects chip fragments to escape.

**Core loop:** Hallway hub → enter cell → collect chip fragment → return → repeat ×5 → escape via Master Console.

Horror is **mechanic-driven**: each SCP anomaly has a unique, uncomfortable rule the player must learn under pressure.

---

## 2. Current State (Pre-v1.0)

All engine infrastructure is complete and compiled. One anomaly is live:

| System | Status |
|---|---|
| Game loop, state machine (TITLE→HALLWAY→PLAYING→DEAD→ESCAPED) | ✅ |
| Mouse-relative WASD movement | ✅ |
| SCP-001 "The Architect" — room shifts on blink if focus lost | ✅ |
| Blink mechanic (4 s cycle, AnchorPoint focus system) | ✅ |
| HallwayManager — 6 doors, camera follow, access key system | ✅ |
| Fragment Synthesis System — 5 chip fragments, one per cell | ✅ |
| Master Console — chip verify, escape cinematic | ✅ |
| SanitySystem — drain, screen shake, glitch distortion | ✅ |
| ContainmentLog — diegetic desk/terminal text (parameterized by cell) | ✅ |
| Flashlight cone | ✅ |
| DeathScreen — retry / main menu | ✅ |
| SCP-096 "The Shy Guy" — cursor avoidance/rage charge (Phase 2) | ✅ |
| `blinkEnabled` flag on RoomManager (per-cell suppression) | ✅ |

---

## 3. File Map (`d:\SCP\src\`)

```
Main.java              — Entry point, JFrame setup
GamePanel.java         — Core render surface, game loop, state machine
Player.java            — Mouse-relative WASD movement, angle tracking
HallwayManager.java    — Scrolling corridor, 6 doors, Master Console, escape cinematic
RoomManager.java       — Cell engine: 3 layout variants, blink cycle; setBlinkEnabled()
AnchorPoint.java       — Wandering eye sigil, mouse hover = focus detection
ChipFragment.java      — Collectible per cell
InventorySystem.java   — 5 chip slots, HUD, carry/save/drop
InteractionSystem.java — Desk proximity, ContainmentLog lifecycle; setCell(label)
ContainmentLog.java    — Draggable log overlay; dispatches body by cell label
SanitySystem.java      — Sanity meter, screen shake, glitch overlay
Flashlight.java        — Dynamic cone lighting
DeathScreen.java       — Death cause display, retry/menu buttons
RoomObject.java        — Furniture: DOOR, DESK, TERMINAL
ShyGuyBehavior.java    — SCP-096 entity (WEEPING/SCREAMING/CHARGING state machine)
```

---

## 4. Architecture Patterns to Follow

- **No external libraries** — pure Swing/AWT only.
- **One SCP per cell** — door label maps 1:1 to SCP encounter.

### Adding a new SCP (established pattern from Phase 2)

When adding a new SCP for cell `SCP-00X`:

1. **Create `[SCP]Behavior.java`** — self-contained entity with `update(dt, mouseX, mouseY, playerX, playerY)`, `draw(g2)`, and a kill-detection method.
2. **`GamePanel.java`** — add a field, update/draw branch guarded by `currentCellLabel.equals("SCP-00X")`, reset in `resetCell()`, instantiate on door entry alongside `roomManager.setBlinkEnabled(...)` and `interaction.setCell(...)`.
3. **`ContainmentLog.java`** — add a `drawBody0XX()` method and register it in `drawBodyContent()`.
4. No changes needed to `RoomManager`, `HallwayManager`, or `InteractionSystem` beyond what's already in place.

### Blink mechanic per cell

```java
roomManager.setBlinkEnabled(false);  // suppress for cells with their own SCP
roomManager.setBlinkEnabled(true);   // keep for SCP-001 (The Architect)
```

---

## 5. Active Requirements (v1.0 Backlog)

### ARCH — Architecture

| ID | Requirement | Status |
|---|---|---|
| ARCH-01 | Refactor `GamePanel` / `RoomManager` to support a formal `SCPBehavior` interface | ⏳ Pending (Phase 1 — deferred, currently using direct field pattern) |
| ARCH-02 | `ContainmentLog` parameterized to display SCP-specific lore | ✅ Done (Phase 2) |

> `ARCH-01` is deferred. The current direct-field pattern (`ShyGuyBehavior`, etc.) is acceptable for v1.0. An interface refactor can follow as Phase 1 post-v1.0.

---

### SCP-002 — "The Shy Guy" (SCP-096)

| ID | Requirement | Status |
|---|---|---|
| ANOM-02-01 | Cursor hover over SCP-096 triggers Rage state | ✅ Done |
| ANOM-02-02 | Rage state: charges player, lethal on contact | ✅ Done |
| ANOM-02-03 | Distinct visuals for WEEPING and SCREAMING phases | ✅ Done |

---

### SCP-003 — "The Sculpture" (SCP-173)

| ID | Requirement | Notes |
|---|---|---|
| ANOM-03-01 | Blink-teleportation: moves toward player only when screen fully black **and** player not staring | Create `SculptureBehavior.java` |
| ANOM-03-02 | "Neck Snap" death on contact | `triggerDeath("SCP-173 — NECK SNAPPED")` |
| ANOM-03-03 | Scraping sound cue / visual indicator on each move | Flash message or red HUD pulse |

**Mechanic detail:** The existing `RoomManager` blink cycle is the hook. During the `CLOSED` black-screen frame, if the player is **not** staring at the `AnchorPoint`, `SculptureBehavior` should close distance. Keep blink **enabled** for SCP-003 — that's the entire threat. The AnchorPoint already provides `isFocusedBy(mouseX, mouseY)`.

---

### SCP-004 — "The Plague Doctor" (SCP-049)

| ID | Requirement | Notes |
|---|---|---|
| ANOM-04-01 | Patrol AI: moves between defined waypoints in cell | Create `DoctorBehavior.java` |
| ANOM-04-02 | Vision cone: player entering cone triggers chase | Cone defined by angle + range |
| ANOM-04-03 | Death on contact ("The Cure") | `triggerDeath("SCP-049 — THE CURE")` |

**Mechanic detail:** Suppress blink for SCP-004. Entity self-navigates a fixed patrol route (3–4 waypoints). Vision cone width ≈ 60°, range ≈ 200 px. Chase speed > patrol speed.

---

### SCP-005 — "The Old Man" (SCP-106)

| ID | Requirement | Notes |
|---|---|---|
| ANOM-05-01 | Periodic corrosion: black rot patches spawn on floor | Create `OldManBehavior.java` |
| ANOM-05-02 | Standing on corrosion drains sanity rapidly, slows player | Hook into `SanitySystem.drain(amount)` |
| ANOM-05-03 | Pocket Dimension glitch phase: periodic full-screen HUD distortion | Reuse `SanitySystem.drawDistortion()` or add a dedicated pulse |

**Mechanic detail:** Suppress blink for SCP-106. Old Man slowly drifts toward player (slow base speed). Periodically spawns 3–5 rot patches (radius ≈ 30 px) at random floor positions. Patches persist until cell reset.

---

## 6. Key Constraints

| Constraint | Detail |
|---|---|
| No save system | Prototype scope — out of scope for v1.0 |
| No audio | Planned for a future milestone |
| No multiplayer | N/A forever |
| Window | 800×600, 60 fps |
| Entry point | `Main.java` → `GamePanel(800, 600)` |
| Compile command | `javac -encoding UTF-8 *.java` from `d:\SCP\src\` |
| Run command | `java -cp d:\SCP\src Main` |
| Door mapping | SCP-001→door 0, SCP-002→door 1, SCP-003→door 2, SCP-004→door 3, SCP-005→door 4, SCP-006→door 5 (locked) |

---

## 7. Remaining Phases

| Phase | Goal | Key New File |
|---|---|---|
| Phase 1 | Architecture refactor (`SCPBehavior` interface) | Deferred post-v1.0 |
| Phase 2 | SCP-096 "The Shy Guy" | `ShyGuyBehavior.java` ✅ |
| Phase 3 | SCP-173 "The Sculpture" | `SculptureBehavior.java` |
| Phase 4 | SCP-049 "The Plague Doctor" | `DoctorBehavior.java` |
| Phase 5 | SCP-106 "The Old Man" | `OldManBehavior.java` |

---

*Generated 2026-04-15 from `PROJECT.md` + `REQUIREMENTS.md` — Milestone v1.0*
