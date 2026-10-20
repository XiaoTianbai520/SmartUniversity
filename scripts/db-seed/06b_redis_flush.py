# -*- coding: utf-8 -*-
"""任务#4（续）：清理 Redis 中过期的选课容量/缓存 Key。

项目 Key 规则见 RedisCommonConstant / RedisKeyUtil：
  edu:smart-university:teaching-class:capacity:%s
  edu:smart-university:teaching-class:selected-count:%s
  edu:smart-university:teaching-class:waiting-count:%s
  edu:smart-university:teaching-class:waitlist-no:%s
  edu:smart-university:teaching-class:major:%s
  edu:smart-university:teaching-class:grade:%s
  edu:smart-university:dashboard:overview:%s
  edu:smart-university:stat:snapshot-time:%s

无 redis 第三方库，这里用最小 RESP 协议直接 socket 通信。
"""
import socket
import sys

HOST = "100.83.74.68"
PORT = 6379
PASSWORD = "123456"
PATTERNS = [
    "edu:smart-university:teaching-class:*",
    "edu:smart-university:dashboard:overview:*",
    "edu:smart-university:stat:snapshot-time:*",
]


class Resp(object):
    def __init__(self):
        self.sock = socket.create_connection((HOST, PORT), timeout=10)
        self.buf = b""

    def cmd(self, *args):
        parts = [b"*%d\r\n" % len(args)]
        for a in args:
            if isinstance(a, str):
                a = a.encode("utf-8")
            parts.append(b"$%d\r\n%s\r\n" % (len(a), a))
        self.sock.sendall(b"".join(parts))
        return self._read()

    def _line(self):
        while b"\r\n" not in self.buf:
            data = self.sock.recv(65536)
            if not data:
                raise EOFError("redis closed")
            self.buf += data
        line, self.buf = self.buf.split(b"\r\n", 1)
        return line

    def _read(self):
        line = self._line()
        t, body = line[:1], line[1:]
        if t == b"+":
            return body.decode("utf-8", "replace")
        if t == b"-":
            raise RuntimeError(body.decode("utf-8", "replace"))
        if t == b":":
            return int(body)
        if t == b"$":
            n = int(body)
            if n == -1:
                return None
            while len(self.buf) < n + 2:
                self.buf += self.sock.recv(65536)
            val, self.buf = self.buf[:n], self.buf[n + 2:]
            return val.decode("utf-8", "replace")
        if t == b"*":
            n = int(body)
            if n == -1:
                return None
            return [self._read() for _ in range(n)]
        raise RuntimeError("bad resp: %r" % line)


def main():
    r = Resp()
    r.cmd("AUTH", PASSWORD)
    print("[redis] AUTH ok, PING=%s" % r.cmd("PING"))
    total = 0
    for pat in PATTERNS:
        cursor = "0"
        found = []
        while True:
            res = r.cmd("SCAN", cursor, "MATCH", pat, "COUNT", "500")
            cursor = res[0]
            found.extend(res[1])
            if cursor == "0":
                break
        print("[redis] %-60s -> %d keys" % (pat, len(found)))
        if found:
            for i in range(0, len(found), 200):
                total += r.cmd("DEL", *found[i:i + 200])
    print("[redis] 共删除 %d 个 Key" % total)
    print("[redis] teaching-class 剩余 Key:",
          r.cmd("SCAN", "0", "MATCH", "edu:smart-university:teaching-class:*", "COUNT", "10")[1])


if __name__ == "__main__":
    main()
