package io.github.crealivity.dptvcursor.gui;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import io.github.crealivity.dptvcursor.R;
import io.github.crealivity.dptvcursor.helper.Helper;

public class InstallationActivity extends AppCompatActivity {

    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 701;
    private static final int ACTION_ACCESSIBILITY_PERMISSION_REQUEST_CODE = 702;
    private CountDownTimer repopulate;
    private TextView tvAccPerm, tvAccServ, tvOverlayPerm, tvOverlayServ;
    private View cardStatus;
    private Button btnSetup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installation_process);
        Helper.limitWidthIfTvPercent(this, R.id.main, 0.6f);

        //tvStatusSummary = findViewById(R.id.tv_status_summary);
        tvAccPerm      = findViewById(R.id.gui_acc_perm);
        tvAccServ      = findViewById(R.id.gui_acc_serv);
        tvOverlayPerm  = findViewById(R.id.gui_overlay_perm);
        tvOverlayServ  = findViewById(R.id.gui_overlay_serv);
        cardStatus     = findViewById(R.id.card_status);
        btnSetup       = findViewById(R.id.gui_setup_perm);

        View.OnClickListener setupClick = v -> askPermissions();
        //cardStatus.setOnClickListener(setupClick);
        btnSetup.setOnClickListener(setupClick);

        populateText();
        updateSummary();
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

    private void startPolling() {
        stopPolling();
        repopulate = new CountDownTimer(2000, 2000) {
            @Override public void onTick(long l) {}
            @Override public void onFinish() {
                populateText();
                updateSummary();
                startPolling(); // loop
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


    private void populateText() {
        final boolean overlayDisabled = Helper.isOverlayDisabled(this);
        final boolean accDisabled     = Helper.isAccessibilityDisabled(this);

        if (overlayDisabled)  tvOverlayPerm.setText(R.string.perm_overlay_denied);
        else                  tvOverlayPerm.setText(R.string.perm_overlay_allowed);

        if (accDisabled) {
            tvAccPerm.setText(R.string.perm_acc_denied);
            tvAccServ.setText(R.string.serv_acc_denied);
            tvOverlayServ.setText(R.string.serv_overlay_denied);
        } else {
            tvAccPerm.setText(R.string.perm_acc_allowed);
        }

        if (accDisabled && overlayDisabled) {
            tvAccPerm.setText(R.string.perm_acc_denied);
            tvAccServ.setText(R.string.serv_acc_denied);
            tvOverlayPerm.setText(R.string.perm_overlay_denied);
            tvOverlayServ.setText(R.string.serv_overlay_denied);
        }

        if (!accDisabled && !overlayDisabled) {
            tvAccPerm.setText(R.string.perm_acc_allowed);
            tvAccServ.setText(R.string.serv_acc_allowed);
            tvOverlayPerm.setText(R.string.perm_overlay_allowed);
            tvOverlayServ.setText(R.string.serv_overlay_allowed);
            // Optional: hide CTA when everything is green
            btnSetup.setVisibility(View.GONE);
        } else {
            btnSetup.setVisibility(View.VISIBLE);
        }
    }

    private void updateSummary() {
        final boolean overlayDisabled = Helper.isOverlayDisabled(this);
        final boolean accDisabled     = Helper.isAccessibilityDisabled(this);
        final boolean complete = !overlayDisabled && !accDisabled;

        /**tvStatusSummary.setText(complete
                ? R.string.installation_complete
                : R.string.installation_incomplete);**/
    }

    private void askPermissions() {
        if (Helper.isOverlayDisabled(this)) {
            try {
                startActivityForResult(
                        new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
                        ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE
                );
                return;
            } catch (Exception unused) {
                Helper.toast(this, "Overlay Permission Handler not found", Toast.LENGTH_SHORT);
            }
        }
        if (!Helper.isOverlayDisabled(this) && Helper.isAccessibilityDisabled(this)) {
            openAccessibilitySettings();
        }
    }

    private void openAccessibilitySettings() {
        if (Helper.isAccessibilityDisabled(this)) {
            try {
                startActivityForResult(
                        new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                        ACTION_ACCESSIBILITY_PERMISSION_REQUEST_CODE
                );
            } catch (Exception e) {
                Helper.toast(this, "Accessibility Handler not found", Toast.LENGTH_SHORT);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Helper.isOverlayDisabled(this)) {
                Helper.toast(this, "Overlay permissions denied", Toast.LENGTH_SHORT);
            } else {
                openAccessibilitySettings();
            }
        } else if (requestCode == ACTION_ACCESSIBILITY_PERMISSION_REQUEST_CODE) {
            if (Helper.isAccessibilityDisabled(this)) {
                Helper.toast(this, "Accessibility service not running", Toast.LENGTH_SHORT);
            }
        }

        populateText();
        updateSummary();
    }
}