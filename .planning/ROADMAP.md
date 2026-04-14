# Roadmap: Milestone v1.0 — Anomaly Roster Expansion

## Milestones

- [ ] **v1.0 Anomaly Roster Expansion** — Implement 5 distinct SCP behaviors (Phases 1-5)

## Phases

### [ ] Phase 1: Architecture Refactor
**Goal:** Prepare the engine for polymorphic SCP behaviors.
- Refactor `RoomManager` to use an `SCPBehavior` strategy interface.
- Implement `BaseBehavior` for the existing "Architect" (SCP-001) logic.
- Parameterize `ContainmentLog` to accept SCP ID and generate unique text.
- **Success Criteria:** SCP-001 works exactly as before but via the new interface; logs for different IDs show different headers.

### [ ] Phase 2: SCP-096 Implementation
**Goal:** Implement the "Shy Guy" with cursor avoidance mechanics.
- Create `ShyGuyBehavior` class.
- Implement "Weeping" vs "Raging" states.
- Logic: Hovering cursor over the entity triggers rage charge.
- **Success Criteria:** Hovering over the sigil in SCP-002 cell causes it to charge and kill the player.

### [ ] Phase 3: SCP-173 Implementation
**Goal:** Implement the "Sculpture" with blink-teleportation.
- Create `SculptureBehavior` class.
- Logic: Entity moves toward player only during black screen frame IF not focused.
- **Success Criteria:** Player must stare at the entity to prevent it from closing the distance after each blink.

### [ ] Phase 4: SCP-049 Implementation
**Goal:** Implement the "Plague Doctor" with patrol/chase AI.
- Create `DoctorBehavior` class.
- Implement vision cone and patrol pathing.
- **Success Criteria:** Entity walks a route; chases the player if they enter the vision cone.

### [ ] Phase 5: SCP-106 Implementation
**Goal:** Implement the "Old Man" with corrosion hazards.
- Create `OldManBehavior` class.
- Logic: Spawns black rot patches that slow and drain sanity.
- **Success Criteria:** Player must navigate around spreading corrosion to reach the chip fragment.

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|---|---|---|---|---|
| 1. Architecture Refactor | v1.0 | 0/1 | ⏳ Pending | — |
| 2. SCP-096 Implementation | v1.0 | 0/1 | ⏳ Pending | — |
| 3. SCP-173 Implementation | v1.0 | 0/1 | ⏳ Pending | — |
| 4. SCP-049 Implementation | v1.0 | 0/1 | ⏳ Pending | — |
| 5. SCP-106 Implementation | v1.0 | 0/1 | ⏳ Pending | — |
