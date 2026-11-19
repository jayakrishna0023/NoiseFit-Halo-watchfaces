"""Find your NoiseFit user ID (sent by NoiseFit as `verifyUserId`, cmd 19) in YOUR OWN Bluetooth HCI log.

READ-ONLY: parses a log file on your PC; never talks to a watch.

Usage:  python find_user_id.py <bugreport.zip | btsnoop_hci.log>
Then paste the ID into Halo Studio → Watch → Settings → "NoiseFit user ID".
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from extract_face import att_pdus, load_snoop_blobs, parse_btsnoop  # noqa: E402


def main():
    found = set()
    for name, blob in load_snoop_blobs(sys.argv[1]):
        for sent, ts, conn, pdu in att_pdus(parse_btsnoop(blob)):
            if not sent or pdu[0] not in (0x12, 0x52):
                continue
            v = pdu[3:]
            # "01 00" + 08 13 1a <len> 32 <len> <token>
            k = v.find(b"\x08\x13\x1a")
            if k < 0:
                continue
            p = k + 3
            p += 1                      # length of field 3
            if p < len(v) and v[p] == 0x32:
                n = v[p + 1]
                token = v[p + 2:p + 2 + n]
                try:
                    found.add(token.decode("ascii"))
                except UnicodeDecodeError:
                    pass
    if not found:
        print("No verifyUserId message found. Capture a log while NoiseFit connects to the watch.")
    for t in sorted(found):
        print("NoiseFit user ID:", t)


if __name__ == "__main__":
    main()
