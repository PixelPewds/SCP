# Requirements: Milestone v1.0

## v1.0 Anomaly Roster Expansion

### Core Architecture
- [ ] **ARCH-01**: Refactor `GamePanel` and `RoomManager` to support an `SCPBehavior` interface.
- [ ] **ARCH-02**: Parameterize `ContainmentLog` to display SCP-specific lore, classification, and containment notes.

### SCP-096 "The Shy Guy" (SCP-002)
- [ ] **ANOM-02-01**: Implement avoidance mechanic — hovering the cursor over SCP-096 triggers a "Rage" state.
- [ ] **ANOM-02-02**: In "Rage" state, SCP-096 charges the player, leading to immediate death if contact is made.
- [ ] **ANOM-02-03**: Create distinct visual cues for SCP-096's weeping and screaming phases.

### SCP-173 "The Sculpture" (SCP-003)
- [ ] **ANOM-03-01**: Implement blink-teleportation logic — moves toward player only when screen is black AND player is not "staring" at it.
- [ ] **ANOM-03-02**: Implement "Neck Snap" death sequence if SCP-173 reaches the player's position.
- [ ] **ANOM-03-03**: Add scraping sound effect logs/visual indicators for movement.

### SCP-049 "The Plague Doctor" (SCP-004)
- [ ] **ANOM-04-01**: Implement patrol AI that moves between points in the containment cell.
- [ ] **ANOM-04-02**: Implement a vision cone; if player enters, SCP-049 initiates a chase.
- [ ] **ANOM-04-03**: Implement death on contact ("The Cure").

### SCP-106 "The Old Man" (SCP-005)
- [ ] **ANOM-05-01**: Implement periodic corrosion generation — black "rot" patches appear on the floor.
- [ ] **ANOM-05-02**: Staying in corrosion patches drains sanity rapidly and slows player movement.
- [ ] **ANOM-05-03**: Periodic "Pocket Dimension" glitch phase (visual/HUD intensity).

## Future Requirements
- SCP-006 Anomaly Implementation.
- Global progression/Save system.
- Sound design overhaul.

## Out of Scope
- Multiplayer support.
- External game engine ports.

## Traceability
*(To be filled by ROADMAP.md)*
