from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BAD_PROSKOMIDE = (
    "واثعنليمعشرصجولتاد",
    "هلماندهنسجد",
    "صلوة نصفالليل اليومية",
    "१ صلوة",
)


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def service(path: str, service_id: str) -> dict:
    payload = json.loads(read(path))
    if payload.get("id") == service_id:
        return payload
    return next(item for item in payload["services"] if item["id"] == service_id)


def test_arabic_proskomide_stops_at_its_real_conclusion_everywhere() -> None:
    paths = (
        "data/services/native_overrides/ar/proskomide.json",
        "data/services/native/library_ar.json",
        "app/src/main/assets/data/native/library_ar.json",
    )
    for path in paths:
        item = service(path, "proskomide")
        assert len(item["segments"]) == 77
        assert item["segments"][-2]["title"]["ar"] == "ختام خدمة التقدمة"
        visible = json.dumps(item["segments"], ensure_ascii=False)
        for marker in BAD_PROSKOMIDE:
            assert marker not in visible


def test_arabic_search_index_uses_the_clean_proskomide() -> None:
    for path in (
        "data/search/search_index_ar.json",
        "app/src/main/assets/data/search/search_index_ar.json",
    ):
        payload = json.loads(read(path))
        document = next(item for item in payload["documents"] if item["id"] == "service:proskomide")
        assert document["display_text"].rstrip().endswith(
            "ثم تكون الصرفة بحسب اليوم، وتبقى القرابين مغطاة ومحفوظة بوقار إلى بدء القداس الإلهي."
        )
        for marker in BAD_PROSKOMIDE:
            assert marker not in document["display_text"]


def test_thanksgiving_is_filtered_for_the_selected_liturgy() -> None:
    source = read("app/src/main/java/com/orthodoxprayers/privateapp/data/DataRepository.java")
    assert "thanksgivingSegmentsForLiturgy" in source
    assert "طروبارية القديس يوحنا الذهبي الفم" in source
    assert "طروبارية القديس باسيليوس الكبير" in source
    assert "طروبارية القديس غريغوريوس الكبير" in source
    assert "activeArabicVariant.equals(selected)" in source


def test_home_uses_calendar_driven_fasting_notice_instead_of_continue_reading() -> None:
    source = read("app/src/main/java/com/orthodoxprayers/privateapp/ui/screens/HomeScreen.java")
    assert "FastingNoticeEngine.evaluate" in source
    assert "addSmartFastingNotice" in source
    assert "addContinueReading" not in source
    assert "ui_old_church_calendar_home_format" in source
    assert 'LocalDate detailsDate = notice.upcomingChangeDate != null' in source
    assert 'host.navigate("fasting_summary", detailsDate.toString())' in source
    assert "specificCommemoration(today)" not in source


def test_unavailable_commemoration_wording_is_never_displayed() -> None:
    source = read("app/src/main/java/com/orthodoxprayers/privateapp/data/CommemorationDisplayPolicy.java")
    assert "تعذّر التحقق من تذكار هذا اليوم" in source
    assert "this day’s commemoration could not be verified" in source
    assert "ἡ μνήμη τῆς ἡμέρας δὲν κατέστη δυνατόν" in source

def test_java_unit_test_covers_each_thanksgiving_variant() -> None:
    source = read("app/src/test/java/com/orthodoxprayers/privateapp/data/ThanksgivingVariantSelectionTest.java")
    assert "arabicStJohnShowsOnlyItsOwnTroparion" in source
    assert "arabicBasilShowsOnlyItsOwnTroparion" in source
    assert "arabicPresanctifiedShowsOnlyItsOwnTroparion" in source
    assert 'assertFalse(visible.contains("عند إقامة قداس"))' in source



def test_empty_optional_liturgy_speaker_is_not_reported_as_missing_arabic_text() -> None:
    repository = read("app/src/main/java/com/orthodoxprayers/privateapp/data/DataRepository.java")
    assert 'rawArabic.isEmpty() && rawEnglish.isEmpty() && rawGreek.isEmpty()' in repository
    assert 'return new LocalizedValue(safeFallback, false);' in repository

    liturgy = json.loads(read("app/src/main/assets/data/native/services/ar/divine_liturgy.json"))
    affected = []
    for index, segment in enumerate(liturgy["segments"]):
        speaker = segment.get("speaker")
        text = segment.get("text")
        if (
            isinstance(speaker, dict)
            and all(not str(speaker.get(lang, "")).strip() for lang in ("ar", "en", "el"))
            and isinstance(text, dict)
            and str(text.get("ar", "")).strip()
        ):
            affected.append(index)
    assert len(affected) >= 200
    assert 151 in affected and 152 in affected and 153 in affected and 154 in affected


def test_daily_liturgy_prefers_calendar_overlay_and_keeps_native_only_as_fallback() -> None:
    repository = read("app/src/main/java/com/orthodoxprayers/privateapp/data/DataRepository.java")
    main = read("app/src/main/java/com/orthodoxprayers/privateapp/MainActivity.java")
    hub = read("app/src/main/java/com/orthodoxprayers/privateapp/ui/screens/LiturgyHubScreen.java")
    assert 'JSONObject dynamic = !libraryOnly && isTodayCurrent()' in repository
    assert 'forceCanonicalLibrary' not in repository
    assert 'if (canOpenTodayLiturgyDirectly())' in main
    assert 'return new ReaderScreen(this, serviceId);' in main
    assert 'host.navigate("reader", appointedId)' in hub
    assert 'library::divine_liturgy' not in hub


def test_daily_reading_slots_preserve_multiple_appointed_readings_in_order() -> None:
    engine = read("app/src/main/java/com/orthodoxprayers/privateapp/data/LocalDailyContentEngine.java")
    assert 'JSONObject existing = slots.optJSONObject(kind);' in engine
    assert 'previous + "\\n\\n" + next' in engine
    assert '"epistle".equals(kind)' in engine
    assert '"gospel".equals(kind)' in engine


def test_fasting_hint_is_week_ahead_calendar_driven_and_single_day_summary_is_compact() -> None:
    engine = read("app/src/main/java/com/orthodoxprayers/privateapp/data/FastingNoticeEngine.java")
    home = read("app/src/main/java/com/orthodoxprayers/privateapp/ui/screens/HomeScreen.java")
    summary = read("app/src/main/java/com/orthodoxprayers/privateapp/ui/screens/FastingSummaryScreen.java")
    assert 'FAST_LOOKAHEAD_DAYS = 7' in engine
    assert 'UPCOMING_CALENDAR_FAST' in engine
    assert 'findRuleChange' in engine
    assert 'isFastDay(day)' in engine
    assert 'notice.upcomingChangeDate != null' in home
    assert 'notice.kind == FastingNoticeEngine.Kind.NONE) return;' in home
    assert 'boolean multiDay = period != null && period.totalDays > 1;' in summary
    assert 'if (multiDay)' in summary
    assert 'compactForbidden' in summary
