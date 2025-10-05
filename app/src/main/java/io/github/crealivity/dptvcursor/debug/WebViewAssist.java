package io.github.crealivity.dptvcursor.debug;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.ArrayList;
import java.util.List;

public final class WebViewAssist {
    private WebViewAssist(){}

    public static boolean DEBUG = true;
    private static final String TAG = "WEB-ASSIST";

    public static boolean tryScrollWebFallback(AccessibilityService svc, Point p, boolean forward) {
        int primary = forward
                ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;

        AccessibilityNodeInfo web = findNearestWebNodeUnderPoint(svc, p);
        if (web == null) { log("no web node under point"); return false; }

        // 1) Normal scroll
        if (performScroll(web, primary)) return true;
        if (performScroll(web, opposite(primary))) return true;

        // 2) Try vertical-specific actions if present (some OEM webviews expose UP/DOWN)
        if (performScroll(web, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) return true;
        if (performScroll(web, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true;

        // 3) Movement granularity PAGE (may trigger visual scroll as focus moves)
        if (moveByGranularity(web, forward, /*granularity*/ 4 /*PAGE*/)) return true;
        // fallback try LINE or PARAGRAPH if PAGE failed (2=WORD, 3=LINE varies by impl)
        if (moveByGranularity(web, forward, 3)) return true;

        // 4) HTML element navigation (21+)
        if (Build.VERSION.SDK_INT >= 21) {
            String[] elems = {"SECTION","ARTICLE","P","DIV","H1","H2","H3","LI","A"};
            for (String el : elems) {
                if (moveByHtmlElement(web, forward, el)) return true;
            }
        }

        return false;
    }

    public static boolean tryClickWebFallback(AccessibilityService svc, Point p) {
        AccessibilityNodeInfo webLeaf = findDeepestWebNodeUnderPoint(svc, p);
        if (webLeaf == null) { log("no web leaf under point"); return false; }

        // Focus, maybe select, then click if clickable;
        // if not clickable, bubble to parents but keep pointer guard via contains().
        if (focus(webLeaf)) { /* ok */ }
        if (webLeaf.isClickable() && webLeaf.isEnabled()) {
            logNode("click leaf", webLeaf, p);
            if (webLeaf.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
        }

        AccessibilityNodeInfo cur = webLeaf;
        for (int i = 0; i < 8 && cur != null; i++) {
            AccessibilityNodeInfo parent = null;
            try { parent = cur.getParent(); } catch (Throwable ignored) {}
            if (cur != webLeaf) try { cur.recycle(); } catch (Throwable ignored) {}
            cur = parent;
            if (cur == null) break;
            if (!contains(cur, p)) continue;
            if (focus(cur)) { /* ok */ }
            if (cur.isClickable() && cur.isEnabled()) {
                logNode("click bubbled", cur, p);
                if (cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            }
        }
        return false;
    }

    private static AccessibilityNodeInfo findNearestWebNodeUnderPoint(AccessibilityService svc, Point p) {
        List<AccessibilityNodeInfo> roots = getRoots(svc);
        AccessibilityNodeInfo best = null;
        int bestArea = Integer.MAX_VALUE;

        for (AccessibilityNodeInfo root : roots) {
            AccessibilityNodeInfo n = findWebHit(root, p);
            if (n != null) {
                Rect r = new Rect();
                try { n.getBoundsInScreen(r); } catch (Throwable ignored) {}
                int area = Math.max(1, r.width()*r.height());
                if (area < bestArea) { bestArea = area; best = n; }
            }
        }
        for (AccessibilityNodeInfo r : roots) { try { r.recycle(); } catch (Throwable ignored) {} }
        return best;
    }

    private static AccessibilityNodeInfo findDeepestWebNodeUnderPoint(AccessibilityService svc, Point p) {
        List<AccessibilityNodeInfo> roots = getRoots(svc);
        AccessibilityNodeInfo best = null;
        int bestDepth = -1;

        for (AccessibilityNodeInfo root : roots) {
            AccessibilityNodeInfo n = findDeepWebHit(root, p, 0);
            if (n != null) {
                int d = (Integer) n.getExtras().get("DEPTH_HINT");
                if (d > bestDepth) { bestDepth = d; best = n; }
            }
        }
        for (AccessibilityNodeInfo r : roots) { try { r.recycle(); } catch (Throwable ignored) {} }
        return best;
    }

    private static AccessibilityNodeInfo findWebHit(AccessibilityNodeInfo node, Point p) {
        if (node == null) return null;
        if (!contains(node, p)) return null;
        if (isWeb(node)) return node;

        final int cc = node.getChildCount();
        for (int i = 0; i < cc; i++) {
            AccessibilityNodeInfo c = null;
            try { c = node.getChild(i); } catch (Throwable ignored) {}
            AccessibilityNodeInfo hit = findWebHit(c, p);
            if (hit != null) return hit;
        }
        return node; // fallback to container at least under point
    }

    private static AccessibilityNodeInfo findDeepWebHit(AccessibilityNodeInfo node, Point p, int depth) {
        if (node == null) return null;
        if (!contains(node, p)) return null;
        AccessibilityNodeInfo best = null;
        final int cc = node.getChildCount();
        for (int i = 0; i < cc; i++) {
            AccessibilityNodeInfo c = null;
            try { c = node.getChild(i); } catch (Throwable ignored) {}
            AccessibilityNodeInfo hit = findDeepWebHit(c, p, depth+1);
            if (hit != null) {
                best = hit;
            }
        }
        if (best != null) return best;
        if (isWeb(node) || node.isClickable()) {
            node.getExtras().putInt("DEPTH_HINT", depth);
            return node;
        }
        node.getExtras().putInt("DEPTH_HINT", depth);
        return node;
    }

    private static boolean performScroll(AccessibilityNodeInfo n, int action) {
        if (n == null) return false;
        if (!hasAction(n, action)) return false;
        logNode("scroll try action=" + action, n, null);
        return n.performAction(action);
    }

    private static boolean moveByGranularity(AccessibilityNodeInfo n, boolean forward, int granularity) {
        if (n == null) return false;
        if (!hasAction(n, AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY)
                && !hasAction(n, AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY)) {
            return false;
        }
        Bundle b = new Bundle();
        b.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT, granularity);
        b.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN, false);
        int act = forward ? AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY
                : AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY;
        logNode("granularity move act=" + act + " gran=" + granularity, n, null);
        return n.performAction(act, b);
    }

    private static boolean moveByHtmlElement(AccessibilityNodeInfo n, boolean forward, String elem) {
        if (Build.VERSION.SDK_INT < 21) return false;
        int act = forward ? AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT
                : AccessibilityNodeInfo.ACTION_PREVIOUS_HTML_ELEMENT;
        if (!hasAction(n, act)) return false;
        Bundle b = new Bundle();
        b.putString(AccessibilityNodeInfo.ACTION_ARGUMENT_HTML_ELEMENT_STRING, elem);
        logNode("html move act=" + act + " el=" + elem, n, null);
        return n.performAction(act, b);
    }

    private static boolean focus(AccessibilityNodeInfo n) {
        boolean any = false;
        try {
            any |= n.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
            if (n.isFocusable() && !n.isFocused()) any |= n.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        } catch (Throwable ignored) {}
        return any;
    }

    private static List<AccessibilityNodeInfo> getRoots(AccessibilityService svc) {
        List<AccessibilityNodeInfo> roots = new ArrayList<>();
        try {
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
        } catch (Throwable ignored) {}
        return roots;
    }

    private static boolean isWeb(AccessibilityNodeInfo n) {
        CharSequence cls = n.getClassName();
        if (cls == null) return false;
        String s = cls.toString().toLowerCase();
        return s.contains("webview") || s.contains("chromium") || s.contains("webkit");
    }

    private static boolean hasAction(AccessibilityNodeInfo n, int id) {
        try {
            List<AccessibilityNodeInfo.AccessibilityAction> list = n.getActionList();
            if (list == null) return ( (n.getActions() & id) != 0 );
            for (AccessibilityNodeInfo.AccessibilityAction a : list) {
                if (a.getId() == id) return true;
            }
            return ((n.getActions() & id) != 0);
        } catch (Throwable ignored) { return false; }
    }

    private static boolean contains(AccessibilityNodeInfo n, Point p) {
        if (n == null || p == null) return false;
        Rect r = new Rect();
        try { n.getBoundsInScreen(r); } catch (Throwable ignored) {}
        r.inset(-8, -8);
        return r.contains(p.x, p.y);
    }

    private static int opposite(int act) {
        return (act == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                ? AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                : (act == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                : act;
    }

    private static void log(String s){ if (DEBUG) android.util.Log.d(TAG, s); }
    private static void logNode(String prefix, AccessibilityNodeInfo n, Point p){
        if (!DEBUG || n == null) return;
        Rect r = new Rect();
        try { n.getBoundsInScreen(r); } catch (Throwable ignored) {}
        android.util.Log.d(TAG, prefix + " pkg=" + n.getPackageName()
                + " cls=" + n.getClassName()
                + " click=" + n.isClickable()
                + " scroll=" + n.isScrollable()
                + " bounds=" + r
                + (p!=null ? " contains=" + r.contains(p.x,p.y) : ""));
    }
}
