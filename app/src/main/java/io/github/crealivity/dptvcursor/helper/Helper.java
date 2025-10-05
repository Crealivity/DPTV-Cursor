package io.github.crealivity.dptvcursor.helper;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import java.util.List;

public class Helper {

    @SuppressLint("StaticFieldLeak")
    public static Context helperContext;
    static final String PREFS_ID = "DPTV";
    static final String PREF_KEY_CB_OVERRIDE_STAT = "CB_OVERRIDE_STAT";
    static final String PREF_KEY_CB_OVERRIDE_VAL = "CB_OVERRIDE_VAL";
    static final String PREF_KEY_MOUSE_ICON = "MOUSE_ICON";
    static final String PREF_KEY_MOUSE_SIZE = "MOUSE_SIZE";
    static final String PREF_KEY_MOUSE_SPEED = "MOUSE_SPEED";
    static final String PREF_KEY_SCROLL_SPEED = "SCROLL_SPEED";
    static final String PREF_KEY_MOUSE_BORDERED = "MOUSE_BORDERED";
    static final String PREF_KEY_CB_DISABLE_BOSSKEY = "DISABLE_BOSSKEY";
    static final String PREF_KEY_CB_BEHAVIOUR_BOSSKEY = "CB_BEHAVIOUR_BOSSKEY";
    static final String PREF_KEY_HIDE_IN_LAUNCHERS   = "HIDE_IN_LAUNCHERS";
    static final String PREF_KEY_DISABLE_TOASTS      = "DISABLE_TOASTS";
    static final String PREF_KEY_AUTO_HIDE           = "AUTO_HIDE";
    public static final String ACTION_REFRESH_CURSOR = "io.github.crealivity.dptvcursor.REFRESH_CURSOR";
    static final String PREF_KEY_DISABLE_INERTIA = "DISABLE_INERTIA";

    public static boolean isAccessibilityDisabled(Context ctx) {
        return !isAccServiceInstalled(ctx.getPackageName() + "/.services.MouseEventService", ctx);
    }

    public static boolean isAnotherServiceInstalled(Context ctx) {
        String fireTVSettings = "com.wolf.firetvsettings/.main.services.HomeService";
        String buttonMapper = "flar2.homebutton/.a.i";
        return isAccServiceInstalled(fireTVSettings, ctx) || isAccServiceInstalled(buttonMapper, ctx);
    }

    public static boolean isAccServiceInstalled(String serviceId, Context ctx) {
        AccessibilityManager am = (AccessibilityManager) ctx.getSystemService(Context.ACCESSIBILITY_SERVICE);
        List<AccessibilityServiceInfo> runningServices = null;
        if (am != null)  runningServices = am.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
        if (runningServices != null) {
            for (AccessibilityServiceInfo service : runningServices)
                if (serviceId.equals(service.getId())) return true;
        }
        return false;
    }

    public static boolean isOverlayDisabled(Context ctx) {
        if (ctx == null) return true;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {
            return false;
        }

        try {
            return !android.provider.Settings.canDrawOverlays(ctx.getApplicationContext());
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isOverriding(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID,
                Context.MODE_PRIVATE);
        return sp.getBoolean(PREF_KEY_CB_OVERRIDE_STAT, false);
    }

    @SuppressLint("ApplySharedPref")
    public static void setOverrideStatus(Context ctx, boolean val) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(PREF_KEY_CB_OVERRIDE_STAT, val);
        editor.commit();
    }

    public static int getBossKeyValue(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        return sp.getInt(PREF_KEY_CB_OVERRIDE_VAL, 164);
    }

    @SuppressLint("ApplySharedPref")
    public static void setBossKeyValue(Context ctx, int val) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(PREF_KEY_CB_OVERRIDE_VAL, val);
        editor.commit();
    }

    public static String getMouseIconPref(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        return sp.getString(PREF_KEY_MOUSE_ICON, "default");
    }

    @SuppressLint("ApplySharedPref")
    public static void setMouseIconPref(Context ctx, String val) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(PREF_KEY_MOUSE_ICON, val);
        editor.commit();
    }

    public static int getMouseSizePref(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        return sp.getInt(PREF_KEY_MOUSE_SIZE, 1);
    }

    @SuppressLint("ApplySharedPref")
    public static void setMouseSizePref(Context ctx, int val) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(PREF_KEY_MOUSE_SIZE, val);
        editor.commit();
    }

    public static int getScrollSpeed(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        return sp.getInt(PREF_KEY_SCROLL_SPEED, 4);  //15 my sweet spot
    }

    @SuppressLint("ApplySharedPref")
    public static void setScrollSpeed(Context ctx, int val) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(PREF_KEY_SCROLL_SPEED, val);
        editor.commit();
    }

    public static int getMouseSpeed(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        return sp.getInt(PREF_KEY_MOUSE_SPEED, 12); // default matches XML progress
    }

    @SuppressLint("ApplySharedPref")
    public static void setMouseSpeed(Context ctx, int val) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(PREF_KEY_MOUSE_SPEED, val);
        editor.commit();
    }

    public static boolean isInertiaDisabled(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        return sp.getBoolean(PREF_KEY_DISABLE_INERTIA, false);
    }

    @SuppressLint("ApplySharedPref")
    public static void setDisableInertia(Context ctx, boolean val) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        sp.edit().putBoolean(PREF_KEY_DISABLE_INERTIA, val).commit();
    }

    public static boolean getHideInLaunchers(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        return sp.getBoolean(PREF_KEY_HIDE_IN_LAUNCHERS, true); // default: hide
    }

    @SuppressLint("ApplySharedPref")
    public static void setHideInLaunchers(Context ctx, boolean val) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(PREF_KEY_HIDE_IN_LAUNCHERS, val);
        editor.commit();
    }

    public static boolean isLauncherPackage(Context ctx, String pkg) {
        if (pkg == null) return false;

        if (pkg.startsWith("com.android.settings")
                || pkg.startsWith("com.google.android.tvsettings")
                || pkg.startsWith("com.android.systemui")
                || pkg.startsWith("com.google.android.tv.launcherx.settings")
                || pkg.startsWith("com.android.tv.settings")
                || pkg.startsWith("com.android.tvsetup")
                || pkg.equals(ctx.getPackageName())) {
            return false;
        }

        PackageManager pm = ctx.getPackageManager();

        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);

        ResolveInfo def = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if (def != null && def.activityInfo != null &&
                pkg.equals(def.activityInfo.packageName)) {
            return true;
        }

        List<ResolveInfo> homes = pm.queryIntentActivities(homeIntent, 0);
        if (homes != null) {
            for (ResolveInfo ri : homes) {
                if (ri != null && ri.activityInfo != null &&
                        pkg.equals(ri.activityInfo.packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isToastsDisabled(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        return sp.getBoolean(PREF_KEY_DISABLE_TOASTS, false);
    }

    public static void toast(Context ctx, String msg, int duration) {
        if (ctx == null) return;
        if (isToastsDisabled(ctx)) return;
        Context app = ctx.getApplicationContext();
        android.widget.Toast.makeText(app, msg, duration).show();
    }

    @SuppressLint("ApplySharedPref")
    public static void setToastsDisabled(Context ctx, boolean val) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(PREF_KEY_DISABLE_TOASTS, val);
        editor.commit();
    }

    public static boolean isAutoHideEnabled(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        return sp.getBoolean(PREF_KEY_AUTO_HIDE, false);
    }

    @SuppressLint("ApplySharedPref")
    public static void setAutoHideEnabled(Context ctx, boolean val) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(PREF_KEY_AUTO_HIDE, val);
        editor.commit();
    }
    @SuppressLint("ApplySharedPref")
    public static void setMouseBordered(Context ctx, Boolean val) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(PREF_KEY_MOUSE_BORDERED, val);
        editor.commit();
    }

    public static boolean getMouseBordered(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        return sp.getBoolean(PREF_KEY_MOUSE_BORDERED, false);
    }

    @SuppressLint("ApplySharedPref")
    public static void setBossKeyDisabled(Context ctx, Boolean val) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(PREF_KEY_CB_DISABLE_BOSSKEY, val);
        editor.commit();
    }

    public static boolean isBossKeyDisabled(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        return sp.getBoolean(PREF_KEY_CB_DISABLE_BOSSKEY, false);
    }

    @SuppressLint("ApplySharedPref")
    public static void setBossKeyBehaviour(Context ctx, Boolean val) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(PREF_KEY_CB_BEHAVIOUR_BOSSKEY, val);
        editor.commit();
    }

    public static boolean isBossKeySetToToggle(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS_ID, Context.MODE_PRIVATE);
        return sp.getBoolean(PREF_KEY_CB_BEHAVIOUR_BOSSKEY, false);
    }
    public static void refreshCursor(Context ctx) {
        if (ctx == null) return;
        Intent i = new Intent(ACTION_REFRESH_CURSOR)
                .setPackage(ctx.getPackageName())
                .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        ctx.sendBroadcast(i);
    }

    public static boolean isTvDevice(Context context) {
        UiModeManager m = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return m != null && m.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    public static void limitWidthIfTvPercent(Activity activity, int layoutId, float percent) {
        if (!isTvDevice(activity)) return;

        if (percent <= 0f || percent > 1f) percent = 1f;

        View view = activity.findViewById(layoutId);
        if (view == null) return;

        int screenW = activity.getResources().getDisplayMetrics().widthPixels;
        int targetWidth = (int) (screenW * percent);

        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params == null) return;

            params.width = targetWidth;
            view.setLayoutParams(params);

            if (params instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) params;
                mlp.leftMargin = mlp.rightMargin = (screenW - targetWidth) / 2;
                view.setLayoutParams(mlp);
            }
        } else {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params == null) return;
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            view.setLayoutParams(params);
        }
    }

}
