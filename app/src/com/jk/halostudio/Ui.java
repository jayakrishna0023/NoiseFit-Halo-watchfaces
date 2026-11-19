package com.jk.halostudio;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** Small dark-theme UI kit (no XML). */
final class Ui {
    static final int BG = 0xFF0B0C12, CARD = 0xFF161822, CARD2 = 0xFF1F2230, TEXT = 0xFFF3F4F8, MUTED = 0xFF9097AD;
    static final int ACCENT1 = 0xFF8B5CF6, ACCENT2 = 0xFF22D3EE, GOOD = 0xFF34D399, WARN = 0xFFFBBF24;

    static final int[] PALETTE = {0xFFFFFFFF, 0xFFFFC542, 0xFFFF7A59, 0xFFFF4D8D, 0xFFC084FC, 0xFF60A5FA,
            0xFF22D3EE, 0xFF34D399, 0xFFA3E635, 0xFFFDE68A, 0xFF94A3B8, 0xFF111111};
    static final int[][] GRADIENTS = {{0xFF7C3AED, 0xFF06B6D4}, {0xFFFF5F6D, 0xFFFFC371}, {0xFF0F2027, 0xFF2C5364},
            {0xFF11998E, 0xFF38EF7D}, {0xFFFC466B, 0xFF3F5EFB}, {0xFF232526, 0xFF414345}, {0xFFF7971E, 0xFFFFD200},
            {0xFF8E2DE2, 0xFF4A00E0}, {0xFF000000, 0xFF434343}, {0xFFEE9CA7, 0xFFFFDDE1}};

    final Context c;
    final float d;

    Ui(Context c) { this.c = c; this.d = c.getResources().getDisplayMetrics().density; }

    int dp(float v) { return Math.round(v * d); }

    GradientDrawable rounded(int color, float r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(r));
        return g;
    }

    GradientDrawable gradient(int a, int b, float r) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{a, b});
        g.setCornerRadius(dp(r));
        return g;
    }

    LinearLayout col() { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout row() { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }

    LinearLayout card() {
        LinearLayout l = col();
        l.setBackground(rounded(CARD, 20));
        l.setPadding(dp(16), dp(14), dp(16), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(12);
        l.setLayoutParams(lp);
        return l;
    }

    TextView text(String s, float size, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return t;
    }

    TextView title(String s) {
        TextView t = text(s, 15, TEXT, true);
        t.setPadding(0, 0, 0, dp(8));
        return t;
    }

    TextView button(String s, boolean primary, View.OnClickListener l) {
        TextView b = text(s, 15, primary ? Color.WHITE : TEXT, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(13), dp(16), dp(13));
        b.setBackground(primary ? gradient(ACCENT1, ACCENT2, 16) : rounded(CARD2, 16));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    TextView pill(String s, boolean selected, View.OnClickListener l) {
        TextView p = text(s, 14, selected ? Color.WHITE : MUTED, true);
        p.setPadding(dp(16), dp(9), dp(16), dp(9));
        p.setBackground(selected ? gradient(ACCENT1, ACCENT2, 30) : rounded(CARD2, 30));
        p.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = dp(8);
        p.setLayoutParams(lp);
        return p;
    }

    interface IntCb { void on(int v); }

    View slider(String label, int min, int max, int value, IntCb cb) {
        LinearLayout box = col();
        LinearLayout r = row();
        TextView l = text(label, 13, MUTED, false);
        TextView v = text(String.valueOf(value), 13, TEXT, true);
        r.addView(l, new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(v);
        SeekBar sb = new SeekBar(c);
        sb.setMin(min); sb.setMax(max); sb.setProgress(value);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) { v.setText(String.valueOf(p)); if (u) cb.on(p); }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        box.addView(r);
        box.addView(sb);
        box.setPadding(0, dp(4), 0, dp(4));
        return box;
    }

    View swatches(int[] colors, int selected, IntCb cb) {
        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout r = row();
        for (int col : colors) {
            View s = new View(c);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(col);
            g.setStroke(dp(col == selected ? 3 : 1), col == selected ? ACCENT2 : 0x33FFFFFF);
            s.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(34), dp(34));
            lp.rightMargin = dp(10);
            s.setOnClickListener(v -> cb.on(col));
            r.addView(s, lp);
        }
        hs.addView(r);
        hs.setPadding(0, dp(4), 0, dp(8));
        return hs;
    }

    /** Subtle press-down scale animation. */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    void pressable(View v) {
        v.setOnTouchListener((x, e) -> {
            if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) x.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start();
            else if (e.getActionMasked() == android.view.MotionEvent.ACTION_UP || e.getActionMasked() == android.view.MotionEvent.ACTION_CANCEL)
                x.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
            return false;
        });
    }

    static void fadeIn(View v, long delay) {
        v.setAlpha(0f); v.setTranslationY(18f);
        v.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(260).start();
    }

    /** Slow "breathing" pulse; runs only while the view is on screen. */
    static void breathe(View v) {
        android.animation.ObjectAnimator a = android.animation.ObjectAnimator.ofPropertyValuesHolder(v,
                android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.92f, 1.06f),
                android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.92f, 1.06f),
                android.animation.PropertyValuesHolder.ofFloat("alpha", 0.5f, 1f));
        a.setDuration(2400); a.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        a.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        a.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        v.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View x) { a.start(); }
            @Override public void onViewDetachedFromWindow(View x) { a.cancel(); }
        });
    }

    static void gradientText(TextView t, int... colors) {
        t.post(() -> {
            float w = t.getPaint().measureText(t.getText().toString());
            t.getPaint().setShader(new android.graphics.LinearGradient(0, 0, w, 0, colors, null, android.graphics.Shader.TileMode.CLAMP));
            t.invalidate();
        });
    }

    TextView chip(String s) {
        TextView t = text(s, 12, MUTED, true);
        t.setBackground(rounded(CARD2, 20));
        t.setPadding(dp(11), dp(5), dp(11), dp(5));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(dp(3), dp(4), dp(3), dp(4));
        t.setLayoutParams(lp);
        return t;
    }

    /** A slowly rotating gradient ring (purely decorative, in-app only). */
    static final class Ring extends View {
        private final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final float stroke;
        private android.animation.ObjectAnimator spin;
        Ring(Context c, float strokePx) {
            super(c);
            stroke = strokePx;
            p.setStyle(android.graphics.Paint.Style.STROKE);
            p.setStrokeWidth(strokePx);
            p.setStrokeCap(android.graphics.Paint.Cap.ROUND);
        }
        @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
            p.setShader(new android.graphics.SweepGradient(w / 2f, h / 2f, new int[]{ACCENT1, ACCENT2, 0x00000000, 0x00000000, ACCENT1},
                    new float[]{0f, 0.3f, 0.62f, 0.9f, 1f}));
            p.setShadowLayer(stroke * 2.5f, 0, 0, ACCENT2);
        }
        @Override protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            spin = android.animation.ObjectAnimator.ofFloat(this, "rotation", 0f, 360f);
            spin.setDuration(6000); spin.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            spin.setInterpolator(new android.view.animation.LinearInterpolator());
            spin.start();
        }
        @Override protected void onDetachedFromWindow() { if (spin != null) spin.cancel(); super.onDetachedFromWindow(); }
        @Override protected void onDraw(android.graphics.Canvas cv) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f, r = Math.min(cx, cy) - stroke * 3;
            cv.drawCircle(cx, cy, r, p);
        }
    }

    /** An ImageView that is always square (height = width). */
    static final class Square extends android.widget.ImageView {
        Square(Context c) { super(c); }
        @Override protected void onMeasure(int w, int h) { super.onMeasure(w, w); }
    }

    /** Line icons for the bottom navigation: 0 gallery, 1 studio, 2 heart, 3 watch. */
    static final class Icon extends View {
        private final int type;
        private final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private int color = MUTED;
        Icon(Context c, int type) {
            super(c);
            this.type = type;
            p.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            p.setStrokeJoin(android.graphics.Paint.Join.ROUND);
        }
        void setColor(int c) { color = c; invalidate(); }
        @Override protected void onDraw(android.graphics.Canvas cv) {
            float s = Math.min(getWidth(), getHeight()), ox = (getWidth() - s) / 2f, oy = (getHeight() - s) / 2f;
            p.setColor(color); p.setStrokeWidth(s * 0.085f); p.setStyle(android.graphics.Paint.Style.STROKE);
            if (type == 0) {
                float g = s * 0.12f, b = (s - 3 * g) / 2;
                for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) {
                    float x = ox + g + i * (b + g), y = oy + g + j * (b + g);
                    cv.drawRoundRect(new android.graphics.RectF(x, y, x + b, y + b), s * 0.09f, s * 0.09f, p);
                }
            } else if (type == 1) {
                for (int k = 0; k < 3; k++) {
                    float y = oy + s * (0.24f + 0.26f * k);
                    cv.drawLine(ox + s * 0.12f, y, ox + s * 0.88f, y, p);
                    p.setStyle(android.graphics.Paint.Style.FILL);
                    cv.drawCircle(ox + s * (k == 1 ? 0.66f : 0.34f), y, s * 0.1f, p);
                    p.setStyle(android.graphics.Paint.Style.STROKE);
                }
            } else if (type == 2) {
                android.graphics.Path h = new android.graphics.Path();
                float cx = ox + s / 2;
                h.moveTo(cx, oy + s * 0.84f);
                h.cubicTo(ox + s * 0.02f, oy + s * 0.52f, ox + s * 0.14f, oy + s * 0.1f, cx, oy + s * 0.3f);
                h.cubicTo(ox + s * 0.86f, oy + s * 0.1f, ox + s * 0.98f, oy + s * 0.52f, cx, oy + s * 0.84f);
                cv.drawPath(h, p);
            } else {
                float cx = ox + s / 2, cy = oy + s / 2;
                cv.drawRoundRect(new android.graphics.RectF(cx - s * 0.17f, oy + s * 0.04f, cx + s * 0.17f, oy + s * 0.22f), s * 0.05f, s * 0.05f, p);
                cv.drawRoundRect(new android.graphics.RectF(cx - s * 0.17f, oy + s * 0.78f, cx + s * 0.17f, oy + s * 0.96f), s * 0.05f, s * 0.05f, p);
                cv.drawCircle(cx, cy, s * 0.29f, p);
                cv.drawLine(cx, cy, cx, cy - s * 0.15f, p);
                cv.drawLine(cx, cy, cx + s * 0.11f, cy + s * 0.05f, p);
            }
        }
    }

    View gradientSwatches(int[][] grads, IntCb cbIndex) {
        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout r = row();
        for (int i = 0; i < grads.length; i++) {
            View s = new View(c);
            GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, grads[i]);
            g.setShape(GradientDrawable.OVAL);
            s.setBackground(g);
            final int idx = i;
            s.setOnClickListener(v -> cbIndex.on(idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(42), dp(42));
            lp.rightMargin = dp(10);
            r.addView(s, lp);
        }
        hs.addView(r);
        hs.setPadding(0, dp(4), 0, dp(8));
        return hs;
    }

    /** Live in-app animation over a face preview: particles chosen per art style + a sweeping seconds ring. */
    static final class LiveFx extends View {
        static final int STARS = 0, BOKEH = 1, RAIN = 2, FIREFLY = 3, SHIMMER = 4, EMBERS = 5;
        private final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Path clip = new android.graphics.Path();
        private final java.util.Random r = new java.util.Random(7);
        private final int mode, color;
        private final boolean seconds;
        private float[][] pt = new float[0][5];
        private final long t0 = System.currentTimeMillis();

        LiveFx(Context c, int artId, int accent, boolean seconds) {
            super(c);
            this.mode = modeFor(artId); this.color = accent | 0xFF000000; this.seconds = seconds;
        }

        static int modeFor(int art) {
            switch (art) {
                case 5: case 6: case 22: case 17: return BOKEH;
                case 26: case 24: case 21: case 3: return RAIN;
                case 27: case 18: return FIREFLY;
                case 12: case 20: case 8: case 29: case 14: case 15: case 10: return SHIMMER;
                case 11: case 2: case 28: return EMBERS;
                default: return STARS;
            }
        }

        @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
            int n = mode == BOKEH ? 14 : mode == RAIN ? 26 : mode == FIREFLY ? 22 : mode == EMBERS ? 26 : mode == SHIMMER ? 0 : 46;
            pt = new float[n][5];
            for (float[] q : pt) { q[0] = r.nextFloat() * w; q[1] = r.nextFloat() * h; q[2] = r.nextFloat() * 6.28f; q[3] = 0.4f + r.nextFloat(); q[4] = r.nextFloat(); }
            clip.reset();
            clip.addCircle(w / 2f, h / 2f, Math.min(w, h) / 2f, android.graphics.Path.Direction.CW);
        }

        @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); postInvalidateOnAnimation(); }

        @Override protected void onDraw(android.graphics.Canvas cv) {
            if (!isAttachedToWindow() || getWidth() == 0) return;
            float w = getWidth(), h = getHeight(), cx = w / 2, cy = h / 2, R = Math.min(cx, cy);
            float t = (System.currentTimeMillis() - t0) / 1000f;
            int rgb = color & 0xFFFFFF;
            cv.save(); cv.clipPath(clip);
            p.setStyle(android.graphics.Paint.Style.FILL); p.setShader(null);
            switch (mode) {
                case STARS: {
                    for (float[] q : pt) {
                        float a = 0.5f + 0.5f * (float) Math.sin(t * 2.2f * q[3] + q[2]);
                        p.setColor(((int) (a * a * 230) << 24) | 0xFFFFFF);
                        float s = R * 0.012f * (0.6f + q[4]);
                        cv.drawCircle(q[0], q[1], s, p);
                        if (q[4] > 0.8f) {
                            cv.drawRect(q[0] - s * 4 * a, q[1] - s * 0.25f, q[0] + s * 4 * a, q[1] + s * 0.25f, p);
                            cv.drawRect(q[0] - s * 0.25f, q[1] - s * 4 * a, q[0] + s * 0.25f, q[1] + s * 4 * a, p);
                        }
                    }
                    float sh = (t % 7f) / 1.2f;          // a shooting star every 7 s
                    if (sh < 1) {
                        float sx = w * (0.1f + sh * 0.8f), sy = h * (0.15f + sh * 0.35f);
                        p.setShader(new android.graphics.LinearGradient(sx, sy, sx - R * 0.3f, sy - R * 0.13f, 0xDDFFFFFF, 0x00FFFFFF, android.graphics.Shader.TileMode.CLAMP));
                        p.setStrokeWidth(R * 0.012f); p.setStyle(android.graphics.Paint.Style.STROKE);
                        cv.drawLine(sx, sy, sx - R * 0.3f, sy - R * 0.13f, p);
                        p.setShader(null); p.setStyle(android.graphics.Paint.Style.FILL);
                    }
                    break;
                }
                case BOKEH:
                    for (float[] q : pt) {
                        float x = q[0] + (float) Math.sin(t * 0.3f * q[3] + q[2]) * R * 0.15f;
                        float y = ((q[1] - t * R * 0.03f * q[3]) % h + h) % h;
                        float a = 0.18f + 0.14f * (float) Math.sin(t * q[3] + q[2]);
                        p.setColor(((int) (a * 255) << 24) | (q[4] > 0.5f ? rgb : 0xFFFFFF));
                        cv.drawCircle(x, y, R * (0.05f + q[4] * 0.1f), p);
                    }
                    break;
                case RAIN:
                    for (float[] q : pt) {
                        float y = (q[1] + t * R * 0.5f * q[3]) % (h + R * 0.4f) - R * 0.05f;
                        float x = Math.round(q[0] / (R * 0.08f)) * R * 0.08f;
                        p.setShader(new android.graphics.LinearGradient(x, y, x, y - R * 0.35f, 0xCC000000 | rgb, rgb, android.graphics.Shader.TileMode.CLAMP));
                        cv.drawRect(x - R * 0.008f, y - R * 0.35f, x + R * 0.008f, y, p);
                    }
                    p.setShader(null);
                    break;
                case FIREFLY: case EMBERS:
                    for (float[] q : pt) {
                        boolean emb = mode == EMBERS;
                        float x = q[0] + (float) Math.sin(t * 0.7f * q[3] + q[2]) * R * 0.08f;
                        float y = emb ? ((q[1] - t * R * 0.08f * q[3]) % h + h) % h : q[1] + (float) Math.cos(t * 0.5f * q[3] + q[2]) * R * 0.06f;
                        float a = 0.5f + 0.5f * (float) Math.sin(t * 1.7f * q[3] + q[2] * 3);
                        p.setColor(((int) (a * 90) << 24) | rgb);
                        cv.drawCircle(x, y, R * 0.04f, p);
                        p.setColor(((int) (a * 255) << 24) | 0xFFFFFF);
                        cv.drawCircle(x, y, R * 0.008f, p);
                    }
                    break;
                default: { // SHIMMER: a soft light sweep every 4 s
                    float x0 = w * ((t % 4f) / 4f * 2.6f - 0.8f);
                    p.setShader(new android.graphics.LinearGradient(x0 - R * 0.4f, 0, x0 + R * 0.4f, h * 0.4f,
                            new int[]{0x00FFFFFF, 0x40FFFFFF, 0x00FFFFFF}, null, android.graphics.Shader.TileMode.CLAMP));
                    cv.drawRect(0, 0, w, h, p);
                    p.setShader(null);
                }
            }
            cv.restore();
            if (seconds) {                           // live seconds ring
                java.util.Calendar c = java.util.Calendar.getInstance();
                float sec = c.get(java.util.Calendar.SECOND) + c.get(java.util.Calendar.MILLISECOND) / 1000f;
                float sw = R * 0.025f, rr = R - sw;
                p.setStyle(android.graphics.Paint.Style.STROKE); p.setStrokeWidth(sw); p.setStrokeCap(android.graphics.Paint.Cap.ROUND);
                p.setColor(0x22FFFFFF);
                cv.drawCircle(cx, cy, rr, p);
                p.setColor(0xFF000000 | rgb);
                cv.drawArc(cx - rr, cy - rr, cx + rr, cy + rr, -90, sec * 6, false, p);
                p.setStyle(android.graphics.Paint.Style.FILL);
                double a = Math.toRadians(sec * 6 - 90);
                p.setColor(0xFFFFFFFF);
                cv.drawCircle(cx + (float) Math.cos(a) * rr, cy + (float) Math.sin(a) * rr, sw * 0.9f, p);
            }
            postInvalidateOnAnimation();
        }
    }

    /** The Lumina logo: a spinning gradient halo around a pulsing four-point spark. */
    static final class Logo extends View {
        private final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        private final android.graphics.Path star = new android.graphics.Path();
        private final long t0 = System.currentTimeMillis();
        Logo(Context c) { super(c); setLayerType(LAYER_TYPE_SOFTWARE, null); }
        @Override protected void onDraw(android.graphics.Canvas cv) {
            float w = getWidth(), h = getHeight(), cx = w / 2, cy = h / 2, R = Math.min(cx, cy);
            float t = (System.currentTimeMillis() - t0) / 1000f;
            p.setShader(null); p.setStyle(android.graphics.Paint.Style.FILL);
            p.setShader(new android.graphics.RadialGradient(cx, cy, R, new int[]{0xFF221A45, 0xFF0B0C12}, null, android.graphics.Shader.TileMode.CLAMP));
            cv.drawCircle(cx, cy, R * 0.96f, p);
            cv.save(); cv.rotate(t * 90, cx, cy);
            p.setStyle(android.graphics.Paint.Style.STROKE); p.setStrokeWidth(R * 0.12f); p.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            p.setShader(new android.graphics.SweepGradient(cx, cy, new int[]{ACCENT1, ACCENT2, 0xFFF472B6, ACCENT1}, null));
            p.setShadowLayer(R * 0.15f, 0, 0, ACCENT2);
            cv.drawArc(cx - R * 0.74f, cy - R * 0.74f, cx + R * 0.74f, cy + R * 0.74f, 20, 300, false, p);
            p.clearShadowLayer();
            cv.restore();
            float k = 0.85f + 0.15f * (float) Math.sin(t * 3), s = R * 0.42f * k, in = s * 0.22f;
            star.reset();
            star.moveTo(cx, cy - s); star.quadTo(cx + in, cy - in, cx + s, cy); star.quadTo(cx + in, cy + in, cx, cy + s);
            star.quadTo(cx - in, cy + in, cx - s, cy); star.quadTo(cx - in, cy - in, cx, cy - s); star.close();
            p.setShader(null); p.setStyle(android.graphics.Paint.Style.FILL); p.setColor(0xFFFFFFFF);
            p.setShadowLayer(R * 0.2f, 0, 0, 0xFFC4B5FD);
            cv.drawPath(star, p);
            p.clearShadowLayer();
            postInvalidateOnAnimation();
        }
    }
}
