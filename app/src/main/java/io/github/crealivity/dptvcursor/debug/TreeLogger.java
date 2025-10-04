package io.github.crealivity.dptvcursor.debug;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class TreeLogger {
    private TreeLogger(){}

    public static boolean DEBUG = false;
    private static final String TAG = "A11Y-TREE";

    public static void dumpAllWindows(AccessibilityService svc, int maxDepth) {
        if (!DEBUG) return;
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                List<AccessibilityWindowInfo> ws = svc.getWindows();
                if (ws == null) { log("no windows"); return; }
                log("windows=" + ws.size());
                int idx = 0;
                for (AccessibilityWindowInfo w : ws) {
                    AccessibilityNodeInfo r = (w != null) ? w.getRoot() : null;
                    if (r == null) continue;
                    Rect rb = new Rect();
                    try { r.getBoundsInScreen(rb); } catch (Throwable ignored) {}
                    log("win[" + idx + "] layer=" + safeLayer(w) + " type=" + safeType(w) + " rootBounds=" + rb);
                    dumpTree(r, maxDepth, "  ");
                    try { r.recycle(); } catch (Throwable ignored) {}
                    idx++;
                }
            } else {
                AccessibilityNodeInfo root = svc.getRootInActiveWindow();
                if (root == null) { log("no active root"); return; }
                log("active root:");
                dumpTree(root, maxDepth, "  ");
                try { root.recycle(); } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            log("dumpAllWindows error: " + t);
        }
    }

    public static void dumpWebViews(AccessibilityService svc, int maxDepth) {
        if (!DEBUG) return;
        try {
            List<AccessibilityNodeInfo> roots = new ArrayList<>();
            if (Build.VERSION.SDK_INT >= 21) {
                List<AccessibilityWindowInfo> ws = svc.getWindows();
                if (ws != null) {
                    for (AccessibilityWindowInfo w : ws) {
                        AccessibilityNodeInfo r = (w != null) ? w.getRoot() : null;
                        if (r != null) roots.add(r);
                    }
                }
            } else {
                AccessibilityNodeInfo r = svc.getRootInActiveWindow();
                if (r != null) roots.add(r);
            }
            if (roots.isEmpty()) { log("dumpWebViews: no roots"); return; }

            for (AccessibilityNodeInfo root : roots) {
                ArrayDeque<AccessibilityNodeInfo> q = new ArrayDeque<>();
                q.add(root);
                while (!q.isEmpty()) {
                    AccessibilityNodeInfo n = q.removeFirst();
                    if (n == null) continue;
                    CharSequence cls = n.getClassName();
                    if (cls != null && containsIgnoreCase(cls.toString(), "webview")) {
                        Rect r = new Rect();
                        try { n.getBoundsInScreen(r); } catch (Throwable ignored) {}
                        log("WEBVIEW node pkg=" + n.getPackageName() + " cls=" + cls + " bounds=" + r);
                        dumpNodeActions(n, "  ");
                        dumpSubtree(n, Math.max(0, maxDepth-1), "    ");
                    }
                    final int cc = n.getChildCount();
                    for (int i = 0; i < cc; i++) {
                        AccessibilityNodeInfo c = null;
                        try { c = n.getChild(i); } catch (Throwable ignored) {}
                        if (c != null) q.add(c);
                    }
                }
            }
            for (AccessibilityNodeInfo r : roots) {
                try { r.recycle(); } catch (Throwable ignored) {}
            }
        } catch (Throwable t) {
            log("dumpWebViews error: " + t);
        }
    }

    public static void dumpHitChainUnderPoint(AccessibilityService svc, Point p, int maxDepthEach) {
        if (!DEBUG) return;
        try {
            AccessibilityNodeInfo root = svc.getRootInActiveWindow();
            if (root == null) { log("no active root"); return; }
            List<AccessibilityNodeInfo> chain = new ArrayList<>();
            findHitChainRec(root, p, chain);
            log("hitChain size=" + chain.size() + " @ " + p);
            for (int i = 0; i < chain.size(); i++) {
                AccessibilityNodeInfo n = chain.get(i);
                log("  [" + i + "]: " + nodeSummary(n));
                dumpNodeActions(n, "    ");
                if (maxDepthEach > 0) dumpSubtree(n, maxDepthEach, "      ");
            }
            try { root.recycle(); } catch (Throwable ignored) {}
        } catch (Throwable t) {
            log("dumpHitChainUnderPoint error: " + t);
        }
    }

    // ---- helpers ----
    private static void dumpTree(AccessibilityNodeInfo n, int depth, String pad) {
        if (n == null || depth < 0) return;
        log(pad + nodeSummary(n));
        dumpNodeActions(n, pad + "  ");
        final int cc = n.getChildCount();
        for (int i = 0; i < cc; i++) {
            AccessibilityNodeInfo c = null;
            try { c = n.getChild(i); } catch (Throwable ignored) {}
            if (c != null) dumpTree(c, depth - 1, pad + "  ");
        }
    }

    private static void dumpSubtree(AccessibilityNodeInfo n, int depth, String pad) {
        if (n == null || depth <= 0) return;
        final int cc = n.getChildCount();
        for (int i = 0; i < cc; i++) {
            AccessibilityNodeInfo c = null;
            try { c = n.getChild(i); } catch (Throwable ignored) {}
            if (c != null) {
                log(pad + nodeSummary(c));
                dumpNodeActions(c, pad + "  ");
                dumpSubtree(c, depth - 1, pad + "  ");
            }
        }
    }

    private static void dumpNodeActions(AccessibilityNodeInfo n, String pad) {
        if (n == null) return;
        StringBuilder sb = new StringBuilder();
        try {
            if (Build.VERSION.SDK_INT >= 21) {
                List<AccessibilityNodeInfo.AccessibilityAction> acts = n.getActionList();
                if (acts != null) {
                    for (AccessibilityNodeInfo.AccessibilityAction a : acts) {
                        sb.append(a.getId()).append(":").append(a.getLabel()).append(", ");
                    }
                }
            } else {
                int mask = n.getActions();
                int[] known = {
                        AccessibilityNodeInfo.ACTION_CLICK,
                        AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
                        AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,
                        AccessibilityNodeInfo.ACTION_FOCUS,
                        AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
                        AccessibilityNodeInfo.ACTION_SELECT,
                        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY,
                        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY
                };
                for (int k : known) {
                    if ( (mask & k) != 0 ) sb.append(k).append(", ");
                }
            }
        } catch (Throwable ignored) {
            // do not let logging break input paths
        }
        log(pad + "actions=[" + sb + "] scrollable=" + n.isScrollable() + " clickable=" + n.isClickable());
    }

    private static boolean findHitChainRec(AccessibilityNodeInfo node, Point p, List<AccessibilityNodeInfo> out) {
        if (node == null) return false;
        Rect r = new Rect();
        try { node.getBoundsInScreen(r); } catch (Throwable ignored) {}
        r.inset(-8, -8);
        if (!r.contains(p.x, p.y)) return false;
        final int cc = node.getChildCount();
        boolean any = false;
        for (int i = 0; i < cc; i++) {
            AccessibilityNodeInfo c = null;
            try { c = node.getChild(i); } catch (Throwable ignored) {}
            if (c != null && findHitChainRec(c, p, out)) any = true;
        }
        out.add(node);
        return true;
    }

    private static String nodeSummary(AccessibilityNodeInfo n) {
        Rect r = new Rect();
        try { n.getBoundsInScreen(r); } catch (Throwable ignored) {}
        return "pkg=" + n.getPackageName()
                + " cls=" + n.getClassName()
                + " text=" + n.getText()
                + " desc=" + n.getContentDescription()
                + " enabled=" + n.isEnabled()
                + " focusable=" + n.isFocusable()
                + " focused=" + n.isFocused()
                + " scrollable=" + n.isScrollable()
                + " clickable=" + n.isClickable()
                + " bounds=" + r;
    }

    private static String safeType(AccessibilityWindowInfo w) {
        try { return String.valueOf(w.getType()); } catch (Throwable t) { return "NA"; }
    }
    private static String safeLayer(AccessibilityWindowInfo w) {
        try { return String.valueOf(w.getLayer()); } catch (Throwable t) { return "NA"; }
    }
    private static boolean containsIgnoreCase(String s, String needle) {
        return s != null && s.toLowerCase().contains(needle.toLowerCase());
    }
    private static void log(String s){ android.util.Log.d(TAG, s); }
}