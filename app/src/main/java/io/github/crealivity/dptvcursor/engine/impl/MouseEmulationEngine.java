package io.github.crealivity.dptvcursor.engine.impl;

import static io.github.crealivity.dptvcursor.helper.Helper.helperContext;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
//import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.crealivity.dptvcursor.engine.modern.ModernInput;
import io.github.crealivity.dptvcursor.engine.modern.Windows21;
import io.github.crealivity.dptvcursor.engine.legacy.LegacyInput;
import io.github.crealivity.dptvcursor.helper.Helper;
import io.github.crealivity.dptvcursor.view.MouseCursorView;
import io.github.crealivity.dptvcursor.view.OverlayView;

public class MouseEmulationEngine {

    private static boolean DPAD_SELECT_PRESSED = false;
    private static String LOG_TAG = "MOUSE_EMULATION";
    CountDownTimer waitToChange;
    CountDownTimer disappearTimer;
    private boolean isInScrollMode = false;
    // service which started this engine
    private AccessibilityService mService;
    private final PointerControl mPointerControl;
    public static int stuckAtSide = 0;
    private int momentumStack;
    private boolean isEnabled;
    public static int bossKey;
    public static int scrollSpeed;
    public static boolean disableInertia;
    private boolean scrollHeld = false;
    private boolean scrollBusy = false;
    private int lastScrollDir = -999;
    private static final int MOMENTUM_CAP = 26; //16;
    private static final int SOFT_MOMENTUM_CAP = 16; // 6;
    public static int pointerSpeed;
    private boolean hideInLauncher = false;   // when true, keep the cursor hidden
    private Boolean wasEnabledBeforeLauncher = null; // null = unknown, true/false recorded state
    private boolean prevInScrollMode = false;
    public static boolean isBossKeyDisabled;
    public static boolean isBossKeySetToToggle;
    private Handler timerHandler;
    private Point DPAD_Center_Init_Point = new Point();
    private Runnable previousRunnable;
    private static final long IDLE_TIMEOUT_MS = 60_000L; // 60sa
    private final Handler idleHandler = new Handler(Looper.getMainLooper());
    private int touchSlopPx = -1;
    private final OverlayView mOverlayRoot;

    public static boolean isOkKey(int code) {
        return code == KeyEvent.KEYCODE_DPAD_CENTER
                || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER
                || code == KeyEvent.KEYCODE_BUTTON_A ; // optional for gamepads
                //|| code == KeyEvent.KEYCODE_S;         // keyboard 'S' (47) used in emulators :S
    }

    private static final Map<Integer, Integer> scrollCodeMap;
    static {
        Map<Integer, Integer> integerMap = new HashMap<>();
        integerMap.put(KeyEvent.KEYCODE_DPAD_UP, PointerControl.DOWN);
        integerMap.put(KeyEvent.KEYCODE_DPAD_DOWN, PointerControl.UP);
        integerMap.put(KeyEvent.KEYCODE_DPAD_LEFT, PointerControl.RIGHT);
        integerMap.put(KeyEvent.KEYCODE_DPAD_RIGHT, PointerControl.LEFT);
        integerMap.put(KeyEvent.KEYCODE_PROG_GREEN, PointerControl.DOWN);
        integerMap.put(KeyEvent.KEYCODE_PROG_RED, PointerControl.UP);
        integerMap.put(KeyEvent.KEYCODE_PROG_BLUE, PointerControl.RIGHT);
        integerMap.put(KeyEvent.KEYCODE_PROG_YELLOW, PointerControl.LEFT);
        scrollCodeMap = Collections.unmodifiableMap(integerMap);
    }

    private static final Map<Integer, Integer> movementCodeMap;
    static {
        Map<Integer, Integer> integerMap = new HashMap<>();
        integerMap.put(KeyEvent.KEYCODE_DPAD_UP, PointerControl.UP);
        integerMap.put(KeyEvent.KEYCODE_DPAD_DOWN, PointerControl.DOWN);
        integerMap.put(KeyEvent.KEYCODE_DPAD_LEFT, PointerControl.LEFT);
        integerMap.put(KeyEvent.KEYCODE_DPAD_RIGHT, PointerControl.RIGHT);
        movementCodeMap = Collections.unmodifiableMap(integerMap);
    }

    private static final Set<Integer> actionableKeyMap;
    static {
        Set<Integer> integerSet = new HashSet<>();
        integerSet.add(KeyEvent.KEYCODE_DPAD_UP);
        integerSet.add(KeyEvent.KEYCODE_DPAD_DOWN);
        integerSet.add(KeyEvent.KEYCODE_DPAD_LEFT);
        integerSet.add(KeyEvent.KEYCODE_DPAD_RIGHT);
        integerSet.add(KeyEvent.KEYCODE_PROG_GREEN);
        integerSet.add(KeyEvent.KEYCODE_PROG_YELLOW);
        integerSet.add(KeyEvent.KEYCODE_PROG_BLUE);
        integerSet.add(KeyEvent.KEYCODE_PROG_RED);
        actionableKeyMap = Collections.unmodifiableSet(integerSet);
    }

    private static final Set<Integer> colorSet;
    static {
        Set<Integer> integerSet = new HashSet<>();
        integerSet.add(KeyEvent.KEYCODE_PROG_GREEN);
        integerSet.add(KeyEvent.KEYCODE_PROG_YELLOW);
        integerSet.add(KeyEvent.KEYCODE_PROG_BLUE);
        integerSet.add(KeyEvent.KEYCODE_PROG_RED);
        colorSet = Collections.unmodifiableSet(integerSet);
    }

    public MouseEmulationEngine (Context c, OverlayView ov) {
        momentumStack = 0;
        MouseCursorView mCursorView = new MouseCursorView(c);
        ov.addFullScreenLayer(mCursorView);
        mPointerControl = new PointerControl(ov, mCursorView);
        mPointerControl.disappear();
        mOverlayRoot = ov;
        Log.i(LOG_TAG, "X, Y: " + mPointerControl.getPointerLocation().x + ", " + mPointerControl.getPointerLocation().y);
    }

    private PointF pointerToScreen(PointF pLocal) {
        if (pLocal == null) return null;
        if (mOverlayRoot == null) return pLocal;
        int[] loc = new int[2];
        try {
            mOverlayRoot.getLocationOnScreen(loc);
        } catch (Throwable ignored) { loc[0] = loc[1] = 0; }
        return new PointF(pLocal.x + loc[0], pLocal.y + loc[1]);
    }

    public void init(@NonNull AccessibilityService s) {
        this.mService = s;
        mPointerControl.reset();
        timerHandler = new Handler();
        isEnabled = false;
        disableInertia = Helper.isInertiaDisabled(s);
    }

    private void attachTimer (final int direction) {
        if (previousRunnable != null) {
            detachPreviousTimer();
        }
        previousRunnable = new Runnable() {
            @Override public void run() {
                maybeShowPointer();
                kickIdleTimer();
                int step = mapPointerStep(pointerSpeed, momentumStack);
                mPointerControl.move(direction, step);
                bumpMomentum();  // <<< unified
                timerHandler.postDelayed(this, 30);
            }
        };
        timerHandler.postDelayed(previousRunnable, 0);
    }

    private void attachGesture(final PointF originPoint, final int direction) {
        if (previousRunnable != null) detachPreviousTimer();
        maybeShowPointer();

        cancelIdleTimer();

        disableInertia = Helper.isInertiaDisabled(mService);

        scrollHeld = true;
        momentumStack = 0;

        if (ModernInput.isAvailable()) {
            dispatchScrollSegment(direction);
        } else {
            timerHandler.post(new Runnable() {
                @Override public void run() {
                    if (!scrollHeld) return;
                    maybeShowPointer();
                    kickIdleTimer();

                    LegacyInput.scrollOnce(mService, mPointerControl, direction);
                    bumpMomentum();

                    int gap = LegacyInput.suggestedDelayMs(scrollSpeed);
                    timerHandler.postDelayed(this, gap);
                }
            });
        }
    }

    private final Runnable idleRunnable = new Runnable() {
        @Override public void run() {
            if (isEnabled && Helper.isAutoHideEnabled(mService) && !hideInLauncher) {
                setMouseModeEnabled(false, true);
            }
        }
    };

    public void setHideInLauncher(boolean hide) {
        if (hide == hideInLauncher) return;
        hideInLauncher = hide;

        if (hide) {
            cancelIdleTimer();
            wasEnabledBeforeLauncher = isEnabled;
            prevInScrollMode = isInScrollMode;
            if (isEnabled) setMouseModeEnabled(false, false);
            return;
        }

        if (Boolean.TRUE.equals(wasEnabledBeforeLauncher)) {
            setMouseModeEnabled(true, false);
            isInScrollMode = prevInScrollMode;
            Helper.toast(mService, isInScrollMode ? "Scroll mode" : "Mouse mode", android.widget.Toast.LENGTH_SHORT);
            kickIdleTimer();
        }
        wasEnabledBeforeLauncher = null;
    }

    private void maybeShowPointer() {
        if (!hideInLauncher && isEnabled) mPointerControl.reappear();
    }

    private void cancelIdleTimer() {
        idleHandler.removeCallbacks(idleRunnable);
    }

    private void kickIdleTimer() {
        idleHandler.removeCallbacks(idleRunnable);
        if (isEnabled && !scrollHeld && Helper.isAutoHideEnabled(mService) && !hideInLauncher) {
            idleHandler.postDelayed(idleRunnable, IDLE_TIMEOUT_MS);
        }
    }

    private int activeMomentumCap() {
        return disableInertia ? SOFT_MOMENTUM_CAP : MOMENTUM_CAP;
    }

    private void bumpMomentum() {
        momentumStack = Math.min(momentumStack + 1, activeMomentumCap());
    }

    private int currentMomentum() { return momentumStack; }

    private static int clamp(int v,int min,int max){return v<min?min:(v>max?max:v);}
    private static int mapPointerStep(int speed,int momentum){
        int s=clamp(speed,1,1000);
        long base=1+s/40;
        long acc=1+(momentum*(5+s/50))/20;
        long step=base+acc;
        return (int)Math.min(step,200);
    }

    private static int gestureCadenceMs(int speed) {
        int s = clamp(speed, 1, 1000);
        // e.g. ~220ms at slowest → ~24ms at fastest
        int min = 24, max = 220;
        return max - ((max - min) * s) / 1000;
    }

    private void dispatchSwipe(final PointF origin, final int direction, final boolean repeat) {
        if (!isEnabled) return;

        if (!repeat && previousRunnable != null) {
            detachPreviousTimer();
        }

        previousRunnable = new Runnable() {
            @Override public void run() {
                int effMomentum = currentMomentum();
                int dist = computeScrollDistance(scrollSpeed, effMomentum);
                int dur  = computeScrollDurationMs(scrollSpeed, dist);
                PointF startLocal = mPointerControl.getPointerLocation();
                PointF start = pointerToScreen(startLocal);

                if (ModernInput.isAvailable()) {
                    if (direction != lastScrollDir) {
                        momentumStack = 0;
                    }
                    lastScrollDir = direction;
                    if (scrollBusy) return;   // guard
                    scrollBusy = true;
                    ModernInput.swipeWithCallbacks(
                            mService,
                            start,
                            PointerControl.dirX[direction],
                            PointerControl.dirY[direction],
                            dist,
                            dur,
                            () -> {
                                scrollBusy = false;
                                bumpMomentum();
                                if (repeat && scrollHeld && previousRunnable != null) {
                                    timerHandler.post(this);
                                }
                            }
                    );
                } else {
                    LegacyInput.scrollOnce(mService, mPointerControl, direction);
                    bumpMomentum();
                    if (repeat && scrollHeld && previousRunnable != null) {
                        int gap = LegacyInput.suggestedDelayMs(scrollSpeed);
                        timerHandler.postDelayed(this, gap);
                    }
                }
            }
        };

        timerHandler.post(previousRunnable);
    }

    private void dispatchScrollSegment(final int direction) {
        if (!scrollHeld) return;
        dispatchSwipe(mPointerControl.getPointerLocation(), direction, true);
    }

    private void createSwipeForSingle(final PointF originPoint, final int direction) {
        dispatchSwipe(originPoint, direction, false);
    }

    private void detachPreviousTimer() {
        scrollHeld = false;
        scrollBusy = false;
        if (disappearTimer != null) disappearTimer.cancel();
        if (previousRunnable != null) {
            timerHandler.removeCallbacks(previousRunnable);
            previousRunnable = null;
        }
        momentumStack = 0;
        kickIdleTimer();
        disappearTimer = new CountDownTimer(10000, 10000) {
            @Override public void onTick(long l) {}
            @Override public void onFinish() { mPointerControl.disappear(); }
        };
        disappearTimer.start();
        kickIdleTimer();
    }

    private int computeScrollDistance(int speed, int momentum) {
        int s = clamp(speed, 1, 1000);
        int shortSide = Math.max(1, Math.min(mPointerControl.getWidth(), mPointerControl.getHeight()));

        float maxFrac = 0.28f + 0.32f * (s - 1) / 999f; // was 0.20..0.50 → try 0.28..0.60
        int maxDist = (int)(shortSide * maxFrac);

        int m = Math.min(momentum, activeMomentumCap());
        float base = 14f + 28f * (s - 1) / 999f;   // was 12 + 32
        float gain = 2.2f + 6.8f * (s - 1) / 999f; // was 3 + 9

        int dist = (int)Math.min(base + m * gain, maxDist);
        int minDist = Math.max(8, (int)(touchSlop() * 1.2f)); // was 1.5x
        return Math.max(dist, minDist);
    }

    private int computeScrollDurationMs(int speed, int distance) {
        int s = clamp(speed, 1, 1000);

        // -> ~0.35 px/ms at slowest ... ~2.0 px/ms at fastest
        float vTarget = 0.35f + 1.65f * (s - 1) / 999f;

        int dur = (int)(distance / vTarget);
        return clamp(dur, 60, 180); // was 60..220 → try up to ~320ms
    }

    public void stopAllMotion() {
        scrollHeld = false;
        scrollBusy = false;
        if (previousRunnable != null) {
            timerHandler.removeCallbacks(previousRunnable);
            previousRunnable = null;
        }
        momentumStack = 0;
    }

    private int touchSlop() {
        if (touchSlopPx < 0) touchSlopPx = ViewConfiguration.get(mService).getScaledTouchSlop();
        return touchSlopPx;
    }

    public PointerControl getPointerControl() { return mPointerControl; }

    public void resetPointerFromPrefs() {
        if (mPointerControl != null) mPointerControl.reset();
    }

    public boolean perform(KeyEvent keyEvent) {
        final int code = keyEvent.getKeyCode();
        final int action = keyEvent.getAction();

        if (isBossKey(code)) {
            return handleBossKey(keyEvent);
        }

        if (isInfoKey(code)) {
            return handleInfoKey(action);
        }

        if (!isEnabled) return false;

        if (action == KeyEvent.ACTION_DOWN) {
            return handleKeyDown(code);
        } else if (action == KeyEvent.ACTION_UP) {
            return handleKeyUp(code, keyEvent);
        }

        return false;
    }

    private boolean isBossKey(int code) {
        return code == bossKey;
    }

    private boolean isInfoKey(int code) {
        return code == KeyEvent.KEYCODE_INFO;
    }

    private boolean handleBossKey(KeyEvent event) {
        if (isBossKeyDisabled) return false;

        if (!isBossKeySetToToggle) {
            return handleBossHoldMode(event);
        } else {
            return handleBossToggleMode(event);
        }
    }

    private boolean handleBossHoldMode(KeyEvent event) {
        int action = event.getAction();

        if (action == KeyEvent.ACTION_UP) {
            if (waitToChange != null) {
                waitToChange.cancel();
                if (isEnabled) return true;
            }
        }

        if (action == KeyEvent.ACTION_DOWN) {
            waitToChange();
            if (isEnabled) {
                isInScrollMode = !isInScrollMode;
                Helper.toast(mService,
                        isInScrollMode ? "Scroll mode: Enabled" : "Scroll mode: Disabled",
                        Toast.LENGTH_SHORT);
                return true;
            }
        }

        return false;
    }

    private boolean handleBossToggleMode(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;

        if (isEnabled && isInScrollMode) {
            setMouseModeEnabled(false, true);
            isInScrollMode = false;
        } else if (isEnabled && !isInScrollMode) {
            Helper.toast(mService, "Scroll mode", Toast.LENGTH_SHORT);
            isInScrollMode = true;
        } else if (!isEnabled) {
            setMouseModeEnabled(true, true);
            isInScrollMode = false;
        }

        return true;
    }

    private boolean handleInfoKey(int action) {
        if (action != KeyEvent.ACTION_DOWN) return false;

        if (isEnabled) {
            setMouseModeEnabled(false, true);
            return true;
        } else {
            setMouseModeEnabled(true, true);
            return true;
        }
    }

    private boolean handleKeyUp(int code, KeyEvent event) {
        if (actionableKeyMap.contains(code) || code == bossKey) {
            detachPreviousTimer();
            return true;
        }

        if (isOkKey(code)) {
            DPAD_SELECT_PRESSED = false;
            detachPreviousTimer();

            int action = AccessibilityNodeInfo.ACTION_CLICK;
            Point pointer = new Point(
                    (int) mPointerControl.getPointerLocation().x,
                    (int) mPointerControl.getPointerLocation().y
            );

            if (DPAD_Center_Init_Point.equals(pointer)) {
                return performClickAtPointer(event, pointer, action);
            } else {
                // TODO: implement drag here if you want drag support
                //forceTapAtPointer(event);
                return false; // kept at false to avoid missing click issues
            }
        }

        return false;
    }

    private void forceTapAtPointer(KeyEvent event) {
        if (ModernInput.isAvailable()) {
            ModernInput.click(
                    mService,
                    pointerToScreen(mPointerControl.getPointerLocation()),
                    Math.max(1, event != null ? (event.getEventTime() - event.getDownTime()) : 60)
            );
        } else {
            LegacyInput.clickAtPointer(mService, mPointerControl);
        }
    }

    private boolean performClickAtPointer(KeyEvent event, Point pInt, int action) {
        boolean wasIME = false;
        boolean focused = false;
        boolean clicked = false;

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            List<android.view.accessibility.AccessibilityWindowInfo> windowList;
            try {
                windowList = mService.getWindows();
            } catch (Throwable t) {
                windowList = Collections.emptyList();
            }

            if (windowList != null) {
                for (android.view.accessibility.AccessibilityWindowInfo window : windowList) {
                    if (clicked || wasIME) break;
                    if (window == null) continue;

                    AccessibilityNodeInfo root = null;
                    try {
                        root = window.getRoot();
                    } catch (Throwable ignored) {}

                    if (root == null) continue;

                    List<AccessibilityNodeInfo> nodeHierarchy = findNode(root, action, pInt);

                    for (int i = nodeHierarchy.size() - 1; i >= 0; i--) {
                        if (clicked || focused) break;

                        AccessibilityNodeInfo hitNode = nodeHierarchy.get(i);
                        if (hitNode == null) continue;

                        List<AccessibilityNodeInfo.AccessibilityAction> acts = hitNode.getActionList();

                        if (acts.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS)) {
                            focused = hitNode.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
                        }
                        if (hitNode.isFocused() && acts.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SELECT)) {
                            hitNode.performAction(AccessibilityNodeInfo.ACTION_SELECT);
                        }
                        if (hitNode.isFocused() && acts.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)) {
                            clicked = hitNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }

                        int wType = -1;
                        try {
                            wType = window.getType();
                        } catch (Throwable ignored) {}

                        if (wType == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD
                                && !String.valueOf(hitNode.getPackageName()).contains("leankeyboard")) {

                            CharSequence pkg = hitNode.getPackageName();
                            if ("com.amazon.tv.ime".contentEquals(pkg)
                                    && event.getKeyCode() == KeyEvent.KEYCODE_BACK
                                    && helperContext != null) {
                                InputMethodManager imm =
                                        (InputMethodManager) helperContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                                if (imm != null) {
                                    imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                                }
                                clicked = wasIME = true;
                            } else {
                                wasIME = true;
                                clicked = hitNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            }
                            break;
                        }

                        if ("com.google.android.tvlauncher".contentEquals(hitNode.getPackageName())
                                && acts.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)) {
                            if (hitNode.isFocusable()) {
                                focused = hitNode.performAction(AccessibilityNodeInfo.FOCUS_INPUT);
                            }
                            clicked = hitNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    }
                }
            }
        }

        if (!clicked && !wasIME) {
            kickIdleTimer();
            if (ModernInput.isAvailable()) {
                ModernInput.click(
                        mService,
                        pointerToScreen(mPointerControl.getPointerLocation()),
                        Math.max(1, event.getEventTime() - event.getDownTime())
                );
                return true;
            } else {
                return LegacyInput.clickAtPointer(mService, mPointerControl);
            }
        }

        return clicked || wasIME;
    }

    public boolean forceModernClickAtPointer(@NonNull AccessibilityService svc, long pressDurationMs) {
        if (!ModernInput.isAvailable() || mPointerControl == null) return false;
        PointF p = pointerToScreen(mPointerControl.getPointerLocation());
        if (p == null) return false;
        ModernInput.click(svc, p, Math.max(1, pressDurationMs));
        return true;
    }

    private boolean handleKeyDown(int code) {
        if (scrollCodeMap.containsKey(code)) {
            if (isInScrollMode || colorSet.contains(code)) {
                attachGesture(mPointerControl.getPointerLocation(), scrollCodeMap.get(code));
            } else if (!isInScrollMode && stuckAtSide != 0 && code == stuckAtSide) {
                createSwipeForSingle(mPointerControl.getCenterPointOfView(), scrollCodeMap.get(code));
            } else if (movementCodeMap.containsKey(code)) {
                attachTimer(movementCodeMap.get(code));
            }
            return true;
        }

        if (isOkKey(code)) {
            DPAD_Center_Init_Point = new Point(
                    (int) mPointerControl.getPointerLocation().x,
                    (int) mPointerControl.getPointerLocation().y
            );
            DPAD_SELECT_PRESSED = true;
            kickIdleTimer();
            return true;
        }
        return false;
    }

    private void setMouseModeEnabled(boolean enable, boolean toasts) {
        if (enable) {
            this.isEnabled = true;
            isInScrollMode = false;
            mPointerControl.reset();
            maybeShowPointer();
            if(toasts){
                Helper.toast(mService, "Mouse mode", Toast.LENGTH_SHORT);
            }
            kickIdleTimer();
        }
        else {
            this.isEnabled = false;
            mPointerControl.disappear();
            if(toasts) {
                Helper.toast(mService, "D-Pad mode", Toast.LENGTH_SHORT);
            }
            cancelIdleTimer();
        }
    }

    private void waitToChange() {
        waitToChange = new CountDownTimer(800, 800) {
            @Override
            public void onTick(long l) { }
            @Override
            public void onFinish() {
                setMouseModeEnabled(!isEnabled, true);
            }
        };
        waitToChange.start();
    }

    //// below code is for supporting legacy devices as per my understanding of evia face cam source
    //// this is only used for long clicks here and isn't exactly something reliable
    //// leaving it in for reference just in case needed in future, because looking up face cam
    //// app's source might be a daunting task

    private List<AccessibilityNodeInfo> findNode (AccessibilityNodeInfo node, int action, Point pInt) {
        if (node == null) {
            node = mService.getRootInActiveWindow();
        }
        if (node == null) {
            Log.i(LOG_TAG, "Root node ======>>>>>" + ((node != null) ? node.toString() : "null"));
        }
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        Log.i(LOG_TAG, "Node found ?" + ((node != null) ? node.toString() : "null"));
        node = findNodeHelper(node, action, pInt, nodeInfos);
        Log.i(LOG_TAG, "Node found ?" + ((node != null) ? node.toString() : "null"));
        Log.i(LOG_TAG, "Number of nodes ?=>>>>> " + nodeInfos.size());
        return nodeInfos;
    }

    private AccessibilityNodeInfo findNodeHelper (AccessibilityNodeInfo node, int action, Point pInt, List<AccessibilityNodeInfo> nodeList) {
        if (node == null) {
            return null;
        }
        Rect tmp = new Rect();
        node.getBoundsInScreen(tmp);
        if (!tmp.contains(pInt.x, pInt.y)) {
            return null;
        }
        nodeList.add(node);
        AccessibilityNodeInfo result = null;
        result = node;
//        if ((node.getActions() & action) != 0 && node != null) {
//            // possible to use this one, but keep searching children as well
//            nodeList.add(node);
//        }
        int childCount = node.getChildCount();
        for (int i=0; i<childCount; i++) {
            AccessibilityNodeInfo child = findNodeHelper(node.getChild(i), action, pInt, nodeList);
            if (child != null) {
                result = child;
            }
        }
        return result;
    }

    /** Not used
     * Letting this stay here just in case the code needs porting back to an obsolete version
     * sometime in future
     //    private void attachActionable (final int action, final AccessibilityNodeInfo node) {
     //        if (previousRunnable != null) {
     //            detachPreviousTimer();
     //        }
     //        previousRunnable = new Runnable() {
     //            @Override
     //            public void run() {
     //                maybeShowPointer();
     //                kickIdleTimer();
     //                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
     //                node.performAction(action);
     //                node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS);
     //                timerHandler.postDelayed(this, 30);
     //            }
     //        };
     //        timerHandler.postDelayed(previousRunnable, 0);
     //    }
     **/
}