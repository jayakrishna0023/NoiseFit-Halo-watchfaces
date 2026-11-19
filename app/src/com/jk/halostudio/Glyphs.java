package com.jk.halostudio;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;

import com.jk.halostudio.core.Composer;
import com.jk.halostudio.core.Composer.Role;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** Draws digits/labels in any font for {@link Composer#compose} (ARGB with alpha = coverage). */
final class Glyphs implements Composer.GlyphPainter {
    static final String[] FAMILIES = {"sans-serif", "sans-serif-medium", "sans-serif-condensed", "sans-serif-light",
            "sans-serif-black", "serif", "monospace", "casual", "cursive", "sans-serif-smallcaps"};
    static final String[] FAMILY_NAMES = {"Sans", "Medium", "Condensed", "Light", "Black", "Serif", "Mono", "Casual", "Cursive", "Small caps"};

    private final Design d;
    private final Typeface tf;
    private final Map<String, Float> sizeCache = new HashMap<>();

    Glyphs(Design d, File fontsDir) {
        this.d = d;
        Typeface base = null;
        if (d.fontFile != null && !d.fontFile.isEmpty()) {
            File f = new File(fontsDir, d.fontFile);
            if (f.exists()) try { base = Typeface.createFromFile(f); } catch (Exception ignored) { }
        }
        if (base == null) base = Typeface.create(d.fontFamily, Typeface.NORMAL);
        int st = (d.fontBold ? Typeface.BOLD : 0) | (d.fontItalic ? Typeface.ITALIC : 0);
        tf = Typeface.create(base, st);
    }

    static boolean isTime(Role r) { return r == Role.HOUR_T || r == Role.HOUR_U || r == Role.MIN_T || r == Role.MIN_U; }

    static String text(Role role, int idx, Design d) {
        switch (role) {
            case WEEKDAY: {
                String s = Composer.WEEKDAY_TEXT[idx];
                return idx < 7 && d.upperLabels ? s.toUpperCase() : s;
            }
            case AMPM: return Composer.AMPM_TEXT[idx];
            case SEP: return d.separator;
            default: return String.valueOf(idx);
        }
    }

    /** Common text size per (role group, box) so every digit/label of a group has the same size. */
    private float fitSize(Paint p, Role role, int w, int h, float fill) {
        String group = (isTime(role) ? "T" : role == Role.WEEKDAY ? "W" : role == Role.AMPM ? "A" : "D") + w + "x" + h;
        Float c = sizeCache.get(group);
        if (c != null) return c;
        String[] samples;
        if (role == Role.WEEKDAY) { samples = new String[14]; for (int i = 0; i < 14; i++) samples[i] = text(role, i, d); }
        else if (role == Role.AMPM) samples = Composer.AMPM_TEXT;
        else samples = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", d.separator};
        float size = h * 1.4f;
        Rect b = new Rect();
        float pad = d.outline + d.glow * 0.6f;
        while (size > 6) {
            p.setTextSize(size);
            boolean fits = true;
            for (String s : samples) {
                p.getTextBounds(s, 0, s.length(), b);
                if (b.width() + 2 * pad > w * fill || b.height() + 2 * pad > h * fill) { fits = false; break; }
            }
            if (fits) break;
            size -= 1;
        }
        sizeCache.put(group, size);
        return size;
    }

    @Override
    public int[] paint(Role role, int idx, int w, int h) {
        boolean time = isTime(role);
        String s = text(role, idx, d);
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        p.setTypeface(tf);
        float fill = (time ? d.timeScale : d.dateScale) / 100f;
        p.setTextSize(fitSize(p, role, w, h, fill));
        p.setTextAlign(Paint.Align.CENTER);
        // vertical centre from the bounds of a reference glyph so all digits share one baseline
        Rect ref = new Rect();
        String refText = (role == Role.WEEKDAY || role == Role.AMPM) ? s : "8";
        p.getTextBounds(refText, 0, refText.length(), ref);
        float baseY = h / 2f - ref.exactCenterY();
        float cx = w / 2f;
        int col = time ? d.timeColor : d.dateColor;
        if (d.glow > 0) {
            Paint g = new Paint(p);
            g.setColor(d.glowColor);
            g.setMaskFilter(new BlurMaskFilter(d.glow, BlurMaskFilter.Blur.NORMAL));
            c.drawText(s, cx, baseY, g);
        }
        if (d.outline > 0) {
            Paint o = new Paint(p);
            o.setStyle(Paint.Style.STROKE);
            o.setStrokeJoin(Paint.Join.ROUND);
            o.setStrokeWidth(d.outline * 2);
            o.setColor(d.outlineColor);
            c.drawText(s, cx, baseY, o);
        }
        Paint f = new Paint(p);
        f.setColor(col);
        if (time && d.timeGradient) {
            f.setShader(new LinearGradient(0, baseY + ref.top, 0, baseY + ref.bottom, d.timeColor, d.timeColor2, Shader.TileMode.CLAMP));
        }
        c.drawText(s, cx, baseY, f);
        int[] px = new int[w * h];
        bm.getPixels(px, 0, w, 0, 0, w, h);
        // getPixels returns non-premultiplied ARGB: alpha = coverage, colour = glyph colour
        return px;
    }
}
