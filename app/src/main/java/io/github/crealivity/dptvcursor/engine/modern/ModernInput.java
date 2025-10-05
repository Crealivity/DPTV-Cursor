package io.github.crealivity.dptvcursor.engine.modern;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.PointF;

public final class ModernInput {
    private ModernInput() {}

    public static boolean isAvailable() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N;
    }

    public static void click(AccessibilityService s, PointF p, long durationMs) {
        if (!isAvailable() || s == null || p == null) return;
        int dur = clamp((int) durationMs, 1, 2000);
        GestureDescription g = buildTap(p, dur);
        s.dispatchGesture(g, null, null);
    }

    public static void swipe(
            AccessibilityService s,
            PointF origin,
            int dirX, int dirY,
            int distancePx,
            int durationMs,
            Runnable onCompleted,
            Runnable onCancelled
    ) {
        if (!isAvailable() || s == null || origin == null) return;

        int dx = clamp(dirX, -1, 1);
        int dy = clamp(dirY, -1, 1);
        int dist = clamp(distancePx, 1, 10_000);
        int dur  = clamp(durationMs, 16, 2_000);

        PointF dst = new PointF(origin.x + dist * dx, origin.y + dist * dy);
        GestureDescription g = buildSwipe(origin, dst, dur);

        s.dispatchGesture(g, new AccessibilityService.GestureResultCallback() {
            @Override public void onCompleted(GestureDescription gd) {
                if (onCompleted != null) onCompleted.run();
            }
            @Override public void onCancelled(GestureDescription gd) {
                if (onCancelled != null) onCancelled.run();
            }
        }, null);
    }

    public static void swipeWithCallbacks(
            AccessibilityService s,
            PointF origin,
            int dirX, int dirY,
            int distancePx,
            int durationMs,
            Runnable onCompletedOrCancelled
    ) {
        if (!isAvailable() || s == null || origin == null) return;

        int dx = clamp(dirX, -1, 1);
        int dy = clamp(dirY, -1, 1);
        int dist = clamp(distancePx, 1, 10_000);
        int dur  = clamp(durationMs, 16, 2_000);

        PointF dst = new PointF(origin.x + dist * dx, origin.y + dist * dy);
        GestureDescription g = buildSwipe(origin, dst, dur);

        AccessibilityService.GestureResultCallback cb = new AccessibilityService.GestureResultCallback() {
            @Override public void onCompleted(GestureDescription gd) {
                if (onCompletedOrCancelled != null) onCompletedOrCancelled.run();
            }
            @Override public void onCancelled(GestureDescription gd) {
                if (onCompletedOrCancelled != null) onCompletedOrCancelled.run();
            }
        };
        s.dispatchGesture(g, cb, null);
    }

    private static GestureDescription buildTap(PointF p, int durMs) {
        Path path = new Path();
        path.moveTo(p.x, p.y);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, durMs);
        return new GestureDescription.Builder().addStroke(stroke).build();
    }

    private static GestureDescription buildSwipe(PointF a, PointF b, int durMs) {
        Path path = new Path();
        path.moveTo(a.x, a.y);
        path.lineTo(b.x, b.y);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, durMs);
        return new GestureDescription.Builder().addStroke(stroke).build();
    }

    public static void cancelOngoing(AccessibilityService s, PointF p) {
        if (!isAvailable() || s == null || p == null) return;
        GestureDescription g = buildTap(p, 1);
        s.dispatchGesture(g, null, null);
    }

    private static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }
}