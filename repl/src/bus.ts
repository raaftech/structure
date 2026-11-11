// A client of the SockJS event bus bridge, on a raw WebSocket.

import type { Request } from "./model/base/request.ts";
import type { Response } from "./model/base/response.ts";
import type { Json } from "./model/json.ts";

// A frame of the bridge. Vert.x gives no schema for it.
interface Frame {
  type: string;
  address?: string;
  body?: Json;
  replyAddress?: string;
  failureType?: string;
  message?: string;
}

interface Pending {
  resolve(response: Response): void;
  reject(error: BusError): void;
  timer: ReturnType<typeof setTimeout>;
}

type Listener = (body: Json) => void;

// A bridge or connection error. Handler errors are in the response.
export class BusError extends Error {}

export class Bus {
  readonly #socket: WebSocket;
  readonly #pending = new Map<string, Pending>();
  readonly #listeners = new Map<string, Set<Listener>>();
  readonly #ping: ReturnType<typeof setInterval>;
  #closed = false;
  // Bridge errors without a request, for example access_denied.
  onerror: (error: BusError) => void = () => {};
  onclose: () => void = () => {};

  // The bridge closes a socket that sends no ping for 10 seconds.
  constructor(socket: WebSocket, ping = 5000) {
    this.#socket = socket;
    socket.binaryType = "arraybuffer";
    socket.onmessage = (event) => this.#receive(JSON.parse(text(event.data)));
    socket.onclose = () => this.#close();
    this.#ping = setInterval(() => this.#write({ type: "ping" }), ping);
  }

  // Connect to a bridge, for example wss://host:8443/eventbus/websocket.
  static connect(url: string, timeout = 10000): Promise<Bus> {
    return new Promise((resolve, reject) => {
      const socket = new WebSocket(url);
      const timer = setTimeout(() => {
        socket.close();
        reject(new BusError(`No connection to ${url} after ${timeout} ms`));
      }, timeout);
      socket.onopen = () => {
        clearTimeout(timer);
        resolve(new Bus(socket));
      };
      socket.onerror = async () => {
        clearTimeout(timer);
        reject(new BusError(`Cannot connect to ${url}: ${await reason(url)}`));
      };
    });
  }

  // Send a request and wait for the response. The bridge waits 30 s.
  request(address: string, msgh: string, args: Request["args"] = {}, timeout = 30000): Promise<Response> {
    return new Promise((resolve, reject) => {
      if (this.#closed) {
        return reject(new BusError("The connection is closed"));
      }
      const replyAddress = crypto.randomUUID();
      const timer = setTimeout(
        () => this.#settle(replyAddress, new BusError(`No response from ${address} after ${timeout} ms`)),
        timeout,
      );
      this.#pending.set(replyAddress, { resolve, reject, timer });
      const body: Request = { msgh, args };
      this.#write({ type: "send", address, body, replyAddress });
    });
  }

  // Receive the messages on an address; call the result to stop.
  register(address: string, listener: Listener): () => void {
    let listeners = this.#listeners.get(address);
    if (!listeners) {
      listeners = new Set();
      this.#listeners.set(address, listeners);
      this.#write({ type: "register", address });
    }
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
      if (listeners.size === 0 && this.#listeners.get(address) === listeners) {
        this.#listeners.delete(address);
        this.#write({ type: "unregister", address });
      }
    };
  }

  // Close the connection. Requests that wait get an error.
  close() {
    this.#socket.close();
    this.#close();
  }

  #write(frame: Frame) {
    if (!this.#closed) {
      this.#socket.send(JSON.stringify(frame));
    }
  }

  #receive(frame: Frame) {
    const address = frame.address ?? "";
    if (frame.type === "rec" && this.#pending.has(address)) {
      this.#settle(address, frame.body as unknown as Response);
    } else if (frame.type === "rec") {
      this.#listeners.get(address)?.forEach((listener) => listener(frame.body ?? null));
    } else if (frame.type === "err") {
      const error = new BusError(frame.message ?? String(frame.body ?? frame.failureType));
      if (this.#pending.has(address)) {
        this.#settle(address, error);
      } else {
        this.onerror(error);
      }
    }
  }

  #settle(replyAddress: string, outcome: Response | BusError) {
    const pending = this.#pending.get(replyAddress);
    if (pending) {
      this.#pending.delete(replyAddress);
      clearTimeout(pending.timer);
      if (outcome instanceof BusError) {
        pending.reject(outcome);
      } else {
        pending.resolve(outcome);
      }
    }
  }

  #close() {
    if (!this.#closed) {
      this.#closed = true;
      clearInterval(this.#ping);
      for (const replyAddress of this.#pending.keys()) {
        this.#settle(replyAddress, new BusError("The connection is closed"));
      }
      this.onclose();
    }
  }
}

// The bridge sends binary frames.
function text(data: unknown): string {
  return typeof data === "string" ? data : new TextDecoder().decode(data as ArrayBuffer);
}

// A WebSocket error has no cause, thus get it with an HTTP request.
async function reason(url: string): Promise<string> {
  try {
    await fetch(url.replace(/^ws/, "http"));
    return "the WebSocket handshake failed";
  } catch (error) {
    const cause = (error as Error).cause as (Error & { code?: string }) | undefined;
    return cause?.message || cause?.code || (error as Error).message;
  }
}
