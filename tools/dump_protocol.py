"""Print the non-bulk ATT conversation around a watch-face install (READ-ONLY log parsing).

Usage: python -I dump_protocol.py <bugreport.zip | btsnoop file> [log-name-substring]
"""
import os
import struct
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from extract_face import att_pdus, load_snoop_blobs, parse_btsnoop  # noqa: E402

OPS = {0x02: "MTU_REQ", 0x03: "MTU_RSP", 0x12: "WRITE_REQ", 0x13: "WRITE_RSP", 0x52: "WRITE_CMD",
       0x1B: "NOTIFY", 0x1D: "INDICATE", 0x0A: "READ_REQ", 0x0B: "READ_RSP", 0x01: "ERROR",
       0x08: "READ_TYPE_REQ", 0x09: "READ_TYPE_RSP", 0x10: "READ_GROUP_REQ", 0x11: "READ_GROUP_RSP",
       0x04: "FIND_INFO_REQ", 0x05: "FIND_INFO_RSP"}


def main():
    src = sys.argv[1]
    want = sys.argv[2] if len(sys.argv) > 2 else "btsnoop_hci.log"
    for name, blob in load_snoop_blobs(src):
        if want not in name:
            continue
        recs = list(parse_btsnoop(blob))
        t0 = None
        bulk_writes = bulk_announce = 0
        acks = {}
        for sent, ts, conn, pdu in att_pdus(recs):
            t0 = t0 or ts
            op = pdu[0]
            name_ = OPS.get(op, f"op{op:02x}")
            handle = struct.unpack("<H", pdu[1:3])[0] if len(pdu) >= 3 and op in (0x12, 0x52, 0x1B, 0x1D, 0x0A, 0x0B) else None
            val = pdu[3:] if handle is not None else pdu[1:]
            # condense the bulk transfer
            if handle == 0x002A and sent and op in (0x12, 0x52):
                if len(val) == 6 and val[:4] == b"\0\0\0\0":
                    bulk_announce += 1
                else:
                    bulk_writes += 1
                continue
            if handle == 0x002A and not sent and op == 0x1B and len(val) >= 4 and val[:3] == b"\0\0\x01":
                acks[val[3]] = acks.get(val[3], 0) + 1
                if val[3] not in (0, 1):
                    print(f"{(ts - t0) / 1e6:9.3f}s  watch->phone NOTIFY 0x002a  {val.hex(' ')}   <-- unusual piece ack")
                continue
            if op in (0x13,):
                continue
            who = "phone->watch" if sent else "watch->phone"
            h = f"0x{handle:04x}" if handle is not None else "      "
            v = val[:48].hex(" ") + (" ..." if len(val) > 48 else "")
            print(f"{(ts - t0) / 1e6:9.3f}s  {who} {name_:14s} {h}  ({len(val):3d}B) {v}")
        print(f"\nbulk handle 0x002a: {bulk_announce} piece announcements, {bulk_writes} data packets")
        print("watch piece acks (00 00 01 xx): " + ", ".join(f"{k:02x}: {n}" for k, n in sorted(acks.items())))
        return


if __name__ == "__main__":
    main()
