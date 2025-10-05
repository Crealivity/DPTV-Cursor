package io.github.crealivity.dptvcursor.engine.legacy;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Point;
import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

import io.github.crealivity.dptvcursor.debug.TreeLogger;
import io.github.crealivity.dptvcursor.debug.WebViewAssist;
import io.github.crealivity.dptvcursor.engine.impl.PointerControl;

public final class LegacyInput19 {
    private LegacyInput19() {}
    public static boolean isActive() { return Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 21; }

    public static boolean clickAtPointer(AccessibilityService svc, PointerControl pc) {
        AccessibilityNodeInfo root = LegacyInput.safeGetActiveRoot(svc);
        if (root == null) return false;

        Point p = new Point((int) pc.getPointerLocation().x, (int) pc.getPointerLocation().y);
        List<AccessibilityNodeInfo> chain = LegacyInput.hitChain(root, p);

        if (TreeLogger.DEBUG) {
            TreeLogger.dumpHitChainUnderPoint(svc, p, /*maxDepthEach=*/0);
            TreeLogger.dumpWebViews(svc, /*maxDepth=*/2);
        }

        boolean ok = false;
        if (!chain.isEmpty()) ok = LegacyInput.bubbleClickUp(chain.get(0));

        if (!ok) {
            AccessibilityNodeInfo focus = null;
            try { focus = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY); } catch (Throwable ignored) {}
            if (focus != null) {
                if (LegacyInput.containsPoint(focus, p)) ok = LegacyInput.tryFocusSelectClick(focus);
                try { focus.recycle(); } catch (Throwable ignored) {}
            }
        }

        LegacyInput.recycleAll(chain);
        try { root.recycle(); } catch (Throwable ignored) {}
        return ok;
    }

    public static boolean scrollOnce(AccessibilityService svc, PointerControl pc, int direction) {
        final int primary = (direction == PointerControl.UP)
                ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                : (direction == PointerControl.DOWN)
                ? AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD : -1;
        if (primary == -1) return false;

        AccessibilityNodeInfo root = LegacyInput.safeGetActiveRoot(svc);
        if (root == null) return false;

        Point p = new Point((int) pc.getPointerLocation().x, (int) pc.getPointerLocation().y);
        List<AccessibilityNodeInfo> chain = LegacyInput.hitChain(root, p);

        boolean ok = false;

        AccessibilityNodeInfo target = LegacyInput.firstScrollableUpChain(chain, primary);
        if (target != null) {
            ok = LegacyInput.focusThenAction(target, primary);
            if (!ok) ok = LegacyInput.focusThenAction(target, LegacyInput.oppositeScroll(primary));
        }

        if (!ok) {
            AccessibilityNodeInfo sc = LegacyInput.firstScrollableBfs(root, primary);
            if (sc != null) {
                ok = LegacyInput.focusThenAction(sc, primary);
                if (!ok) ok = LegacyInput.focusThenAction(sc, LegacyInput.oppositeScroll(primary));
            }
            boolean webOk = WebViewAssist.tryScrollWebFallback(svc, p, direction == PointerControl.UP);
            if (webOk) {
                LegacyInput.recycleAll(chain);
                try { root.recycle(); } catch (Throwable ignored) {}
                return true;
            }

        }

        LegacyInput.recycleAll(chain);
        try { root.recycle(); } catch (Throwable ignored) {}
        return ok;
    }

    public static int suggestedDelayMs(int speed) { return LegacyInput.suggestedDelayMs(speed); }
}
