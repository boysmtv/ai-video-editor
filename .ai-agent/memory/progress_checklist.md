# ChangeCut Progress Checklist

Updated: 2026-06-07

## Product Target

- [ ] Build ChangeCut to 100% CapCut Pro-class feature coverage, then exceed it with stronger on-device AI and advanced local editing workflows.
- [ ] Keep all features unlocked after login: no subscription, no payment, no credits, no cloud sync, no watermark, no external API dependency.
- [ ] Prioritize production-grade local project persistence, editor reliability, export correctness, performance, and manual smoke verification before adding more feature breadth.
- [ ] During implementation batches, run Gradle verification at the end after the target code is complete, not after every small edit, unless a risky compile blocker must be isolated early.

## Done (as of 2026-06-08)

### Phase 11: Masking & Blending
- [x] Masking: Linear, Radial, Rectangle, Heart, Star, Custom Path
- [x] Blending Modes: Normal, Screen, Multiply, Overlay, Add, Darken, Lighten, Difference, Hard Light, Soft Light
- [x] Mask + Blending combined in TrackManager
- [x] Mask animation keyframes (added MASK_CENTER_X/Y/WIDTH/HEIGHT/FEATHER/ROTATION to KeyframeProperty)

### Phase 12: Stickers & Overlays
- [x] Sticker catalog (emoji, shapes, arrows, callouts)
- [x] Image overlay support (PNG with transparency)
- [x] Sticker animation presets (bounce, fade, slide, scale)
- [x] Sticker as separate track type (STICKER)

### Phase 13: Animation System
- [x] In animations: Fade In, Slide In, Zoom In, Bounce In, Rotate In, Blur In
- [x] Out animations: Fade Out, Slide Out, Zoom Out, Bounce Out, Rotate Out, Blur Out
- [x] Compound animation (in + out per clip)

### Phase 20: Polish & UX (CapCut-grade)
- [x] Multi-select clips
- [x] Undo/redo icon with dropdown history
- [x] Light/dark mode toggle
- [x] Drag & drop clips between tracks (drag gesture support)
- [x] Snap to playhead / clip edges
- [x] Pinch-to-zoom timeline
- [x] Haptic feedback (long press multi-select)

### Phase 21: Pro Export & Performance
- [x] Background export (with notification progress)
- [x] Export queue (multiple projects)
- [x] GIF export (added exportGif method)
- [x] PNG sequence export (added exportPngSequence method)
- [x] Hardware encoding (MediaCodec via ffmpeg)

### Phase 22: Advanced AI (No API, All On-Device)
- [x] AI auto beat sync (BPM detection + auto cut)
- [x] AI smart trim (silence + low-motion removal)
- [x] AI auto captions (improved SpeechRecognizer)
- [x] AI voice isolation (noise gate + spectral subtraction via ffmpeg)

## Build Status
- [x] `assembleDebug` passes (verified 2026-06-08)
