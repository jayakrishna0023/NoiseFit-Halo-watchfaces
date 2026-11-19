"""Build the ZH "Apricot" watch-face upload packets exactly like the app (OFFLINE, no Bluetooth).

Mirrors com/zhapp/ble/manager/ThemeManager.initUpload() and BluetoothService.sendThemePiece().
"""
import struct
import zlib

FACE_TYPE = 16  # ThemeManager: "watch" -> 16.  Anything else (32) would be firmware: never allowed.


def frame_stream(face: bytes, ftype: int = FACE_TYPE) -> bytes:
    if ftype != FACE_TYPE:
        raise ValueError("refusing: only watch-face type 16 is allowed")
    hdr = bytes([0, ftype]) + bytes(16) + struct.pack("<I", len(face))
    return hdr + face + struct.pack("<I", zlib.crc32(hdr + face) & 0xFFFFFFFF)


def pieces(stream: bytes, mtu: int):
    """Yield (piece_no, total_pieces, [packets]) as sent to char 16186f04."""
    first_len = mtu - 6            # onePackMaxDataLength
    max_pack = 4000 // mtu         # dataPieceMaxPack
    piece_bytes = (max_pack - 1) * (mtu - 2) + first_len  # dataPieceTotalByteLength
    total = -(-len(stream) // piece_bytes)
    for p in range(1, total + 1):
        start = (p - 1) * piece_bytes
        chunk = stream[start:start + piece_bytes]
        pkts = [bytes([1, 0]) + struct.pack("<HH", total, p) + chunk[:first_len]]
        off, idx = first_len, 2
        while off < len(chunk):
            pkts.append(bytes([idx, 0]) + chunk[off:off + mtu - 2])
            off += mtu - 2
            idx += 1
        yield p, total, pkts


def announce(n_packets: int) -> bytes:
    return bytes([0, 0, 0, 0, n_packets, 0])
