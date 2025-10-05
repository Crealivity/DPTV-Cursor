package io.github.crealivity.dptvcursor.engine.modern;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;


public final class Windows21 {
    private Windows21() {}

    public static boolean isAvailable() { return Build.VERSION.SDK_INT >= 21; }

    public static final class ClickResult {
        public final boolean consumed;
        public final boolean wasIme;
        public ClickResult(boolean c, boolean w) { consumed = c; wasIme = w; }
    }

    public static ClickResult tryClickFromAllWindows(
            AccessibilityService svc,
            Point pInt,
            int actionClick,
            KeyEvent keyEvent
    ) {
        boolean consumed = false, wasIME = false;

        List<AccessibilityWindowInfo> windows;
        try { windows = svc.getWindows(); } catch (Throwable t) { windows = null; }
        if (windows == null) return new ClickResult(false, false);

        outer:
        for (AccessibilityWindowInfo w : windows) {
            AccessibilityNodeInfo root = (w != null) ? w.getRoot() : null;
            if (root == null) continue;

            List<AccessibilityNodeInfo> chain = findHitChain(root, pInt);
            for (int i = chain.size() - 1; i >= 0; i--) {
                AccessibilityNodeInfo n = chain.get(i);
                if (n == null) continue;

                // Focus if possible
                for (AccessibilityNodeInfo.AccessibilityAction a : n.getActionList()) {
                    if (a.getId() == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) {
                        n.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
                        break;
                    }
                }

                if (w.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                    CharSequence pkg = n.getPackageName();
                    if (pkg != null && pkg.toString().equals("com.amazon.tv.ime")
                            && keyEvent != null
                            && keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                        try {
                            InputMethodManager imm =
                                    (InputMethodManager) svc.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                            if (imm != null) imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                        } catch (Throwable ignored) {}
                        consumed = wasIME = true;
                        break outer;
                    } else {
                        consumed = wasIME = n.performAction(actionClick);
                        if (consumed) break outer;
                    }
                }

                if (supportsAction(n, actionClick) && n.isEnabled()) {
                    consumed = n.performAction(actionClick);
                    if (consumed) break outer;
                }

                if ("com.google.android.tvlauncher".contentEquals(n.getPackageName())
                        && supportsAction(n, actionClick)) {
                    if (n.isFocusable()) n.performAction(AccessibilityNodeInfo.FOCUS_INPUT);
                    consumed = n.performAction(actionClick);
                    if (consumed) break outer;
                }
            }
        }

        return new ClickResult(consumed, wasIME);
    }

    private static boolean supportsAction(AccessibilityNodeInfo n, int action) {
        return ((n.getActions() & action) != 0);
    }

    private static List<AccessibilityNodeInfo> findHitChain(AccessibilityNodeInfo root, Point p) {
        ArrayList<AccessibilityNodeInfo> chain = new ArrayList<>();
        if (root != null) findHitChainRec(root, p, chain);
        return chain;
    }

    private static boolean findHitChainRec(AccessibilityNodeInfo node, Point p, List<AccessibilityNodeInfo> out) {
        if (node == null) return false;
        Rect r = new Rect();
        node.getBoundsInScreen(r);
        if (!r.contains(p.x, p.y)) return false;
        for (int i = 0; i < node.getChildCount(); i++) {
            findHitChainRec(node.getChild(i), p, out);
        }
        out.add(node);
        return true;
    }
}
