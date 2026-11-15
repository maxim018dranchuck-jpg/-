package com.liquidkb.keyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Экран настроек: включение клавиатуры, пресеты, ползунки, своя картинка-фон. */
public class SettingsActivity extends Activity {
    private static final int REQ_IMAGE = 7;

    private LinearLayout root;
    private final List<Runnable> syncers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        setTheme(android.R.style.Theme_DeviceDefault_DayNight);
        super.onCreate(b);

        ScrollView sv = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20), dp(16), dp(40));
        sv.addView(root);
        setContentView(sv);

        title("Liquid Keyboard", 24);
        text("Стеклянная клавиатура с размытием фона. Все настройки применяются сразу — "
                + "нажми на поле проверки и двигай ползунки.");

        button("1. Включить клавиатуру в системе", new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        });
        button("2. Выбрать Liquid Keyboard для ввода", new View.OnClickListener() {
            @Override public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
            }
        });

        EditText test = new EditText(this);
        test.setHint("Поле для проверки клавиатуры");
        test.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        root.addView(test);

        section("Пресеты");
        LinearLayout pr = new LinearLayout(this);
        pr.setOrientation(LinearLayout.VERTICAL);
        root.addView(pr);
        preset(pr, "Liquid Light", new String[]{Prefs.THEME, "1", Prefs.BLUR, "60", Prefs.PANEL, "55",
                Prefs.KEY_ALPHA, "75", Prefs.BORDER, "80", Prefs.SHINE, "65", Prefs.RADIUS, "14",
                Prefs.SWELL, "55", Prefs.SAT, "0"});
        preset(pr, "Liquid Dark", new String[]{Prefs.THEME, "2", Prefs.BLUR, "70", Prefs.PANEL, "45",
                Prefs.KEY_ALPHA, "55", Prefs.BORDER, "70", Prefs.SHINE, "55", Prefs.RADIUS, "14",
                Prefs.SWELL, "55", Prefs.SAT, "0"});
        preset(pr, "Clear Glass (почти прозрачная)", new String[]{Prefs.THEME, "0", Prefs.BLUR, "40",
                Prefs.PANEL, "10", Prefs.KEY_ALPHA, "35", Prefs.BORDER, "90", Prefs.SHINE, "75",
                Prefs.RADIUS, "16", Prefs.SWELL, "60", Prefs.SAT, "0"});
        preset(pr, "Frosted (матовое стекло)", new String[]{Prefs.BLUR, "100", Prefs.PANEL, "60",
                Prefs.KEY_ALPHA, "60", Prefs.BORDER, "50", Prefs.SHINE, "40", Prefs.RADIUS, "10",
                Prefs.SWELL, "30", Prefs.SAT, "0"});
        preset(pr, "Aurora (цветное)", new String[]{Prefs.THEME, "2", Prefs.BLUR, "80", Prefs.PANEL, "35",
                Prefs.KEY_ALPHA, "55", Prefs.BORDER, "85", Prefs.SHINE, "70", Prefs.SAT, "70",
                Prefs.HUE, "260", Prefs.RADIUS, "14", Prefs.SWELL, "60"});

        section("Стекло");
        slider("Размытие фона за клавиатурой", Prefs.BLUR, 0, 100);
        slider("Плотность подложки", Prefs.PANEL, 0, 100);
        slider("Плотность стекла клавиш", Prefs.KEY_ALPHA, 0, 100);
        slider("Яркость светлой кромки", Prefs.BORDER, 0, 100);
        slider("Блик на клавишах", Prefs.SHINE, 0, 100);
        slider("«Набухание» клавиши при нажатии", Prefs.SWELL, 0, 100);
        slider("Оттенок", Prefs.HUE, 0, 360);
        slider("Насыщенность оттенка (0 = без цвета)", Prefs.SAT, 0, 100);

        section("Форма");
        slider("Скругление клавиш", Prefs.RADIUS, 0, 24);
        slider("Зазор между клавишами", Prefs.GAP, 0, 10);
        slider("Высота клавиатуры, %", Prefs.HEIGHT, 70, 140);
        slider("Размер букв", Prefs.TEXT, 14, 32);

        section("Тема");
        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.HORIZONTAL);
        final String[] tn = {"Авто (как в системе)", "Светлая", "Тёмная"};
        for (int i = 0; i < 3; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setId(100 + i);
            rb.setText(tn[i]);
            rg.addView(rb);
        }
        root.addView(rg);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup g, int id) {
                if (id >= 100) Prefs.sp(SettingsActivity.this).edit().putInt(Prefs.THEME, id - 100).apply();
            }
        });
        final RadioGroup rgf = rg;
        syncers.add(new Runnable() {
            @Override public void run() { rgf.check(100 + Prefs.get(SettingsActivity.this, Prefs.THEME)); }
        });

        section("Поведение");
        toggle("Вибро при нажатии", Prefs.HAPTIC);
        toggle("Ряд цифр сверху", Prefs.NUMROW);
        toggle("Буква над клавишей при нажатии", Prefs.POPUP);
        toggle("Маленькие подсказки на клавишах (ё, ъ, цифры)", Prefs.HINTS);

        section("Языки");
        langBox("Русский", 1);
        langBox("English", 2);

        section("Своя картинка под клавишами");
        button("Выбрать картинку", new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                startActivityForResult(i, REQ_IMAGE);
            }
        });
        button("Убрать картинку", new View.OnClickListener() {
            @Override public void onClick(View v) {
                new File(getFilesDir(), "bg.jpg").delete();
                bump();
            }
        });
        slider("Видимость картинки", Prefs.BG_ALPHA, 0, 100);

        section("Сброс");
        button("Вернуть настройки по умолчанию", new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.sp(SettingsActivity.this).edit().clear().apply();
                sync();
            }
        });
        sync();
    }

    // ---------- элементы интерфейса ----------

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void title(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setPadding(0, 0, 0, dp(8));
        root.addView(t);
    }

    private void section(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(18);
        t.setPadding(0, dp(22), 0, dp(6));
        root.addView(t);
    }

    private void text(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setPadding(0, 0, 0, dp(12));
        root.addView(t);
    }

    private void button(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        root.addView(b);
    }

    private void preset(LinearLayout parent, String name, final String[] kv) {
        Button b = new Button(this);
        b.setText(name);
        b.setAllCaps(false);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                SharedPreferences.Editor e = Prefs.sp(SettingsActivity.this).edit();
                for (int i = 0; i + 1 < kv.length; i += 2) e.putInt(kv[i], Integer.parseInt(kv[i + 1]));
                e.apply();
                sync();
            }
        });
        parent.addView(b);
    }

    private void slider(final String title, final String key, final int min, final int max) {
        final TextView label = new TextView(this);
        label.setPadding(0, dp(8), 0, 0);
        root.addView(label);
        final SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        root.addView(sb);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                label.setText(title + ": " + (p + min));
                if (fromUser) Prefs.sp(SettingsActivity.this).edit().putInt(key, p + min).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
        syncers.add(new Runnable() {
            @Override public void run() { sb.setProgress(Prefs.get(SettingsActivity.this, key) - min); }
        });
    }

    private void toggle(String title, final String key) {
        final Switch sw = new Switch(this);
        sw.setText(title);
        sw.setPadding(0, dp(8), 0, dp(8));
        root.addView(sw);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton c, boolean on) {
                Prefs.sp(SettingsActivity.this).edit().putInt(key, on ? 1 : 0).apply();
            }
        });
        syncers.add(new Runnable() {
            @Override public void run() { sw.setChecked(Prefs.get(SettingsActivity.this, key) == 1); }
        });
    }

    private void langBox(String name, final int bit) {
        final CheckBox cb = new CheckBox(this);
        cb.setText(name);
        root.addView(cb);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton c, boolean on) {
                int m = Prefs.get(SettingsActivity.this, Prefs.LANGS);
                m = on ? (m | bit) : (m & ~bit);
                if ((m & 3) == 0) m = 3 ^ bit; // хотя бы один язык должен остаться
                Prefs.sp(SettingsActivity.this).edit().putInt(Prefs.LANGS, m).apply();
                sync();
            }
        });
        syncers.add(new Runnable() {
            @Override public void run() {
                cb.setChecked((Prefs.get(SettingsActivity.this, Prefs.LANGS) & bit) != 0);
            }
        });
    }

    private void sync() {
        for (Runnable r : syncers) r.run();
    }

    // ---------- картинка ----------

    private void bump() {
        // клавиатура следит за изменениями настроек, так она подхватит новую картинку
        Prefs.sp(this).edit().putInt("bgStamp", (int) (System.currentTimeMillis() / 1000)).apply();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_IMAGE || res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in = getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(in, null, o);
            if (in != null) in.close();
            int sample = 1;
            while (o.outWidth / sample > 1600) sample *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            in = getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(in, null, o2);
            if (in != null) in.close();
            if (bmp == null) throw new IllegalStateException("decode");
            FileOutputStream out = new FileOutputStream(new File(getFilesDir(), "bg.jpg"));
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.close();
            bump();
            Toast.makeText(this, "Картинка установлена", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть картинку", Toast.LENGTH_LONG).show();
        }
    }
}
