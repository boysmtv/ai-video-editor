# ChangeCut Progress Checklist

Updated: 2026-06-07

## Product Target

- [ ] Build ChangeCut to 100% CapCut Pro-class feature coverage, then exceed it with stronger on-device AI and advanced local editing workflows.
- [ ] Keep all features unlocked after login: no subscription, no payment, no credits, no cloud sync, no watermark, no external API dependency.
- [ ] Prioritize production-grade local project persistence, editor reliability, export correctness, performance, and manual smoke verification before adding more feature breadth.
- [ ] During implementation batches, run Gradle verification at the end after the target code is complete, not after every small edit, unless a risky compile blocker must be isolated early.

## Completed

- [x] Home project list reads persisted local projects through `GetProjectsUseCase`.
- [x] New Project creates real Room-backed `Project` records through `CreateProjectUseCase`.
- [x] New Project no longer navigates with temporary `temp_*` project IDs.
- [x] Confirmed editor autosave/restore is wired through persisted project ID and `ProjectTimelineStore`.
- [x] Added rename action to Recent Projects.
- [x] Added duplicate action to Recent Projects, including local project directory copy.
- [x] Added delete action to Recent Projects, including local project directory cleanup.
- [x] Added thumbnail metadata/display to Recent Projects.
- [x] Generate project thumbnail from first imported video when no cover exists.
- [x] Forgot Password screen is implemented and wired into auth navigation.
- [x] Undo/redo history dropdown is implemented in the editor top bar.
- [x] App-level Settings route is reachable from Home.
- [x] Runtime dark theme toggle is wired through MainActivity/AppNavGraph/Settings.
- [x] Added manual smoke checklist for create/open/edit/reopen/export.
- [x] Fixed `TrackManager` snap compile issue by using valid `kotlin.math.abs` calls.
- [x] Fixed AI caption "To Timeline" callback wiring.
- [x] Verified `:feature-home:compileDebugKotlin`.
- [x] Verified full `assembleDebug`.

## Next

- [ ] Verify on device/emulator: login -> create project -> return Home -> project appears in Recent Projects.
- [ ] Verify editor autosave/restore uses the persisted project ID for timeline state.
- [ ] Execute manual smoke checklist on device/emulator.
- [ ] Continue Phase 20 Polish & UX after local project lifecycle is stable.
