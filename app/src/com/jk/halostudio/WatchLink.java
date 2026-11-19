package com.jk.halostudio;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;

import com.jk.halostudio.core.Upload;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Talks to the Halo exactly like the verified test apps (ZH "Apricot" small-message framing on 16186f01/02,
 * bulk on 16186f04). Every write goes through {@link #guardedWrite}:
 *  - 16186f02: ANNOUNCE, verifyUserId(token) or this job's single message only
 *  - 16186f01: ACK_GO / ACK_DONE only
 *  - 16186f04: only for a face job, only after the watch said READY, only bytes of the current piece
 * Nothing else is ever written (the firmware/DFU service is not referenced at all).
 */
final class WatchLink {
    interface Listener {
        void status(String s);
        void progress(int done, int total);
        void finished(boolean ok, String msg);
        void reply(int cmd, byte[] proto);
    }

    /** One job = verifyUserId, then optionally one message (face pre-check / AOD get / AOD set). */
    static final class Job {
        final byte[] message; final int expectCmd; final List<List<byte[]>> pieces; final String doneText;
        Job(byte[] message, int expectCmd, List<List<byte[]>> pieces, String doneText) {
            this.message = message; this.expectCmd = expectCmd; this.pieces = pieces; this.doneText = doneText;
        }
    }

    static final UUID SVC = UUID.fromString("16186f00-0000-1000-8000-00807f9b34fb");
    static final UUID CH_REPLY = UUID.fromString("16186f01-0000-1000-8000-00807f9b34fb");
    static final UUID CH_CMD = UUID.fromString("16186f02-0000-1000-8000-00807f9b34fb");
    static final UUID CH_BULK = UUID.fromString("16186f04-0000-1000-8000-00807f9b34fb");
    static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    static final String[] STATUS = {"READY", "BUSY", "DUPLICATED", "LOW_STORAGE", "LOW_BATTERY", "DOWNGRADE",
            "LOW_STORAGE_TO_ASK_DELETE"};

    private final Context ctx;
    private final Handler bg;
    private final Listener ui;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic chReply, chCmd, chBulk;

    private Job job;
    private byte[] verifyMsg;
    private final List<byte[]> questions = new ArrayList<>();
    private int q, expectReplyPackets, piece;
    private final List<byte[]> replyParts = new ArrayList<>();
    private boolean finished = true, bulkAllowed, transferDone;
    private long lastProgress;
    private final ArrayDeque<Object[]> queue = new ArrayDeque<>();
    private boolean writing;

    WatchLink(Context c, Handler bg, Listener l) { this.ctx = c; this.bg = bg; this.ui = l; }

    boolean busy() { return !finished; }

    /** Stops the current job at any point. Mid-transfer the watch never gets the final piece/CRC,
     *  so it discards the partial face and keeps the one it is showing. */
    void cancel() {
        bg.post(() -> {
            if (finished) return;
            if (transferDone) return;                    // already fully sent; let it finish
            end(false, bulkAllowed
                    ? "Install cancelled. The watch drops the half-sent face and keeps its current one."
                    : "Cancelled — nothing was sent to the watch.");
        });
    }

    void start(String token, String address, Job j) {
        if (!finished) { ui.status("Already busy."); return; }
        if (token == null || token.trim().isEmpty()) { ui.finished(false, "Set your NoiseFit user ID first (Watch → Settings)."); return; }
        job = j;
        verifyMsg = Upload.verifyUser(token.trim());
        finished = false; bulkAllowed = false; transferDone = false; piece = -1;
        queue.clear(); writing = false;
        questions.clear();
        questions.add(verifyMsg);
        if (j.message != null) questions.add(j.message);
        q = -1;
        try {
            BluetoothAdapter ad = ((BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
            if (ad == null || !ad.isEnabled()) { end(false, "Bluetooth is off. Turn it on."); return; }
            BluetoothDevice dev = null;
            if (address != null && BluetoothAdapter.checkBluetoothAddress(address.trim().toUpperCase())) dev = ad.getRemoteDevice(address.trim().toUpperCase());
            if (dev == null) for (BluetoothDevice d : ad.getBondedDevices()) {
                String n = d.getName();
                if (n != null && n.toLowerCase().contains("halo")) { dev = d; break; }
            }
            if (dev == null) { end(false, "No Halo watch found. Pair it in NoiseFit first, or set its address in Settings."); return; }
            ui.status("Connecting to " + dev.getAddress() + " ...");
            gatt = dev.connectGatt(ctx, false, cb, BluetoothDevice.TRANSPORT_LE);
            lastProgress = System.currentTimeMillis();
            bg.postDelayed(watchdog, 3000);
        } catch (SecurityException e) {
            end(false, "Bluetooth permission missing.");
        }
    }

    private final Runnable watchdog = new Runnable() {
        @Override public void run() {
            if (finished) return;
            if (System.currentTimeMillis() - lastProgress > 20000) { end(false, "Watch stopped answering (20 s). Nothing harmful happened; try again."); return; }
            bg.postDelayed(this, 2000);
        }
    };

    private void guardedWrite(BluetoothGattCharacteristic ch, byte[] v) {
        boolean ok = false;
        UUID u = ch.getUuid();
        if (u.equals(CH_CMD)) {
            ok = Arrays.equals(v, Upload.ANNOUNCE) || Arrays.equals(v, verifyMsg) || (job.message != null && Arrays.equals(v, job.message));
        } else if (u.equals(CH_REPLY)) {
            ok = Arrays.equals(v, Upload.ACK_GO) || Arrays.equals(v, Upload.ACK_DONE);
        } else if (u.equals(CH_BULK) && bulkAllowed && job.pieces != null && piece >= 0 && piece < job.pieces.size()) {
            for (byte[] w : job.pieces.get(piece)) if (Arrays.equals(w, v)) { ok = true; break; }
        }
        if (!ok) { end(false, "Safety stop: blocked an unexpected write."); return; }
        queue.add(new Object[]{ch, v});
        pump();
    }

    private void pump() {
        if (writing || queue.isEmpty() || gatt == null) return;
        Object[] op = queue.poll();
        writing = true;
        try {
            int r = gatt.writeCharacteristic((BluetoothGattCharacteristic) op[0], (byte[]) op[1],
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            if (r != BluetoothGatt.GATT_SUCCESS) { writing = false; queue.addFirst(op); bg.postDelayed(this::pump, 15); }
        } catch (SecurityException e) { end(false, "Bluetooth permission missing."); }
    }

    private void enableNotify(BluetoothGattCharacteristic ch) {
        try {
            gatt.setCharacteristicNotification(ch, true);
            gatt.writeDescriptor(ch.getDescriptor(CCCD), new byte[]{1, 0});
        } catch (SecurityException e) { end(false, "Bluetooth permission missing."); }
    }

    private void nextQuestion() {
        q++;
        if (q >= questions.size()) { if (job.message == null) end(true, job.doneText); return; }
        expectReplyPackets = 0;
        replyParts.clear();
        guardedWrite(chCmd, Upload.ANNOUNCE);
    }

    private final BluetoothGattCallback cb = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(BluetoothGatt g, int status, int state) {
            bg.post(() -> {
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    ui.status("Connected. Preparing...");
                    try { g.requestMtu(247); } catch (SecurityException e) { end(false, "permission"); }
                } else if (state == BluetoothProfile.STATE_DISCONNECTED && !finished) {
                    end(false, "Watch disconnected (status " + status + "). Keep it close and try again.");
                }
            });
        }
        @Override public void onMtuChanged(BluetoothGatt g, int mtu, int status) {
            bg.post(() -> {
                if (mtu - 3 < Upload.MTU) { end(false, "Bluetooth link too small (MTU " + mtu + ")."); return; }
                try { g.discoverServices(); } catch (SecurityException e) { end(false, "permission"); }
            });
        }
        @Override public void onServicesDiscovered(BluetoothGatt g, int status) {
            bg.post(() -> {
                BluetoothGattService s = g.getService(SVC);
                if (s == null) { end(false, "This doesn't look like the Halo (service missing)."); return; }
                chReply = s.getCharacteristic(CH_REPLY);
                chCmd = s.getCharacteristic(CH_CMD);
                chBulk = s.getCharacteristic(CH_BULK);
                if (chReply == null || chCmd == null || chBulk == null) { end(false, "Watch channels missing."); return; }
                enableNotify(chReply);
            });
        }
        @Override public void onDescriptorWrite(BluetoothGatt g, BluetoothGattDescriptor d, int status) {
            bg.post(() -> {
                if (status != BluetoothGatt.GATT_SUCCESS) { end(false, "Could not enable notifications."); return; }
                UUID u = d.getCharacteristic().getUuid();
                if (u.equals(CH_REPLY)) enableNotify(chCmd);
                else if (u.equals(CH_CMD) && job.pieces != null) enableNotify(chBulk);
                else { ui.status("Checking watch..."); nextQuestion(); }
            });
        }
        @Override public void onCharacteristicWrite(BluetoothGatt g, BluetoothGattCharacteristic ch, int status) {
            bg.postDelayed(() -> { writing = false; pump(); }, 20);
        }
        @Override public void onCharacteristicChanged(BluetoothGatt g, BluetoothGattCharacteristic ch, byte[] v) {
            final byte[] val = v.clone();
            bg.post(() -> onNotify(ch.getUuid(), val));
        }
    };

    private void onNotify(UUID ch, byte[] v) {
        if (finished) return;
        lastProgress = System.currentTimeMillis();
        boolean ctrl = v.length == 6 && v[0] == 0 && v[1] == 0;
        if (ch.equals(CH_BULK)) { onBulk(v, ctrl); return; }
        if (ch.equals(CH_CMD)) {
            if (ctrl && v[2] == 1 && v[3] == 1) { guardedWrite(chCmd, questions.get(q)); return; }
            if (ctrl && v[2] == 1 && v[3] == 0) return;
            end(false, "Watch was busy. Wait a moment and try again.");
            return;
        }
        if (ctrl && v[2] == 0 && v[3] == 0) {
            expectReplyPackets = v[4] & 0xFF;
            replyParts.clear();
            guardedWrite(chReply, Upload.ACK_GO);
            return;
        }
        if (expectReplyPackets > 0 && v.length > 2) {
            replyParts.add(Arrays.copyOfRange(v, 2, v.length));
            if (replyParts.size() >= expectReplyPackets) {
                expectReplyPackets = 0;
                guardedWrite(chReply, Upload.ACK_DONE);
                int n = 0;
                for (byte[] p : replyParts) n += p.length;
                byte[] r = new byte[n];
                int o = 0;
                for (byte[] p : replyParts) { System.arraycopy(p, 0, r, o, p.length); o += p.length; }
                handleReply(r);
            }
        }
    }

    private void handleReply(byte[] r) {
        int cmd = Upload.replyCmd(r);
        if (transferDone || q >= questions.size()) return;
        int want = q == 0 ? 19 : job.expectCmd;
        if (cmd != want) return;                      // unrelated status message
        if (q == 0) { bg.postDelayed(this::nextQuestion, 300); return; }
        if (cmd != 83) {                              // AOD get/set etc.
            ui.reply(cmd, r);
            q = questions.size();
            end(true, job.doneText);
            return;
        }
        int st = -1;
        for (int i = 0; i + 3 < r.length; i++) if (r[i] == 0x2A && r[i + 1] == 0x02 && r[i + 2] == 0x08) st = r[i + 3];
        if (st != 0) {
            String name = st >= 0 && st < STATUS.length ? STATUS[st] : "unknown";
            end(false, "Watch said " + name + " - nothing was sent." + (st == 4 ? " Charge the watch first." : ""));
            return;
        }
        q = questions.size();
        bulkAllowed = true;
        piece = 0;
        ui.status("Watch is ready. Sending face...");
        ui.progress(0, job.pieces.size());
        bg.postDelayed(() -> guardedWrite(chBulk, job.pieces.get(0).get(0)), 200);
    }

    private void onBulk(byte[] v, boolean ctrl) {
        if (!bulkAllowed || piece < 0) { end(false, "Safety stop: unexpected message from watch."); return; }
        List<byte[]> p = job.pieces.get(piece);
        if (ctrl && v[2] == 1 && v[3] == 1) {
            for (int i = 1; i < p.size(); i++) guardedWrite(chBulk, p.get(i));
        } else if (ctrl && v[2] == 1 && v[3] == 0) {
            piece++;
            ui.progress(piece, job.pieces.size());
            if (piece == job.pieces.size()) {
                transferDone = true; bulkAllowed = false;
                ui.status("Sent! Finishing...");
                bg.postDelayed(() -> { transferDone = false; end(true, job.doneText); }, 8000);
                return;
            }
            guardedWrite(chBulk, job.pieces.get(piece).get(0));
        } else if (ctrl && v[2] == 1 && v[3] == 5) {
            int idx = (v[4] & 0xFF) | ((v[5] & 0xFF) << 8);
            if (idx >= 1 && idx < p.size()) guardedWrite(chBulk, p.get(idx));
            else end(false, "Safety stop: bad resend request.");
        } else {
            end(false, "Watch stopped the transfer. It keeps its current face; try again.");
        }
    }

    private void end(boolean ok, String msg) {
        if (finished) return;
        finished = true;
        bulkAllowed = false;
        bg.removeCallbacks(watchdog);
        queue.clear();
        try { if (gatt != null) { gatt.disconnect(); gatt.close(); } } catch (SecurityException ignored) { }
        gatt = null;
        ui.finished(ok, msg);
    }
}
