package com.liquidkb.keyboard;

import android.content.Context;
import android.content.SharedPreferences;

/** Все настройки хранятся как целые числа в SharedPreferences. */
final class Prefs {
    static final String FILE = "liquidkb";

    static final String BLUR = "blur";          // радиус размытия фона за клавиатурой
    static final String PANEL = "panel";        // непрозрачность подложки клавиатуры
    static final String KEY_ALPHA = "keyAlpha"; // непрозрачность стекла клавиш
    static final String RADIUS = "radius";      // скругление клавиш, dp
    static final String GAP = "gap";            // зазор между клавишами, dp
    static final String HEIGHT = "height";      // высота рядов, %
    static final String TEXT = "text";          // размер букв, sp
    static final String BORDER = "border";      // яркость светлой кромки
    static final String SHINE = "shine";        // блик на клавише
    static final String SWELL = "swell";        // «набухание» клавиши при нажатии
    static final String HUE = "hue";            // оттенок, 0..360
    static final String SAT = "sat";            // насыщенность оттенка, 0..100
    static final String THEME = "theme";        // 0 авто, 1 светлая, 2 тёмная
    static final String HAPTIC = "haptic";      // вибро
    static final String NUMROW = "numRow";      // ряд цифр
    static final String POPUP = "popup";        // всплывающая буква над клавишей
    static final String LANGS = "langs";        // битовая маска: 1 русский, 2 английский
    static final String BG_ALPHA = "bgAlpha";   // видимость своей картинки-фона
    static final String HINTS = "hints";        // маленькие подсказки (ё, ъ, цифры)

    private Prefs() {}

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static int def(String k) {
        switch (k) {
            case BLUR: return 60;
            case PANEL: return 70;
            case KEY_ALPHA: return 70;
            case RADIUS: return 12;
            case GAP: return 4;
            case HEIGHT: return 100;
            case TEXT: return 22;
            case BORDER: return 70;
            case SHINE: return 60;
            case SWELL: return 50;
            case HUE: return 210;
            case SAT: return 0;
            case THEME: return 0;
            case HAPTIC: return 1;
            case NUMROW: return 1;
            case POPUP: return 1;
            case LANGS: return 3;
            case BG_ALPHA: return 100;
            case HINTS: return 1;
            default: return 0;
        }
    }

    static int get(Context c, String k) {
        return sp(c).getInt(k, def(k));
    }
}
