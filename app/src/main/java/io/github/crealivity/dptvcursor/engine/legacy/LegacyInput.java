package io.github.crealivity.dptvcursor.engine.legacy;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public final class LegacyInput {
    private LegacyInput() {}

    public static boolean clickAtPointer(AccessibilityService s, io.github.crealivity.dptvcursor.engine.impl.PointerControl pc) {
        return (android.os.Build.VERSION.SDK_INT >= 21)
                ? LegacyInput21.clickAtPointer(s, pc)
                : LegacyInput19.clickAtPointer(s, pc);
    }

    public static boolean scrollOnce(AccessibilityService s, io.github.crealivity.dptvcursor.engine.impl.PointerControl pc, int direction) {
        return (android.os.Build.VERSION.SDK_INT >= 21)
                ? LegacyInput21.scrollOnce(s, pc, direction)
                : LegacyInput19.scrollOnce(s, pc, direction);
    }

    static AccessibilityNodeInfo safeGetActiveRoot(AccessibilityService svc) {
        try { return svc.getRootInActiveWindow(); } catch (Throwable t) { return null; }
    }

    static boolean supportsAction(AccessibilityNodeInfo n, int action) {
        return n != null && ((n.getActions() & action) != 0);
    }

    static boolean supportsClick(AccessibilityNodeInfo n) {
        return n != null && (n.isClickable() || supportsAction(n, AccessibilityNodeInfo.ACTION_CLICK));
    }

    static boolean containsPoint(AccessibilityNodeInfo n, Point p) {
        if (n == null || p == null) return false;
        try {
            Rect r = new Rect();
            n.getBoundsInScreen(r);
            r.inset(-6, -6);
            return r.contains(p.x, p.y);
        } catch (Throwable ignored) { return false; }
    }

    static boolean refreshIfPossible(AccessibilityNodeInfo n) {
        try {
            if (n == null) return false;
            if (android.os.Build.VERSION.SDK_INT < 21) return false;
            return n.refresh();
        } catch (Throwable ignored) {
            return false;
        }
    }

    static List<AccessibilityNodeInfo> hitChain(AccessibilityNodeInfo root, Point p) {
        ArrayList<AccessibilityNodeInfo> out = new ArrayList<>();
        if (root != null) hitChainRec(root, p, out);
        return out;
    }

    private static boolean hitChainRec(AccessibilityNodeInfo node, Point p, List<AccessibilityNodeInfo> out) {
        if (node == null) return false;
        Rect r = new Rect();
        try { node.getBoundsInScreen(r); } catch (Throwable ignored) { return false; }
        r.inset(-6, -6);
        if (!r.contains(p.x, p.y)) return false;

        final int cc = node.getChildCount();
        boolean anyChild = false;
        for (int i = 0; i < cc; i++) {
            AccessibilityNodeInfo c = null;
            try { c = node.getChild(i); } catch (Throwable ignored) {}
            if (c != null && hitChainRec(c, p, out)) anyChild = true;
        }
        out.add(node);
        return true;
    }

    static boolean tryFocusSelectClick(AccessibilityNodeInfo n) {
        if (n == null) return false;
        try {
            n.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            if (n.isFocusable() && !n.isFocused()) n.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            if (supportsAction(n, AccessibilityNodeInfo.ACTION_SELECT)) n.performAction(AccessibilityNodeInfo.ACTION_SELECT);
            return supportsClick(n) && n.isEnabled() && n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } catch (Throwable ignored) { return false; }
    }

    static boolean bubbleClickUp(AccessibilityNodeInfo leaf) {
        AccessibilityNodeInfo cur = leaf;
        for (int i = 0; i < 8 && cur != null; i++) {
            refreshIfPossible(cur);
            if (tryFocusSelectClick(cur)) return true;
            AccessibilityNodeInfo parent = null;
            try { parent = cur.getParent(); } catch (Throwable ignored) {}
            if (cur != leaf) { try { cur.recycle(); } catch (Throwable ignored) {} }
            cur = parent;
        }
        return false;
    }

    static AccessibilityNodeInfo firstScrollableUpChain(List<AccessibilityNodeInfo> chain, int action) {
        for (int i = chain.size() - 1; i >= 0; i--) {
            AccessibilityNodeInfo n = chain.get(i);
            if (n == null) continue;
            refreshIfPossible(n);
            if (n.isScrollable() || supportsAction(n, action)) return n;
        }
        return null;
    }

    static boolean focusThenAction(AccessibilityNodeInfo n, int action) {
        if (n == null) return false;
        try {
            n.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            if (n.isFocusable() && !n.isFocused()) n.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            return n.performAction(action);
        } catch (Throwable ignored) { return false; }
    }

    static int oppositeScroll(int action) {
        return (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                ? AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                : (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                : action;
    }

    static AccessibilityNodeInfo firstScrollableBfs(AccessibilityNodeInfo root, int action) {
        if (root == null) return null;
        java.util.ArrayDeque<AccessibilityNodeInfo> q = new java.util.ArrayDeque<>();
        q.addLast(root);
        while (!q.isEmpty()) {
            AccessibilityNodeInfo n = q.removeFirst();
            if (n == null) continue;
            if (n.isScrollable() || supportsAction(n, action)) return n;
            final int cc = n.getChildCount();
            for (int i = 0; i < cc; i++) {
                AccessibilityNodeInfo c = null;
                try { c = n.getChild(i); } catch (Throwable ignored) {}
                if (c != null) q.addLast(c);
            }
        }
        return null;
    }

    static void recycleAll(List<AccessibilityNodeInfo> list) {
        for (AccessibilityNodeInfo n : list) try { if (n != null) n.recycle(); } catch (Throwable ignored) {}
    }

    public static int suggestedDelayMs(int speed) {
        int s = clamp(speed, 1, 1000);
        int min = 28, max = 200;
        return max - ((max - min) * s) / 1000;
    }

    static int clamp(int v, int min, int max) {
        return (v < min) ? min : (v > max) ? max : v;
    }
}
