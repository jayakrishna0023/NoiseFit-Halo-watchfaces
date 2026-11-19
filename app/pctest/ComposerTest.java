import com.jk.halostudio.core.*;
import com.jk.halostudio.core.Composer.Kind;
import com.jk.halostudio.core.Composer.Role;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class ComposerTest {
    static String F = "faces/";
    static int fails = 0;
    static void check(String n, boolean ok) { System.out.println((ok ? "PASS  " : "FAIL  ") + n); if (!ok) fails++; }

    static void save(int[] argb, String name) throws Exception {
        BufferedImage im = new BufferedImage(466, 466, BufferedImage.TYPE_INT_ARGB);
        im.setRGB(0, 0, 466, 466, argb, 0, 466);
        BufferedImage round = new BufferedImage(466, 466, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = round.createGraphics();
        g.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, 466, 466));
        g.drawImage(im, 0, 0, null);
        ImageIO.write(round, "png", new File(F + name));
    }

    static int[] bgOf(byte[] t) {
        int[] b = new int[466 * 466];
        for (int i = 0; i < b.length; i++) b[i] = Composer.argb(((t[Composer.BG_OFF + 2 * i] & 0xFF) << 8) | (t[Composer.BG_OFF + 2 * i + 1] & 0xFF));
        return b;
    }

    static int[] photo(String p) throws Exception {
        BufferedImage ph = ImageIO.read(new File(p));
        int m = Math.min(ph.getWidth(), ph.getHeight());
        BufferedImage sq = new BufferedImage(466, 466, BufferedImage.TYPE_INT_RGB);
        sq.getGraphics().drawImage(ph, 0, 0, 466, 466, (ph.getWidth() - m) / 2, (ph.getHeight() - m) / 2, (ph.getWidth() + m) / 2, (ph.getHeight() + m) / 2, null);
        return sq.getRGB(0, 0, 466, 466, null, 0, 466);
    }

    /** Java2D stand-in for the Android font painter. */
    static Composer.GlyphPainter painter(String family, int style, int timeCol, int dateCol) {
        return (role, idx, w, h) -> {
            String s;
            switch (role) {
                case WEEKDAY: s = Composer.WEEKDAY_TEXT[idx]; break;
                case AMPM: s = Composer.AMPM_TEXT[idx]; break;
                case SEP: s = "/"; break;
                default: s = String.valueOf(idx);
            }
            boolean time = role == Role.HOUR_T || role == Role.HOUR_U || role == Role.MIN_T || role == Role.MIN_U;
            BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = im.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            float size = h;
            Font f = new Font(family, style, (int) size);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            while ((fm.stringWidth(s) > w * 0.94 || fm.getAscent() * 0.78 > h * 0.92) && size > 6) { size -= 1; f = f.deriveFont(size); g.setFont(f); fm = g.getFontMetrics(); }
            g.setColor(new Color(time ? timeCol : dateCol, true));
            g.drawString(s, (w - fm.stringWidth(s)) / 2, (h + fm.getAscent() - fm.getDescent()) / 2);
            g.dispose();
            return im.getRGB(0, 0, w, h, null, 0, w);
        };
    }

    public static void main(String[] a) throws Exception {
        byte[] gal = Files.readAllBytes(Paths.get(F + "face_1246_type16_456460.raw"));
        byte[] moon = Files.readAllBytes(Paths.get(F + "face_898_type16_338386.raw"));

        check("identify galaxy template", Composer.identify(gal) == Kind.GALAXY);
        check("identify moon template", Composer.identify(moon) == Kind.MOON);
        check("identify rejects junk", Composer.identify(Arrays.copyOf(gal, 300000)) == null);

        byte[] id = Composer.compose(Kind.GALAXY, gal, bgOf(gal), 0xFFFFFFFF, 0xFFFFFFFF, 0, 0, 0, null);
        check("galaxy identity: header/records/thumbnail untouched", Arrays.equals(Arrays.copyOf(gal, Composer.BG_OFF), Arrays.copyOf(id, Composer.BG_OFF)));

        int[] ph = photo(a[0]);
        Composer.GlyphPainter p = painter("Serif", Font.BOLD, 0xFFFFE08A, 0xFFFFFFFF);
        byte[] g1 = Composer.compose(Kind.GALAXY, gal, ph, 0xFFFFE08A, 0xFFFFFFFF, 0, -30, 20, p);
        int[] pv = Composer.preview(Kind.GALAXY, g1, 3, false, 37, 10, 30, 4, false);
        // thumbnail: downscale preview to 310 with round mask
        BufferedImage big = new BufferedImage(466, 466, BufferedImage.TYPE_INT_RGB); big.setRGB(0, 0, 466, 466, pv, 0, 466);
        BufferedImage th = new BufferedImage(310, 310, BufferedImage.TYPE_INT_RGB);
        Graphics2D tg = th.createGraphics();
        tg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        tg.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, 310, 310)); tg.drawImage(big, 0, 0, 310, 310, null);
        Composer.writeThumbnail(g1, th.getRGB(0, 0, 310, 310, null, 0, 310));
        check("custom font + thumbnail: verifyAllowed OK", Composer.verifyAllowed(Kind.GALAXY, gal, g1, true) == null);
        check("thumbnail change is refused when not allowed", Composer.verifyAllowed(Kind.GALAXY, gal, g1, false) != null);
        check("custom font face packs + validates", Arrays.equals(ZhDial.unpack(ZhDial.pack(g1)), g1));
        save(pv, "studio2_galaxy_serif.png");
        ImageIO.write(th, "png", new File(F + "studio2_thumb.png"));

        Composer.GlyphPainter p2 = painter("SansSerif", Font.PLAIN, 0xFF7DD3FC, 0xFFF0ABFC);
        byte[] m1 = Composer.compose(Kind.MOON, moon, ph, 0xFF7DD3FC, 0xFFF0ABFC, 0xFF0B1020, 0, 0, p2);
        check("moon custom font: verifyAllowed OK", Composer.verifyAllowed(Kind.MOON, moon, m1, false) == null);
        check("moon custom font packs + validates", Arrays.equals(ZhDial.unpack(ZhDial.pack(m1)), m1));
        save(Composer.preview(Kind.MOON, m1, 15, true, 37, 10, 30, 4, false), "studio2_moon_sans.png");
        save(Composer.preview(Kind.MOON, m1, 15, true, 37, 10, 30, 4, true), "studio2_moon_chinese.png");

        // AOD codec
        Aod x = new Aod(); x.mode = Aod.MODE_SCHEDULED; x.style = Aod.STYLE_DIGITAL;
        x.hasStart = true; x.sh = 22; x.sm = 30; x.hasEnd = true; x.eh = 7; x.em = 0;
        byte[] inner = x.encodeInner();
        // simulate a watch reply: 08 f1 01 | 72 { 62 { inner } }
        byte[] menu = cat(new byte[]{0x62, (byte) inner.length}, inner);
        byte[] reply = cat(new byte[]{0x08, (byte) 0xF1, 0x01, 0x72, (byte) menu.length}, menu);
        Aod y = Aod.parseReply(reply);
        check("AOD parse: values", y != null && y.mode == 1 && y.style == 1 && y.sh == 22 && y.sm == 30 && y.eh == 7 && y.em == 0);
        check("AOD round-trip encode == watch bytes", y != null && y.roundTripOk());
        check("AOD get command = 01 00 08 f1 01", hex(Aod.getCommand()).equals("01000 8f101".replace(" ", "")));
        System.out.println("      AOD set example: " + hex(y.setCommand()));
        check("AOD set wraps 08 f2 01 72 .. 62 .. inner", hex(y.setCommand()).startsWith("01 00 08 f2 01 72".replace(" ", "")) && hex(y.setCommand()).endsWith(hex(inner)));
        System.out.println(fails == 0 ? "ALL PASS" : fails + " FAILED");
    }

    static byte[] cat(byte[] a, byte[] b) { byte[] r = Arrays.copyOf(a, a.length + b.length); System.arraycopy(b, 0, r, a.length, b.length); return r; }
    static String hex(byte[] b) { StringBuilder s = new StringBuilder(); for (byte x : b) s.append(String.format("%02x", x)); return s.toString(); }
}
