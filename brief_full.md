# FULL BRIEF: ChangeCut — 100% CapCut Pro Feature Set (No API, No Cloud, No Subscription)

## Target
Aplikasi Android video editor FULL-featured setara CapCut Pro, semua fitur terbuka,
tanpa monetisasi, tanpa cloud, tanpa API eksternal, 100% on-device.

## Kondisi Saat Ini (MVP ~25%)
- ✅ Multi-track timeline, trim/split/reorder
- ✅ 10 transitions, 9 color filters, chroma key, speed ramp
- ✅ Keyframe animation (position, scale, rotation, opacity)
- ✅ Text overlay, subtitles, SRT import
- ✅ Audio import, record voice-over
- ✅ AI: scene detection, best moment scoring, TTS, captions
- ✅ Export 4K/FullHD/HD/SD to gallery

## Missing Features (Target 100%)

### Phase 11: Masking & Blending (PRIORITAS #1)
- [ ] Masking: Linear, Radial, Rectangle, Heart, Star, Custom Path
- [ ] Mask animation (keyframe mask position/rotation/scale)
- [ ] Blending Modes: Normal, Screen, Multiply, Overlay, Add, Darken, Lighten, Difference, Hard Light, Soft Light
- [ ] Mask + Blending combined in TrackManager

### Phase 12: Stickers & Overlays (PRIORITAS #2)
- [ ] Sticker catalog (emoji, shapes, arrows, callouts)
- [ ] Image overlay support (PNG with transparency)
- [ ] Sticker animation presets (bounce, fade, slide, scale)
- [ ] Sticker as separate track type (STICKER)
- [ ] Multi-layer sticker support

### Phase 13: Animation System (PRIORITAS #3)
- [ ] In animations: Fade In, Slide In, Zoom In, Bounce In, Rotate In, Blur In
- [ ] Out animations: Fade Out, Slide Out, Zoom Out, Bounce Out, Rotate Out, Blur Out
- [ ] Compound animation (in + out per clip)
- [ ] Animation preview in timeline
- [ ] Auto-animate (AI suggest animations)

### Phase 14: Pro Color Grading (PRIORITAS #4)
- [ ] HSL adjustment (hue/saturation/lightness per color channel)
- [ ] Curves adjustment (RGB + individual channels)
- [ ] LUT support (.cube files)
- [ ] Auto color / auto contrast
- [ ] Vignette effect
- [ ] Color lookup table preview
- [ ] Side-by-side before/after comparison

### Phase 15: Adjustment Layer & Grouping
- [ ] Adjustment layer (applies effects to all clips below)
- [ ] Clip grouping (move/resize/delete as group)
- [ ] Nesting (timeline within timeline)
- [ ] Copy/paste clip styles (effects, filters, animation)

### Phase 16: Advanced Audio
- [ ] Audio EQ (parametric: low/high shelf, peaking, bandpass via ffmpeg)
- [ ] Audio ducking (auto-reduce music volume when voice active)
- [ ] Audio compressor/limiter
- [ ] Audio visualizer (waveform bars)
- [ ] Multi-track audio mixing
- [ ] Beat detection (BPM analysis)

### Phase 17: Advanced Transitions & Effects (x50+)
- [ ] 40+ additional transitions: 3D cube, page curl, ripple, swirl, lens blur, glitch, burn, etc.
- [ ] Custom transition parameters (direction, intensity, duration)
- [ ] Video effects: VHS, glitch, neon, mirror, kaleidoscope, pixelate, mosaic
- [ ] Audio transitions (crossfade, duck in/out)
- [ ] Transition preview in picker

### Phase 18: Pro Speed Tools
- [ ] Curve speed editor (bezier curve ramp up/down)
- [ ] Reverse clip with audio
- [ ] Freeze frame (hold frame for X seconds)
- [ ] Optical flow frame interpolation (smooth slow-mo)
- [ ] Speed presets (0.1x to 100x)

### Phase 19: Templates System
- [ ] Local template format (JSON + media bundle)
- [ ] Template browser / grid
- [ ] "Create template from project"
- [ ] Text templates (animated titles)
- [ ] Intros / outros templates
- [ ] Social media templates (TikTok, Reels, YouTube, etc.)

### Phase 20: Polish & UX (CapCut-grade)
- [ ] Drag & drop clips between tracks
- [ ] Snap to playhead / clip edges
- [ ] Ripple delete / ripple move
- [ ] Pinch-to-zoom timeline
- [ ] Multi-select clips
- [ ] Undo/redo icon with dropdown history
- [ ] Haptic feedback
- [ ] Keyboard shortcuts (if tablet/keyboard)
- [ ] Dynamic island / notch safe areas
- [ ] Light/dark mode toggle
- [ ] Arabic/RTL text support

### Phase 21: Pro Export & Performance
- [ ] Background export (with notification progress)
- [ ] Export queue (multiple projects)
- [ ] GIF export
- [ ] PNG sequence export
- [ ] Proxy editing (low-res for preview, hi-res for export)
- [ ] Hardware encoding (MediaCodec via ffmpeg)
- [ ] Segment-based rendering (progress per clip)
- [ ] Cache warming (pre-render transitions)

### Phase 22: Advanced AI (No API, All On-Device)
- [ ] AI background removal (colorbased chroma key fallback, no ML)
- [ ] AI auto beat sync (BPM detection + auto cut)
- [ ] AI auto color (reference frame sampling)
- [ ] AI smart trim (silence + low-motion removal)
- [ ] AI auto captions (improved SpeechRecognizer)
- [ ] AI voice isolation (noise gate + spectral subtraction via ffmpeg)

## Technical Constraints
- NO external APIs (all AI is heuristic/rule-based or Android built-in)
- NO cloud (100% on-device, no network permission)
- NO subscriptions (all features unlocked)
- NO watermark
- NO ffmpeg-kit (use ProcessBuilder-based FfmpegExecutor)
- Compose 2026.01.01 + Material3
- Follow existing code patterns, architecture, naming

## Work Order
Kerjakan berurutan dari Phase 11 ke atas. Setiap phase harus:
1. Build success
2. Tidak merusak fitur phase sebelumnya
3. Testable (walaupun manual)
