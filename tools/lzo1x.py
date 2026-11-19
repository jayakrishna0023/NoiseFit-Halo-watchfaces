"""Pure-Python LZO1X decompressor (follows the Linux kernel lzo1x_decompress_safe state machine)."""


class LzoError(ValueError):
    pass


def _ext(src, ip, base):
    """Length extension: run of zero bytes (255 each) then a final byte."""
    zeros = 0
    while src[ip] == 0:
        zeros += 1
        ip += 1
    return base + 255 * zeros + src[ip], ip + 1


def decompress(src: bytes, out_max: int = 1 << 24) -> bytes:
    out = bytearray()
    ip = 0
    state = 0

    def copy_match(dist, length):
        start = len(out) - dist
        if start < 0:
            raise LzoError("match before start of output")
        for k in range(length):
            out.append(out[start + k])

    def literals(n):
        nonlocal ip
        if ip + n > len(src):
            raise LzoError("literal run past end of input")
        out.extend(src[ip:ip + n])
        ip += n

    if src[0] > 17:
        t = src[0] - 17
        ip = 1
        literals(t)
        state = t if t < 4 else 4
    while True:
        if ip >= len(src):
            raise LzoError("ran out of input without end marker")
        t = src[ip]
        ip += 1
        if t < 16:
            if state == 0:
                if t == 0:
                    t, ip = _ext(src, ip, 15)
                literals(t + 3)
                state = 4
                continue
            if state != 4:
                dist = 1 + (t >> 2) + (src[ip] << 2)
                ip += 1
                copy_match(dist, 2)
            else:
                dist = 1 + 0x0800 + (t >> 2) + (src[ip] << 2)
                ip += 1
                copy_match(dist, 3)
            nxt = t & 3
        elif t >= 64:
            dist = 1 + ((t >> 2) & 7) + (src[ip] << 3)
            ip += 1
            copy_match(dist, (t >> 5) + 1)
            nxt = t & 3
        elif t >= 32:
            length = (t & 31) + 2
            if length == 2:
                length, ip = _ext(src, ip, 33)
            v = src[ip] | (src[ip + 1] << 8)
            ip += 2
            copy_match(1 + (v >> 2), length)
            nxt = v & 3
        else:  # 16..31
            high = (t & 8) << 11
            length = (t & 7) + 2
            if length == 2:
                length, ip = _ext(src, ip, 9)
            v = src[ip] | (src[ip + 1] << 8)
            ip += 2
            dist = high + (v >> 2)
            if dist == 0:
                if length != 3:
                    raise LzoError("bad end marker")
                return bytes(out)
            copy_match(dist + 0x4000, length)
            nxt = v & 3
        if len(out) > out_max:
            raise LzoError("output too large")
        state = nxt
        if nxt:
            literals(nxt)
