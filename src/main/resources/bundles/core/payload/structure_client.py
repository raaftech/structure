import base64
import copy
import hashlib
import json
import os
from pathlib import Path
import socket
import ssl
import struct
import threading
import time
import uuid


BUS = "tech.raaf.structure.bus."


class BusError(RuntimeError):
    def __init__(self, error):
        self.error = error
        super().__init__(error if isinstance(error, str) else json.dumps(error))


class BusClient:
    def __enter__(self):
        directory = Path(os.environ["STRUCTURE_STATE_DIR"])
        if not directory.is_absolute():
            raise ValueError("STRUCTURE_STATE_DIR must be an absolute path")
        settings = json.loads((directory / "structure.json").read_text())
        host, port = settings["https-host"], int(settings["https-port"])
        context = ssl.create_default_context()
        context.load_verify_locations(cafile=settings["ca-cert"])
        self.socket = None
        self.stopped = threading.Event()
        self.lock = threading.Lock()
        self.error = None
        try:
            connection = socket.create_connection((host, port), timeout=10)
            try:
                self.socket = context.wrap_socket(connection, server_hostname=host)
            except BaseException:
                connection.close()
                raise
            key = base64.b64encode(os.urandom(16)).decode("ascii")
            authority = f"[{host}]" if ":" in host else host
            self.socket.sendall((
                "GET /eventbus/websocket HTTP/1.1\r\n"
                f"Host: {authority}:{port}\r\n"
                "Upgrade: websocket\r\nConnection: Upgrade\r\n"
                f"Sec-WebSocket-Key: {key}\r\n"
                "Sec-WebSocket-Version: 13\r\n\r\n"
            ).encode("ascii"))
            deadline = time.monotonic() + 10
            response = bytearray()
            while not response.endswith(b"\r\n\r\n"):
                if len(response) >= 16384:
                    raise RuntimeError("WebSocket response headers are too large")
                response.extend(self.read(1, deadline))
            lines = response.decode("iso-8859-1").split("\r\n")
            headers = dict(line.split(":", 1) for line in lines[1:] if line)
            headers = {name.lower(): value.strip() for name, value in headers.items()}
            accept = base64.b64encode(hashlib.sha1(
                (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").encode("ascii")
            ).digest()).decode("ascii")
            if (lines[0].split()[1] != "101"
                    or headers.get("upgrade", "").lower() != "websocket"
                    or "upgrade" not in {value.strip() for value in headers.get("connection", "").lower().split(",")}
                    or headers.get("sec-websocket-accept") != accept):
                raise RuntimeError("Event bus WebSocket upgrade failed")
            threading.Thread(target=self.heartbeat, daemon=True).start()
            return self
        except BaseException:
            self.close()
            raise

    def __exit__(self, *_):
        self.close()

    def close(self):
        self.stopped.set()
        if self.socket:
            try:
                self.socket.shutdown(socket.SHUT_RDWR)
            except OSError:
                pass
            self.socket.close()

    def send(self, opcode, payload):
        mask = os.urandom(4)
        length = len(payload)
        header = bytes([0x80 | opcode])
        if length < 126:
            header += bytes([0x80 | length])
        elif length < 65536:
            header += bytes([0x80 | 126]) + struct.pack("!H", length)
        else:
            header += bytes([0x80 | 127]) + struct.pack("!Q", length)
        frame = header + mask + bytes(value ^ mask[i % 4] for i, value in enumerate(payload))
        with self.lock:
            if self.error:
                raise self.error
            self.socket.sendall(frame)

    def heartbeat(self):
        while not self.stopped.wait(5):
            try:
                self.send(1, b'{"type":"ping"}')
            except Exception as error:
                self.error = error
                self.close()
                return

    def read(self, length, deadline):
        data = bytearray()
        while len(data) < length:
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                raise TimeoutError("Event bus request timed out")
            self.socket.settimeout(remaining)
            chunk = self.socket.recv(length - len(data))
            if not chunk:
                raise RuntimeError("Event bus connection closed")
            data.extend(chunk)
        return bytes(data)

    def receive(self, deadline):
        message = bytearray()
        started = False
        while True:
            first, second = self.read(2, deadline)
            opcode, final = first & 15, bool(first & 0x80)
            if first & 0x70 or second & 0x80:
                raise RuntimeError("Invalid WebSocket frame")
            length = second & 127
            if length == 126:
                length = struct.unpack("!H", self.read(2, deadline))[0]
            elif length == 127:
                length = struct.unpack("!Q", self.read(8, deadline))[0]
            if opcode >= 8 and (not final or length > 125):
                raise RuntimeError("Invalid WebSocket control frame")
            if len(message) + length > 16 * 1024 * 1024:
                raise RuntimeError("Event bus message is too large")
            payload = self.read(length, deadline)
            if opcode == 8:
                self.send(8, payload)
                raise RuntimeError("Event bus connection closed")
            if opcode == 9:
                self.send(10, payload)
                continue
            if opcode == 10:
                continue
            if opcode in (1, 2) and not started:
                started = True
            elif opcode != 0 or not started:
                raise RuntimeError("Invalid WebSocket message")
            message.extend(payload)
            if final:
                return json.loads(message)

    def request(self, address, msgh, args=None):
        reply_address = str(uuid.uuid4())
        self.send(1, json.dumps({
            "type": "send", "address": BUS + address,
            "replyAddress": reply_address, "body": {"msgh": msgh, "args": args or {}},
        }).encode("utf-8"))
        deadline = time.monotonic() + 10
        while True:
            message = self.receive(deadline)
            if message.get("type") == "err":
                raise BusError(message.get("message", message.get("failureType")))
            if message.get("address") != reply_address:
                continue
            result = message.get("body", {}).get("result", {})
            if "error" in result:
                raise BusError(result["error"])
            if "data" not in result:
                raise RuntimeError("Invalid event bus reply")
            return result["data"]

    def find(self, property, value):
        return self.request("repository.instances", "findOneByData", {
            "property": property, "value": value,
        })

    def instance(self, identifier):
        return self.request("node." + identifier, "json")

    def update(self, entity):
        result = self.request("manager.entitymanager", "update", {"entities": [entity]})
        if not isinstance(result, list) or entity["meta"]["uuid"] not in result:
            raise RuntimeError("Could not update entity: " + json.dumps(result))
        return result

    def persist(self, entities, related=()):
        identifiers = []
        for entity in entities:
            identifier = entity["meta"]["uuid"]
            try:
                result = self.request("manager.entitymanager", "create", {"entities": [entity]})
            except BusError as error:
                result = error.error
            if isinstance(result, list) and identifier in result:
                identifiers.append(identifier)
                continue
            try:
                conflict = json.loads(result) if isinstance(result, str) else result
            except ValueError:
                conflict = None
            if not isinstance(conflict, dict) or conflict.get("code") != 409 or not conflict.get("conflict"):
                raise RuntimeError("Could not create entity: " + json.dumps(result))
            replacement = conflict["conflict"]
            entity["meta"]["uuid"] = replacement
            for item in [*entities, *related]:
                for relation in item["meta"]["relations"].values():
                    relation["to"] = list(dict.fromkeys(
                        replacement if target == identifier else target for target in relation["to"]
                    ))
            identifiers.append(replacement)
        return identifiers


def relate(entity, name, target):
    relation = entity["meta"]["relations"].setdefault(name, {"direction": "outgoing", "to": []})
    if target["meta"]["uuid"] not in relation["to"]:
        relation["to"].append(target["meta"]["uuid"])


class Graph:
    def __init__(self, client):
        self.types = client.request("repository.types", "getAll")

    def type(self, name):
        for item in self.types:
            if name in (item["name"], item["meta"]["uuid"]):
                return item
        raise ValueError("Missing type: " + name)

    def is_type(self, item, base):
        if item["meta"]["uuid"] == self.type(base)["meta"]["uuid"]:
            return True
        return any(self.is_type(self.type(parent), base)
                   for parent in item["meta"]["relations"].get("ISA", {}).get("to", []))

    def instance(self, name, data):
        base = self.type(name)
        entity = copy.deepcopy(base)
        entity["data"] = data
        entity["meta"].pop("constraints", None)
        entity["meta"]["uuid"] = str(uuid.uuid4())
        relations = entity["meta"]["relations"]
        for key in ("ISA", "CONSTRAINTS", "IMPLEMENTS"):
            relations.pop(key, None)
        relations["INSTANCE_OF"] = {"direction": "outgoing", "to": [base["meta"]["uuid"]]}
        return entity

    def software(self, name, version=""):
        item = self.type(name)
        return self.instance(name, {
            "name": item["name"], "desc": item["desc"], "ver": version,
            "shortName": item["name"], "longName": item["name"],
        })

    def address(self, ip):
        entity = self.instance("Ipv4Address", {
            "name": ip, "desc": "An IPv4 address", "addr": ip, "reachable": True,
        })
        entity["name"] = ip
        return entity

    def dns(self, hostname):
        entity = self.instance("DnsName", {
            "name": hostname, "desc": "A host name", "dnsname": hostname,
        })
        entity["name"] = hostname
        return entity
