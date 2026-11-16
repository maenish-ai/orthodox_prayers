import hashlib
import importlib.util
import json
from pathlib import Path
import xml.etree.ElementTree as ET

from pypdf import PdfReader

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'

def load(path):
    return json.loads(path.read_text(encoding='utf-8'))

def test_holy_week_has_real_native_service_books_for_every_day_and_language():
    catalog = load(ASSETS / 'data/occasions/catalog.json')['entries']
    for language in ('ar', 'en', 'el'):
        for day in ('monday', 'tuesday', 'wednesday', 'thursday', 'friday', 'saturday'):
            editions = [item for item in catalog if item['group'] == 'holy_week'
                        and (item['day'] == day or day in item.get('days', []))
                        and language in item['languages']]
            assert editions, (language, day)
            for item in editions:
                service = load(ASSETS / f'data/occasions/{language}/{item["id"]}.json')
                assert service['source_language'] == language
                assert service['displayable'] is True
                assert service['content_kind'] in ('pdf_facsimile', 'native_text')

def test_pdfs_are_exact_complete_source_files_with_valid_chapter_boundaries():
    manifest = load(ROOT / 'canonical/church_occasions_sources.json')['sources']
    verified = {}
    for source in manifest:
        if source['format'] != 'pdf':
            continue
        service = load(ASSETS / f'data/occasions/{source["language"]}/{source["id"]}.json')
        path = ASSETS / service['asset_pdf']
        if path not in verified:
            verified[path] = (hashlib.sha256(path.read_bytes()).hexdigest(), len(PdfReader(path).pages))
        digest, pages = verified[path]
        assert digest == source['sha256'] == service['asset_sha256']
        assert pages == service['page_count']
        assert 'segments' not in service  # No broken OCR substituted for the original.
        assert 0 <= service.get('source_page_start', 0) <= service.get('source_page_end', pages - 1) < pages
    thursday = load(ASSETS / 'data/occasions/ar/holy_thursday_matins.json')
    book = PdfReader(ASSETS / thursday['asset_pdf'])
    assert 'الخميس' in book.pages[thursday['source_page_start']].extract_text()

def test_native_text_fields_never_fall_back_to_another_language():
    manifest = load(ROOT / 'canonical/church_occasions_sources.json')['sources']
    for source in manifest:
        language = source['language']
        service = load(ASSETS / f'data/occasions/{language}/{source["id"]}.json')
        assert service['source_document']['source_sha256'] == source['sha256']
        assert service['source_document']['machine_translation_used'] is False
        if source['format'] == 'pdf':
            continue
        assert len(service['segments']) >= source.get('minimum_segments', 25)
        for segment in service['segments']:
            for field in ('text', 'speaker', 'title'):
                for other, text in segment.get(field, {}).items():
                    if other != language:
                        assert not text, (source['id'], language, other)

def test_dcs_parser_preserves_native_words_and_selects_only_the_requested_column():
    spec = importlib.util.spec_from_file_location('occasion_importer', ROOT / 'scripts/build_church_occasions.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    html = """<table><tr><td class='leftCell'><p class='text'>Κύριε <span>ἐλέησον.</span></p></td>
      <td class='rightCell'><p class='text'>Lord, <span>have mercy.</span></p></td></tr></table>"""
    greek = module.NativeCells('el'); greek.feed(html)
    english = module.NativeCells('en'); english.feed(html)
    assert greek.paragraphs == [('text', 'Κύριε ἐλέησον.')]
    assert english.paragraphs == [('text', 'Lord, have mercy.')]

def test_home_has_occasions_instead_of_search_and_all_label_is_exact():
    home = (ROOT / 'app/src/main/java/com/orthodoxprayers/privateapp/ui/screens/HomeScreen.java').read_text()
    assert '"occasions", null' in home
    assert '"search", null' not in home
    for folder, expected in [('values', 'الكل'), ('values-en', 'All'), ('values-el', 'Ὅλα')]:
        resources = ET.parse(ROOT / f'app/src/main/res/{folder}/ui_strings.xml').getroot()
        assert resources.find("string[@name='ui_worship_all']").text == expected

def test_every_major_feast_has_three_native_titles_and_local_calendar_date_definition():
    feasts = load(ASSETS / 'data/occasions/feasts.json')['feasts']
    assert {'christmas', 'cross', 'pascha', 'pentecost'} <= {item['id'] for item in feasts}
    for feast in feasts:
        assert all(feast['title'].get(language) for language in ('ar', 'en', 'el'))
        assert ('julian_month_day' in feast) != ('pascha_offset' in feast)
