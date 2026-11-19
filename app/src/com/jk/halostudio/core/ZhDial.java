package com.jk.halostudio.core;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/** ZH dial container AF AF AF 02 — mirror of com/zhapp/ble/utils/lzo/LzoUtils.mergeDialFile(). */
public final class ZhDial {
    public static final int BLOCK = 4096;
    /** Background picture in both templates: raw offset 0x2F338, 466x466 RGB565 big-endian. */
    public static final int BG_OFF = 0x2F338;
    public static final int W = 466, H = 466;
    public static final int BG_LEN = W * H * 2;

    private ZhDial() {}

    /** LzoCrcUtils.getCRCNew32: custom table, init 0xFFFFFFFF, 24-bit shift, ~crc little-endian. */
    public static byte[] crcNew32(byte[] d, int off, int len) {
        long c = 0xFFFFFFFFL;
        for (int i = off; i < off + len; i++) c = CrcTable.T[(int) ((c ^ d[i]) & 0xFF)] ^ ((c >> 8) & 0xFFFFFFL);
        long r = ~c;
        return new byte[]{(byte) r, (byte) (r >> 8), (byte) (r >> 16), (byte) (r >> 24)};
    }

    private static void le32(ByteArrayOutputStream o, int v) {
        o.write(v); o.write(v >>> 8); o.write(v >>> 16); o.write(v >>> 24);
    }

    public static byte[] pack(byte[] raw) {
        int count = (raw.length + BLOCK - 1) / BLOCK;
        int[] table = new int[count];
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for (int i = 0; i < count; i++) {
            byte[] blk = Arrays.copyOfRange(raw, i * BLOCK, Math.min(raw.length, (i + 1) * BLOCK));
            byte[] comp = Lzo1x.compress(blk);
            if (blk.length <= comp.length) { table[i] = blk.length; body.write(blk, 0, blk.length); }
            else { table[i] = comp.length | 0x8000; body.write(comp, 0, comp.length); }
        }
        byte[] b = body.toByteArray();
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(0xAF); o.write(0xAF); o.write(0xAF); o.write(0x02);
        le32(o, raw.length);
        o.write(crcNew32(raw, 0, raw.length), 0, 4);
        le32(o, b.length);
        o.write(crcNew32(b, 0, b.length), 0, 4);
        le32(o, 0);
        o.write(count); o.write(count >>> 8);
        for (int e : table) { o.write(e); o.write(e >>> 8); }
        o.write(b, 0, b.length);
        return o.toByteArray();
    }

    /** Full validating unpack: block table, LZO, raw length, both checksums. Throws on any problem. */
    public static byte[] unpack(byte[] f) {
        if ((f[0] & 0xFF) != 0xAF || (f[1] & 0xFF) != 0xAF || (f[2] & 0xFF) != 0xAF || f[3] != 2)
            throw new IllegalStateException("bad magic");
        int rawLen = u32(f, 4), bodyLen = u32(f, 12), count = (f[24] & 0xFF) | (f[25] & 0xFF) << 8;
        int off = 26 + 2 * count;
        if (off + bodyLen != f.length) throw new IllegalStateException("bad body length");
        byte[] raw = new byte[rawLen];
        int p = off, o = 0;
        for (int i = 0; i < count; i++) {
            int e = (f[26 + 2 * i] & 0xFF) | (f[27 + 2 * i] & 0xFF) << 8;
            int n = e & 0x7FFF;
            byte[] blk = Arrays.copyOfRange(f, p, p + n);
            p += n;
            byte[] d = (e & 0x8000) != 0 ? Lzo1x.decompress(blk, BLOCK + 64) : blk;
            int want = Math.min(BLOCK, rawLen - o);
            if (d.length != want) throw new IllegalStateException("block " + i + " size " + d.length);
            System.arraycopy(d, 0, raw, o, d.length);
            o += d.length;
        }
        if (o != rawLen) throw new IllegalStateException("raw length");
        if (!Arrays.equals(crcNew32(raw, 0, rawLen), Arrays.copyOfRange(f, 8, 12))) throw new IllegalStateException("raw CRC");
        if (!Arrays.equals(crcNew32(f, off, bodyLen), Arrays.copyOfRange(f, 16, 20))) throw new IllegalStateException("body CRC");
        return raw;
    }

    /** Unpack an OFFICIAL face (vendor checksums, which we can't compute): checks structure/LZO/length only.
     *  Used only to import a template the user captured from their own watch. */
    public static byte[] unpackOfficial(byte[] f) {
        if ((f[0] & 0xFF) != 0xAF || (f[1] & 0xFF) != 0xAF || (f[2] & 0xFF) != 0xAF || f[3] != 2)
            throw new IllegalStateException("not a watch-face file (bad magic)");
        int rawLen = u32(f, 4), bodyLen = u32(f, 12), count = (f[24] & 0xFF) | (f[25] & 0xFF) << 8;
        int off = 26 + 2 * count;
        if (off + bodyLen != f.length) throw new IllegalStateException("bad body length");
        byte[] raw = new byte[rawLen];
        int p = off, o = 0;
        for (int i = 0; i < count; i++) {
            int e = (f[26 + 2 * i] & 0xFF) | (f[27 + 2 * i] & 0xFF) << 8, n = e & 0x7FFF;
            byte[] blk = Arrays.copyOfRange(f, p, p + n);
            p += n;
            byte[] d = (e & 0x8000) != 0 ? Lzo1x.decompress(blk, BLOCK + 64) : blk;
            if (d.length != Math.min(BLOCK, rawLen - o)) throw new IllegalStateException("block " + i);
            System.arraycopy(d, 0, raw, o, d.length);
            o += d.length;
        }
        return raw;
    }

    static int u32(byte[] b, int p) {
        return (b[p] & 0xFF) | (b[p + 1] & 0xFF) << 8 | (b[p + 2] & 0xFF) << 16 | (b[p + 3] & 0xFF) << 24;
    }
}
