"""Rebuild a watch-face file from an Android btsnoop (HCI) log.

READ-ONLY: this only parses a log file on the PC. It never talks to the watch.

Protocol (from com/zhapp/ble/BluetoothService.java + manager/ThemeManager.java, ZH "Apricot"):
  stream  = header(22) + face_file + CRC32(header + face_file), little-endian CRC
  header  = 00 <type> 00*16 <len u32 LE>          type 16 = watch face
  per piece, phone writes to char 16186f04:
    announce  : 00 00 00 00 <packets_in_piece> 00
    watch ntf : 00 00 01 01 -> "send"   | 00 00 01 00 -> "piece ok"   | 00 00 01 05 <idx LE16> -> "resend idx"
    packet 1  : 01 00 <total_pieces LE16> <piece_no LE16> <data>
    packet n  : <n> 00 <data>

Usage: python -I extract_face.py <bugreport.zip | btsnoop file> <out_dir>
"""
import io
import os
import struct
import sys
import zipfile
import zlib

FACE_TYPE = 16


def load_snoop_blobs(path):
    if zipfile.is_zipfile(path):
        z = zipfile.ZipFile(path)
        for name in z.namelist():
            data = z.read(name) if z.getinfo(name).file_size < 512 * 1024 * 1024 else b""
            if data.startswith(b"btsnoop\0"):
                yield name, data
            elif name.lower().endswith(".zip"):
                inner = zipfile.ZipFile(io.BytesIO(data))
                for n2 in inner.namelist():
                    d2 = inner.read(n2)
                    if d2.startswith(b"btsnoop\0"):
                        yield name + "!" + n2, d2
    else:
        with open(path, "rb") as f:
            yield path, f.read()


def parse_btsnoop(data):
    """Yield (sent_by_phone, timestamp, hci_packet_with_type_byte)."""
    magic, version, dlt = struct.unpack(">8sII", data[:16])
    assert magic == b"btsnoop\0", "not a btsnoop file"
    off = 16
    while off + 24 <= len(data):
        orig, incl, flags, _drops, ts = struct.unpack(">IIIIq", data[off:off + 24])
        off += 24
        pkt = data[off:off + incl]
        off += incl
        if len(pkt) < incl:
            break
        sent = (flags & 1) == 0
        if dlt == 1001:  # un-encapsulated HCI: flag bit1 = command/event
            ptype = (b"\x04" if flags & 2 else b"\x02") if not sent else (b"\x01" if flags & 2 else b"\x02")
            pkt = ptype + pkt
        yield sent, ts, pkt


def att_pdus(records):
    """Reassemble ACL -> L2CAP, yield (sent, ts, conn_handle, att_pdu) for CID 4."""
    partial = {}
    for sent, ts, pkt in records:
        if not pkt or pkt[0] != 0x02 or len(pkt) < 5:
            continue
        hf, alen = struct.unpack("<HH", pkt[1:5])
        handle, pb = hf & 0x0FFF, (hf >> 12) & 0x3
        payload = pkt[5:5 + alen]
        key = (sent, handle)
        if pb in (0, 2):
            partial[key] = bytearray(payload)
        elif key in partial:
            partial[key] += payload
        else:
            continue
        buf = partial[key]
        if len(buf) >= 4:
            l2len, cid = struct.unpack("<HH", buf[:4])
            if len(buf) >= 4 + l2len:
                if cid == 4:
                    yield sent, ts, handle, bytes(buf[4:4 + l2len])
                del partial[key]


def precheck_face_id(v):
    """Pull faceId out of a cmd-83 (0x53) request: 01 00 08 53 3a .. 22 .. 0a <len> <id>."""
    k = v.find(b"\x08\x53\x3a")
    if k < 0:
        return None
    j = v.find(b"\x0a", k + 4)
    if j < 0 or j + 2 > len(v):
        return None
    n = v[j + 1]
    return v[j + 2:j + 2 + n].decode("ascii", "replace")


def split_transfers(writes, conn, h):
    """Group data packets on the transfer handle into separate uploads (a new one starts at piece 1)."""
    transfers, cur_t, cur = [], None, None
    for ts, c, hh, v in writes:
        if (c, hh) != (conn, h) or len(v) < 2 or v[1] != 0 or v[0] == 0:
            continue
        idx = v[0]
        if idx == 1:
            if len(v) < 6:
                continue
            total, cur = struct.unpack("<HH", v[2:6])
            if cur == 1 and (cur_t is None or cur_t["pieces"]):
                cur_t = {"ts": ts, "total": total, "pieces": {}}
                transfers.append(cur_t)
            cur_t["pieces"].setdefault(cur, {})[1] = v[6:]
        elif cur_t is not None and cur is not None:
            cur_t["pieces"][cur][idx] = v[2:]  # resends overwrite
    return transfers


def main():
    src, out_dir = sys.argv[1], sys.argv[2]
    os.makedirs(out_dir, exist_ok=True)
    seen_crc = set()
    for name, blob in load_snoop_blobs(src):
        recs = list(parse_btsnoop(blob))
        print(f"[log] {name}: {len(blob)} bytes, {len(recs)} HCI records")
        writes, notifs = [], []
        for sent, ts, conn, pdu in att_pdus(recs):
            op = pdu[0]
            if sent and op in (0x12, 0x52) and len(pdu) >= 3:
                writes.append((ts, conn, struct.unpack("<H", pdu[1:3])[0], pdu[3:]))
            elif not sent and op in (0x1B, 0x1D) and len(pdu) >= 3:
                notifs.append((ts, conn, struct.unpack("<H", pdu[1:3])[0], pdu[3:]))
        print(f"      ATT writes: {len(writes)}  notifications: {len(notifs)}")
        ann = [(c, h) for _, c, h, v in writes if len(v) == 6 and v[:4] == b"\0\0\0\0" and v[5] == 0 and v[4] > 1]
        if not ann:
            continue
        conn, h = max(set(ann), key=ann.count)
        prechecks = [(ts, precheck_face_id(v)) for ts, c, hh, v in writes if precheck_face_id(v)]
        for t in split_transfers(writes, conn, h):
            pieces, total = t["pieces"], t["total"]
            missing = [p for p in range(1, total + 1) if p not in pieces]
            stream = b"".join(b"".join(pieces[p][i] for i in sorted(pieces[p])) for p in sorted(pieces))
            hdr, body = stream[:22], stream[22:]
            ftype, flen = hdr[1], struct.unpack("<I", hdr[18:22])[0]
            face, crc_rx = body[:flen], body[flen:flen + 4]
            crc_calc = struct.pack("<I", zlib.crc32(hdr + face) & 0xFFFFFFFF)
            if crc_rx in seen_crc:
                continue  # same upload already saved from another copy of the log
            seen_crc.add(crc_rx)
            fid = max((p for p in prechecks if p[0] <= t["ts"]), default=(0, "unknown"))[1]
            ok = crc_rx == crc_calc and not missing and ftype == FACE_TYPE
            print(f"      face id {fid}: type {ftype}, {flen} bytes, pieces {len(pieces)}/{total}"
                  f"{'  MISSING ' + str(missing[:10]) if missing else ''}, CRC {crc_rx.hex()} vs {crc_calc.hex()}"
                  f" -> {'MATCH' if ok else 'PROBLEM'}")
            out = os.path.join(out_dir, f"face_{fid}_type{ftype}_{flen}.bin")
            with open(out, "wb") as f:
                f.write(face)
            print(f"        saved -> {out}   first 24 bytes: {face[:24].hex(' ')}")
    if not seen_crc:
        print("No face transfer reconstructed.")


if __name__ == "__main__":
    main()
