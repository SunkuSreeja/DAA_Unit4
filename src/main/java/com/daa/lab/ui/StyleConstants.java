package com.daa.lab.ui;

/**
 * Shared style constants used across all views and module cards.
 * All fields are {@code public static final} so they can be referenced
 * directly without instantiating this class.
 */
public final class StyleConstants {

    // ── Background colours ────────────────────────────────────────────────────
    public static final String BG_PRIMARY   = "#F8F9FA";
    public static final String BG_CARD      = "#FFFFFF";

    // ── Accent colours ────────────────────────────────────────────────────────
    public static final String ACCENT_BLUE  = "#2563EB";
    public static final String ACCENT_GREEN = "#16A34A";
    public static final String ACCENT_RED   = "#DC2626";
    public static final String ACCENT_AMBER = "#D97706";

    // ── Text colours ──────────────────────────────────────────────────────────
    public static final String TEXT_PRIMARY   = "#111827";
    public static final String TEXT_SECONDARY = "#6B7280";

    // ── Typography (pt / px sizes passed to setStyle) ─────────────────────────
    public static final double FONT_HEADING    = 22.0;
    public static final double FONT_SUBHEADING = 16.0;
    public static final double FONT_BODY       = 13.0;

    // ── Layout ────────────────────────────────────────────────────────────────
    public static final double RADIUS  = 12.0;
    public static final double PADDING = 20.0;
    public static final double GAP     = 16.0;

    // No instances
    private StyleConstants() {}
}
