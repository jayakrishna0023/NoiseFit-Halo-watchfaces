package com.jk.halostudio.core;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

/** ZH "Apricot" upload framing — mirror of ThemeManager.initUpload + BluetoothService.sendThemePiece
 *  (verified byte-identical to the NoiseFit capture with MTU 177). */
public final class Upload {
    public static final int MTU = 177;
    public static final byte[] ANNOUNCE = {0, 0, 0, 0, 1, 0};
    public static final byte[] ACK_GO = {0, 0, 1, 1, 0, 0};
    public static final byte[] ACK_DONE = {0, 0, 1, 0, 0, 0};
    /** "01 00" + cmd 19 verifyUserId { 3 { 6: token } } — exactly as NoiseFit sends it on every connect.
     *  The token is the user's own NoiseFit user id (from settings; never hard-coded). */
    public static byte[] verifyUser(String token) {
        return cat(new byte[]{1, 0}, fieldVarint(1, 19), fieldBytes(3, fieldBytes(6, token.getBytes())));
    }

    /** Extracts the cmd-83/other command number from a reply protobuf (field 1 varint). */
    public static int replyCmd(byte[] r) {
        if (r.length < 2 || r[0] != 0x08) return -1;
        int v = 0, s = 0, p = 1;
        while (p < r.length) { int x = r[p++] & 0xFF; v |= (x & 0x7F) << s; if (x < 0x80) return v; s += 7; }
        return -1;
    }

    private Upload() {}

    /** type 16 = watch face. Anything else is refused (32 would be firmware). */
    public static byte[] frameStream(byte[] face) {
        byte[] hdr = new byte[22];
        hdr[1] = 16;
        hdr[18] = (byte) face.length; hdr[19] = (byte) (face.length >> 8);
        hdr[20] = (byte) (face.length >> 16); hdr[21] = (byte) (face.length >> 24);
        CRC32 c = new CRC32();
        c.update(hdr); c.update(face);
        long v = c.getValue();
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(hdr, 0, 22); o.write(face, 0, face.length);
        o.write((int) v); o.write((int) (v >> 8)); o.write((int) (v >> 16)); o.write((int) (v >> 24));
        return o.toByteArray();
    }

    /** Pieces as written to 16186f04: each = [announce, packet1, packet2, ...]. */
    public static List<List<byte[]>> pieces(byte[] stream) {
        int first = MTU - 6, maxPack = 4000 / MTU, pieceBytes = (maxPack - 1) * (MTU - 2) + first;
        int total = (stream.length + pieceBytes - 1) / pieceBytes;
        List<List<byte[]>> res = new ArrayList<>();
        for (int p = 1; p <= total; p++) {
            int start = (p - 1) * pieceBytes, end = Math.min(stream.length, start + pieceBytes);
            List<byte[]> pk = new ArrayList<>();
            int n1 = Math.min(first, end - start);
            byte[] a = new byte[6 + n1];
            a[0] = 1; a[2] = (byte) total; a[3] = (byte) (total >> 8); a[4] = (byte) p; a[5] = (byte) (p >> 8);
            System.arraycopy(stream, start, a, 6, n1);
            pk.add(a);
            int off = start + n1, idx = 2;
            while (off < end) {
                int n = Math.min(MTU - 2, end - off);
                byte[] b = new byte[2 + n];
                b[0] = (byte) idx;
                System.arraycopy(stream, off, b, 2, n);
                pk.add(b);
                off += n; idx++;
            }
            List<byte[]> withAnn = new ArrayList<>();
            withAnn.add(new byte[]{0, 0, 0, 0, (byte) pk.size(), 0});
            withAnn.addAll(pk);
            res.add(withAnn);
        }
        return res;
    }

    // ---- tiny protobuf writer for the pre-check (cmd 83) ----
    private static void varint(ByteArrayOutputStream o, long n) {
        do { int b = (int) (n & 0x7F); n >>>= 7; o.write(b | (n != 0 ? 0x80 : 0)); } while (n != 0);
    }
    private static byte[] fieldBytes(int num, byte[] payload) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        varint(o, (num << 3) | 2); varint(o, payload.length); o.write(payload, 0, payload.length);
        return o.toByteArray();
    }
    private static byte[] fieldVarint(int num, long v) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        varint(o, num << 3); varint(o, v);
        return o.toByteArray();
    }
    private static byte[] cat(byte[]... parts) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        for (byte[] p : parts) o.write(p, 0, p.length);
        return o.toByteArray();
    }

    /** "01 00" + cmd 83 getDeviceWatchFace(faceId, sizeKb, sizeKb, replace). */
    public static byte[] precheck(String faceId, int faceBytes, boolean replace) {
        int kb = faceBytes / 1000;
        byte[] inner = cat(fieldBytes(1, faceId.getBytes()), fieldVarint(2, kb), fieldVarint(3, kb), fieldVarint(4, replace ? 1 : 0));
        return cat(new byte[]{1, 0}, fieldVarint(1, 83), fieldBytes(7, fieldBytes(4, inner)));
    }
}
