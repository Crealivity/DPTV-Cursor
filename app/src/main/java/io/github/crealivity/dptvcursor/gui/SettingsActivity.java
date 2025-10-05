package io.github.crealivity.dptvcursor.gui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import io.github.crealivity.dptvcursor.R;
import io.github.crealivity.dptvcursor.engine.impl.MouseEmulationEngine;
import io.github.crealivity.dptvcursor.engine.impl.PointerControl;
import io.github.crealivity.dptvcursor.helper.Helper;
import io.github.crealivity.dptvcursor.helper.KeyDetection;

import static io.github.crealivity.dptvcursor.engine.impl.MouseEmulationEngine.bossKey;
import static io.github.crealivity.dptvcursor.engine.impl.MouseEmulationEngine.scrollSpeed;

public class SettingsActivity extends AppCompatActivity {
    private CheckBox cb_mouse_bordered, cb_disable_bossKey, cb_behaviour_bossKey;
    private CheckBox cb_hide_in_launchers, cb_disable_toasts, cb_auto_hide;
    private EditText et_override;
    private Button bt_saveBossKeyValue;
    private Spinner sp_mouse_icon;
    private SeekBar dsbar_mouse_size;
    private SeekBar dsbar_mouse_speed;
    private SeekBar dsbar_scroll_speed;
    private CheckBox cb_scroll_inertia;
    private boolean suppressSpinnerCallback = false;
    private Button btManageCustom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_gui);
        Helper.limitWidthIfTvPercent(this, R.id.main, 0.6f);

        bt_saveBossKeyValue   = findViewById(R.id.bt_saveBossKey);
        et_override           = findViewById(R.id.et_override);

        sp_mouse_icon         = findViewById(R.id.sp_mouse_icon);
        btManageCustom        = findViewById(R.id.bt_manage_custom);
        dsbar_mouse_size      = findViewById(R.id.dsbar_mouse_size);
        dsbar_mouse_speed     = findViewById(R.id.dsbar_mouse_speed);
        dsbar_scroll_speed    = findViewById(R.id.dsbar_scroll_speed);
        cb_scroll_inertia     = findViewById(R.id.cb_scroll_inertia);

        cb_mouse_bordered     = findViewById(R.id.cb_border_window);
        cb_hide_in_launchers  = findViewById(R.id.cb_hide_in_launchers);
        cb_disable_toasts     = findViewById(R.id.cb_disable_toasts);
        cb_auto_hide          = findViewById(R.id.cb_auto_hide);
        cb_disable_bossKey    = findViewById(R.id.cb_disable_bossKey);
        cb_behaviour_bossKey  = findViewById(R.id.cb_behaviour_bossKey);

        IconStyleSpinnerAdapter iconStyleSpinnerAdapter =
                new IconStyleSpinnerAdapter(this, R.layout.spinner_icon_text_gui, R.id.textView);
        sp_mouse_icon.setAdapter(iconStyleSpinnerAdapter);

        populateFromPrefs(iconStyleSpinnerAdapter);

        bt_saveBossKeyValue.setOnClickListener(view -> {
            String dat = et_override.getText().toString().replaceAll("[^0-9]", "");
            int keyValue = dat.isEmpty() ? KeyEvent.KEYCODE_VOLUME_MUTE : Integer.parseInt(dat);
            Helper.setOverrideStatus(this, isBossKeyChanged());
            Helper.setBossKeyValue(this, keyValue);
            bossKey = keyValue;
            Helper.toast(this, "New shortcut key is : " + keyValue, Toast.LENGTH_SHORT);
        });

        btManageCustom.setOnClickListener(v -> {
            startActivity(new Intent(this, CustomCursorActivity.class));
        });

        sp_mouse_icon.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (suppressSpinnerCallback) return;
                String style = ((IconStyleSpinnerAdapter) parent.getAdapter()).getItem(pos);
                Helper.setMouseIconPref(getApplicationContext(), style);
                Helper.refreshCursor(SettingsActivity.this);
                updateManageCustomButton(style);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        dsbar_mouse_size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) Helper.setMouseSizePref(getApplicationContext(), progress);
                Helper.refreshCursor(SettingsActivity.this);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        dsbar_mouse_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    Helper.setMouseSpeed(getApplicationContext(), progress);
                    MouseEmulationEngine.pointerSpeed = progress;
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        dsbar_scroll_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    Helper.setScrollSpeed(getApplicationContext(), progress);
                    scrollSpeed = progress;
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        cb_scroll_inertia = findViewById(R.id.cb_scroll_inertia);

        cb_scroll_inertia.setChecked(Helper.isInertiaDisabled(this));

        cb_scroll_inertia.setOnCheckedChangeListener((btn, checked) -> {
            Helper.setDisableInertia(getApplicationContext(), checked);
            MouseEmulationEngine.disableInertia = checked;  // keep runtime in sync
        });

        cb_mouse_bordered.setOnCheckedChangeListener((compoundButton, checked) -> {
            Helper.setMouseBordered(getApplicationContext(), checked);
            PointerControl.isBordered = checked;
        });

        cb_hide_in_launchers.setOnCheckedChangeListener((compoundButton, checked) ->
                Helper.setHideInLaunchers(getApplicationContext(), checked));

        cb_disable_toasts.setOnCheckedChangeListener((compoundButton, checked) ->
                Helper.setToastsDisabled(getApplicationContext(), checked));

        cb_auto_hide.setOnCheckedChangeListener((compoundButton, checked) ->
                Helper.setAutoHideEnabled(getApplicationContext(), checked));

        cb_disable_bossKey.setOnCheckedChangeListener((compoundButton, value) -> {
            Helper.setBossKeyDisabled(getApplicationContext(), value);
            MouseEmulationEngine.isBossKeyDisabled = value;
        });

        cb_behaviour_bossKey.setOnCheckedChangeListener((compoundButton, value) -> {
            Helper.setBossKeyBehaviour(getApplicationContext(), value);
            MouseEmulationEngine.isBossKeySetToToggle = value;
        });
    }

    private void updateManageCustomButton(String style) {
        boolean isCustom = "Custom".equals(style); // or PointerAssets.STYLE_CUSTOM.equals(style)

        // hide/show
        btManageCustom.setVisibility(isCustom ? View.VISIBLE : View.GONE);

        // or enable/disable + dim
        // btManageCustom.setEnabled(isCustom);
        // btManageCustom.setAlpha(isCustom ? 1f : 0.5f);
    }

    public void callDetect(View v) {
        startActivity(new Intent(this, KeyDetection.class));
    }

    private boolean isBossKeyChanged() {
        return Helper.getBossKeyValue(this) != 164;
    }

    private void refreshSpinnerPreview() {
        String style = Helper.getMouseIconPref(this);

        IconStyleSpinnerAdapter fresh =
                new IconStyleSpinnerAdapter(this, R.layout.spinner_icon_text_gui, R.id.textView);

        suppressSpinnerCallback = true;
        sp_mouse_icon.setAdapter(fresh);
        sp_mouse_icon.setSelection(fresh.getPosition(style), false);
        suppressSpinnerCallback = false;

        updateManageCustomButton(style);
    }

    private void populateFromPrefs(IconStyleSpinnerAdapter adapter) {
        Context ctx = getApplicationContext();

        et_override.setText(String.valueOf(Helper.getBossKeyValue(ctx)));

        String iconStyle = Helper.getMouseIconPref(ctx);
        suppressSpinnerCallback = true;
        sp_mouse_icon.setSelection(adapter.getPosition(iconStyle));
        suppressSpinnerCallback = false;

        updateManageCustomButton(iconStyle);

        int mouseSize  = Helper.getMouseSizePref(ctx);
        dsbar_mouse_size.setProgress(clamp(mouseSize, 0, dsbar_mouse_size.getMax()));

        int mouseSpeed = Helper.getMouseSpeed(ctx);
        dsbar_mouse_speed.setProgress(clamp(mouseSpeed, 0, dsbar_mouse_speed.getMax()));

        int scSpeed    = Helper.getScrollSpeed(ctx);
        dsbar_scroll_speed.setProgress(clamp(scSpeed, 0, dsbar_scroll_speed.getMax()));

        cb_scroll_inertia.setChecked(Helper.isInertiaDisabled(ctx));

        cb_mouse_bordered.setChecked(Helper.getMouseBordered(ctx));
        cb_hide_in_launchers.setChecked(Helper.getHideInLaunchers(ctx));
        cb_disable_toasts.setChecked(Helper.isToastsDisabled(ctx));
        cb_auto_hide.setChecked(Helper.isAutoHideEnabled(ctx));
        cb_disable_bossKey.setChecked(Helper.isBossKeyDisabled(ctx));
        cb_behaviour_bossKey.setChecked(Helper.isBossKeySetToToggle(ctx));
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(v, max));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (et_override != null) et_override.setText(Helper.getBossKeyValue(this) + "");
        refreshSpinnerPreview();
    }
}
