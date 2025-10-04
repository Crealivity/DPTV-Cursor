package io.github.crealivity.dptvcursor.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class OverlayView extends RelativeLayout {
    private static String LOG_TAG = "DPTV Cursor overlay";

    public OverlayView(Context context) {
        super(context);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams overlayParams = new WindowManager.LayoutParams();
        overlayParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        overlayParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        overlayParams.format = PixelFormat.TRANSPARENT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            /**overlayParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY |
                    WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;**/
            overlayParams.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
            overlayParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                overlayParams.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            }
            overlayParams.gravity = Gravity.TOP | Gravity.START;
            overlayParams.x = 0; overlayParams.y = 0;
            overlayParams.alpha = (float) 0.8;
        } else {
            overlayParams.gravity = Gravity.TOP | Gravity.START;
            overlayParams.x = 0;
            overlayParams.y = 0;

            overlayParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT; // was TYPE_PRIORITY_PHONE
            overlayParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN   // <— important
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;  // nice to have
        }

        wm.addView(this, overlayParams);
    }

    public void addFullScreenLayer (View v) {
        RelativeLayout.LayoutParams lp= new RelativeLayout.LayoutParams(this.getWidth(), this.getHeight());
        lp.width= RelativeLayout.LayoutParams.MATCH_PARENT;
        lp.height= RelativeLayout.LayoutParams.MATCH_PARENT;

        v.setLayoutParams(lp);
        this.addView(v);
        Log.i("Overlay view", "W - H : " + this.getWidth() + " " + this.getHeight());
    }
}
