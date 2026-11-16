package com.orthodoxprayers.privateapp.ui.screens;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.orthodoxprayers.privateapp.R;
import com.orthodoxprayers.privateapp.ui.ScreenHost;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Offline facsimile reader: never replaces scanned Arabic with broken OCR. */
public final class PdfServiceScreen extends BaseScreen {
    private final JSONObject edition;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean active = true;
    private volatile int requestVersion;
    private PdfRenderer renderer;
    private ParcelFileDescriptor descriptor;
    private int pageIndex;
    private int pageCount;
    private int firstSourcePage;
    private float zoom = 1f;
    private ImageView image;
    private TextView status;
    private Button pageButton;
    private Button previous;
    private Button next;
    private ScrollView vertical;
    private HorizontalScrollView horizontal;

    public PdfServiceScreen(ScreenHost host, JSONObject edition) { super(host); this.edition = edition; }

    @Override public View createView() {
        LinearLayout root = new LinearLayout(host.activity());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(ui.colors().background());
        root.addView(ui.header(localized(edition.optJSONObject("title"), ""), true, host::goBack));
        LinearLayout toolbar = ui.row();
        previous = ui.smallButton(preferences.isRtl() ? "→" : "←", false);
        previous.setContentDescription(local(R.string.ui_pdf_previous_page));
        previous.setOnClickListener(v -> showPage(pageIndex - 1));
        toolbar.addView(previous, ui.weight(48));
        pageButton = ui.smallButton("—", false);
        pageButton.setContentDescription(local(R.string.ui_pdf_choose_page));
        pageButton.setOnClickListener(v -> choosePage());
        toolbar.addView(pageButton, ui.weight(48));
        next = ui.smallButton(preferences.isRtl() ? "←" : "→", false);
        next.setContentDescription(local(R.string.ui_pdf_next_page));
        next.setOnClickListener(v -> showPage(pageIndex + 1));
        toolbar.addView(next, ui.weight(48));
        Button smaller = ui.smallButton("−", false);
        smaller.setContentDescription(local(R.string.ui_pdf_zoom_out));
        smaller.setOnClickListener(v -> { zoom = Math.max(1f, zoom - .5f); showPage(pageIndex); });
        toolbar.addView(smaller, ui.weight(48));
        Button larger = ui.smallButton("+", false);
        larger.setContentDescription(local(R.string.ui_pdf_zoom_in));
        larger.setOnClickListener(v -> { zoom = Math.min(3f, zoom + .5f); showPage(pageIndex); });
        toolbar.addView(larger, ui.weight(48));
        Button first = ui.smallButton("↑", false);
        first.setContentDescription(local(R.string.ui_reader_back_to_start));
        first.setOnClickListener(v -> showPage(0));
        toolbar.addView(first, ui.weight(48));
        root.addView(toolbar);
        status = ui.infoBadge(local(R.string.ui_pdf_loading));
        root.addView(status);
        vertical = new ScrollView(host.activity());
        horizontal = new HorizontalScrollView(host.activity());
        image = new ImageView(host.activity());
        image.setBackgroundColor(Color.WHITE);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
        image.setContentDescription(local(R.string.ui_pdf_original_page));
        horizontal.addView(image);
        vertical.addView(horizontal);
        root.addView(vertical, new LinearLayout.LayoutParams(-1, 0, 1f));
        JSONObject source = edition.optJSONObject("source_document");
        String sourceName = source == null ? "" : localized(source.optJSONObject("attribution"), "").trim();
        if (!sourceName.isEmpty()) {
            TextView attribution = ui.infoBadge(localFormat(R.string.ui_occasion_source_attribution, sourceName));
            root.addView(attribution);
        }
        previous.setEnabled(false); next.setEnabled(false);
        int restored = preferences.readerPosition(edition.optString("id"));
        worker.execute(() -> openBook(restored));
        return root;
    }

    private void openBook(int restored) {
        try {
            String hash = edition.getString("asset_sha256");
            if (!hash.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid source digest");
            File directory = new File(host.activity().getCacheDir(), "occasion_books");
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IllegalStateException("Book cache unavailable");
            File target = new File(directory, hash + ".pdf");
            File temporary = File.createTempFile(hash, ".tmp", directory);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = host.activity().getAssets().open(edition.getString("asset_pdf"));
                 FileOutputStream output = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[65536]; int count;
                while ((count = input.read(buffer)) != -1) { output.write(buffer, 0, count); digest.update(buffer, 0, count); }
            }
            StringBuilder actual = new StringBuilder();
            for (byte value : digest.digest()) actual.append(String.format(Locale.ROOT, "%02x", value & 255));
            if (!hash.equals(actual.toString())) { temporary.delete(); throw new SecurityException("Source digest mismatch"); }
            if (target.exists() && !target.delete()) throw new IllegalStateException("Cannot replace book cache");
            if (!temporary.renameTo(target)) throw new IllegalStateException("Cannot prepare source book");
            descriptor = ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(descriptor);
            int total = renderer.getPageCount();
            firstSourcePage = Math.max(0, Math.min(edition.optInt("source_page_start", 0), total - 1));
            int last = Math.max(firstSourcePage, Math.min(edition.optInt("source_page_end", total - 1), total - 1));
            pageCount = last - firstSourcePage + 1;
            main.post(() -> { if (active) showPage(Math.min(restored, pageCount - 1)); });
        } catch (Exception error) {
            main.post(() -> { if (active) status.setText(local(R.string.ui_occasions_unavailable)); });
        }
    }

    private void choosePage() {
        if (pageCount == 0) return;
        EditText input = new EditText(host.activity());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(Integer.toString(pageIndex + 1));
        new AlertDialog.Builder(host.activity()).setTitle(local(R.string.ui_pdf_choose_page)).setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    try { showPage(Integer.parseInt(input.getText().toString()) - 1); }
                    catch (NumberFormatException ignored) { /* Keep the current page. */ }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void showPage(int requested) {
        if (!active || pageCount == 0) return;
        pageIndex = Math.max(0, Math.min(requested, pageCount - 1));
        int selected = pageIndex, version = ++requestVersion;
        float scale = zoom;
        int screenWidth = host.activity().getResources().getDisplayMetrics().widthPixels;
        worker.execute(() -> {
            if (!active || version != requestVersion) return;
            try (PdfRenderer.Page page = renderer.openPage(selected + firstSourcePage)) {
                int width = Math.min(1800, Math.round(screenWidth * scale));
                int height = Math.max(1, Math.round(width * (float) page.getHeight() / page.getWidth()));
                if ((long) width * height > 3_000_000) {
                    float factor = (float) Math.sqrt(3_000_000d / ((long) width * height));
                    width = Math.max(1, Math.round(width * factor)); height = Math.max(1, Math.round(height * factor));
                }
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                new Canvas(bitmap).drawColor(Color.WHITE);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                int visibleWidth = Math.round(screenWidth * scale);
                int visibleHeight = Math.round(visibleWidth * (float) page.getHeight() / page.getWidth());
                main.post(() -> {
                    if (!active || version != requestVersion) { bitmap.recycle(); return; }
                    image.setImageBitmap(bitmap);
                    image.setLayoutParams(new HorizontalScrollView.LayoutParams(visibleWidth, visibleHeight));
                    vertical.scrollTo(0, 0); horizontal.scrollTo(0, 0);
                    pageButton.setText((selected + 1) + " / " + pageCount);
                    previous.setEnabled(selected > 0); next.setEnabled(selected + 1 < pageCount);
                    status.setVisibility(View.GONE);
                    preferences.setReaderPosition(edition.optString("id"), selected, 0);
                });
            } catch (Exception error) {
                main.post(() -> { if (active) { status.setVisibility(View.VISIBLE); status.setText(local(R.string.ui_occasions_unavailable)); } });
            }
        });
    }

    @Override public void onHidden() {
        active = false; requestVersion++;
        image.setImageDrawable(null);
        worker.execute(() -> {
            try { if (renderer != null) renderer.close(); if (descriptor != null) descriptor.close(); }
            catch (Exception ignored) { /* The screen is already closed. */ }
        });
        worker.shutdown();
    }
}
