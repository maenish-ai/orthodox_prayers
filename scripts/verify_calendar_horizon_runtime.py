#!/usr/bin/env python3
"""Run the real Java daily engine against packaged assets, with a file-only Context adapter.

This tests date/content composition, not the Android UI or framework scheduling.
"""
from __future__ import annotations
import argparse
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/java/com/orthodoxprayers/privateapp"

CONTEXT = '''package android.content;
public class Context {
 public Context getApplicationContext() { return this; }
 public android.content.res.AssetManager getAssets() { return new android.content.res.AssetManager(); }
}'''
ASSETS = '''package android.content.res;
public class AssetManager {
 public java.io.InputStream open(String name) throws java.io.IOException {
  for(String root:System.getProperty("asset.roots").split(java.io.File.pathSeparator)) {
   java.io.File file=new java.io.File(root,name); if(file.isFile()) return new java.io.FileInputStream(file);
  }
  throw new java.io.FileNotFoundException(name);
 }
}'''
HARNESS = '''import com.orthodoxprayers.privateapp.data.LocalDailyContentEngine;
import org.json.*;
import java.time.LocalDate;
public class HorizonHarness {
 static void require(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
 public static void main(String[] args) throws Exception {
  LocalDailyContentEngine engine=new LocalDailyContentEngine(new android.content.Context());
  int days=0;
  for(int year=2026;year<=2050;year++) {
   for(String suffix:new String[]{"-01-01","-01-07","-07-01","-12-31"}) {
    LocalDate date=LocalDate.parse(year+suffix);
    JSONObject day=engine.buildWindow(date,1);
    require(date.toString().equals(day.getString("date_iso")),"Wrong date: "+date);
    require(day.getJSONObject("local_daily_window").getInt("day_count")==1,"Window mismatch");
    require(day.getJSONArray("weekly_days").length()==0,"Selected date leaked into today window");
    JSONObject service=day.getJSONArray("services").getJSONObject(0);
    require(date.toString().equals(service.getString("dynamic_date")),"Wrong service date");
    JSONArray readings=day.getJSONArray("readings");
    for(int index=0;index<readings.length();index++) {
     JSONObject reading=readings.getJSONObject(index),body=reading.getJSONObject("body");
     for(String language:new String[]{"ar","en","el"})
      require(!body.optString(language).trim().isEmpty(),"Missing "+language+" passage "+date);
    }
    if(suffix.equals("-01-07")) {
     JSONObject feast=day.getJSONObject("feast");
     require(feast.getString("en").contains("Nativity"),"Nativity calendar shifted: "+date);
    }
    days++;
   }
  }
  JSONObject last=engine.buildCurrentWindow(LocalDate.parse("2050-12-31"));
  require(last.getJSONObject("local_daily_window").getInt("day_count")==1,"Horizon invented 2051 dates");
  JSONObject friday=engine.buildWindow(LocalDate.parse("2026-04-10"),1);
  require("no_divine_liturgy".equals(friday.getJSONObject("liturgy_service_selection").getString("service_type")),"Good Friday rite mismatch");
  boolean blocked=false;
  try { engine.buildWindow(LocalDate.parse("2051-01-01"),1); } catch(IllegalStateException expected) { blocked=true; }
  require(blocked,"2051 must remain out of scope");
  System.out.println("CALENDAR_HORIZON_RUNTIME_OK selected_dates="+days+" languages=ar,en,el last=2050-12-31 out_of_range_rejected=true");
 }
}'''

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--json-jar", type=Path)
    args = parser.parse_args()
    if args.json_jar is None:
        cache = Path(os.environ.get("GRADLE_USER_HOME", str(Path.home() / ".gradle")))
        candidates = list((cache / "caches/modules-2/files-2.1/org.json/json/20240303").rglob("json-20240303.jar"))
        if not candidates: raise SystemExit("Build testDebugUnitTest first, or pass --json-jar explicitly")
        args.json_jar = candidates[0]
    java_home = Path(os.environ["JAVA_HOME"]) if os.environ.get("JAVA_HOME") else None
    javac = str(java_home / "bin/javac") if java_home else "javac"
    java = str(java_home / "bin/java") if java_home else "java"
    with tempfile.TemporaryDirectory(prefix="calendar-horizon-") as temporary:
        directory = Path(temporary)
        for name, content in {"android/content/Context.java": CONTEXT,
                              "android/content/res/AssetManager.java": ASSETS,
                              "HorizonHarness.java": HARNESS}.items():
            path = directory / name; path.parent.mkdir(parents=True, exist_ok=True); path.write_text(content)
        originals = [SOURCE / "data/LocalDailyContentEngine.java", SOURCE / "data/DataContract.java",
                     SOURCE / "data/AppointedLiturgyResolver.java", SOURCE / "bible/BibleCorpusRepository.java",
                     SOURCE / "bible/BibleReference.java"]
        subprocess.run([javac, "-encoding", "UTF-8", "-cp", str(args.json_jar), "-d", str(directory),
                        *map(str, directory.rglob("*.java")), *map(str, originals)], check=True)
        assets = os.pathsep.join(map(str, [ROOT / "app/src/main/assets", ROOT / "app/build/generated/bibleAssets"]))
        subprocess.run([java, "-Xmx512m", "-Dasset.roots=" + assets, "-cp",
                        os.pathsep.join([str(directory), str(args.json_jar)]), "HorizonHarness"], check=True)

if __name__ == "__main__": main()
