from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def read(relative: str) -> str:
    return (ROOT / relative).read_text(encoding="utf-8")


def test_external_links_use_one_https_only_gateway():
    policy = read("app/src/main/java/com/orthodoxprayers/privateapp/ui/ExternalLinkPolicy.java")
    assert '"https".equals' in policy
    assert "uri.getUserInfo() == null" in policy
    assert "CATEGORY_BROWSABLE" in policy
    assert "value.length() > 4_096" in policy
    for screen in (
        "SourcesScreen.java",
        "SearchScreen.java",
        "ReadingDetailScreen.java",
        "ChurchesScreen.java",
        "ReaderScreen.java",
    ):
        source = read(f"app/src/main/java/com/orthodoxprayers/privateapp/ui/screens/{screen}")
        assert "ExternalLinkPolicy.open" in source
        assert "new Intent(Intent.ACTION_VIEW" not in source


def test_directory_redirects_cannot_leave_the_official_https_host():
    sync = read("app/src/main/java/com/orthodoxprayers/privateapp/data/ChurchDirectorySync.java")
    assert "setInstanceFollowRedirects(false)" in sync
    assert "original.getHost().equalsIgnoreCase(redirected.getHost())" in sync
    assert '"https".equalsIgnoreCase(redirected.getProtocol())' in sync
    assert "redirects <= 5" in sync


def test_backup_and_broadcast_hardening_are_explicit():
    manifest = read("app/src/main/AndroidManifest.xml")
    receiver = read("app/src/main/java/com/orthodoxprayers/privateapp/update/ScheduleRestoreReceiver.java")
    assert 'android:allowBackup="false"' in manifest
    assert 'android:dataExtractionRules="@xml/data_extraction_rules"' in manifest
    assert 'android:fullBackupContent="@xml/backup_rules"' in manifest
    assert "Intent.ACTION_BOOT_COMPLETED.equals(action)" in receiver
    assert "Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)" in receiver
