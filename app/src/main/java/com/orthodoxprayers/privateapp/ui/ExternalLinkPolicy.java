package com.orthodoxprayers.privateapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import java.net.URI;
import java.util.Locale;

/** One strict gateway for opening untrusted or remotely supplied web links. */
public final class ExternalLinkPolicy {
    private ExternalLinkPolicy() {}

    public static boolean isAllowedHttps(String value) {
        if (value == null || value.length() > 4_096) return false;
        String candidate = value.trim();
        if (candidate.isEmpty() || candidate.indexOf('\n') >= 0 || candidate.indexOf('\r') >= 0) return false;
        try {
            URI uri = URI.create(candidate);
            return "https".equals(uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT))
                    && uri.getHost() != null && !uri.getHost().trim().isEmpty()
                    && uri.getUserInfo() == null;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    public static Uri validatedHttpsUri(String value) {
        if (!isAllowedHttps(value)) return null;
        try { return Uri.parse(value.trim()); }
        catch (RuntimeException error) { return null; }
    }

    public static boolean open(Activity activity, String value) {
        Uri uri = validatedHttpsUri(value);
        if (activity == null || uri == null) return false;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            activity.startActivity(intent);
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }
}
