from __future__ import annotations

import json
import math
import re
import subprocess
import tempfile
import wave
from dataclasses import asdict, dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any, Iterable

import numpy as np
from PIL import Image

VIDEO_EXTENSIONS = {".mp4", ".mov", ".m4v", ".avi", ".mkv", ".webm"}
IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}
AUDIO_EXTENSIONS = {".mp3", ".wav", ".m4a", ".aac"}
DEFAULT_TIMESTAMP_FORMAT = "%Y%m%d_%H%M%S"
WHISPER_IMPORT_ERROR: Exception | None = None

try:
    import whisper  # type: ignore[import-not-found]
except Exception as exc:  # pragma: no cover - environment dependent
    whisper = None
    WHISPER_IMPORT_ERROR = exc


@dataclass(slots=True)
class Settings:
    mode: str = "auto"
    generate_both_versions: bool = True
    variants_per_mode: int = 2
    sample_interval_seconds: float = 0.8
    min_segment_seconds: float = 1.5
    max_segment_seconds: float = 3.2
    tiktok_duration_seconds: list[float] = field(default_factory=lambda: [15.0, 30.0])
    shopee_duration_seconds: list[float] = field(default_factory=lambda: [20.0, 45.0])
    target_width: int = 1080
    target_height: int = 1920
    crf: int = 21
    preset: str = "medium"
    enable_deshake: bool = True
    enable_text_overlay: bool = True
    enable_subtitles: bool = True
    embed_subtitles_when_possible: bool = False
    transcription_enabled: bool = True
    whisper_model: str = "tiny"
    transcription_language: str = "id"
    beat_sync_enabled: bool = True
    learning_enabled: bool = True
    scene_understanding_enabled: bool = True
    warm_grade_strength: float = 1.08
    contrast_boost: float = 1.05
    brightness_boost: float = 0.02
    saturation_boost: float = 1.08
    sharpen_strength: float = 0.80
    audio_ducking: float = 0.28
    subtitle_max_chars: int = 42
    confidence_low_threshold: float = 0.55
    max_history_runs: int = 100
    subtitle_font_size: int = 42
    hook_font_size: int = 56
    tiktok_speed_cycle: list[float] = field(default_factory=lambda: [1.15, 0.95, 1.08, 0.92])
    shopee_speed_cycle: list[float] = field(default_factory=lambda: [1.0, 0.97, 1.03, 1.0])
    tiktok_structure: list[str] = field(
        default_factory=lambda: ["hook", "highlight", "highlight", "peak", "loop"]
    )
    shopee_structure: list[str] = field(
        default_factory=lambda: ["problem", "lifestyle", "product", "demo", "benefit", "cta"]
    )
    product_keywords: list[str] = field(
        default_factory=lambda: [
            "product",
            "shopee",
            "affiliate",
            "review",
            "unboxing",
            "serum",
            "skincare",
            "makeup",
            "bag",
            "bottle",
            "kitchen",
            "gadget",
            "rak",
            "organizer",
            "cable",
            "cleaner",
        ]
    )
    lifestyle_keywords: list[str] = field(
        default_factory=lambda: [
            "daily",
            "lifestyle",
            "vlog",
            "morning",
            "night",
            "cozy",
            "travel",
            "day",
            "routine",
            "cafe",
            "desk",
        ]
    )
    product_transcript_keywords: list[str] = field(
        default_factory=lambda: [
            "produk",
            "barang",
            "beli",
            "pakai",
            "pake",
            "dipakai",
            "kepake",
            "review",
            "shopee",
            "affiliate",
            "useful",
            "praktis",
            "hemat",
        ]
    )
    niche_profiles: dict[str, dict[str, Any]] = field(
        default_factory=lambda: {
            "skincare": {
                "keywords": ["serum", "skincare", "mask", "toner", "moisturizer"],
                "tiktok_hook": "POV: step kecil ini bikin routine terasa beda",
                "shopee_hook": "awalnya iseng coba, ternyata enak dipake tiap hari",
            },
            "kitchen": {
                "keywords": ["kitchen", "masak", "dapur", "pisau", "botol", "storage"],
                "tiktok_hook": "ini bagian dapur yang paling satisfying hari ini",
                "shopee_hook": "gak nyangka barang dapur ini jadi sering kepake",
            },
            "fashion": {
                "keywords": ["bag", "outfit", "fashion", "sepatu", "baju"],
                "tiktok_hook": "POV: outfit jadi beres cuma gara-gara detail ini",
                "shopee_hook": "awalnya cuma lucu, ternyata kepake terus",
            },
            "gadget": {
                "keywords": ["gadget", "charger", "cable", "desk", "tech"],
                "tiktok_hook": "ini hal kecil di meja kerja yang bikin nagih",
                "shopee_hook": "ternyata gadget kecil ini bantu banget buat daily setup",
            },
            "home": {
                "keywords": ["home", "organizer", "storage", "cleaner", "rak"],
                "tiktok_hook": "ini hal kecil yang bikin rumah terasa lebih rapi",
                "shopee_hook": "awalnya iseng beli, ternyata rumah jadi lebih beres",
            },
        }
    )

    @classmethod
    def from_file(cls, path: Path) -> "Settings":
        if not path.exists():
            return cls()
        payload = json.loads(path.read_text())
        defaults = asdict(cls())
        defaults.update(payload)
        return cls(**defaults)


@dataclass(slots=True)
class MemoryBundle:
    style_profile: dict[str, Any]
    best_performance: dict[str, Any]
    history_log: dict[str, Any]


@dataclass(slots=True)
class VideoMetadata:
    path: Path
    duration: float
    width: int
    height: int
    fps: float
    has_audio: bool


@dataclass(slots=True)
class TranscriptSegment:
    start: float
    end: float
    text: str
    confidence: float


@dataclass(slots=True)
class TranscriptResult:
    segments: list[TranscriptSegment]
    full_text: str
    source: str
    language: str | None
    transcript_confidence: float
    keywords: list[str]


@dataclass(slots=True)
class AudioFeatures:
    beat_times: list[float]
    energy_peaks: list[float]
    energy_mean: float
    energy_std: float
    speech_density: float
    has_voice: bool


@dataclass(slots=True)
class SegmentCandidate:
    source_path: Path
    start: float
    end: float
    duration: float
    score: float
    visual_score: float
    audio_score: float
    semantic_score: float
    product_match: float
    product_confidence: float
    sharpness: float
    brightness: float
    contrast: float
    saturation: float
    motion: float
    uniqueness: float
    face_score: float
    hand_score: float
    action_score: float
    emotion_score: float
    benefit_score: float
    demo_score: float
    transcript_score: float
    beat_alignment: float
    retention_risk: float
    confidence: float
    safe_center_x: float
    safe_center_y: float
    semantic_tags: list[str]
    transcript_text: str
    storyboard_role: str
    mode_scores: dict[str, float]
    note: str


@dataclass(slots=True)
class ClipAnalysis:
    metadata: VideoMetadata
    transcript: TranscriptResult
    audio_features: AudioFeatures
    candidates: list[SegmentCandidate]
    detected_product: bool
    detection_score: float
    average_quality: float
    niche: str
    content_tags: list[str]
    summary: str


@dataclass(slots=True)
class StoryboardStep:
    role: str
    segment: SegmentCandidate
    rationale: str


@dataclass(slots=True)
class RenderPlan:
    mode: str
    variant_index: int
    variant_name: str
    content_type: str
    niche: str
    hook: str
    overlay_text: str | None
    selected_segments: list[SegmentCandidate]
    target_duration: float
    music_path: Path | None
    subtitles: list[TranscriptSegment]
    storyboard: list[StoryboardStep]
    retention_risks: list[str]
    confidence_score: float
    quality_band: str
    strategy_notes: list[str]


class VideoEditingPipeline:
    def __init__(self, project_root: Path) -> None:
        self.project_root = project_root.resolve()
        self.config_path = self.project_root / "CONFIG" / "settings.json"
        self.memory_dir = self.project_root / "MEMORY"
        self.input_raw_dir = self.project_root / "INPUT" / "RAW_VIDEO"
        self.input_daily_dir = self.project_root / "INPUT" / "DAILY"
        self.input_product_dir = self.project_root / "INPUT" / "PRODUCT"
        self.output_dir = self.project_root / "OUTPUT"
        self.music_dir = self.project_root / "AUDIO" / "MUSIC"
        self.font_dir = self.project_root / "ASSETS" / "FONT"
        self.report_dir = self.output_dir / "REPORTS"
        self.subtitle_dir = self.output_dir / "SUBTITLES"
        self.analysis_dir = self.output_dir / "ANALYSIS"
        self._whisper_model_cache: Any | None = None
        self._whisper_load_failed = False

    def run(self, mode_override: str | None = None, force_both: bool = False) -> dict[str, Any]:
        self._ensure_directories()
        settings = Settings.from_file(self.config_path)
        if mode_override:
            settings.mode = mode_override
        if force_both:
            settings.generate_both_versions = True

        memory = self._load_memory(settings)
        raw_videos = self._scan_video_inputs()
        if not raw_videos:
            raise RuntimeError(
                "No raw videos found in INPUT/RAW_VIDEO or INPUT/DAILY. Add clips and rerun."
            )

        with tempfile.TemporaryDirectory(prefix="ai_video_editor_") as temp_root_name:
            temp_root = Path(temp_root_name)
            reference_features = self._build_product_reference_features(temp_root)
            analyses = [
                self._analyze_clip(video_path, settings, reference_features, temp_root)
                for video_path in raw_videos
            ]

            content_type = self._detect_content_type(raw_videos, analyses, settings)
            niche = self._detect_niche(analyses, settings)
            modes = self._resolve_modes(settings, content_type)
            if settings.generate_both_versions or force_both:
                modes = ["tiktok", "shopee"]

            outputs: list[dict[str, Any]] = []
            for mode in modes:
                plans = self._build_render_plans(mode, content_type, niche, analyses, memory, settings)
                for plan in plans:
                    outputs.append(self._render_plan(plan, settings, temp_root))

        learning_summary = self._learn_from_run(outputs, analyses, memory, settings)
        run_report = {
            "timestamp": datetime.now().isoformat(timespec="seconds"),
            "content_type": content_type,
            "niche": niche,
            "selected_modes": modes,
            "outputs": outputs,
            "clips_analyzed": len(analyses),
            "analysis_summaries": [analysis.summary for analysis in analyses],
            "learning_summary": learning_summary,
        }
        serializable_report = self._json_ready(run_report)
        self._append_history(serializable_report, settings)
        report_path = self._write_report(serializable_report)
        serializable_report["report_path"] = str(report_path)
        print(json.dumps(serializable_report, indent=2))
        return serializable_report

    def _ensure_directories(self) -> None:
        for path in [
            self.input_raw_dir,
            self.input_daily_dir,
            self.input_product_dir,
            self.output_dir / "TIKTOK",
            self.output_dir / "SHOPEE",
            self.output_dir / "REPORTS",
            self.output_dir / "SUBTITLES",
            self.output_dir / "ANALYSIS",
            self.project_root / "AUDIO" / "MUSIC",
            self.project_root / "AUDIO" / "SFX",
            self.project_root / "ASSETS" / "FONT",
            self.project_root / "ASSETS" / "LOGO",
            self.project_root / "ASSETS" / "OVERLAY",
            self.memory_dir,
            self.project_root / "CONFIG",
        ]:
            path.mkdir(parents=True, exist_ok=True)

    def _scan_video_inputs(self) -> list[Path]:
        candidates = list(self.input_raw_dir.iterdir()) + list(self.input_daily_dir.iterdir())
        return sorted(
            path for path in candidates if path.is_file() and path.suffix.lower() in VIDEO_EXTENSIONS
        )

    def _load_memory(self, settings: Settings) -> MemoryBundle:
        style_profile = self._read_json_with_default(
            self.memory_dir / "style_profile.json",
            {
                "editing_style": "natural_dynamic",
                "color_tone": "warm_vibrant",
                "cut_speed": "adaptive",
                "caption_style": "minimal_bold",
                "platform_weights": {
                    "tiktok": {
                        "visual": 0.24,
                        "audio": 0.14,
                        "semantic": 0.28,
                        "hook": 0.20,
                        "retention_penalty": 0.14,
                    },
                    "shopee": {
                        "visual": 0.20,
                        "audio": 0.08,
                        "semantic": 0.40,
                        "hook": 0.12,
                        "retention_penalty": 0.10,
                    },
                },
            },
        )
        best_performance = self._read_json_with_default(
            self.memory_dir / "best_performance.json",
            {
                "tiktok_hooks": [
                    "POV: satu momen kecil ini bikin hari lebih enak",
                    "day in my life, tapi bagian ini paling satisfying",
                    "ini hal kecil yang bikin aku ulang terus",
                ],
                "shopee_hooks": [
                    "awalnya iseng beli ini, ternyata kepake banget",
                    "gak nyangka ini useful banget buat daily routine",
                    "ternyata yang simpel begini malah paling sering dipake",
                ],
                "best_duration_seconds": {"tiktok": 22, "shopee": 30},
                "best_structure": {
                    "tiktok": settings.tiktok_structure,
                    "shopee": settings.shopee_structure,
                },
                "learned_patterns": {
                    "high_retention_openers": [],
                    "high_conversion_angles": [],
                    "niche_winners": {},
                },
            },
        )
        history_log = self._read_json_with_default(self.memory_dir / "history_log.json", {"runs": []})
        return MemoryBundle(style_profile, best_performance, history_log)

    def _read_json_with_default(self, path: Path, default: dict[str, Any]) -> dict[str, Any]:
        if not path.exists():
            path.write_text(json.dumps(default, indent=2))
            return default
        payload = json.loads(path.read_text())
        merged = default.copy()
        merged.update(payload)
        return merged

    def _write_report(self, report: dict[str, Any]) -> Path:
        timestamp = datetime.now().strftime(DEFAULT_TIMESTAMP_FORMAT)
        path = self.report_dir / f"run_{timestamp}.json"
        path.write_text(json.dumps(self._json_ready(report), indent=2))
        return path

    def _append_history(self, report: dict[str, Any], settings: Settings) -> None:
        history_path = self.memory_dir / "history_log.json"
        history = self._read_json_with_default(history_path, {"runs": []})
        history.setdefault("runs", []).append(self._json_ready(report))
        history["runs"] = history["runs"][-settings.max_history_runs :]
        history_path.write_text(json.dumps(history, indent=2))

    def _build_product_reference_features(self, temp_root: Path) -> list[dict[str, Any]]:
        references: list[dict[str, Any]] = []
        for path in sorted(self.input_product_dir.iterdir()):
            if not path.is_file():
                continue
            suffix = path.suffix.lower()
            if suffix in IMAGE_EXTENSIONS:
                references.append(self._image_reference_features(path))
            elif suffix in VIDEO_EXTENSIONS:
                frame_dir = temp_root / f"product_{path.stem}"
                frame_dir.mkdir(parents=True, exist_ok=True)
                preview_path = frame_dir / "preview.jpg"
                self._run_command(
                    [
                        "ffmpeg",
                        "-hide_banner",
                        "-loglevel",
                        "error",
                        "-y",
                        "-i",
                        str(path),
                        "-frames:v",
                        "1",
                        str(preview_path),
                    ]
                )
                if preview_path.exists():
                    references.append(self._image_reference_features(preview_path))
        return references

    def _image_reference_features(self, path: Path) -> dict[str, Any]:
        image = Image.open(path).convert("RGB")
        array = np.asarray(image, dtype=np.float32)
        return {
            "path": path,
            "hash": self._average_hash(image),
            "center_hash": self._average_hash(self._center_crop_image(image)),
            "histogram": self._color_histogram(array),
        }

    def _analyze_clip(
        self,
        path: Path,
        settings: Settings,
        reference_features: list[dict[str, Any]],
        temp_root: Path,
    ) -> ClipAnalysis:
        metadata = self._probe_video(path)
        work_dir = temp_root / path.stem
        work_dir.mkdir(parents=True, exist_ok=True)
        transcript = self._transcribe_clip(path, settings, work_dir, metadata)
        audio_features = self._extract_audio_features(path, metadata, transcript, work_dir, settings)

        sample_dir = work_dir / "samples"
        sample_dir.mkdir(parents=True, exist_ok=True)
        sample_pattern = sample_dir / "frame_%06d.jpg"
        self._run_command(
            [
                "ffmpeg",
                "-hide_banner",
                "-loglevel",
                "error",
                "-y",
                "-i",
                str(path),
                "-vf",
                f"fps=1/{settings.sample_interval_seconds},scale=320:-1",
                str(sample_pattern),
            ]
        )

        frame_paths = sorted(sample_dir.glob("frame_*.jpg"))
        if not frame_paths:
            return ClipAnalysis(
                metadata=metadata,
                transcript=transcript,
                audio_features=audio_features,
                candidates=[],
                detected_product=False,
                detection_score=0.0,
                average_quality=0.0,
                niche="lifestyle",
                content_tags=[],
                summary=f"{path.name}: no frames sampled",
            )

        file_product_score = self._filename_detection_score(path.name, settings.product_keywords)
        file_lifestyle_score = self._filename_detection_score(path.name, settings.lifestyle_keywords)
        transcript_product_bias = self._keyword_score(
            transcript.full_text, settings.product_transcript_keywords
        )

        candidates: list[SegmentCandidate] = []
        scores: list[float] = []
        previous_luma: np.ndarray | None = None
        previous_hash: np.ndarray | None = None
        product_match_peak = 0.0
        tags_counter: dict[str, int] = {}

        for index, frame_path in enumerate(frame_paths):
            timestamp = round(index * settings.sample_interval_seconds, 3)
            metrics = self._frame_metrics(frame_path)
            motion = 0.0
            uniqueness = 1.0
            if previous_luma is not None:
                motion = float(np.mean(np.abs(metrics["luma"] - previous_luma)) / 255.0)
            if previous_hash is not None:
                uniqueness = float(np.mean(previous_hash != metrics["hash"]))

            product_match, product_confidence = self._product_similarity(metrics, reference_features)
            product_match = max(product_match, file_product_score * 0.6, transcript_product_bias * 0.4)
            product_match_peak = max(product_match_peak, product_match)

            transcript_chunk = self._transcript_window(transcript.segments, timestamp, timestamp + settings.max_segment_seconds)
            transcript_text = " ".join(segment.text for segment in transcript_chunk).strip()
            transcript_score = self._transcript_intensity(transcript_text)
            semantic_tags = self._semantic_tags_for_window(transcript_text, metrics, motion, product_match)
            for tag in semantic_tags:
                tags_counter[tag] = tags_counter.get(tag, 0) + 1

            emotion_score = self._emotion_score(transcript_text, metrics, motion)
            benefit_score = self._benefit_score(transcript_text, product_match)
            demo_score = self._demo_score(transcript_text, metrics, motion, product_match)
            face_score = metrics["face_score"]
            hand_score = metrics["hand_score"]
            action_score = self._action_score(metrics, motion, transcript_text)
            beat_alignment = self._beat_alignment(timestamp, audio_features.beat_times)
            visual_score = self._visual_score(metrics, motion, uniqueness)
            audio_score = self._audio_score(audio_features, timestamp, transcript_text, beat_alignment)
            semantic_score = (
                emotion_score * 0.18
                + benefit_score * 0.25
                + demo_score * 0.25
                + transcript_score * 0.16
                + product_match * 0.16
            )
            retention_risk = self._retention_risk(
                timestamp=timestamp,
                visual_score=visual_score,
                transcript_score=transcript_score,
                motion=motion,
                uniqueness=uniqueness,
                product_match=product_match,
                mode="generic",
            )
            confidence = self._confidence_score(visual_score, semantic_score, audio_score, retention_risk)
            note = "product_visible" if product_match >= 0.82 else "quality_pass"

            quality_gate = (
                metrics["brightness"] >= 0.12
                and metrics["sharpness"] >= 4.5
                and motion <= 0.52
                and visual_score >= 0.24
            )
            if quality_gate:
                end = min(timestamp + settings.max_segment_seconds, metadata.duration)
                start = max(0.0, min(timestamp, end - settings.min_segment_seconds))
                start, end = self._align_segment_to_beats(start, end, audio_features.beat_times, settings)
                storyboard_role = self._initial_storyboard_role(
                    transcript_text, semantic_tags, product_match, emotion_score, demo_score
                )
                candidate = SegmentCandidate(
                    source_path=path,
                    start=round(start, 3),
                    end=round(end, 3),
                    duration=round(end - start, 3),
                    score=round(visual_score * 0.38 + audio_score * 0.18 + semantic_score * 0.44, 4),
                    visual_score=round(visual_score, 4),
                    audio_score=round(audio_score, 4),
                    semantic_score=round(semantic_score, 4),
                    product_match=round(product_match, 4),
                    product_confidence=round(product_confidence, 4),
                    sharpness=round(metrics["sharpness"], 4),
                    brightness=round(metrics["brightness"], 4),
                    contrast=round(metrics["contrast"], 4),
                    saturation=round(metrics["saturation"], 4),
                    motion=round(motion, 4),
                    uniqueness=round(uniqueness, 4),
                    face_score=round(face_score, 4),
                    hand_score=round(hand_score, 4),
                    action_score=round(action_score, 4),
                    emotion_score=round(emotion_score, 4),
                    benefit_score=round(benefit_score, 4),
                    demo_score=round(demo_score, 4),
                    transcript_score=round(transcript_score, 4),
                    beat_alignment=round(beat_alignment, 4),
                    retention_risk=round(retention_risk, 4),
                    confidence=round(confidence, 4),
                    safe_center_x=round(metrics["safe_center_x"], 4),
                    safe_center_y=round(metrics["safe_center_y"], 4),
                    semantic_tags=semantic_tags,
                    transcript_text=transcript_text,
                    storyboard_role=storyboard_role,
                    mode_scores={},
                    note=note,
                )
                candidate.mode_scores = {
                    "tiktok": round(self._platform_score(candidate, "tiktok"), 4),
                    "shopee": round(self._platform_score(candidate, "shopee"), 4),
                }
                candidates.append(candidate)
                scores.append(candidate.score)

            previous_luma = metrics["luma"]
            previous_hash = metrics["hash"]

        detected_product = (
            product_match_peak >= 0.74
            or file_product_score >= 0.5
            or transcript_product_bias >= 0.55
        )
        detection_score = max(product_match_peak, file_product_score, transcript_product_bias)
        content_tags = [tag for tag, count in sorted(tags_counter.items(), key=lambda item: item[1], reverse=True)[:6]]
        niche = self._niche_from_text(
            " ".join([path.name, transcript.full_text, " ".join(content_tags)]),
            settings,
        )
        average_quality = float(np.mean(scores)) if scores else 0.0
        summary = (
            f"{path.name}: {len(candidates)} candidates, transcript={transcript.source}, "
            f"product_score={detection_score:.2f}, niche={niche}, tags={', '.join(content_tags[:4]) or 'none'}"
        )
        return ClipAnalysis(
            metadata=metadata,
            transcript=transcript,
            audio_features=audio_features,
            candidates=candidates,
            detected_product=detected_product,
            detection_score=round(detection_score, 4),
            average_quality=round(average_quality, 4),
            niche=niche,
            content_tags=content_tags,
            summary=summary,
        )

    def _probe_video(self, path: Path) -> VideoMetadata:
        payload = self._run_command(
            [
                "ffprobe",
                "-v",
                "error",
                "-print_format",
                "json",
                "-show_streams",
                "-show_format",
                str(path),
            ],
            capture_output=True,
        )
        data = json.loads(payload)
        video_stream = next(stream for stream in data["streams"] if stream["codec_type"] == "video")
        width = int(video_stream["width"])
        height = int(video_stream["height"])
        fps_raw = video_stream.get("avg_frame_rate", "0/1")
        numerator, denominator = fps_raw.split("/", maxsplit=1)
        fps = float(numerator) / max(float(denominator), 1.0)
        duration = float(data["format"].get("duration", 0.0))
        has_audio = any(stream["codec_type"] == "audio" for stream in data["streams"])
        return VideoMetadata(path=path, duration=duration, width=width, height=height, fps=fps, has_audio=has_audio)

    def _transcribe_clip(
        self,
        path: Path,
        settings: Settings,
        work_dir: Path,
        metadata: VideoMetadata,
    ) -> TranscriptResult:
        if not settings.transcription_enabled or not metadata.has_audio:
            return TranscriptResult([], "", "disabled", None, 0.0, [])

        audio_path = work_dir / "audio.wav"
        self._run_command(
            [
                "ffmpeg",
                "-hide_banner",
                "-loglevel",
                "error",
                "-y",
                "-i",
                str(path),
                "-vn",
                "-ac",
                "1",
                "-ar",
                "16000",
                "-c:a",
                "pcm_s16le",
                str(audio_path),
            ]
        )
        if not audio_path.exists():
            return TranscriptResult([], "", "audio_extract_failed", None, 0.0, [])

        try:
            model = self._load_whisper_model(settings.whisper_model)
            if model is None:
                source = "whisper_unavailable"
                if WHISPER_IMPORT_ERROR:
                    source = f"whisper_import_error:{WHISPER_IMPORT_ERROR.__class__.__name__}"
                return TranscriptResult([], "", source, None, 0.0, [])

            result = model.transcribe(
                str(audio_path),
                language=settings.transcription_language,
                fp16=False,
                verbose=False,
            )
            segments = [
                TranscriptSegment(
                    start=float(segment["start"]),
                    end=float(segment["end"]),
                    text=segment["text"].strip(),
                    confidence=float(segment.get("avg_logprob", -1.0)),
                )
                for segment in result.get("segments", [])
                if segment.get("text", "").strip()
            ]
            full_text = " ".join(segment.text for segment in segments).strip()
            confidences = [segment.confidence for segment in segments if segment.confidence > -10]
            normalized_confidence = self._normalize_whisper_confidence(confidences)
            keywords = self._extract_keywords(full_text)
            return TranscriptResult(
                segments=segments,
                full_text=full_text,
                source="whisper",
                language=result.get("language"),
                transcript_confidence=normalized_confidence,
                keywords=keywords,
            )
        except Exception as exc:  # pragma: no cover - model/runtime dependent
            return TranscriptResult([], "", f"transcription_failed:{exc.__class__.__name__}", None, 0.0, [])

    def _load_whisper_model(self, model_name: str) -> Any | None:
        if self._whisper_load_failed:
            return None
        if self._whisper_model_cache is not None:
            return self._whisper_model_cache
        if whisper is None:
            self._whisper_load_failed = True
            return None
        try:
            self._whisper_model_cache = whisper.load_model(model_name)
            return self._whisper_model_cache
        except Exception:  # pragma: no cover - network/cache dependent
            self._whisper_load_failed = True
            return None

    def _extract_audio_features(
        self,
        path: Path,
        metadata: VideoMetadata,
        transcript: TranscriptResult,
        work_dir: Path,
        settings: Settings,
    ) -> AudioFeatures:
        if not metadata.has_audio:
            return AudioFeatures([], [], 0.0, 0.0, 0.0, False)

        audio_path = work_dir / "analysis.wav"
        self._run_command(
            [
                "ffmpeg",
                "-hide_banner",
                "-loglevel",
                "error",
                "-y",
                "-i",
                str(path),
                "-vn",
                "-ac",
                "1",
                "-ar",
                "16000",
                "-c:a",
                "pcm_s16le",
                str(audio_path),
            ]
        )
        if not audio_path.exists():
            return AudioFeatures([], [], 0.0, 0.0, 0.0, False)

        sample_rate, audio = self._read_wav(audio_path)
        if audio.size == 0:
            return AudioFeatures([], [], 0.0, 0.0, 0.0, False)

        frame_size = 1024
        hop = 512
        energies: list[float] = []
        for start in range(0, max(len(audio) - frame_size, 1), hop):
            frame = audio[start : start + frame_size]
            if frame.size == 0:
                continue
            energies.append(float(np.mean(frame * frame)))
        if not energies:
            return AudioFeatures([], [], 0.0, 0.0, 0.0, bool(transcript.segments))

        energy_array = np.asarray(energies, dtype=np.float32)
        normalized = (energy_array - float(np.mean(energy_array))) / max(float(np.std(energy_array)), 1e-6)
        beat_times: list[float] = []
        energy_peaks: list[float] = []
        last_peak_time = -1.0
        for index in range(1, len(normalized) - 1):
            if normalized[index] < 0.65:
                continue
            if not (normalized[index] >= normalized[index - 1] and normalized[index] > normalized[index + 1]):
                continue
            peak_time = index * hop / sample_rate
            if peak_time - last_peak_time < 0.24:
                continue
            beat_times.append(round(peak_time, 3))
            energy_peaks.append(round(float(normalized[index]), 4))
            last_peak_time = peak_time

        speech_duration = sum(segment.end - segment.start for segment in transcript.segments)
        speech_density = speech_duration / max(metadata.duration, 1e-6)
        has_voice = bool(transcript.segments) and transcript.transcript_confidence > 0.15
        if not settings.beat_sync_enabled:
            beat_times = []
        return AudioFeatures(
            beat_times=beat_times,
            energy_peaks=energy_peaks,
            energy_mean=round(float(np.mean(energy_array)), 6),
            energy_std=round(float(np.std(energy_array)), 6),
            speech_density=round(speech_density, 4),
            has_voice=has_voice,
        )

    def _read_wav(self, path: Path) -> tuple[int, np.ndarray]:
        with wave.open(str(path), "rb") as wav_file:
            sample_rate = wav_file.getframerate()
            frames = wav_file.readframes(wav_file.getnframes())
        audio = np.frombuffer(frames, dtype=np.int16).astype(np.float32) / 32768.0
        return sample_rate, audio

    def _frame_metrics(self, path: Path) -> dict[str, Any]:
        image = Image.open(path).convert("RGB")
        array = np.asarray(image, dtype=np.float32)
        luma = np.dot(array[..., :3], [0.299, 0.587, 0.114]).astype(np.float32)
        brightness = float(np.mean(luma) / 255.0)
        contrast = float(np.std(luma) / 255.0)
        dx = np.diff(luma, axis=1)
        dy = np.diff(luma, axis=0)
        sharpness = float(np.var(dx) + np.var(dy))

        rgb_max = np.max(array, axis=2)
        rgb_min = np.min(array, axis=2)
        saturation = float(np.mean(np.where(rgb_max == 0, 0, (rgb_max - rgb_min) / np.maximum(rgb_max, 1e-6))))
        hashed = self._average_hash(Image.fromarray(array.astype(np.uint8)))
        center_hash = self._average_hash(self._center_crop_image(Image.fromarray(array.astype(np.uint8))))
        histogram = self._color_histogram(array)
        face_score, hand_score, safe_center_x, safe_center_y = self._human_presence_metrics(array, luma)

        return {
            "brightness": brightness,
            "contrast": contrast,
            "sharpness": sharpness,
            "saturation": saturation,
            "hash": hashed,
            "center_hash": center_hash,
            "histogram": histogram,
            "luma": luma,
            "face_score": face_score,
            "hand_score": hand_score,
            "safe_center_x": safe_center_x,
            "safe_center_y": safe_center_y,
        }

    def _human_presence_metrics(self, array: np.ndarray, luma: np.ndarray) -> tuple[float, float, float, float]:
        r = array[..., 0]
        g = array[..., 1]
        b = array[..., 2]
        skin_mask = (
            (r > 95)
            & (g > 40)
            & (b > 20)
            & ((np.max(array, axis=2) - np.min(array, axis=2)) > 15)
            & (np.abs(r - g) > 15)
            & (r > g)
            & (r > b)
        )
        height, width = skin_mask.shape
        upper = skin_mask[: max(height // 2, 1), :]
        lower = skin_mask[height // 3 :, :]
        center = skin_mask[height // 5 : max(height * 3 // 5, 1), width // 4 : max(width * 3 // 4, 1)]
        face_score = float(np.mean(upper) * 0.6 + np.mean(center) * 0.4)
        hand_score = float(np.mean(lower) * 0.55 + np.mean(center) * 0.45)

        gy, gx = np.gradient(luma)
        saliency = np.abs(gx) + np.abs(gy) + (skin_mask.astype(np.float32) * 24.0)
        total = float(np.sum(saliency))
        if total <= 1e-6:
            return face_score, hand_score, 0.5, 0.45
        ys, xs = np.indices(saliency.shape)
        safe_center_x = float(np.sum(xs * saliency) / total / max(width - 1, 1))
        safe_center_y = float(np.sum(ys * saliency) / total / max(height - 1, 1))
        safe_center_x = float(np.clip(safe_center_x, 0.2, 0.8))
        safe_center_y = float(np.clip(safe_center_y, 0.25, 0.65))
        return face_score, hand_score, safe_center_x, safe_center_y

    def _center_crop_image(self, image: Image.Image) -> Image.Image:
        width, height = image.size
        left = int(width * 0.2)
        top = int(height * 0.2)
        right = int(width * 0.8)
        bottom = int(height * 0.8)
        return image.crop((left, top, right, bottom))

    def _color_histogram(self, array: np.ndarray) -> np.ndarray:
        hist_channels: list[np.ndarray] = []
        for channel in range(3):
            hist, _ = np.histogram(array[..., channel], bins=16, range=(0, 255), density=True)
            hist_channels.append(hist.astype(np.float32))
        return np.concatenate(hist_channels)

    def _product_similarity(
        self, metrics: dict[str, Any], reference_features: list[dict[str, Any]]
    ) -> tuple[float, float]:
        if not reference_features:
            return 0.0, 0.0
        scores: list[float] = []
        for reference in reference_features:
            hash_score = 1.0 - float(np.mean(metrics["hash"] != reference["hash"]))
            center_score = 1.0 - float(np.mean(metrics["center_hash"] != reference["center_hash"]))
            hist_score = self._histogram_similarity(metrics["histogram"], reference["histogram"])
            scores.append(hash_score * 0.35 + center_score * 0.30 + hist_score * 0.35)
        if not scores:
            return 0.0, 0.0
        best = max(scores)
        confidence = float(np.mean(sorted(scores, reverse=True)[: min(len(scores), 2)]))
        return best, confidence

    def _histogram_similarity(self, left: np.ndarray, right: np.ndarray) -> float:
        distance = float(np.linalg.norm(left - right))
        return float(max(0.0, 1.0 - distance / max(math.sqrt(len(left)), 1.0)))

    def _transcript_window(
        self, segments: list[TranscriptSegment], start: float, end: float
    ) -> list[TranscriptSegment]:
        return [segment for segment in segments if segment.end >= start and segment.start <= end]

    def _extract_keywords(self, text: str) -> list[str]:
        tokens = re.findall(r"[a-zA-Z0-9]+", text.lower())
        stop_words = {
            "yang",
            "dan",
            "ini",
            "itu",
            "aku",
            "jadi",
            "buat",
            "untuk",
            "the",
            "and",
            "with",
            "banget",
            "sama",
            "atau",
        }
        keywords: list[str] = []
        seen: set[str] = set()
        for token in tokens:
            if len(token) < 3 or token in stop_words or token in seen:
                continue
            seen.add(token)
            keywords.append(token)
            if len(keywords) >= 12:
                break
        return keywords

    def _normalize_whisper_confidence(self, confidences: list[float]) -> float:
        if not confidences:
            return 0.0
        average = float(np.mean(confidences))
        return float(np.clip((average + 1.5) / 1.5, 0.0, 1.0))

    def _transcript_intensity(self, text: str) -> float:
        if not text.strip():
            return 0.0
        keywords = [
            "wow",
            "suka",
            "banget",
            "paling",
            "ternyata",
            "gak nyangka",
            "useful",
            "praktis",
            "cepet",
            "mudah",
            "bagus",
            "favorite",
            "best",
        ]
        return float(min(1.0, 0.18 + self._keyword_score(text, keywords) * 0.82))

    def _semantic_tags_for_window(
        self, text: str, metrics: dict[str, Any], motion: float, product_match: float
    ) -> list[str]:
        tags: list[str] = []
        lowered = text.lower()
        keyword_groups = {
            "problem": ["susah", "ribet", "capek", "before", "masalah", "repot"],
            "benefit": ["praktis", "useful", "hemat", "lebih", "bantu", "rapi", "cepat", "mudah"],
            "demo": ["pakai", "pake", "apply", "use", "pasang", "tuang", "coba"],
            "emotion": ["suka", "seneng", "happy", "puas", "banget", "love", "wow"],
            "cta": ["cek", "coba", "lihat", "beli", "ambil"],
            "cozy": ["cozy", "tenang", "morning", "night", "routine", "daily"],
            "satisfying": ["satisfying", "rapi", "bersih", "smooth", "restock"],
        }
        for tag, words in keyword_groups.items():
            if any(word in lowered for word in words):
                tags.append(tag)
        if product_match >= 0.8:
            tags.append("product")
        if metrics["face_score"] >= 0.08:
            tags.append("face")
        if metrics["hand_score"] >= 0.08:
            tags.append("hand")
        if motion >= 0.12:
            tags.append("action")
        if metrics["brightness"] >= 0.45 and metrics["saturation"] >= 0.20:
            tags.append("aesthetic")
        return sorted(set(tags))

    def _emotion_score(self, text: str, metrics: dict[str, Any], motion: float) -> float:
        keyword_score = self._keyword_score(
            text,
            ["seneng", "happy", "suka", "puas", "wow", "nagih", "banget", "love", "cute"],
        )
        visual_lift = min(1.0, metrics["brightness"] * 0.8 + metrics["saturation"] * 0.9 + motion * 0.4)
        return float(np.clip(keyword_score * 0.65 + visual_lift * 0.35, 0.0, 1.0))

    def _benefit_score(self, text: str, product_match: float) -> float:
        keyword_score = self._keyword_score(
            text,
            ["praktis", "useful", "berguna", "kepake", "hemat", "bantu", "rapi", "cepat", "mudah"],
        )
        return float(np.clip(keyword_score * 0.75 + product_match * 0.25, 0.0, 1.0))

    def _demo_score(self, text: str, metrics: dict[str, Any], motion: float, product_match: float) -> float:
        keyword_score = self._keyword_score(
            text,
            ["pakai", "pake", "coba", "pasang", "apply", "lihat", "demo", "use", "semprot"],
        )
        visual_component = min(1.0, motion * 2.0 + metrics["hand_score"] * 2.5 + product_match * 0.4)
        return float(np.clip(keyword_score * 0.55 + visual_component * 0.45, 0.0, 1.0))

    def _action_score(self, metrics: dict[str, Any], motion: float, text: str) -> float:
        transcript_push = self._keyword_score(text, ["jalan", "buka", "tutup", "angkat", "geser", "putar", "pakai"])
        return float(np.clip(motion * 1.7 + metrics["hand_score"] * 1.2 + transcript_push * 0.3, 0.0, 1.0))

    def _visual_score(self, metrics: dict[str, Any], motion: float, uniqueness: float) -> float:
        brightness_score = 1.0 - min(abs(metrics["brightness"] - 0.52) / 0.52, 1.0)
        motion_score = 1.0 - min(abs(motion - 0.14) / 0.18, 1.0)
        sharpness_score = min(metrics["sharpness"] / 260.0, 1.0)
        contrast_score = min(metrics["contrast"] / 0.28, 1.0)
        face_hand_score = min(1.0, metrics["face_score"] * 2.5 + metrics["hand_score"] * 2.0)
        return float(
            np.clip(
                brightness_score * 0.16
                + motion_score * 0.16
                + sharpness_score * 0.24
                + contrast_score * 0.16
                + metrics["saturation"] * 0.12
                + uniqueness * 0.08
                + face_hand_score * 0.08,
                0.0,
                1.0,
            )
        )

    def _audio_score(
        self,
        audio_features: AudioFeatures,
        timestamp: float,
        transcript_text: str,
        beat_alignment: float,
    ) -> float:
        transcript_push = self._transcript_intensity(transcript_text)
        voice_bonus = 0.18 if audio_features.has_voice else 0.0
        speech_bonus = min(audio_features.speech_density, 0.55)
        return float(np.clip(beat_alignment * 0.32 + transcript_push * 0.34 + voice_bonus + speech_bonus * 0.16, 0.0, 1.0))

    def _retention_risk(
        self,
        timestamp: float,
        visual_score: float,
        transcript_score: float,
        motion: float,
        uniqueness: float,
        product_match: float,
        mode: str,
    ) -> float:
        opening_penalty = 0.0
        if timestamp <= 2.0:
            if visual_score < 0.45:
                opening_penalty += 0.32
            if transcript_score < 0.20 and product_match < 0.55:
                opening_penalty += 0.20
        static_penalty = 0.20 if motion < 0.03 else 0.0
        repetition_penalty = 0.18 if uniqueness < 0.05 else 0.0
        generic = opening_penalty + static_penalty + repetition_penalty + max(0.0, 0.35 - visual_score) * 0.4
        if mode == "shopee" and product_match < 0.45:
            generic += 0.12
        return float(np.clip(generic, 0.0, 1.0))

    def _confidence_score(
        self, visual_score: float, semantic_score: float, audio_score: float, retention_risk: float
    ) -> float:
        return float(np.clip(visual_score * 0.35 + semantic_score * 0.40 + audio_score * 0.25 - retention_risk * 0.30, 0.0, 1.0))

    def _beat_alignment(self, timestamp: float, beat_times: list[float]) -> float:
        if not beat_times:
            return 0.4
        nearest = min(abs(timestamp - beat_time) for beat_time in beat_times)
        return float(np.clip(1.0 - nearest / 0.35, 0.0, 1.0))

    def _align_segment_to_beats(
        self, start: float, end: float, beat_times: list[float], settings: Settings
    ) -> tuple[float, float]:
        if not beat_times:
            return start, end
        aligned_start = self._nearest_beat(start, beat_times, tolerance=0.22)
        aligned_end = self._nearest_beat(end, beat_times, tolerance=0.22)
        if aligned_end - aligned_start < settings.min_segment_seconds:
            return start, end
        return aligned_start, aligned_end

    def _nearest_beat(self, value: float, beat_times: list[float], tolerance: float) -> float:
        nearest = min(beat_times, key=lambda beat: abs(beat - value))
        return nearest if abs(nearest - value) <= tolerance else value

    def _initial_storyboard_role(
        self,
        transcript_text: str,
        semantic_tags: list[str],
        product_match: float,
        emotion_score: float,
        demo_score: float,
    ) -> str:
        lowered = transcript_text.lower()
        if "problem" in semantic_tags:
            return "problem"
        if product_match >= 0.78 and demo_score >= 0.55:
            return "demo"
        if product_match >= 0.78:
            return "product"
        if emotion_score >= 0.62:
            return "peak"
        if any(token in lowered for token in ["pov", "ternyata", "gak nyangka", "awalnya"]):
            return "hook"
        if "benefit" in semantic_tags:
            return "benefit"
        if "cozy" in semantic_tags:
            return "lifestyle"
        return "highlight"

    def _platform_score(self, candidate: SegmentCandidate, mode: str) -> float:
        if mode == "tiktok":
            return float(
                np.clip(
                    candidate.visual_score * 0.24
                    + candidate.semantic_score * 0.22
                    + candidate.audio_score * 0.12
                    + candidate.uniqueness * 0.13
                    + candidate.emotion_score * 0.12
                    + candidate.action_score * 0.10
                    + candidate.beat_alignment * 0.07
                    - candidate.retention_risk * 0.18,
                    0.0,
                    1.0,
                )
            )
        return float(
            np.clip(
                candidate.visual_score * 0.16
                + candidate.semantic_score * 0.22
                + candidate.product_match * 0.18
                + candidate.benefit_score * 0.16
                + candidate.demo_score * 0.14
                + candidate.face_score * 0.06
                + candidate.transcript_score * 0.08
                - candidate.retention_risk * 0.14,
                0.0,
                1.0,
            )
        )

    def _detect_content_type(
        self, raw_videos: list[Path], analyses: list[ClipAnalysis], settings: Settings
    ) -> str:
        if any(analysis.detected_product for analysis in analyses):
            return "affiliate"
        product_filename_bias = max(
            (self._filename_detection_score(path.name, settings.product_keywords) for path in raw_videos),
            default=0.0,
        )
        return "affiliate" if product_filename_bias >= 0.5 else "lifestyle"

    def _detect_niche(self, analyses: list[ClipAnalysis], settings: Settings) -> str:
        text = " ".join(
            [analysis.niche for analysis in analyses]
            + [analysis.summary for analysis in analyses]
            + [analysis.transcript.full_text for analysis in analyses]
        )
        return self._niche_from_text(text, settings)

    def _niche_from_text(self, text: str, settings: Settings) -> str:
        lowered = text.lower()
        best_niche = "lifestyle"
        best_score = 0.0
        for niche, profile in settings.niche_profiles.items():
            score = self._keyword_score(lowered, profile.get("keywords", []))
            if score > best_score:
                best_score = score
                best_niche = niche
        return best_niche

    def _resolve_modes(self, settings: Settings, content_type: str) -> list[str]:
        if settings.mode == "tiktok":
            return ["tiktok"]
        if settings.mode == "shopee":
            return ["shopee"]
        return ["shopee"] if content_type == "affiliate" else ["tiktok"]

    def _build_render_plans(
        self,
        mode: str,
        content_type: str,
        niche: str,
        analyses: list[ClipAnalysis],
        memory: MemoryBundle,
        settings: Settings,
    ) -> list[RenderPlan]:
        plans: list[RenderPlan] = []
        variant_count = max(1, settings.variants_per_mode)
        for variant_index in range(variant_count):
            plan = self._build_render_plan(mode, variant_index, content_type, niche, analyses, memory, settings)
            if plan.selected_segments:
                plans.append(plan)
        return plans

    def _build_render_plan(
        self,
        mode: str,
        variant_index: int,
        content_type: str,
        niche: str,
        analyses: list[ClipAnalysis],
        memory: MemoryBundle,
        settings: Settings,
    ) -> RenderPlan:
        target_duration = self._target_duration(mode, niche, memory, settings)
        all_candidates = [candidate for analysis in analyses for candidate in analysis.candidates]
        ranked = self._rank_candidates_for_mode(all_candidates, mode, variant_index)
        selected = self._select_segments(ranked, target_duration, mode)
        hook = self._generate_hook(mode, variant_index, niche, analyses, memory, selected, settings)
        storyboard = self._build_storyboard(selected, mode, settings)
        subtitles = self._build_output_subtitles(selected, settings)
        music_path = self._pick_music(mode)
        retention_risks = self._render_plan_risks(selected, mode)
        confidence_score = float(np.mean([segment.confidence for segment in selected])) if selected else 0.0
        quality_band = self._quality_band(confidence_score)
        strategy_notes = self._strategy_notes(mode, variant_index, niche, selected)
        overlay_text = hook if settings.enable_text_overlay else None
        return RenderPlan(
            mode=mode,
            variant_index=variant_index,
            variant_name=f"v{variant_index + 1}",
            content_type=content_type,
            niche=niche,
            hook=hook,
            overlay_text=overlay_text,
            selected_segments=selected,
            target_duration=target_duration,
            music_path=music_path,
            subtitles=subtitles,
            storyboard=storyboard,
            retention_risks=retention_risks,
            confidence_score=round(confidence_score, 4),
            quality_band=quality_band,
            strategy_notes=strategy_notes,
        )

    def _target_duration(
        self, mode: str, niche: str, memory: MemoryBundle, settings: Settings
    ) -> float:
        niche_winner = memory.best_performance.get("learned_patterns", {}).get("niche_winners", {}).get(niche, {})
        if niche_winner and niche_winner.get("best_duration_seconds", {}).get(mode):
            return float(niche_winner["best_duration_seconds"][mode])
        best = memory.best_performance.get("best_duration_seconds", {}).get(mode)
        if best:
            return float(best)
        low, high = settings.tiktok_duration_seconds if mode == "tiktok" else settings.shopee_duration_seconds
        return float((low + high) / 2)

    def _rank_candidates_for_mode(
        self, candidates: list[SegmentCandidate], mode: str, variant_index: int
    ) -> list[SegmentCandidate]:
        ranked = list(candidates)
        if mode == "tiktok":
            if variant_index % 2 == 0:
                ranked.sort(
                    key=lambda candidate: (
                        candidate.mode_scores.get("tiktok", 0.0),
                        candidate.emotion_score,
                        candidate.beat_alignment,
                    ),
                    reverse=True,
                )
            else:
                ranked.sort(
                    key=lambda candidate: (
                        candidate.uniqueness,
                        candidate.action_score,
                        candidate.mode_scores.get("tiktok", 0.0),
                    ),
                    reverse=True,
                )
        else:
            if variant_index % 2 == 0:
                ranked.sort(
                    key=lambda candidate: (
                        candidate.mode_scores.get("shopee", 0.0),
                        candidate.product_match,
                        candidate.demo_score,
                    ),
                    reverse=True,
                )
            else:
                ranked.sort(
                    key=lambda candidate: (
                        candidate.benefit_score,
                        candidate.face_score,
                        candidate.mode_scores.get("shopee", 0.0),
                    ),
                    reverse=True,
                )
        return ranked

    def _select_segments(
        self,
        candidates: list[SegmentCandidate],
        target_duration: float,
        mode: str,
    ) -> list[SegmentCandidate]:
        selected: list[SegmentCandidate] = []
        total_duration = 0.0
        seen_signatures: set[tuple[str, int]] = set()
        seen_roles: set[str] = set()
        role_priority = (
            ["hook", "highlight", "peak", "loop"] if mode == "tiktok" else ["problem", "product", "demo", "benefit", "cta"]
        )

        for priority_role in role_priority:
            role_candidate = next(
                (
                    candidate
                    for candidate in candidates
                    if candidate.storyboard_role == priority_role
                    and (str(candidate.source_path), int(candidate.start * 10)) not in seen_signatures
                ),
                None,
            )
            if role_candidate is not None:
                selected.append(role_candidate)
                total_duration += role_candidate.duration
                seen_signatures.add((str(role_candidate.source_path), int(role_candidate.start * 10)))
                seen_roles.add(priority_role)
                if total_duration >= target_duration:
                    return selected

        for candidate in candidates:
            signature = (str(candidate.source_path), int(candidate.start * 10))
            if signature in seen_signatures:
                continue
            if any(
                segment.source_path == candidate.source_path and abs(segment.start - candidate.start) < 0.9
                for segment in selected
            ):
                continue
            if mode == "shopee" and not selected and candidate.product_match < 0.62:
                continue
            selected.append(candidate)
            total_duration += candidate.duration
            seen_signatures.add(signature)
            if mode == "tiktok" and len(selected) >= 9:
                break
            if mode == "shopee" and len(selected) >= 12:
                break
            if total_duration >= target_duration:
                break

        if mode == "shopee" and selected and selected[0].product_match < 0.62:
            selected.sort(key=lambda item: (item.product_match, item.demo_score, item.score), reverse=True)
        return selected

    def _generate_hook(
        self,
        mode: str,
        variant_index: int,
        niche: str,
        analyses: list[ClipAnalysis],
        memory: MemoryBundle,
        selected: list[SegmentCandidate],
        settings: Settings,
    ) -> str:
        transcript_candidates = [segment.transcript_text for segment in selected if segment.transcript_text]
        transcript_text = " ".join(transcript_candidates).strip()
        profile = settings.niche_profiles.get(niche, {})
        if mode == "tiktok" and profile.get("tiktok_hook"):
            niche_hook = profile["tiktok_hook"]
        elif mode == "shopee" and profile.get("shopee_hook"):
            niche_hook = profile["shopee_hook"]
        else:
            niche_hook = ""

        generated = self._contextual_hook_from_text(mode, transcript_text, selected)
        learned_hooks = memory.best_performance.get("tiktok_hooks" if mode == "tiktok" else "shopee_hooks", [])
        choices = [hook for hook in [generated, niche_hook, *learned_hooks] if hook]
        if not choices:
            return "POV: bagian kecil ini malah paling bikin betah nonton" if mode == "tiktok" else "awalnya iseng coba, ternyata kepake terus"
        return choices[variant_index % len(choices)]

    def _contextual_hook_from_text(
        self, mode: str, transcript_text: str, selected: list[SegmentCandidate]
    ) -> str:
        lowered = transcript_text.lower()
        top_tags = [tag for segment in selected for tag in segment.semantic_tags]
        if mode == "tiktok":
            if "satisfying" in top_tags:
                return "POV: bagian ini yang paling satisfying buat ditonton"
            if "cozy" in top_tags:
                return "day in my life, tapi bagian ini paling bikin tenang"
            if "product" in top_tags and "benefit" in top_tags:
                return "ini hal kecil yang bikin aku ulang terus"
            if lowered:
                snippet = self._first_phrase(lowered)
                return f"POV: {snippet}"
            return "POV: momen kecil ini ternyata paling bikin betah"
        if "benefit" in top_tags:
            return "ternyata yang simpel begini malah paling kepake"
        if "demo" in top_tags:
            return "awalnya iseng beli ini, ternyata enak dipake"
        if lowered:
            snippet = self._first_phrase(lowered)
            return f"gak nyangka {snippet}"
        return "awalnya iseng beli ini, ternyata kepake banget"

    def _first_phrase(self, text: str) -> str:
        cleaned = re.sub(r"\s+", " ", text).strip(" .,!?:;")
        if not cleaned:
            return "bagian ini bikin penasaran"
        words = cleaned.split()
        return " ".join(words[: min(len(words), 7)])

    def _build_storyboard(self, selected: list[SegmentCandidate], mode: str, settings: Settings) -> list[StoryboardStep]:
        if not selected:
            return []
        structure = settings.tiktok_structure if mode == "tiktok" else settings.shopee_structure
        storyboard: list[StoryboardStep] = []
        remaining = list(selected)
        for role in structure:
            match = next((segment for segment in remaining if segment.storyboard_role == role), None)
            if match is None and remaining:
                match = remaining[0]
            if match is None:
                continue
            storyboard.append(
                StoryboardStep(
                    role=role,
                    segment=match,
                    rationale=self._storyboard_rationale(role, match, mode),
                )
            )
            if match in remaining:
                remaining.remove(match)
        for segment in remaining:
            storyboard.append(
                StoryboardStep(
                    role=segment.storyboard_role,
                    segment=segment,
                    rationale="additional high-score support clip",
                )
            )
        return storyboard

    def _storyboard_rationale(self, role: str, segment: SegmentCandidate, mode: str) -> str:
        if mode == "tiktok":
            reasons = {
                "hook": "strong opening signal based on speech/emotion",
                "highlight": "fast-moving visual support clip",
                "peak": "highest emotional or satisfying moment",
                "loop": "closing clip chosen for replayability",
            }
        else:
            reasons = {
                "problem": "frames the use-case quickly",
                "lifestyle": "keeps the sell feeling natural",
                "product": "shows the item early and clearly",
                "demo": "demonstrates real usage",
                "benefit": "visual or verbal payoff moment",
                "cta": "soft closing prompt",
            }
        return reasons.get(role, f"selected for {role} coverage with score {segment.score:.2f}")

    def _build_output_subtitles(
        self, selected: list[SegmentCandidate], settings: Settings
    ) -> list[TranscriptSegment]:
        if not settings.enable_subtitles:
            return []
        subtitles: list[TranscriptSegment] = []
        cursor = 0.0
        for segment in selected:
            text = self._subtitle_text(segment.transcript_text, settings.subtitle_max_chars)
            if not text:
                cursor += segment.duration
                continue
            duration = min(max(segment.duration, 1.0), 3.2)
            subtitles.append(
                TranscriptSegment(
                    start=round(cursor, 3),
                    end=round(cursor + duration, 3),
                    text=text,
                    confidence=segment.transcript_score,
                )
            )
            cursor += segment.duration
        return subtitles

    def _subtitle_text(self, text: str, max_chars: int) -> str:
        cleaned = re.sub(r"\s+", " ", text).strip()
        if not cleaned:
            return ""
        if len(cleaned) <= max_chars:
            return cleaned
        cutoff = cleaned[:max_chars].rsplit(" ", maxsplit=1)[0].strip()
        return cutoff if cutoff else cleaned[:max_chars]

    def _pick_music(self, mode: str) -> Path | None:
        if not self.music_dir.exists():
            return None
        music_files = sorted(
            path for path in self.music_dir.iterdir() if path.is_file() and path.suffix.lower() in AUDIO_EXTENSIONS
        )
        if not music_files:
            return None
        if mode == "tiktok":
            for path in music_files:
                lowered = path.name.lower()
                if any(token in lowered for token in ["trend", "upbeat", "beat", "pop", "fast", "hype"]):
                    return path
        else:
            for path in music_files:
                lowered = path.name.lower()
                if any(token in lowered for token in ["calm", "soft", "warm", "acoustic", "lofi", "clean"]):
                    return path
        return music_files[0]

    def _render_plan(self, plan: RenderPlan, settings: Settings, temp_root: Path) -> dict[str, Any]:
        timestamp = datetime.now().strftime(DEFAULT_TIMESTAMP_FORMAT)
        output_dir = self.output_dir / plan.mode.upper()
        output_path = output_dir / f"video_{plan.mode}_{timestamp}_{plan.variant_name}.mp4"
        segment_dir = temp_root / f"{plan.mode}_{plan.variant_name}_segments"
        segment_dir.mkdir(parents=True, exist_ok=True)

        segment_paths: list[Path] = []
        speed_cycle = settings.tiktok_speed_cycle if plan.mode == "tiktok" else settings.shopee_speed_cycle
        for index, segment in enumerate(plan.selected_segments):
            segment_path = segment_dir / f"segment_{index:02d}.mp4"
            playback_speed = speed_cycle[index % len(speed_cycle)]
            self._render_segment(segment, segment_path, settings, playback_speed)
            if segment_path.exists():
                segment_paths.append(segment_path)

        if not segment_paths:
            raise RuntimeError(f"No segments rendered for {plan.mode} {plan.variant_name}.")

        base_concat = temp_root / f"{plan.mode}_{plan.variant_name}_base.mp4"
        concat_list = temp_root / f"{plan.mode}_{plan.variant_name}_concat.txt"
        concat_text = "\n".join(f"file '{self._escape_concat_path(path)}'" for path in segment_paths)
        concat_list.write_text(concat_text)
        self._run_command(
            [
                "ffmpeg",
                "-hide_banner",
                "-loglevel",
                "error",
                "-y",
                "-f",
                "concat",
                "-safe",
                "0",
                "-i",
                str(concat_list),
                "-c",
                "copy",
                str(base_concat),
            ]
        )

        subtitle_path = self._write_subtitle_file(plan, output_path)
        analysis_path = self._write_analysis_file(plan, output_path)
        self._finalize_video(base_concat, output_path, plan, settings, subtitle_path, temp_root)
        return {
            "mode": plan.mode,
            "variant": plan.variant_name,
            "content_type": plan.content_type,
            "niche": plan.niche,
            "output_path": str(output_path),
            "music_path": str(plan.music_path) if plan.music_path else None,
            "subtitle_path": str(subtitle_path) if subtitle_path else None,
            "analysis_path": str(analysis_path),
            "segments": [asdict(segment) for segment in plan.selected_segments],
            "hook": plan.hook,
            "confidence_score": plan.confidence_score,
            "quality_band": plan.quality_band,
            "retention_risks": plan.retention_risks,
            "storyboard": [asdict(step) for step in plan.storyboard],
            "strategy_notes": plan.strategy_notes,
        }

    def _render_segment(
        self,
        segment: SegmentCandidate,
        output_path: Path,
        settings: Settings,
        playback_speed: float,
    ) -> None:
        duration = max(segment.end - segment.start, settings.min_segment_seconds)
        eq_filter = (
            "eq="
            f"brightness={settings.brightness_boost}:"
            f"contrast={settings.contrast_boost}:"
            f"saturation={settings.saturation_boost}"
        )
        scale_w, scale_h, crop_x, crop_y = self._crop_parameters(segment, settings)
        filters = [f"scale={scale_w}:{scale_h}"]
        if settings.enable_deshake:
            filters.append("deshake")
        filters.extend(
            [
                f"crop={settings.target_width}:{settings.target_height}:{crop_x}:{crop_y}",
                eq_filter,
                f"unsharp=5:5:{settings.sharpen_strength}:3:3:0.30",
                f"setpts={1 / playback_speed:.5f}*PTS",
                "fps=30",
                "format=yuv420p",
            ]
        )
        cmd = [
            "ffmpeg",
            "-hide_banner",
            "-loglevel",
            "error",
            "-y",
            "-ss",
            str(segment.start),
            "-t",
            str(round(duration, 3)),
            "-i",
            str(segment.source_path),
            "-an",
            "-vf",
            ",".join(filters),
            "-c:v",
            "libx264",
            "-preset",
            settings.preset,
            "-crf",
            str(settings.crf),
            str(output_path),
        ]
        self._run_command(cmd)

    def _crop_parameters(
        self, segment: SegmentCandidate, settings: Settings
    ) -> tuple[int, int, int, int]:
        metadata = self._probe_video(segment.source_path)
        scale_factor = max(settings.target_width / metadata.width, settings.target_height / metadata.height)
        scaled_width = self._even_int(metadata.width * scale_factor)
        scaled_height = self._even_int(metadata.height * scale_factor)
        crop_x = int(np.clip(scaled_width * segment.safe_center_x - settings.target_width / 2, 0, max(scaled_width - settings.target_width, 0)))
        crop_y = int(np.clip(scaled_height * segment.safe_center_y - settings.target_height / 2, 0, max(scaled_height - settings.target_height, 0)))
        return scaled_width, scaled_height, crop_x, crop_y

    def _finalize_video(
        self,
        base_concat: Path,
        output_path: Path,
        plan: RenderPlan,
        settings: Settings,
        subtitle_path: Path | None,
        temp_root: Path,
    ) -> None:
        self._render_final_video(
            base_concat=base_concat,
            output_path=output_path,
            plan=plan,
            settings=settings,
            subtitle_path=subtitle_path,
            temp_root=temp_root,
            include_text=True,
        )

    def _render_final_video(
        self,
        base_concat: Path,
        output_path: Path,
        plan: RenderPlan,
        settings: Settings,
        subtitle_path: Path | None,
        temp_root: Path,
        include_text: bool,
    ) -> None:
        text_file = temp_root / f"{plan.mode}_{plan.variant_name}_hook.txt"
        if include_text and plan.overlay_text:
            text_file.write_text(plan.overlay_text)
        font_file = self._pick_font_file()

        filter_chain: list[str] = []
        if include_text and plan.overlay_text:
            drawtext = [
                "drawtext",
                f"textfile='{text_file}'",
                "fontcolor=white",
                f"fontsize={settings.hook_font_size}",
                "line_spacing=8",
                "box=1",
                "boxcolor=black@0.28",
                "boxborderw=18",
                "x=(w-text_w)/2",
                "y=h*0.11",
                "enable='between(t,0,2.4)'",
            ]
            if font_file:
                drawtext.insert(1, f"fontfile='{font_file}'")
            filter_chain.append("=".join([drawtext[0], ":".join(drawtext[1:])]))
        if settings.embed_subtitles_when_possible and subtitle_path:
            filter_chain.append(self._subtitle_drawtext_filter(subtitle_path, settings, font_file))

        cmd = ["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(base_concat)]
        if plan.music_path:
            cmd.extend(["-stream_loop", "-1", "-i", str(plan.music_path)])
            duration = self._probe_video(base_concat).duration
            audio_filter = f"[1:a]atrim=0:{duration:.3f},volume={1.0 - settings.audio_ducking},afade=t=out:st={max(duration - 1.0, 0.0):.3f}:d=1[a]"
            if filter_chain:
                video_filter = f"[0:v]{','.join(filter_chain)}[v]"
                cmd.extend(["-filter_complex", ";".join([video_filter, audio_filter]), "-map", "[v]", "-map", "[a]"])
            else:
                cmd.extend(["-filter_complex", audio_filter, "-map", "0:v", "-map", "[a]"])
        elif filter_chain:
            cmd.extend(["-vf", ",".join(filter_chain)])

        cmd.extend(
            [
                "-c:v",
                "libx264",
                "-preset",
                settings.preset,
                "-crf",
                str(settings.crf),
                "-shortest",
                str(output_path),
            ]
        )
        try:
            self._run_command(cmd)
        except RuntimeError as exc:
            message = str(exc)
            if include_text and "No such filter: 'drawtext'" in message:
                self._render_final_video(base_concat, output_path, plan, settings, subtitle_path, temp_root, include_text=False)
                return
            if "subtitles" in message and settings.embed_subtitles_when_possible:
                self._render_final_video(base_concat, output_path, plan, settings, None, temp_root, include_text=include_text)
                return
            raise

    def _subtitle_drawtext_filter(
        self, subtitle_path: Path, settings: Settings, font_file: Path | None
    ) -> str:
        subtitle_text = subtitle_path.read_text().splitlines()
        lines: list[str] = []
        current = []
        for line in subtitle_text:
            stripped = line.strip()
            if not stripped:
                continue
            if stripped.isdigit() or "-->" in stripped:
                continue
            current.append(stripped)
        collapsed = " | ".join(current[:3])
        overlay_path = subtitle_path.with_suffix(".overlay.txt")
        overlay_path.write_text(collapsed)
        drawtext = [
            "drawtext",
            f"textfile='{overlay_path}'",
            "fontcolor=white",
            f"fontsize={settings.subtitle_font_size}",
            "box=1",
            "boxcolor=black@0.34",
            "boxborderw=12",
            "x=(w-text_w)/2",
            "y=h*0.82",
        ]
        if font_file:
            drawtext.insert(1, f"fontfile='{font_file}'")
        return "=".join([drawtext[0], ":".join(drawtext[1:])])

    def _pick_font_file(self) -> Path | None:
        if self.font_dir.exists():
            for path in sorted(self.font_dir.iterdir()):
                if path.is_file() and path.suffix.lower() in {".ttf", ".otf", ".ttc"}:
                    return path
        fallback = Path("/System/Library/Fonts/Supplemental/Arial Unicode.ttf")
        return fallback if fallback.exists() else None

    def _write_subtitle_file(self, plan: RenderPlan, output_path: Path) -> Path | None:
        if not plan.subtitles:
            return None
        subtitle_path = self.subtitle_dir / f"{output_path.stem}.srt"
        chunks: list[str] = []
        for index, subtitle in enumerate(plan.subtitles, start=1):
            chunks.append(str(index))
            chunks.append(
                f"{self._format_srt_time(subtitle.start)} --> {self._format_srt_time(subtitle.end)}"
            )
            chunks.append(subtitle.text)
            chunks.append("")
        subtitle_path.write_text("\n".join(chunks))
        return subtitle_path

    def _write_analysis_file(self, plan: RenderPlan, output_path: Path) -> Path:
        analysis_path = self.analysis_dir / f"{output_path.stem}.json"
        payload = {
            "mode": plan.mode,
            "variant": plan.variant_name,
            "niche": plan.niche,
            "hook": plan.hook,
            "confidence_score": plan.confidence_score,
            "quality_band": plan.quality_band,
            "retention_risks": plan.retention_risks,
            "strategy_notes": plan.strategy_notes,
            "storyboard": [asdict(step) for step in plan.storyboard],
            "segments": [asdict(segment) for segment in plan.selected_segments],
        }
        analysis_path.write_text(json.dumps(self._json_ready(payload), indent=2))
        return analysis_path

    def _render_plan_risks(self, selected: list[SegmentCandidate], mode: str) -> list[str]:
        risks: list[str] = []
        if not selected:
            return ["no_segments"]
        first = selected[0]
        if first.retention_risk >= 0.45:
            risks.append("opening_may_be_too_soft")
        if mode == "shopee" and first.product_match < 0.62:
            risks.append("product_not_clear_in_opening")
        if mode == "tiktok" and first.action_score < 0.35 and first.emotion_score < 0.35:
            risks.append("first_two_seconds_need_more_pattern_interrupt")
        if np.mean([segment.uniqueness for segment in selected]) < 0.08:
            risks.append("sequence_feels_repetitive")
        if np.mean([segment.visual_score for segment in selected]) < 0.48:
            risks.append("visual_energy_is_moderate")
        return risks

    def _quality_band(self, confidence_score: float) -> str:
        if confidence_score >= 0.78:
            return "high"
        if confidence_score >= 0.58:
            return "medium"
        return "low"

    def _strategy_notes(self, mode: str, variant_index: int, niche: str, selected: list[SegmentCandidate]) -> list[str]:
        notes = [f"niche={niche}", f"variant={variant_index + 1}"]
        if mode == "tiktok":
            notes.append("optimized for retention with faster pattern changes")
            if any(segment.beat_alignment >= 0.7 for segment in selected):
                notes.append("opening and cuts aligned toward detected beat peaks")
        else:
            notes.append("optimized for conversion with earlier product/demo emphasis")
            if any(segment.benefit_score >= 0.6 for segment in selected):
                notes.append("benefit-led clip ordering applied")
        if any(segment.transcript_text for segment in selected):
            notes.append("hook and subtitles adapted from transcribed speech")
        return notes

    def _learn_from_run(
        self,
        outputs: list[dict[str, Any]],
        analyses: list[ClipAnalysis],
        memory: MemoryBundle,
        settings: Settings,
    ) -> dict[str, Any]:
        if not settings.learning_enabled:
            return {"learning_enabled": False}

        best_path = self.memory_dir / "best_performance.json"
        style_path = self.memory_dir / "style_profile.json"
        history_runs = memory.history_log.get("runs", [])
        learned_patterns = memory.best_performance.setdefault("learned_patterns", {})
        niche_winners = learned_patterns.setdefault("niche_winners", {})

        for output in outputs:
            mode = output["mode"]
            niche = output["niche"]
            hooks_key = "tiktok_hooks" if mode == "tiktok" else "shopee_hooks"
            existing_hooks = memory.best_performance.setdefault(hooks_key, [])
            if output["hook"] not in existing_hooks:
                existing_hooks.insert(0, output["hook"])
                memory.best_performance[hooks_key] = existing_hooks[:8]

            target_key = memory.best_performance.setdefault("best_duration_seconds", {})
            segment_duration = sum(segment["duration"] for segment in output["segments"])
            target_key[mode] = round((float(target_key.get(mode, segment_duration)) + segment_duration) / 2, 2)

            niche_entry = niche_winners.setdefault(niche, {"best_duration_seconds": {}, "hooks": {}})
            previous_duration = niche_entry["best_duration_seconds"].get(mode, segment_duration)
            niche_entry["best_duration_seconds"][mode] = round((previous_duration + segment_duration) / 2, 2)
            niche_entry["hooks"][mode] = output["hook"]

        platform_weights = memory.style_profile.setdefault("platform_weights", {})
        for mode in ["tiktok", "shopee"]:
            mode_outputs = [output for output in outputs if output["mode"] == mode]
            if not mode_outputs:
                continue
            avg_conf = float(np.mean([output["confidence_score"] for output in mode_outputs]))
            weights = platform_weights.setdefault(
                mode,
                {"visual": 0.25, "audio": 0.15, "semantic": 0.25, "hook": 0.20, "retention_penalty": 0.15},
            )
            if avg_conf < settings.confidence_low_threshold:
                weights["semantic"] = round(min(weights["semantic"] + 0.03, 0.50), 3)
                weights["retention_penalty"] = round(min(weights["retention_penalty"] + 0.02, 0.25), 3)
            else:
                weights["hook"] = round(min(weights["hook"] + 0.02, 0.30), 3)

        memory.best_performance["learned_patterns"] = learned_patterns
        style_path.write_text(json.dumps(self._json_ready(memory.style_profile), indent=2))
        best_path.write_text(json.dumps(self._json_ready(memory.best_performance), indent=2))

        feedback_ready = any(
            run.get("engagement_score") is not None or run.get("conversion_score") is not None
            for run in history_runs
        )
        return {
            "learning_enabled": True,
            "outputs_learned": len(outputs),
            "history_runs_seen": len(history_runs),
            "feedback_ready": feedback_ready,
            "patterns_updated": list(niche_winners.keys())[:8],
        }

    def _filename_detection_score(self, filename: str, keywords: Iterable[str]) -> float:
        lowered = filename.lower()
        hits = sum(1 for keyword in keywords if keyword in lowered)
        if not hits:
            return 0.0
        return float(min(hits / 2.0, 1.0))

    def _keyword_score(self, text: str, keywords: Iterable[str]) -> float:
        lowered = text.lower()
        hits = sum(1 for keyword in keywords if keyword in lowered)
        return float(min(hits / 3.0, 1.0))

    def _average_hash(self, image: Image.Image, size: int = 8) -> np.ndarray:
        reduced = image.convert("L").resize((size, size))
        values = np.asarray(reduced, dtype=np.float32)
        mean = float(np.mean(values))
        return values > mean

    def _format_srt_time(self, value: float) -> str:
        total_ms = int(round(value * 1000))
        hours = total_ms // 3_600_000
        minutes = (total_ms % 3_600_000) // 60_000
        seconds = (total_ms % 60_000) // 1000
        milliseconds = total_ms % 1000
        return f"{hours:02d}:{minutes:02d}:{seconds:02d},{milliseconds:03d}"

    def _escape_concat_path(self, path: Path) -> str:
        return str(path).replace("'", r"'\''")

    def _even_int(self, value: float) -> int:
        integer = int(value)
        return integer if integer % 2 == 0 else integer + 1

    def _json_ready(self, value: Any) -> Any:
        if isinstance(value, Path):
            return str(value)
        if isinstance(value, dict):
            return {key: self._json_ready(item) for key, item in value.items()}
        if isinstance(value, list):
            return [self._json_ready(item) for item in value]
        return value

    def _run_command(self, cmd: list[str], capture_output: bool = False) -> str:
        result = subprocess.run(cmd, check=False, capture_output=True, text=True)
        if result.returncode != 0:
            stderr = result.stderr.strip() if result.stderr else "unknown error"
            raise RuntimeError(f"Command failed: {' '.join(cmd)}\n{stderr}")
        return result.stdout if capture_output else ""
