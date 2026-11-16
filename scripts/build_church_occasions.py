#!/usr/bin/env python3
"""Import complete native editions; retain scanned Arabic books as original PDFs."""
from __future__ import annotations
import argparse
import hashlib
import io
import json
import re
import time
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
import zipfile
from html.parser import HTMLParser
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "canonical/church_occasions_sources.json"
OUTPUT = ROOT / "app/src/main/assets/data/occasions"
LANGUAGES = ("ar", "en", "el")

class NativeCells(HTMLParser):
    def __init__(self, language):
        super().__init__(convert_charrefs=True)
        self.cell = "leftCell" if language == "el" else "rightCell"
        self.active = False
        self.paragraph = None
        self.parts = []
        self.paragraphs = []
    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "td": self.active = self.cell in attrs.get("class", "").split()
        if self.active and tag == "p": self.paragraph = attrs.get("class", "text"); self.parts = []
        if self.paragraph is not None and tag == "br": self.parts.append("\n")
    def handle_data(self, data):
        if self.active and self.paragraph is not None: self.parts.append(data)
    def handle_endtag(self, tag):
        if tag == "p" and self.paragraph is not None:
            value = " ".join("".join(self.parts).split())
            if value: self.paragraphs.append((self.paragraph, value))
            self.paragraph = None
        if tag == "td": self.active = False

def localized(value, language): return {key: value if key == language else "" for key in LANGUAGES}

def source_attribution(source_id):
    if source_id == "orthodox_jordan_arabic_services":
        return {"ar": "مطرانية الأردن للروم الأرثوذكس", "en": "Greek Orthodox Archdiocese of Jordan", "el": "Ἑλληνορθόδοξη Ἀρχιεπισκοπὴ Ἰορδανίας"}
    if source_id.startswith("goarch_digital_chant_stand_"):
        return {"ar": "منصة الترتيل الرقمية للأبرشية اليونانية الأرثوذكسية في أميركا", "en": "GOARCH Digital Chant Stand", "el": "Digital Chant Stand τῆς Ἑλληνορθόδοξης Ἀρχιεπισκοπῆς Ἀμερικῆς"}
    return {"ar": source_id, "en": source_id, "el": source_id}

def import_segments(raw, source):
    language = source["language"]
    if source["format"] == "docx":
        with zipfile.ZipFile(io.BytesIO(raw)) as archive:
            root = ET.fromstring(archive.read("word/document.xml"))
        ns = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
        paragraphs = [("text", "".join(n.text or "" for n in p.findall(".//w:t", ns)).strip())
                      for p in root.findall(".//w:p", ns)]
    else:
        parser = NativeCells(language)
        parser.feed(raw.decode("utf-8-sig"))
        paragraphs = parser.paragraphs
    segments = []
    source_credits = []
    speaker = ""
    for kind, text in paragraphs:
        if not text: continue
        # DCS places its contributor key near the beginning of every book.
        # It is documentation rather than prayer text, so retain it intact at
        # the end of the edition where attribution belongs.
        if "GOADEDES Greek Orthodox Archdiocese" in text:
            source_credits.append(text)
            continue
        if kind == "role": speaker = text; continue
        if kind in ("designation", "title", "chapter"):
            speaker = ""
            segments.append({"type": "section", "title": localized(text, language)})
            continue
        role = re.match(r"^(الكاهن|الشماس|الشعب|الجوقة|القارئ|المرتل)\s*[:：]", text)
        if role: speaker = role[1]
        item = {"type": "rubric" if kind == "rubric" else "text", "text": localized(text, language)}
        if speaker: item["speaker"] = localized(speaker, language)
        segments.append(item)
    if source_credits:
        credit_title = {"ar": "المصدر وإعداد النص", "en": "Source and text preparation", "el": "Πηγὴ καὶ προετοιμασία κειμένου"}[language]
        segments.append({"type": "section", "title": localized(credit_title, language)})
        for credit in source_credits:
            segments.append({"type": "rubric", "text": localized(credit, language)})
    else:
        credit_title = {"ar": "المصدر", "en": "Source", "el": "Πηγή"}[language]
        segments.append({"type": "section", "title": localized(credit_title, language)})
        segments.append({"type": "rubric", "text": localized(source_attribution(source["source_id"])[language], language)})
    text = "\n".join(s.get("text", {}).get(language, "") for s in segments)
    if len(text) < source.get("minimum_chars", 5000) or len(segments) < source.get("minimum_segments", 25):
        raise ValueError(f"Complete native edition missing: {source['id']}.{language}")
    if "Bot Verification" in text or "Just a moment" in text:
        raise ValueError("Source is an access challenge, not service text")
    if language == "el" and sum('\u0370' <= c <= '\u03ff' or '\u1f00' <= c <= '\u1fff' for c in text) < 1000:
        raise ValueError("Greek native lane is missing")
    return segments

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache-dir", type=Path, default=ROOT / "app/build/occasion-source-cache")
    parser.add_argument("--record-hashes", action="store_true", help="Record initial hashes from verified HTTPS source responses")
    args = parser.parse_args()
    manifest = json.loads(MANIFEST.read_text())
    args.cache_dir.mkdir(parents=True, exist_ok=True)
    downloads = {}
    pdf_targets = {}
    catalog = {}
    for source in manifest["sources"]:
        url = source["url"]
        if url not in downloads:
            cache = args.cache_dir / source.get("cache_file", hashlib.sha256(url.encode()).hexdigest())
            if cache.exists(): raw = cache.read_bytes()
            else:
                time.sleep(.5)
                request = urllib.request.Request(urllib.parse.quote(url, safe=":/%?=&"))
                with urllib.request.urlopen(request, timeout=45) as response: raw = response.read()
                cache.write_bytes(raw)
            downloads[url] = raw
        raw = downloads[url]
        digest = hashlib.sha256(raw).hexdigest()
        if source.get("sha256") and source["sha256"] != digest:
            raise ValueError(f"Source checksum changed: {source['id']}.{source['language']}")
        if not source.get("sha256"):
            if not args.record_hashes: raise ValueError("Source is not pinned: " + url)
            source["sha256"] = digest
        language, service_id = source["language"], source["id"]
        target = OUTPUT / language
        target.mkdir(parents=True, exist_ok=True)
        service = {"id": "occasion::" + service_id, "title": source["title"], "source_language": language,
                   "displayable": True, "source_document": {"source_id": source["source_id"], "source_url": url,
                   "source_sha256": digest, "machine_translation_used": False,
                   "attribution": source_attribution(source["source_id"])}, "category": "occasions"}
        service["full_service_complete"] = True
        service["completeness_scope"] = "COMPLETE_SOURCE_EDITION_ONLY"
        service["source_provenance"] = {"status": "PINNED_NATIVE_SERVICE_BOOK", "source_id": source["source_id"],
                                        "source_sha256": digest, "machine_translation_used": False}
        service["native_source"] = {"source_id": source["source_id"], "url": url,
                                    "native_language": language, "permission_confirmed": True}
        if source.get("edition_date"):
            service["edition_date"] = source["edition_date"]
            service["source_document"]["calendar_role"] = source.get("calendar_role", "SOURCE_EDITION_ONLY")
        if source["format"] == "pdf":
            if not raw.startswith(b"%PDF"): raise ValueError("Not a PDF source: " + url)
            from pypdf import PdfReader
            pages = len(PdfReader(io.BytesIO(raw)).pages)
            if pages < 2: raise ValueError("Incomplete PDF book: " + url)
            pdf_id = source.get("reuse_pdf_asset_id", service_id)
            if not re.fullmatch(r"[a-z0-9_]+", pdf_id): raise ValueError("Invalid PDF asset id")
            pdf_target = target / (pdf_id + ".pdf")
            if str(pdf_target) in pdf_targets and pdf_targets[str(pdf_target)] != digest:
                raise ValueError("Conflicting editions share a PDF asset")
            pdf_targets[str(pdf_target)] = digest
            pdf_target.write_bytes(raw)
            service.update(content_kind="pdf_facsimile", asset_pdf=f"data/occasions/{language}/{pdf_id}.pdf",
                           asset_sha256=digest, page_count=pages)
            if "source_page_start" in source:
                start, end = source["source_page_start"], source.get("source_page_end", pages - 1)
                if not 0 <= start <= end < pages: raise ValueError("Invalid source chapter boundary")
                service.update(source_page_start=start, source_page_end=end, completeness_scope="COMPLETE_SOURCE_CHAPTER")
        else:
            service["segments"] = import_segments(raw, source)
            service["content_kind"] = "native_text"
        (target / (service_id + ".json")).write_text(json.dumps(service, ensure_ascii=False, indent=2) + "\n")
        entry = catalog.setdefault(service_id, {"id": service_id, "title": source["title"], "group": source["group"],
                                              "day": source["day"], "languages": []})
        entry["languages"].append(language)
        if source.get("days"): entry["days"] = source["days"]
        print("IMPORTED", service_id, language, service.get("page_count", len(service.get("segments", []))))
    OUTPUT.mkdir(parents=True, exist_ok=True)
    (OUTPUT / "catalog.json").write_text(json.dumps({"schema_version": 1, "entries": list(catalog.values()),
        "machine_translation": False, "cross_language_fallback": False}, ensure_ascii=False, indent=2) + "\n")
    if args.record_hashes: MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")

if __name__ == "__main__": main()
