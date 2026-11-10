package com.orthodoxprayers.privateapp.ui.screens;

import android.view.View;
import android.widget.LinearLayout;

import com.orthodoxprayers.privateapp.data.FastingNoticeEngine;
import com.orthodoxprayers.privateapp.ui.ScreenHost;
import com.orthodoxprayers.privateapp.ui.UiKit;

import org.json.JSONObject;

import java.time.LocalDate;

/**
 * A deliberately small fasting details page opened from the Home fasting notice.
 * It keeps the full calendar-day screen available from the calendar itself.
 */
public final class FastingSummaryScreen extends BaseScreen {
    private final String date;

    public FastingSummaryScreen(ScreenHost host, String date) {
        super(host);
        this.date = date == null ? "" : date.trim();
    }

    @Override
    public View createView() {
        UiKit.Page page = page(local(com.orthodoxprayers.privateapp.R.string.ui_fasting_summary_title_r66), true);
        JSONObject item = findDay();
        if (item == null) {
            add(page.root, centered(
                    local(com.orthodoxprayers.privateapp.R.string.ui_no_trusted_details_for_this_date_are_included_in_dfb3006c),
                    16,
                    ui.colors().secondaryText(),
                    false
            ), 30, 30);
            return page.scroll;
        }

        JSONObject fasting = item.optJSONObject("fasting");
        if (!isFastingDay(fasting)) {
            LinearLayout card = ui.card();
            card.addView(centered(
                    local(com.orthodoxprayers.privateapp.R.string.ui_fast_summary_no_fast),
                    17,
                    ui.colors().secondaryText(),
                    true
            ));
            add(page.root, card, 16, 16);
            return page.scroll;
        }

        LinearLayout card = ui.card();
        FastingPeriod period = fastingPeriod();
        boolean multiDay = period != null && period.totalDays > 1;

        JSONObject verification = fasting.optJSONObject("verification");
        String verificationRule = verification == null ? "" : verification.optString("rule", "").trim();
        String fullTitle = localized(fasting.optJSONObject("title"), fastingDisplayTitle(item, date));
        if (multiDay || !"weekly_wednesday_friday".equals(verificationRule)) {
            String contextTitle = compactFastContextTitle(fullTitle);
            if (!contextTitle.isEmpty()) {
                card.addView(centered(contextTitle, 17, ui.colors().primaryText(), true),
                        ui.margins(-1, -2, 0, 4, 0, 10));
            }
        }

        addField(card,
                local(com.orthodoxprayers.privateapp.R.string.ui_fast_summary_type),
                fastingRuleTitle(fasting)
        );

        if (multiDay) {
            addField(card,
                    local(com.orthodoxprayers.privateapp.R.string.ui_fast_summary_period),
                    localFormat(
                            com.orthodoxprayers.privateapp.R.string.ui_fast_summary_period_format,
                            dayLabel(period.start),
                            dayLabel(period.end)
                    )
            );
            addField(card,
                    local(com.orthodoxprayers.privateapp.R.string.ui_fast_summary_days),
                    localFormat(
                            com.orthodoxprayers.privateapp.R.string.ui_fast_summary_days_format,
                            period.totalDays
                    )
            );
        }

        JSONObject guidance = fasting.optJSONObject("guidance");
        if (guidance != null) {
            // A one-day fast should be immediately readable: rule + what to
            // abstain from. Longer seasons keep the fuller explanatory guide.
            if (multiDay) {
                addField(card,
                        local(com.orthodoxprayers.privateapp.R.string.ui_fast_summary_allowed),
                        localized(guidance.optJSONObject("allowed_summary"), "")
                );
            }
            addField(card,
                    local(com.orthodoxprayers.privateapp.R.string.ui_fast_summary_forbidden),
                    compactForbidden(localized(guidance.optJSONObject("forbidden_summary"), ""))
            );
            if (multiDay) {
                addField(card,
                        local(com.orthodoxprayers.privateapp.R.string.ui_fast_summary_explanation),
                        localized(guidance.optJSONObject("beginner_explanation"), "")
                );
            }
        }

        if (multiDay) {
            JSONObject detail = fasting.optJSONObject("detail");
            if (detail != null) {
                addField(card,
                        local(com.orthodoxprayers.privateapp.R.string.ui_fast_summary_explanation),
                        localized(detail, "")
                );
            }
        }

        JSONObject abstinence = fasting.optJSONObject("abstinence");
        if (abstinence != null) {
            String abstinenceText = localized(abstinence.optJSONObject("detail"), "");
            if (abstinenceText.isEmpty()) {
                abstinenceText = localized(abstinence.optJSONObject("end_condition"), "");
            }
            if (abstinenceText.isEmpty() && abstinence.optBoolean("applies", false)) {
                abstinenceText = local(com.orthodoxprayers.privateapp.R.string.ui_fast_summary_abstinence);
            }
            addField(card,
                    local(com.orthodoxprayers.privateapp.R.string.ui_fast_summary_abstinence),
                    abstinenceText
            );
        }

        if (card.getChildCount() == 0) {
            card.addView(ui.text(
                    local(com.orthodoxprayers.privateapp.R.string.ui_fast_summary_details_unavailable),
                    14,
                    ui.colors().secondaryText(),
                    false
            ));
        }
        add(page.root, card, 12, 16);
        return page.scroll;
    }

    private String fastingRuleTitle(JSONObject fasting) {
        String code = fasting == null ? "" : fasting.optString("code", "").trim();
        String language = preferences.effectiveLanguage();
        if ("strict".equals(code)) {
            if ("ar".equals(language)) return "صوم صارم";
            if ("el".equals(language)) return "Αὐστηρὰ νηστεία";
            return "Strict fast";
        }
        if ("fish_allowed".equals(code)) {
            if ("ar".equals(language)) return "صوم مع السماح بالسمك والزيت والنبيذ";
            if ("el".equals(language)) return "Νηστεία με ψάρι, ἔλαιο καὶ οἶνο";
            return "Fast with fish, oil, and wine permitted";
        }
        if ("wine_oil".equals(code)) {
            if ("ar".equals(language)) return "صوم مع السماح بالزيت والنبيذ";
            if ("el".equals(language)) return "Νηστεία με ἔλαιο καὶ οἶνο";
            return "Fast with oil and wine permitted";
        }
        if ("dairy_allowed".equals(code)) {
            if ("ar".equals(language)) return "صوم مع السماح بالألبان والبيض والسمك";
            if ("el".equals(language)) return "Νηστεία με γαλακτοκομικά, αὐγά καὶ ψάρι";
            return "Fast with dairy, eggs, and fish permitted";
        }
        String title = localized(fasting == null ? null : fasting.optJSONObject("title"), "");
        int separator = title.lastIndexOf(" — ");
        return separator >= 0 ? title.substring(separator + 3).trim() : title;
    }

    private String compactFastContextTitle(String title) {
        if (title == null) return "";
        String value = title.trim();
        int separator = value.lastIndexOf(" — ");
        return separator > 0 ? value.substring(0, separator).trim() : value;
    }

    private String compactForbidden(String value) {
        if (value == null) return "";
        String compact = value.trim();
        String[] prefixes = new String[]{
                "غير المسموح:",
                "Not permitted:",
                "Δὲν ἐπιτρέπονται:",
                "Δεν επιτρέπονται:"
        };
        for (String prefix : prefixes) {
            if (compact.startsWith(prefix)) return compact.substring(prefix.length()).trim();
        }
        return compact;
    }

    private JSONObject findDay() {
        return data.calendarDay(date);
    }

    private void addField(LinearLayout card, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        card.addView(ui.text(label + ":\n" + value, 15, ui.colors().primaryText(), false),
                ui.margins(-1, -2, 0, 9, 0, 0));
    }

    private FastingPeriod fastingPeriod() {
        LocalDate target = parseDate(date);
        if (target == null) return null;

        FastingNoticeEngine.Notice notice = FastingNoticeEngine.evaluate(
                target,
                isoDate -> data.calendarDay(isoDate)
        );
        if (notice.kind == FastingNoticeEngine.Kind.CURRENT_MAJOR_FAST
                && notice.startDate != null
                && notice.endDate != null) {
            return new FastingPeriod(notice.startDate, notice.endDate, notice.totalDays);
        }

        return new FastingPeriod(target, target, 1);
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String dayLabel(LocalDate value) {
        if (value == null) return "";
        JSONObject item = data.calendarDay(value.toString());
        if (item != null) {
            String label = localized(item.optJSONObject("date_label"), "").trim();
            if (!label.isEmpty()) return label;
        }
        return value.toString();
    }

    private static final class FastingPeriod {
        final LocalDate start;
        final LocalDate end;
        final int totalDays;

        FastingPeriod(LocalDate start, LocalDate end, int totalDays) {
            this.start = start;
            this.end = end;
            this.totalDays = Math.max(1, totalDays);
        }
    }
}
