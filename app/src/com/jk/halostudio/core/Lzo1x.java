package com.jk.halostudio.core;

/**
 * LZO1X-1 (LZO 2.10 lzo1x_1_compress, 64-bit match compare) and a safe LZO1X decompressor.
 * Verified on the PC to be byte-identical to python-lzo / the watch vendor's blocks (see pctest).
 * Inputs here are at most one 4096-byte dial block (single 49152-byte chunk).
 */
public final class Lzo1x {
    private static final int D_BITS = 14;
    private static final int D_MASK = (1 << D_BITS) - 1;

    private Lzo1x() {}

    private static int le32(byte[] b, int p) {
        return (b[p] & 0xFF) | (b[p + 1] & 0xFF) << 8 | (b[p + 2] & 0xFF) << 16 | (b[p + 3] & 0xFF) << 24;
    }

    private static long le64(byte[] b, int p) {
        long v = 0;
        for (int i = 7; i >= 0; i--) v = (v << 8) | (b[p + i] & 0xFF);
        return v;
    }

    public static byte[] compress(byte[] in) {
        if (in.length > 49152) throw new IllegalArgumentException("block too large");
        int inLen = in.length;
        byte[] out = new byte[inLen + inLen / 16 + 64 + 3];
        int op = 0;
        int t = 0;
        if (inLen > 20) {
            int[] dict = new int[1 << D_BITS];
            int ipEnd = inLen - 20;
            int ii = 0;
            int ip = 4;
            int ti = 0;
            ip += 1 + ((ip - ii) >> 5);                    // first "literal:" step
            while (ip < ipEnd) {                           // "next:"
                int dv = le32(in, ip);
                int dindex = ((dv * 0x1824429d) >>> (32 - D_BITS)) & D_MASK;
                int mPos = dict[dindex];
                dict[dindex] = ip;
                if (dv != le32(in, mPos)) {                // literal
                    ip += 1 + ((ip - ii) >> 5);
                    continue;
                }
                // a match: flush pending literals
                ii -= ti;
                ti = 0;
                int lt = ip - ii;
                if (lt != 0) {
                    if (lt <= 3) {
                        out[op - 2] |= (byte) lt;
                    } else if (lt <= 16) {
                        out[op++] = (byte) (lt - 3);
                    } else if (lt <= 18) {
                        out[op++] = (byte) (lt - 3);
                    } else {
                        int tt = lt - 18;
                        out[op++] = 0;
                        while (tt > 255) { tt -= 255; out[op++] = 0; }
                        out[op++] = (byte) tt;
                    }
                    System.arraycopy(in, ii, out, op, lt);
                    op += lt;
                }
                // match length (64-bit stride compare, like LZO on 64-bit little-endian)
                int mLen = 4;
                long v = le64(in, ip + mLen) ^ le64(in, mPos + mLen);
                boolean capped = false;
                if (v == 0) {
                    do {
                        mLen += 8;
                        v = le64(in, ip + mLen) ^ le64(in, mPos + mLen);
                        if (ip + mLen >= ipEnd) { capped = true; break; }
                    } while (v == 0);
                }
                if (!capped) mLen += Long.numberOfTrailingZeros(v) / 8;
                int mOff = ip - mPos;
                ip += mLen;
                ii = ip;
                if (mLen <= 8 && mOff <= 0x0800) {
                    mOff -= 1;
                    out[op++] = (byte) (((mLen - 1) << 5) | ((mOff & 7) << 2));
                    out[op++] = (byte) (mOff >>> 3);
                } else if (mOff <= 0x4000) {
                    mOff -= 1;
                    if (mLen <= 33) {
                        out[op++] = (byte) (32 | (mLen - 2));
                    } else {
                        mLen -= 33;
                        out[op++] = 32;
                        while (mLen > 255) { mLen -= 255; out[op++] = 0; }
                        out[op++] = (byte) mLen;
                    }
                    out[op++] = (byte) (mOff << 2);
                    out[op++] = (byte) (mOff >>> 6);
                } else {
                    mOff -= 0x4000;
                    if (mLen <= 9) {
                        out[op++] = (byte) (16 | ((mOff >>> 11) & 8) | (mLen - 2));
                    } else {
                        mLen -= 9;
                        out[op++] = (byte) (16 | ((mOff >>> 11) & 8));
                        while (mLen > 255) { mLen -= 255; out[op++] = 0; }
                        out[op++] = (byte) mLen;
                    }
                    out[op++] = (byte) (mOff << 2);
                    out[op++] = (byte) (mOff >>> 6);
                }
            }
            t = inLen - (ii - ti);
        } else {
            t = inLen;
        }
        if (t > 0) {
            int ii = inLen - t;
            if (op == 0 && t <= 238) {
                out[op++] = (byte) (17 + t);
            } else if (t <= 3) {
                out[op - 2] |= (byte) t;
            } else if (t <= 18) {
                out[op++] = (byte) (t - 3);
            } else {
                int tt = t - 18;
                out[op++] = 0;
                while (tt > 255) { tt -= 255; out[op++] = 0; }
                out[op++] = (byte) tt;
            }
            System.arraycopy(in, ii, out, op, t);
            op += t;
        }
        out[op++] = 16 | 1;
        out[op++] = 0;
        out[op++] = 0;
        byte[] r = new byte[op];
        System.arraycopy(out, 0, r, 0, op);
        return r;
    }

    /** Safe LZO1X decompressor (mirrors analysis/tools/lzo1x.py). */
    public static byte[] decompress(byte[] src, int maxOut) {
        byte[] out = new byte[maxOut];
        int op = 0, ip = 0, state = 0;
        if ((src[0] & 0xFF) > 17) {
            int t = (src[0] & 0xFF) - 17;
            ip = 1;
            System.arraycopy(src, ip, out, op, t);
            op += t; ip += t;
            state = t < 4 ? t : 4;
        }
        while (true) {
            int t = src[ip++] & 0xFF;
            int nxt;
            if (t < 16) {
                if (state == 0) {
                    if (t == 0) {
                        int z = 0;
                        while (src[ip] == 0) { z++; ip++; }
                        t = 15 + 255 * z + (src[ip++] & 0xFF);
                    }
                    int n = t + 3;
                    System.arraycopy(src, ip, out, op, n);
                    op += n; ip += n;
                    state = 4;
                    continue;
                }
                int dist;
                int len;
                if (state != 4) { dist = 1 + (t >>> 2) + ((src[ip++] & 0xFF) << 2); len = 2; }
                else { dist = 1 + 0x0800 + (t >>> 2) + ((src[ip++] & 0xFF) << 2); len = 3; }
                op = copyMatch(out, op, dist, len);
                nxt = t & 3;
            } else if (t >= 64) {
                int dist = 1 + ((t >>> 2) & 7) + ((src[ip++] & 0xFF) << 3);
                op = copyMatch(out, op, dist, (t >>> 5) + 1);
                nxt = t & 3;
            } else if (t >= 32) {
                int len = (t & 31) + 2;
                if (len == 2) {
                    int z = 0;
                    while (src[ip] == 0) { z++; ip++; }
                    len = 33 + 255 * z + (src[ip++] & 0xFF);
                }
                int v = (src[ip] & 0xFF) | ((src[ip + 1] & 0xFF) << 8);
                ip += 2;
                op = copyMatch(out, op, 1 + (v >>> 2), len);
                nxt = v & 3;
            } else {
                int high = (t & 8) << 11;
                int len = (t & 7) + 2;
                if (len == 2) {
                    int z = 0;
                    while (src[ip] == 0) { z++; ip++; }
                    len = 9 + 255 * z + (src[ip++] & 0xFF);
                }
                int v = (src[ip] & 0xFF) | ((src[ip + 1] & 0xFF) << 8);
                ip += 2;
                int dist = high + (v >>> 2);
                if (dist == 0) {
                    byte[] r = new byte[op];
                    System.arraycopy(out, 0, r, 0, op);
                    return r;
                }
                op = copyMatch(out, op, dist + 0x4000, len);
                nxt = v & 3;
            }
            state = nxt;
            if (nxt > 0) {
                System.arraycopy(src, ip, out, op, nxt);
                op += nxt; ip += nxt;
            }
        }
    }

    private static int copyMatch(byte[] out, int op, int dist, int len) {
        int s = op - dist;
        if (s < 0) throw new IllegalStateException("bad LZO match");
        for (int k = 0; k < len; k++) out[op + k] = out[s + k];
        return op + len;
    }
}
