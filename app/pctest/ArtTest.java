import com.jk.halostudio.core.Art;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
public class ArtTest {
    public static void main(String[] a) throws Exception {
        int[][] pals = {{0xFF0B1026,0xFF5B2A86,0xFFD946EF,0xFF22D3EE},{0xFF04131F,0xFF10B981,0xFF22D3EE,0xFFA78BFA},
          {0xFF2B1A12,0xFFB45309,0xFFF59E0B,0xFFFDE68A},{0xFF12002B,0xFF3B0764,0xFFF472B6,0xFFFDE047},
          {0xFF020617,0xFF1E3A8A,0xFF60A5FA,0xFFE0F2FE},{0xFF1F1033,0xFFEC4899,0xFF8B5CF6,0xFF22D3EE},
          {0xFF7C3AED,0xFFEC4899,0xFFF59E0B,0xFF06B6D4,0xFF10B981},{0xFF0F172A,0xFF1E293B,0xFF94A3B8},
          {0xFF0F766E,0xFF14B8A6,0xFF5EEAD4,0xFFCCFBF1},{0xFF082F49,0xFF0369A1,0xFF38BDF8,0xFFE0F2FE},
          {0xFF111827,0xFF374151,0xFF9CA3AF,0xFFF59E0B},{0xFF7F1D1D,0xFFDC2626,0xFFFDBA74},
          {0xFF1C1917,0xFF57534E,0xFFE7E5E4,0xFFFAFAF9},{0xFF0B1026,0xFF6D28D9,0xFFE9D5FF},
          {0xFF111827,0xFFEC4899,0xFFFDE68A},{0xFF0A0A0A,0xFF22D3EE}};
        int n = Art.NAMES.length, cols = 4, t = 200;
        BufferedImage sheet = new BufferedImage(cols * (t + 10), ((n + cols - 1) / cols) * (t + 26), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics(); g.setColor(new Color(30,30,30)); g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            int[] px = Art.render(i, pals[i % pals.length], 3);
            BufferedImage im = new BufferedImage(466, 466, BufferedImage.TYPE_INT_RGB); im.setRGB(0,0,466,466,px,0,466);
            int x = (i % cols) * (t + 10), y = (i / cols) * (t + 26);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setClip(new java.awt.geom.Ellipse2D.Float(x, y, t, t)); g.drawImage(im, x, y, t, t, null); g.setClip(null);
            g.setColor(Color.WHITE); g.drawString(i + " " + Art.NAMES[i], x + 4, y + t + 16);
        }
        System.out.println("rendered " + n + " generators in " + (System.currentTimeMillis() - t0) + " ms");
        ImageIO.write(sheet, "png", new File("out/art_sheet.png"));
    }
}
