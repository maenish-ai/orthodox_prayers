from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def test_reader_avoids_repeating_speaker_in_heading_and_text():
    source = (ROOT / "app/src/main/java/com/orthodoxprayers/privateapp/ui/ReaderAdapter.java").read_text(encoding="utf-8")
    assert "stripSpeakerPrefix(value.text, speaker)" in source
    assert "inferSpeakerPrefix(bodyText)" in source
    assert "text = stripSpeakerPrefix(text, speaker);" in source

def test_continuous_reader_places_third_and_sixth_hours_before_proskomide_and_core():
    source = (ROOT / "app/src/main/java/com/orthodoxprayers/privateapp/data/DataRepository.java").read_text(encoding="utf-8")
    composer = source.split("private JSONObject composeFollowAlongLiturgy", 1)[1].split("private static JSONArray strictAppointedLiturgyCore", 1)[0]
    third = composer.index('appendNativePrayerService(continuous, "third_hour", language)')
    sixth = composer.index('appendNativePrayerService(continuous, "sixth_hour", language)')
    proskomide = composer.index('appendNativePrayerService(continuous, "proskomide", language)')
    core = composer.index("appendSegments(continuous, core)")
    assert third < sixth < proskomide < core
    assert "appendSundayCycleGospel(continuous" not in composer
