from __future__ import annotations

import argparse
from pathlib import Path

from .pipeline import VideoEditingPipeline


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Automated short-form editing pipeline for TikTok and Shopee."
    )
    parser.add_argument(
        "--project-root",
        type=Path,
        default=Path.cwd(),
        help="Project root containing INPUT, OUTPUT, AUDIO, ASSETS, MEMORY, and CONFIG.",
    )
    parser.add_argument(
        "--mode",
        choices=["auto", "tiktok", "shopee"],
        default=None,
        help="Optional override for CONFIG/settings.json mode.",
    )
    parser.add_argument(
        "--both",
        action="store_true",
        help="Force generating both TikTok and Shopee versions when possible.",
    )
    return parser


def main() -> None:
    parser = build_parser()
    args = parser.parse_args()
    pipeline = VideoEditingPipeline(args.project_root)
    pipeline.run(mode_override=args.mode, force_both=args.both)


if __name__ == "__main__":
    main()
