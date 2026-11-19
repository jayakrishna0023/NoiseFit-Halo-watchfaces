package com.jk.halostudio;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;

import com.jk.halostudio.core.Art;
import com.jk.halostudio.core.Composer;
import com.jk.halostudio.core.Presets;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Everything the user can customise, plus rendering of the 466x466 background. */
final class Design {
    static final int S = 466;

    static final class Sticker {
        String text = "⭐"; float x = 233, y = 233, size = 64, rot = 0;
        JSONObject json() throws Exception { JSONObject o = new JSONObject(); o.put("t", text); o.put("x", x); o.put("y", y); o.put("s", size); o.put("r", rot); return o; }
        static Sticker of(JSONObject o) { Sticker s = new Sticker(); s.text = o.optString("t", "⭐"); s.x = (float) o.optDouble("x", 233); s.y = (float) o.optDouble("y", 233); s.size = (float) o.optDouble("s", 64); s.rot = (float) o.optDouble("r", 0); return s; }
    }

    // layout & background
    Composer.Kind layout = Composer.Kind.GALAXY;
    int bgMode = 3;                       // 0 photo, 1 gradient, 2 solid, 3 art
    int artId = 0; long artSeed = 3; int[] artPal = {0xFF0B1026, 0xFF5B2A86, 0xFFD946EF, 0xFF22D3EE};
    String name = "My face";
    float zoom = 1f, panX = 0f, panY = 0f; int rotate = 0;
    int grad1 = 0xFF7C3AED, grad2 = 0xFF06B6D4, gradAngle = 45;
    int solid = 0xFF111827;
    // effects
    int brightness = 0, contrast = 100, saturation = 100, blur = 0, vignette = 0;
    boolean mono = false;
    int hue = 0;                          // hue shift in degrees
    int frame = 0, frameColor = 0xFF22D3EE; // 0 none, 1 thin ring, 2 neon ring, 3 minute ticks, 4 double ring
    int overlay = 0;                      // 0 none, 1 film grain, 2 scanlines, 3 glass sheen, 4 dot grid
    // text
    String text = "";
    int textSize = 34, textX = 233, textY = 400, textColor = 0xFFFFFFFF;
    boolean textBold = true;
    // stickers
    List<Sticker> stickers = new ArrayList<>();
    // clock
    int timeColor = 0xFFFFFFFF, timeColor2 = 0xFF22D3EE, dateColor = 0xFFFFFFFF, tileColor = 0xFF101010;
    boolean timeGradient = false;
    int dx = 0, dy = 0;
    // font
    boolean customFont = false;
    String fontFamily = "sans-serif-black", fontFile = "", separator = "/";
    boolean fontBold = false, fontItalic = false, upperLabels = false;
    int timeScale = 92, dateScale = 90, outline = 0, outlineColor = 0xFF000000, glow = 0, glowColor = 0xFF22D3EE;
    // face-list preview
    boolean updateThumb = false;          // experimental, off unless the user turns it on

    Design copy() {
        try { return fromJson(toJson()); } catch (Exception e) { throw new RuntimeException(e); }
    }

    JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("name", name); o.put("artId", artId); o.put("artSeed", artSeed);
        JSONArray ap = new JSONArray(); for (int c : artPal) ap.put(c); o.put("artPal", ap);
        o.put("layout", layout.name()); o.put("bgMode", bgMode); o.put("zoom", zoom); o.put("panX", panX); o.put("panY", panY); o.put("rotate", rotate);
        o.put("grad1", grad1); o.put("grad2", grad2); o.put("gradAngle", gradAngle); o.put("solid", solid);
        o.put("brightness", brightness); o.put("contrast", contrast); o.put("saturation", saturation);
        o.put("blur", blur); o.put("vignette", vignette); o.put("mono", mono); o.put("hue", hue); o.put("frame", frame); o.put("frameColor", frameColor); o.put("overlay", overlay);
        o.put("text", text); o.put("textSize", textSize); o.put("textX", textX); o.put("textY", textY); o.put("textColor", textColor); o.put("textBold", textBold);
        JSONArray st = new JSONArray(); for (Sticker s : stickers) st.put(s.json()); o.put("stickers", st);
        o.put("timeColor", timeColor); o.put("timeColor2", timeColor2); o.put("timeGradient", timeGradient);
        o.put("dateColor", dateColor); o.put("tileColor", tileColor); o.put("dx", dx); o.put("dy", dy);
        o.put("customFont", customFont); o.put("fontFamily", fontFamily); o.put("fontFile", fontFile); o.put("separator", separator);
        o.put("fontBold", fontBold); o.put("fontItalic", fontItalic); o.put("upperLabels", upperLabels);
        o.put("timeScale", timeScale); o.put("dateScale", dateScale); o.put("outline", outline); o.put("outlineColor", outlineColor);
        o.put("glow", glow); o.put("glowColor", glowColor); o.put("updateThumb", updateThumb);
        return o;
    }

    static Design fromJson(JSONObject o) {
        Design d = new Design();
        d.layout = Composer.Kind.valueOf(o.optString("layout", "GALAXY"));
        d.name = o.optString("name", "My face"); d.artId = o.optInt("artId", 0); d.artSeed = o.optLong("artSeed", 3);
        JSONArray ap = o.optJSONArray("artPal");
        if (ap != null && ap.length() >= 2) { d.artPal = new int[ap.length()]; for (int i = 0; i < ap.length(); i++) d.artPal[i] = ap.optInt(i); }
        d.bgMode = o.optInt("bgMode", 3); d.zoom = (float) o.optDouble("zoom", 1); d.panX = (float) o.optDouble("panX", 0); d.panY = (float) o.optDouble("panY", 0);
        d.rotate = o.optInt("rotate", 0);
        d.grad1 = o.optInt("grad1", d.grad1); d.grad2 = o.optInt("grad2", d.grad2); d.gradAngle = o.optInt("gradAngle", 45); d.solid = o.optInt("solid", d.solid);
        d.brightness = o.optInt("brightness", 0); d.contrast = o.optInt("contrast", 100); d.saturation = o.optInt("saturation", 100);
        d.blur = o.optInt("blur", 0); d.vignette = o.optInt("vignette", 0); d.mono = o.optBoolean("mono", false); d.hue = o.optInt("hue", 0); d.frame = o.optInt("frame", 0); d.frameColor = o.optInt("frameColor", 0xFF22D3EE); d.overlay = o.optInt("overlay", 0);
        d.text = o.optString("text", ""); d.textSize = o.optInt("textSize", 34); d.textX = o.optInt("textX", 233); d.textY = o.optInt("textY", 400);
        d.textColor = o.optInt("textColor", 0xFFFFFFFF); d.textBold = o.optBoolean("textBold", true);
        JSONArray st = o.optJSONArray("stickers");
        if (st != null) for (int i = 0; i < st.length(); i++) { JSONObject so = st.optJSONObject(i); if (so != null) d.stickers.add(Sticker.of(so)); }
        d.timeColor = o.optInt("timeColor", 0xFFFFFFFF); d.timeColor2 = o.optInt("timeColor2", 0xFF22D3EE); d.timeGradient = o.optBoolean("timeGradient", false);
        d.dateColor = o.optInt("dateColor", 0xFFFFFFFF); d.tileColor = o.optInt("tileColor", 0xFF101010);
        d.dx = o.optInt("dx", 0); d.dy = o.optInt("dy", 0);
        d.customFont = o.optBoolean("customFont", false); d.fontFamily = o.optString("fontFamily", "sans-serif-black");
        d.fontFile = o.optString("fontFile", ""); d.separator = o.optString("separator", "/");
        d.fontBold = o.optBoolean("fontBold", false); d.fontItalic = o.optBoolean("fontItalic", false); d.upperLabels = o.optBoolean("upperLabels", false);
        d.timeScale = o.optInt("timeScale", 92); d.dateScale = o.optInt("dateScale", 90);
        d.outline = o.optInt("outline", 0); d.outlineColor = o.optInt("outlineColor", 0xFF000000);
        d.glow = o.optInt("glow", 0); d.glowColor = o.optInt("glowColor", 0xFF22D3EE); d.updateThumb = o.optBoolean("updateThumb", false);
        if (d.layout == Composer.Kind.MOON) { d.dx = 0; d.dy = 0; }
        return d;
    }

    /** A preinstalled face as an editable design. */
    static Design fromPreset(Presets.Spec s) {
        Design d = new Design();
        d.name = s.name; d.layout = s.layout; d.bgMode = 3; d.artId = s.art; d.artPal = s.pal.clone(); d.artSeed = s.seed;
        d.customFont = true; d.fontFamily = s.font; d.fontFile = ""; d.fontBold = s.bold; d.fontItalic = s.italic; d.upperLabels = s.upper;
        d.timeColor = s.time; d.timeColor2 = s.time2; d.timeGradient = s.grad; d.dateColor = s.date; d.tileColor = s.tile;
        d.glow = s.glow; d.glowColor = s.glowColor; d.outline = s.outline; d.outlineColor = 0xFF000000; d.dx = s.dx; d.dy = s.dy; d.frame = s.frame; d.frameColor = s.frameColor; d.overlay = s.overlay;
        return d;
    }

    // small cache: generating art takes ~100-300 ms
    private static String artKey; private static int[] artPx;
    static int[] art(int id, int[] pal, long seed) {
        String k = id + ":" + java.util.Arrays.toString(pal) + ":" + seed;
        synchronized (Design.class) { if (k.equals(artKey)) return artPx; }
        int[] px = Art.render(id, pal, seed);           // outside the lock: gallery renders in parallel
        synchronized (Design.class) { artKey = k; artPx = px; }
        return px;
    }

    /** Photo rotated by the chosen quarter turns. */
    Bitmap rotated(Bitmap photo) {
        if (photo == null || rotate % 360 == 0) return photo;
        Matrix m = new Matrix();
        m.postRotate(rotate);
        return Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), m, true);
    }

    /** Crop rectangle in photo pixels for the current zoom/pan. */
    Rect cropRect(Bitmap photo) {
        int w = photo.getWidth(), h = photo.getHeight();
        float m = Math.min(w, h) / Math.max(1f, zoom);
        float cx = w / 2f + panX * (w - m) / 2f, cy = h / 2f + panY * (h - m) / 2f;
        return new Rect(Math.round(cx - m / 2), Math.round(cy - m / 2), Math.round(cx + m / 2), Math.round(cy + m / 2));
    }

    /** Renders the 466x466 background (photo/gradient/solid + effects + text + stickers). */
    int[] renderBackground(Bitmap photoRotated) {
        Bitmap base = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(base);
        if (bgMode == 3) {
            base.setPixels(art(artId, artPal, artSeed), 0, S, 0, 0, S, S);
        } else if (bgMode == 0 && photoRotated != null) {
            c.drawBitmap(photoRotated, cropRect(photoRotated), new Rect(0, 0, S, S), new Paint(Paint.FILTER_BITMAP_FLAG));
        } else if (bgMode == 0 || bgMode == 1) {
            double a = Math.toRadians(gradAngle);
            float gx = (float) Math.cos(a) * S / 2, gy = (float) Math.sin(a) * S / 2;
            Paint p = new Paint();
            p.setShader(new LinearGradient(S / 2f - gx, S / 2f - gy, S / 2f + gx, S / 2f + gy, grad1, grad2, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, S, S, p);
        } else {
            c.drawColor(solid);
        }
        Bitmap out = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888);
        Canvas oc = new Canvas(out);
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(mono ? 0f : saturation / 100f);
        float k = contrast / 100f, t = 128f * (1 - k) + brightness * 2.55f;
        cm.postConcat(new ColorMatrix(new float[]{k, 0, 0, 0, t, 0, k, 0, 0, t, 0, 0, k, 0, t, 0, 0, 0, 1, 0}));
        if (hue % 360 != 0) cm.postConcat(hueMatrix(hue));
        Paint ep = new Paint(Paint.FILTER_BITMAP_FLAG);
        ep.setColorFilter(new ColorMatrixColorFilter(cm));
        oc.drawBitmap(base, 0, 0, ep);
        int[] px = new int[S * S];
        if (blur > 0) {
            out.getPixels(px, 0, S, 0, 0, S, S);
            boxBlur(px, blur);
            out.setPixels(px, 0, S, 0, 0, S, S);
        }
        if (vignette > 0) {
            Paint vp = new Paint(Paint.ANTI_ALIAS_FLAG);
            int edge = (int) (vignette * 2.4f) << 24;
            vp.setShader(new RadialGradient(S / 2f, S / 2f, S / 2f, new int[]{0, 0, edge}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            oc.drawRect(0, 0, S, S, vp);
        }
        drawOverlay(oc);
        drawFrame(oc);
        for (Sticker s : stickers) {
            Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
            sp.setTextSize(s.size);
            sp.setTextAlign(Paint.Align.CENTER);
            Rect b = new Rect();
            sp.getTextBounds(s.text, 0, s.text.length(), b);
            oc.save();
            oc.rotate(s.rot, s.x, s.y);
            oc.drawText(s.text, s.x, s.y - b.exactCenterY(), sp);
            oc.restore();
        }
        if (text != null && !text.trim().isEmpty()) {
            Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
            tp.setColor(textColor);
            tp.setTextSize(textSize);
            tp.setTextAlign(Paint.Align.CENTER);
            tp.setTypeface(Typeface.create(Typeface.SANS_SERIF, textBold ? Typeface.BOLD : Typeface.NORMAL));
            tp.setShadowLayer(6f, 0f, 2f, 0xAA000000);
            oc.drawText(text, textX, textY, tp);
        }
        out.getPixels(px, 0, S, 0, 0, S, S);
        for (int i = 0; i < px.length; i++) px[i] |= 0xFF000000;
        return px;
    }

    static ColorMatrix hueMatrix(float deg) {
        double a = Math.toRadians(deg); float c = (float) Math.cos(a), s = (float) Math.sin(a);
        float lr = 0.213f, lg = 0.715f, lb = 0.072f;
        return new ColorMatrix(new float[]{
                lr + c * (1 - lr) + s * (-lr), lg + c * (-lg) + s * (-lg), lb + c * (-lb) + s * (1 - lb), 0, 0,
                lr + c * (-lr) + s * 0.143f, lg + c * (1 - lg) + s * 0.140f, lb + c * (-lb) + s * (-0.283f), 0, 0,
                lr + c * (-lr) + s * (-(1 - lr)), lg + c * (-lg) + s * lg, lb + c * (1 - lb) + s * lb, 0, 0,
                0, 0, 0, 1, 0});
    }

    void drawOverlay(Canvas oc) {
        if (overlay == 0) return;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (overlay == 1) {
            java.util.Random r = new java.util.Random(11);
            p.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 9000; i++) { p.setColor(r.nextBoolean() ? 0x22000000 : 0x18FFFFFF); float x = r.nextInt(S), y = r.nextInt(S); oc.drawRect(x, y, x + 1.2f, y + 1.2f, p); }
        } else if (overlay == 2) {
            p.setColor(0x30000000);
            for (int y = 0; y < S; y += 4) oc.drawRect(0, y, S, y + 1.6f, p);
        } else if (overlay == 3) {
            p.setShader(new LinearGradient(0, 0, S * 0.7f, S * 0.7f, new int[]{0x40FFFFFF, 0x10FFFFFF, 0x00FFFFFF}, new float[]{0, 0.45f, 0.46f}, Shader.TileMode.CLAMP));
            oc.drawRect(0, 0, S, S, p);
        } else {
            p.setColor(0x26FFFFFF);
            for (int y = 8; y < S; y += 16) for (int x = 8; x < S; x += 16) oc.drawCircle(x, y, 1.3f, p);
        }
    }

    void drawFrame(Canvas oc) {
        if (frame == 0) return;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(frameColor);
        float c = S / 2f;
        switch (frame) {
            case 1: p.setStrokeWidth(3); oc.drawCircle(c, c, 224, p); break;
            case 2:
                p.setStrokeWidth(5); p.setShadowLayer(14, 0, 0, frameColor);
                p.setShader(new android.graphics.SweepGradient(c, c, new int[]{frameColor, 0x22FFFFFF & frameColor | 0x22000000, frameColor}, null));
                oc.drawCircle(c, c, 220, p); break;
            case 3:
                p.setStrokeCap(Paint.Cap.ROUND);
                for (int i = 0; i < 60; i++) {
                    boolean hr = i % 5 == 0; p.setStrokeWidth(hr ? 5 : 2);
                    double a = Math.toRadians(i * 6);
                    float r1 = hr ? 200 : 212, r2 = 226;
                    oc.drawLine(c + (float) Math.cos(a) * r1, c + (float) Math.sin(a) * r1, c + (float) Math.cos(a) * r2, c + (float) Math.sin(a) * r2, p);
                }
                break;
            default:
                p.setStrokeWidth(2); oc.drawCircle(c, c, 226, p); p.setStrokeWidth(6); oc.drawCircle(c, c, 214, p);
        }
    }

    static void boxBlur(int[] px, int r) {
        int[] tmp = new int[px.length];
        for (int pass = 0; pass < 3; pass++) { blurH(px, tmp, r); blurV(tmp, px, r); }
    }

    private static void blurH(int[] in, int[] out, int r) {
        for (int y = 0; y < S; y++) {
            int sr = 0, sg = 0, sb = 0, row = y * S, n = 2 * r + 1;
            for (int i = -r; i <= r; i++) { int c = in[row + Math.min(S - 1, Math.max(0, i))]; sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255; }
            for (int x = 0; x < S; x++) {
                out[row + x] = 0xFF000000 | ((sr / n) << 16) | ((sg / n) << 8) | (sb / n);
                int a = in[row + Math.min(S - 1, x + r + 1)], b = in[row + Math.max(0, x - r)];
                sr += ((a >> 16) & 255) - ((b >> 16) & 255); sg += ((a >> 8) & 255) - ((b >> 8) & 255); sb += (a & 255) - (b & 255);
            }
        }
    }

    private static void blurV(int[] in, int[] out, int r) {
        for (int x = 0; x < S; x++) {
            int sr = 0, sg = 0, sb = 0, n = 2 * r + 1;
            for (int i = -r; i <= r; i++) { int c = in[Math.min(S - 1, Math.max(0, i)) * S + x]; sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255; }
            for (int y = 0; y < S; y++) {
                out[y * S + x] = 0xFF000000 | ((sr / n) << 16) | ((sg / n) << 8) | (sb / n);
                int a = in[Math.min(S - 1, y + r + 1) * S + x], b = in[Math.max(0, y - r) * S + x];
                sr += ((a >> 16) & 255) - ((b >> 16) & 255); sg += ((a >> 8) & 255) - ((b >> 8) & 255); sb += (a & 255) - (b & 255);
            }
        }
    }
}
