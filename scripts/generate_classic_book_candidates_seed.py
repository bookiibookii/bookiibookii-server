#!/usr/bin/env python3
"""
Generate a one-time MySQL seed file for classic book candidates.

Run from the project root:

    export ALADIN_TTB_KEY=your-key
    python3 scripts/generate_classic_book_candidates_seed.py

The script only writes SQL to scripts/output/classic_book_candidates_seed.sql.
It does not connect to or modify a database.
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Any


PROJECT_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CSV_PATH = PROJECT_ROOT / "src/main/resources/aladin_classic_cids.csv"
DEFAULT_OUTPUT_PATH = (
    PROJECT_ROOT / "scripts/output/classic_book_candidates_seed.sql"
)
ALADIN_ITEM_LIST_URL = "https://www.aladin.co.kr/ttb/api/ItemList.aspx"
SECTION_TYPE = "CLASSIC_BOOK_GROUP"
SOURCE_TYPE = "ALADIN_CATEGORY_SEED"


@dataclass(frozen=True)
class BookCandidate:
    isbn13: str
    title: str
    author: str
    item_id: int
    source_cid: int
    display_order: int


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate classic book candidate INSERT SQL from Aladin CIDs."
    )
    parser.add_argument(
        "--csv",
        type=Path,
        default=DEFAULT_CSV_PATH,
        help=f"CID CSV path (default: {DEFAULT_CSV_PATH})",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT_PATH,
        help=f"SQL output path (default: {DEFAULT_OUTPUT_PATH})",
    )
    parser.add_argument(
        "--max-pages",
        type=int,
        default=20,
        help="Maximum pages fetched per CID (default: 20)",
    )
    parser.add_argument(
        "--sleep",
        type=float,
        default=0.2,
        help="Seconds to wait between API calls (default: 0.2)",
    )
    parser.add_argument(
        "--timeout",
        type=float,
        default=15.0,
        help="HTTP request timeout in seconds (default: 15)",
    )
    return parser.parse_args()


def read_active_classic_cids(csv_path: Path) -> list[int]:
    if not csv_path.is_file():
        raise FileNotFoundError(f"CID CSV not found: {csv_path}")

    cids: list[int] = []
    seen: set[int] = set()
    with csv_path.open("r", encoding="utf-8-sig", newline="") as csv_file:
        reader = csv.DictReader(csv_file)
        required_columns = {"section_type", "cid", "active"}
        missing_columns = required_columns.difference(reader.fieldnames or [])
        if missing_columns:
            missing = ", ".join(sorted(missing_columns))
            raise ValueError(f"CID CSV is missing required columns: {missing}")

        for line_number, row in enumerate(reader, start=2):
            if row["section_type"].strip() != SECTION_TYPE:
                continue
            if row["active"].strip().lower() not in {"true", "1", "yes", "y"}:
                continue

            try:
                cid = int(row["cid"].strip())
            except (TypeError, ValueError) as error:
                raise ValueError(
                    f"Invalid cid at {csv_path}:{line_number}: {row['cid']!r}"
                ) from error

            if cid > 0 and cid not in seen:
                cids.append(cid)
                seen.add(cid)

    if not cids:
        raise ValueError(f"No active {SECTION_TYPE} CIDs found in {csv_path}")
    return cids


def fetch_page(
    ttb_key: str,
    cid: int,
    start: int,
    timeout: float,
    max_attempts: int = 3,
) -> list[dict[str, Any]]:
    params = urllib.parse.urlencode(
        {
            "ttbkey": ttb_key,
            "QueryType": "Bestseller",
            "SearchTarget": "Book",
            "CategoryId": cid,
            "output": "js",
            "Version": "20131101",
            "MaxResults": 50,
            "start": start,
        }
    )
    url = f"{ALADIN_ITEM_LIST_URL}?{params}"

    for attempt in range(1, max_attempts + 1):
        try:
            request = urllib.request.Request(
                url,
                headers={
                    "Accept": "application/json",
                    "User-Agent": "bookiibookii-classic-seed-generator/1.0",
                },
            )
            with urllib.request.urlopen(request, timeout=timeout) as response:
                payload = json.loads(response.read().decode("utf-8-sig"))

            if not isinstance(payload, dict):
                raise RuntimeError("Aladin API returned a non-object JSON response")
            if payload.get("errorCode") or payload.get("errorMessage"):
                raise RuntimeError(
                    "Aladin API error "
                    f"{payload.get('errorCode', '')}: "
                    f"{payload.get('errorMessage', 'unknown error')}"
                )

            items = payload.get("item", [])
            if items is None:
                return []
            if not isinstance(items, list):
                raise RuntimeError("Aladin API response field 'item' is not a list")
            return [item for item in items if isinstance(item, dict)]
        except (
            urllib.error.URLError,
            TimeoutError,
            json.JSONDecodeError,
        ) as error:
            if attempt == max_attempts:
                raise RuntimeError(
                    f"Failed to fetch cid={cid}, start={start} "
                    f"after {max_attempts} attempts: {error}"
                ) from error
            wait_seconds = attempt
            print(
                f"  retrying cid={cid}, start={start} in {wait_seconds}s: {error}",
                file=sys.stderr,
            )
            time.sleep(wait_seconds)

    raise AssertionError("unreachable")


def normalize_isbn13(value: Any) -> str | None:
    isbn13 = "".join(character for character in str(value or "") if character.isdigit())
    return isbn13 if len(isbn13) == 13 else None


def collect_candidates(
    cids: list[int],
    ttb_key: str,
    max_pages: int,
    sleep_seconds: float,
    timeout: float,
) -> list[BookCandidate]:
    # Keep the first occurrence so source_cid and display_order are deterministic
    # according to CSV CID order, page order, and Aladin result order.
    candidates_by_isbn: dict[str, BookCandidate] = {}
    skipped = 0
    request_count = 0

    for cid_index, cid in enumerate(cids, start=1):
        print(f"[{cid_index}/{len(cids)}] Fetching cid={cid}")
        for page in range(1, max_pages + 1):
            if request_count > 0:
                time.sleep(sleep_seconds)
            items = fetch_page(ttb_key, cid, page, timeout)
            request_count += 1
            print(f"  page={page}: {len(items)} item(s)")
            if not items:
                break

            for item in items:
                isbn13 = normalize_isbn13(item.get("isbn13"))
                try:
                    item_id = int(item.get("itemId"))
                except (TypeError, ValueError):
                    item_id = 0

                if not isbn13 or item_id <= 0:
                    skipped += 1
                    continue
                if isbn13 in candidates_by_isbn:
                    continue

                display_order = len(candidates_by_isbn) + 1
                candidates_by_isbn[isbn13] = BookCandidate(
                    isbn13=isbn13,
                    title=str(item.get("title") or "").strip(),
                    author=str(item.get("author") or "").strip(),
                    item_id=item_id,
                    source_cid=cid,
                    display_order=display_order,
                )

    print(
        f"Collected {len(candidates_by_isbn)} unique book(s); "
        f"skipped {skipped} item(s) without a valid isbn13/itemId."
    )
    return list(candidates_by_isbn.values())


def mysql_string(value: str) -> str:
    """Return a MySQL string literal safe under the default SQL mode."""
    escaped = (
        value.replace("\\", "\\\\")
        .replace("\0", "\\0")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\x1a", "\\Z")
        .replace("'", "''")
    )
    return f"'{escaped}'"


def render_sql(candidates: list[BookCandidate], csv_path: Path) -> str:
    lines = [
        "-- Generated by scripts/generate_classic_book_candidates_seed.py",
        f"-- Source CID file: {csv_path.as_posix()}",
        f"-- Unique ISBN13 candidates: {len(candidates)}",
        "-- Duplicate policy: keep the first occurrence in CSV/page/result order.",
        "",
    ]

    if candidates:
        lines.extend(
            [
                "INSERT INTO home_section_book_candidates",
                "(",
                "    section_type,",
                "    isbn13,",
                "    title,",
                "    author,",
                "    aladin_item_id,",
                "    source_cid,",
                "    source_type,",
                "    display_order,",
                "    active,",
                "    created_at,",
                "    updated_at",
                ")",
                "VALUES",
            ]
        )

        value_rows = []
        for candidate in candidates:
            value_rows.append(
                "    ("
                f"{mysql_string(SECTION_TYPE)}, "
                f"{mysql_string(candidate.isbn13)}, "
                f"{mysql_string(candidate.title)}, "
                f"{mysql_string(candidate.author)}, "
                f"{candidate.item_id}, "
                f"{candidate.source_cid}, "
                f"{mysql_string(SOURCE_TYPE)}, "
                f"{candidate.display_order}, "
                "TRUE, NOW(), NOW())"
            )
        lines.append(",\n".join(value_rows))
        lines.extend(
            [
                "ON DUPLICATE KEY UPDATE",
                "    title = VALUES(title),",
                "    author = VALUES(author),",
                "    aladin_item_id = VALUES(aladin_item_id),",
                "    source_cid = VALUES(source_cid),",
                "    source_type = VALUES(source_type),",
                "    display_order = VALUES(display_order),",
                "    active = VALUES(active),",
                "    updated_at = NOW();",
                "",
            ]
        )
    else:
        lines.extend(["-- No valid candidates were returned; no INSERT generated.", ""])

    lines.extend(
        [
            "-- Verification queries",
            "SELECT COUNT(*)",
            "FROM home_section_book_candidates",
            f"WHERE section_type = {mysql_string(SECTION_TYPE)}",
            "  AND active = TRUE;",
            "",
            (
                "SELECT id, section_type, isbn13, title, author, source_cid, "
                "display_order, active"
            ),
            "FROM home_section_book_candidates",
            f"WHERE section_type = {mysql_string(SECTION_TYPE)}",
            "ORDER BY display_order ASC",
            "LIMIT 50;",
            "",
        ]
    )
    return "\n".join(lines)


def main() -> int:
    args = parse_args()
    if args.max_pages < 1 or args.max_pages > 20:
        raise ValueError("--max-pages must be between 1 and 20")
    if args.sleep < 0:
        raise ValueError("--sleep must be zero or greater")
    if args.timeout <= 0:
        raise ValueError("--timeout must be greater than zero")

    ttb_key = os.environ.get("ALADIN_TTB_KEY", "").strip()
    if not ttb_key:
        print(
            "ALADIN_TTB_KEY is required. "
            "Set it before running this script.",
            file=sys.stderr,
        )
        return 2

    csv_path = args.csv.resolve()
    output_path = args.output.resolve()
    cids = read_active_classic_cids(csv_path)
    print(f"Loaded {len(cids)} active classic CID(s) from {csv_path}")

    candidates = collect_candidates(
        cids=cids,
        ttb_key=ttb_key,
        max_pages=args.max_pages,
        sleep_seconds=args.sleep,
        timeout=args.timeout,
    )
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(render_sql(candidates, csv_path), encoding="utf-8")
    print(f"Wrote SQL seed file: {output_path}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (FileNotFoundError, ValueError, RuntimeError) as error:
        print(f"Error: {error}", file=sys.stderr)
        raise SystemExit(1)
