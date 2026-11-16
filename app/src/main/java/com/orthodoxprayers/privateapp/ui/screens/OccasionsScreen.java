package com.orthodoxprayers.privateapp.ui.screens;

import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import com.orthodoxprayers.privateapp.R;
import com.orthodoxprayers.privateapp.ui.ScreenHost;
import com.orthodoxprayers.privateapp.ui.UiKit;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

/** Native service books and date-aware occasions; all navigation stays in the app. */
public final class OccasionsScreen extends BaseScreen {
    private final String section;
    private final String key;
    private final int year;
    private JSONArray annual;
    public OccasionsScreen(ScreenHost host, String argument) {
        super(host);
        String[] parts = argument == null ? new String[0] : argument.split("\\|", -1);
        section = parts.length > 0 ? parts[0] : "root";
        key = parts.length > 1 ? parts[1] : "";
        int selected = LocalDate.parse(data.currentAmmanDate()).getYear();
        try { if (parts.length > 2) selected = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
        year = Math.max(2026, Math.min(2050, selected));
    }
    @Override public View createView() {
        UiKit.Page page = page(local(R.string.ui_church_occasions), true);
        annual = data.calendarDays(year);
        Button chooseYear = ui.smallButton(Integer.toString(year), false);
        chooseYear.setContentDescription(local(R.string.ui_occasion_choose_year));
        chooseYear.setOnClickListener(v -> {
            String[] years = new String[25]; for (int i = 0; i < years.length; i++) years[i] = Integer.toString(2026 + i);
            new AlertDialog.Builder(host.activity()).setTitle(local(R.string.ui_occasion_choose_year))
                    .setSingleChoiceItems(years, year - 2026, (dialog, index) -> { dialog.dismiss(); navigate(section, key, index + 2026); }).show();
        });
        add(page.root, chooseYear, 4, 8);
        switch (section) {
            case "holy_week": holyWeek(page.root); break;
            case "day": serviceBooks(page.root, "holy_week", key); break;
            case "feasts": feasts(page.root); break;
            case "feast": feast(page.root); break;
            case "saints": saints(page.root); break;
            case "books": serviceBooks(page.root, "", ""); break;
            default:
                card(page.root, local(R.string.ui_occasion_holy_week), () -> navigate("holy_week", "", year));
                card(page.root, local(R.string.ui_occasion_feasts), () -> navigate("feasts", "", year));
                card(page.root, local(R.string.ui_occasion_saints), () -> navigate("saints", "1", year));
                card(page.root, local(R.string.ui_occasion_source_books), () -> navigate("books", "", year));
                card(page.root, local(R.string.ui_church_service_section), () -> host.navigate("prayer_category", "church_service"));
        }
        return page.scroll;
    }
    private void navigate(String section, String key, int selectedYear) {
        host.navigate("occasions", section + "|" + key + "|" + selectedYear);
    }
    private void card(LinearLayout root, String title, Runnable action) {
        LinearLayout card = ui.actionCard(R.drawable.ic_action_calendar, title, "");
        card.setOnClickListener(v -> action.run()); add(root, card, 3, 8);
    }
    private LocalDate pascha() {
        for (int i = 0; i < annual.length(); i++) {
            JSONObject day = annual.optJSONObject(i); JSONObject feast = day == null ? null : day.optJSONObject("feast");
            if (feast != null && feast.optString("en", "").startsWith("Holy Pascha"))
                return LocalDate.parse(day.optString("date_iso"));
        }
        return null;
    }
    private LocalDate holyDate(String key) {
        String[] days = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};
        LocalDate pascha = pascha(); if (pascha == null) return null;
        for (int i = 0; i < days.length; i++) if (days[i].equals(key)) return pascha.plusDays(i - 6);
        return null;
    }
    private void holyWeek(LinearLayout root) {
        String[] keys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};
        LocalDate pascha = pascha(); if (pascha == null) return;
        for (int i = 0; i < keys.length; i++) {
            final String dayKey = keys[i]; LocalDate date = pascha.plusDays(i - 6);
            JSONObject day = data.calendarDay(date.toString());
            card(root, date + "\n" + localized(day == null ? null : day.optJSONObject("feast"), ""), () -> navigate("day", dayKey, year));
        }
    }
    private void feasts(LinearLayout root) {
        JSONArray feasts = data.churchFeasts();
        for (int i = 0; i < feasts.length(); i++) {
            JSONObject feast = feasts.optJSONObject(i); if (feast == null) continue;
            String id = feast.optString("id");
            card(root, localized(feast.optJSONObject("title"), ""), () -> navigate("feast", id, year));
        }
    }
    private void feast(LinearLayout root) {
        LocalDate date = null; JSONArray feasts = data.churchFeasts();
        for (int i = 0; i < feasts.length(); i++) {
            JSONObject feast = feasts.optJSONObject(i); if (feast == null || !key.equals(feast.optString("id"))) continue;
            root.addView(ui.sectionTitle(localized(feast.optJSONObject("title"), "")));
            if (feast.has("pascha_offset")) { LocalDate p = pascha(); if (p != null) date = p.plusDays(feast.optInt("pascha_offset")); }
            else for (int j = 0; j < annual.length(); j++) {
                JSONObject day = annual.optJSONObject(j);
                if (day != null && day.optString("julian_date", "").endsWith(feast.optString("julian_month_day"))) {
                    date = LocalDate.parse(day.optString("date_iso")); break;
                }
            }
            break;
        }
        if (date != null) dateCard(root, date.toString());
        serviceBooks(root, "feasts", key);
    }
    private void dateCard(LinearLayout root, String date) {
        card(root, date + "\n" + local(R.string.ui_occasion_day_services), () -> host.navigate("calendar_day", date));
    }
    private void serviceBooks(LinearLayout root, String group, String day) {
        if ("holy_week".equals(group)) { LocalDate date = holyDate(day); if (date != null) dateCard(root, date.toString()); }
        JSONArray entries = data.churchOccasions().optJSONArray("entries"); int count = 0;
        if (entries != null) for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.optJSONObject(i); if (entry == null) continue;
            if (!group.isEmpty() && !group.equals(entry.optString("group"))) continue;
            boolean dayMatch = day.isEmpty() || day.equals(entry.optString("day"));
            JSONArray relatedDays = entry.optJSONArray("days");
            if (relatedDays != null) for (int j = 0; j < relatedDays.length(); j++)
                if (day.equals(relatedDays.optString(j))) dayMatch = true;
            if (!dayMatch) continue;
            JSONArray languages = entry.optJSONArray("languages"); boolean available = false;
            if (languages != null) for (int j = 0; j < languages.length(); j++)
                if (preferences.effectiveLanguage().equals(languages.optString(j))) available = true;
            if (!available) continue;
            count++; String id = entry.optString("id");
            card(root, localized(entry.optJSONObject("title"), ""), () -> host.navigate("reader", "occasion::" + id));
        }
        if (count == 0) add(root, ui.infoBadge(local(R.string.ui_occasions_unavailable)), 8, 8);
    }
    private void saints(LinearLayout root) {
        serviceBooks(root, "saints", "");
        int selectedMonth = 1; try { selectedMonth = Math.max(1, Math.min(12, Integer.parseInt(key))); } catch (NumberFormatException ignored) {}
        Locale locale = Locale.forLanguageTag(preferences.effectiveLanguage());
        Button month = ui.smallButton(Month.of(selectedMonth).getDisplayName(TextStyle.FULL, locale), false);
        final int activeMonth = selectedMonth;
        month.setOnClickListener(v -> {
            String[] months = new String[12]; for (int i = 0; i < 12; i++) months[i] = Month.of(i + 1).getDisplayName(TextStyle.FULL, locale);
            new AlertDialog.Builder(host.activity()).setTitle(local(R.string.ui_occasion_choose_month))
                    .setSingleChoiceItems(months, activeMonth - 1, (dialog, index) -> { dialog.dismiss(); navigate("saints", Integer.toString(index + 1), year); }).show();
        });
        add(root, month, 4, 8);
        for (int i = 0; i < annual.length(); i++) {
            JSONObject day = annual.optJSONObject(i); if (day == null) continue;
            String date = day.optString("date_iso");
            if (LocalDate.parse(date).getMonthValue() != selectedMonth) continue;
            card(root, date + "\n" + displayableCommemoration(day), () -> host.navigate("calendar_day", date));
        }
    }
}
