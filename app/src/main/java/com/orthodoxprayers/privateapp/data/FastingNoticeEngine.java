package com.orthodoxprayers.privateapp.data;

import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Calendar-driven home notice for the nearest meaningful Orthodox fast.
 *
 * <p>The embedded church calendar is authoritative. The engine looks roughly
 * one week ahead for the nearest actual fasting day regardless of weekday,
 * while still grouping the four long fasting seasons. During a long fast it
 * also exposes the nearest food-rule change (fish/oil/wine/strict, etc.) so the
 * Home hint can warn the user before tomorrow's rule changes.</p>
 */
public final class FastingNoticeEngine {
    public static final int MAJOR_FAST_LOOKAHEAD_DAYS = 7;
    public static final int WEEKLY_FAST_LOOKAHEAD_DAYS = 7;
    public static final int FAST_LOOKAHEAD_DAYS = 7;
    private static final int MAJOR_FAST_BOUNDARY_SCAN_DAYS = 80;

    private FastingNoticeEngine() {}

    public interface DayProvider {
        JSONObject day(String isoDate);
    }

    public enum Kind {
        CURRENT_MAJOR_FAST,
        UPCOMING_MAJOR_FAST,
        UPCOMING_WEEKLY_FAST,
        UPCOMING_CALENDAR_FAST,
        NONE
    }

    public enum Family {
        DORMITION,
        NATIVITY,
        GREAT_LENT,
        APOSTLES,
        NONE
    }

    public static final class Notice {
        public final Kind kind;
        public final Family family;
        public final LocalDate targetDate;
        public final LocalDate startDate;
        public final LocalDate endDate;
        public final int daysUntilStart;
        public final int dayNumber;
        public final int daysRemaining;
        public final int totalDays;
        /** True when the selected day is the feast after a major fast, not a fast-season day. */
        public final boolean feastDay;
        public final DayOfWeek weekday;
        /** Nearest change of fasting food rule inside the same long fast, within seven days. */
        public final LocalDate upcomingChangeDate;
        public final int daysUntilChange;

        private Notice(
                Kind kind,
                Family family,
                LocalDate targetDate,
                LocalDate startDate,
                LocalDate endDate,
                int daysUntilStart,
                int dayNumber,
                int daysRemaining,
                int totalDays,
                boolean feastDay,
                DayOfWeek weekday,
                LocalDate upcomingChangeDate,
                int daysUntilChange
        ) {
            this.kind = kind;
            this.family = family;
            this.targetDate = targetDate;
            this.startDate = startDate;
            this.endDate = endDate;
            this.daysUntilStart = daysUntilStart;
            this.dayNumber = dayNumber;
            this.daysRemaining = daysRemaining;
            this.totalDays = totalDays;
            this.feastDay = feastDay;
            this.weekday = weekday;
            this.upcomingChangeDate = upcomingChangeDate;
            this.daysUntilChange = daysUntilChange;
        }

        public static Notice none(LocalDate today) {
            return new Notice(
                    Kind.NONE,
                    Family.NONE,
                    today,
                    null,
                    null,
                    -1,
                    0,
                    0,
                    0,
                    false,
                    today == null ? null : today.getDayOfWeek(),
                    null,
                    -1
            );
        }
    }

    public static Notice evaluate(LocalDate today, DayProvider provider) {
        if (today == null || provider == null) return Notice.none(today);

        JSONObject currentDay = provider.day(today.toString());
        Family currentFamily = majorFamily(currentDay);
        if (currentFamily != Family.NONE) {
            boolean feastDay = !isMajorSeasonDay(currentDay, currentFamily);
            LocalDate seasonAnchor = feastDay ? today.minusDays(1) : today;
            LocalDate start = findBoundary(seasonAnchor, provider, currentFamily, -1);
            LocalDate end = feastDay ? seasonAnchor : findBoundary(today, provider, currentFamily, 1);
            int total = inclusiveDays(start, end);
            int dayNumber = feastDay ? total : (int) ChronoUnit.DAYS.between(start, today) + 1;
            int remaining = feastDay ? 0 : (int) ChronoUnit.DAYS.between(today, end);
            LocalDate ruleChange = feastDay ? null : findRuleChange(today, provider, currentFamily);
            int untilChange = ruleChange == null ? -1 : (int) ChronoUnit.DAYS.between(today, ruleChange);
            return new Notice(
                    Kind.CURRENT_MAJOR_FAST,
                    currentFamily,
                    today,
                    start,
                    end,
                    0,
                    Math.max(1, dayNumber),
                    Math.max(0, remaining),
                    Math.max(1, total),
                    feastDay,
                    today.getDayOfWeek(),
                    ruleChange,
                    untilChange
            );
        }

        // One chronological scan: the nearest actual fast wins, regardless of
        // weekday. A major season is classified when its first fasting day is
        // encountered; Wednesday/Friday retain their friendly weekly label.
        for (int offset = 1; offset <= FAST_LOOKAHEAD_DAYS; offset++) {
            LocalDate candidate = today.plusDays(offset);
            JSONObject day = provider.day(candidate.toString());
            if (!isFastDay(day)) continue;

            Family family = majorFamily(day);
            if (family != Family.NONE) {
                Family previous = majorFamily(provider.day(candidate.minusDays(1).toString()));
                if (previous != family) {
                    LocalDate end = findBoundary(candidate, provider, family, 1);
                    return new Notice(
                            Kind.UPCOMING_MAJOR_FAST,
                            family,
                            candidate,
                            candidate,
                            end,
                            offset,
                            0,
                            (int) ChronoUnit.DAYS.between(candidate, end),
                            inclusiveDays(candidate, end),
                            false,
                            candidate.getDayOfWeek(),
                            null,
                            -1
                    );
                }
            }

            Kind kind = isWeeklyFast(day, candidate.getDayOfWeek())
                    ? Kind.UPCOMING_WEEKLY_FAST
                    : Kind.UPCOMING_CALENDAR_FAST;
            return new Notice(
                    kind,
                    Family.NONE,
                    candidate,
                    candidate,
                    candidate,
                    offset,
                    0,
                    0,
                    1,
                    false,
                    candidate.getDayOfWeek(),
                    null,
                    -1
            );
        }

        return Notice.none(today);
    }

    private static LocalDate findRuleChange(LocalDate today, DayProvider provider, Family family) {
        JSONObject current = provider.day(today.toString());
        String currentSignature = fastingRuleSignature(current);
        if (currentSignature.isEmpty()) return null;
        for (int offset = 1; offset <= FAST_LOOKAHEAD_DAYS; offset++) {
            LocalDate candidate = today.plusDays(offset);
            JSONObject day = provider.day(candidate.toString());
            if (!isMajorSeasonDay(day, family)) break;
            String candidateSignature = fastingRuleSignature(day);
            if (!candidateSignature.isEmpty() && !candidateSignature.equals(currentSignature)) {
                return candidate;
            }
        }
        return null;
    }

    private static String fastingRuleSignature(JSONObject day) {
        if (day == null) return "";
        JSONObject fasting = day.optJSONObject("fasting");
        if (fasting == null || !fasting.optBoolean("is_fast", false)) return "";
        String code = fasting.optString("code", "").trim();
        JSONObject verification = fasting.optJSONObject("verification");
        String rule = verification == null ? "" : verification.optString("rule", "").trim();
        // The food code is the meaningful user-facing distinction. Fall back to
        // the verified rule only for legacy payloads that do not expose a code.
        return code.isEmpty() ? rule : code;
    }

    private static boolean isFastDay(JSONObject day) {
        if (day == null) return false;
        JSONObject fasting = day.optJSONObject("fasting");
        return fasting != null && fasting.optBoolean("is_fast", false);
    }

    private static boolean isMajorSeasonDay(JSONObject day, Family family) {
        if (day == null || family == Family.NONE) return false;
        JSONObject fasting = day.optJSONObject("fasting");
        if (fasting == null || !fasting.optBoolean("is_fast", false)) return false;
        JSONObject verification = fasting.optJSONObject("verification");
        String rule = verification == null ? "" : verification.optString("rule", "").trim();
        if (family == Family.DORMITION && rule.startsWith("dormition_feast_")) return false;
        return majorFamily(day) == family;
    }

    private static LocalDate findBoundary(
            LocalDate anchor,
            DayProvider provider,
            Family family,
            int direction
    ) {
        LocalDate boundary = anchor;
        for (int i = 0; i < MAJOR_FAST_BOUNDARY_SCAN_DAYS; i++) {
            LocalDate next = boundary.plusDays(direction);
            if (!isMajorSeasonDay(provider.day(next.toString()), family)) break;
            boundary = next;
        }
        return boundary;
    }

    private static int inclusiveDays(LocalDate start, LocalDate end) {
        if (start == null || end == null) return 0;
        return (int) ChronoUnit.DAYS.between(start, end) + 1;
    }

    private static boolean isWeeklyFast(JSONObject day, DayOfWeek weekday) {
        return isFastDay(day)
                && (weekday == DayOfWeek.WEDNESDAY || weekday == DayOfWeek.FRIDAY);
    }

    private static Family majorFamily(JSONObject day) {
        if (day == null) return Family.NONE;
        JSONObject fasting = day.optJSONObject("fasting");
        if (fasting == null || !fasting.optBoolean("is_fast", false)) return Family.NONE;
        JSONObject verification = fasting.optJSONObject("verification");
        String rule = verification == null ? "" : verification.optString("rule", "").trim();
        if ("post_dormition_week_fish".equals(rule)) return Family.NONE;
        String text = fastingSearchText(day);
        if (containsAny(text,
                "dormition fast",
                "dormition feast",
                "صوم السيدة والدة الإله",
                "صوم رقاد",
                "عيد رقاد",
                "νηστεία τῆς κοιμήσεως",
                "νηστεια της κοιμησεως",
                "ἑορτὴ τῆς κοιμήσεως",
                "εορτη της κοιμησεως")) {
            return Family.DORMITION;
        }
        if (containsAny(text,
                "nativity fast",
                "صوم الميلاد",
                "νηστεία τῶν χριστουγέννων",
                "νηστεια των χριστουγεννων")) {
            return Family.NATIVITY;
        }
        if (containsAny(text,
                "apostles’ fast",
                "apostles' fast",
                "apostles fast",
                "صوم الرسل",
                "νηστεία τῶν ἁγίων ἀποστόλων",
                "νηστεια των αγιων αποστολων")) {
            return Family.APOSTLES;
        }
        if (containsAny(text,
                "great lent",
                "holy week",
                "الصوم الكبير",
                "الأسبوع العظيم",
                "الاسبوع العظيم",
                "μεγάλη τεσσαρακοστή",
                "μεγαλη τεσσαρακοστη",
                "μεγάλη ἑβδομάδα",
                "μεγαλη εβδομαδα")) {
            return Family.GREAT_LENT;
        }
        return Family.NONE;
    }

    private static String fastingSearchText(JSONObject day) {
        StringBuilder out = new StringBuilder();
        JSONObject fasting = day.optJSONObject("fasting");
        appendLocalized(out, fasting == null ? null : fasting.optJSONObject("title"));
        appendLocalized(out, fasting == null ? null : fasting.optJSONObject("season"));
        appendLocalized(out, day.optJSONObject("fast"));
        appendLocalized(out, day.optJSONObject("status"));
        return out.toString().toLowerCase(Locale.ROOT);
    }

    private static void appendLocalized(StringBuilder out, JSONObject localized) {
        if (localized == null) return;
        for (String language : new String[]{"ar", "en", "el"}) {
            String value = localized.optString(language, "").trim();
            if (value.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(value);
        }
    }

    private static boolean containsAny(String haystack, String... needles) {
        if (haystack == null || haystack.isEmpty()) return false;
        for (String needle : needles) {
            if (haystack.contains(needle)) return true;
        }
        return false;
    }
}
