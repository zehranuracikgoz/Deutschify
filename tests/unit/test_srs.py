import sys
import os
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '../..')))

from backend.srs import calculate_next_review


def test_quality_5_ef_increases_and_interval_grows():
    result = calculate_next_review(ease_factor=2.5, interval_days=6, repetition_count=2, quality=5)
    assert result["ease_factor"] > 2.5
    assert result["interval_days"] > 6


def test_quality_0_resets_repetition_and_interval():
    result = calculate_next_review(ease_factor=2.5, interval_days=10, repetition_count=4, quality=0)
    assert result["repetition_count"] == 0
    assert result["interval_days"] == 1


def test_quality_3_repetition_1_gives_interval_6():
    result = calculate_next_review(ease_factor=2.5, interval_days=1, repetition_count=1, quality=3)
    assert result["interval_days"] == 6