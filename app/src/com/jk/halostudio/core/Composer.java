package com.jk.halostudio.core;

import java.util.HashSet;
import java.util.Set;

/**
 * Composes a custom face from a template raw (element maps verified on the PC):
 *  - background 466x466 (RGB565 BE at 0x2F338)
 *  - every glyph set re-drawn over the new background:
 *      * Original font: template glyph recoloured (glyph == template bg -> new bg; white -> colour; edges blended)
 *      * Custom font:   glyphs supplied by a {@link GlyphPainter} (ARGB with alpha) blended over the new bg
 *  - GALAXY: all sets unique -> clean digits anywhere; the big time block can be shifted (record x/y).
 *  - MOON: shared sets -> solid tiles under every element so shared glyphs are valid at every position.
 *  - optional face-picker preview 310x310 (RGB565 BE at 0x470).
 * Sizes never change; {@link #verifyAllowed} enforces which bytes may differ from the template.
 */
public final class Composer {
    public static final int W = 466, H = 466, BG_OFF = 0x2F338, BG_LEN = W * H * 2;
    public static final int THUMB_OFF = 0x470, THUMB = 310, THUMB_LEN = THUMB * THUMB * 2;

    public enum Kind { GALAXY, MOON }
    public enum Role { HOUR_T, HOUR_U, MIN_T, MIN_U, MONTH_T, MONTH_U, SEP, DAY_T, DAY_U, WEEKDAY, AMPM }

    /** Labels drawn by a custom font, per image index. */
    public static final String[] WEEKDAY_TEXT = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun",
            "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    public static final String[] AMPM_TEXT = {"AM", "PM", "上午", "下午"};

    public interface GlyphPainter {
        /** @return w*h ARGB pixels (alpha = coverage) for this element image, already coloured. */
        int[] paint(Role role, int index, int w, int h);
    }

    public static final class El {
        public final Role role; public final int rec, x, y, w, h, n, off; final boolean second;
        El(Role role, int rec, boolean second, int x, int y, int w, int h, int n, int off) {
            this.role = role; this.rec = rec; this.second = second; this.x = x; this.y = y; this.w = w; this.h = h; this.n = n; this.off = off;
        }
        public boolean isTime() { return role == Role.HOUR_T || role == Role.HOUR_U || role == Role.MIN_T || role == Role.MIN_U; }
    }

    /** Face 1246 — all sets unique, background-matching, full byte coverage (verified). */
    public static final El[] GALAXY = {
            new El(Role.MONTH_T, 0x108, false, 300, 102, 22, 26, 10, 0x0993C0),
            new El(Role.MONTH_U, 0x108, true, 322, 102, 22, 26, 10, 0x09C070),
            new El(Role.SEP, 0x119, false, 344, 102, 14, 26, 1, 0x09ED20),
            new El(Role.DAY_T, 0x126, false, 358, 102, 22, 26, 10, 0x09EFF8),
            new El(Role.DAY_U, 0x126, true, 380, 102, 22, 26, 10, 0x0A1CA8),
            new El(Role.WEEKDAY, 0x173, false, 332, 134, 72, 32, 14, 0x0A4958),
            new El(Role.HOUR_T, 0x18D, false, 94, 51, 102, 114, 10, 0x0B4558),
            new El(Role.HOUR_U, 0x19A, false, 196, 51, 102, 114, 10, 0x0ED1C8),
            new El(Role.MIN_T, 0x1B4, false, 196, 188, 102, 114, 10, 0x125E38),
            new El(Role.MIN_U, 0x1C1, false, 298, 188, 102, 114, 10, 0x15EAA8),
    };

    /** Face 898 — date digits share one set, hour/minute tens share one set (verified). */
    public static final El[] MOON = {
            new El(Role.MONTH_T, 0x108, false, 38, 316, 20, 28, 10, 0x0993C0),
            new El(Role.MONTH_U, 0x108, true, 58, 316, 20, 28, 10, 0x0993C0),
            new El(Role.SEP, 0x119, false, 78, 316, 16, 28, 1, 0x09BF80),
            new El(Role.DAY_T, 0x126, false, 94, 316, 20, 28, 10, 0x0993C0),
            new El(Role.DAY_U, 0x126, true, 114, 316, 20, 28, 10, 0x0993C0),
            new El(Role.WEEKDAY, 0x173, false, 140, 316, 80, 30, 14, 0x09C300),
            new El(Role.AMPM, 0x180, false, 124, 53, 54, 26, 4, 0x0AC980),
            new El(Role.HOUR_T, 0x18D, false, 42, 92, 66, 99, 10, 0x0AF560),
            new El(Role.HOUR_U, 0x19A, false, 116, 92, 66, 99, 10, 0x0CF3D8),
            new El(Role.MIN_T, 0x1B4, false, 42, 198, 66, 99, 10, 0x0AF560),
            new El(Role.MIN_U, 0x1C1, false, 116, 198, 66, 99, 10, 0x0EF250),
    };

    public static El[] elements(Kind k) { return k == Kind.GALAXY ? GALAXY : MOON; }

    /** Identify a template raw by its header/record bytes (used for imported templates). */
    public static Kind identify(byte[] raw) {
        if (raw == null || raw.length < BG_OFF) return null;
        for (Kind k : Kind.values()) {
            boolean ok = true;
            for (El e : elements(k)) {
                if (e.second) continue;
                int off = be32(raw, e.rec + 9);
                if (be16(raw, e.rec + 4) != e.w || be16(raw, e.rec + 6) != e.h || (raw[e.rec + 8] & 0xFF) != e.n || off != e.off) { ok = false; break; }
            }
            int expectLen = k == Kind.GALAXY ? 0x197718 : 0x10F0C8;
            if (ok && raw.length == expectLen) return k;
        }
        return null;
    }

    private Composer() {}

    static int be16(byte[] d, int p) { return ((d[p] & 0xFF) << 8) | (d[p + 1] & 0xFF); }
    static int be32(byte[] d, int p) { return ((d[p] & 0xFF) << 24) | ((d[p + 1] & 0xFF) << 16) | ((d[p + 2] & 0xFF) << 8) | (d[p + 3] & 0xFF); }

    static void put565(byte[] d, int p, int argb) {
        int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
        int v = ((r >> 3) << 11) | ((g >> 2) << 5) | (b >> 3);
        d[p] = (byte) (v >> 8); d[p + 1] = (byte) v;
    }

    static int r8(int v) { return ((v >> 11) & 31) * 255 / 31; }
    static int g8(int v) { return ((v >> 5) & 63) * 255 / 63; }
    static int b8(int v) { return (v & 31) * 255 / 31; }
    public static int argb(int v) { return 0xFF000000 | (r8(v) << 16) | (g8(v) << 8) | b8(v); }

    static int blend(int fgArgb, float a, int bg) {
        int r = Math.round(a * ((fgArgb >> 16) & 0xFF) + (1 - a) * ((bg >> 16) & 0xFF));
        int g = Math.round(a * ((fgArgb >> 8) & 0xFF) + (1 - a) * ((bg >> 8) & 0xFF));
        int b = Math.round(a * (fgArgb & 0xFF) + (1 - a) * (bg & 0xFF));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Allowed shift of the big time block (Galaxy only) so it stays fully on screen. */
    public static int[] moveLimits() {
        int minX = -9999, maxX = 9999, minY = -9999, maxY = 9999;
        for (El s : GALAXY) {
            if (!s.isTime()) continue;
            minX = Math.max(minX, -s.x); maxX = Math.min(maxX, W - (s.x + s.w));
            minY = Math.max(minY, -s.y); maxY = Math.min(maxY, H - (s.y + s.h));
        }
        return new int[]{minX, maxX, minY, maxY};
    }

    /**
     * @param painter   null = recolour the template's own font; otherwise custom-font glyphs
     * @param tileColor MOON only: solid tile colour drawn under every element (ARGB)
     * @param dx,dy     GALAXY only: shift of the big time block
     */
    public static byte[] compose(Kind kind, byte[] template, int[] bgIn, int timeColor, int dateColor,
                                 int tileColor, int dx, int dy, GlyphPainter painter) {
        if (bgIn.length != W * H) throw new IllegalArgumentException("background must be 466x466");
        if (kind == Kind.MOON && (dx != 0 || dy != 0)) throw new IllegalArgumentException("moving is Galaxy-only");
        int[] lim = moveLimits();
        if (dx < lim[0] || dx > lim[1] || dy < lim[2] || dy > lim[3]) throw new IllegalArgumentException("clock off screen");
        El[] els = elements(kind);
        int[] bg = bgIn.clone();
        if (kind == Kind.MOON) {
            // Shared glyph sets need an identical colour under every position they appear at. Instead of a hard
            // box per element, draw two rounded "glass" panels (time block, date row) with a uniform core and a
            // soft feathered rim, plus a faint highlight border — reads as a design element, not a glitch.
            int tc = tileColor | 0xFF000000;
            panel(bg, 30, 82, 194, 305, 20, tc);      // hour/minute block (glyphs 42..182 x 92..297)
            panel(bg, 28, 309, 230, 353, 14, tc);     // date + weekday row (glyphs 38..220 x 316..346)
            panel(bg, 118, 47, 184, 83, 10, tc);      // AM/PM pill (unique set, just for consistency)
        }
        byte[] raw = template.clone();
        for (int i = 0; i < W * H; i++) put565(raw, BG_OFF + 2 * i, bg[i]);
        Set<Integer> done = new HashSet<>();
        for (El s : els) {
            boolean move = kind == Kind.GALAXY && s.isTime();
            int nx = move ? s.x + dx : s.x, ny = move ? s.y + dy : s.y;
            if (move && (dx != 0 || dy != 0)) {
                raw[s.rec] = (byte) (nx >> 8); raw[s.rec + 1] = (byte) nx;
                raw[s.rec + 2] = (byte) (ny >> 8); raw[s.rec + 3] = (byte) ny;
            }
            if (!done.add(s.off)) continue;
            int col = s.isTime() ? timeColor : dateColor;
            for (int g = 0; g < s.n; g++) {
                int base = s.off + g * s.w * s.h * 2;
                int[] pg = painter != null ? painter.paint(s.role, g, s.w, s.h) : null;
                for (int yy = 0; yy < s.h; yy++) {
                    for (int xx = 0; xx < s.w; xx++) {
                        int p = base + (yy * s.w + xx) * 2;
                        int nb = bg[(ny + yy) * W + (nx + xx)];
                        if (pg != null) {
                            int c = pg[yy * s.w + xx];
                            put565(raw, p, blend(c, ((c >>> 24) & 0xFF) / 255f, nb));
                            continue;
                        }
                        int v = be16(template, p);
                        int ob = be16(template, BG_OFF + ((s.y + yy) * W + (s.x + xx)) * 2);
                        if (v == ob) { put565(raw, p, nb); continue; }
                        float a = 1f, best = -1f;
                        int[][] ch = {{r8(v), r8(ob)}, {g8(v), g8(ob)}, {b8(v), b8(ob)}};
                        for (int[] cc : ch) {
                            int den = 255 - cc[1];
                            if (den >= 24) best = Math.max(best, (cc[0] - cc[1]) / (float) den);
                        }
                        if (best >= 0f) a = Math.min(1f, best);
                        put565(raw, p, blend(col, a, nb));
                    }
                }
            }
        }
        return raw;
    }

    /** Rounded panel [x0,x1) x [y0,y1): uniform colour inside, 1.5 px light rim, 6 px soft shadow outside. */
    static void panel(int[] bg, int x0, int y0, int x1, int y1, int rad, int col) {
        int rim = 0xFFFFFFFF;
        for (int y = Math.max(0, y0 - 8); y < Math.min(H, y1 + 8); y++)
            for (int x = Math.max(0, x0 - 8); x < Math.min(W, x1 + 8); x++) {
                float qx = Math.max(Math.max(x0 + rad - (x + 0.5f), (x + 0.5f) - (x1 - rad)), 0);
                float qy = Math.max(Math.max(y0 + rad - (y + 0.5f), (y + 0.5f) - (y1 - rad)), 0);
                float d = (float) Math.hypot(qx, qy) - rad;          // <0 inside the rounded rect
                int i = y * W + x;
                if (d <= -1.5f) bg[i] = col;
                else if (d <= 0f) bg[i] = blend(rim, 0.22f, col);
                else if (d < 1f) bg[i] = blend(blend(rim, 0.22f, col), 1 - d, bg[i]);
                else if (d < 8f) bg[i] = blend(0xFF000000, 0.35f * (1 - d / 8f), bg[i]);
            }
    }

    /** Writes the 310x310 face-picker preview (ARGB, should be round-masked with black outside). */
    public static void writeThumbnail(byte[] raw, int[] argb310) {
        if (argb310.length != THUMB * THUMB) throw new IllegalArgumentException("thumbnail must be 310x310");
        for (int i = 0; i < argb310.length; i++) put565(raw, THUMB_OFF + 2 * i, argb310[i]);
    }

    /** Safety check before sending: same length; below the background only the face-picker preview
     *  (if allowed) and the Galaxy time-record x/y bytes may differ from the template. Null = OK. */
    public static String verifyAllowed(Kind kind, byte[] template, byte[] raw, boolean thumbAllowed) {
        if (raw.length != template.length) return "size changed";
        Set<Integer> allowed = new HashSet<>();
        if (kind == Kind.GALAXY) for (El e : GALAXY) if (e.isTime()) for (int k = 0; k < 4; k++) allowed.add(e.rec + k);
        for (int i = 0; i < BG_OFF; i++) {
            if (raw[i] == template[i] || allowed.contains(i)) continue;
            if (thumbAllowed && i >= THUMB_OFF && i < THUMB_OFF + THUMB_LEN) continue;
            return "header byte 0x" + Integer.toHexString(i) + " changed";
        }
        return null;
    }

    /** What the watch should show for this raw at a given time (live preview). */
    public static int[] preview(Kind kind, byte[] raw, int hour24, boolean h12, int minute, int month, int day,
                                int weekdayMon0, boolean chinese) {
        int[] out = new int[W * H];
        for (int i = 0; i < W * H; i++) out[i] = argb(be16(raw, BG_OFF + 2 * i));
        int hour = h12 ? ((hour24 + 11) % 12 + 1) : hour24;
        for (El s : elements(kind)) {
            int idx;
            switch (s.role) {
                case HOUR_T: idx = hour / 10; break;
                case HOUR_U: idx = hour % 10; break;
                case MIN_T: idx = minute / 10; break;
                case MIN_U: idx = minute % 10; break;
                case MONTH_T: idx = month / 10; break;
                case MONTH_U: idx = month % 10; break;
                case DAY_T: idx = day / 10; break;
                case DAY_U: idx = day % 10; break;
                case WEEKDAY: idx = weekdayMon0 + (chinese ? 7 : 0); break;
                case AMPM: idx = (hour24 < 12 ? 0 : 1) + (chinese ? 2 : 0); break;
                default: idx = 0;
            }
            int x = be16(raw, s.rec) + (s.second ? s.w : 0), y = be16(raw, s.rec + 2);
            int base = s.off + idx * s.w * s.h * 2;
            for (int yy = 0; yy < s.h; yy++)
                for (int xx = 0; xx < s.w; xx++) out[(y + yy) * W + (x + xx)] = argb(be16(raw, base + (yy * s.w + xx) * 2));
        }
        return out;
    }
}
