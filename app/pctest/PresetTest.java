import com.jk.halostudio.core.*;
import com.jk.halostudio.core.Composer.Kind;
import com.jk.halostudio.core.Composer.Role;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import java.util.Arrays;
public class PresetTest {
    static String F = "faces/";
    static Composer.GlyphPainter painter(Presets.Spec s) {
        String fam = s.font.contains("serif") && !s.font.startsWith("sans") ? "Serif" : s.font.equals("monospace") ? "Monospaced" : "SansSerif";
        int style = (s.bold || s.font.contains("black") ? Font.BOLD : 0) | (s.italic ? Font.ITALIC : 0);
        return (role, idx, w, h) -> {
            String t = role == Role.WEEKDAY ? Composer.WEEKDAY_TEXT[idx] : role == Role.AMPM ? Composer.AMPM_TEXT[idx] : role == Role.SEP ? "/" : String.valueOf(idx);
            if (role == Role.WEEKDAY && idx < 7 && s.upper) t = t.toUpperCase();
            boolean time = role == Role.HOUR_T || role == Role.HOUR_U || role == Role.MIN_T || role == Role.MIN_U;
            BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = im.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            float size = h * 1.3f; Font f = new Font(fam, style, (int) size); g.setFont(f); FontMetrics fm = g.getFontMetrics();
            while ((fm.stringWidth(t) > w * 0.92 || fm.getAscent() * 0.75 > h * 0.9) && size > 6) { size--; f = f.deriveFont(size); g.setFont(f); fm = g.getFontMetrics(); }
            g.setColor(new Color(time ? s.time : s.date, true));
            g.drawString(t, (w - fm.stringWidth(t)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
            return im.getRGB(0, 0, w, h, null, 0, w);
        };
    }
    public static void main(String[] a) throws Exception {
        byte[] gal = Files.readAllBytes(Paths.get(F + "face_1246_type16_456460.raw"));
        byte[] moon = Files.readAllBytes(Paths.get(F + "face_898_type16_338386.raw"));
        int n = Presets.ALL.length, cols = 6, t = 180, ok = 0;
        BufferedImage sheet = new BufferedImage(cols * (t + 10), ((n + cols - 1) / cols) * (t + 26), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics(); g.setColor(new Color(24, 24, 28)); g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            Presets.Spec s = Presets.ALL[i];
            int[] bg = Art.render(s.art, s.pal, s.seed);
            byte[] tpl = s.layout == Kind.GALAXY ? gal : moon;
            byte[] raw = Composer.compose(s.layout, tpl, bg, s.time, s.date, s.tile, s.dx, s.dy, painter(s));
            if (Composer.verifyAllowed(s.layout, tpl, raw, false) == null && Arrays.equals(ZhDial.unpack(ZhDial.pack(raw)), raw)) ok++;
            int[] pv = Composer.preview(s.layout, raw, 10, s.layout == Kind.MOON, 8, 10, 30, 4, false);
            BufferedImage im = new BufferedImage(466, 466, BufferedImage.TYPE_INT_RGB); im.setRGB(0, 0, 466, 466, pv, 0, 466);
            int x = (i % cols) * (t + 10), y = (i / cols) * (t + 26);
            g.setClip(new java.awt.geom.Ellipse2D.Float(x, y, t, t)); g.drawImage(im, x, y, t, t, null); g.setClip(null);
            g.setColor(Color.WHITE); g.drawString(s.name, x + 4, y + t + 16);
        }
        System.out.println("presets valid (verifyAllowed + pack/unpack): " + ok + "/" + n + " in " + (System.currentTimeMillis() - t0) + " ms");
        ImageIO.write(sheet, "png", new File("out/preset_sheet.png"));
    }
}
