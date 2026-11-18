import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def test_old_calendar_leap_slot_has_three_independent_official_native_lanes():
    payload = json.loads(
        (ROOT / "canonical/jerusalem_jordan_fixed_commemorations_native.json").read_text(encoding="utf-8")
    )
    record = next(row for row in payload["records"] if row["old_calendar_month_day"] == "02-29")
    assert record["lanes"]["ar"]["source_id"] == "jerusalem_hagiotaphite_calendar_ar_2024"
    assert record["lanes"]["el"]["source_id"] == "jerusalem_hagiotaphite_calendar_el_2024"
    english = record["lanes"]["en"]
    assert english["text"] == "Righteous John Cassian the Confessor"
    assert english["source_id"] == "antioch_patriarchate_john_cassian_en"
    assert english["jurisdiction"] == "antioch_patriarchate"
    assert english["comparative"] is False
    assert english["date_assignment_basis"] == "JERUSALEM_ARABIC_AND_GREEK_LEAP_CALENDARS_2024"


def test_every_civil_day_through_2050_has_a_named_commemoration():
    payload = json.loads((ROOT / "canonical/internal_calendar_2026_2050.json").read_text(encoding="utf-8"))
    assert len(payload["days"]) == 9131
    generic = {
        "old_calendar_date_baseline",
        "comparative_english_lane",
        "mixed_native_and_comparative_lanes",
    }
    assert not [
        day["date_iso"]
        for day in payload["days"]
        if day["commemoration"].get("source_kind") in generic
    ]


def test_all_previously_generic_civil_dates_now_show_john_cassian():
    payload = json.loads((ROOT / "canonical/internal_calendar_2026_2050.json").read_text(encoding="utf-8"))
    by_date = {day["date_iso"]: day for day in payload["days"]}
    for civil_date in ("2028-03-13", "2036-03-13", "2040-03-13", "2048-03-13"):
        day = by_date[civil_date]
        assert day["julian_date"].endswith("-02-29")
        assert day["commemoration"]["name"] == {
            "ar": "أبينا البارّ كاسيانوس المعترف",
            "en": "Righteous John Cassian the Confessor",
            "el": "Κασσιανοῦ ὁσίου",
        }
        assert day["commemoration"]["source_kind"] == "verified_official_orthodox_native_lanes"
