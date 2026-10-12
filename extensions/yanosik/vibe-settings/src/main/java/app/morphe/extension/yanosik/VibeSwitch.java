/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.extension.yanosik;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.widget.Switch;

public final class VibeSwitch extends Switch {
    public VibeSwitch(Context context, boolean dark) {
        super(context);
        float density = context.getResources().getDisplayMetrics().density;
        setShowText(false);
        setSplitTrack(false);
        setSwitchMinWidth(Math.round(52 * density));
        setThumbTintList(null);
        setTrackTintList(null);
        setThumbDrawable(new SwitchPart(density, dark, true));
        setTrackDrawable(new SwitchPart(density, dark, false));
    }

    static final class SwitchPart extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF surface = new RectF();
        private final float density;
        private final boolean dark;
        private final boolean thumb;
        private boolean checked;
        private boolean enabled;
        private boolean pressed;
        private int alpha = 255;

        SwitchPart(float density, boolean dark, boolean thumb) {
            this.density = density;
            this.dark = dark;
            this.thumb = thumb;
        }

        @Override
        public void draw(Canvas canvas) {
            paint.setStyle(Paint.Style.FILL);
            if (thumb) {
                paint.setColor(checked ? 0xffffffff : dark ? 0xff938f99 : 0xff79747e);
                paint.setAlpha(enabled ? alpha : Math.round(alpha * 0.38f));
                float radius = (pressed ? 14 : checked ? 12 : 8) * density;
                Rect bounds = getBounds();
                canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), radius, paint);
            } else {
                surface.set(getBounds());
                paint.setColor(checked ? 0xffe72e38 : dark ? 0xff363438 : 0xffe7e0ec);
                paint.setAlpha(enabled ? alpha : Math.round(alpha * 0.12f));
                canvas.drawRoundRect(surface, 16 * density, 16 * density, paint);
                if (!checked) {
                    surface.inset(density, density);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2 * density);
                    paint.setColor(dark ? 0xff938f99 : 0xff79747e);
                    paint.setAlpha(enabled ? alpha : Math.round(alpha * 0.12f));
                    canvas.drawRoundRect(surface, 15 * density, 15 * density, paint);
                }
            }
        }

        @Override
        protected boolean onStateChange(int[] states) {
            boolean nextChecked = false, nextEnabled = false, nextPressed = false;
            for (int state : states) {
                if (state == android.R.attr.state_checked) nextChecked = true;
                if (state == android.R.attr.state_enabled) nextEnabled = true;
                if (state == android.R.attr.state_pressed) nextPressed = true;
            }
            if (checked == nextChecked && enabled == nextEnabled && pressed == nextPressed) return false;
            checked = nextChecked;
            enabled = nextEnabled;
            pressed = nextPressed;
            invalidateSelf();
            return true;
        }

        @Override
        public boolean isStateful() { return true; }

        @Override
        public int getIntrinsicWidth() { return Math.round((thumb ? 20 : 52) * density); }

        @Override
        public int getIntrinsicHeight() { return Math.round((thumb ? 28 : 32) * density); }

        @Override
        public boolean getPadding(Rect padding) {
            int horizontal = thumb ? 0 : Math.round(6 * density);
            padding.set(horizontal, 0, horizontal, 0);
            return !thumb;
        }

        @Override
        public void setAlpha(int alpha) { this.alpha = alpha; invalidateSelf(); }

        @Override
        public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }

        @Override
        public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
