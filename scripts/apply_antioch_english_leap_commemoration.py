#!/usr/bin/env python3
"""Promote the native English 02-29 title from an official Antioch source.

Jerusalem's official Arabic and Greek leap-year calendars establish the local
Old-Calendar date.  Antioch's official native-English saint page supplies the
English title (and shows the ordinary-year observance on 28 February).  No
translation or cross-language text generation is performed here.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "canonical" / "jerusalem_jordan_fixed_commemorations_native.json"

SOURCE_ID = "antioch_patriarchate_john_cassian_en"
SOURCE_URL = "https://antiochpatriarchate.org/en/saints/righteous-john-cassian-the-confessor/"
SOURCE_SHA256 = "5f1669aff0e0216650dcd47431f9be927673569917a4568f25bd10d16ba3516c"


def main() -> None:
    payload = json.loads(EVIDENCE.read_text(encoding="utf-8"))
    artifacts = payload.setdefault("source_artifacts", [])
    artifacts[:] = [item for item in artifacts if item.get("source_id") != SOURCE_ID]
    artifacts.append({
        "source_id": SOURCE_ID,
        "language": "en",
        "source_year": 2026,
        "jurisdiction": "antioch_patriarchate",
        "source_url": SOURCE_URL,
        "artifact_sha256": SOURCE_SHA256,
        "artifact_available_at_build_time": True,
        "promotion": "native_english_title_for_jerusalem_leap_slot",
        "notes": (
            "Official Greek Orthodox Patriarchate of Antioch native-English page. "
            "The title is used only for the English lane; Jerusalem's official "
            "Arabic and Greek 2024 calendars independently establish 02-29."
        ),
    })
    record = next(
        row for row in payload["records"]
        if row.get("old_calendar_month_day") == "02-29"
    )
    record["lanes"]["en"] = {
        "text": "Righteous John Cassian the Confessor",
        "source_id": SOURCE_ID,
        "source_url": SOURCE_URL,
        "source_year": 2026,
        "jurisdiction": "antioch_patriarchate",
        "evidence_status": "VERIFIED_NATIVE_ORTHODOX_ENGLISH_SOURCE",
        "comparative": False,
        "fixed_slot_eligible": True,
        "eligibility_reason": "OFFICIAL_ANTIOCH_NATIVE_ENGLISH_TITLE_WITH_JERUSALEM_LEAP_DATE_EVIDENCE",
        "source_endpoint": SOURCE_URL,
        "ordinary_year_display_date": "28 February",
        "date_assignment_basis": "JERUSALEM_ARABIC_AND_GREEK_LEAP_CALENDARS_2024",
        "supporting_local_source_ids": [
            "jerusalem_hagiotaphite_calendar_ar_2024",
            "jerusalem_hagiotaphite_calendar_el_2024",
        ],
    }
    coverage = payload["coverage"]
    coverage["en_comparative_records"] = 0
    coverage["en_official_orthodox_cross_jurisdiction_records"] = 1
    coverage["strict_named_local_gate"] = False
    coverage["strict_gate_note"] = (
        "All 366 fixed slots have native Arabic, English, and Greek evidence. "
        "For 02-29, Jerusalem Arabic and Greek leap calendars establish the date "
        "and the official Antioch English page supplies the native English title."
    )
    coverage["en_local_english_timetable_2019_pending_slots"] = 0
    payload["strict_named_local_gate"] = (
        "requires_verified_native_official_ar_en_el_for_same_slot; "
        "cross-jurisdiction English is accepted only with independent Jerusalem leap-date evidence"
    )
    EVIDENCE.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("ANTIOCH_ENGLISH_LEAP_COMMEMORATION_APPLIED")


if __name__ == "__main__":
    main()
