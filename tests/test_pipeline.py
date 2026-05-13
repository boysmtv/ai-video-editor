from __future__ import annotations

import json
import sys
import tempfile
import unittest
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(PROJECT_ROOT / "src"))

from ai_video_editor.pipeline import SegmentCandidate, Settings, TranscriptSegment, VideoEditingPipeline


class PipelineTests(unittest.TestCase):
    def test_settings_loads_defaults_and_overrides(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_name:
            root = Path(tmp_name)
            settings_path = root / "settings.json"
            settings_path.write_text(json.dumps({"mode": "shopee", "crf": 18}))
            settings = Settings.from_file(settings_path)
            self.assertEqual(settings.mode, "shopee")
            self.assertEqual(settings.crf, 18)
            self.assertTrue(settings.generate_both_versions)

    def test_resolve_modes(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_name:
            root = Path(tmp_name)
            pipeline = VideoEditingPipeline(root)
            settings = Settings(mode="auto")
            self.assertEqual(pipeline._resolve_modes(settings, "affiliate"), ["shopee"])
            self.assertEqual(pipeline._resolve_modes(settings, "lifestyle"), ["tiktok"])

    def test_filename_detection_score(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_name:
            root = Path(tmp_name)
            pipeline = VideoEditingPipeline(root)
            score = pipeline._filename_detection_score(
                "daily_shopee_product_review.mp4",
                ["shopee", "product", "review"],
            )
            self.assertGreaterEqual(score, 1.0)

    def test_detect_niche_from_text(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_name:
            root = Path(tmp_name)
            pipeline = VideoEditingPipeline(root)
            settings = Settings()
            niche = pipeline._niche_from_text("serum skincare morning routine", settings)
            self.assertEqual(niche, "skincare")

    def test_build_output_subtitles(self) -> None:
        with tempfile.TemporaryDirectory() as tmp_name:
            root = Path(tmp_name)
            pipeline = VideoEditingPipeline(root)
            settings = Settings(enable_subtitles=True)
            candidate = SegmentCandidate(
                source_path=root / "a.mp4",
                start=0.0,
                end=2.0,
                duration=2.0,
                score=0.8,
                visual_score=0.8,
                audio_score=0.5,
                semantic_score=0.7,
                product_match=0.6,
                product_confidence=0.6,
                sharpness=10.0,
                brightness=0.5,
                contrast=0.3,
                saturation=0.2,
                motion=0.1,
                uniqueness=0.3,
                face_score=0.1,
                hand_score=0.1,
                action_score=0.4,
                emotion_score=0.6,
                benefit_score=0.5,
                demo_score=0.4,
                transcript_score=0.7,
                beat_alignment=0.8,
                shot_boundary_score=0.3,
                spoken_content_score=0.5,
                object_focus_score=0.6,
                scene_index=1,
                retention_risk=0.2,
                confidence=0.75,
                safe_center_x=0.5,
                safe_center_y=0.45,
                semantic_tags=["benefit"],
                transcript_text="awalnya iseng beli ini ternyata kepake banget setiap hari",
                storyboard_role="hook",
                mode_scores={"tiktok": 0.7, "shopee": 0.8},
                note="quality_pass",
            )
            subtitles = pipeline._build_output_subtitles([candidate], settings)
            self.assertEqual(len(subtitles), 1)
            self.assertIsInstance(subtitles[0], TranscriptSegment)
            self.assertLessEqual(len(subtitles[0].text), settings.subtitle_max_chars)


if __name__ == "__main__":
    unittest.main()
