# The Architect's Breach

A lo-fi survival horror experience in Java inspired by Endoparasitic and the SCP Foundation.

## What This Is

A top-down 2D horror game (Java Swing/AWT) where the player navigates a collapsing SCP containment facility. Core horror comes from **mechanics that feel wrong** — the room shifts when you blink, reality is unstable, the player must maintain visual contact with an anchor to prevent spatial distortion.

**Core loop:** Hallway hub → enter containment cell → collect chip fragment → return → repeat × 5 → escape via Master Console.

## Current State

- **Version:** v0.0 (pre-milestone, active development)
- **Stack:** Java, Swing/AWT, custom 2D engine, no external dependencies
- **Entry:** `Main.java` → `GamePanel` (800×600 window, 60fps game loop)
- **Status:** Core engine complete. One anomaly implemented (SCP-001 "The Architect" — room-shift on blink). Ready to expand anomaly roster.

## Architecture

```
Main.java           — Entry point, JFrame setup
GamePanel.java      — Core render surface, game loop, state machine
  States: TITLE → HALLWAY → PLAYING → DEAD → ESCAPED
Player.java         — Mouse-relative WASD movement, angle tracking
HallwayManager.java — Scrolling corridor hub, 6 doors (SCP-001 to 006),
                      Master Console, escape cinematic sequence
RoomManager.java    — Containment cell engine: 3 layout variants,
                      Blink mechanic (4s cycle), AnchorPoint focus system
AnchorPoint.java    — Wandering eye sigil, mouse hover = focus detection
ChipFragment.java   — Collectible item per cell, pickup detection
InventorySystem.java— Tracks 5 chip slots, counter HUD, carry/save/drop
InteractionSystem.java — Desk/terminal interaction, ContainmentLog display
ContainmentLog.java — In-world text logs shown at desk/terminal
SanitySystem.java   — Sanity meter, screen shake, glitch overlay
Flashlight.java     — Dynamic cone lighting overlay
DeathScreen.java    — Death cause display, retry/menu buttons
RoomObject.java     — Furniture types: DOOR, DESK, TERMINAL
```

## Requirements

### Validated (Pre-Milestone)

- ✓ Core game loop and state machine (TITLE → HALLWAY → PLAYING → DEAD → ESCAPED)
- ✓ Mouse-relative movement with deadzone
- ✓ SCP-001 "The Architect" — room layout shifts on blink if focus lost
- ✓ Blink mechanic (4s cycle, AnchorPoint focus system)
- ✓ HallwayManager — 6 doors (SCP-001 to 006), camera follow, access key system
- ✓ Fragment Synthesis System — 5 chip fragments, one per cell
- ✓ Master Console — chip verification, multi-phase escape cinematic
- ✓ SanitySystem — drain on shift, screen shake, glitch distortion
- ✓ ContainmentLog — diegetic desk/terminal text
- ✓ Flashlight cone lighting
- ✓ DeathScreen — retry / main menu

### Active

(Defined in REQUIREMENTS.md for v1.0)

### Out of Scope (pre-v1.0)

- Save system — not needed for prototype scope
- Sound / audio — planned for a future milestone
- Multiplayer — N/A

## Key Decisions

- **No external libs** — pure Swing/AWT keeps the build zero-dependency
- **One SCP per cell** — each door maps 1:1 to one SCP encounter
- **Sanity ≠ health** — sanity is a pressure mechanic, not a direct kill; psychological collapse at 0 triggers death
- **All 6 doors accessible** — gate via `doorLocked` array; SCP-006 currently locked, SCP-001–005 open

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---

*Last updated: 2026-04-15 — GSD bootstrapped, v1.0 milestone started*
