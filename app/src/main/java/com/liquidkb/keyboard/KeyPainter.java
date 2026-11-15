package com.liquidkb.keyboard;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.DisplayMetrics;
import android.util.TypedValue;

/** Рисует «жидкое стекло»: прозрачную заливку, блик сверху, светлую кромку и иконки клавиш. */
final class KeyPainter {
    boolean dark, hints, popup, noBlur;
    int tint, accent, textColor;
    float sat, keyAlpha, panelAlpha, border, shine, swell, bgAlpha;
    float radius, textPx, density;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint over = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hair = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint icon = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bgPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final RectF rf = new RectF();
    private final Path path = new Path();

    private Shader charShader, funcShader, shineShader, rimShader;
    private float shaderH = -1f;

    KeyPainter() {
        rim.setStyle(Paint.Style.STROKE);
        hair.setStyle(Paint.Style.STROKE);
        text.setTextAlign(Paint.Align.CENTER);
        hint.setTextAlign(Paint.Align.RIGHT);
        icon.setStyle(Paint.Style.STROKE);
        icon.setStrokeCap(Paint.Cap.ROUND);
        icon.setStrokeJoin(Paint.Join.ROUND);
    }

    /** Читает настройки заново. */
    void reload(Context c) {
        DisplayMetrics dm = c.getResources().getDisplayMetrics();
        density = dm.density;
        int theme = Prefs.get(c, Prefs.THEME);
        boolean night = (c.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        dark = theme == 2 || (theme == 0 && night);

        tint = Color.HSVToColor(new float[]{Prefs.get(c, Prefs.HUE), 1f, 1f});
        sat = Prefs.get(c, Prefs.SAT) / 100f;
        keyAlpha = Prefs.get(c, Prefs.KEY_ALPHA) / 100f;
        panelAlpha = Prefs.get(c, Prefs.PANEL) / 100f;
        border = Prefs.get(c, Prefs.BORDER) / 100f;
        shine = Prefs.get(c, Prefs.SHINE) / 100f;
        swell = Prefs.get(c, Prefs.SWELL) / 100f;
        bgAlpha = Prefs.get(c, Prefs.BG_ALPHA) / 100f;
        radius = dp(Prefs.get(c, Prefs.RADIUS));
        textPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                Prefs.get(c, Prefs.TEXT), dm);
        hints = Prefs.get(c, Prefs.HINTS) == 1;
        popup = Prefs.get(c, Prefs.POPUP) == 1;

        accent = sat > 0.05f ? tint : 0xFF3D7BFF;
        textColor = dark ? 0xFFFFFFFF : 0xFF14161C;
        shaderH = -1f; // пересобрать градиенты
    }

    float dp(float v) { return v * density; }

    static int a(int color, float f) {
        int al = Math.max(0, Math.min(255, Math.round(f * 255f)));
        return Color.argb(al, Color.red(color), Color.green(color), Color.blue(color));
    }

    static int mix(int c1, int c2, float t) {
        return Color.rgb(
                (int) (Color.red(c1) + (Color.red(c2) - Color.red(c1)) * t),
                (int) (Color.green(c1) + (Color.green(c2) - Color.green(c1)) * t),
                (int) (Color.blue(c1) + (Color.blue(c2) - Color.blue(c1)) * t));
    }

    private void prepare(float h) {
        if (h == shaderH) return;
        shaderH = h;
        float k = dark ? 0.55f : 1f;
        int base = mix(0xFFFFFFFF, tint, sat * 0.6f);
        charShader = new LinearGradient(0, 0, 0, h,
                a(base, keyAlpha * 1.25f * k), a(base, keyAlpha * 0.7f * k), Shader.TileMode.CLAMP);
        int fb = mix(dark ? 0xFF000000 : 0xFF5B6478, tint, sat * 0.5f);
        float fa = keyAlpha * (dark ? 0.6f : 0.5f);
        funcShader = new LinearGradient(0, 0, 0, h,
                a(fb, fa * 1.2f), a(fb, fa * 0.8f), Shader.TileMode.CLAMP);
        shineShader = new LinearGradient(0, 0, 0, h * 0.55f,
                a(0xFFFFFFFF, shine * 0.55f), 0x00FFFFFF, Shader.TileMode.CLAMP);
        rimShader = new LinearGradient(0, 0, 0, h,
                new int[]{a(0xFFFFFFFF, border), a(0xFFFFFFFF, border * 0.12f), a(0xFFFFFFFF, border * 0.5f)},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
    }

    /** Подложка всей клавиатуры: своя картинка (если есть) и тонировка. */
    void drawPanel(Canvas c, int w, int h, Bitmap bg) {
        if (bg != null && bgAlpha > 0f) {
            float s = Math.max(w / (float) bg.getWidth(), h / (float) bg.getHeight());
            float bw = bg.getWidth() * s, bh = bg.getHeight() * s;
            rf.set((w - bw) / 2f, (h - bh) / 2f, (w + bw) / 2f, (h + bh) / 2f);
            bgPaint.setAlpha(Math.round(bgAlpha * 255f));
            c.drawBitmap(bg, null, rf, bgPaint);
        }
        int base = mix(dark ? 0xFF0B0D14 : 0xFFF4F6FB, tint, sat * 0.5f);
        over.setShader(null);
        // без системного размытия подложка плотнее, иначе буквы сливаются с приложением
        over.setColor(a(base, noBlur ? Math.max(panelAlpha, 0.88f) : panelAlpha));
        c.drawRect(0, 0, w, h, over);
        over.setColor(a(0xFFFFFFFF, 0.25f * border));
        c.drawRect(0, 0, w, dp(1), over);
    }

    void drawKey(Canvas c, Key k, String label, int shift, float rowH) {
        if (k.kind == Key.SPACER) return;
        prepare(rowH);
        float sc = 1f + swell * 0.12f * k.press;
        float w = (k.r - k.l) * sc, h = (k.b - k.t) * sc;
        float r = Math.min(radius, Math.min(w, h) / 2f);
        boolean func = k.kind == Key.FUNC || k.kind == Key.ENTER;

        c.save();
        c.translate(k.cx() - w / 2f, k.cy() - h / 2f);

        fill.setShader(func ? funcShader : charShader);
        fill.setAlpha(255); // альфа цвета умножается на альфу шейдера
        c.drawRoundRect(0, 0, w, h, r, r, fill);
        if (k.kind == Key.ENTER) {
            over.setShader(null);
            over.setColor(a(accent, 0.6f));
            c.drawRoundRect(0, 0, w, h, r, r, over);
        }
        if (k.press > 0f) {
            over.setShader(null);
            over.setColor(a(0xFFFFFFFF, 0.28f * k.press));
            c.drawRoundRect(0, 0, w, h, r, r, over);
        }
        if (shine > 0f) {
            shinePaint.setShader(shineShader);
            float in = dp(1.2f);
            rf.set(in, in, w - in, h * 0.55f);
            c.drawRoundRect(rf, r * 0.8f, r * 0.8f, shinePaint);
        }
        float sw = dp(1.1f);
        rim.setShader(rimShader);
        rim.setAlpha(255);
        rim.setStrokeWidth(sw);
        rf.set(sw / 2f, sw / 2f, w - sw / 2f, h - sw / 2f);
        c.drawRoundRect(rf, r, r, rim);
        if (!dark) {
            hair.setColor(0x16000000);
            hair.setStrokeWidth(dp(0.6f));
            rf.set(-dp(0.3f), -dp(0.3f), w + dp(0.3f), h + dp(0.3f));
            c.drawRoundRect(rf, r, r, hair);
        }
        c.restore();

        // содержимое клавиши вместе с «набуханием»
        c.save();
        c.scale(sc, sc, k.cx(), k.cy());
        drawContent(c, k, label, shift);
        c.restore();
    }

    private void drawContent(Canvas c, Key k, String label, int shift) {
        float cx = k.cx(), cy = k.cy();
        text.setColor(textColor);
        if (k.kind == Key.CHAR) {
            text.setTextSize(textPx);
            c.drawText(label, cx, cy - (text.ascent() + text.descent()) / 2f, text);
            if (hints && k.alt != null && k.alt.length() > 0
                    && Character.isLetterOrDigit(k.alt.charAt(0))) {
                hint.setColor(a(textColor, 0.55f));
                hint.setTextSize(textPx * 0.45f);
                c.drawText(k.alt.substring(0, 1), k.r - dp(5), k.t + dp(12), hint);
            }
        } else if (k.kind == Key.SPACE) {
            text.setTextSize(textPx * 0.62f);
            text.setColor(a(textColor, 0.7f));
            c.drawText(label, cx, cy - (text.ascent() + text.descent()) / 2f, text);
        } else if (k.code == Key.C_SYM || k.code == Key.C_ABC) {
            text.setTextSize(textPx * 0.78f);
            c.drawText(label, cx, cy - (text.ascent() + text.descent()) / 2f, text);
        } else {
            icon(c, k.code, cx, cy, textPx * 0.5f, shift);
        }
    }

    private void icon(Canvas c, int code, float cx, float cy, float s, int shift) {
        icon.setColor(textColor);
        icon.setStrokeWidth(dp(1.8f));
        icon.setStyle(Paint.Style.STROKE);
        c.save();
        c.translate(cx, cy);
        path.reset();
        switch (code) {
            case Key.C_SHIFT:
                path.moveTo(0, -s);
                path.lineTo(s * 0.95f, 0);
                path.lineTo(s * 0.42f, 0);
                path.lineTo(s * 0.42f, s * 0.85f);
                path.lineTo(-s * 0.42f, s * 0.85f);
                path.lineTo(-s * 0.42f, 0);
                path.lineTo(-s * 0.95f, 0);
                path.close();
                if (shift > 0) icon.setStyle(Paint.Style.FILL_AND_STROKE);
                c.drawPath(path, icon);
                if (shift == 2) {
                    icon.setStyle(Paint.Style.STROKE);
                    c.drawLine(-s * 0.42f, s * 1.25f, s * 0.42f, s * 1.25f, icon);
                }
                break;
            case Key.C_DEL:
                path.moveTo(-s * 1.15f, 0);
                path.lineTo(-s * 0.4f, -s * 0.8f);
                path.lineTo(s * 1.1f, -s * 0.8f);
                path.lineTo(s * 1.1f, s * 0.8f);
                path.lineTo(-s * 0.4f, s * 0.8f);
                path.close();
                c.drawPath(path, icon);
                c.drawLine(-s * 0.05f, -s * 0.3f, s * 0.65f, s * 0.3f, icon);
                c.drawLine(-s * 0.05f, s * 0.3f, s * 0.65f, -s * 0.3f, icon);
                break;
            case Key.C_ENTER:
                path.moveTo(s * 0.95f, -s * 0.75f);
                path.lineTo(s * 0.95f, s * 0.2f);
                path.lineTo(-s * 0.9f, s * 0.2f);
                path.moveTo(-s * 0.3f, -s * 0.4f);
                path.lineTo(-s * 0.9f, s * 0.2f);
                path.lineTo(-s * 0.3f, s * 0.8f);
                c.drawPath(path, icon);
                break;
            case Key.C_LANG:
                c.drawCircle(0, 0, s, icon);
                rf.set(-s * 0.45f, -s, s * 0.45f, s);
                c.drawOval(rf, icon);
                c.drawLine(-s, 0, s, 0, icon);
                break;
            case Key.C_CLIP:
                rf.set(-s * 0.8f, -s * 0.85f, s * 0.8f, s);
                c.drawRoundRect(rf, s * 0.25f, s * 0.25f, icon);
                rf.set(-s * 0.38f, -s * 1.12f, s * 0.38f, -s * 0.6f);
                c.drawRoundRect(rf, s * 0.2f, s * 0.2f, icon);
                break;
            default:
                break;
        }
        c.restore();
    }

    /** Пузырёк с крупной буквой над нажатой клавишей. */
    void drawBubble(Canvas c, Key k, String label, int viewW) {
        float bw = Math.max((k.r - k.l) * 1.25f, dp(46));
        float bh = (k.b - k.t) * 1.2f;
        float x = Math.max(dp(2), Math.min(viewW - bw - dp(2), k.cx() - bw / 2f));
        float y = Math.max(dp(2), k.t - bh - dp(6));
        float r = radius * 1.2f;
        fill.setShader(null);
        fill.setColor(dark ? 0xF02A2F3C : 0xF2FFFFFF);
        rf.set(x, y, x + bw, y + bh);
        c.drawRoundRect(rf, r, r, fill);
        rim.setShader(null);
        rim.setColor(a(0xFFFFFFFF, 0.35f * border + 0.1f));
        rim.setStrokeWidth(dp(1f));
        c.drawRoundRect(rf, r, r, rim);
        text.setColor(textColor);
        text.setTextSize(textPx * 1.6f);
        c.drawText(label, x + bw / 2f, y + bh / 2f - (text.ascent() + text.descent()) / 2f, text);
    }

    /** {левый край, ширина ячейки} панели с вариантами символов. */
    float[] altGeom(Key k, int n, int viewW) {
        float cw = Math.max(dp(44), k.r - k.l);
        float total = cw * n;
        float x0 = Math.max(dp(2), Math.min(viewW - total - dp(2), k.cx() - total / 2f));
        return new float[]{x0, cw};
    }

    void drawAlt(Canvas c, Key k, String chars, int sel, int viewW) {
        float[] g = altGeom(k, chars.length(), viewW);
        float bh = (k.b - k.t) * 1.1f;
        float y = Math.max(dp(2), k.t - bh - dp(6));
        float r = radius * 1.1f;
        fill.setShader(null);
        fill.setColor(dark ? 0xF02A2F3C : 0xF2FFFFFF);
        rf.set(g[0], y, g[0] + g[1] * chars.length(), y + bh);
        c.drawRoundRect(rf, r, r, fill);
        text.setTextSize(textPx * 1.1f);
        for (int i = 0; i < chars.length(); i++) {
            float x = g[0] + g[1] * i;
            if (i == sel) {
                over.setShader(null);
                over.setColor(a(accent, 0.85f));
                rf.set(x + dp(3), y + dp(3), x + g[1] - dp(3), y + bh - dp(3));
                c.drawRoundRect(rf, r * 0.8f, r * 0.8f, over);
                text.setColor(0xFFFFFFFF);
            } else {
                text.setColor(textColor);
            }
            c.drawText(chars.substring(i, i + 1), x + g[1] / 2f,
                    y + bh / 2f - (text.ascent() + text.descent()) / 2f, text);
        }
    }
}
