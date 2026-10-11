/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.extension.yanosik;

/** Neutral surface/text conversion; colorful service icons and brand colors retain their hue. */
public final class VibeColors {
    public static final int BACKGROUND = 0xff1f1e1f;
    public static final int TEXT = 0xfff1f1f1;

    private VibeColors() {}

    public static boolean isLightSurface(int color) {
        int r = (color >>> 16) & 255, g = (color >>> 8) & 255, b = color & 255;
        return (color >>> 24) != 0 && Math.min(r, Math.min(g, b)) >= 210 &&
                Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) <= 32;
    }

    public static boolean isDarkText(int color) {
        int r = (color >>> 16) & 255, g = (color >>> 8) & 255, b = color & 255;
        return (color >>> 24) != 0 && Math.max(r, Math.max(g, b)) < 170 &&
                Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) <= 100;
    }

    public static int text(int color) {
        return isDarkText(color) ? ((color & 0xff000000) | (TEXT & 0xffffff)) : color;
    }

    /** Compose's sRGB packed representation stores ARGB in the upper 32 bits. */
    public static long compose(long lightColor, long darkColor) {
        if ((lightColor & 0xffffffffL) == 0 && (darkColor & 0xffffffffL) == 0 &&
                isLightSurface((int) (lightColor >>> 32)) && isDarkText((int) (darkColor >>> 32)))
            return Integer.toUnsignedLong(BACKGROUND) << 32;
        // Preserve the application's intended dark text, icon and brand palette.
        return darkColor;
    }
}
