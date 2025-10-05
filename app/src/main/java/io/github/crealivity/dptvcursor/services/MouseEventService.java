package io.github.crealivity.dptvcursor.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import io.github.crealivity.dptvcursor.BuildConfig;
import io.github.crealivity.dptvcursor.debug.TreeLogger;
import io.github.crealivity.dptvcursor.engine.impl.MouseEmulationEngine;
import io.github.crealivity.dptvcursor.engine.impl.PointerControl;
import io.github.crealivity.dptvcursor.engine.legacy.LegacyInput;
import io.github.crealivity.dptvcursor.engine.modern.ModernInput;
import io.github.crealivity.dptvcursor.helper.Helper;
import io.github.crealivity.dptvcursor.helper.KeyDetection;
import io.github.crealivity.dptvcursor.view.OverlayView;

import static io.github.crealivity.dptvcursor.engine.impl.MouseEmulationEngine.bossKey;
import static io.github.crealivity.dptvcursor.engine.impl.MouseEmulationEngine.isOkKey;
import static io.github.crealivity.dptvcursor.engine.impl.MouseEmulationEngine.pointerSpeed;
import static io.github.crealivity.dptvcursor.engine.impl.MouseEmulationEngine.scrollSpeed;
import static io.github.crealivity.dptvcursor.helper.Helper.ACTION_REFRESH_CURSOR;

public class MouseEventService extends AccessibilityService {

    private MouseEmulationEngine mEngine;
    private static String TAG_NAME = "DPTV_SERVICE";

    private @androidx.annotation.Nullable String getActiveWindowPackage() {

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            java.util.List<android.view.accessibility.AccessibilityWindowInfo> windows = null;
            try {
                windows = getWindows();
            } catch (Throwable ignored) {}

            if (windows != null && !windows.isEmpty()) {

                for (android.view.accessibility.AccessibilityWindowInfo w : windows) {
                    if (w == null) continue;
                    android.view.accessibility.AccessibilityNodeInfo root = null;
                    try { root = w.getRoot(); } catch (Throwable ignored) {}
                    if (root == null) continue;

                    CharSequence pkg = null;
                    try { pkg = root.getPackageName(); } catch (Throwable ignored) {}

                    if (w.isActive() && pkg != null) {
                        return pkg.toString();
                    }
                }

                for (android.view.accessibility.AccessibilityWindowInfo w : windows) {
                    if (w == null) continue;
                    android.view.accessibility.AccessibilityNodeInfo root = null;
                    try { root = w.getRoot(); } catch (Throwable ignored) {}
                    if (root == null) continue;

                    CharSequence pkg = null;
                    try { pkg = root.getPackageName(); } catch (Throwable ignored) {}

                    if (pkg != null) {
                        return pkg.toString();
                    }
                }
            }
        }


        android.view.accessibility.AccessibilityNodeInfo root = null;
        try { root = getRootInActiveWindow(); } catch (Throwable ignored) {}
        if (root != null) {
            CharSequence pkg = null;
            try { pkg = root.getPackageName(); } catch (Throwable ignored) {}
            if (pkg != null) return pkg.toString();
        }
        return null;
    }

    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (mEngine != null) {
                try {
                    java.lang.reflect.Field f = mEngine.getClass().getDeclaredField("mPointerControl");
                    f.setAccessible(true);
                    PointerControl pc = (PointerControl) f.get(mEngine);
                    if (pc != null) pc.reset();
                } catch (Exception ignored) { }
            }
        }
    };

    @Override public void onDestroy() {
        try { unregisterReceiver(refreshReceiver); } catch (Exception ignored) {}
        super.onDestroy();
    }

    private void refreshLauncherSuppression(@androidx.annotation.Nullable String pkgFromEvent) {
        String pkg = (pkgFromEvent != null) ? pkgFromEvent : getActiveWindowPackage();
        boolean shouldHide = false;
        if (pkg != null) {
            shouldHide = Helper.getHideInLaunchers(this) && Helper.isLauncherPackage(this, pkg);
        }
        if (mEngine != null) {
            mEngine.setHideInLauncher(shouldHide);
        }
        android.util.Log.i("DPTV_SERVICE", "pkg=" + pkg + " hideInLauncher=" + shouldHide);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (mEngine == null) return;

        final int t = event.getEventType();
        if (t == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || t == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            mEngine.stopAllMotion();
        }

        String pkg = null;
        if (event != null && event.getPackageName() != null) {
            pkg = String.valueOf(event.getPackageName());
        }
        refreshLauncherSuppression(pkg);
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        super.onKeyEvent(event);

        // EMULATOR CLICK TEST: press 'ENTER' to force a mouse click
        if (TreeLogger.DEBUG && event.getAction() == KeyEvent.ACTION_UP && isOkKey(event.getKeyCode())) {
            boolean ok = false;
            if(ModernInput.isAvailable()){
                ok = (mEngine != null) && mEngine.forceModernClickAtPointer(
                        this,
                        Math.max(1, event.getEventTime() - event.getDownTime())
                );
            } else {
                ok = LegacyInput.clickAtPointer(this, mEngine != null ? mEngine.getPointerControl() : null);
            }
            android.util.Log.i("DPTV_SERVICE", "Emulator click result=" + ok);
            return true;
        }

        new KeyDetection(event);
        if (Helper.isAnotherServiceInstalled(this) &&
                event.getKeyCode() == KeyEvent.KEYCODE_HOME) return true;
        if (Helper.isOverlayDisabled(this)) return false;
        return mEngine.perform(event);
    }

    @Override
    public void onInterrupt() {
        if (mEngine != null) mEngine.stopAllMotion();  // << not static
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        IntentFilter filter = new IntentFilter(ACTION_REFRESH_CURSOR);

        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(refreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            // registerReceiver(refreshReceiver, new android.content.IntentFilter(ACTION_REFRESH_CURSOR));
            registerReceiver(refreshReceiver, filter);
        }
        AccessibilityServiceInfo asi = this.getServiceInfo();
        if (asi != null) {
            asi.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            asi.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            asi.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            asi.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    | AccessibilityEvent.TYPE_WINDOWS_CHANGED;
            setServiceInfo(asi);
        }

        Log.i(TAG_NAME, "Starting service initialization sequence. App version " + BuildConfig.VERSION_NAME);
        bossKey = KeyEvent.KEYCODE_VOLUME_MUTE;
        PointerControl.isBordered = Helper.getMouseBordered(this);
        scrollSpeed = Helper.getScrollSpeed(this);
        pointerSpeed = Helper.getMouseSpeed(this);
        MouseEmulationEngine.isBossKeyDisabled = Helper.isBossKeyDisabled(this);
        MouseEmulationEngine.isBossKeySetToToggle = Helper.isBossKeySetToToggle(this);
        if (Helper.isOverriding(this)) bossKey = Helper.getBossKeyValue(this);
        boolean ok = (android.os.Build.VERSION.SDK_INT < 23)
                || !Helper.isOverlayDisabled(this); //android.provider.Settings.canDrawOverlays(this);
        if (ok) init();

        String initialPkg = null;
        android.view.accessibility.AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null && root.getPackageName() != null) {
            initialPkg = root.getPackageName().toString();
        }
        boolean hide = Helper.getHideInLaunchers(this) && Helper.isLauncherPackage(this, initialPkg);
        if (mEngine != null) mEngine.setHideInLauncher(hide);
        Log.i(TAG_NAME, "Initial pkg=" + initialPkg + " hideInLauncher=" + hide);
    }

    private void init() {
        if (Helper.helperContext != null) Helper.helperContext = this;
        OverlayView mOverlayView = new OverlayView(this);
        AccessibilityServiceInfo asi = this.getServiceInfo();
        if (asi != null) {
            asi.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            this.setServiceInfo(asi);
        }

        /**Log.i(TAG_NAME, "Configuration -- Scroll speed " + scrollSpeed);
        Log.i(TAG_NAME, "Configuration -- Boss key disabled " + MouseEmulationEngine.isBossKeyDisabled);
        Log.i(TAG_NAME, "Configuration -- Boss Key toggleable " + MouseEmulationEngine.isBossKeySetToToggle);
        Log.i(TAG_NAME, "Configuration -- Is bordered " + PointerControl.isBordered);
        Log.i(TAG_NAME, "Configuration -- Shortcut key value " + bossKey);**/

        mEngine = new MouseEmulationEngine(this, mOverlayView);
        mEngine.init(this);
    }
}
