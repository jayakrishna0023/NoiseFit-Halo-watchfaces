"""ZH dial container (AF AF AF 02): unpack / pack, mirroring com/zhapp/ble/utils/lzo/LzoUtils.mergeDialFile().

Container:
  0  u32 BE 0xAFAFAF02
  4  u32 LE raw length
  8  4B    CRCNew32(raw)
 12  u32 LE compressed body length
 16  4B    CRCNew32(body)
 20  u32   0
 24  u16 LE block count (4096-byte raw blocks)
 26  u16 LE per block: size | 0x8000 when LZO-compressed (plain size = stored raw)
 ..  body
"""
import os
import struct

import lzo1x

MAGIC = b"\xaf\xaf\xaf\x02"
BLOCK = 4096
_TABLE = [int(x) for x in open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "zh_crc_table.txt")).read().split(",")][:256]


def crc_new32(data: bytes) -> bytes:
    """LzoCrcUtils.getCRCNew32: custom table, init 0xFFFFFFFF, output ~crc little-endian."""
    c = 0xFFFFFFFF
    for b in data:
        c = _TABLE[(c ^ b) & 0xFF] ^ ((c >> 8) & 0xFFFFFF)
    return struct.pack("<I", (~c) & 0xFFFFFFFF)


def unpack(blob: bytes) -> bytes:
    if blob[:4] != MAGIC:
        raise ValueError("not an AF AF AF 02 dial container")
    raw_len, = struct.unpack_from("<I", blob, 4)
    body_len, = struct.unpack_from("<I", blob, 12)
    count, = struct.unpack_from("<H", blob, 24)
    table = struct.unpack_from(f"<{count}H", blob, 26)
    off = 26 + 2 * count
    body = blob[off:off + body_len]
    if blob[8:12] != b"" and len(body) != body_len:
        raise ValueError("body truncated")
    out = bytearray()
    p = 0
    for e in table:
        n = e & 0x7FFF
        blk = body[p:p + n]
        p += n
        out += lzo1x.decompress(blk) if e & 0x8000 else blk
    if len(out) != raw_len:
        raise ValueError(f"raw length {len(out)} != header {raw_len}")
    if crc_new32(bytes(out)) != blob[8:12]:
        raise ValueError("raw CRC mismatch")
    if crc_new32(body) != blob[16:20]:
        raise ValueError("body CRC mismatch")
    return bytes(out)


def pack(raw: bytes) -> bytes:
    """Exact mirror of LzoUtils.mergeDialFile(): 4096-byte blocks, LZO1X-1 (python-lzo), a block is stored raw
    (flag clear) when compression does not make it smaller; app CRCs over raw and over the body."""
    import lzo  # python-lzo; LZO1X-1 output verified identical to the vendor's blocks
    count = -(-len(raw) // BLOCK)
    table, body = [], bytearray()
    for i in range(count):
        blk = raw[i * BLOCK:(i + 1) * BLOCK]
        comp = lzo.compress(blk, 1, False)
        if len(blk) <= len(comp):
            table.append(len(blk))
            body += blk
        else:
            table.append(len(comp) | 0x8000)
            body += comp
    hdr = (MAGIC + struct.pack("<I", len(raw)) + crc_new32(raw) + struct.pack("<I", len(body))
           + crc_new32(bytes(body)) + bytes(4) + struct.pack("<H", count))
    return hdr + struct.pack(f"<{count}H", *table) + bytes(body)


def pack_stored(raw: bytes) -> bytes:
    """Pack with every block stored uncompressed (flag 0) — a form mergeDialFile itself emits
    whenever LZO does not shrink a block."""
    count = -(-len(raw) // BLOCK)
    table, body = [], bytearray()
    for i in range(count):
        blk = raw[i * BLOCK:(i + 1) * BLOCK]
        table.append(len(blk))  # flag bit 15 clear = stored
        body += blk
    hdr = (MAGIC + struct.pack("<I", len(raw)) + crc_new32(raw) + struct.pack("<I", len(body))
           + crc_new32(bytes(body)) + bytes(4) + struct.pack("<H", count))
    return hdr + struct.pack(f"<{count}H", *table) + bytes(body)
