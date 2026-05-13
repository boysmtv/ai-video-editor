# AI Video Editor

Automated short-form video pipeline for TikTok and Shopee.

The pipeline now includes an optional LangChain/LangGraph orchestration layer for plan building, hook strategy, and variant ranking.

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
- Uses LangGraph to orchestrate ranking, selection, hook planning, and optional LLM refinement

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

## Smart features in v3

- LangChain/LangGraph orchestration for planning workflow
- Speech-to-text and subtitle generation
- Semantic hook extraction from transcript and niche
- Product similarity using image hashes plus color histogram
- Beat and energy analysis for faster TikTok pacing
- Shot-boundary detection for cleaner segment transitions
- Speech-driven segment alignment with VAD-style activity gating
- Post-render evaluator for opening strength, pacing, speech clarity, and repetition
- A/B variant ranking per platform
- Richer brand/style memory for pacing, crop center, CTA tone, and music bias
- Human override control through `CONFIG/overrides.json`

## LangChain mode

- `use_langchain_orchestrator=true` enables the LangGraph workflow.
- `langchain_llm_enabled=true` enables optional LLM refinement for hooks and strategy notes.
- `langchain_model` can be set to a provider-qualified model string supported by LangChain.
- Without an API key or model config, the LangGraph workflow still runs with deterministic local nodes.
- Storyboard planner for `hook/highlight/peak/loop` and `problem/product/demo/benefit/cta`
- Retention-risk detector for weak openings and repetitive sequences
- Auto safe-zone crop using saliency plus skin-tone heuristics
- Quality confidence score and multi-variant rendering
- Learning loop that updates memory with discovered durations and hooks

## Human override

Edit `CONFIG/overrides.json` when you want to bias the next run without touching code.

- `focus_mode`: force only `tiktok` or `shopee`
- `focus_product_demo`: prioritize clips with stronger product/object focus
- `prefer_spoken_segments`: bias the ranker toward speech-led moments
- `preferred_hook`: force a specific hook
- `preferred_niche`: force niche memory selection
- `max_variants`: override how many variants per mode are rendered
- `banned_phrases`: remove specific words from generated hooks
- `preferred_music_tokens`: bias music file selection by filename tokens

## Notes

- The pipeline is fully usable with real videos in the input folders.
- Several "smart" features use heuristics so they stay runnable locally.
- Whisper transcription is attempted automatically when audio is present and the local environment can load the selected model.
- The current object/product intelligence is still local descriptor-based, not a heavyweight detection model.
