package io.github.crealivity.dptvcursor.helper;

import static io.github.crealivity.dptvcursor.engine.impl.MouseEmulationEngine.bossKey;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import io.github.crealivity.dptvcursor.R;

public class KeyDetection extends AppCompatActivity{

    public static boolean isDetectionActivityInForeground = false;

    @SuppressLint("StaticFieldLeak")
    private static Activity activity;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_detection);
        Helper.limitWidthIfTvPercent(this, R.id.main, 0.6f);
        activity = KeyDetection.this;
    }

    public KeyDetection(){}

    public KeyDetection(KeyEvent event){
        if (isDetectionActivityInForeground) getKey(event);
    }

    private void getKey(KeyEvent event) {
        TextView textView = activity.findViewById(R.id.pressed_key);
        if (event.getAction() == KeyEvent.ACTION_DOWN) textView.setText(event.getKeyCode()+"");
        else if (event.getAction() == KeyEvent.ACTION_UP) textView.setText(" ");
        if (event.getEventTime() - event.getDownTime() > 1000)
            changeBossKey(event.getKeyCode());
    }

    public static void changeBossKey(int keyCode){
        AlertDialog.Builder builder = new AlertDialog.Builder(KeyDetection.activity, R.style.Wolf_Alert_Disp);
        builder.setTitle("Confirm your changes");
        builder.setMessage("Do you really want to set Key \""+ keyCode + "\" as new Shortcut key??");
        builder.setPositiveButton("YES", (dialogInterface, i) -> {
            Helper.setBossKeyDisabled(activity, false);
            Helper.setOverrideStatus(activity, true);
            Helper.setBossKeyValue(activity, keyCode);
            bossKey = keyCode;
            Helper.toast(activity, "New Shortcut key is : "+keyCode, Toast.LENGTH_SHORT);
            dialogInterface.dismiss();
            activity.finish();
        });
        builder.setNegativeButton("NO", (dialog, whichButton) -> dialog.dismiss());
        AlertDialog alert = builder.create();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alert.show();
    }


    @Override
    protected void onResume() {
        isDetectionActivityInForeground = true;
        super.onResume();
    }

    @Override
    protected void onPause() {
        isDetectionActivityInForeground = false;
        super.onPause();
    }
}
