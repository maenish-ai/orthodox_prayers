from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def test_liturgy_hub_uses_current_annual_calendar_when_snapshot_is_stale():
    hub = (ROOT / "app/src/main/java/com/orthodoxprayers/privateapp/ui/screens/LiturgyHubScreen.java").read_text(encoding="utf-8")
    assert "data.currentDayForDisplay()" in hub
    assert "data.today()" not in hub
    assert "appointedServiceId(selection)" in hub


def test_main_liturgy_route_uses_current_calendar_and_central_appointed_resolver():
    main = (ROOT / "app/src/main/java/com/orthodoxprayers/privateapp/MainActivity.java").read_text(encoding="utf-8")
    resolver = (ROOT / "app/src/main/java/com/orthodoxprayers/privateapp/data/AppointedLiturgyResolver.java").read_text(encoding="utf-8")
    assert "repository.currentDayForDisplay()" in main
    assert "AppointedLiturgyResolver.serviceId(selection)" in main
    assert 'return "divine_liturgy_basil"' in resolver
    assert 'return "presanctified_liturgy"' in resolver


def test_current_day_merges_appointed_liturgy_when_daily_package_omits_it():
    repository = (ROOT / "app/src/main/java/com/orthodoxprayers/privateapp/data/DataRepository.java").read_text(encoding="utf-8")
    assert 'current.optJSONObject("liturgy_service_selection")' in repository
    assert 'annual.optJSONObject("liturgy_service_selection")' in repository
    assert 'merged.put("liturgy_service_selection"' in repository


def test_liturgy_tab_directly_opens_current_appointed_service_with_hub_fallback():
    main = (ROOT / "app/src/main/java/com/orthodoxprayers/privateapp/MainActivity.java").read_text(encoding="utf-8")
    assert 'if (canOpenTodayLiturgyDirectly())' in main
    assert 'return new ReaderScreen(this, serviceId);' in main
    assert 'return new LiturgyHubScreen(this);' in main


def test_church_eucharist_catalog_card_routes_to_calendar_appointed_liturgy():
    base = (ROOT / "app/src/main/java/com/orthodoxprayers/privateapp/ui/screens/BaseScreen.java").read_text(encoding="utf-8")
    assert '"church_eucharist".equals(serviceId)' in base
    assert 'host.navigate("liturgy", null)' in base


def test_liturgy_hub_button_opens_appointed_service_without_static_bypass():
    hub = (ROOT / "app/src/main/java/com/orthodoxprayers/privateapp/ui/screens/LiturgyHubScreen.java").read_text(encoding="utf-8")
    assert 'host.navigate("reader", appointedId)' in hub
    assert 'library::divine_liturgy' not in hub
