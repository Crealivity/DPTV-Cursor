package io.github.crealivity.dptvcursor.assets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.crealivity.dptvcursor.R;

public final class PointerAssets {
    private static final Map<String, Integer> textToResourceIdMap = new LinkedHashMap<>();
    private static final Map<String, Integer> textToOffsetX = new HashMap<>();
    private static final Map<String, Integer> textToOffsetY = new HashMap<>();

    // Optional: keep these as constants if you prefer
    public static final String STYLE_DEFAULT     = "Default";
    public static final String STYLE_LIGHT       = "Light";
    public static final String STYLE_DARK        = "Dark";
    public static final String STYLE_ORB         = "Orb";
    public static final String STYLE_ORB_DARK    = "Orb Dark";
    public static final String STYLE_HAND        = "Hand";
    public static final String STYLE_ARROW       = "Arrow";
    public static final String STYLE_CAT         = "Cat";
    public static final String STYLE_CHICK       = "Chick";
    public static final String STYLE_GHOST       = "Ghost";
    public static final String STYLE_HAMMER      = "Hammer";
    public static final String STYLE_HAND_ALT    = "Hand Alt";
    public static final String STYLE_ICECREAM    = "Ice Cream";
    public static final String STYLE_ONIGIRI     = "Onigiri";
    public static final String STYLE_PAPER       = "Paper";
    public static final String STYLE_RAINBOW     = "Rainbow";
    public static final String STYLE_ROCKET      = "Rocket";
    public static final String STYLE_WATERMELON  = "Watermelon";
    public static final String STYLE_CUSTOM      = "Custom";

    static {
        // original cursors
        textToResourceIdMap.put(STYLE_DEFAULT, R.drawable.pointer);
        textToOffsetX.put(STYLE_DEFAULT, 0);
        textToOffsetY.put(STYLE_DEFAULT, 0);

        textToResourceIdMap.put(STYLE_LIGHT, R.drawable.pointer_light);
        textToOffsetX.put(STYLE_LIGHT, 0);
        textToOffsetY.put(STYLE_LIGHT, 0);

        textToResourceIdMap.put(STYLE_DARK, R.drawable.pointer_mac);
        textToOffsetX.put(STYLE_DARK, 8);
        textToOffsetY.put(STYLE_DARK, 6);

        textToResourceIdMap.put(STYLE_ORB, R.drawable.light_orb);
        textToOffsetX.put(STYLE_ORB, 54);
        textToOffsetY.put(STYLE_ORB, 52);

        textToResourceIdMap.put(STYLE_ORB_DARK, R.drawable.dark_orb);
        textToOffsetX.put(STYLE_ORB_DARK, 54);
        textToOffsetY.put(STYLE_ORB_DARK, 52);

        textToResourceIdMap.put(STYLE_HAND, R.drawable.finger_mac);
        textToOffsetX.put(STYLE_HAND, 56);
        textToOffsetY.put(STYLE_HAND, 10);

        // new cursors
        textToResourceIdMap.put(STYLE_ARROW, R.drawable.pointer_arrow);
        textToOffsetX.put(STYLE_ARROW, 0);
        textToOffsetY.put(STYLE_ARROW, 0);

        textToResourceIdMap.put(STYLE_CAT, R.drawable.pointer_cat);
        textToOffsetX.put(STYLE_CAT, 20);
        textToOffsetY.put(STYLE_CAT, 0);

        textToResourceIdMap.put(STYLE_CHICK, R.drawable.pointer_chick);
        textToOffsetX.put(STYLE_CHICK, 18);
        textToOffsetY.put(STYLE_CHICK, 6);

        textToResourceIdMap.put(STYLE_GHOST, R.drawable.pointer_ghost);
        textToOffsetX.put(STYLE_GHOST, 22);
        textToOffsetY.put(STYLE_GHOST, 6);

        textToResourceIdMap.put(STYLE_HAMMER, R.drawable.pointer_hammer);
        textToOffsetX.put(STYLE_HAMMER, 6);
        textToOffsetY.put(STYLE_HAMMER, 32);

        textToResourceIdMap.put(STYLE_HAND_ALT, R.drawable.pointer_hand);
        textToOffsetX.put(STYLE_HAND_ALT, 12);
        textToOffsetY.put(STYLE_HAND_ALT, 4);

        textToResourceIdMap.put(STYLE_ICECREAM, R.drawable.pointer_icecream);
        textToOffsetX.put(STYLE_ICECREAM, 10);
        textToOffsetY.put(STYLE_ICECREAM, 40);

        textToResourceIdMap.put(STYLE_ONIGIRI, R.drawable.pointer_onigiri);
        textToOffsetX.put(STYLE_ONIGIRI, 12);
        textToOffsetY.put(STYLE_ONIGIRI, 26);

        textToResourceIdMap.put(STYLE_PAPER, R.drawable.pointer_paper);
        textToOffsetX.put(STYLE_PAPER, 6);
        textToOffsetY.put(STYLE_PAPER, 16);

        textToResourceIdMap.put(STYLE_RAINBOW, R.drawable.pointer_rainbow);
        textToOffsetX.put(STYLE_RAINBOW, 18);
        textToOffsetY.put(STYLE_RAINBOW, 18);

        textToResourceIdMap.put(STYLE_ROCKET, R.drawable.pointer_rocket);
        textToOffsetX.put(STYLE_ROCKET, 10);
        textToOffsetY.put(STYLE_ROCKET, 42);

        textToResourceIdMap.put(STYLE_WATERMELON, R.drawable.pointer_watermelon);
        textToOffsetX.put(STYLE_WATERMELON, 18);
        textToOffsetY.put(STYLE_WATERMELON, 12);

        // placeholder for a custom cursor (resource id 0 = file-based)
        textToResourceIdMap.put(STYLE_CUSTOM, 0);
        textToOffsetX.put(STYLE_CUSTOM, 0);
        textToOffsetY.put(STYLE_CUSTOM, 0);
    }

    public static List<String> getAllStyles() {
        return new ArrayList<>(textToResourceIdMap.keySet());
    }

    public static int getResId(String style) {
        return textToResourceIdMap.getOrDefault(style, R.drawable.pointer);
    }

    public static int getOffsetX(String style) {
        return textToOffsetX.getOrDefault(style, 0);
    }

    public static int getOffsetY(String style) {
        return textToOffsetY.getOrDefault(style, 0);
    }
}