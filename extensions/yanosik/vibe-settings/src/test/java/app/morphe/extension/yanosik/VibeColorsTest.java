/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.extension.yanosik;

/** Standalone JVM checks; no Android runtime or account/network dependencies. */
public final class VibeColorsTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    private static long packed(int argb) { return Integer.toUnsignedLong(argb) << 32; }
    public static void main(String[] args) {
        check(VibeColors.BACKGROUND == 0xff1f1e1f);
        for (int surface : new int[]{0xffffffff, 0xfff5f5f5, 0xfff1f5f7, 0xffe3eaed}) {
            check(VibeColors.isLightSurface(surface));
            check(VibeColors.compose(packed(surface), packed(0xff192026)) == packed(0xff1f1e1f));
        }
        check(!VibeColors.isLightSurface(0));
        check(!VibeColors.isLightSurface(0xfff0303e));
        for (int text : new int[]{0xff192026, 0xff303a40, 0xff414141, 0xff102469})
            check(VibeColors.text(text) == 0xfff1f1f1);
        for (int icon : new int[]{0xfff8ba21, 0xff009ba1, 0xfff0303e, 0xff27bacb})
            check(VibeColors.text(icon) == icon);
        // White foreground on branded buttons remains white, not a dark surface.
        check(VibeColors.compose(packed(0xffffffff), packed(0xffffffff)) == packed(0xffffffff));
        check(VibeColors.compose(packed(0xff303a40), packed(0xffffffff)) == packed(0xffffffff));
        check(VibeColors.compose(packed(0xfff0303e), packed(0xfff0303e)) == packed(0xfff0303e));
        check(VibeColors.compose(123L, 456L) == 456L);
        check(VibeColors.text(0x80192026) == 0x80f1f1f1);
        System.out.println("PASS local theme colors: exact dark surface, readable text, original brand/foreground colors");
    }
}
