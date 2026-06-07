# ChangeCut Manual Smoke Checklist

Updated: 2026-06-07

## Project Lifecycle

- [ ] Launch app and pass login/splash flow.
- [ ] Create a 9:16 project from New Project.
- [ ] Return to Home and confirm the project appears in Recent Projects.
- [ ] Rename the project and confirm the new name persists after app restart.
- [ ] Duplicate the project and confirm the copied project opens.
- [ ] Delete a project and confirm it disappears from Home after app restart.
- [ ] Import a video and confirm the project thumbnail appears in Recent Projects.

## Editor Persistence

- [ ] Open a project and import one video.
- [ ] Add text, sticker, or a simple effect.
- [ ] Leave editor and reopen the same project.
- [ ] Confirm timeline clips, overlays, zoom, and snap state restore correctly.

## Export

- [ ] Export a simple timeline to debug output.
- [ ] Confirm export progress reaches completion.
- [ ] Confirm exported file can be opened/shared from device storage.

## Regression

- [ ] Create a second project and confirm timelines do not leak between project IDs.
- [ ] Duplicate a project with timeline content and confirm duplicate has independent timeline files.
- [ ] Delete a duplicated project and confirm original project still opens.
