# NoiseFit Halo (ZH "Apricot") — what Halo Studio relies on

All values verified against real captures of the official app talking to a Halo NSW-301 (fw V1.3.0).

## BLE
| Characteristic (service `16186f00-0000-1000-8000-00807f9b34fb`) | Use |
|---|---|
| `16186f01-…` | watch → phone replies (notify); phone acks here |
| `16186f02-…` | phone → watch commands (write without response); watch acks via notify |
| `16186f04-…` | bulk (watch-face) transfer |
Other services seen: `AA01` (legacy), `FFD0` (**Realtek firmware DFU — never touch**), `1812` (HID).
Request ATT MTU 247; the official app uses 177-byte packets for bulk data.

### Small-message framing (6f02 → watch, and 6f01 ← watch)
sender `00 00 00 00 <n> 00` → receiver `00 00 01 01 00 00` (go) → sender `01 00 <protobuf>` (×n)
→ receiver `00 00 01 00 00 00` (done). `00 00 01 03` = timed out/reset, `00 00 01 05 <idx>` = resend.

### Commands used (protobuf, field 1 = command number)
| cmd | meaning | payload |
|---|---|---|
| 19 | verifyUserId (sent on every connect) | `1a { 32 <user id string> }` |
| 83 | watch-face pre-check | `3a { 22 { 0a <face id> 10 <kB> 18 <kB> 20 <replace> } }` → reply status 0 READY, 1 BUSY, 2 DUPLICATED, 3 LOW_STORAGE, 4 LOW_BATTERY, 5 DOWNGRADE |
| 241 | read always-on-display settings | — |
| 242 | write always-on-display settings | `72 { 62 { 08 mode, 12 {start}, 1a {end}, 20 style } }` |
AOD: mode 0 all-day on, 1 timed, 2 off; style 0 analog, 1 digital; time = `08 h 10 m 18 s 20 ms` (proto2, all set).

## Upload (on 6f04)
Stream = `00 10 00×16 <len u32 LE>` + face file + CRC32(header+face, zlib, LE). Type byte `0x10` = watch face
(anything else is refused by Halo Studio; other values are AGPS/avatar/**firmware**).
Pieces of 22 packets (MTU 177): announce `00 00 00 00 <n> 00` → go → packet 1 `01 00 <total LE16> <piece LE16> data`,
packets 2..n `<i> 00 data` → piece OK `00 00 01 00`.

## Face file `AF AF AF 02`
| off | field |
|---|---|
| 0 | `AF AF AF 02` |
| 4 | raw length (LE) |
| 8 | checksum(raw) |
| 12 | body length (LE) |
| 16 | checksum(body) |
| 20 | 0 |
| 24 | block count (LE16), then per 4096-byte block: LE16 size, bit 15 = LZO1X-1 compressed |
Official faces carry a vendor checksum we can't reproduce, **but the watch also accepts the official app's own
packer checksum** (table-driven CRC with a custom 256-entry table, init 0xFFFFFFFF, `~crc` LE) — that is what
Halo Studio writes.

## Raw face layout (big-endian RGB565 images)
- `0x470`: 310×310 face-picker preview · `0x2F338`: 466×466 background
- element records from `0x108`: `x u16, y u16, w u16, h u16, n u8 (#images), offset u32 [, offset2 u32 = 2nd digit at x+w]`
- glyph images = template background at that spot + white glyph (+ anti-aliased edges) → can be redrawn on any background
- Galaxy layout (face 1246): every glyph set unique · Moon layout (face 898): some glyph sets shared (→ tiles)
- weekday sets have 14 images (7 English + 7 Chinese); Moon AM/PM has 4 (AM, PM, 上午, 下午)
