package com.jk.halostudio;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Insets;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.jk.halostudio.core.Aod;
import com.jk.halostudio.core.Art;
import com.jk.halostudio.core.Composer;
import com.jk.halostudio.core.Composer.Kind;
import com.jk.halostudio.core.Presets;
import com.jk.halostudio.core.Upload;
import com.jk.halostudio.core.ZhDial;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity implements WatchLink.Listener {
    private static final int PICK_PHOTO = 7, PICK_FONT = 8, PICK_TEMPLATE = 9;
    private static final String[] NAV = {"Gallery", "Studio", "My faces", "Watch"};
    private static final String[] TABS = {"Layout", "Background", "Effects", "Clock", "Font", "Stickers", "Text"};
    private static final String[] EMOJI = {"⭐", "❤️", "🔥", "✨", "🌙", "☀️", "⚡", "🌸", "🍀", "🎧", "🏏", "⚽", "🎮", "🚀", "🐱", "🐶",
            "😎", "😍", "🥳", "💎", "👑", "🎵", "🌈", "🕉️", "🇮🇳"};
    private static final String[] SURPRISE_FONTS = {"sans-serif-black", "sans-serif-condensed", "sans-serif-light", "sans-serif-medium",
            "serif", "monospace", "casual"};
    private static final int THUMB_PX = 400;
    private static final String GALLERY_VER = "9";        // bump when the renderer changes
    private static final int[][] ART_PALETTES = palettes();

    private Ui ui;
    private Design design = new Design();
    private Bitmap photo, photoRot;
    private int photoRotFor = -1;
    private byte[] galaxyT, moonT;
    private String galaxySrc = "missing", moonSrc = "missing";
    private Handler render, ble, main;
    private ExecutorService pool;
    private WatchLink link;
    private SharedPreferences prefs;
    private final Random rnd = new Random();

    // shell
    private FrameLayout stage;
    private View screenView;
    private final Ui.Icon[] navIcons = new Ui.Icon[NAV.length];
    private final TextView[] navLabels = new TextView[NAV.length];
    private int screen = -1;
    private TextView statusView;
    private ProgressBar bar;
    private boolean busy = false;
    private volatile boolean cancelReq = false;
    private TextView cancelBtn;
    private long sendStart;

    // gallery
    private int category = 0;
    private String query = "";
    private final Bitmap[] thumbs = new Bitmap[Presets.ALL.length];
    private final Object[] thumbLocks = new Object[Presets.ALL.length];
    private LinearLayout gridBox, chipRow;

    // studio
    private ImageView preview;
    private LinearLayout panel, tabRow;
    private TextView touchHint, installBtn, studioName;
    private int tab = 0, selSticker = -1;
    private volatile int seq = 0;

    // detail
    private FrameLayout overlay;
    private Design detail;
    private File detailDir;
    private Bitmap detailPhoto;
    private ImageView detailView;
    private TextView detailInstall;
    private byte[] detailRaw;
    private int detailPx = 600;
    private volatile int dseq = 0;
    private final Runnable tick = this::tickDetail;

    // watch
    private Aod aod;
    private LinearLayout aodCard;
    private Bitmap pendingWrist;
    private String pendingWristName;

    // ------------------------------------------------------------------ lifecycle
    @Override
    protected void onCreate(Bundle b) {
        installCrashCatcher();
        boolean light = getSharedPreferences("halo", MODE_PRIVATE).getBoolean("lightTheme", true);
        Ui.applyTheme(light);
        setTheme(light ? android.R.style.Theme_Material_Light_NoActionBar : android.R.style.Theme_Material_NoActionBar);
        super.onCreate(b);
        ui = new Ui(this);
        for (int i = 0; i < thumbLocks.length; i++) thumbLocks[i] = new Object();
        prefs = getSharedPreferences("halo", MODE_PRIVATE);
        loadLocalConfigDefaults();
        Window w = getWindow();
        w.setStatusBarColor(Ui.BG);
        w.setNavigationBarColor(Ui.CARD);

        main = new Handler(Looper.getMainLooper());
        HandlerThread rt = new HandlerThread("render"); rt.start(); render = new Handler(rt.getLooper());
        HandlerThread bt = new HandlerThread("ble"); bt.start(); ble = new Handler(bt.getLooper());
        pool = Executors.newFixedThreadPool(2);
        link = new WatchLink(this, ble, this);
        loadTemplates();
        loadState();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        root.setOnApplyWindowInsetsListener((v, in) -> {
            Insets si = in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            v.setPadding(si.left, si.top, si.right, si.bottom);
            return WindowInsets.CONSUMED;
        });
        LinearLayout shell = ui.col();
        stage = new FrameLayout(this);
        shell.addView(stage, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout strip = ui.col();
        strip.setPadding(ui.dp(18), ui.dp(4), ui.dp(18), ui.dp(6));
        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setVisibility(View.GONE);
        LinearLayout barRow = ui.row();
        barRow.addView(bar, new LinearLayout.LayoutParams(0, ui.dp(6), 1));
        cancelBtn = ui.text("✕ Cancel", 13, Ui.BAD, true);
        cancelBtn.setPadding(ui.dp(12), ui.dp(6), ui.dp(4), ui.dp(6));
        cancelBtn.setVisibility(View.GONE);
        cancelBtn.setOnClickListener(v -> cancelInstall());
        barRow.addView(cancelBtn);
        strip.addView(barRow, new LinearLayout.LayoutParams(-1, -2));
        statusView = ui.text("", 12, Ui.MUTED, false);
        statusView.setMaxLines(2);
        statusView.setEllipsize(TextUtils.TruncateAt.END);
        strip.addView(statusView);
        shell.addView(strip);
        shell.addView(buildNav());
        root.addView(shell, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        try {   // only valid once the window has a decor view (after setContentView)
            android.view.WindowInsetsController ic = w.getInsetsController();
            int lightBars = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            if (ic != null) ic.setSystemBarsAppearance(Ui.LIGHT ? lightBars : 0, lightBars);
        } catch (RuntimeException ignored) { }
        if (b == null) splash(root);
        main.postDelayed(this::showLastCrash, 1900);

        statusView.setText(galaxyT == null && moonT == null
                ? "No face templates yet — Watch → Import template."
                : "Pick a face. Before installing: Watch → Force stop NoiseFit.");
        show(0);

        if (checkSelfPermission("android.permission.BLUETOOTH_CONNECT") != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT"}, 2);
        if (getIntent().getBooleanExtra("selftest", false)) render.post(this::selfTest);
    }

    /** Saves any crash (with stack trace) to files/crash.txt so the next launch can show and copy it. */
    private void installCrashCatcher() {
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        final File f = new File(getFilesDir(), "crash.txt");
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new FileOutputStream(f))) {
                pw.println("Lumina " + versionName() + " · Android " + android.os.Build.VERSION.RELEASE + " · " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
                pw.println("Thread: " + t.getName());
                e.printStackTrace(pw);
            } catch (Exception ignored) { }
            if (prev != null) prev.uncaughtException(t, e);
        });
    }

    private String versionName() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception e) { return "?"; }
    }

    private void showLastCrash() {
        File f = new File(getFilesDir(), "crash.txt");
        if (!f.exists()) return;
        String txt;
        try { txt = new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception e) { f.delete(); return; }
        final String report = txt.length() > 6000 ? txt.substring(0, 6000) : txt;
        new android.app.AlertDialog.Builder(this)
                .setTitle("Lumina closed unexpectedly last time")
                .setMessage("Tap Copy error and paste it to the developer so it can be fixed.\n\n" + report)
                .setPositiveButton("Copy error", (dlg, w) -> {
                    android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("Lumina crash", report));
                    f.delete();
                    status("Error copied — paste it in your message.");
                })
                .setNegativeButton("Dismiss", (dlg, w) -> f.delete())
                .show();
    }

    /** Animated launch: the logo spins in, the name fades up, then the app slides into view. */
    private void splash(FrameLayout root) {
        FrameLayout sp = new FrameLayout(this);
        sp.setBackgroundColor(Ui.BG);
        sp.setClickable(true);
        LinearLayout col = ui.col();
        col.setGravity(Gravity.CENTER);
        Ui.Logo logo = new Ui.Logo(this);
        col.addView(logo, new LinearLayout.LayoutParams(ui.dp(120), ui.dp(120)));
        TextView nm = ui.text("Lumina", 34, Ui.TEXT, true);
        Ui.gradientText(nm, Ui.HEAD_A, Ui.HEAD_B, Ui.ACCENT2);
        nm.setPadding(0, ui.dp(16), 0, 0);
        nm.setGravity(Gravity.CENTER);
        col.addView(nm);
        TextView tag = ui.text("living watch faces for your Halo", 13, Ui.MUTED, false);
        tag.setGravity(Gravity.CENTER);
        col.addView(tag);
        sp.addView(col, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        root.addView(sp, new FrameLayout.LayoutParams(-1, -1));
        logo.setScaleX(0.2f); logo.setScaleY(0.2f); logo.setRotation(-180); logo.setAlpha(0f);
        logo.animate().scaleX(1f).scaleY(1f).rotation(0).alpha(1f).setDuration(750).setInterpolator(new OvershootInterpolator(1.4f)).start();
        for (View v : new View[]{nm, tag}) { v.setAlpha(0f); v.setTranslationY(ui.dp(20)); v.animate().alpha(1f).translationY(0).setStartDelay(350).setDuration(450).start(); }
        sp.animate().alpha(0f).scaleX(1.15f).scaleY(1.15f).setStartDelay(1500).setDuration(380)
                .withEndAction(() -> root.removeView(sp)).start();
    }

    /** 3D tilt that follows the finger, springs back on release. */
    private void tilt(View host, View target) {
        host.setOnTouchListener((v, e) -> {
            float nx = e.getX() / Math.max(1, v.getWidth()) - 0.5f, ny = e.getY() / Math.max(1, v.getHeight()) - 0.5f;
            if (e.getActionMasked() == android.view.MotionEvent.ACTION_UP || e.getActionMasked() == android.view.MotionEvent.ACTION_CANCEL)
                target.animate().rotationX(0).rotationY(0).setDuration(500).setInterpolator(new OvershootInterpolator(2f)).start();
            else { target.setRotationX(-ny * 22); target.setRotationY(nx * 22); }
            return true;
        });
    }

    @Override protected void onDestroy() { main.removeCallbacks(tick); pool.shutdownNow(); super.onDestroy(); }

    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() {
        if (overlay != null) closeDetail();
        else if (screen != 0) show(0);
        else super.onBackPressed();
    }

    // ------------------------------------------------------------------ navigation
    private View buildNav() {
        LinearLayout n = ui.row();
        GradientDrawable g = new GradientDrawable();
        g.setColor(Ui.CARD);
        float r = ui.dp(22);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        n.setBackground(g); n.setElevation(ui.dp(Ui.LIGHT ? 10 : 0));
        n.setPadding(0, ui.dp(9), 0, ui.dp(9));
        for (int i = 0; i < NAV.length; i++) {
            final int idx = i;
            LinearLayout it = ui.col();
            it.setGravity(Gravity.CENTER);
            navIcons[i] = new Ui.Icon(this, i);
            it.addView(navIcons[i], new LinearLayout.LayoutParams(ui.dp(22), ui.dp(22)));
            navLabels[i] = ui.text(NAV[i], 11, Ui.MUTED, true);
            navLabels[i].setPadding(0, ui.dp(3), 0, 0);
            navLabels[i].setGravity(Gravity.CENTER);
            navLabels[i].setSingleLine(true);
            it.addView(navLabels[i]);
            it.setOnClickListener(v -> show(idx));
            ui.pressable(it);
            n.addView(it, new LinearLayout.LayoutParams(0, -2, 1));
        }
        return n;
    }

    private void show(int s) {
        if (overlay != null) closeDetail();
        boolean same = s == screen;
        screen = s;
        for (int i = 0; i < NAV.length; i++) {
            navIcons[i].setColor(i == s ? Ui.ACCENT2 : Ui.MUTED);
            navLabels[i].setTextColor(i == s ? Ui.TEXT : Ui.MUTED);
        }
        if (!same) navIcons[s].animate().scaleX(1.2f).scaleY(1.2f).setDuration(120)
                .withEndAction(() -> navIcons[s].animate().scaleX(1f).scaleY(1f).setDuration(160).start()).start();
        if (screenView != null) stage.removeView(screenView);
        preview = null; installBtn = null; gridBox = null; chipRow = null; aodCard = null; panel = null; studioName = null;
        View v;
        switch (s) {
            case 0: v = buildGallery(); break;
            case 1: v = buildStudio(); break;
            case 2: v = buildMyFaces(); break;
            default: v = buildWatch();
        }
        screenView = v;
        stage.addView(v, 0, new FrameLayout.LayoutParams(Math.min(screenW(), contentW()), -1, Gravity.CENTER_HORIZONTAL));
        if (!same) Ui.fadeIn(v, 0);
        if (s == 1) refresh();
    }

    private int screenW() { return getResources().getDisplayMetrics().widthPixels; }
    private int screenH() { return getResources().getDisplayMetrics().heightPixels; }

    /** Width available to screen content (capped so tablets/foldables get a centred column). */
    private int contentW() { return Math.min(screenW(), ui.dp(MAX_CONTENT_DP)); }
    private static final int MAX_CONTENT_DP = 720;

    /** Grid columns by width: 2 on phones, 3 on large phones/foldables, 4 on tablets. */
    private int cols() {
        float wdp = contentW() / getResources().getDisplayMetrics().density;
        return wdp >= 680 ? 4 : wdp >= 520 ? 3 : 2;
    }

    private GradientDrawable heroBg(float r) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Ui.HERO_A, Ui.HERO_B, Ui.HERO_C});
        g.setCornerRadius(ui.dp(r));
        if (Ui.LIGHT) g.setStroke(Math.max(1, ui.dp(0.8f)), Ui.LINE);
        return g;
    }

    private static String layoutName(Kind k) { return k == Kind.GALAXY ? "Big clock" : "Tiles + AM/PM"; }

    private static String fontName(Design d) {
        if (!d.customFont) return "Original digits";
        if (!d.fontFile.isEmpty()) return d.fontFile;
        for (int i = 0; i < Glyphs.FAMILIES.length; i++) if (Glyphs.FAMILIES[i].equals(d.fontFamily)) return Glyphs.FAMILY_NAMES[i];
        return d.fontFamily;
    }

    private TextView heading(String title) {
        TextView t = ui.text(title, 28, Ui.TEXT, true);
        Ui.gradientText(t, Ui.HEAD_A, Ui.HEAD_B, Ui.ACCENT2);
        return t;
    }

    // ------------------------------------------------------------------ gallery
    private View buildGallery() {
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout c = ui.col();
        c.setPadding(ui.dp(18), ui.dp(16), ui.dp(18), ui.dp(24));

        LinearLayout head = ui.row();
        LinearLayout t = ui.col();
        LinearLayout brand = ui.row();
        brand.addView(new Ui.Logo(this), new LinearLayout.LayoutParams(ui.dp(40), ui.dp(40)));
        TextView hd = heading("Lumina");
        hd.setPadding(ui.dp(10), 0, 0, 0);
        brand.addView(hd);
        t.addView(brand);
        t.addView(ui.text(Presets.ALL.length + " original faces · " + Art.NAMES.length + " art styles", 13, Ui.MUTED, false));
        head.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        TextView dice = ui.pill("🎲 Surprise", false, v -> surprise());
        ui.pressable(dice);
        head.addView(dice);
        c.addView(head);

        c.addView(hero());

        EditText search = field(query, "🔍  Search faces or styles");
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence q, int a, int b, int c2) {}
            @Override public void onTextChanged(CharSequence q, int a, int b, int c2) {}
            @Override public void afterTextChanged(Editable q) { query = q.toString().trim().toLowerCase(Locale.ROOT); if (gridBox != null) fillGrid(); }
        });
        LinearLayout.LayoutParams slp2 = new LinearLayout.LayoutParams(-1, -2);
        slp2.bottomMargin = ui.dp(6);
        c.addView(search, slp2);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        chipRow = ui.row();
        chipRow.setPadding(0, ui.dp(4), 0, ui.dp(6));
        hs.addView(chipRow);
        c.addView(hs);
        fillChips();

        gridBox = ui.col();
        c.addView(gridBox);
        fillGrid();

        LinearLayout own = ui.card();
        own.setBackground(heroBg(22)); ui.lift(own);
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(-1, -2);
        olp.topMargin = ui.dp(10);
        own.setLayoutParams(olp);
        own.addView(ui.text("Make your own", 18, Ui.TEXT, true));
        own.addView(ui.text("Your photo, any font, glow, stickers, text — with a live preview.", 13, Ui.MUTED, false));
        own.addView(ui.button("＋ Start from a photo", true, v -> { design = new Design(); design.bgMode = 0; saveState(); show(1); pick("image/*", PICK_PHOTO); }));
        own.addView(ui.button("＋ Blank canvas", false, v -> { design = new Design(); photo = null; photoRotFor = -1; saveState(); show(1); }));
        c.addView(own);

        TextView note = ui.text("Sparkles, rain, fireflies and the seconds ring are live in the app. The Halo's face format stores a still picture, so on the watch you get the crisp image with its ticking clock.", 12, Ui.MUTED, false);
        note.setPadding(ui.dp(4), ui.dp(14), ui.dp(4), 0);
        c.addView(note);
        sv.addView(c);
        return sv;
    }

    private View hero() {
        final int idx = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % Presets.ALL.length;
        Presets.Spec s = Presets.ALL[idx];
        FrameLayout card = new FrameLayout(this);
        card.setBackground(heroBg(26));
        ui.lift(card);
        LinearLayout r = ui.row();
        r.setPadding(ui.dp(14), ui.dp(16), ui.dp(16), ui.dp(16));
        int sz = Math.max(ui.dp(96), Math.min(ui.dp(150), (int) (contentW() * 0.36f)));
        FrameLayout art = new FrameLayout(this);
        art.addView(new Ui.Ring(this, ui.dp(2.5f)), new FrameLayout.LayoutParams(sz, sz, Gravity.CENTER));
        ImageView iv = new ImageView(this);
        art.addView(iv, new FrameLayout.LayoutParams(sz - ui.dp(22), sz - ui.dp(22), Gravity.CENTER));
        art.addView(new Ui.LiveFx(this, s.art, s.glow > 0 ? s.glowColor : s.time, true), new FrameLayout.LayoutParams(sz - ui.dp(22), sz - ui.dp(22), Gravity.CENTER));
        loadThumb(idx, iv);
        r.addView(art, new LinearLayout.LayoutParams(sz, sz));
        LinearLayout tx = ui.col();
        tx.setPadding(ui.dp(14), 0, 0, 0);
        TextView k = ui.text("FACE OF THE DAY", 11, Ui.ACCENT2, true);
        k.setLetterSpacing(0.14f);
        tx.addView(k);
        TextView heroName = ui.text(s.name, 22, Ui.TEXT, true);
        heroName.setMaxLines(2);
        heroName.setEllipsize(TextUtils.TruncateAt.END);
        tx.addView(heroName);
        tx.addView(ui.text(s.category + " · " + layoutName(s.layout), 12, Ui.MUTED, false));
        TextView open = ui.pill("View  →", true, v -> openPreset(idx));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = ui.dp(12);
        tx.addView(open, lp);
        r.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(r);
        card.setOnClickListener(v -> openPreset(idx));
        ui.pressable(card);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.setMargins(0, ui.dp(16), 0, ui.dp(14));
        card.setLayoutParams(clp);
        return card;
    }

    private void fillChips() {
        chipRow.removeAllViews();
        for (int i = 0; i < Presets.CATEGORIES.length; i++) {
            final int idx = i;
            chipRow.addView(ui.pill(Presets.CATEGORIES[i], i == category, v -> { category = idx; fillChips(); fillGrid(); }));
        }
    }

    private void fillGrid() {
        gridBox.removeAllViews();
        LinearLayout row = null;
        int n = 0;
        for (int i = 0; i < Presets.ALL.length; i++) {
            if (category != 0 && !Presets.ALL[i].category.equals(Presets.CATEGORIES[category])) continue;
            if (!query.isEmpty()) {
                Presets.Spec ps = Presets.ALL[i];
                String hay = (ps.name + " " + ps.category + " " + Art.NAMES[ps.art]).toLowerCase(Locale.ROOT);
                if (!hay.contains(query)) continue;
            }
            if (n % cols() == 0) { row = ui.row(); row.setGravity(Gravity.TOP); gridBox.addView(row, new LinearLayout.LayoutParams(-1, -2)); }
            View card = faceCard(i);
            row.addView(card, gridLp(n));
            Ui.fadeIn(card, Math.min(n, 10) * 35L);
            n++;
        }
        for (int k = n; row != null && k % cols() != 0; k++) row.addView(new View(this), gridLp(k));
        if (n == 0) gridBox.addView(ui.text("No faces match “" + query + "”. Try 🎲 Surprise!", 13, Ui.MUTED, false));
    }

    private LinearLayout.LayoutParams gridLp(int n) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        int c = cols(), col = n % c;
        lp.setMargins(col == 0 ? 0 : ui.dp(5), ui.dp(6), col == c - 1 ? 0 : ui.dp(5), ui.dp(6));
        return lp;
    }

    private View faceCard(int i) {
        Presets.Spec s = Presets.ALL[i];
        LinearLayout c = ui.col();
        c.setBackground(ui.surface(22)); ui.lift(c);
        c.setPadding(ui.dp(10), ui.dp(12), ui.dp(10), ui.dp(12));
        Ui.Square iv = new Ui.Square(this);
        c.addView(iv, new LinearLayout.LayoutParams(-1, -2));
        TextView name = ui.text(s.name, 14, Ui.TEXT, true);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setPadding(ui.dp(4), ui.dp(10), ui.dp(4), 0);
        c.addView(name);
        TextView sub = ui.text(s.category + " · " + layoutName(s.layout), 11, Ui.MUTED, false);
        sub.setPadding(ui.dp(4), ui.dp(2), ui.dp(4), 0);
        c.addView(sub);
        c.setOnClickListener(v -> openPreset(i));
        ui.pressable(c);
        loadThumb(i, iv);
        return c;
    }

    private void loadThumb(int i, ImageView iv) {
        iv.setTag(i);
        Bitmap have = thumbs[i];
        if (have != null) { iv.setImageBitmap(have); return; }
        GradientDrawable ph = new GradientDrawable();
        ph.setShape(GradientDrawable.OVAL);
        ph.setColor(Ui.CARD2);
        iv.setImageDrawable(ph);
        pool.execute(() -> {
            Bitmap bm = galleryThumb(i);
            if (bm == null) return;
            runOnUiThread(() -> {
                if (!Integer.valueOf(i).equals(iv.getTag())) return;
                iv.setImageBitmap(bm);
                iv.setAlpha(0f); iv.setScaleX(0.86f); iv.setScaleY(0.86f);
                iv.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(340).setInterpolator(new DecelerateInterpolator()).start();
            });
        });
    }

    private File galleryCache() { File f = new File(getCacheDir(), "gallery"); f.mkdirs(); return f; }

    private Bitmap galleryThumb(int i) {
        synchronized (thumbLocks[i]) {
            if (thumbs[i] != null) return thumbs[i];
            Design d = Design.fromPreset(Presets.ALL[i]);
            boolean full = template(d.layout) != null;
            try {
                String key = Integer.toHexString((d.toJson().toString() + GALLERY_VER + full + prefs.getBoolean("centerClock", false)).hashCode());
                File f = new File(galleryCache(), i + "_" + key + ".png");
                Bitmap bm = f.exists() ? BitmapFactory.decodeFile(f.getPath()) : null;
                if (bm == null) {
                    bm = watchBitmap(facePixels(d, null, true), THUMB_PX);
                    try (FileOutputStream fo = new FileOutputStream(f)) { bm.compress(Bitmap.CompressFormat.PNG, 100, fo); }
                }
                thumbs[i] = bm;
                return bm;
            } catch (Exception e) {
                android.util.Log.e("HaloStudio", "thumb " + i, e);
                return null;
            }
        }
    }

    private void openPreset(int i) {
        Presets.Spec s = Presets.ALL[i];
        openDetail(Design.fromPreset(s), null, null, s.category + " · " + layoutName(s.layout) + " · " + fontName(Design.fromPreset(s)));
    }

    // ------------------------------------------------------------------ surprise
    private void surprise() {
        status("Rolling the dice…");
        render.post(() -> {
            Design d = makeSurprise(rnd);
            runOnUiThread(() -> openDetail(d, null, null, "Generated · " + Art.NAMES[d.artId] + " art · " + fontName(d)));
        });
    }

    /** A random but readable design: random art, palette and font; text colours picked from the art's brightness. */
    private Design makeSurprise(Random r) {
        Design d = new Design();
        d.bgMode = 3;
        d.artId = r.nextInt(Art.NAMES.length);
        d.artPal = ART_PALETTES[r.nextInt(ART_PALETTES.length)].clone();
        d.artSeed = 1 + r.nextInt(99999);
        boolean moonOk = template(Kind.MOON) != null, galaxyOk = template(Kind.GALAXY) != null;
        d.layout = (moonOk && (!galaxyOk || r.nextInt(4) == 0)) ? Kind.MOON : Kind.GALAXY;
        d.customFont = true;
        d.fontFamily = SURPRISE_FONTS[r.nextInt(SURPRISE_FONTS.length)];
        d.fontBold = r.nextInt(3) == 0;
        d.upperLabels = r.nextBoolean();
        int accent = d.artPal[0], dark = d.artPal[0];
        for (int c : d.artPal) { if (lum(c) > lum(accent)) accent = c; if (lum(c) < lum(dark)) dark = c; }
        if (d.layout == Kind.MOON) {
            d.tileColor = Art.mix(dark, 0xFF000000, 0.55f);
            d.timeColor = Art.mix(accent, 0xFFFFFFFF, 0.35f);
            d.dateColor = 0xFFFFFFFF;
            d.glow = 4; d.glowColor = accent;
        } else {
            int[] a = Design.art(d.artId, d.artPal, d.artSeed);
            double sum = 0; int n = 0;
            for (int y = 110; y < 330; y += 4) for (int x = 60; x < 406; x += 4) { sum += lum(a[y * Design.S + x]); n++; }
            boolean light = sum / n > 0.6;
            d.timeColor = light ? 0xFF111111 : 0xFFFFFFFF;
            d.dateColor = light ? 0xFF333333 : Art.mix(accent, 0xFFFFFFFF, 0.4f);
            d.glow = light ? 0 : (r.nextBoolean() ? 0 : 6 + r.nextInt(6));
            d.glowColor = accent;
            if (!light && r.nextInt(4) == 0) { d.timeGradient = true; d.timeColor2 = accent; }
        }
        d.name = "Surprise #" + (100 + r.nextInt(900));
        return d;
    }

    private static double lum(int c) { return (0.299 * ((c >> 16) & 255) + 0.587 * ((c >> 8) & 255) + 0.114 * (c & 255)) / 255.0; }

    /** Harmonious random palette: dark base → two related hues → light accent. */
    private int[] randomPalette() {
        float h = rnd.nextFloat() * 360, spread = 25 + rnd.nextFloat() * 120;
        return new int[]{
                android.graphics.Color.HSVToColor(new float[]{h, 0.7f, 0.12f}),
                android.graphics.Color.HSVToColor(new float[]{h, 0.75f, 0.55f}),
                android.graphics.Color.HSVToColor(new float[]{(h + spread) % 360, 0.65f, 0.9f}),
                android.graphics.Color.HSVToColor(new float[]{(h + spread * 2) % 360, 0.25f, 1f})};
    }

    private static int[][] palettes() {
        LinkedHashMap<String, int[]> m = new LinkedHashMap<>();
        for (Presets.Spec s : Presets.ALL) m.putIfAbsent(Arrays.toString(s.pal), s.pal);
        return m.values().toArray(new int[0][]);
    }

    // ------------------------------------------------------------------ detail
    private void openDetail(Design d, File dir, Bitmap ph, String label) {
        if (overlay != null) { stage.removeView(overlay); overlay = null; }
        detail = d; detailDir = dir; detailPhoto = ph; detailRaw = null;
        overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Ui.BG);
        overlay.setClickable(true);
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout c = ui.col();
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(ui.dp(18), ui.dp(12), ui.dp(18), ui.dp(24));

        LinearLayout top = ui.row();
        top.addView(ui.pill("←  Back", false, v -> closeDetail()));
        top.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        if (d.bgMode == 3) {
            TextView sh = ui.pill("🎲 Shuffle art", false, v -> { detail.artSeed = 1 + rnd.nextInt(99999); renderDetail(true); });
            ((LinearLayout.LayoutParams) sh.getLayoutParams()).rightMargin = 0;
            top.addView(sh);
        }
        c.addView(top, new LinearLayout.LayoutParams(-1, -2));

        int sz = Math.min(Math.min(contentW() - ui.dp(36), ui.dp(380)), (int) (screenH() * 0.46f));
        FrameLayout st = new FrameLayout(this);
        View glow = new View(this);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        gd.setGradientRadius(sz * 0.5f);
        gd.setColors(new int[]{(d.glowColor & 0x00FFFFFF) | 0x55000000, 0x228B5CF6, 0x00000000});
        glow.setBackground(gd);
        st.addView(glow, new FrameLayout.LayoutParams(sz, sz, Gravity.CENTER));
        Ui.breathe(glow);
        st.addView(new Ui.Ring(this, ui.dp(2.5f)), new FrameLayout.LayoutParams(sz - ui.dp(14), sz - ui.dp(14), Gravity.CENTER));
        detailView = new ImageView(this);
        detailPx = Math.min(900, sz - ui.dp(48));
        st.addView(detailView, new FrameLayout.LayoutParams(sz - ui.dp(48), sz - ui.dp(48), Gravity.CENTER));
        if (prefs.getBoolean("liveFx", true))
            st.addView(new Ui.LiveFx(this, d.bgMode == 3 ? d.artId : 13, d.hideClock && d.artPal.length > 0 ? d.artPal[d.artPal.length - 1] : d.glow > 0 ? d.glowColor : d.timeColor, true),
                    new FrameLayout.LayoutParams(sz - ui.dp(48), sz - ui.dp(48), Gravity.CENTER));
        st.setCameraDistance(ui.dp(1600));
        tilt(st, st);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(sz, sz);
        slp.topMargin = ui.dp(6);
        c.addView(st, slp);

        TextView nm = ui.text(d.name, 24, Ui.TEXT, true);
        nm.setGravity(Gravity.CENTER);
        c.addView(nm);
        TextView sub = ui.text(label, 13, Ui.MUTED, false);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, ui.dp(2), 0, ui.dp(6));
        c.addView(sub);
        LinearLayout chips = ui.row();
        chips.setGravity(Gravity.CENTER);
        boolean fxOn = prefs.getBoolean("liveFx", true);
        TextView live = ui.chip(fxOn ? "● Live · tap to pause" : "○ Animations off");
        live.setTextColor(Ui.GOOD);
        live.setOnClickListener(v -> { prefs.edit().putBoolean("liveFx", !fxOn).apply(); openDetail(detail, detailDir, detailPhoto, label); });
        chips.addView(live);
        if (d.glow > 0) chips.addView(ui.chip("Glow"));
        if (d.timeGradient) chips.addView(ui.chip("Gradient"));
        if (d.outline > 0) chips.addView(ui.chip("Outline"));
        if (d.bgMode == 3) chips.addView(ui.chip(Art.NAMES[Math.max(0, Math.min(Art.NAMES.length - 1, d.artId))] + " art"));
        HorizontalScrollView chs = new HorizontalScrollView(this);
        chs.setHorizontalScrollBarEnabled(false);
        chs.addView(chips);
        c.addView(chs, new LinearLayout.LayoutParams(-2, -2));

        detailInstall = ui.button(busy ? "✕  Cancel install" : "Install on watch", true, v -> install(detail, detailPhoto, false));
        
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(-1, -2);
        ilp.topMargin = ui.dp(14);
        c.addView(detailInstall, ilp);
        LinearLayout r = ui.row();
        TextView cust = ui.button("Customize", false, v -> customize());
        TextView second = dir == null
                ? ui.button("♡ Save", false, v -> saveDesign(detail, detailPhoto))
                : ui.button("Delete", false, v -> confirmDelete(dir));
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, -2, 1); a.rightMargin = ui.dp(5); a.topMargin = ui.dp(8);
        LinearLayout.LayoutParams bb = new LinearLayout.LayoutParams(0, -2, 1); bb.leftMargin = ui.dp(5); bb.topMargin = ui.dp(8);
        r.addView(cust, a);
        r.addView(second, bb);
        c.addView(r, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout tip = ui.card();
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, -2);
        tlp.topMargin = ui.dp(16);
        tip.setLayoutParams(tlp);
        tip.addView(ui.text(d.bgMode == 3
                ? "Shuffle art makes a brand-new variation of this background. Customize lets you change colours, font, glow, stickers and text."
                : "Customize opens this face in the Studio.", 12, Ui.MUTED, false));
        c.addView(tip);

        sv.addView(c);
        overlay.addView(sv);
        stage.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        overlay.setAlpha(0f);
        overlay.setTranslationY(ui.dp(40));
        overlay.animate().alpha(1f).translationY(0f).setDuration(260).setInterpolator(new DecelerateInterpolator()).start();
        detailView.setScaleX(0.8f); detailView.setScaleY(0.8f);
        detailView.animate().scaleX(1f).scaleY(1f).setDuration(460).setInterpolator(new OvershootInterpolator(1.3f)).start();
        renderDetail(false);
        main.removeCallbacks(tick);
        main.postDelayed(tick, msToNextMinute());
    }

    private void renderDetail(boolean animate) {
        final int my = ++dseq;
        final Design d = detail.copy();
        final Bitmap ph = detailPhoto;
        final int px = detailPx;
        render.post(() -> {
            if (my != dseq) return;
            try {
                byte[] raw = template(d.layout) != null ? compose(d, bgPhoto(d, ph)) : null;
                Bitmap bm = watchBitmap(raw != null ? livePreview(d.layout, raw) : d.renderBackground(bgPhoto(d, ph)), px);
                runOnUiThread(() -> {
                    if (my != dseq || detailView == null) return;
                    detailRaw = raw;
                    detailView.setImageBitmap(bm);
                    if (animate) {
                        detailView.setAlpha(0.2f); detailView.setRotation(-14f);
                        detailView.animate().alpha(1f).rotation(0f).setDuration(380).setInterpolator(new DecelerateInterpolator()).start();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> status("Preview: " + e.getMessage()));
            }
        });
    }

    private void tickDetail() {
        if (overlay == null) return;
        final byte[] raw = detailRaw;
        final Kind k = detail.layout;
        final int px = detailPx;
        if (raw != null) render.post(() -> {
            Bitmap bm = watchBitmap(livePreview(k, raw), px);
            runOnUiThread(() -> { if (detailView != null && raw == detailRaw) detailView.setImageBitmap(bm); });
        });
        main.postDelayed(tick, msToNextMinute());
    }

    private static long msToNextMinute() { return 60050 - System.currentTimeMillis() % 60000; }

    private void closeDetail() {
        main.removeCallbacks(tick);
        if (overlay == null) return;
        final View o = overlay;
        overlay = null; detailView = null; detailInstall = null; dseq++;
        o.animate().alpha(0f).translationY(ui.dp(30)).setDuration(200).withEndAction(() -> stage.removeView(o)).start();
    }

    private void customize() {
        design = detail.copy();
        if (detailPhoto != null) { photo = detailPhoto; photoRotFor = -1; savePhoto(); }
        selSticker = -1; tab = 0;
        saveState();
        show(1);
        status("Editing “" + design.name + "” — changes save automatically.");
    }

    private void confirmDelete(File dir) {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Delete this face?")
                .setMessage("It is removed from My faces on this phone. Your watch is not changed.")
                .setPositiveButton("Delete", (x, y) -> { deleteDir(dir); closeDetail(); show(2); status("Deleted."); })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ------------------------------------------------------------------ my faces
    private View buildMyFaces() {
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout c = ui.col();
        c.setPadding(ui.dp(18), ui.dp(16), ui.dp(18), ui.dp(24));
        c.addView(heading("My faces"));
        File[] all = designsDir().listFiles(File::isDirectory);
        int n = all == null ? 0 : all.length;
        c.addView(ui.text(n == 0 ? "Your saved faces live here" : n + " saved on this phone", 13, Ui.MUTED, false));
        if (n == 0) {
            LinearLayout e = ui.card();
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = ui.dp(20);
            e.setLayoutParams(lp);
            e.setGravity(Gravity.CENTER_HORIZONTAL);
            Ui.Icon heart = new Ui.Icon(this, 2);
            heart.setColor(Ui.ACCENT1);
            LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(ui.dp(56), ui.dp(56));
            hl.setMargins(0, ui.dp(12), 0, ui.dp(8));
            e.addView(heart, hl);
            Ui.breathe(heart);
            TextView t1 = ui.text("No saved faces yet", 17, Ui.TEXT, true);
            t1.setGravity(Gravity.CENTER);
            e.addView(t1);
            TextView t2 = ui.text("Tap ♡ Save on any gallery face, or save your own design from the Studio.", 13, Ui.MUTED, false);
            t2.setGravity(Gravity.CENTER);
            e.addView(t2);
            e.addView(ui.button("Browse the gallery", true, v -> show(0)));
            e.addView(ui.button("Open the Studio", false, v -> show(1)));
            c.addView(e);
        } else {
            Arrays.sort(all, (a, b) -> b.getName().compareTo(a.getName()));
            LinearLayout grid = ui.col();
            grid.setPadding(0, ui.dp(10), 0, 0);
            LinearLayout row = null;
            for (int i = 0; i < n; i++) {
                if (i % cols() == 0) { row = ui.row(); row.setGravity(Gravity.TOP); grid.addView(row, new LinearLayout.LayoutParams(-1, -2)); }
                View card = savedCard(all[i]);
                row.addView(card, gridLp(i));
                Ui.fadeIn(card, Math.min(i, 10) * 35L);
            }
            for (int k = n; row != null && k % cols() != 0; k++) row.addView(new View(this), gridLp(k));
            c.addView(grid);
        }
        sv.addView(c);
        return sv;
    }

    private View savedCard(File dir) {
        String name = "My face";
        try { name = new JSONObject(new String(Files.readAllBytes(new File(dir, "design.json").toPath()), StandardCharsets.UTF_8)).optString("name", name); }
        catch (Exception ignored) { }
        LinearLayout c = ui.col();
        c.setBackground(ui.surface(22)); ui.lift(c);
        c.setPadding(ui.dp(10), ui.dp(12), ui.dp(10), ui.dp(12));
        Ui.Square iv = new Ui.Square(this);
        c.addView(iv, new LinearLayout.LayoutParams(-1, -2));
        TextView nm = ui.text(name, 14, Ui.TEXT, true);
        nm.setSingleLine(true);
        nm.setEllipsize(TextUtils.TruncateAt.END);
        nm.setPadding(ui.dp(4), ui.dp(10), ui.dp(4), 0);
        c.addView(nm);
        TextView sub = ui.text(savedDate(dir), 11, Ui.MUTED, false);
        sub.setPadding(ui.dp(4), ui.dp(2), ui.dp(4), 0);
        c.addView(sub);
        c.setOnClickListener(v -> openSaved(dir));
        ui.pressable(c);
        pool.execute(() -> {
            File tf = new File(dir, "thumb.png");
            Bitmap bm = tf.exists() ? BitmapFactory.decodeFile(tf.getPath()) : null;
            if (bm != null) runOnUiThread(() -> { iv.setImageBitmap(bm); iv.setAlpha(0f); iv.animate().alpha(1f).setDuration(260).start(); });
        });
        return c;
    }

    private static String savedDate(File dir) {
        try { return "Saved " + new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(new Date(Long.parseLong(dir.getName()))); }
        catch (Exception e) { return "Saved"; }
    }

    private void openSaved(File dir) {
        try {
            Design d = Design.fromJson(new JSONObject(new String(Files.readAllBytes(new File(dir, "design.json").toPath()), StandardCharsets.UTF_8)));
            File p = new File(dir, "photo.jpg");
            Bitmap ph = p.exists() ? BitmapFactory.decodeFile(p.getPath()) : null;
            openDetail(d, dir, ph, "My face · " + savedDate(dir).replace("Saved ", ""));
        } catch (Exception e) { status("Couldn't open that face."); }
    }

    // ------------------------------------------------------------------ studio
    private View buildStudio() {
        LinearLayout root = ui.col();
        root.setPadding(ui.dp(18), ui.dp(12), ui.dp(18), 0);
        LinearLayout head = ui.row();
        LinearLayout t = ui.col();
        t.addView(ui.text("Studio", 24, Ui.TEXT, true));
        studioName = ui.text(design.name, 13, Ui.MUTED, false);
        t.addView(studioName);
        head.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        TextView save = ui.pill("♡ Save", false, v -> saveDesign(design, photo));
        ((LinearLayout.LayoutParams) save.getLayoutParams()).rightMargin = 0;
        head.addView(save);
        root.addView(head);

        FrameLayout pv = new FrameLayout(this);
        int size = Math.max(ui.dp(150), Math.min(Math.min(contentW() - ui.dp(36), ui.dp(300)), (int) (screenH() * 0.27f)));
        View halo = new View(this);
        GradientDrawable hd = new GradientDrawable();
        hd.setShape(GradientDrawable.OVAL);
        hd.setColor(0x00000000);
        hd.setStroke(ui.dp(2), (Ui.ACCENT1 & 0x00FFFFFF) | 0x77000000);
        halo.setBackground(hd);
        pv.addView(halo, new FrameLayout.LayoutParams(size + ui.dp(12), size + ui.dp(12), Gravity.CENTER));
        preview = new ImageView(this);
        pv.addView(preview, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        root.addView(pv, new LinearLayout.LayoutParams(-1, size + ui.dp(20)));
        touchHint = ui.text("", 11, Ui.MUTED, false);
        touchHint.setGravity(Gravity.CENTER);
        root.addView(touchHint);
        setupTouch(size);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        tabRow = ui.row();
        tabRow.setPadding(0, ui.dp(8), 0, 0);
        hs.addView(tabRow);
        root.addView(hs);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        panel = ui.col();
        panel.setPadding(0, ui.dp(12), 0, ui.dp(12));
        sv.addView(panel);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        installBtn = ui.button(busy ? "✕  Cancel install" : "Install on watch", true, v -> install(design, photo, false));
        installBtn.setAlpha(busy ? 0.5f : 1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, ui.dp(4), 0, ui.dp(4));
        root.addView(installBtn, lp);
        buildTabs();
        buildPanel();
        return root;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupTouch(int viewSize) {
        final float f = Composer.W / (float) viewSize;
        final float[] last = new float[2];
        ScaleGestureDetector sg = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector det) {
                String t = TABS[tab];
                if (t.equals("Background") && design.bgMode == 0) {
                    design.zoom = Math.max(1f, Math.min(6f, design.zoom * det.getScaleFactor()));
                    refresh();
                } else if (t.equals("Stickers") && selSticker >= 0 && selSticker < design.stickers.size()) {
                    Design.Sticker s = design.stickers.get(selSticker);
                    s.size = Math.max(12, Math.min(300, s.size * det.getScaleFactor()));
                    refresh();
                }
                return true;
            }
        });
        preview.setOnTouchListener((v, e) -> {
            sg.onTouchEvent(e);
            if (e.getPointerCount() > 1) { last[0] = Float.NaN; return true; }
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    last[0] = e.getX(); last[1] = e.getY();
                    if (TABS[tab].equals("Stickers")) pickStickerAt(e.getX() * f, e.getY() * f);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (Float.isNaN(last[0])) { last[0] = e.getX(); last[1] = e.getY(); return true; }
                    float dx = (e.getX() - last[0]) * f, dy = (e.getY() - last[1]) * f;
                    last[0] = e.getX(); last[1] = e.getY();
                    dragBy(dx, dy);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    saveState();
                    buildPanelSoft();
                    return true;
            }
            return true;
        });
    }

    private void dragBy(float dx, float dy) {
        String t = TABS[tab];
        if (t.equals("Background") && design.bgMode == 0 && photoRot != null) {
            int w = photoRot.getWidth(), h = photoRot.getHeight();
            float m = Math.min(w, h) / Math.max(1f, design.zoom);
            float sx = m / Composer.W;                         // photo px per face px
            if (w - m > 1) design.panX = clamp(design.panX - dx * sx / ((w - m) / 2f), -1, 1);
            if (h - m > 1) design.panY = clamp(design.panY - dy * sx / ((h - m) / 2f), -1, 1);
        } else if (t.equals("Stickers") && selSticker >= 0 && selSticker < design.stickers.size()) {
            Design.Sticker s = design.stickers.get(selSticker);
            s.x = clamp(s.x + dx, 0, Composer.W); s.y = clamp(s.y + dy, 0, Composer.H);
        } else if (t.equals("Text")) {
            design.textX = (int) clamp(design.textX + dx, 0, Composer.W); design.textY = (int) clamp(design.textY + dy, 20, Composer.H);
        } else if (t.equals("Clock") && design.layout == Kind.GALAXY) {
            int[] lim = Composer.moveLimits();
            design.dx = (int) clamp(design.dx + dx, lim[0], lim[1]); design.dy = (int) clamp(design.dy + dy, lim[2], lim[3]);
        } else return;
        refresh();
    }

    private void pickStickerAt(float x, float y) {
        int best = -1; float bd = Float.MAX_VALUE;
        for (int i = 0; i < design.stickers.size(); i++) {
            Design.Sticker s = design.stickers.get(i);
            float d = (s.x - x) * (s.x - x) + (s.y - y) * (s.y - y);
            if (d < bd && d < (s.size * s.size)) { bd = d; best = i; }
        }
        if (best >= 0) selSticker = best;
    }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }

    private void updateTouchHint() {
        String t = TABS[tab], h = "";
        if (t.equals("Background") && design.bgMode == 0) h = "Drag to move · pinch to zoom the photo";
        else if (t.equals("Stickers")) h = "Tap a sticker · drag to move · pinch to resize";
        else if (t.equals("Text")) h = "Drag on the preview to move your text";
        else if (t.equals("Clock") && design.layout == Kind.GALAXY) h = "Drag on the preview to move the clock (experimental)";
        touchHint.setText(h);
    }

    private void buildTabs() {
        tabRow.removeAllViews();
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            tabRow.addView(ui.pill(TABS[i], i == tab, v -> { tab = idx; buildTabs(); buildPanel(); }));
        }
    }

    private void buildPanelSoft() { if (TABS[tab].equals("Stickers") || TABS[tab].equals("Background") || TABS[tab].equals("Clock")) buildPanel(); }

    private void buildPanel() {
        if (panel == null) return;
        panel.removeAllViews();
        updateTouchHint();
        switch (TABS[tab]) {
            case "Layout": panelLayout(); break;
            case "Background": panelBackground(); break;
            case "Effects": panelEffects(); break;
            case "Clock": panelClock(); break;
            case "Font": panelFont(); break;
            case "Stickers": panelStickers(); break;
            default: panelText();
        }
    }

    private void changed() { saveState(); refresh(); }

    private Switch toggle(String label, boolean on, Ui.IntCb cb) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setTextColor(Ui.TEXT);
        s.setChecked(on);
        s.setThumbTintList(new android.content.res.ColorStateList(new int[][]{{android.R.attr.state_checked}, {}}, new int[]{Ui.ACCENT1, Ui.CARD}));
        s.setTrackTintList(new android.content.res.ColorStateList(new int[][]{{android.R.attr.state_checked}, {}}, new int[]{(Ui.ACCENT1 & 0x00FFFFFF) | 0x88000000, Ui.MUTED}));
        s.setPadding(0, ui.dp(6), 0, ui.dp(6));
        s.setOnCheckedChangeListener((x, v) -> cb.on(v ? 1 : 0));
        return s;
    }

    private void panelLayout() {
        LinearLayout c = ui.card();
        c.addView(ui.title("Layout"));
        LinearLayout r = ui.row();
        r.addView(ui.pill("Galaxy · big clock", design.layout == Kind.GALAXY, v -> { design.layout = Kind.GALAXY; changed(); buildPanel(); }));
        r.addView(ui.pill("Moon · tiles + AM/PM", design.layout == Kind.MOON, v -> { design.layout = Kind.MOON; design.dx = 0; design.dy = 0; changed(); buildPanel(); }));
        c.addView(r);
        c.addView(ui.text(design.layout == Kind.GALAXY
                ? "\nGalaxy: digits drawn cleanly on your design, any font & colour."
                : "\nMoon: stacked clock with AM/PM; digits sit on solid tiles (tile colour in Clock).", 13, Ui.MUTED, false));
        if (template(design.layout) == null) c.addView(ui.text("\n⚠️ This layout's template is missing — Watch → Import template.", 13, Ui.WARN, false));
        panel.addView(c);

        LinearLayout n = ui.card();
        n.addView(ui.title("Name"));
        EditText et = field(design.name, "My face");
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c2) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c2) {}
            @Override public void afterTextChanged(Editable s) {
                design.name = s.toString().trim().isEmpty() ? "My face" : s.toString().trim();
                if (studioName != null) studioName.setText(design.name);
                saveState();
            }
        });
        n.addView(et);
        panel.addView(n);

        LinearLayout g = ui.card();
        g.addView(ui.title("Start from a ready-made face"));
        g.addView(ui.text(Presets.ALL.length + " original faces in the gallery — open one and tap Customize.", 13, Ui.MUTED, false));
        g.addView(ui.button("Open the gallery", false, v -> show(0)));
        g.addView(ui.button("🎲 Surprise me", false, v -> surprise()));
        panel.addView(g);
    }

    private void panelBackground() {
        LinearLayout c = ui.card();
        c.addView(ui.title("Source"));
        LinearLayout r = ui.row();
        String[] modes = {"Art", "Photo", "Gradient", "Solid"};
        int[] ids = {3, 0, 1, 2};
        for (int i = 0; i < modes.length; i++) {
            final int m = ids[i];
            r.addView(ui.pill(modes[i], design.bgMode == m, v -> { design.bgMode = m; changed(); buildPanel(); }));
        }
        HorizontalScrollView hs0 = new HorizontalScrollView(this);
        hs0.setHorizontalScrollBarEnabled(false);
        hs0.addView(r);
        c.addView(hs0);
        panel.addView(c);

        LinearLayout d = ui.card();
        if (design.bgMode == 3) {
            d.addView(ui.title("Art style"));
            HorizontalScrollView hs = new HorizontalScrollView(this);
            hs.setHorizontalScrollBarEnabled(false);
            LinearLayout ar = ui.row();
            for (int i = 0; i < Art.NAMES.length; i++) {
                final int id = i;
                ar.addView(ui.pill(Art.NAMES[i], design.artId == i, v -> { design.artId = id; changed(); buildPanel(); }));
            }
            hs.addView(ar);
            d.addView(hs);
            d.addView(ui.title("\nPalette"));
            d.addView(ui.gradientSwatches(ART_PALETTES, i -> { design.artPal = ART_PALETTES[i].clone(); changed(); }));
            LinearLayout pr = ui.row();
            pr.addView(ui.pill("🎨 Random palette", false, v -> { design.artPal = randomPalette(); changed(); }));
            pr.addView(ui.pill("⇄ Reverse", false, v -> { int[] a = design.artPal.clone(); for (int i = 0; i < a.length; i++) design.artPal[i] = a[a.length - 1 - i]; changed(); }));
            pr.addView(ui.pill("◐ Darker", false, v -> { for (int i = 0; i < design.artPal.length; i++) design.artPal[i] = Art.mix(design.artPal[i], 0xFF000000, 0.25f); changed(); }));
            HorizontalScrollView phs = new HorizontalScrollView(this); phs.setHorizontalScrollBarEnabled(false); phs.addView(pr); d.addView(phs);
            d.addView(ui.button("🎲 New variation", false, v -> { design.artSeed = 1 + rnd.nextInt(999); changed(); buildPanel(); }));
            d.addView(ui.slider("Variation", 1, 999, (int) Math.max(1, Math.min(999, design.artSeed)), x -> { design.artSeed = x; changed(); }));
            d.addView(ui.text("Every style × palette × variation is a different picture, generated on the phone.", 12, Ui.MUTED, false));
        } else if (design.bgMode == 0) {
            d.addView(ui.title("Photo & crop"));
            d.addView(ui.button(photo == null ? "Import photo" : "Change photo", true, v -> pick("image/*", PICK_PHOTO)));
            d.addView(ui.text("Drag on the preview to move · pinch to zoom", 12, Ui.MUTED, false));
            d.addView(ui.slider("Zoom ×100", 100, 600, Math.round(design.zoom * 100), x -> { design.zoom = x / 100f; changed(); }));
            d.addView(ui.slider("Left / right", -100, 100, Math.round(design.panX * 100), x -> { design.panX = x / 100f; changed(); }));
            d.addView(ui.slider("Up / down", -100, 100, Math.round(design.panY * 100), x -> { design.panY = x / 100f; changed(); }));
            LinearLayout rr = ui.row();
            rr.addView(ui.pill("⟲ Rotate", false, v -> { design.rotate = (design.rotate + 270) % 360; photoRotFor = -1; changed(); }));
            rr.addView(ui.pill("⟳ Rotate", false, v -> { design.rotate = (design.rotate + 90) % 360; photoRotFor = -1; changed(); }));
            rr.addView(ui.pill("Reset crop", false, v -> { design.zoom = 1; design.panX = 0; design.panY = 0; changed(); buildPanel(); }));
            d.addView(rr);
        } else if (design.bgMode == 1) {
            d.addView(ui.title("Gradient"));
            d.addView(ui.gradientSwatches(Ui.GRADIENTS, i -> { design.grad1 = Ui.GRADIENTS[i][0]; design.grad2 = Ui.GRADIENTS[i][1]; changed(); }));
            d.addView(ui.text("Colour 1", 13, Ui.MUTED, false));
            d.addView(ui.swatches(Ui.PALETTE, design.grad1, col -> { design.grad1 = col; changed(); buildPanel(); }));
            d.addView(ui.text("Colour 2", 13, Ui.MUTED, false));
            d.addView(ui.swatches(Ui.PALETTE, design.grad2, col -> { design.grad2 = col; changed(); buildPanel(); }));
            d.addView(ui.slider("Angle", 0, 359, design.gradAngle, x -> { design.gradAngle = x; changed(); }));
        } else {
            d.addView(ui.title("Solid colour"));
            d.addView(ui.swatches(Ui.PALETTE, design.solid, col -> { design.solid = col; changed(); buildPanel(); }));
        }
        panel.addView(d);
    }

    private void panelEffects() {
        LinearLayout c = ui.card();
        c.addView(ui.title("Effects"));
        c.addView(ui.slider("Brightness", -60, 60, design.brightness, x -> { design.brightness = x; changed(); }));
        c.addView(ui.slider("Contrast", 50, 160, design.contrast, x -> { design.contrast = x; changed(); }));
        c.addView(ui.slider("Saturation", 0, 200, design.saturation, x -> { design.saturation = x; changed(); }));
        c.addView(ui.slider("Blur", 0, 12, design.blur, x -> { design.blur = x; changed(); }));
        c.addView(ui.slider("Vignette", 0, 100, design.vignette, x -> { design.vignette = x; changed(); }));
        c.addView(toggle("Black & white", design.mono, v -> { design.mono = v == 1; changed(); }));
        c.addView(ui.slider("Hue shift", 0, 359, design.hue, x -> { design.hue = x; changed(); }));
        panel.addView(c);
        c = ui.card();
        c.addView(ui.title("Frame"));
        LinearLayout fr = ui.row();
        String[] frames = {"None", "Thin ring", "Neon ring", "Minute ticks", "Double ring"};
        for (int i = 0; i < frames.length; i++) { final int f = i; fr.addView(ui.pill(frames[i], design.frame == i, v -> { design.frame = f; changed(); buildPanel(); })); }
        HorizontalScrollView fhs = new HorizontalScrollView(this); fhs.setHorizontalScrollBarEnabled(false); fhs.addView(fr); c.addView(fhs);
        if (design.frame > 0) c.addView(ui.swatches(Ui.PALETTE, design.frameColor, col -> { design.frameColor = col; changed(); buildPanel(); }));
        c.addView(ui.title("\nTexture overlay"));
        LinearLayout ov = ui.row();
        String[] ovs = {"None", "Film grain", "Scanlines", "Glass sheen", "Dot grid"};
        for (int i = 0; i < ovs.length; i++) { final int f = i; ov.addView(ui.pill(ovs[i], design.overlay == i, v -> { design.overlay = f; changed(); buildPanel(); })); }
        HorizontalScrollView ohs = new HorizontalScrollView(this); ohs.setHorizontalScrollBarEnabled(false); ohs.addView(ov); c.addView(ohs);
        c.addView(ui.button("Reset effects", false, v -> {
            design.brightness = 0; design.contrast = 100; design.saturation = 100; design.blur = 0; design.vignette = 0; design.mono = false; design.hue = 0; design.frame = 0; design.overlay = 0;
            changed(); buildPanel();
        }));
        panel.addView(c);
    }

    private void panelClock() {
        if (design.layout == Kind.GALAXY) {
            LinearLayout h = ui.card();
            h.addView(ui.title("Art-only face"));
            h.addView(toggle("Hide time & date (show only the artwork)", design.hideClock, v -> { design.hideClock = v == 1; changed(); }));
            h.addView(ui.text("The watch still keeps time — raise your wrist and swipe to see it in the menus. Galaxy layout only.", 11, Ui.MUTED, false));
            panel.addView(h);
        }
        LinearLayout c = ui.card();
        c.addView(ui.title("Time colour"));
        c.addView(ui.swatches(Ui.PALETTE, design.timeColor, col -> { design.timeColor = col; changed(); buildPanel(); }));
        c.addView(toggle("Gradient time (needs custom font)", design.timeGradient, v -> { design.timeGradient = v == 1; if (v == 1) design.customFont = true; changed(); buildPanel(); }));
        if (design.timeGradient) {
            c.addView(ui.text("Gradient end colour", 13, Ui.MUTED, false));
            c.addView(ui.swatches(Ui.PALETTE, design.timeColor2, col -> { design.timeColor2 = col; changed(); buildPanel(); }));
        }
        c.addView(ui.title("Date & weekday colour"));
        c.addView(ui.swatches(Ui.PALETTE, design.dateColor, col -> { design.dateColor = col; changed(); buildPanel(); }));
        if (design.layout == Kind.MOON) {
            c.addView(ui.title("Tile colour"));
            c.addView(ui.swatches(Ui.PALETTE, design.tileColor, col -> { design.tileColor = col; changed(); buildPanel(); }));
        }
        panel.addView(c);
        if (design.layout == Kind.GALAXY) {
            int[] lim = Composer.moveLimits();
            LinearLayout p = ui.card();
            p.addView(ui.title("Clock position · experimental"));
            p.addView(ui.text("Not yet tried on the watch. If it ever shows the default face, use Watch → Restore.", 12, Ui.WARN, false));
            p.addView(ui.slider("Left / right", lim[0], lim[1], design.dx, x -> { design.dx = x; changed(); }));
            p.addView(ui.slider("Up / down", lim[2], lim[3], design.dy, x -> { design.dy = x; changed(); }));
            LinearLayout r = ui.row();
            r.addView(ui.pill("Original", false, v -> { design.dx = 0; design.dy = 0; changed(); buildPanel(); }));
            r.addView(ui.pill("Centered", false, v -> { design.dx = -14; design.dy = 30; changed(); buildPanel(); }));
            r.addView(ui.pill("Low", false, v -> { design.dx = -14; design.dy = 110; changed(); buildPanel(); }));
            p.addView(r);
            panel.addView(p);
        }
    }

    private void panelFont() {
        LinearLayout c = ui.card();
        c.addView(ui.title("Clock font"));
        LinearLayout r = ui.row();
        r.addView(ui.pill("Original", !design.customFont, v -> { design.customFont = false; design.timeGradient = false; changed(); buildPanel(); }));
        r.addView(ui.pill("Custom", design.customFont, v -> { design.customFont = true; changed(); buildPanel(); }));
        c.addView(r);
        if (design.customFont) {
            HorizontalScrollView hs = new HorizontalScrollView(this);
            hs.setHorizontalScrollBarEnabled(false);
            LinearLayout fr = ui.row();
            fr.setPadding(0, ui.dp(10), 0, ui.dp(4));
            for (int i = 0; i < Glyphs.FAMILIES.length; i++) {
                final String fam = Glyphs.FAMILIES[i];
                fr.addView(ui.pill(Glyphs.FAMILY_NAMES[i], design.fontFile.isEmpty() && design.fontFamily.equals(fam),
                        v -> { design.fontFamily = fam; design.fontFile = ""; changed(); buildPanel(); }));
            }
            hs.addView(fr);
            c.addView(hs);
            c.addView(ui.button(design.fontFile.isEmpty() ? "Import font (.ttf / .otf)" : "Font: " + design.fontFile + " (tap to change)", false, v -> pick("*/*", PICK_FONT)));
            c.addView(toggle("Bold", design.fontBold, v -> { design.fontBold = v == 1; changed(); }));
            c.addView(toggle("Italic", design.fontItalic, v -> { design.fontItalic = v == 1; changed(); }));
            c.addView(toggle("UPPERCASE weekdays", design.upperLabels, v -> { design.upperLabels = v == 1; changed(); }));
            c.addView(ui.slider("Time size %", 50, 100, design.timeScale, x -> { design.timeScale = x; changed(); }));
            c.addView(ui.slider("Date size %", 50, 100, design.dateScale, x -> { design.dateScale = x; changed(); }));
            c.addView(ui.title("Separator"));
            LinearLayout sr = ui.row();
            for (String sep : new String[]{"/", "-", ".", "·", "|"}) sr.addView(ui.pill(sep, design.separator.equals(sep), v -> { design.separator = sep; changed(); buildPanel(); }));
            c.addView(sr);
            c.addView(ui.title("Outline"));
            c.addView(ui.slider("Outline width", 0, 6, design.outline, x -> { design.outline = x; changed(); }));
            c.addView(ui.swatches(Ui.PALETTE, design.outlineColor, col -> { design.outlineColor = col; changed(); buildPanel(); }));
            c.addView(ui.title("Glow"));
            c.addView(ui.slider("Glow radius", 0, 14, design.glow, x -> { design.glow = x; changed(); }));
            c.addView(ui.swatches(Ui.PALETTE, design.glowColor, col -> { design.glowColor = col; changed(); buildPanel(); }));
        } else {
            c.addView(ui.text("\nOriginal = the watch's own digit shapes, recoloured and drawn cleanly on your design.", 12, Ui.MUTED, false));
        }
        panel.addView(c);
    }

    private void panelStickers() {
        LinearLayout c = ui.card();
        c.addView(ui.title("Add a sticker"));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout er = ui.row();
        for (String e : EMOJI) {
            TextView t = ui.text(e, 26, Ui.TEXT, false);
            t.setPadding(ui.dp(6), ui.dp(4), ui.dp(6), ui.dp(4));
            t.setOnClickListener(v -> addSticker(e));
            ui.pressable(t);
            er.addView(t);
        }
        hs.addView(er);
        c.addView(hs);
        EditText custom = field("", "Or type any emoji / symbol and tap Add");
        c.addView(custom);
        c.addView(ui.button("Add", false, v -> { String s = custom.getText().toString().trim(); if (!s.isEmpty()) addSticker(s); }));
        panel.addView(c);

        if (!design.stickers.isEmpty()) {
            LinearLayout l = ui.card();
            l.addView(ui.title("Stickers (tap to select)"));
            LinearLayout r = ui.row();
            for (int i = 0; i < design.stickers.size(); i++) {
                final int idx = i;
                r.addView(ui.pill(design.stickers.get(i).text, i == selSticker, v -> { selSticker = idx; buildPanel(); }));
            }
            HorizontalScrollView h2 = new HorizontalScrollView(this);
            h2.addView(r);
            l.addView(h2);
            if (selSticker >= 0 && selSticker < design.stickers.size()) {
                Design.Sticker s = design.stickers.get(selSticker);
                l.addView(ui.slider("Size", 12, 300, Math.round(s.size), x -> { s.size = x; changed(); }));
                l.addView(ui.slider("Rotate", -180, 180, Math.round(s.rot), x -> { s.rot = x; changed(); }));
                l.addView(ui.button("Remove sticker", false, v -> { design.stickers.remove(selSticker); selSticker = -1; changed(); buildPanel(); }));
            }
            panel.addView(l);
        }
    }

    private void addSticker(String e) {
        Design.Sticker s = new Design.Sticker();
        s.text = e; s.x = 233; s.y = 380; s.size = 64;
        design.stickers.add(s);
        selSticker = design.stickers.size() - 1;
        changed(); buildPanel();
    }

    private void panelText() {
        LinearLayout c = ui.card();
        c.addView(ui.title("Your text"));
        EditText et = field(design.text, "Name, quote…");
        et.setSingleLine(false);
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c2) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c2) {}
            @Override public void afterTextChanged(Editable s) { design.text = s.toString(); changed(); }
        });
        c.addView(et);
        c.addView(ui.text("Drag on the preview to place it", 12, Ui.MUTED, false));
        c.addView(ui.slider("Size", 14, 80, design.textSize, x -> { design.textSize = x; changed(); }));
        c.addView(ui.swatches(Ui.PALETTE, design.textColor, col -> { design.textColor = col; changed(); buildPanel(); }));
        c.addView(toggle("Bold", design.textBold, v -> { design.textBold = v == 1; changed(); }));
        panel.addView(c);
    }

    // ------------------------------------------------------------------ watch screen
    private View buildWatch() {
        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout root = ui.col();
        root.setPadding(ui.dp(18), ui.dp(16), ui.dp(18), ui.dp(24));
        root.addView(heading("Watch"));
        root.addView(ui.text("Connection, always-on display and settings", 13, Ui.MUTED, false));

        LinearLayout wr = ui.card();
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(-1, -2);
        wlp.setMargins(0, ui.dp(16), 0, ui.dp(12));
        wr.setLayoutParams(wlp);
        wr.setBackground(heroBg(22)); ui.lift(wr);
        LinearLayout row = ui.row();
        ImageView iv = new ImageView(this);
        File wf = new File(getFilesDir(), "wrist.png");
        if (wf.exists()) iv.setImageBitmap(BitmapFactory.decodeFile(wf.getPath()));
        else { GradientDrawable ph = new GradientDrawable(); ph.setShape(GradientDrawable.OVAL); ph.setColor(Ui.CARD2); iv.setImageDrawable(ph); }
        row.addView(iv, new LinearLayout.LayoutParams(ui.dp(96), ui.dp(96)));
        LinearLayout tx = ui.col();
        tx.setPadding(ui.dp(14), 0, 0, 0);
        TextView k = ui.text("ON YOUR WRIST", 11, Ui.ACCENT2, true);
        k.setLetterSpacing(0.14f);
        tx.addView(k);
        tx.addView(ui.text(wf.exists() ? prefs.getString("wristName", "Custom face") : "Nothing sent yet", 18, Ui.TEXT, true));
        tx.addView(ui.text(wf.exists() ? prefs.getString("wristWhen", "") : "Pick a face in the gallery and tap Install.", 12, Ui.MUTED, false));
        row.addView(tx, new LinearLayout.LayoutParams(0, -2, 1));
        wr.addView(row);
        root.addView(wr);

        LinearLayout c = ui.card();
        c.addView(ui.title("Before installing"));
        c.addView(ui.text("1. Close NoiseFit completely (Force stop).\n2. Keep the watch next to the phone.\n3. Battery above 30%.", 13, Ui.MUTED, false));
        c.addView(ui.button("Open NoiseFit → Force stop", false, v ->
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.noisefit")))));
        c.addView(ui.button("Test watch connection", false, v -> simpleJob(new WatchLink.Job(null, 0, null, "Watch connected and verified."), "Testing connection...")));
        c.addView(ui.button("Restore galaxy face (safe fallback)", false, v -> install(design, null, true)));
        c.addView(toggle("Also update the face-list picture on the watch (experimental)", design.updateThumb, v -> { design.updateThumb = v == 1; saveState(); }));
        c.addView(ui.text("Applies to faces installed from the Studio. Off = the watch keeps its own small picture in the face list.", 11, Ui.MUTED, false));
        root.addView(c);

        LinearLayout wd = ui.card();
        wd.addView(ui.title("Watch display"));
        wd.addView(toggle("Centre the big clock on the round screen", prefs.getBoolean("centerClock", false), v -> {
            prefs.edit().putBoolean("centerClock", v == 1).apply(); clearThumbs(); }));
        wd.addView(ui.text("Moves hours + minutes down/left so the top digit isn't cut by the round edge. Experimental: install one face to check; if the watch shows its default face, use Restore galaxy face.", 11, Ui.MUTED, false));
        wd.addView(toggle("My watch shows 24-hour time", watch24(), v -> { prefs.edit().putBoolean("watch24", v == 1).apply(); }));
        wd.addView(ui.text("Previews use this so the clock in the app matches your wrist (the watch follows NoiseFit's time-format setting).", 11, Ui.MUTED, false));
        root.addView(wd);

        aodCard = ui.card();
        fillAod();
        root.addView(aodCard);

        LinearLayout s = ui.card();
        s.addView(ui.title("Settings"));
        s.addView(toggle("Bright beige theme (off = dark)", Ui.LIGHT, v -> { prefs.edit().putBoolean("lightTheme", v == 1).apply(); recreate(); }));
        s.addView(ui.text("NoiseFit user ID (sent like NoiseFit does on every connect)", 12, Ui.MUTED, false));
        EditText tok = field(prefs.getString("token", ""), "e.g. 0a1b2c3d4e5f…");
        tok.addTextChangedListener(saver("token"));
        s.addView(tok);
        s.addView(ui.text("Watch Bluetooth address (blank = find paired 'Halo')", 12, Ui.MUTED, false));
        EditText addr = field(prefs.getString("addr", ""), "AA:BB:CC:DD:EE:FF");
        addr.addTextChangedListener(saver("addr"));
        s.addView(addr);
        s.addView(ui.title("\nFace templates"));
        s.addView(ui.text("Galaxy: " + galaxySrc + "   ·   Moon: " + moonSrc, 13, Ui.TEXT, false));
        s.addView(ui.text("Import an official face file captured from your own watch (see README).", 12, Ui.MUTED, false));
        s.addView(ui.button("Import template file", false, v -> pick("*/*", PICK_TEMPLATE)));
        root.addView(s);

        LinearLayout sf = ui.card();
        sf.addView(ui.title("Safety"));
        sf.addView(ui.text("Every face is built and fully re-checked on this phone before sending. The app can only send the user check, the watch-face READY question, the AOD read/write and the face it built — it has no access to firmware. The watch checks every face again and rejects anything it doesn't trust.", 12, Ui.MUTED, false));
        root.addView(sf);
        sv.addView(root);
        return sv;
    }

    private void fillAod() {
        if (aodCard == null) return;
        LinearLayout a = aodCard;
        a.removeAllViews();
        a.addView(ui.title("Always-on display (AOD) · experimental"));
        a.addView(ui.text("The AOD look is built into the watch (analog or digital). Here you choose the style and when it is on.", 12, Ui.MUTED, false));
        TextView aodView = ui.text(aod == null ? "Not read yet." : "Watch: " + aod, 13, Ui.TEXT, true);
        aodView.setPadding(0, ui.dp(8), 0, ui.dp(4));
        a.addView(aodView);
        a.addView(ui.button("Read AOD settings from watch", false, v -> simpleJob(new WatchLink.Job(Aod.getCommand(), Aod.CMD_GET, null, "AOD settings read."), "Reading AOD settings...")));
        if (aod != null) {
            LinearLayout mr = ui.row();
            String[] modes = {"Always on", "Scheduled", "Off"};
            for (int i = 0; i < 3; i++) { final int m = i; mr.addView(ui.pill(modes[i], aod.mode == m, v -> { aod.mode = m; if (m == 1) { aod.hasStart = true; aod.hasEnd = true; } fillAod(); })); }
            a.addView(mr);
            LinearLayout sr = ui.row();
            sr.setPadding(0, ui.dp(8), 0, 0);
            sr.addView(ui.pill("Analog", aod.style == 0, v -> { aod.style = 0; fillAod(); }));
            sr.addView(ui.pill("Digital", aod.style == 1, v -> { aod.style = 1; fillAod(); }));
            a.addView(sr);
            if (aod.mode == Aod.MODE_SCHEDULED) {
                a.addView(ui.slider("Start hour", 0, 23, aod.sh, x -> aod.sh = x));
                a.addView(ui.slider("Start minute", 0, 59, aod.sm, x -> aod.sm = x));
                a.addView(ui.slider("End hour", 0, 23, aod.eh, x -> aod.eh = x));
                a.addView(ui.slider("End minute", 0, 59, aod.em, x -> aod.em = x));
            }
            a.addView(ui.button("Apply AOD settings to watch", true, v -> applyAod()));
        }
    }

    private EditText field(String value, String hint) {
        EditText e = new EditText(this);
        e.setText(value); e.setHint(hint); e.setSingleLine(true);
        e.setTextColor(Ui.TEXT); e.setHintTextColor(Ui.MUTED);
        e.setBackground(ui.rounded(Ui.CARD2, 14));
        e.setPadding(ui.dp(14), ui.dp(10), ui.dp(14), ui.dp(10));
        return e;
    }

    private TextWatcher saver(String key) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { prefs.edit().putString(key, s.toString().trim()).apply(); }
        };
    }

    // ------------------------------------------------------------------ config & templates
    private void loadLocalConfigDefaults() {
        if (prefs.contains("token")) return;
        try (InputStream in = getAssets().open("local_config.json")) {
            JSONObject o = new JSONObject(new String(readAll(in), StandardCharsets.UTF_8));
            prefs.edit().putString("token", o.optString("userToken", "")).putString("addr", o.optString("watchAddress", "")).apply();
        } catch (Exception ignored) { }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
        return o.toByteArray();
    }

    private static String sha(byte[] d) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (byte x : MessageDigest.getInstance("SHA-256").digest(d)) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private File templateFile(Kind k) { File d = new File(getFilesDir(), "templates"); d.mkdirs(); return new File(d, k.name().toLowerCase() + ".raw"); }

    private void loadTemplates() {
        galaxyT = loadTemplate(Kind.GALAXY, "template_galaxy.raw", Templates.GALAXY_SHA);
        moonT = loadTemplate(Kind.MOON, "template_moon.raw", Templates.MOON_SHA);
    }

    private byte[] loadTemplate(Kind k, String asset, String wantSha) {
        try (InputStream in = getAssets().open(asset)) {
            byte[] d = readAll(in);
            if (sha(d).equals(wantSha)) { if (k == Kind.GALAXY) galaxySrc = "built-in"; else moonSrc = "built-in"; return d; }
        } catch (Exception ignored) { }
        try {
            File f = templateFile(k);
            if (f.exists()) {
                byte[] d = Files.readAllBytes(f.toPath());
                if (Composer.identify(d) == k) { if (k == Kind.GALAXY) galaxySrc = "imported"; else moonSrc = "imported"; return d; }
            }
        } catch (Exception ignored) { }
        return null;
    }

    private byte[] template(Kind k) { return k == Kind.GALAXY ? galaxyT : moonT; }

    // ------------------------------------------------------------------ rendering
    private Bitmap rotatedPhoto() {
        if (photo == null) return null;
        if (photoRotFor != design.rotate || photoRot == null) { photoRot = design.rotated(photo); photoRotFor = design.rotate; }
        return photoRot;
    }

    /** The (rotated) photo a design needs as background, or null when it doesn't use one. */
    private Bitmap bgPhoto(Design d, Bitmap ph) {
        if (d.bgMode != 0 || ph == null) return null;
        if (ph == photo && d.rotate == design.rotate) return rotatedPhoto();
        return d.rotated(ph);
    }

    private byte[] compose(Design d, Bitmap ph) {
        byte[] t = template(d.layout);
        if (t == null) throw new IllegalStateException("template missing for " + d.layout);
        int[] bg = d.renderBackground(ph);
        Composer.GlyphPainter painter = d.hideClock && d.layout == Kind.GALAXY
                ? (role, idx, w, h) -> new int[w * h]          // fully transparent: digits/labels vanish into the art
                : d.customFont ? new Glyphs(d, fontsDir()) : null;
        if (d.layout == Kind.GALAXY) {
            int dx = d.dx, dy = d.dy;
            if (dx == 0 && dy == 0 && prefs.getBoolean("centerClock", false)) { dx = CENTER_DX; dy = CENTER_DY; }
            return Composer.compose(Kind.GALAXY, t, bg, d.timeColor, d.dateColor, 0, dx, dy, painter);
        }
        return Composer.compose(Kind.MOON, t, bg, d.timeColor, d.dateColor, d.tileColor, 0, 0, painter);
    }

    private boolean watch24() { return prefs.getBoolean("watch24", DateFormat.is24HourFormat(this)); }

    /** Galaxy shift that centres the time block (record x/y move; 94..400 x 51..302 -> 80..386 x 108..359). */
    static final int CENTER_DX = -14, CENTER_DY = 57;

    private void clearThumbs() { java.util.Arrays.fill(thumbs, null); }

    private int[] livePreview(Kind k, byte[] raw) {
        Calendar cal = Calendar.getInstance();
        int wd = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;   // Monday = 0
        return Composer.preview(k, raw, cal.get(Calendar.HOUR_OF_DAY), !watch24(),
                cal.get(Calendar.MINUTE), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), wd, false);
    }

    /** Full face pixels; a fixed 10:08 for gallery pictures, the art alone when the template is missing. */
    private int[] facePixels(Design d, Bitmap ph, boolean fixedTime) {
        if (template(d.layout) == null) return d.renderBackground(ph);
        byte[] raw = compose(d, ph);
        return fixedTime ? Composer.preview(d.layout, raw, 10, false, 8, 10, 24, 4, false) : livePreview(d.layout, raw);
    }

    private void refresh() {
        final int my = ++seq;
        final Design d = design.copy();
        render.post(() -> {
            if (my != seq) return;
            try {
                if (template(d.layout) == null) {
                    runOnUiThread(() -> { if (preview != null) preview.setImageBitmap(null); status("Template missing — Watch → Import template."); });
                    return;
                }
                byte[] raw = compose(d, d.bgMode == 0 ? rotatedPhoto() : null);
                Bitmap round = roundBitmap(livePreview(d.layout, raw), Composer.W);
                if (my == seq) runOnUiThread(() -> { if (preview != null) preview.setImageBitmap(round); });
            } catch (Exception e) {
                runOnUiThread(() -> status("Preview: " + e.getMessage()));
            }
        });
    }

    private static Bitmap roundBitmap(int[] px, int out) {
        Bitmap sq = Bitmap.createBitmap(px, Composer.W, Composer.H, Bitmap.Config.ARGB_8888);
        Bitmap res = Bitmap.createBitmap(out, out, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(res);           // transparent outside the round face (no square corners)
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        Bitmap scaled = out == Composer.W ? sq : Bitmap.createScaledBitmap(sq, out, out, true);
        p.setShader(new BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        c.drawCircle(out / 2f, out / 2f, out / 2f, p);
        return res;
    }

    /** The face inside a metal bezel, like a product shot (transparent outside the watch). */
    static Bitmap watchBitmap(int[] px, int out) {
        Bitmap sq = Bitmap.createBitmap(px, Composer.W, Composer.H, Bitmap.Config.ARGB_8888);
        Bitmap res = Bitmap.createBitmap(out, out, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(res);
        float cx = out / 2f, rOuter = out * 0.5f - 1, rFace = out * 0.448f;
        Paint bz = new Paint(Paint.ANTI_ALIAS_FLAG);
        bz.setShader(new SweepGradient(cx, cx, new int[]{0xFF4B4F5C, 0xFF16171D, 0xFF5E6372, 0xFF16171D, 0xFF4B4F5C}, null));
        c.drawCircle(cx, cx, rOuter, bz);
        Paint in = new Paint(Paint.ANTI_ALIAS_FLAG);
        in.setColor(0xFF050507);
        c.drawCircle(cx, cx, rFace + out * 0.014f, in);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        BitmapShader sh = new BitmapShader(sq, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Matrix m = new Matrix();
        float sc = 2 * rFace / Composer.W;
        m.setScale(sc, sc);
        m.postTranslate(cx - rFace, cx - rFace);
        sh.setLocalMatrix(m);
        p.setShader(sh);
        c.drawCircle(cx, cx, rFace, p);
        Paint gl = new Paint(Paint.ANTI_ALIAS_FLAG);
        gl.setShader(new LinearGradient(0, cx - rFace, 0, cx, 0x1CFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cx, rFace, gl);
        Paint hl = new Paint(Paint.ANTI_ALIAS_FLAG);
        hl.setStyle(Paint.Style.STROKE);
        hl.setStrokeWidth(Math.max(1f, out * 0.005f));
        hl.setColor(0x40FFFFFF);
        c.drawCircle(cx, cx, rOuter - out * 0.004f, hl);
        return res;
    }

    // ------------------------------------------------------------------ pickers
    private void pick(String type, int code) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType(type);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(i, "Choose a file"), code);
    }

    private File fontsDir() { File f = new File(getFilesDir(), "fonts"); f.mkdirs(); return f; }

    private String displayName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) { int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (i >= 0) return c.getString(i); }
        } catch (Exception ignored) { }
        return "file";
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (req == PICK_PHOTO) loadPhoto(uri);
        else if (req == PICK_FONT) importFont(uri);
        else if (req == PICK_TEMPLATE) importTemplate(uri);
    }

    private void loadPhoto(Uri uri) {
        status("Loading photo...");
        render.post(() -> {
            try {
                Bitmap src = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri), (dec, info, s) -> {
                    dec.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    int m = Math.max(info.getSize().getWidth(), info.getSize().getHeight());
                    if (m > 2000) dec.setTargetSampleSize(Math.max(1, m / 2000));
                });
                photo = src.copy(Bitmap.Config.ARGB_8888, false);
                photoRotFor = -1;
                savePhoto();
                design.bgMode = 0; design.zoom = 1; design.panX = 0; design.panY = 0; design.rotate = 0;
                runOnUiThread(() -> {
                    status("Photo ready — drag/pinch the preview to crop.");
                    tab = 1;
                    if (screen != 1) show(1); else { buildTabs(); buildPanel(); }
                    changed();
                });
            } catch (Exception e) {
                runOnUiThread(() -> status("Couldn't open that photo: " + e.getMessage()));
            }
        });
    }

    private void savePhoto() {
        try (FileOutputStream fo = new FileOutputStream(new File(getFilesDir(), "current_photo.jpg"))) { photo.compress(Bitmap.CompressFormat.JPEG, 94, fo); }
        catch (Exception ignored) { }
    }

    private void importFont(Uri uri) {
        render.post(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                String name = displayName(uri).replaceAll("[^A-Za-z0-9._-]", "_");
                if (!name.toLowerCase().matches(".*\\.(ttf|otf)$")) throw new IllegalStateException("choose a .ttf or .otf font");
                File out = new File(fontsDir(), name);
                Files.write(out.toPath(), readAll(in));
                android.graphics.Typeface.createFromFile(out);   // throws if not a font
                design.customFont = true; design.fontFile = name;
                runOnUiThread(() -> { status("Font imported: " + name); changed(); buildPanel(); });
            } catch (Exception e) {
                runOnUiThread(() -> status("Font import failed: " + e.getMessage()));
            }
        });
    }

    private void importTemplate(Uri uri) {
        render.post(() -> {
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                byte[] f = readAll(in);
                byte[] raw = (f.length > 4 && (f[0] & 0xFF) == 0xAF) ? ZhDial.unpackOfficial(f) : f;
                Kind k = Composer.identify(raw);
                if (k == null) throw new IllegalStateException("not a supported Halo face (Galaxy 1246 or Moon 898 layout)");
                Files.write(templateFile(k).toPath(), raw);
                loadTemplates();
                Arrays.fill(thumbs, null);
                runOnUiThread(() -> { status("Template imported: " + k); show(screen); });
            } catch (Exception e) {
                runOnUiThread(() -> status("Template import failed: " + e.getMessage()));
            }
        });
    }

    // ------------------------------------------------------------------ state & designs
    private void saveState() {
        try { Files.write(new File(getFilesDir(), "current.json").toPath(), design.toJson().toString().getBytes(StandardCharsets.UTF_8)); }
        catch (Exception ignored) { }
    }

    private void loadState() {
        try {
            File f = new File(getFilesDir(), "current.json");
            if (f.exists()) design = Design.fromJson(new JSONObject(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8)));
            File p = new File(getFilesDir(), "current_photo.jpg");
            if (p.exists()) photo = BitmapFactory.decodeFile(p.getPath());
        } catch (Exception ignored) { }
    }

    private File designsDir() { File f = new File(getFilesDir(), "designs"); f.mkdirs(); return f; }

    private void saveDesign(Design src, Bitmap ph) {
        final Design d = src.copy();
        status("Saving…");
        render.post(() -> {
            try {
                File dir = new File(designsDir(), String.valueOf(System.currentTimeMillis()));
                dir.mkdirs();
                Files.write(new File(dir, "design.json").toPath(), d.toJson().toString().getBytes(StandardCharsets.UTF_8));
                if (d.bgMode == 0 && ph != null)
                    try (FileOutputStream fo = new FileOutputStream(new File(dir, "photo.jpg"))) { ph.compress(Bitmap.CompressFormat.JPEG, 94, fo); }
                Bitmap thumb = watchBitmap(facePixels(d, bgPhoto(d, ph), true), THUMB_PX);
                try (FileOutputStream fo = new FileOutputStream(new File(dir, "thumb.png"))) { thumb.compress(Bitmap.CompressFormat.PNG, 100, fo); }
                runOnUiThread(() -> status("♡ Saved “" + d.name + "” to My faces."));
            } catch (Exception e) {
                runOnUiThread(() -> status("Save failed: " + e.getMessage()));
            }
        });
    }

    private static void deleteDir(File d) {
        File[] fs = d.listFiles();
        if (fs != null) for (File f : fs) f.delete();
        d.delete();
    }

    // ------------------------------------------------------------------ install & AOD
    private void install(Design src, Bitmap ph, boolean restoreGalaxy) {
        if (busy || link.busy()) { cancelInstall(); return; }
        cancelReq = false;
        final Design d = src.copy();
        if (!restoreGalaxy && template(d.layout) == null) { status("Template missing — Watch → Import template."); return; }
        if (restoreGalaxy && galaxyT == null) { status("Galaxy template missing."); return; }
        if (!restoreGalaxy && d.bgMode == 0 && ph == null) { status("Import a photo first (Studio → Background)."); return; }
        setBusy(true);
        status(restoreGalaxy ? "Preparing galaxy face..." : "Building “" + d.name + "”...");
        render.post(() -> {
            try {
                byte[] face;
                String id, name;
                Bitmap wrist;
                if (restoreGalaxy) {
                    face = ZhDial.pack(galaxyT);
                    if (!Arrays.equals(ZhDial.unpack(face), galaxyT)) throw new IllegalStateException("self-check");
                    id = "1246";
                    name = "Galaxy (original)";
                    wrist = watchBitmap(Composer.preview(Kind.GALAXY, galaxyT, 10, false, 8, 10, 24, 4, false), 300);
                } else {
                    byte[] t = template(d.layout);
                    byte[] raw = compose(d, bgPhoto(d, ph));
                    if (d.updateThumb) {
                        Bitmap th = roundBitmap(livePreview(d.layout, raw), Composer.THUMB);
                        int[] px = new int[Composer.THUMB * Composer.THUMB];
                        th.getPixels(px, 0, Composer.THUMB, 0, 0, Composer.THUMB, Composer.THUMB);
                        Composer.writeThumbnail(raw, px);
                    }
                    String why = Composer.verifyAllowed(d.layout, t, raw, d.updateThumb);
                    if (why != null) throw new IllegalStateException(why);
                    face = ZhDial.pack(raw);
                    if (!Arrays.equals(ZhDial.unpack(face), raw)) throw new IllegalStateException("pack check");
                    id = d.layout == Kind.GALAXY ? "1246" : "898";
                    name = d.name;
                    wrist = watchBitmap(Composer.preview(d.layout, raw, 10, false, 8, 10, 24, 4, false), 300);
                }
                if (cancelReq) throw new InterruptedException();
                List<List<byte[]>> pieces = Upload.pieces(Upload.frameStream(face));
                runJob(new WatchLink.Job(Upload.precheck(id, face.length, true), 83, pieces, "Done! Look at your watch (give it a few seconds)."),
                        "Face ready (" + face.length / 1000 + " KB, checked). Connecting...", wrist, name);
            } catch (InterruptedException e) {
                runOnUiThread(() -> { setBusy(false); status("Cancelled — nothing was sent to the watch."); });
            } catch (Exception e) {
                runOnUiThread(() -> { setBusy(false); status("Not sent — self-check failed: " + e.getMessage()); });
            }
        });
    }

    private void applyAod() {
        if (aod == null) { status("Read the AOD settings first."); return; }
        if (!aod.valid()) { status("AOD values out of range."); return; }
        if (aod.mode == Aod.MODE_SCHEDULED) { aod.hasStart = true; aod.hasEnd = true; }
        simpleJob(new WatchLink.Job(aod.setCommand(), Aod.CMD_SET, null, "AOD settings sent: " + aod), "Sending AOD settings...");
    }

    private void runJob(WatchLink.Job job, String msg, Bitmap wrist, String wristName) {
        runOnUiThread(() -> {
            if (link.busy()) { setBusy(false); status("Busy — wait for the current job to finish."); return; }
            if (cancelReq) { setBusy(false); status("Cancelled — nothing was sent to the watch."); return; }
            sendStart = 0;
            pendingWrist = wrist; pendingWristName = wristName;
            setBusy(true); bar.setProgress(0); statusView.setText(msg);
            String token = prefs.getString("token", ""), addr = prefs.getString("addr", "");
            ble.post(() -> link.start(token, addr, job));
        });
    }

    private void setBusy(boolean b) {
        busy = b;
        bar.setVisibility(b ? View.VISIBLE : View.GONE);
        cancelBtn.setVisibility(b ? View.VISIBLE : View.GONE);
        for (TextView t : new TextView[]{installBtn, detailInstall}) {
            if (t == null) continue;
            t.setAlpha(1f);
            t.setText(b ? "✕  Cancel install" : "Install on watch");
        }
    }

    /** Cancels building or sending at any moment. */
    private void cancelInstall() {
        if (!busy && !link.busy()) return;
        cancelReq = true;
        status("Cancelling…");
        if (link.busy()) ble.post(link::cancel);
    }

    @Override public void status(String s) { runOnUiThread(() -> { statusView.setText(s); statusView.setAlpha(0.3f); statusView.animate().alpha(1f).setDuration(200).start(); }); }

    /** Connection test / AOD jobs: refused while a face is being built or sent. */
    private void simpleJob(WatchLink.Job job, String msg) {
        if (busy || link.busy()) { status("Busy — wait for the current job to finish."); return; }
        runJob(job, msg, null, null);
    }

    @Override public void progress(int done, int total) {
        runOnUiThread(() -> {
            long now = System.currentTimeMillis();
            if (done == 0 || sendStart == 0) sendStart = now;
            int pct = total == 0 ? 0 : done * 100 / total;
            bar.setProgress(pct);
            String eta = "";
            if (done > 0 && done < total) {
                long left = (now - sendStart) * (total - done) / done / 1000;
                eta = " · ~" + (left >= 60 ? (left / 60) + " min " + (left % 60) + " s" : left + " s") + " left";
            }
            statusView.setText("Sending to watch… " + pct + "% (" + done + "/" + total + ")" + eta);
        });
    }

    @Override public void finished(boolean ok, String msg) {
        runOnUiThread(() -> {
            setBusy(false);
            status((ok ? "✅ " : "⚠️ ") + msg);
            if (ok && pendingWrist != null) {
                try (FileOutputStream fo = new FileOutputStream(new File(getFilesDir(), "wrist.png"))) { pendingWrist.compress(Bitmap.CompressFormat.PNG, 100, fo); }
                catch (Exception ignored) { }
                prefs.edit().putString("wristName", pendingWristName)
                        .putString("wristWhen", "Sent " + new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(new Date())).apply();
                if (screen == 3 && overlay == null) show(3);
            }
            pendingWrist = null;
        });
    }

    @Override public void reply(int cmd, byte[] proto) {
        if (cmd == Aod.CMD_GET) {
            Aod a = Aod.parseReply(proto);
            runOnUiThread(() -> {
                if (a == null) { status("⚠️ Couldn't understand the watch's AOD reply."); return; }
                if (!a.roundTripOk()) { aod = null; status("⚠️ AOD format not as expected — editing disabled for safety."); fillAod(); return; }
                aod = a;
                status("AOD read: " + a);
                fillAod();
            });
        }
    }

    // ------------------------------------------------------------------ developer self-test (no Bluetooth)
    private void selfTest() {
        StringBuilder report = new StringBuilder();
        try { selfTestInto(report); } catch (Throwable t) { report.append("SELFTEST CRASH ").append(t).append('\n'); }
        try { Files.write(new File(getExternalFilesDir(null), "selftest.txt").toPath(), report.toString().getBytes(StandardCharsets.UTF_8)); }
        catch (Exception ignored) { }
    }

    private boolean checkFace(Design d, StringBuilder report, String label) {
        try {
            long t0 = SystemClock.uptimeMillis();
            byte[] t = template(d.layout);
            byte[] raw = compose(d, null);
            String why = Composer.verifyAllowed(d.layout, t, raw, false);
            byte[] face = ZhDial.pack(raw);
            boolean ok = why == null && Arrays.equals(ZhDial.unpack(face), raw) && Upload.pieces(Upload.frameStream(face)).size() > 0;
            String line = (ok ? "PASS " : "FAIL ") + label + " " + d.layout + " art=" + d.artId + " font=" + fontName(d)
                    + " size=" + face.length + " ms=" + (SystemClock.uptimeMillis() - t0) + (why != null ? " why=" + why : "");
            report.append(line).append('\n');
            return ok;
        } catch (Throwable e) {
            report.append("FAIL ").append(label).append(": ").append(e).append('\n');
            return false;
        }
    }

    private void selfTestInto(StringBuilder report) throws Exception {
        report.append("templates galaxy=").append(galaxySrc).append(" moon=").append(moonSrc).append('\n');
        int pass = 0, fail = 0;
        // 1. fonts & effects on both layouts
        String[] fams = Glyphs.FAMILIES;
        for (int i = 0; i < fams.length + 3; i++) {
            Design d = new Design();
            d.customFont = i < fams.length;
            d.layout = (i % 2 == 0) ? Kind.GALAXY : Kind.MOON;
            d.fontFamily = fams[i % fams.length];
            d.fontBold = i % 3 == 0; d.fontItalic = i % 4 == 0; d.upperLabels = i % 2 == 1;
            d.outline = i % 4; d.glow = (i * 3) % 12; d.timeGradient = d.customFont && i % 3 == 1;
            d.bgMode = i % 4 == 0 ? 1 : 3; d.artId = i % Art.NAMES.length;
            d.blur = i % 5; d.vignette = 30; d.mono = i == 5; d.text = "Halo ✨"; d.separator = i % 2 == 0 ? "/" : "-";
            Design.Sticker s = new Design.Sticker(); s.text = "🚀"; s.x = 120; s.y = 380; s.rot = 20; d.stickers.add(s);
            if (checkFace(d, report, "font" + i)) pass++; else fail++;
        }
        // 2. every preset + its gallery picture
        Bitmap sheet = Bitmap.createBitmap(4 * 200, ((Presets.ALL.length + 3) / 4) * 230, Bitmap.Config.ARGB_8888);
        Canvas sc = new Canvas(sheet);
        sc.drawColor(Ui.BG);
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(Ui.TEXT); tp.setTextSize(15); tp.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < Presets.ALL.length; i++) {
            boolean ok = checkFace(Design.fromPreset(Presets.ALL[i]), report, "preset[" + Presets.ALL[i].name + "]");
            Bitmap th = galleryThumb(i);
            if (th == null) { ok = false; report.append("FAIL gallery picture ").append(i).append('\n'); }
            else {
                int x = (i % 4) * 200, y = (i / 4) * 230;
                sc.drawBitmap(th, null, new Rect(x + 6, y + 6, x + 194, y + 194), null);
                sc.drawText(Presets.ALL[i].name, x + 100, y + 216, tp);
            }
            if (ok) pass++; else fail++;
        }
        try (FileOutputStream fo = new FileOutputStream(new File(getExternalFilesDir(null), "selftest_gallery.png"))) { sheet.compress(Bitmap.CompressFormat.PNG, 100, fo); }
        // 3. random "surprise" designs
        Random r = new Random(42);
        Bitmap sur = Bitmap.createBitmap(4 * 200, 2 * 200, Bitmap.Config.ARGB_8888);
        Canvas suc = new Canvas(sur);
        suc.drawColor(Ui.BG);
        for (int i = 0; i < 8; i++) {
            Design d = makeSurprise(r);
            if (checkFace(d, report, "surprise" + i)) pass++; else fail++;
            suc.drawBitmap(watchBitmap(facePixels(d, null, true), 200), (i % 4) * 200, (i / 4) * 200, null);
        }
        try (FileOutputStream fo = new FileOutputStream(new File(getExternalFilesDir(null), "selftest_surprise.png"))) { sur.compress(Bitmap.CompressFormat.PNG, 100, fo); }
        // 4. every art style x a few palettes (background only)
        for (int a = 0; a < Art.NAMES.length; a++) for (int p = 0; p < ART_PALETTES.length; p += 7) {
            try { int[] px = Art.render(a, ART_PALETTES[p], 5); if (px.length != Art.S * Art.S) throw new IllegalStateException("size"); pass++; }
            catch (Throwable e) { fail++; report.append("FAIL art ").append(a).append(" pal ").append(p).append(": ").append(e).append('\n'); }
        }
        android.util.Log.i("HaloStudioTest", "SELFTEST DONE pass=" + pass + " fail=" + fail);
        report.append("SELFTEST DONE pass=").append(pass).append(" fail=").append(fail).append('\n');
    }
}
