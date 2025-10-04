package io.github.crealivity.dptvcursor.gui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import io.github.crealivity.dptvcursor.R;
import io.github.crealivity.dptvcursor.helper.Helper;

public class GuiActivity extends AppCompatActivity {
    private CountDownTimer repopulate;
    private TextView tvStatusSummary;
    private View cardStatus;
    private Button completeSetup;
    private Button cursorSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_gui);
        Helper.limitWidthIfTvPercent(this, R.id.main, 0.6f);  // limit to ~1000dp width on TV

        tvStatusSummary = findViewById(R.id.tv_status_summary);
        cursorSettings = findViewById(R.id.gui_cursor_settings);
        cardStatus = findViewById(R.id.card_status);
        completeSetup = findViewById(R.id.bt_complete_setup);

        completeSetup.setOnClickListener(v ->
                startActivity(new Intent(this, InstallationActivity.class))
        );

        cursorSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class))
        );

        updateStatusSummary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPolling();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPolling();
    }


    private void updateStatusSummary() {
        boolean overlayOK = !Helper.isOverlayDisabled(this);
        boolean accOK = !Helper.isAccessibilityDisabled(this);

        if (overlayOK && accOK) {
            tvStatusSummary.setText(R.string.installation_complete);
            completeSetup.setVisibility(View.GONE);
        } else {
            tvStatusSummary.setText(R.string.installation_incomplete);
            completeSetup.setVisibility(View.VISIBLE);
        }
    }

    private void startPolling() {
        stopPolling();
        repopulate = new CountDownTimer(2000, 2000) {
            @Override public void onTick(long l) {}
            @Override public void onFinish() {
                updateStatusSummary();
                startPolling();
            }
        };
        repopulate.start();
    }

    private void stopPolling() {
        if (repopulate != null) {
            repopulate.cancel();
            repopulate = null;
        }
    }
}