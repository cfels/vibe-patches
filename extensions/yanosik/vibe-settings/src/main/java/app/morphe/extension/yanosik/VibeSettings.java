/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.extension.yanosik;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public final class VibeSettings {
    private static final String DASHBOARD =
            "pl.neptis.yanosik.mobi.android.dashboard.dashboardpack.activity.DashboardPackActivity";
    private static final String ACCOUNT = "pl.neptis.features.login.AccountStartActivity";
    private static final String PREFERENCES = "vibe_settings";
    private static final String DARK = "dark_home_sidebar";

    private VibeSettings() {}

    private static Activity activity(Context context) {
        for (int depth = 0; context != null && depth < 16; depth++) {
            if (context instanceof Activity) return (Activity) context;
            if (!(context instanceof ContextWrapper)) return null;
            Context base = ((ContextWrapper) context).getBaseContext();
            if (base == context) return null;
            context = base;
        }
        return null;
    }

    static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    /** The patched palette helper is shared, so enforce the activity boundary here too. */
    public static boolean useDark(Context context) {
        Activity host = activity(context);
        return host != null && (host.getClass().getName().equals(DASHBOARD) ||
                host.getClass().getName().equals(ACCOUNT)) && preferences(context).getBoolean(DARK, true);
    }

    public static long darkComposeColor(long lightColor, long darkColor) {
        return VibeColors.compose(lightColor, darkColor);
    }

    public static void bindTile(View titleView, View iconView, View clickView, int originalTextColor) {
        TextView title = (TextView) titleView;
        ImageView icon = (ImageView) iconView;
        title.setText("Vibe settings");
        title.setTextColor(useDark(title.getContext()) ? VibeColors.text(originalTextColor) : originalTextColor);
        Context context = icon.getContext();
        int resource = context.getResources().getIdentifier("vibe_patched", "drawable", context.getPackageName());
        icon.setImageResource(resource);
        icon.setVisibility(View.VISIBLE);
        boolean dark = useDark(context);
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable tile = new GradientDrawable();
        tile.setColor(dark ? 0xff332526 : 0xfffff1f2);
        tile.setCornerRadius(8 * density);
        tile.setStroke(Math.max(1, (int) density), dark ? 0xff784047 : 0xffeeb5ba);
        android.graphics.drawable.InsetDrawable inset = new android.graphics.drawable.InsetDrawable(
                icon.getDrawable(), (int) (3 * density));
        // The original binder always replaces the image on reuse. Keep the accent
        // inside that drawable so another service cannot inherit this tile's style.
        icon.setImageDrawable(new android.graphics.drawable.LayerDrawable(new Drawable[] { tile, inset }));
        clickView.setClickable(true);
        clickView.setOnClickListener(new TileClick(null));
    }

    public static void showSettings(Context context) {
        Activity host = activity(context);
        if (host == null || host.isFinishing() || host.isDestroyed() ||
                !host.getClass().getName().equals(DASHBOARD)) return;
        Context dialogContext = new android.view.ContextThemeWrapper(host,
                useDark(context) ? android.R.style.Theme_Material_Dialog_Alert :
                        android.R.style.Theme_Material_Light_Dialog_Alert);
        boolean dark = useDark(context);
        float density = context.getResources().getDisplayMetrics().density;
        int padding = (int) (24 * density);
        int textColor = dark ? VibeColors.TEXT : 0xff232323;
        int secondary = dark ? 0xffbdbdbd : 0xff666666;
        int accent = 0xffe72e38;
        LinearLayout content = new LinearLayout(dialogContext);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, padding, padding, padding);
        GradientDrawable surface = new GradientDrawable();
        surface.setColor(dark ? VibeColors.BACKGROUND : 0xffffffff);
        surface.setCornerRadius(24 * density);
        content.setBackground(surface);
        content.setClipToOutline(true);
        LinearLayout header = new LinearLayout(dialogContext);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(dialogContext);
        icon.setImageResource(context.getResources().getIdentifier("vibe_patched", "drawable", context.getPackageName()));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        header.addView(icon, new LinearLayout.LayoutParams((int) (48 * density), (int) (48 * density)));
        LinearLayout heading = new LinearLayout(dialogContext);
        heading.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(-1, -2);
        headingParams.leftMargin = (int) (16 * density);
        header.addView(heading, headingParams);
        TextView title = new TextView(dialogContext);
        title.setText("Vibe settings");
        title.setTextSize(22);
        title.setTextColor(textColor);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        heading.addView(title);
        TextView subtitle = new TextView(dialogContext);
        subtitle.setText("Appearance");
        subtitle.setTextSize(13);
        subtitle.setTextColor(secondary);
        subtitle.setPadding(0, (int) (4 * density), 0, 0);
        heading.addView(subtitle);
        content.addView(header, new LinearLayout.LayoutParams(-1, -2));
        View divider = new View(dialogContext);
        divider.setBackgroundColor(dark ? 0xff3a393a : 0xffececec);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, Math.max(1, (int) density));
        dividerParams.topMargin = padding;
        dividerParams.bottomMargin = (int) (8 * density);
        content.addView(divider, dividerParams);
        LinearLayout row = new LinearLayout(dialogContext);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setMinimumHeight((int) (80 * density));
        LinearLayout labels = new LinearLayout(dialogContext);
        labels.setOrientation(LinearLayout.VERTICAL);
        row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        TextView label = new TextView(dialogContext);
        label.setText("Dark mode");
        label.setTextSize(16);
        label.setTextColor(textColor);
        label.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        labels.addView(label);
        TextView explanation = new TextView(dialogContext);
        explanation.setText("Home, sidebar and login screens.");
        explanation.setTextSize(13);
        explanation.setTextColor(secondary);
        explanation.setPadding(0, (int) (6 * density), (int) (12 * density), 0);
        explanation.setLineSpacing(2 * density, 1);
        labels.addView(explanation);
        Switch toggle = new Switch(dialogContext);
        toggle.setContentDescription("Dark mode");
        toggle.setMinHeight((int) (48 * density));
        toggle.setMinWidth((int) (56 * density));
        toggle.setChecked(preferences(context).getBoolean(DARK, true));
        int[][] states = new int[][] { new int[] { android.R.attr.state_checked }, new int[] {} };
        toggle.setThumbTintList(new android.content.res.ColorStateList(states, new int[] { accent, dark ? 0xffbdbdbd : 0xffeeeeee }));
        toggle.setTrackTintList(new android.content.res.ColorStateList(states, new int[] { 0x99e72e38, dark ? 0xff555455 : 0xffbbbbbb }));
        row.addView(toggle, new LinearLayout.LayoutParams(-2, (int) (48 * density)));
        content.addView(row, new LinearLayout.LayoutParams(-1, -2));
        AlertDialog dialog = new AlertDialog.Builder(dialogContext).setView(content).create();
        android.widget.Button done = new android.widget.Button(dialogContext);
        done.setText("Done");
        done.setAllCaps(false);
        done.setTextSize(15);
        done.setTextColor(0xffffffff);
        done.setMinHeight((int) (48 * density));
        GradientDrawable buttonSurface = new GradientDrawable();
        buttonSurface.setColor(accent);
        buttonSurface.setCornerRadius(12 * density);
        done.setBackground(buttonSurface);
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(-1, (int) (48 * density));
        doneParams.topMargin = (int) (24 * density);
        content.addView(done, doneParams);
        done.setOnClickListener(new TileClick(dialog));
        toggle.setOnCheckedChangeListener(new DarkToggle(context, host, dialog));
        dialog.show();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        int width = Math.min((int) (400 * density), context.getResources().getDisplayMetrics().widthPixels - 2 * padding);
        dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    /** Called only by the two verified activity hooks after original setContentView. */
    public static void install(Activity host) {
        if (!useDark(host)) return;
        View root = host.findViewById(android.R.id.content);
        if (root == null) return;
        root.setBackgroundColor(VibeColors.BACKGROUND);
        host.getWindow().getDecorView().setBackgroundColor(VibeColors.BACKGROUND);
        ThemeObserver observer = new ThemeObserver(host, root);
        root.post(observer);
        root.getViewTreeObserver().addOnGlobalLayoutListener(observer);
        root.addOnAttachStateChangeListener(observer);
        host.getWindow().setStatusBarColor(VibeColors.BACKGROUND);
        host.getWindow().setNavigationBarColor(VibeColors.BACKGROUND);
        if (Build.VERSION.SDK_INT >= 30) {
            host.getWindow().getInsetsController().setSystemBarsAppearance(0, 24);
        } else {
            root.setSystemUiVisibility(root.getSystemUiVisibility() & ~(8192 | 16));
        }
    }

    static void applyTree(View view) {
        // CardView paints through its own round-rectangle drawable rather than a
        // standard ColorDrawable. These public method names are verified in 26.9.
        String className = view.getClass().getName();
        Activity host = activity(view.getContext());
        if (host != null && host.getClass().getName().equals(ACCOUNT)) {
            int container = view.getResources().getIdentifier("container", "id", host.getPackageName());
            String id = "";
            if (view.getId() != View.NO_ID) {
                try { id = view.getResources().getResourceEntryName(view.getId()); }
                catch (android.content.res.Resources.NotFoundException ignored) {}
            }
            if (id.equals("container") || id.equals("start_layout") || id.equals("login_layout") ||
                    id.equals("appBarLayout") || (view.getParent() instanceof View &&
                    container != 0 && ((View) view.getParent()).getId() == container))
                view.setBackgroundColor(VibeColors.BACKGROUND);
            if (className.equals("com.google.android.material.textfield.TextInputLayout")) {
                try {
                    android.content.res.ColorStateList hint = android.content.res.ColorStateList.valueOf(0xffbdbdbd);
                    view.getClass().getMethod("setDefaultHintTextColor", android.content.res.ColorStateList.class)
                            .invoke(view, hint);
                    view.getClass().getMethod("setEndIconTintList", android.content.res.ColorStateList.class)
                            .invoke(view, android.content.res.ColorStateList.valueOf(VibeColors.TEXT));
                } catch (ReflectiveOperationException ignored) {}
            }
        }
        if (className.equals("androidx.cardview.widget.CardView") ||
                className.equals("com.google.android.material.card.MaterialCardView")) {
            try {
                android.content.res.ColorStateList color = (android.content.res.ColorStateList)
                        view.getClass().getMethod("getCardBackgroundColor").invoke(view);
                if (VibeColors.isLightSurface(color.getDefaultColor()))
                    view.getClass().getMethod("setCardBackgroundColor", int.class)
                            .invoke(view, VibeColors.BACKGROUND);
            } catch (ReflectiveOperationException ignored) {
                // Ordinary descendants still receive their scoped theme.
            }
        }
        Drawable background = view.getBackground();
        if (background instanceof ColorDrawable &&
                VibeColors.isLightSurface(((ColorDrawable) background).getColor())) {
            view.setBackgroundColor(VibeColors.BACKGROUND);
        } else if (background instanceof GradientDrawable) {
            GradientDrawable gradient = (GradientDrawable) background;
            if (gradient.getColor() != null && VibeColors.isLightSurface(gradient.getColor().getDefaultColor())) {
                Drawable.ConstantState state = gradient.getConstantState();
                GradientDrawable copy = (GradientDrawable) (state != null ? state.newDrawable() : gradient).mutate();
                copy.setColor(VibeColors.BACKGROUND);
                view.setBackground(copy);
            }
        }
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            int color = text.getCurrentTextColor();
            if (VibeColors.isDarkText(color)) text.setTextColor(VibeColors.text(color));
            int hint = text.getCurrentHintTextColor();
            if (VibeColors.isDarkText(hint)) text.setHintTextColor(0xffbdbdbd);
        }
        if (view instanceof ImageView) {
            ImageView image = (ImageView) view;
            int garageId = view.getResources().getIdentifier("garage_grid_services_ic", "drawable",
                    view.getContext().getPackageName());
            Drawable source = image.getDrawable();
            if (garageId != 0 && source instanceof android.graphics.drawable.VectorDrawable) {
                Drawable originalGarage = view.getContext().getDrawable(garageId);
                if (originalGarage != null && source.getConstantState() == originalGarage.getConstantState())
                    image.setImageTintList(android.content.res.ColorStateList.valueOf(VibeColors.TEXT));
            }
            int dropdown = view.getResources().getIdentifier("userNameDropdown", "id",
                    view.getContext().getPackageName());
            if (dropdown != 0 && view.getId() == dropdown)
                image.setImageTintList(android.content.res.ColorStateList.valueOf(VibeColors.TEXT));
            if (image.getImageTintList() != null) {
                int color = image.getImageTintList().getDefaultColor();
                if (VibeColors.isDarkText(color))
                    image.setImageTintList(android.content.res.ColorStateList.valueOf(VibeColors.text(color)));
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) applyTree(group.getChildAt(index));
        }
    }

    public static final class TileClick implements View.OnClickListener {
        private final AlertDialog dialog;
        public TileClick(AlertDialog dialog) { this.dialog = dialog; }
        public void onClick(View view) {
            if (dialog != null) dialog.dismiss(); else showSettings(view.getContext());
        }
    }

    public static final class DarkToggle implements CompoundButton.OnCheckedChangeListener {
        private final Context context;
        private final Activity host;
        private final AlertDialog dialog;
        public DarkToggle(Context context, Activity host, AlertDialog dialog) {
            this.context = context; this.host = host; this.dialog = dialog;
        }
        public void onCheckedChanged(CompoundButton button, boolean enabled) {
            // Write before recreation; the setting survives force-stop and process death.
            if (preferences(context).edit().putBoolean("dark_home_sidebar", enabled).commit()) {
                dialog.dismiss();
                host.recreate();
            }
        }
    }

    public static final class ThemeObserver implements Runnable,
            ViewTreeObserver.OnGlobalLayoutListener, View.OnAttachStateChangeListener {
        private final Activity host;
        private final View root;
        public ThemeObserver(Activity host, View root) { this.host = host; this.root = root; }
        public void run() {
            if (host.isDestroyed()) return;
            applyTree(root);
            // The original edge-to-edge setup can run after onCreate. Repeat the
            // scoped appearance after layout so clock/status icons remain readable.
            View decor = host.getWindow().getDecorView();
            decor.setSystemUiVisibility(decor.getSystemUiVisibility() & ~(8192 | 16));
        }
        public void onGlobalLayout() { run(); }
        public void onViewAttachedToWindow(View view) {}
        public void onViewDetachedFromWindow(View view) {
            if (view.getViewTreeObserver().isAlive())
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            view.removeOnAttachStateChangeListener(this);
        }
    }
}
