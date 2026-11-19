package com.jk.halostudio.core;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Always-on display settings (ZH protobuf, from com/zh/ble/sa_wear/protobuf):
 *   top-level { 1: cmd, 14: SESettingMenu { 12: SEInformationScreenDisplay {
 *       1: timing_mode (ALL_DAY_ON=0, TIMED_ON=1, ALL_DAY_OFF=2)
 *       2: start_time SESettingTime { 1 hour, 2 minute, 3 second, 4 millisecond }
 *       3: end_time   SESettingTime
 *       4: dial_style (POINTER=0 analog, NUMBER=1 digital) } } }
 * Read = cmd 241, write = cmd 242 (proto2: every set field is written, also zeros).
 * The app only writes after {@link #roundTripOk} proved our encoding equals the watch's own bytes.
 */
public final class Aod {
    public static final int CMD_GET = 241, CMD_SET = 242;
    public static final int MODE_ALWAYS = 0, MODE_SCHEDULED = 1, MODE_OFF = 2;
    public static final int STYLE_ANALOG = 0, STYLE_DIGITAL = 1;

    public int mode, style;
    public int sh, sm, ss, sms, eh, em, es, ems;
    public boolean hasStart, hasEnd;
    /** The exact SEInformationScreenDisplay bytes the watch sent (null if built locally). */
    public byte[] watchInner;

    // ---------------- protobuf helpers ----------------
    static void varint(ByteArrayOutputStream o, long v) { do { int b = (int) (v & 0x7F); v >>>= 7; o.write(b | (v != 0 ? 0x80 : 0)); } while (v != 0); }
    static void fVar(ByteArrayOutputStream o, int f, long v) { varint(o, f << 3); varint(o, v); }
    static void fLen(ByteArrayOutputStream o, int f, byte[] b) { varint(o, (f << 3) | 2); varint(o, b.length); o.write(b, 0, b.length); }

    static final class Reader {
        final byte[] b; int p; final int end;
        Reader(byte[] b, int from, int to) { this.b = b; this.p = from; this.end = to; }
        boolean more() { return p < end; }
        long varint() { long v = 0; int s = 0; while (true) { int x = b[p++] & 0xFF; v |= (long) (x & 0x7F) << s; if (x < 0x80) return v; s += 7; } }
    }

    /** Finds a length-delimited field and returns {start, end} of its payload, or null. */
    static int[] findLen(byte[] b, int from, int to, int field) {
        Reader r = new Reader(b, from, to);
        while (r.more()) {
            long key = r.varint();
            int f = (int) (key >>> 3), t = (int) (key & 7);
            if (t == 0) r.varint();
            else if (t == 2) { int n = (int) r.varint(); if (f == field) return new int[]{r.p, r.p + n}; r.p += n; }
            else if (t == 5) r.p += 4;
            else if (t == 1) r.p += 8;
            else return null;
        }
        return null;
    }

    static byte[] time(int h, int m, int s, int ms) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        fVar(o, 1, h); fVar(o, 2, m); fVar(o, 3, s); fVar(o, 4, ms);
        return o.toByteArray();
    }

    /** SEInformationScreenDisplay bytes, field order 1..4 like the generated writeTo(). */
    public byte[] encodeInner() {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        fVar(o, 1, mode);
        if (hasStart) fLen(o, 2, time(sh, sm, ss, sms));
        if (hasEnd) fLen(o, 3, time(eh, em, es, ems));
        fVar(o, 4, style);
        return o.toByteArray();
    }

    /** "01 00" + cmd 241 (read AOD settings). */
    public static byte[] getCommand() {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(1); o.write(0);
        fVar(o, 1, CMD_GET);
        return o.toByteArray();
    }

    /** "01 00" + cmd 242 { 14 { 12 { inner } } }. */
    public byte[] setCommand() {
        ByteArrayOutputStream menu = new ByteArrayOutputStream();
        fLen(menu, 12, encodeInner());
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(1); o.write(0);
        fVar(o, 1, CMD_SET);
        fLen(o, 14, menu.toByteArray());
        return o.toByteArray();
    }

    /** Parses the watch's reply to cmd 241 (protobuf without the 2-byte packet header). */
    public static Aod parseReply(byte[] proto) {
        int[] menu = findLen(proto, 0, proto.length, 14);
        if (menu == null) return null;
        int[] sd = findLen(proto, menu[0], menu[1], 12);
        if (sd == null) return null;
        Aod a = new Aod();
        a.watchInner = Arrays.copyOfRange(proto, sd[0], sd[1]);
        Reader r = new Reader(proto, sd[0], sd[1]);
        while (r.more()) {
            long key = r.varint();
            int f = (int) (key >>> 3), t = (int) (key & 7);
            if (t == 0) { long v = r.varint(); if (f == 1) a.mode = (int) v; else if (f == 4) a.style = (int) v; }
            else if (t == 2) {
                int n = (int) r.varint();
                int[] tv = new int[4];
                Reader tr = new Reader(proto, r.p, r.p + n);
                while (tr.more()) { long k = tr.varint(); long v = tr.varint(); int ff = (int) (k >>> 3); if (ff >= 1 && ff <= 4) tv[ff - 1] = (int) v; }
                if (f == 2) { a.hasStart = true; a.sh = tv[0]; a.sm = tv[1]; a.ss = tv[2]; a.sms = tv[3]; }
                if (f == 3) { a.hasEnd = true; a.eh = tv[0]; a.em = tv[1]; a.es = tv[2]; a.ems = tv[3]; }
                r.p += n;
            } else return null;
        }
        return a;
    }

    /** True only if re-encoding the parsed values reproduces the watch's own bytes exactly. */
    public boolean roundTripOk() { return watchInner != null && Arrays.equals(encodeInner(), watchInner); }

    public boolean valid() {
        return mode >= 0 && mode <= 2 && style >= 0 && style <= 1 && sh >= 0 && sh < 24 && eh >= 0 && eh < 24
                && sm >= 0 && sm < 60 && em >= 0 && em < 60;
    }

    @Override public String toString() {
        String m = mode == MODE_ALWAYS ? "Always on" : mode == MODE_SCHEDULED ? "Scheduled" : "Off";
        return m + (mode == MODE_SCHEDULED ? String.format(" %02d:%02d–%02d:%02d", sh, sm, eh, em) : "")
                + " · " + (style == STYLE_ANALOG ? "Analog" : "Digital");
    }
}
