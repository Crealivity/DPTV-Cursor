package io.github.crealivity.dptvcursor.engine.legacy;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.crealivity.dptvcursor.debug.TreeLogger;
import io.github.crealivity.dptvcursor.debug.WebViewAssist;
import io.github.crealivity.dptvcursor.engine.impl.PointerControl;

public final class LegacyInput21 {
    private LegacyInput21() {}
    public static boolean isActive() { return Build.VERSION.SDK_INT >= 21 && Build.VERSION.SDK_INT < 24; }

    public static boolean clickAtPointer(AccessibilityService svc, PointerControl pc) {
        Point p = new Point((int) pc.getPointerLocation().x, (int) pc.getPointerLocation().y);
        List<AccessibilityNodeInfo> chain = hitChainTopWindow(svc, p);

        if (TreeLogger.DEBUG) {
            TreeLogger.dumpHitChainUnderPoint(svc, p, /*maxDepthEach=*/0);
            TreeLogger.dumpWebViews(svc, /*maxDepth=*/2);
        }

        boolean ok = false;
        if (!chain.isEmpty()) ok = LegacyInput.bubbleClickUp(chain.get(0));

        if (!ok) {
            AccessibilityNodeInfo root = LegacyInput.safeGetActiveRoot(svc);
            if (root != null) {
                AccessibilityNodeInfo focus = null;
                try { focus = root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY); } catch (Throwable ignored) {}
                if (focus != null) {
                    if (LegacyInput.containsPoint(focus, p)) ok = LegacyInput.tryFocusSelectClick(focus);
                    try { focus.recycle(); } catch (Throwable ignored) {}
                }
            }

            if (WebViewAssist.tryClickWebFallback(svc, p)) {
                LegacyInput.recycleAll(chain);
                return true;
            }
        }

        LegacyInput.recycleAll(chain);
        return ok;
    }

    public static boolean scrollOnce(AccessibilityService svc, PointerControl pc, int direction) {
        final int primary = (direction == PointerControl.UP)
                ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                : (direction == PointerControl.DOWN)
                ? AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD : -1;
        if (primary == -1) return false;

        boolean ok = false;

        Point p = new Point((int) pc.getPointerLocation().x, (int) pc.getPointerLocation().y);

        List<AccessibilityNodeInfo> chain = hitChainTopWindow(svc, p);
        if (!chain.isEmpty()) {
            AccessibilityNodeInfo t = LegacyInput.firstScrollableUpChain(chain, primary);
            if (t != null) {
                ok = LegacyInput.focusThenAction(t, primary)
                        || LegacyInput.focusThenAction(t, LegacyInput.oppositeScroll(primary));
                LegacyInput.recycleAll(chain);
                if (ok) return true;
            }
            LegacyInput.recycleAll(chain);
        }

        AccessibilityNodeInfo root = LegacyInput.safeGetActiveRoot(svc);
        if (root != null) {
            AccessibilityNodeInfo sc = LegacyInput.firstScrollableBfs(root, primary);
            ok = (sc != null) && (LegacyInput.focusThenAction(sc, primary)
                    || LegacyInput.focusThenAction(sc, LegacyInput.oppositeScroll(primary)));
            try { root.recycle(); } catch (Throwable ignored) {}
            if (ok) return true;
        }

        for (AccessibilityWindowInfo w : safeGetWindows(svc)) {
            AccessibilityNodeInfo r = (w != null) ? w.getRoot() : null;
            if (r == null) continue;
            AccessibilityNodeInfo sc = LegacyInput.firstScrollableBfs(r, primary);
            ok = (sc != null) && (LegacyInput.focusThenAction(sc, primary)
                    || LegacyInput.focusThenAction(sc, LegacyInput.oppositeScroll(primary)));
            try { r.recycle(); } catch (Throwable ignored) {}
            if (ok) return true;
        }

        // had to add it bc it was missing :(
        if (!ok) {
            // web-specific fallback: direction==UP means "forward" in our mapping
            boolean webOk = WebViewAssist.tryScrollWebFallback(svc, p, direction == PointerControl.UP);
            if (webOk) return true;
        }

        return ok; // returns false why? try return ok? done
    }


    private static List<AccessibilityNodeInfo> hitChainTopWindow(AccessibilityService svc, Point p) {
        ArrayList<AccessibilityNodeInfo> empty = new ArrayList<>();
        List<AccessibilityWindowInfo> ws;
        try { ws = svc.getWindows(); } catch (Throwable t) { ws = null; }
        if (ws == null || ws.isEmpty()) return empty;

        try {
            Collections.sort(ws, (a, b) -> Integer.compare(b.getLayer(), a.getLayer()));
        } catch (Throwable ignored) {}

        for (AccessibilityWindowInfo w : ws) {
            AccessibilityNodeInfo root = (w != null) ? w.getRoot() : null;
            if (root == null) continue;
            LegacyInput.refreshIfPossible(root);
            ArrayList<AccessibilityNodeInfo> chain = new ArrayList<>();
            if (hitChainRec(root, p, chain)) return chain;
            try { root.recycle(); } catch (Throwable ignored) {}
        }
        return empty;
    }

    private static boolean hitChainRec(AccessibilityNodeInfo node, Point p, List<AccessibilityNodeInfo> out) {
        if (node == null) return false;
        Rect r = new Rect();
        try { node.getBoundsInScreen(r); } catch (Throwable ignored) { return false; }
        r.inset(-6, -6);
        if (!r.contains(p.x, p.y)) return false;

        final int cc = node.getChildCount();
        boolean any = false;
        for (int i = 0; i < cc; i++) {
            AccessibilityNodeInfo c = null;
            try { c = node.getChild(i); } catch (Throwable ignored) {}
            if (c != null && hitChainRec(c, p, out)) any = true;
        }
        out.add(node);
        return true;
    }

    private static List<AccessibilityWindowInfo> safeGetWindows(AccessibilityService svc) {
        List<AccessibilityWindowInfo> out = new ArrayList<>();
        try {
            List<AccessibilityWindowInfo> ws = svc.getWindows();
            if (ws != null) out.addAll(ws);
        } catch (Throwable ignored) {}
        return out;
    }

    private static AccessibilityWindowInfo topWindowAtPoint(AccessibilityService svc, Point p) {
        List<AccessibilityWindowInfo> ws = safeGetWindows(svc);
        try { java.util.Collections.sort(ws, (a,b) -> Integer.compare(b.getLayer(), a.getLayer())); } catch (Throwable ignored) {}

        for (AccessibilityWindowInfo w : ws) {
            if (w == null) continue;
            try {
                android.graphics.Rect wb = new android.graphics.Rect();
                w.getBoundsInScreen(wb);
                wb.inset(-6, -6);
                if (wb.contains(p.x, p.y)) return w;
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static List<AccessibilityNodeInfo> hitChainAnyWindow(AccessibilityService svc, Point p) {
        AccessibilityWindowInfo w = topWindowAtPoint(svc, p);
        if (w == null) return new ArrayList<>();

        AccessibilityNodeInfo root = null;
        try { root = w.getRoot(); } catch (Throwable ignored) {}
        if (root == null) return new ArrayList<>();

        ArrayList<AccessibilityNodeInfo> out = new ArrayList<>();
        findNodeRec(root, p, out);

        return out;
    }

    private static boolean findNodeRec(AccessibilityNodeInfo node, Point p, List<AccessibilityNodeInfo> out) {
        if (node == null) return false;
        Rect r = new Rect();
        node.getBoundsInScreen(r);
        r.inset(-6, -6);
        if (!r.contains(p.x, p.y)) return false;

        boolean anyChild = false;
        final int cc = node.getChildCount();
        for (int i = 0; i < cc; i++) {
            AccessibilityNodeInfo c = null;
            try { c = node.getChild(i); } catch (Throwable ignored) {}
            if (c != null && findNodeRec(c, p, out)) anyChild = true;
        }
        out.add(node);
        return true;
    }

    public static int suggestedDelayMs(int speed) { return LegacyInput.suggestedDelayMs(speed); }
}
