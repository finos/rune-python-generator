#!/usr/bin/env python3
#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#
"""
Profile memory usage of importing CDM TradeState.

Outputs:
  - Process RSS before/after import (requires psutil)
  - tracemalloc allocation breakdown by file and by top-level package
  - Count and size of Pydantic BaseModel subclasses loaded
  - Number of modules imported

Usage (from project root, with CDM venv activated):
  python python-test/cdm-tests/profile_import.py [--top N]

Options:
  --top N   Show top N allocating files (default: 30)
"""

import argparse
import gc
import sys
import time

import psutil

_PROC = psutil.Process()


def rss_mb() -> float:
    return _PROC.memory_info().rss / 1024 / 1024


def _count_pydantic_models():
    try:
        import pydantic
        base = pydantic.BaseModel
    except ImportError:
        return 0
    count = 0
    for obj in gc.get_objects():
        try:
            if isinstance(obj, type) and issubclass(obj, base) and obj is not base:
                count += 1
        except TypeError:
            pass
    return count


def main():
    parser = argparse.ArgumentParser(description=__doc__,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--top", type=int, default=15,
                        help="Top N slowest model_rebuild calls to show (default: 15)")
    args = parser.parse_args()

    sep = "=" * 72

    # ── baseline ──────────────────────────────────────────────────────────
    gc.collect()
    modules_before = set(sys.modules.keys())
    rss_before = rss_mb()

    # Instrument model_rebuild to capture per-class timing
    import pydantic
    rebuild_times = []
    _orig = pydantic.BaseModel.model_rebuild.__func__

    def _timed(cls, **kwargs):
        t0 = time.perf_counter()
        result = _orig(cls, **kwargs)
        rebuild_times.append((time.perf_counter() - t0, cls.__name__))
        return result

    pydantic.BaseModel.model_rebuild = classmethod(_timed)

    # ── import ────────────────────────────────────────────────────────────
    print("Importing TradeState …", flush=True)
    t_start = time.perf_counter()
    from finos.cdm.event.common.TradeState import TradeState  # noqa: F401
    t_total = time.perf_counter() - t_start
    print("Import done.", flush=True)

    # ── snapshot ──────────────────────────────────────────────────────────
    gc.collect()
    rss_after = rss_mb()
    modules_after = set(sys.modules.keys())
    model_count = _count_pydantic_models()

    total_rebuild = sum(t for t, _ in rebuild_times)

    # ── report ────────────────────────────────────────────────────────────
    print(f"\n{sep}")
    print("MEMORY SUMMARY")
    print(sep)
    print(f"  RSS before import     : {rss_before:.1f} MB")
    print(f"  RSS after import      : {rss_after:.1f} MB")
    print(f"  RSS delta             : {rss_after - rss_before:.1f} MB")
    print(f"  Modules imported      : {len(modules_after - modules_before)}")
    print(f"  Pydantic models loaded: {model_count}")

    print(f"\n{sep}")
    print("TIMING SUMMARY")
    print(sep)
    print(f"  Total import time     : {t_total:.1f}s")
    print(f"  Time in model_rebuild : {total_rebuild:.1f}s  ({total_rebuild/t_total*100:.0f}% of total)")
    print(f"  model_rebuild calls   : {len(rebuild_times)}")
    print(f"  Average per rebuild   : {total_rebuild/len(rebuild_times)*1000:.1f}ms" if rebuild_times else "")

    print(f"\n{sep}")
    print(f"TOP {args.top} SLOWEST model_rebuild CALLS")
    print(sep)
    for elapsed, name in sorted(rebuild_times, reverse=True)[: args.top]:
        print(f"  {elapsed*1000:7.1f}ms  {name}")

    print(f"\n{sep}\n")


if __name__ == "__main__":
    main()
