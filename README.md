# AI Video Editor

Automated short-form video pipeline for TikTok and Shopee.

## What it does

- Loads `CONFIG/settings.json`
- Loads memory from `MEMORY/*.json`
- Scans `INPUT/RAW_VIDEO` and `INPUT/DAILY`
- Transcribes speech with local Whisper when available
- Detects lifestyle vs affiliate content using filename, transcript, and product reference matching
- Scores frames for brightness, sharpness, motion, contrast, repetition, face/hand presence, and action
- Detects beat peaks for cut timing and opening energy
- Builds platform-specific scores for TikTok retention and Shopee conversion
- Generates contextual hooks, storyboard roles, subtitle sidecars, and multiple variants per mode
- Learns updated hook and duration patterns back into memory
- Builds vertical 9:16 edits for TikTok and Shopee
- Exports MP4 files to `OUTPUT/TIKTOK` and `OUTPUT/SHOPEE`
- Logs each run to `MEMORY/history_log.json`

## Usage

```bash
python3 run_pipeline.py --project-root /Users/dedywijaya/Work/Google/ai/ai-video-editor
```

Optional overrides:

```bash
python3 run_pipeline.py --project-root /Users/dedywijaya/Work/Google/ai/ai-video-editor --mode tiktok
python3 run_pipeline.py --project-root /Users/dedywijaya/Work/Google/ai/ai-video-editor --both
```

## Outputs

- Put raw videos in `INPUT/RAW_VIDEO` or `INPUT/DAILY`.
- Put product reference images or videos in `INPUT/PRODUCT` to improve affiliate detection.
- Put background music in `AUDIO/MUSIC`.
- Put custom fonts in `ASSETS/FONT` if you want the hook overlay to use them.
- Rendered videos go to `OUTPUT/TIKTOK` and `OUTPUT/SHOPEE`.
- Subtitle sidecars go to `OUTPUT/SUBTITLES`.
- Per-output analysis JSON goes to `OUTPUT/ANALYSIS`.
- Per-run reports go to `OUTPUT/REPORTS`.

## Smart features in v2

- Speech-to-text and subtitle generation
- Semantic hook extraction from transcript and niche
- Product similarity using image hashes plus color histogram
- Beat and energy analysis for faster TikTok pacing
- Storyboard planner for `hook/highlight/peak/loop` and `problem/product/demo/benefit/cta`
- Retention-risk detector for weak openings and repetitive sequences
- Auto safe-zone crop using saliency plus skin-tone heuristics
- Quality confidence score and multi-variant rendering
- Learning loop that updates memory with discovered durations and hooks

## Notes

- The pipeline is fully usable with real videos in the input folders.
- Several "smart" features use heuristics so they stay runnable locally.
- Whisper transcription is attempted automatically when audio is present and the local environment can load the selected model.
