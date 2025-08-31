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