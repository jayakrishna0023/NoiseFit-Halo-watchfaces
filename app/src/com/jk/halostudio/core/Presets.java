package com.jk.halostudio.core;

/** Preinstalled faces: original procedural art + clock styling (no third-party artwork). */
public final class Presets {
    public static final String[] CATEGORIES = {"All", "Alien Tech", "Pro", "Cosmic", "Space", "Nature", "Luxury", "Cyber", "Neon", "Abstract", "Minimal", "Retro"};

    public static final class Spec {
        public final String name, category, font;
        public final int art; public final int[] pal; public final long seed;
        public final Composer.Kind layout;
        public final int time, time2, date, tile, glow, glowColor, outline;
        public final boolean grad, bold, italic, upper;
        public final int dx, dy;
        public int frame, frameColor, overlay;
        public boolean hideClock;
        Spec noClock() { this.hideClock = true; return this; }
        Spec fx(int frame, int frameColor, int overlay) { this.frame = frame; this.frameColor = frameColor; this.overlay = overlay; return this; }
        Spec(String name, String category, int art, int[] pal, long seed, Composer.Kind layout, String font, boolean bold, boolean italic,
             int time, int time2, boolean grad, int date, int tile, int glow, int glowColor, int outline, boolean upper, int dx, int dy) {
            this.name = name; this.category = category; this.art = art; this.pal = pal; this.seed = seed; this.layout = layout;
            this.font = font; this.bold = bold; this.italic = italic; this.time = time; this.time2 = time2; this.grad = grad;
            this.date = date; this.tile = tile; this.glow = glow; this.glowColor = glowColor; this.outline = outline; this.upper = upper;
            this.dx = dx; this.dy = dy;
        }
    }

    private static final Composer.Kind G = Composer.Kind.GALAXY, M = Composer.Kind.MOON;

    private static Spec g(String n, String c, int art, int[] pal, long seed, String font, int time, int date, int glow) {
        return new Spec(n, c, art, pal, seed, G, font, false, false, time, time, false, date, 0, glow, time, 0, false, 0, 0);
    }

    /** Like g() but with a dark outline so digits stay readable on busy art. */
    private static Spec o(String n, String c, int art, int[] pal, long seed, String font, int time, int date, int outline) {
        return new Spec(n, c, art, pal, seed, G, font, true, false, time, time, false, date, 0, 0, time, outline, false, 0, 0);
    }

    public static final Spec[] ALL = {
            // Space
            g("Violet Nebula", "Space", 0, new int[]{0xFF0B1026, 0xFF5B2A86, 0xFFD946EF, 0xFF22D3EE}, 3, "sans-serif-black", 0xFFFFFFFF, 0xFFE9D5FF, 8),
            g("Ocean Nebula", "Space", 0, new int[]{0xFF020617, 0xFF0E7490, 0xFF22D3EE, 0xFFA7F3D0}, 11, "sans-serif-condensed", 0xFFE0F2FE, 0xFF67E8F9, 6),
            g("Aurora", "Space", 1, new int[]{0xFF04131F, 0xFF10B981, 0xFF22D3EE, 0xFFA78BFA}, 3, "sans-serif-light", 0xFFFFFFFF, 0xFF6EE7B7, 0),
            g("Aurora Rose", "Space", 1, new int[]{0xFF120617, 0xFFF472B6, 0xFFA78BFA, 0xFF60A5FA}, 7, "sans-serif-light", 0xFFFDF2F8, 0xFFF9A8D4, 4),
            new Spec("Blue Giant", "Space", 4, new int[]{0xFF020617, 0xFF1E3A8A, 0xFF60A5FA, 0xFFE0F2FE}, 3, G, "sans-serif-black", false, false,
                    0xFFFFFFFF, 0xFF93C5FD, true, 0xFFBFDBFE, 0, 0, 0, 0, false, 0, 0),
            new Spec("Saturn Gold", "Space", 4, new int[]{0xFF0C0A09, 0xFF92400E, 0xFFF59E0B, 0xFFFEF3C7}, 9, G, "serif", true, false,
                    0xFFFEF3C7, 0xFFF59E0B, true, 0xFFFDE68A, 0, 0, 0, 0, false, 0, 0),
            g("Deep Starfield", "Space", 13, new int[]{0xFF0B1026, 0xFF6D28D9, 0xFFE9D5FF}, 3, "sans-serif-medium", 0xFFFFFFFF, 0xFFC4B5FD, 6),
            new Spec("Cosmic Tiles", "Space", 13, new int[]{0xFF1E1B4B, 0xFF7C3AED, 0xFFFDE68A}, 5, M, "sans-serif-condensed", true, false,
                    0xFFFDE68A, 0xFFFDE68A, false, 0xFFE9D5FF, 0xFF120E2E, 4, 0xFFFDE68A, 0, true, 0, 0),
            // Nature
            g("Desert Dunes", "Nature", 2, new int[]{0xFF2B1A12, 0xFFB45309, 0xFFF59E0B, 0xFFFDE68A}, 3, "serif", 0xFF1C1917, 0xFF292524, 0),
            new Spec("Dusk Dunes", "Nature", 2, new int[]{0xFF1E1B4B, 0xFF9D174D, 0xFFF472B6, 0xFFFBCFE8}, 8, G, "sans-serif-medium", true, false,
                    0xFFFFFFFF, 0xFFFFFFFF, false, 0xFFFFFFFF, 0, 8, 0xFF500724, 0, false, 0, 0),
            g("Ocean Waves", "Nature", 9, new int[]{0xFF082F49, 0xFF0369A1, 0xFF38BDF8, 0xFFE0F2FE}, 3, "sans-serif-black", 0xFFFFFFFF, 0xFF0C4A6E, 0),
            g("Lagoon", "Nature", 9, new int[]{0xFF042F2E, 0xFF0F766E, 0xFF2DD4BF, 0xFFCCFBF1}, 6, "casual", 0xFF042F2E, 0xFF134E4A, 0),
            g("Mint Crystal", "Nature", 8, new int[]{0xFF0F766E, 0xFF14B8A6, 0xFF5EEAD4, 0xFFCCFBF1}, 3, "sans-serif-black", 0xFFFFFFFF, 0xFF042F2E, 0),
            g("Forest Contours", "Nature", 7, new int[]{0xFF052E16, 0xFF14532D, 0xFF86EFAC}, 4, "sans-serif-condensed", 0xFFDCFCE7, 0xFF86EFAC, 0),
            // Neon
            new Spec("Synthwave", "Neon", 3, new int[]{0xFF12002B, 0xFF3B0764, 0xFFF472B6, 0xFFFDE047}, 3, G, "sans-serif-black", false, true,
                    0xFFFDE047, 0xFFF472B6, true, 0xFFF9A8D4, 0, 10, 0xFFF472B6, 2, true, 0, 0),
            new Spec("Cyber Grid", "Neon", 3, new int[]{0xFF020617, 0xFF0C4A6E, 0xFF22D3EE, 0xFFA78BFA}, 6, G, "monospace", true, false,
                    0xFF22D3EE, 0xFFA78BFA, true, 0xFF67E8F9, 0, 12, 0xFF22D3EE, 2, true, 0, 0),
            g("Neon Bokeh", "Neon", 5, new int[]{0xFF1F1033, 0xFFEC4899, 0xFF8B5CF6, 0xFF22D3EE}, 3, "sans-serif-black", 0xFFFFFFFF, 0xFFF0ABFC, 12),
            g("Candy Bokeh", "Neon", 5, new int[]{0xFF3B0764, 0xFFF97316, 0xFFF472B6, 0xFFFDE047}, 9, "casual", 0xFFFFFFFF, 0xFFFDE047, 8),
            new Spec("Laser Tiles", "Neon", 5, new int[]{0xFF0A0A0A, 0xFF22D3EE, 0xFFA3E635, 0xFFF472B6}, 4, M, "monospace", true, false,
                    0xFFA3E635, 0xFFA3E635, false, 0xFF22D3EE, 0xFF050505, 8, 0xFFA3E635, 0, true, 0, 0),
            // Abstract
            g("Candy Mesh", "Abstract", 6, new int[]{0xFF7C3AED, 0xFFEC4899, 0xFFF59E0B, 0xFF06B6D4, 0xFF10B981}, 3, "sans-serif-black", 0xFFFFFFFF, 0xFFFFFFFF, 4),
            g("Ocean Mesh", "Abstract", 6, new int[]{0xFF1E3A8A, 0xFF0EA5E9, 0xFF14B8A6, 0xFF6366F1}, 12, "sans-serif-light", 0xFFFFFFFF, 0xFFE0F2FE, 0),
            g("Noir Marble", "Abstract", 12, new int[]{0xFF0A0A0A, 0xFF1C1917, 0xFF44403C, 0xFF78716C}, 3, "serif", 0xFFF5D0A9, 0xFFF5D0A9, 0),
            g("Rose Marble", "Abstract", 12, new int[]{0xFF2A0410, 0xFF4C0519, 0xFF881337, 0xFFBE123C}, 5, "serif", 0xFFFFF1F2, 0xFFFDA4AF, 0),
            g("Ember Poly", "Abstract", 8, new int[]{0xFF450A0A, 0xFF991B1B, 0xFFF97316, 0xFFFDE047}, 7, "sans-serif-condensed", 0xFFFFFFFF, 0xFFFEF3C7, 0),
            new Spec("Pop Halftone", "Abstract", 14, new int[]{0xFF111827, 0xFFEC4899, 0xFFFDE68A}, 3, G, "sans-serif-black", false, false, 0xFFFFFFFF, 0xFFFFFFFF, false, 0xFFFFFFFF, 0, 0, 0, 3, false, 0, 0),
            // Minimal
            g("Black Cyan", "Minimal", 15, new int[]{0xFF0A0A0A, 0xFF22D3EE}, 3, "sans-serif-light", 0xFFFFFFFF, 0xFF22D3EE, 0),
            g("Paper", "Minimal", 15, new int[]{0xFFF5F5F4, 0xFF1C1917}, 3, "serif", 0xFF1C1917, 0xFF57534E, 0),
            g("Midnight Gold", "Minimal", 15, new int[]{0xFF0F172A, 0xFFF59E0B}, 5, "sans-serif-condensed", 0xFFF59E0B, 0xFFFDE68A, 0),
            g("Steel Rings", "Minimal", 10, new int[]{0xFF111827, 0xFF374151, 0xFF9CA3AF, 0xFFF59E0B}, 3, "sans-serif-black", 0xFFFFFFFF, 0xFFF59E0B, 0),
            g("Slate Contours", "Minimal", 7, new int[]{0xFF0F172A, 0xFF1E293B, 0xFF94A3B8}, 3, "sans-serif-light", 0xFFFFFFFF, 0xFF94A3B8, 0),
            // Retro
            g("Retro Sunburst", "Retro", 11, new int[]{0xFF7F1D1D, 0xFFDC2626, 0xFFFDBA74}, 3, "serif", 0xFFFFFBEB, 0xFFFFFBEB, 0),
            new Spec("Sunny Halftone", "Retro", 14, new int[]{0xFF1E3A8A, 0xFFF59E0B, 0xFFFEF3C7}, 5, G, "casual", true, false, 0xFFFFFFFF, 0xFFFFFFFF, false, 0xFFFFFFFF, 0, 0, 0, 3, false, 0, 0),

            // ---- v4: Cosmic
            g("Event Horizon", "Cosmic", 16, new int[]{0xFF05010F, 0xFF7C2D12, 0xFFF97316, 0xFFFDE68A}, 3, "sans-serif-light", 0xFFFFFFFF, 0xFFFDBA74, 10),
            g("Violet Singularity", "Cosmic", 16, new int[]{0xFF030014, 0xFF4C1D95, 0xFFD946EF, 0xFF67E8F9}, 7, "sans-serif-light", 0xFFFFFFFF, 0xFFE9D5FF, 12),
            g("Andromeda", "Cosmic", 23, new int[]{0xFF020617, 0xFF4338CA, 0xFFA5B4FC, 0xFFFEF9C3}, 3, "sans-serif-light", 0xFFFFFFFF, 0xFFC7D2FE, 8),
            g("Rose Spiral", "Cosmic", 23, new int[]{0xFF0F0716, 0xFF9D174D, 0xFFF9A8D4, 0xFFFFF1F2}, 9, "serif", 0xFFFFF1F2, 0xFFF9A8D4, 6),
            new Spec("Gold Galaxy", "Cosmic", 23, new int[]{0xFF0C0A09, 0xFF78350F, 0xFFFBBF24, 0xFFFFFBEB}, 5, G, "sans-serif-black", false, false,
                    0xFFFFFBEB, 0xFFFBBF24, true, 0xFFFDE68A, 0, 8, 0xFFF59E0B, 0, false, 0, 0),
            g("Moonlit Peaks", "Cosmic", 19, new int[]{0xFF0B1026, 0xFF312E81, 0xFF818CF8, 0xFFFEF9C3}, 3, "sans-serif-light", 0xFFFFFFFF, 0xFFC7D2FE, 4),
            g("Nebula Ice", "Cosmic", 0, new int[]{0xFF020617, 0xFF1E40AF, 0xFF7DD3FC, 0xFFF0F9FF}, 21, "sans-serif-light", 0xFFFFFFFF, 0xFFBAE6FD, 10),
            new Spec("Orbit Tiles", "Cosmic", 16, new int[]{0xFF020617, 0xFF0E7490, 0xFF22D3EE, 0xFFF0F9FF}, 11, M, "sans-serif-condensed", true, false,
                    0xFF67E8F9, 0xFF67E8F9, false, 0xFFE0F2FE, 0xFF02101A, 6, 0xFF22D3EE, 0, true, 0, 0),
            // ---- Nature (new)
            g("Alpine Dawn", "Nature", 19, new int[]{0xFF1E1B4B, 0xFFBE185D, 0xFFFB923C, 0xFFFEF3C7}, 5, "sans-serif-medium", 0xFFFFFFFF, 0xFFFED7AA, 0),
            g("Misty Pines", "Nature", 19, new int[]{0xFF0F172A, 0xFF115E59, 0xFF99F6E4, 0xFFF0FDFA}, 8, "serif", 0xFFF0FDFA, 0xFF99F6E4, 0),
            g("Firefly Night", "Nature", 27, new int[]{0xFF020617, 0xFF14532D, 0xFF4ADE80, 0xFFFDE047}, 3, "sans-serif-light", 0xFFFFFFFF, 0xFFFDE047, 8),
            g("Ember Forest", "Nature", 27, new int[]{0xFF1C0505, 0xFF9A3412, 0xFFFB923C, 0xFFFDE68A}, 6, "serif", 0xFFFFFBEB, 0xFFFDBA74, 6),
            g("Golden Hour Sea", "Nature", 28, new int[]{0xFF1E1B4B, 0xFFC2410C, 0xFFFB923C, 0xFFFEF3C7}, 3, "sans-serif-black", 0xFFFFFFFF, 0xFFFFEDD5, 0),
            g("Pink Tide", "Nature", 28, new int[]{0xFF1E1B4B, 0xFF9D174D, 0xFFF472B6, 0xFFFFF1F2}, 4, "sans-serif-light", 0xFFFFFFFF, 0xFFFBCFE8, 4),
            g("Moon Sea", "Nature", 28, new int[]{0xFF020617, 0xFF1E3A8A, 0xFF64748B, 0xFFF8FAFC}, 9, "serif", 0xFFF8FAFC, 0xFFCBD5E1, 0),
            // ---- Luxury
            new Spec("Black Gold", "Luxury", 12, new int[]{0xFF050505, 0xFF1C1917, 0xFF3F3F46, 0xFFB45309}, 7, G, "serif", true, false,
                    0xFFFDE68A, 0xFFB45309, true, 0xFFFCD34D, 0, 6, 0xFFB45309, 0, true, 0, 0),
            o("Emerald Marble", "Luxury", 12, new int[]{0xFF022C22, 0xFF064E3B, 0xFF10B981, 0xFFD1FAE5}, 4, "serif", 0xFFFEF3C7, 0xFFFDE68A, 3),
            o("Pearl", "Luxury", 17, new int[]{0xFFE2E8F0, 0xFFF5D0FE, 0xFFBAE6FD, 0xFFFFFFFF}, 3, "serif", 0xFFFFFFFF, 0xFFFFFFFF, 3),
            new Spec("Liquid Chrome", "Luxury", 17, new int[]{0xFF0A0A0A, 0xFF52525B, 0xFFA1A1AA, 0xFFF4F4F5}, 5, G, "sans-serif-black", false, false,
                    0xFFFFFFFF, 0xFFA1A1AA, true, 0xFFE4E4E7, 0, 6, 0xFF000000, 2, true, 0, 0),
            o("Sapphire Facets", "Luxury", 20, new int[]{0xFF0B1437, 0xFF1D4ED8, 0xFF60A5FA, 0xFFE0F2FE}, 3, "serif", 0xFFFFFFFF, 0xFFBFDBFE, 3),
            o("Ruby Facets", "Luxury", 20, new int[]{0xFF1F0208, 0xFF9F1239, 0xFFFB7185, 0xFFFFE4E6}, 8, "serif", 0xFFFFFFFF, 0xFFFECDD3, 3),
            new Spec("Amethyst Tiles", "Luxury", 20, new int[]{0xFF1E0A3C, 0xFF6D28D9, 0xFFC4B5FD, 0xFFF5F3FF}, 12, M, "serif", true, false,
                    0xFFF5F3FF, 0xFFF5F3FF, false, 0xFFDDD6FE, 0xFF140726, 4, 0xFFC4B5FD, 0, true, 0, 0),
            g("Champagne Glass", "Luxury", 22, new int[]{0xFF292524, 0xFFD6A35C, 0xFFF5E1C0, 0xFFFDE68A}, 5, "serif", 0xFFFFFFFF, 0xFFFFF7ED, 4),
            // ---- Cyber
            new Spec("Neon Tunnel", "Cyber", 21, new int[]{0xFF09001A, 0xFF4C1D95, 0xFFEC4899, 0xFF22D3EE}, 3, G, "monospace", true, false,
                    0xFFFFFFFF, 0xFF22D3EE, true, 0xFFF0ABFC, 0, 12, 0xFFEC4899, 2, true, 0, 0),
            new Spec("Acid Tunnel", "Cyber", 21, new int[]{0xFF020403, 0xFF14532D, 0xFFA3E635, 0xFFFDE047}, 6, G, "monospace", true, false,
                    0xFFA3E635, 0xFFFDE047, true, 0xFFD9F99D, 0, 10, 0xFFA3E635, 2, true, 0, 0),
            new Spec("Mainframe", "Cyber", 26, new int[]{0xFF000000, 0xFF14532D, 0xFF4ADE80}, 3, G, "monospace", true, false,
                    0xFFDCFCE7, 0xFF4ADE80, true, 0xFF86EFAC, 0, 10, 0xFF22C55E, 2, true, 0, 0),
            new Spec("Ice Code", "Cyber", 26, new int[]{0xFF000308, 0xFF0C4A6E, 0xFF67E8F9}, 7, G, "monospace", true, false,
                    0xFFFFFFFF, 0xFF67E8F9, true, 0xFFA5F3FC, 0, 10, 0xFF22D3EE, 2, true, 0, 0),
            new Spec("Circuit Board", "Cyber", 24, new int[]{0xFF021714, 0xFF0F3B33, 0xFF2DD4BF, 0xFFFDE047}, 3, G, "monospace", true, false,
                    0xFFFFFFFF, 0xFF5EEAD4, true, 0xFF99F6E4, 0, 8, 0xFF021714, 3, true, 0, 0),
            new Spec("Red Circuit", "Cyber", 24, new int[]{0xFF0A0000, 0xFF450A0A, 0xFFEF4444, 0xFFFDE68A}, 5, M, "monospace", true, false,
                    0xFFFCA5A5, 0xFFFCA5A5, false, 0xFFFECACA, 0xFF0A0000, 8, 0xFFEF4444, 0, true, 0, 0),
            g("Hologram", "Cyber", 17, new int[]{0xFF1E1B4B, 0xFF22D3EE, 0xFFF0ABFC, 0xFFFDE68A, 0xFFA7F3D0}, 9, "sans-serif-black", 0xFFFFFFFF, 0xFFFFFFFF, 6),
            // ---- Abstract (new)
            g("Gold Flow", "Abstract", 18, new int[]{0xFF0C0A09, 0xFF78350F, 0xFFF59E0B, 0xFFFEF3C7}, 3, "serif", 0xFFFFFFFF, 0xFFFDE68A, 6),
            g("Aqua Flow", "Abstract", 18, new int[]{0xFF020617, 0xFF0E7490, 0xFF22D3EE, 0xFFECFEFF}, 7, "sans-serif-light", 0xFFFFFFFF, 0xFFA5F3FC, 6),
            g("Sakura Flow", "Abstract", 18, new int[]{0xFF1A0510, 0xFF9D174D, 0xFFF9A8D4, 0xFFFFF1F2}, 4, "serif", 0xFFFFFFFF, 0xFFFBCFE8, 6),
            g("Mandala", "Abstract", 25, new int[]{0xFF0B0420, 0xFF7C3AED, 0xFFF472B6, 0xFFFDE68A}, 3, "sans-serif-black", 0xFFFFFFFF, 0xFFFDE68A, 10),
            g("Ocean Mandala", "Abstract", 25, new int[]{0xFF020617, 0xFF0369A1, 0xFF38BDF8, 0xFFE0F2FE}, 6, "sans-serif-light", 0xFFFFFFFF, 0xFFBAE6FD, 10),
            g("Frosted Glass", "Abstract", 22, new int[]{0xFF1E1B4B, 0xFF8B5CF6, 0xFFEC4899, 0xFF22D3EE}, 3, "sans-serif-medium", 0xFFFFFFFF, 0xFFFFFFFF, 4),
            g("Citrus Glass", "Abstract", 22, new int[]{0xFF14532D, 0xFFFACC15, 0xFFF97316, 0xFF4ADE80}, 8, "sans-serif-black", 0xFFFFFFFF, 0xFFFFFFFF, 6),
            g("Lava Liquid", "Abstract", 17, new int[]{0xFF1C0303, 0xFF991B1B, 0xFFF97316, 0xFFFDE047}, 6, "sans-serif-black", 0xFFFFFFFF, 0xFFFEF3C7, 8),
            // ---- Retro / Minimal (new)
            new Spec("Bauhaus", "Retro", 29, new int[]{0xFFF5F0E6, 0xFF1D3557, 0xFFE63946, 0xFFF4A261}, 3, G, "sans-serif-black", false, false,
                    0xFF111111, 0xFF111111, false, 0xFF111111, 0, 0, 0, 0, true, 0, 0),
            new Spec("Bauhaus Night", "Retro", 29, new int[]{0xFF111827, 0xFF6D28D9, 0xFFDDD6FE, 0xFF0F172A}, 7, G, "sans-serif-black", false, false,
                    0xFFFFFFFF, 0xFFFFFFFF, false, 0xFFFFFFFF, 0, 0, 0, 5, true, 0, 0),
            new Spec("Mono Grid", "Minimal", 21, new int[]{0xFF000000, 0xFF111111, 0xFF52525B, 0xFFE4E4E7}, 3, G, "sans-serif-light", false, false,
                    0xFFFFFFFF, 0xFFFFFFFF, false, 0xFFA1A1AA, 0, 0, 0, 0, true, 0, 0),
            g("Quiet Peaks", "Minimal", 19, new int[]{0xFF111827, 0xFF374151, 0xFF9CA3AF, 0xFFF3F4F6}, 4, "sans-serif-light", 0xFFFFFFFF, 0xFFD1D5DB, 0),
            // ---- v5: framed & textured
            g("Chronograph", "Luxury", 12, new int[]{0xFF0A0A0A, 0xFF18181B, 0xFF27272A, 0xFF52525B}, 9, "sans-serif-condensed", 0xFFFFFFFF, 0xFFF59E0B, 0).fx(3, 0xFFF59E0B, 1),
            g("Royal Navy", "Luxury", 15, new int[]{0xFF0B1437, 0xFFFDE68A}, 4, "serif", 0xFFFDE68A, 0xFFFDE68A, 0).fx(4, 0xFFFDE68A, 1),
            g("Rose Gold", "Luxury", 17, new int[]{0xFF3B1B1B, 0xFFB76E79, 0xFFF4C2C2, 0xFFFFF1F2}, 12, "serif", 0xFFFFFFFF, 0xFFFFE4E6, 6).fx(1, 0xFFF4C2C2, 3),
            g("Neon Halo", "Cyber", 23, new int[]{0xFF020617, 0xFF7C3AED, 0xFFF0ABFC, 0xFFFFFFFF}, 13, "sans-serif-light", 0xFFFFFFFF, 0xFFF0ABFC, 10).fx(2, 0xFFEC4899, 0),
            g("CRT Terminal", "Cyber", 26, new int[]{0xFF000000, 0xFF7C2D12, 0xFFFBBF24}, 9, "monospace", 0xFFFBBF24, 0xFFFDE68A, 10).fx(1, 0xFFFBBF24, 2),
            g("Synth Scan", "Retro", 3, new int[]{0xFF0B0221, 0xFF581C87, 0xFF22D3EE, 0xFFFB7185}, 11, "sans-serif-black", 0xFFFFFFFF, 0xFF67E8F9, 10).fx(0, 0, 2),
            g("Polaroid Dusk", "Retro", 28, new int[]{0xFF2E1065, 0xFFDB2777, 0xFFFDBA74, 0xFFFFF7ED}, 14, "serif", 0xFFFFFFFF, 0xFFFFEDD5, 0).fx(0, 0, 1),
            g("Pilot", "Minimal", 15, new int[]{0xFF111111, 0xFFFFFFFF}, 6, "sans-serif-condensed", 0xFFFFFFFF, 0xFFA3A3A3, 0).fx(3, 0xFFFFFFFF, 0),
            g("Glass Lilac", "Minimal", 22, new int[]{0xFF2E1065, 0xFFC4B5FD, 0xFFF5D0FE, 0xFFE0E7FF}, 15, "sans-serif-light", 0xFFFFFFFF, 0xFFFFFFFF, 6).fx(0, 0, 3),
            g("Dot Matrix", "Minimal", 15, new int[]{0xFF0A0A0A, 0xFFEF4444}, 7, "monospace", 0xFFFFFFFF, 0xFFEF4444, 0).fx(1, 0xFFEF4444, 4),
            g("Saturn Night", "Cosmic", 4, new int[]{0xFF050314, 0xFF3730A3, 0xFFA78BFA, 0xFFF5F3FF}, 17, "sans-serif-light", 0xFFFFFFFF, 0xFFDDD6FE, 8).fx(1, 0xFFA78BFA, 0),
            g("Supernova", "Cosmic", 16, new int[]{0xFF000000, 0xFF0F766E, 0xFF5EEAD4, 0xFFFFFFFF}, 21, "sans-serif-black", 0xFFFFFFFF, 0xFF99F6E4, 12).fx(2, 0xFF2DD4BF, 0),
            g("Northern Glass", "Nature", 1, new int[]{0xFF020617, 0xFF22C55E, 0xFF06B6D4, 0xFFC084FC}, 19, "sans-serif-light", 0xFFFFFFFF, 0xFFA7F3D0, 4).fx(0, 0, 3),
            g("Koi Pond", "Nature", 17, new int[]{0xFF042F2E, 0xFF0F766E, 0xFFF97316, 0xFFFFEDD5}, 23, "serif", 0xFFFFFFFF, 0xFFFFEDD5, 6),
            g("Opal", "Abstract", 17, new int[]{0xFFF0F9FF, 0xFFA5F3FC, 0xFFF5D0FE, 0xFFFEF9C3, 0xFFBBF7D0}, 31, "sans-serif-black", 0xFF0F172A, 0xFF1E293B, 0),
            g("Prism Kaleido", "Abstract", 25, new int[]{0xFF1E1B4B, 0xFF06B6D4, 0xFFF472B6, 0xFFFDE047, 0xFFA3E635}, 12, "sans-serif-black", 0xFFFFFFFF, 0xFFFFFFFF, 12),
            // ---- v6: Alien Tech (original sci-fi gauntlet dials, one colour per generation)
            new Spec("Xeno Prime", "Alien Tech", 30, new int[]{0xFF050805, 0xFF1F2A1F, 0xFF4ADE80, 0xFFA3E635}, 3, G, "sans-serif-black", false, false,
                    0xFFECFCCB, 0xFFA3E635, true, 0xFFA3E635, 0, 12, 0xFF84CC16, 0, true, 0, 0),
            new Spec("Xeno Nova", "Alien Tech", 30, new int[]{0xFF0A0303, 0xFF2A1010, 0xFFEF4444, 0xFFFB7185}, 5, G, "sans-serif-black", false, false,
                    0xFFFFE4E6, 0xFFFB7185, true, 0xFFFDA4AF, 0, 12, 0xFFEF4444, 0, true, 0, 0),
            new Spec("Xeno Cryo", "Alien Tech", 30, new int[]{0xFF02060A, 0xFF10202A, 0xFF22D3EE, 0xFF67E8F9}, 7, G, "sans-serif-black", false, false,
                    0xFFECFEFF, 0xFF67E8F9, true, 0xFFA5F3FC, 0, 12, 0xFF06B6D4, 0, true, 0, 0),
            new Spec("Xeno Elite", "Alien Tech", 30, new int[]{0xFF0A0703, 0xFF2A2010, 0xFFF59E0B, 0xFFFDE68A}, 9, G, "serif", true, false,
                    0xFFFFFBEB, 0xFFFDE68A, true, 0xFFFDE68A, 0, 10, 0xFFF59E0B, 0, true, 0, 0),
            new Spec("Xeno Void", "Alien Tech", 30, new int[]{0xFF06030C, 0xFF1E1030, 0xFFA855F7, 0xFFE879F9}, 11, G, "sans-serif-black", false, false,
                    0xFFFAF5FF, 0xFFE879F9, true, 0xFFF0ABFC, 0, 12, 0xFFA855F7, 0, true, 0, 0),
            new Spec("Scanner Lime", "Alien Tech", 31, new int[]{0xFF020702, 0xFF166534, 0xFF84CC16}, 3, G, "monospace", true, false,
                    0xFFD9F99D, 0xFFD9F99D, false, 0xFFA3E635, 0, 10, 0xFF65A30D, 0, true, 0, 0),
            new Spec("Scanner Ice", "Alien Tech", 31, new int[]{0xFF01060A, 0xFF0E7490, 0xFF22D3EE}, 6, G, "monospace", true, false,
                    0xFFCFFAFE, 0xFFCFFAFE, false, 0xFF67E8F9, 0, 10, 0xFF0891B2, 0, true, 0, 0),
            new Spec("Scanner Ember", "Alien Tech", 31, new int[]{0xFF0A0402, 0xFF9A3412, 0xFFFB923C}, 8, G, "monospace", true, false,
                    0xFFFFEDD5, 0xFFFFEDD5, false, 0xFFFDBA74, 0, 10, 0xFFEA580C, 0, true, 0, 0),
            new Spec("Reactor Green", "Alien Tech", 32, new int[]{0xFF020802, 0xFF14532D, 0xFF22C55E, 0xFFBEF264}, 3, G, "sans-serif-black", false, false,
                    0xFFFFFFFF, 0xFFBEF264, true, 0xFFD9F99D, 0, 10, 0xFF22C55E, 3, true, 0, 0),
            new Spec("Reactor Plasma", "Alien Tech", 32, new int[]{0xFF04020A, 0xFF4C1D95, 0xFFEC4899, 0xFFF0ABFC}, 5, G, "sans-serif-black", false, false,
                    0xFFFFFFFF, 0xFFF0ABFC, true, 0xFFF5D0FE, 0, 10, 0xFFEC4899, 3, true, 0, 0),
            new Spec("Reactor Sun", "Alien Tech", 32, new int[]{0xFF0A0500, 0xFF9A3412, 0xFFFBBF24, 0xFFFFFBEB}, 9, G, "sans-serif-black", false, false,
                    0xFFFFFFFF, 0xFFFDE68A, true, 0xFFFEF3C7, 0, 10, 0xFFF59E0B, 3, true, 0, 0),
            new Spec("Xeno Tiles", "Alien Tech", 30, new int[]{0xFF050805, 0xFF1F2A1F, 0xFF4ADE80, 0xFFA3E635}, 13, M, "monospace", true, false,
                    0xFFA3E635, 0xFFA3E635, false, 0xFFD9F99D, 0xFF0A120A, 8, 0xFF84CC16, 0, true, 0, 0),
            new Spec("Gauntlet Prime", "Alien Tech", 33, new int[]{0xFF040804, 0xFF1A2E1A, 0xFF16A34A, 0xFF4ADE80}, 3, G, "sans-serif-black", false, false,
                    0xFFF0FDF4, 0xFF4ADE80, true, 0xFFBBF7D0, 0x0, 12, 0xFF22C55E, 2, true, 0, 0),
            new Spec("Gauntlet Omega", "Alien Tech", 33, new int[]{0xFF030303, 0xFF262626, 0xFF65A30D, 0xFFBEF264}, 5, G, "sans-serif-black", false, false,
                    0xFFFFFFFF, 0xFFBEF264, true, 0xFFD9F99D, 0x0, 12, 0xFF84CC16, 2, true, 0, 0),
            new Spec("Gauntlet Crimson", "Alien Tech", 33, new int[]{0xFF080202, 0xFF2E1414, 0xFFDC2626, 0xFFFCA5A5}, 7, G, "sans-serif-black", false, false,
                    0xFFFFF1F2, 0xFFFCA5A5, true, 0xFFFECACA, 0x0, 12, 0xFFEF4444, 2, true, 0, 0),
            new Spec("Gauntlet Azure", "Alien Tech", 33, new int[]{0xFF02040A, 0xFF141E2E, 0xFF2563EB, 0xFF93C5FD}, 9, G, "sans-serif-black", false, false,
                    0xFFEFF6FF, 0xFF93C5FD, true, 0xFFBFDBFE, 0x0, 12, 0xFF3B82F6, 2, true, 0, 0),
            new Spec("Gauntlet Royal", "Alien Tech", 33, new int[]{0xFF050308, 0xFF221A2E, 0xFF7C3AED, 0xFFFDE68A}, 11, G, "serif", false, false,
                    0xFFFFFBEB, 0xFFFDE68A, true, 0xFFFDE68A, 0x0, 10, 0xFFF59E0B, 2, true, 0, 0),
            new Spec("Gauntlet Shadow", "Alien Tech", 33, new int[]{0xFF000000, 0xFF18181B, 0xFF52525B, 0xFFF4F4F5}, 13, G, "sans-serif-condensed", false, false,
                    0xFFFFFFFF, 0xFFA1A1AA, true, 0xFFE4E4E7, 0x0, 8, 0xFFFFFFFF, 2, true, 0, 0),
            new Spec("DNA Lime", "Alien Tech", 34, new int[]{0xFF020702, 0xFF14532D, 0xFF4ADE80, 0xFFD9F99D}, 3, G, "monospace", false, false,
                    0xFFF7FEE7, 0xFFD9F99D, true, 0xFFBEF264, 0x0, 10, 0xFF65A30D, 3, true, 0, 0),
            new Spec("DNA Plasma", "Alien Tech", 34, new int[]{0xFF06020C, 0xFF4C1D95, 0xFFF472B6, 0xFF67E8F9}, 5, G, "monospace", false, false,
                    0xFFFFFFFF, 0xFF67E8F9, true, 0xFFF5D0FE, 0x0, 10, 0xFFA855F7, 3, true, 0, 0),
            new Spec("DNA Ice", "Alien Tech", 34, new int[]{0xFF01060A, 0xFF0C4A6E, 0xFF38BDF8, 0xFFE0F2FE}, 7, G, "monospace", false, false,
                    0xFFFFFFFF, 0xFFBAE6FD, true, 0xFFBAE6FD, 0x0, 10, 0xFF0EA5E9, 3, true, 0, 0),
            new Spec("Burst Verdant", "Alien Tech", 35, new int[]{0xFF010401, 0xFF14532D, 0xFF22C55E, 0xFFECFCCB}, 3, G, "sans-serif-black", false, false,
                    0xFFFFFFFF, 0xFFECFCCB, true, 0xFFD9F99D, 0x0, 12, 0xFF16A34A, 3, true, 0, 0),
            new Spec("Burst Solar", "Alien Tech", 35, new int[]{0xFF080300, 0xFF9A3412, 0xFFF59E0B, 0xFFFFFBEB}, 5, G, "sans-serif-black", false, false,
                    0xFFFFFFFF, 0xFFFDE68A, true, 0xFFFEF3C7, 0x0, 12, 0xFFEA580C, 3, true, 0, 0),
            new Spec("Burst Nova", "Alien Tech", 35, new int[]{0xFF02010A, 0xFF3730A3, 0xFF818CF8, 0xFFF5F3FF}, 7, G, "sans-serif-black", false, false,
                    0xFFFFFFFF, 0xFFC7D2FE, true, 0xFFE0E7FF, 0x0, 12, 0xFF6366F1, 3, true, 0, 0),
            new Spec("Burst Toxic", "Alien Tech", 35, new int[]{0xFF030602, 0xFF365314, 0xFFA3E635, 0xFFFEF9C3}, 9, G, "monospace", false, false,
                    0xFFFFFFFF, 0xFFFEF9C3, true, 0xFFECFCCB, 0x0, 12, 0xFF84CC16, 3, true, 0, 0),
            new Spec("Gauntlet Tiles", "Alien Tech", 33, new int[]{0xFF040804, 0xFF1A2E1A, 0xFF16A34A, 0xFF4ADE80}, 15, M, "monospace", false, false,
                    0xFF4ADE80, 0xFF4ADE80, true, 0xFFBBF7D0, 0xFF06110A, 8, 0xFF22C55E, 0, true, 0, 0),
            new Spec("DNA Tiles", "Alien Tech", 34, new int[]{0xFF06020C, 0xFF4C1D95, 0xFFF472B6, 0xFF67E8F9}, 17, M, "monospace", false, false,
                    0xFF67E8F9, 0xFF67E8F9, true, 0xFFF5D0FE, 0xFF0C0618, 8, 0xFFA855F7, 0, true, 0, 0),
            // ---- v10: art-only (no clock) + Pro
            g("Gauntlet Core · art only", "Alien Tech", 33, new int[]{0xFF040804, 0xFF1A2E1A, 0xFF16A34A, 0xFF4ADE80}, 21, "sans-serif-black", 0xFFFFFFFF, 0xFFFFFFFF, 0).noClock(),
            g("Omega Core · art only", "Alien Tech", 33, new int[]{0xFF030303, 0xFF262626, 0xFF65A30D, 0xFFBEF264}, 23, "sans-serif-black", 0xFFFFFFFF, 0xFFFFFFFF, 0).noClock(),
            g("Burst Core · art only", "Alien Tech", 35, new int[]{0xFF010401, 0xFF14532D, 0xFF22C55E, 0xFFECFCCB}, 25, "sans-serif-black", 0xFFFFFFFF, 0xFFFFFFFF, 0).noClock(),
            g("Xeno Core · art only", "Alien Tech", 30, new int[]{0xFF050805, 0xFF1F2A1F, 0xFF4ADE80, 0xFFA3E635}, 27, "sans-serif-black", 0xFFFFFFFF, 0xFFFFFFFF, 0).noClock(),
            g("Pro Gradient", "Pro", 6, new int[]{0xFF1E1B4B, 0xFF7C3AED, 0xFFEC4899, 0xFFF59E0B}, 41, "sans-serif-light", 0xFFFFFFFF, 0xFFFFFFFF, 0).fx(3, 0xCCFFFFFF, 0),
            g("Pro Graphite", "Pro", 15, new int[]{0xFF111214, 0xFFF97316}, 9, "sans-serif-condensed", 0xFFFFFFFF, 0xFFF97316, 0).fx(3, 0xFF6B7280, 1),
            g("Pro Sunrise", "Pro", 6, new int[]{0xFFFB7185, 0xFFF59E0B, 0xFFFDE68A, 0xFFF97316}, 43, "sans-serif-medium", 0xFFFFFFFF, 0xFFFFFFFF, 0).fx(1, 0xAAFFFFFF, 3),
            g("Pro Ocean", "Pro", 6, new int[]{0xFF020617, 0xFF0C4A6E, 0xFF1D4ED8, 0xFF0E7490}, 45, "sans-serif-light", 0xFFFFFFFF, 0xFFBAE6FD, 4).fx(4, 0xFF7DD3FC, 0),
            g("Pro Chrono Black", "Pro", 6, new int[]{0xFF050505, 0xFF18181B, 0xFF27272A, 0xFF0A0A0A}, 47, "sans-serif-condensed", 0xFFFFFFFF, 0xFFEF4444, 0).fx(3, 0xFFEF4444, 0),
            g("Pro Mint", "Pro", 22, new int[]{0xFF052E2B, 0xFF14B8A6, 0xFF99F6E4, 0xFFF0FDFA}, 49, "sans-serif-light", 0xFFFFFFFF, 0xFFFFFFFF, 6).fx(1, 0x99FFFFFF, 3),
    };

    private Presets() {}
}
