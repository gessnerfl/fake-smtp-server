// MSW v2 polyfills for Jest environment
import { TextEncoder, TextDecoder } from 'util';

// Polyfill text encoding
Object.assign(global, {
  TextEncoder,
  TextDecoder,
});

// Override Request constructor to fix AbortSignal issue
const OriginalRequest = globalThis.Request || require('undici').Request;

// @ts-ignore
globalThis.Request = class Request extends OriginalRequest {
  constructor(input: RequestInfo | URL, init?: RequestInit) {
    // Remove AbortSignal from init to avoid compatibility issues
    const cleanInit = init ? { ...init } : {};
    delete cleanInit.signal;
    super(input, cleanInit);
  }
};

// Ensure other globals are available
if (!globalThis.fetch) {
  // @ts-ignore
  globalThis.fetch = require('undici').fetch;
}

if (!globalThis.Response) {
  // @ts-ignore
  globalThis.Response = require('undici').Response;
}

if (!globalThis.Headers) {
  // @ts-ignore
  globalThis.Headers = require('undici').Headers;
}

// Mock EventSource for tests
if (!globalThis.EventSource) {
  class MockEventSource {
    url: string;
    readyState: number = 1; // OPEN
    
    static readonly CONNECTING = 0;
    static readonly OPEN = 1;
    static readonly CLOSED = 2;
    
    constructor(url: string) {
      this.url = url;
      // Simulate connection opening after a microtask
      setTimeout(() => {
        this.dispatchEvent(new Event('open'));
      }, 0);
    }
    
    addEventListener(type: string, listener: any) {
      // No-op for tests - events aren't fired in test environment
    }
    
    removeEventListener(type: string, listener: any) {
      // No-op for tests
    }
    
    dispatchEvent(event: Event): boolean {
      return true;
    }
    
    close() {
      this.readyState = 2; // CLOSED
    }
  }
  
  // @ts-ignore
  globalThis.EventSource = MockEventSource;
}