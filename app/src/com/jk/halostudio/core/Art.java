package com.jk.halostudio.core;

import java.util.Random;

/**
 * Procedural art engine — original backgrounds generated in code (no external images), pure Java so the
 * same pixels render on the phone and in PC tests. Every generator draws a 466x466 ARGB image.
 */
public final class Art {
    public static final int S = 466;
    public static final String[] NAMES = {"Nebula", "Aurora", "Dunes", "Synthwave", "Planet", "Bokeh", "Mesh",
            "Topography", "Low poly", "Waves", "Rings", "Sunburst", "Marble", "Starfield", "Halftone", "Minimal",
            "Black hole", "Liquid", "Flow", "Mountains", "Crystal", "Tunnel", "Glass", "Galaxy", "Circuit", "Kaleido",
            "Code rain", "Fireflies", "Sunset sea", "Bauhaus"};

    private Art() {}

    // ------------------------------------------------------------------ helpers
    static int clamp8(float v) { return v < 0 ? 0 : v > 255 ? 255 : Math.round(v); }
    static int rgb(float r, float g, float b) { return 0xFF000000 | (clamp8(r) << 16) | (clamp8(g) << 8) | clamp8(b); }
    static int R(int c) { return (c >> 16) & 255; }
    static int G(int c) { return (c >> 8) & 255; }
    static int B(int c) { return c & 255; }

    public static int mix(int a, int b, float t) {
        t = t < 0 ? 0 : t > 1 ? 1 : t;
        return rgb(R(a) + (R(b) - R(a)) * t, G(a) + (G(b) - G(a)) * t, B(a) + (B(b) - B(a)) * t);
    }

    static int add(int a, int b, float k) { return rgb(R(a) + R(b) * k, G(a) + G(b) * k, B(a) + B(b) * k); }
    static int scale(int c, float k) { return rgb(R(c) * k, G(c) * k, B(c) * k); }

    /** Colour from a palette treated as an evenly spaced gradient, t in 0..1. */
    static int ramp(int[] pal, float t) {
        t = t < 0 ? 0 : t > 1 ? 1 : t;
        float x = t * (pal.length - 1);
        int i = Math.min(pal.length - 2, (int) x);
        return mix(pal[i], pal[i + 1], x - i);
    }

    static float smooth(float e0, float e1, float x) { float t = Math.max(0, Math.min(1, (x - e0) / (e1 - e0))); return t * t * (3 - 2 * t); }

    // value noise + fbm
    static final class Noise {
        final int[] perm = new int[512];
        Noise(long seed) {
            int[] p = new int[256];
            for (int i = 0; i < 256; i++) p[i] = i;
            Random r = new Random(seed);
            for (int i = 255; i > 0; i--) { int j = r.nextInt(i + 1); int t = p[i]; p[i] = p[j]; p[j] = t; }
            for (int i = 0; i < 512; i++) perm[i] = p[i & 255];
        }
        float hash(int x, int y) { return perm[(perm[x & 255] + y) & 511] / 255f; }
        float value(float x, float y) {
            int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
            float xf = x - xi, yf = y - yi;
            float u = xf * xf * (3 - 2 * xf), v = yf * yf * (3 - 2 * yf);
            float a = hash(xi, yi), b = hash(xi + 1, yi), c = hash(xi, yi + 1), d = hash(xi + 1, yi + 1);
            return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
        }
        float fbm(float x, float y, int oct) {
            float s = 0, amp = 0.5f, f = 1, norm = 0;
            for (int i = 0; i < oct; i++) { s += amp * value(x * f, y * f); norm += amp; amp *= 0.5f; f *= 2.03f; }
            return s / norm;
        }
    }

    static void fill(int[] px, int c) { java.util.Arrays.fill(px, c); }

    static void vertical(int[] px, int top, int bottom) {
        for (int y = 0; y < S; y++) { int c = mix(top, bottom, y / (float) (S - 1)); for (int x = 0; x < S; x++) px[y * S + x] = c; }
    }

    /** Anti-aliased soft disc blended over the image (alpha 0..1, soft = edge width in px). */
    static void disc(int[] px, float cx, float cy, float r, int col, float alpha, float soft) {
        int x0 = Math.max(0, (int) (cx - r - soft - 1)), x1 = Math.min(S - 1, (int) (cx + r + soft + 1));
        int y0 = Math.max(0, (int) (cy - r - soft - 1)), y1 = Math.min(S - 1, (int) (cy + r + soft + 1));
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) {
                float d = (float) Math.hypot(x + 0.5f - cx, y + 0.5f - cy);
                float a = (1 - smooth(r - soft, r + Math.max(0.75f, soft * 0.01f), d)) * alpha;
                if (a > 0) px[y * S + x] = mix(px[y * S + x], col, a);
            }
    }

    static void stars(int[] px, Random r, int n, float maxR, int tint) {
        for (int i = 0; i < n; i++) {
            float x = r.nextFloat() * S, y = r.nextFloat() * S, s = (float) Math.pow(r.nextFloat(), 3) * maxR + 0.4f;
            int c = mix(0xFFFFFFFF, tint, r.nextFloat() * 0.5f);
            disc(px, x, y, s, c, 0.6f + r.nextFloat() * 0.4f, s * 0.8f);
            if (s > maxR * 0.6f) disc(px, x, y, s * 4, c, 0.12f, s * 4);
        }
    }

    static void grain(int[] px, Random r, float amt) {
        for (int i = 0; i < px.length; i++) { float g = (r.nextFloat() - 0.5f) * amt; int c = px[i]; px[i] = rgb(R(c) + g, G(c) + g, B(c) + g); }
    }

    // ------------------------------------------------------------------ generators
    /**
     * @param id   index into {@link #NAMES}
     * @param pal  3–5 colours (dark → light works best)
     * @param seed variation
     */
    public static int[] render(int id, int[] pal, long seed) {
        if (pal.length < 3) pal = new int[]{pal[0], mix(pal[0], pal[pal.length - 1], 0.5f), pal[pal.length - 1]};
        int[] px = new int[S * S];
        Random r = new Random(seed * 7919 + id);
        Noise n = new Noise(seed + id * 31L);
        switch (id) {
            case 0: { // Nebula
                fill(px, scale(pal[0], 0.35f));
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float f = n.fbm(x / 140f, y / 140f, 6), g = n.fbm(x / 70f + 9, y / 70f + 3, 5);
                    float t = smooth(0.35f, 0.85f, f);
                    int c = mix(px[y * S + x], ramp(pal, g), t * 0.9f);
                    px[y * S + x] = add(c, pal[pal.length - 1], smooth(0.62f, 0.95f, f) * 0.35f);
                }
                stars(px, r, 420, 1.6f, pal[pal.length - 1]);
                break;
            }
            case 1: { // Aurora
                vertical(px, scale(pal[0], 0.4f), pal[0]);
                stars(px, r, 260, 1.2f, 0xFFFFFFFF);
                for (int band = 0; band < 3; band++) {
                    float ph = r.nextFloat() * 6.28f, amp = 30 + r.nextFloat() * 40, base = 120 + band * 70, fq = 0.008f + r.nextFloat() * 0.01f;
                    int col = pal[1 + band % (pal.length - 1)];
                    for (int x = 0; x < S; x++) {
                        float cy = base + (float) Math.sin(x * fq + ph) * amp + n.fbm(x / 60f, band * 10, 3) * 40;
                        for (int y = 0; y < S; y++) {
                            float dy = y - cy;
                            float k = dy < 0 ? (float) Math.exp(-dy * dy / 900) : (float) Math.exp(-dy * dy / 120);
                            float streak = 0.6f + 0.4f * n.value(x / 6f, band * 50);
                            if (k > 0.01f) px[y * S + x] = add(px[y * S + x], col, k * 0.55f * streak);
                        }
                    }
                }
                break;
            }
            case 2: { // Dunes
                vertical(px, pal[pal.length - 1], pal[pal.length - 2]);
                disc(px, 112, 228, 40, mix(pal[pal.length - 1], 0xFFFFFFFF, 0.6f), 0.9f, 3);
                for (int layer = 0; layer < 5; layer++) {
                    float base = 230 + layer * 48, ph = r.nextFloat() * 6.28f, amp = 26 - layer * 3;
                    int col = mix(pal[Math.min(pal.length - 1, 1)], pal[0], layer / 4f);
                    for (int x = 0; x < S; x++) {
                        float h = base + (float) (Math.sin(x * 0.012f + ph) * amp + Math.sin(x * 0.031f + ph * 2) * amp * 0.4f);
                        for (int y = Math.max(0, (int) h); y < S; y++) {
                            float edge = Math.min(1, y - h);
                            px[y * S + x] = mix(px[y * S + x], scale(col, 1 - (y - h) / 900f), edge);
                        }
                    }
                }
                break;
            }
            case 3: { // Synthwave
                vertical(px, pal[0], pal[1]);
                stars(px, r, 120, 1.0f, 0xFFFFFFFF);
                float cx = 233, cy = 220, rad = 105;
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float d = (float) Math.hypot(x - cx, y - cy);
                    if (d < rad && y < 290) {
                        float t = (y - (cy - rad)) / (2 * rad);
                        boolean cut = y > cy && ((int) ((y - cy) / 9f)) % 2 == 1 && (y - cy) % 9 < 2 + (y - cy) / 20f;
                        if (!cut) px[y * S + x] = mix(px[y * S + x], ramp(new int[]{pal[pal.length - 1], pal[2]}, t), smooth(rad, rad - 1.5f, d));
                    }
                }
                for (int y = 290; y < S; y++) {
                    float depth = (y - 280) / 186f;
                    for (int x = 0; x < S; x++) {
                        int c = mix(scale(pal[0], 0.6f), pal[0], depth);
                        float gx = ((x - 233) / (depth * 2.2f + 0.05f)) / 38f;
                        float lx = Math.abs(gx - Math.round(gx)) * 38f * (depth * 2.2f + 0.05f);
                        float zy = 1f / (depth + 0.02f);
                        float ly = Math.abs(zy * 3 - Math.round(zy * 3));
                        float line = Math.max(smooth(1.6f, 0f, lx), smooth(0.08f * depth + 0.02f, 0f, ly) * Math.min(1, depth * 3));
                        px[y * S + x] = mix(c, pal[2], line * 0.9f);
                    }
                }
                break;
            }
            case 4: { // Planet
                fill(px, scale(pal[0], 0.25f));
                stars(px, r, 300, 1.3f, 0xFFFFFFFF);
                float cx = 300, cy = 300, rad = 150, lx = -0.55f, ly = -0.6f, lz = 0.58f;
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float dx = (x - cx) / rad, dy = (y - cy) / rad, d2 = dx * dx + dy * dy;
                    if (d2 > 1.02f) continue;
                    float z = (float) Math.sqrt(Math.max(0, 1 - d2));
                    float light = Math.max(0.04f, dx * lx + dy * ly + z * lz);
                    float bands = n.fbm(dy * 3f + 7, dx * 0.6f, 4);
                    int c = scale(ramp(pal, 0.25f + bands * 0.75f), 0.25f + light);
                    float edge = smooth(1.02f, 0.98f, d2);
                    px[y * S + x] = mix(px[y * S + x], c, edge);
                    float rim = smooth(0.85f, 1.0f, d2) * edge;
                    px[y * S + x] = add(px[y * S + x], pal[pal.length - 1], rim * 0.25f);
                }
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {           // ring
                    float ex = (x - cx) / 255f, ey = (y - cy) / 70f;
                    float e = ex * ex + ey * ey;
                    boolean behind = y < cy && ((x - cx) * (x - cx) + (y - cy) * (y - cy)) < rad * rad;
                    if (!behind && e > 0.55f && e < 1.0f) px[y * S + x] = mix(px[y * S + x], pal[pal.length - 1], 0.45f * smooth(0.55f, 0.65f, e) * smooth(1.0f, 0.9f, e));
                }
                break;
            }
            case 5: { // Bokeh
                vertical(px, pal[0], pal[1]);
                for (int i = 0; i < 70; i++) {
                    float rad = 8 + r.nextFloat() * 46;
                    disc(px, r.nextFloat() * S, r.nextFloat() * S, rad, pal[1 + r.nextInt(pal.length - 1)], 0.18f + r.nextFloat() * 0.3f, rad * 0.25f);
                }
                break;
            }
            case 6: { // Mesh gradient
                fill(px, pal[0]);
                float[][] pts = new float[pal.length][2];
                for (int i = 0; i < pal.length; i++) { pts[i][0] = r.nextFloat() * S; pts[i][1] = r.nextFloat() * S; }
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float wr = 0, wg = 0, wb = 0, ws = 0;
                    for (int i = 0; i < pal.length; i++) {
                        float d = (float) Math.hypot(x - pts[i][0], y - pts[i][1]);
                        float w = 1f / (float) Math.pow(d + 40, 2.2);
                        wr += R(pal[i]) * w; wg += G(pal[i]) * w; wb += B(pal[i]) * w; ws += w;
                    }
                    px[y * S + x] = rgb(wr / ws, wg / ws, wb / ws);
                }
                grain(px, r, 10);
                break;
            }
            case 7: { // Topography
                fill(px, pal[0]);
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float f = n.fbm(x / 160f, y / 160f, 5) * 14;
                    float line = Math.abs(f - Math.round(f));
                    int base = ramp(new int[]{pal[0], pal[1]}, f / 14f);
                    px[y * S + x] = mix(base, pal[pal.length - 1], smooth(0.09f, 0.02f, line) * 0.85f);
                }
                break;
            }
            case 8: { // Low poly
                int cell = 58, nx = S / cell + 3, ny = S / cell + 3;
                float[][][] v = new float[nx][ny][2];
                for (int i = 0; i < nx; i++) for (int j = 0; j < ny; j++) { v[i][j][0] = (i - 1) * cell + (r.nextFloat() - 0.5f) * cell * 0.8f; v[i][j][1] = (j - 1) * cell + (r.nextFloat() - 0.5f) * cell * 0.8f; }
                for (int i = 0; i < nx - 1; i++) for (int j = 0; j < ny - 1; j++) {
                    float[] a = v[i][j], b = v[i + 1][j], c = v[i][j + 1], d = v[i + 1][j + 1];
                    tri(px, a, b, c, ramp(pal, ((a[0] + a[1]) / (2f * S)) * 0.9f + r.nextFloat() * 0.12f));
                    tri(px, b, d, c, ramp(pal, ((d[0] + d[1]) / (2f * S)) * 0.9f + r.nextFloat() * 0.12f));
                }
                break;
            }
            case 9: { // Waves
                vertical(px, pal[pal.length - 1], pal[0]);
                for (int k = 0; k < 9; k++) {
                    float base = 140 + k * 38, ph = r.nextFloat() * 6.28f, amp = 14 + r.nextFloat() * 10;
                    int col = ramp(pal, 1 - k / 9f);
                    for (int x = 0; x < S; x++) {
                        float h = base + (float) Math.sin(x * 0.02f + ph) * amp;
                        for (int y = Math.max(0, (int) h); y < S; y++) px[y * S + x] = mix(px[y * S + x], col, Math.min(1, y - h));
                    }
                }
                break;
            }
            case 10: { // Rings (dial)
                fill(px, pal[0]);
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float d = (float) Math.hypot(x - 233, y - 233);
                    float t = (d / 233f) * 7;
                    int band = (int) t;
                    int c = ramp(pal, (band % pal.length) / (float) (pal.length - 1));
                    float edge = smooth(0f, 0.06f, t - band) * smooth(1f, 0.94f, t - band);
                    px[y * S + x] = mix(scale(c, 0.55f), c, edge);
                    float a = (float) Math.atan2(y - 233, x - 233);
                    float tick = Math.abs(((a / 6.28318f) * 60 + 60) % 1 - 0.5f);
                    if (d > 205 && d < 225 && tick > 0.46f) px[y * S + x] = mix(px[y * S + x], pal[pal.length - 1], 0.9f);
                }
                break;
            }
            case 11: { // Sunburst
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float a = (float) Math.atan2(y - 233, x - 233), d = (float) Math.hypot(x - 233, y - 233);
                    float s = (float) Math.sin(a * 16) * 0.5f + 0.5f;
                    int c = mix(pal[0], pal[1], smooth(0.45f, 0.55f, s));
                    px[y * S + x] = mix(c, pal[pal.length - 1], smooth(180, 0, d) * 0.5f);
                }
                break;
            }
            case 12: { // Marble
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float w = n.fbm(x / 120f, y / 120f, 6) * 9;
                    float v = (float) Math.sin((x + y) / 60f + w) * 0.5f + 0.5f;
                    px[y * S + x] = ramp(pal, (float) Math.pow(v, 1.6));
                }
                grain(px, r, 6);
                break;
            }
            case 13: { // Starfield
                vertical(px, scale(pal[0], 0.2f), scale(pal[0], 0.55f));
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {               // milky band
                    float d = Math.abs((x - y) / 1.414f + 30), f = n.fbm(x / 50f, y / 50f, 5);
                    float k = (float) Math.exp(-d * d / 4000) * f;
                    px[y * S + x] = add(px[y * S + x], pal[1], k * 0.7f);
                }
                stars(px, r, 900, 1.4f, pal[pal.length - 1]);
                break;
            }
            case 14: { // Halftone
                fill(px, pal[0]);
                int step = 14;
                for (int gy = 0; gy < S + step; gy += step) for (int gx = 0; gx < S + step; gx += step) {
                    float t = (gx + gy) / (2f * S);
                    disc(px, gx + ((gy / step) % 2) * step / 2f, gy, 1 + t * step * 0.48f, ramp(pal, 0.4f + t * 0.6f), 1f, 0.8f);
                }
                break;
            }
            case 16: { // Black hole
                fill(px, scale(pal[0], 0.15f));
                stars(px, r, 500, 1.3f, pal[pal.length - 1]);
                float cx = 233, cy = 233;
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float dx = x - cx, dy = y - cy, d = (float) Math.hypot(dx, dy);
                    int c = px[y * S + x];
                    c = add(c, pal[pal.length - 1], (float) Math.exp(-(d - 74) * (d - 74) / 50f) * 0.9f);
                    c = add(c, pal[2], (float) Math.exp(-(d - 90) * (d - 90) / 900f) * 0.35f);
                    if (d < 64) c = mix(c, 0xFF000000, smooth(64, 58, d));
                    float e = (float) Math.hypot(dx, dy / 0.26f);
                    if (e > 70 && e < 232 && (dy > 0 || d > 62)) {
                        float a = (float) Math.atan2(dy / 0.26f, dx);
                        float k = smooth(70, 95, e) * smooth(232, 140, e) * (0.45f + 0.55f * n.fbm(a * 2.5f + 5, e / 14f, 4));
                        float doppler = 0.75f + 0.35f * (float) Math.cos(a);
                        c = add(c, ramp(pal, 1 - (e - 70) / 170f), k * doppler);
                    }
                    px[y * S + x] = c;
                }
                break;
            }
            case 17: { // Liquid (domain-warped iridescence)
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float u = x / 150f, v = y / 150f;
                    float qx = n.fbm(u, v, 4), qy = n.fbm(u + 5.2f, v + 1.3f, 4);
                    float rx = n.fbm(u + 4 * qx + 1.7f, v + 4 * qy + 9.2f, 4), ry = n.fbm(u + 4 * qx + 8.3f, v + 4 * qy + 2.8f, 4);
                    float f = n.fbm(u + 4 * rx, v + 4 * ry, 4);
                    int c = ramp(pal, smooth(0.2f, 0.8f, f));
                    c = add(c, 0xFFFFFFFF, smooth(0.62f, 0.8f, rx) * 0.35f);
                    px[y * S + x] = scale(c, 0.55f + 0.6f * qy);
                }
                grain(px, r, 5);
                break;
            }
            case 18: { // Flow field
                vertical(px, scale(pal[0], 0.5f), pal[0]);
                for (int i = 0; i < 1600; i++) {
                    float x = r.nextFloat() * S, y = r.nextFloat() * S;
                    int col = ramp(pal, 0.3f + 0.7f * r.nextFloat());
                    for (int st = 0; st < 70; st++) {
                        float ang = n.fbm(x / 130f, y / 130f, 3) * 12.566f;
                        x += (float) Math.cos(ang) * 1.6f; y += (float) Math.sin(ang) * 1.6f;
                        if (x < 0 || y < 0 || x >= S || y >= S) break;
                        disc(px, x, y, 0.7f, col, 0.18f, 0.6f);
                    }
                }
                break;
            }
            case 19: { // Mountains
                vertical(px, pal[0], pal[1]);
                stars(px, r, 220, 1.1f, 0xFFFFFFFF);
                disc(px, 405, 215, 20, pal[pal.length - 1], 0.95f, 2);
                disc(px, 405, 215, 60, pal[pal.length - 1], 0.12f, 80);
                for (int layer = 0; layer < 5; layer++) {
                    float base = 250 + layer * 45, amp = 150 - layer * 20;
                    int col = mix(mix(pal[1], pal[pal.length - 1], 0.25f), scale(pal[0], 0.4f), layer / 4f);
                    for (int x = 0; x < S; x++) {
                        float rr = n.fbm(x / (160f - layer * 15), layer * 7.3f, 5);
                        float ridge = 1 - Math.abs(2 * rr - 1); float h = base - ridge * amp * 1.3f + 40 + Math.abs(n.value(x / 9f, layer * 3)) * 6;
                        for (int y = Math.max(0, (int) h); y < S; y++) {
                            float fog = smooth(h, h + 60, y);
                            px[y * S + x] = mix(px[y * S + x], mix(col, scale(col, 0.7f), fog), Math.min(1, y - h));
                        }
                    }
                }
                break;
            }
            case 20: { // Crystal (Voronoi)
                int np = 46;
                float[][] pt = new float[np][2]; int[] pc = new int[np];
                for (int i = 0; i < np; i++) { pt[i][0] = r.nextFloat() * S; pt[i][1] = r.nextFloat() * S; pc[i] = ramp(pal, r.nextFloat() * 0.85f); }
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float d1 = 1e9f, d2 = 1e9f; int best = 0;
                    for (int i = 0; i < np; i++) {
                        float dx = x - pt[i][0], dy = y - pt[i][1], d = dx * dx + dy * dy;
                        if (d < d1) { d2 = d1; d1 = d; best = i; } else if (d < d2) d2 = d;
                    }
                    float a = (float) Math.sqrt(d1), b = (float) Math.sqrt(d2);
                    int c = scale(pc[best], 0.75f + 0.45f * (1 - Math.min(1, a / 70f)));
                    px[y * S + x] = mix(c, pal[pal.length - 1], smooth(2.2f, 0.4f, b - a) * 0.8f);
                }
                break;
            }
            case 21: { // Neon tunnel
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float dx = x - 233, dy = y - 233;
                    float rr = Math.max(Math.abs(dx), Math.abs(dy)) + 0.5f;
                    float z = 900f / (rr + 6);
                    float fr = z - (float) Math.floor(z);
                    float fade = smooth(8, 230, rr);
                    float line = smooth(0.16f, 0f, Math.min(fr, 1 - fr)) * fade;
                    float diag = smooth(1.4f, 0f, Math.abs(Math.abs(dx) - Math.abs(dy))) * fade;
                    int c = mix(scale(pal[0], 0.3f), pal[0], fade);
                    c = add(c, ramp(new int[]{pal[pal.length - 1], pal[2]}, (z % 6) / 6f), line * 0.9f);
                    px[y * S + x] = add(c, pal[pal.length - 1], diag * 0.5f);
                }
                break;
            }
            case 22: { // Glass orbs (glassmorphism)
                vertical(px, pal[0], scale(pal[0], 0.6f));
                for (int i = 0; i < 7; i++) {
                    float rad = 60 + r.nextFloat() * 90;
                    disc(px, r.nextFloat() * S, r.nextFloat() * S, rad, pal[1 + r.nextInt(pal.length - 1)], 0.85f, rad * 0.9f);
                }
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float d = (float) Math.hypot(x - 233, y - 233);
                    if (d < 186) {
                        int c = mix(px[y * S + x], 0xFFFFFFFF, 0.10f + 0.06f * (1 - y / (float) S));
                        c = mix(c, 0xFFFFFFFF, smooth(1.6f, 0, Math.abs(d - 185)) * 0.55f);
                        px[y * S + x] = c;
                    }
                }
                grain(px, r, 7);
                break;
            }
            case 23: { // Spiral galaxy
                fill(px, scale(pal[0], 0.2f));
                stars(px, r, 380, 1.2f, 0xFFFFFFFF);
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float dx = x - 233, dy = (y - 233) / 0.72f;
                    float rr = (float) Math.hypot(dx, dy), a = (float) Math.atan2(dy, dx);
                    float arm = (float) Math.sin(2 * (a - Math.log(rr + 1) * 2.4f)) * 0.5f + 0.5f;
                    float k = (float) Math.pow(arm, 3) * (float) Math.exp(-rr / 150f) * (0.5f + n.fbm(x / 30f, y / 30f, 4));
                    int c = add(px[y * S + x], ramp(pal, 1 - rr / 240f), k * 1.4f);
                    px[y * S + x] = add(c, pal[pal.length - 1], (float) Math.exp(-rr * rr / 1300f) * 0.9f);
                }
                break;
            }
            case 24: { // Circuit
                fill(px, pal[0]);
                for (int gy = 9; gy < S; gy += 18) for (int gx = 9; gx < S; gx += 18) disc(px, gx, gy, 0.8f, pal[1], 0.5f, 0.6f);
                int[][] dirs = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};
                for (int t = 0; t < 70; t++) {
                    float x = 9 + 18 * r.nextInt(26), y = 9 + 18 * r.nextInt(26);
                    int dir = r.nextInt(8), steps = 6 + r.nextInt(16);
                    int col = r.nextInt(4) == 0 ? pal[pal.length - 1] : pal[Math.min(2, pal.length - 1)];
                    disc(px, x, y, 3.2f, col, 0.9f, 0.8f);
                    for (int s2 = 0; s2 < steps; s2++) {
                        if (r.nextInt(5) == 0) dir = (dir + (r.nextBoolean() ? 1 : 7)) % 8;
                        float nx = x + dirs[dir][0] * 18, ny = y + dirs[dir][1] * 18;
                        line(px, x, y, nx, ny, 5f, col, 0.10f);
                        line(px, x, y, nx, ny, 1.3f, col, 0.85f);
                        x = nx; y = ny;
                    }
                    disc(px, x, y, 4f, col, 0.9f, 0.8f);
                    disc(px, x, y, 2f, pal[0], 1f, 0.6f);
                }
                break;
            }
            case 25: { // Kaleidoscope
                float seg = 6.28318f / 12;
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float dx = x - 233, dy = y - 233, rr = (float) Math.hypot(dx, dy);
                    float a = (float) Math.atan2(dy, dx) + 3.14159f;
                    a = a % seg; if (a > seg / 2) a = seg - a;
                    float u = rr * (float) Math.cos(a), v = rr * (float) Math.sin(a);
                    float f = n.fbm(u / 60f, v / 60f, 5);
                    float rings = (float) Math.sin(rr / 14f + f * 6) * 0.5f + 0.5f;
                    int c = ramp(pal, smooth(0.25f, 0.85f, f * 0.7f + rings * 0.3f));
                    px[y * S + x] = scale(c, 1.05f - 0.55f * smooth(150, 240, rr));
                }
                break;
            }
            case 26: { // Code rain
                fill(px, scale(pal[0], 0.3f));
                int cw = 11, ch = 13;
                for (int cx = 0; cx < S / cw + 1; cx++) {
                    int len = 6 + r.nextInt(22), head = r.nextInt(S / ch + len);
                    for (int cy = 0; cy < S / ch + 1; cy++) {
                        int dist = head - cy;
                        if (dist < 0 || dist > len) { if (r.nextInt(14) != 0) continue; dist = len; }
                        float b = 1 - dist / (float) (len + 1);
                        int col = dist == 0 ? mix(pal[pal.length - 1], 0xFFFFFFFF, 0.7f) : ramp(new int[]{pal[1], pal[pal.length - 1]}, b);
                        int bits = r.nextInt(1 << 15);
                        for (int by = 0; by < 5; by++) for (int bx = 0; bx < 3; bx++) if ((bits >> (by * 3 + bx) & 1) != 0) {
                            int x0 = cx * cw + 2 + bx * 2, y0 = cy * ch + 2 + by * 2;
                            for (int yy = y0; yy < y0 + 2 && yy < S; yy++) for (int xx = x0; xx < x0 + 2 && xx < S; xx++)
                                px[yy * S + xx] = mix(px[yy * S + xx], col, 0.25f + 0.75f * b);
                        }
                    }
                }
                break;
            }
            case 27: { // Fireflies forest
                vertical(px, pal[0], pal[1]);
                stars(px, r, 140, 1.0f, 0xFFFFFFFF);
                for (int layer = 0; layer < 3; layer++) {
                    int col = mix(scale(pal[0], 0.7f), 0xFF000000, 0.4f + layer * 0.3f);
                    float base = 300 + layer * 50;
                    for (int t = 0; t < 26 - layer * 4; t++) {
                        float tx = r.nextFloat() * S, th = 90 + r.nextFloat() * 120 - layer * 10, tw = 22 + r.nextFloat() * 20;
                        for (int k = 0; k < 4; k++) {
                            float top = base - th + k * th * 0.22f;
                            tri(px, new float[]{tx, top}, new float[]{tx - tw * (0.5f + k * 0.2f), top + th * 0.38f}, new float[]{tx + tw * (0.5f + k * 0.2f), top + th * 0.38f}, col);
                        }
                    }
                    for (int y = (int) base; y < S; y++) for (int x = 0; x < S; x++) px[y * S + x] = col;
                }
                for (int i = 0; i < 60; i++) {
                    float fx = r.nextFloat() * S, fy = 200 + r.nextFloat() * 260;
                    disc(px, fx, fy, 14, pal[pal.length - 1], 0.18f, 14);
                    disc(px, fx, fy, 1.6f, mix(pal[pal.length - 1], 0xFFFFFFFF, 0.5f), 1f, 1f);
                }
                break;
            }
            case 28: { // Sunset sea
                for (int y = 0; y < 280; y++) { int c = ramp(new int[]{pal[0], pal[1], pal[2]}, y / 280f); for (int x = 0; x < S; x++) px[y * S + x] = c; }
                disc(px, 233, 262, 70, pal[pal.length - 1], 1f, 2);
                disc(px, 233, 262, 160, pal[pal.length - 1], 0.25f, 150);
                for (int y = 280; y < S; y++) {
                    float depth = (y - 280) / 186f;
                    for (int x = 0; x < S; x++) {
                        int c = mix(scale(pal[1], 0.7f), scale(pal[0], 0.6f), depth);
                        float w = 50 + depth * 90;
                        float glint = (float) Math.exp(-(x - 233) * (x - 233) / (2 * w * w));
                        float wave = smooth(0.55f, 0.75f, n.value(x / (8 + depth * 20), y / (1.2f + depth * 3)));
                        px[y * S + x] = add(c, pal[pal.length - 1], glint * wave * (1 - depth * 0.5f));
                    }
                }
                for (int x = 0; x < S; x++) px[280 * S + x] = mix(px[280 * S + x], pal[pal.length - 1], 0.5f);
                break;
            }
            case 29: { // Bauhaus
                int tile = 117;
                fill(px, pal[0]);
                for (int ty = 0; ty < 4; ty++) for (int tx = 0; tx < 4; tx++) {
                    float x0 = tx * tile, y0 = ty * tile;
                    int bi = r.nextInt(pal.length), fi = (bi + 1 + r.nextInt(pal.length - 1)) % pal.length;
                    int bg = pal[bi], fg = pal[fi];
                    for (int y = (int) y0; y < Math.min(S, y0 + tile); y++) for (int x = (int) x0; x < Math.min(S, x0 + tile); x++) px[y * S + x] = bg;
                    switch (r.nextInt(5)) {
                        case 0: disc(px, x0 + tile / 2f, y0 + tile / 2f, tile * 0.38f, fg, 1, 1); break;
                        case 1: quarter(px, x0, y0, tile, r.nextInt(4), fg); break;
                        case 2: tri(px, new float[]{x0, y0 + tile}, new float[]{x0 + tile, y0 + tile}, new float[]{x0 + (r.nextBoolean() ? 0 : tile), y0}, fg); break;
                        case 3:
                            for (int k = 0; k < 4; k++) for (int y = (int) (y0 + k * tile / 4f); y < Math.min(S, y0 + k * tile / 4f + tile / 8f); y++)
                                for (int x = (int) x0; x < Math.min(S, x0 + tile); x++) px[y * S + x] = fg;
                            break;
                        default: disc(px, x0 + tile / 2f, y0 + tile, tile / 2f, fg, 1, 1);
                    }
                }
                grain(px, r, 6);
                break;
            }
            default: { // Minimal
                vertical(px, pal[0], scale(pal[0], 0.7f));
                grain(px, r, 8);
                for (int y = 0; y < S; y++) for (int x = 0; x < S; x++) {
                    float d = (float) Math.hypot(x - 233, y - 233);
                    float ring = smooth(2.2f, 0f, Math.abs(d - 214));
                    if (ring > 0) px[y * S + x] = mix(px[y * S + x], pal[pal.length - 1], ring * 0.8f);
                }
            }
        }
        return px;
    }

    /** Quarter disc filling a tile from one corner. */
    static void quarter(int[] px, float x0, float y0, int tile, int corner, int col) {
        float cx = x0 + ((corner & 1) == 0 ? 0 : tile), cy = y0 + ((corner & 2) == 0 ? 0 : tile);
        for (int y = (int) y0; y < Math.min(S, y0 + tile); y++) for (int x = (int) x0; x < Math.min(S, x0 + tile); x++) {
            float d = (float) Math.hypot(x + 0.5f - cx, y + 0.5f - cy);
            if (d < tile) px[y * S + x] = mix(px[y * S + x], col, smooth(tile + 0.5f, tile - 0.5f, d));
        }
    }

    /** Soft line made of overlapping discs. */
    static void line(int[] px, float x0, float y0, float x1, float y1, float w, int col, float alpha) {
        float len = (float) Math.hypot(x1 - x0, y1 - y0);
        int n = Math.max(1, (int) (len / Math.max(0.8f, w * 0.5f)));
        for (int i = 0; i <= n; i++) { float t = i / (float) n; disc(px, x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, w / 2, col, alpha, Math.max(0.6f, w * 0.3f)); }
    }

    static void tri(int[] px, float[] a, float[] b, float[] c, int col) {
        int x0 = (int) Math.max(0, Math.floor(Math.min(a[0], Math.min(b[0], c[0])))), x1 = (int) Math.min(S - 1, Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
        int y0 = (int) Math.max(0, Math.floor(Math.min(a[1], Math.min(b[1], c[1])))), y1 = (int) Math.min(S - 1, Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
        float area = (b[0] - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (b[1] - a[1]);
        if (Math.abs(area) < 1e-3) return;
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            float px0 = x + 0.5f, py0 = y + 0.5f;
            float w0 = ((b[0] - px0) * (c[1] - py0) - (c[0] - px0) * (b[1] - py0)) / area;
            float w1 = ((c[0] - px0) * (a[1] - py0) - (a[0] - px0) * (c[1] - py0)) / area;
            float w2 = 1 - w0 - w1;
            if (w0 >= -0.002f && w1 >= -0.002f && w2 >= -0.002f) px[y * S + x] = col;
        }
    }
}
